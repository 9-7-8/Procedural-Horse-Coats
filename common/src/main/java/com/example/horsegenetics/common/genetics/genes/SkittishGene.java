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
import java.util.Locale;

/**
 * <b>Skittishness</b> ({@code horsegenetics.skittish}) - what the horse runs
 * from, and when. The mirror of {@link AggressionGene}, allele for allele.
 *
 * <p>The two loci were one locus once: the night and day temper genes carried
 * four "hunts" alleles and four "flees" alleles each, so a horse could not be
 * both a monster-hunter and shy of people. Splitting the mood out means it can -
 * and it is the more interesting animal. "Goes for monsters, runs from riders"
 * is a herd-guard nobody can catch, and it took two loci to say.
 *
 * <h2>The grid</h2>
 * The same {@value AggressionGene#TIMES} times crossed with the same
 * {@value AggressionGene#TARGETS} targets, on {@code F}-prefixed tokens so the
 * two loci read alike: {@code Fdc} flees herds by day, {@code Fnp} flees riders
 * after dark, {@code Faa} flees everything, always. Every allele is recessive
 * and recessive to the others - two copies of the <b>same</b> allele or nothing,
 * exactly as on the aggression locus.
 *
 * <h2>It is not gated on the saddle, and that is deliberate</h2>
 * {@link AggressionGene} stops while somebody is riding, because a horse
 * fighting while you steer it is a horse fighting your steering. Fleeing is the
 * opposite case: a mount that bolts when a creeper comes round the corner is a
 * real behaviour and a real problem to breed out, and gating it on an empty
 * saddle would delete the only version of this gene a rider ever meets.
 *
 * <h2>Shy is commoner than dangerous</h2>
 * A frightened horse costs its owner a fence and an afternoon; an aggressive one
 * costs them the horse. So the expressing combinations here are several times
 * commoner in the wild than the aggression locus's - see
 * {@link Target#wildExpressing()} - and a wild herd that scatters is meant to be
 * an ordinary thing to meet.
 *
 * <p>Paints nothing; the locus is out of the texture key.
 */
public final class SkittishGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.skittish";

    /** The slot the day temper locus used to hold. It paints nothing, so the number is a code-order slot. */
    public static final int PRIORITY = 147;

    /** How far it notices, in blocks. Shared with the aggression locus - the same eyes. */
    public static final double RADIUS = AggressionGene.RADIUS;

    public static final int INTERVAL_TICKS = AggressionGene.INTERVAL_TICKS;
    public static final int MAX_TARGETS = AggressionGene.MAX_TARGETS;

    /** When it is frightened. No saddle gate - see the class note. */
    private enum TimeGate {
        DAY("Fd", "by day", "while the sun is up",
                new GeneAbility.Condition.Flag("day", false)),
        NIGHT("Fn", "after dark", "from dusk",
                new GeneAbility.Condition.Flag("night", false)),
        ALWAYS("Fa", "always", "at any hour", GeneAbility.Condition.ALWAYS);

        private final String prefix;
        private final String label;
        private final String phrase;
        private final GeneAbility.Condition condition;

        TimeGate(String prefix, String label, String phrase, GeneAbility.Condition condition) {
            this.prefix = prefix;
            this.label = label;
            this.phrase = phrase;
            this.condition = condition;
        }

        String label() {
            return label;
        }

        String phrase() {
            return phrase;
        }
    }

    /** What frightens it. Same vocabulary words as the aggression locus. */
    private enum Target {
        HERDS("c", "herds", "passive", "other animals, and drifts out of any herd it was in",
                0.50, 2.0),
        MONSTERS("h", "monsters", "hostile", "any monster it can see, rather than standing to be hit",
                0.60, 2.2),
        RIDERS("p", "riders", "players", "any player who comes near it, and keeps going",
                0.30, 1.6),
        HORSES("e", "horses", "horses", "other horses, which is a real problem for a herd animal",
                0.25, 1.2),
        EVERYTHING("a", "everything", "all", "anything that moves, all day, every day",
                0.15, 0.8);

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

        double wildExpressing() {
            return wildExpressing;
        }

        double wildCarrier() {
            return wildCarrier;
        }
    }

    private record Variant(Allele allele, TimeGate time, Target target) {
    }

    private final List<Variant> variants = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;
    private final List<Expression> expressions;
    private final Expression[] byVariant;

    private final Expression wild = Expression.wildType(
            "A horse with ordinary nerve. This is every combination but a matched pair - two "
                    + "DIFFERENT shy alleles show neither.");

    private final FounderTable founders;

    public SkittishGene() {
        List<Allele> built = new ArrayList<>();
        for (TimeGate time : TimeGate.values()) {
            for (Target target : Target.values()) {
                Allele allele = new Allele(KEY, built.size(), time.prefix + target.suffix,
                        "Flees " + target.label + " " + time.label
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
                    "flee-" + v.time().name().toLowerCase(Locale.ROOT) + "-" + v.target().group(),
                    "Flees " + v.target().label() + " " + v.time().label(),
                    "Two copies. " + capitalise(v.time().phrase()) + " the horse runs from "
                            + v.target().prose() + ". It will not stay where it was left, and "
                            + "being ridden makes no difference to it.");
            byVariant[v.allele().order()] = e;
            all.add(e);
        }
        this.expressions = List.copyOf(all);

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
        // The baseline last - a FounderTable invariant GenotypeTest asserts.
        this.founders = table.weight(n, n, 100.0 - claimed).build();
    }

    private static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private Variant expressed(AllelePair pair) {
        for (Variant v : variants) {
            if (pair.homozygousFor(v.allele())) {
                return v;
            }
        }
        return null;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Skittishness"; }
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
        return List.of(new GeneAbility.Temper("flee", v.target().group(), RADIUS,
                INTERVAL_TICKS, MAX_TARGETS, true, new GeneAbility.Trigger.Continuous(),
                v.time().condition, 1));
    }
}
