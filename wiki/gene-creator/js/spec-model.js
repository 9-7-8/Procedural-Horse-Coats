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
  // alleles and any number of outcomes (wiki/making-a-gene.html), and a gene that
  // needs them is hand-edited JSON for now.
  //
  // visible(spec) is the outcome the forms edit; wild(spec) is the silent one.

  function blank() {
    return {
      format: 3,
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
      founders: { "My/My": 0.5, "My/my": 4.5, "my/my": 95.0 },
      // Format-3 gameplay metadata. Held at the documented defaults so the
      // forms have something to bind to; tidy() drops whatever is still
      // default, so a gene that ignores all of this still exports a short file.
      blurb: "",
      rarity: DEFAULT_RARITY,
      carrot: defaultCarrot(),
      splice: null
    };
  }

  /** The six GeneRarity tiers, ascending. A gene that declares none is UNCOMMON. */
  var RARITIES = ["common", "uncommon", "rare", "epic", "legendary", "mythic"];
  var DEFAULT_RARITY = "uncommon";

  function defaultCarrot() {
    return { enabled: true, behaviour: "homozygous", flavour: [] };
  }

  function carrotIsDefault(c) {
    return !c || (c.enabled !== false
      && (c.behaviour || "homozygous") === "homozygous"
      && !(c.flavour || []).length);
  }

  /** Does this splice table say anything, or is it an empty form? */
  function spliceIsSet(spec) {
    var t = spec.splice;
    if (!t) return false;
    return Object.keys(t).some(function (c) { return Number(t[c]) > 0; });
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
    var oldCombos = null;
    if (oldTokens && oldTokens.length === a.length) {
      oldCombos = HG.specEngine.combinations({
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
    // The splice table is keyed by combination too, so a rename orphans it the
    // same way - and silently, since it is optional and the game would just
    // reject the file.
    if (spec.splice) {
      var moved = {};
      combinations(spec).forEach(function (c, i) {
        var was = oldCombos ? oldCombos[i] : undefined;
        moved[c] = (was !== undefined && spec.splice[was] !== undefined)
          ? spec.splice[was] : 0;
      });
      spec.splice = moved;
    }
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
    // A PATH with no points paints nothing and cannot be exported, so a fresh
    // one starts as a visible shape on the flank rather than as an empty box:
    // a shallow arc across the barrel, in normalised body space.
    if (type === "PATH") mask.points = [0.25, 0.45, 0.45, 0.6, 0.65, 0.55, 0.8, 0.4];
    // Likewise an SVG mask: an empty 'd' is a load error rather than a blank
    // shape, so a fresh one carries a drawing. A rounded teardrop, written with
    // an arc and a curve so the first thing anybody sees exercises both.
    if (type === "SVG") {
      mask.d = "M 50 8 C 78 34 92 56 92 70 A 42 42 0 0 1 8 70 C 8 56 22 34 50 8 Z";
      mask.viewBox = [0, 0, 100, 100];
      mask.originU = 0.35;
      mask.originV = 0.36;
      mask.sizeU = 0.2;
      mask.sizeV = 0.2;
    }
    schema.MASKS[type].params.forEach(function (p) {
      if (p.kind === "VALUE" && !(p.ui && p.ui.seedRef)) mask[p.name] = initial(p);
      if (p.kind === "CHOICE") mask[p.name] = p.fallback;
    });
    return mask;
  }

  function newOp(type) {
    var op = { type: type };
    schema.OPS[type].params.forEach(function (p) {
      if (p.kind === "COLOR") {
        op[p.name] = "#ff69b4";
      } else if (p.kind === "COLORS") {
        // Two stops, because one is not a ramp and the parser says so. Starting
        // a RAMP empty would work - it would fall through to the hue sweep -
        // but then the stop list only appears once you already knew it existed.
        op[p.name] = ["#ff69b4", "#69b4ff"];
      } else {
        op[p.name] = initial(p);
      }
    });
    return op;
  }

  /** What a newly added mask or op starts at - see the note in schema.js. */
  function initial(p) {
    return p.initial === undefined ? p.fallback : p.initial;
  }

  // ---- effects ----------------------------------------------------------
  //
  // The gameplay half of an outcome: what a horse carrying it DOES. Effects
  // hang off the expression, not off the gene, so a homozygote and a carrier
  // can grant entirely different behaviour with nothing comparing doses.

  function effectsOf(spec) {
    var e = visible(spec);
    if (e && !e.effects) e.effects = [];
    return e ? e.effects : [];
  }

  function newEffect(verb) {
    var effect = { type: verb };
    (schema.EFFECTS[verb].params || []).forEach(function (p) {
      // A required parameter has no default the game could fall back on, so it
      // starts at the first legal choice rather than blank - a fresh effect
      // should export as something the game will load.
      if (p.required && p.choices) effect[p.name] = p.choices[0];
      else if (p.required) effect[p.name] = "";
      else if (p.kind === "PARTS") effect[p.name] = [];
      else if (p.kind === "TRIGGER") effect[p.name] = cloneValue(p.fallback);
      else effect[p.name] = p.fallback;
    });
    return effect;
  }

  function cloneValue(v) {
    return (v && typeof v === "object") ? JSON.parse(JSON.stringify(v)) : v;
  }

  /**
   * Can the simple form edit this condition? It handles a single flag and a
   * flat all/any of flags, each optionally negated - which is every condition
   * any shipped gene uses. A nested tree is preserved verbatim and shown
   * read-only rather than silently flattened.
   */
  function simpleCondition(when) {
    if (!when || !Object.keys(when).length) return { mode: "all", flags: [] };
    if (when.flag) return { mode: "all", flags: [{ flag: when.flag, negate: !!when.negate }] };
    var key = when.all ? "all" : (when.any ? "any" : null);
    if (!key || !Array.isArray(when[key])) return null;
    var flags = [];
    for (var i = 0; i < when[key].length; i++) {
      var t = when[key][i];
      if (!t || !t.flag) return null;
      var extra = Object.keys(t).filter(function (k) { return k !== "flag" && k !== "negate"; });
      if (extra.length) return null;
      flags.push({ flag: t.flag, negate: !!t.negate });
    }
    return { mode: key, flags: flags };
  }

  /** The simple form back to the file's shape - and to nothing at all when empty. */
  function buildCondition(simple) {
    var flags = (simple.flags || []).filter(function (f) { return f.flag; });
    if (!flags.length) return undefined;
    function term(f) {
      var t = { flag: f.flag };
      if (f.negate) t.negate = true;
      return t;
    }
    if (flags.length === 1 && simple.mode !== "any") return term(flags[0]);
    var out = {};
    out[simple.mode === "any" ? "any" : "all"] = flags.map(term);
    return out;
  }

  function tidyEffect(effect) {
    var def = schema.EFFECTS[effect.type];
    var out = { type: effect.type };
    if (!def) return Object.assign(out, effect);
    def.params.forEach(function (p) {
      var v = effect[p.name];
      if (v === undefined || v === null) return;
      if (p.kind === "PARTS") {
        if (v.length) out[p.name] = v.slice();
        return;
      }
      if (p.kind === "TRIGGER") {
        // Only when it differs from the verb's own default trigger.
        if (JSON.stringify(v) !== JSON.stringify(p.fallback)) out[p.name] = cloneValue(v);
        return;
      }
      if (p.kind === "NUMBER") {
        if (!p.required && Math.abs(Number(v) - Number(p.fallback)) < 1e-12) return;
        out[p.name] = num(v);
        return;
      }
      if (!p.required && String(v) === String(p.fallback)) return;
      if (v === "" && !p.required) return;
      out[p.name] = v;
    });
    if (effect.when && Object.keys(effect.when).length) out.when = cloneValue(effect.when);
    if (Number(effect.minDose) === 2) out.minDose = 2;
    return out;
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
      format: 3,
      key: spec.key,
      name: spec.name,
      phase: spec.phase,
      priority: Number(spec.priority)
    };
    // Header metadata, emitted only when it says something the game would not
    // already assume - a gene that leaves the economy alone stays a short file.
    if (spec.blurb && spec.blurb.trim()) out.blurb = spec.blurb.trim();
    // The gene's own prose. blurb is a one-to-three-sentence SUMMARY and has to
    // stay that size; notes is where the rest goes - why the numbers are the
    // numbers, what was tried and abandoned - and it is printed on the gene's
    // generated wiki page as written.
    var notes = tidyNotes(spec.notes);
    if (notes) out.notes = notes;
    if (spec.rarity && spec.rarity !== DEFAULT_RARITY) out.rarity = spec.rarity;
    out.alleles = alleles.map(function (a) {
      return a.label ? { token: a.token, label: a.label } : { token: a.token };
    });
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
      // Prose about this one outcome, written for a reader of the FILE rather
      // than for a player - see GeneSpec.ExpressionSpec#notes. Emitted only
      // when there is something in it, so a gene that says nothing stays short.
      var eNotes = tidyNotes(e.notes);
      if (eNotes) entry.notes = eNotes;
      if (e.wildType) entry.wildType = true;
      if (e.masks) entry.masks = true;
      if (e.when !== undefined && e.when !== null) entry.when = e.when;
      if (!e.wildType) {
        entry.layers = (e.layers || []).map(function (layer) {
          var out = {
            name: layer.name,
            masks: (layer.masks || []).map(function (m) { return tidyMask(m); }),
            op: tidyOp(layer.op)
          };
          // A layer's glow used to be dropped here entirely: loading a glowing
          // gene and exporting it again silently put the light out. It is a
          // Value now (a number, a "$knob" or a per-dose triple), and `true`
          // means all of it.
          if (layer.emissive !== undefined && layer.emissive !== null
            && layer.emissive !== false) {
            out.emissive = layer.emissive === true ? 1 : cloneValue(layer.emissive);
          }
          return out;
        });
        if (e.effects && e.effects.length) entry.effects = e.effects.map(tidyEffect);
      }
      return entry;
    });
    out.founders = {};
    Object.keys(spec.founders || {}).forEach(function (c) {
      out.founders[c] = num(spec.founders[c]);
    });
    // What the Unknown Gene Splice carrot rolls on this gene. Same shape as
    // founders and kept as its own key so the two can differ; omitted entirely
    // when the author left it alone, which means "draw uniformly".
    if (spliceIsSet(spec)) {
      out.splice = {};
      combinations(spec).forEach(function (c) {
        out.splice[c] = num((spec.splice || {})[c] || 0);
      });
    }
    if (!carrotIsDefault(spec.carrot)) {
      var c = spec.carrot;
      out.carrot = { enabled: c.enabled !== false };
      if ((c.behaviour || "heterozygous") !== "heterozygous") out.carrot.behaviour = c.behaviour;
      var flavour = (c.flavour || []).filter(function (f) { return f && f.trim(); });
      if (flavour.length) out.carrot.flavour = flavour.map(function (f) { return f.trim(); });
    }
    // Which base coat and which expression the gene's ONE baked picture is
    // taken on, when measuring it comes out wrong (GeneSpec.Preview). There is
    // no editor for it - it is a handful of genes and an odd thing to want -
    // but it is passed through rather than dropped, because a file that loses
    // a key on a round-trip through this tool is worse than one that cannot
    // set it here.
    if (spec.preview && (spec.preview.base || spec.preview.expression)) {
      out.preview = {};
      if (spec.preview.base) out.preview.base = spec.preview.base;
      if (spec.preview.expression) out.preview.expression = spec.preview.expression;
    }
    return out;
  }

  /**
   * A `notes` block, normalised: an array of non-empty trimmed paragraphs, or
   * null when there is nothing to say.
   *
   * Accepts a plain string as well as an array, because the parser does and
   * because that is what somebody writes the first time. Returning null rather
   * than [] is what keeps the key out of the exported file entirely - an empty
   * notes array on ninety genes is noise in every diff.
   */
  function tidyNotes(notes) {
    if (!notes) return null;
    var list = typeof notes === "string" ? [notes] : notes;
    var out = [];
    for (var i = 0; i < list.length; i++) {
      var p = String(list[i] == null ? "" : list[i]).trim();
      if (p) out.push(p);
    }
    return out.length ? out : null;
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
      if (p.kind === "FLAG") {
        // Write it only when it differs from what the game assumes, so a mask
        // does not export every flag its author never touched. For the SVG
        // mask's two true-by-default flags that means the UNticked box is the
        // one that writes - which is the whole reason the fallback moved out of
        // this function and into the table.
        if (!!v !== !!p.fallback) out[p.name] = !!v;
        return;
      }
      if (p.kind === "POINTS" || p.kind === "BOX") {
        if (v.length >= 4) out[p.name] = v.slice();
        return;
      }
      if (p.kind === "SVG" || p.kind === "TEXT") {
        // Strings, written verbatim - and never trimmed to a default, since
        // there is no such thing as a default drawing.
        if (String(v).trim()) out[p.name] = String(v);
        return;
      }
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
      if (p.kind === "COLOR") {
        out[p.name] = String(v);
      } else if (p.kind === "COLORS") {
        if (v.length) {
          out[p.name] = v.map(String);
        }
      } else {
        out[p.name] = tidyValue(v);
      }
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
    // The two things the loader refuses about a glow, said here first.
    layers.forEach(function (layer, i) {
      var glow = layer.emissive;
      if (glow === undefined || glow === null || glow === false) return;
      if (spec.phase !== "magical") {
        out.push("Layer " + (i + 1) + " glows, but the gene is natural. Pigment does not "
          + "glow - move the glow to a magical gene, or turn it off.");
      }
      if (typeof glow === "number" && (glow < 0 || glow > 1)) {
        out.push("Layer " + (i + 1) + "'s glow is " + glow + ". It is a fraction of full "
          + "bright, so it has to be between 0 and 1.");
      }
      (layer.masks || []).forEach(function (m) {
        if (m.type === "PIGMENT" || m.type === "LUMA") {
          out.push("Layer " + (i + 1) + " glows and uses a " + m.type + " mask. Glow is "
            + "decided in the overlay pass, after the texture is baked, where there is "
            + "neither a pigment field nor a colour accumulator left to read. Split the "
            + "layer: paint with the " + m.type + " mask, glow with a shape one.");
        }
      });
    });
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
    // The three rules GeneSpecParser enforces about the dial knob, said
    // here first. The loader's message is the better one; arriving at it by
    // restarting the game is the worse way to read it.
    var dials = (spec.knobs || []).filter(function (k) { return k.dial; });
    if (dials.length > 1) {
      out.push("Two knobs are marked as the dial (\"" + dials[0].name + "\" and \""
        + dials[1].name + "\"). A gene has one measure of how much of itself it shows, "
        + "or none.");
    }
    dials.forEach(function (k) {
      if (k.type === "seed") {
        out.push("Knob \"" + k.name + "\" is a seed and cannot be the dial - a seed has "
          + "no range to be a fraction of.");
      }
      if (k.per === "leg") {
        out.push("Knob \"" + k.name + "\" is per-leg and cannot be the dial - four legs' "
          + "worth of \"how much of itself this horse shows\" is not one number.");
      }
    });
    layers.forEach(function (layer, i) {
      (layer.masks || []).forEach(function (m) {
        if (m.type !== "PATH" || !m.pointsMin) return;
        if (!dials.length) {
          out.push("Layer " + (i + 1) + "'s PATH has a minimal shape but the gene has no knob "
            + "marked as its dial, so there is nothing for the shape to shrink along.");
        }
        if ((m.pointsMin || []).length !== (m.points || []).length) {
          out.push("Layer " + (i + 1) + "'s PATH has " + ((m.pointsMin || []).length / 2)
            + " minimal points against " + ((m.points || []).length / 2) + " full ones - each "
            + "point moves to its twin, so there has to be one of each.");
        }
      });
    });
    effectsOf(spec).forEach(function (effect, i) {
      var where = "Effect " + (i + 1) + " (" + effect.type + ")";
      var def = schema.EFFECTS[effect.type];
      if (!def) {
        out.push(where + ": there is no such effect verb.");
        return;
      }
      def.params.forEach(function (p) {
        if (!p.required) return;
        var v = effect[p.name];
        if (v === undefined || v === null || String(v).trim() === "") {
          out.push(where + ": " + p.name + " is required and has no default.");
        } else if (p.choices && p.choices.indexOf(v) < 0) {
          out.push(where + ": " + p.name + " must be one of " + p.choices.join(", ") + ".");
        }
      });
      // A yield with nothing to give and no denial is a no-op that also
      // swallows the interaction - the shape gap #25 was about.
      if (effect.type === "yield" && !String(effect.produces || "").trim()
        && !Number(effect.denied_damage) && !String(effect.denied_message || "").trim()) {
        out.push(where + ": it produces nothing and has no denial damage or message, so "
          + "right-clicking the horse would do nothing at all - and still not mount it.");
      }
      if (effect.type === "glow" && !Number(effect.light) && !(effect.parts || []).length) {
        out.push(where + ": light is 0 and no parts glow, so it does nothing.");
      }
      if (simpleCondition(effect.when) === null) {
        out.push(where + ": its \"when\" is a nested condition this editor cannot show. "
          + "It is preserved exactly as loaded - edit it in the JSON.");
      }
    });
    if (spec.rarity && RARITIES.indexOf(spec.rarity) < 0) {
      out.push("Rarity must be one of " + RARITIES.join(", ") + " - got \"" + spec.rarity + "\".");
    }
    // An all-zero splice table is a load error, not a no-op: the game reads the
    // key as present and then finds no combination it may roll.
    if (spec.splice && Object.keys(spec.splice).length && !spliceIsSet(spec)) {
      out.push("Every splice share is zero, so the splice carrot could never land on this "
        + "gene. Give one combination a share, or turn the splice table off.");
    }
    (((spec.carrot || {}).flavour) || []).forEach(function (f, i) {
      if (f && f.trim() && !/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(f.trim())) {
        out.push("Carrot flavour " + (i + 1) + " (\"" + f + "\") is not an item id - "
          + "it wants \"namespace:path\", e.g. \"minecraft:sugar\".");
      }
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
    knobUses: knobUses,
    effectsOf: effectsOf,
    newEffect: newEffect,
    tidyEffect: tidyEffect,
    simpleCondition: simpleCondition,
    buildCondition: buildCondition,
    RARITIES: RARITIES,
    DEFAULT_RARITY: DEFAULT_RARITY,
    defaultCarrot: defaultCarrot,
    carrotIsDefault: carrotIsDefault,
    spliceIsSet: spliceIsSet
  };
})(window.HG);
