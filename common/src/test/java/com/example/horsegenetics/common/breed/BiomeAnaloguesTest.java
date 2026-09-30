package com.example.horsegenetics.common.breed;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The modded-biome table, and the promise it was written to keep: <b>every
 * modded overworld biome it knows has at least one breed that may be found wild
 * there.</b> A misspelt analogue ({@code minecraft:plain}) would otherwise pass
 * silently and leave a biome horseless, which is exactly the failure the table
 * exists to fix.
 */
class BiomeAnaloguesTest {

    @BeforeAll
    static void loadBreeds() {
        Breeds.resetForTesting();
        Breeds.loadBuiltins();
    }

    @Test
    void everyModdedBiomeHasAWildBreed() {
        List<String> horseless = new ArrayList<>();
        for (String biome : BiomeAnalogues.moddedBiomes()) {
            if (Breeds.forBiome(biome, BreedSource.WILD).isEmpty()) {
                horseless.add(biome);
            }
        }
        assertTrue(horseless.isEmpty(), "modded biomes no wild breed lives in: " + horseless);
    }

    @Test
    void everyAnalogueIsABiomeSomeBreedNames() {
        Set<String> named = new HashSet<>();
        for (Breed b : Breeds.all()) {
            named.addAll(b.biomes());
        }
        List<String> unknown = new ArrayList<>();
        for (String biome : BiomeAnalogues.moddedBiomes()) {
            for (String analogue : BiomeAnalogues.of(biome)) {
                if (!named.contains(analogue)) {
                    unknown.add(biome + " -> " + analogue);
                }
            }
        }
        assertTrue(unknown.isEmpty(), "analogues no breed names (a typo?): " + unknown);
    }

    @Test
    void aModdedBiomeInheritsItsAnaloguesWholePool() {
        // Terralith's steppe stands in for plains and windswept hills, so every
        // wild breed of either is a breed of the steppe - not a hand-picked few.
        Set<Breed> expected = new HashSet<>(Breeds.forBiome("minecraft:plains", BreedSource.WILD));
        expected.addAll(Breeds.forBiome("minecraft:windswept_hills", BreedSource.WILD));
        assertEquals(expected, new HashSet<>(Breeds.forBiome("terralith:steppe", BreedSource.WILD)));
    }

    @Test
    void vanillaAndUnknownBiomesAreUntouched() {
        assertTrue(BiomeAnalogues.of("minecraft:plains").isEmpty());
        assertTrue(BiomeAnalogues.of("somemod:nowhere").isEmpty());
        assertTrue(Breeds.forBiome("somemod:nowhere", BreedSource.WILD).isEmpty());
    }

    @Test
    void theTableStaysOnTheOverworldSurface() {
        // Owner's scope: caves have no lit grass and the other dimensions are a
        // design call of their own. A Nether or cave id here is a mistake.
        for (String biome : BiomeAnalogues.moddedBiomes()) {
            assertFalse(biome.contains("cave"), biome);
            assertFalse(biome.startsWith("undergarden:") || biome.startsWith("aether_ii:")
                    || biome.startsWith("nullscape:") || biome.startsWith("allthemodium:"), biome);
        }
        for (String nether : List.of("blackstone_basin", "glistering_meadow", "infernal_holt",
                "mycotoxic_undergrowth", "redstone_abyss", "inferno")) {
            assertTrue(BiomeAnalogues.of("regions_unexplored:" + nether).isEmpty(), nether);
        }
    }

    @Test
    void theFeralSettingReadsTheTableToo() {
        BreedSpawnSettings.Feral plainsOnly = new BreedSpawnSettings.Feral(true, List.of("minecraft:plains"), 0);
        assertTrue(plainsOnly.allowedIn("terralith:steppe"));
        assertFalse(plainsOnly.allowedIn("terralith:tropical_jungle"));
    }
}
