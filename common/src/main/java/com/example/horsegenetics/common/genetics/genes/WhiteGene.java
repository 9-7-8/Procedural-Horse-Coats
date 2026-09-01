package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Dominant white</b> ({@code horsegenetics.white}). {@code W} (dominant) =
 * total restriction of <i>both</i> pigments everywhere -> a transparent overlay,
 * so the white template shows through unchanged and every other gene is masked.
 * {@code w} (wild-type) = no effect. Rare ({@code 1 in}
 * {@value #WILD_WHITE_ALLELE_ODDS} per allele). Natural, deterministic. (White
 * <i>markings</i> are the separate {@code horsegenetics.splash} gene.)
 */
public final class WhiteGene implements Gene {

    public static final String KEY = "horsegenetics.white";
    public static final int WILD_WHITE_ALLELE_ODDS = 50;

    public final Allele W = new Allele(KEY, "W", "Dominant white (W)", true, true);
    public final Allele w = new Allele(KEY, "w", "Wild-type (w)", false, true);
    private final List<Allele> alleles = List.of(W, w);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return w; }

    /** CompleteDominant: one {@code W} erases every other gene - every white horse looks alike. */
    @Override public DominancePattern dominance() { return DominancePattern.COMPLETE_DOMINANT; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_WHITE_ALLELE_ODDS) == 0 ? W : w,
                rng.nextInt(WILD_WHITE_ALLELE_ODDS) == 0 ? W : w);
    }

    public boolean isWhite(AllelePair pair) {
        return pair.has(W);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isWhite(pair);
    }

    @Override
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        if (isWhite(pair)) {
            CoatRegions.restrictAll(ctx.skin(), ctx.pigment(), (f, px, py, p) -> {
                f.setRed(px, py, 0f);
                f.setBlack(px, py, 0f);
            });
        }
    }
}
