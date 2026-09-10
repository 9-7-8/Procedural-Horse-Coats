package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.data.HorseProgressData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * <b>The one line a handler writes to tick a checklist task off.</b>
 *
 * <p>Every hook in this mod that completes a task calls
 * {@code HorseProgress.complete(player, ProgressTask.X)} and nothing else - no
 * null checks, no side lookup, no "has it already". This exists so that adding
 * a task to a handler is a single line that cannot be got wrong, because it will
 * be done a dozen times in a dozen files and the twelfth one will be careless.
 *
 * <p>It swallows everything: a client-side player, a null, a world with no
 * server. A checklist is not worth a crash, and a handler that has to guard
 * before calling is a handler that will eventually forget to.
 */
public final class HorseProgress {

    private HorseProgress() {
    }

    /** Tick {@code task} off for {@code player}. Safe with anything. */
    public static void complete(Player player, ProgressTask task) {
        if (!(player instanceof ServerPlayer serverPlayer) || task == null) {
            return;
        }
        if (!(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        HorseProgressData.get(server).complete(serverPlayer, task);
    }
}
