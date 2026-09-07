package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.AlleleRandomness;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyeColorContribution;
import com.example.horsegenetics.common.genetics.EyePatch;
import com.example.horsegenetics.common.genetics.EyePatchContribution;
import com.example.horsegenetics.common.genetics.EyePatches;
import com.example.horsegenetics.common.genetics.EyeSpread;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.LutContribution;

import java.util.Optional;

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
 *   <li><b>overlay</b> - each gene implementing
 *       {@link CoatOverlayContribution} gets to write final pixels over the
 *       finished coat and to mark texels <b>emissive</b>. Almost nothing runs
 *       here; see {@link CoatOverlay} for the two things that have to.</li>
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

    /**
     * A finished coat: the ARGB sheet, plus the texels a
     * {@link CoatOverlayContribution} marked full-bright ({@code null} when none
     * did, which is the ordinary case).
     */
    public record Baked(int[] argb, boolean[] emissive) {

        /** Does any gene on this horse want an emissive render pass at all? */
        public boolean hasEmissive() {
            return emissive != null;
        }
    }

    /** {@link #bake}'s pixels alone - what every caller but the emissive layer wants. */
    public static int[] compose(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult,
                                int[] template, GradientLut lut) {
        return bake(genotype, epigenome, skin, adult, template, LutSet.of(lut)).argb();
    }

    /** {@link #compose} with a {@link LutSet}, so the {@code LUT} locus can swap the phase-2 gradient. */
    public static int[] compose(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult,
                                int[] template, LutSet luts) {
        return bake(genotype, epigenome, skin, adult, template, luts).argb();
    }

    public static Baked bake(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult,
                             int[] template, GradientLut lut) {
        return bake(genotype, epigenome, skin, adult, template, LutSet.of(lut));
    }

    public static Baked bake(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult,
                             int[] template, LutSet luts) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        if (template.length != n * n) {
            throw new IllegalArgumentException("template must be " + (n * n) + " ARGB pixels, got " + template.length);
        }

        CoatBuildContext ctx = new CoatBuildContext(genotype, epigenome, skin, adult);

        // The LUT locus (magical, phase-2): a horse homozygous for a variant
        // allele resolves its pigment against an unnatural gradient instead of
        // the red/black one. Selected here, out of band, because the swap is not
        // a phase-1 restriction or a phase-3 tint.
        GradientLut lut = luts.resolve(selectAlternateLut(genotype));

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

        // 5. overlay phase - final pixels and emissive texels, over the finished
        // coat. Runs after the eyes precisely so a gene can colour them.
        CoatOverlay overlay = new CoatOverlay(skin, out.clone());

        // 5a. Eye colour, before the general overlay pass, so a gene that wants
        // the whole eye (light's glowing gold, the leopard complex's white rim)
        // still gets the last word over the iris tint.
        paintEyes(genotype, epigenome, overlay, whiteCoverage(pigment, skin));

        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof CoatOverlayContribution contribution)) {
                continue;
            }
            AllelePair pair = genotype.pair(gene);
            if (gene.expressionIn(pair, genotype).wildType()) {
                continue;
            }
            contribution.overlay(pair, ctx, overlay);
        }
        overlay.applyTo(out);

        return new Baked(out, overlay.emissiveMask());
    }

    /**
     * The alternate-LUT key the {@code LUT} locus selects for this genotype, or
     * {@code null} for the natural gradient. The first gene implementing
     * {@link LutContribution} with a non-wild, non-empty answer wins - there is
     * only one such gene, so "first" is just defensiveness.
     */
    private static String selectAlternateLut(Genotype genotype) {
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof LutContribution lut)) {
                continue;
            }
            if (gene.expressionIn(genotype.pair(gene), genotype).wildType()) {
                continue;
            }
            Optional<String> key = lut.alternateLut(genotype.pair(gene), genotype);
            if (key.isPresent()) {
                return key.get();
            }
        }
        return null;
    }

    private static int blend(int templateCh, int overlayCh, float a) {
        float factor = (overlayCh / 255f) * a + (1f - a);
        int v = Math.round(templateCh * factor);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    // ------------------------------------------------------------------
    // Eye colour - one channel, several claimants
    // ------------------------------------------------------------------

    /** The gene that won the iris, and what it claimed. */
    record EyeClaim(Gene gene, EyeColor color) {}

    /**
     * <b>Draw both irises.</b> Three layers, and the order between them is the
     * whole model:
     *
     * <ol>
     *   <li>the winning <b>pigment</b> claim - cream, champagne, tiger eye -
     *       over the whole of both eyes;</li>
     *   <li>the winning <b>depigmenting</b> claim, if any, over as much of each
     *       eye as {@link EyeSpread} says it reached. That is what makes one
     *       blue eye and the blue wedge possible at all: a depigmented iris is
     *       not a colour painted over the pigment, it is pigment that never
     *       arrived, so what shows where the blue stops is the horse's own eye
     *       and it has to still be there underneath;</li>
     *   <li>every {@link EyePatchContribution}'s patches, in code order.</li>
     * </ol>
     */
    private static void paintEyes(Genotype genotype, Epigenome epigenome, CoatOverlay overlay,
                                  double whiteCoverage) {
        EyeClaim winner = eyeClaimOf(genotype, epigenome, whiteCoverage, Integer.MAX_VALUE);
        if (winner != null && winner.color().depigmented()) {
            EyeClaim under = eyeClaimOf(genotype, epigenome, whiteCoverage, EyeColor.RANK_DEPIGMENTED);
            if (under != null) {
                overlay.tintIris(under.color().rgb(), under.color().strength());
            }
            EyeColor blue = winner.color();
            EyeSpread spread = EyeSpread.roll(
                    AlleleRandomness.forGene(winner.gene(), genotype, epigenome).expressed());
            overlay.tintIrisSector(CoatRegions.RIGHT_EYE, spread.right(), blue.rgb(), blue.strength());
            overlay.tintIrisSector(CoatRegions.LEFT_EYE, spread.left(), blue.rgb(), blue.strength());
        } else if (winner != null) {
            overlay.tintIris(winner.color().rgb(), winner.color().strength());
        }

        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof EyePatchContribution contribution)) {
                continue;
            }
            contribution.eyePatches(genotype.pair(gene), genotype, epigenome, whiteCoverage)
                    .ifPresent(patches -> {
                        paint(overlay, CoatRegions.RIGHT_EYE, patches.right());
                        paint(overlay, CoatRegions.LEFT_EYE, patches.left());
                    });
        }
    }

    private static void paint(CoatOverlay overlay, int eye, java.util.List<EyePatch> patches) {
        for (EyePatch patch : patches) {
            if (!patch.empty()) {
                overlay.tintIrisSector(eye, patch.quadrants(),
                        patch.color().rgb(), patch.color().strength());
            }
        }
    }

    /**
     * The winning {@link EyeColor} claim for this horse below {@code maxRank},
     * or {@code null} for the template's own dark eye.
     *
     * <p>A horse has one iris colour, so the claims are <b>ranked, not
     * blended</b>: highest {@link EyeColor#rank()} wins and a tie goes to the
     * earlier gene in {@link Genes#codeOrder()}. See
     * {@link EyeColorContribution} for why the white loci are claimants at all
     * and why blue out-ranks a pigment colour.
     *
     * <p>{@code maxRank} is exclusive, and exists for exactly one caller: the
     * eye painter asks a second time, capped below
     * {@link EyeColor#RANK_DEPIGMENTED}, to find out what colour the horse's
     * iris would have been if the white loci had left it alone.
     */
    static EyeClaim eyeClaimOf(Genotype genotype, Epigenome epigenome, double whiteCoverage,
                               int maxRank) {
        EyeClaim best = null;
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof EyeColorContribution contribution)) {
                continue;
            }
            Optional<EyeColor> claim =
                    contribution.eyeColor(genotype.pair(gene), genotype, epigenome, whiteCoverage);
            if (claim.isEmpty() || claim.get().rank() >= maxRank) {
                continue;
            }
            if (best == null || best.color().losesTo(claim.get())) {
                best = new EyeClaim(gene, claim.get());
            }
        }
        return best;
    }

    /** The winning claim's colour - what a caller that does not care who made it wants. */
    static Optional<EyeColor> eyeColorOf(Genotype genotype, Epigenome epigenome, double whiteCoverage) {
        EyeClaim claim = eyeClaimOf(genotype, epigenome, whiteCoverage, Integer.MAX_VALUE);
        return Optional.ofNullable(claim == null ? null : claim.color());
    }

    /**
     * The fraction of mapped texels the white loci have left with <b>no pigment
     * at all</b> - the signal {@link EyeColorContribution} reads to decide
     * whether a horse is white enough for a blue eye.
     *
     * <p>Measured on the resolved pigment field rather than per locus on
     * purpose: a horse that is broadly white because two mild alleles stacked
     * has exactly the same claim on a blue eye as one that is white from a
     * single bold allele, and no per-locus test can see that.
     */
    private static double whiteCoverage(PigmentView coat, Skin skin) {
        int[] tally = new int[2];
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            tally[1]++;
            if (coat.red(px, py) <= TRANSPARENT_EPS && coat.black(px, py) <= TRANSPARENT_EPS) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0.0 : tally[0] / (double) tally[1];
    }

}
