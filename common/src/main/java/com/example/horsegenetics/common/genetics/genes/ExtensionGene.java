package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Extension</b> ({@code horsegenetics.extension}) - the "can this horse make
 * black pigment at all" gene.
 * <ul>
 *   <li>{@code E} (dominant, wild-type): black pigment allowed - no effect.</li>
 *   <li>{@code ee}: black completely restricted everywhere - a chestnut horse
 *       (only red pheomelanin survives).</li>
 * </ul>
 * Deterministic: every chestnut is identical.
 */
public final class ExtensionGene implements Gene {

    public static final String KEY = "horsegenetics.extension";

    public final Allele E = new Allele(KEY, "E", 'E', "Extension (E)", false, true);
    public final Allele e = new Allele(KEY, "e", 'e', "Non-extension / red (e)", true, true);
    private final List<Allele> alleles = List.of(E, e);

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public List<Allele> alleles() {
        return alleles;
    }

    @Override
    public Allele wildType() {
        return E;
    }

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
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        if (isChestnut(pair)) {
            CoatRegions.restrictAll(ctx.pigment(), (f, px, py, p) -> f.setBlack(px, py, 0f));
        }
    }
}
