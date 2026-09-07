package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BodyNoise;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Flaxen</b> ({@code horsegenetics.flaxen}) - the pale mane and tail of a
 * chestnut horse, from a few blond strands to a near-white fall of hair over a
 * red body.
 *
 * <h2>It is a phenotype, and this is a simulation of one</h2>
 * There is <b>no identified flaxen mutation and no DNA test for it</b>. Breeders
 * have modelled it as a simple recessive for a century on pedigree evidence -
 * flaxen bred to flaxen tends to give flaxen, notably in Morgans - but that has
 * never been confirmed, there is no demonstrated one-copy-versus-two dose
 * series, and the intensity varies far too widely for a two-allele locus to be
 * a comfortable fit. The honest summary is <i>unresolved, and probably
 * polygenic</i>.
 *
 * <p>So this locus is a <b>simulated polygenic modifier</b>, and is labelled as
 * one everywhere it is shown. It takes the same shape the
 * {@link com.example.horsegenetics.common.genetics.BayShade bay shade} score
 * does, for the same reason: a graded, heritable, additive quantity is what the
 * evidence describes, and one locus with a dosage plus an expression roll says
 * that in a genotype code a player can read. Three to six separate polygenes
 * would say it too, at the cost of six segments in every horse's code and six
 * loci nobody could reason about.
 *
 * <table>
 *   <tr><th>combination</th><th>dosage</th><th>the long hair</th></tr>
 *   <tr><td>{@code f/f}</td><td>0</td><td>matches the body</td></tr>
 *   <tr><td>{@code Fl1/f}</td><td>1</td><td>mild - red hair with gold and cream strands</td></tr>
 *   <tr><td>{@code Fl1/Fl1}, {@code Fl2/f}</td><td>2</td><td>flaxen - clearly blond</td></tr>
 *   <tr><td>{@code Fl2/Fl1}</td><td>3</td><td>strong - pale cream, hard contrast</td></tr>
 *   <tr><td>{@code Fl2/Fl2}</td><td>4</td><td>near-white</td></tr>
 * </table>
 *
 * <h2>Chestnut only, and carried by everything</h2>
 * Flaxen is a <b>red</b>-pigment trait: it shows on a horse that makes no black
 * hair at all, which is {@code e/e} at {@link ExtensionGene}. A bay or a black
 * carries the whole dosage silently and hands it on, which is the satisfying
 * part - two plain chestnuts can throw a flaxen foal, and a black stallion can
 * be the reason a line has it.
 *
 * <h2>Not the look-alikes</h2>
 * Four other things give a horse pale long hair and none of them is this:
 * {@link MatpGene}'s cream (which dilutes the <i>body</i> to gold as well),
 * {@link SilverGene} (which works on black pigment, so it is a black or bay
 * trait and is invisible here), {@link MushroomGene}, and {@link GreyGene}.
 * Silver is the one that matters, because it carries an eye defect and flaxen
 * carries <b>nothing at all</b> - attaching a health cost to flaxen because a
 * silver looks similar would be exactly the mistake the reference warns about.
 *
 * <p>Natural, non-deterministic. Sits just after silver, the gene it is most
 * often confused with and whose job on the hair it most resembles.
 */
public final class FlaxenGene implements Gene {

    public static final String KEY = "horsegenetics.flaxen";

    public static final int PRIORITY = 31;

    /** Half-width of the per-horse expression roll, in dosage units. */
    public static final double EXPRESSION_RANGE = 0.4;

    /** Dosage at or below which the long hair is not flaxen at all. */
    public static final double PLAIN_MAX = 0.5;

    /** The highest dosage a horse can carry - two strong copies. */
    public static final double MAX_DOSAGE = 4.0;

    /** Fraction of the hair's red kept at the two ends of the range. */
    private static final float KEEP_RED_PLAIN = 1.0f;
    private static final float KEEP_RED_WHITE = 0.16f;

    /** How far apart the mane and the tail may land, in dosage units. */
    private static final double TAIL_OFFSET = 0.45;

    /** Strand mixing: the scale of the noise, and how much of the hair it spares. */
    private static final double STRAND_SCALE = 1.6;
    private static final double STRAND_DEPTH = 0.34;

    /** Fixed - the strand field is a texture of hair, not something a horse rolls. */
    private static final long STRAND_SEED = 0x51F1A7E2C0DDB19FL;

    public final Allele Fl2 = new Allele(KEY, 0, "Fl2", "Flaxen, strong (Fl2)");
    public final Allele Fl1 = new Allele(KEY, 1, "Fl1", "Flaxen, mild (Fl1)");
    public final Allele f = new Allele(KEY, 2, "f", "Wild-type (f)");
    private final List<Allele> alleles = List.of(Fl2, Fl1, f);

    private final Expression WILD = Expression.wildType(
            "The mane and tail are the colour of the body. On a horse that makes black hair this "
                    + "is what every combination looks like - flaxen only shows on a chestnut.");

    private final Expression MILD = flaxen("mild-flaxen", "Mild flaxen",
            "Red long hair shot through with gold and cream strands - enough to notice, not "
                    + "enough to call the horse flaxen at a distance.");
    private final Expression FLAXEN = flaxen("flaxen", "Flaxen",
            "Clearly blond mane, tail and forelock over a red body: the classic flaxen chestnut.");
    private final Expression STRONG = flaxen("strong-flaxen", "Strong flaxen",
            "Pale cream long hair against the body colour, with the hard contrast a Haflinger is "
                    + "known for.");
    private final Expression EXTREME = flaxen("extreme-flaxen", "Extreme flaxen",
            "A near-white fall of mane and tail, with only a few darker strands left in it.");

    private final List<Expression> expressions = List.of(WILD, MILD, FLAXEN, STRONG, EXTREME);

    private Expression flaxen(String id, String name, String description) {
        return Expression.of(id, name)
                .describe(description)
                .varies()
                .restrict(this::paint);
    }

    /**
     * Flaxen is common wherever chestnut is - it is the Haflinger's whole look
     * - so the alleles are not rare. What is rare is the <i>top</i> of the
     * range: {@code Fl2/Fl2} is about one horse in a hundred, and it still needs
     * a chestnut to show on.
     */
    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), pair -> true);

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(Fl2, 0.10);
        p.put(Fl1, 0.28);
        p.put(f, 0.62);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Flaxen"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return f; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    // ------------------------------------------------------------------
    // The score
    // ------------------------------------------------------------------

    /** This combination's dosage: {@code 0} to {@link #MAX_DOSAGE}, the sum of the two copies. */
    public double dosage(AllelePair pair) {
        return copyDosage(pair.first()) + copyDosage(pair.second());
    }

    private double copyDosage(Allele a) {
        if (a.equals(Fl2)) {
            return 2.0;
        }
        return a.equals(Fl1) ? 1.0 : 0.0;
    }

    /**
     * The dosage plus this horse's own expression roll - the seasonal, age and
     * condition variation that is one of the reasons a single-locus model never
     * convinced anyone. Rolled off the expressing copy's epigenetic seed, so it
     * is fixed for life and inherited with the allele.
     *
     * <p><b>Consumes one {@link Rng#nextFloat()}</b>, and is the first draw a
     * flaxen horse makes.
     */
    public static double score(double dosage, Rng epi) {
        return dosage + (epi.nextFloat() * 2.0 - 1.0) * EXPRESSION_RANGE;
    }

    // ------------------------------------------------------------------

    @Override
    public Expression expressionOf(AllelePair pair) {
        double d = dosage(pair);
        if (d <= PLAIN_MAX) {
            return WILD;
        }
        if (d <= 1.5) {
            return MILD;
        }
        if (d <= 2.5) {
            return FLAXEN;
        }
        return d <= 3.5 ? STRONG : EXTREME;
    }

    /**
     * <b>Nothing to lighten on a horse that has black hair.</b> Flaxen is a
     * red-pigment trait, so it reports the wild type on anything but a chestnut
     * - the dosage is still carried, still inherited, and completely invisible.
     * The same suppression agouti does on a chestnut, in the opposite
     * direction.
     */
    @Override
    public Expression expressionIn(AllelePair pair, Genotype genotype) {
        return genotype.hasBlackPigment() ? WILD : expressionOf(pair);
    }

    public boolean isFlaxen(AllelePair pair) {
        return dosage(pair) > PLAIN_MAX;
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * Lighten the long hair, and only the long hair. <b>Two
     * {@code nextFloat()}s</b>: the mane's score, then the tail's offset from
     * it - real flaxen manes and tails are often not the same shade, and one
     * being paler than the other is the commonest thing to notice about a
     * mixed-hair chestnut.
     *
     * <p>The body, the legs, the ear rims, the skin and the eyes are untouched.
     * The reference is unusually firm about that: flaxen is a long-hair
     * modifier and nothing else, and every gene that also lightens a body is a
     * different gene.
     *
     * <p>It takes red away and leaves black alone, which on the chestnut this
     * only ever runs on is the whole move - the chart's top row runs from a
     * warm brick red to white without passing through grey, so a mane at
     * {@link #KEEP_RED_WHITE} lands blond rather than silver with no tint term
     * needed. That is exactly what {@link SilverGene} <i>cannot</i> do, and why
     * it has one.
     */
    private PigmentField paint(CoatBuildContext ctx, PigmentView coat) {
        Skin skin = ctx.skin();
        Rng epi = ctx.epigeneticsFor(KEY);
        double dosage = dosage(ctx.genotype().pair(this));

        double mane = score(dosage, epi);
        double tail = mane + (epi.nextFloat() * 2.0 - 1.0) * TAIL_OFFSET;

        PigmentField out = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double s;
            if (part == Part.MANE) {
                s = mane;
            } else if (part == Part.TAIL) {
                s = tail;
            } else {
                return;     // the forelock is part of the mane box; nothing else is long hair
            }
            // Strand mixing: a flaxen mane keeps some darker hairs, and a
            // uniform wash reads as paint rather than as hair.
            double strands = BodyNoise.value(STRAND_SEED, point.x() * STRAND_SCALE,
                    point.y() * STRAND_SCALE, point.z() * STRAND_SCALE);
            float keep = keepRed(s * (1.0 - STRAND_DEPTH * strands));
            if (keep < 1.0f) {
                out.setRed(px, py, out.red(px, py) * keep);
            }
        });
        return out;
    }

    /** How much red a texel keeps at this local score. Clamped at both ends. */
    private static float keepRed(double score) {
        double t = (score - PLAIN_MAX) / (MAX_DOSAGE - PLAIN_MAX);
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return (float) (KEEP_RED_PLAIN + (KEEP_RED_WHITE - KEEP_RED_PLAIN) * t);
    }
}
