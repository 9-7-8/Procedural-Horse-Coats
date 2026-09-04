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
 * <b>HMGA2</b> ({@code horsegenetics.hmga2}) - the <b>pony</b> locus, the second
 * of the two size genes and the one that pulls the other way.
 *
 * <p>Each {@code p} copy takes {@value #SCALE_PER_P} off the body scale along
 * with a little speed and jump, and gives {@value #HEALTH_PER_P} health back.
 * That last number is the whole point of the gene. A pony is not a broken horse,
 * it is a hardy one, so the small end of the size range has to buy something or
 * nobody would ever breed toward it - and a locus whose only effect is "worse"
 * is a locus a player fixes once and never thinks about again.
 *
 * <p>Between this and {@link LcorlGene} the healthy size range runs from roughly
 * {@code 0.88} to {@code 1.10} of a vanilla horse, with the dwarfism loci
 * ({@link AcanGene}, {@link B4galt7Gene}) reaching further down by multiplying
 * rather than subtracting - so a dwarf pony is smaller than either alone, which
 * is right, and a dwarf draught horse is still unmistakably a dwarf.
 *
 * <p><b>This is not dwarfism</b>, and the model keeps them apart on purpose:
 * being a pony is a size, being a dwarf is a disorder. Only the second reports a
 * {@link com.example.horsegenetics.common.trait.Condition}, only the second is
 * suppressed when the health genetics are switched off, and only the second
 * costs the horse hearts.
 */
public final class Hmga2Gene implements Gene, TraitContribution {

    public static final String KEY = "horsegenetics.hmga2";
    public static final int PRIORITY = 85;

    /** Body scale removed per {@code p} copy. */
    public static final double SCALE_PER_P = 0.06;
    /** Movement speed removed per {@code p} copy - shorter stride. */
    public static final double SPEED_PER_P = 0.008;
    /** Jump strength removed per {@code p} copy. */
    public static final double JUMP_PER_P = 0.02;
    /** Max health added per {@code p} copy - what makes small worth breeding for. */
    public static final double HEALTH_PER_P = 2.0;

    public static final double WILD_P_FREQUENCY = 0.25;

    public final Allele p = new Allele(KEY, 0, "p", "Pony (p)");
    public final Allele N = new Allele(KEY, 1, "N", "Wild-type (N)");
    private final List<Allele> alleles = List.of(p, N);

    private final Expression PONY = Expression.wildType("pony", "Pony",
            "Two pony copies. A visibly smaller, slower, lower-jumping horse - and the "
                    + "hardiest one this locus makes.");

    private final Expression MIXED = Expression.wildType("pony-mixed", "Part pony",
            "One pony copy. Half the loss of height and half the gain in hearts.");

    private final Expression PLAIN = Expression.wildType("pony-plain", "Wild type",
            "No pony copy. Ordinary height.");

    private final List<Expression> expressions = List.of(PONY, MIXED, PLAIN);

    private final FounderTable founders = FounderTable.hardyWeinberg(p, N, WILD_P_FREQUENCY);

    @Override public String key() { return KEY; }
    @Override public String name() { return "HMGA2 (pony)"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(p)) {
            case 2 -> PONY;
            case 1 -> MIXED;
            default -> PLAIN;
        };
    }

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        int copies = pair.count(p);
        out.addScale(-copies * SCALE_PER_P)
                .addSpeed(-copies * SPEED_PER_P)
                .addJump(-copies * JUMP_PER_P)
                .addHealth(copies * HEALTH_PER_P);
    }
}
