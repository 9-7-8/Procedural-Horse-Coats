package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.network.NamingPolicyPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * On joining a world, tell the server the preferences it has to enforce on this
 * player's behalf. On leaving one, drop everything that was tied to that
 * world's session: the client coat / record caches (keyed by per-world entity
 * ids) and the generated bay textures (made for that world's horse database).
 * The database itself is a per-world SavedData file, so it goes when the save
 * folder does.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientLifecycleHandler {

    /**
     * Hand the server this player's foal-naming policy.
     *
     * <p>Sent on every join rather than once, because the client config is a
     * file the player edits between sessions and the server's copy is the one
     * that names foals - including while they are offline, which is exactly why
     * the server has to hold a copy at all. It is a suggestion: the server
     * files it under this player's UUID and applies it to no other herd.
     */
    @SubscribeEvent
    static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientPacketDistributor.sendToServer(NamingPolicyPayload.of(ClientConfig.namingPolicy()));
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCoatCache.clear();
        ClientHorseRecordCache.clear();
        ClientHorseCareCache.clear();
        ClientGeneDatabase.clear();
        ClientProgress.clear();
        ClientHorseRoster.clear();
        ClientRealmRoster.clear();
        ClientRealmWatch.forget();
        ClientHorseLog.clear();
        ClientHorseCoats.clear();
        ClientOffspring.clear();
        ClientPopulation.clear();
        HorsePortrait.clear();
        // The tutorial's villager and cowboy belong to that client level too, and
        // its item stacks were built against that world's registries.
        TutorialPortraits.clear();
        TutorialPage.clear();
        GenePreviews.clear();
        BreedPreviews.clear();
        // The browser remembers where you were for the session; the parts of that
        // which name a *horse* are meaningless in the next world.
        HorseBrowserScreen.forgetWorld();
        GeneticCoatTextureFactory.clear();
        FlatItemCatalog.clear();
    }

    private ClientLifecycleHandler() {
    }
}
