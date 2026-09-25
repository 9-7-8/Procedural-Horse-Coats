#!/usr/bin/env node
// Two wiki pages print a height range per breed, and both are hand-typed:
//
//   wiki/breeds.html       the #commonness table, third column
//   wiki/breed-book.html   the <span class="tag"> after each breed's <dt>
//
// Both are derived from `stats.size` in the breed JSON, and nothing recomputed
// them, so a wrong digit could sit there indefinitely - which is exactly what
// happened: the Dhampir's breed-book tag read 15.0-17.3 hh for a breed whose
// size band tops out at 16.3. CLAUDE.md's rule is that a derived number lives
// in the code that computes it, so the fix is this file, not just the digit.
//
// THE TWO PAGES USE DIFFERENT CONVENTIONS AND BOTH ARE CORRECT - do not
// "unify" them:
//
//   breeds.html      STABLE NOTATION, a port of BreedStatCurve.formatHands:
//                    15.3 hh is fifteen hands and three *inches*, so the digit
//                    after the point only ever runs 0..3.
//   breed-book.html  DECIMAL hands to one place. The Cloudtouched's 15.9 is
//                    the proof: there is no such stable figure.
//
//   node wiki/tools/check-breed-heights.mjs
//
// Exit 0 and one summary line when clean; exit 1 and a list of mismatches when
// not, in the shape of the other checkers in this directory.

import { readFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const wiki = join(dirname(fileURLToPath(import.meta.url)), "..");
const repo = join(wiki, "..");

const CURVE = join(
  repo,
  "common/src/main/java/com/example/horsegenetics/common/breed/BreedStatCurve.java"
);

// ---------------------------------------------------------------------------
// The curve's constants are READ OUT OF THE JAVA, never typed here.
//
// This checker exists to police exactly one thing - a derived number written
// down a second time and left to rot. Pasting BASELINE_HH or BIG_GAMMA into
// this file would make it that same defect: change the baseline in Java and
// the checker would go green against the stale copy, confirming the wiki
// against a number the game no longer uses. So they are scraped at run time,
// and a rename breaks this tool loudly instead of quietly defaulting.
// ---------------------------------------------------------------------------

const curveSrc = readFileSync(CURVE, "utf8");

function constant(name) {
  const m = curveSrc.match(
    new RegExp(
      `(?:public|private|protected)?\\s*static\\s+final\\s+double\\s+${name}\\s*=\\s*([-+0-9.eE]+)\\s*;`
    )
  );
  if (!m) {
    console.error(
      `check-breed-heights: cannot find the declaration of ${name} in\n  ${CURVE}\n\n` +
        "This checker reads the height curve's constants out of the Java on purpose, so\n" +
        "that it never validates the wiki against a stale copy of them. If the constant\n" +
        "was renamed or moved, follow it here; do NOT hardcode its value."
    );
    process.exit(1);
  }
  return Number(m[1]);
}

const BASELINE_HH = constant("BASELINE_HH");
const BIG_GAMMA = constant("BIG_GAMMA");

/** BreedStatCurve.handsFor - a body scale back to hands. */
function handsFor(scale) {
  const ratio = scale > 1.0 ? 1.0 + (scale - 1.0) / BIG_GAMMA : scale;
  return BASELINE_HH * ratio;
}

/** BreedStatCurve.formatHands, minus the " hh" the pages write separately. */
function stableHands(hands) {
  const inches = Math.round(Math.max(0.0, hands) * 4.0);
  return `${Math.floor(inches / 4)}.${inches % 4}`;
}

/** The breed book's convention: plain decimal hands, one place, half up. */
function decimalHands(hands) {
  return (Math.round(hands * 10) / 10).toFixed(1);
}

/**
 * `size` is written to three decimals, and it is a ROUNDING of an exact height
 * the breed was originally specified in - sizeForHands(13.75) is 0.873016 and
 * the file says 0.873. Multiplying that back out lands at 13.74975, a hair
 * under the halfway point, so a strict port would demand 13.7 where the page
 * correctly prints 13.8. That is the stored precision talking, not a wrong
 * digit, so a printed figure is accepted if it is what ANY scale inside the
 * stored value's own rounding interval formats to. The cost is that this
 * checker cannot see an error of a single step; the benefit is that it never
 * asks for a correct figure to be made wrong. Errors that matter - the Dhampir
 * was a whole hand out - clear this by a wide margin.
 */
const SIZE_EPSILON = 0.0005;

function acceptable(scale, format) {
  return new Set([
    format(handsFor(scale - SIZE_EPSILON)),
    format(handsFor(scale)),
    format(handsFor(scale + SIZE_EPSILON)),
  ]);
}

// ---------------------------------------------------------------------------
// Names are the join key, because that is what the pages actually print - an
// id appears only in the odd anchor. They have to survive the wiki's entities
// and its diacritics: the book writes D&oslash;lahest and Selle Fran&ccedil;ais
// where breeds.json has Dolahest and Selle Français.
// ---------------------------------------------------------------------------

const NAMED = {
  amp: "&", lt: "<", gt: ">", quot: '"', apos: "'", nbsp: " ",
  ndash: "–", mdash: "—", middot: "·", hellip: "…",
  lsquo: "‘", rsquo: "’", ldquo: "“", rdquo: "”",
  times: "×", deg: "°",
  aacute: "á", agrave: "à", acirc: "â", auml: "ä", aring: "å", atilde: "ã",
  ccedil: "ç", eacute: "é", egrave: "è", ecirc: "ê", euml: "ë",
  iacute: "í", icirc: "î", iuml: "ï", ntilde: "ñ",
  oacute: "ó", ocirc: "ô", ouml: "ö", oslash: "ø", otilde: "õ",
  uacute: "ú", ucirc: "û", uuml: "ü", yacute: "ý", szlig: "ß",
  aelig: "æ", eth: "ð", thorn: "þ",
};

function decodeEntities(s) {
  return s
    .replace(/&#x([0-9a-fA-F]+);/g, (_, h) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(Number(d)))
    .replace(/&([a-zA-Z]+);/g, (whole, name) => {
      const lower = NAMED[name] ?? NAMED[name.toLowerCase()];
      if (lower === undefined) return whole; // leave it: an unknown entity should
      return name[0] === name[0].toUpperCase() && NAMED[name] === undefined
        ? lower.toUpperCase()                //  fail the name match loudly rather
        : lower;                             //  than be silently swallowed
    });
}

const FOLD = { ø: "o", æ: "ae", œ: "oe", ß: "ss", ð: "d", þ: "th", ł: "l", đ: "d" };

/** Fold a printed or JSON breed name to a comparison key. */
function key(name) {
  return decodeEntities(name)
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[øæœßðþłđ]/g, (c) => FOLD[c])
    .replace(/[^a-z0-9]+/g, "");
}

// ---------------------------------------------------------------------------
// The breed data.
// ---------------------------------------------------------------------------

const breeds = JSON.parse(
  readFileSync(join(wiki, "horse-designer/assets/breeds.json"), "utf8")
);

const byKey = new Map();
const collisions = [];
for (const b of breeds) {
  const k = key(b.name);
  if (byKey.has(k)) collisions.push(`${byKey.get(k).id} and ${b.id} both fold to "${k}"`);
  byKey.set(k, b);
}

/**
 * A strain may override any stat block, so a strain that overrides `size`
 * would widen the breed's real height range beyond the top-level band and this
 * checker's arithmetic would be a guess. None does today; if one ever does,
 * say so and go red rather than confirm a range we did not actually compute.
 */
const noSize = [];
const unchecked = [];
const sizeOf = new Map();
for (const b of breeds) {
  const overriding = (b.strains || []).filter((s) => s.stats && "size" in s.stats);
  if (overriding.length) {
    unchecked.push(
      `${b.name}: strain(s) ${overriding.map((s) => `"${s.name}"`).join(", ")} override size`
    );
    continue;
  }
  const size = b.stats && b.stats.size;
  if (!Array.isArray(size) || size.length !== 2) {
    noSize.push(b.name);
    continue;
  }
  sizeOf.set(b.name, [Math.min(size[0], size[1]), Math.max(size[0], size[1])]);
}

// ---------------------------------------------------------------------------
// What the two pages print.
// ---------------------------------------------------------------------------

/**
 * Split a printed range. Returns null for a cell that carries no height.
 *
 * A trailing "+" on the high end is allowed and ignored: the Shire sits on the
 * draught ceiling and its row says so with "16.2&ndash;17.3+". That is a claim
 * about the band, not about the digits, and the digits are still checked.
 */
function parseRange(text) {
  const t = decodeEntities(text).replace(/<[^>]+>/g, "").trim();
  // an em dash alone is a legitimately absent stat, not a height
  if (/^[–—-]$/.test(t) || t === "") return null;
  const m = t.match(/^(\d+(?:\.\d+)?)\s*[–—-]\s*(\d+(?:\.\d+)?)\+?$/);
  return m ? [m[1], m[2]] : { unparsed: t };
}

function breedsTableRows() {
  const html = readFileSync(join(wiki, "breeds.html"), "utf8");
  const at = html.indexOf('id="commonness"');
  if (at < 0) {
    console.error('check-breed-heights: no id="commonness" heading in wiki/breeds.html');
    process.exit(1);
  }
  const start = html.indexOf("<tbody>", at);
  const end = html.indexOf("</tbody>", start);
  if (start < 0 || end < 0) {
    console.error("check-breed-heights: the #commonness table has no <tbody>");
    process.exit(1);
  }
  const out = [];
  for (const row of html.slice(start, end).match(/<tr>[\s\S]*?<\/tr>/g) || []) {
    const cells = [...row.matchAll(/<td>([\s\S]*?)<\/td>/g)].map((m) => m[1]);
    if (cells.length < 3) continue;
    // the name cell is sometimes an <a href="breed-book.html#id">
    const name = decodeEntities(cells[0].replace(/<[^>]+>/g, "")).trim();
    out.push({ name, printed: parseRange(cells[2]) });
  }
  return out;
}

function breedBookEntries() {
  const html = readFileSync(join(wiki, "breed-book.html"), "utf8");
  const out = [];
  for (const dt of html.match(/<dt\b[^>]*>[\s\S]*?<\/dt>/g) || []) {
    const m = dt.match(/<dt\b[^>]*>([\s\S]*?)<span class="tag">([\s\S]*?)<\/span>/);
    if (!m) continue;
    const name = decodeEntities(m[1].replace(/<[^>]+>/g, "")).trim();
    // the tag is "<range> hh &middot; rarity &middot; ..."; only the head is a height
    const head = m[2].split(/&middot;|·/)[0].trim();
    const hh = head.match(/^([\s\S]*?)\s*hh$/);
    out.push({ name, printed: hh ? parseRange(hh[1]) : null });
  }
  return out;
}

// ---------------------------------------------------------------------------
// Compare.
// ---------------------------------------------------------------------------

const problems = [];
let checked = 0;

function audit(page, entries, format) {
  const seen = new Set();
  for (const { name, printed } of entries) {
    const breed = byKey.get(key(name));
    if (!breed) {
      problems.push(`${page} - ${name}: prints a height but is not a breed in breeds.json`);
      continue;
    }
    seen.add(breed.name);
    if (unchecked.some((u) => u.startsWith(breed.name + ":"))) continue;

    const size = sizeOf.get(breed.name);
    if (!size) {
      if (printed) {
        problems.push(
          `${page} - ${breed.name}: prints ${printed.unparsed ?? printed.join("-")}, ` +
            "but the breed has no size band to derive it from"
        );
      }
      continue;
    }
    if (!printed) {
      problems.push(`${page} - ${breed.name}: prints no height, but the breed has a size band`);
      continue;
    }
    if (printed.unparsed !== undefined) {
      problems.push(`${page} - ${breed.name}: cannot read the printed range "${printed.unparsed}"`);
      continue;
    }
    const ok = [acceptable(size[0], format), acceptable(size[1], format)];
    if (!ok[0].has(printed[0]) || !ok[1].has(printed[1])) {
      const want = [format(handsFor(size[0])), format(handsFor(size[1]))];
      problems.push(
        `${page} - ${breed.name}: printed ${printed.join("–")}, expected ${want.join("–")}`
      );
    }
    checked++;
  }

  // The other half of the same defect: a breed that shipped with no row at all
  // is silently absent rather than visibly wrong, and no amount of checking the
  // printed figures would ever notice it.
  for (const b of breeds) {
    if (!seen.has(b.name)) problems.push(`${page} - ${b.name}: no entry on this page at all`);
  }
}

audit("breeds.html", breedsTableRows(), stableHands);
audit("breed-book.html", breedBookEntries(), decimalHands);

for (const c of collisions) {
  problems.push(`breeds.json - ${c}; this checker joins on the printed name, so it cannot tell them apart`);
}

if (unchecked.length) {
  console.error(
    `${unchecked.length} breed(s) cannot be checked, because a strain overrides the size\n` +
      "band this tool computes the range from. Teach it the strain rule rather than\n" +
      "letting it report a range it did not actually derive:"
  );
  for (const u of unchecked) console.error("  " + u);
  console.error("");
}

if (problems.length) {
  console.error(`${problems.length} breed height problem(s):`);
  for (const p of problems) console.error("  " + p);
  console.error(
    "\nbreeds.html prints STABLE notation (BreedStatCurve.formatHands - the digit after\n" +
      "the point runs 0-3); breed-book.html prints DECIMAL hands to one place. Both are\n" +
      "correct for their page - fix the digit, not the convention."
  );
}

if (problems.length || unchecked.length) process.exit(1);

console.log(
  `breed heights OK - ${checked} breeds checked on two pages, ${noSize.length} with no size band`
);
