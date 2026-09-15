package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.spec.BreedSpecParser;
import com.example.horsegenetics.common.breed.spec.BreedSpecWriter;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.trait.HealthContribution;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/** Magical herds (owner, 2026-09-15): one magical gene, one showing pair, the whole herd. */
class MagicalVariantTest {

    private static final BreedSpawnSettings.Magical ALWAYS = new BreedSpawnSettings.Magical(true, 1.0);

    private static Breed friesian() {
        return Breeds.get("friesian");
    }

    @Test
    void everyCandidateIsAMagicalGeneTheSheetLeavesOpen() {
        Breed breed = friesian();
        assertFalse(MagicalVariant.candidates(breed).isEmpty(), "a plain breed has magical genes to pick from");
        for (Gene gene : MagicalVariant.candidates(breed)) {
            assertTrue(Genes.magicalOrder().contains(gene), gene.key());
            assertFalse(BreedFounder.BODY_STAT_KEYS.contains(gene.key()), gene.key() + " is owned by the stat scores");
            assertFalse(breed.constrains(gene.key()), gene.key() + " is on the sheet already");
            assertFalse(gene.feralOnly(), gene.key());
            assertFalse(gene.inheritance().sexLinked(), gene.key());
            assertFalse(gene instanceof HealthContribution, gene.key());
        }
    }

    @Test
    void thePickedPairShows() {
        for (long seed = 0; seed < 200; seed++) {
            MagicalVariant v = MagicalVariant.pick(friesian(), new SeededRng(seed)).orElseThrow();
            assertFalse(v.gene().expressionOf(v.pair()).wildType(), v.gene().key() + "=" + v.pair().toTokens());
        }
    }

    @Test
    void theSameLeadSeedGivesTheWholeHerdTheSameVariant() {
        Optional<MagicalVariant> a = MagicalVariant.roll(friesian(), ALWAYS, new SeededRng(42L, "magical-herd"));
        Optional<MagicalVariant> b = MagicalVariant.roll(friesian(), ALWAYS, new SeededRng(42L, "magical-herd"));
        assertEquals(a, b);
    }

    @Test
    void everyMemberCarriesTheHerdsPairWhateverElseItRolls() {
        MagicalVariant v = MagicalVariant.pick(friesian(), new SeededRng(7L)).orElseThrow();
        for (long seed = 0; seed < 50; seed++) {
            Genome g = BreedFounder.roll(friesian(), new SeededRng(seed), seed % 2 == 0 ? Sex.MALE : Sex.FEMALE, v);
            assertEquals(v.pair(), g.genotype().pair(v.gene()));
        }
    }

    @Test
    void theChanceAndTheSwitchAreHonoured() {
        int magical = 0;
        BreedSpawnSettings.Magical fivePercent = BreedSpawnSettings.Magical.DEFAULT;
        for (long seed = 0; seed < 4000; seed++) {
            if (MagicalVariant.roll(friesian(), fivePercent, new SeededRng(seed, "magical-herd")).isPresent()) {
                magical++;
            }
            assertTrue(MagicalVariant.roll(friesian(), new BreedSpawnSettings.Magical(false, 1.0),
                    new SeededRng(seed)).isEmpty());
        }
        assertTrue(magical > 140 && magical < 260, "about 5% of 4000, got " + magical);
        assertTrue(MagicalVariant.roll(Breeds.FERAL_MIXED, ALWAYS, new SeededRng(1L)).isEmpty());
    }

    @Test
    void aBreedFileCanOptOutAndTheDefaultIsNeverWritten() {
        Breed optedOut = BreedSpecParser.parse(
                "{\"id\": \"x\", \"name\": \"X\", \"magical_variant\": false}", "test.json");
        assertFalse(optedOut.magicalVariant());
        assertTrue(MagicalVariant.roll(optedOut, ALWAYS, new SeededRng(1L)).isEmpty());
        assertTrue(BreedSpecWriter.write(optedOut).contains("\"magical_variant\": false"));

        Breed plain = BreedSpecParser.parse("{\"id\": \"x\", \"name\": \"X\"}", "test.json");
        assertTrue(plain.magicalVariant(), "every breed is in unless it says not");
        assertFalse(BreedSpecWriter.write(plain).contains("magical_variant"));
    }
}
