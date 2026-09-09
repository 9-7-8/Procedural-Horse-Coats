// Collapsible sections on the landing page.
//
// WHY IT IS DONE AT RUN TIME. index.html is half hand-written and half
// generated: everything between the BEGIN/END markers belongs to
// :common:bakeGeneWikiPages, which rewrites it whenever a gene moves. Wrapping
// each section in a <details> in the HTML would mean the generator and the hand
// written half both had to know the wrapper, and a card added by hand in the
// wrong place would silently fall outside it. So the markup stays flat - a
// .section-head followed by whatever belongs to it, up to the next
// .section-head - and this walks that shape once on load. A section added by
// either half gets the behaviour for free and no generator needs changing.
//
// WHAT OPENS SHUT. A section whose head carries data-collapsed="true" starts
// closed; the generator writes it from GeneFamily.collapsed(), which is true for
// exactly one family (the health loci - forty-odd cards with nothing to show,
// sitting between the two halves people actually came for). Everything else
// starts open.
//
// The reader's own choices win over both, and are remembered per section in
// localStorage - opening the health genes should not have to be done twice.
(function () {
  "use strict";

  var STORE = "hg-landing-sections";

  function readState() {
    try {
      return JSON.parse(localStorage.getItem(STORE)) || {};
    } catch (e) {
      return {};                 // private window, blocked storage, stale value
    }
  }

  function writeState(state) {
    try {
      localStorage.setItem(STORE, JSON.stringify(state));
    } catch (e) {
      /* not worth telling anyone about; the page works without it */
    }
  }

  function slug(head) {
    var h2 = head.querySelector("h2");
    return (h2 ? h2.textContent : "").trim().toLowerCase().replace(/[^a-z0-9]+/g, "-");
  }

  function start() {
    var main = document.querySelector(".landing-main");
    if (!main) return;
    var heads = Array.prototype.slice.call(main.querySelectorAll(".section-head"));
    if (!heads.length) return;

    var state = readState();

    heads.forEach(function (head, n) {
      // Everything from this head to the next one is the section's body.
      var body = document.createElement("div");
      body.className = "section-body";
      var node = head.nextSibling;
      while (node && !(node.nodeType === 1 && node.classList.contains("section-head"))) {
        var next = node.nextSibling;
        body.appendChild(node);
        node = next;
      }
      if (!body.childNodes.length) return;
      head.parentNode.insertBefore(body, head.nextSibling);

      var key = slug(head) || ("section-" + n);
      var id = "landing-" + key;
      body.id = id;

      // The whole head is the hit target - it is a heading and a paragraph, and
      // a small chevron on its own is a worse one.
      var toggle = document.createElement("button");
      toggle.type = "button";
      toggle.className = "section-toggle";
      toggle.setAttribute("aria-controls", id);
      while (head.firstChild) {
        toggle.appendChild(head.firstChild);
      }
      var chevron = document.createElement("span");
      chevron.className = "section-chevron";
      chevron.setAttribute("aria-hidden", "true");
      toggle.appendChild(chevron);
      head.appendChild(toggle);
      head.classList.add("collapsible");

      var open = Object.prototype.hasOwnProperty.call(state, key)
        ? !!state[key]
        : head.getAttribute("data-collapsed") !== "true";

      function apply(next) {
        open = next;
        body.hidden = !open;
        head.classList.toggle("shut", !open);
        toggle.setAttribute("aria-expanded", open ? "true" : "false");
      }

      apply(open);

      toggle.addEventListener("click", function () {
        apply(!open);
        state[key] = open;
        writeState(state);
      });
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})();
