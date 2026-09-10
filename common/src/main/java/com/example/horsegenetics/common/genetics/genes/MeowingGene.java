package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.List;

/**
 * <b>Meowing</b> ({@code horsegenetics.meowing}) - the horse meows, and creepers
 * keep their distance.
 *
 * <h2>Silly and useful, which is the good combination</h2>
 * The gene exists because a meowing horse is funny. It survived design review
 * because vanilla already establishes that creepers fear cats, so the joke has a
 * rule behind it - and because the effect is genuinely worth breeding: creepers
 * are the mob that costs you a horse, and a meowing horse left in a pasture is
 * one that is still there in the morning.
 *
 * <h2>Two verbs, both built for other genes</h2>
 * The sound and the filtered repel are both machinery
 * {@link SingerGene}, {@link BaseAlarmGene} and {@link IntimidatingGene} needed
 * anyway, so this locus adds nothing of its own - which makes it a good gene to
 * build last in a batch, when it is free.
 */
public final class MeowingGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.meowing";
    public static final int PRIORITY = 163;

    public static final String RADIUS = "radius";
    public static final double MIN_RADIUS = 8.0;
    public static final double MAX_RADIUS = 16.0;

    /** The cat, borrowed wholesale. */
    public static final String SOUND = "minecraft:entity.cat.ambient";

    /** Ticks between meows, and the cooldown under it. Sound genes fail by spam, not by tick cost. */
    public static final int MEOW_INTERVAL_TICKS = 300;
    public static final int MEOW_COOLDOWN_TICKS = 200;

    public static final int INTERVAL_TICKS = 20;
    public static final int MAX_TARGETS = 8;

    /** The one mob that matters here. */
    public static final String CREEPER = "minecraft:creeper";

    public MeowingGene() {
        super(KEY, PRIORITY, "Meowing",
                "Mew", "Meowing (Mew)",
                Dominance.RECESSIVE, Founders.EXPRESSING, 1.4,
                "The horse sounds like a horse, and creepers treat it like one.",
                "Meowing",
                "Two copies. The horse meows now and then - not often, not loudly, and with no explanation available anywhere in its genome - and creepers stay outside a radius written on the allele copy.");
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(RADIUS, MIN_RADIUS, MAX_RADIUS));
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(
                new GeneAbility.Sound(SOUND, new GeneAbility.Trigger.Interval(MEOW_INTERVAL_TICKS),
                        1.0, 1.0, 0, MEOW_COOLDOWN_TICKS, GeneAbility.Condition.ALWAYS, 1),
                new GeneAbility.MobAura("repel", "hostile", CREEPER, epi.get(RADIUS),
                        INTERVAL_TICKS, MAX_TARGETS, GeneAbility.Condition.ALWAYS, 1));
    }
}
