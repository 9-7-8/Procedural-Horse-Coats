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
 * The keybind that opens the {@link HorseBrowserScreen} - a real feature, so
 * (unlike {@link DebugKeyBindings}) it is registered in production too. Default
 * <kbd>H</kbd>, rebindable in Controls; conflicts resolve the normal way.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = HorseGenetics.MOD_ID)
public final class HorseBrowserKeyBindings {

    public static KeyMapping openBrowser;

    @SubscribeEvent
    static void register(RegisterKeyMappingsEvent event) {
        openBrowser = new KeyMapping(
                "key.horsegenetics.horse_browser",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                // Reuse a built-in category (as DebugKeyBindings does) rather
                // than registering and localising a custom one.
                KeyMapping.Category.MISC);
        event.register(openBrowser);
    }

    private HorseBrowserKeyBindings() {
    }
}
