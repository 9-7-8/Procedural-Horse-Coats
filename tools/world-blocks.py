"""Read blocks out of a saved world, with no game running (#30).

A death at known coordinates in a soak or test world is a question about the
terrain there, and the world is still on disk. Reasoning about it from the log
guessed "a lava cave"; reading the blocks found a sea under the barn and a
whirlpool off its edge in one pass.

    python tools/world-blocks.py <world> block X Y Z
    python tools/world-blocks.py <world> slice Z X0 X1 Y0 Y1      (a north-facing cut)

<world> is the save folder holding level.dat, e.g. neoforge-26.1.2/run-soak/world.
Overworld only. Plain Python 3, no libraries: region file -> zlib NBT -> the
section's paletted block_states. It reads what the game last SAVED, so stop the
server (or let it halt) first.
"""
import io
import os
import struct
import sys
import zlib


def _nbt(buf):
    def r(fmt):
        return struct.unpack(fmt, buf.read(struct.calcsize(fmt)))[0]

    def payload(t):
        if t == 1: return r('>b')
        if t == 2: return r('>h')
        if t == 3: return r('>i')
        if t == 4: return r('>q')
        if t == 5: return r('>f')
        if t == 6: return r('>d')
        if t == 7: return buf.read(r('>i'))
        if t == 8: return buf.read(r('>H')).decode('utf-8', 'replace')
        if t == 9:
            et = r('>b')
            return [payload(et) for _ in range(r('>i'))]
        if t == 10:
            d = {}
            while True:
                tt = r('>b')
                if tt == 0:
                    return d
                name = buf.read(r('>H')).decode('utf-8', 'replace')
                d[name] = payload(tt)
        if t == 11:
            n = r('>i'); return list(struct.unpack('>%di' % n, buf.read(4 * n)))
        if t == 12:
            n = r('>i'); return list(struct.unpack('>%dq' % n, buf.read(8 * n)))
        raise ValueError('NBT tag %d' % t)

    t = r('>b')
    buf.read(r('>H'))
    return payload(t)


class World:
    def __init__(self, folder):
        self.region = os.path.join(folder, 'dimensions', 'minecraft', 'overworld', 'region')
        if not os.path.isdir(self.region):
            self.region = os.path.join(folder, 'region')
        self.chunks = {}

    def _chunk(self, cx, cz):
        if (cx, cz) not in self.chunks:
            path = os.path.join(self.region, 'r.%d.%d.mca' % (cx >> 5, cz >> 5))
            sections = None
            if os.path.exists(path):
                with open(path, 'rb') as f:
                    data = f.read()
                i = 4 * ((cx & 31) + (cz & 31) * 32)
                off = int.from_bytes(data[i:i + 3], 'big') * 4096
                if off:
                    length = struct.unpack('>i', data[off:off + 4])[0]
                    nbt = _nbt(io.BytesIO(zlib.decompress(data[off + 5:off + 4 + length])))
                    sections = {}
                    for s in nbt['sections']:
                        bs = s.get('block_states')
                        if bs is not None:
                            sections[s['Y']] = ([p['Name'].replace('minecraft:', '') for p in bs['palette']],
                                                bs.get('data'))
            self.chunks[(cx, cz)] = sections
        return self.chunks[(cx, cz)]

    def block(self, x, y, z):
        """The block's id without 'minecraft:', or '?' where nothing is saved."""
        sections = self._chunk(x >> 4, z >> 4)
        if sections is None or (y >> 4) not in sections:
            return '?'
        palette, data = sections[y >> 4]
        if data is None or len(palette) == 1:
            return palette[0]
        bits = max(4, (len(palette) - 1).bit_length())
        per = 64 // bits
        idx = (y & 15) * 256 + (z & 15) * 16 + (x & 15)
        word = data[idx // per] % (1 << 64)
        return palette[(word >> ((idx % per) * bits)) & ((1 << bits) - 1)]


SYMBOLS = {'air': ' ', 'cave_air': ' ', 'water': '~', 'lava': 'L', 'stone': '#', 'deepslate': '#',
           'andesite': '#', 'diorite': '#', 'granite': '#', 'tuff': '#', 'dirt': 'd', 'grass_block': 'g'}


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    world = World(argv[1])
    nums = [int(a) for a in argv[3:]]
    if argv[2] == 'block' and len(nums) == 3:
        print(world.block(*nums))
        return 0
    if argv[2] == 'slice' and len(nums) == 5:
        z, x0, x1, y0, y1 = nums
        other = {}
        for y in range(max(y0, y1), min(y0, y1) - 1, -1):
            row = ''
            for x in range(x0, x1 + 1):
                b = world.block(x, y, z)
                row += SYMBOLS.get(b) or other.setdefault(b, chr(ord('A') + len(other)) if len(other) < 26 else '*')
            print('%4d %s' % (y, row))
        print('x from %d; ' % x0 + ', '.join('%s=%s' % (v, k) for k, v in other.items()))
        return 0
    print(__doc__)
    return 2


if __name__ == '__main__':
    sys.exit(main(sys.argv))
