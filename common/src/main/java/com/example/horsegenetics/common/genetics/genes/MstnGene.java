package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.TraitContribution;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>MSTN</b> ({@code horsegenetics.mstn}), myostatin - the "speed gene", and
 * the first gene in this mod with no coat effect whatsoever.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code C/C}</td><td>{@code sprinter} - fastest, least hardy</td></tr>
 *   <tr><td>{@code C/T}</td><td>{@code middle-distance} - half of each</td></tr>
 *   <tr><td>{@code T/T}</td><td>{@code stayer} - slowest, hardiest</td></tr>
 * </table>
 *
 * <h2>Codominant, and therefore additive</h2>
 * Neither allele hides the other: each {@code C} copy is worth
 * {@value #SPEED_PER_C} movement speed <i>and</i> costs
 * {@value #HEALTH_COST_PER_C} health, so the heterozygote really is the middle
 * of the two homozygotes rather than a copy of one of them. That is what
 * "incompletely dominant" means, and it needs no special case here - the
 * contribution just counts copies.
 *
 * <p>The whole trade rides on {@code C}, and {@code T} is worth exactly nothing.
 * That is deliberate and it is the rule everywhere in this model: a gene's
 * <b>baseline allele contributes zero</b>, so an all-wild-type horse resolves
 * to the flat baselines in
 * {@link com.example.horsegenetics.common.trait.HorseTraits} and every number a
 * player sees is a departure from a known starting point. Paying the stayer a
 * bonus instead would have said the same thing about the difference between the
 * two homozygotes and made the baseline a lie.
 *
 * <h2>Where the stamina went</h2>
 * In real horses this locus trades sprint against stamina. This mod has settled
 * that <b>there is no stamina resource</b> ({@code wiki/roadmap.html} 21), so
 * there is nothing for the {@code T} side to buy - and a gene where one allele
 * is simply better than the other is not a choice, it is a chore. So stamina is
 * paid out in the nearest thing the game does have: <b>max health</b>. A stayer
 * is the horse that keeps going, and in Minecraft terms that is the horse with
 * more hearts.
 *
 * <p>Both alleles are common in the wild, unlike every disorder locus: this is a
 * trait, not a defect, and a player should be able to find both ends of it in
 * the first handful of horses they catch.
 */
public final class MstnGene implements Gene, TraitContribution {

    public static final String KEY = "horsegenetics.mstn";
    public static final int PRIORITY = 80;

    /** Movement speed added per {@code C} copy. */
    public static final double SPEED_PER_C = 0.020;
    /** Max health <b>lost</b> per {@code C} copy - what the speed is bought with. */
    public static final double HEALTH_COST_PER_C = 2.0;

    /** Population frequency of the sprint allele. Both ends are easy to find. */
    public static final double WILD_C_FREQUENCY = 0.35;

    public final Allele C = new Allele(KEY, 0, "C", "Sprint (C)");
    public final Allele T = new Allele(KEY, 1, "T", "Stayer (T)");
    private final java.util.List<Allele> alleles = java.util.List.of(C, T);

    private final Expression SPRINTER = Expression.wildType("sprinter", "Sprinter",
            "Two sprint copies. The fastest horse this locus can make, and the one with the "
                    + "fewest hearts to show for it.");

    private final Expression MIDDLE = Expression.wildType("middle-distance", "Middle distance",
            "One of each. Half a sprinter's speed for half a sprinter's hearts - the alleles do "
                    + "not hide each other, they add.");

    private final Expression STAYER = Expression.wildType("stayer", "Stayer",
            "Two stayer copies - the baseline horse. No speed bonus, and no hearts given up "
                    + "for one.");

    private final java.util.List<Expression> expressions = java.util.List.of(SPRINTER, MIDDLE, STAYER);

    private final FounderTable founders = FounderTable.hardyWeinberg(C, T, WILD_C_FREQUENCY);

    @Override public String key() { return KEY; }
    @Override public String name() { return "MSTN (myostatin)"; }
    @Override public int priority() { return PRIORITY; }
    @Override public java.util.List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return T; }
    @Override public java.util.List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(C)) {
            case 2 -> SPRINTER;
            case 1 -> MIDDLE;
            default -> STAYER;
        };
    }

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        int sprint = pair.count(C);
        out.addSpeed(sprint * SPEED_PER_C)
                .addHealth(-sprint * HEALTH_COST_PER_C);
    }
}
