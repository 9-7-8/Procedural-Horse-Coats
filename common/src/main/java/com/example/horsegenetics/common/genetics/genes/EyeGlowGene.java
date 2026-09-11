package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.eye.EyeLocus;

import java.util.List;

/**
 * <b>Does one half of one eye render full-bright?</b> - four loci off one class:
 * a glowing iris and a glowing sclera, left and right.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code WT/*}</td><td>wild type - no glow</td></tr>
 *   <tr><td>{@code Glo/Glo}</td><td>that half of that eye renders at full brightness</td></tr>
 * </table>
 *
 * <h2>It has no colour of its own, on purpose</h2>
 * A glow here is the eye it is already: the iris glows in whatever colour
 * {@link EyeColourGene} settled, the sclera in whatever
 * {@link EyeScleraGene} settled. That is the whole point of separating them
 * from the colour loci - a gold glowing eye and a chaos-coloured glowing eye are
 * the <b>same</b> allele here on two different horses, so a breeder who has
 * found the glow can then go and change what colour it glows.
 *
 * <p>Glowing nothing is nothing: an {@code Inv} iris or sclera has no texels to
 * light, so the glow simply does not show. It is still carried and still
 * inherited.
 *
 * <h2>Why iris and sclera are separate loci</h2>
 * They are very different faces. A glowing iris is a lamp; a glowing sclera
 * with a dark hole in the middle of it is a dhampir. The dhampir gene has
 * lit scleras hard-coded since before these loci existed, and still does - see
 * {@code wiki/eye-colour.html} for which of the magical genes overrule the eye
 * loci and why they are allowed to.
 *
 * <p><b>Magical.</b> Nothing in life glows, so these four sit in the magical
 * band whatever the eight colour loci beside them do.
 */
public final class EyeGlowGene extends AbstractEyeGene {

    /** How many founders in a hundred glow here. Rare: it is a loud thing to meet by accident. */
    public static final double EXPRESS_PERCENT = 0.18;

    private final Allele glowing;

    public EyeGlowGene(EyeLocus locus, int priority) {
        super(locus, priority, title(locus), false,
                new Allele(locus.key(), 0, "WT", "Wild type (WT)"),
                Expression.wildType("No glow."),
                Expression.wildType("eye-glow-carrier", "Glow carrier",
                        "One copy, which shows nothing. Two carriers bred together is the only way "
                                + "a glowing eye appears."),
                null,
                List.of(new Variant(
                        new Allele(locus.key(), 1, "Glo", "Glowing (Glo)"),
                        Expression.of("eye-glow-" + slug(locus), title(locus))
                                .describe(iris(locus)
                                        ? "This iris renders at full brightness, in whatever colour "
                                        + "the iris already is. An invisible iris has nothing to "
                                        + "light, and shows no glow."
                                        : "The white of this eye renders at full brightness, in "
                                        + "whatever colour the sclera already is - a lit ring with "
                                        + "the iris a hole in the middle of it.")
                                .marker(),
                        EXPRESS_PERCENT)));
        this.glowing = alleles().get(1);
    }

    private static boolean iris(EyeLocus locus) {
        return locus == EyeLocus.GLOW_IRIS_LEFT || locus == EyeLocus.GLOW_IRIS_RIGHT;
    }

    private static boolean left(EyeLocus locus) {
        return locus == EyeLocus.GLOW_IRIS_LEFT || locus == EyeLocus.GLOW_SCLERA_LEFT;
    }

    private static String title(EyeLocus locus) {
        return (left(locus) ? "Left" : "Right") + " glowing " + (iris(locus) ? "iris" : "sclera");
    }

    private static String slug(EyeLocus locus) {
        return (iris(locus) ? "iris-" : "sclera-") + (left(locus) ? "left" : "right");
    }

    @Override
    public String description() {
        return "Whether this horse's " + (left(locus()) ? "left" : "right")
                + (iris(locus()) ? " iris" : " sclera")
                + " renders at full brightness. Recessive, and it has no colour of its own: it "
                + "lights whatever that half of the eye already is, so the same allele is a gold "
                + "lamp on one horse and a chaos-coloured one on the next. Nothing to light is "
                + "no glow - an invisible half carries it and shows none of it.";
    }

    /** Does this horse glow here? */
    public boolean glows(AllelePair pair) {
        return pair.homozygousFor(glowing);
    }
}
