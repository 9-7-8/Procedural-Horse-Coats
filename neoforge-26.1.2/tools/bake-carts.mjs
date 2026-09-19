#!/usr/bin/env node
// Writes the item models, item definitions, recipes and lang keys for the carts
// in *vanilla's twelve woods*, into src/main/resources.
//
// THE SAME CONTRACT EXISTS TWICE, exactly as it does for the double gates:
// this tool covers vanilla at author time, and compat/GeneratedCarts.java
// covers every wood another mod adds, at run time. They must agree. A drift
// shows up as a modded cart that is a purple cube while every vanilla one is
// fine, or as a recipe that exists for oak and not for maple - and neither logs
// anything. Change one, change the other.
//
// Only ever writes. Dropping a wood or a cart type means deleting that wood's
// files and its lang keys by hand.
//
// Usage: node neoforge-26.1.2/tools/bake-carts.mjs
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const RES = path.resolve(HERE, '../src/main/resources');
const NS = 'horsegenetics';

// [name, label, log suffix]. The suffix is the irregular column - the nether
// woods are stems and bamboo is a block - and it is duplicated in CartWood.java
// for the same reason it is duplicated for the gates.
const WOODS = [
  ['oak', 'Oak', 'log'],
  ['spruce', 'Spruce', 'log'],
  ['birch', 'Birch', 'log'],
  ['jungle', 'Jungle', 'log'],
  ['acacia', 'Acacia', 'log'],
  ['dark_oak', 'Dark Oak', 'log'],
  ['pale_oak', 'Pale Oak', 'log'],
  ['mangrove', 'Mangrove', 'log'],
  ['cherry', 'Cherry', 'log'],
  ['bamboo', 'Bamboo', 'block'],
  ['crimson', 'Crimson', 'stem'],
  ['warped', 'Warped', 'stem'],
];

// Must match common/cart/CartKind. The recipes are upstream's, unchanged.
const CARTS = [
  ['wagon', 'Wagon'],
  ['plow', 'Plow'],
  ['reaper', 'Reaper'],
  ['seed_drill', 'Seed Drill'],
  ['supply_cart', 'Supply Cart'],
  ['animal_cart', 'Animal Cart'],
];

const WHEEL = `${NS}:cart_wheel`;

/** The shaped recipe for one cart in one wood. Mirrored in GeneratedCarts.recipe. */
export function recipe(cart, wood, suffix) {
  const planks = `minecraft:${wood}_planks`;
  const slab = `minecraft:${wood}_slab`;
  const stripped = `minecraft:stripped_${wood}_${suffix}`;
  switch (cart) {
    case 'supply_cart':
      return { key: { c: 'minecraft:chest', p: planks, w: WHEEL }, pattern: ['pcp', 'pcp', 'wpw'] };
    case 'plow':
      return { key: { p: planks, s: 'minecraft:stick', w: WHEEL }, pattern: ['sss', 'psp', 'wpw'] };
    case 'seed_drill':
      return { key: { c: 'minecraft:chest', h: 'minecraft:hopper', p: planks, w: WHEEL }, pattern: ['pcp', 'php', 'wpw'] };
    case 'reaper':
      // 'sl ' and not 'sl': a shaped recipe's rows must all be the same width,
      // and a short one does not fail the row - it fails the whole file, at
      // datapack load, with "Invalid pattern: each row must be the same width".
      // Twelve woods' reapers were uncraftable for a release because of the
      // missing space, and the only place it was ever said out loud was a red
      // line in the server log that nothing was reading. GeneratedCarts has the
      // same three rows for modded woods - change both.
      return { key: { i: 'minecraft:iron_ingot', l: slab, p: planks, s: 'minecraft:stick', w: WHEEL }, pattern: ['sl ', 'spp', 'iww'] };
    case 'animal_cart':
      return { key: { p: planks, w: WHEEL }, pattern: ['ppp', 'ppp', 'wpw'] };
    case 'wagon':
      return { key: { l: stripped, p: planks, w: WHEEL }, pattern: ['lll', 'wpw', 'wpw'] };
    default:
      throw new Error('no recipe for ' + cart);
  }
}

function write(rel, json) {
  const out = path.join(RES, rel);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(json, null, 2) + '\n');
  written++;
}

let written = 0;
const lang = {};

// --- the wheel ------------------------------------------------------------
write(`assets/${NS}/models/item/cart_wheel.json`,
  { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/cart_wheel` } });
write(`assets/${NS}/items/cart_wheel.json`,
  { model: { type: 'minecraft:model', model: `${NS}:item/cart_wheel` } });
write(`data/${NS}/recipe/cart_wheel.json`, {
  type: 'minecraft:crafting_shaped',
  category: 'misc',
  key: { p: '#minecraft:planks', s: 'minecraft:stick' },
  pattern: ['sss', 'sps', 'sss'],
  result: { count: 1, id: WHEEL },
});
lang[`item.${NS}.cart_wheel`] = 'Cart Wheel';

// --- every cart in every vanilla wood -------------------------------------
for (const [wood, woodLabel, suffix] of WOODS) {
  for (const [cart, cartLabel] of CARTS) {
    const id = `${wood}_${cart}`;
    write(`assets/${NS}/models/item/${id}.json`,
      { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/${id}` } });
    write(`assets/${NS}/items/${id}.json`,
      { model: { type: 'minecraft:model', model: `${NS}:item/${id}` } });
    const r = recipe(cart, wood, suffix);
    write(`data/${NS}/recipe/${id}.json`, {
      type: 'minecraft:crafting_shaped',
      category: 'misc',
      key: r.key,
      pattern: r.pattern,
      result: { count: 1, id: `${NS}:${id}` },
    });
    lang[`item.${NS}.${id}`] = `${woodLabel} ${cartLabel}`;
  }
}

// --- the keys that are not per-wood ---------------------------------------
Object.assign(lang, {
  [`itemGroup.${NS}.carts`]: 'Horse Carts',
  [`key.category.${NS}.carts`]: 'Horse Carts',
  [`key.${NS}.cart_action`]: 'Attach/Detach Cart',
  [`message.${NS}.use_reaper`]: 'Sit on the reaper to use it',
  'item.cart.press_shift_tooltip': 'Hold [Shift] to show details',
  [`item.${NS}.cart.draught`]: 'Empty, an ordinary horse keeps %s%% of its speed',
  [`item.${NS}.cart.cargo`]: 'Full, it keeps %s%%',
  [`subtitles.${NS}.cart.attached`]: 'Cart attaches',
  [`subtitles.${NS}.cart.detached`]: 'Cart detaches',
  [`subtitles.${NS}.cart.placed`]: 'Cart placed',
  [`entity.${NS}.wagon`]: 'Wagon',
  [`entity.${NS}.plow`]: 'Plow',
  [`entity.${NS}.reaper`]: 'Reaper',
  [`entity.${NS}.seed_drill`]: 'Seed Drill',
  [`entity.${NS}.supply_cart`]: 'Supply Cart',
  [`entity.${NS}.animal_cart`]: 'Animal Cart',
  [`item.${NS}.wagon.tooltip1`]: 'Seats four; chests add storage and cost seats, five carpets make a roof',
  [`item.${NS}.wagon.tooltip2`]: 'A passenger on the box seat drives the horse',
  [`item.${NS}.plow.tooltip1`]: 'Tills the ground, makes dirt paths, or strips logs as the horse walks',
  [`item.${NS}.plow.tooltip2`]: 'Needs the matching tool inside; right-click to toggle',
  [`item.${NS}.reaper.tooltip1`]: 'Harvests mature crops it is drawn over',
  [`item.${NS}.reaper.tooltip2`]: 'Only works while a player is sitting on it',
  [`item.${NS}.seed_drill.tooltip1`]: 'Plants seeds on farmland it is drawn over',
  [`item.${NS}.seed_drill.tooltip2`]: 'Holds nine stacks of seeds',
  [`item.${NS}.supply_cart.tooltip1`]: 'Holds 54 stacks, and shows what is in it',
  [`item.${NS}.supply_cart.tooltip2`]: 'One seat, a banner, and the fuller it is the harder it pulls',
  [`item.${NS}.animal_cart.tooltip1`]: 'Two seats for players or animals, and it can fly a banner',
  [`item.${NS}.animal_cart.tooltip2`]: 'Small animals climb in by themselves, and their weight is felt',
  [`stat.${NS}.ride_cart_cm`]: 'Distance by Cart',
  [`stat.${NS}.steer_animal_cart_cm`]: 'Distance steering Animal Cart',
  [`stat.${NS}.steer_reaper_cm`]: 'Distance steering Reaper',
  [`stat.${NS}.steer_wagon_cm`]: 'Distance steering Wagon',
});
for (const [cart, cartLabel] of CARTS) {
  lang[`stat.${NS}.${cart}_pull_cm`] = `Distance pulling ${cartLabel}`;
}

// --- merge the lang keys, never replace the file --------------------------
const langPath = path.join(RES, `assets/${NS}/lang/en_us.json`);
const existing = fs.existsSync(langPath) ? JSON.parse(fs.readFileSync(langPath, 'utf8')) : {};
const added = Object.keys(lang).filter(k => !(k in existing)).length;
const merged = { ...existing, ...lang };
const sorted = Object.fromEntries(Object.keys(merged).sort().map(k => [k, merged[k]]));
fs.mkdirSync(path.dirname(langPath), { recursive: true });
fs.writeFileSync(langPath, JSON.stringify(sorted, null, 2) + '\n');

console.log(`bake-carts: ${written} files, ${Object.keys(lang).length} lang keys (${added} new)`);
console.log(`            ${WOODS.length} woods x ${CARTS.length} carts, plus the wheel`);
