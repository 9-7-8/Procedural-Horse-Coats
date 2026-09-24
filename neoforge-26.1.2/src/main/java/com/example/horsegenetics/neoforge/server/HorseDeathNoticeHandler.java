package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.DeathNotice;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>"Somebody's horse just died", in everybody's chat.</b>
 *
 * <p>Owner's ask: <i>"When an owned horse dies, post a server-wide death
 * message. This should be configurable and on by default."</i> The switch is
 * {@code notices.owned_horse_death}.
 *
 * <p>The sentence is {@link DeathNotice}, in the game-free module, so a test can
 * pin the wording down. This class is the translation: which horse, whose, what
 * killed it, and has it already been said.
 *
 * <h4>Vanilla says nothing at all here</h4>
 * {@code TamableAnimal.die} tells an owner their pet died, but
 * {@code AbstractHorse} extends {@code Animal} and is only
 * {@code OwnableEntity}, so that block never runs for a horse. There is no
 * vanilla line to duplicate or to suppress - this is the only one.
 *
 * <h4>Everybody, but the owner in red</h4>
 * Server-wide is the ask: a stable hand who finds the body should not have to
 * work out whose mare it was. The owner's copy is red because on a server with a
 * hundred horses the one line that matters to them is their own, and it is worth
 * being able to find it in the scroll.
 *
 * <h4>Once</h4>
 * The event fires from {@code LivingEntity.die} <i>before</i> vanilla's own
 * already-dead guard, so a second {@code die()} in the same tick would say it
 * twice. {@link #ANNOUNCED} is the guard, swept the same way the hurt notice
 * sweeps its quiet periods.
 *
 * <h4>The game rule still rules</h4>
 * A server that turned {@code showDeathMessages} off has said what it wants of
 * its chat, and a horse is not an exception to it. That is vanilla's own order
 * of business in {@code TamableAnimal.die}, kept here.
 */
@EventBusSubscriber
public final class HorseDeathNoticeHandler {

    private HorseDeathNoticeHandler() {
    }

    /** Horse UUID to the game tick its death was announced, so one death is one line. */
    private static final Map<UUID, Long> ANNOUNCED = new HashMap<>();

    /** Above this many remembered horses, the long-dead ones are dropped on the next death. */
    private static final int SWEEP_ABOVE = 512;

    /** Nothing dies twice five minutes apart; anything older than this is a corpse nobody is announcing again. */
    private static final long FORGET_AFTER = 6_000L;

    @SubscribeEvent
    static void onHorseDied(LivingDeathEvent event) {
        // Cheapest question first: every mob in the world dies through here.
        if (!(event.getEntity() instanceof AbstractHorse horse)
                || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (!ServerConfig.deathNotices() || !horse.isTamed()) {
            return;
        }
        if (!level.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES)) {
            return;
        }
        var ownerRef = horse.getOwnerReference();
        if (ownerRef == null) {
            return;         // tamed by nobody the horse still remembers
        }
        long now = level.getGameTime();
        if (alreadySaid(horse.getUUID(), now)) {
            return;
        }

        Entity killer = event.getSource().getEntity();
        String line = DeathNotice.line(HorseNotices.name(horse),
                HorseOwnership.ownerName(horse).orElse(""),
                HorseNotices.causeOf(horse, event.getSource().getMsgId()),
                killer == null ? "" : killer.getDisplayName().getString());

        Component toTheOwner = Component.literal(line).withStyle(ChatFormatting.RED);
        Component toEveryoneElse = Component.literal(line).withStyle(ChatFormatting.GRAY);
        UUID owner = ownerRef.getUUID();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(player.getUUID().equals(owner) ? toTheOwner : toEveryoneElse);
        }
    }

    /** Has this death already been announced - and remember it if not. */
    private static boolean alreadySaid(UUID horse, long now) {
        Long said = ANNOUNCED.get(horse);
        if (said != null && now - said < FORGET_AFTER && now >= said) {
            return true;
        }
        if (ANNOUNCED.size() > SWEEP_ABOVE) {
            ANNOUNCED.entrySet().removeIf(e -> now - e.getValue() > FORGET_AFTER);
        }
        ANNOUNCED.put(horse, now);
        return false;
    }
}
