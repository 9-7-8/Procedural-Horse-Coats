package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.client.ClientCoatCache;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
    }

    private ModNetworking() {
    }
}
