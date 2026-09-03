package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BayCoat;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Agouti</b> ({@code horsegenetics.agouti}) - two alleles. {@code A}
 * (dominant) restricts black to the <b>points</b>: red-brown body, black
 * mane / tail / ears / hooves, and black that climbs the legs + face a
 * <b>random</b> amount (fading out at its top edge). {@code a} (recessive,
 * wild-type) = no restriction, black everywhere. Natural. {@code A_} on a
 * black-capable horse is <b>non-deterministic</b> - the two point heights (one
 * for all four legs, one for the face) are the horse's epigenetic value.
 *
 * <p><b>Seal brown</b> is just a high roll of those heights - the "black creeps
 * most of the way up" look. There is no separate seal gene / allele.
 */
public final class AgoutiGene implements Gene {

    public static final String KEY = "horsegenetics.agouti";

    public final Allele A = new Allele(KEY, "A", "Agouti / bay (A)", true, false);
    public final Allele a = new Allele(KEY, "a", "Non-agouti (a)", false, true);
    private final List<Allele> alleles = List.of(A, a);

    @Override public String key() { return KEY; }
    @Override public int priority() { return 20; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return a; }

    /** Dominant: one {@code A} is enough for bay; {@code Aa} and {@code AA} are the same horse. */
    @Override public DominancePattern dominance() { return DominancePattern.DOMINANT; }

    @Override
    public AllelePair randomPair(Rng rng) {
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
        return !isVisible(pair, genotype);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isBay(pair)) {
            return null;
        }
        PigmentField f = coat.mutableCopy();
        BayCoat.apply(ctx, f, ctx.epigeneticsFor(KEY));
        return f;
    }
}
