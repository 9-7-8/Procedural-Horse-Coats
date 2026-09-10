package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Many alleles, and you need the same one twice.</b> The shape
 * {@link LycanGene} established and three more loci wanted: a locus with dozens
 * of variants, every one recessive to the wild type <i>and to each other</i>, so
 * only a matched pair does anything at all.
 *
 * <h2>Why a matched pair rather than "any variant shows"</h2>
 * Lycan's own note puts it best and the argument carries here: a locus with
 * thirty-seven alleles where any one of them showed would be a slot machine; a
 * locus where you must find the <i>same</i> allele twice is a search with a
 * target. Two carriers caught in different biomes are probably carrying
 * different things, and their foals are ordinary - the disappointment is the
 * game.
 *
 * <h2>Founders carry, and never show</h2>
 * Every locus on this base is {@code CARRIERS_ONLY}: the wild population carries
 * variants and never expresses one, so the first expressing horse in a world is
 * always something somebody bred. That makes these the clearest argument the
 * gene database has for existing, since testing is the only way to hunt one -
 * and it is why {@link #wildCarrierPercent} is deliberately generous compared
 * with a two-allele locus. With this many variants, a carrier rate that felt
 * reasonable per-allele would mean no two carriers ever matched.
 *
 * <h2>Paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type}.
 */
public abstract class AbstractMatchedPairGene implements Gene, EpigeneticAbilityContribution {

    /** One variant: its allele, the thing it names, and the label a player reads. */
    public record Variant(Allele allele, String subject, String label) {
    }

    private final String key;
    private final int priority;
    private final String displayName;

    private final List<Variant> variants = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;
    private final List<Expression> expressions;

    /** Indexed by allele order; {@code null} at the wild type's slot. */
    private final Expression[] matched;

    private final Expression wild;
    private final Expression carrier;
    private final Expression mismatched;

    private final FounderTable founders;

    protected AbstractMatchedPairGene(String key, int priority, String displayName,
                                      List<Variant0> subjects, double wildCarrierPercent,
                                      String wildText, String carrierText, String mismatchedText,
                                      MatchedText matchedText) {
        this.key = key;
        this.priority = priority;
        this.displayName = displayName;

        for (Variant0 v : subjects) {
            Allele a = new Allele(key, variants.size(), v.token(),
                    v.label() + " (" + v.token() + ")");
            variants.add(new Variant(a, v.subject(), v.label()));
        }
        this.n = new Allele(key, variants.size(), "n", "Wild-type (n)");

        List<Allele> all = new ArrayList<>(variants.size() + 1);
        for (Variant v : variants) {
            all.add(v.allele());
        }
        all.add(n);
        this.alleles = List.copyOf(all);

        this.wild = Expression.wildType(wildText);
        this.carrier = Expression.wildType(idPrefix() + "-carrier", displayName + " carrier",
                carrierText);
        this.mismatched = Expression.wildType(idPrefix() + "-mismatched", "Mismatched pair",
                mismatchedText);

        List<Expression> out = new ArrayList<>();
        out.add(wild);
        out.add(carrier);
        out.add(mismatched);
        this.matched = new Expression[alleles.size()];
        for (Variant v : variants) {
            Expression e = Expression.wildType(idPrefix() + "-" + v.allele().token(),
                    matchedText.name(v), matchedText.description(v));
            this.matched[v.allele().order()] = e;
            out.add(e);
        }
        this.expressions = List.copyOf(out);

        // Carriers only, spread evenly across the variants: an expressing horse
        // is always one somebody bred.
        FounderTable.Builder b = FounderTable.builder();
        double each = wildCarrierPercent / Math.max(1, variants.size());
        for (Variant v : variants) {
            b.weight(v.allele(), n, each);
        }
        this.founders = b.weight(n, n, 100.0 - wildCarrierPercent).build();
    }

    /** What a subject looks like before its allele exists. */
    public record Variant0(String token, String subject, String label) {
    }

    /** How a matched pair is described. Two sentences per variant, built from the subject. */
    public interface MatchedText {
        String name(Variant v);

        String description(Variant v);
    }

    /** Prefix for every expression id this locus makes. */
    protected abstract String idPrefix();

    /**
     * What the horse does with a matched pair. Called only for a matched pair,
     * so an implementation never has to test for the wild type or a mismatch.
     */
    protected abstract List<GeneAbility> abilitiesFor(Variant matchedVariant, EpiValues epi);

    /** The variant a matched pair names, or {@code null} for every other combination. */
    public final Variant matchedVariant(AllelePair pair) {
        int a = pair.first().order();
        int b = pair.second().order();
        if (a != b || a == n.order()) {
            return null;
        }
        return variants.get(a);
    }

    /** Every variant this locus offers, in allele order. */
    public final List<Variant> variants() {
        return List.copyOf(variants);
    }

    public final Allele wildType() {
        return n;
    }

    @Override public String key() { return key; }
    @Override public String name() { return displayName; }
    @Override public int priority() { return priority; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int a = pair.first().order();
        int b = pair.second().order();
        if (a == n.order()) {
            return wild;                 // n/n
        }
        if (b == n.order()) {
            return carrier;              // one variant, one wild type
        }
        return a == b ? matched[a] : mismatched;
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.EMPTY;
    }

    @Override
    public final List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                                GeneEpigenetics epigenetics) {
        Variant v = matchedVariant(pair);
        return v == null ? List.of() : abilitiesFor(v, epigenetics.copy(0));
    }

    /** Convenience for a subclass building its subject list from a table. */
    protected static Map<String, String> index(List<Variant0> list) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Variant0 v : list) {
            out.put(v.token(), v.subject());
        }
        return out;
    }
}
