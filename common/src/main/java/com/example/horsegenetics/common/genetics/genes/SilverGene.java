package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.BodyNoise;
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
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
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
 * <h2>The dappling the gene is named for</h2>
 * Drawn on the body only, as a modulation of how much black each texel keeps:
 * a little <i>less</i> at a dapple centre than in the web between them. The
 * lattice is {@link com.example.horsegenetics.common.coat.pattern.BodyNoise#cellDistance}
 * sampled in body space and warped, which is
 * {@link com.example.horsegenetics.common.coat.pattern.GreyCoat}'s method -
 * deliberately, so the two kinds of dapple on a horse are the same shape of
 * thing - but at a much shallower depth, because a silver dapple is a shading
 * within one colour rather than grey's pigment loss.
 *
 * <p><b>Its seed lives in the allele's own epigenetics</b> (owner, 2026-09-17),
 * not in a hash of the genotype. So it inherits with the copy it sits on and
 * drifts slightly each breeding, which means two horses of the same genotype
 * can dapple differently and a line can be selected toward heavy dappling -
 * neither of which a genotype-derived seed could express.
 *
 * <p>Natural, deterministic. Founder frequency
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
    private static final float HAIR_KEEP_RED = 0.30f;
    private static final float HAIR_KEEP_BLACK = 0.10f;
    private static final float HAIR_BLACK_TINT = 0.28f;

    // --- the dapples ---------------------------------------------------

    /**
     * How much less black a dapple centre keeps than the web around it, at full
     * strength. <b>Shallow on purpose.</b> {@code GreyCoat}'s equivalent is
     * {@code 0.42} because a grey dapple is the difference between pigment kept
     * and pigment lost; a silver dapple is a shading <i>within</i> one chocolate,
     * and at grey's depth it reads as spots rather than as dappling.
     */
    public static final float DAPPLE_DEPTH = 0.16f;

    /**
     * Dapple lattice seed, spacing and strength - stored on the allele.
     *
     * <p>Public so {@code SilverDappleTest} can name them rather than repeating
     * the string literals. A test that hardcodes an epigenetic name goes quietly
     * green when the name changes, which is exactly how {@code CoatSampleTool}
     * came to ask grey for an allele it had not had for months.
     */
    public static final String DAPPLE_SEED = "dappleSeed";
    public static final String DAPPLE_SPACING = "dappleSpacing";
    public static final String DAPPLE_STRENGTH = "dappleStrength";

    /** Body units between dapple centres, min .. max (the body is ~22 units long). */
    private static final double SPACING_MIN = 2.0;
    private static final double SPACING_MAX = 3.4;

    /**
     * <b>MCOA</b> - multiple congenital ocular anomalies, the eye defect that
     * rides along with a homozygous silver. Cysts and a malformed cornea; the
     * horse sees badly. The mod has no vision for a horse to lose, so it is
     * priced the way every sub-lethal disorder here is priced - in hearts.
     *
     * <p>The severity splits by dose, the way the real defect does. {@code Z/Z}
     * is the malformation and costs hearts; a single copy carries {@link #MCOA_CYST}
     * instead - named, visible in the info panel, and free. That is why the
     * disorder survives in the population: the gene people breed <i>for</i> is
     * the gene that hides it.
     */
    public static final Condition MCOA = Condition.impairing(
            "mcoa", "Multiple congenital ocular anomalies",
            "Two silver copies. The eyes are malformed - cysts and a misshapen cornea - and "
                    + "the horse is a little frailer for it.");

    /**
     * <b>The heterozygote's half</b>, and the commonest eye finding in silver
     * horses: cysts at the back of the iris, usually harmless. Informational, so
     * it shows on the horse without costing it anything - and so a founder may
     * carry it, which is the point. A silver carrier is not a sick horse.
     */
    public static final Condition MCOA_CYST = Condition.informational(
            "mcoa-cyst", "Ocular cysts (silver)",
            "One silver copy. Small cysts sit at the back of the iris. Common in silver "
                    + "horses and usually harmless - but it is the same defect that "
                    + "disables a horse with two copies.");

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

    /**
     * The dapple lattice, stored on the allele so it inherits with the copy it
     * sits on rather than being re-rolled per horse or derived from the
     * genotype. <b>Strength runs down to zero</b>: a silver that barely dapples
     * is a real horse and a common one, and a locus where every carrier dapples
     * equally would read as a uniform treatment rather than as variation.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.seed(DAPPLE_SEED),
                EpiValue.uniform(DAPPLE_SPACING, SPACING_MIN, SPACING_MAX),
                EpiValue.uniform(DAPPLE_STRENGTH, 0, 1));
    }

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

        EpiValues epi = ctx.epigeneticsFor(KEY);
        long seed = epi.seed(DAPPLE_SEED);
        double spacing = epi.get(DAPPLE_SPACING);
        float strength = (float) epi.get(DAPPLE_STRENGTH);

        // Warp the sample so the lattice flows instead of gridding up - the same
        // trick, and the same numbers, as GreyCoat's dappling.
        double warpScale = 1.0 / (spacing * 3.0);
        double dappleScale = 1.0 / spacing;
        double warp = spacing * 0.45;

        float keepWeb = BODY_KEEP_BLACK;
        float keepDapple = BODY_KEEP_BLACK * (1f - DAPPLE_DEPTH * strength);

        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            boolean hair = part == Part.MANE || part == Part.TAIL
                    || part == Part.LEFT_EAR || part == Part.RIGHT_EAR;
            if (hair) {
                f.dilute(px, py, HAIR_KEEP_RED, HAIR_KEEP_BLACK, HAIR_BLACK_TINT);
                return;
            }

            double n = BodyNoise.value(seed ^ 0x51L,
                    point.x() * warpScale, point.y() * warpScale, point.z() * warpScale);
            double m = BodyNoise.value(seed ^ 0x52L,
                    point.z() * warpScale, point.x() * warpScale, point.y() * warpScale);
            double wx = point.x() + (n - 0.5) * warp;
            double wy = point.y() + (m - 0.5) * warp;
            double wz = point.z() + (n - m) * warp;

            double d = BodyNoise.cellDistance(seed, wx * dappleScale, wy * dappleScale, wz * dappleScale);
            // 0 at a dapple centre -> 1 out in the web between dapples.
            float web = (float) smoothstep(0.35, 0.78, d);

            f.dilute(px, py, BODY_KEEP_RED, lerp(keepDapple, keepWeb, web), BODY_BLACK_TINT);
        });
        return f;
    }

    /**
     * A dapple is only ever a <i>shading</i>, so both of these stay small and
     * local - there is no branch here that can make a horse a different colour,
     * which is what keeps the dappling from changing what silver <em>is</em>.
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(double lo, double hi, double v) {
        double t = (v - lo) / (hi - lo);
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    /**
     * Silver's ocular defect, dosed. Two copies is {@link #MCOA} and costs
     * hearts; one is {@link #MCOA_CYST}, which is named and free. A {@code z/z}
     * horse has nothing.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (pair.homozygousFor(Z)) {
            out.condition(MCOA).addHealth(-MCOA_HEALTH_PENALTY);
        } else if (pair.has(Z)) {
            out.condition(MCOA_CYST);
        }
    }
}
