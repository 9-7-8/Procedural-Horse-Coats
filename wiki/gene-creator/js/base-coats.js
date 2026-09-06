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

  var BODY_BLACK = 0.32;
  var HOOF_FRACTION = 0.12;
  var SOLID_PORTION = 0.3;

  function fade(t, solid, band) {
    if (t <= solid) return 1;
    if (t >= band) return 0;
    var u = (t - solid) / (band - solid);
    return 1 - u * u * (3 - 2 * u);
  }

  function bay(skin, f, legHeight, faceHeight) {
    restrictAll(skin, f, function (field, px, py) { field.setBlack(px, py, BODY_BLACK); });
    ["MANE", "TAIL", "LEFT_EAR", "RIGHT_EAR"].forEach(function (p) { blackenPart(skin, f, p); });

    var band = Math.max(HOOF_FRACTION / SOLID_PORTION, legHeight);
    geo.LEGS.forEach(function (leg) {
      var b = geo.bounds(skin, leg);
      if (!b) return;
      var solid = band * SOLID_PORTION;
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
      var solidFace = faceHeight * SOLID_PORTION;
      forPart(skin, f, "HEAD", function (field, px, py, point) {
        var k = fade((head.xMax - point.x) / head.span("X"), solidFace, faceHeight);
        if (k > 0) {
          field.setBlack(px, py, lerp(field.blackAt(px, py), 1, k));
          field.setRed(px, py, lerp(field.redAt(px, py), 0, k));
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
   *     agouti:    null | { leg, face },          // BayCoat's two point extents
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
    if (c.agouti) bay(skin, f, c.agouti.leg, c.agouti.face);
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

  var BAY = { leg: 0.45, face: 0.22 };

  /**
   * Each preset is a config run before the gene under test - the same position
   * ordinary genes occupy in Genes.naturalOrder(). `build(skin, field)` is kept
   * as the calling convention so nothing downstream had to change.
   */
  var PRESET_CONFIGS = [
    { id: "black", label: "Black", config: {} },
    { id: "chestnut", label: "Chestnut", config: { extension: "chestnut" } },
    { id: "bay", label: "Bay", config: { agouti: BAY } },
    { id: "bay_low", label: "Bay, low points", config: { agouti: { leg: 0.2, face: 0.05 } } },
    { id: "seal", label: "Seal brown", config: { agouti: { leg: 0.92, face: 0.6 } } },
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
