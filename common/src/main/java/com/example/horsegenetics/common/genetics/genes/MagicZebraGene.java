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

import java.util.List;

/**
 * <b>Magic zebra</b> ({@code horsegenetics.magic_zebra}) - a <b>magical</b>
 * gene.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Mzeb/n}, {@code Mzeb/Mzeb}</td><td>{@code zebra} - black bands over any coat at all</td></tr>
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

    private final Expression ZEBRA = Expression.of("zebra", "Magic zebra")
            .describe("A zebra's own stripe map painted in hard black: vertical bands off the spine, "
                    + "arcs round the hip, rings down the legs, narrow bands on the neck and face, a "
                    + "black dorsal stripe and muzzle, and an unstriped belly. Per-horse in spacing, "
                    + "width, bend and leg reach. They read black over any coat at all, including a "
                    + "cremello or a dominant white.")
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
        ZebraStripes.Pattern pat = new ZebraStripes.Pattern(
                epi.nextLong(),
                SPACING_MIN + epi.nextFloat() * SPACING_RANGE,
                WIDTH_MIN + epi.nextFloat() * WIDTH_RANGE,
                BEND_MIN + epi.nextFloat() * BEND_RANGE,
                REACH_MIN + epi.nextFloat() * REACH_RANGE,
                DORSAL_HALF_WIDTH);

        Skin skin = ctx.skin();
        ColorField delta = ColorField.deltaLike(accumulated);
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double c = ZebraStripes.coverage(skin, part, point, pat);
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
