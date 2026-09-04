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
 * <b>LCORL / NCAPG</b> ({@code horsegenetics.lcorl}) - <b>body height</b>, the
 * larger of the two size loci. The single biggest determinant of how tall a real
 * horse is, and the first gene in this mod that changes how big a horse looks
 * without touching a pixel of its coat.
 *
 * <p><b>How the size is drawn:</b> {@code Attributes.SCALE}, which exists on
 * every living entity in 26.1.2 and scales the model <i>and</i> the hitbox
 * together. That answers the question the roadmap flagged as unverified - no
 * renderer-side scaling and no hand-written hitbox change are needed, and the
 * whole size system is one attribute write in the server adapter.
 *
 * <p>Each {@code L} copy adds {@value #SCALE_PER_L} to the scale, plus a little
 * speed and jump: a taller horse covers more ground per stride and clears more.
 * Those side effects are what stop the size loci being pure decoration, and they
 * are what makes {@link Hmga2Gene}'s pony allele a real trade rather than a
 * strict downgrade.
 */
public final class LcorlGene implements Gene, TraitContribution {

    public static final String KEY = "horsegenetics.lcorl";
    public static final int PRIORITY = 84;

    /** Body scale added per {@code L} copy, on a baseline of 1.0. */
    public static final double SCALE_PER_L = 0.05;
    /** Movement speed added per {@code L} copy - longer stride. */
    public static final double SPEED_PER_L = 0.010;
    /** Jump strength added per {@code L} copy. */
    public static final double JUMP_PER_L = 0.02;

    public static final double WILD_L_FREQUENCY = 0.30;

    public final Allele L = new Allele(KEY, 0, "L", "Tall (L)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(L, n);

    private final Expression TALL = Expression.wildType("lcorl-tall", "Tall",
            "Two tall copies. A visibly larger horse, a longer stride and a higher jump.");

    private final Expression MIXED = Expression.wildType("lcorl-mixed", "Part tall",
            "One tall copy. Half the height, half the stride - the alleles add.");

    private final Expression PLAIN = Expression.wildType("lcorl-plain", "Wild type",
            "No tall copy. Ordinary height.");

    private final List<Expression> expressions = List.of(TALL, MIXED, PLAIN);

    private final FounderTable founders = FounderTable.hardyWeinberg(L, n, WILD_L_FREQUENCY);

    @Override public String key() { return KEY; }
    @Override public String name() { return "LCORL / NCAPG (height)"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(L)) {
            case 2 -> TALL;
            case 1 -> MIXED;
            default -> PLAIN;
        };
    }

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        int copies = pair.count(L);
        out.addScale(copies * SCALE_PER_L)
                .addSpeed(copies * SPEED_PER_L)
                .addJump(copies * JUMP_PER_L);
    }
}
