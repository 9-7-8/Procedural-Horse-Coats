package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rainbow dust - one allele, one effect, and the two rules the owner set for a particle gene. */
class RainbowDustGeneTest {

    private static final RainbowDustGene GENE = Genes.RAINBOW_DUST;

    private static AllelePair pair(String a, String b) {
        return new AllelePair(GENE.fromToken(a), GENE.fromToken(b));
    }

    private static List<GeneAbility.Emitter> emittersOf(AllelePair p, long seed) {
        Genome genome = new Genome(Genotype.wildType().with(p), Epigenome.random(new SeededRng(seed)));
        List<GeneAbility.Emitter> out = new ArrayList<>();
        for (HorseAbilities.Active active : HorseAbilities.activeFor(genome.genotype(), genome.epigenome())) {
            if (active.geneKey().equals(RainbowDustGene.KEY)
                    && active.ability() instanceof GeneAbility.Emitter e) {
                out.add(e);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------

    /** Recessive: one wild-type copy and there is no dust at all. */
    @Test
    void oneWildTypeCopySilencesIt() {
        assertTrue(GENE.isRainbow(pair("Rbw", "Rbw")));
        assertFalse(GENE.isRainbow(pair("Rbw", "n")));
        assertFalse(GENE.isRainbow(pair("n", "n")));

        assertEquals(1, emittersOf(pair("Rbw", "Rbw"), 1).size());
        assertEquals(List.of(), emittersOf(pair("Rbw", "n"), 1));
        assertEquals(List.of(), emittersOf(pair("n", "n"), 1));
    }

    /**
     * The trail is a {@code dust_color_transition} off the hooves with a
     * <b>cycle</b> set, which is what tells the translator to read the hue off
     * the clock and ignore the two colours the record carries. Without the cycle
     * this gene would be a white dust trail.
     */
    @Test
    void theTrailIsACyclingDustOffTheHooves() {
        GeneAbility.Emitter e = emittersOf(pair("Rbw", "Rbw"), 3).get(0);
        assertEquals(RainbowDustGene.PARTICLE, e.particle());
        assertEquals("hooves", e.anchor());
        assertTrue(e.trigger() instanceof GeneAbility.Trigger.OnMove, "it fires as the horse walks");
        assertTrue(e.cycleTicks() >= RainbowDustGene.CYCLE_FAST_TICKS
                        && e.cycleTicks() <= RainbowDustGene.CYCLE_SLOW_TICKS,
                "cycle out of the epigenetic range: " + e.cycleTicks());
        assertTrue(e.chance() > 0 && e.chance() <= 1);
        assertTrue(e.count() >= 1);
    }

    /** How fast the colour turns is the one thing that varies, and it is inherited. */
    @Test
    void twoRainbowHorsesTurnAtDifferentSpeeds() {
        Set<Integer> speeds = new HashSet<>();
        for (long seed = 0; seed < 40; seed++) {
            speeds.add(emittersOf(pair("Rbw", "Rbw"), seed).get(0).cycleTicks());
        }
        assertTrue(speeds.size() > 25, "expected a spread of cycle speeds, got " + speeds.size());
    }

    @Test
    void theSameHorseAlwaysTurnsAtTheSameSpeed() {
        assertEquals(emittersOf(pair("Rbw", "Rbw"), 9), emittersOf(pair("Rbw", "Rbw"), 9));
    }

    // ------------------------------------------------------------------

    /** Wild horses are plain or they are rainbows - never silent carriers. */
    @Test
    void noFounderIsACarrier() {
        for (AllelePair p : GENE.founderTable(null).pairs()) {
            assertTrue(p.homozygous(), "a founder must carry two of the same: " + p);
        }
    }

    @Test
    void aRainbowHorseIsRareInTheWild() {
        int draws = 200_000;
        int rainbows = 0;
        SeededRng rng = new SeededRng(0xBA17);
        for (int i = 0; i < draws; i++) {
            if (GENE.isRainbow(Genotype.random(rng).pair(GENE))) {
                rainbows++;
            }
        }
        double share = (double) rainbows / draws;
        assertTrue(share > 0.001 && share < 0.006,
                "expected roughly one founder in four hundred, got " + share);
    }

    @Test
    void itPaintsNothing() {
        for (Expression e : GENE.expressions()) {
            assertTrue(e.wildType(), e.id() + " should change nothing about the coat");
        }
        assertFalse(GENE.affectsCoat());
        assertEquals(1, GenotypeCatalog.distinctPairsOf(GENE).size());
        assertFalse(Genotype.random(new SeededRng(6)).coatCode().contains(RainbowDustGene.KEY),
                "the locus must stay out of the texture key");
    }
}
