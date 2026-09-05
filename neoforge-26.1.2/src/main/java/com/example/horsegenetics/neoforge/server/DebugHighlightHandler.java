package com.example.horsegenetics.neoforge.server;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dev tool (F8): a <b>toggle</b> that makes every horse near you glow so you can
 * find your herds. Press once to turn it on - horses within
 * {@value #RADIUS} blocks glow, refreshed every {@value #REFRESH} ticks - press
 * again to turn it off. Session-only, dropped on logout, dev builds only.
 */
@EventBusSubscriber
public final class DebugHighlightHandler {

    private static final double RADIUS = 96.0;
    private static final int REFRESH = 40;

    private static final Set<UUID> ON = ConcurrentHashMap.newKeySet();

    private DebugHighlightHandler() {
    }

    /** Flip the toggle for one player (called from the F8 payload handler). */
    public static void toggle(ServerPlayer player) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        if (ON.remove(player.getUUID())) {
            clearNear(player);
            player.sendSystemMessage(Component.literal("[debug] horse highlight OFF"));
        } else {
            ON.add(player.getUUID());
            glowNear(player);
            player.sendSystemMessage(Component.literal("[debug] horse highlight ON"));
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (ON.isEmpty()) {
            return;
        }
        if (event.getServer().getTickCount() % REFRESH != 0) {
            return;
        }
        for (UUID id : ON) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
            if (player == null) {
                ON.remove(id);
            } else {
                glowNear(player);
            }
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ON.remove(event.getEntity().getUUID());
    }

    private static void glowNear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (Horse h : level.getEntitiesOfClass(Horse.class, player.getBoundingBox().inflate(RADIUS))) {
            h.addEffect(new MobEffectInstance(MobEffects.GLOWING, REFRESH + 20, 0, false, false, false));
        }
    }

    private static void clearNear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (Horse h : level.getEntitiesOfClass(Horse.class, player.getBoundingBox().inflate(RADIUS + 32))) {
            h.removeEffect(MobEffects.GLOWING);
        }
    }
}
