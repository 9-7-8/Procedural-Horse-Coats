package com.example.horsegenetics.common.genetics.genes;

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

import java.util.List;

/**
 * <b>A disorder with no silent carrier</b>: one copy is enough to affect the
 * horse, and two copies are worse.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code N/N}</td><td>wild type</td></tr>
 *   <tr><td>{@code v/N}</td><td>affected - the {@link #heterozygousCondition()}</td></tr>
 *   <tr><td>{@code v/v}</td><td>worse, and sometimes lethal - the {@link #homozygousCondition()}</td></tr>
 * </table>
 *
 * <h2>Why this could not be {@link RecessiveDisorderGene}</h2>
 * Every disorder locus in the mod before these two was recessive, and they all
 * lean on the same two facts: a heterozygote is <b>indistinguishable from
 * normal</b>, and <b>no founder is ever affected</b>. Neither holds here.
 *
 * <p>The second one is the interesting break. A recessive disorder can be
 * absent from its own founder table entirely - a wild-caught horse is an adult
 * that survived, so it carries at most one copy, and the affected combination
 * is something you bred. A <b>dominant</b> disorder has no silent carrier at
 * all, so if founders could never be affected the allele could never enter the
 * world and the gene would not exist. So founders <i>can</i> be born
 * heterozygous, and a wild-caught horse really can be a sick one. The homozygote
 * is still excluded, which keeps the worst outcome something a breeder made.
 *
 * <p>That is not a loophole, it is what a dominant disorder <i>is</i>, and it
 * changes what the locus is for: a recessive lethal is a thing you discover
 * about two horses after the fact, while one of these is a thing you can see in
 * the animal in front of you and decline to breed from.
 *
 * <h2>Every outcome is still a wild type</h2>
 * Same as the recessive base and for the same reason:
 * {@link Expression#wildType()} means "changes nothing about the coat", and none
 * of these genes paints. What the disorder does travels on {@link Condition} and
 * {@link TraitBuilder}.
 */
public abstract class DominantDisorderGene implements Gene, HealthContribution {

    private final String key;
    private final String name;
    private final int priority;

    /** The variant copy - the one that does the damage. Declared first. */
    public final Allele variant;
    /** The working copy - the population baseline and the parsing default. */
    public final Allele baseline;

    private final List<Allele> alleles;
    private final Condition hetCondition;
    private final Condition homCondition;

    private final Expression WILD;
    private final Expression HETEROZYGOUS;
    private final Expression HOMOZYGOUS;
    private final List<Expression> expressions;

    private final FounderTable founders;

    /**
     * @param affectedPercent how many founders in a hundred are born carrying
     *                        exactly one copy - and are therefore <b>affected</b>,
     *                        because there is no silent carrier here. The rest
     *                        are {@code N/N}; no founder is ever homozygous.
     */
    protected DominantDisorderGene(String key, String name, int priority,
                                   String variantToken, String variantLabel,
                                   String baselineToken, String baselineLabel,
                                   double affectedPercent,
                                   Condition hetCondition, Condition homCondition) {
        this.key = key;
        this.name = name;
        this.priority = priority;
        this.variant = new Allele(key, 0, variantToken, variantLabel);
        this.baseline = new Allele(key, 1, baselineToken, baselineLabel);
        this.alleles = List.of(variant, baseline);
        this.hetCondition = hetCondition;
        this.homCondition = homCondition;

        this.WILD = Expression.wildType(
                "Two working copies. Nothing is wrong and nothing is passed on.");
        this.HETEROZYGOUS = Expression.wildType(hetCondition.id(), hetCondition.name(),
                hetCondition.description());
        this.HOMOZYGOUS = Expression.wildType(homCondition.id(), homCondition.name(),
                homCondition.description());
        this.expressions = List.of(WILD, HETEROZYGOUS, HOMOZYGOUS);

        this.founders = FounderTable.builder()
                .weight(variant, baseline, affectedPercent)
                .weight(baseline, baseline, 100.0 - affectedPercent)
                .build();
    }

    /** What one copy costs. The {@link Condition} is added for you. */
    protected abstract void affectHeterozygote(TraitBuilder out);

    /** What two copies cost. The {@link Condition} is added for you. */
    protected abstract void affectHomozygote(TraitBuilder out);

    // --- Gene ------------------------------------------------------------

    @Override public String key() { return key; }
    @Override public String name() { return name; }
    @Override public int priority() { return priority; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return baseline; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /** No gene carrot for a disorder locus - forcing one onto a line is hostile (roadmap &sect;14.2). */
    @Override public boolean hasGeneCarrot() { return false; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(variant)) {
            case 2 -> HOMOZYGOUS;
            case 1 -> HETEROZYGOUS;
            default -> WILD;
        };
    }

    /** True for either affected combination - <b>one</b> copy is enough. */
    public boolean isAffected(AllelePair pair) {
        return pair.count(variant) > 0;
    }

    /**
     * Deliberately absent: there is no such thing as a carrier here. The method
     * exists on {@link RecessiveDisorderGene} and its absence on this one is the
     * whole difference between the two shapes.
     */
    public Condition heterozygousCondition() {
        return hetCondition;
    }

    public Condition homozygousCondition() {
        return homCondition;
    }

    // --- TraitContribution -----------------------------------------------

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        switch (pair.count(variant)) {
            case 2 -> {
                out.condition(homCondition);
                affectHomozygote(out);
            }
            case 1 -> {
                out.condition(hetCondition);
                affectHeterozygote(out);
            }
            default -> { }
        }
    }
}
