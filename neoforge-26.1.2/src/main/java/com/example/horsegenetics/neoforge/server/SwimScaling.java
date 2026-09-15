package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.function.ToDoubleFunction;

/**
 * <b>Magic swim speed: how hard a horse pushes through water</b> (owner, 2026-09-15, gap 249).
 *
 * <p>{@code mixin.HorseSwimMixin} multiplies the push vanilla's {@code LivingEntity.travelInWater} hands to
 * {@code moveRelative} by {@link #factorOf}. Vanilla swimming is {@code v = drag * (v + push)}, so scaling the push
 * scales the steady swimming speed by exactly the factor, in either direction. A horse that is not pushing is not
 * moved: no drift. Vanilla runs {@code travelInWater} on whichever side moves the horse - the server for a loose horse,
 * the rider's client for a ridden one - so one mixin covers both.
 *
 * <p><b>Why not a velocity nudge.</b> The first version inferred each tick's push from the velocity and scaled that. The
 * yard's SWIM SPEED pen caught it amplifying noise: an Otter horse given almost no push drifted 1.06 blocks in two
 * seconds, seven times a plain horse's 0.15.
 */
public final class SwimScaling {

    private SwimScaling() {
    }

    private static final double MIN_FACTOR = 0.05;

    /**
     * The client's lookup, installed by {@code client.ClientSwimHandler} at client setup. A hook rather than a direct
     * call, so this class - which the mixin loads on a dedicated server too - never names a client-only class.
     */
    public static volatile ToDoubleFunction<Horse> clientFactor = horse -> 1.0;

    /** 1.0 for a horse without magic swim speed. Cheap enough per tick: the server side reads a cached ability list. */
    public static double factorOf(Horse horse) {
        if (horse.level().isClientSide()) {
            return clientFactor.applyAsDouble(horse);
        }
        for (HorseAbilities.Active active : GeneAbilityHandler.abilitiesOf(horse)) {
            if (active.ability() instanceof GeneAbility.Swim swim) {
                return Math.max(MIN_FACTOR, swim.factor());
            }
        }
        return 1.0;
    }
}
