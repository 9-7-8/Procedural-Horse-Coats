package com.example.horsegenetics.neoforge.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Horses being read about stand still.</b> A horse that wanders off the
 * moment you open its information screen takes the screen with it - you close
 * it to go and find the horse - so while a player has that screen open, the
 * horse it is about is held in place.
 *
 * <h2>Why a timestamp and not a flag</h2>
 * A hold is a <i>lease</i>: the client re-asserts it once a second
 * ({@code InspectHorsePayload}) and it lapses {@link #LEASE_TICKS} ticks after
 * the last word from the client. A boolean set on open and cleared on close
 * would be correct right up until the client crashed, disconnected or was
 * killed with the screen up - and then that horse would stand in a field
 * forever with nothing in the world able to say why. Anything that can freeze a
 * mob has to be the kind of thing that thaws by itself.
 *
 * <p>One horse per player, so opening a second screen releases the first. The
 * freeze itself is {@link InspectHoldGoal}, which asks {@link #isHeld} - so it
 * goes through the AI rather than around it, and a held horse stops <i>being
 * driven</i> instead of having its position overwritten under whatever else is
 * moving it.
 */
@EventBusSubscriber
public final class HorseInspectHold {

    /** How long a hold survives without the client saying so again. */
    public static final int LEASE_TICKS = 60; // three seconds; the client speaks every second

    /** player -> the horse they are reading about, and when they last said so. */
    private static final Map<UUID, Hold> HOLDS = new HashMap<>();

    private record Hold(UUID horseId, long expiresAt) {
    }

    private HorseInspectHold() {
    }

    /**
     * Take or renew a player's hold on one horse, or drop it. Range-checked the
     * way every other horse interaction in this mod is: the screen is opened
     * from the horse's own inventory, so a request about a horse across the
     * world did not come from the screen.
     */
    public static void set(ServerPlayer player, int entityId, boolean watching) {
        UUID who = player.getUUID();
        if (!watching) {
            HOLDS.remove(who);
            return;
        }
        Entity target = player.level().getEntity(entityId);
        if (!(target instanceof AbstractHorse horse) || !horse.closerThan(player, 16.0)) {
            HOLDS.remove(who);
            return;
        }
        HOLDS.put(who, new Hold(horse.getUUID(), horse.level().getGameTime() + LEASE_TICKS));
    }

    /**
     * Dropped the moment the player leaves, rather than waiting out the lease.
     * The lease would get there anyway - this just means a horse does not stand
     * for three seconds after its reader has gone.
     */
    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HOLDS.remove(player.getUUID());
        }
    }

    /**
     * Is anyone reading about this horse right now? Walks the holds, which is a
     * map of at most one entry per online player and is asked once per tick per
     * held-capable horse from {@link InspectHoldGoal#canUse()} - cheap enough
     * that a reverse index would be more bookkeeping than it saves.
     */
    public static boolean isHeld(AbstractHorse horse) {
        if (HOLDS.isEmpty() || !(horse.level() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        UUID id = horse.getUUID();
        boolean held = false;
        for (Map.Entry<UUID, Hold> entry : HOLDS.entrySet()) {
            Hold hold = entry.getValue();
            if (hold.expiresAt() < now) {
                continue; // lapsed; swept below rather than mid-iteration
            }
            if (hold.horseId().equals(id)) {
                held = true;
            }
        }
        HOLDS.values().removeIf(hold -> hold.expiresAt() < now);
        return held;
    }
}
