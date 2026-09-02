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
 * <b>Champagne</b> ({@code horsegenetics.champagne}) - a simple dominant,
 * non-dose-dependent dilution. Natural: it just moves the pigment sample. It
 * keeps most of the red, cuts black hard, and feeds part of the removed black
 * back in as red ({@link com.example.horsegenetics.common.coat.pattern.PigmentField#dilute}),
 * so it reads off the <i>current</i> pigment - gold champagne (on chestnut)
 * stays gold, classic champagne (on black) lands taupe, and amber champagne
 * (on bay) keeps <b>chocolate points</b> over a gold body instead of washing
 * the points out to the body colour. Champagne-on-white is invisible.
 * {@code Ch} dominant, {@code c}
 * recessive/wild-type. {@code 1 in} {@value #WILD_CHAMPAGNE_ALLELE_ODDS} per
 * allele. Deterministic.
 */
public final class ChampagneGene implements Gene {

    public static final String KEY = "horsegenetics.champagne";
    public static final int WILD_CHAMPAGNE_ALLELE_ODDS = 40;

    /** Pheomelanin kept - champagne barely touches red (gold champagne stays gold). */
    private static final float KEEP_RED = 0.55f;
    /** Eumelanin kept - hard, but not so hard that a black horse ends up gold. */
    private static final float KEEP_BLACK = 0.42f;
    /**
     * Fraction of a texel's eumelanin fed back in as pheomelanin. This is what
     * gives an <b>amber champagne</b> its chocolate points: bay's black points
     * carry no red at all, and without this term champagne washed them to the
     * same gold as the body (the previous {@code setRed(0.45 + 0.10 * red)} set
     * red almost identically whether the texel was a red body or a black point).
     */
    private static final float BLACK_TINT = 0.30f;

    public final Allele Ch = new Allele(KEY, "Ch", "Champagne (Ch)", true, true);
    public final Allele c = new Allele(KEY, "c", "Wild-type (c)", false, true);
    private final List<Allele> alleles = List.of(Ch, c);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return c; }

    /** Dominant: one {@code Ch} gives the full dilution. */
    @Override public DominancePattern dominance() { return DominancePattern.DOMINANT; }

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
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isChampagne(pair)) {
            return null;
        }
        PigmentField f = coat.mutableCopy();
        CoatRegions.restrictAll(ctx.skin(), f,
                (field, px, py, p) -> field.dilute(px, py, KEEP_RED, KEEP_BLACK, BLACK_TINT));
        return f;
    }
}
