package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.network.OpenHorseBrowserPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Asks the server to open the Horse Browser menu when the browser key is pressed
 * and no screen is currently up. The browser is a real container menu now (it
 * has a crafting tab with slots), so opening goes through the server; the client
 * screen is bound to the menu type in {@code ClientSetup}. (While a screen is
 * open the key never reaches the mapping - Escape closes the browser.)
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class HorseBrowserKeyHandler {

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (HorseBrowserKeyBindings.openBrowser == null) {
            return;
        }
        boolean pressed = false;
        while (HorseBrowserKeyBindings.openBrowser.consumeClick()) {
            pressed = true;
        }
        if (!pressed) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null && mc.player != null) {
            ClientPacketDistributor.sendToServer(new OpenHorseBrowserPayload());
        }
    }

    private HorseBrowserKeyHandler() {
    }
}
