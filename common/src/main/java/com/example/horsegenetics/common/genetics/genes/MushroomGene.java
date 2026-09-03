package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

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
 */
public final class MushroomGene implements Gene {

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

    public final Allele Mu = new Allele(KEY, 0, "Mu", "Mushroom (Mu)");
    public final Allele mu = new Allele(KEY, 1, "mu", "Wild-type (mu)");
    private final List<Allele> alleles = List.of(Mu, mu);

    private final Expression WILD = Expression.wildType("Red pigment is left alone.");

    private final Expression CARRIER = Expression.wildType(
            "mushroom-carrier", "Mushroom carrier",
            "One copy shows nothing. The allele passes on invisibly - two carriers bred together "
                    + "are how mushroom appears.");

    private final Expression MUSHROOM = Expression.of("mushroom", "Mushroom")
            .describe("Red pigment cut hard and partly traded for black, so a chestnut becomes a flat "
                    + "sepia-khaki. A black or bay horse has little red to lose and looks much the "
                    + "same.")
            .restrict((ctx, coat) -> {
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
            });

    private final List<Expression> expressions = List.of(WILD, CARRIER, MUSHROOM);

    private final FounderTable founders = FounderTable.hardyWeinberg(Mu, mu, 1.0 / WILD_MUSHROOM_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Mushroom"; }
    @Override public int priority() { return 32; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return mu; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Mu)) {
            case 2 -> MUSHROOM;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    public boolean isMushroom(AllelePair pair) {
        return pair.count(Mu) == 2;
    }
}
