// A mirror of common/coat/pattern/SvgPath.java - the SVG mask's geometry.
//
// Its own file rather than another thousand lines inside spec-engine.js,
// because none of it is about horses: it is a path-data parser, an affine
// transform list, a viewBox fit, and two measurements against a flattened
// polyline. Everything here has a named counterpart in the Java, and the two
// have to agree texel for texel - tools/check-parity.mjs is the check.
//
// The flattening runs ONCE, when a gene is loaded or edited, and the result is
// cached on the mask. Doing it per texel would cost more than painting the
// horse; that is the same trade the Java makes at parse time.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // Must equal SvgPath.MAX_POINTS / CURVE_SAMPLES / ARC_SAMPLES_PER_TURN.
  var MAX_POINTS = 1024;
  var CURVE_SAMPLES = 16;
  var ARC_SAMPLES_PER_TURN = 48;

  var ALIGNMENTS = ["xMidYMid", "xMinYMin", "xMidYMin", "xMaxYMin",
    "xMinYMid", "xMaxYMid", "xMinYMax", "xMidYMax", "xMaxYMax"];

  // ---- the number grammar ----------------------------------------------
  // Not JavaScript's: "10-5" is two numbers, "1.5.5" is 1.5 and .5, and an
  // arc's two flags may run together with no separator. All three come out of
  // real drawing programs.
  function Reader(s) { this.s = s; this.i = 0; }
  Reader.prototype.done = function () { return this.i >= this.s.length; };
  Reader.prototype.peek = function () { return this.s.charAt(this.i); };
  Reader.prototype.skip = function () {
    while (this.i < this.s.length && /[\s,]/.test(this.s.charAt(this.i))) this.i++;
  };
  Reader.prototype.flag = function () {
    this.skip();
    var c = this.s.charAt(this.i++);
    if (c !== "0" && c !== "1") {
      throw new Error("an arc's large-arc and sweep flags are 0 or 1, got '" + c
        + "' at index " + (this.i - 1));
    }
    return c === "1";
  };
  Reader.prototype.number = function () {
    this.skip();
    var start = this.i, s = this.s;
    if (this.i < s.length && (s.charAt(this.i) === "+" || s.charAt(this.i) === "-")) this.i++;
    var dot = false;
    while (this.i < s.length) {
      var c = s.charAt(this.i);
      if (c >= "0" && c <= "9") this.i++;
      else if (c === "." && !dot) { dot = true; this.i++; }
      else break;
    }
    if (this.i < s.length && (s.charAt(this.i) === "e" || s.charAt(this.i) === "E")) {
      var save = this.i;
      this.i++;
      if (this.i < s.length && (s.charAt(this.i) === "+" || s.charAt(this.i) === "-")) this.i++;
      if (this.i < s.length && s.charAt(this.i) >= "0" && s.charAt(this.i) <= "9") {
        while (this.i < s.length && s.charAt(this.i) >= "0" && s.charAt(this.i) <= "9") this.i++;
      } else this.i = save;
    }
    if (this.i === start) {
      throw new Error("expected a number at index " + this.i + " of the path data, got "
        + (this.i >= s.length ? "the end of it - a command is missing its arguments"
          : "'" + s.charAt(this.i) + "'"));
    }
    return parseFloat(s.substring(start, this.i));
  };

  function numbers(s) {
    var r = new Reader(String(s == null ? "" : s)), out = [];
    for (;;) { r.skip(); if (r.done()) break; out.push(r.number()); }
    return out;
  }

  // ---- transforms -------------------------------------------------------
  function multiply(a, b) {
    return [
      a[0] * b[0] + a[2] * b[1], a[1] * b[0] + a[3] * b[1],
      a[0] * b[2] + a[2] * b[3], a[1] * b[2] + a[3] * b[3],
      a[0] * b[4] + a[2] * b[5] + a[4], a[1] * b[4] + a[3] * b[5] + a[5]];
  }

  function single(name, a) {
    switch (name) {
      case "matrix": return [a[0], a[1], a[2], a[3], a[4], a[5]];
      case "translate": return [1, 0, 0, 1, a.length > 0 ? a[0] : 0, a.length > 1 ? a[1] : 0];
      case "scale": {
        var sx = a.length > 0 ? a[0] : 1;
        return [sx, 0, 0, a.length > 1 ? a[1] : sx, 0, 0];
      }
      case "rotate": {
        var r = a[0] * Math.PI / 180, cos = Math.cos(r), sin = Math.sin(r);
        var rot = [cos, sin, -sin, cos, 0, 0];
        if (a.length < 3) return rot;
        return multiply(multiply([1, 0, 0, 1, a[1], a[2]], rot), [1, 0, 0, 1, -a[1], -a[2]]);
      }
      case "skewX": return [1, 0, Math.tan(a[0] * Math.PI / 180), 1, 0, 0];
      case "skewY": return [1, Math.tan(a[0] * Math.PI / 180), 0, 1, 0, 0];
      default:
        throw new Error("unknown transform '" + name + "' - the list is matrix, translate, "
          + "scale, rotate, skewX, skewY");
    }
  }

  /** An SVG transform list as one [a b c d e f], composed left to right. */
  function transformMatrix(list) {
    var m = [1, 0, 0, 1, 0, 0], i = 0, n = list.length;
    while (i < n) {
      while (i < n && /[\s,]/.test(list.charAt(i))) i++;
      if (i >= n) break;
      var e = i;
      while (e < n && /[A-Za-z]/.test(list.charAt(e))) e++;
      var name = list.substring(i, e);
      var open = list.indexOf("(", e), close = open < 0 ? -1 : list.indexOf(")", open);
      if (open < 0 || close < 0) throw new Error("transform '" + name + "' has no bracketed arguments");
      m = multiply(m, single(name, numbers(list.substring(open + 1, close))));
      i = close + 1;
    }
    return m;
  }

  // ---- flattening -------------------------------------------------------
  function line(xs, ys, x, y) {
    var n = xs.length;
    if (n && Math.abs(xs[n - 1] - x) < 1e-12 && Math.abs(ys[n - 1] - y) < 1e-12) return;
    xs.push(x); ys.push(y);
  }

  function cubic(xs, ys, x0, y0, x1, y1, x2, y2, x3, y3) {
    for (var i = 1; i <= CURVE_SAMPLES; i++) {
      var t = i / CURVE_SAMPLES, u = 1 - t;
      var a = u * u * u, b = 3 * u * u * t, c = 3 * u * t * t, e = t * t * t;
      line(xs, ys, a * x0 + b * x1 + c * x2 + e * x3, a * y0 + b * y1 + c * y2 + e * y3);
    }
  }

  function quad(xs, ys, x0, y0, x1, y1, x2, y2) {
    for (var i = 1; i <= CURVE_SAMPLES; i++) {
      var t = i / CURVE_SAMPLES, u = 1 - t;
      var a = u * u, b = 2 * u * t, c = t * t;
      line(xs, ys, a * x0 + b * x1 + c * x2, a * y0 + b * y1 + c * y2);
    }
  }

  function angleBetween(ux, uy, vx, vy) {
    var dot = ux * vx + uy * vy;
    var len = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
    var a = Math.acos(Math.max(-1, Math.min(1, dot / len)));
    return ux * vy - uy * vx < 0 ? -a : a;
  }

  /** The endpoint-parameterised elliptical arc, per the SVG appendix. */
  function arc(xs, ys, x0, y0, rx, ry, rotDeg, large, sweep, x1, y1) {
    rx = Math.abs(rx); ry = Math.abs(ry);
    if (rx < 1e-9 || ry < 1e-9 || (Math.abs(x1 - x0) < 1e-12 && Math.abs(y1 - y0) < 1e-12)) {
      line(xs, ys, x1, y1);
      return;
    }
    var phi = rotDeg * Math.PI / 180, cos = Math.cos(phi), sin = Math.sin(phi);
    var dx2 = (x0 - x1) / 2, dy2 = (y0 - y1) / 2;
    var px = cos * dx2 + sin * dy2, py = -sin * dx2 + cos * dy2;
    var lambda = (px * px) / (rx * rx) + (py * py) / (ry * ry);
    if (lambda > 1) { var k = Math.sqrt(lambda); rx *= k; ry *= k; }
    var num = rx * rx * ry * ry - rx * rx * py * py - ry * ry * px * px;
    var den = rx * rx * py * py + ry * ry * px * px;
    var factor = Math.sqrt(Math.max(0, num / den)) * (large === sweep ? -1 : 1);
    var cxp = factor * rx * py / ry, cyp = -factor * ry * px / rx;
    var cx = cos * cxp - sin * cyp + (x0 + x1) / 2;
    var cy = sin * cxp + cos * cyp + (y0 + y1) / 2;
    var theta = angleBetween(1, 0, (px - cxp) / rx, (py - cyp) / ry);
    var delta = angleBetween((px - cxp) / rx, (py - cyp) / ry, (-px - cxp) / rx, (-py - cyp) / ry);
    if (!sweep && delta > 0) delta -= 2 * Math.PI;
    else if (sweep && delta < 0) delta += 2 * Math.PI;
    var steps = Math.max(2, Math.ceil(Math.abs(delta) / (2 * Math.PI) * ARC_SAMPLES_PER_TURN));
    for (var i = 1; i <= steps; i++) {
      var t = theta + delta * i / steps;
      var ex = rx * Math.cos(t), ey = ry * Math.sin(t);
      line(xs, ys, cx + cos * ex - sin * ey, cy + sin * ex + cos * ey);
    }
  }

  function emit(subX, subY, closed, xs, ys, isClosed) {
    if (xs.length < 2) return;
    var n = xs.length;
    if (isClosed && n > 2 && Math.abs(xs[n - 1] - xs[0]) < 1e-9 && Math.abs(ys[n - 1] - ys[0]) < 1e-9) n--;
    subX.push(xs.slice(0, n));
    subY.push(ys.slice(0, n));
    closed.push(isClosed);
  }

  function flatten(d, subX, subY, closed) {
    var r = new Reader(d), xs = [], ys = [];
    var cx = 0, cy = 0, startX = 0, startY = 0;
    var lcx = 0, lcy = 0, lqx = 0, lqy = 0, hadCubic = false, hadQuad = false, cmd = "";
    for (;;) {
      r.skip();
      if (r.done()) break;
      var c = r.peek();
      if ("MmLlHhVvCcSsQqTtAaZz".indexOf(c) >= 0) {
        if (!cmd && c !== "M" && c !== "m") {
          throw new Error("path data must start with a moveto, got '" + c + "' at index " + r.i);
        }
        cmd = c; r.i++;
      }
      else if (!cmd) throw new Error("path data must start with a moveto, got '" + c + "' at index " + r.i);
      else if (cmd === "M") cmd = "L";
      else if (cmd === "m") cmd = "l";

      var rel = cmd >= "a" && cmd <= "z";
      var up = cmd.toUpperCase();
      var x, y, x1, y1, x2, y2;
      if (up === "M") {
        x = r.number(); y = r.number();
        if (rel) { x += cx; y += cy; }
        emit(subX, subY, closed, xs, ys, false);
        xs = [x]; ys = [y];
        cx = startX = x; cy = startY = y;
        hadCubic = hadQuad = false;
      } else if (up === "L") {
        x = r.number(); y = r.number();
        if (rel) { x += cx; y += cy; }
        line(xs, ys, x, y); cx = x; cy = y; hadCubic = hadQuad = false;
      } else if (up === "H") {
        x = r.number(); if (rel) x += cx;
        line(xs, ys, x, cy); cx = x; hadCubic = hadQuad = false;
      } else if (up === "V") {
        y = r.number(); if (rel) y += cy;
        line(xs, ys, cx, y); cy = y; hadCubic = hadQuad = false;
      } else if (up === "C" || up === "S") {
        if (up === "S") {
          // The reflection of the previous cubic's second control point - the
          // whole of what makes an S curve smooth.
          x1 = hadCubic ? 2 * cx - lcx : cx;
          y1 = hadCubic ? 2 * cy - lcy : cy;
        } else {
          x1 = r.number(); y1 = r.number();
          if (rel) { x1 += cx; y1 += cy; }
        }
        x2 = r.number(); y2 = r.number(); x = r.number(); y = r.number();
        if (rel) { x2 += cx; y2 += cy; x += cx; y += cy; }
        cubic(xs, ys, cx, cy, x1, y1, x2, y2, x, y);
        lcx = x2; lcy = y2; hadCubic = true; hadQuad = false; cx = x; cy = y;
      } else if (up === "Q" || up === "T") {
        if (up === "T") {
          x1 = hadQuad ? 2 * cx - lqx : cx;
          y1 = hadQuad ? 2 * cy - lqy : cy;
        } else {
          x1 = r.number(); y1 = r.number();
          if (rel) { x1 += cx; y1 += cy; }
        }
        x = r.number(); y = r.number();
        if (rel) { x += cx; y += cy; }
        quad(xs, ys, cx, cy, x1, y1, x, y);
        lqx = x1; lqy = y1; hadQuad = true; hadCubic = false; cx = x; cy = y;
      } else if (up === "A") {
        var rx = r.number(), ry = r.number(), rot = r.number();
        var large = r.flag(), sweep = r.flag();
        x = r.number(); y = r.number();
        if (rel) { x += cx; y += cy; }
        arc(xs, ys, cx, cy, rx, ry, rot, large, sweep, x, y);
        cx = x; cy = y; hadCubic = hadQuad = false;
      } else if (up === "Z") {
        emit(subX, subY, closed, xs, ys, true);
        cx = startX; cy = startY;
        // A subpath after a Z with no moveto starts where the Z returned to.
        xs = [cx]; ys = [cy];
        hadCubic = hadQuad = false;
      } else {
        throw new Error("unknown path command '" + cmd + "' at index " + r.i);
      }
    }
    emit(subX, subY, closed, xs, ys, false);
  }

  /** Flatten a `d` string under an optional transform list. Mirrors SvgPath.parse. */
  function parse(d, transform) {
    var subX = [], subY = [], subClosed = [];
    flatten(String(d), subX, subY, subClosed);
    if (!subX.length) throw new Error("path data draws nothing - no subpath has two points in it");

    var m = transform && String(transform).trim()
      ? transformMatrix(String(transform)) : [1, 0, 0, 1, 0, 0];

    var total = 0, s, i;
    for (s = 0; s < subX.length; s++) total += subX[s].length;
    if (total > MAX_POINTS) {
      throw new Error("flattens to " + total + " points, over the " + MAX_POINTS
        + "-point ceiling - every texel of every skin walks all of them");
    }

    var xs = new Float64Array(total), ys = new Float64Array(total), cum = new Float64Array(total);
    var starts = new Int32Array(subX.length + 1), closed = [], subBox = new Float64Array(subX.length * 4);
    var minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity, at = 0;
    for (s = 0; s < subX.length; s++) {
      starts[s] = at;
      closed.push(subClosed[s]);
      var px = subX[s], py = subY[s];
      var sMinX = Infinity, sMinY = Infinity, sMaxX = -Infinity, sMaxY = -Infinity, run = 0;
      for (i = 0; i < px.length; i++) {
        var x = m[0] * px[i] + m[2] * py[i] + m[4];
        var y = m[1] * px[i] + m[3] * py[i] + m[5];
        if (i > 0) {
          var dx = x - xs[at - 1], dy = y - ys[at - 1];
          run += Math.sqrt(dx * dx + dy * dy);
        }
        xs[at] = x; ys[at] = y; cum[at] = run;
        if (x < sMinX) sMinX = x;
        if (y < sMinY) sMinY = y;
        if (x > sMaxX) sMaxX = x;
        if (y > sMaxY) sMaxY = y;
        at++;
      }
      subBox[s * 4] = sMinX; subBox[s * 4 + 1] = sMinY;
      subBox[s * 4 + 2] = sMaxX; subBox[s * 4 + 3] = sMaxY;
      minX = Math.min(minX, sMinX); minY = Math.min(minY, sMinY);
      maxX = Math.max(maxX, sMaxX); maxY = Math.max(maxY, sMaxY);
    }
    starts[subX.length] = at;
    return {
      d: String(d), xs: xs, ys: ys, cum: cum, starts: starts, closed: closed,
      subBox: subBox, box: [minX, minY, maxX, maxY]
    };
  }

  // ---- fitting ----------------------------------------------------------
  /** preserveAspectRatio, as [scaleX, scaleY, offsetX, offsetY]. Mirrors SvgPath.fit. */
  function fit(viewBox, vx, vy, vw, vh, mode, align) {
    var bw = Math.max(1e-9, viewBox[2]), bh = Math.max(1e-9, viewBox[3]);
    var sx = vw / bw, sy = vh / bh;
    if (mode !== "none") {
      var s = mode === "slice" ? Math.max(sx, sy) : Math.min(sx, sy);
      sx = s; sy = s;
    }
    var slackX = vw - bw * sx, slackY = vh - bh * sy;
    var ax = align.indexOf("xMax") >= 0 ? 1 : (align.indexOf("xMin") >= 0 ? 0 : 0.5);
    var ay = align.indexOf("YMax") >= 0 ? 1 : (align.indexOf("YMin") >= 0 ? 0 : 0.5);
    return [sx, sy, vx - viewBox[0] * sx + slackX * ax, vy - viewBox[1] * sy + slackY * ay];
  }

  // ---- measuring --------------------------------------------------------
  /** The fill rule. Mirrors SvgPath.inside. */
  function inside(shape, x, y, evenOdd) {
    var winding = 0;
    for (var s = 0; s < shape.closed.length; s++) {
      var from = shape.starts[s], to = shape.starts[s + 1];
      if (to - from < 3) continue;
      for (var i = from; i < to; i++) {
        var j = i + 1 < to ? i + 1 : from;
        var ax = shape.xs[i], ay = shape.ys[i], bx = shape.xs[j], by = shape.ys[j];
        if ((ay > y) === (by > y)) continue;
        var t = (y - ay) / (by - ay);
        if (x < ax + t * (bx - ax)) winding += by > ay ? 1 : -1;
      }
    }
    return evenOdd ? (winding & 1) !== 0 : winding !== 0;
  }

  /** Distance to the outline, and the arc length of the point that won. Mirrors SvgPath.distance. */
  function distance(shape, x, y, caps, half, out) {
    var best = Infinity, bestArc = 0, bestSub = 0;
    var square = caps === "square", round = caps === "round";
    for (var s = 0; s < shape.closed.length; s++) {
      var bx0 = shape.subBox[s * 4], by0 = shape.subBox[s * 4 + 1];
      var bx1 = shape.subBox[s * 4 + 2], by1 = shape.subBox[s * 4 + 3];
      var qx = x < bx0 ? bx0 - x : (x > bx1 ? x - bx1 : 0);
      var qy = y < by0 ? by0 - y : (y > by1 ? y - by1 : 0);
      if (qx * qx + qy * qy > best * best) continue;

      var from = shape.starts[s], to = shape.starts[s + 1], isClosed = shape.closed[s];
      var spans = isClosed ? to - from : to - from - 1;
      for (var k = 0; k < spans; k++) {
        var i = from + k, j = i + 1 < to ? i + 1 : from;
        var ax = shape.xs[i], ay = shape.ys[i];
        var vx = shape.xs[j] - ax, vy = shape.ys[j] - ay;
        var len2 = vx * vx + vy * vy;
        var raw = len2 <= 1e-18 ? 0 : ((x - ax) * vx + (y - ay) * vy) / len2;
        var t = raw < 0 ? 0 : (raw > 1 ? 1 : raw);
        var dx = x - (ax + t * vx), dy = y - (ay + t * vy);
        var d = Math.sqrt(dx * dx + dy * dy);
        var over = 0;
        if (!isClosed && !round) {
          var len = Math.sqrt(len2);
          if (k === 0 && raw < 0) over = -raw * len;
          else if (k === spans - 1 && raw > 1) over = (raw - 1) * len;
          if (square) over -= half;
        }
        if (over > 0) {
          // Past a flat cap the stroke is a rectangle, not a capsule - see the
          // Java for why this reports a distance rather than a region.
          var lenC = Math.max(1e-9, Math.sqrt(len2));
          var perp = Math.abs((x - ax) * vy - (y - ay) * vx) / lenC;
          d = Math.max(perp, half + over);
        }
        if (d < best) {
          best = d;
          bestArc = shape.cum[i] + t * Math.sqrt(len2);
          bestSub = s;
        }
      }
    }
    out[0] = best; out[1] = bestArc; out[2] = bestSub;
  }

  function sign(ux, uy, wx, wy) { return ux * wy - uy * wx > 0 ? -1 : 1; }

  function pointInWedge(x, y, px, py, ax, ay, tx, ty, bx, by) {
    var qx = [px, ax, tx, bx], qy = [py, ay, ty, by];
    var inIt = true, sgn = false, first = true, best = Infinity;
    for (var i = 0; i < 4; i++) {
      var j = (i + 1) & 3;
      var cross = (qx[j] - qx[i]) * (y - qy[i]) - (qy[j] - qy[i]) * (x - qx[i]);
      if (first) { sgn = cross > 0; first = false; }
      else if ((cross > 0) !== sgn) inIt = false;
      var vx = qx[j] - qx[i], vy = qy[j] - qy[i], len2 = vx * vx + vy * vy;
      var t = len2 <= 1e-18 ? 0 : ((x - qx[i]) * vx + (y - qy[i]) * vy) / len2;
      t = t < 0 ? 0 : (t > 1 ? 1 : t);
      var ex = qx[i] + t * vx - x, ey = qy[i] + t * vy - y;
      best = Math.min(best, Math.sqrt(ex * ex + ey * ey));
    }
    return inIt ? 0 : best;
  }

  /** How far past a round join a miter reaches. Mirrors SvgPath.miterDistance. */
  function miterDistance(shape, x, y, half, limit) {
    var best = Infinity;
    for (var s = 0; s < shape.closed.length; s++) {
      var from = shape.starts[s], to = shape.starts[s + 1], isClosed = shape.closed[s];
      var corners = isClosed ? to - from : to - from - 2;
      for (var k = 0; k < corners; k++) {
        var i = isClosed ? from + k : from + k + 1;
        var prev = i - 1 >= from ? i - 1 : to - 1;
        var next = i + 1 < to ? i + 1 : from;
        var ux = shape.xs[i] - shape.xs[prev], uy = shape.ys[i] - shape.ys[prev];
        var wx = shape.xs[next] - shape.xs[i], wy = shape.ys[next] - shape.ys[i];
        var ul = Math.sqrt(ux * ux + uy * uy), wl = Math.sqrt(wx * wx + wy * wy);
        if (ul < 1e-12 || wl < 1e-12) continue;
        ux /= ul; uy /= ul; wx /= wl; wy /= wl;
        var cos = -(ux * wx + uy * wy);
        var sinHalf = Math.sqrt(Math.max(1e-12, (1 - cos) / 2));
        var ratio = 1 / sinHalf;
        if (ratio > limit) continue;
        var bx = ux - wx, by = uy - wy, bl = Math.sqrt(bx * bx + by * by);
        if (bl < 1e-12) continue;
        var sg = sign(ux, uy, wx, wy);
        var d = pointInWedge(x, y, shape.xs[i], shape.ys[i],
          shape.xs[i] - uy * half * sg, shape.ys[i] + ux * half * sg,
          shape.xs[i] - bx / bl * half * ratio, shape.ys[i] - by / bl * half * ratio,
          shape.xs[i] - wy * half * sg, shape.ys[i] + wx * half * sg);
        if (d < best) best = d;
      }
    }
    return best;
  }

  HG.svgPath = {
    MAX_POINTS: MAX_POINTS,
    CURVE_SAMPLES: CURVE_SAMPLES,
    ARC_SAMPLES_PER_TURN: ARC_SAMPLES_PER_TURN,
    ALIGNMENTS: ALIGNMENTS,
    numbers: numbers,
    transformMatrix: transformMatrix,
    parse: parse,
    fit: fit,
    inside: inside,
    distance: distance,
    miterDistance: miterDistance
  };
})(window.HG);
