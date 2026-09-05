package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.SpecPainter;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Gene} whose behaviour comes from a {@link GeneSpec} instead of from
 * Java. Everything the interface asks - alleles, the combination table,
 * inheritance, the founder distribution, the coat hooks - is answered from the
 * spec, so a gene that fits the format is a file rather than a class.
 *
 * <p>It is an ordinary gene in every other respect: it goes in the same
 * registry, takes the same place in the genotype code, breeds by the same
 * Mendelian draw, and its non-deterministic numbers come off the same
 * per-allele epigenetic seed. Nothing downstream knows or cares that it was
 * loaded from a file.
 *
 * <p>The spec's {@code expressions} table is turned into real
 * {@link Expression} objects at construction, one per entry, each carrying a
 * painter that runs that entry's layers through {@link SpecPainter}. A
 * combination is then resolved by a map lookup, which is why the parser insists
 * the table cover every combination exactly once.
 */
public final class SpecGene implements Gene {

    private final GeneSpec spec;
    private final List<Allele> alleles;
    private final Allele baseline;
    private final Allele variant;
    private final List<Expression> expressions;
    /** Canonical {@code "<a>/<b>"} to the outcome it produces - total, by construction. */
    private final Map<String, Expression> byCombination;
    /** Same key, back to the spec entry - what {@link HorseAbilities} needs. */
    private final Map<String, GeneSpec.ExpressionSpec> specByCombination;
    private final FounderTable founders;

    public SpecGene(GeneSpec spec) {
        this.spec = spec;

        List<Allele> built = new ArrayList<>();
        for (int i = 0; i < spec.alleles().size(); i++) {
            GeneSpec.AlleleSpec a = spec.alleles().get(i);
            built.add(new Allele(spec.key(), i, a.token(), a.label()));
        }
        this.alleles = List.copyOf(built);
        this.variant = alleles.get(0);
        this.baseline = alleles.get(alleles.size() - 1);

        List<Expression> outcomes = new ArrayList<>();
        Map<String, Expression> byCombo = new LinkedHashMap<>();
        Map<String, GeneSpec.ExpressionSpec> specByCombo = new LinkedHashMap<>();
        for (GeneSpec.ExpressionSpec e : spec.expressions()) {
            Expression expression = toExpression(spec, e);
            outcomes.add(expression);
            for (String combination : e.combinations()) {
                byCombo.put(combination, expression);
                specByCombo.put(combination, e);
            }
        }
        this.expressions = List.copyOf(outcomes);
        this.byCombination = Map.copyOf(byCombo);
        this.specByCombination = Map.copyOf(specByCombo);

        FounderTable.Builder table = FounderTable.builder();
        for (GeneSpec.FounderWeight w : spec.founders()) {
            String[] tokens = w.combination().split("/");
            table.weight(fromToken(tokens[0]), fromToken(tokens[1]), w.percent());
        }
        this.founders = table.build();
    }

    private Expression toExpression(GeneSpec spec, GeneSpec.ExpressionSpec e) {
        if (e.wildType()) {
            return Expression.wildType(e.id(), e.name(), e.description());
        }
        Expression.Builder b = Expression.of(e.id(), e.name()).describe(e.description());
        if (e.masks()) {
            b = b.masking();
        }
        if (!e.deterministic()) {
            b = b.varies();
        }
        // An expression carrying only effects still isn't a wild type - it
        // changes the horse, just not its coat - so it gets a painter that
        // contributes nothing rather than being folded into the wild type.
        return spec.natural()
                ? b.restrict((ctx, coat) -> e.layers().isEmpty()
                        ? null
                        : SpecPainter.restrict(spec, e.layers(), values(ctx), ctx, coat))
                : b.tint((ctx, coat, accumulated) -> e.layers().isEmpty()
                        ? null
                        : SpecPainter.tint(spec, e.layers(), values(ctx), ctx, coat, accumulated));
    }

    private SpecValues values(CoatBuildContext ctx) {
        AllelePair pair = ctx.genotype().pair(spec.key());
        return SpecValues.draw(spec, ctx.epigeneticsFor(spec.key()), pair == null ? 0 : dose(pair));
    }

    public GeneSpec spec() {
        return spec;
    }

    /**
     * The gene's processing priority - see {@link Gene#priority()}. A spec gene
     * sorts into the one unified {@code (priority, key)} order alongside the
     * built-ins, so a data-driven natural gene at priority 35 lands between the
     * built-in silver and MATP.
     */
    @Override
    public int priority() {
        return spec.priority();
    }

    @Override public String key() { return spec.key(); }

    @Override public String name() { return spec.name(); }

    @Override public List<Allele> alleles() { return alleles; }

    @Override public Allele defaultAllele() { return baseline; }

    @Override public boolean isNatural() { return spec.natural(); }

    @Override public List<Expression> expressions() { return expressions; }

    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        Expression e = byCombination.get(pair.toTokens());
        if (e == null) {
            // Unreachable: the parser proves the table is total over every
            // combination. Loud rather than silent if that ever stops holding.
            throw new IllegalStateException(spec.key() + " has no expression for " + pair.toTokens());
        }
        return e;
    }

    /** The spec entry behind {@code pair} - the effects list the translator walks. */
    public GeneSpec.ExpressionSpec expressionSpecOf(AllelePair pair) {
        return specByCombination.get(pair.toTokens());
    }

    /** How many copies of the <b>first-declared</b> allele this horse carries - what {@code perDose} counts. */
    public int dose(AllelePair pair) {
        return pair.count(variant);
    }

    @Override
    public String toString() {
        return "SpecGene[" + spec.key() + "]";
    }
}
