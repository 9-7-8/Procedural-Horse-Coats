package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatDataTest {

    private static final String BLACK = Genotype.wildType().toCode();
    private static final String BAY = Codes.of("extension", "E/e", "agouti", "A/a");
    /** Same bay, but also carrying grey - whose epigenetics are invisible on a foal-free adult check. */
    private static final String BAY_GREY = Codes.of("extension", "E/e", "agouti", "A/a", "grey", "G3/N");

    private static CoatData coat(String code, long seed) {
        return new CoatData(Genotype.parse(code), Epigenome.fromSeed(seed));
    }

    @Test
    void carriesGenotypeAndEpigenomeAndDerivesPhenotype() {
        CoatData c = coat(BLACK, 42L);
        assertEquals(CoatPhenotype.BLACK, c.phenotype());
        // Fitted to the alleles: a wild-type copy carries no numbers (2026-09-14).
        assertEquals(Epigenome.fromSeed(42L).alignedTo(Genotype.parse(BLACK)), c.epigenome());
    }

    @Test
    void deterministicCoatsIgnoreEpigeneticsInTheirTextureKey() {
        assertTrue(coat(BLACK, 1L).isDeterministic());
        assertEquals(coat(BLACK, 1L).textureKey(), coat(BLACK, 999L).textureKey());
    }

    @Test
    void nonDeterministicCoatsKeyOnTheirVisibleEpigenetics() {
        assertFalse(coat(BAY, 1L).isDeterministic());
        assertNotEquals(coat(BAY, 1L).textureKey(), coat(BAY, 2L).textureKey());
    }

    @Test
    void greyIsPerHorseToo() {
        assertFalse(coat(BAY_GREY, 1L).isDeterministic());
        assertNotEquals(coat(BAY_GREY, 1L).textureKey(), coat(BAY_GREY, 2L).textureKey());
    }

    @Test
    void equalityIsGenotypePlusEpigenome() {
        assertEquals(coat(BLACK, 7L), coat(BLACK, 7L));
        // A bay, not a black: a horse that is wild type everywhere carries no numbers except
        // fertility's (the one gene whose wild type reads its copy), so two blacks from
        // different seeds differ there and nowhere else.
        assertNotEquals(coat(BAY, 7L), coat(BAY, 8L));
        String fertility = com.example.horsegenetics.common.genetics.genes.FertilityGene.KEY;
        Epigenome seven = coat(BLACK, 7L).epigenome();
        Epigenome eight = coat(BLACK, 8L).epigenome();
        assertNotEquals(seven, eight);
        assertEquals(seven, eight.with(fertility, seven.copies(fertility)));
    }

    @Test
    void defaultIsAPlainBlackDeterministicHorse() {
        assertEquals(CoatPhenotype.BLACK, CoatData.DEFAULT.phenotype());
        assertTrue(CoatData.DEFAULT.isDeterministic());
    }

    // ------------------------------------------------------------------
    // The memo (#205): every key is computed once per CoatData, and must be
    // the value a fresh computation gives, or a horse wears the wrong coat.
    // ------------------------------------------------------------------

    /** Every key, glow set and part list, built straight from the genome - no memo involved. */
    private static void assertMemoMatchesAFreshComputation(CoatData c) {
        String texture = c.isDeterministic()
                ? c.genotype().coatCode()
                : c.genotype().coatCode() + "@"
                        + Long.toUnsignedString(c.epigenome().visibleFingerprint(c.genotype()), 16);
        Set<HorseSkinGeometry.Part> lit =
                com.example.horsegenetics.common.coat.pattern.CoatTextureComposer.glowParts(c.genotype());
        StringBuilder tag = new StringBuilder();
        for (HorseSkinGeometry.Part part : new TreeSet<>(lit)) {
            tag.append(tag.length() == 0 ? "" : "+").append(part.name());
        }
        String code = c.genotype().toCode();
        assertEquals(texture, c.textureKey(), code);
        assertEquals(texture + ":adult", c.meshKey(false), code);
        assertEquals(texture + ":foal", c.meshKey(true), code);
        assertEquals(texture + ":adult:glow:" + tag, c.glowKey(false), code);
        assertEquals(texture + ":foal:glow:" + tag, c.glowKey(true), code);
        assertEquals(lit, c.glowParts(), code);
        assertEquals(GrownParts.of(c.genotype(), c.epigenome()), c.grownParts(), code);
        // ... and the second ask is the first answer, not a rebuild.
        assertSame(c.textureKey(), c.textureKey(), code);
        assertSame(c.meshKey(true), c.meshKey(true), code);
        assertSame(c.glowKey(false), c.glowKey(false), code);
        assertSame(c.glowParts(), c.glowParts(), code);
        assertSame(c.grownParts(), c.grownParts(), code);
    }

    @Test
    void memoisedKeysAreTheFreshlyComputedOnesAcrossRandomHorses() {
        SeededRng rng = new SeededRng(205L);
        for (int i = 0; i < 200; i++) {
            Genotype g = Genotype.random(rng);
            assertMemoMatchesAFreshComputation(new CoatData(g, Epigenome.fromSeed(rng.nextLong())));
        }
        assertMemoMatchesAFreshComputation(CoatData.DEFAULT);
    }

    @Test
    void aGlowingHorseKeysItsMaskOnItsLitParts() {
        CoatData glowing = coat(Codes.of("extension", "E/e", "agouti", "A/a", "suntouched", "Sntch/n"), 3L);
        assertFalse(glowing.glowParts().isEmpty(), "suntouched's glow effect lights its parts outright");
        assertMemoMatchesAFreshComputation(glowing);
        assertNotEquals(glowing.meshKey(false) + ":glow:", glowing.glowKey(false));
    }

    @Test
    void memoisedCollectionsCannotBeChangedUnderTheRenderer() {
        CoatData glowing = coat(Codes.of("suntouched", "Sntch/Sntch"), 4L);
        assertThrows(UnsupportedOperationException.class, () -> glowing.glowParts().clear());
        assertThrows(UnsupportedOperationException.class, () -> glowing.grownParts().clear());
    }
}
