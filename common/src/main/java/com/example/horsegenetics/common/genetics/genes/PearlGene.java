package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CreamPearlDilution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Pearl</b> ({@code horsegenetics.pearl}). {@code prl} is <b>recessive</b>;
 * {@code N} wild-type. One copy does nothing. Two copies (with <b>no cream</b>)
 * = a mild, uniform dilution of both pigments (Classic Pearl - apricot body,
 * sepia points). With cream present, {@link CreamGene} is the driver
 * ({@code Cr/prl} acts as double cream) and this gene is inert.
 *
 * <p>Natural, deterministic. {@code 1 in} {@value #WILD_PEARL_ALLELE_ODDS} per allele.
 */
public final class PearlGene implements Gene {

    public static final String KEY = "horsegenetics.pearl";
    public static final int WILD_PEARL_ALLELE_ODDS = 22;

    public final Allele prl = new Allele(KEY, "prl", "Pearl (prl)", true, true);
    public final Allele N = new Allele(KEY, "N", "Wild-type (N)", false, true);
    private final List<Allele> alleles = List.of(prl, N);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return N; }

    /** IncompleteDominant: {@code prl/prl} is the mild uniform dilution and {@code Cr/prl} a double cream - the heterozygote is its own thing. */
    @Override public DominancePattern dominance() { return DominancePattern.INCOMPLETE_DOMINANT; }

    @Override
    public int precedence(Allele allele) {
        // prl is recessive; put N "first" so N/prl canonicalizes as N/prl (visible allele last)
        return allele.equals(N) ? 0 : 1;
    }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_PEARL_ALLELE_ODDS) == 0 ? prl : N,
                rng.nextInt(WILD_PEARL_ALLELE_ODDS) == 0 ? prl : N);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        // pearl only "drives" when there's no cream and it's homozygous
        return CreamPearlDilution.creamDose(genotype) == 0 && CreamPearlDilution.pearlDose(genotype) == 2;
    }

    @Override
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        CreamPearlDilution.apply(ctx);
    }
}
