// Bring a gene's SVG masks under the point ceiling without changing the drawing.
//
//   node intake/tools/fit-svg.mjs <gene.json> [--budget 700] [--write]
//
// Only a drawing over the ceiling is touched; one already under it is left
// exactly as it arrived.
//
// WHY THIS EXISTS. SvgPath flattens every cubic and quadratic to a fixed sixteen
// segments, so what a drawing costs is its COMMAND count, not its size - and a
// traced image is hundreds of tiny curves. Blackwork, dorsal wing and seven genes
// of the 2026-09-10 evening batch all arrived three to eight times over
// SvgPath.MAX_POINTS for drawings whose silhouettes need a few hundred points.
// Gap 168 is the engine-side fix (adaptive flattening); until it lands, this is
// the intake-side one, and it was being done by hand every time.
//
// What it does, per distinct `d` in the file:
//   1. flattens it exactly the way the engine does - by running the creator's
//      svg-path.js, the parity-checked mirror of SvgPath.java, with only the
//      ceiling lifted in memory so an over-budget drawing can be read at all;
//   2. Ramer-Douglas-Peucker on each subpath, at whatever tolerance brings the
//      whole path under --budget points (binary search);
//   3. re-emits it as M/L/Z polylines - one point per vertex, where a curve
//      costs sixteen;
//   4. MEASURES what that did: both paths are rasterised through the engine's
//      own inside() test on a 400-sample grid over the viewBox, and the share of
//      samples whose inside/outside flipped is printed. Under half a percent is
//      invisible at a horse's resolution, where the whole viewBox is a few dozen
//      texels across.
//
// The mask's transform, viewBox and fill rule are untouched: the points stay in
// the drawing's own user space, so everything downstream maps them as before.
// Without --write it only reports.
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const here = dirname(fileURLToPath(import.meta.url));
const creator = join(here, "..", "..", "wiki", "gene-creator", "js");

const argv = process.argv.slice(2);
const file = argv.find((a) => !a.startsWith("--"));
if (!file) {
  console.error("usage: node intake/tools/fit-svg.mjs <gene.json> [--budget 700] [--write]");
  process.exit(2);
}
const opt = (name, fallback) => {
  const i = argv.indexOf("--" + name);
  return i >= 0 && argv[i + 1] ? argv[i + 1] : fallback;
};
const budget = Number(opt("budget", "700"));
const write = argv.includes("--write");

// The engine, ceiling lifted. Only the constant changes, and only in this
// process - the real ceiling is what the result is then checked against.
const sandbox = { window: {}, console };
sandbox.window.window = sandbox.window;
vm.createContext(sandbox);
const source = readFileSync(join(creator, "svg-path.js"), "utf8");
const lifted = source.replace("var MAX_POINTS = 1024;", "var MAX_POINTS = 1e9;");
if (lifted === source) throw new Error("svg-path.js no longer declares MAX_POINTS the way this tool expects");
vm.runInContext(lifted, sandbox, { filename: "svg-path.js" });
const SVG = sandbox.window.HG.svgPath;
const CEILING = 1024;

function subpaths(shape) {
  const out = [];
  for (let s = 0; s < shape.closed.length; s++) {
    const pts = [];
    for (let i = shape.starts[s]; i < shape.starts[s + 1]; i++) pts.push([shape.xs[i], shape.ys[i]]);
    out.push({ pts, closed: shape.closed[s] });
  }
  return out;
}

function segDist(p, a, b) {
  const dx = b[0] - a[0], dy = b[1] - a[1];
  const len = dx * dx + dy * dy;
  let t = len ? ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / len : 0;
  t = Math.max(0, Math.min(1, t));
  const x = a[0] + t * dx - p[0], y = a[1] + t * dy - p[1];
  return Math.sqrt(x * x + y * y);
}

function rdp(pts, eps) {
  if (pts.length < 3) return pts.slice();
  const keep = new Uint8Array(pts.length);
  keep[0] = keep[pts.length - 1] = 1;
  const stack = [[0, pts.length - 1]];
  while (stack.length) {
    const [a, b] = stack.pop();
    let best = -1, at = -1;
    for (let i = a + 1; i < b; i++) {
      const d = segDist(pts[i], pts[a], pts[b]);
      if (d > best) { best = d; at = i; }
    }
    if (best > eps) {
      keep[at] = 1;
      stack.push([a, at], [at, b]);
    }
  }
  return pts.filter((_, i) => keep[i]);
}

// A closed ring is split at the point farthest from its start, so RDP has two
// real endpoints to anchor on rather than a start that equals its own end.
function simplify(sub, eps) {
  let pts = sub.pts;
  if (sub.closed && pts.length > 3) {
    const last = pts[pts.length - 1];
    if (last[0] === pts[0][0] && last[1] === pts[0][1]) pts = pts.slice(0, -1);
    let far = 0, fd = -1;
    for (let i = 1; i < pts.length; i++) {
      const d = Math.hypot(pts[i][0] - pts[0][0], pts[i][1] - pts[0][1]);
      if (d > fd) { fd = d; far = i; }
    }
    const one = rdp(pts.slice(0, far + 1), eps);
    const two = rdp(pts.slice(far).concat([pts[0]]), eps);
    const ring = one.concat(two.slice(1, -1));
    return ring.length >= 3 ? ring : null;          // collapsed to a sliver: gone
  }
  const open = rdp(pts, eps);
  return open.length >= 2 ? open : null;
}

const r2 = (v) => String(Math.round(v * 100) / 100);
// "M x y L x y x y ... Z" - an L's coordinates repeat implicitly, per the grammar.
function emit(subs) {
  return subs.map(({ pts, closed }) =>
    "M" + r2(pts[0][0]) + " " + r2(pts[0][1])
      + "L" + pts.slice(1).map((p) => r2(p[0]) + " " + r2(p[1])).join(" ")
      + (closed ? "Z" : "")).join("");
}

function rasterDiff(dA, dB, fillRule, box) {
  const A = SVG.parse(dA, null), B = SVG.parse(dB, null);
  const evenOdd = fillRule === "evenodd";
  const N = 400;
  let flips = 0, inA = 0;
  for (let j = 0; j < N; j++) {
    for (let i = 0; i < N; i++) {
      const x = box[0] + (i + 0.5) * box[2] / N, y = box[1] + (j + 0.5) * box[3] / N;
      const a = SVG.inside(A, x, y, evenOdd), b = SVG.inside(B, x, y, evenOdd);
      if (a) inA++;
      if (a !== b) flips++;
    }
  }
  return { flips: flips / (N * N), filled: inA / (N * N) };
}

const gene = JSON.parse(readFileSync(file, "utf8"));
const done = new Map();          // original d -> fitted d, so shared drawings are fitted once
let changed = false;
for (const e of gene.expressions || []) {
  for (const layer of e.layers || []) {
    for (const mask of layer.masks || []) {
      if (mask.type !== "SVG" || typeof mask.d !== "string") continue;
      if (!done.has(mask.d)) {
        const shape = SVG.parse(mask.d, null);
        const before = shape.xs.length;
        if (before <= CEILING) {
          done.set(mask.d, null);
          console.log(`${e.id} / ${layer.name}: ${before} points, already fits`);
          continue;
        }
        const subs = subpaths(shape);
        const span = Math.max(shape.box[2] - shape.box[0], shape.box[3] - shape.box[1]);
        let lo = 0, hi = span / 20, fitted = null;
        for (let k = 0; k < 40; k++) {
          const eps = (lo + hi) / 2;
          const out = subs.map((s) => ({ pts: simplify(s, eps), closed: s.closed })).filter((s) => s.pts);
          const n = out.reduce((t, s) => t + s.pts.length, 0);
          if (n <= budget) { hi = eps; fitted = { out, n, eps }; } else lo = eps;
        }
        if (!fitted) throw new Error(`${e.id} / ${layer.name}: could not reach ${budget} points`);
        const d = emit(fitted.out);
        const check = SVG.parse(d, null).xs.length;
        const vb = (mask.viewBox || "").trim().split(/[\s,]+/).map(Number);
        const box = vb.length === 4 ? vb : [shape.box[0], shape.box[1], shape.box[2] - shape.box[0], shape.box[3] - shape.box[1]];
        const diff = rasterDiff(mask.d, d, mask.fillRule || "nonzero", box);
        console.log(`${e.id} / ${layer.name}: ${before} -> ${check} points (${subs.length} -> ${fitted.out.length} subpaths), `
          + `tolerance ${fitted.eps.toFixed(3)} = ${(100 * fitted.eps / span).toFixed(2)}% of the drawing; `
          + `${(100 * diff.flips).toFixed(2)}% of the viewBox flipped (${(100 * diff.filled).toFixed(1)}% of it filled)`);
        if (check > CEILING) throw new Error("still over the ceiling after fitting - the emitter and the engine disagree");
        done.set(mask.d, d);
      }
      const fitted = done.get(mask.d);
      if (fitted) { mask.d = fitted; changed = true; }
    }
  }
}
if (write && changed) {
  writeFileSync(file, JSON.stringify(gene, null, 2) + "\n");
  console.log("written: " + file);
} else if (changed) {
  console.log("(report only - pass --write to rewrite the file)");
}
