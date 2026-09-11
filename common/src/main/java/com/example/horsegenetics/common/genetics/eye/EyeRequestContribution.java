package com.example.horsegenetics.common.genetics.eye;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>A capability a {@link com.example.horsegenetics.common.genetics.Gene} may
 * additionally implement</b>: "a horse carrying this combination of my alleles
 * has eyes <i>like this</i>".
 *
 * <p>It replaces {@link com.example.horsegenetics.common.genetics.EyeColorContribution}
 * for every <b>natural</b> gene. The difference is not cosmetic:
 * <ul>
 *   <li>the old hook returned an {@link com.example.horsegenetics.common.genetics.EyeColor}
 *       - a colour the gene invented, applied at bake time, gone the moment the
 *       horse lost the allele;</li>
 *   <li>this one returns an {@link EyeRequest} - alleles at the
 *       {@link EyeLocus eye loci}, written onto the horse when it is made, and
 *       <b>inherited by its foals</b>.</li>
 * </ul>
 *
 * <h2>Magical genes do not use this</h2>
 * The old colour channel is still there and is still how the four magical genes
 * that touch an eye work (dhampir's red, the shadowcreature's gold, light's
 * glow, magic sectoral heterochromia). They are painting something no pigment
 * can make and they are painting it <i>over</i> the horse's real eyes, which is
 * a different claim from "this horse's eyes are gold". A modder writing a gene
 * file is discouraged from doing either - see the note in the gene creator, and
 * {@code wiki/making-a-gene.html}.
 *
 * <h2>The contract</h2>
 * <ul>
 *   <li>Pure, and answered from the <b>genotype alone</b> - plus the epigenome,
 *       which may be {@code null}.</li>
 *   <li><b>No coat.</b> The old hook was handed {@code whiteCoverage}, measured
 *       on the finished coat; this one cannot be, because it runs when the horse
 *       is <i>bred</i> and there is no coat yet. A white locus that wants the
 *       "broadly white however it got there" rule reads
 *       {@link com.example.horsegenetics.common.genetics.genes.WhitePatternEyes#whiteScore}
 *       instead, which answers the same question from the alleles.</li>
 *   <li>Requests are collected in
 *       {@link com.example.horsegenetics.common.genetics.Genes#codeOrder()} and
 *       merged last-writer-wins per locus. A gene with nothing to say returns
 *       {@link EyeRequest#none()}.</li>
 *   <li>A request <b>may</b> depend on the epigenome - champagne picks which of
 *       three hues to ask for off its own allele copy - and that is safe here in
 *       a way it was not before: the answer is written down as an allele once,
 *       at birth, rather than re-derived at every bake.</li>
 * </ul>
 */
public interface EyeRequestContribution {

    /**
     * What this gene asks of the eye loci, or {@link EyeRequest#none()}.
     *
     * @param pair      this gene's combination on the horse
     * @param genotype  the whole genotype, for a genotype-context read
     * @param epigenome the horse's epigenetics, or {@code null} for a
     *                  genotype-only question
     */
    EyeRequest requestEyes(AllelePair pair, Genotype genotype, Epigenome epigenome);
}
