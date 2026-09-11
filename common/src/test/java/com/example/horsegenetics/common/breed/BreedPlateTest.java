package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A founder carries no magical gene its breed does not name. Owner-reported
 * 2026-09-10: some Breeds-tab plates showed a magic coat, because founders got
 * a small random dose of magic. That dose is gone from every breed but Feral
 * Mixed, so the plate is an ordinary founder roll again.
 */
class BreedPlateTest {

    private static final Set<String> BODY_STATS = Set.of("horsegenetics.body_size",
            "horsegenetics.magic_speed", "horsegenetics.magic_health", "horsegenetics.magic_jump");

    /**
     * A breed is exactly its breed sheet: no founder of any breed carries a
     * magical gene or a disorder the breed does not name. There is no stray
     * magic and no background disorder rate - only Feral Mixed rolls those.
     */
    @Test
    void noFounderCarriesAnUnnamedMagicalGeneOrDisorder() {
        for (Breed breed : Breeds.all()) {
            for (long seed = 1; seed <= 40; seed++) {
                Genome g = BreedFounder.roll(breed, new SeededRng(seed * 7919L + breed.id().hashCode()));
                for (Gene gene : Genes.codeOrder()) {
                    String key = gene.key();
                    boolean magical = Genes.magicalOrder().contains(gene);
                    boolean disorder = gene instanceof com.example.horsegenetics.common.trait.HealthContribution;
                    if (!(magical || disorder) || BODY_STATS.contains(key) || breed.constrains(key)) {
                        continue;
                    }
                    assertEquals(2, g.genotype().pair(gene).count(gene.defaultAllele()),
                            breed.id() + " founder carries " + key + " (seed " + seed + ")");
                }
            }
        }
    }
}
