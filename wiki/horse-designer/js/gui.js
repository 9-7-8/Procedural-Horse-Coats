// The horse designer's GUI - a deliberate redraw of CustomHorseSpawnScreen.
//
// KEEP THESE IN STEP. The spawn egg is the in-game gene tester and this is the
// browser one; a change to either belongs in both. Every layout constant below
// is copied from the Java by name and value, the colours are the same ARGB
// numbers, and the widgets are in the same order with the same labels - so the
// two screens are the same screen, and a divergence is a bug rather than a
// style choice.
//
// It is drawn on a canvas rather than built from DOM elements for exactly that
// reason: at 8px text on a virtual grid, with the geometry copied across, the
// layout can be compared side by side. A form full of <select>s could not be.
//
// What this file does NOT contain is any genetics. Every question it asks -
// what a row expresses, which allele a gene is added at, how a sex-linked row
// snaps - goes to the compiled Java in HG.java. This is a view.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // ---- layout, verbatim from CustomHorseSpawnScreen ---------------------
  var ROW_H = 20;
  var LIST_X = 8;
  var LIST_TOP = 40;
  var ALLELE_W = 38;
  var REMOVE_W = 14;
  var RIGHT_W = 96;
  var RIGHT_STEP = 22;
  var DD_ROW_H = 12;
  var DD_VISIBLE = 8;
  var DD_W = 76;
  var BREED_DD_W = 118;

  // ---- colours, the same ARGB the screen fills with ---------------------
  var PANEL = "rgba(0,0,0,0.565)";        // 0x90000000
  var NAME_BG = "rgba(0,0,0,0.88)";       // 0xE0000000 - opaque enough to read over grass
  var ROW_BG = "rgba(32,40,56,0.2)";      // 0x33202838
  var HOVER = "rgba(255,255,255,0.2)";    // 0x33FFFFFF
  var C_HEADER = "#9098A8";
  var C_NAME_OFF = "#9AA0B0";
  var C_NAME_ON = "#FFFFFF";
  var C_EXPRESSING = "#9BE08A";
  var C_ADDED = "#C8C8C8";
  var C_SUB = "#787888";
  var C_SHORT = "#88CC88";
  var C_EPI = "#8890A8";
  var C_BIG = "#E0C070";
  var C_SMALL = "#80B8D0";
  var DD_BG = "#0E0E16";
  var DD_BORDER = "#5A6478";
  var DD_SEL = "rgba(112,136,255,0.33)";
  var DD_TEXT = "#C0C4D0";
  var C_UNSHOWN = "#6B7182";   // a gene this page cannot draw at all

  function create(canvas, opts) {
    var ctx = canvas.getContext("2d");
    var scale = 3;
    var vw = 0, vh = 0;          // virtual (GUI) size, like Minecraft's gui scale
    var genes = [], breeds = [], state = null;
    var scroll = 0;
    var mouse = { x: -1, y: -1, inside: false };
    var widgets = [];            // rebuilt every draw, like the screen's init()
    var dd = null;               // { kind:"allele"|"breed", row, slot, x, y, scroll }

    function resize() {
      var w = canvas.clientWidth, h = canvas.clientHeight;
      if (!w || !h) return;
      // Minecraft picks the largest whole scale that still leaves a usable
      // 320x240 grid; the same rule keeps the panel legible on a laptop and
      // not absurd on a 4K monitor.
      scale = Math.max(2, Math.min(4, Math.floor(Math.min(w / 480, h / 320)) || 2));
      canvas.width = w * devicePixelRatio;
      canvas.height = h * devicePixelRatio;
      vw = Math.floor(w / scale);
      vh = Math.floor(h / scale);
    }

    // ---- geometry, verbatim ---------------------------------------------
    function listWidth() { return Math.max(150, Math.min(240, vw - RIGHT_W - 130)); }
    function listBottom() { return vh - 44; }
    function rightX() { return vw - RIGHT_W - 8; }
    function previewLeft() { return LIST_X + listWidth() + 8; }
    function previewRight() { return rightX() - 8; }
    function visibleRows() { return Math.max(1, Math.floor((listBottom() - LIST_TOP) / ROW_H)); }
    function maxScroll() { return Math.max(0, genes.length - visibleRows()); }
    function nameWidth(added) {
      return added ? listWidth() - 2 * ALLELE_W - REMOVE_W - 10 : listWidth() - 8;
    }
    function rowAt(mx, my) {
      if (mx < LIST_X - 4 || mx > LIST_X + listWidth()
        || my < LIST_TOP || my >= LIST_TOP + visibleRows() * ROW_H) return -1;
      var i = scroll + Math.floor((my - LIST_TOP) / ROW_H);
      return i < genes.length ? i : -1;
    }

    /** Where the horse should be framed, in CSS pixels - the screen's preview box. */
    function previewRect() {
      return {
        x: previewLeft() * scale, y: LIST_TOP * scale,
        w: (previewRight() - previewLeft()) * scale, h: (vh - 30 - LIST_TOP) * scale
      };
    }

    // ---- drawing helpers -------------------------------------------------
    function fill(x0, y0, x1, y1, colour) {
      ctx.fillStyle = colour;
      ctx.fillRect(x0, y0, x1 - x0, y1 - y0);
    }

    function text(s, x, y, colour) {
      ctx.fillStyle = colour;
      ctx.fillText(s, x, y + 7);
    }

    function widthOf(s) { return ctx.measureText(s).width; }

    /** Left-aligned, scaled down (never up) so the whole string fits - the screen's drawFitted. */
    function fitted(s, x, y, maxW, colour) {
      var w = widthOf(s);
      if (w <= maxW || w <= 0) { text(s, x, y, colour); return; }
      ctx.save();
      ctx.translate(x, y + 7);
      ctx.scale(maxW / w, maxW / w);
      ctx.fillStyle = colour;
      ctx.fillText(s, 0, 0);
      ctx.restore();
    }

    /**
     * Can this page show anything of the gene at all? Java decides
     * (DesignerApi.showsAs), derived from the gene rather than from a list here
     * - so a gene added later classifies itself.
     */
    function viewable(gene) {
      return gene.shows !== "ability" && gene.shows !== "stats";
    }

    /** A name with a line through it: real gene, nothing to see. */
    function struck(label, x, y, maxW, colour) {
      fitted(label, x, y, maxW, colour);
      var w = Math.min(widthOf(label), maxW);
      fill(x, y + 4, x + w, y + 5, colour);
    }

    function centred(s, x0, x1, y, colour) {
      text(s, (x0 + x1) / 2 - widthOf(s) / 2, y, colour);
    }

    function button(x, y, w, h, label, onClick, hot) {
      var over = mouse.x >= x && mouse.x < x + w && mouse.y >= y && mouse.y < y + h;
      fill(x, y, x + w, y + h, over ? "rgba(70,74,88,0.95)" : "rgba(30,32,42,0.92)");
      ctx.strokeStyle = over ? "#FFFFFF" : (hot ? "#8890A8" : "#5A6478");
      ctx.lineWidth = 1;
      ctx.strokeRect(x + 0.5, y + 0.5, w - 1, h - 1);
      fitted(label, x + 3, y + (h - 8) / 2, w - 6, over ? "#FFFFFF" : "#E0E4EC");
      widgets.push({ x: x, y: y, w: w, h: h, click: onClick });
    }

    // ---- the frame -------------------------------------------------------

    function draw() {
      resize();
      if (!vw || !genes.length || !state) return;
      widgets = [];
      scroll = Math.max(0, Math.min(scroll, maxScroll()));

      ctx.setTransform(scale * devicePixelRatio, 0, 0, scale * devicePixelRatio, 0, 0);
      ctx.clearRect(0, 0, vw, vh);
      ctx.textBaseline = "alphabetic";
      ctx.font = "8px ui-monospace, 'DejaVu Sans Mono', Menlo, Consolas, monospace";
      ctx.imageSmoothingEnabled = false;

      var hovered = dd ? -1 : rowAt(mouse.x, mouse.y);
      var shown = Math.min(genes.length, scroll + visibleRows()) - scroll;

      // Header band. The screen puts its title here; a horse has a better one,
      // so this is the name - and each half is its own button, which is where
      // "reroll either half" lives without costing a widget in the column.
      fill(0, 6, vw, 32, PANEL);
      drawName();

      // the name column only - stop short of an added row's allele buttons
      fill(LIST_X - 4, LIST_TOP - 14, LIST_X + nameWidth(true) + 2, LIST_TOP + shown * ROW_H, NAME_BG);
      text(maxScroll() > 0 ? "Genes - click to add  (scroll)" : "Genes - click to add",
        LIST_X, LIST_TOP - 12, C_HEADER);

      drawRows(hovered);
      drawRightColumn();
      drawGenomeLine();
      if (dd) drawDropdown();
    }

    /** The name, in two clickable halves. */
    function drawName() {
      var first = state.first || "?";
      var last = state.last || "?";
      var gap = 5;
      var wf = widthOf(first), wl = widthOf(last);
      var x = vw / 2 - (wf + gap + wl) / 2;
      half(first, x, wf, 1);
      half(last, x + wf + gap, wl, 2);
      text("click a half to reroll it", vw / 2 - widthOf("click a half to reroll it") / 2, 22, C_SUB);
    }

    function half(word, x, w, bit) {
      var y = 9;
      var over = mouse.x >= x - 2 && mouse.x < x + w + 2 && mouse.y >= y - 1 && mouse.y < y + 11;
      if (over) fill(x - 2, y - 1, x + w + 2, y + 11, HOVER);
      text(word, x, y, over ? "#FFFFFF" : "#E8EEF8");
      widgets.push({ x: x - 2, y: y - 1, w: w + 4, h: 12,
        click: function () { opts.edit("rerollName", bit); } });
    }

    function drawRows(hovered) {
      var listW = listWidth();
      var aX = LIST_X + listW - 2 * ALLELE_W - REMOVE_W - 6;
      var bX = LIST_X + listW - ALLELE_W - REMOVE_W - 4;
      var xX = LIST_X + listW - REMOVE_W;

      for (var i = scroll; i < genes.length && i < scroll + visibleRows(); i++) {
        var gene = genes[i];
        var row = state.rows[i];
        var ry = LIST_TOP + (i - scroll) * ROW_H;

        var canShow = viewable(gene);

        if (!row.added) {
          // off the horse: a plain name, the whole row clickable
          if (i === hovered) fill(LIST_X - 4, ry, LIST_X + listW, ry + ROW_H - 2, HOVER);
          var offColour = canShow ? (i === hovered ? C_NAME_ON : C_NAME_OFF) : C_UNSHOWN;
          if (canShow) fitted(gene.name, LIST_X, ry + 6, nameWidth(false), offColour);
          else struck(gene.name, LIST_X, ry + 6, nameWidth(false), offColour);
          widgets.push({ x: LIST_X - 4, y: ry, w: listW + 4, h: ROW_H - 2, click: add(i) });
          continue;
        }

        // on the horse: name, what it expresses, and its two allele buttons
        fill(LIST_X - 4, ry, LIST_X + nameWidth(true) + 2, ry + ROW_H - 2, ROW_BG);
        var onColour = !canShow ? C_UNSHOWN : (row.expressing ? C_EXPRESSING : C_ADDED);
        if (canShow) fitted(gene.name, LIST_X, ry + 1, nameWidth(true), onColour);
        else struck(gene.name, LIST_X, ry + 1, nameWidth(true), onColour);
        fitted(canShow ? row.expression : "not shown here", LIST_X, ry + 10, nameWidth(true), C_SUB);

        var many = gene.alleles.length > 3;
        button(aX, ry, ALLELE_W, ROW_H - 2, gene.alleles[row.a].token, slot(i, 0, many, aX, ry));
        button(bX, ry, ALLELE_W, ROW_H - 2, gene.alleles[row.b].token, slot(i, 1, many, bX, ry));
        button(xX, ry, REMOVE_W, ROW_H - 2, "x", remove(i));
      }
    }

    // Clicking a struck-through gene still adds it - it is a real gene and it
    // belongs in an exported horse - but says out loud that you will not see it.
    function add(i) {
      return function () {
        if (!viewable(genes[i])) opts.notShown(i);
        opts.edit("addGene", i);
      };
    }
    function remove(i) { return function () { opts.edit("removeGene", i); }; }

    function slot(i, s, many, x, y) {
      return function () {
        if (many) {
          dd = { kind: "allele", row: i, slot: s, x: x, y: clampDd(y), scroll: 0 };
          var cur = s === 0 ? state.rows[i].a : state.rows[i].b;
          dd.scroll = Math.max(0, Math.min(cur - (DD_VISIBLE >> 1),
            Math.max(0, genes[i].alleles.length - DD_VISIBLE)));
        } else {
          opts.edit("cycleAllele", i, s);
        }
      };
    }

    function clampDd(y) {
      return Math.max(LIST_TOP, Math.min(y, vh - DD_VISIBLE * DD_ROW_H - 4));
    }

    function drawRightColumn() {
      var rx = rightX();
      var ry = LIST_TOP + 4;
      var step = function () { ry += RIGHT_STEP; };

      button(rx, ry, RIGHT_W, 20, state.baby ? "Age: Foal" : "Age: Adult",
        function () { opts.edit("setBaby", !state.baby); });
      step();
      button(rx, ry, RIGHT_W, 20, state.female ? "Sex: Mare" : "Sex: Stallion",
        function () { opts.edit("setSex", !state.female); });
      step();
      var breedName = breeds[state.breedIndex] || "(none)";
      if (breedName.length > 12) breedName = breedName.slice(0, 11) + "…";
      var by = ry;
      button(rx, ry, RIGHT_W, 20, "Breed: " + breedName + " ▾", function () {
        dd = {
          kind: "breed", x: rx, y: clampDd(by),
          scroll: Math.max(0, Math.min(state.breedIndex - (DD_VISIBLE >> 1),
            Math.max(0, breeds.length - DD_VISIBLE)))
        };
      });
      step();
      button(rx, ry, RIGHT_W, 20, "Randomize", function () { opts.edit("randomize"); });
      step();
      button(rx, ry, RIGHT_W, 20, "Reroll epi.", function () { opts.edit("rerollEpigenome"); });
      step();
      button(rx, ry, RIGHT_W, 20, "Copy code", function () { opts.copyCode(); });
      step();
      button(rx, ry, RIGHT_W, 20, "Paste code", function () { opts.pasteCode(); });
      step();
      button(rx, ry, RIGHT_W, 20, "Clear genes", function () { opts.edit("clearGenes"); });

      // Where the screen has Spawn / Cancel there is nothing to spawn - the
      // horse is already standing in the field. These four are the browser's
      // own, and the only controls here with no counterpart in game.
      button(rx, vh - 92, RIGHT_W, 20, "Reroll name",
        function () { opts.edit("rerollName", 3); }, true);
      // Export and Import share a row - two halves of one idea, and the column
      // has no space to spare.
      var halfW = (RIGHT_W - 4) / 2;
      button(rx, vh - 70, halfW, 20, "Export", function () { opts.exportJson(); }, true);
      button(rx + halfW + 4, vh - 70, halfW, 20, "Import", function () { opts.importJson(); }, true);
      button(rx, vh - 48, RIGHT_W, 20, state.wander ? "Wander: on" : "Wander: off",
        function () { opts.toggleWander(); }, true);
      button(rx, vh - 26, RIGHT_W, 20, "Reset view", function () { opts.resetView(); }, true);
    }

    function drawGenomeLine() {
      var listW = listWidth();
      fill(LIST_X - 4, vh - 42, LIST_X + listW, vh - 14, NAME_BG);
      fitted(state.shortForm || "(wild type)", LIST_X, vh - 39, listW - 4, C_SHORT);
      fitted("epigenetics #" + state.epiFingerprint, LIST_X, vh - 26, listW - 4, C_EPI);

      // The size readout, centred under the preview - only when the horse is
      // not ordinary size, so an untouched screen looks exactly as it did.
      if (Math.abs(state.scale - 1) >= 0.005) {
        var s = "size " + state.scale.toFixed(2) + "x";
        centred(s, previewLeft(), previewRight(), vh - 41,
          state.scale > 1 ? C_BIG : C_SMALL);
      }
    }

    function drawDropdown() {
      var isBreed = dd.kind === "breed";
      var w = isBreed ? BREED_DD_W : DD_W;
      var items = isBreed ? breeds : genes[dd.row].alleles.map(function (a) { return a.token; });
      var current = isBreed ? state.breedIndex
        : (dd.slot === 0 ? state.rows[dd.row].a : state.rows[dd.row].b);
      var h = DD_VISIBLE * DD_ROW_H;

      fill(dd.x - 1, dd.y - 1, dd.x + w + 1, dd.y + h + 1, DD_BG);
      ctx.strokeStyle = DD_BORDER;
      ctx.strokeRect(dd.x - 0.5, dd.y - 0.5, w + 1, h + 1);

      for (var k = 0; k < DD_VISIBLE; k++) {
        var idx = dd.scroll + k;
        if (idx >= items.length) break;
        var ry = dd.y + k * DD_ROW_H;
        var over = mouse.x >= dd.x && mouse.x < dd.x + w && mouse.y >= ry && mouse.y < ry + DD_ROW_H;
        if (idx === current) fill(dd.x, ry, dd.x + w, ry + DD_ROW_H, DD_SEL);
        else if (over) fill(dd.x, ry, dd.x + w, ry + DD_ROW_H, HOVER);
        fitted(items[idx], dd.x + 3, ry + 2, w - 6, idx === current ? "#FFFFFF" : DD_TEXT);
      }

      if (items.length > DD_VISIBLE) {
        var barH = Math.max(6, h * DD_VISIBLE / items.length);
        var barY = dd.y + (h - barH) * dd.scroll / (items.length - DD_VISIBLE);
        fill(dd.x + w - 2, dd.y, dd.x + w, dd.y + h, "rgba(255,255,255,0.25)");
        fill(dd.x + w - 2, barY, dd.x + w, barY + barH, "#8890A8");
      }
      // The dropdown eats every click while it is open - any click resolves or
      // dismisses it, exactly as the screen does.
      widgets = [{ x: 0, y: 0, w: vw, h: vh, click: function () { pickFromDropdown(); } }];
    }

    function pickFromDropdown() {
      var isBreed = dd.kind === "breed";
      var w = isBreed ? BREED_DD_W : DD_W;
      var items = isBreed ? breeds.length : genes[dd.row].alleles.length;
      var h = DD_VISIBLE * DD_ROW_H;
      if (mouse.x >= dd.x && mouse.x < dd.x + w && mouse.y >= dd.y && mouse.y < dd.y + h) {
        var idx = dd.scroll + Math.floor((mouse.y - dd.y) / DD_ROW_H);
        if (idx >= 0 && idx < items) {
          if (isBreed) opts.edit("setBreed", idx);
          else opts.edit("setAllele", dd.row, dd.slot, idx);
        }
      }
      dd = null;
    }

    // ---- input -----------------------------------------------------------

    function toVirtual(e) {
      var r = canvas.getBoundingClientRect();
      mouse.x = (e.clientX - r.left) / scale;
      mouse.y = (e.clientY - r.top) / scale;
      mouse.inside = mouse.x >= 0 && mouse.y >= 0 && mouse.x < vw && mouse.y < vh;
    }

    /** Is this pixel over GUI furniture? If not, the field gets the event. */
    function overGui(e) {
      toVirtual(e);
      if (dd) return true;
      for (var i = 0; i < widgets.length; i++) {
        var wg = widgets[i];
        if (mouse.x >= wg.x && mouse.x < wg.x + wg.w && mouse.y >= wg.y && mouse.y < wg.y + wg.h) {
          return true;
        }
      }
      // the two opaque panels, so a drag on them never spins the horse
      var listW = listWidth();
      if (mouse.x < LIST_X + listW && mouse.y > LIST_TOP - 16) return true;
      if (mouse.x > rightX() - 4) return true;
      if (mouse.y < 32) return true;
      return false;
    }

    /** Events that belong to real DOM chrome, not to the canvas GUI. */
    function isDom(e) {
      var t = e.target;
      while (t && t !== document.body) {
        if (t.classList && (t.classList.contains("toasts") || t.classList.contains("boot")
          || t.classList.contains("back"))) return true;
        t = t.parentNode;
      }
      return false;
    }

    // Listeners go on the window in the CAPTURE phase, not on the GUI canvas.
    // The canvas covers the field, so a listener there could only ever swallow
    // events - stopPropagation does not hand them on to what is underneath.
    // Capturing at the window lets the GUI take what it wants first and simply
    // decline the rest, which then reaches the field and spins the horse.
    window.addEventListener("pointermove", function (e) {
      if (isDom(e)) return;
      toVirtual(e);
    }, true);

    window.addEventListener("pointerdown", function (e) {
      if (isDom(e) || !overGui(e)) return;      // let it fall through to the field
      e.stopPropagation();
      e.preventDefault();
      for (var i = widgets.length - 1; i >= 0; i--) {
        var wg = widgets[i];
        if (mouse.x >= wg.x && mouse.x < wg.x + wg.w && mouse.y >= wg.y && mouse.y < wg.y + wg.h) {
          wg.click();
          return;
        }
      }
      if (dd) dd = null;
    }, true);

    window.addEventListener("wheel", function (e) {
      if (isDom(e)) return;
      toVirtual(e);
      var dir = e.deltaY > 0 ? 1 : -1;
      if (dd) {
        var items = dd.kind === "breed" ? breeds.length : genes[dd.row].alleles.length;
        dd.scroll = Math.max(0, Math.min(dd.scroll + dir, Math.max(0, items - DD_VISIBLE)));
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      if (mouse.x < LIST_X + listWidth() && mouse.y > LIST_TOP - 16) {
        scroll = Math.max(0, Math.min(scroll + dir, maxScroll()));
        e.preventDefault();
        e.stopPropagation();
      }
    }, { passive: false, capture: true });

    return {
      setData: function (g, b) { genes = g; breeds = b; },
      setState: function (s) { state = s; },
      draw: draw,
      previewRect: previewRect,
      overGui: overGui,
      guiScale: function () { return scale; }
    };
  }

  HG.gui = { create: create };
})(window.HG);
