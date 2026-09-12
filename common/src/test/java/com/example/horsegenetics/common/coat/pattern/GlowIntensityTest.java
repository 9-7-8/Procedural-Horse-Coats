package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>How brightly a layer glows</b> - roadmap &sect;10's glow intensity.
 *
 * <p>Emissiveness was one bit per texel, cut at a coverage threshold of 0.5.
 * That threshold was a stand-in: it existed so a soft-edged mask would not
 * bloom a glow two body units wider than the shape that drew it, and it paid
 * for that with a hard line across a soft edge. A glow is a <i>level</i> now -
 * the emissive pass blends rather than replaces, so a fraction means something
 * - and coverage scales it, which is the falloff the author already drew.
 *
 * <p>What these pin is the arithmetic of that, since the picture is in a
 * render nobody can assert on: the level equals the coverage times what the
 * layer declared, two genes over one texel take the brighter, and the format
 * refuses the two things it always refused.
 */
class GlowIntensityTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** A magical gene whose one layer glows at {@code level} over the body. */
    private static String gene(String level, String knobs) {
        return "{\n"
                + "  \"format\": 3,\n"
                + "  \"key\": \"test.glow\",\n"
                + "  \"name\": \"Glow Test\",\n"
                + "  \"phase\": \"magical\",\n"
                + "  \"priority\": 900,\n"
                + "  \"alleles\": [ { \"token\": \"G\" }, { \"token\": \"n\" } ],\n"
                + "  \"knobs\": [" + knobs + "],\n"
                + "  \"expressions\": [\n"
                + "    { \"id\": \"on\", \"name\": \"On\", \"description\": \"a lit band\","
                + " \"when\": [\"G/G\", \"G/n\"],\n"
                + "      \"layers\": [ { \"name\": \"band\", \"emissive\": " + level + ",\n"
                + "        \"masks\": [ { \"type\": \"AXIS\", \"parts\": [\"BODY\"], \"axis\": \"X\",\n"
                + "          \"space\": \"part\", \"from\": 0.65, \"to\": 1.0, \"softness\": 0.6 } ],\n"
                + "        \"op\": { \"type\": \"FLAT\", \"color\": \"#40e0ff\" } } ] },\n"
                + "    { \"id\": \"wild\", \"name\": \"Wild\", \"description\": \"nothing\","
                + " \"wildType\": true }\n"
                + "  ],\n"
                + "  \"founders\": { \"G/n\": 1.0, \"n/n\": 99.0 }\n"
                + "}\n";
    }

    /**
     * The brightest and dimmest lit texels this gene produces, and how many
     * there are. The {@code AXIS} band is deliberately soft-edged - it runs to
     * the end of the barrel and fades over a long 0.6 - so its coverage arrives
     * at every value between 0 and 1 and one bake paints the whole range of
     * levels at once, which is the thing worth measuring.
     */
    private static double[] glow(String level, String knobs) {
        GeneSpec spec = GeneSpecParser.parse(gene(level, knobs), "glow.json");
        EpiValues epi = EpiRoll.founder(SpecValues.schema(spec), new SeededRng(3L, spec.key()));
        SpecValues values = SpecValues.read(spec, epi, 1);
        CoatOverlay overlay = new CoatOverlay(Skin.ADULT, new int[N * N]);
        SpecPainter.emissive(spec, spec.expressions().get(0).layers(), values, Skin.ADULT, overlay);

        float[] mask = overlay.emissiveMask();
        if (mask == null) {
            return new double[]{0, 0, 0};
        }
        double lowest = 2;
        double highest = 0;
        int lit = 0;
        for (float v : mask) {
            if (v > 0) {
                lit++;
                lowest = Math.min(lowest, v);
                highest = Math.max(highest, v);
            }
        }
        return new double[]{lit == 0 ? 0 : lowest, highest, lit};
    }

    /**
     * <b>A soft edge glows soft.</b> The band ramps its coverage from 0 to 1
     * across the barrel, so the glow it leaves has to arrive in levels rather
     * than as one flat sheet of lit texels - which is what the old threshold
     * produced, and the whole of what changed.
     */
    @Test
    void coverageScalesTheGlow() {
        double[] got = glow("true", "");
        assertTrue(got[2] > 0, "nothing glowed at all");
        assertTrue(got[1] > 0.9, "the fully covered end should be near full bright, got " + got[1]);
        assertTrue(got[0] < 0.25,
                "the barely covered end should be barely lit, got " + got[0]
                        + " - a threshold would make every lit texel equal");
    }

    /** A declared level is a ceiling: the brightest texel is exactly it. */
    @Test
    void theDeclaredLevelIsTheBrightestItGets() {
        double[] half = glow("0.5", "");
        assertEquals(0.5, half[1], 0.01, "a layer at 0.5 should peak at half bright");
        double[] full = glow("true", "");
        assertTrue(full[1] > half[1], "0.5 is not dimmer than true");
        assertEquals(full[2], half[2], "dimming a glow should not change WHICH texels are lit");
    }

    /** {@code true} is the spelling for 1, and reads as exactly that. */
    @Test
    void trueMeansAllOfIt() {
        assertEquals(glow("1", "")[1], glow("true", "")[1], 1e-6);
    }

    /** A layer that says it does not glow leaves the pass empty. */
    @Test
    void falseAndAbsentBothMeanNoGlow() {
        assertEquals(0.0, glow("false", "")[2]);
    }

    /** Driven by a knob, so one horse smoulders and the next blazes. */
    @Test
    void theLevelCanBeAKnob() {
        String knob = "{ \"name\": \"burn\", \"min\": 0.3, \"max\": 0.3 }";
        assertEquals(0.3, glow("\"$burn\"", knob)[1], 0.01,
                "a glow pointed at a knob should burn at what the knob drew");
    }

    /** The two refusals the format always had, now on a Value rather than a flag. */
    @Test
    void aNaturalGeneCannotGlow() {
        String json = gene("true", "").replace("\"phase\": \"magical\"", "\"phase\": \"natural\"")
                .replace("{ \"type\": \"FLAT\", \"color\": \"#40e0ff\" }",
                        "{ \"type\": \"WHITEN\", \"amount\": 1 }");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "natural.json"));
        assertTrue(e.getMessage().contains("pigment does not glow"), e.getMessage());
    }

    /** And a level outside the scale, which is always a misunderstanding of it. */
    @Test
    void aLevelOutsideZeroToOneIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(gene("15", ""), "bright.json"));
        assertTrue(e.getMessage().contains("between 0 and 1"), e.getMessage());
        assertTrue(e.getMessage().contains("fraction of full bright"),
                "the message should say what the scale IS: " + e.getMessage());
    }

    /**
     * Two genes over one texel take the <b>brighter</b>, not the sum. There is
     * nothing above full bright to spend an overlap on, and summing would make
     * a horse carrying two dim glows brighter than one carrying a bright one.
     */
    @Test
    void twoGlowsOverOneTexelTakeTheBrighter() {
        CoatOverlay overlay = new CoatOverlay(Skin.ADULT, new int[N * N]);
        int px = 20;
        int py = 20;
        overlay.markEmissive(px, py, 0.3);
        overlay.markEmissive(px, py, 0.7);
        overlay.markEmissive(px, py, 0.4);
        assertEquals(0.7, overlay.emissiveMask()[py * N + px], 1e-6);
    }

    /** A level of zero is not a lit texel, so it never reaches the renderer. */
    @Test
    void zeroIsNotLit() {
        CoatOverlay overlay = new CoatOverlay(Skin.ADULT, new int[N * N]);
        overlay.markEmissive(10, 10, 0.0);
        assertEquals(null, overlay.emissiveMask(),
                "a horse whose only glow is zero should skip the emissive pass entirely");
    }

    /** Out-of-range levels clamp at the overlay rather than escaping into alpha. */
    @Test
    void theOverlayClampsWhatItIsHanded() {
        CoatOverlay overlay = new CoatOverlay(Skin.ADULT, new int[N * N]);
        overlay.markEmissive(5, 5, 4.0);
        assertEquals(1.0, overlay.emissiveMask()[5 * N + 5], 1e-6);
    }

    /** The whole-part helper still lights outright - a part glow has no level. */
    @Test
    void aWholePartStillLightsFully() {
        CoatOverlay overlay = new CoatOverlay(Skin.ADULT, new int[N * N]);
        overlay.markEmissivePart(Part.MANE);
        float[] mask = overlay.emissiveMask();
        boolean any = false;
        for (float v : mask) {
            if (v > 0) {
                assertEquals(1.0, v, 1e-6, "a part glow should be full bright");
                any = true;
            }
        }
        assertTrue(any, "the mane did not light");
    }
}
