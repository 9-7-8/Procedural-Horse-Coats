// A mirror of common/genetics/spec/SpecSchema.java.
//
// One table, three jobs: the game validates gene files against it, this file
// builds the creator's parameter forms from it, and the docs quote it. If they
// disagree, the tool offers settings the game ignores - so when you add a mask
// or an op, add it in BOTH files and in SpecPainter/spec-engine.
//
// `ui` fields are creator-only (slider ranges, grouping); everything else is
// the format.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // `fallback` MUST equal the Java SpecSchema's fallback: it is the value the
  // game uses when the key is absent, and tidy() drops any setting that equals
  // it. `initial` is a creator-only nicety - what a freshly added layer starts
  // at, so a new AXIS mask is a sock rather than the whole leg. Getting these
  // two confused writes a file that previews differently from how it plays;
  // tools/check-parity.mjs compares every fallback against the Java table.
  function v(name, fallback, doc, ui, initial) {
    return {
      name: name, kind: "VALUE", fallback: fallback,
      initial: initial === undefined ? fallback : initial,
      doc: doc, ui: ui || { min: 0, max: 1, step: 0.01 }
    };
  }
  function parts(name, doc) { return { name: name, kind: "PARTS", doc: doc }; }
  function choice(name, choices, doc) { return { name: name, kind: "CHOICE", choices: choices, fallback: choices[0], doc: doc }; }
  function color(name, doc) { return { name: name, kind: "COLOR", fallback: "#ffffff", doc: doc }; }

  var PART_NAMES = HG.geometry.PARTS;
  var GROUP_NAMES = ["ALL", "LEGS", "FRONT_LEGS", "HIND_LEGS", "EARS", "HAIR", "FACE", "POINTS", "BARREL"];

  var GROUPS = {
    ALL: PART_NAMES.slice(),
    LEGS: ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"],
    FRONT_LEGS: ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG"],
    HIND_LEGS: ["LEFT_HIND_LEG", "RIGHT_HIND_LEG"],
    EARS: ["LEFT_EAR", "RIGHT_EAR"],
    HAIR: ["MANE", "TAIL"],
    FACE: ["HEAD", "MUZZLE"],
    POINTS: ["MANE", "TAIL", "LEFT_EAR", "RIGHT_EAR", "MUZZLE",
      "LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"],
    BARREL: ["BODY", "NECK"]
  };

  function expandParts(names) {
    var out = [];
    (names || []).forEach(function (n) {
      var g = GROUPS[n];
      (g || [n]).forEach(function (p) { if (out.indexOf(p) < 0) out.push(p); });
    });
    return out;
  }

  var MASKS = {
    ALL: {
      blurb: "Everywhere this horse has skin. The starting point.",
      params: []
    },
    PARTS: {
      blurb: "Named body parts - a mane stripe, black ears, white socks on the front legs only.",
      params: [parts("parts", "the body parts this layer touches")]
    },
    AXIS: {
      blurb: "A soft band along the horse. Y for socks and belly, X for face and rump, Z for sides.",
      params: [
        parts("parts", "restrict to these parts (and, in 'part' space, measure inside each one)"),
        choice("axis", ["Y", "X", "Z"], "X tail-to-nose, Y hoof-to-withers, Z centre-to-right"),
        choice("space", ["part", "body", "units"],
          "'part' normalises inside each part - 'to: 0.4' is the lower 40% of EVERY leg"),
        v("from", 0.0, "start of the solid band"),
        v("to", 1.0, "end of the solid band", { min: 0, max: 1, step: 0.01 }, 0.4),
        v("softness", 0.15, "fade width outside the band")
      ]
    },
    CENTERLINE: {
      blurb: "A stripe down the middle - a blaze on the face, a dorsal stripe if you add a height band.",
      params: [
        parts("parts", "restrict to these parts - a blaze is FACE"),
        v("halfWidth", 1.0, "body units either side of centre", { min: 0, max: 6, step: 0.05 }),
        v("softness", 0.35, "edge fade, body units", { min: 0, max: 3, step: 0.05 }),
        v("offset", 0.0, "shift off centre, body units", { min: -6, max: 6, step: 0.05 })
      ]
    },
    STRIPES: {
      blurb: "Bands wrapping across the horse - zebra bars, dun leg barring, brindle.",
      params: [
        parts("parts", "restrict to these parts"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("spacing", 3.0, "centre to centre, body units (the barrel is 22 long)", { min: 0.4, max: 12, step: 0.1 }),
        v("duty", 0.45, "how much of each period is stripe"),
        v("warp", 1.0, "how far noise may bend a stripe, body units", { min: 0, max: 6, step: 0.05 })
      ]
    },
    DAPPLES: {
      blurb: "Rounded cells with a web between them - dapples, rosettes, appaloosa spots.",
      params: [
        parts("parts", "restrict to these parts"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("spacing", 3.5, "body units between centres", { min: 0.5, max: 12, step: 0.1 }),
        v("warp", 0.45, "how far the lattice flows off the grid"),
        v("edge0", 0.35, "where a dapple centre ends"),
        v("edge1", 0.78, "where the web begins")
      ]
    },
    PATCHES: {
      blurb: "Big irregular blobs - pinto, tobiano, roan patching.",
      params: [
        parts("parts", "restrict to these parts"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("scale", 6.0, "body units across a typical patch", { min: 0.5, max: 20, step: 0.1 }),
        v("threshold", 0.5, "lower covers more of the horse"),
        v("softness", 0.12, "patch edge softness")
      ]
    },
    NOISE: {
      blurb: "Smooth shading rather than a shape - sooty, countershading, a mottled overlay.",
      params: [
        parts("parts", "restrict to these parts"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("scale", 8.0, "body units per feature", { min: 0.5, max: 24, step: 0.1 }),
        v("low", 0.0, "coverage the darkest noise maps to"),
        v("high", 1.0, "coverage the brightest maps to")
      ]
    },
    PIGMENT: {
      blurb: "Wherever the coat underneath is already dark (or already red) - find the black points, then paint them.",
      params: [
        parts("parts", "restrict to these parts"),
        choice("channel", ["darkness", "red", "black", "total"],
          "'darkness' is 0.55*red + 0.95*black, the reading grey uses"),
        v("from", 0.5, "reading where coverage starts"),
        v("to", 1.0, "reading where coverage reaches 1")
      ]
    }
  };

  var OPS = {
    DILUTE: {
      phase: "natural",
      blurb: "Wash pigment out - cream, champagne, pearl, silver. The dilution move.",
      params: [
        v("keepRed", 1.0, "share of red pigment kept"),
        v("keepBlack", 1.0, "share of black pigment kept", null, 0.5),
        v("blackTint", 0.0,
          "removed black fed back as red - without it a diluted black point stays a void", null, 0.2)
      ]
    },
    RESTRICT: {
      phase: "natural",
      blurb: "Take a share of one pigment away, leaving the other alone.",
      params: [v("red", 0.0, "share of red removed"), v("black", 0.0, "share of black removed")]
    },
    SET_PIGMENT: {
      phase: "natural",
      blurb: "Drive pigment to a level: (0, 0) is a white marking, (0, 1) is a black point.",
      params: [v("red", 0.0, "red level to move toward"), v("black", 0.0, "black level to move toward")]
    },
    TINT: {
      phase: "magical",
      blurb: "Add signed colour. Percentages past 100 (or under -100) commit hard enough that no other gene can pull the horse back.",
      params: [
        v("red", 0, "percent added to red", { min: -300, max: 300, step: 1 }),
        v("green", 0, "percent added to green", { min: -300, max: 300, step: 1 }),
        v("blue", 0, "percent added to blue", { min: -300, max: 300, step: 1 }),
        v("opacity", 100, "percent opacity added, so it shows on a white horse", { min: -300, max: 300, step: 1 })
      ]
    },
    TOWARD: {
      phase: "magical",
      blurb: "Walk the texel toward a colour, reading what it already looks like - so it lands the same on a black mane and a cremello one.",
      params: [
        color("color", "the colour to walk toward"),
        v("strength", 100, "percent of the way there", { min: 0, max: 100, step: 1 }, 82),
        v("opacity", 100, "percent opacity the texel ends at", { min: 0, max: 100, step: 1 })
      ]
    },
    FLAT: {
      phase: "magical",
      blurb: "Flat paint that replaces everything under it. For a gene that must look identical on any base.",
      params: [
        color("color", "flat paint"),
        v("opacity", 100, "percent opacity", { min: 0, max: 100, step: 1 })
      ]
    }
  };

  HG.schema = {
    MASKS: MASKS,
    OPS: OPS,
    PART_NAMES: PART_NAMES,
    GROUP_NAMES: GROUP_NAMES,
    GROUPS: GROUPS,
    expandParts: expandParts,
    COMBINES: ["MULTIPLY", "MAX", "MIN", "ADD", "SUBTRACT"],
    DOMINANCE: ["DOMINANT", "RECESSIVE", "INCOMPLETE_DOMINANT", "COMPLETE_DOMINANT"],

    maskParam: function (type, name) {
      var ps = MASKS[type].params;
      for (var i = 0; i < ps.length; i++) if (ps[i].name === name) return ps[i];
      return null;
    },
    opParam: function (type, name) {
      var ps = OPS[type].params;
      for (var i = 0; i < ps.length; i++) if (ps[i].name === name) return ps[i];
      return null;
    },
    opsForPhase: function (phase) {
      return Object.keys(OPS).filter(function (k) { return OPS[k].phase === phase; });
    }
  };
})(window.HG);
