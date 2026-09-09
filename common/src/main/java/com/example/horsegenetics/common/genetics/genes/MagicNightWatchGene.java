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
 * <b>Magic night watch</b> ({@code horsegenetics.magic_night_watch}) - what the
 * horse does about <i>you</i> after dark.
 *
 * <p>Five variants, and they are a progression rather than a list. Each one is
 * a little closer and a little worse.
 *
 * <table>
 *   <tr><th>combination</th><th>after dark</th></tr>
 *   <tr><td>{@code Wst/Wst}</td><td>stands still and stares, <b>through walls</b>, from wherever it is</td></tr>
 *   <tr><td>{@code Wnr/Wnr}</td><td>closes to about {@value #NEAR_RADIUS} blocks, then stares</td></tr>
 *   <tr><td>{@code Wsi/Wsi}</td><td>watches only while it can actually see you</td></tr>
 *   <tr><td>{@code Wun/Wun}</td><td>moves to where you <b>cannot see it</b>, and watches from there</td></tr>
 *   <tr><td>{@code Wbh/Wbh}</td><td>stands as close behind you as it can get</td></tr>
 *   <tr><td>anything else</td><td>wild type - an ordinary horse</td></tr>
 * </table>
 *
 * <h2>It goes quiet</h2>
 * A horse homozygous for <b>any</b> of these makes <b>no footstep sound at
 * night</b>. That is not a sixth variant, it is a property of all five, and it
 * is what turns the locus from a novelty into the thing it was asked for: the
 * behaviours above are unsettling in proportion to how little warning you get,
 * and hoofbeats are warning.
 *
 * <h2>Every feral horse carries one, and none of them shows it</h2>
 * The founder table is <b>entirely heterozygous</b> - the five variants split
 * the wild population between them at {@value #WILD_EACH_PERCENT}% each and
 * there is no {@code n/n} row. So a caught horse always carries one of the five
 * and never expresses it. Same design as {@link MagicNightTemperGene}, and the
 * same consequence: the plainest outcome the locus has is the one that cannot
 * be caught.
 *
 * <h2>The temper locus talks over it</h2>
 * {@link MagicNightTemperGene} <b>supersedes</b> this whenever it has something
 * to act on. A horse that is both stalking you and fleeing you is not two
 * behaviours, it is a bug - so while the temper gene has a target in range the
 * watching stands down, and it resumes when the field is empty. The rule lives
 * in {@code neoforge/server/NightBehaviourHandler}, because which mobs are
 * nearby is a question only the running game can answer.
 *
 * <h2>Paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type}.
 */
public final class MagicNightWatchGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.magic_night_watch";
    public static final int PRIORITY = 137;

    /** How close the closing variant comes before it stops and stares, in blocks. */
    public static final double NEAR_RADIUS = 10.0;

    /** How far the others look for a player, in blocks. */
    public static final double WATCH_RADIUS = 24.0;

    /** How close the one that gets behind you tries to be, in blocks. */
    public static final double BEHIND_RADIUS = 2.0;

    /** Share of the wild population each variant takes. Five of them, adding to one hundred. */
    public static final double WILD_EACH_PERCENT = 100.0 / 5;

    private record Variant(String token, String label, String mode, double radius,
                           String name, String description) {
    }

    private static final List<Variant> VARIANTS = List.of(
            new Variant("Wst", "Watcher, fixed (Wst)", "stare", WATCH_RADIUS,
                    "Stares without moving",
                    "Two copies. After dark the horse stops. It does not graze, it does not "
                            + "wander, it does not follow - it turns to face the nearest player "
                            + "and holds there, through walls, through floors, from across the "
                            + "valley. It makes no sound while it does it. At dawn it goes back "
                            + "to being a horse."),
            new Variant("Wnr", "Watcher, closing (Wnr)", "approach", NEAR_RADIUS,
                    "Closes, then stares",
                    "Two copies. The same stare, but it comes to about " + (int) NEAR_RADIUS
                            + " blocks first and then stops. It does not come closer and it does "
                            + "not go away, and it makes no sound arriving."),
            new Variant("Wsi", "Watcher, sighted (Wsi)", "line_of_sight", WATCH_RADIUS,
                    "Watches while it can see you",
                    "Two copies. It watches only while it genuinely has line of sight, and goes "
                            + "back to grazing the moment you step behind something. The "
                            + "gentlest of the five, and the only one that can be escaped by "
                            + "closing a door."),
            new Variant("Wun", "Watcher, unseen (Wun)", "unseen", WATCH_RADIUS,
                    "Watches from where you cannot see it",
                    "Two copies. It works out where you are looking and puts itself somewhere "
                            + "else - behind you, round a corner, out of the arc - and watches "
                            + "from there. Turn round and it is already moving. It makes no "
                            + "sound doing any of it."),
            new Variant("Wbh", "Watcher, close behind (Wbh)", "behind", BEHIND_RADIUS,
                    "Stands directly behind you",
                    "Two copies. It gets as close behind you as it can and stays there, "
                            + "silently, all night. It is the last step of the progression and "
                            + "the one people ask for."));

    private final List<Allele> alleles;
    private final Allele n;
    private final List<Expression> expressions;
    private final Expression WILD;
    private final List<Expression> byVariant = new ArrayList<>();
    private final FounderTable founders;

    public MagicNightWatchGene() {
        List<Allele> built = new ArrayList<>();
        for (int i = 0; i < VARIANTS.size(); i++) {
            Variant v = VARIANTS.get(i);
            built.add(new Allele(KEY, i, v.token(), v.label()));
        }
        this.n = new Allele(KEY, VARIANTS.size(), "n", "Wild-type (n)");
        built.add(n);
        this.alleles = List.copyOf(built);

        this.WILD = Expression.wildType(
                "An ordinary horse. This is every combination but a matched pair, including a "
                        + "horse carrying two DIFFERENT watchers - it shows neither, because the "
                        + "five modes are five answers to one question about where to stand.");
        List<Expression> all = new ArrayList<>();
        all.add(WILD);
        for (Variant v : VARIANTS) {
            Expression e = Expression.wildType("watch-" + v.mode(), v.name(), v.description());
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
        this.founders = table.build();
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
    @Override public String name() { return "Magic night watch"; }
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
        // silentSteps is true on every variant - see the class note. It is a
        // field rather than a constant so the format can express a loud one.
        return List.of(new GeneAbility.NightWatch(v.mode(), v.radius(), true,
                GeneAbility.Condition.ALWAYS, 1));
    }
}
