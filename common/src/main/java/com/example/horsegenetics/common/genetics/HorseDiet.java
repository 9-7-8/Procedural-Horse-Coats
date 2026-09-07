package com.example.horsegenetics.common.genetics;

import java.util.Objects;
import java.util.Optional;

/**
 * <b>One horse's diet</b>: a {@link Diet} category and, for the two categories
 * that have them, <i>which</i> one - which metal, which gem. The resolved
 * answer the game module acts on.
 *
 * <p>{@link #resolve} is the whole channel: it walks {@link Genes#codeOrder()},
 * asks every {@link DietContribution} on the way, and keeps the <b>last</b>
 * claim. See {@link DietContribution} for why last rather than first.
 */
public final class HorseDiet {

    /** The answer for the overwhelming majority of horses: vanilla decides. */
    public static final HorseDiet NORMAL = new HorseDiet(Diet.NORMAL, 0);

    private final Diet diet;
    private final int variant;

    private HorseDiet(Diet diet, int variant) {
        this.diet = Objects.requireNonNull(diet, "diet");
        this.variant = variant;
    }

    /** A diet with no variants - anything but {@link Diet#INGOT} and {@link Diet#GEM}. */
    public static HorseDiet of(Diet diet) {
        return of(diet, 0);
    }

    /**
     * A diet and its chosen variant. {@code variant} is clamped into
     * {@code [0, diet.variants())}, and is ignored entirely - forced to
     * {@code 0} - for a diet with no variants, so a claim cannot smuggle a
     * meaningless index through into the game module's table lookup.
     */
    public static HorseDiet of(Diet diet, int variant) {
        int n = diet.variants();
        if (n <= 0) {
            return new HorseDiet(diet, 0);
        }
        return new HorseDiet(diet, Math.max(0, Math.min(n - 1, variant)));
    }

    public Diet diet() {
        return diet;
    }

    /** Index into the game module's item list for {@link #diet()}. Always {@code 0} without variants. */
    public int variant() {
        return variant;
    }

    /** Convenience: {@link Diet#isSpecial()} on the category. */
    public boolean isSpecial() {
        return diet.isSpecial();
    }

    /**
     * This horse's diet: the last {@link DietContribution} in
     * {@link Genes#codeOrder()} with something to say, or {@link #NORMAL}.
     *
     * <p>{@code epigenome} may be {@code null} - the question was asked about a
     * genotype rather than about a horse - in which case a claim that varies
     * gets midpoints and answers with the middle of what it could pick. That is
     * the honest answer, and it is what the wiki and the designer ask for.
     */
    public static HorseDiet resolve(Genotype genotype, Epigenome epigenome) {
        HorseDiet found = NORMAL;
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof DietContribution contribution)) {
                continue;
            }
            Optional<HorseDiet> claim = contribution.diet(genotype.pair(gene), genotype,
                    AlleleRandomness.forGene(gene, genotype, epigenome));
            if (claim.isPresent()) {
                found = claim.get();
            }
        }
        return found;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HorseDiet d && d.diet == diet && d.variant == variant;
    }

    @Override
    public int hashCode() {
        return diet.hashCode() * 31 + variant;
    }

    @Override
    public String toString() {
        return diet.variants() > 0 ? diet.id() + "[" + variant + "]" : diet.id();
    }
}
