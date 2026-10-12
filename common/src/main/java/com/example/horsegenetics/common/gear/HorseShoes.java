package com.example.horsegenetics.common.gear;

import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartAnchor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>Horse shoes, as far as the horse is concerned</b>: which materials a shoe
 * ships in, and what four hooves' worth of shoes draw as.
 *
 * <p>A shoe is one item to a hoof. Each of the four is its own stack in its own
 * gear slot, so a horse may be shod on one foot, or in four different metals,
 * and this never treats them as a set. The item, the slot and the tag are the
 * NeoForge side's; what is here is what a port would otherwise write twice.
 *
 * <p>What a worn shoe does is here too: each one adds its own metal's flat
 * amount to speed and jump ({@link #bonus}), summed over the four hooves, and
 * any one of them is enough to stand on powder snow ({@link #shod}). The two
 * enchantments are not built (#248).
 */
public final class HorseShoes {

    // One shoe's share, in the attribute's own units: movement speed (a horse
    // is roughly 0.11 to 0.34) and jump strength (roughly 0.4 to 1.0). Small on
    // purpose (the building session's numbers, #247): four netherite shoes are
    // about a block a second, so how fast a horse is stays a matter of breeding.
    // A server's own numbers are shoes.<metal>_speed and _jump.
    public static final double DEFAULT_COPPER_SPEED = 0.002;
    public static final double DEFAULT_COPPER_JUMP = 0.004;
    public static final double DEFAULT_IRON_SPEED = 0.003;
    public static final double DEFAULT_IRON_JUMP = 0.006;
    public static final double DEFAULT_GOLD_SPEED = 0.004;
    public static final double DEFAULT_GOLD_JUMP = 0.008;
    public static final double DEFAULT_DIAMOND_SPEED = 0.005;
    public static final double DEFAULT_DIAMOND_JUMP = 0.010;
    public static final double DEFAULT_NETHERITE_SPEED = 0.006;
    public static final double DEFAULT_NETHERITE_JUMP = 0.012;

    /** Whether shoes do anything at all, unless a server says otherwise. */
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * The most one shoe may add, whatever a server writes. The cap is per piece
     * and there is none across pieces (owner, 2026-10-09): four shoes at the
     * cap roughly double an ordinary horse, which is as far as a number that
     * sits inside the movement code should be allowed to go.
     */
    public static final double MAX_SPEED = 0.05;
    public static final double MAX_JUMP = 0.15;

    /**
     * The materials a shoe ships in without another mod (owner, 2026-10-12): the
     * five horse armour is made of. A metal another mod brings gets a shoe of its
     * own at run time, named {@link #idFor} its material.
     */
    public enum Metal {
        // Cheapest first; the metalsmith stocks them in this order, one a level.
        COPPER("copper", "copper", 0xC15A36, DEFAULT_COPPER_SPEED, DEFAULT_COPPER_JUMP),
        IRON("iron", "iron", 0xC6C6C6, DEFAULT_IRON_SPEED, DEFAULT_IRON_JUMP),
        GOLD("gold", "golden", 0xE9B115, DEFAULT_GOLD_SPEED, DEFAULT_GOLD_JUMP),
        DIAMOND("diamond", "diamond", 0x4AEDD9, DEFAULT_DIAMOND_SPEED, DEFAULT_DIAMOND_JUMP),
        NETHERITE("netherite", "netherite", 0x4D4346, DEFAULT_NETHERITE_SPEED, DEFAULT_NETHERITE_JUMP);

        private final String material;
        private final String prefix;
        private final int rgb;
        private final double speed;
        private final double jump;

        Metal(String material, String prefix, int rgb, double speed, double jump) {
            this.material = material;
            this.prefix = prefix;
            this.rgb = rgb;
            this.speed = speed;
            this.jump = jump;
        }

        /** What one shoe of this metal adds unless a server says otherwise. */
        public Bonus defaultBonus() {
            return new Bonus(speed, jump);
        }

        /** The bare material name another mod's ingot of the same stuff would be filed under. */
        public String material() {
            return material;
        }

        /** The item id's path. Vanilla's own spelling for the metal: "golden", not "gold". */
        public String id() {
            return prefix + SUFFIX;
        }

        /** The metal's colour, {@code 0xRRGGBB}. */
        public int rgb() {
            return rgb;
        }
    }

    private static final String SUFFIX = "_horse_shoe";

    /**
     * What shoes add to a horse: flat amounts, in the attributes' own units.
     *
     * @param speed added to movement speed
     * @param jump  added to jump strength
     */
    public record Bonus(double speed, double jump) {

        public static final Bonus NONE = new Bonus(0.0, 0.0);

        public boolean isNone() {
            return speed == 0.0 && jump == 0.0;
        }
    }

    /**
     * A server's numbers for the whole family: the one switch, and what one
     * shoe of each metal adds. Built only by {@code ServerConfig.shoeRates()},
     * so nothing downstream can read one key and miss the switch.
     */
    public static final class Rates {

        /** Every metal at its own default, switched on. */
        public static final Rates DEFAULT = new Rates(DEFAULT_ENABLED, null, null);

        private final boolean enabled;
        private final Bonus[] perShoe = new Bonus[Metal.values().length];

        /**
         * @param speed one number per {@link Metal}, in its order; a missing or
         *              unreadable one is that metal's default
         * @param jump  likewise
         */
        public Rates(boolean enabled, double[] speed, double[] jump) {
            this.enabled = enabled;
            for (Metal metal : Metal.values()) {
                int i = metal.ordinal();
                perShoe[i] = new Bonus(
                        capped(speed, i, metal.speed, MAX_SPEED),
                        capped(jump, i, metal.jump, MAX_JUMP));
            }
        }

        private static double capped(double[] values, int i, double fallback, double max) {
            double value = values == null || i >= values.length || Double.isNaN(values[i])
                    ? fallback : values[i];
            return Math.max(0.0, Math.min(max, value));
        }

        /** The family's switch. Off, a shoe is still worn and drawn, and does nothing. */
        public boolean enabled() {
            return enabled;
        }

        /** What one shoe of this metal adds here - {@link Bonus#NONE} with the family off. */
        public Bonus perShoe(Metal metal) {
            return enabled ? perShoe[metal.ordinal()] : Bonus.NONE;
        }
    }

    /**
     * The rung a metal another mod brought stands on: a gem where diamond is
     * and an ingot where iron is. The same guess, from the same one signal, as
     * its horse armour's ({@code ModdedArmour.protectionFor}), and pure for the
     * same reason: nothing about their item can be read when ours is registered.
     */
    public static Metal tierOf(boolean gem) {
        return gem ? Metal.DIAMOND : Metal.IRON;
    }

    /**
     * What the four hooves add together: each shoe's own metal, summed. Never a
     * set bonus - three shoes are three quarters of four, and four metals are
     * four different amounts.
     *
     * @param frontLeft the near fore shoe's rung, or {@code null} for a bare hoof;
     *                  the other three likewise
     */
    public static Bonus bonus(Rates rates, Metal frontLeft, Metal frontRight, Metal hindLeft, Metal hindRight) {
        if (!rates.enabled()) {
            return Bonus.NONE;
        }
        double speed = 0.0;
        double jump = 0.0;
        for (Metal metal : new Metal[] {frontLeft, frontRight, hindLeft, hindRight}) {
            if (metal != null) {
                Bonus one = rates.perShoe(metal);
                speed += one.speed();
                jump += one.jump();
            }
        }
        return speed == 0.0 && jump == 0.0 ? Bonus.NONE : new Bonus(speed, jump);
    }

    /**
     * Whether the horse stands on powder snow: any one shoe is enough, of any
     * metal, and a bare horse sinks. Not a bonus with a number, so a metal set
     * to zero still carries it; only the family's switch takes it away.
     *
     * @param shoes how many of the four hooves are shod
     */
    public static boolean shod(Rates rates, int shoes) {
        return rates.enabled() && shoes > 0;
    }

    private HorseShoes() {
    }

    /** The id of the shoe made of {@code material}: {@code tin_horse_shoe} for {@code tin}. */
    public static String idFor(String material) {
        return material + SUFFIX;
    }

    /**
     * Whether a shoe of this material already ships, so that another mod's ingot
     * of the same stuff must not get a second one. A mod's own copper ingot would
     * otherwise register {@code copper_horse_shoe} twice, and its gold a
     * {@code gold_horse_shoe} beside the golden one.
     */
    public static boolean ships(String material) {
        for (Metal metal : Metal.values()) {
            if (metal.material.equals(material) || metal.prefix.equals(material)) {
                return true;
            }
        }
        return false;
    }

    /** A shoe's tint from its metal's colour: opaque, so it is never the {@code 0} that means a bare hoof. */
    public static int tint(int rgb) {
        return 0xFF000000 | rgb;
    }

    /**
     * What the horse's shoes draw as: one {@link WornPart} for each shod hoof, on
     * that hoof's anchor, in {@link PartAnchor#hooves()} order.
     *
     * @param frontLeft the near fore shoe's {@link #tint}, or {@code 0} for a bare hoof;
     *                  the other three likewise
     * @return empty for an unshod horse, and then nothing is allocated
     */
    public static List<WornPart> worn(int frontLeft, int frontRight, int hindLeft, int hindRight) {
        if ((frontLeft | frontRight | hindLeft | hindRight) == 0) {
            return Collections.emptyList();
        }
        int[] tints = {frontLeft, frontRight, hindLeft, hindRight};
        List<PartAnchor> hooves = PartAnchor.hooves();
        List<WornPart> out = new ArrayList<>(tints.length);
        for (int i = 0; i < tints.length; i++) {
            if (tints[i] != 0) {
                out.add(new WornPart(WornPieces.HORSE_SHOE, AttachedPart.shoe(tints[i]), hooves.get(i)));
            }
        }
        return Collections.unmodifiableList(out);
    }
}
