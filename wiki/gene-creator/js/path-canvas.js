// The drawing surface for a PATH mask - a side view of the horse with the
// control points as draggable handles.
//
// It changes NO format. A PATH's `points` array is exactly what it always was
// and the textarea beside this still edits it; this is a second control over
// the same numbers, which is why the mask shipped first and the canvas after.
//
// Two things here have to stay true or the drawing lies about where the mark
// lands:
//
//  - the silhouette is the model's own boxes. Every part is drawn as its
//    body-space AABB projected into the plane, because that is precisely the
//    set of (u, v) the mask can reach - a texel of a part always lies inside
//    that part's box. It is blocky because the horse is blocky.
//  - the two axes are drawn at ONE scale, in body units. Fitting the plane to
//    the canvas box independently per axis would be prettier and would draw a
//    circle where the horse wears an ellipse.
//
// The curve is HG.specEngine.pathSpline, the same Catmull-Rom the painter
// walks, so a handle you drag bends the line the game will paint.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var geo = HG.geometry;

  // Body-space fraction the handles snap to. Coarse on purpose: these numbers
  // are committed into a gene file and read by a human, so 0.42 beats
  // 0.41873982696533203.
  var SNAP = 0.01;

  var HANDLE_R = 5.5;
  var INSERT_R = 3.5;
  var GRAB_R = 9;
  var MARGIN = 10;

  var live = [];

  function axesOf(mask) {
    var plane = mask.plane || "side";
    return {
      plane: plane,
      u: plane === "front" ? "Z" : "X",
      v: plane === "top" ? "Z" : "Y",
      // What the two ends mean, for the axis labels. Body space runs
      // tail -> nose on X, hoof -> ear on Y, and left -> right on Z.
      uEnds: plane === "front" ? ["left", "right"] : ["tail", "nose"],
      vEnds: plane === "top" ? ["left", "right"] : ["hoof", "ear"]
    };
  }

  /**
   * The mapping between the three coordinate systems in play: the numbers in
   * the file, body units, and canvas pixels.
   *
   * <p>{@code space: "body"} stores a 0..1 fraction of the whole-horse AABB and
   * {@code "units"} stores raw body units - the same distinction
   * SpecPainter's uMin/uSpan makes, and the reason this is a lookup rather
   * than an identity.
   */
  function frame(mask, skin, w, h) {
    var ax = axesOf(mask);
    var bb = geo.bodyBounds(skin);
    var norm = (mask.space || "body") !== "units";
    var uMin = bb.min(ax.u), uSpan = Math.max(1e-6, bb.span(ax.u));
    var vMin = bb.min(ax.v), vSpan = Math.max(1e-6, bb.span(ax.v));
    var scale = Math.min((w - 2 * MARGIN) / uSpan, (h - 2 * MARGIN) / vSpan);
    var ox = (w - uSpan * scale) / 2;
    var oy = (h - vSpan * scale) / 2;

    return {
      axes: ax,
      norm: norm,
      scale: scale,
      // body units -> canvas px
      px: function (unitU) { return ox + (unitU - uMin) * scale; },
      py: function (unitV) { return oy + (vMin + vSpan - unitV) * scale; },
      // the stored number -> body units, and back
      unitU: function (value) { return norm ? uMin + value * uSpan : value; },
      unitV: function (value) { return norm ? vMin + value * vSpan : value; },
      valueU: function (unit) { return norm ? (unit - uMin) / uSpan : unit; },
      valueV: function (unit) { return norm ? (unit - vMin) / vSpan : unit; },
      // canvas px -> body units
      atX: function (x) { return uMin + (x - ox) / scale; },
      atY: function (y) { return vMin + vSpan - (y - oy) / scale; },
      // A stored number rounded to what a person would type. In body space
      // that is three decimals; in units, two - a body unit is two texels.
      tidy: function (value) {
        var dp = norm ? 1000 : 100;
        return Math.round(value * dp) / dp;
      },
      snapU: function (unit) { return norm ? uMin + Math.round((unit - uMin) / uSpan / SNAP) * SNAP * uSpan : unit; },
      snapV: function (unit) { return norm ? vMin + Math.round((unit - vMin) / vSpan / SNAP) * SNAP * vSpan : unit; }
    };
  }

  /** The path as canvas points, curved exactly the way the painter curves it. */
  function polyline(pts, curve, closed, f) {
    var n = pts.length / 2;
    var out = [];
    if (n < 1) return out;
    out.push([f.px(f.unitU(pts[0])), f.py(f.unitV(pts[1]))]);
    if (n < 2) return out;
    var spans = closed ? n : n - 1;
    var steps = curve ? HG.schema.PATH_CURVE_SAMPLES : 1;
    for (var span = 0; span < spans; span++) {
      for (var step = 1; step <= steps; step++) {
        var u, v;
        if (curve) {
          var t = step / steps;
          u = HG.specEngine.pathSpline(pts, n, closed, span, t, 0);
          v = HG.specEngine.pathSpline(pts, n, closed, span, t, 1);
        } else {
          var j = (span + 1) % n;
          u = pts[j * 2];
          v = pts[j * 2 + 1];
        }
        out.push([f.px(f.unitU(u)), f.py(f.unitV(v))]);
      }
    }
    return out;
  }

  /**
   * Where clicking would insert a point - the middle of each drawn span, in
   * canvas pixels. On a curve that is the spline's own midpoint rather than
   * the chord's, so the marker sits on the line you can see.
   */
  function inserters(pts, curve, closed, f) {
    var n = pts.length / 2;
    var out = [];
    if (n < 2) return out;
    var spans = closed ? n : n - 1;
    for (var span = 0; span < spans; span++) {
      var u, v;
      if (curve) {
        u = HG.specEngine.pathSpline(pts, n, closed, span, 0.5, 0);
        v = HG.specEngine.pathSpline(pts, n, closed, span, 0.5, 1);
      } else {
        var j = (span + 1) % n;
        u = (pts[span * 2] + pts[j * 2]) / 2;
        v = (pts[span * 2 + 1] + pts[j * 2 + 1]) / 2;
      }
      out.push({ after: span, x: f.px(f.unitU(u)), y: f.py(f.unitV(v)) });
    }
    return out;
  }

  /**
   * <b>One PATH mask's drawing surface.</b>
   *
   * @param opts.mask     the mask object; read live, so plane / curve / fill
   *                      changes show without rebuilding the control
   * @param opts.name     the parameter holding the points ("points")
   * @param opts.minName  the parameter holding the MINIMAL shape ("pointsMin"),
   *                      edited on this same canvas behind a toggle. The two
   *                      arrays are twins - a point added to one is added to
   *                      the other - because the loader refuses a pair of
   *                      different lengths on both sides
   * @param opts.hasDial  () -> boolean, whether the gene has declared the dial
   *                      knob a minimal shape blends along. Without one the
   *                      file will not load, so the canvas declines to start a
   *                      minimal shape rather than writing a broken gene
   * @param opts.skinOf   () -> "ADULT" | "BABY", the horse being previewed
   * @param opts.widthOf  (raw, fallback) -> number, to draw the stroke at its
   *                      real width when it is a knob rather than a literal
   * @param opts.onEdit   called after every change; must NOT rebuild the DOM,
   *                      or the pointer capture dies mid-drag
   */
  function create(opts) {
    var mask = opts.mask;
    var name = opts.name;
    var selected = -1;
    var hoverAt = null;

    var canvas = document.createElement("canvas");
    canvas.className = "path-canvas";
    canvas.tabIndex = 0;

    var note = document.createElement("span");
    note.className = "hint path-note";

    var bar = document.createElement("div");
    bar.className = "row path-bar";

    var root = document.createElement("div");
    root.className = "stack path-editor";
    root.appendChild(canvas);
    root.appendChild(bar);
    root.appendChild(note);

    // Which of the two shapes the handles belong to. The other is drawn behind
    // as a ghost, so you are always editing one shape against the other rather
    // than against a memory of it.
    var editingMin = false;
    var minName = opts.minName || null;

    function pts() { return active() || (mask[activeName()] = []); }
    function activeName() { return editingMin && minName ? minName : name; }
    function active() { return mask[activeName()]; }
    /** The other array when there is one and it is in use, else null. */
    function twin() {
      var other = editingMin ? name : minName;
      if (!other) return null;
      var a = mask[other];
      return a && a.length ? a : null;
    }
    function hasMin() { return !!(minName && mask[minName] && mask[minName].length); }
    function count() { return Math.floor(pts().length / 2); }

    /**
     * <b>Every structural edit goes through these three</b>, because a point
     * added to one shape has to be added to the other. The parser refuses a
     * minimal shape with a different number of points - each one moves to its
     * twin, so there has to be one of each - and a canvas that let the two
     * drift apart would write a gene the game will not load.
     */
    function insertPoint(index, u, v) {
      pts().splice(index * 2, 0, u, v);
      var other = twin();
      if (!other) return;
      // The twin gets the same span's own midpoint, so the shape it already
      // describes does not move when a point is added to the other one.
      var n = other.length / 2;
      var before = Math.max(0, Math.min(n - 1, index - 1));
      var after = Math.min(n - 1, index);
      other.splice(index * 2, 0,
        (other[before * 2] + other[after * 2]) / 2,
        (other[before * 2 + 1] + other[after * 2 + 1]) / 2);
    }

    function removePoint(index) {
      pts().splice(index * 2, 2);
      var other = twin();
      if (other) other.splice(index * 2, 2);
    }

    function reverseBoth() {
      [active(), twin()].forEach(function (a) {
        if (!a) return;
        var out = [];
        for (var i = a.length - 2; i >= 0; i -= 2) out.push(a[i], a[i + 1]);
        a.length = 0;
        for (var j = 0; j < out.length; j++) a.push(out[j]);
      });
    }

    function tool(label, title, fn) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "btn tiny";
      b.textContent = label;
      b.title = title;
      b.addEventListener("click", function () { fn(); });
      bar.appendChild(b);
      return b;
    }

    tool("Reverse", "Flip the order of the points - which end is the start", function () {
      reverseBoth();
      if (selected >= 0) selected = count() - 1 - selected;
      edited();
    });
    tool("Clear", "Remove every point", function () {
      mask[name] = [];
      if (minName) delete mask[minName];
      editingMin = false;
      selected = -1;
      edited();
    });

    // ---- the minimal shape ----------------------------------------------

    if (minName) {
      var fullTab = tool("Full", "Edit the shape at this gene's fullest", function () {
        editingMin = false;
        selected = -1;
        edited();
      });
      var minTab = tool("Minimal", "Edit the shape it erases down to at its weakest",
        function () {
          if (!hasMin()) {
            seedMinimal();
          }
          editingMin = true;
          selected = -1;
          edited();
        });
      var dropMin = tool("Drop minimal", "Go back to one shape on every carrier", function () {
        delete mask[minName];
        editingMin = false;
        selected = -1;
        edited();
      });
    }

    /**
     * Start a minimal shape by <b>shrinking the drawn one</b> toward its own
     * centre, rather than by copying it or by starting empty.
     *
     * <p>A copy would make the two ends identical, so the gene would look
     * static until the author moved every point; empty is not a legal shape at
     * all. Shrunk is what "erases down to" means in the ordinary case, and it
     * is immediately visible on the slider - which is the point of seeding it
     * at all rather than asking for eight more clicks.
     */
    function seedMinimal() {
      var p = mask[name] || [];
      var n = p.length / 2;
      if (n < 2) return;
      var cu = 0, cv = 0;
      for (var i = 0; i < n; i++) { cu += p[i * 2]; cv += p[i * 2 + 1]; }
      cu /= n;
      cv /= n;
      var out = [];
      for (var j = 0; j < n; j++) {
        out.push(Math.round((cu + (p[j * 2] - cu) * 0.4) * 1000) / 1000,
          Math.round((cv + (p[j * 2 + 1] - cv) * 0.4) * 1000) / 1000);
      }
      mask[minName] = out;
    }

    function edited() {
      draw();
      if (opts.onEdit) opts.onEdit();
    }

    // ---- drawing ---------------------------------------------------------

    function sizeToBox() {
      var w = Math.max(200, canvas.clientWidth || root.clientWidth || 320);
      // The plane's own aspect, in body units, so the box is not a crop.
      var f0 = frame(mask, opts.skinOf(), w, w);
      var bb = geo.bodyBounds(opts.skinOf());
      var h = Math.round((w - 2 * MARGIN) * bb.span(f0.axes.v) / bb.span(f0.axes.u)) + 2 * MARGIN;
      h = Math.max(120, Math.min(360, h));
      var dpr = window.devicePixelRatio || 1;
      canvas.style.height = h + "px";
      canvas.width = Math.round(w * dpr);
      canvas.height = Math.round(h * dpr);
      return { w: w, h: h, dpr: dpr };
    }

    function draw() {
      var box = sizeToBox();
      var skin = opts.skinOf();
      var f = frame(mask, skin, box.w, box.h);
      var ctx = canvas.getContext("2d");
      ctx.setTransform(box.dpr, 0, 0, box.dpr, 0, 0);
      ctx.clearRect(0, 0, box.w, box.h);

      ctx.fillStyle = "#0d1422";
      ctx.fillRect(0, 0, box.w, box.h);

      drawHorse(ctx, f, skin);
      drawGrid(ctx, f, skin, box);
      drawGhost(ctx, f);
      drawShape(ctx, f);
      drawHandles(ctx, f);
      drawReadout(ctx, f, box);
      renderNote();
      syncTabs();
    }

    function drawHorse(ctx, f, skin) {
      var mesh = geo.mesh(skin);
      mesh.partNames.forEach(function (part) {
        var b = geo.bounds(skin, part);
        var x0 = f.px(b.min(f.axes.u)), x1 = f.px(b.max(f.axes.u));
        var y0 = f.py(b.max(f.axes.v)), y1 = f.py(b.min(f.axes.v));
        ctx.fillStyle = "rgba(46, 62, 90, 0.72)";
        ctx.fillRect(x0, y0, x1 - x0, y1 - y0);
        ctx.strokeStyle = "rgba(84, 108, 150, 0.5)";
        ctx.lineWidth = 1;
        ctx.strokeRect(x0 + 0.5, y0 + 0.5, x1 - x0 - 1, y1 - y0 - 1);
      });
    }

    function drawGrid(ctx, f, skin, box) {
      var bb = geo.bodyBounds(skin);
      ctx.strokeStyle = "rgba(120, 150, 200, 0.14)";
      ctx.fillStyle = "rgba(147, 163, 189, 0.75)";
      ctx.font = "9px ui-monospace, Consolas, monospace";
      ctx.lineWidth = 1;
      for (var i = 0; i <= 4; i++) {
        var fr = i / 4;
        var x = f.px(bb.min(f.axes.u) + fr * bb.span(f.axes.u));
        var y = f.py(bb.min(f.axes.v) + fr * bb.span(f.axes.v));
        ctx.beginPath();
        ctx.moveTo(x, 0); ctx.lineTo(x, box.h);
        ctx.moveTo(0, y); ctx.lineTo(box.w, y);
        ctx.stroke();
        if (i > 0 && i < 4) {
          ctx.fillText(fr.toFixed(2), x + 2, box.h - 3);
          ctx.fillText(fr.toFixed(2), 2, y - 2);
        }
      }
      ctx.fillStyle = "rgba(147, 163, 189, 0.9)";
      ctx.fillText(f.axes.uEnds[0], 2, box.h - 3);
      var right = f.axes.uEnds[1];
      ctx.fillText(right, box.w - 3 - ctx.measureText(right).width, box.h - 3);
      ctx.fillText(f.axes.vEnds[1], 2, 10);
    }

    /**
     * The shape that is <b>not</b> being edited, in outline. Drawing the pair
     * together is the whole reason the minimal shape shares this canvas: what
     * matters about it is how much smaller it is than the other one, and that
     * is invisible in a second editor somewhere else on the page.
     */
    function drawGhost(ctx, f) {
      var p = twin();
      if (!p || p.length < 4) return;
      var closed = !!mask.fill || !!mask.closed;
      var line = polyline(p, !!mask.curve, closed, f);
      if (!line.length) return;
      ctx.save();
      ctx.beginPath();
      ctx.moveTo(line[0][0], line[0][1]);
      for (var i = 1; i < line.length; i++) ctx.lineTo(line[i][0], line[i][1]);
      if (closed) ctx.closePath();
      ctx.setLineDash([4, 3]);
      ctx.strokeStyle = "rgba(140, 195, 255, 0.35)";
      ctx.lineWidth = 1;
      ctx.stroke();
      for (var j = 0; j < p.length / 2; j++) {
        ctx.beginPath();
        ctx.arc(f.px(f.unitU(p[j * 2])), f.py(f.unitV(p[j * 2 + 1])), 2, 0, Math.PI * 2);
        ctx.fillStyle = "rgba(140, 195, 255, 0.4)";
        ctx.fill();
      }
      ctx.restore();
    }

    function drawShape(ctx, f) {
      var p = pts();
      if (p.length < 4) return;
      var fill = !!mask.fill;
      var closed = fill || !!mask.closed;
      var line = polyline(p, !!mask.curve, closed, f);
      if (!line.length) return;

      ctx.beginPath();
      ctx.moveTo(line[0][0], line[0][1]);
      for (var i = 1; i < line.length; i++) ctx.lineTo(line[i][0], line[i][1]);
      if (closed) ctx.closePath();

      if (fill) {
        ctx.fillStyle = "rgba(79, 157, 255, 0.34)";
        ctx.fill();
        ctx.strokeStyle = "rgba(79, 157, 255, 0.9)";
        ctx.lineWidth = 1.5;
        ctx.stroke();
        return;
      }
      // Stroke width is body units, so it is a real measurement on this
      // canvas rather than a decoration.
      var wide = Math.max(1, opts.widthOf(mask.width, 1.0) * f.scale);
      ctx.strokeStyle = "rgba(79, 157, 255, 0.32)";
      ctx.lineWidth = wide;
      ctx.lineJoin = "round";
      ctx.lineCap = "round";
      ctx.stroke();
      ctx.strokeStyle = "rgba(140, 195, 255, 0.95)";
      ctx.lineWidth = 1.25;
      ctx.stroke();
    }

    function drawHandles(ctx, f) {
      var p = pts();
      if (p.length >= 4) {
        inserters(p, !!mask.curve, !!mask.fill || !!mask.closed, f).forEach(function (m) {
          ctx.beginPath();
          ctx.arc(m.x, m.y, INSERT_R, 0, Math.PI * 2);
          ctx.strokeStyle = "rgba(232, 238, 248, 0.55)";
          ctx.lineWidth = 1;
          ctx.stroke();
        });
      }
      for (var i = 0; i < p.length / 2; i++) {
        var x = f.px(f.unitU(p[i * 2])), y = f.py(f.unitV(p[i * 2 + 1]));
        ctx.beginPath();
        ctx.arc(x, y, HANDLE_R, 0, Math.PI * 2);
        ctx.fillStyle = i === selected ? "#e8eef8" : (editingMin ? "#c98bff" : "#4f9dff");
        ctx.fill();
        ctx.strokeStyle = "#0b1120";
        ctx.lineWidth = 1.5;
        ctx.stroke();
        if (i === 0) {
          // The start, so "reverse" and an open path's ends mean something.
          ctx.beginPath();
          ctx.arc(x, y, HANDLE_R + 3, 0, Math.PI * 2);
          ctx.strokeStyle = "rgba(53, 196, 138, 0.9)";
          ctx.lineWidth = 1.5;
          ctx.stroke();
        }
      }
    }

    function drawReadout(ctx, f, box) {
      if (!hoverAt) return;
      var text = f.tidy(f.valueU(f.atX(hoverAt[0]))) + ", "
        + f.tidy(f.valueV(f.atY(hoverAt[1])));
      ctx.font = "10px ui-monospace, Consolas, monospace";
      var w = ctx.measureText(text).width;
      ctx.fillStyle = "rgba(11, 17, 32, 0.85)";
      ctx.fillRect(box.w - w - 8, 2, w + 6, 14);
      ctx.fillStyle = "#9fd4ff";
      ctx.fillText(text, box.w - w - 5, 12);
    }

    function renderNote() {
      var n = count();
      var what = n === 0 ? "no points yet - click the horse to start drawing"
        : n < 2 ? "1 point - a PATH wants at least two"
          : n + " points";
      var which = editingMin ? "the MINIMAL shape (dashed: the full one) — " : "";
      note.textContent = which + what
        + " — click to add, drag to move, click a hollow dot to insert, "
        + "right-click or select and press Delete to remove";
    }

    /**
     * The two tabs, and whether a minimal shape can be started at all. A gene
     * with no dial knob has nothing for the shape to shrink along and the
     * loader says so, so the button explains itself rather than writing a file
     * that fails at startup.
     */
    function syncTabs() {
      if (!minName) return;
      var allowed = !opts.hasDial || opts.hasDial();
      var drawn = (mask[name] || []).length >= 4;
      fullTab.classList.toggle("on", !editingMin);
      minTab.classList.toggle("on", editingMin);
      minTab.disabled = !allowed || !drawn;
      minTab.title = !drawn
        ? "Draw the full shape first - the minimal one starts as a shrunken copy of it"
        : allowed
          ? "Edit the shape it erases down to at its weakest"
          : "Mark one of the gene's knobs as its \"dial\" first - a minimal shape "
            + "needs something to shrink along";
      dropMin.style.display = hasMin() ? "" : "none";
    }

    // ---- pointer ---------------------------------------------------------

    function at(e) {
      var r = canvas.getBoundingClientRect();
      return [e.clientX - r.left, e.clientY - r.top];
    }

    function currentFrame() {
      return frame(mask, opts.skinOf(), canvas.clientWidth,
        canvas.clientHeight || parseFloat(canvas.style.height) || 200);
    }

    function handleAt(xy, f) {
      var p = pts();
      // Backwards, so the point drawn on top is the one grabbed.
      for (var i = p.length / 2 - 1; i >= 0; i--) {
        var dx = xy[0] - f.px(f.unitU(p[i * 2]));
        var dy = xy[1] - f.py(f.unitV(p[i * 2 + 1]));
        if (dx * dx + dy * dy <= GRAB_R * GRAB_R) return i;
      }
      return -1;
    }

    function inserterAt(xy, f) {
      var p = pts();
      if (p.length < 4) return null;
      var found = null;
      inserters(p, !!mask.curve, !!mask.fill || !!mask.closed, f).forEach(function (m) {
        var dx = xy[0] - m.x, dy = xy[1] - m.y;
        if (dx * dx + dy * dy <= GRAB_R * GRAB_R) found = m;
      });
      return found;
    }

    function setPoint(i, xy, f) {
      var u = f.snapU(f.atX(xy[0]));
      var v = f.snapV(f.atY(xy[1]));
      pts()[i * 2] = f.tidy(f.valueU(u));
      pts()[i * 2 + 1] = f.tidy(f.valueV(v));
    }

    var dragging = -1;

    canvas.addEventListener("pointerdown", function (e) {
      if (e.button !== 0) return;
      canvas.focus();
      var f = currentFrame();
      var xy = at(e);
      var hit = handleAt(xy, f);
      if (hit < 0) {
        var ins = inserterAt(xy, f);
        if (ins) {
          hit = ins.after + 1;
        } else {
          hit = count();
        }
        insertPoint(hit, 0, 0);
        setPoint(hit, xy, f);
        edited();
      }
      selected = hit;
      dragging = hit;
      canvas.setPointerCapture(e.pointerId);
      draw();
      e.preventDefault();
    });

    canvas.addEventListener("pointermove", function (e) {
      var xy = at(e);
      hoverAt = xy;
      if (dragging < 0) {
        draw();
        return;
      }
      setPoint(dragging, xy, currentFrame());
      edited();
    });

    function endDrag(e) {
      if (dragging < 0) return;
      dragging = -1;
      if (e && e.pointerId !== undefined && canvas.hasPointerCapture(e.pointerId)) {
        canvas.releasePointerCapture(e.pointerId);
      }
      // One last edit so the textarea and the export agree with the handle.
      edited();
    }
    canvas.addEventListener("pointerup", endDrag);
    canvas.addEventListener("pointercancel", endDrag);
    canvas.addEventListener("pointerleave", function () {
      hoverAt = null;
      if (dragging < 0) draw();
    });

    canvas.addEventListener("contextmenu", function (e) {
      e.preventDefault();
      var f = currentFrame();
      var hit = handleAt(at(e), f);
      if (hit < 0) return;
      removePoint(hit);
      selected = -1;
      edited();
    });

    canvas.addEventListener("keydown", function (e) {
      if (selected < 0) return;
      var f = currentFrame();
      if (e.key === "Delete" || e.key === "Backspace") {
        removePoint(selected);
        selected = -1;
        edited();
        e.preventDefault();
        return;
      }
      var step = (e.shiftKey ? 4 : 1) * SNAP;
      var du = 0, dv = 0;
      if (e.key === "ArrowLeft") du = -step;
      else if (e.key === "ArrowRight") du = step;
      else if (e.key === "ArrowUp") dv = step;
      else if (e.key === "ArrowDown") dv = -step;
      else return;
      var p = pts();
      var uSpan = f.norm ? 1 : geo.bodyBounds(opts.skinOf()).span(f.axes.u);
      var vSpan = f.norm ? 1 : geo.bodyBounds(opts.skinOf()).span(f.axes.v);
      p[selected * 2] = f.tidy(p[selected * 2] + du * uSpan);
      p[selected * 2 + 1] = f.tidy(p[selected * 2 + 1] + dv * vSpan);
      edited();
      e.preventDefault();
    });

    var api = {
      root: root,
      /**
       * Which array the handles are currently editing - so the textarea folded
       * under the canvas shows the shape you are dragging rather than always
       * the full one.
       */
      activeName: activeName,
      refresh: function () { selected = Math.min(selected, count() - 1); draw(); },
      dispose: function () {
        var i = live.indexOf(api);
        if (i >= 0) live.splice(i, 1);
      }
    };
    live.push(api);
    // The first draw has to wait for the layout pass, or clientWidth is 0 and
    // the canvas fits the horse into nothing.
    if (typeof requestAnimationFrame === "function") requestAnimationFrame(draw);
    else draw();
    return api;
  }

  HG.pathCanvas = {
    create: create,
    /** Every open canvas redraws - the skin picker changes the silhouette. */
    redrawAll: function () {
      // A canvas whose card was replaced by a re-render is dropped here rather
      // than kept alive redrawing a detached node.
      for (var i = live.length - 1; i >= 0; i--) {
        if (!live[i].root.isConnected) live.splice(i, 1);
        else live[i].refresh();
      }
    }
  };
})(window.HG);
