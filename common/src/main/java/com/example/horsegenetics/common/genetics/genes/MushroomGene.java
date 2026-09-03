package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Mushroom</b> ({@code horsegenetics.mushroom}) - the mirror of
 * {@link SilverGene}: it dilutes <b>pheomelanin only</b>, and only when
 * <b>homozygous</b> ({@code Mu/Mu}). {@code mu} is wild-type. Natural,
 * deterministic.
 *
 * <p>On a <b>chestnut</b> the red body walks toward a flat sepia / khaki - the
 * "mushroom" colour. A black or bay horse carries it invisibly: it has little
 * or no red for mushroom to touch (bay's red body dulls slightly, its black
 * points not at all).
 *
 * <p>It still lowers the red channel on every horse it expresses on - a black
 * one just has almost none to lose - so the gene always makes a contribution
 * when {@code Mu/Mu}; whether you can <i>see</i> it is up to what pheomelanin
 * the melanin genes left behind.
 */
public final class MushroomGene implements Gene {

    public static final String KEY = "horsegenetics.mushroom";
    public static final int WILD_MUSHROOM_ALLELE_ODDS = 34;

    /**
     * Pheomelanin kept - cut hard, so the sample leaves the top-left (chestnut)
     * corner instead of just sliding a little toward white. What is left
     * ({@code ~0.12}) plus the black added below lands the texel near the
     * gradient's <b>neutral column</b> at a mid value - a dull grey-sepia,
     * which is what "mushroom" is.
     */
    private static final float KEEP_RED = 0.12f;
    /** Fraction of the removed red fed back as eumelanin - drops the sample down the neutral ramp. */
    private static final float RED_TINT_BLACK = 0.34f;

    public final Allele Mu = new Allele(KEY, "Mu", "Mushroom (Mu)", true, true);
    public final Allele mu = new Allele(KEY, "mu", "Wild-type (mu)", false, true);
    private final List<Allele> alleles = List.of(Mu, mu);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return mu; }

    /** Recessive: {@code Mu/mu} is an invisible carrier; only {@code Mu/Mu} shows. */
    @Override public DominancePattern dominance() { return DominancePattern.RECESSIVE; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_MUSHROOM_ALLELE_ODDS) == 0 ? Mu : mu,
                rng.nextInt(WILD_MUSHROOM_ALLELE_ODDS) == 0 ? Mu : mu);
    }

    public boolean isMushroom(AllelePair pair) {
        return pair.first().equals(Mu) && pair.second().equals(Mu);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isMushroom(pair);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isMushroom(pair)) {
            return null;
        }
        PigmentField f = coat.mutableCopy();
        // dilute(keepRed, keepBlack, blackTint) walks black sideways into red; here
        // the reverse move is done by hand - scale red, and add a little of what
        // was removed back as black so the result is a dull sepia, not a pale tan.
        CoatRegions.restrictAll(ctx.skin(), f, (field, px, py, p) -> {
            float r = field.red(px, py);
            field.setRed(px, py, r * KEEP_RED);
            field.setBlack(px, py, field.black(px, py) + r * (1f - KEEP_RED) * RED_TINT_BLACK);
        });
        return f;
    }
}
