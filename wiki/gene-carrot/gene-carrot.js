// The gene-carrot recipe card that sits on every gene page: how you make this
// gene's Known Gene Splice carrot, and what feeding it does.
//
// WHY IT IS NOT WRITTEN INTO THE PAGES. There is one parameterised recipe, not
// one per gene (server/recipe/KnownGeneSpliceRecipe), so what actually differs
// between two genes is small and exact: whether a carrot exists at all, which
// rarity tier pays for it, whether the fed parent comes out heterozygous or
// homozygous for that gamete, and whether the Unknown splice may land on the
// locus. Those four are asked of the mod through the same wasm the preview
// window and the horse designer run. Fifty pages holding their own copy would
// be fifty pages to re-check the next time a gene was re-tiered - and the tier
// is the recipe's price, so a stale one is a wrong recipe.
//
// WHAT THIS FILE DOES HOLD, AND THE TWO PLACES IT ANSWERS TO:
//
//   * the SLOT LAYOUT - hair, paper, golden carrot, rarity item, in grid order.
//     menu/SpliceRecipeDisplay is canonical (it is what the Horse Browser
//     ghosts into the crafting grid); this is the wiki's rendering of it, and a
//     change there lands here.
//   * the TIER -> ITEM table. server/recipe/RarityItems is the authority and
//     deliberately lives on the recipe side, where common/ - and so the wasm -
//     cannot see it. wiki/carrots.html carries the same six rows in prose.
//
// Neither varies per gene, which is why a copy is tolerable here and a copy of
// the per-gene facts is not.
//
// USING IT. Two lines on a gene page:
//
//     <div class="gene-carrot" data-gene="horsegenetics.tobiano"></div>
//     <script defer src="gene-carrot/gene-carrot.js"></script>
//
// It needs the wiki served over http, for the same reason the preview window
// does: loading the wasm is a fetch.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var HERE = (function () {
    var self = document.currentScript;
    if (!self || !self.src) return "";
    return self.src.replace(/gene-carrot\.js(\?.*)?$/, "");
  })();
  var WIKI = HERE + "../";

  // GeneRarity tier -> what the recipe charges. See the header: RarityItems is
  // the authority, this is the label for it.
  var RARITY_ITEM = {
    COMMON: "Iron ingot",
    UNCOMMON: "Gold ingot",
    RARE: "Diamond",
    EPIC: "Emerald",
    LEGENDARY: "Netherite ingot",
    MYTHIC: "Nether star"
  };

  var TIER_NAME = {
    COMMON: "common", UNCOMMON: "uncommon", RARE: "rare",
    EPIC: "epic", LEGENDARY: "legendary", MYTHIC: "mythic"
  };

  // The four filled slots, in grid order, as menu/SpliceRecipeDisplay lays them
  // out. `rarity` is filled in per gene; everything else is the same for every
  // gene there is.
  function slots(carrot) {
    return [
      { name: "Horse hair", note: "or hair cloth", mod: true },
      { name: "Research paper", note: carrot.name, mod: true },
      { name: "Golden carrot", note: "" },
      { name: RARITY_ITEM[carrot.rarity] || "Rarity item",
        note: (TIER_NAME[carrot.rarity] || "?") + " tier" },
      null, null, null, null, null
    ];
  }

  // ---- loading ---------------------------------------------------------

  function script(src) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement("script");
      s.src = src;
      s.onload = resolve;
      s.onerror = function () { reject(new Error("could not load " + src)); };
      document.head.appendChild(s);
    });
  }

  /**
   * The mod, and nothing else - this card needs no three.js and no mesh. If the
   * page also carries a preview window the two share one wasm, because
   * HG.java.load() is memoised.
   */
  var booted = null;
  function boot() {
    if (booted) return booted;
    booted = Promise.resolve()
      .then(function () { return HG.java ? null : script(WIKI + "horse-designer/js/java.js"); })
      .then(function () { return HG.java.load(); });
    return booted;
  }

  // ---- one card --------------------------------------------------------

  function mount(host) {
    var geneKey = host.getAttribute("data-gene");
    host.classList.add("gene-carrot");
    host.innerHTML = '<div class="gc-boot">Asking the mod about this gene&rsquo;s carrot&hellip;</div>';

    boot().then(function (api) {
      draw(host, JSON.parse(api.geneCarrotJson(geneKey)), geneKey);
    }, function (err) {
      host.innerHTML = '<div class="gc-boot gc-failed">'
        + 'The recipe card could not load: ' + escapeHtml(err.message) + '.'
        + (location.protocol === "file:"
          ? ' This page is open from a file:// path, and reading the mod is a fetch -'
          + ' serve the repo with any static server, or read it on the published wiki.'
          : '')
        + '</div>';
    });
  }

  function draw(host, carrot, geneKey) {
    if (carrot.missing) {
      host.innerHTML = '<div class="gc-boot gc-failed">No gene is registered as <code>'
        + escapeHtml(geneKey) + '</code> - the page is naming a key the mod no longer has.</div>';
      return;
    }

    // A locus that turns its carrot off: the sex locus, the recessive lethals,
    // extension and agouti. Saying so is the whole card - there is no recipe to
    // draw, and an empty grid would read as "not written up yet".
    if (!carrot.hasCarrot) {
      host.innerHTML = '<div class="gc-none">'
        + '<strong>' + escapeHtml(carrot.name) + ' has no gene carrot.</strong> '
        + 'The parameterised recipe refuses a research paper naming it, so there is '
        + 'nothing to craft: a carrot for this locus would be nonsense or hostile. '
        + 'See <a href="carrots.html#known">Known Gene Splice carrots</a>.'
        + '</div>';
      return;
    }

    var cells = slots(carrot).map(function (slot) {
      if (!slot) return '<li class="gc-slot gc-empty"></li>';
      return '<li class="gc-slot' + (slot.mod ? " gc-mod" : "") + '">'
        + '<span class="gc-item">' + escapeHtml(slot.name) + '</span>'
        + (slot.note ? '<span class="gc-note">' + escapeHtml(slot.note) + '</span>' : "")
        + '</li>';
    }).join("");

    host.innerHTML = ''
      + '<div class="gc-craft">'
      + '<ul class="gc-grid">' + cells + '</ul>'
      + '<div class="gc-arrow" aria-hidden="true">&rarr;</div>'
      + '<div class="gc-out">'
      + '<span class="gc-item">Known Gene Splice carrot</span>'
      + '<span class="gc-note">' + escapeHtml(carrot.name) + ', '
      + (carrot.homozygous ? "homozygous" : "heterozygous") + '</span>'
      + '</div>'
      + '</div>'
      + '<p class="gc-say">'
      + 'Shapeless, and <strong>exactly those four items</strong> - anything else in the '
      + 'grid and it does not resolve. The gene is read off the paper at craft time, so '
      + 'there is no per-gene recipe to register. Feeding the carrot to a parent makes '
      + 'the game treat that parent as <strong>'
      + (carrot.homozygous ? "homozygous" : "heterozygous")
      + '</strong> for ' + escapeHtml(carrot.name) + ' when it forms that one gamete; '
      + 'ordinary Mendelian rules take it from there, so two carrot-fed parents give the '
      + 'usual 25/50/25. <a href="carrots.html#known">How the carrots work</a>.'
      + '</p>'
      + '<dl class="gc-facts">'
      + fact("Rarity tier", (TIER_NAME[carrot.rarity] || carrot.rarity)
        + ' &mdash; ' + escapeHtml(RARITY_ITEM[carrot.rarity] || "?"))
      // The weight itself is the gene's, off GeneRarity.lootWeight(). What the
      // other tiers weigh is not written down here - that is exactly the kind of
      // derived number that goes stale in prose.
      + fact("Paper loot weight", carrot.lootWeight
        + ' <span class="gc-dim">(relative &mdash; commoner genes turn up more often)</span>')
      + fact("Effect token", '<code>' + escapeHtml(carrot.effect) + '</code>')
      + fact("Unknown Gene Splice", carrot.unknownSpliceable
        ? 'may roll this locus'
        : '<span class="gc-off">never rolls this locus</span> '
        + '<span class="gc-dim">(<a href="carrots.html#blacklist">the blacklist</a> '
        + 'is derived, not listed)</span>')
      + '</dl>';
  }

  function fact(term, html) {
    return "<div><dt>" + term + "</dt><dd>" + html + "</dd></div>";
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"]/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
    });
  }

  function start() {
    var hosts = document.querySelectorAll(".gene-carrot[data-gene]");
    if (!hosts.length) return;
    var link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = HERE + "gene-carrot.css";
    document.head.appendChild(link);
    Array.prototype.forEach.call(hosts, mount);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }

  HG.geneCarrot = { mount: mount };
})(window.HG);
