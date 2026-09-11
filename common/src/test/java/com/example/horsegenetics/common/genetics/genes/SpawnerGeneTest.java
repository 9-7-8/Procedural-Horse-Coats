package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The spawner fires when it is <b>fed</b>, two per meal with no cap. It used to be a
 * once-a-day timer counted on the horse's unsaved {@code tickCount}, which in
 * play meant it never fired at all.
 */
class SpawnerGeneTest {

    private static Allele allele(Gene gene, String token) {
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        throw new AssertionError("no allele " + token + " on " + gene.key());
    }

    @Test
    void aMatchedPairSummonsTwoPerFeeding() {
        Gene spawner = Genes.byKey(SpawnerGene.KEY);
        Allele cow = allele(spawner, "Cow");
        Genotype gt = Genotype.wildType().with(new AllelePair(cow, cow));

        List<GeneAbility.Summon> summons = HorseAbilities.activeFor(gt, Epigenome.fromSeed(1L)).stream()
                .map(HorseAbilities.Active::ability)
                .filter(a -> a instanceof GeneAbility.Summon)
                .map(a -> (GeneAbility.Summon) a)
                .toList();

        assertEquals(1, summons.size());
        GeneAbility.Summon su = summons.get(0);
        assertTrue(su.trigger() instanceof GeneAbility.Trigger.OnFeed, "fed, not timed");
        assertEquals(SpawnerGene.PER_FEEDING, su.upTo());
        assertEquals("minecraft:cow", su.mob());
        assertTrue(su.variant() >= 0 && su.variant() < 1,
                "the colour is the allele copy's own number, not left to the game");
    }
}
