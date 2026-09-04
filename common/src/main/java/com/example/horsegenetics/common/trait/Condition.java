package com.example.horsegenetics.common.trait;

import java.util.Objects;

/**
 * <b>A named thing that is wrong with a horse</b> - a genetic disorder it
 * expresses, resolved out of its genotype by {@link HorseTraits}.
 *
 * <p>A condition is <i>declared by a gene</i>, as a constant, next to the
 * {@link com.example.horsegenetics.common.genetics.Expression} that produces
 * it. It carries the same two pieces of prose an expression does - a short
 * {@link #name()} and a sentence of {@link #description()} - because the
 * surfaces that show it (the info panel, the paper dump) are the same surfaces
 * that show an expression, and a player who has just lost a foal deserves to be
 * told which disorder took it.
 *
 * <p><b>The mechanical effect is not here.</b> A gene applies its own stat
 * changes through {@link TraitBuilder} in the same breath as it reports the
 * condition; this record is the <i>label</i>, plus the one bit
 * ({@link #severity()}) that the breeding and foal-death code switches on.
 * Keeping them apart is what lets two genes cause "dwarfism" with different
 * numbers without either of them pretending to be the other.
 */
public record Condition(String id, String name, String description, Severity severity) {

    public Condition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(severity, "severity");
    }

    /** Shown, but costs the horse nothing - deafness, the ocular defects. */
    public static Condition informational(String id, String name, String description) {
        return new Condition(id, name, description, Severity.INFORMATIONAL);
    }

    /** The horse lives with it: fewer hearts, and usually a smaller or slower body. */
    public static Condition impairing(String id, String name, String description) {
        return new Condition(id, name, description, Severity.IMPAIRING);
    }

    /** The foal is born and then dies. */
    public static Condition lethalAtBirth(String id, String name, String description) {
        return new Condition(id, name, description, Severity.LETHAL_AT_BIRTH);
    }

    /** The embryo never develops; the pairing produces no foal. */
    public static Condition lethalAtConception(String id, String name, String description) {
        return new Condition(id, name, description, Severity.LETHAL_AT_CONCEPTION);
    }

    /** Identity is the id, so two genes can never quietly declare the same condition twice. */
    @Override
    public boolean equals(Object o) {
        return o instanceof Condition c && c.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
