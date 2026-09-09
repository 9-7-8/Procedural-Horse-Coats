// The paint order, top to bottom: every gene that paints the coat, stacked in
// the order the composer applies it, with the last thing painted at the top.
//
// USING IT. One line on the page:
//
//     <div class="paint-order"></div>
//
// Everything in it is asked of the mod at run time - DesignerApi.genesJson
// carries each gene's key, name, priority, phase and whether it paints at all -
// so a gene that moves in the order moves here, and a page that would otherwise
// be a hand-maintained list of a hundred and fifty priorities cannot go stale.
// It is the same wasm the gene preview and the horse designer run.
//
// WHY IT READS DOWNWARD. Priority is a paint order: a higher number is applied
// later and therefore lands ON TOP. Drawn as a list that reads high-to-low, the
// page is a picture of the stack - what covers what - which is the question the
// number is actually asked. Reading it the other way up would put agouti, the
// thing everything else is painted over, at the top of the page.
//
// The icons are the baked ones from :common:bakeGeneIcons - the same picture
// the landing page's card uses. A gene with no icon is a gene the icon baker
// could not photograph, which for a coat gene is worth seeing rather than
// hiding, so it gets a plain slot with the reason on it.
//
// Needs the wiki over http, like every other wasm-backed widget here.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var HERE = (function () {
    var self = document.currentScript;
    if (!self || !self.src) return "";
    return self.src.replace(/paint-order\.js(\?.*)?$/, "");
  })();
  var WIKI = HERE + "../";

  function script(src) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement("script");
      s.src = src;
      s.onload = resolve;
      s.onerror = function () { reject(new Error("could not load " + src)); };
      document.head.appendChild(s);
    });
  }

  var booted = null;
  function boot() {
    if (booted) return booted;
    booted = Promise.resolve()
      .then(function () { return HG.java ? null : script(WIKI + "horse-designer/js/java.js"); })
      .then(function () { return HG.java.load(); });
    return booted;
  }

  function esc(s) {
    return String(s == null ? "" : s).replace(/[&<>"]/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
    });
  }

  /** The gene's slug, the way GeneIconTool and GeneWikiTool both spell it. */
  function slug(key) {
    return key.substring(key.indexOf(".") + 1);
  }

  function pageOf(key) {
    return "gene-" + slug(key).replace(/_/g, "-") + ".html";
  }

  /** Which gene pages actually exist, off the wiki's own manifest. */
  function knownPages() {
    var have = {};
    var sections = (HG.pages && HG.pages.SECTIONS) || [];
    sections.forEach(function (s) {
      (s.items || []).forEach(function (i) { have[i.href.split("?")[0]] = true; });
    });
    return have;
  }

  /**
   * Which genes have a baked icon. index.txt is written by the icon baker
   * alongside the PNGs, so asking it is cheaper and quieter than firing a
   * hundred and fifty image requests and watching two-thirds of them 404.
   */
  function icons() {
    return fetch(WIKI + "assets/gene-icons/index.txt")
      .then(function (r) { return r.ok ? r.text() : ""; })
      .then(function (text) {
        var have = {};
        text.split(/\r?\n/).forEach(function (line) {
          if (line) have[line.trim()] = true;
        });
        return have;
      }, function () { return {}; });
  }

  function row(gene, hasIcon, hasPage) {
    var name = esc(gene.name);
    var inner = '<div class="po-shot">'
      + (hasIcon
        ? '<img src="' + WIKI + 'assets/gene-icons/' + esc(slug(gene.key))
          + '.png" alt="' + name + ' on the icon baker\'s standard horse"'
          + ' width="96" loading="lazy">'
        : '<span class="po-noshot" title="the icon baker could not photograph this one">'
          + "no icon</span>")
      + "</div>"
      + '<div class="po-text"><span class="po-name">' + name + "</span>"
      + '<span class="po-meta"><code>' + gene.priority + "</code> &middot; "
      + (gene.natural ? "natural &mdash; phase 1" : "magical &mdash; phase 3")
      + "</span></div>";

    return '<li class="po-row ' + (gene.natural ? "natural" : "magical") + '">'
      + (hasPage
        ? '<a href="' + WIKI + esc(pageOf(gene.key)) + '">' + inner + "</a>"
        : '<div class="po-nolink">' + inner + "</div>")
      + "</li>";
  }

  function draw(host, genes, iconIndex) {
    var pages = knownPages();
    // Highest first: the top of this page is the top of the stack.
    var paint = genes.filter(function (g) { return g.paints; })
      .sort(function (a, b) {
        return b.priority - a.priority || a.key.localeCompare(b.key);
      });

    var magical = paint.filter(function (g) { return !g.natural; }).length;

    var h = '<p class="po-count">' + paint.length + " genes paint the coat &mdash; "
      + magical + " magical, " + (paint.length - magical)
      + " natural. Everything else the horse carries is either invisible or"
      + " not a coat gene at all, and is left out.</p>";

    h += '<ol class="po-list">';
    var lastPhase = null;
    paint.forEach(function (g) {
      var phase = g.natural ? "natural" : "magical";
      if (phase !== lastPhase) {
        h += '<li class="po-divider ' + phase + '"><span>'
          + (phase === "magical"
            ? "Phase 3 &mdash; magical, painted over the resolved colour"
            : "Phase 2 resolves the colour here &middot; below, phase 1 &mdash; "
              + "natural, restricting pigment")
          + "</span></li>";
        lastPhase = phase;
      }
      h += row(g, !!iconIndex[slug(g.key)], !!pages[pageOf(g.key)]);
    });
    h += "</ol>";
    host.innerHTML = h;
  }

  function mount(host) {
    host.classList.add("paint-order");
    host.innerHTML = '<div class="po-boot">Asking the mod for the paint order&hellip;</div>';

    Promise.all([boot(), icons()]).then(function (both) {
      draw(host, JSON.parse(both[0].genesJson()), both[1]);
    }, function (err) {
      host.innerHTML = '<div class="po-boot po-failed">The paint order could not load: '
        + esc(err.message) + "."
        + (location.protocol === "file:"
          ? " This page is open from a file:// path, and asking the mod is a fetch -"
          + " serve the repo with any static server, or read it on the published wiki."
          : "")
        + "</div>";
    });
  }

  function start() {
    Array.prototype.forEach.call(document.querySelectorAll(".paint-order"), mount);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})(window.HG);
