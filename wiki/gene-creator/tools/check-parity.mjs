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
for (const file of ["geometry.js", "noise.js", "fields.js", "schema.js", "spec-engine.js"]) {
  vm.runInContext(readFileSync(join(creator, "js", file), "utf8"), sandbox, { filename: file });
}
const HG = sandbox.window.HG;

const specsDir = join(repo, "common", "src", "main", "resources", "horsegenetics", "example-genes");
const specs = Object.fromEntries(
  readdirSync(specsDir)
    .filter((f) => f.endsWith(".json"))
    .map((f) => [f, JSON.parse(readFileSync(join(specsDir, f), "utf8"))]));

const expected = JSON.parse(readFileSync(join(creator, "fixtures", "expected.json"), "utf8"));

// SeededRng(seed, namespace) = new Random(seed ^ (namespace.hashCode() * 0x9E3779B97F4A7C15L)).
function javaHashCode(s) {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return h;
}

function seededRngSeed(seed, namespace) {
  const n = HG.noise;
  return n.xor(n.u64(seed < 0 ? 0xFFFFFFFF : 0, seed >>> 0),
    n.mul(n.fromInt(javaHashCode(namespace)), n.K1));
}

const TOLERANCE = 1e-4;
let checked = 0;
const failures = [];

// 0. The schema tables must agree BEFORE anything else is worth checking.
//
// The creator omits any setting left at its default, so a default that differs
// between js/schema.js and SpecSchema.java writes a file that plays differently
// from how it previewed - and only for the settings you never touched, which is
// the hardest kind of bug to see. (This check found exactly that on AXIS.to,
// DILUTE.keepBlack, DILUTE.blackTint and TOWARD.strength.)
function checkParams(kind, type, javaParams, jsParams) {
  const jsByName = Object.fromEntries(jsParams.map((p) => [p.name, p]));
  for (const [name, jp] of Object.entries(javaParams)) {
    const p = jsByName[name];
    if (!p) {
      failures.push(`${kind} ${type}: the game accepts "${name}" but the creator does not offer it`);
      continue;
    }
    checked++;
    if (jp.kind !== p.kind) {
      failures.push(`${kind} ${type}.${name}: kind ${jp.kind} in Java, ${p.kind} in the creator`);
    }
    if (jp.kind === "VALUE" && Math.abs(Number(jp.fallback) - p.fallback) > 1e-9) {
      failures.push(`${kind} ${type}.${name}: default ${jp.fallback} in Java, ${p.fallback} in the creator`);
    }
    if (jp.choices && String(jp.choices) !== String(p.choices || [])) {
      failures.push(`${kind} ${type}.${name}: choices [${jp.choices}] in Java, [${p.choices}] in the creator`);
    }
  }
  for (const name of Object.keys(jsByName)) {
    if (!(name in javaParams)) {
      failures.push(`${kind} ${type}: the creator offers "${name}" but the game ignores it`);
    }
  }
}

if (expected.schema) {
  for (const [type, params] of Object.entries(expected.schema.masks)) {
    if (!HG.schema.MASKS[type]) failures.push(`mask ${type} exists in the game but not in the creator`);
    else checkParams("mask", type, params, HG.schema.MASKS[type].params);
  }
  for (const [type, def] of Object.entries(expected.schema.ops)) {
    const js = HG.schema.OPS[type];
    if (!js) { failures.push(`op ${type} exists in the game but not in the creator`); continue; }
    if (js.phase !== def.phase) failures.push(`op ${type}: phase ${def.phase} in Java, ${js.phase} in the creator`);
    checkParams("op", type, def.params, js.params);
  }
  for (const type of Object.keys(HG.schema.MASKS)) {
    if (!(type in expected.schema.masks)) failures.push(`the creator offers mask ${type}, which the game does not have`);
  }
  for (const type of Object.keys(HG.schema.OPS)) {
    if (!(type in expected.schema.ops)) failures.push(`the creator offers op ${type}, which the game does not have`);
  }
} else {
  failures.push("fixtures have no schema section - re-run ./gradlew :common:bakeSpecFixtures");
}

function report(c, what, index, want, got) {
  failures.push(`${c.spec} seed=${c.seed} dose=${c.dose} ${c.skin}: ${what}[${index}] `
    + `expected ${want}, got ${got}`);
}

for (const c of expected.cases) {
  const spec = specs[c.spec];
  if (!spec) {
    failures.push(`no such example gene: ${c.spec}`);
    continue;
  }
  const s = seededRngSeed(c.seed, spec.key);
  const values = HG.specEngine.drawValues(spec, s.h, s.l, c.dose);

  // 0b. the combination table itself. The fixture records which outcome the
  // game resolved this dose to, so a creator that maps a combination to the
  // wrong expression fails here rather than previewing a different horse.
  const expression = HG.specEngine.expressionFor(spec, c.combination);
  checked++;
  if (!expression) {
    failures.push(`${c.spec}: no expression covers ${c.combination}`);
    continue;
  }
  if (expression.id !== c.expression) {
    failures.push(`${c.spec} ${c.combination}: the game resolves it to "${c.expression}", `
      + `the creator to "${expression.id}"`);
    continue;
  }
  const layers = expression.layers || [];

  // 1. the knob draw - this is the java.util.Random port under test.
  (spec.knobs || []).forEach((knob, i) => {
    const want = c.knobs[i];
    if (knob.type === "seed") {
      if (want !== "seed") report(c, "knob", i, want, "seed");
      return;
    }
    want.forEach((w, leg) => {
      const got = values.ranges[i][leg];
      if (Math.abs(Number(w) - got) > TOLERANCE) report(c, `knob ${knob.name}.${leg}`, i, w, got);
      checked++;
    });
  });

  // 2. the painter.
  const N = HG.geometry.SHEET_SIZE;
  if (spec.phase !== "magical") {
    const after = HG.specEngine.restrict(spec, layers, values, c.skin, new HG.fields.PigmentField(N));
    c.probes.forEach((p, i) => {
      const [px, py, red, black] = p;
      if (Math.abs(after.redAt(px, py) - Number(red)) > TOLERANCE) {
        report(c, `red @${px},${py}`, i, red, after.redAt(px, py));
      }
      if (Math.abs(after.blackAt(px, py) - Number(black)) > TOLERANCE) {
        report(c, `black @${px},${py}`, i, black, after.blackAt(px, py));
      }
      checked += 2;
    });
  } else {
    const colour = new HG.fields.ColorField(N);
    HG.geometry.forEachTexel(c.skin, (px, py) => colour.setArgb(px, py, 0xFF404040));
    const delta = HG.specEngine.tint(spec, layers, values, c.skin, new HG.fields.PigmentField(N), colour);
    c.probes.forEach((p, i) => {
      const [px, py, r, g, b, a] = p;
      const got = [delta.redAt(px, py), delta.greenAt(px, py), delta.blueAt(px, py), delta.opacityAt(px, py)];
      [r, g, b, a].forEach((want, k) => {
        if (Math.abs(got[k] - Number(want)) > 1) report(c, `rgba${k} @${px},${py}`, i, want, got[k]);
        checked++;
      });
    });
  }
}

if (failures.length) {
  console.error(`PARITY FAILED - ${failures.length} mismatch(es) out of ${checked} checks:\n`);
  failures.slice(0, 25).forEach((f) => console.error("  " + f));
  if (failures.length > 25) console.error(`  ...and ${failures.length - 25} more`);
  console.error("\nThe creator's preview and the game's engine disagree. Fix the port before shipping.");
  process.exit(1);
}

console.log(`parity OK - ${checked} checks across ${expected.cases.length} cases`);
