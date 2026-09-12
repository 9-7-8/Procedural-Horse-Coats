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
// NeoForge 26.1.2 dropped EventBusSubscriber#bus - RegisterKeyMappingsEvent is
// an IModBusEvent, so it's routed to the mod bus automatically.
@EventBusSubscriber(value = Dist.CLIENT, modid = HorseGenetics.MOD_ID)
public final class DebugKeyBindings {

    /** Null in production - always null-check before use. */
    public static KeyMapping generateDebugPens;

    /** Null in production - flashes the particle outline of nearby stalls. */
    public static KeyMapping showStalls;

    /**
     * <b>Registered in production too</b>, unlike the other two - the horse
     * highlight is a "where are my horses" toggle rather than a way to reach
     * anything a player should not have, and the bug testers are running real
     * jars and asked for it (owner, 2026-09-11). It appears in the Controls
     * menu and can be rebound like any other key.
     */
    public static KeyMapping highlightHorses;

    @SubscribeEvent
    static void register(RegisterKeyMappingsEvent event) {
        // The one binding a real build keeps. Registered before the production
        // check, so the early return below cannot take it with it.
        highlightHorses = new KeyMapping(
                "key.horsegenetics.highlight_horses",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                KeyMapping.Category.MISC
        );
        event.register(highlightHorses);

        if (FMLEnvironment.isProduction()) {
            return;
        }
        generateDebugPens = new KeyMapping(
                "key.horsegenetics.debug_pens",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                // 26.1.2's KeyMapping takes a KeyMapping.Category, not a lang key.
                // Reusing the built-in MISC category avoids registering (and
                // localizing) a custom one for a dev-only keybind.
                KeyMapping.Category.MISC
        );
        event.register(generateDebugPens);

        showStalls = new KeyMapping(
                "key.horsegenetics.show_stalls",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                KeyMapping.Category.MISC
        );
        event.register(showStalls);

    }

    private DebugKeyBindings() {
    }
}
