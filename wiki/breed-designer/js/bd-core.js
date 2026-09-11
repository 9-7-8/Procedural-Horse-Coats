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
// translating it.
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

  // ---- gene groups ------------------------------------------------------
  //
  // Which step offers which gene. The families come from the registry
  // (GeneFamily, the same grouping the gene editors' filter uses); the lists
  // below are for the steps no family describes. A key this install has not
  // got is simply not offered.

  var BODY_STATS = bd.BODY_STATS = ["horsegenetics.body_size", "horsegenetics.magic_speed",
    "horsegenetics.magic_health", "horsegenetics.magic_jump"];
  bd.BASE_KEYS = ["horsegenetics.extension", "horsegenetics.agouti", "horsegenetics.shade"];
  bd.DIET_KEYS = ["horsegenetics.diet", "horsegenetics.food_preference"];
  bd.AGGRO_KEYS = ["horsegenetics.magic_mob_aura", "horsegenetics.magic_night_temper",
    "horsegenetics.magic_night_watch", "horsegenetics.lycan", "horsegenetics.intimidating",
    "horsegenetics.gladiator", "horsegenetics.guardian", "horsegenetics.pack_leader",
    "horsegenetics.magic_fighter", "horsegenetics.holy_ward", "horsegenetics.dhampir"];
  // The natural eye loci every breed names from the start, at their wild type:
  // brown irises, white sclera. Champagne, cream and the white patterns still
  // change them when a horse is made (Eyes.force) - naming them only stops a
  // breed inheriting the wild population's odd eyes.
  bd.EYE_DEFAULTS = {
    "horsegenetics.eye_colour_right": "Brn", "horsegenetics.eye_colour_left": "Brn",
    "horsegenetics.eye_sclera_right": "Wht", "horsegenetics.eye_sclera_left": "Wht"
  };

  bd.isEye = function (g) {
    return g.key !== "horsegenetics.eyesight"
      && /\.(eye_|tiger_eye$|magic_sectoral_heterochromia$)/.test(g.key);
  };

  bd.present = function (keys) {
    return keys.map(function (k) { return bd.geneByKey[k]; }).filter(Boolean);
  };

  // ---- the file ---------------------------------------------------------

  bd.blank = function () {
    var genes = {};
    Object.keys(bd.EYE_DEFAULTS).forEach(function (k) {
      var t = bd.EYE_DEFAULTS[k];
      genes[k] = [{ pair: t + "/" + t, weight: 1 }];
    });
    return {
      id: "", name: "", description: "",
      kind: undefined, commonness: undefined, spawn: undefined, spawn_time: undefined,
      biomes: [], price: undefined,
      // Health, speed and jump are always asked, so they always have a score;
      // size starts as "an ordinary horse" rather than unset, because an unset
      // size is not an obvious thing to leave alone.
      stats: { size: [0.95, 1.05], health: 5, speed: 5, jump: 5 },
      genes: genes, bands: {}, notes: [],
      // Not in the file: the base step's colour weights, so reopening the step
      // shows the sliders where they were left.
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
    ["id", "name", "description", "kind", "commonness", "spawn", "spawn_time", "price"]
      .forEach(function (k) { if (parsed[k] !== undefined) s[k] = parsed[k]; });
    s.biomes = parsed.biomes || [];
    s.stats = parsed.stats || {};
    // The file is the truth, including about the eyes: a breed file that names
    // no eye locus gets none named here either.
    s.genes = parsed.genes || {};
    s.bands = parsed.bands || {};
    s.notes = parsed.notes || [];
    return s;
  };

  /** The file with a placeholder id/name, so an unnamed breed still previews and verdicts. */
  bd.fileForCheck = function () {
    var s = Object.assign({}, bd.state);
    if (!s.id) s.id = "unnamed_breed";
    if (!s.name) s.name = "Unnamed breed";
    return bd.toJson(s);
  };

  bd.changed = function () { bd.onChange(); };
  bd.rerender = function () {};   // the app sets this to "redraw the open step"

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
  var el = bd.el;

  bd.esc = function (s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  };

  bd.round = function (n) {
    return Math.abs(n) >= 100 ? String(Math.round(n)) : String(Math.round(n * 1000) / 1000);
  };

  /** "dorsal_width" / "cellSeed" -> "Dorsal width" / "Cell seed". */
  bd.human = function (name) {
    var s = String(name).replace(/_/g, " ").replace(/([a-z])([A-Z])/g, "$1 $2").toLowerCase();
    return s.charAt(0).toUpperCase() + s.slice(1);
  };

  // ---- colour helpers ---------------------------------------------------

  /**
   * Anything a person might type as a colour - "#e0a040", "e0a040", "rgb(224,
   * 160, 64)", "orange", "rebeccapurple" - to {r, g, b}, or null. Names go
   * through the browser's own CSS parser, so every CSS colour name works.
   */
  bd.parseColour = function (text) {
    var t = String(text || "").trim();
    if (!t) return null;
    if (/^[0-9a-f]{6}$/i.test(t) || /^[0-9a-f]{3}$/i.test(t)) t = "#" + t;
    var probe = document.createElement("canvas").getContext("2d");
    probe.fillStyle = "#010203";
    probe.fillStyle = t;
    var out = probe.fillStyle;
    if (out === "#010203" && !/^#?010203$/i.test(t)) return null;
    var m = /^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i.exec(out);
    if (m) return { r: parseInt(m[1], 16), g: parseInt(m[2], 16), b: parseInt(m[3], 16) };
    m = /rgba?\((\d+),\s*(\d+),\s*(\d+)/.exec(out);
    return m ? { r: +m[1], g: +m[2], b: +m[3] } : null;
  };

  bd.hex = function (c) {
    function h(v) { v = Math.max(0, Math.min(255, Math.round(v))); return (v < 16 ? "0" : "") + v.toString(16); }
    return "#" + h(c.r) + h(c.g) + h(c.b);
  };

  /** HSV hue in degrees, 0-360. */
  bd.hueOf = function (c) {
    var r = c.r / 255, g = c.g / 255, b = c.b / 255;
    var max = Math.max(r, g, b), min = Math.min(r, g, b), d = max - min;
    if (d === 0) return 0;
    var h = max === r ? ((g - b) / d) % 6 : max === g ? (b - r) / d + 2 : (r - g) / d + 4;
    h *= 60;
    return h < 0 ? h + 360 : h;
  };

  bd.hueColour = function (h) {
    return "hsl(" + Math.round(h) + ", 80%, 50%)";
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

  function normal(pair) { return String(pair).split("/").slice().sort().join("/"); }
  bd.normal = normal;

  // ---- the default pool -------------------------------------------------

  /**
   * Ticking a gene on ticks ONE pair: the strongest one. For a gene that
   * paints, that is the outcome measured to cover the most of a horse
   * (marking-facts.json), on its homozygote where it has one; otherwise the
   * first variant's homozygote. Every eye gene starts at its wild type -
   * brown irises, white sclera, no glow, no third eye - because that is what
   * "an ordinary eye" means, and the eye step is where they are coloured.
   */
  bd.strongestPair = function (key) {
    var pairs = bd.pairsOf(key);
    var g = bd.geneByKey[key];
    if (g && bd.isEye(g)) {
      var wild = g.alleles[g.defaultIndex] || g.alleles[0];
      return wild.token + "/" + wild.token;
    }
    var f = bd.facts && bd.facts.genes[key];
    if (f && f.outcomes.length) {
      var best = f.outcomes.slice().sort(function (a, b) { return b.coverageMax - a.coverageMax; })[0];
      var hom = best.pairs.filter(function (p) { var t = p.split("/"); return t[0] === t[1]; });
      return (hom[0] || best.pairs[0]);
    }
    var variantHom = pairs.filter(function (p) { return !p.wild && p.homozygous; });
    if (variantHom.length) return variantHom[0].pair;
    var variant = pairs.filter(function (p) { return !p.wild; });
    return (variant[0] || pairs[0]).pair;
  };

  bd.enable = function (key, pair) {
    bd.state.genes[key] = [{ pair: pair || bd.strongestPair(key), weight: 1 }];
  };

  // ---- the gene card ----------------------------------------------------

  /**
   * One gene, as the breed will carry it. Off: the breed does not name it.
   * On: exactly the pairs ticked, at the weights beside them, and underneath
   * how each of its numbers varies from horse to horse.
   *
   * @param opts { epi: false to hide the numbers, note: a line under the heading }
   */
  bd.geneCard = function (key, opts) {
    opts = opts || {};
    var g = bd.geneByKey[key];
    var s = bd.state;
    var on = !!(s.genes[key] && s.genes[key].length);
    var card = el("div", { "class": "gene-card" + (on ? " on" : "") });

    var head = el("div", { "class": "gc-head" });
    var toggle = el("input", { type: "checkbox" });
    toggle.checked = on;
    toggle.addEventListener("change", function () {
      if (toggle.checked) bd.enable(key);
      else { delete s.genes[key]; delete s.bands[key]; }
      bd.changed();
      bd.rerender();
    });
    head.appendChild(el("label", { "class": "gc-toggle" }, [toggle,
      el("span", { "class": "gc-name", "data-gene": key, text: g ? g.name : key })]));
    head.appendChild(el("span", { "class": "gc-state", text: on ? "in this breed" : unnamedMeans(g) }));
    card.appendChild(head);
    if (opts.note) card.appendChild(el("p", { "class": "gc-note", text: opts.note }));
    if (!on) return card;

    card.appendChild(pairGrid(key));
    if (opts.epi !== false && bd.schemaOf(key).length) card.appendChild(epiPanel(key));
    return card;
  };

  function unnamedMeans(g) {
    if (!g) return "not installed";
    if (g.influencesCoat || g.paints || !g.natural) return "not named - never appears";
    if (g.shows === "condition") return "not named - never appears";
    return "not named - varies as in the wild";
  }

  function pairGrid(key) {
    var s = bd.state;
    var g = bd.geneByKey[key];
    var wrap = el("div", { "class": "pairs" });
    var pool = s.genes[key];
    var inPool = {};
    pool.forEach(function (c) { inPool[normal(c.pair)] = c; });
    var all = bd.pairsOf(key);

    wrap.appendChild(el("div", { "class": "sub-h", text: "Which allele pairs the breed may have" }));

    // Bulk tools: check/uncheck all where there are many alleles, and one chip
    // per allele - "every pair with it" on, then off.
    var tools = el("div", { "class": "quick" });
    if (g && g.alleles.length >= 3) {
      tools.appendChild(qb("check all", function () {
        pool.length = 0;
        all.forEach(function (p) { pool.push({ pair: p.pair, weight: 1 }); });
      }));
      tools.appendChild(qb("uncheck all", function () { pool.length = 0; }));
    }
    tools.appendChild(qb("only homozygous", function () { keep(function (p) { return p.homozygous; }); }));
    tools.appendChild(qb("only heterozygous", function () { keep(function (p) { return !p.homozygous; }); }));
    tools.appendChild(qb("equal weights", function () { pool.forEach(function (c) { c.weight = 1; }); }));
    wrap.appendChild(tools);

    if (g && g.alleles.length > 2) {
      var chips = el("div", { "class": "quick allele-chips" }, [el("span", { "class": "hint", text: "every pair with:" })]);
      g.alleles.forEach(function (a) {
        var withIt = all.filter(function (p) { return p.pair.split("/").indexOf(a.token) >= 0; });
        var allOn = withIt.length && withIt.every(function (p) { return inPool[normal(p.pair)]; });
        chips.appendChild(el("button", { type: "button", "class": "btn tiny chip-btn" + (allOn ? " on" : ""),
          title: (allOn ? "Untick" : "Tick") + " every pair carrying " + a.label,
          text: (allOn ? "− " : "+ ") + a.token,
          onclick: function () {
            withIt.forEach(function (p) {
              var c = inPool[normal(p.pair)];
              if (allOn && c) pool.splice(pool.indexOf(c), 1);
              if (!allOn && !c) pool.push({ pair: p.pair, weight: 1 });
            });
            done();
          } }));
      });
      wrap.appendChild(chips);
    }

    all.forEach(function (p) {
      var c = inPool[normal(p.pair)];
      var row = el("label", { "class": "pair-row" + (c ? " on" : "") + (p.wild ? " wild" : "") });
      var box = el("input", { type: "checkbox" });
      box.checked = !!c;
      box.addEventListener("change", function () {
        if (box.checked) pool.push({ pair: p.pair, weight: 1 });
        else pool.splice(pool.indexOf(inPool[normal(p.pair)]), 1);
        done();
      });
      row.appendChild(box);
      row.appendChild(el("code", { text: p.pair }));
      row.appendChild(el("span", { "class": "zyg", text: p.homozygous ? "homozygous" : "heterozygous" }));
      row.appendChild(el("span", { "class": "out", text: p.wild ? "wild type - shows nothing" : p.outcomeName }));
      if (c) {
        var w = el("input", { type: "number", min: "0", step: "any", value: String(c.weight), "class": "w",
          title: "relative weight - how often founders get this pair against the others" });
        w.addEventListener("click", function (e) { e.preventDefault(); });
        var share = el("span", { "class": "share", text: pct(c.weight) });
        w.addEventListener("input", function () {
          c.weight = Math.max(0, Number(w.value) || 0);
          share.textContent = pct(c.weight);
          bd.changed();
        });
        row.appendChild(w);
        row.appendChild(share);
      }
      wrap.appendChild(row);
    });
    if (!pool.length) {
      wrap.appendChild(el("p", { "class": "warn", text: "Nothing ticked - the breed will not name this gene." }));
    }

    function pct(wt) {
      var t = pool.reduce(function (a, x) { return a + (Number(x.weight) || 0); }, 0);
      return t > 0 ? (100 * (Number(wt) || 0) / t).toFixed(0) + "%" : "-";
    }
    function keep(test) {
      var byPair = {};
      all.forEach(function (p) { byPair[normal(p.pair)] = p; });
      var kept = pool.filter(function (x) { var p = byPair[normal(x.pair)]; return p && test(p); });
      if (!kept.length) kept = all.filter(test).slice(0, 1).map(function (p) { return { pair: p.pair, weight: 1 }; });
      pool.length = 0;
      kept.forEach(function (x) { pool.push(x); });
    }
    function qb(label, fn) {
      return el("button", { type: "button", "class": "btn tiny", text: label, onclick: function () { fn(); done(); } });
    }
    function done() {
      if (!pool.length) { delete s.genes[key]; delete s.bands[key]; }
      bd.changed();
      bd.rerender();
    }
    return wrap;
  }

  // ---- how the numbers vary ---------------------------------------------
  //
  // Every value a gene stores on its allele copies, as a row with three
  // choices: vary like a wild horse (nothing in the file), vary inside a range
  // ([lo, hi]), or be the same on every founder (a lock). Colours are one row
  // with a colour picker, hues a hue slider, seeds "same pattern or not".

  function epiPanel(key) {
    var s = bd.state;
    var schema = bd.schemaOf(key);
    var box = el("div", { "class": "epi" });
    box.appendChild(el("div", { "class": "sub-h", text: "How it varies from horse to horse" }));
    box.appendChild(el("p", { "class": "hint", text:
      "Each founder is given its own numbers - how far a pattern spreads, which colour, which " +
      "pattern. Leave a number wild, narrow it to a range, or make it the same on every horse. " +
      "From there it inherits and drifts like any other." }));

    var rgbPrefixes = {};
    schema.forEach(function (v) {
      var m = /^(.*)_r$/.exec(v.name);
      if (m && schema.some(function (x) { return x.name === m[1] + "_g"; })
        && schema.some(function (x) { return x.name === m[1] + "_b"; })) rgbPrefixes[m[1]] = true;
    });

    schema.forEach(function (v) {
      var m = /^(.*)_([rgb])$/.exec(v.name);
      if (m && rgbPrefixes[m[1]]) {
        if (m[2] === "r") box.appendChild(colourRow(key, m[1]));
        return;
      }
      if (v.kind === "seed") box.appendChild(seedRow(key, v));
      else box.appendChild(numberRow(key, v));
    });
    return box;
  }

  function current(key, name) {
    var b = bd.state.bands[key];
    return b ? b[name] : undefined;
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
  bd.setBand = setBand;

  function modeSelect(mode, options, onChange) {
    var sel = el("select", { "class": "mode" });
    options.forEach(function (o) {
      var opt = el("option", { value: o[0], text: o[1] });
      if (o[0] === mode) opt.selected = true;
      sel.appendChild(opt);
    });
    sel.addEventListener("change", function () { onChange(sel.value); });
    return sel;
  }

  function numberRow(key, v) {
    var cur = current(key, v.name);
    var isHue = /hue/i.test(v.name) && v.max <= 360 && v.max - v.min >= 180;
    var isCat = v.kind === "category";
    var lo = isCat ? 0 : v.min, hi = isCat ? v.max - 1 : v.max;
    var step = isCat ? 1 : (hi - lo) / 200;
    var mode = cur === undefined ? "wild" : Array.isArray(cur) ? "range" : "fixed";
    var row = el("div", { "class": "epi-row" + (isHue ? " hue" : "") });
    row.appendChild(el("span", { "class": "nm", text: bd.human(v.name) + (v.arity > 1 ? " (each leg)" : "") }));
    row.appendChild(modeSelect(mode, [["wild", "varies like a wild horse"], ["range", "varies within a range"],
      ["fixed", "the same on every horse"]], function (m) {
      var mid = Math.round(((lo + hi) / 2) / step) * step;
      if (m === "wild") setBand(key, v.name, undefined);
      else if (m === "fixed") setBand(key, v.name, Array.isArray(cur) ? cur[0] : (cur !== undefined ? cur : (isHue ? 30 : mid)));
      else setBand(key, v.name, [lo + (hi - lo) * 0.25, lo + (hi - lo) * 0.75].map(function (x) { return isCat ? Math.round(x) : +x.toFixed(3); }));
      bd.rerender();
    }));
    var ctl = el("div", { "class": "epi-ctl" });
    if (mode === "wild") {
      ctl.appendChild(el("span", { "class": "rng", text: isCat
        ? "any of " + (v.max) + " options"
        : "wild " + bd.round(v.min) + " to " + bd.round(v.max) }));
    } else {
      var a = Array.isArray(cur) ? cur[0] : cur, b = Array.isArray(cur) ? cur[1] : cur;
      ctl.appendChild(slider(a, function (x) { a = x; commit(); }, mode === "fixed" ? "value" : "from"));
      if (mode === "range") ctl.appendChild(slider(b, function (x) { b = x; commit(); }, "to"));
    }
    row.appendChild(ctl);
    return row;

    function commit() {
      if (mode === "fixed") setBand(key, v.name, a);
      else setBand(key, v.name, [Math.min(a, b), Math.max(a, b)]);
    }
    function slider(value, onInput, label) {
      var r = el("input", { type: "range", min: String(lo), max: String(hi), step: String(step), value: String(value) });
      var out = el("output", { text: fmt(value) });
      var sw = isHue ? el("span", { "class": "hue-sw", style: "background:" + bd.hueColour(value) }) : null;
      r.addEventListener("input", function () {
        var x = isCat ? Math.round(Number(r.value)) : +Number(r.value).toFixed(3);
        out.textContent = fmt(x);
        if (sw) sw.style.background = bd.hueColour(x);
        onInput(x);
      });
      return el("label", { "class": "sl" }, [el("span", { "class": "sl-l", text: label }), r, sw, out]);
    }
    function fmt(x) { return isCat ? "option " + x : isHue ? Math.round(x) + "°" : bd.round(x); }
  }

  function colourRow(key, prefix) {
    var cur = current(key, prefix + "_r");
    var locked = cur !== undefined && !Array.isArray(cur);
    var c = locked ? { r: cur, g: current(key, prefix + "_g"), b: current(key, prefix + "_b") } : { r: 224, g: 160, b: 64 };
    var row = el("div", { "class": "epi-row" });
    row.appendChild(el("span", { "class": "nm", text: bd.human(prefix === "eye_chaos" ? "chaos colour" : prefix) + " colour" }));
    row.appendChild(modeSelect(locked ? "fixed" : "wild", [["wild", "a different colour on every horse"],
      ["fixed", "this colour on every horse"]], function (m) {
      if (m === "wild") ["_r", "_g", "_b"].forEach(function (s) { setBand(key, prefix + s, undefined); });
      else setColour(key, prefix, c);
      bd.rerender();
    }));
    var ctl = el("div", { "class": "epi-ctl" });
    if (locked) {
      var pick = el("input", { type: "color", value: bd.hex(c) });
      var text = el("input", { type: "text", value: bd.hex(c), "class": "colour-text", placeholder: "#e0a040 or orange" });
      pick.addEventListener("input", function () { var x = bd.parseColour(pick.value); text.value = pick.value; setColour(key, prefix, x); });
      text.addEventListener("change", function () {
        var x = bd.parseColour(text.value);
        if (!x) { text.classList.add("bad"); return; }
        text.classList.remove("bad");
        pick.value = bd.hex(x);
        setColour(key, prefix, x);
      });
      ctl.appendChild(pick);
      ctl.appendChild(text);
    } else {
      ctl.appendChild(el("span", { "class": "rng", text: "rolled bright, per horse" }));
    }
    row.appendChild(ctl);
    return row;
  }

  function setColour(key, prefix, c) {
    setBand(key, prefix + "_r", c.r);
    setBand(key, prefix + "_g", c.g);
    setBand(key, prefix + "_b", c.b);
  }
  bd.setColour = setColour;

  function seedRow(key, v) {
    var cur = current(key, v.name);
    var row = el("div", { "class": "epi-row" });
    row.appendChild(el("span", { "class": "nm", text: bd.human(v.name) }));
    row.appendChild(modeSelect(cur === undefined ? "wild" : "fixed", [["wild", "a different pattern on every horse"],
      ["fixed", "the same pattern on every horse"]], function (m) {
      setBand(key, v.name, m === "wild" ? undefined : randomSeed());
      bd.rerender();
    }));
    var ctl = el("div", { "class": "epi-ctl" });
    if (cur !== undefined) {
      ctl.appendChild(el("button", { type: "button", "class": "btn tiny", text: "↻ try another pattern",
        title: "The preview on the right shows it", onclick: function () { setBand(key, v.name, randomSeed()); bd.rerender(); } }));
      ctl.appendChild(el("code", { "class": "rng", text: String(cur) }));
    } else {
      ctl.appendChild(el("span", { "class": "rng", text: "which noise the pattern is drawn from" }));
    }
    row.appendChild(ctl);
    return row;
  }

  /** A whole number that survives JSON exactly - written as a string, as the parser reads it. */
  function randomSeed() {
    return String(Math.floor(Math.random() * 9007199254740991) * (Math.random() < 0.5 ? -1 : 1));
  }

  // ---- colour search ----------------------------------------------------
  //
  // "I want this colour": type one, see only the genes whose numbers can be
  // that colour (colourableJson - three rgb channels, or a hue), and adding
  // one names it and locks those numbers to the colour. One control, used on
  // every step whose genes can carry a colour.

  var colourable = null;
  bd.colourSearch = function (host, geneFilter, label) {
    if (!colourable) colourable = JSON.parse(bd.api.colourableJson());
    var rows = colourable.filter(function (c) { var g = bd.geneByKey[c.key]; return g && geneFilter(g); });
    if (!rows.length) return;
    var box = el("div", { "class": "colour-search" });
    box.appendChild(el("div", { "class": "sub-h", text: label || "Find genes by colour" }));
    var pick = el("input", { type: "color", value: bd.colourQuery ? bd.hex(bd.colourQuery) : "#e05a3a" });
    var text = el("input", { type: "text", placeholder: "type a colour - #e05a3a, orange, rgb(224, 90, 58)",
      value: bd.colourQuery ? bd.hex(bd.colourQuery) : "" });
    var list = el("div", { "class": "cs-list" });
    box.appendChild(el("div", { "class": "cs-input" }, [pick, text]));
    box.appendChild(list);
    host.appendChild(box);

    pick.addEventListener("input", function () { text.value = pick.value; search(bd.parseColour(pick.value)); });
    text.addEventListener("input", function () {
      var c = bd.parseColour(text.value);
      text.classList.toggle("bad", !!text.value.trim() && !c);
      if (c) { pick.value = bd.hex(c); search(c); }
    });
    if (bd.colourQuery) search(bd.colourQuery);
    else list.appendChild(el("p", { "class": "hint", text: rows.length + " genes here can take a colour of your choosing. Type one to see them." }));

    function search(c) {
      bd.colourQuery = c;
      list.innerHTML = "";
      if (!c) return;
      list.appendChild(el("p", { "class": "hint", text: rows.length + " genes can be made this colour:" }));
      rows.map(function (r) { return bd.geneByKey[r.key] ? r : null; }).filter(Boolean)
        .sort(function (a, b) { return bd.geneByKey[a.key].name.localeCompare(bd.geneByKey[b.key].name); })
        .forEach(function (r) {
          var g = bd.geneByKey[r.key];
          var on = !!bd.state.genes[r.key];
          var what = r.rgb.map(function (p) { return bd.human(p) + " colour"; })
            .concat(r.hue.map(function (h) { return bd.human(h) + " " + Math.round(bd.hueOf(c)) + "°"; })).join(", ");
          list.appendChild(el("div", { "class": "cs-row" + (on ? " on" : "") }, [
            el("span", { "class": "cs-sw", style: "background:" + bd.hex(c) }),
            el("span", { "class": "gc-name", "data-gene": r.key, text: g.name }),
            el("span", { "class": "rng", text: what }),
            el("button", { type: "button", "class": "btn tiny" + (on ? "" : " primary"),
              text: on ? "make it this colour" : "add in this colour",
              onclick: function () { applyColour(r, c); bd.changed(); bd.rerender(); } })
          ]));
        });
    }
  };

  /** Name the gene (its coloured form, where it has one) and lock its colour numbers to c. */
  function applyColour(r, c) {
    if (!bd.state.genes[r.key]) {
      var pairs = bd.pairsOf(r.key);
      var coloured = pairs.filter(function (p) { return !p.wild && /colou?r/i.test(p.outcome + " " + p.outcomeName); });
      var hom = coloured.filter(function (p) { return p.homozygous; });
      bd.enable(r.key, (hom[0] || coloured[0] || { pair: bd.strongestPair(r.key) }).pair);
    }
    r.rgb.forEach(function (p) { setColour(r.key, p, c); });
    var hue = Math.round(bd.hueOf(c));
    r.hue.forEach(function (h) { setBand(r.key, h, hue); });
  }

  // ---- the hover card ---------------------------------------------------

  bd.installHoverCard = function () {
    var pop = el("div", { "class": "gene-pop", hidden: "hidden" });
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
      var img = el("img", { src: HG.geneIcons.url(key), alt: "" });
      img.onerror = function () { img.remove(); };
      pop.appendChild(img);
      pop.appendChild(el("h4", { text: g.name }));
      pop.appendChild(el("p", { text: g.description }));
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
})(window.HG);
