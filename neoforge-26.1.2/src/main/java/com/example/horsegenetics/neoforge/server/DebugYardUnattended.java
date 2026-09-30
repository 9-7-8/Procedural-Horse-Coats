package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.genes.AbstractMagicStatGene;
import com.example.horsegenetics.common.genetics.genes.MagicSpeedGene;
import com.example.horsegenetics.common.genetics.genes.MstnGene;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_X_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Y;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Y_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows X and Y</b> (row AA emptied and went the same day), since 2026-09-30 minus every pen whose question was answered (BONE MEAL, GELDING BAND,
 * LYCAN ROUND TRIP, WERE-COW, WATERBORN; then LYCAN DOOMED and LETHAL FOALS, on their own PASS lines the same
 * day - owner: an automatic yard PASS closes a check "same as clockwork").
 *
 * <p><b>Rows X and Y: open tests that need nobody at the keyboard</b> (owner, 2026-09-15: "put
 * everything which can be tested unattended into the yard"). Each pen logs what to expect beside
 * what happened, so the answer is in {@code latest.log}.
 *
 * <p><b>And each writes its own verdict</b> (owner, 2026-09-30: "Build in more pens that answer for
 * themselves"): one {@code [trace] test yard | <PEN> ... - PASS} line, or {@code - FAIL (...)}, or
 * {@code - INCONCLUSIVE (...)} when the setup rather than the gene went wrong. An automatic PASS closes the
 * pen's wiki check and the pen is then deleted, so a PASS here is only written when the pen's question was
 * actually asked - a pair that never came within reach, or a night that never fell, is INCONCLUSIVE, not a
 * pass by absence. Every clock counts game ticks ({@link DebugYardHerd#after}), so a sprinted run answers
 * sooner and says the same thing.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>X</td><td>NIGHT SHY - a night-shy horse, a band and two cows</td><td>(empty)</td></tr>
 *   <tr><td>Y</td><td>REACH WALL, REACH FENCE - cover reach and courtship</td>
 *       <td>STATS - speed and health floors and stacking</td></tr>
 * </table>
 */
final class DebugYardUnattended {

    private DebugYardUnattended() {
    }

    private static final String PLAIN = "horsegenetics.scn4a=N/N";

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        try {
            nightShy(level, gy, west, mouthZ + ROW_X);
            reachWall(level, gy, west, mouthZ + ROW_Y);
            reachFence(level, gy, west + 9, mouthZ + ROW_Y);
            stats(level, gy, east, mouthZ + ROW_Y);
            // KICK HUNTER and KICK PLAIN went on their own verdicts, 2026-09-30: the hunter's blows 11, 12 and 17
            // ticks apart (gap 222's ten-tick swing), the plain horse none. DebugWorldWatch.kicksBy and
            // DebugTestYard.lidded, which that pen needed against Last Stand's jump, stay for the next fighter.
            // SUNTOUCHED went on 2026-09-30: one light per horse, carried and never left behind (run 11: 8 lamps at
            // build, 10 with its two horses), which is the whole of what the owner cares about - "we've never ...
            // had an issue with them leaving behind light blocks".
            ActionTrace.log("test yard", "unattended pens built (rows X and Y: night shy, reach, stats)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: rows X-AA failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Row X
    // ------------------------------------------------------------------

    /** Gap 241: a night-shy horse flees passive animals after dark, and never its own kind. */
    private static void nightShy(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 18, ROW_X_D, "NIGHT SHY", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("NIGHT SHY", "Fnc/Fnc + a band", "+ two cows: flees", "cows, never horses"));
        Horse shy = horse(level, gy, x0 + 3.5, z0 + 6.5, Sex.FEMALE, "horsegenetics.skittish=Fnc/Fnc", false, "NIGHT SHY");
        horse(level, gy, x0 + 9.5, z0 + 4.5, Sex.MALE, PLAIN, false, "SHY BAND STALLION");
        horse(level, gy, x0 + 10.5, z0 + 7.5, Sex.FEMALE, PLAIN, false, "SHY BAND MARE 1");
        horse(level, gy, x0 + 12.5, z0 + 5.5, Sex.FEMALE, PLAIN, false, "SHY BAND MARE 2");
        animal(level, EntityType.COW, gy, x0 + 15.5, z0 + 3.5);
        animal(level, EntityType.COW, gy, x0 + 15.5, z0 + 8.5);
        // The trace is '[trace] flee | ...' since the night handler's flee moved into GeneAbilityHandler.flee;
        // the old 'night flee' wording is gone from the code, so it is not what to grep for.
        ActionTrace.log("test yard", "NIGHT SHY: expect '[trace] flee | ... \"NIGHT SHY\" from minecraft:cow'"
                + " after dark, and never 'from minecraft:horse' - a flee from a horse is a FAIL (gap 241)");
        if (shy == null) {
            ActionTrace.log("test yard", "NIGHT SHY: the night-shy horse did not spawn - INCONCLUSIVE");
            return;
        }
        nightShyVerdict(level, shy.getUUID(), level.getGameTime(), 0L, 0L);
    }

    /** How often NIGHT SHY looks at the sky and the tally: ten seconds. */
    private static final int NIGHT_POLL = 200;

    /**
     * Five game minutes of dark in one stretch make a night. A yard built at 22:00 sees a tail of dark that
     * ends a minute later, and a tail is not the night the question is about - it waits for the next one.
     */
    private static final long NIGHT_MIN_DARK = 6_000L;

    /** Two game days. Long enough for a whole night whenever the yard was built; past it, no night fell. */
    private static final long NIGHT_DEADLINE = 48_000L;

    /**
     * <b>NIGHT SHY answers for itself</b> (gap 241). Every ten seconds it reads
     * {@link DebugWorldWatch#fleesBy} for <i>this</i> horse's UUID - the log cannot be counted by pen, since
     * a wild horse in the ARCANE DEALER pen also logs {@code flee ... from minecraft:horse} - and the sky,
     * by the same {@code !isBrightOutside()} the gene's own {@code night} condition reads
     * ({@code GeneAbilityHandler.conditionHolds}), so the two cannot disagree about when it is dark.
     * <ul>
     *   <li><b>FAIL</b> the moment she has fled any equine - the whole of gap 241.</li>
     *   <li><b>PASS</b> when a night of at least {@link #NIGHT_MIN_DARK} ends with at least one flee from a
     *       cow and none from a horse.</li>
     *   <li><b>INCONCLUSIVE</b> if she is gone, if that night ends with no cow flee (nothing asked her the
     *       question), or if {@link #NIGHT_DEADLINE} passes with no such night.</li>
     * </ul>
     * Run 10 of 2026-09-30 read 66 flee lines from minecraft:cow and none from minecraft:horse over a night.
     */
    private static void nightShyVerdict(ServerLevel level, UUID id, long builtAt, long darkRun, long darkSeen) {
        DebugYardHerd.after(level, NIGHT_POLL, () -> {
            Map<String, Integer> fled = DebugWorldWatch.fleesBy(id);
            int cows = fled.getOrDefault("minecraft:cow", 0);
            int equines = 0;
            for (Map.Entry<String, Integer> e : fled.entrySet()) {
                if (isEquine(e.getKey())) {
                    equines += e.getValue();
                }
            }
            long elapsed = level.getGameTime() - builtAt;
            String reading = "fled " + (fled.isEmpty() ? "nothing" : fled.toString()) + ", "
                    + darkSeen + " ticks of dark watched over " + elapsed;
            if (equines > 0) {
                ActionTrace.log("test yard", "NIGHT SHY: " + reading + " - FAIL (" + equines
                        + " flee(s) from a horse; a night-shy horse must leave its own kind alone, gap 241)");
                return;
            }
            if (!(level.getEntity(id) instanceof Horse h) || !h.isAlive()) {
                ActionTrace.log("test yard", "NIGHT SHY: " + reading + " - INCONCLUSIVE (the night-shy horse is gone)");
                return;
            }
            boolean dark = !level.isBrightOutside();
            if (!dark && darkRun >= NIGHT_MIN_DARK) {
                ActionTrace.log("test yard", "NIGHT SHY after a night of " + darkRun + " ticks: " + reading
                        + (cows > 0 ? " - PASS"
                        : " - INCONCLUSIVE (a whole night and no flee from a cow - were the cows in the pen?"
                                + " the census's 'other creatures' count says)"));
                return;
            }
            if (elapsed >= NIGHT_DEADLINE) {
                ActionTrace.log("test yard", "NIGHT SHY at " + elapsed + " ticks: " + reading
                        + " - INCONCLUSIVE (no night of " + NIGHT_MIN_DARK + " ticks fell; does this dimension"
                        + " keep a day cycle?)");
                return;
            }
            nightShyVerdict(level, id, builtAt, dark ? darkRun + NIGHT_POLL : 0L,
                    darkSeen + (dark ? NIGHT_POLL : 0L));
        });
    }

    /** Horses, donkeys and mules, and their undead - everything {@code MobGroups}' "passive" leaves out. */
    private static boolean isEquine(String typeId) {
        return typeId.endsWith("horse") || typeId.endsWith("donkey") || typeId.endsWith("mule");
    }

    // ------------------------------------------------------------------
    // Row Y
    // ------------------------------------------------------------------

    /**
     * Gap 230: a stallion cannot cover a mare through a stone wall. <b>One block thick since 2026-09-30</b>: at
     * three, with {@code ReproRules.NATURAL_REACH} also three, the stud was never even in reach (run 11: "0 of 225
     * polls ... the stud within 3 blocks"), so the path check the pen exists for was never asked.
     */
    private static void reachWall(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 8, ROW_Y_D, "REACH WALL", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("REACH: WALL", "mare in heat and a", "stallion either side", "of stone: no cover"));
        for (int x = x0 + 1; x < x0 + 8; x++) {
            for (int z = z0 + 6; z <= z0 + 6; z++) {
                for (int y = gy + 1; y <= gy + 3; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
        Horse mare = horse(level, gy, x0 + 4.5, z0 + 4.5, Sex.FEMALE, PLAIN, true, "RW MARE");
        Horse stud = horse(level, gy, x0 + 4.5, z0 + 7.5, Sex.MALE, PLAIN, true, "RW STUD");
        DebugYardFertility.inHeat(mare);
        // NOTE: YardPens.groupOf takes the FIRST registered pen containing a horse, and pen() above already
        // registered the whole of "REACH WALL" - so these two halves are never what a horse is grouped by, and
        // the pair are "together" to the breeding scan. That is what the pen wants (the wall, not the pen
        // list, must be what stops them), but these two lines do nothing. Left as found.
        YardPens.register(gy, x0, x0 + 8, z0, z0 + 6, "REACH WALL NORTH");
        YardPens.register(gy, x0, x0 + 8, z0 + 6, z0 + ROW_Y_D, "REACH WALL SOUTH");
        ActionTrace.log("test yard", "REACH WALL: expect no 'natural cover' line naming RW MARE, ever (gap 230)");
        reachVerdict(level, "REACH WALL", mare, stud, false);
    }

    /**
     * Gap 230: <b>a fence blocks a cover, like a wall</b> (owner, 2026-09-30). The pen was built to expect a cover
     * across one fence, before {@code NaturalBreedingHandler.canMeet} (2026-09-25) required a walkable path; asked,
     * the owner chose the path rule - so the pair must never be covered, though the stud stands in reach.
     */
    private static void reachFence(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_Y_D, "REACH FENCE", Blocks.GRASS_BLOCK.defaultBlockState(),
                List.of("REACH: FENCE", "one fence between", "them: NO cover,", "like a wall"));
        for (int x = x0 + 1; x < x0 + 9; x++) {
            level.setBlock(new BlockPos(x, gy + 1, z0 + 6), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
        Horse mare = horse(level, gy, x0 + 4.5, z0 + 5.3, Sex.FEMALE, PLAIN, true, "RF MARE");
        Horse stud = horse(level, gy, x0 + 4.5, z0 + 6.7, Sex.MALE, PLAIN, true, "RF STUD");
        DebugYardFertility.inHeat(mare);
        ActionTrace.log("test yard", "REACH FENCE: expect no 'natural cover' line naming RF MARE, ever - a fence"
                + " blocks a cover like a wall (gap 230, owner 2026-09-30)");
        reachVerdict(level, "REACH FENCE", mare, stud, true);
    }

    /** {@code NaturalBreedingHandler.SCAN}: the breeding scan looks every forty ticks, so the tally does too. */
    private static final int REACH_POLL = 40;

    /** Heats to watch before a verdict. Five in the debug clock is about ten game minutes. */
    private static final int REACH_HEATS = 5;

    /** A guard, in case the repro clock is not the debug one: forty game minutes. */
    private static final long REACH_DEADLINE = 48_000L;

    /** What a REACH pen has seen so far. Mutable, and only ever touched on the server thread. */
    private static final class ReachTally {
        final Set<Long> heats = new HashSet<>();
        /** Polls on which she could have been covered: a try left this heat, and the stud in reach. */
        int chances;
        int polls;
    }

    /**
     * <b>REACH WALL and REACH FENCE answer for themselves</b> (gap 230). Every {@link #REACH_POLL} ticks this
     * reads what {@code NaturalBreedingHandler.onTick} reads, in its order: her own repro clock
     * ({@code HorseRealmRepro.reproTime}), {@code ReproRules.mayTryNaturally} - in heat with her one try this
     * heat unspent - and whether the stud is inside her box inflated by {@code ReproRules.NATURAL_REACH},
     * the handler's candidate test. A poll where all three hold is a <i>chance</i>: the only moment the
     * handler would go on to ask {@code canMeet}, which is the walkable-path gate the wall and the fence
     * are actually testing. A cover is her try being spent ({@code Reproduction.lastNaturalTry} moving, which
     * a cover does whether or not it takes), or her being pregnant or foaled.
     *
     * <p><b>Both pens</b>: FAIL the moment she is covered. PASS after {@link #REACH_HEATS} heats uncovered
     * <i>with at least one chance</i>. With none it is INCONCLUSIVE and says why - worked out from the build
     * geometry, the wall was three blocks thick and NATURAL_REACH is three, so a mare pressed to the north
     * face and a stud to the south face never met; the wall is one block now (see {@link #reachWall}).
     * REACH FENCE used to be always INCONCLUSIVE, pending the owner's call on whether a fence should block a
     * cover; it should (2026-09-30), so it is judged the same way.
     */
    private static void reachVerdict(ServerLevel level, String name, @Nullable Horse mare, @Nullable Horse stud,
                                     boolean fence) {
        if (mare == null || stud == null) {
            ActionTrace.log("test yard", name + ": a horse did not spawn - INCONCLUSIVE");
            return;
        }
        reachPoll(level, name, mare.getUUID(), stud.getUUID(), fence, ReproHandler.of(mare).lastNaturalTry(),
                level.getGameTime(), new ReachTally());
    }

    private static void reachPoll(ServerLevel level, String name, UUID mareId, UUID studId, boolean fence,
                                  long triedAtBuild, long builtAt, ReachTally tally) {
        DebugYardHerd.after(level, REACH_POLL, () -> {
            if (!(level.getEntity(mareId) instanceof Horse mare) || !mare.isAlive()
                    || !(level.getEntity(studId) instanceof Horse stud) || !stud.isAlive()) {
                ActionTrace.log("test yard", name + ": the mare or the stud is gone after " + tally.heats.size()
                        + " heat(s) - INCONCLUSIVE");
                return;
            }
            ReproTiming t = ServerConfig.reproTiming();
            long now = HorseRealmRepro.reproTime(mare);
            Reproduction r = ReproHandler.of(mare);
            long heatStart = ReproRules.heatStartAt(r, now, t);
            if (heatStart != Reproduction.NEVER) {
                tally.heats.add(heatStart);
            }
            boolean inReach = mare.getBoundingBox().inflate(ReproRules.NATURAL_REACH).intersects(stud.getBoundingBox());
            if (inReach && ReproRules.mayTryNaturally(r, now, t)) {
                tally.chances++;
            }
            tally.polls++;
            boolean covered = r.lastNaturalTry() != triedAtBuild || r.pregnant() || r.foaledTick() != Reproduction.NEVER;
            long elapsed = level.getGameTime() - builtAt;
            boolean done = covered || tally.heats.size() >= REACH_HEATS || elapsed >= REACH_DEADLINE;
            if (!done) {
                reachPoll(level, name, mareId, studId, fence, triedAtBuild, builtAt, tally);
                return;
            }
            String reading = (covered ? "covered" : "never covered") + " in " + tally.heats.size() + " heat(s) over "
                    + elapsed + " ticks, " + tally.chances + " of " + tally.polls + " polls with a try left and the"
                    + " stud within " + (int) ReproRules.NATURAL_REACH + " blocks"
                    + (r.pregnant() ? ", pregnant" : "");
            String verdict;
            if (covered) {
                verdict = "FAIL (covered through " + (fence ? "a fence" : "a stone wall") + ")";
            } else if (tally.heats.isEmpty()) {
                verdict = "INCONCLUSIVE (she was never in heat)";
            } else if (tally.chances == 0) {
                verdict = "INCONCLUSIVE (never a chance: the stud was never within NATURAL_REACH while she had a"
                        + " try, so the path check was never asked)";
            } else {
                verdict = "PASS";
            }
            ActionTrace.log("test yard", name + ": " + reading + " - " + verdict);
        });
    }

    /** Stat floors and stacking: the slowest speed, Swift with myostatin, identical horses, a frail floor. */
    private static void stats(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_Y_D, "STATS", Blocks.STONE.defaultBlockState(),
                List.of("STATS", "speed floor, Swift x", "MSTN, three equal", "C/T, a frail floor"));
        water(level, gy, x0 + 4, z0 + 4);    // clear of the walls and the horses (gap 247)
        Map<String, UUID> ids = new LinkedHashMap<>();
        keep(ids, horse(level, gy, x0 + 2.5, z0 + 2.5, Sex.FEMALE, "horsegenetics.magic_speed=Sluggish/Sluggish", true, "STAT SLUGGISH"));
        keep(ids, horse(level, gy, x0 + 6.5, z0 + 2.5, Sex.FEMALE, "horsegenetics.magic_speed=Swift/Swift", true, "STAT SWIFT"));
        keep(ids, horse(level, gy, x0 + 2.5, z0 + 5.5, Sex.FEMALE, "horsegenetics.mstn=C/C", true, "STAT MSTN CC"));
        keep(ids, horse(level, gy, x0 + 6.5, z0 + 5.5, Sex.FEMALE,
                "horsegenetics.magic_speed=Swift/Swift-horsegenetics.mstn=C/C", true, "STAT SWIFT+CC"));
        for (int i = 0; i < 3; i++) {
            keep(ids, horse(level, gy, x0 + 2.5 + i * 2, z0 + 8.5, Sex.FEMALE, "horsegenetics.mstn=C/T", true, "STAT MSTN CT " + (i + 1)));
        }
        keep(ids, horse(level, gy, x0 + 7.5, z0 + 10.0, Sex.FEMALE,
                "horsegenetics.magic_health=Frail/Frail-horsegenetics.b4galt7=d/d", true, "STAT FRAIL"));
        DebugYardHerd.after(level, 1_200, () -> statsVerdict(level, ids));
        DebugWorldWatch.watchAttribute("STATS SPEED", DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_Y_D),
                Attributes.MOVEMENT_SPEED);
        DebugWorldWatch.watchAttribute("STATS HEALTH", DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + ROW_Y_D),
                Attributes.MAX_HEALTH);
        ActionTrace.log("test yard", "STATS: read the STATS SPEED census - STAT SLUGGISH at 0.1125 or above, STAT"
                + " SWIFT+CC about STAT SWIFT times STAT MSTN CC over base, the three STAT MSTN CT identical; STATS HEALTH:"
                + " STAT FRAIL at or above the floor and alive");
    }

    private static void keep(Map<String, UUID> ids, @Nullable Horse h) {
        if (h != null && h.getCustomName() != null) {
            ids.put(h.getCustomName().getString(), h.getUUID());
        }
    }

    /** Two doubles computed the same way from the same inputs agree far closer than this. */
    private static final double STAT_EPS = 1.0e-6;

    /**
     * <b>STATS answers for itself</b>, one game minute in, from each horse's movement_speed and max_health
     * <b>base</b> values - the genetic number {@code HorseRecords.applyTraitsToEntity} writes, with no
     * condition or pregnancy modifier on it.
     *
     * <h2>What "SWIFT+CC about SWIFT times MSTN CC over base" has to mean</h2>
     * {@code TraitBuilder.build}: speed = max(floor, (BASE_SPEED + every natural locus's addition) x the
     * magical factor), where MSTN's C is an addition ({@code MstnGene.SPEED_PER_C} a copy) and Swift/Swift's
     * factor is 1 + <i>that horse's own</i> two copy percentages ({@code AbstractMagicStatGene}, a bounded
     * normal about 0.10 each, rolled per founder). So SWIFT = BASE x f1 and SWIFT+CC = (BASE + 2C) x f2, and
     * "SWIFT x CC / BASE" is SWIFT+CC only if f1 = f2 - which two separately founded horses never are. That
     * is why run 10 read SWIFT 0.271 against SWIFT+CC 0.270 (f1 = 1.445, f2 = 1.187) and an earlier run
     * 0.225 against 0.244: nothing failed to stack, the two horses rolled different percentages. Every other
     * locus is the default allele ({@code Genotype.of}), and a default allele adds nothing, so the only free
     * variable is each Swift horse's own factor. The verdict reads that factor off the horse's epigenome
     * ({@code GeneEpigenetics.forGene}, as {@code HorseTraits.resolve} does) and checks the law per horse:
     * SWIFT / f1 is BASE, and SWIFT+CC / f2 is MSTN CC - the multiplier lands on the natural sum, after the
     * addition. The naive ratio is still printed, marked as not a fair comparison.
     *
     * <p>PASS needs every one of: SLUGGISH at or above {@code HorseSpeedFloor.floor()}; the three CT
     * identical and at BASE + C; MSTN CC at BASE + 2C; both Swift horses on their own factor to
     * {@link #STAT_EPS}; FRAIL alive with max health at or above {@code HorseTraits.MIN_HEALTH}. A speed horse
     * missing is INCONCLUSIVE; FRAIL gone is a FAIL, since staying alive is part of its claim.
     */
    private static void statsVerdict(ServerLevel level, Map<String, UUID> ids) {
        Map<String, Horse> horses = new LinkedHashMap<>();
        List<String> missing = new java.util.ArrayList<>();
        for (String label : List.of("STAT SLUGGISH", "STAT SWIFT", "STAT MSTN CC", "STAT SWIFT+CC",
                "STAT MSTN CT 1", "STAT MSTN CT 2", "STAT MSTN CT 3", "STAT FRAIL")) {
            UUID id = ids.get(label);
            if (id != null && level.getEntity(id) instanceof Horse h && h.isAlive()) {
                horses.put(label, h);
            } else {
                missing.add(label);
            }
        }
        missing.remove("STAT FRAIL");
        if (!missing.isEmpty()) {
            ActionTrace.log("test yard", "STATS at 1 min: " + missing + " missing or dead - INCONCLUSIVE");
            return;
        }
        double floor = HorseSpeedFloor.floor();
        double base = HorseTraits.BASE_SPEED;
        double c = MstnGene.SPEED_PER_C;
        double sluggish = speedBase(horses.get("STAT SLUGGISH"));
        double swift = speedBase(horses.get("STAT SWIFT"));
        double cc = speedBase(horses.get("STAT MSTN CC"));
        double swiftCc = speedBase(horses.get("STAT SWIFT+CC"));
        double f1 = swiftFactor(horses.get("STAT SWIFT"));
        double f2 = swiftFactor(horses.get("STAT SWIFT+CC"));
        double[] ct = {speedBase(horses.get("STAT MSTN CT 1")), speedBase(horses.get("STAT MSTN CT 2")),
                speedBase(horses.get("STAT MSTN CT 3"))};
        if (Double.isNaN(f1) || Double.isNaN(f2)) {
            ActionTrace.log("test yard", "STATS at 1 min: a Swift horse is not Swift/Swift on its record, or"
                    + " magic_speed is not registered - INCONCLUSIVE");
            return;
        }
        List<String> fails = new java.util.ArrayList<>();
        if (sluggish < floor - STAT_EPS) {
            fails.add(String.format("SLUGGISH %.4f under the floor %.4f", sluggish, floor));
        }
        double ctWant = Math.max(floor, base + c);
        for (int i = 0; i < ct.length; i++) {
            if (Math.abs(ct[i] - ctWant) > STAT_EPS) {
                fails.add(String.format("MSTN CT %d %.6f, not %.6f", i + 1, ct[i], ctWant));
            }
        }
        double ccWant = Math.max(floor, base + 2 * c);
        if (Math.abs(cc - ccWant) > STAT_EPS) {
            fails.add(String.format("MSTN CC %.6f, not %.6f", cc, ccWant));
        }
        double swiftWant = Math.max(floor, base * f1);
        if (Math.abs(swift - swiftWant) > STAT_EPS) {
            fails.add(String.format("SWIFT %.6f, not base x its factor %.4f = %.6f", swift, f1, swiftWant));
        }
        double swiftCcWant = Math.max(floor, (base + 2 * c) * f2);
        if (Math.abs(swiftCc - swiftCcWant) > STAT_EPS) {
            fails.add(String.format("SWIFT+CC %.6f, not (base + 2C) x its factor %.4f = %.6f - the magic"
                    + " factor is not multiplying the natural sum", swiftCc, f2, swiftCcWant));
        }
        Horse frail = horses.get("STAT FRAIL");
        String frailReading;
        if (frail == null) {
            frailReading = "FRAIL gone";
            fails.add("STAT FRAIL is not alive at 1 min (read the watch's death line for the cause)");
        } else {
            net.minecraft.world.entity.ai.attributes.AttributeInstance hp = frail.getAttribute(Attributes.MAX_HEALTH);
            double max = hp == null ? Double.NaN : hp.getBaseValue();
            frailReading = String.format("FRAIL max health %.2f (floor %.1f), %.1f now", max, HorseTraits.MIN_HEALTH,
                    frail.getHealth());
            if (!(max >= HorseTraits.MIN_HEALTH - STAT_EPS) || !(frail.getHealth() > 0.0F)) {
                fails.add(frailReading + " - under the floor or not alive");
            }
        }
        String reading = String.format("SLUGGISH %.4f (floor %.4f), SWIFT %.4f = %.4f x %.4f, MSTN CC %.4f,"
                        + " SWIFT+CC %.4f = %.4f x %.4f (its own Swift copies), MSTN CT %.4f/%.4f/%.4f, %s;"
                        + " the naive SWIFT x CC / base = %.4f is not a fair expectation (f1 %.4f vs f2 %.4f)",
                sluggish, floor, swift, base, f1, cc, swiftCc, base + 2 * c, f2, ct[0], ct[1], ct[2], frailReading,
                swift * cc / base, f1, f2);
        ActionTrace.log("test yard", "STATS at 1 min: " + reading + " - "
                + (fails.isEmpty() ? "PASS" : "FAIL (" + String.join("; ", fails) + ")"));
    }

    private static double speedBase(Horse h) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance a = h.getAttribute(Attributes.MOVEMENT_SPEED);
        return a == null ? Double.NaN : a.getBaseValue();
    }

    /**
     * This horse's own Swift/Swift multiplier: 1 + both copies' {@code delta}, clamped the way
     * {@code TraitBuilder.magicFactor} clamps it. NaN if the locus is missing or the horse is not Swift/Swift.
     */
    private static double swiftFactor(Horse h) {
        Gene gene = Genes.byKeyOrNull(MagicSpeedGene.KEY);
        if (gene == null) {
            return Double.NaN;
        }
        HorseRecord record = HorseRecords.of(h);
        var pair = record.genotype().pair(gene);
        if (!"Swift".equals(pair.first().token()) || !"Swift".equals(pair.second().token())) {
            return Double.NaN;
        }
        GeneEpigenetics epi = GeneEpigenetics.forGene(gene, record.genotype(), record.epigenome());
        double f = 1.0 + epi.copy(0).get(AbstractMagicStatGene.DELTA) + epi.copy(1).get(AbstractMagicStatGene.DELTA);
        return Math.min(HorseTraits.MAGICAL_MAX_FACTOR, Math.max(HorseTraits.MAGICAL_MIN_FACTOR, f));
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

    private static void water(ServerLevel level, int gy, int x, int z) {
        level.setBlock(new BlockPos(x, gy + 1, z),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 3);
    }

}
