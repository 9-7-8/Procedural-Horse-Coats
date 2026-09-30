// Item sprites this mod draws for itself.
//
//   node neoforge-26.1.2/tools/bake-drawn-items.mjs
//
// There are now three ways an item gets its picture here, and they are separate
// tools on purpose because they have different failure modes:
//
//   bake-item-recolours.mjs  somebody else's drawing in a different colour,
//                            derived from the vanilla jar or from our own art.
//   (a licensed icon)        copied in by hand from a pack, with its notice.
//                            See THIRD_PARTY_NOTICES.md.
//   THIS FILE                drawn here, a character per pixel.
//
// Owner, 2026-09-29: fix the stall signs. They were vanilla's oak sign hue-shifted
// blue, and the bound one was THE SAME BLUE deeper - which was a deliberate call
// ("the same object in a second state") but left the two closest things in the
// roster telling apart by saturation alone. And the Raven pack, which supplied
// the vet's kit and two others the same day, has no sign in it at all: it is an
// RPG icon set, full of potions and swords, and a wooden board on a post is not
// a fantasy prop. So these are drawn.
//
// WHAT MAKES THEM DIFFERENT IS NOW THE DRAWING, NOT THE HUE. A blank board, a
// board with a name written on it, and a board with a pen drawn on it. Put the
// three side by side at inventory size and they are three different pictures -
// which is the thing a colour shift could never buy.
import fs from 'node:fs';
import path from 'node:path';
import { writePng } from './png.mjs';

const ROOT = path.resolve(import.meta.dirname, '../..');
const OUT = path.join(ROOT,
  'neoforge-26.1.2/src/main/resources/assets/horsegenetics/textures/item');

/** Shared with every sign: the board, and the post it hangs on. */
const WOOD = {
  l: 'c8a878',   // board face
  d: 'a8865a',   // board face, shaded
  p: '6b5332',   // the post
  k: '2a2a35',   // ink
};

const ITEMS = [
  {
    id: 'stall_sign',
    // Blue is kept from the recolour it replaces: a player who learned the
    // colour keeps it, and only the drawing changes.
    palette: { ...WOOD, B: '3a6ab0' },
    map: [
      '................',
      '................',
      '.BBBBBBBBBBBBBB.',
      '.BllllllllllllB.',
      '.BllllllllllllB.',
      '.BlldddddddddlB.',
      '.BllllllllllllB.',
      '.BllllllllllllB.',
      '.BlldddddddddlB.',
      '.BBBBBBBBBBBBBB.',
      '.......pp.......',
      '.......pp.......',
      '.......pp.......',
      '.......pp.......',
      '......dppd......',
      '................',
    ],
  },
  {
    id: 'bound_stall_sign',
    // The same blue, deeper - and now a name written on the board, which is what
    // binding a sign actually does.
    palette: { ...WOOD, B: '27477a' },
    map: [
      '................',
      '................',
      '.BBBBBBBBBBBBBB.',
      '.BllllllllllllB.',
      '.BlkkkkllkkkllB.',
      '.BllllllllllllB.',
      '.BlkkllkkkkkllB.',
      '.BllllllllllllB.',
      '.BllkkkkkkklllB.',
      '.BBBBBBBBBBBBBB.',
      '.......pp.......',
      '.......pp.......',
      '.......pp.......',
      '.......pp.......',
      '......dppd......',
      '................',
    ],
  },
  {
    id: 'holding_pen_sign',
    // Amber, warm against the stall signs' blue - the holding pen is a different
    // system and the two sit in the same tab. The board carries a fence, because
    // that is what a holding pen is.
    palette: { ...WOOD, A: 'd2913a', m: '8a6a3c', r: '6b5332' },
    map: [
      '................',
      '................',
      '.AAAAAAAAAAAAAA.',
      '.AllllllllllllA.',
      '.AlmmllmmllmmlA.',
      '.AlmmllmmllmmlA.',
      '.AlrrrrrrrrrrlA.',
      '.AlmmllmmllmmlA.',
      '.AlmmllmmllmmlA.',
      '.AAAAAAAAAAAAAA.',
      '.......pp.......',
      '.......pp.......',
      '.......pp.......',
      '.......pp.......',
      '......dppd......',
      '................',
    ],
  },
];

let wrote = 0;
for (const item of ITEMS) {
  const px = Buffer.alloc(16 * 16 * 4);   // transparent
  if (item.map.length !== 16) throw new Error(`${item.id}: ${item.map.length} rows, want 16`);
  for (let y = 0; y < 16; y++) {
    if (item.map[y].length !== 16) {
      throw new Error(`${item.id}: row ${y} is ${item.map[y].length} wide, want 16`);
    }
    for (let x = 0; x < 16; x++) {
      const c = item.map[y][x];
      if (c === '.') continue;
      const hex = item.palette[c];
      if (!hex) throw new Error(`${item.id}: '${c}' is not in its palette`);
      const i = (y * 16 + x) * 4;
      px[i] = parseInt(hex.slice(0, 2), 16);
      px[i + 1] = parseInt(hex.slice(2, 4), 16);
      px[i + 2] = parseInt(hex.slice(4, 6), 16);
      px[i + 3] = 255;
    }
  }
  writePng(path.join(OUT, `${item.id}.png`), 16, 16, px);
  wrote++;
}
console.log(`drawn items: ${wrote} sprite(s) -> textures/item/`);
console.log('Now run check-duplicate-item-art.mjs - it is the thing that says whether it worked.');
