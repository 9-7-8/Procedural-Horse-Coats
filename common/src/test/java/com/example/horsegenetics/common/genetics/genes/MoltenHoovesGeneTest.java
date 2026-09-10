package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Molten hooves - the one <b>dominant</b> emission locus, and the gene whose
 * name most invites somebody to make it set things on fire.
 */
class MoltenHoovesGeneTest {

    private static final MoltenHoovesGene GENE = Genes.MOLTEN_HOOVES;

    private static AllelePair pair(String a, String b) {
        return new AllelePair(GENE.fromToken(a), GENE.fromToken(b));
    }

    private static List<GeneAbility.Emitter> emittersOf(AllelePair p, long seed) {
        Genome genome = new Genome(Genotype.wildType().with(p), Epigenome.random(new SeededRng(seed)));
        List<GeneAbility.Emitter> out = new ArrayList<>();
        for (HorseAbilities.Active active : HorseAbilities.activeFor(genome.genotype(), genome.epigenome())) {
            if (active.geneKey().equals(MoltenHoovesGene.KEY)
                    && active.ability() instanceof GeneAbility.Emitter e) {
                out.add(e);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------

    /**
     * Dominant, which no other emission locus is: one copy is the whole effect.
     * A doubled horse gets a second emitter rather than a stronger one, which is
     * the only difference between the two expressing rows.
     */
    @Test
    void oneCopyIsEnoughAndTwoCopiesTrailTwoColours() {
        assertEquals(1, emittersOf(pair("Mlt", "n"), 1).size(), "one copy expresses");
        assertEquals(2, emittersOf(pair("Mlt", "Mlt"), 1).size(), "two copies, two trails");
        assertEquals(List.of(), emittersOf(pair("n", "n"), 1));
    }

    /** The three rows are three distinct outcomes, and none of them paints. */
    @Test
    void theExpressionsAreDistinctAndNoneOfThemPaints() {
        assertNotEquals(GENE.expressionOf(pair("Mlt", "Mlt")).id(),
                GENE.expressionOf(pair("Mlt", "n")).id());
        assertNotEquals(GENE.expressionOf(pair("Mlt", "n")).id(),
                GENE.expressionOf(pair("n", "n")).id());
        GENE.expressions().forEach(e ->
                assertTrue(e.wildType(), e.id() + " must paint nothing - this locus is an emission"));
    }

    /**
     * A fading dust off all four feet as the horse walks. Deliberately
     * <b>not</b> a flame particle: flames ignore colour, and a colour written on
     * the allele copy is the entire point of the locus.
     */
    @Test
    void theTrailIsAFadingDustOffTheHooves() {
        GeneAbility.Emitter e = emittersOf(pair("Mlt", "n"), 7).get(0);
        assertEquals(MoltenHoovesGene.PARTICLE, e.particle());
        assertEquals("hooves", e.anchor());
        assertTrue(e.trigger() instanceof GeneAbility.Trigger.OnMove, "it fires as the horse walks");
        assertEquals(0, e.cycleTicks(), "it is not a rainbow - the two colours are the colours");
        assertEquals(MoltenHoovesGene.COUNT, e.count(), "count is fixed, not epigenetic");
        assertTrue(e.chance() > 0 && e.chance() <= 1);
    }

    /**
     * <b>The budget guard.</b> This locus is dominant, so a populated stable can
     * have a dozen of these emitting on the same moving tick - which is why the
     * count is fixed and the chance sits below the particle locus's. If somebody
     * turns either up for drama, this is the test that should stop them and send
     * them to change the colour instead.
     */
    @Test
    void itStaysCheaperThanTheRecessiveParticleLocus() {
        assertTrue(MoltenHoovesGene.EMIT_CHANCE < ParticleGene.EMIT_CHANCE,
                "a dominant trail must fire less often than the recessive one");
        assertTrue(MoltenHoovesGene.COUNT <= ParticleGene.MAX_COUNT,
                "a dominant trail must not be denser than the recessive one");
    }

    /** The colour is per allele copy, so two horses burn differently and a line breeds true. */
    @Test
    void twoMoltenHorsesBurnDifferentColours() {
        int a = emittersOf(pair("Mlt", "n"), 11).get(0).color();
        int b = emittersOf(pair("Mlt", "n"), 12).get(0).color();
        assertNotEquals(a, b, "the colour is drawn per copy");
    }

    /**
     * Both expressing combinations are in the founder table, so there is no such
     * thing as an invisible carrier here - the rule for every emission locus.
     */
    @Test
    void everyFounderCombinationExpresses() {
        assertTrue(GENE.founderTable(null).share(pair("Mlt", "n")) > 0, "single copies are born wild");
        assertTrue(GENE.founderTable(null).share(pair("Mlt", "Mlt")) > 0, "doubles are born wild too");
    }
}
