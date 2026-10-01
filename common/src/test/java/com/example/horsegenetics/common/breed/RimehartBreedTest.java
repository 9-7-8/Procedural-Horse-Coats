package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.genes.AntlerBloomGene;
import com.example.horsegenetics.common.genetics.genes.AntlersGene;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AntlerGenerator;
import com.example.horsegenetics.common.parts.AttachedPart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The Rimehart</b> - the antlered breed, and the first breed to pin a value no
 * wild horse can carry. Every assertion here is a sentence in the file's own
 * {@code notes}, the way {@link NetherBreedsTest} holds its breeds: the notes are
 * the design, and this is what stops them drifting away from the pool.
 */
class RimehartBreedTest {

    private static final int FOUNDERS = 400;

    private static Breed rimehart() {
        Breed b = Breeds.get("rimehart");
        assertNotNull(b, "the Rimehart should be a registered breed");
        return b;
    }

    /** Antlers on both sexes - every founder carries Ant, so a mare is antlered too. */
    @Test
    void everyRimehartMareAndStallionIsAntlered() {
        Breed b = rimehart();
        assertTrue(b.magical());
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            assertTrue(g.genotype().pair(Genes.ANTLERS).count(Genes.ANTLERS.Ant) > 0, "seed " + seed);
            for (Sex sex : Sex.values()) {
                Genome as = g.withSex(sex);
                List<AttachedPart> parts = GrownParts.of(as.genotype(), as.epigenome());
                assertEquals(2, parts.size(), "a " + sex + " Rimehart without a rack at seed " + seed);
            }
        }
    }

    /** Brow-tined, or palmate where a founder carries Pal over it - never forked or spiked. */
    @Test
    void theRackIsBrowTinedOrPalmate() {
        Breed b = rimehart();
        int palmate = 0;
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            int habit = Genes.ANTLER_FORM.habitOf(g.genotype().pair(Genes.ANTLER_FORM));
            assertTrue(habit == AntlerGenerator.BROW || habit == AntlerGenerator.PALMATE,
                    "habit " + habit + " at seed " + seed);
            palmate += habit == AntlerGenerator.PALMATE ? 1 : 0;
        }
        assertTrue(palmate > 20 && palmate < 110, "about one in seven is palmate: " + palmate);
    }

    /**
     * <b>Lopsided, which no wild horse can be.</b> Asymmetry is banded 0.15 to 0.45,
     * every one of which is past the threshold, so the left antler is always a rack
     * of its own and carries fewer tines than the right.
     */
    @Test
    void everyRackIsAsymmetric() {
        Breed b = rimehart();
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            EpiValues v = g.expressedValues(Genes.ANTLERS);
            double asym = v.get(AntlersGene.ASYMMETRY);
            assertTrue(asym >= 0.15 - 1e-9 && asym <= 0.45 + 1e-9, "asymmetry " + asym + " at seed " + seed);
            List<AttachedPart> rack = GrownParts.of(g.genotype(), g.epigenome());
            assertTrue(rack.get(0).shape().style() != rack.get(1).shape().style(),
                    "a symmetric Rimehart at seed " + seed);
            assertTrue(rack.get(1).shown() < rack.get(0).shown(), "seed " + seed);
            double size = v.get(AntlersGene.SIZE);
            double tines = v.get(AntlersGene.TINES);
            assertTrue(size >= 0.35 - 1e-9 && size <= 0.75 + 1e-9, "size " + size);
            assertTrue(tines >= 3.5 - 1e-9 && tines <= 6.5 + 1e-9, "tines " + tines);
        }
    }

    /** Moss, never leaves or blossom, on every one that blooms. */
    @Test
    void aBloomingRimehartGrowsMoss() {
        Breed b = rimehart();
        int mossy = 0;
        int moss = AntlerBloomGene.GROWTHS.get(1).tint();
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            AttachedPart right = GrownParts.of(g.genotype(), g.epigenome()).get(0);
            if (right.blooms()) {
                mossy++;
                assertEquals(moss, right.bloomTint(), "seed " + seed);
            }
        }
        assertTrue(mossy > 25 && mossy < 110, "about one in seven blooms: " + mossy);
    }
}
