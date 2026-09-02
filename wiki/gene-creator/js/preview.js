// Bakes the coat you are looking at: base coat, then the gene under test,
// through the same three phases the game runs.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var geo = HG.geometry;
  var N = geo.SHEET_SIZE;
  var assets = null;

  function decode(dataUri, cb) {
    var img = new Image();
    img.onload = function () {
      var c = document.createElement("canvas");
      c.width = img.width;
      c.height = img.height;
      var ctx = c.getContext("2d", { willReadFrequently: true });
      ctx.drawImage(img, 0, 0);
      var d = ctx.getImageData(0, 0, img.width, img.height).data;
      var out = new Uint32Array(img.width * img.height);
      for (var i = 0; i < out.length; i++) {
        out[i] = ((d[i * 4 + 3] << 24) | (d[i * 4] << 16) | (d[i * 4 + 1] << 8) | d[i * 4 + 2]) >>> 0;
      }
      cb({ pixels: out, width: img.width, height: img.height });
    };
    img.src = dataUri;
  }

  function load(done) {
    if (assets) { done(assets); return; }
    var t = HG.textures;
    decode(t.ADULT_TEMPLATE_PNG, function (adult) {
      decode(t.BABY_TEMPLATE_PNG, function (baby) {
        decode(t.GRADIENT_PNG, function (grad) {
          assets = {
            ADULT: adult.pixels,
            BABY: baby.pixels,
            lut: new HG.fields.GradientLut(grad.pixels, grad.width, grad.height)
          };
          done(assets);
        });
      });
    });
  }

  /** Does this gene express at this dose, by its declared dominance? */
  function expresses(spec, dose) {
    if (dose <= 0) return false;
    return spec.dominance === "RECESSIVE" ? dose >= 2 : true;
  }

  /**
   * opts: { spec, skin, baseCoatId, seed, dose, coverageLayer }
   * Returns the composed sheet plus the intermediate fields, so the UI can show
   * pigment and coverage as well as the finished coat.
   */
  function bake(opts) {
    var spec = opts.spec;
    var skin = opts.skin;
    var base = HG.baseCoats.byId(opts.baseCoatId);
    var seedLow = opts.seed >>> 0;
    var showsGene = expresses(spec, opts.dose);

    // Seed the draw the way the game does: SeededRng(seed, geneKey).
    var s = HG.noise.xor(HG.noise.u64(0, seedLow),
      HG.noise.mul(HG.noise.fromInt(javaHashCode(spec.key || "")), HG.noise.K1));
    var values = HG.specEngine.drawValues(spec, s.h, s.l, opts.dose);

    var coverage = null;
    var pigmentBeforeGene = null;

    var naturals = [function (field) {
      var f = field.mutableCopy();
      base.build(skin, f);
      return f;
    }];

    if (spec.phase === "natural") {
      naturals.push(function (field) {
        pigmentBeforeGene = field.mutableCopy();
        if (opts.coverageLayer >= 0 && spec.layers[opts.coverageLayer]) {
          coverage = HG.specEngine.coverageMap(spec, opts.coverageLayer, values, skin, field);
        }
        return showsGene ? HG.specEngine.restrict(spec, values, skin, field) : null;
      });
    }

    var magicals = [];
    if (spec.phase === "magical") {
      magicals.push(function (pigment, colour) {
        pigmentBeforeGene = pigment;
        if (opts.coverageLayer >= 0 && spec.layers[opts.coverageLayer]) {
          coverage = HG.specEngine.coverageMap(spec, opts.coverageLayer, values, skin, pigment);
        }
        return showsGene ? HG.specEngine.tint(spec, values, skin, pigment, colour) : null;
      });
    }

    var result = HG.fields.compose({
      skin: skin,
      template: assets[skin === "BABY" ? "BABY" : "ADULT"],
      lut: assets.lut,
      naturals: naturals,
      magicals: magicals
    });
    result.coverage = coverage;
    result.values = values;
    result.expresses = showsGene;
    result.pigmentBeforeGene = pigmentBeforeGene;
    return result;
  }

  function javaHashCode(s) {
    var h = 0;
    for (var i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
    return h;
  }

  /** ARGB sheet -> ImageData, optionally with a coverage heat overlay. */
  function toImageData(ctx, pixels, coverage) {
    var img = ctx.createImageData(N, N);
    for (var i = 0; i < N * N; i++) {
      var p = pixels[i];
      var r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF, a = (p >>> 24) & 0xFF;
      if (coverage) {
        var k = coverage[i];
        if (k > 0) {
          // Magenta over the covered texels, proportional - reads clearly on
          // every base coat, including a cremello.
          r = Math.round(r * (1 - k) + 255 * k);
          g = Math.round(g * (1 - k) + 0 * k);
          b = Math.round(b * (1 - k) + 200 * k);
          a = Math.max(a, Math.round(255 * k));
        }
      }
      img.data[i * 4] = r;
      img.data[i * 4 + 1] = g;
      img.data[i * 4 + 2] = b;
      img.data[i * 4 + 3] = a;
    }
    return img;
  }

  HG.preview = {
    load: load,
    bake: bake,
    toImageData: toImageData,
    expresses: expresses,
    SHEET: N
  };
})(window.HG);
