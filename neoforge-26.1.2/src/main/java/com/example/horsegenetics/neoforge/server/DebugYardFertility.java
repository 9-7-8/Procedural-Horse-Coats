package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.Pregnancy;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_O;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_O_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_R;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_R_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Row O of the test yard, and MET NATURAL in R: breeding and fertility, one
 * scenario per pen</b> (2026-09-13, packed 2026-09-14).
 *
 * <p>Every pen is a single claim with its expected outcome on the sign, and every
 * pen registers {@link DebugWorldWatch#watchBreeding}, so the log carries each
 * horse's breeding state - in heat, pregnant, nursing, covers today - whenever it
 * changes and at every census. Every pen is also its own {@link YardPens} pen, so the
 * natural-cover cap and partner search see nothing over the wall.
 *
 * <p><b>Confirmed and deleted, 2026-09-14</b>: NATURAL PAIR, GELDING CONTROL,
 * SUBFERTILE PAIR and COWBOY STOCK, all read off the first quarter hour's log.
 *
 * <p><b>Deleted as hands-only, 2026-09-30</b>: WEANING (lead the foal away - AI's
 * WEANING AWAY does it by distance), the JAR STUD and JAR &amp; KIT BENCH (the seed
 * jar is {@link DebugYardClockwork}'s now), GOLD ANY HEAT (AH's GOLD TIMER) and
 * SUBFERTILE GOLD (retired 2026-09-25, when golden carrots stopped rolling against
 * fertility). THE CAP and MATERNITY lost their chests of leads and vet's kits: the
 * pens log their own reading, and nobody is coming to open a chest.
 *
 * <p><b>A three-block gap, not a shared wall, between breeding pens.</b> Golden-carrot
 * breeding is vanilla's goal, which pairs two horses in love within three blocks
 * regardless of walls, and {@code YardPens} does not reach it. So no two pens share a wall.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>O</td><td>THE CAP, HURT MARE</td><td>MATERNITY</td></tr>
 *   <tr><td>R</td><td>(herd rows)</td><td>MET NATURAL</td></tr>
 * </table>
 *
 * <p>Timings assume {@code debug.tools} is on, the dev default: a reproductive
 * day is one minute. The maternity due dates are absolute ticks and hold either
 * way.
 */
final class DebugYardFertility {

    private DebugYardFertility() {
    }

    private static final String FERT = "horsegenetics.fertility=";
    private static final String MET_CARRIER = "horsegenetics.met=met/N";

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN;
        try {
            theCap(level, gy, west, mouthZ + ROW_O);
            hurtMare(level, gy, west + 14, mouthZ + ROW_O);
            maternity(level, gy, east, mouthZ + ROW_O);
            metNatural(level, gy, east, mouthZ + ROW_R);
            ActionTrace.log("test yard", "fertility pens built (row O, and MET NATURAL in R)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: fertility rows failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Row O
    // ------------------------------------------------------------------

    private static void theCap(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 11, ROW_O_D, "THE CAP",
                List.of("THE CAP", "8 mares + 1 stud,", "all in heat: NOBODY", "is covered"));
        for (int i = 0; i < 8; i++) {
            inHeat(horse(level, gy, x0 + 2.0 + (i % 4) * 2.0, z0 + 2.5 + (i / 4) * 3.0, Sex.FEMALE,
                    FERT + "n/n", true, "CAP MARE " + (i + 1)));
        }
        horse(level, gy, x0 + 9.2, z0 + 6, Sex.MALE, FERT + "n/n", true, "CAP STUD");
    }

    private static void hurtMare(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 5, ROW_O_D, "HURT MARE",
                List.of("HURT MARE", "half health, in", "heat: NOT covered", "until she heals"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 2.5, Sex.FEMALE, FERT + "n/n", true, "HURT MARE");
        horse(level, gy, x0 + 2.5, z0 + 5.5, Sex.MALE, FERT + "n/n", true, "HURT PEN STUD");
        inHeat(mare);
        if (mare == null) {
            return;
        }
        // HALF HEALTH A SECOND LATER, NOT NOW. Set at spawn, it was undone before the
        // first scan: the horse's traits are applied as it joins the level, and a
        // new max-health attribute takes the health with it. First run, 2026-09-14:
        // covered at 0.80 seven seconds after the build - the full-health chance -
        // and five foals by the first quarter hour.
        UUID id = mare.getUUID();
        DebugYardHerd.after(level, 20, () -> {
            if (level.getEntity(id) instanceof Horse h && h.isAlive()) {
                h.setHealth(h.getMaxHealth() / 2.0F);
                ActionTrace.log("test yard", "HURT MARE set to " + h.getHealth() + "/" + h.getMaxHealth());
            }
        });
    }

    private static void maternity(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_O_D, "MATERNITY",
                List.of("MATERNITY", "foals at 1, 2, 3 min", "TWINS mare: two;", "LOSS mare: none"));
        ReproTiming t = ServerConfig.reproTiming();
        long day = t.dayTicks();
        NeoRng rng = new NeoRng(level.getRandom());

        Genome plainSire = Genome.of(Genotype.parse(FERT + "n/n"), rng).withSex(Sex.MALE);
        HorseRecord plainSireRecord = HorseRecord.founder(new UUID(0x51EL, 1L), "Yard", "Sire", plainSire);
        Genome metSire = Genome.of(Genotype.parse(MET_CARRIER), rng).withSex(Sex.MALE);
        HorseRecord metSireRecord = HorseRecord.founder(new UUID(0x51EL, 2L), "Carrier", "Sire", metSire);

        Horse due1 = horse(level, gy, x0 + 2.0, z0 + 2.5, Sex.FEMALE, FERT + "n/n", true, "DUE 1 MIN");
        Horse twins = horse(level, gy, x0 + 4.5, z0 + 4.5, Sex.FEMALE, FERT + "tw/tw", true, "TWINS DUE 2 MIN");
        Horse loss = horse(level, gy, x0 + 7.0, z0 + 6.5, Sex.FEMALE, MET_CARRIER, true, "LOSS AT 1, DUE 3");
        pregnant(due1, plainSire, plainSireRecord, false, false, day);
        pregnant(twins, plainSire, plainSireRecord, true, false, 2 * day);
        pregnant(loss, metSire, metSireRecord, false, true, 3 * day);
        // THE VET'S KIT, READ WITHOUT HANDS (2026-09-15): the kit's examine is chat only, so the pen logs the same
        // report the kit would print, before the first birth. And TWINS' speed early and late, for "15% lower".
        DebugYardHerd.after(level, 200, () -> {
            for (Horse h : new Horse[]{due1, twins, loss}) {
                if (h != null && h.isAlive()) {
                    ActionTrace.log("test yard", "MATERNITY vet: " + (h.getCustomName() == null ? "?" : h.getCustomName().getString())
                            + " - " + String.join(" / ", ReproHandler.vetReport(h)));
                }
            }
            ActionTrace.log("test yard", "MATERNITY vet: expect DUE 1 MIN one foal, TWINS DUE 2 MIN twins, LOSS AT 1, DUE 3"
                    + " one foal");
        });
        // Three readings across the heavy window, not one at its edge. "Late" is the last third (ReproRules.LATE_FRACTION)
        // and the watch line works it out on the spot, but the speed modifier is only put on at the horse's own SCAN
        // tick - the 09:26 run read 1900 ticks five seconds after the line first said "heavy and slow", still at base.
        for (long at : new long[]{200L, 2_000L, 2_150L, 2_300L}) {
            DebugYardHerd.after(level, at, () -> {
                if (twins != null && twins.isAlive()) {
                    ActionTrace.log("test yard", String.format("MATERNITY speed at %d ticks: TWINS %.4f (base %.4f)%s", at,
                            twins.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),
                            twins.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),
                            at > 1_000 ? " - expect about 15% under base late in a twin pregnancy" : ""));
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // Row R east
    // ------------------------------------------------------------------

    /** Deeper than the others: it breeds on its own until it holds nine horses. */
    private static void metNatural(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_R_D, "MET NATURAL",
                List.of("MET CARRIERS", "left to breed:", "count levels off", "at the cap (9)"));
        Horse mare = horse(level, gy, x0 + 3.0, z0 + 5, Sex.FEMALE, MET_CARRIER, true, "MET MARE");
        horse(level, gy, x0 + 6.0, z0 + 5, Sex.MALE, MET_CARRIER, true, "MET STUD");
        inHeat(mare);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Walls, sign, a breeding watch over the inside, and a pen of its own for the checks. */
    private static void pen(ServerLevel level, int gy, int x0, int z0, int width, int depth, String watchName,
                            List<String> sign) {
        int x1 = x0 + width;
        int z1 = z0 + depth;
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        DebugWorldWatch.watchBreeding(watchName, DebugTestYard.box(x0, gy, z0, x1, gy + 1, z1));
        YardPens.register(gy, x0, x1, z0, z1, watchName);
    }

    private static @Nullable Horse horse(ServerLevel level, int gy, double x, double z, Sex sex, String code,
                                         boolean tamed, String name) {
        Horse h = DebugPenManager.spawnHorse(level, gy + 1, x, z, sex, code, tamed);
        DebugTestYard.label(h, name);
        return h;
    }

    /** At the start of the better half of a heat, so a pair has time to meet. */
    static void inHeat(@Nullable Horse mare) {
        if (mare == null) {
            return;
        }
        ReproTiming t = ServerConfig.reproTiming();
        long now = mare.level().getGameTime();
        ReproHandler.set(mare, ReproHandler.of(mare).withCyclePhase(
                ReproRules.phaseFor(now, t.estrusTicks() / 2, t)));
    }

    /** Half way through the quiet part of her cycle. */
    static void outOfHeat(@Nullable Horse mare) {
        if (mare == null) {
            return;
        }
        ReproTiming t = ServerConfig.reproTiming();
        long now = mare.level().getGameTime();
        ReproHandler.set(mare, ReproHandler.of(mare).withCyclePhase(
                ReproRules.phaseFor(now, t.estrusTicks() + t.diestrusTicks() / 2, t)));
    }

    /**
     * <b>A test-only switch</b>: her last natural try is stamped at the end of
     * time, so no heat ever starts after it. For pens whose test is something other
     * than natural breeding, and which would otherwise breed on their own.
     */
    static void noNaturalCovers(@Nullable Horse mare) {
        if (mare != null) {
            ReproHandler.set(mare, ReproHandler.of(mare).withNaturalTry(Long.MAX_VALUE));
        }
    }

    /** {@link #noNaturalCovers} for every mare in a box. */
    static void noNaturalCoversIn(ServerLevel level, AABB box) {
        for (Horse h : level.getEntitiesOfClass(Horse.class, box.inflate(0.0, 2.0, 0.0), Horse::isAlive)) {
            if (HorseRecords.hasRealRecord(h) && HorseRecords.of(h).sex() == Sex.FEMALE) {
                noNaturalCovers(h);
            }
        }
    }

    /**
     * <b>Make this mare pregnant with exactly the pregnancy the pen promises</b> -
     * twins or not, an early loss or not - then set when it ends. Drawn through the
     * real conception path, retried until the dice agree, so what is carried is an
     * ordinary pregnancy rather than a hand-built one.
     */
    private static void pregnant(@Nullable Horse mare, Genome sire, HorseRecord sireRecord,
                                 boolean twins, boolean loss, long dueIn) {
        if (mare == null) {
            return;
        }
        ReproTiming t = ServerConfig.reproTiming();
        long now = mare.level().getGameTime();
        HorseRecord mareRecord = HorseRecords.of(mare);
        Genome mareGenome = HorseBreedingHandler.genomeOf(mare, mareRecord, HorseRecords.rng(mare));
        for (int attempt = 0; attempt < 400; attempt++) {
            ReproHandler.set(mare, ReproHandler.of(mare).withPregnancy(Optional.<Pregnancy>empty())
                    .withCyclePhase(ReproRules.phaseFor(now, t.estrusTicks() * 3 / 4, t)));
            Conception.Result result = ReproHandler.breed(mare, mareRecord, mareGenome, sire, sireRecord, null,
                    List.of(), "test yard", null);
            Optional<Pregnancy> p = result.pregnancy();
            if (p.isEmpty() || p.get().twins() != twins || p.get().hasEarlyLoss() != loss) {
                continue;
            }
            long lossTick = loss ? now + dueIn / 3 : Pregnancy.NO_LOSS;
            ReproHandler.set(mare, ReproHandler.of(mare).withPregnancy(
                    new Pregnancy(p.get().embryos(), now - 1, now + dueIn, lossTick)));
            noNaturalCovers(mare);
            return;
        }
        ReproHandler.set(mare, ReproHandler.of(mare).withPregnancy(Optional.<Pregnancy>empty()));
        HorseGenetics.LOGGER.warn("[Debug] test yard: could not make {} pregnant as promised (twins={}, loss={})"
                + " - is health.mode FULL?", mare.getName().getString(), twins, loss);
    }
}
