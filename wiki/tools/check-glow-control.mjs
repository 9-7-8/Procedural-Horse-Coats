#!/usr/bin/env node
// The gene creator's glow control, driven the way a person drives it.
//
//   node wiki/tools/check-glow-control.mjs
//
// A layer's glow is the one thing in the format the creator cannot show you:
// the preview is a flat sheet with no lighting to be brighter than, so a glow
// that is written wrongly looks exactly like a glow that is written correctly.
// That is how it came to be dropped from the export entirely - loading a
// glowing gene and exporting it again silently put the light out, and nothing
// in the tool could have told you.
//
// So this checks the parts that ARE observable: that the control writes what it
// says, that the value survives the export tidy (which drops anything equal to
// a default and is where the glow was being lost), and that an impossible level
// is named before the file reaches the game rather than after.
//
// Skipped, not failed, when there is no Chrome: this is a check you run, not a
// gate anything depends on.

import { open, checks, findChrome, sleep } from "./chrome.mjs";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const PAGE = pathToFileURL(resolve("wiki/gene-creator/index.html")).href;

if (!findChrome()) {
  console.log("no Chrome found - skipping the glow check (set $CHROME to run it)");
  process.exit(0);
}

const page = await open(PAGE, {
  readyWhen: `!!(window.HG && HG.ui && document.querySelector("#layers-panel select"))`
});
const c = checks();
const LAYER = `HG.ui.state.spec.expressions[0].layers[0]`;

try {
  // Glow is magical-phase only - pigment does not glow, and the loader refuses
  // it - so the gene has to be made magical through its own control first.
  const phase = await page.evaluate(`(function () {
    var sels = Array.prototype.slice.call(document.querySelectorAll("#gene-panel select"));
    for (var i = 0; i < sels.length; i++) {
      var opts = Array.prototype.map.call(sels[i].options, function (o) { return o.value; });
      if (opts.indexOf("magical") >= 0) {
        sels[i].value = "magical";
        sels[i].dispatchEvent(new Event("change", { bubbles: true }));
        return "set";
      }
    }
    return "no phase control";
  })()`);
  c.ok("the gene can be made magical", phase === "set", phase);
  await sleep(500);

  await page.evaluate(`(function () {
    HG.ui.state.selectedLayer = 0;
    var t = document.querySelector("#layers-panel .layer-title");
    if (t && !document.querySelector("#layers-panel .layer.open")) t.click();
  })()`);
  await sleep(400);

  const ticked = await page.evaluate(`(function () {
    var l = Array.prototype.filter.call(document.querySelectorAll("#layers-panel label"),
      function (x) { return x.textContent.indexOf("glows") >= 0; })[0];
    if (!l) return "no glow control on a magical layer";
    l.querySelector("input").click();
    return JSON.stringify(${LAYER}.emissive);
  })()`);
  // A layer somebody ticks almost always wants all of it; the number is there
  // to turn down, which is why ticking writes 1 rather than opening at zero.
  c.ok("ticking 'glows' writes a full glow", ticked === "1", ticked);
  await sleep(400);

  c.ok("and it reaches the export",
    (await page.evaluate(`document.getElementById("json-output").value`)).indexOf('"emissive"') >= 0);

  // The export drops anything equal to its default. A glow is not a default,
  // and this is the exact step that used to lose it.
  const dimmed = await page.evaluate(`(function () {
    ${LAYER}.emissive = 0.35;
    return JSON.stringify(HG.specModel.tidy(HG.ui.state.spec).expressions[0].layers[0].emissive);
  })()`);
  c.ok("a dimmed glow survives the export tidy", dimmed === "0.35", dimmed);

  const knob = await page.evaluate(`(function () {
    ${LAYER}.emissive = "$burn";
    return JSON.stringify(HG.specModel.tidy(HG.ui.state.spec).expressions[0].layers[0].emissive);
  })()`);
  c.ok("so does one driven by a knob", knob === '"$burn"', knob);

  const off = await page.evaluate(`(function () {
    delete ${LAYER}.emissive;
    return HG.specModel.tidy(HG.ui.state.spec).expressions[0].layers[0].emissive === undefined;
  })()`);
  c.ok("and a layer that does not glow writes no key at all", off === true);

  // The loader's own refusals, said by the validator first. The message a
  // person reads at midnight is better here than after a restart.
  const flagged = await page.evaluate(`(function () {
    ${LAYER}.emissive = 4;
    var out = HG.specModel.problems(HG.ui.state.spec)
      .filter(function (x) { return x.indexOf("full bright") >= 0; });
    ${LAYER}.emissive = 1;
    return out.length;
  })()`);
  c.ok("an out-of-range level is flagged before export", flagged === 1, String(flagged));

  const natural = await page.evaluate(`(function () {
    var was = HG.ui.state.spec.phase;
    HG.ui.state.spec.phase = "natural";
    var out = HG.specModel.problems(HG.ui.state.spec)
      .filter(function (x) { return x.indexOf("Pigment does not") >= 0; });
    HG.ui.state.spec.phase = was;
    return out.length;
  })()`);
  c.ok("so is a glow on a natural gene", natural === 1, String(natural));

  c.ok("nothing on the console", page.errors().length === 0, page.errors().join(" ;; "));
} finally {
  page.close();
}

process.exit(c.report("glow control"));
