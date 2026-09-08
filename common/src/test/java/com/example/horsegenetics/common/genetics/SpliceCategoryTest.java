package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The themed random-splice carrots. The properties worth pinning are the ones
 * a player would notice being broken and nobody would notice in a diff: that a
 * themed pool can never be less safe than the unthemed one, that the base coat
 * stays out of all of them, and that the carrot sold as a <b>positive</b>
 * health splice cannot roll a horse a worse one.
 */
class SpliceCategoryTest {

    /**
     * The load-bearing safety property. Every themed carrot draws from a subset
     * of {@link SpliceSafety#pool()}, so the lethal and heart-costing loci are
     * out of all of them for free - and stay out when somebody adds a gene.
     */
    @Test
    void everyThemedPoolIsASubsetOfTheSafePool() {
        List<Gene> safe = SpliceSafety.pool();
        for (SpliceCategory category : SpliceCategory.values()) {
            for (Gene gene : SpliceCategory.pool(category)) {
                assertTrue(safe.contains(gene),
                        category.id() + " offers " + gene.key() + ", which the safety pool excludes");
            }
        }
    }

    @Test
    void theUnthemedCarrotIsStillTheWholeSafePool() {
        assertEquals(SpliceSafety.pool(), SpliceCategory.pool(SpliceCategory.ANY));
        assertEquals(SpliceSafety.pool(), SpliceCategory.pool(null));
    }

    /**
     * A carrot sold as "markings" must not turn a black horse chestnut. The
     * three base-coat loci are reachable only through the unthemed carrot, which
     * promises nothing about what it rolls.
     */
    @Test
    void theBaseCoatIsInNoThemedCarrot() {
        for (Gene gene : List.of(Genes.EXTENSION, Genes.AGOUTI, Genes.SHADE)) {
            assertNull(SpliceCategory.of(gene), gene.key() + " should be in no themed carrot");
            for (SpliceCategory category : SpliceCategory.values()) {
                if (category != SpliceCategory.ANY) {
                    assertFalse(SpliceCategory.pool(category).contains(gene),
                            category.id() + " must not offer " + gene.key());
                }
            }
        }
    }

    @Test
    void noThemedPoolIsEmpty() {
        for (SpliceCategory category : SpliceCategory.values()) {
            assertFalse(SpliceCategory.pool(category).isEmpty(), category.id() + " has nothing in it");
        }
    }

    @Test
    void aGeneBelongsToAtMostOneTheme() {
        for (Gene gene : SpliceSafety.pool()) {
            int found = 0;
            for (SpliceCategory category : SpliceCategory.values()) {
                if (category != SpliceCategory.ANY && SpliceCategory.pool(category).contains(gene)) {
                    found++;
                }
            }
            assertTrue(found <= 1, gene.key() + " is in " + found + " themes");
        }
    }

    @Test
    void everyMagicalCarrotGeneIsMagicalAndEveryOtherThemeIsNatural() {
        for (Gene gene : SpliceCategory.pool(SpliceCategory.MAGICAL)) {
            assertFalse(gene.isNatural(), gene.key());
        }
        for (SpliceCategory category : List.of(SpliceCategory.DILUTION, SpliceCategory.WHITE,
                SpliceCategory.MARKING, SpliceCategory.PERFORMANCE)) {
            for (Gene gene : SpliceCategory.pool(category)) {
                assertTrue(gene.isNatural(), gene.key() + " is magical but is in " + category.id());
            }
        }
    }

    /**
     * The one that would be a real bug: {@code HMGA2}'s pony allele and
     * {@code LCORL}'s short copy are both perfectly safe and both make a horse
     * slower, and a carrot that says "positive" must not be able to roll them.
     */
    @Test
    void thePerformanceCarrotCanOnlyMakeAHorseBetter() {
        Traits baseline = HorseTraits.baseline();
        List<Gene> pool = SpliceCategory.pool(SpliceCategory.PERFORMANCE);
        assertFalse(pool.isEmpty());
        for (Gene gene : pool) {
            List<AllelePair> pairs = SpliceCategory.pairsFor(gene, SpliceCategory.PERFORMANCE);
            assertFalse(pairs.isEmpty(), gene.key() + " is on the carrot with nothing to draw");
            for (AllelePair pair : pairs) {
                Traits t = HorseTraits.resolve(Genotype.wildType().with(pair));
                assertTrue(t.speed() >= baseline.speed(), gene.key() + " " + pair.toTokens() + " speed");
                assertTrue(t.health() >= baseline.health(), gene.key() + " " + pair.toTokens() + " health");
                assertTrue(t.jump() >= baseline.jump(), gene.key() + " " + pair.toTokens() + " jump");
                assertTrue(t.speed() > baseline.speed() || t.health() > baseline.health()
                                || t.jump() > baseline.jump(),
                        gene.key() + " " + pair.toTokens() + " is not an improvement at all");
            }
        }
    }

    /** Only the performance carrot narrows the pair draw; the rest use the gene's own table. */
    @Test
    void onlyThePerformanceCarrotNarrowsThePairDraw() {
        for (SpliceCategory category : SpliceCategory.values()) {
            if (category == SpliceCategory.PERFORMANCE) {
                continue;
            }
            for (Gene gene : SpliceCategory.pool(category)) {
                assertTrue(SpliceCategory.pairsFor(gene, category).isEmpty(),
                        category.id() + " narrowed " + gene.key());
            }
        }
    }

    @Test
    void everyThemeRoundTripsThroughItsCarrotToken() {
        for (SpliceCategory category : SpliceCategory.values()) {
            CarrotEffect effect = new CarrotEffect.GeneSplice(category);
            CarrotEffect back = CarrotEffect.parse(effect.id()).orElse(null);
            assertNotNull(back, effect.id());
            assertEquals(effect, back);
        }
        // The unthemed carrot keeps the token it has always written.
        assertEquals("gene_splice", new CarrotEffect.GeneSplice().id());
        assertEquals(new CarrotEffect.GeneSplice(SpliceCategory.ANY),
                CarrotEffect.parse("gene_splice").orElseThrow());
        assertTrue(CarrotEffect.parse("gene_splice:nonsense").isEmpty());
    }
}
