package com.example.horsegenetics.common.herd;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.horse.Sex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The herd model's bookkeeping and rules: that relationships grow, fade and stay
 * bounded; that a contest is written the same way on both sides; that the ledger
 * forgets the right horse when it is full; and that the per-day rates stay true
 * however often the game scans.
 */
class HerdModelTest {

    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final long SCAN = 100;

    @Test
    void familiarityGrowsTogetherAndSaturates() {
        SocialLedger l = SocialLedger.EMPTY;
        long now = 0;
        double last = 0;
        for (int i = 0; i < 400; i++) {
            now += SCAN;
            l = l.together(B, now, SCAN, 0, 0);
            double f = l.with(B).orElseThrow().familiarity();
            assertTrue(f >= last, "familiarity never goes down while together");
            assertTrue(f <= 1.0);
            last = f;
        }
        assertTrue(last > 0.95, "a day and a half together makes a horse very familiar, got " + last);
    }

    @Test
    void familiarityFadesApartButGroomingOutlastsIt() {
        SocialLedger l = SocialLedger.EMPTY;
        long now = 0;
        for (int i = 0; i < 2000; i++) {
            now += SCAN;
            l = l.together(B, now, SCAN, 1.0, 0);
        }
        Relationship before = l.with(B).orElseThrow();
        // four days apart, decayed in scan-sized steps
        for (int i = 0; i < 4 * 240; i++) {
            now += SCAN;
            l = l.decayed(now, SCAN);
        }
        Relationship after = l.with(B).orElseThrow();
        assertTrue(after.familiarity() < before.familiarity() * 0.4, "familiarity fades apart");
        assertTrue(after.grooming() > before.grooming() * 0.7, "grooming bonds outlast it");
    }

    @Test
    void decayIsTheSameWhateverTheScanInterval() {
        // Last together well in the past, so neither run sees "together this interval".
        SocialLedger seed = SocialLedger.EMPTY.together(B, -100_000, 24_000, 1.0, 1.0);
        SocialLedger fine = seed;
        long now = 0;
        for (int i = 0; i < 240; i++) {
            now += 100;
            fine = fine.decayed(now, 100);
        }
        SocialLedger coarse = seed.decayed(24_000, 24_000);
        assertEquals(coarse.with(B).orElseThrow().familiarity(), fine.with(B).orElseThrow().familiarity(), 1e-9);
    }

    @Test
    void aContestIsWrittenAsMirrorImages() {
        SocialLedger[] r = HerdRules.settle(SocialLedger.EMPTY, A, SocialLedger.EMPTY, B, HerdRules.STAKES_SPAR, 10);
        assertTrue(r[0].rankOver(B) > 0, "the winner outranks the loser");
        assertTrue(r[1].rankOver(A) < 0, "the loser yields to the winner");
        assertEquals(r[0].rankOver(B), -r[1].rankOver(A), 1e-12);
    }

    @Test
    void rankIsBoundedAndAnEstablishedOrderTakesSeveralReversals() {
        SocialLedger me = SocialLedger.EMPTY;
        for (int i = 0; i < 50; i++) {
            me = me.contest(B, true, HerdRules.STAKES_FIGHT, i);
        }
        assertTrue(me.rankOver(B) <= 1.0 && me.rankOver(B) > 0.99);
        SocialLedger once = me.contest(B, false, HerdRules.STAKES_SPAR, 60);
        assertTrue(once.rankOver(B) > 0.5, "one lost spar does not overturn a settled order");
    }

    @Test
    void aFullLedgerForgetsTheLeastImportantHorse() {
        SocialLedger l = SocialLedger.EMPTY;
        UUID friend = new UUID(1, 0);
        l = l.together(friend, 0, 48_000, 1.0, 0);    // a strong grooming partner
        for (int i = 0; i < SocialLedger.MAX + 5; i++) {
            l = l.together(new UUID(2, i), i, 200, 0, 0);   // passing acquaintances
        }
        assertEquals(SocialLedger.MAX, l.entries().size());
        assertTrue(l.with(friend).isPresent(), "the grooming partner is never the one forgotten");
    }

    @Test
    void companionsAndRivalsNeedRealStrength() {
        SocialLedger l = SocialLedger.EMPTY.together(B, 0, 100, 0, 0);
        assertTrue(l.companions(3).isEmpty(), "a horse met once is not a companion");
        assertTrue(l.rival().isEmpty());
        l = l.together(B, 24_000, 24_000, 0, 1.0);
        assertFalse(l.companions(3).isEmpty());
        assertTrue(l.rival().isPresent());
    }

    @Test
    void perScanChancesKeepTheDailyRate() {
        // Summing expected events over a day's scans recovers the daily rate
        // for rare events, at any interval.
        for (long scan : new long[]{20, 100, 600}) {
            double p = HerdRules.perScan(HerdRules.CHALLENGES_PER_BAND_PER_DAY, scan);
            double perDay = -Math.log(1 - p) * (HerdRules.DAY_TICKS / (double) scan);
            assertEquals(HerdRules.CHALLENGES_PER_BAND_PER_DAY, perDay, 1e-9);
        }
    }

    @Test
    void coltsLeaveBeforeFillies() {
        SeededRng rng = new SeededRng(4L);
        for (int i = 0; i < 200; i++) {
            double colt = HerdRules.disperseAfterDays(Sex.MALE, rng);
            double filly = HerdRules.disperseAfterDays(Sex.FEMALE, rng);
            assertTrue(colt >= 1.0 && colt <= 3.0, "colt " + colt);
            assertTrue(filly >= 3.0 && filly <= 5.0, "filly " + filly);
        }
        assertFalse(HerdRules.dueToDisperse(0, HerdRules.GROW_UP_TICKS + 12_000, 1.0));
        assertTrue(HerdRules.dueToDisperse(0, HerdRules.GROW_UP_TICKS + 24_000, 1.0));
    }

    @Test
    void anEvenContestIsACoinAndConditionMatters() {
        HerdRules.Contestant even = new HerdRules.Contestant(20, 20, 1.0, 5, 0);
        assertEquals(0.5, HerdRules.winChance(even, even), 1e-12);
        HerdRules.Contestant tired = new HerdRules.Contestant(8, 20, 1.0, 5, 0);
        assertTrue(HerdRules.winChance(even, tired) > 0.7, "a fresh horse usually beats a wounded one");
        HerdRules.Contestant settled = new HerdRules.Contestant(20, 20, 1.0, 5, 0.8);
        assertTrue(HerdRules.winChance(settled, even) > 0.6, "the established order counts");
    }

    @Test
    void theLeadMareIsTheOldestAndNeverFlickers() {
        UUID young = new UUID(0, 5);
        UUID old = new UUID(0, 9);
        List<HerdRules.MareFacts> mares = new ArrayList<>();
        mares.add(new HerdRules.MareFacts(young, 3.0, 0.9));
        mares.add(new HerdRules.MareFacts(old, 8.0, -0.5));
        assertEquals(old, HerdRules.leadMare(mares).orElseThrow());
        // two equally old mares: the same answer in either order
        List<HerdRules.MareFacts> twins = List.of(
                new HerdRules.MareFacts(A, 6.0, 0.0), new HerdRules.MareFacts(B, 6.1, 0.0));
        assertEquals(HerdRules.leadMare(twins), HerdRules.leadMare(List.of(twins.get(1), twins.get(0))));
    }

    @Test
    void rolesReadTheFactsInTheRightOrder() {
        assertEquals(BandRole.TAMED, BandRole.of(true, true, false, true, true, false, false, false));
        assertEquals(BandRole.BAND_STALLION, BandRole.of(false, true, false, true, true, false, false, false));
        assertEquals(BandRole.BACHELOR_LEAD, BandRole.of(false, true, true, true, true, false, false, false));
        assertEquals(BandRole.LEAD_MARE, BandRole.of(false, true, false, false, false, false, true, false));
        assertEquals(BandRole.FOAL, BandRole.of(false, true, false, false, false, true, false, true));
        assertEquals(BandRole.YOUNGSTER, BandRole.of(false, true, false, false, false, false, false, true));
        assertEquals(BandRole.LONER, BandRole.of(false, false, false, false, true, false, false, false));
    }
}
