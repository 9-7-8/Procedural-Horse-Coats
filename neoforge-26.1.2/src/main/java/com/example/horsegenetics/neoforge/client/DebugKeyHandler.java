package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.server.ActionTrace;
import com.example.horsegenetics.neoforge.network.RequestDebugPensPayload;
import com.example.horsegenetics.neoforge.network.RequestHighlightHorsesPayload;
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
        // Each binding is guarded on its own. It used to return early when
        // generateDebugPens was null, which in a production build is always -
        // so a binding that IS registered there (the horse highlight) would
        // never have been read at all.
        while (DebugKeyBindings.generateDebugPens != null
                && DebugKeyBindings.generateDebugPens.consumeClick()) {
            ActionTrace.log("key F6", "debug pens requested");
            ClientPacketDistributor.sendToServer(new RequestDebugPensPayload());
        }
        while (DebugKeyBindings.showStalls != null && DebugKeyBindings.showStalls.consumeClick()) {
            ActionTrace.log("key F7", "stall overlay requested");
            ClientPacketDistributor.sendToServer(new RequestStallHighlightPayload());
        }
        while (DebugKeyBindings.highlightHorses != null && DebugKeyBindings.highlightHorses.consumeClick()) {
            // Logged on the press rather than on the toggle's answer, so a key
            // that is read but produces nothing is distinguishable from a key
            // that was never read - which is exactly how the F8 toggle bug read
            // from the outside (2026-09-12).
            ActionTrace.log("key F8", "horse highlight toggle pressed");
            ClientPacketDistributor.sendToServer(new RequestHighlightHorsesPayload());
        }
    }

    private DebugKeyHandler() {
    }
}
