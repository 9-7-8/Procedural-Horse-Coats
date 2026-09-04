package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.Epigenome;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatTextureComposerTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private static final String BLACK = Genotype.wildType().toCode();
    private static String override(String... kv) {
        String[] segs = Genotype.wildType().toCode().split("-");
        var order = Genes.codeOrder();
        for (String pair : kv) {
            String[] p = pair.split("=");
            for (int i = 0; i < order.size(); i++) {
                if (order.get(i).key().endsWith("." + p[0])) {
                    segs[i] = order.get(i).key() + "=" + p[1];
                }
            }
        }
        return String.join("-", segs);
    }

    private static final String CHESTNUT = override("extension=e/e");
    private static final String BAY = override("agouti=A/a");
    private static final String WHITE = override("kit=W22/N");
    private static final String CHAMPAGNE_BLACK = override("champagne=Ch/c");
    private static final String CHAMPAGNE_BAY = override("agouti=A/a", "champagne=Ch/c");
    private static final String SPLASH = override("agouti=A/a", "mitf=SW1/N");
    private static final String GREY_BLACK = override("grey=G/g");
    private static final String BUCKSKIN = override("agouti=A/a", "matp=Cr/N");
    private static final String PERLINO = override("agouti=A/a", "matp=Cr/Cr");
    private static final String PEARL_BAY = override("agouti=A/a", "matp=prl/prl");

    /** Synthetic LUT; bottom row is pure black. */
    private static GradientLut lut() {
        int s = 16;
        int[] a = new int[s * s];
        int white = 0xFFF0EDEA, red = 0xFF9B4A28, black = 0xFF000000;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float redLevel = 1f - x / (float) (s - 1);
                float blackLevel = y / (float) (s - 1);
                a[y * s + x] = lerp(lerp(white, red, redLevel), black, blackLevel);
            }
        }
        return new GradientLut(a, s, s);
    }

    private static int lerp(int c0, int c1, float t) {
        int r = Math.round(((c0 >> 16) & 0xFF) + (((c1 >> 16) & 0xFF) - ((c0 >> 16) & 0xFF)) * t);
        int gg = Math.round(((c0 >> 8) & 0xFF) + (((c1 >> 8) & 0xFF) - ((c0 >> 8) & 0xFF)) * t);
        int b = Math.round((c0 & 0xFF) + ((c1 & 0xFF) - (c0 & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static int[] template(Skin skin) {
        int[] t = new int[N * N];
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> t[py * N + px] = 0xFFFFFFFF);
        return t;
    }

    private static int[] compose(String code, long seed) {
        return CoatTextureComposer.compose(
                Genotype.parse(code), Epigenome.fromSeed(seed), Skin.ADULT, true, template(Skin.ADULT), lut());
    }

    private static int[] composeFoal(String code, long seed) {
        return CoatTextureComposer.compose(
                Genotype.parse(code), Epigenome.fromSeed(seed), Skin.BABY, false, template(Skin.BABY), lut());
    }

    private static int brightness(int[] img, Skin skin, Part part) {
        long[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(skin, part, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            acc[0] += ((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF);
            acc[1]++;
        });
        return (int) (acc[0] / Math.max(1, acc[1]));
    }

    /**
     * Every gene has to compose without throwing, alone and in combination, adult
     * and foal. The full {@code 2^genes} homozygous sweep is no longer tractable,
     * so this runs the all-wild and all-variant corners, a single-gene-on sweep
     * (each gene by itself), and a large seeded random sample of combinations.
     */
    @Test
    void everyBuiltInGeneComboComposesWithoutThrowingAdultAndFoal() {
        var genes = Genes.codeOrder();
        int n = genes.size();
        int[] tA = template(Skin.ADULT);
        int[] tB = template(Skin.BABY);
        GradientLut lut = lut();

        java.util.List<Long> masks = new java.util.ArrayList<>();
        masks.add(0L);
        masks.add((1L << n) - 1);
        for (int i = 0; i < n; i++) {
            masks.add(1L << i);
        }
        java.util.Random rng = new java.util.Random(20260902L);
        for (int k = 0; k < 3000; k++) {
            masks.add(rng.nextLong() & ((1L << n) - 1));
        }

        for (long mask : masks) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < n; i++) {
                var alleles = genes.get(i).alleles();
                var al = ((mask >> i) & 1) == 1 ? alleles.get(0) : alleles.get(alleles.size() - 1);
                if (code.length() > 0) code.append('-');
                code.append(genes.get(i).key()).append('=')
                        .append(al.token()).append('/').append(al.token());
            }
            Genotype gt = Genotype.parse(code.toString());
            Epigenome epi = Epigenome.fromSeed(mask);
            assertEquals(N * N, CoatTextureComposer.compose(gt, epi, Skin.ADULT, true, tA, lut).length);
            assertEquals(N * N, CoatTextureComposer.compose(gt, epi, Skin.BABY, false, tB, lut).length);
        }
    }

    @Test
    void pureBlackIsLiftedToAbout80PercentOpacity() {
        int b = brightness(compose(BLACK, 0L), Skin.ADULT, Part.BODY);
        assertTrue(b > 120 && b < 180, "black body should be lifted to ~20% grey, got " + b);
    }

    @Test
    void chestnutIsReddishAndLighterThanBlack() {
        int[] chestnut = compose(CHESTNUT, 0L);
        assertTrue(brightness(chestnut, Skin.ADULT, Part.BODY) > brightness(compose(BLACK, 0L), Skin.ADULT, Part.BODY));
        long[] rb = {0, 0};
        HorseSkinGeometry.forEachTexel(Part.BODY, (px, py, p, face, point) -> {
            int v = chestnut[py * N + px];
            rb[0] += (v >> 16) & 0xFF;
            rb[1] += v & 0xFF;
        });
        assertTrue(rb[0] > rb[1], "chestnut body redder than blue");
    }

    @Test
    void whiteHorseIsExactlyTheTemplate() {
        assertArrayEquals(template(Skin.ADULT), compose(WHITE, 0L));
    }

    @Test
    void champagneReadsOffWhatItSitsOn() {
        int chBlack = brightness(compose(CHAMPAGNE_BLACK, 0L), Skin.ADULT, Part.BODY);
        int chBay = brightness(compose(CHAMPAGNE_BAY, 0L), Skin.ADULT, Part.BODY);
        assertTrue(chBlack > brightness(compose(BLACK, 0L), Skin.ADULT, Part.BODY), "champagne lightens black");
        assertNotEquals(chBlack, chBay, "champagne-on-black differs from champagne-on-bay");
    }

    /** Mean and population standard deviation of texel brightness over one part. */
    private static double[] partStats(int[] img, Skin skin, Part part) {
        java.util.List<Double> vs = new java.util.ArrayList<>();
        HorseSkinGeometry.forEachTexel(skin, part, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            vs.add((((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF)) / 3.0);
        });
        double mean = vs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double var = vs.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0);
        return new double[]{mean, Math.sqrt(var)};
    }

    /** How far up a leg, as a fraction of its height, black still reads darker than the body. */
    private static double blackLegHeight(int[] img, Part leg) {
        Bounds b = HorseSkinGeometry.bounds(leg);
        double body = partStats(img, Skin.ADULT, Part.BODY)[0];
        double[] highest = {0};
        HorseSkinGeometry.forEachTexel(leg, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            double lum = (((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF)) / 3.0;
            double frac = (point.y() - b.yMin()) / b.span(HorseSkinGeometry.Axis.Y);
            // ignore the hoof end, which the template shades dark on every horse
            if (frac > 0.2 && lum < body * 0.75) {
                highest[0] = Math.max(highest[0], frac);
            }
        });
        return highest[0];
    }

    @Test
    void greyIsDappledNotFlat() {
        // the plain-black body only varies by the template's own shading; a grey
        // one carries the dapple field on top of it
        double flat = partStats(compose(BLACK, 0L), Skin.ADULT, Part.BODY)[1];
        double grey = partStats(compose(GREY_BLACK, 3L), Skin.ADULT, Part.BODY)[1];
        assertTrue(grey > flat * 2, "grey body should be dappled, sd " + grey + " vs flat " + flat);
    }

    @Test
    void greyStaysNeutralInsteadOfWalkingIntoTheGradientsGolds() {
        int[] img = compose(GREY_BLACK, 3L);
        long[] rb = {0, 0, 0};
        HorseSkinGeometry.forEachTexel(Part.BODY, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            rb[0] += (v >> 16) & 0xFF;
            rb[1] += v & 0xFF;
            rb[2]++;
        });
        double warmth = (rb[0] - rb[1]) / (double) rb[2];
        assertTrue(warmth < 25, "grey body should read neutral, red-minus-blue was " + warmth);
    }

    @Test
    void howFarGreyingHasGoneIsPerHorse() {
        double a = greyBodyBrightness(55L);
        double b = greyBodyBrightness(8L);
        assertTrue(Math.abs(a - b) > 20, "two greys should be at different stages, got " + a + " / " + b);
    }

    /** Mean body brightness of a grey adult rolled from {@code seed}. */
    private static double greyBodyBrightness(long seed) {
        return partStats(compose(GREY_BLACK, seed), Skin.ADULT, Part.BODY)[0];
    }

    @Test
    void greyNeverEndsUpDarkerThanTheCoatItGreys() {
        double plain = partStats(compose(BLACK, 0L), Skin.ADULT, Part.BODY)[0];
        for (long seed : new long[]{0L, 1L, 3L, 9L, 21L}) {
            double grey = partStats(compose(GREY_BLACK, seed), Skin.ADULT, Part.BODY)[0];
            assertTrue(grey > plain, "grey seed " + seed + " came out at " + grey + " vs black " + plain);
        }
    }

    /**
     * Bays spread from low socks to seal. Scanned over a range of seeds rather
     * than pinned to two of them: the epigenome derives a gene's seed from its
     * position in {@code codeOrder()}, so registering a gene reshuffles which
     * horse a given seed produces without changing the spread this asserts.
     */
    @Test
    void bayPointHeightVariesFromHorseToHorse() {
        double low = Double.MAX_VALUE;
        double high = -1.0;
        for (long seed = 0L; seed < 12L; seed++) {
            double h = blackLegHeight(compose(BAY, seed), Part.LEFT_FRONT_LEG);
            low = Math.min(low, h);
            high = Math.max(high, h);
        }
        assertTrue(high > low + 0.15,
                "bays should run from low socks to seal, got " + low + " .. " + high);
    }

    @Test
    void bayLegsDoNotAllStopAtExactlyTheSameHeight() {
        int[] img = compose(BAY, 3L);
        java.util.Set<Double> heights = new java.util.HashSet<>();
        for (Part leg : com.example.horsegenetics.common.coat.pattern.CoatRegions.LEGS) {
            heights.add(blackLegHeight(img, leg));
        }
        assertTrue(heights.size() > 1, "the four legs should jitter, all stopped at " + heights);
    }

    @Test
    void greyOnlyGreysAdultsNotFoals() {
        int adultGrey = brightness(compose(GREY_BLACK, 0L), Skin.ADULT, Part.BODY);
        int adultPlain = brightness(compose(BLACK, 0L), Skin.ADULT, Part.BODY);
        assertTrue(adultGrey > adultPlain, "adult grey should be lighter than adult black");
        // a grey foal == a plain-black foal
        assertArrayEquals(composeFoal(BLACK, 0L), composeFoal(GREY_BLACK, 0L));
    }

    @Test
    void creamDiluesRedOnlyOnSingleDoseAndBothOnDouble() {
        int bayBody = brightness(compose(BAY, 0L), Skin.ADULT, Part.BODY);
        int buckBody = brightness(compose(BUCKSKIN, 0L), Skin.ADULT, Part.BODY);
        int perlinoBody = brightness(compose(PERLINO, 0L), Skin.ADULT, Part.BODY);
        assertTrue(buckBody > bayBody, "single cream lightens the (red) body");
        assertTrue(perlinoBody > buckBody, "double cream lightens it further");
        // buckskin points (mane) stay near-black; perlino points lift
        int buckMane = brightness(compose(BUCKSKIN, 0L), Skin.ADULT, Part.MANE);
        int perlinoMane = brightness(compose(PERLINO, 0L), Skin.ADULT, Part.MANE);
        assertTrue(perlinoMane > buckMane, "double cream also lifts the black points");
    }

    @Test
    void creamPlusPearlActsAsDoubleCream() {
        int perlino = brightness(compose(PERLINO, 0L), Skin.ADULT, Part.BODY);
        int crPrl = brightness(compose(override("agouti=A/a", "matp=Cr/prl"), 0L), Skin.ADULT, Part.BODY);
        assertTrue(Math.abs(perlino - crPrl) < 25, "Cr/prl body should be about as pale as Cr/Cr");
    }

    @Test
    void doublePearlDilutesBothPigmentsMildly() {
        int[] bay = compose(BAY, 0L);
        int[] pearl = compose(PEARL_BAY, 0L);
        assertTrue(brightness(pearl, Skin.ADULT, Part.BODY) > brightness(bay, Skin.ADULT, Part.BODY),
                "pearl bay body is diluted lighter than bay");
        // and unlike single cream (body only), it touches the black points too
        assertTrue(brightness(pearl, Skin.ADULT, Part.MANE) > brightness(bay, Skin.ADULT, Part.MANE),
                "double pearl also lifts the points");
    }

    @Test
    void bayHasBlackManeAndDarkerLegBottoms() {
        int[] img = compose(BAY, 12345L);
        assertTrue(brightness(img, Skin.ADULT, Part.MANE) < brightness(img, Skin.ADULT, Part.BODY));
        Bounds leg = HorseSkinGeometry.bounds(Part.LEFT_FRONT_LEG);
        long[] low = {0, 0};
        long[] high = {0, 0};
        HorseSkinGeometry.forEachTexel(Part.LEFT_FRONT_LEG, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            int b = ((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF);
            double frac = (point.y() - leg.yMin()) / leg.span(HorseSkinGeometry.Axis.Y);
            if (frac < 0.10) { low[0] += b; low[1]++; } else if (frac > 0.9) { high[0] += b; high[1]++; }
        });
        assertTrue(low[1] > 0 && high[1] > 0 && low[0] / low[1] < high[0] / high[1]);
    }

    @Test
    void splashPunchesTransparentWhiteIntoTheLowerLegs() {
        int[] img = compose(SPLASH, 4242L);
        int[] tmpl = template(Skin.ADULT);
        Bounds leg = HorseSkinGeometry.bounds(Part.LEFT_FRONT_LEG);
        boolean[] found = {false};
        HorseSkinGeometry.forEachTexel(Part.LEFT_FRONT_LEG, (px, py, p, face, point) -> {
            double frac = (point.y() - leg.yMin()) / leg.span(HorseSkinGeometry.Axis.Y);
            if (frac < 0.15 && img[py * N + px] == tmpl[py * N + px]) found[0] = true;
        });
        assertTrue(found[0]);
    }

    @Test
    void testGenePaintsItsGradientFlatOnTopOfAnyBase() {
        // flat paint on top - visible over chestnut AND over pure black
        assertFalse(Arrays.equals(compose(CHESTNUT, 0L), compose(override("extension=e/e", "test=T/t"), 0L)));
        assertFalse(Arrays.equals(compose(BLACK, 0L), compose(override("test=T/t"), 0L)));
    }

    @Test
    void deterministicCoatsIgnoreTheSeedNonDeterministicOnesReplay() {
        assertArrayEquals(compose(BLACK, 1L), compose(BLACK, 9999L));
        assertArrayEquals(compose(BAY, 7L), compose(BAY, 7L));
        assertFalse(Arrays.equals(compose(BAY, 1L), compose(BAY, 2L)));
    }

    @Test
    void foalGetsTheSameTreatments() {
        int[] foal = composeFoal(BAY, 12345L);
        assertTrue(brightness(foal, Skin.BABY, Part.TAIL) < brightness(foal, Skin.BABY, Part.BODY),
                "bay foal tail should be black-ish vs the body");
        assertFalse(Arrays.equals(composeFoal(BLACK, 0L), foal), "foal coats vary by genotype");
        // grey does not touch a foal
        assertArrayEquals(composeFoal(BAY, 12345L), composeFoal(override("agouti=A/a", "grey=G/g"), 12345L));
    }
}
