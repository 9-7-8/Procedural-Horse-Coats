package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.pattern.LutSet;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The white lock. Three claims, and the middle one is the gene:
 *
 * <ul>
 *   <li>it paints <b>nothing</b> - no phase-1 restriction, no phase-3 tint;</li>
 *   <li>on a horse with white <i>and</i> a magical marking, it <b>changes the
 *       coat</b> - the marking is discarded over the white;</li>
 *   <li>on a horse with no white at all, it changes <b>nothing whatever</b>,
 *       which is what stops it being a general "dim everything" switch.</li>
 * </ul>
 *
 * <p>The third is the one worth having a test for. A lock built off the wrong
 * measurement - the resolved colour rather than the pigment, say - would still
 * pass the second claim while quietly eating markings on horses that have no
 * white on them at all, and nothing else here would notice.
 */
class ExtremeWhiteDominantGeneTest {

    private static final ExtremeWhiteDominantGene EWD = Genes.EXTREME_WHITE_DOMINANT;
    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    // ------------------------------------------------------------------

    @Test
    void itPaintsNothingInEitherPhase() {
        for (AllelePair pair : GenotypeCatalog.allPairsOf(EWD)) {
            Expression e = EWD.expressionOf(pair);
            assertNull(e.restrict(null, null), pair + " must not restrict pigment");
            assertNull(e.tint(null, null, null), pair + " must not return a tint");
        }
    }

    /** One copy is the whole of it - "white is final" has no second half to reach. */
    @Test
    void oneCopyIsTheWholeOfIt() {
        assertTrue(EWD.locksWhite(pair(EWD.EWD, EWD.EWD), null));
        assertTrue(EWD.locksWhite(pair(EWD.EWD, EWD.n), null));
        assertFalse(EWD.locksWhite(pair(EWD.n, EWD.n), null));
        assertEquals(EWD.expressionOf(pair(EWD.EWD, EWD.EWD)),
                EWD.expressionOf(pair(EWD.EWD, EWD.n)),
                "a heterozygote and a homozygote are the same horse");
    }

    /** The carrier is not a wild type: it genuinely changes what the horse looks like. */
    @Test
    void theExpressingCombinationIsNotAWildType() {
        assertFalse(EWD.expressionOf(pair(EWD.EWD, EWD.n)).wildType());
        assertTrue(EWD.expressionOf(pair(EWD.n, EWD.n)).wildType());
        assertTrue(EWD.affectsCoat());
        assertFalse(EWD.isNatural());
    }

    // ------------------------------------------------------------------
    // What it does to a bake
    // ------------------------------------------------------------------

    /**
     * A tobiano with magic zebra over it. Magic zebra subtracts 200% from every
     * channel and raises opacity, so it deliberately reads black over
     * <i>anything</i>, unpigmented white included - which makes it the loudest
     * possible test of a rule that says the white wins anyway.
     */
    @Test
    void theMarkingStopsAtTheWhiteWhenTheLockIsOn() {
        String white = "agouti=A/a tobiano=To/to magic_zebra=Mzeb/Mzeb";
        int[] without = bake(white);
        int[] with = bake(white + " extreme_white_dominant=EWD/n");

        assertTrue(moved(with, without) > 200,
                "the lock has to take a visible amount of the striping off the white");
    }

    /** And on a horse with nothing white on it, it is silent. */
    @Test
    void itIsSilentOnAHorseWithNoWhite() {
        String solid = "agouti=A/a magic_zebra=Mzeb/Mzeb";
        assertEquals(0, moved(bake(solid + " extreme_white_dominant=EWD/EWD"), bake(solid)),
                "with no white to lock, not one texel may move");
    }

    // ------------------------------------------------------------------

    private static AllelePair pair(Allele a, Allele b) {
        return new AllelePair(a, b);
    }

    private static int[] bake(String overrides) {
        return CoatTextureComposer.compose(genotype(overrides), Epigenome.fromSeed(7L),
                Skin.ADULT, true, template(), luts());
    }

    /** {@code "agouti=A/a tobiano=To/to"} - the spelling the icon baker uses. */
    private static Genotype genotype(String spec) {
        Genotype gt = Genotype.wildType();
        for (String kv : spec.trim().split("\\s+")) {
            String[] halves = kv.split("=");
            Gene gene = Genes.byKeyOrNull(Genes.NS + "." + halves[0]);
            String[] tokens = halves[1].split("/");
            gt = gt.with(new AllelePair(allele(gene, tokens[0]), allele(gene, tokens[1])));
        }
        return gt;
    }

    private static Allele allele(Gene gene, String token) {
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        throw new IllegalArgumentException(gene.key() + " has no allele " + token);
    }

    private static int moved(int[] a, int[] b) {
        int n = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                n++;
            }
        }
        return n;
    }

    /**
     * A synthetic gradient and a synthetic template, the same way
     * {@code CoatPipelineGoldenTest} builds them - so this test says nothing
     * about the shipped artwork and cannot go red when somebody recolours it.
     */
    private static LutSet luts() {
        int size = 32;
        int[] px = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                px[y * size + x] = 0xFF000000 | ((255 - x * 8) << 16) | ((255 - y * 8) << 8) | 0x80;
            }
        }
        return LutSet.of(new GradientLut(px, size, size));
    }

    private static int[] template() {
        int[] t = new int[N * N];
        HorseSkinGeometry.forEachTexel(Skin.ADULT,
                (px, py, part, face, point) -> t[py * N + px] = 0xFFF0F0F0);
        return t;
    }
}
