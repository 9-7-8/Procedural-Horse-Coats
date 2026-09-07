"""Bake the barn's structure-block save into the mod's cowboy_barn.nbt.

    python neoforge-26.1.2/tools/barn/bake-barn.py

Re-run this after re-exporting the barn from a structure block in game, then
commit both files. It adds the two things a structure-block save cannot carry:

  * a jigsaw block on the west face at y=0, so the village generator can attach
    the barn to a plains-village street connector (see BarnPoolInjector),
  * the two work posts stacked in the middle of that same road-facing end, and a
    villager either side of them - one becomes the cowboy, one the horseman,
  * one villager beside that post, the same mechanism vanilla uses to put
    villagers in village/plains/villagers/*.nbt. He claims the post and becomes
    the cowboy; the next villager to claim it becomes the horseman.

It also takes one thing away: the blocks directly above the doors, so a big
horse can path through them (see CLEAR_ABOVE_DOORS).

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
# The barn's west face (x=0) is the step in front of the north pair of doors;
# the generator rotates the whole piece so this ends up facing back at whichever
# street connector it attached to.
#
# y=0 - the barn's own foundation course - and NOT y=1.  A jigsaw pair lands the
# two blocks at the same world height, and the street connector it meets sits at
# the street piece's y=1, one *above* the road block.  So whatever local layer
# carries this jigsaw is the layer that ends up resting on the ground.  Vanilla
# houses put their `building_entrance` jigsaw in their foundation layer for
# exactly that reason, and they are the shape to copy: a plains house sits one
# block proud of the road with a step up into the doorway.
#
# It was y=1 once, which sank the whole barn a block: the foundation landed in
# the road's own layer, the stair skirt round the doors was buried level with the
# ground instead of stepping up onto it, and the building read as half-dug-in.
#
# final_state is the block the jigsaw replaces itself with once the piece is
# placed - read straight out of the source so the step is put back exactly as it
# was drawn, and so re-exporting the barn with something else there just works.
JIGSAW_POS = (0, 0, 2)

def state_string(entry):
    """A block-state string ('minecraft:oak_stairs[facing=east,...]') for a palette entry."""
    name = entry.v['Name'].v
    props = entry.v.get('Properties')
    if not props:
        return name
    pairs = ','.join('%s=%s' % (k, v.v) for k, v in sorted(props.v.items()))
    return '%s[%s]' % (name, pairs)

replaced = [b for b in blocks if tuple(x.v for x in b.v['pos'].v[1]) == JIGSAW_POS]
final_state = state_string(palette[replaced[0].v['state'].v]) if replaced else 'minecraft:air'

palette.append(T_cmp({
    'Name': T_str('minecraft:jigsaw'),
    'Properties': T_cmp({'orientation': T_str('west_up')}),
}))
jigsaw_state = len(palette) - 1

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
        'final_state': T_str(final_state),
        'selection_priority': T_int(0),
        'placement_priority': T_int(0),
    }),
}))

# ---- 2. the two work posts ---------------------------------------------
# Stacked in the middle of the road-facing end (x=0), between the two door
# pairs, in place of the step that was drawn there.  They are the first thing
# you walk up to.
#
# The hitch on the ground and the table above it, which is the way round they
# read: you tie a horse to the low one.  Two blocks and not one because one
# could never hand out both trades - see server/CowboyHitchHandler.
#
# Baked into the structure rather than placed at runtime because a job-site POI
# is indexed off the block, so putting the block in the world *is* the whole
# mechanism - and because a shop front is architecture.  Neither villager below
# is given a profession: one takes the table the ordinary way, which is the only
# part of the wiring that has ever been in doubt (whether the POI registered,
# whether the acquirable_job_site tag merged), and the other is picked up by the
# hitch.  Two nitwits standing there for ever is a real answer to a real
# question.
POSTS = {
    (0, 0, 3): 'horsegenetics:cowboy_hitch',
    (0, 1, 3): 'horsegenetics:horsemans_table',
}

for pos, block_id in POSTS.items():
    palette.append(T_cmp({'Name': T_str(block_id)}))
    blocks[:] = [b for b in blocks if tuple(x.v for x in b.v['pos'].v[1]) != pos]
    blocks.append(T_cmp({
        'pos': T_ilist(list(pos)),
        'state': T_int(len(palette) - 1),
    }))

# ---- 3. headroom over the doors ----------------------------------------
# Minecraft's ground pathfinder does not measure a mob, it rounds it up:
# WalkNodeEvaluator asks for floor(width + 1) blocks across and floor(height + 1)
# blocks up, and refuses a route that has not got them.  A vanilla-sized horse is
# 1.40 x 1.60, so it wants 2 x 2 - which is exactly what a double door with a
# solid block over it provides, and exactly why the barn worked for ordinary
# horses and jammed for big ones.
#
# The scale attribute multiplies both numbers, so the thresholds are sharp:
#
#     scale < 1.25   2 wide, 2 tall   fits as drawn
#     scale 1.25-1.43   2 wide, 3 tall   needs this
#     scale >= 1.43     3 wide, 3 tall   will not fit a double door at all
#
# So the course of blocks over each doorway is cleared to air, which buys every
# horse up to about 1.43.  It was oak fence, which is the worst possible choice
# there twice over - it is a full pathfinding blocker in its own right, and it
# held the opening to two blocks.  There is no "better block": anything solid
# enough to read as a lintel is solid enough to stop a horse, so the answer is
# nothing at all.  A horse over 1.43 needs a three-wide doorway, which is a
# change to the building rather than to its trim.
#
# Done here rather than in the source so the barn can still be drawn in game with
# whatever header looks right; the bake is what turns "what it looks like" into
# "what a horse can walk through".
CLEAR_ABOVE_DOORS = 3   # the y course immediately over a two-block doorway

door_columns = set()
for b in blocks:
    entry = palette[b.v['state'].v]
    if entry.v['Name'].v.endswith('_door'):
        x, y, z = [t.v for t in b.v['pos'].v[1]]
        door_columns.add((x, z))

palette.append(T_cmp({'Name': T_str('minecraft:air')}))
air_state = len(palette) - 1

cleared = 0
for b in blocks:
    x, y, z = [t.v for t in b.v['pos'].v[1]]
    if y == CLEAR_ABOVE_DOORS and (x, z) in door_columns:
        if palette[b.v['state'].v].v['Name'].v != 'minecraft:air':
            b.v['state'] = T_int(air_state)
            cleared += 1

# ---- 4. the people ------------------------------------------------------
# Two plain villagers, one either side of the stacked posts so neither is
# standing in them.  Nothing else: one claims the table the ordinary way and
# becomes the horseman, the other is taken on by the hitch and becomes the
# cowboy.  The barn used to carry a horsegenetics:cowboy directly; letting the
# blocks hand out both roles is what makes a player-placed post work the same
# way a generated one does.
root.v['entities'] = Tag(9, (10, [
    T_cmp({
        'pos': T_dlist([0.5, 1.0, 4.5]),
        'blockPos': T_ilist([0, 1, 4]),
        'nbt': T_cmp({'id': T_str('minecraft:villager')}),
    }),
    T_cmp({
        'pos': T_dlist([0.5, 1.0, 2.5]),
        'blockPos': T_ilist([0, 1, 2]),
        'nbt': T_cmp({'id': T_str('minecraft:villager')}),
    }),
]))

out = io.BytesIO()
out.write(struct.pack('>B', roottype)); w_str(out, rootname); w_payload(out, root)
# mtime=0 so the bake is byte-reproducible. Without it gzip stamps the
# current time into the header, every run rewrites the file, and
# `git status` stops being able to tell you the barn is stale - which is
# the only staleness signal a checked-in derived artefact has.
open(DST, 'wb').write(gzip.compress(out.getvalue(), mtime=0))
print('wrote', DST, 'palette', len(palette), 'blocks', len(blocks),
      'jigsaw at', JIGSAW_POS, 'final_state', final_state,
      'posts at', sorted(POSTS),
      'cleared', cleared, 'blocks over', len(door_columns), 'door columns')
