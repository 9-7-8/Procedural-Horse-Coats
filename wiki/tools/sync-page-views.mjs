/*
 * Rewrites the `views:` field in wiki/pages.js to match what each page actually
 * contains.
 *
 * WHY IT EXISTS. `views` decides which of the three index views list a page. For
 * a page split into Gameplay / Coding / Science tabs that is not a judgement
 * call - it is exactly the tabs the page has - and a hand-maintained copy of it
 * would go stale the first time a section moved between tabs. So this reads the
 * panels out of the HTML and writes the answer back.
 *
 * A page with no tab panels is left exactly as it is: for a page that is one
 * continuous document, `views` is a real editorial decision about who it is
 * written for, and nothing here can infer that.
 *
 *     node wiki/tools/sync-page-views.mjs
 *
 * Run it after splitting a page into tabs, or after moving a section from one
 * tab to another. It prints what it changed.
 */
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const WIKI = dirname(dirname(fileURLToPath(import.meta.url)));
const ORDER = ["gameplay", "coding", "science"];

/** An href to the file it names - a registered link may carry a ?view=. */
function fileOf(href) {
    const path = href.split("?")[0].split("#")[0];
    return path === "../index.html"
        ? join(dirname(WIKI), "index.html")
        : join(WIKI, path);
}

/** The tabs a page has, or null if it is not a tabbed page. */
function tabsOf(href) {
    const file = fileOf(href);
    if (!existsSync(file)) { return null; }
    const html = readFileSync(file, "utf8");
    const found = new Set();
    for (const m of html.matchAll(/<section\b[^>]*class="[^"]*\btab-panel\b[^"]*"[^>]*>/gi)) {
        const tab = /data-tab="([^"]+)"/i.exec(m[0]);
        if (tab) { found.add(tab[1]); }
    }
    if (!found.size) { return null; }
    return ORDER.filter((t) => found.has(t));
}

const path = join(WIKI, "pages.js");
let src = readFileSync(path, "utf8");
const changed = [];
const missing = [];

src = src.replace(
    /\{ href: "([^"]+)", text: ("(?:[^"\\]|\\.)*"), kind: ("[^"]*"), views: (\[[^\]]*\]) \}/g,
    (whole, href, text, kind, views) => {
        const tabs = tabsOf(href);
        if (tabs === null) {
            if (!existsSync(fileOf(href))) {
                missing.push(href);
            }
            return whole;
        }
        const next = JSON.stringify(tabs);
        if (next !== views.replace(/\s/g, "")) {
            changed.push(`${href}: ${views.replace(/\s/g, "")} -> ${next}`);
        }
        return `{ href: "${href}", text: ${text}, kind: ${kind}, views: ${next} }`;
    }
);

writeFileSync(path, src);
console.log(changed.length ? `updated ${changed.length} entries:` : "nothing to update");
changed.forEach((c) => console.log("  " + c));
if (missing.length) {
    console.log(`\n${missing.length} registered page(s) have no file:`);
    missing.forEach((m) => console.log("  " + m));
}
