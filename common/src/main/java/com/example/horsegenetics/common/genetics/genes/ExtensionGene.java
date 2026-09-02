package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Extension</b> ({@code horsegenetics.extension}) - can this horse make black
 * pigment at all. {@code E} dominant/wild-type = yes (no effect); {@code ee} =
 * black completely restricted -> chestnut (only red pheomelanin survives).
 * Natural, deterministic. {@code E} is <b>dominant</b>, {@code e} recessive.
 */
public final class ExtensionGene implements Gene {

    public static final String KEY = "horsegenetics.extension";

    public final Allele E = new Allele(KEY, "E", "Extension (E)", false, true);
    public final Allele e = new Allele(KEY, "e", "Non-extension / red (e)", true, true);
    private final List<Allele> alleles = List.of(E, e);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return E; }

    /** Dominant: {@code e} is recessive to {@code E}, and one {@code E} is enough - {@code Ee} is a plain black-capable horse. */
    @Override public DominancePattern dominance() { return DominancePattern.DOMINANT; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(rng.nextBoolean() ? E : e, rng.nextBoolean() ? E : e);
    }

    public boolean producesBlack(AllelePair pair) {
        return pair.has(E);
    }

    public boolean isChestnut(AllelePair pair) {
        return !pair.has(E);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isChestnut(pair);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isChestnut(pair)) {
            return null;
        }
        PigmentField f = coat.mutableCopy();
        CoatRegions.restrictAll(ctx.skin(), f, (field, px, py, p) -> field.setBlack(px, py, 0f));
        return f;
    }
}
