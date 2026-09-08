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

  function JavaRandom(seedHigh, seedLow) {
    var s = noise.xor(noise.u64(seedHigh, seedLow), MULT);
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

  function maskCoverage(mask, values, skin, part, point, coat, px, py, legIndex, seedBase) {
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
      case "PIGMENT":
        return pigmentCoverage(mask, values, coat, px, py, legIndex);
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
        if ((mask.shape || "round") === "heart") rad4 *= heartRadius(Math.atan2(c4.dy, c4.dx));
        var soft4 = Math.max(1e-6, get(values, mask.softness, 0.25, legIndex));
        return 1 - smoothstep(rad4, rad4 + soft4, c4.distance * spacing4);
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
        var ring = 1 - smoothstep(half5, half5 + soft5, Math.abs(c5.distance * spacing5 - rad5));
        var arc = clamp01(get(values, mask.arc, 1.0, legIndex));
        if (arc >= 1) return ring;
        var theta5 = Math.atan2(c5.dy, c5.dz) / (2 * Math.PI) + 0.5;
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
        return smoothstep(halfC, halfC + softC, noise.cellEdge(sc, wx, wy, wz) * scaleC);
      }
      default:
        return 0;
    }
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
  function spreadMask(mask, values, skin, coat, part, point, px, py, legIndex, c) {
    if (mask.type !== "PIGMENT" || c >= 1) return c;
    var parts = HG.schema.expandParts(mask.parts);
    if (parts.length && parts.indexOf(part) < 0) return c;
    var radius = get(values, mask.spread, 0, legIndex);
    if (radius <= 0) return c;
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
        var n = pigmentCoverage(mask, values, coat, qx, qy, legIndex);
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
  function coverage(layer, values, skin, part, point, coat, px, py, legIndex, fallbackSeed) {
    var acc = 1;
    var masks = layer.masks || [];
    for (var i = 0; i < masks.length; i++) {
      var mask = masks[i];
      var c = maskCoverage(mask, values, skin, part, point, coat, px, py, legIndex,
        noise.xor(fallbackSeed, noise.mul(noise.fromInt(i), noise.K1)));
      if (mask.invert) c = 1 - c;
      c = spreadMask(mask, values, skin, coat, part, point, px, py, legIndex, c);
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

  function axisPosition(op, values, legIndex, skin, part, point) {
    var axis = (op.axis || "X").toUpperCase();
    var coord = axis === "X" ? point.x : axis === "Y" ? point.y : point.z;
    var space = op.space || "part";
    var t = space === "body" ? normalise(coord, geo.bodyBounds(skin), axis)
      : space === "units" ? coord
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
          rampColour(op, values, legIndex, axisPosition(op, values, legIndex, skin, part, point)));
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
        var k = coverage(layer, values, skin, part, point, asRead, px, py, leg, seed);
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
        var k = coverage(layer, values, skin, part, point, coat, px, py, leg, seed);
        if (k > 0) applyColour(layer.op, values, delta, colour, skin, part, point, px, py, leg, k, seed);
      });
    });
    return delta;
  }

  /** Coverage of one layer at every texel - what the "coverage" overlay draws. */
  function coverageMap(spec, layers, layerIndex, values, skin, coat) {
    var layer = layers[layerIndex];
    var seed = layerSeed(spec, layerIndex);
    var out = new Float32Array(geo.SHEET_SIZE * geo.SHEET_SIZE);
    geo.forEachTexel(skin, function (px, py, part, face, point) {
      out[py * geo.SHEET_SIZE + px] =
        coverage(layer, values, skin, part, point, coat, px, py, geo.legIndex(part), seed);
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
    JavaRandom: JavaRandom
  };
})(window.HG);
