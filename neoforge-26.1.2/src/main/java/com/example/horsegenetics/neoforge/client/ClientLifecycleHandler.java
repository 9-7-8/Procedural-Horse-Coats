package com.example.horsegenetics.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * On leaving a world, drop everything that was tied to that world's session:
 * the client coat / record caches (keyed by per-world entity ids) and the
 * generated bay textures (made for that world's horse database). The database
 * itself is a per-world SavedData file, so it goes when the save folder does.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientLifecycleHandler {

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCoatCache.clear();
        ClientHorseRecordCache.clear();
        GeneticCoatTextureFactory.clear();
    }

    private ClientLifecycleHandler() {
    }
}
