package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

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
 */
public final class MatpGene implements Gene {

    public static final String KEY = "horsegenetics.matp";

    /** Founder frequency of {@code Cr}: one allele copy in this many. */
    public static final int WILD_CREAM_ONE_IN = 30;
    /** Founder frequency of {@code prl}: one allele copy in this many. */
    public static final int WILD_PEARL_ONE_IN = 22;

    // pigment kept (multiplied), per outcome
    private static final float SINGLE_CREAM_RED = 0.45f;   // copper -> golden
    // Single cream keeps most - but not all - of the black. Bay never *adds*
    // black anywhere; the points / lower legs are just black it declined to
    // restrict, so a real pigment dilution has to reach them too (a smoky /
    // sooty buckskin), not leave them jet black.
    private static final float SINGLE_CREAM_BLACK = 0.62f;
    private static final float CLASSIC_PEARL_RED = 0.55f;
    private static final float CLASSIC_PEARL_BLACK = 0.52f;
    private static final float DOUBLE_DILUTE_RED = 0.08f;   // body -> pale cream
    private static final float DOUBLE_DILUTE_BLACK = 0.38f; // points -> smoky rust

    // Removed eumelanin fed back in as pheomelanin - see PigmentField#dilute.
    private static final float SINGLE_CREAM_TINT = 0.30f;
    private static final float CLASSIC_PEARL_TINT = 0.28f;
    private static final float DOUBLE_DILUTE_TINT = 0.33f;

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
            .describe("Both pigments diluted mildly and evenly - an apricot body with sepia points. "
                    + "Only two pearl copies and no cream produce it.")
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

    private static Expression.Pigment dilution(float keepRed, float keepBlack, float blackTint) {
        return (ctx, coat) -> {
            PigmentField f = coat.mutableCopy();
            CoatRegions.restrictAll(ctx.skin(), f,
                    (field, px, py, p) -> field.dilute(px, py, keepRed, keepBlack, blackTint));
            return f;
        };
    }
}
