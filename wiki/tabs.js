/* Gameplay / Coding / Science tabs.
 *
 * A page opts in by wrapping its body in panels:
 *
 *     <section class="tab-panel" data-tab="gameplay"> ... </section>
 *     <section class="tab-panel" data-tab="coding">   ... </section>
 *     <section class="tab-panel" data-tab="science">  ... </section>
 *
 * Anything before the first panel (the eyebrow, the h1, the lede) stays put and
 * is shown on every tab - it is the page's identity, not one view of it.
 *
 * A page with no panels is left completely alone, so the pages that are one
 * subject with one audience (philosophy, the session log) need no change.
 *
 * WHICH TAB OPENS. In order: an explicit ?view= in the URL, then a #hash that
 * names something inside a panel, then the view the reader last chose (the
 * index's view switcher writes the same key), then gameplay. A view the page
 * does not have falls back to the first one it does, so a link into the science
 * view of a page with no science tab still lands somewhere sensible rather than
 * on a blank page.
 *
 * Switching rewrites ?view= with replaceState, so a copied URL reopens on the
 * tab the reader was actually looking at.
 *
 * Works from file:// - it only ever touches the DOM.
 */
window.HG = window.HG || {};
(function (HG) {
    "use strict";

    var TABS = [
        { id: "gameplay", label: "Gameplay", hint: "What it does in game" },
        { id: "coding",   label: "Coding",   hint: "How it is implemented" },
        { id: "science",  label: "Science",  hint: "The real-world genetics" }
    ];
    var STORE = "hg-view";
    var DEFAULT = "gameplay";

    HG.tabs = { TABS: TABS, STORE: STORE, DEFAULT: DEFAULT };

    /** The view the reader last chose anywhere on the wiki, or null. */
    HG.readView = function () {
        try {
            var v = window.sessionStorage.getItem(STORE);
            return valid(v) ? v : null;
        } catch (e) {
            return null;   // private mode, file:// in some browsers
        }
    };

    HG.writeView = function (view) {
        try { window.sessionStorage.setItem(STORE, view); } catch (e) { /* fine */ }
    };

    function valid(v) {
        for (var i = 0; i < TABS.length; i++) {
            if (TABS[i].id === v) { return true; }
        }
        return false;
    }

    function param(name) {
        var m = new RegExp("[?&]" + name + "=([^&#]*)").exec(window.location.search);
        return m ? decodeURIComponent(m[1]) : null;
    }

    function build() {
        var article = document.querySelector("article.doc");
        if (!article) { return; }

        var panels = [];
        Array.prototype.forEach.call(article.children, function (el) {
            if (el.classList && el.classList.contains("tab-panel")) { panels.push(el); }
        });
        if (!panels.length) { return; }      // an untabbed page, left alone

        var have = {};
        panels.forEach(function (p) { have[p.getAttribute("data-tab")] = p; });

        // The bar goes immediately before the first panel, so the page head
        // (eyebrow, title, lede) stays above it on every tab.
        var bar = document.createElement("div");
        bar.className = "tab-bar";
        bar.setAttribute("role", "tablist");

        var buttons = {};
        TABS.forEach(function (tab) {
            if (!have[tab.id]) { return; }
            var b = document.createElement("button");
            b.type = "button";
            b.className = "tab-btn tab-" + tab.id;
            b.setAttribute("role", "tab");
            b.setAttribute("data-tab", tab.id);
            b.id = "tab-btn-" + tab.id;
            b.innerHTML = '<span class="tab-label">' + tab.label + "</span>"
                + '<span class="tab-hint">' + tab.hint + "</span>";
            b.addEventListener("click", function () { show(tab.id, true); });
            bar.appendChild(b);
            buttons[tab.id] = b;

            have[tab.id].setAttribute("role", "tabpanel");
            have[tab.id].setAttribute("aria-labelledby", b.id);
        });
        panels[0].parentNode.insertBefore(bar, panels[0]);

        function show(view, userChose) {
            if (!have[view]) {
                for (var i = 0; i < TABS.length; i++) {
                    if (have[TABS[i].id]) { view = TABS[i].id; break; }
                }
            }
            TABS.forEach(function (tab) {
                var on = tab.id === view;
                if (have[tab.id]) { have[tab.id].hidden = !on; }
                if (buttons[tab.id]) {
                    buttons[tab.id].classList.toggle("active", on);
                    buttons[tab.id].setAttribute("aria-selected", on ? "true" : "false");
                    buttons[tab.id].tabIndex = on ? 0 : -1;
                }
            });
            // Retheming is one attribute on <html>: every accent on the page
            // reads var(--view-accent), which the stylesheet redefines per view.
            document.documentElement.setAttribute("data-view", view);
            HG.writeView(view);
            if (userChose) {
                var url = window.location.pathname + "?view=" + view + window.location.hash;
                try { window.history.replaceState(null, "", url); } catch (e) { /* file:// */ }
                document.dispatchEvent(new CustomEvent("hg:view", { detail: view }));
            }
            HG.view = view;
        }

        // A #hash that names something inside a panel wins over a stored view -
        // otherwise a link straight to a section could open the tab that hides it.
        var hashPanel = null;
        if (window.location.hash.length > 1) {
            var target = null;
            try { target = article.querySelector(window.location.hash); } catch (e) { target = null; }
            for (var p = target; p; p = p.parentNode) {
                if (p.classList && p.classList.contains("tab-panel")) {
                    hashPanel = p.getAttribute("data-tab");
                    break;
                }
            }
        }

        var initial = param("view") || hashPanel || HG.readView() || DEFAULT;
        show(valid(initial) ? initial : DEFAULT, false);

        // The browser could not scroll to a hash that was hidden at load.
        if (hashPanel && window.location.hash.length > 1) {
            var el = article.querySelector(window.location.hash);
            if (el) { window.setTimeout(function () { el.scrollIntoView(); }, 0); }
        }

        // An in-page link to a section on another tab switches to it.
        document.addEventListener("click", function (ev) {
            var a = ev.target.closest ? ev.target.closest("a") : null;
            if (!a) { return; }
            var href = a.getAttribute("href") || "";
            if (href.charAt(0) !== "#" || href.length < 2) { return; }
            var dest = null;
            try { dest = article.querySelector(href); } catch (e) { return; }
            if (!dest) { return; }
            for (var q = dest; q; q = q.parentNode) {
                if (q.classList && q.classList.contains("tab-panel")) {
                    var want = q.getAttribute("data-tab");
                    if (want !== HG.view) { show(want, true); }
                    break;
                }
            }
        });

        HG.showTab = show;
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", build);
    } else {
        build();
    }
})(window.HG);
