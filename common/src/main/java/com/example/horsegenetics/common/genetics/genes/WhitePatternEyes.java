package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyeColorContribution;

import java.util.Optional;

/**
 * <b>The blue eye</b> - the one rule shared by all four white-spotting loci,
 * kept here rather than written out four times.
 *
 * <p>A high-white horse commonly has one or two blue eyes, and it is not a
 * separate trait: the same failure of pigment cells to reach the skin also
 * leaves them out of the iris. So this is a property the white loci already
 * have, not a new gene - a "blue eyes" locus that mysteriously only ever
 * appeared on white horses would be a worse model of the same fact.
 *
 * <h2>Two ways a horse qualifies</h2>
 * <ol>
 *   <li><b>Its own allele says so.</b> Splash is <i>the</i> blue-eyed pattern,
 *       diagnostic even when the white itself is modest, so any expressing
 *       {@code MITF} or {@code PAX3} combination qualifies. Frame does too.
 *       {@code KIT} only qualifies from {@code broad-white} upward - a sabino
 *       with four socks and a blaze has ordinary dark eyes.</li>
 *   <li><b>The horse is broadly white however it got there.</b> Past
 *       {@value #COVERAGE_THRESHOLD} of the coat depigmented, the eyes go with
 *       it. This is what covers the case no per-locus test can see: a horse
 *       white from <i>two</i> mild alleles stacking has exactly the same claim
 *       as one white from a single bold allele.</li>
 * </ol>
 *
 * <p>Blue is claimed at {@link EyeColor#RANK_DEPIGMENTED}, above any pigment
 * gene, because a depigmented iris has nothing left for tiger eye to recolour.
 *
 * @see EyeColorContribution
 */
public final class WhitePatternEyes {

    private WhitePatternEyes() {}

    /**
     * How much of the coat has to be unpigmented before the eyes follow,
     * regardless of which loci did it.
     */
    public static final double COVERAGE_THRESHOLD = 0.55;

    /**
     * {@link EyeColor#BLUE} if either {@code locusQualifies} or the horse is
     * broadly white; empty otherwise.
     *
     * @param locusQualifies this gene's own combination is one of the
     *                       blue-eyed patterns
     * @param whiteCoverage  fraction of the finished coat left unpigmented
     */
    public static Optional<EyeColor> blueIf(boolean locusQualifies, double whiteCoverage) {
        return locusQualifies || whiteCoverage >= COVERAGE_THRESHOLD
                ? Optional.of(EyeColor.BLUE)
                : Optional.empty();
    }
}
