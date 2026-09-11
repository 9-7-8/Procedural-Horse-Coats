// The baked one-horse picture of a gene - wiki/assets/gene-icons/<slug>.png,
// written by :common:bakeGeneIcons - for the gene hover cards.
//
// Shared by the horse designer (drawn onto its canvas, the twin of
// CustomHorseSpawnScreen's blurb) and the breed designer (an <img> in an HTML
// card). The game side is neoforge/client/GeneIcons, which reads the same PNGs
// out of the jar.
//
// A gene with no icon - one that paints nothing and so was never photographed,
// or a drop-in gene - is remembered as missing, and the card draws no picture
// rather than a broken-image box. Nothing here is genetic; it is a cache of
// Image objects keyed by gene key.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var cache = {};   // gene key -> { img, ok, done }

  /** The URL of a gene's icon, relative to a page one folder below wiki/. */
  function url(key, base) {
    var slug = key.slice(key.indexOf(".") + 1);
    return (base || "../assets/gene-icons/") + slug + ".png";
  }

  function entry(key, base) {
    var e = cache[key];
    if (e) return e;
    e = cache[key] = { img: new Image(), ok: false, done: false };
    e.img.onload = function () { e.ok = true; e.done = true; };
    e.img.onerror = function () { e.done = true; };
    e.img.src = url(key, base);
    return e;
  }

  HG.geneIcons = {
    /** The loaded Image, or null while it loads and for good if there is none. */
    image: function (key, base) {
      var e = entry(key, base);
      return e.ok ? e.img : null;
    },
    /** The URL, for an <img>. Pair with an onerror that hides it. */
    url: url
  };
})(window.HG);
