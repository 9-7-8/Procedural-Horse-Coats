package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.server.HorseDraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>A strong horse carries two.</b>
 *
 * <p>Vanilla allows exactly one passenger on anything, decided in
 * {@code Entity.canAddPassenger}. A horse whose pulling ability is
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
 * <h2>Why {@code canAddPassenger} is an override and not an {@code @Inject}</h2>
 * It was an {@code @Inject} first, and the server would not boot:
 *
 * <pre>Critical injection failure: @Inject annotation on horsegenetics$allowPillion
 * could not find any targets matching 'canAddPassenger' in AbstractHorse</pre>
 *
 * <p><b>{@code AbstractHorse} does not declare it.</b> The method exists only on
 * {@code Entity}, and a mixin injector can only target a method the target
 * class actually has - inheritance does not count. The alternatives were to
 * mixin into {@code Entity}, the hottest class in the game and one this repo's
 * access-transformer file already argues against touching, or to <i>add</i> the
 * override to {@code AbstractHorse} from here. This is the second, and it is
 * the same move {@code CartHorseMixin} makes for {@code maxUpStep}.
 *
 * <p>There is no {@code super} call because there is nothing worth calling:
 * vanilla's whole implementation is {@code passengers.isEmpty()}, i.e. "fewer
 * than one", and the line below is that generalised to "fewer than one, or two
 * if this horse is strong enough". Reimplementing one expression beats a
 * cross-hierarchy super call that mixin has to rewrite.
 *
 * <h2>The second seat</h2>
 * {@code getPassengerAttachmentPoint} <i>is</i> declared on {@code AbstractHorse}
 * (it adds the rearing offset), so that half stays an {@code @Inject}. Vanilla
 * answers for the saddle regardless of which passenger is asking - it has never
 * had to place a second - so the pillion is moved back along the horse's own
 * body axis. The offset is rotated by the body yaw rather than subtracted from
 * Z because it is in world space by the time it is returned.
 *
 * <h2>Interaction with the carts</h2>
 * The wagon's box seat puts a hidden {@code PostilionEntity} on the horse as a
 * passenger so a wagon passenger can drive. That postilion occupies a seat, so
 * before this a driven wagon meant nobody could also ride the horse. With two
 * seats on a strong horse, a driver on the wagon and a rider on the horse now
 * coexist - which is the arrangement that makes a carriage read as a carriage.
 *
 * <p><b>Unverified:</b> the threshold works (the server boots and the method is
 * reached), but nothing here has been ridden in-game. The seating offset in
 * particular is a number picked to look right on vanilla's horse model, not one
 * measured - see wiki/verification.html.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseTwoRidersMixin extends LivingEntity {

    /** How far back the second rider sits, in blocks, along the horse's spine. */
    private static final double HORSEGENETICS$PILLION_OFFSET = 0.55;

    protected HorseTwoRidersMixin(final EntityType<? extends @NotNull LivingEntity> entityType, final Level level) {
        super(entityType, level);
    }

    /**
     * Vanilla is {@code this.passengers.isEmpty()}. This is the same rule with
     * the limit read off the horse.
     */
    @Override
    protected boolean canAddPassenger(final @NotNull Entity passenger) {
        final int seats = HorseDraft.carriesTwoRiders((AbstractHorse) (Object) this) ? 2 : 1;
        return this.getPassengers().size() < seats;
    }

    @Inject(method = "getPassengerAttachmentPoint", at = @At("RETURN"), cancellable = true)
    private void horsegenetics$seatPillion(final Entity passenger, final EntityDimensions dimensions,
                                           final float scale, final CallbackInfoReturnable<Vec3> cir) {
        if (this.getPassengers().indexOf(passenger) != 1) {
            return;
        }
        final Vec3 saddle = cir.getReturnValue();
        if (saddle == null) {
            return;
        }
        final double yaw = this.yBodyRot * (Math.PI / 180.0);
        cir.setReturnValue(saddle.add(
                Math.sin(yaw) * HORSEGENETICS$PILLION_OFFSET,
                0.0,
                -Math.cos(yaw) * HORSEGENETICS$PILLION_OFFSET));
    }
}
