package com.example.horsegenetics.common.repro;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every rule a natural cover obeys, one test each - so an in-game failure can be
 * pinned on the gathering in {@code NaturalBreedingHandler} rather than on the
 * decision.
 */
class NaturalCoverTest {

    private static final ReproTiming T = ReproTiming.STANDARD;
    private static final long DAY = ReproTiming.DAY_TICKS;
    /** In heat from tick 0 to DAY. */
    private static final Reproduction IN_HEAT = new Reproduction(0.0, Optional.empty(), Reproduction.NEVER,
            List.of(), Reproduction.NEVER, Reproduction.NEVER, 0, Reproduction.NEVER);
    private static final long NOW = DAY / 4;

    private static final NaturalCover.Party MARE = new NaturalCover.Party(true, true, false, true, false, false, false);
    private static final NaturalCover.Party STALLION = new NaturalCover.Party(true, false, false, true, false, false, false);

    private static NaturalCover.Stallion near(NaturalCover.Party p) {
        return new NaturalCover.Stallion(p, 4.0, 0);
    }

    private static NaturalCover.Decision decide(NaturalCover.Party mare, List<NaturalCover.Stallion> s, int crowd) {
        return NaturalCover.decide(mare, IN_HEAT, NOW, T, s, crowd);
    }

    @Test
    void anAbleStallionBesideAMareInHeatCoversHer() {
        NaturalCover.Decision d = decide(MARE, List.of(near(STALLION)), 1);
        assertEquals(NaturalCover.Verdict.COVER, d.verdict());
        assertEquals(0, d.stallion());
    }

    @Test
    void theNearestAbleStallionIsTheOne() {
        NaturalCover.Decision d = decide(MARE, List.of(
                new NaturalCover.Stallion(STALLION, 8.0, 0),
                new NaturalCover.Stallion(STALLION, 2.0, 0),
                new NaturalCover.Stallion(STALLION, 5.0, 0)), 3);
        assertEquals(1, d.stallion());
    }

    @Test
    void onlyOncePerHeat() {
        Reproduction covered = IN_HEAT.withNaturalTry(10);
        assertEquals(NaturalCover.Verdict.NOT_NOW,
                NaturalCover.decide(MARE, covered, NOW, T, List.of(near(STALLION)), 1).verdict());
        assertEquals(NaturalCover.Verdict.COVER,
                NaturalCover.decide(MARE, covered, T.cycleTicks() + 10, T, List.of(near(STALLION)), 1).verdict(),
                "her next heat is a new try");
    }

    @Test
    void notOutOfHeat() {
        assertEquals(NaturalCover.Verdict.NOT_NOW,
                NaturalCover.decide(MARE, IN_HEAT, DAY + 10, T, List.of(near(STALLION)), 1).verdict());
    }

    @Test
    void aGeldingNeverCovers() {
        NaturalCover.Party gelding = new NaturalCover.Party(true, false, true, true, false, false, false);
        assertEquals(NaturalCover.Verdict.NO_STALLION, decide(MARE, List.of(near(gelding)), 1).verdict());
    }

    @Test
    void threeCoversADayIsAHardStop() {
        assertEquals(NaturalCover.Verdict.NO_STALLION,
                decide(MARE, List.of(new NaturalCover.Stallion(STALLION, 4.0, 3)), 1).verdict());
        assertEquals(NaturalCover.Verdict.COVER,
                decide(MARE, List.of(new NaturalCover.Stallion(STALLION, 4.0, 2)), 1).verdict());
    }

    @Test
    void exactlyThreeBlocksIsOutOfReachAsInVanilla() {
        double three = ReproRules.NATURAL_REACH * ReproRules.NATURAL_REACH;
        assertEquals(NaturalCover.Verdict.NO_STALLION,
                decide(MARE, List.of(new NaturalCover.Stallion(STALLION, three, 0)), 1).verdict());
        assertEquals(NaturalCover.Verdict.COVER,
                decide(MARE, List.of(new NaturalCover.Stallion(STALLION, three - 0.01, 0)), 1).verdict());
    }

    /** Gap 230, settled the vanilla way: three seconds in reach of the same stallion, then a cover. */
    @Test
    void aCoverWaitsForVanillasThreeSecondCourtship() {
        java.util.UUID him = new java.util.UUID(1L, 1L);
        java.util.UUID other = new java.util.UUID(2L, 2L);
        NaturalCover.Courtship c = NaturalCover.Courtship.start(him, 1000);
        assertEquals(false, c.complete(1000));
        c = c.seen(him, 1040);
        assertEquals(false, c.complete(1040), "two seconds is not enough");
        c = c.seen(him, 1080);
        assertEquals(true, c.complete(1080), "four seconds is");

        assertEquals(1080, NaturalCover.Courtship.start(him, 1000).seen(other, 1080).since(),
                "another stallion starts his own courtship");
        assertEquals(2000, NaturalCover.Courtship.start(him, 1000).seen(him, 2000).since(),
                "a pair that drifted apart starts over");
    }

    @Test
    void aCourtshipResetsAfterTheStallionIsMissingTooLong() {
        java.util.UUID him = new java.util.UUID(1L, 1L);
        NaturalCover.Courtship courtship = NaturalCover.Courtship.start(him, 1000)
                .seen(him, 1050)
                .seen(him, 1050 + NaturalCover.Courtship.MAY_LAPSE + 1);

        assertEquals(1050 + NaturalCover.Courtship.MAY_LAPSE + 1, courtship.since());
        assertEquals(courtship.since(), courtship.lastSeen());
        assertEquals(false, courtship.complete(courtship.since()),
                "a reappearing stallion must start a fresh courtship");
    }

    @Test
    void outOfReachDoesNotCount() {
        double justOut = (ReproRules.NATURAL_REACH + 0.1) * (ReproRules.NATURAL_REACH + 0.1);
        assertEquals(NaturalCover.Verdict.NO_STALLION,
                decide(MARE, List.of(new NaturalCover.Stallion(STALLION, justOut, 0)), 1).verdict());
    }

    @Test
    void hurtRiddenLeashedOrCowboyStallionsAreIgnored() {
        NaturalCover.Party[] unable = {
                new NaturalCover.Party(true, false, false, false, false, false, false),
                new NaturalCover.Party(true, false, false, true, true, false, false),
                new NaturalCover.Party(true, false, false, true, false, true, false),
                new NaturalCover.Party(true, false, false, true, false, false, true),
                new NaturalCover.Party(false, false, false, true, false, false, false)};
        for (NaturalCover.Party p : unable) {
            assertEquals(NaturalCover.Verdict.NO_STALLION, decide(MARE, List.of(near(p)), 1).verdict(), p.toString());
        }
    }

    @Test
    void aMareWhoIsHurtRiddenLeashedCowboyOrYoungIsNotCovered() {
        NaturalCover.Party[] unable = {
                new NaturalCover.Party(true, true, false, false, false, false, false),
                new NaturalCover.Party(true, true, false, true, true, false, false),
                new NaturalCover.Party(true, true, false, true, false, true, false),
                new NaturalCover.Party(true, true, false, true, false, false, true),
                new NaturalCover.Party(false, true, false, true, false, false, false)};
        for (NaturalCover.Party p : unable) {
            assertEquals(NaturalCover.Verdict.NOT_A_BREEDING_MARE, decide(p, List.of(near(STALLION)), 1).verdict(),
                    p.toString());
        }
        assertEquals(NaturalCover.Verdict.NOT_A_BREEDING_MARE, decide(STALLION, List.of(near(STALLION)), 1).verdict(),
                "a stallion is not a mare");
    }

    @Test
    void theCapCountsOtherHorsesAndStopsAtEight() {
        assertEquals(NaturalCover.Verdict.COVER,
                decide(MARE, List.of(near(STALLION)), ReproRules.NATURAL_CAP - 1).verdict());
        assertEquals(NaturalCover.Verdict.CROWDED,
                decide(MARE, List.of(near(STALLION)), ReproRules.NATURAL_CAP).verdict());
    }
}
