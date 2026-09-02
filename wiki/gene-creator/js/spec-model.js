// The document the creator edits: a gene spec, exactly as the game's JSON.
//
// There is deliberately no intermediate model. The object the UI mutates IS the
// file - so the preview runs the file, the export writes the file, and there is
// no third representation to fall out of step with either.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var schema = HG.schema;

  function blank() {
    return {
      format: 1,
      key: "mymod.my_gene",
      name: "My gene",
      phase: "natural",
      dominance: "DOMINANT",
      wildOdds: 40,
      priority: 100,
      alleles: [
        { token: "My", label: "My gene (My)" },
        { token: "my", label: "Wild-type (my)" }
      ],
      knobs: [],
      layers: [newLayer("natural")]
    };
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
    var layers = spec.layers || [];
    var alleles = spec.alleles || [];
    var out = {
      format: 1,
      key: spec.key,
      name: spec.name,
      phase: spec.phase,
      dominance: spec.dominance,
      wildOdds: Number(spec.wildOdds),
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
    out.layers = layers.map(function (layer) {
      return {
        name: layer.name,
        masks: (layer.masks || []).map(function (m) { return tidyMask(m); }),
        op: tidyOp(layer.op)
      };
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
    if (!(Number(spec.wildOdds) >= 1)) out.push("Wild frequency must be 1 in 1 or rarer.");
    if (!spec.layers || !spec.layers.length) out.push("A gene with no layers does nothing to the coat.");
    (spec.layers || []).forEach(function (layer, i) {
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
    (spec.layers || []).forEach(function (layer) {
      (layer.masks || []).forEach(function (m) {
        Object.keys(m).forEach(function (k) { if (m[k] === ref) uses++; });
      });
      Object.keys(layer.op || {}).forEach(function (k) { if (layer.op[k] === ref) uses++; });
    });
    return uses;
  }

  HG.specModel = {
    blank: blank,
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
