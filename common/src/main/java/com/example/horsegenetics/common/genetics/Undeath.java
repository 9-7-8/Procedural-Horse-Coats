package com.example.horsegenetics.common.genetics;

/**
 * <b>Is this horse undead, and which kind?</b> The one answer every caller asks -
 * the "undead" mob group, the undead voice, the converter's tests - so it is
 * written once. Only an <b>expressing</b> horse counts: a carrier looks and acts
 * like any other horse, and is not undead for cleansing light, a holy ward or
 * anything else that asks (undead treatment D6).
 */
public final class Undeath {

    /** Which undead a horse is. */
    public enum Kind { NONE, SKELETON, ZOMBIE }

    private Undeath() {
    }

    public static Kind kindOf(Genotype genotype) {
        if (genotype == null) {
            return Kind.NONE;
        }
        if (Genes.SKELETON.expresses(genotype)) {
            return Kind.SKELETON;
        }
        return Genes.ZOMBIE.expresses(genotype) ? Kind.ZOMBIE : Kind.NONE;
    }

    public static boolean isUndead(Genotype genotype) {
        return kindOf(genotype) != Kind.NONE;
    }
}
