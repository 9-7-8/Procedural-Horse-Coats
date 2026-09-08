package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.List;

/**
 * <b>Hued pangare</b> ({@code horsegenetics.hued_pangare}) - pangare's shape in
 * a colour the line carries, instead of in cream.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Hpa/n}</td><td>{@code classic-hued-pangare}</td></tr>
 *   <tr><td>{@code Hpa/Hpa}</td><td>{@code extreme-hued-pangare}</td></tr>
 * </table>
 *
 * <h2>Why this is a class and not a gene file</h2>
 * It was a gene file, and the file was an <b>approximation</b> of pangare: a Y
 * band up the belly and a second one down the neck, tuned by eye until it looked
 * about right. It never was about right, because pangare is not a band. It is a
 * region map with nine named weights on it - muzzle, eye ring, jaw, belly,
 * flank, elbow, inner leg, outer leg, lower neck - each hung on its own shape,
 * plus a mottle so the boundary is hairs rather than an airbrush. Two hand-tuned
 * copies of that would drift apart the first time either was touched.
 *
 * <p>So this gene <b>calls</b> {@link PangareGene#mealyCoverage} rather than
 * describing the same field again. It is literally the same code: change where
 * pangare sits and this moves with it, in the same commit, with nothing to
 * remember. What it does differently is the only thing it should do
 * differently - pangare takes red <i>out</i> of the soft parts in phase 1, and
 * this walks them <i>toward</i> a hue in phase 3.
 *
 * <p><b>Ordinary pangare is not changed by any of this.</b> The shared method
 * was factored out of its own painter and it still paints exactly what it did.
 *
 * <p>Magical, <b>non-deterministic</b> - the hue and the intensity offset are
 * both stored per allele copy, so a foal that inherits the copy inherits the
 * colour. See {@code wiki/gene-hued-pangare.html}.
 */
public final class HuedPangareGene implements Gene {

    public static final String KEY = "horsegenetics.hued_pangare";

    /**
     * Sorted where the gene file had it: after the white patterns, among the
     * magical field genes.
     */
    public static final int PRIORITY = 254;

    /**
     * The dosage one copy is worth on {@link PangareGene}'s own scale, which
     * runs to {@link PangareGene#MAX_DOSAGE}. Two copies therefore reach the
     * top of it and one lands a little under half way - which is the "a band up
     * the belly with one copy, half the horse with two" the outcome table
     * promises, expressed in the units pangare already thinks in rather than in
     * a second set of numbers.
     */
    private static final double DOSAGE_PER_COPY = 2.0;

    /** How far toward the hue a fully covered texel goes. Short of 100, so shading survives. */
    private static final double STRENGTH = 0.86;

    private static final double SATURATION = 0.78;
    private static final double LIGHTNESS = 0.58;

    public final Allele Hpa = new Allele(KEY, 0, "Hpa", "Hued Pangare (Hpa)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Hpa, n);

    private final Expression WILD = Expression.wildType("No hued pangare.");

    private final Expression CLASSIC = Expression.of("classic", "Classic Hued Pangare")
            .describe("Pangare's own field, in colour rather than in cream, at about the strength a "
                    + "moderate mealy horse shows: the muzzle and the rings round the eyes, a "
                    + "coloured underline up the belly, soft patches behind the elbows and in "
                    + "front of the stifle, and a little way up the inside of each leg. Every "
                    + "boundary is a soft mottled fade - there are no edges on this marking - and "
                    + "the mane, tail, ears and topline are left alone, because pangare is a "
                    + "soft-parts trait and so is this.")
            .varies()
            .tint(this::paint);

    private final Expression EXTREME = Expression.of("extreme", "Extreme Hued Pangare")
            .describe("The same field at full strength: the colour reaches up the flanks and the "
                    + "sides of the neck, over the cheek, jaw and muzzle and round the eye, and "
                    + "most of the way up the inside of all four legs. Half the horse or better. "
                    + "It is not a different shape from the classic form - it is the same map "
                    + "turned all the way up, which is what pangare itself does between a trace "
                    + "and a strong one.")
            .varies()
            .tint(this::paint);

    private final List<Expression> expressions = List.of(WILD, CLASSIC, EXTREME);

    /**
     * Rare, and rarer still as a homozygote - the frequencies the gene file
     * carried, kept so the conversion did not quietly change how often the
     * horse turns up.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(Hpa, Hpa, 0.006)
            .weight(Hpa, n, 0.114)
            .weight(n, n, 99.88)
            .build();

    @Override public String key() { return KEY; }

    @Override public String name() { return "Hued Pangare"; }

    @Override
    public String description() {
        return "Pangare's own region map painted in a colour the line carries instead of in cream - "
                + "muzzle, eye rings, belly, flank and the insides of the legs, with no edge "
                + "anywhere on it.";
    }

    @Override public int priority() { return PRIORITY; }

    @Override public boolean isNatural() { return false; }

    @Override public List<Allele> alleles() { return alleles; }

    @Override public Allele defaultAllele() { return n; }

    @Override public List<Expression> expressions() { return expressions; }

    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Hpa)) {
            case 2 -> EXTREME;
            case 1 -> CLASSIC;
            default -> WILD;
        };
    }

    /**
     * The hue this line runs to, and the same expression offset pangare stores -
     * so two horses of the same dosage read a shade apart here as well.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform("hue", 0, 360),
                EpiValue.uniform("expression",
                        -PangareGene.EXPRESSION_RANGE, PangareGene.EXPRESSION_RANGE));
    }

    private ColorField paint(CoatBuildContext ctx, PigmentView coat, ColorView accumulated) {
        AllelePair pair = ctx.genotype().pair(this);
        EpiValues epi = ctx.epigeneticsFor(KEY);
        double intensity = PangareGene.strength(
                PangareGene.score(DOSAGE_PER_COPY * (pair == null ? 0 : pair.count(Hpa)), epi));
        int rgb = hsl(epi.get("hue"), SATURATION, LIGHTNESS);
        int wantR = (rgb >> 16) & 0xFF;
        int wantG = (rgb >> 8) & 0xFF;
        int wantB = rgb & 0xFF;

        ColorField delta = ColorField.deltaLike(accumulated);
        HorseSkinGeometry.forEachTexel(ctx.skin(), (px, py, part, face, point) -> {
            double k = PangareGene.mealyCoverage(ctx.skin(), part, face, point) * intensity * STRENGTH;
            if (k <= 0) {
                return;
            }
            delta.add(px, py,
                    toward(accumulated, px, py, 0, wantR, k),
                    toward(accumulated, px, py, 1, wantG, k),
                    toward(accumulated, px, py, 2, wantB, k));
            delta.addOpacity(px, py, (int) Math.round((255 - accumulated.opacity(px, py)) * k));
        });
        return delta;
    }

    /**
     * The signed step from what this texel <b>looks like</b> to a share of the
     * way toward {@code target} - the same move every colour gene here makes,
     * and the reason one hue lands correctly on a bay and on a cremello.
     */
    private static int toward(ColorView colour, int px, int py, int channel, int target, double k) {
        int seen = colour.visible(px, py, channel);
        double wanted = seen + (target - seen) * k;
        int stored = switch (channel) {
            case 0 -> colour.red(px, py);
            case 1 -> colour.green(px, py);
            default -> colour.blue(px, py);
        };
        return (int) Math.round(wanted - stored);
    }

    /** HSL to {@code 0xRRGGBB}. Degrees, wrapping; saturation and lightness clamp. */
    private static int hsl(double h, double s, double l) {
        double hue = (((h % 360) + 360) % 360) / 60.0;
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs(hue % 2 - 1));
        double m = l - c / 2;
        double r;
        double g;
        double b;
        if (hue < 1) {
            r = c; g = x; b = 0;
        } else if (hue < 2) {
            r = x; g = c; b = 0;
        } else if (hue < 3) {
            r = 0; g = c; b = x;
        } else if (hue < 4) {
            r = 0; g = x; b = c;
        } else if (hue < 5) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        return (channel(r + m) << 16) | (channel(g + m) << 8) | channel(b + m);
    }

    private static int channel(double v) {
        int i = (int) Math.round(v * 255);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }
}
