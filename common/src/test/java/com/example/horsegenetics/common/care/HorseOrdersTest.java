package com.example.horsegenetics.common.care;

import com.example.horsegenetics.common.care.HorseOrders.Refusal;
import com.example.horsegenetics.common.care.HorseOrders.Leash;
import com.example.horsegenetics.common.care.HorseOrders.Situation;
import com.example.horsegenetics.common.care.HorseOrders.WhistleUse;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The command whistle's gate: the lead and the cart before the bond, every order against
 * every bond tier, Rejoin herd never refused; the saved name's fallback; the summary line.
 * Piece 3: the fighting-gene gate for every aggression pair, guardian and magic fighter,
 * and the leash on a combat order.
 */
class HorseOrdersTest {

    /** Free to take any order its bond allows; bred to fight, so only the bond is being tested. */
    private static Situation free(int tier) {
        return new Situation(tier, false, false, true);
    }

    private static Allele allele(Gene gene, String token) {
        return gene.alleles().stream().filter(a -> a.token().equals(token)).findFirst().orElseThrow();
    }

    private static boolean fights(Gene gene, String a, String b) {
        return HorseOrders.fighter(Genotype.wildType().with(
                new AllelePair(allele(gene, a), allele(gene, b))), null);
    }

    @Test
    void eachOrderNeedsItsBondTier() {
        for (HorseOrder order : HorseOrder.values()) {
            for (int tier = 0; tier <= 3; tier++) {
                Refusal r = HorseOrders.refusal(order, free(tier));
                if (tier >= order.bondTier()) {
                    assertNull(r, order + " at tier " + tier);
                } else {
                    assertEquals(Refusal.NOT_BONDED, r, order + " at tier " + tier);
                }
            }
        }
    }

    @Test
    void theTiersAreTheOwnersCalls() {
        // Stay and Follow need tier 1 (bond 31+); every other order tier 2 (61+); clearing none.
        assertEquals(1, HorseOrder.STAY.bondTier());
        assertEquals(1, HorseOrder.FOLLOW.bondTier());
        assertEquals(2, HorseOrder.WANDER.bondTier());
        assertEquals(0, HorseOrder.REJOIN_HERD.bondTier());
        assertEquals("Needs bond 61", HorseOrders.bondNeeded(HorseOrder.WANDER));
        assertEquals("Needs bond 31", HorseOrders.bondNeeded(HorseOrder.STAY));
    }

    @Test
    void theLeadAndTheCartWinOverTheBond() {
        // An unbonded horse on a lead says "on a lead": that is what the player can fix first.
        assertEquals(Refusal.ON_A_LEAD, HorseOrders.refusal(HorseOrder.STAY, new Situation(0, true, false, false)));
        assertEquals(Refusal.PULLING_A_CART, HorseOrders.refusal(HorseOrder.FOLLOW, new Situation(3, false, true, false)));
        assertEquals(Refusal.ON_A_LEAD, HorseOrders.refusal(HorseOrder.WANDER, new Situation(3, true, true, false)));
    }

    @Test
    void rejoinHerdIsNeverRefused() {
        assertNull(HorseOrders.refusal(HorseOrder.REJOIN_HERD, new Situation(0, true, true, false)));
    }

    @Test
    void anUnknownSavedNameLoadsAsNoOrder() {
        assertEquals(HorseOrder.STAY, HorseOrder.byName("STAY"));
        assertEquals(HorseOrder.STAY, HorseOrder.byName(" stay "));
        assertEquals(HorseOrder.REJOIN_HERD, HorseOrder.byName("HUNT_DRAGONS"));
        assertEquals(HorseOrder.REJOIN_HERD, HorseOrder.byName(""));
        assertEquals(HorseOrder.REJOIN_HERD, HorseOrder.byName(null));
    }

    @Test
    void theSpotOrdersAreAnchored() {
        assertTrue(HorseOrder.STAY.anchored());
        assertTrue(HorseOrder.HUNT_MONSTERS.anchored());
        assertTrue(HorseOrder.GUARD_HERE.anchored());
        assertTrue(HorseOrder.GRAZE_NEARBY.anchored());
        assertEquals(4, Arrays.stream(HorseOrder.values()).filter(HorseOrder::anchored).count());
        // Every order that stands still is anchored; grazing is anchored and roams.
        for (HorseOrder order : HorseOrder.values()) {
            assertTrue(!order.stands() || order.anchored(), order.name());
        }
        assertFalse(HorseOrder.GRAZE_NEARBY.stands());
        assertEquals(3, Arrays.stream(HorseOrder.values()).filter(HorseOrder::stands).count());
        assertTrue(HorseOrder.FOLLOW.follows());
        assertTrue(HorseOrder.DEFEND_ME.follows());
        assertEquals(2, Arrays.stream(HorseOrder.values()).filter(HorseOrder::follows).count());
    }

    @Test
    void onlyTheCombatOrdersNeedAFighter() {
        Situation plain = new Situation(3, false, false, false);
        for (HorseOrder order : HorseOrder.values()) {
            Refusal r = HorseOrders.refusal(order, plain);
            if (order.combat()) {
                assertEquals(Refusal.NOT_A_FIGHTER, r, order.name());
                assertEquals(2, order.bondTier(), order.name() + " is a tier-2 order (owner)");
            } else {
                assertNull(r, order.name());
            }
        }
        assertEquals(3, Arrays.stream(HorseOrder.values()).filter(HorseOrder::combat).count());
        assertTrue(HorseOrder.GUARD_HERE.combat(), "Guard here is a combat order (owner)");
    }

    @Test
    void breedingIsSaidBeforeTheBond() {
        // An unbonded plain horse: the bond would never help, so the breeding is the answer.
        assertEquals(Refusal.NOT_A_FIGHTER,
                HorseOrders.refusal(HorseOrder.HUNT_MONSTERS, new Situation(0, false, false, false)));
        // But a lead is still what the player can fix first.
        assertEquals(Refusal.ON_A_LEAD,
                HorseOrders.refusal(HorseOrder.DEFEND_ME, new Situation(0, true, false, false)));
        assertEquals(Refusal.NOT_BONDED,
                HorseOrders.refusal(HorseOrder.DEFEND_ME, new Situation(1, false, false, true)));
        assertEquals("Needs a horse bred to fight", HorseOrders.needs(HorseOrder.HUNT_MONSTERS, Refusal.NOT_A_FIGHTER));
        assertEquals("Needs bond 61", HorseOrders.needs(HorseOrder.HUNT_MONSTERS, Refusal.NOT_BONDED));
        assertEquals("Not while on a lead", HorseOrders.needs(HorseOrder.STAY, Refusal.ON_A_LEAD));
    }

    @Test
    void everyAggressionPairAgainstTheGate() {
        Gene g = Genes.AGGRESSION;
        int fighters = 0;
        for (Allele a : g.alleles()) {
            for (Allele b : g.alleles()) {
                boolean fights = fights(g, a.token(), b.token());
                // A matched pair whose target includes monsters: h, and a ("everything" -
                // owner's call). Anything else - n, a mixed pair, herds, riders, horses - not.
                boolean expected = a.equals(b) && (a.token().endsWith("h") || a.token().endsWith("a"))
                        && !a.token().equals("n");
                assertEquals(expected, fights, a.token() + "/" + b.token());
                fighters += fights ? 1 : 0;
            }
        }
        assertEquals(6, fighters, "three times of day, two targets");
        assertTrue(fights(g, "Adh", "Adh"));
        assertTrue(fights(g, "Aaa", "Aaa"));
        assertFalse(fights(g, "Adh", "Anh"), "two different alleles show neither");
        assertFalse(fights(g, "Adc", "Adc"), "a herd-chaser is not a monster-hunter");
        assertFalse(fights(g, "Aap", "Aap"), "a rider-hunter is not one either");
    }

    @Test
    void guardianAndMagicFighterAgainstTheGate() {
        assertTrue(fights(Genes.GUARDIAN, "Grd", "Grd"));
        assertFalse(fights(Genes.GUARDIAN, "Grd", "n"), "guardian is recessive");
        assertTrue(fights(Genes.MAGIC_FIGHTER, "Gld", "n"));
        assertTrue(fights(Genes.MAGIC_FIGHTER, "Gld", "Gld"));
        assertFalse(fights(Genes.MAGIC_FIGHTER, "Wmp", "n"), "a weak horse is not a fighter");
        assertFalse(fights(Genes.MAGIC_FIGHTER, "Wmp", "Wmp"));
        assertFalse(fights(Genes.MAGIC_FIGHTER, "n", "n"));
        assertFalse(HorseOrders.fighter(Genotype.wildType(), null), "a plain horse");
    }

    @Test
    void theLeashHoldsItsRadiusWithSlack() {
        Leash leash = new Leash(16, 0.3);
        double r = 16;
        double edge = r + HorseOrders.LEASH_SLACK_BLOCKS;
        // Picks only inside the radius...
        assertTrue(leash.mayEngage(r * r, 20, 20));
        assertFalse(leash.mayEngage((r + 0.1) * (r + 0.1), 20, 20));
        // ...and lets go only past the slack, of either the quarry or the horse.
        assertTrue(leash.keepsFighting((r + 2) * (r + 2), (r + 2) * (r + 2), 20, 20));
        assertFalse(leash.keepsFighting((edge + 0.1) * (edge + 0.1), 0, 20, 20));
        assertFalse(leash.keepsFighting(0, (edge + 0.1) * (edge + 0.1), 20, 20));
    }

    @Test
    void aHurtHorseBreaksOff() {
        Leash leash = new Leash(8, 0.3);
        assertFalse(leash.breaksOff(6, 20), "30% exactly still fights");
        assertTrue(leash.breaksOff(5.9, 20));
        assertFalse(leash.mayEngage(0, 5, 20), "and picks no new fight");
        assertFalse(leash.keepsFighting(0, 0, 5, 20));
        Leash toTheDeath = new Leash(8, 0.0);
        assertFalse(toTheDeath.breaksOff(0.5, 20), "0 = fight to the death");
        // A bad value is clamped, never trusted.
        assertEquals(1.0, new Leash(8, 7).breakOffHealth());
        assertEquals(1.0, new Leash(-3, 0.3).radius());
    }

    @Test
    void pieceTwoOrdersAreTierTwo() {
        // Owner: Graze nearby, Guard here and Go home are all on the wheel at bond tier 2.
        assertEquals(2, HorseOrder.GRAZE_NEARBY.bondTier());
        assertEquals(2, HorseOrder.GUARD_HERE.bondTier());
        assertEquals(2, HorseOrder.GO_HOME.bondTier());
        assertEquals(Refusal.NOT_BONDED, HorseOrders.refusal(HorseOrder.GO_HOME, free(1)));
        assertNull(HorseOrders.refusal(HorseOrder.GO_HOME, free(2)));
    }

    @Test
    void goHomeIsTheOnlyOneShot() {
        assertTrue(HorseOrder.GO_HOME.oneShot());
        assertEquals(1, Arrays.stream(HorseOrder.values()).filter(HorseOrder::oneShot).count());
        assertFalse(HorseOrder.GO_HOME.anchored(), "a one-shot holds no spot");
        assertFalse(HorseOrder.GO_HOME.combat());
    }

    @Test
    void theTetherLetsAHorseRoamThenWalksItWellIn() {
        HorseOrders.Tether tether = new HorseOrders.Tether(12);
        assertFalse(tether.strayed(12 * 12), "the edge itself is still inside");
        assertTrue(tether.strayed(12.1 * 12.1));
        // Walked back until it is at half the radius, not merely back over the edge.
        assertFalse(tether.backInside(11 * 11));
        assertFalse(tether.backInside(6.1 * 6.1));
        assertTrue(tether.backInside(6 * 6));
        // Clamped, never trusted.
        assertEquals(1.0, new HorseOrders.Tether(-4).radius());
    }

    @Test
    void theRadiiDefaultsSitInsideTheirBounds() {
        assertTrue(HorseOrders.MIN_GRAZE_RADIUS <= HorseOrders.DEFAULT_GRAZE_RADIUS
                && HorseOrders.DEFAULT_GRAZE_RADIUS <= HorseOrders.MAX_GRAZE_RADIUS);
        assertTrue(HorseOrders.MIN_GUARD_RADIUS <= HorseOrders.DEFAULT_GUARD_RADIUS
                && HorseOrders.DEFAULT_GUARD_RADIUS <= HorseOrders.MAX_GUARD_RADIUS);
        assertTrue(HorseOrders.DEFAULT_GUARD_RADIUS < HorseOrders.DEFAULT_HUNT_RADIUS,
                "Guard here is Hunt monsters with a short reach");
    }

    @Test
    void goHomesAnswersCountInTheSummary() {
        String line = HorseOrders.summary(HorseOrder.GO_HOME, Arrays.asList(
                null, Refusal.NO_HOME, Refusal.CANNOT_PAY, null));
        assertEquals("Go home: 2 obeyed; 1 no stall; 1 unpaid.", line);
        assertEquals("Bramble has no stall, and you have no holding pen.",
                HorseOrders.oneLine("Bramble", HorseOrder.GO_HOME, Refusal.NO_HOME));
    }

    @Test
    void theSummaryCountsEachReason() {
        String line = HorseOrders.summary(HorseOrder.STAY, Arrays.asList(
                null, Refusal.NOT_BONDED, null, Refusal.ON_A_LEAD, null, Refusal.NOT_BONDED));
        assertEquals("Stay: 3 obeyed; 1 on a lead; 2 not bonded enough.", line);
        assertEquals("Follow: 2 obeyed.", HorseOrders.summary(HorseOrder.FOLLOW, Arrays.asList(null, null)));
        assertEquals("No horses of yours within 16 blocks.", HorseOrders.summary(HorseOrder.STAY, List.of()));
    }

    @Test
    void oneHorseSaysWhatItDid() {
        assertEquals("Bramble stays here.", HorseOrders.oneLine("Bramble", HorseOrder.STAY, null));
        assertEquals("Bramble is not bonded enough.",
                HorseOrders.oneLine("Bramble", HorseOrder.WANDER, Refusal.NOT_BONDED));
        assertEquals("Bramble was not bred to fight.",
                HorseOrders.oneLine("Bramble", HorseOrder.HUNT_MONSTERS, Refusal.NOT_A_FIGHTER));
        for (HorseOrder order : HorseOrder.values()) {
            assertTrue(HorseOrders.oneLine("Bramble", order, null).startsWith("Bramble "), order.name());
        }
    }

    @Test
    void theWheelHoldsEveryOrderOnce() {
        assertEquals(HorseOrder.values().length, HorseOrders.wheel().size());
        assertEquals(HorseOrders.wheel().size(), HorseOrders.wheel().stream().distinct().count());
    }

    private static final UUID ME = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    /** A horse's record as the client holds it: owned by {@code owner}, or by nobody. */
    private static HorseRecord record(UUID owner) {
        return new HorseRecord(UUID.randomUUID(), "Bramble", "Test", Optional.empty(), "EeAa", "",
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0,
                Optional.empty(), false, Optional.ofNullable(owner));
    }

    /**
     * Issue #36: the plain hold on your own horse opened nothing, because the client asked
     * vanilla's owner, which it never has. The record's owner is the client's answer.
     */
    @Test
    void thePlainHoldOpensTheWheelOnYourOwnHorse() {
        assertEquals(WhistleUse.AIMED_HORSE, HorseOrders.whistleUse(false, record(ME), ME));
    }

    @Test
    void thePlainHoldLeavesEveryOtherHorseAlone() {
        assertEquals(WhistleUse.NOTHING, HorseOrders.whistleUse(false, record(STRANGER), ME), "someone else's");
        assertEquals(WhistleUse.NOTHING, HorseOrders.whistleUse(false, record(null), ME), "wild, or gone wild");
        assertEquals(WhistleUse.NOTHING, HorseOrders.whistleUse(false, null, ME), "no horse aimed, or no record yet");
        assertEquals(WhistleUse.NOTHING, HorseOrders.whistleUse(false, record(ME), null), "no player");
    }

    @Test
    void theSneakHoldIsEveryHorseWhateverIsAimed() {
        assertEquals(WhistleUse.EVERY_HORSE, HorseOrders.whistleUse(true, null, ME));
        assertEquals(WhistleUse.EVERY_HORSE, HorseOrders.whistleUse(true, record(ME), ME),
                "sneaking at your own horse still orders them all, as the tooltip says");
        assertEquals(WhistleUse.EVERY_HORSE, HorseOrders.whistleUse(true, record(STRANGER), ME));
    }
}
