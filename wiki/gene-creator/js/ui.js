// The editor. Every form in here is generated from js/schema.js, which mirrors
// the game's SpecSchema - so the tool cannot offer a setting the game ignores,
// and a new mask or op appears in the UI the moment it appears in the schema.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var schema = HG.schema;
  var model = HG.specModel;

  var state = {
    spec: model.blank(),
    skin: "ADULT",
    baseCoatId: "bay",
    seed: 7,
    dose: 1,
    coverageLayer: -1,
    selectedLayer: 0,
    viewport: null,
    lastBake: null
  };

  // ---- tiny DOM helpers ------------------------------------------------

  function el(tag, attrs, children) {
    var node = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      if (k === "class") node.className = attrs[k];
      else if (k === "text") node.textContent = attrs[k];
      else if (k === "html") node.innerHTML = attrs[k];
      else if (k.slice(0, 2) === "on") node.addEventListener(k.slice(2), attrs[k]);
      else node.setAttribute(k, attrs[k]);
    });
    (children || []).forEach(function (c) { if (c) node.appendChild(c); });
    return node;
  }

  function $(id) { return document.getElementById(id); }

  function field(label, control, hint) {
    return el("label", { class: "field" }, [
      el("span", { class: "field-label", text: label }),
      control,
      hint ? el("span", { class: "hint", text: hint }) : null
    ]);
  }

  function select(options, value, onChange) {
    var s = el("select", { onchange: function () { onChange(s.value); } });
    options.forEach(function (o) {
      var opt = el("option", { value: o.value !== undefined ? o.value : o, text: o.label || o });
      s.appendChild(opt);
    });
    s.value = value;
    return s;
  }

  function number(value, onChange, attrs) {
    var input = el("input", Object.assign({ type: "number", value: value }, attrs || {}));
    input.addEventListener("input", function () { onChange(input.value === "" ? "" : Number(input.value)); });
    return input;
  }

  function checkbox(value, onChange) {
    var input = el("input", { type: "checkbox" });
    input.checked = !!value;
    input.addEventListener("change", function () { onChange(input.checked); });
    return input;
  }

  /** e.g. "My/my" - the combination for a copy count, for a hint line. */
  function combinationLabel(spec, dose) {
    return HG.specEngine.combinationForDose(spec, dose);
  }

  function text(value, onChange) {
    var input = el("input", { type: "text", value: value || "" });
    input.addEventListener("input", function () { onChange(input.value); });
    return input;
  }

  function button(label, onClick, cls) {
    return el("button", { type: "button", class: cls || "btn", text: label, onclick: onClick });
  }

  // ---- value editor ----------------------------------------------------
  //
  // Every numeric parameter can be four things. Rather than hide that, the
  // editor names them: Fixed, Knob, Random and Per dose. "Random" is the one
  // that makes a gene look alive - and it writes an inline knob, which is
  // exactly what the file format calls it.

  function valueMode(v) {
    if (typeof v === "string" && v.charAt(0) === "$") return "knob";
    if (v && typeof v === "object" && v.perDose) return "dose";
    if (v && typeof v === "object" && v.min !== undefined) return "random";
    return "fixed";
  }

  function valueEditor(owner, key, param, onChange) {
    var start = param.initial === undefined ? param.fallback : param.initial;
    var current = owner[key];
    if (current === undefined) current = start;
    var mode = valueMode(current);
    var ui = param.ui || { min: 0, max: 1, step: 0.01 };
    var wrap = el("div", { class: "value-editor" });

    var modes = [{ value: "fixed", label: "Fixed" }, { value: "random", label: "Random" },
      { value: "dose", label: "Per dose" }];
    if (state.spec.knobs.length) modes.splice(1, 0, { value: "knob", label: "Knob" });

    wrap.appendChild(select(modes, mode, function (m) {
      if (m === "fixed") owner[key] = typeof current === "number" ? current : start;
      else if (m === "random") owner[key] = { min: ui.min === undefined ? 0 : ui.min, max: start, per: "horse", spread: 0 };
      else if (m === "dose") owner[key] = { perDose: [0, start, start] };
      else owner[key] = "$" + state.spec.knobs[0].name;
      onChange();
    }));

    if (mode === "fixed") {
      var slider = el("input", {
        type: "range", min: ui.min, max: ui.max, step: ui.step,
        value: typeof current === "number" ? current : start
      });
      var box = number(typeof current === "number" ? current : start, function (v) {
        owner[key] = v;
        slider.value = v;
        onChange();
      }, { step: ui.step, class: "narrow" });
      slider.addEventListener("input", function () {
        owner[key] = Number(slider.value);
        box.value = slider.value;
        onChange();
      });
      wrap.appendChild(slider);
      wrap.appendChild(box);
    } else if (mode === "knob") {
      var seedOnly = !!(param.ui && param.ui.seedRef);
      var usable = state.spec.knobs.filter(function (k) {
        return seedOnly ? k.type === "seed" : k.type !== "seed";
      });
      if (!usable.length) {
        wrap.appendChild(el("span", {
          class: "warn",
          text: seedOnly ? "add a seed knob first" : "add a knob first"
        }));
      } else {
        wrap.appendChild(select(usable.map(function (k) { return { value: "$" + k.name, label: k.name }; }),
          current, function (v) { owner[key] = v; onChange(); }));
      }
    } else if (mode === "random") {
      var r = current;
      wrap.appendChild(el("span", { class: "mini", text: "min" }));
      wrap.appendChild(number(r.min, function (v) { r.min = v; onChange(); }, { step: ui.step, class: "narrow" }));
      wrap.appendChild(el("span", { class: "mini", text: "max" }));
      wrap.appendChild(number(r.max, function (v) { r.max = v; onChange(); }, { step: ui.step, class: "narrow" }));
      wrap.appendChild(select([{ value: "horse", label: "per horse" }, { value: "leg", label: "per leg" }],
        r.per || "horse", function (v) { r.per = v; onChange(); }));
      if (r.per === "leg") {
        wrap.appendChild(el("span", { class: "mini", text: "spread" }));
        wrap.appendChild(number(r.spread || 0, function (v) { r.spread = v; onChange(); },
          { step: 0.01, min: 0, max: 1, class: "narrow" }));
      }
    } else {
      var d = current.perDose;
      ["0 copies", "1 copy", "2 copies"].forEach(function (label, i) {
        wrap.appendChild(el("span", { class: "mini", text: label }));
        wrap.appendChild(number(d[i], function (v) { d[i] = v; onChange(); },
          { step: ui.step, class: "narrow" }));
      });
    }
    return wrap;
  }

  function partsEditor(owner, onChange) {
    var chosen = owner.parts || [];
    var wrap = el("div", { class: "parts-editor" });
    var summary = el("div", {
      class: "parts-summary",
      text: chosen.length ? chosen.join(", ") : "whole horse"
    });
    wrap.appendChild(summary);

    var grid = el("div", { class: "parts-grid" });
    schema.GROUP_NAMES.concat(schema.PART_NAMES).forEach(function (name) {
      var on = chosen.indexOf(name) >= 0;
      grid.appendChild(el("button", {
        type: "button",
        class: "chip" + (on ? " on" : "") + (schema.GROUP_NAMES.indexOf(name) >= 0 ? " group" : ""),
        text: name.toLowerCase().replace(/_/g, " "),
        onclick: function () {
          var i = chosen.indexOf(name);
          if (i >= 0) chosen.splice(i, 1); else chosen.push(name);
          owner.parts = chosen;
          onChange();
        }
      }));
    });
    wrap.appendChild(grid);
    return wrap;
  }

  // ---- panels ----------------------------------------------------------

  function renderGenePanel() {
    var spec = state.spec;
    var root = $("gene-panel");
    root.innerHTML = "";

    root.appendChild(field("Gene key", text(spec.key, function (v) { spec.key = v; changed(); }),
      "modid.gene - unique, lower case"));
    root.appendChild(field("Display name", text(spec.name, function (v) { spec.name = v; changed(); })));

    root.appendChild(field("Phase", select([
      { value: "natural", label: "Natural - moves pigment" },
      { value: "magical", label: "Magical - adds colour" }
    ], spec.phase, function (v) {
      spec.phase = v;
      // Ops belong to one phase; carry each layer over to that phase's default.
      model.layersOf(spec).forEach(function (layer) {
        if (schema.OPS[layer.op.type].phase !== v) {
          layer.op = model.newOp(v === "magical" ? "TINT" : "SET_PIGMENT");
        }
      });
      changed();
    }), spec.phase === "natural"
      ? "Pushes red / black pigment down before the coat resolves. Dilutions and white markings."
      : "Adds signed RGB after the coat resolves. Paints over anything, including dominant white."));

    root.appendChild(field("Priority",
      number(spec.priority, function (v) { spec.priority = v; changed(); }, { step: 1 }),
      "lower runs earlier, among drop-in genes"));

    // ---- what this gene does, and to whom -------------------------------
    //
    // There is no "dominance" setting, because there is no such property. A
    // gene has alleles, and every combination of two of them produces some
    // outcome; which combinations share an outcome is the whole of what the
    // classical words meant. So this asks the question directly.
    var expression = model.visible(spec);

    root.appendChild(el("h3", { text: "What it does" }));
    root.appendChild(el("p", {
      class: "hint",
      text: "One outcome, and the allele combinations that produce it. The "
        + "description is what the gene dictionary and the wiki show."
    }));
    root.appendChild(field("Outcome name",
      text(expression.name, function (v) { expression.name = v; changed(); })));
    root.appendChild(field("Description",
      text(expression.description || "", function (v) { expression.description = v; changed(); }),
      "one sentence: what a horse carrying this looks like"));

    root.appendChild(field("Shows when the horse has", select([
      { value: "any", label: "one copy or two" },
      { value: "homozygous", label: "two copies only" }
    ], model.showsWhen(spec), function (v) { model.setShowsWhen(spec, v); changed(); }),
      model.showsWhen(spec) === "homozygous"
        ? "A single copy is an invisible carrier - " + combinationLabel(spec, 1)
          + " lands on the wild type."
        : "Both " + combinationLabel(spec, 2) + " and " + combinationLabel(spec, 1)
          + " land on this outcome."));

    root.appendChild(field("Masks every other gene",
      checkbox(!!expression.masks, function (v) { expression.masks = v || undefined; changed(); }),
      "while this shows, no other gene is visible - dominant white does this"));

    root.appendChild(el("h3", { text: "Founder population" }));
    root.appendChild(el("p", {
      class: "hint",
      text: "The share of wild horses carrying each combination. Declared per "
        + "combination, not per allele, so you set the rare-homozygote rate "
        + "yourself. Should add up to 100%."
    }));
    var total = 0;
    model.combinations(spec).forEach(function (c) {
      total += Number((spec.founders || {})[c]) || 0;
      root.appendChild(field(c, number((spec.founders || {})[c] || 0, function (v) {
        spec.founders = spec.founders || {};
        spec.founders[c] = Number(v);
        changed();
      }, { min: 0, step: 0.001 }), "% of founders"));
    });
    root.appendChild(el("p", {
      class: Math.abs(total - 100) > 0.01 ? "hint warn" : "hint",
      text: "Total: " + (Math.round(total * 1000) / 1000) + "%"
    }));

    root.appendChild(el("h3", { text: "Alleles" }));
    root.appendChild(el("p", {
      class: "hint",
      text: "Variant first, population baseline last. The format allows any number "
        + "of alleles and an outcome per combination, but this editor authors one "
        + "visible outcome on a two-allele gene - hand-edit the JSON for more."
    }));
    spec.alleles.forEach(function (a, i) {
      var row = el("div", { class: "row" }, [
        text(a.token, function (v) {
          // Renaming an allele has to carry the combination table and the
          // founder shares with it, or both quietly point at a token that no
          // longer exists and the game refuses the file.
          var before = spec.alleles.map(function (x) { return x.token; });
          a.token = v;
          model.retoken(spec, before);
          changed();
        }),
        text(a.label, function (v) { a.label = v; changed(); }),
        el("span", { class: "tag", text: i === spec.alleles.length - 1 ? "baseline" : "variant" })
      ]);
      root.appendChild(row);
    });

    root.appendChild(el("h3", { text: "Knobs" }));
    root.appendChild(el("p", {
      class: "hint",
      text: "Numbers each horse draws once from the allele copy it inherited - "
        + "the reason two horses with the same gene do not look identical."
    }));
    spec.knobs.forEach(function (k, i) {
      var row = el("div", { class: "knob" });
      row.appendChild(el("div", { class: "row" }, [
        text(k.name, function (v) { k.name = v; changed(); }),
        el("span", { class: "tag", text: k.type === "seed" ? "seed" : "range" }),
        button("remove", function () {
          var uses = model.knobUses(spec, k.name);
          if (uses && !window.confirm("\"" + k.name + "\" is used by " + uses
            + " setting(s). Remove it anyway?")) return;
          spec.knobs.splice(i, 1);
          changed();
        }, "btn small danger")
      ]));
      if (k.type !== "seed") {
        row.appendChild(el("div", { class: "row" }, [
          el("span", { class: "mini", text: "min" }),
          number(k.min, function (v) { k.min = v; changed(); }, { step: 0.01, class: "narrow" }),
          el("span", { class: "mini", text: "max" }),
          number(k.max, function (v) { k.max = v; changed(); }, { step: 0.01, class: "narrow" }),
          select([{ value: "horse", label: "per horse" }, { value: "leg", label: "per leg" }],
            k.per || "horse", function (v) { k.per = v; changed(); }),
          k.per === "leg" ? el("span", { class: "mini", text: "spread" }) : null,
          k.per === "leg" ? number(k.spread || 0, function (v) { k.spread = v; changed(); },
            { step: 0.01, class: "narrow" }) : null
        ]));
      }
      root.appendChild(row);
    });
    root.appendChild(el("div", { class: "row" }, [
      button("+ range knob", function () { spec.knobs.push(model.newKnob(spec, "range")); changed(); }, "btn small"),
      button("+ seed knob", function () { spec.knobs.push(model.newKnob(spec, "seed")); changed(); }, "btn small")
    ]));
  }

  function renderLayers() {
    var spec = state.spec;
    var layers = model.layersOf(spec);
    var root = $("layers-panel");
    root.innerHTML = "";

    layers.forEach(function (layer, li) {
      var open = li === state.selectedLayer;
      var card = el("div", { class: "layer" + (open ? " open" : "") });

      card.appendChild(el("div", { class: "layer-head" }, [
        el("button", {
          type: "button", class: "layer-title", text: (li + 1) + ". " + (layer.name || "layer"),
          onclick: function () { state.selectedLayer = open ? -1 : li; renderLayers(); }
        }),
        el("label", { class: "cover-toggle", title: "Highlight what this layer covers" }, [
          (function () {
            var cb = el("input", { type: "checkbox" });
            cb.checked = state.coverageLayer === li;
            cb.addEventListener("change", function () {
              state.coverageLayer = cb.checked ? li : -1;
              renderLayers();
              rebake();
            });
            return cb;
          })(),
          el("span", { text: "show area" })
        ]),
        button("↑", function () { move(layers, li, -1); }, "btn tiny"),
        button("↓", function () { move(layers, li, 1); }, "btn tiny"),
        button("✕", function () {
          layers.splice(li, 1);
          if (state.coverageLayer >= layers.length) state.coverageLayer = -1;
          state.selectedLayer = Math.min(state.selectedLayer, layers.length - 1);
          changed();
        }, "btn tiny danger")
      ]));

      if (open) {
        var body = el("div", { class: "layer-body" });
        body.appendChild(field("Layer name", text(layer.name, function (v) { layer.name = v; changed(); })));

        body.appendChild(el("h4", { text: "Where" }));
        layer.masks.forEach(function (mask, mi) {
          body.appendChild(maskCard(layer, mask, mi));
        });
        var addMask = [{ value: "", label: "+ add a mask…" }].concat(
          Object.keys(schema.MASKS).map(function (t) { return { value: t, label: t.toLowerCase() }; }));
        body.appendChild(el("div", { class: "row" }, [
          select(addMask, "", function (t) {
            if (!t) return;
            layer.masks.push(model.newMask(t));
            changed();
          })
        ]));

        body.appendChild(el("h4", { text: "What" }));
        var opTypes = schema.opsForPhase(spec.phase);
        body.appendChild(field("Effect", select(opTypes, layer.op.type, function (t) {
          layer.op = model.newOp(t);
          changed();
        }), schema.OPS[layer.op.type].blurb));
        schema.OPS[layer.op.type].params.forEach(function (p) {
          body.appendChild(opParamRow(layer.op, p));
        });
        card.appendChild(body);
      }
      root.appendChild(card);
    });

    root.appendChild(button("+ add layer", function () {
      layers.push(model.newLayer(spec.phase));
      state.selectedLayer = layers.length - 1;
      changed();
    }, "btn"));
  }

  function maskCard(layer, mask, mi) {
    var card = el("div", { class: "mask" });
    card.appendChild(el("div", { class: "row mask-head" }, [
      select(Object.keys(schema.MASKS), mask.type, function (t) {
        layer.masks[mi] = model.newMask(t);
        changed();
      }),
      mi > 0 ? select(schema.COMBINES, mask.combine || "MULTIPLY",
        function (v) { mask.combine = v; changed(); }) : null,
      (function () {
        var l = el("label", { class: "cover-toggle", title: "Use everywhere this mask does NOT cover" });
        var cb = el("input", { type: "checkbox" });
        cb.checked = !!mask.invert;
        cb.addEventListener("change", function () { mask.invert = cb.checked; changed(); });
        l.appendChild(cb);
        l.appendChild(el("span", { text: "invert" }));
        return l;
      })(),
      button("✕", function () { layer.masks.splice(mi, 1); changed(); }, "btn tiny danger")
    ]));
    card.appendChild(el("p", { class: "hint", text: schema.MASKS[mask.type].blurb }));

    schema.MASKS[mask.type].params.forEach(function (p) {
      if (p.kind === "PARTS") {
        card.appendChild(field(p.name, partsEditor(mask, changed), p.doc));
      } else if (p.kind === "CHOICE") {
        card.appendChild(field(p.name, select(p.choices, mask[p.name] || p.fallback,
          function (v) { mask[p.name] = v; changed(); }), p.doc));
      } else {
        card.appendChild(field(p.name, valueEditor(mask, p.name, p, changed), p.doc));
      }
    });
    return card;
  }

  function opParamRow(op, p) {
    if (p.kind === "COLOR") {
      var input = el("input", { type: "color", value: op[p.name] || "#ffffff" });
      input.addEventListener("input", function () { op[p.name] = input.value; changed(); });
      return field(p.name, input, p.doc);
    }
    return field(p.name, valueEditor(op, p.name, p, changed), p.doc);
  }

  function move(list, index, delta) {
    var to = index + delta;
    if (to < 0 || to >= list.length) return;
    var item = list.splice(index, 1)[0];
    list.splice(to, 0, item);
    state.selectedLayer = to;
    changed();
  }

  // ---- preview + export ------------------------------------------------

  var rebakeTimer = null;

  function changed() {
    renderGenePanel();
    renderLayers();
    renderExport();
    rebake();
  }

  function rebake() {
    if (rebakeTimer) clearTimeout(rebakeTimer);
    rebakeTimer = setTimeout(doBake, 40);
  }

  function doBake() {
    var canvas = $("sheet-canvas");
    var ctx = canvas.getContext("2d");
    var t0 = performance.now();
    var result;
    try {
      result = HG.preview.bake({
        spec: state.spec,
        skin: state.skin,
        baseCoatId: state.baseCoatId,
        seed: state.seed,
        dose: state.dose,
        coverageLayer: state.coverageLayer
      });
    } catch (e) {
      $("bake-status").textContent = "preview failed: " + e.message;
      $("bake-status").className = "status bad";
      return;
    }
    state.lastBake = result;

    var img = HG.preview.toImageData(ctx, result.pixels, result.coverage);
    ctx.putImageData(img, 0, 0);
    if (state.viewport) {
      state.viewport.setSkin(state.skin);
      state.viewport.setImage(img);
    }

    var ms = Math.round(performance.now() - t0);
    var note = result.expresses ? "" : " - this gene does not express at " + state.dose
      + " copy/copies, so you are seeing the base coat";
    $("bake-status").textContent = "baked in " + ms + " ms" + note;
    $("bake-status").className = "status" + (result.expresses ? "" : " warnish");
    renderDrawnKnobs(result.values);
  }

  function renderDrawnKnobs(values) {
    var root = $("drawn-knobs");
    root.innerHTML = "";
    if (!state.spec.knobs.length) {
      root.appendChild(el("span", { class: "hint", text: "no knobs - every carrier looks the same" }));
      return;
    }
    state.spec.knobs.forEach(function (knob, i) {
      var shown = knob.type === "seed" ? "(seed)"
        : values.ranges[i].map(function (v) { return v.toFixed(3); }).join(" / ");
      root.appendChild(el("div", { class: "drawn" }, [
        el("span", { class: "drawn-name", text: knob.name }),
        el("span", { class: "drawn-value", text: shown })
      ]));
    });
  }

  function renderExport() {
    var json = model.toJson(state.spec);
    $("json-output").value = json;
    $("export-filename").textContent = model.fileName(state.spec);

    var issues = model.problems(state.spec);
    var box = $("problems");
    box.innerHTML = "";
    if (!issues.length) {
      box.appendChild(el("div", { class: "ok", text: "Ready to export." }));
    } else {
      issues.forEach(function (p) { box.appendChild(el("div", { class: "problem", text: p })); });
    }
  }

  // ---- toolbar ---------------------------------------------------------

  function renderToolbar() {
    var bar = $("preview-controls");
    bar.innerHTML = "";

    bar.appendChild(field("Base coat", select(
      HG.baseCoats.presets.map(function (p) { return { value: p.id, label: p.label }; }),
      state.baseCoatId, function (v) { state.baseCoatId = v; rebake(); })));

    bar.appendChild(field("Age", select([
      { value: "ADULT", label: "Adult" }, { value: "BABY", label: "Foal" }
    ], state.skin, function (v) { state.skin = v; rebake(); })));

    bar.appendChild(field("Copies", select([
      { value: "0", label: "0 (wild type)" }, { value: "1", label: "1 copy" }, { value: "2", label: "2 copies" }
    ], String(state.dose), function (v) { state.dose = Number(v); rebake(); })));

    var seedBox = number(state.seed, function (v) { state.seed = v || 0; rebake(); }, { step: 1, class: "narrow" });
    bar.appendChild(field("Horse", el("div", { class: "row" }, [
      seedBox,
      button("roll", function () {
        state.seed = Math.floor(Math.random() * 100000);
        seedBox.value = state.seed;
        rebake();
      }, "btn small")
    ]), "a different horse carrying the same gene"));
  }

  // ---- import / export actions ----------------------------------------

  function loadSpec(spec) {
    state.spec = spec;
    state.selectedLayer = 0;
    state.coverageLayer = -1;
    changed();
  }

  function wireActions() {
    $("btn-copy").addEventListener("click", function () {
      var ta = $("json-output");
      navigator.clipboard.writeText(ta.value).then(function () {
        flash($("btn-copy"), "Copied");
      }, function () {
        ta.select();
        document.execCommand("copy");
        flash($("btn-copy"), "Copied");
      });
    });

    $("btn-download").addEventListener("click", function () {
      var blob = new Blob([$("json-output").value], { type: "application/json" });
      var a = el("a", { href: URL.createObjectURL(blob), download: model.fileName(state.spec) });
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
    });

    $("btn-new").addEventListener("click", function () {
      if (window.confirm("Start a new gene? The current one is not saved.")) loadSpec(model.blank());
    });

    $("file-input").addEventListener("change", function (e) {
      var file = e.target.files[0];
      if (!file) return;
      var reader = new FileReader();
      reader.onload = function () {
        try {
          loadSpec(normalise(JSON.parse(reader.result)));
        } catch (err) {
          window.alert("Could not read that file: " + err.message);
        }
      };
      reader.readAsText(file);
      e.target.value = "";
    });

    $("btn-paste").addEventListener("click", function () {
      var raw = window.prompt("Paste a gene's JSON:");
      if (!raw) return;
      try {
        loadSpec(normalise(JSON.parse(raw)));
      } catch (err) {
        window.alert("Could not read that JSON: " + err.message);
      }
    });

    var examples = $("example-picker");
    Object.keys(HG.examples).forEach(function (name) {
      examples.appendChild(el("option", { value: name, text: name }));
    });
    examples.addEventListener("change", function () {
      if (!examples.value) return;
      loadSpec(normalise(JSON.parse(JSON.stringify(HG.examples[examples.value]))));
      examples.value = "";
    });
  }

  /** Fill in what the file left out, so the forms have something to bind to. */
  function normalise(spec) {
    var out = Object.assign(model.blank(), spec);
    out.knobs = (spec.knobs || []).map(function (k) {
      return Object.assign({ per: "horse", spread: 0 }, k);
    });
    out.expressions = (spec.expressions || []).map(function (e) {
      var entry = Object.assign({ description: "" }, e);
      entry.layers = (e.layers || []).map(function (layer) {
        return {
          name: layer.name || "layer",
          masks: (layer.masks || []).map(function (m) { return Object.assign({}, m); }),
          op: Object.assign({}, layer.op)
        };
      });
      return entry;
    });
    // The forms need one visible outcome and one wild type to bind to; a file
    // with more than that still previews, it just cannot be fully edited here.
    if (!model.visible(out)) {
      out.expressions.unshift(model.blank().expressions[0]);
    }
    if (!model.wild(out)) {
      out.expressions.push({ id: "wild", name: "Wild type", description: "No effect.", wildType: true });
    }
    if (!model.layersOf(out).length) {
      model.visible(out).layers = [model.newLayer(out.phase)];
    }
    out.founders = Object.assign({}, spec.founders || model.blank().founders);
    return out;
  }

  function flash(btn, label) {
    var was = btn.textContent;
    btn.textContent = label;
    setTimeout(function () { btn.textContent = was; }, 1200);
  }

  // ---- click-to-inspect ------------------------------------------------

  function onPick(sample, px, py) {
    var p = sample.point;
    var bake = state.lastBake;
    var lines = [
      sample.part.toLowerCase().replace(/_/g, " ") + " · " + sample.face.toLowerCase() + " face",
      "body (x " + p.x.toFixed(2) + ", y " + p.y.toFixed(2) + ", z " + p.z.toFixed(2) + ")",
      "texel " + px + ", " + py
    ];
    if (bake) {
      lines.push("pigment red " + bake.pigment.redAt(px, py).toFixed(3)
        + " · black " + bake.pigment.blackAt(px, py).toFixed(3));
      if (bake.coverage) lines.push("layer coverage " + bake.coverage[py * 128 + px].toFixed(3));
    }
    $("pick-readout").innerHTML = "";
    lines.forEach(function (l) { $("pick-readout").appendChild(el("div", { text: l })); });

    var bounds = HG.geometry.bounds(state.skin, sample.part);
    if (bounds) {
      var t = (p.y - bounds.yMin) / bounds.span("Y");
      $("pick-readout").appendChild(el("div", {
        class: "hint",
        text: "that is " + Math.round(t * 100) + "% up this part - an AXIS mask in 'part' space "
          + "with to = " + t.toFixed(2) + " would stop here"
      }));
    }
  }

  // ---- boot ------------------------------------------------------------

  function start() {
    renderToolbar();
    wireActions();
    HG.preview.load(function () {
      state.viewport = HG.viewport.create($("viewport"), { onPick: onPick });
      if (!state.viewport) {
        $("viewport").appendChild(el("div", {
          class: "no-3d",
          text: "three.js did not load, so there is no 3D view. The flat sheet below is "
            + "still the real bake; reload with a connection for the 3D horse."
        }));
      }
      changed();
    });
  }

  HG.ui = { start: start, state: state };
})(window.HG);
