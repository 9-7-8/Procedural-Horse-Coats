// Turn an .svg file into SVG mask blocks a gene file can carry.
//
//   node intake/tools/svg-to-mask.mjs drawing.svg [--plane side] [--origin u,v]
//                                     [--size u,v] [--parts BODY,NECK] [--merge]
//
// WHY THIS EXISTS. The SVG mask reads a `d` string, a transform list and a
// viewBox - which is three of the roughly forty things an .svg file contains.
// The other thirty-seven are the ones that quietly move the drawing:
//
//   - <rect>, <circle>, <ellipse>, <line>, <polyline> and <polygon> are not
//     paths at all, and half of any exported file is made of them.
//   - a <g transform> applies to everything inside it, and they nest.
//   - the file's own viewBox is what relates one element to another. Drop it
//     and every mark fills its own viewport, which is to say the drawing comes
//     apart into pieces that no longer line up.
//   - fill-rule, stroke-width, stroke-linecap, stroke-linejoin and
//     stroke-dasharray each change the picture and each has a default that is
//     not the mask's.
//
// So this reads the file the way a renderer would, flattens the group
// transforms into each element, converts the shape elements to path data, and
// writes out one mask per drawable with the presentation attributes carried
// across. Paste the result into a layer's `masks` array.
//
// Pure standard library, no XML parser: the subset of the grammar an exported
// SVG uses is regular enough to walk with a scanner, and adding a dependency to
// a repo that has none for a tool this size is the wrong trade. It handles
// attributes, self-closing tags, nesting and comments, and it says so plainly
// when it meets something it does not (entities, <use>, <style>, CSS classes).
import { readFileSync } from "node:fs";
import { basename } from "node:path";

const argv = process.argv.slice(2);
if (!argv.length || argv[0] === "--help") {
  console.log(`usage: node intake/tools/svg-to-mask.mjs <file.svg> [options]

  --plane side|top|front   which two body axes the drawing lies in (default side)
  --origin u,v             the viewport's near corner, body-normalised (default 0.2,0.35)
  --size u,v               the viewport's extent, body-normalised (default 0.4,0.3)
  --parts A,B              restrict the masks to these parts
  --merge                  emit ONE mask with every subpath in it, rather than
                           one mask per drawable. Use it when the drawing is a
                           single mark; keep them separate when the pieces want
                           different colours.
  --fit meet|slice|none    preserveAspectRatio (default meet)
`);
  process.exit(argv.length ? 0 : 1);
}

const file = argv[0];
const opt = (name, fallback) => {
  const i = argv.indexOf("--" + name);
  return i >= 0 && argv[i + 1] ? argv[i + 1] : fallback;
};
const has = (name) => argv.includes("--" + name);

const plane = opt("plane", "side");
const fit = opt("fit", "meet");
const [originU, originV] = opt("origin", "0.2,0.35").split(",").map(Number);
const [sizeU, sizeV] = opt("size", "0.4,0.3").split(",").map(Number);
const parts = opt("parts", "").split(",").map((s) => s.trim()).filter(Boolean);

const src = readFileSync(file, "utf8");
const warnings = [];

// ---- the scanner ---------------------------------------------------------
// Tags, attributes, nesting. Not a parser: an .svg written by a drawing program
// has no mixed content and no entities in the places we read, and anything
// stranger than that is reported rather than guessed at.

function attributes(tag) {
  const out = {};
  const re = /([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*"([^"]*)"|([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*'([^']*)'/g;
  let m;
  while ((m = re.exec(tag))) {
    out[m[1] || m[3]] = m[2] !== undefined ? m[2] : m[4];
  }
  return out;
}

function walk(text) {
  const drawables = [];
  const stack = [];
  let root = null;
  const re = /<!--[\s\S]*?-->|<\/([A-Za-z][-A-Za-z0-9]*)\s*>|<([A-Za-z][-A-Za-z0-9]*)([^>]*?)(\/?)>/g;
  let m;
  while ((m = re.exec(text))) {
    if (m[0].startsWith("<!--")) continue;
    if (m[1]) {                       // a closing tag
      if (stack.length && stack[stack.length - 1].name === m[1]) stack.pop();
      continue;
    }
    const name = m[2];
    const attrs = attributes(m[3] || "");
    const selfClosing = m[4] === "/";

    if (name === "svg" && !root) root = attrs;
    if (name === "style" || name === "use" || name === "text" || name === "image") {
      warnings.push(`<${name}> ignored - this tool reads geometry, not `
        + (name === "style" ? "stylesheets (inline the presentation attributes instead)"
          : name === "use" ? "references (expand it in the drawing program first)"
            : "content"));
    }
    if (attrs.class) {
      warnings.push(`<${name} class="${attrs.class}"> - a CSS class carries presentation `
        + "this tool cannot see; check the fill rule and stroke width by hand");
    }

    const transform = stack.map((s) => s.transform).concat(attrs.transform || "")
      .filter(Boolean).join(" ");
    const d = pathDataFor(name, attrs);
    if (d) {
      drawables.push({ name, attrs, d, transform, id: attrs.id || null });
    }
    if (!selfClosing && name !== "svg") stack.push({ name, transform: attrs.transform || "" });
    else if (!selfClosing && name === "svg") stack.push({ name, transform: "" });
  }
  return { root: root || {}, drawables };
}

// ---- shape elements as path data ----------------------------------------
// A <rect> is four lines (or eight, with corner arcs); a <circle> is two arcs,
// because one arc cannot span a full turn. Both are exactly what a renderer
// does with them - the spec defines these elements BY their equivalent path.

const num = (v, fallback = 0) => (v === undefined || v === "" ? fallback : parseFloat(v));

function pathDataFor(name, a) {
  switch (name) {
    case "path":
      return a.d || null;
    case "rect": {
      const x = num(a.x), y = num(a.y), w = num(a.width), h = num(a.height);
      if (!(w > 0 && h > 0)) return null;
      let rx = a.rx !== undefined ? num(a.rx) : (a.ry !== undefined ? num(a.ry) : 0);
      let ry = a.ry !== undefined ? num(a.ry) : rx;
      rx = Math.min(rx, w / 2);
      ry = Math.min(ry, h / 2);
      if (!(rx > 0 && ry > 0)) {
        return `M ${x} ${y} H ${x + w} V ${y + h} H ${x} Z`;
      }
      return `M ${x + rx} ${y} H ${x + w - rx} A ${rx} ${ry} 0 0 1 ${x + w} ${y + ry} `
        + `V ${y + h - ry} A ${rx} ${ry} 0 0 1 ${x + w - rx} ${y + h} `
        + `H ${x + rx} A ${rx} ${ry} 0 0 1 ${x} ${y + h - ry} `
        + `V ${y + ry} A ${rx} ${ry} 0 0 1 ${x + rx} ${y} Z`;
    }
    case "circle": {
      const cx = num(a.cx), cy = num(a.cy), r = num(a.r);
      if (!(r > 0)) return null;
      // Two half-turns: a single arc command cannot close a full circle,
      // because its start and end points would coincide and the sweep would be
      // undefined. Every renderer splits it the same way.
      return `M ${cx - r} ${cy} A ${r} ${r} 0 1 0 ${cx + r} ${cy} `
        + `A ${r} ${r} 0 1 0 ${cx - r} ${cy} Z`;
    }
    case "ellipse": {
      const cx = num(a.cx), cy = num(a.cy), rx = num(a.rx), ry = num(a.ry);
      if (!(rx > 0 && ry > 0)) return null;
      return `M ${cx - rx} ${cy} A ${rx} ${ry} 0 1 0 ${cx + rx} ${cy} `
        + `A ${rx} ${ry} 0 1 0 ${cx - rx} ${cy} Z`;
    }
    case "line":
      return `M ${num(a.x1)} ${num(a.y1)} L ${num(a.x2)} ${num(a.y2)}`;
    case "polyline":
    case "polygon": {
      const pts = (a.points || "").trim().split(/[\s,]+/).filter((s) => s.length).map(Number);
      if (pts.length < 4) return null;
      let d = `M ${pts[0]} ${pts[1]}`;
      for (let i = 2; i + 1 < pts.length; i += 2) d += ` L ${pts[i]} ${pts[i + 1]}`;
      return name === "polygon" ? d + " Z" : d;
    }
    default:
      return null;
  }
}

// ---- presentation --------------------------------------------------------
// The attributes that change the picture, and only those. Each default here is
// SVG's, which is why they are worth carrying: a <path> with no fill attribute
// is FILLED, and one whose fill is "none" is not, and the difference is
// invisible in the geometry.

const CAPS = { butt: "butt", round: "round", square: "square" };
const JOINS = { miter: "miter", round: "round", bevel: "bevel" };

function presentation(a) {
  const out = {};
  const stroked = a.stroke && a.stroke !== "none";
  const filled = !(a.fill === "none");
  if (stroked && !filled) {
    out.fill = false;
    const w = num(a["stroke-width"], 1);
    if (w) out.__strokeWidthUser = w;
    if (CAPS[a["stroke-linecap"]]) out.cap = CAPS[a["stroke-linecap"]];
    if (JOINS[a["stroke-linejoin"]]) out.join = JOINS[a["stroke-linejoin"]];
    if (a["stroke-miterlimit"]) out.miterLimit = num(a["stroke-miterlimit"], 4);
    if (a["stroke-dasharray"] && a["stroke-dasharray"] !== "none") {
      const dash = a["stroke-dasharray"].trim().split(/[\s,]+/).map(Number).filter((n) => n >= 0);
      if (dash.length) {
        out.__dashUser = dash[0];
        out.__gapUser = dash.length > 1 ? dash[1] : dash[0];
      }
      if (a["stroke-dashoffset"]) out.__dashOffsetUser = num(a["stroke-dashoffset"], 0);
    }
  } else if (stroked && filled) {
    warnings.push("an element is both filled and stroked; the mask is one or the other, "
      + "so it was emitted as a fill - add a second, stroked mask if the outline matters");
  }
  if (a["fill-rule"] === "evenodd" || a["clip-rule"] === "evenodd") out.fillRule = "evenodd";
  if (a.fill && a.fill !== "none" && /^#|^rgb/.test(a.fill)) out.__fill = a.fill;
  if (a.stroke && a.stroke !== "none" && /^#|^rgb/.test(a.stroke)) out.__stroke = a.stroke;
  return out;
}

// ---- output --------------------------------------------------------------

const { root, drawables } = walk(src);
if (!drawables.length) {
  console.error(`${file}: nothing drawable found. Shapes this tool reads: `
    + "path, rect, circle, ellipse, line, polyline, polygon.");
  process.exit(1);
}

let viewBox = (root.viewBox || "").trim().split(/[\s,]+/).map(Number).filter((n) => !isNaN(n));
if (viewBox.length !== 4) {
  const w = num(root.width, 0), h = num(root.height, 0);
  if (w > 0 && h > 0) {
    viewBox = [0, 0, w, h];
    warnings.push("no viewBox on the <svg>; using its width and height instead");
  } else {
    viewBox = null;
    warnings.push("no viewBox and no width/height on the <svg> - each mask will fit its own "
      + "bounds, so several marks that lined up in the drawing will not line up on the horse. "
      + "Add a viewBox by hand.");
  }
}

// Body units per drawing unit, so a stroke width written in the file's own
// space comes out as the width it looked like. The viewport is given in
// body-normalised terms and the adult horse is 41 long by 33.7 tall; that is
// the conversion, and it is approximate on purpose - the mask's `space: body`
// scales with the skin, and a foal's stroke should be a foal's stroke.
const BODY_SPAN = plane === "top" ? [41, 10] : plane === "front" ? [10, 33.7] : [41, 33.7];
const perUnit = viewBox
  ? Math.sqrt((sizeU * BODY_SPAN[0] / viewBox[2]) * (sizeV * BODY_SPAN[1] / viewBox[3]))
  : 1;

function toMask(item) {
  const mask = { type: "SVG" };
  if (parts.length) mask.parts = parts;
  mask.d = item.d.replace(/\s+/g, " ").trim();
  if (item.transform) mask.transform = item.transform.replace(/\s+/g, " ").trim();
  if (viewBox) mask.viewBox = viewBox;
  if (plane !== "side") mask.plane = plane;
  mask.originU = round(originU);
  mask.originV = round(originV);
  mask.sizeU = round(sizeU);
  mask.sizeV = round(sizeV);
  if (fit !== "meet") mask.fit = fit;

  const p = presentation(item.attrs);
  if (p.fillRule) mask.fillRule = p.fillRule;
  if (p.fill === false) {
    mask.fill = false;
    if (p.__strokeWidthUser) mask.width = round(p.__strokeWidthUser * perUnit);
    if (p.cap) mask.cap = p.cap;
    if (p.join) mask.join = p.join;
    if (p.miterLimit) mask.miterLimit = p.miterLimit;
    if (p.__dashUser) {
      mask.dash = round(p.__dashUser * perUnit);
      mask.gap = round(p.__gapUser * perUnit);
      if (p.__dashOffsetUser) mask.dashOffset = round(p.__dashOffsetUser * perUnit);
    }
  }
  mask.softness = 0.25;
  return { mask, colour: p.__fill || p.__stroke || null, id: item.id, tag: item.name };
}

function round(n) {
  return Math.round(n * 1000) / 1000;
}

let out;
if (has("merge")) {
  // One mask, every subpath in it. Only honest when the transforms agree -
  // otherwise the pieces would each need their own, and a mask carries one.
  const transforms = new Set(drawables.map((d) => d.transform.trim()));
  if (transforms.size > 1) {
    warnings.push("--merge with elements under DIFFERENT transforms: they were merged anyway, "
      + "which will move the ones whose transform was dropped. Flatten the transforms in the "
      + "drawing program, or drop --merge.");
  }
  const first = toMask(drawables[0]);
  first.mask.d = drawables.map((d) => d.d.replace(/\s+/g, " ").trim()).join(" ");
  out = [first];
} else {
  out = drawables.map(toMask);
}

const name = basename(file).replace(/\.svg$/i, "");
console.log(`// ${out.length} mask(s) from ${basename(file)}`);
console.log(`// Paste into a layer's "masks". One mask is one layer's shape: give each`);
console.log(`// its own layer if the pieces want different colours.`);
out.forEach((entry, i) => {
  const label = entry.id || `${entry.tag} ${i + 1}`;
  console.log(`\n// ${name} - ${label}${entry.colour ? `, drawn ${entry.colour} in the file` : ""}`);
  console.log(JSON.stringify(entry.mask, null, 2));
});

if (warnings.length) {
  console.error("\n" + warnings.length + " thing(s) worth reading before you paste:");
  [...new Set(warnings)].forEach((w) => console.error("  - " + w));
}
