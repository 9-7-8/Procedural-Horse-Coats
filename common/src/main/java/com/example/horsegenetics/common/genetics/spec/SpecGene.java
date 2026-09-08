package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatOverlay;
import com.example.horsegenetics.common.coat.pattern.CoatOverlayContribution;
import com.example.horsegenetics.common.coat.pattern.SpecPainter;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;

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
 * Mendelian draw, and its varying numbers are stored on the allele copy the
 * same way. Nothing downstream knows or cares that it was
 * loaded from a file.
 *
 * <p>The spec's {@code expressions} table is turned into real
 * {@link Expression} objects at construction, one per entry, each carrying a
 * painter that runs that entry's layers through {@link SpecPainter}. A
 * combination is then resolved by a map lookup, which is why the parser insists
 * the table cover every combination exactly once.
 */
public final class SpecGene implements Gene, CoatOverlayContribution {

    private final GeneSpec spec;
    private final List<Allele> alleles;
    private final Allele baseline;
    private final Allele variant;
    private final List<Expression> expressions;
    /** Canonical {@code "<a>/<b>"} to the outcome it produces - total, by construction. */
    private final Map<String, Expression> byCombination;
    /** Same key, back to the spec entry - what {@link HorseAbilities} needs. */
    private final Map<String, GeneSpec.ExpressionSpec> specByCombination;
    /**
     * The entries carrying {@code needs}, in file order, each with the
     * {@link Expression} it builds. They are consulted <b>before</b> the two
     * maps above, and the first whose combination and whose second locus both
     * agree wins - see {@link #expressionIn}.
     */
    private final List<Conditional> conditionals;
    /** Every other gene any {@code needs} names - {@link Gene#coatDependsOn()}. */
    private final List<String> dependsOn;
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
        List<Conditional> conditional = new ArrayList<>();
        List<String> depends = new ArrayList<>();
        for (GeneSpec.ExpressionSpec e : spec.expressions()) {
            Expression expression = toExpression(spec, e);
            outcomes.add(expression);
            if (e.conditional()) {
                conditional.add(new Conditional(e, expression));
                for (GeneSpec.LocusCondition c : e.needs()) {
                    if (!depends.contains(c.gene())) {
                        depends.add(c.gene());
                    }
                }
                continue;
            }
            for (String combination : e.combinations()) {
                byCombo.put(combination, expression);
                specByCombo.put(combination, e);
            }
        }
        this.expressions = List.copyOf(outcomes);
        this.byCombination = Map.copyOf(byCombo);
        this.specByCombination = Map.copyOf(specByCombo);
        this.conditionals = List.copyOf(conditional);
        this.dependsOn = List.copyOf(depends);

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

    /**
     * The overlay pass: hand {@link SpecPainter} the expressing outcome's
     * layers so the {@code emissive} ones can light their texels.
     *
     * <p>Every spec gene carries this hook, and almost every one of them does
     * nothing in it - the early return is the normal case. It is cheaper to
     * check a flag per gene than to make the registry sort genes by whether
     * they glow.
     */
    @Override
    public void overlay(AllelePair pair, CoatBuildContext ctx, CoatOverlay out) {
        GeneSpec.ExpressionSpec e = expressionSpecIn(pair, ctx.genotype());
        if (e == null || !hasEmissiveLayer(e)) {
            return;
        }
        SpecPainter.emissive(spec, e.layers(), values(ctx), ctx.skin(), out);
    }

    private static boolean hasEmissiveLayer(GeneSpec.ExpressionSpec e) {
        for (GeneSpec.Layer layer : e.layers()) {
            if (layer.emissive()) {
                return true;
            }
        }
        return false;
    }

    private SpecValues values(CoatBuildContext ctx) {
        AllelePair pair = ctx.genotype().pair(spec.key());
        return SpecValues.read(spec, ctx.epigeneticsFor(spec.key()), pair == null ? 0 : dose(pair));
    }

    /**
     * The spec's knobs, as stored values - see {@link SpecValues#schema}. A
     * data-driven gene needed almost nothing here: the format already declared
     * its varying numbers by name and with a range, which is exactly what an
     * {@link EpiSchema} is.
     */
    @Override
    public EpiSchema epiSchema() {
        return SpecValues.schema(spec);
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

    @Override
    public String description() {
        return spec.blurb().isBlank() ? Gene.super.description() : spec.blurb();
    }

    @Override public com.example.horsegenetics.common.genetics.GeneRarity rarity() { return spec.rarity(); }

    @Override public boolean hasGeneCarrot() { return spec.carrot().enabled(); }

    @Override public boolean geneCarrotHomozygous() { return spec.carrot().homozygous(); }

    @Override
    public java.util.Optional<FounderTable> spliceTable() {
        if (spec.splice().isEmpty()) {
            return java.util.Optional.empty();
        }
        FounderTable.Builder table = FounderTable.builder();
        for (GeneSpec.FounderWeight w : spec.splice()) {
            String[] tokens = w.combination().split("/");
            table.weight(fromToken(tokens[0]), fromToken(tokens[1]), w.percent());
        }
        return java.util.Optional.of(table.build());
    }

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

    /** The same, reading any second locus a {@code needs} names. */
    public GeneSpec.ExpressionSpec expressionSpecIn(AllelePair pair, Genotype genotype) {
        Conditional c = matching(pair, genotype);
        return c == null ? expressionSpecOf(pair) : c.spec();
    }

    /**
     * The outcome, <b>reading the loci this gene's {@code needs} name</b>.
     *
     * <p>This is what the coat pipeline calls, and the only place a spec gene
     * is polygenic. {@link #expressionOf} answers the same question without a
     * genotype to read, and so gives the unconditional outcome - which is the
     * right answer for a UI listing what a locus can do on its own, and the
     * wrong one for painting a horse.
     */
    @Override
    public Expression expressionIn(AllelePair pair, Genotype genotype) {
        Conditional c = matching(pair, genotype);
        return c == null ? expressionOf(pair) : c.expression();
    }

    /** Which second locus, if any, this gene's outcome depends on. */
    @Override
    public List<String> coatDependsOn() {
        return dependsOn;
    }

    private Conditional matching(AllelePair pair, Genotype genotype) {
        if (conditionals.isEmpty() || genotype == null) {
            return null;
        }
        String tokens = pair.toTokens();
        for (Conditional c : conditionals) {
            if (c.spec().combinations().contains(tokens) && satisfied(c.spec(), genotype)) {
                return c;
            }
        }
        return null;
    }

    private static boolean satisfied(GeneSpec.ExpressionSpec e, Genotype genotype) {
        for (GeneSpec.LocusCondition need : e.needs()) {
            Gene other = Genes.byKeyOrNull(need.gene());
            if (other == null) {
                return false;
            }
            AllelePair at = genotype.pair(other);
            if (at == null) {
                return false;
            }
            for (Map.Entry<String, Integer> want : need.copies().entrySet()) {
                Allele allele = tokenOf(other, want.getKey());
                if (allele == null || at.count(allele) < want.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Allele tokenOf(Gene gene, String token) {
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        return null;
    }

    /** One {@code needs}-carrying entry, and the outcome it produces. */
    private record Conditional(GeneSpec.ExpressionSpec spec, Expression expression) {}

    /** How many copies of the <b>first-declared</b> allele this horse carries - what {@code perDose} counts. */
    public int dose(AllelePair pair) {
        return pair.count(variant);
    }

    @Override
    public String toString() {
        return "SpecGene[" + spec.key() + "]";
    }
}
