package com.example.horsegenetics.common.stable;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.SpliceSafety;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Severity;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The horses a generated stable is filled with. A player did not choose to open
 * this box - they walked past a building - so the property that matters most is
 * the one about harm: whatever a stable promises, it may not hand out a horse
 * that is worse off than a wild one.
 */
class StableSpawnTest {

    private static StableSpawn stable(StableSpawn.Magic magic, int rare, int minMagic, int maxMagic,
                                      String... breeds) {
        return new StableSpawn("horsegenetics:test", "Test", 7, 7, 4, List.of(breeds),
                magic, rare, GeneRarity.RARE, minMagic, maxMagic, 0.10, 0.5);
    }

    /**
     * Counted over {@link StableSpawn#magicalPool()} rather than over every
     * magical gene, because the four body-stat loci are magical and are not
     * <i>magic</i>: they are how a breed gets its speed and height, and every
     * horse in the game has all four set.
     */
    private static int homozygousMagicLoci(Genome genome) {
        int n = 0;
        for (Gene gene : StableSpawn.magicalPool()) {
            if (!gene.atBaseline(genome.genotype().pair(gene))
                    && genome.genotype().pair(gene).homozygous()) {
                n++;
            }
        }
        return n;
    }

    private static int magicalLociCarried(Genome genome) {
        int n = 0;
        for (Gene gene : StableSpawn.magicalPool()) {
            if (!gene.atBaseline(genome.genotype().pair(gene))) {
                n++;
            }
        }
        return n;
    }

    /**
     * The load-bearing one. Every locus a stable <i>forces</i> comes from the
     * splice safety pool, so nothing a stable adds can kill a horse.
     *
     * <p>An <b>impairing</b> condition is a different matter and is allowed:
     * that is the breed's own genetics talking, exactly as it would on a wild
     * horse of the same breed, and a stable is not a promise of a healthy
     * animal. A <b>lethal</b> is not allowed, because a player finding a
     * building full of dying foals is not a surprise, it is a bug.
     */
    @Test
    void noStableCanProduceADyingHorse() {
        List<StableSpawn> stables = List.of(
                stable(StableSpawn.Magic.NONE, 2, 0, 0, "arabian"),
                stable(StableSpawn.Magic.ALLOWED, 0, 1, 11, "friesian"),
                stable(StableSpawn.Magic.NO_COAT, 0, 0, 0, "appaloosa", "american_paint"),
                stable(StableSpawn.Magic.ALLOWED, 3, 2, 4));
        for (StableSpawn spawn : stables) {
            for (long seed = 0; seed < 60; seed++) {
                Genome genome = spawn.roll(new SeededRng(seed));
                Traits traits = HorseTraits.resolve(genome.genotype(), genome.epigenome(), true);
                for (Condition condition : traits.conditions()) {
                    assertFalse(condition.severity().lethal(),
                            "seed " + seed + " produced " + condition.name());
                }
                // Not "at or above baseline": a pony breed's health band is
                // legitimately below it, and that is the breed talking rather
                // than the stable handing out a damaged horse. The disorder
                // check above is the one that matters.
                assertTrue(traits.health() > 0, "seed " + seed + " produced a horse with no health");
            }
        }
    }

    @Test
    void magicNoneLeavesNoMagicalGeneAtAll() {
        StableSpawn spawn = stable(StableSpawn.Magic.NONE, 1, 0, 0, "arabian");
        for (long seed = 0; seed < 40; seed++) {
            assertEquals(0, magicalLociCarried(spawn.roll(new SeededRng(seed))),
                    "seed " + seed + " carried a magical gene");
        }
    }

    @Test
    void magicNoCoatLeavesNoMagicalGeneThatPaints() {
        StableSpawn spawn = stable(StableSpawn.Magic.NO_COAT, 0, 0, 0, "appaloosa");
        for (long seed = 0; seed < 40; seed++) {
            Genome genome = spawn.roll(new SeededRng(seed));
            for (Gene gene : Genes.codeOrder()) {
                if (!gene.isNatural() && gene.affectsCoat()) {
                    assertTrue(gene.atBaseline(genome.genotype().pair(gene)),
                            "seed " + seed + " kept " + gene.key());
                }
            }
        }
    }

    @Test
    void everyHorseGetsAtLeastTheMinimumHomozygousMagic() {
        StableSpawn spawn = stable(StableSpawn.Magic.ALLOWED, 0, 1, 11, "friesian");
        for (long seed = 0; seed < 60; seed++) {
            assertTrue(homozygousMagicLoci(spawn.roll(new SeededRng(seed))) >= 1,
                    "seed " + seed + " came out with no homozygous magic");
        }
    }

    /** The 10% / 5% / ... ladder should make extras rare and never exceed the cap. */
    @Test
    void theExtraMagicLadderStaysInsideItsBounds() {
        StableSpawn spawn = stable(StableSpawn.Magic.ALLOWED, 0, 1, 11, "friesian");
        int mostSeen = 0;
        int withExtras = 0;
        int runs = 400;
        for (long seed = 0; seed < runs; seed++) {
            int n = homozygousMagicLoci(spawn.roll(new SeededRng(seed)));
            mostSeen = Math.max(mostSeen, n);
            if (n > 1) {
                withExtras++;
            }
        }
        assertTrue(mostSeen <= 11, "a horse got " + mostSeen + " magical loci against a cap of 11");
        // A tenth get a second and it halves from there, so "most horses have
        // exactly one" is the shape rather than an accident of these seeds.
        assertTrue(withExtras < runs / 2, withExtras + " of " + runs + " got extras");
    }

    @Test
    void rareNaturalsAreForcedAndAreActuallyRare() {
        StableSpawn spawn = stable(StableSpawn.Magic.NONE, 2, 0, 0, "arabian");
        for (long seed = 0; seed < 40; seed++) {
            Genome genome = spawn.roll(new SeededRng(seed));
            int rare = 0;
            for (Gene gene : Genes.codeOrder()) {
                if (gene.isNatural()
                        && gene.rarity().ordinal() >= GeneRarity.RARE.ordinal()
                        && !gene.atBaseline(genome.genotype().pair(gene))) {
                    rare++;
                }
            }
            assertTrue(rare >= 2, "seed " + seed + " carried only " + rare + " rare naturals");
        }
    }

    @Test
    void theRareNaturalPoolIsSafeAndNatural() {
        StableSpawn spawn = stable(StableSpawn.Magic.NONE, 1, 0, 0);
        assertFalse(spawn.rareNaturalPool().isEmpty());
        for (Gene gene : spawn.rareNaturalPool()) {
            assertTrue(gene.isNatural(), gene.key());
            assertTrue(SpliceSafety.pool().contains(gene), gene.key());
        }
        assertFalse(StableSpawn.magicalPool().isEmpty());
        for (Gene gene : StableSpawn.magicalPool()) {
            assertFalse(gene.isNatural(), gene.key());
            assertTrue(SpliceSafety.pool().contains(gene), gene.key());
        }
    }

    @Test
    void theBreedIsAlwaysOneTheStableNamed() {
        StableSpawn spawn = stable(StableSpawn.Magic.NO_COAT, 0, 0, 0, "appaloosa", "american_paint");
        for (long seed = 0; seed < 40; seed++) {
            String id = spawn.breed(new SeededRng(seed)).id();
            assertTrue(id.equals("appaloosa") || id.equals("american_paint"), id);
        }
    }

    @Test
    void countStaysInsideItsRange() {
        StableSpawn spawn = new StableSpawn("horsegenetics:test", "Test", 2, 12, 0, List.of(),
                StableSpawn.Magic.ALLOWED, 0, GeneRarity.RARE, 0, 0, 0.1, 0.5);
        for (long seed = 0; seed < 200; seed++) {
            int n = spawn.rollCount(new SeededRng(seed));
            assertTrue(n >= 2 && n <= 12, "rolled " + n);
        }
        StableSpawn fixed = stable(StableSpawn.Magic.ALLOWED, 0, 0, 0);
        assertEquals(7, fixed.rollCount(new SeededRng(1L)));
    }
}
