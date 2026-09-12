package com.example.horsegenetics.neoforge.server;

import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.equine.Horse;
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

    private static final Set<UUID> ON = ConcurrentHashMap.newKeySet();

    private DebugHighlightHandler() {
    }

    /** The team whose colour the lead horse's glow outline takes. */
    private static final String LEAD_TEAM = "phc_herd_lead";

    /** Flip the toggle for one player (called from the F8 payload handler). */
    public static void toggle(ServerPlayer player) {
        if (ON.remove(player.getUUID())) {
            clearNear(player);
            player.sendSystemMessage(Component.literal("Horse highlight OFF")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            ON.add(player.getUUID());
            glowNear(player);
            player.sendSystemMessage(Component.literal("Horse highlight ON - herd leads in ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("red").withStyle(ChatFormatting.RED)));
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
        PlayerTeam lead = leadTeam(level);
        for (Horse h : level.getEntitiesOfClass(Horse.class, player.getBoundingBox().inflate(RADIUS))) {
            h.addEffect(new MobEffectInstance(MobEffects.GLOWING, REFRESH + 20, 0, false, false, false));
            if (isHerdLead(h)) {
                level.getScoreboard().addPlayerToTeam(h.getStringUUID(), lead);
            }
        }
    }

    private static void clearNear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (Horse h : level.getEntitiesOfClass(Horse.class, player.getBoundingBox().inflate(RADIUS + 32))) {
            h.removeEffect(MobEffects.GLOWING);
            // Off the team as well, so a lead does not keep a red name after
            // the highlight is off.
            level.getScoreboard().removePlayerFromTeam(h.getStringUUID(), leadTeam(level));
        }
    }
}
