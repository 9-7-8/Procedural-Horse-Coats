package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.List;

/**
 * <b>Echolocate</b> ({@code horsegenetics.echolocate}) - the horse makes a sound
 * no horse should make, and for a moment you can see everything alive around
 * you.
 *
 * <h2>Everything alive, which is what the cap pays for</h2>
 * Hostiles-only was the cheaper and quieter design and was rejected - finding
 * lost animals is half the appeal. That decision moves the whole burden onto
 * {@link #MAX_TARGETS}, which is therefore not a tuning knob.
 *
 * <p>The cost of this gene is <b>packets, not ticks</b>. Glowing syncs to every
 * client tracking the entity it lands on, so the refresh is long and the effect
 * is left to lapse rather than re-stamped on every beat - which is exactly the
 * cost that does not show up when testing with one horse in a field.
 *
 * <h2>A gene that changes what the player can see</h2>
 * Almost every locus in the mod changes the horse. This one changes the
 * <i>rider's</i> information, which is a category the mod has barely touched.
 */
public final class EcholocateGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.echolocate";
    public static final int PRIORITY = 166;

    public static final String RADIUS = "radius";
    public static final double MIN_RADIUS = 8.0;
    public static final double MAX_RADIUS = 20.0;

    public static final String SOUND = "minecraft:entity.bat.ambient";
    public static final String EFFECT = "minecraft:glowing";

    /** Long on purpose. The cost of this gene is packets, and this is the knob that sets them. */
    public static final int REFRESH_TICKS = 120;

    /** A packet cap rather than a tick cap - see the class note. */
    public static final int MAX_TARGETS = 10;

    public EcholocateGene() {
        super(KEY, PRIORITY, "Echolocate",
                "Ech", "Echoing (Ech)",
                Dominance.RECESSIVE, Founders.EXPRESSING, 1.0,
                "The horse tells you nothing you could not see for yourself.",
                "Echoing",
                "Two copies. Every so often the horse makes a bat\u2019s cry, and everything living within a radius written on the allele copy is outlined - through walls, through the dark. It shows you the skeleton round the corner and the cow you lost, without distinguishing between them.");
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(RADIUS, MIN_RADIUS, MAX_RADIUS));
    }

    /**
     * <b>Tamed, and carrying somebody.</b>
     *
     * <p>It was {@link GeneAbility.Condition#ALWAYS}, which made every
     * echolocating horse in the world - wild ones included - light up
     * everything within up to twenty blocks, every six seconds, for ever.
     * Owner, 2026-09-13: <i>"the echolocate gene is still activating. We need
     * to tune it. It should do NOTHING on untamed horses. On a tame horse, it
     * normally only activates while ridden."</i>
     *
     * <p>Which is the right shape for what the ability <em>is</em>. Echolocation
     * is a thing the horse does <b>for its rider</b> - the outline is drawn on
     * the rider's screen, and a wild horse doing it in an empty field is
     * painting for nobody while still paying the whole cost: a ten-target
     * entity scan and an effect application per horse per six seconds, which is
     * <a href="known-gaps.html">exactly the sort of cost that does not show up
     * when testing with one horse in a field</a> and does show up in a stable.
     *
     * <p>Built from the two flags that already exist rather than a new one, so
     * this costs the {@code AbilityType} contract nothing.
     */
    private static final GeneAbility.Condition RIDDEN_AND_TAMED =
            new GeneAbility.Condition.All(List.of(
                    new GeneAbility.Condition.Flag("tamed", false),
                    new GeneAbility.Condition.Flag("has_rider", false)));

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(
                new GeneAbility.Sound(SOUND, new GeneAbility.Trigger.Interval(REFRESH_TICKS),
                        0.6, 1.0, 0, REFRESH_TICKS, RIDDEN_AND_TAMED, 1),
                new GeneAbility.SelfEffect(EFFECT, "group", "all", epi.get(RADIUS), 0,
                        REFRESH_TICKS, MAX_TARGETS, RIDDEN_AND_TAMED, 1));
    }
}
