package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CreamPearlDilution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Cream</b> ({@code horsegenetics.cream}). {@code Cr} is an <b>incomplete
 * dominant</b> dilution; {@code N} is wild-type. Real-horse {@code SLC45A2}, so
 * it interacts with {@link PearlGene} - the combined rule lives in
 * {@link CreamPearlDilution} and this gene is the driver <b>whenever a
 * {@code Cr} is present</b>:
 * <ul>
 *   <li>{@code Cr/N} - single cream: red only (buckskin on bay).</li>
 *   <li>{@code Cr/Cr} - double cream: red + black, severe (perlino on bay).</li>
 *   <li>{@code Cr/prl} - complements pearl, acts as double cream.</li>
 * </ul>
 * Natural, deterministic. {@code 1 in} {@value #WILD_CREAM_ALLELE_ODDS} per allele.
 */
public final class CreamGene implements Gene {

    public static final String KEY = "horsegenetics.cream";
    public static final int WILD_CREAM_ALLELE_ODDS = 30;

    public final Allele Cr = new Allele(KEY, "Cr", "Cream (Cr)", true, true);
    public final Allele N = new Allele(KEY, "N", "Wild-type (N)", false, true);
    private final List<Allele> alleles = List.of(Cr, N);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return N; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_CREAM_ALLELE_ODDS) == 0 ? Cr : N,
                rng.nextInt(WILD_CREAM_ALLELE_ODDS) == 0 ? Cr : N);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return CreamPearlDilution.creamDose(genotype) >= 1;
    }

    @Override
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        CreamPearlDilution.apply(ctx); // reads cream + pearl together
    }
}
