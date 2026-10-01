package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.RamHornGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The two ram-horned breeds</b> - the Mirrorgazer and the Not A Sheep - held to the
 * owner's spec as their {@code notes} write it down.
 */
class RamHornBreedsTest {

    private static Breed breed(String id) {
        Breed b = Breeds.get(id);
        assertNotNull(b, id + " should be a registered breed");
        return b;
    }

    private static Gene gene(String key) {
        Gene g = Genes.byKeyOrNull(key);
        assertNotNull(g, key);
        return g;
    }

    private static String pair(Genotype g, String key) {
        return g.pair(gene(key)).toTokens();
    }

    // ------------------------------------------------------------------
    // Mirrorgazer
    // ------------------------------------------------------------------

    @Test
    void everyMirrorgazerIsBlackWithGlowingRedIrisesOnBlackScleras() {
        Breed b = breed("mirrorgazer");
        assertTrue(b.magical());
        for (long seed = 0; seed < 300; seed++) {
            Genotype g = BreedFounder.roll(b, new SeededRng(seed)).genotype();
            assertEquals("E/E", pair(g, "horsegenetics.extension"));
            assertEquals("a/a", pair(g, "horsegenetics.agouti"));
            for (String side : new String[]{"left", "right"}) {
                assertEquals("Red/Red", pair(g, "horsegenetics.eye_colour_" + side));
                assertEquals("Blk/Blk", pair(g, "horsegenetics.eye_sclera_" + side));
                assertEquals("Glo/Glo", pair(g, "horsegenetics.eye_glow_iris_" + side));
            }
        }
    }

    @Test
    void halfAreShadowcreaturesAndEveryNightWatchVariationIsThere() {
        Breed b = breed("mirrorgazer");
        int shadow = 0;
        Set<String> watch = new HashSet<>();
        int n = 2000;
        for (long seed = 0; seed < n; seed++) {
            Genotype g = BreedFounder.roll(b, new SeededRng(seed)).genotype();
            shadow += g.pair(Genes.SHADOWCREATURE).homozygousFor(Genes.SHADOWCREATURE.Shc) ? 1 : 0;
            Gene nw = gene("horsegenetics.magic_night_watch");
            AllelePair plain = new AllelePair(nw.defaultAllele(), nw.defaultAllele());
            assertTrue(!nw.expressionOf(g.pair(nw)).equals(nw.expressionOf(plain)),
                    "a Mirrorgazer that does not watch, seed " + seed);
            watch.add(g.pair(nw).toTokens());
        }
        assertTrue(shadow > 0.42 * n && shadow < 0.58 * n, "shadowcreatures: " + shadow + " of " + n);
        assertEquals(5, watch.size(), "every night-watch variation: " + watch);
    }

    /**
     * Horns black at the base, any colour at the tip, every shape - and the prints the
     * colour of the tip, horse by horse.
     */
    @Test
    void hornsAreBlackToAColouredTipAndThePrintsMatchIt() {
        Breed b = breed("mirrorgazer");
        Set<Integer> shapes = new HashSet<>();
        Set<Integer> tips = new HashSet<>();
        for (long seed = 0; seed < 400; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            for (Sex sex : Sex.values()) {
                Genome as = g.withSex(sex);
                assertEquals(2, GrownParts.of(as.genotype(), as.epigenome()).size(), sex + " seed " + seed);
            }
            AttachedPart right = GrownParts.of(g.genotype(), g.epigenome()).get(0);
            assertEquals(0xFF141110, right.baseTint(), "a base that is not black, seed " + seed);
            assertTrue(right.twoTone(), "seed " + seed);
            shapes.add(right.shape().style() / RamHornGenerator.CURLS);
            tips.add(right.tipTint());
            List<GeneAbility> prints = Genes.MOLTEN_HOOVES.abilitiesFor(g.genotype().pair(Genes.MOLTEN_HOOVES),
                    g.genotype(), GeneEpigenetics.forGene(Genes.MOLTEN_HOOVES, g.genotype(), g.epigenome()));
            assertEquals(1, prints.size());
            assertEquals(right.tipTint() & 0xFFFFFF, ((GeneAbility.Emitter) prints.get(0)).color(),
                    "prints that do not match the horn tip, seed " + seed);
        }
        assertEquals(RamHornGenerator.FORMS, shapes.size(), "every shape");
        assertTrue(tips.size() > 300, "the tip colour should be any colour: " + tips.size());
    }

    // ------------------------------------------------------------------
    // Not A Sheep
    // ------------------------------------------------------------------

    /** Mostly white; every solid; the patterns with a natural gene; horns like real flocks. */
    @Test
    void notASheepComesInSheepColoursAndFlockHorns() {
        Breed b = breed("not_a_sheep");
        int n = 4000;
        int white = 0;
        int black = 0;
        int moorit = 0;
        int roan = 0;
        int pied = 0;
        int ramsHorned = 0;
        int ewesHorned = 0;
        Set<Integer> shapes = new HashSet<>();
        for (long seed = 0; seed < n; seed++) {
            Genome genome = BreedFounder.roll(b, new SeededRng(seed));
            Genotype g = genome.genotype();
            boolean cream = pair(g, "horsegenetics.matp").equals("Cr/Cr");
            boolean red = pair(g, "horsegenetics.extension").equals("e/e");
            boolean bay = !red && pair(g, "horsegenetics.agouti").equals("A/A");
            white += cream ? 1 : 0;
            moorit += !cream && red ? 1 : 0;
            black += !cream && !red && !bay ? 1 : 0;
            roan += pair(g, "horsegenetics.roan").contains("Rn") ? 1 : 0;
            pied += pair(g, "horsegenetics.tobiano").contains("To") ? 1 : 0;
            Genome ram = genome.withSex(Sex.MALE);
            Genome ewe = genome.withSex(Sex.FEMALE);
            List<AttachedPart> horns = GrownParts.of(ram.genotype(), ram.epigenome());
            ramsHorned += horns.isEmpty() ? 0 : 1;
            ewesHorned += GrownParts.of(ewe.genotype(), ewe.epigenome()).isEmpty() ? 0 : 1;
            if (!horns.isEmpty()) {
                shapes.add(horns.get(0).shape().style() / RamHornGenerator.CURLS);
                assertTrue(!horns.get(0).twoTone(), "no coloured tips on a sheep");
            }
        }
        assertTrue(white > 0.62 * n && white < 0.78 * n, "white " + white);
        assertTrue(moorit > 0.15 * n && moorit < 0.27 * n, "moorit " + moorit);
        assertTrue(black > 0.04 * n && black < 0.11 * n, "black " + black);
        assertTrue(roan > 0.05 * n && roan < 0.11 * n, "roan-grey " + roan);
        assertTrue(pied > 0.07 * n && pied < 0.13 * n, "pied " + pied);
        assertTrue(ramsHorned > 0.62 * n && ramsHorned < 0.78 * n, "horned rams " + ramsHorned);
        assertTrue(ewesHorned > 0.10 * n && ewesHorned < 0.20 * n, "horned ewes " + ewesHorned);
        assertEquals(RamHornGenerator.FORMS, shapes.size(), "every horn shape turns up in a flock");
    }
}
