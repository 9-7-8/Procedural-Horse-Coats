#!/usr/bin/env node
// The gene creator's colour control, driven the way a person drives it.
//
//   node wiki/tools/check-colour-control.mjs
//
// A colour op's fork is exclusive and it is invisible in the file:
// SpecPainter.solidColour paints `color` while `hue` is below zero, and the HSL
// triple the moment it is not. The tool used to show a live, editable colour
// well beside three parameters that silently overruled it - so a gene could be
// authored with a colour that was never read, and nothing said a word.
//
// It also covers the repair underneath that control. "Random" used to write an
// inline {min,max} range, which the format accepts (GeneSpecParser hoists one
// into an anonymous inline#N knob) but which this tool's own engine never
// learned to resolve: drawValues walks spec.knobs only, so the preview drew the
// parameter's FALLBACK while the game drew a real range. Random now declares a
// named knob instead, which previews, shows up in "What this horse drew", and
// can be pointed at twice.
//
// What is asserted is the SPEC, not the picture. Every check below reads back
// what the control wrote, because "it looked right" is exactly the evidence
// that was available while the old behaviour was wrong.
//
// Skipped, not failed, when there is no Chrome: this is a check you run, not a
// gate anything depends on.

import { open, checks, findChrome, sleep } from "./chrome.mjs";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const PAGE = pathToFileURL(resolve("wiki/gene-creator/index.html")).href;

if (!findChrome()) {
  console.log("no Chrome found - skipping the colour control check (set $CHROME to run it)");
  process.exit(0);
}

const page = await open(PAGE, {
  readyWhen: `!!(window.HG && HG.ui && document.querySelector("#layers-panel select"))`,
  port: 9226
});
const c = checks();

const OP = `HG.ui.state.spec.expressions[0].layers[0].op`;

const ROW_LABELS = `(function () {
  return Array.prototype.map.call(
    document.querySelectorAll("#layers-panel .field > .field-label"),
    function (s) { return s.textContent.trim(); });
})()`;

// Set the <select> within `scope` that offers `value`, as a person would.
function pick(scope, value) {
  return `(function () {
    var sels = Array.prototype.slice.call(document.querySelectorAll(${JSON.stringify(scope)}));
    for (var i = 0; i < sels.length; i++) {
      var opts = Array.prototype.map.call(sels[i].options, function (o) { return o.value; });
      if (opts.indexOf(${JSON.stringify(value)}) >= 0) {
        sels[i].value = ${JSON.stringify(value)};
        sels[i].dispatchEvent(new Event("change", { bubbles: true }));
        return "set";
      }
    }
    return "not found";
  })()`;
}

try {
  // The colour ops are magical-phase, so the gene has to be made magical first.
  await page.evaluate(pick("#gene-panel select", "magical"));
  await sleep(400);
  await page.evaluate(`(function () {
    HG.ui.state.selectedLayer = 0;
    var t = document.querySelector("#layers-panel .layer-title");
    if (t && !document.querySelector("#layers-panel .layer.open")) t.click();
  })()`);
  await sleep(400);

  c.ok("a layer can be given a TOWARD op",
    (await page.evaluate(pick("#layers-panel select", "TOWARD"))) === "set");
  await sleep(400);

  let labels = await page.evaluate(ROW_LABELS);
  c.ok("the op form shows one Colour row", labels.filter((l) => l === "Colour").length === 1,
    JSON.stringify(labels));
  // The whole point: the three that overrule the well are inside it, not beside
  // it looking like unrelated numbers.
  c.ok("with no loose hue/saturation/lightness rows beside it",
    !labels.includes("hue") && !labels.includes("saturation") && !labels.includes("lightness"),
    JSON.stringify(labels));
  c.ok("and the well is live while hue is the -1 sentinel",
    (await page.evaluate(`${OP}.hue`)) === -1);

  c.ok("the control offers a varying mode",
    (await page.evaluate(pick("#layers-panel .field select", "varies"))) === "set");
  await sleep(400);
  c.ok("switching to HSL lifts hue off the sentinel so the triple is read",
    (await page.evaluate(`${OP}.hue`)) === 0);

  labels = await page.evaluate(ROW_LABELS);
  c.ok("and the triple appears, folded into the colour block",
    labels.includes("hue") && labels.includes("saturation") && labels.includes("lightness"),
    JSON.stringify(labels));

  // Random must declare a REAL knob. An inline range would preview as the
  // fallback - the defect this control was built on top of.
  const picked = await page.evaluate(`(function () {
    var fields = Array.prototype.slice.call(document.querySelectorAll("#layers-panel .field"));
    for (var i = 0; i < fields.length; i++) {
      var lab = fields[i].querySelector(".field-label");
      if (!lab || lab.textContent.trim() !== "hue") continue;
      var sel = fields[i].querySelector("select");
      if (!sel) return "no mode select";
      sel.value = "random";
      sel.dispatchEvent(new Event("change", { bubbles: true }));
      return "picked";
    }
    return "no hue field";
  })()`);
  c.ok("the hue can be set to vary", picked === "picked", picked);
  await sleep(500);

  const drawn = JSON.parse(await page.evaluate(`(function () {
    var spec = HG.ui.state.spec;
    var values = HG.specEngine.drawValues(spec, 4242, 99, 1);
    var knob = spec.knobs.filter(function (k) { return "$" + k.name === ${OP}.hue; })[0] || null;
    return JSON.stringify({
      hue: ${OP}.hue,
      knob: knob,
      resolved: HG.specEngine.resolveValue(values, ${OP}.hue, -1, -1)
    });
  })()`));
  c.ok("which writes a knob reference, not an inline range",
    typeof drawn.hue === "string" && drawn.hue.charAt(0) === "$", JSON.stringify(drawn.hue));
  c.ok("the knob is named after the parameter", !!drawn.knob && /^hue/.test(drawn.knob.name),
    drawn.knob && drawn.knob.name);
  // The sentinel is -1, so a knob spanning ui.min would be a range of "not set".
  c.ok("and spans 0..360 rather than the -1 sentinel",
    drawn.knob && drawn.knob.min === 0 && drawn.knob.max === 360,
    drawn.knob && `${drawn.knob.min}..${drawn.knob.max}`);
  c.ok("the preview resolves it to a drawn hue instead of the fallback",
    typeof drawn.resolved === "number" && drawn.resolved >= 0 && drawn.resolved <= 360,
    drawn.resolved);

  const painted = JSON.parse(await page.evaluate(`(function () {
    var cells = Array.prototype.slice.call(document.querySelectorAll("#layers-panel .swatch-cell"));
    return JSON.stringify({
      cells: cells.length,
      painted: cells.filter(function (x) {
        var bg = getComputedStyle(x).backgroundColor;
        return bg && bg !== "rgba(0, 0, 0, 0)" && bg !== "transparent";
      }).length
    });
  })()`));
  c.ok("the range strip shows the span the knob can draw", painted.painted >= 2,
    `${painted.painted}/${painted.cells} painted`);

  c.ok("the control goes back to a fixed colour",
    (await page.evaluate(pick("#layers-panel .field select", "fixed"))) === "set");
  await sleep(400);
  c.ok("restoring the sentinel so `color` is read again",
    (await page.evaluate(`${OP}.hue`)) === -1);

  // A ramp's fork is the stop list rather than the sentinel - same question.
  c.ok("a layer can be given a RAMP op",
    (await page.evaluate(pick("#layers-panel select", "RAMP"))) === "set");
  await sleep(400);
  labels = await page.evaluate(ROW_LABELS);
  c.ok("a ramp shows one Colours row", labels.filter((l) => l === "Colours").length === 1,
    JSON.stringify(labels));
  c.ok("starting with named stops",
    JSON.parse(await page.evaluate(`JSON.stringify(${OP}.colors)`)).length >= 2);

  await page.evaluate(pick("#layers-panel .field select", "varies"));
  await sleep(400);
  c.ok("switching to a swept hue empties the stop list, which is how the engine reads it",
    JSON.parse(await page.evaluate(`JSON.stringify(${OP}.colors)`)).length === 0);

  await page.evaluate(pick("#layers-panel .field select", "fixed"));
  await sleep(400);
  c.ok("and switching back restores a usable pair, not an empty list the loader refuses",
    JSON.parse(await page.evaluate(`JSON.stringify(${OP}.colors)`)).length === 2);

  c.ok("the gene still validates clean",
    JSON.parse(await page.evaluate(`JSON.stringify(HG.specModel.problems(HG.ui.state.spec))`)).length === 0);
  c.ok("nothing on the console", page.errors().length === 0, page.errors().join(" | "));
} finally {
  await page.close();
}

process.exit(c.report("colour control"));
