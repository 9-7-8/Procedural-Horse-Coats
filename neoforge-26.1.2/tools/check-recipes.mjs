#!/usr/bin/env node
// Every crafting recipe must have inputs no other recipe already claims.
//
// Two recipes with the same inputs are not a warning in Minecraft: the recipe
// manager resolves one of them and the other item becomes UNCRAFTABLE. Nothing
// goes red, nothing logs, and the only symptom is a player who cannot make a
// thing the wiki says they can.
//
// It had already happened. cowboy_hitch and horsemans_table shipped with
// byte-identical shapeless inputs - 3 hair cloth + 2 braided rope + 2 fence -
// and the wiki documented the shared recipe as though it were a feature, which
// is how it survived being noticed. See known-gaps gap 92.
//
//   node neoforge-26.1.2/tools/check-recipes.mjs
//
// Shapeless recipes are compared as a SORTED ingredient list, because that is
// how the game compares them: order is not part of the recipe. Shaped ones are
// compared as pattern plus key, which is order-sensitive and so compared as
// written.

import { readFileSync, readdirSync } from "node:fs";
import { join, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const RECIPES = resolve(HERE, "..", "src", "main", "resources", "data", "horsegenetics", "recipe");

/** An ingredient may be a string, a tag, or an object; normalise all three. */
function ingredientKey(ing) {
  if (typeof ing === "string") return ing;
  if (ing && typeof ing === "object") {
    if (ing.item) return String(ing.item);
    if (ing.tag) return "#" + String(ing.tag);
  }
  return JSON.stringify(ing);
}

/**
 * The identity a recipe occupies in the crafting grid, or null when this file
 * is a kind of recipe that cannot collide (smelting, stonecutting, ...).
 */
function slot(recipe) {
  const type = String(recipe.type || "");
  if (type.includes("crafting_shapeless")) {
    const ings = (recipe.ingredients || []).map(ingredientKey).sort();
    return "shapeless:" + JSON.stringify(ings);
  }
  if (type.includes("crafting_shaped")) {
    const key = Object.entries(recipe.key || {})
      .map(([k, v]) => k + "=" + ingredientKey(v))
      .sort();
    return "shaped:" + JSON.stringify(recipe.pattern || []) + JSON.stringify(key);
  }
  return null;
}

const bySlot = new Map();
const problems = [];
let scanned = 0;

for (const name of readdirSync(RECIPES)) {
  if (!name.endsWith(".json")) continue;
  const path = join(RECIPES, name);
  let recipe;
  try {
    recipe = JSON.parse(readFileSync(path, "utf8"));
  } catch (e) {
    problems.push(`${name}: not valid JSON - ${e.message}`);
    continue;
  }
  scanned++;

  // A VANILLA recipe with no result makes nothing, which is always a mistake.
  // The mod's own types (horsegenetics:known_gene_splice, :carrot_combine) are
  // custom serialisers whose result is computed in Java from the inputs, so
  // they carry a type and nothing else - and demanding a result of them would
  // be a false positive, which is worse than no check at all.
  const result = recipe.result && (recipe.result.id || recipe.result.item);
  const vanilla = String(recipe.type || "").startsWith("minecraft:");
  if (vanilla && !result) {
    problems.push(`${name}: no result id - this recipe produces nothing`);
  }

  const key = slot(recipe);
  if (key === null) continue;
  if (!bySlot.has(key)) bySlot.set(key, []);
  bySlot.get(key).push({ name, result });
}

for (const [, entries] of bySlot) {
  if (entries.length < 2) continue;
  const names = entries.map((e) => `${e.name} (-> ${e.result})`).join("  and  ");
  problems.push(
    `these recipes have identical inputs, so only ONE of them can ever be crafted:\n      ${names}`
  );
}

if (problems.length) {
  console.error(`${problems.length} recipe problem(s) across ${scanned} files:\n`);
  for (const p of problems) console.error("  " + p);
  process.exit(1);
}
console.log(`recipes OK - ${scanned} files, no two claim the same inputs`);
