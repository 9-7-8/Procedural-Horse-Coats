package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.TraitBuilder;
import com.example.horsegenetics.common.trait.TraitContribution;

import java.util.List;

/**
 * <b>PDK4</b> ({@code horsegenetics.pdk4}) - racing performance, as <b>one
 * atomic gene</b>.
 *
 * <p>In reality this is one marker in a cluster, and racing ability is a
 * polygenic sum over many of them. <b>Polygenic inheritance is cut</b> from this
 * mod ({@code wiki/roadmap.html} 6.3 / 21) and the reason is a design one rather
 * than a technical one: a hidden sum of anonymous markers is indistinguishable
 * from a dice roll at the scale a player can observe, and the whole point of the
 * genetics here is that a player can breed <i>for</i> something and see it work.
 * So the cluster collapses into two alleles with an additive weight, and a
 * player who fixes {@code A/A} in a line has visibly made it faster.
 *
 * <p>Each {@code A} copy is worth {@value #SPEED_PER_A} movement speed. Together
 * with {@link MstnGene} and {@link CkmGene} it makes speed a three-locus
 * breeding problem rather than a single switch.
 */
public final class Pdk4Gene implements Gene, TraitContribution {

    public static final String KEY = "horsegenetics.pdk4";
    public static final int PRIORITY = 81;

    /** Movement speed added per {@code A} copy. */
    public static final double SPEED_PER_A = 0.018;

    public static final double WILD_A_FREQUENCY = 0.25;

    public final Allele A = new Allele(KEY, 0, "A", "Racing (A)");
    public final Allele G = new Allele(KEY, 1, "G", "Wild-type (G)");
    private final List<Allele> alleles = List.of(A, G);

    private final Expression FAST = Expression.wildType("pdk4-fast", "PDK4 racing type",
            "Two racing copies. The full speed bonus this locus offers.");

    private final Expression MIXED = Expression.wildType("pdk4-mixed", "PDK4 intermediate",
            "One racing copy. Exactly half the bonus - the alleles add rather than one "
                    + "masking the other.");

    private final Expression PLAIN = Expression.wildType("pdk4-plain", "PDK4 wild type",
            "No racing copy. No contribution to speed.");

    private final List<Expression> expressions = List.of(FAST, MIXED, PLAIN);

    private final FounderTable founders = FounderTable.hardyWeinberg(A, G, WILD_A_FREQUENCY);

    @Override public String key() { return KEY; }
    @Override public String name() { return "PDK4"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return G; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(A)) {
            case 2 -> FAST;
            case 1 -> MIXED;
            default -> PLAIN;
        };
    }

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        out.addSpeed(pair.count(A) * SPEED_PER_A);
    }
}
