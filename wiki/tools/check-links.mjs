#!/usr/bin/env node
// Every internal link in the wiki must resolve to a file that exists - and a
// page's footer must not point somewhere its own subject is not.
//
// This exists because a wiki page is hand-written HTML and nothing has ever
// checked its hrefs. A gene page linking to gene-foo.html when the gene is
// called foo-bar looks exactly like a working page until somebody clicks it,
// and the two bulk gene imports added several thousand cross-links between
// pages in one sitting each.
//
// It also catches the reverse of known-gaps gap 115: a page that exists but
// nothing links to and no nav lists - an orphan left behind by a rename.
//
//   node wiki/tools/check-links.mjs            report and exit non-zero on a break
//   node wiki/tools/check-links.mjs --orphans  also list unreachable pages
//
// Fragments (#anchor) are checked too: a link to page.html#section fails if
// nothing on that page carries that id. That is the half most likely to rot,
// because an id is invisible in the rendered page.
//
// The last two rules are about links that RESOLVE AND STILL LIE, which is the
// failure no amount of href checking finds. A page's footer - its See-also row
// and the blurb under it - sits OUTSIDE the tab panels, so every edit made at
// tab level leaves it behind, and it is the one part of a page an author never
// has open. horse-stasis.html shipped its last stage and still said "everything
// on this page is built except the self-triggering emergency chamber", with a
// See-also pointing at roadmap.html - a generated index the page had just left,
// so the link worked and landed the reader on a list its subject was absent
// from. See wiki/coding-notes.html#page-foot.

import { readFileSync, readdirSync, existsSync, statSync } from "node:fs";
import { join, dirname, resolve, relative } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..", "..");
const WIKI = join(ROOT, "wiki");

/** Every .html under wiki/ plus the landing page, as repo-relative paths. */
function htmlFiles(dir, out = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) {
      // The three editors are apps with their own chrome; they are still
      // scanned, because a broken link out of one is just as broken.
      if (entry.name === "node_modules") continue;
      htmlFiles(full, out);
    } else if (entry.name.endsWith(".html")) {
      out.push(full);
    }
  }
  return out;
}

const pages = [...htmlFiles(WIKI), join(ROOT, "index.html")].filter(existsSync);

/** id="..." on any element, plus name="..." on an anchor. */
function idsOf(html) {
  const ids = new Set();
  for (const m of html.matchAll(/\sid="([^"]+)"/g)) ids.add(m[1]);
  for (const m of html.matchAll(/<a[^>]+\sname="([^"]+)"/g)) ids.add(m[1]);
  return ids;
}

const idCache = new Map();
function idsFor(file) {
  if (!idCache.has(file)) {
    idCache.set(file, existsSync(file) ? idsOf(readFileSync(file, "utf8")) : new Set());
  }
  return idCache.get(file);
}

const broken = [];
const linkedTo = new Set();

/**
 * Duplicate ids on one page. An `#anchor` resolves to the FIRST match, so a
 * repeated id means one of the two sections is unreachable by link and the
 * other silently answers for it - and every link to it looks fine, here
 * included, because the id does exist.
 *
 * It had already happened: known-gaps.html carried two entries numbered
 * gap-46, so one of them could not be linked at all.
 */
function duplicateIds(html) {
  const seen = new Set();
  const dupes = new Set();
  for (const m of html.matchAll(/\sid="([^"]+)"/g)) {
    if (seen.has(m[1])) dupes.add(m[1]);
    seen.add(m[1]);
  }
  return [...dupes];
}

// ---------------------------------------------------------------------------
// The footer: a See-also, or a blurb, that resolves and still lies
// ---------------------------------------------------------------------------

/**
 * The two generated indexes, and the tab a page must carry to belong in each.
 * Both are built by their bake script from the tabs themselves
 * (bake-roadmap-index.mjs, bake-verification-index.mjs), so a page without the
 * tab is simply not on the list - and a See-also promising it is a link that
 * works and arrives nowhere.
 */
const INDEXES = { "roadmap.html": "roadmap", "verification.html": "verification" };

/**
 * The words a footer uses to say there is work outstanding. Deliberately a
 * closed, small vocabulary rather than anything clever: across 167 footers it
 * matches three, and the point is to catch the page that says "except the X"
 * after X shipped, not to grade prose.
 */
const OUTSTANDING =
  /\bstill\s+(?:planned|unbuilt|open|to\s+\w+)\b|\bnot\s+(?:yet\s+)?built\b|\bunbuilt\b|\b(?:is|are)\s+planned\b|\bexcept\s+the\b|\bleft\s+to\s+(?:build|do|write)\b|\byet\s+to\s+be\b/i;

/**
 * <b>Is this page about the process rather than about a subject?</b> Such a
 * page may link either index freely and may talk about unbuilt work in general
 * - that is what it is for. Read off the page's own eyebrow rather than a list
 * of filenames here, so a new Project page is exempt by being one and nothing
 * has to be kept in step.
 */
function isProcessPage(html) {
  const m = html.match(/class="eyebrow[^"]*">\s*([^<]*)/);
  const kind = (m ? m[1] : "").trim().toLowerCase();
  return kind.startsWith("project") || kind.startsWith("start here");
}

/** The page-foot block, from its opening div to the end of the article. */
function footerOf(html) {
  const at = html.indexOf('<div class="page-foot">');
  if (at < 0) return null;
  const rest = html.slice(at);
  const end = rest.indexOf("</article>");
  return end < 0 ? rest : rest.slice(0, end);
}

function textOf(fragment) {
  return fragment
    .replace(/<[^>]+>/g, " ")
    .replace(/&[a-z]+;/gi, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function footerProblems(html, from) {
  const footer = footerOf(html);
  if (!footer || isProcessPage(html)) return [];

  const problems = [];
  const seealso = footer.match(/<div class="seealso">([\s\S]*?)<\/div>/);

  for (const m of (seealso ? seealso[1] : "").matchAll(/href="([^"#?]+)/g)) {
    const file = m[1].split("/").pop();
    const tab = INDEXES[file];
    if (tab && !html.includes(`data-tab="${tab}"`)) {
      problems.push(
        `${from} -> ${file} in its See-also (file exists, but this page has no ` +
          `${tab} tab, so ${file} does not list it - link the tab that holds it, or drop the row)`
      );
    }
  }

  const blurb = textOf(footer.replace(/<div class="seealso">[\s\S]*?<\/div>/, ""));
  const claim = blurb.match(OUTSTANDING);
  if (claim && !html.includes('data-tab="roadmap"')) {
    problems.push(
      `${from} footer blurb says "${claim[0]}" but this page has no roadmap tab - ` +
        `either the work shipped and the blurb was not updated, or it needs a Roadmap tab`
    );
  }
  return problems;
}

for (const page of pages) {
  const html = readFileSync(page, "utf8");
  const from = relative(ROOT, page).replaceAll("\\", "/");

  for (const id of duplicateIds(html)) {
    broken.push(`${from} has TWO elements with id="${id}" - an #anchor only ever reaches the first`);
  }

  for (const problem of footerProblems(html, from)) broken.push(problem);

  for (const m of html.matchAll(/\s(?:href|src)="([^"]+)"/g)) {
    const raw = m[1];
    // External, protocol-relative, mail, in-page JS, and data URIs are not ours.
    if (/^(https?:|mailto:|data:|javascript:|\/\/)/i.test(raw)) continue;

    // A query string is the view switcher (?view=science, ?view=lab) - it is a
    // parameter the page reads, not part of its filename.
    const [beforeHash, fragment] = raw.split("#");
    const pathPart = beforeHash.split("?")[0];

    // A bare "#id" is an in-page anchor.
    if (pathPart === "") {
      if (fragment && !idsFor(page).has(fragment)) {
        broken.push(`${from} -> #${fragment} (no such id on this page)`);
      }
      continue;
    }

    const target = resolve(dirname(page), decodeURIComponent(pathPart));
    if (!existsSync(target)) {
      broken.push(`${from} -> ${raw} (no such file)`);
      continue;
    }
    if (statSync(target).isDirectory()) continue;
    linkedTo.add(relative(ROOT, target).replaceAll("\\", "/"));

    if (fragment && target.endsWith(".html") && !idsFor(target).has(fragment)) {
      broken.push(`${from} -> ${raw} (file exists, no id "${fragment}")`);
    }
  }
}

if (broken.length) {
  console.error(`${broken.length} broken or misleading link(s):\n`);
  for (const b of broken.sort()) console.error("  " + b);
} else {
  console.log(
    `links OK - every internal href and src across ${pages.length} pages resolves, ` +
      `and every footer points where its own tabs say it should`
  );
}

if (process.argv.includes("--orphans")) {
  // A page nothing links to AND that pages.js does not list. pages.js is the
  // sidebar's source of truth, so being in it counts as reachable even with no
  // inbound prose link.
  const listed = new Set();
  const pagesJs = readFileSync(join(WIKI, "pages.js"), "utf8");
  for (const m of pagesJs.matchAll(/href:\s*"([^"]+)"/g)) listed.add("wiki/" + m[1]);

  const orphans = pages
    .map((p) => relative(ROOT, p).replaceAll("\\", "/"))
    .filter((p) => p !== "index.html")
    .filter((p) => !linkedTo.has(p) && !listed.has(p));

  console.log(`\n${orphans.length} page(s) neither linked to nor listed in pages.js:`);
  for (const o of orphans.sort()) console.log("  " + o);
}

process.exit(broken.length ? 1 : 0);
