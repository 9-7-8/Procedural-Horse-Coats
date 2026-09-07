// The coats your new gene will be seen ON.
//
// A marking gene looks completely different over a black horse, a bay, a
// palomino and a grey, and "does it read on a cremello?" is the question the
// creator exists to answer before you ship the gene. So these are ports of the
// mod's real natural genes and their real constants - ExtensionGene,
// AgoutiGene/BayCoat, MatpGene, ChampagneGene, GreyCoat, and KitGene's
// dominant-white outcome - not hand-picked colours that merely look similar.
//
// They are ports, so they drift if the Java changes. What they are NOT is a
// second implementation the game could ever use: the game runs the Java.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var geo = HG.geometry;
  var noise = HG.noise;

  function forEachTexel(skin, fn) { geo.forEachTexel(skin, fn); }

  function restrictAll(skin, f, fn) {
    forEachTexel(skin, function (px, py, part, face, point) { fn(f, px, py, point, part); });
  }

  function forPart(skin, f, part, fn) {
    if (!geo.hasPart(skin, part)) return;
    forEachTexel(skin, function (px, py, p, face, point) {
      if (p === part) fn(f, px, py, point);
    });
  }

  function blackenPart(skin, f, part) {
    forPart(skin, f, part, function (field, px, py) {
      field.setBlack(px, py, 1);
      field.setRed(px, py, 0);
    });
  }

  function lerp(a, b, t) { return a + (b - a) * t; }
  function clamp01(v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

  // ---- BayCoat ---------------------------------------------------------

  // The whole bay range off one number - BayShade.spread(), 0 for a blood bay
  // and 1 for a seal brown. Constants verbatim from BayCoat.
  var BODY_BLACK_LIGHT = 0.10, BODY_BLACK_DARK = 0.82, BODY_CURVE = 1.7;
  var HOOF_FRACTION = 0.12;
  var SOLID_PORTION = 0.3;
  var LEG_MIN = 0.15, LEG_RANGE = 0.80;
  var FACE_MIN = 0.04, FACE_RANGE = 0.62;
  var SOFT_START = 0.62, SOFT_FULL = 0.95, SOFT_BLACK = 0.28;
  var SOFT_EYE_REACH = 2.6, MUZZLE_BACK = 0.45;

  function smooth01(t) { t = clamp01(t); return t * t * (3 - 2 * t); }

  function bodyBlack(spread) {
    return BODY_BLACK_LIGHT
      + (BODY_BLACK_DARK - BODY_BLACK_LIGHT) * Math.pow(clamp01(spread), BODY_CURVE);
  }
  function legHeight(spread) { return LEG_MIN + clamp01(spread) * LEG_RANGE; }
  function faceHeight(spread) { var t = clamp01(spread); return FACE_MIN + t * t * FACE_RANGE; }
  function softPoints(spread) {
    return smooth01((clamp01(spread) - SOFT_START) / (SOFT_FULL - SOFT_START));
  }

  function fade(t, solid, band) {
    if (t <= solid) return 1;
    if (t >= band) return 0;
    var u = (t - solid) / (band - solid);
    return 1 - u * u * (3 - 2 * u);
  }

  function blob(fx, fy, cx, cy, rx, ry) {
    var dx = (fx - cx) / rx, dy = (fy - cy) / ry;
    return 1 - smooth01(Math.sqrt(dx * dx + dy * dy));
  }

  /** BayCoat.softWeight - the muzzle, the ring over the eye, elbow and stifle. */
  function softWeight(skin, part, point) {
    if (part === "MUZZLE") {
      var m = geo.bounds(skin, part);
      if (!m) return 0;
      var span = m.span("X");
      var fromNose = span <= 0 ? 0 : (m.xMax - point.x) / span;
      return MUZZLE_BACK + (1 - MUZZLE_BACK) * (1 - clamp01(fromNose));
    }
    if (part === "HEAD") {
      var h = geo.bounds(skin, part);
      if (!h) return 0;
      var dx = point.x - (h.xMax - h.span("X") * 0.30);
      var dy = point.y - (h.yMax - h.span("Y") * 0.22);
      var r = Math.sqrt(dx * dx + dy * dy + point.z * point.z);
      return 1 - smooth01(r / SOFT_EYE_REACH);
    }
    if (part === "BODY") {
      var b = geo.bounds(skin, part);
      if (!b) return 0;
      var fx = (point.x - b.xMin) / b.span("X");
      var fy = (point.y - b.yMin) / b.span("Y");
      return Math.max(blob(fx, fy, 0.80, 0.08, 0.28, 0.48), blob(fx, fy, 0.20, 0.10, 0.30, 0.46));
    }
    return 0;
  }

  function bay(skin, f, spread) {
    var body = bodyBlack(spread);
    restrictAll(skin, f, function (field, px, py) { field.setBlack(px, py, body); });
    ["MANE", "TAIL", "LEFT_EAR", "RIGHT_EAR"].forEach(function (p) { blackenPart(skin, f, p); });

    geo.LEGS.forEach(function (leg) {
      var b = geo.bounds(skin, leg);
      if (!b) return;
      var solid = Math.max(HOOF_FRACTION, legHeight(spread) * SOLID_PORTION);
      var band = Math.max(legHeight(spread), solid);
      forPart(skin, f, leg, function (field, px, py, point) {
        var k = fade((point.y - b.yMin) / b.span("Y"), solid, band);
        if (k > 0) {
          field.setBlack(px, py, lerp(field.blackAt(px, py), 1, k));
          field.setRed(px, py, lerp(field.redAt(px, py), 0, k));
        }
      });
    });

    blackenPart(skin, f, "MUZZLE");
    var head = geo.bounds(skin, "HEAD");
    if (head) {
      var band = faceHeight(spread);
      var solidFace = band * SOLID_PORTION;
      forPart(skin, f, "HEAD", function (field, px, py, point) {
        var k = fade((head.xMax - point.x) / head.span("X"), solidFace, band);
        if (k > 0) {
          field.setBlack(px, py, lerp(field.blackAt(px, py), 1, k));
          field.setRed(px, py, lerp(field.redAt(px, py), 0, k));
        }
      });
    }

    var strength = softPoints(spread);
    if (strength > 0) {
      restrictAll(skin, f, function (field, px, py, point, part) {
        var k = strength * softWeight(skin, part, point);
        if (k > 0) {
          field.setBlack(px, py, lerp(field.blackAt(px, py), SOFT_BLACK, k));
          field.setRed(px, py, lerp(field.redAt(px, py), 1, k));
        }
      });
    }
  }

  // ---- GreyCoat --------------------------------------------------------

  var KEEP_YOUNG = 0.46, KEEP_OLD = 0.10, RED_YOUNG = 0.22, RED_OLD = 0.02, DAPPLE_DEPTH = 0.42;

  function pointWeight(skin, part, point) {
    switch (part) {
      case "MANE": case "TAIL": case "LEFT_EAR": case "RIGHT_EAR": case "MUZZLE": return 1;
      case "HEAD": return 0.5;
      default:
        if (geo.LEGS.indexOf(part) < 0) return 0;
        var b = geo.bounds(skin, part);
        return 1 - noise.smoothstep(0.05, 0.55, (point.y - b.yMin) / b.span("Y"));
    }
  }

  function grey(skin, f, seed, progress, spacing, dappleStrength, pointRetention) {
    var p = clamp01(progress);
    var keepWeb = lerp(KEEP_YOUNG, KEEP_OLD, p);
    var redKeep = lerp(RED_YOUNG, RED_OLD, p);
    var contrast = clamp01(dappleStrength * (1 - Math.abs(p - 0.5) * 1.4));
    var keepDapple = keepWeb * (1 - DAPPLE_DEPTH * contrast);
    var pointBoost = pointRetention * (1 - p) * 0.9;
    var warpScale = 1 / (spacing * 3), dappleScale = 1 / spacing, warp = spacing * 0.45;

    forEachTexel(skin, function (px, py, part, face, point) {
      var n = noise.value(noise.xor(seed, noise.u64(0, 0x51)),
        point.x * warpScale, point.y * warpScale, point.z * warpScale);
      var m = noise.value(noise.xor(seed, noise.u64(0, 0x52)),
        point.z * warpScale, point.x * warpScale, point.y * warpScale);
      var d = noise.cellDistance(seed,
        (point.x + (n - 0.5) * warp) * dappleScale,
        (point.y + (m - 0.5) * warp) * dappleScale,
        (point.z + (n - m) * warp) * dappleScale);
      var web = noise.smoothstep(0.35, 0.78, d);
      var keep = lerp(keepDapple, keepWeb, web);
      var boost = pointBoost * pointWeight(skin, part, point);
      if (boost > 0) keep = Math.min(1, keep * (1 + boost));

      var red = f.redAt(px, py), black = f.blackAt(px, py);
      var darkness = clamp01(0.55 * red + 0.95 * black);
      f.setBlack(px, py, darkness * keep);
      f.setRed(px, py, red * redKeep * keep);
    });
  }
  // ---- the loci, as a config ------------------------------------------

  // keepRed, keepBlack, blackTint - CreamPearlDilution's four modes plus
  // champagne, verbatim.
  var DILUTIONS = {
    singleCream: [0.45, 0.62, 0.30],
    doublePearl: [0.55, 0.52, 0.28],
    doubleDilute: [0.08, 0.38, 0.33],
    champagne: [0.55, 0.42, 0.30]
  };

  function dilute(skin, f, mode) {
    var d = DILUTIONS[mode];
    if (!d) return;
    restrictAll(skin, f, function (field, px, py) { field.dilute(px, py, d[0], d[1], d[2]); });
  }

  function chestnut(skin, f) {
    restrictAll(skin, f, function (field, px, py) { field.setBlack(px, py, 0); });
  }

  function white(skin, f) {
    restrictAll(skin, f, function (field, px, py) {
      field.setRed(px, py, 0);
      field.setBlack(px, py, 0);
    });
  }

  var GREY_SEED = "00000000000001a3";

  var GREY_DEFAULTS = {
    seed: GREY_SEED, progress: 0.5, spacing: 3.4, dappleStrength: 1.0, pointRetention: 0.6
  };

  /**
   * Run a base coat described as a genotype rather than as a named preset:
   *
   *   { extension: "wild" | "chestnut",
   *     agouti:    null | { spread },              // BayShade.spread(): 0 blood bay .. 1 seal brown
   *     dilution:  null | one of DILUTIONS,        // MATP / champagne
   *     grey:      null | { seed, progress, spacing, dappleStrength, pointRetention },
   *     white:     false | true }                  // KIT's dominant-white outcome
   *
   * The order is Genes.naturalOrder(): extension, agouti, the dilutions, grey,
   * then the white loci. It matters - agouti's points are set absolutely and
   * the dilutions scale them, so swapping those two gives a horse with jet
   * black points on a gold body, which is the bug the `blackTint` term exists
   * to prevent.
   *
   * This is the one implementation. PRESETS below are configs, and the horse
   * designer's per-locus controls build the same object, so the creator's "Bay"
   * and the designer's "Bay" cannot drift apart.
   */
  function compose(skin, f, config) {
    var c = config || {};
    if (c.extension === "chestnut") chestnut(skin, f);
    if (c.agouti) bay(skin, f, num(c.agouti.spread, BAY.spread));
    if (c.dilution) dilute(skin, f, c.dilution);
    if (c.grey) {
      var g = c.grey;
      grey(skin, f,
        noise.fromHex(g.seed || GREY_DEFAULTS.seed),
        num(g.progress, GREY_DEFAULTS.progress),
        num(g.spacing, GREY_DEFAULTS.spacing),
        num(g.dappleStrength, GREY_DEFAULTS.dappleStrength),
        num(g.pointRetention, GREY_DEFAULTS.pointRetention));
    }
    if (c.white) white(skin, f);
  }

  function num(v, fallback) { return typeof v === "number" && isFinite(v) ? v : fallback; }

  // ---- the presets -----------------------------------------------------

  // The population's middle - Sh/Sh at ordinary MC1R / ASIP dosage.
  var BAY = { spread: 0.47 };

  /**
   * Each preset is a config run before the gene under test - the same position
   * ordinary genes occupy in Genes.naturalOrder(). `build(skin, field)` is kept
   * as the calling convention so nothing downstream had to change.
   */
  var PRESET_CONFIGS = [
    { id: "black", label: "Black", config: {} },
    { id: "chestnut", label: "Chestnut", config: { extension: "chestnut" } },
    { id: "bay", label: "Bay", config: { agouti: BAY } },
    { id: "blood_bay", label: "Blood bay", config: { agouti: { spread: 0.10 } } },
    { id: "liver_bay", label: "Liver bay", config: { agouti: { spread: 0.72 } } },
    { id: "seal", label: "Seal brown", config: { agouti: { spread: 0.93 } } },
    { id: "buckskin", label: "Buckskin", config: { agouti: BAY, dilution: "singleCream" } },
    { id: "palomino", label: "Palomino", config: { extension: "chestnut", dilution: "singleCream" } },
    { id: "perlino", label: "Perlino", config: { agouti: BAY, dilution: "doubleDilute" } },
    { id: "cremello", label: "Cremello", config: { extension: "chestnut", dilution: "doubleDilute" } },
    { id: "pearl_bay", label: "Pearl bay", config: { agouti: BAY, dilution: "doublePearl" } },
    { id: "champagne_bay", label: "Amber champagne", config: { agouti: BAY, dilution: "champagne" } },
    {
      id: "grey_steel", label: "Grey, steel",
      config: { grey: { seed: GREY_SEED, progress: 0.15, spacing: 3.4, dappleStrength: 0.8, pointRetention: 0.5 } }
    },
    {
      id: "grey_dapple", label: "Grey, dappled",
      config: { grey: { seed: GREY_SEED, progress: 0.5, spacing: 3.4, dappleStrength: 1.0, pointRetention: 0.6 } }
    },
    {
      id: "grey_old", label: "Grey, near-white",
      config: { grey: { seed: GREY_SEED, progress: 0.88, spacing: 3.4, dappleStrength: 0.7, pointRetention: 0.2 } }
    },
    { id: "white", label: "Dominant white", config: { white: true } }
  ];

  var PRESETS = PRESET_CONFIGS.map(function (p) {
    return {
      id: p.id,
      label: p.label,
      config: p.config,
      build: function (skin, field) { compose(skin, field, p.config); }
    };
  });

  HG.baseCoats = {
    presets: PRESETS,
    byId: function (id) {
      for (var i = 0; i < PRESETS.length; i++) if (PRESETS[i].id === id) return PRESETS[i];
      return PRESETS[0];
    },
    // The loci, for a caller that wants to drive them one at a time.
    compose: compose,
    DILUTIONS: DILUTIONS,
    GREY_DEFAULTS: GREY_DEFAULTS,
    loci: { chestnut: chestnut, bay: bay, dilute: dilute, grey: grey, white: white }
  };
})(window.HG);
