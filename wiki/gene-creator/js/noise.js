// A JavaScript port of common/coat/pattern/BodyNoise.java and BodyStripes.java.
//
// The hash these fields are built on is 64-bit integer arithmetic, and
// JavaScript numbers are doubles - so the low bits of a 64-bit multiply are
// simply not there. Rather than approximate (which would give the creator a
// different dapple field from the game's, on the same seed), this file carries
// a small two-word u64 so the multiplications are exact. BigInt would also be
// exact and is roughly twenty times too slow for a field sampled at every texel.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // ---- 64-bit integers as { h, l }, both unsigned 32-bit ----------------

  function u64(h, l) { return { h: h >>> 0, l: l >>> 0 }; }

  /** A JS number (or a 32-bit int) as a sign-extended 64-bit value. */
  function fromInt(i) {
    i = i | 0;
    return u64(i < 0 ? 0xFFFFFFFF : 0, i);
  }

  function fromHex(hex) {
    var s = hex.replace(/^0x/i, "").padStart(16, "0");
    return u64(parseInt(s.slice(0, 8), 16), parseInt(s.slice(8), 16));
  }

  function xor(a, b) { return u64(a.h ^ b.h, a.l ^ b.l); }

  function add(a, b) {
    var l = (a.l >>> 0) + (b.l >>> 0);
    return u64((a.h + b.h + (l > 0xFFFFFFFF ? 1 : 0)) >>> 0, l >>> 0);
  }

  function mul(a, b) {
    var a0 = a.l & 0xFFFF, a1 = a.l >>> 16, a2 = a.h & 0xFFFF, a3 = a.h >>> 16;
    var b0 = b.l & 0xFFFF, b1 = b.l >>> 16, b2 = b.h & 0xFFFF, b3 = b.h >>> 16;
    var c0 = a0 * b0;
    var c1 = Math.floor(c0 / 65536) + a0 * b1 + a1 * b0;
    var c2 = Math.floor(c1 / 65536) + a0 * b2 + a1 * b1 + a2 * b0;
    var c3 = Math.floor(c2 / 65536) + a0 * b3 + a1 * b2 + a2 * b1 + a3 * b0;
    return u64((c3 & 0xFFFF) * 65536 + (c2 & 0xFFFF), (c1 & 0xFFFF) * 65536 + (c0 & 0xFFFF));
  }

  /** Logical right shift, 0 < n < 32. */
  function shru(a, n) {
    return u64(a.h >>> n, ((a.l >>> n) | (a.h << (32 - n))) >>> 0);
  }

  // Java's Random-free hash constants, verbatim from BodyNoise.
  var K1 = fromHex("9E3779B97F4A7C15");
  var K2 = fromHex("BF58476D1CE4E5B9");
  var K3 = fromHex("C2B2AE3D27D4EB4F");
  var K4 = fromHex("94D049BB133111EB");
  var K5 = fromHex("165667B19E3779F9");
  var K6 = fromHex("D6E8FEB86659FD93");
  var K7 = fromHex("27D4EB2F165667C5");

  var TWO_POW_32 = 4294967296;
  var TWO_POW_53 = 9007199254740992;

  // Lattice hashes repeat hard between neighbouring texels, so caching them is
  // the difference between a preview that updates as you drag a slider and one
  // that doesn't. Keyed per seed; cleared when it grows past a sane size.
  var caches = new Map();

  function cacheFor(seed) {
    var key = seed.h + ":" + seed.l;
    var c = caches.get(key);
    if (!c) {
      if (caches.size > 24) caches.clear();
      c = new Map();
      caches.set(key, c);
    }
    return c;
  }

  function hash01(seedCache, seed, x, y, z, salt) {
    // Lattice coordinates stay small (a horse is ~22 units long), so this packs
    // cleanly into one number; anything out of range just misses the cache.
    var key = (((x + 1024) * 2048 + (y + 1024)) * 2048 + (z + 1024)) * 8 + salt;
    var hit = seedCache.get(key);
    if (hit !== undefined) return hit;

    var h = seed;
    h = mul(xor(h, mul(fromInt(x), K1)), K2);
    h = mul(xor(h, mul(fromInt(y), K3)), K4);
    h = mul(xor(h, mul(fromInt(z), K5)), K6);
    h = mul(xor(h, mul(fromInt(salt), K7)), K1);
    h = xor(h, shru(h, 31));
    var s = shru(h, 11);
    var v = (s.h * TWO_POW_32 + s.l) / TWO_POW_53;
    seedCache.set(key, v);
    return v;
  }

  function floor(v) { var i = v | 0; return v < i ? i - 1 : i; }
  function smooth(t) { return t * t * (3.0 - 2.0 * t); }
  function lerp(a, b, t) { return a + (b - a) * t; }

  /**
   * Distance to the nearest point of a jittered unit lattice, normalised to
   * roughly [0, 1]: near 0 at a lattice point, near 1 in the gaps. Dapples.
   */
  function cellDistance(seed, x, y, z) {
    var cache = cacheFor(seed);
    var cx = floor(x), cy = floor(y), cz = floor(z);
    var best = Infinity;
    for (var dx = -1; dx <= 1; dx++) {
      for (var dy = -1; dy <= 1; dy++) {
        for (var dz = -1; dz <= 1; dz++) {
          var lx = cx + dx, ly = cy + dy, lz = cz + dz;
          var px = lx + hash01(cache, seed, lx, ly, lz, 1);
          var py = ly + hash01(cache, seed, lx, ly, lz, 2);
          var pz = lz + hash01(cache, seed, lx, ly, lz, 3);
          var d = (px - x) * (px - x) + (py - y) * (py - y) + (pz - z) * (pz - z);
          if (d < best) best = d;
        }
      }
    }
    var out = Math.sqrt(best) / 0.9;
    return out < 0 ? 0 : (out > 1 ? 1 : out);
  }

  /** Smooth value noise in [0, 1] on a unit lattice. */
  function value(seed, x, y, z) {
    var cache = cacheFor(seed);
    var x0 = floor(x), y0 = floor(y), z0 = floor(z);
    var fx = smooth(x - x0), fy = smooth(y - y0), fz = smooth(z - z0);
    var c00 = lerp(hash01(cache, seed, x0, y0, z0, 0), hash01(cache, seed, x0 + 1, y0, z0, 0), fx);
    var c10 = lerp(hash01(cache, seed, x0, y0 + 1, z0, 0), hash01(cache, seed, x0 + 1, y0 + 1, z0, 0), fx);
    var c01 = lerp(hash01(cache, seed, x0, y0, z0 + 1, 0), hash01(cache, seed, x0 + 1, y0, z0 + 1, 0), fx);
    var c11 = lerp(hash01(cache, seed, x0, y0 + 1, z0 + 1, 0), hash01(cache, seed, x0 + 1, y0 + 1, z0 + 1, 0), fx);
    return lerp(lerp(c00, c10, fy), lerp(c01, c11, fy), fz);
  }

  // ---- BodyStripes -----------------------------------------------------

  var WARP_SCALE = 1.0 / 9.0;
  var WIDTH_SCALE = 0.26;
  var EDGE = 0.10;
  var SLANT = 0.35;

  function smoothstep(edge0, edge1, v) {
    if (edge1 <= edge0) return v < edge0 ? 0 : 1;
    var t = (v - edge0) / (edge1 - edge0);
    t = t < 0 ? 0 : (t > 1 ? 1 : t);
    return t * t * (3 - 2 * t);
  }

  function stripeCoverage(seed, x, y, z, spacing, duty, warp) {
    var bend = (value(seed, x * WARP_SCALE, y * WARP_SCALE, z * WARP_SCALE) - 0.5) * 2.0 * warp;
    var phase = (x + bend + Math.abs(z) * SLANT) / spacing;
    var offset = phase - Math.floor(phase);
    var d = Math.abs(offset - 0.5) * 2.0;
    var width = duty * (0.75 + 0.5 * value(xor(seed, K1), x * WIDTH_SCALE, y * WIDTH_SCALE, z * WIDTH_SCALE));
    return 1.0 - smoothstep(width - EDGE, width + EDGE, d);
  }

  HG.noise = {
    u64: u64,
    fromInt: fromInt,
    fromHex: fromHex,
    xor: xor,
    add: add,
    mul: mul,
    K1: K1,
    K3: K3,
    cellDistance: cellDistance,
    value: value,
    stripeCoverage: stripeCoverage,
    smoothstep: smoothstep,
    clearCache: function () { caches.clear(); }
  };
})(window.HG);
