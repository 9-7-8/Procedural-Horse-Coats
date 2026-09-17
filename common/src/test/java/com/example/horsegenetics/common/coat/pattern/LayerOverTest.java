package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpecParser;
import com.example.horsegenetics.common.genetics.spec.SpecValues;
import com.example.horsegenetics.common.genetics.epi.EpiRoll;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.SeededRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>{@code "over": true} makes a layer stack instead of sum.</b>
 *
 * <p>The gene under test is the shape that keeps going wrong in real files: a
 * pale disc with a dark core drawn inside it. Summed - which is what every
 * layer did before this flag - the core's pull toward black and the disc's pull
 * toward white are both measured from the <i>original</i> coat and added, so
 * the middle comes out pale-plus-dark rather than dark. Stacked, the core
 * measures from the pale disc the layer above it just painted, and lands on the
 * colour it asked for.
 *
 * <p>Both halves are asserted, because the whole argument for the flag is that
 * it is opt-in: the summing case has to keep summing exactly as before, or
 * every shipped gene moves.
 */
class LayerOverTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** A white disc over the barrel, with a black core inside it. */
    private static String gene(boolean over) {
        return "{\n"
                + "  \"format\": 3,\n"
                + "  \"key\": \"test.over\",\n"
                + "  \"name\": \"Over Test\",\n"
                + "  \"phase\": \"magical\",\n"
                + "  \"priority\": 900,\n"
                + "  \"alleles\": [ { \"token\": \"O\" }, { \"token\": \"n\" } ],\n"
                + "  \"expressions\": [\n"
                + "    { \"id\": \"on\", \"name\": \"On\", \"description\": \"a core in a disc\","
                + " \"when\": [\"O/O\", \"O/n\"],\n"
                + "      \"layers\": [\n"
                + "        { \"name\": \"the pale disc\",\n"
                + "          \"masks\": [ { \"type\": \"AXIS\", \"parts\": [\"BODY\"], \"axis\": \"Y\",\n"
                + "            \"space\": \"part\", \"from\": 0.0, \"to\": 1.0, \"softness\": 0.0 } ],\n"
                + "          \"op\": { \"type\": \"TOWARD\", \"color\": \"#ffffff\", \"strength\": 100 } },\n"
                + "        { \"name\": \"the dark core\",\n"
                + (over ? "          \"over\": true,\n" : "")
                + "          \"masks\": [ { \"type\": \"AXIS\", \"parts\": [\"BODY\"], \"axis\": \"Y\",\n"
                + "            \"space\": \"part\", \"from\": 0.0, \"to\": 1.0, \"softness\": 0.0 } ],\n"
                + "          \"op\": { \"type\": \"TOWARD\", \"color\": \"#000000\", \"strength\": 100 } }\n"
                + "      ] },\n"
                + "    { \"id\": \"wild\", \"name\": \"Wild\", \"description\": \"nothing\","
                + " \"wildType\": true }\n"
                + "  ],\n"
                + "  \"founders\": { \"O/n\": 1.0, \"n/n\": 99.0 }\n"
                + "}\n";
    }

    /**
     * The finished colour of one BODY texel - the accumulator with this gene's
     * delta folded in, which is what the composer will hand the texture.
     */
    private static int[] painted(boolean over) {
        GeneSpec spec = GeneSpecParser.parse(gene(over), "over.json");
        EpiValues epi = EpiRoll.founder(SpecValues.schema(spec), new SeededRng(5L, spec.key()));
        SpecValues values = SpecValues.read(spec, epi, 1);
        CoatBuildContext ctx = new CoatBuildContext(
                Genotype.wildType(), Epigenome.fromSeed(5L), Skin.ADULT, true);

        ColorField colour = new ColorField(N);
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                colour.set(px, py, 255, 40, 40, 40));

        ColorField delta = SpecPainter.tint(spec, spec.expressions().get(0).layers(), values,
                ctx, new PigmentField(N), colour);
        colour.apply(delta);

        int[] seen = new int[3];
        int[] found = {0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (found[0] == 0 && part == HorseSkinGeometry.Part.BODY) {
                seen[0] = colour.red(px, py);
                seen[1] = colour.green(px, py);
                seen[2] = colour.blue(px, py);
                found[0] = 1;
            }
        });
        assertEquals(1, found[0], "the test needs at least one BODY texel");
        return seen;
    }

    /**
     * Stacked, the core lands on the black it asked for: it measures from the
     * white the disc painted, so its pull is the whole way down.
     */
    @Test
    void anOverLayerPaintsOnTopOfTheOneAboveIt() {
        int[] rgb = painted(true);
        assertTrue(rgb[0] <= 8 && rgb[1] <= 8 && rgb[2] <= 8,
                "a core over a white disc should be black, was "
                        + rgb[0] + "," + rgb[1] + "," + rgb[2]);
    }

    /**
     * Summed - the old behaviour, and still the default - the two pulls are both
     * measured from the grey coat and added, so they very nearly cancel and the
     * core is nothing like black. This is the bug the flag exists to let an
     * author avoid, asserted here so that it cannot change by accident.
     */
    @Test
    void withoutTheFlagTheLayersStillSum() {
        int[] rgb = painted(false);
        assertTrue(rgb[0] > 8 || rgb[1] > 8 || rgb[2] > 8,
                "without 'over' the pulls sum and the core is not black, was "
                        + rgb[0] + "," + rgb[1] + "," + rgb[2]);
        assertEquals(painted(false)[0], rgb[0], "summing must be deterministic");
    }

    /** The two are not the same picture - which is the whole point of the flag. */
    @Test
    void theFlagChangesTheResult() {
        int[] stacked = painted(true);
        int[] summed = painted(false);
        assertTrue(stacked[0] != summed[0] || stacked[1] != summed[1] || stacked[2] != summed[2],
                "over and summed produced the same colour, so the flag did nothing");
    }
}
