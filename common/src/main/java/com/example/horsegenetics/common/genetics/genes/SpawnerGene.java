package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Spawner</b> ({@code horsegenetics.spawner}) - feed the horse, and two of a
 * particular creature appear nearby.
 *
 * <h2>Fed, not timed</h2>
 * It used to fire once a Minecraft day, and in play it never fired at all: the
 * day was counted on the horse's {@code tickCount}, which restarts at every
 * load. The owner's call (2026-09-10) was to hand the trigger to the player
 * instead - anything the horse eats, from the hand ({@code Trigger.OnFeed}). A
 * spawner you can work beats one you wait for, and a horse left alone in a
 * field now does nothing at all.
 *
 * <h2>Two per meal, and no cap</h2>
 * On its old timer it counted what was already there and only topped up to two,
 * because a spawning gene that counts <i>time</i> and not <i>population</i> is a
 * lag machine. Fed, the player pays for every firing with food, so the owner
 * took the cap off (2026-09-10): each meal makes {@link #PER_FEEDING}, full stop,
 * and the cost is the lever - "I'll up the cost for it later". Only a meal the
 * horse actually <i>eats</i> counts; the translator checks, because a horse at
 * full health hands the wheat back and an uncapped trigger on a free click would
 * be an infinite spawner.
 *
 * <h2>It reaches the monsters, and only by being bred</h2>
 * The allele set is {@link MobRoster#all()} - every mob, hostile ones included,
 * which is a deliberate departure from {@link LycanGene}'s non-hostile rule. A
 * horse that produces two creepers every time it is fed is a real thing this locus can
 * express, and it is never something you will catch: like every locus on this
 * base, only carriers are born wild, so somebody has to make it.
 *
 * <h2>A locus almost nobody will find by accident</h2>
 * With no expressing founders and this many alleles, a player must find two
 * carriers of the <b>same</b> allele, invisibly. That is deliberate - it is the
 * clearest argument the gene database has for existing - but it does mean the
 * first spawner in a world is a project rather than a discovery.
 *
 * <p>Three requirements land on the translator rather than here, and each is on
 * the gene's page as a hazard: spawn through the normal path so other mods'
 * protections still fire, never mark what is spawned persistent, and only
 * count a feeding that was eaten.
 */
public final class SpawnerGene extends AbstractMatchedPairGene {

    public static final String KEY = "horsegenetics.spawner";
    public static final int PRIORITY = 182;

    /** Blocks - about two chunks, which is what "nearby" was specified as. */
    public static final double RADIUS = 32.0;

    /** Creatures per meal. No cap on the total - see the class note. */
    public static final int PER_FEEDING = 2;

    /** Carriers in a hundred. The largest roster in the mod, so the rate has to be generous. */
    public static final double WILD_CARRIER_PERCENT = 20.0;

    public SpawnerGene() {
        super(KEY, PRIORITY, "Spawner",
                subjects(), WILD_CARRIER_PERCENT,
                "The horse makes nothing but more horses, and only in the usual way.",
                "One copy, and nothing happens. This horse carries a spawning allele invisibly - "
                        + "including, possibly, one you would rather it did not.",
                "Two different spawning alleles. The horse settles on neither and produces "
                        + "nothing at all.",
                new MatchedText() {
                    @Override public String name(Variant v) {
                        return "Spawns " + v.label().toLowerCase() + "s";
                    }

                    @Override public String description(Variant v) {
                        return "Two matching copies. Feed the horse anything it eats and two "
                                + v.label().toLowerCase() + "s appear nearby - every meal, as "
                                + "many times as you care to feed it.";
                    }
                });
    }

    private static List<Variant0> subjects() {
        List<Variant0> out = new ArrayList<>();
        for (MobRoster.Entry e : MobRoster.all()) {
            out.add(new Variant0(e.token(), e.mob(), e.label()));
        }
        return out;
    }

    @Override
    protected String idPrefix() {
        return "spawner";
    }

    @Override
    protected List<GeneAbility> abilitiesFor(Variant v, EpiValues epi) {
        return List.of(new GeneAbility.Summon(v.subject(), RADIUS, PER_FEEDING,
                new GeneAbility.Trigger.OnFeed(), GeneAbility.Condition.ALWAYS, 1));
    }
}
