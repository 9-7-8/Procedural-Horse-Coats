package com.example.horsegenetics.common.cart;

import com.example.horsegenetics.common.trait.HorseTraits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The draught model's claims, as assertions rather than prose.
 *
 * <p>The three that matter are {@link #balancedHorseBeatsEitherSpecialist()},
 * {@link #pullSetsACeilingSpeedCannotPass()} and
 * {@link #neitherStatIsADumpStat()} - between them they pin the entire reason
 * the model is shaped the way it is. If a future simplification of
 * {@link CartDraft#retention} keeps the first two and breaks the third, it has
 * turned speed back into a dump stat on draft horses, which is the exact thing
 * this was built to avoid.
 */
class CartDraftTest {

    private static final double EPS = 5.0e-4;

    /** The table in {@link CartDraft}'s class javadoc, checked rather than trusted. */
    @Test
    @DisplayName("the documented wagon table is what the code actually produces")
    void documentedWagonTableHolds() {
        final double wagon = CartKind.WAGON.load();
        assertEquals(0.430, CartDraft.retention(3, 0.30, wagon), EPS, "fast and weak, retention");
        assertEquals(0.129, CartDraft.haul(3, 0.30, wagon), EPS, "fast and weak, haul");
        assertEquals(0.808, CartDraft.retention(9, 0.13, wagon), EPS, "slow and strong, retention");
        assertEquals(0.105, CartDraft.haul(9, 0.13, wagon), EPS, "slow and strong, haul");
        assertEquals(0.670, CartDraft.retention(7, 0.22, wagon), EPS, "balanced, retention");
        assertEquals(0.147, CartDraft.haul(7, 0.22, wagon), EPS, "balanced, haul");
    }

    @Test
    @DisplayName("a balanced horse out-hauls both the fast-weak and the slow-strong one")
    void balancedHorseBeatsEitherSpecialist() {
        final double wagon = CartKind.WAGON.load();
        final double fastWeak = CartDraft.haul(3, 0.30, wagon);
        final double slowStrong = CartDraft.haul(9, 0.13, wagon);
        final double balanced = CartDraft.haul(7, 0.22, wagon);
        assertTrue(balanced > fastWeak, balanced + " should beat fast-and-weak " + fastWeak);
        assertTrue(balanced > slowStrong, balanced + " should beat slow-and-strong " + slowStrong);
    }

    @Test
    @DisplayName("pull sets a ceiling raw speed cannot pass")
    void pullSetsACeilingSpeedCannotPass() {
        final double wagon = CartKind.WAGON.load();
        for (final double pull : new double[]{1, 3, 5, 7, 10}) {
            final double ceiling = CartDraft.ceiling(pull, wagon);
            // Absurd speeds, far past anything a horse can be bred to.
            for (final double speed : new double[]{0.3375, 1.0, 10.0, 1000.0}) {
                assertTrue(CartDraft.haul(pull, speed, wagon) <= ceiling + EPS,
                        "pull " + pull + " at speed " + speed + " passed its ceiling " + ceiling);
            }
            // ...and it is actually approached, not just never reached.
            assertEquals(ceiling, CartDraft.haul(pull, 1.0e6, wagon), ceiling * 1.0e-3,
                    "pull " + pull + " never approaches its ceiling");
        }
    }

    @Test
    @DisplayName("neither stat is a dump stat: raising either one always hauls faster")
    void neitherStatIsADumpStat() {
        for (final CartKind kind : CartKind.values()) {
            final double load = kind.load();
            // More pull, same speed.
            double previous = -1.0;
            for (double pull = 1.0; pull <= 10.0; pull += 0.5) {
                final double haul = CartDraft.haul(pull, HorseTraits.BASE_SPEED, load);
                assertTrue(haul > previous, kind + ": pull " + pull + " did not beat the point below it");
                previous = haul;
            }
            // More speed, same pull. The ceiling makes the gains shrink, but
            // they must never stop or reverse.
            previous = -1.0;
            for (double speed = 0.05; speed <= 0.40; speed += 0.025) {
                final double haul = CartDraft.haul(HorseTraits.BASE_PULL, speed, load);
                assertTrue(haul > previous, kind + ": speed " + speed + " did not beat the speed below it");
                previous = haul;
            }
        }
    }

    @Test
    @DisplayName("an unhitched horse is untouched, and every cart is a penalty")
    void unhitchedIsUntouchedAndEveryCartCosts() {
        assertEquals(1.0, CartDraft.retention(5, 0.1875, 0.0), 0.0, "no load must mean no change");
        assertEquals(0.0, CartDraft.speedModifier(5, 0.1875, 0.0), 0.0, "no load must mean no modifier");
        for (final CartKind kind : CartKind.values()) {
            final double modifier = CartDraft.speedModifier(5, HorseTraits.BASE_SPEED, kind.load());
            assertTrue(modifier < 0.0, kind + " produced a speed bonus: " + modifier);
            assertTrue(modifier > -1.0, kind + " would stop a horse dead: " + modifier);
        }
    }

    @Test
    @DisplayName("the heaviest cart is the slowest, all the way down the ladder")
    void loadLadderIsMonotonic() {
        final CartKind[] kinds = CartKind.values();
        for (int i = 1; i < kinds.length; i++) {
            assertTrue(kinds[i - 1].load() > kinds[i].load(),
                    kinds[i - 1] + " should be heavier than " + kinds[i]);
            assertTrue(CartDraft.haul(5, 0.1875, kinds[i - 1].load())
                            < CartDraft.haul(5, 0.1875, kinds[i].load()),
                    kinds[i - 1] + " should be slower to pull than " + kinds[i]);
        }
    }

    @Test
    @DisplayName("nothing ever pins a horse in place, even at a pull score of zero")
    void nothingIsEverPinned() {
        // A pull score below the trait system's own floor, on the heaviest
        // cart: the worst case the model can be handed. It must still move,
        // and it must still respect its ceiling - the two properties that were
        // in conflict while retention had a flat floor of its own.
        final double wagon = CartKind.WAGON.load();
        assertTrue(CartDraft.retention(0.0, 0.1875, wagon) > 0.0, "a cart must always move");
        assertTrue(CartDraft.haul(0.0, 0.1875, wagon) > 0.0, "a cart must always move");
        assertEquals(CartDraft.capacity(HorseTraits.MIN_PULL), CartDraft.capacity(0.0), 0.0,
                "a score under MIN_PULL must clamp to it, not to zero");
        assertTrue(CartDraft.haul(0.0, 1000.0, wagon) <= CartDraft.ceiling(0.0, wagon) + EPS,
                "even the worst horse obeys its ceiling");
    }

    @Test
    @DisplayName("an empty vehicle is its bare load, and a full one is heavier")
    void cargoAddsLoadAndNothingElseDoes() {
        for (final CartKind kind : CartKind.values()) {
            final double empty = CartDraft.loaded(kind.load(), kind.cargoShare(), 0.0);
            final double full = CartDraft.loaded(kind.load(), kind.cargoShare(), 1.0);
            assertEquals(kind.load(), empty, 0.0, kind + ": an empty one must be the bare load");
            assertTrue(full >= empty, kind + ": filling one must never make it lighter");
            if (kind.cargoShare() > 0.0) {
                assertTrue(full > empty, kind + " carries cargo and should feel it");
                // Half full is half the penalty - the property that lets a
                // player reason about loading without reading the source.
                assertEquals(0.5 * (empty + full), CartDraft.loaded(kind.load(), kind.cargoShare(), 0.5),
                        EPS, kind + ": the cargo term must stay linear");
            } else {
                assertEquals(empty, full, 0.0, kind + " has no cargo and must not gain load from one");
            }
        }
    }

    @Test
    @DisplayName("a fill outside 0..1 is clamped rather than believed")
    void fillIsClamped() {
        final double load = CartKind.SUPPLY_CART.load();
        final double share = CartKind.SUPPLY_CART.cargoShare();
        assertEquals(load, CartDraft.loaded(load, share, -5.0), 0.0, "a negative fill must not discount the cart");
        assertEquals(CartDraft.loaded(load, share, 1.0), CartDraft.loaded(load, share, 9.0), 0.0,
                "an over-full cart must cap, not grow without limit");
    }

    @Test
    @DisplayName("loading a cart costs speed, and the loaded cart still obeys its ceiling")
    void aLoadedCartIsSlowerButStillBounded() {
        final double empty = CartDraft.loaded(CartKind.SUPPLY_CART.load(), CartKind.SUPPLY_CART.cargoShare(), 0.0);
        final double full = CartDraft.loaded(CartKind.SUPPLY_CART.load(), CartKind.SUPPLY_CART.cargoShare(), 1.0);
        final double pull = HorseTraits.BASE_PULL;
        assertTrue(CartDraft.haul(pull, HorseTraits.BASE_SPEED, full)
                        < CartDraft.haul(pull, HorseTraits.BASE_SPEED, empty),
                "a full supply cart should be slower than an empty one");
        assertTrue(CartDraft.ceiling(pull, full) < CartDraft.ceiling(pull, empty),
                "loading a cart should lower the ceiling, not only the speed");
        assertTrue(CartDraft.haul(pull, 1000.0, full) <= CartDraft.ceiling(pull, full) + EPS,
                "a loaded cart must keep its ceiling");
        // A strong horse must be able to buy the cargo back: the point of the
        // whole feature is that pulling ability is worth breeding for a full cart.
        assertTrue(CartDraft.haul(9.0, HorseTraits.BASE_SPEED, full)
                        > CartDraft.haul(HorseTraits.BASE_PULL, HorseTraits.BASE_SPEED, empty),
                "a draft horse with a full cart should beat an ordinary horse with an empty one");
    }

    @Test
    @DisplayName("an ordinary horse does exactly one unit of machine work")
    void ordinaryHorseIsTheMachineBaseline() {
        assertEquals(1.0, CartDraft.workRate(HorseTraits.BASE_PULL, HorseTraits.BASE_SPEED), EPS);
    }

    @Test
    @DisplayName("machine work leans on pull but still pays for speed")
    void machineWorkLeansOnPullWithoutIgnoringSpeed() {
        final double draft = CartDraft.workRate(9, 0.13);
        final double racer = CartDraft.workRate(3, 0.30);
        final double ordinary = CartDraft.workRate(HorseTraits.BASE_PULL, HorseTraits.BASE_SPEED);
        assertTrue(draft > ordinary, "a draft horse should out-work an ordinary one: " + draft);
        assertTrue(draft > racer, "a draft horse should out-work a racer at a mill: " + draft + " vs " + racer);
        // ...but speed is still worth having, at equal pull.
        assertTrue(CartDraft.workRate(9, 0.30) > draft, "a fast draft horse must beat a slow one");
        assertTrue(CartDraft.workRate(3, 0.30) > CartDraft.workRate(3, 0.13), "speed must pay at low pull too");
    }

    @Test
    @DisplayName("the second rider arrives at six and not before")
    void secondRiderThreshold() {
        assertFalse(CartDraft.carriesTwoRiders(HorseTraits.BASE_PULL), "an ordinary horse carries one");
        assertFalse(CartDraft.carriesTwoRiders(5.99));
        assertTrue(CartDraft.carriesTwoRiders(6.0));
        assertTrue(CartDraft.carriesTwoRiders(10.0));
    }
}
