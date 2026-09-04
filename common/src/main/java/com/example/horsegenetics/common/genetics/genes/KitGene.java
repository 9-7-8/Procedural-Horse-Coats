package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.WhitePattern;
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
 * <b>{@code KIT}</b> ({@code horsegenetics.kit}) - the white-patterning
 * neighbourhood on equine chromosome 3, and the mod's <b>eight-allele</b> locus.
 * It replaces the old {@code horsegenetics.white} (dominant white) and
 * {@code horsegenetics.sabino} ({@code SB1}) genes, which modelled two alleles
 * of <i>one</i> gene as two independent genes and so let a horse be
 * homozygous-sabino and dominant-white at the same time - a genotype that
 * cannot exist, because a horse has two copies of chromosome 3 and no more.
 *
 * <h2>What is and is not at this locus</h2>
 * {@code SB1} and the numbered {@code W} series are all <b>defined {@code KIT}
 * variants</b>, so they are alleles of this one gene. Two patterns that are
 * often filed next to them are <b>not</b>:
 * <ul>
 *   <li><b>Tobiano</b> is a large inversion <i>near / downstream of</i>
 *       {@code KIT}, not a {@code KIT} variant - {@link TobianoGene}.</li>
 *   <li><b>Roan</b> maps to the region but its causal change is unresolved and
 *       breed-dependent - {@link RoanGene}.</li>
 * </ul>
 * Both can therefore co-occur with anything here, which is exactly what real
 * horses do: a tobiano can also carry {@code W20}.
 *
 * <h2>The alleles</h2>
 * Eight of the thirty-plus named ones, chosen to span the real range rather
 * than to enumerate a testing panel - the {@code W} number is a discovery
 * order, <b>not</b> a severity ranking, so nothing here can be inferred from
 * the numbers:
 * <table>
 *   <tr><th>allele</th><th>one copy</th><th>two copies</th></tr>
 *   <tr><td>{@code N}</td><td>-</td><td>wild type</td></tr>
 *   <tr><td>{@code W20}</td><td>subtle: ordinary face / leg white</td><td><b>viable</b>, modestly more</td></tr>
 *   <tr><td>{@code SB1}</td><td>classic sabino-1</td><td><b>viable</b>, sabino-white (90%+)</td></tr>
 *   <tr><td>{@code W23}</td><td>modest to broad spotting</td><td>viability unknown - <b>allowed</b></td></tr>
 *   <tr><td>{@code W5}</td><td>sabino-like, broad</td><td><b>nonviable</b></td></tr>
 *   <tr><td>{@code W10}</td><td>sabino-like, broad</td><td><b>nonviable</b></td></tr>
 *   <tr><td>{@code W13}</td><td>extensive white spotting</td><td><b>nonviable</b></td></tr>
 *   <tr><td>{@code W22}</td><td>dominant white - all white, <b>masks</b></td><td><b>nonviable</b></td></tr>
 * </table>
 *
 * <h2>Viability, and what {@code canOccur} means here</h2>
 * The four {@code W} alleles UC Davis lists as "homozygosity thought nonviable"
 * get {@link #canOccur} {@code false}: those are <i>embryonic</i> lethals, so
 * there is no such horse to give a pen to and none in the founder population.
 * That is the same statement the sex locus makes about {@code Y/Y}.
 *
 * <p><b>Compound heterozygotes are allowed</b> - {@code W5/W13}, {@code W22/W5}
 * and the rest. The evidence is about an allele paired <i>with itself</i>; the
 * breeding advice is to avoid crossing carriers of <i>the same exact</i>
 * variant, and horses carrying two different strong {@code W}s are recorded. So
 * the rule is one allele twice, not two strong alleles.
 *
 * <p>{@code W23}'s homozygote has <b>no evidence either way</b>. The model has
 * to choose, and choosing "allowed" is the choice that does not invent a lethal
 * the source does not claim; it lands on the same near-white outcome as
 * sabino-white.
 *
 * <h2>How one copy and two relate</h2>
 * There is no dose arithmetic here and deliberately so. Some of these alleles
 * have a real viable dose series ({@code SB1}), one is a <b>booster</b> that is
 * subtle alone and adds white beside another variant ({@code W20}), and most
 * are "dominant with variable expression" - one copy is already effective and
 * the phenotype is not neatly intermediate. Only a combination table can say
 * all three at once, so {@link #expressionOf} is a table.
 *
 * <p>Every painted outcome is the same {@link WhitePattern#sabino} shape at a
 * different strength - ragged margins growing inward from legs, belly and face -
 * because that is how the source describes the whole series: "sabino-like
 * through nearly all-white". Only {@code W22}'s dominant white is a different
 * painter, because all-white is not a lot of sabino, it is the absence of
 * pigment cells altogether.
 *
 * <p>Natural. Every outcome but the wild type and dominant white is
 * <b>non-deterministic</b>. See {@code wiki/gene-kit.html}.
 */
public final class KitGene implements Gene {

    public static final String KEY = "horsegenetics.kit";

    /**
     * Where each outcome sits on {@link WhitePattern#sabino}'s ramp. Tuned so
     * the ladder reads as distinct steps in-game rather than as a smooth blur:
     * a {@code W20} horse should look like an ordinary horse with a star, and a
     * sabino-white should be unmistakably not just a bold sabino.
     */
    private static final double S_MINIMAL = 0.12;
    private static final double S_MODEST = 0.24;
    private static final double S_SABINO = 0.42;
    private static final double S_BROAD = 0.58;
    private static final double S_EXTENSIVE = 0.74;
    private static final double S_NEAR_WHITE = 0.93;

    // Declaration order is AllelePair's canonical slot order and nothing else -
    // it is not a dominance ranking. Strongest first reads best in a code string.
    public final Allele W22 = new Allele(KEY, 0, "W22", "Dominant white (W22)");
    public final Allele W13 = new Allele(KEY, 1, "W13", "White spotting (W13)");
    public final Allele W10 = new Allele(KEY, 2, "W10", "White spotting (W10)");
    public final Allele W5 = new Allele(KEY, 3, "W5", "White spotting (W5)");
    public final Allele W23 = new Allele(KEY, 4, "W23", "White spotting (W23)");
    public final Allele SB1 = new Allele(KEY, 5, "SB1", "Sabino 1 (SB1)");
    public final Allele W20 = new Allele(KEY, 6, "W20", "White booster (W20)");
    public final Allele N = new Allele(KEY, 7, "N", "Wild-type (N)");

    private final List<Allele> alleles = List.of(W22, W13, W10, W5, W23, SB1, W20, N);

    /** The alleles whose homozygote UC Davis lists as thought nonviable. */
    private final List<Allele> lethalWhenDoubled = List.of(W22, W13, W10, W5);

    /**
     * The four alleles that already produce broad-to-extensive white on their
     * own; two of them together produce more, whichever two they are.
     */
    private final List<Allele> strong = List.of(W13, W10, W5, W23);

    private final Expression WILD = Expression.wildType("No congenital white markings.");

    private final Expression MINIMAL = Expression.of("minimal-white", "Minimal white")
            .describe("Ordinary-looking white: a star or snip and a low sock or two, the sort of "
                    + "marking nobody would call a pattern. This is what a single W20 usually does "
                    + "on its own - it is a booster, and it shows properly beside another variant.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_MINIMAL));

    private final Expression MODEST = Expression.of("modest-white", "Modest white")
            .describe("A little more than ordinary: a narrow blaze, socks climbing past the fetlock, "
                    + "maybe a fleck under the belly. Two copies of W20 are viable and do add white, "
                    + "but not reliably twice as much as one.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_MODEST));

    private final Expression SABINO = Expression.of("sabino", "Sabino")
            .describe("Classic sabino: tall jagged stockings, white up the belly, a broad blaze, and "
                    + "roaned ragged margins rather than the clean edge a splash marking leaves.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_SABINO));

    private final Expression BROAD = Expression.of("broad-white", "Broad white spotting")
            .describe("Sabino carried much further: irregular white well up the legs and flanks, a "
                    + "wide blaze or bald face, and the first real patches on the barrel.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_BROAD));

    private final Expression EXTENSIVE = Expression.of("extensive-white", "Extensive white")
            .describe("Most of the horse is white, with colour holding out over the topline, the "
                    + "crest and the quarters. The margins are still ragged - this is a very white "
                    + "spotted horse, not a white one.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_EXTENSIVE));

    private final Expression NEAR_WHITE = Expression.of("near-white", "Near-white")
            .describe("Ninety per cent white or more, with a few coloured flecks left on the ears and "
                    + "the crest. Sabino-white, and where the strong W alleles land when they meet "
                    + "each other.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.sabino(ctx, coat, KEY, S_NEAR_WHITE));

    private final Expression DOMINANT_WHITE = Expression.of("dominant-white", "Dominant white")
            .describe("Every pigment gone over the whole body and pink skin underneath, so the horse "
                    + "renders pure white and no other coat gene it carries can be seen. This is an "
                    + "absence of pigment cells, not a great deal of sabino.")
            .masking()
            .restrict((ctx, coat) -> {
                PigmentField f = coat.mutableCopy();
                CoatRegions.restrictAll(ctx.skin(), f, (field, px, py, p) -> {
                    field.setRed(px, py, 0f);
                    field.setBlack(px, py, 0f);
                });
                return f;
            });

    private final List<Expression> expressions =
            List.of(WILD, MINIMAL, MODEST, SABINO, BROAD, EXTENSIVE, NEAR_WHITE, DOMINANT_WHITE);

    /**
     * Founder allele frequencies. {@code W20} is genuinely common in some
     * breeds, {@code SB1} uncommon, and every strong {@code W} is rare -
     * these are all founder-effect alleles traced to individual horses. The
     * four lethal homozygotes are dropped and the rest rescaled, which is what
     * a real adult population <i>is</i>.
     */
    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), this::canOccur);

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(W22, 0.001);
        p.put(W13, 0.002);
        p.put(W10, 0.004);
        p.put(W5, 0.005);
        p.put(W23, 0.006);
        p.put(SB1, 0.022);
        p.put(W20, 0.060);
        p.put(N, 0.900);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "KIT (white spotting)"; }
    @Override public int priority() { return 76; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * <b>The table.</b> Thirty-six combinations, eight outcomes, written as a
     * cascade because that is how the source reads: strongest allele first,
     * then what the second copy adds.
     *
     * <p>It still answers for the four combinations {@link #canOccur} rules
     * out - parsing is tolerant, so a hand-written code can name one, and the
     * nearest sensible outcome beats throwing.
     */
    @Override
    public Expression expressionOf(AllelePair pair) {
        // W22 is the strong dominant white: one copy already removes essentially
        // all pigment, and nothing another allele does can be seen under it.
        if (pair.has(W22)) {
            return DOMINANT_WHITE;
        }
        // Two strong alleles - the same one twice, or two different ones - and
        // the horse is near-white however they got there.
        if (strong.contains(pair.first()) && strong.contains(pair.second())) {
            return NEAR_WHITE;
        }
        if (pair.has(W13)) {
            // W13 alone is already extensive; SB1 beside it tips it over.
            return pair.has(SB1) ? NEAR_WHITE : EXTENSIVE;
        }
        if (pair.has(W10) || pair.has(W5)) {
            if (pair.has(SB1)) {
                return NEAR_WHITE;
            }
            return pair.has(W20) ? EXTENSIVE : BROAD;
        }
        if (pair.has(W23)) {
            // Milder than W5 / W10 on its own, so SB1 lifts it one step, not two.
            return pair.has(SB1) ? EXTENSIVE : BROAD;
        }
        if (pair.has(SB1)) {
            // The clearest viable dose series in the whole locus, and the reason
            // W20 is worth having: SB1 + W20 is visibly more than SB1 alone.
            if (pair.homozygousFor(SB1)) {
                return NEAR_WHITE;
            }
            return pair.has(W20) ? BROAD : SABINO;
        }
        if (pair.has(W20)) {
            return pair.homozygousFor(W20) ? MODEST : MINIMAL;
        }
        return WILD;
    }

    /**
     * The four {@code W} alleles whose homozygote is thought nonviable have no
     * horse to describe. A <b>compound</b> heterozygote of two of them is a
     * different question and is allowed - see the class javadoc.
     */
    @Override
    public boolean canOccur(AllelePair pair) {
        return !(pair.homozygous() && lethalWhenDoubled.contains(pair.first()));
    }

    /** Does this combination remove every pigment everywhere - i.e. carry {@code W22}? */
    public boolean isDominantWhite(AllelePair pair) {
        return pair.has(W22);
    }
}
