package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.BayShade;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The Greenwood</b> - a dark bay forest horse whose pool holds every variation of
 * the fawn, dryad and verdant loci. Each assertion is a sentence in the file's
 * {@code notes}, the way {@link NetherBreedsTest} holds its breeds.
 */
class GreenwoodBreedTest {

    /** Enough that a 2%-weight pair turns up dozens of times. */
    private static final int FOUNDERS = 3000;

    private static Breed greenwood() {
        Breed b = Breeds.get("greenwood");
        assertNotNull(b, "the Greenwood should be a registered breed");
        return b;
    }

    @Test
    void everyGreenwoodIsADarkBay() {
        Breed b = greenwood();
        assertTrue(b.magical());
        for (long seed = 0; seed < 500; seed++) {
            Genotype g = BreedFounder.roll(b, new SeededRng(seed)).genotype();
            assertEquals("E/E", g.pair(Genes.EXTENSION).toTokens(), "seed " + seed);
            assertEquals("A/A", g.pair(Genes.AGOUTI).toTokens(), "seed " + seed);
            BayShade.Shade shade = BayShade.shadeOf(g);
            assertTrue(shade == BayShade.Shade.LIVER || shade == BayShade.Shade.SEAL,
                    "a " + shade + " Greenwood at seed " + seed);
        }
    }

    /**
     * <b>Every variation that shows, and nothing that does not.</b> Each locus's pool is
     * exactly its expressing pairs, judged by the gene's own rule: dryad expresses from
     * any two variant copies (a mixed pair plants both), verdant only from a matched
     * pair, fawn from whatever its gene file says shows. So every Greenwood plants
     * something, spreads a cover and wears the spotted topline.
     */
    @Test
    void thePoolHoldsEveryShowingFawnDryadAndVerdantVariation() {
        Gene fawn = Genes.byKeyOrNull("horsegenetics.fawn");
        assertNotNull(fawn);
        assertEveryVariation(Genes.DRYAD, p -> p.count(Genes.DRYAD.defaultAllele()) == 0);
        assertEveryVariation(Genes.VERDANT, p -> !Genes.VERDANT.coverOf(p).isEmpty());
        assertEveryVariation(fawn, p -> !fawn.expressionOf(p).wildType());
    }

    private static void assertEveryVariation(Gene gene, Predicate<AllelePair> shows) {
        Breed b = greenwood();
        Set<String> seen = new HashSet<>();
        for (long seed = 0; seed < FOUNDERS; seed++) {
            AllelePair p = BreedFounder.roll(b, new SeededRng(seed)).genotype().pair(gene);
            assertTrue(shows.test(p), gene.key() + " " + p.toTokens() + " shows nothing, at seed " + seed);
            seen.add(p.toTokens());
        }
        Set<String> expected = new HashSet<>();
        for (AllelePair p : GenotypeCatalog.allPairsOf(gene)) {
            if (shows.test(p)) {
                expected.add(p.toTokens());
            }
        }
        assertEquals(expected, seen, gene.key() + ": the pool is missing a variation");
    }
}