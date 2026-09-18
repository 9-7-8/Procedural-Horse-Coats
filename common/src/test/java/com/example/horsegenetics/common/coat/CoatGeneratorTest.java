package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.genes.AbstractEyeGene;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatGeneratorTest {

    private static final Genotype BAY = Genotype.parse(Codes.of("extension", "E/e", "agouti", "A/a"));

    /**
     * <b>Generation moves no locus it was not asked to move.</b>
     *
     * <p>It used to assert the whole genotype came back identical, which stopped
     * being true when the eye loci arrived: champagne is an eye <i>requester</i>,
     * so generating this horse resolves its request into real {@code Gld/Gld}
     * alleles at the two eye loci. That is the eye system working, and a flat
     * equality check reads it as corruption.
     *
     * <p>So the contract is stated the way it is actually meant: every locus is
     * untouched <i>except</i> the eye loci, and the request really did land. A
     * gene that quietly rewrote extension or champagne still fails this.
     */
    @Test
    void movesNoLocusItWasNotAskedTo() {
        Genotype g = Genotype.parse(Codes.of("extension", "e/e", "champagne", "Ch/c"));
        CoatData data = CoatGenerator.generate(g, new SeededRng(1L));
        Genotype out = data.genotype();

        int eyeLociMoved = 0;
        for (Gene gene : Genes.codeOrder()) {
            if (gene instanceof AbstractEyeGene) {
                if (!g.pair(gene).equals(out.pair(gene))) {
                    eyeLociMoved++;
                }
                continue;
            }
            assertEquals(g.pair(gene), out.pair(gene),
                    gene.key() + " was rewritten by generation, and nothing asked it to be");
        }
        assertTrue(eyeLociMoved > 0,
                "champagne requests a gold eye, so generating it should have resolved that "
                        + "request into alleles at the eye loci - none moved");
        assertEquals(CoatPhenotype.CHESTNUT, data.phenotype());
    }

    @Test
    void givesEveryAlleleCopyItsOwnEpigenetics() {
        CoatData data = CoatGenerator.generate(BAY, new SeededRng(0xABCDL));
        var agouti = data.epigenome().copies(Genes.AGOUTI);
        assertNotEquals(agouti.first().values(), agouti.second().values(),
                "the A and the a copy get independent numbers");
        assertNotEquals(agouti.first().priority(), agouti.second().priority(),
                "no gene may carry the same priority twice");
    }

    @Test
    void twoFounderRollsOfTheSameGenotypeAreDifferentHorses() {
        CoatData a = CoatGenerator.generate(BAY, new SeededRng(1L));
        CoatData b = CoatGenerator.generate(BAY, new SeededRng(2L));
        assertEquals(a.genotype(), b.genotype());
        assertNotEquals(a.textureKey(), b.textureKey());
    }
}
