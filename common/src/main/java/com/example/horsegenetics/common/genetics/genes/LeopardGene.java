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

    /** Varnish roaning: body-space frequency of the hair mottle, and per-outcome strength. */
    private static final double VARNISH_FREQ = 2.4;
    private static final double MOTTLED_STRENGTH = 0.32;
    private static final double VARNISH_STRENGTH = 0.58;
    /** Varnish never takes a body texel all the way to white - it is a mix, not a patch. */
    private static final double VARNISH_MAX_WHITE = 0.9;
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
            .describe("One leopard-complex copy and no pattern modifier: light roaning that spares "
                    + "the face and legs, a scatter of small white spots, striped hooves and a "
                    + "white-rimmed eye. The appaloosa “look” without a bold pattern.")
            .varies()
            .restrict((ctx, coat) -> paintVarnish(ctx, coat, MOTTLED_STRENGTH, true));

    private final Expression VARNISH_ROAN = Expression.of("varnish-roan", "Varnish roan")
            .describe("Two leopard-complex copies and no modifier: heavier, uneven roaning over the "
                    + "body and neck, leaving darker “varnish marks” on the bony parts of "
                    + "the face and legs. Distinct from true roan - patchier, and it reaches the head.")
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

    /** A mottled / varnish-roan coat: a hair-by-hair white mix that spares the bony parts. */
    private static PigmentField paintVarnish(CoatBuildContext ctx, PigmentView coat,
                                             double strength, boolean snowflakes) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double densityJitter = 0.85 + 0.30 * epi.nextFloat();
        double asymmetry = (epi.nextFloat() - 0.5) * 0.5;   // one side roans a little more

        Skin skin = ctx.skin();
        Bounds body = HorseSkinGeometry.bounds(skin, Part.BODY);
        Bounds neck = HorseSkinGeometry.bounds(skin, Part.NECK);
        double frontStart = body.xMin() + body.span(Axis.X) * 0.12;
        double frontEnd = neck.xMax();

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double region = varnishRegion(skin, part, point, frontStart, frontEnd);
            if (region <= 0) {
                return;
            }
            double n = PatchNoise.fbm2(seed,
                    point.x() * VARNISH_FREQ,
                    point.y() * VARNISH_FREQ,
                    point.z() * VARNISH_FREQ * 0.6 + point.z() * asymmetry);
            double threshold = 1.0 - region * strength * densityJitter;
            double w = PatchNoise.smoothstep(threshold - 0.06, threshold + 0.06, n);
            if (w > 0) {
                whiten(f, px, py, w * VARNISH_MAX_WHITE);
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

    /** 1 deep inside a round cell, 0 between cells - the shape of an appaloosa spot. */
    private static double spotStrength(long seed, BodyPoint p, double spacing, double radiusFrac) {
        double d = BodyNoise.cellDistance(seed, p.x() / spacing, p.y() / spacing, p.z() / spacing);
        return 1.0 - PatchNoise.smoothstep(radiusFrac - SPOT_EDGE, radiusFrac + SPOT_EDGE, d);
    }

    /**
     * How hard varnish roaning bites: full over the barrel and neck, tapering
     * forward, roughly half over the head (varnish leaves dark marks there
     * rather than sparing it whole), fading out toward the hooves, and nothing
     * on the mane, tail and ears.
     */
    private static double varnishRegion(Skin skin, Part part, BodyPoint point,
                                        double frontStart, double frontEnd) {
        switch (part) {
            case BODY, NECK -> {
                return 1.0 - 0.65 * PatchNoise.smoothstep(frontStart, frontEnd, point.x());
            }
            case HEAD -> {
                return 0.5;
            }
            case MUZZLE -> {
                return 0.2;
            }
            case LEFT_FRONT_LEG, RIGHT_FRONT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG -> {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double frac = (point.y() - b.yMin()) / Math.max(1e-6, b.span(Axis.Y));
                return 0.8 * PatchNoise.smoothstep(0.20, 0.55, frac);
            }
            default -> {
                return 0.0;
            }
        }
    }

    /**
     * Whiten one texel by {@code amount} in {@code [0, 1]}: 1 is bald white
     * ({@code setRed}/{@code setBlack} to 0, the transparent path), a fraction
     * is a white-hair mix with the red pulled down a little faster than the
     * black so the ~1px soft edge greys out instead of going gold (the roan
     * lesson).
     */
    private static void whiten(PigmentField f, int px, int py, double amount) {
        if (amount <= 0) {
            return;
        }
        double a = amount > 1 ? 1 : amount;
        double keep = 1.0 - a;
        f.setRed(px, py, f.red(px, py) * (float) (keep * (1.0 - 0.6 * a)));
        f.setBlack(px, py, f.black(px, py) * (float) keep);
    }
}
