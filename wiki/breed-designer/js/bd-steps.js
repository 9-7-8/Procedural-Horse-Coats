// The breed designer's steps, in order. Each one is a few decisions about the
// breed and nothing else, and each writes straight into HG.bd.state - the file.
//
// A step is { id, n, title, lede, render(host), summary() }. summary() is what
// the "Chosen so far" tab lists; it reads the file, never a private copy, so
// what it says is what an export would say.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var bd = HG.bd;
  var el = function (t, a, c) { return bd.el(t, a, c); };

  function s() { return bd.state; }

  /** A list of gene cards, or a line saying there is nothing here. */
  function cards(host, genes, opts) {
    if (!genes.length) {
      host.appendChild(el("p", { "class": "hint", text: "This install has no genes of this kind." }));
      return;
    }
    genes.forEach(function (g) { host.appendChild(bd.geneCard(g.key, opts)); });
  }

  function named(genes) {
    return genes.filter(function (g) { return s().genes[g.key] && s().genes[g.key].length; });
  }

  function namesOf(genes) {
    var n = named(genes);
    return n.length ? n.map(function (g) { return g.name; }).join(", ") : "";
  }

  function byName(a, b) { return a.name.localeCompare(b.name); }

  function family(name, natural) {
    return bd.genes.filter(function (g) { return g.family === name && g.natural === natural; });
  }

  // ---- 1. base colour ---------------------------------------------------

  var presets = null;
  function preset(key) {
    if (!presets) presets = JSON.parse(bd.api.basePresetsJson());
    return presets.filter(function (p) { return p.key === key; })[0];
  }

  function shadeTokens() {
    var g = bd.geneByKey["horsegenetics.shade"];
    return g ? g.alleles.map(function (a) { return a; }) : [];
  }

  /**
   * The colour mix, turned into pools. Extension and agouti are drawn
   * independently, so P(chestnut) is the extension pool's e/e share and the
   * black/bay split among the rest is agouti's a/a share - which reproduces the
   * mix exactly. A colour at zero is not carried at all, so a black-and-bay
   * breed never throws a chestnut foal.
   */
  function applyBase(b) {
    var st = s();
    var tot = b.bay + b.black + b.chestnut;
    if (tot <= 0) {
      bd.BASE_KEYS.forEach(function (k) { delete st.genes[k]; });
      return;
    }
    var pc = b.chestnut / tot;
    var ext = [];
    if (pc >= 1) ext.push(["e/e", 100]);
    else if (pc <= 0) ext.push(["E/E", 100]);
    else ext.push(["E/E", (1 - pc) * 50], ["E/e", (1 - pc) * 50], ["e/e", pc * 100]);
    st.genes["horsegenetics.extension"] = pool(ext);

    var coloured = b.bay + b.black;
    if (coloured <= 0) {
      st.genes["horsegenetics.agouti"] = copy(preset("any").genes["horsegenetics.agouti"]);
    } else {
      var r = b.black / coloured;
      var ag = [];
      if (r >= 1) ag.push(["a/a", 100]);
      else if (r <= 0) ag.push(["A/A", 100]);
      else ag.push(["A/A", (1 - r) * 50], ["A/a", (1 - r) * 50], ["a/a", r * 100]);
      st.genes["horsegenetics.agouti"] = pool(ag);
    }

    // Shade: the bay's shade. Named on every breed even without bays - a black
    // or chestnut horse still carries and passes shade on (Breed.shadeAny).
    var chosen = Object.keys(b.shades || {}).filter(function (t) { return b.shades[t]; });
    if (b.bay > 0 && chosen.length && chosen.length < shadeTokens().length) {
      st.genes["horsegenetics.shade"] = bd.pairsOf("horsegenetics.shade")
        .filter(function (p) {
          var t = p.pair.split("/");
          return chosen.indexOf(t[0]) >= 0 && chosen.indexOf(t[1]) >= 0;
        })
        .map(function (p) { return { pair: p.pair, weight: 10 }; });
    } else {
      st.genes["horsegenetics.shade"] = copy(preset("any").genes["horsegenetics.shade"]);
    }
  }

  function pool(rows) {
    return rows.filter(function (r) { return r[1] > 0; })
      .map(function (r) { return { pair: r[0], weight: Math.round(r[1] * 10) / 10 }; });
  }
  function copy(p) { return (p || []).map(function (c) { return { pair: c.pair, weight: c.weight }; }); }

  function renderBase(host) {
    var st = s();
    var b = st._base;
    if (!b) {
      var custom = bd.BASE_KEYS.some(function (k) { return st.genes[k]; });
      if (custom) {
        host.appendChild(el("div", { "class": "callout" }, [
          el("p", { text: "This breed's colour is set gene by gene (it came from a file, or was edited below). " +
            "Use the sliders to replace it with a simple mix." }),
          el("button", { type: "button", "class": "btn", text: "Start from sliders",
            onclick: function () { st._base = { bay: 60, black: 25, chestnut: 15, shades: {} }; applyBase(st._base); bd.changed(); bd.rerender(); } })
        ]));
      } else {
        st._base = b = { bay: 60, black: 25, chestnut: 15, shades: {} };
        applyBase(b);
        bd.changed();
      }
    }
    if (st._base) {
      b = st._base;
      var mix = el("div", { "class": "mix" });
      [["bay", "Bay", "Black points, a red-brown body."],
        ["black", "Black", "Black all over."],
        ["chestnut", "Chestnut", "Red all over, no black."]].forEach(function (c) {
        var val = el("output", { text: "" });
        var range = el("input", { type: "range", min: "0", max: "100", step: "1", value: String(b[c[0]]) });
        function show() {
          var tot = b.bay + b.black + b.chestnut;
          val.textContent = tot ? Math.round(100 * b[c[0]] / tot) + "%" : "-";
        }
        range.addEventListener("input", function () {
          b[c[0]] = Number(range.value);
          applyBase(b);
          Array.prototype.forEach.call(mix.querySelectorAll("output"), function (o) { o.update(); });
          bd.changed();
        });
        range.addEventListener("change", function () { bd.rerender(); });
        val.update = show;
        show();
        mix.appendChild(el("div", { "class": "mix-row" }, [
          el("span", { "class": "swatch " + c[0] }), el("b", { text: c[1] }),
          el("span", { "class": "hint", text: c[2] }), range, val]));
      });
      host.appendChild(mix);

      if (b.bay > 0) {
        host.appendChild(el("h3", { text: "Which shade of bay?" }));
        host.appendChild(el("p", { "class": "hint", text: "Tick every shade the breed may have. All three, or none, leaves shade at its wild spread." }));
        var row = el("div", { "class": "chips" });
        shadeTokens().forEach(function (a) {
          var box = el("input", { type: "checkbox" });
          box.checked = !!(b.shades && b.shades[a.token]);
          box.addEventListener("change", function () {
            b.shades = b.shades || {};
            b.shades[a.token] = box.checked;
            applyBase(b);
            bd.changed();
          });
          row.appendChild(el("label", { "class": "chip-check" }, [box, " " + a.label]));
        });
        host.appendChild(row);
      }
    }

    var det = el("details", { "class": "advanced" });
    det.appendChild(el("summary", { text: "Exact alleles, and the numbers (extension, agouti, shade)" }));
    det.appendChild(el("p", { "class": "hint", text: "Editing a pool here turns the sliders off - the file then says exactly what you tick." }));
    bd.present(bd.BASE_KEYS).forEach(function (g) {
      var card = bd.geneCard(g.key);
      card.addEventListener("change", function () { st._base = null; }, true);
      det.appendChild(card);
    });
    host.appendChild(det);
  }

  function baseSummary() {
    var b = s()._base;
    if (b) {
      var tot = b.bay + b.black + b.chestnut || 1;
      var parts = [["bay", b.bay], ["black", b.black], ["chestnut", b.chestnut]]
        .filter(function (p) { return p[1] > 0; })
        .map(function (p) { return Math.round(100 * p[1] / tot) + "% " + p[0]; });
      return parts.join(", ");
    }
    return bd.BASE_KEYS.some(function (k) { return s().genes[k]; }) ? "set gene by gene" : "";
  }

  // ---- 2. size ----------------------------------------------------------

  var catalog = null;
  function breedCatalog() {
    if (!catalog) catalog = JSON.parse(bd.api.breedCatalogJson());
    return catalog;
  }

  function renderSize(host) {
    var st = s();
    var size = st.stats.size;
    var pinned = size !== undefined;
    var lo = pinned ? (Array.isArray(size) ? size[0] : size) : 0.9;
    var hi = pinned ? (Array.isArray(size) ? size[1] : size) : 1.1;

    var pin = el("input", { type: "checkbox" });
    pin.checked = pinned;
    pin.addEventListener("change", function () {
      if (pin.checked) st.stats.size = [lo, hi]; else delete st.stats.size;
      bd.changed();
      bd.rerender();
    });
    host.appendChild(el("label", { "class": "check" }, [pin,
      el("span", { text: " Give the breed a size. Unticked, it is the size of an ordinary horse." })]));

    var chartHost = el("div", { "class": "size-chart" });
    if (pinned) {
      var form = el("div", { "class": "size-form" });
      var loR = slider(lo), hiR = slider(hi);
      var loL = el("output"), hiL = el("output");
      function label() {
        loL.textContent = Number(loR.value).toFixed(2) + "x - " + bd.api.handsLabel(Number(loR.value));
        hiL.textContent = Number(hiR.value).toFixed(2) + "x - " + bd.api.handsLabel(Number(hiR.value));
      }
      function commit() {
        var a = Number(loR.value), b = Number(hiR.value);
        if (a > b) { var t = a; a = b; b = t; }
        st.stats.size = a === b ? a : [a, b];
        label();
        drawChart(chartHost, a, b);
        bd.changed();
      }
      loR.addEventListener("input", commit);
      hiR.addEventListener("input", commit);
      form.appendChild(el("div", { "class": "size-row" }, [el("b", { text: "Smallest" }), loR, loL]));
      form.appendChild(el("div", { "class": "size-row" }, [el("b", { text: "Largest" }), hiR, hiL]));
      host.appendChild(form);
      label();
    }
    host.appendChild(el("p", { "class": "hint", html:
      "Set as a multiple of an ordinary Minecraft horse (1x). The game works out the genes: a founder " +
      "between <b>0.7x and 1.3x</b> carries one copy of the size allele, one outside it carries two - so " +
      "the giants and the miniatures breed true, and the ordinary-sized breeds throw some variety." }));
    host.appendChild(chartHost);
    drawChart(chartHost, pinned ? lo : null, pinned ? hi : null);

    function slider(v) {
      return el("input", { type: "range", min: "0.3", max: "2", step: "0.01", value: String(v) });
    }
  }

  /** Every breed's size range, the baseline, the one-copy band, and this breed on top. */
  function drawChart(host, lo, hi) {
    var rows = breedCatalog().filter(function (b) { return b.sizeLo !== undefined; })
      .sort(function (a, b) { return (a.sizeLo + a.sizeHi) - (b.sizeLo + b.sizeHi); });
    var W = 460, rowH = 6, top = 26, left = 8, right = 8;
    var H = top + rows.length * rowH + 30;
    var min = 0.3, max = 2.0;
    function x(v) { return left + (W - left - right) * (v - min) / (max - min); }
    var svg = '<svg viewBox="0 0 ' + W + " " + H + '" role="img" aria-label="Size of every breed">';
    svg += '<rect class="het" x="' + x(0.7) + '" y="' + (top - 6) + '" width="' + (x(1.3) - x(0.7)) +
      '" height="' + (rows.length * rowH + 10) + '"/>';
    svg += '<text class="band-label" x="' + x(1.0) + '" y="' + (top - 10) + '" text-anchor="middle">one size copy (0.7x-1.3x)</text>';
    rows.forEach(function (b, i) {
      var y = top + i * rowH;
      svg += '<rect class="bar' + (b.magical ? " magic" : "") + '" x="' + x(b.sizeLo) + '" y="' + y +
        '" width="' + Math.max(2, x(b.sizeHi) - x(b.sizeLo)) + '" height="' + (rowH - 2) + '"><title>' +
        bd.esc(b.name) + ": " + b.sizeLo + "x-" + b.sizeHi + "x</title></rect>";
    });
    var baseY = top + rows.length * rowH + 4;
    svg += '<line class="one" x1="' + x(1) + '" x2="' + x(1) + '" y1="' + (top - 6) + '" y2="' + baseY + '"/>';
    [0.5, 1, 1.5, 2].forEach(function (t) {
      svg += '<text class="tick" x="' + x(t) + '" y="' + (baseY + 12) + '" text-anchor="middle">' + t + "x</text>";
    });
    svg += '<text class="tick one-label" x="' + (x(1) + 4) + '" y="' + (baseY + 24) + '">1x = a vanilla horse</text>';
    if (lo !== null && hi !== null) {
      svg += '<rect class="mine" x="' + x(lo) + '" y="' + (top - 6) + '" width="' + Math.max(2, x(hi) - x(lo)) +
        '" height="' + (rows.length * rowH + 10) + '"><title>this breed</title></rect>';
    }
    svg += "</svg>";
    host.innerHTML = '<div class="chart-title">Every breed the mod ships, smallest to largest' +
      (lo !== null ? ' - <span class="mine-key">this breed</span>' : "") + "</div>" + svg;
  }

  function sizeSummary() {
    var v = s().stats.size;
    if (v === undefined) return "";
    return Array.isArray(v) ? v[0] + "x to " + v[1] + "x" : v + "x";
  }

  // ---- 6, 11, 12. the three scores --------------------------------------

  function scoreStep(axis, what, lede) {
    return {
      render: function (host) {
        var st = s();
        var cur = st.stats[axis];
        var pinned = cur !== undefined;
        var value = pinned ? (Array.isArray(cur) ? cur[0] : cur) : 5;
        var pin = el("input", { type: "checkbox" });
        pin.checked = pinned;
        pin.addEventListener("change", function () {
          if (pin.checked) st.stats[axis] = value; else delete st.stats[axis];
          bd.changed();
          bd.rerender();
        });
        host.appendChild(el("label", { "class": "check" }, [pin, el("span", { text: " Give the breed a " + what + " score." })]));
        if (pinned) {
          var out = el("output", { text: String(value) + " / 10" });
          var r = el("input", { type: "range", min: "1", max: "10", step: "1", value: String(value) });
          r.addEventListener("input", function () {
            value = Number(r.value);
            st.stats[axis] = value;
            out.textContent = value + " / 10";
            bd.changed();
          });
          host.appendChild(el("div", { "class": "score" }, [el("span", { text: "1" }), r, el("span", { text: "10" }), out]));
        }
        host.appendChild(el("p", { "class": "hint", text: lede }));
      },
      summary: function () {
        var v = s().stats[axis];
        return v === undefined ? "" : (Array.isArray(v) ? v[0] + "-" + v[1] : v) + " / 10";
      }
    };
  }

  // ---- 5. magical markings ----------------------------------------------

  var filter = {
    q: "", sex: "any", colorful: false, blackOnly: false, whiteOnly: false,
    coverMax: 100, coverMin: 0, onBlack: false, onWhite: false, onColour: false,
    parts: {}, onlyParts: false, effects: "any", selected: "any"
  };
  var PARTS = [["head", "Head"], ["neck", "Neck"], ["body", "Body"], ["front_legs", "Front legs"],
    ["back_legs", "Back legs"], ["mane", "Mane"], ["tail", "Tail"]];

  /** Every magical painting outcome, with its gene. */
  function outcomes() {
    var out = [];
    if (!bd.facts) return out;
    Object.keys(bd.facts.genes).forEach(function (key) {
      var g = bd.geneByKey[key];
      var f = bd.facts.genes[key];
      if (!g || f.natural || bd.isEye(g) || bd.BODY_STATS.indexOf(key) >= 0) return;
      f.outcomes.forEach(function (o) { out.push({ gene: g, o: o }); });
    });
    return out.sort(function (a, b) { return a.gene.name.localeCompare(b.gene.name) || a.o.name.localeCompare(b.o.name); });
  }

  function matches(r) {
    var o = r.o, f = filter;
    if (f.q) {
      var q = f.q.toLowerCase();
      if ((r.gene.name + " " + o.name + " " + (r.gene.description || "")).toLowerCase().indexOf(q) < 0) return false;
    }
    if (f.sex === "male" && o.sexes.indexOf("male") < 0) return false;
    if (f.sex === "female" && o.sexes.indexOf("female") < 0) return false;
    if (f.sex === "linked" && !bd.facts.genes[r.gene.key].sexLinked) return false;
    if (f.colorful && !o.colorful) return false;
    if (f.blackOnly && !o.blackOnly) return false;
    if (f.whiteOnly && !o.whiteOnly) return false;
    if (o.coverageMax > f.coverMax) return false;
    if (o.coverageMin < f.coverMin) return false;
    if (f.onBlack && !o.onBlack) return false;
    if (f.onWhite && !o.onWhite) return false;
    if (f.onColour && !o.onColour) return false;
    var want = Object.keys(f.parts).filter(function (p) { return f.parts[p]; });
    for (var i = 0; i < want.length; i++) if (o.parts.indexOf(want[i]) < 0) return false;
    if (f.onlyParts && want.length) {
      for (var j = 0; j < o.parts.length; j++) if (want.indexOf(o.parts[j]) < 0) return false;
    }
    if (f.effects === "yes" && !o.effects) return false;
    if (f.effects === "no" && o.effects) return false;
    var sel = isSelected(r);
    if (f.selected === "yes" && !sel) return false;
    if (f.selected === "no" && sel) return false;
    return true;
  }

  function isSelected(r) {
    var pool = s().genes[r.gene.key];
    if (!pool) return false;
    var have = pool.map(function (c) { return c.pair.split("/").sort().join("/"); });
    return r.o.pairs.some(function (p) { return have.indexOf(p.split("/").sort().join("/")) >= 0; });
  }

  function select(r, on) {
    var st = s();
    var key = r.gene.key;
    var pool = st.genes[key] || [];
    var norm = function (p) { return p.split("/").sort().join("/"); };
    var mine = r.o.pairs.map(norm);
    pool = pool.filter(function (c) { return mine.indexOf(norm(c.pair)) < 0; });
    if (on) r.o.pairs.forEach(function (p) { pool.push({ pair: p, weight: 10 }); });
    if (pool.length) st.genes[key] = pool;
    else { delete st.genes[key]; delete st.bands[key]; }
  }

  function renderMarkings(host) {
    var all = outcomes();
    if (!bd.facts) {
      host.appendChild(el("p", { "class": "warn", text: "The marking facts did not load (assets/marking-facts.json)." }));
      return;
    }
    var panel = el("div", { "class": "filters" });
    var q = el("input", { type: "search", placeholder: "search names and descriptions", value: filter.q });
    q.addEventListener("input", function () { filter.q = q.value.trim(); list(); });
    panel.appendChild(q);

    panel.appendChild(group("Sex", radio("sex", [["any", "either"], ["male", "can show in stallions"],
      ["female", "can show in mares"], ["linked", "sex-linked genes only"]])));
    panel.appendChild(group("Colour", [check("colorful", "colourful"), check("blackOnly", "black only"),
      check("whiteOnly", "white only")]));
    panel.appendChild(group("Paints on", [check("onBlack", "black"), check("onWhite", "white markings"),
      check("onColour", "other colours")]));
    panel.appendChild(group("Coverage", [range("coverMax", "covers at most"), range("coverMin", "covers at least")]));
    var partBoxes = PARTS.map(function (p) {
      var box = el("input", { type: "checkbox" });
      box.checked = !!filter.parts[p[0]];
      box.addEventListener("change", function () { filter.parts[p[0]] = box.checked; list(); });
      return el("label", { "class": "chip-check" }, [box, " " + p[1]]);
    });
    partBoxes.push(check("onlyParts", "and nowhere else"));
    panel.appendChild(group("Reaches", partBoxes));
    panel.appendChild(group("Magic effects", radio("effects", [["any", "either"], ["yes", "has effects"], ["no", "coat only"]])));
    panel.appendChild(group("Show", radio("selected", [["any", "everything"], ["yes", "chosen"], ["no", "not chosen"]])));
    host.appendChild(panel);

    var tools = el("div", { "class": "mk-tools" });
    var count = el("span", { "class": "count" });
    var n = el("input", { type: "number", min: "1", value: "3", "class": "w" });
    tools.appendChild(count);
    tools.appendChild(el("span", { "class": "grow" }));
    tools.appendChild(el("button", { type: "button", "class": "btn tiny", text: "choose all shown",
      onclick: function () { shown().forEach(function (r) { select(r, true); }); done(); } }));
    tools.appendChild(el("button", { type: "button", "class": "btn tiny", text: "clear shown",
      onclick: function () { shown().forEach(function (r) { select(r, false); }); done(); } }));
    tools.appendChild(el("span", { "class": "grow" }));
    tools.appendChild(n);
    tools.appendChild(el("button", { type: "button", "class": "btn tiny", text: "random from shown",
      onclick: function () { pickRandom(Number(n.value) || 1, false); } }));
    tools.appendChild(el("button", { type: "button", "class": "btn tiny", text: "replace all with random",
      title: "Clear every chosen marking, then choose this many at random from the list as it is filtered",
      onclick: function () { pickRandom(Number(n.value) || 1, true); } }));
    host.appendChild(tools);

    var listHost = el("div", { "class": "mk-list" });
    host.appendChild(listHost);
    var chosenHost = el("div", { "class": "mk-chosen" });
    host.appendChild(chosenHost);

    function shown() { return all.filter(matches); }
    function pickRandom(k, replace) {
      if (replace) all.forEach(function (r) { if (isSelected(r)) select(r, false); });
      var pool = shown().filter(function (r) { return !isSelected(r); });
      for (var i = 0; i < k && pool.length; i++) {
        select(pool.splice(Math.floor(Math.random() * pool.length), 1)[0], true);
      }
      done();
    }
    function done() { bd.changed(); list(); chosen(); }

    function list() {
      var rows = shown();
      count.textContent = rows.length + " of " + all.length + " outcomes";
      listHost.innerHTML = "";
      var frag = document.createDocumentFragment();
      rows.forEach(function (r) {
        var box = el("input", { type: "checkbox" });
        box.checked = isSelected(r);
        box.addEventListener("change", function () { select(r, box.checked); bd.changed(); chosen(); });
        var o = r.o;
        var tags = [];
        if (o.colorful) tags.push("colourful");
        if (o.blackOnly) tags.push("black");
        if (o.whiteOnly) tags.push("white");
        if (o.effects) tags.push("magic");
        if (o.sexes.length === 1) tags.push(o.sexes[0] === "male" ? "stallions" : "mares");
        frag.appendChild(el("label", { "class": "mk-row" + (box.checked ? " on" : "") }, [
          box,
          el("span", { "class": "mk-gene", "data-gene": r.gene.key, text: r.gene.name }),
          el("span", { "class": "mk-out", text: o.name }),
          el("code", { "class": "mk-pairs", text: o.pairs.join("  ") }),
          el("span", { "class": "mk-cov", title: "share of the horse it repaints, least and most seen",
            text: o.coverageMin === o.coverageMax ? o.coverageMax + "%" : o.coverageMin + "-" + o.coverageMax + "%" }),
          el("span", { "class": "mk-parts", text: o.parts.map(function (p) { return p.replace("_", " "); }).join(", ") }),
          el("span", { "class": "mk-tags", text: tags.join(" · ") })
        ]));
      });
      listHost.appendChild(frag);
    }

    function chosen() {
      chosenHost.innerHTML = "";
      var genes = {};
      all.forEach(function (r) { if (isSelected(r)) genes[r.gene.key] = r.gene; });
      var keys = Object.keys(genes);
      chosenHost.appendChild(el("h3", { text: keys.length ? "Chosen markings - alleles, weights and numbers" : "No markings chosen yet" }));
      if (keys.length) {
        chosenHost.appendChild(el("p", { "class": "hint", text:
          "Every founder draws one pair from each card below. Tick the wild pair too if only some of the breed " +
          "should show a marking - its weight is how many do not." }));
      }
      keys.map(function (k) { return genes[k]; }).sort(byName).forEach(function (g) {
        chosenHost.appendChild(bd.geneCard(g.key));
      });
    }

    function group(label, children) {
      return el("div", { "class": "fgroup" }, [el("span", { "class": "flabel", text: label })].concat(children));
    }
    function check(field, label) {
      var box = el("input", { type: "checkbox" });
      box.checked = !!filter[field];
      box.addEventListener("change", function () { filter[field] = box.checked; list(); });
      return el("label", { "class": "chip-check" }, [box, " " + label]);
    }
    function radio(field, opts) {
      return opts.map(function (o) {
        var r = el("input", { type: "radio", name: "f-" + field });
        r.checked = filter[field] === o[0];
        r.addEventListener("change", function () { filter[field] = o[0]; list(); });
        return el("label", { "class": "chip-check" }, [r, " " + o[1]]);
      });
    }
    function range(field, label) {
      var out = el("output", { text: filter[field] + "%" });
      var r = el("input", { type: "range", min: "0", max: "100", step: "1", value: String(filter[field]) });
      r.addEventListener("input", function () { filter[field] = Number(r.value); out.textContent = r.value + "%"; list(); });
      return el("label", { "class": "cov" }, [label + " ", r, out]);
    }

    list();
    chosen();
  }

  function markingsSummary() {
    var keys = Object.keys(s().genes).filter(function (k) {
      var g = bd.geneByKey[k];
      return g && !g.natural && bd.facts && bd.facts.genes[k] && !bd.facts.genes[k].natural && !bd.isEye(g)
        && bd.BODY_STATS.indexOf(k) < 0;
    });
    return keys.map(function (k) { return bd.geneByKey[k].name; }).sort().join(", ");
  }

  // ---- 13. where it lives -----------------------------------------------

  var VANILLA_BIOMES = ["plains", "sunflower_plains", "snowy_plains", "ice_spikes", "desert", "swamp",
    "mangrove_swamp", "forest", "flower_forest", "birch_forest", "dark_forest", "old_growth_birch_forest",
    "old_growth_pine_taiga", "old_growth_spruce_taiga", "taiga", "snowy_taiga", "savanna", "savanna_plateau",
    "windswept_savanna", "windswept_hills", "windswept_gravelly_hills", "windswept_forest", "jungle",
    "sparse_jungle", "bamboo_jungle", "badlands", "eroded_badlands", "wooded_badlands", "meadow",
    "cherry_grove", "pale_garden", "grove", "snowy_slopes", "frozen_peaks", "jagged_peaks", "stony_peaks",
    "river", "frozen_river", "beach", "snowy_beach", "stony_shore", "mushroom_fields"]
    .map(function (b) { return "minecraft:" + b; });

  var SOURCES = [["wild", "Wild herds in its biomes"], ["cowboy", "The cowboy can sell one"],
    ["spawn_egg", "A breed spawn egg (loot, the horseman)"], ["stable", "Pre-placed in generated stables"]];
  var COMMONNESS = ["extremely_common", "very_common", "common", "moderate", "uncommon", "rare", "very_rare"];

  function renderHome(host) {
    var st = s();
    host.appendChild(el("h3", { text: "Biomes" }));
    var chips = el("div", { "class": "chips" });
    st.biomes.forEach(function (id, i) {
      chips.appendChild(el("span", { "class": "chip" }, [id,
        el("button", { type: "button", text: "×", title: "remove", onclick: function () {
          st.biomes.splice(i, 1); bd.changed(); bd.rerender(); } })]));
    });
    host.appendChild(chips);
    var input = el("input", { type: "text", placeholder: "minecraft:plains - type and press enter", list: "biome-list" });
    input.addEventListener("keydown", function (e) {
      if (e.key !== "Enter") return;
      var v = input.value.trim();
      if (v && v.indexOf(":") < 0) v = "minecraft:" + v;
      if (v && st.biomes.indexOf(v) < 0) st.biomes.push(v);
      bd.changed();
      bd.rerender();
    });
    var known = JSON.parse(bd.api.knownBiomesJson());
    var dl = el("datalist", { id: "biome-list" });
    VANILLA_BIOMES.concat(known).filter(function (v, i, a) { return a.indexOf(v) === i; }).sort()
      .forEach(function (b) { dl.appendChild(el("option", { value: b })); });
    host.appendChild(input);
    host.appendChild(dl);
    host.appendChild(el("p", { "class": "hint", text:
      "Any biome works, modded ones included, as long as a horse can stand there - herds need grass and " +
      "light, so a desert or the Nether will rarely or never see one. New herds appear as new chunks are " +
      "generated. With no biome the breed never heads a wild herd." }));

    host.appendChild(el("h3", { text: "When" }));
    host.appendChild(el("div", { "class": "chips" }, [["any", "day or night"], ["day", "day only"], ["night", "night only"]]
      .map(function (o) {
        var r = el("input", { type: "radio", name: "spawn-time" });
        r.checked = (st.spawn_time || "any") === o[0];
        r.addEventListener("change", function () { st.spawn_time = o[0] === "any" ? undefined : o[0]; bd.changed(); });
        return el("label", { "class": "chip-check" }, [r, " " + o[1]]);
      })));
    host.appendChild(el("p", { "class": "hint", text:
      "Checked when a herd is founded. Vanilla will not spawn an animal in the dark near a player, so a " +
      "night breed's herds mostly come from chunks first generated at night." }));

    host.appendChild(el("h3", { text: "Where else it comes from" }));
    var box = el("div", { "class": "stack" });
    SOURCES.forEach(function (src) {
      var c = el("input", { type: "checkbox" });
      c.checked = st.spawn ? st.spawn.indexOf(src[0]) >= 0 : true;
      c.addEventListener("change", function () {
        var now = st.spawn ? st.spawn.slice() : SOURCES.map(function (x) { return x[0]; });
        var at = now.indexOf(src[0]);
        if (c.checked && at < 0) now.push(src[0]);
        if (!c.checked && at >= 0) now.splice(at, 1);
        st.spawn = SOURCES.map(function (x) { return x[0]; }).filter(function (k) { return now.indexOf(k) >= 0; });
        if (st.spawn.length === SOURCES.length) st.spawn = undefined;
        bd.changed();
      });
      box.appendChild(el("label", { "class": "check" }, [c, el("span", { text: " " + src[1] })]));
    });
    host.appendChild(box);

    host.appendChild(el("h3", { text: "How common" }));
    var sel = el("select");
    COMMONNESS.forEach(function (c) {
      var o = el("option", { value: c, text: c.replace(/_/g, " ") });
      if ((st.commonness || "moderate") === c) o.selected = true;
      sel.appendChild(o);
    });
    sel.addEventListener("change", function () { st.commonness = sel.value === "moderate" ? undefined : sel.value; bd.changed(); });
    host.appendChild(sel);
    host.appendChild(el("p", { "class": "hint", text: "How often it heads a herd, against the other breeds of the same biome." }));
  }

  function homeSummary() {
    var st = s();
    var bits = [];
    if (st.biomes.length) bits.push(st.biomes.map(function (b) { return b.replace("minecraft:", ""); }).join(", "));
    if (st.spawn_time) bits.push(st.spawn_time + " only");
    if (st.commonness) bits.push(st.commonness.replace(/_/g, " "));
    if (st.spawn) bits.push("from " + (st.spawn.length ? st.spawn.join(", ") : "nowhere"));
    return bits.join("; ");
  }

  // ---- 13b. trade and stray magic --------------------------------------

  function renderTrade(host) {
    var st = s();
    host.appendChild(el("h3", { text: "Price at the cowboy's, in emeralds" }));
    var lo = el("input", { type: "number", min: "1", placeholder: "1", value: st.price ? st.price[0] : "" });
    var hi = el("input", { type: "number", min: "1", placeholder: "3", value: st.price ? st.price[1] : "" });
    function commit() {
      if (lo.value === "" || hi.value === "") st.price = undefined;
      else st.price = [Math.max(1, Math.round(Number(lo.value))), Math.max(1, Math.round(Number(hi.value)))];
      bd.changed();
    }
    lo.addEventListener("input", commit);
    hi.addEventListener("input", commit);
    host.appendChild(el("div", { "class": "pair-inputs" }, [lo, el("span", { text: "to" }), hi]));
    host.appendChild(el("p", { "class": "hint", text: "Leave both empty for the ordinary 1-3." }));

    host.appendChild(el("h3", { text: "Stray magic" }));
    var p = st.magic_chance === undefined ? 0.2 : st.magic_chance;
    var out = el("output", { text: Math.round(p * 100) + "%" });
    var r = el("input", { type: "range", min: "0", max: "1", step: "0.01", value: String(p) });
    r.addEventListener("input", function () {
      st.magic_chance = Number(r.value) === 0.2 ? undefined : Number(r.value);
      out.textContent = Math.round(Number(r.value) * 100) + "%";
      bd.changed();
    });
    host.appendChild(el("div", { "class": "score" }, [el("span", { text: "0%" }), r, el("span", { text: "100%" }), out]));
    host.appendChild(el("p", { "class": "hint", text:
      "The chance a wild founder picks up one random magical gene you did not choose - halving for each " +
      "extra one. The preview on the right never shows these; the herd strip under it does." }));

    host.appendChild(el("h3", { text: "Natural or magical" }));
    var sel = el("select");
    [["auto", "decide from the genes (magical if it names any magic)"], ["natural", "natural"], ["magical", "magical"]]
      .forEach(function (o) {
        var opt = el("option", { value: o[0], text: o[1] });
        if ((st.kind || "auto") === o[0]) opt.selected = true;
        sel.appendChild(opt);
      });
    sel.addEventListener("change", function () { st.kind = sel.value === "auto" ? undefined : sel.value; bd.changed(); });
    host.appendChild(sel);
  }

  function tradeSummary() {
    var st = s();
    var bits = [];
    if (st.price) bits.push(st.price[0] + "-" + st.price[1] + " emeralds");
    if (st.magic_chance !== undefined) bits.push(Math.round(st.magic_chance * 100) + "% stray magic");
    if (st.kind) bits.push(st.kind);
    return bits.join("; ");
  }

  // ---- 14. name ---------------------------------------------------------

  function slug(name) {
    return name.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "")
      .replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "");
  }

  function renderName(host) {
    var st = s();
    var name = el("input", { type: "text", value: st.name, placeholder: "Zombie Horse" });
    var id = el("input", { type: "text", value: st.id, placeholder: "zombie_horse" });
    var idTouched = !!st.id && st.id !== slug(st.name);
    name.addEventListener("input", function () {
      st.name = name.value;
      if (!idTouched) { st.id = slug(name.value); id.value = st.id; }
      bd.changed();
    });
    id.addEventListener("input", function () {
      idTouched = true;
      st.id = id.value.trim().toLowerCase();
      bd.changed();
    });
    host.appendChild(el("label", { "class": "field" }, ["Name", name]));
    host.appendChild(el("label", { "class": "field" }, ["File id", id]));
    host.appendChild(el("p", { "class": "hint", text: "The id is how the game tells breeds apart - lower case, unique. It is filled in from the name." }));

    var desc = el("textarea", { rows: "4", placeholder: "A rotting horse from the swamps: slow, bad-tempered, and not quite dead." });
    desc.value = st.description || "";
    var left = el("span", { "class": "hint" });
    function count() { left.textContent = (desc.value.length) + " characters - a sentence or two reads best in the book."; }
    desc.addEventListener("input", function () { st.description = desc.value.trim(); count(); bd.changed(); });
    count();
    host.appendChild(el("label", { "class": "field" }, ["Description, for the breed book", desc]));
    host.appendChild(left);

    var notes = el("textarea", { rows: "3", placeholder: "one note per line - they only show on the wiki" });
    notes.value = st.notes.join("\n");
    notes.addEventListener("input", function () {
      st.notes = notes.value.split("\n").map(function (x) { return x.trim(); }).filter(Boolean);
      bd.changed();
    });
    host.appendChild(el("label", { "class": "field" }, ["Notes (optional)", notes]));
  }

  // ---- the step list ----------------------------------------------------

  function geneStep(id, n, title, lede, genesFn, opts) {
    return {
      id: id, n: n, title: title, lede: lede,
      render: function (host) { cards(host, genesFn().sort(byName), opts); },
      summary: function () { return namesOf(genesFn()); }
    };
  }

  var health = scoreStep("health", "health",
    "Health, 1 to 10. 5 is an ordinary horse; 10 is about twice as hard to kill, 1 very fragile. Unticked, the " +
    "breed takes whatever its genes give it.");
  var speed = scoreStep("speed", "speed",
    "Speed, 1 to 10. 5 is an ordinary horse; 10 is about twice as fast, 1 a plod.");
  var jump = scoreStep("jump", "jump",
    "Jump, 1 to 10. 5 is an ordinary horse; 10 clears walls three times the height, 1 barely hops.");

  bd.steps = [
    { id: "base", n: "1", title: "Base colour",
      lede: "Every other colour gene works on this. Pick the mix of bay, black and chestnut the breed comes in - and, if bay, which shades.",
      render: renderBase, summary: baseSummary },
    { id: "size", n: "2", title: "Size",
      lede: "How small and how large the breed runs, as a multiple of an ordinary horse.",
      render: renderSize, summary: sizeSummary },
    geneStep("dilute", "3", "Dilutions and shading",
      "Natural genes that lighten, darken or shade the base colour - cream, dun, silver, sooty and the rest. Tick one to let the breed carry it; untouched, none of them appear.",
      function () {
        return bd.genes.filter(function (g) {
          return g.natural && (g.family === "NATURAL_DILUTION" || g.family === "NATURAL_COAT")
            && bd.BASE_KEYS.indexOf(g.key) < 0 && !bd.isEye(g);
        });
      }),
    geneStep("white", "4", "White markings",
      "Natural white: pinto patterns, roan, appaloosa spotting, face and leg white.",
      function () { return family("NATURAL_WHITE", true); }),
    geneStep("eyes", "4½", "Eye colour",
      "Eye colour, each eye its own loci - and the magical eye genes, glowing irises and a third eye among them.",
      function () { return bd.genes.filter(bd.isEye); }),
    { id: "markings", n: "5", title: "Magical markings",
      lede: "Every magical coat marking in the mod, one row per outcome. Filter by what it does, tick what the breed carries, and fine-tune each chosen gene underneath.",
      render: renderMarkings, summary: markingsSummary },
    { id: "health", n: "6", title: "Health", lede: "How tough the breed is.",
      render: health.render, summary: health.summary },
    { id: "disorders", n: "7", title: "Health issues",
      lede: "Natural disorders and hidden traits. Untouched, each turns up at its wild rate - tick one to set exactly which alleles the breed carries, or make the breed hardy.",
      render: function (host) {
        var st = s();
        var hardy = el("input", { type: "checkbox" });
        hardy.checked = !!st.hardy;
        hardy.addEventListener("change", function () { st.hardy = hardy.checked ? true : undefined; bd.changed(); });
        host.appendChild(el("label", { "class": "check callout" }, [hardy,
          el("span", { text: " Hardy: every disorder the breed does not name below is cleared on its founders." })]));
        var list = family("NATURAL_HEALTH", true).sort(byName);
        var disorders = list.filter(function (g) { return g.shows === "condition"; });
        var other = list.filter(function (g) { return g.shows !== "condition"; });
        host.appendChild(el("h3", { text: "Disorders" }));
        cards(host, disorders);
        host.appendChild(el("h3", { text: "Other hidden traits" }));
        cards(host, other);
      },
      summary: function () {
        var n = namesOf(family("NATURAL_HEALTH", true));
        return (s().hardy ? "hardy" : "") + (s().hardy && n ? "; " : "") + n;
      } },
    geneStep("diet", "8", "Diet",
      "What the breed eats. Untouched, it eats what an ordinary horse eats.",
      function () { return bd.present(bd.DIET_KEYS); }),
    geneStep("temper", "9", "Temper",
      "Whether the breed goes for anything - mobs around it, things at night, whoever attacks it or its rider.",
      function () { return bd.present(bd.AGGRO_KEYS); }),
    geneStep("abilities", "10", "Magical abilities",
      "Everything else magic can do that is not a marking: trails, yields, water, fire, light, and more. Size, speed, health and jump have steps of their own.",
      function () {
        return bd.genes.filter(function (g) {
          return !g.natural && !g.influencesCoat && bd.BODY_STATS.indexOf(g.key) < 0
            && bd.DIET_KEYS.indexOf(g.key) < 0 && bd.AGGRO_KEYS.indexOf(g.key) < 0 && !bd.isEye(g);
        });
      }),
    { id: "speed", n: "11", title: "Speed", lede: "How fast the breed runs.",
      render: speed.render, summary: speed.summary },
    { id: "jump", n: "12", title: "Jump", lede: "How high the breed jumps.",
      render: jump.render, summary: jump.summary },
    { id: "home", n: "13", title: "Where it lives",
      lede: "The biomes its wild herds live in, when they appear, and everywhere else it may come from.",
      render: renderHome, summary: homeSummary },
    { id: "trade", n: "13½", title: "Price and stray magic",
      lede: "What the cowboy asks for one, how often a founder picks up magic you did not choose, and whether it counts as a magical breed.",
      render: renderTrade, summary: tradeSummary },
    { id: "name", n: "14", title: "Name and description",
      lede: "What it is called, and the lines the breed book shows about it.",
      render: renderName, summary: function () { return s().name || ""; } }
  ];
})(window.HG);
