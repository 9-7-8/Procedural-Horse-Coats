// Builds the preview horse in three.js straight out of the geometry tables in
// js/geometry.js.
//
// The old creator loaded a horse.glb. This does not, for three reasons: the GLB
// had its own UV layout and so could disagree with the game about which texel
// shades which bit of horse; loading it needed a web server (a file:// fetch is
// blocked); and a box mesh built from HorseSkinGeometry's own numbers is, by
// construction, the model the game textures. Clicking it gives a real body-space
// point, not an approximation.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var geo = HG.geometry;
  var N = geo.SHEET_SIZE;

  // face -> which bounds corner each of the four vertices takes, walked so the
  // winding faces outward.
  var FACE_CORNERS = {
    NOSE: [[1, 0, 0], [1, 0, 1], [1, 1, 1], [1, 1, 0]],
    TAIL: [[0, 0, 1], [0, 0, 0], [0, 1, 0], [0, 1, 1]],
    TOP: [[0, 1, 0], [1, 1, 0], [1, 1, 1], [0, 1, 1]],
    BOTTOM: [[0, 0, 1], [1, 0, 1], [1, 0, 0], [0, 0, 0]],
    RIGHT: [[0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1]],
    LEFT: [[1, 0, 0], [0, 0, 0], [0, 1, 0], [1, 1, 0]]
  };

  function build(THREE, skin) {
    var mesh = geo.mesh(skin);
    var positions = [], uvs = [], normals = [], indices = [];
    var vertex = 0;

    mesh.partNames.forEach(function (partName) {
      var pd = mesh.parts[partName];
      var b = pd.bounds;
      var lo = { X: b.xMin, Y: b.yMin, Z: b.zMin };
      var hi = { X: b.xMax, Y: b.yMax, Z: b.zMax };

      Object.keys(FACE_CORNERS).forEach(function (face) {
        var fm = pd.faces[face];
        var A = geo.spanA(face), B = geo.spanB(face);
        var normal = { X: 0, Y: 0, Z: 0 };
        normal[geo.FACES[face].normal] = geo.FACES[face].atMax ? 1 : -1;

        FACE_CORNERS[face].forEach(function (c) {
          var p = {
            X: c[0] ? hi.X : lo.X,
            Y: c[1] ? hi.Y : lo.Y,
            Z: c[2] ? hi.Z : lo.Z
          };
          positions.push(p.X, p.Y, p.Z);
          normals.push(normal.X, normal.Y, normal.Z);

          // Where this corner sits along the face's two spanning axes, then
          // through the same face map the texel grid uses.
          var fa = span(lo, hi, A) === 0 ? 0 : (p[A] - lo[A]) / span(lo, hi, A);
          var fb = span(lo, hi, B) === 0 ? 0 : (p[B] - lo[B]) / span(lo, hi, B);
          var u = lerp(fm.u0, fm.u1, fm.uUsesA ? fa : fb);
          var v = lerp(fm.v0, fm.v1, fm.vUsesA ? fa : fb);
          uvs.push(u / N, v / N);
        });

        indices.push(vertex, vertex + 1, vertex + 2, vertex, vertex + 2, vertex + 3);
        vertex += 4;
      });
    });

    var g = new THREE.BufferGeometry();
    g.setAttribute("position", new THREE.Float32BufferAttribute(positions, 3));
    g.setAttribute("uv", new THREE.Float32BufferAttribute(uvs, 2));
    g.setAttribute("normal", new THREE.Float32BufferAttribute(normals, 3));
    g.setIndex(indices);

    // Centre on the body so orbiting feels right, and scale model units
    // (1 = 1/16 block) down to something camera-friendly.
    var bb = mesh.bodyBounds;
    g.translate(-(bb.xMin + bb.xMax) / 2, -(bb.yMin + bb.yMax) / 2, -(bb.zMin + bb.zMax) / 2);
    g.scale(0.1, 0.1, 0.1);
    return g;
  }

  function span(lo, hi, axis) { return hi[axis] - lo[axis]; }
  function lerp(a, b, t) { return a + (b - a) * t; }

  HG.model3d = { build: build };
})(window.HG);
