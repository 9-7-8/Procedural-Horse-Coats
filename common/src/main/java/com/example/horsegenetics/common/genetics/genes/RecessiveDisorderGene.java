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
 * <b>The shape every simple recessive disorder in this mod has</b>: two
 * alleles, three combinations, and only the double-variant one does anything.
 * Six genes are exactly this and differ only in their numbers and their prose,
 * so they share it rather than being six copies of the same eighty lines.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code N/N}</td><td>wild type</td></tr>
 *   <tr><td>{@code v/N}</td><td>wild type - a <b>carrier</b>, indistinguishable from normal</td></tr>
 *   <tr><td>{@code v/v}</td><td>affected: the {@link Condition} this gene declares</td></tr>
 * </table>
 *
 * <h2>Every outcome is a wild type</h2>
 * Which reads oddly for a gene that can kill a foal, and is nevertheless
 * correct: {@link Expression#wildType()} means "changes nothing <i>about the
 * coat</i>", and none of these genes paints. That is what keeps them free -
 * {@link Gene#affectsCoat()} is false, so they are left out of a horse's
 * texture key and the whole locus collapses to one entry in the genotype
 * gallery. What the disorder actually does to the horse travels on
 * {@link Condition} and {@link TraitBuilder}, not on the expression table.
 *
 * <h2>Founders never carry two</h2>
 * The founder table lists only {@code N/N} and the carrier: a wild-caught horse
 * is an adult that survived, so it cannot be an affected foal. The homozygote's
 * weight is simply absent (the table is sparse), which means the <b>only</b>
 * way to produce one is to breed two carriers - the difference between a
 * breeding programme and a lottery, and the reason the carrier wording on
 * {@link #CARRIER} is worth showing in the info panel.
 *
 * <p>Subclasses supply the numbers by overriding {@link #affect} and, where the
 * embryo never implants at all, {@link #canOccur}.
 */
public abstract class RecessiveDisorderGene implements Gene, HealthContribution {

    private final String key;
    private final String name;
    private final int priority;

    /** The non-functional copy. Declared first, so the baseline sorts last. */
    public final Allele variant;
    /** The working copy - the population baseline and the parsing default. */
    public final Allele baseline;

    private final List<Allele> alleles;
    private final Condition condition;

    private final Expression WILD;
    /** The silent heterozygote. Its wording is the gene's whole player-facing value. */
    private final Expression CARRIER;
    private final Expression AFFECTED;
    private final List<Expression> expressions;

    private final FounderTable founders;

    /**
     * @param carrierPercent how many founders in a hundred carry exactly one
     *                       copy. The rest are {@code N/N}; no founder is ever
     *                       affected.
     * @param condition      what an affected horse has - its name, its sentence
     *                       and its {@link com.example.horsegenetics.common.trait.Severity}.
     */
    protected RecessiveDisorderGene(String key, String name, int priority,
                                    String variantToken, String variantLabel,
                                    String baselineToken, String baselineLabel,
                                    double carrierPercent, Condition condition) {
        this.key = key;
        this.name = name;
        this.priority = priority;
        this.variant = new Allele(key, 0, variantToken, variantLabel);
        this.baseline = new Allele(key, 1, baselineToken, baselineLabel);
        this.alleles = List.of(variant, baseline);
        this.condition = condition;

        this.WILD = Expression.wildType(
                "Two working copies. Nothing is wrong and nothing is passed on.");
        this.CARRIER = Expression.wildType(carrierId(), name + " carrier",
                "One copy of " + variantToken + ". The horse is completely normal and there is no way "
                        + "to see it - but half its foals inherit the copy, and two carriers bred "
                        + "together are the only way " + condition.name().toLowerCase() + " appears.");
        this.AFFECTED = Expression.wildType(affectedId(), condition.name(), condition.description());
        this.expressions = List.of(WILD, CARRIER, AFFECTED);

        this.founders = FounderTable.builder()
                .weight(variant, baseline, carrierPercent)
                .weight(baseline, baseline, 100.0 - carrierPercent)
                .build();
    }

    /** The id of the carrier outcome - {@code "<gene>-carrier"}. */
    protected String carrierId() {
        return shortKey() + "-carrier";
    }

    /** The id of the affected outcome - the condition's own id. */
    protected String affectedId() {
        return condition.id();
    }

    private String shortKey() {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? key : key.substring(dot + 1);
    }

    /**
     * What being affected costs. Called only for {@code v/v}, and only when the
     * server's health genetics are switched on - the
     * {@link Condition} is added for you, so this is purely the numbers.
     */
    protected abstract void affect(TraitBuilder out);

    // --- Gene ------------------------------------------------------------

    @Override public String key() { return key; }
    @Override public String name() { return name; }
    @Override public int priority() { return priority; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return baseline; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(variant)) {
            case 2 -> AFFECTED;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    /** True for the double-variant combination - the affected horse. */
    public boolean isAffected(AllelePair pair) {
        return pair.count(variant) == 2;
    }

    /** True for exactly one copy - a silent carrier. */
    public boolean isCarrier(AllelePair pair) {
        return pair.count(variant) == 1;
    }

    /** The disorder this gene causes. */
    public Condition condition() {
        return condition;
    }

    // --- TraitContribution -----------------------------------------------

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (isAffected(pair)) {
            out.condition(condition);
            affect(out);
        }
    }
}
