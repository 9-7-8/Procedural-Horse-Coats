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
 * <b>CKM</b> ({@code horsegenetics.ckm}), creatine kinase - the third speed
 * locus, and the smallest of the three.
 *
 * <p>Same treatment as {@link Pdk4Gene}: really a marker in a polygenic cluster,
 * modelled here as one atomic gene with an additive weight, because a legible
 * gene a player can breed for beats an accurate sum a player cannot see.
 *
 * <p>Each {@code T} copy is worth {@value #SPEED_PER_T} movement speed. It is
 * deliberately the weakest of the three speed loci and the second-rarest: the
 * last few hundredths of a horse's speed should be the hardest to find, or
 * there is nothing left to breed for once the obvious gene is fixed.
 */
public final class CkmGene implements Gene, TraitContribution {

    public static final String KEY = "horsegenetics.ckm";
    public static final int PRIORITY = 82;

    /** Movement speed added per {@code T} copy. */
    public static final double SPEED_PER_T = 0.015;

    public static final double WILD_T_FREQUENCY = 0.20;

    public final Allele T = new Allele(KEY, 0, "T", "Racing (T)");
    public final Allele C = new Allele(KEY, 1, "C", "Wild-type (C)");
    private final List<Allele> alleles = List.of(T, C);

    private final Expression FAST = Expression.wildType("ckm-fast", "CKM racing type",
            "Two racing copies. The full contribution this locus makes to speed.");

    private final Expression MIXED = Expression.wildType("ckm-mixed", "CKM intermediate",
            "One racing copy, half the contribution.");

    private final Expression PLAIN = Expression.wildType("ckm-plain", "CKM wild type",
            "No racing copy. No contribution to speed.");

    private final List<Expression> expressions = List.of(FAST, MIXED, PLAIN);

    private final FounderTable founders = FounderTable.hardyWeinberg(T, C, WILD_T_FREQUENCY);

    @Override public String key() { return KEY; }
    @Override public String name() { return "CKM"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return C; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(T)) {
            case 2 -> FAST;
            case 1 -> MIXED;
            default -> PLAIN;
        };
    }

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        out.addSpeed(pair.count(T) * SPEED_PER_T);
    }
}
