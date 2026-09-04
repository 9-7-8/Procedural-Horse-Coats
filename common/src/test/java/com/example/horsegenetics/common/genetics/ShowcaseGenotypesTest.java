package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The horse dimension's stock draw: a wild founder with a floor under it, so a
 * pen is never a plain horse. See {@link ShowcaseGenotypes}.
 */
class ShowcaseGenotypesTest {

    private static final int DRAWS = 4_000;

    @Test
    void everyDrawExpressesANaturalGeneBesidesExtensionAndAgouti() {
        List<Gene> pool = ShowcaseGenotypes.naturalShowcaseGenes();
        assertTrue(pool.size() > 1, "no natural showcase genes registered");
        for (int i = 0; i < DRAWS; i++) {
            Genotype g = ShowcaseGenotypes.random(new SeededRng(i));
            assertTrue(pool.stream().anyMatch(gene -> expresses(g, gene)),
                    "plain horse at seed " + i + ": " + GeneCodeDisplay.shortForm(g));
        }
    }

    /**
     * The magical half is a coin, not a guarantee - so it is asserted as a
     * proportion. Masking combinations do not count, here or in the draw: a
     * quarter of all founders carry the diagnostic test gene, which paints flat
     * over everything, and letting that satisfy the floor would exempt a
     * quarter of the corridor from it.
     */
    @Test
    void aboutHalfOfDrawsExpressAMagicalGene() {
        List<Gene> magical = ShowcaseGenotypes.magicalShowcaseGenes();
        assertTrue(!magical.isEmpty(), "no magical showcase genes registered");
        int with = 0;
        for (int i = 0; i < DRAWS; i++) {
            Genotype g = ShowcaseGenotypes.random(new SeededRng(i));
            if (magical.stream().anyMatch(gene -> expresses(g, gene))) {
                with++;
            }
        }
        double share = with / (double) DRAWS;
        assertTrue(share > 0.44 && share < 0.60,
                "magical share drifted to " + share + " (expected about "
                        + ShowcaseGenotypes.MAGICAL_CHANCE + ")");
    }

    /** Expressing, and not by hiding everything else. */
    private static boolean expresses(Genotype genotype, Gene gene) {
        Expression e = genotype.expressionOf(gene);
        return !e.wildType() && !e.masks();
    }

    /** A forced combination never masks - a horse that hides every other gene is not a showcase. */
    @Test
    void noShowcasePairMasksEveryOtherGene() {
        for (Gene gene : ShowcaseGenotypes.naturalShowcaseGenes()) {
            for (AllelePair pair : ShowcaseGenotypes.showcasePairs(gene)) {
                Expression e = gene.expressionOf(pair);
                assertTrue(!e.masks() && !e.wildType(),
                        gene.key() + " offered " + pair.toTokens() + " as a showcase pair");
                assertTrue(gene.canOccur(pair), gene.key() + " offered an impossible pair " + pair.toTokens());
            }
        }
    }

    /** Extension and agouti are the base colour, not a marking - never forced. */
    @Test
    void extensionAndAgoutiAreNotInTheForcedPool() {
        List<Gene> pool = ShowcaseGenotypes.naturalShowcaseGenes();
        assertTrue(!pool.contains(Genes.EXTENSION));
        assertTrue(!pool.contains(Genes.AGOUTI));
        assertTrue(pool.stream().allMatch(Gene::affectsCoat));
    }

    /**
     * The pen sign has to survive a random draw. This is the property-test
     * replacement for the catalogue's per-entry sign check, now that the
     * corridor is random ({@code wiki/roadmap.html} §8): {@code wrap} may
     * overflow its <b>last</b> line, but it must never lose a gene.
     */
    @Test
    void everyDrawWrapsOntoTheSignWithoutLosingAnything() {
        int widestLast = 0;
        for (int i = 0; i < DRAWS; i++) {
            Genotype g = ShowcaseGenotypes.random(new SeededRng(i));
            List<String> lines = GeneCodeDisplay.wrap(g, 3, 15);
            assertTrue(lines.size() <= 3, "too many sign lines: " + lines);
            for (int k = 0; k < lines.size() - 1; k++) {
                assertTrue(lines.get(k).length() <= 15, "line too wide: '" + lines.get(k) + "'");
            }
            widestLast = Math.max(widestLast, lines.get(lines.size() - 1).length());
            assertEquals(GeneCodeDisplay.shortForm(g), String.join(" ", lines));
        }
        assertTrue(widestLast <= 200, "the overflowing last sign line has grown to " + widestLast + " chars");
    }

    /** Pure: the same seed always makes the same horse. */
    @Test
    void theDrawIsDeterministicInItsSeed() {
        for (int i = 0; i < 200; i++) {
            assertEquals(ShowcaseGenotypes.random(new SeededRng(i)).toCode(),
                    ShowcaseGenotypes.random(new SeededRng(i)).toCode());
        }
    }
}
