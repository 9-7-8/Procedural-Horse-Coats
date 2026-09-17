package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.Rng;

/**
 * <b>The shape a breed's wild herds are founded in</b> - how often a new herd is
 * a band of stallions rather than a stallion with mares, and how many of each
 * either kind wants.
 *
 * <h2>What this can and cannot decide (owner, 2026-09-15)</h2>
 * A breed is chosen <b>after</b> the game has already spawned the pack: the
 * biome modifier ({@code add_horse_herds.json}) decides how many horses appear,
 * then {@code HerdManager} elects a lead, derives the breed from the lead's seed
 * and founds the herd over whatever clump is standing there. So the counts below
 * are <b>a preference applied to the horses that spawned</b>, not an order for
 * more of them:
 *
 * <ul>
 *   <li><b>Sexes are ours</b> - nothing has read them yet when founding runs, so
 *       "one stallion and three mares" is honoured exactly whenever the clump is
 *       big enough.</li>
 *   <li><b>Counts are clamped by the clump.</b> A breed asking for four mares
 *       gets two if three horses spawned. It is never padded out by spawning
 *       extra horses - that was considered and left out.</li>
 *   <li><b>Foals are vanilla's.</b> A horse that spawned as a baby stays one and
 *       takes a coin-flip sex, as it always did.</li>
 * </ul>
 *
 * <p>And it is only ever asked <b>at founding</b>. What a band becomes
 * afterwards is {@code BandLife}'s - colts leave for bachelor bands, challengers
 * take harems over, mares drift between bands - so none of this is a claim about
 * what a herd looks like an hour later.
 *
 * <h2>Single-sex herds</h2>
 * A breed that should only ever appear in stallion bands sets
 * {@code bachelor_chance} to 1. One that should never appear in one sets it to 0.
 */
public record BreedHerd(double bachelorChance, Band traditional, Band bachelor) {

    /**
     * The share of a breed's new herds founded as a bachelor band - the roll
     * {@code HerdManager} used to make as a hardcoded 3 in 10 for every breed.
     */
    public static final double DEFAULT_BACHELOR_CHANCE = 0.3;

    /**
     * Stand-in for "as many as turned up". A clump is bounded by
     * {@code HerdManager.MAX_CLUMP}, so nothing larger can ever be asked of it.
     */
    public static final int MANY = 64;

    /**
     * <b>How many mares one stallion's band will hold before the surplus is
     * something else.</b>
     *
     * <p>It was {@link #MANY}, which is to say no limit at all, and that is
     * half of why testers saw far more mares than stallions in the wild: a
     * traditional band took exactly one stallion however many horses turned up,
     * so every adult past the first was a mare and the ratio fell away with the
     * size of the pack. Four is a real harem, and past it
     * {@code Band.stallionCount} turns the surplus stallion - the precedence
     * that rule already documents.
     */
    public static final int MAX_HAREM_MARES = 4;

    /** What the game did before any breed could say otherwise. */
    public static final BreedHerd DEFAULT = new BreedHerd(DEFAULT_BACHELOR_CHANCE,
            new Band(1, 1, 0, MAX_HAREM_MARES),
            new Band(1, MANY, 0, 0));

    public BreedHerd {
        bachelorChance = Math.max(0.0, Math.min(1.0, bachelorChance));
        traditional = traditional == null ? new Band(1, 1, 0, MAX_HAREM_MARES) : traditional;
        bachelor = bachelor == null ? new Band(1, MANY, 0, 0) : bachelor;
    }

    /** The composition the named kind of band wants, before the clump clamps it. */
    public Band bandOf(boolean isBachelor) {
        return isBachelor ? bachelor : traditional;
    }

    /**
     * How many stallions and how many mares one kind of band wants, as two
     * inclusive ranges. A single number in the file is a zero-width range.
     */
    public record Band(int minStallions, int maxStallions, int minMares, int maxMares) {

        public Band {
            minStallions = Math.max(0, minStallions);
            maxStallions = Math.max(minStallions, maxStallions);
            minMares = Math.max(0, minMares);
            maxMares = Math.max(minMares, maxMares);
        }

        /**
         * How many of {@code adults} become stallions; the rest become mares.
         *
         * <p>Precedence when the clump cannot satisfy everything, which is the
         * common case rather than the exotic one: <b>the stallion range is
         * honoured first</b>, then {@code maxMares} (so a bachelor band never
         * grows a mare), and {@code minMares} last, because the only way to
         * insist on a mare that did not spawn would be to spawn one.
         *
         * <p>Pure, and deterministic for a given {@code rng} - {@code HerdManager}
         * passes one seeded from the herd's lead, so every member of the clump
         * computes the same answer and they agree on who is a stallion without
         * talking to each other.
         */
        public int stallionCount(int adults, Rng rng) {
            if (adults <= 0) {
                return 0;
            }
            int lo = Math.min(minStallions, adults);
            int hi = Math.min(maxStallions, adults);
            int stallions = lo + (hi > lo ? rng.nextInt(hi - lo + 1) : 0);

            // Too many mares left over for this kind of band: the surplus turns
            // stallion. This is what keeps a bachelor band all-male at any size.
            int fewestForMares = adults - maxMares;
            if (stallions < fewestForMares) {
                stallions = Math.min(adults, fewestForMares);
            }
            // Too few mares left: give some back, but never below the stallion
            // floor - a band of one horse cannot also hold a mare.
            int mostForMares = adults - minMares;
            if (stallions > mostForMares) {
                stallions = Math.max(lo, Math.max(0, mostForMares));
            }
            return Math.max(0, Math.min(adults, stallions));
        }

        /** Whether this is the range the game has always used for its kind of band. */
        public boolean isDefault(boolean isBachelor) {
            return equals(isBachelor ? DEFAULT.bachelor() : DEFAULT.traditional());
        }
    }
}
