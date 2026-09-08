/* The landing page's Gameplay / Coding / Science switcher.
 *
 * The wiki serves three readers who want almost disjoint things from it: a
 * player who wants to know what a horse will look like, someone adding a gene,
 * and someone checking the model against real horse genetics. One index that
 * lists all ninety-odd pages to all three of them serves none of them.
 *
 * So the switcher repaints the page and hides every card whose page has nothing
 * for the current view. Which pages those are is declared once, in
 * wiki/pages.js - the same manifest the sidebar reads - so a page is never
 * listed in two places and cannot drift between them.
 *
 * A card whose page is not in the manifest stays visible in every view. That is
 * deliberate: the failure mode of a page nobody registered should be "shown too
 * often", not "silently unreachable".
 *
 * The choice is remembered (the same sessionStorage key wiki/tabs.js uses) and
 * pushed into every card link as ?view=, so choosing Science here and clicking
 * Flaxen opens Flaxen on its science tab.
 */
window.HG = window.HG || {};
(function (HG) {
    "use strict";

    var VIEWS = [
        { id: "gameplay", label: "Gameplay", hint: "playing with horses" },
        { id: "coding",   label: "Coding",   hint: "how it is built" },
        { id: "science",  label: "Science",  hint: "the real genetics" }
    ];

    var NOTE = {
        gameplay: "What the mod is to play: the horses, the genes you can see, the items and"
            + " the places. Pages that are only about the code are hidden.",
        coding: "The implementation: the model, the pipeline, the file formats, the API notes"
            + " and the project's own record of what is broken.",
        science: "The real-world genetics behind each gene — what is actually known about"
            + " it in horses, and where this mod knowingly departs from it."
    };

    function param(name) {
        var m = new RegExp("[?&]" + name + "=([^&#]*)").exec(window.location.search);
        return m ? decodeURIComponent(m[1]) : null;
    }

    function valid(v) {
        for (var i = 0; i < VIEWS.length; i++) {
            if (VIEWS[i].id === v) { return true; }
        }
        return false;
    }

    /** href on a card ("wiki/gene-dun.html") -> key in the manifest ("gene-dun.html"). */
    function key(href) {
        var clean = href.split("#")[0].split("?")[0];
        if (clean === "index.html" || clean === "" || clean === "./") { return "../index.html"; }
        return clean.replace(/^wiki\//, "");
    }

    function manifest() {
        var map = {};
        var sections = (HG.pages && HG.pages.SECTIONS) || [];
        sections.forEach(function (section) {
            section.items.forEach(function (item) {
                if (item.views) { map[item.href] = item.views; }
            });
        });
        return map;
    }

    function withView(href, view) {
        if (href.indexOf("?") >= 0) { return href; }
        var hash = "";
        var h = href.indexOf("#");
        if (h >= 0) { hash = href.slice(h); href = href.slice(0, h); }
        return href + "?view=" + view + hash;
    }

    function build() {
        var hero = document.querySelector(".hero-inner");
        var main = document.querySelector(".landing-main");
        if (!hero || !main) { return; }

        var views = manifest();

        // Remember each card's original href once, so switching views repeatedly
        // does not stack ?view= on top of itself.
        var cards = main.querySelectorAll("a.card");
        Array.prototype.forEach.call(cards, function (card) {
            card.setAttribute("data-href", card.getAttribute("href"));
        });

        var switcher = document.createElement("div");
        switcher.className = "view-switch";
        switcher.setAttribute("role", "group");
        switcher.setAttribute("aria-label", "Choose a view of the wiki");

        var note = document.createElement("p");
        note.className = "view-note";

        var buttons = {};
        VIEWS.forEach(function (v) {
            var b = document.createElement("button");
            b.type = "button";
            b.setAttribute("data-view", v.id);
            b.innerHTML = '<span class="swatch"></span>' + v.label
                + " <small>" + v.hint + "</small>";
            b.addEventListener("click", function () { show(v.id, true); });
            switcher.appendChild(b);
            buttons[v.id] = b;
        });

        var search = document.createElement("button");
        search.type = "button";
        search.className = "landing-search";
        search.setAttribute("data-search-launch", "");
        search.innerHTML = "<span>Search every page&hellip;</span><kbd>/</kbd>";

        var stats = hero.querySelector(".stats");
        if (stats) {
            stats.parentNode.insertBefore(switcher, stats.nextSibling);
        } else {
            hero.appendChild(switcher);
        }
        switcher.parentNode.insertBefore(note, switcher.nextSibling);
        note.parentNode.insertBefore(search, note.nextSibling);

        function show(view, remember) {
            document.documentElement.setAttribute("data-view", view);
            note.textContent = NOTE[view] || "";
            VIEWS.forEach(function (v) {
                var on = v.id === view;
                buttons[v.id].classList.toggle("active", on);
                buttons[v.id].setAttribute("aria-pressed", on ? "true" : "false");
            });

            Array.prototype.forEach.call(cards, function (card) {
                var href = card.getAttribute("data-href") || "";
                var allowed = views[key(href)];
                var on = !allowed || allowed.indexOf(view) >= 0;
                card.classList.toggle("view-hidden", !on);
                card.setAttribute("href", on ? withView(href, view) : href);
            });

            // A section whose every card just went is a heading over nothing.
            Array.prototype.forEach.call(main.querySelectorAll(".cards"), function (grid) {
                var shown = grid.querySelectorAll("a.card:not(.view-hidden)").length;
                grid.classList.toggle("view-hidden", shown === 0);
                var head = grid.previousElementSibling;
                if (head && head.classList.contains("section-head")) {
                    head.classList.toggle("view-hidden", shown === 0);
                }
            });

            if (remember) {
                if (HG.writeView) { HG.writeView(view); }
                try {
                    window.history.replaceState(null, "",
                        window.location.pathname + "?view=" + view);
                } catch (e) { /* file:// */ }
            }
        }

        var initial = param("view") || (HG.readView && HG.readView()) || "gameplay";
        show(valid(initial) ? initial : "gameplay", false);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", build);
    } else {
        build();
    }
})(window.HG);
