package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.BarebackSteeringHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>A saddle alone does not hand over the reins</b> (#74).
 *
 * <p>Vanilla's {@code AbstractHorse.getControllingPassenger()} gives control to a
 * player passenger whenever {@code isSaddled()}. {@code BarebackSteeringHandler}
 * can lend a saddle to add a steerer, but it cannot take one away from a real
 * saddle, so a saddled horse whose rider {@code Reins} calls a passenger needs
 * this: no controlling passenger, so the horse's own goals keep the MOVE flag
 * ({@code Mob.updateControlFlags}) and carry the rider where it likes.
 *
 * <p>The server decides and sets {@code ModAttachments.REINS_WITHHELD}, which is
 * synced, and this reads only that flag - so the rider's client, which decides
 * for itself whether it is driving, agrees with the server without knowing the
 * bond tier or whether the horse is bolting.
 *
 * <p>Only a {@link Player} first passenger is refused: {@code CartHorseMixin}
 * answers the same call for a postilion at HEAD, and a cart's driver is never a
 * player on the horse's back. Horses only, as the flag is only ever set on one.
 *
 * <p><b>UNVERIFIED at runtime:</b> written against the 26.1.2 sources. The first
 * tick or two after mounting, before the flag reaches the client, the client may
 * believe it drives; the server ignores that client's vehicle moves
 * ({@code handleMoveVehicle} requires the controlling passenger) and its position
 * updates put the horse back. Checked on horse-gear's Verification tab.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseReinsMixin {

    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$withholdReins(CallbackInfoReturnable<LivingEntity> cir) {
        if ((Object) this instanceof Horse horse && horse.getFirstPassenger() instanceof Player
                && BarebackSteeringHandler.reinsWithheld(horse)) {
            cir.setReturnValue(null);
        }
    }
}
