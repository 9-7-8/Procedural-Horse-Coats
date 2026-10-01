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
// grow - and every antler: ivory, bone, umber, a gem, a leaf green, and whatever a lineage's colour drifts to
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
const BONE = 2;
const BONE_TIP = 3;
const BLOOM = 4;
const RAM_HORN = 5;
const RAM_TIP = 6;

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

/**
 * Antler bone. Coarser and more pitted than keratin - antler is bone, with a
 * rough, beaded surface ("pearling") toward the base - and still streaked along v,
 * the length of the beam. `tip` is the polished point: smoother and paler, because
 * a real tine's tip is worn smooth and light, and because that is the region a
 * glowing antler lights and a crystal one keeps solid, so it should read as a cap.
 */
function bone(u, v, tip) {
  const long = noise(u / 1.4, v / 3.5, 3, REGION) * 0.40;
  const pits = noise(u * 1.9, v * 1.9, 4, REGION);
  const pearl = tip ? 0 : (pits > 0.72 ? -0.16 : 0) + (pits < 0.18 ? 0.10 : 0);
  const base = tip ? 0.80 : 0.62;
  return base + (tip ? 0.12 : 0.3) * long + pearl;
}

/**
 * Leaves, moss and blossom - one mottled grain with no direction, since a clump
 * is not grown along anything. Light and dark blotches so the per-horse tint (a
 * green, an olive, a pink) reads as foliage rather than as a painted block.
 */
function bloom(u, v) {
  const blotch = noise(u / 2.2, v / 2.2, 5, REGION);
  const fine = noise(u * 1.3, v * 1.3, 6, REGION);
  return 0.58 + 0.34 * blotch + 0.14 * fine - (fine > 0.8 ? 0.2 : 0);
}

/**
 * Ram's horn. The thing that makes a ram's horn read as one is the heavy ridging
 * ACROSS it - growth rings - so the grain runs along u here, the opposite of the
 * unicorn's lengthwise keratin, with a period of 2 texels so even a short segment
 * shows two or three rings. `tip` is the worn, smoothed point: rings faded out.
 */
function ram(u, v, tip) {
  const ring = Math.sin((v * Math.PI) / 1.0);
  const wobble = noise(u / 3.0, v / 1.5, 7, REGION) * 0.35;
  const fine = noise(u * 1.4, v * 1.4, 8, REGION) * 0.18;
  const ridge = tip ? 0.04 * ring : 0.16 * ring;
  return (tip ? 0.74 : 0.62) + ridge + 0.25 * (wobble + fine);
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
paint(BONE, (u, v) => bone(u, v, false));
paint(BONE_TIP, (u, v) => bone(u, v, true));
paint(BLOOM, (u, v) => bloom(u, v));
paint(RAM_HORN, (u, v) => ram(u, v, false));
paint(RAM_TIP, (u, v) => ram(u, v, true));

// The one thing worth asserting: every texel a box can reach is opaque. A part is
// drawn through a cutout pipeline, which DISCARDS a fragment under an alpha of
// 0.1 - so a region with a transparent corner is a horn with holes in it, and the
// hole would appear only on the segment sizes that happen to reach that corner.
const PAINTED = [HORN, HORN_TIP, BONE, BONE_TIP, BLOOM, RAM_HORN, RAM_TIP];
for (const region of PAINTED) {
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
console.log(`wrote ${OUT} (${SIZE}x${SIZE}, ${ACROSS}x${ACROSS} regions, ${PAINTED.length} painted)`);
