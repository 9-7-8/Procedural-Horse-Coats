package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.common.genetics.Undeath;
import com.example.horsegenetics.neoforge.server.UndeadHorses;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.equine.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>An undead horse sounds like one</b> (undead treatment D11): a horse homozygous
 * for the skeleton or zombie allele takes vanilla's skeleton or zombie horse
 * ambient, hurt and death sounds, and the zombie its angry and eating ones too -
 * exactly the five {@code ZombieHorse} overrides and the three {@code SkeletonHorse}
 * ones, read off 26.1.2 with javap (2026-10-02). Everything else stays a horse's.
 *
 * <p>A mixin rather than the {@code sound} effect verb, which plays a sound on a
 * trigger beside the horse's own and could never replace its voice.
 *
 * <p>The answer comes off the horse's record; on a client that has not had the
 * record yet it is a horse's voice. UNVERIFIED at runtime which side plays each of
 * these in 26.1.2 - both read the same record, so either way it should agree.
 */
@Mixin(Horse.class)
public abstract class HorseUndeadVoiceMixin {

    @Inject(method = "getAmbientSound", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$undeadAmbient(CallbackInfoReturnable<SoundEvent> cir) {
        switch (UndeadHorses.kindOf((Horse) (Object) this)) {
            case SKELETON -> cir.setReturnValue(SoundEvents.SKELETON_HORSE_AMBIENT);
            case ZOMBIE -> cir.setReturnValue(SoundEvents.ZOMBIE_HORSE_AMBIENT);
            default -> { }
        }
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$undeadHurt(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        switch (UndeadHorses.kindOf((Horse) (Object) this)) {
            case SKELETON -> cir.setReturnValue(SoundEvents.SKELETON_HORSE_HURT);
            case ZOMBIE -> cir.setReturnValue(SoundEvents.ZOMBIE_HORSE_HURT);
            default -> { }
        }
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$undeadDeath(CallbackInfoReturnable<SoundEvent> cir) {
        switch (UndeadHorses.kindOf((Horse) (Object) this)) {
            case SKELETON -> cir.setReturnValue(SoundEvents.SKELETON_HORSE_DEATH);
            case ZOMBIE -> cir.setReturnValue(SoundEvents.ZOMBIE_HORSE_DEATH);
            default -> { }
        }
    }

    @Inject(method = "getAngrySound", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$undeadAngry(CallbackInfoReturnable<SoundEvent> cir) {
        if (UndeadHorses.kindOf((Horse) (Object) this) == Undeath.Kind.ZOMBIE) {
            cir.setReturnValue(SoundEvents.ZOMBIE_HORSE_ANGRY);
        }
    }

    @Inject(method = "getEatingSound", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$undeadEating(CallbackInfoReturnable<SoundEvent> cir) {
        if (UndeadHorses.kindOf((Horse) (Object) this) == Undeath.Kind.ZOMBIE) {
            cir.setReturnValue(SoundEvents.ZOMBIE_HORSE_EAT);
        }
    }
}
