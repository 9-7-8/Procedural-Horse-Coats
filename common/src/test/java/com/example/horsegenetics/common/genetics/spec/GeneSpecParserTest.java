package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parser's job is to be strict and to say <i>why</i>. A gene file is written
 * by a tool and read by a person debugging at midnight, so every rejection here
 * asserts on the message as well as the failure.
 */
class GeneSpecParserTest {

    /** A minimal well-formed gene, so a test only has to state the part it is about. */
    private static String gene(String body) {
        return "{ \"format\": 2, \"key\": \"example.probe\","
                + " \"alleles\": [ {\"token\":\"A\"}, {\"token\":\"a\"} ],"
                + " \"founders\": { \"A/A\": 1, \"A/a\": 9, \"a/a\": 90 }"
                + body + " }";
    }

    static String example(String name) {
        String resource = "/horsegenetics/example-genes/" + name;
        try (InputStream in = GeneSpecParserTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static GeneSpec.ExpressionSpec named(GeneSpec spec, String id) {
        GeneSpec.ExpressionSpec e = spec.expression(id);
        assertTrue(e != null, spec.key() + " has no expression '" + id + "'");
        return e;
    }

    @Test
    void readsASimpleDilution() {
        GeneSpec spec = GeneSpecParser.parse(example("silver.json"), "silver.json");

        assertEquals("example.silver", spec.key());
        assertTrue(spec.natural());
        assertEquals(2, spec.alleles().size());
        assertEquals("Z", spec.variant().token());
        assertEquals("z", spec.baseline().token());
        assertEquals(2, spec.expressions().size());
        assertEquals(2, named(spec, "silver").layers().size());
        assertTrue(named(spec, "wild").wildType());
        assertTrue(spec.isDeterministic(), "no knobs means every carrier bakes the same coat");
    }

    /** Both variant combinations land on one outcome; the baseline pair on the other. */
    @Test
    void mapsEveryCombinationToExactlyOneExpression() {
        GeneSpec spec = GeneSpecParser.parse(example("silver.json"), "silver.json");
        assertEquals(List.of("Z/Z", "Z/z"), named(spec, "silver").combinations());
        assertEquals(List.of("z/z"), named(spec, "wild").combinations());
    }

    /** The catch-all takes whatever the explicit entries did not claim. */
    @Test
    void anExpressionWithNoWhenCatchesTheRest() {
        GeneSpec spec = GeneSpecParser.parse(example("dun.json"), "dun.json");
        assertEquals(List.of("D/D", "D/d"), named(spec, "dun").combinations());
        assertEquals(List.of("d/d"), named(spec, "wild").combinations());
        assertTrue(named(spec, "wild").isCatchAll() || named(spec, "wild").combinations().size() == 1);
    }

    /** {@code "when": {"Aur": 2}} expands to every combination with that copy count. */
    @Test
    void aCountTableSelectsByDose() {
        GeneSpec spec = GeneSpecParser.parse(example("aurora.json"), "aurora.json");
        assertEquals(List.of("Aur/Aur"), named(spec, "aurora").combinations());
        assertEquals(List.of("Aur/n"), named(spec, "carrier").combinations());
        assertTrue(named(spec, "carrier").wildType(), "a single copy of a carrier allele shows nothing");
        assertEquals(List.of("n/n"), named(spec, "wild").combinations());
    }

    @Test
    void readsTheFounderTable() {
        GeneSpec spec = GeneSpecParser.parse(example("dun.json"), "dun.json");
        assertEquals(3, spec.founders().size());
        double total = spec.founders().stream().mapToDouble(GeneSpec.FounderWeight::percent).sum();
        assertEquals(100.0, total, 1e-6);
    }

    @Test
    void expandsPartGroups() {
        GeneSpec spec = GeneSpecParser.parse(example("silver.json"), "silver.json");
        assertEquals(List.of(Part.MANE, Part.TAIL),
                named(spec, "silver").layers().get(1).masks().get(0).params().parts("parts"));
    }

    @Test
    void resolvesKnobReferencesAndPerLegDraws() {
        GeneSpec spec = GeneSpecParser.parse(example("dun.json"), "dun.json");

        assertEquals(3, spec.knobs().size());
        assertTrue(spec.knobs().get(0).seed());
        assertTrue(spec.knobs().get(1).perLeg());
        assertEquals(0.18, spec.knobs().get(1).spread(), 1e-9);
        assertFalse(spec.isDeterministic());

        GeneSpec.Value to = named(spec, "dun").layers().get(2).masks().get(1).params().value("to", 1);
        assertEquals(new GeneSpec.Value.FromKnob(1), to);
    }

    @Test
    void readsPerDoseTriples() {
        GeneSpec spec = GeneSpecParser.parse(example("tobiano.json"), "tobiano.json");
        GeneSpec.Value threshold =
                named(spec, "tobiano").layers().get(0).masks().get(0).params().value("threshold", 0.5);
        assertEquals(new GeneSpec.Value.PerDose(1.0, 0.62, 0.44), threshold);
    }

    @Test
    void readsAMagicalGene() {
        GeneSpec spec = GeneSpecParser.parse(example("aurora.json"), "aurora.json");
        assertFalse(spec.natural());
        assertEquals(0x2ee6c1, named(spec, "aurora").layers().get(0).op().params().color("color", 0));
    }

    // --- what it refuses ------------------------------------------------

    @Test
    void rejectsAnUnknownParameter() {
        String json = gene("""
                , "expressions": [ { "id": "v", "when": ["A/A", "A/a"],
                    "layers": [ { "masks": [ { "type": "STRIPES", "spacng": 3 } ],
                                  "op": { "type": "RESTRICT", "black": 0.5 } } ] },
                  { "id": "wild", "wildType": true } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "typo.json"));
        assertTrue(e.getMessage().contains("spacng"), e.getMessage());
        assertTrue(e.getMessage().contains("spacing"), "the message should list the legal keys: " + e.getMessage());
    }

    @Test
    void rejectsAMagicalOpOnANaturalGene() {
        String json = gene("""
                , "phase": "natural",
                  "expressions": [ { "id": "v", "when": ["A/A", "A/a"],
                    "layers": [ { "masks": [], "op": { "type": "TINT", "blue": 100 } } ] },
                  { "id": "wild", "wildType": true } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "mixed.json"));
        assertTrue(e.getMessage().contains("never both"), e.getMessage());
    }

    @Test
    void rejectsAnUndeclaredKnobReference() {
        String json = gene("""
                , "expressions": [ { "id": "v", "when": ["A/A", "A/a"],
                    "layers": [ { "masks": [], "op": { "type": "RESTRICT", "black": "$nope" } } ] },
                  { "id": "wild", "wildType": true } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "ghost.json"));
        assertTrue(e.getMessage().contains("no knob named 'nope'"), e.getMessage());
    }

    @Test
    void rejectsAlleleTokensThatWouldBreakAGenotypeCode() {
        String json = """
                { "format": 2, "key": "example.bad",
                  "alleles": [ {"token":"A/B"}, {"token":"a"} ],
                  "founders": { "a/a": 100 },
                  "expressions": [ { "id": "wild", "wildType": true } ] }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad.json"));
        assertTrue(e.getMessage().contains("'/'"), e.getMessage());
    }

    @Test
    void rejectsAKeyWithoutANamespace() {
        String json = """
                { "format": 2, "key": "silver",
                  "alleles": [ {"token":"A"}, {"token":"a"} ],
                  "founders": { "a/a": 100 },
                  "expressions": [ { "id": "wild", "wildType": true } ] }
                """;
        assertThrows(IllegalArgumentException.class, () -> GeneSpecParser.parse(json, "bad.json"));
    }

    @Test
    void rejectsAnUnknownPart() {
        String json = gene("""
                , "expressions": [ { "id": "v", "when": ["A/A", "A/a"],
                    "layers": [ { "masks": [ { "type": "PARTS", "parts": ["WITHERS"] } ],
                                  "op": { "type": "RESTRICT", "black": 1 } } ] },
                  { "id": "wild", "wildType": true } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad.json"));
        assertTrue(e.getMessage().contains("WITHERS"), e.getMessage());
    }

    // --- the combination table has to be total and unambiguous ------------

    @Test
    void rejectsACombinationNoExpressionCovers() {
        String json = gene("""
                , "expressions": [ { "id": "v", "when": ["A/A"],
                    "layers": [ { "masks": [], "op": { "type": "RESTRICT", "black": 0.5 } } ] } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "gap.json"));
        assertTrue(e.getMessage().contains("A/a"), e.getMessage());
        assertTrue(e.getMessage().contains("a/a"), e.getMessage());
    }

    @Test
    void rejectsTwoExpressionsClaimingTheSameCombination() {
        String json = gene("""
                , "expressions": [
                    { "id": "one", "when": ["A/A", "A/a"],
                      "layers": [ { "masks": [], "op": { "type": "RESTRICT", "black": 0.5 } } ] },
                    { "id": "two", "when": ["A/a"],
                      "layers": [ { "masks": [], "op": { "type": "RESTRICT", "black": 0.2 } } ] },
                    { "id": "wild", "wildType": true } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "overlap.json"));
        assertTrue(e.getMessage().contains("A/a"), e.getMessage());
        assertTrue(e.getMessage().contains("claimed by both"), e.getMessage());
    }

    @Test
    void rejectsTwoCatchAlls() {
        String json = gene("""
                , "expressions": [
                    { "id": "one", "wildType": true },
                    { "id": "two", "wildType": true } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "two-catch-alls.json"));
        assertTrue(e.getMessage().contains("only one expression can be the catch-all"), e.getMessage());
    }

    @Test
    void rejectsAnUnreachableCatchAll() {
        String json = gene("""
                , "expressions": [
                    { "id": "v", "when": ["A/A", "A/a", "a/a"],
                      "layers": [ { "masks": [], "op": { "type": "RESTRICT", "black": 0.5 } } ] },
                    { "id": "never", "wildType": true } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "unreachable.json"));
        assertTrue(e.getMessage().contains("can never happen"), e.getMessage());
    }

    @Test
    void rejectsAWildTypeExpressionThatPaints() {
        String json = gene("""
                , "expressions": [
                    { "id": "v", "wildType": true, "when": ["A/A", "A/a"],
                      "layers": [ { "masks": [], "op": { "type": "RESTRICT", "black": 0.5 } } ] },
                    { "id": "wild", "wildType": true } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "contradiction.json"));
        assertTrue(e.getMessage().contains("cannot carry layers"), e.getMessage());
    }

    @Test
    void rejectsAnExpressionThatDoesNothingButIsNotMarkedWildType() {
        String json = gene("""
                , "expressions": [
                    { "id": "v", "when": ["A/A", "A/a"] },
                    { "id": "wild", "wildType": true } ]
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "empty.json"));
        assertTrue(e.getMessage().contains("wildType"), e.getMessage());
    }

    @Test
    void rejectsAFounderTableNamingACombinationTheGeneCannotHave() {
        String json = """
                { "format": 2, "key": "example.bad",
                  "alleles": [ {"token":"A"}, {"token":"a"} ],
                  "founders": { "A/Q": 100 },
                  "expressions": [ { "id": "wild", "wildType": true } ] }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad-founders.json"));
        assertTrue(e.getMessage().contains("A/Q"), e.getMessage());
    }

    @Test
    void rejectsAMissingFounderTable() {
        String json = """
                { "format": 2, "key": "example.bad",
                  "alleles": [ {"token":"A"}, {"token":"a"} ],
                  "expressions": [ { "id": "wild", "wildType": true } ] }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "no-founders.json"));
        assertTrue(e.getMessage().contains("founders"), e.getMessage());
    }

    /** Three alleles, six combinations - the shape the old format could not describe. */
    @Test
    void readsAThreeAlleleLocus() {
        String json = """
                { "format": 2, "key": "example.matp", "phase": "natural",
                  "alleles": [ {"token":"Cr"}, {"token":"prl"}, {"token":"N"} ],
                  "founders": { "Cr/Cr": 1, "Cr/prl": 1, "Cr/N": 8, "prl/prl": 2, "prl/N": 8, "N/N": 80 },
                  "expressions": [
                    { "id": "double", "when": [ "Cr/Cr", "Cr/prl" ],
                      "layers": [ { "masks": [], "op": { "type": "DILUTE", "keepRed": 0.08 } } ] },
                    { "id": "single", "when": { "Cr": 1, "prl": 0 },
                      "layers": [ { "masks": [], "op": { "type": "DILUTE", "keepRed": 0.45 } } ] },
                    { "id": "pearl", "when": [ "prl/prl" ],
                      "layers": [ { "masks": [], "op": { "type": "DILUTE", "keepRed": 0.55 } } ] },
                    { "id": "wild", "wildType": true } ]
                }
                """;
        GeneSpec spec = GeneSpecParser.parse(json, "matp.json");
        assertEquals(4, spec.expressions().size());
        assertEquals(List.of("Cr/Cr", "Cr/prl"), named(spec, "double").combinations());
        assertEquals(List.of("Cr/N"), named(spec, "single").combinations());
        assertEquals(List.of("prl/prl"), named(spec, "pearl").combinations());
        // the catch-all sweeps up the two that are genuinely nothing
        assertEquals(List.of("prl/N", "N/N"), named(spec, "wild").combinations());
    }

    /** A combination written the other way round resolves to the same entry. */
    @Test
    void acceptsACombinationInEitherOrder() {
        String json = gene("""
                , "expressions": [
                    { "id": "v", "when": ["a/A"],
                      "layers": [ { "masks": [], "op": { "type": "RESTRICT", "black": 0.5 } } ] },
                    { "id": "wild", "wildType": true } ]
                """);
        GeneSpec spec = GeneSpecParser.parse(json, "flipped.json");
        assertEquals(List.of("A/a"), named(spec, "v").combinations());
    }
}
