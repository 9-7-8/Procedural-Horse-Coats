package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.repro.Conception;
import com.example.horsegenetics.common.repro.Pregnancy;
import com.example.horsegenetics.common.repro.ReproTiming;
import com.example.horsegenetics.common.repro.Reproduction;
import com.example.horsegenetics.common.trait.MiscarriageSigns;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gap 225: a genotype a gene rules out must never be born. Found generically -
 * every gene, every homozygote - so a new gene that overrides {@code canOccur}
 * is covered the day it is added.
 */
class ConceivableTest {

    @Test
    void aWildTypeHorseIsConceivable() {
        assertTrue(Conceivable.failure(Genotype.wildType()).isEmpty());
    }

    @Test
    void everyHomozygoteAGeneRulesOutIsCaught() {
        int found = 0;
        for (Gene gene : Genes.codeOrder()) {
            if (gene == Genes.SEX) {
                continue;   // Y/Y: the sex draw never makes one
            }
            for (Allele a : gene.alleles()) {
                AllelePair pair = new AllelePair(a, a);
                if (gene.canOccur(pair)) {
                    continue;
                }
                found++;
                var failure = Conceivable.failure(Genotype.wildType().with(pair));
                assertTrue(failure.isPresent(), gene.key() + " " + pair.toTokens());
                assertTrue(failure.get().severity().lethal(), "a nonviable pair is a lethal");
                assertEquals(MiscarriageSigns.NONVIABLE, MiscarriageSigns.of(failure.get()));
            }
        }
        assertTrue(found >= 7, "KIT, MITF and PAX3 alone rule out seven homozygotes; found " + found);
    }

    /** The case the gap was about: two carriers, bred on the pregnancy path, lose about one in four. */
    @Test
    void twoCarriersOfANonviableAlleleLoseAboutOnePregnancyInFour() {
        Gene gene = null;
        Allele allele = null;
        search:
        for (Gene g : Genes.codeOrder()) {
            if (g == Genes.SEX || g.inheritance().copiesIn(Sex.MALE) != 2) {
                continue;
            }
            for (Allele a : g.alleles()) {
                if (!a.equals(g.defaultAllele()) && !g.canOccur(new AllelePair(a, a))
                        && g.canOccur(new AllelePair(a, g.defaultAllele()))) {
                    gene = g;
                    allele = a;
                    break search;
                }
            }
        }
        assertTrue(gene != null, "some autosomal gene rules out a homozygote");
        Genotype carrier = Genotype.wildType().with(new AllelePair(allele, gene.defaultAllele()));
        Genome dam = Genome.of(carrier, new SeededRng(1)).withSex(Sex.FEMALE);
        Genome sire = Genome.of(carrier, new SeededRng(2)).withSex(Sex.MALE);
        Conception.Mating mating = new Conception.Mating(dam, BreedLineage.FERAL, sire, BreedLineage.FERAL,
                new UUID(9L, 9L), "Test", "Sire", 0, GameteBias.NONE, GameteBias.NONE, "tester");
        Reproduction inHeat = new Reproduction(0.0, Optional.empty(), Reproduction.NEVER, List.of(),
                Reproduction.NEVER, Reproduction.NEVER, 0, Reproduction.NEVER);
        long peak = ReproTiming.DAY_TICKS * 3 / 4;

        int pregnancies = 0;
        int lost = 0;
        for (long seed = 0; seed < 3000; seed++) {
            Optional<Pregnancy> p = Conception.attempt(mating, inHeat, peak, ReproTiming.STANDARD, 0,
                    true, true, new SeededRng(seed)).pregnancy();
            if (p.isPresent()) {
                pregnancies++;
                if (p.get().hasEarlyLoss()) {
                    lost++;
                }
            }
        }
        double share = lost / (double) pregnancies;
        assertTrue(share > 0.18 && share < 0.33, gene.key() + " " + allele.token() + " carriers lost " + share);
    }
}
