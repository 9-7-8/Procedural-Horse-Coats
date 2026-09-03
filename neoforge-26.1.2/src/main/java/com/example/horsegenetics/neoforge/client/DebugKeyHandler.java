package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.network.RequestDebugPensPayload;
import com.example.horsegenetics.neoforge.network.RequestStallHighlightPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * NOTE: verify "ClientTickEvent.Post" is the correct current event class/name
 * for 26.1.2 against NeoForged docs - client tick event naming has shifted
 * across versions before (ClientTickEvent used to be a single fireable event
 * rather than split Pre/Post). Same category of caveat as the render-state
 * generics elsewhere in this project.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class DebugKeyHandler {

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (DebugKeyBindings.generateDebugPens == null) {
            return; // not registered - we're in a production build
        }
        while (DebugKeyBindings.generateDebugPens.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RequestDebugPensPayload());
        }
        while (DebugKeyBindings.showStalls != null && DebugKeyBindings.showStalls.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RequestStallHighlightPayload());
        }
    }

    private DebugKeyHandler() {
    }
}
