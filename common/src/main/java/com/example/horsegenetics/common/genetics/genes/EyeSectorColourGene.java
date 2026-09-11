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
 * <b>What colour one iris's sector is</b> - the companion to
 * {@link EyeSectorGene}, registered twice, once per eye.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code MBl/*}</td><td>wild type - mid blue, the colour a splashed eye already was</td></tr>
 *   <tr><td>{@code Grn/Grn} … {@code Cha/Cha}</td><td>the other seven, each recessive to mid blue and to each other</td></tr>
 * </table>
 *
 * <h2>It shows nothing on its own</h2>
 * The sector locus gates this one: a horse whose {@link EyeSectorGene} is wild
 * type has no sector for a colour to land in, and every combination here is
 * then invisible. That is deliberate and it is the reason the wild type is
 * <b>mid blue</b> rather than brown - the commonest sector in the world is the
 * blue wedge a white locus asks for, so the locus's default should be the
 * colour that wedge already is, and a horse that acquires a sector without
 * being asked about the colour gets the one people expect.
 *
 * <p>There is no {@code Inv} allele here. An invisible sector is an iris with a
 * hole in it, which is not a thing the eye model has a meaning for; a horse that
 * wants no iris at all asks {@link EyeColourGene} for {@link EyeHue#INVISIBLE},
 * which takes the sector with it.
 *
 * <p>Natural. Chaos is the one outcome that varies per horse. See
 * {@code wiki/eye-colour.html}.
 */
public final class EyeSectorColourGene extends AbstractEyeGene {

    /**
     * The hues, and how many founders in a hundred carry each as a matched pair.
     * Low across the board: almost every horse that has a sector at all has a
     * blue one, because almost every horse that has a sector was asked for one
     * by a white locus.
     */
    private static final Object[][] VARIANTS = {
            {EyeHue.GREEN, 0.20},
            {EyeHue.GOLD, 0.20},
            {EyeHue.DARK_BLUE, 0.15},
            {EyeHue.BROWN, 0.15},
            {EyeHue.LIGHT_BLUE, 0.12},
            {EyeHue.WHITE, 0.08},
            {EyeHue.CHAOS, 0.05},
    };

    private final List<EyeHue> hues = new ArrayList<>();

    public EyeSectorColourGene(EyeLocus locus, int priority) {
        super(locus, priority, side(locus) + " eye sector colour", true,
                new Allele(locus.key(), 0, EyeHue.MID_BLUE.token(), "Mid blue (MBl)"),
                Expression.wildType("A mid-blue sector, if this eye has a sector at all."),
                Expression.wildType("eye-sector-colour-carrier", "Sector colour carrier",
                        "One copy of something other than mid blue, which shows nothing: every "
                                + "variant here is recessive to it."),
                Expression.wildType("eye-sector-colour-mismatched", "Two different sector colours",
                        "Two different non-blue variants, which is a mid-blue sector. Each is "
                                + "recessive to the other."),
                variantsOf(locus));
        for (Object[] row : VARIANTS) {
            hues.add((EyeHue) row[0]);
        }
    }

    private static String side(EyeLocus locus) {
        return locus == EyeLocus.SECTOR_COLOUR_LEFT ? "Left" : "Right";
    }

    private static List<Variant> variantsOf(EyeLocus locus) {
        List<Variant> out = new ArrayList<>();
        int order = 1;
        for (Object[] row : VARIANTS) {
            EyeHue hue = (EyeHue) row[0];
            Expression.Builder e = Expression
                    .of("eye-sector-" + hue.name().toLowerCase().replace('_', '-')
                            + "-" + side(locus).toLowerCase(), hue.label() + " sector")
                    .describe("This eye's sector, wherever the sector locus put it, comes out "
                            + hue.label().toLowerCase() + " instead of blue. Invisible on a horse "
                            + "whose sector locus is wild type, which is almost all of them.");
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
        return "What colour the sector in this horse's " + side(locus()).toLowerCase() + " iris "
                + "is, if it has one. Mid blue is dominant - the commonest sector in the world is "
                + "the blue wedge a white locus asks for, so the default is the colour that wedge "
                + "already is - and the other seven each take a matched pair. Completely "
                + "invisible on a horse whose sector locus is wild type, which is almost all of "
                + "them.";
    }

    /** The hue this eye's sector is. Never {@code null}; {@link EyeHue#MID_BLUE} by default. */
    public EyeHue hueOf(AllelePair pair) {
        int i = shownIndex(pair);
        return i < 0 ? EyeHue.MID_BLUE : hues.get(i);
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EyeHue.schema());
    }
}
