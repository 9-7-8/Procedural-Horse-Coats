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
import static com.example.horsegenetics.neoforge.server.DebugTestYard.FERTILITY_ROW_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_O;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_P;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_Q;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_R;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_S;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_T;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows O-T of the test yard: breeding and fertility, one scenario per pen</b>
 * (2026-09-13, built for the owner to audit the whole system in one walk).
 *
 * <p>Every pen is a single claim with its expected outcome on the sign, and every
 * pen registers {@link DebugWorldWatch#watchBreeding}, so the log carries each
 * horse's breeding state - in heat, pregnant, nursing, covers today - whenever it
 * changes and at every census. Most of them run themselves: a pen whose claim is
 * "this never happens" only needs its horse count to stay put.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>O</td><td>NATURAL PAIR - covered on arrival, foal in a minute</td>
 *       <td>GELDING CONTROL - never a foal</td></tr>
 *   <tr><td>P</td><td>SUBFERTILE PAIR - one cover a heat, most miss</td>
 *       <td>HURT MARE - never covered while hurt</td></tr>
 *   <tr><td>Q</td><td>THE CAP - nine horses, nobody covered</td>
 *       <td>COWBOY STOCK - never covered</td></tr>
 *   <tr><td>R</td><td>MATERNITY - births at 1, 2, 3 minutes: single, twins, loss</td>
 *       <td>WEANING - lead the foal away</td></tr>
 *   <tr><td>S</td><td>GOLD ANY HEAT - instant foal out of heat</td>
 *       <td>JAR BENCH - seed jar, vet's kit, clock</td></tr>
 *   <tr><td>T</td><td>MET NATURAL - left to breed; early losses and a plateau</td>
 *       <td>SUBFERTILE GOLD - golden carrots, mostly "didn't take"</td></tr>
 * </table>
 *
 * <p>Timings assume {@code debug.tools} is on, the dev default: a reproductive
 * day is one minute. The maternity due dates are absolute ticks and hold either
 * way.
 */
final class DebugYardFertility {

    private DebugYardFertility() {
    }

    /** A pen's inside width; the cap pen is wider. */
    private static final int W = 8;
    private static final int CAP_W = 18;

    private static final String FERT = "horsegenetics.fertility=";
    private static final String MET_CARRIER = "horsegenetics.met=met/N";

    /** A cowboy who does not exist, for the branded pair. */
    private static final UUID NOBODY = new UUID(0x7E57L, 0xC0B0L);

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 10;
        try {
            naturalPair(level, gy, west, mouthZ + ROW_O);
            geldingControl(level, gy, east, mouthZ + ROW_O);
            subfertilePair(level, gy, west, mouthZ + ROW_P);
            hurtMare(level, gy, east, mouthZ + ROW_P);
            theCap(level, gy, west, mouthZ + ROW_Q);
            cowboyStock(level, gy, east, mouthZ + ROW_Q);
            maternity(level, gy, west, mouthZ + ROW_R);
            weaning(level, gy, east, mouthZ + ROW_R);
            goldAnyHeat(level, gy, west, mouthZ + ROW_S);
            jarBench(level, gy, east, mouthZ + ROW_S);
            metNatural(level, gy, west, mouthZ + ROW_T);
            subfertileGold(level, gy, east, mouthZ + ROW_T);
            ActionTrace.log("test yard", "fertility rows O-T built");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: fertility rows failed to build", e);
        }
    }

    // ------------------------------------------------------------------
    // Row O
    // ------------------------------------------------------------------

    private static void naturalPair(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "NATURAL PAIR",
                List.of("NATURAL PAIR", "mare in heat now:", "hearts in seconds,", "foal in ~1 min"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 5, Sex.FEMALE, FERT + "n/n", true, "NATURAL MARE");
        horse(level, gy, x0 + 5.5, z0 + 5, Sex.MALE, FERT + "n/n", true, "NATURAL STUD");
        inHeat(mare);
    }

    private static void geldingControl(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "GELDING CONTROL",
                List.of("GELDING CONTROL", "mare in heat,", "gelded stallion:", "NO foal, ever"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 5, Sex.FEMALE, FERT + "n/n", true, "CONTROL MARE");
        Horse gelding = horse(level, gy, x0 + 5.5, z0 + 5, Sex.MALE, FERT + "n/n", true, "GELDING");
        inHeat(mare);
        if (gelding != null) {
            HorseRecords.apply(gelding, HorseRecords.of(gelding).withGelded(true));
        }
    }

    // ------------------------------------------------------------------
    // Row P
    // ------------------------------------------------------------------

    private static void subfertilePair(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "SUBFERTILE PAIR",
                List.of("SUBFERTILE PAIR", "sf/sf both: ONE", "cover per heat,", "most do not take"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 5, Sex.FEMALE, FERT + "sf/sf", true, "SF MARE");
        horse(level, gy, x0 + 5.5, z0 + 5, Sex.MALE, FERT + "sf/sf", true, "SF STUD");
        inHeat(mare);
    }

    private static void hurtMare(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "HURT MARE",
                List.of("HURT MARE", "half health, in", "heat: NOT covered", "until she heals"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 5, Sex.FEMALE, FERT + "n/n", true, "HURT MARE");
        horse(level, gy, x0 + 5.5, z0 + 5, Sex.MALE, FERT + "n/n", true, "HURT PEN STUD");
        if (mare != null) {
            mare.setHealth(mare.getMaxHealth() / 2.0F);
        }
        inHeat(mare);
    }

    // ------------------------------------------------------------------
    // Row Q
    // ------------------------------------------------------------------

    private static void theCap(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, CAP_W, "THE CAP",
                List.of("THE CAP", "8 mares + 1 stud,", "all in heat: NOBODY", "is covered"));
        for (int i = 0; i < 8; i++) {
            inHeat(horse(level, gy, x0 + 2.5 + (i % 4) * 2.0, z0 + 3.0 + (i / 4) * 3.0, Sex.FEMALE,
                    FERT + "n/n", true, "CAP MARE " + (i + 1)));
        }
        horse(level, gy, x0 + 14.5, z0 + 5, Sex.MALE, FERT + "n/n", true, "CAP STUD");
        DebugYardGameplay.chest(level, gy, x0 + 10, z0 - 2, "THE CAP", List.of(
                new ItemStack(Items.LEAD, 2),
                new ItemStack(ModItems.VET_KIT.get())));
    }

    private static void cowboyStock(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "COWBOY STOCK",
                List.of("COWBOY STOCK", "branded pair in", "heat: NEVER", "covered"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 5, Sex.FEMALE, FERT + "n/n", true, "BRANDED MARE");
        Horse stud = horse(level, gy, x0 + 5.5, z0 + 5, Sex.MALE, FERT + "n/n", true, "BRANDED STUD");
        inHeat(mare);
        for (Horse h : new Horse[]{mare, stud}) {
            if (h != null) {
                h.setData(ModAttachments.COWBOY_BRAND.get(), CowboyBrand.of(NOBODY));
            }
        }
    }

    // ------------------------------------------------------------------
    // Row R
    // ------------------------------------------------------------------

    private static void maternity(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "MATERNITY",
                List.of("MATERNITY", "foals at 1, 2, 3 min", "TWINS mare: two;", "LOSS mare: none"));
        ReproTiming t = ServerConfig.reproTiming();
        long day = t.dayTicks();
        NeoRng rng = new NeoRng(level.getRandom());

        Genome plainSire = Genome.of(Genotype.parse(FERT + "n/n"), rng).withSex(Sex.MALE);
        HorseRecord plainSireRecord = HorseRecord.founder(new UUID(0x51EL, 1L), "Yard", "Sire", plainSire);
        Genome metSire = Genome.of(Genotype.parse(MET_CARRIER), rng).withSex(Sex.MALE);
        HorseRecord metSireRecord = HorseRecord.founder(new UUID(0x51EL, 2L), "Carrier", "Sire", metSire);

        pregnant(horse(level, gy, x0 + 2.0, z0 + 3, Sex.FEMALE, FERT + "n/n", true, "DUE 1 MIN"),
                plainSire, plainSireRecord, false, false, day);
        pregnant(horse(level, gy, x0 + 4.5, z0 + 5, Sex.FEMALE, FERT + "tw/tw", true, "TWINS DUE 2 MIN"),
                plainSire, plainSireRecord, true, false, 2 * day);
        pregnant(horse(level, gy, x0 + 7.0, z0 + 7, Sex.FEMALE, MET_CARRIER, true, "LOSS AT 1, DUE 3"),
                metSire, metSireRecord, false, true, 3 * day);
        DebugYardGameplay.chest(level, gy, x0 + 5, z0 - 2, "MATERNITY", List.of(
                new ItemStack(ModItems.VET_KIT.get())));
    }

    private static void weaning(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "WEANING",
                List.of("WEANING", "mare nursing: lead", "foal 32+ blocks", "away 1 min"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 5, Sex.FEMALE, FERT + "n/n", true, "NURSING MARE");
        Horse foal = horse(level, gy, x0 + 5.5, z0 + 5, Sex.FEMALE, FERT + "n/n", true, "WEAN ME");
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
    // Row S
    // ------------------------------------------------------------------

    private static void goldAnyHeat(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "GOLD ANY HEAT",
                List.of("GOLD, ANY HEAT", "mare OUT of heat:", "gold both = foal", "AT ONCE anyway"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 5, Sex.FEMALE, FERT + "n/n", true, "GOLD MARE");
        horse(level, gy, x0 + 5.5, z0 + 5, Sex.MALE, FERT + "n/n", true, "GOLD STUD");
        outOfHeat(mare);
        noNaturalCovers(mare);
        DebugYardGameplay.chest(level, gy, x0 + 5, z0 - 2, "GOLD ANY HEAT", List.of(
                new ItemStack(Items.GOLDEN_CARROT, 16),
                new ItemStack(ModItems.VET_KIT.get())));
    }

    private static void jarBench(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "JAR BENCH",
                List.of("JAR & KIT BENCH", "TAME all 3 first,", "fill a jar, use on", "both; geld stud"));
        horse(level, gy, x0 + 1.5, z0 + 3, Sex.MALE, FERT + "n/n", false, "JAR STUD");
        Horse ready = horse(level, gy, x0 + 4.5, z0 + 5, Sex.FEMALE, FERT + "n/n", false, "IN HEAT: JAR TAKES");
        Horse notReady = horse(level, gy, x0 + 7.0, z0 + 7, Sex.FEMALE, FERT + "n/n", false, "OUT OF HEAT: REFUSES");
        inHeat(ready);
        outOfHeat(notReady);
        noNaturalCovers(ready);
        noNaturalCovers(notReady);
        DebugYardGameplay.chest(level, gy, x0 + 5, z0 - 2, "JAR BENCH", List.of(
                new ItemStack(Items.STICK, 3),
                new ItemStack(ModItems.EMPTY_SEED_JAR.get(), 3),
                new ItemStack(Items.GOLDEN_CARROT, 8),
                new ItemStack(ModItems.STABILIZER_CARROT.get(), 4),
                new ItemStack(ModItems.VET_KIT.get()),
                new ItemStack(Items.CLOCK)));
    }

    // ------------------------------------------------------------------
    // Row T
    // ------------------------------------------------------------------

    private static void metNatural(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "MET NATURAL",
                List.of("MET CARRIERS", "left to breed: ~1", "in 4 lost early;", "stops at the cap"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 5, Sex.FEMALE, MET_CARRIER, true, "MET MARE");
        horse(level, gy, x0 + 5.5, z0 + 5, Sex.MALE, MET_CARRIER, true, "MET STUD");
        inHeat(mare);
    }

    private static void subfertileGold(ServerLevel level, int gy, int x0, int z0) {
        pen(level, gy, x0, z0, W, "SUBFERTILE GOLD",
                List.of("SUBFERTILE GOLD", "sf/sf pair: gold", "both. ~1 foal in 4,", "else didn't take"));
        Horse mare = horse(level, gy, x0 + 2.5, z0 + 5, Sex.FEMALE, FERT + "sf/sf", true, "SF GOLD MARE");
        horse(level, gy, x0 + 5.5, z0 + 5, Sex.MALE, FERT + "sf/sf", true, "SF GOLD STUD");
        noNaturalCovers(mare);
        DebugYardGameplay.chest(level, gy, x0 + 5, z0 - 2, "SUBFERTILE GOLD", List.of(
                new ItemStack(Items.GOLDEN_CARROT, 32)));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Walls, sign, and a breeding watch over the inside. */
    private static void pen(ServerLevel level, int gy, int x0, int z0, int width, String watchName,
                            List<String> sign) {
        int x1 = x0 + width;
        int z1 = z0 + FERTILITY_ROW_D;
        DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        DebugWorldWatch.watchBreeding(watchName, DebugTestYard.box(x0, gy, z0, x1, gy + 1, z1));
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
