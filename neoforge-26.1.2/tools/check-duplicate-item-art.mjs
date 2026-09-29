// Every item in this pack should look like itself.
//
// Owner, 2026-09-29: "recolor every single item we have, other than the breed
// eggs, which is currently identical to something else within this pack or
// within minecraft".
//
// Two items drawn the same are a real cost, not a tidiness problem: a hotbar of
// five identical tickets is five items the player has to read the tooltip of,
// and an item drawn as a vanilla one is an item they will not notice they have.
// This is the check that says which are which, and it is a CHECK rather than a
// bake - nothing here writes art, because the art is hand-made and must not be
// regenerated out from under somebody.
//
//   node neoforge-26.1.2/tools/check-duplicate-item-art.mjs [--vanilla-jar <path>]
//
// Exits non-zero when any item collides. Breed eggs are exempt by the owner's
// call: they are one item parameterised by a component and are MEANT to share a
// silhouette - what tells them apart is the tint, which no static PNG carries.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { readPng, artHash, artSignature, artDistance } from './png.mjs';

const ROOT = path.resolve(import.meta.dirname, '../..');
const A = path.join(ROOT, 'neoforge-26.1.2/src/main/resources/assets/horsegenetics');
const MODELS = path.join(A, 'models/item');
const TEXTURES = path.join(A, 'textures/item');

/** Exempt, and why. Anything added here needs a reason a reader can check. */
const EXEMPT = [
  // One item, one component, one tint per breed. A shared silhouette is the
  // point; the colour is applied at render time and is in no PNG. Owner's call.
  /^breed_egg\//,
  /^breed_spawn_egg$/,
];

/**
 * The per-wood families - a wagon in each of the vanilla woods, and the same for
 * every other cart, the gates and the jumps.
 *
 * <p>Exempt from the NEAR-identical test only; two of them being byte-identical
 * would still be a real bug and is still caught. An acacia wagon and a jungle
 * wagon look alike because acacia and jungle planks look alike, and that is the
 * contract: the sprite says what the thing is made of. "Recolour it so it stands
 * out" would be a lie about the material, and there is nothing to recolour it
 * TO that some other wood does not already own.
 */
const WOOD_FAMILY = /^horsegenetics:item\/[a-z_]+_(animal_cart|plow|reaper|seed_drill|supply_cart|wagon|gate|jump)$/;

/**
 * How far apart two sprites of the same shape have to look, as a mean
 * per-channel distance (0-255). Twelve is empirical rather than principled: the
 * two books this check was strengthened over scored about 8 while being
 * indistinguishable, and every pair the recolour pass produced on purpose scores
 * well above 20. If a legitimate pair ever lands between, the answer is a better
 * measure rather than a lower number.
 */
const NEAR = 12;

const args = process.argv.slice(2);
const jarArg = args.indexOf('--vanilla-jar');
const VANILLA_JAR = jarArg >= 0 ? args[jarArg + 1] : defaultJar();

function defaultJar() {
  const cache = path.join(os.homedir(), '.gradle/caches/neoformruntime/artifacts');
  if (!fs.existsSync(cache)) return null;
  const hit = fs.readdirSync(cache).find(f => /^minecraft_.*_client\.jar$/.test(f));
  return hit ? path.join(cache, hit) : null;
}

// --- what every item model actually draws -------------------------------
function modelTextures() {
  const map = new Map(); // item id -> [texture refs]
  for (const f of fs.readdirSync(MODELS).filter(f => f.endsWith('.json'))) {
    const j = JSON.parse(fs.readFileSync(path.join(MODELS, f), 'utf8'));
    map.set(f.replace(/\.json$/, ''), Object.values(j.textures || {}).map(String));
  }
  // The breed eggs live in a subfolder of their own.
  const sub = path.join(MODELS, 'breed_egg');
  if (fs.existsSync(sub)) {
    for (const f of fs.readdirSync(sub).filter(f => f.endsWith('.json'))) {
      const j = JSON.parse(fs.readFileSync(path.join(sub, f), 'utf8'));
      map.set('breed_egg/' + f.replace(/\.json$/, ''), Object.values(j.textures || {}).map(String));
    }
  }
  return map;
}

const exempt = id => EXEMPT.some(re => re.test(id));

/**
 * Items whose definition recolours the model at render time. Their art is not
 * what the player sees, so comparing it to anybody else's is meaningless.
 */
function tintedItems() {
  const dir = path.join(A, 'items');
  const out = new Set();
  const walk = (d, prefix) => {
    for (const f of fs.readdirSync(d)) {
      const full = path.join(d, f);
      if (fs.statSync(full).isDirectory()) { walk(full, prefix + f + '/'); continue; }
      if (!f.endsWith('.json')) continue;
      if (/"tints"\s*:/.test(fs.readFileSync(full, 'utf8'))) {
        out.add(prefix + f.replace(/\.json$/, ''));
      }
    }
  };
  walk(dir, '');
  return out;
}

// --- vanilla art, by hash ------------------------------------------------
function vanillaHashes() {
  if (!VANILLA_JAR || !fs.existsSync(VANILLA_JAR)) return null;
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'vanilla-item-art-'));
  try {
    // `jar` ships with the JDK this project already requires, so there is no
    // new dependency and no unzip on PATH to depend on.
    execFileSync('jar', ['xf', VANILLA_JAR, 'assets/minecraft/textures/item'], { cwd: tmp });
  } catch {
    return null;
  }
  const dir = path.join(tmp, 'assets/minecraft/textures/item');
  if (!fs.existsSync(dir)) return null;
  const byHash = new Map();
  for (const f of fs.readdirSync(dir).filter(f => f.endsWith('.png'))) {
    try {
      const h = artHash(readPng(path.join(dir, f)));
      if (!byHash.has(h)) byHash.set(h, []);
      byHash.get(h).push('minecraft:item/' + f.replace(/\.png$/, ''));
    } catch { /* an animated or odd-format vanilla sprite; not our problem */ }
  }
  fs.rmSync(tmp, { recursive: true, force: true });
  return byHash;
}

// --- the audit -----------------------------------------------------------
const models = modelTextures();
const vanilla = vanillaHashes();
const problems = [];

// 1. An item model whose WHOLE art is one vanilla texture is identical to that
//    vanilla item by construction - no pixel comparison needed.
//
//    Two things are deliberately not collisions here. A model that LAYERS two
//    vanilla textures is a picture that does not exist in vanilla (the freedom
//    stick is a stick with a feather on it, and reads as neither). And a model
//    the item definition puts a TINT on is not drawn in vanilla's colours at all
//    - golden carrot seeds are wheat seeds in gold, which is the whole point of
//    them. Both would otherwise be false positives, and a checker that cries
//    wolf about art gets switched off.
const tinted = tintedItems();
const borrowing = new Map(); // "minecraft:item/x" -> [our items]
for (const [id, texs] of models) {
  if (exempt(id) || tinted.has(id) || texs.length !== 1) continue;
  const t = texs[0];
  if (t.startsWith('minecraft:')) {
    if (!borrowing.has(t)) borrowing.set(t, []);
    borrowing.get(t).push(id);
  }
}
for (const [tex, ids] of [...borrowing].sort()) {
  problems.push({ kind: 'vanilla-ref', what: ids.sort(), same: tex });
}

// 1b. ...and every texture an item model names actually exists. Not a duplicate
//     at all, but this is the only tool that walks item models, and a model
//     pointing at a texture nobody wrote is a PURPLE CUBE that logs nothing -
//     exactly the failure check-block-models.mjs exists to catch on the block
//     side. Repointing a model at new art is precisely when it happens.
for (const [id, texs] of models) {
  for (const t of texs) {
    if (!t.startsWith('horsegenetics:')) continue;
    const rel = t.slice('horsegenetics:'.length);
    if (!fs.existsSync(path.join(A, 'textures', rel + '.png'))) {
      problems.push({ kind: 'missing-texture', what: [id], same: t });
    }
  }
}

// 2. Two of our item models pointing at the same texture set.
const byTexSet = new Map();
for (const [id, texs] of models) {
  if (exempt(id)) continue;
  const key = texs.slice().sort().join('|');
  if (!key) continue;
  if (!byTexSet.has(key)) byTexSet.set(key, []);
  byTexSet.get(key).push(id);
}
for (const [key, ids] of [...byTexSet].sort()) {
  if (ids.length > 1 && !key.startsWith('minecraft:')) {
    problems.push({ kind: 'shared-texture', what: ids.sort(), same: key });
  }
}

// 3. Two of our PNGs that are the same picture under different names, and any
//    of ours that is the same picture as a vanilla one.
//
//    NEAR-identical counts, not just identical. The first version of this check
//    compared hashes alone, passed a recolour that had moved a sprite's
//    saturation from 0.40 to 0.18, and certified as distinct two books nobody
//    could tell apart. Different pixels is not the question; a player being able
//    to see the difference is.
const ours = new Map(); // hash -> [texture names]
const sigs = new Map(); // texture name -> signature
for (const f of fs.readdirSync(TEXTURES).filter(f => f.endsWith('.png'))) {
  const name = 'horsegenetics:item/' + f.replace(/\.png$/, '');
  try {
    const img = readPng(path.join(TEXTURES, f));
    const h = artHash(img);
    if (!ours.has(h)) ours.set(h, []);
    ours.get(h).push(name);
    sigs.set(name, artSignature(img));
  } catch (e) {
    problems.push({ kind: 'unreadable', what: [name], same: String(e.message) });
  }
}
for (const [h, names] of [...ours].sort()) {
  if (names.length > 1) {
    problems.push({ kind: 'same-art', what: names.sort(), same: 'identical pixels' });
  }
  if (vanilla && vanilla.has(h)) {
    problems.push({ kind: 'same-as-vanilla', what: names.sort(), same: vanilla.get(h).join(', ') });
  }
}

// 4. And the same question asked so it can be answered by degree. Only sprites
//    ACTUALLY DRAWN by an item model are compared: textures/item/ also holds
//    art nothing references any more, and failing the build over two unused
//    files would be noise.
const drawn = new Set();
for (const [id, texs] of models) {
  if (exempt(id)) continue;
  for (const t of texs) if (t.startsWith('horsegenetics:')) drawn.add(t);
}
const names = [...sigs.keys()].filter(n => drawn.has(n)).sort();
for (let i = 0; i < names.length; i++) {
  for (let j = i + 1; j < names.length; j++) {
    if (WOOD_FAMILY.test(names[i]) && WOOD_FAMILY.test(names[j])) continue;
    const d = artDistance(sigs.get(names[i]), sigs.get(names[j]));
    if (d !== null && d < NEAR) {
      problems.push({
        kind: 'near-identical',
        what: [names[i], names[j]],
        same: `mean channel distance ${d.toFixed(1)}, under the threshold of ${NEAR}`,
      });
    }
  }
}

// --- report --------------------------------------------------------------
if (!vanilla) {
  console.warn('! No vanilla client jar found, so "identical to a Minecraft item" was NOT checked.');
  console.warn('  Pass --vanilla-jar <path>, or run a Gradle task once so the jar is in the cache.');
}
if (problems.length === 0) {
  console.log(`item art OK - ${models.size} item models, every texture present, every one `
    + `distinct from the others `
    + `and from vanilla${vanilla ? ` (${vanilla.size} vanilla sprites compared)` : ''}.`);
  process.exit(0);
}
console.error(`${problems.length} item art collision(s):\n`);
const LABEL = {
  'vanilla-ref': 'draws a vanilla texture directly',
  'shared-texture': 'share one texture',
  'same-art': 'are the same picture',
  'same-as-vanilla': 'is the same picture as a vanilla item',
  'missing-texture': 'points at a texture that does not exist',
  'near-identical': 'are too alike to tell apart',
  'unreadable': 'could not be read',
};
for (const p of problems) {
  console.error(`  [${LABEL[p.kind]}]`);
  console.error(`      ${p.what.join(', ')}`);
  console.error(`      -> ${p.same}\n`);
}
process.exit(1);
