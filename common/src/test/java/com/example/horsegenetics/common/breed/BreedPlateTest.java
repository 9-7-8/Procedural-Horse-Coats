package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Breeds tab's plate horse ({@link BreedFounder#plate}) carries no magical
 * gene its breed does not name. Owner-reported 2026-09-10: some plates showed a
 * magic coat, because a plate was an ordinary founder roll and founders get a
 * small random dose of magic.
 */
class BreedPlateTest {

    private static final Set<String> BODY_STATS = Set.of("horsegenetics.body_size",
            "horsegenetics.magic_speed", "horsegenetics.magic_health", "horsegenetics.magic_jump");

    @Test
    void noBreedPlateCarriesAnUnnamedMagicalGene() {
        for (Breed breed : Breeds.all()) {
            for (long seed = 1; seed <= 40; seed++) {
                Genome g = BreedFounder.plate(breed, new SeededRng(seed * 7919L + breed.id().hashCode()));
                for (Gene gene : Genes.magicalOrder()) {
                    String key = gene.key();
                    if (BODY_STATS.contains(key) || breed.constrains(key)
                            || breed.magicWhitelist().contains(key)) {
                        continue;
                    }
                    assertEquals(2, g.genotype().pair(gene).count(gene.defaultAllele()),
                            breed.id() + " plate carries " + key + " (seed " + seed + ")");
                }
            }
        }
    }
}
