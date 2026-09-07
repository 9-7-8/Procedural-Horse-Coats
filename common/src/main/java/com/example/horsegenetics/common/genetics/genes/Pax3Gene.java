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
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.common.trait.TraitBuilder;

import java.util.List;

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
 * <p><b>Deafness is reported.</b> Two variant copies at this locus and the
 * horse is deaf - the same {@link MitfGene#DEAFNESS} condition its twin emits,
 * de-duplicated so a horse doubled at both loci is told once. It is
 * informational and costs the horse nothing: the mod has no hearing to lose,
 * and inventing a penalty the player cannot perceive would be flavour text
 * pretending to be a mechanic.
 *
 * <h2>{@code SW2} is the <i>rare</i> one - see {@link MitfGene}</h2>
 * This locus used to carry the mod's near-ubiquitous minimal splash allele, on
 * the reasoning that a mild splash allele is what gives an ordinary horse its
 * socks and its blaze. The reasoning was right and the allele was wrong.
 * {@code SW1}, on {@code MITF}, is the widespread, several-hundred-year-old,
 * minimally-expressed one; {@code SW2} is a coding change in {@code PAX3} with
 * a much narrower distribution - Quarter Horses and American Paints above all,
 * with reports in Lipizzaners and Norikers. So the common-allele job moved to
 * {@code MITF}, and {@code SW2} is now rare in the wild pool and concentrated
 * in the breeds that actually have it.
 *
 * <p>What the split still buys is the thing it was built for: because both
 * splash loci read the coat they are handed
 * ({@linkplain WhitePattern#splash white finds white}), a horse carrying
 * {@code SW1} <i>and</i> {@code SW2} is whiter than either alone, with no
 * interaction rule anywhere. In a Quarter Horse study that is exactly what was
 * measured - horses with both had more facial white on average than horses with
 * either.
 *
 * <p>Natural, <b>non-deterministic</b>, painted with the same
 * {@linkplain WhitePattern#splash dipped-from-below} shape as {@code MITF} -
 * the two loci produce the same pattern, which is exactly why they were
 * mistaken for one gene. See {@code wiki/gene-pax3.html}.
 */
public final class Pax3Gene implements Gene, HealthContribution, EyeColorContribution {

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
                    + "viable and land here - and, like any doubled splash, deaf: the pigment "
                    + "cells that never reached the coat never reached the inner ear either.")
            .varies()
            .restrict((ctx, coat) -> WhitePattern.splash(ctx, coat, KEY, S_BOLD));

    private final List<Expression> expressions = List.of(WILD, SPLASH, BOLD);

    /**
     * How many founders carry <b>one</b> copy of {@code SW2}. Rare in the wild
     * pool: this allele's distribution is a handful of breeds, not a species,
     * and the breeds that have it say so in their own tables.
     */
    public static final double WILD_SW2_PERCENT = 3.0;
    /** {@code SW4} is rarer still - one Appaloosa family is the whole of it. */
    public static final double WILD_SW4_PERCENT = 0.5;

    /**
     * <b>Heterozygotes only, and written out rather than derived.</b> Two
     * separate reasons, and both of them rule out
     * {@link FounderTable#hardyWeinberg}:
     * <ul>
     *   <li><b>The doubled combinations are the reward for breeding.</b>
     *       {@code SW2/SW2} is the bold outcome and it is deaf; it must not turn
     *       up in a wild-caught horse, the same rule the health loci and
     *       {@link MagicSizeGene} follow.</li>
     *   <li><b>Hardy-Weinberg cannot express a carriage rate directly.</b> Its
     *       heterozygote share is {@code 2pq}, which peaks at 50%, so any table
     *       stating "this many founders carry one copy" has to state it rather
     *       than derive it. That mattered more when this locus carried the
     *       common allele - {@link MitfGene} carries it now, and uses the same
     *       shape of table for the same reason.</li>
     * </ul>
     * Baseline last, as every table in the mod does it, so a high founder roll
     * is the plain horse.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(SW2, N, WILD_SW2_PERCENT)
            .weight(SW4, N, WILD_SW4_PERCENT)
            .weight(N, N, 100.0 - WILD_SW2_PERCENT - WILD_SW4_PERCENT)
            .build();

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

    /**
     * Two variant copies at this locus and the horse is deaf. One copy is a
     * pattern and nothing else.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (pair.count(N) == 0) {
            out.condition(MitfGene.DEAFNESS);
        }
    }

    /** The second splash locus, and the same rule - see {@link MitfGene#eyeColor}. */
    @Override
    public java.util.Optional<EyeColor> eyeColor(AllelePair pair, Genotype genotype,
            com.example.horsegenetics.common.genetics.Epigenome epigenome, double whiteCoverage) {
        return WhitePatternEyes.blueIf(!expressionOf(pair).wildType(), whiteCoverage);
    }

}
