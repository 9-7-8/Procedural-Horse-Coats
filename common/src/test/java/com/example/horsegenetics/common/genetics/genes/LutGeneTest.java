package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.pattern.LutSet;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The LUT locus: a magical gene that paints nothing but, for a horse homozygous
 * for a variant allele, swaps the phase-2 gradient the natural genes resolve
 * against. One copy - or, later, two different variants - is a silent carrier.
 */
class LutGeneTest {

    private static final LutGene LUT = Genes.LUT;
    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private static AllelePair pair(String tokens) {
        return Genotype.parse(Codes.of("lut", tokens)).pair(LUT);
    }

    // --- the combination table -------------------------------------------

    @Test
    void oneCopyIsASilentCarrierTwoCopiesSwapThePalette() {
        assertTrue(LUT.expressionOf(pair("n/n")).wildType());

        Expression carrier = LUT.expressionOf(pair("Blupnk/n"));
        assertTrue(carrier.wildType(), "one Blupnk copy shows nothing");
        assertEquals("bluepink-carrier", carrier.id());

        Expression bluepink = LUT.expressionOf(pair("Blupnk/Blupnk"));
        assertFalse(bluepink.wildType(), "two Blupnk copies change the coat");
        assertEquals("bluepink", bluepink.id());
        assertTrue(bluepink.deterministic(), "every bluepink horse resolves against the same LUT");
    }

    @Test
    void itIsMagicalPaintsNothingItselfAndHasNoCarrot() {
        assertFalse(LUT.isNatural());
        assertTrue(LUT.affectsCoat(), "Blupnk/Blupnk is a real coat change");
        assertFalse(LUT.hasGeneCarrot());
        assertTrue(Genes.magicalOrder().contains(LUT));
        assertFalse(Genes.naturalOrder().contains(LUT));
        // the non-wild outcome has no painter of its own - the composer applies
        // the swap out of band
        Expression bluepink = LUT.expressionOf(pair("Blupnk/Blupnk"));
        var wild = Genotype.wildType();
        var ctx = new com.example.horsegenetics.common.coat.pattern.CoatBuildContext(
                wild, Epigenome.fromSeed(0), Skin.ADULT, true);
        assertEquals(null, bluepink.restrict(ctx, new com.example.horsegenetics.common.coat.pattern.PigmentField(N)));
        assertEquals(null, bluepink.tint(ctx, new com.example.horsegenetics.common.coat.pattern.PigmentField(N),
                new com.example.horsegenetics.common.coat.pattern.ColorField(N)));
    }

    // --- LutContribution ------------------------------------------------

    @Test
    void alternateLutOnlyFiresForATrueHomozygote() {
        assertTrue(LUT.alternateLut(pair("n/n"), Genotype.wildType()).isEmpty());
        assertTrue(LUT.alternateLut(pair("Blupnk/n"), Genotype.wildType()).isEmpty());
        assertEquals("bluepink", LUT.alternateLut(pair("Blupnk/Blupnk"), Genotype.wildType()).orElseThrow());
    }

    @Test
    void lutResourcesNamesTheTextureForEachKey() {
        assertEquals("textures/coat/lutbluepink.png", LUT.lutResources().get("bluepink"));
        assertEquals("textures/coat/lutgreenpink.png", LUT.lutResources().get("greenpink"));
        assertEquals(LUT.VARIANTS.size(), LUT.lutResources().size());
    }

    /**
     * <b>Every</b> variant behaves the same way, whichever it is - which is the
     * claim that stops a second palette being wired up as a special case of the
     * first. Walks {@code VARIANTS} rather than naming them, so the next one is
     * covered by existing.
     */
    @Test
    void everyVariantIsASilentCarrierAloneAndAPaletteInPairs() {
        for (LutGene.Variant v : LUT.VARIANTS) {
            String t = v.allele().token();
            assertTrue(LUT.expressionOf(pair(t + "/n")).wildType(), t + "/n should show nothing");
            assertEquals(v.carrier(), LUT.expressionOf(pair(t + "/n")), t);
            assertEquals(v.outcome(), LUT.expressionOf(pair(t + "/" + t)), t);
            assertFalse(v.outcome().wildType(), t + "/" + t + " should change the coat");
            assertTrue(v.outcome().deterministic(), t + " must not vary per horse");
            assertTrue(LUT.alternateLut(pair(t + "/n"), Genotype.wildType()).isEmpty(), t);
            assertEquals(v.lutKey(),
                    LUT.alternateLut(pair(t + "/" + t), Genotype.wildType()).orElseThrow(), t);
        }
    }

    /**
     * Two <i>different</i> variant alleles are not half of each palette - they
     * are the natural gradient. It is the rule the locus is built on and the
     * one a new palette is most likely to break.
     */
    @Test
    void aMixedPairOfVariantsIsTheNaturalGradient() {
        for (LutGene.Variant a : LUT.VARIANTS) {
            for (LutGene.Variant b : LUT.VARIANTS) {
                if (a.allele().equals(b.allele())) {
                    continue;
                }
                String tokens = a.allele().token() + "/" + b.allele().token();
                assertTrue(LUT.alternateLut(pair(tokens), Genotype.wildType()).isEmpty(),
                        tokens + " must resolve against the natural gradient");
                assertTrue(LUT.expressionOf(pair(tokens)).wildType(), tokens + " shows nothing");
            }
        }
    }

    // --- founders ------------------------------------------------------

    @Test
    void aVariantHomozygoteIsNeverAFounder() {
        SeededRng rng = new SeededRng(1234);
        int carriers = 0;
        for (int i = 0; i < 20_000; i++) {
            AllelePair p = Genotype.random(rng).pair(LUT);
            assertFalse(p.count(LUT.Blupnk) == 2, "a wild horse must never be homozygous for a LUT variant");
            if (p.count(LUT.Blupnk) == 1) {
                carriers++;
            }
        }
        // ~1 in 60 -> a few hundred carriers in 20k draws
        assertTrue(carriers > 150 && carriers < 550, "carrier rate off: " + carriers + " / 20000");
    }

    // --- the composer actually swaps the gradient ----------------------

    @Test
    void theComposerResolvesABluepinkHorseAgainstTheAlternateLut() {
        int[] template = new int[N * N];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                template[py * N + px] = 0xFFB0B0B0);

        GradientLut base = flat(0xFF9B4A28);      // warm
        GradientLut alt = flat(0xFF3A6ED8);       // cool - a different colour entirely
        LutSet luts = new LutSet(base, Map.of("bluepink", alt));

        Genotype carrier = Genotype.parse(Codes.of("lut", "Blupnk/n"));
        Genotype homozygote = Genotype.parse(Codes.of("lut", "Blupnk/Blupnk"));
        Epigenome epi = Epigenome.fromSeed(7);

        int[] carrierCoat = CoatTextureComposer.compose(carrier, epi, Skin.ADULT, true, template, luts);
        int[] baseCoat = CoatTextureComposer.compose(Genotype.wildType(), epi, Skin.ADULT, true, template, luts);
        int[] swapped = CoatTextureComposer.compose(homozygote, epi, Skin.ADULT, true, template, luts);

        assertTrue(Arrays.equals(carrierCoat, baseCoat), "a carrier keeps the natural gradient");
        assertFalse(Arrays.equals(swapped, baseCoat), "Blupnk/Blupnk resolves against the alternate LUT");
    }

    /** A LUT whose every entry is one colour, so a swap is unmistakable. */
    private static GradientLut flat(int argb) {
        int s = 4;
        int[] a = new int[s * s];
        Arrays.fill(a, argb);
        return new GradientLut(a, s, s);
    }
}
