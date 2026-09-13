package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.spec.BreedSpecParser;
import com.example.horsegenetics.common.breed.spec.BreedSpecWriter;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.HorseDiet;
import com.example.horsegenetics.common.genetics.eye.EyeHue;
import com.example.horsegenetics.common.genetics.genes.AbstractMagicStatGene;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The Dhampir breed, and the strains that make it possible.</b> The old
 * dhampir gene is five loci now; this pins that the breed puts them back together
 * the way the owner described - "either a horse with recessive white and all the
 * boosts, or brown with just the demon eyes and heterozygous for all the boosts" -
 * and never a founder that is half of each.
 */
class DhampirBreedTest {

    private static final List<AbstractMagicStatGene> STATS =
            List.of(Genes.MAGIC_HEALTH, Genes.MAGIC_SPEED, Genes.MAGIC_JUMP);

    private static Breed dhampir() {
        Breed b = Breeds.get("dhampir");
        assertNotNull(b, "the dhampir breed file should be registered");
        return b;
    }

    @Test
    void itIsANightBreedWithTwoStrains() {
        Breed b = dhampir();
        assertEquals(SpawnTime.NIGHT, b.spawnTime());
        assertTrue(b.magical());
        assertEquals(2, b.strains().size());
    }

    @Test
    void everyFounderIsWholeWhiteOrWholeBrownAndNeverAMix() {
        Breed b = dhampir();
        int white = 0;
        int n = 2000;
        for (long seed = 0; seed < n; seed++) {
            Genotype g = BreedFounder.roll(b, new SeededRng(seed)).genotype();
            // the face is on every founder
            assertEquals(EyeHue.RED, Genes.EYE_COLOUR_RIGHT.hueOf(g.pair(Genes.EYE_COLOUR_RIGHT)));
            assertEquals(EyeHue.RED, Genes.EYE_COLOUR_LEFT.hueOf(g.pair(Genes.EYE_COLOUR_LEFT)));
            assertTrue(Genes.EYE_GLOW_SCLERA_RIGHT.glows(g.pair(Genes.EYE_GLOW_SCLERA_RIGHT)));

            int copies = g.pair(Genes.MAGIC_WHITE).count(Genes.MAGIC_WHITE.Wm);
            assertTrue(copies == 1 || copies == 2, "seed " + seed + ": every founder carries magic white");
            for (AbstractMagicStatGene stat : STATS) {
                assertEquals(copies, g.pair(stat).count(stat.vampiric), stat.key() + " disagrees at seed " + seed);
            }
            assertEquals(copies, g.pair(Genes.SUN_SENSITIVITY).count(Genes.SUN_SENSITIVITY.Sun));
            AllelePair diet = g.pair(Genes.DIET);
            assertEquals(copies, diet.count(Genes.DIET.alleleFor(Diet.BLOOD)));
            if (copies == 2) {
                white++;
            }
        }
        assertEquals(0.10, white / (double) n, 0.025, "one founder in ten is white");
    }

    @Test
    void aWhiteDhampirIsTheWholeAnimalAndABrownOneIsHalfTheStrength() {
        Breed b = dhampir();
        Genome whiteOne = null;
        Genome brownOne = null;
        for (long seed = 0; whiteOne == null || brownOne == null; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            if (Genes.MAGIC_WHITE.isWhite(g.genotype().pair(Genes.MAGIC_WHITE))) {
                whiteOne = g;
            } else {
                brownOne = g;
            }
        }
        Genotype white = whiteOne.genotype();
        Genotype brown = brownOne.genotype();

        assertTrue(Genes.SUN_SENSITIVITY.isSensitive(white.pair(Genes.SUN_SENSITIVITY)));
        assertEquals(Diet.BLOOD, HorseDiet.resolve(white, null).diet());
        assertFalse(Genes.SUN_SENSITIVITY.isSensitive(brown.pair(Genes.SUN_SENSITIVITY)));
        assertEquals(Diet.NORMAL, HorseDiet.resolve(brown, null).diet());

        // The same horse with the stat loci wiped: the ratio is the vampiric allele alone.
        Traits whiteT = HorseTraits.resolve(white);
        Traits brownT = HorseTraits.resolve(brown);
        Traits whitePlain = HorseTraits.resolve(wipeStats(white));
        Traits brownPlain = HorseTraits.resolve(wipeStats(brown));
        assertEquals(3.0, whiteT.health() / whitePlain.health(), 1e-6);
        assertEquals(1.5, whiteT.speed() / whitePlain.speed(), 1e-6);
        assertEquals(2.0, whiteT.jump() / whitePlain.jump(), 1e-6);
        assertEquals(2.0, brownT.health() / brownPlain.health(), 1e-6);
        assertEquals(1.25, brownT.speed() / brownPlain.speed(), 1e-6);
        assertEquals(1.5, brownT.jump() / brownPlain.jump(), 1e-6);
    }

    /** No breed but the Dhampir carries any of the split loci - a breed carries what it names. */
    @Test
    void noOtherBreedCarriesThePieces() {
        for (Breed breed : Breeds.all()) {
            if (breed == Breeds.FERAL_MIXED || breed.id().equals("dhampir")) {
                continue;
            }
            for (long seed = 0; seed < 10; seed++) {
                Genotype g = BreedFounder.roll(breed, new SeededRng(seed)).genotype();
                assertEquals(0, g.pair(Genes.MAGIC_WHITE).count(Genes.MAGIC_WHITE.Wm), breed.id());
                assertEquals(0, g.pair(Genes.SUN_SENSITIVITY).count(Genes.SUN_SENSITIVITY.Sun), breed.id());
                for (AbstractMagicStatGene stat : STATS) {
                    assertEquals(0, g.pair(stat).count(stat.vampiric), breed.id() + " " + stat.key());
                }
            }
        }
    }

    /** A strain survives the parse -> write -> parse round trip the breed designer depends on. */
    @Test
    void strainsRoundTrip() {
        Breed once = dhampir();
        String written = BreedSpecWriter.write(once);
        List<String> warnings = new ArrayList<>();
        Breed twice = BreedSpecParser.parse(written, "round-trip", warnings::add);
        assertEquals(once.strains(), twice.strains());
        assertEquals(written, BreedSpecWriter.write(twice));
        assertTrue(warnings.isEmpty(), "unexpected warnings: " + warnings);
    }

    /** The one sentence a founder log or the gene browser reads - every split locus is on the sheet. */
    @Test
    void theSheetNamesEverySplitLocus() {
        Breed b = dhampir();
        for (String key : List.of(Genes.MAGIC_WHITE.key(), Genes.SUN_SENSITIVITY.key(), Genes.DIET.key(),
                Genes.MAGIC_HEALTH.key(), Genes.MAGIC_SPEED.key(), Genes.MAGIC_JUMP.key())) {
            assertTrue(b.constrains(key), key);
        }
    }

    private static Genotype wipeStats(Genotype g) {
        Genotype out = g;
        for (AbstractMagicStatGene stat : STATS) {
            out = out.with(new AllelePair(stat.n, stat.n));
        }
        return out;
    }
}
