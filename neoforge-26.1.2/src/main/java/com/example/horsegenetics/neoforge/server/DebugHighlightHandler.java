package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>F8: a toggle that makes every horse near you glow, with the herd lead in
 * red.</b> Press once to turn it on - horses within {@value #RADIUS} blocks
 * glow, refreshed every {@value #REFRESH} ticks - press again to turn it off.
 * Session-only and dropped on logout.
 *
 * <h2>It ships in real builds</h2>
 * The other two debug keys (the pen generator, the stall overlay) are
 * {@code isProduction()}-gated because they reach things a player should not
 * have. This one does not: it makes horses the player can already see glow for
 * a few seconds. The bug testers run real jars and asked for it, so it is
 * registered like any ordinary keybind and appears in Controls.
 *
 * <h2>Why the lead is red, and how</h2>
 * "Which of these is the herd lead" has no surface at all otherwise - the alpha
 * is computed on demand and nothing displays it. A glow outline takes its
 * colour from the entity's <b>scoreboard team</b>, so the lead is put on a red
 * team for as long as it is lit and taken off again when the toggle goes off.
 * That also tints its name tag red, which is a bonus rather than a cost: it is
 * the same fact said twice.
 *
 * <p>A horse is the lead exactly when the herd id it stores is its <i>own</i>
 * UUID - every member records the lead's UUID, the lead included, which is the
 * same test {@code HerdManager} makes when it elects one.
 */
@EventBusSubscriber
public final class DebugHighlightHandler {

    private static final double RADIUS = 96.0;
    private static final int REFRESH = 40;

    /** How often the stale-outline sweep runs. Ten seconds, and only while the highlight is off. */
    private static final int REAP_INTERVAL = 200;

    private static final Set<UUID> ON = ConcurrentHashMap.newKeySet();

    /**
     * <b>The effect carries no particles and no status icon</b>
     * ({@code ambient/visible/showIcon} are all false, so the outline is the
     * only thing a glowing horse shows) and it is refreshed for as long as the
     * toggle is on. Left on by accident that is indistinguishable from a bug -
     * a playtester reported exactly that, having lit up a dimension full of
     * pens and no longer being able to tell why. So the toggle gives up on its
     * own after {@value #AUTO_OFF_TICKS} ticks and says so.
     */
    private static final int AUTO_OFF_TICKS = 4 * 60 * 20;   // four minutes

    /** Server tick at which each player's toggle expires. */
    private static final java.util.Map<UUID, Long> EXPIRES = new ConcurrentHashMap<>();

    /**
     * <b>Which horses each player has actually lit.</b>
     *
     * <p>This is the whole fix for the outline that would not go away, and the
     * bug it replaces is a mismatch rather than an oversight: {@link #glowNear}
     * lit horses <b>by proximity at the time</b> and {@code clearNear} cleared
     * them <b>by proximity later</b>. Ride away from a lit group, press F8 off,
     * and nothing ever visits them again - they are not near you any more, so
     * the sweep looks straight past them.
     *
     * <p>Ordinarily the effect would expire on its own in three seconds. It does
     * not, because <b>effect timers do not tick in unloaded chunks</b>: a horse
     * lit and then left behind holds the outline <i>frozen</i> until somebody
     * walks back into its chunk. That is exactly the shape the owner reported,
     * twice - "there's an area of the horse dimension where a bunch of horses
     * had an outline, F8 was not on", and then "there's STILL horses outlined
     * in the distance".
     *
     * <p>So: remember what was lit and clear that, which is the only set that
     * can be right.
     */
    private static final java.util.Map<UUID, Set<UUID>> LIT = new ConcurrentHashMap<>();

    /**
     * <b>Lit horses that were in an unloaded chunk when the sweep ran.</b>
     *
     * <p>Tracking is necessary but not sufficient: an entity in an unloaded
     * chunk cannot be reached to have its effect removed, which is the same
     * wall the plot teardown hit. The answer is the same shape - do it when
     * they come back. Anything left here is cleared by
     * {@link #onHorseLoaded} the moment it joins a level again, so the fix
     * survives a horse that is never revisited in this session, and survives a
     * restart, because the effect is re-applied by nothing and removed by the
     * first load.
     */
    private static final Set<UUID> PENDING_CLEAR = ConcurrentHashMap.newKeySet();

    private DebugHighlightHandler() {
    }

    /** The team whose colour the lead horse's glow outline takes. */
    private static final String LEAD_TEAM = "phc_herd_lead";

    /** Flip the toggle for one player (called from the F8 payload handler). */
    public static void toggle(ServerPlayer player) {
        EXPIRES.remove(player.getUUID());
        if (ON.remove(player.getUUID())) {
            safely(player, () -> clearNear(player));
            safely(player, () -> HorseDestinationDebug.clear(player));
            player.sendSystemMessage(Component.literal("Horse highlight OFF")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            ON.add(player.getUUID());
            if (player.level().getServer() != null) {
                EXPIRES.put(player.getUUID(),
                        (long) player.level().getServer().getTickCount() + AUTO_OFF_TICKS);
            }
            safely(player, () -> glowNear(player));
            // Immediately, rather than on the next multiple of PUSH_INTERVAL -
            // a debug key that takes half a second to do anything reads as a
            // debug key that did not work.
            safely(player, () -> HorseDestinationDebug.push(player, RADIUS));
            player.sendSystemMessage(Component.literal("Horse highlight ON - herd leads in ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("red").withStyle(ChatFormatting.RED))
                    .append(Component.literal(", lines to where each horse is walking ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("green").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("red").withStyle(ChatFormatting.RED))
                    .append(Component.literal(" for can/cannot reach. Press the key again to turn it off.")
                            .withStyle(ChatFormatting.GRAY)));
        }
    }

    /**
     * The red team, made on first use. {@code addPlayerTeam} throws if the name
     * is taken, so this asks first - the team survives in the world save once
     * created, and a second press must find the existing one rather than blow
     * up.
     */
    private static PlayerTeam leadTeam(ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(LEAD_TEAM);
        if (team == null) {
            team = scoreboard.addPlayerTeam(LEAD_TEAM);
            team.setColor(ChatFormatting.RED);
            team.setDisplayName(Component.literal("Herd lead"));
        }
        return team;
    }

    /** Every member records the lead's UUID - so the lead is the one recording its own. */
    private static boolean isHerdLead(Horse horse) {
        return horse.getData(ModAttachments.HORSE_CARE.get()).herd()
                .filter(lead -> lead.equals(horse.getUUID()))
                .isPresent();
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        // BEFORE the early-out, because the sweep's whole job is what happens
        // while the highlight is OFF - which is exactly when ON is empty.
        if (event.getServer().getTickCount() % REAP_INTERVAL == 0) {
            try {
                reapStaleGlow(event.getServer());
            } catch (RuntimeException e) {
                HorseGenetics.LOGGER.warn("[Debug] stale-glow sweep failed", e);
            }
        }
        if (ON.isEmpty()) {
            return;
        }
        long now = event.getServer().getTickCount();
        // Two cadences on one pass. The glow is refreshed slowly because it is
        // an effect with a duration; the destination lines are pushed four
        // times as often because they are a position, and a stale one points at
        // somewhere the horse has already given up on.
        boolean refreshGlow = now % REFRESH == 0;
        boolean pushDestinations = now % HorseDestinationDebug.PUSH_INTERVAL == 0;
        if (!refreshGlow && !pushDestinations) {
            return;
        }
        for (UUID id : ON) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
            if (player == null) {
                ON.remove(id);
                EXPIRES.remove(id);
                continue;
            }
            Long expires = EXPIRES.get(id);
            if (expires != null && now >= expires) {
                ON.remove(id);
                EXPIRES.remove(id);
                safely(player, () -> clearNear(player));
                safely(player, () -> HorseDestinationDebug.clear(player));
                player.sendSystemMessage(Component.literal(
                        "Horse highlight timed out.").withStyle(ChatFormatting.GRAY));
                continue;
            }
            if (refreshGlow) {
                safely(player, () -> glowNear(player));
            }
            if (pushDestinations) {
                safely(player, () -> HorseDestinationDebug.push(player, RADIUS));
            }
        }
    }

    /**
     * <b>A debug overlay may not take a world down.</b> This one did: a
     * scoreboard call that throws on an entry it was never given ran from the
     * tick loop and ended the server (see {@link #clearNear}). The call is
     * fixed, and this is the belt beside it - anything that still goes wrong in
     * here turns the highlight off for that player and says so, rather than
     * ending the tick loop for everybody.
     */
    private static void safely(ServerPlayer player, Runnable work) {
        try {
            work.run();
        } catch (RuntimeException e) {
            ON.remove(player.getUUID());
            EXPIRES.remove(player.getUUID());
            HorseGenetics.LOGGER.error("[Debug] horse highlight failed for {} - switching it off",
                    player.getGameProfile().name(), e);
            player.sendSystemMessage(Component.literal(
                    "Horse highlight hit an error and switched itself off.")
                    .withStyle(ChatFormatting.RED));
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ON.remove(event.getEntity().getUUID());
        EXPIRES.remove(event.getEntity().getUUID());
        // Logging out with the highlight ON used to strand every lit horse:
        // the register was dropped and nothing ever cleared them. Anything that
        // cannot be reached right now goes to PENDING_CLEAR and is dealt with
        // when it next loads - which may be after a restart, and still works.
        if (event.getEntity() instanceof ServerPlayer player) {
            safely(player, () -> clearNear(player));
        }
        Set<UUID> stranded = LIT.remove(event.getEntity().getUUID());
        if (stranded != null) {
            PENDING_CLEAR.addAll(stranded);
        }
    }

    private static void glowNear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        PlayerTeam lead = leadTeam(level);
        Set<UUID> lit = LIT.computeIfAbsent(player.getUUID(), id -> ConcurrentHashMap.newKeySet());
        for (Horse h : level.getEntitiesOfClass(Horse.class, player.getBoundingBox().inflate(RADIUS))) {
            h.addEffect(new MobEffectInstance(MobEffects.GLOWING, REFRESH + 20, 0, false, false, false));
            lit.add(h.getUUID());
            if (isHerdLead(h)) {
                level.getScoreboard().addPlayerToTeam(h.getStringUUID(), lead);
            }
        }
    }

    /**
     * <b>Ask before removing.</b> {@code Scoreboard.removePlayerFromTeam(name,
     * team)} <i>throws</i> when the entry is not on that team - it is written
     * for the {@code /team leave} command, where being on the team is the
     * precondition - and every horse here but a herd lead was never added.
     *
     * <p>That threw on the very first horse and took two things with it: the
     * exception escaped the packet handler on the OFF press, so the toggle
     * removed the player from {@link #ON}, never said "OFF", and read as "F8
     * only turns on"; and four minutes later the same line ran from
     * {@link #onServerTick} and <b>crashed the server</b>
     * (2026-09-12, owner's session). A debug overlay is not allowed to take a
     * world down, which is also why the tick that calls this is wrapped.
     */
    private static void clearNear(ServerPlayer player) {
        if (player.level().getServer() == null) {
            return;
        }
        Set<UUID> lit = LIT.remove(player.getUUID());
        if (lit == null) {
            return;
        }
        for (UUID id : lit) {
            Entity found = null;
            for (ServerLevel candidate : player.level().getServer().getAllLevels()) {
                found = candidate.getEntity(id);
                if (found != null) {
                    break;
                }
            }
            if (found instanceof Horse horse) {
                unlight(horse);
            } else {
                // Unloaded. It cannot be reached now and its effect timer is
                // frozen, so it would hold the outline indefinitely - hand it
                // to onHorseLoaded instead of dropping it.
                PENDING_CLEAR.add(id);
            }
        }
    }

    /**
     * <b>Take the outline off one horse</b>, wherever it turned up.
     *
     * <p>The team half is not optional and not symmetric with the effect:
     * {@code Scoreboard.removePlayerFromTeam} <b>throws</b> when the entry is
     * not on that team - it is written for the {@code /team leave} command,
     * where membership is the precondition - and every horse here but a herd
     * lead was never added. That threw on the very first horse once and took
     * two things with it: the exception escaped the packet handler on the OFF
     * press, so the toggle removed the player from {@link #ON}, never said
     * "OFF", and read as "F8 only turns on"; and four minutes later the same
     * line ran from {@link #onServerTick} and <b>crashed the server</b>
     * (2026-09-12, owner's session). A debug overlay is not allowed to take a
     * world down, which is also why the tick that calls this is wrapped.
     */
    private static void unlight(Horse horse) {
        horse.removeEffect(MobEffects.GLOWING);
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam lead = leadTeam(level);
        if (scoreboard.getPlayersTeam(horse.getStringUUID()) == lead) {
            scoreboard.removePlayerFromTeam(horse.getStringUUID(), lead);
        }
    }

    /**
     * <b>And a reaper for the ones already stranded.</b>
     *
     * <p>Tracking only helps horses lit <em>after</em> the fix. The owner's
     * world already holds an unknown number wearing a frozen outline from
     * before it, scattered wherever she happened to ride - and telling her the
     * bug is fixed while she can still see them is not fixing it.
     *
     * <p>Safe to do bluntly because of one fact: <b>this mod applies
     * {@code GLOWING} in exactly one place</b>, {@link #glowNear}. So a horse
     * wearing it while <em>nobody</em> has the highlight on is stale by
     * definition, whatever lit it and whenever. Gated on {@link #ON} being
     * empty, so it costs nothing during normal use and cannot fight the feature
     * it is cleaning up after.
     */
    private static void reapStaleGlow(MinecraftServer server) {
        if (!ON.isEmpty()) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Horse horse : level.getEntities(EntityType.HORSE,
                    h -> h.hasEffect(MobEffects.GLOWING))) {
                unlight(horse);
            }
        }
    }

    /**
     * <b>The other half of the fix: clear on the way back in.</b> A horse that
     * was in an unloaded chunk when the highlight went off is unreachable at
     * that moment and its effect timer is frozen, so it holds the outline until
     * something touches it. This is that something.
     */
    @SubscribeEvent
    static void onHorseLoaded(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || PENDING_CLEAR.isEmpty()) {
            return;
        }
        if (event.getEntity() instanceof Horse horse && PENDING_CLEAR.remove(horse.getUUID())) {
            unlight(horse);
        }
    }
}
