// The JavaScript twin of common/coat/pattern/SpecPainter.java, plus the knob
// draw from SpecValues.java.
//
// This is the file that makes the preview honest. Everything else in the
// creator is chrome; this decides what a mask covers and what an op does, and
// it has to give the same answers as the Java or the tool is drawing a horse
// the game will not breed.
//
// The spec here is the same object the creator exports - no intermediate form,
// so what you preview is literally what the file says.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var geo = HG.geometry;
  var noise = HG.noise;
  var svg = HG.svgPath;
  var smoothstep = noise.smoothstep;
  var LEG_COUNT = 4;

  function clamp01(v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
  function lerp(a, b, t) { return a + (b - a) * t; }

  // ---- a deterministic RNG matching java.util.Random -------------------
  //
  // Knobs are drawn off a horse's epigenetic seed with java.util.Random in the
  // game. The creator has no horse, so it draws from an arbitrary preview seed -
  // but it uses the same generator, so "seed 7" here shows a horse the game can
  // actually produce rather than one from a different distribution.

  var MULT = noise.fromHex("5DEECE66D");
  var ADDEND = noise.u64(0, 0xB);
  var MASK48_H = 0xFFFF; // the seed is 48 bits: all of `l`, the low 16 of `h`.

  // The seed scramble SeededRng applies before java.util.Random ever sees the
  // seed - splitmix64's finaliser, mirrored constant for constant.
  //
  // java.util.Random's own seed handling is a single XOR, which is not a mix:
  // neighbouring seeds stay neighbours and the draw at a given position stays
  // correlated across the whole set. The game scrambles first (see
  // SeededRng.scramble and known-gaps gaps 44 and 117), so the creator has to
  // as well - otherwise "seed 7" in the preview is a horse from a different
  // distribution than "seed 7" in the game, which is exactly the class of
  // silent divergence this mirror exists to prevent.
  var SM_GAMMA = noise.fromHex("9E3779B97F4A7C15");
  var SM_MIX1 = noise.fromHex("BF58476D1CE4E5B9");
  var SM_MIX2 = noise.fromHex("94D049BB133111EB");

  function scramble(seed) {
    var z = noise.add(seed, SM_GAMMA);
    z = noise.mul(noise.xor(z, noise.shru(z, 30)), SM_MIX1);
    z = noise.mul(noise.xor(z, noise.shru(z, 27)), SM_MIX2);
    return noise.xor(z, noise.shru(z, 31));
  }

  function JavaRandom(seedHigh, seedLow) {
    var s = noise.xor(scramble(noise.u64(seedHigh, seedLow)), MULT);
    this.s = noise.u64(s.h & MASK48_H, s.l);
  }

  JavaRandom.prototype.next = function (bits) {
    var s = noise.mul(this.s, MULT);
    s = noise.u64((s.h + ((ADDEND.l + s.l > 0xFFFFFFFF) ? 1 : 0)) & MASK48_H, (s.l + ADDEND.l) >>> 0);
    this.s = s;
    // (int)(seed >>> (48 - bits)) - the seed's 48 bits live in h[0..15]:l.
    var shift = 48 - bits;
    var value;
    if (shift >= 32) {
      value = (s.h >>> (shift - 32));
    } else {
      value = ((s.h << (32 - shift)) | (s.l >>> shift)) >>> 0;
    }
    return value | 0;
  };

  JavaRandom.prototype.nextFloat = function () {
    return (this.next(24) >>> 0) / (1 << 24);
  };

  // Java is ((long) next(32) << 32) + next(32) - a signed ADD, not a
  // concatenation, so a negative low word borrows from the high one.
  JavaRandom.prototype.nextLong = function () {
    var hi = this.next(32);
    var lo = this.next(32);
    return noise.add(noise.u64(hi >>> 0, 0), noise.fromInt(lo));
  };

  /**
   * Which of `options` outcomes this horse drew - the port of
   * SpecPainter.choiceOf. Mixed rather than taken modulo directly: a seed
   * knob's low bits are not a fair coin, and `% 2` on a raw seed is the kind
   * of thing that comes out 60/40 and is never noticed.
   */
  /**
   * The FRACTAL mask's field - mirrors SpecPainter.fractal, including its
   * unusual divisor.
   *
   * Textbook fbm divides the octave sum by the TOTAL amplitude, which makes it
   * a weighted mean of independent samples - so the field bunches harder round
   * 0.5 with every octave added, and a threshold tuned at three octaves covers
   * a different amount of horse at five. Dividing by the ROOT of the summed
   * squares keeps the spread where one octave put it, so octaves buy detail and
   * nothing else. See the Java, and common's FractalMaskTest.
   *
   * Coordinates arrive already divided by the mask's scale, warp included.
   */
  function fractal(seed, x, y, z, octaves, lacunarity, gain, warp) {
    var sx = x, sy = y, sz = z;
    if (warp !== 0) {
      // All three offsets come off the UNWARPED point, or the second axis would
      // be displaced by an already-displaced first one.
      var wx = noise.value(noise.xor(seed, noise.u64(0, 0xA11CE5)), x * 0.5, y * 0.5, z * 0.5) - 0.5;
      var wy = noise.value(noise.xor(seed, noise.u64(0, 0xB22DF6)), x * 0.5, y * 0.5, z * 0.5) - 0.5;
      var wz = noise.value(noise.xor(seed, noise.u64(0, 0xC33E07)), x * 0.5, y * 0.5, z * 0.5) - 0.5;
      sx += wx * 2 * warp;
      sy += wy * 2 * warp;
      sz += wz * 2 * warp;
    }
    var sum = 0, sumSq = 0, amp = 1, freq = 1;
    for (var o = 0; o < octaves; o++) {
      sum += amp * (noise.value(noise.add(seed, noise.fromInt(131 * o)),
        sx * freq, sy * freq, sz * freq) - 0.5);
      sumSq += amp * amp;
      amp *= gain;
      freq *= lacunarity;
    }
    return clamp01(0.5 + sum / Math.sqrt(sumSq));
  }

  /**
   * The PATH mask's coverage - mirrors SpecPainter.pathCoverage.
   *
   * One walk of the control points answers both questions: distance to the
   * nearest sub-segment strokes the line, crossing parity of a ray along +u
   * fills it. Sub-segments are generated on the fly rather than collected into
   * an array, because this runs per texel.
   */
  function pathCoverage(pts, curve, closed, fill, uMin, uSpan, vMin, vSpan, u, v, half, soft) {
    var n = pts.length / 2;
    var spans = closed ? n : n - 1;
    var steps = curve ? HG.schema.PATH_CURVE_SAMPLES : 1;
    var best = Infinity;
    var inside = false;
    var au = uMin + pts[0] * uSpan;
    var av = vMin + pts[1] * vSpan;
    for (var span = 0; span < spans; span++) {
      for (var step = 1; step <= steps; step++) {
        var bu, bv;
        if (curve) {
          var t = step / steps;
          bu = uMin + spline(pts, n, closed, span, t, 0) * uSpan;
          bv = vMin + spline(pts, n, closed, span, t, 1) * vSpan;
        } else {
          var j = (span + 1) % n;
          bu = uMin + pts[j * 2] * uSpan;
          bv = vMin + pts[j * 2 + 1] * vSpan;
        }
        var d = segmentDistance(u, v, au, av, bu, bv);
        if (d < best) best = d;
        if (fill && crosses(u, v, au, av, bu, bv)) inside = !inside;
        au = bu;
        av = bv;
      }
    }
    if (fill) return inside ? 1 : 1 - smoothstep(0, soft, best);
    return 1 - smoothstep(half, half + soft, best);
  }

  /** Catmull-Rom, ends held by duplicating the first and last point. */
  function spline(pts, n, closed, span, t, axis) {
    var i1 = span;
    var i2 = closed ? (span + 1) % n : Math.min(span + 1, n - 1);
    var i0 = closed ? (span - 1 + n) % n : Math.max(span - 1, 0);
    var i3 = closed ? (span + 2) % n : Math.min(span + 2, n - 1);
    var p0 = pts[i0 * 2 + axis], p1 = pts[i1 * 2 + axis];
    var p2 = pts[i2 * 2 + axis], p3 = pts[i3 * 2 + axis];
    var t2 = t * t, t3 = t2 * t;
    return 0.5 * ((2 * p1) + (-p0 + p2) * t
      + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
      + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
  }

  function segmentDistance(u, v, au, av, bu, bv) {
    var du = bu - au, dv = bv - av;
    var len2 = du * du + dv * dv;
    var t = len2 <= 1e-12 ? 0 : ((u - au) * du + (v - av) * dv) / len2;
    t = t < 0 ? 0 : (t > 1 ? 1 : t);
    var cu = au + t * du, cv = av + t * dv;
    return Math.sqrt((u - cu) * (u - cu) + (v - cv) * (v - cv));
  }

  /**
   * Even-odd crossing along +u. The half-open comparison on v is what stops a
   * vertex sitting exactly on the ray being counted twice - which shows up as
   * one inverted row through a filled shape.
   */
  function crosses(u, v, au, av, bu, bv) {
    if ((av > v) === (bv > v)) return false;
    var at = (v - av) / (bv - av);
    return u < au + at * (bu - au);
  }

  function choiceOf(seed, options) {
    var z = scramble(seed);
    // The low 32 bits are enough for a modulus this small, and staying inside
    // one word avoids a 64-bit division the u64 helpers do not have.
    var lo = z.l >>> 0;
    return lo % options;
  }

  /**
   * Round to what an epigenome code can write - the port of
   * EpiCodec.quantise. Every stored value passes through this in Java, so the
   * creator has to as well or parity fails in the sixth decimal.
   */
  function quantise(v) {
    if (!isFinite(v)) return 0;
    if (Math.abs(v) >= 1e12) return Math.trunc(v);
    return Math.round(v * 1000000) / 1000000;
  }

  /**
   * Roll a founder's knob values - the port of EpiRoll.founder over the schema
   * SpecValues.schema builds.
   *
   * <p>A per-leg knob draws FOUR floats, one per leg, across a range widened by
   * its spread. It used to draw five - one base, then a jitter per leg around
   * it - which meant the four legs were correlated and no single leg was a
   * number anyone could edit. Now each leg is an independent stored value, and
   * the spread is folded into the range instead of applied afterwards.
   */
  function drawValues(spec, seedHigh, seedLow, dose) {
    var rng = new JavaRandom(seedHigh, seedLow);
    var ranges = [], seeds = [];
    (spec.knobs || []).forEach(function (knob, i) {
      if (knob.type === "seed") {
        seeds[i] = rng.nextLong();
        ranges[i] = null;
        return;
      }
      if (knob.per !== "leg") {
        ranges[i] = [quantise(knob.min + rng.nextFloat() * (knob.max - knob.min))];
        return;
      }
      var spread = knob.spread || 0;
      var lo = knob.min * (1 - spread);
      var hi = knob.max * (1 + spread);
      var perLeg = [];
      for (var leg = 0; leg < LEG_COUNT; leg++) {
        perLeg.push(quantise(lo + rng.nextFloat() * (hi - lo)));
      }
      ranges[i] = perLeg;
    });
    return { spec: spec, ranges: ranges, seeds: seeds, dose: dose };
  }

  function knobIndex(spec, name) {
    for (var i = 0; i < (spec.knobs || []).length; i++) {
      if (spec.knobs[i].name === name) return i;
    }
    return -1;
  }

  /** Resolve a raw JSON parameter (number, "$knob", {perDose}) to a number. */
  function get(values, raw, fallback, legIndex) {
    if (raw === undefined || raw === null) return fallback;
    if (typeof raw === "number") return raw;
    if (typeof raw === "string" && raw.charAt(0) === "$") {
      var i = knobIndex(values.spec, raw.slice(1));
      if (i < 0 || !values.ranges[i]) return fallback;
      var drawn = values.ranges[i];
      var leg = legIndex < 0 ? 0 : Math.min(legIndex, drawn.length - 1);
      return drawn[leg];
    }
    if (raw && raw.perDose) {
      return raw.perDose[Math.min(2, Math.max(0, values.dose))];
    }
    return fallback;
  }

  function getSeed(values, raw, fallback) {
    if (typeof raw === "string" && raw.charAt(0) === "$") {
      var i = knobIndex(values.spec, raw.slice(1));
      if (i >= 0 && values.seeds[i]) return values.seeds[i];
    }
    return fallback;
  }

  /** Matches SpecPainter.layerSeed: Java String.hashCode, then the mixers. */
  function javaHashCode(s) {
    var h = 0;
    for (var i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
    return h;
  }

  function layerSeed(spec, layerIndex) {
    return noise.xor(
      noise.mul(noise.fromInt(javaHashCode(spec.key || "")), noise.K1),
      noise.mul(noise.fromInt(layerIndex + 1), noise.fromHex("C2B2AE3D27D4EB4F")));
  }

  /** The polynomial smooth minimum - the port of SpecPainter.smoothMin. */
  function smoothMin(a, b, k) {
    var h = Math.min(1, Math.max(0, 0.5 + 0.5 * (b - a) / k));
    return b + (a - b) * h - k * h * (1 - h);
  }

  function band(t, from, to, softness) {
    var soft = Math.max(1e-6, softness);
    return smoothstep(from - soft, from, t) * (1 - smoothstep(to, to + soft, t));
  }

  /**
   * Distance to the surface where the noise crosses its midpoint, in lattice
   * units - the port of SpecPainter.levelSetDistance, and the reason a STROKES
   * stroke keeps its width. |n - 0.5| alone does not: value noise is flat at
   * its extrema, so the band would balloon wherever the field went quiet.
   */
  function levelSetDistance(seed, x, y, z) {
    var n = noise.value(seed, x, y, z);
    var e = 0.25;
    var gx = (noise.value(seed, x + e, y, z) - n) / e;
    var gy = (noise.value(seed, x, y + e, z) - n) / e;
    var gz = (noise.value(seed, x, y, z + e) - n) / e;
    var grad = Math.sqrt(gx * gx + gy * gy + gz * gz);
    return Math.abs(n - 0.5) / Math.max(grad, 1e-4);
  }

  function normalise(coord, bounds, axis) {
    if (!bounds) return 0;
    var span = bounds.span(axis);
    return span === 0 ? 0 : (coord - bounds.min(axis)) / span;
  }

  // ---- masks -----------------------------------------------------------

  function maskCoverage(mask, values, skin, part, face, point, coat, colour, px, py, legIndex, seedBase) {
    var parts = HG.schema.expandParts(mask.parts);
    if (parts.length && mask.type !== "PARTS" && parts.indexOf(part) < 0) return 0;

    switch (mask.type) {
      case "ALL":
        return 1;
      case "PARTS":
        return parts.indexOf(part) >= 0 ? 1 : 0;
      case "AXIS": {
        var axis = (mask.axis || "Y").toUpperCase();
        var coord = axis === "X" ? point.x : axis === "Y" ? point.y : point.z;
        var space = mask.space || "part";
        var t = space === "body" ? normalise(coord, geo.bodyBounds(skin), axis)
          : space === "units" ? coord
            : space === "local" ? axisOf(geo.local(skin, part, point), axis)
              : normalise(coord, geo.bounds(skin, part), axis);
        return band(t, get(values, mask.from, 0, legIndex), get(values, mask.to, 1, legIndex),
          get(values, mask.softness, 0.15, legIndex));
      }
      case "CENTERLINE": {
        var d = Math.abs(point.z - get(values, mask.offset, 0, legIndex));
        var half = get(values, mask.halfWidth, 1, legIndex);
        var soft = Math.max(1e-6, get(values, mask.softness, 0.35, legIndex));
        return 1 - smoothstep(half, half + soft, d);
      }
      case "STRIPES": {
        return noise.stripeCoverage(getSeed(values, mask.seed, seedBase), point.x, point.y, point.z,
          Math.max(0.01, get(values, mask.spacing, 3.0, legIndex)),
          clamp01(get(values, mask.duty, 0.45, legIndex)),
          get(values, mask.warp, 1.0, legIndex));
      }
      case "DAPPLES": {
        var seed = getSeed(values, mask.seed, seedBase);
        var spacing = Math.max(0.05, get(values, mask.spacing, 3.5, legIndex));
        var warp = get(values, mask.warp, 0.45, legIndex) * spacing;
        var ws = 1 / (spacing * 3);
        var n = noise.value(noise.xor(seed, noise.u64(0, 0x51)), point.x * ws, point.y * ws, point.z * ws);
        var m = noise.value(noise.xor(seed, noise.u64(0, 0x52)), point.z * ws, point.x * ws, point.y * ws);
        var dd = noise.cellDistance(seed,
          (point.x + (n - 0.5) * warp) / spacing,
          (point.y + (m - 0.5) * warp) / spacing,
          (point.z + (n - m) * warp) / spacing);
        return 1 - smoothstep(get(values, mask.edge0, 0.35, legIndex),
          get(values, mask.edge1, 0.78, legIndex), dd);
      }
      case "PATCHES": {
        var s2 = getSeed(values, mask.seed, seedBase);
        var scale = Math.max(0.05, get(values, mask.scale, 6.0, legIndex));
        var nv = noise.value(s2, point.x / scale, point.y / scale, point.z / scale);
        var threshold = get(values, mask.threshold, 0.5, legIndex);
        var sf = Math.max(1e-6, get(values, mask.softness, 0.12, legIndex));
        return smoothstep(threshold - sf, threshold + sf, nv);
      }
      case "NOISE": {
        var s3 = getSeed(values, mask.seed, seedBase);
        var sc = Math.max(0.05, get(values, mask.scale, 8.0, legIndex));
        var v3 = noise.value(s3, point.x / sc, point.y / sc, point.z / sc);
        var low = get(values, mask.low, 0, legIndex);
        return clamp01(low + (get(values, mask.high, 1, legIndex) - low) * v3);
      }
      case "FRACTAL": {
        var fs = getSeed(values, mask.seed, seedBase);
        var fsc = Math.max(0.05, get(values, mask.scale, 6.0, legIndex));
        var oct = Math.round(get(values, mask.octaves, 3.0, legIndex));
        // 6 is SpecSchema.MAX_OCTAVES - a cost ceiling, not a taste one.
        oct = Math.max(1, Math.min(6, oct));
        var lac = Math.max(1.01, get(values, mask.lacunarity, 2.13, legIndex));
        var gain = clamp01(get(values, mask.gain, 0.5, legIndex));
        var fn = fractal(fs, point.x / fsc, point.y / fsc, point.z / fsc, oct, lac, gain,
          get(values, mask.warp, 0.0, legIndex) / fsc);
        var fshape = mask.shape || "fbm";
        if (fshape === "ridged") fn = 1 - Math.abs(2 * fn - 1);
        else if (fshape === "billow") fn = Math.abs(2 * fn - 1);
        var ft = get(values, mask.threshold, 0.5, legIndex);
        var fsf = Math.max(1e-6, get(values, mask.softness, 0.12, legIndex));
        return smoothstep(ft - fsf, ft + fsf, fn);
      }
      case "PATH": {
        var ppts = mask.points || [];
        if (ppts.length < 4) return 0;
        var whole = geo.bodyBounds(skin);
        var pl = mask.plane || "side";
        var uAxis = pl === "front" ? "Z" : "X";
        var vAxis = pl === "top" ? "Z" : "Y";
        // 'side' is (x,y) extruded along z, so |z| is never consulted and the
        // shape shows on both flanks. Mirrors the Java.
        var pu = uAxis === "X" ? point.x : point.z;
        var pv = vAxis === "Z" ? point.z : point.y;
        var norm = (mask.space || "body") !== "units";
        var uMin = norm ? whole.min(uAxis) : 0;
        var uSpan = norm ? Math.max(1e-6, whole.span(uAxis)) : 1;
        var vMin = norm ? whole.min(vAxis) : 0;
        var vSpan = norm ? Math.max(1e-6, whole.span(vAxis)) : 1;
        var pfill = !!mask.fill;
        var pclosed = pfill || !!mask.closed;
        var psoft = Math.max(1e-6, get(values, mask.softness, 0.25, legIndex));
        var phalf = Math.max(0, get(values, mask.width, 1.0, legIndex)) / 2;
        return pathCoverage(ppts, !!mask.curve, pclosed, pfill,
          uMin, uSpan, vMin, vSpan, pu, pv, phalf, psoft);
      }
      case "CHOICE": {
        // Constant across the horse and exactly 0 or 1 - the position is
        // deliberately not read. Mirrors SpecPainter's CHOICE case.
        var cs = getSeed(values, mask.seed, seedBase);
        var opts = Math.max(1, Math.round(get(values, mask.options, 2.0, legIndex)));
        var want = Math.round(get(values, mask.is, 0.0, legIndex));
        var got = choiceOf(cs, opts);
        return got === (((want % opts) + opts) % opts) ? 1 : 0;
      }
      case "PIGMENT":
        return pigmentCoverage(mask, values, coat, px, py, legIndex);
      case "LUMA":
        return lumaCoverage(mask, values, colour, px, py, legIndex);
      case "EDGE": {
        // The rim of this part's box, in the two axes that span the face - the
        // third is the face's normal and is constant across it. See the Java.
        var eb = geo.bounds(skin, part);
        var ew = Math.max(1e-6, get(values, mask.width, 0.6, legIndex));
        var es = Math.max(1e-6, get(values, mask.softness, 0.25, legIndex));
        var ed = Math.min(edgeDistance(point, eb, geo.spanA(face)),
          edgeDistance(point, eb, geo.spanB(face)));
        return 1 - smoothstep(ew, ew + es, ed);
      }
      case "SPOTS": {
        var sp = getSeed(values, mask.seed, seedBase);
        var spacing4 = Math.max(0.05, get(values, mask.spacing, 4.0, legIndex));
        var stretch = Math.max(0.05, get(values, mask.stretch, 1.0, legIndex));
        var la = (mask.axis || "X").toUpperCase();
        // |z| when mirrored, so the far flank draws the near one's spots.
        var sz4 = mask.mirror ? Math.abs(point.z) : point.z;
        var c4 = noise.cell(sp,
          point.x / (spacing4 * (la === "X" ? stretch : 1)),
          point.y / (spacing4 * (la === "Y" ? stretch : 1)),
          sz4 / (spacing4 * (la === "Z" ? stretch : 1)));
        if (c4.pick >= get(values, mask.chance, 1.0, legIndex)) return 0;
        var vary4 = clamp01(get(values, mask.vary, 0.5, legIndex));
        var rad4 = get(values, mask.radius, 0.9, legIndex) * (1 - vary4 + 2 * vary4 * c4.size);
        // The offsets move where THIS mask measures from, not where the cell is,
        // so a second mask on the same seed still finds the same centres.
        var ox4 = get(values, mask.offsetX, 0.0, legIndex) / spacing4;
        var oy4 = get(values, mask.offsetY, 0.0, legIndex) / spacing4;
        var oz4 = get(values, mask.offsetZ, 0.0, legIndex) / spacing4;
        var dx4 = c4.dx - ox4, dy4 = c4.dy - oy4, dz4 = c4.dz - oz4;
        var dist4 = (ox4 === 0 && oy4 === 0 && oz4 === 0) ? c4.distance
          : Math.sqrt(dx4 * dx4 + dy4 * dy4 + dz4 * dz4);
        if ((mask.shape || "round") === "heart") rad4 *= heartRadius(Math.atan2(dy4, dx4));
        var arc4 = clamp01(get(values, mask.arc, 1.0, legIndex));
        if (arc4 < 1) {
          // Every element clipped to the SAME wedge, which is what makes a
          // tiling read as shingled rather than as a closed net.
          var a4 = la === "X" ? Math.atan2(dy4, dz4)
            : (la === "Y" ? Math.atan2(dz4, dx4) : Math.atan2(dy4, dx4));
          var turn4 = a4 / (2 * Math.PI) - get(values, mask.angle, 0.0, legIndex) / 360 + 0.5;
          if ((((turn4 % 1) + 1) % 1) > arc4) return 0;
        }
        var soft4 = Math.max(1e-6, get(values, mask.softness, 0.25, legIndex));
        return 1 - smoothstep(rad4, rad4 + soft4, dist4 * spacing4);
      }
      case "RINGS": {
        var sr = getSeed(values, mask.seed, seedBase);
        var spacing5 = Math.max(0.05, get(values, mask.spacing, 6.0, legIndex));
        var c5 = noise.cell(sr, point.x / spacing5, point.y / spacing5, point.z / spacing5);
        if (c5.pick >= get(values, mask.chance, 1.0, legIndex)) return 0;
        var vary5 = clamp01(get(values, mask.vary, 0.4, legIndex));
        var rad5 = get(values, mask.radius, 2.0, legIndex) * (1 - vary5 + 2 * vary5 * c5.size);
        var half5 = Math.max(1e-6, get(values, mask.thickness, 0.6, legIndex)) / 2;
        var soft5 = Math.max(1e-6, get(values, mask.softness, 0.2, legIndex));
        var ox5 = get(values, mask.offsetX, 0.0, legIndex) / spacing5;
        var oy5 = get(values, mask.offsetY, 0.0, legIndex) / spacing5;
        var oz5 = get(values, mask.offsetZ, 0.0, legIndex) / spacing5;
        var dx5 = c5.dx - ox5, dy5 = c5.dy - oy5, dz5 = c5.dz - oz5;
        var dist5 = (ox5 === 0 && oy5 === 0 && oz5 === 0) ? c5.distance
          : Math.sqrt(dx5 * dx5 + dy5 * dy5 + dz5 * dz5);
        var ring = 1 - smoothstep(half5, half5 + soft5, Math.abs(dist5 * spacing5 - rad5));
        var arc = clamp01(get(values, mask.arc, 1.0, legIndex));
        if (arc >= 1) return ring;
        var theta5 = Math.atan2(dy5, dz5) / (2 * Math.PI) + 0.5;
        return ((((theta5 - c5.angle) % 1) + 1) % 1) <= arc ? ring : 0;
      }
      case "SPECKLE": {
        var ss = getSeed(values, mask.seed, seedBase);
        var spacing6 = Math.max(0.02, get(values, mask.spacing, 0.7, legIndex));
        var c6 = noise.cell(ss, point.x / spacing6, point.y / spacing6, point.z / spacing6);
        var density = clamp01(get(values, mask.density, 0.5, legIndex));
        var clumping = clamp01(get(values, mask.clumping, 0.0, legIndex));
        if (clumping > 0) {
          var cs = Math.max(0.05, get(values, mask.clumpScale, 7.0, legIndex));
          var n6 = noise.value(noise.xor(ss, noise.u64(0, 0x5EC1E)),
            point.x / cs, point.y / cs, point.z / cs);
          density = clamp01(density * (1 - clumping + 2 * clumping * n6));
        }
        if (c6.pick >= density) return 0;
        var rad6 = clamp01(get(values, mask.size, 0.45, legIndex));
        var soft6 = Math.max(1e-6, get(values, mask.softness, 0.3, legIndex)) * rad6;
        return 1 - smoothstep(rad6, rad6 + soft6, c6.distance);
      }
      case "STROKES": {
        var st = getSeed(values, mask.seed, seedBase);
        var spacing7 = Math.max(0.05, get(values, mask.spacing, 3.0, legIndex));
        var length7 = Math.max(0.05, get(values, mask.length, 12.0, legIndex));
        var la7 = (mask.axis || "X").toUpperCase();
        var curl = get(values, mask.curl, 0.35, legIndex) * spacing7;
        var w7 = noise.value(noise.xor(st, noise.u64(0, 0x71)),
          point.x / (spacing7 * 4), point.y / (spacing7 * 4), point.z / (spacing7 * 4));
        var sx = (point.x + (la7 === "X" ? 0 : (w7 - 0.5) * curl)) / (la7 === "X" ? length7 : spacing7);
        var sy = (point.y + (la7 === "Y" ? 0 : (w7 - 0.5) * curl)) / (la7 === "Y" ? length7 : spacing7);
        var sz = (point.z + (la7 === "Z" ? 0 : (w7 - 0.5) * curl)) / (la7 === "Z" ? length7 : spacing7);
        var half7 = Math.max(1e-4, get(values, mask.width, 0.8, legIndex)) / 2;
        var soft7 = Math.max(1e-6, get(values, mask.softness, 0.25, legIndex));
        return 1 - smoothstep(half7, half7 + soft7, levelSetDistance(st, sx, sy, sz) * spacing7);
      }
      case "SPIRAL": {
        var s8 = getSeed(values, mask.seed, seedBase);
        var b8 = geo.bounds(skin, part);
        if (!b8) return 0;
        var view = (mask.axis || "Z").toUpperCase();
        var u8 = view === "X" ? "Y" : "X";
        var w8 = view === "Z" ? "Y" : "Z";
        var cu = (b8.min(u8) + b8.max(u8)) / 2 + get(values, mask.offset, 0.0, legIndex) * b8.span(u8);
        var cw = (b8.min(w8) + b8.max(w8)) / 2;
        var du = (u8 === "X" ? point.x : u8 === "Y" ? point.y : point.z) - cu;
        var dw = (w8 === "X" ? point.x : w8 === "Y" ? point.y : point.z) - cw;
        var r8 = Math.sqrt(du * du + dw * dw);
        var outer = Math.max(0.05, get(values, mask.radius, 4.0, legIndex));
        if (r8 > outer) return 0;
        var turns = Math.max(0.25, get(values, mask.turns, 2.0, legIndex));
        var pitch = outer / turns;
        var theta8 = Math.atan2(dw, du) / (2 * Math.PI) + (s8.l & 0xFF) / 255;
        var offsetR = r8 - (((theta8 % 1) + 1) % 1) * pitch;
        var nearest = Math.abs(offsetR - Math.round(offsetR / pitch) * pitch);
        var half8 = Math.max(1e-6, get(values, mask.width, 0.5, legIndex)) / 2;
        var soft8 = Math.max(1e-6, get(values, mask.softness, 0.2, legIndex));
        return 1 - smoothstep(half8, half8 + soft8, nearest);
      }
      case "WAVES": {
        var s9 = getSeed(values, mask.seed, seedBase);
        var along = (mask.axis || "X").toUpperCase();
        var across = (mask.across || "Y").toUpperCase();
        var travel = along === "X" ? point.x : along === "Y" ? point.y : point.z;
        var coord9 = across === "X" ? point.x : across === "Y" ? point.y : point.z;
        var space9 = mask.space || "part";
        var t9 = space9 === "body" ? normalise(coord9, geo.bodyBounds(skin), across)
          : space9 === "units" ? coord9
            : space9 === "local" ? axisOf(geo.local(skin, part, point), across)
              : normalise(coord9, geo.bounds(skin, part), across);
        var shape9 = mask.shape || "sine";
        var lambda = Math.max(0.05, get(values, mask.wavelength, 8.0, legIndex));
        var amp = get(values, mask.amplitude, 0.5, legIndex);
        var from9 = get(values, mask.from, 0.0, legIndex);
        var to9 = get(values, mask.to, 1.0, legIndex);
        var soft9 = get(values, mask.softness, 0.15, legIndex);
        var pitch9 = get(values, mask.spacing, 0.0, legIndex);
        if (pitch9 <= 0) return band(t9 - amp * waveform(shape9, travel / lambda), from9, to9, soft9);
        var ph = get(values, mask.phase, 0.0, legIndex);
        var mid9 = (from9 + to9) / 2;
        var lane = Math.round((t9 - mid9) / pitch9);
        var best9 = 0;
        for (var n9 = lane - 1; n9 <= lane + 1; n9++) {
          var jitter = ph === 0 ? 0
            : ph * noise.value(noise.xor(s9, noise.u64(0, 0x77)), n9, 0.25, 0.25);
          var centre = n9 * pitch9 + amp * waveform(shape9, travel / lambda + jitter);
          best9 = Math.max(best9, band(t9 - centre, from9, to9, soft9));
        }
        return best9;
      }
      case "GOO": {
        var sg = getSeed(values, mask.seed, seedBase);
        var alongG = (mask.axis || "X").toUpperCase();
        var acrossG = (mask.across || "Y").toUpperCase();
        var coordG = axisOf(point, acrossG);
        // A face the drips run INTO has one 'along' value over its whole
        // surface, so it would take a single drip's answer for the lot. Carry
        // the pattern round the corner from each side instead.
        var travelG = axisOf(point, alongG);
        var boxG = geo.bounds(skin, part);
        var faceG = geo.FACES[face];
        if (boxG && faceG && faceG.normal === alongG) {
          var lateralG = (alongG !== "Z" && acrossG !== "Z") ? "Z"
            : ((alongG !== "X" && acrossG !== "X") ? "X" : "Y");
          var midG = (boxG.min(lateralG) + boxG.max(lateralG)) / 2;
          var reachG = boxG.span(lateralG) / 2 - Math.abs(axisOf(point, lateralG) - midG);
          travelG = faceG.atMax ? boxG.max(alongG) + reachG : boxG.min(alongG) - reachG;
        }
        var spaceG = mask.space || "part";
        var boundsG = spaceG === "body" ? geo.bodyBounds(skin) : geo.bounds(skin, part);
        var tG = spaceG === "body" ? normalise(coordG, geo.bodyBounds(skin), acrossG)
          : spaceG === "units" ? coordG
            : spaceG === "local" ? axisOf(geo.local(skin, part, point), acrossG)
              : normalise(coordG, geo.bounds(skin, part), acrossG);
        // Every length but 'from' and 'to' is in body units - a drip is a round
        // shape and has to stay round - so the band's span is what converts.
        var spanG = (spaceG === "units" || spaceG === "local") ? 1
          : (boundsG ? boundsG.span(acrossG) : 1);
        var fromG = get(values, mask.from, 0.75, legIndex);
        var toG = get(values, mask.to, 1.6, legIndex);
        var spacingG = Math.max(0.05, get(values, mask.spacing, 4.0, legIndex));
        var dropG = Math.max(0, get(values, mask.drop, 3.0, legIndex));
        var widthG = Math.max(0.05, get(values, mask.width, 1.6, legIndex));
        var bulbG = Math.max(0, get(values, mask.bulb, 1.5, legIndex));
        var varyG = Math.min(1, Math.max(0, get(values, mask.vary, 0.6, legIndex)));
        var chanceG = get(values, mask.chance, 0.75, legIndex);
        var wobbleG = get(values, mask.wobble, 0.5, legIndex);
        var sagG = Math.max(0, get(values, mask.sag, 0.6, legIndex));
        var softG = Math.max(1e-6, get(values, mask.softness, 0.1, legIndex));

        var dirG = toG >= fromG ? 1 : -1;
        var depthG = (fromG - tG) * spanG * dirG
          + wobbleG * (noise.value(noise.xor(sg, noise.u64(0, 0x51)), travelG / (spacingG * 2.5), 0.5, 0.5) * 2 - 1);
        var thicknessG = Math.abs(toG - fromG) * spanG;

        var sdG = depthG;
        var filletG = Math.max(1e-6, widthG * 0.5);
        var cellG = Math.floor(travelG / spacingG);
        for (var iG = cellG - 1; iG <= cellG + 1; iG++) {
          if (noise.value(noise.xor(sg, noise.u64(0, 0xA4)), iG * 0.73 + 0.19, 0.61, 0.29) > chanceG) continue;
          var jitterG = (noise.value(noise.xor(sg, noise.u64(0, 0xA1)), iG * 0.73 + 0.19, 0.31, 0.57) - 0.5) * 0.7 * varyG;
          var cxG = (iG + 0.5 + jitterG) * spacingG;
          var lenG = dropG * (1 - varyG * noise.value(noise.xor(sg, noise.u64(0, 0xA2)), iG * 0.73 + 0.19, 0.13, 0.83));
          var halfG = widthG * 0.5
            * (1 - 0.45 * varyG * noise.value(noise.xor(sg, noise.u64(0, 0xA3)), iG * 0.73 + 0.19, 0.47, 0.11));
          var qxG = travelG - cxG;
          var hG = Math.min(1, Math.max(0, depthG / Math.max(1e-6, lenG)));
          var stemG = Math.hypot(qxG, depthG - hG * lenG) - halfG;
          var tipG = Math.hypot(qxG, depthG - lenG) - halfG * bulbG;
          sdG = smoothMin(sdG, Math.min(stemG, tipG), filletG);
        }
        // The straight edge between two drips arcs up when a disc is bitten
        // out of it at every cell boundary - a draining meniscus.
        if (sagG > 0) {
          var biteG = sagG * 1.4;
          for (var jG = cellG - 1; jG <= cellG + 2; jG++) {
            var dG = Math.hypot(travelG - jG * spacingG, depthG + biteG * 0.35) - biteG;
            sdG = -smoothMin(-sdG, dG, filletG);
          }
        }
        return 1 - smoothstep(0, softG, Math.max(sdG, -(depthG + thicknessG)));
      }
      case "CRACKLE": {
        var sc = getSeed(values, mask.seed, seedBase);
        var scaleC = Math.max(0.05, get(values, mask.scale, 5.0, legIndex));
        var warpC = get(values, mask.warp, 0.35, legIndex);
        var wx = point.x / scaleC, wy = point.y / scaleC, wz = point.z / scaleC;
        if (warpC !== 0) {
          var nC = noise.value(noise.xor(sc, noise.u64(0, 0x2C)), wx / 3, wy / 3, wz / 3);
          var mC = noise.value(noise.xor(sc, noise.u64(0, 0x2D)), wz / 3, wx / 3, wy / 3);
          wx += (nC - 0.5) * warpC;
          wy += (mC - 0.5) * warpC;
          wz += (nC - mC) * warpC;
        }
        var chanceC = get(values, mask.chance, 1.0, legIndex);
        if (chanceC < 1 && noise.cell(sc, wx, wy, wz).pick >= chanceC) return 0;
        var halfC = Math.max(1e-4, get(values, mask.gap, 0.5, legIndex)) / 2;
        var softC = Math.max(1e-6, get(values, mask.softness, 0.08, legIndex));
        if ((mask.measure || "wall") === "centroid") {
          // The SAME tessellation, read from the middle out - the only per-cell
          // radial coordinate a crackle outline is guaranteed concentric with.
          return smoothstep(halfC, halfC + softC, noise.cellDistance(sc, wx, wy, wz) * scaleC);
        }
        var edgeC = noise.cellEdge(sc, wx, wy, wz) * scaleC;
        var vwC = clamp01(get(values, mask.vertexWeight, 0.0, legIndex));
        if (vwC > 0) {
          edgeC = edgeC * (1 - vwC) + noise.cellVertex(sc, wx, wy, wz) * scaleC * vwC;
        }
        return smoothstep(halfC, halfC + softC, edgeC);
      }
      case "SVG": {
        var shape = svgShape(mask);
        var viewBox = svgViewBox(mask, shape);
        if (!shape || !viewBox) return 0;
        var wholeS = geo.bodyBounds(skin);
        var plS = mask.plane || "side";
        var uAxisS = plS === "front" ? "Z" : "X";
        var vAxisS = plS === "top" ? "Z" : "Y";
        var normS = (mask.space || "body") !== "units";
        var uMinS = normS ? wholeS.min(uAxisS) : 0;
        var uSpanS = normS ? Math.max(1e-6, wholeS.span(uAxisS)) : 1;
        var vMinS = normS ? wholeS.min(vAxisS) : 0;
        var vSpanS = normS ? Math.max(1e-6, wholeS.span(vAxisS)) : 1;
        var vwS = get(values, mask.sizeU, 1.0, legIndex) * uSpanS;
        var vhS = get(values, mask.sizeV, 1.0, legIndex) * vSpanS;
        if (Math.abs(vwS) < 1e-9 || Math.abs(vhS) < 1e-9) return 0;
        var fitS = svg.fit(viewBox,
          uMinS + get(values, mask.originU, 0.0, legIndex) * uSpanS,
          vMinS + get(values, mask.originV, 0.0, legIndex) * vSpanS,
          vwS, vhS, mask.fit || "meet", mask.align || "xMidYMid");
        var puS = uAxisS === "X" ? point.x : point.z;
        var pvS = vAxisS === "Z" ? point.z : point.y;
        var xS = (puS - fitS[2]) / fitS[0];
        var yS = (pvS - fitS[3]) / fitS[1];
        // SVG's y runs down the page and the horse's runs up.
        if (mask.flipY !== false) yS = 2 * viewBox[1] + viewBox[3] - yS;
        var toBodyS = Math.sqrt(Math.abs(fitS[0] * fitS[1]));
        var softS = Math.max(1e-6, get(values, mask.softness, 0.25, legIndex));
        if (mask.fill !== false) {
          if (svg.inside(shape, xS, yS, (mask.fillRule || "nonzero") === "evenodd")) return 1;
          svg.distance(shape, xS, yS, "round", 0, SVG_HIT);
          return 1 - smoothstep(0, softS, SVG_HIT[0] * toBodyS);
        }
        var halfS = Math.max(0, get(values, mask.width, 1.0, legIndex)) / 2;
        var halfUserS = toBodyS < 1e-9 ? 0 : halfS / toBodyS;
        svg.distance(shape, xS, yS, mask.cap || "butt", halfUserS, SVG_HIT);
        var dS = SVG_HIT[0];
        if ((mask.join || "miter") === "miter") {
          dS = Math.min(dS, svg.miterDistance(shape, xS, yS, halfUserS,
            Math.max(1, get(values, mask.miterLimit, 4.0, legIndex))));
        }
        var dashS = get(values, mask.dash, 0.0, legIndex);
        if (dashS > 0) {
          var gapS = get(values, mask.gap, 0.0, legIndex);
          var periodS = dashS + (gapS > 0 ? gapS : dashS);
          var alongS = SVG_HIT[1] * toBodyS + get(values, mask.dashOffset, 0.0, legIndex);
          if ((((alongS % periodS) + periodS) % periodS) > dashS) return 0;
        }
        return 1 - smoothstep(halfS, halfS + softS, dS * toBodyS);
      }
      case "FAN": {
        var wholeF = geo.bodyBounds(skin);
        var plF = mask.plane || "side";
        var uAxisF = plF === "front" ? "Z" : "X";
        var vAxisF = plF === "top" ? "Z" : "Y";
        var normF = (mask.space || "body") !== "units";
        var ouF = normF
          ? wholeF.min(uAxisF) + get(values, mask.originU, 0.5, legIndex) * wholeF.span(uAxisF)
          : get(values, mask.originU, 0.5, legIndex);
        var ovF = normF
          ? wholeF.min(vAxisF) + get(values, mask.originV, 0.5, legIndex) * wholeF.span(vAxisF)
          : get(values, mask.originV, 0.5, legIndex);
        var duF = (uAxisF === "X" ? point.x : point.z) - ouF;
        var dvF = (vAxisF === "Z" ? point.z : point.y) - ovF;
        var rF = Math.sqrt(duF * duF + dvF * dvF);
        var innerF = get(values, mask.inner, 0.0, legIndex);
        var outerF = get(values, mask.outer, 0.0, legIndex);
        if (rF < innerF || (outerF > 0 && rF > outerF) || rF < 1e-6) return 0;
        // The phase is an ANGLE and the period an arc length at one unit out, so
        // the bars widen with distance - what a linear coordinate cannot do.
        var spacingF = Math.max(1e-4, get(values, mask.spacing, 2.0, legIndex));
        var twistF = get(values, mask.twist, 0.0, legIndex) * rF / 360;
        var turnsF = (Math.atan2(dvF, duF) / (2 * Math.PI) + twistF) * (2 * Math.PI / spacingF);
        var fF = turnsF - Math.floor(turnsF);
        var dutyF = clamp01(get(values, mask.duty, 0.5, legIndex));
        var softF = Math.max(1e-6, clamp01(get(values, mask.softness, 0.15, legIndex)) * dutyF);
        return Math.max(band(fF, 0, dutyF, softF), band(fF - 1, 0, dutyF, softF));
      }
      case "NORMAL": {
        var axisN = (mask.axis || "Y").toUpperCase();
        var bN = geo.bounds(skin, part);
        var faceN = geo.FACES[face];
        var nN = faceN.normal === axisN ? (faceN.atMax ? 1 : -1) : 0;
        var roundN = clamp01(get(values, mask.round, 0.0, legIndex));
        if (roundN > 0 && bN) {
          // The normal the part would have if its box were an ellipsoid -
          // continuous, where a flat face normal takes three values.
          var localN = (mask.space || "body") === "local";
          var atN = localN ? geo.local(skin, part, point) : point;
          var exN = localN ? (atN.x - 0.5) * 2 : ellipsoid(atN, bN, "X");
          var eyN = localN ? (atN.y - 0.5) * 2 : ellipsoid(atN, bN, "Y");
          var ezN = localN ? (atN.z - 0.5) * 2 : ellipsoid(atN, bN, "Z");
          var lenN = Math.sqrt(exN * exN + eyN * eyN + ezN * ezN);
          if (lenN > 1e-9) {
            var roundedN = axisN === "X" ? exN / lenN : (axisN === "Y" ? eyN / lenN : ezN / lenN);
            nN = nN * (1 - roundN) + roundedN * roundN;
          }
        }
        var fromN = get(values, mask.from, -1.0, legIndex);
        var toN = get(values, mask.to, 1.0, legIndex);
        return toN === fromN ? (nN >= toN ? 1 : 0) : clamp01((nN - fromN) / (toN - fromN));
      }
      default:
        return 0;
    }
  }

  /**
   * One component of the normal a part's box would have if it were an ellipsoid.
   * Mirrors SpecPainter.ellipsoid.
   */
  function ellipsoid(point, bounds, axis) {
    var half = Math.max(1e-6, bounds.span(axis) / 2);
    var coord = axis === "X" ? point.x : (axis === "Y" ? point.y : point.z);
    return (coord - (bounds.min(axis) + half)) / half;
  }

  // Scratch for svg.distance - three numbers, reused rather than allocated per
  // texel. Single-threaded here, unlike the Java, which needs a thread local.
  var SVG_HIT = [0, 0, 0];

  /**
   * An SVG mask's flattened path, cached ON THE MASK.
   *
   * The Java flattens at parse time and hands the painter a Shape; here the
   * mask is a plain object the editor rewrites on every keystroke, so the cache
   * is keyed on the two strings that produce it. A bad `d` caches the failure
   * too - otherwise a typo re-parses eight thousand times per preview.
   */
  function svgShape(mask) {
    var key = String(mask.d || "") + "\u0000" + String(mask.transform || "");
    if (mask.__svgKey !== key) {
      mask.__svgKey = key;
      try {
        mask.__svg = mask.d ? svg.parse(mask.d, mask.transform) : null;
      } catch (e) {
        mask.__svg = null;
        mask.__svgError = e.message;
      }
      if (mask.__svg) mask.__svgError = null;
    }
    return mask.__svg;
  }

  /** The declared viewBox, or the drawing's own bounds when the file left it out. */
  function svgViewBox(mask, shape) {
    if (mask.viewBox && mask.viewBox.length === 4) return mask.viewBox;
    if (!shape) return null;
    return [shape.box[0], shape.box[1],
      Math.max(1e-9, shape.box[2] - shape.box[0]),
      Math.max(1e-9, shape.box[3] - shape.box[1])];
  }

  /** One cycle of a WAVES displacement, in [-1, 1] - the port of SpecPainter.waveform. */
  function waveform(shape, turns) {
    var t = turns - Math.floor(turns);
    if (shape === "triangle") return t < 0.25 ? 4 * t : (t < 0.75 ? 2 - 4 * t : 4 * t - 4);
    if (shape === "saw") return 2 * t - 1;
    return Math.sin(2 * Math.PI * turns);
  }

  /**
   * A PIGMENT mask's `spread`: grow whatever the mask selected, by taking the
   * largest coverage in a disc. It runs after `invert` - see the Java for why
   * that is the whole design.
   */
  function spreadMask(mask, values, skin, coat, colour, part, face, point, px, py, legIndex, c) {
    var luma = mask.type === "LUMA";
    if ((mask.type !== "PIGMENT" && !luma) || c >= 1) return c;
    var parts = HG.schema.expandParts(mask.parts);
    if (parts.length && parts.indexOf(part) < 0) return c;
    var radius = get(values, mask.spread, 0, legIndex);
    if (radius <= 0) return c;
    var from = mask.spreadFrom || "any";
    // The radius is in BODY units and the test is done in body space - a disc
    // measured in texels leaks across a UV seam. See the Java.
    var r = Math.ceil(radius * geo.TEXELS_PER_UNIT) + 1, rr = radius * radius, best = c;
    for (var qy = py - r; qy <= py + r; qy++) {
      for (var qx = px - r; qx <= px + r; qx++) {
        if (qx < 0 || qy < 0 || qx >= geo.SHEET_SIZE || qy >= geo.SHEET_SIZE) continue;
        var at = geo.sample(skin, qx, qy);
        if (!at) continue;
        var bx = at.point.x - point.x, by = at.point.y - point.y, bz = at.point.z - point.z;
        if (bx * bx + by * by + bz * bz > rr) continue;
        if (!onSide(from, bx, by)) continue;
        var n = luma ? lumaCoverage(mask, values, colour, qx, qy, legIndex)
          : pigmentCoverage(mask, values, coat, qx, qy, legIndex);
        if (mask.invert) n = 1 - n;
        if (n > best) { best = n; if (best >= 1) return 1; }
      }
    }
    return best;
  }

  /** A PIGMENT mask at one texel, before invert and before spread. */
  function pigmentCoverage(mask, values, coat, px, py, legIndex) {
    return smoothstep(get(values, mask.from, 0.5, legIndex),
      get(values, mask.to, 1, legIndex),
      pigmentReading(coat, mask.channel || "darkness", px, py));
  }

  /**
   * Does a candidate `(bx, by)` away count, given which side the growth may
   * come from? A candidate ABOVE has by > 0, so "above" grows the selection
   * downward. Strict inequalities - see the Java.
   */
  function onSide(from, bx, by) {
    if (from === "above") return by > 0;
    if (from === "below") return by < 0;
    if (from === "ahead") return bx > 0;
    if (from === "behind") return bx < 0;
    return true;
  }

  /** How far `point` is from the nearer of this part's two bounds along `axis`. */
  function edgeDistance(point, b, axis) {
    var c = axis === "X" ? point.x : axis === "Y" ? point.y : point.z;
    return Math.min(c - b.min(axis), b.max(axis) - c);
  }

  /** A LUMA mask at one texel, before invert and before spread. */
  function lumaCoverage(mask, values, colour, px, py, legIndex) {
    if (!colour) return 0; // no accumulator in this phase - the parser refuses to get here
    return smoothstep(get(values, mask.from, 0.5, legIndex),
      get(values, mask.to, 1, legIndex),
      lumaReading(colour, mask.channel || "dark", px, py));
  }

  /**
   * One channel of the RESOLVED COLOUR, as a LUMA mask reads it - the port of
   * SpecPainter.lumaReading. Every reading comes off `visible`, so a texel the
   * natural phase left bare reads as the white template a viewer sees.
   */
  function lumaReading(colour, channel, px, py) {
    var r = colour.visible(px, py, 0) / 255;
    var g = colour.visible(px, py, 1) / 255;
    var b = colour.visible(px, py, 2) / 255;
    var light = clamp01(0.2126 * r + 0.7152 * g + 0.0722 * b);
    if (channel === "light") return light;
    if (channel === "white") return clamp01(Math.min(r, Math.min(g, b)));
    if (channel === "saturation") {
      var max = Math.max(r, Math.max(g, b));
      return max <= 0 ? 0 : clamp01((max - Math.min(r, Math.min(g, b))) / max);
    }
    if (channel === "red") return clamp01(r);
    if (channel === "green") return clamp01(g);
    if (channel === "blue") return clamp01(b);
    return clamp01(1 - light);
  }

  /** One channel of the coat, as a PIGMENT mask reads it. */
  function pigmentReading(coat, channel, px, py) {
    var red = coat.redAt(px, py), black = coat.blackAt(px, py);
    return channel === "red" ? red
      : channel === "black" ? black
        : channel === "total" ? (red + black) / 2
          : clamp01(0.55 * red + 0.95 * black);
  }

  /**
   * The radius of a heart at this angle, as a multiple of the round radius -
   * the port of SpecPainter.heartRadius.
   */
  function heartRadius(theta) {
    var sin = Math.sin(theta), cos = Math.cos(theta);
    return (2 - 2 * sin + sin * Math.sqrt(Math.abs(cos)) / (sin + 1.4)) / 2.4;
  }

  // NOTE: the game refuses a layer whose FIRST mask combines by MAX or ADD -
  // coverage starts at 1, so either returns 1 and the layer covers everything.
  // The creator cannot write one (the picker defaults to MULTIPLY), so this
  // mirrors the arithmetic and leaves the refusing to GeneSpecParser.
  function coverage(layer, values, skin, part, face, point, coat, colour, px, py, legIndex, fallbackSeed) {
    var acc = 1;
    var masks = layer.masks || [];
    for (var i = 0; i < masks.length; i++) {
      var mask = masks[i];
      var c = maskCoverage(mask, values, skin, part, face, point, coat, colour, px, py, legIndex,
        noise.xor(fallbackSeed, noise.mul(noise.fromInt(i), noise.K1)));
      // A mask the parts test ruled out does not apply here, and what that
      // means depends on its POSITION: the first mask defines the layer's
      // region (so outside it, coverage is 0), every later mask modifies that
      // region (so outside it, the mask contributes its combine's identity -
      // which is to say, nothing). See the Java for the two genes each half of
      // that rule exists for.
      if (excludedByParts(mask, part)) {
        if (i === 0) {
          acc = 0;
          if (cannotRise(masks, 1)) return 0;
        }
        continue;
      }
      if (mask.invert) c = 1 - c;
      c = spreadMask(mask, values, skin, coat, colour, part, face, point, px, py, legIndex, c);
      switch (mask.combine || "MULTIPLY") {
        case "MAX": acc = Math.max(acc, c); break;
        case "MIN": acc = Math.min(acc, c); break;
        case "ADD": acc = clamp01(acc + c); break;
        case "SUBTRACT": acc = clamp01(acc - c); break;
        default: acc = acc * c;
      }
      // Only bail out when nothing left can put coverage back - see the Java.
      // The old test threw away every union of masks.
      if (acc <= 0 && cannotRise(masks, i + 1)) return 0;
    }
    return clamp01(acc);
  }

  /**
   * Did this mask's `parts` list rule the texel out? PARTS is exempt: there the
   * list IS the mask, so an inverted one means "everywhere except these". See
   * the Java.
   */
  function excludedByParts(mask, part) {
    if (mask.type === "PARTS") return false;
    var parts = HG.schema.expandParts(mask.parts);
    return parts.length > 0 && parts.indexOf(part) < 0;
  }

  /** Can any mask from `from` on raise the accumulator above zero? */
  function cannotRise(masks, from) {
    for (var i = from; i < masks.length; i++) {
      var c = masks[i].combine || "MULTIPLY";
      if (c === "MAX" || c === "ADD") return false;
    }
    return true;
  }

  // ---- ops -------------------------------------------------------------

  function hexToRgb(hex) {
    var s = String(hex || "#ffffff").replace("#", "");
    return [parseInt(s.slice(0, 2), 16) || 0, parseInt(s.slice(2, 4), 16) || 0, parseInt(s.slice(4, 6), 16) || 0];
  }

  function percentToChannel(percent) { return Math.round(255 * percent / 100); }

  function applyPigment(op, values, f, px, py, legIndex, k) {
    switch (op.type) {
      case "DILUTE": {
        var keepRed = lerp(1, get(values, op.keepRed, 1, legIndex), k);
        var keepBlack = lerp(1, get(values, op.keepBlack, 1, legIndex), k);
        f.dilute(px, py, keepRed, keepBlack, get(values, op.blackTint, 0, legIndex) * k);
        break;
      }
      case "RESTRICT":
        f.restrictRed(px, py, get(values, op.red, 0, legIndex) * k);
        f.restrictBlack(px, py, get(values, op.black, 0, legIndex) * k);
        break;
      case "SET_PIGMENT":
        if (op.red !== undefined) f.setRed(px, py, lerp(f.redAt(px, py), get(values, op.red, 0, legIndex), k));
        if (op.black !== undefined) f.setBlack(px, py, lerp(f.blackAt(px, py), get(values, op.black, 0, legIndex), k));
        break;
      case "WHITEN":
        f.whiten(px, py, get(values, op.amount, 1, legIndex) * k);
        break;
    }
  }

  /** HSL to [r, g, b] - the port of SpecPainter.hsl. */
  function hsl(h, s, l) {
    var hue = ((((h % 360) + 360) % 360)) / 60;
    var sat = clamp01(s), light = clamp01(l);
    var c = (1 - Math.abs(2 * light - 1)) * sat;
    var x = c * (1 - Math.abs(hue % 2 - 1));
    var m = light - c / 2;
    var r, g, b;
    if (hue < 1) { r = c; g = x; b = 0; }
    else if (hue < 2) { r = x; g = c; b = 0; }
    else if (hue < 3) { r = 0; g = c; b = x; }
    else if (hue < 4) { r = 0; g = x; b = c; }
    else if (hue < 5) { r = x; g = 0; b = c; }
    else { r = c; g = 0; b = x; }
    return [channel8(r + m), channel8(g + m), channel8(b + m)];
  }

  function channel8(v) {
    var i = Math.round(v * 255);
    return i < 0 ? 0 : (i > 255 ? 255 : i);
  }

  // A TOWARD / FLAT layer's target: the literal colour, unless hue is 0 or
  // above. A negative sentinel rather than a presence test, because the creator
  // carries every parameter at its default whether or not you touched one - see
  // the note on SpecPainter.solidColour.
  function solidColour(op, values, legIndex) {
    var hue = get(values, op.hue, -1, legIndex);
    if (hue < 0) return hexToRgb(op.color);
    return hsl(hue, get(values, op.saturation, 0.8, legIndex),
      get(values, op.lightness, 0.55, legIndex));
  }

  function mixRgb(a, b, t) {
    return [Math.round(lerp(a[0], b[0], t)), Math.round(lerp(a[1], b[1], t)), Math.round(lerp(a[2], b[2], t))];
  }

  function rampColour(op, values, legIndex, t) {
    var stops = op.colors || [];
    if (!stops.length) {
      var span = get(values, op.hueSpan, 60, legIndex);
      return hsl(get(values, op.hue, 0, legIndex) + span * t,
        get(values, op.saturation, 0.8, legIndex),
        get(values, op.lightness, 0.55, legIndex));
    }
    var scaled = t * (stops.length - 1);
    var i = Math.floor(scaled);
    if (i >= stops.length - 1) return hexToRgb(stops[stops.length - 1]);
    return mixRgb(hexToRgb(stops[i]), hexToRgb(stops[i + 1]), scaled - i);
  }

  /** One component of a {x,y,z} triple by body-axis name. */
  function axisOf(p, axis) {
    return axis === "X" ? p.x : axis === "Y" ? p.y : p.z;
  }

  function axisPosition(op, values, legIndex, skin, part, point, seedBase) {
    var axisName = op.axis || "X";
    if (axisName !== "X" && axisName !== "Y" && axisName !== "Z") {
      // Three of the six answers are not axes. A straight line is monotonic by
      // construction; a colour that wanders, or is decided per cell, is not a
      // tuning of that. Mirrors SpecPainter.axisPosition.
      var sR = getSeed(values, op.seed, seedBase);
      var scaleR = Math.max(0.05, get(values, op.scale, 5.0, legIndex));
      if (axisName === "noise") {
        return clamp01(noise.value(sR, point.x / scaleR, point.y / scaleR, point.z / scaleR));
      }
      if (axisName === "cell") {
        return clamp01(noise.cellDistance(sR, point.x / scaleR, point.y / scaleR, point.z / scaleR));
      }
      return clamp01(noise.cell(sR, point.x / scaleR, point.y / scaleR, point.z / scaleR).pick);
    }
    return spatialAxisPosition(op, values, legIndex, skin, part, point);
  }

  function spatialAxisPosition(op, values, legIndex, skin, part, point) {
    var axis = (op.axis || "X").toUpperCase();
    var coord = axis === "X" ? point.x : axis === "Y" ? point.y : point.z;
    var space = op.space || "part";
    var t = space === "body" ? normalise(coord, geo.bodyBounds(skin), axis)
      : space === "units" ? coord
        : space === "local" ? axisOf(geo.local(skin, part, point), axis)
          : normalise(coord, geo.bounds(skin, part), axis);
    var from = get(values, op.from, 0, legIndex);
    var to = get(values, op.to, 1, legIndex);
    return to === from ? 0 : clamp01((t - from) / (to - from));
  }

  function paletteColour(op, values, legIndex, point, seedBase) {
    var seed = getSeed(values, op.seed, seedBase);
    var scale = Math.max(0.05, get(values, op.scale, 5.0, legIndex));
    var c = noise.cell(seed, point.x / scale, point.y / scale, point.z / scale);
    var palette = op.colors || [];
    if (palette.length) {
      return hexToRgb(palette[Math.min(Math.floor(c.pick * palette.length), palette.length - 1)]);
    }
    var spread = get(values, op.hueSpread, 40, legIndex);
    return hsl(get(values, op.hue, 0, legIndex) + (c.pick * 2 - 1) * spread,
      get(values, op.saturation, 0.8, legIndex),
      get(values, op.lightness, 0.55, legIndex));
  }

  function toward(colour, px, py, channel, target, strength) {
    var seen = colour.visible(px, py, channel);
    var wanted = seen + (target - seen) * strength;
    var stored = channel === 0 ? colour.redAt(px, py)
      : channel === 1 ? colour.greenAt(px, py) : colour.blueAt(px, py);
    return Math.round(wanted - stored);
  }

  function towardColour(delta, colour, op, values, legIndex, px, py, k, rgb) {
    var strength = get(values, op.strength, 100, legIndex) / 100 * k;
    delta.add(px, py,
      toward(colour, px, py, 0, rgb[0], strength),
      toward(colour, px, py, 1, rgb[1], strength),
      toward(colour, px, py, 2, rgb[2], strength));
    var want = percentToChannel(get(values, op.opacity, 100, legIndex));
    delta.addOpacity(px, py, Math.round((want - colour.opacityAt(px, py)) * k));
  }

  function applyColour(op, values, delta, colour, skin, part, point, px, py, legIndex, k, seedBase) {
    switch (op.type) {
      case "TINT":
        delta.add(px, py,
          percentToChannel(get(values, op.red, 0, legIndex) * k),
          percentToChannel(get(values, op.green, 0, legIndex) * k),
          percentToChannel(get(values, op.blue, 0, legIndex) * k));
        delta.addOpacity(px, py, percentToChannel(get(values, op.opacity, 100, legIndex) * k));
        break;
      case "TOWARD":
        towardColour(delta, colour, op, values, legIndex, px, py, k, solidColour(op, values, legIndex));
        break;
      case "RAMP":
        towardColour(delta, colour, op, values, legIndex, px, py, k,
          rampColour(op, values, legIndex,
            axisPosition(op, values, legIndex, skin, part, point, seedBase)));
        break;
      case "PALETTE":
        towardColour(delta, colour, op, values, legIndex, px, py, k,
          paletteColour(op, values, legIndex, point, seedBase));
        break;
      case "INVERT": {
        // Against what the texel LOOKS like - see SpecPainter.
        var amt = get(values, op.amount, 100, legIndex) / 100 * k;
        delta.add(px, py,
          toward(colour, px, py, 0, 255 - colour.visible(px, py, 0), amt),
          toward(colour, px, py, 1, 255 - colour.visible(px, py, 1), amt),
          toward(colour, px, py, 2, 255 - colour.visible(px, py, 2), amt));
        var wantInv = percentToChannel(get(values, op.opacity, 100, legIndex));
        delta.addOpacity(px, py, Math.round((wantInv - colour.opacityAt(px, py)) * k));
        break;
      }
      case "FLAT": {
        var c = solidColour(op, values, legIndex);
        var wantOpacity = percentToChannel(get(values, op.opacity, 100, legIndex));
        delta.set(px, py,
          Math.round(lerp(colour.opacityAt(px, py), wantOpacity, k)),
          Math.round(lerp(colour.redAt(px, py), c[0], k)),
          Math.round(lerp(colour.greenAt(px, py), c[1], k)),
          Math.round(lerp(colour.blueAt(px, py), c[2], k)));
        break;
      }
    }
  }

  // ---- the combination table -------------------------------------------
  //
  // Mirrors GeneSpecParser.readExpressions / SpecGene.expressionOf: an
  // expression claims the combinations its "when" names, and at most one
  // expression may omit "when" and take whatever is left.

  /** Every unordered combination, canonical (earlier-declared allele first). */
  function combinations(spec) {
    var out = [];
    var a = spec.alleles || [];
    for (var i = 0; i < a.length; i++) {
      for (var j = i; j < a.length; j++) out.push(a[i].token + "/" + a[j].token);
    }
    return out;
  }

  /** The combination a horse with `dose` copies of the first-declared allele holds. */
  function combinationForDose(spec, dose) {
    var a = spec.alleles || [];
    if (!a.length) return "";
    var variant = a[0].token;
    var baseline = a[a.length - 1].token;
    if (dose >= 2) return variant + "/" + variant;
    if (dose === 1) return variant + "/" + baseline;
    return baseline + "/" + baseline;
  }

  function claims(spec, expression) {
    var when = expression.when;
    if (when === undefined || when === null) return null;   // the catch-all
    if (Array.isArray(when)) {
      var all = combinations(spec);
      return when.map(function (c) {
        if (all.indexOf(c) >= 0) return c;
        var parts = String(c).split("/");
        return parts.length === 2 ? parts[1] + "/" + parts[0] : c;
      });
    }
    return combinations(spec).filter(function (c) {
      var t = c.split("/");
      return Object.keys(when).every(function (token) {
        return (t[0] === token ? 1 : 0) + (t[1] === token ? 1 : 0) === Number(when[token]);
      });
    });
  }

  /** Which outcome `combination` produces. Never null for a well-formed spec. */
  function expressionFor(spec, combination) {
    var list = spec.expressions || [];
    var fallback = null;
    for (var i = 0; i < list.length; i++) {
      var owned = claims(spec, list[i]);
      if (owned === null) fallback = list[i];
      else if (owned.indexOf(combination) >= 0) return list[i];
    }
    return fallback;
  }

  /** {@link #expressionFor} for a copy count of the first-declared allele. */
  function expressionForDose(spec, dose) {
    return expressionFor(spec, combinationForDose(spec, dose));
  }

  // ---- the two hooks ---------------------------------------------------

  function restrict(spec, layers, values, skin, coat) {
    var field = coat.mutableCopy();
    (layers || []).forEach(function (layer, i) {
      var asRead = field.mutableCopy();
      var seed = layerSeed(spec, i);
      geo.forEachTexel(skin, function (px, py, part, face, point) {
        var leg = geo.legIndex(part);
        var k = coverage(layer, values, skin, part, face, point, asRead, null, px, py, leg, seed);
        if (k > 0) applyPigment(layer.op, values, field, px, py, leg, k);
      });
    });
    return field;
  }

  function tint(spec, layers, values, skin, coat, colour) {
    var delta = new HG.fields.ColorField(geo.SHEET_SIZE);
    (layers || []).forEach(function (layer, i) {
      var seed = layerSeed(spec, i);
      geo.forEachTexel(skin, function (px, py, part, face, point) {
        var leg = geo.legIndex(part);
        var k = coverage(layer, values, skin, part, face, point, coat, colour, px, py, leg, seed);
        if (k > 0) applyColour(layer.op, values, delta, colour, skin, part, point, px, py, leg, k, seed);
      });
    });
    return delta;
  }

  /** Coverage of one layer at every texel - what the "coverage" overlay draws. */
  function coverageMap(spec, layers, layerIndex, values, skin, coat, colour) {
    var layer = layers[layerIndex];
    var seed = layerSeed(spec, layerIndex);
    var out = new Float32Array(geo.SHEET_SIZE * geo.SHEET_SIZE);
    geo.forEachTexel(skin, function (px, py, part, face, point) {
      out[py * geo.SHEET_SIZE + px] =
        coverage(layer, values, skin, part, face, point, coat, colour || null, px, py,
          geo.legIndex(part), seed);
    });
    return out;
  }

  HG.specEngine = {
    drawValues: drawValues,
    combinations: combinations,
    combinationForDose: combinationForDose,
    expressionFor: expressionFor,
    expressionForDose: expressionForDose,
    restrict: restrict,
    tint: tint,
    coverageMap: coverageMap,
    JavaRandom: JavaRandom,
    // For the PATH drawing canvas. It draws the curve the painter walks and
    // resolves a knob-driven width the way a mask parameter resolves, rather
    // than keeping a second copy of either - a canvas that bends the line
    // differently from the game is worse than no canvas.
    pathSpline: spline,
    resolveValue: get
  };
})(window.HG);
