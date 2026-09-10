// Bake the mod's own recipe files into one client-readable reference.
//
// WHY THIS EXISTS
// The Horse Browser's Recipes tab is a reference: it shows how to make this
// mod's things, and you then make them at an ordinary crafting table. To draw a
// recipe it needs the recipe, and the client has no dependable way to ask for
// one - `data/` is datapack territory, the client-side recipe API in 26.1.2
// exposes display objects rather than ingredients, and neither is worth
// depending on for a static list the jar already contains.
//
// So this reads `data/horsegenetics/recipe/*.json` - the real recipes, the ones
// the game loads - and writes a flat summary into `assets/`, where the client
// can read it straight off the classpath. It is generated, so it cannot drift
// from the recipes it describes; re-run it whenever a recipe changes, and see
// CLAUDE.md's regenerate table.
//
// The two `horsegenetics:*` recipe types are CustomRecipes with no ingredients
// in their JSON - their inputs are computed in Java. They are emitted as
// `kind: "custom"` and the screen describes them itself; SpliceRecipeDisplay is
// the per-gene half of that.
//
//   node neoforge-26.1.2/tools/bake-recipe-reference.mjs

import { readFileSync, writeFileSync, readdirSync } from "node:fs";
import { join, dirname, basename } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..");
const inDir = join(root, "src/main/resources/data/horsegenetics/recipe");
const outFile = join(root, "src/main/resources/assets/horsegenetics/recipe_reference.json");

/** An ingredient is an item id, or a "#tag" the client resolves at draw time. */
function ingredient(value, where) {
  if (typeof value === "string") return value;
  // Arrays (a choice of items) are legal in vanilla and unused here. Fail loudly
  // rather than silently drawing the first option as though it were the only one.
  throw new Error(`${where}: unsupported ingredient ${JSON.stringify(value)}`);
}

const recipes = [];

for (const file of readdirSync(inDir).filter((f) => f.endsWith(".json")).sort()) {
  const id = basename(file, ".json");
  const d = JSON.parse(readFileSync(join(inDir, file), "utf8"));
  const type = d.type;

  if (type === "minecraft:crafting_shapeless") {
    recipes.push({
      id,
      kind: "shapeless",
      in: d.ingredients.map((v, i) => ingredient(v, `${file} ingredient ${i}`)),
      out: d.result.id,
      count: d.result.count ?? 1,
    });
  } else if (type === "minecraft:crafting_shaped") {
    const rows = d.pattern;
    const grid = [];
    for (let r = 0; r < 3; r++) {
      for (let c = 0; c < 3; c++) {
        const ch = rows[r]?.[c] ?? " ";
        grid.push(ch === " " ? null : ingredient(d.key[ch], `${file} key ${ch}`));
      }
    }
    recipes.push({ id, kind: "shaped", grid, out: d.result.id, count: d.result.count ?? 1 });
  } else if (type.startsWith("horsegenetics:")) {
    // Inputs live in Java; the screen writes its own description.
    recipes.push({ id, kind: "custom", type });
  } else {
    throw new Error(`${file}: unhandled recipe type ${type}`);
  }
}

writeFileSync(outFile, JSON.stringify({ recipes }, null, 2) + "\n", "utf8");

const byKind = recipes.reduce((m, r) => ((m[r.kind] = (m[r.kind] ?? 0) + 1), m), {});
console.log(
  `recipe reference: ${recipes.length} recipes ` +
    `(${Object.entries(byKind).map(([k, n]) => `${n} ${k}`).join(", ")}) -> ` +
    outFile.slice(outFile.indexOf("neoforge")),
);
