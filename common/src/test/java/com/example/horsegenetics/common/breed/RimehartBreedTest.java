package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.genes.AntlerBloomGene;
import com.example.horsegenetics.common.genetics.genes.AntlersGene;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AntlerGenerator;
import com.example.horsegenetics.common.parts.AttachedPart;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** Symmetric (owner, 2026-10-01): no asymmetry band, so every rack matches itself. */
    @Test
    void everyRackIsSymmetricAndBandedUp() {
        Breed b = rimehart();
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            EpiValues v = g.expressedValues(Genes.ANTLERS);
            assertEquals(0.0, v.get(AntlersGene.ASYMMETRY), 0.0, "seed " + seed);
            List<AttachedPart> rack = GrownParts.of(g.genotype(), g.epigenome());
            assertEquals(rack.get(0).shape().style(), rack.get(1).shape().style(), "lopsided at seed " + seed);
            assertEquals(rack.get(0).shown(), rack.get(1).shown(), 0f, "seed " + seed);
            double size = v.get(AntlersGene.SIZE);
            double tines = v.get(AntlersGene.TINES);
            assertTrue(size >= 0.35 - 1e-9 && size <= 0.75 + 1e-9, "size " + size);
            assertTrue(tines >= 3.5 - 1e-9 && tines <= 6.5 + 1e-9, "tines " + tines);
        }
    }

    /**
     * <b>The coat, as the owner specified it.</b> A black base on every horse; half dun
     * (grulla), a quarter grey, a quarter carrying silver - three independent loci.
     */
    @Test
    void aBlackBaseHalfGrullaAQuarterGreyAQuarterSilver() {
        Breed b = rimehart();
        int n = 4000;
        int dun = 0;
        int grey = 0;
        int silver = 0;
        for (long seed = 0; seed < n; seed++) {
            Genotype g = BreedFounder.roll(b, new SeededRng(seed)).genotype();
            assertEquals("E/E", g.pair(Genes.EXTENSION).toTokens());
            assertEquals("a/a", g.pair(Genes.AGOUTI).toTokens());
            dun += g.pair(gene("horsegenetics.dun")).toTokens().contains("D") ? 1 : 0;
            grey += g.pair(gene("horsegenetics.grey")).toTokens().contains("G") ? 1 : 0;
            silver += g.pair(gene("horsegenetics.silver")).toTokens().contains("Z") ? 1 : 0;
        }
        assertShare("dun", dun, n, 0.50);
        assertShare("grey", grey, n, 0.25);
        assertShare("silver", silver, n, 0.25);
    }

    /**
     * <b>About one in twenty carries one natural white allele</b>, any of all of them,
     * and never two copies of one.
     */
    @Test
    void aboutOneInTwentyCarriesASingleNaturalWhite() {
        Breed b = rimehart();
        int n = 20_000;
        int carriers = 0;
        Set<String> alleles = new HashSet<>();
        for (long seed = 0; seed < n; seed++) {
            Genotype g = BreedFounder.roll(b, new SeededRng(seed)).genotype();
            boolean any = false;
            for (Gene w : GeneFamily.NATURAL_WHITE.members()) {
                AllelePair p = g.pair(w);
                int variant = 2 - p.count(w.defaultAllele());
                assertTrue(variant <= 1, w.key() + " " + p.toTokens() + " - two copies at seed " + seed);
                if (variant == 1) {
                    any = true;
                    alleles.add(w.key() + ":" + p.toTokens());
                }
            }
            carriers += any ? 1 : 0;
        }
        assertShare("natural white carriers", carriers, n, 0.05);
        assertTrue(alleles.size() > 40, "only " + alleles.size() + " distinct white alleles turned up");
    }

    private static void assertShare(String what, int count, int n, double expected) {
        double share = (double) count / n;
        assertTrue(Math.abs(share - expected) < 0.25 * expected,
                what + ": " + count + " of " + n + " is not about " + expected);
    }

    private static Gene gene(String key) {
        Gene g = Genes.byKeyOrNull(key);
        assertNotNull(g, key);
        return g;
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
