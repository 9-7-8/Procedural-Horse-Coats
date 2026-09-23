#!/usr/bin/env node
import { readFileSync, writeFileSync } from "node:fs";

const gapPage = "wiki/known-gaps.html";
const mapping = {
    17: "items.html",
    143: "item-research-shelf.html",
    243: "gene-lycan.html",
    245: "breeding.html",
    250: "horse-body.html",
    254: "architecture.html",
    255: "architecture.html",
    256: "breeds.html",
    259: "horse-care.html",
    261: "making-a-gene.html",
    264: "horse-body.html",
    265: "horse-body.html",
    266: "breeds.html",
    267: "coding-notes.html",
    268: "gene-aggression.html",
    271: "compatibility.html",
    272: "compatibility.html",
    273: "item-double-gates.html",
    274: "compatibility.html",
    275: "compatibility.html",
    276: "ai-setup.html",
    278: "breed-book.html",
    279: "compatibility.html",
    280: "compatibility.html",
    281: "compatibility.html",
    282: "compatibility.html",
    283: "item-stall-signs.html",
    284: "gene-suntouched.html",
    285: "carts.html",
    286: "carts.html",
    287: "item-jumps.html",
    288: "item-jumps.html",
    292: "gene-leopard.html",
    293: "horse-body.html",
    294: "horse-gear.html",
    295: "gene-brindle.html",
    296: "coding-notes.html",
    297: "genetics-model.html",
    298: "genetics-model.html",
    299: "gene-ednrb.html",
    300: "horse-care.html",
    301: "coding-notes.html",
    302: "item-research-papers.html",
    303: "gene-singer.html",
    304: "gene-weather-speed.html"
};

function gapRecords(html) {
    const records = new Map();
    const pattern = /<li id="gap-(\d+)">([\s\S]*?)(?=<li id="gap-|<\/ol>|<\/ul>)/g;
    for (const match of html.matchAll(pattern)) {
        records.set(Number(match[1]), match[0]);
    }
    return records;
}

function stripOuterList(record) {
    return record
        .replace(/^<li id="gap-\d+">/, "")
        .replace(/<\/li>\s*$/, "");
}

function insertIntoVerification(html, id, record) {
    if (html.includes(`id="gap-${id}"`)) {
        throw new Error(`${id}: destination already contains this check`);
    }
    const open = html.indexOf('data-tab="verification"');
    if (open >= 0) {
        const start = html.lastIndexOf("<section", open);
        const end = html.indexOf("</section>", open);
        if (start < 0 || end < 0) {
            throw new Error(`${id}: malformed verification section`);
        }
        const block = `\n<h3 id="gap-${id}" data-check-kind="open-gap">Open check ${id}</h3>\n<div class="open-check">\n${stripOuterList(record)}\n</div>\n`;
        return html.slice(0, end) + block + html.slice(end);
    }

    const section = `\n<section id="verification" class="tab-panel" data-tab="verification" data-tab-label="Verification" data-tab-hint="Open checks">\n\n<div class="note verify-summary">\n    <span class="label">Open checks here</span>\n    <p>This page carries open defects, decisions, documentation checks, or runtime checks that still need a result.</p>\n</div>\n\n<h3 id="gap-${id}" data-check-kind="open-gap">Open check ${id}</h3>\n<div class="open-check">\n${stripOuterList(record)}\n</div>\n\n</section>\n`;
    const articleEnd = html.lastIndexOf("</article>");
    if (articleEnd < 0) {
        throw new Error(`${id}: destination has no article`);
    }
    return html.slice(0, articleEnd) + section + html.slice(articleEnd);
}

let gapHtml = readFileSync(gapPage, "utf8");
const records = gapRecords(gapHtml);
const migrated = [];

for (const [idText, page] of Object.entries(mapping)) {
    const id = Number(idText);
    const record = records.get(id);
    if (!record) {
        throw new Error(`gap-${id}: record not found`);
    }
    const path = `wiki/${page}`;
    let html = readFileSync(path, "utf8");
    html = insertIntoVerification(html, id, record);
    writeFileSync(path, html);
    gapHtml = gapHtml.replace(record, "");
    migrated.push({ id, page });
}

const links = migrated.map(({ id, page }) =>
    `    <li id="gap-${id}">Moved to <a href="${page}#gap-${id}">${page}#gap-${id}</a>.</li>`
).join("\n");
const compatibility = `\n<h2 id="migrated">Migrated open checks</h2>\n<p>These anchors remain only so old links resolve. The check itself lives on the owning page&rsquo;s Verification tab; this page is no longer a source of gap records.</p>\n<ul>\n${links}\n</ul>\n`;
gapHtml = gapHtml.replace("</article>", compatibility + "</article>");
writeFileSync(gapPage, gapHtml);

console.log(`migrated ${migrated.length} open checks`);
for (const { id, page } of migrated) {
    console.log(`gap-${id} -> wiki/${page}#gap-${id}`);
}
