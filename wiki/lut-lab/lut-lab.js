// The LUT lab: drop a gradient in, and see the horses the mod bakes against it.
//
// WHAT IS NEW HERE, AND WHAT IS NOT. The genetics is not. The coat comes out of
// common/ compiled to WebAssembly (wiki/horse-designer/js/java.js), the horse is
// the gene creator's mesh emitter over the game's own geometry tables, and it is
// shown in the gene creator's orbit viewport - the same three files the per-gene
// preview window uses, for the same reason (hard rule 3: a wiki page must not
// hold a second opinion about what a champagne looks like).
//
// What this file adds is the one thing that is actually about a LUT: swapping
// the gradient the pipeline resolves against, at run time, for one the reader
// picked off their own disk. That is a single call - DesignerApi.setGradient -
// and CoatTextureComposer reads the current LUT on every compose with nothing
// cached in between, so every horse on the page redraws against the new chart.
//
// WHICH HORSES. The base coats come from BaseCoats.all(); the dilution rows come
// from the registry, one button per combination the mod says looks different
// (genePreviewJson, the same call the gene pages make). So this file names six
// gene KEYS, an order to show them in, and one combination the registry
// deliberately collapses - all editorial choices about what is worth looking at
// when you are judging a palette - and nothing else. A locus that gains an
// allele gains a button here without this file being touched, and a key the mod
// no longer has says so instead of quietly showing a plain horse.
//
// It needs the wiki served over http: loading the wasm is a fetch, and a file://
// page is its own opaque origin. The panel says so rather than failing silently.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // This file's own directory and the wiki root above it, from the script's src
  // rather than the document's - the same reason java.js does it.
  var HERE = (function () {
    var self = document.currentScript;
    if (!self || !self.src) return "";
    return self.src.replace(/lut-lab\.js(\?.*)?$/, "");
  })();
  var WIKI = HERE + "../";
  var DESIGNER_ASSETS = WIKI + "horse-designer/assets/";

  var THREE_CDN = "https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js";
  var ORBIT_CDN = "https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js";

  var SHEET = 128;   // replaced by the mod's own sheetSize() once it is up

  /* The loci worth a row when you are judging a palette, in the order a reader
     wants to meet them: the dilutions first, because a dilution IS a different
     path through the gradient and is where a LUT lives or dies, then one white
     pattern. Only the keys are written here - every button, label and token in
     these rows is asked of the registry at run time. */
  var LOCI = [
    { key: "horsegenetics.champagne" },
    { key: "horsegenetics.dun" },
    {
      key: "horsegenetics.matp",
      /* Cr/prl is asked for by name, and the registry does not offer it: it
         shares the DOUBLE_DILUTE expression with Cr/Cr, so distinctPairsOf
         collapses the two and a coat-only preview is right to. Measured, the
         two bakes differ in exactly eight texels - the eye blocks, at MATP's
         CREAM_PEARL_GREEN - and the body is byte-identical. So the button is
         worth having and worth labelling: that difference is a fixed constant
         and is the one thing on this page a gradient cannot move. */
      also: [{
        tokens: "Cr/prl",
        name: "Cream + pearl",
        note: "same coat as Cr/Cr - only the eyes differ, and the LUT cannot move them"
      }]
    },
    { key: "horsegenetics.mushroom" },
    { key: "horsegenetics.silver" },
    // One white pattern and no more. Every white locus paints the same colour -
    // unpigmented white, which is off the chart entirely - so a second one would
    // be a second copy of the same answer about the LUT.
    { key: "horsegenetics.tobiano" }
  ];

  /* The gradients the mod ships, so there is something to look at before the
     reader uploads anything and something to go back to afterwards. Both are
     read from the designer's asset folder, which is where the pipeline's own
     copies live. */
  var SHIPPED = [
    {
      name: "Natural",
      file: "redblackgradient.png",
      note: "the gradient every wild-type horse in the game resolves against"
    },
    {
      name: "Blue / pink",
      file: "lutbluepink.png",
      note: "the LUT locus's Blupnk palette, shipped as an alternate"
    },
    {
      name: "Green / pink",
      file: "lutgreenpink.png",
      note: "the LUT locus's Grnpnk palette, shipped as an alternate"
    }
  ];

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

  /* Strictly in order: model3d and viewport both read HG.geometry at definition
     time, and OrbitControls attaches itself to THREE. */
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
        // The shipped charts, decoded once. The natural one is also what "back
        // to the mod's own gradient" means, so it is worth holding on to.
        return Promise.all(SHIPPED.map(function (s) {
          return HG.java.decode(DESIGNER_ASSETS + s.file).then(function (img) {
            return { name: s.name, note: s.note, shipped: true, img: img };
          });
        })).then(function (luts) {
          return { api: api, luts: luts };
        });
      });
    return booted;
  }

  // ---- one lab ---------------------------------------------------------

  var DICE = '<svg viewBox="0 0 24 24" aria-hidden="true">'
    + '<rect x="3" y="3" width="18" height="18" rx="4" fill="none" stroke="currentColor" stroke-width="1.7"/>'
    + '<circle cx="8" cy="8" r="1.6" fill="currentColor"/>'
    + '<circle cx="16" cy="8" r="1.6" fill="currentColor"/>'
    + '<circle cx="12" cy="12" r="1.6" fill="currentColor"/>'
    + '<circle cx="8" cy="16" r="1.6" fill="currentColor"/>'
    + '<circle cx="16" cy="16" r="1.6" fill="currentColor"/></svg>';

  function mount(host) {
    host.classList.add("lut-lab");
    host.innerHTML = '<div class="ll-boot">Loading the coat pipeline&hellip;</div>';

    boot().then(function (up) {
      build(host, up.api, up.luts);
    }, function (err) {
      host.innerHTML = '<div class="ll-boot ll-failed">'
        + 'The LUT lab could not start: ' + escapeHtml(err.message) + '.'
        + (location.protocol === "file:"
          ? ' This page is open from a file:// path, and loading the mod is a fetch -'
          + ' serve the repo with any static server, or read it on the published wiki.'
          : '')
        + '</div>';
    });
  }

  function build(host, api, shippedLuts) {
    var bases = JSON.parse(api.baseCoatsJson());

    // Each locus, as the mod describes it. A key the registry no longer has is
    // dropped with a note rather than silently - the page is then naming a gene
    // that does not exist, and that is worth seeing.
    var missing = [];
    var loci = LOCI.map(function (spec) {
      var g = JSON.parse(api.genePreviewJson(spec.key));
      if (g.missing || !g.outcomes.length) {
        missing.push(spec.key);
        return null;
      }
      // A combination this page asks for by name that the registry collapsed.
      // Only added if it is genuinely absent, so the day the mod starts telling
      // them apart the button does not appear twice.
      (spec.also || []).forEach(function (extra) {
        var already = g.outcomes.some(function (o) { return o.tokens === extra.tokens; });
        if (!already) {
          g.outcomes.push({
            tokens: extra.tokens,
            name: extra.name,
            description: extra.note,
            collapsed: true
          });
        }
      });
      return g;
    }).filter(Boolean);

    host.innerHTML = ''
      + '<div class="ll-grid">'
      + '<div class="ll-chart-panel">'
      + '<div class="ll-chart-head">The gradient</div>'
      + '<div class="ll-chart-wrap">'
      + '<canvas class="ll-chart" width="320" height="320"></canvas>'
      + '<span class="ll-corner ll-tl">chestnut</span>'
      + '<span class="ll-corner ll-tr">white</span>'
      + '<span class="ll-corner ll-bl">black</span>'
      + '<span class="ll-axis ll-axis-x">&larr; more red pigment</span>'
      + '<span class="ll-axis ll-axis-y">&larr; more black pigment</span>'
      + '</div>'
      + '<div class="ll-readout"><span class="ll-swatch"></span><code class="ll-hex">hover the chart</code></div>'
      + '<div class="ll-find">'
      + '<input type="text" class="ll-hexin" placeholder="#B0A08A" spellcheck="false"'
      + ' aria-label="Find a colour on the chart" maxlength="7">'
      + '<button type="button" class="ll-findbtn">Find</button>'
      + '</div>'
      + '<div class="ll-probe"></div>'
      + '<label class="ll-toggle"><input type="checkbox" checked>'
      + '<span>Show what this coat reads</span></label>'
      + '<div class="ll-footprint"></div>'
      + '<div class="ll-luts" role="group" aria-label="Gradient"></div>'
      + '<label class="ll-upload">'
      + '<input type="file" accept="image/*" multiple>'
      + '<span>Upload a LUT&hellip;</span>'
      + '</label>'
      + '<p class="ll-dims"></p>'
      + '</div>'
      + '<div class="ll-horse-panel">'
      + '<div class="ll-bar">'
      + '<div class="ll-group ll-bases" role="group" aria-label="Base coat"></div>'
      + '<button type="button" class="ll-dice" title="Reroll this horse&rsquo;s epigenetics"'
      + ' aria-label="Reroll epigenetics">' + DICE + '</button>'
      + '</div>'
      + '<div class="ll-mods"></div>'
      + '<div class="ll-stage"><div class="ll-view"></div></div>'
      + '<div class="ll-foot">'
      + '<code class="ll-code"></code>'
      + '<span class="ll-hint">drag to spin &middot; scroll to zoom</span>'
      + '</div>'
      + '</div>'
      + '</div>'
      + (missing.length
        ? '<p class="ll-missing">No gene is registered as <code>'
          + missing.map(escapeHtml).join('</code>, <code>')
          + '</code> &mdash; this page is naming a key the mod no longer has.</p>'
        : '');

    var view = HG.viewport.create(host.querySelector(".ll-view"), {
      background: 0x0f172a,   // the wiki's own --bg-soft
      grid: false
    });
    if (!view) {
      host.innerHTML = '<div class="ll-boot ll-failed">This browser has no usable WebGL,'
        + ' so there is nothing to draw the horse with.</div>';
      return;
    }
    if (view.controls) {
      view.controls.enablePan = false;
      view.controls.minDistance = 1.4;
      view.controls.maxDistance = 14;
    }
    view.setSkin("ADULT");

    var luts = shippedLuts.slice();

    var state = {
      lut: 0,
      base: bases[0].key,        // bay - BaseCoats.all() is ordered for this
      // One selected button per locus row, all starting at 0 = off, so the
      // first horse shown is the base coat with nothing on it.
      loci: loci.map(function () { return 0; }),
      epigenome: api.newEpigenomeCode()
    };

    // ---- the chart -----------------------------------------------------

    var chart = host.querySelector(".ll-chart");
    var dims = host.querySelector(".ll-dims");
    var swatch = host.querySelector(".ll-swatch");
    var hex = host.querySelector(".ll-hex");
    var chartSource = null;   // an offscreen canvas at the LUT's native size
    var footprint = null;     // the current horse's reading of the chart
    var showFootprint = true;
    var fpEl = host.querySelector(".ll-footprint");
    var probe = null;         // {x, y} of the clicked / found point, in chart space
    var probeEl = host.querySelector(".ll-probe");

    function drawChart(lut) {
      var off = document.createElement("canvas");
      off.width = lut.width;
      off.height = lut.height;
      var img = new ImageData(lut.width, lut.height);
      for (var i = 0; i < lut.pixels.length; i++) {
        var p = lut.pixels[i];
        img.data[i * 4] = (p >> 16) & 0xFF;
        img.data[i * 4 + 1] = (p >> 8) & 0xFF;
        img.data[i * 4 + 2] = p & 0xFF;
        img.data[i * 4 + 3] = (p >>> 24) & 0xFF;
      }
      off.getContext("2d").putImageData(img, 0, 0);
      chartSource = off;

      paintChart();
    }

    /** The gradient art alone, with the footprint overlay on top of it if it is on. */
    function paintChart() {
      if (!chartSource) return;
      var g = chart.getContext("2d");
      g.clearRect(0, 0, chart.width, chart.height);
      // Smoothed, because GradientLut samples it bilinearly - a nearest-neighbour
      // blow-up would show banding the pipeline never sees.
      g.imageSmoothingEnabled = true;
      g.drawImage(chartSource, 0, 0, chart.width, chart.height);
      if (showFootprint && footprint) drawFootprint(g, footprint);
      if (probe) drawProbe(g, probe);
    }

    /**
     * The clicked (or found) point. Deliberately the same shape as the footprint's
     * centre-of-mass mark but amber rather than white, because the two answer
     * different questions and will often sit close together: one is where this
     * horse's coat averages out, the other is wherever the reader just pointed.
     */
    function drawProbe(g, pt) {
      var x = pt.x * chart.width;
      var y = pt.y * chart.height;
      g.save();
      // A dark halo first, so the mark survives on a pale corner of the chart.
      g.strokeStyle = "rgba(8, 12, 24, 0.85)";
      g.lineWidth = 3.5;
      g.beginPath();
      g.arc(x, y, 5.5, 0, Math.PI * 2);
      g.moveTo(x - 9, y); g.lineTo(x + 9, y);
      g.moveTo(x, y - 9); g.lineTo(x, y + 9);
      g.stroke();
      g.strokeStyle = "rgba(251, 191, 36, 0.98)";
      g.lineWidth = 1.5;
      g.beginPath();
      g.arc(x, y, 5.5, 0, Math.PI * 2);
      g.moveTo(x - 9, y); g.lineTo(x + 9, y);
      g.moveTo(x, y - 9); g.lineTo(x, y + 9);
      g.stroke();
      g.restore();
    }

    /**
     * Draw where this coat actually reads from - the whole reason the chart is
     * on the page rather than just the horse.
     *
     * <p>Everything drawn here comes from lutFootprintJson, which ran phase 1
     * and reported positions in chart coordinates. Nothing about the axis
     * convention is restated here; if it were, this overlay could disagree with
     * the pipeline it is drawing, which is the one thing it must never do.
     */
    function drawFootprint(g, fp) {
      var W = chart.width, H = chart.height;
      if (fp.empty) return;

      // Dim the whole chart, then erase the dimming back off wherever the coat
      // samples - so the art shows through at full strength exactly where it is
      // used, and the density of use reads as brightness.
      g.save();
      g.fillStyle = "rgba(8, 12, 24, 0.78)";
      g.fillRect(0, 0, W, H);
      g.globalCompositeOperation = "destination-out";
      var cw = W / fp.bins, ch = H / fp.bins;
      fp.cells.forEach(function (c) {
        var t = fp.peak > 1 ? Math.pow(c.n / fp.peak, 0.45) : 1;
        g.fillStyle = "rgba(0,0,0," + (0.30 + 0.70 * t) + ")";
        // +1 so neighbouring cells meet instead of leaving a grid of seams.
        g.fillRect(c.x * cw, c.y * ch, cw + 1, ch + 1);
      });
      g.restore();

      var x0 = fp.minX * W, x1 = fp.maxX * W;
      var y0 = fp.minY * H, y1 = fp.maxY * H;

      // The bounding box - "the range it uses", as one rectangle.
      g.save();
      g.strokeStyle = "rgba(125, 211, 252, 0.95)";
      g.lineWidth = 1;
      g.setLineDash([4, 3]);
      g.strokeRect(x0 + 0.5, y0 + 0.5, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
      g.setLineDash([]);

      // Solid bars along the bottom and right edges, so each axis range can be
      // read on its own - the X level and the Y level, not just the box.
      g.strokeStyle = "rgba(125, 211, 252, 0.95)";
      g.lineWidth = 3;
      g.beginPath();
      g.moveTo(x0, H - 1.5); g.lineTo(x1, H - 1.5);
      g.moveTo(W - 1.5, y0); g.lineTo(W - 1.5, y1);
      g.stroke();

      // The texel-weighted centroid: where the coat sits on average.
      var cx = fp.cx * W, cy = fp.cy * H;
      g.strokeStyle = "rgba(255, 255, 255, 0.95)";
      g.lineWidth = 1.5;
      g.beginPath();
      g.arc(cx, cy, 4.5, 0, Math.PI * 2);
      g.moveTo(cx - 8, cy); g.lineTo(cx + 8, cy);
      g.moveTo(cx, cy - 8); g.lineTo(cx, cy + 8);
      g.stroke();
      g.restore();
    }

    chart.addEventListener("mousemove", function (e) {
      if (!chartSource) return;
      var r = chart.getBoundingClientRect();
      var x = Math.floor((e.clientX - r.left) / r.width * chartSource.width);
      var y = Math.floor((e.clientY - r.top) / r.height * chartSource.height);
      x = Math.max(0, Math.min(chartSource.width - 1, x));
      y = Math.max(0, Math.min(chartSource.height - 1, y));
      var d = chartSource.getContext("2d").getImageData(x, y, 1, 1).data;
      var css = "rgb(" + d[0] + "," + d[1] + "," + d[2] + ")";
      swatch.style.background = css;
      hex.textContent = "#" + [d[0], d[1], d[2]].map(function (c) {
        return ("0" + c.toString(16)).slice(-2);
      }).join("").toUpperCase();
    });
    chart.addEventListener("mouseleave", function () {
      swatch.style.background = "transparent";
      hex.textContent = "hover the chart";
    });

    // Click anywhere on the chart to ask what pigment resolves there. The
    // inversion is the mod's (GradientLut.redAtChartX / blackAtChartY), not
    // this file's - see DesignerApi.pigmentAtChartJson.
    chart.addEventListener("click", function (e) {
      var r = chart.getBoundingClientRect();
      var x = (e.clientX - r.left) / r.width;
      var y = (e.clientY - r.top) / r.height;
      showProbe(JSON.parse(api.pigmentAtChartJson(x, y)), null);
    });

    function findColour() {
      var text = host.querySelector(".ll-hexin").value;
      if (!text.trim()) return;
      var res = JSON.parse(api.nearestOnChartJson(text));
      if (!res.ok) {
        probeEl.innerHTML = '<span class="ll-probe-bad">'
          + escapeHtml(text) + ' is not a colour &mdash; try <code>#B0A08A</code>.</span>';
        return;
      }
      showProbe(res, text);
    }

    host.querySelector(".ll-findbtn").addEventListener("click", findColour);
    host.querySelector(".ll-hexin").addEventListener("keydown", function (e) {
      if (e.key === "Enter") { e.preventDefault(); findColour(); }
    });

    /** Pin a point on the chart and say what pigment lands there. */
    function showProbe(p, asked) {
      probe = { x: p.x, y: p.y };
      paintChart();
      function pct(v) { return Math.round(v * 100) + "%"; }
      // A poor match means the chart simply has nothing like the colour asked
      // for, and the position is then not an answer to anything - so say so
      // rather than printing a confident pair of levels.
      var far = asked !== null && p.distance > 90;
      probeEl.innerHTML = ''
        + '<div class="ll-probe-head">'
        + '<span class="ll-probe-sw" style="background:' + escapeHtml(p.rgb || "#000") + '"></span>'
        + '<code>' + escapeHtml(p.rgb || "") + '</code>'
        + '<span class="ll-probe-at">at ' + pct(p.x) + ', ' + pct(p.y) + '</span>'
        + '</div>'
        + '<div class="ll-fp-row"><span>red pigment left</span><code>' + pct(p.red) + '</code></div>'
        + '<div class="ll-fp-row"><span>red restricted</span><code>' + pct(p.redRestricted) + '</code></div>'
        + '<div class="ll-fp-row"><span>black pigment left</span><code>' + pct(p.black) + '</code></div>'
        + '<div class="ll-fp-row"><span>black restricted</span><code>' + pct(p.blackRestricted) + '</code></div>'
        + (asked !== null
          ? '<div class="ll-probe-note' + (far ? ' ll-probe-bad' : '') + '">'
            + (far
              ? 'nearest match is a long way off &mdash; this chart has nothing like '
                + escapeHtml(asked) + ', so the position above is not meaningful'
              : 'nearest match to ' + escapeHtml(asked))
            + '</div>'
          : '');
    }

    // ---- the gradient buttons -----------------------------------------

    var lutRow = host.querySelector(".ll-luts");
    var lutButtons = null;

    function rebuildLutButtons() {
      lutRow.innerHTML = "";
      lutButtons = buttons(lutRow, luts.map(function (l) {
        return {
          label: l.name,
          title: l.note || (l.img.width + "x" + l.img.height + " uploaded gradient"),
          className: l.shipped ? "" : "is-uploaded"
        };
      }), pickLut);
      lutButtons.select(state.lut);
    }

    /**
     * Hand the pipeline a different chart and redraw. This is the whole point of
     * the page: setGradient replaces the LUT the composer resolves against, and
     * because nothing caches a baked coat, the next compose is against the new
     * one.
     */
    function pickLut(i) {
      state.lut = i;
      var lut = luts[i];
      api.setGradient(lut.img.pixels, lut.img.width, lut.img.height);
      drawChart(lut.img);
      dims.textContent = lut.img.width + " × " + lut.img.height + " · "
        + (lut.shipped ? "shipped with the mod" : "uploaded, this browser only");
      lutButtons.select(i);
      render();
    }

    function addLuts(files) {
      var jobs = Array.prototype.map.call(files, function (file) {
        var url = URL.createObjectURL(file);
        return HG.java.decode(url).then(function (img) {
          URL.revokeObjectURL(url);
          return { name: file.name.replace(/\.[a-z0-9]+$/i, ""), shipped: false, img: img };
        }, function () {
          URL.revokeObjectURL(url);
          return null;   // not an image the browser could decode - skip it
        });
      });
      Promise.all(jobs).then(function (added) {
        var kept = added.filter(Boolean);
        if (!kept.length) return;
        luts = luts.concat(kept);
        state.lut = luts.length - 1;   // show the last one dropped in
        rebuildLutButtons();
        pickLut(state.lut);
      });
    }

    host.querySelector(".ll-upload input").addEventListener("change", function (e) {
      if (e.target.files && e.target.files.length) addLuts(e.target.files);
      e.target.value = "";   // so the same file can be picked again after an edit
    });

    // Dropping onto the panel is the gesture people reach for first.
    var panel = host.querySelector(".ll-chart-panel");
    ["dragenter", "dragover"].forEach(function (t) {
      panel.addEventListener(t, function (e) {
        e.preventDefault();
        panel.classList.add("is-dropping");
      });
    });
    ["dragleave", "drop"].forEach(function (t) {
      panel.addEventListener(t, function (e) {
        e.preventDefault();
        panel.classList.remove("is-dropping");
      });
    });
    panel.addEventListener("drop", function (e) {
      if (e.dataTransfer && e.dataTransfer.files.length) addLuts(e.dataTransfer.files);
    });

    // ---- the horse -----------------------------------------------------

    var baseButtons = buttons(host.querySelector(".ll-bases"), bases.map(function (b) {
      return { label: b.name, title: b.name + " base coat" };
    }), function (i) {
      state.base = bases[i].key;
      render();
    });

    /* One row per locus, each starting at "off". They stack: champagne on a
       cream is a different journey across the chart than either alone, and a
       palette that survives one and not the other is worth catching here. */
    var modButtons = loci.map(function (locus, m) {
      var row = document.createElement("div");
      row.className = "ll-mod";
      row.innerHTML = '<span class="ll-modname">' + escapeHtml(locus.name) + '</span>';
      host.querySelector(".ll-mods").appendChild(row);
      var items = [{ label: "off", title: "no " + locus.name }].concat(
        locus.outcomes.map(function (o) {
          return {
            label: o.tokens,
            title: o.name + (o.description ? " - " + o.description : ""),
            className: o.collapsed ? "is-collapsed" : ""
          };
        }));
      return buttons(row, items, function (i) {
        state.loci[m] = i;
        render();
      });
    });

    host.querySelector(".ll-dice").addEventListener("click", function () {
      state.epigenome = api.newEpigenomeCode();
      render();
    });

    host.querySelector(".ll-toggle input").addEventListener("change", function (e) {
      showFootprint = e.target.checked;
      paintChart();
    });

    /** The footprint in words and numbers, under the chart. */
    function describeFootprint(fp) {
      if (!fp || fp.empty || !fp.texels) {
        fpEl.innerHTML = '<span class="ll-fp-none">No pigment anywhere on this horse'
          + ' &mdash; nothing reads the chart.</span>';
        return;
      }
      function pct(v) { return Math.round(v * 100) + "%"; }
      fpEl.innerHTML = ''
        + '<div class="ll-fp-row"><span>red pigment</span><code>'
        + pct(fp.redMin) + ' &ndash; ' + pct(fp.redMax) + '</code></div>'
        + '<div class="ll-fp-row"><span>black pigment</span><code>'
        + pct(fp.blackMin) + ' &ndash; ' + pct(fp.blackMax) + '</code></div>'
        + '<div class="ll-fp-row"><span>chart X</span><code>'
        + pct(fp.minX) + ' &ndash; ' + pct(fp.maxX) + '</code></div>'
        + '<div class="ll-fp-row"><span>chart Y</span><code>'
        + pct(fp.minY) + ' &ndash; ' + pct(fp.maxY) + '</code></div>'
        + '<div class="ll-fp-row ll-fp-mid"><span>centre of mass</span><code>'
        + pct(fp.cx) + ', ' + pct(fp.cy) + '</code></div>'
        + '<div class="ll-fp-row"><span>pigmented texels</span><code>'
        + fp.texels + '</code></div>';
    }

    var codeEl = host.querySelector(".ll-code");

    function render() {
      // The base coat, then each active locus laid on it - always through the
      // mod, never by concatenating a code here (hard rule 3).
      var code = api.previewGenotypeCode(state.base, "", "");
      var parts = [];
      loci.forEach(function (locus, m) {
        var pick = state.loci[m];
        if (!pick) return;                       // 0 is "off"
        var outcome = locus.outcomes[pick - 1];
        var next = api.withGene(code, locus.key, outcome.tokens);
        if (!next) return;                       // did not resolve - leave it out
        code = next;
        parts.push(outcome.tokens);
      });
      if (!code) {
        codeEl.textContent = "that combination did not resolve";
        return;
      }

      view.setImage(toImageData(api.coatOf(code, state.epigenome, true)));

      // Where this exact horse reads the chart. 48 bins is finer than the eye
      // resolves at the size the canvas is drawn, and far cheaper than one cell
      // per texel.
      footprint = JSON.parse(api.lutFootprintJson(code, state.epigenome, true, 48));
      paintChart();
      describeFootprint(footprint);

      var baseIndex = indexOfBase(bases, state.base);
      codeEl.textContent = [luts[state.lut].name + " LUT", bases[baseIndex].name]
        .concat(parts).join(" · ");

      baseButtons.select(baseIndex);
      modButtons.forEach(function (row, m) { row.select(state.loci[m]); });
    }

    rebuildLutButtons();
    pickLut(0);
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
      b.className = "ll-btn" + (item.className ? " " + item.className : "");
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
    var hosts = document.querySelectorAll(".lut-lab");
    if (!hosts.length) return;
    stylesheet(HERE + "lut-lab.css");
    Array.prototype.forEach.call(hosts, mount);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }

  HG.lutLab = { mount: mount };
})(window.HG);
