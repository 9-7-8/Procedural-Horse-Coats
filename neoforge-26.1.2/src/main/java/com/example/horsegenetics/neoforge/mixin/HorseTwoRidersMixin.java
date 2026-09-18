package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.HorseDraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>A strong horse carries two.</b>
 *
 * <p>Vanilla allows exactly one passenger on a horse, decided in
 * {@code canAddPassenger}. A horse whose pulling ability is
 * {@link com.example.horsegenetics.common.cart.CartDraft#TWO_RIDER_PULL} or
 * better takes a second - one point above an ordinary horse, so it is a real
 * breeding goal but not a draft-only one, and a wild-caught horse usually will
 * not manage it.
 *
 * <h2>Why this hangs off the pull score</h2>
 * Carrying and hauling are the same physical claim about an animal, and the mod
 * already has a stat for it. Inventing a second "carrying capacity" gene would
 * have split one idea across two numbers a player has to breed for separately.
 *
 * <h2>The second seat</h2>
 * {@code getPassengerAttachmentPoint} is vanilla's answer to "where does rider
 * <i>n</i> sit", and it answers for the saddle regardless of <i>n</i> - it has
 * never had to place a second. The second rider is moved back along the horse's
 * own body axis so the two are not inside one another. The offset is in the
 * entity's local space, which is why it is rotated by the body yaw rather than
 * simply subtracted from Z.
 *
 * <h2>Interaction with the carts</h2>
 * The wagon's box seat puts a hidden {@code PostilionEntity} on the horse as a
 * passenger so a wagon passenger can drive. That postilion occupies a seat, so
 * before this a driven wagon meant nobody could also ride the horse. With two
 * seats on a strong horse, a driver on the wagon and a rider on the horse now
 * coexist - which is the arrangement that makes a carriage read as a carriage.
 *
 * <p><b>Unverified:</b> nothing here has been ridden in-game yet. The seating
 * offset in particular is a number picked to look right on vanilla's horse
 * model, not one measured - see wiki/verification.html.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseTwoRidersMixin {

    /** How far back the second rider sits, in blocks, along the horse's spine. */
    private static final float HORSEGENETICS$PILLION_OFFSET = 0.55F;

    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
    private void horsegenetics$allowPillion(final Entity passenger, final CallbackInfoReturnable<Boolean> cir) {
        final AbstractHorse self = (AbstractHorse) (Object) this;
        if (self.getPassengers().size() == 1 && HorseDraft.carriesTwoRiders(self)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    @Inject(method = "getPassengerAttachmentPoint", at = @At("RETURN"), cancellable = true)
    private void horsegenetics$seatPillion(final Entity passenger, final EntityDimensions dimensions,
                                           final float partialTick, final CallbackInfoReturnable<Vec3> cir) {
        final AbstractHorse self = (AbstractHorse) (Object) this;
        if (self.getPassengers().indexOf(passenger) != 1) {
            return;
        }
        final Vec3 saddle = cir.getReturnValue();
        if (saddle == null) {
            return;
        }
        final float yaw = self.yBodyRot * ((float) Math.PI / 180F);
        cir.setReturnValue(saddle.add(
                Math.sin(yaw) * HORSEGENETICS$PILLION_OFFSET,
                0.0D,
                -Math.cos(yaw) * HORSEGENETICS$PILLION_OFFSET));
    }
}
