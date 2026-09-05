package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Healer</b> ({@code horsegenetics.healer}) - a <b>magical</b> gene. A horse
 * with two copies mends the people standing next to it, and carries a red stripe
 * down the middle of its mane that says so.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Hlr/n}</td><td>{@code healer-carrier} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code Hlr/Hlr}</td><td>{@code healer} - the stripe, and the aura</td></tr>
 * </table>
 *
 * <p>Recessive, and about one wild horse in {@value #WILD_HEALER_ONE_IN_HORSES}
 * is born with it - so a healer is something you find by breeding rather than by
 * catching, and the sixth of the population carrying one copy is invisible.
 *
 * <h2>The mark and the effect are the same fact</h2>
 * The stripe is not decoration. It is the only way to tell a healer from an
 * ordinary horse without standing next to it and waiting, and it is drawn
 * <i>because</i> the gene does something - the mod's rule that a magical ability
 * a player cannot see coming is a magical ability a player never learns to
 * breed for.
 *
 * <p>The stripe's <b>opacity</b> is this gene's one epigenetic value, drawn off
 * the expressing copy: a faint line to a vivid one, inherited with the allele.
 * It says nothing about the strength of the healing - the aura is the same on
 * every healer. That is deliberate: a mark that quietly encoded a stat would
 * make a horse's value legible from a screenshot, and this model would rather
 * make you keep a pedigree.
 *
 * <p><b>The stripe needs a mane</b>, so a foal shows nothing - but a foal
 * <i>heals</i>, because the aura is a property of the genotype and not of the
 * mane. That is the same split light makes with a foal's missing mane.
 */
public final class HealerGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.healer";
    public static final int PRIORITY = 116;

    public static final double WILD_HLR_FREQUENCY = 0.09;
    /** Both copies, so the square of the per-allele odds - for the Javadoc above. */
    public static final int WILD_HEALER_ONE_IN_HORSES = (int) Math.round(1.0 / (WILD_HLR_FREQUENCY * WILD_HLR_FREQUENCY));

    /** The red the stripe is walked toward. */
    public static final int RED = 0xE01B24;

    /** Half the stripe's width, as a fraction of the mane's second-longest span. */
    private static final double HALF_WIDTH = 0.22;

    /** The opacity range the epigenetic roll spans - never invisible, never flat paint. */
    private static final double OPACITY_MIN = 0.35;
    private static final double OPACITY_RANGE = 0.55;

    /** How far the aura reaches, in blocks. */
    public static final double HEAL_RADIUS = 3.0;
    /** Health points restored per beat - two per heart. */
    public static final double HEAL_AMOUNT = 1.0;
    /** Ticks between beats. */
    public static final int HEAL_INTERVAL_TICKS = 40;
    /** Most players one beat may reach. Small: the aura is for a stable, not a battlefield. */
    public static final int HEAL_MAX_TARGETS = 8;

    public final Allele Hlr = new Allele(KEY, 0, "Hlr", "Healer (Hlr)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Hlr, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression CARRIER = Expression.wildType("healer-carrier", "Healer carrier",
            "One copy, which shows nothing and does nothing. Two carriers bred together are the "
                    + "only way a healer appears.");

    private final Expression HEALER = Expression.of("healer", "Healer")
            .describe("A red stripe runs down the centre of the mane, faint on some horses and "
                    + "vivid on others, and everyone standing within " + (int) HEAL_RADIUS
                    + " blocks of the horse steadily mends.")
            .varies()
            .tint(HealerGene::paintStripe);

    private final List<Expression> expressions = List.of(WILD, CARRIER, HEALER);

    private final FounderTable founders = FounderTable.hardyWeinberg(Hlr, n, WILD_HLR_FREQUENCY);

    private final List<GeneAbility> aura = List.of(new GeneAbility.Healing(
            "players", HEAL_RADIUS, HEAL_AMOUNT, HEAL_INTERVAL_TICKS, HEAL_MAX_TARGETS,
            GeneAbility.Condition.ALWAYS, 1));

    @Override public String key() { return KEY; }
    @Override public String name() { return "Healer"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Hlr)) {
            case 2 -> HEALER;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    public boolean isHealer(AllelePair pair) {
        return pair.count(Hlr) == 2;
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        return isHealer(pair) ? aura : List.of();
    }

    private static ColorField paintStripe(CoatBuildContext ctx, PigmentView coat, ColorView accumulated) {
        Rng epi = ctx.epigeneticsFor(KEY);
        double opacity = OPACITY_MIN + epi.nextFloat() * OPACITY_RANGE;

        ColorField delta = ColorField.deltaLike(accumulated);
        HairPattern.paint(ctx, accumulated, delta, Part.MANE, (px, py, point) -> {
            double c = HairPattern.centreStripe(ctx.skin(), Part.MANE, point, HALF_WIDTH);
            return c <= 0 ? null : new HairPattern.Tone(RED, opacity * c);
        });
        return delta;
    }
}
