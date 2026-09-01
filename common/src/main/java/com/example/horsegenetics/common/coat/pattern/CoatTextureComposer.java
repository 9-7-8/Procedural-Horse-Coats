package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * The coat overlay pipeline. Turns a {@link Genotype} + its {@link Epigenome}
 * into a 128px ARGB coat texture for a given {@link Skin} (adult vs foal) and age
 * ({@code adult} - grey only greys adults), given the white template and the
 * red/black {@link GradientLut}.
 *
 * <ol>
 *   <li><b>natural pass</b> - every pixel starts max red + max black; each
 *       visible natural gene (in {@link Genes#naturalOrder()}) pushes the
 *       {@link PigmentField} down.</li>
 *   <li><b>resolve</b> - {@code (red, black)} -&gt; {@link GradientLut}. Fully
 *       restricted -&gt; transparent; resolves to pure black -&gt; 80% opacity.</li>
 *   <li><b>overlay pass</b> - each visible non-natural gene
 *       ({@link Genes#overlayOrder()}) paints its layer flat on top of the
 *       overlay (opaque layer texels replace what the natural pass resolved,
 *       so the effect shows even on a white / fully-restricted coat).</li>
 *   <li><b>composite</b> onto the template, alpha-aware, keeping template alpha.</li>
 *   <li><b>eyes</b> - copied verbatim from the template.</li>
 * </ol>
 */
public final class CoatTextureComposer {

    private static final int PURE_BLACK_ALPHA = 0xCC; // 80%

    /**
     * A texel goes fully transparent (bald white template shows through) only
     * when <i>both</i> pigments are essentially <b>zero</b> - i.e. dominant
     * white ({@code W_}) or a splash marking, both of which {@code setRed(0)} /
     * {@code setBlack(0)} exactly. This must stay far below any value a
     * <i>dilution</i> can legitimately leave behind: grey keeps 0.15, and grey
     * stacked on a double-dilute cream still lands near 0.012 - a near-white
     * coat that must resolve in the gradient, not vanish. (A 0.02 cutoff here
     * was turning grey cremello / grey perlino chestnuts and bays into flat
     * white horses.)
     */
    private static final float TRANSPARENT_EPS = 0.001f;

    private CoatTextureComposer() {}

    public static int[] compose(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult,
                                int[] template, GradientLut lut) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        if (template.length != n * n) {
            throw new IllegalArgumentException("template must be " + (n * n) + " ARGB pixels, got " + template.length);
        }

        CoatBuildContext ctx = new CoatBuildContext(genotype, epigenome, skin, adult);

        for (Gene gene : Genes.naturalOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (gene.isVisible(pair, genotype)) {
                gene.restrict(pair, ctx);
            }
        }

        int[] overlay = ctx.overlay();
        PigmentField pig = ctx.pigment();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            int i = py * n + px;
            float r = pig.red(px, py);
            float b = pig.black(px, py);
            if (r <= TRANSPARENT_EPS && b <= TRANSPARENT_EPS) {
                overlay[i] = 0;
                return;
            }
            int rgb = lut.sample(r, b) & 0xFFFFFF;
            overlay[i] = (rgb == 0 ? PURE_BLACK_ALPHA << 24 : 0xFF000000) | rgb;
        });

        for (Gene gene : Genes.overlayOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (!gene.isVisible(pair, genotype)) {
                continue;
            }
            int[] layer = new int[n * n]; // transparent (0) = "no paint here"
            gene.overlayLayer(pair, ctx, layer);
            for (int i = 0; i < overlay.length; i++) {
                int l = layer[i];
                if ((l >>> 24) == 0) {
                    continue;
                }
                overlay[i] = l; // flat paint on top - the layer wins outright
            }
        }

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

        CoatRegions.redrawEyes(skin, out, template);
        return out;
    }

    private static int blend(int templateCh, int overlayCh, float a) {
        float factor = (overlayCh / 255f) * a + (1f - a);
        int v = Math.round(templateCh * factor);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
