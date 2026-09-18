package com.example.horsegenetics.common.cart;

import com.example.horsegenetics.common.trait.HorseTraits;

/**
 * <b>How fast a horse pulls a thing.</b> The one place the pulling-ability
 * score becomes a number the game can use, and the first consumer
 * {@link com.example.horsegenetics.common.trait.Traits#pull()} has ever had.
 *
 * <h2>The rule, in one line</h2>
 * <b>Pull sets the ceiling; speed decides how close you get to it.</b>
 *
 * <p>A hitched horse has a <i>capacity</i> (what its pulling ability supplies)
 * and the load has a <i>demand</i> (what the vehicle asks for, which grows with
 * how fast you are trying to drag it). The fraction of its own speed the horse
 * keeps is
 *
 * <pre>{@code   retention = 1 - demand / (capacity + demand)  =  capacity / (capacity + demand)}</pre>
 *
 * and because demand is proportional to speed, the resulting ground speed
 *
 * <pre>{@code   haul = speed * capacity / (capacity + load * speed / BASE_SPEED)}</pre>
 *
 * approaches <b>{@code BASE_SPEED * capacity / load}</b> as speed rises and
 * never passes it. That asymptote is the design: a weak horse hitched to a
 * wagon is capped no matter how fast it is, and a strong horse that is slow
 * never reaches the ceiling its shoulders could carry.
 *
 * <h2>Why both stats, and not just pull</h2>
 * The brief for this model was explicit: speed must not become a dump stat on
 * draft breeds. A model that read pull alone would do exactly that - breed the
 * pulling gene, ignore everything else, and the fastest carthorse in the world
 * is the one with the highest pull score and a speed of nearly nothing. Here
 * the two multiply into the answer and <b>a balanced horse beats either
 * specialist</b>:
 *
 * <table border="1">
 * <caption>Ground speed pulling a wagon ({@code load = 0.55})</caption>
 * <tr><th>horse</th><th>speed</th><th>pull</th><th>retention</th><th>haul</th></tr>
 * <tr><td>fast and weak</td><td>0.30</td><td>3</td><td>0.43</td><td>0.129</td></tr>
 * <tr><td>slow and strong</td><td>0.13</td><td>9</td><td>0.81</td><td>0.105</td></tr>
 * <tr><td>balanced</td><td>0.22</td><td>7</td><td>0.67</td><td>0.147</td></tr>
 * </table>
 *
 * <h2>The exponents</h2>
 * Capacity is {@code (pull / BASE_PULL) ^ 0.8} rather than linear, so the top
 * of the 1-10 score is worth reaching but the last point is worth less than the
 * first - the usual shape for a stat a player grinds towards. Demand is linear
 * in speed, which is what produces the clean asymptote above; making it a power
 * too was tried and only smeared the ceiling without changing any decision a
 * player makes.
 *
 * <h2>Why there is no floor on retention</h2>
 * There was one, briefly - a flat "never below a quarter speed", so that a
 * hopeless horse still visibly moved. It had to go, because a floor on
 * retention destroys the ceiling: past the speed at which the floor engages,
 * haul becomes {@code speed * 0.25} and climbs without limit, so a fast enough
 * weak horse out-pulls its own capacity and pull is a dump stat again at
 * exactly the margin the model exists to defend. {@link CartDraftTest} caught
 * it on the first run.
 *
 * <p>The floor's actual job - never pin a horse in place - is done instead by
 * clamping the <i>score</i> at {@link HorseTraits#MIN_PULL} before the curve,
 * which is the same floor the trait system already applies. Capacity is then
 * always positive, retention is always above zero, and the asymptote is exact
 * for every speed.
 *
 * <h2>Purity</h2>
 * No RNG, no entity, no Minecraft. Everything here is a function of two doubles
 * and a load, so the numbers in the table above are a unit test rather than a
 * claim - see {@code CartDraftTest}.
 */
public final class CartDraft {

    /**
     * Curve on the pulling score. Below one, so points get cheaper in effect as
     * they get more expensive to breed.
     */
    public static final double PULL_EXPONENT = 0.8;

    /**
     * How the pull score is weighted against speed when a horse turns a machine
     * rather than pulls a cart. {@code 0.6} to pull, the remainder to speed -
     * see {@link #workRate}.
     */
    public static final double WORK_PULL_WEIGHT = 0.6;

    private CartDraft() {
    }

    /**
     * What this horse's shoulders supply, as a multiple of what an ordinary
     * horse supplies. {@code 1.0} at {@link HorseTraits#BASE_PULL}.
     */
    public static double capacity(final double pull) {
        // Clamped at the trait system's own floor rather than at zero: a
        // capacity of zero is a horse that cannot move a cart at all, which is
        // a soft-lock rather than a punishment. See the class note.
        final double score = Math.max(pull, HorseTraits.MIN_PULL);
        return Math.pow(score / HorseTraits.BASE_PULL, PULL_EXPONENT);
    }

    /**
     * What the vehicle asks for at this speed, on the same scale as
     * {@link #capacity}. Linear in speed: dragging a wagon twice as fast is
     * twice the work.
     */
    public static double demand(final double speed, final double load) {
        if (load <= 0.0) {
            return 0.0;
        }
        return load * (Math.max(speed, 0.0) / HorseTraits.BASE_SPEED);
    }

    /**
     * The fraction of its own speed a horse keeps while hitched: {@code 1.0}
     * for an unhitched horse, and always strictly above zero for a hitched one.
     */
    public static double retention(final double pull, final double speed, final double load) {
        final double d = demand(speed, load);
        if (d <= 0.0) {
            return 1.0;
        }
        final double c = capacity(pull);
        return c / (c + d);
    }

    /**
     * The value to hand a multiplicative movement-speed modifier: negative, and
     * {@code 0.0} for an unhitched horse.
     *
     * <p>Named for the attribute operation it feeds rather than for the maths,
     * because getting the sign wrong here makes a cart <i>faster</i> and that
     * reads as a feature until someone measures it.
     */
    public static double speedModifier(final double pull, final double speed, final double load) {
        return retention(pull, speed, load) - 1.0;
    }

    /**
     * Ground speed while hitched, in movement-speed attribute units - the
     * number the table in the class note is built from, and the one to compare
     * two horses with.
     */
    public static double haul(final double pull, final double speed, final double load) {
        return Math.max(speed, 0.0) * retention(pull, speed, load);
    }

    /**
     * The speed this horse can never exceed on this load, however fast it is
     * bred: {@code BASE_SPEED * capacity / load}.
     *
     * <p>Exposed because it is the single most useful thing to show a player
     * about a draft horse, and because a test that pins the asymptote is what
     * stops someone "simplifying" {@link #retention} into something that has no
     * ceiling at all.
     */
    public static double ceiling(final double pull, final double load) {
        if (load <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        return HorseTraits.BASE_SPEED * capacity(pull) / load;
    }

    /**
     * <b>How hard this horse drives a machine</b>, as a multiple of an ordinary
     * horse: {@code 1.0} at {@link HorseTraits#BASE_PULL} and
     * {@link HorseTraits#BASE_SPEED}.
     *
     * <p>A treadmill is not a cart. The horse is not trying to get anywhere, so
     * there is no asymptote to reach and no retention to lose - the two stats
     * simply combine into a rate. That makes it a weighted geometric blend,
     * {@link #WORK_PULL_WEIGHT} of it pull and the rest speed: torque matters
     * more than pace at a mill, but a quick draft horse still beats a
     * ponderous one, which is the same anti-dump-stat rule the cart model
     * follows.
     *
     * <p>This is what the Horse Powered compatibility layer reads. It is
     * deliberately not clamped: a mod deciding what to do with a horse that is
     * three times an ordinary one knows its own machine better than this does.
     */
    public static double workRate(final double pull, final double speed) {
        final double c = capacity(pull);
        final double s = Math.max(speed, 0.0) / HorseTraits.BASE_SPEED;
        return Math.pow(c, WORK_PULL_WEIGHT) * Math.pow(s, 1.0 - WORK_PULL_WEIGHT);
    }

    /**
     * Whether a horse this strong can carry a second rider.
     *
     * <p>Six on the 1-10 score - one point above ordinary. Low enough that a
     * modestly bred horse gets there and the feature is reachable without a
     * draft breed, high enough that a wild-caught horse usually will not.
     */
    public static boolean carriesTwoRiders(final double pull) {
        return pull >= TWO_RIDER_PULL;
    }

    /** The pulling score at which a horse's back takes a second rider. */
    public static final double TWO_RIDER_PULL = 6.0;
}
