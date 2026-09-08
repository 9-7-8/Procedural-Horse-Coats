package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.WhitePattern;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyeColorContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.common.trait.TraitBuilder;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.EyeSpread;

import java.util.List;

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
 *   <tr><td>{@code SW1}</td><td><b>usually subtle</b>; occasionally a full splash</td><td><b>viable</b>, and reliably whiter</td></tr>
 *   <tr><td>{@code SW3}</td><td>usually a more obvious splash</td><td><b>nonviable</b> (unconfirmed, likely embryonic lethal)</td></tr>
 *   <tr><td>{@code SW5}</td><td>splash-type, variable</td><td>viability not established - <b>allowed</b></td></tr>
 * </table>
 *
 * <p>{@code SW1} is the best-established viable splash homozygote in the whole
 * family and the one place a real dose effect is documented, so it is the
 * allele whose two copies get their own outcome.
 *
 * <h2>{@code SW1} is the common one, and it is usually invisible</h2>
 * {@code SW1} is a several-hundred-year-old regulatory change that predates
 * most modern breeds, and it is far and away the most widespread splash allele
 * - the others are rare and mostly trace to one family or one stallion. It is
 * also the <b>most variably expressed</b>: a single copy is typically a snip
 * and a sock, occasionally a blue-eyed splash, and the paper that identified it
 * says outright that a minimally expressed splash cannot be told from common
 * white markings by eye.
 *
 * <p>So {@code SW1} carries the mod's population of ordinary-looking marked
 * horses: {@value #WILD_SW1_PERCENT}% of founders have one copy, and one copy
 * is {@link #MINIMAL}. <b>That is a stand-in, and worth being honest about.</b>
 * In life most stars and socks are <i>not</i> splash - they are polygenic, and
 * the same study found no splash allele at all in 112 deliberately
 * minimally-marked horses. The mod has no polygenic markings system, so the one
 * allele that genuinely does hide in plain sight is doing that job as well as
 * its own. The honest fix is a markings system; see {@code wiki/roadmap.html}.
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
public final class MitfGene implements Gene, HealthContribution, EyeColorContribution {

    public static final String KEY = "horsegenetics.mitf";

    /**
     * {@code N/SW1} is a <b>range</b>, not a value. See {@link #MINIMAL} - the
     * bottom of it is a snip and one sock, the top a classic splash, and the
     * horse's own roll decides. Every other outcome is a single number.
     */
    private static final double S_MINIMAL_LOW = 0.10;
    private static final double S_MINIMAL_HIGH = 0.44;

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

    /**
     * <b>The allele that hides in plain sight.</b> A single {@code SW1} copy is
     * the most variably expressed thing in the white loci: it can be a snip and
     * one white foot, ordinary-looking face white beside a single blue eye, or a
     * full blue-eyed splash - on the same genotype, in the same family. The
     * original {@code MITF} / {@code PAX3} paper says so directly: in minimally
     * expressed horses splashed white cannot be told from common white markings
     * by eye.
     *
     * <p>So this outcome is painted over a <i>range</i> rather than at a
     * strength, and the name describes the usual case rather than the whole of
     * it. It is why {@code SW1} is the allele worth testing for when a horse
     * has ordinary-looking markings, and why phenotype is a poor guide to it.
     */
    private final Expression MINIMAL = Expression.of("splash-minimal", "Minimal splash white")
            .describe("Usually no more than a star or a snip and a clean-edged sock or two - "
                    + "markings you would not look at twice - and sometimes a blue eye beside "
                    + "them. But the same single copy occasionally draws a broad blaze, high "
                    + "white and a belly, so two horses with this genotype need not look "
                    + "remotely alike.")
            .varies()
            .restrict((ctx, coat) ->
                    WhitePattern.splash(ctx, coat, KEY, S_MINIMAL_LOW, S_MINIMAL_HIGH));

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

    private final List<Expression> expressions = List.of(WILD, MINIMAL, SPLASH, BOLD, EXTENSIVE);

    /**
     * How many founders carry <b>one</b> copy of {@code SW1}. It is the oldest
     * and by a long way the most widespread splash allele - several hundred
     * years old, found across a dozen-odd modern breeds - and because
     * {@link #MINIMAL one copy is usually subtle}, a horse carrying it looks
     * like an ordinary horse with a star and a sock. So most horses have it,
     * and most horses wearing it are not what anyone would call a splash.
     */
    public static final double WILD_SW1_PERCENT = 55.0;
    /** Rare, family-limited, and loud when it turns up. */
    public static final double WILD_SW3_PERCENT = 0.4;
    /** {@code SW5} stands for the rare {@code MITF} deletions as a group. */
    public static final double WILD_SW5_PERCENT = 0.6;

    /**
     * <b>Heterozygotes only, and written out rather than derived</b>, for the
     * two reasons {@link Pax3Gene} sets out at length: no allele frequency
     * makes Hardy-Weinberg produce a majority-heterozygous population (its
     * {@code 2pq} peaks at 50%), and the doubled combinations are the reward
     * for breeding rather than something a wild-caught horse should arrive
     * wearing. At 55% carriage, leaving the homozygote to Hardy-Weinberg would
     * have made bold splash the commonest coat in the game.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(SW1, N, WILD_SW1_PERCENT)
            .weight(SW3, N, WILD_SW3_PERCENT)
            .weight(SW5, N, WILD_SW5_PERCENT)
            .weight(N, N, 100.0 - WILD_SW1_PERCENT - WILD_SW3_PERCENT - WILD_SW5_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "MITF (splash white)"; }
    @Override public int priority() { return 78; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * Ten combinations, five outcomes. {@code SW3} is the strong one, so any
     * second variant beside it tips the horse into the extensive outcome;
     * {@code SW1} is the one with a documented viable dose effect, so its
     * homozygote is a step up rather than more of the same - and it is a
     * <i>two</i>-step up, because one copy of it is the minimal outcome.
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
            return (pair.homozygousFor(SW1) || pair.has(SW5)) ? BOLD : MINIMAL;
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

    /**
     * <b>Splash is the blue-eyed pattern.</b> Diagnostic even when the white is
     * modest - a horse with one blue eye and white to the knee is how a splash
     * carrier is spotted in the field - so any expressing combination qualifies,
     * not only the bold ones. See {@link WhitePatternEyes}.
     */
    @Override
    public java.util.Optional<EyeColor> eyeColor(AllelePair pair, Genotype genotype,
            com.example.horsegenetics.common.genetics.Epigenome epigenome, double whiteCoverage) {
        return WhitePatternEyes.blueIf(isSplash(pair), whiteCoverage);
    }

    /**
     * The splash shape - the waterline field, where in its range this horse lands,
     * and the shared face marking - plus the eye spread, which splash claims more
     * often than any other locus.
     */
    @Override
    public EpiSchema epiSchema() {
        return WhitePattern.splashSchema().and(EyeSpread.schema().values().toArray(new EpiValue[0]));
    }

}
