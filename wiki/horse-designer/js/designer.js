// Wires the GUI to the compiled mod and to the field.
//
// The shape is deliberately thin: the GUI reports a click, this forwards it to
// Java as a method name and some ints, Java answers with new state and a fresh
// coat, and the field draws it. There is no genetics here and there must never
// be any - the moment a rule gets reimplemented in this file, the page starts
// disagreeing with the game, which is the whole thing the wasm build exists to
// prevent.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var $ = function (id) { return document.getElementById(id); };

  function start() {
    var scene = HG.designerScene.create($("field"));
    var gui = HG.gui.create($("gui"), {
      edit: edit,
      copyCode: copyCode,
      pasteCode: pasteCode,
      toggleWander: function () { wander = !wander; refresh(); },
      resetView: function () { if (scene) scene.resetView(); }
    });

    var anim = scene ? HG.designerAnimation.create(scene) : null;
    var wander = true;
    var state = null;
    var genes = [];
    var api = null;

    if (!scene) {
      toast("No WebGL", "The field needs WebGL, which this browser will not give. "
        + "Everything else still works and the coat sheet is a real bake.", "warn");
    }

    HG.java.load().then(function (loaded) {
      api = loaded;
      genes = JSON.parse(api.genesJson());
      gui.setData(genes, JSON.parse(api.breedsJson()));
      $("boot").hidden = true;

      var geomProblem = HG.java.checkGeometry(true) || HG.java.checkGeometry(false);
      if (geomProblem) {
        toast("The 3D mesh disagrees with the game",
          "The coat is still exact - it comes from the compiled Java. But the mesh the "
          + "browser builds is drawn from a JavaScript copy of the geometry tables, and "
          + "that copy has drifted: <em>" + geomProblem + "</em>. Re-run "
          + "<code>check-parity.mjs</code>.", "bad");
      }
      refresh();
      if (scene) {
        scene.onFrame(function (dt) {
          anim.update(dt, { wander: wander, walkSpeed: 1.1, sizeScale: state ? state.scale : 1 });
          var m = scene.measure();
          var f = anim.focusPoint(1, m.height);
          scene.lookAt(f.x, f.y, f.z, false);
          // Draw first: previewRect() is only meaningful once the GUI has
          // measured itself for this frame.
          gui.draw();
          scene.setScreenOffset(guiOffset());
        });
      } else {
        // No field, so nothing is driving the clock - keep the panel alive.
        (function tick() { requestAnimationFrame(tick); gui.draw(); })();
      }
    }).catch(function (err) {
      $("boot").hidden = true;
      toast("Could not start the mod", String(err.message || err)
        + "<br><br>This page loads <code>wasm/web.wasm</code> — the mod itself, compiled. "
        + "It cannot run from a <code>file://</code> path; serve the repo over HTTP, or "
        + "open it on the published wiki. If the file is missing, run "
        + "<code>./gradlew :web:bakeDesignerAssets</code>.", "bad");
    });

    /**
     * How far right of centre the horse should sit, in CSS pixels, so it lands
     * in the gap between the two panels rather than behind the gene list.
     */
    function guiOffset() {
      var r = gui.previewRect();
      return (r.x + r.w / 2) - $("field").clientWidth / 2;
    }

    /** Every edit is the same shape: name a Java method, hand it ints. */
    function edit(method, a, b, c) {
      if (!api) return;
      if (c !== undefined) api[method](a, b, c);
      else if (b !== undefined) api[method](a, b);
      else if (a !== undefined) api[method](a);
      else api[method]();
      refresh();
    }

    /** Pull the new state and the new coat. Java decides both; nothing is cached here. */
    function refresh() {
      if (!api) return;
      state = JSON.parse(api.stateJson());
      state.wander = wander;
      gui.setState(state);

      var pixels = api.coat(!state.baby);
      var n = api.sheetSize();
      var img = new ImageData(n, n);
      for (var i = 0; i < n * n; i++) {
        var p = pixels[i];
        img.data[i * 4] = (p >> 16) & 0xFF;
        img.data[i * 4 + 1] = (p >> 8) & 0xFF;
        img.data[i * 4 + 2] = p & 0xFF;
        img.data[i * 4 + 3] = (p >>> 24) & 0xFF;
      }
      if (scene) {
        scene.setSkin(state.baby ? "BABY" : "ADULT");
        scene.setSize(state.scale);
        scene.setImage(img);
      }
      gui.draw();
      describeConditions();
    }

    /**
     * The one thing the panel has no room for. A lethal genotype is worth
     * saying out loud rather than leaving as a word in a list - it is the
     * difference between "this horse is unusual" and "this foal dies".
     */
    var shownConditions = {};
    function describeConditions() {
      (state.conditions || []).forEach(function (c) {
        if (shownConditions[c.name]) return;
        shownConditions[c.name] = true;
        toast(c.name, c.description + "<br><br><em>" + c.severity.toLowerCase() + "</em>",
          c.severity === "LETHAL_AT_BIRTH" || c.severity === "LETHAL_AT_CONCEPTION" ? "bad" : "warn");
      });
    }

    function copyCode() {
      var code = api.genotypeCode();
      if (navigator.clipboard) {
        navigator.clipboard.writeText(code).then(function () {
          toast("Copied", "The genotype code is on your clipboard. Paste it into the "
            + "custom horse spawn egg in game, or back in here.", "info");
        });
      } else {
        window.prompt("Genotype code", code);
      }
    }

    function pasteCode() {
      var code = window.prompt("Paste a genotype code");
      if (!code) return;
      if (!api.pasteCode(code)) {
        toast("That code did not parse",
          "A genotype code is <code>gene=a/b</code> segments joined by <code>-</code>. "
          + "Parsing is tolerant of missing genes but not of an allele token the gene "
          + "does not have.", "bad");
        return;
      }
      refresh();
    }

    // ---- toasts ----------------------------------------------------------

    function toast(title, html, kind) {
      var box = document.createElement("div");
      box.className = "toast " + (kind || "info");
      var x = document.createElement("button");
      x.type = "button";
      x.className = "toast-close";
      x.setAttribute("aria-label", "Dismiss");
      x.textContent = "×";
      x.addEventListener("click", function () { box.remove(); });
      var h = document.createElement("h4");
      h.textContent = title;
      var p = document.createElement("p");
      p.innerHTML = html;
      box.appendChild(x);
      box.appendChild(h);
      box.appendChild(p);
      $("toasts").appendChild(box);
    }
  }

  HG.designer = { start: start };
})(window.HG);
