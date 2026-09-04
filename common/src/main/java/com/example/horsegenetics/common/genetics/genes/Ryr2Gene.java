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
 * <b>RYR2</b> ({@code horsegenetics.ryr2}) - <b>jumping ability</b>, the one
 * locus that moves a stat nothing in this mod tracked before.
 *
 * <p>Jump strength was on the "not tracked yet" list for as long as the stats
 * were a random roll off the parents, because there was nothing sensible to roll
 * it from. With the traits resolved out of the genotype it costs one gene and
 * one attribute write, and it gives the trait system a third axis so a breeding
 * programme has something to trade against speed.
 *
 * <p>Each {@code J} copy is worth {@value #JUMP_PER_J} jump strength, on a
 * baseline of {@code 0.5} - so a homozygous jumper clears noticeably more than a
 * wild-caught horse, and the two size loci nudge it further in both directions.
 *
 * <p>Modelled as one atomic gene for the same reason as the two speed markers -
 * see {@link Pdk4Gene}.
 */
public final class Ryr2Gene implements Gene, TraitContribution {

    public static final String KEY = "horsegenetics.ryr2";
    public static final int PRIORITY = 83;

    /** Jump strength added per {@code J} copy. */
    public static final double JUMP_PER_J = 0.09;

    public static final double WILD_J_FREQUENCY = 0.20;

    public final Allele J = new Allele(KEY, 0, "J", "Jumper (J)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(J, n);

    private final Expression JUMPER = Expression.wildType("jumper", "Jumper",
            "Two jumping copies. The full bonus to jump strength.");

    private final Expression MIXED = Expression.wildType("jumper-mixed", "Part jumper",
            "One jumping copy, half the bonus.");

    private final Expression PLAIN = Expression.wildType("jumper-plain", "Wild type",
            "No jumping copy. Baseline jump strength.");

    private final List<Expression> expressions = List.of(JUMPER, MIXED, PLAIN);

    private final FounderTable founders = FounderTable.hardyWeinberg(J, n, WILD_J_FREQUENCY);

    @Override public String key() { return KEY; }
    @Override public String name() { return "RYR2 (jumping)"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(J)) {
            case 2 -> JUMPER;
            case 1 -> MIXED;
            default -> PLAIN;
        };
    }

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        out.addJump(pair.count(J) * JUMP_PER_J);
    }
}
