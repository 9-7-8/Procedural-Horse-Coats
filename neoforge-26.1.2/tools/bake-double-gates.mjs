#!/usr/bin/env node
// Generate every asset and data file for the double-wide fence gates.
//
// WHY THIS EXISTS
// Twelve woods is twelve blocks, and each one needs a blockstate of 32 variants,
// eight models, an item definition, a recipe and a loot table. That is over a
// hundred and fifty files whose only difference is a wood name and a plank
// texture. Writing them by hand is not a job anybody does correctly twice, and a
// single wrong texture id fails silently as a purple chequerboard on one gate
// nobody happens to craft.
//
// So the woods are a table here and the files are derived from it. Re-run it
// after changing WOODS or any of the geometry below; see CLAUDE.md's regenerate
// table. It only ever WRITES - it does not delete, so removing a wood means
// deleting that wood's files by hand as well.
//
//   node neoforge-26.1.2/tools/bake-double-gates.mjs
//
// THERE WAS A SECOND STYLE, AND IT WAS DROPPED
// A "double farm gate" shipped alongside this for a few hours: the same block
// with four rails instead of two. It existed because the mod that prompted all
// of this advertised "farm-style variant gates" - four words, no screenshot, no
// source, Fabric-only - so its appearance was guessed rather than reproduced.
// The owner looked at the result ("what is the double farm gate? It looks
// weird") and had it removed. Do not re-add a second style on the same basis:
// two gates whose difference nobody can justify is worse than one gate.
//
// THE ONE IRREGULAR WOOD
// Eleven of the twelve texture their gate with <wood>_planks. Bamboo does not -
// vanilla gives it a dedicated block/bamboo_fence_gate texture - so the table
// carries an explicit texture per wood rather than composing the name. That
// sheet is drawn for VANILLA's geometry, whose rails sit at x 2-6 and 10-14,
// and these run one rail the full 2-14, so sampling it comes out visibly
// scrambled: bamboo uses its planks here. Owner, in play: "like the texture was
// moved around wrong on the model". The wood list itself was read off the
// 26.1.2 client jar (assets/minecraft/blockstates/*_fence_gate.json), so it is
// this version's roster and not a remembered one.
//
// THE GEOMETRY, AND WHY IT IS NOT A PARENT OF THE VANILLA TEMPLATE
// A vanilla gate draws a post at BOTH ends, x 0-2 and x 14-16. Two of them side
// by side therefore show a double post down the middle, which is exactly what a
// double gate is supposed not to look like. Each half here keeps its own OUTER
// post, drops the inner one, and runs its rails the full way to the block
// boundary, so the pair reads as one opening with a post at each end and the two
// leaves meeting in the middle.

import { mkdirSync, writeFileSync, readFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..");
const NS = "horsegenetics";

const A = join(root, "src/main/resources/assets", NS);
const D = join(root, "src/main/resources/data", NS);
const MC_TAGS = join(root, "src/main/resources/data/minecraft/tags/block");

/** name -> the texture vanilla's own fence gate of that wood uses, and a label. */
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
  // PLANKS, not vanilla's bespoke block/bamboo_fence_gate sheet - see the header.
  ["bamboo", "minecraft:block/bamboo_planks", "Bamboo"],
  ["crimson", "minecraft:block/crimson_planks", "Crimson"],
  ["warped", "minecraft:block/warped_planks", "Warped"],
];

/** The suffix every id and model carries. One style, deliberately - see the header. */
const STYLE = "double_fence_gate";
const STYLE_LABEL = "Double Fence Gate";

const HALVES = ["left", "right"];

let written = 0;
function put(path, value) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, JSON.stringify(value, null, 2) + "\n", "utf8");
  written++;
}

// --- geometry -------------------------------------------------------------
//
// All boxes are written for the LEFT half - outer post at low x, leaf reaching
// toward high x, partner beyond x=16 - and mirrored for the right half by
// x -> 16-x. `lift` is 0 for a free-standing gate and -3 for one in a wall,
// which is the offset vanilla uses between its two template pairs.

const face = (uv) => ({ uv, texture: "#texture" });

function box(from, to, faces) {
  return { from, to, faces };
}

/** The two rails of a shut leaf, as [bottom, top] pairs in model space. */
const RAILS = [[6, 9], [12, 15]];

/** Post, rails and leaf-edge for a shut half. */
function closedElements(lift) {
  const y = (v) => v + lift;
  const els = [
    // Outer hinge post - the one at the end of the whole opening.
    box([0, y(5), 7], [2, y(16), 9], {
      down: face([0, 7, 2, 9]),
      up: face([0, 7, 2, 9]),
      north: face([0, 0, 2, 11]),
      south: face([0, 0, 2, 11]),
      west: { ...face([7, 0, 9, 11]), cullface: "west" },
      east: face([7, 0, 9, 11]),
    }),
    // Leaf edge, where this half meets its partner at the block boundary.
    box([14, y(6), 7], [16, y(15), 9], {
      down: face([14, 7, 16, 9]),
      up: face([14, 7, 16, 9]),
      north: face([14, 1, 16, 10]),
      south: face([14, 1, 16, 10]),
      west: face([7, 1, 9, 10]),
      east: face([7, 1, 9, 10]),
    }),
  ];
  for (const [lo, hi] of RAILS) {
    els.push(
      box([2, y(lo), 7], [14, y(hi), 9], {
        down: face([2, 7, 14, 9]),
        up: face([2, 7, 14, 9]),
        north: face([2, 16 - hi, 14, 16 - lo]),
        south: face([2, 16 - hi, 14, 16 - lo]),
      })
    );
  }
  return els;
}

/**
 * An open half. The leaf has swung back against its own outer post and lies
 * along z, which is vanilla's own open pose - so this half is geometrically
 * vanilla's left-hand leaf, and nothing needed inventing.
 */
function openElements(lift) {
  const y = (v) => v + lift;
  const els = [
    box([0, y(5), 7], [2, y(16), 9], {
      down: face([0, 7, 2, 9]),
      up: face([0, 7, 2, 9]),
      north: face([0, 0, 2, 11]),
      south: face([0, 0, 2, 11]),
      west: { ...face([7, 0, 9, 11]), cullface: "west" },
      east: face([7, 0, 9, 11]),
    }),
    box([0, y(6), 13], [2, y(15), 15], {
      down: face([0, 13, 2, 15]),
      up: face([0, 13, 2, 15]),
      north: face([0, 1, 2, 10]),
      south: face([0, 1, 2, 10]),
      west: face([13, 1, 15, 10]),
      east: face([13, 1, 15, 10]),
    }),
  ];
  for (const [lo, hi] of RAILS) {
    els.push(
      box([0, y(lo), 9], [2, y(hi), 13], {
        down: face([0, 9, 2, 13]),
        up: face([0, 9, 2, 13]),
        west: face([13, 16 - hi, 15, 16 - lo]),
        east: face([13, 16 - hi, 15, 16 - lo]),
      })
    );
  }
  return els;
}

/** Mirror a set of elements through the block's x centre, for the right half. */
function mirrorX(elements) {
  const flip = { west: "east", east: "west" };
  return elements.map((el) => {
    const faces = {};
    for (const [name, f] of Object.entries(el.faces)) {
      const target = flip[name] ?? name;
      const [u0, v0, u1, v1] = f.uv;
      const uv =
        name === "up" || name === "down" || name === "north" || name === "south"
          ? [16 - u1, v0, 16 - u0, v1]
          : [u0, v0, u1, v1];
      const moved = { ...f, uv };
      if (moved.cullface === "west") moved.cullface = "east";
      else if (moved.cullface === "east") moved.cullface = "west";
      faces[target] = moved;
    }
    return {
      from: [16 - el.to[0], el.from[1], el.from[2]],
      to: [16 - el.from[0], el.to[1], el.to[2]],
      faces,
    };
  });
}

// --- templates ------------------------------------------------------------
// One per half x open x in_wall: eight models carrying the actual boxes, which
// every wood then parents to with only its texture changed.

const templateNames = [];
for (const half of HALVES) {
  for (const open of [false, true]) {
    for (const inWall of [false, true]) {
      const lift = inWall ? -3 : 0;
      let els = open ? openElements(lift) : closedElements(lift);
      if (half === "right") els = mirrorX(els);
      const name = `template_${STYLE}_${half}` + (inWall ? "_wall" : "") + (open ? "_open" : "");
      templateNames.push(name);
      const model = { textures: { particle: "#texture" }, elements: els };
      // Only the plain shut model is ever shown in an inventory slot, so it is
      // the only one that needs vanilla's gate display transform.
      if (!open && !inWall) {
        model.parent = "block/block";
        model.display = {
          gui: { rotation: [30, 45, 0], translation: [0, -1, 0], scale: [0.8, 0.8, 0.8] },
          head: { rotation: [0, 0, 0], translation: [0, -3, -6], scale: [1, 1, 1] },
        };
      }
      put(join(A, "models/block", name + ".json"), model);
    }
  }
}

// --- per-wood models, blockstates, items, recipes, loot --------------------

const VARIANT_ROTATION = { south: 0, west: 90, north: 180, east: 270 };
const lang = {};
const tagValues = [];

for (const [wood, texture, woodLabel] of WOODS) {
  const id = `${wood}_${STYLE}`;
  tagValues.push(`${NS}:${id}`);
  lang[`block.${NS}.${id}`] = `${woodLabel} ${STYLE_LABEL}`;

  // eight models, each one line of difference from its template
  for (const half of HALVES) {
    for (const open of [false, true]) {
      for (const inWall of [false, true]) {
        const suffix = `_${half}` + (inWall ? "_wall" : "") + (open ? "_open" : "");
        put(join(A, "models/block", id + suffix + ".json"), {
          parent: `${NS}:block/template_${STYLE}${suffix}`,
          textures: { texture },
        });
      }
    }
  }

  // 4 facings x in_wall x open x half = 32 variants
  const variants = {};
  for (const [facing, y] of Object.entries(VARIANT_ROTATION)) {
    for (const inWall of [false, true]) {
      for (const open of [false, true]) {
        for (const half of HALVES) {
          const key = `facing=${facing},half=${half},in_wall=${inWall},open=${open}`;
          const suffix = `_${half}` + (inWall ? "_wall" : "") + (open ? "_open" : "");
          const v = { model: `${NS}:block/${id}${suffix}`, uvlock: true };
          if (y !== 0) v.y = y;
          variants[key] = v;
        }
      }
    }
  }
  put(join(A, "blockstates", id + ".json"), { variants });

  // THE ICON IS THE LEFT HALF, and there is no bare `<id>` model to point at:
  // a double gate is two blocks and neither of them is "the" block. Pointing an
  // item definition at a model that was never written is the silent failure this
  // whole asset layer is prone to - nothing logs, nothing goes red, and the item
  // is simply a purple chequerboard in the slot. The left half is the one given
  // vanilla's gate gui transform up in the template loop.
  put(join(A, "items", id + ".json"), {
    model: { type: "minecraft:model", model: `${NS}:block/${id}_left` },
  });

  // ONE ITEM OUT, AND THE half=left CONDITION IS THE WHOLE OF IT.
  // A pair is two blocks, so breaking one runs two removals: the half the
  // player hit, and the orphan its partner becomes, which DoubleFenceGateBlock
  // .updateShape turns to air. That second removal is NOT free of drops -
  // Block.updateOrDestroy calls destroyBlock(pos, (flags & 32) == 0), and an
  // ordinary neighbour update carries no UPDATE_SUPPRESS_DROPS - so an
  // unconditional table paid out twice and a gate duplicated every time it was
  // broken. Vanilla's doors and beds solve it exactly here and not in Java:
  // only the half that carries the loot may pay, whichever half was struck.
  // Compare data/minecraft/loot_table/blocks/oak_door.json (half=lower).
  put(join(D, "loot_table/blocks", id + ".json"), {
    type: "minecraft:block",
    pools: [
      {
        rolls: 1,
        bonus_rolls: 0,
        entries: [
          {
            type: "minecraft:item",
            name: `${NS}:${id}`,
            conditions: [
              {
                condition: "minecraft:block_state_property",
                block: `${NS}:${id}`,
                properties: { half: "left" },
              },
            ],
          },
        ],
        conditions: [{ condition: "minecraft:survives_explosion" }],
      },
    ],
  });

  // THE ONE RECIPE IN THE MOD WITH NO MODDED INGREDIENT, ON PURPOSE.
  // The house rule (wiki/items.html#rules) is that every recipe carries at least
  // one modded ingredient so none of them can collide with a vanilla or
  // third-party one. This is the documented exception. It used to take a braided
  // rope for the hinge, which priced a paddock's worth of gates at twelve horse
  // hairs each - and hair is sheared one to three at a time, once per horse per
  // day. Owner: make the gate not want hair at all. Two gates of the wood, and
  // that is the whole recipe.
  //
  // WHY THAT IS JUDGED SAFE: nothing in vanilla consumes a fence gate - every
  // vanilla recipe mentioning one produces it - so there is no collision to have
  // today. The residual risk is a third-party mod shipping its own two-gate
  // shapeless recipe, which would be ambiguous with this one. check-recipes.mjs
  // reads only this mod's folder and cannot see that; the rule was the guard and
  // this recipe is outside it. If a pack ever reports a gate that will not craft,
  // look here first.
  put(join(D, "recipe", id + ".json"), {
    type: "minecraft:crafting_shapeless",
    category: "misc",
    ingredients: [
      `minecraft:${wood}_fence_gate`,
      `minecraft:${wood}_fence_gate`,
    ],
    result: { id: `${NS}:${id}` },
  });
}

// --- tags -----------------------------------------------------------------
// Joining vanilla's own tags: mineable/axe so an axe is the right tool, and
// fence_gates so anything reading that tag (ours and other mods') sees these.

for (const tag of ["mineable/axe", "fence_gates"]) {
  put(join(MC_TAGS, tag + ".json"), { values: tagValues });
}

// --- lang -----------------------------------------------------------------
// Merged into the existing file rather than replacing it: every other key in
// there is hand-written. Merging only ADDS, so a removed gate's key has to be
// taken out by hand.

const langPath = join(A, "lang/en_us.json");
const existing = JSON.parse(readFileSync(langPath, "utf8"));
for (const [k, v] of Object.entries(lang)) existing[k] = v;
writeFileSync(langPath, JSON.stringify(existing, null, 2) + "\n", "utf8");

console.log(
  `double gates: ${WOODS.length} woods, ${templateNames.length} templates, ` +
    `${written} files written, ${Object.keys(lang).length} lang keys merged`
);
