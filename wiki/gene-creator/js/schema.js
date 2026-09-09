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
  function flag(name, doc) { return { name: name, kind: "FLAG", fallback: false, doc: doc }; }
  function choice(name, choices, doc) { return { name: name, kind: "CHOICE", choices: choices, fallback: choices[0], doc: doc }; }
  function color(name, doc) { return { name: name, kind: "COLOR", fallback: "#ffffff", doc: doc }; }
  function colors(name, doc) { return { name: name, kind: "COLORS", doc: doc }; }

  // The three that turn any colour op into an epigenetic one. They repeat on
  // four ops, so they are built rather than retyped - a hue that drifts between
  // the ops is the kind of divergence nobody notices until a horse is wrong.
  function hueParams(hueDoc, unset) {
    return [
      v("hue", unset ? -1 : 0, hueDoc, { min: unset ? -1 : 0, max: 360, step: 1 }),
      v("saturation", 0.8, "saturation 0 to 1; read only when 'hue' is set"),
      v("lightness", 0.55, "lightness 0 to 1; read only when 'hue' is set")
    ];
  }
  var HUE_DOC = "hue in degrees - 0 red, 120 green, 240 blue. Below 0 means \"not set\", "
    + "so the layer paints 'color' instead. Point it at a knob to give every horse its own.";

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
        v("to", 1.0, "reading where coverage reaches 1"),
        v("spread", 0.0, "body units to grow the PALE side by - 'only beside white the horse already has'",
          { min: 0, max: 8, step: 0.25 }),
        choice("spreadFrom", ["any", "above", "below", "ahead", "behind"],
          "which side the growth comes from - 'above' grows the selection DOWNWARD, 'below' upward. 'any' is the isotropic disc")
      ]
    },
    LUMA: {
      blurb: "Wherever the coat underneath already LOOKS dark, or white, or red - the colour the gradient chart resolved, not the pigment behind it. Magical genes only.",
      params: [
        parts("parts", "restrict to these parts"),
        choice("channel", ["dark", "light", "white", "saturation", "red", "green", "blue"],
          "'dark' is 1 - luminance, 'white' the achromatic floor - bald white 1, saturated colour ~0"),
        v("from", 0.5, "reading where coverage starts"),
        v("to", 1.0, "reading where coverage reaches 1"),
        v("spread", 0.0, "body units to grow what this mask SELECTED by, exactly as on PIGMENT",
          { min: 0, max: 8, step: 0.25 }),
        choice("spreadFrom", ["any", "above", "below", "ahead", "behind"],
          "which side the growth comes from - 'above' grows the selection DOWNWARD, 'below' upward. 'any' is the isotropic disc")
      ]
    },
    EDGE: {
      blurb: "The rim of each body part's box, on the face the texel is on - a wireframe of the horse. It outlines EVERY box, not the silhouette.",
      params: [
        parts("parts", "restrict to these parts - one rectangle per face of each"),
        v("width", 0.6, "how far in from the boundary the rim runs, body units"),
        v("softness", 0.25, "fade width inside the rim, body units")
      ]
    },
    SPOTS: {
      blurb: "Countable round or oval marks with bare coat between them - freckles, stars, leopard spots. DAPPLES fills the horse; this scatters on it.",
      params: [
        parts("parts", "restrict to these parts"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("spacing", 4.0, "body units between centres", { min: 0.3, max: 20, step: 0.1 }),
        v("radius", 0.9, "spot radius, body units", { min: 0.05, max: 8, step: 0.05 }),
        v("vary", 0.5, "how much the radius varies spot to spot"),
        v("chance", 1.0, "share of lattice cells that carry a spot at all"),
        v("stretch", 1.0, "1 is round, 2 is a 2:1 oval", { min: 0.2, max: 6, step: 0.05 }),
        choice("axis", ["X", "Y", "Z"], "the axis the oval is stretched along"),
        choice("shape", ["round", "heart"], "'heart' swaps the disc for a heart, point down"),
        flag("mirror", "draw on |z|, so the two sides of the horse match"),
        v("softness", 0.25, "edge fade, body units", { min: 0, max: 3, step: 0.05 })
      ]
    },
    RINGS: {
      blurb: "The same scatter drawn hollow - rosettes, wormholes, crescents. Drop 'arc' below 1 to open the ring.",
      params: [
        parts("parts", "restrict to these parts"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("spacing", 6.0, "body units between centres", { min: 0.5, max: 24, step: 0.1 }),
        v("radius", 2.0, "ring radius, body units", { min: 0.1, max: 12, step: 0.05 }),
        v("thickness", 0.6, "wall thickness, body units", { min: 0.05, max: 6, step: 0.05 }),
        v("vary", 0.4, "how much the radius varies ring to ring"),
        v("chance", 1.0, "share of lattice cells that carry a ring at all"),
        v("arc", 1.0, "share of the circumference drawn - below 1 gives a crescent"),
        v("softness", 0.2, "edge fade, body units", { min: 0, max: 3, step: 0.05 })
      ]
    },
    SPECKLE: {
      blurb: "Fine stipple with no boundary anywhere - dust, ticking, speckling. Turn 'clumping' up to make it drift into patches.",
      params: [
        parts("parts", "restrict to these parts"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("spacing", 0.7, "body units between particles - a texel is about 0.5", { min: 0.1, max: 4, step: 0.05 }),
        v("size", 0.45, "particle radius as a share of the spacing"),
        v("density", 0.5, "share of cells carrying a particle"),
        v("clumping", 0.0, "how far a slow field pushes the density around"),
        v("clumpScale", 7.0, "body units per clump", { min: 1, max: 30, step: 0.5 }),
        v("softness", 0.3, "edge fade as a share of the particle radius")
      ]
    },
    STROKES: {
      blurb: "Tapering, curving lines that fork and pinch out - scratches, riblines, brindle bars, wisps. Raise 'length' for long strokes, 'curl' to make them wander.",
      params: [
        parts("parts", "restrict to these parts"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("spacing", 3.0, "body units between neighbouring strokes", { min: 0.2, max: 16, step: 0.1 }),
        v("length", 12.0, "body units a stroke runs before it curves away", { min: 1, max: 60, step: 0.5 }),
        choice("axis", ["X", "Y", "Z"], "the axis the strokes run along"),
        v("width", 0.8, "stroke width, body units - a texel is 0.5", { min: 0.05, max: 8, step: 0.05 }),
        v("curl", 0.35, "how far strokes wander off the axis"),
        v("softness", 0.25, "edge fade, body units", { min: 0, max: 3, step: 0.05 })
      ]
    },
    SPIRAL: {
      blurb: "One closed spiral per named part - the filigree curl. Use 'offset' to slide it off the part's centre.",
      params: [
        parts("parts", "one spiral is drawn per part named here"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("radius", 4.0, "outer radius, body units", { min: 0.5, max: 20, step: 0.1 }),
        v("turns", 2.0, "revolutions from the centre out", { min: 0.25, max: 8, step: 0.25 }),
        v("width", 0.5, "stroke width, body units", { min: 0.05, max: 4, step: 0.05 }),
        choice("axis", ["Z", "X", "Y"], "the axis the spiral is viewed down"),
        v("offset", 0.0, "shift the centre along the part's long axis", { min: -1, max: 1, step: 0.05 }),
        v("softness", 0.2, "edge fade, body units", { min: 0, max: 2, step: 0.05 })
      ]
    },
    WAVES: {
      blurb: "A band with a sine for an edge - scalloped lobes, or (with 'spacing' above 0) parallel ribbons that rise and fall. The one shape here that repeats on purpose.",
      params: [
        parts("parts", "restrict to these parts (and, in 'part' space, measure inside each one)"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        choice("axis", ["X", "Y", "Z"], "the axis the wave runs along"),
        choice("across", ["Y", "X", "Z"], "the axis the wave displaces the band on"),
        choice("shape", ["sine", "triangle", "saw"], "curved, folded into teeth, or cut back square"),
        choice("space", ["part", "body", "units"], "how 'across' is measured - as on AXIS"),
        v("from", 0.0, "start of the band, before the sine displaces it"),
        v("to", 1.0, "end of the band", { min: -2, max: 2, step: 0.01 }),
        v("wavelength", 8.0, "body units per full oscillation", { min: 0.5, max: 40, step: 0.5 }),
        v("amplitude", 0.5, "how far the sine displaces the band", { min: 0, max: 8, step: 0.05 }),
        v("spacing", 0.0, "0 is one band; above 0 repeats it into ribbons", { min: 0, max: 16, step: 0.1 }),
        v("phase", 0.0, "0 keeps every repeat in step, 1 gives each its own"),
        v("softness", 0.15, "fade width outside the band")
      ]
    },
    CRACKLE: {
      blurb: "Polygons that tile, each filled solid, separated by an even channel - a giraffe, a cracked glaze, a dry lake bed. DAPPLES and SPOTS draw round; this draws straight.",
      params: [
        parts("parts", "restrict to these parts"),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("scale", 5.0, "body units across one polygon", { min: 0.5, max: 20, step: 0.1 }),
        v("gap", 0.5, "width of the channel between two polygons", { min: 0.05, max: 4, step: 0.05 }),
        v("warp", 0.35, "how far the polygons are pushed out of true"),
        v("chance", 1.0, "share of polygons that are filled at all"),
        v("softness", 0.08, "edge fade, body units", { min: 0, max: 2, step: 0.01 })
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
      blurb: "Drive pigment to a level: (0, 1) is a black point. For white, use WHITEN.",
      params: [v("red", 0.0, "red level to move toward"), v("black", 0.0, "black level to move toward")]
    },
    WHITEN: {
      phase: "natural",
      blurb: "Mix white hair in - the op every white marking wants. 1 is bald white, a fraction is a roan fleck, and a soft-edged mask greys out instead of browning on its way there.",
      params: [v("amount", 1.0, "share of white hair mixed in")]
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
      blurb: "Walk the texel toward a colour, reading what it already looks like - so it lands the same on a black mane and a cremello one. Point 'hue' at a knob and the colour becomes the horse's own.",
      params: [
        color("color", "the colour to walk toward")
      ].concat(hueParams(HUE_DOC, true), [
        v("strength", 100, "percent of the way there", { min: 0, max: 100, step: 1 }, 82),
        v("opacity", 100, "percent opacity the texel ends at", { min: 0, max: 100, step: 1 })
      ])
    },
    FLAT: {
      phase: "magical",
      blurb: "Flat paint that replaces everything under it. For a gene that must look identical on any base.",
      params: [
        color("color", "flat paint")
      ].concat(hueParams(HUE_DOC, true), [
        v("opacity", 100, "percent opacity", { min: 0, max: 100, step: 1 })
      ])
    },
    RAMP: {
      phase: "magical",
      blurb: "TOWARD with the colour running along an axis - a spectral tail, an aurora band, a mane fading root to tip. Give it 'colors' for named stops, or 'hue' + 'hueSpan' to sweep the wheel.",
      params: [
        colors("colors", "two or more stops, walked through in order")
      ].concat(hueParams(HUE_DOC + " On a ramp it names the first stop."), [
        v("hueSpan", 60, "degrees of hue the ramp travels; negative runs the other way",
          { min: -360, max: 360, step: 5 }),
        choice("axis", ["X", "Y", "Z"], "the axis the ramp runs along"),
        choice("space", ["part", "body", "units"], "'part' runs the ramp inside each part - what a mane wants"),
        v("from", 0.0, "axis position the first stop sits at"),
        v("to", 1.0, "axis position the last stop sits at"),
        v("strength", 100, "percent of the way to the ramp colour", { min: 0, max: 100, step: 1 }),
        v("opacity", 100, "percent opacity the texel ends at", { min: 0, max: 100, step: 1 })
      ])
    },
    PALETTE: {
      phase: "magical",
      blurb: "TOWARD with the colour drawn per cell - opal, nebula, galaxy. Neighbouring cells take unrelated colours and meet at a wall, which is what makes it read as iridescent rather than as a gradient.",
      params: [
        colors("colors", "the palette; each cell takes one entry whole")
      ].concat(hueParams(HUE_DOC + " On a palette it names the centre hue."), [
        v("hueSpread", 40, "degrees either side of 'hue' a cell may land", { min: 0, max: 180, step: 5 }),
        v("seed", 0, "pick a seed knob, or leave it for a stable default", { seedRef: true }),
        v("scale", 5.0, "body units across one colour cell", { min: 0.3, max: 30, step: 0.1 }),
        v("strength", 100, "percent of the way to the cell's colour", { min: 0, max: 100, step: 1 }),
        v("opacity", 100, "percent opacity the texel ends at", { min: 0, max: 100, step: 1 })
      ])
    },
    INVERT: {
      phase: "magical",
      blurb: "The photographic negative of whatever is already there - white to black, orange to blue. The only colour op that is a function of the texel rather than a walk toward a colour, so put it late.",
      params: [
        v("amount", 100, "percent of the way to the negative; 50 lands on flat grey",
          { min: 0, max: 100, step: 1 }),
        v("opacity", 100, "percent opacity the texel ends at", { min: 0, max: 100, step: 1 })
      ]
    }
  };

  // ---- effects: a mirror of common/genetics/spec/AbilityType.java ---------
  //
  // The gameplay half of a gene - what it makes the horse DO, as opposed to the
  // pixels the layers paint. Same contract as MASKS / OPS above: `fallback` MUST
  // equal the Java one, because the export drops any parameter equal to it, and
  // tools/check-parity.mjs compares every entry against the baked Java table.
  //
  // Adding a verb here does NOT add it to the game; the game's copy is the
  // AbilityType registration, and this only lets the creator write the block.

  function e(name, kind, fallback, doc, extra) {
    return Object.assign({ name: name, kind: kind, fallback: fallback, doc: doc }, extra || {});
  }
  function eNum(name, fallback, doc, ui) { return e(name, "NUMBER", fallback, doc, { ui: ui }); }
  function eStr(name, fallback, doc) { return e(name, "STRING", fallback, doc); }
  function eReq(name, doc) { return e(name, "STRING", null, doc, { required: true }); }
  function eChoice(name, choices, fallback, doc) {
    return e(name, "CHOICE", fallback, doc, { choices: choices, required: fallback === null });
  }
  function eColor(name, doc) { return e(name, "COLOR", "#ffffff", doc); }

  var EFFECTS = {
    traversal: {
      doc: "Grant a movement or survival flag while the condition holds.",
      params: [
        eChoice("flag", ["walk_on_water", "walk_on_lava", "fire_immune", "fall_immune",
          "underwater_breathing", "water_averse"], null, "the flag to grant")
      ]
    },
    attribute: {
      doc: "A temporary attribute modifier. Parsed by the game but NOT applied yet - "
        + "the translator logs it once and moves on.",
      params: [
        eChoice("attribute", ["movement_speed", "jump_strength", "max_health", "armor",
          "armor_toughness", "knockback_resistance", "step_height", "safe_fall_distance",
          "scale", "water_movement_efficiency", "movement_efficiency", "oxygen_bonus",
          "gravity"], null,
          "the attribute to modify. There is no 'swim_speed' - vanilla has no such attribute; "
          + "what it has is 'water_movement_efficiency', the share of its land speed a mob "
          + "keeps in water"),
        eChoice("op", ["add", "multiply_base", "multiply_total"], "add",
          "how 'amount' is applied - vanilla modifier operations"),
        eNum("amount", 0, "signed modifier amount", { min: -10, max: 10, step: 0.01 })
      ]
    },
    emitter: {
      doc: "Trail a particle off the horse. 'light' is parsed but not wired - use glow.",
      params: [
        eChoice("kind", ["particle", "light"], "particle", "'particle', or 'light' (not wired yet)"),
        eChoice("shape", ["point", "ring", "trail", "burst"], "point", "emission shape"),
        eChoice("anchor", ["feet", "body", "head", "eyes", "spine", "hooves",
          "front_hooves", "back_hooves", "tail"], "feet", "where on the horse it is centred"),
        e("trigger", "TRIGGER", { on_move: true }, "when it fires"),
        eStr("particle", "minecraft:dust",
          "particle id; 'minecraft:dust' is the one that takes 'color'"),
        eColor("color", "used by particle types that take a colour"),
        eColor("color2", "the second colour, for particles that fade between two"),
        eNum("count", 1, "particles per firing, 1-16", { min: 1, max: 16, step: 1 }),
        eNum("data", 0, "a spare 0-1 number some particles read", { min: 0, max: 1, step: 0.01 }),
        eNum("chance", 1, "odds of firing on any given beat", { min: 0, max: 1, step: 0.01 }),
        eNum("cycle", 0, "ticks for one lap of the hue circle - a rainbow trail; 0 = fixed colours",
          { min: 0, max: 12000, step: 1 })
      ]
    },
    mob_effect: {
      doc: "Keep a status effect topped up on the horse or its rider.",
      params: [
        eReq("effect", "mob effect id, e.g. 'minecraft:dolphins_grace'"),
        eChoice("target", ["self", "rider"], "self", "who the effect lands on"),
        eNum("amplifier", 0, "0-based amplifier", { min: 0, max: 4, step: 1 }),
        eNum("refresh", 40, "re-apply every N ticks (at least 1)", { min: 1, max: 200, step: 1 })
      ]
    },
    yield: {
      doc: "Right-click the horse with an item and get something back - milking. "
        + "Fires on on_interact only.",
      params: [
        e("trigger", "TRIGGER", { on_interact: "" },
          "on_interact only - the item that triggers it, or empty for anything"),
        eStr("consumes", "", "item id taken from the hand, or empty for nothing"),
        eStr("produces", "", "item id handed back; empty makes this a denial branch"),
        eNum("cooldown", 0, "per-horse cooldown, ticks", { min: 0, max: 24000, step: 20 }),
        eNum("denied_damage", 0, "damage dealt when the condition fails (a stallion kick); 0 = none",
          { min: 0, max: 20, step: 0.5 }),
        eStr("denied_message", "", "message shown when the condition fails, or empty for silent"),
        eStr("kind", "", "a name for what sort of yield this is, so a 'charges' effect on some "
          + "OTHER gene can grant extra uses of it. Empty opts out")
      ]
    },
    glow: {
      doc: "Light the world from the horse, and/or render coat regions full-bright.",
      params: [
        eNum("light", 0, "world light level 0-15 the horse emits (0 = none)",
          { min: 0, max: 15, step: 1 }),
        { name: "parts", kind: "PARTS", fallback: [],
          doc: "coat regions that render full-bright, e.g. HAIR (empty = none)" }
      ]
    },
    healing: {
      doc: "A healing aura around the horse.",
      params: [
        eChoice("target", ["players", "rider", "self", "animals"], "players", "who the aura heals"),
        eNum("radius", 3, "reach in blocks, 1-16", { min: 1, max: 16, step: 1 }),
        eNum("amount", 1, "health points restored per beat (two per heart)",
          { min: 0.5, max: 10, step: 0.5 }),
        eNum("interval", 40, "ticks between beats (at least 1)", { min: 1, max: 200, step: 1 }),
        eNum("max_targets", 8, "most entities one beat may reach, 1-64", { min: 1, max: 64, step: 1 })
      ]
    },
    spread: {
      doc: "Convert blocks under the hooves - mycelium, moss or grass.",
      params: [
        eChoice("cover", ["mycelium", "moss", "grass"], null, "what it spreads"),
        eNum("radius", 2, "reach in blocks", { min: 1, max: 8, step: 1 }),
        eNum("chance", 0.5, "odds of converting on any given beat", { min: 0, max: 1, step: 0.01 }),
        eNum("interval", 40, "ticks between beats", { min: 1, max: 200, step: 1 })
      ]
    },
    charges: {
      doc: "Grant extra uses of somebody else's yield before its cooldown bites - "
        + "the 'milked more than once a day' verb. Names a yield KIND, not a gene.",
      params: [
        eReq("kind", "the yield 'kind' this grants extra uses of"),
        eNum("extra", 1, "additional uses per cooldown window", { min: 1, max: 64, step: 1 })
      ]
    },
    breath: {
      doc: "Multiply how long the horse lasts under water. The graded counterpart of the "
        + "underwater_breathing traversal flag, which is absolute.",
      params: [
        eNum("factor", 1.0, "multiplier on the air supply - 2 lasts twice as long, 0.5 half",
          { min: 0.05, max: 40, step: 0.05 })
      ]
    },
    on_death: {
      doc: "What happens to the GROUND where the horse died. Items are a different verb.",
      params: [
        eChoice("effect", ["lava", "water", "explode"], null,
          "what happens at the horse's feet when it dies")
      ]
    },
    item_drop: {
      doc: "What the horse leaves behind. Every value but 'meat' REPLACES the vanilla drop; "
        + "'meat' is added beside it.",
      params: [
        eChoice("drop", ["vanilla", "diamonds", "spawn_egg", "enchanted_sword", "meat"], null,
          "what the horse drops"),
        eNum("min", 1, "fewest items", { min: 0, max: 64, step: 1 }),
        eNum("max", 1, "most items, at least 'min'", { min: 0, max: 64, step: 1 })
      ]
    },
    mob_aura: {
      doc: "How mobs feel about the horse - keep away from it, or fight over it.",
      params: [
        eChoice("mode", ["repel", "attract"], null,
          "'repel' keeps mobs outside the radius; 'attract' makes hostiles inside it "
          + "prefer the horse to anything else"),
        eNum("radius", 8, "reach in blocks, 1-32", { min: 1, max: 32, step: 1 }),
        eNum("interval", 20, "ticks between beats (at least 1)", { min: 1, max: 200, step: 1 }),
        eNum("max_targets", 12, "most entities one beat may reach, 1-64", { min: 1, max: 64, step: 1 })
      ]
    },
    combat: {
      doc: "What the horse hits for, in health points. A vanilla horse has no attack at all.",
      params: [
        eNum("damage", 3, "health points per hit - two per heart", { min: 0, max: 200, step: 0.5 })
      ]
    }
  };

  // The vocabulary a "when" may name, shared by every verb.
  var CONDITION_FLAGS = ["sex_female", "sex_male", "tamed", "untamed", "adult", "baby",
    "full_health", "has_rider", "in_water", "submerged", "on_ground", "on_fire",
    "day", "night", "raining", "thundering", "sky_visible"];

  var TRIGGER_KINDS = ["continuous", "on_move", "interval", "on_interact"];

  HG.schema = {
    MASKS: MASKS,
    OPS: OPS,
    EFFECTS: EFFECTS,
    CONDITION_FLAGS: CONDITION_FLAGS,
    TRIGGER_KINDS: TRIGGER_KINDS,
    effectParam: function (verb, name) {
      var ps = (EFFECTS[verb] || {}).params || [];
      for (var i = 0; i < ps.length; i++) if (ps[i].name === name) return ps[i];
      return null;
    },
    PART_NAMES: PART_NAMES,
    GROUP_NAMES: GROUP_NAMES,
    GROUPS: GROUPS,
    expandParts: expandParts,
    COMBINES: ["MULTIPLY", "MAX", "MIN", "ADD", "SUBTRACT"],
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
