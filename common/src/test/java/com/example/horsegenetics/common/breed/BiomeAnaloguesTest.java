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
    void theTableNeverReachesADimensionAModAdded() {
        // Owner's scope: overworld, Nether and End only - no horses in the
        // Aether, the Undergarden or AllTheModium's worlds. Caves have no lit grass.
        for (String biome : BiomeAnalogues.moddedBiomes()) {
            assertFalse(biome.contains("cave"), biome);
            assertFalse(biome.startsWith("undergarden:") || biome.startsWith("aether_ii:")
                    || biome.startsWith("allthemodium:") || biome.startsWith("mahoutsukai:"), biome);
        }
    }

    @Test
    void everyNetherAndEndBiomeHasABreedThatCanStandOnItsGround() {
        // A named biome is not a liveable one: nothing there is grass, so some
        // breed of it must list the floor. One floor per biome, read off the
        // mods' surface rules, is the minimum that makes the mapping real.
        String[][] floors = {
                {"regions_unexplored:glistering_meadow", "regions_unexplored:glistering_nylium"},
                {"regions_unexplored:mycotoxic_undergrowth", "regions_unexplored:mycotoxic_nylium"},
                {"regions_unexplored:infernal_holt", "regions_unexplored:brimsprout_nylium"},
                {"regions_unexplored:blackstone_basin", "regions_unexplored:cobalt_nylium"},
                {"regions_unexplored:blackstone_basin", "minecraft:blackstone"},
                {"nullscape:shadowlands", "minecraft:dead_tube_coral_block"},
                {"nullscape:crystal_peaks", "minecraft:blackstone"},
                {"nullscape:void_barrens", "minecraft:smooth_basalt"},
                {"nullscape:void_barrens", "minecraft:basalt"},
                {"minecraft:end_highlands", "minecraft:end_stone"},
                {"minecraft:small_end_islands", "minecraft:end_stone"},
        };
        for (String[] f : floors) {
            assertTrue(Breeds.wildGroundAllows(f[0], f[1], false), f[0] + " on " + f[1] + " in the dark");
        }
    }

    @Test
    void noOverworldBiomeGainsADarkFloor() {
        // The Enderpony lives in forty overworld biomes and names End floors;
        // smooth basalt lines every amethyst geode, and end stone is a builder's
        // block. A floor counts only in the Nether and the End.
        for (String block : List.of("minecraft:smooth_basalt", "minecraft:blackstone", "minecraft:basalt",
                "minecraft:end_stone", "minecraft:dead_tube_coral_block")) {
            for (String biome : List.of("minecraft:plains", "minecraft:desert", "terralith:steppe")) {
                assertFalse(Breeds.wildGroundAllows(biome, block, false), block + " in " + biome);
            }
        }
    }

    @Test
    void theFeralSettingReadsTheTableToo() {
        BreedSpawnSettings.Feral plainsOnly = new BreedSpawnSettings.Feral(true, List.of("minecraft:plains"), 0);
        assertTrue(plainsOnly.allowedIn("terralith:steppe"));
        assertFalse(plainsOnly.allowedIn("terralith:tropical_jungle"));
    }
}
