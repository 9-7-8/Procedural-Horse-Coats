package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * This mod's one addition to the vanilla horse inventory screen (<kbd>E</kbd>
 * while riding, or shift-right-click on a tamed horse): a single <b>i</b>
 * button at the top left of the window, which opens {@link HorseInfoScreen}.
 *
 * <h2>One button, and nothing else</h2>
 * There used to be a grey vanilla-style panel to the left of the GUI, behind a
 * collapsible tab, carrying the horse's name, its four body numbers, its
 * disorders, its bond and an editable barn-name box. All of that now lives on
 * the information screen's Overview tab, which has room to say it properly -
 * the panel was a 128-pixel column trying to hold a page. Nothing is duplicated
 * and nothing here draws over the vanilla screen, so a second horse-inventory
 * mod keeps working and the only pixel this mod owns is the button.
 *
 * <h2>Which horse is this</h2>
 * The horse comes off the <b>screen</b>, not off {@code player.getVehicle()}.
 * The two agree while you are riding, and disagree completely when the screen
 * was opened by shift-right-clicking a tamed horse - vanilla's
 * {@code AbstractHorse.mobInteract} opens the inventory <i>without</i> mounting,
 * so the old vehicle lookup returned null and every one of this mod's additions
 * came up blank. {@code AbstractMountInventoryScreen.mount} is the authority;
 * it is protected, and this mod's access transformer opens it.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class HorseScreenHooks {

    /** Square, and tucked inside the window's top-left corner. */
    private static final int BUTTON = 14;
    private static final int INSET = 5;

    private static Button infoButton;

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof HorseInventoryScreen screen)) {
            return;
        }
        int left = ((AbstractContainerScreen<?>) screen).getGuiLeft();
        int top = ((AbstractContainerScreen<?>) screen).getGuiTop();

        infoButton = Button.builder(Component.literal("i"), b -> openInfo(screen))
                .bounds(left + INSET, top + INSET, BUTTON, BUTTON)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Horse information")))
                .build();
        infoButton.active = horseOf(screen) != null;
        event.addListener(infoButton);
    }

    private static void openInfo(HorseInventoryScreen screen) {
        AbstractHorse horse = horseOf(screen);
        if (horse == null) {
            return;
        }
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        if (record != null) {
            Minecraft.getInstance().setScreen(new HorseInfoScreen(record, horse, screen));
        }
    }

    /**
     * The horse this screen is showing. Public because
     * {@link HorseInfoScreen} needs it to go back, and because it is the one
     * correct answer to "which horse" on this screen - see the class note.
     */
    public static AbstractHorse horseOf(HorseInventoryScreen screen) {
        return screen.mount instanceof AbstractHorse horse ? horse : null;
    }

    private HorseScreenHooks() {
    }
}
