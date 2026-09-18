#!/usr/bin/env node
// Builds a tiny fake mod that adds one wood and two metals, so the modded-
// material compatibility layer can actually be tested.
//
//   node neoforge-26.1.2/tools/compat/build-test-mod.mjs          # write it
//   node neoforge-26.1.2/tools/compat/build-test-mod.mjs --remove # take it away
//
// WHY THIS EXISTS
// ---------------
// compat/ModdedMaterials reads other mods' jars to find woods and metals, and
// compat/GeneratedPack writes gates and armour for what it finds. With no other
// mods installed BOTH DO NOTHING, and doing nothing correctly is
// indistinguishable from being broken. Every claim about this feature was
// otherwise going to be "it should work" - which wiki/verification.html exists
// to stop.
//
// So this is the smallest thing that is a mod: a neoforge.mods.toml, one
// blockstate, one plank texture, one gate model, two common-tag files and two
// ingot textures. It registers nothing and has no Java in it at all, which is
// the point - the scan reads FILES, so a jar that only has files is a complete
// test of it.
//
// WHAT IT SHOULD PRODUCE
// ----------------------
//   horsegenetics:compattest_maple_double_fence_gate   from the maple gate
//   horsegenetics:tin_horse_armor                      grey-white, from c:ingots/tin
//   horsegenetics:ruby_horse_armor                     red, from c:gems/ruby
//
// The two metals are deliberately different: tin is an INGOT drawn in near-grey
// and ruby is a GEM drawn in saturated red, so the averaged colour and the
// ingot/gem ladder are both exercised rather than one of them twice.
//
// IT GOES IN run/mods AND run/ IS GITIGNORED, so this leaves nothing behind in
// the repo - but it DOES leave a mod in the owner's dev client until it is
// removed. Run with --remove when finished, and note that a dev client started
// while it is installed will have a maple double gate in its creative menu.

import { writeFileSync, mkdirSync, rmSync, existsSync } from "node:fs";
import { dirname, resolve, join } from "node:path";
import { fileURLToPath } from "node:url";
import { deflateRawSync, crc32 } from "node:zlib";

const HERE = dirname(fileURLToPath(import.meta.url));
const MODS = resolve(HERE, "..", "..", "run", "mods");
const JAR = join(MODS, "compattest.jar");
const MOD_ID = "compattest";

if (process.argv.includes("--remove")) {
  if (existsSync(JAR)) {
    rmSync(JAR);
    console.log("removed " + JAR);
  } else {
    console.log("not installed: " + JAR);
  }
  process.exit(0);
}

// --- the smallest valid PNG writer ----------------------------------------
// A solid RGBA square. Written by hand rather than pulled from a library: this
// repo has no npm dependencies and is not about to grow one for six pixels.
function png(size, [r, g, b]) {
  const chunk = (type, data) => {
    const out = Buffer.alloc(8 + data.length + 4);
    out.writeUInt32BE(data.length, 0);
    out.write(type, 4, "ascii");
    data.copy(out, 8);
    out.writeUInt32BE(crc32(Buffer.concat([Buffer.from(type, "ascii"), data])) >>> 0,
      8 + data.length);
    return out;
  };
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8;   // bit depth
  ihdr[9] = 6;   // colour type: RGBA
  // A scanline is a filter byte then the pixels.
  const raw = Buffer.alloc(size * (1 + size * 4));
  for (let y = 0; y < size; y++) {
    const row = y * (1 + size * 4);
    for (let x = 0; x < size; x++) {
      const p = row + 1 + x * 4;
      raw[p] = r; raw[p + 1] = g; raw[p + 2] = b; raw[p + 3] = 255;
    }
  }
  // zlib stream: 0x78 0x01 header, the deflated data, then the adler32.
  let a = 1, bsum = 0;
  for (const byte of raw) { a = (a + byte) % 65521; bsum = (bsum + a) % 65521; }
  const adler = Buffer.alloc(4);
  adler.writeUInt32BE(((bsum << 16) | a) >>> 0, 0);
  const idat = Buffer.concat([Buffer.from([0x78, 0x01]), deflateRawSync(raw), adler]);
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", ihdr), chunk("IDAT", idat), chunk("IEND", Buffer.alloc(0)),
  ]);
}

// --- a stored (uncompressed) zip ------------------------------------------
// Stored rather than deflated so this stays short; a jar is a zip and nothing
// requires the entries be compressed.
function jar(entries) {
  const locals = [];
  const central = [];
  let offset = 0;
  for (const [name, body] of entries) {
    const data = Buffer.isBuffer(body) ? body : Buffer.from(body, "utf8");
    const nameBuf = Buffer.from(name, "utf8");
    const crc = crc32(data) >>> 0;

    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(data.length, 18);
    local.writeUInt32LE(data.length, 22);
    local.writeUInt16LE(nameBuf.length, 26);
    locals.push(local, nameBuf, data);

    const dir = Buffer.alloc(46);
    dir.writeUInt32LE(0x02014b50, 0);
    dir.writeUInt16LE(20, 4);
    dir.writeUInt16LE(20, 6);
    dir.writeUInt32LE(crc, 16);
    dir.writeUInt32LE(data.length, 20);
    dir.writeUInt32LE(data.length, 24);
    dir.writeUInt16LE(nameBuf.length, 28);
    dir.writeUInt32LE(offset, 42);
    central.push(dir, nameBuf);

    offset += local.length + nameBuf.length + data.length;
  }
  const centralBuf = Buffer.concat(central);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(entries.length, 8);
  end.writeUInt16LE(entries.length, 10);
  end.writeUInt32LE(centralBuf.length, 12);
  end.writeUInt32LE(offset, 16);
  return Buffer.concat([Buffer.concat(locals), centralBuf, end]);
}

const TOML = `
modLoader = "javafml"
loaderVersion = "[1,)"
license = "CC BY-NC 4.0"

[[mods]]
modId = "${MOD_ID}"
version = "1.0.0"
displayName = "Compat Test Materials"
description = "One wood and two metals, so the modded-material scan has something to find. Generated by tools/compat/build-test-mod.mjs; not a real mod."
`.trimStart();

const entries = [
  ["META-INF/neoforge.mods.toml", TOML],

  // --- one wood -----------------------------------------------------------
  // A blockstate is all the scan needs to call something a wood, and the plank
  // texture beside it is what the generated models will point at.
  [`assets/${MOD_ID}/blockstates/maple_fence_gate.json`,
    JSON.stringify({ variants: { "": { model: `${MOD_ID}:block/maple_fence_gate` } } }, null, 2)],
  [`assets/${MOD_ID}/models/block/maple_fence_gate.json`,
    JSON.stringify({ parent: "minecraft:block/template_fence_gate",
      textures: { texture: `${MOD_ID}:block/maple_planks` } }, null, 2)],
  [`assets/${MOD_ID}/textures/block/maple_planks.png`, png(16, [0xC8, 0x8E, 0x5A])],

  // --- two metals ---------------------------------------------------------
  ["data/c/tags/item/ingots/tin.json",
    JSON.stringify({ values: [`${MOD_ID}:tin_ingot`] }, null, 2)],
  ["data/c/tags/item/gems/ruby.json",
    JSON.stringify({ values: [`${MOD_ID}:ruby_gem`] }, null, 2)],
  [`assets/${MOD_ID}/textures/item/tin_ingot.png`, png(16, [0xD6, 0xD8, 0xDE])],
  [`assets/${MOD_ID}/textures/item/ruby_gem.png`, png(16, [0xC0, 0x1A, 0x2B])],
];

mkdirSync(MODS, { recursive: true });
writeFileSync(JAR, jar(entries));
console.log(`wrote ${JAR} (${entries.length} entries)`);
console.log("now launch the game or runServer, then look in run/phc/generated/");
console.log("run with --remove when you are done - it stays in the dev client otherwise");
