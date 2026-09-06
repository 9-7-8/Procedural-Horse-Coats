package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;

import java.util.List;

/**
 * Shared base for the two <b>leopard-complex pattern modifiers</b>,
 * {@link Patn1Gene} and {@link Patn2Gene}. A modifier is a two-allele locus -
 * {@code PATN1} / {@code n} - that <b>paints nothing on its own</b>: every one
 * of its combinations is a {@link Expression#wildType() wild type}, so
 * {@link #affectsCoat()} is false, it collapses to one entry in the gallery,
 * and it never reaches the coat pipeline directly. What it does instead is get
 * <i>read</i> by {@link LeopardGene#expressionIn}, which is why
 * {@code LeopardGene.coatDependsOn()} names both of these and
 * {@code Genotype.coatCode()} folds their alleles into the texture key anyway.
 *
 * <h2>Founder frequency is conditional on {@code LP}</h2>
 * A modifier means nothing on a horse with no leopard complex, so a wild
 * founder only draws a variant copy of it if it already rolled at least one
 * {@code LP} at {@link LeopardGene} (which sorts earlier, so the
 * {@link FounderContext} has it). A non-{@code LP} founder gets {@code n/n}
 * every time. This keeps the wild population from carrying a drift of
 * invisible {@code PATN} that would only ever surface generations later - and,
 * combined with {@code LP} itself being rare, makes a wild spotted appaloosa a
 * genuine find rather than a inevitability.
 *
 * <p><b>Sits in the {@code 80}-{@code 99} "paints nothing" band</b> alongside
 * the performance and health loci, after {@link LeopardGene} at 73, so the
 * founder roll has the {@code LP} pair in hand by the time this one is drawn.
 */
abstract class AppaloosaModifierGene implements Gene {

    private final String key;
    private final int priority;
    private final Allele present;
    private final Allele wild;
    private final List<Allele> alleles;

    /** Given the founder carries {@code LP}: percent heterozygous, percent homozygous. */
    private final double givenLpHet;
    private final double givenLpHom;

    private final Expression WILD = Expression.wildType(
            "Carried, but silent on its own - it only shapes the pattern of a horse that also "
                    + "carries the leopard complex.");
    private final List<Expression> expressions = List.of(WILD);

    /** The whole population is {@code n/n} unless the founder rolled {@code LP} first. */
    private final FounderTable noLeopard;
    /** Given {@code LP}: {@code present} spread through the appaloosa sub-population. */
    private final FounderTable withLeopard;

    AppaloosaModifierGene(String key, int priority, String presentToken, String presentLabel,
                          double givenLpHet, double givenLpHom) {
        this.key = key;
        this.priority = priority;
        this.present = new Allele(key, 0, presentToken, presentLabel);
        this.wild = new Allele(key, 1, "n", "Wild-type (n)");
        this.alleles = List.of(present, wild);
        this.givenLpHet = givenLpHet;
        this.givenLpHom = givenLpHom;
        this.noLeopard = FounderTable.always(wild, wild);
        this.withLeopard = FounderTable.builder()
                .weight(present, present, givenLpHom)
                .weight(present, wild, givenLpHet)
                .weight(wild, wild, 100.0 - givenLpHet - givenLpHom)
                .build();
    }

    public Allele present() {
        return present;
    }

    @Override public String key() { return key; }
    @Override public int priority() { return priority; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return wild; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public Expression expressionOf(AllelePair pair) { return WILD; }

    /** True iff the horse carries at least one {@code present} copy. */
    public boolean isPresent(AllelePair pair) {
        return pair != null && pair.has(present);
    }

    /**
     * {@code n/n} for a founder with no leopard complex; the appaloosa-line
     * distribution once {@code LP} is on the horse. Reads {@link LeopardGene}'s
     * pair through the context - safe, because {@code LeopardGene} sorts
     * earlier.
     */
    @Override
    public FounderTable founderTable(FounderContext context) {
        Gene leopard = Genes.byKeyOrNull(LeopardGene.KEY);
        if (leopard != null && context.isRolled(leopard)
                && !context.expressionOf(leopard).wildType()) {
            return withLeopard;
        }
        return noLeopard;
    }

    /** A gene carrot for a modifier that does nothing without the leopard complex would be a trap. */
    @Override
    public boolean hasGeneCarrot() {
        return false;
    }
}
