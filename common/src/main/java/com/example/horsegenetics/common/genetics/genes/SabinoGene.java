package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PatchNoise;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

/**
 * <b>Sabino 1</b> ({@code horsegenetics.sabino}) - real-horse {@code KIT}
 * {@code SB1}. {@code SB1} / {@code sb1}, <b>incomplete dominant</b>, and the
 * one gene that <b>reads its own dose</b> (unlike {@code splash}).
 *
 * <ul>
 *   <li>{@code SB1/sb1} - tall <b>jagged</b> stockings, a belly splash, a broad
 *       blaze, a little roaning at the edges.</li>
 *   <li>{@code SB1/SB1} - "sabino-white": 90%+ white, a few coloured flecks on
 *       the ears / flank.</li>
 * </ul>
 *
 * <p>Every field is a warped fractal ({@link PatchNoise}) sampled in body space
 * - the earlier version used raw low-frequency value noise, whose integer
 * lattice showed as axis-aligned <b>squares</b>.
 *
 * <p>Natural, <b>non-deterministic</b>. Draws off the expressing {@code SB1}
 * copy, in order: {@code nextLong()} (the noise seed), four {@code nextFloat()}s
 * (one leg-white height per leg), then {@code nextFloat()} for the belly patch
 * and {@code nextFloat()} for the face.
 */
public final class SabinoGene implements Gene {

    public static final String KEY = "horsegenetics.sabino";
    public static final int WILD_SABINO_ONE_IN = 45;

    /** Leg-white height as a fraction of leg height: dose 1 band, then dose 2 band. */
    private static final double LEG1_MIN = 0.35, LEG1_RANGE = 0.42;
    private static final double LEG2_MIN = 0.78, LEG2_RANGE = 0.32;
    /** How far the jagged edge wanders, in fractions of leg height. */
    private static final double LEG_JAG = 0.18;

    public final Allele SB1 = new Allele(KEY, 0, "SB1", "Sabino 1 (SB1)");
    public final Allele sb1 = new Allele(KEY, 1, "sb1", "Wild-type (sb1)");
    private final List<Allele> alleles = List.of(SB1, sb1);

    private final Expression WILD = Expression.wildType("No white markings.");

    private final Expression SABINO = Expression.of("sabino1", "Sabino 1")
            .describe("Tall jagged stockings, a splash of white up the belly, a broad blaze, and a "
                    + "little roaning at the margins - the edges are ragged rather than the clean "
                    + "ring a splash sock leaves.")
            .varies()
            .restrict((ctx, coat) -> paintSabino(ctx, coat, 1));

    private final Expression SABINO_WHITE = Expression.of("sabino-white", "Sabino-white")
            .describe("Ninety per cent white or more, with a few coloured flecks left on the ears "
                    + "and flank. Two copies, and unmistakably not just a bolder sabino.")
            .varies()
            .restrict((ctx, coat) -> paintSabino(ctx, coat, 2));

    private final List<Expression> expressions = List.of(WILD, SABINO, SABINO_WHITE);

    private final FounderTable founders = FounderTable.hardyWeinberg(SB1, sb1, 1.0 / WILD_SABINO_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Sabino 1"; }
    @Override public int priority() { return 76; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return sb1; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * The gene that made the dominance vocabulary creak: all three combinations
     * land somewhere different, and the difference between one copy and two is
     * not a matter of degree.
     */
    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(SB1)) {
            case 2 -> SABINO_WHITE;
            case 1 -> SABINO;
            default -> WILD;
        };
    }

    /** 0, 1 or 2 copies of {@code SB1}. */
    public int dose(AllelePair pair) {
        return pair.count(SB1);
    }

    private static PigmentField paintSabino(CoatBuildContext ctx, PigmentView coat, int dose) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double[] legH = new double[CoatRegions.LEGS.size()];
        for (int i = 0; i < legH.length; i++) {
            legH[i] = dose == 2
                    ? LEG2_MIN + epi.nextFloat() * LEG2_RANGE
                    : LEG1_MIN + epi.nextFloat() * LEG1_RANGE;
        }
        double bellyRoll = epi.nextFloat();
        double faceRoll = epi.nextFloat();

        Skin skin = ctx.skin();
        Bounds body = HorseSkinGeometry.bodyBounds(skin);
        double bellyLine = body.yMin() + body.span(Axis.Y) * (dose == 2 ? 0.66 : 0.34 + bellyRoll * 0.14);
        double faceHalf = (dose == 2 ? 3.2 : 1.0 + faceRoll * 2.0);
        // dose 2: the body-wide roaning threshold is low, so most of the coat whitens.
        double bodyCut = dose == 2 ? 0.34 : 0.74;
        double bellyCut = dose == 2 ? 0.34 : 0.52 - 0.14 * bellyRoll;

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            int legIndex = CoatRegions.LEGS.indexOf(part);
            if (legIndex >= 0) {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double frac = (point.y() - b.yMin()) / b.span(Axis.Y);
                double jag = (PatchNoise.fbm2(seed ^ (0x11L * (legIndex + 1)),
                        point.x() * 1.6, point.y() * 1.6, point.z() * 2.4) - 0.5) * 2.0 * LEG_JAG;
                if (frac < legH[legIndex] + jag) {
                    whiten(f, px, py);
                }
                return;
            }
            if (part == Part.HEAD || part == Part.MUZZLE) {
                if (Math.abs(point.z()) <= faceHalf) {
                    whiten(f, px, py);
                }
                return;
            }
            if (part == Part.LEFT_EAR || part == Part.RIGHT_EAR) {
                return; // ears keep colour, even on a near-white sabino
            }
            // belly splash - a soft-bottomed patch rising from the underline
            if (point.y() < bellyLine) {
                double n = PatchNoise.field(seed ^ 0x7EL, point.x(), point.y(), point.z(), 0.12);
                double rise = 1.0 - PatchNoise.smoothstep(bellyLine - body.span(Axis.Y) * 0.18, bellyLine, point.y());
                if (n * (0.4 + 0.6 * rise) > bellyCut) {
                    whiten(f, px, py);
                    return;
                }
            }
            // body roaning / white - dense only at dose 2
            double bw = PatchNoise.fbm2(seed ^ 0x1234L, point.x() * 1.5, point.y() * 1.5, point.z() * 2.3);
            if (bw > bodyCut) {
                whiten(f, px, py);
            }
        });
        return f;
    }

    private static void whiten(PigmentField f, int px, int py) {
        f.setRed(px, py, 0f);
        f.setBlack(px, py, 0f);
    }
}
