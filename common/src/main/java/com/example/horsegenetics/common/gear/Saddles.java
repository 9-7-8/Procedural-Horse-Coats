package com.example.horsegenetics.common.gear;

/**
 * <b>The mod's own saddle, as far as the horse is concerned</b> (#75): which
 * materials it ships in, and what it adds to the horse while somebody rides it.
 *
 * <p>The vanilla saddle item is the bridle on a horse (#73), so this saddle is a
 * second piece beside it, worn in the vanilla saddle slot. The bridle steers; the
 * saddle carries stats. The item, the slot and the recipe are the NeoForge side's.
 *
 * <p><b>The materials are the chestplate's</b> (owner, 2026-11-07): every
 * material vanilla makes a chestplate of, and every material another mod makes a
 * chestplate of, adapted in at run time. Horse armour follows the same rule
 * (#294). Every saddle is leather first: a metal saddle is a leather saddle with
 * that metal's fittings, so none is made without leather (owner).
 *
 * <p><b>What it does</b> is a share of the horse's own speed and jump, and only
 * while a player rides it ({@link #bonus}). The top tier is worth
 * {@link #DEFAULT_TOP_BONUS} (owner, 2026-10-09: 10% at the top tier, through the
 * config); every other tier is a fixed share of that ceiling ({@link Tier#share}),
 * so a server that moves the ceiling moves the whole ladder. Small on purpose:
 * how fast a horse is stays a matter of breeding.
 */
public final class Saddles {

    /** What the top tier adds to speed and to jump, as a share of the horse's own (owner: 10%). */
    public static final double DEFAULT_TOP_BONUS = 0.10;

    /**
     * The most the top tier may add, whatever a server writes. The cap is per
     * piece (owner, 2026-10-09); a quarter is already more than any one gene
     * moves a horse's speed.
     */
    public static final double MAX_TOP_BONUS = 0.25;

    /** Whether saddles do anything at all, unless a server says otherwise. */
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * The materials a saddle ships in without another mod: the seven vanilla
     * makes a chestplate of, cheapest first. A material another mod brings gets
     * a saddle of its own at run time, named {@link #idFor} its material, on the
     * rung {@link #tierOf} gives it.
     */
    public enum Tier {
        // share: of the top tier's bonus. Leather to netherite in the order of
        // their chestplates' defence, gold put above iron as the shoes have it:
        // a gold-fitted saddle is the show saddle.
        // rgb: the colour vanilla's own chestplate icon of the material averages
        // to (owner, 2026-11-07: the saddle takes its colours from the chestplate
        // it matches), measured from the 26.1.2 jar with the same linear-light
        // average compat/MaterialScan.averageColour applies to another mod's
        // chestplate. Leather's icon is undyed grey, so it is measured under
        // vanilla's default leather dye (0xA06540). Re-measure if Mojang repaints.
        LEATHER("leather", "leather", 0x5B3923, 0.25),
        COPPER("copper", "copper", 0xB06048, 0.40),
        CHAINMAIL("chainmail", "chainmail", 0x939393, 0.50),
        IRON("iron", "iron", 0x9E9E9E, 0.60),
        GOLD("gold", "golden", 0xCDBE4E, 0.70),
        DIAMOND("diamond", "diamond", 0x52BFB0, 0.85),
        NETHERITE("netherite", "netherite", 0x413A3E, 1.00);

        private final String material;
        private final String prefix;
        private final int rgb;
        private final double share;

        Tier(String material, String prefix, int rgb, double share) {
            this.material = material;
            this.prefix = prefix;
            this.rgb = rgb;
            this.share = share;
        }

        /** The bare material name another mod's ingot of the same stuff would be filed under. */
        public String material() {
            return material;
        }

        /** The item id's path. Vanilla's own spelling: "golden", not "gold". */
        public String id() {
            return prefix + SUFFIX;
        }

        /** The fittings' colour, {@code 0xRRGGBB}: its vanilla chestplate's. */
        public int rgb() {
            return rgb;
        }

        /** This tier's bonus as a share of the top tier's, 0 to 1. */
        public double share() {
            return share;
        }
    }

    private static final String SUFFIX = "_saddle";

    /**
     * A server's numbers for the family: the one switch and the ceiling. Built
     * only by {@code ServerConfig.saddleRates()}.
     */
    public static final class Rates {

        public static final Rates DEFAULT = new Rates(DEFAULT_ENABLED, DEFAULT_TOP_BONUS, DEFAULT_TOP_BONUS);

        private final boolean enabled;
        private final double topSpeed;
        private final double topJump;

        /**
         * @param topSpeed what the top tier adds to speed, as a share; unreadable is the default
         * @param topJump  likewise for jump
         */
        public Rates(boolean enabled, double topSpeed, double topJump) {
            this.enabled = enabled;
            this.topSpeed = capped(topSpeed);
            this.topJump = capped(topJump);
        }

        private static double capped(double value) {
            double v = Double.isNaN(value) ? DEFAULT_TOP_BONUS : value;
            return Math.max(0.0, Math.min(MAX_TOP_BONUS, v));
        }

        /** The family's switch. Off, a saddle is still worn and drawn, and adds nothing. */
        public boolean enabled() {
            return enabled;
        }

        /** What a saddle of this tier adds while ridden - {@link Bonus#NONE} with the family off. */
        public Bonus of(Tier tier) {
            if (!enabled || tier == null) {
                return Bonus.NONE;
            }
            double speed = topSpeed * tier.share;
            double jump = topJump * tier.share;
            return speed == 0.0 && jump == 0.0 ? Bonus.NONE : new Bonus(speed, jump);
        }
    }

    /**
     * What a saddle adds: shares of the horse's own base values, so a saddle is
     * worth the same proportion to a slow horse as to a fast one.
     *
     * @param speed added to movement speed, as a share of the base (0.1 is +10%)
     * @param jump  added to jump strength, likewise
     */
    public record Bonus(double speed, double jump) {

        public static final Bonus NONE = new Bonus(0.0, 0.0);

        public boolean isNone() {
            return speed == 0.0 && jump == 0.0;
        }
    }

    /**
     * What the saddle adds right now. <b>Only while a player rides</b> (treatment
     * #72: "stats apply only while ridden"): a saddled horse grazing in a field,
     * or carrying a villager, is the horse its genes made.
     *
     * @param tier          the saddle's rung, or {@code null} for no saddle
     * @param ridden        a player is aboard
     */
    public static Bonus bonus(Rates rates, Tier tier, boolean ridden) {
        return ridden ? rates.of(tier) : Bonus.NONE;
    }

    /**
     * The rung a material another mod brought stands on: a gem where diamond is
     * and an ingot where iron is. The same guess as its horse armour's and its
     * shoe's ({@link HorseShoes#tierOf}), and pure for the same reason.
     */
    public static Tier tierOf(boolean gem) {
        return gem ? Tier.DIAMOND : Tier.IRON;
    }

    /** The id of the saddle made of {@code material}: {@code tin_saddle} for {@code tin}. */
    public static String idFor(String material) {
        return material + SUFFIX;
    }

    /**
     * Whether a saddle of this material already ships, so another mod's ingot of
     * the same stuff gets no second one: a mod's own copper ingot would otherwise
     * register {@code copper_saddle} twice, and its gold a {@code gold_saddle}
     * beside the golden one.
     */
    public static boolean ships(String material) {
        for (Tier tier : Tier.values()) {
            if (tier.material.equals(material) || tier.prefix.equals(material)) {
                return true;
            }
        }
        return false;
    }

    private Saddles() {
    }
}
