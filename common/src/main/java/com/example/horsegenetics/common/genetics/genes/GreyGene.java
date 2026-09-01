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
 * <b>Grey</b> ({@code horsegenetics.grey}). {@code G} dominant, {@code g}
 * wild-type. Natural: {@code G_} <b>equally restricts black and red pigment all
 * over</b> - but only on <b>adults</b>. A foal is born whatever colour it would
 * be without grey; once grown it renders greying (restriction
 * {@value #KEEP_INV_PCT}% - strong, but stops short of dominant white's total).
 *
 * <p>(Real grey is progressive with age; this is a single flat adult step -
 * the pipeline has no year-by-year age input.)
 */
public final class GreyGene implements Gene {

    public static final String KEY = "horsegenetics.grey";
    public static final int WILD_GREY_ALLELE_ODDS = 16;

    /**
     * Fraction of each pigment an adult grey keeps. Lower = greyer; kept low
     * enough that a grey adult reads as an unmistakable pale dapple-grey rather
     * than "a slightly washed-out black".
     */
    private static final float KEEP = 0.15f;
    static final int KEEP_INV_PCT = Math.round((1 - KEEP) * 100);

    public final Allele G = new Allele(KEY, "G", "Grey (G)", true, true);
    public final Allele g = new Allele(KEY, "g", "Wild-type (g)", false, true);
    private final List<Allele> alleles = List.of(G, g);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return g; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_GREY_ALLELE_ODDS) == 0 ? G : g,
                rng.nextInt(WILD_GREY_ALLELE_ODDS) == 0 ? G : g);
    }

    public boolean isGrey(AllelePair pair) {
        return pair.has(G);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isGrey(pair); // gates the pass; restrict() no-ops for foals
    }

    @Override
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        if (!isGrey(pair) || !ctx.isAdult()) {
            return;
        }
        CoatRegions.restrictAll(ctx.skin(), ctx.pigment(), (f, px, py, p) -> {
            f.setRed(px, py, f.red(px, py) * KEEP);
            f.setBlack(px, py, f.black(px, py) * KEEP);
        });
    }
}
