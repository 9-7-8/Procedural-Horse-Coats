package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
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
 * <b>MATP</b> ({@code horsegenetics.matp}) - the cream / pearl locus, and the
 * mod's first gene with <b>three alleles</b>.
 *
 * <p>Cream and pearl are the same physical gene in a real horse
 * ({@code SLC45A2} / {@code MATP}); this mod used to model them as two separate
 * two-allele genes with a shared resolver, which let a horse be
 * {@code Cr/Cr} <i>and</i> {@code prl/prl} at once - a genotype that cannot
 * exist. One locus with three alleles is both honest and simpler: the
 * impossible combinations stop being representable, and the whole dose table is
 * a six-row {@code switch} instead of a cross-gene lookup.
 *
 * <p>Three alleles, so <b>six combinations</b>, landing on four outcomes:
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code N/N}</td><td>wild type</td></tr>
 *   <tr><td>{@code prl/N}</td><td>wild type - a pearl carrier, invisible</td></tr>
 *   <tr><td>{@code Cr/N}</td><td>{@code single-cream} - palomino / buckskin / smoky black</td></tr>
 *   <tr><td>{@code prl/prl}</td><td>{@code classic-pearl} - a mild uniform dilution of both pigments</td></tr>
 *   <tr><td>{@code Cr/prl}</td><td>{@code double-dilute} - the two complement, acting as a double cream</td></tr>
 *   <tr><td>{@code Cr/Cr}</td><td>{@code double-dilute} - cremello / perlino / smoky cream</td></tr>
 * </table>
 *
 * <p>This is the table that no single word describes: {@code Cr} on its own is
 * a partial dilution, {@code prl} on its own is nothing, two {@code prl} is a
 * different dilution again, and one of each is the strongest of the three.
 * "Incomplete dominant" was never going to say that, which is why there is no
 * dominance property any more - the six rows above are the whole model.
 *
 * <p>Red is always restricted more than black under a double dilution, which is
 * why a diluted bay body fades to cream while the points hold smoky colour.
 * Each mode carries a <b>black tint</b> (see
 * {@link PigmentField#dilute}) so the points land in a real diluted-black hue
 * instead of the gradient's jet-black zero-red column.
 *
 * <p><b>House rule</b> (owner, 2026-09-01): <i>no</i> cream horse keeps a
 * pitch-black point. A single-cream point may be a very dark brown but never a
 * void - so its tint is a full one, not the token amount a real-world buckskin
 * would argue for; classic pearl gets sepia points, perlino rusty ones.
 *
 * <p>Natural, deterministic. Founder allele frequencies {@code 1/}{@value
 * #WILD_CREAM_ONE_IN} for {@code Cr} and {@code 1/}{@value #WILD_PEARL_ONE_IN}
 * for {@code prl} - the same numbers the two old genes carried, so the wild
 * population is unchanged apart from the impossible genotypes disappearing.
 *
 * <h2>The eye follows the same six rows</h2>
 * A double dilute's pale blue eye is the famous one, and it is not the blue of a
 * splashed white horse: the iris has all its pigment cells, they are simply not
 * handling enough melanin to colour it, so it claims at
 * {@link EyeColor#RANK_DILUTION} and loses to the depigmented blue rather than
 * competing with it. The compound heterozygote {@code Cr/prl} is the one row
 * where cream and pearl do not agree about the eye - it comes out
 * <b>blue-green</b>, which is the mod's ordinary source of a green eye - and two
 * pearls alone give the pale, bright iris pearl is described by. A single cream
 * copy does not touch the eye at all.
 */
public final class MatpGene implements Gene, EyeColorContribution {

    public static final String KEY = "horsegenetics.matp";

    /** Founder frequency of {@code Cr}: one allele copy in this many. */
    public static final int WILD_CREAM_ONE_IN = 30;
    /** Founder frequency of {@code prl}: one allele copy in this many. */
    public static final int WILD_PEARL_ONE_IN = 22;

    // Pigment kept (multiplied), per outcome - so a value of 0.80 is "20% of this
    // pigment restricted away". Set to the owner's measured targets against the
    // current gradient (2026-09-06), read off the LUT lab: the chart was replaced
    // the same day and every one of these was recalibrated against it rather than
    // against the chart they were originally tuned for.
    //
    // Single cream: 20% of red restricted, 29% of black.
    private static final float SINGLE_CREAM_RED = 0.80f;
    // Bay never *adds* black anywhere; the points / lower legs are just black it
    // declined to restrict, so a real pigment dilution has to reach them too (a
    // smoky / sooty buckskin), not leave them jet black.
    private static final float SINGLE_CREAM_BLACK = 0.71f;
    // Classic pearl: 20% of red restricted, 60% of black - a mild, mostly
    // eumelanin dilution on this chart, where the previous chart wanted it far
    // paler to read as anything at all.
    private static final float CLASSIC_PEARL_RED = 0.80f;
    private static final float CLASSIC_PEARL_BLACK = 0.40f;
    // Double dilute: 47% of red restricted, 95% of black - and it is the only one
    // of the three that takes most of both, which is what keeps it the strongest
    // row in the table however the other two are tuned.
    private static final float DOUBLE_DILUTE_RED = 0.53f;
    private static final float DOUBLE_DILUTE_BLACK = 0.05f;

    // Removed eumelanin fed back in as pheomelanin - see PigmentField#dilute.
    //
    // These came down with the keepBlack values, and had to. The tint exists to
    // walk a diluted point off the gradient's zero-red column, which reads
    // visually black all the way down to about black 0.4 - so it is sized
    // against how much black is *left*. At the old 0.38 double dilute needed a
    // third of the removed eumelanin back to escape that column; at 0.09 it is
    // nowhere near it, and the same tint would only make a near-white horse
    // orange.
    private static final float SINGLE_CREAM_TINT = 0.30f;
    private static final float CLASSIC_PEARL_TINT = 0.13f;
    private static final float DOUBLE_DILUTE_TINT = 0.10f;

    /** {@code Cr/Cr} - the cremello / perlino pale blue. */
    public static final int DOUBLE_CREAM_BLUE = 0x93C4E4;
    /** {@code Cr/prl} - the compound heterozygote, where the two dilutions disagree. */
    public static final int CREAM_PEARL_GREEN = 0x59A08F;
    /** {@code prl/prl} - light and bright rather than blue. */
    public static final int PEARL_LIGHT = 0xB99B5E;

    public final Allele Cr = new Allele(KEY, 0, "Cr", "Cream (Cr)");
    public final Allele prl = new Allele(KEY, 1, "prl", "Pearl (prl)");
    public final Allele N = new Allele(KEY, 2, "N", "Wild-type (N)");
    private final List<Allele> alleles = List.of(Cr, prl, N);

    private final Expression WILD = Expression.wildType("No dilution.");

    private final Expression PEARL_CARRIER = Expression.wildType(
            "pearl-carrier", "Pearl carrier",
            "One pearl copy does nothing on its own - the horse looks undiluted and only its "
                    + "descendants show it. Breeding two carriers is how classic pearl appears.");

    private final Expression SINGLE_CREAM = Expression.of("single-cream", "Single cream")
            .describe("Red cut hard and black cut a little - palomino on a chestnut, buckskin on a bay, "
                    + "smoky black on a black. The points go dark brown, never jet black.")
            .restrict(dilution(SINGLE_CREAM_RED, SINGLE_CREAM_BLACK, SINGLE_CREAM_TINT));

    private final Expression CLASSIC_PEARL = Expression.of("classic-pearl", "Classic pearl")
            .describe("Black cut hard and red barely touched - a warm, lightened coat that keeps "
                    + "its colour rather than washing out. Only two pearl copies and no cream "
                    + "produce it.")
            .restrict(dilution(CLASSIC_PEARL_RED, CLASSIC_PEARL_BLACK, CLASSIC_PEARL_TINT));

    private final Expression DOUBLE_DILUTE = Expression.of("double-dilute", "Double dilute")
            .describe("Red almost entirely gone and black heavily cut - cremello, perlino or smoky "
                    + "cream, a near-white body over rusty points. Two cream copies do it, and so "
                    + "does one cream with one pearl.")
            .restrict(dilution(DOUBLE_DILUTE_RED, DOUBLE_DILUTE_BLACK, DOUBLE_DILUTE_TINT));

    private final List<Expression> expressions =
            List.of(WILD, PEARL_CARRIER, SINGLE_CREAM, CLASSIC_PEARL, DOUBLE_DILUTE);

    /**
     * The six combinations at their Hardy-Weinberg shares given
     * {@code p(Cr) = 1/30} and {@code p(prl) = 1/22}. Written out rather than
     * computed so the numbers are readable and an author can retune one row
     * without touching the others.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(Cr, Cr, 0.111111)
            .weight(Cr, prl, 0.303030)
            .weight(Cr, N, 6.141414)
            .weight(prl, prl, 0.206612)
            .weight(prl, N, 8.374656)
            .weight(N, N, 84.863177)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "MATP (cream / pearl)"; }
    @Override public int priority() { return 40; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * The whole gene, in one function: count the copies of each allele and read
     * the row off.
     */
    @Override
    public Expression expressionOf(AllelePair pair) {
        int cream = pair.count(Cr);
        int pearl = pair.count(prl);
        if (cream == 2 || (cream == 1 && pearl == 1)) {
            return DOUBLE_DILUTE;
        }
        if (cream == 1) {
            return SINGLE_CREAM;
        }
        if (pearl == 2) {
            return CLASSIC_PEARL;
        }
        return pearl == 1 ? PEARL_CARRIER : WILD;
    }

    /** How many cream copies - for the wiki and anything that wants the dose. */
    public int creamDose(AllelePair pair) {
        return pair.count(Cr);
    }

    /** How many pearl copies. */
    public int pearlDose(AllelePair pair) {
        return pair.count(prl);
    }

    /**
     * The iris, read off the same dose count as the coat. Deterministic - every
     * cremello has the same pale blue eye - so this locus costs the coat cache
     * one entry per outcome and not one per horse.
     */
    @Override
    public Optional<EyeColor> eyeColor(AllelePair pair, Genotype genotype, Epigenome epigenome,
                                       double whiteCoverage) {
        int cream = pair.count(Cr);
        int pearl = pair.count(prl);
        if (cream == 2) {
            return Optional.of(EyeColor.dilution("cream-blue", "Cream blue", DOUBLE_CREAM_BLUE));
        }
        if (cream == 1 && pearl == 1) {
            return Optional.of(EyeColor.dilution("cream-pearl-green", "Blue-green", CREAM_PEARL_GREEN));
        }
        if (pearl == 2) {
            return Optional.of(EyeColor.dilution("pearl-light", "Pale pearl", PEARL_LIGHT));
        }
        return Optional.empty();
    }

    private static Expression.Pigment dilution(float keepRed, float keepBlack, float blackTint) {
        return (ctx, coat) -> {
            PigmentField f = coat.mutableCopy();
            CoatRegions.restrictAll(ctx.skin(), f,
                    (field, px, py, p) -> field.dilute(px, py, keepRed, keepBlack, blackTint));
            return f;
        };
    }
}
