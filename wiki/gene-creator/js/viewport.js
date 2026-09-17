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
    // Held, not just applied, so the lights-out view can put it back - and so
    // the two callers keep their own backdrops (see the note at the top).
    var dayBackground = opts.background === undefined ? 0x11161f : opts.background;
    scene.background = new THREE.Color(dayBackground);

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

    // Kept as variables, not inlined, because the lights-out view turns them
    // down - see setNight().
    var DAY = { ambient: 0.75, key: 0.85, fill: 0.35 };
    // Not zero. A glow is the only thing that should read at night, but a horse
    // lit by nothing at all is a silhouette, and you cannot tell whether the
    // unlit half of a marking is the shape you drew.
    var NIGHT = { ambient: 0.08, key: 0.06, fill: 0.04 };
    var NIGHT_BG = 0x05070c;

    var ambient = new THREE.AmbientLight(0xffffff, DAY.ambient);
    scene.add(ambient);
    var key = new THREE.DirectionalLight(0xffffff, DAY.key);
    key.position.set(4, 6, 4);
    scene.add(key);
    var fill = new THREE.DirectionalLight(0x93b7ff, DAY.fill);
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

    // ---- the glow ------------------------------------------------------
    // A second sheet: the coat's own colours, keyed by how brightly each texel
    // glows. It is the material's emissive map, which three adds on top of the
    // lit colour and does NOT attenuate by the lights - so turning the lights
    // down leaves exactly the glowing texels standing, which is what the game's
    // EmissiveCoatLayer does with RenderTypes.eyes at FULL_BRIGHT.
    var glowSheet = document.createElement("canvas");
    glowSheet.width = N;
    glowSheet.height = N;
    var glowCtx = glowSheet.getContext("2d", { willReadFrequently: true });
    var glowTex = new THREE.CanvasTexture(glowSheet);
    glowTex.magFilter = THREE.NearestFilter;
    glowTex.minFilter = THREE.NearestFilter;
    glowTex.generateMipmaps = false;
    glowTex.flipY = false;

    // emissiveMap on a Standard material: CONFIRMED on the pinned three r128 by
    // check-glow-render.mjs (2026-09-17) - the property survives construction,
    // and so does the white `emissive` that multiplies it. Asserted rather than
    // trusted because a material quietly dropping either fails INVISIBLY: a
    // perfectly good horse that simply never lights up.
    var material = new THREE.MeshStandardMaterial({
      map: texture, roughness: 0.85, metalness: 0.0,
      transparent: true, alphaTest: 0.05, side: THREE.DoubleSide,
      emissive: 0xffffff, emissiveMap: glowTex
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

    /**
     * The glow sheet, as DesignerApi.coatGlow hands it over - or null/an empty
     * ImageData when nothing on this horse glows.
     *
     * <p>The mod writes the glow level as the texel's ALPHA, because its
     * emissive pass blends. An emissive map is read RGB-only, with no alpha to
     * honour, so the level is folded into the colour here instead: RGB x level,
     * opaque. Same result - a dimmer colour, not a darker one - arrived at the
     * way this renderer can express it. This is the one deliberate divergence
     * from the texture GeneticCoatTextureFactory.buildGlow hands the game.
     */
    function setGlow(imageData) {
      glowCtx.clearRect(0, 0, N, N);
      if (imageData && imageData.width) {
        var d = imageData.data;
        for (var i = 0; i < d.length; i += 4) {
          var level = d[i + 3] / 255;
          d[i] = d[i] * level;
          d[i + 1] = d[i + 1] * level;
          d[i + 2] = d[i + 2] * level;
          d[i + 3] = 255;
        }
        glowCtx.putImageData(imageData, 0, 0);
      }
      glowTex.needsUpdate = true;
    }

    /**
     * Lights out. The glow is always drawn - in daylight it is simply lost in
     * an already-bright coat, exactly as it is in game - so this turns the
     * scene down rather than turning the glow up.
     */
    function setNight(on) {
      var night = !!on;
      ambient.intensity = night ? NIGHT.ambient : DAY.ambient;
      key.intensity = night ? NIGHT.key : DAY.key;
      fill.intensity = night ? NIGHT.fill : DAY.fill;
      scene.background.set(night ? NIGHT_BG : dayBackground);
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
      setGlow: setGlow,
      setNight: setNight,
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
