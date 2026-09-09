package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.BaseCoats;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

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

    /**
     * <b>How close to its best a base coat has to come to be preferred for
     * being earlier in the list.</b>
     *
     * <p>"The first coat it shows on at all" is not the same question as "the
     * coat it is worth photographing on", and treating them as one is how
     * Fielded ended up illustrated on a plain bay. Fielded draws tendrils out of
     * the edges of existing white; on a bay there is no white, but the muzzle
     * and the pasterns sit low enough in the pigment band to catch a few dozen
     * texels of it - over {@link #MIN_TEXELS}, so the bay "worked" and the
     * tobiano was never tried. Mushroom is the same story on a bay it barely
     * touches and a chestnut it transforms.
     *
     * <p>So the rule is relative: measure every base coat, and take the
     * <b>first</b> one that reaches this share of the <b>loudest</b>. An
     * ordinary gene paints about the same amount on all of them and still
     * answers "bay"; a gene that needs something to work on falls through to the
     * coat that gives it something.
     */
    public static final double ENOUGH_OF_BEST = 0.6;

    /**
     * <b>How much of the horse may be unpigmented before the picture stops being
     * a picture of the gene.</b>
     *
     * <p>{@link #measure} spends its one photograph on the combination that
     * repaints the most, and for KIT that is dominant white - a horse-shaped
     * area of bare template with no pattern in it at all. It is the loudest
     * thing the locus does and the least informative, so a combination this
     * blank is passed over for the next-loudest one that is not. (Owner's call:
     * "the KIT gene preview image should use one of the gene combinations that
     * is NOT completely white".)
     *
     * <p><b>Unpigmented, not pale, and not flat.</b> Two earlier versions of
     * this measured the finished pixels - distinct colours, then contrast - and
     * both were wrong, in opposite directions. Counting colours let a dominant
     * white through, because the composite is a multiply against the template's
     * hair shading and a "flat white" horse is a spread of a dozen near-whites.
     * Counting contrast then caught the dominant white and <b>caught a cremello,
     * a chestnut, a black and a grey with it</b> - genes for which a horse of one
     * colour is the honest picture and there is no better allele to move to.
     *
     * <p>Asking phase 1 instead separates the two exactly. A dominant white has
     * had its pigment <i>removed</i>; a cremello has had it <i>diluted</i>, and
     * still resolves through the gradient like any other coat. That is a
     * difference in kind, it is the difference this is trying to detect, and it
     * is a number the pipeline already computes.
     */
    public static final double BLANK_FRACTION = 0.9;

    /**
     * Total pigment at or below which a texel counts as unpigmented - the bare
     * white template shows through it. Deliberately above
     * {@code CoatTextureComposer}'s own transparency cutoff: this is asking "is
     * there a coat here" rather than "does this texel composite", and the very
     * palest a dilution stack leaves behind is an order of magnitude above it.
     */
    public static final float BLANK_PIGMENT = 0.02f;

    private CoatVisibility() {}

    /** A bake, and how far it moved off the plain horse underneath it. */
    public record Shown(int[] sheet, int moved) {}

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
        Shown shown = measure(gene, base, epi, template, luts);
        return shown == null ? null : shown.sheet();
    }

    /**
     * {@link #showing}, keeping the measurement the choice was made on so a
     * caller can compare one base coat against another.
     *
     * <h2>The loudest combination that is still a picture</h2>
     * Every allele's <b>homozygote</b> is a candidate, which is the honest full
     * expression and is what nearly every gene is photographed on. An allele
     * whose homozygote comes out <b>blank</b> - {@link #BLANK_FRACTION} of the
     * horse unpigmented - offers its <b>heterozygote with the wild type</b>
     * instead, and the loudest candidate of all of them wins.
     *
     * <p>That second half is KIT, and it is the whole reason it exists.
     * <i>Every</i> KIT variant is a white horse when homozygous - dominant
     * white, camarillo white, near-white or dead - so a homozygotes-only rule
     * can photograph nothing but a blank white horse, while the patterns the
     * locus is actually famous for are all heterozygous. (Owner's call: "the KIT
     * gene preview image should use one of the gene combinations that is NOT
     * completely white".) The two pools are ranked <b>together</b> rather than
     * in sequence, and that matters: KIT does have one non-blank homozygote -
     * {@code W20/W20}, which puts white on four pasterns and reads as a plain
     * bay - and preferring any homozygote over any heterozygote would have
     * settled for it over an eighteen-times louder pinto.
     *
     * <p>A blank combination is still kept as a fallback: dominant white is a
     * real outcome, and a locus with only that to show should still show it.
     * And a candidate has to clear {@link #MIN_TEXELS} to be considered at all,
     * without which the wild type - which moves nothing and is never blank -
     * would win by default and the gene would get no picture whatever.
     */
    public static Shown measure(Gene gene, Genotype base, Epigenome epi,
                                int[] template, LutSet luts) {
        int[] plain = CoatTextureComposer.compose(base, epi, Skin.ADULT, true, template, luts);

        if (gene.previewExpression() != null) {
            return declared(gene, base, epi, template, luts, plain);
        }

        Shown best = null;
        Shown blank = null;
        for (Allele variant : gene.alleles()) {
            Shown homozygote = bake(base, variant, variant, epi, template, luts, plain);
            if (homozygote.moved() < MIN_TEXELS) {
                continue;
            }
            if (!isBlank(base, variant, variant, epi)) {
                best = louder(best, homozygote);
                continue;
            }
            blank = louder(blank, homozygote);
            // Only worth baking a second horse for an allele whose full
            // expression came out blank - which for KIT is all of them.
            if (!variant.equals(gene.defaultAllele())
                    && !isBlank(base, variant, gene.defaultAllele(), epi)) {
                Shown het = bake(base, variant, gene.defaultAllele(), epi, template, luts, plain);
                if (het.moved() >= MIN_TEXELS) {
                    best = louder(best, het);
                }
            }
        }
        Shown pick = best != null ? best : blank;
        return pick != null && pick.moved() >= MIN_TEXELS ? pick : null;
    }

    /**
     * <b>The gene as its author said to photograph it</b> - the loudest
     * combination landing on {@link Gene#previewExpression()}, rather than the
     * loudest combination full stop.
     *
     * <p>Every pair is tried, not just the homozygotes: the reason a gene
     * declares an expression at all is usually that the one worth looking at is
     * heterozygous. Flametouched is the case in hand - {@code Ffm/Ffm} is a
     * whole-horse ember gradient and outshouts the {@code Ffm/n} flames the
     * gene is named for.
     */
    private static Shown declared(Gene gene, Genotype base, Epigenome epi,
                                  int[] template, LutSet luts, int[] plain) {
        String want = gene.previewExpression();
        Shown best = null;
        for (Allele a : gene.alleles()) {
            for (Allele b : gene.alleles()) {
                AllelePair pair = new AllelePair(a, b);
                if (!gene.canOccur(pair)) {
                    continue;
                }
                Expression shows = gene.expressionOf(pair);
                if (shows == null || !want.equals(shows.id())) {
                    continue;
                }
                Shown shot = bake(base, a, b, epi, template, luts, plain);
                if (shot.moved() >= MIN_TEXELS) {
                    best = louder(best, shot);
                }
            }
        }
        return best;
    }

    private static Shown bake(Genotype base, Allele a, Allele b, Epigenome epi,
                              int[] template, LutSet luts, int[] plain) {
        int[] sheet = CoatTextureComposer.compose(base.with(new AllelePair(a, b)), epi,
                Skin.ADULT, true, template, luts);
        return new Shown(sheet, repainted(sheet, plain));
    }

    private static Shown louder(Shown held, Shown candidate) {
        return held == null || candidate.moved() > held.moved() ? candidate : held;
    }

    /**
     * The <b>base coat this gene is worth being photographed on</b>: the first
     * one in {@link BaseCoats#all()}'s own order that comes within
     * {@link #ENOUGH_OF_BEST} of the loudest of them - or {@code null} if it
     * shows on none of them.
     *
     * <p>The order is what makes this useful rather than arbitrary: bay leads,
     * so every ordinary gene answers "bay" and nothing changes. Chestnut and the
     * white-marked coats sit after it, so a gene that needs red pigment
     * (mushroom, flaxen) or somebody else's white (fielded, voided, opalized)
     * falls through to one, and it falls through to the first one that gives it
     * enough to do rather than to a hand-picked favourite.
     */
    public static BaseCoats.BaseCoat firstShowing(Gene gene, Epigenome epi,
                                                  int[] template, LutSet luts) {
        if (gene.previewBase() != null) {
            BaseCoats.BaseCoat declared = declaredBase(gene);
            return measure(gene, declared.genotype(), epi, template, luts) == null ? null : declared;
        }
        List<BaseCoats.BaseCoat> coats = BaseCoats.all();
        int[] moved = new int[coats.size()];
        int best = 0;
        for (int i = 0; i < coats.size(); i++) {
            Shown shown = measure(gene, coats.get(i).genotype(), epi, template, luts);
            moved[i] = shown == null ? 0 : shown.moved();
            best = Math.max(best, moved[i]);
        }
        if (best < MIN_TEXELS) {
            return null;
        }
        for (int i = 0; i < coats.size(); i++) {
            if (moved[i] >= MIN_TEXELS && moved[i] >= best * ENOUGH_OF_BEST) {
                return coats.get(i);
            }
        }
        return null;
    }

    /**
     * The base coat {@link Gene#previewBase()} names, resolved here rather than
     * at parse time - {@link BaseCoats} builds its genotypes out of the gene
     * registry, which is mid-load while a spec file is being read.
     */
    public static BaseCoats.BaseCoat declaredBase(Gene gene) {
        BaseCoats.BaseCoat coat = BaseCoats.byKey(gene.previewBase());
        if (coat == null) {
            throw new IllegalArgumentException(gene.key() + ": preview.base '" + gene.previewBase()
                    + "' is not a base coat");
        }
        return coat;
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

    /**
     * Is this horse <b>essentially unpigmented</b> - {@link #BLANK_FRACTION} or
     * more of its mapped texels left under {@link #BLANK_PIGMENT} by phase 1?
     *
     * <p>A dominant white answers yes and so does a sabino-white; a cremello, a
     * chestnut, a black and a grey all answer no, because dilution is not
     * depigmentation. See {@link #BLANK_FRACTION} for why the difference has to
     * be read off phase 1 rather than off the finished pixels.
     */
    private static boolean isBlank(Genotype base, Allele a, Allele b, Epigenome epi) {
        PigmentField pigment = CoatTextureComposer.pigmentField(
                base.with(new AllelePair(a, b)), epi, Skin.ADULT, true);
        int[] counts = new int[2];      // {mapped, unpigmented}
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            counts[0]++;
            if (pigment.red(px, py) + pigment.black(px, py) <= BLANK_PIGMENT) {
                counts[1]++;
            }
        });
        return counts[0] > 0 && counts[1] >= counts[0] * BLANK_FRACTION;
    }
}
