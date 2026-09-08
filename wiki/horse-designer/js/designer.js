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

  // Long enough to read a lethal-genotype note, short enough that a run of
  // Randomize clicks does not bury the field in cards.
  var TOAST_LIFE_MS = 10000;

  function start() {
    var scene = HG.designerScene.create($("field"));
    var gui = HG.gui.create($("gui"), {
      edit: edit,
      toggleWander: function () { wander = !wander; refresh(); },
      resetView: function () { if (scene) scene.resetView(); },
      exportJson: exportJson,
      importJson: importJson,
      notShown: notShown
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
      gui.setData(genes, JSON.parse(api.breedsJson()),
        JSON.parse(api.familiesJson()),
        JSON.parse(api.randomizeModesJson()),
        JSON.parse(api.addScopesJson()));
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
      if (anim) anim.reset();       // start beside the reference block
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

    /**
     * The horse as a file - the whole animal, not just its alleles. Two code
     * strings are the load-bearing part; everything else in there is for a
     * person reading it, and a loader must re-resolve traits rather than trust
     * them.
     *
     * <p>This is the page's only way out, and the custom horse spawn egg's Copy
     * horse writes the identical format to the clipboard. Copy code / Paste code
     * used to sit beside these doing a worse version of the same job - a
     * genotype code alone, so the horse came back with fresh epigenetics, no
     * name and no breed - and are gone.
     */
    function exportJson() {
      var json = api.horseJson();
      var name = api.horseFileName();
      var blob = new Blob([json], { type: "application/json" });
      var url = URL.createObjectURL(blob);
      var a = document.createElement("a");
      a.href = url;
      a.download = name;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
      toast("Exported " + name,
        "Genotype and epigenome codes, plus the name, sex, breed and a readable summary — "
        + "the whole horse. <em>Import</em> reads it back, and the custom horse spawn egg's "
        + "<em>Paste horse</em> takes the same text off the clipboard.",
        "info");
    }

    /**
     * Read a horse file back. The whole of it happens in Java - HorseFile.read
     * plus the editor - because the format is the mod's and not the page's; this
     * function picks a file and reports what came back.
     *
     * <p>Deliberately tolerant: a file that names a gene this build does not
     * have loads with that locus dropped (Genotype.parse already does that), a
     * missing gene reads as its wild type, and an unrecognised breed loses only
     * its label. Say what was dropped rather than refusing the horse.
     */
    function importJson() {
      var input = document.createElement("input");
      input.type = "file";
      input.accept = ".json,application/json";
      input.addEventListener("change", function () {
        var file = input.files && input.files[0];
        if (!file) return;
        var reader = new FileReader();
        reader.onload = function () { applyHorseFile(String(reader.result), file.name); };
        reader.readAsText(file);
      });
      input.click();
    }

    function applyHorseFile(text, label) {
      var result = JSON.parse(api.loadHorseJson(text));
      if (!result.ok) {
        toast("That is not a horse file",
          result.error + ".<br><br>A horse file needs a <code>genotype</code> code this "
          + "build can read. Export one to see the shape.", "bad");
        return;
      }
      var notes = [];
      if (result.droppedEpigenome) {
        notes.push("its epigenome did not parse, so this horse has a fresh one "
          + "\u2014 the coat will differ wherever a gene varies per horse");
      }
      if (result.droppedBreed) {
        notes.push("there is no breed by that name in this build, so the label was dropped");
      }
      refresh();
      var st = JSON.parse(api.stateJson());
      toast("Loaded " + (st.first + " " + st.last).trim(),
        "From <code>" + label + "</code>."
        + (notes.length ? " Worth knowing: " + notes.join("; ") + "." : ""),
        notes.length ? "warn" : "info");
    }

    /**
     * A gene the page cannot draw. Java worked out which those are
     * (DesignerApi.showsAs, derived from the gene itself), and there are two
     * honest reasons - so say the right one rather than a vague one.
     */
    var shownUnshowable = {};
    function notShown(i) {
      var gene = genes[i];
      if (shownUnshowable[gene.key]) return;
      shownUnshowable[gene.key] = true;
      if (gene.shows === "ability") {
        toast(gene.name + " is only viewable in game",
          "That gene relies on Minecraft's own assets — particle types, item icons, "
          + "world blocks — so there is nothing here to draw it with. It is still on "
          + "the horse and still in <em>Export</em>; you just cannot see it.", "warn");
      } else {
        toast(gene.name + " changes numbers, not looks",
          "That gene moves the horse's speed, health or jump and nothing else, so there "
          + "is nothing to draw. It is still on the horse, and the figures ride along in "
          + "<em>Export</em>.", "warn");
      }
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
      // Everything here is an aside, not a decision - so nothing waits on being
      // dismissed. Hovering holds it, because a toast that vanishes while you
      // are reading it is worse than one that lingers.
      var life = setTimeout(function () { box.remove(); }, TOAST_LIFE_MS);
      box.addEventListener("pointerenter", function () { clearTimeout(life); });
      box.addEventListener("pointerleave", function () {
        life = setTimeout(function () { box.remove(); }, TOAST_LIFE_MS);
      });
    }
  }

  HG.designer = { start: start };
})(window.HG);
