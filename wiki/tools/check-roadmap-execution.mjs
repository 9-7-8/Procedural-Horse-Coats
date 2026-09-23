import fs from "node:fs";

const roadmap = fs.readFileSync("wiki/roadmap.html", "utf8");
const execution = fs.readFileSync("wiki/roadmap-execution.html", "utf8");
const gaps = fs.readFileSync("wiki/known-gaps.html", "utf8");

function uniqueMatches(text, pattern, group = 1) {
    return new Set([...text.matchAll(pattern)].map(match => match[group]));
}

function gapText(html) {
    return html
        .replace(/<[^>]*>/g, " ")
        .replace(/&[^;]+;/g, " ")
        .replace(/\s+/g, " ")
        .trim();
}

const roadmapPages = uniqueMatches(roadmap, /<a href="([^"]+)#roadmap">/g);
const coverage = execution.match(/<h2 id="coverage">[\s\S]*?<h2 id="gates">/)?.[0];
if (!coverage) {
    throw new Error("roadmap execution coverage section is missing");
}

const coveredPages = uniqueMatches(coverage, /<a href="([^"]+)#roadmap">/g);
const coverageRows = [...coverage.matchAll(/<tr><td>/g)].length;
const completeRows = [...coverage.matchAll(/<tr>(?:<td>[\s\S]*?){4}<\/tr>/g)].length;
const missingPages = [...roadmapPages].filter(page => !coveredPages.has(page));
const extraPages = [...coveredPages].filter(page => !roadmapPages.has(page));

if (coverageRows !== roadmapPages.size || completeRows !== roadmapPages.size) {
    throw new Error(`coverage rows are incomplete: pages=${roadmapPages.size}, rows=${coverageRows}, complete=${completeRows}`);
}
if (missingPages.length || extraPages.length) {
    throw new Error(`roadmap coverage mismatch: missing=${missingPages.join(", ") || "none"}; extra=${extraPages.join(", ") || "none"}`);
}

const openGaps = new Set();
const gapPattern = /<li id="(gap-\d+)">([\s\S]*?)(?=<li id="gap-|<\/ol>|<\/ul>)/g;
for (const match of gaps.matchAll(gapPattern)) {
    if (/\bOpen\b/.test(gapText(match[2]))) {
        openGaps.add(match[1]);
    }
}

const gapRouting = execution.match(/<h2 id="gap-routing">[\s\S]*?<h2 id="session-packet">/)?.[0];
if (!gapRouting) {
    throw new Error("gap routing section is missing");
}
const routedGaps = uniqueMatches(gapRouting, /known-gaps\.html#(gap-\d+)/g);
const missingGaps = [...openGaps].filter(gap => !routedGaps.has(gap));
if (missingGaps.length) {
    throw new Error(`open gaps are not routed: ${missingGaps.join(", ")}`);
}

console.log(`roadmap pages: ${roadmapPages.size}`);
console.log(`coverage rows: ${coverageRows}`);
console.log(`open gaps: ${openGaps.size}`);
console.log(`routed open gaps: ${openGaps.size - missingGaps.length}`);
console.log("roadmap execution checks OK");
