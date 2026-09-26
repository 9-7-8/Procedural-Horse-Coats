package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.network.CowboyFilterPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
 *
 * <h2>Why the box never appeared, until 2026-09-26</h2>
 * It was decided <i>at screen init</i>, by reading the menu's offers - and at
 * screen init <b>there are no offers</b>. The server sends the open-screen
 * packet and the offer list as two packets in that order, so the client builds
 * the screen against an empty {@code MerchantMenu} and fills it a moment later.
 * {@code sellsHorses} therefore answered false every single time, the box was
 * never created, and the feature read as "the filter does nothing" because
 * there was nothing on screen to do it with. (Owner, 2026-09-26.)
 *
 * <p>So the box is now built for <b>every</b> merchant screen and shown when the
 * offers turn out to be a cowboy's, which is a question asked each tick until it
 * is answered. Once it <b>is</b> answered the answer is <b>latched</b>, and that
 * is not an optimisation: filtering down to no matches empties the offer list,
 * which would un-answer the question and take away the very box the player needs
 * to clear the filter with.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class CowboyTradeFilterHooks {

    private static final int BOX_W = 96;
    private static final int BOX_H = 14;
    private static final int GAP = 4;

    /** The "▾" that opens the saved searches, beside the box. */
    private static final int SAVED_W = 14;

    private static @Nullable EditBox filterBox;

    /** The screen the box belongs to, so a tick knows whether it is still up. */
    private static @Nullable MerchantScreen owner;

    /** Have the offers arrived and proved this a cowboy? Latched - see the class note. */
    private static boolean confirmed;

    private static final SavedSearchPicker SAVED = new SavedSearchPicker();

    private static @Nullable Button savedButton;

    private CowboyTradeFilterHooks() {
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        filterBox = null;
        savedButton = null;
        owner = null;
        confirmed = false;
        SAVED.close();
        if (!(event.getScreen() instanceof MerchantScreen screen)) {
            return;
        }
        int left = ((AbstractContainerScreen<?>) screen).getGuiLeft();
        int top = ((AbstractContainerScreen<?>) screen).getGuiTop();

        // Above the window rather than in it. The merchant sprite is fixed-size
        // with no spare pixels, the way the horse screen's i button found out.
        EditBox box = new EditBox(screen.getMinecraft().font,
                left, top - BOX_H - GAP, BOX_W, BOX_H, Component.literal("Filter"));
        box.setHint(Component.literal("mare AND galaxy = any"));
        box.setMaxLength(CowboyFilterPayload.MAX_QUERY);
        box.setResponder(query -> ClientPacketDistributor.sendToServer(new CowboyFilterPayload(query)));
        // Hidden until the offers arrive and say this is a cowboy.
        box.visible = false;
        box.active = false;
        filterBox = box;
        event.addListener(box);

        // No room here for a query builder, so this window gets the other two
        // ways in: type it, or pick one you saved somewhere roomier.
        Button saved = Button.builder(Component.literal("▾"), b -> SAVED.open(
                        left + BOX_W + GAP, top - GAP, box.getValue(),
                        screen.width, screen.height))
                .bounds(left + BOX_W + GAP, top - BOX_H - GAP, SAVED_W, BOX_H)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        "Saved searches - the same list the horse browser uses.")))
                .build();
        saved.visible = false;
        saved.active = false;
        savedButton = saved;
        event.addListener(saved);
        owner = screen;
    }

    /**
     * Ask, until it is answered, whether the offers make this a cowboy's window.
     * A tick rather than a render because the answer changes at most once.
     */
    @SubscribeEvent
    static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        MerchantScreen screen = owner;
        if (screen == null || filterBox == null) {
            return;
        }
        if (net.minecraft.client.Minecraft.getInstance().screen != screen) {
            return;
        }
        if (!confirmed && sellsHorses(screen)) {
            confirmed = true;
            filterBox.visible = true;
            filterBox.active = true;
            if (savedButton != null) {
                savedButton.visible = true;
                savedButton.active = true;
            }
        }
    }

    /** The saved-search menu is drawn over the window, so it goes on last. */
    @SubscribeEvent
    static void onRender(ScreenEvent.Render.Post event) {
        if (owner != null && SAVED.isOpen()
                && event.getScreen() == owner
                && event.getGuiGraphics() instanceof GuiGraphicsExtractor g) {
            SAVED.draw(g, owner.getMinecraft().font, event.getMouseX(), event.getMouseY());
        }
    }

    /** An open menu eats the next click, wherever it lands. */
    @SubscribeEvent
    static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (owner == null || !SAVED.isOpen() || event.getScreen() != owner) {
            return;
        }
        SavedSearchPicker.Pick pick = SAVED.click(event.getMouseX(), event.getMouseY());
        if (pick != null && filterBox != null) {
            filterBox.setValue(pick.query());   // the responder sends it
        }
        event.setCanceled(true);
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
    /** The saved menu takes the keyboard while it is open - arrows, Escape, type-to-jump. */
    @SubscribeEvent
    static void onKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        if (owner != null && SAVED.isOpen() && event.getScreen() == owner
                && SAVED.keyPressed(event.getKeyCode())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (owner != null && SAVED.isOpen() && event.getScreen() == owner
                && SAVED.charTyped(event.getCodePoint())) {
            event.setCanceled(true);
        }
    }

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
            savedButton = null;
            owner = null;
            confirmed = false;
            SAVED.close();
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
