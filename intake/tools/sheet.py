"""Stitch wiki/assets/gene-icons/*.png into one contact sheet.

    python process/tools/sheet.py [cols]

Written because looking at eighty genes one PNG at a time is not looking at
them. Pure standard library - a minimal PNG reader and writer - so it runs
without anything installed.
"""
import io
import os
import struct
import sys
import zlib

DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(
    os.path.abspath(__file__)))), 'wiki', 'assets', 'gene-icons')
OUT = os.path.join(DIR, '_sheet.png')
BACKDROP = b'\x20\x20\x28\xff'


def read_png(path):
    """8-bit RGBA, non-interlaced - which is all GeneIconTool writes."""
    raw = io.open(path, 'rb').read()
    pos, width, height, idat = 8, None, None, b''
    while pos < len(raw):
        length = struct.unpack('>I', raw[pos:pos + 4])[0]
        kind = raw[pos + 4:pos + 8]
        data = raw[pos + 8:pos + 8 + length]
        if kind == b'IHDR':
            width, height = struct.unpack('>II', data[:8])
        elif kind == b'IDAT':
            idat += data
        pos += 12 + length
    buf = zlib.decompress(idat)
    stride = width * 4
    out = bytearray(width * height * 4)
    prev = bytearray(stride)
    o = 0
    for y in range(height):
        filt = buf[o]
        o += 1
        line = bytearray(buf[o:o + stride])
        o += stride
        for x in range(stride):
            a = line[x - 4] if x >= 4 else 0
            b = prev[x]
            c = prev[x - 4] if x >= 4 else 0
            if filt == 1:
                line[x] = (line[x] + a) & 255
            elif filt == 2:
                line[x] = (line[x] + b) & 255
            elif filt == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if (pa <= pb and pa <= pc) else (b if pb <= pc else c))) & 255
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return width, height, out


def write_png(path, width, height, px):
    raw = b''.join(b'\x00' + bytes(px[y * width * 4:(y + 1) * width * 4]) for y in range(height))

    def chunk(kind, data):
        return (struct.pack('>I', len(data)) + kind + data
                + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff))

    io.open(path, 'wb').write(
        b'\x89PNG\r\n\x1a\n'
        + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
        + chunk(b'IDAT', zlib.compress(raw, 6))
        + chunk(b'IEND', b''))


def main(cols):
    names = [n for n in sorted(os.listdir(DIR)) if n.endswith('.png') and n != '_sheet.png']
    tiles = [(n,) + read_png(os.path.join(DIR, n)) for n in names]
    cw = max(t[1] for t in tiles)
    ch = max(t[2] for t in tiles)
    rows = (len(tiles) + cols - 1) // cols
    width, height = cw * cols, ch * rows
    sheet = bytearray(BACKDROP * (width * height))
    for i, (name, w, h, px) in enumerate(tiles):
        ox, oy = (i % cols) * cw, (i // cols) * ch
        for y in range(h):
            for x in range(w):
                src = (y * w + x) * 4
                if px[src + 3] == 0:
                    continue
                dst = ((oy + y) * width + (ox + x)) * 4
                sheet[dst:dst + 4] = px[src:src + 4]
    write_png(OUT, width, height, sheet)
    print('%dx%d, %d tiles, %d per row:' % (width, height, len(tiles), cols))
    for i in range(0, len(names), cols):
        print('  ' + ', '.join(n[:-4] for n in names[i:i + cols]))


if __name__ == '__main__':
    main(int(sys.argv[1]) if len(sys.argv) > 1 else 6)
