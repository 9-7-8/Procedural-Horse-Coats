/*
 * Builds wiki/search-index.js - the corpus the wiki's search box reads.
 *
 * WHY THIS IS A BUILD STEP. The wiki is static files, opened as often from a
 * file:// path as from a server, so there is nothing to run a query against at
 * read time and no fetch that is guaranteed to work. So the text of every page
 * is baked into one script the search box loads on first use.
 *
 * WHAT IT KNOWS ABOUT TABS. A page split into gameplay / coding / science
 * panels is indexed per panel, so a hit can say which tab it is on and link
 * straight to it. Text above the panels (the title and the lede) belongs to
 * every tab and is filed under "all". A page with no panels is one record.
 *
 * It also emits, per page, the list of tabs that page actually has - which is
 * what the index's view switcher hides pages by, so nothing has to declare it
 * twice.
 *
 * IT IS DERIVED AND IT FAILS SILENTLY. A stale index means search quietly
 * misses new pages, so re-run it whenever wiki text changes:
 *
 *     node wiki/tools/build-search-index.mjs
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from "node:fs";
import { join, dirname, basename } from "node:path";
import { fileURLToPath } from "node:url";

const WIKI = dirname(dirname(fileURLToPath(import.meta.url)));
const ROOT = dirname(WIKI);

/** Pages that are tools with their own chrome, or generated - not prose. */
const SKIP = new Set(["nav.js"]);

function htmlFiles() {
    const out = [{ path: join(ROOT, "index.html"), href: "index.html" }];
    for (const name of readdirSync(WIKI).sort()) {
        if (!name.endsWith(".html") || SKIP.has(name)) continue;
        if (!statSync(join(WIKI, name)).isFile()) continue;
        out.push({ path: join(WIKI, name), href: "wiki/" + name });
    }
    return out;
}

/** Everything that is not prose: scripts, styles, comments, inline svg. */
function strip(html) {
    return html
        .replace(/<script\b[\s\S]*?<\/script>/gi, " ")
        .replace(/<style\b[\s\S]*?<\/style>/gi, " ")
        .replace(/<svg\b[\s\S]*?<\/svg>/gi, " ")
        .replace(/<!--[\s\S]*?-->/g, " ");
}

const ENTITIES = {
    amp: "&", lt: "<", gt: ">", quot: '"', apos: "'", nbsp: " ",
    mdash: "—", ndash: "–", hellip: "…", middot: "·",
    rsquo: "’", lsquo: "‘", ldquo: "“", rdquo: "”",
    times: "×", minus: "−", deg: "°", sect: "§",
    times2: "×", frac12: "½", hearts: "♥", check: "✓"
};

function text(html) {
    return strip(html)
        .replace(/<[^>]+>/g, " ")
        .replace(/&#(\d+);/g, (_, d) => String.fromCharCode(+d))
        .replace(/&([a-zA-Z][a-zA-Z0-9]*);/g, (m, name) =>
            Object.prototype.hasOwnProperty.call(ENTITIES, name) ? ENTITIES[name] : " ")
        .replace(/\s+/g, " ")
        .trim();
}

/**
 * Split a page body into { tab -> html }. "all" is everything outside a panel.
 * Counts <section> depth so a panel that contains one still closes correctly.
 */
function panels(html) {
    const open = /<section\b[^>]*class="[^"]*\btab-panel\b[^"]*"[^>]*>/gi;
    const out = { all: "" };
    let cursor = 0, m;
    while ((m = open.exec(html)) !== null) {
        out.all += html.slice(cursor, m.index);
        const tab = (/data-tab="([^"]+)"/i.exec(m[0]) || [, "gameplay"])[1];
        // Walk forward balancing <section> ... </section>.
        const tag = /<section\b|<\/section\s*>/gi;
        tag.lastIndex = open.lastIndex;
        let depth = 1, end = html.length, t;
        while ((t = tag.exec(html)) !== null) {
            if (t[0][1] === "/") { depth--; } else { depth++; }
            if (depth === 0) { end = t.index; break; }
        }
        out[tab] = (out[tab] || "") + html.slice(open.lastIndex, end);
        cursor = tag.lastIndex || end;
        open.lastIndex = cursor;
    }
    out.all += html.slice(cursor);
    return out;
}

/** Headings with an id, so a hit can link to the section rather than the page. */
function headings(html) {
    const out = [];
    const re = /<h([2-4])\b([^>]*)>([\s\S]*?)<\/h\1>/gi;
    let m;
    while ((m = re.exec(html)) !== null) {
        const id = (/id="([^"]+)"/i.exec(m[2]) || [, ""])[1];
        out.push({ id, level: +m[1], title: text(m[3]), at: m.index });
    }
    return out;
}

/**
 * One page's records: a chunk per heading, so a result can point at a section
 * and quote from it, rather than pointing at a 1 000-line page.
 */
function chunks(tab, html) {
    const hs = headings(html);
    const out = [];
    const lead = text(hs.length ? html.slice(0, hs[0].at) : html);
    if (lead) out.push({ tab, id: "", h: "", t: lead });
    hs.forEach((h, i) => {
        const end = i + 1 < hs.length ? hs[i + 1].at : html.length;
        const body = text(html.slice(h.at, end));
        if (body) out.push({ tab, id: h.id, h: h.title, t: body });
    });
    return out;
}

const pages = [];
for (const file of htmlFiles()) {
    const raw = readFileSync(file.path, "utf8");
    const title = text((/<h1\b[^>]*>([\s\S]*?)<\/h1>/i.exec(raw) || [, ""])[1])
        || text((/<title\b[^>]*>([\s\S]*?)<\/title>/i.exec(raw) || [, ""])[1])
             .replace(/\s*·.*$/, "");
    const eyebrow = text((/<p class="eyebrow[^"]*"[^>]*>([\s\S]*?)<\/p>/i.exec(raw) || [, ""])[1]);
    const kindMatch = /<p class="eyebrow ([a-z]+)"/i.exec(raw);

    const body = (/<article class="doc"[^>]*>([\s\S]*)<\/article>/i.exec(raw)
        || /<main\b[^>]*>([\s\S]*)<\/main>/i.exec(raw)
        || [, raw])[1];

    const split = panels(body);
    const tabs = ["gameplay", "coding", "science"].filter((t) => split[t] && text(split[t]));
    const recs = [];
    for (const [tab, html] of Object.entries(split)) {
        if (!html.trim()) continue;
        recs.push(...chunks(tab, html));
    }
    if (!recs.length) continue;
    pages.push({
        href: file.href,
        title,
        eyebrow,
        kind: kindMatch ? kindMatch[1] : "core",
        tabs,
        chunks: recs
    });
}

const banner = "/* GENERATED by wiki/tools/build-search-index.mjs - do not edit.\n"
    + " * Re-run it whenever wiki text changes; a stale index silently misses pages.\n"
    + " */\n";
const out = banner + "window.HG = window.HG || {};\nwindow.HG.searchIndex = "
    + JSON.stringify({ pages }) + ";\n";
writeFileSync(join(WIKI, "search-index.js"), out);

const words = pages.reduce((n, p) => n + p.chunks.reduce((m, c) => m + c.t.split(" ").length, 0), 0);
console.log(`indexed ${pages.length} pages, ${words.toLocaleString()} words, `
    + `${(out.length / 1024).toFixed(0)} KB -> wiki/search-index.js`);
