package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * The <b>only</b> thing done at spawn time: mark a horse that came from a
 * <b>natural</b> spawn (or chunk generation, or a mob spawner) so that
 * {@link HerdManager}, a tick later, knows to try to build it into a herd
 * rather than leave it a lone Unknown.
 *
 * <p>It does not touch the pack {@link net.minecraft.world.entity.SpawnGroupData}
 * &mdash; {@code Horse.finalizeSpawn} replaces any custom one with its own
 * {@code Horse.HorseGroupData} between every pack member, so that route can't
 * carry anything. {@link HerdManager} groups the pack by <b>proximity</b>
 * instead.
 */
@EventBusSubscriber
public final class BreedSpawnHandler {

    /** NBT flag: this horse came from a natural spawn and should try to form a herd. */
    public static final String WILD_SPAWN_KEY = "horsegenetics:wild_spawn";

    private BreedSpawnHandler() {
    }

    @SubscribeEvent
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        EntitySpawnReason reason = event.getSpawnType();
        if (reason == EntitySpawnReason.NATURAL
                || reason == EntitySpawnReason.CHUNK_GENERATION
                || reason == EntitySpawnReason.SPAWNER) {
            horse.getPersistentData().putBoolean(WILD_SPAWN_KEY, true);
        }
    }
}
