package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.genes.AbstractMagicStatGene;
import com.example.horsegenetics.common.genetics.epi.EpiDrift;
import com.example.horsegenetics.common.genetics.genes.MagicSizeGene;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>A breed shapes its founders and then lets go.</b>
 *
 * <p>This is the property the epigenetics rewrite was largely for. It used to be
 * false: a breed's {@code TargetBand} was applied every time a horse's body was
 * <i>resolved</i>, and {@code BreedLineage.statTargets()} handed a <b>cross</b>
 * the per-axis <i>average</i> of its two parents' bands. So a Percheron bred to
 * a Falabella produced a foal whose size was recomputed into a mid-size band
 * every time anything looked at it - regardless of which alleles it had actually
 * inherited, and with no way for a breeder to select their way out of it.
 *
 * <p>Now the band is consulted once, by {@link BreedFounder}, and written onto
 * the founder's allele copies as ordinary numbers. After that a horse is
 * whatever it inherited. Keeping a line to a standard is the player's job.
 */
class BreedCrossTest {

    private static final String SIZE = MagicSizeGene.KEY;

    /** The size percentage on one copy of a horse's body-size locus. */
    private static double delta(Genome g, boolean firstCopy) {
        Epigenome.Copies c = g.epigenome().copies(Genes.byKey(SIZE));
        return (firstCopy ? c.first() : c.second()).values().get(AbstractMagicStatGene.DELTA);
    }

    /** A breed that pins scale writes real numbers onto its founders' copies. */
    @Test
    void aFounderCarriesItsBreedsTargetOnItsOwnCopies() {
        for (long seed = 0; seed < 20; seed++) {
            Genome big = BreedFounder.roll(Breeds.get("percheron"), new SeededRng(seed));
            Genome small = BreedFounder.roll(Breeds.get("falabella"), new SeededRng(seed));
            assertTrue(delta(big, true) > 0, "a Percheron's copies carry a real percentage");
            assertTrue(delta(small, true) > 0, "so do a Falabella's - the sign is the allele's job");
        }
    }

    /**
     * A founder's two copies are <b>not</b> identical, so which one a foal draws
     * matters. Half the interest in breeding from a good stallion is that his
     * foals differ depending on which copy they got.
     */
    @Test
    void aFoundersTwoCopiesDiffer() {
        // Except when both copies are sitting on the floor, which is not the
        // same claim and is not a bug.
        //
        // BreedFounder splits the band's total unevenly across the two copies
        // (COPY_SKEW), so a founder whose breed pinned a real target always has
        // two different numbers. But the stored percentage is floored at
        // MIN_DELTA so a copy can never point the wrong way, and when the band
        // draws a total under about twice that, BOTH halves clamp to the floor
        // and come out identical. About one Percheron in twenty. There is
        // nowhere for those two copies to differ.
        //
        // The test used to run twenty seeds and require nineteen to differ,
        // which passed for the same reason a coin lands heads - twenty draws
        // happened to hold at most one floored horse. Adding genes elsewhere in
        // the registry moved the RNG stream and it held two. Excluding the case
        // the claim was never about makes it exact rather than lucky, and two
        // hundred seeds make the exclusion itself visible.
        int free = 0;
        int floored = 0;
        for (long seed = 0; seed < 200; seed++) {
            Genome g = BreedFounder.roll(Breeds.get("percheron"), new SeededRng(seed));
            double a = delta(g, true);
            double b = delta(g, false);
            if (a <= AbstractMagicStatGene.MIN_DELTA && b <= AbstractMagicStatGene.MIN_DELTA) {
                floored++;
                continue;
            }
            free++;
            assertTrue(Math.abs(a - b) > 1e-6,
                    "a founder with room to differ must differ, seed " + seed
                            + " has " + a + " and " + b);
        }
        assertTrue(free > 150, "most founders should have room to differ, only " + free + " did");
        assertTrue(floored < 40, floored + " of 200 founders were floored - the band moved");
    }

    /**
     * <b>The cross is never normalised.</b> Every size copy a Percheron x
     * Falabella foal carries has to be a copy one of its parents actually had -
     * not a number computed from the two breeds' standards.
     */
    @Test
    void aCrossInheritsItsParentsNumbersRatherThanAnAverageOfTheirBreeds() {
        for (long seed = 0; seed < 40; seed++) {
            Genome sire = BreedFounder.roll(Breeds.get("percheron"), new SeededRng(seed));
            Genome dam = BreedFounder.roll(Breeds.get("falabella"), new SeededRng(seed + 500));
            Genome foal = dam.breedWith(sire, new SeededRng(seed + 9000));

            double[] parents = {
                    delta(sire, true), delta(sire, false),
                    delta(dam, true), delta(dam, false),
            };
            for (boolean first : new boolean[]{true, false}) {
                double got = delta(foal, first);
                assertTrue(matchesAParent(got, parents),
                        "foal copy " + got + " is not one of its parents' "
                                + java.util.Arrays.toString(parents)
                                + " - a breed standard leaked back in");
            }
        }
    }

    /**
     * How far one generation of drift can move a size percentage. The value's
     * design span is {@code 12 sigma}, {@link com.example.horsegenetics.common.genetics.epi.EpiDrift}
     * moves it by an exponential with a scale of {@code SCALE} of that, and the
     * tail is bounded near {@code 17x} the scale by float resolution - so this
     * sits comfortably above anything a single breeding can do.
     */
    private static final double ONE_GENERATION =
            20 * EpiDrift.SCALE * 12 * MagicSizeGene.SIGMA_DELTA;

    /**
     * Within one generation's drift of one of {@code parents}.
     *
     * <p>The tolerance is what makes this a real test rather than a tautology.
     * It is about {@code 0.02}, while the Percheron and Falabella copies a foal
     * chooses between sit whole tenths apart - so a value computed from the two
     * breeds' standards, or from any blend of them, lands nowhere near a parent
     * and fails here.
     */
    private static boolean matchesAParent(double got, double[] parents) {
        for (double p : parents) {
            if (Math.abs(got - p) < ONE_GENERATION) {
                return true;
            }
        }
        return false;
    }

    /**
     * And the consequence a player would actually notice: crossing the two
     * extremes gives foals that <b>spread</b> across the range, rather than a
     * paddock of identical mid-size horses.
     */
    @Test
    void crossingTwoExtremesProducesASpreadNotOneAverageHorse() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (long seed = 0; seed < 60; seed++) {
            Genome sire = BreedFounder.roll(Breeds.get("percheron"), new SeededRng(seed));
            Genome dam = BreedFounder.roll(Breeds.get("falabella"), new SeededRng(seed + 500));
            Genome foal = dam.breedWith(sire, new SeededRng(seed + 9000));
            double scale = com.example.horsegenetics.common.trait.HorseTraits
                    .resolve(foal.genotype(), foal.epigenome(), true).scale();
            min = Math.min(min, scale);
            max = Math.max(max, scale);
        }
        assertTrue(max - min > 0.05,
                "foals of two extreme breeds should vary, got " + min + ".." + max);
    }
}
