package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The eye-colour channel</b>, measured through the real pipeline - tiger eye
 * and the blue eye the white loci give, and the one rule that orders them.
 *
 * <p>The load-bearing assertion is {@link #theIrisTakesTheColourAndTheScleraDoesNot()}:
 * on the coat sheet an eye is a block of pure black beside a block of white, and
 * eye colour means colouring the <b>black</b>. The obvious tool
 * ({@code shadeToward}) does the opposite.
 */
class EyeColorTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** The adult right eye: two sclera texels then two iris texels, at y = 42. */
    private static final int EYE_X = 6;
    private static final int EYE_Y = 42;

    private static int[] template() {
        // A white template with the real eye shape stamped in: white sclera at
        // x+0..1, black iris at x+2..3, on both eyes.
        int[] t = new int[N * N];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                t[py * N + px] = 0xFFFFFFFF);
        for (int eyeX : new int[]{6, 28}) {
            for (int y = EYE_Y; y < EYE_Y + 2; y++) {
                t[y * N + eyeX] = 0xFFFFFFFF;
                t[y * N + eyeX + 1] = 0xFFFFFFFF;
                t[y * N + eyeX + 2] = 0xFF000000;
                t[y * N + eyeX + 3] = 0xFF000000;
            }
        }
        return t;
    }

    private static GradientLut greyLut() {
        int[] lut = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int shade = 255 - Math.round(y / 15f * 255);
                lut[y * 16 + x] = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
            }
        }
        return new GradientLut(lut, 16, 16);
    }

    /** {@code {sclera, iris}} RGB of the adult right eye on a composed coat. */
    private static int[] eye(String code) {
        int[] img = CoatTextureComposer.compose(Genotype.parse(code), Epigenome.fromSeed(7),
                Skin.ADULT, true, template(), greyLut());
        return new int[]{
                img[EYE_Y * N + EYE_X] & 0xFFFFFF,
                img[EYE_Y * N + EYE_X + 2] & 0xFFFFFF
        };
    }

    private static final String BAY = Codes.of("extension", "E/E", "agouti", "A/a");

    private static String with(String gene, String tokens) {
        return BAY + "-horsegenetics." + gene + "=" + tokens;
    }

    // ------------------------------------------------------------------

    /**
     * <b>The iris is the dark texels.</b> A tiger-eye horse's black iris becomes
     * amber and its white sclera stays white - which is the whole specification
     * of eye colour, and the opposite of what a luma-weighted shade would do.
     */
    @Test
    void theIrisTakesTheColourAndTheScleraDoesNot() {
        int[] plain = eye(BAY);
        assertEquals(0xFFFFFF, plain[0], "sclera starts white");
        assertEquals(0x000000, plain[1], "iris starts black");

        int[] amber = eye(with("tiger_eye", "TE1/TE1"));
        assertEquals(0xFFFFFF, amber[0], "the sclera must stay white");
        assertEquals(TigerEyeGene.AMBER, amber[1], "the iris must take the amber");
    }

    @Test
    void theTwoTigerEyeShadesDiffer() {
        assertEquals(TigerEyeGene.AMBER, eye(with("tiger_eye", "TE1/TE1"))[1]);
        assertEquals(TigerEyeGene.AMBER, eye(with("tiger_eye", "TE1/TE2"))[1]);
        assertEquals(TigerEyeGene.YELLOW, eye(with("tiger_eye", "TE2/TE2"))[1]);
        assertNotEquals(TigerEyeGene.AMBER, TigerEyeGene.YELLOW);
    }

    /** Recessive: one wild-type copy and the horse is indistinguishable from a plain one. */
    @Test
    void aTigerEyeCarrierShowsNothing() {
        assertEquals(0x000000, eye(with("tiger_eye", "TE1/N"))[1]);
        assertEquals(0x000000, eye(with("tiger_eye", "TE2/N"))[1]);
        assertTrue(Genes.TIGER_EYE.expressionOf(
                Genotype.parse(with("tiger_eye", "TE1/N")).pair(Genes.TIGER_EYE)).wildType());
    }

    // ------------------------------------------------------------------
    // Blue, from the white loci
    // ------------------------------------------------------------------

    /** Splash is the blue-eyed pattern, diagnostic even when the white is modest. */
    @Test
    void aSplashHorseHasBlueEyes() {
        assertEquals(EyeColor.BLUE.rgb(), eye(with("mitf", "SW1/N"))[1]);
        assertEquals(EyeColor.BLUE.rgb(), eye(with("pax3", "SW2/N"))[1]);
    }

    /** Frame overos are blue-eyed too. */
    @Test
    void aFrameHorseHasBlueEyes() {
        assertEquals(EyeColor.BLUE.rgb(), eye(with("ednrb", "O/N"))[1]);
    }

    /**
     * {@code KIT} is the locus that has to draw a line: an ordinary sabino has
     * dark eyes, and only the broad-white end of the ladder goes blue.
     */
    @Test
    void kitOnlyGivesBlueEyesFromBroadWhiteUpward() {
        assertEquals(0x000000, eye(with("kit", "SB1/N"))[1], "a plain sabino has dark eyes");
        assertEquals(0x000000, eye(with("kit", "W20/N"))[1]);
        assertEquals(EyeColor.BLUE.rgb(), eye(with("kit", "W5/N"))[1], "broad white does");
        assertEquals(EyeColor.BLUE.rgb(), eye(with("kit", "W22/N"))[1], "dominant white certainly does");
    }

    /**
     * <b>Blue beats amber</b>, and it has to: a depigmented iris has no pigment
     * left for tiger eye to recolour. The ranking, exercised through the whole
     * pipeline rather than asserted on the constants.
     */
    @Test
    void blueBeatsAmberOnAHorseThatIsBoth() {
        assertEquals(EyeColor.BLUE.rgb(), eye(with("mitf", "SW1/N") + "-horsegenetics.tiger_eye=TE1/TE1")[1]);
        assertTrue(EyeColor.pigment("x", "x", 0).losesTo(EyeColor.BLUE));
        assertFalse(EyeColor.BLUE.losesTo(EyeColor.pigment("x", "x", 0)));
    }

    // ------------------------------------------------------------------

    /**
     * Tiger eye changes the baked texture, so its outcomes must <b>not</b> be
     * wild types - otherwise it would drop out of the coat code and two horses
     * with different eyes would share one cached texture. It is deterministic,
     * so it costs the cache one entry per outcome and not one per horse.
     */
    @Test
    void tigerEyeIsInTheTextureKeyAndIsDeterministic() {
        assertTrue(Genes.TIGER_EYE.affectsCoat(),
                "the eyes are drawn into the coat texture, so the locus belongs in its key");
        Genotype amber = Genotype.parse(with("tiger_eye", "TE1/TE1"));
        Genotype plain = Genotype.parse(BAY);
        assertNotEquals(plain.coatCode(), amber.coatCode());
        // Every amber eye is the same amber, so the locus stays out of the
        // per-horse fingerprint - one cache entry per outcome, not per horse.
        // (Asserted on the gene's own outcome: the surrounding bay is
        // non-deterministic for its own reasons.)
        assertTrue(Genes.TIGER_EYE.expressionOf(amber.pair(Genes.TIGER_EYE)).deterministic());
    }
}
