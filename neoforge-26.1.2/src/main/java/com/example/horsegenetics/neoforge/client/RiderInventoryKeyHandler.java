package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.server.ActionTrace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Opens the player's own inventory when
 * {@link RiderInventoryKeyBindings#riderInventory} is pressed - the door back
 * to your pack that {@link HorseInfoKeyHandler} closed by taking <kbd>E</kbd>
 * from the saddle.
 *
 * <p><b>No server round trip, and none needed.</b> The player's own
 * {@code InventoryMenu} is not opened on demand: it is live on the server for
 * the whole session, which is exactly why vanilla's own <kbd>E</kbd> path in
 * {@code Minecraft.handleKeybinds} just calls {@code setScreen} rather than
 * sending a packet. {@link HorseBrowserKeyHandler} made the same simplification
 * for the same reason. The one case where vanilla <i>does</i> send a packet is
 * {@code MultiPlayerGameMode.isServerControlledInventory()} - true precisely
 * when you are riding something with its own screen - and that is the case this
 * key exists to step around, so taking it would defeat the point.
 *
 * <p><b>Creative mode needs no branch here</b>, which is a divergence from the
 * plan this was built from. That plan said to pick the screen class by game
 * mode; reading 26.1.2's {@code InventoryScreen} says not to. Both its
 * {@code init} and its {@code containerTick} begin by checking
 * {@code player.hasInfiniteMaterials()} and swapping themselves out for a
 * {@code CreativeModeInventoryScreen}, so constructing the survival screen is
 * what vanilla itself does in both modes - and a hand-rolled branch would be a
 * second copy of that rule, wrong the moment the first one moves.
 *
 * <p><b>{@code Post}, not {@code Pre}.</b> Its neighbour
 * {@code HorseInfoKeyHandler} has to run in {@link ClientTickEvent.Pre} because
 * it is stealing a click from vanilla and must get there before
 * {@code handleKeybinds}. This one steals nothing - it owns its mapping - so it
 * runs in {@link ClientTickEvent.Post} like {@link HorseBrowserKeyHandler}.
 *
 * <p>The clicks are drained in a loop for the usual reason: two presses in one
 * tick must not leave a click banked for the next screen to swallow.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class RiderInventoryKeyHandler {

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (RiderInventoryKeyBindings.riderInventory == null) {
            return;
        }
        boolean pressed = false;
        while (RiderInventoryKeyBindings.riderInventory.consumeClick()) {
            pressed = true;
        }
        if (!pressed) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // The same guard vanilla puts around handleKeybinds: a press made while
        // a screen or an overlay is up must not reach through it.
        if (mc.player == null || mc.screen != null || mc.getOverlay() != null) {
            return;
        }
        ActionTrace.log("key ALT+E", "own inventory requested");
        // Vanilla's own keyInventory branch does this pair, in this order.
        mc.getTutorial().onOpenInventory();
        mc.setScreen(new InventoryScreen(mc.player));
    }

    private RiderInventoryKeyHandler() {
    }
}
