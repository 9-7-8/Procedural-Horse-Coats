package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.eye.EyeHue;
import com.example.horsegenetics.common.genetics.eye.EyeRequest;
import com.example.horsegenetics.common.genetics.eye.EyeRequestContribution;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

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
public final class ChampagneGene implements Gene, EyeRequestContribution {

    public static final String KEY = "horsegenetics.champagne";
    public static final int WILD_CHAMPAGNE_ONE_IN = 40;

    /**
     * Pheomelanin kept: <b>31% of red restricted</b>. Champagne is mostly a
     * eumelanin dilution - it takes four fifths of the black and only a third of
     * the red, which is what leaves a champagne warm rather than washed out.
     *
     * <p>Set to the owner's measured target against the gradient installed on
     * 2026-09-06. It has moved twice in a day and in opposite directions, which
     * is worth knowing before moving it again: a value here is only meaningful
     * against a particular chart, and the chart changed under it.
     */
    private static final float KEEP_RED = 0.69f;
    /** Eumelanin kept: <b>80% of black restricted</b>, which is the bulk of what champagne does. */
    private static final float KEEP_BLACK = 0.20f;
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
     * <p>One stored number, from the expressing copy: where this horse falls in
     * the champagne eye range. It is a weighted pick rather than a flat one -
     * amber is commoner than hazel - so it is kept as a {@code 0..1} position
     * with the probabilities as thresholds, which preserves the weighting
     * exactly. That also means drift can, very occasionally and over many
     * generations, walk a line's eyes from one shade to the next.
     */
    /**
     * Where this horse sits in the champagne eye range - see
     * {@link #eyeColor}. Champagne changes nothing else per horse.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform("eye", 0, 1));
    }

    @Override
    public EyeRequest requestEyes(AllelePair pair, Genotype genotype, Epigenome epigenome) {
        if (!pair.has(Ch)) {
            return EyeRequest.none();
        }
        double roll = GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get("eye");
        EyeHue hue;
        if (roll < P_AMBER) {
            hue = EyeHue.GOLD;
        } else if (roll < P_LIGHT_BROWN) {
            hue = EyeHue.BROWN;          // hazel and light brown both land here
        } else {
            hue = EyeHue.GREEN;          // the olive tail of the distribution
        }
        return EyeRequest.none().bothIrises(hue);
    }
}
