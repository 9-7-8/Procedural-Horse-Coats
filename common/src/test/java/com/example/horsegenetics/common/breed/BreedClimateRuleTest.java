package com.example.horsegenetics.common.breed;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The climate rule holds for every shipped breed</b> - {@link BreedClimate}.
 *
 * <p>The owner asked for it "as a rule", and a rule nothing checks drifts the
 * first time a breed is added. The Mirrorgazer is the case in point: it shipped
 * the same day the rule was written down, after the survey, and nothing would
 * have noticed it spawning in the Nether with no heat tolerance.
 *
 * <p>Pools are checked at the breed level and in every strain that names the
 * locus, since a strain's pool replaces the breed's for that founder.
 */
class BreedClimateRuleTest {

    @Test
    void everyNetherBreedIsFixedHeatTolerant() {
        int nether = 0;
        for (Breed b : Breeds.all()) {
            if (!BreedClimate.isNether(b.biomes())) {
                continue;
            }
            nether++;
            for (Map.Entry<String, List<Breed.Combo>> pool : pools(b, BreedClimate.HEAT)) {
                List<Breed.Combo> combos = pool.getValue();
                assertTrue(combos != null && !combos.isEmpty(),
                        b.id() + " spawns in the Nether but " + pool.getKey() + " pins no heat locus");
                for (Breed.Combo c : combos) {
                    assertEquals("Htol/Htol", c.a() + "/" + c.b(),
                            b.id() + " " + pool.getKey() + ": a Nether breed is Htol/Htol at 100, not "
                                    + c.a() + "/" + c.b());
                }
            }
        }
        assertTrue(nether >= 3, "the Netherhorse, the Nightmare and the Mirrorgazer at least");
    }

    @Test
    void everyBreedThatSpawnsInTheColdCarriesItsTier() {
        TreeSet<String> skipped = new TreeSet<>();
        List<String> below = new ArrayList<>();
        int cold = 0;
        for (Breed b : Breeds.all()) {
            for (String id : b.biomes()) {
                if (!BreedClimate.isClassified(id)) {
                    skipped.add(id);
                }
            }
            BreedClimate.Tier target = BreedClimate.coldTarget(b.biomes());
            if (target == BreedClimate.Tier.NONE) {
                continue;
            }
            cold++;
            for (Map.Entry<String, List<Breed.Combo>> pool : pools(b, BreedClimate.COLD)) {
                double copies = BreedClimate.expectedCopies(pool.getValue(), "Ctol");
                // A hair of slack for weights that do not sum to exactly 100.
                if (copies + 1e-9 < target.copies) {
                    below.add(b.id() + " " + pool.getKey() + ": " + copies + " Ctol copies, "
                            + target + " wants " + target.copies);
                }
            }
        }
        if (!skipped.isEmpty()) {
            System.out.println("BreedClimateRuleTest: not classified (modded) biome ids, counted as neither: "
                    + skipped);
        }
        assertTrue(cold > 0, "some breed spawns in the cold");
        assertTrue(below.isEmpty(), "below their cold tier: " + below);
    }

    @Test
    void theLadderIsTheOneTheOwnerSet() {
        assertEquals(BreedClimate.Tier.NEAR_FIXED, BreedClimate.coldTarget(
                List.of("minecraft:snowy_plains", "minecraft:plains")));
        assertEquals(BreedClimate.Tier.PARTIAL, BreedClimate.coldTarget(
                List.of("minecraft:snowy_plains", "minecraft:plains", "minecraft:forest", "minecraft:meadow")));
        assertEquals(BreedClimate.Tier.LIGHT, BreedClimate.coldTarget(
                List.of("minecraft:grove", "minecraft:plains", "minecraft:forest", "minecraft:meadow",
                        "minecraft:taiga")));
        assertEquals(BreedClimate.Tier.NONE, BreedClimate.coldTarget(List.of("minecraft:taiga")));
        // Each id counted once.
        assertEquals(0.5, BreedClimate.coldShare(
                List.of("minecraft:ice_spikes", "minecraft:ice_spikes", "minecraft:desert")), 1e-9);
        // The tiers' copy counts are their pools'.
        assertEquals(1.55, BreedClimate.expectedCopies(List.of(
                new Breed.Combo("Ctol", "Ctol", 60), new Breed.Combo("Ctol", "n", 35),
                new Breed.Combo("n", "n", 5)), "Ctol"), 1e-9);
        assertEquals(0.40, BreedClimate.expectedCopies(List.of(
                new Breed.Combo("Ctol", "Ctol", 5), new Breed.Combo("Ctol", "n", 30),
                new Breed.Combo("n", "n", 65)), "Ctol"), 1e-9);
    }

    /** The breed's own pool, then each strain's that names the locus. */
    private static List<Map.Entry<String, List<Breed.Combo>>> pools(Breed b, String key) {
        List<Map.Entry<String, List<Breed.Combo>>> out = new ArrayList<>();
        out.add(Map.entry("the breed", b.genePools().getOrDefault(key, List.of())));
        for (Breed.Strain s : b.strains()) {
            if (s.genePools().containsKey(key)) {
                out.add(Map.entry("strain '" + s.name() + "'", s.genePools().get(key)));
            }
        }
        return out;
    }
}
