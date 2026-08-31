package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatTextureComposerTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** A synthetic LUT with the same axis convention as the real gradient. */
    private static GradientLut lut() {
        int s = 16;
        int[] a = new int[s * s];
        int white = 0xFFF0EDEA, red = 0xFF9B4A28, black = 0xFF171412;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float redLevel = 1f - x / (float) (s - 1);
                float blackLevel = y / (float) (s - 1);
                int base = lerp(white, red, redLevel);
                a[y * s + x] = lerp(base, black, blackLevel);
            }
        }
        return new GradientLut(a, s, s);
    }

    private static int lerp(int c0, int c1, float t) {
        int r = Math.round(((c0 >> 16) & 0xFF) + (((c1 >> 16) & 0xFF) - ((c0 >> 16) & 0xFF)) * t);
        int g = Math.round(((c0 >> 8) & 0xFF) + (((c1 >> 8) & 0xFF) - ((c0 >> 8) & 0xFF)) * t);
        int b = Math.round((c0 & 0xFF) + ((c1 & 0xFF) - (c0 & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Opaque mid-grey everywhere the geometry maps a texel; transparent elsewhere. */
    private static int[] template() {
        int[] t = new int[N * N];
        HorseSkinGeometry.forEachTexel((px, py, part, face, point) -> t[py * N + px] = 0xFFFFFFFF);
        // fake eye pixels so redrawEyes has something distinctive to copy back
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
    void blackHorseBodyIsDark() {
        int[] img = compose("Eeaawwttcc", 0L);
        assertTrue(brightness(img, Part.BODY) < 120, "black body brightness " + brightness(img, Part.BODY));
    }

    @Test
    void chestnutBodyIsReddishAndLighterThanBlack() {
        int[] chestnut = compose("eeaawwttcc", 0L);
        int[] black = compose("Eeaawwttcc", 0L);
        assertTrue(brightness(chestnut, Part.BODY) > brightness(black, Part.BODY));
        // reddish: average R clearly above average B on the body
        long[] rb = {0, 0, 0};
        HorseSkinGeometry.forEachTexel(Part.BODY, (px, py, p, face, point) -> {
            int v = chestnut[py * N + px];
            rb[0] += (v >> 16) & 0xFF;
            rb[1] += v & 0xFF;
            rb[2]++;
        });
        assertTrue(rb[0] > rb[1], "chestnut body should be redder than blue");
    }

    @Test
    void whiteHorseIsExactlyTheTemplate() {
        int[] img = compose("eeaaWwttcc", 0L);
        assertArrayEquals(template(), img);
    }

    @Test
    void champagneLightensABlackCoat() {
        int black = brightness(compose("Eeaawwttcc", 0L), Part.BODY);
        int champagne = brightness(compose("EeaawwttCc", 0L), Part.BODY);
        assertTrue(champagne > black, "champagne " + champagne + " should be lighter than black " + black);
    }

    @Test
    void bayHasABlackManeAndDarkerLegBottomsThanBody() {
        int[] img = compose("EeAawwttcc", 12345L);
        int mane = brightness(img, Part.MANE);
        int body = brightness(img, Part.BODY);
        assertTrue(mane < body, "bay mane (" + mane + ") should be darker than the body (" + body + ")");

        // hoof band vs upper body
        Bounds leg = HorseSkinGeometry.bounds(Part.LEFT_FRONT_LEG);
        long[] lowAcc = {0, 0};
        long[] highAcc = {0, 0};
        HorseSkinGeometry.forEachTexel(Part.LEFT_FRONT_LEG, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            int b = ((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF);
            double frac = (point.y() - leg.yMin()) / leg.span(HorseSkinGeometry.Axis.Y);
            if (frac < 0.15) { lowAcc[0] += b; lowAcc[1]++; }
            else if (frac > 0.85) { highAcc[0] += b; highAcc[1]++; }
        });
        assertTrue(lowAcc[1] > 0 && highAcc[1] > 0);
        assertTrue(lowAcc[0] / lowAcc[1] < highAcc[0] / highAcc[1],
                "hoof end should be darker than the top of the leg");
    }

    @Test
    void bayIsNonDeterministicButReplaysFromTheSameSeed() {
        assertFalse(Genotype.parse("EeAawwttcc").isDeterministic());
        assertArrayEquals(compose("EeAawwttcc", 777L), compose("EeAawwttcc", 777L));
        assertFalse(java.util.Arrays.equals(compose("EeAawwttcc", 1L), compose("EeAawwttcc", 2L)));
    }

    @Test
    void deterministicCoatsIgnoreTheSeed() {
        assertArrayEquals(compose("Eeaawwttcc", 1L), compose("Eeaawwttcc", 9_999L));
    }

    @Test
    void eyesAreCopiedStraightFromTheTemplate() {
        int[] img = compose("Eeaawwttcc", 0L);
        int[] tmpl = template();
        for (int[] r : CoatRegions.EYE_RECTS) {
            for (int y = r[1]; y < r[1] + r[3]; y++) {
                for (int x = r[0]; x < r[0] + r[2]; x++) {
                    assertEquals(tmpl[y * N + x], img[y * N + x], "eye texel (" + x + "," + y + ")");
                }
            }
        }
    }

    @Test
    void championeIsWarmToned() {
        int[] img = compose("EeaawwttCc", 0L);
        long[] rgb = {0, 0, 0, 0};
        HorseSkinGeometry.forEachTexel(Part.BODY, (px, py, p, face, point) -> {
            int v = img[py * N + px];
            rgb[0] += (v >> 16) & 0xFF;
            rgb[1] += (v >> 8) & 0xFF;
            rgb[2] += v & 0xFF;
            rgb[3]++;
        });
        // gold: R > G > B on average
        assertTrue(rgb[0] > rgb[1] && rgb[1] > rgb[2],
                "champagne body should read gold (R>G>B), got " + rgb[0] + "/" + rgb[1] + "/" + rgb[2]);
    }

    @Test
    void sealLiftsBlackOnTheLowerLegsOnly() {
        int[] img = compose("EeSawwttcc", 4242L);
        int body = brightness(img, Part.BODY);
        Bounds leg = HorseSkinGeometry.bounds(Part.LEFT_HIND_LEG);
        long[] low = {0, 0};
        HorseSkinGeometry.forEachTexel(Part.LEFT_HIND_LEG, (px, py, p, face, point) -> {
            double frac = (point.y() - leg.yMin()) / leg.span(HorseSkinGeometry.Axis.Y);
            if (frac < 0.10) {
                int v = img[py * N + px];
                low[0] += ((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF);
                low[1]++;
            }
        });
        assertTrue(body < 90, "seal body should still be dark");
        assertTrue(low[1] > 0 && low[0] / low[1] > body, "seal lower-leg should be lighter (tan) than the black body");
    }

    @Test
    void testGenePaintsTheGradientOverWhateverIsUnderneath() {
        int[] plain = compose("Eeaawwttcc", 0L);
        int[] tested = compose("EeaawwTtcc", 0L);
        assertFalse(java.util.Arrays.equals(plain, tested));
    }
}
