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
 * <b>Aggression</b> ({@code horsegenetics.aggression}) - the one locus that
 * decides what a horse attacks, and when.
 *
 * <p>It replaces three loci that each answered half the question:
 * {@code magic_night_temper} and {@code magic_day_temper} named a target but
 * welded one half of the day into the verb, and {@code gladiator} named a gate
 * but only ever one target. A breeder had to know three loci to answer "is this
 * horse dangerous", and two of them could disagree. Now there is one, and its
 * alleles are a grid.
 *
 * <h2>The grid</h2>
 * Every allele is a <b>time</b> crossed with a <b>target</b> - {@value #TIMES}
 * times, {@value #TARGETS} targets, and so {@code timesTargets()} alleles plus
 * the wild type.
 *
 * <table>
 *   <tr><th></th><th>{@code c} herds</th><th>{@code h} monsters</th>
 *       <th>{@code p} riders</th><th>{@code e} horses</th><th>{@code a} everything</th></tr>
 *   <tr><th>{@code Ad} by day</th><td>Adc</td><td>Adh</td><td>Adp</td><td>Ade</td><td>Ada</td></tr>
 *   <tr><th>{@code An} after dark</th><td>Anc</td><td>Anh</td><td>Anp</td><td>Ane</td><td>Ana</td></tr>
 *   <tr><th>{@code Aa} always</th><td>Aac</td><td>Aah</td><td>Aap</td><td>Aae</td><td>Aaa</td></tr>
 * </table>
 *
 * <h2>Every allele is recessive, and recessive to the others</h2>
 * Two copies of the <b>same</b> allele, or the horse is an ordinary horse.
 * {@code Adh/n} is calm; {@code Adh/Anh} is <b>also</b> calm - "hunts monsters by
 * day" and "hunts monsters after dark" are two instructions and the animal
 * settles on neither. That is the same rule {@link LycanGene} settled, and it is
 * what makes a fifteen-allele locus a search with a target rather than a slot
 * machine: you must find the same allele twice.
 *
 * <h2>The time is a condition now, not a verb</h2>
 * The night locus welded darkness into {@code night_temper} on purpose, and its
 * own note defended that: eight alleles each carrying {@code "when": night} is a
 * gate written eight times and wrong the first time somebody forgets it. That
 * argument is about a <i>gene file</i>, where each allele is hand-written. Here
 * the alleles are generated from {@link #TIME_GATES} in a loop, so the gate is
 * written <b>once</b> and cannot drift between alleles - and the general
 * {@code temper} verb already carried a {@code when}. So the two time-of-day
 * verbs are gone and this locus uses {@code temper} for all three times.
 *
 * <h2>It holds its ground - but a saddle is not a gate</h2>
 * <b>{@code hold}</b> is inherited from the gladiator locus this absorbs and is
 * still a settled call: it attacks what comes within reach and returns, never
 * pursuing. A horse that chases is a horse that dies forty blocks from its
 * owner, and the owner blames the gene, correctly.
 *
 * <p>Gladiator's <i>other</i> rail is gone. That locus stopped dead while
 * somebody was riding it, on the argument that a horse fighting while you steer
 * is a horse fighting your steering. <b>The owner reopened that deliberately</b>,
 * and the rule now is the more frightening one:
 * <ul>
 *   <li>aggressive toward <b>riders</b>, and being ridden is no protection - the
 *       horse <b>bucks its rider off</b> and then comes for them. A horse that
 *       hunts people is not a horse you can simply sit on;</li>
 *   <li>aggressive toward <b>anything else</b>, and it <b>ignores its rider</b>
 *       and goes for the target regardless. You are a passenger on it, not a
 *       driver of it, until the fight is over.</li>
 * </ul>
 * Neither is expressed here. "The horse has just targeted its own rider" is a
 * question about a live horse, so both live in the translator, at the same
 * chokepoint {@link PassificationGene}'s veto does - which is also what makes
 * them true of <i>every</i> source of aggression rather than of this locus
 * alone.
 *
 * <h2>Some of it can be caught</h2>
 * Unlike the night locus it replaces - whose founder table was entirely
 * heterozygous, so no wild horse ever expressed one - this one puts expressing
 * combinations in the wild, and the nastier the target the rarer they are (see
 * {@link Target#wildExpressing()}). A horse that hunts monsters is something to
 * find; a horse that hunts everything is something somebody bred. It matters
 * beyond flavour: {@link PassificationGene} exists to tame a wild horse that is
 * aggressive to players, and there has to <i>be</i> one. (Owner's call.)
 *
 * <p>Paints nothing - every outcome is a {@link Expression#wildType() wild
 * type}, so the locus is out of the texture key and the genotype gallery
 * collapses it to one entry.
 */
public final class AggressionGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.aggression";

    /** The slot the night temper locus used to hold. It paints nothing, so the number is a code-order slot. */
    public static final int PRIORITY = 136;

    /** How far it notices, in blocks. */
    public static final double RADIUS = 16.0;

    /** Ticks between scans. A second - often enough to feel alive, cheap enough for every horse. */
    public static final int INTERVAL_TICKS = 20;

    /** Most entities one scan may consider. Every radius effect here states a cap. */
    public static final int MAX_TARGETS = 8;

    /** How many times of day the grid has. */
    public static final int TIMES = 3;

    /** How many targets the grid has. */
    public static final int TARGETS = 5;

    /**
     * <b>When</b> the horse is dangerous, and the {@code when} clause that says
     * so. Written once here rather than once per allele - see the class note.
     *
     * <p>The clock is the <b>whole</b> condition. There is deliberately no
     * saddle term: an aggressive horse is aggressive while you are on it, and
     * what happens to the rider is the translator's business.
     */
    private enum TimeGate {
        DAY("Ad", "by day", "while the sun is up",
                new GeneAbility.Condition.Flag("day", false)),
        NIGHT("An", "after dark", "from dusk",
                new GeneAbility.Condition.Flag("night", false)),
        ALWAYS("Aa", "always", "at any hour", GeneAbility.Condition.ALWAYS);

        private final String prefix;
        private final String label;
        private final String phrase;
        private final GeneAbility.Condition clock;

        TimeGate(String prefix, String label, String phrase, GeneAbility.Condition clock) {
            this.prefix = prefix;
            this.label = label;
            this.phrase = phrase;
            this.clock = clock;
        }

        GeneAbility.Condition condition() {
            return clock;
        }

        String label() {
            return label;
        }

        String phrase() {
            return phrase;
        }
    }

    /**
     * <b>Who</b> it goes for. {@code group} is a {@code MOB_GROUPS} vocabulary
     * word, resolved against entity type tags by the translator - never a
     * hardcoded list of vanilla mobs, or every modded creature goes invisible to
     * the locus at once and nothing errors.
     */
    private enum Target {
        HERDS("c", "herds", "passive", "the passive animals around it - the cows, the sheep",
                0.20, 1.4),
        MONSTERS("h", "monsters", "hostile", "any monster that comes within reach",
                0.40, 1.6),
        RIDERS("p", "riders", "players", "any player that comes within reach",
                0.08, 0.8),
        HORSES("e", "horses", "horses", "other horses - its own herd included",
                0.20, 1.0),
        EVERYTHING("a", "everything", "all", "anything alive that comes within reach, its own owner included",
                0.03, 0.4);

        private final String suffix;
        private final String label;
        private final String group;
        private final String prose;
        private final double wildExpressing;
        private final double wildCarrier;

        Target(String suffix, String label, String group, String prose,
               double wildExpressing, double wildCarrier) {
            this.suffix = suffix;
            this.label = label;
            this.group = group;
            this.prose = prose;
            this.wildExpressing = wildExpressing;
            this.wildCarrier = wildCarrier;
        }

        String label() {
            return label;
        }

        /** The {@code MOB_GROUPS} vocabulary word the translator resolves. */
        String group() {
            return group;
        }

        String prose() {
            return prose;
        }

        /** Share of wild horses born homozygous for <b>one</b> allele of this target. */
        double wildExpressing() {
            return wildExpressing;
        }

        /** Share of wild horses born carrying <b>one</b> allele of this target, silently. */
        double wildCarrier() {
            return wildCarrier;
        }
    }

    /** One allele of the grid. */
    private record Variant(Allele allele, TimeGate time, Target target) {
    }

    private final List<Variant> variants = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;
    private final List<Expression> expressions;

    /** Indexed by allele order; {@code null} at the wild type's slot. */
    private final Expression[] byVariant;

    private final Expression wild = Expression.wildType(
            "An ordinary horse. This is every combination but a matched pair - including a "
                    + "horse carrying two DIFFERENT aggression alleles, which shows neither, "
                    + "because \"hunt monsters by day\" and \"hunt riders at night\" cannot both "
                    + "be what one animal does.");

    private final FounderTable founders;

    public AggressionGene() {
        List<Allele> built = new ArrayList<>();
        for (TimeGate time : TimeGate.values()) {
            for (Target target : Target.values()) {
                Allele allele = new Allele(KEY, built.size(), time.prefix + target.suffix,
                        "Hunts " + target.label + " " + time.label
                                + " (" + time.prefix + target.suffix + ")");
                built.add(allele);
                variants.add(new Variant(allele, time, target));
            }
        }
        this.n = new Allele(KEY, built.size(), "n", "Wild-type (n)");
        built.add(n);
        this.alleles = List.copyOf(built);

        List<Expression> all = new ArrayList<>();
        all.add(wild);
        this.byVariant = new Expression[alleles.size()];
        for (Variant v : variants) {
            Expression e = Expression.wildType(
                    "aggressive-" + v.time().name().toLowerCase(java.util.Locale.ROOT)
                            + "-" + v.target().group(),
                    "Hunts " + v.target().label() + " " + v.time().label(),
                    "Two copies. " + capitalise(v.time().phrase()) + " the horse goes for "
                            + v.target().prose() + " within about " + (int) RADIUS
                            + " blocks - and it holds its ground rather than chasing. Riding it "
                            + "is no protection: "
                            + (v.target() == Target.RIDERS || v.target() == Target.EVERYTHING
                                    ? "it throws you out of the saddle and comes for you."
                                    : "it ignores you and goes for the target anyway."));
            byVariant[v.allele().order()] = e;
            all.add(e);
        }
        this.expressions = List.copyOf(all);

        // Expressing rows first, then the silent carriers, then the baseline.
        // The n/n row is LAST on purpose: "the last bucket is the baseline" is a
        // FounderTable invariant GenotypeTest asserts.
        FounderTable.Builder table = FounderTable.builder();
        double claimed = 0.0;
        for (Variant v : variants) {
            table.weight(v.allele(), v.target().wildExpressing());
            claimed += v.target().wildExpressing();
        }
        for (Variant v : variants) {
            table.weight(v.allele(), n, v.target().wildCarrier());
            claimed += v.target().wildCarrier();
        }
        this.founders = table.weight(n, n, 100.0 - claimed).build();
    }

    private static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** The variant this horse is homozygous for, or {@code null}. */
    private Variant expressed(AllelePair pair) {
        for (Variant v : variants) {
            if (pair.homozygousFor(v.allele())) {
                return v;
            }
        }
        return null;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Aggression"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        Variant v = expressed(pair);
        return v == null ? wild : byVariant[v.allele().order()];
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        Variant v = expressed(pair);
        if (v == null) {
            return List.of();
        }
        return List.of(new GeneAbility.Temper("aggressive", v.target().group(), RADIUS,
                INTERVAL_TICKS, MAX_TARGETS, true, new GeneAbility.Trigger.Continuous(),
                v.time().condition(), 1));
    }
}
