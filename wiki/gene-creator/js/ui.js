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

  function textarea(value, onChange, rows) {
    var box = el("textarea", { rows: rows || 3, spellcheck: "false" });
    box.value = value || "";
    box.addEventListener("input", function () { onChange(box.value); });
    return box;
  }

  /**
   * An editable list of strings - one row each, plus an add button. Used for
   * the carrot's flavour ingredients, which is a list of item ids.
   */
  function stringList(list, placeholder, onChange) {
    var wrap = el("div", { class: "stack" });
    list.forEach(function (item, i) {
      var input = el("input", { type: "text", value: item || "", placeholder: placeholder });
      input.addEventListener("input", function () { list[i] = input.value; onChange(); });
      wrap.appendChild(el("div", { class: "row" }, [
        input,
        button("x", function () { list.splice(i, 1); onChange(); }, "btn small danger")
      ]));
    });
    wrap.appendChild(button("+ add", function () { list.push(""); onChange(); }, "btn small"));
    return wrap;
  }

  // ---- keeping the caret ------------------------------------------------
  //
  // Every edit re-renders both panels from scratch, which destroys the input
  // the user is typing into - so a text field used to accept exactly one
  // character per click. Rather than make the render incremental, remember
  // where the caret was by its position in the panel's own tree and put it
  // back: a pure text edit does not change that tree's shape, so the path
  // still names the same field.

  function focusPath(root) {
    var active = document.activeElement;
    if (!root || !active || !root.contains(active)) return null;
    var path = [], node = active;
    while (node && node !== root) {
      path.unshift(Array.prototype.indexOf.call(node.parentNode.childNodes, node));
      node = node.parentNode;
    }
    if (node !== root) return null;
    var start = null, end = null;
    // A number input throws on selectionStart in some browsers; the caret in
    // one is not worth a broken render.
    try { start = active.selectionStart; end = active.selectionEnd; } catch (e) { /* not text */ }
    return { path: path, start: start, end: end };
  }

  function restoreFocus(root, saved) {
    if (!root || !saved) return;
    var node = root;
    for (var i = 0; i < saved.path.length; i++) {
      node = node.childNodes[saved.path[i]];
      if (!node) return;
    }
    if (typeof node.focus !== "function") return;
    node.focus();
    if (saved.start === null || !node.setSelectionRange) return;
    try { node.setSelectionRange(saved.start, saved.end); } catch (e) { /* not text */ }
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

    renderGameplay(root, spec);

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

  /**
   * The format-3 gameplay metadata: what the gene database says about this
   * gene, how rare it is, and what the two splice carrots do with it. All of it
   * is optional - the export drops anything still at its default - but it used
   * to be hand-written JSON only, which is why a drop-in gene shipped without
   * a blurb and at whatever rarity the game assumed.
   */
  function renderGameplay(root, spec) {
    root.appendChild(el("h3", { text: "Gameplay" }));
    root.appendChild(el("p", {
      class: "hint",
      text: "Everything here is optional and everything here has a sensible "
        + "default, so a gene that ignores it still exports a short file."
    }));

    root.appendChild(field("Blurb",
      textarea(spec.blurb || "", function (v) { spec.blurb = v; changed(); }, 3),
      "a paragraph the Horse Browser's gene database shows once the gene is discovered"));

    root.appendChild(field("Rarity", select(
      model.RARITIES.map(function (r) {
        return { value: r, label: r.charAt(0).toUpperCase() + r.slice(1)
          + (r === model.DEFAULT_RARITY ? " (default)" : "") };
      }), spec.rarity || model.DEFAULT_RARITY,
      function (v) { spec.rarity = v; changed(); }),
      "sets the gene carrot's rarity ingredient and how often a research paper "
        + "for it turns up in loot"));

    // ---- the Known Gene Splice carrot ----------------------------------
    var carrot = spec.carrot = spec.carrot || model.defaultCarrot();
    root.appendChild(field("Has a gene carrot",
      checkbox(carrot.enabled !== false, function (v) { carrot.enabled = v; changed(); }),
      "off means the Known Gene Splice carrot cannot be made for this gene at all"));

    if (carrot.enabled !== false) {
      root.appendChild(field("The carrot gives", select([
        { value: "heterozygous", label: "one copy" },
        { value: "homozygous", label: "two copies" }
      ], carrot.behaviour || "heterozygous", function (v) { carrot.behaviour = v; changed(); }),
        carrot.behaviour === "homozygous"
          ? "a foal fed this gets the gene expressing outright"
          : "a foal fed this becomes a carrier"));

      carrot.flavour = carrot.flavour || [];
      root.appendChild(field("Extra recipe items",
        stringList(carrot.flavour, "minecraft:sugar", changed),
        "item ids added to this gene's carrot recipe on top of the four the "
          + "recipe always takes; leave empty for none"));
    }

    // ---- the Unknown Gene Splice carrot --------------------------------
    //
    // Absent means "draw uniformly over every combination this gene can have",
    // which for a gene with a nasty homozygote is not what you want.
    var hasSplice = !!spec.splice;
    root.appendChild(field("Own splice distribution",
      checkbox(hasSplice, function (v) {
        if (!v) { spec.splice = null; changed(); return; }
        // Start from the founder shares: the honest first guess, and it makes
        // the shape of the table obvious.
        var seeded = {};
        model.combinations(spec).forEach(function (c) {
          seeded[c] = Number((spec.founders || {})[c]) || 0;
        });
        spec.splice = seeded;
        changed();
      }),
      hasSplice
        ? "what the Unknown Gene Splice carrot rolls when it lands on this gene"
        : "off means the random splice draws evenly over every combination this "
          + "gene can carry"));

    if (hasSplice) {
      var spliceTotal = 0;
      model.combinations(spec).forEach(function (c) {
        spliceTotal += Number(spec.splice[c]) || 0;
        root.appendChild(field(c, number(spec.splice[c] || 0, function (v) {
          spec.splice[c] = Number(v);
          changed();
        }, { min: 0, step: 0.001 }), "% of splices"));
      });
      root.appendChild(el("p", {
        class: spliceTotal > 0 ? "hint" : "hint warn",
        text: spliceTotal > 0
          ? "Total: " + (Math.round(spliceTotal * 1000) / 1000) + "%"
          : "Every share is zero - the game will refuse the file."
      }));
    }
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

  // ---- effects ----------------------------------------------------------

  function renderEffects() {
    var spec = state.spec;
    var root = $("effects-panel");
    root.innerHTML = "";
    var effects = model.effectsOf(spec);

    effects.forEach(function (effect, i) {
      var def = schema.EFFECTS[effect.type];
      var card = el("div", { class: "layer" });
      card.appendChild(el("div", { class: "layer-head" }, [
        el("strong", { text: effect.type }),
        button("x", function () { effects.splice(i, 1); changed(); }, "btn small danger")
      ]));
      if (def) card.appendChild(el("p", { class: "hint", text: def.doc }));

      card.appendChild(field("Effect", select(
        Object.keys(schema.EFFECTS).map(function (v) { return { value: v, label: v }; }),
        effect.type, function (v) {
          if (v === effect.type) return;
          effects[i] = model.newEffect(v);
          changed();
        })));

      (def ? def.params : []).forEach(function (p) {
        card.appendChild(effectParamRow(effect, p));
      });

      card.appendChild(field("Needs two copies",
        checkbox(Number(effect.minDose) === 2, function (v) {
          effect.minDose = v ? 2 : 1;
          changed();
        }),
        "off means any expressing copy grants it"));

      card.appendChild(conditionEditor(effect));
      root.appendChild(card);
    });

    var picker = select(
      [{ value: "", label: "+ add an effect…" }].concat(
        Object.keys(schema.EFFECTS).map(function (v) { return { value: v, label: v }; })),
      "", function (v) {
        if (!v) return;
        effects.push(model.newEffect(v));
        changed();
      });
    root.appendChild(picker);
  }

  function effectParamRow(effect, p) {
    if (p.kind === "PARTS") {
      return field(p.name, partsEditor(effect, changed), p.doc);
    }
    if (p.kind === "TRIGGER") {
      return triggerEditor(effect, p);
    }
    if (p.kind === "CHOICE") {
      return field(p.name, select(p.choices, effect[p.name] === undefined ? p.fallback : effect[p.name],
        function (v) { effect[p.name] = v; changed(); }), p.doc);
    }
    if (p.kind === "COLOR") {
      var picker = el("input", { type: "color", value: effect[p.name] || p.fallback });
      picker.addEventListener("input", function () { effect[p.name] = picker.value; changed(); });
      return field(p.name, picker, p.doc);
    }
    if (p.kind === "NUMBER") {
      var ui = p.ui || { min: 0, max: 100, step: 1 };
      return field(p.name, number(effect[p.name] === undefined ? p.fallback : effect[p.name],
        function (v) { effect[p.name] = v === "" ? p.fallback : Number(v); changed(); },
        { min: ui.min, max: ui.max, step: ui.step }), p.doc);
    }
    return field(p.name, text(effect[p.name] === undefined ? p.fallback : effect[p.name],
      function (v) { effect[p.name] = v; changed(); }), p.doc);
  }

  /**
   * A trigger is one of four shapes, two of which carry a value. Written as a
   * kind picker plus the value that kind needs, so the file's union type does
   * not leak into the form as raw JSON.
   */
  function triggerEditor(effect, p) {
    var current = effect[p.name] || p.fallback || { on_move: true };
    var kind = typeof current === "string" ? current : Object.keys(current)[0];
    var wrap = el("div", { class: "row" });
    wrap.appendChild(select(schema.TRIGGER_KINDS.map(function (k) { return { value: k, label: k }; }),
      kind, function (v) {
        if (v === "interval") effect[p.name] = { interval: 40 };
        else if (v === "on_interact") effect[p.name] = { on_interact: "" };
        else { effect[p.name] = {}; effect[p.name][v] = true; }
        changed();
      }));
    if (kind === "interval") {
      wrap.appendChild(number(current.interval, function (v) {
        effect[p.name] = { interval: Math.max(1, Number(v) || 1) };
        changed();
      }, { min: 1, step: 1, class: "narrow" }));
      wrap.appendChild(el("span", { class: "hint", text: "ticks" }));
    } else if (kind === "on_interact") {
      var box = el("input", { type: "text", value: current.on_interact || "",
        placeholder: "minecraft:bucket (empty = any item)" });
      box.addEventListener("input", function () {
        effect[p.name] = { on_interact: box.value };
        changed();
      });
      wrap.appendChild(box);
    }
    return field(p.name, wrap, p.doc);
  }

  /**
   * The "when" gate. The full format is a recursive tree; this form covers a
   * flat ALL/ANY of (optionally negated) flags, which is every condition any
   * shipped gene actually uses. Anything more nested is left exactly as loaded
   * and shown read-only, because silently flattening someone's condition is
   * worse than declining to edit it.
   */
  function conditionEditor(effect) {
    var simple = model.simpleCondition(effect.when);
    if (simple === null) {
      return field("Only when", el("div", {}, [
        el("code", { class: "frozen", text: JSON.stringify(effect.when) }),
        el("span", { class: "hint", text: "a nested condition - kept exactly as loaded; "
          + "edit it in the JSON" })
      ]));
    }
    var wrap = el("div", { class: "stack" });
    if (simple.flags.length > 1) {
      wrap.appendChild(select([
        { value: "all", label: "all of these hold" },
        { value: "any", label: "any of these holds" }
      ], simple.mode, function (v) {
        simple.mode = v;
        effect.when = model.buildCondition(simple);
        changed();
      }));
    }
    simple.flags.forEach(function (f, i) {
      var row = el("div", { class: "row" });
      row.appendChild(select(schema.CONDITION_FLAGS.map(function (c) {
        return { value: c, label: c };
      }), f.flag, function (v) {
        simple.flags[i].flag = v;
        effect.when = model.buildCondition(simple);
        changed();
      }));
      var neg = el("label", { class: "inline" }, [
        checkbox(f.negate, function (v) {
          simple.flags[i].negate = v;
          effect.when = model.buildCondition(simple);
          changed();
        }),
        el("span", { text: "not" })
      ]);
      row.appendChild(neg);
      row.appendChild(button("x", function () {
        simple.flags.splice(i, 1);
        effect.when = model.buildCondition(simple);
        changed();
      }, "btn small danger"));
      wrap.appendChild(row);
    });
    wrap.appendChild(button("+ condition", function () {
      simple.flags.push({ flag: schema.CONDITION_FLAGS[0], negate: false });
      effect.when = model.buildCondition(simple);
      changed();
    }, "btn small"));
    return field("Only when", wrap,
      simple.flags.length ? "" : "no conditions - it is always on while the gene expresses");
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
      } else if (p.kind === "FLAG") {
        // A mask flag used to fall through to valueEditor, which is the
        // number/knob/per-dose control - so SPOTS' `mirror` offered to be
        // driven by a knob and could not simply be ticked. There is no such
        // thing as a per-dose boolean; it is a checkbox.
        var flagOn = mask[p.name] === undefined ? !!p.fallback : !!mask[p.name];
        card.appendChild(field(p.name, checkbox(flagOn, function (v) {
          mask[p.name] = v;
          changed();
        }), p.doc));
      } else if (p.kind === "POINTS") {
        card.appendChild(field(p.name, pointsEditor(mask, p.name), p.doc));
      } else if (p.kind === "SVG" || p.kind === "TEXT") {
        card.appendChild(field(p.name, svgTextEditor(mask, p.name, p, changed), p.doc));
      } else if (p.kind === "BOX") {
        card.appendChild(field(p.name, boxEditor(mask, p.name, changed), p.doc));
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
    if (p.kind === "COLORS") {
      return field(p.name, colorListEditor(op, p.name), p.doc);
    }
    if (p.kind === "CHOICE") {
      return field(p.name, select(p.choices, op[p.name] || p.fallback,
        function (v) { op[p.name] = v; changed(); }), p.doc);
    }
    return field(p.name, valueEditor(op, p.name, p, changed), p.doc);
  }

  /**
   * <b>A PATH's control points</b>, as a textarea of {@code u, v} pairs.
   *
   * <p>This is deliberately the plain version. The point of the PATH mask is
   * that a shape can be <i>drawn</i>, and a canvas with draggable handles is
   * what it is for - but the mask has to exist and be trusted before the
   * surface that edits it is worth building, and in the meantime typing
   * coordinates against the live preview is a real way to author one. The
   * format does not change when the canvas arrives; only this control does.
   *
   * <p>One point per line, {@code u, v}. Anything unparseable is left in the
   * box rather than silently dropped, so a half-typed line does not erase the
   * shape while you are in the middle of it.
   */
  function pointsEditor(mask, name) {
    var wrap = el("div", { class: "col" });
    var flat = mask[name] || [];
    var text = "";
    for (var i = 0; i + 1 < flat.length; i += 2) {
      text += flat[i] + ", " + flat[i + 1] + "\n";
    }
    var area = el("textarea", { rows: Math.max(4, flat.length / 2 + 1), value: text.trim() });
    var note = el("span", { class: "hint" });
    function reread() {
      var out = [];
      var bad = 0;
      area.value.split("\n").forEach(function (line) {
        if (!line.trim()) return;
        var bits = line.split(/[\s,]+/).filter(function (b) { return b.length; });
        if (bits.length !== 2 || isNaN(Number(bits[0])) || isNaN(Number(bits[1]))) {
          bad++;
          return;
        }
        out.push(Number(bits[0]), Number(bits[1]));
      });
      note.textContent = bad
        ? bad + " line(s) not read yet - each one wants two numbers"
        : (out.length / 2) + " points";
      if (out.length >= 4) {
        mask[name] = out;
        changed();
      }
    }
    area.addEventListener("input", reread);
    wrap.appendChild(area);
    wrap.appendChild(note);
    reread();
    return wrap;
  }

  /**
   * <b>An SVG mask's path data, or its transform list</b> - a textarea, and a
   * line underneath saying whether it parsed.
   *
   * <p>The note is the whole value of this control. A {@code d} string is a
   * thing people paste rather than type, and the two ways it goes wrong -
   * a stray character from the surrounding XML, and a path that flattens past
   * the point ceiling - both produce a mask that simply paints nothing. Saying
   * "84 points, 3 subpaths" or naming the character it choked on turns a blank
   * horse into a one-line fix.
   */
  function svgTextEditor(mask, name, p, changed) {
    var wrap = el("div", { class: "stack" });
    var area = el("textarea", { rows: name === "d" ? 5 : 2, value: mask[name] || "" });
    var note = el("span", { class: "hint" });
    function reread() {
      mask[name] = area.value;
      changed();
      if (!mask.d) {
        note.textContent = "no path data yet";
        return;
      }
      try {
        var shape = HG.svgPath.parse(mask.d, mask.transform);
        note.textContent = shape.xs.length + " points in " + shape.closed.length
          + " subpath(s), " + fmt(shape.box[0]) + " " + fmt(shape.box[1]) + " to "
          + fmt(shape.box[2]) + " " + fmt(shape.box[3]);
        note.classList.remove("bad");
      } catch (e) {
        note.textContent = e.message;
        note.classList.add("bad");
      }
    }
    function fmt(n) { return Math.round(n * 100) / 100; }
    area.addEventListener("input", reread);
    wrap.appendChild(area);
    wrap.appendChild(note);
    reread();
    return wrap;
  }

  /**
   * A {@code viewBox}: four numbers, taken the way the attribute writes them so
   * one can be pasted straight across. Blank means "use the drawing's own
   * bounds", which is what the game does with the key absent.
   */
  function boxEditor(mask, name, changed) {
    var wrap = el("div", { class: "stack" });
    var box = mask[name];
    var input = el("input", {
      type: "text",
      value: box && box.length === 4 ? box.join(" ") : "",
      placeholder: "minU minV width height - blank fits the drawing's own bounds"
    });
    var note = el("span", { class: "hint" });
    function reread() {
      if (!input.value.trim()) {
        delete mask[name];
        note.textContent = "using the drawing's own bounding box";
        note.classList.remove("bad");
        changed();
        return;
      }
      var nums = HG.svgPath.numbers(input.value);
      if (nums.length !== 4 || nums[2] <= 0 || nums[3] <= 0) {
        note.textContent = "four numbers, and the last two above 0";
        note.classList.add("bad");
        return;
      }
      mask[name] = nums;
      note.textContent = "";
      note.classList.remove("bad");
      changed();
    }
    input.addEventListener("input", reread);
    wrap.appendChild(input);
    wrap.appendChild(note);
    reread();
    return wrap;
  }

  /**
   * A ramp's stops, or a palette's entries: a row of colour wells with a way to
   * add and remove one. Order matters for a RAMP (it walks them left to right)
   * and does not for a PALETTE, which is the only thing to know.
   */
  function colorListEditor(op, name) {
    var wrap = el("div", { class: "row wrap" });
    var list = op[name] || (op[name] = []);
    list.forEach(function (hex, i) {
      var well = el("input", { type: "color", value: hex });
      well.addEventListener("input", function () { list[i] = well.value; changed(); });
      var cell = el("div", { class: "swatch-cell" }, [
        well,
        button("\u2715", function () {
          if (list.length <= 2) {
            return;   // the parser wants two; taking the last one away is a load error
          }
          list.splice(i, 1);
          changed();
        }, "btn tiny danger")
      ]);
      wrap.appendChild(cell);
    });
    wrap.appendChild(button("+ stop", function () {
      list.push(list.length ? list[list.length - 1] : "#ff69b4");
      changed();
    }, "btn tiny"));
    return wrap;
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
    var gene = focusPath($("gene-panel"));
    var layers = focusPath($("layers-panel"));
    var effects = focusPath($("effects-panel"));
    renderGenePanel();
    renderLayers();
    renderEffects();
    restoreFocus($("gene-panel"), gene);
    restoreFocus($("layers-panel"), layers);
    restoreFocus($("effects-panel"), effects);
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
      // Effects are deep-copied so editing one cannot reach back into the
      // example that was loaded, and so a nested "when" survives untouched.
      entry.effects = (e.effects || []).map(function (fx) {
        return JSON.parse(JSON.stringify(fx));
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
    // The metadata blocks are optional and may arrive partial, so merge rather
    // than let Object.assign above replace the defaults wholesale.
    out.blurb = spec.blurb || "";
    out.rarity = spec.rarity || model.DEFAULT_RARITY;
    out.carrot = Object.assign(model.defaultCarrot(), spec.carrot || {});
    out.carrot.flavour = ((spec.carrot || {}).flavour || []).slice();
    out.splice = spec.splice ? Object.assign({}, spec.splice) : null;
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

  /**
   * Run the port against the baked Java fixtures, right here, every time the
   * tool opens. Parity used to be a Node script at the terminal, so editing
   * js/ and forgetting to run it left the creator happily previewing a horse
   * the game would not breed - convincingly, which is the whole danger. Now
   * the tool refuses to look trustworthy when it is not.
   */
  function runParitySelfCheck() {
    var box = $("parity-status");
    if (!box) return;
    if (!HG.parity || !HG.fixtures || !HG.exampleFiles) {
      box.className = "status warnish";
      box.textContent = "parity self-check unavailable - fixtures/expected.js did not load "
        + "(run ./gradlew :common:bakeSpecFixtures)";
      return;
    }
    // The fixtures name a gene by file; the examples are keyed by menu label.
    var specs = {};
    Object.keys(HG.exampleFiles).forEach(function (file) {
      var spec = HG.examples[HG.exampleFiles[file]];
      if (spec) specs[file] = spec;
    });

    var result;
    try {
      result = HG.parity.run({ fixtures: HG.fixtures, specs: specs });
    } catch (e) {
      box.className = "status bad";
      box.textContent = "parity self-check crashed: " + e.message;
      return;
    }

    if (!result.failures.length) {
      box.className = "status ok";
      box.textContent = "engine matches the game - " + result.checked
        + " checks across " + result.cases + " cases";
      return;
    }
    box.className = "status bad";
    box.innerHTML = "";
    box.appendChild(el("strong", {
      text: "PREVIEW IS NOT TRUSTWORTHY - " + result.failures.length
        + " mismatch(es) against the game out of " + result.checked + " checks"
    }));
    result.failures.slice(0, 5).forEach(function (f) {
      box.appendChild(el("div", { class: "problem", text: f }));
    });
    if (result.failures.length > 5) {
      box.appendChild(el("div", { class: "hint", text: "...and "
        + (result.failures.length - 5) + " more - see check-parity.mjs for the full list" }));
    }
    console.error("gene-creator parity failures:", result.failures);
  }

  function start() {
    renderToolbar();
    wireActions();
    runParitySelfCheck();
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
