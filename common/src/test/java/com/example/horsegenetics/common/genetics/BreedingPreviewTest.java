package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.horse.Sex;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Punnett squares behind the browser's Breeding preview tab.
 *
 * <p>The load-bearing property is the last test: the odds this screen prints
 * have to be the odds {@link Genotype#breedWith} actually draws, or the tab is
 * a confident lie. It is checked by <b>breeding ten thousand foals and
 * counting</b> rather than by re-deriving the arithmetic, because re-deriving
 * it would just be this class's own implementation agreeing with itself.
 */
class BreedingPreviewTest {

    private static Genotype mare(String code) {
        return Genotype.parse(code).withSex(Sex.FEMALE);
    }

    private static Genotype stallion(String code) {
        return Genotype.parse(code).withSex(Sex.MALE);
    }

    private static BreedingPreview.Locus locusOf(Gene gene, Genotype dam, Genotype sire) {
        return BreedingPreview.locus(gene, dam, sire);
    }

    /** Every locus is a probability distribution: the outcomes sum to one. */
    @Test
    void outcomesAtEveryLocusSumToOne() {
        Genotype dam = Genotype.random(new SeededRng(4)).withSex(Sex.FEMALE);
        Genotype sire = Genotype.random(new SeededRng(9)).withSex(Sex.MALE);
        for (Gene gene : Genes.codeOrder()) {
            double total = 0;
            for (BreedingPreview.Outcome o : locusOf(gene, dam, sire).outcomes()) {
                total += o.chance();
            }
            assertEquals(1.0, total, 1e-9, gene.key());
        }
    }

    /** Carrier x carrier is one in four, which is the number the whole tab exists to show. */
    @Test
    void twoCarriersThrowTheHomozygoteOneTimeInFour() {
        Genotype dam = mare("horsegenetics.extension=E/e");
        Genotype sire = stallion("horsegenetics.extension=E/e");
        BreedingPreview.Locus locus = locusOf(Genes.EXTENSION, dam, sire);

        assertEquals(3, locus.outcomes().size());
        double chestnut = 0;
        for (BreedingPreview.Outcome o : locus.outcomes()) {
            if (o.pair().toTokens().equals("e/e")) {
                chestnut = o.chance();
            }
        }
        assertEquals(0.25, chestnut, 1e-9);
        assertFalse(locus.settled());
    }

    /** Both parents homozygous for the same thing: one outcome, and it is settled. */
    @Test
    void aSettledLocusHasOneOutcome() {
        Genotype dam = mare("horsegenetics.extension=E/E");
        Genotype sire = stallion("horsegenetics.extension=E/E");
        BreedingPreview.Locus locus = locusOf(Genes.EXTENSION, dam, sire);
        assertEquals(1, locus.outcomes().size());
        assertEquals(1.0, locus.outcomes().get(0).chance(), 1e-9);
        assertTrue(locus.settled());
    }

    /** A settled locus is dropped from the grouping unless it is asked for. */
    @Test
    void groupingHidesSettledLociByDefault() {
        Genotype dam = Genotype.random(new SeededRng(1)).withSex(Sex.FEMALE);
        Genotype sire = Genotype.random(new SeededRng(2)).withSex(Sex.MALE);
        int hidden = distinctGenes(BreedingPreview.groups(dam, sire, false)).size();
        Map<String, Integer> all = distinctGenes(BreedingPreview.groups(dam, sire, true));
        assertTrue(all.size() > hidden,
                "showing everything must show at least as much: " + all.size() + " vs " + hidden);
        assertEquals(Genes.codeOrder().size(), all.size(),
                "with settled loci included, every registered gene should appear somewhere");
    }

    /**
     * A gene may be listed under more than one heading, but <b>only</b> across
     * the body axes - {@code HMGA2} really does move size, speed, jump and
     * health, and a player reading Speed wants to see it there. What must never
     * happen is the same gene under a body heading <i>and</i> under Coat or
     * Abilities: those three are the mutually exclusive categories, and a gene
     * in two of them means {@code GeneCategory} and the grouping disagree.
     */
    @Test
    void aGeneRepeatsOnlyAcrossTheBodyAxes() {
        Genotype dam = Genotype.random(new SeededRng(7)).withSex(Sex.FEMALE);
        Genotype sire = Genotype.random(new SeededRng(8)).withSex(Sex.MALE);
        Map<String, Integer> outsideBody = new HashMap<>();
        for (BreedingPreview.Group group : BreedingPreview.groups(dam, sire, true)) {
            boolean bodyAxis = !group.label().equals("Coat")
                    && !group.label().equals("Disorders")
                    && !group.label().startsWith("Abilities");
            if (!bodyAxis) {
                for (BreedingPreview.Locus locus : group.loci()) {
                    outsideBody.merge(locus.gene().key(), 1, Integer::sum);
                }
            }
        }
        for (Map.Entry<String, Integer> e : outsideBody.entrySet()) {
            assertEquals(1, e.getValue(), e.getKey() + " is listed under two non-body headings");
        }
    }

    /**
     * <b>The one that matters.</b> The printed odds are checked against the
     * real draw: breed the same pair ten thousand times and every predicted
     * combination should turn up at about its predicted rate, and nothing else
     * should turn up at all.
     */
    @Test
    void thePredictedOddsMatchWhatBreedingActuallyDraws() {
        Genotype dam = mare("horsegenetics.extension=E/e-horsegenetics.agouti=A/a");
        Genotype sire = stallion("horsegenetics.extension=E/e-horsegenetics.agouti=a/a");

        for (Gene gene : List.of(Genes.EXTENSION, Genes.AGOUTI)) {
            Map<String, Double> predicted = new HashMap<>();
            for (BreedingPreview.Outcome o : locusOf(gene, dam, sire).outcomes()) {
                predicted.put(o.pair().toTokens(), o.chance());
            }

            int trials = 10_000;
            Map<String, Integer> drawn = new HashMap<>();
            SeededRng rng = new SeededRng(20260907L);
            for (int i = 0; i < trials; i++) {
                drawn.merge(dam.breedWith(sire, rng).pair(gene).toTokens(), 1, Integer::sum);
            }

            for (String tokens : drawn.keySet()) {
                assertTrue(predicted.containsKey(tokens),
                        gene.key() + " drew " + tokens + ", which the preview said was impossible");
            }
            for (Map.Entry<String, Double> e : predicted.entrySet()) {
                double actual = drawn.getOrDefault(e.getKey(), 0) / (double) trials;
                assertEquals(e.getValue(), actual, 0.02,
                        gene.key() + " " + e.getKey() + ": predicted " + e.getValue() + ", drew " + actual);
            }
        }
    }

    /** Gene key to how many groups it appears in. */
    private static Map<String, Integer> distinctGenes(List<BreedingPreview.Group> groups) {
        Map<String, Integer> seen = new HashMap<>();
        for (BreedingPreview.Group group : groups) {
            for (BreedingPreview.Locus locus : group.loci()) {
                seen.merge(locus.gene().key(), 1, Integer::sum);
            }
        }
        return seen;
    }
}
