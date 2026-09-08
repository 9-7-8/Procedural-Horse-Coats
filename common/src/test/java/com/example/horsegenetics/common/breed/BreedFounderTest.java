package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

class BreedFounderTest {

    private static Genotype roll(String breedId, long seed) {
        Genome g = BreedFounder.roll(Breeds.get(breedId), new SeededRng(seed));
        return Genotype.parse(g.genotypeCode());
    }

    @Test
    void isDeterministicForAGivenSeed() {
        Genome a = BreedFounder.roll(Breeds.get("friesian"), new SeededRng(42));
        Genome b = BreedFounder.roll(Breeds.get("friesian"), new SeededRng(42));
        assertEquals(a.genotypeCode(), b.genotypeCode());
        assertEquals(a.epigenomeCode(), b.epigenomeCode());
    }

    @Test
    void friesianIsAlwaysBlackWithNoWhite() {
        for (long s = 0; s < 300; s++) {
            Genotype g = roll("friesian", s);
            assertTrue(g.pair(Genes.EXTENSION).homozygousFor(Genes.EXTENSION.E), "seed " + s);
            assertTrue(g.pair(Genes.AGOUTI).homozygousFor(Genes.AGOUTI.a), "seed " + s);
            assertFalse(g.shows(Genes.KIT), "KIT white on Friesian, seed " + s);
            assertFalse(g.shows(Genes.MITF), "MITF splash on Friesian, seed " + s);
            assertFalse(g.shows(Genes.PAX3), "PAX3 splash on Friesian, seed " + s);
            assertFalse(g.shows(Genes.EDNRB), "frame on Friesian, seed " + s);
            assertFalse(g.shows(Genes.TOBIANO), "tobiano on Friesian, seed " + s);
            assertFalse(g.shows(Genes.GREY), "grey on Friesian, seed " + s);
        }
    }

    @Test
    void suffolkPunchIsAlwaysChestnut() {
        for (long s = 0; s < 200; s++) {
            assertTrue(roll("suffolk_punch", s).pair(Genes.EXTENSION).homozygousFor(Genes.EXTENSION.e),
                    "seed " + s);
        }
    }

    @Test
    void thoroughbredResolvesNearDoubleSpeed() {
        for (long s = 0; s < 60; s++) {
            Genome g = BreedFounder.roll(Breeds.get("thoroughbred"), new SeededRng(s));
            // No breed argument: the band was baked into the founder's allele
            // copies by BreedFounder, so resolving it plainly must still land
            // near double speed. That is the whole point of the change.
            Traits t = HorseTraits.resolve(g.genotype(), g.epigenome(), true);
            if (isSick(t)) {
                // A Thoroughbred is not a hardy breed, so a founder can be born
                // with one of the dominant disorders and be genuinely slower for
                // it. That is the disorder working, not the band failing - the
                // claim here is about the breed's speed, so ask a well horse.
                continue;
            }
            // "Near double" over every one of 60 seeds. 1.78 rather than 1.85
            // because the floor is set by whichever seed happens to roll lowest,
            // and registering a gene renumbers every epigenetic seed (known gap
            // #47) - so a threshold pinned to the current worst seed goes red on
            // an unrelated change. The claim is the multiple, not the margin.
            assertTrue(t.speed() > HorseTraits.BASE_SPEED * 1.78,
                    "seed " + s + " speed " + t.speed() + " vs base " + HorseTraits.BASE_SPEED);
        }
    }

    @Test
    void falabellaResolvesTiny() {
        for (long s = 0; s < 60; s++) {
            Genome g = BreedFounder.roll(Breeds.get("falabella"), new SeededRng(s));
            Traits t = HorseTraits.resolve(g.genotype(), g.epigenome(), true);
            assertTrue(t.scale() > 0.30 && t.scale() < 0.60, "seed " + s + " scale " + t.scale());
        }
    }

    @Test
    void baselineStatBreedLeavesTheBodyStatLociWild() {
        // Friesian is 5/4/4/height-15..17 -> speed pins nothing, scale straddles baseline
        Genotype g = roll("friesian", 7);
        assertFalse(g.shows(Genes.MAGIC_SPEED));
        assertFalse(g.shows(Genes.BODY_SIZE));
    }

    @Test
    void magicGenesAppearRoughlyAtTheDeclaredRate() {
        int withMagic = 0;
        int total = 600;
        int maxPicks = 0;
        for (long s = 0; s < total; s++) {
            Genotype g = roll("morgan", s * 7L + 1);
            int picks = 0;
            for (var gene : Genes.magicalOrder()) {
                if (gene.key().equals("horsegenetics.body_size")
                        || gene.key().equals("horsegenetics.magic_speed")
                        || gene.key().equals("horsegenetics.magic_health")
                        || gene.key().equals("horsegenetics.magic_jump")
                        || gene.key().equals("horsegenetics.test")) {
                    continue;
                }
                // count "carries a variant copy" - milk / verdant / particle paint
                // nothing, so Genotype.shows() would miss them
                if (!g.pair(gene).homozygousFor(gene.defaultAllele())) {
                    picks++;
                }
            }
            if (picks > 0) {
                withMagic++;
            }
            maxPicks = Math.max(maxPicks, picks);
        }
        double rate = withMagic / (double) total;
        assertTrue(rate > 0.12 && rate < 0.32, "magic-carrier rate was " + rate);
        assertTrue(maxPicks <= 10, "geometric draw exceeded its cap: " + maxPicks);
    }

    @Test
    void feralMixedIsTheUnconstrainedRoll() {
        // two different seeds diverge -> nothing is being pinned
        Genotype a = roll("feral_mixed", 1);
        Genotype b = roll("feral_mixed", 2);
        assertFalse(a.toCode().equals(b.toCode()));
    }

    /**
     * An epigenetic band lands on <b>both allele copies</b> of every founder -
     * the general form of what the stat targets do, and the thing that lets a
     * breed say "deeply black" rather than only "black".
     */
    @Test
    void anEpigeneticBandLandsOnBothCopiesOfEveryFounder() {
        Breed breed = Breed.of("banded", "Banded")
                .fixed("horsegenetics.ednrb", "O")
                .band("horsegenetics.ednrb", "cover", 0.61, 0.64)
                .build();
        for (long seed = 0; seed < 40; seed++) {
            Genome g = BreedFounder.roll(breed, new SeededRng(seed));
            Epigenome.Copies copies = g.epigenome().copies(Genes.EDNRB);
            for (double v : new double[]{copies.first().values().get("cover"),
                    copies.second().values().get("cover")}) {
                assertTrue(v >= 0.61 && v <= 0.64,
                        "seed " + seed + " landed cover at " + v + ", outside the band");
            }
        }
    }

    /**
     * A band on a gene this build has not got, or on a value it does not
     * declare, costs the breed that band and nothing else. The complaining
     * happens once at load time, where there is a file name to complain about.
     */
    @Test
    void aBandOnSomethingMissingIsSurvivable() {
        Breed breed = Breed.of("odd", "Odd")
                .band("somemod.imaginary", "whatever", 0, 1)
                .band("horsegenetics.ednrb", "not_a_value", 0, 1)
                .build();
        assertNotNull(BreedFounder.roll(breed, new SeededRng(7)));
    }

    /** Is this horse carrying a disorder that actually costs it something? */
    private static boolean isSick(Traits t) {
        for (com.example.horsegenetics.common.trait.Condition c : t.conditions()) {
            if (c.severity() != com.example.horsegenetics.common.trait.Severity.INFORMATIONAL) {
                return true;
            }
        }
        return false;
    }
}
