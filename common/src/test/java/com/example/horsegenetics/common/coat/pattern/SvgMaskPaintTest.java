package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiRoll;
import com.example.horsegenetics.common.genetics.spec.GeneSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpecParser;
import com.example.horsegenetics.common.genetics.spec.SpecValues;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <b>{@code SVG} mask on an actual horse</b> - the half {@link SvgPathTest}
 * cannot see.
 *
 * <p>{@code SvgPathTest} proves the geometry: that the grammar parses and that
 * the measurements are the ones a browser would make. None of that says the
 * mask <i>lands anywhere</i>. The placement is three separate coordinate
 * changes stacked - viewBox into viewport, body-normalised into body units, and
 * SVG's downward y into the horse's upward one - and every one of them has a
 * failure that leaves the drawing correct and invisible: off the end of the
 * horse, inside out, or a thousand times too small.
 *
 * <p>So these paint a real sheet and count texels. The assertions are
 * deliberately coarse - "some of the barrel, not all of it, and the right way
 * up" - because the precise number is a golden file's business and what is
 * worth pinning here is that the transform chain has the right sign and the
 * right scale.
 */
class SvgMaskPaintTest {

    /** A magical one-outcome gene whose only layer is one SVG mask. */
    private static String gene(String mask) {
        return "{\n"
                + "  \"format\": 3,\n"
                + "  \"key\": \"test.svgmask\",\n"
                + "  \"name\": \"SVG Mask Test\",\n"
                + "  \"phase\": \"magical\",\n"
                + "  \"priority\": 900,\n"
                + "  \"alleles\": [ { \"token\": \"S\", \"label\": \"S\" }, { \"token\": \"n\", \"label\": \"n\" } ],\n"
                + "  \"expressions\": [\n"
                + "    { \"id\": \"on\", \"name\": \"On\", \"description\": \"x\", \"when\": [\"S/S\", \"S/n\"],\n"
                + "      \"layers\": [ { \"name\": \"the drawing\", \"masks\": [ " + mask + " ],\n"
                + "        \"op\": { \"type\": \"FLAT\", \"color\": \"#ff0000\" } } ] },\n"
                + "    { \"id\": \"wild\", \"name\": \"Wild\", \"description\": \"nothing\", \"wildType\": true }\n"
                + "  ],\n"
                + "  \"founders\": { \"S/n\": 1.0, \"n/n\": 99.0 }\n"
                + "}\n";
    }

    /**
     * What the layer covered: how many texels, and the <b>body-space</b> Y range
     * they sit in.
     *
     * <p>Body space rather than sheet space, which is the whole reason this
     * helper is not two lines. The sheet is an atlas - the barrel's six faces
     * are packed wherever they fit - so a texel's {@code py} says nothing about
     * whether it is high on the horse. Asking the geometry where each covered
     * texel actually <i>is</i> is the only reading that means "up".
     */
    private static Cover paint(String mask) {
        GeneSpec spec = GeneSpecParser.parse(gene(mask), "svgmask.json");
        SpecValues values = SpecValues.read(spec,
                EpiRoll.founder(SpecValues.schema(spec), new SeededRng(7L, spec.key())), 1);
        CoatBuildContext ctx = new CoatBuildContext(
                Genotype.wildType(), Epigenome.fromSeed(7L), Skin.ADULT, true);
        ColorField colour = new ColorField(HorseSkinGeometry.SHEET_SIZE);
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                colour.set(px, py, 255, 40, 40, 40));
        ColorField delta = SpecPainter.tint(spec, spec.expressions().get(0).layers(), values,
                ctx, new PigmentField(HorseSkinGeometry.SHEET_SIZE), colour);

        int[] count = {0};
        double[] range = {Double.MAX_VALUE, -Double.MAX_VALUE};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (delta.red(px, py) > 20) {
                count[0]++;
                range[0] = Math.min(range[0], point.y());
                range[1] = Math.max(range[1], point.y());
            }
        });
        return new Cover(count[0], range[0], range[1]);
    }

    /** Texels covered, and how far up the horse they reached. */
    private record Cover(int texels, double lowY, double highY) {}

    /**
     * A filled square over the middle of the flank covers a real, bounded piece
     * of the horse.
     *
     * <p>Zero would mean the placement chain lost it; the whole sheet would mean
     * the fill rule inverted. Both are one sign away from correct and neither
     * would fail a geometry test.
     */
    @Test
    void aFilledSquareCoversPartOfTheBarrelAndNotAllOfIt() {
        Cover got = paint("{ \"type\": \"SVG\", \"parts\": [\"BODY\"], "
                + "\"d\": \"M 20 20 H 80 V 80 H 20 Z\", \"viewBox\": [0, 0, 100, 100], "
                + "\"originU\": 0.3, \"originV\": 0.38, \"sizeU\": 0.2, \"sizeV\": 0.2 }");
        assertTrue(got.texels() > 40, "the drawing painted almost nothing - " + got.texels() + " texels");
        assertTrue(got.texels() < 2000, "the drawing painted the whole horse - " + got.texels() + " texels");
    }

    /**
     * <b>The y flip is real and it is the default.</b>
     *
     * <p>A shape in the TOP half of the drawing has to land on the UPPER half of
     * the flank, because SVG's y runs down the page and the horse's runs up.
     * Getting it wrong draws a perfectly good marking in the mirror position,
     * which reads as a deliberate choice and is the reason this has its own
     * case rather than a comment.
     */
    @Test
    void theDrawingLandsTheRightWayUp() {
        String viewport = "\"viewBox\": [0, 0, 100, 100], \"originU\": 0.3, \"originV\": 0.3, "
                + "\"sizeU\": 0.2, \"sizeV\": 0.25";
        // In the drawing, y 5..25 is the top band and y 75..95 the bottom one.
        Cover top = paint("{ \"type\": \"SVG\", \"parts\": [\"BODY\"], "
                + "\"d\": \"M 20 5 H 80 V 25 H 20 Z\", " + viewport + " }");
        Cover bottom = paint("{ \"type\": \"SVG\", \"parts\": [\"BODY\"], "
                + "\"d\": \"M 20 75 H 80 V 95 H 20 Z\", " + viewport + " }");
        assertTrue(top.texels() > 0 && bottom.texels() > 0, "both bands should paint something");
        assertTrue(top.lowY() > bottom.highY(),
                "the top of the drawing landed at body y " + top.lowY() + " and the bottom of it at "
                        + bottom.highY() + " - the y flip is inverted");

        // And turning the flip off swaps them, which is the whole of what the
        // flag does.
        Cover unflipped = paint("{ \"type\": \"SVG\", \"parts\": [\"BODY\"], \"flipY\": false, "
                + "\"d\": \"M 20 5 H 80 V 25 H 20 Z\", " + viewport + " }");
        assertTrue(unflipped.highY() < top.lowY(), "flipY: false should put the same band lower down");
    }

    /**
     * A stroke is as wide as it says in <b>body units</b>, whatever the drawing
     * was scaled by - which is the reason the viewBox fit is done at paint time
     * and the stroke is not scaled with it. A drawing authored on a 1000-unit
     * canvas and one on a 10-unit canvas get the same line on the horse.
     */
    @Test
    void aStrokeIsTheSameWidthWhateverTheDrawingWasScaledBy() {
        Cover small = paint("{ \"type\": \"SVG\", \"parts\": [\"BODY\"], \"fill\": false, "
                + "\"d\": \"M 2 5 H 8\", \"viewBox\": [0, 0, 10, 10], \"width\": 1.2, "
                + "\"originU\": 0.3, \"originV\": 0.4, \"sizeU\": 0.2, \"sizeV\": 0.2 }");
        Cover large = paint("{ \"type\": \"SVG\", \"parts\": [\"BODY\"], \"fill\": false, "
                + "\"d\": \"M 200 500 H 800\", \"viewBox\": [0, 0, 1000, 1000], \"width\": 1.2, "
                + "\"originU\": 0.3, \"originV\": 0.4, \"sizeU\": 0.2, \"sizeV\": 0.2 }");
        assertTrue(small.texels() > 0, "the small-canvas stroke painted nothing");
        assertEquals(small.texels(), large.texels(),
                "the same line on a canvas 100x bigger should be the same stroke on the horse");
    }

    /**
     * The dash pattern breaks the stroke, and the broken one covers less than
     * the solid one - measured rather than asserted by eye, because a dash
     * measured in the wrong space either does nothing or erases the line
     * completely, and both look plausible in a screenshot.
     */
    @Test
    void aDashPatternBreaksTheStroke() {
        String line = "\"d\": \"M 5 50 H 95\", \"viewBox\": [0, 0, 100, 100], \"fill\": false, "
                + "\"width\": 1.0, \"originU\": 0.25, \"originV\": 0.4, \"sizeU\": 0.3, \"sizeV\": 0.1";
        Cover solid = paint("{ \"type\": \"SVG\", \"parts\": [\"BODY\"], " + line + " }");
        Cover dashed = paint("{ \"type\": \"SVG\", \"parts\": [\"BODY\"], " + line
                + ", \"dash\": 1.0, \"gap\": 1.0 }");
        assertTrue(solid.texels() > 0, "the solid line painted nothing");
        assertTrue(dashed.texels() < solid.texels(),
                "the dashed line should cover less than the solid one");
        assertTrue(dashed.texels() > solid.texels() / 8,
                "and should not have erased the line entirely");
    }

    /** A bad {@code d} is a load error naming the file and the layer, not a blank horse. */
    @Test
    void aBadPathIsALoadErrorAndNotASilentlyEmptyMask() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(gene("{ \"type\": \"SVG\", \"d\": \"M 0 0 Q\" }"),
                        "svgmask.json"));
        assertTrue(e.getMessage().contains("SVG 'd'"), e.getMessage());

        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(gene("{ \"type\": \"SVG\" }"), "svgmask.json"));
        assertTrue(missing.getMessage().contains("'d'"), missing.getMessage());
    }
}
