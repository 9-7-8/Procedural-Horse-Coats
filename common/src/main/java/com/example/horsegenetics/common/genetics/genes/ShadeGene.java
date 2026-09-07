package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;

import java.util.List;

/**
 * <b>Shade</b> ({@code horsegenetics.shade}) - the regulatory locus that
 * decides <b>how far black spreads over a bay</b>, and so whether that bay
 * reads as a blood bay, an ordinary bay, a liver bay or a seal brown.
 *
 * <p>It models the <i>ASIP</i>/<i>RALY</i> region on equine chromosome 22. That
 * region is the strongest known association with bay shade - much stronger than
 * the standard {@code A}/{@code a} test - but the causal mutation has not been
 * identified, so there is no real allele to name. What is modelled here is the
 * <b>haplotype dosage</b> the association describes: three alleles carrying
 * {@code -1.5}, {@code 0} and {@code +1.5}, summed, which is the "numerical
 * haplotype" form rather than a pretend {@code At}. See
 * {@code wiki/gene-shade.html} for why {@code At} is deliberately <i>not</i> in
 * this mod.
 *
 * <table>
 *   <tr><th>combination</th><th>dosage</th><th>tendency</th></tr>
 *   <tr><td>{@code ShL/ShL}</td><td>-3</td><td>black held to the points</td></tr>
 *   <tr><td>{@code ShL/Sh}</td><td>-1.5</td><td>light</td></tr>
 *   <tr><td>{@code ShL/ShD}, {@code Sh/Sh}</td><td>0</td><td>ordinary</td></tr>
 *   <tr><td>{@code Sh/ShD}</td><td>+1.5</td><td>dark</td></tr>
 *   <tr><td>{@code ShD/ShD}</td><td>+3</td><td>black over most of the body</td></tr>
 * </table>
 *
 * <p><b>It paints nothing itself.</b> Every combination is a
 * {@link Expression#wildType() wild type}, so {@link #affectsCoat()} is false
 * and it never reaches the coat pipeline - exactly like {@link Patn1Gene} and
 * {@link Patn2Gene}. What reads it is {@link AgoutiGene}, which names it in
 * {@code coatDependsOn()}, so a bay's texture key folds these alleles in and a
 * chestnut's does not. The six combinations are still worded separately,
 * because "carried, and pushing dark" and "carried, and pushing light" are
 * different things to read in the gene dictionary even though neither of them
 * draws a pixel on its own.
 *
 * <p>Unlike the appaloosa modifiers this is <b>not</b> conditional on the gene
 * that reads it: the region is present in every horse, a chestnut or a black
 * carries and transmits it silently, and that is what lets a chestnut line
 * throw a seal brown two generations later.
 *
 * <p>Natural. Sits in the {@code 80}-{@code 99} "paints nothing" band.
 */
public final class ShadeGene implements Gene {

    public static final String KEY = "horsegenetics.shade";

    /** How much one copy of each allele moves the shade score. */
    public static final double DARK_DOSAGE = 1.5;

    public final Allele ShD = new Allele(KEY, 0, "ShD", "Shade, dark haplotype (ShD)");
    public final Allele Sh = new Allele(KEY, 1, "Sh", "Shade, neutral haplotype (Sh)");
    public final Allele ShL = new Allele(KEY, 2, "ShL", "Shade, light haplotype (ShL)");
    private final List<Allele> alleles = List.of(ShD, Sh, ShL);

    private final Expression LIGHTEST = Expression.wildType("lightest", "Restricting (ShL/ShL)",
            "Carried, and silent on its own. On a bay it holds black to the points and keeps the "
                    + "body its reddest - a blood bay. On anything else it does nothing at all.");
    private final Expression LIGHT = Expression.wildType("light", "Light-leaning (ShL/Sh)",
            "Carried, and silent on its own. On a bay it leans the body toward red.");
    private final Expression NEUTRAL = Expression.wildType("neutral", "Neutral (Sh/Sh, ShL/ShD)",
            "Carried, and silent on its own. On a bay it gives the ordinary red-brown body and "
                    + "black points most people picture.");
    private final Expression DARK = Expression.wildType("dark", "Dark-leaning (Sh/ShD)",
            "Carried, and silent on its own. On a bay it spreads black further over the body - "
                    + "a mahogany or liver bay.");
    private final Expression DARKEST = Expression.wildType("darkest", "Expanding (ShD/ShD)",
            "Carried, and silent on its own. On a bay it lets black cover nearly the whole horse, "
                    + "leaving only the soft tan points of a seal brown.");

    private final List<Expression> expressions = List.of(LIGHTEST, LIGHT, NEUTRAL, DARK, DARKEST);

    /**
     * Hardy-Weinberg-ish shares for {@code p(ShL) = 0.30}, {@code p(Sh) = 0.45},
     * {@code p(ShD) = 0.25}, rounded to whole percents. Written out rather than
     * computed so one row can be retuned without disturbing the others.
     *
     * <p>The tail matters more than the middle here: {@code ShD/ShD} is the only
     * combination that can reach a seal brown at all (and only alongside
     * {@code E/E} or {@code A/a}), so its share is what makes a wild seal an
     * uncommon find rather than a routine one.
     *
     * <p>Row order follows the registry's convention - <b>rarest first, the
     * baseline combination last</b> - so a maximal roll lands on {@code Sh/Sh}
     * the way it lands on every other gene's plain horse.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(ShD, ShD, 7.0)
            .weight(ShL, ShL, 9.0)
            .weight(ShL, ShD, 15.0)
            .weight(Sh, ShD, 22.0)
            .weight(ShL, Sh, 27.0)
            .weight(Sh, Sh, 20.0)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Shade (ASIP/RALY)"; }
    @Override public int priority() { return 95; }
    @Override public GeneRarity rarity() { return GeneRarity.COMMON; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return Sh; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * This combination's contribution to the shade score: {@code -3} to
     * {@code +3}, the sum of the two copies' dosages. Positive is more black
     * spread.
     */
    public double dosage(AllelePair pair) {
        return copyDosage(pair.first()) + copyDosage(pair.second());
    }

    private double copyDosage(Allele a) {
        if (a.equals(ShD)) {
            return DARK_DOSAGE;
        }
        return a.equals(ShL) ? -DARK_DOSAGE : 0.0;
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        double d = dosage(pair);
        if (d <= -2.0 * DARK_DOSAGE) {
            return LIGHTEST;
        }
        if (d < 0) {
            return LIGHT;
        }
        if (d == 0) {
            return NEUTRAL;
        }
        return d >= 2.0 * DARK_DOSAGE ? DARKEST : DARK;
    }

    /** A carrot for a locus that is invisible until it is bred onto a bay is not a trap - it is the point. */
    @Override
    public boolean hasGeneCarrot() {
        return true;
    }
}
