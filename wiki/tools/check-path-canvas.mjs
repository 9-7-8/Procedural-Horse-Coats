#!/usr/bin/env node
// The gene creator's PATH drawing canvas, driven the way a person drives it.
//
//   node wiki/tools/check-path-canvas.mjs [--shot out.png]
//
// The canvas is a second control over an array the textarea also edits, so the
// things that can break it are all invisible to a static check: a click that
// lands somewhere other than where it was aimed, a drag that moves the wrong
// handle, an insert that goes in at the wrong index, the two controls falling
// out of step. Every one of those still renders a perfectly good-looking page.
//
// It found its first bug on its first run and the bug was not in the canvas:
// every mask parameter is wrapped in a <label>, which forwards a click
// anywhere inside it to the first control it contains - so each click on the
// horse was also pressing the canvas's own Reverse button, and the drawing
// came out backwards one point at a time. That is the class of defect this
// exists for, and it is why the checks below assert on the POINTS ARRAY after
// each gesture rather than on the picture.
//
// Skipped, not failed, when there is no Chrome: this is a check you run, not a
// gate anything depends on.

import { open, checks, findChrome, sleep } from "./chrome.mjs";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const PAGE = pathToFileURL(resolve("wiki/gene-creator/index.html")).href;
const SHOT = process.argv.includes("--shot")
  ? process.argv[process.argv.indexOf("--shot") + 1] : null;

if (!findChrome()) {
  console.log("no Chrome found - skipping the canvas check (set $CHROME to run it)");
  process.exit(0);
}

// The creator runs a parity self-check on boot, so "loaded" is not "ready".
const page = await open(PAGE, {
  readyWhen: `!!(window.HG && HG.ui && document.querySelector("#layers-panel select"))`
});
const c = checks();
const POINTS = `HG.ui.state.spec.expressions[0].layers[0].masks[0].points`;
const read = async () => JSON.parse(await page.evaluate(`JSON.stringify(${POINTS} || null)`));

try {
  c.ok("the creator booted", await page.evaluate("!!(window.HG && HG.ui && HG.pathCanvas)"));
  c.ok("the canvas draws the painter's own curve",
    await page.evaluate("typeof HG.specEngine.pathSpline === 'function'"));

  // The starter gene's first mask, switched to PATH through its own control.
  const type = await page.evaluate(`(function () {
    var s = document.querySelector("#layers-panel select");
    s.value = "PATH";
    s.dispatchEvent(new Event("change", { bubbles: true }));
    return s.value;
  })()`);
  c.ok("a PATH mask can be chosen", type === "PATH", type);
  await sleep(800);

  const box = await page.evaluate(`(function () {
    var e = document.querySelector(".path-canvas");
    if (!e) return null;
    var r = e.getBoundingClientRect();
    return { x: r.left + 1, y: r.top + 1, w: e.clientWidth, h: e.clientHeight };
  })()`);
  if (!c.ok("the canvas is there and has a size", !!box && box.w > 100 && box.h > 100,
      JSON.stringify(box))) {
    throw new Error("no canvas to drive");
  }
  c.ok("exactly one canvas per mask",
    await page.evaluate(`document.querySelectorAll(".path-canvas").length`) === 1);

  // The horse is drawn at ONE scale in body units. Fitting each axis to the
  // box independently would look tidier and would draw a circle where the
  // horse wears an ellipse, so it is worth a check rather than a comment.
  const bodyAspect = await page.evaluate(
    `(function(){var b=HG.geometry.bodyBounds("ADULT");return b.span("X")/b.span("Y");})()`);
  c.ok("both axes are drawn at one scale",
    Math.abs((box.w - 20) / (box.h - 20) - bodyAspect) < 0.02,
    ((box.w - 20) / (box.h - 20)).toFixed(3) + " vs body " + bodyAspect.toFixed(3));

  const painted = await page.evaluate(`(function () {
    var e = document.querySelector(".path-canvas");
    var d = e.getContext("2d").getImageData(0, 0, e.width, e.height).data;
    var n = 0;
    for (var i = 0; i < d.length; i += 4) if (d[i] !== 13 || d[i+1] !== 20 || d[i+2] !== 34) n++;
    return Math.round(100 * n / (d.length / 4));
  })()`);
  c.ok("the horse silhouette is drawn", painted > 5, painted + "% non-background");

  const px = (f) => box.x + box.w * f;
  const py = (f) => box.y + box.h * f;
  const spots = [[0.30, 0.45], [0.50, 0.35], [0.70, 0.45]];

  const start = await read();
  c.ok("a fresh PATH starts with a shape", start.length === 8, JSON.stringify(start));

  for (const [fx, fy] of spots) {
    await page.click(px(fx), py(fy));
    await sleep(180);
  }
  const pts = await read();
  c.ok("three clicks append three points", pts.length === start.length + 6, JSON.stringify(pts));
  c.ok("and leave the shape that was already there alone",
    JSON.stringify(pts.slice(0, 8)) === JSON.stringify(start));
  c.ok("a click lands where it was aimed",
    Math.abs(pts[8] - 0.30) < 0.04 && Math.abs(pts[12] - 0.70) < 0.04,
    JSON.stringify(pts.slice(8)));
  // These numbers are committed into a gene file and read by a person.
  c.ok("the numbers are tidy",
    pts.every((n) => Math.abs(n * 1000 - Math.round(n * 1000)) < 1e-9), JSON.stringify(pts));

  await page.mouse("mousePressed", px(spots[1][0]), py(spots[1][1]));
  await page.mouse("mouseMoved", px(spots[1][0]), py(spots[1][1]) - 40, "left", 1);
  await sleep(120);
  await page.mouse("mouseReleased", px(spots[1][0]), py(spots[1][1]) - 40, "left", 0);
  await sleep(250);
  const moved = await read();
  c.ok("a drag moves the handle", moved[11] > pts[11] + 0.02, pts[11] + " -> " + moved[11]);
  c.ok("a drag moves ONLY that handle",
    moved[8] === pts[8] && moved[12] === pts[12] && moved.length === pts.length,
    JSON.stringify(moved));
  c.ok("the export follows a drag",
    (await page.evaluate(`document.getElementById("json-output").value`)).includes('"points"'));
  const status = await page.evaluate(`document.getElementById("bake-status").textContent`);
  c.ok("the horse re-bakes", /baked in/.test(status), status);

  // The hollow marker mid-span inserts, in order - an insert at the wrong
  // index reorders the shape and is invisible until the curve looks wrong.
  const mid = await page.evaluate(`(function () {
    var p = ${POINTS};
    var e = document.querySelector(".path-canvas");
    var r = e.getBoundingClientRect();
    var bb = HG.geometry.bodyBounds(HG.ui.state.skin), m = 10;
    var su = bb.span("X"), sv = bb.span("Y");
    var scale = Math.min((e.clientWidth - 2*m) / su, (e.clientHeight - 2*m) / sv);
    var ox = (e.clientWidth - su * scale) / 2, oy = (e.clientHeight - sv * scale) / 2;
    var u = (p[0] + p[2]) / 2, v = (p[1] + p[3]) / 2;
    return [r.left + 1 + ox + u * su * scale, r.top + 1 + oy + (1 - v) * sv * scale];
  })()`);
  await page.click(mid[0], mid[1]);
  await sleep(250);
  const inserted = await read();
  c.ok("the midpoint marker inserts in order",
    inserted.length === moved.length + 2 && inserted[0] === moved[0] && inserted[4] === moved[2],
    JSON.stringify(inserted.slice(0, 8)));

  await page.mouse("mousePressed", px(spots[2][0]), py(spots[2][1]), "right", 2);
  await page.mouse("mouseReleased", px(spots[2][0]), py(spots[2][1]), "right", 0);
  await sleep(250);
  const afterDelete = await read();
  c.ok("right-click removes a handle", afterDelete.length === inserted.length - 2,
    "length " + afterDelete.length);

  const textarea = await page.evaluate(`(function () {
    var t = document.querySelector(".path-numbers textarea");
    return t ? t.value : null;
  })()`);
  c.ok("the textarea mirrors the canvas",
    (textarea || "").split("\n").filter(Boolean).length === afterDelete.length / 2,
    JSON.stringify(textarea));

  await page.evaluate(`(function () {
    var t = document.querySelector(".path-numbers textarea");
    t.value = "0.1, 0.1\\n0.9, 0.9";
    t.dispatchEvent(new Event("input", { bubbles: true }));
  })()`);
  await sleep(250);
  c.ok("and the textarea writes back to it",
    JSON.stringify(await read()) === JSON.stringify([0.1, 0.1, 0.9, 0.9]),
    JSON.stringify(await read()));

  // The skin picker only re-bakes, so this is the path that would leave the
  // canvas silhouetting the wrong horse.
  await page.evaluate(`(function () {
    var s = Array.prototype.filter.call(document.querySelectorAll("#preview-controls select"),
      function (x) { return x.options[0].value === "ADULT"; })[0];
    s.value = "BABY";
    s.dispatchEvent(new Event("change", { bubbles: true }));
  })()`);
  await sleep(600);
  const foal = await page.evaluate(`(function () {
    var e = document.querySelector(".path-canvas");
    return (e.clientWidth - 20) / (e.clientHeight - 20);
  })()`);
  const foalBody = await page.evaluate(
    `(function(){var b=HG.geometry.bodyBounds("BABY");return b.span("X")/b.span("Y");})()`);
  c.ok("switching to a foal reshapes the canvas", Math.abs(foal - foalBody) < 0.02,
    foal.toFixed(3) + " vs " + foalBody.toFixed(3));

  // Each plane must map both axes at one scale too. The BOX may letterbox (it
  // is clamped in height), so the element's aspect is not the thing to measure
  // - the mapping is.
  for (const [plane, u, v] of [["side", "X", "Y"], ["top", "X", "Z"], ["front", "Z", "Y"]]) {
    await page.evaluate(`(function () {
      var sel = Array.prototype.filter.call(document.querySelectorAll("#layers-panel select"),
        function (s) { return s.options.length === 3 && s.options[0].value === "side"; })[0];
      sel.value = "${plane}";
      sel.dispatchEvent(new Event("change", { bubbles: true }));
    })()`);
    await sleep(500);
    const el = await page.evaluate(`(function () {
      var e = document.querySelector(".path-canvas");
      var r = e.getBoundingClientRect();
      return { x: r.left + 1, y: r.top + 1, w: e.clientWidth, h: e.clientHeight };
    })()`);
    await page.evaluate(`(function () {
      var t = document.querySelector(".path-numbers textarea");
      t.value = "";
      t.dispatchEvent(new Event("input", { bubbles: true }));
    })()`);
    await sleep(200);
    const cx = el.x + el.w / 2, cy = el.y + el.h / 2;
    for (const [dx, dy] of [[0, 0], [50, 0], [0, -50]]) {
      await page.click(cx + dx, cy + dy);
      await sleep(160);
    }
    const p = await read();
    const spans = await page.evaluate(
      `(function(){var b=HG.geometry.bodyBounds(HG.ui.state.skin);
        return [b.span("${u}"), b.span("${v}")];})()`);
    c.ok("the " + plane + " plane centres the horse",
      Math.abs(p[0] - 0.5) < 0.02 && Math.abs(p[1] - 0.5) < 0.02, JSON.stringify(p.slice(0, 2)));
    const right = (p[2] - p[0]) * spans[0];
    const up = (p[5] - p[1]) * spans[1];
    // The tolerance is the snap grid, not slack: handles land on 0.01 of the
    // body box, which is a third of a body unit on the short axis.
    c.ok("the " + plane + " plane maps both axes at one scale",
      Math.abs(right - up) < 0.35 * Math.max(1, Math.abs(right)),
      "50px right = " + right.toFixed(2) + " units, 50px up = " + up.toFixed(2));
  }

  c.ok("nothing on the console", page.errors().length === 0, page.errors().join(" ;; "));
  if (SHOT) await page.screenshot(SHOT);
} finally {
  page.close();
}

process.exit(c.report("path canvas"));
