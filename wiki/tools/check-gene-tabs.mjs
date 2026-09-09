#!/usr/bin/env node
// Every NEW natural gene page must carry a science tab.
//
// known-gaps gap 89: a natural gene page with no `data-tab="science"` panel is
// missing from the science view entirely - not shown with an empty section, but
// absent, because wiki/pages.js derives a page's `views` from the tab panels it
// actually has. The group that loses most is the health loci, which is exactly
// the group a science reader comes for.
//
// The pages below are what is left of the debt as it stood when this check was
// written - 25 pages then, and the rest have since been written. They are NOT
// stubbed on purpose: a tab reading "not written yet" on a page is
// worse than an honest absence, and filling them means writing real veterinary
// and population-genetics material, not generating it. So this tool does not
// demand they be fixed today. What it does is stop the list GROWING: add a new
// natural gene page without a science tab and this goes red, naming it.
//
// That is the same shape as the parser's refusals - freeze the corpus, prevent
// the next one - and it is why the allowlist is written out in full rather than
// counted. A number would let a page swap in for another silently.
//
//   node wiki/tools/check-gene-tabs.mjs           report and exit non-zero on a new one
//   node wiki/tools/check-gene-tabs.mjs --list    print the outstanding pages and exit 0
//
// When you write one of these tabs, DELETE its line here. The check then holds
// that page to its science tab for good, and the list is a live backlog rather
// than a comment that rots.

import { readFileSync, readdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const wiki = join(dirname(fileURLToPath(import.meta.url)), "..");

/** Natural gene pages that predate this check and have no science tab yet. */
const GRANDFATHERED = new Set([
  // The science source files for these two arrived with the wrong content (both
  // were copies of the mushroom write-up), so they are the last two left of the
  // original 25. Everything else on that list now has a real science tab.
  "gene-rabicano.html",
  "gene-tiger-eye.html"
]);

const pages = readdirSync(wiki).filter((f) => /^gene-.*\.html$/.test(f)).sort();

const missing = [];
const fixed = [];

for (const file of pages) {
  const html = readFileSync(join(wiki, file), "utf8");

  // A page with no tab panels at all is a different shape and is left alone,
  // the way wiki/tabs.js leaves it alone.
  if (!/class="tab-panel"/.test(html)) continue;

  // The eyebrow carries the family: only the natural genes are held to this.
  // A magical gene has no real-world counterpart, so having no science tab is
  // correct rather than a gap - that distinction is the whole reason this
  // check reads the eyebrow instead of counting pages.
  const kind = (html.match(/<p class="eyebrow ([a-z]+)"/) || [])[1];
  if (kind !== "natural") continue;

  const hasScience = /data-tab="science"/.test(html);
  if (!hasScience && !GRANDFATHERED.has(file)) missing.push(file);
  if (hasScience && GRANDFATHERED.has(file)) fixed.push(file);
}

if (process.argv.includes("--list")) {
  const outstanding = [...GRANDFATHERED].filter((f) => !fixed.includes(f)).sort();
  console.log(`${outstanding.length} natural gene page(s) still owe a science tab:`);
  for (const f of outstanding) console.log("  " + f);
  process.exit(0);
}

let bad = false;

if (missing.length) {
  bad = true;
  console.error(
    `${missing.length} natural gene page(s) have no science tab and are not on the\n` +
    "grandfathered list, so they will not appear in the science view at all:"
  );
  for (const f of missing) console.error("  " + f);
  console.error(
    "\nAdd a <section class=\"tab-panel\" data-tab=\"science\"> to each, then re-run\n" +
    "node wiki/tools/sync-page-views.mjs so pages.js lists the page in that view."
  );
}

if (fixed.length) {
  bad = true;
  console.error(
    `\n${fixed.length} page(s) now HAVE a science tab but are still listed as\n` +
    "grandfathered in this file. Delete them from GRANDFATHERED so the check\n" +
    "starts holding them to it:"
  );
  for (const f of fixed) console.error("  " + f);
}

if (bad) process.exit(1);

const outstanding = GRANDFATHERED.size - fixed.length;
console.log(
  `gene tabs OK - every natural gene page outside the grandfathered list has a ` +
  `science tab (${outstanding} still owed; --list to see them)`
);
