package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.NeoRng;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <b>Dev-only: the client render bench's stage</b> (#205). Driven by
 * {@code client/DebugRenderBenchClient} when {@code -Dhorsegenetics.renderBench=<n>}
 * (gradle {@code -PrenderBench=<n>}), and inert otherwise - nothing else calls it.
 *
 * <p>{@code n} founders, AI off, stand in rows on a platform high in the sky, and the
 * player hovers behind and above them looking down at the whole herd. Every horse is
 * seeded from its index and its breed is the index's turn through {@link Breeds#all()},
 * so two runs on two commits draw the same herd: that is what makes a before and an
 * after comparable. Every horse is inside the default coat detail distance (32 blocks),
 * so each one has its own coat and pays the full per-frame cost.
 */
public final class DebugRenderBench {

    /** Stage floor height. High enough to be over every terrain and under every ceiling. */
    private static final int STAGE_Y = 200;
    private static final int PER_ROW = 10;
    private static final double SPACING_X = 2.2;
    private static final double SPACING_Z = 3.0;

    private static final List<Horse> HORSES = new ArrayList<>();

    private DebugRenderBench() {
    }

    /** Build the platform, spawn the herd, and put the player behind it looking on. */
    public static void stage(ServerPlayer player, int count) {
        ServerLevel level = player.level();
        int cx = player.blockPosition().getX();
        int cz = player.blockPosition().getZ();
        int rows = (count + PER_ROW - 1) / PER_ROW;
        int halfX = (int) Math.ceil(PER_ROW * SPACING_X / 2) + 2;
        int depth = (int) Math.ceil(rows * SPACING_Z) + 2;
        BlockState floor = Blocks.WHITE_CONCRETE.defaultBlockState();
        for (int x = -halfX; x <= halfX; x++) {
            for (int z = -10; z <= depth; z++) {
                level.setBlock(new BlockPos(cx + x, STAGE_Y - 1, cz + z), floor, 2 | 16);
            }
        }
        var commands = level.getServer().getCommands();
        var quiet = level.getServer().createCommandSourceStack().withSuppressedOutput();
        commands.performPrefixedCommand(quiet, "time set noon");
        commands.performPrefixedCommand(quiet, "weather clear");

        List<Breed> breeds = Breeds.all();
        for (int i = 0; i < count; i++) {
            Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.COMMAND);
            if (horse == null) {
                continue;
            }
            double x = cx + 0.5 + (i % PER_ROW - (PER_ROW - 1) / 2.0) * SPACING_X;
            double z = cz + 0.5 + (i / PER_ROW) * SPACING_Z;
            horse.setPos(x, STAGE_Y, z);
            horse.setNoAi(true);
            horse.setYRot(90f);
            horse.setYBodyRot(90f);
            horse.setYHeadRot(90f);
            NeoRng rng = new NeoRng(RandomSource.create(20_51_205L + i));
            HorseRecords.apply(horse, HorseRecords.newFounder(horse, rng, breeds.get(i % breeds.size())));
            level.addFreshEntity(horse);
            HORSES.add(horse);
        }

        // Behind the first row and above it, pitched down onto the middle of the herd.
        double ex = cx + 0.5;
        double ey = STAGE_Y + 6.0;
        double ez = cz + 0.5 - 8.0;
        double tz = cz + 0.5 + rows * SPACING_Z / 2;
        float pitch = (float) Math.toDegrees(Math.atan2(ey - (STAGE_Y + 1.0), tz - ez));
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(level, ex, ey - 1.62, ez, Set.of(), 0f, pitch, false);
        HorseGenetics.LOGGER.info("[bench] staged {} horses", HORSES.size());
    }

    /** Clear the herd away. */
    public static void finish() {
        for (Horse horse : HORSES) {
            horse.discard();
        }
        HORSES.clear();
    }
}
