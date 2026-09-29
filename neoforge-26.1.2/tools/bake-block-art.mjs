// The mod's own block art: the five job-site posts, the dyeing bench, the
// research shelf.
//
// Owner, 2026-09-29: the five posts - cowboy_hitch, leatherworkers_post,
// metalsmiths_post, scientists_post, suppliers_post - all pointed at the ONE
// texture set, horse_traders_post_{top,side,bottom}. Five different workstations
// that were pixel-identical in world, which `ModBlocks` itself admitted to:
// "identical in every respect but their name and, for now, share the one post
// model and texture; making each look like the trade it belongs to is open work."
// This is that work.
//
//   node neoforge-26.1.2/tools/bake-block-art.mjs
//
// The bench and the shelf are here for a different reason than the posts. They
// wore VANILLA art - the bench was the smithing table, the shelf was the
// bookshelf - so a player who put a research shelf next to a bookshelf saw two
// bookshelves, and neither block could have a 3D preview on the wiki, because
// the wiki may not ship Mojang's textures. Drawing them fixes both at once.
//
// WHY A TOOL AND NOT HAND-DRAWN PNGS. Same reason as
// `bake-item-recolours.mjs`: every post is the same piece of carpentry with a
// different trade laid on it, so the carpentry should exist once. Redrawing the
// barrel would mean redrawing it five times and keeping five copies in step. The
// motifs below ARE hand-drawn - they are pixel maps, one character per pixel,
// which is the honest way to author sixteen-square art in a file somebody has to
// review later. Edit a letter, re-run, look at it.
//
// WHAT IS SHARED ON PURPOSE. The plain plank face, plank_bottom.png. It is the
// underside of the five posts and of the bench, and it is the plank grain every
// other face here is drawn on top of. It has now been renamed twice in one day -
// horse_traders_post_bottom -> post_bottom -> plank_bottom - and the second
// rename is the admission that the first was right for a family of one and wrong
// the moment a second family shared the file. It stays shared because an
// underside is never seen,
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
const SRC = path.join(ROOT, 'neoforge-26.1.2/tools/block-art');

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


/**
 * <b>The three workstations.</b> Unlike the posts these are not a motif on a
 * barrel - a bench is not a barrel - so each face is a full pixel map drawn over
 * the plain plank grain, which is the one piece of carpentry worth sharing.
 * Faces are named for the model slot they fill: `cube_bottom_top` wants top and
 * side, `cube_column` wants end and side.
 *
 * <p>The stasis bank is the exception to "drawn over the plank grain": its map has
 * no `.` in it anywhere, so the plank never shows through. That is on purpose
 * rather than an oversight - the bench and the shelf are furniture and want to
 * look like the wood they are made of, and the bank is a machine full of glass.
 */
const WORKSTATIONS = [
  {
    id: 'equestrian_bench',
    // Dark oak frame, iron fittings, and the three dyes the bench actually uses
    // on a saddle - one for the seat, one for the bridle, one for the metal.
    palette: {
      d: '4e3520', i: '6a6a72', I: '9aa0aa',
      r: 'b03a3a', b: '3a5ab0', y: 'd2b23a',
      L: '6e3820', l: 'a0522d',
    },
    faces: {
      // Looking down at the bench: three open dye pots at the back, a saddle
      // panel laid out at the front with its iron fittings showing.
      top: [
        '................',
        '.dddd.dddd.dddd.',
        '.drrd.dbbd.dyyd.',
        '.drrd.dbbd.dyyd.',
        '.dddd.dddd.dddd.',
        '................',
        '................',
        '..LLLLLLLLLLLL..',
        '..LllllllllllL..',
        '..LlllddlllllL..',
        '..LllllllllllL..',
        '..LllllllllllL..',
        '..LLLLLLLLLLLL..',
        '................',
        '................',
        '................',
      ],
      // The front of it: a rail top and bottom, two drawers with iron pulls, and
      // the dye that has got onto the boards underneath and stayed there.
      side: [
        'dddddddddddddddd',
        'dddddddddddddddd',
        '................',
        '.dddddd..dddddd.',
        '.d....d..d....d.',
        '.d.II.d..d.II.d.',
        '.d....d..d....d.',
        '.dddddd..dddddd.',
        '................',
        '................',
        '..rr...bb...yy..',
        '..rr...bb...yy..',
        '................',
        '................',
        'dddddddddddddddd',
        'dddddddddddddddd',
      ],
    },
  },
  {
    id: 'equine_research_shelf',
    // Paper and ink against the same dark frame, so the two workstations read as
    // a matched pair of furniture rather than two unrelated blocks.
    palette: { d: '4e3520', P: 'e8e2cf', p: 'c3bda6', k: '2a2a35', t: 'a0522d' },
    faces: {
      // Two shelves of filed papers, some of them written on.
      side: [
        'dddddddddddddddd',
        'dddddddddddddddd',
        '.PpP.PpP.PpP.tP.',
        '.PkP.PpP.PkP.tP.',
        '.PpP.PkP.PpP.tP.',
        '.PpP.PpP.PpP.tP.',
        'dddddddddddddddd',
        'dddddddddddddddd',
        '..PpP.PkP.PpP...',
        '..PkP.PpP.PpP...',
        '..PpP.PpP.PkP...',
        '..PpP.PpP.PpP...',
        'dddddddddddddddd',
        'dddddddddddddddd',
        '................',
        '................',
      ],
      // The top of the cabinet: a framed plank lid with one paper left on it.
      end: [
        'dddddddddddddddd',
        'd..............d',
        'd..............d',
        'd...PPPPPP.....d',
        'd...PkkkpP.....d',
        'd...PppppP.....d',
        'd...PkkppP.....d',
        'd...PPPPPP.....d',
        'd..............d',
        'd..............d',
        'd..............d',
        'd..............d',
        'd..............d',
        'd..............d',
        'd..............d',
        'dddddddddddddddd',
      ],
    },
  },
  {
    id: 'horse_stasis_bank',
    // Not wood. The bank holds chambers and reads the horses inside them, so it
    // is drawn as a rack of them: the palette is lifted straight off
    // basic_stasis_chamber.png and its occupied twin, so the block and the item
    // it stores are recognisably the same object at two sizes.
    palette: {
      D: '2b2b33', d: '3e3e49', m: '6f7280',
      g: 'c3dbe8', G: 'e8f4fa', b: '97c2e0',
      t: '1d3d4c', c: '6babc7',
    },
    faces: {
      // Six chambers in their slots. One of them - top right - is drawn in the
      // occupied colours, because a bank with a horse in it is the normal case
      // and an empty rack says nothing about what the block is for.
      side: [
        'DDDDDDDDDDDDDDDD',
        'DmmmmmmmmmmmmmmD',
        'DddddmddddmddddD',
        'DgGgbmgGgbmtctcD',
        'DgbgbmgbgbmtcccD',
        'DggbgmggbgmtcctD',
        'DddddmddddmddddD',
        'DmmmmmmmmmmmmmmD',
        'DddddmddddmddddD',
        'DgGgbmgGgbmgGgbD',
        'DgbgbmgbgbmgbgbD',
        'DggbgmggbgmggbgD',
        'DddddmddddmddddD',
        'DmmmmmmmmmmmmmmD',
        'DmmmmmmmmmmmmmmD',
        'DDDDDDDDDDDDDDDD',
      ],
      // The lid, and by cube_column the underside too: a recessed metal panel
      // around a lit glass port.
      end: [
        'DDDDDDDDDDDDDDDD',
        'DmmmmmmmmmmmmmmD',
        'DmDDDDDDDDDDDDmD',
        'DmDmmmmmmmmmmDmD',
        'DmDmddddddddmDmD',
        'DmDmdGbbbbGdmDmD',
        'DmDmdbGGGGbdmDmD',
        'DmDmdbGGGGbdmDmD',
        'DmDmdGbbbbGdmDmD',
        'DmDmddddddddmDmD',
        'DmDmmmmmmmmmmDmD',
        'DmDDDDDDDDDDDDmD',
        'DmmmmmmmmmmmmmmD',
        'DmmmmmmmmmmmmmmD',
        'DmmmmmmmmmmmmmmD',
        'DDDDDDDDDDDDDDDD',
      ],
    },
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

const basePlank = readPng(path.join(SRC, 'plank_bottom.png'));

let wrote = 0;
for (const p of POSTS) {
  for (const [face, base] of [['top', baseTop], ['side', baseSide]]) {
    const body = face === 'side' ? reHoop(base.px, p.hoop) : base.px;
    const px = stamp({ px: body }, p[face], p.palette);
    writePng(path.join(OUT, `${p.id}_${face}.png`), 16, 16, px);
    wrote++;
  }
}
for (const w of WORKSTATIONS) {
  for (const [face, map] of Object.entries(w.faces)) {
    const px = stamp({ px: basePlank.px }, map, w.palette);
    writePng(path.join(OUT, `${w.id}_${face}.png`), 16, 16, px);
    wrote++;
  }
}
console.log(`block art: wrote ${wrote} textures for ${POSTS.length} posts `
  + `and ${WORKSTATIONS.length} workstations`);
console.log('plank_bottom.png stays shared - the undersides, and the grain the rest is drawn on');
