package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BodyStripes;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

/**
 * <b>Magic zebra</b> ({@code horsegenetics.magic_zebra}) - a <b>magical</b>
 * gene.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Mzeb/n}, {@code Mzeb/Mzeb}</td><td>{@code zebra} - black stripes hung from the topline</td></tr>
 * </table>
 *
 * <p><b>Not the natural zebra gene.</b> A real-world zebra-striping locus is a
 * separate, later, <i>natural</i> gene; this one is invented, runs in phase 3,
 * and paints over whatever the melanin genes produced rather than restricting
 * pigment.
 *
 * <p><b>It subtracts {@value #STRIPE_PERCENT}%</b> from all three channels. That
 * is deliberate overkill and is the whole point of the unclamped signed
 * accumulator: the largest a resolved channel can be is 100%, so a stripe lands
 * hard on 0 and reads black over <i>any</i> coat - a cremello, a chestnut, a
 * grey - without this gene needing to know what else the horse carries. It also
 * raises opacity, so the stripes show on a dominant-white horse too.
 *
 * <p><b>Non-deterministic.</b> Five knobs come off the expressing {@code Mzeb}
 * copy, in this order: {@code nextLong()} (the stripe field's seed), then
 * {@code nextFloat()} for stripe <b>spacing</b>, <b>width</b>, how far the
 * stripes <b>bend</b>, and how far down the horse they <b>reach</b>. A foal
 * that inherits the copy inherits the pattern.
 *
 * <p>Founder frequency {@code 1/}{@value #WILD_MZEB_ONE_IN} per allele.
 */
public final class MagicZebraGene implements Gene {

    public static final String KEY = "horsegenetics.magic_zebra";
    public static final int WILD_MZEB_ONE_IN = 100;

    /** Per channel, as a percentage of full scale. Negative - stripes remove colour. */
    public static final int STRIPE_PERCENT = -200;

    // Body units (1 = 1/16 block); the adult barrel is 22 long, a foal's 14.
    private static final double SPACING_MIN = 2.2;
    private static final double SPACING_RANGE = 2.0;
    private static final double WIDTH_MIN = 0.32;
    private static final double WIDTH_RANGE = 0.24;
    private static final double BEND_MIN = 0.6;
    private static final double BEND_RANGE = 1.6;

    /** How far below the topline the stripes die out, as a fraction of the drop to the hooves. */
    private static final double REACH_MIN = 0.35;
    private static final double REACH_RANGE = 0.60;
    /** The fraction of the reach spent fading out, so stripes don't stop on a line. */
    private static final double REACH_FADE = 0.25;

    public final Allele Mzeb = new Allele(KEY, 0, "Mzeb", "Magic zebra (Mzeb)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Mzeb, n);

    private final Expression WILD = Expression.wildType("No stripes.");

    private final Expression ZEBRA = Expression.of("zebra", "Magic zebra")
            .describe("Hard black stripes hung from the topline and reaching down the sides, per-horse "
                    + "in spacing, width, bend and reach. They read black over any coat at all, "
                    + "including a cremello or a dominant white.")
            .varies()
            .tint(MagicZebraGene::paintStripes);

    private final List<Expression> expressions = List.of(WILD, ZEBRA);

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
        return pair.has(Mzeb) ? ZEBRA : WILD;
    }

    public boolean isZebra(AllelePair pair) {
        return pair.has(Mzeb);
    }

    private static ColorField paintStripes(
            com.example.horsegenetics.common.coat.pattern.CoatBuildContext ctx,
            com.example.horsegenetics.common.coat.pattern.PigmentView coat,
            com.example.horsegenetics.common.coat.pattern.ColorView accumulated) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double spacing = SPACING_MIN + epi.nextFloat() * SPACING_RANGE;
        double width = WIDTH_MIN + epi.nextFloat() * WIDTH_RANGE;
        double bend = BEND_MIN + epi.nextFloat() * BEND_RANGE;
        double reach = REACH_MIN + epi.nextFloat() * REACH_RANGE;

        Skin skin = ctx.skin();
        // The topline is the back, not the ear tips: everything above it - head,
        // neck, mane, ears - is inside the stripes at full strength.
        double topline = HorseSkinGeometry.bounds(skin, Part.BODY).yMax();
        double hooves = HorseSkinGeometry.bodyBounds(skin).yMin();
        double drop = topline - hooves;

        ColorField delta = ColorField.deltaLike(accumulated);
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double below = (topline - point.y()) / drop;
            double vertical = 1.0 - BodyStripes.smoothstep(reach * (1 - REACH_FADE), reach, below);
            if (vertical <= 0) {
                return;
            }
            double c = BodyStripes.coverage(seed, point.x(), point.y(), point.z(), spacing, width, bend) * vertical;
            if (c <= 0) {
                return;
            }
            int amount = (int) Math.round(255.0 * STRIPE_PERCENT / 100.0 * c);
            delta.add(px, py, amount, amount, amount);
            delta.addOpacity(px, py, (int) Math.round(255.0 * c));
        });
        return delta;
    }
}
