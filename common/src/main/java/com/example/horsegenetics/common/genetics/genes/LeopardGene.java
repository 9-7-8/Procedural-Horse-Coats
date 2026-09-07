package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BodyNoise;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatOverlay;
import com.example.horsegenetics.common.coat.pattern.CoatOverlayContribution;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PatchNoise;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.common.trait.TraitBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The leopard complex</b> ({@code horsegenetics.leopard}) - real-horse
 * {@code TRPM1} ({@code LP}), the appaloosa spotting family, and the first gene
 * in the model that <b>reads other loci to decide what it paints</b>.
 *
 * <h2>Three loci, one pattern</h2>
 * {@code LP} on its own gives a horse the appaloosa <i>characteristics</i> -
 * roaning that spares the bony parts, the odd scatter of white spots, striped
 * hooves and a white-rimmed eye - but not a bold pattern. The bold patterns
 * come from two modifier loci that mean nothing without {@code LP}:
 * <ul>
 *   <li>{@link Patn1Gene} ({@code PATN1}) pushes toward <b>spots</b> -
 *       <b>leopard</b> (a white horse covered in round dark spots) with one
 *       {@code LP}, <b>fewspot</b> (nearly solid white) with two.</li>
 *   <li>{@link Patn2Gene} ({@code PATN2}) pushes toward a <b>blanket</b> - a
 *       white sheet over the hips, <b>spotted</b> with one {@code LP},
 *       <b>plain</b> (snowcap) with two.</li>
 *   <li>Both together give a <b>semi-leopard</b> - a spotted blanket that
 *       reaches forward over the barrel.</li>
 * </ul>
 * So the visible outcome is a function of this locus's zygosity <i>and</i> two
 * other pairs. {@link #expressionOf} - which only sees this pair - can only give
 * a coarse answer ({@code varnish-roan} for any {@code LP} horse); the pipeline
 * calls {@link #expressionIn(AllelePair, Genotype)}, which reads {@code PATN1}
 * and {@code PATN2} and returns the real one of eight. {@link #coatDependsOn()}
 * names both modifiers so a horse's texture key reflects them even though they
 * paint nothing themselves.
 *
 * <table>
 *   <tr><th>{@code LP}</th><th>no PATN</th><th>+ PATN1</th><th>+ PATN2</th><th>+ both</th></tr>
 *   <tr><td>{@code LP/lp}</td><td>{@code mottled}</td><td>{@code leopard}</td><td>{@code blanket}</td><td>{@code semi-leopard}</td></tr>
 *   <tr><td>{@code LP/LP}</td><td>{@code varnish-roan}</td><td>{@code fewspot}</td><td>{@code snowcap}</td><td>{@code semi-leopard}</td></tr>
 *   <tr><td>{@code lp/lp}</td><td colspan="4">wild type</td></tr>
 * </table>
 *
 * <p>Natural, phase 1, <b>non-deterministic</b> - every visible outcome draws
 * its spot field / roaning / blanket edge from the expressing {@code LP} copy's
 * epigenetic seed, so a foal that inherits the copy inherits the exact pattern.
 * It also carries {@link CoatOverlayContribution} (striped hooves + a
 * white-rimmed eye, drawn in the overlay phase so the eye redraw does not wipe
 * them) and {@link HealthContribution} ({@code LP/LP} &rarr; night blindness,
 * informational).
 *
 * <p><b>Placement is a first guess.</b> Spot sizes, blanket coverage, roaning
 * strength and the hoof-stripe frequency are all eyeballed and want a play
 * session - {@code wiki/verification.html} §0. Mottled skin (the third member
 * of the characteristics triad) is not drawn; the coat has no skin layer.
 *
 * <p>See {@code wiki/gene-leopard.html}.
 */
public final class LeopardGene implements Gene, CoatOverlayContribution, HealthContribution {

    public static final String KEY = "horsegenetics.leopard";
    public static final int WILD_LP_ONE_IN = 40;

    // --- spot / blanket / roaning tuning (all first-guess) -------------------

    /** Soft edge on the round-spot decision, in normalised cell-distance units. */
    private static final double SPOT_EDGE = 0.06;

    /** Leopard: big even spots over the whole horse. */
    private static final double LEOPARD_SPACING = 3.4;
    private static final double LEOPARD_RADIUS = 0.40;
    /** Fewspot: much smaller, much sparser. */
    private static final double FEWSPOT_SPACING = 5.6;
    private static final double FEWSPOT_RADIUS = 0.16;

    /** Blanket: how far forward the white sheet reaches, 0 = rump only .. 1 = whole body. */
    private static final double BLANKET_COVERAGE = 0.42;
    private static final double SEMI_COVERAGE = 0.68;
    private static final double BLANKET_SPOT_SPACING = 3.0;
    private static final double BLANKET_SPOT_RADIUS = 0.34;
    /** How much the blanket's front edge wobbles. */
    private static final double BLANKET_EDGE_WOBBLE = 0.12;
    private static final double BLANKET_EDGE_SOFT = 0.06;

    /**
     * Varnish roaning: body-space frequency of the hair mottle, and per-outcome
     * strength. The frequency is deliberately <b>low</b> - far coarser than
     * {@code RoanGene}'s near-per-texel mix - because that is the difference the
     * two patterns are told apart by: classic roan is an even salt-and-pepper,
     * varnish is uneven pale areas.
     */
    private static final double VARNISH_FREQ = 1.15;
    private static final double MOTTLED_STRENGTH = 0.32;
    private static final double VARNISH_STRENGTH = 0.58;
    /** Varnish never takes a body texel all the way to white - it is a mix, not a patch. */
    private static final double VARNISH_MAX_WHITE = 0.9;

    /**
     * <b>Varnish marks</b> - the pigment a varnish horse keeps over its bony
     * prominences, and the trait it is actually recognised by. How deeply the
     * marks hold is rolled per horse, because "varnishes out" is a process and
     * different horses are at different points along it.
     */
    private static final double MARK_MIN = 0.55;
    private static final double MARK_RANGE = 0.40;
    /** The two wide, shallow shields; and the small, deep joints. */
    private static final double SHIELD_DEPTH = 0.85;
    private static final double JOINT_DEPTH = 1.0;
    /** How far a mark's edge wanders, as a fraction of its own radius. */
    private static final double MARK_WOBBLE = 0.42;
    private static final double MARK_WOBBLE_FREQ = 1.7;

    /** Where each mark sits, as fractions of the part box it is derived from. */
    private static final double SHOULDER_X = 0.76;
    private static final double SHOULDER_Y = 0.56;
    private static final double SHOULDER_RADIUS = 0.17;
    private static final double HIP_X = 0.15;
    private static final double HIP_Y = 0.70;
    private static final double HIP_RADIUS = 0.15;
    private static final double UPPER_JOINT_Y = 0.86;
    private static final double LOWER_JOINT_Y = 0.40;
    private static final double JOINT_RADIUS = 0.085;
    private static final double CHEEK_X = 0.30;
    private static final double CHEEK_Y = 0.55;
    private static final double CHEEK_RADIUS = 0.075;
    /** How far out toward each flank the two-sided marks sit. */
    private static final double SIDE_Z = 0.92;

    /**
     * How much of the body's roaning strength the head, muzzle and legs get.
     * Varnish <b>reaches the head</b>, which is the loudest single difference
     * from classic roan - a dark head over a roaned body is the classic-roan
     * tell - and it does not exclude the lower leg wholesale either; the knee
     * and hock keep their colour through {@link #varnishMark} instead.
     */
    private static final double HEAD_REGION = 0.85;
    private static final double MUZZLE_REGION = 0.55;
    private static final double LEG_REGION = 0.80;

    /** Snowflake spots on a mottled horse: sparse, small, crisp. */
    private static final double SNOWFLAKE_SPACING = 6.0;
    private static final double SNOWFLAKE_RADIUS = 0.12;
    private static final double SNOWFLAKE_EDGE = 0.04;

    // --- overlay: striped hooves + white sclera -----------------------------

    /** Bottom fraction of each leg treated as hoof. */
    private static final double HOOF_FRACTION = 0.14;
    /** Body-space frequency of the vertical hoof stripes. */
    private static final double HOOF_STRIPE_FREQ = 2.2;
    /** Pale horn colour for the light hoof stripes. */
    private static final int HORN_LIGHT = 0xD8CBB0;
    private static final double HOOF_STRIPE_BLEND = 0.55;
    /** Cool near-white the eye rim is shaded toward - luma-scaled, so the pupil stays dark. */
    private static final int SCLERA_TINT = 0xECECF4;
    private static final double SCLERA_BLEND = 0.40;

    /**
     * <b>Congenital stationary night blindness.</b> Every {@code LP/LP} horse
     * has it - the same {@code TRPM1} change that makes the pattern also stops
     * the retina resetting in the dark. The mod has no night vision for a horse
     * to lose, so like deafness on a splash homozygote it is
     * {@link Condition#informational informational}: shown on the info panel and
     * the paper dump, costing the horse nothing.
     */
    public static final Condition CSNB = Condition.informational(
            "csnb", "Congenital stationary night blindness",
            "Two leopard-complex copies. The horse sees poorly in the dark from birth - it does "
                    + "not get better or worse, and in daylight the horse is unaffected.");

    // declaration order is AllelePair's canonical slot order, not dominance
    public final Allele LP = new Allele(KEY, 0, "LP", "Leopard complex (LP)");
    public final Allele lp = new Allele(KEY, 1, "lp", "Wild-type (lp)");
    private final List<Allele> alleles = List.of(LP, lp);

    private final Expression WILD = Expression.wildType("No leopard complex - a solid-coloured coat.");

    private final Expression MOTTLED = Expression.of("mottled", "Mottled (LP characteristics)")
            .describe("One leopard-complex copy and no pattern modifier: light, uneven roaning that "
                    + "keeps its colour over the cheekbones, shoulders, hips and knees, a scatter of "
                    + "small white spots, striped hooves and a white-rimmed eye. The appaloosa "
                    + "“look” without a bold pattern.")
            .varies()
            .restrict((ctx, coat) -> paintVarnish(ctx, coat, MOTTLED_STRENGTH, true));

    private final Expression VARNISH_ROAN = Expression.of("varnish-roan", "Varnish roan")
            .describe("Two leopard-complex copies and no modifier: heavy, uneven whitening that "
                    + "leaves darker “varnish marks” over the horse's bones - cheekbones, "
                    + "shoulder blades, elbows, hips, stifles and knees - so it ends up outlined by "
                    + "its own anatomy. Not true roan and not a stronger version of it: patchy where "
                    + "classic roan is even, and it whitens the head, which classic roan never does.")
            .varies()
            .restrict((ctx, coat) -> paintVarnish(ctx, coat, VARNISH_STRENGTH, false));

    private final Expression LEOPARD = Expression.of("leopard", "Leopard")
            .describe("One leopard-complex copy plus PATN1: a white or cream body carrying round "
                    + "dark spots of the base colour, fairly evenly spread from head to hoof.")
            .varies()
            .restrict((ctx, coat) -> paintSpotted(ctx, coat, LEOPARD_SPACING, LEOPARD_RADIUS));

    private final Expression FEWSPOT = Expression.of("fewspot", "Fewspot leopard")
            .describe("Two leopard-complex copies plus PATN1: almost solid white, with only a "
                    + "handful of small spots left - usually on the flank, elbow and stifle.")
            .varies()
            .restrict((ctx, coat) -> paintSpotted(ctx, coat, FEWSPOT_SPACING, FEWSPOT_RADIUS));

    private final Expression BLANKET = Expression.of("blanket", "Spotted blanket")
            .describe("One leopard-complex copy plus PATN2: a solid white sheet over the hips and "
                    + "croup with dark spots of the base colour inside it; the rest of the horse is "
                    + "its ordinary colour.")
            .varies()
            .restrict((ctx, coat) -> paintBlanket(ctx, coat, BLANKET_COVERAGE, true));

    private final Expression SNOWCAP = Expression.of("snowcap", "Snowcap blanket")
            .describe("Two leopard-complex copies plus PATN2: the same white blanket over the hips, "
                    + "with no spots in it at all.")
            .varies()
            .restrict((ctx, coat) -> paintBlanket(ctx, coat, BLANKET_COVERAGE, false));

    private final Expression SEMI_LEOPARD = Expression.of("semi-leopard", "Semi-leopard")
            .describe("A leopard-complex horse carrying both PATN1 and PATN2: a spotted white "
                    + "blanket that reaches well forward over the barrel, part-way to a full leopard.")
            .varies()
            .restrict((ctx, coat) -> paintBlanket(ctx, coat, SEMI_COVERAGE, true));

    private final List<Expression> expressions = List.of(
            WILD, MOTTLED, VARNISH_ROAN, LEOPARD, FEWSPOT, BLANKET, SNOWCAP, SEMI_LEOPARD);

    private final FounderTable founders = FounderTable.hardyWeinberg(LP, lp, 1.0 / WILD_LP_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Leopard complex (appaloosa)"; }
    @Override public int priority() { return 73; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return lp; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public List<String> coatDependsOn() {
        return List.of(Patn1Gene.KEY, Patn2Gene.KEY);
    }

    /**
     * The coarse, pair-only answer: any {@code LP} horse reads as
     * {@code varnish-roan} here. It is deliberately not the real outcome - that
     * needs {@code PATN1} / {@code PATN2}, which this method cannot see - and it
     * keeps the gallery to one leopard-complex pen per {@code LP} zygosity.
     * {@link #expressionIn} is what the coat pipeline calls.
     */
    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(LP) ? VARNISH_ROAN : WILD;
    }

    /** The real outcome, reading the two modifier loci and this locus's zygosity. */
    @Override
    public Expression expressionIn(AllelePair pair, Genotype genotype) {
        if (!pair.has(LP)) {
            return WILD;
        }
        boolean hom = pair.homozygousFor(LP);
        boolean p1 = modifierPresent(genotype, Patn1Gene.KEY);
        boolean p2 = modifierPresent(genotype, Patn2Gene.KEY);
        if (p1 && p2) {
            return SEMI_LEOPARD;
        }
        if (p1) {
            return hom ? FEWSPOT : LEOPARD;
        }
        if (p2) {
            return hom ? SNOWCAP : BLANKET;
        }
        return hom ? VARNISH_ROAN : MOTTLED;
    }

    public boolean isLeopardComplex(AllelePair pair) {
        return pair.has(LP);
    }

    private static boolean modifierPresent(Genotype genotype, String modifierKey) {
        Gene gene = Genes.byKeyOrNull(modifierKey);
        if (!(gene instanceof AppaloosaModifierGene mod)) {
            return false;
        }
        return mod.isPresent(genotype.pair(modifierKey));
    }

    // ------------------------------------------------------------------
    // Health
    // ------------------------------------------------------------------

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (pair.homozygousFor(LP)) {
            out.condition(CSNB);
        }
    }

    // ------------------------------------------------------------------
    // Overlay: striped hooves + white-rimmed eye (any LP horse)
    // ------------------------------------------------------------------

    @Override
    public void overlay(AllelePair pair, CoatBuildContext ctx, CoatOverlay out) {
        Skin skin = ctx.skin();
        for (Part leg : CoatRegions.LEGS) {
            if (!HorseSkinGeometry.hasPart(skin, leg)) {
                continue;
            }
            Bounds b = HorseSkinGeometry.bounds(skin, leg);
            double cut = b.yMin() + b.span(Axis.Y) * HOOF_FRACTION;
            HorseSkinGeometry.forEachTexel(skin, leg, (px, py, part, face, point) -> {
                if (point.y() > cut) {
                    return;
                }
                double phase = (point.x() + point.z()) * HOOF_STRIPE_FREQ;
                double band = phase - Math.floor(phase);
                if (band < 0.5) {
                    out.blendToward(px, py, HORN_LIGHT, HOOF_STRIPE_BLEND);
                }
            });
        }
        // White sclera: shadeToward is luma-scaled, so the bright sclera lifts
        // toward the cool tint while the dark pupil stays dark.
        out.shadeEyes(SCLERA_TINT, SCLERA_BLEND);
    }

    // ------------------------------------------------------------------
    // Painters
    // ------------------------------------------------------------------

    /** A leopard / fewspot coat: the whole horse white, with round base-colour spots punched back in. */
    private static PigmentField paintSpotted(CoatBuildContext ctx, PigmentView coat,
                                             double spacing, double radiusFrac) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double sizeJitter = 0.85 + 0.30 * epi.nextFloat();   // per-horse spot-size wobble
        double densityJitter = 0.90 + 0.20 * epi.nextFloat();

        Skin skin = ctx.skin();
        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (part == Part.MANE || part == Part.TAIL) {
                return;   // leave the hair its base colour
            }
            double s = spotStrength(seed, point, spacing * densityJitter, radiusFrac * sizeJitter);
            whiten(f, px, py, 1.0 - s);
        });
        return f;
    }

    /**
     * A blanket coat: a white sheet from the rump forward to {@code coverage},
     * optionally with spots inside it. Outside the blanket the base colour is
     * untouched.
     */
    private static PigmentField paintBlanket(CoatBuildContext ctx, PigmentView coat,
                                             double coverage, boolean spots) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double coverJitter = coverage + (epi.nextFloat() - 0.5) * 0.12;
        double spotJitter = 0.85 + 0.30 * epi.nextFloat();

        Skin skin = ctx.skin();
        Bounds body = HorseSkinGeometry.bounds(skin, Part.BODY);
        double bMin = body.xMin();
        double bSpan = Math.max(1e-6, body.span(Axis.X));

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double inBlanket;
            if (part == Part.BODY) {
                double bx = (point.x() - bMin) / bSpan;   // 0 at the rump, 1 at the shoulder
                double wob = (BodyNoise.value(seed, point.x() * 0.6, point.y() * 0.6, point.z() * 0.6) - 0.5)
                        * BLANKET_EDGE_WOBBLE;
                double edge = coverJitter + wob;
                inBlanket = 1.0 - PatchNoise.smoothstep(edge - BLANKET_EDGE_SOFT, edge + BLANKET_EDGE_SOFT, bx);
            } else if (coverJitter > 0.62 && (part == Part.LEFT_HIND_LEG || part == Part.RIGHT_HIND_LEG)) {
                // a large blanket creeps onto the top of the hind legs
                Bounds lb = HorseSkinGeometry.bounds(skin, part);
                double frac = (point.y() - lb.yMin()) / Math.max(1e-6, lb.span(Axis.Y));
                inBlanket = PatchNoise.smoothstep(0.30, 0.60, frac);
            } else {
                return;
            }
            if (inBlanket <= 0) {
                return;
            }
            double keepSpot = spots
                    ? spotStrength(seed ^ 0x5BL, point, BLANKET_SPOT_SPACING * spotJitter, BLANKET_SPOT_RADIUS)
                    : 0.0;
            whiten(f, px, py, inBlanket * (1.0 - keepSpot));
        });
        return f;
    }

    /**
     * A mottled / varnish-roan coat: an uneven white mix that <b>whitens around
     * the horse's bones without erasing them</b>.
     *
     * <p>This is the half of the leopard complex that is not a spot, and it is
     * not "roan but more". The two look fundamentally different, and both halves
     * of that matter:
     * <ul>
     *   <li><b>It is patchy where classic roan is even.</b> The mottle runs at
     *       {@value #VARNISH_FREQ} against roan's much finer mix, so varnish
     *       reads as irregular pale areas rather than as a uniform salt-and-pepper.
     *       "Uneven or incomplete roaning" is what a real horse is looked at for
     *       when classic roan is in doubt.</li>
     *   <li><b>It leaves varnish marks.</b> Pigment persists over bony
     *       prominences - cheekbones, shoulder blades, elbows, hips, stifles,
     *       knees - so the horse ends up outlined by its own anatomy. That is
     *       the single most recognisable varnish trait and it is what
     *       {@link #varnishMark} draws.</li>
     *   <li><b>It reaches the head</b>, which classic roan famously does not.
     *       A dark head over a roaned body is the classic-roan tell; a varnish
     *       horse whitens the face too and keeps colour only over the facial
     *       bone.</li>
     *   <li><b>The legs are not a uniform dark exclusion.</b> Classic roan stops
     *       above the knee on a level-ish line; varnish keeps colour over the
     *       knee and hock themselves and whitens around them.</li>
     * </ul>
     */
    private static PigmentField paintVarnish(CoatBuildContext ctx, PigmentView coat,
                                             double strength, boolean snowflakes) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double densityJitter = 0.85 + 0.30 * epi.nextFloat();
        double asymmetry = (epi.nextFloat() - 0.5) * 0.5;   // one side roans a little more
        double markStrength = MARK_MIN + epi.nextFloat() * MARK_RANGE;

        Skin skin = ctx.skin();
        Mark[] marks = varnishMarks(skin);

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double region = varnishRegion(skin, part, point);
            if (region > 0) {
                // Pigment kept over the bone. Its edge is wobbled by the same
                // seed, so a varnish mark is a ragged anatomical island rather
                // than an airbrushed oval.
                double keep = markStrength * varnishMark(marks, point, seed);
                region *= 1.0 - keep;
            }
            if (region > 0) {
                double n = PatchNoise.fbm2(seed,
                        point.x() * VARNISH_FREQ,
                        point.y() * VARNISH_FREQ,
                        point.z() * VARNISH_FREQ * 0.6 + point.z() * asymmetry);
                double threshold = 1.0 - region * strength * densityJitter;
                double w = PatchNoise.smoothstep(threshold - 0.06, threshold + 0.06, n);
                if (w > 0) {
                    whiten(f, px, py, w * VARNISH_MAX_WHITE);
                }
            }
            if (snowflakes) {
                double d = BodyNoise.cellDistance(seed ^ 0x5107L,
                        point.x() / SNOWFLAKE_SPACING, point.y() / SNOWFLAKE_SPACING, point.z() / SNOWFLAKE_SPACING);
                double sf = 1.0 - PatchNoise.smoothstep(SNOWFLAKE_RADIUS - SNOWFLAKE_EDGE,
                        SNOWFLAKE_RADIUS + SNOWFLAKE_EDGE, d);
                if (sf > 0) {
                    whiten(f, px, py, sf);
                }
            }
        });
        return f;
    }

    /**
     * One <b>varnish mark</b>: a body-space sphere over a bony prominence, inside
     * which pigment is retained.
     *
     * @param x      centre, in the same model units every texel is handed in
     * @param radius how far the retention reaches, in the same units
     * @param depth  how much pigment is kept at the centre, {@code [0,1]}
     */
    private record Mark(double x, double y, double z, double radius, double depth) {}

    /**
     * <b>Where a varnish horse keeps its colour</b> - the bony prominences,
     * derived from the mesh's own part boxes rather than written down as
     * coordinates, so a mesh change moves them with it.
     *
     * <p>The list is the one the literature gives: cheekbones, shoulder blades,
     * elbows, hips, stifles and knees. The two shields (shoulder and hip) are
     * wide and shallow; the joints are small and deep, which is what makes a
     * varnish horse look outlined at the knee and shaded at the shoulder rather
     * than uniformly blotchy.
     */
    private static Mark[] varnishMarks(Skin skin) {
        List<Mark> marks = new ArrayList<>();
        Bounds body = HorseSkinGeometry.bounds(skin, Part.BODY);
        double bw = body.span(Axis.X);
        double bh = body.span(Axis.Y);
        double bz = body.span(Axis.Z) * 0.5;
        double zc = (body.zMin() + body.zMax()) * 0.5;

        // Shoulder blade and point of hip, one per side. Larger x is toward the
        // nose, so the shoulder is the high-x end of the barrel and the hip the low.
        for (int s = -1; s <= 1; s += 2) {
            marks.add(new Mark(body.xMin() + bw * SHOULDER_X, body.yMin() + bh * SHOULDER_Y,
                    zc + s * bz * SIDE_Z, bw * SHOULDER_RADIUS, SHIELD_DEPTH));
            marks.add(new Mark(body.xMin() + bw * HIP_X, body.yMin() + bh * HIP_Y,
                    zc + s * bz * SIDE_Z, bw * HIP_RADIUS, SHIELD_DEPTH));
        }

        // Elbow / stifle at the top of each leg, knee / hock part-way down it.
        for (Part leg : CoatRegions.LEGS) {
            if (!HorseSkinGeometry.hasPart(skin, leg)) {
                continue;
            }
            Bounds b = HorseSkinGeometry.bounds(skin, leg);
            double lx = (b.xMin() + b.xMax()) * 0.5;
            double lz = (b.zMin() + b.zMax()) * 0.5;
            double lh = b.span(Axis.Y);
            marks.add(new Mark(lx, b.yMin() + lh * UPPER_JOINT_Y, lz, bw * JOINT_RADIUS, JOINT_DEPTH));
            marks.add(new Mark(lx, b.yMin() + lh * LOWER_JOINT_Y, lz, bw * JOINT_RADIUS, JOINT_DEPTH));
        }

        // Cheekbone, one per side. The head's low-x end is the poll end.
        if (HorseSkinGeometry.hasPart(skin, Part.HEAD)) {
            Bounds h = HorseSkinGeometry.bounds(skin, Part.HEAD);
            double hz = h.span(Axis.Z) * 0.5;
            double hzc = (h.zMin() + h.zMax()) * 0.5;
            for (int s = -1; s <= 1; s += 2) {
                marks.add(new Mark(h.xMin() + h.span(Axis.X) * CHEEK_X,
                        h.yMin() + h.span(Axis.Y) * CHEEK_Y,
                        hzc + s * hz * SIDE_Z, bw * CHEEK_RADIUS, JOINT_DEPTH));
            }
        }
        return marks.toArray(new Mark[0]);
    }

    /**
     * How much pigment this texel keeps because it sits over a bone, in
     * {@code [0,1]}. The strongest mark wins rather than the sum, so two
     * overlapping marks do not add up to a solid patch, and the distance is
     * wobbled by a noise field so the island's outline is ragged - a real
     * varnish mark follows the bone loosely, not exactly.
     */
    private static double varnishMark(Mark[] marks, BodyPoint p, long seed) {
        if (marks.length == 0) {
            return 0;
        }
        double wobble = MARK_WOBBLE * (PatchNoise.fbm2(seed ^ 0x8A2EL,
                p.x() * MARK_WOBBLE_FREQ, p.y() * MARK_WOBBLE_FREQ, p.z() * MARK_WOBBLE_FREQ) - 0.5);
        double best = 0;
        for (Mark m : marks) {
            double dx = p.x() - m.x();
            double dy = p.y() - m.y();
            double dz = p.z() - m.z();
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz) + wobble * m.radius();
            double v = m.depth() * (1.0 - PatchNoise.smoothstep(m.radius() * 0.35, m.radius(), d));
            if (v > best) {
                best = v;
            }
        }
        return best > 1 ? 1 : best;
    }

    /**
     * How hard varnish roaning bites <b>before</b> the marks are subtracted:
     * near-full over the barrel, neck and head, a little less on the legs, and
     * nothing on the mane, tail and ears.
     *
     * <p>Deliberately almost flat. The old version had a front-to-back gradient,
     * which made varnish read as a weaker classic roan; the difference between
     * the two patterns is not how much white there is, it is that varnish's white
     * is <b>shaped by the skeleton</b> and classic roan's is shaped by nothing at
     * all. All the shaping lives in {@link #varnishMark} now.
     */
    private static double varnishRegion(Skin skin, Part part, BodyPoint point) {
        switch (part) {
            case BODY, NECK -> {
                return 1.0;
            }
            case HEAD -> {
                return HEAD_REGION;
            }
            case MUZZLE -> {
                return MUZZLE_REGION;
            }
            case LEFT_FRONT_LEG, RIGHT_FRONT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG -> {
                return LEG_REGION;
            }
            default -> {
                return 0.0;
            }
        }
    }

    /** 1 deep inside a round cell, 0 between cells - the shape of an appaloosa spot. */
    private static double spotStrength(long seed, BodyPoint p, double spacing, double radiusFrac) {
        double d = BodyNoise.cellDistance(seed, p.x() / spacing, p.y() / spacing, p.z() / spacing);
        return 1.0 - PatchNoise.smoothstep(radiusFrac - SPOT_EDGE, radiusFrac + SPOT_EDGE, d);
    }

    /**
     * Whiten one texel by {@code amount} in {@code [0, 1]}: 1 is bald white (the
     * transparent path), a fraction is the white-hair mix a varnished or
     * snowflake texel wants. Both cases are
     * {@link PigmentField#whiten}, which is what stops the partial ones - the
     * spot edges and the varnish - reading as tan on a black-based appaloosa.
     */
    private static void whiten(PigmentField f, int px, int py, double amount) {
        f.whiten(px, py, (float) amount);
    }
}
