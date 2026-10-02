package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>Row AW east: WALL PAIR and FENCE PAIR - the cover's "side by side, nothing between" fallback</b> (2026-10-02).
 *
 * <p>The first night's ratio pens bred for an hour and then stood refused for two and a half - "not covered: no
 * walkable path to ... (1.6 blocks)" - with the pair grazing against the pen's east wall: the pathfinder plans a
 * horse as a 2x2 mob, so beside a wall its own start node does not fit. {@code NaturalBreedingHandler.canMeet} now
 * falls back to hitboxes within a block of each other and a line between them that crosses no collision shape.
 *
 * <p><b>WALL PAIR</b> asks the case that failed: a mare in heat pinned against a pen wall with her stallion 1.6 blocks
 * along it. PASS: she conceives within {@value #DEADLINE_TICKS} ticks. <b>FENCE PAIR</b> asks that the fallback did not
 * open a fence (the owner's 2026-09-30 call: a fence blocks a cover like a wall): a mare and a stallion pressed to either
 * side of an oak fence, both halves registered as ONE YardPens group so only the fence can part them. PASS: never
 * pregnant in the same time, while WALL PAIR's mare did conceive (else INCONCLUSIVE - nothing was breeding at all).
 * Both are pinned to their marks every 10 ticks (snapTo plus navigation stop) and kept in heat. About 20 minutes.
 */
final class DebugYardReach {

    private DebugYardReach() {
    }

    private static final String WALL = "WALL PAIR - a mare in heat pinned against a pen wall, her stallion 1.6 blocks along it, conceives";
    private static final String FENCE = "FENCE PAIR - a mare and a stallion pressed to either side of a fence never conceive";
    private static final long DEADLINE_TICKS = 24_000L;
    private static int run;

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int thisRun = ++run;
        try {
            DebugYardClockwork.expect(WALL);
            DebugYardClockwork.expect(FENCE);

            // WALL PAIR: a 6x5 brick pen; both horses against its east wall.
            int wx0 = x0 + 25;
            int wx1 = wx0 + 7;
            DebugTestYard.fencedPlot(level, gy, wx0, wx1, z0, z0 + 6);
            DebugPenManager.placeSign(level, new BlockPos(wx0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                    List.of("WALL PAIR", "against the wall,", "1.6 apart: must", "still conceive"));
            YardPens.register(gy, wx0, wx1, z0, z0 + 6, "AW WALL PAIR");
            double wallX = wx1 - 0.72;   // a horse's half-width off the wall's face
            Horse wm = DebugYardUnattended.horse(level, gy, wallX, z0 + 2.2, Sex.FEMALE, "", true, "WALL PAIR MARE");
            Horse ws = DebugYardUnattended.horse(level, gy, wallX, z0 + 3.8, Sex.MALE, "", true, "WALL PAIR STUD");

            // FENCE PAIR: one group, split down the middle by an oak fence.
            int fx0 = x0 + 34;
            int fx1 = x0 + 44;
            DebugTestYard.fencedPlot(level, gy, fx0, fx1, z0, z0 + 6);
            DebugPenManager.placeSign(level, new BlockPos(fx0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                    List.of("FENCE PAIR", "a fence between:", "must never", "conceive"));
            int fenceX = (fx0 + fx1) / 2;
            BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
            for (int z = z0 + 1; z < z0 + 6; z++) {
                // UNVERIFIED in this repo beyond DebugYardStud: an oak fence placed with setBlockAndUpdate.
                level.setBlockAndUpdate(new BlockPos(fenceX, gy + 1, z), fence);
            }
            YardPens.register(gy, fx0, fx1, z0, z0 + 6, "AW FENCE PAIR");
            // Flush against the post on either side (a fence post's collision is 0.375..0.625 of its block, a
            // horse's half-width 0.70): their hitboxes then sit 0.25 apart, well inside the fallback's one block, so
            // only the line test can keep them apart - which is the thing being tested.
            Horse fm = DebugYardUnattended.horse(level, gy, fenceX + 0.375 - 0.70, z0 + 3.0, Sex.FEMALE, "", true,
                    "FENCE PAIR MARE");
            Horse fs = DebugYardUnattended.horse(level, gy, fenceX + 0.625 + 0.70, z0 + 3.0, Sex.MALE, "", true,
                    "FENCE PAIR STUD");

            // Pinned to the marks they were asked for, not where spawnHorse's placeClear may have nudged them.
            double fy = gy + 1;
            Pin[] pins = {new Pin(wm, wallX, fy, z0 + 2.2), new Pin(ws, wallX, fy, z0 + 3.8),
                    new Pin(fm, fenceX + 0.375 - 0.70, fy, z0 + 3.0), new Pin(fs, fenceX + 0.625 + 0.70, fy, z0 + 3.0)};
            long start = level.getGameTime();
            tick(level, thisRun, start, pins, wm, fm);
            HorseGenetics.LOGGER.info("[Debug] test yard: row AW east built (WALL PAIR, FENCE PAIR)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: WALL PAIR / FENCE PAIR failed to build", e);
        }
    }

    private record Pin(@Nullable Horse horse, double x, double y, double z) {
    }

    private static void tick(ServerLevel level, int thisRun, long start, Pin[] pins, @Nullable Horse wallMare,
                             @Nullable Horse fenceMare) {
        DebugYardHerd.after(level, 10, () -> {
            if (thisRun != run) {
                return;
            }
            try {
                for (Pin p : pins) {
                    if (p.horse() == null || !p.horse().isAlive()) {
                        DebugYardClockwork.inconclusive(WALL, "a horse is missing");
                        DebugYardClockwork.inconclusive(FENCE, "a horse is missing");
                        return;
                    }
                    p.horse().getNavigation().stop();
                    p.horse().snapTo(p.x(), p.y(), p.z(), p.horse().getYRot(), 0.0F);
                }
                long t = level.getGameTime() - start;
                boolean wallPregnant = ReproHandler.of(wallMare).pregnant();
                boolean fencePregnant = ReproHandler.of(fenceMare).pregnant();
                if (fencePregnant) {
                    DebugYardClockwork.verdict(FENCE, false, "the fence mare conceived at " + t + " ticks - the"
                            + " fallback opened a fence");
                    if (wallPregnant) {
                        DebugYardClockwork.verdict(WALL, true, "conceived (by " + t + " ticks)");
                    }
                    return;
                }
                if (wallPregnant && t >= DEADLINE_TICKS) {
                    DebugYardClockwork.verdict(WALL, true, "conceived within " + DEADLINE_TICKS + " ticks");
                    DebugYardClockwork.verdict(FENCE, true, "no conception in " + t + " ticks with the fence between,"
                            + " while the wall mare did conceive");
                    return;
                }
                if (t >= DEADLINE_TICKS) {
                    DebugYardClockwork.verdict(WALL, false, "never conceived in " + t + " ticks against the wall");
                    DebugYardClockwork.inconclusive(FENCE, "the control (WALL PAIR) never conceived either");
                    return;
                }
                if (!wallPregnant) {
                    DebugYardFertility.inHeat(wallMare);
                }
                DebugYardFertility.inHeat(fenceMare);
                tick(level, thisRun, start, pins, wallMare, fenceMare);
            } catch (RuntimeException e) {
                DebugYardClockwork.inconclusive(WALL, "a step threw " + e);
                DebugYardClockwork.inconclusive(FENCE, "a step threw " + e);
            }
        });
    }
}
