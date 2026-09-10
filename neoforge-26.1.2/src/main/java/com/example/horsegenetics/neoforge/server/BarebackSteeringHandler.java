package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * <b>A horse that trusts you enough to be steered without a saddle.</b>
 *
 * <p>At the top bond tier ({@link HorseCareAttachment#behaviourTier()} 3, which
 * is where the horse already follows you around) its owner can climb on bare
 * and ride it. Below that tier, and for anyone who is not the owner, a bareback
 * rider is a passenger and the horse goes where it likes - which is vanilla's
 * behaviour and stays the default.
 *
 * <h2>Why this is worth a whole handler</h2>
 * The obvious implementation does not exist. Vanilla decides who may steer in
 * {@code AbstractHorse.getControllingPassenger()}, which hands control to a
 * player passenger <b>only if the horse is saddled</b>, and there is no event
 * or hook on that call - so from outside the class there is no way to say "this
 * one, too". This module has no mixins and is not adding one for a courtesy.
 *
 * <p>So the horse is driven from the outside instead, on the entity tick, the
 * same way {@link HorseWaterRidingHandler} paddles a swimming one: read the
 * rider's own {@code zza}/{@code xxa}, point the horse where the rider is
 * looking, and push it. That is enough for it to be a mount. It deliberately
 * is <b>not</b> a full re-implementation - no jump, no sprint, no rearing -
 * because those are exactly the parts a saddle and a bridle are for.
 *
 * <h2>What a saddle is still for</h2>
 * Speed and control. Bareback runs at {@link #BAREBACK_FRACTION} of the horse's
 * own speed attribute and cannot jump, so a saddle remains the right answer for
 * going anywhere in particular. What this buys is the thing a saddle cannot:
 * getting on a horse that simply lets you, because you have spent the time.
 *
 * <p><b>Unverified:</b> {@code isLocalInstanceAuthoritative()} is true on the
 * server here rather than on the rider's client, because the rider is not the
 * <i>controlling</i> passenger - so this is server-driven and will feel a beat
 * behind a saddled horse. Whether that beat is tolerable is a question for a
 * play session, not for the compiler.
 */
@EventBusSubscriber
public final class BarebackSteeringHandler {

    /** The bond tier at which a horse will take direction bare. The tier where it already follows you. */
    private static final int TIER = 3;

    /** Bareback is slower than a saddled trot, on purpose. */
    private static final double BAREBACK_FRACTION = 0.72;

    /** How fast the horse ramps toward the direction asked for. */
    private static final double BLEND = 0.30;

    private BarebackSteeringHandler() {
    }

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) {
            return;
        }
        if (!horse.isAlive() || !horse.isVehicle() || !horse.isTamed() || horse.isSaddled()) {
            return; // saddled is vanilla's job and it does it better
        }
        if (!horse.isLocalInstanceAuthoritative()) {
            return;
        }
        if (!(horse.getFirstPassenger() instanceof Player rider) || !ownedBy(horse, rider)) {
            return;
        }
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        if (care.behaviourTier() < TIER) {
            return;
        }

        // Point it where the rider is looking. This is the "steering" half, and
        // it is what a rider notices first - a horse that turns under you reads
        // as controlled even before it has moved anywhere.
        horse.setYRot(rider.getYRot());
        horse.yRotO = horse.getYRot();
        horse.setYHeadRot(horse.getYRot());
        horse.setYBodyRot(horse.getYRot());

        float forward = rider.zza;
        float strafe = rider.xxa;
        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }
        double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * BAREBACK_FRACTION;
        float yawRad = horse.getYRot() * ((float) Math.PI / 180.0F);
        double sin = Mth.sin(yawRad);
        double cos = Mth.cos(yawRad);
        // the rotation vanilla's moveRelative uses for (xxa, _, zza)
        double wishX = strafe * cos - forward * sin;
        double wishZ = forward * cos + strafe * sin;
        double len = Math.sqrt(wishX * wishX + wishZ * wishZ);
        if (len <= 1.0E-4) {
            return;
        }
        wishX = wishX / len * speed;
        wishZ = wishZ / len * speed;
        Vec3 dm = horse.getDeltaMovement();
        horse.setDeltaMovement(Mth.lerp(BLEND, dm.x, wishX), dm.y, Mth.lerp(BLEND, dm.z, wishZ));
    }

    private static boolean ownedBy(AbstractHorse horse, Player player) {
        LivingEntity owner = horse.getOwner();
        return owner != null && owner.getUUID().equals(player.getUUID());
    }
}
