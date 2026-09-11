// The breed designer's core: the file being edited, the gene card, the hover card.
//
// NOTHING GENETIC LIVES HERE. Every gene, allele pair, outcome, epigenetic
// schema, founder and verdict comes out of common/ compiled to WebAssembly
// (../horse-designer/js/java.js) - the same parser, registry and founder roll
// the game runs. This file is a form. When the page says a file is good, the
// game's own BreedSpecParser said so; when it draws a founder, BreedFounder
// rolled it.
//
// THE STATE IS THE FILE. HG.bd.state is exactly the JSON object a breed file
// holds - the same keys, the same shapes - and toJson() spells it rather than
// translating it. A field nobody has touched is ABSENT, which is what makes
// "leave it and the game decides" true: an untouched step writes nothing.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var bd = HG.bd = HG.bd || {};

  bd.api = null;
  bd.genes = [];
  bd.geneByKey = {};
  bd.facts = null;          // assets/marking-facts.json - measured per outcome
  bd.state = null;
  bd.onChange = function () {};   // the app hangs the preview and autosave here

  // ---- the file ---------------------------------------------------------

  bd.blank = function () {
    return {
      id: "", name: "", description: "",
      kind: undefined, commonness: undefined, spawn: undefined, spawn_time: undefined,
      biomes: [], price: undefined, hardy: undefined, magic_chance: undefined,
      magic_whitelist: [], magic_blacklist: [],
      stats: {}, genes: {}, bands: {}, notes: [],
      // Not in the file: the base step's colour weights, so reopening the step
      // shows the sliders where they were left. Recomputed from the pools on
      // import - see steps.base.
      _base: null
    };
  };

  /** Field order is BreedSpecWriter's, so a file from here diffs cleanly against a baked one. */
  bd.toJson = function (s) {
    s = s || bd.state;
    var out = {};
    out.id = s.id || "";
    out.name = s.name || "";
    if (s.description) out.description = s.description;
    var kind = s.kind === "auto" || !s.kind ? (bd.anyMagical(s) ? "magical" : undefined) : s.kind;
    if (kind && kind !== "natural") out.kind = kind;
    if (s.commonness) out.commonness = s.commonness;
    if (s.spawn) out.spawn = s.spawn;
    if (s.spawn_time && s.spawn_time !== "any") out.spawn_time = s.spawn_time;
    if (s.biomes.length) out.biomes = s.biomes;
    if (s.price) out.price = s.price;
    if (s.hardy) out.hardy = true;
    if (s.magic_chance !== undefined && !isNaN(s.magic_chance)) out.magic_chance = s.magic_chance;
    if (s.magic_whitelist.length) out.magic_whitelist = s.magic_whitelist;
    if (s.magic_blacklist.length) out.magic_blacklist = s.magic_blacklist;
    if (Object.keys(s.stats).length) out.stats = s.stats;
    var pools = {};
    Object.keys(s.genes).forEach(function (k) { if (s.genes[k].length) pools[k] = s.genes[k]; });
    if (Object.keys(pools).length) out.genes = pools;
    var bands = {};
    Object.keys(s.bands).forEach(function (k) { if (Object.keys(s.bands[k]).length) bands[k] = s.bands[k]; });
    if (Object.keys(bands).length) out.bands = bands;
    if (s.notes.length) out.notes = s.notes;
    return JSON.stringify(out, null, 2) + "\n";
  };

  /** Any magical gene named in a pool - what "kind: auto" resolves to. */
  bd.anyMagical = function (s) {
    return Object.keys(s.genes).some(function (k) {
      var g = bd.geneByKey[k];
      return g && !g.natural && s.genes[k].length && BODY_STATS.indexOf(k) < 0;
    });
  };

  bd.fromJson = function (text) {
    var parsed = JSON.parse(text);
    var s = bd.blank();
    ["id", "name", "description", "kind", "commonness", "spawn", "spawn_time", "price", "hardy",
      "magic_chance"].forEach(function (k) { if (parsed[k] !== undefined) s[k] = parsed[k]; });
    s.biomes = parsed.biomes || [];
    s.magic_whitelist = parsed.magic_whitelist || [];
    s.magic_blacklist = parsed.magic_blacklist || [];
    s.stats = parsed.stats || {};
    s.genes = parsed.genes || {};
    s.bands = parsed.bands || {};
    s.notes = parsed.notes || [];
    return s;
  };

  /** "the file changed" - re-verdict, re-roll, autosave. Debounced by the app. */
  bd.changed = function () { bd.onChange(); };

  // ---- gene groups ------------------------------------------------------
  //
  // Which step offers which gene. The families come from the registry
  // (GeneFamily, the same grouping the gene editors' filter uses); the two
  // lists below are for the steps no family describes. A key this install
  // has not got is simply not offered.

  var BODY_STATS = bd.BODY_STATS = ["horsegenetics.body_size", "horsegenetics.magic_speed",
    "horsegenetics.magic_health", "horsegenetics.magic_jump"];
  bd.BASE_KEYS = ["horsegenetics.extension", "horsegenetics.agouti", "horsegenetics.shade"];
  bd.DIET_KEYS = ["horsegenetics.diet", "horsegenetics.food_preference"];
  bd.AGGRO_KEYS = ["horsegenetics.magic_mob_aura", "horsegenetics.magic_night_temper",
    "horsegenetics.magic_night_watch", "horsegenetics.lycan", "horsegenetics.intimidating",
    "horsegenetics.gladiator", "horsegenetics.guardian", "horsegenetics.pack_leader",
    "horsegenetics.magic_fighter", "horsegenetics.holy_ward", "horsegenetics.dhampir"];

  bd.isEye = function (g) {
    return g.key !== "horsegenetics.eyesight"
      && /\.(eye_|tiger_eye$|magic_sectoral_heterochromia$)/.test(g.key);
  };

  bd.present = function (keys) {
    return keys.map(function (k) { return bd.geneByKey[k]; }).filter(Boolean);
  };

  // ---- small DOM helpers ------------------------------------------------

  bd.el = function (tag, attrs, children) {
    var e = document.createElement(tag);
    if (attrs) Object.keys(attrs).forEach(function (k) {
      if (k === "text") e.textContent = attrs[k];
      else if (k === "html") e.innerHTML = attrs[k];
      else if (k.slice(0, 2) === "on") e.addEventListener(k.slice(2), attrs[k]);
      else if (attrs[k] !== undefined && attrs[k] !== null && attrs[k] !== false) e.setAttribute(k, attrs[k]);
    });
    (children || []).forEach(function (c) {
      if (c === null || c === undefined) return;
      e.appendChild(typeof c === "string" ? document.createTextNode(c) : c);
    });
    return e;
  };

  bd.esc = function (s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  };

  bd.round = function (n) {
    return Math.abs(n) >= 100 ? String(Math.round(n)) : String(Math.round(n * 1000) / 1000);
  };

  // ---- api caches -------------------------------------------------------

  var pairsCache = {}, schemaCache = {};
  bd.pairsOf = function (key) {
    if (!pairsCache[key]) pairsCache[key] = JSON.parse(bd.api.allelePairsJson(key));
    return pairsCache[key];
  };
  bd.schemaOf = function (key) {
    if (!schemaCache[key]) schemaCache[key] = JSON.parse(bd.api.epiSchemaJson(key));
    return schemaCache[key];
  };

  // ---- the gene card ----------------------------------------------------
  //
  // One gene, as the breed will carry it. Off: the breed does not name it, and
  // the game rolls it the way it rolls every unnamed gene (coat genes wild,
  // the rest at their global rates). On: exactly the pairs ticked, at the
  // weights beside them, and - under "fine-tune" - the ranges and locks the
  // founders' epigenetic numbers are drawn inside.

  /**
   * @param key    gene key
   * @param opts   { epi: false to hide the numbers (the stat steps), open: start expanded,
   *                 note: a line under the heading }
   */
  bd.geneCard = function (key, opts) {
    opts = opts || {};
    var g = bd.geneByKey[key];
    var s = bd.state;
    var on = !!(s.genes[key] && s.genes[key].length);
    var card = bd.el("div", { "class": "gene-card" + (on ? " on" : "") });

    var head = bd.el("div", { "class": "gc-head" });
    var toggle = bd.el("input", { type: "checkbox" });
    toggle.checked = on;
    toggle.addEventListener("change", function () {
      if (toggle.checked) {
        s.genes[key] = defaultPool(key);
      } else {
        delete s.genes[key];
        delete s.bands[key];
      }
      bd.changed();
      bd.rerender();
    });
    head.appendChild(bd.el("label", { "class": "gc-toggle" }, [toggle,
      bd.el("span", { "class": "gc-name", "data-gene": key, text: g ? g.name : key })]));
    head.appendChild(bd.el("span", { "class": "gc-state", text: on ? "in this breed" : "not named - " + unnamedMeans(g) }));
    card.appendChild(head);
    if (opts.note) card.appendChild(bd.el("p", { "class": "gc-note", text: opts.note }));
    if (!on) return card;

    card.appendChild(pairGrid(key));
    if (opts.epi !== false && bd.schemaOf(key).length) card.appendChild(epiPanel(key));
    return card;
  };

  function unnamedMeans(g) {
    if (!g) return "not installed";
    if (g.influencesCoat || g.paints) return "rolled wild, so it never shows";
    return "left at its wild rate";
  }

  /** Ticking a gene on: every pair that shows something, at equal weights. */
  function defaultPool(key) {
    var pairs = bd.pairsOf(key).filter(function (p) { return !p.wild; });
    if (!pairs.length) pairs = bd.pairsOf(key);
    return pairs.map(function (p) { return { pair: p.pair, weight: 10 }; });
  }

  function pairGrid(key) {
    var s = bd.state;
    var wrap = bd.el("div", { "class": "pairs" });
    var pool = s.genes[key];
    var total = pool.reduce(function (a, c) { return a + (Number(c.weight) || 0); }, 0);
    var inPool = {};
    pool.forEach(function (c) { inPool[normal(c.pair)] = c; });

    bd.pairsOf(key).forEach(function (p) {
      var c = inPool[normal(p.pair)];
      var row = bd.el("label", { "class": "pair-row" + (c ? " on" : "") + (p.wild ? " wild" : "") });
      var box = bd.el("input", { type: "checkbox" });
      box.checked = !!c;
      box.addEventListener("change", function () {
        if (box.checked) pool.push({ pair: p.pair, weight: 10 });
        else {
          var at = pool.indexOf(inPool[normal(p.pair)]);
          if (at >= 0) pool.splice(at, 1);
        }
        if (!pool.length) { delete s.genes[key]; delete s.bands[key]; }
        bd.changed();
        bd.rerender();
      });
      row.appendChild(box);
      row.appendChild(bd.el("code", { text: p.pair }));
      row.appendChild(bd.el("span", { "class": "zyg", text: p.homozygous ? "homozygous" : "heterozygous" }));
      row.appendChild(bd.el("span", { "class": "out", text: p.wild ? "wild type - shows nothing" : p.outcomeName }));
      if (c) {
        var w = bd.el("input", { type: "number", min: "0", step: "any", value: String(c.weight), "class": "w" });
        w.addEventListener("click", function (e) { e.preventDefault(); });
        w.addEventListener("input", function () {
          c.weight = Math.max(0, Number(w.value) || 0);
          share.textContent = pct(c.weight);
          bd.changed();
        });
        var share = bd.el("span", { "class": "share", text: pct(c.weight) });
        row.appendChild(w);
        row.appendChild(share);
      }
      wrap.appendChild(row);
    });

    function pct(wt) {
      var t = pool.reduce(function (a, c) { return a + (Number(c.weight) || 0); }, 0);
      return t > 0 ? (100 * (Number(wt) || 0) / t).toFixed(0) + "%" : "-";
    }

    var quick = bd.el("div", { "class": "quick" }, [
      qb("every pair", function () {
        pool.length = 0;
        bd.pairsOf(key).forEach(function (p) { pool.push({ pair: p.pair, weight: 10 }); });
      }),
      qb("only homozygous", function () { keep(function (p) { return p.homozygous; }); }),
      qb("only heterozygous", function () { keep(function (p) { return !p.homozygous; }); }),
      qb("equal weights", function () { pool.forEach(function (c) { c.weight = 10; }); }),
      qb("random weights", function () { pool.forEach(function (c) { c.weight = 1 + Math.round(Math.random() * 19); }); })
    ]);
    function keep(test) {
      var byPair = {};
      bd.pairsOf(key).forEach(function (p) { byPair[normal(p.pair)] = p; });
      var kept = pool.filter(function (c) { var p = byPair[normal(c.pair)]; return p && test(p); });
      if (!kept.length) {
        kept = bd.pairsOf(key).filter(test).map(function (p) { return { pair: p.pair, weight: 10 }; });
      }
      pool.length = 0;
      kept.forEach(function (c) { pool.push(c); });
    }
    function qb(label, fn) {
      return bd.el("button", { type: "button", "class": "btn tiny", text: label,
        onclick: function () { fn(); if (!pool.length) delete s.genes[key]; bd.changed(); bd.rerender(); } });
    }
    wrap.appendChild(quick);
    if (total <= 0) wrap.appendChild(bd.el("p", { "class": "warn", text: "Every weight is zero - the game will drop the pool and roll this gene wild." }));
    return wrap;
  }

  /** "e/E" and "E/e" are one pair; the grid and the file may spell it either way. */
  function normal(pair) {
    var t = String(pair).split("/");
    return t.slice().sort().join("/");
  }

  // ---- epigenetic ranges and locks --------------------------------------

  function epiPanel(key) {
    var s = bd.state;
    var schema = bd.schemaOf(key);
    var det = bd.el("details", { "class": "epi" });
    var pinned = s.bands[key] ? Object.keys(s.bands[key]).length : 0;
    det.appendChild(bd.el("summary", { text: "Fine-tune the numbers" + (pinned ? " (" + pinned + " set)" : "") }));
    det.appendChild(bd.el("p", { "class": "hint", text:
      "Not which alleles but how much: how far a pattern spreads, how dark a shade, which noise a " +
      "pattern is drawn from. Founders are given a value inside each range on both copies; after " +
      "that it inherits and drifts. Lock a value to give every founder exactly that number - for a " +
      "seed, that means every founder carries the same pattern." }));
    if (pinned) det.open = true;

    schema.forEach(function (v) {
      var bands = s.bands[key] || {};
      var cur = bands[v.name];
      var row = bd.el("div", { "class": "epi-row" });
      row.appendChild(bd.el("span", { "class": "nm", text: v.name + (v.arity > 1 ? " (each leg)" : "") }));

      if (v.kind === "seed") {
        var seedBox = bd.el("input", { type: "text", placeholder: "seed - random for every founder",
          value: cur === undefined ? "" : String(cur), "class": "seed" });
        seedBox.addEventListener("change", function () {
          var t = seedBox.value.trim();
          setBand(key, v.name, /^-?\d+$/.test(t) ? t : undefined);
        });
        row.appendChild(seedBox);
        row.appendChild(bd.el("button", { type: "button", "class": "btn tiny", text: "lock a random seed",
          onclick: function () {
            // A 53-bit whole number, written as a string - JSON numbers are
            // doubles, and the parser reads a string of digits exactly.
            var n = Math.floor(Math.random() * 9007199254740991) * (Math.random() < 0.5 ? -1 : 1);
            setBand(key, v.name, String(n));
            bd.rerender();
          } }));
        det.appendChild(row);
        return;
      }

      var isCat = v.kind === "category";
      var lo = cur === undefined ? "" : (Array.isArray(cur) ? cur[0] : cur);
      var hi = cur === undefined ? "" : (Array.isArray(cur) ? cur[1] : cur);
      var locked = cur !== undefined && !Array.isArray(cur);
      var loBox = num(lo, isCat ? "0" : bd.round(v.min));
      var hiBox = num(hi, isCat ? String(v.max - 1) : bd.round(v.max));
      var lock = bd.el("input", { type: "checkbox", title: "lock to a single value" });
      lock.checked = locked;
      hiBox.disabled = locked;

      function commit() {
        var a = loBox.value.trim(), b = hiBox.value.trim();
        if (lock.checked) setBand(key, v.name, a === "" ? undefined : Number(a));
        else setBand(key, v.name, a === "" || b === "" ? undefined : [Number(a), Number(b)]);
      }
      loBox.addEventListener("input", commit);
      hiBox.addEventListener("input", commit);
      lock.addEventListener("change", function () { hiBox.disabled = lock.checked; commit(); });

      row.appendChild(loBox);
      row.appendChild(bd.el("span", { "class": "dash", text: "to" }));
      row.appendChild(hiBox);
      row.appendChild(bd.el("label", { "class": "lock" }, [lock, " lock"]));
      row.appendChild(bd.el("span", { "class": "rng", text: isCat
        ? "option 0-" + (v.max - 1)
        : "wild " + bd.round(v.min) + "-" + bd.round(v.max) }));
      det.appendChild(row);
    });
    return det;

    function num(value, placeholder) {
      return bd.el("input", { type: "number", step: "any", value: value === "" ? "" : String(value),
        placeholder: placeholder });
    }
  }

  function setBand(key, name, value) {
    var s = bd.state;
    if (value === undefined || (typeof value === "number" && isNaN(value))) {
      if (s.bands[key]) {
        delete s.bands[key][name];
        if (!Object.keys(s.bands[key]).length) delete s.bands[key];
      }
    } else {
      s.bands[key] = s.bands[key] || {};
      s.bands[key][name] = value;
    }
    bd.changed();
  }

  // ---- the hover card ---------------------------------------------------
  //
  // Anything carrying data-gene shows the gene's blurb on hover, with its
  // baked icon floated left so the text wraps round it - the same card the
  // horse designer and the spawn egg draw.

  bd.installHoverCard = function () {
    var pop = bd.el("div", { "class": "gene-pop", hidden: "hidden" });
    document.body.appendChild(pop);
    var current = null;
    document.addEventListener("mouseover", function (e) {
      var t = e.target.closest ? e.target.closest("[data-gene]") : null;
      if (!t) { if (current) { pop.hidden = true; current = null; } return; }
      var key = t.getAttribute("data-gene");
      if (key === current) return;
      current = key;
      var g = bd.geneByKey[key];
      if (!g || !g.description) { pop.hidden = true; return; }
      pop.innerHTML = "";
      var img = bd.el("img", { src: HG.geneIcons.url(key), alt: "" });
      img.onerror = function () { img.remove(); };
      pop.appendChild(img);
      pop.appendChild(bd.el("h4", { text: g.name }));
      pop.appendChild(bd.el("p", { text: g.description }));
      pop.hidden = false;
      place(e);
    });
    document.addEventListener("mousemove", function (e) { if (!pop.hidden) place(e); });
    function place(e) {
      var w = pop.offsetWidth, h = pop.offsetHeight;
      var x = e.clientX + 16, y = e.clientY + 14;
      if (x + w > innerWidth - 8) x = e.clientX - w - 16;
      if (y + h > innerHeight - 8) y = innerHeight - h - 8;
      pop.style.left = Math.max(8, x) + "px";
      pop.style.top = Math.max(8, y) + "px";
    }
  };

  bd.rerender = function () {};   // the app sets this to "redraw the open step"
})(window.HG);
