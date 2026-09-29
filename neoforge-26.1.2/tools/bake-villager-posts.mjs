// The five job-site posts, drawn so you can tell whose is whose.
//
// Owner, 2026-09-29: the five posts - cowboy_hitch, leatherworkers_post,
// metalsmiths_post, scientists_post, suppliers_post - all pointed at the ONE
// texture set, horse_traders_post_{top,side,bottom}. Five different workstations
// that were pixel-identical in world, which `ModBlocks` itself admitted to:
// "identical in every respect but their name and, for now, share the one post
// model and texture; making each look like the trade it belongs to is open work."
// This is that work.
//
//   node neoforge-26.1.2/tools/bake-villager-posts.mjs
//
// WHY A TOOL AND NOT TEN HAND-DRAWN PNGS. Same reason as
// `bake-item-recolours.mjs`: every post is the same piece of carpentry with a
// different trade laid on it, so the carpentry should exist once. Redrawing the
// barrel would mean redrawing it five times and keeping five copies in step. The
// motifs below ARE hand-drawn - they are pixel maps, one character per pixel,
// which is the honest way to author sixteen-square art in a file somebody has to
// review later. Edit a letter, re-run, look at it.
//
// WHAT IS SHARED ON PURPOSE. The bottom face. All five keep post_bottom - which
// is the old horse_traders_post_bottom.png, renamed in the same change. The wiki
// page had argued the fossil name should be fixed "when the four stop sharing one
// set", and this is that moment, with every model already being rewritten anyway.
// It stays shared because it is the underside of a block, it is never seen,
// and five copies of a plank texture would be five things to keep in step for
// nothing. The duplicate that mattered was the one you could walk up to.
//
// THE COLOURS ARE NOT NEW. Each post is keyed to the hat its villager already
// wears, from `tools/villagers/bake-profession-hats.ps1` - russet for the
// leatherworker, teal for the scientist, olive for the supplier, steel for the
// metalsmith. A player who has learned the hats has already learned the posts.
// The cowboy's hitch keeps the original black iron: it is the plain one, and the
// cowboy's identity is the hat he never takes off rather than his post.
import fs from 'node:fs';
import path from 'node:path';
import { readPng, writePng } from './png.mjs';

const ROOT = path.resolve(import.meta.dirname, '../..');
const OUT = path.join(ROOT,
  'neoforge-26.1.2/src/main/resources/assets/horsegenetics/textures/block');

/**
 * <b>Pristine copies of the shared carpentry.</b> Read-only, and the reason this
 * tool can be run twice - exactly the property `bake-item-recolours.mjs` keeps
 * its own sources for. These are the original `horse_traders_post_*` art
 * converted to 8-bit RGBA, because two of the three shipped as 4-bit indexed
 * PNGs and `png.mjs` refuses those.
 */
const SRC = path.join(ROOT, 'neoforge-26.1.2/tools/villager-posts');

/** The barrel's iron hoops, darkest first. Recoloured per post. */
const HOOP = ['28221c', '302c29', '453f3b'].map(h => h.padStart(6, '0'));

const hex = h => [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)];

/**
 * A motif is sixteen strings of sixteen characters. '.' keeps the pixel under
 * it, so the barrel shows through everywhere the trade does not cover; any other
 * character is a key into the post's own palette.
 */
const POSTS = [
  {
    id: 'cowboy_hitch',
    // Black iron, unchanged: the plain post the other four are variations on.
    hoop: null,
    palette: { r: 'd8ba80', o: 'a8853f', i: '453f3b', k: '28221c', s: '8a8078' },
    // The hitching ring itself. Drawn in a lighter iron than the hoops above and
    // below it - at this size the hoops' near-black reads as a hole, not a ring.
    side: [
      '................',
      '................',
      '................',
      '................',
      '................',
      '.....ssss.......',
      '....si..is......',
      '...s......s.....',
      '...s......s.....',
      '....si..is......',
      '.....ssss.......',
      '................',
      '................',
      '................',
      '................',
      '................',
    ],
    // A lariat, coiled and dropped on the lid.
    top: [
      '................',
      '................',
      '................',
      '................',
      '.....oooo.......',
      '...oorrrroo.....',
      '..orr.oo.rro....',
      '..or.o..o.ro....',
      '..or.o..o.ro....',
      '..orr.oo.rro....',
      '...oorrrroo.....',
      '.....oooo.......',
      '................',
      '................',
      '................',
      '................',
    ],
  },
  {
    id: 'leatherworkers_post',
    // Warm russet - the hat, and the leather.
    hoop: ['6e3820', 'a0522d', 'c8834f'],
    palette: { m: 'a0522d', l: 'c8834f', d: '6e3820', h: 'e8d7b0' },
    // A hide hung over the side to cure, stitch marks and all.
    side: [
      '................',
      '................',
      '................',
      '................',
      '................',
      '...m..mm..m.....',
      '..mllmllmllm....',
      '..mlllllllllm...',
      '..mllhllhlllm...',
      '...mlllllllm....',
      '....mmm.mmm.....',
      '................',
      '................',
      '................',
      '................',
      '................',
    ],
    // The same hide, stretched flat on the bench top.
    top: [
      '................',
      '................',
      '................',
      '................',
      '.....mmmm.......',
      '...mmllllmm.....',
      '..mllhlllhlm....',
      '..mlllllllllm...',
      '..mlllllllllm...',
      '..mllhlllhlm....',
      '...mmllllmm.....',
      '.....mmmm.......',
      '................',
      '................',
      '................',
      '................',
    ],
  },
  {
    id: 'scientists_post',
    // Teal. Nothing else in the mod is teal - the hat tool's own reasoning.
    hoop: ['124c4c', '1e7a7a', '3fb3b0'],
    palette: { g: 'bfe9e6', m: '1e7a7a', d: '124c4c', p: 'e8e2cf' },
    // Three tubes in a rack, filled to three different heights.
    side: [
      '................',
      '................',
      '................',
      '................',
      '................',
      '...gg..gg..gg...',
      '...gg..gg..mm...',
      '...gg..mm..mm...',
      '...mm..mm..mm...',
      '...mm..mm..mm...',
      '...dd..dd..dd...',
      '................',
      '................',
      '................',
      '................',
      '................',
    ],
    // From above: the mouth of a full vial, and the notes beside it.
    top: [
      '................',
      '................',
      '................',
      '................',
      '....gggg........',
      '...gmmmmg.......',
      '..gmmmmmmg......',
      '..gmmmmmmg......',
      '...gmmmmg.......',
      '....gggg........',
      '........pppppp..',
      '........p....p..',
      '........pppppp..',
      '................',
      '................',
      '................',
    ],
  },
  {
    id: 'suppliers_post',
    // Olive green, "for the one who sells carrots and rope" - and so both are here.
    hoop: ['2f4a19', '4e7a2a', '8fc24a'],
    palette: { s: 'c9a86b', d: '8a6f3c', h: 'e07a20', m: '4e7a2a', l: '8fc24a' },
    // A grain sack, and a bunch of carrots hung to dry.
    side: [
      '................',
      '................',
      '................',
      '................',
      '................',
      '....ss....l..l..',
      '...ssss...mhhm..',
      '..ssssss..hhhh..',
      '..ssssss...hh...',
      '..sddds.....h...',
      '..dddddd........',
      '................',
      '................',
      '................',
      '................',
      '................',
    ],
    // Three carrots, tops on. A coil of rope was the first draft and it was too
    // near the cowboy's lariat to tell apart from across a village.
    top: [
      '................',
      '................',
      '................',
      '................',
      '....ll.ll.ll....',
      '....mm.mm.mm....',
      '....hh.hh.hh....',
      '....hh.hh.hh....',
      '....hh.hh.hh....',
      '.....h..h..h....',
      '................',
      '................',
      '................',
      '................',
      '................',
      '................',
    ],
  },
  {
    id: 'metalsmiths_post',
    // Cold steel blue-grey, "for the one who sells metal".
    hoop: ['333b45', '55606e', '9aa7b8'],
    palette: { l: '9aa7b8', m: '55606e', d: '333b45', h: 'd2603a' },
    // A horseshoe on a nail. The one shape nobody has to be told the meaning of.
    side: [
      '................',
      '................',
      '................',
      '................',
      '................',
      '.....llll.......',
      '....ll..ll......',
      '...ld....dl.....',
      '...l......l.....',
      '...l......l.....',
      '...ll....ll.....',
      '................',
      '................',
      '................',
      '................',
      '................',
    ],
    // A steel plate, riveted at the corners, still warm at two of them.
    top: [
      '................',
      '................',
      '................',
      '................',
      '..llllllllll....',
      '..lmmmmmmmml....',
      '..lmhmmmmhml....',
      '..lmmmmmmmml....',
      '..lmmmmmmmml....',
      '..lmhmmmmhml....',
      '..lmmmmmmmml....',
      '..dddddddddd....',
      '................',
      '................',
      '................',
      '................',
    ],
  },
];

function stamp(base, rows, palette) {
  const px = Buffer.from(base.px);
  if (rows.length !== 16) throw new Error(`motif is ${rows.length} rows, want 16`);
  for (let y = 0; y < 16; y++) {
    if (rows[y].length !== 16) throw new Error(`motif row ${y} is ${rows[y].length} wide, want 16`);
    for (let x = 0; x < 16; x++) {
      const c = rows[y][x];
      if (c === '.') continue;
      const h = palette[c];
      if (!h) throw new Error(`motif uses '${c}', which is not in the palette`);
      const [r, g, b] = hex(h);
      const i = (y * 16 + x) * 4;
      px[i] = r; px[i + 1] = g; px[i + 2] = b; px[i + 3] = 255;
    }
  }
  return px;
}

/**
 * <b>Take the latch off the lid.</b> The shared top art has a four-pixel grey
 * catch at (3..4, 8..9), which was fine when there was one post and is not fine
 * now: every trade motif is drawn over the middle of the lid, and the latch sat
 * inside three of them looking like a hole punched in the hide. It is painted
 * back to the two wood tones around it before anything is stamped.
 */
const LATCH = { '7a7a7a': '805e36', '5b5b5b': '6b5332' };
function unlatch(px) {
  const out = Buffer.from(px);
  for (let i = 0; i < out.length; i += 4) {
    const h = [out[i], out[i + 1], out[i + 2]]
      .map(v => v.toString(16).padStart(2, '0')).join('');
    const to = LATCH[h];
    if (!to) continue;
    const [r, g, b] = hex(to);
    out[i] = r; out[i + 1] = g; out[i + 2] = b;
  }
  return out;
}

/** Repaint the barrel's iron hoops in the post's own metal, shade for shade. */
function reHoop(px, hoop) {
  if (!hoop) return px;
  const out = Buffer.from(px);
  for (let i = 0; i < out.length; i += 4) {
    const h = [out[i], out[i + 1], out[i + 2]]
      .map(v => v.toString(16).padStart(2, '0')).join('');
    const k = HOOP.indexOf(h);
    if (k < 0) continue;
    const [r, g, b] = hex(hoop[k]);
    out[i] = r; out[i + 1] = g; out[i + 2] = b;
  }
  return out;
}

const baseTop = readPng(path.join(SRC, 'post_top.png'));
baseTop.px = unlatch(baseTop.px);
const baseSide = readPng(path.join(SRC, 'post_side.png'));

let wrote = 0;
for (const p of POSTS) {
  for (const [face, base] of [['top', baseTop], ['side', baseSide]]) {
    const body = face === 'side' ? reHoop(base.px, p.hoop) : base.px;
    const px = stamp({ px: body }, p[face], p.palette);
    const file = path.join(OUT, `${p.id}_${face}.png`);
    writePng(file, 16, 16, px);
    wrote++;
  }
}
console.log(`villager posts: wrote ${wrote} textures for ${POSTS.length} posts`);
console.log('the bottom face stays shared - post_bottom, an underside nobody sees');
