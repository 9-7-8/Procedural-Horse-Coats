package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The key that blows every whistle you are carrying. Default <kbd>'</kbd>,
 * rebindable in Controls.
 *
 * <p><b>Why the apostrophe.</b> A plain single key rather than a chord:
 * <kbd>Shift</kbd>+<kbd>C</kbd> is out because shift dismounts a rider and
 * <kbd>Ctrl</kbd>+<kbd>C</kbd> because ctrl is sprint, and no modifier means no
 * modifier API to get wrong and no reason to switch the key off while mounted -
 * calling your other horses from the saddle is a thing a player wants. The
 * apostrophe sits somewhere else on plenty of keyboard layouts, so some players
 * will rebind it; that is what Controls is for, and the swap prompt reads the
 * live binding rather than saying "press apostrophe".
 *
 * <p>Same shape as {@link HorseBrowserKeyBindings} - {@code IN_GAME} context, a
 * built-in category rather than a custom one to register and localise.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = HorseGenetics.MOD_ID)
public final class WhistleKeyBindings {

    public static final String BLOW_KEY = "key.horsegenetics.blow_whistles";

    public static KeyMapping blowWhistles;

    @SubscribeEvent
    static void register(RegisterKeyMappingsEvent event) {
        blowWhistles = new KeyMapping(
                BLOW_KEY,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_APOSTROPHE,
                KeyMapping.Category.MISC);
        event.register(blowWhistles);
    }

    private WhistleKeyBindings() {
    }
}
