package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.server.FlightToggle;
import com.example.horsegenetics.neoforge.server.HorseFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * <b>A line above the hotbar while you are flying, saying how to get down.</b>
 *
 * <p>Sneak does not dismount a flying horse - {@link ClientFlightInput} takes the key away before the server can read
 * it as "get off", because a rider who sneaks to descend would otherwise step off a hundred blocks up. That is the
 * right behaviour and completely undiscoverable, which is what this is for (owner, 2026-09-17: <i>"you need some kind
 * of button overlay or tooltip that says f to dismount"</i>).
 *
 * <p><b>It reads the live binding rather than the letter F.</b> The key is {@code key.horsegenetics.dive_horse},
 * which is contextual - it dismounts while flying and toggles diving otherwise - and it collides with vanilla's
 * swap-hands, so it is a key people will rebind. A prompt that hard-coded "F" would start lying the moment they did.
 *
 * <p>Drawn above {@code SELECTED_ITEM_NAME}: vanilla's own contextual prompts sit on the {@code guiHeight() - 59}
 * row, and the horse's jump bar occupies the band at {@code guiHeight() - 29}, so this sits clear of both.
 *
 * <p><b>UNVERIFIED at runtime.</b>
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class FlightHudLayer {

    private FlightHudLayer() {
    }

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "flight_prompt");

    /** Clear of the item-name row (59) and the jump bar (29), by one line of font height plus a little. */
    private static final int ABOVE_BOTTOM = 70;

    @SubscribeEvent
    static void register(RegisterGuiLayersEvent event) {
        // No bus() - RegisterGuiLayersEvent implements IModBusEvent and routes
        // itself, the same way RegisterKeyMappingsEvent does in DebugKeyBindings.
        event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME, ID, (graphics, deltaTracker) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.options.hideGui) {
                return;
            }
            // TRUE FLIGHT ONLY. A glider never rebinds anything: sneak still
            // dismounts it, F still means nothing up there, and there is no
            // landing to ask for - it comes down by running out of height, which
            // is the entire point of an elytra. Advertising "F to land" on one
            // would be offering a control it does not have (owner, 2026-09-17).
            if (!(minecraft.player.getVehicle() instanceof Horse horse)
                    || HorseFlight.of(horse).mode() != HorseFlight.Mode.TRUE_FLIGHT
                    || !HorseFlight.active(horse)) {
                return;
            }
            Component key = DebugKeyBindings.diveHorse == null
                    ? Component.literal("?")
                    : DebugKeyBindings.diveHorse.getTranslatedKeyMessage();
            Component text = FlightToggle.landing(horse)
                    ? Component.translatable("hud.horsegenetics.flight_landing", key)
                    : Component.translatable("hud.horsegenetics.flight_dismount", key);
            // Opaque alpha matters: text() draws nothing at all when the colour
            // has none, which is a silent failure rather than an error.
            graphics.centeredText(minecraft.font, text,
                    graphics.guiWidth() / 2, graphics.guiHeight() - ABOVE_BOTTOM, 0xFFFFFFFF);
        });
    }
}
