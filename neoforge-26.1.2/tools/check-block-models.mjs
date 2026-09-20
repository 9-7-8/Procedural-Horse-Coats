#!/usr/bin/env node
// Every model id a blockstate, item definition or model parent points at must
// be a file that exists.
//
// WHY THIS EXISTS
// This is the asset layer's signature silent failure and the one the comments
// in bake-double-gates.mjs and GeneratedGates keep warning about: a model id
// that was never written does not crash, does not log, and does not go red in
// any build. It is a purple chequerboard, in one blockstate variant, on one
// block, which somebody finds months later if they happen to place it.
//
// It caught its first one immediately. bake-jumps.mjs composed a blockstate's
// model suffix as `(left?"_l":"") + (right?"_r":"")` - "_l_r" - while the
// models were written as "_lr". Only the both-connected case differed, so a
// lone jump and a row of TWO were perfect and the middle of every row of three
// was a purple cube. That is the exact shape of bug this file is for.
//
//   node neoforge-26.1.2/tools/check-block-models.mjs
//
// SCOPE, AND WHAT IT CANNOT SEE
// Shipped assets only - `src/main/resources/assets/horsegenetics`. The
// generated pack that compat/Generated* writes at run time for other mods'
// woods is not on disk and is not checked here, so the run-time twins can
// still drift. Their own class docs are the guard for that; this only proves
// the author-time half.
//
// Vanilla parents (`block/block`, `minecraft:...`) are assumed present rather
// than resolved against the client jar - a wrong one there fails loudly at
// load, which is the case this file is NOT needed for.

import { readdirSync, readFileSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const A = join(here, "..", "src/main/resources/assets/horsegenetics");

const missing = [];
const seen = new Set();

/** Where a model id would live on disk, or null if it is not ours. */
function modelPath(id) {
  const [ns, path] = id.includes(":") ? id.split(":") : ["minecraft", id];
  if (ns !== "horsegenetics") {
    return null;
  }
  return join(A, "models", path + ".json");
}

function check(id, from) {
  if (!id) {
    return;
  }
  const path = modelPath(id);
  if (path === null) {
    return;
  }
  seen.add(id);
  if (!existsSync(path)) {
    missing.push(`${from} -> ${id}`);
  }
}

/** A variant value is either one model or a weighted list of them. */
function checkVariant(value, from) {
  for (const one of [].concat(value)) {
    check(one.model, from);
  }
}

for (const file of readdirSync(join(A, "blockstates"))) {
  const json = JSON.parse(readFileSync(join(A, "blockstates", file), "utf8"));
  for (const [state, value] of Object.entries(json.variants ?? {})) {
    checkVariant(value, `blockstates/${file} [${state}]`);
  }
  // multipart, which nothing uses yet - cheap to cover before something does
  for (const [i, part] of (json.multipart ?? []).entries()) {
    checkVariant(part.apply, `blockstates/${file} [multipart ${i}]`);
  }
}

for (const file of readdirSync(join(A, "items"))) {
  const json = JSON.parse(readFileSync(join(A, "items", file), "utf8"));
  check(json.model?.model, `items/${file}`);
}

for (const file of readdirSync(join(A, "models/block"))) {
  const json = JSON.parse(readFileSync(join(A, "models/block", file), "utf8"));
  check(json.parent, `models/block/${file} (parent)`);
}

if (missing.length) {
  console.error(`block models: ${missing.length} reference(s) point at a file that is not there\n`);
  console.error(missing.join("\n"));
  process.exit(1);
}

console.log(`block models OK - ${seen.size} distinct model ids, all present`);
