package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.NeoRng;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Founds a wild horse's record / coat / breed / herd, and re-resolves a
 * reloaded or bred horse's body, from the <b>entity tick</b> - never from the
 * join event or a {@code server.execute} task.
 *
 * <h2>Why the tick, and not {@code server.execute}</h2>
 * {@link HorseRecords#applyTraitsToEntity} writes {@code Attributes.SCALE}, and
 * a non-1.0 scale makes {@code Entity.refreshDimensions()} run a collision scan
 * that can force-load a chunk. Do that anywhere that is interleaved with the
 * chunk system's ticket pass - inside {@code EntityJoinLevelEvent} (which fires
 * mid {@code DistanceManager.runAllUpdates} during chunk promotion), or inside a
 * {@code server.execute} task (drained by {@code MinecraftServer.waitUntilNextTick}
 * right next to {@code ServerChunkCache.pollTask}) - and the re-entry corrupts
 * the ticket-set iterator: a hard {@code NullPointerException} crash in
 * {@code DistanceManager.runAllUpdates}. Breeds made it reliable, because a
 * breed pins the body-size locus homozygous, so every Fjord / Percheron /
 * Falabella gets a real scale change on its first resolve.
 *
 * <p>{@code EntityTickEvent.Post} fires from {@code ServerLevel.tick}'s entity
 * loop, <i>after</i> the tick's chunk updates are done and with the horse's
 * surroundings already loaded, so {@code refreshDimensions} touches nothing that
 * re-enters. Each horse is handled once per level load; {@link EntityLeaveLevelEvent}
 * forgets it so a reload re-resolves.
 *
 * <h2>Rationed per tick</h2>
 * One founding is not cheap - a clump scan, a breed pick against the biome, a
 * genome, a body resolve and a coat sync - and on 2026-09-13 the log put it at
 * <b>10 to 24 ms each</b>, measured from consecutive founders in one burst.
 * Loading into fresh terrain founds a herd a chunk at a time and a world reload
 * re-resolves every horse at once, so a single tick could spend several hundred
 * milliseconds here. Each tick now gets {@link #FOUNDING_BUDGET_NANOS} across
 * every dimension; a horse over budget is simply not marked handled and comes
 * back next tick. Always at least one per tick, so a slow founding can never
 * stall the queue.
 */
@EventBusSubscriber
public final class HorseFoundingTickHandler {

    /**
     * Ticks a natural-spawn horse waits before founding, so its whole pack is
     * loaded and query-visible first (~1s). A horse that already has a record,
     * or a non-natural spawn, is handled on its first tick.
     */
    private static final int WILD_FOUND_DELAY_TICKS = 20;

    /** Persistent counter: ticks a flagged wild horse has waited to be founded. */
    private static final String WAIT_KEY = "horsegenetics:wild_spawn_wait";

    /**
     * How much of one server tick founding may spend, across every dimension.
     * A tick is 50 ms and this mod is not the only thing in it, so fifteen
     * leaves the rest of the world room - at the measured cost that is one or
     * two horses a tick, which founds a herd of eight in well under a second.
     */
    private static final long FOUNDING_BUDGET_NANOS = 15_000_000L;

    /** How often, at most, the deferral summary is written. Five seconds. */
    private static final int REPORT_EVERY_TICKS = 100;

    /** Entities handled this session (network id). Cleared on leave. */
    private static final IntOpenHashSet HANDLED = new IntOpenHashSet();

    // Server thread only.
    private static int budgetTick = Integer.MIN_VALUE;
    private static long spentThisTick;
    private static boolean foundedThisTick;

    private static int lastReportTick = Integer.MIN_VALUE;
    private static int foundedSinceReport;
    private static int deferredSinceReport;
    private static long slowestSinceReport;

    private HorseFoundingTickHandler() {
    }

    @SubscribeEvent
    static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (HANDLED.contains(horse.getId())) {
            return;
        }

        if (HorseRecords.hasRealRecord(horse)) {
            // Reloaded or freshly bred: register, name, fill epigenome, re-resolve
            // the body, sync the coat. Safe here - not so from server.execute.
            if (!mayFound(level)) {
                return;
            }
            long started = System.nanoTime();
            HANDLED.add(horse.getId());
            horse.getPersistentData().remove(WAIT_KEY);
            horse.getPersistentData().remove(BreedSpawnHandler.WILD_SPAWN_KEY);
            HorseGeneticsEventHandler.ensureExistingRecordResolved(horse);
            charge(level, System.nanoTime() - started);
            return;
        }

        CompoundTag data = horse.getPersistentData();
        if (data.getBooleanOr(BreedSpawnHandler.WILD_SPAWN_KEY, false)) {
            // A natural spawn: let the pack settle, then found the herd.
            int waited = data.getIntOr(WAIT_KEY, 0) + 1;
            if (waited < WILD_FOUND_DELAY_TICKS) {
                data.putInt(WAIT_KEY, waited);
                return;
            }
            // Checked BEFORE the wait key is cleared, so a deferred horse stays
            // due: its counter still reads one short of the delay and the next
            // tick lands on it again.
            if (!mayFound(level)) {
                return;
            }
            long started = System.nanoTime();
            data.remove(WAIT_KEY);
            HANDLED.add(horse.getId());
            HerdManager.assignFounder(horse, new NeoRng(horse.getRandom()));
            charge(level, System.nanoTime() - started);
            return;
        }

        // No record, not a natural spawn (/summon, an imported horse): a lone
        // Feral Mixed, founded as soon as the budget allows - there is no pack
        // coming for it.
        if (!mayFound(level)) {
            return;
        }
        long started = System.nanoTime();
        HANDLED.add(horse.getId());
        HerdManager.assignFounder(horse, new NeoRng(horse.getRandom()));
        charge(level, System.nanoTime() - started);
    }

    /**
     * May another horse be founded this tick? Always the first, then only while
     * under {@link #FOUNDING_BUDGET_NANOS}. A refusal is counted, and nothing
     * else happens: the horse is not marked handled, so it is back next tick.
     *
     * <p>A herd spread over several ticks still agrees with itself - that was
     * true before this, since every member was already founded on its own tick
     * and {@code HerdManager} derives the herd from the lead's UUID rather than
     * from who happened to go first.
     */
    private static boolean mayFound(ServerLevel level) {
        int tick = level.getServer().getTickCount();
        if (tick != budgetTick) {
            budgetTick = tick;
            spentThisTick = 0L;
            foundedThisTick = false;
        }
        if (!foundedThisTick || spentThisTick < FOUNDING_BUDGET_NANOS) {
            return true;
        }
        deferredSinceReport++;
        return false;
    }

    private static void charge(ServerLevel level, long nanos) {
        foundedThisTick = true;
        spentThisTick += nanos;
        foundedSinceReport++;
        slowestSinceReport = Math.max(slowestSinceReport, nanos);
        int tick = level.getServer().getTickCount();
        // Only when the budget actually had to act, and at most every five
        // seconds - a line per deferral would be the heal-spam mistake again.
        if (deferredSinceReport > 0 && tick - lastReportTick >= REPORT_EVERY_TICKS) {
            DebugAnnounce.log("Founding", String.format(
                    "%d horses founded, %d founding(s) put off to a later tick since the last report; "
                            + "slowest %.1f ms (budget %d ms per tick)",
                    foundedSinceReport, deferredSinceReport, slowestSinceReport / 1_000_000.0,
                    FOUNDING_BUDGET_NANOS / 1_000_000L));
            lastReportTick = tick;
            foundedSinceReport = 0;
            deferredSinceReport = 0;
            slowestSinceReport = 0L;
        }
    }

    @SubscribeEvent
    static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Horse horse) {
            HANDLED.remove(horse.getId());
        }
    }

    /** Don't leak ids or budget state across a singleplayer world reload in the same JVM. */
    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        HANDLED.clear();
        budgetTick = Integer.MIN_VALUE;
        spentThisTick = 0L;
        foundedThisTick = false;
        lastReportTick = Integer.MIN_VALUE;
        foundedSinceReport = 0;
        deferredSinceReport = 0;
        slowestSinceReport = 0L;
    }
}
