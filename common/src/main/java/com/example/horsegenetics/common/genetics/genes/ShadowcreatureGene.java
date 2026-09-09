package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.BodyNoise;
import com.example.horsegenetics.common.coat.pattern.BodyStripes;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatOverlay;
import com.example.horsegenetics.common.coat.pattern.CoatOverlayContribution;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.AbilityContribution;
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
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Shadowcreature</b> ({@code horsegenetics.shadowcreature}) - a magical gene
 * that takes the front third of the horse away.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code Shc/Shc}</td><td>{@code shadowcreature} - black head and neck, gold eyes, tentacles into the barrel</td></tr>
 *   <tr><td>{@code Shc/n}, {@code n/n}</td><td>wild type</td></tr>
 * </table>
 *
 * <p>Three things at once, and they land in three different phases, which is
 * why this is a Java gene and not a gene file:
 *
 * <ul>
 *   <li><b>The head and neck go as black as the pipeline can make them.</b> Not
 *       a walk toward black - {@link ColorField#set} flat, at full opacity, so
 *       nothing underneath survives and no dilution the horse carries lightens
 *       it.</li>
 *   <li><b>The eyes are gold, with no white left round them.</b> That cannot be
 *       phase 3: {@code CoatRegions.redrawEyes} restores the eyes from the
 *       template as the last act of the bake, exactly so that a wide white
 *       pattern can never blind a horse. So it runs in the overlay pass, as
 *       {@link LightGene}'s gold eyes do - and it uses
 *       {@link CoatOverlay#shadeEyes} rather than {@code tintIris}, because
 *       shading the <i>whole</i> eye is precisely what "no sclera" means.</li>
 *   <li><b>Tentacles run out of the black and down over the shoulder.</b> The
 *       {@code STROKES} shape, faded out with distance from the join.</li>
 * </ul>
 *
 * <h2>Why the black is a colour and not a pigment</h2>
 * Because black pigment is not black. Phase 1 works in melanin, and melanin is
 * resolved through a gradient chart that the {@link LutGene LUT} locus can
 * swap; the chart's black corner is whatever the artwork says, and on an
 * alternate palette it is not black at all. A shadowcreature's head has to be
 * <i>the absence of the horse</i> on every palette, so it is painted as a
 * colour in phase 3 rather than as a pigment level in phase 1.
 *
 * <h2>Recessive, and never a carrier in the wild</h2>
 * One copy shows nothing. The founder table therefore lists the homozygote and
 * the plain horse and nothing between them - the rule every magical locus with
 * an invisible carrier follows here, because a wild population full of silent
 * carriers is a locus run in the dark. What you catch is what you watched it
 * do, and the carriers turn up a generation later.
 */
public final class ShadowcreatureGene implements Gene, CoatOverlayContribution, AbilityContribution {

    public static final String KEY = "horsegenetics.shadowcreature";

    /**
     * Late, among the modifiers. It has to have the <b>last</b> word on the
     * head and neck: a gene that blackens them and is then painted over by a
     * face marking is not a shadowcreature, it is a horse with a dark head.
     */
    public static final int PRIORITY = 613;

    /** Not {@code #000000} - the composer lifts pure black off the floor anyway, so this is where it would land. */
    public static final int VOID = 0x07070B;

    /** The gold, shared with {@link LightGene} on purpose: one glowing gold in the mod, not two. */
    public static final int GOLD = LightGene.GOLD;

    /** How far of the way to the gold the eye goes. Short of 1 so the pupil still reads. */
    private static final double EYE_STRENGTH = 0.94;

    /** A dim, close light. A shadowcreature that lit a stable like a torch would stop being one. */
    public static final int LIGHT_LEVEL = 5;

    /** How far down the barrel the tentacles reach, as a share of its length from the shoulder. */
    public static final String REACH = "reach";
    /** Centre-to-centre spacing of the tentacles, body units. */
    public static final String SPACING = "spacing";
    /** Tentacle width, body units. A texel is 0.5. */
    public static final String WIDTH = "width";
    /** The seed the tentacle field is drawn from. */
    public static final String SEED = "tentacleSeed";

    public final Allele Shc = new Allele(KEY, 0, "Shc", "Shadowcreature (Shc)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Shc, n);

    private final Expression WILD = Expression.wildType(
            "Nothing. A single copy is silent, so a carrier is an ordinary horse in every "
                    + "respect and can only be found by breeding two of them together.");

    private final Expression SHADOW = Expression.of("shadowcreature", "Shadowcreature")
            .describe("Two copies. The head, the muzzle, the ears and the whole neck are flat "
                    + "black - not a dark coat colour but an absence, painted at full opacity so "
                    + "that nothing the horse's own genes did shows through it. The eyes are gold "
                    + "and burn, with no white left around them, and they are the only thing on "
                    + "the front of the horse a viewer can find. Where the black meets the barrel "
                    + "it does not stop in a line: tapering tentacles run out of it and down over "
                    + "the shoulder, thinning as they go.")
            .tint(ShadowcreatureGene::paint);

    private final List<Expression> expressions = List.of(WILD, SHADOW);

    /** The homozygote and the plain horse, and nothing between them - see the class note. */
    private final FounderTable founders = FounderTable.builder()
            .weight(Shc, Shc, 0.04)
            .weight(n, n, 99.96)
            .build();

    private final List<GeneAbility> glow = List.of(
            new GeneAbility.Glow(LIGHT_LEVEL, List.of(), GeneAbility.Condition.ALWAYS, 1));

    @Override public String key() { return KEY; }
    @Override public String name() { return "Shadowcreature"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.homozygousFor(Shc) ? SHADOW : WILD;
    }

    /**
     * The four numbers a copy carries. On the <b>copy</b> rather than on the
     * allele for the usual reason: two shadowcreatures should not be the same
     * animal, and a foal that inherits the copy inherits its parent's tentacles
     * rather than rolling its own.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.seed(SEED),
                EpiValue.uniform(REACH, 0.30, 0.62),
                EpiValue.uniform(SPACING, 1.5, 2.6),
                EpiValue.uniform(WIDTH, 0.55, 1.0));
    }

    // ------------------------------------------------------------------
    // Phase 3 - the black, and what runs out of it
    // ------------------------------------------------------------------

    /** The parts that go black outright. The neck is included whole; the barrel never is. */
    private static final List<Part> DARK = List.of(
            Part.HEAD, Part.MUZZLE, Part.NECK, Part.LEFT_EAR, Part.RIGHT_EAR);

    private static ColorField paint(CoatBuildContext ctx, PigmentView coat, ColorView accumulated) {
        ColorField delta = ColorField.deltaLike(accumulated);
        for (Part part : DARK) {
            if (!HorseSkinGeometry.hasPart(ctx.skin(), part)) {
                continue;
            }
            HorseSkinGeometry.forEachTexel(ctx.skin(), part, (px, py, p, face, point) ->
                    delta.set(px, py, 255,
                            (VOID >> 16) & 0xFF, (VOID >> 8) & 0xFF, VOID & 0xFF));
        }
        tentacles(ctx, ctx.epigeneticsFor(KEY), accumulated, delta);
        return delta;
    }

    /**
     * Tapering strokes running down the barrel out of the black neck.
     *
     * <p>The shape is the {@code STROKES} mask's, deliberately and not by
     * coincidence: the distance to a value-noise level set, <b>divided through
     * by the field's own gradient</b>. The naive {@code |n - 0.5|} does not
     * work - value noise is flat near its extrema and steep between them, so a
     * fixed band is a hairline over half the horse and a blot over the other
     * half. See {@code SpecPainter.levelSetDistance}: the same three extra
     * samples, for the same reason.
     *
     * <p>What is <i>not</i> shared with that mask is the falloff, and it is
     * what makes these tentacles rather than stripes. A tentacle has to come
     * <b>out of</b> the neck, so coverage is scaled by how far the texel sits
     * from the shoulder, normalised by this copy's {@code reach} and squared -
     * so a tentacle spends most of its length thinning rather than most of it
     * solid, and finishes by running out instead of by stopping.
     */
    private static void tentacles(CoatBuildContext ctx, EpiValues epi,
                                  ColorView accumulated, ColorField delta) {
        Bounds body = HorseSkinGeometry.bounds(ctx.skin(), Part.BODY);
        // The barrel runs tail (xMin) to shoulder (xMax) and the neck joins at
        // the shoulder, so distance is measured back from xMax.
        double shoulder = body.max(Axis.X);
        double reach = Math.max(1e-6, epi.get(REACH) * body.span(Axis.X));
        double spacing = Math.max(0.05, epi.get(SPACING));
        double half = Math.max(1e-4, epi.get(WIDTH)) / 2;
        long seed = epi.seed(SEED);

        HorseSkinGeometry.forEachTexel(ctx.skin(), Part.BODY, (px, py, part, face, point) -> {
            double back = shoulder - point.x();
            if (back < 0 || back > reach) {
                return;
            }
            double fade = 1.0 - back / reach;
            fade *= fade;
            double d = levelSetDistance(seed,
                    point.x() / (spacing * 4.0), point.y() / spacing, point.z() / spacing) * spacing;
            double k = (1.0 - BodyStripes.smoothstep(half, half + 0.3, d)) * fade;
            if (k <= 0) {
                return;
            }
            // A walk toward the void, exactly as LightGene walks toward its
            // gold: the delta is what has to be ADDED to the accumulator to
            // make the texel LOOK k of the way to VOID. Writing the colour in
            // directly would ignore whatever the horse already is.
            delta.add(px, py,
                    toward(accumulated, px, py, 0, (VOID >> 16) & 0xFF, k),
                    toward(accumulated, px, py, 1, (VOID >> 8) & 0xFF, k),
                    toward(accumulated, px, py, 2, VOID & 0xFF, k));
            delta.addOpacity(px, py, (int) Math.round((255 - accumulated.opacity(px, py)) * k));
        });
    }

    /** What to add to channel {@code c} so the texel looks {@code k} of the way to {@code target}. */
    private static int toward(ColorView colour, int px, int py, int c, int target, double k) {
        int seen = colour.visible(px, py, c);
        double wanted = seen + (target - seen) * k;
        int stored = switch (c) {
            case 0 -> colour.red(px, py);
            case 1 -> colour.green(px, py);
            default -> colour.blue(px, py);
        };
        return (int) Math.round(wanted - stored);
    }

    /** {@code SpecPainter.levelSetDistance}, which is private there. */
    private static double levelSetDistance(long seed, double x, double y, double z) {
        double n = BodyNoise.value(seed, x, y, z);
        double e = 0.25;
        double gx = (BodyNoise.value(seed, x + e, y, z) - n) / e;
        double gy = (BodyNoise.value(seed, x, y + e, z) - n) / e;
        double gz = (BodyNoise.value(seed, x, y, z + e) - n) / e;
        double grad = Math.sqrt(gx * gx + gy * gy + gz * gz);
        return Math.abs(n - 0.5) / Math.max(grad, 1e-4);
    }

    // ------------------------------------------------------------------
    // Overlay - the eyes
    // ------------------------------------------------------------------

    @Override
    public void overlay(AllelePair pair, CoatBuildContext ctx, CoatOverlay out) {
        if (!pair.homozygousFor(Shc)) {
            return;
        }
        // shadeEyes, not tintIris: the WHOLE eye goes gold, which is what "no
        // sclera" means. tintIris would leave the white ring round it intact.
        out.shadeEyes(GOLD, EYE_STRENGTH);
        out.markEmissiveEyes();
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        return pair.homozygousFor(Shc) ? glow : List.of();
    }
}
