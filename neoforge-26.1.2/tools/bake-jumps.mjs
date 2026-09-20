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
 * The intermediate upright - the T where a post meets the rail partway along a
 * run. Dead centre, so it is the same box whichever way the rail runs.
 * JumpBlock.post() is the twin.
 */
const POST = {
  from: [6, 0, 6],
  to: [10, 16, 10],
  faces: {
    down: face([6, 6, 10, 10]),
    up: face([6, 6, 10, 10]),
    north: face([6, 0, 10, 16]),
    south: face([6, 0, 10, 16]),
    west: face([6, 0, 10, 16]),
    east: face([6, 0, 10, 16]),
  },
};

/**
 * One arm of the X, rotated in the block's own model.
 *
 * THE ARM IS LONGER THAN THE BLOCK IS WIDE, AND THAT IS THE FIX.
 * A bar 16 long turned 45 degrees only PROJECTS about 11.3 across, so the X sat
 * marooned in the middle of its block: it never reached the standards, and a
 * row of crossrails was a row of little disconnected X's with a gap at every
 * seam. The owner's verdict, three times, was that the crossrails "look bad".
 *
 * So the arm is sized by its projection instead - half-length 8/cos(angle),
 * which lands its centre line exactly on the block's corners. It now meets the
 * standards at the ends of a run and meets its neighbour's arm mid-run, which
 * is what a real crossrail line looks like. Element bounds may legally run from
 * -16 to 32, so the overhang is fine.
 *
 * THE OLD COMMENT HERE CLAIMED element rotation was limited to +/-45 and
 * +/-22.5 about one axis. **That is no longer true in 26.1.2** - the angle is
 * an unvalidated float and there is a second multi-axis form - so a shallower
 * arm spanning two or three blocks is expressible whenever that gets built.
 * See wiki/item-jumps.html.
 */
function crossArm(angle) {
  // Sized by projection: the half-length whose horizontal span is exactly 8.
  const half = 8 / Math.cos((Math.abs(angle) * Math.PI) / 180);
  return {
    from: [8 - half, 7, 6],
    to: [8 + half, 10, 10],
    rotation: { origin: [8, 8, 8], axis: "z", angle },
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

/**
 * Each style: the bars it draws, and how deep its standards must be to
 * enclose them. Mirrors JumpBlock.drawnBars and JumpBlock.standardDepth.
 */
const STYLE_PARTS = {
  vertical: { bars: [bar(11, 15, 6, 10)], depth: [5, 11] },
  // Front and back within the one block. The standards widen to hold both.
  oxer: { bars: [bar(11, 15, 2, 6), bar(11, 15, 10, 14)], depth: [1, 15] },
  crossrails: { bars: [crossArm(45), crossArm(-45)], depth: [5, 11] },
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
const suffixFor = (left, right, post) =>
  left && right ? (post ? "_lr_post" : "_lr") : left ? "_l" : right ? "_r" : "";

/** Every connection suffix, in the order the models are written. */
const CONNECTIONS = ["", "_l", "_r", "_lr", "_lr_post"];

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
  if (suffix === "_lr_post") {
    els.push(POST);
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
  if (suffix === "_lr_post") standards.push(POST);
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
        for (const post of [false, true]) {
          const v = {
            type: `${NS}:jump`,
            rails: `jump_${style}_rails`,
            standards: `jump_${style}_standards${suffixFor(left, right, post)}`,
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
            `facing=${facing},left=${left},post=${post},right=${right},style=${style}`
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
