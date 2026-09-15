package com.example.horsegenetics.neoforge.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>A horse rearing in a fight keeps fighting.</b>
 *
 * <p>{@code HorseMeleeGoal} rears the horse after every blow, because a horse has no attack
 * animation and the rear is what reads as a strike (gap 213). But in 26.1.2
 * {@code AbstractHorse.isImmobile()} is {@code ... || isEating() || isStanding()}, a rear is
 * {@code setStanding(20)}, and {@code LivingEntity.aiStep} skips {@code serverAiStep} entirely
 * while a mob is immobile (read in the decompiled source and in {@code javap}). So each blow froze
 * the melee goal, its ten-tick cooldown and its pathing for twenty ticks: the yard's KICK GLADIATOR
 * pen swung at ticks 84 and 143 while two husks hit it eight times and killed it (gap 222).
 *
 * <p>Owner, 2026-09-15: rear, and keep fighting. While a {@link Horse} is standing <i>and has a live
 * target</i>, the rear alone no longer makes it immobile. Every other reason is left exactly as
 * vanilla has it: an eating horse, a saddled horse with a rider, and a horse that rears with nothing
 * to fight (spooked, or thrown a rider) still stand still. Donkeys, mules and llamas are untouched.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseRearFightMixin {

    @Inject(method = "isImmobile", at = @At("RETURN"), cancellable = true)
    private void horsegenetics$rearKeepsFighting(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        AbstractHorse self = (AbstractHorse) (Object) this;
        if (!(self instanceof Horse) || !self.isStanding() || self.isEating() || self.isVehicle()) {
            return;
        }
        LivingEntity target = self.getTarget();
        if (target != null && target.isAlive()) {
            cir.setReturnValue(false);
        }
    }
}
