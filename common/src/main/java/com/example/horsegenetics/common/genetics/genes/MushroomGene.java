package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;

/**
 * <b>Mushroom</b> ({@code horsegenetics.mushroom}) - the mirror of
 * {@link SilverGene}: it dilutes <b>pheomelanin only</b>.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code mu/mu}</td><td>wild type</td></tr>
 *   <tr><td>{@code Mu/mu}</td><td>wild type - a carrier, invisible</td></tr>
 *   <tr><td>{@code Mu/Mu}</td><td>{@code mushroom} - red walked to a flat sepia</td></tr>
 * </table>
 *
 * <p>On a <b>chestnut</b> the red body walks toward a flat sepia / khaki - the
 * "mushroom" colour. A black or bay horse carries it invisibly: it has little
 * or no red for mushroom to touch (bay's red body dulls slightly, its black
 * points not at all).
 *
 * <p>It still lowers the red channel on every horse it expresses on - a black
 * one just has almost none to lose - so the outcome always makes a contribution
 * when {@code Mu/Mu}; whether you can <i>see</i> it is up to what pheomelanin
 * the melanin genes left behind.
 *
 * <p>Natural, deterministic. Founder frequency
 * {@code 1/}{@value #WILD_MUSHROOM_ONE_IN} per allele.
 *
 * <p><b>The shortest gene in the mod</b>, and deliberately the example one:
 * everything above the paint function is a {@link TwoAlleleGene#gene
 * declaration}, so what is left to read is the dilution itself. See
 * {@link AbstractNaturalGene}.
 */
public final class MushroomGene extends AbstractNaturalGene {

    public static final String KEY = "horsegenetics.mushroom";
    public static final int WILD_MUSHROOM_ONE_IN = 34;

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

    public MushroomGene() {
        super(gene(KEY, 32, "Mushroom")
                .variant("Mu", "Mushroom (Mu)")
                .wildAllele("mu", "Wild-type (mu)")
                .recessive()
                .hardyWeinberg(1.0 / WILD_MUSHROOM_ONE_IN)
                .wild("Red pigment is left alone.")
                .carrier("mushroom-carrier", "Mushroom carrier",
                        "One copy shows nothing. The allele passes on invisibly - two carriers bred "
                                + "together are how mushroom appears.")
                .outcome("mushroom", "Mushroom",
                        "Red pigment cut hard and partly traded for black, so a chestnut becomes a flat "
                                + "sepia-khaki. A black or bay horse has little red to lose and looks much "
                                + "the same."));
    }

    /** The {@code Mu} allele, for a test or a breed that names it. */
    public final Allele Mu = variant;
    /** The {@code mu} wild type. */
    public final Allele mu = wild;

    @Override
    protected PigmentField restrict(CoatBuildContext ctx, PigmentView coat) {
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

    public boolean isMushroom(AllelePair pair) {
        return expresses(pair);
    }
}
