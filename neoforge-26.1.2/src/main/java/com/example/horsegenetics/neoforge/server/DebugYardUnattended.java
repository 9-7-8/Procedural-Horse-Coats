package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>The yard's pen helpers, and no pens</b> (2026-09-30). Rows X and Y were every open test that needed
 * nobody at the keyboard (owner, 2026-09-15); by the end of 2026-09-30 each had answered for itself and gone -
 * NIGHT SHY, SUNTOUCHED, REACH WALL, REACH FENCE, STATS and the kick pens before them - under the owner's rule
 * that an automatic yard PASS closes a check. What stays is what other classes build with: {@link #pen},
 * {@link #horse} (the clockwork hands stock through it) and {@link #animal}. The verdicts that closed them are
 * in git ({@code d853359f}, {@code 5f3988c1}); {@code DebugWorldWatch.fleesBy} and {@code kicksBy} stay for the
 * next pen that needs them.
 */
final class DebugYardUnattended {

    private DebugYardUnattended() {
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    static void pen(ServerLevel level, int gy, int x0, int z0, int width, int depth, String name,
                            BlockState floor, List<String> sign, net.minecraft.world.level.block.Block... watched) {
        int x1 = x0 + width;
        int z1 = z0 + depth;
        for (int x = x0 + 1; x < x1; x++) {
            for (int z = z0 + 1; z < z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, floor);
            }
        }
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        YardPens.register(gy, x0, x1, z0, z1, name);
        DebugWorldWatch.watch(name, DebugTestYard.box(x0, gy, z0, x1, gy + 3, z1), null, watched);
    }

    static @Nullable Horse horse(ServerLevel level, int gy, double x, double z, Sex sex, String code,
                                         boolean tamed, String label) {
        Horse h = DebugPenManager.spawnHorse(level, gy + 1, x, z, sex, code, tamed);
        DebugTestYard.label(h, label);
        return h;
    }

    static @Nullable Entity animal(ServerLevel level, EntityType<?> type, int gy, double x, double z) {
        Entity e = type.create(level, EntitySpawnReason.COMMAND);
        if (e == null) {
            return null;
        }
        e.snapTo(x, gy + 1, z, 0.0F, 0.0F);
        if (e instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(e);
        return e;
    }

}
