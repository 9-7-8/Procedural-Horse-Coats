// Proves the creator's JavaScript engine still agrees with the Java one.
//
//   ./gradlew :common:bakeSpecFixtures      # what the game's engine produces
//   node wiki/gene-creator/tools/check-parity.mjs
//
// The creator's preview is a port (js/spec-engine.js mirrors
// common/coat/pattern/SpecPainter.java, js/noise.js mirrors BodyNoise, and the
// knob draw mirrors java.util.Random). A port drifts silently, and a drifted
// preview is worse than none: it shows a horse the game will not breed, and it
// looks right while doing it. Run this after touching either side.
//
// The checks themselves live in js/parity.js, which the creator also runs on
// boot - one implementation, so the terminal gate and the in-page self-check
// can never disagree about what "parity" means. This file is only the plumbing:
// find the fixtures and the example genes on disk, and turn the result into an
// exit code.
//
// Exit code 0 means they agree.
import { readFileSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const here = dirname(fileURLToPath(import.meta.url));
const creator = join(here, "..");
const repo = join(creator, "..", "..");

// The engine files are classic scripts hanging off `window` (so the creator
// works from file://, where ES modules are blocked). Give them a window.
const sandbox = { window: {}, console };
sandbox.window.window = sandbox.window;
vm.createContext(sandbox);
for (const file of ["geometry.js", "noise.js", "svg-path.js", "fields.js", "schema.js",
                    "spec-engine.js", "parity.js"]) {
  vm.runInContext(readFileSync(join(creator, "js", file), "utf8"), sandbox, { filename: file });
}
const HG = sandbox.window.HG;

const specsDir = join(repo, "common", "src", "main", "resources", "horsegenetics", "example-genes");
const specs = Object.fromEntries(
  readdirSync(specsDir)
    .filter((f) => f.endsWith(".json"))
    .map((f) => [f, JSON.parse(readFileSync(join(specsDir, f), "utf8"))]));

const fixtures = JSON.parse(readFileSync(join(creator, "fixtures", "expected.json"), "utf8"));

const { checked, failures, cases } = HG.parity.run({ fixtures, specs });

if (failures.length) {
  console.error(`PARITY FAILED - ${failures.length} mismatch(es) out of ${checked} checks:\n`);
  failures.slice(0, 25).forEach((f) => console.error("  " + f));
  if (failures.length > 25) console.error(`  ...and ${failures.length - 25} more`);
  console.error("\nThe creator's preview and the game's engine disagree. Fix the port before shipping.");
  process.exit(1);
}

console.log(`parity OK - ${checked} checks across ${cases} cases`);
