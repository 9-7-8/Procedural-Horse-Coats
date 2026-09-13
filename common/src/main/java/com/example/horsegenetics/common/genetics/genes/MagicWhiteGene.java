package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;

import java.util.List;

/**
 * <b>Magic white</b> ({@code horsegenetics.magic_white}) - a magical recessive
 * that paints the whole horse white, and the coat half of what used to be the
 * dhampir gene.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Wm/n}</td><td>a silent carrier</td></tr>
 *   <tr><td>{@code Wm/Wm}</td><td>{@code magic-white} - white from nose to tail</td></tr>
 * </table>
 *
 * <h2>A backdrop, not a verdict</h2>
 * Painted absolutely in phase 3 over whatever the melanin genes made - so it is
 * white over a black horse, a cremello or a leopard alike, and the coat
 * underneath is still in the genome for a foal to inherit. It is painted
 * <b>early</b> in phase 3 ({@link #PRIORITY}), so every marking gene above it
 * still draws on top: a magic-white horse carrying anything else that paints
 * shows that marking on white. (Owner's call, inherited from the dhampir -
 * "so that you aren't stuck with a pure white horse".) Below it sit only
 * {@code suit} and {@code hood}, which are lower still for the same reason.
 *
 * <p>It does nothing else. The eyes, the sunburn, the diet and the strength a
 * dhampir has are four other loci now - see {@code wiki/breed-book.html} for
 * the breed that puts them together.
 *
 * <p>Magical, so it paints in phase 3 and never touches the pigment field.
 */
public final class MagicWhiteGene implements Gene {

    public static final String KEY = "horsegenetics.magic_white";

    /**
     * <b>Near the bottom of the magical band</b>, where the dhampir white sat and
     * for the same reason: wherever a whole-horse paint sorts is where every
     * marking above it stops being visible. See the class note.
     */
    public static final int PRIORITY = 105;

    /** How many founders in a hundred carry one silent copy. None carry two. */
    public static final double CARRIER_PERCENT = 3.0;

    public final Allele Wm = new Allele(KEY, 0, "Wm", "Magic white (Wm)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Wm, n);

    private final Expression WILD = Expression.wildType(
            "An ordinary coat - whatever the rest of the genome made it.");

    private final Expression CARRIER = Expression.wildType("magic-white-carrier", "Magic white carrier",
            "One copy, which shows nothing at all. Two carriers bred together is the only way a "
                    + "magic white horse appears.");

    private final Expression WHITE = Expression.of("magic-white", "Magic white")
            .describe("White from nose to tail, painted over whatever coat the horse really has - "
                    + "which is still in its genes for a foal to inherit. It is painted low in the "
                    + "magical order, so any marking the horse carries still draws on top of it.")
            .tint((ctx, coat, colour) -> {
                ColorField delta = ColorField.deltaLike(colour);
                // Absolute, not additive: phase 3's accumulator would otherwise
                // let a dark coat show through from underneath.
                HorseSkinGeometry.forEachTexel(ctx.skin(),
                        (px, py, part, face, point) -> delta.set(px, py, 255, 255, 255, 255));
                return delta;
            });

    private final List<Expression> expressions = List.of(WILD, CARRIER, WHITE);

    /**
     * Carriers only. The same "founders never carry two" rule the old dhampir
     * locus used, for the same reason: a white horse is loud, and one caught by
     * accident would retire the reason to breed for it.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(Wm, n, CARRIER_PERCENT)
            .weight(n, n, 100.0 - CARRIER_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic white"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int copies = pair.count(Wm);
        if (copies == 2) {
            return WHITE;
        }
        return copies == 1 ? CARRIER : WILD;
    }

    /** Is this horse painted white? */
    public boolean isWhite(AllelePair pair) {
        return pair != null && pair.count(Wm) == 2;
    }
}
