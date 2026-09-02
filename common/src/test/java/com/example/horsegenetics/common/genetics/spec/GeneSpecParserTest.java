package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.DominancePattern;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

    @Test
    void readsASimpleDilution() {
        GeneSpec spec = GeneSpecParser.parse(example("silver.json"), "silver.json");

        assertEquals("example.silver", spec.key());
        assertTrue(spec.natural());
        assertEquals(DominancePattern.DOMINANT, spec.dominance());
        assertEquals(60, spec.wildOdds());
        assertEquals(2, spec.alleles().size());
        assertEquals("Z", spec.variant().token());
        assertEquals("z", spec.wild().token());
        assertTrue(spec.variant().visible());
        assertFalse(spec.wild().visible());
        assertEquals(2, spec.layers().size());
        assertTrue(spec.isDeterministic(), "no knobs means every carrier bakes the same coat");
    }

    @Test
    void expandsPartGroups() {
        GeneSpec spec = GeneSpecParser.parse(example("silver.json"), "silver.json");
        assertEquals(java.util.List.of(Part.MANE, Part.TAIL),
                spec.layers().get(1).masks().get(0).params().parts("parts"));
    }

    @Test
    void resolvesKnobReferencesAndPerLegDraws() {
        GeneSpec spec = GeneSpecParser.parse(example("dun.json"), "dun.json");

        assertEquals(3, spec.knobs().size());
        assertTrue(spec.knobs().get(0).seed());
        assertTrue(spec.knobs().get(1).perLeg());
        assertEquals(0.18, spec.knobs().get(1).spread(), 1e-9);
        assertFalse(spec.isDeterministic());

        GeneSpec.Value to = spec.layers().get(2).masks().get(1).params().value("to", 1);
        assertEquals(new GeneSpec.Value.FromKnob(1), to);
    }

    @Test
    void readsPerDoseTriples() {
        GeneSpec spec = GeneSpecParser.parse(example("tobiano.json"), "tobiano.json");
        GeneSpec.Value threshold = spec.layers().get(0).masks().get(0).params().value("threshold", 0.5);
        assertEquals(new GeneSpec.Value.PerDose(1.0, 0.62, 0.44), threshold);
    }

    @Test
    void readsAMagicalGene() {
        GeneSpec spec = GeneSpecParser.parse(example("aurora.json"), "aurora.json");
        assertFalse(spec.natural());
        assertEquals(DominancePattern.RECESSIVE, spec.dominance());
        assertEquals(0x2ee6c1, spec.layers().get(0).op().params().color("color", 0));
    }

    // --- what it refuses ------------------------------------------------

    @Test
    void rejectsAnUnknownParameter() {
        String json = """
                { "key": "example.typo", "alleles": [ {"token":"A"}, {"token":"a"} ],
                  "layers": [ { "masks": [ { "type": "STRIPES", "spacng": 3 } ],
                                "op": { "type": "RESTRICT", "black": 0.5 } } ] }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "typo.json"));
        assertTrue(e.getMessage().contains("spacng"), e.getMessage());
        assertTrue(e.getMessage().contains("spacing"), "the message should list the legal keys: " + e.getMessage());
    }

    @Test
    void rejectsAMagicalOpOnANaturalGene() {
        String json = """
                { "key": "example.mixed", "phase": "natural",
                  "alleles": [ {"token":"A"}, {"token":"a"} ],
                  "layers": [ { "masks": [], "op": { "type": "TINT", "blue": 100 } } ] }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "mixed.json"));
        assertTrue(e.getMessage().contains("never both"), e.getMessage());
    }

    @Test
    void rejectsAnUndeclaredKnobReference() {
        String json = """
                { "key": "example.ghost",
                  "alleles": [ {"token":"A"}, {"token":"a"} ],
                  "layers": [ { "masks": [], "op": { "type": "RESTRICT", "black": "$nope" } } ] }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "ghost.json"));
        assertTrue(e.getMessage().contains("no knob named 'nope'"), e.getMessage());
    }

    @Test
    void rejectsAlleleTokensThatWouldBreakAGenotypeCode() {
        String json = """
                { "key": "example.bad", "alleles": [ {"token":"A/B"}, {"token":"a"} ], "layers": [] }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad.json"));
        assertTrue(e.getMessage().contains("'/'"), e.getMessage());
    }

    @Test
    void rejectsAKeyWithoutANamespace() {
        String json = """
                { "key": "silver", "alleles": [ {"token":"A"}, {"token":"a"} ], "layers": [] }
                """;
        assertThrows(IllegalArgumentException.class, () -> GeneSpecParser.parse(json, "bad.json"));
    }

    @Test
    void rejectsAnUnknownPart() {
        String json = """
                { "key": "example.bad",
                  "alleles": [ {"token":"A"}, {"token":"a"} ],
                  "layers": [ { "masks": [ { "type": "PARTS", "parts": ["WITHERS"] } ],
                                "op": { "type": "RESTRICT", "black": 1 } } ] }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GeneSpecParser.parse(json, "bad.json"));
        assertTrue(e.getMessage().contains("WITHERS"), e.getMessage());
    }
}
