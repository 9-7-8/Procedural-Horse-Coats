package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.GameteBias;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.ReproRules;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In-bank breeding, minus the bank.
 *
 * <p>The thing being pinned is that <b>the bank invents no rule of its own</b>.
 * A chamber at stud is a horse in a paddock: she is covered when she is in heat
 * and not before, once per heat and not twice, and only if both of them are well
 * enough - and every one of those answers comes out of {@link ReproRules},
 * which the rest of the mod already obeys. Owner, 2026-09-24: <i>"only creates a
 * new foal once per the mare's heat, same cycle as on land"</i>.
 *
 * <p>The cycle itself needs no ticking to reach a shelved mare, which is the
 * property that makes the whole feature possible; {@code ReproRulesTest} is
 * where that is pinned, and {@link #aShelvedMareComesIntoHeatOnHerOwn} only
 * checks that this class reads it the same way.
 */
class StasisMatingTest {

    private static final ReproTiming T = ReproTiming.STANDARD;
    private static final UUID MARE_ID = UUID.nameUUIDFromBytes("mare".getBytes());
    private static final UUID SIRE_ID = UUID.nameUUIDFromBytes("sire".getBytes());

    private static HorseRecord mare() {
        Genome genome = Genome.of(Genotype.wildType(), new SeededRng(1)).withSex(Sex.FEMALE);
        return HorseRecord.founder(MARE_ID, "Amber", "Testcase", genome, "arabian");
    }

    private static HorseRecord sire() {
        Genome genome = Genome.of(Genotype.wildType(), new SeededRng(2)).withSex(Sex.MALE);
        return HorseRecord.founder(SIRE_ID, "Boyd", "Testcase", genome, "shire");
    }

    /** Her record, wound so that {@code now} is the peak of a heat. */
    private static Reproduction inHeat(long now) {
        return Reproduction.fresh(MARE_ID)
                .withCyclePhase(ReproRules.phaseFor(now, T.estrusTicks() * 3 / 4, T));
    }

    /** ...and so that it is the middle of the long quiet stretch after one. */
    private static Reproduction outOfHeat(long now) {
        return Reproduction.fresh(MARE_ID)
                .withCyclePhase(ReproRules.phaseFor(now, T.estrusTicks() + T.diestrusTicks() / 2, T));
    }

    @Test
    void aMareInHeatIsCovered() {
        assertEquals(StasisMating.Verdict.READY,
                StasisMating.mareVerdict(mare(), inHeat(1000L), 30.0F, 30.0F, 1000L, T));
    }

    @Test
    void aMareOutOfHeatIsNot() {
        assertEquals(StasisMating.Verdict.NOT_IN_HEAT,
                StasisMating.mareVerdict(mare(), outOfHeat(1000L), 30.0F, 30.0F, 1000L, T));
    }

    /**
     * <b>Once per heat.</b> The same {@code lastNaturalTry} stamp a stallion
     * left in a paddock sets - so a cover that did not take waits for her next
     * heat, and a bank cannot be a faster field.
     */
    @Test
    void onceAHeatAndNoMore() {
        long now = 1000L;
        Reproduction covered = inHeat(now).withNaturalTry(now);
        assertEquals(StasisMating.Verdict.COVERED_THIS_HEAT,
                StasisMating.mareVerdict(mare(), covered, 30.0F, 30.0F, now, T));

        // ...and she is available again on the next heat, with no stamp cleared.
        long nextHeat = now + T.cycleTicks();
        assertEquals(StasisMating.Verdict.READY,
                StasisMating.mareVerdict(mare(), covered, 30.0F, 30.0F, nextHeat, T));
    }

    /**
     * A mare on a shelf comes into heat on the clock, not on her ticks. Every
     * anchor in her record is an absolute game tick, so time passing is the
     * whole of what has to happen.
     */
    @Test
    void aShelvedMareComesIntoHeatOnHerOwn() {
        Reproduction quiet = outOfHeat(0L);
        boolean sawHeat = false;
        for (long now = 0L; now <= T.cycleTicks(); now += T.cycleTicks() / 16) {
            sawHeat |= StasisMating.mareVerdict(mare(), quiet, 30.0F, 30.0F, now, T)
                    == StasisMating.Verdict.READY;
        }
        assertTrue(sawHeat, "a mare left on a shelf for one whole cycle never came into heat");
    }

    /**
     * <b>The draw is the ordinary one</b>, run through {@link Conception} like
     * every other path in the mod - and a mare who is carrying is then left
     * alone, because the bank is waiting for her due date and not for her next
     * heat.
     */
    @Test
    void aBankBreedingConceivesAndThenSheIsLeftAlone() {
        long now = 1000L;
        Conception.Result took = null;
        for (int seed = 1; seed < 50 && took == null; seed++) {
            Conception.Result result = Conception.attempt(
                    StasisMating.mating(mare(), sire(), ""), inHeat(now), now, T, 0,
                    true, true, new SeededRng(seed));
            if (result.outcome() == Conception.Outcome.CONCEIVED) {
                took = result;
            }
        }
        assertTrue(took != null, "fifty tries at the peak of a heat and none of them took");

        Reproduction carrying = inHeat(now).withNaturalTry(now)
                .withPregnancy(took.pregnancy().orElseThrow());
        assertEquals(StasisMating.Verdict.PREGNANT,
                StasisMating.mareVerdict(mare(), carrying, 30.0F, 30.0F, now, T));
        // ...and still, a whole cycle later, rather than coming back into heat
        // on top of a pregnancy.
        assertEquals(StasisMating.Verdict.PREGNANT,
                StasisMating.mareVerdict(mare(), carrying, 30.0F, 30.0F, now + T.cycleTicks(), T));
    }

    /**
     * <b>Hurt horses do not breed</b>, and that is the same threshold a natural
     * cover uses - which is also why the bank's feed and water slots matter to
     * this half of the feature and not only to the healing one.
     */
    @Test
    void bothHalvesOfThePairHaveToBeWell() {
        long now = 1000L;
        float hurt = (float) (30.0 * ReproRules.COVER_HEALTH) - 1.0F;
        assertEquals(StasisMating.Verdict.TOO_HURT,
                StasisMating.mareVerdict(mare(), inHeat(now), hurt, 30.0F, now, T));
        assertFalse(StasisMating.canSire(sire(), hurt, 30.0F));
        assertTrue(StasisMating.canSire(sire(), 30.0F, 30.0F));
    }

    /** A stallion is a stallion: not a mare, not a gelding, and carrying a genome. */
    @Test
    void onlyAnEntireStallionSires() {
        assertTrue(StasisMating.canSire(sire(), 30.0F, 30.0F));
        assertFalse(StasisMating.canSire(mare(), 30.0F, 30.0F));
        assertFalse(StasisMating.canSire(sire().withGelded(true), 30.0F, 30.0F));
        assertFalse(StasisMating.canSire(null, 30.0F, 30.0F));
    }

    /** ...and a stallion put in a chamber cannot be bred as if he were the mare. */
    @Test
    void aStallionIsNeverTheMare() {
        assertEquals(StasisMating.Verdict.UNFIT,
                StasisMating.mareVerdict(sire(), inHeat(1000L), 30.0F, 30.0F, 1000L, T));
    }

    /**
     * The mating carries no gamete bias either way. A breeding carrot is armed
     * on a live horse and spent by a breeding somebody is watching; a bank
     * breeding has nobody there, so it draws like a wild cover.
     */
    @Test
    void aBankBreedingCarriesNoCarrots() {
        Conception.Mating mating = StasisMating.mating(mare(), sire(), "");
        assertSame(GameteBias.NONE, mating.damBias());
        assertSame(GameteBias.NONE, mating.sireBias());
        assertEquals(SIRE_ID, mating.sireId());
        assertEquals("Boyd", mating.sireFirstName());
        assertEquals("", mating.bredBy());
    }
}
