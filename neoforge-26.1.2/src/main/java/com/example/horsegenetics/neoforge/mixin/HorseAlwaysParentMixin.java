package com.example.horsegenetics.neoforge.mixin;

import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>Two of vanilla's breeding conditions, dropped.</b> The owner's rule is that
 * a golden carrot always works and the only limits are age and sex; vanilla
 * {@code AbstractHorse.canParent} imposes four more:
 *
 * <pre>
 * !isVehicle() &amp;&amp; !isPassenger() &amp;&amp; isTamed() &amp;&amp; !isBaby()
 *     &amp;&amp; getHealth() &gt;= getMaxHealth() &amp;&amp; isInLove()
 * </pre>
 *
 * <p>This drops <b>{@code isTamed()}</b> and <b>{@code getHealth() &gt;=
 * getMaxHealth()}</b> and keeps the rest. The health one is the invisible
 * offender: a horse one heart down from a fall reads as perfectly ordinary,
 * eats the carrot, shows hearts, and then simply does not breed - there is no
 * message anywhere in vanilla for it, and the mod cannot add one to a condition
 * it does not evaluate. The taming one goes because "adults, opposite sex" says
 * nothing about taming, and a wild pair is the shortest route to a foal in a
 * test world. {@code BreedingCooldownHandler} does the other half of the wild
 * case - vanilla will not put an untamed horse in love in the first place.
 *
 * <p>The three kept are not conditions on the animal: being ridden or riding is
 * transient and breeding mid-ride is nonsense, {@code isBaby} is the age limit
 * the owner named, and {@code isInLove} <i>is</i> the golden carrot.
 *
 * <h2>Why it is guarded to {@link Horse}</h2>
 * {@code canParent} is declared on {@code AbstractHorse}, so a mixin on it also
 * reaches donkeys, mules, llamas, trader llamas, skeleton and zombie horses.
 * This mod is about horses, and {@code HorseBreedingHandler} refuses any
 * pairing that is not two of them, so relaxing the rule for a llama would
 * change vanilla behaviour this mod has no opinion on. The instance check is the
 * cheap way to say that.
 *
 * <p>Sex is not tested here and must not be: vanilla has no notion of it, and
 * the mod's own check is at {@code BabyEntitySpawnEvent}, where it can name both
 * horses in the refusal. Answering "cannot parent" here would show the player
 * nothing at all.
 *
 * <p><b>UNVERIFIED at runtime.</b> Written against the 26.1.2 sources, where
 * {@code canParent} is {@code protected boolean} on {@code AbstractHorse} and is
 * called only from the {@code canMate} overrides.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseAlwaysParentMixin {

    @Inject(method = "canParent", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$alwaysParent(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Horse horse)) {
            return;
        }
        cir.setReturnValue(!horse.isVehicle() && !horse.isPassenger()
                && !horse.isBaby() && horse.isInLove());
    }
}
