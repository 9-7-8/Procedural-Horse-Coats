// The 3D preview: orbit the horse, click it to find out where you clicked.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var geo = HG.geometry;
  var N = geo.SHEET_SIZE;

  function create(container, opts) {
    var THREE = window.THREE;
    if (!THREE) return null;

    var scene = new THREE.Scene();
    scene.background = new THREE.Color(0x11161f);

    var camera = new THREE.PerspectiveCamera(42, 1, 0.1, 100);
    camera.position.set(2.6, 1.1, 2.6);

    var renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.appendChild(renderer.domElement);

    var controls = null;
    if (THREE.OrbitControls) {
      controls = new THREE.OrbitControls(camera, renderer.domElement);
      controls.enableDamping = true;
      controls.dampingFactor = 0.08;
      controls.target.set(0, 0, 0);
    }

    scene.add(new THREE.AmbientLight(0xffffff, 0.75));
    var key = new THREE.DirectionalLight(0xffffff, 0.85);
    key.position.set(4, 6, 4);
    scene.add(key);
    var fill = new THREE.DirectionalLight(0x93b7ff, 0.35);
    fill.position.set(-5, 2, -4);
    scene.add(fill);
    var grid = new THREE.GridHelper(4, 12, 0x2a3444, 0x1b2230);
    grid.position.y = -1.05;
    scene.add(grid);

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
    var currentSkin = null;

    function setSkin(skin) {
      if (skin === currentSkin) return;
      currentSkin = skin;
      if (mesh) {
        scene.remove(mesh);
        mesh.geometry.dispose();
      }
      mesh = new THREE.Mesh(HG.model3d.build(THREE, skin), material);
      scene.add(mesh);
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

    return { setSkin: setSkin, setImage: setImage, resize: resize, available: true };
  }

  HG.viewport = { create: create };
})(window.HG);
