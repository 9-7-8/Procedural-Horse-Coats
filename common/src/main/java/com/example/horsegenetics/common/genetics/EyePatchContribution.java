package com.example.horsegenetics.common.genetics;

import java.util.Optional;

/**
 * <b>A capability a {@link Gene} may additionally implement</b>: "this
 * combination of my alleles paints part of an iris a colour of its own".
 *
 * <p>The companion to {@link EyeColorContribution}, and the difference between
 * them is the difference between the two things an eye can be:
 * <ul>
 *   <li><b>{@link EyeColorContribution}</b> - one colour for both irises,
 *       ranked, because a horse has one eye colour and several genes can claim
 *       it. Cream, champagne, tiger eye, and the blue of the white loci.</li>
 *   <li><b>this</b> - coloured patches drawn <i>over</i> that, per eye, which
 *       compose instead of competing. It is how a horse gets two different
 *       colours in one iris.</li>
 * </ul>
 *
 * <h2>The contract</h2>
 * <ul>
 *   <li>Pure, and answered from the pair, the genotype and the horse's
 *       epigenome alone.</li>
 *   <li><b>May vary per horse</b> - unlike the eye-colour channel this one
 *       exists for the case that does. Take every number from
 *       {@link GeneEpigenetics}, so the patch is inherited with the allele
 *       copy that drew it, and declare the outcome
 *       {@link Expression.Builder#varies()} so the horse gets its own texture
 *       instead of colliding with another in the coat cache.</li>
 *   <li>{@code epigenome} may be {@code null} - a question asked about a
 *       genotype rather than about a horse. {@link GeneEpigenetics#forGene}
 *       already handles that by handing back midpoints; a gene that cannot
 *       answer without a real horse should return {@link Optional#empty()}.</li>
 *   <li>A gene with nothing to say returns empty. All but one do.</li>
 * </ul>
 *
 * <p>Applied by the composer in {@link Genes#codeOrder()} during the overlay
 * phase, immediately after the eye-colour claim - so a gene that wants the
 * whole eye (light's glowing gold, the leopard complex's white sclera rim)
 * still gets the last word.
 */
public interface EyePatchContribution {

    /**
     * This gene's patches, or empty.
     *
     * @param pair          this gene's combination on the horse
     * @param genotype      the whole genotype, for a genotype-context read
     * @param epigenome     the horse's epigenetics, or {@code null} for a
     *                      genotype-only question
     * @param whiteCoverage fraction of the finished coat left unpigmented, {@code [0,1]}
     */
    Optional<EyePatches> eyePatches(AllelePair pair, Genotype genotype, Epigenome epigenome,
                                    double whiteCoverage);
}
