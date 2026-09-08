"""Bake the third-party stable schematics into Minecraft structure NBTs.

    python neoforge-26.1.2/tools/stables/bake-stables.py

Sources are in this directory as `<id>.source.nbt` and the outputs land in
`src/main/resources/data/horsegenetics/structure/<id>.nbt`. Re-run after
replacing a source, then commit both.

Attribution and the terms each build was downloaded under are in
`wiki/stables.html` - that page is the source of truth for who made what, and a
new stable is not finished until it is on it.

WHY A BAKE STEP EXISTS AT ALL
=============================
None of these files is a Minecraft structure. They are WorldEdit exports, in
two different formats, neither of which the game can read:

  * **Sponge Schematic v2** (`horsestable`, `tm_u_stable`) - a `Palette` of
    state strings, a `BlockData` byte array of LEB128 varints, and `Width` /
    `Height` / `Length` shorts. Close to a structure and still not one.
  * **MCEdit legacy** (`stables`) - `Materials: "Alpha"`, and *numeric* block
    ids with a data nibble. No block names anywhere in the file; it predates
    the flattening.

A structure NBT is `{size, palette, blocks, entities, DataVersion}` with the
blocks as a sparse list of positions. This script writes that.

THE DATA VERSION IS THE TRICK
=============================
The output keeps **DataVersion 1976** (Minecraft 1.13.2) rather than claiming
to be current, and that is deliberate: `TemplateSource.readStructure` runs
every structure it loads through `DataFixTypes.STRUCTURE.updateToCurrentVersion`
against the file's own `DataVersion`. So `grass_path` becomes `dirt_path`,
`grass` becomes `short_grass`, `cauldron` grows its water levels, and every
other rename between 1.13 and today is done by the game, correctly, for free.

That is also why the legacy numeric ids below only have to reach **1.13**. The
table stops there on purpose; carrying it forward by hand would be redoing work
the DataFixer already does, and getting it subtly wrong.

WHAT THE BAKE CHANGES
=====================
  * Block entities are **dropped** - the blocks stay, their contents do not. A
    chest full of 1.13 item ids is a second class of upgrade to get wrong for
    no gain, and an empty chest in a stable is not a loss.
  * Entities are dropped. The horses are the mod's business
    (`server/StablePopulator`), and a schematic's stray armour stand is not.
  * Blocks from mods this mod does not depend on are substituted - see
    `FOREIGN`. `tm_u_stable` carries `placeableitems:saddle_stand_block`, and a
    structure naming a block that does not exist fails to load at all.
"""
import gzip
import io
import os
import struct
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
MODULE = os.path.abspath(os.path.join(HERE, '..', '..'))
DST_DIR = os.path.join(MODULE, 'src', 'main', 'resources', 'data', 'horsegenetics', 'structure')

# 1.13.2. See "THE DATA VERSION IS THE TRICK" above - do not raise this.
DATA_VERSION = 1976

# A block from a mod this one does not depend on becomes the nearest thing that
# reads the same in a stable. A structure naming an unknown block does not
# degrade, it fails to load.
FOREIGN = {
    'placeableitems:saddle_stand_block': 'minecraft:barrel[facing=up,open=false]',
}


# ============================================================ nbt reading
class Tag:
    def __init__(self, t, v):
        self.t = t
        self.v = v


def parse(data):
    pos = [0]

    def u(fmt, n):
        v = struct.unpack_from(fmt, data, pos[0])[0]
        pos[0] += n
        return v

    def rstr():
        n = u('>H', 2)
        s = data[pos[0]:pos[0] + n].decode('utf-8')
        pos[0] += n
        return s

    def payload(t):
        if t == 1:
            return u('>b', 1)
        if t == 2:
            return u('>h', 2)
        if t == 3:
            return u('>i', 4)
        if t == 4:
            return u('>q', 8)
        if t == 5:
            return u('>f', 4)
        if t == 6:
            return u('>d', 8)
        if t == 7:
            n = u('>i', 4)
            b = data[pos[0]:pos[0] + n]
            pos[0] += n
            return b
        if t == 8:
            return rstr()
        if t == 9:
            it = u('>b', 1)
            n = u('>i', 4)
            return (it, [Tag(it, payload(it)) for _ in range(n)])
        if t == 10:
            d = {}
            while True:
                ct = u('>b', 1)
                if ct == 0:
                    return d
                name = rstr()
                d[name] = Tag(ct, payload(ct))
        if t == 11:
            n = u('>i', 4)
            return [u('>i', 4) for _ in range(n)]
        if t == 12:
            n = u('>i', 4)
            return [u('>q', 8) for _ in range(n)]
        raise ValueError('unknown tag %d' % t)

    t = u('>b', 1)
    rstr()
    return Tag(t, payload(t))


def load(path):
    raw = open(path, 'rb').read()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    return parse(raw)


# ============================================================ nbt writing
def w_str(out, s):
    b = s.encode('utf-8')
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
        out.write(v)
    elif t == 8:
        w_str(out, v)
    elif t == 9:
        it, items = v
        out.write(struct.pack('>b', it))
        out.write(struct.pack('>i', len(items)))
        for item in items:
            w_payload(out, item)
    elif t == 10:
        for name, child in v.items():
            out.write(struct.pack('>b', child.t))
            w_str(out, name)
            w_payload(out, child)
        out.write(struct.pack('>b', 0))
    else:
        raise ValueError('cannot write tag %d' % t)


def T_str(s): return Tag(8, s)
def T_int(n): return Tag(3, n)
def T_cmp(d): return Tag(10, d)
def T_ilist(xs): return Tag(9, (3, [T_int(x) for x in xs]))


# ============================================================ state strings
def split_state(s):
    """`a:b[k=v,...]` -> ('a:b', {'k': 'v'})"""
    if '[' not in s:
        return s, {}
    name, rest = s.split('[', 1)
    props = {}
    for part in rest.rstrip(']').split(','):
        if part:
            k, v = part.split('=', 1)
            props[k] = v
    return name, props


def palette_entry(state):
    name, props = split_state(state)
    d = {'Name': T_str(name)}
    if props:
        d['Properties'] = T_cmp({k: T_str(v) for k, v in sorted(props.items())})
    return T_cmp(d)


# ============================================================ readers
def read_sponge(root):
    """Sponge Schematic v2 -> (w, h, l, [(x, y, z, state_string)])."""
    d = root.v
    w, h, l = d['Width'].v, d['Height'].v, d['Length'].v
    ids = {}
    for state, tag in d['Palette'].v.items():
        ids[tag.v] = state
    raw = d['BlockData'].v

    # LEB128, one varint per block, in y-z-x order.
    values = []
    i = 0
    while i < len(raw):
        val = 0
        shift = 0
        while True:
            b = raw[i]
            i += 1
            val |= (b & 0x7F) << shift
            if not (b & 0x80):
                break
            shift += 7
        values.append(val)
    if len(values) != w * h * l:
        raise ValueError('BlockData is %d entries for a %dx%dx%d volume'
                         % (len(values), w, h, l))

    out = []
    for y in range(h):
        for z in range(l):
            for x in range(w):
                state = ids[values[y * w * l + z * w + x]]
                out.append((x, y, z, state))
    return w, h, l, out


# 1.12 numeric id -> a 1.13 block state. Only the ids these sources actually
# use, because a table nobody needs is a table nobody checks. The DataFixer
# carries 1.13 forward from here.
COLOURS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
           'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']


def legacy_state(bid, data):
    facing4 = ['east', 'west', 'south', 'north']          # stairs / ladders order
    torch4 = {1: 'east', 2: 'west', 3: 'south', 4: 'north'}
    sign4 = {2: 'north', 3: 'south', 4: 'west', 5: 'east'}
    axis3 = {0: 'y', 4: 'x', 8: 'z'}

    if bid == 0:
        return 'minecraft:air'
    if bid == 2:
        return 'minecraft:grass_block[snowy=false]'
    if bid == 3:
        return 'minecraft:dirt'
    if bid == 5:
        return ['minecraft:oak_planks', 'minecraft:spruce_planks', 'minecraft:birch_planks',
                'minecraft:jungle_planks', 'minecraft:acacia_planks',
                'minecraft:dark_oak_planks'][data % 6]
    if bid == 13:
        return 'minecraft:gravel'
    if bid == 35:
        return 'minecraft:%s_wool' % COLOURS[data]
    if bid == 43:  # full double slab -> the solid block it doubles
        return ['minecraft:smooth_stone', 'minecraft:sandstone', 'minecraft:oak_planks',
                'minecraft:cobblestone', 'minecraft:bricks', 'minecraft:stone_bricks',
                'minecraft:nether_bricks', 'minecraft:quartz_block'][data % 8]
    if bid == 44:
        kind = ['minecraft:smooth_stone_slab', 'minecraft:sandstone_slab', 'minecraft:oak_slab',
                'minecraft:cobblestone_slab', 'minecraft:brick_slab', 'minecraft:stone_brick_slab',
                'minecraft:nether_brick_slab', 'minecraft:quartz_slab'][data % 8]
        half = 'top' if data & 8 else 'bottom'
        return '%s[type=%s,waterlogged=false]' % (kind, half)
    if bid == 50:
        if data in torch4:
            return 'minecraft:wall_torch[facing=%s]' % torch4[data]
        return 'minecraft:torch'
    if bid == 55:
        return 'minecraft:redstone_wire[east=none,north=none,power=0,south=none,west=none]'
    if bid == 68:
        return 'minecraft:oak_wall_sign[facing=%s,waterlogged=false]' % sign4.get(data, 'north')
    if bid == 72:
        return 'minecraft:oak_pressure_plate[powered=false]'
    if bid == 85:
        return 'minecraft:oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]'
    if bid == 96:
        return ('minecraft:oak_trapdoor[facing=%s,half=%s,open=%s,powered=false,waterlogged=false]'
                % (facing4[data & 3], 'top' if data & 8 else 'bottom',
                   'true' if data & 4 else 'false'))
    if bid == 98:
        return ['minecraft:stone_bricks', 'minecraft:mossy_stone_bricks',
                'minecraft:cracked_stone_bricks', 'minecraft:chiseled_stone_bricks'][data % 4]
    if bid == 101:
        return 'minecraft:iron_bars[east=false,north=false,south=false,waterlogged=false,west=false]'
    if bid == 109:
        return ('minecraft:stone_brick_stairs[facing=%s,half=%s,shape=straight,waterlogged=false]'
                % (facing4[data & 3], 'top' if data & 4 else 'bottom'))
    if bid == 118:
        return 'minecraft:cauldron[level=%d]' % min(3, data)
    if bid == 125:
        return ['minecraft:oak_planks', 'minecraft:spruce_planks', 'minecraft:birch_planks',
                'minecraft:jungle_planks', 'minecraft:acacia_planks',
                'minecraft:dark_oak_planks'][data % 6]
    if bid == 126:
        kind = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak'][data % 6]
        half = 'top' if data & 8 else 'bottom'
        return 'minecraft:%s_slab[type=%s,waterlogged=false]' % (kind, half)
    if bid == 140:
        return 'minecraft:flower_pot'
    if bid == 160:
        return ('minecraft:%s_stained_glass_pane'
                '[east=false,north=false,south=false,waterlogged=false,west=false]' % COLOURS[data])
    if bid == 162:  # log2: acacia / dark oak
        kind = 'acacia' if (data & 3) == 0 else 'dark_oak'
        return 'minecraft:%s_log[axis=%s]' % (kind, axis3.get(data & 12, 'y'))
    if bid == 164:
        return ('minecraft:dark_oak_stairs[facing=%s,half=%s,shape=straight,waterlogged=false]'
                % (facing4[data & 3], 'top' if data & 4 else 'bottom'))
    if bid == 169:
        return 'minecraft:sea_lantern'
    if bid == 170:
        return 'minecraft:hay_block[axis=%s]' % axis3.get(data & 12, 'y')
    if bid == 191:
        return 'minecraft:dark_oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]'
    if bid == 193:
        # Doors carry half the state in the block above; the DataFixer sorts a
        # door out from the pair, so the facing here only has to be plausible.
        half = 'upper' if data & 8 else 'lower'
        return ('minecraft:spruce_door[facing=north,half=%s,hinge=left,open=false,powered=false]'
                % half)
    if bid == 208:
        return 'minecraft:grass_path'
    raise KeyError('no 1.13 mapping for legacy block id %d (data %d)' % (bid, data))


def read_legacy(root):
    """MCEdit `.schematic` -> (w, h, l, [(x, y, z, state_string)])."""
    d = root.v
    w, h, l = d['Width'].v, d['Height'].v, d['Length'].v
    blocks = d['Blocks'].v
    data = d['Data'].v
    if len(blocks) != w * h * l:
        raise ValueError('Blocks is %d entries for a %dx%dx%d volume' % (len(blocks), w, h, l))

    cache = {}
    out = []
    for y in range(h):
        for z in range(l):
            for x in range(w):
                i = y * w * l + z * w + x
                key = (blocks[i] & 0xFF, data[i] & 0x0F)
                state = cache.get(key)
                if state is None:
                    state = legacy_state(*key)
                    cache[key] = state
                out.append((x, y, z, state))
    return w, h, l, out


# ============================================================ the bake
def bake(source, dest):
    root = load(source)
    keys = root.v.keys()
    if 'BlockData' in keys:
        kind = 'sponge v2'
        w, h, l, cells = read_sponge(root)
    elif 'Blocks' in keys:
        kind = 'mcedit legacy'
        w, h, l, cells = read_legacy(root)
    else:
        raise ValueError('%s is neither a Sponge schematic nor an MCEdit one' % source)

    palette = []
    index = {}
    blocks = []
    substituted = 0
    air = 0
    for x, y, z, state in cells:
        name = state.split('[')[0]
        if name in FOREIGN:
            state = FOREIGN[name]
            substituted += 1
        if state == 'minecraft:air':
            air += 1
            continue  # a structure's blocks list is sparse; air is the gap
        i = index.get(state)
        if i is None:
            i = len(palette)
            index[state] = i
            palette.append(palette_entry(state))
        blocks.append(T_cmp({'pos': T_ilist([x, y, z]), 'state': T_int(i)}))

    out_root = T_cmp({
        'size': T_ilist([w, h, l]),
        'entities': Tag(9, (10, [])),
        'blocks': Tag(9, (10, blocks)),
        'palette': Tag(9, (10, palette)),
        'DataVersion': T_int(DATA_VERSION),
    })

    buf = io.BytesIO()
    buf.write(struct.pack('>B', 10))
    w_str(buf, '')
    w_payload(buf, out_root)
    # mtime=0 so the bake is byte-reproducible, exactly as bake-barn.py does:
    # without it every run rewrites the file and `git status` stops being able
    # to tell you the piece is stale.
    open(dest, 'wb').write(gzip.compress(buf.getvalue(), mtime=0))
    print('%-22s %-14s %2dx%2dx%2d  palette %3d  blocks %6d  air %6d  substituted %d'
          % (os.path.basename(dest), kind, w, h, l, len(palette), len(blocks), air, substituted))


def main():
    os.makedirs(DST_DIR, exist_ok=True)
    sources = sorted(f for f in os.listdir(HERE) if f.endswith('.source.nbt'))
    if not sources:
        print('no *.source.nbt in', HERE)
        return 1
    for name in sources:
        stable_id = name[:-len('.source.nbt')]
        bake(os.path.join(HERE, name), os.path.join(DST_DIR, stable_id + '.nbt'))
    return 0


if __name__ == '__main__':
    sys.exit(main())
