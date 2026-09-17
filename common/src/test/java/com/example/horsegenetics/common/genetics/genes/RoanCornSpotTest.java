package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Roan's corn spots, measured.</b> Islands of solid base colour left inside
 * the roan field, which the painter makes by <i>suppressing</i> whitening rather
 * than painting anything of its own.
 *
 * <h2>Why this is statistical where the silver dapple test is not</h2>
 * A corn spot cannot be isolated on one horse. The painter reads its epigenetics
 * from the build context, so there is no way to paint the <i>same</i> roan twice
 * with the spots off and on; and {@code density} varies from 0 to
 * {@code DENSITY_RANGE} on top of {@code WHITE_FRACTION}, which moves a
 * horse's total whiteness a great deal more than its spots do. So the
 * whiteness assertion compares <b>means over many horses</b>, where the density
 * spread averages out and a systematic suppression does not.
 *
 * <p>{@link #cornSpotsAreAMinorityTrait} is the crisp one, and is the claim most
 * likely to be wrong: the javadoc says most roans have none, and a propensity
 * that fired on every horse would be a different and much rarer animal.
 */
class RoanCornSpotTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;
    private static final int HORSES = 240;

    private static Genotype roan() {
        String[] segs = Genotype.wildType().toCode().split("-");
        for (int i = 0; i < segs.length; i++) {
            if (segs[i].startsWith(RoanGene.KEY + "=")) {
                segs[i] = RoanGene.KEY + "=Rn/rn";
            }
        }
        return Genotype.parse(String.join("-", segs));
    }

    private static CoatBuildContext ctx(long seed) {
        return new CoatBuildContext(roan(), Epigenome.fromSeed(seed), Skin.ADULT, true);
    }

    /** This copy's corn propensity, as the painter reads it. */
    private static double cornRoll(CoatBuildContext c) {
        EpiValues epi = c.epigeneticsFor(RoanGene.KEY);
        return epi.get(RoanGene.CORN);
    }

    private static boolean spotty(CoatBuildContext c) {
        return cornRoll(c) < RoanGene.CORN_CHANCE;
    }

    private static PigmentField paint(CoatBuildContext c) {
        AllelePair pair = new AllelePair(Genes.ROAN.Rn, Genes.ROAN.rn);
        PigmentField out = Genes.ROAN.expressionOf(pair).restrict(c, new PigmentField(N));
        assertTrue(out != null, "roan's expression must paint");
        return out;
    }

    /** Total pigment removed over the trunk - the horse's overall whiteness. */
    private static double whiteness(PigmentField f) {
        double[] sum = {0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.BODY) {
                return;
            }
            sum[0] += (1.0 - f.black(px, py)) + (1.0 - f.red(px, py));
        });
        return sum[0];
    }

    /**
     * <b>Most roans have no corn spots.</b> The propensity is rolled against
     * {@link RoanGene#CORN_CHANCE} the way the forehead is, so a clear majority
     * of horses must come out clean - and some must not.
     */
    @Test
    void cornSpotsAreAMinorityTrait() {
        int spotted = 0;
        for (long seed = 0; seed < HORSES; seed++) {
            if (spotty(ctx(seed))) {
                spotted++;
            }
        }
        double share = spotted / (double) HORSES;
        assertTrue(share > 0.05,
                "no roan in " + HORSES + " rolled corn spots - the propensity is never firing");
        assertTrue(share < 0.55,
                share + " of roans are corn-spotted; it is meant to be a minority trait");
    }

    /**
     * Corn spots suppress whitening, so a spotted roan carries measurably less
     * white over its trunk than a clean one. Compared as means, for the reason
     * in the class note.
     */
    @Test
    void spottedRoansCarryLessWhiteOverall() {
        double spottySum = 0;
        int spottyCount = 0;
        double plainSum = 0;
        int plainCount = 0;

        for (long seed = 0; seed < HORSES; seed++) {
            CoatBuildContext c = ctx(seed);
            double w = whiteness(paint(c));
            if (spotty(c)) {
                spottySum += w;
                spottyCount++;
            } else {
                plainSum += w;
                plainCount++;
            }
        }

        assertTrue(spottyCount > 10 && plainCount > 10,
                "not enough of each kind to compare: " + spottyCount + " spotted, " + plainCount + " plain");

        double spottyMean = spottySum / spottyCount;
        double plainMean = plainSum / plainCount;
        assertTrue(spottyMean < plainMean,
                "corn-spotted roans are not whiter-suppressed than plain ones: spotted mean "
                        + spottyMean + " vs plain mean " + plainMean
                        + " - the spots are removing no white at all");
    }
}
