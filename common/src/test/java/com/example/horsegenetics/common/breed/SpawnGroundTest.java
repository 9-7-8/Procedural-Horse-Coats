package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * <b>The rule that makes a named biome a liveable one.</b>
 *
 * <p>The bug this guards is not a crash, it is a silence: both Nether breeds
 * named five biomes, were added to all five by {@code BreedHerdsBiomeModifier},
 * and could not spawn in any of them, because vanilla's animal rule wants
 * {@code minecraft:grass_block} underfoot and generated Nether terrain has none.
 * Nothing failed; there were simply never any horses. So the assertions here are
 * mostly about the <i>shape</i> of the permission rather than about one breed:
 * that it is additive, that it cannot be switched on by accident, and that the
 * two Nether breeds can now actually stand on a Nether floor.
 */
class SpawnGroundTest {

    private static final String NETHER = "minecraft:nether_wastes";

    // ---- the shape of the permission -----------------------------------

    @Test
    void sayingNothingChangesNothing() {
        assertTrue(SpawnGround.NONE.isEmpty());
        assertFalse(SpawnGround.NONE.allows("minecraft:grass_block", true),
                "NONE must grant nothing at all - vanilla's rule is the host's OR branch, not this one's");
        assertFalse(SpawnGround.NONE.allows("minecraft:netherrack", false));
    }

    @Test
    void itIsAdditiveAndNeverRestrictive() {
        // A breed that names basalt says nothing about grass: the host ORs this
        // with vanilla's rule, so lit grass keeps working for every horse. If
        // this ever starts answering for grass, the OR has become a REPLACE
        // somewhere and every ordinary horse's spawning is this class's problem.
        SpawnGround basalt = new SpawnGround(java.util.List.of("minecraft:basalt"), true);
        assertFalse(basalt.standsOn("minecraft:grass_block"),
                "spawn_ground must not claim to speak for vanilla's own tag");
    }

    @Test
    void theDarkFlagWaivesOnlyTheLightAndOnlyOverADeclaredFloor() {
        SpawnGround dark = new SpawnGround(java.util.List.of("minecraft:netherrack"), true);
        assertTrue(dark.allows("minecraft:netherrack", false), "a declared floor, unlit");
        assertTrue(dark.allows("minecraft:netherrack", true), "a declared floor, lit");
        assertFalse(dark.allows("minecraft:soul_sand", false),
                "the flag is not a licence to spawn on anything in the dark");
    }

    @Test
    void theFloorOrderIsTheAuthorsAndDuplicatesCollapse() {
        // It is written back to a checked-in breed file, so a hash order would
        // make the diff depend on the JVM's seed - the same trap Breed's own
        // canonical constructor documents.
        SpawnGround g = new SpawnGround(
                java.util.List.of("minecraft:soul_soil", "minecraft:basalt", "minecraft:soul_soil"), false);
        assertEquals(java.util.List.of("minecraft:soul_soil", "minecraft:basalt"), g.floors());
    }

    // ---- the two breeds that forced it to exist -------------------------

    @Test
    void bothNetherBreedsCanStandOnANetherFloorInTheDark() {
        for (String id : new String[] {"netherhorse", "nightmare"}) {
            Breed b = Breeds.get(id);
            // Breeds.get falls back to Feral Mixed rather than throwing, so a
            // renamed or unregistered breed would otherwise pass this silently.
            assertEquals(id, b.id(), "no breed registered as " + id);
            assertFalse(b.spawnGround().isEmpty(), id + " names no floor, so it cannot spawn wild at all");
            assertTrue(b.spawnGround().inDark(), id + " needs the light waived: the Nether has no skylight");
            assertTrue(b.spawnGround().allows("minecraft:netherrack", false),
                    id + " cannot stand on netherrack, which is most of the Nether floor");
        }
    }

    @Test
    void theNetherIsReachableThroughTheRegistry() {
        // The accessor the host calls per spawn attempt. Netherrack in the dark
        // is the exact position that used to be rejected.
        assertTrue(Breeds.wildGroundAllows(NETHER, "minecraft:netherrack", false),
                "a wild breed of the nether wastes must accept netherrack unlit");
        assertFalse(Breeds.wildGroundAllows(NETHER, "minecraft:end_stone", false),
                "a floor no breed here named must still be refused");
        assertFalse(Breeds.wildGroundAllows("minecraft:plains", "minecraft:netherrack", false),
                "a floor is only walkable in the biomes whose breeds asked for it");
    }

    @Test
    void anOverworldBiomeGrantsNoExtraFloors() {
        // The regression that would matter most: some breed quietly acquiring a
        // floor list and making plains spawn horses on stone in the dark.
        assertTrue(Breeds.wildFloors("minecraft:plains").isEmpty(),
                "no overworld breed should name extra floors yet; if one now does, "
                        + "check it did not mean to and that the dark flag is off");
    }

    @Test
    void everyDeclaredFloorIsNamespaced() {
        for (Breed b : Breeds.all()) {
            for (String floor : b.spawnGround().floors()) {
                assertTrue(floor.indexOf(':') > 0, b.id() + " floor not namespaced: " + floor);
            }
        }
    }
}
