package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatTextureComposerTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    // canonical codes (7 gene segments)
    private static final String BLACK = "E/E-a/a-w/w-t/t-c/c-sl/sl-spl/spl";
    private static final String CHESTNUT = "e/e-a/a-w/w-t/t-c/c-sl/sl-spl/spl";
    private static final String BAY = "E/e-A/a-w/w-t/t-c/c-sl/sl-spl/spl";
    private static final String WHITE = "e/e-a/a-W/w-t/t-c/c-sl/sl-spl/spl";
    private static final String CHAMPAGNE_BLACK = "E/E-a/a-w/w-t/t-Ch/c-sl/sl-spl/spl";
    private static final String SEAL = "E/E-a/a-w/w-t/t-c/c-Sl/sl-spl/spl";
    private static final String SPLASH = "E/E-a/a-w/w-t/t-c/c-sl/sl-Spl/spl";

    /** Synthetic LUT with the same axis convention as the real gradient. Bottom row is pure black. */
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

    private static int[] template() {
        int[] t = new int[N * N];
        HorseSkinGeometry.forEachTexel((px, py, part, face, point) -> t[py * N + px] = 0xFFFFFFFF);
        for (int[] r : CoatRegions.EYE_RECTS) {
            for (int y = r[1]; y < r[1] + r[3]; y++) {
                for (int x = r[0]; x < r[0] + r[2]; x++) {
                    t[y * N + x] = 0xFF00FF00;
                }
            }
        }
        return t;
    }

    private static int[] compose(String code, long seed) {
        return CoatTextureComposer.compose(Genotype.parse(code), seed, template(), lut());
    }

    private static int brightness(int[] img, Part part) {
        long[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(part, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            acc[0] += ((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF);
            acc[1]++;
        });
        return (int) (acc[0] / Math.max(1, acc[1]));
    }

    @Test
    void everyBuiltInGeneComboComposesWithoutThrowing() {
        var genes = com.example.horsegenetics.common.genetics.Genes.codeOrder();
        int[] tmpl = template();
        GradientLut lut = lut();
        for (int mask = 0; mask < (1 << genes.size()); mask++) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < genes.size(); i++) {
                var alleles = genes.get(i).alleles();
                var a = ((mask >> i) & 1) == 1 ? alleles.get(0) : alleles.get(alleles.size() - 1);
                if (code.length() > 0) {
                    code.append('-');
                }
                code.append(a.token()).append('/').append(a.token());
            }
            int[] img = CoatTextureComposer.compose(Genotype.parse(code.toString()), mask, tmpl, lut);
            assertEquals(N * N, img.length);
        }
    }

    @Test
    void pureBlackIsLiftedToAbout80PercentOpacity() {
        // pure-black overlay at 80% opacity over opaque white -> ~20% of 255 per
        // channel -> ~51/ch -> brightness (R+G+B) ~153, not 0.
        int b = brightness(compose(BLACK, 0L), Part.BODY);
        assertTrue(b > 120 && b < 180, "black body should be lifted to ~20% grey, got " + b);
    }

    @Test
    void chestnutIsReddishAndLighterThanBlack() {
        int[] chestnut = compose(CHESTNUT, 0L);
        assertTrue(brightness(chestnut, Part.BODY) > brightness(compose(BLACK, 0L), Part.BODY));
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
        assertArrayEquals(template(), compose(WHITE, 0L));
    }

    @Test
    void champagneReadsOffWhatItSitsOn() {
        int chBlack = brightness(compose(CHAMPAGNE_BLACK, 0L), Part.BODY);
        int black = brightness(compose(BLACK, 0L), Part.BODY);
        int chBay = brightness(compose("E/e-A/a-w/w-t/t-Ch/c-sl/sl-spl/spl", 0L), Part.BODY);
        int bay = brightness(compose(BAY, 0L), Part.BODY);
        assertTrue(chBlack > black, "champagne lightens black");
        assertNotEquals(chBlack, chBay, "champagne-on-black differs from champagne-on-bay");
    }

    @Test
    void bayHasABlackManeAndDarkerLegBottomsThanBody() {
        int[] img = compose(BAY, 12345L);
        assertTrue(brightness(img, Part.MANE) < brightness(img, Part.BODY));
        Bounds leg = HorseSkinGeometry.bounds(Part.LEFT_FRONT_LEG);
        long[] low = {0, 0};
        long[] high = {0, 0};
        HorseSkinGeometry.forEachTexel(Part.LEFT_FRONT_LEG, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            int b = ((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF);
            double frac = (point.y() - leg.yMin()) / leg.span(HorseSkinGeometry.Axis.Y);
            if (frac < 0.15) { low[0] += b; low[1]++; } else if (frac > 0.85) { high[0] += b; high[1]++; }
        });
        assertTrue(low[1] > 0 && high[1] > 0 && low[0] / low[1] < high[0] / high[1]);
    }

    @Test
    void sealFadesBlackDownTheLegsSmoothly() {
        int[] img = compose(SEAL, 999L);
        Bounds leg = HorseSkinGeometry.bounds(Part.LEFT_HIND_LEG);
        // hoof band should be darker than mid-leg (a gradient, not uniform)
        long[] hoof = {0, 0};
        long[] mid = {0, 0};
        HorseSkinGeometry.forEachTexel(Part.LEFT_HIND_LEG, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            int b = ((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF);
            double frac = (point.y() - leg.yMin()) / leg.span(HorseSkinGeometry.Axis.Y);
            if (frac < 0.08) { hoof[0] += b; hoof[1]++; } else if (frac > 0.55) { mid[0] += b; mid[1]++; }
        });
        assertTrue(hoof[1] > 0 && mid[1] > 0);
        assertTrue(hoof[0] / hoof[1] < mid[0] / mid[1], "seal hoof end should be darker than mid-leg (a gradient)");
    }

    @Test
    void splashPunchesTransparentWhiteIntoTheLowerLegs() {
        int[] img = compose(SPLASH, 4242L);
        int[] tmpl = template();
        // some lower-leg texel should now equal the template (pigment removed -> template shows)
        Bounds leg = HorseSkinGeometry.bounds(Part.LEFT_FRONT_LEG);
        boolean[] found = {false};
        HorseSkinGeometry.forEachTexel(Part.LEFT_FRONT_LEG, (px, py, p, face, point) -> {
            double frac = (point.y() - leg.yMin()) / leg.span(HorseSkinGeometry.Axis.Y);
            if (frac < 0.15 && img[py * N + px] == tmpl[py * N + px]) {
                found[0] = true;
            }
        });
        assertTrue(found[0], "splash should leave the very bottom of the leg as bare template");
    }

    @Test
    void testGeneMultipliesAGradientOverTheCoat() {
        // multiply by anything leaves pure black unchanged, so test on a lighter base
        int[] plain = compose(CHESTNUT, 0L);
        int[] tested = compose("e/e-a/a-w/w-T/t-c/c-sl/sl-spl/spl", 0L);
        assertFalse(Arrays.equals(plain, tested));
    }

    @Test
    void deterministicCoatsIgnoreTheSeedNonDeterministicOnesReplay() {
        assertArrayEquals(compose(BLACK, 1L), compose(BLACK, 9999L));
        assertArrayEquals(compose(BAY, 7L), compose(BAY, 7L));
        assertFalse(Arrays.equals(compose(BAY, 1L), compose(BAY, 2L)));
    }

    @Test
    void eyesAreCopiedStraightFromTheTemplate() {
        int[] img = compose(BLACK, 0L);
        int[] tmpl = template();
        for (int[] r : CoatRegions.EYE_RECTS) {
            for (int y = r[1]; y < r[1] + r[3]; y++) {
                for (int x = r[0]; x < r[0] + r[2]; x++) {
                    assertEquals(tmpl[y * N + x], img[y * N + x]);
                }
            }
        }
    }
}
