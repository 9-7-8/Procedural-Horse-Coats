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
 * <p>The stat bonus, powder snow and the enchantments are not built (#164).
 */
public final class HorseShoes {

    /**
     * The materials a shoe ships in without another mod (owner, 2026-10-12): the
     * five horse armour is made of. A metal another mod brings gets a shoe of its
     * own at run time, named {@link #idFor} its material.
     */
    public enum Metal {
        // Cheapest first; the metalsmith stocks them in this order, one a level.
        COPPER("copper", "copper", 0xC15A36),
        IRON("iron", "iron", 0xC6C6C6),
        GOLD("gold", "golden", 0xE9B115),
        DIAMOND("diamond", "diamond", 0x4AEDD9),
        NETHERITE("netherite", "netherite", 0x4D4346);

        private final String material;
        private final String prefix;
        private final int rgb;

        Metal(String material, String prefix, int rgb) {
            this.material = material;
            this.prefix = prefix;
            this.rgb = rgb;
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
