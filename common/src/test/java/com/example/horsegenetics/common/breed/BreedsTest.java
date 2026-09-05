package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.genetics.Genes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BreedsTest {

    @Test
    void everyBreedSheetRowIsRegistered() {
        assertEquals(49, Breeds.all().size());
    }

    @Test
    void idsAndNamesAreUnique() {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Breed b : Breeds.all()) {
            assertTrue(ids.add(b.id()), "duplicate id " + b.id());
            assertTrue(names.add(b.name()), "duplicate name " + b.name());
        }
        assertFalse(ids.contains("unknown"), "UNKNOWN must not be in all()");
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
        assertEquals("Unknown", Breeds.displayName("unknown"));
        assertEquals("Unknown", Breeds.displayName(null));
        assertEquals(Breeds.UNKNOWN, Breeds.get("no_such_breed"));

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
