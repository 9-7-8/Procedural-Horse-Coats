package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
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

import java.util.List;

/**
 * <b>Pink hair</b> ({@code horsegenetics.pink_hair}) - a <b>magical</b> gene,
 * and the model's clearest carrier locus.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code n/Pihr}</td><td>wild type - a carrier you cannot see</td></tr>
 *   <tr><td>{@code Pihr/Pihr}</td><td>{@code pink-hair} - the mane and tail turn hot pink</td></tr>
 * </table>
 *
 * <p>Two of the three combinations landing on a wild type is exactly what the
 * word "recessive" used to mean, and the table says it without needing the word.
 * Founder frequency {@code 1/}{@value #WILD_PIHR_ONE_IN} per allele, so roughly
 * one wild horse in {@value #WILD_PIHR_HORSE_ODDS} is born with it and a good
 * many more carry it - which is the point: an invisible carrier is something you
 * <i>breed for</i>.
 *
 * <p>The pink is <b>not</b> flat paint - that would throw away the shading the
 * natural phase gave those strands and leave a dead pink patch. Instead the
 * expression <i>reads</i> what each hair texel currently looks like
 * ({@link ColorView#visible}) and returns the delta that walks it
 * {@value #STRENGTH_PERCENT}% of the way to hot pink, so the mane keeps its own
 * light and dark while ending up unmistakably pink on a black, a chestnut or a
 * cremello alike. It raises opacity too, so a dominant-white horse gets pink
 * hair rather than nothing.
 *
 * <p>A blind {@code add} was tried first and can't do this: to reach pink on a
 * black mane it has to push so hard that a pale mane saturates to white. Reading
 * first is the point of the phase-3 read access - the cost is that this gene is
 * <b>order-dependent</b>, so it runs before magic zebra (whose stripes should
 * black out pink hair, not the other way round). See {@code Genes.magicalOrder}.
 *
 * <p>Deterministic - one intensity, no per-horse variation yet. Alleles for a
 * couple of intensities are the obvious extension, and are now just two more
 * rows in the table above.
 *
 * <p><b>Foals get a pink tail only.</b> The foal mesh has no {@code MANE} part
 * (see {@code HorseSkinGeometry}), so the mane comes in with adulthood.
 */
public final class PinkHairGene implements Gene {

    public static final String KEY = "horsegenetics.pink_hair";
    public static final int WILD_PIHR_ONE_IN = 12;
    /** Both copies, so the square of the per-allele odds - for the Javadoc above. */
    public static final int WILD_PIHR_HORSE_ODDS = WILD_PIHR_ONE_IN * WILD_PIHR_ONE_IN;

    /** Hot pink, the colour the hair is walked toward. */
    private static final int PINK_R = 255;
    private static final int PINK_G = 105;
    private static final int PINK_B = 180;
    /** How far of the way there. Short of 100 so the strands keep their shading. */
    public static final int STRENGTH_PERCENT = 82;

    private static final List<Part> HAIR = List.of(Part.MANE, Part.TAIL);

    public final Allele n = new Allele(KEY, 0, "n", "Wild-type (n)");
    public final Allele Pihr = new Allele(KEY, 1, "Pihr", "Pink hair (Pihr)");
    private final List<Allele> alleles = List.of(n, Pihr);

    private final Expression WILD = Expression.wildType("The mane and tail keep their coat colour.");

    private final Expression CARRIER = Expression.wildType(
            "pink-carrier", "Pink hair carrier",
            "One copy shows nothing at all. The horse looks ordinary and only passes the allele on - "
                    + "two carriers bred together are how pink hair appears.");

    private final Expression PINK = Expression.of("pink-hair", "Pink hair")
            .describe("The mane and tail walk most of the way to hot pink while keeping their own "
                    + "strand shading, on any base coat at all. A foal gets a pink tail; the mane "
                    + "arrives with adulthood.")
            .tint(PinkHairGene::paintHair);

    private final List<Expression> expressions = List.of(WILD, CARRIER, PINK);

    private final FounderTable founders = FounderTable.hardyWeinberg(Pihr, n, 1.0 / WILD_PIHR_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Pink hair"; }
    @Override public int priority() { return 110; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Pihr)) {
            case 2 -> PINK;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    public boolean isPinkHaired(AllelePair pair) {
        return pair.count(Pihr) == 2;
    }

    private static ColorField paintHair(CoatBuildContext ctx, PigmentView coat, ColorView accumulated) {
        Skin skin = ctx.skin();
        ColorField delta = ColorField.deltaLike(accumulated);
        for (Part part : HAIR) {
            if (!HorseSkinGeometry.hasPart(skin, part)) {
                continue; // a foal has no mane
            }
            HorseSkinGeometry.forEachTexel(skin, part, (px, py, p, face, point) -> {
                // The hair ends up fully opaque, so what it will look like is
                // just the accumulated colour - hence the delta that lands there.
                delta.add(px, py,
                        toward(accumulated, px, py, 0, PINK_R),
                        toward(accumulated, px, py, 1, PINK_G),
                        toward(accumulated, px, py, 2, PINK_B));
                delta.addOpacity(px, py, 255 - accumulated.opacity(px, py));
            });
        }
        return delta;
    }

    /** The signed step from this texel's accumulated channel to its pink one. */
    private static int toward(ColorView colour, int px, int py, int channel, int target) {
        int seen = colour.visible(px, py, channel);
        int wanted = (int) Math.round(seen + (target - seen) * (STRENGTH_PERCENT / 100.0));
        return wanted - switch (channel) {
            case 0 -> colour.red(px, py);
            case 1 -> colour.green(px, py);
            default -> colour.blue(px, py);
        };
    }
}
