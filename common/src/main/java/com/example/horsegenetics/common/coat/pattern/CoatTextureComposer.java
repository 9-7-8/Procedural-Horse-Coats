package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * The three-phase coat pipeline. Turns a {@link Genotype} + its
 * {@link Epigenome} into a 128px ARGB coat texture for a given {@link Skin}
 * (adult vs foal) and age ({@code adult} - grey only greys adults), given the
 * white template and the red/black {@link GradientLut}.
 *
 * <ol>
 *   <li><b>natural (melanin) phase</b> - every texel starts max red + max
 *       black; each visible natural gene (in {@link Genes#naturalOrder()})
 *       returns a {@link PigmentField} with the pigment pushed further down.
 *       Downward only.</li>
 *   <li><b>resolve</b> - {@code (red, black)} -&gt; {@link GradientLut}, into
 *       the {@link ColorField}. Fully restricted -&gt; transparent; resolves to
 *       pure black -&gt; 80% opacity.</li>
 *   <li><b>magical (RGB) phase</b> - each visible magical gene (in
 *       {@link Genes#magicalOrder()}) returns a signed RGB delta, folded into
 *       that colour field by integer addition (or, for flat paint, a replace).
 *       Nothing is capped to 0-255 until the field is converted.</li>
 *   <li><b>composite</b> onto the template, alpha-aware, keeping template
 *       alpha.</li>
 *   <li><b>eyes</b> - copied verbatim from the template.</li>
 * </ol>
 *
 * <p>The composer owns both fields; genes only ever see read-only views and
 * hand back their own contribution, so the whole bake is a fold and a gene can
 * be tested against a synthetic coat on its own.
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

        // 1. natural phase - each gene's expression folds the pigment field further down.
        PigmentField pigment = new PigmentField(n);
        for (Gene gene : Genes.naturalOrder()) {
            Expression expression = gene.expressionIn(genotype.pair(gene), genotype);
            if (expression.wildType()) {
                continue;
            }
            PigmentField next = expression.restrict(ctx, pigment);
            if (next != null) {
                pigment = next;
            }
        }

        // 2. resolve - pigment through the gradient, into the colour field.
        // Texels this skin doesn't map are left at zero = fully transparent.
        ColorField colour = new ColorField(n);
        PigmentField resolved = pigment;
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            float r = resolved.red(px, py);
            float b = resolved.black(px, py);
            if (r <= TRANSPARENT_EPS && b <= TRANSPARENT_EPS) {
                return;
            }
            int rgb = lut.sample(r, b) & 0xFFFFFF;
            colour.setArgb(px, py, (rgb == 0 ? PURE_BLACK_ALPHA << 24 : 0xFF000000) | rgb);
        });

        // 3. magical phase - each gene's signed RGB delta accumulates.
        for (Gene gene : Genes.magicalOrder()) {
            Expression expression = gene.expressionIn(genotype.pair(gene), genotype);
            if (expression.wildType()) {
                continue;
            }
            ColorField delta = expression.tint(ctx, pigment, colour);
            if (delta != null) {
                colour.apply(delta);
            }
        }

        // 4. composite onto the template, alpha-aware multiply.
        int[] out = new int[n * n];
        for (int i = 0; i < out.length; i++) {
            int t = template[i];
            int ta = t >>> 24;
            if (ta == 0) {
                out[i] = 0;
                continue;
            }
            int o = colour.argb(i % n, i / n);
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
