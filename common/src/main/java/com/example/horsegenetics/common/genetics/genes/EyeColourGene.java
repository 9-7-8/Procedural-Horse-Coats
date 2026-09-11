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
 * <b>What colour one iris is</b> - registered twice, once per eye, and the
 * locus every other gene in the mod that used to paint an eye now asks rather
 * than overrules.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code Brn/*}</td><td>wild type - a brown eye. Brown is dominant to everything here</td></tr>
 *   <tr><td>{@code Grn/Grn}</td><td>green</td></tr>
 *   <tr><td>{@code Gld/Gld}</td><td>gold - the shade tiger eye and champagne used to paint directly</td></tr>
 *   <tr><td>{@code DBl/DBl}, {@code MBl/MBl}, {@code LBl/LBl}</td><td>the three blues</td></tr>
 *   <tr><td>{@code Wht/Wht}</td><td>a white iris</td></tr>
 *   <tr><td>{@code Cha/Cha}</td><td>chaos - a colour off the allele copy, so no two are alike</td></tr>
 *   <tr><td>{@code Inv/Inv}</td><td>invisible - <b>no iris is painted at all</b></td></tr>
 *   <tr><td>{@code Grn/Gld} and every other mismatched pair</td><td>a wild type - brown</td></tr>
 * </table>
 *
 * <h2>Two loci, not one</h2>
 * The left and right irises are separate genes because they are separately
 * heritable: that is the only way complete heterochromia becomes something a
 * player can <i>breed</i> rather than something they wait for. The founder roll
 * makes the two usually agree - see {@link AbstractEyeGene} - so an odd-eyed
 * wild horse stays a find.
 *
 * <h2>Invisible is not a colour</h2>
 * {@code Inv/Inv} does not paint a transparent iris; it stops the iris being
 * <b>drawn</b>, so the coat that happens to be on the head shows through where
 * the eye would be. That has to happen before {@code CoatRegions.redrawEyes}
 * copies the template's eyes back over the finished coat, which is why the eye
 * phenotype is resolved ahead of that step and not in the overlay pass with
 * everything else. It overrides everything further down that eye's paint stack:
 * an invisible iris has no sector and no glow, because there is nothing there
 * to have them.
 *
 * <p>Natural. Chaos is the one outcome that {@link Expression.Builder#varies()}.
 * See {@code wiki/eye-colour.html}.
 */
public final class EyeColourGene extends AbstractEyeGene {

    /**
     * The hues this locus offers, and how many founders in a hundred show each.
     * Green and gold are the two a player will actually meet; invisible is the
     * rarest thing at the locus because a horse with no irises is the loudest.
     */
    private static final Object[][] VARIANTS = {
            {EyeHue.GREEN, 1.2, "A clear green iris - the colour the cream/pearl compound used to "
                    + "paint directly, and now the colour it asks for."},
            {EyeHue.GOLD, 1.2, "A warm gold, the colour of a hawk's eye. Tiger eye and champagne "
                    + "both request this rather than owning a shade of their own."},
            {EyeHue.DARK_BLUE, 0.6, "A deep blue, dark enough to read as almost black at distance "
                    + "and unmistakable close up."},
            {EyeHue.MID_BLUE, 0.6, "The pale, cool blue of a splashed white horse - which is exactly "
                    + "what the four white loci request when a horse is white enough to have it."},
            {EyeHue.LIGHT_BLUE, 0.4, "Blue washed almost to white. The palest iris that still reads "
                    + "as an iris at two texels."},
            {EyeHue.WHITE, 0.15, "A white iris in a white sclera - an eye that is all one colour, "
                    + "with only the antialiased rim to say where it ends."},
            {EyeHue.CHAOS, 0.08, "A colour written on the allele copy rather than on the allele, so "
                    + "no two chaos-eyed horses match and a line of them drifts."},
            {EyeHue.INVISIBLE, 0.05, "No iris is painted at all - the coat on the head shows straight "
                    + "through where the eye should be. Nothing further down this eye's stack "
                    + "shows either: no sector, no glow."},
    };

    private final List<EyeHue> hues = new ArrayList<>();

    public EyeColourGene(EyeLocus locus, int priority) {
        this(locus, priority, new Allele(locus.key(), 0, EyeHue.BROWN.token(), "Brown (Brn)"),
                variantsOf(locus));
    }

    private EyeColourGene(EyeLocus locus, int priority, Allele wild, List<Variant> variants) {
        super(locus, priority, side(locus) + " eye colour", true, wild,
                Expression.wildType("A brown eye - what nearly every horse has."),
                Expression.wildType("eye-colour-carrier", "Eye colour carrier",
                        "One copy of something other than brown, which shows nothing: every variant "
                                + "at this locus is recessive to brown."),
                Expression.wildType("eye-colour-mismatched", "Two different eye colours",
                        "Two different non-brown variants, which is a brown eye. Each is recessive "
                                + "to the other as well as to brown, so a horse shows one only by "
                                + "carrying the same one twice."),
                variants);
        for (Object[] row : VARIANTS) {
            hues.add((EyeHue) row[0]);
        }
    }

    private static String side(EyeLocus locus) {
        return locus == EyeLocus.IRIS_LEFT ? "Left" : "Right";
    }

    private static List<Variant> variantsOf(EyeLocus locus) {
        List<Variant> out = new ArrayList<>();
        int order = 1;
        for (Object[] row : VARIANTS) {
            EyeHue hue = (EyeHue) row[0];
            double percent = (Double) row[1];
            String description = (String) row[2];
            Expression.Builder e = Expression
                    .of("eye-" + hue.name().toLowerCase().replace('_', '-')
                            + "-" + side(locus).toLowerCase(), hue.label() + " eye")
                    .describe(description);
            if (hue.varies()) {
                e = e.varies();
            }
            out.add(new Variant(
                    new Allele(locus.key(), order++, hue.token(), hue.label() + " (" + hue.token() + ")"),
                    e.marker(), percent));
        }
        return out;
    }

    @Override
    public String description() {
        return "What colour this horse's " + side(locus()).toLowerCase() + " iris is. Brown is "
                + "dominant to all eight variants and to a horse carrying two different ones, so "
                + "green, gold, the three blues, white, chaos and invisible each take a matched "
                + "pair. The two eyes are separate loci, which is what makes complete "
                + "heterochromia something you can breed for rather than wait for - but a wild "
                + "horse's second eye usually copies its first. Cream, champagne, tiger eye and "
                + "the white loci do not paint an eye: they ask this locus for one, and the "
                + "allele is written onto the foal at birth.";
    }

    /** The hue this horse's iris is on this side. Never {@code null}. */
    public EyeHue hueOf(AllelePair pair) {
        int i = shownIndex(pair);
        return i < 0 ? EyeHue.BROWN : hues.get(i);
    }

    /**
     * The chaos colour, carried whether or not this horse is chaos-eyed - a
     * schema is a property of the gene, not of the horse.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EyeHue.schema());
    }
}
