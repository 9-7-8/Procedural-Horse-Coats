package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Rainbow dust</b> ({@code horsegenetics.rainbow_dust}) - a <b>magical
 * recessive</b>, and the smallest gene in the mod that anybody will actually
 * want. Two copies and the horse kicks up coloured dust from its hooves as it
 * walks, and the colour never settles: it works its way round the whole hue
 * circle and starts again.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Rbw/n}</td><td>a wild type - nothing shows at all</td></tr>
 *   <tr><td>{@code Rbw/Rbw}</td><td>{@code rainbow-dust} - the trail</td></tr>
 * </table>
 *
 * <h2>Why it is not a forty-first {@link ParticleGene} allele</h2>
 * It is the obvious question, and the answer is that the particle locus's whole
 * premise is that a horse can carry <b>two</b> of it and no more. Every allele
 * there competes for one of two slots, which is what makes choosing between
 * them a game. This one does not compete with anything - a horse can trail
 * flames <i>and</i> rainbow dust - so filing it there would have quietly cost a
 * flame horse its flames, and filing it here costs nothing but a locus.
 *
 * <p>It is also a different <i>kind</i> of thing. Every particle allele names a
 * particle and lets the epigenetics pick the colour; this one names the colour
 * and lets the clock pick it, which is a behaviour rather than a variant.
 *
 * <h2>Recessive, and no wild carriers</h2>
 * One copy shows nothing, exactly as at the particle locus, and for the same
 * reason: a trail is a thing you breed for. The founder table follows the same
 * rule too - a wild horse is either plain or the whole rainbow, never a carrier
 * - so what you catch is what you watched it do, and the carriers appear one
 * generation later in the foals of a rainbow horse bred to an ordinary one.
 *
 * <h2>The one thing that varies</h2>
 * How <b>fast</b> the rainbow turns, in ticks per lap, drawn on the allele copy
 * and inherited with it. A horse near {@link #CYCLE_FAST_TICKS} strobes; one
 * near {@link #CYCLE_SLOW_TICKS} looks like a solid colour that has changed by
 * the time you next look at it. It is one number rather than a colour because
 * the colour is the point of the gene and is not the gene's to vary - but two
 * rainbow horses side by side should still not be the same horse.
 *
 * <p>It paints nothing, so {@link #affectsCoat()} is false and the locus stays
 * out of the texture key - the same trick {@link ParticleGene}, milk and verdant
 * use.
 */
public final class RainbowDustGene implements Gene, EpigeneticAbilityContribution {

    public static final String KEY = "horsegenetics.rainbow_dust";

    /** Immediately after the particle locus (150), which is the gene it is most often confused with. */
    public static final int PRIORITY = 151;

    /**
     * How many founders in a hundred are born {@code Rbw/Rbw} - the whole wild
     * share of the gene, because no founder carries a single copy. About one
     * horse in four hundred, so it is rarer than any coat gene and less rare
     * than any <i>named</i> particle.
     */
    public static final double WILD_HOMOZYGOUS_PERCENT = 0.25;

    /** The particle. The one vanilla particle that fades between two colours, which is the whole effect. */
    public static final String PARTICLE = "minecraft:dust_color_transition";

    /** Ticks for one lap of the hue circle at the fast end of the epigenetic range. */
    public static final int CYCLE_FAST_TICKS = 40;
    /** ...and at the slow end. Three seconds against fifteen is a visible difference between two horses. */
    public static final int CYCLE_SLOW_TICKS = 300;

    /** Particles per firing. Fixed - the density is not what this gene is about. */
    public static final int COUNT = 2;

    /** Probability the trail fires on any given moving tick. Denser than the particle locus: it is dust. */
    public static final double EMIT_CHANCE = 0.5;

    public final Allele Rbw = new Allele(KEY, 0, "Rbw", "Rainbow dust (Rbw)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Rbw, n);

    private final Expression WILD = Expression.wildType(
            "An ordinary horse. The ground it walks over stays the colour it was.");

    /**
     * The carrier is a wild type and not a {@link Expression.Builder#marker()}:
     * it changes nothing about the horse, so two carriers must be free to share
     * a baked texture with a plain horse.
     */
    private final Expression CARRIER = Expression.wildType("rainbow-dust-carrier", "Rainbow dust carrier",
            "One copy, which shows nothing and does nothing. Two of these bred together are the "
                    + "only way rainbow dust appears.");

    private final Expression RAINBOW = Expression.wildType("rainbow-dust", "Rainbow dust",
            "Coloured dust comes off all four hooves as the horse walks, fading from one colour "
                    + "into the next and working its way through the whole rainbow before it "
                    + "starts again. How fast it turns is written on the allele copy, so no two "
                    + "rainbow horses are on the same colour at the same moment.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, RAINBOW);

    /**
     * Plain horses and rainbow horses, and nothing in between - the rule the
     * particle locus's table is built on. See
     * {@code ParticleGene.foundersTable()} for the argument: a recessive whose
     * carrier is invisible needs its <i>expressing</i> combination in the wild,
     * or the allele is only ever found by accident.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(Rbw, WILD_HOMOZYGOUS_PERCENT)
            .weight(n, 100.0 - WILD_HOMOZYGOUS_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Rainbow dust"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Rbw)) {
            case 2 -> RAINBOW;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    /** Does this horse trail the rainbow? Two copies, or nothing. */
    public boolean isRainbow(AllelePair pair) {
        return pair != null && pair.count(Rbw) == 2;
    }

    /**
     * How fast this horse's rainbow turns. A {@code SCALAR} rather than a
     * category, so drift moves it by a hair each generation and a line really
     * can be bred faster or slower - which is the only thing about this gene
     * there is to breed.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform("cycle", CYCLE_FAST_TICKS, CYCLE_SLOW_TICKS));
    }

    /**
     * One emitter, off the hooves, with {@code cycleTicks} set - which is what
     * tells the translator to ignore the two colours below and read the hue off
     * the clock instead. They are still filled in, because an emitter always
     * fills all of its colour fields and lets the particle decide; a white
     * fallback is what a build that somehow lost the cycle would draw.
     */
    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype, GeneEpigenetics epigenetics) {
        if (!isRainbow(pair)) {
            return List.of();
        }
        EpiValues epi = epigenetics.expressed();
        int cycle = (int) Math.round(epi.get("cycle"));
        return List.of(new GeneAbility.Emitter("particle", "trail", "hooves",
                new GeneAbility.Trigger.OnMove(), 0xFFFFFF, 0xFFFFFF, COUNT, 0.0,
                PARTICLE, EMIT_CHANCE, cycle, GeneAbility.Condition.ALWAYS, 1));
    }
}
