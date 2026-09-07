package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.AlleleRandomness;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyeColorContribution;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;
import java.util.Optional;

/**
 * <b>Champagne</b> ({@code horsegenetics.champagne}) - a dilution that does not
 * read its own dose.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code c/c}</td><td>wild type</td></tr>
 *   <tr><td>{@code Ch/c}, {@code Ch/Ch}</td><td>{@code champagne} - the full dilution either way</td></tr>
 * </table>
 *
 * <p>Natural: it just moves the pigment sample. It keeps most of the red, cuts
 * black hard, and feeds part of the removed black back in as red
 * ({@link com.example.horsegenetics.common.coat.pattern.PigmentField#dilute}),
 * so it reads off the <i>current</i> pigment - gold champagne (on chestnut)
 * stays gold, classic champagne (on black) lands taupe, and amber champagne
 * (on bay) keeps <b>chocolate points</b> over a gold body instead of washing
 * the points out to the body colour. Champagne on a white horse is invisible.
 *
 * <h2>The eye is the one thing about it that varies</h2>
 * Champagne dilutes the iris along with everything else, and the shade it
 * settles on in an adult is famously not one colour: amber, golden, hazel,
 * light brown, or - occasionally - a greenish olive. So the coat is
 * deterministic and the <b>iris is epigenetic</b>, rolled from the expressing
 * copy's seed and inherited with it, which makes an olive-eyed champagne
 * something a breeder can chase rather than something the game hands out.
 *
 * <p>It is also the mod's ordinary source of a <b>hazel</b> eye, and its only
 * source of a green one that is not magical.
 *
 * <p>Founder frequency {@code 1/}{@value #WILD_CHAMPAGNE_ONE_IN} per allele.
 */
public final class ChampagneGene implements Gene, EyeColorContribution {

    public static final String KEY = "horsegenetics.champagne";
    public static final int WILD_CHAMPAGNE_ONE_IN = 40;

    /**
     * Pheomelanin kept. Champagne cuts red <b>hard</b> - it was 0.55 until
     * 2026-09-06, on the reasoning that gold champagne should stay gold, and the
     * LUT lab's footprint showed what that actually meant: a gold champagne
     * resolving barely a third of the way across the chart from a plain
     * chestnut, which is not a dilution anyone would name.
     */
    private static final float KEEP_RED = 0.32f;
    /** Eumelanin kept - hard, but not so hard that a black horse ends up gold. */
    private static final float KEEP_BLACK = 0.36f;
    /**
     * Fraction of a texel's eumelanin fed back in as pheomelanin. This is what
     * gives an <b>amber champagne</b> its chocolate points: bay's black points
     * carry no red at all, and without this term champagne washed them to the
     * same gold as the body.
     */
    private static final float BLACK_TINT = 0.30f;

    /**
     * The iris shades, commonest first, with the cumulative share of champagne
     * horses that reach each one. Straight off the reference table's champagne
     * row; the ordering is what makes olive a find.
     */
    public static final int AMBER = 0xB07A2E;
    public static final int HAZEL = 0x8E7238;
    public static final int LIGHT_BROWN = 0x9A7B5E;
    public static final int OLIVE = 0x6B7F44;

    private static final float P_AMBER = 0.40f;
    private static final float P_HAZEL = 0.75f;
    private static final float P_LIGHT_BROWN = 0.92f;

    public final Allele Ch = new Allele(KEY, 0, "Ch", "Champagne (Ch)");
    public final Allele c = new Allele(KEY, 1, "c", "Wild-type (c)");
    private final List<Allele> alleles = List.of(Ch, c);

    private final Expression WILD = Expression.wildType("No dilution.");

    private final Expression CHAMPAGNE = Expression.of("champagne", "Champagne")
            .describe("Red mostly kept and black cut hard, with some of the removed black fed back as "
                    + "red - gold champagne on a chestnut, classic taupe on a black, amber with "
                    + "chocolate points on a bay. One copy and two look the same. The eye is "
                    + "diluted too, landing anywhere from amber through hazel to a rare olive "
                    + "green, and which one is inherited with the allele.")
            .varies()
            .restrict((ctx, coat) -> {
                PigmentField f = coat.mutableCopy();
                CoatRegions.restrictAll(ctx.skin(), f,
                        (field, px, py, p) -> field.dilute(px, py, KEEP_RED, KEEP_BLACK, BLACK_TINT));
                return f;
            });

    private final List<Expression> expressions = List.of(WILD, CHAMPAGNE);

    private final FounderTable founders = FounderTable.hardyWeinberg(Ch, c, 1.0 / WILD_CHAMPAGNE_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Champagne"; }
    @Override public int priority() { return 50; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return c; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(Ch) ? CHAMPAGNE : WILD;
    }

    public boolean isChampagne(AllelePair pair) {
        return pair.has(Ch);
    }

    /**
     * The diluted iris, at {@link EyeColor#RANK_DILUTION} - so tiger eye, which
     * is aimed at the eye and nothing else, over-rides it, and a splashed white
     * champagne has blue eyes.
     *
     * <p>One {@code nextFloat}, from the expressing copy: the whole draw-order
     * contract of this gene.
     */
    @Override
    public Optional<EyeColor> eyeColor(AllelePair pair, Genotype genotype, Epigenome epigenome,
                                       double whiteCoverage) {
        if (!pair.has(Ch)) {
            return Optional.empty();
        }
        Rng r = AlleleRandomness.forGene(this, genotype, epigenome).expressed();
        float roll = r.nextFloat();
        if (roll < P_AMBER) {
            return Optional.of(EyeColor.dilution("champagne-amber", "Champagne amber", AMBER));
        }
        if (roll < P_HAZEL) {
            return Optional.of(EyeColor.dilution("champagne-hazel", "Champagne hazel", HAZEL));
        }
        if (roll < P_LIGHT_BROWN) {
            return Optional.of(EyeColor.dilution("champagne-light-brown", "Champagne light brown", LIGHT_BROWN));
        }
        return Optional.of(EyeColor.dilution("champagne-olive", "Champagne olive", OLIVE));
    }
}
