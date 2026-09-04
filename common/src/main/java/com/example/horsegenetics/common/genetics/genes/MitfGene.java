package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.WhitePattern;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.common.trait.TraitBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>{@code MITF}</b> ({@code horsegenetics.mitf}) - the first of the two
 * <b>splash white</b> loci, carrying {@code SW1}, {@code SW3} and {@code SW5}.
 *
 * <h2>Splash is two genes, not one</h2>
 * This is the whole reason the old single {@code horsegenetics.splash} gene had
 * to go. Splash White is caused by variants in <b>two different genes</b> -
 * {@code MITF} here and {@code PAX3} in {@link Pax3Gene} - which means a horse
 * can carry {@code N/SW1} <i>and</i> {@code N/SW2} at once and be whiter than
 * either alone. A single splash gene cannot express that: it has two slots, and
 * two different loci need four. Modelling them as one gene also implied that
 * {@code SW1} and {@code SW2} compete for the same slot, which is simply false.
 *
 * <p>Two {@code MITF} variants <i>can</i> still meet - {@code SW1/SW3} is a
 * real compound heterozygote - and that is what this locus's own table says.
 *
 * <h2>The alleles</h2>
 * <table>
 *   <tr><th>allele</th><th>one copy</th><th>two copies</th></tr>
 *   <tr><td>{@code N}</td><td>-</td><td>wild type</td></tr>
 *   <tr><td>{@code SW1}</td><td>subtle to bold splash</td><td><b>viable</b>, and reliably whiter</td></tr>
 *   <tr><td>{@code SW3}</td><td>usually a more obvious splash</td><td><b>nonviable</b> (unconfirmed, likely embryonic lethal)</td></tr>
 *   <tr><td>{@code SW5}</td><td>splash-type, variable</td><td>viability not established - <b>allowed</b></td></tr>
 * </table>
 *
 * <p>{@code SW1} is the best-established viable splash homozygote in the whole
 * family and the one place a real dose effect is documented, so it is the
 * allele whose two copies get their own outcome.
 *
 * <p><b>{@code SW6}, {@code SW7} and {@code SW8} are deliberately folded into
 * {@code SW5}.</b> All four are {@code MITF} variants the source describes in
 * word-for-word identical terms - "splash-type variable white pattern; rare;
 * homozygous viability not established" - so four separate alleles would be
 * four indistinguishable rows in a table whose whole point is that each row
 * says something. {@code SW5} stands for the group; adding the others is one
 * line each the day the science separates them.
 *
 * <h2>What splash looks like</h2>
 * The horse dipped in white from below ({@link WhitePattern#splash}): high,
 * <b>sharply bounded</b> leg white, belly white, a broad blaze to a bald face.
 * The crisp margin is the diagnostic difference from {@code KIT}'s ragged
 * sabino edge, not a stylistic choice. <b>Blue eyes are not modelled</b> - see
 * {@link WhitePattern}.
 *
 * <p>Natural, <b>non-deterministic</b>. See {@code wiki/gene-mitf.html}.
 */
public final class MitfGene implements Gene, HealthContribution {

    public static final String KEY = "horsegenetics.mitf";

    private static final double S_SPLASH = 0.38;
    private static final double S_BOLD = 0.62;
    private static final double S_EXTENSIVE = 0.86;

    /**
     * <b>Congenital deafness.</b> The splash pattern comes from melanocytes
     * failing to reach the skin, and the same cells line the inner ear - so a
     * horse white enough at this locus is very often deaf. The mod has no
     * hearing for a horse to lose, so this costs it nothing: it is reported and
     * named and that is all. See {@link com.example.horsegenetics.common.trait.Severity#INFORMATIONAL}.
     *
     * <p>Shared between the two splash loci deliberately - it is one condition
     * with two causes, and a horse homozygous at both should be told it is deaf
     * once, not twice. {@link com.example.horsegenetics.common.trait.TraitBuilder}
     * de-duplicates on the condition id for exactly this case.
     */
    public static final Condition DEAFNESS = Condition.informational(
            "splash-deafness", "Congenital deafness",
            "Two splash copies at one locus. The pigment cells that never reached the coat "
                    + "never reached the inner ear either, and the horse is deaf.");

    public final Allele SW3 = new Allele(KEY, 0, "SW3", "Splash white 3 (SW3)");
    public final Allele SW1 = new Allele(KEY, 1, "SW1", "Splash white 1 (SW1)");
    public final Allele SW5 = new Allele(KEY, 2, "SW5", "Splash white 5 (SW5)");
    public final Allele N = new Allele(KEY, 3, "N", "Wild-type (N)");

    private final List<Allele> alleles = List.of(SW3, SW1, SW5, N);

    private final Expression WILD = Expression.wildType("No splash markings.");

    private final Expression SPLASH = Expression.of("splash", "Splash white")
            .describe("As if the horse had been dipped in white paint to just above the knee: high "
                    + "leg white with a clean, sharply bounded edge, white up the belly and a broad "
                    + "blaze. How much varies enormously between horses with the same genotype.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.splash(ctx, coat, KEY, S_SPLASH));

    private final Expression BOLD = Expression.of("splash-bold", "Bold splash white")
            .describe("The same pattern carried up the barrel: white well past the elbow and stifle, "
                    + "a bald face, and only the topline and quarters left coloured. Two copies of "
                    + "SW1 land here, and so does a single SW3.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.splash(ctx, coat, KEY, S_BOLD));

    private final Expression EXTENSIVE = Expression.of("splash-extensive", "Extensive splash white")
            .describe("Almost the whole horse below the topline is white, with colour surviving only "
                    + "along the spine and over the ears. This is where two different MITF variants "
                    + "meet.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.splash(ctx, coat, KEY, S_EXTENSIVE));

    private final List<Expression> expressions = List.of(WILD, SPLASH, BOLD, EXTENSIVE);

    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), this::canOccur);

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(SW3, 0.004);
        p.put(SW1, 0.040);
        p.put(SW5, 0.006);
        p.put(N, 0.950);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "MITF (splash white)"; }
    @Override public int priority() { return 78; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * Ten combinations, four outcomes. {@code SW3} is the strong one, so any
     * second variant beside it tips the horse into the extensive outcome;
     * {@code SW1} is the one with a documented viable dose effect, so its
     * homozygote is a step up rather than more of the same.
     */
    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.has(SW3)) {
            if (pair.homozygousFor(SW3)) {
                return EXTENSIVE;    // cannot occur; answered anyway, parsing is tolerant
            }
            return (pair.has(SW1) || pair.has(SW5)) ? EXTENSIVE : BOLD;
        }
        if (pair.has(SW1)) {
            return (pair.homozygousFor(SW1) || pair.has(SW5)) ? BOLD : SPLASH;
        }
        if (pair.has(SW5)) {
            return pair.homozygousFor(SW5) ? BOLD : SPLASH;
        }
        return WILD;
    }

    /**
     * {@code SW3/SW3} has never been confirmed and is thought to be an
     * embryonic lethal, so there is no such horse. {@code SW5}'s homozygote is
     * merely unestablished, which is not the same claim - the model allows it
     * rather than inventing a lethal the source does not state.
     */
    @Override
    public boolean canOccur(AllelePair pair) {
        return !pair.homozygousFor(SW3);
    }

    /** Does this combination draw splash markings at all? */
    public boolean isSplash(AllelePair pair) {
        return !expressionOf(pair).wildType();
    }

    /**
     * Two variant copies at this locus and the horse is deaf. One copy is a
     * pattern and nothing else.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (pair.count(N) == 0) {
            out.condition(DEAFNESS);
        }
    }
}
