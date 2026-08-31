package com.example.horsegenetics.neoforge.server;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Extends a player's plot corridor ahead of them as they walk. Runs every
 * player tick - {@link DebugPenManager#ensureGeneratedAheadOfPlayer} is cheap
 * when nothing new needs building (one integer comparison). Not dev-gated: the
 * horse dimension is a normal feature now, reachable by hay-bale portal in any
 * build. (Only the F6 shortcut is dev-only - see {@code DebugKeyBindings} /
 * {@code ModNetworking}.)
 */
@EventBusSubscriber
public final class DebugPenTickHandler {

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(DebugPenManager.DEBUG_LEVEL)) return;

        DebugPenManager.ensureGeneratedAheadOfPlayer(player);
    }

    private DebugPenTickHandler() {
    }
}
