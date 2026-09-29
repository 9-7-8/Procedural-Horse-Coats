package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.EnderWhistleItem;
import com.example.horsegenetics.neoforge.item.WhistleItem;
import com.example.horsegenetics.neoforge.network.BlowWhistlesPayload;
import com.example.horsegenetics.neoforge.server.ActionTrace;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Sends the blow-whistles key to the server, and tells a player holding a
 * whistle that the key exists.
 *
 * <p><b>The key sends a bare request, not a decision.</b> Which whistles get
 * blown is worked out server-side from the player's real inventory - see
 * {@code network/BlowWhistlesPayload} for why the packet carries nothing.
 *
 * <h2>The swap prompt</h2>
 *
 * <p>A keybind nobody is told about is a keybind nobody uses, and this one is
 * on a key some layouts do not even have where we put it. So selecting a
 * whistle shows one action-bar line naming <em>the player's own</em> binding:
 * {@link Component#keybind} resolves against their Controls, so a rebound key
 * reads correctly and an unbound one gets a different line telling them to bind
 * it.
 *
 * <p>Client-side only, with no packet: it fires off a change in the selected
 * item, not every tick, and it is behind {@code whistle.keyPrompt} for players
 * who already know.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class WhistleKeyHandler {

    private WhistleKeyHandler() {
    }

    /**
     * The item the player was holding last tick. An {@link Item}, not an
     * {@link ItemStack}: comparing stacks would re-fire on anything that
     * changes a component, and binding an ender whistle does exactly that.
     */
    private static @Nullable Item lastHeld;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            lastHeld = null;
            return;
        }
        blowOnKey(mc);
        promptOnSwap(mc);
    }

    private static void blowOnKey(Minecraft mc) {
        if (WhistleKeyBindings.blowWhistles == null) {
            return;
        }
        boolean pressed = false;
        while (WhistleKeyBindings.blowWhistles.consumeClick()) {
            pressed = true;
        }
        // A screen being open swallows the key before the mapping sees it, so
        // there is no mc.screen check here the way the browser key needs one -
        // that key opens a screen, this one does not.
        if (pressed) {
            ActionTrace.log("key whistle", "blow-all requested");
            ClientPacketDistributor.sendToServer(new BlowWhistlesPayload());
        }
    }

    private static void promptOnSwap(Minecraft mc) {
        Item held = mc.player.getMainHandItem().getItem();
        if (held == lastHeld) {
            return;
        }
        lastHeld = held;
        if (!ClientConfig.whistleKeyPrompt()) {
            return;
        }
        if (!blowable(mc.player.getMainHandItem())) {
            return;
        }
        boolean unbound = WhistleKeyBindings.blowWhistles == null
                || WhistleKeyBindings.blowWhistles.isUnbound();
        // sendOverlayMessage, not displayClientMessage(msg, true): 26.1.2
        // renamed the action-bar call, as the note on block/Jumps records.
        mc.player.sendOverlayMessage(unbound
                ? Component.translatable("horsegenetics.whistle.prompt_unbound")
                : Component.translatable("horsegenetics.whistle.prompt",
                        Component.keybind(WhistleKeyBindings.BLOW_KEY)));
    }

    /**
     * Is this a whistle the key would actually do something with? An
     * <b>unbound</b> ender whistle is not - there is nothing to blow yet, and
     * its tooltip already says to right-click a horse. Prompting to blow it
     * would send the player to the wrong verb.
     */
    private static boolean blowable(ItemStack stack) {
        if (stack.getItem() instanceof WhistleItem) {
            return true;
        }
        return stack.getItem() instanceof EnderWhistleItem
                && stack.get(ModDataComponents.BOUND_HORSE.get()) != null;
    }
}
