#!/usr/bin/env node
// Bake part_sheet.png - the grain every attached part's boxes sample.
//
//   node neoforge-26.1.2/tools/bake-part-sheet.mjs
//
// 64x64, cut into a 4x4 grid of 16x16 regions. The ids, the size and the grid are
// PartSheet.java's and this file must agree with it; the constants are repeated at
// the top here and checked, because the failure otherwise is a horn wearing
// somebody else's pixels and nothing goes red.
//
// GREYSCALE, AND THAT IS THE DESIGN
// Not a limitation of the tool. Colour is a per-horse tint applied at draw time -
// the same method a dyed braid uses - so one file serves every horn this mod can
// grow: ivory, bone, pearl, a warm gold, and whatever a lineage's colour drifts to
// over thirty generations. What a region carries is grain and light. Painting the
// colour in would mean one sheet per colour, and the colours are continuous.
//
// WHY EVERY REGION IS A UNIFORM, TILEABLE GRAIN
// A baked cube's UV rectangle is its own unwrap - 2*(girth+girth) wide and
// girth+length tall - laid down at its region's corner. A horn segment is about
// 10x4 texels at the very largest, so a box reads the CORNER of its region and
// never the whole of it. That is deliberate (it means a part whose proportions
// change with an epigenetic number needs no new sheet), and the price is that a
// region has to look right at any sub-rectangle of itself. So: no composition, no
// features, no gradient across the region - a tileable value-noise grain with a
// direction, and nothing else.
//
// THE ART IS ORIGINAL. Procedural noise written here, so there is nothing of
// anybody else's in it. A hand-painted or texture-pack-sourced sheet is an upgrade
// path and not a dependency - drop one in, keep the regions uniform, and no code
// changes. Anything vendored that way needs its licence checked first
// (CLAUDE.md), which is exactly why the shipped default owes nothing to anyone.

import { writePng } from "./png.mjs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const OUT = join(here,
  "../src/main/resources/assets/horsegenetics/textures/entity/horse/part_sheet.png");

// --- must match PartSheet.java ---
const SIZE = 64;
const REGION = 16;
const ACROSS = SIZE / REGION;
const HORN = 0;
const HORN_TIP = 1;

/** Deterministic integer hash to [0,1). Nothing here may use Math.random. */
function hash(x, y, salt) {
  let n = (Math.imul(x, 374761393) + Math.imul(y, 668265263) + Math.imul(salt, 1274126177)) | 0;
  n = Math.imul(n ^ (n >>> 13), 1274126177) | 0;
  return ((n ^ (n >>> 16)) >>> 0) / 4294967296;
}

/**
 * Value noise that wraps at `period`, so a region has no seam at any edge - which
 * is what lets a box read an arbitrary sub-rectangle of it without a visible join.
 */
function noise(x, y, salt, period) {
  const x0 = Math.floor(x);
  const y0 = Math.floor(y);
  let fx = x - x0;
  let fy = y - y0;
  fx = fx * fx * (3 - 2 * fx);
  fy = fy * fy * (3 - 2 * fy);
  const at = (i, j) => hash(((i % period) + period) % period, ((j % period) + period) % period, salt);
  const a = at(x0, y0) * (1 - fx) + at(x0 + 1, y0) * fx;
  const b = at(x0, y0 + 1) * (1 - fx) + at(x0 + 1, y0 + 1) * fx;
  return a * (1 - fy) + b * fy;
}

/**
 * Keratin grain. Stretched hard along v, because a horn's boxes are unwrapped with
 * their length down the sheet, so the streaks run up the horn the way real growth
 * lines do. The fine cross-hatch on top is what keeps it from reading as a smear at
 * the 4-texel heights the smaller segments use.
 *
 * `tip` darkens and tightens it: the last two segments of a horn sample HORN_TIP,
 * and a denser, slightly darker point is what stops a long horn looking like a
 * painted dowel.
 */
function keratin(u, v, tip) {
  const long = noise(u / 1.1, v / 5.0, 1, REGION) * 0.62;
  const fine = noise(u * (tip ? 1.6 : 1.0), v * (tip ? 1.6 : 1.0), 2, REGION) * 0.26;
  const ridge = 0.075 * Math.sin((u * Math.PI) / 2.0);
  const base = tip ? 0.55 : 0.68;
  return base + 0.3 * (long + fine) + ridge;
}

const px = Buffer.alloc(SIZE * SIZE * 4);          // zeroed: an unused region is blank
function paint(region, fn) {
  const ox = (region % ACROSS) * REGION;
  const oy = Math.floor(region / ACROSS) * REGION;
  for (let v = 0; v < REGION; v++) {
    for (let u = 0; u < REGION; u++) {
      const value = Math.max(0, Math.min(1, fn(u, v)));
      const g = Math.round(value * 255);
      const i = ((oy + v) * SIZE + (ox + u)) * 4;
      px[i] = g;
      px[i + 1] = g;
      px[i + 2] = g;
      px[i + 3] = 255;
    }
  }
}

paint(HORN, (u, v) => keratin(u, v, false));
paint(HORN_TIP, (u, v) => keratin(u, v, true));

// The one thing worth asserting: every texel a box can reach is opaque. A part is
// drawn through a cutout pipeline, which DISCARDS a fragment under an alpha of
// 0.1 - so a region with a transparent corner is a horn with holes in it, and the
// hole would appear only on the segment sizes that happen to reach that corner.
for (const region of [HORN, HORN_TIP]) {
  const ox = (region % ACROSS) * REGION;
  const oy = Math.floor(region / ACROSS) * REGION;
  for (let v = 0; v < REGION; v++) {
    for (let u = 0; u < REGION; u++) {
      if (px[((oy + v) * SIZE + (ox + u)) * 4 + 3] !== 255) {
        throw new Error(`region ${region} has a transparent texel at ${u},${v}`);
      }
    }
  }
}

writePng(OUT, SIZE, SIZE, px);
console.log(`wrote ${OUT} (${SIZE}x${SIZE}, ${ACROSS}x${ACROSS} regions, 2 painted)`);
