// The boot screen's progress list, shared by every page that loads the mod.
//
// WHY THIS EXISTS. Bringing common/ up in a browser moves about six and a half
// megabytes - a 2 MB wasm, a 2.6 MB gene bundle, the breeds, two name tables
// and five PNGs - and then compiles the wasm, which on a phone is seconds more.
// At a desk the whole thing is over before the boot screen has finished fading
// in. On mobile data it is a minute or more, and for that minute the screen
// said "Loading the mod..." and nothing else, which is indistinguishable from
// a page that has hung. It was reported as one.
//
// So the reader gets the whole list of stages, which one is running, how many
// bytes have arrived, and - if a stage takes long enough to worry about - a
// line saying in as many words that this is slow rather than stuck.
//
// It renders HG.java.progress(). Nothing here knows what a gene is; it is a
// progress list that happens to be watching a wasm boot.
//
//   HG.bootProgress.attach(document.querySelector(".boot-inner"));
//
// The styles come with it, because the two designers and the wiki's gene pages
// do not share a stylesheet and a fourth copy of nine rules is how they drift.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var HERE = (function () {
    var self = document.currentScript;
    return self && self.src ? self.src.replace(/boot-progress\.js(\?.*)?$/, "") : "";
  })();

  // How long a single stage may take before the screen stops looking confident
  // and starts reassuring. Generous on purpose: the wasm compile legitimately
  // sits still for several seconds on a phone, and crying "slow" at four
  // seconds would train the reader to ignore the line that matters.
  var PATIENCE_MS = 12000;

  (function stylesheet() {
    if (document.querySelector("style[data-phc-boot]")) { return; }
    var s = document.createElement("style");
    s.setAttribute("data-phc-boot", "");
    s.textContent = [
      ".phc-boot { margin: 14px auto 0; max-width: 340px; text-align: left; }",
      ".phc-boot ol { list-style: none; margin: 0; padding: 0; }",
      ".phc-boot li { display: flex; align-items: baseline; gap: 8px; padding: 2px 0;",
      "  font-size: 0.86rem; color: #6f7f9b; line-height: 1.35; }",
      ".phc-boot li .tick { flex: none; width: 1em; text-align: center; }",
      ".phc-boot li.done { color: #7f8fa8; }",
      ".phc-boot li.done .tick { color: #4ade80; }",
      ".phc-boot li.now { color: #e8eef8; font-weight: 600; }",
      ".phc-boot li.now .tick { color: #38bdf8; }",
      ".phc-boot .num { font-variant-numeric: tabular-nums; font-weight: 400;",
      "  color: #8fa0bb; font-size: 0.8rem; }",
      ".phc-boot .bar { height: 4px; border-radius: 3px; background: rgba(255,255,255,0.10);",
      "  margin: 5px 0 2px calc(1em + 8px); overflow: hidden; }",
      ".phc-boot .bar i { display: block; height: 100%; background: #38bdf8;",
      "  border-radius: 3px; transition: width 0.18s linear; }",
      ".phc-boot .stall { margin: 9px 0 0; font-size: 0.82rem; color: #fbbf24; }",
      ".phc-boot .stall.bad { color: #fb7185; }",
      "@media (prefers-reduced-motion: reduce) { .phc-boot .bar i { transition: none; } }"
    ].join("\n");
    document.head.appendChild(s);
  })();

  function esc(s) {
    return String(s == null ? "" : s).replace(/[&<>"]/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
    });
  }

  function amount(p) {
    if (!p.loaded) { return ""; }
    if (p.unit !== "bytes") {
      return p.total ? p.loaded + " of " + p.total : String(p.loaded);
    }
    var size = HG.java && HG.java.size ? HG.java.size : function (b) { return b + " B"; };
    return p.total ? size(p.loaded) + " of " + size(p.total) : size(p.loaded);
  }

  /**
   * Put a live progress list inside `host`, and keep it there until the boot
   * finishes or fails.
   *
   * The clock is this file's, not java.js's: the interesting question on a slow
   * phone is "how long has this stage been going", and only something ticking
   * can answer it. It stops itself on done, on error, and on detach.
   */
  function attach(host) {
    if (!host || !HG.java || !HG.java.progress) { return function () {}; }

    var box = document.createElement("div");
    box.className = "phc-boot";
    box.setAttribute("role", "status");
    box.setAttribute("aria-live", "polite");
    host.appendChild(box);

    var latest = null;
    var high = 0;           // the bar is a floor: see the note where it is drawn
    var ticker = window.setInterval(function () { if (latest) { paint(latest); } }, 1000);

    function paint(p) {
      if (p.error) {
        // The page's own failure handler owns the message; this just gets out
        // of the way rather than leaving a half-filled bar under it.
        box.innerHTML = "";
        stop();
        return;
      }
      var waited = p.since ? Date.now() - p.since : 0;
      var h = "<ol>";
      for (var i = 0; i < p.phases.length; i++) {
        var cls = i < p.index || p.done ? "done" : (i === p.index ? "now" : "");
        var tick = i < p.index || p.done ? "&#10003;" : (i === p.index ? "&#9679;" : "&#9675;");
        h += '<li class="' + cls + '"><span class="tick">' + tick + "</span>"
          + "<span>" + esc(p.phases[i]);
        if (i === p.index && !p.done) {
          // The number when there is one, the sentence when there is not. Both
          // at once read as one run-on line and the eye cannot find the count.
          var num = amount(p);
          if (num) { h += ' <span class="num">' + esc(num) + "</span>"; }
          else if (p.detail) { h += ' <span class="num">' + esc(p.detail) + "</span>"; }
        }
        h += "</span></li>";
      }
      h += "</ol>";
      if (!p.done) {
        // The bar measures STAGES, not bytes. Bytes would be the better ruler,
        // but the published wiki is gzipped and the browser will not say how
        // much of the compressed stream has landed (see fetchTracked), so a
        // byte bar is indeterminate in exactly the case that matters. Stages
        // are always countable, always go forwards, and never lie. Where a
        // stage does know its own fraction, it refines the step it is on.
        //
        // And it only ever goes forwards. A stage that starts out believing a
        // Content-Length and then discovers the response was compressed loses
        // its within-stage fraction mid-flight, which slid the bar backwards -
        // the one thing a progress bar must never do, because a reader reads it
        // as the work being undone.
        var within = p.total ? Math.max(0, Math.min(1, p.loaded / p.total)) : 0;
        var frac = (Math.max(0, p.index) + within) / p.phases.length;
        high = Math.max(high, Math.min(1, frac));
        h += '<div class="bar"><i style="width:' + (high * 100).toFixed(1) + '%"></i></div>';
      }
      if (waited > PATIENCE_MS && !p.done) {
        h += '<p class="stall' + (waited > PATIENCE_MS * 5 ? " bad" : "") + '">'
          + "Still on this step after " + Math.round(waited / 1000) + "s. "
          + (waited > PATIENCE_MS * 5
            ? "That is longer than it should take - if it never moves, reload the page."
            : "It is downloading, not stuck.")
          + "</p>";
      }
      box.innerHTML = h;
    }

    function stop() {
      window.clearInterval(ticker);
      if (unsubscribe) { unsubscribe(); }
    }

    var unsubscribe = HG.java.progress(function (p) {
      latest = p;
      paint(p);
      if (p.done) {
        // Leave the finished list on screen; the boot overlay is about to go.
        window.clearInterval(ticker);
      }
    });

    return stop;
  }

  HG.bootProgress = { attach: attach, here: HERE };
})(window.HG);
