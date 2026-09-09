package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.BaseCoats;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Can this gene be seen on this horse?</b> - asked by baking both coats and
 * counting the texels that moved.
 *
 * <p>It exists because two tools need the same answer and were about to hold
 * two opinions about it. {@code GeneIconTool} asks it to choose a backdrop for
 * an icon: a gene that paints nothing on a plain bay gets photographed on a
 * tobiano instead, or it gets no icon at all. {@code GeneWikiTool} asks it to
 * choose which base coat a gene page's <i>preview window</i> opens on, which is
 * the same question about the same gene and must not come out differently.
 *
 * <p><b>Detected rather than declared</b>, in both callers. No list anywhere
 * says "fielded needs a tobiano"; a gene that starts or stops painting quietly
 * changes what it is shown on at the next bake, and no table goes stale. That
 * property is the whole reason this is measured rather than written down.
 */
public final class CoatVisibility {

    /** A channel has to move this far before a texel counts as repainted. */
    public static final int CHANNEL_STEP = 16;

    /** And this many texels have to move before the gene counts as showing. */
    public static final int MIN_TEXELS = 24;

    private CoatVisibility() {}

    /**
     * <b>The gene showing itself</b>, over {@code base}: whichever of its
     * alleles repaints the most of the horse when homozygous - or {@code null}
     * if none of them repaints enough of it to be worth looking at.
     *
     * <p>Homozygous, because a single picture has one expression to spend and
     * the full one is the honest thing to spend it on. <b>The loudest allele</b>
     * rather than the first declared, because a great many genes declare the
     * wild type first - Extension leads with {@code E}, and a picture of
     * {@code E/E} on a bay is a picture of a bay.
     *
     * <p>Loudest is measured, not assumed: swapping one allele copy for another
     * swaps the <i>epigenome slot</i> the coat reads with it, so {@code A/A} is
     * never byte-identical to the {@code A/a} underneath it even though both
     * are the same bay. A plain equality test therefore calls the wild type a
     * picture of the gene. Counting the texels that moved by
     * {@value #CHANNEL_STEP} or more separates that jitter, which is a handful
     * of texels, from a gene that actually paints, which is hundreds.
     */
    public static int[] showing(Gene gene, Genotype base, Epigenome epi,
                                int[] template, LutSet luts) {
        int[] plain = CoatTextureComposer.compose(base, epi, Skin.ADULT, true, template, luts);
        int[] best = null;
        int loudest = 0;
        for (Allele variant : gene.alleles()) {
            int[] sheet = CoatTextureComposer.compose(
                    base.with(new AllelePair(variant, variant)), epi, Skin.ADULT, true,
                    template, luts);
            int moved = repainted(sheet, plain);
            if (moved > loudest) {
                loudest = moved;
                best = sheet;
            }
        }
        return loudest >= MIN_TEXELS ? best : null;
    }

    /**
     * The <b>first base coat this gene can actually be seen on</b>, in
     * {@link BaseCoats#all()}'s own order - or {@code null} if it shows on none
     * of them.
     *
     * <p>The order is what makes this useful rather than arbitrary: bay leads,
     * so every ordinary gene answers "bay" and nothing changes. The white-marked
     * coats sit at the end, so only a gene that modifies <i>somebody else's</i>
     * white - fielded, voided, opalized - falls through to one, and it falls
     * through to the first one that works rather than to a hand-picked
     * favourite.
     */
    public static BaseCoats.BaseCoat firstShowing(Gene gene, Epigenome epi,
                                                  int[] template, LutSet luts) {
        for (BaseCoats.BaseCoat base : BaseCoats.all()) {
            if (showing(gene, base.genotype(), epi, template, luts) != null) {
                return base;
            }
        }
        return null;
    }

    /** How many texels {@code sheet} moved off {@code from}, ignoring epigenome jitter. */
    public static int repainted(int[] sheet, int[] from) {
        int moved = 0;
        for (int i = 0; i < sheet.length; i++) {
            if (sheet[i] == from[i]) {
                continue;
            }
            int d = Math.max(Math.max(
                    Math.abs(((sheet[i] >> 16) & 0xFF) - ((from[i] >> 16) & 0xFF)),
                    Math.abs(((sheet[i] >> 8) & 0xFF) - ((from[i] >> 8) & 0xFF))),
                    Math.abs((sheet[i] & 0xFF) - (from[i] & 0xFF)));
            if (d >= CHANNEL_STEP) {
                moved++;
            }
        }
        return moved;
    }
}
