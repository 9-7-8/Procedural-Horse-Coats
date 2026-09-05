package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.SeededRng;
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
            Traits t = HorseTraits.resolve(g.genotype(), g.epigenome(),
                    Breeds.get("thoroughbred").statTargets(), true);
            assertTrue(t.speed() > HorseTraits.BASE_SPEED * 1.85,
                    "seed " + s + " speed " + t.speed() + " vs base " + HorseTraits.BASE_SPEED);
        }
    }

    @Test
    void falabellaResolvesTiny() {
        for (long s = 0; s < 60; s++) {
            Genome g = BreedFounder.roll(Breeds.get("falabella"), new SeededRng(s));
            Traits t = HorseTraits.resolve(g.genotype(), g.epigenome(),
                    Breeds.get("falabella").statTargets(), true);
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
    void unknownBreedIsTheUnconstrainedRoll() {
        // two different seeds diverge -> nothing is being pinned
        Genotype a = roll("unknown", 1);
        Genotype b = roll("unknown", 2);
        assertFalse(a.toCode().equals(b.toCode()));
    }
}
