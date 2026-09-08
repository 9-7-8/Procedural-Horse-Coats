package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.CutieMarkContribution;
import com.example.horsegenetics.common.genetics.DietContribution;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.EyeColorContribution;
import com.example.horsegenetics.common.genetics.EyePatchContribution;
import com.example.horsegenetics.common.genetics.Gene;

/**
 * <b>Which tab of the horse-information screen a gene belongs on.</b> Three
 * buckets, decided by what the gene <i>does</i> rather than by a hand-kept list
 * of keys - a drop-in gene from the gene creator sorts itself.
 *
 * <ul>
 *   <li>{@link #OTHER} - the gene reaches the horse through a channel that is
 *       neither the coat nor the body: an ability, the diet locus, eye colour,
 *       an eye patch, a cutie mark. <b>This wins over the other two</b>, which
 *       is the owner's rule and the reason dhampir is here rather than under
 *       Coat: it does change the coat, but it does more than that, and the
 *       interesting thing about it is the more.</li>
 *   <li>{@link #BODY} - speed, max health, jump, size, or a disorder. Anything
 *       {@link TraitBreakdown} has a term for.</li>
 *   <li>{@link #COAT} - it paints, and that is all it does.</li>
 * </ul>
 *
 * <p>A gene that does none of the three - the sex locus, and a coat modifier
 * like {@code PATN1} that only means anything read through another gene -
 * is {@link #COAT}, because that is where a reader will look for it: its
 * alleles are listed under the gene it modifies.
 */
public enum GeneCategory {
    COAT,
    BODY,
    OTHER;

    public static GeneCategory of(Gene gene) {
        if (hasOtherChannel(gene)) {
            return OTHER;
        }
        if (movesBody(gene)) {
            return BODY;
        }
        return COAT;
    }

    /**
     * Does the gene reach the horse through something other than the coat and
     * the body? Declared by the interfaces it implements, so this is the one
     * place to add a channel when a new one is invented.
     */
    public static boolean hasOtherChannel(Gene gene) {
        return gene instanceof AbilityContribution
                || gene instanceof EpigeneticAbilityContribution
                || gene instanceof DietContribution
                || gene instanceof EyeColorContribution
                || gene instanceof EyePatchContribution
                || gene instanceof CutieMarkContribution;
    }

    /** Does the gene push speed, health, jump, size or a disorder? */
    public static boolean movesBody(Gene gene) {
        return gene instanceof TraitContribution || gene instanceof EpigeneticTraitContribution;
    }
}
