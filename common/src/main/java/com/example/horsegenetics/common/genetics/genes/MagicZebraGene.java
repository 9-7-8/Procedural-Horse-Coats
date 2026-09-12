package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ZebraStripes;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.List;

/**
 * <b>Magic zebra</b> ({@code horsegenetics.magic_zebra}) - a <b>magical</b>
 * gene.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Mzeb/n}</td><td>{@code dusky} - the same bands, darkened rather than blacked</td></tr>
 *   <tr><td>{@code Mzeb/Mzeb}</td><td>{@code zebra} - hard black bands over any coat at all</td></tr>
 * </table>
 *
 * <h2>The same map as the natural gene, in the opposite direction</h2>
 * {@link NaturalZebraGene} is the real-world locus: it runs in phase 1 and
 * <b>takes pigment away</b> between the bands, so the horse's own colour
 * survives as the stripes. This one is invented, runs in phase 3, and
 * <b>adds black</b> on the bands themselves - which is the only way to stripe a
 * cremello or a dominant white, because those horses have no pigment left to
 * take.
 *
 * <p>Both read the same {@link ZebraStripes} body map, which is why the field
 * returns the <i>dark</i> band coverage rather than the pale one: this gene
 * blackens {@code coverage}, the natural gene whitens {@code 1 - coverage}, and
 * the shape they draw is identical. A horse can carry both and show both.
 *
 * <p><b>It subtracts {@value #STRIPE_PERCENT}%</b> from all three channels. That
 * is deliberate overkill and is the whole point of the unclamped signed
 * accumulator: the largest a resolved channel can be is 100%, so a stripe lands
 * hard on 0 and reads black over <i>any</i> coat - a cremello, a chestnut, a
 * grey - without this gene needing to know what else the horse carries. It also
 * raises opacity, so the stripes show on a dominant-white horse too.
 *
 * <h2>Codominant, like the natural locus</h2>
 * One {@code Mzeb} copy used to draw the whole pattern, which left the locus
 * with nothing between a plain horse and a fully striped one. It is codominant
 * now, the same way {@link NaturalZebraGene} is and for the same reason: a
 * carrier a player can <i>see</i> is a carrier they can breed from.
 *
 * <p>There is <b>one painter</b> and it takes a strength, so the two outcomes
 * cannot drift apart - a dusky parent and its black-striped foal wear the same
 * bands in the same places. Two copies pass {@code 1.0} and are therefore
 * byte-for-byte what this gene has always drawn; one copy passes
 * {@value #SHADOW_STRENGTH}, which scales both the subtraction and the opacity.
 *
 * <p><b>The heterozygote is the one outcome that reads differently on different
 * coats</b>, and that is a consequence of the overkill rather than a bug in it.
 * A fraction of a deliberately-too-large number is an ordinary-sized number, so
 * a dusky band is a real darkening of whatever was underneath: clearly visible
 * on a cremello or a palomino, and close to invisible on a black horse, which
 * is what a shadow on a black horse looks like. The homozygote is the outcome
 * that promises to read on <i>any</i> coat, and it still does.
 *
 * <p><b>Non-deterministic.</b> Five knobs come off the expressing {@code Mzeb}
 * copy, in this order: {@code nextLong()} (the band field's seed), then
 * {@code nextFloat()} for band <b>spacing</b>, <b>width</b>, how far the bands
 * <b>bend</b>, and how far down the legs the rings <b>reach</b>. A foal that
 * inherits the copy inherits the pattern.
 *
 * <p>Founder frequency {@code 1/}{@value #WILD_MZEB_ONE_IN} per allele.
 */
public final class MagicZebraGene implements Gene {

    public static final String KEY = "horsegenetics.magic_zebra";
    public static final int WILD_MZEB_ONE_IN = 100;

    /** Per channel, as a percentage of full scale. Negative - stripes remove colour. */
    public static final int STRIPE_PERCENT = -200;

    /**
     * How much of the full subtraction <i>and</i> of the opacity one copy gets.
     * Not a fraction picked for its own sake: {@link #STRIPE_PERCENT} is sized
     * to floor any channel from anywhere, so the useful range for a band that
     * must <b>not</b> floor is well under a quarter of it.
     */
    public static final double SHADOW_STRENGTH = 0.09;

    // Body units (1 = 1/16 block); the adult barrel is 22 long, a foal's 14.
    private static final double SPACING_MIN = 2.0;
    private static final double SPACING_RANGE = 1.8;
    /**
     * Narrower than the natural gene's, and deliberately. This one <i>adds</i>
     * black on the band, so at an even duty the horse simply reads as a black
     * horse; the natural gene takes colour out of the gap, where an even duty is
     * exactly right.
     */
    private static final double WIDTH_MIN = 0.34;
    private static final double WIDTH_RANGE = 0.16;
    private static final double BEND_MIN = 0.4;
    private static final double BEND_RANGE = 1.0;

    /** How far down each leg the rings reach, as a fraction of the leg. */
    private static final double REACH_MIN = 0.45;
    private static final double REACH_RANGE = 0.55;

    /** Half-width of the dorsal stripe, in body units. */
    private static final double DORSAL_HALF_WIDTH = 1.3;

    public final Allele Mzeb = new Allele(KEY, 0, "Mzeb", "Magic zebra (Mzeb)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Mzeb, n);

    private final Expression WILD = Expression.wildType("No stripes.");

    private final Expression DUSKY = Expression.of("dusky", "Dusky-banded")
            .describe("One copy darkens the zebra map instead of blacking it, so the horse wears the "
                    + "full pattern as dusky bands a few shades down from its own colour. Obvious on a "
                    + "pale coat, subtle on a dark one - and visible enough to breed from, which is "
                    + "the point of it.")
            .varies()
            .tint(paint(SHADOW_STRENGTH));

    private final Expression ZEBRA = Expression.of("zebra", "Magic zebra")
            .describe("A zebra's own stripe map painted in hard black: vertical bands off the spine, "
                    + "arcs round the hip, rings down the legs, narrow bands on the neck and face, a "
                    + "black dorsal stripe and muzzle, and an unstriped belly. Per-horse in spacing, "
                    + "width, bend and leg reach. They read black over any coat at all, including a "
                    + "cremello or a dominant white.")
            .varies()
            .tint(paint(1.0));

    private final List<Expression> expressions = List.of(WILD, DUSKY, ZEBRA);

    private final FounderTable founders = FounderTable.hardyWeinberg(Mzeb, n, 1.0 / WILD_MZEB_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic zebra"; }
    @Override public int priority() { return 120; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (!pair.has(Mzeb)) {
            return WILD;
        }
        return pair.has(n) ? DUSKY : ZEBRA;
    }

    /** Does this combination band the coat at all - one copy or two? */
    public boolean isZebra(AllelePair pair) {
        return pair.has(Mzeb);
    }

    /** The stripe field and its four shape numbers - spacing, width, bend, reach. */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.seed("seed"),
                EpiValue.uniform("spacing", SPACING_MIN, SPACING_MIN + SPACING_RANGE),
                EpiValue.uniform("width", WIDTH_MIN, WIDTH_MIN + WIDTH_RANGE),
                EpiValue.uniform("bend", BEND_MIN, BEND_MIN + BEND_RANGE),
                EpiValue.uniform("reach", REACH_MIN, REACH_MIN + REACH_RANGE));
    }

    /**
     * One painter for both outcomes, differing only in how hard it subtracts -
     * which is what codominance means here, and the reason a dusky horse and a
     * black-striped one cannot end up wearing different bands.
     *
     * <p>{@code strength} scales the subtraction <b>and</b> the opacity
     * together. Scaling only the colour would leave a dusky band at full opacity
     * over the white template, which on a dominant white is a grey stripe as
     * strong as the homozygote's black one - the opposite of subtle.
     *
     * <p>Both outcomes read the same five stored numbers off the expressing copy,
     * so the pattern is the horse's and the strength is its genotype's.
     */
    private static Expression.Colour paint(double strength) {
        return (ctx, coat, accumulated) -> {
            EpiValues epi = ctx.epigeneticsFor(KEY);
            ZebraStripes.Pattern pat = new ZebraStripes.Pattern(
                    epi.seed("seed"),
                    epi.get("spacing"),
                    epi.get("width"),
                    epi.get("bend"),
                    epi.get("reach"),
                    DORSAL_HALF_WIDTH);

            Skin skin = ctx.skin();
            ColorField delta = ColorField.deltaLike(accumulated);
            HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
                double c = ZebraStripes.coverage(skin, part, point, pat) * strength;
                if (c <= 0) {
                    return;
                }
                int amount = (int) Math.round(255.0 * STRIPE_PERCENT / 100.0 * c);
                delta.add(px, py, amount, amount, amount);
                delta.addOpacity(px, py, (int) Math.round(255.0 * c));
            });
            return delta;
        };
    }
}
