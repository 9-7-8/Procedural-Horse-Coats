package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.block.HayPortalBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * Client-side pacing for the hay-portal animation ({@link HayPortalRenderer}).
 * There is no server signal for "how close am I to teleporting", so this
 * estimates it: while the local player stands in a {@code hay_portal} block a
 * <b>charge</b> ramps 0 -&gt; 1 over ~{@value #RAMP_SECONDS}s (the player
 * teleport dwell); stepping off, it decays over ~{@value #DECAY_SECONDS}s. The
 * strip then plays at {@value #MIN_FPS} fps at charge 0 up to {@value #MAX_FPS}
 * fps at charge 1, so it visibly speeds up as the teleport nears.
 *
 * <p>Frame position advances by wall-clock time. {@link #currentFrame} is
 * called once per portal block per frame; only the first call each frame does
 * real work (the rest see ~0 dt), so every block on screen shows the same
 * frame and pacing is independent of portal size.
 */
public final class HayPortalClientAnim {

    private static final float MIN_FPS = 12f;
    private static final float MAX_FPS = 48f;
    private static final float RAMP_SECONDS = 10f;
    private static final float DECAY_SECONDS = 2f;
    private static final double MAX_DT = 0.25; // ignore hitches / tab-outs

    private static float charge = 0f;
    private static double framePos = 0.0;
    private static long lastNanos = 0L;

    /** Frame index in {@code [0, frameCount)} for this render frame. */
    public static int currentFrame(int frameCount) {
        long now = System.nanoTime();
        double dt = lastNanos == 0L ? 0.0 : (now - lastNanos) / 1.0e9;
        lastNanos = now;
        if (dt < 0.0 || dt > MAX_DT) {
            dt = 0.0;
        }

        boolean inPortal = localPlayerInPortal();
        charge += (float) (inPortal ? dt / RAMP_SECONDS : -dt / DECAY_SECONDS);
        charge = Math.max(0f, Math.min(1f, charge));

        float fps = MIN_FPS + (MAX_FPS - MIN_FPS) * charge;
        framePos = (framePos + dt * fps) % frameCount;
        if (framePos < 0.0) {
            framePos += frameCount;
        }
        return (int) framePos;
    }

    private static boolean localPlayerInPortal() {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null) {
            return false;
        }
        if (mc.level.getBlockState(p.blockPosition()).getBlock() instanceof HayPortalBlock) {
            return true;
        }
        BlockPos mid = BlockPos.containing(p.getX(), p.getY() + 0.9, p.getZ());
        return mc.level.getBlockState(mid).getBlock() instanceof HayPortalBlock;
    }

    private HayPortalClientAnim() {
    }
}
