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
 * What each style is called in an item name, and the id suffix its item
 * carries.
 *
 * The VERTICAL is the bare `<wood>_jump` and is named from the BLOCK, because
 * its item is the one registered with useBlockDescriptionPrefix(). The other
 * two need names of their own - "Oak Oxer", not a second "Oak Jump" - so they
 * get `item.horsegenetics.*` keys here. Jumps.register is the twin.
 */
const STYLE_ITEM = {
  vertical: { suffix: "", label: null },
  oxer: { suffix: "_oxer", label: "Oxer" },
  crossrails: { suffix: "_crossrails", label: "Crossrails" },
};

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
 * Element rotation is limited to +/-45 and +/-22.5 about ONE axis, which is
 * exactly enough for a crossrail. A 16-long bar turned 45 degrees about z
 * projects to about 11.3 across, so it stays inside the block rather than
 * poking into its neighbours - which is right: crossrails are each their own
 * X, and do not run together the way the other two styles do.
 */
function crossArm(angle) {
  return {
    from: [0, 7, 6],
    to: [16, 10, 10],
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
// Three styles x five connection states, carrying the actual boxes, which
// every wood then parents to with only its texture changed.

for (const style of STYLES) {
  for (const suffix of CONNECTIONS) {
    const model = {
      textures: { particle: "#texture" },
      elements: elementsFor(style, suffix),
    };
    // Only the both-standards model of each style is ever shown in an
    // inventory slot, so it is the only one that needs a display transform.
    // Vanilla's fence gui pose: a jump is long and low and looks like nothing
    // at all face-on.
    if (suffix === "") {
      model.parent = "block/block";
      model.display = {
        gui: { rotation: [30, 45, 0], translation: [0, -2, 0], scale: [0.8, 0.8, 0.8] },
        head: { rotation: [0, 0, 0], translation: [0, -3, -6], scale: [1, 1, 1] },
      };
    }
    put(join(A, "models/block", `template_${STYLE}_${style}${suffix}.json`), model);
  }
}

// --- per-wood models, blockstates, items, recipes, loot --------------------

// Authored for facing=south. Blockstate y rotation maps a direction clockwise.
const VARIANT_ROTATION = { south: 0, west: 90, north: 180, east: 270 };

const lang = {};
const tagValues = [];

for (const [wood, texture, woodLabel] of WOODS) {
  const id = `${wood}_${STYLE}`;
  tagValues.push(`${NS}:${id}`);
  lang[`block.${NS}.${id}`] = `${woodLabel} ${STYLE_LABEL}`;

  for (const style of STYLES) {
    for (const suffix of CONNECTIONS) {
      put(join(A, "models/block", `${id}_${style}${suffix}.json`), {
        parent: `${NS}:block/template_${STYLE}_${style}${suffix}`,
        textures: { texture },
      });
    }
  }

  // 3 styles x 4 facings x left x right x post = 96 variants
  const variants = {};
  for (const style of STYLES) {
    for (const [facing, y] of Object.entries(VARIANT_ROTATION)) {
      for (const left of [false, true]) {
        for (const right of [false, true]) {
          for (const post of [false, true]) {
            const v = {
              model: `${NS}:block/${id}_${style}${suffixFor(left, right, post)}`,
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
  put(join(A, "blockstates", id + ".json"), { variants });

  // ONE ITEM PER STYLE. Each points at that style's both-standards model,
  // which is the only one of the five given a gui transform above.
  for (const style of STYLES) {
    const { suffix, label } = STYLE_ITEM[style];
    put(join(A, "items", id + suffix + ".json"), {
      model: { type: "minecraft:model", model: `${NS}:block/${id}_${style}` },
    });
    if (label !== null) {
      lang[`item.${NS}.${id}${suffix}`] = `${woodLabel} ${label}`;
    }
  }

  // ONE DROP, BUT THE RIGHT STYLE'S ITEM. A pool with rolls:1 picks among the
  // entries whose conditions pass, and exactly one style condition can pass,
  // so this is a switch rather than a lottery. Without it, breaking an oxer
  // hands back a vertical and the style is quietly lost - which is the kind of
  // thing nobody reports as a bug, they just stop using the feature.
  //
  // Unlike the double gate this is ONE block, so there is no partner to orphan
  // and no half=left condition to get right.
  put(join(D, "loot_table/blocks", id + ".json"), {
    type: "minecraft:block",
    pools: [
      {
        rolls: 1,
        bonus_rolls: 0,
        entries: STYLES.map((style) => ({
          type: "minecraft:item",
          name: `${NS}:${id}${STYLE_ITEM[style].suffix}`,
          conditions: [
            {
              condition: "minecraft:block_state_property",
              block: `${NS}:${id}`,
              properties: { style },
            },
          ],
        })),
        conditions: [{ condition: "minecraft:survives_explosion" }],
      },
    ],
  });

  // THE HOUSE RULE IS OBEYED HERE, AND THE YIELD IS WHY IT CAN BE.
  // wiki/items.html#rules: every recipe carries at least one modded
  // ingredient, so none of them can collide with a vanilla or third-party one.
  // The double gate is the documented exception; this is deliberately NOT a
  // second one, because the argument that got the gate its exemption - nothing
  // in vanilla consumes a fence gate - does not hold for plain fences, which
  // plenty of mods do consume.
  //
  // So it takes one raw horse hair. The thing to avoid was the braided rope
  // the gate used to want, which priced a paddock at twelve hairs a gate; a
  // hair is sheared one to three at a time, once per horse per day. Four jumps
  // per hair is the other side of that.
  //
  // THIS IS THE SAME-WOOD RECIPE. A mixed-wood craft - three fences of
  // different woods giving a jump of whichever wood there was most of - cannot
  // be a data recipe at all, because the result depends on the inputs. That is
  // a custom recipe in Java and it is separate work; until it lands, mixing
  // woods simply does not craft.
  put(join(D, "recipe", id + ".json"), {
    type: "minecraft:crafting_shapeless",
    category: "building",
    ingredients: [
      ...Array(RECIPE_FENCES).fill(`minecraft:${wood}_fence`),
      `${NS}:horse_hair`,
    ],
    result: { count: RECIPE_YIELD, id: `${NS}:${id}` },
  });
}

// --- THE SINGLE BLOCK -----------------------------------------------------
//
// The twelve per-wood blocks above are being replaced by ONE block whose woods
// are block-entity data, so that the rails and the standards can be different
// woods. As blockstate properties that pair would be 12 x 12 x everything else
// - 13,824 states, every one allocated at registry bootstrap on the server too.
//
// The model that reads the data is client/JumpModel, selected by the "type"
// field below. It needs the geometry SPLIT IN TWO - a rails model and a
// standards model per wood - because it composes a jump out of one part of
// each. That is the trick that turns a 12 x 12 product into a 12 + 12 sum.
//
// While the migration is in progress this emits ALONGSIDE the per-wood assets
// rather than instead of them, so the game keeps loading at every step.

/** Which elements belong to the rails half, and which to the standards half. */
function splitElements(style, suffix) {
  const { bars, depth } = STYLE_PARTS[style];
  const connectedLeft = suffix === "_l" || suffix.startsWith("_lr");
  const connectedRight = suffix === "_r" || suffix.startsWith("_lr");
  const standards = [];
  if (!connectedLeft) standards.push(standard(13, depth[0], depth[1]));
  if (!connectedRight) standards.push(standard(0, depth[0], depth[1]));
  // The intermediate post is a STANDARD, not a rail: it is an upright, and it
  // should take the standards' wood when the two differ.
  if (suffix === "_lr_post") standards.push(POST);
  return { rails: [...bars], standards };
}

for (const style of STYLES) {
  // The rails do not change with the connection state - only the standards do -
  // so there is one rails model per style rather than one per connection.
  put(join(A, "models/block", `template_jump_${style}_rails.json`), {
    textures: { particle: "#texture" },
    elements: STYLE_PARTS[style].bars,
  });
  for (const suffix of CONNECTIONS) {
    const { standards } = splitElements(style, suffix);
    const model = { textures: { particle: "#texture" } };
    // A mid-run block with no post draws no standards at all. An empty
    // elements list is legal and bakes to nothing, which is what we want -
    // but it must still EXIST, because the model id is referenced.
    model.elements = standards;
    put(join(A, "models/block", `template_jump_${style}_standards${suffix}.json`), model);
  }
}

for (const [wood, texture] of WOODS) {
  for (const style of STYLES) {
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
}

// One blockstate for the one block. Every variant names the custom model and
// the two SUFFIXES it should compose per wood - the wood list itself is not in
// here, because it would be twenty-four entries repeated ninety-six times and
// half of it depends on which other mods are installed. See JumpModel.Unbaked.
const singleVariants = {};
for (const style of STYLES) {
  for (const [facing, y] of Object.entries(VARIANT_ROTATION)) {
    for (const left of [false, true]) {
      for (const right of [false, true]) {
        for (const post of [false, true]) {
          const conn = suffixFor(left, right, post);
          const v = {
            type: `${NS}:jump`,
            rails: `jump_${style}_rails`,
            standards: `jump_${style}_standards${conn}`,
            uvlock: style !== "crossrails",
          };
          if (y !== 0) v.y = y;
          singleVariants[
            `facing=${facing},left=${left},post=${post},right=${right},style=${style}`
          ] = v;
        }
      }
    }
  }
}
put(join(A, "blockstates", "jump.json"), { variants: singleVariants });

// The item shows the oak vertical. Which wood an item PLACES comes from its
// materials component, not from the model - the icon is just an icon.
put(join(A, "items", "jump.json"), {
  model: { type: "minecraft:model", model: `${NS}:block/oak_jump_vertical` },
});

lang[`block.${NS}.jump`] = "Jump";

// --- tags -----------------------------------------------------------------
// MERGED, not replaced: mineable/axe already belongs to the double gates, and
// a datapack has one file per tag. Writing {values: ours} here would silently
// take an axe off every gate in the mod. bake-double-gates.mjs merges into the
// same file for the same reason - if you add a third family, merge too.

const axePath = join(MC_TAGS, "mineable/axe.json");
const existingTag = existsSync(axePath)
  ? JSON.parse(readFileSync(axePath, "utf8")).values ?? []
  : [];
const merged = [...new Set([...existingTag, ...tagValues])];
put(axePath, { values: merged });

// --- lang -----------------------------------------------------------------
// Merged into the existing file rather than replacing it: every other key in
// there is hand-written. Merging only ADDS, so a removed jump's key has to be
// taken out by hand.

const langPath = join(A, "lang/en_us.json");
const existing = JSON.parse(readFileSync(langPath, "utf8"));
for (const [k, v] of Object.entries(lang)) existing[k] = v;
writeFileSync(langPath, JSON.stringify(existing, null, 2) + "\n", "utf8");

console.log(
  `jumps: ${WOODS.length} woods, ${STYLES.length} styles, ` +
    `${STYLES.length * CONNECTIONS.length} templates, ${written} files written, ` +
    `${Object.keys(lang).length} lang keys merged, ${merged.length} values in mineable/axe`
);
