package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.NeoRng;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.nbt.CompoundTag;
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

    /** Entities handled this session (network id). Cleared on leave. */
    private static final IntOpenHashSet HANDLED = new IntOpenHashSet();

    private HorseFoundingTickHandler() {
    }

    @SubscribeEvent
    static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (horse.level().isClientSide()) {
            return;
        }
        if (HANDLED.contains(horse.getId())) {
            return;
        }

        if (HorseRecords.hasRealRecord(horse)) {
            // Reloaded or freshly bred: register, name, fill epigenome, re-resolve
            // the body, sync the coat. Safe here - not so from server.execute.
            HANDLED.add(horse.getId());
            horse.getPersistentData().remove(WAIT_KEY);
            horse.getPersistentData().remove(BreedSpawnHandler.WILD_SPAWN_KEY);
            HorseGeneticsEventHandler.ensureExistingRecordResolved(horse);
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
            data.remove(WAIT_KEY);
            HANDLED.add(horse.getId());
            HerdManager.assignFounder(horse, new NeoRng(horse.getRandom()));
            return;
        }

        // No record, not a natural spawn (/summon, an imported horse): a lone
        // Unknown, founded immediately - there is no pack coming for it.
        HANDLED.add(horse.getId());
        HerdManager.assignFounder(horse, new NeoRng(horse.getRandom()));
    }

    @SubscribeEvent
    static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Horse horse) {
            HANDLED.remove(horse.getId());
        }
    }

    /** Don't leak ids across a singleplayer world reload in the same JVM. */
    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        HANDLED.clear();
    }
}
