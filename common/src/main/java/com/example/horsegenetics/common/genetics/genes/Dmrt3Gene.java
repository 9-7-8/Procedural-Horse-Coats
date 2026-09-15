package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

/**
 * <b>DMRT3</b> ({@code horsegenetics.dmrt3}) - the "gait keeper", the gene behind ambling and pacing.
 *
 * <p>A stop mutation (Ser301STOP, Andersson et al. 2012) in a gene expressed in the spinal cord's locomotor
 * circuits. It is <b>permissive</b> rather than simply dominant or recessive: it lets a horse perform the extra gaits,
 * and other genes and training decide which one appears. One copy helps the tolt and keeps good walk, trot and canter;
 * two copies give the best tolt and pace and cost the diagonal gaits. Every Paso Fino, Peruvian Paso and US Standardbred
 * tested carries two copies, the Tennessee Walking Horse is near-fixed, and the Icelandic sits around 0.9; the Arabian,
 * Thoroughbred and Przewalski's horse essentially never carry it.
 *
 * <p><b>It changes nothing about movement yet</b> (owner, 2026-09-15): the gene carries the right inheritance and breed
 * frequencies and shows on the information screen, and the gait itself waits for animation. So every outcome is a
 * {@link Expression#wildType() wild type}, and the locus contributes no trait.
 *
 * <p>Natural. See {@code wiki/gene-dmrt3.html}.
 */
public final class Dmrt3Gene implements Gene {

    public static final String KEY = "horsegenetics.dmrt3";
    public static final int PRIORITY = 68;

    /**
     * The gait allele's frequency in a wild (Feral Mixed) founder. The breeds that carry it say so in their own sheets;
     * an unbred population is mostly non-gaited stock.
     */
    public static final double WILD_A_FREQUENCY = 0.05;

    public final Allele A = new Allele(KEY, 0, "A", "Gait (A)");
    public final Allele C = new Allele(KEY, 1, "C", "Wild-type (C)");
    private final List<Allele> alleles = List.of(A, C);

    private final Expression GAITED = Expression.wildType("dmrt3-gaited", "Gaited",
            "Two gait copies. The horse can amble and pace, and has the smoothest four-beat gaits, at some cost to "
                    + "its trot and canter. (The gait is not animated yet.)");
    private final Expression CARRIER = Expression.wildType("dmrt3-carrier", "One gait copy",
            "One gait copy. A better tolt than a horse with none and good diagonal gaits, but it does not pace. "
                    + "(The gait is not animated yet.)");
    private final Expression PLAIN = Expression.wildType("dmrt3-plain", "No gait copy",
            "No gait copy: walk, trot and canter, the ordinary horse.");

    private final List<Expression> expressions = List.of(GAITED, CARRIER, PLAIN);

    private final FounderTable founders = FounderTable.hardyWeinberg(A, C, WILD_A_FREQUENCY);

    @Override public String key() { return KEY; }
    @Override public String name() { return "DMRT3 (gait)"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return C; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(A)) {
            case 2 -> GAITED;
            case 1 -> CARRIER;
            default -> PLAIN;
        };
    }
}
