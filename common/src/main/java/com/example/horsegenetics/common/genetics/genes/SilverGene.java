package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.common.trait.TraitBuilder;

import java.util.List;

/**
 * <b>Silver dapple</b> ({@code horsegenetics.silver}) - real-horse
 * {@code PMEL17}.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code z/z}</td><td>wild type</td></tr>
 *   <tr><td>{@code Z/z}, {@code Z/Z}</td><td>{@code silver} - eumelanin-only dilution</td></tr>
 * </table>
 *
 * <p>Silver dilutes <b>eumelanin only</b> - it walks black pigment toward
 * chocolate and, on the mane and tail, most of the way to flaxen, while leaving
 * pheomelanin (red) untouched. So a <b>black</b> becomes a chocolate-bodied
 * horse with a pale mane, a <b>bay</b> becomes "silver bay" (red body,
 * chocolate points, flaxen mane / tail), and a <b>chestnut carrying it looks
 * unchanged</b> - it has no black for silver to act on. That last point is why
 * silver has to run <i>after</i> agouti in {@code Genes.naturalOrder()}: the
 * black points have to be placed before silver can lighten them.
 *
 * <p>The chestnut case stays one expression rather than a genotype-aware
 * suppression, because silver genuinely does its move there - there is simply
 * almost no black to move. Nothing is gained by pretending it did not run.
 *
 * <p>The dappling that gives the gene its name is a follow-up - v1 is the
 * dilution only. Natural, deterministic. Founder frequency
 * {@code 1/}{@value #WILD_SILVER_ONE_IN} per allele.
 */
public final class SilverGene implements Gene, HealthContribution {

    public static final String KEY = "horsegenetics.silver";
    public static final int WILD_SILVER_ONE_IN = 60;

    /** Body: black cut to a chocolate; red barely touched. */
    private static final float BODY_KEEP_RED = 0.90f;
    private static final float BODY_KEEP_BLACK = 0.46f;
    private static final float BODY_BLACK_TINT = 0.30f;
    /**
     * Mane / tail: silver's <b>flaxen</b> signature. The red is pulled well
     * down too (not just the black) so the sample leaves the dark-red corner
     * and lands light and only faintly warm - a flaxen mane, not a chestnut one.
     */
    private static final float HAIR_KEEP_RED = 0.40f;
    private static final float HAIR_KEEP_BLACK = 0.10f;
    private static final float HAIR_BLACK_TINT = 0.28f;

    /**
     * <b>MCOA</b> - multiple congenital ocular anomalies, the eye defect that
     * rides along with a homozygous silver. Cysts and a malformed cornea; the
     * horse sees badly. The mod has no vision for a horse to lose, so it is
     * priced the way every sub-lethal disorder here is priced - in hearts.
     *
     * <p>Only {@code Z/Z}. A single silver copy gives the coat with none of the
     * defect, which is exactly why the disorder survives in the population: the
     * gene people breed <i>for</i> is the gene that hides it.
     */
    public static final Condition MCOA = Condition.impairing(
            "mcoa", "Multiple congenital ocular anomalies",
            "Two silver copies. The eyes are malformed - cysts and a misshapen cornea - and "
                    + "the horse is a little frailer for it.");

    /** Max health a homozygous silver loses to MCOA. */
    public static final double MCOA_HEALTH_PENALTY = 2.0;

    public final Allele Z = new Allele(KEY, 0, "Z", "Silver dapple (Z)");
    public final Allele z = new Allele(KEY, 1, "z", "Wild-type (z)");
    private final List<Allele> alleles = List.of(Z, z);

    private final Expression WILD = Expression.wildType("Black pigment is left alone.");

    private final Expression SILVER = Expression.of("silver", "Silver dapple")
            .describe("Black pigment walked toward chocolate, and most of the way to flaxen on the "
                    + "mane and tail, with red untouched - a chocolate body under a pale mane on a "
                    + "black horse, silver bay on a bay. A chestnut carrier looks unchanged.")
            .restrict(SilverGene::paintSilver);

    private final List<Expression> expressions = List.of(WILD, SILVER);

    private final FounderTable founders = FounderTable.hardyWeinberg(Z, z, 1.0 / WILD_SILVER_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Silver dapple"; }
    @Override public int priority() { return 30; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return z; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(Z) ? SILVER : WILD;
    }

    public boolean isSilver(AllelePair pair) {
        return pair.has(Z);
    }

    private static PigmentField paintSilver(CoatBuildContext ctx, PigmentView coat) {
        Skin skin = ctx.skin();
        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            boolean hair = part == Part.MANE || part == Part.TAIL
                    || part == Part.LEFT_EAR || part == Part.RIGHT_EAR;
            f.dilute(px, py,
                    hair ? HAIR_KEEP_RED : BODY_KEEP_RED,
                    hair ? HAIR_KEEP_BLACK : BODY_KEEP_BLACK,
                    hair ? HAIR_BLACK_TINT : BODY_BLACK_TINT);
        });
        return f;
    }

    /**
     * Homozygous silver carries {@link #MCOA}. Nothing else on this locus does:
     * a {@code Z/z} horse is a silver dapple with sound eyes.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (pair.homozygousFor(Z)) {
            out.condition(MCOA).addHealth(-MCOA_HEALTH_PENALTY);
        }
    }
}
