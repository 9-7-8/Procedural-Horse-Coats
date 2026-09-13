package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.server.ActionTrace;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Opens the Horse Browser when the browser key is pressed and no screen is up.
 *
 * <p><b>Straight to the screen, with no server round trip.</b> The browser was a
 * real container menu while it had a crafting tab, which meant the key asked the
 * server to open a menu and the client bound a screen to the menu type. With the
 * crafting gone there is nothing to synchronise: every tab draws data the client
 * already has, pushed by its own payloads. So the key just opens a screen, and
 * one payload, one menu type and one screen registration went with it.
 *
 * <p>(While a screen is open the key never reaches the mapping - Escape closes
 * the browser.)
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
            ActionTrace.log("key H", "horse browser requested");
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
