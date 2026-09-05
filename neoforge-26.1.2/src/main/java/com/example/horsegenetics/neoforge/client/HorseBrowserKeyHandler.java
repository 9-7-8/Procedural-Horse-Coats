package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Opens the {@link HorseBrowserScreen} when the browser key is pressed and no
 * screen is currently up. (While a screen is open the key never reaches the
 * mapping, so there is nothing to toggle - Escape closes the browser.)
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
            mc.setScreen(new HorseBrowserScreen());
        }
    }

    private HorseBrowserKeyHandler() {
    }
}
