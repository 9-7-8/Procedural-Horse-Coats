/* Shared wiki navigation.
 *
 * Every page in wiki/ loads this and gets the same sidebar. The page list is
 * NOT here - it is wiki/pages.js, which the landing page reads too, so a page
 * is registered in one place and appears in both. Load pages.js first.
 *
 * The active link is worked out from the filename, so nothing per-page has to
 * be set.
 *
 * VIEWS. The wiki is read in one of three views - Gameplay, Coding, Science.
 * The sidebar shows only the pages that have something for the current view,
 * and carries the view through every link it builds, so choosing Science on the
 * landing page and clicking Flaxen lands on Flaxen's science tab. The view
 * itself is chosen by the tabs (wiki/tabs.js) or the landing page's switcher;
 * this only reads it.
 *
 * The old box filtered page names. It is now the launcher for the real
 * full-text search (wiki/search.js) - the names were never the hard part.
 *
 * Works from file:// as well as a web server (it builds DOM, it does not
 * fetch anything).
 */
(function () {
    "use strict";

    var HG = window.HG || (window.HG = {});

    function sections() {
        return (HG.pages && HG.pages.SECTIONS) || [];
    }

    function basename(path) {
        var i = path.lastIndexOf("/");
        var name = i < 0 ? path : path.slice(i + 1);
        return name === "" ? "index.html" : name;
    }

    function param(name) {
        var m = new RegExp("[?&]" + name + "=([^&#]*)").exec(window.location.search);
        return m ? decodeURIComponent(m[1]) : null;
    }

    /** The view being read, if the reader has expressed one. */
    function currentView() {
        var v = param("view") || (HG.readView && HG.readView());
        return v === "gameplay" || v === "coding" || v === "science" ? v : null;
    }

    /**
     * Carry the view through a link, so the destination opens on the tab the
     * reader is already reading in. A page that has no such tab falls back on
     * its own (wiki/tabs.js), so this never has to know what a page contains.
     */
    function withView(href, view) {
        if (!view || href.indexOf("?") >= 0) { return href; }
        var hash = "";
        var h = href.indexOf("#");
        if (h >= 0) { hash = href.slice(h); href = href.slice(0, h); }
        return href + "?view=" + view + hash;
    }

    function build() {
        var here = basename(window.location.pathname);
        var view = currentView();

        var nav = document.createElement("nav");
        nav.className = "sidebar";
        nav.id = "wiki-nav";

        var brand = document.createElement("a");
        brand.className = "brand";
        brand.href = withView("../index.html", view);
        brand.innerHTML = "<strong>Procedural Horse Genetics</strong><span>Wiki</span>";
        nav.appendChild(brand);

        var search = document.createElement("button");
        search.type = "button";
        search.className = "nav-search";
        search.setAttribute("data-search-launch", "");
        search.innerHTML = "<span>Search the wiki&hellip;</span><kbd>/</kbd>";
        nav.appendChild(search);

        sections().forEach(function (section) {
            var items = section.items.filter(function (item) {
                if (!view || !item.views) { return true; }
                return item.views.indexOf(view) >= 0;
            });
            if (!items.length) { return; }

            var group = document.createElement("div");
            group.className = "nav-group";

            var h = document.createElement("h4");
            h.textContent = section.title;
            group.appendChild(h);

            var ul = document.createElement("ul");
            items.forEach(function (item) {
                var li = document.createElement("li");
                var a = document.createElement("a");
                a.href = withView(item.href, view);
                a.className = "k-" + item.kind;
                if (basename(item.href) === here) {
                    a.className += " active";
                    a.setAttribute("aria-current", "page");
                }
                a.innerHTML = '<span class="dot"></span>';
                a.appendChild(document.createTextNode(item.text));
                li.appendChild(a);
                ul.appendChild(li);
            });
            group.appendChild(ul);
            nav.appendChild(group);
        });

        var toggle = document.createElement("button");
        toggle.className = "nav-toggle";
        toggle.type = "button";
        toggle.textContent = "☰ Menu";
        toggle.addEventListener("click", function () {
            document.body.classList.toggle("nav-open");
        });

        document.body.insertBefore(nav, document.body.firstChild);
        document.body.insertBefore(toggle, document.body.firstChild);
    }

    /** Tear the sidebar down and build it again for the current view. */
    function rebuild() {
        var old = document.getElementById("wiki-nav");
        var oldToggle = document.querySelector(".nav-toggle");
        if (old) { old.parentNode.removeChild(old); }
        if (oldToggle) { oldToggle.parentNode.removeChild(oldToggle); }
        build();
    }

    // Switching tab re-filters the sidebar, so the page list always matches the
    // tab being read rather than the one the page was opened on. Registered
    // once, out here: doing it inside build() would add a fresh listener on
    // every rebuild, and each switch would then cost one more rebuild than the
    // last.
    document.addEventListener("hg:view", rebuild);

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", build);
    } else {
        build();
    }
})();
