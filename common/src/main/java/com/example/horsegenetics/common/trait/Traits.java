package com.example.horsegenetics.common.trait;

import java.util.List;
import java.util.Optional;

/**
 * <b>Everything a horse's genotype says about its body</b>, as plain data:
 * the three attribute values the game reads, the body scale, and the list of
 * {@link Condition}s it expresses.
 *
 * <p>Produced by {@link HorseTraits#resolve}. It is a <b>pure function of the
 * genotype</b> - no RNG, no epigenetics, no entity state - which is the whole
 * point of the change that created it: a horse's speed and health used to be a
 * uniform roll off its parents' numbers, so two identical genotypes could
 * differ by a factor of two and "breeding for speed" was breeding for luck.
 * Now the same alleles always give the same horse, and the only way to move a
 * number is to move an allele.
 *
 * <p>Nothing is stored on the {@code HorseRecord}: the record holds the
 * genotype, and the traits are re-derived from it wherever they are needed.
 * That is one fact in one place, and it means a change to a gene's weights
 * takes effect on horses that already exist.
 */
public record Traits(double speed, double health, double jump, double scale,
                     List<Condition> conditions) {

    public Traits {
        conditions = List.copyOf(conditions);
    }

    /**
     * The worst thing in {@link #conditions()} - {@link Viability#VIABLE} when
     * there is nothing lethal in the list.
     */
    public Viability viability() {
        Severity worst = Severity.INFORMATIONAL;
        for (Condition c : conditions) {
            if (c.severity().ordinal() > worst.ordinal()) {
                worst = c.severity();
            }
        }
        return Viability.of(worst);
    }

    /** The condition that kills this horse, for the chat line and the death message. */
    public Optional<Condition> lethalCondition() {
        Condition worst = null;
        for (Condition c : conditions) {
            if (c.severity().lethal() && (worst == null || c.severity().ordinal() > worst.severity().ordinal())) {
                worst = c;
            }
        }
        return Optional.ofNullable(worst);
    }

    /** Does this genotype produce a horse that dies (at conception or shortly after birth)? */
    public boolean lethal() {
        return viability() != Viability.VIABLE;
    }

    /** Conditions worth naming in the UI - every one of them, in gene order. */
    public boolean hasConditions() {
        return !conditions.isEmpty();
    }
}
