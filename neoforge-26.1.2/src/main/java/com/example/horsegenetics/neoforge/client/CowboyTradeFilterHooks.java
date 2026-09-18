package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.network.CowboyFilterPayload;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * <b>The filter box above a cowboy's string.</b> Type a breed, a gene or an
 * allele and the list narrows to the horses that answer it.
 *
 * <p>A cowboy's window is a column of names and prices, and a name says nothing
 * about the animal. That was liveable when a cowboy sold six horses of one
 * breed. It is not liveable for the
 * <a href="../server/ArcaneWandererHandler.html">arcane dealer</a>, whose ten
 * horses carry a hundred-odd magical loci between them and whose entire product
 * is the genes: "which of these has galaxy" cannot be a question you answer by
 * opening ten horses one at a time.
 *
 * <h2>The filtering is the server's, not this screen's</h2>
 * All this box does is send what was typed. <b>It never hides a row itself</b>,
 * and that restraint is the whole design: a trade is sent back to the server as
 * an <i>index</i> into the merchant's offer list, so a client that quietly drew
 * fewer rows would make row three mean one horse on screen and another on the
 * server - and the player would buy the wrong animal, at twelve to twenty-eight
 * emeralds, with nothing anywhere reporting an error. The server rebuilds its
 * own list and sends it back, so there is only ever one list and the indices
 * cannot drift apart. See {@link CowboyFilterPayload}.
 *
 * <h2>Knowing it is a cowboy</h2>
 * The client cannot ask: {@code MerchantMenu} holds a stand-in merchant, not the
 * entity. But a cowboy's offers are unmistakable - every one pays out a signed
 * transfer paper carrying a {@code HORSE_DEED} - so the screen is identified by
 * what is being sold rather than by who is selling it. That also means the box
 * appears for <b>every</b> cowboy, which is what was asked for: the arcane
 * dealer needs it most, and the ordinary one is no worse for having it.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class CowboyTradeFilterHooks {

    private static final int BOX_W = 96;
    private static final int BOX_H = 14;
    private static final int GAP = 4;

    private static @Nullable EditBox filterBox;

    private CowboyTradeFilterHooks() {
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        filterBox = null;
        if (!(event.getScreen() instanceof MerchantScreen screen) || !sellsHorses(screen)) {
            return;
        }
        int left = ((AbstractContainerScreen<?>) screen).getGuiLeft();
        int top = ((AbstractContainerScreen<?>) screen).getGuiTop();

        // Above the window rather than in it. The merchant sprite is fixed-size
        // with no spare pixels, the way the horse screen's i button found out.
        EditBox box = new EditBox(screen.getMinecraft().font,
                left, top - BOX_H - GAP, BOX_W, BOX_H, Component.literal("Filter"));
        box.setHint(Component.literal("breed, gene or allele"));
        box.setMaxLength(CowboyFilterPayload.MAX_QUERY);
        box.setResponder(query -> ClientPacketDistributor.sendToServer(new CowboyFilterPayload(query)));
        filterBox = box;
        event.addListener(box);
    }

    /**
     * Stop the inventory key closing the window while somebody is typing in the
     * box.
     *
     * <p>The same vanilla trap {@code HorseBrowserScreen.keyPressed} documents:
     * {@link EditBox#keyPressed} handles the control keys and returns false for
     * an ordinary letter, because letters arrive separately through
     * {@code charTyped}. The letter falls through, and the next thing
     * {@code AbstractContainerScreen} does is test it against
     * {@code keyInventory} and close. So pressing "e" would shut the shop instead
     * of typing an e - the first key somebody searching for "ember" reaches for.
     *
     * <p>Swallow every key but Escape while the box has focus. It costs nothing:
     * GLFW's character callback is separate, so {@code charTyped} still delivers
     * the letter and the text still types.
     */
    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Post event) {
        EditBox box = filterBox;
        if (box == null || !box.isFocused() || !box.isActive()) {
            return;
        }
        if (event.getKeyCode() != com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            event.setCanceled(true);
        }
    }

    /** Escape should leave the box before it leaves the shop. */
    @SubscribeEvent
    static void onScreenClosed(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof MerchantScreen) {
            filterBox = null;
        }
    }

    /** A merchant whose offers pay out horse papers is a cowboy - see the class note. */
    private static boolean sellsHorses(MerchantScreen screen) {
        for (MerchantOffer offer : screen.getMenu().getOffers()) {
            if (offer.getResult().has(ModDataComponents.HORSE_DEED.get())) {
                return true;
            }
        }
        return false;
    }
}
