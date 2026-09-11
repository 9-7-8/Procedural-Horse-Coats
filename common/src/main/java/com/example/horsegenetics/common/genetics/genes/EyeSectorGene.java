package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.eye.EyeLocus;
import com.example.horsegenetics.common.genetics.eye.EyeSector;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Which part of one iris is a second colour</b> - sectoral heterochromia as
 * ten named alleles, registered twice, once per eye.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code WT/*}</td><td>wild type - one colour, the whole iris</td></tr>
 *   <tr><td>{@code Top/Top}, {@code Bot/Bot}, {@code Lf/Lf}, {@code Rt/Rt}</td><td>a half</td></tr>
 *   <tr><td>{@code UL/UL}, {@code UR/UR}, {@code LL/LL}, {@code LR/LR}</td><td>one corner</td></tr>
 *   <tr><td>{@code URLL/URLL}, {@code ULLR/ULLR}</td><td>the two diagonals</td></tr>
 * </table>
 *
 * <h2>It says where, not what</h2>
 * The colour that lands in the sector is
 * {@link EyeSectorColourGene}'s, and the two are separate loci for the same
 * reason left and right are: each doubling of the outcome space is a doubling of
 * what a breeder can aim at. A horse wild type here shows nothing whatever its
 * sector-colour locus says, which is the one asymmetry between the pair - the
 * shape gates the colour, not the other way round.
 *
 * <h2>The sectors are named from the horse's side</h2>
 * The two eyes' faces are mirrored on the coat sheet, so the raw quadrant bits
 * are not the same corner on both. {@link EyeSector#maskFor} un-mirrors them, so
 * that a horse bred to {@code UL/UL} on both sides has the sector in the same
 * place on both sides. See {@link EyeSector} - and note the flip is derived from
 * the sheet's own documented mirroring rather than from a render, so it is on
 * {@code wiki/verification.html}.
 *
 * <h2>Where the old sectoral heterochromia went</h2>
 * A blue wedge in a splashed white horse's eye used to be a roll off
 * {@code EyeSpread}, read from whichever white locus won the eye claim. It is
 * still that roll - the white loci keep the 62 / 22 / 16 distribution - but the
 * roll now comes out as a <b>request for these alleles</b> rather than as a
 * mask painted on the spot. A splash-bred wedge is therefore inheritable, which
 * it never was before.
 *
 * <p>Natural, deterministic. See {@code wiki/eye-colour.html}.
 */
public final class EyeSectorGene extends AbstractEyeGene {

    /** How many founders in a hundred show each sector. Flat: no shape is special. */
    private static final double EACH_PERCENT = 0.12;

    private final List<EyeSector> sectors = new ArrayList<>();

    public EyeSectorGene(EyeLocus locus, int priority) {
        super(locus, priority, side(locus) + " eye sector", true,
                new Allele(locus.key(), 0, EyeSector.WILD.token(), "Wild type (WT)"),
                Expression.wildType("One colour, the whole iris."),
                Expression.wildType("eye-sector-carrier", "Eye sector carrier",
                        "One copy of a sector allele, which shows nothing - every sector here is "
                                + "recessive to the wild type and to the other nine."),
                Expression.wildType("eye-sector-mismatched", "Two different sectors",
                        "Two different sector alleles, which is no sector at all. A horse shows a "
                                + "shape only by carrying the same shape twice."),
                variantsOf(locus));
        for (EyeSector s : EyeSector.sectors()) {
            sectors.add(s);
        }
    }

    private static String side(EyeLocus locus) {
        return locus == EyeLocus.SECTOR_LEFT ? "Left" : "Right";
    }

    private static List<Variant> variantsOf(EyeLocus locus) {
        List<Variant> out = new ArrayList<>();
        int order = 1;
        for (EyeSector s : EyeSector.sectors()) {
            out.add(new Variant(
                    new Allele(locus.key(), order++, s.token(), s.label() + " (" + s.token() + ")"),
                    Expression.of("eye-sector-" + s.name().toLowerCase().replace('_', '-')
                                    + "-" + side(locus).toLowerCase(),
                            s.label() + " sector")
                            .describe("The " + s.label().toLowerCase() + " of this iris takes the "
                                    + "sector colour, and the rest of it keeps the eye's own. "
                                    + "An iris is four texels, so that is as fine as a sector gets.")
                            .marker(),
                    EACH_PERCENT));
        }
        return out;
    }

    @Override
    public String description() {
        return "Which part of this horse's " + side(locus()).toLowerCase() + " iris is a second "
                + "colour - sectoral heterochromia, as ten named alleles: four halves, four "
                + "corners and the two diagonals. All ten are recessive to the wild type and to "
                + "each other. It says where, not what: the colour is the sector-colour locus's, "
                + "and a horse wild type here shows nothing whatever that locus says.";
    }

    /** The sector this horse shows on this side - {@link EyeSector#WILD} for none. */
    public EyeSector sectorOf(AllelePair pair) {
        int i = shownIndex(pair);
        return i < 0 ? EyeSector.WILD : sectors.get(i);
    }
}
