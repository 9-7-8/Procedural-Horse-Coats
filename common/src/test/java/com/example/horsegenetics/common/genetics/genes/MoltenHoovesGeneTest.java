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
 * Molten hooves: a dominant white and three recessive colours - black (unlit),
 * one colour, and multicolour - per the owner's design of 2026-09-10.
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

    /** White is dominant over everything, the wild type and the three colours alike. */
    @Test
    void whiteIsDominantOverEveryOtherAllele() {
        for (String other : new String[] {"n", "MltB", "MltC", "MltM", "MltW"}) {
            List<GeneAbility.Emitter> e = emittersOf(pair("MltW", other), 1);
            assertEquals(1, e.size(), "MltW/" + other + " shows white");
            assertEquals(MoltenHoovesGene.WHITE, e.get(0).color());
            assertEquals(MoltenHoovesGene.GLOWS, e.get(0).data(), "white glows");
            assertEquals("white", GENE.expressionOf(pair("MltW", other)).id());
        }
    }

    /** The three colours are recessive: each shows only as its own homozygote, and a mix is a carrier. */
    @Test
    void theColoursAreRecessiveAndOnlyShowAsHomozygotes() {
        for (String a : new String[] {"MltB", "MltC", "MltM"}) {
            assertEquals(List.of(), emittersOf(pair(a, "n"), 1), a + "/n is a carrier");
            assertEquals("carrier", GENE.expressionOf(pair(a, "n")).id());
            assertTrue(!emittersOf(pair(a, a), 1).isEmpty(), a + "/" + a + " expresses");
        }
        assertEquals(List.of(), emittersOf(pair("MltB", "MltC"), 1), "two different recessives show nothing");
        assertEquals(List.of(), emittersOf(pair("n", "n"), 1));
    }

    /** Black is the one print that does not glow. */
    @Test
    void blackIsUnlit() {
        GeneAbility.Emitter e = emittersOf(pair("MltB", "MltB"), 3).get(0);
        assertEquals(MoltenHoovesGene.BLACK, e.color());
        assertEquals(MoltenHoovesGene.UNLIT, e.data());
    }

    /** One colour: a single emitter in one epigenetic colour, and two horses differ. */
    @Test
    void theColourAlleleIsOneEpigeneticColour() {
        List<GeneAbility.Emitter> e = emittersOf(pair("MltC", "MltC"), 11);
        assertEquals(1, e.size());
        assertEquals(e.get(0).color(), e.get(0).color2(), "one colour, start to finish");
        assertNotEquals(e.get(0).color(), emittersOf(pair("MltC", "MltC"), 12).get(0).color(),
                "the colour is drawn per copy");
    }

    /** Multicolour: one emitter per copy, each with its own two colours, taken in turn. */
    @Test
    void theMulticolourAlleleRunsThroughBothCopiesColours() {
        List<GeneAbility.Emitter> e = emittersOf(pair("MltM", "MltM"), 5);
        assertEquals(2, e.size());
        e.forEach(x -> assertEquals(MoltenHoovesGene.GLOWS, x.data()));
    }

    /** Every outcome is an emission; none paints. */
    @Test
    void noOutcomePaints() {
        GENE.expressions().forEach(x ->
                assertTrue(x.wildType(), x.id() + " must paint nothing - this locus is an emission"));
    }

    /** The prints are the mod's own particle, off the hooves, as the horse walks. */
    @Test
    void thePrintsAreTheHoofprintParticle() {
        GeneAbility.Emitter e = emittersOf(pair("MltW", "n"), 7).get(0);
        assertEquals(MoltenHoovesGene.PARTICLE, e.particle());
        assertEquals("hooves", e.anchor());
        assertTrue(e.trigger() instanceof GeneAbility.Trigger.OnMove);
    }

    /** Wild founders only ever express - no invisible carriers, the rule for every emission locus. */
    @Test
    void everyFounderCombinationExpresses() {
        for (String[] p : new String[][] {{"MltW", "n"}, {"MltW", "MltW"}, {"MltB", "MltB"},
                {"MltC", "MltC"}, {"MltM", "MltM"}}) {
            assertTrue(GENE.founderTable(null).share(pair(p[0], p[1])) > 0, p[0] + "/" + p[1] + " is born wild");
        }
        assertEquals(0.0, GENE.founderTable(null).share(pair("MltB", "n")), "no invisible carriers");
    }

    /** The budget guard, kept from the single-allele version: a dominant emitter stays cheap. */
    @Test
    void itStaysCheaperThanTheRecessiveParticleLocus() {
        assertTrue(MoltenHoovesGene.EMIT_CHANCE < ParticleGene.EMIT_CHANCE);
        assertTrue(MoltenHoovesGene.COUNT <= ParticleGene.MAX_COUNT);
    }
}
