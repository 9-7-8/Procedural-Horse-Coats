package com.example.horsegenetics.common.breed;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A world's breed settings ({@code phc/breed-spawning.toml} in game) reach
 * every caller through the registry, and "only my breeds" really leaves nothing
 * else to spawn.
 */
class BreedSpawnSettingsTest {

    @AfterEach
    void restore() {
        Breeds.applySpawnSettings(BreedSpawnSettings.DEFAULT);
    }

    @Test
    void switchingTheShippedBreedsOffLeavesNothingWildButFeralMixed() {
        assertFalse(Breeds.wildCandidates("minecraft:plains", false).isEmpty(), "plains has shipped breeds");
        Breeds.applySpawnSettings(new BreedSpawnSettings(false, Map.of(), BreedSpawnSettings.Feral.DEFAULT));
        for (Breed b : Breeds.shipped()) {
            Breed seen = Breeds.get(b.id());
            for (BreedSource source : BreedSource.values()) {
                assertFalse(seen.allows(source), b.id() + " still comes from " + source);
            }
            assertEquals(b.name(), seen.name(), "the breed itself is untouched");
        }
        assertTrue(Breeds.wildCandidates("minecraft:plains", false).isEmpty());
        assertTrue(Breeds.from(BreedSource.COWBOY).isEmpty());
        assertTrue(Breeds.anythingMaySpawn("minecraft:plains", false), "Feral Mixed is still allowed");

        Breeds.applySpawnSettings(new BreedSpawnSettings(false, Map.of(),
                new BreedSpawnSettings.Feral(false, List.of(), 0)));
        assertFalse(Breeds.anythingMaySpawn("minecraft:plains", false), "nothing left to be");
    }

    @Test
    void aBreedCanBeMovedMadeRarerAndKeptOutOfTheWild() {
        Breed arabian = Breeds.get("arabian");
        assertFalse(arabian.biomes().contains("minecraft:dark_forest"));

        Breeds.applySpawnSettings(new BreedSpawnSettings(true, Map.of("arabian",
                new BreedSpawnSettings.BreedOverride(true, 0.5, List.of("minecraft:dark_forest"), SpawnTime.NIGHT)),
                BreedSpawnSettings.Feral.DEFAULT));
        Breed moved = Breeds.get("arabian");
        assertEquals(List.of("minecraft:dark_forest"), moved.biomes());
        assertEquals(0.5, moved.spawnWeight());
        assertTrue(Breeds.wildCandidates("minecraft:dark_forest", true).contains(moved));
        assertFalse(Breeds.wildCandidates("minecraft:dark_forest", false).contains(moved), "night only");
        assertFalse(Breeds.forBiome("minecraft:desert").contains(moved));

        Breeds.applySpawnSettings(new BreedSpawnSettings(true, Map.of("arabian",
                new BreedSpawnSettings.BreedOverride(true, 0, arabian.biomes(), arabian.spawnTime())),
                BreedSpawnSettings.Feral.DEFAULT));
        Breed tame = Breeds.get("arabian");
        assertFalse(tame.allows(BreedSource.WILD), "weight 0 is never wild");
        assertTrue(tame.allows(BreedSource.COWBOY), "but the cowboy still has one");

        Breeds.applySpawnSettings(BreedSpawnSettings.DEFAULT);
        assertEquals(arabian, Breeds.get("arabian"), "the defaults put the file's breed back");
    }

    @Test
    void feralMixedCanBeConfinedToSomeBiomes() {
        BreedSpawnSettings.Feral feral = new BreedSpawnSettings.Feral(true, List.of("minecraft:plains"), 0);
        assertTrue(feral.allowedIn("minecraft:plains"));
        assertFalse(feral.allowedIn("minecraft:taiga"));
        assertTrue(BreedSpawnSettings.Feral.DEFAULT.allowedIn("modded:anywhere"));
    }
}
