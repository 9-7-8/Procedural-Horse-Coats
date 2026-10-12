package com.example.horsegenetics.common.gear;

import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Horse shoes: four hooves are four separate shoes, each drawn on its own foot
 * in its own metal, and a shipped material is never given a second shoe.
 */
class HorseShoesTest {

    private static final int IRON = HorseShoes.tint(HorseShoes.Metal.IRON.rgb());
    private static final int GOLD = HorseShoes.tint(HorseShoes.Metal.GOLD.rgb());

    @Test
    @DisplayName("an unshod horse draws nothing")
    void aBareHorseDrawsNothing() {
        assertTrue(HorseShoes.worn(0, 0, 0, 0).isEmpty());
    }

    @Test
    @DisplayName("one shoe is drawn on the hoof it is on, and on no other")
    void oneShoeIsOnItsOwnHoof() {
        PartAnchor[] hooves = {PartAnchor.HOOF_FRONT_LEFT, PartAnchor.HOOF_FRONT_RIGHT,
                PartAnchor.HOOF_HIND_LEFT, PartAnchor.HOOF_HIND_RIGHT};
        for (int hoof = 0; hoof < 4; hoof++) {
            int[] t = new int[4];
            t[hoof] = IRON;
            List<WornPart> worn = HorseShoes.worn(t[0], t[1], t[2], t[3]);
            assertEquals(1, worn.size());
            assertEquals(hooves[hoof], worn.get(0).anchor());
            assertEquals(WornPieces.HORSE_SHOE, worn.get(0).piece());
            assertEquals(PartKind.SHOE, worn.get(0).part().kind());
            assertEquals(IRON, worn.get(0).part().baseTint());
        }
    }

    @Test
    @DisplayName("four shoes in mixed metals keep their own metal, in hoof order")
    void mixedMetalsStayApart() {
        int copper = HorseShoes.tint(HorseShoes.Metal.COPPER.rgb());
        int diamond = HorseShoes.tint(HorseShoes.Metal.DIAMOND.rgb());
        List<WornPart> worn = HorseShoes.worn(IRON, GOLD, copper, diamond);
        assertEquals(PartAnchor.hooves().size(), worn.size());
        int[] want = {IRON, GOLD, copper, diamond};
        for (int i = 0; i < want.length; i++) {
            assertEquals(PartAnchor.hooves().get(i), worn.get(i).anchor());
            assertEquals(want[i], worn.get(i).part().baseTint());
        }
    }

    @Test
    @DisplayName("a black metal is still a shoe: its tint is never the bare hoof's zero")
    void aBlackShoeIsNotABareHoof() {
        int black = HorseShoes.tint(0x000000);
        assertNotEquals(0, black);
        assertEquals(1, HorseShoes.worn(0, 0, black, 0).size());
    }

    @Test
    @DisplayName("every shipped shoe has its own id, and another mod's copy of the material gets no second shoe")
    void shippedMaterialsAreNotDoubled() {
        Set<String> ids = new HashSet<>();
        for (HorseShoes.Metal metal : HorseShoes.Metal.values()) {
            assertTrue(ids.add(metal.id()), metal.id());
            assertTrue(metal.id().endsWith("_horse_shoe"), metal.id());
            assertTrue(HorseShoes.ships(metal.material()), metal.material());
            assertEquals(0, metal.rgb() >>> 24, metal.id() + " is a colour, not a tint");
        }
        // The two spellings of gold are one shoe.
        assertEquals("golden_horse_shoe", HorseShoes.Metal.GOLD.id());
        assertTrue(HorseShoes.ships("gold"));
        assertTrue(HorseShoes.ships("golden"));
        assertFalse(ids.contains(HorseShoes.idFor("gold")));
        // A metal nobody ships gets one of its own.
        assertFalse(HorseShoes.ships("tin"));
        assertEquals("tin_horse_shoe", HorseShoes.idFor("tin"));
    }

    // ---- what a worn shoe does (#247) ----

    private static final double EPS = 1e-12;

    @Test
    @DisplayName("four metals each add their own amount, and taking one off leaves the other three")
    void fourMetalsEachAddTheirOwn() {
        HorseShoes.Rates rates = HorseShoes.Rates.DEFAULT;
        HorseShoes.Metal[] on = {HorseShoes.Metal.COPPER, HorseShoes.Metal.IRON,
                HorseShoes.Metal.DIAMOND, HorseShoes.Metal.NETHERITE};
        double speed = 0.0;
        double jump = 0.0;
        for (HorseShoes.Metal metal : on) {
            speed += metal.defaultBonus().speed();
            jump += metal.defaultBonus().jump();
        }
        HorseShoes.Bonus four = HorseShoes.bonus(rates, on[0], on[1], on[2], on[3]);
        assertEquals(speed, four.speed(), EPS);
        assertEquals(jump, four.jump(), EPS);

        // Each hoof in turn goes bare: exactly that shoe's share is lost.
        for (int bare = 0; bare < on.length; bare++) {
            HorseShoes.Metal[] three = on.clone();
            three[bare] = null;
            HorseShoes.Bonus left = HorseShoes.bonus(rates, three[0], three[1], three[2], three[3]);
            assertEquals(speed - on[bare].defaultBonus().speed(), left.speed(), EPS, "hoof " + bare);
            assertEquals(jump - on[bare].defaultBonus().jump(), left.jump(), EPS, "hoof " + bare);
        }
    }

    @Test
    @DisplayName("a bare horse gets nothing, and a better metal always gives more")
    void bareIsNothingAndTheLadderClimbs() {
        assertTrue(HorseShoes.bonus(HorseShoes.Rates.DEFAULT, null, null, null, null).isNone());
        HorseShoes.Bonus below = HorseShoes.Bonus.NONE;
        for (HorseShoes.Metal metal : HorseShoes.Metal.values()) {
            HorseShoes.Bonus one = metal.defaultBonus();
            assertTrue(one.speed() > below.speed() && one.jump() > below.jump(), metal.id());
            assertTrue(one.speed() <= HorseShoes.MAX_SPEED && one.jump() <= HorseShoes.MAX_JUMP, metal.id());
            below = one;
        }
    }

    @Test
    @DisplayName("a server may set each metal's numbers, capped per shoe, and switch the family off")
    void aServerMaySetTheNumbers() {
        int n = HorseShoes.Metal.values().length;
        double[] speed = new double[n];
        double[] jump = new double[n];
        speed[HorseShoes.Metal.IRON.ordinal()] = 0.02;
        jump[HorseShoes.Metal.IRON.ordinal()] = 99.0; // over the cap
        speed[HorseShoes.Metal.GOLD.ordinal()] = -1.0; // under zero
        jump[HorseShoes.Metal.GOLD.ordinal()] = Double.NaN; // unreadable: the default
        HorseShoes.Rates rates = new HorseShoes.Rates(true, speed, jump);

        HorseShoes.Bonus iron = HorseShoes.bonus(rates, HorseShoes.Metal.IRON, null, null, HorseShoes.Metal.IRON);
        assertEquals(0.04, iron.speed(), EPS);
        assertEquals(2 * HorseShoes.MAX_JUMP, iron.jump(), EPS, "the cap is per shoe, not across shoes");
        HorseShoes.Bonus gold = rates.perShoe(HorseShoes.Metal.GOLD);
        assertEquals(0.0, gold.speed(), EPS);
        assertEquals(HorseShoes.DEFAULT_GOLD_JUMP, gold.jump(), EPS);

        HorseShoes.Rates off = new HorseShoes.Rates(false, speed, jump);
        assertTrue(HorseShoes.bonus(off, HorseShoes.Metal.IRON, HorseShoes.Metal.IRON,
                HorseShoes.Metal.IRON, HorseShoes.Metal.IRON).isNone());
        assertFalse(HorseShoes.shod(off, 4));
    }

    @Test
    @DisplayName("one shoe is enough for powder snow and none is not, even when its metal adds nothing")
    void oneShoeStandsOnPowderSnow() {
        assertFalse(HorseShoes.shod(HorseShoes.Rates.DEFAULT, 0));
        assertTrue(HorseShoes.shod(HorseShoes.Rates.DEFAULT, 1));
        int n = HorseShoes.Metal.values().length;
        HorseShoes.Rates zeroed = new HorseShoes.Rates(true, new double[n], new double[n]);
        assertTrue(HorseShoes.bonus(zeroed, HorseShoes.Metal.COPPER, null, null, null).isNone());
        assertTrue(HorseShoes.shod(zeroed, 1));
    }

    @Test
    @DisplayName("another mod's gem stands where diamond does and its ingot where iron does")
    void aModdedMetalTakesItsArmoursRung() {
        assertEquals(HorseShoes.Metal.DIAMOND, HorseShoes.tierOf(true));
        assertEquals(HorseShoes.Metal.IRON, HorseShoes.tierOf(false));
    }
}
