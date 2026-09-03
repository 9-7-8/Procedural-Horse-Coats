/* Shared wiki navigation.
 *
 * Every page in wiki/ loads this and gets the same sidebar. Add a page in ONE
 * place - the SECTIONS array below - and it appears everywhere. The active
 * link is worked out from the filename, so nothing per-page has to be set.
 *
 * Works from file:// as well as a web server (it builds DOM, it does not
 * fetch anything).
 */
(function () {
    "use strict";

    var SECTIONS = [
        {
            title: "Start here",
            items: [
                { href: "../index.html", text: "Wiki home", kind: "core" },
                { href: "philosophy.html", text: "Philosophy", kind: "core" },
                { href: "genetics-model.html", text: "The genetics model", kind: "core" },
                { href: "breeding.html", text: "Breeding & pedigree", kind: "core" }
            ]
        },
        {
            title: "The coat engine",
            items: [
                { href: "pipeline.html", text: "Three-phase pipeline", kind: "core" },
                { href: "body-space.html", text: "Body space & regions", kind: "core" }
            ]
        },
        {
            title: "For modders",
            items: [
                { href: "modding.html", text: "Writing a gene", kind: "core" },
                { href: "api-reference.html", text: "Class abstractions", kind: "core" },
                { href: "horse-traits.html", text: "Trait & effect architecture", kind: "magical" }
            ]
        },
        {
            title: "Natural genes",
            items: [
                { href: "gene-extension.html", text: "Extension", kind: "natural" },
                { href: "gene-agouti.html", text: "Agouti (bay / seal)", kind: "natural" },
                { href: "gene-cream.html", text: "Cream", kind: "natural" },
                { href: "gene-pearl.html", text: "Pearl", kind: "natural" },
                { href: "gene-champagne.html", text: "Champagne", kind: "natural" },
                { href: "gene-grey.html", text: "Grey (dapple)", kind: "natural" },
                { href: "gene-white.html", text: "Dominant white", kind: "natural" },
                { href: "gene-splash.html", text: "Splash white", kind: "natural" }
            ]
        },
        {
            title: "Magical genes",
            items: [
                { href: "gene-pink-hair.html", text: "Pink hair", kind: "magical" },
                { href: "gene-magic-zebra.html", text: "Magic zebra", kind: "magical" },
                { href: "gene-suntouched.html", text: "Suntouched", kind: "magical" },
                { href: "gene-waterborn.html", text: "Waterborn", kind: "magical" },
                { href: "gene-test.html", text: "Test (diagnostic)", kind: "magical" }
            ]
        },
        {
            title: "Project",
            items: [
                { href: "verification.html", text: "To be verified", kind: "core" },
                { href: "roadmap.html", text: "Roadmap / backlog", kind: "core" }
            ]
        },
        {
            title: "Tools",
            items: [
                { href: "gene-creator/index.html", text: "Gene creator", kind: "tool" },
                { href: "gene-format.html", text: "Gene file format", kind: "tool" },
                { href: "gene-effects.html", text: "Gene effects", kind: "tool" }
            ]
        }
    ];

    function basename(path) {
        var i = path.lastIndexOf("/");
        var name = i < 0 ? path : path.slice(i + 1);
        return name === "" ? "index.html" : name;
    }

    function build() {
        var here = basename(window.location.pathname);

        var nav = document.createElement("nav");
        nav.className = "sidebar";
        nav.id = "wiki-nav";

        var brand = document.createElement("a");
        brand.className = "brand";
        brand.href = "../index.html";
        brand.innerHTML = "<strong>Procedural Horse Genetics</strong><span>Wiki</span>";
        nav.appendChild(brand);

        var filter = document.createElement("input");
        filter.type = "search";
        filter.className = "nav-filter";
        filter.placeholder = "Filter pages…";
        filter.setAttribute("aria-label", "Filter wiki pages");
        nav.appendChild(filter);

        SECTIONS.forEach(function (section) {
            var group = document.createElement("div");
            group.className = "nav-group";

            var h = document.createElement("h4");
            h.textContent = section.title;
            group.appendChild(h);

            var ul = document.createElement("ul");
            section.items.forEach(function (item) {
                var li = document.createElement("li");
                var a = document.createElement("a");
                a.href = item.href;
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

        filter.addEventListener("input", function () {
            var q = filter.value.trim().toLowerCase();
            nav.querySelectorAll(".nav-group").forEach(function (group) {
                var shown = 0;
                group.querySelectorAll("li").forEach(function (li) {
                    var hit = q === "" || li.textContent.toLowerCase().indexOf(q) >= 0;
                    li.classList.toggle("nav-hidden", !hit);
                    if (hit) { shown++; }
                });
                group.classList.toggle("nav-hidden", shown === 0);
            });
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

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", build);
    } else {
        build();
    }
})();
