package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.Arrays;

/**
 * The coat overlay pipeline. Turns a {@link Genotype} + epigenetic seed into a
 * 128px ARGB coat texture, given the white-horse template and the red/black
 * {@link GradientLut}.
 *
 * <ol>
 *   <li><b>natural pass</b> - every pixel starts at max red + max black
 *       pigment; each visible <b>natural</b> gene, in {@link Genes#naturalOrder()},
 *       pushes the {@link PigmentField} down.</li>
 *   <li><b>resolve</b> - each mapped texel's {@code (red, black)} pair is looked
 *       up in the gradient. A fully-restricted texel becomes transparent; a
 *       texel that resolves to <b>pure black</b> is knocked to 80% opacity so
 *       black coats aren't a flat void.</li>
 *   <li><b>multiply pass</b> - each visible <b>non-natural</b> gene
 *       ({@link Genes#multiplyOrder()}, currently only Test) fills a layer that
 *       is multiplied onto the resolved overlay.</li>
 *   <li><b>composite</b> - the overlay is multiplied onto the template, alpha
 *       taken into account (0% = template unchanged, 80% = 20% of the template
 *       survives), keeping the template's alpha.</li>
 *   <li><b>eyes</b> - copied straight back from the template.</li>
 * </ol>
 */
public final class CoatTextureComposer {

    private static final int OPAQUE_WHITE = 0xFFFFFFFF;
    private static final int PURE_BLACK_ALPHA = 0xCC; // 80%

    private CoatTextureComposer() {}

    public static int[] compose(Genotype genotype, long epigeneticSeed, int[] template, GradientLut lut) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        if (template.length != n * n) {
            throw new IllegalArgumentException("template must be " + (n * n) + " ARGB pixels, got " + template.length);
        }

        CoatBuildContext ctx = new CoatBuildContext(genotype, epigeneticSeed);

        // 1. natural pass
        for (Gene gene : Genes.naturalOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (gene.isVisible(pair, genotype)) {
                gene.restrict(pair, ctx);
            }
        }

        // 2. resolve pigment field -> ARGB overlay
        int[] overlay = ctx.overlay();
        PigmentField pig = ctx.pigment();
        HorseSkinGeometry.forEachTexel((px, py, part, face, point) -> {
            int i = py * n + px;
            float r = pig.red(px, py);
            float b = pig.black(px, py);
            if (r <= 0.02f && b <= 0.02f) {
                overlay[i] = 0; // fully restricted -> transparent
                return;
            }
            int rgb = lut.sample(r, b) & 0xFFFFFF;
            overlay[i] = (rgb == 0 ? PURE_BLACK_ALPHA << 24 : 0xFF000000) | rgb;
        });

        // 3. multiply pass (non-natural genes)
        for (Gene gene : Genes.multiplyOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (!gene.isVisible(pair, genotype)) {
                continue;
            }
            int[] layer = new int[n * n];
            Arrays.fill(layer, OPAQUE_WHITE);
            gene.multiplyLayer(pair, ctx, layer);
            for (int i = 0; i < overlay.length; i++) {
                int o = overlay[i];
                if ((o >>> 24) == 0) {
                    continue;
                }
                int l = layer[i];
                int rr = mul((o >> 16) & 0xFF, (l >> 16) & 0xFF);
                int gg = mul((o >> 8) & 0xFF, (l >> 8) & 0xFF);
                int bb = mul(o & 0xFF, l & 0xFF);
                overlay[i] = (o & 0xFF000000) | (rr << 16) | (gg << 8) | bb;
            }
        }

        // 4. composite overlay onto the template, alpha-aware, keep template alpha
        int[] out = new int[n * n];
        for (int i = 0; i < out.length; i++) {
            int t = template[i];
            int ta = t >>> 24;
            if (ta == 0) {
                out[i] = 0;
                continue;
            }
            int o = overlay[i];
            float oa = (o >>> 24) / 255f;
            int rr = blend((t >> 16) & 0xFF, (o >> 16) & 0xFF, oa);
            int gg = blend((t >> 8) & 0xFF, (o >> 8) & 0xFF, oa);
            int bb = blend(t & 0xFF, o & 0xFF, oa);
            out[i] = (ta << 24) | (rr << 16) | (gg << 8) | bb;
        }

        // 5. eyes
        CoatRegions.redrawEyes(out, template);
        return out;
    }

    /** template * (overlay/255 * a + (1-a)) - a=0 leaves the template, a=1 is a pure multiply. */
    private static int blend(int templateCh, int overlayCh, float a) {
        float factor = (overlayCh / 255f) * a + (1f - a);
        int v = Math.round(templateCh * factor);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static int mul(int a, int b) {
        return (a * b + 127) / 255;
    }
}
