// A JavaScript port of common/coat/skin/HorseSkinGeometry.java.
//
// WHY A PORT AND NOT AN APPROXIMATION: everything the creator shows - which
// part you clicked, what body-space (x, y, z) a texel sits at, which texels a
// mask covers - has to be the same answer the game gives, or the tool is
// drawing a different horse from the one you will breed. The raw tables below
// are copied verbatim from the Java file; if that file's geometry changes, this
// one has to change with it.
//
// Body space: X 0 at the tail's rear edge -> +nose, Y 0 at the hoof bottoms ->
// +up, Z 0 on the centre plane, +Z to the horse's right. Model units, 1 unit =
// 1/16 block = 2 texels on the 128px sheet.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var TEXELS_PER_UNIT = 2;
  var SHEET_SIZE = 128;

  var PARTS = ["BODY", "NECK", "HEAD", "MUZZLE", "MANE", "TAIL",
    "LEFT_EAR", "RIGHT_EAR",
    "LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"];

  var LEGS = ["LEFT_FRONT_LEG", "RIGHT_FRONT_LEG", "LEFT_HIND_LEG", "RIGHT_HIND_LEG"];

  // Faces: NOSE/TAIL span (Z,Y); TOP/BOTTOM span (X,Z); RIGHT/LEFT span (X,Y).
  var FACES = {
    NOSE: { normal: "X", atMax: true }, TAIL: { normal: "X", atMax: false },
    TOP: { normal: "Y", atMax: true }, BOTTOM: { normal: "Y", atMax: false },
    RIGHT: { normal: "Z", atMax: true }, LEFT: { normal: "Z", atMax: false }
  };

  function spanA(face) { return FACES[face].normal === "X" ? "Z" : "X"; }
  function spanB(face) { return FACES[face].normal === "Y" ? "Z" : "Y"; }

  var ADULT_HEAD_PITCH = Math.PI / 6.0;
  var BABY_NECK_PITCH = 0.6109;

  // part, px,py,pz, ox,oy,oz, w,h,d, pitch, tu,tv
  var ADULT_RAW = [
    ["BODY", 0, 11, 5, -5, -8, -17, 10, 10, 22, 0, 0, 32],
    ["NECK", 0, 4, -12, -2.05, -6, -2, 4, 12, 7, ADULT_HEAD_PITCH, 0, 35],
    ["HEAD", 0, 4, -12, -3, -11, -2, 6, 5, 7, ADULT_HEAD_PITCH, 0, 13],
    ["MUZZLE", 0, 4, -12, -2, -11, -7, 4, 5, 5, ADULT_HEAD_PITCH, 0, 25],
    ["MANE", 0, 4, -12, -1, -11, 5.01, 2, 16, 2, ADULT_HEAD_PITCH, 56, 36],
    ["TAIL", 0, 6, 7, -1.5, 0, 0, 3, 14, 4, ADULT_HEAD_PITCH, 42, 36],
    ["LEFT_EAR", 0, 4, -12, 0.55, -13, 4, 2, 3, 1, ADULT_HEAD_PITCH, 19, 0],
    ["RIGHT_EAR", 0, 4, -12, -2.55, -13, 4, 2, 3, 1, ADULT_HEAD_PITCH, 19, 16],
    ["LEFT_HIND_LEG", 4, 14, 7, -3, -1.01, -1, 4, 11, 4, 0, 26, 0],
    ["RIGHT_HIND_LEG", -4, 14, 7, -1, -1.01, -1, 4, 11, 4, 0, 48, 21],
    ["LEFT_FRONT_LEG", 4, 14, -10, -3, -1.01, -1.9, 4, 11, 4, 0, 26, 16],
    ["RIGHT_FRONT_LEG", -4, 14, -10, -1, -1.01, -1.9, 4, 11, 4, 0, 48, 0]
  ];

  var BABY_RAW = [
    ["BODY", 0, 12.5, 0, -4, -3.5, -7, 8, 7, 14, 0, 0, 13],
    ["NECK", 0, 10, -6, -2, -6, -2, 4, 8, 4, BABY_NECK_PITCH, 30, 0],
    ["HEAD", 0, 5.212, -9.713, -3, -3.9484, -6.705, 6, 4, 9, BABY_NECK_PITCH, 0, 0],
    ["TAIL", 0, 11.5, 7, -1.5, -1.5, -1, 3, 3, 8, -0.7418, 24, 34],
    ["LEFT_EAR", 2, 0.616, -10.557, -1, -2.5, -0.8, 2, 3, 1, BABY_NECK_PITCH, 0, 4],
    ["RIGHT_EAR", -2, 0.788, -10.802, -1, -2.5, -0.5, 2, 3, 1, BABY_NECK_PITCH, 0, 0],
    ["LEFT_HIND_LEG", 2.4, 16, 5.4, -1.5, -1, -1.5, 3, 9, 3, 0, 12, 46],
    ["RIGHT_HIND_LEG", -2.4, 16, 5.4, -1.5, -1, -1.5, 3, 9, 3, 0, 0, 46],
    ["LEFT_FRONT_LEG", 2.4, 16, -5.4, -1.5, -1, -1.5, 3, 9, 3, 0, 12, 34],
    ["RIGHT_FRONT_LEG", -2.4, 16, -5.4, -1.5, -1, -1.5, 3, 9, 3, 0, 0, 34]
  ];

  function raw(r) {
    return {
      part: r[0], px: r[1], py: r[2], pz: r[3], ox: r[4], oy: r[5], oz: r[6],
      w: r[7], h: r[8], d: r[9], pitch: r[10], tu: r[11], tv: r[12]
    };
  }

  function modelAabbOf(r) {
    var out = [Infinity, -Infinity, Infinity, -Infinity, Infinity, -Infinity];
    var cos = Math.cos(r.pitch), sin = Math.sin(r.pitch);
    for (var cx = 0; cx < 2; cx++) {
      for (var cy = 0; cy < 2; cy++) {
        for (var cz = 0; cz < 2; cz++) {
          var lx = r.ox + (cx === 0 ? 0 : r.w);
          var ly = r.oy + (cy === 0 ? 0 : r.h);
          var lz = r.oz + (cz === 0 ? 0 : r.d);
          var mx = r.px + lx;
          var my = r.py + (ly * cos - lz * sin);
          var mz = r.pz + (ly * sin + lz * cos);
          out[0] = Math.min(out[0], mx); out[1] = Math.max(out[1], mx);
          out[2] = Math.min(out[2], my); out[3] = Math.max(out[3], my);
          out[4] = Math.min(out[4], mz); out[5] = Math.max(out[5], mz);
        }
      }
    }
    return out;
  }

  function faceMapsOf(r) {
    var k = TEXELS_PER_UNIT;
    var u0 = k * r.tu, u1 = k * (r.tu + r.d), u2 = k * (r.tu + r.d + r.w);
    var u2b = k * (r.tu + r.d + r.w + r.w);
    var u3 = k * (r.tu + r.d + r.w + r.d);
    var u4 = k * (r.tu + r.d + r.w + r.d + r.w);
    var v0 = k * r.tv, v1 = k * (r.tv + r.d), v2 = k * (r.tv + r.d + r.h);
    return {
      RIGHT: { u0: u0, u1: u1, uUsesA: true, v0: v2, v1: v1, vUsesA: false },
      LEFT: { u0: u3, u1: u2, uUsesA: true, v0: v2, v1: v1, vUsesA: false },
      NOSE: { u0: u2, u1: u1, uUsesA: true, v0: v2, v1: v1, vUsesA: false },
      TAIL: { u0: u3, u1: u4, uUsesA: true, v0: v2, v1: v1, vUsesA: false },
      TOP: { u0: u2b, u1: u2, uUsesA: false, v0: v0, v1: v1, vUsesA: true },
      BOTTOM: { u0: u2, u1: u1, uUsesA: false, v0: v0, v1: v1, vUsesA: true }
    };
  }

  function lerp(a, b, t) { return a + (b - a) * t; }
  function invLerp(a, b, v) { return a === b ? 0 : (v - a) / (b - a); }
  function within(a, b, v) { return v >= Math.min(a, b) && v <= Math.max(a, b); }

  function boundsOf(min, max) {
    return {
      xMin: min[0], xMax: max[0], yMin: min[1], yMax: max[1], zMin: min[2], zMax: max[2],
      min: function (axis) { return axis === "X" ? this.xMin : axis === "Y" ? this.yMin : this.zMin; },
      max: function (axis) { return axis === "X" ? this.xMax : axis === "Y" ? this.yMax : this.zMax; },
      span: function (axis) { return this.max(axis) - this.min(axis); }
    };
  }

  function buildMesh(rawRows) {
    var rows = rawRows.map(raw);
    var aabb = {};
    var myMaxAll = -Infinity, mzMaxAll = -Infinity;
    rows.forEach(function (r) {
      var a = modelAabbOf(r);
      aabb[r.part] = a;
      myMaxAll = Math.max(myMaxAll, a[3]);
      mzMaxAll = Math.max(mzMaxAll, a[5]);
    });

    var parts = {};
    var lo = [Infinity, Infinity, Infinity], hi = [-Infinity, -Infinity, -Infinity];
    rows.forEach(function (r) {
      var m = aabb[r.part];
      var b = boundsOf([mzMaxAll - m[5], myMaxAll - m[3], -m[1]],
        [mzMaxAll - m[4], myMaxAll - m[2], -m[0]]);
      parts[r.part] = { bounds: b, faces: faceMapsOf(r), raw: r };
      lo = [Math.min(lo[0], b.xMin), Math.min(lo[1], b.yMin), Math.min(lo[2], b.zMin)];
      hi = [Math.max(hi[0], b.xMax), Math.max(hi[1], b.yMax), Math.max(hi[2], b.zMax)];
    });

    var mesh = {
      parts: parts,
      bodyBounds: boundsOf(lo, hi),
      partNames: rows.map(function (r) { return r.part; }),
      grid: null
    };
    mesh.grid = buildGrid(mesh);
    return mesh;
  }

  function pointOf(face, a, b, plane) {
    var p = { x: 0, y: 0, z: 0 };
    var A = spanA(face), B = spanB(face), N = FACES[face].normal;
    p[A.toLowerCase()] = a;
    p[B.toLowerCase()] = b;
    p[N.toLowerCase()] = plane;
    return p;
  }

  function buildGrid(mesh) {
    var grid = new Array(SHEET_SIZE * SHEET_SIZE).fill(null);
    for (var py = 0; py < SHEET_SIZE; py++) {
      for (var px = 0; px < SHEET_SIZE; px++) {
        grid[py * SHEET_SIZE + px] = sampleUncached(mesh, px, py);
      }
    }
    return grid;
  }

  function sampleUncached(mesh, px, py) {
    var cx = px + 0.5, cy = py + 0.5;
    for (var i = 0; i < mesh.partNames.length; i++) {
      var name = mesh.partNames[i];
      var pd = mesh.parts[name];
      for (var face in pd.faces) {
        var fm = pd.faces[face];
        if (!within(fm.u0, fm.u1, cx) || !within(fm.v0, fm.v1, cy)) continue;
        var fu = invLerp(fm.u0, fm.u1, cx);
        var fv = invLerp(fm.v0, fm.v1, cy);
        var fa = fm.uUsesA ? fu : fv;
        var fb = fm.uUsesA ? fv : fu;
        var bd = pd.bounds;
        var A = spanA(face), B = spanB(face), N = FACES[face].normal;
        var a = lerp(bd.min(A), bd.max(A), fa);
        var b = lerp(bd.min(B), bd.max(B), fb);
        var plane = FACES[face].atMax ? bd.max(N) : bd.min(N);
        return { part: name, face: face, point: pointOf(face, a, b, plane) };
      }
    }
    return null;
  }

  var MESHES = { ADULT: buildMesh(ADULT_RAW), BABY: buildMesh(BABY_RAW) };

  HG.geometry = {
    SHEET_SIZE: SHEET_SIZE,
    TEXELS_PER_UNIT: TEXELS_PER_UNIT,
    PARTS: PARTS,
    LEGS: LEGS,
    FACES: FACES,
    spanA: spanA,
    spanB: spanB,

    mesh: function (skin) { return MESHES[skin === "BABY" ? "BABY" : "ADULT"]; },

    hasPart: function (skin, part) { return !!this.mesh(skin).parts[part]; },

    bounds: function (skin, part) {
      var pd = this.mesh(skin).parts[part];
      return pd ? pd.bounds : null;
    },

    bodyBounds: function (skin) { return this.mesh(skin).bodyBounds; },

    /** The sample at a texel, or null where this skin maps nothing. */
    sample: function (skin, px, py) {
      if (px < 0 || py < 0 || px >= SHEET_SIZE || py >= SHEET_SIZE) return null;
      return this.mesh(skin).grid[py * SHEET_SIZE + px];
    },

    /** Visit every mapped texel: fn(px, py, part, face, point). */
    forEachTexel: function (skin, fn) {
      var grid = this.mesh(skin).grid;
      for (var py = 0; py < SHEET_SIZE; py++) {
        for (var px = 0; px < SHEET_SIZE; px++) {
          var s = grid[py * SHEET_SIZE + px];
          if (s) fn(px, py, s.part, s.face, s.point);
        }
      }
    },

    legIndex: function (part) { return LEGS.indexOf(part); }
  };
})(window.HG);
