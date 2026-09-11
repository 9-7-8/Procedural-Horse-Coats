package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.breed.spec.BreedSpecParser;
import com.example.horsegenetics.common.breed.spec.BreedSpecWriter;
import com.example.horsegenetics.common.trait.StatAxis;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The breed file format: what a blank field means, and the difference between
 * a file that is <b>wrong</b> and a file that names something this install has
 * not got.
 *
 * <p>That distinction is the one worth pinning. A breed is mostly references to
 * other people's genes, so "you have not installed that" must cost the breed
 * one locus and nothing else - a shared breed has to degrade to the horses this
 * install can make rather than vanish. A malformed file, by contrast, is the
 * author's mistake and is thrown.
 */
class BreedSpecParserTest {

    private final List<String> warnings = new ArrayList<>();

    private Breed parse(String json) {
        return BreedSpecParser.parse(json, "test.json", warnings::add);
    }

    // ---- defaults ------------------------------------------------------

    @Test
    void everythingButIdAndNameMayBeLeftOut() {
        Breed b = parse("{\"id\": \"x\", \"name\": \"X\"}");
        assertEquals("x", b.id());
        assertEquals("X", b.name());
        assertFalse(b.magical(), "kind defaults to natural");
        assertEquals(Commonness.MODERATE.weight, b.spawnWeight());
        assertEquals(BreedSource.ALL, b.sources(), "a breed that names no source gets every source");
        assertTrue(b.biomes().isEmpty());
        assertTrue(b.price().isEmpty(), "an unpriced breed is priced by HorsePrices, not here");
        assertFalse(b.hardy());
        assertEquals(0.20, b.magicChance());
        assertTrue(b.scores().isEmpty(), "no stats block leaves every axis wild");
        assertTrue(b.genePools().isEmpty());
        assertTrue(b.bands().isEmpty());
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void anEmptySpawnListMeansNowhereRatherThanEverywhere() {
        Breed b = parse("{\"id\": \"x\", \"name\": \"X\", \"spawn\": []}");
        assertTrue(b.sources().isEmpty());
        for (BreedSource source : BreedSource.values()) {
            assertFalse(b.allows(source));
        }
    }

    @Test
    void aSpawnChecklistIsHonouredExactly() {
        Breed b = parse("{\"id\": \"x\", \"name\": \"X\", \"spawn\": [\"cowboy\", \"spawn_egg\"]}");
        assertFalse(b.allows(BreedSource.WILD));
        assertTrue(b.allows(BreedSource.COWBOY));
        assertTrue(b.hasSpawnEgg());
        assertFalse(b.allows(BreedSource.STABLE));
    }

    // ---- the malformed / the missing -----------------------------------

    @Test
    void aMalformedFileThrows() {
        assertThrows(IllegalArgumentException.class, () -> parse("{\"name\": \"no id\"}"));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"id\": \"x\", \"name\": 7}"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("{\"id\": \"x\", \"name\": \"X\", \"nonsense\": 1}"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("{\"id\": \"X\", \"name\": \"X\"}"), "an upper-case id");
        assertThrows(IllegalArgumentException.class,
                () -> parse("{\"id\": \"x\", \"name\": \"X\", \"spawn\": [\"nowhere\"]}"));
    }

    @Test
    void aGeneThisInstallHasNotGotIsWarnedAndSkipped() {
        Breed b = parse("""
                {"id": "x", "name": "X", "genes": {
                  "somemod.imaginary": [ {"pair": "A/a", "weight": 1} ],
                  "horsegenetics.extension": [ {"pair": "E/E", "weight": 1} ]
                }}""");
        assertFalse(b.constrains("somemod.imaginary"));
        assertTrue(b.constrains("horsegenetics.extension"), "the rest of the breed still loads");
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("somemod.imaginary"));
    }

    @Test
    void anAlleleTheGeneDoesNotDeclareDropsThatCombinationOnly() {
        Breed b = parse("""
                {"id": "x", "name": "X", "genes": {
                  "horsegenetics.extension": [
                    {"pair": "E/E", "weight": 1},
                    {"pair": "E/Q", "weight": 1}
                  ]
                }}""");
        assertEquals(1, b.genePools().get("horsegenetics.extension").size());
        assertEquals(1, warnings.size(), warnings.toString());
        assertNotNull(b.founderTable("horsegenetics.extension"));
    }

    @Test
    void aPoolThatLosesEverythingLeavesTheLocusUnnamed() {
        Breed b = parse("""
                {"id": "x", "name": "X", "genes": {
                  "horsegenetics.extension": [ {"pair": "Q/Q", "weight": 1} ]
                }}""");
        assertFalse(b.constrains("horsegenetics.extension"),
                "an empty pool must not reach founderTable(), which would throw");
        assertEquals(2, warnings.size(), warnings.toString());
    }

    // ---- stats ---------------------------------------------------------

    @Test
    void statScoresSurviveAsWrittenAndStillResolveToBands() {
        Breed b = parse("""
                {"id": "x", "name": "X",
                 "stats": {"speed": 9, "jump": 5, "health": 8, "size": [0.92, 1.05]}}""");
        assertEquals(9.0, b.scores().speed().orElseThrow().lo());
        assertEquals(0.92, b.scores().size().orElseThrow().lo());
        assertNotNull(b.statTargets().band(StatAxis.SPEED));
        // A score of 5 is the baseline, so its band is dropped - see BreedStatCurve.
        assertEquals(null, b.statTargets().band(StatAxis.JUMP));
    }

    @Test
    void aScoreMayBeARangeAsWellAsANumber() {
        Breed b = parse("{\"id\": \"x\", \"name\": \"X\", \"stats\": {\"speed\": [7, 9]}}");
        assertEquals(7.0, b.scores().speed().orElseThrow().lo());
        assertEquals(9.0, b.scores().speed().orElseThrow().hi());
    }

    // ---- bands ---------------------------------------------------------

    @Test
    void aBandOnAValueTheGeneDoesNotDeclareIsWarnedAndIgnored() {
        Breed b = parse("""
                {"id": "x", "name": "X",
                 "bands": {"horsegenetics.ednrb": {"not_a_value": [0, 1]}}}""");
        assertTrue(b.bands().forGene("horsegenetics.ednrb").isEmpty());
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void aBandOnABodyStatGeneIsRefusedInFavourOfTheStatsBlock() {
        Breed b = parse("""
                {"id": "x", "name": "X",
                 "bands": {"horsegenetics.body_size": {"delta": [0.1, 0.2]}}}""");
        assertTrue(b.bands().isEmpty());
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("stats"));
    }

    // ---- round trip ----------------------------------------------------

    @Test
    void writingAndReadingBackIsLossless() {
        String source = """
                {"id": "x", "name": "X", "kind": "magical", "commonness": "rare",
                 "spawn": ["cowboy", "spawn_egg"],
                 "biomes": ["minecraft:plains"],
                 "price": [8, 14], "hardy": true, "magic_chance": 0.4,
                 "magic_whitelist": ["horsegenetics.lycan"],
                 "description": "A test breed.", "spawn_time": "night",
                 "stats": {"speed": 9, "size": [1.1, 1.4]},
                 "genes": {"horsegenetics.extension": [ {"pair": "E/e", "weight": 3} ]},
                 "bands": {"horsegenetics.ednrb": {"cover": [0.55, 0.8]},
                           "horsegenetics.contour_cells": {"hue": 120, "cellSeed": "-1234567890123456789"}},
                 "notes": ["a note"]}""";
        Breed once = parse(source);
        String written = BreedSpecWriter.write(once);
        Breed twice = BreedSpecParser.parse(written, "round-trip.json", warnings::add);

        assertEquals(written, BreedSpecWriter.write(twice), "write -> parse -> write must be stable");
        assertEquals(once.sources(), twice.sources());
        assertEquals(once.magical(), twice.magical());
        assertEquals(once.spawnWeight(), twice.spawnWeight());
        assertEquals(once.price(), twice.price());
        assertEquals(once.scores(), twice.scores());
        assertEquals(once.genePools(), twice.genePools());
        assertEquals(once.notes(), twice.notes());
        assertEquals(0.55, twice.bands().forGene("horsegenetics.ednrb").get("cover").lo());
    }
}
