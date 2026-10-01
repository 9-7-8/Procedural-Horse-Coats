package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AlleleEpigenetics;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Chaos at the two ability-driven mob loci</b> - Pack leader and Spawner.
 * Lycan's half is in {@link LycanGeneTest}. What is pinned here is the common
 * side of the contract with the game module: a matched Chaos pair hands its
 * ability the subject {@code "chaos:<seed>"}, a mismatched one hands it
 * nothing, and the seed-to-entry pick is stable and never negative.
 */
class ChaosMobLociTest {

    private static final List<AbstractMatchedPairGene> LOCI = List.of(Genes.PACK_LEADER, Genes.SPAWNER);

    @Test
    void bothLociOfferChaosAfterTheirVanillaMobsAndBeforeN() {
        for (AbstractMatchedPairGene gene : LOCI) {
            assertTrue(gene.offersChaos(), gene.key());
            var chaos = gene.fromToken(MobRoster.CHAOS_TOKEN);
            assertEquals(gene.wildType().order() - 1, chaos.order(), gene.key() + ": Chaos must sit just before n");
            assertEquals(List.of(MobRoster.CHAOS_SEED), gene.foundersShareSeeds());
            assertTrue(gene.epiSchema().indexOf(MobRoster.CHAOS_SEED) >= 0, gene.key() + " must store the seed");
        }
        // The egg layer shares the base and offers no Chaos yet: nothing is shared there.
        assertFalse(Genes.EGG_LAYER.offersChaos());
        assertEquals(List.of(), Genes.EGG_LAYER.foundersShareSeeds());
    }

    private static List<GeneAbility> abilities(Gene gene, long first, long second) {
        AllelePair chaos = new AllelePair(gene.fromToken(MobRoster.CHAOS_TOKEN), gene.fromToken(MobRoster.CHAOS_TOKEN));
        Genotype genotype = Genotype.wildType().with(chaos);
        EpiValues mid = gene.epiSchema().midpoint();
        Epigenome epi = Epigenome.random(new SeededRng(3)).with(gene.key(), new Epigenome.Copies(
                new AlleleEpigenetics(7, mid.withSeed(MobRoster.CHAOS_SEED, first)),
                new AlleleEpigenetics(9, mid.withSeed(MobRoster.CHAOS_SEED, second))));
        Genome genome = new Genome(genotype, epi);
        List<GeneAbility> out = new ArrayList<>();
        for (HorseAbilities.Active a : HorseAbilities.activeFor(genome.genotype(), genome.epigenome())) {
            if (a.geneKey().equals(gene.key())) {
                out.add(a.ability());
            }
        }
        return out;
    }

    private static String subjectOf(GeneAbility ability) {
        if (ability instanceof GeneAbility.MobAura aura) {
            return aura.mob();
        }
        if (ability instanceof GeneAbility.Summon summon) {
            return summon.mob();
        }
        throw new AssertionError("unexpected ability " + ability);
    }

    @Test
    void aMatchedChaosPairNamesItsSeedAndAMismatchedOneDoesNothing() {
        for (AbstractMatchedPairGene gene : LOCI) {
            List<GeneAbility> matched = abilities(gene, -5L, -5L);
            assertEquals(1, matched.size(), gene.key());
            assertEquals("chaos:-5", subjectOf(matched.get(0)));
            assertTrue(MobRoster.isChaos(subjectOf(matched.get(0))));
            assertEquals(-5L, MobRoster.chaosSeed(subjectOf(matched.get(0))));

            assertTrue(abilities(gene, 1L, 2L).isEmpty(), gene.key() + ": two Chaos lines must do nothing");
        }
    }

    @Test
    void aSeedPicksByPositionAndNeverNegative() {
        assertEquals(-1, MobRoster.chaosPick(12345L, 0), "no modded mob: nothing to pick");
        assertEquals(2, MobRoster.chaosPick(7L, 5));
        assertEquals(3, MobRoster.chaosPick(-7L, 5));
        assertEquals(MobRoster.chaosPick(Long.MIN_VALUE, 7), MobRoster.chaosPick(Long.MIN_VALUE, 7));
        assertTrue(MobRoster.chaosPick(Long.MIN_VALUE, 7) >= 0);
        assertFalse(MobRoster.isChaos("minecraft:wolf"));
        assertEquals(0L, MobRoster.chaosSeed("chaos:not-a-number"));
    }
}
