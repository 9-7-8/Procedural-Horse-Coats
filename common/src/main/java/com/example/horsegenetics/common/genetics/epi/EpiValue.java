package com.example.horsegenetics.common.genetics.epi;

/**
 * <b>One declared number</b> on one allele copy - the unit this whole package
 * exists to store literally.
 *
 * <p>It replaces a position in a gene's old draw order. Where {@code EdnrbGene}
 * once documented "{@code nextLong()}, then a {@code nextFloat()} for the face
 * strength, then one for the body cover, then..." and recovered those numbers
 * by replaying a PRNG off a stored seed, it now declares an {@link EpiSchema}
 * of these and the numbers are <b>written down</b>. A player can read a horse's
 * cover fraction, and change it.
 *
 * <h2>The three kinds, and why drift treats them differently</h2>
 * <ul>
 *   <li>{@link Kind#SCALAR} - a magnitude. "How much white", "how much bigger",
 *       "how wide the dorsal stripe". Nudging one by a hair produces a horse a
 *       hair different, so this is the kind {@link EpiDrift} moves
 *       continuously.</li>
 *   <li>{@link Kind#SEED} - the {@code long} behind a noise field. Nudging it
 *       does not nudge the splash; it replaces the splash with an unrelated
 *       one. There is no "slightly different seed", so drift never moves it by
 *       a small amount - it either leaves it alone or, rarely, re-rolls it
 *       whole.</li>
 *   <li>{@link Kind#CATEGORY} - an index into a list the gene owns (a particle
 *       body site, an iris wedge). Unordered, so "one step up" is meaningless
 *       and a boundary nudge would silently move a horse's particles from its
 *       mane to its tail. Treated like a seed: rare wholesale re-pick.</li>
 * </ul>
 *
 * <h2>Two ranges, and they are not the same range</h2>
 * {@link #min()}..{@link #max()} is the <b>design range</b> - where founders are
 * rolled and, crucially, the span drift measures itself against
 * ({@link EpiDrift} moves a value by a fraction of it). {@link #clampLo()}..
 * {@link #clampHi()} is the <b>hard safety bound</b>, and it is deliberately
 * much wider: a lineage bred for two hundred generations is <i>supposed</i> to
 * be able to leave the range wild horses are born in. The clamp is there so a
 * horse cannot become large enough to break the model or fling its rider out of
 * the world, not to enforce the gene author's taste.
 *
 * <p>Colours are three {@code SCALAR}s named {@code <prefix>_r} / {@code _g} /
 * {@code _b} over {@code 0..255} rather than one packed int, so a lineage's
 * colour can drift - see {@link #colour}.
 */
public record EpiValue(String name, Kind kind, double min, double max,
                       Dist dist, int arity, double clampLo, double clampHi) {

    /** How much wider than the design range a value may drift before the hard clamp bites. */
    private static final double CLAMP_SPANS = 8.0;

    /** The number of legs a per-leg value carries, in {@code CoatRegions.LEGS} order. */
    public static final int LEGS = 4;

    public enum Kind { SCALAR, SEED, CATEGORY }

    /**
     * How a <b>founder's</b> value is rolled. Genes were deliberately skewed -
     * ednrb's cover is a power curve so most splash horses carry a little white
     * and a few carry a lot; the magical stat deltas are Gaussian - and
     * flattening all of that to uniform would change the character of the wild
     * population on every one of them. So the distribution is declared here and
     * {@link EpiRoll} honours it.
     */
    public sealed interface Dist {

        /** Flat across the design range. */
        record Uniform() implements Dist {}

        /**
         * A bounded normal draw, floored. {@code floor} is also the value's hard
         * lower clamp - a {@code Big} allele that drifted to a negative
         * percentage would be a big allele making a horse smaller, which is not
         * a distribution tail, it is a bug in the reader's head.
         */
        record Gaussian(double mean, double sigma, double floor) implements Dist {}

        /** {@code min + range * pow(u, gamma)} - biased low for {@code gamma > 1}. */
        record Power(double gamma) implements Dist {}

        /**
         * One channel of a colour that must be <b>bright</b> when it is first
         * rolled. The three channels of a colour are stored and drifted
         * independently - the owner's call, so a lineage's colour can wander -
         * but a founder's has to come out of the hue circle at a held-up
         * saturation and value, because a magical mane that rolled a muddy
         * olive reads as a bug rather than as variety.
         *
         * <p>So {@link EpiRoll} rolls the three <i>together</i>: component 0
         * draws hue, saturation and value, converts once, and components 1 and
         * 2 read the green and blue out of that same colour. This is the only
         * place a value's roll depends on its neighbours, which is why the
         * three must be declared adjacent and in order.
         */
        record Bright(int component, double satMin, double satMax,
                      double valMin, double valMax) implements Dist {}
    }

    public EpiValue {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("an epigenetic value needs a name");
        }
        if (arity < 1) {
            throw new IllegalArgumentException(name + ": arity must be >= 1, got " + arity);
        }
        if (max < min) {
            throw new IllegalArgumentException(name + ": max < min");
        }
    }

    // ------------------------------------------------------------------
    // Factories - what a gene's schema is written with
    // ------------------------------------------------------------------

    /** A magnitude, drawn flat across {@code [min, max]}. */
    public static EpiValue uniform(String name, double min, double max) {
        return scalar(name, min, max, new Dist.Uniform(), 1);
    }

    /**
     * A magnitude drawn {@code min + (max-min) * pow(u, gamma)} - the shape
     * ednrb's cover uses to keep most splashes small.
     */
    public static EpiValue power(String name, double min, double max, double gamma) {
        return scalar(name, min, max, new Dist.Power(gamma), 1);
    }

    /**
     * A magnitude drawn {@code mean + gaussian * sigma}, floored at
     * {@code floor}. The design range is {@code mean +/- 6 sigma}, because
     * {@link com.example.horsegenetics.common.Rng#nextGaussian()} is bounded
     * there by construction - so this really is the span a founder can occupy,
     * not a nominal one.
     */
    public static EpiValue gaussian(String name, double mean, double sigma, double floor) {
        double lo = Math.max(floor, mean - 6 * sigma);
        double hi = mean + 6 * sigma;
        double span = hi - lo;
        // The floor is a hard clamp, not just a founder-time one: drift must not
        // walk the value through zero and flip what the allele means.
        return new EpiValue(name, Kind.SCALAR, lo, hi, new Dist.Gaussian(mean, sigma, floor),
                1, floor, hi + CLAMP_SPANS * span);
    }

    /** Four magnitudes, one per leg, each drawn independently across {@code [min, max]}. */
    public static EpiValue perLeg(String name, double min, double max) {
        return scalar(name, min, max, new Dist.Uniform(), LEGS);
    }

    /** The {@code long} behind a noise field. Opaque by nature - see the class doc. */
    public static EpiValue seed(String name) {
        return new EpiValue(name, Kind.SEED, 0, 0, new Dist.Uniform(), 1, 0, 0);
    }

    /** An index into a list of {@code count} things the gene owns. */
    public static EpiValue category(String name, int count) {
        if (count < 1) {
            throw new IllegalArgumentException(name + ": a category needs at least one option");
        }
        return new EpiValue(name, Kind.CATEGORY, 0, count, new Dist.Uniform(), 1, 0, count);
    }

    /** One channel of a colour: {@code 0..255}, drifting freely inside it. */
    public static EpiValue channel(String name) {
        return new EpiValue(name, Kind.SCALAR, 0, 255, new Dist.Uniform(), 1, 0, 255);
    }

    /**
     * A colour, as the three channels {@code <prefix>_r}, {@code _g} and
     * {@code _b} - which is how {@link EpiValues#rgb} reads it back. Founders
     * roll it bright (see {@link Dist.Bright}); after that each channel drifts
     * on its own, so a line's colour shifts gradually rather than being
     * re-invented.
     *
     * <p>The three <b>must</b> stay adjacent and in this order - see
     * {@link Dist.Bright}.
     */
    public static EpiValue[] colour(String prefix, double satMin, double satMax,
                                    double valMin, double valMax) {
        return new EpiValue[] {
                brightChannel(prefix + "_r", 0, satMin, satMax, valMin, valMax),
                brightChannel(prefix + "_g", 1, satMin, satMax, valMin, valMax),
                brightChannel(prefix + "_b", 2, satMin, satMax, valMin, valMax),
        };
    }

    private static EpiValue brightChannel(String name, int component, double satMin, double satMax,
                                          double valMin, double valMax) {
        return new EpiValue(name, Kind.SCALAR, 0, 255,
                new Dist.Bright(component, satMin, satMax, valMin, valMax), 1, 0, 255);
    }

    private static EpiValue scalar(String name, double min, double max, Dist dist, int arity) {
        double span = max - min;
        return new EpiValue(name, Kind.SCALAR, min, max, dist, arity,
                min - CLAMP_SPANS * span, max + CLAMP_SPANS * span);
    }

    /** This value with an explicit hard safety bound, replacing the derived one. */
    public EpiValue clampedTo(double lo, double hi) {
        return new EpiValue(name, kind, min, max, dist, arity, lo, hi);
    }

    /** This value, but four of it - one per leg. */
    public EpiValue overLegs() {
        return new EpiValue(name, kind, min, max, dist, LEGS, clampLo, clampHi);
    }

    // ------------------------------------------------------------------

    /** The span drift measures itself against - see the class doc. */
    public double designSpan() {
        return max - min;
    }

    /**
     * What a horse with <b>no epigenome</b> reports - the honest middle of the
     * design range, so a question asked about a genotype is answered about the
     * genotype rather than about a horse nobody owns. This is what
     * {@code MidpointRng} used to do by returning 0.5 from every draw.
     */
    public double midpoint() {
        if (kind == Kind.CATEGORY) {
            return Math.floor(max / 2);
        }
        if (dist instanceof Dist.Gaussian g) {
            // A zero-sigma draw lands on the mean, which is the distribution's
            // actual centre - not the midpoint of the +/-6 sigma design range.
            return Math.max(g.floor(), g.mean());
        }
        return min + (max - min) / 2.0;
    }

    /** Hold {@code v} inside the hard safety bound. Never applies the design range. */
    public double clamp(double v) {
        if (kind != Kind.SCALAR) {
            return v;
        }
        return v < clampLo ? clampLo : (v > clampHi ? clampHi : v);
    }
}
