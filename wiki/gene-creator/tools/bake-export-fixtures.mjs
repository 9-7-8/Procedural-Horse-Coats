// Regenerates the three fixtures CreatorMetadataRoundTripTest parses.
//
//   node wiki/gene-creator/tools/bake-export-fixtures.mjs
//   ./gradlew :common:test --tests '*CreatorMetadataRoundTripTest*'
//
// WHY THEY EXIST: check-parity.mjs guards the paint engine, and only the paint
// engine - it compares pigment probes. The creator also writes two things that
// never paint and so are invisible to it: the format-3 gameplay metadata (the
// blurb, the rarity, the gene carrot, the splice table) and the `effects`
// block. Those are guarded instead by exporting them here and parsing the
// result with the real GeneSpecParser.
//
// Run this after changing js/spec-model.js's tidy(), the metadata forms, or the
// effects mirror in js/schema.js.
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const here = dirname(fileURLToPath(import.meta.url));
const creator = join(here, "..");
const repo = join(creator, "..", "..");
const out = join(repo, "common", "src", "test", "resources");

const sandbox = { window: {}, console };
sandbox.window.window = sandbox.window;
vm.createContext(sandbox);
for (const f of ["geometry.js", "noise.js", "fields.js", "schema.js", "spec-engine.js", "spec-model.js"]) {
  vm.runInContext(readFileSync(join(creator, "js", f), "utf8"), sandbox, { filename: f });
}
const HG = sandbox.window.HG;
const model = HG.specModel;

function write(name, text) {
  writeFileSync(join(out, name), text);
  console.log("wrote common/src/test/resources/" + name);
}

// 1. Every metadata block, filled in.
const meta = model.blank();
meta.blurb = "A gene that does a thing.";
meta.rarity = "legendary";
meta.carrot = { enabled: true, behaviour: "homozygous", flavour: ["minecraft:sugar"] };
meta.splice = { "My/My": 10, "My/my": 40, "my/my": 50 };
write("creator-metadata-roundtrip.json", model.toJson(meta));

// 2. A shipped gene, before and after a creator round-trip. The export is
//    shorter than the file it read - the test's job is to prove the game still
//    reads the two as the same gene.
const waterborn = JSON.parse(readFileSync(
  join(repo, "neoforge-26.1.2", "src", "main", "resources", "horsegenetics", "genes", "waterborn.json"), "utf8"));
write("creator-effects-waterborn-original.json", JSON.stringify(waterborn, null, 2) + "\n");
write("creator-effects-waterborn-tidied.json", model.toJson(waterborn));

// 3. One of every effect verb the creator's form can emit, with the awkward
//    shapes exercised: a trigger that carries a value, a multi-flag condition,
//    a two-copy gate, a parts list and a colour.
const all = model.blank();
all.key = "mymod.every_effect";
const effects = model.effectsOf(all);
for (const verb of Object.keys(HG.schema.EFFECTS)) effects.push(model.newEffect(verb));
const by = (v) => effects.find((e) => e.type === v);

by("traversal").when = { flag: "adult" };
by("attribute").op = "multiply_base";
by("attribute").amount = 0.2;
by("emitter").trigger = { interval: 6 };
by("emitter").color = "#ffcf47";
by("emitter").count = 4;
by("mob_effect").effect = "minecraft:dolphins_grace";
by("mob_effect").target = "rider";
by("yield").trigger = { on_interact: "minecraft:bucket" };
by("yield").produces = "minecraft:milk_bucket";
by("yield").cooldown = 24000;
by("yield").denied_damage = 1;
by("yield").denied_message = "He kicks you.";
by("yield").when = { all: [{ flag: "sex_female" }, { flag: "tamed" }, { flag: "baby", negate: true }] };
by("glow").light = 12;
by("glow").parts = ["HAIR"];
by("healing").target = "rider";
by("spread").minDose = 2;

const problems = model.problems(all);
if (problems.length) {
  console.error("the all-verbs fixture does not validate:");
  problems.forEach((p) => console.error("  - " + p));
  process.exit(1);
}
write("creator-effects-all-verbs.json", model.toJson(all));
