package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
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
 * <b>Natural zebra</b> ({@code horsegenetics.natural_zebra}) - a real zebra's
 * striping, done the way a real zebra does it.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Zeb/n}</td><td>{@code shadow-striped} - the pattern at half strength</td></tr>
 *   <tr><td>{@code Zeb/Zeb}</td><td>{@code zebra-striped} - white stripes</td></tr>
 * </table>
 *
 * <h2>It takes pigment away; it does not add black</h2>
 * This is the whole difference between this gene and {@link MagicZebraGene},
 * and it is not a stylistic one. Ask which of a zebra's two colours is the
 * animal and which is the marking, and the biology answers clearly: the dark
 * hair is the default state, and a white stripe is a strip of skin where
 * melanin production was locally suppressed. A zebra is a dark animal with the
 * colour taken out in bands.
 *
 * <p>So this gene <b>whitens the gaps</b>. The horse's own colour - whatever
 * extension, agouti, the dilutions and grey have made of it by the time this
 * runs at priority {@value #PRIORITY} - survives as the dark bands, and
 * everything between them goes to white hair through
 * {@link PigmentField#whiten}. A striped black horse is black and white; a
 * striped chestnut is red and white; a striped palomino is gold and white. None
 * of that needed a line of code, because the gene never chooses a colour: it
 * only decides where there is <i>less</i> of one.
 *
 * <p>That also makes it a <b>natural</b> gene in the sense phase 1 means -
 * restrict-only, no pigment ever added - which the magical one structurally
 * could never be.
 *
 * <h2>Codominant, and the heterozygote is the interesting one</h2>
 * One copy suppresses melanin part of the way rather than all of it, which is
 * exactly what a plains zebra's <b>shadow stripes</b> look like: faint bands a
 * shade off the body colour, between the strong ones. Two copies take the same
 * texels to white. So the locus has three distinguishable phenotypes and the
 * carrier is not invisible - a player can see a horse is worth breeding before
 * they have the pair.
 *
 * <p>The pattern itself is {@link ZebraStripes} - a body map, not a field of
 * parallel bars: vertical bands off the spine, arcs round the hip, rings on the
 * legs, narrow bands on the neck and face, a dark dorsal stripe and muzzle, and
 * a pale belly. {@link MagicZebraGene} paints the same map in the opposite
 * direction, which is the point of the field returning <i>dark</i> coverage.
 *
 * <p>Founder frequency {@code 1/}{@value #WILD_ZEB_ONE_IN} per allele - so a
 * shadow-striped horse turns up now and then and a fully striped one is a
 * breeding project. Non-deterministic; see {@code wiki/gene-natural-zebra.html}.
 */
public final class NaturalZebraGene implements Gene {

    public static final String KEY = "horsegenetics.natural_zebra";

    /**
     * After every gene that decides what colour the horse is - extension,
     * agouti, the dilutions, grey - and before the white-spotting loci at 70+,
     * which paint their own white over the top and do not care what was under
     * it. The stripes are a subtraction from the finished base coat, so they
     * have to run once the base coat is finished.
     */
    /**
     * 69, not 68: the eight natural eye loci took {@code 61}-{@code 68} and this
     * is the first gene of the white-pattern band, so the band boundary in
     * {@link com.example.horsegenetics.common.genetics.GeneFamily} moved with
     * it. Nothing about this gene cares which number it is - it only has to
     * stay after the dilutions and before the white loci proper.
     */
    public static final int PRIORITY = 69;

    /** Founder frequency: one allele copy in this many. */
    public static final int WILD_ZEB_ONE_IN = 60;

    /** How much of the way to white one copy takes the gaps - the shadow stripes. */
    private static final float SHADOW_STRENGTH = 0.45f;

    // Body units (1 = 1/16 block); the adult barrel is 22 long, a foal's 14.
    private static final double SPACING_MIN = 2.0;
    private static final double SPACING_RANGE = 1.6;
    /** Fraction of each period that is dark band. Around a half is even stripe and gap. */
    private static final double DUTY_MIN = 0.42;
    private static final double DUTY_RANGE = 0.18;
    /** How far the noise may bend a band off its plane. Well under half the spacing. */
    private static final double BEND_MIN = 0.4;
    private static final double BEND_RANGE = 0.9;
    /**
     * How far down each leg the rings reach. The low end is a plains zebra,
     * whose lower legs go plain; the high end is a Gr&eacute;vy's, ringed to
     * the hoof.
     */
    private static final double LEG_REACH_MIN = 0.45;
    private static final double LEG_REACH_RANGE = 0.55;

    /** Half-width of the dorsal stripe, in body units - the organiser the rest hangs off. */
    private static final double DORSAL_HALF_WIDTH = 1.3;

    public final Allele Zeb = new Allele(KEY, 0, "Zeb", "Zebra striping (Zeb)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Zeb, n);

    private final Expression WILD = Expression.wildType("No striping.");

    private final Expression SHADOW = Expression.of("shadow-striped", "Shadow-striped")
            .describe("One copy suppresses pigment part of the way, so the horse wears the full "
                    + "zebra map as faint bands a shade off its own colour rather than as white "
                    + "ones - a plains zebra's shadow stripes. Visible, and worth breeding from.")
            .varies()
            .restrict(paint(SHADOW_STRENGTH));

    private final Expression STRIPED = Expression.of("zebra-striped", "Zebra-striped")
            .describe("White stripes taken out of the horse's own colour: vertical bands off the "
                    + "spine, arcs round the hip, rings down the legs, narrow bands on the neck and "
                    + "face, a dark dorsal stripe and muzzle, and a pale belly. Black and white on a "
                    + "black horse, red and white on a chestnut, gold and white on a palomino.")
            .varies()
            .restrict(paint(1.0f));

    private final List<Expression> expressions = List.of(WILD, SHADOW, STRIPED);

    private final FounderTable founders = FounderTable.hardyWeinberg(Zeb, n, 1.0 / WILD_ZEB_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Zebra striping"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return true; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (!pair.has(Zeb)) {
            return WILD;
        }
        return pair.has(n) ? SHADOW : STRIPED;
    }

    /** Does this combination stripe the coat at all - one copy or two? */
    public boolean isStriped(AllelePair pair) {
        return pair.has(Zeb);
    }

    // ------------------------------------------------------------------
    // The painter
    // ------------------------------------------------------------------

    /**
     * One painter for both outcomes, differing only in how far toward white the
     * gaps go - which is what codominance means here and is the reason the two
     * expressions cannot drift apart.
     *
     * <p>It whitens <b>{@code 1 - coverage}</b>: {@link ZebraStripes} returns
     * the <i>dark</i> band coverage, so the bands are simply the texels this
     * leaves alone. Regions a real zebra keeps solid dark - the dorsal stripe,
     * the muzzle, the tail - come back as coverage 1 and are therefore never
     * touched, without this method knowing they exist.
     *
     * <p>Both outcomes read the same five stored numbers - the band field's
     * seed, the spacing, duty, bend and leg reach - so a shadow-striped horse
     * and its fully striped foal wear the same pattern at two strengths.
     */
    /**
     * The band field and its four shape numbers. Shared by both outcomes, which
     * is what keeps a shadow-striped parent and a fully striped foal wearing the
     * same pattern.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.seed("seed"),
                EpiValue.uniform("spacing", SPACING_MIN, SPACING_MIN + SPACING_RANGE),
                EpiValue.uniform("duty", DUTY_MIN, DUTY_MIN + DUTY_RANGE),
                EpiValue.uniform("bend", BEND_MIN, BEND_MIN + BEND_RANGE),
                EpiValue.uniform("leg_reach", LEG_REACH_MIN, LEG_REACH_MIN + LEG_REACH_RANGE));
    }

    private static Expression.Pigment paint(float strength) {
        return (ctx, coat) -> {
            EpiValues epi = ctx.epigeneticsFor(KEY);
            ZebraStripes.Pattern pat = new ZebraStripes.Pattern(
                    epi.seed("seed"),
                    epi.get("spacing"),
                    epi.get("duty"),
                    epi.get("bend"),
                    epi.get("leg_reach"),
                    DORSAL_HALF_WIDTH);

            Skin skin = ctx.skin();
            PigmentField f = coat.mutableCopy();
            HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
                double pale = 1.0 - ZebraStripes.coverage(skin, part, point, pat);
                if (pale <= 0.0) {
                    return;
                }
                f.whiten(px, py, (float) (pale * strength));
            });
            return f;
        };
    }
}
