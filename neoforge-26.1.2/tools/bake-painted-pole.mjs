#!/usr/bin/env node
// Generate the PAINTED POLE texture - the pale timber a dyed jump is drawn on.
//
//   node neoforge-26.1.2/tools/bake-painted-pole.mjs
//
// WHY THIS EXISTS
// A block tint is a MULTIPLY: the renderer multiplies the texture's colour by
// the tint. So a tint can only ever darken, and it cannot change a hue the
// texture does not already have. Painting a jump blue by tinting oak planks
// therefore cannot work and did not - oak's blue channel sits around 0.33, so
// blue-on-oak came out a dark brown-navy and blue on a dark modded wood came
// out, in the owner's words, "nearly black purple".
//
// Vanilla solves exactly this for leather armour by dyeing a GREYSCALE base.
// This is that base: a pale, nearly-white timber, so multiplying by a dye lands
// the dye at close to its own brightness with the grain still reading through
// as shading.
//
// IT IS ALSO WHAT A REAL JUMP LOOKS LIKE. Showjumping poles and standards are
// painted solid colours; you do not see oak grain through the paint on a real
// course. So a painted jump losing its wood's identity is right rather than a
// compromise - and the wood is still stored underneath, and comes back the
// moment the paint is stripped with a fresh plank.
//
// THIS ART IS ORIGINAL, AND IT HAS TO BE.
// The obvious implementation was to desaturate vanilla's plank texture, and
// that is Mojang's art - a greyscale copy of it is a derivative work and this
// repo does not ship one (wiki/philosophy.html, THIRD_PARTY_NOTICES.md). So the
// grain here is generated: a deterministic value-noise field plus plank seams,
// drawn from nothing but the numbers below.

import { writeFileSync, mkdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { deflateSync } from "node:zlib";

const here = dirname(fileURLToPath(import.meta.url));
const OUT = join(here, "../src/main/resources/assets/horsegenetics/textures/block/painted_pole.png");

const SIZE = 16;

// The base is deliberately HIGH - a multiply by a dye should land near the
// dye's own brightness, so the timber has to be nearly white. Not pure white:
// 0 shading would make a painted jump a flat silhouette with no form at all,
// and the whole point of painting timber rather than plastic is that you can
// still see it is timber.
const BASE = 0xEC;

/** How far the grain darkens below BASE, at most. */
const GRAIN = 0x1A;

/** The seams between boards - darker than the grain, and the only hard lines. */
const SEAM = 0x2E;

/**
 * Deterministic value noise. Not Math.random: this file is checked in, and a
 * texture that changed every time it was baked would show up as a diff on every
 * unrelated re-run and tell nobody anything.
 */
function hash(x, y) {
  let h = (x * 374761393 + y * 668265263) >>> 0;
  h = (h ^ (h >>> 13)) >>> 0;
  h = Math.imul(h, 1274126177) >>> 0;
  return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
}

/**
 * The grain runs ALONG the pole, which is the x axis in the authored
 * facing=south frame - the rails span x, and so does a standard's own length
 * once the model rotates it. So the noise is stretched hard in x and tight in
 * y: long streaks rather than a speckle.
 */
function grainAt(x, y) {
  const streak = hash(Math.floor(x / 5), y) * 0.65 + hash(Math.floor(x / 2), y) * 0.35;
  return streak;
}

const pixels = Buffer.alloc(SIZE * SIZE * 4);
for (let y = 0; y < SIZE; y++) {
  for (let x = 0; x < SIZE; x++) {
    let value = BASE - Math.round(grainAt(x, y) * GRAIN);
    // Two seams, at thirds, so a 16px pole reads as boards rather than as one
    // slab. Not at 8 - a seam down the exact middle of a pole looks like a
    // modelling mistake rather than like joinery.
    if (y === 5 || y === 11) {
      value = BASE - SEAM;
    }
    const i = (y * SIZE + x) * 4;
    pixels[i] = value;
    pixels[i + 1] = value;
    pixels[i + 2] = value;
    pixels[i + 3] 	= 0xFF;
  }
}

// --- a minimal PNG encoder ------------------------------------------------
// Pure Node: zlib is built in, and this is the only thing in the repo's tools
// that writes an image, so a dependency for it would be a dependency for one
// file.

function chunk(type, data) {
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, "ascii"), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body) >>> 0);
  return Buffer.concat([length, body, crc]);
}

const CRC_TABLE = (() => {
  const table = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
    }
    table[n] = c;
  }
  return table;
})();

function crc32(buf) {
  let c = 0xFFFFFFFF;
  for (const byte of buf) {
    c = CRC_TABLE[(c ^ byte) & 0xFF] ^ (c >>> 8);
  }
  return (c ^ 0xFFFFFFFF) >>> 0;
}

const ihdr = Buffer.alloc(13);
ihdr.writeUInt32BE(SIZE, 0);
ihdr.writeUInt32BE(SIZE, 4);
ihdr[8] = 8;   // bit depth
ihdr[9] = 6;   // colour type: RGBA
ihdr[10] = 0;  // deflate
ihdr[11] = 0;  // adaptive filtering
ihdr[12] = 0;  // no interlace

// One filter byte (0 = None) per scanline, which is what the spec requires and
// what every decoder expects even when nothing is filtered.
const raw = Buffer.alloc(SIZE * (SIZE * 4 + 1));
for (let y = 0; y < SIZE; y++) {
  raw[y * (SIZE * 4 + 1)] = 0;
  pixels.copy(raw, y * (SIZE * 4 + 1) + 1, y * SIZE * 4, (y + 1) * SIZE * 4);
}

const png = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]),
  chunk("IHDR", ihdr),
  chunk("IDAT", deflateSync(raw, { level: 9 })),
  chunk("IEND", Buffer.alloc(0)),
]);

mkdirSync(dirname(OUT), { recursive: true });
writeFileSync(OUT, png);
console.log(`painted pole: ${SIZE}x${SIZE}, base ${BASE.toString(16)}, ${png.length} bytes -> ${OUT}`);
