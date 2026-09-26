package com.example.horsegenetics.common.repro;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.GenomeSample;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The words. Mostly that the right fact reaches the right place: twins only
 * through the vet's kit, "nursing" wherever she is, a covered heat reported.
 */
class ReproTextTest {

    private static final ReproTiming T = ReproTiming.STANDARD;
    private static final long DAY = ReproTiming.DAY_TICKS;
    private static final UUID SIRE = new UUID(3L, 3L);

    private static Reproduction inHeatFromZero() {
        return new Reproduction(0.0, Optional.empty(), Reproduction.NEVER, List.of(),
                Reproduction.NEVER, Reproduction.NEVER, 0, Reproduction.NEVER);
    }

    private static Pregnancy pregnancy(int embryos, long conceived, long due) {
        GenomeSample g = GenomeSample.of(Genome.of(Genotype.wildType(), new SeededRng(5)));
        Embryo e = new Embryo(g, "", false, SIRE, "S", "S", 0, g, "");
        return new Pregnancy(embryos == 2 ? List.of(e, e) : List.of(e), conceived, due, Pregnancy.NO_LOSS);
    }

    @Test
    void durationsReadInMinutesThenHours() {
        assertEquals("about 1 min", ReproText.duration(1));
        assertEquals("about 1 min", ReproText.duration(1_200));
        assertEquals("about 20 min", ReproText.duration(DAY));
        assertEquals("about 1 hour", ReproText.duration(72_000));
        assertEquals("about 2 hours", ReproText.duration(72_001));
    }

    /**
     * <b>Every state answers, including the boring one.</b> There used to be a
     * second, shorter readout - {@code glanceLine} - that gave a state word with
     * no countdown and said nothing at all for a mare between heats. The owner
     * asked for the countdown and for "not in heat" on the Jade tooltip
     * (2026-09-25), which left it with no callers, so it and the argument behind
     * it are gone. This is what the tooltip shows now.
     */
    @Test
    void everyStateGetsALineAndABetweenHeatsMareGetsACountdown() {
        Reproduction pregnant = inHeatFromZero().withPregnancy(pregnancy(1, 0, DAY));
        assertTrue(ReproText.breedingLine(pregnant, DAY / 4, T).startsWith("Pregnant"));
        assertTrue(ReproText.breedingLine(pregnant, DAY / 4, T).contains("to go"));

        assertTrue(ReproText.breedingLine(inHeatFromZero(), 0, T).startsWith("In heat"));

        // The case the old readout was silent on, and the one a breeder is
        // actually asking about: she is not in heat, and here is how long.
        Reproduction resting = inHeatFromZero().withCyclePhase(0.5);
        String between = ReproText.breedingLine(resting, 0, T);
        assertTrue(between.startsWith("Not in heat"), between);
        assertTrue(between.contains("to go"), between);

        // Twins stay the vet kit's alone, on every state and at every moment.
        for (long now = 0; now < DAY * 3; now += DAY / 12) {
            String line = ReproText.breedingLine(inHeatFromZero(), now, T);
            assertFalse(line.toLowerCase().contains("twin"), "twins leaked at " + now + ": " + line);
        }
    }

    @Test
    void nursingRidesAlongsideWhateverElseIsTrue() {
        Reproduction nursing = inHeatFromZero().foaled(0, List.of(new UUID(9L, 9L)));
        assertTrue(nursing.lactating());
        // Just foaled, so POSTPARTUM - and still visibly nursing.
        assertTrue(ReproText.breedingLine(nursing, 1, T).endsWith("nursing"));
    }

    @Test
    void theInfoLineNeverRevealsTwinsAndTheKitAlwaysDoes() {
        Reproduction twins = inHeatFromZero().withPregnancy(pregnancy(2, 0, DAY));
        String line = ReproText.breedingLine(twins, DAY / 4, T);
        assertTrue(line.startsWith("Pregnant"), line);
        assertFalse(line.toLowerCase().contains("twin"), line);

        assertTrue(String.join(" ", ReproText.vetMare("Bess", true, twins, DAY / 4, T)).contains("twins"));
        Reproduction single = inHeatFromZero().withPregnancy(pregnancy(1, 0, DAY));
        assertTrue(String.join(" ", ReproText.vetMare("Bess", true, single, DAY / 4, T)).contains("one foal"));
    }

    @Test
    void lateInPregnancySheIsHeavyAndSlow() {
        Reproduction r = inHeatFromZero().withPregnancy(pregnancy(1, 0, 3 * DAY));
        assertFalse(ReproText.breedingLine(r, DAY, T).contains("heavy"));
        assertTrue(ReproText.breedingLine(r, 2 * DAY + 5, T).contains("heavy and slow"));
    }

    @Test
    void heatLinesSayWhichHalf() {
        Reproduction r = inHeatFromZero();
        assertEquals("In heat", ReproText.breedingLine(r, 10, T));
        assertEquals("In heat - best time now", ReproText.breedingLine(r, DAY * 3 / 4, T));
        assertTrue(ReproText.breedingLine(r, DAY + 10, T).startsWith("Not in heat - about"));
        assertTrue(ReproText.vetMare("Bess", true, r, 10, T).get(0).contains("first half"));
        assertTrue(ReproText.vetMare("Bess", true, r, DAY * 3 / 4, T).get(0).contains("better half"));
    }

    @Test
    void theKitSaysWhenAStallionHasAlreadyCoveredHerThisHeat() {
        Reproduction covered = inHeatFromZero().withNaturalTry(5);
        assertTrue(ReproText.vetMare("Bess", true, covered, DAY / 2, T).contains(
                "A stallion has already covered her this heat."));
        assertFalse(ReproText.vetMare("Bess", true, inHeatFromZero(), DAY / 2, T).contains(
                "A stallion has already covered her this heat."));
    }

    @Test
    void nursingShowsEverywhere() {
        Reproduction r = inHeatFromZero().foaled(50 * DAY, List.of(UUID.randomUUID()));
        assertTrue(ReproText.breedingLine(r, 50 * DAY + 10, T).endsWith("nursing"));
        assertTrue(ReproText.vetMare("Bess", true, r, 50 * DAY + 10, T).contains("She is nursing."));
    }

    @Test
    void youngHorsesAndGeldings() {
        assertEquals(List.of("Bess is a filly, too young to breed."),
                ReproText.vetMare("Bess", false, inHeatFromZero(), 10, T));
        assertEquals(List.of("Rook is a gelding."), ReproText.vetMale("Rook", true, true, 0));
        assertEquals(List.of("Rook is a colt, too young to breed."), ReproText.vetMale("Rook", false, false, 0));
        assertEquals(List.of("Rook is an entire stallion: 2 covers made today."),
                ReproText.vetMale("Rook", true, false, 2));
        assertEquals(List.of("Rook is an entire stallion: 1 cover made today."),
                ReproText.vetMale("Rook", true, false, 1));
    }

    /**
     * A stallion's day is not capped (owner, 2026-09-24), so the kit no longer
     * counts him against a limit - it says when he is past his free covers and
     * what that costs, and it must never read "5 of 3".
     */
    @Test
    void theKitNamesATiredStallionRatherThanCountingHimAgainstACap() {
        List<String> rested = ReproText.vetMale("Rook", true, false, ReproRules.FREE_COVERS_PER_DAY - 1);
        assertEquals(1, rested.size(), "nothing to say about a stallion with covers in hand");

        List<String> tired = ReproText.vetMale("Rook", true, false, ReproRules.FREE_COVERS_PER_DAY + 2);
        assertEquals(2, tired.size());
        assertTrue(tired.get(0).contains((ReproRules.FREE_COVERS_PER_DAY + 2) + " covers made today"));
        assertFalse(tired.get(0).contains(" of "), "no cap to count him against any more");
        assertTrue(tired.get(1).contains("halved"), tired.get(1));
    }

    @Test
    void refusalsSayHowLong() {
        Reproduction pregnant = inHeatFromZero().withPregnancy(pregnancy(1, 0, DAY));
        assertTrue(ReproText.notReceptive("Bess", pregnant, DAY / 2, T).startsWith("Bess is pregnant - about"));
        assertTrue(ReproText.notReceptive("Bess", inHeatFromZero(), DAY + 10, T)
                .startsWith("Bess is not in heat - about"));
    }
}
