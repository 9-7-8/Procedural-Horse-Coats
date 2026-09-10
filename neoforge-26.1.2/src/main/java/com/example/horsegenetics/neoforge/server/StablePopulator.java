package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.stable.StableSpawn;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.data.StableDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * <b>Puts the horses in a generated stable.</b> The building is vanilla's
 * ({@code worldgen/structure/*}); which horses stand in it is
 * {@link StableSpawn}, loaded from {@link StableDefinitions}; finding somewhere
 * for them to stand is this.
 *
 * <h2>Why it runs on a delay after a chunk loads</h2>
 * There is no "a structure finished generating" event. What there is:
 * {@link ChunkEvent.Load}, and a structure's {@link StructureStart}, which knows
 * the piece's whole bounding box. So a stable is <b>queued</b> when the chunk
 * holding its start loads, and populated {@link #SETTLE_TICKS} ticks later -
 * by which time the neighbouring chunks the piece spans have had their own
 * feature stage and the stalls actually exist. Populating on the load itself
 * put horses into a building that was still half air.
 *
 * <p>The delay is also what makes the "is every chunk loaded" check meaningful:
 * a piece 86 blocks across spans six chunks, and a horse spawned into an
 * unloaded one is a horse that never existed.
 *
 * <h2>Once, and remembered</h2>
 * {@link StablePopulationData} is the per-world record of which stables have
 * been filled, keyed by the start's corner. Without it every reload of that
 * chunk would add another seven horses, which is the failure mode this kind of
 * hook is famous for.
 *
 * <h2>Finding the stalls without touching the building</h2>
 * A stall is not a thing a structure NBT can say. Rather than require a modder
 * to bake data markers into somebody else's schematic - which would make
 * "drop in an NBT" untrue - the populator <b>reads the placed blocks</b>:
 *
 * <ul>
 *   <li>a <b>standing spot</b> is a solid floor with two blocks of air on it;</li>
 *   <li>a <b>stall</b> is a standing spot that is <i>enclosed</i> - at least two
 *       of its four sides are a fence, gate, wall, door or solid block - and has
 *       something over its head;</li>
 *   <li>a <b>field</b> spot is a standing spot with open sky.</li>
 * </ul>
 *
 * <p>That is a heuristic and it is meant to be: it costs a modder nothing, it
 * degrades to "put them in the field" rather than to "put them in a wall", and
 * a stable that asks for three stalled horses in a building with two stalls
 * gets two and the third outside. The owner's phrasing for the first stable was
 * "3 in the stalls (if possible)", which is exactly this contract.
 */
@EventBusSubscriber
public final class StablePopulator {

    /** Ticks between a stable's start chunk loading and its horses arriving. */
    private static final int SETTLE_TICKS = 40;

    /** A cap on the scan, so a huge piece cannot cost a tick spike. */
    private static final int MAX_SCAN_SPOTS = 512;

    private record Pending(Identifier structure, BoundingBox box, long dueAt) {
    }

    private static final Deque<Pending> QUEUE = new ArrayDeque<>();

    private StablePopulator() {
    }

    /** Already waiting? Same structure, same corner - the key claim() uses. */
    private static boolean queued(Identifier structure, BoundingBox box) {
        for (Pending p : QUEUE) {
            if (p.structure().equals(structure)
                    && p.box().minX() == box.minX()
                    && p.box().minY() == box.minY()
                    && p.box().minZ() == box.minZ()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Noticing a stable
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || StableDefinitions.all().isEmpty()) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        long now = level.getGameTime();
        for (var entry : chunk.getAllStarts().entrySet()) {
            Structure structure = entry.getKey();
            Identifier id = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
            if (id == null || StableDefinitions.forStructure(id) == null) {
                continue;
            }
            StructureStart start = entry.getValue();
            if (!start.isValid()) {
                continue;
            }
            BoundingBox box = start.getBoundingBox();
            // Deduped by identity, NOT by position. This used to queue a stable
            // only when the chunk being loaded was the one containing the box's
            // minimum corner - which is not the same chunk as the one holding
            // the start. A jigsaw start piece is placed at its chunk's corner
            // and then rotated, and three of the four rotations carry the box
            // into -x or -z, i.e. into the previous chunk. So three stables in
            // four were never queued at all and generated empty, which is
            // exactly how it was reported: the buildings are there, the horses
            // are not. StablePopulationData.claim() is what makes this happen
            // once per world, and it is per (structure, corner), so queueing
            // the same stable more than once is harmless.
            if (!queued(id, box)) {
                QUEUE.add(new Pending(id, box, now + SETTLE_TICKS));
                // Log, not DebugAnnounce: this is once per stable per world and
                // it is the line that answers "the building is there and the
                // horses are not", so it must survive debug.announce being off.
                // Its partner is the "filled ... with N horse(s)" line below;
                // one without the other tells you which half broke.
                HorseGenetics.LOGGER.info("[Stables] queued {} box {},{},{} to {},{},{}",
                        id, box.minX(), box.minY(), box.minZ(),
                        box.maxX(), box.maxY(), box.maxZ());
            }
        }
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (QUEUE.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        int checked = QUEUE.size();
        for (int i = 0; i < checked; i++) {
            Pending pending = QUEUE.poll();
            if (pending == null) {
                return;
            }
            if (pending.dueAt() > now) {
                QUEUE.add(pending);
                continue;
            }
            if (!fullyLoaded(level, pending.box())) {
                // Come back when the rest of the building exists. Re-queued
                // rather than dropped: a player who walks away mid-generation
                // and returns should still find horses.
                QUEUE.add(new Pending(pending.structure(), pending.box(), now + SETTLE_TICKS));
                continue;
            }
            try {
                populate(level, pending.structure(), pending.box());
            } catch (RuntimeException failed) {
                HorseGenetics.LOGGER.warn("[Stables] could not populate {} at {}",
                        pending.structure(), pending.box().getCenter(), failed);
            }
        }
    }

    private static boolean fullyLoaded(ServerLevel level, BoundingBox box) {
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Filling it
    // ------------------------------------------------------------------

    private static void populate(ServerLevel level, Identifier structureId, BoundingBox box) {
        StableSpawn spawn = StableDefinitions.forStructure(structureId);
        if (spawn == null) {
            return;
        }
        StablePopulationData data = StablePopulationData.get(level);
        BlockPos corner = new BlockPos(box.minX(), box.minY(), box.minZ());
        if (!data.claim(structureId, corner)) {
            return; // already filled, once, in this world
        }

        // Seeded off the corner, so the same stable in the same world is the
        // same stable however many times the chunk is reloaded - and so a bug
        // report naming a location can be reproduced.
        RandomSource random = RandomSource.create(
                level.getSeed() ^ corner.asLong() ^ structureId.toString().hashCode());
        NeoRng rng = new NeoRng(random);

        List<BlockPos> stalls = new ArrayList<>();
        List<BlockPos> field = new ArrayList<>();
        scan(level, box, stalls, field);
        Collections.shuffle(stalls, new java.util.Random(random.nextLong()));
        Collections.shuffle(field, new java.util.Random(random.nextLong()));

        int total = spawn.rollCount(rng);
        int wantField = Math.min(spawn.fieldHorses(), total);
        int wantStalls = total - wantField;

        List<BlockPos> places = new ArrayList<>();
        take(places, stalls, wantStalls);
        take(places, field, wantField);
        // Whatever could not be placed where it was asked for goes wherever is
        // left - the "(if possible)" in the spec, made concrete.
        take(places, field, total - places.size());
        take(places, stalls, total - places.size());

        int spawned = 0;
        for (BlockPos pos : places) {
            if (spawnHorse(level, spawn, pos, rng)) {
                spawned++;
            }
        }
        HorseGenetics.LOGGER.info("[Stables] filled {} at {} with {} horse(s) ({} stall, {} field found)",
                structureId, corner, spawned, stalls.size(), field.size());
        if (spawned < total) {
            HorseGenetics.LOGGER.info("[Stables] {} wanted {} and found room for {}",
                    structureId, total, spawned);
        }
    }

    private static void take(List<BlockPos> into, List<BlockPos> from, int count) {
        for (int i = 0; i < count && !from.isEmpty(); i++) {
            into.add(from.remove(from.size() - 1));
        }
    }

    private static boolean spawnHorse(ServerLevel level, StableSpawn spawn, BlockPos pos, NeoRng rng) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.STRUCTURE);
        if (horse == null) {
            return false;
        }
        horse.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                rng.nextFloat() * 360.0F, 0.0F);
        // Persistent on purpose: a horse a player walked half a world to find
        // must not despawn because they left to fetch a saddle.
        horse.setPersistenceRequired();

        Genome genome = spawn.roll(rng);
        HorseRecord record = HorseRecords.newFounder(horse, rng, genome);
        record = record.withBreed(spawn.breed(rng).id());
        HorseRecords.apply(horse, record);
        level.addFreshEntity(horse);
        return true;
    }

    // ------------------------------------------------------------------
    // The scan
    // ------------------------------------------------------------------

    /**
     * Walk the piece's bounding box and sort every place a horse could stand
     * into stalls and field. Capped at {@link #MAX_SCAN_SPOTS} each, because the
     * biggest of these buildings is 86x32x72 and nothing needs five hundred
     * candidate positions to pick seven.
     */
    private static void scan(ServerLevel level, BoundingBox box,
                             List<BlockPos> stalls, List<BlockPos> field) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY() - 2; y++) {
                    pos.set(x, y, z);
                    if (!standable(level, pos)) {
                        continue;
                    }
                    if (enclosed(level, pos) && roofed(level, pos, box)) {
                        if (stalls.size() < MAX_SCAN_SPOTS) {
                            stalls.add(pos.immutable());
                        }
                    } else if (level.canSeeSky(pos) && grassy(level, pos.below())) {
                        if (field.size() < MAX_SCAN_SPOTS) {
                            field.add(pos.immutable());
                        }
                    }
                    // One spot per column: a horse standing on a stall floor and
                    // another on its roof is not two stalls.
                    break;
                }
            }
        }
    }

    /** Solid floor, two blocks of air on it - the shape a horse occupies. */
    private static boolean standable(ServerLevel level, BlockPos pos) {
        BlockState floor = level.getBlockState(pos.below());
        return floor.isSolidRender()
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
    }

    private static boolean grassy(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND);
    }

    /**
     * Two or more of the four sides shut in. Two rather than three because a
     * stall's open side is its gate and the one opposite is often open to the
     * aisle - three would find only boxes, and a box is not a stall.
     */
    private static boolean enclosed(ServerLevel level, BlockPos pos) {
        int walls = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (partition(level.getBlockState(pos.relative(dir)))) {
                walls++;
            }
        }
        return walls >= 2;
    }

    private static boolean partition(BlockState state) {
        Block block = state.getBlock();
        return state.isSolidRender()
                || block instanceof FenceBlock
                || block instanceof FenceGateBlock
                || block instanceof WallBlock
                || block instanceof DoorBlock;
    }

    /** Something overhead inside the piece - a stall is indoors. */
    private static boolean roofed(ServerLevel level, BlockPos pos, BoundingBox box) {
        for (int y = pos.getY() + 2; y <= box.maxY(); y++) {
            if (level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())).isSolidRender()) {
                return true;
            }
        }
        return false;
    }

    /** Only used to keep the heightmap import honest when a spot has no floor. */
    static int surfaceAt(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }
}
