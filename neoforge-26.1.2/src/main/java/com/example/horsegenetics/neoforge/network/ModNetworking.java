package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.client.ClientCoatCache;
import com.example.horsegenetics.neoforge.server.DebugPenManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public final class ModNetworking {

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                CoatSyncPayload.TYPE,
                CoatSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientCoatCache.put(payload.entityId(), payload.coatData()))
        );
        registrar.playToServer(
                RequestDebugPensPayload.TYPE,
                RequestDebugPensPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // Re-checked here independently of the client-side keybind gate -
                    // a forged packet against a production server should still no-op.
                    if (FMLEnvironment.isProduction()) return;
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        DebugPenManager.teleportAndGenerate(serverPlayer);
                    }
                })
        );
    }

    private ModNetworking() {
    }
}
