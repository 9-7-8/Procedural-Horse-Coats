package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AL;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AL_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Row AL-west: the arcane dealer</b> (verification &sect;0-FI, entirely unplayed).
 *
 * <p>The cowboy who deals in magic rather than in a breed is about one barn in twelve
 * ({@code CowboyHandler.ARCANE_CHANCE}), which is the right rate for a world and a
 * miserable one for looking at him. This pen is the whole feature standing still: one
 * dealer, founded on the spot, with the string he rolls for himself.
 *
 * <p><b>He is founded by the real code path</b>, not assembled here -
 * {@link CowboyHandler#foundArcaneInPlace} is the ordinary founding routine with the
 * paddock walk switched off, because a fixture that built its own herd would be a second
 * copy of the thing under test and would go on passing after the real one broke. The only
 * thing this class decides is that he is arcane and where he stands.
 *
 * <p>What to read off it: every horse <b>Mixed</b> and <b>entire</b> (a gelding here is a
 * bug), eleven or twelve showing magical genes each, and no two horses sharing an allele
 * pair. Prices should be 12-28 emeralds and should not move between two looks.
 */
final class DebugYardArcane {

    private DebugYardArcane() {
    }

    /**
     * Wide enough to hold the string he places. {@code CowboyHandler.findPlacement}
     * scatters horses within five blocks of him and gives up rather than stacking them,
     * so a cramped pen does not crash - it silently yields a dealer with four horses
     * instead of ten, which reads exactly like the feature being broken.
     */
    private static final int PEN_W = 19;

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int z0 = mouthZ + ROW_AL;
        try {
            DebugYardUnattended.pen(level, gy, west, z0, PEN_W, ROW_AL_D, "ARCANE DEALER",
                    Blocks.GRASS_BLOCK.defaultBlockState(),
                    List.of("ARCANE DEALER", "1 barn in 12.", "11+ magic genes", "each, no repeats"));

            // Dead centre, so his five-block placement radius stays inside the fence.
            Entity spawned = DebugYardUnattended.animal(level, ModEntities.COWBOY.get(), gy,
                    west + PEN_W / 2.0, z0 + ROW_AL_D / 2.0);
            if (!(spawned instanceof Cowboy cowboy)) {
                HorseGenetics.LOGGER.warn("[Debug] test yard: row AL could not place a cowboy");
                return;
            }
            CowboyHandler.foundArcaneInPlace(cowboy, level);

            ActionTrace.log("test yard", "arcane dealer pen built (row AL): " + cowboy.cowboyName()
                    + " with " + cowboy.herdIds().size() + " horses. Expect every one Mixed, entire,"
                    + " and carrying 11-12 showing magical genes with no allele pair used twice");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AL (arcane dealer) failed to build", e);
        }
    }
}
