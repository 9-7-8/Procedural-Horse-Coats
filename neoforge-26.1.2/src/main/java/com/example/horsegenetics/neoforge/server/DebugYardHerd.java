package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


/**
 * <b>The yard's clock</b> - {@link #after} runs a step a given number of ticks from now, and every timed
 * pen in the yard uses it; {@link #cancel} forgets them all when the plot is torn down or the server stops.
 *
 * <p>This class also built rows Q and R, band life with nobody at the controls (2026-09-14). Every one
 * of those pens - LEAVING HOME, HOST BAND, LEADERLESS, GROOMING, TAKEOVER, DAM DEFENCE - was answered
 * (horse-care.html#verified-bands) and deleted on 2026-09-30. The clock stayed where it was, rather than
 * moving and taking every caller with it.
 */
@EventBusSubscriber
final class DebugYardHerd {

    private DebugYardHerd() {
    }


    // ------------------------------------------------------------------
    // The clock
    // ------------------------------------------------------------------

    private record Task(long due, Runnable run) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static @Nullable ServerLevel taskLevel;

    /** Forget every pending step - the yard was torn down, or the server is stopping. */
    static void cancel() {
        TASKS.clear();
        taskLevel = null;
    }

    /** Run {@code run} {@code ticks} game ticks from now. The breeding rows use it too. */
    static void after(ServerLevel level, long ticks, Runnable run) {
        taskLevel = level;
        TASKS.add(new Task(level.getGameTime() + ticks, run));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = taskLevel;
        if (level == null || TASKS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.due() <= now) {
                due.add(t);
                return true;
            }
            return false;
        });
        for (Task t : due) {
            try {
                t.run().run();
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] herd rows: a timed step failed", e);
            }
        }
    }

    @SubscribeEvent
    static void onStopping(ServerStoppingEvent event) {
        cancel();
    }

    // ------------------------------------------------------------------
    // Build
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Row R - dispersal and mare transfers; a band that loses its stallion; sparring
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Row S - a takeover; a dam defending her foal; displacement
    // ------------------------------------------------------------------

    // DISPLACEMENT (four ranked mares at hay, with its hay restock) was deleted on 2026-09-15 as
    // verified: overnight all 19 "displaced ... at food or water" lines put the higher rank first.

    // ------------------------------------------------------------------
    // Row Q east - grooming
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

}
