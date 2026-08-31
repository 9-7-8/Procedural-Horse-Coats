package com.example.horsegenetics.neoforge.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Fills in {@code tamedBy} for horses that got tamed. We can't do this in the
 * interaction handler because wild-horse taming resolves a few ticks later
 * (inside the horse's own tick, after the bucking logic), not during the
 * interact packet. So instead: every couple of seconds, any tamed horse whose
 * record has no {@code tamedBy} yet gets its owner's username recorded.
 */
@EventBusSubscriber
public final class HorseOwnerTrackingHandler {

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse)) return;
        if (horse.level().isClientSide() || !horse.isTamed()) return;
        if (horse.tickCount % 40 != 0) return;                 // ~ every 2s
        if (!HorseRecords.hasRealRecord(horse)) return;
        if (HorseRecords.of(horse).tamedBy().isPresent()) return;

        String ownerName = resolveOwnerName(horse);
        if (ownerName != null) {
            HorseRecords.setTamedBy(horse, ownerName);
        }
    }

    private static String resolveOwnerName(Horse horse) {
        LivingEntity owner = horse.getOwner();
        if (owner instanceof Player player) {
            return player.getGameProfile().name();
        }
        EntityReference<LivingEntity> ref = horse.getOwnerReference();
        if (ref != null && horse.level() instanceof ServerLevel level) {
            ServerPlayer online = level.getServer().getPlayerList().getPlayer(ref.getUUID());
            if (online != null) {
                return online.getGameProfile().name();
            }
        }
        return null; // owner offline / unknown - try again next pass
    }

    private HorseOwnerTrackingHandler() {
    }
}
