package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyePatch;
import com.example.horsegenetics.common.genetics.EyePatchContribution;
import com.example.horsegenetics.common.genetics.EyePatches;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Magic sectoral heterochromia</b>
 * ({@code horsegenetics.magic_sectoral_heterochromia}) - a <b>magical</b> gene
 * that only exists in the heterozygote, and the mod's first gene that needs
 * <i>two different</i> variant alleles to show anything at all.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code <colour>/n}</td><td>{@code sectoral-carrier} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code <colour>/<same colour>}</td><td>{@code sectoral-matched} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code <colour>/<other colour>}</td><td>{@code sectoral-heterochromia} - a wedge of each colour in each eye</td></tr>
 * </table>
 *
 * <h2>The gene is the disagreement, not the colour</h2>
 * Six colour alleles - {@code green}, {@code blue}, {@code brown},
 * {@code hazel}, {@code gold}, {@code chaos} - and a wild type. A horse
 * carrying <b>two different</b> colour alleles shows both of them at once, in a
 * randomly shaped sector of each iris. Every other combination shows nothing:
 * one colour agreeing with itself has nothing to be sectoral <i>about</i>, and a
 * wild-type copy is a colour that was never claimed.
 *
 * <p>That is the whole design, and it is deliberately the opposite shape to
 * every other gene here. A dominant gene rewards finding one allele; a recessive
 * one rewards finding two of the same. This one rewards finding two
 * <b>different</b> ones, so its fifteen visible combinations are fifteen
 * separate breeding targets - fifteen <i>outcomes</i>, declared one apiece,
 * because a green-and-gold eye is not a blue-and-brown one - and none of them is
 * reachable by doubling up on a single lucky horse.
 *
 * <h2>It does not overwrite the horse's eye colour</h2>
 * A non-expressing combination contributes <i>nothing</i> - not a claim on the
 * iris, not a wild-type dark eye. A champagne horse carrying {@code gold/gold}
 * keeps its champagne eye, and a splashed white one carrying {@code blue/n}
 * keeps its blue. The gene reaches the coat through
 * {@link EyePatchContribution}, which paints over the eye-colour channel rather
 * than competing with it, so an expressing horse keeps whatever its natural eye
 * was everywhere the two sectors do not cover.
 *
 * <h2>Chaos</h2>
 * The {@code chaos} allele has no colour of its own; it takes one from <b>its
 * own copy's epigenetics</b>, so two horses carrying chaos rarely agree and a
 * chaos eye a breeder likes is inherited with that copy like any other
 * epigenetic trait. It is rolled in hue / saturation / value rather than as
 * three raw bytes, because an iris four texels across has to stay a colour and
 * not a near-black or a near-white.
 *
 * <p>{@code chaos/chaos} is still a matched pair and still shows nothing, even
 * though the two copies would have rolled different colours. The rule is about
 * the alleles, not about what they happen to look like.
 *
 * <h2>Shape</h2>
 * Each eye gets its own wedge ({@link EyePatch#WEDGES}), drawn from the seed of
 * one allele copy each - so the shape is inherited alongside the colour that
 * leads it - and the second eye's is guaranteed to differ from the first's. The
 * two colours also swap which of them leads: the first allele's colour takes the
 * wedge in the right eye and the rest of the left one. A sectoral eye that was
 * symmetric would not look like heterochromia, it would look like a decal.
 *
 * <p>Magical, non-deterministic. See {@code wiki/gene-magic-sectoral-heterochromia.html}.
 */
public final class MagicSectoralHeterochromiaGene implements Gene, EyePatchContribution {

    public static final String KEY = "horsegenetics.magic_sectoral_heterochromia";
    public static final int PRIORITY = 170; // magical band; only a code-order slot - it paints in the overlay

    /** Per <i>colour</i> allele, in the wild population. There are six of them. */
    public static final int WILD_ONE_IN = 260;

    public static final int GREEN_RGB = 0x4E8B3C;
    public static final int BLUE_RGB = 0x4A79C4;
    public static final int BROWN_RGB = 0x6B4A2A;
    public static final int HAZEL_RGB = 0x9A7B3F;
    public static final int GOLD_RGB = 0xD9A62B;

    public final Allele n = new Allele(KEY, 0, "n", "Wild-type (n)");
    public final Allele green = new Allele(KEY, 1, "green", "Green");
    public final Allele blue = new Allele(KEY, 2, "blue", "Blue");
    public final Allele brown = new Allele(KEY, 3, "brown", "Brown");
    public final Allele hazel = new Allele(KEY, 4, "hazel", "Hazel");
    public final Allele gold = new Allele(KEY, 5, "gold", "Gold");
    public final Allele chaos = new Allele(KEY, 6, "chaos", "Chaos");

    private final List<Allele> alleles = List.of(n, green, blue, brown, hazel, gold, chaos);

    /** The six variant alleles, in declaration order - everything but the wild type. */
    private final List<Allele> colours = List.of(green, blue, brown, hazel, gold, chaos);

    private final Expression WILD = Expression.wildType("Ordinary eyes.");

    private final Expression CARRIER = Expression.wildType("sectoral-carrier", "Sectoral carrier",
            "One colour allele and one wild type. Nothing shows: the gene needs two different "
                    + "colours to have anything to split the iris between. Breeding a carrier to a "
                    + "carrier of a different colour is how it appears.");

    private final Expression MATCHED = Expression.wildType("sectoral-matched", "Matched pair",
            "Two copies of the same colour, which cancel - a colour cannot be sectoral against "
                    + "itself. It shows nothing at all, and does not touch whatever eye colour the "
                    + "horse's other genes gave it.");

    /**
     * <b>One outcome per pair of colours</b> - fifteen of them, not one.
     *
     * <p>It would be shorter to declare a single {@code sectoral-heterochromia}
     * outcome for every expressing combination, and it would be wrong: an
     * {@link Expression} is what the gene <i>looks like</i>, and a green-and-gold
     * eye does not look like a blue-and-brown one. Everything downstream reads
     * the outcome list rather than the alleles - the gene dictionary, the wiki's
     * preview widget, the genotype catalogue - so collapsing them would show a
     * breeder one button where there are fifteen real, separately breedable
     * results.
     */
    private final Map<AllelePair, Expression> sectoralOutcomes = buildOutcomes();

    private final List<Expression> expressions = buildExpressionList();

    private Map<AllelePair, Expression> buildOutcomes() {
        Map<AllelePair, Expression> m = new LinkedHashMap<>();
        for (int i = 0; i < colours.size(); i++) {
            for (int j = i + 1; j < colours.size(); j++) {
                Allele a = colours.get(i);
                Allele b = colours.get(j);
                m.put(new AllelePair(a, b), Expression
                        .of("sectoral-" + a.token() + "-" + b.token(),
                                a.label() + " & " + b.label().toLowerCase(Locale.ROOT))
                        .describe("A randomly shaped wedge of " + a.label().toLowerCase(Locale.ROOT)
                                + " and the rest of the iris " + b.label().toLowerCase(Locale.ROOT)
                                + ", with a different shape in each eye and the two colours leading "
                                + "opposite sides. The shapes are inherited with the alleles that "
                                + "drew them"
                                + (a == chaos || b == chaos
                                        ? ", and the chaos half is a colour of its own on almost "
                                                + "every horse." : "."))
                        .varies()
                        .marker());
            }
        }
        return m;
    }

    private List<Expression> buildExpressionList() {
        List<Expression> out = new ArrayList<>();
        out.add(WILD);
        out.add(CARRIER);
        out.add(MATCHED);
        out.addAll(sectoralOutcomes.values());
        return List.copyOf(out);
    }

    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), p -> true);

    private Map<Allele, Double> frequencies() {
        // Wild type last: iteration order is the founder table's row order, and
        // every table in the mod puts its rarest combination first and its
        // baseline last.
        Map<Allele, Double> p = new LinkedHashMap<>();
        double each = 1.0 / WILD_ONE_IN;
        p.put(green, each);
        p.put(blue, each);
        p.put(brown, each);
        p.put(hazel, each);
        p.put(gold, each);
        p.put(chaos, each);
        p.put(n, 1.0 - each * 6);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic sectoral heterochromia"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /** Two different colour alleles and nothing else. */
    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.has(n)) {
            return pair.homozygousFor(n) ? WILD : CARRIER;
        }
        if (pair.homozygous()) {
            return MATCHED;
        }
        return sectoralOutcomes.get(pair);
    }

    public boolean shows(AllelePair pair) {
        return !pair.has(n) && !pair.homozygous();
    }

    /**
     * The two wedges. Draw order is the contract, and it is <b>per allele
     * copy</b>: each copy's seed yields a wedge and then a chaos colour, the
     * chaos draws happening whether or not that copy is the chaos allele, so
     * changing one allele never shifts the other copy's shape.
     *
     * <p>The left eye's wedge is drawn with {@link EyePatch#differentWedge} so
     * the two eyes cannot come out matching - which at twelve shapes would
     * otherwise happen to one horse in twelve and read as a bug rather than as
     * a coincidence.
     */
    /**
     * Per copy: which wedge of the iris it claims, how far the other eye's wedge
     * is rotated from it, and the colour a {@code chaos} copy shows.
     *
     * <p>The wedges are categories, so drift never rotates a horse's sector a
     * step at a time - a line keeps the shape of its eyes.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                        EpiValue.category("wedge", EyePatch.wedgeCount()),
                        EpiValue.category("wedge_step", EyePatch.wedgeStepCount()))
                .and(EpiValue.colour("chaos", 0.45, 0.95, 0.45, 0.90));
    }

    @Override
    public Optional<EyePatches> eyePatches(AllelePair pair, Genotype genotype, Epigenome epigenome,
                                           double whiteCoverage) {
        if (!shows(pair)) {
            return Optional.empty();
        }
        GeneEpigenetics rnd = GeneEpigenetics.forGene(this, genotype, epigenome);
        EpiValues first = rnd.copy(0);
        EpiValues second = rnd.copy(1);

        int rightWedge = EyePatch.wedgeAt(first.category("wedge"));
        int leftWedge = EyePatch.differentWedgeAt(rightWedge, second.category("wedge_step"));
        EyeColor a = colorOf(pair.first(), first);
        EyeColor b = colorOf(pair.second(), second);

        return Optional.of(new EyePatches(
                List.of(new EyePatch(rightWedge, a),
                        new EyePatch(EyePatch.complement(rightWedge), b)),
                List.of(new EyePatch(leftWedge, b),
                        new EyePatch(EyePatch.complement(leftWedge), a))));
    }

    /**
     * One allele's colour. Every copy stores a chaos colour whichever allele it
     * carries, so a fixed-colour copy that is later spliced to {@code chaos}
     * already has one rather than being invented on the spot.
     */
    private EyeColor colorOf(Allele allele, EpiValues epi) {
        if (allele == chaos) {
            return patchColor("chaos", "Chaos", epi.rgb("chaos"));
        }
        if (allele == green) {
            return patchColor("magic-green", "Green", GREEN_RGB);
        }
        if (allele == blue) {
            return patchColor("magic-blue", "Blue", BLUE_RGB);
        }
        if (allele == brown) {
            return patchColor("magic-brown", "Brown", BROWN_RGB);
        }
        if (allele == hazel) {
            return patchColor("magic-hazel", "Hazel", HAZEL_RGB);
        }
        return patchColor("magic-gold", "Gold", GOLD_RGB);
    }

    /**
     * A patch colour. The rank is never read - {@link EyePatchContribution}
     * paints rather than competes - so it takes the iris-specific one, which is
     * the honest description of what this gene is.
     */
    private static EyeColor patchColor(String id, String name, int rgb) {
        return EyeColor.pigment(id, name, rgb);
    }

    /**
     * HSV to {@code 0xRRGGBB}, hand-rolled because {@code common/} may not use
     * {@code java.awt} (it targets TeaVM and, one day, a 1.12.2 module).
     * {@code h}, {@code s} and {@code v} are all in {@code [0,1]}.
     */
    static int hsvToRgb(float h, float s, float v) {
        float sector = (h - (float) Math.floor(h)) * 6.0f;
        int i = (int) sector;
        float f = sector - i;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        float r;
        float g;
        float b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (byteOf(r) << 16) | (byteOf(g) << 8) | byteOf(b);
    }

    private static int byteOf(float v) {
        int i = Math.round(v * 255f);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }
}
