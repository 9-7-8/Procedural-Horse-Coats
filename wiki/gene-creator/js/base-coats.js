// The coats your new gene will be seen ON.
//
// A marking gene looks completely different over a black horse, a bay, a
// palomino and a grey, and "does it read on a cremello?" is the question the
// creator exists to answer before you ship the gene. So these are ports of the
// mod's real natural genes and their real constants - ExtensionGene,
// AgoutiGene/BayCoat, CreamPearlDilution, ChampagneGene, GreyCoat, WhiteGene -
// not hand-picked colours that merely look similar.
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

  // ---- the presets -----------------------------------------------------

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
    restrictAll(skin, f, function (field, px, py) { field.dilute(px, py, d[0], d[1], d[2]); });
  }

  function chestnut(skin, f) {
    restrictAll(skin, f, function (field, px, py) { field.setBlack(px, py, 0); });
  }

  /**
   * Each preset is a function(skin, pigmentField) run before the gene under
   * test - the same position ordinary genes occupy in Genes.naturalOrder().
   */
  var PRESETS = [
    { id: "black", label: "Black", build: function () {} },
    { id: "chestnut", label: "Chestnut", build: chestnut },
    { id: "bay", label: "Bay", build: function (s, f) { bay(s, f, 0.45, 0.22); } },
    { id: "bay_low", label: "Bay, low points", build: function (s, f) { bay(s, f, 0.2, 0.05); } },
    { id: "seal", label: "Seal brown", build: function (s, f) { bay(s, f, 0.92, 0.6); } },
    {
      id: "buckskin", label: "Buckskin",
      build: function (s, f) { bay(s, f, 0.45, 0.22); dilute(s, f, "singleCream"); }
    },
    {
      id: "palomino", label: "Palomino",
      build: function (s, f) { chestnut(s, f); dilute(s, f, "singleCream"); }
    },
    {
      id: "perlino", label: "Perlino",
      build: function (s, f) { bay(s, f, 0.45, 0.22); dilute(s, f, "doubleDilute"); }
    },
    {
      id: "cremello", label: "Cremello",
      build: function (s, f) { chestnut(s, f); dilute(s, f, "doubleDilute"); }
    },
    {
      id: "pearl_bay", label: "Pearl bay",
      build: function (s, f) { bay(s, f, 0.45, 0.22); dilute(s, f, "doublePearl"); }
    },
    {
      id: "champagne_bay", label: "Amber champagne",
      build: function (s, f) { bay(s, f, 0.45, 0.22); dilute(s, f, "champagne"); }
    },
    {
      id: "grey_steel", label: "Grey, steel",
      build: function (s, f) { grey(s, f, noise.fromHex("00000000000001a3"), 0.15, 3.4, 0.8, 0.5); }
    },
    {
      id: "grey_dapple", label: "Grey, dappled",
      build: function (s, f) { grey(s, f, noise.fromHex("00000000000001a3"), 0.5, 3.4, 1.0, 0.6); }
    },
    {
      id: "grey_old", label: "Grey, near-white",
      build: function (s, f) { grey(s, f, noise.fromHex("00000000000001a3"), 0.88, 3.4, 0.7, 0.2); }
    },
    {
      id: "white", label: "Dominant white",
      build: function (s, f) {
        restrictAll(s, f, function (field, px, py) { field.setRed(px, py, 0); field.setBlack(px, py, 0); });
      }
    }
  ];

  HG.baseCoats = {
    presets: PRESETS,
    byId: function (id) {
      for (var i = 0; i < PRESETS.length; i++) if (PRESETS[i].id === id) return PRESETS[i];
      return PRESETS[0];
    }
  };
})(window.HG);
