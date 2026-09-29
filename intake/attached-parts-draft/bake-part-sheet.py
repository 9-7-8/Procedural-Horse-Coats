"""Bake the 64x64 attached-part sheet (4x4 regions of 16x16) - see PartSheet.java.

    python intake/attached-parts-draft/bake-part-sheet.py [--pack DIR] [--out FILE] [--preview FILE]

Two sources per region, same output:
  procedural (default)  a deterministic pattern written here - no art, no download.
  --pack DIR            downsample a 256x256 tile from a texture pack. DIR holds one PNG
                        per region, named in PACK_FILES below (8-bit RGBA or RGB,
                        non-interlaced; re-save from any editor if a pack ships palettes).

The sheet is GREYSCALE. Colour is a per-horse tint applied at draw time (the BraidLayer
method), so one sheet serves ivory, gold, bone, and every crystal colour. What a region
carries is pattern and light: grain, ridges, facet shading and - for crystals - alpha.
Pure standard library, reusing the PNG reader/writer beside it.
"""
import argparse
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'tools'))
from sheet import read_png, write_png  # noqa: E402

SIZE, REGION = 64, 16
# region id -> file in --pack DIR. Ids mirror PartSheet.java; keep the two in step.
PACK_FILES = {0: 'horn.png', 1: 'horn_tip.png', 2: 'bone.png', 3: 'bone_tip.png',
              4: 'crystal.png', 5: 'crystal_core.png'}


def h(x, y, s):
    """Small integer hash -> [0,1). Deterministic; nothing here may use random()."""
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65536.0


def vnoise(x, y, s, period):
    """Tileable value noise: period keeps the region's edges seamless so a box unwrap has no seam."""
    x0, y0 = int(math.floor(x)), int(math.floor(y))
    fx, fy = x - x0, y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    def c(i, j):
        return h(i % period, j % period, s)
    a = c(x0, y0) * (1 - fx) + c(x0 + 1, y0) * fx
    b = c(x0, y0 + 1) * (1 - fx) + c(x0 + 1, y0 + 1) * fx
    return a * (1 - fy) + b * fy


def horn(u, v, tip):
    # long grain along v (a horn's growth direction), fine cross ridges
    g = vnoise(u / 1.2, v / 6.0, 1, 6) * 0.55 + vnoise(u, v, 2, 16) * 0.25
    ridge = 0.10 * math.sin(u * math.pi / 2.0)
    val = 0.62 + 0.28 * g + ridge - (0.12 if tip else 0.0)
    return val, 1.0


def bone(u, v, tip):
    g = vnoise(u / 1.5, v / 5.0, 3, 8) * 0.5 + vnoise(u, v, 4, 16) * 0.3
    pit = 0.18 if h(u, v, 5) > 0.93 else 0.0     # the odd pit, like a real antler
    val = 0.66 + 0.24 * g - pit + (0.10 if tip else 0.0)
    return val, 1.0


def crystal(u, v, core):
    # four vertical facets per face, each lit differently; a bright edge line between them
    facet = int(u // 4) % 4
    shade = [0.40, 0.62, 0.50, 0.72][facet]
    edge = 0.18 if u % 4 == 0 else 0.0
    grad = 0.12 * (1 - v / 15.0)                    # brighter toward the tip end
    flaw = 0.12 * vnoise(u / 2.0, v / 2.0, 6, 8)
    val = shade + edge + grad + flaw
    alpha = 0.55 + 0.3 * (facet % 2) + (0.15 if u % 4 == 0 else 0.0)
    if core:
        val, alpha = min(1.0, val + 0.15), min(1.0, alpha + 0.15)
    return val, min(1.0, alpha)


PROC = {0: lambda u, v: horn(u, v, False), 1: lambda u, v: horn(u, v, True),
        2: lambda u, v: bone(u, v, False), 3: lambda u, v: bone(u, v, True),
        4: lambda u, v: crystal(u, v, False), 5: lambda u, v: crystal(u, v, True)}


def from_pack(path, alpha_from_lum=False):
    """Box-downsample a tile to 16x16 as greyscale + alpha. Colour is discarded on purpose.

    alpha_from_lum: pack tiles are opaque, so for a crystal region alpha is derived from
    brightness (dark = clearer, bright = more opaque) - a glassy read from a flat tile."""
    head = open(path, 'rb').read(33)
    if head[24] != 8 or head[25] != 6 or head[28] != 0:
        sys.exit('%s: need 8-bit RGBA non-interlaced PNG (re-save it); got depth %d colour type %d'
                 % (path, head[24], head[25]))
    w, hh, px = read_png(path)
    out = []
    for v in range(REGION):
        for u in range(REGION):
            x0, x1 = u * w // REGION, (u + 1) * w // REGION
            y0, y1 = v * hh // REGION, (v + 1) * hh // REGION
            lum = al = n = 0
            for y in range(y0, y1):
                for x in range(x0, x1):
                    i = (y * w + x) * 4
                    lum += 0.299 * px[i] + 0.587 * px[i + 1] + 0.114 * px[i + 2]
                    al += px[i + 3]
                    n += 1
            l = lum / n / 255.0
            out.append((l, (0.40 + 0.5 * l) if alpha_from_lum else al / n / 255.0))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--pack')
    ap.add_argument('--out', default='part_sheet.png')
    ap.add_argument('--preview')
    a = ap.parse_args()
    px = bytearray(SIZE * SIZE * 4)
    for rid in range(16):
        ru, rv = (rid % 4) * REGION, (rid // 4) * REGION
        if rid not in PROC:
            continue
        if a.pack and os.path.exists(os.path.join(a.pack, PACK_FILES[rid])):
            tile = from_pack(os.path.join(a.pack, PACK_FILES[rid]), alpha_from_lum=rid in (4, 5))
        else:
            tile = [PROC[rid](u, v) for v in range(REGION) for u in range(REGION)]
        for v in range(REGION):
            for u in range(REGION):
                val, al = tile[v * REGION + u]
                g = max(0, min(255, int(val * 255)))
                i = ((rv + v) * SIZE + ru + u) * 4
                px[i:i + 4] = bytes((g, g, g, max(0, min(255, int(al * 255)))))
    write_png(a.out, SIZE, SIZE, px)
    if a.preview:            # nearest-neighbour x8 over a checkerboard, so alpha is visible
        s = 8
        big = bytearray(SIZE * s * SIZE * s * 4)
        for y in range(SIZE * s):
            for x in range(SIZE * s):
                i = ((y // s) * SIZE + x // s) * 4
                chk = 200 if ((x // 32 + y // 32) % 2) else 235
                al = px[i + 3] / 255.0
                o = (y * SIZE * s + x) * 4
                for c in range(3):
                    big[o + c] = int(px[i + c] * al + chk * (1 - al))
                big[o + 3] = 255
        write_png(a.preview, SIZE * s, SIZE * s, big)


if __name__ == '__main__':
    main()
