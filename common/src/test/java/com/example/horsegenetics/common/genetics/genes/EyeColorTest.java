package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyePatch;
import com.example.horsegenetics.common.genetics.EyeSpread;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The eye-colour channel</b>, measured through the real pipeline - the
 * dilutions, tiger eye, the blue the white loci give, the two kinds of
 * heterochromia, and the rules that order them.
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

    private static int[] compose(String code, long seed) {
        return CoatTextureComposer.compose(Genotype.parse(code), Epigenome.fromSeed(seed),
                Skin.ADULT, true, template(), greyLut());
    }

    /** {@code {sclera, iris}} RGB of the adult right eye on a composed coat. */
    private static int[] eye(String code) {
        return eye(code, 7L);
    }

    private static int[] eye(String code, long seed) {
        int[] img = compose(code, seed);
        return new int[]{
                img[EYE_Y * N + EYE_X] & 0xFFFFFF,
                img[EYE_Y * N + EYE_X + 2] & 0xFFFFFF
        };
    }

    /**
     * The four iris texels of one eye, in {@link EyePatch} quadrant order
     * (top-left, top-right, bottom-left, bottom-right). The iris of the right
     * eye sits at x 8-9, the left eye's at x 30-31 - see {@link #template()}.
     */
    private static int[] iris(int[] img, int eye) {
        int x0 = eye == 0 ? 8 : 30;
        return new int[]{
                img[EYE_Y * N + x0] & 0xFFFFFF,
                img[EYE_Y * N + x0 + 1] & 0xFFFFFF,
                img[(EYE_Y + 1) * N + x0] & 0xFFFFFF,
                img[(EYE_Y + 1) * N + x0 + 1] & 0xFFFFFF,
        };
    }

    /** Both irises entirely one colour. */
    private static boolean bothEyesAre(int[] img, int rgb) {
        return colours(iris(img, 0)).equals(Set.of(rgb)) && colours(iris(img, 1)).equals(Set.of(rgb));
    }

    private static Set<Integer> colours(int[] quadrants) {
        Set<Integer> s = new LinkedHashSet<>();
        for (int q : quadrants) {
            s.add(q);
        }
        return s;
    }

    /**
     * The first seed at which this horse's eyes satisfy {@code wanted}. Written
     * as a search rather than a magic number so that retuning a probability
     * moves the seed and not the test: what is being asserted is that the
     * outcome is <b>reachable</b> and looks right, never that seed 7 in
     * particular produces it.
     */
    private static long seedWhere(String code, Predicate<int[]> wanted) {
        for (long seed = 0; seed < 4000; seed++) {
            if (wanted.test(compose(code, seed))) {
                return seed;
            }
        }
        throw new AssertionError("no seed in 4000 gave the eyes this test is about: " + code);
    }

    private static final String BAY = Codes.of("extension", "E/E", "agouti", "A/a");

    private static String with(String gene, String tokens) {
        return BAY + "-horsegenetics." + gene + "=" + tokens;
    }

    private static String with(String gene, String tokens, String gene2, String tokens2) {
        return with(gene, tokens) + "-horsegenetics." + gene2 + "=" + tokens2;
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
    // The dilutions: green and hazel
    // ------------------------------------------------------------------

    /**
     * <b>The cream / pearl locus reads the same six rows for the eye as for the
     * coat.</b> The double cream's pale blue is the famous one; the compound
     * heterozygote is the odd one out and the mod's ordinary source of a green
     * eye.
     */
    @Test
    void theCreamPearlLocusColoursTheIrisByDose() {
        assertEquals(0x000000, eye(with("matp", "Cr/N"))[1], "a single cream does not touch the eye");
        assertEquals(0x000000, eye(with("matp", "prl/N"))[1], "nor does a pearl carrier");
        assertEquals(MatpGene.DOUBLE_CREAM_BLUE, eye(with("matp", "Cr/Cr"))[1]);
        assertEquals(MatpGene.CREAM_PEARL_GREEN, eye(with("matp", "Cr/prl"))[1]);
        assertEquals(MatpGene.PEARL_LIGHT, eye(with("matp", "prl/prl"))[1]);
    }

    /**
     * <b>Champagne is where a hazel eye comes from</b>, and - rarely - a green
     * one. The shade is epigenetic, so what is asserted is that all four shades
     * are reachable and that a given horse always regenerates its own.
     */
    @Test
    void champagneRollsItsIrisFromAmberThroughHazelToOlive() {
        String champagne = with("champagne", "Ch/c");
        Set<Integer> seen = new LinkedHashSet<>();
        for (long seed = 0; seed < 400; seed++) {
            seen.add(eye(champagne, seed)[1]);
        }
        assertTrue(seen.contains(ChampagneGene.AMBER), "amber is the commonest");
        assertTrue(seen.contains(ChampagneGene.HAZEL), "hazel");
        assertTrue(seen.contains(ChampagneGene.LIGHT_BROWN), "light brown");
        assertTrue(seen.contains(ChampagneGene.OLIVE), "and olive green, the rare one");
        assertEquals(4, seen.size(), "and nothing else");

        assertEquals(eye(champagne, 11L)[1], eye(champagne, 11L)[1],
                "the same horse regenerates the same eye");
    }

    /**
     * The eye is baked into the coat texture, so a locus whose iris varies has
     * to say so or two visibly different horses share one cached texture.
     */
    @Test
    void champagneIsNoLongerDeterministicBecauseItsIrisIsNot() {
        Genotype g = Genotype.parse(with("champagne", "Ch/c"));
        assertFalse(Genes.CHAMPAGNE.isDeterministic(g.pair(Genes.CHAMPAGNE), g));
    }

    // ------------------------------------------------------------------
    // Blue, from the white loci
    // ------------------------------------------------------------------

    /** Splash is the blue-eyed pattern, diagnostic even when the white is modest. */
    @Test
    void aSplashHorseHasBlueEyes() {
        for (String code : new String[]{with("mitf", "SW1/N"), with("pax3", "SW2/N")}) {
            long seed = seedWhere(code, img -> bothEyesAre(img, EyeColor.BLUE.rgb()));
            int[] img = compose(code, seed);
            assertEquals(Set.of(EyeColor.BLUE.rgb()), colours(iris(img, 0)));
            assertEquals(Set.of(EyeColor.BLUE.rgb()), colours(iris(img, 1)),
                    "two whole blue eyes is by far the commonest outcome");
        }
    }

    /** Frame overos are blue-eyed too. */
    @Test
    void aFrameHorseHasBlueEyes() {
        String code = with("ednrb", "O/N");
        long seed = seedWhere(code, img -> bothEyesAre(img, EyeColor.BLUE.rgb()));
        assertTrue(bothEyesAre(compose(code, seed), EyeColor.BLUE.rgb()));
    }

    /**
     * {@code KIT} is the locus that has to draw a line: an ordinary sabino has
     * dark eyes, and only the broad-white end of the ladder goes blue.
     */
    @Test
    void kitOnlyGivesBlueEyesFromBroadWhiteUpward() {
        for (long seed = 0; seed < 60; seed++) {
            assertEquals(0x000000, eye(with("kit", "SB1/N"), seed)[1], "a plain sabino has dark eyes");
            assertEquals(0x000000, eye(with("kit", "W20/N"), seed)[1]);
        }
        for (String code : new String[]{with("kit", "W5/N"), with("kit", "W22/N")}) {
            long seed = seedWhere(code, img -> colours(iris(img, 0)).contains(EyeColor.BLUE.rgb()));
            assertTrue(colours(iris(compose(code, seed), 0)).contains(EyeColor.BLUE.rgb()),
                    "broad white and up do go blue");
        }
    }

    /**
     * <b>Blue beats amber</b>, and it has to: a depigmented iris has no pigment
     * left for tiger eye to recolour. The ranking, exercised through the whole
     * pipeline rather than asserted on the constants.
     */
    @Test
    void blueBeatsAmberOnAHorseThatIsBoth() {
        String code = with("mitf", "SW1/N", "tiger_eye", "TE1/TE1");
        long seed = seedWhere(code, img -> bothEyesAre(img, EyeColor.BLUE.rgb()));
        assertTrue(bothEyesAre(compose(code, seed), EyeColor.BLUE.rgb()));
        assertTrue(EyeColor.pigment("x", "x", 0).losesTo(EyeColor.BLUE));
        assertFalse(EyeColor.BLUE.losesTo(EyeColor.pigment("x", "x", 0)));
    }

    /**
     * <b>The three ranks are the three biological routes</b>, and they nest: a
     * whole-body dilution loses to a gene aimed at the iris, which loses to an
     * iris that never got pigment cells at all.
     */
    @Test
    void anIrisSpecificGeneBeatsAWholeBodyDilution() {
        assertTrue(EyeColor.RANK_DILUTION < EyeColor.RANK_PIGMENT);
        assertTrue(EyeColor.RANK_PIGMENT < EyeColor.RANK_DEPIGMENTED);
        // A cremello that also carries tiger eye: the eye is amber, not cream blue.
        assertEquals(TigerEyeGene.AMBER, eye(with("matp", "Cr/Cr", "tiger_eye", "TE1/TE1"))[1]);
    }

    // ------------------------------------------------------------------
    // Natural heterochromia - how far the depigmentation reached
    // ------------------------------------------------------------------

    /**
     * <b>One blue eye.</b> The classic splash-carrier tell, and it is not a
     * separate trait: it is the same failure of melanocyte colonisation as two
     * blue eyes, stopped one iris earlier. The other eye keeps whatever colour
     * the horse's pigment genes gave it - here tiger eye's amber, which proves
     * the pigment layer is still underneath rather than painted over.
     */
    @Test
    void completeHeterochromiaLeavesTheOtherEyeItsOwnColour() {
        String code = with("mitf", "SW1/N", "tiger_eye", "TE1/TE1");
        long seed = seedWhere(code, img ->
                colours(iris(img, 0)).equals(Set.of(EyeColor.BLUE.rgb()))
                        && colours(iris(img, 1)).equals(Set.of(TigerEyeGene.AMBER)));
        int[] img = compose(code, seed);
        assertEquals(Set.of(EyeColor.BLUE.rgb()), colours(iris(img, 0)), "one blue eye");
        assertEquals(Set.of(TigerEyeGene.AMBER), colours(iris(img, 1)), "and one amber one");
    }

    /**
     * <b>A wedge of blue in an otherwise coloured iris.</b> Same mechanism
     * again, stopped part-way across one eye - so the iris comes out two
     * colours, not one.
     */
    @Test
    void sectoralHeterochromiaSplitsOneIrisBetweenTwoColours() {
        String code = with("mitf", "SW1/N", "tiger_eye", "TE1/TE1");
        long seed = seedWhere(code, img ->
                colours(iris(img, 0)).equals(Set.of(EyeColor.BLUE.rgb(), TigerEyeGene.AMBER)));
        int[] wedged = iris(compose(code, seed), 0);
        assertEquals(Set.of(EyeColor.BLUE.rgb(), TigerEyeGene.AMBER), colours(wedged),
                "part of the iris blue, the rest the horse's own amber");
    }

    /** The roll itself: every outcome is reachable and two blue eyes is much the commonest. */
    @Test
    void theSpreadRollIsMostlyTwoWholeBlueEyes() {
        int both = 0;
        int oneEye = 0;
        int sectoral = 0;
        // One generator, many rolls - deliberately NOT one fresh SeededRng per
        // sequential seed. java.util.Random's *first* nextFloat() is strongly
        // correlated with its seed (seeds 0-3999 span only 0.22 to 0.59), which
        // is harmless in play - a real epigenetic seed is a full random long -
        // and lethal to a distribution test. See wiki/known-gaps.html.
        Rng rng = new SeededRng(20260906L, "spread");
        for (int i = 0; i < 4000; i++) {
            EyeSpread s = EyeSpread.roll(rng);
            assertFalse(s.empty(), "a blue-eyed horse always has some blue somewhere");
            if (s.equals(EyeSpread.BOTH)) {
                both++;
            } else if (s.sectoral()) {
                sectoral++;
            } else {
                oneEye++;
            }
        }
        assertTrue(both > oneEye + sectoral, "two blue eyes is much the commonest");
        assertTrue(oneEye > 0, "one blue eye happens");
        assertTrue(sectoral > 0, "and a wedge happens");
        assertTrue(sectoral < oneEye, "a wedge is the rarest of the three");
    }

    // ------------------------------------------------------------------
    // Magic sectoral heterochromia
    // ------------------------------------------------------------------

    private static String sectoral(String tokens) {
        return BAY + "-horsegenetics.magic_sectoral_heterochromia=" + tokens;
    }

    /** Two different colour alleles: both colours show, in both eyes. */
    @Test
    void twoDifferentAllelesPutBothColoursInBothEyes() {
        int[] img = compose(sectoral("green/gold"), 3L);
        Set<Integer> expected = Set.of(
                MagicSectoralHeterochromiaGene.GREEN_RGB, MagicSectoralHeterochromiaGene.GOLD_RGB);
        assertEquals(expected, colours(iris(img, 0)), "the right eye carries both colours");
        assertEquals(expected, colours(iris(img, 1)), "and so does the left");
    }

    /**
     * <b>A different shape in each eye</b>, guaranteed rather than merely
     * likely - at twelve wedges a matched pair would otherwise turn up on one
     * horse in twelve and read as a bug.
     */
    @Test
    void theTwoEyesNeverGetTheSameShape() {
        int green = MagicSectoralHeterochromiaGene.GREEN_RGB;
        for (long seed = 0; seed < 300; seed++) {
            int[] img = compose(sectoral("green/gold"), seed);
            int[] right = iris(img, 0);
            int[] left = iris(img, 1);
            int rightMask = 0;
            int leftMask = 0;
            for (int i = 0; i < 4; i++) {
                rightMask |= (right[i] == green ? 1 : 0) << i;
                // the two colours swap which of them leads on the second eye
                leftMask |= (left[i] == green ? 0 : 1) << i;
            }
            assertNotEquals(rightMask, leftMask, "seed " + seed + " gave both eyes one shape");
            assertNotEquals(0, rightMask, "a wedge is never the whole iris");
            assertNotEquals(EyePatch.WHOLE, rightMask, "nor is it empty");
        }
    }

    /** Homozygous, and anything carrying the wild type, shows nothing at all. */
    @Test
    void matchedPairsAndCarriersShowNothing() {
        for (String tokens : new String[]{"green/green", "chaos/chaos", "gold/n", "chaos/n", "n/n"}) {
            assertEquals(0x000000, eye(sectoral(tokens))[1], tokens + " must not touch the iris");
        }
    }

    /**
     * And it must not <i>overwrite</i> an eye colour either - a champagne horse
     * carrying a matched pair keeps its champagne eye.
     */
    @Test
    void aMatchedPairDoesNotOverwriteTheHorsesOwnEyeColour() {
        String code = with("matp", "Cr/Cr") + "-horsegenetics.magic_sectoral_heterochromia=gold/gold";
        assertEquals(MatpGene.DOUBLE_CREAM_BLUE, eye(code)[1]);
    }

    /** Chaos takes its colour from its own copy's epigenetics, so horses disagree. */
    @Test
    void chaosDrawsItsColourFromItsOwnAlleleCopy() {
        Set<Integer> seen = new LinkedHashSet<>();
        for (long seed = 0; seed < 60; seed++) {
            for (int q : iris(compose(sectoral("blue/chaos"), seed), 0)) {
                if (q != MagicSectoralHeterochromiaGene.BLUE_RGB) {
                    seen.add(q);
                }
            }
        }
        assertTrue(seen.size() > 20, "chaos should almost never repeat itself, saw " + seen.size());
        assertEquals(iris(compose(sectoral("blue/chaos"), 5L), 0)[0],
                iris(compose(sectoral("blue/chaos"), 5L), 0)[0],
                "but the same horse always regenerates the same chaos colour");
    }

    /**
     * It paints over the eye-colour channel rather than competing with it, so a
     * splashed white horse carrying two colours shows the two colours.
     */
    @Test
    void theMagicLocusPaintsOverEvenABlueEye() {
        String code = with("mitf", "SW1/N") + "-horsegenetics.magic_sectoral_heterochromia=green/gold";
        assertEquals(Set.of(MagicSectoralHeterochromiaGene.GREEN_RGB,
                        MagicSectoralHeterochromiaGene.GOLD_RGB),
                colours(iris(compose(code, 3L), 0)));
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

    /**
     * <b>Fifteen outcomes, not one.</b> The wiki's preview widget, the in-game
     * gene dictionary and the genotype catalogue all enumerate a gene by its
     * <i>distinct expressions</i>, so declaring one
     * {@code sectoral-heterochromia} for every expressing pair would collapse
     * fifteen separately breedable results into one button. This is the
     * assertion that keeps that from being quietly undone.
     */
    @Test
    void everyPairOfColoursIsItsOwnOutcome() {
        int colours = Genes.SECTORAL_EYES.alleles().size() - 1; // all but the wild type
        int expected = colours * (colours - 1) / 2;
        Set<String> ids = new LinkedHashSet<>();
        int expressing = 0;
        for (AllelePair pair : GenotypeCatalog.allPairsOf(Genes.SECTORAL_EYES)) {
            Expression e = Genes.SECTORAL_EYES.expressionOf(pair);
            if (e.wildType()) {
                continue;
            }
            expressing++;
            assertTrue(ids.add(e.id()), e.id() + " is claimed by two different pairs");
        }
        assertEquals(expected, expressing, "one expressing combination per pair of colours");
        assertEquals(expected, GenotypeCatalog.distinctPairsOf(Genes.SECTORAL_EYES).size() - 1,
                "and the catalogue keeps them apart (the -1 is the wild-type group)");
    }

    /**
     * The magic locus is the other way round: it varies per horse, so it must
     * declare that, and its expressing outcome must not be a wild type.
     */
    @Test
    void theMagicLocusIsInTheTextureKeyAndVaries() {
        assertTrue(Genes.SECTORAL_EYES.affectsCoat());
        Genotype g = Genotype.parse(sectoral("green/gold"));
        assertFalse(Genes.SECTORAL_EYES.isDeterministic(g.pair(Genes.SECTORAL_EYES), g));
        assertNotEquals(Genotype.parse(BAY).coatCode(), g.coatCode());
    }
}
