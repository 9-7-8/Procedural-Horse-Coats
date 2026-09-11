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
// The deliberate divergences, both the same one: a browser page has no world
// and no inventory, so the screen's two output buttons have no twin here.
// "Spawn" has nothing to spawn into; "Make egg" (which writes the horse on
// screen into a preset_horse_spawn_egg item) has no inventory to put an item
// in. Export / Import sit in the slot the screen gives Copy horse / Paste
// horse, and carry the identical payload - HorseFile, the whole animal - to a
// file rather than to a clipboard, because a browser tab has no chat to paste
// into and a Minecraft screen has no file picker. Nothing else may differ.
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
  var RIGHT_W = 128;      // was 96 - several labels were wider than the button
  var RIGHT_STEP = 24;    // 20-high buttons with a 4px gutter (was 22, i.e. 2px)
  var RIGHT_STEP_MIN = 20;    // the tightest gutter, i.e. buttons touching
  var RIGHT_ROWS = 7;     // Age, Sex, Breed, Randomize, Add, Rnd health, Clear
  var BLURB_W = 176;      // the hover blurb panel - see drawGeneBlurb
  var BLURB_LINE_H = 10;
  var BLURB_PAD = 5;
  var DD_ROW_H = 12;
  var DD_VISIBLE = 8;
  var DD_W = 76;
  var BREED_DD_W = 118;
  var LOCK_W = 10;        // the padlock column down the left of the gene list
  var FILTER_H = 14;      // the family filter sitting above it
  var ARROW_W = 14;       // the arrow half of a split button
  var MENU_DD_W = 118;    // the randomize / add-random / family menus

  // ---- colours, the same ARGB the screen fills with ---------------------
  var PANEL = "rgba(0,0,0,0.565)";        // 0x90000000
  var NAME_BG = "rgba(0,0,0,0.88)";       // 0xE0000000 - opaque enough to read over grass
  var ROW_BG = "rgba(32,40,56,0.2)";      // 0x33202838
  var HOVER = "rgba(255,255,255,0.2)";    // 0x33FFFFFF
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
  var C_LOCK_ON = "#E8C060";   // a locked row - the same amber the size readout uses
  var C_LOCK_OFF = "#565C6C";  // an unlocked one: present, and clearly not doing anything

  function create(canvas, opts) {
    var ctx = canvas.getContext("2d");
    var scale = 3;
    var vw = 0, vh = 0;          // virtual (GUI) size, like Minecraft's gui scale
    var genes = [], breeds = [], state = null;
    var families = [], modes = [], addScopes = [];
    var filter = "";             // "" = every gene; otherwise a GeneFamily name
    var search = "";             // the search box - the screen's EditBox
    var searchFocused = false;   // typing goes to the box while this is true
    var searchHits = null;       // { geneIndex: true } for searchFor, from Java
    var searchFor = null;
    var view = [];               // indices into genes[], after the filter and search
    var scroll = 0;
    var mouse = { x: -1, y: -1, inside: false };
    var widgets = [];            // rebuilt every draw, like the screen's init()
    var dd = null;               // { kind:"allele"|"breed"|"family"|"randomize"|"add", ... }

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
    // The group pinned to the bottom of the right column, and the gutter the
    // top group gets. Both verbatim from CustomHorseSpawnScreen - see
    // bottomStackTop() / rightStep() there for why the spacing can give.
    // Three rows, not four - the screen moved its Spawn button under the preview
    // and this moved Reroll name to the same place, so the two columns stay the
    // same shape. See CustomHorseSpawnScreen.bottomStackTop().
    function bottomStackTop() { return vh - 26 - 2 * RIGHT_STEP; }
    /** The screen's spawnButtonBounds(), for the button under the horse. */
    function underPreview() {
      var x0 = previewLeft(), x1 = previewRight();
      var w = Math.max(60, Math.min(140, x1 - x0 - 8));
      return { x: (x0 + x1) / 2 - w / 2, y: vh - 26, w: w };
    }
    function rightStep() {
      var available = bottomStackTop() - 8 - (LIST_TOP + 4);
      var fits = Math.floor((available - 20) / (RIGHT_ROWS - 1));
      return Math.max(RIGHT_STEP_MIN, Math.min(RIGHT_STEP, fits));
    }
    function previewLeft() { return LIST_X + listWidth() + 8; }
    function previewRight() { return rightX() - 8; }
    function visibleRows() { return Math.max(1, Math.floor((listBottom() - LIST_TOP) / ROW_H)); }
    function maxScroll() { return Math.max(0, view.length - visibleRows()); }
    function nameX() { return LIST_X + LOCK_W; }
    function nameWidth(added) {
      return added ? listWidth() - LOCK_W - 2 * ALLELE_W - REMOVE_W - 10
        : listWidth() - LOCK_W - 8;
    }

    /**
     * Which genes the list is showing, after the family filter and the search.
     * Recomputed every frame rather than cached: it is a walk of a few hundred
     * strings and a cache would be one more thing to invalidate when a gene is
     * added. The search itself is asked of Java (EditorRules.matchesSearch, via
     * opts.search) and only re-asked when the query changes.
     */
    function rebuildView() {
      if (search.trim() && searchFor !== search) {
        searchHits = {};
        var hits = opts.search ? opts.search(search) : [];
        for (var h = 0; h < hits.length; h++) searchHits[hits[h]] = true;
        searchFor = search;
      }
      var searching = !!search.trim();
      view = [];
      for (var i = 0; i < genes.length; i++) {
        if ((!filter || genes[i].family === filter) && (!searching || searchHits[i])) view.push(i);
      }
    }

    /**
     * The <b>view slot</b> under a point, or -1 - i.e. which drawn row, which is
     * what positions anything anchored to a row. This is what the screen's
     * rowAt() returns; rowAt() here returns the gene index instead, because the
     * screen's rows carry their own gene and these are bare indices into
     * genes[]. Keeping both named separately is the point: they differ whenever
     * a family filter is on, and confusing them puts a panel beside the wrong
     * row.
     */
    function rowSlotAt(mx, my) {
      if (mx < LIST_X - 4 || mx > LIST_X + listWidth()
        || my < LIST_TOP || my >= LIST_TOP + visibleRows() * ROW_H) return -1;
      var k = scroll + Math.floor((my - LIST_TOP) / ROW_H);
      return k < view.length ? k : -1;
    }

    /** The gene index under a point, or -1. Rows are addressed through the view. */
    function rowAt(mx, my) {
      var k = rowSlotAt(mx, my);
      return k < 0 ? -1 : view[k];
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

    /**
     * A padlock, drawn rather than typed. Minecraft's font has no lock glyph
     * outside the astral planes it cannot reach, and this has to be the same
     * mark on both screens - so both draw the same eight rectangles.
     */
    function padlock(x, y, locked, colour) {
      var bx = x + 2, by = y + 6;            // 5x6 icon, centred in the column
      if (locked) {
        fill(bx + 1, by, bx + 4, by + 1, colour);       // shackle, closed
        fill(bx + 1, by + 1, bx + 2, by + 2, colour);
        fill(bx + 3, by + 1, bx + 4, by + 2, colour);
      } else {
        fill(bx + 2, by, bx + 5, by + 1, colour);       // shackle, swung open
        fill(bx + 4, by + 1, bx + 5, by + 2, colour);
      }
      fill(bx, by + 2, bx + 5, by + 6, colour);         // body
    }

    /** A button and, glued to its right, the arrow that opens its menu. */
    function splitButton(x, y, w, label, onClick, onMenu) {
      button(x, y, w - ARROW_W, 20, label, onClick, true);
      button(x + w - ARROW_W, y, ARROW_W, 20, "\u25be", onMenu, true);
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

      rebuildView();
      scroll = Math.max(0, Math.min(scroll, maxScroll()));
      var hovered = dd ? -1 : rowAt(mouse.x, mouse.y);
      var shown = Math.min(view.length, scroll + visibleRows()) - scroll;

      // Header band. The screen puts its title here; a horse has a better one,
      // so this is the name - and each half is its own button, which is where
      // "reroll either half" lives without costing a widget in the column.
      fill(0, 6, vw, 32, PANEL);
      drawName();

      // the name column only - stop short of an added row's allele buttons
      fill(LIST_X - 4, LIST_TOP - 2, LIST_X + nameWidth(true) + LOCK_W + 2,
        LIST_TOP + shown * ROW_H, NAME_BG);

      drawFilter();
      drawRows(hovered);
      drawRightColumn();
      drawGenomeLine();
      // Last but one, so it sits over the preview and the genome line - but not
      // over an open dropdown, which is the thing you are actually pointing at.
      // (hovered is already -1 while a dropdown is open.)
      if (hovered >= 0) drawGeneBlurb(genes[hovered], rowSlotAt(mouse.x, mouse.y));
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

    /**
     * Search and the family filter, sharing the row above the list half each -
     * the screen's init(), same split. Hundreds of loci in one alphabetical
     * column is a list you scroll rather than read; the filter asks for the
     * dilutions, the search for the one gene you already know the name of.
     */
    function drawFilter() {
      var w = listWidth() + 4;
      var y = LIST_TOP - FILTER_H - 2;
      var searchW = Math.floor(w / 2);
      drawSearch(LIST_X - 4, y, searchW, FILTER_H);

      var familyX = LIST_X - 4 + searchW + 2;
      var familyW = w - searchW - 2;
      var label = "All genes";
      for (var i = 0; i < families.length; i++) {
        if (families[i].key === filter) label = families[i].label;
      }
      var fy = y;
      button(familyX, y, familyW, FILTER_H, label + "  \u25be", function () {
        dd = {
          kind: "family", x: familyX, y: clampDd(fy + FILTER_H),
          scroll: Math.max(0, Math.min(indexOfFamily() - (DD_VISIBLE >> 1),
            Math.max(0, families.length - DD_VISIBLE)))
        };
      }, true);
    }

    /** The search box, drawn the way Minecraft draws an EditBox: black, a border, a caret. */
    function drawSearch(x, y, w, h) {
      fill(x, y, x + w, y + h, searchFocused ? "#FFFFFF" : "#A0A0A0");
      fill(x + 1, y + 1, x + w - 1, y + h - 1, "#000000");
      var tx = x + 4, ty = y + (h - 8) / 2;
      if (search) {
        // Show the end of a long query, as an EditBox scrolls to its cursor.
        var shown = search;
        while (shown.length > 1 && widthOf(shown) > w - 10) shown = shown.substring(1);
        text(shown, tx, ty, "#E0E0E0");
        tx += widthOf(shown);
      } else if (!searchFocused) {
        text("Search genes", tx, ty, "#707070");
      }
      if (searchFocused && Math.floor(Date.now() / 300) % 2 === 0) {
        fill(tx, ty - 1, tx + 1, ty + 9, "#D0D0D0");
      }
      widgets.push({ x: x, y: y, w: w, h: h, click: function () { searchFocused = true; } });
    }

    function setSearch(s) {
      search = s;
      scroll = 0;
      draw();
    }

    function indexOfFamily() {
      for (var i = 0; i < families.length; i++) if (families[i].key === filter) return i;
      return 0;
    }

    function drawRows(hovered) {
      var listW = listWidth();
      var aX = LIST_X + listW - 2 * ALLELE_W - REMOVE_W - 6;
      var bX = LIST_X + listW - ALLELE_W - REMOVE_W - 4;
      var xX = LIST_X + listW - REMOVE_W;

      var nx = nameX();

      for (var k = scroll; k < view.length && k < scroll + visibleRows(); k++) {
        var i = view[k];
        var gene = genes[i];
        var row = state.rows[i];
        var ry = LIST_TOP + (k - scroll) * ROW_H;

        var canShow = viewable(gene);
        drawLock(i, row, ry);

        if (!row.added) {
          // off the horse: a plain name, the whole row clickable
          if (i === hovered) fill(nx - 2, ry, LIST_X + listW, ry + ROW_H - 2, HOVER);
          var offColour = canShow ? (i === hovered ? C_NAME_ON : C_NAME_OFF) : C_UNSHOWN;
          if (canShow) fitted(gene.name, nx, ry + 6, nameWidth(false), offColour);
          else struck(gene.name, nx, ry + 6, nameWidth(false), offColour);
          // Starts at nx, not at the hover fill's nx-2: the padlock is pushed
          // first and the later widget wins the hit test, so a row that
          // overlapped the lock column would swallow its clicks.
          widgets.push({ x: nx, y: ry, w: LIST_X + listW - nx, h: ROW_H - 2, click: add(i) });
          continue;
        }

        // on the horse: name, what it expresses, and its two allele buttons
        fill(nx - 2, ry, nx + nameWidth(true) + 2, ry + ROW_H - 2, ROW_BG);
        var onColour = !canShow ? C_UNSHOWN : (row.expressing ? C_EXPRESSING : C_ADDED);
        if (canShow) fitted(gene.name, nx, ry + 1, nameWidth(true), onColour);
        else struck(gene.name, nx, ry + 1, nameWidth(true), onColour);
        fitted(canShow ? row.expression : "not shown here", nx, ry + 10, nameWidth(true), C_SUB);

        var many = gene.alleles.length > 3;
        button(aX, ry, ALLELE_W, ROW_H - 2, gene.alleles[row.a].token, slot(i, 0, many, aX, ry));
        button(bX, ry, ALLELE_W, ROW_H - 2, gene.alleles[row.b].token, slot(i, 1, many, bX, ry));
        // Extension, agouti and shade carry no x - every horse has alleles at
        // all three, so there is no state in which taking one off is honest.
        if (!gene.alwaysCarried) {
          button(xX, ry, REMOVE_W, ROW_H - 2, "x", remove(i));
        }
      }
    }

    /**
     * The padlock at the head of a row. Locked means every randomize - the main
     * one and the epigenetic one - leaves this gene exactly as it stands, which
     * is what turns Randomize from "another horse" into "another horse, keeping
     * this".
     */
    function drawLock(i, row, ry) {
      var over = mouse.x >= LIST_X - 4 && mouse.x < LIST_X + LOCK_W
        && mouse.y >= ry && mouse.y < ry + ROW_H - 2;
      if (over) fill(LIST_X - 4, ry, LIST_X + LOCK_W, ry + ROW_H - 2, HOVER);
      padlock(LIST_X - 2, ry, row.locked, row.locked ? C_LOCK_ON : C_LOCK_OFF);
      widgets.push({
        x: LIST_X - 4, y: ry, w: LOCK_W + 4, h: ROW_H - 2,
        click: function () { opts.edit("setLocked", i, !row.locked); }
      });
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
      return Math.max(4, Math.min(y, vh - DD_VISIBLE * DD_ROW_H - 4));
    }

    /** A menu of {key,label} entries, anchored under a split button's arrow. */
    function openMenu(kind, x, y, items, current) {
      var at = 0;
      for (var i = 0; i < items.length; i++) if (items[i].key === current) at = i;
      dd = {
        kind: kind, x: x, y: clampDd(y),
        scroll: Math.max(0, Math.min(at - (DD_VISIBLE >> 1),
          Math.max(0, items.length - DD_VISIBLE)))
      };
    }

    /**
     * What this gene does, while you are pointing at it - the screen's
     * drawGeneBlurb, fill for fill. It sits to the right of the list, which is
     * empty space on both, and is pushed back inside the window rather than
     * being allowed to run off the edge. A gene whose description is empty
     * draws no panel at all: Gene.description() is documented as possibly
     * empty, and an empty box is worse than nothing.
     */
    function drawGeneBlurb(gene, hoveredIndex) {
      var blurb = gene.description;
      if (!blurb) return;
      var lines = wrap(blurb, BLURB_W - 2 * BLURB_PAD);

      var h = BLURB_PAD * 2 + BLURB_LINE_H + 2 + lines.length * BLURB_LINE_H;
      var x = LIST_X + listWidth() + 6;
      var y = LIST_TOP + (hoveredIndex - scroll) * ROW_H - 2;
      x = Math.max(4, Math.min(x, vw - BLURB_W - 4));
      y = Math.max(4, Math.min(y, vh - h - 4));

      fill(x - 1, y - 1, x + BLURB_W + 1, y + h + 1, "rgba(14,14,22,0.94)");
      fill(x - 1, y - 1, x + BLURB_W + 1, y, "#5A6478");
      fitted(gene.name, x + BLURB_PAD, y + BLURB_PAD, BLURB_W - 2 * BLURB_PAD, "#FFFFFF");
      var ty = y + BLURB_PAD + BLURB_LINE_H + 2;
      for (var i = 0; i < lines.length; i++) {
        text(lines[i], x + BLURB_PAD, ty, "#C0C4D0");
        ty += BLURB_LINE_H;
      }
    }

    /**
     * Greedy word wrap to a pixel width - the screen's wrap(). A single word
     * longer than the line is left long rather than broken mid-word.
     */
    function wrap(s, maxW) {
      var lines = [], line = "";
      var words = s.split(" ");
      for (var i = 0; i < words.length; i++) {
        if (!words[i]) continue;
        var candidate = line ? line + " " + words[i] : words[i];
        if (widthOf(candidate) <= maxW || !line) {
          line = candidate;
        } else {
          lines.push(line);
          line = words[i];
        }
      }
      if (line) lines.push(line);
      return lines;
    }

    function drawRightColumn() {
      var rx = rightX();
      var ry = LIST_TOP + 4;
      var rs = rightStep();
      var step = function () { ry += rs; };

      button(rx, ry, RIGHT_W, 20, state.baby ? "Age: Foal" : "Age: Adult",
        function () { opts.edit("setBaby", !state.baby); });
      step();
      button(rx, ry, RIGHT_W, 20, state.female ? "Sex: Mare" : "Sex: Stallion",
        function () { opts.edit("setSex", !state.female); });
      step();
      // No fixed character cut: button() fits its label to the width, which is
      // what the screen's truncate() now does too.
      var breedName = breeds[state.breedIndex] || "(none)";
      var by = ry;
      button(rx, ry, RIGHT_W, 20, "Breed: " + breedName + " ▾", function () {
        dd = {
          kind: "breed", x: rx, y: clampDd(by),
          scroll: Math.max(0, Math.min(state.breedIndex - (DD_VISIBLE >> 1),
            Math.max(0, breeds.length - DD_VISIBLE)))
        };
      });
      step();
      // Two split buttons. The face does the thing; the arrow picks which thing
      // it is, and the choice sticks - the label always says what will happen.
      var randY = ry;
      splitButton(rx, ry, RIGHT_W, state.randomizeLabel,
        function () { opts.edit("randomize"); },
        function () { openMenu("randomize", rx, randY + 20, modes, state.randomizeMode); });
      step();
      var addY = ry;
      splitButton(rx, ry, RIGHT_W, state.addLabel,
        function () { opts.edit("addRandom"); },
        function () { openMenu("add", rx, addY + 20, addScopes, state.addScope); });
      step();
      button(rx, ry, RIGHT_W, 20,
        state.randomizeInvisible ? "Rnd health: on" : "Rnd health: off",
        function () { opts.edit("setRandomizeInvisible", !state.randomizeInvisible); });
      step();
      button(rx, ry, RIGHT_W, 20, "Clear genes", function () { opts.edit("clearGenes"); });

      // Where the screen has Spawn / Cancel there is nothing to spawn - the
      // horse is already standing in the field. These are the browser's own,
      // and the only controls here with no counterpart in game.
      // Where the screen puts Spawn. There is nothing to spawn here, so the
      // browser's own primary action takes the slot.
      var under = underPreview();
      button(under.x, under.y, under.w, 20, "Reroll name",
        function () { opts.edit("rerollName", 3); }, true);
      // Export and Import share a row - two halves of one idea, and the column
      // has no space to spare. They sit in the slot the screen gives Copy horse
      // and Paste horse, which write the same format to the clipboard.
      var halfW = (RIGHT_W - 4) / 2;
      button(rx, bottomStackTop(), halfW, 20, "Export", function () { opts.exportJson(); }, true);
      button(rx + halfW + 4, bottomStackTop(), halfW, 20, "Import", function () { opts.importJson(); }, true);
      button(rx, bottomStackTop() + RIGHT_STEP, RIGHT_W, 20, state.wander ? "Wander: on" : "Wander: off",
        function () { opts.toggleWander(); }, true);
      button(rx, bottomStackTop() + 2 * RIGHT_STEP, RIGHT_W, 20, "Reset view", function () { opts.resetView(); }, true);
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

    /** {labels, currentIndex, width} for whichever dropdown is open. */
    function ddContents() {
      if (dd.kind === "breed") {
        return { labels: breeds, current: state.breedIndex, w: BREED_DD_W };
      }
      if (dd.kind === "allele") {
        return {
          labels: genes[dd.row].alleles.map(function (a) { return a.token; }),
          current: dd.slot === 0 ? state.rows[dd.row].a : state.rows[dd.row].b,
          w: DD_W
        };
      }
      var src = dd.kind === "family" ? families : (dd.kind === "randomize" ? modes : addScopes);
      var key = dd.kind === "family" ? filter
        : (dd.kind === "randomize" ? state.randomizeMode : state.addScope);
      var at = 0;
      var labels = [];
      for (var i = 0; i < src.length; i++) {
        labels.push(src[i].label);
        if (src[i].key === key) at = i;
      }
      return { labels: labels, current: at, w: MENU_DD_W };
    }

    function drawDropdown() {
      var c = ddContents();
      var w = c.w;
      var items = c.labels;
      var current = c.current;
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
      var c = ddContents();
      var h = DD_VISIBLE * DD_ROW_H;
      var kind = dd.kind, row = dd.row, slot = dd.slot;
      var idx = -1;
      if (mouse.x >= dd.x && mouse.x < dd.x + c.w && mouse.y >= dd.y && mouse.y < dd.y + h) {
        var hit = dd.scroll + Math.floor((mouse.y - dd.y) / DD_ROW_H);
        if (hit >= 0 && hit < c.labels.length) idx = hit;
      }
      dd = null;
      if (idx < 0) return;
      if (kind === "breed") opts.edit("setBreed", idx);
      else if (kind === "allele") opts.edit("setAllele", row, slot, idx);
      else if (kind === "family") { filter = families[idx].key; scroll = 0; draw(); }
      // Picking a mode also runs it. The menu is how you say what Randomize
      // means, and having said it you wanted it done - the label keeps the
      // choice for next time.
      else if (kind === "randomize") { opts.edit("setRandomizeMode", modes[idx].key); opts.edit("randomize"); }
      else if (kind === "add") { opts.edit("setAddScope", addScopes[idx].key); opts.edit("addRandom"); }
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
      if (mouse.x < LIST_X + listW && mouse.y > LIST_TOP - FILTER_H - 4) return true;
      if (mouse.x > rightX() - 4) return true;
      if (mouse.y < 32) return true;
      return false;
    }

    /** Events that belong to real DOM chrome, not to the canvas GUI. */
    function isDom(e) {
      var t = e.target;
      while (t && t !== document.body) {
        if (t.classList && (t.classList.contains("boot")
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
      if (isDom(e)) return;
      // Any click unfocuses the search box; a click on the box re-focuses it
      // through its own widget below. The screen's EditBox behaves the same.
      searchFocused = false;
      if (!overGui(e)) return;                  // let it fall through to the field
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
        var items = ddContents().labels.length;
        dd.scroll = Math.max(0, Math.min(dd.scroll + dir, Math.max(0, items - DD_VISIBLE)));
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      if (mouse.x < LIST_X + listWidth() && mouse.y > LIST_TOP - FILTER_H - 4) {
        scroll = Math.max(0, Math.min(scroll + dir, maxScroll()));
        e.preventDefault();
        e.stopPropagation();
      }
    }, { passive: false, capture: true });

    // Typing into the search box. Capture phase on the window, like the pointer
    // listeners, so a focused box takes the key before scene.js reads it as
    // walking the horse. Esc clears and lets go; Enter just lets go.
    window.addEventListener("keydown", function (e) {
      if (!searchFocused) return;
      if (e.ctrlKey || e.metaKey || e.altKey) return;   // shortcuts; a paste arrives as its own event
      if (e.key === "Escape") { searchFocused = false; setSearch(""); }
      else if (e.key === "Enter") { searchFocused = false; }
      else if (e.key === "Backspace") { setSearch(search.slice(0, -1)); }
      else if (e.key.length === 1) {
        if (search.length < 40) setSearch(search + e.key);   // the EditBox's setMaxLength
      }
      else return;
      e.preventDefault();
      e.stopPropagation();
    }, true);

    window.addEventListener("paste", function (e) {
      if (!searchFocused) return;
      var t = (e.clipboardData && e.clipboardData.getData("text")) || "";
      setSearch((search + t.replace(/[\r\n]+/g, " ")).slice(0, 40));
      e.preventDefault();
      e.stopPropagation();
    }, true);

    return {
      setData: function (g, b, f, m, a) {
        genes = g;
        breeds = b;
        families = f;
        modes = m;
        addScopes = a;
      },
      setState: function (s) { state = s; },
      draw: draw,
      previewRect: previewRect,
      overGui: overGui,
      guiScale: function () { return scale; }
    };
  }

  HG.gui = { create: create };
})(window.HG);
