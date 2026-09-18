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
 * <b>Magic day watch</b> ({@code horsegenetics.magic_day_watch}) - the daylight
 * twin of {@link MagicNightWatchGene}: what the horse does about <i>you</i> while
 * the sun is up.
 *
 * <p>The same five-rung ladder - stare, close in, watch while it can see you,
 * watch from where you cannot see it, stand behind you - on the same tokens, and
 * the same silence on its feet while it does it. After dark it is an ordinary
 * horse.
 *
 * <p>Rarer than the night locus: {@value #WILD_EACH_PERCENT}% of wild horses carry
 * each variant and the rest are wild type. It yields to {@link AggressionGene}
 * and {@link SkittishGene} exactly as the night watch does. Paints nothing.
 */
public final class MagicDayWatchGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.magic_day_watch";
    public static final int PRIORITY = 148;

    /** Share of wild horses carrying each variant, silently. The rest are {@code n/n}. */
    public static final double WILD_EACH_PERCENT = 1.5;

    private record Variant(String token, String label, String mode, double radius,
                           String name, String description) {
    }

    private static final List<Variant> VARIANTS = List.of(
            new Variant("Wst", "Day watcher, fixed (Wst)", "stare", MagicNightWatchGene.WATCH_RADIUS,
                    "Stares by day without moving",
                    "Two copies. In broad daylight the horse stops, turns to face the nearest "
                            + "player and holds there, through walls, making no sound. It is "
                            + "somehow worse with the sun out."),
            new Variant("Wnr", "Day watcher, closing (Wnr)", "approach", MagicNightWatchGene.NEAR_RADIUS,
                    "Closes, then stares, by day",
                    "Two copies. The same stare, but it comes to about "
                            + (int) MagicNightWatchGene.NEAR_RADIUS + " blocks first and stops."),
            new Variant("Wsi", "Day watcher, sighted (Wsi)", "line_of_sight", MagicNightWatchGene.WATCH_RADIUS,
                    "Watches by day while it can see you",
                    "Two copies. It watches only while it has line of sight, and goes back to "
                            + "grazing the moment you step behind something."),
            new Variant("Wun", "Day watcher, unseen (Wun)", "unseen", MagicNightWatchGene.WATCH_RADIUS,
                    "Watches by day from where you cannot see it",
                    "Two copies. It puts itself outside wherever you are looking and watches from "
                            + "there, silently, all day."),
            new Variant("Wbh", "Day watcher, close behind (Wbh)", "behind", MagicNightWatchGene.BEHIND_RADIUS,
                    "Stands directly behind you by day",
                    "Two copies. It gets as close behind you as it can while you are not looking "
                            + "at it, and is an ordinary horse the instant you turn round."));

    private final List<Allele> alleles;
    private final Allele n;
    private final List<Expression> expressions;
    private final Expression WILD;
    private final List<Expression> byVariant = new ArrayList<>();
    private final FounderTable founders;

    public MagicDayWatchGene() {
        List<Allele> built = new ArrayList<>();
        for (int i = 0; i < VARIANTS.size(); i++) {
            Variant v = VARIANTS.get(i);
            built.add(new Allele(KEY, i, v.token(), v.label()));
        }
        this.n = new Allele(KEY, VARIANTS.size(), "n", "Wild-type (n)");
        built.add(n);
        this.alleles = List.copyOf(built);

        this.WILD = Expression.wildType(
                "An ordinary horse. This is every combination but a matched pair, including two "
                        + "DIFFERENT watchers - it shows neither.");
        List<Expression> all = new ArrayList<>();
        all.add(WILD);
        for (Variant v : VARIANTS) {
            Expression e = Expression.wildType("day-watch-" + v.mode(), v.name(), v.description());
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
    @Override public String name() { return "Magic day watch"; }
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
        return List.of(new GeneAbility.DayWatch(v.mode(), v.radius(), true,
                GeneAbility.Condition.ALWAYS, 1));
    }
}
