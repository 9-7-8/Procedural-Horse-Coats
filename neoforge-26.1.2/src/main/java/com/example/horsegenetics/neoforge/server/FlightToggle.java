package com.example.horsegenetics.neoforge.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * <b>Has the rider asked this horse to fly?</b> A toggle, like the dive one beside it, and F sets both.
 *
 * <p><b>Why it is held on both sides.</b> The movement runs on the rider's client, so the client must know. But
 * {@code ServerGamePacketListenerImpl} asks {@code Entity.isFlyingVehicle()} on the SERVER when it decides whether a
 * floating vehicle has been floating too long - and a rider whose vehicle fails that test is <i>disconnected</i> after
 * 80 ticks. So the server has to agree, or flight ends in a kick four seconds in. The client toggles its own copy for
 * responsiveness and tells the server with {@code ToggleFlightPayload}; neither side waits for the other.
 *
 * <p>Cleared on dismount, the same as diving and for the same reason: a flag that outlived the ride would put the next
 * horse in the air for reasons its rider could not see.
 */
@EventBusSubscriber
public final class FlightToggle {

    private FlightToggle() {
    }

    /** Riders who have asked to fly and not yet asked to stop. Server-side; the client keeps its own. */
    private static final Set<UUID> WANTS = ConcurrentHashMap.newKeySet();

    /**
     * The client's copy, installed by {@code client.ClientFlightHandler} at client setup. A hook rather than a direct
     * call, so this class - which the mixin loads on a dedicated server too - never names a client-only class.
     */
    public static volatile Predicate<Horse> clientWants = horse -> false;

    /** Riders who have asked to come down. A landing is a flight that is ending, not a separate state. */
    private static final Set<UUID> LANDING = ConcurrentHashMap.newKeySet();

    /** The client's copy of {@link #LANDING}, installed by {@code client.ClientFlightInput}. */
    public static volatile Predicate<Horse> clientLanding = horse -> false;

    /**
     * <b>Coming down under control.</b> True while the rider has asked to land and the horse is still in the air -
     * so it stops being true by itself the moment the hooves touch, on both sides, without either having to tell the
     * other. Owner's call, 2026-09-17: F lands the horse rather than dropping the rider off it.
     */
    public static boolean landing(Horse horse) {
        if (horse.onGround()) {
            return false;
        }
        if (horse.level().isClientSide()) {
            return clientLanding.test(horse);
        }
        return horse.getControllingPassenger() instanceof Player rider && LANDING.contains(rider.getUUID());
    }

    /** Ask to come down. */
    public static void land(ServerPlayer player) {
        LANDING.add(player.getUUID());
    }

    /** Whether the horse's own rider is currently asking it to fly. False for a loose horse: flight is ridden-only. */
    public static boolean wants(Horse horse) {
        if (horse.level().isClientSide()) {
            return clientWants.test(horse);
        }
        return horse.getControllingPassenger() instanceof Player rider && WANTS.contains(rider.getUUID());
    }

    /**
     * Whether the rider is holding sneak to come down, installed by {@code client.ClientFlightInput}.
     *
     * <p>Client-only by nature. Sneak is stripped out of the input packet before the server is told, so that
     * {@code Player.rideTick} never dismounts a rider a hundred blocks up - which means the server genuinely does not
     * know, and does not need to: the side that reads this is the side that moves the horse.
     */
    public static volatile java.util.function.BooleanSupplier clientDescending = () -> false;

    /** True only on the client, and only while its rider is asking to descend. */
    public static boolean descending(Horse horse) {
        return horse.level().isClientSide() && clientDescending.getAsBoolean();
    }

    /** Called from {@code ToggleFlightPayload}. Returns the new state, so the caller may say so in chat. */
    public static boolean toggle(ServerPlayer player) {
        boolean on = WANTS.add(player.getUUID());
        if (!on) {
            WANTS.remove(player.getUUID());
        }
        // SAY SO IN THE LOG. Flight is the one system in this mod that runs on
        // the rider's client, so when it misbehaves there is nothing on the
        // server to look at afterwards - the first test session had to be
        // diagnosed entirely from what the owner could see. A line per toggle
        // at least fixes which horse, which mode and which way round.
        if (player.getVehicle() instanceof Horse horse) {
            ActionTrace.log("flight", (on ? "ON" : "off") + " | " + horse.getName().getString()
                    + " mode=" + HorseFlight.of(horse).mode()
                    + " cloudWalk=" + HorseFlight.of(horse).cloudWalk()
                    + " groundSpeed=" + String.format("%.4f",
                            horse.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED))
                    + " y=" + String.format("%.1f", horse.getY()));
        }
        return on;
    }

    public static void stop(ServerPlayer player) {
        WANTS.remove(player.getUUID());
        LANDING.remove(player.getUUID());
    }

    /** Forget a rider's flight the moment they are off, so it cannot carry to the next horse. */
    @SubscribeEvent
    static void clearOnDismount(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
        if (!event.isMounting() && event.getEntityMounting() instanceof ServerPlayer player) {
            WANTS.remove(player.getUUID());
            LANDING.remove(player.getUUID());
        }
    }
}
