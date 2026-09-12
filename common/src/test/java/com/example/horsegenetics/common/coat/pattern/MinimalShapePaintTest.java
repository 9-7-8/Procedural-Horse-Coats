package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiRoll;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpecParser;
import com.example.horsegenetics.common.genetics.spec.SpecValues;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>A marking that erases down</b> - {@code PATH}'s {@code pointsMin} on an
 * actual horse, driven by the gene's dial knob.
 *
 * <p>{@link PathMaskTest} proves the morph as geometry: that the blend of two
 * point arrays is the shape halfway between them. What it cannot see is the
 * wiring, which is where the interesting failures are. The dial knob is
 * drawn from the epigenome, normalised against <i>its own</i> declared range,
 * and handed to the mask - three steps, each of which can be correct in
 * isolation and wrong together. A knob read raw instead of normalised paints a
 * plausible marking of the wrong size; a blend wired backwards paints the
 * minimal shape on the horse showing most of the gene.
 *
 * <p>So these paint real sheets at known dial values and count texels.
 * The marking has to <b>grow with the dial, in that direction</b>, and the
 * two ends have to be the two shapes as drawn.
 */
class MinimalShapePaintTest {

    /** A one-outcome magical gene whose only layer is a PATH that erases down. */
    private static final String GENE = "{\n"
            + "  \"format\": 3,\n"
            + "  \"key\": \"test.minimal\",\n"
            + "  \"name\": \"Minimal Shape Test\",\n"
            + "  \"phase\": \"magical\",\n"
            + "  \"priority\": 900,\n"
            + "  \"alleles\": [ { \"token\": \"M\" }, { \"token\": \"n\" } ],\n"
            + "  \"knobs\": [ { \"name\": \"extent\", \"min\": 0.0, \"max\": 2.0,"
            + " \"dial\": true } ],\n"
            + "  \"expressions\": [\n"
            + "    { \"id\": \"on\", \"name\": \"On\", \"description\": \"a stripe\","
            + " \"when\": [\"M/M\", \"M/n\"],\n"
            + "      \"layers\": [ { \"name\": \"stripe\", \"masks\": [ { \"type\": \"PATH\",\n"
            + "        \"parts\": [\"BODY\"], \"plane\": \"side\", \"space\": \"body\",\n"
            + "        \"points\":    [0.25, 0.45, 0.75, 0.45],\n"
            + "        \"pointsMin\": [0.49, 0.45, 0.51, 0.45],\n"
            + "        \"width\": 1.5, \"softness\": 0.05 } ],\n"
            + "        \"op\": { \"type\": \"FLAT\", \"color\": \"#ff0000\" } } ] },\n"
            + "    { \"id\": \"wild\", \"name\": \"Wild\", \"description\": \"nothing\","
            + " \"wildType\": true }\n"
            + "  ],\n"
            + "  \"founders\": { \"M/n\": 1.0, \"n/n\": 99.0 }\n"
            + "}\n";

    private static final GeneSpec SPEC = GeneSpecParser.parse(GENE, "minimal.json");

    /**
     * How many texels the stripe covered with the dial knob pinned at
     * {@code extent}.
     *
     * <p>Pinned rather than rolled: the point of the test is the relationship
     * between the number and the marking, so the number has to be chosen. The
     * rest of the draw is a real founder roll, so nothing else about the horse
     * is special.
     */
    private static int cover(double extent) {
        EpiValues epi = EpiRoll.founder(SpecValues.schema(SPEC), new SeededRng(11L, SPEC.key()))
                .with("extent", extent);
        SpecValues values = SpecValues.read(SPEC, epi, 1);
        CoatBuildContext ctx = new CoatBuildContext(
                Genotype.wildType(), Epigenome.fromSeed(11L), Skin.ADULT, true);
        ColorField colour = new ColorField(HorseSkinGeometry.SHEET_SIZE);
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                colour.set(px, py, 255, 40, 40, 40));
        ColorField delta = SpecPainter.tint(SPEC, SPEC.expressions().get(0).layers(), values,
                ctx, new PigmentField(HorseSkinGeometry.SHEET_SIZE), colour);

        int[] count = {0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (delta.red(px, py) > 20) {
                count[0]++;
            }
        });
        return count[0];
    }

    /**
     * The knob is read as <b>a fraction of its own range</b>, not raw. This
     * gene's range is 0 to 2, so a horse that drew 1.0 is showing half of the
     * gene - and a raw reading would call that "fully expressed and then some".
     */
    @Test
    void theKnobIsNormalisedAgainstItsOwnRange() {
        assertEquals(0.0, dialAt(0.0), 1e-9);
        assertEquals(0.5, dialAt(1.0), 1e-9);
        assertEquals(1.0, dialAt(2.0), 1e-9);
    }

    private static double dialAt(double extent) {
        EpiValues epi = EpiRoll.founder(SpecValues.schema(SPEC), new SeededRng(11L, SPEC.key()))
                .with("extent", extent);
        return SpecValues.read(SPEC, epi, 1).dial();
    }

    /**
     * <b>The marking grows with the dial</b>, and grows the right way round.
     * Checked as a ladder rather than at the ends alone, because a blend that is
     * inverted only in the middle - a sign error inside the spline, say - would
     * pass a two-point test.
     */
    @Test
    void theMarkingGrowsWithTheDialKnob() {
        int previous = -1;
        for (double extent = 0.0; extent <= 2.0001; extent += 0.25) {
            int texels = cover(extent);
            assertTrue(texels >= previous,
                    "the stripe shrank as the dial rose - at extent " + extent
                            + " it covers " + texels + " texels, against " + previous + " below it");
            previous = texels;
        }
    }

    /**
     * The two ends are real markings and different sizes - the test that says
     * the morph is wired at all. A minimal shape that never reached the painter
     * would make these two numbers equal.
     */
    @Test
    void theWeakestEndIsASmallerMarkingAndNotAnAbsentOne() {
        int least = cover(0.0);
        int most = cover(2.0);
        assertTrue(least > 0, "the weakest horse paints nothing at all");
        assertTrue(most > least * 2,
                "the two ends are barely different - " + least + " texels against " + most
                        + " - so the shape is probably not morphing");
    }
}
