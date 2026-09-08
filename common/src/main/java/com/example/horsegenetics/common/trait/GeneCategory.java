package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.CutieMarkContribution;
import com.example.horsegenetics.common.genetics.DietContribution;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.EyeColorContribution;
import com.example.horsegenetics.common.genetics.EyePatchContribution;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.genes.CutieMarkGene;

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
 *
 * <h2>The gene, and the gene on <i>this</i> horse</h2>
 * {@link #of(Gene)} answers for the gene: does it implement the interface at
 * all. That is the right question for a reference page listing what a locus
 * <em>can</em> do, and the wrong one for a horse's own screen -
 * {@code KIT} implements {@link EyeColorContribution}, so every horse alive
 * came out with a {@code KIT} row filed under "Other genes" whether or not its
 * particular alleles have ever claimed an iris. Most do not: only the broad
 * white outcomes claim one.
 *
 * <p>{@link #of(Gene, AllelePair, Genotype, Epigenome)} asks the narrower
 * question - <b>does this horse's combination actually say anything on that
 * channel</b> - by calling the channel and looking at the answer. A gene that
 * says nothing falls through to body or coat, so a {@code KIT} sabino is filed
 * under Coat where a reader would look for it, and a {@code KIT} near-white,
 * which really does turn the eyes blue, is not.
 *
 * <p>The eye channels are asked with a <b>white coverage of 1</b>, the most
 * generous value there is. That is deliberate: the question being answered is
 * about the horse's <i>alleles</i>, not about how white the coat happened to
 * come out, and computing the real coverage means baking the coat - far too
 * much work for sorting a row onto a tab.
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
     * Where this gene belongs <b>on this horse</b> - see the class note. Falls
     * back to {@link #of(Gene)} for the body and coat buckets, which do not
     * depend on the combination: a gene that can move the body is on Health
     * even when this horse's alleles move it by nothing, because the row saying
     * "adds nothing" is the answer to the question the tab is asking.
     *
     * @param epigenome may be {@code null} - a question about a genotype rather
     *                  than about a horse; the channels take midpoints then.
     */
    public static GeneCategory of(Gene gene, AllelePair pair, Genotype genotype,
                                  Epigenome epigenome) {
        if (hasOtherChannel(gene)) {
            // A gene that neither paints nor moves the body has nowhere else to
            // be: the particle locus, the diet locus and the cutie mark are
            // "other" genes whatever a horse's combination happens to grant, and
            // filing a silent carrier of one under Coat would put it on a tab
            // about colour with nothing to say about colour.
            if (!gene.affectsCoat() && !movesBody(gene)) {
                return OTHER;
            }
            if (pair == null || saysSomethingElse(gene, pair, genotype, epigenome)) {
                return OTHER;
            }
        }
        return movesBody(gene) ? BODY : COAT;
    }

    /**
     * Does this combination reach the horse through a channel that is neither
     * the coat nor the body? Each branch calls the gene's own answer and looks
     * at it, so a gene needs no extra declaration to be sorted correctly - and
     * a channel that throws is treated as silent rather than taking the screen
     * down with it.
     */
    public static boolean saysSomethingElse(Gene gene, AllelePair pair, Genotype genotype,
                                            Epigenome epigenome) {
        try {
            if (gene instanceof AbilityContribution g
                    && !g.abilitiesFor(pair, genotype).isEmpty()) {
                return true;
            }
            if (gene instanceof EpigeneticAbilityContribution g
                    && !g.abilitiesFor(pair, genotype,
                            GeneEpigenetics.forGene(gene, genotype, epigenome)).isEmpty()) {
                return true;
            }
            if (gene instanceof DietContribution g
                    && g.diet(pair, genotype,
                            GeneEpigenetics.forGene(gene, genotype, epigenome)).isPresent()) {
                return true;
            }
            if (gene instanceof EyeColorContribution g
                    && g.eyeColor(pair, genotype, epigenome, 1.0).isPresent()) {
                return true;
            }
            if (gene instanceof EyePatchContribution g
                    && g.eyePatches(pair, genotype, epigenome, 1.0).isPresent()) {
                return true;
            }
            if (gene instanceof CutieMarkContribution g) {
                // Compared by identity, not by value: a modifier with nothing
                // to say hands the mark straight back, and Mark holds a double[]
                // whose record equals() is reference equality anyway - so value
                // comparison would be the same test wearing a disguise.
                CutieMarkGene.Mark probe =
                        new CutieMarkGene.Mark(1, false, new double[]{0, 0, 0}, 1.0, 0.0, false);
                return g.modifyCutieMark(pair, genotype, epigenome, probe) != probe;
            }
        } catch (RuntimeException unanswerable) {
            return false;
        }
        return false;
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
