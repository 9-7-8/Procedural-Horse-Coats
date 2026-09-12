package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.ClientConfig;
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
 * Debug hook, registered only when {@link ClientConfig#debugTools()} says so -
 * on by default in a dev run, off by default in a normal install, and
 * switchable in either. When it is off the keybind is simply never registered:
 * it won't appear in the Controls menu, can't be rebound, and there is no way
 * for a player to trigger it.
 *
 * <p>It was {@code FMLEnvironment.isProduction()} until testing moved onto a
 * release build running against a real server, at which point "only exists
 * under runClient" meant "gone for the person doing the testing". See the
 * {@code debug.tools} section of {@link com.example.horsegenetics.neoforge.ServerConfig}.
 *
 * <p>The server-side handler in ModNetworking re-checks the <i>server's</i>
 * {@code debug.tools} independently, so a modified or forged client packet
 * against a server that has not switched them on still won't do anything.
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

        if (!ClientConfig.debugTools()) {
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
