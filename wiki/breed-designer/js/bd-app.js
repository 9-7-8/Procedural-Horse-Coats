// The breed designer's frame: the step on the left, the horse on the right.
//
// The left pane has two tabs - the step you are on, and everything chosen so
// far (click any step to go back to it). The right pane is one founder of the
// breed as it stands, rolled by the game's own BreedFounder, with a button in
// the top-right corner that rolls another. Every change to the file reaches
// it through HG.bd.changed(), which is debounced here: a slider produces an
// event a pixel, and a founder is a full coat compose.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var bd = HG.bd;
  var el = function (t, a, c) { return bd.el(t, a, c); };
  var SAVE_KEY = "phc-breed-designer";
  var current = 0;
  var tab = "step";
  var seed = 1;
  var view = null;
  var SHEET = 128;
  var $ = function (id) { return document.getElementById(id); };

  function start() {
    if (location.protocol === "file:") {
      return fail("This page loads the mod as WebAssembly, which a browser will not fetch from a " +
        "file:// path. Serve the repository with any static server, or open the published wiki.");
    }
    HG.java.load().then(function (api) {
      return fetch("assets/marking-facts.json")
        .then(function (r) { return r.ok ? r.json() : null; })
        .catch(function () { return null; })
        .then(function (facts) { boot(api, facts); });
    }).catch(function (e) { fail(String(e && e.message ? e.message : e)); });
  }

  function fail(message) {
    $("boot").classList.add("failed");
    $("boot").querySelector(".boot-inner").innerHTML = "<h1>Could not start</h1><p>" + bd.esc(message) + "</p>";
  }

  function boot(api, facts) {
    bd.api = api;
    SHEET = api.sheetSize();
    bd.genes = JSON.parse(api.genesJson());
    bd.genes.forEach(function (g) { bd.geneByKey[g.key] = g; });
    bd.facts = facts;
    bd.state = restore() || bd.blank();
    bd.rerender = renderPanel;
    bd.onChange = changed;
    bd.installHoverCard();
    wire();
    renderPanel();
    renderVerdict();
    renderFounders();
    $("boot").classList.add("gone");
  }

  // ---- persistence: a convenience, never the file -----------------------

  function restore() {
    try {
      var saved = JSON.parse(localStorage.getItem(SAVE_KEY) || "null");
      if (!saved || !saved.state) return null;
      current = Math.max(0, Math.min(bd.steps.length - 1, saved.step || 0));
      var s = bd.blank();
      Object.keys(s).forEach(function (k) { if (saved.state[k] !== undefined) s[k] = saved.state[k]; });
      return s;
    } catch (e) { return null; }
  }

  function save() {
    try { localStorage.setItem(SAVE_KEY, JSON.stringify({ state: bd.state, step: current })); } catch (e) { /* private window */ }
  }

  // ---- change -----------------------------------------------------------

  var timer = null;
  function changed() {
    if (timer) clearTimeout(timer);
    timer = setTimeout(function () {
      save();
      renderVerdict();
      renderFounders();
      if (tab === "chosen") renderPanel();
    }, 160);
  }

  // ---- the left pane ----------------------------------------------------

  function wire() {
    $("tab-step").addEventListener("click", function () { tab = "step"; renderPanel(); });
    $("tab-chosen").addEventListener("click", function () { tab = "chosen"; renderPanel(); });
    $("btn-back").addEventListener("click", function () { go(current - 1); });
    $("btn-next").addEventListener("click", function () { go(current + 1); });
    $("btn-reroll").addEventListener("click", function () { seed = (seed + 1) | 0; renderFounders(); });
    $("btn-export").addEventListener("click", exportJson);
    $("file-input").addEventListener("change", function (e) {
      var file = e.target.files && e.target.files[0];
      e.target.value = "";
      if (file) file.text().then(importJson);
    });
    $("btn-new").addEventListener("click", function () {
      if (!confirm("Start a new breed? The one open now is not saved anywhere but this browser - export it first if you want it.")) return;
      bd.state = bd.blank();
      current = 0;
      tab = "step";
      changed();
      renderPanel();
    });
    var picker = $("open-builtin");
    JSON.parse(bd.api.breedCatalogJson()).forEach(function (b) {
      if (b.id === "feral_mixed") return;
      picker.appendChild(el("option", { value: b.id, text: b.name }));
    });
    picker.addEventListener("change", function () {
      if (!picker.value) return;
      importJson(bd.api.breedFileJson(picker.value));
      picker.value = "";
    });
  }

  function go(i) {
    current = Math.max(0, Math.min(bd.steps.length - 1, i));
    tab = "step";
    save();
    renderPanel();
    $("panel").scrollTop = 0;
  }

  function renderPanel() {
    var panel = $("panel");
    var keep = panel.scrollTop;
    panel.innerHTML = "";
    $("tab-step").classList.toggle("on", tab === "step");
    $("tab-chosen").classList.toggle("on", tab === "chosen");
    $("nav").hidden = tab !== "step";
    if (tab === "chosen") {
      renderChosen(panel);
    } else {
      var step = bd.steps[current];
      panel.appendChild(el("div", { "class": "step-head" }, [
        el("span", { "class": "step-n", text: "Step " + step.n }),
        el("h2", { text: step.title }),
        el("p", { "class": "lede", text: step.lede })
      ]));
      var host = el("div", { "class": "step-body" });
      panel.appendChild(host);
      step.render(host);
      $("btn-back").disabled = current === 0;
      $("btn-next").disabled = current === bd.steps.length - 1;
      $("step-of").textContent = (current + 1) + " of " + bd.steps.length;
      var next = bd.steps[current + 1];
      $("btn-next").textContent = next ? "Next: " + next.title + " →" : "Next →";
    }
    panel.scrollTop = keep;
  }

  function renderChosen(panel) {
    panel.appendChild(el("div", { "class": "step-head" }, [
      el("h2", { text: "Chosen so far" }),
      el("p", { "class": "lede", text: "Everything this breed says, step by step. Click a step to go back to it." })
    ]));
    var list = el("ol", { "class": "chosen" });
    bd.steps.forEach(function (step, i) {
      var said = step.summary();
      list.appendChild(el("li", { "class": (said ? "set" : "unset") + (i === current ? " here" : "") }, [
        el("button", { type: "button", onclick: function () { go(i); } }, [
          el("span", { "class": "step-n", text: step.n }),
          el("b", { text: step.title }),
          el("span", { "class": "said", text: said || "left to the game" })
        ])
      ]));
    });
    panel.appendChild(list);
    var v = el("div", { "class": "verdict-box", id: "verdict-long" });
    panel.appendChild(v);
    fillVerdict(v, true);
    var det = el("details", { "class": "json" }, [el("summary", { text: "The file as it stands" }),
      el("pre", { text: bd.toJson() })]);
    panel.appendChild(det);
  }

  // ---- the verdict: the game's own parser -------------------------------

  function renderVerdict() { fillVerdict($("verdict"), false); var long = $("verdict-long"); if (long) fillVerdict(long, true); }

  function fillVerdict(host, long) {
    var v = JSON.parse(bd.api.checkBreedJson(fileForCheck()));
    host.innerHTML = "";
    if (v.ok) {
      host.appendChild(el("span", { "class": "ok", text: long
        ? "The game will load this breed" + (bd.state.name ? "" : " (it will be named for today's date when exported)") + "."
        : "✓ loads" }));
    } else {
      host.appendChild(el("span", { "class": "err", text: "✗ " + v.error }));
    }
    if (long && v.warnings && v.warnings.length) {
      host.appendChild(el("ul", {}, v.warnings.map(function (w) { return el("li", { text: w }); })));
    }
  }

  /** The file with a placeholder id/name, so an unnamed breed still previews and verdicts. */
  function fileForCheck() {
    var s = Object.assign({}, bd.state);
    if (!s.id) s.id = "unnamed_breed";
    if (!s.name) s.name = "Unnamed breed";
    return bd.toJson(s);
  }

  // ---- import / export --------------------------------------------------

  function stamp() {
    var d = new Date();
    var p = function (n) { return (n < 10 ? "0" : "") + n; };
    return { human: d.getFullYear() + "-" + p(d.getMonth() + 1) + "-" + p(d.getDate()) + " " + p(d.getHours()) + ":" + p(d.getMinutes()),
      token: d.getFullYear() + p(d.getMonth() + 1) + p(d.getDate()) + "_" + p(d.getHours()) + p(d.getMinutes()) };
  }

  /**
   * Download the file. Any step, any time - it is how progress is kept. A breed
   * with no name yet is named for the moment it was exported, so the file
   * still loads in the game and still sorts sensibly in a folder.
   */
  function exportJson() {
    var s = bd.state;
    if (!s.name || !s.id) {
      var t = stamp();
      if (!s.name) s.name = "Breed " + t.human;
      if (!s.id) s.id = "breed_" + t.token;
      changed();
      if (tab === "step" && bd.steps[current].id === "name") renderPanel();
    }
    var blob = new Blob([bd.toJson()], { type: "application/json" });
    var a = el("a", { href: URL.createObjectURL(blob), download: s.id + ".json" });
    document.body.appendChild(a);
    a.click();
    setTimeout(function () { URL.revokeObjectURL(a.href); a.remove(); }, 1000);
  }

  function importJson(text) {
    try {
      bd.state = bd.fromJson(text);
    } catch (e) {
      alert("That file is not a breed file: " + e.message);
      return;
    }
    current = 0;
    tab = "chosen";
    changed();
    renderPanel();
  }

  // ---- the horse --------------------------------------------------------

  function ensureView() {
    if (view) return view;
    view = HG.viewport.create($("viewport"), { background: 0x0f172a, grid: false });
    if (view) view.setSkin("ADULT");
    return view;
  }

  /**
   * The example founder - BreedFounder.plate, so no stray magic the author did
   * not pick - and a strip of real founder rolls under it, which do carry the
   * wild magic draw. A breed is judged on a herd, not on one horse: "they are
   * all the same" and "one in six is a colour it should not be" are only
   * visible in several.
   */
  function renderFounders() {
    var json = fileForCheck();
    var one = JSON.parse(bd.api.breedPlateJson(json, seed));
    var info = $("founder-info");
    if (!one.ok) {
      info.textContent = "The file does not load yet: " + one.error;
      $("herd").innerHTML = "";
      return;
    }
    var v = ensureView();
    if (v) v.setImage(toImageData(bd.api.coatOf(one.genotype, one.epigenome, true)));
    var t = JSON.parse(bd.api.traitsOfJson(one.genotype, one.epigenome));
    info.innerHTML = "";
    info.appendChild(stat("Size", t.scale.toFixed(2) + "x", bd.api.handsLabel(t.scale), t.scale));
    info.appendChild(stat("Speed", "x" + (t.speed / t.baseSpeed).toFixed(2), "of an ordinary horse", t.speed / t.baseSpeed));
    info.appendChild(stat("Jump", "x" + (t.jump / t.baseJump).toFixed(2), "", t.jump / t.baseJump));
    info.appendChild(stat("Health", "x" + (t.health / t.baseHealth).toFixed(2), "", t.health / t.baseHealth));
    if (t.conditions && t.conditions.length) {
      info.appendChild(el("div", { "class": "conds", text: "Carries: " + t.conditions.map(function (c) { return c.name; }).join(", ") }));
    }

    var herd = $("herd");
    herd.innerHTML = "";
    for (var i = 1; i <= 6; i++) {
      var h = JSON.parse(bd.api.breedFounderJson(json, seed * 1013 + i));
      if (!h.ok) break;
      var c = el("canvas", { width: String(SHEET), height: String(SHEET), title: "a wild founder's coat sheet" });
      c.getContext("2d").putImageData(toImageData(bd.api.coatOf(h.genotype, h.epigenome, true)), 0, 0);
      herd.appendChild(c);
    }
  }

  /** One figure, with a bar against the ordinary horse at the middle. */
  function stat(label, value, sub, ratio) {
    var pct = Math.max(2, Math.min(100, ratio * 50));
    return el("div", { "class": "stat" }, [
      el("span", { "class": "sl", text: label }),
      el("b", { text: value }),
      el("span", { "class": "sub", text: sub }),
      el("span", { "class": "gauge", title: "the tick is an ordinary horse" }, [el("i", { style: "width:" + pct + "%" })])
    ]);
  }

  function toImageData(pixels) {
    var img = new ImageData(SHEET, SHEET);
    for (var i = 0; i < SHEET * SHEET; i++) {
      var p = pixels[i];
      img.data[i * 4] = (p >> 16) & 0xFF;
      img.data[i * 4 + 1] = (p >> 8) & 0xFF;
      img.data[i * 4 + 2] = p & 0xFF;
      img.data[i * 4 + 3] = (p >>> 24) & 0xFF;
    }
    return img;
  }

  HG.breedDesigner = { start: start };
})(window.HG);
