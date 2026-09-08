// The preview window that sits on every gene page: this gene, on a bay, a
// black and a chestnut, as a horse you can spin. It opens on the bay.
//
// WHAT IS NEW HERE, AND WHAT IS NOT. Almost nothing genetic is. The coat comes
// out of common/ compiled to WebAssembly (wiki/horse-designer/js/java.js - the
// same wasm the horse designer runs), the horse is built by the gene creator's
// mesh emitter (gene-creator/js/model3d.js) off the game's own geometry tables,
// and it is shown in the gene creator's own orbit viewport. What this file adds
// is the three buttons, the dice, and the wiring between them, which is the
// only part that is actually about previewing one gene.
//
// That is deliberate. A gene page must not hold a second opinion about what a
// tobiano looks like (hard rule 3): it asks the mod, the way the designer does,
// and gets back the bytes the game bakes.
//
// USING IT. Two lines on a gene page, anywhere in the article:
//
//     <div class="gene-preview" data-gene="horsegenetics.tobiano"></div>
//     <script defer src="gene-preview/gene-preview.js"></script>
//
// The key is the gene's Gene#key(). Everything else - the gene's name, which
// allele combinations are worth a button, what each one is called, whether it
// resolves a condition - is asked of the registry at run time, so a gene that
// gains an allele gains a button and no page is edited. A key that no longer
// resolves says so in the window rather than drawing a plain horse and calling
// it the gene.
//
// It needs the wiki served over http: loading the wasm is a fetch, and a file://
// page is its own opaque origin. The window says so rather than failing silently.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // This file's own directory, and the wiki root above it. Derived from the
  // script's src for the same reason java.js derives its asset paths that way:
  // the including page is a different page every time.
  var HERE = (function () {
    var self = document.currentScript;
    if (!self || !self.src) return "";
    return self.src.replace(/gene-preview\.js(\?.*)?$/, "");
  })();
  var WIKI = HERE + "../";

  var THREE_CDN = "https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js";
  var ORBIT_CDN = "https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js";

  var SHEET = 128;   // replaced by the mod's own sheetSize() once it is up

  // ---- loading ---------------------------------------------------------

  function script(src) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement("script");
      s.src = src;
      s.onload = resolve;
      s.onerror = function () { reject(new Error("could not load " + src)); };
      document.head.appendChild(s);
    });
  }

  function stylesheet(href) {
    var link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = href;
    document.head.appendChild(link);
  }

  /**
   * Bring up everything the window needs - once per page, however many previews
   * are on it. Strictly in order: model3d and viewport both read HG.geometry at
   * definition time, and OrbitControls attaches itself to THREE.
   */
  var booted = null;
  function boot() {
    if (booted) return booted;
    booted = Promise.resolve()
      .then(function () { return window.THREE ? null : script(THREE_CDN); })
      .then(function () { return window.THREE.OrbitControls ? null : script(ORBIT_CDN); })
      .then(function () { return HG.geometry ? null : script(WIKI + "gene-creator/js/geometry.js"); })
      .then(function () { return HG.model3d ? null : script(WIKI + "gene-creator/js/model3d.js"); })
      .then(function () { return HG.viewport ? null : script(WIKI + "gene-creator/js/viewport.js"); })
      .then(function () { return HG.java ? null : script(WIKI + "horse-designer/js/java.js"); })
      .then(function () { return HG.java.load(); })
      .then(function (api) {
        SHEET = api.sheetSize();
        return api;
      });
    return booted;
  }

  // ---- one window ------------------------------------------------------

  var DICE = '<svg viewBox="0 0 24 24" aria-hidden="true">'
    + '<rect x="3" y="3" width="18" height="18" rx="4" fill="none" stroke="currentColor" stroke-width="1.7"/>'
    + '<circle cx="8" cy="8" r="1.6" fill="currentColor"/>'
    + '<circle cx="16" cy="8" r="1.6" fill="currentColor"/>'
    + '<circle cx="12" cy="12" r="1.6" fill="currentColor"/>'
    + '<circle cx="8" cy="16" r="1.6" fill="currentColor"/>'
    + '<circle cx="16" cy="16" r="1.6" fill="currentColor"/></svg>';

  function mount(host) {
    var geneKey = host.getAttribute("data-gene");
    host.classList.add("gene-preview");
    host.innerHTML = '<div class="gp-boot">Loading the coat pipeline&hellip;</div>';

    boot().then(function (api) {
      build(host, api, geneKey);
    }, function (err) {
      host.innerHTML = '<div class="gp-boot gp-failed">'
        + 'The preview could not start: ' + escapeHtml(err.message) + '.'
        + (location.protocol === "file:"
          ? ' This page is open from a file:// path, and loading the mod is a fetch -'
          + ' serve the repo with any static server, or read it on the published wiki.'
          : '')
        + '</div>';
    });
  }

  function build(host, api, geneKey) {
    var gene = JSON.parse(api.genePreviewJson(geneKey));
    if (gene.missing) {
      host.innerHTML = '<div class="gp-boot gp-failed">No gene is registered as <code>'
        + escapeHtml(geneKey) + '</code> - the page is naming a key the mod no longer has.</div>';
      return;
    }
    var bases = JSON.parse(api.baseCoatsJson());

    host.innerHTML = ''
      + '<div class="gp-bar">'
      + '<div class="gp-group gp-bases" role="group" aria-label="Base coat"></div>'
      + '<div class="gp-group gp-outcomes" role="group" aria-label="Outcome"></div>'
      + '<button type="button" class="gp-dice" title="Reroll this horse&rsquo;s epigenetics"'
      + ' aria-label="Reroll epigenetics">' + DICE + '</button>'
      + '</div>'
      + (gene.modifiers.length ? '<div class="gp-mods"></div>' : "")
      + '<div class="gp-stage"><div class="gp-view"></div></div>'
      + '<div class="gp-foot">'
      + '<span class="gp-conditions"></span>'
      + '<code class="gp-code"></code>'
      + '<span class="gp-hint">drag to spin &middot; scroll to zoom</span>'
      + '</div>';

    var view = HG.viewport.create(host.querySelector(".gp-view"), {
      // The wiki's own --bg-soft. The creator's near-black is right on a tool
      // and too heavy inside an article.
      background: 0x0f172a,
      grid: false
    });
    if (!view) {
      host.innerHTML = '<div class="gp-boot gp-failed">This browser has no usable WebGL,'
        + ' so there is nothing to draw the horse with.</div>';
      return;
    }
    if (view.controls) {
      view.controls.enablePan = false;      // an article is not a workspace
      view.controls.minDistance = 1.4;
      view.controls.maxDistance = 14;
    }
    view.setSkin("ADULT");

    var state = {
      base: defaultBase(bases),
      outcome: 0,
      // One selected option per modifier locus this gene reads, all starting at
      // the baseline - so the first thing shown is the gene on its own.
      modifiers: gene.modifiers.map(function () { return 0; }),
      epigenome: api.newEpigenomeCode()
    };

    var baseButtons = buttons(host.querySelector(".gp-bases"), bases.map(function (b) {
      return { label: b.name, title: b.name + " base coat" };
    }), function (i) {
      state.base = bases[i].key;
      render();
    });

    // One button per outcome that looks different, which the mod worked out -
    // tobiano has one (To/to and To/To paint the same), KIT has one per white.
    // A single outcome needs no choice, only a label; none at all means the
    // page asked for a window on a gene that paints nothing.
    var outcomeRow = host.querySelector(".gp-outcomes");
    var outcomeButtons = null;
    if (gene.outcomes.length > 1) {
      outcomeButtons = buttons(outcomeRow, gene.outcomes.map(function (o) {
        return { label: o.name, title: o.tokens + (o.description ? " - " + o.description : "") };
      }), function (i) {
        state.outcome = i;
        render();
      });
    } else if (gene.outcomes.length === 1) {
      outcomeRow.innerHTML = '<span class="gp-single" title="'
        + escapeHtml(gene.outcomes[0].description || "") + '">'
        + escapeHtml(gene.outcomes[0].name) + ' <em>'
        + escapeHtml(gene.outcomes[0].tokens) + '</em></span>';
    } else {
      outcomeRow.innerHTML = '<span class="gp-single">' + escapeHtml(gene.name)
        + ' paints nothing &mdash; this is the base coat alone</span>';
    }

    // A gene that reads other loci (the leopard complex reads PATN1 and PATN2)
    // gets one row per locus. Without them a leopard preview would show one of
    // its eight patterns and call it the gene - see genePreviewJson.modifiers.
    var modButtons = gene.modifiers.map(function (mod, m) {
      var row = document.createElement("div");
      row.className = "gp-mod";
      row.innerHTML = '<span class="gp-modname">' + escapeHtml(mod.name) + '</span>';
      host.querySelector(".gp-mods").appendChild(row);
      return buttons(row, mod.options.map(function (o) {
        return { label: o.baseline ? "off" : o.tokens, title: mod.name + " " + o.tokens };
      }), function (i) {
        state.modifiers[m] = i;
        render();
      });
    });

    host.querySelector(".gp-dice").addEventListener("click", function () {
      state.epigenome = api.newEpigenomeCode();
      render();
    });

    var codeEl = host.querySelector(".gp-code");
    var condEl = host.querySelector(".gp-conditions");

    function render() {
      var tokens = gene.outcomes.length ? gene.outcomes[state.outcome].tokens : "";
      var code = api.previewGenotypeCode(state.base, gene.key, tokens);
      var extras = [];
      gene.modifiers.forEach(function (mod, m) {
        var option = mod.options[state.modifiers[m]];
        if (!code) return;
        code = api.withGene(code, mod.key, option.tokens);
        if (!option.baseline) extras.push(option.tokens);
      });
      if (!code) {
        codeEl.textContent = "that combination did not resolve";
        return;
      }
      view.setImage(toImageData(api.coatOf(code, state.epigenome, true)));

      var baseIndex = indexOfBase(bases, state.base);
      codeEl.textContent = [bases[baseIndex].name, tokens]
        .concat(extras).filter(Boolean).join(" · ");

      // Conditions, because a preview window is also where a reader meets the
      // homozygote that kills the foal - EDNRB's lethal white, KIT's nonviable
      // dominant whites. Asked of HorseTraits; never listed here.
      var traits = JSON.parse(api.traitsOfJson(code, state.epigenome));
      condEl.innerHTML = traits.conditions.map(function (c) {
        return '<span class="gp-cond gp-' + escapeHtml(c.severity.toLowerCase()) + '">'
          + escapeHtml(c.name) + '</span>';
      }).join("");

      baseButtons.select(baseIndex);
      if (outcomeButtons) outcomeButtons.select(state.outcome);
      modButtons.forEach(function (row, m) { row.select(state.modifiers[m]); });
    }

    render();
  }

  /**
   * Which base coat a gene page opens on: BAY, by name.
   *
   * BaseCoats.all() puts bay first for exactly this reason, so this and the
   * plain "take the first one" other callers use now agree. It still asks by
   * name rather than taking index 0, because the reason bay is the right
   * default belongs next to the page that depends on it: a black horse hides
   * every dark marking on it, and a great many of these genes paint dark.
   */
  function defaultBase(bases) {
    for (var i = 0; i < bases.length; i++) {
      if (bases[i].key === "bay") return bases[i].key;
    }
    return bases[0].key;
  }

  function indexOfBase(bases, key) {
    for (var i = 0; i < bases.length; i++) {
      if (bases[i].key === key) return i;
    }
    return 0;
  }

  /** A row of mutually exclusive buttons, with the pressed state on the DOM. */
  function buttons(container, items, onPick) {
    var els = items.map(function (item, i) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "gp-btn";
      b.textContent = item.label;
      if (item.title) b.title = item.title;
      b.addEventListener("click", function () { onPick(i); });
      container.appendChild(b);
      return b;
    });
    return {
      select: function (index) {
        els.forEach(function (b, i) {
          b.classList.toggle("is-on", i === index);
          b.setAttribute("aria-pressed", i === index ? "true" : "false");
        });
      }
    };
  }

  /** The pipeline's ARGB ints as pixels three.js can upload. */
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

  function escapeHtml(s) {
    return String(s).replace(/[&<>"]/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
    });
  }

  function start() {
    var hosts = document.querySelectorAll(".gene-preview[data-gene]");
    if (!hosts.length) return;
    stylesheet(HERE + "gene-preview.css");
    Array.prototype.forEach.call(hosts, mount);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }

  HG.genePreview = { mount: mount };
})(window.HG);
