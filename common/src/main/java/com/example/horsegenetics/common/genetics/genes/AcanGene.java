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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>ACAN</b> ({@code horsegenetics.acan}) - <b>chondrodysplastic dwarfism</b>,
 * and the gene that proves the combination table was worth building.
 *
 * <p>Five alleles: four independent non-functional variants ({@code D1} to
 * {@code D4}) and one working copy ({@code N}). Fifteen combinations.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code N/N}</td><td>wild type</td></tr>
 *   <tr><td>one {@code D} and {@code N}</td><td>wild type - a carrier, invisible</td></tr>
 *   <tr><td>{@code D1/D1}</td><td>{@code acan-lethal} - the severe form; the foal dies</td></tr>
 *   <tr><td>any other two {@code D}s</td><td>{@code acan-dwarf} - a small, weak, surviving horse</td></tr>
 * </table>
 *
 * <h2>Why this is not "is it homozygous?"</h2>
 * A {@code D1/D4} horse is affected. So is {@code D2/D3}. The four variants are
 * different broken versions of the same gene, and what matters is that the horse
 * has <b>no working copy left</b>, not that its two copies match. Every previous
 * disorder in this mod could be written as "count the variant allele and check
 * for two"; this one cannot, and the check has to be a predicate over the whole
 * pair. It is the clearest argument in the model for a combination table over a
 * dominance label - "recessive" describes nine of these ten affected
 * combinations and quietly loses the compound heterozygotes.
 *
 * <p>The consequence for a breeder is the interesting part: two carriers of
 * <i>different</i> variants are just as dangerous as two carriers of the same
 * one, so "my two lines carry different mutations" is not the safety it sounds
 * like.
 *
 * <h2>The founder table</h2>
 * Built with the multi-allele Hardy-Weinberg helper at a frequency per variant,
 * with <b>every</b> affected combination excluded and the rest rescaled - which
 * is the biology as well as the bookkeeping: a wild-caught horse is an adult
 * that survived, and an affected foal mostly did not. Ten of the fifteen
 * combinations therefore have weight zero, and the only way to make one is to
 * breed two carriers.
 */
public final class AcanGene implements Gene, HealthContribution {

    public static final String KEY = "horsegenetics.acan";
    public static final int PRIORITY = 86;

    /** Population frequency of <b>each</b> of the four broken variants. */
    public static final double VARIANT_FREQUENCY = 0.004;

    public static final Condition DWARFISM = Condition.impairing(
            "chondrodysplastic-dwarfism", "Chondrodysplastic dwarfism",
            "No working copy of ACAN. The horse is markedly small with shortened limbs, "
                    + "moves badly, jumps badly, and has far fewer hearts than it should.");

    public static final Condition LETHAL_DWARFISM = Condition.lethalAtBirth(
            "chondrodysplastic-dwarfism-lethal", "Lethal dwarfism (D1/D1)",
            "Two copies of the severe D1 variant. The foal is born profoundly malformed "
                    + "and does not survive.");

    public final Allele D1 = new Allele(KEY, 0, "D1", "Dwarfism D1 (severe)");
    public final Allele D2 = new Allele(KEY, 1, "D2", "Dwarfism D2");
    public final Allele D3 = new Allele(KEY, 2, "D3", "Dwarfism D3");
    public final Allele D4 = new Allele(KEY, 3, "D4", "Dwarfism D4");
    public final Allele N = new Allele(KEY, 4, "N", "Wild-type (N)");

    private final List<Allele> alleles = List.of(D1, D2, D3, D4, N);

    private final Expression WILD = Expression.wildType(
            "Two working copies of ACAN. Normal skeletal growth.");

    private final Expression CARRIER = Expression.wildType(
            "acan-carrier", "ACAN carrier",
            "One broken copy and one working one. The horse is a normal size and there is no "
                    + "way to see it. Two carriers bred together throw a dwarf one time in four - "
                    + "and it does not matter whether they carry the same variant.");

    private final Expression DWARF = Expression.wildType(
            "acan-dwarf", DWARFISM.name(), DWARFISM.description());

    private final Expression LETHAL = Expression.wildType(
            "acan-lethal", LETHAL_DWARFISM.name(), LETHAL_DWARFISM.description());

    private final List<Expression> expressions = List.of(WILD, CARRIER, DWARF, LETHAL);

    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), pair -> !isAffected(pair));

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> f = new LinkedHashMap<>();
        f.put(D1, VARIANT_FREQUENCY);
        f.put(D2, VARIANT_FREQUENCY);
        f.put(D3, VARIANT_FREQUENCY);
        f.put(D4, VARIANT_FREQUENCY);
        f.put(N, 1.0 - 4.0 * VARIANT_FREQUENCY);
        return f;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "ACAN"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(D1)) {
            return LETHAL;
        }
        if (isAffected(pair)) {
            return DWARF;
        }
        return pair.count(N) == 2 ? WILD : CARRIER;
    }

    /** <b>No working copy left</b> - the real check, and not "are the two alleles equal". */
    public boolean isAffected(AllelePair pair) {
        return pair.count(N) == 0;
    }

    /** Exactly one broken copy: normal to look at, dangerous to breed blind. */
    public boolean isCarrier(AllelePair pair) {
        return pair.count(N) == 1;
    }

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (!isAffected(pair)) {
            return;
        }
        if (pair.homozygousFor(D1)) {
            out.condition(LETHAL_DWARFISM).addHealth(-14.0).multiplyScale(0.62)
                    .addSpeed(-0.06).addJump(-0.28);
        } else {
            out.condition(DWARFISM).addHealth(-6.0).multiplyScale(0.70)
                    .addSpeed(-0.030).addJump(-0.12);
        }
    }
}
