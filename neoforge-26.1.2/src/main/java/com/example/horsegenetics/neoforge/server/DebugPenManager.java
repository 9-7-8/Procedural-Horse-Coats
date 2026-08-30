package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Everything about how a pen looks and gets placed lives here. This is
 * debug-tool code, not a feature meant to survive contact with real
 * gameplay balance - it's meant to make eyeballing coat-genetics output
 * across many horses fast.
 *
 * Not thread-safe beyond what a single-threaded server tick already
 * guarantees - all calls are expected to happen on the server thread.
 */
public final class DebugPenManager {

    public static final ResourceKey<Level> DEBUG_LEVEL = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "debug_pens"));

    private static final int PEN_SIZE = 20;
    private static final int WALKWAY_WIDTH = 10;
    private static final int PERIOD = PEN_SIZE + WALKWAY_WIDTH; // 30
    private static final int LOOKAHEAD_PENS = 3;

    // Matches the flat generator layers: bedrock(1) + dirt(2) + grass(1) = floor
    // surface at y=3, so the walkable/buildable level is y=4.
    private static final int FLOOR_Y = 4;

    // In-memory only - resets on server restart. Re-entering after a restart
    // will re-run generation for already-built pens, but buildPen() checks
    // for existing horses before spawning more, so you won't get duplicates -
    // you'll just pay a small one-time cost re-placing fence blocks that
    // were already there.
    private static final AtomicInteger highestGeneratedIndex = new AtomicInteger(-1);

    /** Entry point from the network handler: get the player into the dimension and generate around them. */
    public static void teleportAndGenerate(ServerPlayer player) {
        ServerLevel level = ((ServerLevel) player.level()).getServer().getLevel(DEBUG_LEVEL);
        if (level == null) {
            HorseGenetics.LOGGER.error("Debug pens dimension not found - is data/horsegenetics/dimension/debug_pens.json present?");
            return;
        }
        ensureGeneratedUpTo(level, PEN_SIZE / 2); // make sure pen 0 exists before teleporting in
        double centerX = PEN_SIZE / 2.0;
        double centerZ = PEN_SIZE / 2.0;
        player.teleportTo(level, centerX, FLOOR_Y, centerZ, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
    }

    /** Call periodically (e.g. every few player ticks) while a player is in the debug dimension. */
    public static void ensureGeneratedAheadOfPlayer(ServerLevel level, int playerBlockX) {
        int neededIndex = Math.floorDiv(Math.max(playerBlockX, 0), PERIOD) + LOOKAHEAD_PENS;
        ensureGeneratedUpTo(level, neededIndex * PERIOD);
    }

    private static void ensureGeneratedUpTo(ServerLevel level, int blockX) {
        int neededIndex = Math.floorDiv(Math.max(blockX, 0), PERIOD);
        while (highestGeneratedIndex.get() < neededIndex) {
            int next = highestGeneratedIndex.incrementAndGet();
            buildPen(level, next);
        }
    }

    private static void buildPen(ServerLevel level, int index) {
        int x0 = index * PERIOD;
        buildPerimeterFence(level, x0);

        AABB penBounds = new AABB(x0, FLOOR_Y - 1, 0, x0 + PEN_SIZE, FLOOR_Y + 3, PEN_SIZE);
        boolean alreadyOccupied = !level.getEntitiesOfClass(Horse.class, penBounds).isEmpty();
        if (!alreadyOccupied) {
            spawnHorsePair(level, x0);
        }
    }

    private static void buildPerimeterFence(ServerLevel level, int x0) {
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
        BlockState gate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(FenceGateBlock.FACING, Direction.SOUTH);

        int gateX = x0 + PEN_SIZE / 2;

        forEachPerimeterBlock(x0, (pos) -> {
            if (pos.getX() == gateX && pos.getZ() == 0) {
                level.setBlockAndUpdate(pos, gate);
            } else {
                level.setBlockAndUpdate(pos, fence);
            }
        });
    }

    private static void forEachPerimeterBlock(int x0, Consumer<BlockPos> action) {
        for (int x = x0; x < x0 + PEN_SIZE; x++) {
            action.accept(new BlockPos(x, FLOOR_Y, 0));
            action.accept(new BlockPos(x, FLOOR_Y, PEN_SIZE - 1));
        }
        for (int z = 0; z < PEN_SIZE; z++) {
            action.accept(new BlockPos(x0, FLOOR_Y, z));
            action.accept(new BlockPos(x0 + PEN_SIZE - 1, FLOOR_Y, z));
        }
    }

    private static void spawnHorsePair(ServerLevel level, int x0) {
        spawnOneHorse(level, x0 + PEN_SIZE / 3.0, PEN_SIZE / 2.0);
        spawnOneHorse(level, x0 + (2 * PEN_SIZE) / 3.0, PEN_SIZE / 2.0);
    }

    private static void spawnOneHorse(ServerLevel level, double x, double z) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.COMMAND);
        if (horse == null) return;
        horse.setPos(x, FLOOR_Y, z);
        // No genotype/coat assignment here on purpose - HorseGeneticsEventHandler
        // already listens for EntityJoinLevelEvent on any Horse and will assign
        // one automatically the instant addFreshEntity fires below.
        level.addFreshEntity(horse);
    }

    private DebugPenManager() {
    }
}
