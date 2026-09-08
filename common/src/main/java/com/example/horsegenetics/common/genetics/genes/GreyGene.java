package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.GreyCoat;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.common.trait.TraitBuilder;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;

import java.util.List;

/**
 * <b>Grey</b> ({@code horsegenetics.grey}) - progressive depigmentation of the
 * <i>hair</i>, and the only coat gene in the mod that is also a health gene.
 *
 * <h2>Three alleles, because the real locus is a copy number</h2>
 * Grey is not a point mutation. It is a <b>copy-number variation</b> in and
 * around intron 6 of <i>STX17</i>, and the number of copies is what varies:
 *
 * <table>
 *   <tr><th>allele</th><th>copies</th><th>greying</th><th>melanoma</th></tr>
 *   <tr><td>{@code N}</td><td>normal</td><td>none</td><td>baseline</td></tr>
 *   <tr><td>{@code G2}</td><td>duplication</td><td>slower</td><td>lower</td></tr>
 *   <tr><td>{@code G3}</td><td>triplication</td><td>faster</td><td>higher</td></tr>
 * </table>
 *
 * <p>Either copy-bearing allele makes a horse grey, so grey is <b>dominant</b>
 * in the ordinary sense - but the four grey genotype classes are not the same
 * animal. They differ in <b>rate</b>, and rate is dosage: {@code G3} counts two,
 * {@code G2} counts one, and the sum runs 1 to 4.
 *
 * <table>
 *   <tr><th>combination</th><th>dosage</th><th>outcome</th></tr>
 *   <tr><td>{@code N/N}</td><td>0</td><td>wild type</td></tr>
 *   <tr><td>{@code N/G2}</td><td>1</td><td>{@code grey-slow}</td></tr>
 *   <tr><td>{@code N/G3}, {@code G2/G2}</td><td>2</td><td>{@code grey}</td></tr>
 *   <tr><td>{@code G2/G3}</td><td>3</td><td>{@code grey-fast}</td></tr>
 *   <tr><td>{@code G3/G3}</td><td>4</td><td>{@code grey-rapid}</td></tr>
 * </table>
 *
 * <p>Four grey outcomes rather than one is what makes the dosage <i>visible</i>
 * without an age clock. Real greying is a rate, and a rate needs a timeline to
 * become a picture; this mod has no aging (see {@code wiki/philosophy.html}), so
 * each dosage class is drawn at the stage that rate would plausibly have reached
 * on an ordinary adult. A slow grey is a steel or dapple grey for life; a rapid
 * grey is the near-white one. The windows deliberately <b>overlap</b>, because
 * individual variation inside a genotype class is large and a horse's own
 * epigenetics still pick where in its window it sits.
 *
 * <p>Natural: it remaps the coat onto the gradient's neutral column, so it
 * lightens without shifting hue. <b>Non-deterministic</b> - the expressing
 * copy's epigenetics decide the exact stage, the dapples, how long the legs hold
 * their colour, whether the mane and tail run ahead of or behind the body, and
 * whether the horse carries chubari spots or a bloody shoulder. See
 * {@link GreyCoat}.
 *
 * <p>A <b>foal is born its base colour</b>: the outcome is the grey one either
 * way (so a grey foal bakes its own texture), but the painter returns nothing
 * until the horse is an adult.
 *
 * <h2>Melanoma</h2>
 * The same <i>STX17</i> change that removes pigment from hair drives melanocyte
 * proliferation, and grey horses carry a markedly raised lifetime risk of dermal
 * melanoma - over 70% of greys past fifteen. It is reported here as an
 * {@link Condition#informational informational} condition and <b>nothing else</b>:
 * no lesions are drawn, no stat is docked, and a horse's whiteness is never a
 * diagnosis. See {@code wiki/gene-grey.html} for why the line was drawn there.
 */
public final class GreyGene implements Gene, HealthContribution {

    public static final String KEY = "horsegenetics.grey";

    /** Founder frequency of one {@code G3} copy: {@code 1/}{@value}. */
    public static final int WILD_G3_ONE_IN = 18;
    /** Founder frequency of one {@code G2} copy: {@code 1/}{@value} - rarer, and breed-clustered. */
    public static final int WILD_G2_ONE_IN = 70;

    public final Allele G3 = new Allele(KEY, 0, "G3", "Grey, triplication (G3)");
    public final Allele G2 = new Allele(KEY, 1, "G2", "Grey, duplication (G2)");
    public final Allele N = new Allele(KEY, 2, "N", "Wild-type (N)");
    private final List<Allele> alleles = List.of(G3, G2, N);

    private final Expression WILD = Expression.wildType("The coat keeps its colour for life.");

    private final Expression SLOW = grey("grey-slow", "Slow grey (N/G2)",
            "One duplication, and the slowest greying there is: this horse spends its life as a "
                    + "dark steel or mid dapple grey with its base colour still legible under the "
                    + "white hairs.");
    private final Expression MID = grey("grey", "Grey (N/G3, G2/G2)",
            "The ordinary grey - a dappled mid grey with the head already pale, the legs and the "
                    + "long hair holding their colour longest.");
    private final Expression FAST = grey("grey-fast", "Fast grey (G2/G3)",
            "Three copies between the two alleles. A light grey: dapples fading out, base colour "
                    + "left mostly on the legs and around the flank.");
    private final Expression RAPID = grey("grey-rapid", "Rapid grey (G3/G3)",
            "Two triplications, the fastest greying and the highest melanoma risk. Very nearly "
                    + "white - the dark skin and dark eyes are what still say grey rather than "
                    + "white.");

    private final List<Expression> expressions = List.of(WILD, SLOW, MID, FAST, RAPID);

    /**
     * Hardy-Weinberg over the two grey alleles. {@code G3} is the broadly
     * prevalent one; {@code G2} is real but breed-clustered, so the wild pool
     * carries it at about a quarter of {@code G3}'s rate and the breeds the
     * duplication was actually found in carry most of it.
     */
    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), pair -> true);

    private java.util.Map<Allele, Double> frequencies() {
        java.util.Map<Allele, Double> f = new java.util.LinkedHashMap<>();
        f.put(G3, 1.0 / WILD_G3_ONE_IN);
        f.put(G2, 1.0 / WILD_G2_ONE_IN);
        f.put(N, 1.0 - 1.0 / WILD_G3_ONE_IN - 1.0 / WILD_G2_ONE_IN);
        return f;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Grey"; }
    @Override public int priority() { return 55; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * How many <i>STX17</i> copies over normal this pair carries: 0 to 4.
     * {@code G3} is a triplication and counts two, {@code G2} a duplication and
     * counts one. This is the greying rate and the melanoma risk in one number,
     * which is what the real copy-number test measures.
     */
    public int dosage(AllelePair pair) {
        return copyDosage(pair.first()) + copyDosage(pair.second());
    }

    private int copyDosage(Allele a) {
        if (a.equals(G3)) {
            return 2;
        }
        return a.equals(G2) ? 1 : 0;
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        switch (dosage(pair)) {
            case 0:
                return WILD;
            case 1:
                return SLOW;
            case 2:
                return MID;
            case 3:
                return FAST;
            default:
                return RAPID;
        }
    }

    public boolean isGrey(AllelePair pair) {
        return dosage(pair) > 0;
    }

    // ------------------------------------------------------------------
    // Melanoma
    // ------------------------------------------------------------------

    /**
     * <b>Reported, never simulated.</b> Every grey gets a risk flag whose
     * wording follows the copy number, raised a tier on a black-based horse
     * because non-agouti greys are the group with the documented excess.
     *
     * <p>It is {@link Condition#informational}, so it changes no stat and kills
     * no horse. That is the design, not a stub: melanoma in life is a
     * <i>lifetime probability</i> that mostly resolves as benign nodules on an
     * old horse, and this mod has no age to hang a probability on. Docking
     * health from every grey would assert a disease four greys in ten never get,
     * and drawing lesions would be worse. So the mod says what the DNA test says
     * - this horse is in a raised-risk group, and here is how raised - and stops
     * there.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        int dosage = dosage(pair);
        if (dosage == 0) {
            return;
        }
        boolean blackBased = genotype.hasBlackPigment() && !genotype.isAgouti();
        int tier = Math.min(3, (dosage >= 3 ? 2 : 1) + (blackBased ? 1 : 0));
        out.condition(Condition.informational("melanoma-risk-" + tier,
                "Melanoma risk (" + RISK[tier] + ")", RISK_TEXT[tier]));
    }

    private static final String[] RISK = {"", "raised", "high", "highest"};
    private static final String[] RISK_TEXT = {"",
            "Carries one grey duplication. Raised lifetime risk of dermal melanoma - in life the "
                    + "tail dock, the sheath or udder, the lips and the eyelids are where it shows. "
                    + "No effect on this horse's body.",
            "Carries three or four grey copies, or a grey allele on a black-based coat. High "
                    + "lifetime risk of dermal melanoma. No effect on this horse's body.",
            "Three or four grey copies on a black-based coat - the highest-risk combination there "
                    + "is. No effect on this horse's body."};

    private static Expression grey(String id, String name, String description) {
        return Expression.of(id, name)
                .describe(description + " A foal is born its base colour and greys on reaching "
                        + "adulthood; the head goes first, the legs and the long hair last.")
                .varies()
                .restrict((ctx, coat) -> {
                    if (!ctx.isAdult()) {
                        return null; // a foal is born its base colour
                    }
                    PigmentField f = coat.mutableCopy();
                    GreyCoat.apply(ctx, f, ctx.epigeneticsFor(KEY));
                    return f;
                });
    }
    /**
     * Everything greying varies by - see {@link GreyCoat#schema()}. Grey is the
     * gene with the most per-horse character in the mod: two greys of the same
     * age and dosage differ in how far along they are, how they dapple, and
     * whether they carry chubari spots or a bloody shoulder.
     */
    @Override
    public EpiSchema epiSchema() {
        return GreyCoat.schema();
    }

}
