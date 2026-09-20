#!/usr/bin/env node
// Generate PAINT_OVERLAY - the translucent wash a dyed jump is painted with.
//
//   node neoforge-26.1.2/tools/bake-paint-overlay.mjs
//
// WHY A SEPARATE OVERLAY, AND NOT A TINT ON THE WOOD
// A block tint is a MULTIPLY. That has two consequences and the jumps hit both
// in turn:
//
//   1. It can only DARKEN, and it can only deepen a hue the texture already
//      has. Oak's blue channel sits around a third, so blue-on-oak came out a
//      dark navy-brown and blue on a dark modded wood came out "nearly black
//      purple" (owner). Lightening the dye does not help - multiplying oak by a
//      pale blue just gives slightly darker oak, because the ratio between the
//      channels barely moves.
//   2. Painting a PALE NEUTRAL POLE instead fixes the colour and throws the
//      wood away with it: "we're losing too much of the original texture /
//      shading" (owner). Correct - there was no wood left to see.
//
// So the paint is a THIRD PART laid over the wood, drawn with this texture and
// tinted: white, at a constant alpha, so the result is an alpha blend rather
// than a multiply. wood*(1-a) + dye*a. The wood's grain and shading survive at
// full contrast underneath, the colour reads as itself, and OPACITY below is a
// single number that trades between them.
//
// THE ART IS ORIGINAL AND HAS TO BE. The tempting implementation is a
// desaturated copy of vanilla's plank sheet, which is Mojang's art and a
// derivative work this repo does not ship. This is flat white - there is
// nothing of anybody's in it.

import { writeFileSync, mkdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { deflateSync } from "node:zlib";

const here = dirname(fileURLToPath(import.meta.url));
const OUT = join(here, "../src/main/resources/assets/horsegenetics/textures/block/paint_overlay.png");

const SIZE = 16;

/**
 * How much of the paint you see, 0-1. The one number worth tuning.
 *
 * 0.62 keeps the grain and the shading clearly readable while the colour still
 * reads as the colour rather than as a tint - which is the balance the owner
 * asked for after the fully-opaque version ("we're losing too much of the
 * original texture/shading") and the multiply version ("making it too dark").
 */
const OPACITY = 0.62;

const ALPHA = Math.round(OPACITY * 255);

// Flat white. The colour comes entirely from the block tint; the texture's only
// job is to be a uniform surface at a known alpha.
const pixels = Buffer.alloc(SIZE * SIZE * 4);
for (let i = 0; i < SIZE * SIZE; i++) {
  pixels[i * 4] = 0xFF;
  pixels[i * 4 + 1] = 0xFF;
  pixels[i * 4 + 2] = 0xFF;
  pixels[i * 4 + 3] = ALPHA;
}

// --- a minimal PNG encoder ------------------------------------------------
// Pure Node: zlib is built in, and this is the only thing in the repo's tools
// that writes an image, so a dependency for it would be a dependency for one
// file.

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

function chunk(type, data) {
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, "ascii"), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([length, body, crc]);
}

const ihdr = Buffer.alloc(13);
ihdr.writeUInt32BE(SIZE, 0);
ihdr.writeUInt32BE(SIZE, 4);
ihdr[8] = 8;   // bit depth
ihdr[9] = 6;   // colour type: RGBA
ihdr[10] = 0;  // deflate
ihdr[11] = 0;  // adaptive filtering
ihdr[12] = 0;  // no interlace

// One filter byte (0 = None) per scanline, which the spec requires and every
// decoder expects even when nothing is filtered.
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
console.log(
  `paint overlay: ${SIZE}x${SIZE}, opacity ${OPACITY} (alpha ${ALPHA}), ` +
    `${png.length} bytes -> ${OUT}`
);
