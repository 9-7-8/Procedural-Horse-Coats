// Item sprites that are somebody else's art in a different colour.
//
// Owner, 2026-09-29: "recolor every single item we have, other than the breed
// eggs, which is currently identical to something else within this pack or
// within minecraft". `check-duplicate-item-art.mjs` is what says WHICH; this is
// what does it, and the two are deliberately separate - the check must be able
// to fail on art this tool has never touched.
//
//   node neoforge-26.1.2/tools/bake-item-recolours.mjs [--vanilla-jar <path>]
//
// Every sprite it writes is listed in RECIPES below with its source and its
// numbers, so re-tuning a colour is a one-line edit and re-running. Nothing else
// in textures/item/ is touched: the hand-drawn sprites are not derived from
// anything and this must never overwrite one.
//
// It is IDEMPOTENT, and that took care rather than luck - see SOURCES.
//
// WHY A TOOL AND NOT TWELVE HAND-EDITED PNGS. Four of these are derived from
// VANILLA art, which moves when the game version does. A hand-edited copy would
// silently keep a 26.1.2 bundle on the vet's kit for ever and nobody would know
// why it looked wrong; re-running this against a newer client jar re-derives it.
// The trade is that the recolour is a formula rather than a drawing, which is
// honest about what it is - these are recolours, not artwork, and the roster
// wants a real artist eventually.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { readPng, writePng, rgb2hsl, hsl2rgb } from './png.mjs';

const ROOT = path.resolve(import.meta.dirname, '../..');
const TEXTURES = path.join(ROOT,
  'neoforge-26.1.2/src/main/resources/assets/horsegenetics/textures/item');
const BLOCK_TEXTURES = path.join(ROOT,
  'neoforge-26.1.2/src/main/resources/assets/horsegenetics/textures/block');

/**
 * <b>Pristine copies of the sprites this bake derives from.</b> Read-only, and
 * the reason this tool can be run twice.
 *
 * <p>Several recipes derive a sprite from the art of the item ITSELF - a splice
 * carrot rotated away from its neighbours is still that carrot. If those read
 * the live texture they would rotate the already-rotated output on every run,
 * so the fifth run of a "no-op" re-bake would quietly hand back a different
 * palette. Keeping the sources in a folder nothing writes to makes the bake a
 * function of its inputs, which is the only property that lets a checked-in
 * generated asset be trusted.
 */
const SOURCES = path.join(ROOT, 'neoforge-26.1.2/tools/item-art');

/**
 * `mode` picks between the two ways of moving a colour, and which one is right
 * depends entirely on the source art:
 *
 *   'rotate' - shift every hue by the same amount, keeping the relationships
 *              inside the sprite. Right for art with SEVERAL colours in it (a
 *              sign's board against its post), and it does nothing at all to a
 *              grey pixel, which is why it is not the default.
 *   'flat'   - give every opaque pixel one hue and one saturation, keeping only
 *              the source's LIGHTNESS, which is what carries the drawing. Right
 *              for art that is already one colour, and the only thing that works
 *              at all on near-grey art. It flattens a two-colour sprite, so it
 *              is never used on one.
 *
 * `light` is added to lightness (-1..1) in both modes. `sat` means different
 * things by mode and this is deliberate: under 'rotate' it MULTIPLIES the
 * source's saturation, because the point of rotating is to keep the source's
 * own relationships; under 'flat' it is the ABSOLUTE saturation every pixel
 * gets.
 *
 * <p>That second half was a real bug and is worth the paragraph. While 'flat'
 * multiplied, a recolour of a low-saturation sprite came out at
 * source-saturation times the multiplier - so blank_transfer_paper, derived
 * from a book already at 0.40, landed at 0.18 and was still a brown book.
 * It passed the duplicate check, because the pixels genuinely differed, and was
 * indistinguishable from its source to the eye. A recolour nobody can see is
 * the failure this whole pass exists to fix.
 */
const RECIPES = [
  // A NOTE ON THE PALETTE. These hues are chosen against each other and against
  // the sprites that are NOT recoloured, so the whole roster is separable. The
  // fixed points are horse_ticket (0.76, the blank stall ticket),
  // horse_whistle (0.05, the basic whistle), research_paper (0.12) and the
  // jockey passes (0.34) - a new colour has to clear all four as well as its
  // own neighbours. Turnout is red on purpose rather than for spacing: it is
  // the one ticket that cannot be undone.

  // WHAT LEFT THIS LIST, 2026-09-29. The vet's kit is now a licensed Raven icon
  // (see THIRD_PARTY_NOTICES.md) and the three stall signs are drawn by
  // `bake-drawn-items.mjs`. Both had to leave, not merely be ignored: this tool
  // writes straight over textures/item/, so a recipe left behind would have
  // quietly repainted the new art on the next re-bake.

  // --- derived from VANILLA art -----------------------------------------
  {
    out: 'custom_horse_spawn_egg', from: 'minecraft:horse_spawn_egg', mode: 'flat',
    hue: 0.50, sat: 0.70, light: 0.0,
    note: "The editor egg. Cyan - it is a dev tool that opens a genome editor, and looking "
        + "nothing like a spawn egg is the point.",
  },
  {
    out: 'preset_horse_spawn_egg', from: 'minecraft:horse_spawn_egg', mode: 'flat',
    hue: 0.88, sat: 0.65, light: -0.02,
    note: "The egg a SAVED horse lives in - a resurrection, or one kept aside. Magenta, well "
        + "away from the editor egg it sits beside in the same tab.",
  },


  // --- the golden carrot crop, which is a BLOCK ---------------------------
  // Owner, 2026-09-29: "a flat gold recolor is fine". The crop was vanilla's
  // carrot art tinted gold at runtime, which is Mojang's file on the ground with
  // a filter over it; these are ours. Flat mode with no saturation floor trouble:
  // carrot foliage is green and the root orange, and flattening both to one gold
  // hue is exactly what "a golden carrot" should look like.
  {
    out: 'golden_carrot_stage0', from: 'minecraft:carrots_stage0', folder: 'block',
    mode: 'flat', hue: 0.125, sat: 0.72, light: 0.06,
    note: "Growth stage 0 of four.",
  },
  {
    out: 'golden_carrot_stage1', from: 'minecraft:carrots_stage1', folder: 'block',
    mode: 'flat', hue: 0.125, sat: 0.72, light: 0.06,
    note: "Growth stage 1 of four.",
  },
  {
    out: 'golden_carrot_stage2', from: 'minecraft:carrots_stage2', folder: 'block',
    mode: 'flat', hue: 0.125, sat: 0.72, light: 0.06,
    note: "Growth stage 2 of four.",
  },
  {
    out: 'golden_carrot_stage3', from: 'minecraft:carrots_stage3', folder: 'block',
    mode: 'flat', hue: 0.125, sat: 0.72, light: 0.06,
    note: "Growth stage 3 of four.",
  },

  // --- derived from OUR OWN art ------------------------------------------
  // The five tickets were one sprite. The blank keeps it; the four that do
  // something each get a colour, so a hotbar of tickets can be read without
  // hovering every one.
  {
    out: 'basic_ticket', from: 'horse_ticket', mode: 'flat', hue: 0.60, sat: 0.60, light: 0.0,
    note: "Blue. The ordinary stall ticket, and the one there will be most of.",
  },
  {
    out: 'bound_ticket', from: 'horse_ticket', mode: 'flat', hue: 0.08, sat: 0.70, light: 0.0,
    note: "Amber. Costs an ender pearl and reaches further than the basic one.",
  },
  {
    out: 'interdimensional_ticket', from: 'horse_ticket', mode: 'flat',
    hue: 0.92, sat: 0.70, light: 0.02,
    note: "Magenta. The one that crosses dimensions, and the most expensive.",
  },
  {
    out: 'turnout_ticket', from: 'horse_ticket', mode: 'flat', hue: 0.99, sat: 0.75, light: -0.02,
    note: "Red, and not for spacing. This is the one ticket that is NOT a stall ticket: it "
        + "gives the horse up to the realm and cannot be undone. Red is what that should look "
        + "like sitting in a row of blue ones.",
  },

  // The four whistles were one sprite, and two of them have a colour already
  // implied by what they are made of.
  {
    out: 'golden_whistle', from: 'horse_whistle', mode: 'flat', hue: 0.13, sat: 0.75, light: 0.04,
    note: "Gold, which is what it is made of.",
  },
  {
    out: 'echo_whistle', from: 'horse_whistle', mode: 'flat', hue: 0.55, sat: 0.28, light: 0.10,
    note: "The pale blue-grey of an echo shard, which is what it is made of. Deliberately the "
        + "washed-out one: it is the quiet whistle in a row of loud ones.",
  },
  {
    out: 'ender_whistle', from: 'horse_whistle', mode: 'flat', hue: 0.78, sat: 0.70, light: -0.02,
    note: "Ender purple. It teleports the horse to you rather than calling it.",
  },
  {
    out: 'command_whistle', from: 'horse_whistle', mode: 'flat', hue: 0.33, sat: 0.55, light: 0.0,
    note: "Green, for go: it gives orders rather than calling. Not amethyst purple, although "
        + "a shard is in the recipe, because the ender whistle already owns purple.",
  },

  // Three papers were one sprite - which is drawn as a BOOK, and has been since
  // both items existed. The research paper keeps it.
  {
    out: 'blank_transfer_paper', from: 'research_paper', mode: 'flat',
    hue: 0.12, sat: 0.22, light: 0.26,
    note: "Pale cream, and much lighter rather than merely a different brown. Closes the oldest "
        + "known gap on the item roster. Blank is the thing to say about it, so it is the "
        + "palest object in the roster.",
  },
  {
    out: 'signed_transfer_paper', from: 'research_paper', mode: 'flat',
    hue: 0.62, sat: 0.50, light: -0.06,
    note: "Ink blue, dark. Only ever seen as the particles a signed paper throws when it breaks "
        + "- the item itself is drawn as the horse - but those particles should not be a "
        + "research paper's.",
  },



  // --- the two families the strengthened check turned up -----------------
  // Neither of these was IDENTICAL to anything, which is why they survived the
  // first pass: they are near-identical, and near-identical is the thing the
  // player actually cannot get past. They are recoloured on the same terms.

  // Five themed splice carrots, all of them a green top over a tinted body
  // within a fifth of a turn of each other. ROTATED rather than flattened,
  // because flattening one would stop it looking like a carrot - the whole
  // sprite would become a single-hue blob and the family would lose the thing
  // that says what it is.
  {
    out: 'performance_gene_splice_carrot', from: 'performance_gene_splice_carrot',
    mode: 'rotate', hue: 0.53, sat: 1.1, light: 0.0,
    note: "Red. Speed, jump and stamina - the carrot that makes a horse do more.",
  },
  {
    out: 'magical_gene_splice_carrot', from: 'magical_gene_splice_carrot',
    mode: 'rotate', hue: 0.37, sat: 1.1, light: 0.0,
    note: "Purple, which is what every magical thing in this pack already is.",
  },
  {
    out: 'dilution_gene_splice_carrot', from: 'dilution_gene_splice_carrot',
    mode: 'rotate', hue: 0.90, sat: 0.9, light: 0.08,
    note: "Pale gold. The dilutions LIGHTEN a coat, so the carrot is the lightest of the five.",
  },
  {
    out: 'marking_gene_splice_carrot', from: 'marking_gene_splice_carrot',
    mode: 'rotate', hue: 0.39, sat: 1.0, light: 0.0,
    note: "Blue. White markings - and blue rather than white because a white carrot at 16 "
        + "pixels is a grey smudge.",
  },
  // white_gene_splice_carrot keeps its own art: one of every family stays put,
  // and the other four move away from it.

  // The five occupied stasis chambers - a horse in a bottle, which is mostly
  // pale glass whatever tier it is, so the tier's colour had almost no sprite
  // left to live in. Each takes its EMPTY twin's hue: a full chamber should read
  // as the same object as the empty one it came from, and the empty five are
  // already distinguishable from each other.
  {
    out: 'occupied_basic_stasis_chamber', from: 'occupied_basic_stasis_chamber',
    mode: 'flat', hue: 0.55, sat: 0.45, light: 0.02,
    note: "Basic: the empty one's blue, saturated enough to survive the glass.",
  },
  {
    out: 'occupied_intermediate_stasis_chamber', from: 'occupied_intermediate_stasis_chamber',
    mode: 'flat', hue: 0.44, sat: 0.55, light: 0.0,
    note: "Intermediate: the empty one's teal.",
  },
  {
    out: 'occupied_advanced_stasis_chamber', from: 'occupied_advanced_stasis_chamber',
    mode: 'flat', hue: 0.31, sat: 0.60, light: 0.0,
    note: "Advanced: the empty one's green.",
  },
  {
    out: 'occupied_emergency_stasis_chamber', from: 'occupied_emergency_stasis_chamber',
    mode: 'flat', hue: 0.14, sat: 0.65, light: 0.02,
    note: "Emergency: the empty one's hue pushed to gold, which is as far from the other four "
        + "as its own family allows. This is the chamber that fires by itself when a horse is "
        + "about to die, so it is the one worth spotting in a full inventory.",
  },
  {
    out: 'occupied_spacer_stasis_chamber', from: 'occupied_spacer_stasis_chamber',
    mode: 'flat', hue: 0.72, sat: 0.45, light: 0.0,
    note: "Spacer: violet, and deliberately outside the tier ramp. A spacer is not a better "
        + "chamber, it is a different kind of thing, and it should not read as the top of a "
        + "ladder the other four are on.",
  },];

// --- vanilla source art --------------------------------------------------
const args = process.argv.slice(2);
const jarArg = args.indexOf('--vanilla-jar');
const VANILLA_JAR = jarArg >= 0 ? args[jarArg + 1] : defaultJar();

function defaultJar() {
  const cache = path.join(os.homedir(), '.gradle/caches/neoformruntime/artifacts');
  if (!fs.existsSync(cache)) return null;
  const hit = fs.readdirSync(cache).find(f => /^minecraft_.*_client\.jar$/.test(f));
  return hit ? path.join(cache, hit) : null;
}

const vanillaDirs = {};
/**
 * Vanilla art to derive from. `folder` is 'item' or 'block' - the crop stages
 * live under textures/block, and they are the only reason this takes an argument.
 */
function vanillaPath(name, folder = 'item') {
  if (!vanillaDirs[folder]) {
    if (!VANILLA_JAR || !fs.existsSync(VANILLA_JAR)) {
      throw new Error('No Minecraft client jar found, and this bake needs vanilla art to derive '
        + 'from. Pass --vanilla-jar <path>, or run any Gradle task once so the jar lands in '
        + '~/.gradle/caches/neoformruntime/artifacts.');
    }
    const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'vanilla-src-'));
    execFileSync('jar', ['xf', VANILLA_JAR, `assets/minecraft/textures/${folder}`], { cwd: tmp });
    vanillaDirs[folder] = path.join(tmp, 'assets/minecraft/textures', folder);
  }
  return path.join(vanillaDirs[folder], name + '.png');
}

// --- the recolour --------------------------------------------------------
function recolour(src, r) {
  const px = Buffer.from(src.px);
  for (let i = 0; i < px.length; i += 4) {
    if (px[i + 3] === 0) continue;
    const [h, s, l] = rgb2hsl(px[i], px[i + 1], px[i + 2]);
    const hue = r.mode === 'rotate' ? (h + r.hue) % 1 : r.hue;
    // Under 'flat' a grey pixel has s === 0 and would stay grey however the hue
    // moved, which is exactly how a half-recoloured sprite happens. The floor is
    // what stops that; under 'rotate' the source's own relationships are the
    // point and nothing is lifted.
    // See the note on RECIPES: multiply when rotating, set when flattening.
    const sat = r.mode === 'rotate' ? s * (r.sat ?? 1) : (r.sat ?? 0.6);
    const [rr, gg, bb] = hsl2rgb(hue, Math.min(1, sat),
      Math.max(0, Math.min(1, l + (r.light ?? 0))));
    px[i] = rr; px[i + 1] = gg; px[i + 2] = bb;
  }
  return px;
}

let wrote = 0;
for (const r of RECIPES) {
  const vanilla = r.from.startsWith('minecraft:');
  const srcPath = vanilla
    ? vanillaPath(r.from.slice('minecraft:'.length), r.folder ?? 'item')
    : path.join(SOURCES, r.from + '.png');
  if (!fs.existsSync(srcPath)) {
    console.error(`! ${r.out}: source ${r.from} not found at ${srcPath}`);
    process.exitCode = 1;
    continue;
  }
  const src = readPng(srcPath);
  const outDir = r.folder === 'block' ? BLOCK_TEXTURES : TEXTURES;
  writePng(path.join(outDir, r.out + '.png'), src.w, src.h, recolour(src, r));
  wrote++;
}

for (const d of Object.values(vanillaDirs)) {
  fs.rmSync(path.dirname(path.dirname(path.dirname(d))), { recursive: true, force: true });
}
console.log(`item recolours: ${wrote}/${RECIPES.length} sprites written -> ${path.relative(ROOT, TEXTURES)}`);
console.log('Now run check-duplicate-item-art.mjs - it is the thing that says whether it worked.');
