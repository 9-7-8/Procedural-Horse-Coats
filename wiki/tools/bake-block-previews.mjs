// Turn a block's shipped model into something the wiki can spin.
//
//   node wiki/tools/bake-block-previews.mjs
//
// Owner, 2026-09-29: item pages should show the item, and where the item is a
// block, that means a 3D preview you can rotate - "only update the preview for
// pages which no longer re-use unchanged minecraft textures. We'll update each
// page as we go."
//
// THAT RULE IS ENFORCED HERE RATHER THAN REMEMBERED. Nearly every 3D block in
// this mod is dressed in vanilla's art - the gates and jumps are plank textures,
// the bench is a smithing table, the shelf a bookshelf, the stasis bank a barrel.
// Previewing those on the wiki would mean committing Mojang's PNGs to this repo,
// which is not ours to do. So the resolver below HARD-FAILS on a `minecraft:`
// texture: a block joins the previews on the day it gets art of its own, and not
// before, and nobody has to remember the rule to keep it.
//
// WHAT IT WRITES, to wiki/assets/block-preview/:
//   shapes.js    every resolved shape - six faces each, naming a texture
//   <name>.png   a copy of each texture the shapes reference
// The page side is `wiki/block-preview.js`, which is handed a list of ids by a
// `data-blocks` attribute and draws them in one orbitable scene.
//
// WHY A SCRIPT AND NOT JSON. Wiki pages have to work opened straight off the
// disk - that is the constraint `wiki/tools/chrome.mjs` tests against, and the
// gene creator's whole design. Over file:// a browser refuses `fetch` of a
// neighbouring JSON as a cross-origin read, so a .json here would work on a
// server and quietly break for anyone who cloned the repo and double-clicked the
// page. A <script> tag has no such rule, and an <img> (which is how the textures
// load) never did either.
//
// ON RESOLVING PARENTS. Every block that qualifies today is a full cube, so the
// resolver understands vanilla's cube family and nothing else. An unknown parent
// is an ERROR naming the parent, not a cube drawn in hope: the first block with a
// real shape must make somebody extend this, because a jump silently rendered as
// a box would be a worse page than no preview at all.
import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '../..');
const MODELS = path.join(ROOT, 'neoforge-26.1.2/src/main/resources/assets/horsegenetics/models/block');
const TEXTURES = path.join(ROOT, 'neoforge-26.1.2/src/main/resources/assets/horsegenetics/textures/block');
const OUT = path.join(ROOT, 'wiki/assets/block-preview');
const WIKI = path.join(ROOT, 'wiki');

/** Vanilla's cube family, as face -> texture-slot. The only shapes understood. */
const CUBES = {
  'minecraft:block/cube_all': { up: 'all', down: 'all', north: 'all', south: 'all', east: 'all', west: 'all' },
  'minecraft:block/cube_bottom_top': { up: 'top', down: 'bottom', north: 'side', south: 'side', east: 'side', west: 'side' },
  'minecraft:block/cube_column': { up: 'end', down: 'end', north: 'side', south: 'side', east: 'side', west: 'side' },
};

const errors = [];

function resolve(id) {
  const file = path.join(MODELS, `${id}.json`);
  if (!fs.existsSync(file)) { errors.push(`${id}: no model at models/block/${id}.json`); return null; }
  const model = JSON.parse(fs.readFileSync(file, 'utf8'));
  const faces = CUBES[model.parent];
  if (!faces) {
    errors.push(`${id}: parent "${model.parent}" is not a shape this tool can resolve. `
      + 'It only knows vanilla\'s cube family. Teach it the real geometry rather than '
      + 'letting a shaped block be drawn as a box.');
    return null;
  }
  const out = {};
  for (const [face, slot] of Object.entries(faces)) {
    const ref = model.textures?.[slot];
    if (!ref) { errors.push(`${id}: model has no texture for slot "${slot}"`); return null; }
    if (!ref.startsWith('horsegenetics:block/')) {
      errors.push(`${id}: face "${face}" uses ${ref}. A preview may only use this mod's own `
        + 'art - see the header. This block needs a texture of its own before it gets a preview.');
      return null;
    }
    out[face] = ref.slice('horsegenetics:block/'.length);
  }
  return { id, faces: out };
}

/** Every `data-blocks` on every wiki page, so the bake covers exactly what is asked for. */
function wanted() {
  const ids = new Map();
  for (const f of fs.readdirSync(WIKI).filter(f => f.endsWith('.html'))) {
    const html = fs.readFileSync(path.join(WIKI, f), 'utf8');
    for (const m of html.matchAll(/data-blocks="([^"]+)"/g)) {
      for (const id of m[1].split(',').map(s => s.trim()).filter(Boolean)) {
        if (!ids.has(id)) ids.set(id, []);
        ids.get(id).push(f);
      }
    }
  }
  return ids;
}

const ask = wanted();
if (ask.size === 0) {
  console.log('block previews: no page carries a data-blocks attribute, nothing to bake');
  process.exit(0);
}

fs.mkdirSync(OUT, { recursive: true });
const shapes = [];
for (const id of [...ask.keys()].sort()) {
  const shape = resolve(id);
  if (shape) shapes.push(shape);
}

if (errors.length) {
  console.error(`block previews: ${errors.length} problem(s)\n`);
  for (const e of errors) console.error('  ' + e);
  process.exit(1);
}

// Copy exactly the textures the shapes name, and no others.
const needed = new Set(shapes.flatMap(s => Object.values(s.faces)));
for (const name of needed) {
  const from = path.join(TEXTURES, `${name}.png`);
  if (!fs.existsSync(from)) { console.error(`block previews: no texture at ${from}`); process.exit(1); }
  fs.copyFileSync(from, path.join(OUT, `${name}.png`));
}

// Drop copies of textures no shape names any more, so a removed preview does not
// leave its art behind in the wiki for ever.
for (const f of fs.readdirSync(OUT)) {
  if (f === 'shapes.js') continue;
  if (!needed.has(f.replace(/\.png$/, ''))) fs.unlinkSync(path.join(OUT, f));
}

const table = {};
for (const s of shapes) table[s.id] = s.faces;
fs.writeFileSync(path.join(OUT, 'shapes.js'),
  '// Generated by wiki/tools/bake-block-previews.mjs - do not edit.\n'
  + '// Read by wiki/block-preview.js. A script rather than JSON so the page works\n'
  + '// opened straight off the disk, where fetch() of a sibling file is refused.\n'
  + 'window.HG_BLOCK_SHAPES = ' + JSON.stringify(table, null, 2) + ';\n');

console.log(`block previews: ${shapes.length} block(s), ${needed.size} texture(s) -> wiki/assets/block-preview/`);
for (const [id, pages] of [...ask].sort()) console.log(`  ${id}  on ${pages.join(', ')}`);
