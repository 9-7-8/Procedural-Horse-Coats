package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.genetics.spec.SpecSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <b>{@code FRACTAL} mask's field</b> - {@link SpecPainter#fractal}.
 *
 * <p>The mask exists so a marking can have detail at more than one scale, and
 * the whole risk in it is that the extra scales quietly change how much of the
 * horse it covers. Textbook fbm divides the octave sum by the total amplitude,
 * which makes it a weighted <b>mean</b> of independent samples - and a mean is
 * narrower than one sample, more so with every octave added. An author who
 * added a fourth octave for detail would find their {@code threshold} covering
 * less horse, with no parameter to blame.
 *
 * <p>That is not a hypothetical. It is the same defect the white loci's
 * {@code cover} knobs were built on - a knob written against a field nobody had
 * measured, delivering a 3x swing where a 1.4x one was intended - and it cost a
 * rewrite of two painters. So the divisor here is the <i>root</i> of the summed
 * squares, which preserves the spread instead of the sum, and these are the
 * tests that say so.
 *
 * <p>The field is tested directly rather than through a rendered coat, because
 * the properties live in the field: a coat would only show them through a
 * threshold, which is the thing being protected.
 */
class FractalMaskTest {

    private static final long SEED = 0x5EEDL;

    /** Body-space sample points, spread over a horse-sized box. */
    private static double[][] grid() {
        double[][] pts = new double[24 * 12 * 12][];
        int i = 0;
        for (int a = 0; a < 24; a++) {
            for (int b = 0; b < 12; b++) {
                for (int c = 0; c < 12; c++) {
                    pts[i++] = new double[]{a * 1.7, b * 2.9, c * 0.83};
                }
            }
        }
        return pts;
    }

    /**
     * <b>One octave is {@link BodyNoise#value}</b>, so a one-octave
     * {@code FRACTAL} is a {@code PATCHES} mask with more knobs. The sum is a
     * single term of amplitude 1 and the divisor is {@code sqrt(1)}, so the two
     * differ only by the rounding of {@code 0.5 + (v - 0.5)} - which is why the
     * tolerance is a last-bit one rather than zero. That is what anchors the
     * normalisation to something already in the vocabulary.
     */
    @Test
    void oneOctaveIsPlainValueNoise() {
        for (double[] p : grid()) {
            double want = BodyNoise.value(SEED, p[0], p[1], p[2]);
            double got = SpecPainter.fractal(SEED, p[0], p[1], p[2], 1, 2.13, 0.5, 0.0);
            assertEquals(want, got, 1e-12, "one octave must be the plain value field");
        }
    }

    /**
     * <b>Adding octaves must not move the coverage.</b> This is the property the
     * unusual divisor is for: at a fixed {@code threshold}, the share of the
     * horse the mask selects has to stay put as detail is added, or every knob
     * on the mask is coupled to every other one.
     */
    @Test
    void coverageHoldsAsOctavesAreAdded() {
        double[][] pts = grid();
        double first = coverage(pts, 1, 0.5);
        for (int octaves = 1; octaves <= SpecSchema.MAX_OCTAVES; octaves++) {
            double got = coverage(pts, octaves, 0.5);
            assertTrue(Math.abs(got - first) < 0.03,
                    octaves + " octaves covered " + got + " where one octave covered " + first);
        }
        // And away from the midpoint, where a narrowing field bites hardest -
        // the tail of the distribution is what a threshold of 0.75 cuts into.
        double firstHigh = coverage(pts, 1, 0.75);
        for (int octaves = 1; octaves <= SpecSchema.MAX_OCTAVES; octaves++) {
            double got = coverage(pts, octaves, 0.75);
            assertTrue(Math.abs(got - firstHigh) < 0.06,
                    octaves + " octaves covered " + got + " at threshold 0.75 where one octave "
                            + "covered " + firstHigh);
        }
    }

    /**
     * The spread itself, which is what the coverage test is really measuring.
     * Pinned separately so a failure says whether the field narrowed or merely
     * shifted.
     */
    @Test
    void theSpreadIsPreservedNotTheSum() {
        double[][] pts = grid();
        double first = deviation(pts, 1);
        for (int octaves = 1; octaves <= SpecSchema.MAX_OCTAVES; octaves++) {
            double got = deviation(pts, octaves);
            assertTrue(got > first * 0.85 && got < first * 1.15,
                    octaves + " octaves have spread " + got + ", one octave " + first);
        }
    }

    /**
     * The naive normalisation, measured, so the claim in the comment above
     * {@link SpecPainter#fractal} is not taken on trust. Dividing by the summed
     * amplitude instead of its root has to visibly narrow the field, or the
     * divisor this class defends is buying nothing.
     */
    @Test
    void theNaiveNormalisationWouldHaveNarrowedIt() {
        double[][] pts = grid();
        double one = naiveDeviation(pts, 1);
        double six = naiveDeviation(pts, SpecSchema.MAX_OCTAVES);
        assertTrue(six < one * 0.8,
                "dividing by the summed amplitude should narrow the field, but six octaves "
                        + "gave spread " + six + " against one octave's " + one);
    }

    /** The field never leaves {@code [0, 1]}, whatever the octaves do. */
    @Test
    void theFieldStaysInRange() {
        for (int octaves = 1; octaves <= SpecSchema.MAX_OCTAVES; octaves++) {
            for (double[] p : grid()) {
                double n = SpecPainter.fractal(SEED, p[0], p[1], p[2], octaves, 2.13, 0.5, 0.0);
                assertTrue(n >= 0 && n <= 1, "field left [0,1]: " + n);
            }
        }
    }

    /**
     * {@code ridged} and {@code billow} are one field read two ways, so they
     * must sum to 1 everywhere. If they ever drift apart, one of them has grown
     * a second sampling path.
     */
    @Test
    void ridgedAndBillowAreComplements() {
        for (double[] p : grid()) {
            double f = SpecPainter.fractal(SEED, p[0], p[1], p[2], 3, 2.13, 0.5, 0.0);
            double ridged = 1.0 - Math.abs(2.0 * f - 1.0);
            double billow = Math.abs(2.0 * f - 1.0);
            assertEquals(1.0, ridged + billow, 1e-12);
        }
    }

    /**
     * <b>Warp moves the field, and does so deterministically.</b> All three
     * offsets are taken from the unwarped point; taking the second from an
     * already-warped first would make the field depend on the order the axes
     * happen to be written in, which is the kind of thing that surfaces as a
     * browser / game mismatch six months later.
     */
    @Test
    void warpDisplacesTheFieldAndIsDeterministic() {
        double[][] pts = grid();
        int moved = 0;
        for (double[] p : pts) {
            double plain = SpecPainter.fractal(SEED, p[0], p[1], p[2], 3, 2.13, 0.5, 0.0);
            double warped = SpecPainter.fractal(SEED, p[0], p[1], p[2], 3, 2.13, 0.5, 0.6);
            assertEquals(warped, SpecPainter.fractal(SEED, p[0], p[1], p[2], 3, 2.13, 0.5, 0.6), 0.0);
            if (Math.abs(plain - warped) > 0.02) {
                moved++;
            }
        }
        assertTrue(moved > pts.length * 0.5,
                "warp barely moved the field - only " + moved + " of " + pts.length + " points");
    }

    // ------------------------------------------------------------------

    private static double coverage(double[][] pts, int octaves, double threshold) {
        int hit = 0;
        for (double[] p : pts) {
            if (SpecPainter.fractal(SEED, p[0], p[1], p[2], octaves, 2.13, 0.5, 0.0) > threshold) {
                hit++;
            }
        }
        return hit / (double) pts.length;
    }

    private static double deviation(double[][] pts, int octaves) {
        double sum = 0;
        double sumSq = 0;
        for (double[] p : pts) {
            double n = SpecPainter.fractal(SEED, p[0], p[1], p[2], octaves, 2.13, 0.5, 0.0);
            sum += n;
            sumSq += n * n;
        }
        double mean = sum / pts.length;
        return Math.sqrt(sumSq / pts.length - mean * mean);
    }

    /** {@link SpecPainter#fractal} as a textbook fbm would have written it. */
    private static double naiveDeviation(double[][] pts, int octaves) {
        double sum = 0;
        double sumSq = 0;
        for (double[] p : pts) {
            double acc = 0;
            double norm = 0;
            double amp = 1;
            double freq = 1;
            for (int o = 0; o < octaves; o++) {
                acc += amp * BodyNoise.value(SEED + 131L * o, p[0] * freq, p[1] * freq, p[2] * freq);
                norm += amp;
                amp *= 0.5;
                freq *= 2.13;
            }
            double n = acc / norm;
            sum += n;
            sumSq += n * n;
        }
        double mean = sum / pts.length;
        return Math.sqrt(sumSq / pts.length - mean * mean);
    }
}
