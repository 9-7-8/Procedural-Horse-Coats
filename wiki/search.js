/* Full-text search across the whole wiki.
 *
 * The sidebar has always had a filter box, but it only ever matched the names
 * of pages - so anything you could not already name was unfindable, which on a
 * wiki with seventy gene pages is most of it. This searches the text.
 *
 * WHERE THE TEXT COMES FROM. wiki/search-index.js, baked by
 * wiki/tools/build-search-index.mjs. It is loaded lazily, on the first
 * keystroke rather than on page load, because it is a megabyte or so of prose
 * and almost every visit never searches. Being a script rather than JSON is
 * deliberate: it has to work from a file:// path, where fetch does not.
 *
 * TABS. A hit knows which tab it is on, says so, and links to that tab
 * (?view=coding#anchor), so searching for something implementation-shaped
 * takes you to the coding tab of the page rather than its gameplay summary.
 *
 * Open with the box, with ctrl/cmd-K, or with "/".
 */
window.HG = window.HG || {};
(function (HG) {
    "use strict";

    var HERE = (function () {
        var self = document.currentScript;
        if (!self || !self.src) { return ""; }
        return self.src.replace(/search\.js(\?.*)?$/, "");
    })();
    var ROOT = HERE + "../";           // hrefs in the index are repo-root relative

    var TAB_LABEL = { gameplay: "Gameplay", coding: "Coding", science: "Science", all: "" };
    var MAX_PAGES = 40;
    var MAX_PER_PAGE = 3;

    var loading = null;
    function corpus() {
        if (HG.searchIndex) { return Promise.resolve(HG.searchIndex); }
        if (loading) { return loading; }
        loading = new Promise(function (resolve, reject) {
            var s = document.createElement("script");
            s.src = HERE + "search-index.js";
            s.onload = function () {
                if (HG.searchIndex) { resolve(HG.searchIndex); }
                else { reject(new Error("the index loaded but was empty")); }
            };
            s.onerror = function () { reject(new Error("could not load search-index.js")); };
            document.head.appendChild(s);
        });
        return loading;
    }

    function esc(s) {
        return String(s == null ? "" : s).replace(/[&<>"]/g, function (c) {
            return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
        });
    }

    function terms(q) {
        return q.toLowerCase().split(/\s+/).filter(function (t) { return t.length > 1; });
    }

    /**
     * Score one chunk. A word in the page title outranks a word in a heading,
     * which outranks a word in the body - so searching "flaxen" finds the
     * flaxen page before the twenty pages that mention it in passing.
     */
    function score(page, chunk, ts, phrase) {
        var title = page.title.toLowerCase();
        var head = chunk.h.toLowerCase();
        var body = chunk.t.toLowerCase();
        var total = 0;
        for (var i = 0; i < ts.length; i++) {
            var t = ts[i];
            var inBody = body.indexOf(t) >= 0;
            if (!inBody && title.indexOf(t) < 0 && head.indexOf(t) < 0) {
                return 0;                       // every term has to appear
            }
            if (title.indexOf(t) >= 0) { total += 24; }
            if (head.indexOf(t) >= 0) { total += 9; }
            if (inBody) {
                var n = body.split(t).length - 1;
                total += Math.min(n, 6);
            }
        }
        if (phrase && body.indexOf(phrase) >= 0) { total += 18; }
        if (phrase && title.indexOf(phrase) >= 0) { total += 40; }
        if (chunk.tab === "gameplay" || chunk.tab === "all") { total += 1; }
        return total;
    }

    function snippet(text, ts) {
        var low = text.toLowerCase();
        var at = -1;
        for (var i = 0; i < ts.length && at < 0; i++) { at = low.indexOf(ts[i]); }
        if (at < 0) { at = 0; }
        var from = Math.max(0, at - 90);
        var cut = text.slice(from, from + 240);
        if (from > 0) { cut = "…" + cut.replace(/^\S*\s/, ""); }
        if (from + 240 < text.length) { cut = cut.replace(/\s\S*$/, "") + "…"; }
        var out = esc(cut);
        ts.forEach(function (t) {
            var safe = t.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
            out = out.replace(new RegExp("(" + safe + ")", "gi"), "<mark>$1</mark>");
        });
        return out;
    }

    function run(index, q) {
        var ts = terms(q);
        if (!ts.length) { return []; }
        var phrase = q.trim().toLowerCase();
        var hits = [];
        index.pages.forEach(function (page) {
            var best = [];
            page.chunks.forEach(function (chunk) {
                var s = score(page, chunk, ts, phrase);
                if (s > 0) { best.push({ chunk: chunk, score: s }); }
            });
            if (!best.length) { return; }
            best.sort(function (a, b) { return b.score - a.score; });
            hits.push({
                page: page,
                score: best[0].score + best.length,
                best: best.slice(0, MAX_PER_PAGE)
            });
        });
        hits.sort(function (a, b) { return b.score - a.score; });
        return hits.slice(0, MAX_PAGES);
    }

    function href(page, chunk) {
        var url = ROOT + page.href;
        if (chunk.tab && chunk.tab !== "all") { url += "?view=" + chunk.tab; }
        if (chunk.id) { url += "#" + chunk.id; }
        return url;
    }

    function render(box, hits, q, ts) {
        if (!q.trim()) {
            box.innerHTML = '<p class="sr-empty">Search every page of the wiki &mdash; '
                + 'gene names, allele tokens, class names, anything written down.</p>';
            return;
        }
        if (!hits.length) {
            box.innerHTML = '<p class="sr-empty">Nothing matches <strong>' + esc(q)
                + '</strong>.</p>';
            return;
        }
        var h = '<p class="sr-count">' + hits.length + (hits.length === 1 ? " page" : " pages")
            + '</p>';
        hits.forEach(function (hit) {
            h += '<div class="sr-page">';
            h += '<a class="sr-title" href="' + esc(href(hit.page, hit.best[0].chunk)) + '">'
                + esc(hit.page.title) + '</a>';
            hit.best.forEach(function (b) {
                var tab = TAB_LABEL[b.chunk.tab] || "";
                h += '<a class="sr-hit" href="' + esc(href(hit.page, b.chunk)) + '">';
                if (tab) {
                    h += '<span class="sr-tab sr-' + esc(b.chunk.tab) + '">' + tab + '</span>';
                }
                if (b.chunk.h) { h += '<span class="sr-head">' + esc(b.chunk.h) + '</span>'; }
                h += '<span class="sr-snip">' + snippet(b.chunk.t, ts) + '</span>';
                h += '</a>';
            });
            h += '</div>';
        });
        box.innerHTML = h;
    }

    var overlay = null;
    var input = null;
    var results = null;

    function open(seed) {
        if (!overlay) { create(); }
        overlay.hidden = false;
        document.body.classList.add("search-open");
        if (typeof seed === "string") { input.value = seed; }
        input.focus();
        input.select();
        query();
    }

    function close() {
        if (overlay) { overlay.hidden = true; }
        document.body.classList.remove("search-open");
    }

    var timer = null;
    function query() {
        var q = input.value;
        window.clearTimeout(timer);
        timer = window.setTimeout(function () {
            if (!q.trim()) { render(results, [], q, []); return; }
            results.innerHTML = '<p class="sr-empty">Searching&hellip;</p>';
            corpus().then(function (index) {
                if (input.value !== q) { return; }        // typed on
                render(results, run(index, q), q, terms(q));
            }, function (err) {
                results.innerHTML = '<p class="sr-empty">The search index could not load: '
                    + esc(err.message) + '. Run <code>node wiki/tools/build-search-index.mjs</code>'
                    + ' if this is a working copy.</p>';
            });
        }, 110);
    }

    function create() {
        overlay = document.createElement("div");
        overlay.className = "search-overlay";
        overlay.hidden = true;
        overlay.innerHTML = ''
            + '<div class="search-panel" role="dialog" aria-modal="true" aria-label="Search the wiki">'
            + '<div class="search-head">'
            + '<input type="search" class="search-input" placeholder="Search the wiki&hellip;"'
            + ' autocomplete="off" spellcheck="false" aria-label="Search the wiki">'
            + '<button type="button" class="search-close" aria-label="Close search">Esc</button>'
            + '</div>'
            + '<div class="search-results"></div>'
            + '</div>';
        document.body.appendChild(overlay);

        input = overlay.querySelector(".search-input");
        results = overlay.querySelector(".search-results");

        input.addEventListener("input", query);
        overlay.querySelector(".search-close").addEventListener("click", close);
        overlay.addEventListener("mousedown", function (ev) {
            if (ev.target === overlay) { close(); }
        });
        // Warm the index as soon as the box is focused, so the first query is
        // not the thing that waits for a megabyte.
        input.addEventListener("focus", function () { corpus().catch(function () {}); });
        render(results, [], "", []);
    }

    document.addEventListener("keydown", function (ev) {
        if ((ev.ctrlKey || ev.metaKey) && ev.key && ev.key.toLowerCase() === "k") {
            ev.preventDefault();
            open();
            return;
        }
        if (ev.key === "Escape" && overlay && !overlay.hidden) {
            close();
            return;
        }
        var active = document.activeElement || document.body;
        var typing = /^(INPUT|TEXTAREA|SELECT)$/.test(active.tagName) || active.isContentEditable;
        if (ev.key === "/" && !typing && (!overlay || overlay.hidden)) {
            ev.preventDefault();
            open("");
        }
    });

    function wire() {
        var launchers = document.querySelectorAll("[data-search-launch]");
        Array.prototype.forEach.call(launchers, function (el) {
            el.addEventListener("click", function (ev) { ev.preventDefault(); open(); });
        });
    }

    HG.search = { open: open, close: close };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", wire);
    } else {
        wire();
    }
})(window.HG);
