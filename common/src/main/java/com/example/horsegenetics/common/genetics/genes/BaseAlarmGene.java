package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Base alarm</b> ({@code horsegenetics.base_alarm}) - the horse whinnies when
 * something hostile comes near.
 *
 * <h2>Near the horse, because there is no "base"</h2>
 * The original specification said "your base radius". No such concept exists in
 * this mod - there are stall signs, the barn and pens, and nothing that means
 * home. Measuring from the horse was chosen over measuring from a stall sign,
 * which would silently do nothing for any horse that has not got one. It also
 * turns stabling into the interface: where you put the horse <i>is</i> the
 * configuration.
 *
 * <h2>Dominant on purpose</h2>
 * Most of the behaviour family is recessive and rare. An alarm is only useful if
 * you happen to have one, so this is among the few that should turn up without
 * being hunted - a common, mildly useful gene that makes an ordinary stable
 * slightly safer. Which also makes the cooldown matter more than usual: several
 * alarm horses in one paddock all noticing the same skeleton is the normal case.
 */
public final class BaseAlarmGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.base_alarm";
    public static final int PRIORITY = 167;

    public static final String SOUND = "minecraft:entity.horse.angry";

    /**
     * How often the condition is even looked at, and the cooldown under it.
     * Dominant inheritance makes the cooldown matter more than usual: several
     * alarm horses noticing one skeleton is the normal case, not the edge case.
     */
    public static final int CHECK_TICKS = 40;
    public static final int COOLDOWN_TICKS = 200;

    public BaseAlarmGene() {
        super(KEY, PRIORITY, "Base alarm",
                "Alm", "Alarm (Alm)",
                Dominance.DOMINANT, Founders.EXPRESSING, 4.0,
                "The horse has nothing in particular to say about monsters.",
                "Alarm",
                "One copy is enough. When something hostile comes within about sixteen blocks of the horse, it whinnies. It does nothing else about it - this announces the danger and leaves it entirely to you.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        // hostile_near is a sampled flag - an entity scan the translator runs on
        // an interval and caches, rather than once per ability per tick.
        return List.of(new GeneAbility.Sound(SOUND,
                new GeneAbility.Trigger.Interval(CHECK_TICKS), 1.2, 1.0, 0, COOLDOWN_TICKS,
                new GeneAbility.Condition.Flag("hostile_near", false), 1));
    }
}
