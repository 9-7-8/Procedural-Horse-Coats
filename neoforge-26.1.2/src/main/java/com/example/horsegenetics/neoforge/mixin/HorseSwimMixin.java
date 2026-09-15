package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.SwimScaling;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * <b>Magic swim speed scales a horse's push through water</b> (gap 249).
 *
 * <p>In 26.1.2, {@code LivingEntity.travelInWater} works out a push strength - 0.02, raised by water movement
 * efficiency - and hands it to {@code moveRelative(float, Vec3)}, which is the only call of that method in the body (read
 * in {@code javap}). Multiplying that one argument by the horse's swim factor scales its own swimming and nothing else:
 * no drag, no current, no drift when it is not trying to move. See {@link SwimScaling}.
 *
 * <p>UNVERIFIED at runtime until the yard's SWIM SPEED pen reads it; {@code defaultRequire: 1} makes a missed target
 * fail loudly at load.
 */
@Mixin(LivingEntity.class)
public abstract class HorseSwimMixin {

    @ModifyArg(method = "travelInWater",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"),
            index = 0)
    private float horsegenetics$swimPush(float push) {
        if ((Object) this instanceof Horse horse) {
            return (float) (push * SwimScaling.factorOf(horse));
        }
        return push;
    }
}
