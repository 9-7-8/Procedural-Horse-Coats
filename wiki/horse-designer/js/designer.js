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
      console.warn("[horsegenetics] no WebGL in this browser, so there is no horse to spin."
        + " Everything else still works and the coat sheet is a real bake.");
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
        console.error("[horsegenetics] the 3D mesh disagrees with the game: " + geomProblem
          + ". The coat is still exact - it comes from the compiled Java - but the mesh is"
          + " built from a JavaScript copy of the geometry tables and that copy has drifted."
          + " Re-run check-parity.mjs.");
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
      // The boot overlay is already up and is the right place for this: a page
      // whose mod did not load has nothing else to show, so it stays up and
      // says why rather than clearing to an empty field.
      var box = $("boot").querySelector(".boot-inner");
      box.innerHTML = '<h1>Could not start the mod</h1>'
        + '<p>' + escapeHtml(String(err.message || err)) + '</p>'
        + '<p class="sub">This page loads <code>wasm/web.wasm</code> — the mod itself, '
        + 'compiled. It cannot run from a <code>file://</code> path; serve the repo over '
        + 'HTTP, or open it on the published wiki. If the file is missing, run '
        + '<code>./gradlew :web:bakeDesignerAssets</code>.</p>';
      console.error("[horsegenetics] " + (err.stack || err));
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
     * A lethal genotype, said out loud once - to the console.
     *
     * <p>It used to be a card over the field. The cards did not reliably clear
     * themselves and a run of Randomize clicks buried the horse in them, which
     * is a worse failure than the one they were solving: the conditions are
     * <b>already on screen</b>, named on the row of the gene that caused them
     * and drawn in the panel's condition line. This is the debugging copy, not
     * the notification.
     */
    var shownConditions = {};
    function describeConditions() {
      (state.conditions || []).forEach(function (c) {
        if (shownConditions[c.name]) return;
        shownConditions[c.name] = true;
        console.info("[horsegenetics] " + c.name + " (" + c.severity.toLowerCase() + "): "
          + c.description);
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
      console.info("[horsegenetics] exported " + name + " - the whole horse. Import reads it"
        + " back, and the custom horse spawn egg's Paste takes the same text off the"
        + " clipboard.");
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
        // The one place a dialog is right: the reader asked for this file by
        // name a moment ago and nothing on screen will change to tell them it
        // failed.
        window.alert("That is not a horse file.\n\n" + result.error
          + ".\n\nA horse file needs a genotype code this build can read. "
          + "Export one to see the shape.");
        return;
      }
      var notes = [];
      if (result.droppedEpigenome) {
        notes.push("its epigenome did not parse, so this horse has a fresh one - the coat "
          + "will differ wherever a gene varies per horse");
      }
      if (result.droppedBreed) {
        notes.push("there is no breed by that name in this build, so the label was dropped");
      }
      refresh();
      var st = JSON.parse(api.stateJson());
      console.info("[horsegenetics] loaded " + (st.first + " " + st.last).trim()
        + " from " + label + (notes.length ? " - " + notes.join("; ") : ""));
      if (notes.length) {
        window.alert("Loaded " + (st.first + " " + st.last).trim()
          + ", but:\n\n- " + notes.join("\n- "));
      }
    }

    /**
     * A gene the page cannot draw. Java worked out which those are
     * (DesignerApi.showsAs, derived from the gene itself), and there are two
     * honest reasons - so say the right one rather than a vague one.
     *
     * <p>The row already says it, and says it permanently: the name is struck
     * through in the list and reads "not shown here" underneath. This is the
     * console copy of that, kept because the <i>reason</i> does not fit on a
     * row.
     */
    var shownUnshowable = {};
    function notShown(i) {
      var gene = genes[i];
      if (shownUnshowable[gene.key]) return;
      shownUnshowable[gene.key] = true;
      console.info("[horsegenetics] " + gene.name + (gene.shows === "ability"
        ? " is only viewable in game: it relies on Minecraft's own assets - particle types,"
          + " item icons, world blocks - so there is nothing here to draw it with."
        : " changes numbers, not looks: it moves speed, health or jump and nothing else.")
        + " It is still on the horse and still in Export.");
    }

    // ---- saying things --------------------------------------------------
    //
    // There is no toast system any more. There was: a stack of dismissible
    // cards over the field, on a timer. The timer did not reliably fire - a
    // pointer resting anywhere over the stack held every card in it - so a run
    // of Randomize clicks buried the horse behind the thing you were trying to
    // look at, which is the opposite of what a notification is for.
    //
    // What replaced it is not a smaller toast. Everything the cards said is
    // either already permanent on screen (a struck-through gene row, the
    // condition line under the panel) or is a developer's note, and a
    // developer's note belongs in the console. The two exceptions are here:
    // a failure to boot, which owns the boot overlay because the page has
    // nothing else to show, and a file the reader just chose that would not
    // load, which gets a dialog because nothing on screen would otherwise
    // change.

    function escapeHtml(s) {
      return String(s).replace(/[&<>"]/g, function (c) {
        return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
      });
    }
  }

  HG.designer = { start: start };
})(window.HG);
