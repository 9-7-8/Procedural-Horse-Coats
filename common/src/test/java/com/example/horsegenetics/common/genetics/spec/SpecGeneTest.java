package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A gene loaded from JSON has to be indistinguishable from a hand-written one
 * everywhere downstream - the genotype code, breeding, the catalogue, the coat.
 * These tests register one, use it as an ordinary gene, and unregister.
 */
class SpecGeneTest {

    private static final int BUILT_IN_GENES = 38;

    @AfterEach
    void unregister() {
        Genes.clearLoaded();
    }

    private static SpecGene register(String file) {
        SpecGene gene = new SpecGene(GeneSpecParser.parse(GeneSpecParserTest.example(file), file));
        Genes.register(gene);
        return gene;
    }

    @Test
    void aLoadedGeneJoinsTheRegistryAndTheCode() {
        assertEquals(BUILT_IN_GENES, Genes.codeOrder().size());
        int catalogueBefore = GenotypeCatalog.size();
        int maskingBefore = maskingCombinations();

        SpecGene silver = register("silver.json"); // example.silver, priority 45

        assertEquals(BUILT_IN_GENES + 1, Genes.codeOrder().size());
        assertEquals(silver, Genes.byKey("example.silver"));
        // priority 45 sorts it into the one unified order, between the built-in
        // MATP (40) and champagne (50) - not appended after the built-ins.
        int i = Genes.codeOrder().indexOf(silver);
        assertEquals("horsegenetics.matp", Genes.codeOrder().get(i - 1).key());
        assertEquals("horsegenetics.champagne", Genes.codeOrder().get(i + 1).key());
        assertTrue(Genes.naturalOrder().contains(silver));
        assertFalse(Genes.magicalOrder().contains(silver));

        String code = Genotype.wildType().toCode();
        assertTrue(code.contains("example.silver=z/z"), "the wild type gains a segment: " + code);
        assertEquals(Genotype.wildType(), Genotype.parse(code));
        // Every unmasked entry doubles; each masking combination (KIT's
        // dominant white, EDNRB's lethal white, test) stays at one pen, because
        // while it shows nothing else is visible - including this gene.
        assertEquals((catalogueBefore - maskingBefore) * 2 + maskingBefore, GenotypeCatalog.size(),
                "a dominant two-allele gene doubles every unmasked pen");
    }

    @Test
    void magicalGenesLandBeforeTheMaskingBuiltIn() {
        SpecGene aurora = register("aurora.json");
        assertEquals(aurora, Genes.magicalOrder().get(Genes.magicalOrder().size() - 2));
        assertEquals(Genes.TEST, Genes.magicalOrder().get(Genes.magicalOrder().size() - 1),
                "Test paints flat and masks everything, so it stays last");
    }

    @Test
    void loadOrderDoesNotDecideGeneOrder() {
        register("tobiano.json");   // priority 80
        register("dun.json");       // priority 30
        assertOrder("example.dun", "example.tobiano");

        Genes.clearLoaded();
        register("dun.json");
        register("tobiano.json");
        assertOrder("example.dun", "example.tobiano");
    }

    /** {@code a} sorts before {@code b} in the one derived {@code codeOrder()}. */
    private static void assertOrder(String a, String b) {
        int ia = Genes.codeOrder().indexOf(Genes.byKey(a));
        int ib = Genes.codeOrder().indexOf(Genes.byKey(b));
        assertTrue(ia >= 0 && ib >= 0 && ia < ib,
                "priority decides, not registration order: " + a + "@" + ia + " " + b + "@" + ib);
    }

    /**
     * What "recessive" used to be a word for: two of the three combinations map
     * to a wild-type expression, and the gene never has to say so.
     */
    @Test
    void aCarrierCombinationLandsOnAWildTypeExpression() {
        SpecGene aurora = register("aurora.json");
        AllelePair carrier = new AllelePair(aurora.alleles().get(0), aurora.defaultAllele());
        AllelePair shows = new AllelePair(aurora.alleles().get(0), aurora.alleles().get(0));
        Genotype genotype = Genotype.of(carrier);

        assertFalse(aurora.isVisible(carrier, genotype));
        assertTrue(aurora.isVisible(shows, Genotype.of(shows)));
        assertEquals(1, aurora.dose(carrier));
        assertEquals(2, aurora.dose(shows));
    }

    @Test
    void aNaturalGeneOnlyAnswersRestrictAndAMagicalOneOnlyTint() {
        SpecGene silver = register("silver.json");
        SpecGene aurora = register("aurora.json");
        AllelePair silverPair = new AllelePair(silver.alleles().get(0), silver.defaultAllele());
        AllelePair auroraPair = new AllelePair(aurora.alleles().get(0), aurora.alleles().get(0));
        Genotype genotype = Genotype.of(silverPair, auroraPair);
        CoatBuildContext ctx = context(genotype);
        PigmentField coat = new PigmentField(HorseSkinGeometry.SHEET_SIZE);

        var silverExpression = silver.expressionOf(silverPair);
        var auroraExpression = aurora.expressionOf(auroraPair);
        var accumulator = new com.example.horsegenetics.common.coat.pattern
                .ColorField(HorseSkinGeometry.SHEET_SIZE);

        assertNotNull(silverExpression.restrict(ctx, coat));
        assertNull(silverExpression.tint(ctx, coat, accumulator));
        assertNull(auroraExpression.restrict(ctx, coat));
        assertNotNull(auroraExpression.tint(ctx, coat, accumulator));
    }

    @Test
    void aDilutionTakesBlackDownAndWalksTheSampleOffTheZeroRedColumn() {
        SpecGene silver = register("silver.json");
        AllelePair pair = new AllelePair(silver.alleles().get(0), silver.defaultAllele());
        Genotype genotype = Genotype.of(pair);
        PigmentField before = new PigmentField(HorseSkinGeometry.SHEET_SIZE);
        // A black point: all eumelanin, no pheomelanin - the case a naive
        // "scale black down" dilution leaves looking jet black anyway.
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            before.setRed(px, py, 0f);
            before.setBlack(px, py, 1f);
        });

        PigmentField after = silver.expressionOf(pair).restrict(context(genotype), before);

        int[] body = someTexelOn(Part.BODY);
        assertEquals(0.45f, after.black(body[0], body[1]), 1e-4);
        assertEquals(0.18f, after.red(body[0], body[1]), 1e-4,
                "blackTint feeds removed eumelanin back as red, so the point is brown not void");

        int[] mane = someTexelOn(Part.MANE);
        assertTrue(after.black(mane[0], mane[1]) < after.black(body[0], body[1]),
                "the second layer takes the mane further than the body");
    }

    /**
     * One layer, so the measurement can't pick up white from anywhere else -
     * this is about the {@code perLeg} + {@code spread} draw, nothing more.
     */
    private static final String SOCKS_ONLY = """
            { "format": 2, "key": "example.socks",
              "alleles": [ {"token":"S"}, {"token":"s"} ],
              "founders": { "S/S": 1, "S/s": 9, "s/s": 90 },
              "knobs": [ { "name": "sock", "min": 0.2, "max": 0.6, "per": "leg", "spread": 0.25 } ],
              "expressions": [
                { "id": "socks", "when": [ "S/S", "S/s" ],
                  "layers": [ { "name": "socks",
                                "masks": [ { "type": "AXIS", "parts": ["LEGS"], "axis": "Y",
                                             "space": "part", "from": 0.0, "to": "$sock",
                                             "softness": 0.05 } ],
                                "op": { "type": "SET_PIGMENT", "red": 0.0, "black": 0.0 } } ] },
                { "id": "wild", "wildType": true } ] }
            """;

    @Test
    void aPerLegKnobGivesFourDifferentSockHeights() {
        SpecGene socks = new SpecGene(GeneSpecParser.parse(SOCKS_ONLY, "socks.json"));
        Genes.register(socks);
        AllelePair pair = new AllelePair(socks.alleles().get(0), socks.defaultAllele());
        PigmentField coat = socks.expressionOf(pair).restrict(context(Genotype.of(pair)),
                new PigmentField(HorseSkinGeometry.SHEET_SIZE));

        double[] heights = new double[4];
        for (int i = 0; i < 4; i++) {
            heights[i] = whiteHeightOf(coat, com.example.horsegenetics.common.coat.pattern
                    .CoatRegions.LEGS.get(i));
        }
        boolean allEqual = true;
        for (int i = 1; i < 4; i++) {
            allEqual &= Math.abs(heights[i] - heights[0]) < 1e-6;
        }
        assertFalse(allEqual, "spread should stop the four socks landing level: "
                + java.util.Arrays.toString(heights));
        for (double h : heights) {
            assertTrue(h > 0.05 && h < 0.85, "a sock should be a sock, was " + h);
        }
    }

    /** The highest point on a leg the layer took to zero pigment, as a fraction of leg height. */
    private static double whiteHeightOf(PigmentField coat, Part leg) {
        HorseSkinGeometry.Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, leg);
        double[] best = {0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, leg, (px, py, part, face, point) -> {
            if (coat.red(px, py) < 0.02f && coat.black(px, py) < 0.02f) {
                best[0] = Math.max(best[0], (point.y() - b.yMin()) / b.span(Axis.Y));
            }
        });
        return best[0];
    }

    private static int[] someTexelOn(Part part) {
        int[] found = new int[2];
        boolean[] any = {false};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, part, (px, py, p, face, point) -> {
            if (!any[0]) {
                found[0] = px;
                found[1] = py;
                any[0] = true;
            }
        });
        if (!any[0]) {
            throw new IllegalStateException("no texels on " + part);
        }
        return found;
    }

    private static CoatBuildContext context(Genotype genotype) {
        return new CoatBuildContext(genotype, Epigenome.fromSeed(12345L), Skin.ADULT, true);
    }

    @Test
    void everyLoadedGeneStillAnswersTheWholeGeneInterface() {
        for (String file : new String[]{"silver.json", "dun.json", "tobiano.json", "aurora.json"}) {
            SpecGene gene = new SpecGene(GeneSpecParser.parse(GeneSpecParserTest.example(file), file));
            Gene asGene = gene;
            assertNotNull(asGene.key());
            assertFalse(asGene.expressions().isEmpty());
            assertEquals(gene.defaultAllele(), asGene.alleles().get(asGene.alleles().size() - 1));
            assertEquals(0, asGene.alleles().get(0).order());
            assertEquals(asGene.alleles().get(0), asGene.fromToken(asGene.alleles().get(0).token()));
            // the combination table is total: every pair resolves to a declared outcome
            for (var pair : GenotypeCatalog.allPairsOf(asGene)) {
                assertTrue(asGene.expressions().contains(asGene.expressionOf(pair)),
                        file + " " + pair.toTokens());
            }
        }
    }

    /** How many combinations anywhere in the registry hide every other gene. */
    private static int maskingCombinations() {
        int n = 0;
        for (var gene : Genes.codeOrder()) {
            for (var pair : GenotypeCatalog.distinctPairsOf(gene)) {
                if (gene.expressionOf(pair).masks()) {
                    n++;
                }
            }
        }
        return n;
    }
}
