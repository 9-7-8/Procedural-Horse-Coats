// The breed designer: build a breed file, watch the horses it makes, download it.
//
// WHAT IS NEW HERE, AND WHAT IS NOT. Nothing genetic is. The gene list, the
// allele tokens, the epigenetic schemas, the base-coat presets, the founder
// roll and - crucially - the validation of the file itself all come out of
// common/ compiled to WebAssembly (../horse-designer/js/java.js, the same wasm
// the horse designer and every gene page's preview window run). The horse is
// built by the gene creator's mesh emitter off the game's own geometry tables
// and shown in the gene creator's orbit viewport.
//
// So there is no parity to keep, because there is no second implementation.
// When this page says a file is good, it is saying so with the same parser the
// game will use on it; when it warns that a gene is missing, that is the
// warning the log would print. The only thing in this file is the form.
//
// THE STATE IS THE FILE. `state` below is exactly the JSON object a breed file
// holds - same key names, same shapes - and `toJson()` is a spelling of it, not
// a translation. A field that has never been touched is ABSENT rather than set
// to a default value, which is what makes "leave it blank and the game decides"
// truthful: the boxes are empty, they say what would happen if they stay empty,
// and an untouched box writes nothing into the file.
//
// It needs the wiki served over http: loading the wasm is a fetch, and a
// file:// page is its own opaque origin.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var SHEET = 128;
  var api = null;
  var genes = [];          // from api.genesJson()
  var geneByKey = {};
  var presets = [];        // from api.basePresetsJson()
  var el = {};             // cached DOM

  // ---- the file being edited -------------------------------------------
  //
  // Absent means "the game's default", and the note under each box says what
  // that default is. Only `id` and `name` are required, and they are the only
  // two that start as empty strings rather than undefined.
  var state = null;
  var basePreset = null;   // which preset button is lit, purely cosmetic
  var seed = 1;

  function blank() {
    return {
      id: "",
      name: "",
      kind: undefined,          // natural
      commonness: undefined,    // moderate
      biomes: [],               // no wild herd anywhere
      spawn: undefined,         // all four sources
      price: undefined,         // HorsePrices.DEFAULT_PRICE
      hardy: undefined,         // false
      magic_chance: undefined,  // 0.20
      magic_whitelist: [],
      magic_blacklist: [],
      stats: {},                // every axis wild
      genes: {},                // every locus wild / global
      bands: {},
      notes: []
    };
  }

  // What the game does when a box is left empty. Shown under the box; never
  // put IN the box - see the note at the top of the file.
  var DEFAULTS = {
    kind: "natural",
    commonness: "moderate",
    biomes: "no biome, so it never heads a wild herd",
    price: "1-3 emeralds (HorsePrices.DEFAULT_PRICE)",
    hardy: "off - the breed can carry any disorder at the global rate",
    magic_chance: "0.20, halving per extra gene drawn",
    speed: "wild - the horse sits on the vanilla baseline",
    jump: "wild",
    health: "wild",
    height: "wild - 15.75 hh, the baseline horse",
    spawn: "all four - wild herds, the cowboy, spawn eggs and stables"
  };

  var SOURCES = [
    ["wild", "May head a wild herd, in one of its biomes."],
    ["cowboy", "The cowboy may breed and sell one, priced from the range above."],
    ["spawn_egg", "A breed spawn egg exists, and can turn up in dungeon loot or the horseman's stock."],
    ["stable", "May be pre-placed in a generated stable."]
  ];

  var COMMONNESS = ["extremely_common", "very_common", "common", "moderate",
    "uncommon", "rare", "very_rare"];

  // ---- boot -------------------------------------------------------------

  function start() {
    ["identity", "sources", "biomes", "stats", "base-presets", "pools", "bands",
      "verdict", "output", "boot", "herd", "founder-code"].forEach(function (id) {
        el[id.replace(/-(\w)/g, function (m, c) { return c.toUpperCase(); })] =
          document.getElementById(id);
      });

    if (location.protocol === "file:") {
      return fail("This page loads the mod as WebAssembly, which a browser will not "
        + "fetch from a file:// path. Serve the repository with any static server, "
        + "or open the published wiki.");
    }
    HG.java.load().then(boot).catch(function (e) {
      fail(String(e && e.message ? e.message : e));
    });
  }

  function fail(message) {
    el.boot.classList.add("failed");
    el.boot.querySelector(".boot-inner").innerHTML =
      "<h1>Could not start</h1><p>" + escapeHtml(message) + "</p>";
  }

  function boot(loaded) {
    api = loaded;
    SHEET = api.sheetSize();
    genes = JSON.parse(api.genesJson());
    genes.forEach(function (g) { geneByKey[g.key] = g; });
    presets = JSON.parse(api.basePresetsJson());

    state = blank();
    buildStaticUi();
    renderAll();
    el.boot.classList.add("gone");
  }

  // ---- the left column ---------------------------------------------------

  function buildStaticUi() {
    // Built-in breeds, to open one and edit from there.
    var picker = document.getElementById("open-builtin");
    JSON.parse(api.breedCatalogJson()).forEach(function (b) {
      if (b.id === "feral_mixed") return;   // not a breed anyone can edit
      var o = document.createElement("option");
      o.value = b.id;
      o.textContent = b.name;
      picker.appendChild(o);
    });
    picker.addEventListener("change", function () {
      if (!picker.value) return;
      load(api.breedFileJson(picker.value));
      picker.value = "";
    });

    document.getElementById("btn-new").addEventListener("click", function () {
      state = blank();
      basePreset = null;
      renderAll();
    });
    document.getElementById("file-input").addEventListener("change", function (e) {
      var file = e.target.files && e.target.files[0];
      if (!file) return;
      file.text().then(load);
      e.target.value = "";
    });
    document.getElementById("btn-reroll").addEventListener("click", function () {
      seed = (seed + 1) | 0;
      renderFounders();
    });
    document.getElementById("btn-copy").addEventListener("click", function () {
      navigator.clipboard.writeText(toJson());
    });
    document.getElementById("btn-download").addEventListener("click", download);

    // The two "add" pickers are the same gene list; bands only offer genes that
    // actually store numbers, because a band on a gene with an empty schema is
    // a control that does nothing.
    fillGenePicker(document.getElementById("add-gene"), false, function (key) {
      if (!state.genes[key]) state.genes[key] = [];
      renderAll();
    });
    fillGenePicker(document.getElementById("add-band"), true, function (key) {
      if (!state.bands[key]) state.bands[key] = {};
      renderAll();
    });

    presets.forEach(function (p) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "preset";
      b.innerHTML = '<span class="n">' + escapeHtml(p.name) + '</span>'
        + '<span class="b">' + escapeHtml(p.blurb) + '</span>';
      b.addEventListener("click", function () { applyPreset(p); });
      p.el = b;
      el.basePresets.appendChild(b);
    });
  }

  /**
   * A base-coat preset replaces the loci it owns and leaves every other pool
   * alone, so it can be re-picked after the rest of the breed is built without
   * throwing that work away.
   */
  function applyPreset(p) {
    Object.keys(p.genes).forEach(function (key) {
      state.genes[key] = p.genes[key].map(function (c) {
        return { pair: c.pair, weight: c.weight };
      });
    });
    basePreset = p.key;
    renderAll();
  }

  function fillGenePicker(select, bandable, onPick) {
    genes.slice().sort(function (a, b) { return a.name.localeCompare(b.name); })
      .forEach(function (g) {
        if (bandable && JSON.parse(api.epiSchemaJson(g.key)).length === 0) return;
        var o = document.createElement("option");
        o.value = g.key;
        o.textContent = g.name;
        select.appendChild(o);
      });
    select.addEventListener("change", function () {
      if (!select.value) return;
      onPick(select.value);
      select.value = "";
    });
  }

  // ---- rendering ---------------------------------------------------------

  function renderAll() {
    renderIdentity();
    renderSources();
    renderBiomes();
    renderStats();
    renderPools();
    renderBands();
    presets.forEach(function (p) { p.el.classList.toggle("is-on", p.key === basePreset); });
    renderOutput();
    renderFounders();
  }

  /** One labelled text box. An empty box is expected to clear the field, not set it to "". */
  function textField(label, value, placeholder, defaultNote, onChange) {
    var wrap = document.createElement("div");
    wrap.className = "field";
    var l = document.createElement("label");
    l.textContent = label;
    var i = document.createElement("input");
    i.type = "text";
    i.value = value == null ? "" : String(value);
    i.placeholder = placeholder;
    i.addEventListener("input", function () { onChange(i.value.trim()); });
    wrap.appendChild(l);
    wrap.appendChild(i);
    if (defaultNote) wrap.appendChild(note(defaultNote));
    return wrap;
  }

  function renderIdentity() {
    var host = el.identity;
    host.innerHTML = "";

    host.appendChild(textField("id (required)", state.id, "friesian",
      null, function (v) { state.id = v; renderOutput(); }));
    host.appendChild(textField("name (required)", state.name, "Friesian",
      null, function (v) { state.name = v; renderOutput(); }));

    host.appendChild(select("kind", state.kind, ["natural", "magical"], DEFAULTS.kind,
      function (v) { state.kind = v || undefined; renderOutput(); }));
    host.appendChild(select("commonness", state.commonness, COMMONNESS, DEFAULTS.commonness,
      function (v) { state.commonness = v || undefined; renderOutput(); }));

    // Price is the transfer-paper cost at the cowboy's, in emeralds, rolled per
    // horse inside the range - so two of a breeder's Fjords are not the same
    // price to the copper.
    var priceWrap = document.createElement("div");
    priceWrap.className = "field";
    priceWrap.innerHTML = '<label>transfer paper price, emeralds</label>';
    var pair = document.createElement("div");
    pair.className = "pair";
    pair.appendChild(numberBox(state.price ? state.price[0] : "", "min", function (v) {
      setPrice(0, v);
    }));
    pair.appendChild(numberBox(state.price ? state.price[1] : "", "max", function (v) {
      setPrice(1, v);
    }));
    priceWrap.appendChild(pair);
    priceWrap.appendChild(note(DEFAULTS.price));
    host.appendChild(priceWrap);

    var hardy = document.createElement("div");
    hardy.className = "field checks";
    hardy.innerHTML = "";
    hardy.appendChild(checkbox("hardy", state.hardy === true,
      "Force every implemented disorder locus clear on founders.", function (on) {
        state.hardy = on ? true : undefined;
        renderOutput();
      }));
    hardy.appendChild(note(DEFAULTS.hardy));
    host.appendChild(hardy);

    host.appendChild(textField("magic_chance", state.magic_chance, "DEFAULT",
      DEFAULTS.magic_chance, function (v) {
        state.magic_chance = v === "" ? undefined : Number(v);
        renderOutput();
      }));

    host.appendChild(textField("magic_whitelist (gene keys, ; separated)",
      state.magic_whitelist.join(" ; "), "DEFAULT",
      "no whitelist - the magic draw may pick any magical gene", function (v) {
        state.magic_whitelist = splitList(v);
        renderOutput();
      }));
    host.appendChild(textField("magic_blacklist (gene keys, ; separated)",
      state.magic_blacklist.join(" ; "), "DEFAULT",
      "no blacklist", function (v) {
        state.magic_blacklist = splitList(v);
        renderOutput();
      }));

    host.appendChild(textField("notes (one per line, ; separated)",
      state.notes.join(" ; "), "DEFAULT",
      "no notes - they only show on the wiki", function (v) {
        state.notes = splitList(v);
        renderOutput();
      }));
  }

  function splitList(v) {
    return v ? v.split(";").map(function (s) { return s.trim(); }).filter(Boolean) : [];
  }

  function setPrice(index, v) {
    if (v === "") {
      // Half a range is not a range. Clearing either box clears the field, which
      // is what puts the breed back on the game's default rather than on 1-null.
      state.price = undefined;
    } else {
      var p = state.price || [1, 3];
      p = p.slice();
      p[index] = Math.max(1, Math.round(Number(v) || 1));
      state.price = p;
    }
    renderOutput();
  }

  function numberBox(value, placeholder, onChange) {
    var i = document.createElement("input");
    i.type = "number";
    i.min = "1";
    i.value = value === "" || value == null ? "" : String(value);
    i.placeholder = placeholder;
    i.addEventListener("input", function () { onChange(i.value.trim()); });
    return i;
  }

  function select(label, value, options, defaultNote, onChange) {
    var wrap = document.createElement("div");
    wrap.className = "field";
    var l = document.createElement("label");
    l.textContent = label;
    var s = document.createElement("select");
    var blankOpt = document.createElement("option");
    blankOpt.value = "";
    blankOpt.textContent = "DEFAULT";
    s.appendChild(blankOpt);
    options.forEach(function (o) {
      var opt = document.createElement("option");
      opt.value = o;
      opt.textContent = o.replace(/_/g, " ");
      s.appendChild(opt);
    });
    s.value = value || "";
    s.addEventListener("change", function () { onChange(s.value); });
    wrap.appendChild(l);
    wrap.appendChild(s);
    if (defaultNote) wrap.appendChild(note(defaultNote));
    return wrap;
  }

  function note(text) {
    var n = document.createElement("span");
    n.className = "default-note";
    n.innerHTML = "blank &rarr; <b>" + escapeHtml(text) + "</b>";
    return n;
  }

  function checkbox(label, checked, what, onChange) {
    var l = document.createElement("label");
    var i = document.createElement("input");
    i.type = "checkbox";
    i.checked = checked;
    i.addEventListener("change", function () { onChange(i.checked); });
    var span = document.createElement("span");
    span.innerHTML = "<b>" + escapeHtml(label) + "</b>"
      + (what ? '<span class="what">' + escapeHtml(what) + "</span>" : "");
    l.appendChild(i);
    l.appendChild(span);
    return l;
  }

  /**
   * The spawn checklist. Untouched it is absent, which means all four; touching
   * any box makes it explicit, and unticking all four is a real answer ("comes
   * from nowhere") rather than an omission - see BreedSource.
   */
  function renderSources() {
    var host = el.sources;
    host.innerHTML = "";
    var box = document.createElement("div");
    box.className = "checks";
    var chosen = state.spawn;
    SOURCES.forEach(function (s) {
      box.appendChild(checkbox(s[0], chosen ? chosen.indexOf(s[0]) >= 0 : true, s[1],
        function (on) {
          var now = state.spawn ? state.spawn.slice()
            : SOURCES.map(function (x) { return x[0]; });
          var at = now.indexOf(s[0]);
          if (on && at < 0) now.push(s[0]);
          if (!on && at >= 0) now.splice(at, 1);
          // Keep the checklist in BreedSource declaration order so the file
          // reads the same whichever order the boxes were clicked in.
          state.spawn = SOURCES.map(function (x) { return x[0]; })
            .filter(function (k) { return now.indexOf(k) >= 0; });
          renderAll();
        }));
    });
    host.appendChild(box);
    host.appendChild(note(DEFAULTS.spawn));
  }

  function renderBiomes() {
    var host = el.biomes;
    host.innerHTML = "";

    var chips = document.createElement("div");
    chips.className = "chips";
    state.biomes.forEach(function (id, i) {
      var c = document.createElement("span");
      c.className = "chip";
      c.appendChild(document.createTextNode(id));
      var x = document.createElement("button");
      x.type = "button";
      x.textContent = "×";
      x.title = "remove";
      x.addEventListener("click", function () {
        state.biomes.splice(i, 1);
        renderAll();
      });
      c.appendChild(x);
      chips.appendChild(c);
    });
    host.appendChild(chips);

    var field = document.createElement("div");
    field.className = "field";
    var input = document.createElement("input");
    input.type = "text";
    input.placeholder = "minecraft:plains  (enter to add)";
    input.setAttribute("list", "biome-suggestions");
    input.addEventListener("keydown", function (e) {
      if (e.key !== "Enter") return;
      var v = input.value.trim();
      if (v && state.biomes.indexOf(v) < 0) state.biomes.push(v);
      input.value = "";
      renderAll();
    });
    field.appendChild(input);

    // Suggestions are the union of what the built-in breeds already say - the
    // page has no Minecraft registry to ask, and the biomes worth suggesting
    // are the ones horses already live in.
    var list = document.createElement("datalist");
    list.id = "biome-suggestions";
    JSON.parse(api.knownBiomesJson()).forEach(function (id) {
      var o = document.createElement("option");
      o.value = id;
      list.appendChild(o);
    });
    field.appendChild(list);
    field.appendChild(note(DEFAULTS.biomes));
    host.appendChild(field);
  }

  function renderStats() {
    var host = el.stats;
    host.innerHTML = "";
    [["speed", "speed, 1-10"], ["jump", "jump, 1-10"], ["health", "health, 1-10"]]
      .forEach(function (axis) {
        // A score may be written "9" or "7-9" - the parser takes either, and a
        // file opened from disk may well hold the second, so the box has to be
        // able to show one without turning it into NaN on the next keystroke.
        host.appendChild(textField(axis[1], scoreText(state.stats[axis[0]]), "DEFAULT",
          DEFAULTS[axis[0]], function (v) {
            var parsed = parseScore(v);
            if (parsed === null) delete state.stats[axis[0]];
            else state.stats[axis[0]] = parsed;
            renderOutput();
            renderFounders();
          }));
      });

    var wrap = document.createElement("div");
    wrap.className = "field";
    wrap.innerHTML = "<label>height, hands</label>";
    var pair = document.createElement("div");
    pair.className = "pair";
    var h = state.stats.height;
    pair.appendChild(numberBox(h ? h[0] : "", "lo", function (v) { setHeight(0, v); }));
    pair.appendChild(numberBox(h ? h[1] : "", "hi", function (v) { setHeight(1, v); }));
    wrap.appendChild(pair);
    wrap.appendChild(note(DEFAULTS.height));
    host.appendChild(wrap);
  }

  function scoreText(value) {
    if (value == null) return "";
    return Array.isArray(value) ? value[0] + "-" + value[1] : String(value);
  }

  /** "9" -> 9, "7-9" -> [7, 9], anything empty or unreadable -> null (clear it). */
  function parseScore(text) {
    if (!text) return null;
    var parts = text.split(/[\s,-]+/).filter(Boolean).map(Number);
    if (!parts.length || parts.some(isNaN)) return null;
    return parts.length === 1 ? parts[0] : [parts[0], parts[1]];
  }

  function setHeight(index, v) {
    if (v === "") {
      delete state.stats.height;
    } else {
      var r = (state.stats.height || [15, 16]).slice();
      r[index] = Number(v);
      state.stats.height = r;
    }
    renderOutput();
    renderFounders();
  }

  // ---- the pools ---------------------------------------------------------

  function renderPools() {
    var host = el.pools;
    host.innerHTML = "";
    Object.keys(state.genes).forEach(function (key) {
      host.appendChild(poolCard(key, state.genes[key]));
    });
    if (!Object.keys(state.genes).length) {
      host.innerHTML = '<p class="hint">No locus named yet. Pick a base coat above, '
        + 'or add a gene.</p>';
    }
  }

  function poolCard(key, combos) {
    var gene = geneByKey[key];
    var card = document.createElement("div");
    card.className = "locus";

    var head = document.createElement("div");
    head.className = "locus-head";
    head.innerHTML = '<span class="n">' + escapeHtml(gene ? gene.name : key) + "</span>"
      + '<span class="k">' + escapeHtml(key) + "</span>";
    var rm = document.createElement("button");
    rm.type = "button";
    rm.className = "btn small danger rm";
    rm.textContent = "remove";
    rm.title = "Stop naming this locus - the breed will roll it wild";
    rm.addEventListener("click", function () {
      delete state.genes[key];
      renderAll();
    });
    head.appendChild(rm);
    card.appendChild(head);

    var body = document.createElement("div");
    body.className = "locus-body";
    if (!gene) {
      body.innerHTML = '<p class="hint">This install has no gene under that key, so the '
        + 'game will roll the locus wild and say so in the log. The pool is kept in the '
        + 'file for whoever does have it.</p>';
      card.appendChild(body);
      return card;
    }

    var head2 = document.createElement("div");
    head2.className = "combo combo-head";
    head2.innerHTML = "<span>allele</span><span>allele</span><span>weight</span><span></span>";
    body.appendChild(head2);

    var total = combos.reduce(function (a, c) { return a + (Number(c.weight) || 0); }, 0);
    combos.forEach(function (combo, i) {
      body.appendChild(comboRow(gene, combos, combo, i, total));
    });

    var add = document.createElement("button");
    add.type = "button";
    add.className = "btn small";
    add.textContent = "+ pair";
    add.addEventListener("click", function () {
      var t = gene.alleles[gene.defaultIndex].token;
      combos.push({ pair: t + "/" + t, weight: 10 });
      renderAll();
    });
    body.appendChild(add);
    card.appendChild(body);
    return card;
  }

  function comboRow(gene, combos, combo, index, total) {
    var row = document.createElement("div");
    row.className = "combo";
    var tokens = String(combo.pair).split("/");

    [0, 1].forEach(function (slot) {
      var s = document.createElement("select");
      gene.alleles.forEach(function (a) {
        var o = document.createElement("option");
        o.value = a.token;
        o.textContent = a.token + " · " + a.label;
        s.appendChild(o);
      });
      s.value = tokens[slot] || gene.alleles[gene.defaultIndex].token;
      s.addEventListener("change", function () {
        tokens[slot] = s.value;
        combo.pair = tokens[0] + "/" + tokens[1];
        renderAll();
      });
      row.appendChild(s);
    });

    var w = document.createElement("input");
    w.type = "number";
    w.min = "0";
    w.step = "any";
    w.value = String(combo.weight);
    w.addEventListener("input", function () {
      combo.weight = Number(w.value) || 0;
      renderOutput();
      renderFounders();
    });
    row.appendChild(w);

    var rm = document.createElement("button");
    rm.type = "button";
    rm.className = "tiny";
    rm.textContent = "×";
    rm.title = "remove this pair";
    rm.addEventListener("click", function () {
      combos.splice(index, 1);
      renderAll();
    });
    row.appendChild(rm);

    // The weight in the file is relative, so the useful number is the share it
    // actually works out to - "44" means nothing until you know what the others
    // add up to. It hangs off the box rather than taking a column: the row is
    // already four controls wide in a 420px panel.
    w.title = total > 0
      ? (100 * (Number(combo.weight) || 0) / total).toFixed(1) + "% of this locus"
      : "the only weights here are zero";
    return row;
  }

  // ---- the bands ---------------------------------------------------------

  function renderBands() {
    var host = el.bands;
    host.innerHTML = "";
    Object.keys(state.bands).forEach(function (key) {
      host.appendChild(bandCard(key, state.bands[key]));
    });
    if (!Object.keys(state.bands).length) {
      host.innerHTML = '<p class="hint">No numbers pinned. Most breeds want none - '
        + 'a band is for when "black" is not specific enough and you mean '
        + '"<i>deeply</i> black".</p>';
    }
  }

  function bandCard(key, bands) {
    var gene = geneByKey[key];
    var schema = JSON.parse(api.epiSchemaJson(key));
    var card = document.createElement("div");
    card.className = "locus";

    var head = document.createElement("div");
    head.className = "locus-head";
    head.innerHTML = '<span class="n">' + escapeHtml(gene ? gene.name : key) + "</span>"
      + '<span class="k">' + escapeHtml(key) + "</span>";
    var rm = document.createElement("button");
    rm.type = "button";
    rm.className = "btn small danger rm";
    rm.textContent = "remove";
    rm.addEventListener("click", function () {
      delete state.bands[key];
      renderAll();
    });
    head.appendChild(rm);
    card.appendChild(head);

    var body = document.createElement("div");
    body.className = "locus-body";
    if (!schema.length) {
      body.innerHTML = '<p class="hint">This gene stores no numbers - it is a pure '
        + 'function of its alleles, so there is nothing to band.</p>';
      card.appendChild(body);
      return card;
    }
    schema.forEach(function (v) {
      var row = document.createElement("div");
      row.className = "band";
      var current = bands[v.name];

      var nm = document.createElement("span");
      nm.className = "nm";
      nm.textContent = v.name + (v.arity > 1 ? " (×" + v.arity + ")" : "");
      nm.title = v.arity > 1
        ? "One number per leg. The band is applied to each, drawn separately."
        : "";
      row.appendChild(nm);

      [0, 1].forEach(function (slot) {
        var i = document.createElement("input");
        i.type = "number";
        i.step = "any";
        i.placeholder = slot === 0 ? String(round(v.min)) : String(round(v.max));
        i.value = current ? String(current[slot]) : "";
        i.addEventListener("input", function () {
          var lo = row.querySelectorAll("input")[0].value.trim();
          var hi = row.querySelectorAll("input")[1].value.trim();
          if (lo === "" || hi === "") delete bands[v.name];
          else bands[v.name] = [Number(lo), Number(hi)];
          renderOutput();
          renderFounders();
        });
        row.appendChild(i);
      });

      var rng = document.createElement("span");
      rng.className = "rng";
      rng.textContent = round(v.min) + "–" + round(v.max);
      rng.title = "Where wild founders are rolled. A band outside it is allowed - the "
        + "hard clamp is " + round(v.clampLo) + " to " + round(v.clampHi)
        + " - but it is a claim that this breed is unlike any wild horse.";
      row.appendChild(rng);
      body.appendChild(row);
    });
    card.appendChild(body);
    return card;
  }

  function round(n) {
    return Math.abs(n) >= 100 ? String(Math.round(n)) : String(Math.round(n * 1000) / 1000);
  }

  // ---- the file ----------------------------------------------------------

  /**
   * The state, spelled as JSON, in the same field order BreedSpecWriter uses -
   * so a file written here and a file baked from the game diff cleanly against
   * each other. Absent fields are simply not emitted.
   */
  function toJson() {
    var out = {};
    out.id = state.id || "";
    out.name = state.name || "";
    if (state.kind) out.kind = state.kind;
    if (state.commonness) out.commonness = state.commonness;
    if (state.spawn) out.spawn = state.spawn;
    if (state.biomes.length) out.biomes = state.biomes;
    if (state.price) out.price = state.price;
    if (state.hardy) out.hardy = true;
    if (state.magic_chance !== undefined && !isNaN(state.magic_chance)) {
      out.magic_chance = state.magic_chance;
    }
    if (state.magic_whitelist.length) out.magic_whitelist = state.magic_whitelist;
    if (state.magic_blacklist.length) out.magic_blacklist = state.magic_blacklist;
    if (Object.keys(state.stats).length) out.stats = state.stats;
    var genePools = {};
    Object.keys(state.genes).forEach(function (k) {
      if (state.genes[k].length) genePools[k] = state.genes[k];
    });
    if (Object.keys(genePools).length) out.genes = genePools;
    var bands = {};
    Object.keys(state.bands).forEach(function (k) {
      if (Object.keys(state.bands[k]).length) bands[k] = state.bands[k];
    });
    if (Object.keys(bands).length) out.bands = bands;
    if (state.notes.length) out.notes = state.notes;
    return JSON.stringify(out, null, 2) + "\n";
  }

  function load(text) {
    var parsed;
    try {
      parsed = JSON.parse(text);
    } catch (e) {
      el.verdict.innerHTML = '<span class="err">That file is not JSON: '
        + escapeHtml(e.message) + "</span>";
      return;
    }
    var s = blank();
    ["id", "name", "kind", "commonness", "spawn", "price", "hardy", "magic_chance"]
      .forEach(function (k) { if (parsed[k] !== undefined) s[k] = parsed[k]; });
    s.biomes = parsed.biomes || [];
    s.magic_whitelist = parsed.magic_whitelist || [];
    s.magic_blacklist = parsed.magic_blacklist || [];
    s.stats = parsed.stats || {};
    s.genes = parsed.genes || {};
    s.bands = parsed.bands || {};
    s.notes = parsed.notes || [];
    state = s;
    basePreset = null;
    renderAll();
  }

  /**
   * Run the file through the real parser and say what it said.
   *
   * <p>This is the whole reason the page is wasm rather than a form: the tool's
   * verdict and the game's are the same code, so a file this calls good is a
   * file the game will load, and a warning here is the warning the log prints.
   */
  function renderOutput() {
    var json = toJson();
    el.output.textContent = json;

    var v = JSON.parse(api.checkBreedJson(json));
    var html = v.ok
      ? '<span class="ok">✓ loads as <b>' + escapeHtml(v.name) + "</b>, "
        + v.genes + " locus" + (v.genes === 1 ? "" : "es") + " named</span>"
      : '<span class="err">✗ ' + escapeHtml(v.error) + "</span>";
    if (v.warnings && v.warnings.length) {
      html += "<ul>" + v.warnings.map(function (w) {
        return "<li>" + escapeHtml(w) + "</li>";
      }).join("") + "</ul>";
    }
    el.verdict.innerHTML = html;
  }

  function download() {
    var name = (state.id || "breed") + ".json";
    var blob = new Blob([toJson()], { type: "application/json" });
    var a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = name;
    a.click();
    setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
  }

  // ---- the horses --------------------------------------------------------

  var view = null;

  function ensureView() {
    if (view) return view;
    view = HG.viewport.create(document.getElementById("viewport"), {
      background: 0x0f172a,
      grid: false
    });
    if (view) view.setSkin("ADULT");
    return view;
  }

  /**
   * One founder in the big window and a small herd of them beside it, all from
   * the real roll. A herd rather than one horse because the thing a breed file
   * gets wrong is almost never "this horse is wrong" - it is "they are all the
   * same" or "one in six is a colour this breed should not have", and neither
   * is visible in a single animal.
   */
  /**
   * Rolling and baking nine horses is not free, and a weight box produces a
   * keystroke a character. The window is short enough that it still feels live
   * and long enough that typing "120" is one herd rather than three.
   */
  var founderTimer = null;
  function renderFounders() {
    if (founderTimer) clearTimeout(founderTimer);
    founderTimer = setTimeout(renderFoundersNow, 150);
  }

  function renderFoundersNow() {
    var json = toJson();
    var first = JSON.parse(api.breedFounderJson(json, seed));
    if (!first.ok) {
      el.founderCode.textContent = "the file does not parse yet";
      el.herd.innerHTML = "";
      return;
    }
    var v = ensureView();
    if (v) v.setImage(toImageData(api.coatOf(first.genotype, first.epigenome, true)));
    el.founderCode.textContent = shortLine(first.genotype);

    el.herd.innerHTML = "";
    for (var i = 1; i <= 8; i++) {
      var horse = JSON.parse(api.breedFounderJson(json, seed * 1013 + i));
      if (!horse.ok) break;
      el.herd.appendChild(sheetCanvas(api.coatOf(horse.genotype, horse.epigenome, true)));
    }
  }

  /** The coat sheet itself. Small, honest, and free - no second mesh to build. */
  function sheetCanvas(pixels) {
    var c = document.createElement("canvas");
    c.width = SHEET;
    c.height = SHEET;
    c.getContext("2d").putImageData(toImageData(pixels), 0, 0);
    c.title = "one founder's 128px coat sheet";
    return c;
  }

  function shortLine(code) {
    return code.length > 90 ? code.slice(0, 90) + "…" : code;
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

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }

  HG.breedDesigner = { start: start };
})(window.HG);
