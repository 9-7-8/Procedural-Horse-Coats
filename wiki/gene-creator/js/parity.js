// Does the creator's JavaScript engine still agree with the game's Java one?
//
// The preview is a port: js/spec-engine.js mirrors SpecPainter, js/noise.js
// mirrors BodyNoise, js/fields.js mirrors the composer, and the knob draw
// mirrors java.util.Random. A port drifts silently, and a drifted preview is
// worse than none - it shows a horse the game will not breed, and it looks
// right while doing it.
//
// This file is the single implementation of that check. It is pure (no DOM, no
// filesystem), so BOTH callers run the same code:
//
//   tools/check-parity.mjs  - the terminal gate, reading the fixtures off disk
//   the creator itself      - a self-check on boot, so the tool cannot quietly
//                             show a stale preview because someone edited js/
//                             and forgot to run the script
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  var TOLERANCE = 1e-4;

  function javaHashCode(s) {
    var h = 0;
    for (var i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
    return h;
  }

  /** SeededRng(seed, namespace) = new Random(seed ^ (namespace.hashCode() * K1)). */
  function seededRngSeed(seed, namespace) {
    var n = HG.noise;
    return n.xor(n.u64(seed < 0 ? 0xFFFFFFFF : 0, seed >>> 0),
      n.mul(n.fromInt(javaHashCode(namespace)), n.K1));
  }

  /**
   * The schema tables must agree BEFORE anything else is worth checking: the
   * creator omits any setting left at its default, so a default that differs
   * between js/schema.js and SpecSchema.java writes a file that plays
   * differently from how it previewed - and only for the settings you never
   * touched, which is the hardest kind of bug to see. (This found exactly that
   * on AXIS.to, DILUTE.keepBlack, DILUTE.blackTint and TOWARD.strength.)
   */
  // The face fractions SpecFixtureTool.geometrySection() bakes, in its order.
  // Kept in step by hand, which is safe because a mismatch shows up as a wrong
  // answer rather than a silent pass: the probes are asymmetric.
  var PROBES = [[0, 0], [1, 0], [0, 1], [1, 1], [0.5, 0.5], [0.37, 0.81]];

  // Both languages use IEEE-754 doubles and compute this in the same order, so
  // the agreement is exact rather than approximate. The epsilon is only for the
  // 1e-6 rounding the fixture applies to stay readable - it is far tighter than
  // any real disagreement would be, and a genuine axis or sign error is orders
  // of magnitude bigger than it.
  function comparePoint(fail, what, want, got) {
    var g = [got.x, got.y, got.z];
    for (var i = 0; i < 3; i++) {
      if (Math.abs(want[i] - g[i]) > 1e-6) {
        fail(what + ": the game gives [" + want.join(", ") + "], the creator ["
          + g.map(function (v) { return Math.round(v * 1e6) / 1e6; }).join(", ") + "]");
        return;
      }
    }
  }

  function checkParams(fail, count, kind, type, javaParams, jsParams) {
    var jsByName = {};
    jsParams.forEach(function (p) { jsByName[p.name] = p; });
    Object.keys(javaParams).forEach(function (name) {
      var jp = javaParams[name];
      var p = jsByName[name];
      if (!p) {
        fail(kind + " " + type + ": the game accepts " + name + " but the creator does not offer it");
        return;
      }
      count();
      if (jp.kind !== p.kind) {
        fail(kind + " " + type + "." + name + ": kind " + jp.kind + " in Java, "
          + p.kind + " in the creator");
      }
      if (jp.kind === "VALUE" && Math.abs(Number(jp.fallback) - p.fallback) > 1e-9) {
        fail(kind + " " + type + "." + name + ": default " + jp.fallback + " in Java, "
          + p.fallback + " in the creator");
      }
      if (jp.choices && String(jp.choices) !== String(p.choices || [])) {
        fail(kind + " " + type + "." + name + ": choices [" + jp.choices + "] in Java, ["
          + p.choices + "] in the creator");
      }
    });
    Object.keys(jsByName).forEach(function (name) {
      if (!(name in javaParams)) {
        fail(kind + " " + type + ": the creator offers " + name + " but the game ignores it");
      }
    });
  }

  /**
   * The same comparison for an effect verb. Effects carry a wider set of kinds
   * than masks and ops (STRING, TRIGGER, PARTS, BOOL as well as NUMBER / CHOICE
   * / COLOR), and some parameters are required rather than defaulted - which the
   * creator has to know, since a missing required key is a load error rather
   * than a quiet default.
   */
  function checkEffectParams(fail, count, verb, javaParams, jsParams) {
    var jsByName = {};
    jsParams.forEach(function (p) { jsByName[p.name] = p; });
    Object.keys(javaParams).forEach(function (name) {
      var jp = javaParams[name];
      var p = jsByName[name];
      if (!p) {
        fail("effect " + verb + ": the game accepts " + name + " but the creator does not offer it");
        return;
      }
      count();
      if (jp.kind !== p.kind) {
        fail("effect " + verb + "." + name + ": kind " + jp.kind + " in Java, "
          + p.kind + " in the creator");
      }
      if (jp.required !== !!p.required) {
        fail("effect " + verb + "." + name + ": required=" + jp.required + " in Java, "
          + !!p.required + " in the creator");
      }
      // TRIGGER and PARTS fallbacks are structures, not scalars; the baked
      // table does not carry them, so there is nothing to compare.
      if ("fallback" in jp && jp.kind !== "TRIGGER" && jp.kind !== "PARTS") {
        var want = jp.fallback, got = p.fallback;
        var same = (typeof want === "number")
          ? (typeof got === "number" && Math.abs(want - got) < 1e-9)
          : String(want) === String(got);
        if (!same) {
          fail("effect " + verb + "." + name + ": default " + JSON.stringify(want)
            + " in Java, " + JSON.stringify(got) + " in the creator");
        }
      }
      if (jp.choices && String(jp.choices) !== String(p.choices || [])) {
        fail("effect " + verb + "." + name + ": choices [" + jp.choices + "] in Java, ["
          + (p.choices || []) + "] in the creator");
      }
    });
    Object.keys(jsByName).forEach(function (name) {
      if (!(name in javaParams)) {
        fail("effect " + verb + ": the creator offers " + name + " but the game ignores it");
      }
    });
  }

  /**
   * opts: { fixtures, specs } - the baked expected.json, and the example genes
   * keyed by the file name each fixture case names.
   * Returns { checked, failures, cases }.
   */
  function run(opts) {
    var fixtures = (opts && opts.fixtures) || {};
    var specs = (opts && opts.specs) || {};
    var failures = [];
    var checked = 0;
    function fail(m) { failures.push(m); }
    function count() { checked++; }

    if (fixtures.schema) {
      var schema = fixtures.schema;
      Object.keys(schema.masks).forEach(function (type) {
        if (!HG.schema.MASKS[type]) {
          fail("mask " + type + " exists in the game but not in the creator");
        } else {
          checkParams(fail, count, "mask", type, schema.masks[type], HG.schema.MASKS[type].params);
        }
      });
      Object.keys(schema.ops).forEach(function (type) {
        var js = HG.schema.OPS[type];
        if (!js) {
          fail("op " + type + " exists in the game but not in the creator");
          return;
        }
        if (js.phase !== schema.ops[type].phase) {
          fail("op " + type + ": phase " + schema.ops[type].phase + " in Java, "
            + js.phase + " in the creator");
        }
        checkParams(fail, count, "op", type, schema.ops[type].params, js.params);
      });
      Object.keys(HG.schema.MASKS).forEach(function (type) {
        if (!(type in schema.masks)) {
          fail("the creator offers mask " + type + ", which the game does not have");
        }
      });
      Object.keys(HG.schema.OPS).forEach(function (type) {
        if (!(type in schema.ops)) {
          fail("the creator offers op " + type + ", which the game does not have");
        }
      });

      // The effect vocabulary, on the same terms. Effects do not paint, so the
      // probe cases below say nothing about them - this table comparison is the
      // whole of their net, which is why it checks required-ness as well.
      if (schema.effects) {
        Object.keys(schema.effects).forEach(function (verb) {
          var js = HG.schema.EFFECTS[verb];
          if (!js) {
            fail("effect " + verb + " exists in the game but not in the creator");
            return;
          }
          checkEffectParams(fail, count, verb, schema.effects[verb], js.params);
        });
        Object.keys(HG.schema.EFFECTS).forEach(function (verb) {
          if (!(verb in schema.effects)) {
            fail("the creator offers effect " + verb + ", which the game does not have");
          }
        });
        count();
        if (String(schema.conditionFlags) !== String(HG.schema.CONDITION_FLAGS)) {
          fail("condition flags differ: the game has [" + schema.conditionFlags
            + "], the creator [" + HG.schema.CONDITION_FLAGS + "]");
        }
      }

      // The COMPOSER's arithmetic. The probe cases below run restrict() and
      // tint() and never compose(), so the alpha ramp and the shadow lift had
      // no net at all - which is where gap 50 lived: this file's mirror kept an
      // `rgb == 0` equality for months after the game replaced it with a ramp,
      // and once the gradient's black corner moved off #000000 the branch
      // stopped matching anything and every dark coat drew too dark.
      //
      // The composite itself cannot be compared here (it needs the gradient and
      // the template, and there is no image decoder in the harness). The
      // arithmetic can, and the arithmetic is what drifted.
      if (schema.composer) {
        var comp = schema.composer;
        if (comp.shadowFloor !== HG.fields.SHADOW_FLOOR) {
          fail("SHADOW_FLOOR differs: the game has " + comp.shadowFloor
            + ", the creator " + HG.fields.SHADOW_FLOOR);
        }
        count();
        Object.keys(comp.nearBlackAlpha || {}).forEach(function (hex) {
          var want = comp.nearBlackAlpha[hex];
          var got = HG.fields.nearBlackAlpha(parseInt(hex, 16));
          if (got !== want) {
            fail("nearBlackAlpha(#" + hex + "): the game gives " + want
              + ", the creator " + got);
          }
          count();
        });
        Object.keys(comp.liftShadows || {}).forEach(function (hex) {
          var want = comp.liftShadows[hex];
          var got = HG.fields.liftedForParity(parseInt(hex, 16));
          if (got !== want) {
            fail("liftShadows(#" + hex + "): the game gives #" + want
              + ", the creator #" + got);
          }
          count();
        });
      }

      // ---- the posed mesh -------------------------------------------------
      //
      // geometry.js's posed()/posedNormal() against HorseSkinGeometry's. This
      // was gap 100: the posed walk lived here AND in model3d.js's emitPart,
      // in two languages, in two files that were not obviously a pair, and
      // nothing compared them - so a change to how a part is posed could reach
      // the baked gene icons and not the browser preview, or the reverse.
      //
      // The probes deliberately include face centres and one asymmetric point,
      // not just corners: a corner is a fraction of 0 or 1 on both axes, and
      // several ways of getting the axis pairing wrong agree at every corner
      // and disagree everywhere else. That is the exact shape of the UV swap
      // that once hid behind a stale fixture for a day.
      if (schema.posed) {
        Object.keys(schema.posed).forEach(function (key) {
          var parts = key.split("|");
          var skin = parts[0], part = parts[1], face = parts[2];
          var want = schema.posed[key];

          var gotN = HG.geometry.posedNormal(skin, part, face);
          if (!gotN) {
            fail("posedNormal " + key + ": the creator has no such part");
          } else {
            comparePoint(fail, "posedNormal " + key, want.n, gotN);
          }
          count();

          want.p.forEach(function (w, i) {
            var probe = PROBES[i];
            var got = HG.geometry.posed(skin, part, face, probe[0], probe[1]);
            if (!got) {
              fail("posed " + key + ": the creator has no such part");
            } else {
              comparePoint(fail, "posed " + key + " at (" + probe[0] + ", " + probe[1] + ")",
                w, got);
            }
            count();
          });
        });
      }
    } else {
      fail("fixtures have no schema section - re-run ./gradlew :common:bakeSpecFixtures");
    }

    var cases = fixtures.cases || [];
    cases.forEach(function (c) {
      var spec = specs[c.spec];
      if (!spec) {
        fail("no such example gene: " + c.spec);
        return;
      }

      function report(what, index, want, got) {
        fail(c.spec + " seed=" + c.seed + " dose=" + c.dose + " " + c.skin + ": "
          + what + "[" + index + "] expected " + want + ", got " + got);
      }

      var s = seededRngSeed(c.seed, spec.key);
      var values = HG.specEngine.drawValues(spec, s.h, s.l, c.dose);

      // The combination table itself. The fixture records which outcome the
      // game resolved this combination to, so a creator that maps it to the
      // wrong expression fails here rather than previewing a different horse.
      var expression = HG.specEngine.expressionFor(spec, c.combination);
      count();
      if (!expression) {
        fail(c.spec + ": no expression covers " + c.combination);
        return;
      }
      if (expression.id !== c.expression) {
        fail(c.spec + " " + c.combination + ": the game resolves it to " + c.expression
          + ", the creator to " + expression.id);
        return;
      }
      var layers = expression.layers || [];

      // The knob draw - this is the java.util.Random port under test.
      (spec.knobs || []).forEach(function (knob, i) {
        var want = c.knobs[i];
        if (knob.type === "seed") {
          if (want !== "seed") report("knob", i, want, "seed");
          return;
        }
        want.forEach(function (w, leg) {
          var got = values.ranges[i][leg];
          if (Math.abs(Number(w) - got) > TOLERANCE) {
            report("knob " + knob.name + "." + leg, i, w, got);
          }
          count();
        });
      });

      // The painter.
      var N = HG.geometry.SHEET_SIZE;
      if (spec.phase !== "magical") {
        var after = HG.specEngine.restrict(spec, layers, values, c.skin, new HG.fields.PigmentField(N));
        c.probes.forEach(function (p, i) {
          var px = p[0], py = p[1], red = p[2], black = p[3];
          if (Math.abs(after.redAt(px, py) - Number(red)) > TOLERANCE) {
            report("red @" + px + "," + py, i, red, after.redAt(px, py));
          }
          if (Math.abs(after.blackAt(px, py) - Number(black)) > TOLERANCE) {
            report("black @" + px + "," + py, i, black, after.blackAt(px, py));
          }
          checked += 2;
        });
      } else {
        var colour = new HG.fields.ColorField(N);
        HG.geometry.forEachTexel(c.skin, function (px, py) { colour.setArgb(px, py, 0xFF404040); });
        var delta = HG.specEngine.tint(spec, layers, values, c.skin,
          new HG.fields.PigmentField(N), colour);
        c.probes.forEach(function (p, i) {
          var px = p[0], py = p[1];
          var got = [delta.redAt(px, py), delta.greenAt(px, py),
            delta.blueAt(px, py), delta.opacityAt(px, py)];
          [p[2], p[3], p[4], p[5]].forEach(function (want, k) {
            if (Math.abs(got[k] - Number(want)) > 1) {
              report("rgba" + k + " @" + px + "," + py, i, want, got[k]);
            }
            count();
          });
        });
      }
    });

    return { checked: checked, failures: failures, cases: cases.length };
  }

  HG.parity = { run: run, seededRngSeed: seededRngSeed, javaHashCode: javaHashCode };
})(window.HG);
