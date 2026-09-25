package com.example.horsegenetics.common.log;

import com.example.horsegenetics.common.horse.StasisRescue;
import com.example.horsegenetics.common.repro.CoverNotice;
import com.example.horsegenetics.common.repro.NaturalCover;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules the Log tab depends on, each one on its own - so a wrong-looking
 * log in game can be pinned on the handlers that write it rather than on the
 * store.
 *
 * <p>Ordering, bounded retention, per-owner scoping and deduplication are all
 * here because all four fail silently: a log with the right rows in the wrong
 * order, or missing the oldest, or carrying somebody else's horse, looks
 * exactly like a log.
 */
class HorseEventLogTest {

    private static final UUID ANNA = UUID.nameUUIDFromBytes("anna".getBytes());
    private static final UUID BEN = UUID.nameUUIDFromBytes("ben".getBytes());
    private static final UUID MARE = UUID.nameUUIDFromBytes("mare".getBytes());
    private static final UUID FOAL = UUID.nameUUIDFromBytes("foal".getBytes());
    private static final UUID OTHER_FOAL = UUID.nameUUIDFromBytes("other foal".getBytes());

    private static HorseEvent birth(long at, UUID foal) {
        return HorseEvent.birth(at, foal, "Foal", "Dam x Sire");
    }

    private static HorseEvent crowded(long at) {
        return HorseEvent.cover(at, MARE, "Mare", CoverNotice.Reason.CROWDED, new NaturalCover.Crowd(12, 8));
    }

    // --- ordering ---------------------------------------------------------

    @Test
    void theNewestRowIsFirst() {
        HorseEventLog log = new HorseEventLog();
        log.add(ANNA, birth(100L, FOAL));
        log.add(ANNA, birth(200L, OTHER_FOAL));

        List<HorseEvent> rows = log.of(ANNA);
        assertEquals(2, rows.size());
        assertEquals(200L, rows.get(0).at());
        assertEquals(100L, rows.get(1).at());
    }

    /**
     * Two things on the same tick keep the order they were written in, newest
     * first - so a birth reported after the cover that caused it reads above it
     * rather than in whichever order a map happened to hold them.
     */
    @Test
    void rowsOnTheSameTickKeepTheOrderTheyArrivedIn() {
        HorseEventLog log = new HorseEventLog();
        log.add(ANNA, HorseEvent.cover(500L, MARE, "Mare", CoverNotice.Reason.CONCEIVED, null));
        log.add(ANNA, birth(500L, FOAL));

        List<HorseEvent> rows = log.of(ANNA);
        assertEquals(HorseEvent.Kind.BIRTH, rows.get(0).kind());
        assertEquals(HorseEvent.Kind.COVER, rows.get(1).kind());
    }

    // --- bounded retention -------------------------------------------------

    @Test
    void theOldestRowFallsOffAtTheCap() {
        HorseEventLog log = new HorseEventLog();
        int over = HorseEventLog.MAX_PER_OWNER + 20;
        for (int i = 0; i < over; i++) {
            // A distinct horse each time, or deduplication would do the trimming.
            log.add(ANNA, birth(i * HorseEventLog.DEDUPE_TICKS,
                    UUID.nameUUIDFromBytes(("foal " + i).getBytes())));
        }

        assertEquals(HorseEventLog.MAX_PER_OWNER, log.size(ANNA));
        List<HorseEvent> rows = log.of(ANNA);
        assertEquals((over - 1) * HorseEventLog.DEDUPE_TICKS, rows.get(0).at(),
                "the newest row must survive the cap");
        assertEquals(20L * HorseEventLog.DEDUPE_TICKS, rows.get(rows.size() - 1).at(),
                "the cap must drop from the old end, not the new one");
    }

    // --- scoping -----------------------------------------------------------

    @Test
    void oneOwnersRowsNeverReachAnother() {
        HorseEventLog log = new HorseEventLog();
        log.add(ANNA, birth(100L, FOAL));

        assertEquals(1, log.of(ANNA).size());
        assertEquals(List.of(), log.of(BEN));
        assertEquals(List.of(), log.of(null));
    }

    // --- deduplication -----------------------------------------------------

    /**
     * The case this rule exists for: a crowded paddock is re-evaluated every two
     * seconds for the whole of a mare's heat.
     */
    @Test
    void theSameRefusalTwoSecondsLaterIsNotASecondRow() {
        HorseEventLog log = new HorseEventLog();
        assertTrue(log.add(ANNA, crowded(1_000L)));
        assertFalse(log.add(ANNA, crowded(1_040L)));
        assertEquals(1, log.size(ANNA));
    }

    @Test
    void theSameRefusalAfterTheWindowIsNews() {
        HorseEventLog log = new HorseEventLog();
        log.add(ANNA, crowded(1_000L));
        assertTrue(log.add(ANNA, crowded(1_000L + HorseEventLog.DEDUPE_TICKS)));
        assertEquals(2, log.size(ANNA));
    }

    /** A different refusal for the same mare is a different thing to tell her owner. */
    @Test
    void aChangedReasonIsAlwaysItsOwnRow() {
        HorseEventLog log = new HorseEventLog();
        log.add(ANNA, crowded(1_000L));
        assertTrue(log.add(ANNA, HorseEvent.cover(1_040L, MARE, "Mare",
                CoverNotice.Reason.HURT, new NaturalCover.Crowd(1, 8))));
        assertEquals(2, log.size(ANNA));
    }

    /** Twins are two births, not one reported twice: the key is the foal. */
    @Test
    void twoFoalsInOneWindowAreTwoRows() {
        HorseEventLog log = new HorseEventLog();
        log.add(ANNA, birth(1_000L, FOAL));
        assertTrue(log.add(ANNA, birth(1_040L, OTHER_FOAL)));
        assertEquals(2, log.size(ANNA));
    }

    /** One death reported by three handlers is one death. */
    @Test
    void oneDeathReportedTwiceIsOneRow() {
        HorseEventLog log = new HorseEventLog();
        assertTrue(log.add(ANNA, HorseEvent.death(2_000L, MARE, "Mare", "a wolf")));
        assertFalse(log.add(ANNA, HorseEvent.death(2_001L, MARE, "Mare", "")));
        assertEquals(1, log.size(ANNA));
    }

    /** Two owners watching the same cover each get their own row. */
    @Test
    void deduplicationIsPerOwner() {
        HorseEventLog log = new HorseEventLog();
        assertTrue(log.add(ANNA, crowded(1_000L)));
        assertTrue(log.add(BEN, crowded(1_000L)));
        assertEquals(1, log.size(ANNA));
        assertEquals(1, log.size(BEN));
    }

    // --- filtering ---------------------------------------------------------

    @Test
    void filteringByKindKeepsNewestFirst() {
        HorseEventLog log = new HorseEventLog();
        log.add(ANNA, birth(100L, FOAL));
        log.add(ANNA, crowded(200L));
        log.add(ANNA, birth(300L, OTHER_FOAL));

        List<HorseEvent> births = log.of(ANNA, HorseEvent.Kind.BIRTH);
        assertEquals(2, births.size());
        assertEquals(300L, births.get(0).at());
        assertEquals(100L, births.get(1).at());
        assertEquals(3, log.of(ANNA, null).size(), "no kind means every kind");
    }

    // --- persistence -------------------------------------------------------

    @Test
    void aSnapshotRoundTripsWithItsOrderAndItsOwners() {
        HorseEventLog log = new HorseEventLog();
        log.add(ANNA, birth(100L, FOAL));
        log.add(BEN, HorseEvent.tamed(150L, MARE, "Mare"));
        log.add(ANNA, birth(300L, OTHER_FOAL));

        HorseEventLog reloaded = new HorseEventLog(log.snapshot());
        assertEquals(2, reloaded.size(ANNA));
        assertEquals(1, reloaded.size(BEN));
        assertEquals(300L, reloaded.of(ANNA).get(0).at());
        assertEquals(HorseEvent.Kind.TAMED, reloaded.of(BEN).get(0).kind());
    }

    @Test
    void anEmptyLogIsEmpty() {
        HorseEventLog log = new HorseEventLog();
        assertTrue(log.isEmpty());
        log.add(ANNA, birth(1L, FOAL));
        assertFalse(log.isEmpty());
    }

    // --- the row itself ----------------------------------------------------

    /**
     * A refused cover must read as the chat line the owner missed, numbers and
     * all - that is the whole reason the reason is a constant and not a string.
     */
    @Test
    void aRefusedCoverRowIsTheChatLine() {
        NaturalCover.Crowd crowd = new NaturalCover.Crowd(12, 8);
        HorseEvent row = HorseEvent.cover(1_000L, MARE, "Mare", CoverNotice.Reason.CROWDED, crowd);
        assertEquals(CoverNotice.line("Mare", CoverNotice.Reason.CROWDED, crowd), row.line());
    }

    /**
     * <b>And a rescue row must read as the chat line too</b>, for the same
     * reason and harder: an emergency chamber fires with nobody watching by
     * design, so the row is very often the <i>only</i> place the player ever
     * reads about it. The two must also stay distinguishable - which chamber
     * paid for the save is the actionable half.
     */
    @Test
    void aRescueRowIsTheChatLineAndSaysWhichChamber() {
        assertEquals(StasisRescue.saved("Mare"),
                HorseEvent.rescued(1_000L, MARE, "Mare", false).line());
        assertEquals(StasisRescue.savedInBank("Mare"),
                HorseEvent.rescued(1_000L, MARE, "Mare", true).line());
        assertTrue(HorseEvent.rescued(0L, MARE, "Mare", true).good());
        assertFalse(HorseEvent.rescued(0L, MARE, "Mare", true).bad(),
                "the horse lived - a near miss is not a loss");
    }

    @Test
    void aConceivedCoverReadsAsGoodAndADeathAsBad() {
        assertTrue(HorseEvent.cover(0L, MARE, "Mare", CoverNotice.Reason.CONCEIVED, null).good());
        assertFalse(HorseEvent.cover(0L, MARE, "Mare", CoverNotice.Reason.CROWDED, null).good());
        assertFalse(HorseEvent.cover(0L, MARE, "Mare", CoverNotice.Reason.CROWDED, null).bad(),
                "a refusal is not a loss");
        assertTrue(HorseEvent.death(0L, MARE, "Mare", "").bad());
    }

    /** Tick zero is six in the morning of day zero, and the clock follows from that. */
    @Test
    void theClockCountsFromDawn() {
        assertEquals("Day 0, 06:00", HorseEvent.when(0L));
        assertEquals("Day 0, 12:00", HorseEvent.when(6_000L));
        assertEquals("Day 0, 00:00", HorseEvent.when(18_000L));
        assertEquals("Day 1, 06:00", HorseEvent.when(24_000L));
        assertEquals("Day 3, 00:30", HorseEvent.when(3L * 24_000L + 18_500L));
    }

    /**
     * The table has a Horse column, so the cell beside it must not repeat the
     * name - but it must still be the same sentence, trimmed, and never a second
     * phrasing that can drift from {@link CoverNotice}'s.
     */
    @Test
    void theDetailCellIsTheSameSentenceWithoutTheName() {
        HorseEvent crowded = crowded(0L);
        assertEquals(CoverNotice.line("Mare", CoverNotice.Reason.CROWDED, new NaturalCover.Crowd(12, 8))
                        .substring("Mare".length()).trim(),
                crowded.detail());
        assertFalse(crowded.detail().startsWith("Mare"));
    }

    @Test
    void aSentenceThatDoesNotStartWithTheNameIsLeftWhole() {
        HorseEvent tamed = HorseEvent.tamed(0L, MARE, "Willow");
        assertEquals("You tamed Willow.", tamed.detail());

        HorseEvent didNotTake = HorseEvent.cover(0L, MARE, "Willow",
                CoverNotice.Reason.DID_NOT_TAKE, null);
        assertEquals(didNotTake.line(), didNotTake.detail());
    }

    @Test
    void anUnnamedHorseStillMakesASentence() {
        assertEquals("A horse died.", HorseEvent.death(0L, MARE, "", "").line());
    }
}
