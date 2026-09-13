package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseWhereabouts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Keeps {@link HorseWhereabouts} current: a sighting when a tamed horse joins or
 * leaves a level and on a slow stagger in between, and a death when it dies.
 *
 * <p>Tamed horses only. A wild horse cannot have a whistle bound to it, and the
 * horse dimension spawns horses by the hundred.
 */
@EventBusSubscriber
public final class HorseWhereaboutsHandler {

    private HorseWhereaboutsHandler() {
    }

    /** Ten seconds. The index only needs to be near enough that the named chunk holds the horse. */
    private static final int SIGHTING_INTERVAL = 200;

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof AbstractHorse horse
                && horse.isTamed()) {
            sight(level, horse);
        }
    }

    @SubscribeEvent
    static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof AbstractHorse horse
                && horse.isTamed() && horse.isAlive()) {
            sight(level, horse);
        }
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || !horse.isTamed()) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SIGHTING_INTERVAL != 0) {
            return;
        }
        sight(level, horse);
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse && horse.level() instanceof ServerLevel level) {
            HorseWhereabouts.get(level.getServer())
                    .died(horse.getUUID(), level.dimension(), horse.blockPosition());
        }
    }

    private static void sight(ServerLevel level, AbstractHorse horse) {
        HorseWhereabouts.get(level.getServer()).seen(horse.getUUID(), level.dimension(), horse.blockPosition());
    }
}
