package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * <b>The horse stress run</b> (owner, 2026-10-02: "stress test performance of the horses and see if there's ways you
 * can make the horses cost fewer ticks"). A measuring instrument, not gameplay: it does nothing unless the JVM is
 * started with {@code -Dhorsegenetics.stress=0,100,300,600} (the {@code stressServer} run sets it).
 *
 * <p>On a dedicated server, once started: force-load a fenced square of the overworld, then for each stage in the
 * list top the horse count up to that number with natural-reason spawns (so founding, herds and every gene system run
 * as they do for a wild horse), let it settle for {@link #WARMUP} ticks, and time {@link #MEASURE} ticks of server
 * work - ServerTickEvent.Pre to .Post, which is the tick's own work and none of the sleep between ticks. One line per
 * stage: {@code [stress] <n> horses: mean, p50, p95, max ms/tick}. After the last stage the server halts, so a JFR
 * recording started with the JVM ({@code dumponexit}) is written. No player is needed: force-loaded chunks tick their
 * entities.
 */
@EventBusSubscriber
final class DebugStress {

    private DebugStress() {
    }

    private static final int WARMUP = 1_200;
    private static final int MEASURE = 1_800;
    /** The square, in chunks either side of the centre; fenced at its edge so nobody walks out of the ticking area. */
    private static final int HALF_CHUNKS = 6;

    private static int[] stages;
    private static int stage = -1;
    private static int tick;
    private static long tickStart;
    private static long[] samples;
    private static int sampled;
    private static ServerLevel level;
    private static BlockPos centre;

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        String spec = System.getProperty("horsegenetics.stress");
        if (spec == null || spec.isBlank()) {
            return;
        }
        stages = Arrays.stream(spec.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
        MinecraftServer server = event.getServer();
        level = server.overworld();
        centre = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(8, 0, 8));
        int cx = centre.getX() >> 4;
        int cz = centre.getZ() >> 4;
        for (int x = cx - HALF_CHUNKS; x < cx + HALF_CHUNKS; x++) {
            for (int z = cz - HALF_CHUNKS; z < cz + HALF_CHUNKS; z++) {
                level.setChunkForced(x, z, true);
            }
        }
        int lo = (cx - HALF_CHUNKS) * 16 + 2;
        int hi = (cx + HALF_CHUNKS) * 16 - 3;
        for (int i = lo; i <= hi; i++) {
            for (BlockPos p : List.of(new BlockPos(i, 0, lo), new BlockPos(i, 0, hi), new BlockPos(lo, 0, i),
                    new BlockPos(hi, 0, i))) {
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
                level.setBlock(top, Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
                level.setBlock(top.above(), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
            }
        }
        HorseGenetics.LOGGER.info("[stress] ON - stages {}, {} chunks forced round {}, {} ticks warm-up and {} measured"
                + " per stage", Arrays.toString(stages), 4 * HALF_CHUNKS * HALF_CHUNKS, centre.toShortString(),
                WARMUP, MEASURE);
        next(server);
    }

    private static void next(MinecraftServer server) {
        stage++;
        if (stage >= stages.length) {
            HorseGenetics.LOGGER.info("[stress] done - halting the server");
            server.halt(false);
            stages = null;
            return;
        }
        int want = stages[stage];
        int have = horses().size();
        RandomSource r = level.getRandom();
        int span = HALF_CHUNKS * 16 - 8;
        for (int i = have; i < want; i++) {
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    centre.offset(r.nextInt(2 * span) - span, 0, r.nextInt(2 * span) - span));
            EntityType.HORSE.spawn(level, at, EntitySpawnReason.NATURAL);
        }
        tick = 0;
        sampled = 0;
        samples = new long[MEASURE];
        HorseGenetics.LOGGER.info("[stress] stage {}: {} horses asked for ({} were alive)", stage, want, have);
    }

    private static List<Horse> horses() {
        int r = HALF_CHUNKS * 16 + 16;
        return level.getEntitiesOfClass(Horse.class, new AABB(centre).inflate(r, 256, r), Horse::isAlive);
    }

    @SubscribeEvent
    static void onPre(ServerTickEvent.Pre event) {
        if (stages != null) {
            tickStart = System.nanoTime();
        }
    }

    @SubscribeEvent
    static void onPost(ServerTickEvent.Post event) {
        if (stages == null) {
            return;
        }
        long took = System.nanoTime() - tickStart;
        tick++;
        if (tick > WARMUP && sampled < MEASURE) {
            samples[sampled++] = took;
        }
        if (sampled == MEASURE) {
            long[] s = Arrays.copyOf(samples, sampled);
            Arrays.sort(s);
            double mean = Arrays.stream(s).average().orElse(0) / 1e6;
            int alive = horses().size();
            int entities = 0;
            for (var ignored : level.getAllEntities()) {
                entities++;
            }
            HorseGenetics.LOGGER.info(String.format(Locale.ROOT,
                    "[stress] %d horses alive (asked %d), %d entities in the overworld: mean %.3f, p50 %.3f, p95 %.3f,"
                            + " max %.3f ms/tick over %d ticks; server average %.3f ms",
                    alive, stages[stage], entities, mean, s[s.length / 2] / 1e6, s[(int) (s.length * 0.95)] / 1e6,
                    s[s.length - 1] / 1e6, s.length, event.getServer().getAverageTickTimeNanos() / 1e6));
            next(event.getServer());
        }
    }
}
