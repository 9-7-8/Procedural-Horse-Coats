package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * The coat overlay pipeline. Turns a {@link Genotype} + epigenetic seed into a
 * 128px ARGB coat texture, given the white-horse template and the red/black
 * {@link GradientLut} (both supplied by the game module).
 *
 * <ol>
 *   <li><b>restrict</b> - every pixel starts at max red + max black pigment;
 *       each visible gene, in {@link Genes#restrictionOrder()}, pushes the
 *       {@link PigmentField} down.</li>
 *   <li><b>resolve</b> - each mapped texel's {@code (red, black)} pair is looked
 *       up in the gradient; a fully-restricted pixel becomes a transparent
 *       overlay texel.</li>
 *   <li><b>paint</b> - direct-paint genes ({@link Genes#paintOrder()}, e.g.
 *       Test) draw ARGB over the resolved overlay.</li>
 *   <li><b>multiply</b> - overlay x template, per channel, keeping the
 *       template's alpha (so the horse silhouette is exactly the vanilla
 *       white one).</li>
 *   <li><b>eyes</b> - the eye texels are copied straight back from the
 *       template.</li>
 * </ol>
 */
public final class CoatTextureComposer {

    private CoatTextureComposer() {}

    public static int[] compose(Genotype genotype, long epigeneticSeed, int[] template, GradientLut lut) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        if (template.length != n * n) {
            throw new IllegalArgumentException("template must be " + (n * n) + " ARGB pixels, got " + template.length);
        }

        CoatBuildContext ctx = new CoatBuildContext(genotype, epigeneticSeed);

        // 1. restriction pass
        for (Gene gene : Genes.restrictionOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (gene.isVisible(pair, genotype)) {
                gene.restrict(pair, ctx);
            }
        }

        // 2. resolve the pigment field -> ARGB overlay
        int[] overlay = ctx.overlay();
        PigmentField pig = ctx.pigment();
        HorseSkinGeometry.forEachTexel((px, py, part, face, point) -> {
            int i = py * n + px;
            float r = pig.red(px, py);
            float b = pig.black(px, py);
            overlay[i] = (r <= 0.02f && b <= 0.02f) ? 0 : lut.sample(r, b);
        });

        // 3. paint pass
        for (Gene gene : Genes.paintOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (gene.isVisible(pair, genotype)) {
                gene.paint(pair, ctx);
            }
        }

        // 4. multiply overlay onto the template, keep template alpha
        int[] out = new int[n * n];
        for (int i = 0; i < out.length; i++) {
            int t = template[i];
            int ta = t >>> 24;
            if (ta == 0) {
                out[i] = 0;
                continue;
            }
            int o = overlay[i];
            if ((o >>> 24) == 0) {
                out[i] = t; // transparent overlay -> template shows through
                continue;
            }
            int rr = mul((t >> 16) & 0xFF, (o >> 16) & 0xFF);
            int gg = mul((t >> 8) & 0xFF, (o >> 8) & 0xFF);
            int bb = mul(t & 0xFF, o & 0xFF);
            out[i] = (ta << 24) | (rr << 16) | (gg << 8) | bb;
        }

        // 5. eyes
        CoatRegions.redrawEyes(out, template);
        return out;
    }

    private static int mul(int a, int b) {
        return (a * b + 127) / 255;
    }
}
