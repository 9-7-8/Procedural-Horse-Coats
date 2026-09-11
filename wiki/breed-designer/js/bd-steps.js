// The breed designer's steps, in order. Each one is a few decisions about the
// breed and nothing else, and each writes straight into HG.bd.state - the file.
//
// A step is { id, title, lede, render(host), summary() }. summary() is what
// the "Chosen so far" tab lists; it reads the file, never a private copy, so
// what it says is what an export would say. Steps are numbered by position.
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

  function keysOf(genes) { return genes.map(function (g) { return g.key; }); }

  // ---- base colour ------------------------------------------------------

  var presets = null;
  function preset(key) {
    if (!presets) presets = JSON.parse(bd.api.basePresetsJson());
    return presets.filter(function (p) { return p.key === key; })[0];
  }

  function shadeTokens() {
    var g = bd.geneByKey["horsegenetics.shade"];
    return g ? g.alleles.slice() : [];
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
    det.appendChild(el("summary", { text: "Exact alleles, and how the numbers vary (extension, agouti, shade)" }));
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

  // ---- size -------------------------------------------------------------

  var catalog = null;
  function breedCatalog() {
    if (!catalog) catalog = JSON.parse(bd.api.breedCatalogJson());
    return catalog;
  }

  function renderSize(host) {
    var st = s();
    var size = st.stats.size;
    var pinned = size !== undefined;
    var lo = pinned ? (Array.isArray(size) ? size[0] : size) : 0.95;
    var hi = pinned ? (Array.isArray(size) ? size[1] : size) : 1.05;

    var pin = el("input", { type: "checkbox" });
    pin.checked = pinned;
    pin.addEventListener("change", function () {
      if (pin.checked) st.stats.size = [lo, hi]; else delete st.stats.size;
      bd.changed();
      bd.rerender();
    });
    host.appendChild(el("label", { "class": "check" }, [pin,
      el("span", { html: " <b>Give the breed a size.</b> Every founder is made somewhere between the two sizes below. " +
        "Untick it and founders are left to chance, like a wild horse: mostly ordinary, now and then a pony or a big one." })]));

    var chartHost = el("div", { "class": "size-chart" });
    if (pinned) {
      var form = el("div", { "class": "size-form" });
      var loR = slider(lo), hiR = slider(hi);
      var loL = el("output"), hiL = el("output");
      var label = function () {
        loL.textContent = Number(loR.value).toFixed(2) + "x - " + bd.api.handsLabel(Number(loR.value));
        hiL.textContent = Number(hiR.value).toFixed(2) + "x - " + bd.api.handsLabel(Number(hiR.value));
      };
      var commit = function () {
        var a = Number(loR.value), b = Number(hiR.value);
        if (a > b) { var t = a; a = b; b = t; }
        st.stats.size = a === b ? a : [a, b];
        label();
        drawChart(chartHost, a, b);
        bd.changed();
      };
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
    if (v === undefined) return "left to chance";
    return Array.isArray(v) ? v[0] + "x to " + v[1] + "x" : v + "x";
  }

  // ---- health, speed, jump: always asked --------------------------------
  //
  // Every number on these steps comes from the game's own curve
  // (BreedStatCurve.factor, through statFactor) applied to the anchors the
  // curve is written against - so "19.6 m/s at 10" is a fact about the mod,
  // not a guess on this page.

  var AXES = {
    health: {
      title: "Health", anchor: 22.5,
      value: function (f) {
        var hp = 22.5 * f;
        return hp.toFixed(1) + " health - " + (hp / 2).toFixed(1) + " hearts";
      },
      compare: function (f) {
        var hp = 22.5 * f;
        if (hp < 10) return "a zombie's few hits or one bad fall kills it";
        if (hp < 18) return "fragile - weaker than most vanilla horses";
        if (hp <= 30) return "within the range vanilla horses roll (15 to 30)";
        if (hp < 40) return "tougher than any vanilla horse";
        return "a war horse - more than twice an ordinary horse's health";
      },
      explain: [
        "Health is how much harm a founder can take before it dies - from mobs, falls, fire, drowning, and " +
        "the rider's own mistakes. 5 is an ordinary horse, 22.5 health (about eleven hearts). The curve is " +
        "not symmetric: 10 more than doubles it, while 1 leaves a horse so frail that a single fall can finish it.",
        "It is also a trade-off for the rest of the breed: a breed that goes looking for fights (the Temper " +
        "step) or lives somewhere dangerous needs the health to survive them. A low score suits a delicate, " +
        "prized breed that is meant to be kept safe in a stable.",
        "The score is written onto the founders' body genes, so it breeds on: foals of two high-health parents " +
        "stay high, and a cross with an ordinary horse lands between the two."
      ]
    },
    speed: {
      title: "Speed", anchor: 9.71,
      value: function (f) {
        return (9.71 * f).toFixed(1) + " m/s";
      },
      compare: function (f) {
        var v = 9.71 * f;
        if (v < 4.3) return "slower than a player walking (4.3 m/s)";
        if (v < 5.6) return "about a player's walk - slower than sprinting";
        if (v < 8) return "a little faster than a sprinting player (5.6 m/s)";
        if (v <= 14.6) return "within the range vanilla horses roll (up to about 14.6 m/s)";
        return "faster than the fastest horse vanilla can breed";
      },
      explain: [
        "Speed is how fast a founder runs under a rider. 5 is an ordinary horse, 9.71 m/s. At 10 it is about " +
        "twice that, 19.6 m/s - faster than anything vanilla can breed. At 1 it barely " +
        "walks: slower than you do on foot, so it is only worth " +
        "riding for what else it does.",
        "Speed sets what the breed is for. A fast breed is a travel horse; a slow one is a pack animal, a " +
        "companion, or a monster whose danger is somewhere other than its legs.",
        "Like health, the score is written onto the founders' body genes and inherited: foals of two fast " +
        "parents stay fast, and a cross with an ordinary horse lands between."
      ]
    },
    jump: {
      title: "Jump", anchor: 2.5,
      value: function (f) {
        return (2.5 * f).toFixed(1) + " blocks";
      },
      compare: function (f) {
        var m = 2.5 * f;
        if (m < 1.5) return "cannot clear a fence or wall (1.5 blocks)";
        if (m < 2.2) return "clears a fence, only just";
        if (m <= 5.3) return "within the range vanilla horses roll (up to about 5.3)";
        return "higher than any vanilla horse - onto rooftops";
      },
      explain: [
        "Jump is how high a founder can leap with a rider, at full charge. 5 is an ordinary horse, about 2.5 " +
        "blocks. At 10 it is about 8.6 blocks, over houses and up cliffs. The curve climbs faster than speed " +
        "or health does at the top, because a few blocks of jump is the difference between walking round a " +
        "hill and going over it.",
        "Below a score of 3 the horse cannot clear a fence or a wall (1.5 blocks tall) - a breed kept in a fenced " +
        "field will stay there, and a rider has to go round obstacles rather than over them. That is a real " +
        "choice for a heavy draught breed or a slow monster.",
        "The score is written onto the founders' body genes and inherited like the others."
      ]
    }
  };

  function scoreStep(axis) {
    var A = AXES[axis];
    return {
      render: function (host) {
        var st = s();
        var cur = st.stats[axis];
        if (cur === undefined) { st.stats[axis] = 5; cur = 5; bd.changed(); }
        var value = Array.isArray(cur) ? cur[0] : cur;

        var big = el("div", { "class": "score-big" });
        var r = el("input", { type: "range", min: "1", max: "10", step: "0.5", value: String(value) });
        host.appendChild(el("div", { "class": "score" }, [el("span", { text: "1" }), r, el("span", { text: "10" })]));
        host.appendChild(big);
        if (Array.isArray(cur)) {
          host.appendChild(el("p", { "class": "hint", text: "The file gives a range, " + cur[0] + " to " + cur[1] +
            ". Moving the slider makes it one score." }));
        }
        var table = el("div", { "class": "score-table" });
        host.appendChild(table);
        show();
        r.addEventListener("input", function () {
          value = Number(r.value);
          st.stats[axis] = value;
          show();
          bd.changed();
        });
        A.explain.forEach(function (p) { host.appendChild(el("p", { "class": "explain", text: p })); });

        function show() {
          var f = bd.api.statFactor(axis, value);
          big.innerHTML = "";
          big.appendChild(el("b", { text: value + " / 10" }));
          big.appendChild(el("span", { text: A.value(f) + " (x" + f.toFixed(2) + " an ordinary horse)" }));
          big.appendChild(el("span", { "class": "cmp", text: A.compare(f) }));
          table.innerHTML = "";
          for (var n = 1; n <= 10; n++) {
            var fn = bd.api.statFactor(axis, n);
            table.appendChild(el("div", { "class": "st-row" + (Math.round(value) === n ? " here" : "") }, [
              el("b", { text: String(n) }),
              el("span", { text: A.value(fn) }),
              el("span", { "class": "cmp", text: A.compare(fn) })
            ]));
          }
        }
      },
      summary: function () {
        var v = s().stats[axis];
        return v === undefined ? "5 / 10" : (Array.isArray(v) ? v[0] + "-" + v[1] : v) + " / 10";
      }
    };
  }

  // ---- eyes -------------------------------------------------------------
  //
  // Eyes are drawn, not listed: every colour is a swatch, and ticking one
  // names that colour's homozygous pair on the locus. The natural iris and
  // sclera start named at brown and white. Above it all is what the founders
  // actually get, rolled by the game - which is how champagne's amber eyes and
  // a splash horse's blue ones show up here without the page knowing about
  // either: Eyes.force wrote them.

  var hues = null;
  function hueByToken() {
    if (!hues) {
      hues = {};
      JSON.parse(bd.api.eyeHuesJson()).forEach(function (h) { hues[h.token] = h; });
    }
    return hues;
  }

  function rgbCss(n) {
    n = n & 0xFFFFFF;
    return "#" + ("00000" + n.toString(16)).slice(-6);
  }

  /** The CSS fill for a hue token - chaos and invisible have none of their own. */
  function hueFill(token) {
    if (token === "Cha") return "conic-gradient(#e0433a, #e8c13a, #4ec46a, #3a8ee0, #b04fe0, #e0433a)";
    if (token === "Inv") return "repeating-conic-gradient(#556 0 25%, #334 0 50%) 0 0 / 8px 8px";
    var h = hueByToken()[token];
    return h ? rgbCss(h.rgb) : "#777";
  }

  var EYE_ROWS = [
    { title: "Iris colour", right: "horsegenetics.eye_colour_right", left: "horsegenetics.eye_colour_left", hue: true,
      hint: "The coloured ring of the eye. Tick more than one and each founder gets one of them." },
    { title: "White of the eye", right: "horsegenetics.eye_sclera_right", left: "horsegenetics.eye_sclera_left", hue: true,
      hint: "The sclera. White in every ordinary horse." },
    { title: "Heterochromia wedge", right: "horsegenetics.eye_sector_right", left: "horsegenetics.eye_sector_left",
      hint: "A slice of the iris in a second colour." },
    { title: "Wedge colour", right: "horsegenetics.eye_sector_colour_right", left: "horsegenetics.eye_sector_colour_left", hue: true,
      hint: "Only shows on an eye that has a wedge." },
    { title: "Glowing iris", right: "horsegenetics.eye_glow_iris_right", left: "horsegenetics.eye_glow_iris_left", magic: true,
      hint: "The iris glows in the dark." },
    { title: "Glowing sclera", right: "horsegenetics.eye_glow_sclera_right", left: "horsegenetics.eye_glow_sclera_left", magic: true,
      hint: "The white of the eye glows in the dark." },
    { title: "Third eye", single: "horsegenetics.eye_third", magic: true,
      hint: "An eye in the middle of the forehead, copying one of the other two or its own." }
  ];

  var unlinked = {};   // per row title: the author asked to set each eye apart

  function homTokens(key) {
    var p = s().genes[key] || [];
    return p.map(function (c) { return c.pair.split("/"); })
      .filter(function (t) { return t[0] === t[1]; }).map(function (t) { return t[0]; });
  }

  function hasHets(key) {
    return (s().genes[key] || []).some(function (c) { var t = c.pair.split("/"); return t[0] !== t[1]; });
  }

  function toggleToken(key, token) {
    var st = s();
    var p = st.genes[key] || [];
    var at = -1;
    p.forEach(function (c, i) { var t = c.pair.split("/"); if (t[0] === token && t[1] === token) at = i; });
    if (at >= 0) p.splice(at, 1);
    else p.push({ pair: token + "/" + token, weight: 1 });
    if (p.length) st.genes[key] = p; else { delete st.genes[key]; delete st.bands[key]; }
  }

  function sameSet(a, b) {
    var x = (s().genes[a] || []).map(function (c) { return bd.normal(c.pair) + "@" + c.weight; }).sort().join();
    var y = (s().genes[b] || []).map(function (c) { return bd.normal(c.pair) + "@" + c.weight; }).sort().join();
    return x === y;
  }

  function renderEyes(host) {
    var st = s();
    var tallyHost = el("div", { "class": "eye-tally" });
    host.appendChild(tallyHost);
    drawTally(tallyHost);

    host.appendChild(el("h3", { text: "Natural" }));
    EYE_ROWS.filter(function (r) { return !r.magic; }).forEach(function (r) { host.appendChild(eyeRow(r)); });
    host.appendChild(el("h3", { text: "Magical" }));
    EYE_ROWS.filter(function (r) { return r.magic; }).forEach(function (r) { host.appendChild(eyeRow(r)); });

    bd.colourSearch(host, bd.isEye, "An eye colour of your own");

    var det = el("details", { "class": "advanced" });
    det.appendChild(el("summary", { text: "Exact alleles, weights and numbers for every eye gene" }));
    det.appendChild(el("p", { "class": "hint", text:
      "The swatches above name homozygous pairs at equal weights. Here is everything: mixed pairs, weights, " +
      "and the numbers each eye gene carries. Turning a gene on starts it at its wild type." }));
    bd.genes.filter(bd.isEye).sort(byName).forEach(function (g) { det.appendChild(bd.geneCard(g.key)); });
    host.appendChild(det);

    function eyeRow(r) {
      var box = el("div", { "class": "eye-row" + (r.magic ? " magic" : "") });
      var keys = r.single ? [r.single] : [r.right, r.left];
      var g0 = bd.geneByKey[keys[0]];
      if (!g0) return box;
      var head = el("div", { "class": "eye-row-head" }, [
        el("b", { "data-gene": keys[0], text: r.title }),
        el("span", { "class": "hint", text: r.hint })
      ]);
      var linked = !r.single && !unlinked[r.title] && sameSet(r.right, r.left);
      if (!r.single) {
        var link = el("input", { type: "checkbox" });
        link.checked = linked;
        link.addEventListener("change", function () {
          unlinked[r.title] = !link.checked;
          if (link.checked) {
            // Both eyes take the right eye's answer.
            var p = st.genes[r.right];
            if (p) st.genes[r.left] = p.map(function (c) { return { pair: c.pair, weight: c.weight }; });
            else delete st.genes[r.left];
            bd.changed();
          }
          bd.rerender();
        });
        head.appendChild(el("label", { "class": "chip-check link" }, [link, " same in both eyes"]));
      }
      box.appendChild(head);
      if (r.single || linked) {
        box.appendChild(swatches(r, keys, null));
      } else {
        box.appendChild(swatches(r, [r.right], "Right eye"));
        box.appendChild(swatches(r, [r.left], "Left eye"));
      }
      return box;
    }

    function swatches(r, keys, label) {
      var g = bd.geneByKey[keys[0]];
      var have = homTokens(keys[0]);
      var on = !!st.genes[keys[0]];
      var row = el("div", { "class": "sw-row" });
      if (label) row.appendChild(el("span", { "class": "sw-label", text: label }));
      g.alleles.forEach(function (a, i) {
        var picked = have.indexOf(a.token) >= 0;
        var wild = i === g.defaultIndex;
        var face = r.hue
          ? el("span", { "class": "sw-dot", style: "background:" + hueFill(a.token) })
          : el("span", { "class": "sw-dot chip-dot" + (wild ? " none" : ""), text: wild ? "–" : a.token });
        row.appendChild(el("button", { type: "button", "class": "eye-sw" + (picked ? " on" : ""),
          title: a.label + (picked ? " - click to remove" : " - click to add"),
          onclick: function () {
            keys.forEach(function (k) { toggleToken(k, a.token); });
            bd.changed();
            bd.rerender();
          } }, [face, el("span", { "class": "sw-name", text: wild && !r.hue ? "none" : shortLabel(a.label) })]));
      });
      var note = !on ? "not named - " + (g.natural ? "the wild type" : "never appears")
        : hasHets(keys[0]) ? "includes mixed pairs - see the exact alleles below" : "";
      if (on && have.length > 1) {
        // The two eyes are separate loci and a breed file draws each pool on
        // its own, so "same in both eyes" means the same choices, not the
        // same eye - say so rather than let the strip above surprise anyone.
        note = (keys.length > 1
          ? "each eye draws one of these on its own, so some founders have one of each"
          : "each founder gets one of these, equally often") + (note ? "; " + note : "");
      }
      if (note) row.appendChild(el("span", { "class": "sw-note", text: note }));
      return row;
    }
  }

  function shortLabel(label) { return String(label).replace(/\s*\([^)]*\)\s*$/, ""); }

  /**
   * Roll founders and draw the eyes they got. Two dozen is enough to see a
   * one-in-six outcome and cheap - a founder roll is no coat compose.
   */
  function drawTally(host) {
    var st = s();
    var json = bd.fileForCheck();
    var counts = {}, order = [];
    var n = 24, rolled = 0;
    for (var i = 1; i <= n; i++) {
      var f = JSON.parse(bd.api.breedFounderJson(json, 7919 * i));
      if (!f.ok || !f.eyes) break;
      rolled++;
      var e = f.eyes;
      var k = [e.right.irisRgb, e.right.scleraRgb, e.right.iris, e.left.irisRgb, e.left.scleraRgb, e.left.iris,
        e.right.sectoral, e.left.sectoral, e.right.glowIris || e.left.glowIris, e.third].join("|");
      if (!counts[k]) { counts[k] = { n: 0, eyes: e }; order.push(k); }
      counts[k].n++;
    }
    host.appendChild(el("div", { "class": "sub-h", text: "The eyes this breed's founders actually get" }));
    if (!rolled) {
      host.appendChild(el("p", { "class": "hint", text: "The file does not load yet, so there are no founders to show." }));
      return;
    }
    var strip = el("div", { "class": "eye-strip" });
    order.sort(function (a, b) { return counts[b].n - counts[a].n; }).forEach(function (k) {
      var c = counts[k];
      strip.appendChild(el("div", { "class": "eye-pair", title: describe(c.eyes) }, [
        eyeFig(c.eyes.right), eyeFig(c.eyes.left),
        c.eyes.third ? el("span", { "class": "third-mark", text: "+3rd" }) : null,
        el("span", { "class": "eye-n", text: Math.round(100 * c.n / rolled) + "%" })
      ]));
    });
    host.appendChild(strip);

    // Why they differ from the swatches: the genes that write eye colour.
    var causes = Object.keys(st.genes).filter(function (k) {
      var g = bd.geneByKey[k];
      return g && g.eyeRequest && st.genes[k].length;
    }).map(function (k) { return bd.geneByKey[k].name; }).sort();
    var picked = homTokens("horsegenetics.eye_colour_right").concat(homTokens("horsegenetics.eye_colour_left"));
    var off = 0;
    order.forEach(function (k) {
      var e = counts[k].eyes;
      if (picked.length && (picked.indexOf(e.right.iris) < 0 || picked.indexOf(e.left.iris) < 0)) off += counts[k].n;
    });
    if (causes.length) {
      host.appendChild(el("p", { "class": "hint cause", text:
        causes.join(", ") + (causes.length === 1 ? " sets" : " set") + " a horse's eye colour when it is carried " +
        "- that wins over the colours picked below, so " +
        (off ? Math.round(100 * off / rolled) + "% of founders have eyes you did not pick." :
          "these eyes follow the coat. None of these founders happened to be affected.") +
        " Change those genes and this updates by itself." }));
    } else {
      host.appendChild(el("p", { "class": "hint", text:
        "Nothing else in the breed changes eye colour, so founders have exactly the eyes picked below. Adding " +
        "champagne, cream or a white pattern like splash would change that - this strip follows it." }));
    }

    function describe(e) {
      var hn = hueByToken();
      function one(x) { return (hn[x.iris] ? hn[x.iris].label : x.iris) + " iris, " + (hn[x.sclera] ? hn[x.sclera].label.toLowerCase() : x.sclera) + " sclera"; }
      return "Right: " + one(e.right) + ". Left: " + one(e.left) + "." + (e.third ? " A third eye." : "");
    }
  }

  function eyeFig(r) {
    var sclera = r.sclera === "Inv" ? hueFill("Inv") : rgbCss(r.scleraRgb);
    var iris = r.iris === "Inv" ? "transparent" : rgbCss(r.irisRgb);
    var wedge = r.sectoral ? "conic-gradient(" + rgbCss(r.sectorRgb) + " 0 20%, " + iris + " 20%)" : iris;
    return el("span", { "class": "eye-fig" + (r.glowIris ? " glow-i" : "") + (r.glowSclera ? " glow-s" : ""),
      style: "background:" + sclera }, [el("i", { style: "background:" + wedge })]);
  }

  function eyesSummary() {
    var hn = hueByToken();
    function names(key) {
      return homTokens(key).map(function (t) { return hn[t] ? hn[t].label.toLowerCase() : t; });
    }
    var bits = [];
    var ir = names("horsegenetics.eye_colour_right"), il = names("horsegenetics.eye_colour_left");
    if (ir.length) bits.push(ir.join("/") + (sameSet("horsegenetics.eye_colour_right", "horsegenetics.eye_colour_left") ? "" : " and " + il.join("/")) + " irises");
    var sc = names("horsegenetics.eye_sclera_right");
    if (sc.length && sc.join() !== "white") bits.push(sc.join("/") + " sclera");
    var magic = EYE_ROWS.filter(function (r) { return r.magic; }).filter(function (r) {
      return homTokens(r.single || r.right).some(function (t) { return t !== "WT"; });
    }).map(function (r) { return r.title.toLowerCase(); });
    return bits.concat(magic).join("; ");
  }

  // ---- magical markings -------------------------------------------------

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
    var have = pool.map(function (c) { return bd.normal(c.pair); });
    return r.o.pairs.some(function (p) { return have.indexOf(bd.normal(p)) >= 0; });
  }

  /**
   * Tick an outcome: its one strongest pair - the homozygote where it has
   * one, so the marking breeds true. The other pairs that give the same
   * outcome are a click away on the gene's card below.
   */
  function select(r, on) {
    var st = s();
    var key = r.gene.key;
    var pool = st.genes[key] || [];
    var mine = r.o.pairs.map(bd.normal);
    pool = pool.filter(function (c) { return mine.indexOf(bd.normal(c.pair)) < 0; });
    if (on) {
      var hom = r.o.pairs.filter(function (p) { var t = p.split("/"); return t[0] === t[1]; });
      pool.push({ pair: hom[0] || r.o.pairs[0], weight: 1 });
    }
    if (pool.length) st.genes[key] = pool;
    else { delete st.genes[key]; delete st.bands[key]; }
  }

  function renderMarkings(host) {
    var all = outcomes();
    if (!bd.facts) {
      host.appendChild(el("p", { "class": "warn", text: "The marking facts did not load (assets/marking-facts.json)." }));
      return;
    }
    bd.colourSearch(host, function (g) { return !g.natural && !bd.isEye(g) && (g.paints || g.influencesCoat); },
      "Search by colour - markings you can make any colour");

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
      var candidates = shown().filter(function (r) { return !isSelected(r); });
      for (var i = 0; i < k && candidates.length; i++) {
        select(candidates.splice(Math.floor(Math.random() * candidates.length), 1)[0], true);
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
        box.addEventListener("change", function () { select(r, box.checked); bd.changed(); list(); chosen(); });
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
      Object.keys(s().genes).forEach(function (k) {
        var g = bd.geneByKey[k];
        if (g && isMarking(k)) genes[k] = g;
      });
      var keys = Object.keys(genes);
      chosenHost.appendChild(el("h3", { text: keys.length ? "Chosen markings - alleles, weights, and how they vary" : "No markings chosen yet" }));
      if (keys.length) {
        chosenHost.appendChild(el("p", { "class": "hint", text:
          "Every founder draws one pair from each card below. Tick the wild pair too if only some of the breed " +
          "should show a marking - its weight is how many do not. Under each card: how big, which colour, " +
          "which pattern - wild, within a range, or the same on every horse." }));
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

  function isMarking(k) {
    var g = bd.geneByKey[k];
    return !!(g && !g.natural && bd.facts && bd.facts.genes[k] && !bd.facts.genes[k].natural && !bd.isEye(g)
      && bd.BODY_STATS.indexOf(k) < 0);
  }

  function markingsSummary() {
    return Object.keys(s().genes).filter(isMarking)
      .map(function (k) { return bd.geneByKey[k].name; }).sort().join(", ");
  }

  // ---- disorders --------------------------------------------------------

  function renderDisorders(host) {
    host.appendChild(el("div", { "class": "callout" }, [el("p", { html:
      "<b>A breed only ever carries the disorders ticked here.</b> Nothing else turns up on its founders - no " +
      "background rate, no surprises. Ticking one names its strongest form, so every founder is affected; to " +
      "make it a hidden risk instead, tick its clear pair (N/N) as well and give that the larger weight. " +
      "Foals can still inherit a disorder from a parent of another breed." })]));
    var list = family("NATURAL_HEALTH", true).sort(byName);
    var disorders = list.filter(function (g) { return g.shows === "condition"; });
    var other = list.filter(function (g) { return g.shows !== "condition"; });
    host.appendChild(el("h3", { text: "Disorders" }));
    cards(host, disorders);
    host.appendChild(el("h3", { text: "Other hidden traits" }));
    host.appendChild(el("p", { "class": "hint", text: "Not disorders - left alone, these vary as they do in wild horses." }));
    cards(host, other);
  }

  // ---- where it lives ---------------------------------------------------

  var BIOME_GROUPS = [
    ["Plains and meadows", ["plains", "sunflower_plains", "meadow", "cherry_grove"]],
    ["Forests", ["forest", "flower_forest", "birch_forest", "old_growth_birch_forest", "dark_forest", "pale_garden"]],
    ["Taiga and snow", ["taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga", "snowy_taiga", "snowy_plains",
      "ice_spikes", "grove", "snowy_slopes"]],
    ["Savanna", ["savanna", "savanna_plateau", "windswept_savanna"]],
    ["Hills and peaks", ["windswept_hills", "windswept_gravelly_hills", "windswept_forest", "stony_peaks",
      "jagged_peaks", "frozen_peaks"]],
    ["Jungle", ["jungle", "sparse_jungle", "bamboo_jungle"]],
    ["Dry lands", ["desert", "badlands", "wooded_badlands", "eroded_badlands"]],
    ["Wetlands and shores", ["swamp", "mangrove_swamp", "river", "frozen_river", "beach", "snowy_beach", "stony_shore"]],
    ["Unusual", ["mushroom_fields"]]
  ];
  var VANILLA = [];
  BIOME_GROUPS.forEach(function (g) { g[1].forEach(function (b) { VANILLA.push("minecraft:" + b); }); });

  var SOURCES = [["wild", "Wild herds in its biomes"], ["cowboy", "The cowboy can sell one"],
    ["spawn_egg", "A breed spawn egg (loot, the horseman)"], ["stable", "Pre-placed in generated stables"]];
  var COMMONNESS = ["extremely_common", "very_common", "common", "moderate", "uncommon", "rare", "very_rare"];

  function biomeName(id) {
    var bare = id.replace(/^minecraft:/, "");
    var t = bare.replace(/^[^:]*:/, "").replace(/_/g, " ");
    return t.charAt(0).toUpperCase() + t.slice(1);
  }

  function setBiome(id, on) {
    var st = s();
    var at = st.biomes.indexOf(id);
    if (on && at < 0) st.biomes.push(id);
    if (!on && at >= 0) st.biomes.splice(at, 1);
  }

  function renderHome(host) {
    var st = s();
    host.appendChild(el("h3", { text: "Biomes" }));
    host.appendChild(el("p", { "class": "hint", text:
      "Tick every biome its wild herds live in. Herds need grass and light, so a desert or the badlands " +
      "will see one only rarely. New herds appear as new chunks are generated. With no biome ticked the " +
      "breed never heads a wild herd - it can still come from the other places below." }));
    var grid = el("div", { "class": "biomes" });
    BIOME_GROUPS.forEach(function (g) {
      var ids = g[1].map(function (b) { return "minecraft:" + b; });
      var allOn = ids.every(function (id) { return st.biomes.indexOf(id) >= 0; });
      var box = el("fieldset", { "class": "biome-group" });
      box.appendChild(el("legend", {}, [g[0] + " ",
        el("button", { type: "button", "class": "btn tiny", text: allOn ? "none" : "all", onclick: function () {
          ids.forEach(function (id) { setBiome(id, !allOn); });
          bd.changed();
          bd.rerender();
        } })]));
      ids.forEach(function (id) {
        var c = el("input", { type: "checkbox" });
        c.checked = st.biomes.indexOf(id) >= 0;
        c.addEventListener("change", function () { setBiome(id, c.checked); bd.changed(); bd.rerender(); });
        box.appendChild(el("label", { "class": "check biome", title: id }, [c, el("span", { text: " " + biomeName(id) })]));
      });
      grid.appendChild(box);
    });
    host.appendChild(grid);

    // Modded biomes: whatever the file already says, whatever other breeds use,
    // and a box for anything else.
    var known = JSON.parse(bd.api.knownBiomesJson());
    var extra = st.biomes.concat(known).filter(function (v, i, a) {
      return VANILLA.indexOf(v) < 0 && a.indexOf(v) === i;
    }).sort();
    var other = el("fieldset", { "class": "biome-group wide" }, [el("legend", { text: "Modded and other biomes" })]);
    extra.forEach(function (id) {
      var c = el("input", { type: "checkbox" });
      c.checked = st.biomes.indexOf(id) >= 0;
      c.addEventListener("change", function () { setBiome(id, c.checked); bd.changed(); bd.rerender(); });
      other.appendChild(el("label", { "class": "check biome" }, [c, el("code", { text: " " + id })]));
    });
    var input = el("input", { type: "text", placeholder: "modid:biome_name, e.g. biomesoplenty:lavender_field" });
    var bad = el("span", { "class": "warn" });
    var add = function () {
      var v = input.value.trim().toLowerCase();
      if (!v) return;
      if (v.indexOf(":") < 0) v = "minecraft:" + v;
      if (!/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(v)) {
        bad.textContent = "A biome id is namespace:name - lower case letters, digits and _ . / -";
        return;
      }
      setBiome(v, true);
      bd.changed();
      bd.rerender();
    };
    input.addEventListener("keydown", function (e) { if (e.key === "Enter") add(); });
    other.appendChild(el("div", { "class": "biome-add" }, [input,
      el("button", { type: "button", "class": "btn small", text: "Add", onclick: add })]));
    other.appendChild(bad);
    other.appendChild(el("p", { "class": "hint", text:
      "The id is the one F3 shows in-game when you stand in the biome. A biome the game does not have is " +
      "ignored, so a breed file can list modded biomes and still load without the mod." }));
    host.appendChild(other);

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
    if (st.biomes.length) bits.push(st.biomes.map(biomeName).join(", "));
    if (st.spawn_time) bits.push(st.spawn_time + " only");
    if (st.commonness) bits.push(st.commonness.replace(/_/g, " "));
    if (st.spawn) bits.push("from " + (st.spawn.length ? st.spawn.join(", ") : "nowhere"));
    return bits.join("; ");
  }

  // ---- price and kind ---------------------------------------------------

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
    host.appendChild(el("p", { "class": "hint", text:
      "Which half of the breed book it is listed in. A breed carries exactly the magic you gave it on the " +
      "earlier steps - founders never pick up a magical gene you did not choose." }));
  }

  function tradeSummary() {
    var st = s();
    var bits = [];
    if (st.price) bits.push(st.price[0] + "-" + st.price[1] + " emeralds");
    if (st.kind) bits.push(st.kind);
    return bits.join("; ");
  }

  // ---- name -------------------------------------------------------------

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
    host.appendChild(el("div", { "class": "callout done" }, [el("p", { html:
      "<b>That's the whole breed.</b> Export saves it as a file. To play it, drop the file into " +
      "<code>.minecraft/phc/breeds/</code> (the Breeds tab of the H menu has a button that opens the folder) " +
      "and restart the game - it spawns from then on." })]));
  }

  // ---- the step list ----------------------------------------------------

  function geneStep(id, title, lede, genesFn, opts) {
    return {
      id: id, title: title, lede: lede,
      render: function (host) {
        var genes = genesFn().sort(byName);
        var keys = keysOf(genes);
        bd.colourSearch(host, function (g) { return keys.indexOf(g.key) >= 0; });
        cards(host, genes, opts);
      },
      summary: function () { return namesOf(genesFn()); }
    };
  }

  var health = scoreStep("health"), speed = scoreStep("speed"), jump = scoreStep("jump");

  bd.steps = [
    { id: "base", title: "Base colour",
      lede: "Every other colour gene works on this. Pick the mix of bay, black and chestnut the breed comes in - and, if bay, which shades.",
      render: renderBase, summary: baseSummary },
    { id: "size", title: "Size",
      lede: "How small and how large the breed runs, as a multiple of an ordinary horse.",
      render: renderSize, summary: sizeSummary },
    geneStep("dilute", "Dilutions and shading",
      "Natural genes that lighten, darken or shade the base colour - cream, dun, silver, sooty and the rest. Tick one to let the breed carry it; untouched, none of them appear.",
      function () {
        return bd.genes.filter(function (g) {
          return g.natural && (g.family === "NATURAL_DILUTION" || g.family === "NATURAL_COAT")
            && bd.BASE_KEYS.indexOf(g.key) < 0 && !bd.isEye(g);
        });
      }),
    geneStep("white", "White markings",
      "Natural white: pinto patterns, roan, appaloosa spotting, face and leg white. Several of these also give a horse blue eyes - the next step shows it.",
      function () { return family("NATURAL_WHITE", true); }),
    { id: "eyes", title: "Eye colour",
      lede: "Brown eyes with white sclera, like every ordinary horse, unless you pick otherwise. Click a colour to add or remove it.",
      render: renderEyes, summary: eyesSummary },
    { id: "markings", title: "Magical markings",
      lede: "Every magical coat marking in the mod, one row per outcome. Filter by what it does, tick what the breed carries, and fine-tune each chosen gene underneath.",
      render: renderMarkings, summary: markingsSummary },
    { id: "health", title: "Health", lede: "How much it takes to kill one. Every breed has a score; 5 is an ordinary horse.",
      render: health.render, summary: health.summary },
    { id: "disorders", title: "Health issues",
      lede: "Natural disorders the breed carries - if any.",
      render: renderDisorders,
      summary: function () { return namesOf(family("NATURAL_HEALTH", true)) || "none"; } },
    geneStep("diet", "Diet",
      "What the breed eats. Untouched, it eats what an ordinary horse eats.",
      function () { return bd.present(bd.DIET_KEYS); }),
    geneStep("temper", "Temper",
      "Whether the breed goes for anything - mobs around it, things at night, whoever attacks it or its rider.",
      function () { return bd.present(bd.AGGRO_KEYS); }),
    geneStep("abilities", "Magical abilities",
      "Everything else magic can do that is not a marking: trails, yields, water, fire, light, and more. Size, speed, health and jump have steps of their own.",
      function () {
        return bd.genes.filter(function (g) {
          return !g.natural && !g.influencesCoat && bd.BODY_STATS.indexOf(g.key) < 0
            && bd.DIET_KEYS.indexOf(g.key) < 0 && bd.AGGRO_KEYS.indexOf(g.key) < 0 && !bd.isEye(g);
        });
      }),
    { id: "speed", title: "Speed", lede: "How fast it runs. Every breed has a score; 5 is an ordinary horse.",
      render: speed.render, summary: speed.summary },
    { id: "jump", title: "Jump", lede: "How high it jumps. Every breed has a score; 5 is an ordinary horse.",
      render: jump.render, summary: jump.summary },
    { id: "home", title: "Where it lives",
      lede: "The biomes its wild herds live in, when they appear, and everywhere else it may come from.",
      render: renderHome, summary: homeSummary },
    { id: "trade", title: "Price and kind",
      lede: "What the cowboy asks for one, and which half of the breed book it belongs in.",
      render: renderTrade, summary: tradeSummary },
    { id: "name", title: "Name and description",
      lede: "What it is called, and the lines the breed book shows about it.",
      render: renderName, summary: function () { return s().name || ""; } }
  ];
  bd.steps.forEach(function (step, i) { step.n = String(i + 1); });
})(window.HG);
