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
 * <b>Champagne</b> ({@code horsegenetics.champagne}) - a simple dominant,
 * non-dose-dependent dilution. Natural: it just moves the pigment sample. It
 * pulls red toward the <b>horizontal middle</b> of the red/black gradient (its
 * champagne-gold column) and lifts black sharply, so it reads off the
 * <i>current</i> pigment - champagne-on-bay differs from champagne-on-black,
 * and champagne-on-white is invisible. {@code Ch} dominant, {@code c}
 * recessive/wild-type. {@code 1 in} {@value #WILD_CHAMPAGNE_ALLELE_ODDS} per
 * allele. Deterministic.
 */
public final class ChampagneGene implements Gene {

    public static final String KEY = "horsegenetics.champagne";
    public static final int WILD_CHAMPAGNE_ALLELE_ODDS = 40;

    public final Allele Ch = new Allele(KEY, "Ch", "Champagne (Ch)", true, true);
    public final Allele c = new Allele(KEY, "c", "Wild-type (c)", false, true);
    private final List<Allele> alleles = List.of(Ch, c);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return c; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_CHAMPAGNE_ALLELE_ODDS) == 0 ? Ch : c,
                rng.nextInt(WILD_CHAMPAGNE_ALLELE_ODDS) == 0 ? Ch : c);
    }

    public boolean isChampagne(AllelePair pair) {
        return pair.has(Ch);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isChampagne(pair);
    }

    @Override
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        if (!isChampagne(pair)) {
            return;
        }
        CoatRegions.restrictAll(ctx.pigment(), (f, px, py, p) -> {
            f.setRed(px, py, 0.45f + 0.10f * f.red(px, py)); // ~horizontal middle
            f.setBlack(px, py, f.black(px, py) * 0.18f);      // reach the gold band
        });
    }
}
