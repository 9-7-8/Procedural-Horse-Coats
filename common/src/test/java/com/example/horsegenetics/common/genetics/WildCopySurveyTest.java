package com.example.horsegenetics.common.genetics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>A wild-type copy carries no epigenetics</b> (owner, 2026-09-14), and this is what
 * makes that safe: no registered gene ever <i>expresses</i> the copy on its default
 * allele in an outcome that does something. If a new gene does, it must say so with
 * {@link Gene#wildTypeCarriesEpigenetics()}, or its wild-type copy would be read empty.
 * Survey result on the day it was written: no exceptions among the storing genes.
 */
class WildCopySurveyTest {

    @Test
    void noGeneExpressesAWildTypeCopyUnlessItSaysSo() {
        List<String> offenders = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            if (!Epigenome.carries(g) || g.wildTypeCarriesEpigenetics()) {
                continue;
            }
            Allele wild = g.defaultAllele();
            for (AllelePair pair : GenotypeCatalog.allPairsOf(g)) {
                if (!pair.has(wild) || g.expressionOf(pair).wildType()) {
                    continue;
                }
                // Epigenome.expressed: a heterozygote shows its first copy, a homozygote either.
                if (pair.homozygous() || pair.first().equals(wild)) {
                    offenders.add(g.key() + " " + pair.toTokens() + " -> " + g.expressionOf(pair).id());
                }
            }
        }
        assertEquals(List.of(), offenders, "these genes express a wild-type copy, which carries no numbers");
    }

    /**
     * The survey above only asks {@code expressionOf}. The white-pattern eye roll reads a
     * locus's copy on the <i>horse's</i> whiteness, wild or not - which is how an empty
     * copy first threw at breeding. Every white locus, alone and under a tobiano, through
     * the eye force: nothing throws, and a wild requester asks for nothing.
     */
    @Test
    void theEyeForceNeverReadsAnEmptyCopy() {
        Gene tobiano = null;
        List<Gene> white = new ArrayList<>();
        for (Gene g : Genes.codeOrder()) {
            if (g instanceof com.example.horsegenetics.common.genetics.genes.WhitePatternEyes.WhiteExtent) {
                white.add(g);
                if (g instanceof com.example.horsegenetics.common.genetics.genes.TobianoGene) {
                    tobiano = g;
                }
            }
        }
        assertFalse(white.isEmpty());
        List<AllelePair> under = new ArrayList<>(GenotypeCatalog.allPairsOf(tobiano));
        for (Gene g : white) {
            for (AllelePair pair : GenotypeCatalog.allPairsOf(g)) {
                for (AllelePair t : under) {
                    Genotype genotype = Genotype.parse("").with(t).with(pair);
                    Genome genome = new Genome(genotype, Epigenome.fromSeed(3L));
                    com.example.horsegenetics.common.genetics.eye.Eyes.force(
                            genome.genotype(), genome.epigenome());
                    for (Gene r : Genes.codeOrder()) {
                        if (r instanceof com.example.horsegenetics.common.genetics.eye.EyeRequestContribution c
                                && r instanceof com.example.horsegenetics.common.genetics.genes.WhitePatternEyes.WhiteExtent
                                && genome.genotype().pair(r).count(r.defaultAllele()) == 2) {
                            assertTrue(c.requestEyes(genome.genotype().pair(r), genome.genotype(),
                                    genome.epigenome()).isEmpty(), r.key() + " is wild and asked for eyes");
                        }
                    }
                }
            }
        }
    }

    /**
     * The survey cannot see a reader that uses a wild copy's number without changing the
     * outcome - fertility did exactly that and crashed a world. So an empty copy reads as
     * its gene's midpoint rather than throwing, and fertility keeps its wild numbers.
     */
    @Test
    void anEmptyCopyReadsAsItsGenesDefaultAndFertilityKeepsItsWildNumber() {
        Gene speed = Genes.byKeyOrNull("horsegenetics.magic_speed");
        Genotype genotype = Genotype.parse("horsegenetics.magic_speed=Swift/n");
        Genome genome = new Genome(genotype, Epigenome.fromSeed(7L));
        GeneEpigenetics epi = GeneEpigenetics.forGene(speed, genotype, genome.epigenome());
        int wildSlot = genotype.pair(speed).first().equals(speed.defaultAllele()) ? 0 : 1;
        assertEquals(speed.epiSchema().midpoint().get("delta"), epi.copy(wildSlot).get("delta"), 0.0);

        com.example.horsegenetics.common.genetics.genes.FertilityGene fertility =
                (com.example.horsegenetics.common.genetics.genes.FertilityGene)
                        Genes.byKeyOrNull(com.example.horsegenetics.common.genetics.genes.FertilityGene.KEY);
        Genome plain = new Genome(Genotype.parse(""), Epigenome.fromSeed(7L));
        assertFalse(plain.epigenome().copies(fertility).first().isEmpty(), "an n/n mare keeps her number");
        assertFalse(plain.epigenome().copies(fertility).second().isEmpty());
        double factor = fertility.mareFactor(plain);
        assertTrue(factor > 0.5 && factor < 1.5, "fertility reads a real number, got " + factor);
    }

    @Test
    void aGenomesWildTypeCopiesCarryNothingAndItsVariantsCarrySomething() {
        Genotype genotype = Genotype.parse("horsegenetics.magic_speed=Swift/n");
        Genome genome = new Genome(genotype, Epigenome.fromSeed(7L));
        Gene speed = Genes.byKeyOrNull("horsegenetics.magic_speed");
        Epigenome.Copies c = genome.epigenome().copies(speed);
        AllelePair pair = genotype.pair(speed);
        AlleleEpigenetics wildCopy = pair.first().equals(speed.defaultAllele()) ? c.first() : c.second();
        AlleleEpigenetics variantCopy = wildCopy == c.first() ? c.second() : c.first();
        assertTrue(wildCopy.isEmpty(), "the n copy carries nothing");
        assertFalse(variantCopy.isEmpty(), "the Swift copy keeps its numbers");

        // A horse wild type everywhere writes no segment for a storing gene it only carries n at.
        Genome plain = new Genome(Genotype.parse(""), Epigenome.fromSeed(7L));
        assertFalse(plain.epigenome().toCode().contains("horsegenetics.magic_speed="));
    }

    @Test
    void thePresentCodeLeavesOutGenesThatAreNotThereAndParsesBack() {
        Genotype g = Genotype.parse("horsegenetics.magic_speed=Swift/n-horsegenetics.dryad=Oak/Oak");
        String present = g.presentCode();
        assertTrue(present.contains("horsegenetics.magic_speed=Swift/n"), "a carrier still prints");
        assertTrue(present.contains("horsegenetics.dryad=Oak/Oak"));
        assertTrue(present.contains("horsegenetics.sex="), "sex always prints");
        assertFalse(present.contains("horsegenetics.magic_jump="), "a plain n/n gene is not there");
        assertTrue(present.length() < g.toCode().length() / 4, "most of a code is genes that are not there");
        assertEquals(g, Genotype.parse(present));
    }

    @Test
    void emptyCopiesSurviveTheCodeAndAVariantThatArrivesEmptyIsFilled() {
        Genotype genotype = Genotype.parse("horsegenetics.magic_speed=Swift/n");
        Genome genome = new Genome(genotype, Epigenome.fromSeed(11L));
        Genome back = Genome.parse(genotype.toCode(), genome.epigenome().toCode());
        assertEquals(genome, back);

        // The same numbers put on a Swift/Swift horse: the copy that was empty gets values.
        Genotype both = Genotype.parse("horsegenetics.magic_speed=Swift/Swift");
        Genome filled = new Genome(both, genome.epigenome());
        Gene speed = Genes.byKeyOrNull("horsegenetics.magic_speed");
        assertFalse(filled.epigenome().copies(speed).first().isEmpty());
        assertFalse(filled.epigenome().copies(speed).second().isEmpty());
    }
}
