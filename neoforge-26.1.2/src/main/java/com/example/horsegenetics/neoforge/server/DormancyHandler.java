package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Dormancy;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * <b>Horses nobody is near tick one tick in {@link Dormancy#STRIDE}</b> (owner, 2026-10-06, lag audit #204).
 * The rule, and why nothing is lost by it, is {@code common/care/Dormancy}; this class only decides who is
 * dormant and skips their ticks.
 *
 * <p><b>How a tick is skipped.</b> {@link EntityTickEvent.Pre} is cancellable, and cancelling it skips
 * {@code Entity.tick()} whole - vanilla AI, movement and physics, and every mod {@code EntityTickEvent.Post}
 * handler - while {@code tickCount} still advances (read in the 26.1.2 patched sources:
 * {@code ServerLevel.tickNonPassenger} increments it before firing the event). That is what keeps the mod's
 * scans on time: see {@link Dormancy}.
 *
 * <p><b>Decided on run ticks only</b>, the ticks {@link Dormancy#runTick} picks, so a horse is asked about once
 * every {@code STRIDE} ticks whether it is awake or not, and wakes within that of a player arriving.
 *
 * <p><b>Never dormant:</b> a ridden or riding horse, a leashed one (its holder may be a mob that walks at full
 * speed - a cowboy's string - and a leash stretched past ten blocks breaks), and anything on a gametest server,
 * where nobody is ever near and every test would run in slow motion.
 *
 * <p><b>Not verified in-game.</b> The skipped ticks' effect on a horse in mid-air or in water (it falls and swims
 * a tenth as often) is untested.
 */
@EventBusSubscriber
public final class DormancyHandler {

    private DormancyHandler() {
    }

    /**
     * Horses that are dormant now, or whose last run stood for skipped ticks, by entity. Weak, so an unloaded
     * horse needs no eviction; server thread only, which is the only thread that ticks entities.
     */
    private static final Map<Horse, State> STATE = new WeakHashMap<>();

    /** {@code span} is how many ticks the current run stands for: itself, and the ticks skipped before it. */
    private static final class State {
        boolean dormant;
        int span = 1;
    }

    @SubscribeEvent
    static void onTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Horse horse) || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (Dormancy.runTick(horse.tickCount, horse.getId())) {
            State state = STATE.get(horse);
            boolean wasDormant = state != null && state.dormant;
            boolean dormant = shouldSleep(horse, level);
            if (!dormant && !wasDormant) {
                if (state != null) {
                    STATE.remove(horse);
                }
                return;
            }
            if (state == null) {
                state = new State();
                STATE.put(horse, state);
            }
            state.span = wasDormant ? Dormancy.STRIDE : 1;
            state.dormant = dormant;
            return; // a run tick always runs
        }
        State state = STATE.get(horse);
        if (state == null || !state.dormant) {
            return;
        }
        event.setCanceled(true);
        stepAge(horse);
    }

    /**
     * How many ticks this horse's current run stands for: {@link Dormancy#STRIDE} on the run of a horse that was
     * dormant, 1 otherwise. For world-clock beats - {@link Dormancy#beatWithin}.
     */
    static int span(Horse horse) {
        State state = STATE.get(horse);
        return state == null ? 1 : state.span;
    }

    private static boolean shouldSleep(Horse horse, ServerLevel level) {
        int radius = ServerConfig.dormancyRadius();
        if (radius <= 0 || horse.isVehicle() || horse.isPassenger() || horse.isLeashed()
                || level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer) {
            return false;
        }
        return !level.hasNearbyAlivePlayer(horse.getX(), horse.getY(), horse.getZ(), radius);
    }

    /**
     * Vanilla's own per-tick age step ({@code AgeableMob.aiStep}, server branch), for a tick that was skipped:
     * a foal grows up on time, and an adult's breeding cooldown runs down on time, whoever is watching.
     */
    private static void stepAge(Horse horse) {
        if (!horse.isAlive()) {
            return;
        }
        int age = horse.getAge();
        if (horse.canAgeUp()) {
            horse.setAge(age + 1);
        } else if (age > 0) {
            horse.setAge(age - 1);
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        STATE.clear();
    }
}
