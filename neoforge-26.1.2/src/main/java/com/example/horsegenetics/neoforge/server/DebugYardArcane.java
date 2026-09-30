package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.ArcaneStock;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
            verdict(level, cowboy);
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: row AL (arcane dealer) failed to build", e);
        }
    }

    /**
     * <b>ARCANE DEALER answers for itself</b> (owner, 2026-09-30: "build in more pens that answer for
     * themselves"). Five seconds after founding - his string is placed in the founding call, so this is
     * only margin - every horse in his herd is read off its record: <b>Mixed</b> lineage, <b>not gelded</b>,
     * and at least {@code ArcaneStock.REQUIRED_FAMILIES.size()} showing magical combinations.
     *
     * <p><b>Extras are allowed</b> (owner, 2026-09-30). The first run of this verdict failed two horses of eight
     * for showing thirteen: the dealer forces his eleven or twelve picks, and the rest of the genome is rolled
     * as a feral-mixed founder, which can add a natural magic allele on top. Asked whether the spec or the code
     * was wrong, the owner said the spec - his picks are a floor, not a ceiling. So a combination seen on two
     * horses is logged, not failed: his own picks are distinct by construction ({@code ArcaneStock.rollHorse}
     * writes each into {@code taken}), so any repeat involves an extra. The
     * prices (12-28 emeralds, steady between looks) are the merchant screen's and are not read here.
     */
    private static void verdict(ServerLevel level, Cowboy cowboy) {
        DebugYardHerd.after(level, 100, () -> {
            int want = ArcaneStock.REQUIRED_FAMILIES.size();
            List<String> faults = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            Set<String> repeats = new java.util.LinkedHashSet<>();
            int horses = 0;
            for (UUID id : cowboy.herdIds()) {
                if (!(level.getEntity(id) instanceof Horse h) || !h.isAlive() || !HorseRecords.hasRealRecord(h)) {
                    continue;
                }
                horses++;
                HorseRecord record = HorseRecords.of(h);
                String who = ActionTrace.describeShort(h);
                if (record.lineage().kind() != BreedLineage.Kind.MIXED) {
                    faults.add(who + " is " + record.lineage().kind() + ", not MIXED");
                }
                if (record.gelded()) {
                    faults.add(who + " is a gelding");
                }
                Set<String> tokens = ArcaneStock.showingTokens(record.genome().genotype());
                if (tokens.size() < want) {
                    faults.add(who + " shows " + tokens.size() + " magical combinations, fewer than his " + want);
                }
                for (String token : tokens) {
                    if (!seen.add(token)) {
                        repeats.add(token);
                    }
                }
            }
            String verdict = horses == 0 ? "INCONCLUSIVE (no horses of his were found)"
                    : faults.isEmpty() ? "PASS" : "FAIL: " + String.join("; ", faults);
            ActionTrace.log("test yard", "ARCANE DEALER at 5 s: " + horses + " horse(s), " + seen.size()
                    + " distinct magical combinations" + (repeats.isEmpty() ? "" : ", repeated beyond his picks: " + repeats)
                    + " - " + verdict);
        });
    }
}
