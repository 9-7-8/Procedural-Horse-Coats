package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The stall "debug overlay": on the dev keybind (F7, via
 * {@code network/RequestStallHighlightPayload}) and once right after a stall is
 * created, the server draws a <b>particle wireframe</b> of every stall's block
 * span near the player and prints a one-line summary per stall in chat. Kept
 * server-side deliberately - no client render code to break across a
 * render-pipeline change.
 */
public final class StallDebug {

    private static final double RANGE = 96.0;   // only outline stalls this close
    private static final double STEP = 0.4;     // particle spacing along an edge

    private StallDebug() {
    }

    /** Outline + summarise every stall in the player's dimension within {@link #RANGE}. */
    public static void highlight(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<StallRecord> here = StallData.get(server).inDimension(level.dimension());
        Vec3 eye = player.position();
        int shown = 0;
        for (StallRecord stall : here) {
            if (stall.center().distanceToSqr(eye) > RANGE * RANGE) {
                continue;
            }
            outline(level, player, stall.bounds());
            player.sendSystemMessage(Component.literal(summary(stall)));
            shown++;
        }
        if (shown == 0) {
            player.sendSystemMessage(Component.literal(here.isEmpty()
                    ? "[stalls] none defined in this dimension"
                    : "[stalls] " + here.size() + " in this dimension, none within " + (int) RANGE + " blocks"));
        }
    }

    /** Called right after a stall is (re)defined, so the player sees what was captured. */
    public static void showOne(ServerPlayer player, StallRecord stall) {
        if (player.level() instanceof ServerLevel level) {
            outline(level, player, stall.bounds());
            player.sendSystemMessage(Component.literal(summary(stall)));
        }
    }

    private static String summary(StallRecord s) {
        return "[stall] " + (s.horseName().isBlank() ? "?" : s.horseName())
                + " @ " + s.min().getX() + "," + s.min().getY() + "," + s.min().getZ()
                + "  " + (s.max().getX() - s.min().getX() + 1)
                + "x" + (s.max().getY() - s.min().getY() + 1)
                + "x" + (s.max().getZ() - s.min().getZ() + 1)
                + "  (" + s.blockCount() + " blocks)";
    }

    private static void outline(ServerLevel level, ServerPlayer player, AABB box) {
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};
        // 4 edges along each axis
        for (double y : ys) {
            for (double z : zs) {
                edge(level, player, box.minX, y, z, box.maxX, y, z);
            }
        }
        for (double x : xs) {
            for (double z : zs) {
                edge(level, player, x, box.minY, z, x, box.maxY, z);
            }
        }
        for (double x : xs) {
            for (double y : ys) {
                edge(level, player, x, y, box.minZ, x, y, box.maxZ);
            }
        }
    }

    private static void edge(ServerLevel level, ServerPlayer player,
                             double x0, double y0, double z0, double x1, double y1, double z1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) Math.round(len / STEP));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true, true,
                    x0 + dx * t, y0 + dy * t, z0 + dz * t, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
