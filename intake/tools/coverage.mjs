// What does each layer of a gene actually cover, and where?
//
//   node process/tools/coverage.mjs <gene.json> [seed]
//
// Runs the gene through the creator's engine - the same interpreter the game
// uses, proven equal by check-parity - and prints, per layer, the share of each
// body part's texels the layer's masks cover.
//
// It exists because "the icon shows nothing" has two very different causes: the
// masks covered nothing, or they covered something the camera cannot see. This
// tells you which, in a second, without launching anything.
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const here = dirname(fileURLToPath(import.meta.url));
const creator = join(here, "..", "..", "wiki", "gene-creator");

const sandbox = { window: {}, console };
sandbox.window.window = sandbox.window;
vm.createContext(sandbox);
for (const file of ["geometry.js", "noise.js", "fields.js", "schema.js", "spec-engine.js"]) {
  vm.runInContext(readFileSync(join(creator, "js", file), "utf8"), sandbox, { filename: file });
}
const HG = sandbox.window.HG;

const specPath = process.argv[2];
if (!specPath) {
  console.error("usage: node process/tools/coverage.mjs <gene.json> [seed]");
  process.exit(2);
}
const spec = JSON.parse(readFileSync(specPath, "utf8"));
const seed = Number(process.argv[3] || 7);

// Two copies of the first-declared allele: the gene at full expression, which
// is the case an icon shows and so the case worth probing.
const values = HG.specEngine.drawValues(spec, 0, seed, 2);
const expression = HG.specEngine.expressionForDose(spec, 2);
if (!expression) {
  console.error("no expression claims the homozygote");
  process.exit(1);
}
console.log(`${spec.key}  seed=${seed}  ->  ${expression.id}`);

const drawn = (spec.knobs || []).map((k, i) => {
  const v = values.ranges[i];
  return `${k.name}=${k.type === "seed" ? "seed" : v.map((x) => x.toFixed(2)).join("/")}`;
});
if (drawn.length) console.log("  knobs: " + drawn.join("  "));

const layers = expression.layers || [];
if (!layers.length) {
  console.log("  (paints nothing)");
  process.exit(0);
}

const coat = new HG.fields.PigmentField(HG.geometry.SHEET_SIZE);
HG.geometry.forEachTexel("ADULT", (px, py) => {
  // A mid-tone coat, so a PIGMENT mask reads something plausible.
  coat.setRed(px, py, 0.55);
  coat.setBlack(px, py, 0.55);
});

const parts = HG.geometry.PARTS;
layers.forEach((layer, i) => {
  const map = HG.specEngine.coverageMap(spec, layers, i, values, "ADULT", coat);
  const total = {};
  const count = {};
  HG.geometry.forEachTexel("ADULT", (px, py, part) => {
    const k = map[py * HG.geometry.SHEET_SIZE + px];
    total[part] = (total[part] || 0) + k;
    count[part] = (count[part] || 0) + 1;
  });
  const hits = parts
    .filter((p) => count[p] && total[p] / count[p] > 0.005)
    .map((p) => `${p} ${(100 * total[p] / count[p]).toFixed(0)}%`);
  const all = parts.reduce((a, p) => a + (total[p] || 0), 0)
    / parts.reduce((a, p) => a + (count[p] || 0), 0);
  console.log(`  [${i}] ${(100 * all).toFixed(1)}% overall  ${layer.name || layer.op.type}`);
  console.log(`      ${hits.length ? hits.join("  ") : "NOTHING"}`);
});
