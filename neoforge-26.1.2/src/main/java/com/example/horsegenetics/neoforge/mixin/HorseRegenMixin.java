package com.example.horsegenetics.neoforge.mixin;

import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * <b>No passive regeneration a horse did not pay for.</b>
 *
 * <p>{@code AbstractHorse.aiStep} heals every equine one point on a 1-in-900 roll each server
 * tick, whatever its state - no health check, no gate. Read in 26.1.2's patched sources,
 * and it is the only {@code heal} call in that method: {@code javap} shows it as
 * {@code invokevirtual AbstractHorse.heal(F)V}. With hunger (owner, 2026-09-14: a starving
 * horse "cant heal"), a free heal about once every 45 seconds would make the rule a
 * suggestion. The owner chose to switch it off. It is also the likeliest source of the
 * "heals on horses already at full health" the yard census has counted for weeks
 * ({@code known-gaps.html} gap 217).
 *
 * <p>Only for a {@link Horse}. Donkeys, mules, llamas and the undead horses carry no
 * hunger, and keep vanilla's heal exactly. Hand-feeding heals come from
 * {@code handleEating}, a different method, and are untouched.
 *
 * <p>UNVERIFIED at runtime: that this {@link Redirect} applies in the dev and production
 * environments - the existing mixin config's refmap warning ("could not be read") is
 * harmless in dev, and 26.1.2 runs on Mojang names, but no redirect has been booted here
 * before. {@code defaultRequire: 1} means a miss fails loudly at mod load, not silently.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseRegenMixin {

    @Redirect(method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;heal(F)V"))
    private void horsegenetics$noFreeHeal(AbstractHorse self, float amount) {
        if (!(self instanceof Horse)) {
            self.heal(amount);
        }
    }
}
