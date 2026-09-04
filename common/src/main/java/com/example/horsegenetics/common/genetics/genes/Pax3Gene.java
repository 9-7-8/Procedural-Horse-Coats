package com.example.horsegenetics.common.genetics.genes;

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
 * <b>{@code PAX3}</b> ({@code horsegenetics.pax3}) - the <b>second</b> splash
 * white locus, carrying {@code SW2} and {@code SW4}. Its twin is
 * {@link MitfGene}.
 *
 * <p>This gene exists to make one point the old single splash gene could not:
 * <b>a horse can be splash twice over</b>. {@code SW1} sits on {@code MITF} and
 * {@code SW2} on {@code PAX3}, they are different chromosomes' worth of
 * different genes, and a horse carrying one copy of each has more face, leg and
 * belly white than a horse carrying either alone. In this model that falls out
 * for free - both genes paint, one after the other, and white on white is
 * whiter - with no interaction rule anywhere.
 *
 * <table>
 *   <tr><th>allele</th><th>one copy</th><th>two copies</th></tr>
 *   <tr><td>{@code N}</td><td>-</td><td>wild type</td></tr>
 *   <tr><td>{@code SW2}</td><td>variable splash, often bold face and leg white</td><td><b>viable</b>; reported, and associated with deafness</td></tr>
 *   <tr><td>{@code SW4}</td><td>splash-type face, leg and belly white</td><td><b>never detected</b> - not modelled as occurring</td></tr>
 * </table>
 *
 * <p><b>Deafness is not modelled.</b> {@code SW2/SW2} carries a real hearing
 * risk in live horses; the mod has no hearing, and inventing a penalty the
 * player cannot perceive would be flavour text pretending to be a mechanic. It
 * is recorded in the outcome's description instead, which is where the gene
 * dictionary will read it.
 *
 * <p>Natural, <b>non-deterministic</b>, painted with the same
 * {@linkplain WhitePattern#splash dipped-from-below} shape as {@code MITF} -
 * the two loci produce the same pattern, which is exactly why they were
 * mistaken for one gene. See {@code wiki/gene-pax3.html}.
 */
public final class Pax3Gene implements Gene {

    public static final String KEY = "horsegenetics.pax3";

    private static final double S_SPLASH = 0.34;
    private static final double S_BOLD = 0.60;

    public final Allele SW2 = new Allele(KEY, 0, "SW2", "Splash white 2 (SW2)");
    public final Allele SW4 = new Allele(KEY, 1, "SW4", "Splash white 4 (SW4)");
    public final Allele N = new Allele(KEY, 2, "N", "Wild-type (N)");

    private final List<Allele> alleles = List.of(SW2, SW4, N);

    private final Expression WILD = Expression.wildType("No splash markings.");

    private final Expression SPLASH = Expression.of("splash", "Splash white")
            .describe("Splash-type white: leg white with a clean edge, white up the belly and a bold "
                    + "face marking. On its own it is hard to tell from the MITF kind - the two loci "
                    + "make the same pattern, which is why they were long taken for one gene.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.splash(ctx, coat, KEY, S_SPLASH));

    private final Expression BOLD = Expression.of("splash-bold", "Bold splash white")
            .describe("White carried well up the barrel and over the face. Two copies of SW2 are "
                    + "viable and land here; in live horses that genotype also carries a real risk "
                    + "of deafness, which this mod does not model.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.splash(ctx, coat, KEY, S_BOLD));

    private final List<Expression> expressions = List.of(WILD, SPLASH, BOLD);

    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), this::canOccur);

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(SW2, 0.020);
        p.put(SW4, 0.005);
        p.put(N, 0.975);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "PAX3 (splash white)"; }
    @Override public int priority() { return 79; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(N)) {
            return WILD;
        }
        // Two variant copies - the same one twice or one of each - is the bold
        // outcome; one variant beside N is the ordinary splash.
        return pair.has(N) ? SPLASH : BOLD;
    }

    /** {@code SW4/SW4} has never been detected in a live horse. */
    @Override
    public boolean canOccur(AllelePair pair) {
        return !pair.homozygousFor(SW4);
    }

    /** Does this combination draw splash markings at all? */
    public boolean isSplash(AllelePair pair) {
        return !pair.homozygousFor(N);
    }
}
