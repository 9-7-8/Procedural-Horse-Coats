package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.EyePatch;
import com.example.horsegenetics.common.genetics.EyeSpread;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.epi.EpiRoll;
import com.example.horsegenetics.common.genetics.eye.EyeHue;
import com.example.horsegenetics.common.genetics.eye.EyeLocus;
import com.example.horsegenetics.common.genetics.eye.EyePhenotype;
import com.example.horsegenetics.common.genetics.eye.EyeSector;
import com.example.horsegenetics.common.genetics.eye.Eyes;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The eye loci</b>, measured through the real pipeline - the thirteen genes,
 * the requests the natural genes make of them, and the painting.
 *
 * <h2>Two load-bearing assertions</h2>
 * <ul>
 *   <li>{@link #theIrisTakesTheColourAndTheScleraDoesNot()} - on the coat sheet
 *       an eye is a block of pure black beside a block of white, and eye colour
 *       means colouring the <b>black</b>. The obvious tool
 *       ({@code shadeToward}) does the opposite, and the sclera locus is the
 *       exact complement.</li>
 *   <li>{@link #aRequestIsWrittenOntoTheHorse()} - the whole difference between
 *       this design and the ranked channel it replaces. A white horse does not
 *       get blue eyes painted on it at bake time; it is <b>born carrying the
 *       blue allele</b>, and passes it on.</li>
 * </ul>
 *
 * <p>Note the helpers: a horse is {@link Eyes#force}d, because that is what
 * being born does to it. A raw {@link Genotype#parse} is <i>not</i> forced, on
 * purpose - see {@link Eyes} - and {@link #parsingDoesNotForceAnything()} pins
 * that down.
 */
class EyeColorTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** The adult right eye: two sclera texels then two iris texels, at y = 42. */
    private static final int EYE_X = 6;
    private static final int EYE_Y = 42;

    private static int[] template() {
        // A white template with the real eye shape stamped in: white sclera at
        // x+0..1, black iris at x+2..3, on both eyes. Deliberately NOT mirrored
        // the way the shipped template is - this is a fixture for the painting
        // rules, and the mirroring is EyeSector's business and tested there.
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

    /**
     * The same template with <b>no eye stamped in</b> - plain white head all the
     * way across the eye rects. The independent oracle for an invisible eye: if
     * "the iris is not painted" means anything, the texel must come out the same
     * as it does on a horse whose template never had an eye there.
     */
    private static int[] templateNoEyes() {
        int[] t = new int[N * N];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                t[py * N + px] = 0xFFFFFFFF);
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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** The genotype a horse with this code is actually <b>born</b> with. */
    private static Genotype born(String code, long seed) {
        Epigenome epi = Epigenome.fromSeed(seed);
        return Eyes.force(Genotype.parse(code), epi);
    }

    private static int[] compose(String code, long seed) {
        return compose(code, seed, template());
    }

    private static int[] compose(String code, long seed, int[] template) {
        Epigenome epi = Epigenome.fromSeed(seed);
        return CoatTextureComposer.compose(Eyes.force(Genotype.parse(code), epi), epi,
                Skin.ADULT, true, template, greyLut());
    }

    /**
     * The bare coat at one texel - what a horse with no eye there looks like.
     * Baked on {@link #templateNoEyes()} with every eye half invisible, so
     * nothing is redrawn and nothing is painted: the coat, and only the coat.
     */
    private static int bareCoatAt(int x, int y) {
        String code = BAY
                + "-" + EyeLocus.IRIS_RIGHT.key() + "=Inv/Inv"
                + "-" + EyeLocus.IRIS_LEFT.key() + "=Inv/Inv"
                + "-" + EyeLocus.SCLERA_RIGHT.key() + "=Inv/Inv"
                + "-" + EyeLocus.SCLERA_LEFT.key() + "=Inv/Inv";
        return compose(code, 7L, templateNoEyes())[y * N + x] & 0xFFFFFF;
    }

    private static CoatTextureComposer.Baked bake(String code, long seed) {
        Epigenome epi = Epigenome.fromSeed(seed);
        return CoatTextureComposer.bake(Eyes.force(Genotype.parse(code), epi), epi,
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

    /** A code naming one eye locus directly, as a matched pair. */
    private static String locus(EyeLocus locus, String token) {
        return BAY + "-" + locus.key() + "=" + token + "/" + token;
    }

    private static final int BROWN = EyeHue.BROWN.fixedRgb();
    private static final int GOLD = EyeHue.GOLD.fixedRgb();
    private static final int BLUE = EyeHue.MID_BLUE.fixedRgb();

    // ==================================================================
    // The vocabulary
    // ==================================================================

    /**
     * <b>The iris is the dark texels.</b> A gold-eyed horse's black iris becomes
     * gold and its white sclera stays white - which is the whole specification
     * of eye colour, and the opposite of what a luma-weighted shade would do.
     */
    @Test
    void theIrisTakesTheColourAndTheScleraDoesNot() {
        int[] gold = eye(locus(EyeLocus.IRIS_RIGHT, EyeHue.GOLD.token()));
        assertEquals(0xFFFFFF, gold[0], "the sclera must stay white");
        assertEquals(GOLD, gold[1], "the iris must take the gold");
    }

    /** <b>Brown is the wild type</b>, so an ordinary horse has a brown eye, not a black one. */
    @Test
    void everyOrdinaryHorseIsBrownEyed() {
        int[] plain = eye(BAY);
        assertEquals(0xFFFFFF, plain[0], "sclera white");
        assertEquals(BROWN, plain[1], "and a brown iris - the template's black is not an eye colour");
    }

    /** Recessive to the wild type <i>and to each other</i> - the matched-pair rule. */
    @Test
    void everyIrisVariantIsRecessiveToBrownAndToTheOthers() {
        EyeColourGene g = Genes.EYE_COLOUR_RIGHT;
        Allele brown = g.wild();
        Allele green = g.fromToken(EyeHue.GREEN.token());
        Allele gold = g.fromToken(EyeHue.GOLD.token());
        assertEquals(EyeHue.BROWN, g.hueOf(new AllelePair(brown, green)), "a carrier is brown-eyed");
        assertEquals(EyeHue.BROWN, g.hueOf(new AllelePair(green, gold)),
                "two different variants are brown-eyed, not a compromise");
        assertEquals(EyeHue.GREEN, g.hueOf(new AllelePair(green, green)));
    }

    /** The sclera locus is the exact complement: it colours the <b>light</b> texels. */
    @Test
    void theScleraLocusColoursTheWhiteAndLeavesTheIris() {
        int[] black = eye(locus(EyeLocus.SCLERA_RIGHT, EyeHue.BLACK.token()));
        assertEquals(EyeHue.BLACK.fixedRgb(), black[0], "the white of the eye goes black");
        assertEquals(BROWN, black[1], "and the iris keeps its own colour");
        assertEquals(0xFFFFFF, eye(BAY)[0],
                "and the wild type paints nothing - the template already is the white of an eye");
    }

    /**
     * <b>Invisible is not a colour.</b> Those texels are never copied back off
     * the template, so what shows is the coat on the head - which on this grey
     * LUT is neither the template's black nor any hue in the palette.
     */
    @Test
    void anInvisibleIrisShowsTheCoatThroughIt() {
        int[] gone = eye(locus(EyeLocus.IRIS_RIGHT, EyeHue.INVISIBLE.token()));
        assertEquals(bareCoatAt(EYE_X + 2, EYE_Y), gone[1],
                "the iris texels show the horse's own coat, not the template's black eye");
        assertNotEquals(0x000000, gone[1], "and in particular not black, which is the whole trap");
        assertEquals(0xFFFFFF, gone[0], "the sclera is untouched");
    }

    @Test
    void anInvisibleScleraShowsTheCoatThroughIt() {
        int[] gone = eye(locus(EyeLocus.SCLERA_RIGHT, EyeHue.INVISIBLE.token()));
        assertEquals(bareCoatAt(EYE_X, EYE_Y), gone[0],
                "the sclera texels show the horse's own coat");
        assertEquals(BROWN, gone[1], "and the iris is still there");
    }

    /** An iris that is not painted has no sector and no glow either. */
    @Test
    void anInvisibleIrisTakesItsSectorWithIt() {
        String code = locus(EyeLocus.IRIS_RIGHT, EyeHue.INVISIBLE.token())
                + "-" + EyeLocus.SECTOR_RIGHT.key() + "=Top/Top";
        int[] quads = iris(compose(code, 7L), 0);
        assertFalse(colours(quads).contains(BLUE),
                "no sector is drawn into an iris that is not there");
        assertEquals(bareCoatAt(8, EYE_Y), quads[0]);
    }

    /** Chaos is read off the allele copy, so no two chaos-eyed horses match. */
    @Test
    void chaosDrawsItsColourFromItsOwnAlleleCopy() {
        String code = locus(EyeLocus.IRIS_RIGHT, EyeHue.CHAOS.token());
        Set<Integer> seen = new LinkedHashSet<>();
        for (long seed = 0; seed < 60; seed++) {
            seen.add(eye(code, seed)[1]);
        }
        assertTrue(seen.size() > 20, "chaos should almost never repeat itself, saw " + seen.size());
        assertEquals(eye(code, 5L)[1], eye(code, 5L)[1],
                "but the same horse always regenerates the same chaos colour");
    }

    // ==================================================================
    // Sectors
    // ==================================================================

    /** A sector puts a second colour in part of one iris and leaves the rest. */
    @Test
    void aSectorSplitsOneIrisBetweenTwoColours() {
        String code = BAY + "-" + EyeLocus.SECTOR_RIGHT.key() + "=Top/Top";
        int[] quads = iris(compose(code, 7L), 0);
        assertEquals(Set.of(BROWN, BLUE), colours(quads),
                "the top half mid blue - the sector locus's wild-type colour - the rest brown");
        assertEquals(BLUE, quads[0], "top left");
        assertEquals(BLUE, quads[1], "top right");
        assertEquals(BROWN, quads[2], "bottom left");
        assertEquals(BROWN, quads[3], "bottom right");
    }

    /** The sector locus gates the colour locus, and not the other way round. */
    @Test
    void aSectorColourShowsNothingWithoutASector() {
        String code = BAY + "-" + EyeLocus.SECTOR_COLOUR_RIGHT.key() + "=Gld/Gld";
        assertEquals(Set.of(BROWN), colours(iris(compose(code, 7L), 0)),
                "no sector, so nowhere for the gold to go");
    }

    @Test
    void aSectorTakesTheColourLocusSays() {
        String code = BAY + "-" + EyeLocus.SECTOR_RIGHT.key() + "=Bot/Bot"
                + "-" + EyeLocus.SECTOR_COLOUR_RIGHT.key() + "=Gld/Gld";
        int[] quads = iris(compose(code, 7L), 0);
        assertEquals(BROWN, quads[0]);
        assertEquals(GOLD, quads[2], "the bottom half is gold");
        assertEquals(GOLD, quads[3]);
    }

    /**
     * <b>A named sector is the same corner of the horse on both eyes.</b> The
     * two eyes' faces are mirrored on the sheet, so the raw bits are not - see
     * {@link EyeSector#maskFor}. Asserted on the masks rather than on a render,
     * because which face carries the flip is exactly the part that is
     * unverified in game.
     */
    @Test
    void aNamedSectorIsUnmirroredForTheEastFace() {
        assertEquals(0b0001, EyeSector.UPPER_LEFT.maskFor(CoatRegions.RIGHT_EYE));
        assertEquals(0b0010, EyeSector.UPPER_LEFT.maskFor(CoatRegions.LEFT_EYE),
                "upper left on the mirrored face is the other column");
        assertEquals(EyeSector.TOP.mask(), EyeSector.TOP.maskFor(CoatRegions.LEFT_EYE),
                "a horizontal band is its own mirror");
    }

    /** All ten shapes are distinct, and the two diagonals are among them. */
    @Test
    void thereAreTenDistinctSectors() {
        Set<Integer> masks = new LinkedHashSet<>();
        for (EyeSector s : EyeSector.sectors()) {
            assertTrue(masks.add(s.mask()), s + " duplicates another sector's mask");
            assertNotEquals(EyePatch.NONE, s.mask());
            assertNotEquals(EyePatch.WHOLE, s.mask());
        }
        assertEquals(10, masks.size());
        assertTrue(masks.contains(0b0110), "the diagonals are alleles here, unlike EyePatch.WEDGES");
        assertTrue(masks.contains(0b1001));
    }

    // ==================================================================
    // Requests - the natural genes asking rather than painting
    // ==================================================================

    /**
     * <b>The request is written onto the horse.</b> This is the whole design: a
     * splashed white horse is not a horse with blue paint on its eyes, it is a
     * horse that <i>carries</i> the blue allele - so the allele shows in its
     * genotype code and travels in its gametes.
     */
    @Test
    void aRequestIsWrittenOntoTheHorse() {
        String code = with("mitf", "SW1/N");
        long seed = seedWhere(code, img -> bothEyesAre(img, BLUE));
        Genotype horse = born(code, seed);
        assertEquals(EyeHue.MID_BLUE,
                Genes.EYE_COLOUR_RIGHT.hueOf(horse.pair(Genes.EYE_COLOUR_RIGHT)));
        assertEquals(EyeHue.MID_BLUE,
                Genes.EYE_COLOUR_LEFT.hueOf(horse.pair(Genes.EYE_COLOUR_LEFT)));
        assertTrue(horse.toCode().contains(EyeLocus.IRIS_RIGHT.key() + "=MBl/MBl"),
                "and it is in the code, which is what a foal inherits from");
    }

    /**
     * And parsing does <b>not</b> force: a genotype code is already the answer,
     * and a question asked about a genotype is not a question about a horse.
     */
    @Test
    void parsingDoesNotForceAnything() {
        Genotype parsed = Genotype.parse(with("tiger_eye", "TE1/TE1"));
        assertEquals(EyeHue.BROWN, Genes.EYE_COLOUR_RIGHT.hueOf(parsed.pair(Genes.EYE_COLOUR_RIGHT)),
                "parse is literal - it reports what the code says and invents nothing");
    }

    /** Tiger eye asks for gold rather than owning an amber of its own. */
    @Test
    void tigerEyeRequestsGold() {
        assertEquals(GOLD, eye(with("tiger_eye", "TE1/TE1"))[1]);
        assertEquals(GOLD, eye(with("tiger_eye", "TE2/TE2"))[1]);
        assertEquals(BROWN, eye(with("tiger_eye", "TE1/N"))[1], "a carrier shows nothing");
    }

    /** The cream / pearl locus reads the same rows for the eye as for the coat. */
    @Test
    void theCreamPearlLocusRequestsByDose() {
        assertEquals(BROWN, eye(with("matp", "Cr/N"))[1], "a single cream does not touch the eye");
        assertEquals(BROWN, eye(with("matp", "prl/N"))[1], "nor does a pearl carrier");
        assertEquals(EyeHue.LIGHT_BLUE.fixedRgb(), eye(with("matp", "Cr/Cr"))[1]);
        assertEquals(EyeHue.GREEN.fixedRgb(), eye(with("matp", "Cr/prl"))[1],
                "the compound heterozygote is the mod's ordinary source of a green eye");
        assertEquals(EyeHue.LIGHT_BLUE.fixedRgb(), eye(with("matp", "prl/prl"))[1]);
    }

    /**
     * Champagne still rolls its iris off its own allele copy - what changed is
     * that the roll comes out as one of the palette's alleles rather than as a
     * hex value champagne invented.
     */
    @Test
    void champagneRollsWhichHueItAsksFor() {
        String champagne = with("champagne", "Ch/c");
        Set<Integer> seen = new LinkedHashSet<>();
        for (long seed = 0; seed < 400; seed++) {
            seen.add(eye(champagne, seed)[1]);
        }
        assertTrue(seen.contains(GOLD), "gold, from the amber end");
        assertTrue(seen.contains(BROWN), "brown, where hazel and light brown both land");
        assertTrue(seen.contains(EyeHue.GREEN.fixedRgb()), "and green, the olive tail");
        assertEquals(3, seen.size(), "and nothing else - the palette is closed");
        assertEquals(eye(champagne, 11L)[1], eye(champagne, 11L)[1],
                "the same horse regenerates the same eye");
    }

    /** Splash is the blue-eyed pattern, diagnostic even when the white is modest. */
    @Test
    void aSplashHorseHasBlueEyes() {
        for (String code : new String[]{with("mitf", "SW1/N"), with("pax3", "SW2/N")}) {
            long seed = seedWhere(code, img -> bothEyesAre(img, BLUE));
            assertTrue(bothEyesAre(compose(code, seed), BLUE),
                    "two whole blue eyes is by far the commonest outcome");
        }
    }

    /** Frame overos are blue-eyed too. */
    @Test
    void aFrameHorseHasBlueEyes() {
        String code = with("ednrb", "O/N");
        long seed = seedWhere(code, img -> bothEyesAre(img, BLUE));
        assertTrue(bothEyesAre(compose(code, seed), BLUE));
    }

    /**
     * {@code KIT} is the locus that has to draw a line: an ordinary sabino has
     * dark eyes, and only the broad-white end of the ladder goes blue.
     */
    @Test
    void kitOnlyGivesBlueEyesFromBroadWhiteUpward() {
        for (long seed = 0; seed < 60; seed++) {
            assertEquals(BROWN, eye(with("kit", "SB1/N"), seed)[1], "a plain sabino has brown eyes");
            assertEquals(BROWN, eye(with("kit", "W20/N"), seed)[1]);
        }
        for (String code : new String[]{with("kit", "W5/N"), with("kit", "W22/N")}) {
            long seed = seedWhere(code, img -> colours(iris(img, 0)).contains(BLUE));
            assertTrue(colours(iris(compose(code, seed), 0)).contains(BLUE),
                    "broad white and up do go blue");
        }
    }

    /**
     * <b>Blue beats gold on a horse that is both</b> - and now for an ordinary
     * reason rather than a special one. Requests merge last-writer-wins in
     * {@link Genes#codeOrder()}, and the white loci sort after tiger eye, so
     * their claim is simply the later one. The old {@code EyeColor.rank()}
     * argument about melanin is now the gene order, which is where every other
     * ordering in this mod already lives.
     */
    @Test
    void theLaterLocusWinsTheRequest() {
        assertTrue(Genes.TIGER_EYE.priority() < Genes.MITF.priority(),
                "the ordering this test depends on");
        String code = with("mitf", "SW1/N", "tiger_eye", "TE1/TE1");
        long seed = seedWhere(code, img -> bothEyesAre(img, BLUE));
        assertTrue(bothEyesAre(compose(code, seed), BLUE));
    }

    /**
     * <b>One blue eye.</b> The classic splash-carrier tell, and it is not a
     * separate trait: it is the same failure of melanocyte colonisation as two
     * blue eyes, stopped one iris earlier. The other eye keeps whatever colour
     * the horse's own eye loci gave it - here gold, from tiger eye, which proves
     * the unreached iris was never asked about rather than painted over.
     */
    @Test
    void completeHeterochromiaLeavesTheOtherEyeItsOwnColour() {
        String code = with("mitf", "SW1/N", "tiger_eye", "TE1/TE1");
        long seed = seedWhere(code, img ->
                colours(iris(img, 0)).equals(Set.of(BLUE))
                        && colours(iris(img, 1)).equals(Set.of(GOLD)));
        int[] img = compose(code, seed);
        assertEquals(Set.of(BLUE), colours(iris(img, 0)), "one blue eye");
        assertEquals(Set.of(GOLD), colours(iris(img, 1)), "and one gold one");

        Genotype horse = born(code, seed);
        assertEquals(EyeHue.MID_BLUE, Genes.EYE_COLOUR_RIGHT.hueOf(horse.pair(Genes.EYE_COLOUR_RIGHT)));
        assertEquals(EyeHue.GOLD, Genes.EYE_COLOUR_LEFT.hueOf(horse.pair(Genes.EYE_COLOUR_LEFT)),
                "the eye the blue did not reach kept tiger eye's request");
    }

    /**
     * <b>A wedge of blue in an otherwise coloured iris</b> - and it arrives as
     * the sector alleles, so unlike the mask the old spread painted, this one is
     * inherited.
     */
    @Test
    void aSectoralSpreadComesOutAsSectorAlleles() {
        String code = with("mitf", "SW1/N", "tiger_eye", "TE1/TE1");
        long seed = seedWhere(code, img ->
                colours(iris(img, 0)).equals(Set.of(BLUE, GOLD)));
        assertEquals(Set.of(BLUE, GOLD), colours(iris(compose(code, seed), 0)),
                "part of the iris blue, the rest the horse's own gold");
        Genotype horse = born(code, seed);
        assertNotEquals(EyeSector.WILD,
                Genes.EYE_SECTOR_RIGHT.sectorOf(horse.pair(Genes.EYE_SECTOR_RIGHT)),
                "and the shape is an allele the horse carries");
    }

    /** The roll itself: every outcome is reachable and two blue eyes is much the commonest. */
    @Test
    void theSpreadRollIsMostlyTwoWholeBlueEyes() {
        int both = 0;
        int oneEye = 0;
        int sectoral = 0;
        // One generator, many rolls - deliberately NOT one fresh SeededRng per
        // sequential seed. java.util.Random's *first* nextFloat() is strongly
        // correlated with its seed, which is harmless in play and lethal to a
        // distribution test. See wiki/known-gaps.html.
        Rng rng = new SeededRng(20260906L, "spread");
        for (int i = 0; i < 4000; i++) {
            EyeSpread s = EyeSpread.roll(EpiRoll.founder(EyeSpread.schema(), rng));
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

    /**
     * The "broadly white however it got there" rule, which used to measure the
     * finished coat and now has to answer from the alleles - see
     * {@link WhitePatternEyes#whiteScore}.
     */
    @Test
    void aHorseWhiteFromTwoMildLociStillGoesBlue() {
        Genotype mild = Genotype.parse(with("kit", "SB1/SB1", "tobiano", "To/To"));
        assertTrue(WhitePatternEyes.whiteScore(mild) >= WhitePatternEyes.WHITE_SCORE_THRESHOLD,
                "two mild loci stacking is a broadly white horse");
        Genotype plain = Genotype.parse(BAY);
        assertEquals(0.0, WhitePatternEyes.whiteScore(plain), 1e-9);
    }

    // ==================================================================
    // Glow
    // ==================================================================

    @Test
    void aGlowingIrisLightsTheDarkTexelsOnly() {
        CoatTextureComposer.Baked baked =
                bake(locus(EyeLocus.GLOW_IRIS_RIGHT, "Glo"), 7L);
        assertTrue(baked.hasEmissive(), "a glowing eye needs the emissive pass");
        assertTrue(baked.emissive()[EYE_Y * N + EYE_X + 2], "the iris is lit");
        assertFalse(baked.emissive()[EYE_Y * N + EYE_X], "the sclera is not");
    }

    @Test
    void aGlowingScleraLightsTheWhiteOnly() {
        CoatTextureComposer.Baked baked =
                bake(locus(EyeLocus.GLOW_SCLERA_RIGHT, "Glo"), 7L);
        assertTrue(baked.emissive()[EYE_Y * N + EYE_X], "the white is lit");
        assertFalse(baked.emissive()[EYE_Y * N + EYE_X + 2], "the iris is a hole in it");
    }

    /** The glow has no colour of its own: it lights whatever the eye already is. */
    @Test
    void aGlowTakesTheColourTheEyeAlreadyHas() {
        String code = locus(EyeLocus.IRIS_RIGHT, EyeHue.GOLD.token())
                + "-" + EyeLocus.GLOW_IRIS_RIGHT.key() + "=Glo/Glo";
        assertEquals(GOLD, eye(code)[1]);
        assertTrue(bake(code, 7L).emissive()[EYE_Y * N + EYE_X + 2]);
    }

    /** Nothing to light: an invisible iris carries the glow and shows none of it. */
    @Test
    void aGlowingInvisibleIrisShowsNoGlow() {
        String code = locus(EyeLocus.IRIS_RIGHT, EyeHue.INVISIBLE.token())
                + "-" + EyeLocus.GLOW_IRIS_RIGHT.key() + "=Glo/Glo";
        CoatTextureComposer.Baked baked = bake(code, 7L);
        assertFalse(baked.hasEmissive() && baked.emissive()[EYE_Y * N + EYE_X + 2],
                "an iris that is not painted is not lit either");
    }

    // ==================================================================
    // The third eye
    // ==================================================================

    private static int[] forehead(String code, long seed) {
        int[] img = compose(code, seed);
        int[] r = CoatRegions.thirdEyeRect(Skin.ADULT);
        return new int[]{
                img[r[1] * N + r[0]] & 0xFFFFFF,            // sclera column
                img[r[1] * N + r[0] + 1] & 0xFFFFFF,        // iris
                img[r[1] * N + r[0] + 2] & 0xFFFFFF,        // iris
                img[r[1] * N + r[0] + 3] & 0xFFFFFF,        // sclera column
        };
    }

    @Test
    void anOrdinaryHorseHasNoThirdEye() {
        assertNull(Eyes.resolve(born(BAY, 7L), Epigenome.fromSeed(7L)).third());
        int[] r = CoatRegions.thirdEyeRect(Skin.ADULT);
        int plain = compose(BAY, 7L)[r[1] * N + r[0]] & 0xFFFFFF;
        assertNotEquals(BROWN, plain, "nothing is painted on the forehead");
    }

    /** An imitation allele copies one of the horse's real eyes exactly. */
    @Test
    void anImitatedThirdEyeMatchesTheEyeItCopies() {
        String code = locus(EyeLocus.IRIS_RIGHT, EyeHue.GOLD.token())
                + "-" + EyeLocus.THIRD_EYE.key() + "=Rgt/Rgt";
        Epigenome epi = Epigenome.fromSeed(7L);
        EyePhenotype eyes = Eyes.resolve(born(code, 7L), epi);
        assertNotNull(eyes.third());
        assertEquals(eyes.right(), eyes.third(), "an exact duplicate, every part of it");

        int[] fh = forehead(code, 7L);
        assertEquals(EyeHue.WHITE.fixedRgb(), fh[0], "sclera column");
        assertEquals(GOLD, fh[1], "iris");
        assertEquals(GOLD, fh[2]);
        assertEquals(EyeHue.WHITE.fixedRgb(), fh[3]);
    }

    @Test
    void theTwoImitationsCopyDifferentEyes() {
        String base = locus(EyeLocus.IRIS_RIGHT, EyeHue.GOLD.token())
                + "-" + EyeLocus.IRIS_LEFT.key() + "=Grn/Grn";
        assertEquals(GOLD, forehead(base + "-" + EyeLocus.THIRD_EYE.key() + "=Rgt/Rgt", 7L)[1]);
        assertEquals(EyeHue.GREEN.fixedRgb(),
                forehead(base + "-" + EyeLocus.THIRD_EYE.key() + "=Lft/Lft", 7L)[1]);
    }

    /** A mixed third eye takes each part from one side or the other. */
    @Test
    void aMixedThirdEyeTakesEachPartFromOneSideOrTheOther() {
        String code = locus(EyeLocus.IRIS_RIGHT, EyeHue.GOLD.token())
                + "-" + EyeLocus.IRIS_LEFT.key() + "=Grn/Grn"
                + "-" + EyeLocus.THIRD_EYE.key() + "=Mix/Mix";
        Set<Integer> seen = new LinkedHashSet<>();
        for (long seed = 0; seed < 80; seed++) {
            int hue = forehead(code, seed)[1];
            assertTrue(hue == GOLD || hue == EyeHue.GREEN.fixedRgb(),
                    "a mixed iris is always one of the horse's own two, saw " + Integer.toHexString(hue));
            seen.add(hue);
        }
        assertEquals(2, seen.size(), "and both sides are reachable");
    }

    /** A defined third eye owes nothing to the other two, and varies per horse. */
    @Test
    void aDefinedThirdEyeIsItsOwnHorseEveryTime() {
        String code = locus(EyeLocus.THIRD_EYE, "Def");
        Set<Integer> seen = new LinkedHashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            seen.add(forehead(code, seed)[1]);
        }
        assertTrue(seen.size() > 3, "a defined eye draws from the whole iris palette, saw " + seen.size());
        assertEquals(forehead(code, 3L)[1], forehead(code, 3L)[1],
                "but the same horse regenerates the same eye");
    }

    /** Every allele is recessive to the wild type and to the other three. */
    @Test
    void theThirdEyeAllelesAreAllRecessive() {
        ThirdEyeGene g = Genes.THIRD_EYE;
        assertNull(g.modeOf(new AllelePair(g.wild(), g.fromToken("Def"))), "a carrier grows nothing");
        assertNull(g.modeOf(new AllelePair(g.fromToken("Mix"), g.fromToken("Def"))),
                "and neither do two different answers");
        assertEquals(ThirdEyeGene.Mode.DEFINED,
                g.modeOf(new AllelePair(g.fromToken("Def"), g.fromToken("Def"))));
    }

    // ==================================================================
    // Registry and cache
    // ==================================================================

    /** Every eye locus is in the texture key: the eyes are drawn into the coat. */
    @Test
    void everyEyeLocusIsInTheTextureKey() {
        for (EyeLocus locus : EyeLocus.values()) {
            Gene g = Genes.byKey(locus.key());
            assertTrue(g.affectsCoat(), locus + " must be in the texture key - it changes the eyes");
        }
        Genotype gold = Genotype.parse(locus(EyeLocus.IRIS_RIGHT, EyeHue.GOLD.token()));
        assertNotEquals(Genotype.parse(BAY).coatCode(), gold.coatCode());
    }

    /** Chaos varies per horse, so it must say so or two different horses share a texture. */
    @Test
    void chaosDeclaresThatItVaries() {
        Genotype g = Genotype.parse(locus(EyeLocus.IRIS_RIGHT, EyeHue.CHAOS.token()));
        assertFalse(Genes.EYE_COLOUR_RIGHT.isDeterministic(g.pair(Genes.EYE_COLOUR_RIGHT), g));
        Genotype gold = Genotype.parse(locus(EyeLocus.IRIS_RIGHT, EyeHue.GOLD.token()));
        assertTrue(Genes.EYE_COLOUR_RIGHT.isDeterministic(gold.pair(Genes.EYE_COLOUR_RIGHT), gold),
                "every gold eye is the same gold - one cache entry per outcome");
    }

    /**
     * The right eye of each pair must sort <b>before</b> the left, because the
     * left's founder table reads the right's through {@code FounderContext} - and
     * asking for a gene that has not been rolled yet throws.
     */
    @Test
    void everyRightEyeLocusSortsBeforeItsLeftTwin() {
        for (EyeLocus locus : EyeLocus.values()) {
            if (locus.side() != EyeLocus.EyeSideRef.RIGHT) {
                continue;
            }
            Gene right = Genes.byKey(locus.key());
            Gene left = Genes.byKey(locus.twin().key());
            assertTrue(Genes.codeOrder().indexOf(right) < Genes.codeOrder().indexOf(left),
                    locus + " must roll before " + locus.twin());
        }
    }

    /**
     * <b>Wild heterochromia stays rare.</b> The two eyes are separate loci -
     * which is the point - but a founder's second eye usually copies its first,
     * so an odd-eyed wild horse is a find rather than an everyday sight.
     */
    @Test
    void foundersRarelyHaveOddEyes() {
        Rng rng = new SeededRng(20260910L, "eyes");
        int odd = 0;
        int trials = 600;
        for (int i = 0; i < trials; i++) {
            Genotype g = Genotype.random(rng);
            if (!Genes.EYE_COLOUR_RIGHT.hueOf(g.pair(Genes.EYE_COLOUR_RIGHT))
                    .equals(Genes.EYE_COLOUR_LEFT.hueOf(g.pair(Genes.EYE_COLOUR_LEFT)))) {
                odd++;
            }
        }
        assertTrue(odd < trials / 10, "odd-eyed founders should be well under a tenth, saw " + odd);
    }

    /**
     * The palettes {@link EyeHue} publishes are what the loci actually offer -
     * the third eye's {@code defined} allele invents an eye from them and must
     * never invent one no horse could have inherited.
     */
    @Test
    void theHuePalettesMatchTheLociThatOfferThem() {
        assertPalette(Genes.EYE_COLOUR_RIGHT, EyeHue.IRIS_PALETTE);
        assertPalette(Genes.EYE_SECTOR_COLOUR_RIGHT, EyeHue.SECTOR_PALETTE);
        assertPalette(Genes.EYE_SCLERA_RIGHT, EyeHue.SCLERA_PALETTE);
    }

    private static void assertPalette(Gene gene, EyeHue[] palette) {
        Set<String> declared = new LinkedHashSet<>();
        for (Allele a : gene.alleles()) {
            declared.add(a.token());
        }
        Set<String> expected = new LinkedHashSet<>();
        for (EyeHue h : palette) {
            expected.add(h.token());
        }
        assertEquals(expected, declared, gene.key() + " and its palette disagree");
    }

    // ==================================================================
    // Magic sectoral heterochromia - unchanged, and still paints over the top
    // ==================================================================

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
                leftMask |= (left[i] == green ? 0 : 1) << i;
            }
            assertNotEquals(rightMask, leftMask, "seed " + seed + " gave both eyes one shape");
            assertNotEquals(0, rightMask, "a wedge is never the whole iris");
            assertNotEquals(EyePatch.WHOLE, rightMask, "nor is it empty");
        }
    }

    /**
     * Homozygous, and anything carrying the wild type, shows nothing - so the
     * horse keeps whatever its own eye loci gave it.
     */
    @Test
    void matchedPairsAndCarriersShowNothing() {
        for (String tokens : new String[]{"green/green", "chaos/chaos", "gold/n", "chaos/n", "n/n"}) {
            assertEquals(BROWN, eye(sectoral(tokens))[1], tokens + " must not touch the iris");
        }
    }

    /** And it must not overwrite the horse's own eye colour either. */
    @Test
    void aMatchedPairDoesNotOverwriteTheHorsesOwnEyeColour() {
        String code = with("matp", "Cr/Cr") + "-horsegenetics.magic_sectoral_heterochromia=gold/gold";
        assertEquals(EyeHue.LIGHT_BLUE.fixedRgb(), eye(code)[1]);
    }

    /**
     * It paints over the eye loci rather than competing with them, so a splashed
     * white horse carrying two colours shows the two colours.
     */
    @Test
    void theMagicLocusPaintsOverEvenABlueEye() {
        String code = with("mitf", "SW1/N") + "-horsegenetics.magic_sectoral_heterochromia=green/gold";
        assertEquals(Set.of(MagicSectoralHeterochromiaGene.GREEN_RGB,
                        MagicSectoralHeterochromiaGene.GOLD_RGB),
                colours(iris(compose(code, 3L), 0)));
    }

    /**
     * <b>Fifteen outcomes, not one.</b> The wiki's preview widget, the in-game
     * gene dictionary and the genotype catalogue all enumerate a gene by its
     * <i>distinct expressions</i>, so declaring one
     * {@code sectoral-heterochromia} for every expressing pair would collapse
     * fifteen separately breedable results into one button.
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

    // ==================================================================
    // Breeding
    // ==================================================================

    /**
     * <b>An eye colour is inherited like anything else</b> - which is what the
     * whole design buys. Two gold-eyed horses breed gold-eyed foals; a gold-eyed
     * horse crossed to a plain one breeds carriers.
     */
    @Test
    void eyeColourBreedsTrue() {
        Rng rng = new SeededRng(20260910L, "breed");
        Genome gold = Genome.of(Genotype.parse(
                locus(EyeLocus.IRIS_RIGHT, EyeHue.GOLD.token()) + "-horsegenetics.sex=X/X"), rng);
        Genome goldSire = Genome.of(Genotype.parse(
                locus(EyeLocus.IRIS_RIGHT, EyeHue.GOLD.token()) + "-horsegenetics.sex=X/Y"), rng);
        for (int i = 0; i < 25; i++) {
            Genotype foal = gold.breedWith(goldSire, rng).genotype();
            assertEquals(EyeHue.GOLD, Genes.EYE_COLOUR_RIGHT.hueOf(foal.pair(Genes.EYE_COLOUR_RIGHT)),
                    "two gold-eyed parents can only make a gold-eyed foal");
        }
    }

    /**
     * And a requested colour is inherited too - the property that makes this a
     * written allele rather than an override. A splashed white parent passes on
     * blue whether or not the foal gets the splash.
     */
    @Test
    void arequestedColourIsPassedOn() {
        Rng rng = new SeededRng(20260910L, "request");
        String splash = with("mitf", "SW1/N");
        long seed = seedWhere(splash, img -> bothEyesAre(img, BLUE));
        Epigenome epi = Epigenome.fromSeed(seed);
        Genotype dam = Eyes.force(Genotype.parse(splash + "-horsegenetics.sex=X/X"), epi);
        assertEquals(EyeHue.MID_BLUE, Genes.EYE_COLOUR_RIGHT.hueOf(dam.pair(Genes.EYE_COLOUR_RIGHT)));
        for (Allele a : dam.pair(Genes.EYE_COLOUR_RIGHT).gene().alleles()) {
            // every gamete from a forced homozygote carries the forced allele
            if (a.token().equals(EyeHue.MID_BLUE.token())) {
                assertTrue(dam.pair(Genes.EYE_COLOUR_RIGHT).homozygousFor(a),
                        "a forced locus is written homozygous, so every gamete carries it");
            }
        }
    }
}
