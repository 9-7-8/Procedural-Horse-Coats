package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.HorseDiet;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The diet locus: that it takes two identical copies, that no breed carries it,
 * that a founder is never a carrier and a random splice never anything else,
 * and that the variant diets pick one item and keep picking it.
 */
class DietGeneTest {

    private static final DietGene DIET = Genes.DIET;

    private static Genotype with(Allele a, Allele b) {
        return Genotype.wildType().with(new AllelePair(a, b));
    }

    private static Genotype homozygous(Diet diet) {
        Allele a = DIET.alleleFor(diet);
        assertNotNull(a, diet + " should have an allele");
        return with(a, a);
    }

    // ------------------------------------------------------------------
    // The locus
    // ------------------------------------------------------------------

    @Test
    void everyNarrowDietHasAnAlleleAndTheWildTypeDoesNot() {
        for (Diet diet : Diet.values()) {
            boolean expected = diet != Diet.NORMAL && diet != Diet.NOTHING;
            assertEquals(expected, DIET.alleleFor(diet) != null, diet + " allele");
        }
        assertEquals(Diet.values().length - 1, DIET.alleles().size(),
                "one allele per narrow diet, plus the wild type");
    }

    @Test
    void itTakesTwoIdenticalCopies() {
        Allele lava = DIET.alleleFor(Diet.LAVA);
        Allele ingot = DIET.alleleFor(Diet.INGOT);

        assertEquals(Diet.LAVA, HorseDiet.resolve(with(lava, lava), null).diet());
        // one copy - an ordinary horse carrying a surprise
        assertEquals(Diet.NORMAL, HorseDiet.resolve(with(lava, DIET.n), null).diet());
        // two different narrow copies - an ordinary horse carrying two
        assertEquals(Diet.NORMAL, HorseDiet.resolve(with(lava, ingot), null).diet());
    }

    @Test
    void aWildTypeHorseIsLeftToVanilla() {
        assertSame(HorseDiet.NORMAL.diet(), HorseDiet.resolve(Genotype.wildType(), null).diet());
        assertFalse(HorseDiet.resolve(Genotype.wildType(), null).isSpecial());
    }

    /** It paints in neither phase, so it is out of every texture key and gets one gallery pen. */
    @Test
    void dietDoesNotTouchTheCoat() {
        assertFalse(DIET.affectsCoat());
        for (var expression : DIET.expressions()) {
            assertTrue(expression.wildType(), expression.id() + " should be a wild type");
        }
        assertEquals(homozygous(Diet.LAVA).coatCode(), Genotype.wildType().coatCode());
    }

    @Test
    void dietSortsBeforeEveryGeneThatPaints() {
        for (var gene : Genes.codeOrder()) {
            if (gene.affectsCoat()) {
                assertTrue(DIET.priority() < gene.priority(),
                        "diet must sort before " + gene.key() + " so a later gene can override it");
            }
        }
    }

    // ------------------------------------------------------------------
    // Where it comes from
    // ------------------------------------------------------------------

    /**
     * A feral founder has a narrow diet outright or not at all - the table
     * lists no heterozygote, so there is no such thing as a wild-caught carrier.
     */
    @Test
    void aFounderIsNeverACarrier() {
        Set<Diet> seen = new HashSet<>();
        for (long seed = 0; seed < 4000; seed++) {
            AllelePair pair = Genotype.random(new SeededRng(seed)).pair(DIET);
            assertTrue(pair.homozygous(), "founder at seed " + seed + " is a carrier: " + pair.toTokens());
            seen.add(HorseDiet.resolve(Genotype.wildType().with(pair), null).diet());
        }
        assertTrue(seen.size() > 6, "4000 founders should turn up a spread of diets, saw " + seen);
        assertTrue(seen.contains(Diet.NORMAL), "most founders should be ordinary");
    }

    /** The mirror image: a random splice hands over a carrier and never the diet itself. */
    @Test
    void theRandomSpliceOnlyEverHandsOverACarrier() {
        var table = DIET.spliceTable().orElseThrow();
        for (long seed = 0; seed < 4000; seed++) {
            AllelePair pair = table.draw(new SeededRng(seed));
            assertEquals(Diet.NORMAL, HorseDiet.resolve(Genotype.wildType().with(pair), null).diet(),
                    "a splice produced a diet outright at seed " + seed + ": " + pair.toTokens());
        }
    }

    /** No named breed carries any of it - {@link com.example.horsegenetics.common.genetics.Gene#feralOnly()}. */
    @Test
    void noBreedCarriesADiet() {
        assertTrue(DIET.feralOnly());
        for (Breed breed : Breeds.all()) {
            if (breed == Breeds.FERAL_MIXED) {
                continue;   // the unbred population is the one place it lives
            }
            for (long seed = 0; seed < 40; seed++) {
                Genome g = BreedFounder.roll(breed, new SeededRng(seed));
                assertTrue(g.genotype().pair(DIET).homozygousFor(DIET.n),
                        breed.id() + " seed " + seed + " carries a diet allele");
            }
        }
    }

    // ------------------------------------------------------------------
    // The variant diets
    // ------------------------------------------------------------------

    @Test
    void aVariantDietPicksOneAndKeepsPicking() {
        for (Diet diet : new Diet[]{Diet.INGOT, Diet.GEM}) {
            Genotype gt = homozygous(diet);
            Set<Integer> picked = new HashSet<>();
            for (long seed = 0; seed < 200; seed++) {
                Epigenome epi = Epigenome.fromSeed(seed);
                HorseDiet resolved = HorseDiet.resolve(gt, epi);
                assertEquals(diet, resolved.diet());
                assertTrue(resolved.variant() >= 0 && resolved.variant() < diet.variants(),
                        diet + " picked out-of-range variant " + resolved.variant());
                // the same horse asked twice gives the same answer
                assertEquals(resolved, HorseDiet.resolve(gt, epi));
                picked.add(resolved.variant());
            }
            assertEquals(diet.variants(), picked.size(),
                    diet + " should reach every one of its variants across 200 horses");
        }
    }

    @Test
    void aDietWithoutVariantsAlwaysAnswersZero() {
        for (Diet diet : Diet.values()) {
            if (diet.variants() > 0 || DIET.alleleFor(diet) == null) {
                continue;
            }
            assertEquals(0, HorseDiet.resolve(homozygous(diet), Epigenome.fromSeed(9L)).variant());
        }
    }

    // ------------------------------------------------------------------
    // The trade
    // ------------------------------------------------------------------

    /**
     * The whole bargain in one assertion: eating anything is the least
     * rewarding diet there is, and every narrow one beats it.
     */
    @Test
    void aNarrowerDietFeedsBetterThanEatingAnything() {
        double anything = Diet.ANYTHING.healPoints();
        for (Diet diet : Diet.values()) {
            if (diet == Diet.NORMAL || diet == Diet.NOTHING || diet == Diet.ANYTHING) {
                continue;
            }
            assertTrue(diet.healsFully() || diet.healPoints() > anything,
                    diet + " should out-feed eating anything");
        }
        assertFalse(Diet.ANYTHING.healsFully());
    }
}
