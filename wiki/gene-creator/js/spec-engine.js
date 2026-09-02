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

  /** Draw every knob, in declaration order - the SpecValues contract. */
  function drawValues(spec, seedHigh, seedLow, dose) {
    var rng = new JavaRandom(seedHigh, seedLow);
    var ranges = [], seeds = [];
    (spec.knobs || []).forEach(function (knob, i) {
      if (knob.type === "seed") {
        seeds[i] = rng.nextLong();
        ranges[i] = null;
        return;
      }
      var base = knob.min + rng.nextFloat() * (knob.max - knob.min);
      if (knob.per !== "leg") { ranges[i] = [base]; return; }
      var perLeg = [];
      for (var leg = 0; leg < LEG_COUNT; leg++) {
        perLeg.push(base * (1 - (knob.spread || 0) + rng.nextFloat() * (knob.spread || 0) * 2));
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
      case "PIGMENT": {
        var red = coat.redAt(px, py), black = coat.blackAt(px, py);
        var channel = mask.channel || "darkness";
        var reading = channel === "red" ? red
          : channel === "black" ? black
            : channel === "total" ? (red + black) / 2
              : clamp01(0.55 * red + 0.95 * black);
        return smoothstep(get(values, mask.from, 0.5, legIndex),
          get(values, mask.to, 1, legIndex), reading);
      }
      default:
        return 0;
    }
  }

  function coverage(layer, values, skin, part, point, coat, px, py, legIndex, fallbackSeed) {
    var acc = 1;
    var masks = layer.masks || [];
    for (var i = 0; i < masks.length; i++) {
      var mask = masks[i];
      var c = maskCoverage(mask, values, skin, part, point, coat, px, py, legIndex,
        noise.xor(fallbackSeed, noise.mul(noise.fromInt(i), noise.K1)));
      if (mask.invert) c = 1 - c;
      switch (mask.combine || "MULTIPLY") {
        case "MAX": acc = Math.max(acc, c); break;
        case "MIN": acc = Math.min(acc, c); break;
        case "ADD": acc = clamp01(acc + c); break;
        case "SUBTRACT": acc = clamp01(acc - c); break;
        default: acc = acc * c;
      }
      if (acc <= 0 && (mask.combine || "MULTIPLY") === "MULTIPLY") return 0;
    }
    return clamp01(acc);
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
    }
  }

  function toward(colour, px, py, channel, target, strength) {
    var seen = colour.visible(px, py, channel);
    var wanted = seen + (target - seen) * strength;
    var stored = channel === 0 ? colour.redAt(px, py)
      : channel === 1 ? colour.greenAt(px, py) : colour.blueAt(px, py);
    return Math.round(wanted - stored);
  }

  function applyColour(op, values, delta, colour, px, py, legIndex, k) {
    switch (op.type) {
      case "TINT":
        delta.add(px, py,
          percentToChannel(get(values, op.red, 0, legIndex) * k),
          percentToChannel(get(values, op.green, 0, legIndex) * k),
          percentToChannel(get(values, op.blue, 0, legIndex) * k));
        delta.addOpacity(px, py, percentToChannel(get(values, op.opacity, 100, legIndex) * k));
        break;
      case "TOWARD": {
        var rgb = hexToRgb(op.color);
        var strength = get(values, op.strength, 100, legIndex) / 100 * k;
        delta.add(px, py,
          toward(colour, px, py, 0, rgb[0], strength),
          toward(colour, px, py, 1, rgb[1], strength),
          toward(colour, px, py, 2, rgb[2], strength));
        var want = percentToChannel(get(values, op.opacity, 100, legIndex));
        delta.addOpacity(px, py, Math.round((want - colour.opacityAt(px, py)) * k));
        break;
      }
      case "FLAT": {
        var c = hexToRgb(op.color);
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

  // ---- the two hooks ---------------------------------------------------

  function restrict(spec, values, skin, coat) {
    var field = coat.mutableCopy();
    (spec.layers || []).forEach(function (layer, i) {
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

  function tint(spec, values, skin, coat, colour) {
    var delta = new HG.fields.ColorField(geo.SHEET_SIZE);
    (spec.layers || []).forEach(function (layer, i) {
      var seed = layerSeed(spec, i);
      geo.forEachTexel(skin, function (px, py, part, face, point) {
        var leg = geo.legIndex(part);
        var k = coverage(layer, values, skin, part, point, coat, px, py, leg, seed);
        if (k > 0) applyColour(layer.op, values, delta, colour, px, py, leg, k);
      });
    });
    return delta;
  }

  /** Coverage of one layer at every texel - what the "coverage" overlay draws. */
  function coverageMap(spec, layerIndex, values, skin, coat) {
    var layer = spec.layers[layerIndex];
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
    restrict: restrict,
    tint: tint,
    coverageMap: coverageMap,
    JavaRandom: JavaRandom
  };
})(window.HG);
