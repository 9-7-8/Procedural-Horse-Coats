"""Bake the cowboy's homestead into the mod's cowboy_barn.nbt.

    python neoforge-26.1.2/tools/barn/bake-barn.py

Two sources, one output. Re-run this after re-exporting either building from a
structure block in game, then commit all three files.

  * `cowboy_barn.source.nbt`  - the stable, drawn by hand.
  * `cowboy_house.source.nbt` - a byte copy of vanilla's
    `village/plains/houses/plains_small_house_1`, reshaped here.

They are baked into ONE piece, and that is the decision this file exists to
record. The house used to be placed at runtime when the cowboy founded, and it
cannot be: the work posts live on the house, the work posts are what make a
cowboy, and a cowboy cannot build the thing that made him. A piece that
generates with both buildings in it has no such order to get wrong - and it
cannot fail to find flat ground either, which is what the runtime version
actually did, every single time it ran.

The cost is a bigger bounding box, and a piece that does not fit is a piece that
does not generate; the barn's weight in the terminator pool goes up to
compensate (see `worldgen/BarnPoolInjector`).

What the bake adds that a structure-block save cannot carry:

  * a ground course under the whole homestead, carrying the path at road level,
  * a jigsaw block on the barn's west face in the foundation course, so the
    village generator can attach the piece to a plains-village street connector,
  * headroom over the barn's doorways, so a big horse can path through them,
  * the house beside the barn - widened, deepened, re-furnished, jigsaws
    resolved,
  * the work posts and the bench down the house's frontage, with a villager
    standing at each post, and
  * the beds and the two chests inside it.

The output is what ships. The inputs are kept only so the buildings can be
edited in game and re-baked - do not point the mod at them.
"""
import gzip
import io
import os
import re
import struct
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
MODULE = os.path.abspath(os.path.join(HERE, '..', '..'))

BARN_SRC = os.path.join(HERE, 'cowboy_barn.source.nbt')
HOUSE_SRC = os.path.join(HERE, 'cowboy_house.source.nbt')
DST = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    MODULE, 'src', 'main', 'resources', 'data', 'horsegenetics', 'structure', 'cowboy_barn.nbt')


# ============================================================ nbt reading
class Tag:
    def __init__(self, t, v):
        self.t = t
        self.v = v


def parse(data):
    i = [0]

    def u1():
        v = data[i[0]]
        i[0] += 1
        return v

    def u2():
        v = struct.unpack_from('>H', data, i[0])[0]
        i[0] += 2
        return v

    def i4():
        v = struct.unpack_from('>i', data, i[0])[0]
        i[0] += 4
        return v

    def nm():
        n = u2()
        s = data[i[0]:i[0] + n].decode('utf8')
        i[0] += n
        return s

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
                k = nm()
                o[k] = read(tt)
        if t == 11:
            n = i4(); return Tag(11, [i4() for _ in range(n)])
        if t == 12:
            n = i4(); out = []
            for _ in range(n):
                out.append(struct.unpack_from('>q', data, i[0])[0])
                i[0] += 8
            return Tag(12, out)
        raise Exception('tag ' + str(t))

    rt = u1()
    nm()
    return read(rt)


def load(path):
    raw = open(path, 'rb').read()
    try:
        return parse(gzip.decompress(raw))
    except Exception:
        return parse(raw)


# ============================================================ nbt writing
def w_str(out, s):
    b = s.encode('utf8')
    out.write(struct.pack('>H', len(b)))
    out.write(b)


def w_payload(out, tag):
    t, v = tag.t, tag.v
    if t == 1:
        out.write(struct.pack('>b', v))
    elif t == 2:
        out.write(struct.pack('>h', v))
    elif t == 3:
        out.write(struct.pack('>i', v))
    elif t == 4:
        out.write(struct.pack('>q', v))
    elif t == 5:
        out.write(struct.pack('>f', v))
    elif t == 6:
        out.write(struct.pack('>d', v))
    elif t == 7:
        out.write(struct.pack('>i', len(v)))
        out.write(bytes(x & 0xFF for x in v))
    elif t == 8:
        w_str(out, v)
    elif t == 9:
        et, items = v
        if not items:
            et = 0
        out.write(struct.pack('>B', et))
        out.write(struct.pack('>i', len(items)))
        for it in items:
            w_payload(out, it)
    elif t == 10:
        for k, val in v.items():
            out.write(struct.pack('>B', val.t))
            w_str(out, k)
            w_payload(out, val)
        out.write(b'\x00')
    elif t == 11:
        out.write(struct.pack('>i', len(v)))
        for x in v:
            out.write(struct.pack('>i', x))
    elif t == 12:
        out.write(struct.pack('>i', len(v)))
        for x in v:
            out.write(struct.pack('>q', x))
    else:
        raise Exception('write tag ' + str(t))


def T_str(s): return Tag(8, s)
def T_int(n): return Tag(3, n)
def T_dbl(d): return Tag(6, d)
def T_cmp(d): return Tag(10, d)
def T_ilist(xs): return Tag(9, (3, [T_int(x) for x in xs]))
def T_dlist(xs): return Tag(9, (6, [T_dbl(x) for x in xs]))


# ================================================= a structure, as a dict
# Everything below works on {(x, y, z): (state string, block-entity nbt)}.
# Splicing two files together by palette index is a bug waiting to happen, and
# a state string is the one representation both of them already agree on.
def state_string(entry):
    name = entry.v['Name'].v
    props = entry.v.get('Properties')
    if not props:
        return name
    return '%s[%s]' % (name, ','.join('%s=%s' % (k, v.v) for k, v in sorted(props.v.items())))


def parse_state(s):
    m = re.match(r'^([^\[]+)(?:\[(.*)\])?$', s)
    entry = {'Name': T_str(m.group(1))}
    if m.group(2):
        entry['Properties'] = T_cmp({
            k: T_str(v) for k, v in (pair.split('=', 1) for pair in m.group(2).split(','))
        })
    return T_cmp(entry)


def to_grid(root):
    palette = root.v['palette'].v[1]
    grid = {}
    for b in root.v['blocks'].v[1]:
        pos = tuple(x.v for x in b.v['pos'].v[1])
        grid[pos] = (state_string(palette[b.v['state'].v]), b.v.get('nbt'))
    return grid, [x.v for x in root.v['size'].v[1]]


AIR = 'minecraft:air'


def put(grid, pos, state, nbt=None):
    grid[pos] = (state, nbt)


def at(grid, pos):
    return grid.get(pos, (AIR, None))[0]


# ================================================================= inputs
barn, barn_size = to_grid(load(BARN_SRC))
house, house_size = to_grid(load(HOUSE_SRC))

# ================================================ the house, before moving
# ---- its jigsaws come out ----------------------------------------------
# Placement machinery. Nothing resolves a jigsaw block when a piece is placed as
# part of a bigger one, so left in they would stand in the finished wall.
for pos in [p for p in house if at(house, p).startswith('minecraft:jigsaw')]:
    x, y, z = pos
    inside = 0 < x < house_size[0] - 1 and 0 < z < house_size[2] - 1
    put(house, pos, 'minecraft:oak_planks' if inside else AIR)

# ---- its furniture comes out --------------------------------------------
# The vanilla bed and the chair beside it. Both are replaced below, and the bed
# has to go before the widening or the widening would saw it in half.
for pos in [p for p in house if at(house, p).startswith('minecraft:white_bed')]:
    put(house, pos, AIR)
put(house, (4, 1, 4), AIR)

# ---- and so does the front door, for the length of the deepening ---------
# The row the deepening duplicates is the door's own row - it is the only
# cross-section of a hip roof that tiles, see DEEPEN_AT - so a door left in
# place would come back once per copy, four doors down the west wall. Lift both
# halves out first, fill the hole with the wall vanilla drew one row along (plain
# cobblestone at both heights), and set the same two states back down at the
# doorway once the building is its final depth.
#
# The pair is read out rather than written in: `hinge=right` and `facing=east`
# are whatever the vanilla export says, and the door that goes back is the door
# that came out.
DOOR_POS = (1, 3)
door_halves = {y: at(house, (DOOR_POS[0], y, DOOR_POS[1])) for y in (1, 2)}
assert all('oak_door' in s for s in door_halves.values()), 'the door moved in the source'
for y in door_halves:
    put(house, (DOOR_POS[0], y, DOOR_POS[1]), at(house, (DOOR_POS[0], y, DOOR_POS[1] - 1)))

# ---- one column wider ----------------------------------------------------
# The vanilla cottage is three by three inside: a bed takes two of the three
# columns and what is left is a single-file squeeze past the end of it. Four is
# the bed and a two-block aisle beside it - and the deepening below is what fills
# that bed column with sleepers. Done by duplicating an interior
# column rather than by drawing a new house: every wall, corner and roof stair
# stays exactly what vanilla drew, and the only thing that changes is that the
# middle of the building is one longer. The roof gains a short ridge where it
# used to come to a point, which is what a hip roof on a rectangle looks like
# anyway.
WIDEN_AT = 3
widened = {}
for (x, y, z), cell in house.items():
    widened[(x + 1 if x >= WIDEN_AT else x, y, z)] = cell
for (x, y, z), cell in list(house.items()):
    if x == WIDEN_AT:
        widened[(x, y, z)] = cell
house = widened
house_size = [house_size[0] + 1, house_size[1], house_size[2]]

# ---- and three rows deeper -----------------------------------------------
# The same trick along z, for the same reason: duplicate a cross-section rather
# than draw a longer house, so the hip roof, the eaves, the corner posts and the
# gable windows stay exactly what vanilla drew.
#
# WHICH row is duplicated is not free choice, and this is the trap. At the top
# course of the roof (y=6) vanilla's three rows are a south-sloping stair course
# at z=2, the ridge at z=3, and a north-sloping stair course at z=4. Only z=3
# tiles: copy it and the ridge simply gets longer, copy either of its neighbours
# and the house grows a flat band of stairs all sloping the same way where the
# roof should be. Read off the source the same way at every other height: y=4's
# eave course and y=5's attic ring both run `stair, planks, void, planks, stair`
# across z=3, which is a section that repeats, while their z=2 and z=4 rows are
# solid and do not. So z=3 is the cross-section at all seven levels - and it is
# also the door's row, which is the whole reason the door is lifted out above.
#
# THREE copies, and it is the frontage that sets the number, not the beds. The
# west wall has to carry the hitch, the doorway, the four profession posts and
# the bench standing side by side, which is eight columns of wall; vanilla's is
# five. The interior that comes with eight is six rows deep, which is one row for
# the chests and one row per sleeper - see the beds below.
#
# The cost is three more blocks of bounding box on the piece's long axis. That
# is the thing most likely to stop the homestead generating, so it is bought for
# the frontage and not spent on floor space nobody stands in.
DEEPEN_AT = 3
DEEPEN_BY = 3
deepened = {}
for (x, y, z), cell in house.items():
    deepened[(x, y, z + DEEPEN_BY if z >= DEEPEN_AT else z)] = cell
for (x, y, z), cell in list(house.items()):
    if z == DEEPEN_AT:
        for extra in range(DEEPEN_BY):
            deepened[(x, y, z + extra)] = cell
house = deepened
house_size = [house_size[0], house_size[1], house_size[2] + DEEPEN_BY]

# ---- the door goes back in ------------------------------------------------
# At the same z it came out of: the copies land *after* DEEPEN_AT, so the row the
# door was in is still the row at DEEPEN_AT, and the doorway stays where vanilla
# put it relative to the front corner rather than sliding down the wall.
assert DOOR_POS[1] == DEEPEN_AT, 'the deepened row is not the door row - the door would move'
for y, state in door_halves.items():
    put(house, (DOOR_POS[0], y, DOOR_POS[1]), state)

# ---- bark back on the corner posts ---------------------------------------
# Stripped oak in the vanilla house. Nothing else about a homestead beside a
# stable is planed.
for pos, (state, nbt) in list(house.items()):
    if state.startswith('minecraft:stripped_oak_log'):
        put(house, pos, state.replace('stripped_oak_log', 'oak_log'), nbt)

# ---- where the inside of the house actually is ---------------------------
# Read off the finished grid rather than counted up from WIDEN_AT and DEEPEN_BY
# by hand. Vanilla floors the room in oak_planks and nothing else in the building
# is oak_planks at the foundation course - the walls are cobblestone, the corners
# are log - so the plank footprint at y=0 *is* the interior, and it is right by
# construction however the two duplications are retuned.
floor = [(x, z) for (x, y, z) in house
         if y == 0 and at(house, (x, y, z)) == 'minecraft:oak_planks']
IN_X = (min(x for x, _ in floor), max(x for x, _ in floor))
IN_Z = (min(z for _, z in floor), max(z for _, z in floor))

# ---- a bed per sleeper down the back wall, chests in the row behind -------
# FIVE sleepers, because the frontage hands out five jobs: the cowboy from the
# hitch, and the leatherworker, scientist, supplier and metalsmith from the four
# posts. A villager who claims a job site and has no bed to go home to is a
# villager who never sleeps and never restocks, so the bed count is not decor -
# it is the other half of the post count, and the two are written down together.
#
# The beds run along x against the wall opposite the door, one per interior row,
# so the row count IS the sleeper count and neither number is free to drift.
# Heads to the back wall, feet to the door: the head takes the last interior
# column before the east wall, the foot the one in front of it, and walking in
# through the west door you get the feet and the length of the aisle.
#
# The one interior row the beds do not take is the row against the north wall,
# the one you look down on walking in, and it stays the chest row - the two ends
# of it are the only floor in the building with nothing sleeping on them.
SLEEPERS = 5
bed_rows = list(range(IN_Z[0] + 1, IN_Z[1] + 1))
assert len(bed_rows) == SLEEPERS, \
    'the house is %d rows deep inside, which sleeps %d - retune DEEPEN_BY' % (
        IN_Z[1] - IN_Z[0] + 1, len(bed_rows))
for z in bed_rows:
    put(house, (IN_X[1], 1, z), 'minecraft:white_bed[facing=east,occupied=false,part=head]')
    put(house, (IN_X[1] - 1, 1, z), 'minecraft:white_bed[facing=east,occupied=false,part=foot]')

# One chest in each end of that row, so neither can be buried by a bed and both
# keep their old place at the corners of the room. They do not face the same way:
# the one on the aisle side looks south down the length of the room, and the one
# on the bed side has a bed head immediately south of it, so it turns to look at
# the open square beside it instead. Both are asserted, not eyeballed - the cell
# the chest goes in has to be empty, and the cell it opens towards has to be
# somewhere a player can stand.
CHEST_LOOT = 'horsegenetics:chests/cowboy_house'
CHEST_Z = IN_Z[0]
for x, facing, dx, dz in ((IN_X[0], 'south', 0, 1), (IN_X[1], 'west', -1, 0)):
    assert at(house, (x, 1, CHEST_Z)) == AIR, 'a chest would bury something'
    assert at(house, (x + dx, 1, CHEST_Z + dz)) == AIR, 'no room to stand and open the chest'
    put(house, (x, 1, CHEST_Z),
        'minecraft:chest[facing=%s,type=single,waterlogged=false]' % facing,
        T_cmp({'LootTable': T_str(CHEST_LOOT), 'id': T_str('minecraft:chest')}))

# ---- the frontage: which column, and which rows of it are real -----------
# The posts stand in the empty margin column outside the west wall, one block
# west of the wall itself - so FRONT_X is derived from the interior rather than
# written down, and the wall it leans on is the column between them.
#
# Not every z in that column is a place a post may stand. Outside the walls it is
# thin air with the roof's eave overhead, and the eave reaches a block further
# than the building does - so "is there something above it" is not the test.
# The test is the wall's own foundation course: wherever the west wall has a
# block at y=0, the post in front of it has a building at its back, and wherever
# it has not, the post would be standing in the front garden. Read that straight
# off the grid the deepening has just built, because DEEPEN_BY moves it.
FRONT_X = IN_X[0] - 2
WALL_X = FRONT_X + 1
frontage = [z for z in range(house_size[2]) if at(house, (WALL_X, 0, z)) != AIR]
assert frontage == list(range(frontage[0], frontage[-1] + 1)), 'the west wall has a hole in it'

# ---- a step up to the front door -----------------------------------------
# The house sits a block proud of the ground the way every vanilla plains house
# does, so without one the threshold is a ledge you jump at.
put(house, (FRONT_X, 0, DOOR_POS[1]),
    'minecraft:oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]')

# ---- the work posts, down the frontage from that step --------------------
# On the house and not on the barn, because this is where the trade is done: you
# walk up to the front door of the homestead, not into the stable. Every one of
# them is the same block trick - a job site a profession-less villager claims,
# see server/CowboyHitchHandler - and each one has a bed waiting for its taker
# inside, which is what SLEEPERS above is counting.
#
# The order is the order you meet them walking the path from the street, and the
# hitch comes first because the cowboy is the one who has to exist before any of
# the rest of the homestead means anything. The doorway splits them: hitch on the
# near side, the four trades on the far side, all of it derived from the door's
# own row so that moving the door moves the whole frontage with it.
#
# The bench is not a job site - nobody is employed by it - so it goes at the far
# END of the frontage, past the last post, where it fronts the house's back
# corner post the way it used to front the front one. One here so a player meets
# tack dyeing before they know to want it; it is craftable too.
WORK_POSTS = [
    ('horsegenetics:cowboy_hitch', DOOR_POS[1] - 1),
    ('horsegenetics:leatherworkers_post', DOOR_POS[1] + 1),
    ('horsegenetics:scientists_post', DOOR_POS[1] + 2),
    ('horsegenetics:suppliers_post', DOOR_POS[1] + 3),
    ('horsegenetics:metalsmiths_post', DOOR_POS[1] + 4),
]
BENCH_Z = frontage[-1]

# `put` overwrites without checking what was there, and the two ways to get this
# wrong are opposites: a post past the end of the wall hangs in the air over the
# walk, and a post on a z that is already spoken for silently eats the doorstep.
# So every coordinate is checked against the grid as built - fronting wall, and
# empty right now - rather than against a count that was right last time.
for block, z in WORK_POSTS + [('horsegenetics:equestrian_bench', BENCH_Z)]:
    assert z in frontage, '%s at z=%d is off the end of the wall' % (block, z)
    assert at(house, (FRONT_X, 0, z)) == AIR, \
        '%s at z=%d would overwrite %s' % (block, z, at(house, (FRONT_X, 0, z)))
    put(house, (FRONT_X, 0, z), block)

# ============================================== compose the two buildings
# The house goes behind the barn along z, and both front faces stay on the same
# x - which is the side the village street attaches to, so the street reaches
# the whole homestead and not just the stable.
#
# Neither building starts at x=0. The westernmost column of the piece is left
# empty for the walk that runs along the front of both of them, because the walk
# has to be *in front of* the steps and there is nowhere else for it to go: one
# column further west is the street's own connector block, and a piece whose box
# reaches over that overlaps the street piece and is thrown out by the placer, so
# the homestead would simply never generate.
WALK_X = 0
BUILDINGS_X = WALK_X + 1
HOUSE_AT = (BUILDINGS_X, 0, 8)

grid = {(x + BUILDINGS_X, y, z): cell for (x, y, z), cell in barn.items()}
for (x, y, z), cell in house.items():
    grid[(x + HOUSE_AT[0], y + HOUSE_AT[1], z + HOUSE_AT[2])] = cell

size = [
    max(barn_size[0] + BUILDINGS_X, house_size[0] + HOUSE_AT[0]),
    max(barn_size[1], house_size[1] + HOUSE_AT[1]),
    max(barn_size[2], house_size[2] + HOUSE_AT[2]),
]

# ---- a ground course under both buildings --------------------------------
# Both buildings are drawn with their foundation on layer 0, and that layer is
# the one that ends up resting *on* the ground - the street's road block is the
# layer below it, outside the piece entirely (see the jigsaw note further down).
# So layer 0 is not the ground, it is the first course of the building, and
# anything laid there that is meant to be walked on ends up a block-high curb.
#
# The lane was exactly that. To put a block at road level the piece needs a
# layer below the foundation, and a structure cannot have a negative one - so
# everything moves up by one and the new layer 0 becomes the ground the
# homestead stands on. Nothing about where the piece lands changes: the jigsaw
# rides up with the rest, so the foundation is still the course that meets the
# street.
GROUND = 1
grid = {(x, y + GROUND, z): cell for (x, y, z), cell in grid.items()}
size[1] += GROUND

# ---- the walk along the front ---------------------------------------------
# One column of dirt path, the same block the village's own streets are made of,
# laid in that ground course so you walk on it rather than over it. It runs the
# whole length of the piece down the empty column, which puts it at the foot of
# the barn's stair skirt, in front of the house's doorstep, and in front of both
# work posts - one walk from the street to the front door, past everything.
#
# There was a second run of it between the two buildings once. It is gone: the
# gap between them is a yard, not a corridor, and a path down it only led from
# the front of the homestead back to the front of the homestead.
#
# The column has clear sky the whole way, which it has to have.
# `DirtPathBlock.canSurvive` is false with a solid block overhead, so a path
# block under a step or a post would be scheduled to tick straight back to plain
# dirt - laying dirt into the world under the guise of laying a path. In front
# of the steps rather than under them, nothing is covered and nothing converts.
PATH = 'minecraft:dirt_path'
for z in range(size[2]):
    assert at(grid, (WALK_X, GROUND, z)) == AIR, 'the walk column must stay clear'
    put(grid, (WALK_X, 0, z), PATH)

# ================================================= the barn's own fixings
# ---- the jigsaw connector -------------------------------------------------
# It sits in the walk column, level with the barn's north pair of doors; the
# generator rotates the whole piece so this ends up facing back at whichever
# street connector it attached to. It used to sit in the barn's west face, and
# moved out with the walk - the position it needs is the piece's own west edge,
# which is now this column rather than the building.
#
# The barn's foundation course, and nothing else. A jigsaw pair lands the two
# blocks at the same world height, and the street connector it meets sits at the
# street piece's y=1, one *above* the road block. So whatever local layer carries
# this jigsaw is the layer that ends up resting on the ground. Vanilla houses put
# their `building_entrance` jigsaw in their foundation layer for exactly that
# reason, and they are the shape to copy: a plains house sits one block proud of
# the road with a step up into the doorway.
#
# It was a course too low once, which sank the whole homestead a block: the
# foundation landed in the road's own layer, the stair skirt round the doors was
# buried level with the ground instead of stepping up onto it, and the building
# read as half-dug-in. So this tracks GROUND rather than naming a number - the
# foundation is wherever the lift left it.
#
# final_state is the block the jigsaw replaces itself with once the piece is
# placed - read straight back out of the grid, so whatever occupies the spot is
# what is put there. In the walk column that is air, one block above the path,
# and it has to stay air: `JigsawReplacementProcessor` swaps the jigsaw out
# during placement, so a solid final_state here would be a block sitting on the
# walk and would tick the path under it back to dirt.
JIGSAW_POS = (WALK_X, GROUND, 2)
final_state = at(grid, JIGSAW_POS)
assert final_state == AIR, 'final_state would cover the walk'
put(grid, JIGSAW_POS, 'minecraft:jigsaw[orientation=west_up]', T_cmp({
    'id': T_str('minecraft:jigsaw'),
    'name': T_str('minecraft:street'),
    'target': T_str('minecraft:street'),
    'pool': T_str('minecraft:empty'),
    'joint': T_str('aligned'),
    'final_state': T_str(final_state),
    'selection_priority': T_int(0),
    'placement_priority': T_int(0),
}))

# ---- headroom over the barn's doorways -----------------------------------
# Minecraft's ground pathfinder does not measure a mob, it rounds it up:
# WalkNodeEvaluator asks for floor(width + 1) blocks across and floor(height + 1)
# blocks up, and refuses a route that has not got them. A vanilla-sized horse is
# 1.40 x 1.60, so it wants 2 x 2 - which is exactly what a double door with a
# solid block over it provides, and exactly why the barn worked for ordinary
# horses and jammed for big ones.
#
# The scale attribute multiplies both numbers, so the thresholds are sharp:
#
#     scale < 1.25      2 wide, 2 tall   fits as drawn
#     scale 1.25-1.43   2 wide, 3 tall   needs this
#     scale >= 1.43     3 wide, 3 tall   will not fit a double door at all
#
# So the course of blocks over each doorway is cleared to air, which buys every
# horse up to about 1.43. It was oak fence, which is the worst possible choice
# there twice over - a full pathfinding blocker in its own right, and it held
# the opening to two blocks. There is no "better block": anything solid enough
# to read as a lintel is solid enough to stop a horse, so the answer is nothing
# at all. A horse over 1.43 needs a three-wide doorway, which is a change to the
# building rather than to its trim.
CLEAR_ABOVE_DOORS = 3 + GROUND
door_columns = {(x + BUILDINGS_X, z) for (x, y, z) in barn if '_door' in at(barn, (x, y, z))}
cleared = 0
for (x, z) in door_columns:
    if at(grid, (x, CLEAR_ABOVE_DOORS, z)) != AIR:
        put(grid, (x, CLEAR_ABOVE_DOORS, z), AIR)
        cleared += 1

# ============================================================== the people
# One plain villager on the walk per work post, each facing its own post across
# it - on the path itself rather than on top of the post, which is where they
# stood while the path was a course too high and a column too far in. The bench
# gets nobody, because nobody is employed by it.
#
# NONE of them is given a profession, and that is the point rather than an
# oversight: each has to claim its post the ordinary way and come out a cowboy, a
# leatherworker, a scientist, a supplier or a metalsmith. Letting them take the
# jobs themselves is the only test there is of the part that has ever been in
# doubt - whether every POI registered and whether the acquirable_job_site tag
# merged for all five - so a row of nitwits standing there for ever is a real
# answer to a real question, and one that names which post is the broken one.
entities = []
for _, z in WORK_POSTS:
    stand = (WALK_X, GROUND, HOUSE_AT[2] + z)
    entities.append(T_cmp({
        'pos': T_dlist([stand[0] + 0.5, float(stand[1]), stand[2] + 0.5]),
        'blockPos': T_ilist(list(stand)),
        'nbt': T_cmp({'id': T_str('minecraft:villager')}),
    }))

# ================================================================== output
palette, index, blocks = [], {}, []
for pos in sorted(grid):
    state, nbt = grid[pos]
    if state not in index:
        index[state] = len(palette)
        palette.append(parse_state(state))
    entry = {'pos': T_ilist(list(pos)), 'state': T_int(index[state])}
    if nbt is not None:
        entry['nbt'] = nbt
    blocks.append(T_cmp(entry))

root = T_cmp({
    'size': T_ilist(size),
    'entities': Tag(9, (10, entities)),
    'blocks': Tag(9, (10, blocks)),
    'palette': Tag(9, (10, palette)),
    'DataVersion': T_int(4790),
})

out = io.BytesIO()
out.write(struct.pack('>B', 10))
w_str(out, '')
w_payload(out, root)
# mtime=0 so the bake is byte-reproducible. Without it gzip stamps the current
# time into the header, every run rewrites the file, and `git status` stops
# being able to tell you the piece is stale - which is the only staleness signal
# a checked-in derived artefact has.
open(DST, 'wb').write(gzip.compress(out.getvalue(), mtime=0))
print('wrote', DST,
      'size', size, 'palette', len(palette), 'blocks', len(blocks),
      'jigsaw at', JIGSAW_POS, 'final_state', final_state,
      'door headers cleared', cleared,
      'house interior x', IN_X, 'z', IN_Z,
      'beds', len(bed_rows), 'frontage z', (frontage[0], frontage[-1]),
      'villagers', len(entities))
