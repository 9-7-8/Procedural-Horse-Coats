package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.block.HayPortalBlock;
import com.example.horsegenetics.neoforge.data.HorseRealmSize;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * <b>Everything in the horse realm that the flat generator cannot make.</b> The
 * water, the hundred exits and the perimeter, worked out from a chunk position
 * and written when that chunk loads.
 *
 * <h2>Why a chunk-load pass and not a chunk generator</h2>
 * The ground itself - bedrock at Y={@value HorseRealm#BEDROCK_Y}, grass at
 * Y={@value HorseRealm#GROUND_Y} - is three lines of {@code minecraft:flat} in
 * {@code dimension/horse_realm.json}, which is a path this mod already runs on
 * and a codec it does not have to register. What flat cannot do is a fixed grid:
 * vanilla has no placement modifier meaning "at the origin of every fifth
 * chunk", and a hay portal is a block with a block entity, which a
 * {@code ProtoChunk} makes awkward. Both are trivial against a live
 * {@link ServerLevel}, so that is where they are done.
 *
 * <h2>Why it is safe to run on every load</h2>
 * Nothing here is remembered, so nothing can go stale - but nothing is written
 * twice either. Most chunks do <b>no</b> block access at all: the pool grid is
 * one chunk in twenty-five, the portal grid one in ten thousand, and the
 * perimeter is four lines of chunks in a million. A chunk that does qualify
 * reads before it writes, so a second load is a handful of comparisons. That is
 * cheaper than a saved "decorated" flag, and it means a chunk generated before a
 * change to this class is corrected the next time anybody walks into it.
 *
 * <p>Work is queued from {@link ChunkEvent.Load} and applied on the next server
 * tick, never inside the load itself - writing blocks into a chunk the chunk
 * system is still handing out is how a deadlock is written.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HorseRealmTerrain {

    /** Chunks waiting for their pools, portal or perimeter. Packed by {@link ChunkPos#pack()}. */
    private static final Deque<Long> PENDING = new ArrayDeque<>();

    /**
     * How many queued chunks are decorated per tick. A player flying into fresh
     * country loads chunks far faster than they can look at them, and a burst of
     * portal frames in one tick is a stutter for a field nobody has reached yet.
     */
    private static final int PER_TICK = 8;

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !HorseRealm.isRealm(level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        HorseRealmSize size = HorseRealmSize.get(server);
        ChunkPos at = event.getChunk().getPos();
        if (interesting(at.x(), at.z(), size.radius(), size.retiredRings())) {
            PENDING.add(at.pack());
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        ServerLevel realm = event.getServer().getLevel(HorseRealm.REALM_LEVEL);
        if (realm == null) {
            PENDING.clear();
            return;
        }
        for (int done = 0; done < PER_TICK && !PENDING.isEmpty(); done++) {
            ChunkPos at = ChunkPos.unpack(PENDING.poll());
            // It may have unloaded again while it sat in the queue. Touching it
            // would load it back, which is the opposite of what a realm with
            // nobody in it is supposed to do.
            if (realm.hasChunk(at.x(), at.z())) {
                decorate(realm, at);
            }
        }
    }

    /** Decorate a chunk right now - used on arrival, so nobody lands beside a missing exit. */
    public static void decorateNow(ServerLevel realm, ChunkPos at) {
        MinecraftServer server = realm.getServer();
        if (server == null) {
            return;
        }
        HorseRealmSize size = HorseRealmSize.get(server);
        if (interesting(at.x(), at.z(), size.radius(), size.retiredRings())) {
            decorate(realm, at);
        }
    }

    private static boolean interesting(int cx, int cz, int radius, java.util.List<Integer> retired) {
        if (HorseRealm.isPoolChunk(cx, cz, radius) || HorseRealm.isPortalChunk(cx, cz)
                || touchesRing(cx, cz, radius) || touchesOldField(cx, cz)) {
            return true;
        }
        for (int old : retired) {
            if (touchesRing(cx, cz, old)) {
                return true;
            }
        }
        return false;
    }

    private static void decorate(ServerLevel realm, ChunkPos at) {
        // Always first. A chunk built on the old floor would otherwise get a new
        // portal at the new surface and then have its old one dropped on top of
        // it when the lift caught up - see HorseRealmLift.
        HorseRealmLift.liftNow(realm, at);

        MinecraftServer server = realm.getServer();
        if (server == null) {
            return;
        }
        HorseRealmSize size = HorseRealmSize.get(server);
        int radius = size.radius();

        // Take things down before putting things up, so a chunk that is both an
        // old wall and a new one ends with the new one standing.
        if (touchesOldField(at.x(), at.z())) {
            eraseOldField(realm, at, radius);
        }
        for (int old : size.retiredRings()) {
            if (touchesRing(at.x(), at.z(), old)) {
                clearRing(realm, at, old, radius);
            }
        }

        if (HorseRealm.isPoolChunk(at.x(), at.z(), radius)) {
            pool(realm, at);
        }
        if (HorseRealm.isPortalChunk(at.x(), at.z())) {
            portal(realm, at);
        }
        if (touchesRing(at.x(), at.z(), radius)) {
            perimeter(realm, at, radius);
        }
    }

    // --- water ---

    /**
     * A {@value HorseRealm#POOL_SIZE}x{@value HorseRealm#POOL_SIZE} source-block
     * pool sunk into the surface course at the cell origin. One block deep,
     * flush with the grass and bounded by it on all four sides, so it cannot
     * flow, cannot drown a foal, and is not a step up onto anything.
     */
    private static void pool(ServerLevel realm, ChunkPos at) {
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int dx = 0; dx < HorseRealm.POOL_SIZE; dx++) {
            for (int dz = 0; dz < HorseRealm.POOL_SIZE; dz++) {
                BlockPos p = new BlockPos(at.getMinBlockX() + dx, HorseRealm.GROUND_Y, at.getMinBlockZ() + dz);
                if (!realm.getBlockState(p).is(Blocks.WATER)) {
                    realm.setBlock(p, water, 2);
                }
            }
        }
    }

    // --- exits ---

    /**
     * One of the hundred. A cobblestone frame with a {@value HorseRealm#PORTAL_INNER_W}
     * by {@value HorseRealm#PORTAL_INNER_H} opening already lit, standing on the
     * plane at chunk-local z={@value HorseRealm#PORTAL_DZ} with its axis along X.
     *
     * <p>Filled with {@link HorsePortalManager}'s flag 18 for the same reason it
     * uses it: bit 16 suppresses the neighbour shape update, and without it
     * {@link HayPortalBlock#updateShape} eats each portal block as it is placed
     * because the rest of the sheet is still air.
     */
    private static void portal(ServerLevel realm, ChunkPos at) {
        BlockPos anchor = HorseRealm.portalAnchor(at);
        int x0 = anchor.getX() + HorseRealm.PORTAL_DX;
        int z = anchor.getZ() + HorseRealm.PORTAL_DZ;
        int y0 = HorseRealm.GROUND_Y;
        int w = HorseRealm.PORTAL_INNER_W + 2;
        int h = HorseRealm.PORTAL_INNER_H + 2;

        BlockState centre = realm.getBlockState(new BlockPos(x0 + 1, y0 + 1, z));
        if (centre.is(ModBlocks.HAY_PORTAL.get())) {
            return;     // already standing
        }
        BlockState hay = HorsePortalManager.frameBlock();
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                boolean edge = dx == 0 || dx == w - 1 || dy == 0 || dy == h - 1;
                if (edge) {
                    realm.setBlock(new BlockPos(x0 + dx, y0 + dy, z), hay, 2);
                }
            }
        }
        BlockState portal = ModBlocks.HAY_PORTAL.get().defaultBlockState()
                .setValue(HayPortalBlock.AXIS, Direction.Axis.X);
        for (int dx = 1; dx < w - 1; dx++) {
            for (int dy = 1; dy < h - 1; dy++) {
                realm.setBlock(new BlockPos(x0 + dx, y0 + dy, z), portal, 18);
            }
        }
    }

    /**
     * Put a player's own Overworld portal back where they left it, if the space
     * is still free. Same frame as the realm's own, laid on the ground at the
     * remembered position; false if anything solid is in the way, in which case
     * {@code HorseRealm} drops them on the surface instead of carving somebody's
     * house out to make room.
     */
    static boolean rebuildReturnPortal(ServerLevel level, BlockPos remembered) {
        int w = HorseRealm.PORTAL_INNER_W + 2;
        int h = HorseRealm.PORTAL_INNER_H + 2;
        // The remembered position is one block above the portal block that was
        // stood in, so the frame's bottom-left sits one west and two down of it.
        int x0 = remembered.getX() - 1;
        int y0 = remembered.getY() - 2;
        int z = remembered.getZ();
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                BlockState there = level.getBlockState(new BlockPos(x0 + dx, y0 + dy, z));
                if (!there.isAir() && !there.canBeReplaced() && !HorsePortalManager.isFrame(there)) {
                    return false;
                }
            }
        }
        BlockState hay = HorsePortalManager.frameBlock();
        BlockState portal = ModBlocks.HAY_PORTAL.get().defaultBlockState()
                .setValue(HayPortalBlock.AXIS, Direction.Axis.X);
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                boolean edge = dx == 0 || dx == w - 1 || dy == 0 || dy == h - 1;
                if (edge) {
                    level.setBlock(new BlockPos(x0 + dx, y0 + dy, z), hay, 2);
                }
            }
        }
        for (int dx = 1; dx < w - 1; dx++) {
            for (int dy = 1; dy < h - 1; dy++) {
                level.setBlock(new BlockPos(x0 + dx, y0 + dy, z), portal, 18);
            }
        }
        return true;
    }

    // --- the edge of the world ---

    /**
     * The four walls, one block outside the field, {@value HorseRealm#WALL_HEIGHT}
     * blocks tall. {@code minecraft:barrier} is literally the invisible barrier
     * the treatment asks for: no model, no collision box you can see, and
     * unbreakable in survival. It is the honest half of the perimeter - you walk
     * into it and stop - and {@link HorseRealmRules} covers the dishonest half,
     * anything that gets above {@value HorseRealm#WALL_HEIGHT} blocks.
     */
    private static void perimeter(ServerLevel realm, ChunkPos at, int radius) {
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        int minX = at.getMinBlockX();
        int minZ = at.getMinBlockZ();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = minX + dx;
                int z = minZ + dz;
                if (!HorseRealm.isWall(x, z, radius)) {
                    continue;
                }
                for (int y = HorseRealm.GROUND_Y; y < HorseRealm.GROUND_Y + HorseRealm.WALL_HEIGHT; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!realm.getBlockState(p).is(Blocks.BARRIER)) {
                        realm.setBlock(p, barrier, 2);
                    }
                }
            }
        }
    }

    /**
     * <b>Take down a wall the field has outgrown.</b> When the realm grows, the
     * ring it used to stop at is still standing - <i>inside</i> the field now,
     * which is an invisible fence across the middle of somebody's paddock. This
     * clears the columns that were wall at {@code oldRadius} and are not wall
     * now.
     *
     * <p>Only barriers are removed, and only in the wall's own sixteen-block
     * band, so this cannot eat anything else that happens to be standing there.
     */
    private static void clearRing(ServerLevel realm, ChunkPos at, int oldRadius, int radius) {
        int minX = at.getMinBlockX();
        int minZ = at.getMinBlockZ();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = minX + dx;
                int z = minZ + dz;
                if (!HorseRealm.isWall(x, z, oldRadius) || HorseRealm.isWall(x, z, radius)) {
                    continue;
                }
                for (int y = HorseRealm.GROUND_Y; y < HorseRealm.GROUND_Y + HorseRealm.WALL_HEIGHT; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (realm.getBlockState(p).is(Blocks.BARRIER)) {
                        realm.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /**
     * <b>Erase what the old square field left here.</b> The realm used to be a
     * {@value HorseRealm#OLD_CHUNKS}-chunk square with its corner at the origin,
     * carrying a hundred exits, a pool every five chunks and a wall right round
     * it. Almost all of that is now outside the circle and unreachable, and the
     * owner's call was to clean it up rather than leave it lying in the dark.
     *
     * <p><b>The part that is not merely tidiness</b> is the old wall. Two of its
     * four sides ran along {@code x = -1} and {@code z = -1}, which are
     * <i>inside</i> the new circle - a pair of invisible walls straight through
     * the middle of the new field, a few steps from the portal. That is the one
     * piece of this that a player would actually walk into.
     */
    private static void eraseOldField(ServerLevel realm, ChunkPos at, int radius) {
        int minX = at.getMinBlockX();
        int minZ = at.getMinBlockZ();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = minX + dx;
                int z = minZ + dz;
                if (!onOldPerimeter(x, z) || HorseRealm.isWall(x, z, radius)) {
                    continue;
                }
                for (int y = HorseRealm.GROUND_Y; y < HorseRealm.GROUND_Y + HorseRealm.WALL_HEIGHT; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (realm.getBlockState(p).is(Blocks.BARRIER)) {
                        realm.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // An old exit, if this chunk carried one. The frame and its portal sheet
        // both go; the ground under them is left, since it is ordinary field.
        if (isOldPortalChunk(at.x(), at.z()) && !HorseRealm.isPortalChunk(at.x(), at.z())) {
            int x0 = at.getMinBlockX() + HorseRealm.PORTAL_DX;
            int z = at.getMinBlockZ() + HorseRealm.PORTAL_DZ;
            int w = HorseRealm.PORTAL_INNER_W + 2;
            int h = HorseRealm.PORTAL_INNER_H + 2;
            for (int ddx = 0; ddx < w; ddx++) {
                for (int ddy = 0; ddy < h; ddy++) {
                    BlockPos p = new BlockPos(x0 + ddx, HorseRealm.GROUND_Y + ddy, z);
                    BlockState there = realm.getBlockState(p);
                    if (there.is(ModBlocks.HAY_PORTAL.get()) || HorsePortalManager.isFrame(there)
                            || there.is(Blocks.HAY_BLOCK)) {
                        realm.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** The old square field's wall: the ring one block outside {@code [0, OLD_SIZE]}. */
    private static boolean onOldPerimeter(int x, int z) {
        boolean xEdge = x == -1 || x == HorseRealm.OLD_SIZE;
        boolean zEdge = z == -1 || z == HorseRealm.OLD_SIZE;
        boolean xIn = x >= -1 && x <= HorseRealm.OLD_SIZE;
        boolean zIn = z >= -1 && z <= HorseRealm.OLD_SIZE;
        return (xEdge && zIn) || (zEdge && xIn);
    }

    /** One of the old hundred, on the 100-chunk grid inside the old square. */
    private static boolean isOldPortalChunk(int cx, int cz) {
        return cx >= 0 && cz >= 0 && cx < HorseRealm.OLD_CHUNKS && cz < HorseRealm.OLD_CHUNKS
                && cx % 100 == 0 && cz % 100 == 0;
    }

    /** Does this chunk hold any column of the wall at {@code radius}? */
    private static boolean touchesRing(int cx, int cz, int radius) {
        // Nearest and furthest corner of the chunk from the origin. If the ring's
        // radius falls between them (with a block of slack either side for the
        // eight-neighbour band), some column in here is wall.
        int x0 = cx * 16;
        int z0 = cz * 16;
        int x1 = x0 + 15;
        int z1 = z0 + 15;
        double nearX = x0 > 0 ? x0 : (x1 < 0 ? -x1 : 0);
        double nearZ = z0 > 0 ? z0 : (z1 < 0 ? -z1 : 0);
        double farX = Math.max(Math.abs((double) x0), Math.abs((double) x1));
        double farZ = Math.max(Math.abs((double) z0), Math.abs((double) z1));
        double near = Math.sqrt(nearX * nearX + nearZ * nearZ);
        double far = Math.sqrt(farX * farX + farZ * farZ);
        return near <= radius + 2 && far >= radius - 2;
    }

    /** Does this chunk hold anything the old square field left behind? */
    private static boolean touchesOldField(int cx, int cz) {
        int last = HorseRealm.OLD_CHUNKS;
        boolean onWall = (cx == -1 || cx == last || cz == -1 || cz == last)
                && cx >= -1 && cx <= last && cz >= -1 && cz <= last;
        return onWall || isOldPortalChunk(cx, cz);
    }

    private HorseRealmTerrain() {
    }
}
