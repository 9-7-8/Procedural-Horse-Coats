package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Leader of the pack</b> ({@code horsegenetics.pack_leader}) - something
 * follows this horse. Which something depends on the allele, and there is one
 * for every non-hostile mob in the game.
 *
 * <h2>A third mob_aura mode, not a new verb</h2>
 * {@code mob_aura} already had {@code repel} and {@code attract}; following is
 * {@code follow}, on the same verb, with the same cap and the same beat. A
 * {@code summon} verb was specified for this gene early in design and folded
 * away once it became clear that {@link SpawnerGene} was the only locus that
 * genuinely needed one.
 *
 * <h2>The cost is pathfinding, and it is per follower</h2>
 * Every follower recalculates a path on its own schedule, and pathfinding is the
 * most expensive thing a mob does. The long {@link #INTERVAL_TICKS} and the low
 * {@link #MAX_TARGETS} are not tuning - they are the design. The translator
 * nudges navigation per beat rather than injecting a goal into another mod's
 * mob, which is stateless and therefore cannot be left behind on a creature when
 * the horse dies.
 *
 * <h2>The allele set is {@link MobRoster}'s</h2>
 * Every non-hostile vanilla mob minus the horse family - the same list
 * {@link LycanGene} uses, from one table rather than two that drift. Lycan turns
 * the horse into the mob; this makes the mob follow the horse, and the same list
 * serving two opposite ideas is a good sign it was derived from the right rule.
 *
 * <p>Plus {@link MobRoster#CHAOS_TOKEN Chaos}: one allele for every modded mob,
 * a seed on the copy choosing which. The game module only offers it the
 * <b>peaceful</b> ones - not a monster spawn category, and not hostile on
 * inspection - since a horse leading a retinue of somebody's modded monsters is
 * the Spawner's job, not this one's.
 */
public final class PackLeaderGene extends AbstractMatchedPairGene {

    public static final String KEY = "horsegenetics.pack_leader";
    public static final int PRIORITY = 181;

    /** Blocks. Sixteen, which is close enough to read as a retinue and far enough to be useful. */
    public static final double RADIUS = 16.0;

    /** Ticks between beats. Long: every follower pathfinds, and that is the whole cost of the gene. */
    public static final int INTERVAL_TICKS = 40;

    /** The cap every radius effect must state, and here the thing that bounds the pathfinding. */
    public static final int MAX_TARGETS = 6;

    /** Carriers in a hundred, spread across every variant. Generous, because a match is the hard part. */
    public static final double WILD_CARRIER_PERCENT = 22.0;

    public PackLeaderGene() {
        super(KEY, PRIORITY, "Leader of the pack",
                subjects(), WILD_CARRIER_PERCENT,
                "Nothing pays the horse any particular attention.",
                "One copy, and it does nothing at all. This horse carries a following allele and "
                        + "shows no sign of it - the only way to know is to test it.",
                "Two different following alleles. The horse settles on neither and nothing "
                        + "follows it, which is most crosses between two carriers and is the "
                        + "reason the locus is a search rather than a slot machine.",
                new MatchedText() {
                    @Override public String name(Variant v) {
                        if (MobRoster.isChaos(v.subject())) {
                            return "Leads an animal from another mod";
                        }
                        return "Leads " + v.label().toLowerCase() + "s that are already nearby";
                    }

                    @Override public String description(Variant v) {
                        if (MobRoster.isChaos(v.subject())) {
                            return "Two Chaos copies. One kind of peaceful animal from another mod "
                                    + "trails the horse - which kind is a seed on the copies, worked "
                                    + "out from the mods this world has loaded, so it can change "
                                    + "with the modpack. Both copies must carry the same seed: a "
                                    + "horse caught or made fresh always does, two Chaos lines "
                                    + "crossed never do. Like every allele here it creates nothing, "
                                    + "and with no peaceful modded animal installed it does nothing "
                                    + "at all.";
                        }
                        return "Two matching copies. Every " + v.label().toLowerCase() + " within "
                                + "sixteen blocks trails the horse wherever it goes, and gives up "
                                + "when it gets too far. IT DOES NOT CREATE THEM: take this horse "
                                + "somewhere with no " + v.label().toLowerCase() + "s and nothing "
                                + "happens, which is the single most likely reason to think the "
                                + "gene is broken when it is working. It is a retinue rather than "
                                + "a command - "
                                + "they are not tamed, not owned, and will still wander off if "
                                + "something more interesting happens.";
                    }
                });
    }

    private static List<Variant0> subjects() {
        List<Variant0> out = new ArrayList<>();
        for (MobRoster.Entry e : MobRoster.peaceful()) {
            out.add(new Variant0(e.token(), e.mob(), e.label()));
        }
        out.add(chaosVariant()); // every modded mob, after the last vanilla one - see MobRoster
        return out;
    }

    @Override
    protected String idPrefix() {
        return "pack";
    }

    /** Nothing but the Chaos seed - every vanilla allele here is the same on every horse. */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.seed(MobRoster.CHAOS_SEED));
    }

    @Override
    protected List<GeneAbility> abilitiesFor(Variant v, EpiValues epi) {
        return List.of(new GeneAbility.MobAura("follow", "all", v.subject(), RADIUS,
                INTERVAL_TICKS, MAX_TARGETS, GeneAbility.Condition.ALWAYS, 1));
    }
}
