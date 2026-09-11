package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.eye.EyeHue;
import com.example.horsegenetics.common.genetics.eye.EyeLocus;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>What colour the white of one eye is</b> - registered twice, once per eye.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code Wht/*}</td><td>wild type - the white every horse has</td></tr>
 *   <tr><td>{@code Blk/Blk}</td><td>black sclera: the eye reads as one dark block with an iris-coloured middle</td></tr>
 *   <tr><td>{@code Inv/Inv}</td><td>invisible: the sclera texels are not painted, so the coat shows through</td></tr>
 *   <tr><td>{@code Cha/Cha}</td><td>chaos: a colour off the allele copy</td></tr>
 * </table>
 *
 * <h2>The sclera is the light texels, not the iris</h2>
 * On the coat sheet an adult eye is a block of near-white beside a block of
 * pure black, and which is which is worked out from the template rather than
 * hard-coded, because the two eyes' faces are mirrored. So this locus is the
 * exact complement of {@link EyeColourGene}: it weights each texel by how
 * <b>bright</b> it already is, where the iris weights by how dark. A foal has no
 * sclera at all on its template - its eye is the pupil block alone - so this
 * locus shows nothing on a foal and everything on the same horse grown up.
 *
 * <p>An invisible sclera, like an invisible iris, is handled before the
 * template's eyes are copied back over the coat: there is no such thing as
 * painting a hole in the overlay pass.
 *
 * <p>Natural. Chaos is the one outcome that varies per horse. See
 * {@code wiki/eye-colour.html}.
 */
public final class EyeScleraGene extends AbstractEyeGene {

    private static final Object[][] VARIANTS = {
            {EyeHue.BLACK, 0.30, "The white of the eye is black, so the eye reads as one dark patch "
                    + "with the iris colour buried in the middle of it. On a pale horse it is the "
                    + "loudest thing about the face."},
            {EyeHue.INVISIBLE, 0.10, "The sclera is not painted at all - the coat on the head runs "
                    + "straight through it, and the iris floats on the horse's own colour with "
                    + "nothing round it."},
            {EyeHue.CHAOS, 0.06, "A colour written on the allele copy, so no two horses have the "
                    + "same whites and a line of them drifts."},
    };

    private final List<EyeHue> hues = new ArrayList<>();

    public EyeScleraGene(EyeLocus locus, int priority) {
        super(locus, priority, side(locus) + " eye sclera", true,
                new Allele(locus.key(), 0, EyeHue.WHITE.token(), "White (Wht)"),
                Expression.wildType("The ordinary white of the eye."),
                Expression.wildType("eye-sclera-carrier", "Sclera colour carrier",
                        "One copy of a variant, which shows nothing - all three are recessive to "
                                + "white and to each other."),
                Expression.wildType("eye-sclera-mismatched", "Two different sclera colours",
                        "Two different variants, which is an ordinary white eye."),
                variantsOf(locus));
        for (Object[] row : VARIANTS) {
            hues.add((EyeHue) row[0]);
        }
    }

    private static String side(EyeLocus locus) {
        return locus == EyeLocus.SCLERA_LEFT ? "Left" : "Right";
    }

    private static List<Variant> variantsOf(EyeLocus locus) {
        List<Variant> out = new ArrayList<>();
        int order = 1;
        for (Object[] row : VARIANTS) {
            EyeHue hue = (EyeHue) row[0];
            Expression.Builder e = Expression
                    .of("eye-sclera-" + hue.name().toLowerCase().replace('_', '-')
                            + "-" + side(locus).toLowerCase(), hue.label() + " sclera")
                    .describe((String) row[2]);
            if (hue.varies()) {
                e = e.varies();
            }
            out.add(new Variant(
                    new Allele(locus.key(), order++, hue.token(), hue.label() + " (" + hue.token() + ")"),
                    e.marker(), (Double) row[1]));
        }
        return out;
    }

    @Override
    public String description() {
        return "What colour the white of this horse's " + side(locus()).toLowerCase() + " eye is. "
                + "White is dominant; black, chaos and invisible each take a matched pair. A black "
                + "sclera turns the eye into one dark patch with the iris buried in it, and an "
                + "invisible one leaves the iris floating on the horse's own coat. It shows "
                + "nothing on a foal, whose template has no white round the pupil at all.";
    }

    /** The hue of this eye's sclera. Never {@code null}; {@link EyeHue#WHITE} by default. */
    public EyeHue hueOf(AllelePair pair) {
        int i = shownIndex(pair);
        return i < 0 ? EyeHue.WHITE : hues.get(i);
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EyeHue.schema());
    }
}
