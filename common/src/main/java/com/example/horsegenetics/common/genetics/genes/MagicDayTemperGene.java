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
 * <b>Magic day temper</b> ({@code horsegenetics.magic_day_temper}) - the daylight
 * twin of {@link MagicNightTemperGene}: what the horse becomes while the sun is up.
 *
 * <p>The same eight variants in the same two families - four that hunt, four that
 * run, each naming riders, passive animals, monsters or everything - and the same
 * tokens, so a breeder who knows one locus can read the other. After dark every
 * one of them is an ordinary horse.
 *
 * <h2>Rarer than the night locus, on purpose</h2>
 * The night locus makes every wild horse a hidden carrier. This one does not:
 * each variant is carried by {@value #WILD_EACH_PERCENT}% of wild horses and the
 * rest are wild type, so a day hunter is something to find rather than something
 * every herd hides. Owner's call, 2026-09-13.
 *
 * <p>It talks over {@link MagicDayWatchGene} the way the night temper talks over
 * the night watch - see {@code neoforge/server/NightBehaviourHandler}, which runs
 * both pairs. Paints nothing.
 */
public final class MagicDayTemperGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.magic_day_temper";
    public static final int PRIORITY = 147;

    /** Share of wild horses carrying each variant, silently. The rest are {@code n/n}. */
    public static final double WILD_EACH_PERCENT = 1.0;

    private record Variant(String token, String label, String mood, String towards,
                           String name, String description) {
    }

    private static final List<Variant> VARIANTS = List.of(
            new Variant("Agp", "Day-hunter, riders (Agp)", "aggressive", "players",
                    "Hunts riders by day",
                    "Two copies. While it is light the horse goes for any player within about "
                            + (int) MagicNightTemperGene.RADIUS + " blocks, and after dark it is an "
                            + "ordinary horse. The opposite of what anyone expects of a dangerous "
                            + "animal, which is the trap."),
            new Variant("Agc", "Day-hunter, herds (Agc)", "aggressive", "passive",
                    "Hunts animals by day",
                    "Two copies. By day it goes for the passive animals around it - the cows, the "
                            + "sheep. A problem you can at least watch happening."),
            new Variant("Agh", "Day-hunter, monsters (Agh)", "aggressive", "hostile",
                    "Hunts monsters by day",
                    "Two copies. By day it goes for any monster still about - the spiders, the "
                            + "creepers in the shade, a drowned at the shore. Quiet work, since "
                            + "most of them burn at sunrise."),
            new Variant("Aga", "Day-hunter, everything (Aga)", "aggressive", "all",
                    "Hunts everything by day",
                    "Two copies. Everything within reach while the sun is up, its own owner "
                            + "included - and a perfectly ordinary horse to ride home at night."),
            new Variant("Flp", "Day-shy, riders (Flp)", "flee", "players",
                    "Flees riders by day",
                    "Two copies. While it is light it will not let a player near it. After dark "
                            + "it can be caught, tamed and ridden - which makes it a night horse "
                            + "whether you wanted one or not."),
            new Variant("Flc", "Day-shy, herds (Flc)", "flee", "passive",
                    "Flees animals by day",
                    "Two copies. By day it will not stay near other animals, and spends the "
                            + "daylight hours at the edge of any herd."),
            new Variant("Flh", "Day-shy, monsters (Flh)", "flee", "hostile",
                    "Flees monsters by day",
                    "Two copies. It runs from monsters while it is light - rarely needed, since "
                            + "most of them are gone by then, and useless at night when they come."),
            new Variant("Fla", "Day-shy, everything (Fla)", "flee", "all",
                    "Flees everything by day",
                    "Two copies. It runs from anything that moves, all day, every day, and "
                            + "comes back calm at dusk."));

    private final List<Allele> alleles;
    private final Allele n;
    private final List<Expression> expressions;
    private final Expression WILD;
    private final List<Expression> byVariant = new ArrayList<>();
    private final FounderTable founders;

    public MagicDayTemperGene() {
        List<Allele> built = new ArrayList<>();
        for (int i = 0; i < VARIANTS.size(); i++) {
            Variant v = VARIANTS.get(i);
            built.add(new Allele(KEY, i, v.token(), v.label()));
        }
        this.n = new Allele(KEY, VARIANTS.size(), "n", "Wild-type (n)");
        built.add(n);
        this.alleles = List.copyOf(built);

        this.WILD = Expression.wildType(
                "An ordinary horse, day and night. This is every combination but a matched "
                        + "pair - including two DIFFERENT variants, which shows neither.");
        List<Expression> all = new ArrayList<>();
        all.add(WILD);
        for (Variant v : VARIANTS) {
            Expression e = Expression.wildType("day-" + v.mood() + "-" + v.towards(), v.name(),
                    v.description());
            byVariant.add(e);
            all.add(e);
        }
        this.expressions = List.copyOf(all);

        FounderTable.Builder table = FounderTable.builder();
        for (Allele a : alleles) {
            if (!a.equals(n)) {
                table.weight(a, n, WILD_EACH_PERCENT);
            }
        }
        // The baseline last - "the last bucket is the baseline" is a FounderTable invariant.
        this.founders = table.weight(n, n, 100.0 - WILD_EACH_PERCENT * VARIANTS.size()).build();
    }

    private int expressedIndex(AllelePair pair) {
        for (int i = 0; i < VARIANTS.size(); i++) {
            if (pair.homozygousFor(alleles.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic day temper"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int i = expressedIndex(pair);
        return i < 0 ? WILD : byVariant.get(i);
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        int i = expressedIndex(pair);
        if (i < 0) {
            return List.of();
        }
        Variant v = VARIANTS.get(i);
        return List.of(new GeneAbility.DayTemper(v.mood(), v.towards(), MagicNightTemperGene.RADIUS,
                MagicNightTemperGene.INTERVAL_TICKS, MagicNightTemperGene.MAX_TARGETS,
                GeneAbility.Condition.ALWAYS, 1));
    }
}
