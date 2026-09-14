package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.trait.Condition;

import java.util.Optional;

/**
 * <b>Can this genotype exist at all?</b> A gene says no for an allele pair
 * through {@link Gene#canOccur} - KIT's doubled W alleles, MITF's SW3/SW3,
 * PAX3's SW4/SW4, Y/Y - and until 2026-09-13 nothing on the breeding path ever
 * asked, so two carriers could be bred into a horse the model itself calls
 * impossible (known gap 225).
 *
 * <p>Both breeding paths now treat a failure here exactly like an embryonic
 * lethal: no instant foal, or a pregnancy lost early. The {@link Condition} this
 * returns is synthetic - those genes declare none of their own - so the
 * miscarriage line has a cause to describe. Giving KIT, MITF and PAX3 real
 * lethal conditions instead was considered and not done: it would drop all three
 * loci from the random splice carrot's pool, which is a gameplay decision for the
 * owner rather than a bug fix.
 */
public final class Conceivable {

    /** The id prefix of the synthetic condition - see {@code MiscarriageSigns}. */
    public static final String ID_PREFIX = "nonviable-";

    private Conceivable() {
    }

    /** The first gene that rules this genotype out, as a lethal-at-conception cause; empty if none does. */
    public static Optional<Condition> failure(Genotype genotype) {
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (!gene.canOccur(pair)) {
                String key = gene.key();
                return Optional.of(Condition.lethalAtConception(
                        ID_PREFIX + key.substring(key.indexOf('.') + 1),
                        "Nonviable " + gene.name() + " (" + pair.toTokens() + ")",
                        "An allele pair that has never been found in a live horse. The embryo does not develop."));
            }
        }
        return Optional.empty();
    }
}
