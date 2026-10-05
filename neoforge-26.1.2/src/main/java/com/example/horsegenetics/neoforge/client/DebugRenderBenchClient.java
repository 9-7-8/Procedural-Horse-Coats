package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.server.DebugRenderBench;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Arrays;

/**
 * <b>Dev-only: what a herd on screen costs per frame</b> (#205). Off unless the client
 * was started with {@code -Dhorsegenetics.renderBench=<n>}
 * ({@code gradlew :neoforge-26.1.2:runClientTestWorld -PrenderBench=50}).
 *
 * <p>Stages {@code n} horses in view ({@link DebugRenderBench}), waits for their coats to
 * bake, then keeps every {@code extractRenderState} time for {@link #MEASURE} ticks and
 * logs one line: {@code [bench] <n> horses ...} with the mean, p50, p95 and slowest
 * extraction and the FPS over the window. Bakes are excluded from every figure, and the
 * line says if one happened inside the window anyway. Run it on two commits, with the
 * window focused (a hidden client pauses itself), and compare the two lines.
 *
 * <p>Singleplayer only: it reaches the integrated server directly, like the photo shoot.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class DebugRenderBenchClient {

    static final String PROPERTY = "horsegenetics.renderBench";

    /** Ticks after the player appears before anything happens - the test world's own setup first. */
    private static final int START_DELAY = 400;
    /** Ticks for the herd to sync and every coat to bake under the budget. */
    private static final int SETTLE = 600;
    /** Ticks measured. */
    private static final int MEASURE = 600;

    private static int ticks;
    private static int phase;
    private static int wait;
    private static int count;
    private static long bakedAtStart;
    private static long fpsTotal;
    private static int fpsMin = Integer.MAX_VALUE;
    private static int fpsSamples;

    private DebugRenderBenchClient() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (phase > 2) {
            return;
        }
        String spec = System.getProperty(PROPERTY);
        if (spec == null || spec.isBlank()) {
            phase = 3;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null || mc.level == null) {
            return;
        }
        if (++ticks < START_DELAY) {
            return;
        }
        if (wait > 0) {
            wait--;
            if (phase == 2) {
                int fps = mc.getFps();
                fpsTotal += fps;
                fpsMin = Math.min(fpsMin, fps);
                fpsSamples++;
            }
            return;
        }
        java.util.UUID id = mc.player.getUUID();
        switch (phase) {
            case 0 -> {
                count = Integer.parseInt(spec.trim());
                mc.options.hideGui = true;
                server.execute(() -> {
                    ServerPlayer sp = server.getPlayerList().getPlayer(id);
                    if (sp != null) {
                        DebugRenderBench.stage(sp, count);
                    }
                });
                phase = 1;
                wait = SETTLE;
            }
            case 1 -> {
                bakedAtStart = GeneticCoatTextureFactory.bakeNanos();
                GeneticCoatTextureFactory.sampleExtracts(2_000_000);
                phase = 2;
                wait = MEASURE;
            }
            default -> {
                report();
                mc.options.hideGui = false;
                server.execute(DebugRenderBench::finish);
                phase = 3;
            }
        }
    }

    private static void report() {
        long[] samples = GeneticCoatTextureFactory.takeExtractSamples();
        long bakedInWindow = GeneticCoatTextureFactory.bakeNanos() - bakedAtStart;
        if (samples.length == 0) {
            HorseGenetics.LOGGER.warn("[bench] {} horses: no extraction measured - was the window hidden?", count);
            return;
        }
        Arrays.sort(samples);
        long total = 0L;
        for (long s : samples) {
            total += s;
        }
        HorseGenetics.LOGGER.info(
                "[bench] {} horses, {} ticks: {} extractions ({} per horse), mean {} us, p50 {} us, "
                        + "p95 {} us, slowest {} us, {} ms in total; FPS mean {} min {}; bakes in window {} ms",
                count, MEASURE, samples.length, samples.length / Math.max(1, count),
                us(total / samples.length), us(samples[samples.length / 2]),
                us(samples[(int) (samples.length * 0.95)]), us(samples[samples.length - 1]),
                total / 1_000_000L,
                fpsSamples == 0 ? 0 : fpsTotal / fpsSamples, fpsMin == Integer.MAX_VALUE ? 0 : fpsMin,
                bakedInWindow / 1_000_000L);
    }

    private static String us(long nanos) {
        return String.format("%.1f", nanos / 1_000.0);
    }
}
