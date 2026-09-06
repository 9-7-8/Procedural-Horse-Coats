// The world the horse stands in: a grass field, a plain sky, and a camera you
// can orbit, pan and zoom.
//
// One world unit is ONE BLOCK, and that is the whole point of this file. The
// gene creator's viewport normalises the horse to a fixed on-screen length,
// because there it does not matter how big a horse is - you are looking at a
// pattern. Here it matters completely: the grass tiles at one block per tile
// and the reference cube is a real cubic metre, so a horse resolved at scale
// 1.4 has to LOOK 1.4x, against something. So the rig scales model units
// (1 = 1/16 block) by sizeScale/16 and nothing else.
//
// The mesh itself comes from HG.model3d.buildParts - the gene creator's own
// geometry, split per part so a leg can swing about its pivot. Both tools share
// that emitter, so a fix to how a horse is drawn reaches both.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var geo = HG.geometry;
  var N = geo.SHEET_SIZE;

  var UNITS_PER_BLOCK = 16;
  var FIELD_BLOCKS = 160;          // the grass plane, a side
  var SKY = 0x8fc3f0;
  var GROUND_TINT = 0x7c7c7c;   // multiplies the grass texture - see below
  var HORIZON = 0xcfe4f5;

  function create(container) {
    var THREE = window.THREE;
    if (!THREE) return null;

    var scene = new THREE.Scene();
    scene.background = new THREE.Color(SKY);
    // Fog does the work a skybox would: the grass fades to the sky colour at
    // the horizon instead of ending in a hard edge against it.
    scene.fog = new THREE.Fog(HORIZON, 18, 90);

    var camera = new THREE.PerspectiveCamera(55, 1, 0.02, 400);

    // A machine with no usable WebGL throws here rather than returning null, and
    // an uncaught throw would take the whole panel down with the field.
    var renderer;
    try {
      renderer = new THREE.WebGLRenderer({ antialias: true });
    } catch (err) {
      return null;
    }
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.appendChild(renderer.domElement);
    renderer.domElement.setAttribute("tabindex", "0");
    renderer.domElement.style.outline = "none";

    // ---- light ---------------------------------------------------------
    // Flat and bright, like the game: a strong ambient so nothing reads black,
    // plus one key light so the boxes still have corners.
    scene.add(new THREE.HemisphereLight(0xffffff, 0x6b8f4a, 0.95));
    var key = new THREE.DirectionalLight(0xffffff, 0.75);
    key.position.set(6, 10, 4);
    scene.add(key);
    var fill = new THREE.DirectionalLight(0xbcd6ff, 0.25);
    fill.position.set(-6, 3, -5);
    scene.add(fill);

    // ---- ground --------------------------------------------------------

    var grassTex = null;
    var ground = new THREE.Mesh(
      new THREE.PlaneGeometry(FIELD_BLOCKS, FIELD_BLOCKS),
      new THREE.MeshLambertMaterial({ color: 0x4a6b2a })
    );
    ground.rotation.x = -Math.PI / 2;
    scene.add(ground);

    (function () {
      var img = new Image();
      img.onload = function () {
        grassTex = new THREE.Texture(img);
        grassTex.wrapS = grassTex.wrapT = THREE.RepeatWrapping;
        // One tile per block, and nearest-filtered: the point of using the
        // game's own grass is that it reads at the game's own resolution.
        grassTex.repeat.set(FIELD_BLOCKS, FIELD_BLOCKS);
        grassTex.magFilter = THREE.NearestFilter;
        grassTex.minFilter = THREE.LinearMipmapLinearFilter;
        grassTex.anisotropy = renderer.capabilities.getMaxAnisotropy
          ? renderer.capabilities.getMaxAnisotropy() : 1;
        grassTex.needsUpdate = true;
        // GROUND_TINT multiplies the texture down. The grass PNG is a bright
        // top-face texture authored to be read under Minecraft's own baked
        // lighting; put under a hemisphere light plus a key light it comes out
        // luminous. Darkening the material rather than the lights is deliberate
        // - the horse is lit correctly and must stay that way.
        ground.material = new THREE.MeshLambertMaterial({ map: grassTex, color: GROUND_TINT });
      };
      // A plain fetch: this page is served over HTTP (the wasm needs it), so
      // there is nothing to inline and nothing to keep in step.
      img.src = "assets/grass.jpg";
    })();

    // A one-block cube, off to the side, so "how big is this horse" has an
    // answer you can see rather than one you have to read off a slider.
    var refCube = new THREE.Mesh(
      new THREE.BoxGeometry(1, 1, 1),
      new THREE.MeshLambertMaterial({ color: 0xd8d2c4 })
    );
    refCube.position.set(0, 0.5, 2.6);
    scene.add(refCube);
    var refEdges = new THREE.LineSegments(
      new THREE.EdgesGeometry(refCube.geometry),
      new THREE.LineBasicMaterial({ color: 0x5c5648 })
    );
    refEdges.position.copy(refCube.position);
    scene.add(refEdges);

    // A player standing beside it. One block tells you a length; a person tells
    // you whether a horse is a horse - which is the question the size locus
    // actually raises.
    var player = makePlayer(THREE);
    player.position.set(1.3, 0, 2.6);
    player.rotation.y = -0.5;
    scene.add(player);

    // ---- the coat sheet, as a texture ----------------------------------

    var sheet = document.createElement("canvas");
    sheet.width = N;
    sheet.height = N;
    var sheetCtx = sheet.getContext("2d", { willReadFrequently: true });
    var coatTex = new THREE.CanvasTexture(sheet);
    coatTex.magFilter = THREE.NearestFilter;
    coatTex.minFilter = THREE.NearestFilter;
    coatTex.generateMipmaps = false;
    coatTex.flipY = false;   // the sheet's v runs top-down, like the texel grid

    var coatMaterial = new THREE.MeshLambertMaterial({
      map: coatTex, transparent: true, alphaTest: 0.05, side: THREE.DoubleSide
    });

    // ---- the horse rig -------------------------------------------------

    var horse = new THREE.Group();          // world position + heading
    var scaled = new THREE.Group();         // size, and model units -> blocks
    var centred = new THREE.Group();        // nose-to-tail centre over the origin
    horse.add(scaled);
    scaled.add(centred);
    scene.add(horse);

    var shadow = makeShadow(THREE);
    scene.add(shadow);

    var partGroups = {};
    var currentSkin = null;
    var built = null;

    function setSkin(skin) {
      if (skin === currentSkin) return;
      currentSkin = skin;

      Object.keys(partGroups).forEach(function (name) {
        var grp = partGroups[name];
        centred.remove(grp);
        grp.children.forEach(function (m) { if (m.geometry) m.geometry.dispose(); });
      });
      partGroups = {};

      built = HG.model3d.buildParts(THREE, skin);
      built.parts.forEach(function (p) {
        var grp = new THREE.Group();
        grp.position.set(p.pivot.x, p.pivot.y, p.pivot.z);
        grp.add(new THREE.Mesh(p.geometry, coatMaterial));
        centred.add(grp);
        partGroups[p.name] = grp;
      });

      var bb = built.bodyBounds;
      centred.position.set(-(bb.xMin + bb.xMax) / 2, 0, 0);
      applyScale();
    }

    var sizeScale = 1;

    function applyScale() {
      scaled.scale.setScalar(sizeScale / UNITS_PER_BLOCK);
      if (built) {
        var bb = built.bodyBounds;
        var w = (bb.zMax - bb.zMin) / UNITS_PER_BLOCK * sizeScale;
        var l = (bb.xMax - bb.xMin) / UNITS_PER_BLOCK * sizeScale;
        shadow.scale.set(l * 0.62, 1, w * 0.95);
      }
    }

    function setSize(s) { sizeScale = s; applyScale(); }

    /** Hoof-to-ear-tip height and nose-to-tail length, in blocks, at the live size. */
    function measure() {
      if (!built) return { height: 0, length: 0, width: 0 };
      var bb = built.bodyBounds;
      return {
        height: (bb.yMax - bb.yMin) / UNITS_PER_BLOCK * sizeScale,
        length: (bb.xMax - bb.xMin) / UNITS_PER_BLOCK * sizeScale,
        width: (bb.zMax - bb.zMin) / UNITS_PER_BLOCK * sizeScale
      };
    }

    function setImage(imageData) {
      sheetCtx.putImageData(imageData, 0, 0);
      coatTex.needsUpdate = true;
    }

    // ---- camera rig ----------------------------------------------------
    //
    // Hand-rolled rather than OrbitControls, because the brief is orbit AND
    // free movement: OrbitControls owns the focus point, so WASD panning has to
    // fight it for the target every frame. Here the focus is just a Vector3 the
    // two inputs share.

    var cam = {
      yaw: Math.PI * 0.25,
      pitch: 0.30,
      dist: 5.5,
      focus: new THREE.Vector3(0, 1, 0)
    };
    var MIN_DIST = 0.6, MAX_DIST = 60;
    var MAX_PITCH = 1.45, MIN_PITCH = -0.35;

    var dragging = false, lastX = 0, lastY = 0, dragMoved = 0;
    var el = renderer.domElement;

    el.addEventListener("pointerdown", function (e) {
      dragging = true; dragMoved = 0;
      lastX = e.clientX; lastY = e.clientY;
      el.setPointerCapture(e.pointerId);
      el.focus();
    });
    el.addEventListener("pointermove", function (e) {
      if (!dragging) return;
      var dx = e.clientX - lastX, dy = e.clientY - lastY;
      lastX = e.clientX; lastY = e.clientY;
      dragMoved += Math.abs(dx) + Math.abs(dy);
      cam.yaw -= dx * 0.006;
      cam.pitch = clamp(cam.pitch + dy * 0.005, MIN_PITCH, MAX_PITCH);
    });
    function endDrag(e) {
      if (!dragging) return;
      dragging = false;
      try { el.releasePointerCapture(e.pointerId); } catch (ignored) {}
    }
    el.addEventListener("pointerup", endDrag);
    el.addEventListener("pointercancel", endDrag);

    el.addEventListener("wheel", function (e) {
      e.preventDefault();
      cam.dist = clamp(cam.dist * Math.pow(1.0015, e.deltaY), MIN_DIST, MAX_DIST);
    }, { passive: false });

    var keys = {};
    var onMove = null;   // told when WASD is used, so "follow" can switch itself off
    function keyName(e) { return (e.key || "").toLowerCase(); }
    el.addEventListener("keydown", function (e) {
      var k = keyName(e);
      // Length check first: "".indexOf on the key list matches at 0, so a key
      // event with no `key` would otherwise be swallowed as movement.
      if (k.length !== 1 || ("wasdqe".indexOf(k) < 0 && k !== " ")) return;
      e.preventDefault();
      keys[k] = true;
      if (onMove) onMove();
    });
    el.addEventListener("keyup", function (e) { keys[keyName(e)] = false; });
    el.addEventListener("blur", function () { keys = {}; });

    function moveFocus(dt) {
      var f = 0, s = 0, up = 0;
      if (keys.w) f += 1;
      if (keys.s) f -= 1;
      if (keys.d) s += 1;
      if (keys.a) s -= 1;
      if (keys.e || keys[" "]) up += 1;
      if (keys.q) up -= 1;
      if (!f && !s && !up) return false;

      // Move in the camera's own ground plane, at a speed that scales with how
      // far out you are - close in you want fine control, zoomed out you want
      // to cross the field.
      var speed = Math.max(1.5, cam.dist * 0.9) * dt;
      var sinY = Math.sin(cam.yaw), cosY = Math.cos(cam.yaw);
      // Forward is the direction the camera looks, flattened.
      cam.focus.x += (-sinY * f + cosY * s) * speed;
      cam.focus.z += (-cosY * f - sinY * s) * speed;
      cam.focus.y = clamp(cam.focus.y + up * speed, 0.05, 40);
      return true;
    }

    // How far right of window centre the horse should appear, in CSS pixels.
    // The GUI's two panels are not symmetric, so centring the horse in the
    // window would bury it behind the gene list; this slides the whole view
    // sideways instead, which is a shift rather than a rotation and so leaves
    // the horse un-skewed.
    var screenOffsetPx = 0;

    function setScreenOffset(px) { screenOffsetPx = px || 0; }

    function placeCamera() {
      var cp = Math.cos(cam.pitch), sp = Math.sin(cam.pitch);
      camera.position.set(
        cam.focus.x + cam.dist * cp * Math.sin(cam.yaw),
        cam.focus.y + cam.dist * sp,
        cam.focus.z + cam.dist * cp * Math.cos(cam.yaw)
      );
      var target = cam.focus;
      if (screenOffsetPx) {
        var h = container.clientHeight || 1;
        var perPixel = 2 * cam.dist * Math.tan(camera.fov * Math.PI / 360) / h;
        var world = -screenOffsetPx * perPixel;
        // right = (cos yaw, 0, -sin yaw) for this spherical rig
        var rx = Math.cos(cam.yaw) * world, rz = -Math.sin(cam.yaw) * world;
        camera.position.x += rx;
        camera.position.z += rz;
        target = new THREE.Vector3(cam.focus.x + rx, cam.focus.y, cam.focus.z + rz);
      }
      camera.lookAt(target);
    }

    function resize() {
      var w = container.clientWidth, h = container.clientHeight;
      if (!w || !h) return;
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h, false);
    }

    // ---- the loop ------------------------------------------------------

    var onFrame = null;
    var last = performance.now();

    function loop(now) {
      requestAnimationFrame(loop);
      var dt = Math.min(0.1, (now - last) / 1000);
      last = now;
      moveFocus(dt);
      if (onFrame) onFrame(dt);
      shadow.position.set(horse.position.x, 0.01, horse.position.z);
      placeCamera();
      renderer.render(scene, camera);
    }

    resize();
    requestAnimationFrame(loop);
    window.addEventListener("resize", resize);
    if (window.ResizeObserver) new ResizeObserver(resize).observe(container);

    return {
      available: true,
      setSkin: setSkin,
      setSize: setSize,
      setImage: setImage,
      measure: measure,
      resize: resize,
      horse: horse,
      parts: function () { return partGroups; },
      focus: cam.focus,
      camera: cam,
      onFrame: function (fn) { onFrame = fn; },
      /** Where the horse wanders back to: the reference block. */
      homePoint: function () { return { x: refCube.position.x, z: refCube.position.z }; },
      setScreenOffset: setScreenOffset,
      resetView: function () {
        cam.yaw = Math.PI * 0.25;
        cam.pitch = 0.30;
        cam.dist = 5.5;
      },
      onManualMove: function (fn) { onMove = fn; },
      lookAt: function (x, y, z, snap) {
        if (snap) cam.focus.set(x, y, z);
        else cam.focus.lerp(new THREE.Vector3(x, y, z), 0.12);
      }
    };
  }

  /**
   * A default-proportioned Minecraft player, built from boxes.
   *
   * <p>Vanilla numbers, in model units: legs and arms 4x12x4, body 8x12x4, head
   * 8x8x8, stacked to 32 - then the 0.9375 the player renderer applies, which
   * lands it at 1.875 blocks. It is untextured on purpose: the default skin is
   * Mojang's asset and not this repo's to ship, so these are flat colours in
   * roughly its palette. It is a ruler, not a character.
   */
  function makePlayer(THREE) {
    var U = 0.9375 / 16;          // model units -> blocks, with the renderer's scale
    var SKIN = 0xC69C6D, SHIRT = 0x00A8A8, TROUSERS = 0x3B44A0, HAIR = 0x3F2E1E;
    var g = new THREE.Group();

    function box(w, h, d, x, y, z, colour) {
      var m = new THREE.Mesh(
        new THREE.BoxGeometry(w * U, h * U, d * U),
        new THREE.MeshLambertMaterial({ color: colour })
      );
      // y is given as the box's BOTTOM, in model units off the ground.
      m.position.set(x * U, (y + h / 2) * U, z * U);
      g.add(m);
      return m;
    }

    box(4, 12, 4, -2, 0, 0, TROUSERS);   // right leg
    box(4, 12, 4, 2, 0, 0, TROUSERS);    // left leg
    box(8, 12, 4, 0, 12, 0, SHIRT);      // body
    box(4, 12, 4, -6, 12, 0, SKIN);      // right arm
    box(4, 12, 4, 6, 12, 0, SKIN);       // left arm
    box(8, 8, 8, 0, 24, 0, SKIN);        // head
    box(8.4, 3, 8.4, 0, 29, 0, HAIR);    // a suggestion of hair, so it reads as a person
    return g;
  }

  /** A soft dark ellipse under the horse - cheaper and calmer than a shadow map. */
  function makeShadow(THREE) {
    var c = document.createElement("canvas");
    c.width = c.height = 64;
    var g = c.getContext("2d");
    var grad = g.createRadialGradient(32, 32, 0, 32, 32, 32);
    grad.addColorStop(0, "rgba(0,0,0,0.42)");
    grad.addColorStop(0.6, "rgba(0,0,0,0.18)");
    grad.addColorStop(1, "rgba(0,0,0,0)");
    g.fillStyle = grad;
    g.fillRect(0, 0, 64, 64);
    var m = new THREE.Mesh(
      new THREE.PlaneGeometry(1, 1),
      new THREE.MeshBasicMaterial({
        map: new THREE.CanvasTexture(c), transparent: true, depthWrite: false
      })
    );
    m.rotation.x = -Math.PI / 2;
    m.position.y = 0.01;
    return m;
  }

  function clamp(v, lo, hi) { return v < lo ? lo : (v > hi ? hi : v); }

  HG.designerScene = { create: create, UNITS_PER_BLOCK: UNITS_PER_BLOCK };
})(window.HG);
