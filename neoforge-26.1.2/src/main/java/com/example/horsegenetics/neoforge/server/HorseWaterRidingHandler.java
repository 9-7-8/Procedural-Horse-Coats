package com.example.horsegenetics.neoforge.server;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Lets a <b>tamed, ridden</b> horse cross water - slowly. Vanilla already
 * floats a ridden horse at the surface (horses are in the
 * {@code minecraft:can_float_while_ridden} entity tag), but its in-water
 * movement speed is a near-frozen ~0.02 blocks/tick. This nudges it to a
 * deliberate {@value #WATER_RIDE_SPEED} b/t paddle in whatever direction the
 * rider steers, and adds a touch of lift so the rider's head stays above
 * water.
 *
 * <p>Runs on whichever side owns the horse's movement simulation
 * ({@code isLocalInstanceAuthoritative} - the controlling client for a
 * player-ridden horse), so the assist and the authoritative position agree.
 *
 * <p><b>Not verified in-game</b> - written against 26.1.2 sources; the feel
 * (speed, lift) is a first guess.
 */
@EventBusSubscriber
public final class HorseWaterRidingHandler {

    private static final double WATER_RIDE_SPEED = 0.09; // blocks/tick - clearly slower than a land trot
    private static final double SURFACE_LIFT = 0.05;
    private static final double BLEND = 0.25;            // how fast the paddle ramps toward WATER_RIDE_SPEED

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        if (!horse.isAlive() || !horse.isVehicle() || !horse.isInWater()) return;
        if (!horse.isTamed()) return;
        if (!horse.isLocalInstanceAuthoritative()) return;
        if (!(horse.getControllingPassenger() instanceof Player rider)) return;

        Vec3 dm = horse.getDeltaMovement();

        if (horse.isUnderWater() && dm.y < SURFACE_LIFT) {
            dm = new Vec3(dm.x, SURFACE_LIFT, dm.z);
        }

        float forward = rider.zza;
        float strafe = rider.xxa;
        if (forward != 0.0F || strafe != 0.0F) {
            float yawRad = horse.getYRot() * ((float) Math.PI / 180.0F);
            double sin = Mth.sin(yawRad);
            double cos = Mth.cos(yawRad);
            // same rotation vanilla's moveRelative uses for (xxa, _, zza)
            double wishX = strafe * cos - forward * sin;
            double wishZ = forward * cos + strafe * sin;
            double len = Math.sqrt(wishX * wishX + wishZ * wishZ);
            if (len > 1.0E-4) {
                wishX = wishX / len * WATER_RIDE_SPEED;
                wishZ = wishZ / len * WATER_RIDE_SPEED;
                dm = new Vec3(Mth.lerp(BLEND, dm.x, wishX), dm.y, Mth.lerp(BLEND, dm.z, wishZ));
            }
        }

        horse.setDeltaMovement(dm);
    }

    private HorseWaterRidingHandler() {
    }
}
