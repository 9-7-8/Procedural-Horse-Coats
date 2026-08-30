package com.example.horsegenetics.neoforge.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Checked every player tick, which is more often than strictly necessary
 * for a debug tool - if this turns out to be wasteful, throttle it to
 * "every 20 ticks" (once a second) instead. ensureGeneratedAheadOfPlayer
 * is cheap to call when nothing new needs building (single integer
 * comparison), so the naive every-tick version is left as-is for now.
 */
@EventBusSubscriber
public final class DebugPenTickHandler {

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (FMLEnvironment.isProduction()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(DebugPenManager.DEBUG_LEVEL)) return;

        DebugPenManager.ensureGeneratedAheadOfPlayer((ServerLevel) player.level(), player.getBlockX());
    }

    private DebugPenTickHandler() {
    }
}
