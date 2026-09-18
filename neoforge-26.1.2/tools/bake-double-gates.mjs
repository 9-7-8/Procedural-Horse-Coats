#!/usr/bin/env node
// Generate every asset and data file for the double-wide fence gates.
//
// WHY THIS EXISTS
// Twelve woods times two styles is twenty-four blocks, and each one needs a
// blockstate of 32 variants, eight models, an item definition, a recipe and a
// loot table. That is well over three hundred files whose only difference is a
// wood name and a plank texture. Writing them by hand is not a job anybody does
// correctly twice, and a single wrong texture id fails silently as a purple
// chequerboard on one gate nobody happens to craft.
//
// So the woods are a table here and the files are derived from it. Re-run it
// after changing WOODS, STYLES or any of the geometry below; see CLAUDE.md's
// regenerate table.
//
//   node neoforge-26.1.2/tools/bake-double-gates.mjs
//
// THE ONE IRREGULAR WOOD
// Eleven of the twelve texture their gate with <wood>_planks. Bamboo does not -
// vanilla gives it a dedicated block/bamboo_fence_gate texture - so the table
// carries an explicit texture per wood rather than composing the name. Checked
// against the 26.1.2 client jar, which is also where the wood list came from
// (assets/minecraft/blockstates/*_fence_gate.json), so it is this version's
// roster and not a remembered one.
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

/** name -> the texture vanilla's own fence gate of that wood uses. */
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
  // PLANKS, even though vanilla's bamboo gate uses a bespoke block/bamboo_fence_gate
  // sheet. That sheet is drawn for vanilla's GEOMETRY - its rails sit at x 2-6 and
  // 10-14 - and these gates run one rail the full 2-14 instead, so sampling it lands
  // on the wrong regions and the gate comes out visibly scrambled (owner, in play:
  // "like the texture was moved around wrong on the model"). A planks texture tiles
  // and has no such alignment to lose, which is why the other eleven are fine.
  ["bamboo", "minecraft:block/bamboo_planks", "Bamboo"],
  ["crimson", "minecraft:block/crimson_planks", "Crimson"],
  ["warped", "minecraft:block/warped_planks", "Warped"],
];

const STYLES = [
  { key: "double_fence_gate", label: "Double Fence Gate" },
  { key: "double_farm_gate", label: "Double Farm Gate" },
];

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

/** Post, rails and leaf-edge for a shut half. */
function closedElements(style, lift) {
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
  // The rails. Two for a fence gate, four for a farm gate - which is the whole
  // visible difference between the styles.
  const rails =
    style === "double_farm_gate"
      ? [[6, 8], [9, 11], [12, 14], [3, 5]]
      : [[6, 9], [12, 15]];
  for (const [lo, hi] of rails) {
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
function openElements(style, lift) {
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
  const rails =
    style === "double_farm_gate"
      ? [[6, 8], [9, 11], [12, 14], [3, 5]]
      : [[6, 9], [12, 15]];
  for (const [lo, hi] of rails) {
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
      const uv = name === "up" || name === "down" || name === "north" || name === "south"
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
// One per style x half x open x in_wall: sixteen models carrying the actual
// boxes, which every wood then parents to with only its texture changed.

const templateNames = [];
for (const style of STYLES) {
  for (const half of HALVES) {
    for (const open of [false, true]) {
      for (const inWall of [false, true]) {
        const lift = inWall ? -3 : 0;
        let els = open ? openElements(style.key, lift) : closedElements(style.key, lift);
        if (half === "right") els = mirrorX(els);
        const name =
          `template_${style.key}_${half}` + (inWall ? "_wall" : "") + (open ? "_open" : "");
        templateNames.push(name);
        const model = {
          textures: { particle: "#texture" },
          elements: els,
        };
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
}

// --- per-wood models, blockstates, items, recipes, loot --------------------

const VARIANT_ROTATION = { south: 0, west: 90, north: 180, east: 270 };
const lang = {};
const tagValues = [];

for (const [wood, texture, woodLabel] of WOODS) {
  for (const style of STYLES) {
    const id = `${wood}_${style.key}`;
    tagValues.push(`${NS}:${id}`);
    lang[`block.${NS}.${id}`] = `${woodLabel} ${style.label}`;

    // eight models, each one line of difference from its template
    for (const half of HALVES) {
      for (const open of [false, true]) {
        for (const inWall of [false, true]) {
          const suffix = `_${half}` + (inWall ? "_wall" : "") + (open ? "_open" : "");
          put(join(A, "models/block", id + suffix + ".json"), {
            parent: `${NS}:block/template_${style.key}${suffix}`,
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
            const key =
              `facing=${facing},half=${half},in_wall=${inWall},open=${open}`;
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
    // whole mod's asset layer is prone to - nothing logs, nothing goes red, and the
    // item is simply a purple chequerboard in the slot. The left half is the one
    // given vanilla's gate gui transform up in the template loop, so it is the half
    // that poses correctly in an inventory.
    put(join(A, "items", id + ".json"), {
      model: { type: "minecraft:model", model: `${NS}:block/${id}_left` },
    });

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

    // HOUSE RULE (wiki/items.html#rules): every recipe carries at least one
    // modded ingredient and every output is a modded item, so none of these can
    // collide with a vanilla or third-party recipe. check-recipes.mjs only reads
    // this mod's own folder, so it could not catch such a collision - the rule is
    // the guard, not the checker. Two vanilla gates of the wood plus rope for the
    // hinge; the farm gate takes hair cloth on top, which also keeps the two
    // styles' sorted ingredient lists distinct.
    const ingredients = [
      `minecraft:${wood}_fence_gate`,
      `minecraft:${wood}_fence_gate`,
      `${NS}:braided_rope`,
    ];
    if (style.key === "double_farm_gate") ingredients.push(`${NS}:hair_cloth`);
    put(join(D, "recipe", id + ".json"), {
      type: "minecraft:crafting_shapeless",
      category: "misc",
      ingredients,
      result: { id: `${NS}:${id}` },
    });
  }
}

// --- tags -----------------------------------------------------------------
// Joining vanilla's own tags: mineable/axe so an axe is the right tool, and
// fence_gates so anything reading that tag (ours and other mods') sees these.

for (const tag of ["mineable/axe", "fence_gates"]) {
  put(join(MC_TAGS, tag + ".json"), { values: tagValues });
}

// --- lang -----------------------------------------------------------------
// Merged into the existing file rather than replacing it: every other key in
// there is hand-written.

const langPath = join(A, "lang/en_us.json");
const existing = JSON.parse(readFileSync(langPath, "utf8"));
for (const [k, v] of Object.entries(lang)) existing[k] = v;
writeFileSync(langPath, JSON.stringify(existing, null, 2) + "\n", "utf8");

console.log(
  `double gates: ${WOODS.length} woods x ${STYLES.length} styles = ` +
    `${WOODS.length * STYLES.length} blocks, ${templateNames.length} templates, ` +
    `${written} files written, ${Object.keys(lang).length} lang keys merged`
);
