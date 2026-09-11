package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Potion milk</b> ({@code horsegenetics.potion_milk}) - a mare who can be
 * milked into a glass bottle rather than a bucket, and hands back a potion.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>two copies of one effect</td><td>that potion, at the stronger grade</td></tr>
 *   <tr><td>one copy of one effect</td><td>that potion, weak</td></tr>
 *   <tr><td>two <b>different</b> effects</td><td>both effects, both weak, in one bottle</td></tr>
 * </table>
 *
 * <h2>Why the compound gives both</h2>
 * {@link LycanGene}'s rule - a mismatched pair yields nothing - was considered
 * and rejected here. Mismatch has to bite on lycan because a shifting horse is a
 * whole animal and picking one of two shapes is incoherent. A potion is not a
 * shape: two effects in one bottle is a thing the game already has, so the
 * honest reading of two half-doses is two half-doses.
 *
 * <p>That puts one requirement on the translator, and it is the one part of this
 * gene that is not free: <b>two yields of the same kind must merge</b> into a
 * single item carrying both effects, rather than the first one firing. Without
 * the merge the gene silently gives half of what this page promises.
 *
 * <h2>It does not touch the milk locus</h2>
 * {@link MilkGene} is a different locus with different alleles and a different
 * container, and a horse may express both. The container is the disambiguator,
 * which is why the trigger carries the item rather than the gene carrying a
 * mode. Declaring the same yield {@code kind} hands governance of how often to
 * {@link MagicMilkVolumeGene} for free, without either gene knowing about the
 * other.
 */
public final class PotionMilkGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.potion_milk";
    public static final int PRIORITY = 178;

    /** The container, and what comes back. */
    public static final String CONSUMES = "minecraft:glass_bottle";
    public static final String PRODUCES = "minecraft:potion";

    /** The same kind milk declares, so the volume locus governs this too, for free. */
    public static final String YIELD_KIND = "milk";

    /** Ticks. The weak and strong grades differ in duration and amplifier, not in cooldown. */
    public static final int COOLDOWN_TICKS = 6000;
    public static final int WEAK_DURATION = 900;
    public static final int STRONG_DURATION = 1800;

    public static final double WILD_CARRIER_PERCENT = 10.0;

    /** One effect this locus can produce. */
    public record Brew(Allele allele, String effect, String label) {
    }

    private final List<Brew> brews = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;
    private final List<Expression> expressions;
    private final Expression[] singles;
    private final Expression[] doubles;
    private final Expression mixed;
    private final Expression wild = Expression.wildType(
            "A bottle gets you nothing; this mare is not that sort of mare.");

    private final FounderTable founders;

    public PotionMilkGene() {
        brew("Spd", "minecraft:speed", "Swiftness");
        brew("Str", "minecraft:strength", "Strength");
        brew("Rgn", "minecraft:regeneration", "Regeneration");
        brew("Fir", "minecraft:fire_resistance", "Fire Resistance");
        brew("Nvs", "minecraft:night_vision", "Night Vision");
        brew("Wtr", "minecraft:water_breathing", "Water Breathing");
        brew("Jmp", "minecraft:jump_boost", "Leaping");
        brew("Inv", "minecraft:invisibility", "Invisibility");
        brew("Res", "minecraft:resistance", "Resistance");
        brew("Hst", "minecraft:haste", "Haste");

        n = new Allele(KEY, brews.size(), "n", "Wild-type (n)");
        List<Allele> all = new ArrayList<>();
        for (Brew b : brews) {
            all.add(b.allele());
        }
        all.add(n);
        alleles = List.copyOf(all);

        List<Expression> out = new ArrayList<>();
        out.add(wild);
        singles = new Expression[alleles.size()];
        doubles = new Expression[alleles.size()];
        for (Brew b : brews) {
            Expression weak = Expression.wildType("brew-" + b.allele().token(),
                    b.label() + " milk",
                    "One copy. Milking her with an empty bottle gets a weak potion of "
                            + b.label() + ".");
            Expression strong = Expression.wildType("brew2-" + b.allele().token(),
                    "Strong " + b.label() + " milk",
                    "Two matching copies, and the locus is incompletely dominant - so this is the "
                            + "same potion of " + b.label() + " at twice the duration and a step "
                            + "up in strength. The clearest reason to double a brew line.");
            singles[b.allele().order()] = weak;
            doubles[b.allele().order()] = strong;
            out.add(weak);
            out.add(strong);
        }
        mixed = Expression.wildType("brew-mixed", "Mixed brew",
                "Two different effects. One bottle comes back carrying BOTH of them, each at the "
                        + "weaker grade - not one of the two, and not nothing. A potion is not a "
                        + "shape, so there is no reason a horse cannot mean two things at once.");
        out.add(mixed);
        expressions = List.copyOf(out);

        FounderTable.Builder fb = FounderTable.builder();
        double each = WILD_CARRIER_PERCENT / brews.size();
        for (Brew b : brews) {
            fb.weight(b.allele(), n, each);
        }
        founders = fb.weight(n, n, 100.0 - WILD_CARRIER_PERCENT).build();
    }

    private void brew(String token, String effect, String label) {
        brews.add(new Brew(new Allele(KEY, brews.size(), token, label + " (" + token + ")"),
                effect, label));
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Potion milk"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int a = pair.first().order();
        int b = pair.second().order();
        if (a == n.order()) {
            return wild;
        }
        if (b == n.order()) {
            return singles[a];
        }
        return a == b ? doubles[a] : mixed;
    }

    /**
     * One yield per expressing copy, and the strong grade only for a matched
     * pair.
     *
     * <p>A compound heterozygote therefore returns <i>two</i> weak yields of the
     * same kind, which is precisely the case the translator has to merge into
     * one bottle. It is written this way rather than as a single pre-merged
     * yield because merging is a game-side concern - what a potion item can
     * carry is a Minecraft question, and {@code common/} does not get to know
     * the answer.
     */
    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        int a = pair.first().order();
        int b = pair.second().order();
        if (a == n.order()) {
            return List.of();
        }
        if (a == b) {
            return List.of(bottle(brews.get(a), 1, STRONG_DURATION), STALLION, FOAL, HURT);
        }
        if (b == n.order()) {
            return List.of(bottle(brews.get(a), 0, WEAK_DURATION), STALLION, FOAL, HURT);
        }
        return List.of(bottle(brews.get(a), 0, WEAK_DURATION),
                bottle(brews.get(b), 0, WEAK_DURATION), STALLION, FOAL, HURT);
    }

    /**
     * A tamed mare below her own full health refuses, exactly as the plain milk
     * gene's mare does - the rule that ties milking to the healing gate. The
     * bottle's own condition carries {@code full_health} too; this is the line
     * that says why, instead of the bottle doing nothing (owner's call,
     * 2026-09-10: "an injured potion horse should refuse to give milk same as a
     * milk horse").
     */
    private static final GeneAbility HURT = denied(new GeneAbility.Condition.All(List.of(
                    new GeneAbility.Condition.Flag("adult", false),
                    new GeneAbility.Condition.Flag("sex_female", false),
                    new GeneAbility.Condition.Flag("tamed", false),
                    new GeneAbility.Condition.Flag("full_health", true))),
            0.0, "message.horsegenetics.potion_milk.hurt");

    /** The kick, in the same currency as {@code MilkGene}'s. */
    public static final double STALLION_KICK_DAMAGE = MilkGene.STALLION_KICK_DAMAGE;

    /**
     * <b>The else branches</b>, the same two {@code MilkGene} has: a stallion
     * kicks, a foal says it has nothing. Without them a bottle held out to the
     * wrong horse fell through to vanilla and did nothing at all, which the owner
     * reported (2026-09-10) as reading like a bug. No kind, so the volume locus
     * cannot make a stallion kick more often.
     */
    private static final GeneAbility STALLION = denied(new GeneAbility.Condition.All(List.of(
                    new GeneAbility.Condition.Flag("adult", false),
                    new GeneAbility.Condition.Flag("sex_male", false))),
            STALLION_KICK_DAMAGE, "message.horsegenetics.potion_milk.stallion");

    private static final GeneAbility FOAL = denied(new GeneAbility.Condition.Flag("baby", false),
            0.0, "message.horsegenetics.potion_milk.foal");

    private static GeneAbility denied(GeneAbility.Condition when, double damage, String messageKey) {
        return new GeneAbility.Yield(new GeneAbility.Trigger.OnInteract(CONSUMES),
                "", "", 0, damage, messageKey, "", "", 0, 0, when, 1);
    }

    private static GeneAbility bottle(Brew brew, int amplifier, int duration) {
        return new GeneAbility.Yield(new GeneAbility.Trigger.OnInteract(CONSUMES),
                CONSUMES, PRODUCES, COOLDOWN_TICKS, 0.0, "", YIELD_KIND,
                brew.effect(), amplifier, duration,
                new GeneAbility.Condition.All(List.of(
                        new GeneAbility.Condition.Flag("sex_female", false),
                        new GeneAbility.Condition.Flag("adult", false),
                        new GeneAbility.Condition.Flag("tamed", false),
                        new GeneAbility.Condition.Flag("full_health", false))),
                1);
    }
}
