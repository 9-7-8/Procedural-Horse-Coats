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

  /**
   * Mix white hair in by `amount`. Mirrors PigmentField.whiten - and the reason
   * it is not `red *= keep; black *= keep` is that a black horse stores a full
   * load of red that only the black is masking, so scaling both together
   * unmasks it and the soft edge of every marking goes tan. What is held
   * constant is the VISIBLE red, red * (1 - black); on a black horse that is 0,
   * so the fade runs down the gradient's neutral red = 0 column and the edge
   * greys out. On a chestnut (black = 0) it collapses to red * keep.
   */
  PigmentField.prototype.whiten = function (px, py, amount) {
    if (amount <= 0) return;
    var i = py * this.size + px;
    var keep = 1 - clamp01(amount);
    var b = this.black[i];
    var visibleRed = this.red[i] * (1 - b);
    var newBlack = b * keep;
    var room = 1 - newBlack;
    this.red[i] = room <= 1e-4 ? 0 : clamp01(visibleRed * keep / room);
    this.black[i] = clamp01(newBlack);
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
    // Texels an apply()d delta has written - what liftShadows is allowed to touch.
    this.painted = new Uint8Array(n);
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
        this.painted[i] = 1;
      } else {
        if ((delta.r[i] | delta.g[i] | delta.b[i] | delta.a[i]) === 0) continue;
        this.r[i] += delta.r[i];
        this.g[i] += delta.g[i];
        this.b[i] += delta.b[i];
        this.a[i] += delta.a[i];
        this.painted[i] = 1;
      }
    }
  };

  /**
   * The shadow pass - mirrors ColorField.liftShadows. Raise every texel a
   * magical gene painted at or below `floor` in all three channels so its
   * brightest channel lands exactly on `floor`, keeping its hue. The composite
   * below is a multiply, so a texel painted pure black scales the template to
   * nothing and takes its hair shading with it.
   *
   * Only texels apply() wrote are considered: phase 2 has its own answer to the
   * same problem (PURE_BLACK_ALPHA) and lifting a texel with both puts a black
   * mane above the brightness of the dark bay body under it.
   */
  ColorField.prototype.liftShadows = function (floor) {
    function cap(v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
    function scale(v, max) { return ((v * floor + ((max / 2) | 0)) / max) | 0; }
    for (var i = 0; i < this.r.length; i++) {
      if (!this.painted[i] || this.a[i] <= 0) continue;
      var rr = cap(this.r[i]), gg = cap(this.g[i]), bb = cap(this.b[i]);
      var max = Math.max(rr, Math.max(gg, bb));
      if (max >= floor) continue;
      if (max <= 0) {
        this.r[i] = this.g[i] = this.b[i] = floor;
      } else {
        this.r[i] = scale(rr, max);
        this.g[i] = scale(gg, max);
        this.b[i] = scale(bb, max);
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
  // The reading at which phase 2 stops softening the alpha. Mirrors
  // CoatTextureComposer.NEAR_BLACK - see nearBlackAlpha below for why this is a
  // ramp and not an equality test.
  var NEAR_BLACK = 0x30;
  var TRANSPARENT_EPS = 0.001;
  var SHADOW_FLOOR = 0x15;

  var EYE_RECTS = {
    ADULT: [[6, 42, 4, 2], [30, 42, 4, 2]],
    BABY: [[6, 20, 2, 2], [40, 20, 2, 2]]
  };

  function blend(templateCh, overlayCh, a) {
    var factor = (overlayCh / 255) * a + (1 - a);
    var v = Math.round(templateCh * factor);
    return v < 0 ? 0 : (v > 255 ? 255 : v);
  }

  /**
   * The opacity a resolved colour composites at: PURE_BLACK_ALPHA at black,
   * ramping to fully opaque by NEAR_BLACK. The port of
   * CoatTextureComposer.nearBlackAlpha.
   *
   * <p>This used to be `rgb === 0 ? PURE_BLACK_ALPHA : 0xFF` - an exact
   * equality against pure black, which is what the GAME used to have too. The
   * game was changed to a ramp when a LUT edit moved the gradient's black
   * corner off exactly #000000 and switched the softening off for every bay;
   * the mirror here was not, so the equality had been DEAD CODE ever since.
   * Black bakes to about #161515 on the shipped chart, so the branch never
   * fired and the creator drew every dark coat at full opacity - darker than
   * the game, on every preview. See known-gaps gap 50.
   */
  function nearBlackAlpha(rgb) {
    var max = Math.max((rgb >> 16) & 0xFF, Math.max((rgb >> 8) & 0xFF, rgb & 0xFF));
    if (max >= NEAR_BLACK) return 0xFF;
    return 0xFF - ((0xFF - PURE_BLACK_ALPHA) * (NEAR_BLACK - max) / NEAR_BLACK | 0);
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
      colour.setArgb(px, py, ((nearBlackAlpha(rgb) << 24) | rgb) >>> 0);
    });

    (opts.magicals || []).forEach(function (fn) {
      var delta = fn(pigment, colour);
      if (delta) colour.apply(delta);
    });

    // the shadow pass - nothing phase 3 painted leaves it true black
    colour.liftShadows(SHADOW_FLOOR);

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
    TRANSPARENT_EPS: TRANSPARENT_EPS,
    SHADOW_FLOOR: SHADOW_FLOOR,

    // Exported for js/parity.js only - see the composer section there. Nothing
    // in the creator calls these two directly; compose() uses them internally.
    nearBlackAlpha: nearBlackAlpha,

    /** One opaque texel through the shadow pass, mirroring the Java fixture helper. */
    liftedForParity: function (rgb) {
      var f = new ColorField(1);
      var d = new ColorField(1);
      d.add(0, 0, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
      d.addOpacity(0, 0, 0xFF);
      f.apply(d);
      f.liftShadows(SHADOW_FLOOR);
      var hex = (f.argb(0, 0) & 0xFFFFFF).toString(16);
      return "000000".substring(hex.length) + hex;
    }
  };
})(window.HG);
