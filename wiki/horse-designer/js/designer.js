// Wires the settings panel to the coat bake and the field.
//
// The bake is HG.preview.bake - the gene creator's, unchanged - with a
// per-locus base-coat config instead of a named preset, and with `spec` left
// null when no data-driven gene is loaded. So the horse in the field is
// composed by the same three phases in the same order with the same constants
// as the horse in the creator, and as the horse in the game.
//
// What this file owns is only the UI: which loci are on, what the sliders say,
// and the toasts that admit what the page cannot show.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var $ = function (id) { return document.getElementById(id); };

  // Genes that exist in JavaScript, for the honesty toast. Kept as prose rather
  // than derived, because there is nothing to derive it from - the registry
  // lives in Java and this page cannot see it.
  var PORTED = [
    "extension (MC1R)", "agouti / BayCoat", "MATP (cream + pearl)",
    "champagne", "grey / GreyCoat", "KIT's dominant-white outcome"
  ];

  function start() {
    var scene = HG.designerScene.create($("viewport"));
    var sheetCtx = $("sheet-canvas").getContext("2d");
    var anim = null;

    if (!scene) {
      $("nowebgl").hidden = false;
      $("hud").hidden = true;
    } else {
      anim = HG.designerAnimation.create(scene);
      scene.onManualMove(function () {
        if ($("follow").checked) { $("follow").checked = false; }
      });
    }

    fillPresets();
    bindControls();

    HG.preview.load(function () {
      rebuild(true);
      openingToasts();
    });

    if (scene) {
      scene.onFrame(function (dt) {
        anim.update(dt, {
          wander: $("wander").checked,
          walkSpeed: parseFloat($("walk-speed").value),
          sizeScale: parseFloat($("size").value)
        });
        if ($("follow").checked) {
          var m = scene.measure();
          var f = anim.focusPoint(parseFloat($("size").value), m.height);
          scene.lookAt(f.x, f.y, f.z, false);
        }
      });
    }

    // ---- the coat ------------------------------------------------------

    var loadedSpec = null;
    var pending = 0;

    /** Read the panel into the base-coat config HG.baseCoats.compose understands. */
    function coatConfig() {
      return {
        extension: $("extension").value,
        agouti: $("agouti").checked
          ? { leg: parseFloat($("agouti-leg").value), face: parseFloat($("agouti-face").value) }
          : null,
        dilution: $("dilution").value || null,
        grey: $("grey").checked ? {
          seed: HG.baseCoats.GREY_DEFAULTS.seed,
          progress: parseFloat($("grey-progress").value),
          spacing: parseFloat($("grey-spacing").value),
          dappleStrength: parseFloat($("grey-strength").value),
          pointRetention: parseFloat($("grey-retention").value)
        } : null,
        white: $("white").checked
      };
    }

    function rebuild(immediate) {
      cancelAnimationFrame(pending);
      var run = function () {
        var skin = $("skin").value;
        var result = HG.preview.bake({
          spec: loadedSpec,
          skin: skin,
          baseCoatConfig: coatConfig(),
          seed: parseInt($("gene-seed").value, 10) || 0,
          dose: parseInt($("gene-dose").value, 10),
          coverageLayer: -1
        });
        var img = HG.preview.toImageData(sheetCtx, result.pixels, null);
        sheetCtx.putImageData(img, 0, 0);
        if (scene) {
          scene.setSkin(skin);
          scene.setSize(parseFloat($("size").value));
          scene.setImage(img);
        }
        if (loadedSpec) {
          $("gene-status").textContent = result.expresses
            ? "This copy count expresses: the gene is painting."
            : "This copy count is a wild type — a silent carrier, so nothing is painted.";
        }
        readouts();
      };
      if (immediate) run();
      else pending = requestAnimationFrame(run);
    }

    function readouts() {
      $("size-val").textContent = fixed($("size").value, 2) + "×";
      $("walk-val").textContent = fixed($("walk-speed").value, 2);
      $("agouti-leg-val").textContent = fixed($("agouti-leg").value, 2);
      $("agouti-face-val").textContent = fixed($("agouti-face").value, 2);
      $("grey-progress-val").textContent = fixed($("grey-progress").value, 2);
      $("grey-spacing-val").textContent = fixed($("grey-spacing").value, 2);
      $("grey-strength-val").textContent = fixed($("grey-strength").value, 2);
      $("grey-retention-val").textContent = fixed($("grey-retention").value, 2);
      $("gene-dose-val").textContent = $("gene-dose").value;

      var s = parseFloat($("size").value);
      var band = s < 0.45 || s > 1.75
        ? "outside the natural clamp (0.45–1.75) — only a magical size locus reaches here"
        : "inside the natural clamp (0.45–1.75)";
      $("size-note").textContent = band;

      if (scene) {
        var m = scene.measure();
        $("hud-size").textContent =
          fixed(s, 2) + "× · " + fixed(m.height, 2) + " blocks to the ear tip · "
          + fixed(m.length, 2) + " long";
      }
    }

    // ---- controls ------------------------------------------------------

    function fillPresets() {
      var sel = $("preset");
      sel.appendChild(opt("", "Custom"));
      HG.baseCoats.presets.forEach(function (p) { sel.appendChild(opt(p.id, p.label)); });
      sel.value = "black";
      applyPreset("black");
    }

    /** Push a preset's config into the per-locus controls, so they stay honest. */
    function applyPreset(id) {
      var p = HG.baseCoats.byId(id);
      var c = p.config || {};
      $("extension").value = c.extension === "chestnut" ? "chestnut" : "wild";
      $("agouti").checked = !!c.agouti;
      if (c.agouti) {
        $("agouti-leg").value = c.agouti.leg;
        $("agouti-face").value = c.agouti.face;
      }
      $("dilution").value = c.dilution || "";
      $("grey").checked = !!c.grey;
      if (c.grey) {
        $("grey-progress").value = c.grey.progress;
        $("grey-spacing").value = c.grey.spacing;
        $("grey-strength").value = c.grey.dappleStrength;
        $("grey-retention").value = c.grey.pointRetention;
      }
      $("white").checked = !!c.white;
      subs();
    }

    function subs() {
      $("agouti-sub").classList.toggle("off", !$("agouti").checked);
      $("grey-sub").classList.toggle("off", !$("grey").checked);
    }

    function bindControls() {
      // Any locus touched by hand means the preset name no longer describes the
      // horse, so say so rather than leave a stale label sitting there.
      ["extension", "agouti", "agouti-leg", "agouti-face", "dilution",
        "grey", "grey-progress", "grey-spacing", "grey-strength", "grey-retention", "white"
      ].forEach(function (id) {
        $(id).addEventListener("input", function () {
          $("preset").value = "";
          subs();
          rebuild();
        });
      });

      $("preset").addEventListener("change", function () {
        if (!$("preset").value) return;
        applyPreset($("preset").value);
        rebuild();
      });

      $("skin").addEventListener("change", function () {
        if ($("skin").value === "BABY" && $("grey").checked) {
          toast("A grey foal is not a thing",
            "Grey is adults only in game — a foal is born its base colour and greys later. "
            + "The sheet below still bakes it, so you can see where it is heading.", "warn");
        }
        rebuild();
      });

      $("size").addEventListener("input", function () {
        if (scene) scene.setSize(parseFloat($("size").value));
        readouts();
      });

      document.querySelectorAll(".chip[data-size]").forEach(function (b) {
        b.addEventListener("click", function () {
          $("size").value = b.getAttribute("data-size");
          if (scene) scene.setSize(parseFloat($("size").value));
          readouts();
        });
      });

      $("walk-speed").addEventListener("input", readouts);
      $("wander").addEventListener("change", function () {
        if (!$("wander").checked && anim) anim.reset();
      });

      $("ref-cube").addEventListener("change", function () {
        if (scene) scene.setReferenceCube($("ref-cube").checked);
      });
      $("shadow").addEventListener("change", function () {
        if (scene) scene.setShadow($("shadow").checked);
      });
      $("show-sheet").addEventListener("change", function () {
        $("sheet-wrap").hidden = !$("show-sheet").checked;
      });

      $("btn-reset-view").addEventListener("click", function () {
        if (!scene) return;
        scene.camera.yaw = Math.PI * 0.25;
        scene.camera.pitch = 0.30;
        scene.camera.dist = 5.5;
        $("follow").checked = true;
      });

      $("btn-random").addEventListener("click", randomise);
      $("btn-limits").addEventListener("click", limitsToast);

      // ---- the data-driven gene ----
      $("gene-file").addEventListener("change", function (e) {
        var file = e.target.files && e.target.files[0];
        if (!file) return;
        var reader = new FileReader();
        reader.onload = function () { loadGeneText(String(reader.result), file.name); };
        reader.readAsText(file);
        e.target.value = "";
      });

      $("btn-gene-paste").addEventListener("click", function () {
        var text = window.prompt("Paste a gene JSON file");
        if (text) loadGeneText(text, "pasted");
      });

      $("btn-gene-clear").addEventListener("click", function () {
        loadedSpec = null;
        $("gene-loaded").hidden = true;
        rebuild();
      });

      $("gene-dose").addEventListener("input", rebuild);
      $("gene-seed").addEventListener("input", rebuild);
      $("btn-gene-reseed").addEventListener("click", function () {
        $("gene-seed").value = (Math.random() * 0x7fffffff) | 0;
        rebuild();
      });
    }

    function loadGeneText(text, label) {
      var spec;
      try {
        spec = JSON.parse(text);
      } catch (err) {
        toast("That is not valid JSON", String(err.message || err), "bad");
        return;
      }
      if (!spec || !spec.expressions) {
        toast("That does not look like a gene file",
          "A gene file needs an <code>expressions</code> table. Format 1 files predate it "
          + "and will not load — the gene creator can rewrite one.", "bad");
        return;
      }
      loadedSpec = spec;
      $("gene-loaded").hidden = false;
      $("gene-name").textContent = (spec.name || spec.key || label)
        + "  ·  " + (spec.phase || "natural") + "  ·  " + ((spec.alleles || []).length) + " alleles";
      rebuild();
      geneWarnings(spec);
    }

    /**
     * Everything a gene file can declare that this page renders as nothing.
     * Said out loud, per gene, because a silently-ignored block is exactly how
     * a preview tool teaches someone the wrong thing.
     */
    function geneWarnings(spec) {
      var verbs = {};
      (spec.expressions || []).forEach(function (e) {
        // The verb is the effect entry's `type` - see wiki/gene-effects.html.
        (e.effects || []).forEach(function (fx) { if (fx.type) verbs[fx.type] = true; });
      });
      var list = Object.keys(verbs);
      if (list.length) {
        toast("Effects are not simulated here",
          "This gene declares <code>" + list.join("</code>, <code>") + "</code>. Effects are "
          + "Minecraft-side — particles, glowing, world light, mob effects, milking, block "
          + "spreading — and none of it exists in a browser. The coat is unaffected; "
          + "everything the gene <em>does</em> is invisible on this page.", "warn");
      }
      if (spec.phase === "magical") {
        toast("Magical phase renders, emissiveness does not",
          "A magical gene’s signed RGB really is added here, in phase 3. What is missing is "
          + "the <code>glow</code> verb’s emissive mask — in game those texels are drawn "
          + "full-bright by <code>EmissiveCoatLayer</code>; here they are just their own colour.",
          "warn");
      }
    }

    function randomise() {
      var p = HG.baseCoats.presets[(Math.random() * HG.baseCoats.presets.length) | 0];
      $("preset").value = p.id;
      applyPreset(p.id);
      if (Math.random() < 0.45) {
        $("agouti").checked = true;
        $("agouti-leg").value = (Math.random()).toFixed(2);
        $("agouti-face").value = (Math.random() * 0.7).toFixed(2);
        $("preset").value = "";
      }
      $("size").value = (0.55 + Math.random() * 1.15).toFixed(2);
      subs();
      if (scene) scene.setSize(parseFloat($("size").value));
      rebuild();
    }

    // ---- toasts --------------------------------------------------------

    function openingToasts() {
      toast("The coat is real; the horse is not the whole horse",
        "Every pixel here comes from the mod’s own pipeline, ported to JavaScript and "
        + "parity-checked against the Java. But only <strong>" + PORTED.length + " genes</strong> "
        + "exist in that port. The other forty-odd built-ins — every white-pattern locus, "
        + "the leopard complex, dun, silver, roan, tobiano, brindle, and all the magical ones "
        + "— are Java-only.", "warn");
      toast("The gait is a guess",
        "The walk is hand-tuned, not a port of <code>AbstractEquineModel.setupAnim</code>. "
        + "Judge a coat on a moving horse by all means; do not judge the animation.", "info");
    }

    function limitsToast() {
      toast("What is ported, and what is not",
        "<strong>Ported and exact:</strong> the geometry, the noise, the three-phase composer, "
        + "the gradient LUT, the spec engine, and these genes — " + PORTED.join(", ") + ". "
        + "<br><br><strong>Not here at all:</strong> the other built-in genes; every "
        + "<code>effects</code> verb; emissive/glowing texels; particles; the cutie-mark item "
        + "icons (they come from the game’s item registry); per-part scaling for the two "
        + "dwarfisms; and eye-colour tinting, which lives in the overlay phase and has no JS "
        + "port yet.", "info");
    }

    function toast(title, html, kind) {
      var box = document.createElement("div");
      box.className = "toast " + (kind || "info");
      var h = document.createElement("h4");
      h.textContent = title;
      var p = document.createElement("p");
      p.innerHTML = html;
      var x = document.createElement("button");
      x.type = "button";
      x.className = "toast-close";
      x.setAttribute("aria-label", "Dismiss");
      x.textContent = "×";
      x.addEventListener("click", function () { box.remove(); });
      box.appendChild(x);
      box.appendChild(h);
      box.appendChild(p);
      $("toasts").appendChild(box);
    }

    function opt(value, text) {
      var o = document.createElement("option");
      o.value = value;
      o.textContent = text;
      return o;
    }

    function fixed(v, n) { return Number(v).toFixed(n); }
  }

  HG.designer = { start: start };
})(window.HG);
