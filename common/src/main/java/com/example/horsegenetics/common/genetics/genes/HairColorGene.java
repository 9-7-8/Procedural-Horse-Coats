package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape shared by <b>mane colour</b> ({@link ManeColorGene}) and <b>tail
 * colour</b> ({@link TailColorGene}) - two separate loci that do the same thing
 * to different hair, so a horse can have a lime mane and a magenta tail, or
 * either alone.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code <solid>/n}, {@code <solid>/<solid>}</td><td>{@code solid} - the hair is one colour</td></tr>
 *   <tr><td>{@code <striped>/n}, {@code <striped>/<striped>}</td><td>{@code striped} - bands of colour across the hair</td></tr>
 *   <tr><td>{@code <solid>/<striped>}</td><td>{@code solid-striped} - the solid colour, with the other copy's bands over it</td></tr>
 * </table>
 *
 * <h2>The heterozygote is the interesting one</h2>
 * {@code solid/striped} is not a compromise between the two and it is not one
 * of them winning: it is <b>both, at once, in two different colours</b>. That
 * needs something no other gene in the mod has needed - the epigenetics of
 * <i>each copy separately</i> rather than "the copy that expresses"
 * ({@link CoatBuildContext#epigeneticsForCopy}). Asking for the expressed copy
 * would paint the stripes in the base colour, which is a horse with no stripes.
 *
 * <h2>Where the colour lives</h2>
 * Each allele copy carries <b>one</b> epigenetic value: its colour, drawn as a
 * bright hue off that copy's seed. So a colour is a property of the allele copy
 * and travels with it: breed a horse with a specific mane and its foals inherit
 * that exact colour, not a new roll. The gene has no palette and no list - the
 * hue circle is continuous, and two unrelated horses agreeing on a colour is
 * essentially impossible.
 *
 * <p>The paint walks each texel {@value #STRENGTH_PERCENT}% of the way to the
 * colour rather than replacing it, so the hair keeps its own strand shading -
 * see {@link HairPattern}. On a foal the mane locus draws nothing, because the
 * foal mesh has no mane; the tail locus works on both.
 */
public abstract class HairColorGene implements Gene {

    /** How far of the way to the target colour. Short of 100 so the strands keep their shading. */
    public static final int STRENGTH_PERCENT = 88;

    /** Band period, in body units (1 = 1/16 block). The adult mane is 16 units long. */
    private static final double SPACING_MIN = 1.6;
    private static final double SPACING_RANGE = 2.4;
    /** Fraction of each period the band covers. */
    private static final double DUTY_MIN = 0.34;
    private static final double DUTY_RANGE = 0.26;

    private final String key;
    private final String name;
    private final int priority;
    private final Part part;

    private final Allele solid;
    private final Allele striped;
    private final Allele wild;
    private final List<Allele> alleles;

    private final Expression WILD;
    private final Expression SOLID;
    private final Expression STRIPED;
    private final Expression BOTH;
    private final List<Expression> expressions;
    private final FounderTable founders;

    protected HairColorGene(String key, String name, int priority, Part part,
                            String solidToken, String solidLabel,
                            String stripedToken, String stripedLabel,
                            String hair, double solidFrequency, double stripedFrequency) {
        this.key = key;
        this.name = name;
        this.priority = priority;
        this.part = part;

        this.solid = new Allele(key, 0, solidToken, solidLabel);
        this.striped = new Allele(key, 1, stripedToken, stripedLabel);
        this.wild = new Allele(key, 2, "n", "Wild-type (n)");
        this.alleles = List.of(solid, striped, wild);

        this.WILD = Expression.wildType("The " + hair + " keeps its coat colour.");

        this.SOLID = Expression.of("solid", "Solid " + hair)
                .describe("The whole " + hair + " is one bright colour, kept from the copy that "
                        + "carries it and passed on with the allele.")
                .varies()
                .tint(this::paintSolid);

        this.STRIPED = Expression.of("striped", "Striped " + hair)
                .describe("Bands of one bright colour run across the " + hair + ", irregularly "
                        + "spaced. The colour and the spacing come from the copy that carries it.")
                .varies()
                .tint(this::paintStriped);

        this.BOTH = Expression.of("solid-striped", "Striped over solid")
                .describe("One copy of each. The " + hair + " takes the solid copy's colour and the "
                        + "striped copy's bands are drawn over it - two independent colours on one "
                        + "horse, which is a look neither allele can make on its own.")
                .varies()
                .tint(this::paintBoth);

        this.expressions = List.of(WILD, SOLID, STRIPED, BOTH);
        // Baseline last, so a high founder roll is the ordinary horse, and a
        // LinkedHashMap because the iteration order is the table's row order.
        Map<Allele, Double> frequencies = new LinkedHashMap<>();
        frequencies.put(this.solid, solidFrequency);
        frequencies.put(this.striped, stripedFrequency);
        frequencies.put(this.wild, 1.0 - solidFrequency - stripedFrequency);
        this.founders = FounderTable.hardyWeinberg(frequencies, pair -> true);
    }

    /** The solid-colour allele of this locus. */
    public Allele solid() {
        return solid;
    }

    /** The striped allele of this locus. */
    public Allele striped() {
        return striped;
    }

    @Override public String key() { return key; }
    @Override public String name() { return name; }
    @Override public int priority() { return priority; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return wild; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        boolean hasSolid = pair.has(solid);
        boolean hasStriped = pair.has(striped);
        if (hasSolid && hasStriped) {
            return BOTH;
        }
        if (hasSolid) {
            return SOLID;
        }
        return hasStriped ? STRIPED : WILD;
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * The four numbers one allele copy carries, in a fixed draw order: colour
     * (three floats), then the band seed, spacing and duty. Every copy draws all
     * of them whichever role it ends up in, so an allele's colour is the same
     * whether it is painting a solid hair or a set of bands.
     */
    private record Hair(int rgb, long seed, double spacing, double duty) {}

    private static Hair draw(Rng rng) {
        int rgb = HairPattern.randomBrightColour(rng);
        long seed = rng.nextLong();
        double spacing = SPACING_MIN + rng.nextFloat() * SPACING_RANGE;
        double duty = DUTY_MIN + rng.nextFloat() * DUTY_RANGE;
        return new Hair(rgb, seed, spacing, duty);
    }

    private static double strength() {
        return STRENGTH_PERCENT / 100.0;
    }

    private ColorField paintSolid(CoatBuildContext ctx, PigmentView coat, ColorView accumulated) {
        Hair hair = draw(ctx.epigeneticsFor(key));
        ColorField delta = ColorField.deltaLike(accumulated);
        HairPattern.paint(ctx, accumulated, delta, part,
                (px, py, point) -> new HairPattern.Tone(hair.rgb(), strength()));
        return delta;
    }

    private ColorField paintStriped(CoatBuildContext ctx, PigmentView coat, ColorView accumulated) {
        Hair hair = draw(ctx.epigeneticsFor(key));
        ColorField delta = ColorField.deltaLike(accumulated);
        HairPattern.paint(ctx, accumulated, delta, part, (px, py, point) -> {
            double c = HairPattern.bands(ctx.skin(), part, point, hair.seed(), hair.spacing(), hair.duty());
            return c <= 0 ? null : new HairPattern.Tone(hair.rgb(), strength() * c);
        });
        return delta;
    }

    /**
     * The heterozygote. The solid allele always sits in slot 0 of the pair (it
     * is declared first, and {@code AllelePair} orders by declaration), so the
     * two copies can be told apart without guessing.
     */
    private ColorField paintBoth(CoatBuildContext ctx, PigmentView coat, ColorView accumulated) {
        Hair base = draw(ctx.epigeneticsForCopy(key, 0));
        Hair band = draw(ctx.epigeneticsForCopy(key, 1));
        ColorField delta = ColorField.deltaLike(accumulated);
        HairPattern.paint(ctx, accumulated, delta, part, (px, py, point) -> {
            double c = HairPattern.bands(ctx.skin(), part, point, band.seed(), band.spacing(), band.duty());
            int rgb = c <= 0 ? base.rgb() : blend(base.rgb(), band.rgb(), c);
            return new HairPattern.Tone(rgb, strength());
        });
        return delta;
    }

    /** {@code t} of the way from {@code from} to {@code to}, per channel. */
    private static int blend(int from, int to, double t) {
        int r = channel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = channel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = channel(from & 0xFF, to & 0xFF, t);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }
}
