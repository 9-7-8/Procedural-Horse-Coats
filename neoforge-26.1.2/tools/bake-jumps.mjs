#!/usr/bin/env node
// Generate every asset and data file for the showjumping rails.
//
// WHY THIS EXISTS
// The same reason as bake-double-gates.mjs, which this is modelled on: twelve
// woods times five connection shapes is sixty models, twelve blockstates of
// thirty-two variants, and a recipe, loot table and lang key each, whose only
// difference is a wood name and a plank texture. Nobody writes that correctly
// twice, and a wrong texture id fails silently as a purple chequerboard on the
// one jump nobody happens to craft.
//
//   node neoforge-26.1.2/tools/bake-jumps.mjs
//
// Re-run it after changing WOODS or any of the geometry below; see CLAUDE.md's
// regenerate table. It only ever WRITES - removing a wood means deleting that
// wood's files and lang keys by hand.
//
// THE GEOMETRY, AND WHY THE STANDARDS ARE CONDITIONAL
// A jump is a rail spanning the whole block plus an upright standard at each
// END of a run. The standards are drawn only where the rail stops, which is
// what the two connection flags are for - a row of jumps that each drew both
// standards would grow a pair of posts between every adjacent pair, which is
// precisely the double-post look the double gate was reworked to avoid. See
// block/JumpBlock for the matching VoxelShape; the two are written twice and
// must agree, or the jump you can see and the jump you collide with differ.
//
// HEIGHT IS STACKING
// There is no height state; you stack them, and each block asks for one more
// block of clearance. The bottom rung is 1.5 blocks because the COLLISION box
// is half a block taller than anything drawn here - that lives in JumpBlock,
// not in this file, and without it a horse steps straight over a lone jump.
// The standards run the full 0-16 so that a stack reads as one continuous
// upright, and so does the intermediate post.
//
// THE INTERMEDIATE POST
// A long run grows a T-post every third block, so a fence line is not one
// unbroken rail between two distant standards. Which blocks get one is decided
// in JumpBlock.postsHere off the world coordinate, not here - this file only
// has to provide the model for a mid-run rail that carries one.
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

/** How many a single craft yields - see the recipe note at the bottom. */
const RECIPE_YIELD = 4;

let written = 0;
function put(path, value) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, JSON.stringify(value, null, 2) + "\n", "utf8");
  written++;
}

// --- geometry -------------------------------------------------------------

const face = (uv) => ({ uv, texture: "#texture" });

/**
 * The rail: the full width of the block, so two adjacent jumps meet with no
 * seam. Its end caps are left in rather than culled - between two jumps they
 * are back-to-back quads facing opposite ways, which back-face culling handles
 * and which therefore does not z-fight.
 */
const RAIL = {
  from: [0, 11, 6],
  to: [16, 15, 10],
  faces: {
    down: face([0, 6, 16, 10]),
    up: face([0, 6, 16, 10]),
    north: face([0, 1, 16, 5]),
    south: face([0, 1, 16, 5]),
    west: face([6, 1, 10, 5]),
    east: face([6, 1, 10, 5]),
  },
};

/** The standard at high x - the LEFT one, for the authored facing=south. */
const STANDARD_LEFT = {
  from: [13, 0, 5],
  to: [16, 16, 11],
  faces: {
    down: face([13, 5, 16, 11]),
    up: face([13, 5, 16, 11]),
    north: face([13, 0, 16, 16]),
    south: face([13, 0, 16, 16]),
    west: face([5, 0, 11, 16]),
    east: face([5, 0, 11, 16]),
  },
};

/** The standard at low x - the RIGHT one. */
const STANDARD_RIGHT = {
  from: [0, 0, 5],
  to: [3, 16, 11],
  faces: {
    down: face([0, 5, 3, 11]),
    up: face([0, 5, 3, 11]),
    north: face([0, 0, 3, 16]),
    south: face([0, 0, 3, 16]),
    west: face([5, 0, 11, 16]),
    east: face([5, 0, 11, 16]),
  },
};

/**
 * The intermediate upright - the T where a post meets the rail partway along a
 * run. Dead centre of the block, so it is the same box whichever way the rail
 * runs and needs no per-facing variant. JumpBlock.post() is the twin.
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
 * The five shapes, keyed by the suffix their models carry.
 *
 * The suffix names the CONNECTIONS, not the standards: `_l` means a jump
 * continues the rail on the left, so the left standard is the one that is
 * gone. `_lr` is a bare rail mid-run, and `_lr_post` is that same rail
 * carrying an intermediate upright.
 */
const SHAPES = [
  ["", [RAIL, STANDARD_LEFT, STANDARD_RIGHT]],
  ["_l", [RAIL, STANDARD_RIGHT]],
  ["_r", [RAIL, STANDARD_LEFT]],
  ["_lr", [RAIL]],
  ["_lr_post", [RAIL, POST]],
];

/**
 * The model suffix for a set of flags.
 *
 * <p>ONE function, called from both the model loop and the blockstate loop,
 * because writing it out twice is how this shipped broken the first time:
 * composing `(left?"_l":"") + (right?"_r":"")` gives "_l_r", the models are
 * named "_lr", and the mismatch is invisible until somebody places three jumps
 * in a row and the middle one is a purple cube. Only the both-connected case
 * differs, so a row of two looks perfect. GeneratedJumps.suffix is the twin.
 *
 * `post` is honoured only mid-run, matching JumpBlock.withPost, which never
 * sets it otherwise. The blockstate still has to name every combination, so the
 * end-of-run states map to the postless model rather than to one that would
 * then have to exist.
 */
const suffixFor = (left, right, post) =>
  left && right ? (post ? "_lr_post" : "_lr") : left ? "_l" : right ? "_r" : "";

// --- templates ------------------------------------------------------------
// Five models carrying the actual boxes, which every wood then parents to with
// only its texture changed.

for (const [suffix, elements] of SHAPES) {
  const model = { textures: { particle: "#texture" }, elements };
  // Only the both-standards model is ever shown in an inventory slot, so it is
  // the only one that needs a display transform. Vanilla's fence gui pose:
  // a jump is long and low and looks like nothing at all face-on.
  if (suffix === "") {
    model.parent = "block/block";
    model.display = {
      gui: { rotation: [30, 45, 0], translation: [0, -2, 0], scale: [0.8, 0.8, 0.8] },
      head: { rotation: [0, 0, 0], translation: [0, -3, -6], scale: [1, 1, 1] },
    };
  }
  put(join(A, "models/block", `template_${STYLE}${suffix}.json`), model);
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

  for (const [suffix] of SHAPES) {
    put(join(A, "models/block", id + suffix + ".json"), {
      parent: `${NS}:block/template_${STYLE}${suffix}`,
      textures: { texture },
    });
  }

  // 4 facings x left x right x post = 32 variants
  const variants = {};
  for (const [facing, y] of Object.entries(VARIANT_ROTATION)) {
    for (const left of [false, true]) {
      for (const right of [false, true]) {
        for (const post of [false, true]) {
          const v = {
            model: `${NS}:block/${id}${suffixFor(left, right, post)}`,
            uvlock: true,
          };
          if (y !== 0) v.y = y;
          variants[`facing=${facing},left=${left},right=${right},post=${post}`] = v;
        }
      }
    }
  }
  put(join(A, "blockstates", id + ".json"), { variants });

  // The icon is the both-standards model - the only one given a gui transform
  // above, and the only one that reads as a jump rather than as a plank.
  put(join(A, "items", id + ".json"), {
    model: { type: "minecraft:model", model: `${NS}:block/${id}` },
  });

  // An ordinary single-drop table. Unlike the double gate this is ONE block,
  // so there is no partner to orphan and no half=left condition to get right.
  put(join(D, "loot_table/blocks", id + ".json"), {
    type: "minecraft:block",
    pools: [
      {
        rolls: 1,
        bonus_rolls: 0,
        entries: [{ type: "minecraft:item", name: `${NS}:${id}` }],
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
  // per hair is the other side of that: a twelve-fence course costs three
  // hairs, which is one horse's afternoon. Raw, per the rule that landed with
  // the rope's removal - no intermediate, nothing to craft first.
  put(join(D, "recipe", id + ".json"), {
    type: "minecraft:crafting_shapeless",
    category: "building",
    ingredients: [
      `minecraft:${wood}_fence`,
      `minecraft:${wood}_fence`,
      `${NS}:horse_hair`,
    ],
    result: { count: RECIPE_YIELD, id: `${NS}:${id}` },
  });
}

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
  `jumps: ${WOODS.length} woods, ${SHAPES.length} templates, ` +
    `${written} files written, ${Object.keys(lang).length} lang keys merged, ` +
    `${merged.length} values in mineable/axe`
);
