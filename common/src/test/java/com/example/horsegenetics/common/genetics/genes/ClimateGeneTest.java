package com.example.horsegenetics.common.genetics.genes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.AbilityType;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import org.junit.jupiter.api.Test;

import java.util.List;

/** The heat and cold loci (owner, 2026-09-15): tolerant helps, sensitive hurts, the wild type is neutral. */
class ClimateGeneTest {

    private static List<GeneAbility> abilities(AbstractClimateGene gene, AllelePair pair, long seed) {
        Genome genome = Genome.of(Genotype.random(new SeededRng(seed)).with(pair), new SeededRng(seed));
        return gene.abilitiesFor(pair, genome.genotype(),
                GeneEpigenetics.forGene(gene, genome.genotype(), genome.epigenome()));
    }

    private static double total(List<GeneAbility> out) {
        double sum = 0.0;
        for (GeneAbility a : out) {
            sum += ((GeneAbility.AttributeMod) a).amount();
        }
        return sum;
    }

    @Test
    void bothLociAreRegisteredAndKeyOffTheirOwnClimate() {
        assertTrue(Genes.magicalOrder().contains(Genes.MAGIC_HEAT));
        assertTrue(Genes.magicalOrder().contains(Genes.MAGIC_COLD));
        assertEquals("hot_biome", Genes.MAGIC_HEAT.flag());
        assertEquals("cold_biome", Genes.MAGIC_COLD.flag());
        assertTrue(AbilityType.CONDITION_FLAGS.contains("hot_biome") && AbilityType.WORLD_FLAGS.contains("hot_biome"));
        assertTrue(AbilityType.CONDITION_FLAGS.contains("cold_biome") && AbilityType.WORLD_FLAGS.contains("cold_biome"));
    }

    @Test
    void theWildTypeIsNeutral() {
        AbstractClimateGene g = Genes.MAGIC_HEAT;
        assertTrue(abilities(g, new AllelePair(g.n, g.n), 1L).isEmpty());
    }

    @Test
    void tolerantHelpsAndSensitiveHurtsOnlyInTheirClimate() {
        for (AbstractClimateGene g : List.of(Genes.MAGIC_HEAT, Genes.MAGIC_COLD)) {
            for (long seed = 0; seed < 40; seed++) {
                List<GeneAbility> up = abilities(g, new AllelePair(g.tolerant, g.tolerant), seed);
                List<GeneAbility> down = abilities(g, new AllelePair(g.sensitive, g.n), seed);
                assertTrue(total(up) > 0.0, g.key() + " tolerant");
                assertTrue(total(down) < 0.0, g.key() + " sensitive");
                for (GeneAbility a : up) {
                    GeneAbility.AttributeMod mod = (GeneAbility.AttributeMod) a;
                    assertTrue(mod.attribute().equals("movement_speed") || mod.attribute().equals("jump_strength"));
                    assertEquals(new GeneAbility.Condition.Flag(g.flag(), false), mod.when());
                }
            }
        }
    }

    @Test
    void eachCopySplitsItsAmountBetweenSpeedAndJump() {
        AbstractClimateGene g = Genes.MAGIC_HEAT;
        AllelePair pair = new AllelePair(g.tolerant, g.n);
        for (long seed = 0; seed < 40; seed++) {
            Genome genome = Genome.of(Genotype.random(new SeededRng(seed)).with(pair), new SeededRng(seed));
            var copy = GeneEpigenetics.forGene(g, genome.genotype(), genome.epigenome()).copy(0);
            double delta = copy.get(AbstractClimateGene.DELTA);
            double share = copy.get(AbstractClimateGene.SPEED_SHARE);
            double speed = 0.0;
            double jump = 0.0;
            for (GeneAbility a : g.abilitiesFor(pair, genome.genotype(), GeneEpigenetics.forGene(g, genome.genotype(), genome.epigenome()))) {
                GeneAbility.AttributeMod mod = (GeneAbility.AttributeMod) a;
                if (mod.attribute().equals("movement_speed")) {
                    speed = mod.amount();
                } else {
                    jump = mod.amount();
                }
            }
            assertEquals(delta * share, speed, 1e-9);
            assertEquals(delta * (1.0 - share), jump, 1e-9);
        }
    }
}
