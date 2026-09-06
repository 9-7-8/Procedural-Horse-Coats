// The 3D preview: orbit the horse, click it to find out where you clicked.
//
// TWO CALLERS. The gene creator built this, and the wiki's per-gene preview
// window (wiki/gene-preview/) uses it as-is - same mesh, same framing, same
// orbit - so a fix to how a horse is shown reaches both. That is why the few
// things the two disagree about (the backdrop, the ground grid, whether a click
// means anything) are options with the creator's behaviour as the default,
// rather than a second copy of this file.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var geo = HG.geometry;
  var N = geo.SHEET_SIZE;

  function create(container, opts) {
    opts = opts || {};
    var THREE = window.THREE;
    if (!THREE) return null;

    var scene = new THREE.Scene();
    scene.background = new THREE.Color(
      opts.background === undefined ? 0x11161f : opts.background);

    var camera = new THREE.PerspectiveCamera(42, 1, 0.1, 100);
    // Re-aimed by frame() once the horse is built; these are only a first pose
    // so the first frame is not looking at nothing.
    camera.position.set(3.2, 2.2, 3.6);

    var renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.appendChild(renderer.domElement);

    var controls = null;
    if (THREE.OrbitControls) {
      controls = new THREE.OrbitControls(camera, renderer.domElement);
      controls.enableDamping = true;
      controls.dampingFactor = 0.08;
      controls.target.set(0, 1, 0);
    }

    scene.add(new THREE.AmbientLight(0xffffff, 0.75));
    var key = new THREE.DirectionalLight(0xffffff, 0.85);
    key.position.set(4, 6, 4);
    scene.add(key);
    var fill = new THREE.DirectionalLight(0x93b7ff, 0.35);
    fill.position.set(-5, 2, -4);
    scene.add(fill);
    // The horse stands on y = 0 (see model3d), so the grid is the ground.
    if (opts.grid !== false) {
      var grid = new THREE.GridHelper(6, 12, 0x2a3444, 0x1b2230);
      grid.position.y = 0;
      scene.add(grid);
    }

    var sheet = document.createElement("canvas");
    sheet.width = N;
    sheet.height = N;
    var sheetCtx = sheet.getContext("2d", { willReadFrequently: true });

    var texture = new THREE.CanvasTexture(sheet);
    texture.magFilter = THREE.NearestFilter;
    texture.minFilter = THREE.NearestFilter;
    texture.generateMipmaps = false;
    // The sheet's v runs top-down, like the texel grid, so don't let three flip it.
    texture.flipY = false;

    var material = new THREE.MeshStandardMaterial({
      map: texture, roughness: 0.85, metalness: 0.0,
      transparent: true, alphaTest: 0.05, side: THREE.DoubleSide
    });

    var mesh = null;
    var built = null;
    var currentSkin = null;

    function setSkin(skin) {
      if (skin === currentSkin) return;
      currentSkin = skin;
      if (mesh) {
        scene.remove(mesh);
        mesh.geometry.dispose();
      }
      built = HG.model3d.build(THREE, skin);
      mesh = new THREE.Mesh(built.geometry, material);
      scene.add(mesh);
      frame(built);
    }

    /**
     * Point the camera at the horse that was actually built. A foal is a third
     * the size of an adult, so a fixed camera pose either buries it in the grid
     * or clips the adult's head - the framing has to come from the model.
     */
    function frame(built) {
      var eye = built.height * 0.55;
      var dist = Math.max(built.length, built.height) * 1.45;
      camera.position.set(dist * 0.62, eye + built.height * 0.35, dist * 0.72);
      camera.near = dist / 50;
      camera.far = dist * 20;
      camera.updateProjectionMatrix();
      if (controls) {
        controls.target.set(0, eye, 0);
        controls.update();
      } else {
        camera.lookAt(0, eye, 0);
      }
    }

    function setImage(imageData) {
      sheetCtx.putImageData(imageData, 0, 0);
      texture.needsUpdate = true;
    }

    function resize() {
      var w = container.clientWidth, h = container.clientHeight;
      if (!w || !h) return;
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h, false);
    }

    // ---- picking -------------------------------------------------------

    var raycaster = new THREE.Raycaster();
    var pointer = new THREE.Vector2();
    var down = null;

    renderer.domElement.addEventListener("pointerdown", function (e) {
      down = { x: e.clientX, y: e.clientY };
    });

    renderer.domElement.addEventListener("pointerup", function (e) {
      if (!down || !mesh || !opts.onPick) return;
      var moved = Math.hypot(e.clientX - down.x, e.clientY - down.y);
      down = null;
      if (moved > 5) return; // that was an orbit, not a click

      var rect = renderer.domElement.getBoundingClientRect();
      pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
      pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
      raycaster.setFromCamera(pointer, camera);
      var hits = raycaster.intersectObject(mesh, false);
      if (!hits.length || !hits[0].uv) return;

      // UV back to a texel, then straight into the same sample grid the coat
      // pipeline uses - so a click reports the game's own part and body point.
      var px = Math.min(N - 1, Math.max(0, Math.floor(hits[0].uv.x * N)));
      var py = Math.min(N - 1, Math.max(0, Math.floor(hits[0].uv.y * N)));
      var sample = geo.sample(currentSkin, px, py);
      if (sample) opts.onPick(sample, px, py);
    });

    function animate() {
      requestAnimationFrame(animate);
      if (controls) controls.update();
      renderer.render(scene, camera);
    }

    resize();
    animate();
    window.addEventListener("resize", resize);
    if (window.ResizeObserver) new ResizeObserver(resize).observe(container);

    return {
      setSkin: setSkin,
      setImage: setImage,
      resize: resize,
      available: true,
      /** The OrbitControls instance, or null - so a caller can set its limits. */
      controls: controls,
      /** Back to the pose frame() chose for the horse that is loaded. */
      resetView: function () { if (built) frame(built); }
    };
  }

  HG.viewport = { create: create };
})(window.HG);
