#!/usr/bin/env node
// The gene creator's step bar: does every step show something to fill in, beside
// the horse, and does "show everything" still give back the old three columns?
//
//   node wiki/tools/check-creator-steps.mjs [--shot out.png]
//
// Run it after touching wiki/gene-creator/index.html, creator.css or js/ui.js.
// The steps are a CSS filter over panels that are otherwise unchanged, so the
// failure this catches is the quiet one: a block tagged with the wrong step, and
// a step that comes up empty.

import { open, checks, findChrome, sleep } from "./chrome.mjs";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const PAGE = pathToFileURL(resolve("wiki/gene-creator/index.html")).href;
const SHOT = process.argv.includes("--shot")
  ? process.argv[process.argv.indexOf("--shot") + 1] : null;

if (!findChrome()) {
  console.log("no Chrome found - skipping the creator steps check (set $CHROME to run it)");
  process.exit(0);
}

const page = await open(PAGE, { port: 9224 });
const c = checks();

// How many form controls are actually on screen in the visible side column.
const VISIBLE_CONTROLS = `(function () {
  var cols = Array.prototype.filter.call(document.querySelectorAll(".column.left, .column.right"),
    function (col) { return !col.hidden; });
  var n = 0;
  cols.forEach(function (col) {
    col.querySelectorAll("input, select, textarea, button").forEach(function (el) {
      if (el.offsetParent !== null) n++;
    });
  });
  return { columns: cols.length, controls: n,
           centre: !!document.querySelector(".column.centre").offsetParent };
})()`;

try {
  for (let i = 0; i < 40; i++) {
    if (await page.evaluate(`!!(window.HG && HG.ui && HG.ui.state && HG.ui.state.spec)`).catch(() => false)) break;
    await sleep(250);
  }
  c.ok("the creator started with a gene loaded",
    await page.evaluate(`!!(HG.ui.state.spec)`), "");

  const steps = await page.evaluate(`HG.ui.steps.length`);
  for (let n = 1; n <= steps; n++) {
    await page.evaluate(`HG.ui.goToStep(${n})`);
    await sleep(100);
    const v = await page.evaluate(VISIBLE_CONTROLS);
    const title = await page.evaluate(`HG.ui.steps[${n - 1}].title`);
    c.ok(`step ${n} (${title}) shows one side column beside the horse`, v.columns === 1 && v.centre,
      JSON.stringify(v));
    c.ok(`step ${n} (${title}) has something to fill in`, v.controls > 0, v.controls + " controls");
  }

  await page.evaluate(`(function () {
    HG.ui.state.showAll = true;
    document.querySelector("#stepper input[type=checkbox]").click();
  })()`);
  await sleep(100);
  // The click toggled it back off; set it explicitly the way the checkbox does.
  await page.evaluate(`(function () {
    var cb = document.querySelector("#stepper input[type=checkbox]");
    if (!cb.checked) cb.click();
  })()`);
  await sleep(100);
  const all = await page.evaluate(VISIBLE_CONTROLS);
  c.ok("show everything brings back both side columns", all.columns === 2, JSON.stringify(all));

  c.ok("the export is still written", (await page.evaluate(`document.getElementById("json-output").value.length`)) > 50, "");
  c.ok("nothing on the console", page.errors().length === 0, page.errors().join(" ;; "));
  if (SHOT) await page.screenshot(SHOT);
} finally {
  page.close();
}

process.exit(c.report("creator steps"));
