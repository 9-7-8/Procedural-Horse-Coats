/*
 * Shared HTML-to-text helpers used by both wiki bakes that read page prose:
 * build-search-index.mjs (the in-browser chat corpus) and bake-agent-text.mjs
 * (the plain-text mirrors + anchor index for coding agents). One parser, two
 * outputs - keep it that way rather than letting the two scripts drift.
 */
import { readdirSync, statSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

export const WIKI = dirname(dirname(fileURLToPath(import.meta.url)));
export const ROOT = dirname(WIKI);

/** Every top-level wiki page, plus the root index.html. Apps (gene-creator/
 *  etc.) own their own chrome and are not prose pages, so they are not walked -
 *  readdirSync here is non-recursive on purpose. */
export function htmlFiles(skip = new Set()) {
    const out = [{ path: join(ROOT, "index.html"), href: "index.html" }];
    for (const name of readdirSync(WIKI).sort()) {
        if (!name.endsWith(".html") || skip.has(name)) continue;
        if (!statSync(join(WIKI, name)).isFile()) continue;
        out.push({ path: join(WIKI, name), href: "wiki/" + name });
    }
    return out;
}

/** Everything that is not prose: scripts, styles, comments, inline svg. */
export function strip(html) {
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

export function text(html) {
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
export function panels(html) {
    const open = /<section\b[^>]*class="[^"]*\btab-panel\b[^"]*"[^>]*>/gi;
    const out = { all: "" };
    let cursor = 0, m;
    while ((m = open.exec(html)) !== null) {
        out.all += html.slice(cursor, m.index);
        const tab = (/data-tab="([^"]+)"/i.exec(m[0]) || [, "gameplay"])[1];
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
export function headings(html) {
    const out = [];
    const re = /<h([2-4])\b([^>]*)>([\s\S]*?)<\/h\1>/gi;
    let m;
    while ((m = re.exec(html)) !== null) {
        const id = (/id="([^"]+)"/i.exec(m[2]) || [, ""])[1];
        out.push({ id, level: +m[1], title: text(m[3]), at: m.index });
    }
    return out;
}

/** A page's title/eyebrow/lede, read the same way on every page. */
export function frontMatter(raw) {
    const title = text((/<h1\b[^>]*>([\s\S]*?)<\/h1>/i.exec(raw) || [, ""])[1])
        || text((/<title\b[^>]*>([\s\S]*?)<\/title>/i.exec(raw) || [, ""])[1])
             .replace(/\s*·.*$/, "");
    const eyebrow = text((/<p class="eyebrow[^"]*"[^>]*>([\s\S]*?)<\/p>/i.exec(raw) || [, ""])[1]);
    const lede = text((/<p class="lede"[^>]*>([\s\S]*?)<\/p>/i.exec(raw) || [, ""])[1]);
    const kindMatch = /<p class="eyebrow ([a-z]+)"/i.exec(raw);
    return { title, eyebrow, lede, kind: kindMatch ? kindMatch[1] : "core" };
}

/** The <article class="doc"> or <main> body - where the prose lives. */
export function articleBody(raw) {
    return (/<article class="doc"[^>]*>([\s\S]*)<\/article>/i.exec(raw)
        || /<main\b[^>]*>([\s\S]*)<\/main>/i.exec(raw)
        || [, raw])[1];
}
