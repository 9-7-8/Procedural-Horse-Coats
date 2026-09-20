#!/usr/bin/env node
// Generate every asset and data file for the showjumping jumps.
//
// WHY THIS EXISTS
// The same reason as bake-double-gates.mjs, which this is modelled on: twelve
// woods times three styles times five connection shapes is a hundred and
// eighty models, plus twelve blockstates of ninety-six variants, whose only
// difference is a wood name and a plank texture. Nobody writes that correctly
// twice, and a wrong texture id fails silently as a purple chequerboard on the
// one jump nobody happens to craft.
//
//   node neoforge-26.1.2/tools/bake-jumps.mjs
//
// Re-run it after changing WOODS, STYLES or any geometry below; see CLAUDE.md's
// regenerate table. It only ever WRITES - removing a wood or a style means
// deleting those files and lang keys by hand.
//
// THE STYLES, AND THE ONE HARD RULE ABOUT THEM
//   vertical   - one rail. The original.
//   oxer       - two rails with a spread, FRONT AND BACK INSIDE ONE BLOCK.
//   crossrails - two poles crossed in an X, lowest in the middle.
// **No style is more than one block deep**, owner's rule. Real courses put
// ground poles in front of an oxer and we are deliberately not building that:
// a jump that occupies two blocks stops being a thing you put in a row and
// starts being a structure.
//
// Style is COSMETIC. Every style collides identically, so a stack of three
// means the same thing whatever it is built from - which is what keeps the
// height ladder an honest reading of a horse's genetics. The collision boxes
// live in block/JumpBlock, not here.
//
// THE STANDARDS ARE CONDITIONAL
// A jump is its rails plus an upright standard at each END of a run. The
// standards are drawn only where the rail stops, which is what the two
// connection flags are for - a row of jumps that each drew both standards
// would grow a pair of posts between every adjacent pair, precisely the
// double-post look the double gate was reworked to avoid.
//
// THE INTERMEDIATE POST
// A long run grows a T-post every third block. Which blocks get one is decided
// in JumpBlock.postsHere off the world coordinate, not here - this file only
// provides the model for a mid-run rail that carries one.
//
// WHICH SIDE IS LEFT
// Authored for facing=south, exactly like the gates, and rotated by the
// blockstate. Left is facing.getCounterClockWise(), which for south is EAST -
// so in this file the LEFT standard is the one at high x. Blockstate y
// rotation maps a direction clockwise (W->N at y=90), which is what keeps that
// consistent at every facing. JumpBlock defines left the same way.

import { mkdirSync, writeFileSync, readFileSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..");
const NS = "horsegenetics";

const A = join(root, "src/main/resources/assets", NS);
const D = join(root, "src/main/resources/data", NS);
const MC_TAGS = join(root, "src/main/resources/data/minecraft/tags/block");

/** name -> plank texture, label. The same twelve block/Jumps.java registers. */
const WOODS = [
  ["oak", "minecraft:block/oak_planks", "Oak"],
  ["spruce", "minecraft:block/spruce_planks", "Spruce"],
  ["birch", "minecraft:block/birch_planks", "Birch"],
  ["jungle", "minecraft:block/jungle_planks", "Jungle"],
  ["acacia", "minecraft:block/acacia_planks", "Acacia"],
  ["dark_oak", "minecraft:block/dark_oak_planks", "Dark Oak"],
  ["pale_oak", "minecraft:block/pale_oak_planks", "Pale Oak"],
  ["mangrove", "minecraft:block/mangrove_planks", "Mangrove"],
  ["cherry", "minecraft:block/cherry_planks", "Cherry"],
  ["bamboo", "minecraft:block/bamboo_planks", "Bamboo"],
  ["crimson", "minecraft:block/crimson_planks", "Crimson"],
  ["warped", "minecraft:block/warped_planks", "Warped"],
];

const STYLE = "jump";
const STYLE_LABEL = "Jump";

/** Must match JumpBlock.Style, in the same order. */
const STYLES = ["vertical", "oxer", "crossrails"];

/**
 * What each style's BUTTON in the jump's screen says.
 *
 * There is no item per style any more - there is one jump item, and style is
 * chosen after placing (owner, 2026-09-20: "let's just combine this all into
 * one horse jump item"). So this is a label table and nothing else; JumpScreen
 * is the twin, and JumpBlock.Style holds the ids.
 */
const STYLE_BUTTON = {
  vertical: "Vertical",
  oxer: "Oxer",
  crossrails: "Crossrails",
};

/** The noun every jump item's name ends in. */
const ITEM_LABEL = "Horse Jump";

/** The style the ITEM ICON is drawn as. Style is not on the item; this is a picture. */
const ICON_STYLE = "vertical";

/** How many a single craft yields, and how many fences it takes. */
const RECIPE_YIELD = 4;
const RECIPE_FENCES = 3;

let written = 0;
function put(path, value) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, JSON.stringify(value, null, 2) + "\n", "utf8");
  written++;
}

// --- geometry -------------------------------------------------------------

const face = (uv) => ({ uv, texture: "#texture" });

// WHICH TINT INDEX EACH HALF CARRIES.
//
// A jump can be painted, per half, and paint is a tint rather than a texture:
// client/JumpTintSource is registered as a LIST of two BlockTintSources and the
// list index is the tintindex on the face. So every rail face says 0 and every
// upright face says 1, and the block's two halves can be two colours.
//
// These must match the order JumpTintSource registers them in, and the tints[]
// array in the item definition below. Nothing checks it: a swap shows as a
// jump whose rails take the colour you painted its standards.
const RAILS_TINT = 0;
const STANDARDS_TINT = 1;

/**
 * The same elements, every face tagged with a tint index.
 *
 * Applied at the last moment rather than built into bar()/standard()/POST,
 * because the SAME boxes are used twice - once split into a rails model and a
 * standards model for the block, and once combined into the whole-jump model
 * the item icon uses - and only the tagging differs between them.
 */
function tinted(elements, tintindex) {
  return elements.map((element) => ({
    ...element,
    faces: Object.fromEntries(
      Object.entries(element.faces).map(([side, f]) => [side, { ...f, tintindex }])
    ),
  }));
}

/** A rail: a box spanning the full width, at some height and depth. */
function bar(yMin, yMax, zMin, zMax) {
  return {
    from: [0, yMin, zMin],
    to: [16, yMax, zMax],
    faces: {
      down: face([0, zMin, 16, zMax]),
      up: face([0, zMin, 16, zMax]),
      north: face([0, 16 - yMax, 16, 16 - yMin]),
      south: face([0, 16 - yMax, 16, 16 - yMin]),
      west: face([zMin, 16 - yMax, zMax, 16 - yMin]),
      east: face([zMin, 16 - yMax, zMax, 16 - yMin]),
    },
  };
}

/** An upright at one end of the run. `x0` is 13 for the LEFT (high x) one. */
function standard(x0, zMin, zMax) {
  const x1 = x0 + 3;
  return {
    from: [x0, 0, zMin],
    to: [x1, 16, zMax],
    faces: {
      down: face([x0, zMin, x1, zMax]),
      up: face([x0, zMin, x1, zMax]),
      north: face([x0, 0, x1, 16]),
      south: face([x0, 0, x1, 16]),
      west: face([zMin, 0, zMax, 16]),
      east: face([zMin, 0, zMax, 16]),
    },
  };
}


/**
 * <b>One block's share of a crossed pair that spans up to three blocks.</b>
 *
 * A crossrail is not one X per block. It is TWO POLES crossing once, and a wide
 * one is two long poles crossing once across the whole obstacle - which is what
 * the owner asked for after three goes at it: "you need one pair of rails that
 * spans up to 3 blocks wide". A row of little per-block X's is a row of little
 * per-block X's however well each one is drawn.
 *
 * So the poles are defined ONCE across the whole run and each block draws the
 * slice of them that passes through it. Over a run of width W = 16 * span, pole
 * A climbs from CROSS_LOW to CROSS_LOW + CROSS_RISE and pole B falls the other
 * way; they cross dead centre. Block `index` covers run-x from 16*index to
 * 16*index + 16, so its slice is a short bar at the height each pole has at the
 * middle of that range, turned to the pole's own angle.
 *
 * THE ANGLE IS THE RUN'S, NOT 45 DEGREES. atan(rise / width): about 45 for one
 * block, 27 for two, 18 for three. That used to be impossible - element
 * rotation was limited to +/-45 and +/-22.5 about one axis - and in 26.1.2 it
 * is not: the angle is an unvalidated float. Verified against the artefacts,
 * because the comment that used to sit here asserted the opposite.
 *
 * THE BAR IS LONGER THAN THE BLOCK IS WIDE. A bar of length L turned by theta
 * only projects L*cos(theta) across, so sizing by length leaves a gap at every
 * seam - which is exactly how the old single-block X ended up marooned in the
 * middle of its block, touching neither the standards nor its neighbour. Sizing
 * by PROJECTION instead - half-length 8/cos(theta) - makes each slice hand off
 * to the next one exactly at the block boundary, so the pole reads as one
 * unbroken pole. Element bounds may legally run from -16 to 32.
 */

/** How far a pole climbs across the WHOLE run, whatever the run's width. */
const CROSS_RISE = 16;

/** The y the low end of a pole sits at - the ground. */
const CROSS_LOW = 0;

/** How thick a pole is. */
const CROSS_THICK = 3;

/**
 * THE TWO POLES SIT AT DIFFERENT DEPTHS, and pass rather than intersect.
 *
 * A real crossrail is two poles in cups at different depths - they cross in
 * front of one another, they do not occupy the same wood. Drawn at one depth
 * they overlap wherever they meet, and two coplanar boxes z-fight: at 45
 * degrees that is a small diamond at the block's centre, but on a two-wide X
 * the poles converge right at the seam between the blocks and the fight runs
 * along a visible wedge. Separating them costs nothing and is what the real
 * thing does.
 *
 * The gap between them is wider than OVERLAY_INFLATE, so the painted overlay
 * boxes do not overlap either.
 */
const CROSS_DEPTH_RISING = [5, 7.8];
const CROSS_DEPTH_FALLING = [8.2, 11];

/** Every span a crossrail can have, and every position within it. */
const CROSS_SPANS = [1, 2, 3];

/** The id suffix for one segment: s2i0 is the left half of a two-wide X. */
const crossSuffix = (span, index) => `_s${span}i${index}`;

function crossArm(yCentre, angleDegrees, half, depth) {
  return {
    from: [8 - half, yCentre - CROSS_THICK / 2, depth[0]],
    to: [8 + half, yCentre + CROSS_THICK / 2, depth[1]],
    // Rotated about the bar's OWN centre, so it turns in place rather than
    // swinging away from the height it was placed at.
    rotation: { origin: [8, yCentre, 8], axis: "z", angle: angleDegrees },
    faces: {
      down: face([0, 6, 16, 10]),
      up: face([0, 6, 16, 10]),
      north: face([0, 6, 16, 9]),
      south: face([0, 6, 16, 9]),
      west: face([6, 6, 10, 9]),
      east: face([6, 6, 10, 9]),
    },
  };
}

/** The two pole slices block `index` of a `span`-wide crossrail draws. */
function crossSegment(span, index) {
  const width = 16 * span;
  const theta = Math.atan2(CROSS_RISE, width);
  const degrees = (theta * 180) / Math.PI;
  const half = 8 / Math.cos(theta);
  // Where the middle of this block sits along the run, as a fraction of it.
  const along = (16 * index + 8) / width;
  const rising = CROSS_LOW + along * CROSS_RISE;
  const falling = CROSS_LOW + (1 - along) * CROSS_RISE;
  return [
    crossArm(rising, degrees, half, CROSS_DEPTH_RISING),
    crossArm(falling, -degrees, half, CROSS_DEPTH_FALLING),
  ];
}

/**
 * Each style: the bars it draws, and how deep its standards must be to
 * enclose them. Mirrors JumpBlock.drawnBars and JumpBlock.standardDepth.
 */
const STYLE_PARTS = {
  vertical: { bars: [bar(11, 15, 6, 10)], depth: [5, 11] },
  // Front and back within the one block. The standards widen to hold both.
  oxer: { bars: [bar(11, 15, 2, 6), bar(11, 15, 10, 14)], depth: [1, 15] },
  // A LONE crossrail, which is segment s1i0. A run of two or three draws
  // different slices - see crossSegment - but this is the shape the icon and
  // the single-block case use, and the one JumpBlock's VoxelShape bounds.
  crossrails: { bars: crossSegment(1, 0), depth: [5, 11] },
};

/**
 * The model suffix for a set of connection flags.
 *
 * ONE function, called from both the model loop and the blockstate loop,
 * because writing it out twice is how this shipped broken the first time:
 * composing `(left?"_l":"") + (right?"_r":"")` gives "_l_r", the models are
 * named "_lr", and the mismatch is invisible until somebody places three jumps
 * in a row and the middle one is a purple cube. Only the both-connected case
 * differs, so a row of two looks perfect. GeneratedJumps.suffix is the twin.
 *
 * `post` is honoured only mid-run, matching JumpBlock.withPost, which never
 * sets it otherwise. The blockstate still has to name every combination, so
 * the end-of-run states map to the postless model rather than to one that
 * would then have to exist.
 */
const suffixFor = (left, right) =>
  left && right ? "_lr" : left ? "_l" : right ? "_r" : "";

/**
 * Every connection suffix, in the order the models are written.
 *
 * THERE IS NO "_lr_post" ANY MORE. The centred T it named is gone: a run is now
 * posted with an UPRIGHT at each group boundary, on the block's own face, not
 * with a post through the middle of a block. The owner's complaint was that a
 * three-wide run had a T "in the middle" of it - it did, because the old rule
 * posted every third block by WORLD POSITION rather than every third block of
 * the run. JumpModel.CONNECTIONS is the twin.
 */
const CONNECTIONS = ["", "_l", "_r", "_lr"];

/** The elements of one style in one connection state. */
function elementsFor(style, suffix) {
  const { bars, depth } = STYLE_PARTS[style];
  const els = [...bars];
  // The suffix names CONNECTIONS, so a connected side is the one with NO
  // standard: the rail carries on into the next block.
  const connectedLeft = suffix === "_l" || suffix.startsWith("_lr");
  const connectedRight = suffix === "_r" || suffix.startsWith("_lr");
  if (!connectedLeft) {
    els.push(standard(13, depth[0], depth[1]));
  }
  if (!connectedRight) {
    els.push(standard(0, depth[0], depth[1]));
  }
  return els;
}

// --- templates ------------------------------------------------------------
//
// A jump is drawn as TWO PARTS - a rails model and a standards model - which
// client/JumpModel pushes one of each of, in whichever two woods the block
// entity names. That is the trick that turns a 12 x 12 product into a 12 + 12
// sum: one part per wood per half, not one model per PAIR of woods.
//
// The rails do not change with the connection state, only the standards do, so
// there is one rails template per style and five standards templates.
//
// The WHOLE-JUMP template is still written, and is now used for one thing only:
// the icon in an inventory slot. An item has no block entity to read, so its
// model is an ordinary single model of one wood - see the item definitions
// below, which select one per wood off the rails component.

for (const style of STYLES) {
  put(join(A, "models/block", `template_${STYLE}_${style}.json`), {
    parent: "block/block",
    textures: { particle: "#texture" },
    // Composed from the SAME split the block's two parts are, so the icon
    // cannot drift from the block, and so its two halves carry the two tint
    // indices the item's own tints[] pair feeds.
    elements: [
      ...tinted(splitElements(style, "").rails, RAILS_TINT),
      ...tinted(splitElements(style, "").standards, STANDARDS_TINT),
    ],
    // Vanilla's fence gui pose: a jump is long and low and looks like nothing
    // at all face-on.
    display: {
      gui: { rotation: [30, 45, 0], translation: [0, -2, 0], scale: [0.8, 0.8, 0.8] },
      head: { rotation: [0, 0, 0], translation: [0, -3, -6], scale: [1, 1, 1] },
    },
  });

  put(join(A, "models/block", `template_jump_${style}_rails.json`), {
    textures: { particle: "#texture" },
    elements: tinted(STYLE_PARTS[style].bars, RAILS_TINT),
  });

  // CROSSRAILS GET ONE RAILS MODEL PER SEGMENT - six of them, for the three
  // spans and each position within them. Every other style's rails are the same
  // in every block of a run, so they get the one above and nothing else.
  if (style === "crossrails") {
    for (const span of CROSS_SPANS) {
      for (let index = 0; index < span; index++) {
        put(
          join(A, "models/block",
            `template_jump_${style}_rails${crossSuffix(span, index)}.json`),
          {
            textures: { particle: "#texture" },
            elements: tinted(crossSegment(span, index), RAILS_TINT),
          }
        );
      }
    }
  }

  for (const suffix of CONNECTIONS) {
    // A mid-run block with no post draws no standards at all. An empty
    // elements list is legal and bakes to nothing, which is what we want -
    // but it must still EXIST, because the model id is referenced.
    put(join(A, "models/block", `template_jump_${style}_standards${suffix}.json`), {
      textures: { particle: "#texture" },
      elements: tinted(splitElements(style, suffix).standards, STANDARDS_TINT),
    });
  }
}

/** Which elements belong to the rails half, and which to the standards half. */
function splitElements(style, suffix) {
  const { depth } = STYLE_PARTS[style];
  const connectedLeft = suffix === "_l" || suffix.startsWith("_lr");
  const connectedRight = suffix === "_r" || suffix.startsWith("_lr");
  const standards = [];
  if (!connectedLeft) standards.push(standard(13, depth[0], depth[1]));
  if (!connectedRight) standards.push(standard(0, depth[0], depth[1]));
  // The intermediate post is a STANDARD, not a rail: it is an upright, and it
  // should take the standards' wood when the two differ.
  return { rails: [...STYLE_PARTS[style].bars], standards };
}

// --- per-wood models and recipes -------------------------------------------
//
// THERE IS NO PER-WOOD BLOCK ANY MORE, and no per-wood item. There were twelve
// of each until 2026-09-20, when the woods became block-entity data so that a
// jump's rails and its standards could differ. What a wood still needs is
// models - three per style, each one line of parent-and-texture - and a recipe
// that stamps its name onto the crafted item.

const lang = {};

// THE PAINT OVERLAY, which is not per wood and is not a wood at all.
//
// A dyed half keeps its own wood part and gains a SECOND part laid over it:
// the same boxes, very slightly inflated, drawn in flat white at a fixed alpha
// and tinted. So the result is an alpha blend - wood*(1-a) + dye*a - and the
// grain and shading underneath survive at full contrast.
//
// It got here by two wrong turns, both worth recording. Tinting the wood itself
// is a MULTIPLY, which can only darken and cannot shift a hue the wood does not
// already have: blue-on-oak came out a dark navy-brown. Painting a pale neutral
// pole instead fixed the colour and threw the wood away with it - "we're losing
// too much of the original texture/shading" (owner). An overlay is the only one
// of the three that keeps both.
//
// INFLATION IS NOT OPTIONAL. Two coplanar surfaces z-fight, and z-fighting on a
// fence rail reads as flickering dirt rather than as a rendering bug. OVERLAY_
// INFLATE is in the same pixel units as the boxes.
const OVERLAY_TEXTURE = `${NS}:block/paint_overlay`;
const OVERLAY_INFLATE = 0.12;

/** The same elements, grown a hair in every direction, keeping their tint index. */
function inflated(elements) {
  return elements.map((element) => ({
    ...element,
    from: element.from.map((v) => v - OVERLAY_INFLATE),
    to: element.to.map((v) => v + OVERLAY_INFLATE),
  }));
}

for (const style of STYLES) {
  if (style === "crossrails") {
    for (const span of CROSS_SPANS) {
      for (let index = 0; index < span; index++) {
        put(
          join(A, "models/block",
            `overlay_jump_${style}_rails${crossSuffix(span, index)}.json`),
          {
            textures: {
              texture: { sprite: OVERLAY_TEXTURE, force_translucent: true },
              particle: OVERLAY_TEXTURE,
            },
            elements: inflated(tinted(crossSegment(span, index), RAILS_TINT)),
          }
        );
      }
    }
  }
  put(join(A, "models/block", `overlay_jump_${style}_rails.json`), {
    // force_translucent, because the sprite is flat white at a constant alpha
    // and we are not leaving the render layer to be inferred from it: a wash
    // that came out CUTOUT would threshold to fully opaque and hide the very
    // wood it exists to let through.
    textures: {
      texture: { sprite: OVERLAY_TEXTURE, force_translucent: true },
      particle: OVERLAY_TEXTURE,
    },
    elements: inflated(tinted(STYLE_PARTS[style].bars, RAILS_TINT)),
  });
  for (const suffix of CONNECTIONS) {
    put(join(A, "models/block", `overlay_jump_${style}_standards${suffix}.json`), {
      textures: {
        texture: { sprite: OVERLAY_TEXTURE, force_translucent: true },
        particle: OVERLAY_TEXTURE,
      },
      elements: inflated(
        tinted(splitElements(style, suffix).standards, STANDARDS_TINT)
      ),
    });
  }
}

for (const [wood, texture, woodLabel] of WOODS) {
  // What the screen and every item name call this wood. NOT the plank's own
  // name, which is "Oak Planks" and would give "Oak Planks Jump".
  // JumpWoods.label reads these, and falls back to the title-cased key for a
  // modded wood that has none.
  lang[`horsegenetics.wood.${wood}`] = woodLabel;

  for (const style of STYLES) {
    // The icon model: a whole jump of one wood, both standards drawn.
    put(join(A, "models/block", `${wood}_${STYLE}_${style}.json`), {
      parent: `${NS}:block/template_${STYLE}_${style}`,
      textures: { texture },
    });
    put(join(A, "models/block", `${wood}_jump_${style}_rails.json`), {
      parent: `${NS}:block/template_jump_${style}_rails`,
      textures: { texture },
    });
    if (style === "crossrails") {
      for (const span of CROSS_SPANS) {
        for (let index = 0; index < span; index++) {
          const seg = crossSuffix(span, index);
          put(join(A, "models/block", `${wood}_jump_${style}_rails${seg}.json`), {
            parent: `${NS}:block/template_jump_${style}_rails${seg}`,
            textures: { texture },
          });
        }
      }
    }
    for (const suffix of CONNECTIONS) {
      put(join(A, "models/block", `${wood}_jump_${style}_standards${suffix}.json`), {
        parent: `${NS}:block/template_jump_${style}_standards${suffix}`,
        textures: { texture },
      });
    }
  }

  // THE HOUSE RULE IS OBEYED HERE, AND THE YIELD IS WHY IT CAN BE.
  // wiki/items.html#rules: every recipe carries at least one modded
  // ingredient, so none of them can collide with a vanilla or third-party one.
  // The double gate is the documented exception; this is deliberately NOT a
  // second one, because the argument that got the gate its exemption - nothing
  // in vanilla consumes a fence gate - does not hold for plain fences, which
  // plenty of mods do consume.
  //
  // So it takes one raw horse hair. A hair is sheared one to three at a time,
  // once per horse per day; four jumps per hair is the other side of that.
  //
  // ONE RECIPE PER WOOD STILL, even though there is one item. The wood is not
  // in the id any more, it is in the result's COMPONENTS - which is the whole
  // reason twelve recipes can make one item and it still comes out birch. A
  // mixed-wood craft remains impossible, and is now also unnecessary: put the
  // jump down and swap one half's plank in its screen.
  //
  // Only the VERTICAL is craftable. The other two styles are a free click in
  // the screen, so a recipe for each would be twenty-four files to save one.
  put(join(D, "recipe", `${STYLE}_${wood}.json`), {
    type: "minecraft:crafting_shapeless",
    category: "building",
    ingredients: [
      ...Array(RECIPE_FENCES).fill(`minecraft:${wood}_fence`),
      `${NS}:horse_hair`,
    ],
    result: {
      count: RECIPE_YIELD,
      id: `${NS}:${STYLE}`,
      components: {
        [`${NS}:jump_rails`]: wood,
        [`${NS}:jump_standards`]: wood,
      },
    },
  });
}

// --- the one block ---------------------------------------------------------

// Authored for facing=south. Blockstate y rotation maps a direction clockwise.
const VARIANT_ROTATION = { south: 0, west: 90, north: 180, east: 270 };

// Every variant names the custom model and the two SUFFIXES it should compose
// per wood - the wood list itself is not in here, because it would be
// twenty-four entries repeated ninety-six times and half of it depends on which
// other mods are installed. See JumpModel.Unbaked.
const variants = {};
for (const style of STYLES) {
  for (const [facing, y] of Object.entries(VARIANT_ROTATION)) {
    for (const left of [false, true]) {
      for (const right of [false, true]) {
        {
          const v = {
            type: `${NS}:jump`,
            rails: `jump_${style}_rails`,
            // THE STANDARDS FIELD IS A BASE, NOT A CONNECTION STATE, for every
            // style. JumpModel appends the suffix itself, because which blocks
            // post is decided by where the group boundaries fall and that is
            // only known once the run has been walked - which a blockstate
            // cannot do, since it sees one neighbour.
            //
            // This read `..._standards${suffixFor(left, right)}` for the
            // non-spanning styles for one build, from back when only crossrails
            // chose their own. The model appended a second suffix to it and
            // every vertical in a run asked for `_standards_lr_l`, which is
            // nothing - a missing-texture cube on every jump with a neighbour.
            // check-block-models.mjs passed throughout: it reads the ids in the
            // blockstate, and those were all real.
            standards: `jump_${style}_standards`,
            // "spanning" tells JumpModel to bake the six segment models and the
            // four standards states beside the plain ones, and to pick between
            // them per position. Only crossrails have them.
            ...(style === "crossrails" ? { spanning: true } : {}),
            // NOT uvlocked on the crossrails. uvlock re-projects a face's UVs
            // against the block axes after the blockstate's y rotation, and on
            // an element that is ITSELF rotated 45 degrees the two fight and
            // the grain shears. The other two styles are axis-aligned and lock
            // cleanly. UNVERIFIED: this is reasoning about the renderer, not
            // something that has been looked at.
            uvlock: style !== "crossrails",
          };
          if (y !== 0) v.y = y;
          // Property order in the key does not matter to the game, but
          // keeping it alphabetical keeps the diffs readable.
          variants[
            `facing=${facing},left=${left},right=${right},style=${style}`
          ] = v;
        }
      }
    }
  }
}
put(join(A, "blockstates", `${STYLE}.json`), { variants });

// ONE ITEM, PICKING ITS ICON OFF A COMPONENT AND ITS TWO COLOURS OFF TWO MORE.
//
// minecraft:select with property minecraft:component matches the WHOLE value of
// one component, so this selects on jump_rails alone - twelve cases - rather
// than on a pair-valued component, which would need a case per pair. The icon
// is therefore always right about the rails and says nothing about the
// standards' WOOD, which is the correct trade at sixteen pixels.
//
// The fallback is oak: a jump with no components at all (a /give) draws as one
// rather than as a missing model.
//
// THE TWO TINTS ARE ORDER-SENSITIVE. tints[0] feeds every face whose
// tintindex is 0 - the rails - and tints[1] the standards. That order has to
// match RAILS_TINT/STANDARDS_TINT above and the list JumpTintSource registers;
// nothing checks it, and a swap shows as a jump whose rails take the colour you
// painted its uprights. The item's paint DOES show both halves, unlike its
// wood, because a tint is a colour per face rather than a whole model.
{
  const model = (wood) => ({
    type: "minecraft:model",
    model: `${NS}:block/${wood}_${STYLE}_${ICON_STYLE}`,
    tints: [
      { type: `${NS}:jump_rails_tint` },
      { type: `${NS}:jump_standards_tint` },
    ],
  });
  put(join(A, "items", `${STYLE}.json`), {
    model: {
      type: "minecraft:select",
      property: "minecraft:component",
      component: `${NS}:jump_rails`,
      cases: WOODS.map(([wood]) => ({ when: wood, model: model(wood) })),
      fallback: model("oak"),
    },
  });
}

// "%s Horse Jump" / "%s & %s Horse Jump" - JumpItem.getName picks between them
// on whether the two halves match, wood AND paint, because a jump whose halves
// differ has no one thing to be named after.
lang[`item.${NS}.${STYLE}`] = `%s ${ITEM_LABEL}`;
lang[`item.${NS}.${STYLE}.mixed`] = `%s & %s ${ITEM_LABEL}`;

// ONE DROP, CARRYING THE RIGHT TWO WOODS AND WHATEVER PAINT IS ON THEM.
//
// It used to be a three-entry switch on the style property, because there was
// an item per style and breaking an oxer had to give an oxer back. There is one
// item now, so there is one entry: style is not on the item at all, and a jump
// broken out of a course and put back down takes its style from whatever it
// lands next to.
//
// copy_components is what carries everything that IS on the item. The woods and
// the paint live on the block entity, and without copying them off it every
// jump broken out of a course comes back plain oak.
// JumpBlockEntity.collectImplicitComponents is what it reads;
// JumpBlock.setPlacedBy is what puts them back.
put(join(D, "loot_table/blocks", `${STYLE}.json`), {
  type: "minecraft:block",
  pools: [
    {
      rolls: 1,
      bonus_rolls: 0,
      entries: [
        {
          type: "minecraft:item",
          name: `${NS}:${STYLE}`,
          functions: [
            {
              function: "minecraft:copy_components",
              source: "block_entity",
              include: [
                `${NS}:jump_rails`,
                `${NS}:jump_standards`,
                `${NS}:jump_rails_dye`,
                `${NS}:jump_standards_dye`,
                `${NS}:jump_size`,
              ],
            },
          ],
        },
      ],
      conditions: [{ condition: "minecraft:survives_explosion" }],
    },
  ],
});

// The block's own name, which is what the screen's title bar shows.
lang[`block.${NS}.${STYLE}`] = STYLE_LABEL;

// The screen: two row captions, the style heading, and a button per style.
lang[`${NS}.jump.rails`] = "Rails";
lang[`${NS}.jump.standards`] = "Standards";
lang[`${NS}.jump.style`] = "Style";
lang[`${NS}.jump.size`] = "Height";
// The multiple on its own is a number nobody can act on; the blocks-to-clear is
// what a player is actually asking when they set one, and it is the number the
// horse has to beat. JumpScreen formats both.
lang[`${NS}.jump.size.value`] = "%s blocks";
lang[`${NS}.jump.size.clear`] = "%s to clear";
for (const style of STYLES) {
  lang[`${NS}.jump.style.${style}`] = STYLE_BUTTON[style];
}

// The action-bar line a player gets the first times they place one, until they
// open a screen. One item with everything behind a right-click needs saying.
lang[`${NS}.jump.hint`] = "Right-click the jump to choose its wood, paint and style";

// --- tags -----------------------------------------------------------------
// MERGED, not replaced: mineable/axe already belongs to the double gates, and
// a datapack has one file per tag. Writing {values: ours} here would silently
// take an axe off every gate in the mod. bake-double-gates.mjs merges into the
// same file for the same reason - if you add a third family, merge too.
//
// ONE VALUE NOW, not twelve. The twelve per-wood ids are stale and were deleted
// by hand when the block families merged; a tag naming a block that does not
// exist is not an error, so nothing would have told us.

const axePath = join(MC_TAGS, "mineable/axe.json");
const existingTag = existsSync(axePath)
  ? JSON.parse(readFileSync(axePath, "utf8")).values ?? []
  : [];
const merged = [...new Set([...existingTag, `${NS}:${STYLE}`])];
put(axePath, { values: merged });

// --- lang -----------------------------------------------------------------
// Merged into the existing file rather than replacing it: every other key in
// there is hand-written. Merging only ADDS, so a removed wood's key has to be
// taken out by hand.

const langPath = join(A, "lang/en_us.json");
const existing = JSON.parse(readFileSync(langPath, "utf8"));
for (const [k, v] of Object.entries(lang)) existing[k] = v;
writeFileSync(langPath, JSON.stringify(existing, null, 2) + "\n", "utf8");

console.log(
  `jumps: one block, one item, ${STYLES.length} styles, ${WOODS.length} woods, ` +
    `${written} files written, ${Object.keys(lang).length} lang keys merged, ` +
    `${merged.length} values in mineable/axe`
);
