// A JavaScript port of the coat pipeline's data structures and composer:
// common/coat/pattern/PigmentField, ColorField, GradientLut and
// CoatTextureComposer.
//
// The whole reason the preview is trustworthy is that it runs the same three
// phases in the same order with the same constants - every texel starts at max
// red and max black, natural layers push pigment down, the survivors resolve
// through the red/black gradient, magical layers add signed RGB on top, and the
// result multiplies onto the white template. Change a constant here only when
// the Java changes.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var N = HG.geometry.SHEET_SIZE;

  function clamp01(v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
  function cap(v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

  // ---- PigmentField ----------------------------------------------------

  function PigmentField(size) {
    this.size = size || N;
    this.red = new Float32Array(this.size * this.size).fill(1);
    this.black = new Float32Array(this.size * this.size).fill(1);
  }

  PigmentField.prototype.redAt = function (px, py) { return this.red[py * this.size + px]; };
  PigmentField.prototype.blackAt = function (px, py) { return this.black[py * this.size + px]; };

  PigmentField.prototype.mutableCopy = function () {
    var c = new PigmentField(this.size);
    c.red.set(this.red);
    c.black.set(this.black);
    return c;
  };

  PigmentField.prototype.setRed = function (px, py, v) { this.red[py * this.size + px] = clamp01(v); };
  PigmentField.prototype.setBlack = function (px, py, v) { this.black[py * this.size + px] = clamp01(v); };

  PigmentField.prototype.restrictRed = function (px, py, amount) {
    var i = py * this.size + px;
    this.red[i] = clamp01(this.red[i] * (1 - amount));
  };

  PigmentField.prototype.restrictBlack = function (px, py, amount) {
    var i = py * this.size + px;
    this.black[i] = clamp01(this.black[i] * (1 - amount));
  };

  /** black *= keepBlack; red = red * keepRed + blackBefore * blackTint. */
  PigmentField.prototype.dilute = function (px, py, keepRed, keepBlack, blackTint) {
    var i = py * this.size + px;
    var b = this.black[i];
    this.red[i] = clamp01(this.red[i] * keepRed + b * blackTint);
    this.black[i] = clamp01(b * keepBlack);
  };

  // ---- ColorField ------------------------------------------------------

  function ColorField(size) {
    this.size = size || N;
    var n = this.size * this.size;
    this.r = new Int32Array(n);
    this.g = new Int32Array(n);
    this.b = new Int32Array(n);
    this.a = new Int32Array(n);
    this.absolute = new Uint8Array(n);
  }

  ColorField.prototype.redAt = function (px, py) { return this.r[py * this.size + px]; };
  ColorField.prototype.greenAt = function (px, py) { return this.g[py * this.size + px]; };
  ColorField.prototype.blueAt = function (px, py) { return this.b[py * this.size + px]; };
  ColorField.prototype.opacityAt = function (px, py) { return this.a[py * this.size + px]; };

  ColorField.prototype.add = function (px, py, dr, dg, db) {
    var i = py * this.size + px;
    this.r[i] = this.r[i] + dr;
    this.g[i] = this.g[i] + dg;
    this.b[i] = this.b[i] + db;
  };

  ColorField.prototype.addOpacity = function (px, py, da) {
    var i = py * this.size + px;
    this.a[i] = this.a[i] + da;
  };

  ColorField.prototype.set = function (px, py, opacity, red, green, blue) {
    var i = py * this.size + px;
    this.a[i] = opacity;
    this.r[i] = red;
    this.g[i] = green;
    this.b[i] = blue;
    this.absolute[i] = 1;
  };

  ColorField.prototype.setArgb = function (px, py, argb) {
    this.set(px, py, (argb >>> 24) & 0xFF, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
  };

  ColorField.prototype.apply = function (delta) {
    for (var i = 0; i < this.r.length; i++) {
      if (delta.absolute[i]) {
        this.a[i] = delta.a[i];
        this.r[i] = delta.r[i];
        this.g[i] = delta.g[i];
        this.b[i] = delta.b[i];
        this.absolute[i] = 1;
      } else {
        this.r[i] += delta.r[i];
        this.g[i] += delta.g[i];
        this.b[i] += delta.b[i];
        this.a[i] += delta.a[i];
      }
    }
  };

  ColorField.prototype.argb = function (px, py) {
    var i = py * this.size + px;
    return ((cap(this.a[i]) << 24) | (cap(this.r[i]) << 16) | (cap(this.g[i]) << 8) | cap(this.b[i])) >>> 0;
  };

  /**
   * What a texel will actually LOOK like, per channel - colour over the white
   * template, before the template's own shading multiplies in. A transparent
   * texel reads white here, not black, which is what a magical gene painting
   * over a dominant-white horse has to reason about.
   */
  ColorField.prototype.visible = function (px, py, channel) {
    var i = py * this.size + px;
    var c = channel === 0 ? this.r[i] : channel === 1 ? this.g[i] : this.b[i];
    var capped = cap(c);
    var a = cap(this.a[i]) / 255;
    return Math.round(capped * a + 255 * (1 - a));
  };

  // ---- GradientLut -----------------------------------------------------

  function GradientLut(argb, width, height) {
    this.argb = argb;
    this.width = width;
    this.height = height;
  }

  GradientLut.prototype.sample = function (redLevel, blackLevel) {
    var r = clamp01(redLevel), b = clamp01(blackLevel);
    var fx = (1 - r) * (this.width - 1);
    var fy = b * (this.height - 1);
    var x0 = Math.floor(fx), y0 = Math.floor(fy);
    var x1 = Math.min(x0 + 1, this.width - 1), y1 = Math.min(y0 + 1, this.height - 1);
    var tx = fx - x0, ty = fy - y0;
    var c00 = this.argb[y0 * this.width + x0], c10 = this.argb[y0 * this.width + x1];
    var c01 = this.argb[y1 * this.width + x0], c11 = this.argb[y1 * this.width + x1];
    function ch(c, s) { return (c >> s) & 0xFF; }
    function bilerp(a, b2, c, d) {
      var top = a + (b2 - a) * tx, bot = c + (d - c) * tx;
      var v = Math.round(top + (bot - top) * ty);
      return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
    var rr = bilerp(ch(c00, 16), ch(c10, 16), ch(c01, 16), ch(c11, 16));
    var gg = bilerp(ch(c00, 8), ch(c10, 8), ch(c01, 8), ch(c11, 8));
    var bb = bilerp(ch(c00, 0), ch(c10, 0), ch(c01, 0), ch(c11, 0));
    return (0xFF000000 | (rr << 16) | (gg << 8) | bb) >>> 0;
  };

  // ---- the composer ----------------------------------------------------

  var PURE_BLACK_ALPHA = 0xCC;
  var TRANSPARENT_EPS = 0.001;

  var EYE_RECTS = {
    ADULT: [[6, 42, 4, 2], [28, 42, 4, 2]],
    BABY: [[6, 20, 2, 2], [40, 20, 2, 2]]
  };

  function blend(templateCh, overlayCh, a) {
    var factor = (overlayCh / 255) * a + (1 - a);
    var v = Math.round(templateCh * factor);
    return v < 0 ? 0 : (v > 255 ? 255 : v);
  }

  /**
   * The three phases, then the composite. `naturals` and `magicals` are arrays
   * of functions; each returns its contribution or null, exactly as a Java gene
   * returns a PigmentField / ColorField or null.
   */
  function compose(opts) {
    var skin = opts.skin;
    var template = opts.template;
    var lut = opts.lut;

    var pigment = new PigmentField(N);
    (opts.naturals || []).forEach(function (fn) {
      var next = fn(pigment);
      if (next) pigment = next;
    });

    var colour = new ColorField(N);
    var resolved = pigment;
    HG.geometry.forEachTexel(skin, function (px, py) {
      var r = resolved.redAt(px, py);
      var b = resolved.blackAt(px, py);
      if (r <= TRANSPARENT_EPS && b <= TRANSPARENT_EPS) return;
      var rgb = lut.sample(r, b) & 0xFFFFFF;
      colour.setArgb(px, py, ((rgb === 0 ? PURE_BLACK_ALPHA << 24 : 0xFF000000) | rgb) >>> 0);
    });

    (opts.magicals || []).forEach(function (fn) {
      var delta = fn(pigment, colour);
      if (delta) colour.apply(delta);
    });

    var out = new Uint32Array(N * N);
    for (var i = 0; i < out.length; i++) {
      var t = template[i];
      var ta = (t >>> 24) & 0xFF;
      if (ta === 0) { out[i] = 0; continue; }
      var o = colour.argb(i % N, (i / N) | 0);
      var oa = ((o >>> 24) & 0xFF) / 255;
      out[i] = ((ta << 24)
        | (blend((t >> 16) & 0xFF, (o >> 16) & 0xFF, oa) << 16)
        | (blend((t >> 8) & 0xFF, (o >> 8) & 0xFF, oa) << 8)
        | blend(t & 0xFF, o & 0xFF, oa)) >>> 0;
    }

    (EYE_RECTS[skin === "BABY" ? "BABY" : "ADULT"]).forEach(function (r) {
      for (var y = r[1]; y < r[1] + r[3]; y++) {
        for (var x = r[0]; x < r[0] + r[2]; x++) {
          if (x >= 0 && y >= 0 && x < N && y < N) out[y * N + x] = template[y * N + x];
        }
      }
    });

    return { pixels: out, pigment: pigment, colour: colour };
  }

  HG.fields = {
    PigmentField: PigmentField,
    ColorField: ColorField,
    GradientLut: GradientLut,
    compose: compose,
    TRANSPARENT_EPS: TRANSPARENT_EPS
  };
})(window.HG);
