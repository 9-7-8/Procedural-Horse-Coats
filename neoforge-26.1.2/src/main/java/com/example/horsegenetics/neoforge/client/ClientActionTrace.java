package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.server.ActionTrace;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * <b>The client half of {@link ActionTrace}</b>: which of this mod's screens
 * you opened, and which of its buttons you pressed.
 *
 * <p>It exists because of a specific failure. "Make egg does nothing" was
 * reported with <i>no evidence of any kind</i> - no egg, no refusal, and
 * nothing in the log, because the click never reached the code that logs. The
 * cause turned out to be another widget sitting on top of the button and
 * eating the press, and the only reason it took a diagnosis rather than a
 * glance is that a button press left no trace at all.
 *
 * <p>So the mod's screens say when they open and when one of their buttons is
 * pressed. A button that is <i>covered</i> then produces silence where every
 * other button produces a line - which turns "it does nothing" from a mystery
 * into a reading.
 *
 * <p>In singleplayer the client and the integrated server write the same log
 * file, so these lines interleave with the server-side ones and a session reads
 * in order.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = HorseGenetics.MOD_ID)
public final class ClientActionTrace {

    private ClientActionTrace() {
    }

    /**
     * Screens opening. Vanilla's own are skipped - this is a trace of the mod,
     * and "the player opened their inventory" is noise in it.
     */
    @SubscribeEvent
    static void onScreenOpen(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        String name = screen.getClass().getName();
        if (!name.startsWith("com.example.horsegenetics")) {
            return;
        }
        ActionTrace.log("screen open", screen.getClass().getSimpleName()
                + " (" + screen.width + "x" + screen.height + " gui px)");
    }

    /**
     * <b>Say that a button was pressed, from inside the button.</b> Call this
     * from a mod screen's button handlers.
     *
     * <p>Deliberately called by the handler rather than hooked from outside: a
     * press that never reaches a handler is the thing worth seeing, and a hook
     * that reported presses the screen never received would report exactly the
     * press that is broken as if it had worked.
     */
    public static void button(String screen, String label) {
        ActionTrace.log("button", screen + ": " + label);
    }
}
