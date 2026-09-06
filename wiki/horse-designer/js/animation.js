// Makes the horse walk about the field.
//
// READ THIS BEFORE TRUSTING IT: this is an APPROXIMATION, not a port. The coat
// pipeline in this tool is the game's own code (ported and parity-checked
// against the Java); the gait is not. Vanilla's leg swing lives in
// AbstractEquineModel.setupAnim and is driven by walkAnimationPos /
// walkAnimationSpeed, neither of which exists outside Minecraft, and the mod
// additionally divides the phase by the entity's scale in
// GeneticHorseRenderer.stretchGaitToSize. What is below is a plain diagonal-pair
// swing on the same pivots, tuned by eye.
//
// So: use this page to judge a COAT on a moving horse. Do not use it to judge
// an animation. The one thing it does borrow deliberately is the stride/scale
// relationship - stride length scales with the horse, which is exactly what
// stretchGaitToSize buys in game (a bigger horse takes proportionally longer,
// slower strides rather than skating).
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // Diagonal pairs: a Minecraft quadruped swings front-left with hind-right.
  var LEG_PHASE = {
    LEFT_FRONT_LEG: 0,
    RIGHT_HIND_LEG: 0,
    RIGHT_FRONT_LEG: Math.PI,
    LEFT_HIND_LEG: Math.PI
  };

  var SWING = 0.42;          // radians at full stride
  var STRIDE_BLOCKS = 1.15;  // ground covered per half-cycle, at scale 1
  var FIELD_RADIUS = 24;     // how far the horse is allowed to wander, in blocks

  function create(scene) {
    var horse = scene.horse;

    var s = {
      x: 0, z: 0, yaw: 0,
      phase: 0,
      gait: 0,          // 0 standing .. 1 walking, eased so legs settle rather than snap
      mode: "stand",
      timer: 1.5,
      yawRate: 0
    };

    function pick() {
      var r = Math.random();
      if (r < 0.42) { s.mode = "walk"; s.timer = 2 + Math.random() * 5; s.yawRate = (Math.random() - 0.5) * 0.35; }
      else if (r < 0.72) { s.mode = "stand"; s.timer = 1.2 + Math.random() * 3.5; s.yawRate = 0; }
      else { s.mode = "turn"; s.timer = 0.6 + Math.random() * 1.6; s.yawRate = (Math.random() < 0.5 ? -1 : 1) * (0.6 + Math.random() * 0.9); }
    }

    function headHome() {
      // Turn toward the middle of the field, the short way round.
      var want = Math.atan2(-s.x, -s.z);
      var d = wrapPi(want - s.yaw);
      s.mode = "turn";
      s.timer = Math.min(2.5, Math.abs(d) / 1.1 + 0.2);
      s.yawRate = (d >= 0 ? 1 : -1) * 1.1;
    }

    /**
     * opts: { wander, walkSpeed (blocks/sec at scale 1), sizeScale }
     * Returns nothing; it writes straight into the rig.
     */
    function update(dt, opts) {
      var parts = scene.parts();
      var size = opts.sizeScale || 1;

      if (opts.wander) {
        s.timer -= dt;
        if (s.timer <= 0) pick();
        if (Math.hypot(s.x, s.z) > FIELD_RADIUS && s.mode !== "turn") headHome();

        s.yaw += s.yawRate * dt;

        var target = s.mode === "walk" ? 1 : 0;
        s.gait += (target - s.gait) * Math.min(1, dt * 3.5);

        // A bigger horse covers more ground per stride, so speed scales too.
        var speed = (opts.walkSpeed || 1.1) * size * s.gait;
        var moved = speed * dt;
        s.x += Math.sin(s.yaw) * moved;
        s.z += Math.cos(s.yaw) * moved;

        // Phase advances with DISTANCE, not with time - which is what stops the
        // feet skating when the speed or the size changes. Vanilla does the
        // same thing (walkAnimationPos accumulates distance moved).
        s.phase += moved / (STRIDE_BLOCKS * size);
      } else {
        s.gait += (0 - s.gait) * Math.min(1, dt * 4);
      }

      horse.position.set(s.x, 0, s.z);
      // Body space puts the nose at +X, so a yaw of 0 faces world +X.
      horse.rotation.y = s.yaw - Math.PI / 2;

      var w = s.phase * Math.PI * 2;
      var amp = SWING * s.gait;

      Object.keys(LEG_PHASE).forEach(function (leg) {
        var g = parts[leg];
        if (!g) return;
        // A model-space pitch is a body-space z rotation, negated - see
        // model3d.buildParts.
        g.rotation.z = -Math.sin(w + LEG_PHASE[leg]) * amp;
      });

      // The neck nods once per full stride, the tail sways at half that. Both
      // are small on purpose: an over-animated preview hides the coat, which is
      // the thing you came here to look at.
      if (parts.NECK) parts.NECK.rotation.z = -Math.sin(w) * 0.05 * s.gait;
      if (parts.TAIL) parts.TAIL.rotation.z = -Math.sin(w * 0.5) * 0.10 * s.gait;
    }

    function reset() {
      s.x = s.z = s.yaw = s.phase = 0;
      s.mode = "stand";
      s.timer = 1.5;
      s.yawRate = 0;
      horse.position.set(0, 0, 0);
    }

    /** Where the camera should look: the middle of the barrel, at this size. */
    function focusPoint(sizeScale, height) {
      return { x: s.x, y: height * 0.62, z: s.z };
    }

    return { update: update, reset: reset, state: s, focusPoint: focusPoint };
  }

  function wrapPi(a) {
    while (a > Math.PI) a -= Math.PI * 2;
    while (a < -Math.PI) a += Math.PI * 2;
    return a;
  }

  HG.designerAnimation = { create: create, FIELD_RADIUS: FIELD_RADIUS };
})(window.HG);
