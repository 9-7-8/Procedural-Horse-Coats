package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breeds;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * The two things done at spawn time, for a horse that came from a
 * <b>natural</b> spawn (or chunk generation, or a mob spawner):
 * <ul>
 *   <li><b>Refuse it</b> when the world's breed settings leave nothing it could
 *       be - no breed of this biome at this hour, and no Feral Mixed here
 *       ({@code Breeds.anythingMaySpawn}). A world run on its owner's breeds
 *       alone has no horses where those breeds do not live. What gets past this
 *       - a lone horse where Feral Mixed is off - is discarded by
 *       {@link HerdManager} before it is founded.</li>
 *   <li>Otherwise <b>mark it</b> so that {@link HerdManager}, a tick later,
 *       knows to try to build it into a herd rather than leave it a lone Feral
 *       Mixed.</li>
 * </ul>
 * A {@code /summon} or a vanilla spawn egg is neither: a player asked for that
 * horse, and it is a Feral Mixed whatever the settings say.
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
            String biome = event.getLevel().getBiome(horse.blockPosition()).unwrapKey()
                    .map(k -> k.identifier().toString()).orElse("");
            if (!Breeds.anythingMaySpawn(biome, event.getLevel().getLevel().isDarkOutside())) {
                // FinalizeSpawnEvent's own cancel: NeoForge drops the mob before
                // it joins the level. Unverified for CHUNK_GENERATION in-game;
                // if one slips through it still carries WILD_SPAWN_KEY, and
                // HerdManager discards it a second later for the same reason.
                event.setSpawnCancelled(true);
            }
            horse.getPersistentData().putBoolean(WILD_SPAWN_KEY, true);
        }
    }
}
