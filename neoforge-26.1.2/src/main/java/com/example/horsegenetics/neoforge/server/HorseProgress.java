package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.data.HorseProgressData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
 *
 * <h2>Tasks nobody performs</h2>
 * The wild chapter is largely horses doing things to <i>each other</i> - two
 * stallions sparring, a mare being driven back into her band, a pair standing
 * head to tail - and there is no player action to hang those on.
 * {@link #completeForWatcher} is the second shape of hook for exactly those:
 * it credits a player who was near enough, and looking, to have seen it.
 *
 * <p>Two deliberate choices in it. It credits <b>the nearest player only</b>,
 * not everyone in range, because these fire from goals that tick repeatedly and
 * a sweep of every player on a busy server is a cost paid for a checkbox. And
 * it requires <b>line of sight</b>, because a box that ticks for something that
 * happened behind a hill teaches the player nothing and quietly lies about what
 * they have seen - the whole point of these tasks is that you watched it.
 */
public final class HorseProgress {

    /**
     * How near a player has to be to be credited with watching something.
     *
     * <p>Twenty-four blocks, which is not an arbitrary number: it is the radius
     * over which a hurt wild horse alerts its own band
     * ({@code HorseAggroHandler.HERD_ALERT_RADIUS}), so it is already this
     * mod's answer to "near enough for a horse to notice". Far enough to take
     * in a whole band from outside it, close enough that the horses are more
     * than dots.
     */
    public static final double WATCH_RADIUS = 24.0;

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

    /**
     * Tick {@code task} off for whoever was watching {@code seen} happen, from
     * within {@link #WATCH_RADIUS}. Safe with anything, including a null entity
     * or a client-side one.
     */
    public static void completeForWatcher(Entity seen, ProgressTask task) {
        completeForWatcher(seen, WATCH_RADIUS, task);
    }

    /**
     * As {@link #completeForWatcher(Entity, ProgressTask)}, with a reach of your
     * own - for something that can only sensibly be seen from close up, or one
     * that carries a long way.
     */
    public static void completeForWatcher(Entity seen, double radius, ProgressTask task) {
        if (seen == null || task == null) {
            return;
        }
        if (!(seen.level() instanceof ServerLevel level)) {
            return;
        }
        Player near = level.getNearestPlayer(seen, radius);
        if (!(near instanceof ServerPlayer watcher)) {
            return;
        }
        // UNVERIFIED in NeoForge 26.1.2: hasLineOfSight(Entity) is being called
        // here off LivingEntity rather than Entity. If it has moved, the fix is
        // the clear-sight raycast and not dropping the test - a box that ticks
        // through a hillside is the failure this guard exists for.
        if (!watcher.hasLineOfSight(seen)) {
            return;
        }
        complete(watcher, task);
    }
}
