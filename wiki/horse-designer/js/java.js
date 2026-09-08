// Loads the mod itself into the page.
//
// web.wasm is common/ compiled by TeaVM - not a port of it, not a retelling of
// it, the actual bytecode. So the coat this page shows is the coat the game
// bakes, byte for byte, and there is no parity to keep. Regenerate it with
// `./gradlew :web:bakeDesignerAssets`.
//
// This file's whole job is to get the four PNGs the pipeline needs across the
// boundary as int[] and then get out of the way. Nothing here knows what a gene
// is; ask HG.java.api.
//
// NOTE: this page cannot run from a file:// path. Loading wasm is a fetch, and
// a local file is its own opaque origin, so the browser refuses. Serve the repo
// with any static server, or use the published wiki.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // Where the wasm and its four PNGs live, worked out from THIS FILE's own URL
  // rather than from the page's. The designer is not the only page that loads
  // the mod any more - every gene page's preview window does too (see
  // wiki/gene-preview/), and it sits one directory up, so a path relative to
  // the document would resolve somewhere else on every caller. The script's own
  // src is the one thing that is the same wherever it is included from.
  var BASE = (function () {
    var self = document.currentScript;
    if (!self || !self.src) return "";       // inlined or bundled - assume alongside
    return self.src.replace(/js\/java\.js(\?.*)?$/, "");
  })();

  var WASM = BASE + "wasm/web.wasm";
  var RUNTIME = BASE + "wasm/web.wasm-runtime.js";

  var ASSETS = {
    gradient: BASE + "assets/redblackgradient.png",
    bluepink: BASE + "assets/lutbluepink.png",
    adult: BASE + "assets/horse_white.png",
    baby: BASE + "assets/horse_white_baby.png"
  };

  // The two name word tables, for the same reason the textures are handed in
  // as pixels: TeaVM is weakest at reading its own classpath.
  var NAMES = [BASE + "assets/horse-names-alpha.txt", BASE + "assets/horse-names-beta.txt"];

  // Every breed, as one array - common/src/main/resources/horsegenetics/breeds/
  // rewritten by :common:bakeBreedFiles. Same reason again: the game reads that
  // folder off its classpath and a wasm build cannot. Regenerate it in the same
  // breath as the breed files, or the tools are a version behind the mod.
  var BREEDS = BASE + "assets/breeds.json";

  var api = null;

  /**
   * Decode a PNG into ARGB ints, the layout every int[] in the pipeline uses.
   *
   * <p>Exposed as HG.java.decode because the LUT lab (wiki/lut-lab/) hands the
   * pipeline a gradient the reader picked off their own disk, and a second
   * opinion about channel order is exactly the kind of thing that renders a
   * whole page of horses blue and looks deliberate.
   */
  function decode(url) {
    return new Promise(function (resolve, reject) {
      var img = new Image();
      img.onload = function () {
        var c = document.createElement("canvas");
        c.width = img.width;
        c.height = img.height;
        var g = c.getContext("2d", { willReadFrequently: true });
        g.drawImage(img, 0, 0);
        var d = g.getImageData(0, 0, img.width, img.height).data;
        var out = new Int32Array(img.width * img.height);
        for (var i = 0; i < out.length; i++) {
          out[i] = (d[i * 4 + 3] << 24) | (d[i * 4] << 16) | (d[i * 4 + 1] << 8) | d[i * 4 + 2];
        }
        resolve({ pixels: out, width: img.width, height: img.height });
      };
      img.onerror = function () { reject(new Error("could not load " + url)); };
      img.src = url;
    });
  }

  function scriptTag(src) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement("script");
      s.src = src;
      s.onload = resolve;
      s.onerror = function () { reject(new Error("could not load " + src)); };
      document.head.appendChild(s);
    });
  }

  /**
   * Bring up the mod. Resolves with the exported API once the registry is
   * built and all four textures are in Java's hands.
   *
   * <p>Memoised, because a page may have more than one thing that wants the
   * mod: a gene page carries a preview window and a gene-carrot card, and each
   * asks for the API without knowing the other exists. Loading the wasm twice
   * would work and would be two megabytes and two registries for no reason.
   */
  var loading = null;
  function load() {
    if (loading) return loading;
    loading = boot();
    return loading;
  }

  function boot() {
    return scriptTag(RUNTIME)
      .then(function () {
        if (!window.TeaVM || !window.TeaVM.wasmGC) {
          throw new Error("the wasm runtime did not install itself");
        }
        return window.TeaVM.wasmGC.load(WASM);
      })
      .then(function (app) {
        api = app.exports;
        api.main([]);
        return Promise.all([
          decode(ASSETS.gradient), decode(ASSETS.bluepink),
          decode(ASSETS.adult), decode(ASSETS.baby),
          fetch(NAMES[0]).then(function (r) { return r.text(); }),
          fetch(NAMES[1]).then(function (r) { return r.text(); }),
          fetch(BREEDS).then(function (r) { return r.text(); })
        ]);
      })
      .then(function (imgs) {
        // Breeds first: the registry is lazy, so anything that asks before this
        // gets an empty list and caches nothing to correct later.
        var breedProblems = JSON.parse(api.registerBreeds(imgs[6]));
        for (var i = 0; i < breedProblems.length; i++) {
          console.warn("[horsegenetics] breeds: " + breedProblems[i]);
        }
        api.setNameWords(imgs[4], imgs[5]);
        api.setGradient(imgs[0].pixels, imgs[0].width, imgs[0].height);
        // Keyed exactly as LutContribution.lutResources() keys it, so the LUT
        // locus resolves against the right chart.
        api.setAlternateGradient("bluepink", imgs[1].pixels, imgs[1].width, imgs[1].height);
        api.setTemplate(true, imgs[2].pixels);
        api.setTemplate(false, imgs[3].pixels);
        if (!api.ready()) {
          throw new Error("the pipeline did not accept its textures");
        }
        return api;
      });
  }

  /**
   * Confirm the JavaScript geometry port still agrees with the Java it was
   * copied from, on every load rather than whenever someone remembers to run
   * check-parity.mjs. The mesh builder is view code and stays in JS; the tables
   * it reads are the game's, and this is what says so.
   *
   * @return null when they agree, or a description of the first disagreement
   */
  function checkGeometry(adult) {
    if (!HG.geometry) return null;
    var skin = adult ? "ADULT" : "BABY";
    var probe = JSON.parse(api.geometryProbeJson(adult));
    for (var i = 0; i < probe.length; i++) {
      var want = probe[i];
      var got = HG.geometry.sample(skin, want.px, want.py);
      if (!got) return "texel " + want.px + "," + want.py + " maps to nothing in JS";
      if (got.part !== want.part || got.face !== want.face) {
        return "texel " + want.px + "," + want.py + ": Java says " + want.part + "/" + want.face
          + ", JS says " + got.part + "/" + got.face;
      }
      if (Math.abs(got.point.x - want.x) > 0.01
        || Math.abs(got.point.y - want.y) > 0.01
        || Math.abs(got.point.z - want.z) > 0.01) {
        return "texel " + want.px + "," + want.py + " lands at a different body point";
      }
    }
    return null;
  }

  HG.java = {
    load: load,
    decode: decode,
    checkGeometry: checkGeometry,
    get api() { return api; }
  };
})(window.HG);
