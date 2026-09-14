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
import com.example.horsegenetics.neoforge.data.CowboyBrand;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_O;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_O_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_P;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_P_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Q;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Q_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_S;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_S_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows O-Q of the test yard, and MET NATURAL in S: breeding and fertility, one
 * scenario per pen</b> (2026-09-13, packed 2026-09-14).
 *
 * <p>Every pen is a single claim with its expected outcome on the sign, and every
 * pen registers {@link DebugWorldWatch#watchBreeding}, so the log carries each
 * horse's breeding state - in heat, pregnant, nursing, covers today - whenever it
 * changes and at every census. Every pen is also its own {@link YardPens} pen, so the
 * natural-cover cap and partner search see nothing over the wall.
 *
 * <p><b>A three-block gap, not a shared wall, between breeding pens.</b> Golden-carrot
 * breeding is vanilla's goal, which pairs two horses in love within three blocks
 * regardless of walls, and {@code YardPens} does not reach it. So the jar stud has his
 * own pen apart from the jar mares (owner, 2026-09-14), and no two pens share a wall.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>O</td><td>NATURAL PAIR, GELDING CONTROL</td><td>SUBFERTILE PAIR, HURT MARE</td></tr>
 *   <tr><td>P</td><td>THE CAP, COWBOY STOCK</td><td>MATERNITY, WEANING</td></tr>
 *   <tr><td>Q</td><td>JAR STUD, JAR &amp; KIT BENCH</td><td>GOLD ANY HEAT, SUBFERTILE GOLD</td></tr>
 *   <tr><td>S</td><td>(herd rows)</td><td>MET NATURAL</td></tr>
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

    /** A cowboy who does not exist, for the branded pair. */
    private static final UUID NOBODY = new UUID(0x7E57L, 0xC0B0L);

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN;
        try {
            naturalPair(level, gy, west, mouthZ + ROW_O);
            geldingControl(level, gy, west + 10, mouthZ + ROW_O);
            subfertilePair(level, gy, east, mouthZ + ROW_O);
            hurtMare(level, gy, east + 10, mouthZ + ROW_O);
            theCap(level, gy, west, mouthZ + ROW_P);
            cowboyStock(level, gy, west + 14, mouthZ + ROW_P);
            maternity(level, gy, east, mouthZ + ROW_P);
            weaning(level, gy, east + 12, mouthZ + ROW_P);
            jarBench(level, gy, west, mouthZ + ROW_Q);
            goldAnyHeat(level, gy, east, mouthZ + ROW_Q);
            subfertileGold(level, gy, east + 10, mouthZ + ROW_Q);
            metNatural(level, gy, east, mouthZ + ROW_S);
            ActionTrace.log("test yard", "fertility pens built (rows O-Q, and MET NATURAL in S)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: fertility rows failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Row O
    // ------------------------------------------------------------------

    private static void naturalPair(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 7, ROW_O_D, "NATURAL PAIR",
                List.of("NATURAL PAIR", "mare in heat now:", "hearts in seconds,", "foal in ~1 min"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 4, Sex.FEMALE, FERT + "n/n", true, "NATURAL MARE");
        horse(level, gy, x0 + 4.5, z0 + 4, Sex.MALE, FERT + "n/n", true, "NATURAL STUD");
        inHeat(mare);
    }

    private static void geldingControl(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 7, ROW_O_D, "GELDING CONTROL",
                List.of("GELDING CONTROL", "mare in heat,", "gelded stallion:", "NO foal, ever"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 4, Sex.FEMALE, FERT + "n/n", true, "CONTROL MARE");
        Horse gelding = horse(level, gy, x0 + 4.5, z0 + 4, Sex.MALE, FERT + "n/n", true, "GELDING");
        inHeat(mare);
        if (gelding != null) {
            HorseRecords.apply(gelding, HorseRecords.of(gelding).withGelded(true));
            DebugTestYard.label(gelding, "GELDING");    // applying a record clears the label
        }
    }

    private static void subfertilePair(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 7, ROW_O_D, "SUBFERTILE PAIR",
                List.of("SUBFERTILE PAIR", "sf/sf both: ONE", "cover per heat,", "most do not take"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 4, Sex.FEMALE, FERT + "sf/sf", true, "SF MARE");
        horse(level, gy, x0 + 4.5, z0 + 4, Sex.MALE, FERT + "sf/sf", true, "SF STUD");
        inHeat(mare);
    }

    private static void hurtMare(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 7, ROW_O_D, "HURT MARE",
                List.of("HURT MARE", "half health, in", "heat: NOT covered", "until she heals"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 4, Sex.FEMALE, FERT + "n/n", true, "HURT MARE");
        horse(level, gy, x0 + 4.5, z0 + 4, Sex.MALE, FERT + "n/n", true, "HURT PEN STUD");
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

    // ------------------------------------------------------------------
    // Row P
    // ------------------------------------------------------------------

    private static void theCap(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 11, ROW_P_D, "THE CAP",
                List.of("THE CAP", "8 mares + 1 stud,", "all in heat: NOBODY", "is covered"));
        for (int i = 0; i < 8; i++) {
            inHeat(horse(level, gy, x0 + 2.0 + (i % 4) * 2.0, z0 + 2.5 + (i / 4) * 3.0, Sex.FEMALE,
                    FERT + "n/n", true, "CAP MARE " + (i + 1)));
        }
        horse(level, gy, x0 + 9.2, z0 + 6, Sex.MALE, FERT + "n/n", true, "CAP STUD");
        DebugYardGameplay.chest(level, gy, x0 + 8, z0 - 2, "THE CAP", List.of(
                new ItemStack(Items.LEAD, 2),
                new ItemStack(ModItems.VET_KIT.get())));
    }

    private static void cowboyStock(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 5, ROW_P_D, "COWBOY STOCK",
                List.of("COWBOY STOCK", "branded pair in", "heat: NEVER", "covered"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 2.5, Sex.FEMALE, FERT + "n/n", true, "BRANDED MARE");
        Horse stud = horse(level, gy, x0 + 2.5, z0 + 5.5, Sex.MALE, FERT + "n/n", true, "BRANDED STUD");
        inHeat(mare);
        for (Horse h : new Horse[]{mare, stud}) {
            if (h != null) {
                h.setData(ModAttachments.COWBOY_BRAND.get(), CowboyBrand.of(NOBODY));
            }
        }
    }

    private static void maternity(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_P_D, "MATERNITY",
                List.of("MATERNITY", "foals at 1, 2, 3 min", "TWINS mare: two;", "LOSS mare: none"));
        ReproTiming t = ServerConfig.reproTiming();
        long day = t.dayTicks();
        NeoRng rng = new NeoRng(level.getRandom());

        Genome plainSire = Genome.of(Genotype.parse(FERT + "n/n"), rng).withSex(Sex.MALE);
        HorseRecord plainSireRecord = HorseRecord.founder(new UUID(0x51EL, 1L), "Yard", "Sire", plainSire);
        Genome metSire = Genome.of(Genotype.parse(MET_CARRIER), rng).withSex(Sex.MALE);
        HorseRecord metSireRecord = HorseRecord.founder(new UUID(0x51EL, 2L), "Carrier", "Sire", metSire);

        pregnant(horse(level, gy, x0 + 2.0, z0 + 2.5, Sex.FEMALE, FERT + "n/n", true, "DUE 1 MIN"),
                plainSire, plainSireRecord, false, false, day);
        pregnant(horse(level, gy, x0 + 4.5, z0 + 4.5, Sex.FEMALE, FERT + "tw/tw", true, "TWINS DUE 2 MIN"),
                plainSire, plainSireRecord, true, false, 2 * day);
        pregnant(horse(level, gy, x0 + 7.0, z0 + 6.5, Sex.FEMALE, MET_CARRIER, true, "LOSS AT 1, DUE 3"),
                metSire, metSireRecord, false, true, 3 * day);
        DebugYardGameplay.chest(level, gy, x0 + 5, z0 - 2, "MATERNITY", List.of(
                new ItemStack(ModItems.VET_KIT.get())));
    }

    private static void weaning(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 7, ROW_P_D, "WEANING",
                List.of("WEANING", "mare nursing: lead", "foal 32+ blocks", "away 1 min"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 4, Sex.FEMALE, FERT + "n/n", true, "NURSING MARE");
        Horse foal = horse(level, gy, x0 + 4.5, z0 + 4, Sex.FEMALE, FERT + "n/n", true, "WEAN ME");
        if (mare == null || foal == null) {
            return;
        }
        foal.setAge(-24000);
        foal.setData(ModAttachments.HORSE_SOCIAL.get(),
                foal.getData(ModAttachments.HORSE_SOCIAL.get()).withDam(Optional.of(mare.getUUID())));
        long now = level.getGameTime();
        ReproHandler.set(mare, ReproHandler.of(mare).foaled(now - 10, List.of(foal.getUUID())));
        DebugYardGameplay.chest(level, gy, x0 + 5, z0 - 2, "WEANING", List.of(new ItemStack(Items.LEAD, 2)));
    }

    // ------------------------------------------------------------------
    // Row Q
    // ------------------------------------------------------------------

    /**
     * Two pens with a gap between them: the stud on his own, the two mares next door.
     * Owner, 2026-09-14: "the jar stud shouldn't be physically in the same pen as the
     * mares or else he'll just breed them as normal".
     */
    private static void jarBench(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 5, ROW_Q_D, "JAR STUD",
                List.of("JAR STUD", "TAME, carrot him,", "fill a jar, then", "use it next door"));
        horse(level, gy, x0 + 2.5, z0 + 4, Sex.MALE, FERT + "n/n", false, "JAR STUD");

        int mx = x0 + 8;
        pen(level, gy, mx, z0, 11, ROW_Q_D, "JAR BENCH",
                List.of("JAR & KIT BENCH", "TAME both first;", "jar on each mare;", "geld the stud"));
        Horse ready = horse(level, gy, mx + 3.0, z0 + 4, Sex.FEMALE, FERT + "n/n", false, "IN HEAT: JAR TAKES");
        Horse notReady = horse(level, gy, mx + 7.5, z0 + 4, Sex.FEMALE, FERT + "n/n", false, "OUT OF HEAT: REFUSES");
        inHeat(ready);
        outOfHeat(notReady);
        noNaturalCovers(ready);
        noNaturalCovers(notReady);
        DebugYardGameplay.chest(level, gy, mx + 5, z0 - 2, "JAR BENCH", List.of(
                new ItemStack(Items.STICK, 3),
                new ItemStack(ModItems.EMPTY_SEED_JAR.get(), 3),
                new ItemStack(Items.GOLDEN_CARROT, 8),
                new ItemStack(ModItems.STABILIZER_CARROT.get(), 4),
                new ItemStack(ModItems.VET_KIT.get()),
                new ItemStack(Items.CLOCK)));
    }

    private static void goldAnyHeat(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 7, ROW_Q_D, "GOLD ANY HEAT",
                List.of("GOLD, ANY HEAT", "mare OUT of heat:", "gold both = foal", "AT ONCE anyway"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 4, Sex.FEMALE, FERT + "n/n", true, "GOLD MARE");
        horse(level, gy, x0 + 4.5, z0 + 4, Sex.MALE, FERT + "n/n", true, "GOLD STUD");
        outOfHeat(mare);
        noNaturalCovers(mare);
        DebugYardGameplay.chest(level, gy, x0 + 5, z0 - 2, "GOLD ANY HEAT", List.of(
                new ItemStack(Items.GOLDEN_CARROT, 16),
                new ItemStack(ModItems.VET_KIT.get())));
    }

    private static void subfertileGold(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 7, ROW_Q_D, "SUBFERTILE GOLD",
                List.of("SUBFERTILE GOLD", "sf/sf pair: gold", "both. ~1 foal in 4,", "else didn't take"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 4, Sex.FEMALE, FERT + "sf/sf", true, "SF GOLD MARE");
        horse(level, gy, x0 + 4.5, z0 + 4, Sex.MALE, FERT + "sf/sf", true, "SF GOLD STUD");
        noNaturalCovers(mare);
        DebugYardGameplay.chest(level, gy, x0 + 5, z0 - 2, "SUBFERTILE GOLD", List.of(
                new ItemStack(Items.GOLDEN_CARROT, 32)));
    }

    // ------------------------------------------------------------------
    // Row S east
    // ------------------------------------------------------------------

    /** Deeper than the others: it breeds on its own until it holds nine horses. */
    private static void metNatural(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, 9, ROW_S_D, "MET NATURAL",
                List.of("MET CARRIERS", "left to breed: ~1", "in 4 lost early;", "stops at the cap"));
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
    private static void outOfHeat(@Nullable Horse mare) {
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
