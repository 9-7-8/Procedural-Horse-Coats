// Builds the preview horse in three.js straight out of the geometry tables in
// js/geometry.js.
//
// The old creator loaded a horse.glb. This does not, for three reasons: the GLB
// had its own UV layout and so could disagree with the game about which texel
// shades which bit of horse; loading it needed a web server (a file:// fetch is
// blocked); and a box mesh built from HorseSkinGeometry's own numbers is, by
// construction, the model the game textures. Clicking it gives a real body-space
// point, not an approximation.
//
// WHAT IT DRAWS, AND WHY IT IS NOT THE AABB: HorseSkinGeometry stores each part
// as its *rest-pose axis-aligned bounding box*, because that is all the coat
// pipeline needs - it projects texels onto a box and never asks what shape the
// horse is. For a part with a pitch that box is much bigger than the part: the
// adult neck is a 4x12x7 cuboid tilted 30 degrees, whose AABB is 4x13.9x12.1 -
// nearly twice as deep. Drawing AABBs gave a pile of oversized blocks rather
// than a horse. So this walks the *raw* cuboid (origin + size, rotated about the
// pivot) exactly as HdHorseModel/vanilla poses it, and only then flips into body
// space. Nothing about the pipeline changes; the preview just stops lying about
// the silhouette.
//
// The legs are deliberately ONE 4x11x4 box each, not the classic upper/shin/hoof
// trio: that trio is the pre-1.13 ModelHorse, while vanilla 26.1.2 - and so
// HdHorseModel, which copies its texOffs and box numbers verbatim - uses a
// single box per leg. Following the model doc's classic numbers here would draw
// a horse the game does not render.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var geo = HG.geometry;
  var N = geo.SHEET_SIZE;

  // How long the horse comes out in scene units, nose to tail. The viewport
  // frames the camera off what build() reports rather than off model units.
  var TARGET_LENGTH = 3.0;

  // Body-space face -> the four corners that make it, each corner written as a
  // fraction along (bodyX, bodyY, bodyZ), walked so the winding faces outward.
  var FACE_CORNERS = {
    NOSE: [[1, 0, 0], [1, 0, 1], [1, 1, 1], [1, 1, 0]],
    TAIL: [[0, 0, 1], [0, 0, 0], [0, 1, 0], [0, 1, 1]],
    TOP: [[0, 1, 0], [1, 1, 0], [1, 1, 1], [0, 1, 1]],
    BOTTOM: [[0, 0, 1], [1, 0, 1], [1, 0, 0], [0, 0, 0]],
    RIGHT: [[0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1]],
    LEFT: [[1, 0, 0], [0, 0, 0], [0, 1, 0], [1, 1, 0]]
  };

  // The body <-> local flip, per axis: bodyX = modelMax.z - mz (so it runs
  // against local z), bodyY = modelMax.y - my (against local y), bodyZ = -mx
  // (against local x). All three reverse, which is why every fraction below is
  // taken as 1 - c. Two axes swap and all three negate, so the map is
  // orientation-preserving and the winding above still faces outward.
  function localFractions(c) {
    return { x: 1 - c[2], y: 1 - c[1], z: 1 - c[0] };
  }

  function build(THREE, skin) {
    var mesh = geo.mesh(skin);
    var positions = [], uvs = [], normals = [], indices = [];
    var vertex = 0;

    mesh.partNames.forEach(function (partName) {
      var pd = mesh.parts[partName];
      var r = pd.raw;
      var cos = Math.cos(r.pitch), sin = Math.sin(r.pitch);

      Object.keys(FACE_CORNERS).forEach(function (face) {
        var fm = pd.faces[face];
        var A = geo.spanA(face), B = geo.spanB(face);
        var quad = [];

        FACE_CORNERS[face].forEach(function (c) {
          var f = localFractions(c);
          // The cuboid as the model declares it: origin + size, about the pivot.
          var lx = r.ox + f.x * r.w;
          var ly = r.oy + f.y * r.h;
          var lz = r.oz + f.z * r.d;
          // Pitch rotates y/z about the pivot's model-x axis.
          var p = geo.toBody(skin,
            r.px + lx,
            r.py + (ly * cos - lz * sin),
            r.pz + (ly * sin + lz * cos));
          quad.push(p);
          positions.push(p.x, p.y, p.z);

          // The texel this corner takes is a property of the box, not of where
          // the box ended up, so it reads off the same body-axis fractions the
          // pipeline's own face map is written in - rotation carries the
          // texture with the part, exactly as the game's renderer does.
          var along = { X: c[0], Y: c[1], Z: c[2] };
          var fa = along[A], fb = along[B];
          var u = lerp(fm.u0, fm.u1, fm.uUsesA ? fa : fb);
          var v = lerp(fm.v0, fm.v1, fm.vUsesA ? fa : fb);
          uvs.push(u / N, v / N);
        });

        // Take the normal from the face we just built rather than from the
        // body axis: a pitched part's "top" is tilted, and a lie here reads as
        // a mis-lit neck.
        var n = faceNormal(quad);
        for (var i = 0; i < 4; i++) normals.push(n.x, n.y, n.z);

        indices.push(vertex, vertex + 1, vertex + 2, vertex, vertex + 2, vertex + 3);
        vertex += 4;
      });
    });

    var g = new THREE.BufferGeometry();
    g.setAttribute("position", new THREE.Float32BufferAttribute(positions, 3));
    g.setAttribute("uv", new THREE.Float32BufferAttribute(uvs, 2));
    g.setAttribute("normal", new THREE.Float32BufferAttribute(normals, 3));
    g.setIndex(indices);

    // Centre the horse over the origin left-to-right and nose-to-tail, stand it
    // on y = 0 so the grid reads as ground, and scale model units (1 = 1/16
    // block) to something the camera can frame.
    var bb = mesh.bodyBounds;
    var scale = TARGET_LENGTH / Math.max(1e-6, bb.xMax - bb.xMin);
    g.translate(-(bb.xMin + bb.xMax) / 2, -bb.yMin, -(bb.zMin + bb.zMax) / 2);
    g.scale(scale, scale, scale);

    return {
      geometry: g,
      length: (bb.xMax - bb.xMin) * scale,
      height: (bb.yMax - bb.yMin) * scale,
      width: (bb.zMax - bb.zMin) * scale
    };
  }

  function faceNormal(q) {
    var ax = q[1].x - q[0].x, ay = q[1].y - q[0].y, az = q[1].z - q[0].z;
    var bx = q[2].x - q[0].x, by = q[2].y - q[0].y, bz = q[2].z - q[0].z;
    var nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
    var len = Math.hypot(nx, ny, nz) || 1;
    return { x: nx / len, y: ny / len, z: nz / len };
  }

  function lerp(a, b, t) { return a + (b - a) * t; }

  HG.model3d = { build: build };
})(window.HG);
