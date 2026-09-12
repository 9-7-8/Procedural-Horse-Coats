#!/usr/bin/env node
// The horse designer really runs common/ as WebAssembly. Does it still boot?
//
//   node wiki/tools/check-designer-boots.mjs [--shot out.png]
//
// Run this after :web:bakeDesignerAssets, which is to say after any change to
// common/ or web/. TeaVM compiles only the REACHABLE graph, so a new call in
// common/ can pull in a JDK method TeaVM does not have - and the failure is at
// INSTANTIATION, not at compile time. The Gradle task goes green, the wasm is
// written, and the page comes up blank.
//
// That is the shape of known-gaps gap 199 (nothing gates :web) seen from the
// other end: the build not being gated is one problem, and the build passing
// while the artefact is broken is the other. This covers the second.
//
// Deliberately shallow - it asserts the engine answered and a horse was drawn,
// not that the horse is right. The coat goldens own correctness; this owns
// "the browser can run the mod at all".

import { open, checks, findChrome, sleep } from "./chrome.mjs";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const PAGE = pathToFileURL(resolve("wiki/horse-designer/index.html")).href;
const SHOT = process.argv.includes("--shot")
  ? process.argv[process.argv.indexOf("--shot") + 1] : null;

if (!findChrome()) {
  console.log("no Chrome found - skipping the designer check (set $CHROME to run it)");
  process.exit(0);
}

const page = await open(PAGE, { port: 9223 });
const c = checks();

try {
  // The wasm instantiates asynchronously; wait for the designer to publish
  // itself rather than for a fixed time.
  for (let i = 0; i < 40; i++) {
    if (await page.evaluate(`!!(window.HG && HG.designer)`).catch(() => false)) break;
    await sleep(500);
  }

  const surface = await page.evaluate(`Object.keys(window.HG || {}).sort().join(",")`);
  c.ok("the designer published itself", (surface || "").includes("designer"), surface);
  c.ok("the wasm bridge is there", (surface || "").includes("java"), surface);

  // Something on screen. The 3D view is a WebGL canvas and cannot be read back
  // this way; the GUI overlay beside it is an ordinary 2D one, and it is drawn
  // from the same genome - a gene list, the horse's name, its genotype code -
  // so if the engine never answered, it stays empty.
  //
  // Polled rather than sampled once: the scene and the overlay are painted a
  // frame or two after the designer publishes itself, and measuring in between
  // reads zero from a page that is perfectly healthy.
  let painted = 0;
  for (let i = 0; i < 20; i++) {
    painted = await page.evaluate(`(function () {
      var best = 0;
      document.querySelectorAll("canvas").forEach(function (el) {
        try {
          var d = el.getContext("2d").getImageData(0, 0, el.width, el.height).data;
          var n = 0;
          for (var i = 3; i < d.length; i += 4) if (d[i] !== 0) n++;
          if (n > best) best = n;
        } catch (e) { /* WebGL canvas - not readable, and not the one we mean */ }
      });
      return best;
    })()`);
    if (painted > 10000) break;
    await sleep(500);
  }
  c.ok("the overlay was drawn from a real genome", painted > 10000, painted + " opaque pixels");

  c.ok("nothing on the console", page.errors().length === 0, page.errors().join(" ;; "));
  if (SHOT) await page.screenshot(SHOT);
} finally {
  page.close();
}

process.exit(c.report("designer boot"));
