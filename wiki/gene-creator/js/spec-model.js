// The document the creator edits: a gene spec, exactly as the game's JSON.
//
// There is deliberately no intermediate model. The object the UI mutates IS the
// file - so the preview runs the file, the export writes the file, and there is
// no third representation to fall out of step with either.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var schema = HG.schema;

  // ---- the gene the creator edits ---------------------------------------
  //
  // The creator authors a two-allele gene with ONE visible outcome plus a wild
  // type. That is what its single layer list can describe, and saying so out
  // loud is better than pretending otherwise: the format allows any number of
  // alleles and any number of outcomes (wiki/gene-format.html), and a gene that
  // needs them is hand-edited JSON for now.
  //
  // visible(spec) is the outcome the forms edit; wild(spec) is the silent one.

  function blank() {
    return {
      format: 2,
      key: "mymod.my_gene",
      name: "My gene",
      phase: "natural",
      priority: 100,
      alleles: [
        { token: "My", label: "My gene (My)" },
        { token: "my", label: "Wild-type (my)" }
      ],
      knobs: [],
      expressions: [
        {
          id: "my_gene",
          name: "My gene",
          description: "What a horse carrying this looks like.",
          when: ["My/My", "My/my"],
          layers: [newLayer("natural")]
        },
        { id: "wild", name: "Wild type", description: "No effect.", wildType: true }
      ],
      founders: { "My/My": 0.5, "My/my": 4.5, "my/my": 95.0 }
    };
  }

  /** The one outcome the forms edit - the first non-wild-type entry. */
  function visible(spec) {
    var list = spec.expressions || [];
    for (var i = 0; i < list.length; i++) {
      if (!list[i].wildType) return list[i];
    }
    return list[0];
  }

  /** The do-nothing outcome. */
  function wild(spec) {
    var list = spec.expressions || [];
    for (var i = 0; i < list.length; i++) {
      if (list[i].wildType) return list[i];
    }
    return null;
  }

  /** The layer list the forms edit. */
  function layersOf(spec) {
    var e = visible(spec);
    if (e && !e.layers) e.layers = [];
    return e ? e.layers : [];
  }

  /** Every unordered combination of the gene alleles, canonical order. */
  function combinations(spec) {
    return HG.specEngine.combinations(spec);
  }

  /**
   * Which combinations land on the visible outcome. Two shapes, which is all a
   * two-allele gene can be: one copy is enough, or it takes two. That is the
   * old DOMINANT / RECESSIVE pair said as the table rather than as a label.
   */
  function showsWhen(spec) {
    var e = visible(spec);
    var owned = (e && Array.isArray(e.when)) ? e.when : [];
    return owned.length > 1 ? "any" : "homozygous";
  }

  function setShowsWhen(spec, mode) {
    var a = spec.alleles || [];
    if (a.length < 2) return;
    var variant = a[0].token;
    var baseline = a[a.length - 1].token;
    var e = visible(spec);
    e.when = mode === "any"
      ? [variant + "/" + variant, variant + "/" + baseline]
      : [variant + "/" + variant];
  }

  /**
   * Keep the "when" list and the founder table pointing at the current allele
   * tokens - renaming an allele must not silently orphan either.
   */
  function retoken(spec, oldTokens) {
    var a = spec.alleles || [];
    if (a.length < 2) return;
    var mode = showsWhen(spec);
    var weights = {};
    if (oldTokens && oldTokens.length === a.length) {
      var oldCombos = HG.specEngine.combinations({
        alleles: oldTokens.map(function (t) { return { token: t }; })
      });
      var newCombos = combinations(spec);
      oldCombos.forEach(function (c, i) {
        if (newCombos[i] !== undefined) weights[newCombos[i]] = (spec.founders || {})[c];
      });
    }
    setShowsWhen(spec, mode);
    var out = {};
    combinations(spec).forEach(function (c) {
      out[c] = weights[c] === undefined ? 0 : weights[c];
    });
    spec.founders = out;
  }

  function newLayer(phase) {
    return {
      name: "new layer",
      masks: [newMask("PARTS")],
      op: newOp(phase === "magical" ? "TINT" : "SET_PIGMENT")
    };
  }

  function newMask(type) {
    var mask = { type: type };
    if (type === "PARTS") mask.parts = ["LEGS"];
    schema.MASKS[type].params.forEach(function (p) {
      if (p.kind === "VALUE" && !(p.ui && p.ui.seedRef)) mask[p.name] = initial(p);
      if (p.kind === "CHOICE") mask[p.name] = p.fallback;
    });
    return mask;
  }

  function newOp(type) {
    var op = { type: type };
    schema.OPS[type].params.forEach(function (p) {
      op[p.name] = p.kind === "COLOR" ? "#ff69b4" : initial(p);
    });
    return op;
  }

  /** What a newly added mask or op starts at - see the note in schema.js. */
  function initial(p) {
    return p.initial === undefined ? p.fallback : p.initial;
  }

  function newKnob(spec, type) {
    var base = type === "seed" ? "seed" : "amount";
    var name = base, i = 2;
    while (spec.knobs.some(function (k) { return k.name === name; })) name = base + i++;
    return type === "seed"
      ? { name: name, type: "seed" }
      : { name: name, min: 0.2, max: 0.8, per: "horse", spread: 0 };
  }

  /**
   * Strip the fields that only exist because a form wrote a default into them.
   * A gene file people read and edit should be short - the game fills in every
   * omitted parameter with the same value anyway.
   */
  function tidy(spec) {
    // Callable on a raw file as well as on the editor's own object, so the
    // export and the validator work on anything you paste in.
    var knobs = spec.knobs || [];
    var alleles = spec.alleles || [];
    var out = {
      format: 2,
      key: spec.key,
      name: spec.name,
      phase: spec.phase,
      priority: Number(spec.priority),
      alleles: alleles.map(function (a) {
        return a.label ? { token: a.token, label: a.label } : { token: a.token };
      })
    };
    if (knobs.length) {
      out.knobs = knobs.map(function (k) {
        if (k.type === "seed") return { name: k.name, type: "seed" };
        var knob = { name: k.name, min: num(k.min), max: num(k.max) };
        if (k.per === "leg") {
          knob.per = "leg";
          if (Number(k.spread)) knob.spread = num(k.spread);
        }
        return knob;
      });
    }
    out.expressions = (spec.expressions || []).map(function (e) {
      var entry = { id: e.id, name: e.name, description: e.description || "" };
      if (e.wildType) entry.wildType = true;
      if (e.masks) entry.masks = true;
      if (e.when !== undefined && e.when !== null) entry.when = e.when;
      if (!e.wildType) {
        entry.layers = (e.layers || []).map(function (layer) {
          return {
            name: layer.name,
            masks: (layer.masks || []).map(function (m) { return tidyMask(m); }),
            op: tidyOp(layer.op)
          };
        });
        if (e.effects && e.effects.length) entry.effects = e.effects;
      }
      return entry;
    });
    out.founders = {};
    Object.keys(spec.founders || {}).forEach(function (c) {
      out.founders[c] = num(spec.founders[c]);
    });
    return out;
  }

  function tidyMask(mask) {
    var out = { type: mask.type };
    if (mask.parts && mask.parts.length) out.parts = mask.parts.slice();
    schema.MASKS[mask.type].params.forEach(function (p) {
      if (p.kind === "PARTS") return;
      var v = mask[p.name];
      if (v === undefined || v === null || v === "") return;
      if (p.kind === "VALUE" && isDefault(v, p.fallback)) return;
      if (p.kind === "CHOICE" && v === p.fallback) return;
      out[p.name] = tidyValue(v);
    });
    if (mask.combine && mask.combine !== "MULTIPLY") out.combine = mask.combine;
    if (mask.invert) out.invert = true;
    return out;
  }

  function tidyOp(op) {
    var out = { type: op.type };
    schema.OPS[op.type].params.forEach(function (p) {
      var v = op[p.name];
      if (v === undefined || v === null || v === "") return;
      out[p.name] = p.kind === "COLOR" ? String(v) : tidyValue(v);
    });
    return out;
  }

  function tidyValue(v) {
    if (typeof v === "object" && v && v.perDose) return { perDose: v.perDose.map(num) };
    if (typeof v === "object" && v && v.min !== undefined) {
      var o = { min: num(v.min), max: num(v.max) };
      if (v.per === "leg") {
        o.per = "leg";
        if (Number(v.spread)) o.spread = num(v.spread);
      }
      return o;
    }
    if (typeof v === "string") return v;
    return num(v);
  }

  function isDefault(v, fallback) {
    return typeof v === "number" && Math.abs(v - fallback) < 1e-12;
  }

  function num(v) {
    var n = Number(v);
    return Math.round(n * 1e6) / 1e6;
  }

  function toJson(spec) {
    return JSON.stringify(tidy(spec), null, 2) + "\n";
  }

  function fileName(spec) {
    var k = String(spec.key || "gene");
    return k.slice(k.indexOf(".") + 1).replace(/[^a-z0-9_]/gi, "_") + ".json";
  }

  // ---- validation ------------------------------------------------------
  //
  // A subset of GeneSpecParser's rules - the ones you can trip while editing.
  // The game is the authority; this just stops you exporting something it will
  // reject, while you can still see why.

  function problems(spec) {
    var out = [];
    if (!/^[a-z0-9_]+\.[a-z0-9_]+$/.test(spec.key || "")) {
      out.push("Key must be \"modid.gene\" in lower case - e.g. \"mymod.silver\".");
    }
    if (!spec.alleles || spec.alleles.length < 2) {
      out.push("A gene needs at least two alleles: the variant first, the wild type last.");
    }
    (spec.alleles || []).forEach(function (a, i) {
      if (!a.token || !a.token.trim()) out.push("Allele " + (i + 1) + " has no token.");
      else if (/[/-]/.test(a.token)) {
        out.push("Allele token \"" + a.token + "\" contains / or -, which separate "
          + "alleles and genes in a genotype code.");
      }
    });
    var tokens = (spec.alleles || []).map(function (a) { return a.token; });
    tokens.forEach(function (t, i) {
      if (tokens.indexOf(t) !== i) out.push("Two alleles share the token \"" + t + "\".");
    });
    var founders = spec.founders || {};
    var total = 0;
    combinations(spec).forEach(function (c) { total += Number(founders[c]) || 0; });
    if (!(total > 0)) {
      out.push("Every founder share is zero, so no wild horse can carry this gene.");
    } else if (Math.abs(total - 100) > 0.01) {
      out.push("Founder shares add up to " + (Math.round(total * 100) / 100)
        + "%, not 100. The game normalises them and warns - better to fix the numbers.");
    }
    var layers = layersOf(spec);
    if (!layers.length) out.push("A gene with no layers does nothing to the coat.");
    layers.forEach(function (layer, i) {
      var op = schema.OPS[layer.op.type];
      if (op && op.phase !== spec.phase) {
        out.push("Layer " + (i + 1) + " uses the " + op.phase + " op " + layer.op.type
          + " but the gene is " + spec.phase + ". A gene is one or the other, never both.");
      }
      if (!layer.masks || !layer.masks.length) {
        out.push("Layer " + (i + 1) + " has no masks, so it covers the whole horse. "
          + "Add an ALL mask if that is what you meant.");
      }
    });
    var names = (spec.knobs || []).map(function (k) { return k.name; });
    names.forEach(function (n, i) {
      if (names.indexOf(n) !== i) out.push("Two knobs are named \"" + n + "\".");
    });
    return out;
  }

  /** Every "$knob" reference anywhere in the spec, for the rename / delete guard. */
  function knobUses(spec, name) {
    var uses = 0;
    var ref = "$" + name;
    layersOf(spec).forEach(function (layer) {
      (layer.masks || []).forEach(function (m) {
        Object.keys(m).forEach(function (k) { if (m[k] === ref) uses++; });
      });
      Object.keys(layer.op || {}).forEach(function (k) { if (layer.op[k] === ref) uses++; });
    });
    return uses;
  }

  HG.specModel = {
    blank: blank,
    visible: visible,
    wild: wild,
    layersOf: layersOf,
    combinations: combinations,
    showsWhen: showsWhen,
    setShowsWhen: setShowsWhen,
    retoken: retoken,
    newLayer: newLayer,
    newMask: newMask,
    newOp: newOp,
    newKnob: newKnob,
    tidy: tidy,
    toJson: toJson,
    fileName: fileName,
    problems: problems,
    knobUses: knobUses
  };
})(window.HG);
