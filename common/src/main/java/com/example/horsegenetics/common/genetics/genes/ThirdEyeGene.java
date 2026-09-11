package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.eye.EyeHue;
import com.example.horsegenetics.common.genetics.eye.EyeInk;
import com.example.horsegenetics.common.genetics.eye.EyeLocus;
import com.example.horsegenetics.common.genetics.eye.EyeRender;
import com.example.horsegenetics.common.genetics.eye.EyeSector;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A third eye in the middle of the forehead</b> - the one eye locus with no
 * side, and the only one that has to <i>invent</i> an eye rather than describe
 * one the horse already has.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code WT/*}</td><td>wild type - two eyes, like everything else</td></tr>
 *   <tr><td>{@code Lft/Lft}</td><td>an exact duplicate of the left eye</td></tr>
 *   <tr><td>{@code Rgt/Rgt}</td><td>an exact duplicate of the right eye</td></tr>
 *   <tr><td>{@code Mix/Mix}</td><td>each part taken from one side or the other, decided per horse</td></tr>
 *   <tr><td>{@code Def/Def}</td><td>every part decided per horse, from nothing</td></tr>
 * </table>
 *
 * <h2>Four alleles because there are four honest answers</h2>
 * A third eye has no locus of its own to take a colour from - there is no
 * "third iris colour" gene and there should not be, because that would be six
 * more loci for one rare allele. So the four alleles are the four ways of
 * answering "and what colour is <i>that</i> one":
 * <ul>
 *   <li>the two <b>imitations</b> copy an eye the horse already has, which is
 *       the outcome that reads as a third eye rather than as a decal;</li>
 *   <li><b>mix</b> takes each part - iris, sector, sector colour, sclera, and
 *       the two glows - from the left or the right eye independently, so a horse
 *       with two ordinary matching eyes gets a third ordinary one and a
 *       heterochromatic horse gets something genuinely new;</li>
 *   <li><b>defined</b> ignores both and builds an eye out of the epigenetics on
 *       the allele copy - every part of it, drawn from the same palettes the
 *       real loci offer, so a defined third eye is never something a horse could
 *       not have inherited.</li>
 * </ul>
 * Mix and defined are written on the <b>allele copy</b>, which is what makes
 * them heritable: a defined third eye breeds true to the eye and drifts slowly,
 * like every other epigenetic thing in the mod. Both {@link Expression.Builder#varies()}.
 *
 * <h2>Where it is drawn</h2>
 * The middle of the head's top face - the forehead. Not the head's front face,
 * which the muzzle box covers almost entirely: the head is
 * {@code 6 x 5 x 7} and the muzzle {@code 4 x 5 x 5} sits directly in front of
 * it at the same height, so a third eye painted there would be visible on a one
 * unit strip either side of the nose and nowhere else.
 *
 * <p><b>Unverified.</b> The forehead rect ({@code CoatRegions.thirdEyeRect}) is
 * derived from {@code HorseSkinGeometry}'s own box-UV tables rather than from a
 * render, and unlike the two real eyes there is nothing on the template at that
 * spot to check it against - the third eye is painted absolutely, not tinted,
 * because the forehead is plain white and there is no iris there to colour. It
 * is on {@code wiki/verification.html}.
 *
 * <p><b>Magical.</b> See {@code wiki/eye-colour.html}.
 */
public final class ThirdEyeGene extends AbstractEyeGene {

    /** How many founders in a hundred show each of the four. Very rare, deliberately. */
    private static final double EACH_PERCENT = 0.05;

    /** Which eye each part came from, under {@code Mix}. One category per part. */
    public static final String MIX_IRIS = "third_mix_iris";
    public static final String MIX_SECTOR = "third_mix_sector";
    public static final String MIX_SECTOR_COLOUR = "third_mix_sector_colour";
    public static final String MIX_SCLERA = "third_mix_sclera";
    public static final String MIX_GLOW_IRIS = "third_mix_glow_iris";
    public static final String MIX_GLOW_SCLERA = "third_mix_glow_sclera";

    /** What each part is, under {@code Def}. */
    public static final String DEF_IRIS = "third_iris";
    public static final String DEF_SECTOR = "third_sector";
    public static final String DEF_SECTOR_COLOUR = "third_sector_colour";
    public static final String DEF_SCLERA = "third_sclera";
    public static final String DEF_GLOW_IRIS = "third_glow_iris";
    public static final String DEF_GLOW_SCLERA = "third_glow_sclera";

    /** How often a {@code Def} third eye glows, per half. */
    public static final double DEFINED_GLOW_CHANCE = 0.25;

    /** The four non-wild alleles, in declaration order. */
    public enum Mode { LEFT_IMITATION, RIGHT_IMITATION, MIX, DEFINED }

    public ThirdEyeGene(int priority) {
        super(EyeLocus.THIRD_EYE, priority, "Third eye", false,
                new Allele(EyeLocus.THIRD_EYE.key(), 0, "WT", "Wild type (WT)"),
                Expression.wildType("Two eyes, like every other horse."),
                Expression.wildType("third-eye-carrier", "Third eye carrier",
                        "One copy, which shows nothing - all four variants are recessive to the "
                                + "wild type and to each other."),
                Expression.wildType("third-eye-mismatched", "Two different third eyes",
                        "Two different variants, which is no third eye. A horse grows one only by "
                                + "carrying the same answer twice."),
                variants());
    }

    private static List<Variant> variants() {
        String k = EyeLocus.THIRD_EYE.key();
        List<Variant> out = new ArrayList<>();
        out.add(new Variant(new Allele(k, 1, "Lft", "Left eye imitation (Lft)"),
                Expression.of("third-eye-left", "Third eye (left imitation)")
                        .describe("An eye in the middle of the forehead, identical to the horse's "
                                + "left eye in every respect - colour, sector, sclera and glow.")
                        .marker(),
                EACH_PERCENT));
        out.add(new Variant(new Allele(k, 2, "Rgt", "Right eye imitation (Rgt)"),
                Expression.of("third-eye-right", "Third eye (right imitation)")
                        .describe("The same, copied from the right eye instead - which is a "
                                + "different horse entirely if the two eyes differ.")
                        .marker(),
                EACH_PERCENT));
        out.add(new Variant(new Allele(k, 3, "Mix", "Mix (Mix)"),
                Expression.of("third-eye-mix", "Third eye (mixed)")
                        .describe("An eye assembled from both of the others - iris from one side, "
                                + "sclera from the other, and so on for each part independently. "
                                + "Which side each part came from is written on the allele copy, "
                                + "so it is inherited and it drifts.")
                        .varies().marker(),
                EACH_PERCENT));
        out.add(new Variant(new Allele(k, 4, "Def", "Defined (Def)"),
                Expression.of("third-eye-defined", "Third eye (defined)")
                        .describe("An eye that owes nothing to the other two: every part of it - "
                                + "iris colour, sector, sector colour, sclera, and whether either "
                                + "half glows - is written on the allele copy, drawn from the same "
                                + "palettes the real eye loci offer.")
                        .varies().marker(),
                EACH_PERCENT));
        return out;
    }

    @Override
    public String description() {
        return "An eye in the middle of the forehead. Four recessive answers to the question of "
                + "what colour it is, each needing a matched pair: copy the left eye, copy the "
                + "right eye, take each part from one side or the other, or invent the whole thing "
                + "from the numbers on the allele copy. The last two are written on the copy "
                + "rather than on the allele, so they are inherited and they drift.";
    }

    /** Which of the four this horse shows, or {@code null} for no third eye. */
    public Mode modeOf(AllelePair pair) {
        int i = shownIndex(pair);
        return i < 0 ? null : Mode.values()[i];
    }

    /**
     * The third eye this horse has, or {@code null}.
     *
     * @param pair  this locus's combination
     * @param right the horse's resolved right eye
     * @param left  the horse's resolved left eye
     * @param epi   this locus's expressed values - midpoints for a question
     *              asked about a genotype rather than about a horse
     */
    public EyeRender renderOf(AllelePair pair, EyeRender right, EyeRender left, EpiValues epi) {
        Mode mode = modeOf(pair);
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case LEFT_IMITATION -> left;
            case RIGHT_IMITATION -> right;
            case MIX -> mixed(right, left, epi);
            case DEFINED -> defined(epi);
        };
    }

    /** Each part from whichever side its own stored category names. */
    private static EyeRender mixed(EyeRender right, EyeRender left, EpiValues epi) {
        EyeRender irisFrom = pick(epi, MIX_IRIS, right, left);
        EyeRender sectorFrom = pick(epi, MIX_SECTOR, right, left);
        EyeRender sectorColourFrom = pick(epi, MIX_SECTOR_COLOUR, right, left);
        EyeRender scleraFrom = pick(epi, MIX_SCLERA, right, left);
        return new EyeRender(
                irisFrom.iris(),
                sectorFrom.sector(),
                sectorColourFrom.sectorInk(),
                pick(epi, MIX_GLOW_IRIS, right, left).glowIris(),
                scleraFrom.sclera(),
                pick(epi, MIX_GLOW_SCLERA, right, left).glowSclera());
    }

    private static EyeRender pick(EpiValues epi, String name, EyeRender right, EyeRender left) {
        return category(epi, name, 2) == 0 ? right : left;
    }

    /** Every part off the allele copy, from the real loci's own palettes. */
    private static EyeRender defined(EpiValues epi) {
        EyeHue iris = EyeHue.IRIS_PALETTE[category(epi, DEF_IRIS, EyeHue.IRIS_PALETTE.length)];
        EyeSector[] sectors = EyeSector.values();
        EyeSector sector = sectors[category(epi, DEF_SECTOR, sectors.length)];
        EyeHue sectorHue = EyeHue.SECTOR_PALETTE[
                category(epi, DEF_SECTOR_COLOUR, EyeHue.SECTOR_PALETTE.length)];
        EyeHue sclera = EyeHue.SCLERA_PALETTE[
                category(epi, DEF_SCLERA, EyeHue.SCLERA_PALETTE.length)];
        return new EyeRender(
                EyeInk.of(iris, epi),
                sector,
                EyeInk.of(sectorHue, epi),
                value(epi, DEF_GLOW_IRIS) < DEFINED_GLOW_CHANCE,
                EyeInk.of(sclera, epi),
                value(epi, DEF_GLOW_SCLERA) < DEFINED_GLOW_CHANCE);
    }

    /** A stored category, or the middle of its range when nothing is stored. */
    private static int category(EpiValues epi, String name, int count) {
        if (epi == null || epi.isEmpty() || epi.schema().indexOf(name) < 0) {
            return count / 2;
        }
        return Math.floorMod(epi.category(name), count);
    }

    private static double value(EpiValues epi, String name) {
        if (epi == null || epi.isEmpty() || epi.schema().indexOf(name) < 0) {
            return 0.5;
        }
        return epi.get(name);
    }

    /**
     * Both answers at once: the six {@code mix} choices and the six
     * {@code defined} ones, plus a chaos colour in case a defined eye lands on
     * it. Every copy carries all of them whatever allele it is - a schema is a
     * property of the gene, and a copy that changes from {@code Mix} to
     * {@code Def} down a line should not lose its numbers on the way.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                        EpiValue.category(MIX_IRIS, 2),
                        EpiValue.category(MIX_SECTOR, 2),
                        EpiValue.category(MIX_SECTOR_COLOUR, 2),
                        EpiValue.category(MIX_SCLERA, 2),
                        EpiValue.category(MIX_GLOW_IRIS, 2),
                        EpiValue.category(MIX_GLOW_SCLERA, 2),
                        EpiValue.category(DEF_IRIS, EyeHue.IRIS_PALETTE.length),
                        EpiValue.category(DEF_SECTOR, EyeSector.values().length),
                        EpiValue.category(DEF_SECTOR_COLOUR, EyeHue.SECTOR_PALETTE.length),
                        EpiValue.category(DEF_SCLERA, EyeHue.SCLERA_PALETTE.length),
                        EpiValue.uniform(DEF_GLOW_IRIS, 0, 1),
                        EpiValue.uniform(DEF_GLOW_SCLERA, 0, 1))
                .and(EyeHue.schema());
    }
}
