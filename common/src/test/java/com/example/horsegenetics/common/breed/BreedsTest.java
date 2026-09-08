package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.breed.spec.BreedSpecLoader;
import com.example.horsegenetics.common.genetics.Genes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BreedsTest {

    /**
     * Every file in the shipped folder is registered, and nothing else is.
     * Written against {@code index.json} rather than against a number, because a
     * number in a test is the same stale-derived-value trap CLAUDE.md keeps
     * finding in prose - it would have to be edited every time a breed is added,
     * and editing it is not the same as checking it.
     */
    @Test
    void everyShippedBreedFileIsRegistered() {
        assertEquals(BreedSpecLoader.fromClasspath().breeds().size(), Breeds.all().size());
        assertFalse(Breeds.all().isEmpty(), "no breed files were found at all");
    }

    @Test
    void idsAndNamesAreUnique() {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Breed b : Breeds.all()) {
            assertTrue(ids.add(b.id()), "duplicate id " + b.id());
            assertTrue(names.add(b.name()), "duplicate name " + b.name());
        }
        assertFalse(ids.contains("feral_mixed"), "FERAL_MIXED must not be in all()");
    }

    @Test
    void everyBreedHasBiomesAndAPositiveWeight() {
        for (Breed b : Breeds.all()) {
            assertFalse(b.biomes().isEmpty(), b.id() + " has no biomes");
            assertTrue(b.spawnWeight() > 0, b.id() + " has non-positive weight");
            for (String biome : b.biomes()) {
                assertTrue(biome.startsWith("minecraft:"), b.id() + " biome not namespaced: " + biome);
            }
        }
    }

    @Test
    void everyGenePoolResolvesAgainstTheLiveRegistry() {
        for (Breed b : Breeds.all()) {
            for (String key : b.genePools().keySet()) {
                assertNotNull(Genes.byKey(key), b.id() + " names unknown gene " + key);
                // founderTable() resolves every token; a bad one throws
                assertNotNull(b.founderTable(key), b.id() + " / " + key);
            }
        }
    }

    @Test
    void lookupHelpers() {
        assertEquals("Friesian", Breeds.get("friesian").name());
        assertEquals("Feral Mixed", Breeds.displayName("feral_mixed"));
        assertEquals("Feral Mixed", Breeds.displayName(null));
        assertEquals(Breeds.FERAL_MIXED, Breeds.get("no_such_breed"));

        List<Breed> plains = Breeds.forBiome("minecraft:plains");
        assertFalse(plains.isEmpty());
        assertTrue(plains.stream().anyMatch(b -> b.id().equals("quarter_horse")));

        assertTrue(Breeds.forBiome("minecraft:the_void_nonsense").isEmpty());
    }

    @Test
    void hardyBreedsAreFlagged() {
        assertTrue(Breeds.get("exmoor_pony").hardy());
        assertTrue(Breeds.get("icelandic_horse").hardy());
        assertFalse(Breeds.get("thoroughbred").hardy());
    }

    @Test
    void fixedColourBreedsPinExtensionAndAgouti() {
        assertTrue(Breeds.get("friesian").constrains("horsegenetics.extension"));
        assertTrue(Breeds.get("friesian").constrains("horsegenetics.agouti"));
        assertTrue(Breeds.get("suffolk_punch").constrains("horsegenetics.extension"));
    }
}
