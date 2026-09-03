package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A {@link GenomeSample} is a genome taken off a horse: it must round-trip its
 * codes exactly and breed identically to the live genome it came from.
 */
class GenomeSampleTest {

    @Test
    void roundTripsTheGenomeCodesVerbatim() {
        Genome sire = Genome.random(new SeededRng(1));
        GenomeSample sample = GenomeSample.of(sire);

        assertEquals(sire.genotypeCode(), sample.genotypeCode());
        assertEquals(sire.epigenomeCode(), sample.epigenomeCode());
        assertEquals(sire.genotypeCode(), sample.genome().genotypeCode());
        assertEquals(sire.epigenomeCode(), sample.genome().epigenomeCode());
    }

    @Test
    void breedingThroughTheSampleMatchesBreedingWithTheLiveGenome() {
        Genome sire = Genome.random(new SeededRng(7));
        Genome mare = Genome.random(new SeededRng(9));

        Genome viaLive = mare.breedWith(sire, new SeededRng(42));
        Genome viaSample = GenomeSample.of(sire).breedInto(mare, new SeededRng(42));

        assertEquals(viaLive.genotypeCode(), viaSample.genotypeCode());
        assertEquals(viaLive.epigenomeCode(), viaSample.epigenomeCode());
    }

    @Test
    void rejectsAMalformedCodeAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new GenomeSample("not-a-code", "nope"));
    }
}
