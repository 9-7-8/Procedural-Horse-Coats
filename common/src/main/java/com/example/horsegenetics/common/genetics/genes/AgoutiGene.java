package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BayCoat;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Agouti</b> ({@code horsegenetics.agouti}) - <b>two alleles only</b>.
 * {@code A} (dominant) restricts black to the <b>points</b> - a bay horse:
 * red-brown body, black mane / tail / ears / hooves, and black that climbs a
 * random amount up the legs and face. {@code a} (recessive, wild-type) = no
 * restriction, black everywhere. Natural. {@code A_} is
 * <b>non-deterministic</b> (the random point heights are the epigenetic value);
 * {@code aa} is deterministic.
 *
 * <p>(Seal brown is its own {@code horsegenetics.seal} gene, not an agouti
 * allele.)
 */
public final class AgoutiGene implements Gene {

    public static final String KEY = "horsegenetics.agouti";

    public final Allele A = new Allele(KEY, "A", "Agouti / bay (A)", true, false);
    public final Allele a = new Allele(KEY, "a", "Non-agouti (a)", false, true);
    private final List<Allele> alleles = List.of(A, a);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return a; }

    @Override
    public AllelePair randomPair(Rng rng) {
        // ~50/50 per allele
        return new AllelePair(rng.nextBoolean() ? A : a, rng.nextBoolean() ? A : a);
    }

    public boolean isBay(AllelePair pair) {
        return pair.has(A);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isBay(pair) && genotype.hasBlackPigment();
    }

    @Override
    public boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return !isVisible(pair, genotype); // bay carries random point heights
    }

    @Override
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        if (isBay(pair)) {
            BayCoat.apply(ctx, ctx.epigeneticsFor(KEY));
        }
    }
}
