package com.example.horsegenetics.common.repro;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GameteBias;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Timing, the derived state, the odds, and a conception end to end - everything
 * in {@code common.repro}, with no world.
 */
class ReproRulesTest {

    private static final ReproTiming T = ReproTiming.STANDARD;
    private static final long DAY = ReproTiming.DAY_TICKS;
    private static final UUID SIRE = new UUID(7L, 7L);

    // ------------------------------------------------------------------
    // Timing
    // ------------------------------------------------------------------

    @Test
    void aOneDayGestationFloorsEveryStageAtADay() {
        assertEquals(DAY, T.gestationTicks());
        assertEquals(DAY, T.estrusTicks());
        assertEquals(DAY, T.diestrusTicks());
        assertEquals(2 * DAY, T.cycleTicks(), "a cycle is estrus plus diestrus, not a third floor");
        assertEquals(DAY, T.postpartumTicks());
    }

    @Test
    void aRealLengthGestationRecoversTheRealRatios() {
        ReproTiming real = ReproTiming.of(340, DAY);
        assertEquals(5 * DAY, real.estrusTicks());
        assertEquals(16 * DAY, real.diestrusTicks());
        assertEquals(21 * DAY, real.cycleTicks());
        assertEquals(7 * DAY, real.postpartumTicks());
    }

    @Test
    void estrusLeavesTheFloorAtSixtyEightDays() {
        assertEquals(DAY, ReproTiming.of(68, DAY).estrusTicks());
        assertTrue(ReproTiming.of(80, DAY).estrusTicks() > DAY);
    }

    @Test
    void theDebugDayShrinksTheWholeCalendar() {
        ReproTiming fast = ReproTiming.of(1, 1200);
        assertEquals(1200, fast.gestationTicks());
        assertEquals(2400, fast.cycleTicks());
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    @Test
    void aMareSpendsHalfOfEachShortCycleInHeat() {
        Reproduction r = Reproduction.fresh(UUID.randomUUID());
        int inHeat = 0;
        int samples = 0;
        for (long now = 0; now < 10 * T.cycleTicks(); now += 100) {
            samples++;
            if (ReproRules.stateAt(r, now, T) == ReproState.ESTRUS) {
                inHeat++;
            }
        }
        assertEquals(0.5, inHeat / (double) samples, 0.01);
    }

    @Test
    void twoMaresDoNotComeIntoHeatTogether() {
        Reproduction a = Reproduction.fresh(new UUID(1L, 0x1234_5678_9ABC_DEF0L));
        Reproduction b = Reproduction.fresh(new UUID(1L, 0x7EDC_BA98_7654_3210L));
        assertTrue(Math.abs(a.cyclePhase() - b.cyclePhase()) > 0.05);
    }

    @Test
    void peakIsTheSecondHalfOfHeat() {
        Reproduction r = new Reproduction(0.0, Optional.empty(), Reproduction.NEVER, List.of(),
                Reproduction.NEVER, Reproduction.NEVER, 0);
        assertEquals(ReproRules.EARLY_CHANCE, ReproRules.baseChance(r, 10, T));
        assertEquals(ReproRules.PEAK_CHANCE, ReproRules.baseChance(r, DAY / 2 + 10, T));
        assertEquals(0.0, ReproRules.baseChance(r, DAY + 10, T), "diestrus");
        assertEquals(DAY - 10, ReproRules.ticksUntilReceptive(r, DAY + 10, T));
    }

    @Test
    void phaseForPutsAMareWhereTheClockAsks() {
        for (long now : new long[]{0L, 12_345L, 987_654_321L}) {
            long peak = T.estrusTicks() * 3 / 4;
            Reproduction r = Reproduction.fresh(UUID.randomUUID()).withCyclePhase(ReproRules.phaseFor(now, peak, T));
            assertEquals(ReproState.ESTRUS, ReproRules.stateAt(r, now, T));
            assertTrue(ReproRules.inPeak(r, now, T), "at " + now);
        }
    }

    @Test
    void afterBirthComesPostpartumThenFoalHeatThenTheCycle() {
        long born = 50 * DAY;
        Reproduction r = new Reproduction(0.5, Optional.empty(), Reproduction.NEVER, List.of(),
                Reproduction.NEVER, Reproduction.NEVER, 0).foaled(born, List.of(UUID.randomUUID()));
        assertEquals(ReproState.POSTPARTUM, ReproRules.stateAt(r, born + 1, T));
        assertEquals(ReproState.FOAL_HEAT, ReproRules.stateAt(r, born + DAY + 1, T));
        assertEquals(ReproRules.FOAL_HEAT_CHANCE, ReproRules.baseChance(r, born + DAY + 1, T));
        ReproState after = ReproRules.stateAt(r, born + 2 * DAY + 1, T);
        assertTrue(after == ReproState.ESTRUS || after == ReproState.DIESTRUS, "back to cycling: " + after);
        assertTrue(r.lactating(), "nursing runs beside every state");
    }

    // ------------------------------------------------------------------
    // Odds
    // ------------------------------------------------------------------

    @Test
    void chanceIsCappedAndZeroStaysZero() {
        assertEquals(ReproRules.MAX_CHANCE, ReproRules.conceptionChance(0.8, 1.3, 1.0));
        assertEquals(0.0, ReproRules.conceptionChance(0.0, 5.0, 5.0));
        assertEquals(0.4, ReproRules.conceptionChance(0.8, 1.0, ReproRules.stallionFactor(3)), 1e-9);
        assertEquals(1.0, ReproRules.stallionFactor(2));
    }

    @Test
    void coversResetEachDay() {
        Reproduction r = Reproduction.fresh(SIRE);
        r = r.withCover(10, DAY).withCover(20, DAY).withCover(30, DAY);
        assertEquals(3, r.coversOn(40, DAY));
        assertEquals(0, r.coversOn(DAY + 5, DAY));
        assertEquals(1, r.withCover(DAY + 5, DAY).coversOn(DAY + 6, DAY));
    }

    @Test
    void twinsMostlyBothLive() {
        int[] counts = new int[3];
        for (long seed = 0; seed < 20_000; seed++) {
            counts[ReproRules.twinSurvivors(new SeededRng(seed))]++;
        }
        assertEquals(0.75, counts[2] / 20_000.0, 0.02);
        assertEquals(0.20, counts[1] / 20_000.0, 0.02);
        assertEquals(0.05, counts[0] / 20_000.0, 0.01);
    }

    @Test
    void anEarlyLossFallsInTheFirstThird() {
        for (long seed = 0; seed < 500; seed++) {
            long loss = ReproRules.earlyLossTick(1000, 1000 + DAY, new SeededRng(seed));
            assertTrue(loss > 1000 && loss <= 1000 + DAY / 3 + 1, "loss at " + loss);
        }
    }

    @Test
    void theLastThirdIsLate() {
        Pregnancy p = pregnancy(0, 3 * DAY);
        assertFalse(ReproRules.late(p, 2 * DAY - 1));
        assertTrue(ReproRules.late(p, 2 * DAY));
        assertEquals(1, ReproRules.daysLeft(p, 2 * DAY + 5, T));
    }

    @Test
    void aDayApartWeansTheFoal() {
        assertFalse(ReproRules.weanedByAbsence(Reproduction.NEVER, 5 * DAY, T));
        assertFalse(ReproRules.weanedByAbsence(DAY, 2 * DAY - 1, T));
        assertTrue(ReproRules.weanedByAbsence(DAY, 2 * DAY, T));
    }

    // ------------------------------------------------------------------
    // Conception
    // ------------------------------------------------------------------

    @Test
    void aMareOutOfHeatIsNotReceptiveAndNothingIsDrawn() {
        Reproduction mare = cycleStart();
        Conception.Result r = Conception.attempt(mating(Genotype.wildType(), Genotype.wildType()),
                mare, DAY + 10, T, 0, true, true, new SeededRng(1));
        assertEquals(Conception.Outcome.NOT_RECEPTIVE, r.outcome());
        assertTrue(r.pregnancy().isEmpty());
    }

    @Test
    void aPeakBreedingTakesAboutFourTimesInFive() {
        int took = 0;
        int n = 2000;
        for (long seed = 0; seed < n; seed++) {
            Conception.Result r = Conception.attempt(mating(Genotype.wildType(), Genotype.wildType()),
                    cycleStart(), DAY * 3 / 4, T, 0, true, true, new SeededRng(seed));
            if (r.outcome() == Conception.Outcome.CONCEIVED) {
                took++;
                Pregnancy p = r.pregnancy().orElseThrow();
                assertEquals(DAY * 3 / 4 + T.gestationTicks(), p.dueTick());
            }
        }
        // the dam's fertility number sits around 1, so the odds do too
        assertEquals(0.80, took / (double) n, 0.06);
    }

    @Test
    void aSubfertileMareHalvesTheOdds() {
        Genotype sf = Genotype.wildType().with(new AllelePair(Genes.FERTILITY.sf, Genes.FERTILITY.sf));
        int took = 0;
        int n = 2000;
        for (long seed = 0; seed < n; seed++) {
            if (Conception.attempt(mating(sf, Genotype.wildType()), cycleStart(), DAY * 3 / 4, T, 0, true, true,
                    new SeededRng(seed)).outcome() == Conception.Outcome.CONCEIVED) {
                took++;
            }
        }
        assertEquals(0.40, took / (double) n, 0.05);
    }

    @Test
    void twoTwinningCopiesMakeTwinsCommon() {
        Genotype twTw = Genotype.wildType().with(new AllelePair(Genes.FERTILITY.tw, Genes.FERTILITY.tw));
        int pregnancies = 0;
        int twins = 0;
        for (long seed = 0; seed < 4000; seed++) {
            Optional<Pregnancy> p = Conception.attempt(mating(twTw, Genotype.wildType()), cycleStart(),
                    DAY * 3 / 4, T, 0, true, true, new SeededRng(seed)).pregnancy();
            if (p.isPresent()) {
                pregnancies++;
                if (p.get().twins()) {
                    twins++;
                }
            }
        }
        assertEquals(0.25, twins / (double) pregnancies, 0.03);
    }

    /** Two MET carriers lose one pregnancy in four, early, and the embryos say which. */
    @Test
    void aLethalEmbryoIsCarriedThenLostEarly() {
        AllelePair carrier = new AllelePair(Genes.MET.variant, Genes.MET.baseline);
        Genotype dam = Genotype.wildType().with(carrier);
        Genotype sire = Genotype.wildType().with(carrier);
        int pregnancies = 0;
        int withLoss = 0;
        for (long seed = 0; seed < 3000; seed++) {
            Optional<Pregnancy> p = Conception.attempt(mating(dam, sire), cycleStart(), DAY * 3 / 4, T, 0,
                    true, true, new SeededRng(seed)).pregnancy();
            if (p.isEmpty()) {
                continue;
            }
            pregnancies++;
            Pregnancy preg = p.get();
            assertEquals(preg.lostEarlyCount() > 0, preg.hasEarlyLoss());
            if (preg.hasEarlyLoss()) {
                withLoss++;
                if (!preg.twins()) {
                    assertTrue(preg.afterEarlyLoss().isEmpty(), "a lost single leaves nothing");
                }
            }
        }
        assertEquals(0.25, withLoss / (double) pregnancies, 0.04);
    }

    @Test
    void lethalsOffCarriesEveryEmbryo() {
        AllelePair affected = new AllelePair(Genes.MET.variant, Genes.MET.variant);
        Genotype g = Genotype.wildType().with(affected);
        for (long seed = 0; seed < 200; seed++) {
            Conception.attempt(mating(g, g), cycleStart(), DAY * 3 / 4, T, 0, true, false, new SeededRng(seed))
                    .pregnancy().ifPresent(p -> assertFalse(p.hasEarlyLoss()));
        }
    }

    @Test
    void vanillaIsUntouchedUnlessAParentIsSubfertile() {
        Genotype wild = Genotype.wildType();
        Genotype carrier = wild.with(new AllelePair(Genes.FERTILITY.sf, Genes.FERTILITY.n));
        Genotype sf = wild.with(new AllelePair(Genes.FERTILITY.sf, Genes.FERTILITY.sf));
        assertEquals(1.0, Conception.vanillaFoalChance(wild, wild));
        assertEquals(1.0, Conception.vanillaFoalChance(carrier, carrier));
        assertEquals(0.5, Conception.vanillaFoalChance(sf, wild));
        assertEquals(0.25, Conception.vanillaFoalChance(sf, sf));
    }

    // ------------------------------------------------------------------

    /** A mare whose cycle starts at tick 0: in heat for the first day, peak from half a day. */
    private static Reproduction cycleStart() {
        return new Reproduction(0.0, Optional.empty(), Reproduction.NEVER, List.of(),
                Reproduction.NEVER, Reproduction.NEVER, 0);
    }

    private static Conception.Mating mating(Genotype dam, Genotype sire) {
        Genome d = Genome.of(dam, new SeededRng(11)).withSex(Sex.FEMALE);
        Genome s = Genome.of(sire, new SeededRng(12)).withSex(Sex.MALE);
        return new Conception.Mating(d, BreedLineage.FERAL, s, BreedLineage.FERAL, SIRE, "Test", "Sire", 0,
                GameteBias.NONE, GameteBias.NONE, "tester");
    }

    private static Pregnancy pregnancy(long conceived, long due) {
        Genome g = Genome.of(Genotype.wildType(), new SeededRng(3));
        Embryo e = new Embryo(com.example.horsegenetics.common.genetics.GenomeSample.of(g), "", false, SIRE,
                "Test", "Sire", 0, com.example.horsegenetics.common.genetics.GenomeSample.of(g), "");
        return new Pregnancy(List.of(e), conceived, due, Pregnancy.NO_LOSS);
    }
}
