package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A climate locus</b> (owner, 2026-09-15): a tolerant allele that helps the horse in one kind of biome, a sensitive
 * allele that hurts it there, and the wild type, which is neutral. {@link MagicHeatGene} keys off a hot biome and
 * {@link MagicColdGene} off a cold one; the translator owns what those two words mean (the {@code hot_biome} and
 * {@code cold_biome} flags).
 *
 * <h2>Two numbers on every copy</h2>
 * The genetics are {@link AbstractMagicFactorGene}'s - three alleles, both copies add, the sensitive allele counting
 * negative, so a tolerant/sensitive horse lands near neutral while passing on both - but each copy carries <b>two</b>
 * values rather than one:
 * <ul>
 *   <li>{@value #DELTA}, how much the copy is worth, a bounded normal draw about {@value #MEAN_DELTA};</li>
 *   <li>{@value #SPEED_SHARE}, how that amount divides between speed and jump: 1 is all speed, 0 all jump.</li>
 * </ul>
 * So one heat-tolerant horse runs faster in the desert and another jumps higher there, and a foal takes each copy's
 * split with the copy. That is why this is its own class and not a factor gene, whose one sum has nowhere to keep a
 * split.
 *
 * <h2>Paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type}, as on the other factor loci: the locus stays out of the
 * texture key.
 */
public abstract class AbstractClimateGene implements Gene, EpigeneticAbilityContribution {

    /** How much one copy is worth, as a fraction of the stat. */
    public static final String DELTA = "delta";
    /** How a copy's amount divides between speed (1) and jump (0). */
    public static final String SPEED_SHARE = "speed_share";

    public static final double MEAN_DELTA = 0.20;
    public static final double SIGMA_DELTA = 0.10;
    /** The floor on a copy's amount, so a tolerant copy can never come out hurting. */
    public static final double MIN_DELTA = 0.02;

    /** Share of wild founders (Feral Mixed) carrying one variant copy, split evenly between the two. */
    public static final double WILD_CARRIER_PERCENT = 16.0;

    /** The floor on the resulting multiplier: a horse is slowed by a climate, never stopped. */
    public static final double MIN_FACTOR = 0.05;

    private final String key;
    private final int priority;
    private final String displayName;
    private final String flag;

    public final Allele tolerant;
    public final Allele sensitive;
    public final Allele n;
    private final List<Allele> alleles;

    private final Expression wild;
    private final Expression tolerantOne;
    private final Expression tolerantTwo;
    private final Expression sensitiveOne;
    private final Expression sensitiveTwo;
    private final Expression balanced;
    private final List<Expression> expressions;

    private final FounderTable founders;

    /**
     * @param flag  the condition flag that says the horse is in this climate
     * @param place how a sentence names that climate, e.g. "a hot biome"
     */
    protected AbstractClimateGene(String key, int priority, String displayName, String flag, String place,
                                  String tolerantToken, String tolerantLabel,
                                  String sensitiveToken, String sensitiveLabel) {
        this.key = key;
        this.priority = priority;
        this.displayName = displayName;
        this.flag = flag;

        this.tolerant = new Allele(key, 0, tolerantToken, tolerantLabel + " (" + tolerantToken + ")");
        this.sensitive = new Allele(key, 1, sensitiveToken, sensitiveLabel + " (" + sensitiveToken + ")");
        this.n = new Allele(key, 2, "n", "Wild-type (n)");
        this.alleles = List.of(tolerant, sensitive, n);

        String split = " How much, and whether it goes to speed, jump or some of each, is written on each copy.";
        this.wild = Expression.wildType("The horse is the same in " + place + " as anywhere else.");
        this.tolerantOne = Expression.wildType("tolerant", tolerantLabel,
                "In " + place + " the horse moves or jumps better." + split);
        this.tolerantTwo = Expression.wildType("double-tolerant", tolerantLabel + ", both copies",
                "Two tolerant copies, and their amounts add: in " + place + " the horse is markedly better." + split);
        this.sensitiveOne = Expression.wildType("sensitive", sensitiveLabel,
                "In " + place + " the horse moves or jumps worse." + split);
        this.sensitiveTwo = Expression.wildType("double-sensitive", sensitiveLabel + ", both copies",
                "Two sensitive copies, and their amounts add: in " + place + " the horse is markedly worse." + split);
        this.balanced = Expression.wildType("balanced", "Tolerant and sensitive",
                "One copy of each. They pull against each other, so the horse is close to neutral in " + place
                        + ", and either way it can pass on both.");
        this.expressions = List.of(wild, tolerantOne, tolerantTwo, sensitiveOne, sensitiveTwo, balanced);

        double half = WILD_CARRIER_PERCENT / 2;
        this.founders = FounderTable.builder()
                .weight(tolerant, n, half)
                .weight(sensitive, n, half)
                .weight(n, n, 100.0 - WILD_CARRIER_PERCENT)
                .build();
    }

    @Override public String key() { return key; }
    @Override public String name() { return displayName; }
    @Override public int priority() { return priority; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /** The condition flag this locus keys off. */
    public String flag() {
        return flag;
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int more = pair.count(tolerant);
        int less = pair.count(sensitive);
        if (more > 0 && less > 0) {
            return balanced;
        }
        if (more == 2) {
            return tolerantTwo;
        }
        if (more == 1) {
            return tolerantOne;
        }
        if (less == 2) {
            return sensitiveTwo;
        }
        return less == 1 ? sensitiveOne : wild;
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.gaussian(DELTA, MEAN_DELTA, SIGMA_DELTA, MIN_DELTA),
                EpiValue.uniform(SPEED_SHARE, 0.0, 1.0));
    }

    /**
     * One speed modifier and one jump modifier, each gated on the climate. Each copy's signed amount is split by its
     * own share and the two halves are summed across both copies, so a copy that is all speed and a copy that is all
     * jump give a horse that is better at both, each by one copy's worth.
     */
    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype, GeneEpigenetics epigenetics) {
        double speed = 0.0;
        double jump = 0.0;
        for (int i = 0; i < 2; i++) {
            Allele allele = i == 0 ? pair.first() : pair.second();
            double sign = allele.equals(tolerant) ? 1.0 : allele.equals(sensitive) ? -1.0 : 0.0;
            if (sign == 0.0) {
                continue;
            }
            EpiValues copy = epigenetics.copy(i);
            double amount = sign * copy.get(DELTA);
            double share = copy.get(SPEED_SHARE);
            speed += amount * share;
            jump += amount * (1.0 - share);
        }
        List<GeneAbility> out = new ArrayList<>(2);
        GeneAbility.Condition when = new GeneAbility.Condition.Flag(flag, false);
        if (speed != 0.0) {
            out.add(new GeneAbility.AttributeMod("movement_speed", "multiply_total",
                    Math.max(MIN_FACTOR - 1.0, speed), when, 1));
        }
        if (jump != 0.0) {
            out.add(new GeneAbility.AttributeMod("jump_strength", "multiply_total",
                    Math.max(MIN_FACTOR - 1.0, jump), when, 1));
        }
        return List.copyOf(out);
    }
}
