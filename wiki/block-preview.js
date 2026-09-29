// Spin a block on a wiki page.
//
// Markup, anywhere in an article:
//
//   <div class="block-preview" data-blocks="cowboy_hitch,leatherworkers_post"
//        data-labels="Cowboy Hitch,Leatherworker's Post"></div>
//
// Every id is baked by `wiki/tools/bake-block-previews.mjs` into
// assets/block-preview/shapes.js plus the PNGs it names, and the page loads that
// script beside this one. That tool refuses any block dressed in vanilla art, so
// anything reaching this file is the mod's own.
//
// One scene, not one per block: several WebGL contexts on a page is a good way
// to hit a browser's limit, and the point of a row of posts is comparing them,
// which is easier when one drag turns all of them together.
//
// The camera is hand-rolled yaw/pitch/distance rather than OrbitControls, the
// same way `horse-designer/js/scene.js` does it and for the same reason - it is
// twenty lines, and it avoids a second CDN file that can fail independently of
// the first.
(function () {
  "use strict";

  var THREE_SRC = "https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js";
  var GAP = 1.35;          // blocks between block centres
  var FACES = ["east", "west", "up", "down", "south", "north"];  // THREE's BoxGeometry order

  function load(cb) {
    if (window.THREE) { cb(); return; }
    var existing = document.querySelector('script[data-block-preview-three]');
    if (existing) { existing.addEventListener("load", cb); return; }
    var s = document.createElement("script");
    s.src = THREE_SRC;
    s.setAttribute("data-block-preview-three", "");
    s.addEventListener("load", cb);
    s.addEventListener("error", function () { fail(document.querySelectorAll(".block-preview")); });
    document.head.appendChild(s);
  }

  function fail(nodes) {
    Array.prototype.forEach.call(nodes, function (el) {
      if (el.querySelector(".block-preview-note")) return;
      var p = document.createElement("p");
      p.className = "block-preview-note";
      p.textContent = "This preview needs WebGL, which this browser did not give us. "
        + "The block art itself is in the repository under textures/block/.";
      el.appendChild(p);
    });
  }

  function clamp(v, lo, hi) { return v < lo ? lo : v > hi ? hi : v; }

  function texture(name, done) {
    var loader = new window.THREE.TextureLoader();
    return loader.load("assets/block-preview/" + name + ".png", function (t) {
      // Sixteen-square art: never smooth it, and never let a mip average two
      // faces of a barrel together at a distance.
      t.magFilter = window.THREE.NearestFilter;
      t.minFilter = window.THREE.NearestFilter;
      t.generateMipmaps = false;
      if (done) done();
    });
  }

  function build(el) {
    var THREE = window.THREE;
    var ids = (el.getAttribute("data-blocks") || "").split(",").map(function (s) { return s.trim(); })
      .filter(Boolean);
    if (!ids.length) return;
    var labels = (el.getAttribute("data-labels") || "").split(",").map(function (s) { return s.trim(); });

    var canvas = document.createElement("canvas");
    canvas.className = "block-preview-canvas";
    canvas.setAttribute("tabindex", "0");
    canvas.setAttribute("role", "img");
    canvas.setAttribute("aria-label",
      (labels.filter(Boolean).join(", ") || ids.join(", ")) + " - a rotatable 3D preview");
    el.insertBefore(canvas, el.firstChild);

    var renderer;
    try {
      renderer = new THREE.WebGLRenderer({ canvas: canvas, antialias: true, alpha: true });
    } catch (e) { fail([el]); return; }
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));

    var scene = new THREE.Scene();
    var camera = new THREE.PerspectiveCamera(38, 1, 0.1, 100);

    // Flat-ish lighting. A block's own shading is painted into its texture, so
    // strong lights here would fight the art rather than help it read.
    scene.add(new THREE.AmbientLight(0xffffff, 0.82));
    var key = new THREE.DirectionalLight(0xffffff, 0.45);
    key.position.set(0.6, 1, 0.45);
    scene.add(key);

    var group = new THREE.Group();
    scene.add(group);

    var shapes = window.HG_BLOCK_SHAPES || {};
    var span = (ids.length - 1) * GAP;
    ids.forEach(function (id, i) {
      var faces = shapes[id];
      // A block the bake never saw. Leave a hole rather than take the row down -
      // the other four still answer the question the picture is there to answer.
      if (!faces) return;
      var mats = FACES.map(function (f) {
        return new THREE.MeshLambertMaterial({ map: texture(faces[f]) });
      });
      var cube = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), mats);
      cube.position.x = -span / 2 + i * GAP;
      group.add(cube);
    });

    // Framed so the row fills the canvas: the distance has to grow with the row,
    // but slower than the row does, or five blocks sit in the middle of a lot of
    // empty sky.
    var cam = { yaw: Math.PI * 0.22, pitch: 0.42, dist: 1.9 + span * 0.62 };
    var MIN_PITCH = -1.35, MAX_PITCH = 1.35;
    var MIN_DIST = 1.4, MAX_DIST = 6 + span * 2;

    var dragging = false, lastX = 0, lastY = 0;
    canvas.addEventListener("pointerdown", function (e) {
      dragging = true; lastX = e.clientX; lastY = e.clientY;
      canvas.setPointerCapture(e.pointerId);
    });
    canvas.addEventListener("pointermove", function (e) {
      if (!dragging) return;
      cam.yaw -= (e.clientX - lastX) * 0.008;
      cam.pitch = clamp(cam.pitch + (e.clientY - lastY) * 0.006, MIN_PITCH, MAX_PITCH);
      lastX = e.clientX; lastY = e.clientY;
    });
    function end(e) {
      if (!dragging) return;
      dragging = false;
      try { canvas.releasePointerCapture(e.pointerId); } catch (ignored) {}
    }
    canvas.addEventListener("pointerup", end);
    canvas.addEventListener("pointercancel", end);
    // Not passive, and deliberately: the wheel zooms the block instead of
    // scrolling the page past it, which is the behaviour anybody expects from a
    // 3D viewport. Keyboard users get the arrow keys below and never trap the page.
    canvas.addEventListener("wheel", function (e) {
      e.preventDefault();
      cam.dist = clamp(cam.dist * Math.pow(1.0015, e.deltaY), MIN_DIST, MAX_DIST);
    }, { passive: false });
    canvas.addEventListener("keydown", function (e) {
      var step = 0.16;
      if (e.key === "ArrowLeft") cam.yaw += step;
      else if (e.key === "ArrowRight") cam.yaw -= step;
      else if (e.key === "ArrowUp") cam.pitch = clamp(cam.pitch - step, MIN_PITCH, MAX_PITCH);
      else if (e.key === "ArrowDown") cam.pitch = clamp(cam.pitch + step, MIN_PITCH, MAX_PITCH);
      else return;
      e.preventDefault();
    });

    var spin = true;
    canvas.addEventListener("pointerdown", function () { spin = false; });
    canvas.addEventListener("keydown", function () { spin = false; });

    var last = 0;
    function frame(now) {
      // A slow idle turn until the reader takes hold of it, so the preview
      // announces itself as something to grab rather than sitting there looking
      // like a screenshot.
      if (spin) cam.yaw -= (now - last) * 0.00012;
      last = now;

      var w = el.clientWidth || 480;
      var h = Math.max(180, Math.round(w * (ids.length > 2 ? 0.34 : 0.6)));
      if (canvas.width !== w || canvas.height !== h) {
        renderer.setSize(w, h, false);
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
      }
      var cp = Math.cos(cam.pitch);
      camera.position.set(
        Math.sin(cam.yaw) * cp * cam.dist,
        Math.sin(cam.pitch) * cam.dist,
        Math.cos(cam.yaw) * cp * cam.dist);
      camera.lookAt(0, 0, 0);
      renderer.render(scene, camera);
      requestAnimationFrame(frame);
    }
    requestAnimationFrame(frame);

    if (labels.filter(Boolean).length === ids.length) {
      var row = document.createElement("div");
      row.className = "block-preview-labels";
      labels.forEach(function (t) {
        var s = document.createElement("span");
        s.textContent = t;
        row.appendChild(s);
      });
      el.appendChild(row);
    }
  }

  function start() {
    var nodes = document.querySelectorAll(".block-preview");
    if (!nodes.length) return;
    load(function () {
      if (!window.THREE) { fail(nodes); return; }
      Array.prototype.forEach.call(nodes, build);
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
}());
