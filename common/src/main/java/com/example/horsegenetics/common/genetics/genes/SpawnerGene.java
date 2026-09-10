package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Spawner</b> ({@code horsegenetics.spawner}) - at first light the horse
 * looks around, and if there are fewer than two of a particular creature nearby,
 * there are two now.
 *
 * <h2>Up to two, which is the whole design</h2>
 * Almost every design for a spawning gene is a lag machine, because almost every
 * one counts <i>time</i> and not <i>population</i>. This one counts what is
 * already there first, so a horse in a stocked field does nothing at all, for
 * ever, and only acts when something has been lost. The ceiling is in the
 * definition rather than bolted on as a cooldown.
 *
 * <h2>It reaches the monsters, and only by being bred</h2>
 * The allele set is {@link MobRoster#all()} - every mob, hostile ones included,
 * which is a deliberate departure from {@link LycanGene}'s non-hostile rule. A
 * horse that produces two creepers every dawn is a real thing this locus can
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
 * protections still fire, never mark what is spawned persistent, and count
 * before spawning.
 */
public final class SpawnerGene extends AbstractMatchedPairGene {

    public static final String KEY = "horsegenetics.spawner";
    public static final int PRIORITY = 182;

    /** Blocks - about two chunks, which is what "nearby" was specified as. */
    public static final double RADIUS = 32.0;

    /** The local population it tops up to. Not "how many to make" - see the class note. */
    public static final int UP_TO = 2;

    /** A Minecraft day. The trigger fires once per day rather than at a wall-clock sunrise. */
    public static final int DAY_TICKS = 24000;

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
                        return "Two matching copies. Once a day the horse looks around, and if "
                                + "there are fewer than two " + v.label().toLowerCase() + "s "
                                + "within about two chunks, it makes up the difference. If they "
                                + "are all still there it does nothing, which is most days.";
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
        return List.of(new GeneAbility.Summon(v.subject(), RADIUS, UP_TO,
                new GeneAbility.Trigger.Interval(DAY_TICKS), GeneAbility.Condition.ALWAYS, 1));
    }
}
