// Put the actual sprite on the page that describes it.
//
//   node wiki/tools/bake-item-art.mjs
//
// Owner, 2026-09-29: "update the wiki, so every item page on a wiki has that
// item". A page that spends two thousand words on four whistles and never shows
// you one of them is asking the reader to picture it.
//
// A page asks for its items with one attribute and nothing else:
//
//   <div class="item-art" data-items="blank_ticket,basic_ticket"></div>
//
// Those are ITEM ids - the names of models under models/item/ - not texture
// names, and the difference is not pedantry: the blank ticket's sprite file is
// called horse_ticket.png and the basic whistle's is horse_whistle.png. Keying on
// the item means the caption can come from the mod's own lang file, so a renamed
// item renames its caption on the next bake and nobody retypes anything.
//
// THE VANILLA RULE, again. The owner's rule for the 3D previews next door - only
// show what no longer re-uses unchanged Minecraft textures - applies here for the
// same reason: the wiki may not ship Mojang's PNGs. So an item whose model points
// layer0 at `minecraft:` is refused by name. Three do today (the breed spawn egg
// is vanilla's horse egg, the freedom stick is a stick and a feather, and golden
// carrot seeds are wheat seeds), and each will become showable on the day it is
// drawn.
//
// WHY COPIES AND NOT LINKS INTO THE MODULE. The wiki is opened off the disk and
// also served as a site; a ../../neoforge-26.1.2/... src would work in one and
// not the other, and would put the site's assets outside the folder it publishes.
//
// IT FAILS ON AN ITEM THAT NO PAGE CLAIMS. That is the point of running it: a new
// item is easy to add and easy to forget to write up, and a roster where every
// item is somewhere is the only version of "every item page has that item" that
// stays true.
import { readPng, writePng } from '../../neoforge-26.1.2/tools/png.mjs';
import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '../..');
const ASSETS = path.join(ROOT, 'neoforge-26.1.2/src/main/resources/assets/horsegenetics');
const MODELS = path.join(ASSETS, 'models/item');
const SPRITES = path.join(ASSETS, 'textures/item');
const OUT = path.join(ROOT, 'wiki/assets/item-art');
const WIKI = path.join(ROOT, 'wiki');

const WOOD = /^(acacia|bamboo|birch|cherry|crimson|dark_oak|jungle|mangrove|oak|pale_oak|spruce|warped)_/;

/**
 * Sprites that are nobody's item model, and what to call them. Only for art that
 * a RUNTIME-generated item wears - there is no models/item/ file to read a name
 * from, because the model is written on the fly.
 */
const NOT_AN_ITEM = {
  plate_horse_armor: {
    name: 'Plate Horse Armour',
    why: 'the base sprite compat/GeneratedArmour tints per metal, for armour made of '
       + 'another mod\'s ingot. No model file exists until the game writes one.',
  },
};

/** Item ids deliberately on no page, and why. Anything else uncovered is an error. */
const NOT_ON_A_PAGE = [
  [WOOD, 'the per-wood cart and implement families. carts.html shows one wood of each kind; '
       + 'twelve rows of one drawing in twelve browns would teach nobody anything.'],
];

const lang = JSON.parse(fs.readFileSync(path.join(ASSETS, 'lang/en_us.json'), 'utf8'));
const errors = [];

/** Every item model id, and the sprite it wears - or why it cannot be shown. */
function items() {
  const out = new Map();
  for (const f of fs.readdirSync(MODELS).filter(f => f.endsWith('.json'))) {
    const id = f.slice(0, -5);
    const model = JSON.parse(fs.readFileSync(path.join(MODELS, f), 'utf8'));
    // layer0 is the flat sprite. An item drawn by a renderer rather than a sprite
    // has none - the signed transfer paper is drawn as the horse it names, by
    // client/TransferDeedRenderer - and for those the `particle` texture IS the
    // item's own art, which is what the game falls back to as well.
    const ref = model.textures?.layer0 || model.textures?.particle;
    if (!ref) { out.set(id, { sprite: null }); continue; }
    if (!ref.startsWith('horsegenetics:item/')) { out.set(id, { vanilla: ref }); continue; }
    // A layered item (the storage harness: dyed leather under metal fittings) is
    // not any one of its files. Its upper layers are kept so the bake can put the
    // picture together the way the game does.
    const over = [];
    for (let i = 1; model.textures[`layer${i}`]; i++) {
      const upper = model.textures[`layer${i}`];
      if (upper.startsWith('horsegenetics:item/')) over.push(upper.slice('horsegenetics:item/'.length));
    }
    out.set(id, { sprite: ref.slice('horsegenetics:item/'.length), over });
  }
  return out;
}

/** Every `data-items` on every wiki page. */
function asked() {
  const map = new Map();
  for (const f of fs.readdirSync(WIKI).filter(f => f.endsWith('.html'))) {
    const html = fs.readFileSync(path.join(WIKI, f), 'utf8');
    for (const m of html.matchAll(/data-items="([^"]+)"/g)) {
      for (const id of m[1].split(',').map(s => s.trim()).filter(Boolean)) {
        if (!map.has(id)) map.set(id, []);
        map.get(id).push(f);
      }
    }
  }
  return map;
}

const roster = items();
const want = asked();

/** id -> { sprite, name } for everything a page asked for. */
const show = new Map();
for (const [id, pages] of want) {
  const override = NOT_AN_ITEM[id];
  if (override) { show.set(id, { sprite: id, name: override.name }); continue; }
  const item = roster.get(id);
  if (!item) { errors.push(`${pages.join(', ')}: asks for "${id}", which is not an item model`); continue; }
  if (item.vanilla) {
    errors.push(`${pages.join(', ')}: asks for "${id}", which still wears vanilla art `
      + `(${item.vanilla}). The wiki may not ship Mojang's textures - draw it first.`);
    continue;
  }
  if (!item.sprite) {
    errors.push(`${pages.join(', ')}: asks for "${id}", whose model names no texture at all`);
    continue;
  }
  const name = lang[`item.horsegenetics.${id}`] || lang[`block.horsegenetics.${id}`];
  if (!name) { errors.push(`${id}: no lang key, so it has no name in game either`); continue; }
  show.set(id, { sprite: item.sprite, over: item.over || [], name });
}

// Everything shippable that nobody put on a page.
for (const [id, item] of roster) {
  if (want.has(id) || item.vanilla || !item.sprite) continue;
  if (NOT_ON_A_PAGE.some(([re]) => re.test(id))) continue;
  errors.push(`${id}: drawn, shipped, and on no wiki page. Add it to a page's data-items, `
    + 'or say in NOT_ON_A_PAGE why it has none.');
}

if (errors.length) {
  console.error(`item art: ${errors.length} problem(s)\n`);
  for (const e of errors) console.error('  ' + e);
  process.exit(1);
}

fs.mkdirSync(OUT, { recursive: true });
for (const [id, { sprite, over }] of show) {
  if (!over || !over.length) {
    fs.copyFileSync(path.join(SPRITES, `${sprite}.png`), path.join(OUT, `${id}.png`));
    continue;
  }
  // Layered: layer0 in its default dye (items/<id>.json, tints[0]), the upper
  // layers over it untinted - what the item looks like in a hand, undyed.
  const base = readPng(path.join(SPRITES, `${sprite}.png`));
  const def = JSON.parse(fs.readFileSync(path.join(ASSETS, 'items', `${id}.json`), 'utf8'));
  const dye = def.model?.tints?.[0]?.default;
  const px = Buffer.from(base.px);
  if (typeof dye === 'number') {
    const rgb = [(dye >> 16) & 255, (dye >> 8) & 255, dye & 255];
    for (let i = 0; i < px.length; i += 4) {
      for (let c = 0; c < 3; c++) px[i + c] = Math.round(px[i + c] * rgb[c] / 255);
    }
  }
  for (const upper of over) {
    const top = readPng(path.join(SPRITES, `${upper}.png`));
    if (top.w !== base.w || top.h !== base.h) throw new Error(`${id}: layer ${upper} is a different size`);
    for (let i = 0; i < px.length; i += 4) {
      if (top.px[i + 3] > 0) top.px.copy(px, i, i, i + 4);
    }
  }
  writePng(path.join(OUT, `${id}.png`), base.w, base.h, px);
}
for (const f of fs.readdirSync(OUT)) {
  if (f !== 'names.js' && !show.has(f.replace(/\.png$/, ''))) fs.unlinkSync(path.join(OUT, f));
}

const names = {};
for (const id of [...show.keys()].sort()) names[id] = show.get(id).name;
fs.writeFileSync(path.join(OUT, 'names.js'),
  '// Generated by wiki/tools/bake-item-art.mjs - do not edit.\n'
  + '// Captions come from the mod\'s own en_us.json, so renaming an item renames these.\n'
  + '// A script rather than JSON because wiki pages must work opened off the disk.\n'
  + 'window.HG_ITEM_NAMES = ' + JSON.stringify(names, null, 2) + ';\n');

const vanilla = [...roster].filter(([, i]) => i.vanilla).map(([id]) => id);
const pages = new Set([...want.values()].flat());
console.log(`item art: ${show.size} sprite(s) across ${pages.size} page(s) -> wiki/assets/item-art/`);
console.log(`          ${vanilla.length} item(s) still wearing vanilla art, so not shown: ${vanilla.join(', ')}`);
