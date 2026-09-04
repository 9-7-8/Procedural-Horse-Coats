package com.example.horsegenetics.common.trait;

/**
 * Whether a drawn genotype produces a horse at all, and if so whether it
 * survives being born. Derived from the worst {@link Severity} in a
 * {@link Traits}' condition list - never declared directly.
 */
public enum Viability {

    /** A normal horse. Everything short of a lethal lands here, impairments included. */
    VIABLE,

    /**
     * Born, then dies. The breeding code still creates the foal, names it and
     * files it in the pedigree; a separate handler does the killing so the
     * player can see what happened.
     */
    LETHAL_AT_BIRTH,

    /**
     * No foal. The pairing draws a genotype as usual, this check reads it, and
     * the birth is cancelled - so {@code Genotype.breedWith} stays untouched
     * and nothing about inheritance needs a special case.
     */
    LETHAL_AT_CONCEPTION;

    static Viability of(Severity severity) {
        return switch (severity) {
            case LETHAL_AT_CONCEPTION -> LETHAL_AT_CONCEPTION;
            case LETHAL_AT_BIRTH -> LETHAL_AT_BIRTH;
            default -> VIABLE;
        };
    }
}
