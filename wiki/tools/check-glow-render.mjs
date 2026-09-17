#!/usr/bin/env node
// The lights-out view, and the one API call underneath it that was a guess.
//
//   node wiki/tools/check-glow-render.mjs [--shot out.png]
//
// A glow is drawn by handing the coat's own colours to the material as an
// EMISSIVE MAP: three adds an emissive map on top of the lit colour without
// attenuating it by the lights, which is what lets turning the lights down
// leave exactly the glowing texels standing. That mirrors what the mod does in
// game with RenderTypes.eyes at FULL_BRIGHT.
//
// Two things about that were worth asserting rather than trusting:
//
//   1. emissiveMap on r128's MeshLambertMaterial (the horse designer) and
//      MeshStandardMaterial (the gene pages, the breed designer, the LUT lab).
//      Long-standing in three, but it is pinned to one CDN revision here, and a
//      material that silently drops the property fails INVISIBLY - the page
//      renders a perfectly good horse that simply never lights up. Both
//      viewport.js and scene.js carry a comment pointing at this check.
//
//   2. The control itself latches and reaches the scene.
//
// Deliberately shallow, the same way check-designer-boots.mjs is: it asserts
// the glow can be expressed and the switch is wired, NOT that any given gene
// glows the right shape. The coat goldens own correctness.
//
// Skipped, not failed, when there is no Chrome: this is a check you run, not a
// gate anything depends on.

import { open, checks, findChrome, sleep } from "./chrome.mjs";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const PAGE = pathToFileURL(resolve("wiki/horse-designer/index.html")).href;
const SHOT = process.argv.includes("--shot")
  ? process.argv[process.argv.indexOf("--shot") + 1] : null;

if (!findChrome()) {
  console.log("no Chrome found - skipping the glow render check (set $CHROME to run it)");
  process.exit(0);
}

const page = await open(PAGE, { port: 9224 });
const c = checks();

try {
  // The wasm instantiates asynchronously - wait for the designer to publish
  // itself rather than for a fixed time.
  for (let i = 0; i < 40; i++) {
    if (await page.evaluate(`!!(window.HG && HG.designer)`).catch(() => false)) break;
    await sleep(500);
  }

  // ---- 1. the API that was flagged unverified -------------------------
  const mats = await page.evaluate(`(function () {
    if (!window.THREE) return JSON.stringify({ error: "no THREE" });
    var tex = new THREE.CanvasTexture(document.createElement("canvas"));
    var lam = new THREE.MeshLambertMaterial({ emissive: 0xffffff, emissiveMap: tex });
    var std = new THREE.MeshStandardMaterial({ emissive: 0xffffff, emissiveMap: tex });
    return JSON.stringify({
      revision: THREE.REVISION,
      lambert: !!lam.emissiveMap,
      standard: !!std.emissiveMap,
      // The colour has to survive too - it multiplies the map, so an emissive
      // that silently stayed black would put the light out just as completely.
      lambertEmissive: lam.emissive ? lam.emissive.getHexString() : null,
      standardEmissive: std.emissive ? std.emissive.getHexString() : null
    });
  })()`);
  const m = JSON.parse(mats || "{}");
  c.ok("three honours emissiveMap on a Lambert material (the horse designer)",
    m.lambert === true, mats);
  c.ok("and on a Standard material (gene pages, breed designer, LUT lab)",
    m.standard === true, mats);
  c.ok("the emissive colour survives construction on both",
    m.lambertEmissive === "ffffff" && m.standardEmissive === "ffffff", mats);

  // ---- 2. the scene actually took a glow ------------------------------
  // scene.js keeps its instance private inside start(), so this goes through
  // the control a person uses rather than reaching for the object.
  const before = await page.evaluate(`(function () {
    var b = document.getElementById("lights");
    return b ? b.getAttribute("aria-pressed") : "absent";
  })()`);
  c.ok("the lights-out button is on the page, and starts on", before === "false", before);

  const after = await page.evaluate(`(function () {
    var b = document.getElementById("lights");
    if (!b) return "absent";
    b.click();
    return b.getAttribute("aria-pressed");
  })()`);
  c.ok("clicking it latches, and setNight did not throw", after === "true", after);
  await sleep(600);

  const back = await page.evaluate(`(function () {
    var b = document.getElementById("lights");
    if (!b) return "absent";
    b.click();
    return b.getAttribute("aria-pressed");
  })()`);
  c.ok("and it comes back on", back === "false", back);

  c.ok("nothing on the console", page.errors().length === 0, page.errors().join(" ;; "));
  if (SHOT) await page.screenshot(SHOT);
} finally {
  page.close();
}

// ---- 3. the data path, end to end, on a real gene page ------------------
//
// The strongest assertion available without reading a WebGL buffer back. The
// gene pages reveal their lights button ONLY when a bake returns glow texels,
// so the button un-hiding proves the whole chain: the new @JSExport, the wasm,
// the unpack, the DOM. And a gene that does not glow must leave it hidden, or
// the gate is not a gate and the button was simply always there.
async function genePage(file, label) {
  const p = await open(pathToFileURL(resolve(file)).href, { port: 9225 });
  try {
    for (let i = 0; i < 40; i++) {
      const up = await p.evaluate(`!!document.querySelector(".gene-preview .gp-bar")`)
        .catch(() => false);
      if (up) break;
      await sleep(500);
    }
    return {
      built: await p.evaluate(`!!document.querySelector(".gene-preview .gp-bar")`),
      lights: await p.evaluate(`(function () {
        var b = document.querySelector(".gp-lights");
        return b ? (b.hidden ? "hidden" : "shown") : "absent";
      })()`),
      errors: p.errors().join(" ;; ")
    };
  } finally {
    p.close();
  }
}

// tron glows - four layers at 0.25 / "$burn".
const glows = await genePage("wiki/gene-tron.html", "tron");
c.ok("a gene page builds its preview", glows.built === true, JSON.stringify(glows));
c.ok("and a GLOWING gene reveals the lights button - so coatGlowOf returned texels",
  glows.lights === "shown", glows.lights);
c.ok("nothing on a glowing gene page's console", glows.errors === "", glows.errors);

// agouti is an ordinary pigment locus - a natural gene, which cannot glow at all.
const dark = await genePage("wiki/gene-agouti.html", "agouti");
c.ok("a gene that cannot glow leaves the button hidden", dark.lights === "hidden", dark.lights);

process.exit(c.report("glow render"));
