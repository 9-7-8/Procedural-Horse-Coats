"""Bake the barn's structure-block save into the mod's cowboy_barn.nbt.

    python neoforge-26.1.2/tools/barn/bake-barn.py

Re-run this after re-exporting the barn from a structure block in game, then
commit both files. It adds the two things a structure-block save cannot carry:

  * a jigsaw block on the west face at y=1, so the village generator can attach
    the barn to a plains-village street connector (see BarnPoolInjector), and
  * the cowboy himself as a structure entity - the same mechanism vanilla uses
    to put villagers in village/plains/villagers/*.nbt. He is placed bare;
    CowboyHandler builds him on his first server tick.

The output is what ships. The input is kept only so the barn can be edited in
game and re-baked - do not point the mod at it.
"""
import gzip, struct, sys, io, os

HERE = os.path.dirname(os.path.abspath(__file__))
MODULE = os.path.abspath(os.path.join(HERE, '..', '..'))

SRC = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, 'cowboy_barn.source.nbt')
DST = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
    MODULE, 'src', 'main', 'resources', 'data', 'horsegenetics', 'structure', 'cowboy_barn.nbt')

# ---------------------------------------------------------------- read
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
    n = u2(); s = data[i[0]:i[0]+n].decode('utf8'); i[0] += n; return s

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
        n = i4(); v = list(data[i[0]:i[0]+n]); i[0] += n; return Tag(7, v)
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
        out = []
        n = i4()
        for _ in range(n):
            out.append(struct.unpack_from('>q', data, i[0])[0]); i[0] += 8
        return Tag(12, out)
    raise Exception('tag ' + str(t))

roottype = u1(); rootname = nm(); root = read(roottype)

# ---------------------------------------------------------------- write
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

# helpers to build tags
def T_str(s): return Tag(8, s)
def T_int(n): return Tag(3, n)
def T_dbl(d): return Tag(6, d)
def T_cmp(d): return Tag(10, d)
def T_ilist(xs): return Tag(9, (3, [T_int(x) for x in xs]))
def T_dlist(xs): return Tag(9, (6, [T_dbl(x) for x in xs]))

palette = root.v['palette'].v[1]
blocks = root.v['blocks'].v[1]

# ---- 1. the jigsaw connector -------------------------------------------
# The barn's west face (x=0) is the open column in front of the north pair of
# doors; the generator rotates the whole piece so this ends up facing back at
# whichever street connector it attached to.  y=1 puts the barn's floor block
# level with the road's surface block, the same relationship the vanilla
# terminators have (their jigsaw is at y=1 over a y=0 road).
palette.append(T_cmp({
    'Name': T_str('minecraft:jigsaw'),
    'Properties': T_cmp({'orientation': T_str('west_up')}),
}))
jigsaw_state = len(palette) - 1

JIGSAW_POS = (0, 1, 2)
blocks[:] = [b for b in blocks if tuple(x.v for x in b.v['pos'].v[1]) != JIGSAW_POS]
blocks.append(T_cmp({
    'pos': T_ilist(list(JIGSAW_POS)),
    'state': T_int(jigsaw_state),
    'nbt': T_cmp({
        'id': T_str('minecraft:jigsaw'),
        'name': T_str('minecraft:street'),
        'target': T_str('minecraft:street'),
        'pool': T_str('minecraft:empty'),
        'joint': T_str('aligned'),
        'final_state': T_str('minecraft:air'),
        'selection_priority': T_int(0),
        'placement_priority': T_int(0),
    }),
}))

# ---- 2. the cowboy ------------------------------------------------------
# Placed bare: CowboyHandler gives him his name, his mount and his herd on his
# first tick, the same deferred-founding shape the wild horses use.
root.v['entities'] = Tag(9, (10, [T_cmp({
    'pos': T_dlist([7.5, 1.0, 3.5]),
    'blockPos': T_ilist([7, 1, 3]),
    'nbt': T_cmp({'id': T_str('horsegenetics:cowboy')}),
})]))

out = io.BytesIO()
out.write(struct.pack('>B', roottype)); w_str(out, rootname); w_payload(out, root)
open(DST, 'wb').write(gzip.compress(out.getvalue()))
print('wrote', DST, 'palette', len(palette), 'blocks', len(blocks))
