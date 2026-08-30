package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Dev-only debug hook. FMLEnvironment.isProduction() is true when running
 * from a normal built/installed jar and false when running via the Gradle
 * dev environment (runClient) - so in a real build, this keybind is simply
 * never registered. It won't appear in the Controls menu, can't be rebound,
 * and there is no way for a player to trigger it.
 *
 * The server-side handler in DebugPenNetworkHandler re-checks isProduction()
 * independently, so a modified/forged client packet against a production
 * server still won't do anything.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = HorseGenetics.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DebugKeyBindings {

    /** Null in production - always null-check before use. */
    public static KeyMapping generateDebugPens;

    @SubscribeEvent
    static void register(RegisterKeyMappingsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        generateDebugPens = new KeyMapping(
                "key.horsegenetics.debug_pens",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "key.categories.horsegenetics.debug"
        );
        event.register(generateDebugPens);
    }

    private DebugKeyBindings() {
    }
}
