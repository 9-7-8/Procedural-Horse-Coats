"""Bake the cowboy's house from a copy of a vanilla plains house.

    python neoforge-26.1.2/tools/barn/bake-house.py

The source is `cowboy_house.source.nbt`, which is a byte copy of
`minecraft:village/plains/houses/plains_small_house_1`. It is duplicated into
this mod rather than referenced because two things have to change, and because
a vanilla template is not ours to rely on staying the shape it is.

The horseman needs a house because the barn cannot be one. A villager brain
claims a bed and goes to it at night, and that is the whole of what he needs -
but the barn is a building made almost entirely of doors, and a villager only
shuts the one he walked through, so sleeping in there is sleeping in a corridor
with three doors open. A house is a house.

What the bake changes:

  * The two jigsaw blocks come out. They are placement machinery, meaningless
    once the piece is placed by hand at runtime, and left in they would sit in
    the finished building as jigsaw blocks. Each becomes whatever belongs there:
    the one in the doorway becomes air, the one in the floor becomes floor.
  * A second bed goes in. One is all the horseman needs; the second is for the
    look of the place, and for the day something else wants a bed there.

Placement is `server/CowboyHouseBuilder`, at runtime, next to the barn.
"""
import gzip, struct, sys, io, os

HERE = os.path.dirname(os.path.abspath(__file__))
MODULE = os.path.abspath(os.path.join(HERE, '..', '..'))

SRC = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, 'cowboy_house.source.nbt')
DST = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
    MODULE, 'src', 'main', 'resources', 'data', 'horsegenetics', 'structure', 'cowboy_house.nbt')

raw = open(SRC, 'rb').read()
try:
    data = gzip.decompress(raw)
except Exception:
    data = raw
i = [0]


def u1():
    v = data[i[0]]; i[0] += 1; return v


def u2():
    v = struct.unpack_from('>H', data, i[0])[0]; i[0] += 2; return v


def i4():
    v = struct.unpack_from('>i', data, i[0])[0]; i[0] += 4; return v


def nm():
    n = u2(); s = data[i[0]:i[0] + n].decode('utf8'); i[0] += n; return s


class Tag:
    def __init__(self, t, v):
        self.t = t; self.v = v


def read(t):
    if t == 1:
        v = struct.unpack_from('>b', data, i[0])[0]; i[0] += 1; return Tag(1, v)
    if t == 2:
        v = struct.unpack_from('>h', data, i[0])[0]; i[0] += 2; return Tag(2, v)
    if t == 3:
        return Tag(3, i4())
    if t == 4:
        v = struct.unpack_from('>q', data, i[0])[0]; i[0] += 8; return Tag(4, v)
    if t == 5:
        v = struct.unpack_from('>f', data, i[0])[0]; i[0] += 4; return Tag(5, v)
    if t == 6:
        v = struct.unpack_from('>d', data, i[0])[0]; i[0] += 8; return Tag(6, v)
    if t == 7:
        n = i4(); v = list(data[i[0]:i[0] + n]); i[0] += n; return Tag(7, v)
    if t == 8:
        return Tag(8, nm())
    if t == 9:
        et = u1(); n = i4(); return Tag(9, (et, [read(et) for _ in range(n)]))
    if t == 10:
        o = {}
        while True:
            tt = u1()
            if tt == 0:
                return Tag(10, o)
            k = nm(); o[k] = read(tt)
    if t == 11:
        n = i4(); return Tag(11, [i4() for _ in range(n)])
    if t == 12:
        n = i4(); out = []
        for _ in range(n):
            out.append(struct.unpack_from('>q', data, i[0])[0]); i[0] += 8
        return Tag(12, out)
    raise Exception('tag ' + str(t))


roottype = u1(); rootname = nm(); root = read(roottype)


def w_str(out, s):
    b = s.encode('utf8'); out.write(struct.pack('>H', len(b))); out.write(b)


def w_payload(out, tag):
    t, v = tag.t, tag.v
    if t == 1: out.write(struct.pack('>b', v))
    elif t == 2: out.write(struct.pack('>h', v))
    elif t == 3: out.write(struct.pack('>i', v))
    elif t == 4: out.write(struct.pack('>q', v))
    elif t == 5: out.write(struct.pack('>f', v))
    elif t == 6: out.write(struct.pack('>d', v))
    elif t == 7:
        out.write(struct.pack('>i', len(v))); out.write(bytes(x & 0xFF for x in v))
    elif t == 8: w_str(out, v)
    elif t == 9:
        et, items = v
        if not items:
            et = 0
        out.write(struct.pack('>B', et)); out.write(struct.pack('>i', len(items)))
        for it in items:
            w_payload(out, it)
    elif t == 10:
        for k, val in v.items():
            out.write(struct.pack('>B', val.t)); w_str(out, k); w_payload(out, val)
        out.write(b'\x00')
    elif t == 11:
        out.write(struct.pack('>i', len(v)))
        for x in v: out.write(struct.pack('>i', x))
    elif t == 12:
        out.write(struct.pack('>i', len(v)))
        for x in v: out.write(struct.pack('>q', x))
    else:
        raise Exception('write tag ' + str(t))


def T_str(s): return Tag(8, s)
def T_int(n): return Tag(3, n)
def T_cmp(d): return Tag(10, d)
def T_ilist(xs): return Tag(9, (3, [T_int(x) for x in xs]))


palette = root.v['palette'].v[1]
blocks = root.v['blocks'].v[1]
size = [x.v for x in root.v['size'].v[1]]


def add_state(name, props=None):
    entry = {'Name': T_str(name)}
    if props:
        entry['Properties'] = T_cmp({k: T_str(v) for k, v in props.items()})
    palette.append(T_cmp(entry))
    return len(palette) - 1


def put(pos, state):
    """Replace whatever is at pos with this palette index, dropping any block entity."""
    blocks[:] = [b for b in blocks if tuple(x.v for x in b.v['pos'].v[1]) != pos]
    blocks.append(T_cmp({'pos': T_ilist(list(pos)), 'state': T_int(state)}))


# ---- 1. the jigsaws come out -------------------------------------------
# Placement machinery. Left in they would stand in the finished house as jigsaw
# blocks, because nothing resolves them when a template is placed by hand.
AIR = add_state('minecraft:air')
FLOOR = add_state('minecraft:oak_planks')

jigsaw_states = {n for n, p in enumerate(palette) if p.v['Name'].v == 'minecraft:jigsaw'}
replaced = 0
for b in list(blocks):
    if b.v['state'].v in jigsaw_states:
        x, y, z = [t.v for t in b.v['pos'].v[1]]
        # The floor one becomes floor; the one in the doorway becomes air.
        inside = 0 < x < size[0] - 1 and 0 < z < size[2] - 1
        put((x, y, z), FLOOR if inside else AIR)
        replaced += 1

# ---- 2. a second bed ----------------------------------------------------
# The house ships with one, at (3,1,2)-(4,1,2) facing east. The far corner of
# the same room is clear, so the pair sits along the same wall.
BED = 'minecraft:white_bed'
SECOND_BED = {
    (2, 1, 4): 'foot',
    (3, 1, 4): 'head',
}
for pos, part in SECOND_BED.items():
    put(pos, add_state(BED, {'facing': 'east', 'part': part, 'occupied': 'false'}))

out = io.BytesIO()
out.write(struct.pack('>B', roottype)); w_str(out, rootname); w_payload(out, root)
# mtime=0 so the bake is byte-reproducible - see bake-barn.py for why that
# matters for a checked-in derived artefact.
open(DST, 'wb').write(gzip.compress(out.getvalue(), mtime=0))
print('wrote', DST, 'size', size, 'palette', len(palette),
      'jigsaws replaced', replaced, 'beds', 1 + len(SECOND_BED) // 2)
