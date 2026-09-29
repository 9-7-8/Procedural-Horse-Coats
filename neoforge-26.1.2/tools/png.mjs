// Minimal PNG read/write for 8-bit RGBA, no dependencies.
//
// It exists because the item-texture audit (check-duplicate-item-art.mjs) has to
// compare PICTURES rather than files: two PNGs of the same image written by
// different tools differ in every byte, and two textures at different scales are
// still the same art. Nothing here is general - it handles the one format this
// pack's sprites are actually in, and throws on anything else rather than
// guessing.
import fs from 'node:fs';
import zlib from 'node:zlib';
import crypto from 'node:crypto';

/** @returns {{w:number,h:number,px:Buffer}} RGBA8, row-major, no padding. */
export function readPng(pathOrBuffer) {
  const b = Buffer.isBuffer(pathOrBuffer) ? pathOrBuffer : fs.readFileSync(pathOrBuffer);
  let o = 8;
  let w = 0, h = 0, bitDepth = 0, colourType = 0;
  let palette = null, trns = null;
  const idat = [];
  while (o < b.length) {
    const len = b.readUInt32BE(o);
    const type = b.toString('ascii', o + 4, o + 8);
    const data = b.subarray(o + 8, o + 8 + len);
    if (type === 'IHDR') {
      w = data.readUInt32BE(0); h = data.readUInt32BE(4);
      bitDepth = data[8]; colourType = data[9];
      if (data[12] !== 0) throw new Error('interlaced PNG not handled');
    } else if (type === 'PLTE') palette = Buffer.from(data);
    else if (type === 'tRNS') trns = Buffer.from(data);
    else if (type === 'IDAT') idat.push(data);
    o += 12 + len;
  }
  if (bitDepth !== 8) throw new Error(`want 8-bit, got ${bitDepth}`);
  const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colourType];
  if (channels === undefined) throw new Error('bad colour type ' + colourType);
  const raw = zlib.inflateSync(Buffer.concat(idat));

  const stride = w * channels;
  const flat = Buffer.alloc(h * stride);
  for (let y = 0; y < h; y++) {
    const ft = raw[y * (stride + 1)];
    const line = raw.subarray(y * (stride + 1) + 1, y * (stride + 1) + 1 + stride);
    for (let x = 0; x < stride; x++) {
      const a = x >= channels ? flat[y * stride + x - channels] : 0;
      const bb = y > 0 ? flat[(y - 1) * stride + x] : 0;
      const c = (x >= channels && y > 0) ? flat[(y - 1) * stride + x - channels] : 0;
      let v = line[x];
      if (ft === 1) v += a;
      else if (ft === 2) v += bb;
      else if (ft === 3) v += Math.floor((a + bb) / 2);
      else if (ft === 4) {
        const p = a + bb - c;
        const pa = Math.abs(p - a), pb = Math.abs(p - bb), pc = Math.abs(p - c);
        v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? bb : c);
      } else if (ft !== 0) throw new Error('bad filter ' + ft);
      flat[y * stride + x] = v & 0xff;
    }
  }

  // Normalise everything to RGBA so callers never branch on colour type.
  const px = Buffer.alloc(w * h * 4);
  for (let i = 0, j = 0; i < w * h; i++, j += 4) {
    const s = i * channels;
    if (colourType === 6) { flat.copy(px, j, s, s + 4); }
    else if (colourType === 2) { flat.copy(px, j, s, s + 3); px[j + 3] = 255; }
    else if (colourType === 0) { px[j] = px[j + 1] = px[j + 2] = flat[s]; px[j + 3] = 255; }
    else if (colourType === 4) { px[j] = px[j + 1] = px[j + 2] = flat[s]; px[j + 3] = flat[s + 1]; }
    else { // 3: palette
      const idx = flat[s];
      px[j] = palette[idx * 3]; px[j + 1] = palette[idx * 3 + 1]; px[j + 2] = palette[idx * 3 + 2];
      px[j + 3] = trns && idx < trns.length ? trns[idx] : 255;
    }
  }
  return { w, h, px };
}

let TABLE = null;
function crc32(buf) {
  if (!TABLE) {
    TABLE = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      TABLE[n] = c;
    }
  }
  let c = -1;
  for (let i = 0; i < buf.length; i++) c = TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return c ^ -1;
}

/** Write RGBA8. Filter 0 throughout, which compresses fine at sprite sizes. */
export function writePng(path, w, h, px) {
  const stride = w * 4;
  const raw = Buffer.alloc(h * (stride + 1));
  for (let y = 0; y < h; y++) {
    raw[y * (stride + 1)] = 0;
    px.copy(raw, y * (stride + 1) + 1, y * stride, y * stride + stride);
  }
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
    const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td) >>> 0);
    return Buffer.concat([len, td, crc]);
  };
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6;
  fs.writeFileSync(path, Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]));
}

/**
 * <b>A hash of the picture, not the file.</b> Sampled to a fixed 16x16 grid so
 * that a 32x32 sprite and the 16x16 it was scaled up from hash the same - which
 * is the whole point, since "identical to a vanilla item" is a statement about
 * what a player sees and not about bytes. Fully transparent pixels normalise to
 * one value, because RGB under alpha 0 is invisible and varies by editor.
 */
export function artHash(img) {
  const N = 16;
  const parts = [];
  for (let y = 0; y < N; y++) {
    for (let x = 0; x < N; x++) {
      const sx = Math.floor(x * img.w / N), sy = Math.floor(y * img.h / N);
      const i = (sy * img.w + sx) * 4;
      parts.push(img.px[i + 3] === 0 ? '----' :
        [img.px[i], img.px[i + 1], img.px[i + 2], img.px[i + 3]]
          .map(v => v.toString(16).padStart(2, '0')).join(''));
    }
  }
  return crypto.createHash('sha1').update(parts.join('')).digest('hex').slice(0, 16);
}

// --- colour helpers, shared by the recolour tool -------------------------
export function rgb2hsl(r, g, b) {
  r /= 255; g /= 255; b /= 255;
  const mx = Math.max(r, g, b), mn = Math.min(r, g, b), l = (mx + mn) / 2;
  if (mx === mn) return [0, 0, l];
  const d = mx - mn;
  const s = l > 0.5 ? d / (2 - mx - mn) : d / (mx + mn);
  let h;
  if (mx === r) h = (g - b) / d + (g < b ? 6 : 0);
  else if (mx === g) h = (b - r) / d + 2;
  else h = (r - g) / d + 4;
  return [h / 6, s, l];
}
const hue2rgb = (p, q, t) => {
  if (t < 0) t += 1; if (t > 1) t -= 1;
  if (t < 1 / 6) return p + (q - p) * 6 * t;
  if (t < 1 / 2) return q;
  if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
  return p;
};
export function hsl2rgb(h, s, l) {
  if (s === 0) { const v = Math.round(l * 255); return [v, v, v]; }
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
  return [hue2rgb(p, q, h + 1 / 3), hue2rgb(p, q, h), hue2rgb(p, q, h - 1 / 3)]
    .map(v => Math.round(Math.max(0, Math.min(1, v)) * 255));
}

/**
 * <b>A sprite reduced to something two of them can be compared on.</b> The 16x16
 * sampling grid again, but kept as numbers rather than hashed, so that "almost
 * the same picture" is answerable at all.
 *
 * <p>{@link artHash} only ever answers IDENTICAL or NOT, and that turned out to
 * be too weak for the job it was written for: a recolour that shifted a sprite's
 * saturation from 0.40 to 0.18 produced different pixels in every position and
 * an entirely different hash, while being indistinguishable from its source to
 * the eye. A checker that passes on that is worse than no checker, because it
 * certifies the thing it was meant to catch.
 */
export function artSignature(img) {
  const N = 16;
  const rgba = new Float64Array(N * N * 4);
  for (let y = 0; y < N; y++) {
    for (let x = 0; x < N; x++) {
      const sx = Math.floor(x * img.w / N), sy = Math.floor(y * img.h / N);
      const i = (sy * img.w + sx) * 4, o = (y * N + x) * 4;
      rgba[o] = img.px[i]; rgba[o + 1] = img.px[i + 1];
      rgba[o + 2] = img.px[i + 2]; rgba[o + 3] = img.px[i + 3];
    }
  }
  return rgba;
}

/**
 * <b>How far apart two sprites look</b>, 0 (identical) upward. Mean per-channel
 * distance over the sampled grid, counting only cells where at least one of the
 * two is opaque - so two sprites of the same SHAPE in different colours score
 * on the colour rather than being flattered by the transparent surround they
 * share.
 *
 * <p>Returns {@code null} when the two share no opaque cell at all, which means
 * they are different shapes and the question does not arise.
 */
export function artDistance(a, b) {
  let sum = 0, n = 0;
  for (let i = 0; i < a.length; i += 4) {
    const aOn = a[i + 3] > 32, bOn = b[i + 3] > 32;
    if (!aOn && !bOn) continue;
    if (aOn !== bOn) { sum += 255; n++; continue; }
    sum += (Math.abs(a[i] - b[i]) + Math.abs(a[i + 1] - b[i + 1]) + Math.abs(a[i + 2] - b[i + 2])) / 3;
    n++;
  }
  return n === 0 ? null : sum / n;
}
