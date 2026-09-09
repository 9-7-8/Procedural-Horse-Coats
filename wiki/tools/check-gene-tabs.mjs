#!/usr/bin/env node
// Every NEW natural gene page must carry a science tab.
//
// known-gaps gap 89: a natural gene page with no `data-tab="science"` panel is
// missing from the science view entirely - not shown with an empty section, but
// absent, because wiki/pages.js derives a page's `views` from the tab panels it
// actually has. The group that loses most is the health loci, which is exactly
// the group a science reader comes for.
//
// The debt this check was written to freeze is now PAID: 25 natural gene pages
// had no science tab when it was added, and all 25 have been written. The
// allowlist below is deliberately EMPTY, which turns the check from "stop the
// list growing" into a flat rule - every natural gene page carries a science
// tab, and a new one without one goes red immediately.
//
// Leave it empty. If a page genuinely cannot have one, the honest move is to
// argue that here in a comment beside its name rather than to let a silent
// entry accumulate - that was the original failure mode this file exists to
// prevent, and an empty set is the only state where nothing can rot in it.
//
// That is the same shape as the parser's refusals - freeze the corpus, prevent
// the next one - and it is why the allowlist is written out in full rather than
// counted. A number would let a page swap in for another silently.
//
//   node wiki/tools/check-gene-tabs.mjs           report and exit non-zero on a gap
//   node wiki/tools/check-gene-tabs.mjs --list    print anything still outstanding

import { readFileSync, readdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const wiki = join(dirname(fileURLToPath(import.meta.url)), "..");

/** Natural gene pages exempted from the rule. Empty, and meant to stay empty. */
const GRANDFATHERED = new Set([]);

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
  outstanding === 0
    ? "gene tabs OK - every natural gene page has a science tab, and none is exempt"
    : `gene tabs OK - every natural gene page outside the exempt list has a ` +
      `science tab (${outstanding} still owed; --list to see them)`
);
