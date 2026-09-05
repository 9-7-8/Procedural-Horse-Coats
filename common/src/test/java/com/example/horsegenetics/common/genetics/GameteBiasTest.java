package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The breeding-carrot gamete hook (roadmap wiki &sect;14). The load-bearing
 * property is the first test: a carrot that is not fed changes nothing.
 */
class GameteBiasTest {

    private static Genome founder(long seed) {
        return Genome.random(new SeededRng(seed));
    }

    private static Genome withExtension(String pair, long seed) {
        return Genome.of(Genotype.parse("horsegenetics.extension=" + pair), new SeededRng(seed));
    }

    /** NONE on both sides is bit-for-bit the plain breed - no extra draws, same result. */
    @Test
    void noneOnBothSidesIsIdenticalToThePlainBreed() {
        Genome dam = founder(1);
        Genome sire = founder(2);
        for (long s = 0; s < 25; s++) {
            Genome plain = dam.breedWith(sire, new SeededRng(s));
            Genome biased = dam.breedWith(sire, new SeededRng(s), GameteBias.NONE, GameteBias.NONE);
            assertEquals(plain.genotypeCode(), biased.genotypeCode(), "seed " + s + " genotype");
            assertEquals(plain.epigenomeCode(), biased.epigenomeCode(), "seed " + s + " epigenome");
        }
    }

    /** A Known Gene Splice carrot forces the fed parent's gamete for that one gene. */
    @Test
    void knownGeneSpliceCarrotForcesTheGameteHomozygous() {
        Genome dam = withExtension("e/e", 10);   // dam has no E of her own
        Genome sire = withExtension("e/e", 11);
        GameteBias damBias = GameteBias.substituting(
                Map.of("horsegenetics.extension",
                        new AllelePair(Genes.EXTENSION.alleles().get(0), Genes.EXTENSION.alleles().get(0))));

        for (long s = 0; s < 40; s++) {
            Genome foal = dam.breedWith(sire, new SeededRng(s), damBias, GameteBias.NONE);
            AllelePair ext = foal.genotype().pair(Genes.EXTENSION);
            assertTrue(ext.has(Genes.EXTENSION.alleles().get(0)),
                    "seed " + s + ": foal must carry the E the carrot forced (" + ext.toTokens() + ")");
        }
    }

    /** Stabilizer / magnifier bias which copy a heterozygous parent contributes. */
    @Test
    void stabilizerAndMagnifierBiasTheHeterozygousLocus() {
        Genome dam = withExtension("E/e", 20);
        Genome sire = withExtension("e/e", 21);
        Allele dominant = Genes.EXTENSION.alleles().get(0); // E

        for (long s = 0; s < 40; s++) {
            Genome stab = dam.breedWith(sire, new SeededRng(s), GameteBias.stabilizer(), GameteBias.NONE);
            assertTrue(stab.genotype().pair(Genes.EXTENSION).has(dominant),
                    "stabilizer: foal always gets the dam's E");
            Allele recessive = Genes.EXTENSION.alleles().get(1); // e
            Genome magn = dam.breedWith(sire, new SeededRng(s), GameteBias.magnifier(), GameteBias.NONE);
            assertTrue(magn.genotype().pair(Genes.EXTENSION).homozygousFor(recessive),
                    "magnifier: foal always e/e");
        }
    }

    /** Epigenetic splice leaves the alleles alone but re-rolls the epigenetic seeds from the fed parent. */
    @Test
    void epigeneticSpliceRerollsEpigeneticsNotAlleles() {
        Genome dam = founder(30);
        Genome sire = founder(31);
        for (long s = 0; s < 15; s++) {
            Genome plain = dam.breedWith(sire, new SeededRng(s));
            Genome spliced = dam.breedWith(sire, new SeededRng(s), GameteBias.epigeneticSplice(), GameteBias.NONE);
            assertEquals(plain.genotypeCode(), spliced.genotypeCode(), "seed " + s + ": alleles unchanged");
            assertNotEquals(plain.epigenomeCode(), spliced.epigenomeCode(), "seed " + s + ": some seeds re-rolled");
        }
    }

    /** fold() over an empty list is NONE; a mixed list merges. */
    @Test
    void foldCollapsesTheEffectList() {
        assertEquals(GameteBias.NONE, CarrotEffect.fold(List.of(), Genotype.wildType(), new SeededRng(1)));

        GameteBias b = CarrotEffect.fold(
                List.of(new CarrotEffect.EpigeneticSplice(), new CarrotEffect.Stabilizer()),
                Genotype.wildType(), new SeededRng(1));
        assertTrue(b.rerollEpigenetics());
        assertEquals(Optional.of(Boolean.TRUE), b.preferLowerOrder());

        GameteBias splice = CarrotEffect.fold(
                List.of(new CarrotEffect.GeneSplice()), Genotype.wildType(), new SeededRng(1));
        assertEquals(1, splice.substitutePairs().size(), "gene splice substitutes exactly one gene");
        assertTrue(splice.substitutePairs().keySet().stream().noneMatch(k -> k.equals(Genes.SEX.key())),
                "gene splice never touches the sex locus");
    }
}
