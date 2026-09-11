package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.EyePatch;
import com.example.horsegenetics.common.genetics.EyeSpread;
import com.example.horsegenetics.common.genetics.eye.EyeHue;
import com.example.horsegenetics.common.genetics.eye.EyeLocus;
import com.example.horsegenetics.common.genetics.eye.EyeRequest;
import com.example.horsegenetics.common.genetics.eye.EyeSector;

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
 * <h2>What changed when the eyes became loci</h2>
 * The blue itself <i>is</i> a gene now - {@link EyeColourGene}, once per eye -
 * and a white locus does not paint it. It
 * {@linkplain EyeRequest requests} {@link EyeHue#MID_BLUE} at the iris loci, and
 * the allele is written onto the horse when the horse is made. A splashed white
 * foal is genuinely {@code MBl/MBl}, so it shows blue eyes <b>and passes them
 * on</b> - which is the owner's call and the whole difference between this and
 * the override it replaces.
 *
 * <h2>Two ways a horse qualifies</h2>
 * <ol>
 *   <li><b>Its own allele says so.</b> Splash is <i>the</i> blue-eyed pattern,
 *       diagnostic even when the white itself is modest, so any expressing
 *       {@code MITF} or {@code PAX3} combination qualifies. Frame does too.
 *       {@code KIT} only qualifies from {@code broad-white} upward - a sabino
 *       with four socks and a blaze has ordinary dark eyes.</li>
 *   <li><b>The horse is broadly white however it got there.</b> Past
 *       {@value #WHITE_SCORE_THRESHOLD} on {@link #whiteScore}, the eyes go
 *       with it. This is what covers the case no per-locus test can see: a
 *       horse white from <i>two</i> mild alleles stacking has exactly the same
 *       claim as one white from a single bold allele.</li>
 * </ol>
 *
 * <h2>The score is an estimate, and it has to be</h2>
 * That second rule used to read the <b>finished coat</b>: the fraction of mapped
 * texels the white loci had left with no pigment at all, measured after the
 * bake. It cannot any more. A request is applied when the horse is
 * <i>bred</i>, and there is no coat at that point and no cheap way to make one -
 * so the question has to be answerable from the alleles.
 *
 * <p>{@link #whiteScore} is therefore a declared sum rather than a measurement:
 * each locus that can take pigment off a horse says how much of it, through
 * {@link WhiteExtent}, and the numbers are set so that the combinations that
 * used to cross the old coverage threshold still cross this one. It is an
 * approximation and it is recorded as one on {@code wiki/known-gaps.html} - the
 * case it will get wrong is a horse whose loci stack in a way the flat sum does
 * not model.
 */
public final class WhitePatternEyes {

    private WhitePatternEyes() {}

    /**
     * <b>How much of a horse this locus takes the pigment off</b>, roughly, on a
     * scale where {@code 1.0} is "white from end to end".
     *
     * <p>Declared by the gene rather than tabulated here, for the same reason
     * {@link Gene#coatDependsOn()} is declared: a hand-written list somewhere
     * else is wrong the first time someone adds a white gene and forgets it, and
     * wrong silently - the failure being a broadly white horse with brown eyes,
     * and nothing going red.
     */
    public interface WhiteExtent {
        /** {@code [0,1]} - {@code 0} for a combination that takes nothing off. */
        double whiteness(AllelePair pair);
    }

    /**
     * Past this, the eyes follow the coat whatever the individual loci say. The
     * old measured threshold was {@code 0.55} of the finished coat; this is the
     * same intent against {@link #whiteScore}'s declared sum, which runs a
     * little hotter because two stacked loci add rather than overlapping.
     */
    public static final double WHITE_SCORE_THRESHOLD = 0.62;

    /** Every {@link WhiteExtent} locus on this horse, summed. */
    public static double whiteScore(Genotype genotype) {
        double total = 0.0;
        for (Gene g : Genes.codeOrder()) {
            if (g instanceof WhiteExtent extent) {
                total += extent.whiteness(genotype.pair(g));
            }
        }
        return total;
    }

    /**
     * The request a white locus makes: blue at the iris loci, over as much of
     * each eye as {@link EyeSpread} says the depigmentation reached.
     *
     * <p>The spread is still the 62 / 22 / 16 roll it always was, and still read
     * off the requesting locus's own allele copy - so how far the blue got is
     * still not an allele question, it is still inherited as a tendency, and it
     * is still one roll rather than one per white locus. What is new is where
     * the answer <i>goes</i>: two whole blue eyes come out as
     * {@code MBl/MBl} at both iris loci, one blue eye as {@code MBl/MBl} at one
     * of them, and a wedge as a sector allele plus a blue sector colour, leaving
     * the iris itself whatever the horse's own eye loci gave it. All three are
     * then inherited, which the old painted spread never was.
     *
     * @param locusQualifies this gene's own combination is one of the blue-eyed
     *                       patterns
     * @param gene           the requesting locus, whose copy the spread is read off
     */
    public static EyeRequest blueIf(boolean locusQualifies, Gene gene, AllelePair pair,
                                    Genotype genotype, Epigenome epigenome) {
        if (!locusQualifies && whiteScore(genotype) < WHITE_SCORE_THRESHOLD) {
            return EyeRequest.none();
        }
        EyeSpread spread = EyeSpread.roll(
                GeneEpigenetics.forGene(gene, genotype, epigenome).expressed());
        EyeRequest request = EyeRequest.none();
        request = apply(request, EyeLocus.EyeSideRef.RIGHT, spread.right());
        request = apply(request, EyeLocus.EyeSideRef.LEFT, spread.left());
        return request;
    }

    /** One eye's share of the spread, as alleles. */
    private static EyeRequest apply(EyeRequest request, int eye, int quadrants) {
        if (quadrants == EyePatch.NONE) {
            return request;              // the blue never reached this eye
        }
        if (quadrants == EyePatch.WHOLE) {
            return request.iris(eye, EyeHue.MID_BLUE);
        }
        EyeSector sector = sectorFor(quadrants);
        return sector == null
                ? request.iris(eye, EyeHue.MID_BLUE)
                : request.sector(eye, sector, EyeHue.MID_BLUE);
    }

    /**
     * The named sector whose mask is {@code quadrants}, or {@code null}.
     *
     * <p>{@link EyeSpread} draws from {@link EyePatch#WEDGES}, which is twelve
     * shapes; {@link EyeSector} names ten, the difference being that the wedge
     * table leaves out the two diagonals and the sector alleles include them and
     * leave out the four three-corner shapes. A wedge with no allele falls back
     * to a whole blue eye rather than to nothing - the horse is white enough to
     * have earned a blue eye either way, and "no eye at all" is the one answer
     * that would be wrong.
     */
    private static EyeSector sectorFor(int quadrants) {
        for (EyeSector s : EyeSector.sectors()) {
            if (s.mask() == quadrants) {
                return s;
            }
        }
        return null;
    }
}
