package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.HurtNotice;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>"Your horse is being hurt", in the owner's chat and nobody else's.</b>
 *
 * <p>Owner's ask: <i>"When an owned horse takes damage, only its owner should
 * receive a chat message with the horse name, damage source, and, when
 * applicable, how to cure or prevent it. This should be configurable and on by
 * default."</i> The switch is {@code notices.owned_horse_damage}.
 *
 * <p>The words and the quiet period are {@link HurtNotice}, in the game-free
 * module, so they can be tested. This class is the translation: which horse,
 * whose, what hurt it, and has it been said recently.
 *
 * <h4>Only the owner, and only an owner who is here</h4>
 * {@code getOwner()} answers only for an owner loaded in the same level, which
 * is exactly the audience this message has: a line in the chat log of a player
 * who logged out an hour ago is not an alarm. An owner elsewhere in the world
 * hears nothing - that gap is the browser's Log tab to fill, not this one's.
 *
 * <h4>Not when the owner did it</h4>
 * A player who hits their own horse is looking at it. Telling them what they
 * just did, in red, is noise - and in the test yard it would be constant.
 *
 * <h4>Sunlight is not fire</h4>
 * The damage type alone cannot tell a dhampir burning in the open from a horse
 * standing in a campfire - {@link HorseNotices#causeOf} settles it, and names
 * the horse too, for this class and for {@link HorseDeathNoticeHandler}.
 */
@EventBusSubscriber
public final class HorseHurtNoticeHandler {

    private HorseHurtNoticeHandler() {
    }

    /** Horse UUID to the game tick its owner was last told, so one fire is one line. */
    private static final Map<UUID, Long> LAST_TOLD = new HashMap<>();

    /** Above this many remembered horses, the long-quiet ones are dropped on the next message. */
    private static final int SWEEP_ABOVE = 512;

    /** A horse quiet for five minutes is not coming back to spam anyone. */
    private static final long FORGET_AFTER = 6_000L;

    @SubscribeEvent
    static void onHorseHurt(LivingDamageEvent.Post event) {
        // Cheapest question first: this fires for every hit anything in the
        // world takes, and almost none of them are a horse.
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide()) {
            return;
        }
        if (!ServerConfig.damageNotices()) {
            return;
        }
        double damage = event.getHealthDamage();
        if (!HurtNotice.worthTelling(damage) || !horse.isTamed()) {
            return;
        }
        LivingEntity owner = horse.getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            return;         // wild, or an owner who is not here to be alarmed
        }
        Entity dealer = event.getSource().getEntity();
        if (dealer == player) {
            return;         // they are holding the sword; they know
        }
        long now = horse.level().getGameTime();
        Long last = LAST_TOLD.get(horse.getUUID());
        if (!HurtNotice.dueAgain(last == null ? -1L : last, now)) {
            return;
        }
        remember(horse.getUUID(), now);

        HurtNotice.Cause cause = HorseNotices.causeOf(horse, event.getSource().getMsgId());
        String attacker = dealer == null ? "" : dealer.getDisplayName().getString();
        String line = HurtNotice.line(HorseNotices.name(horse), cause, attacker,
                damage, horse.getHealth(), horse.getMaxHealth());
        player.sendSystemMessage(Component.literal(line).withStyle(
                HurtNotice.badlyHurt(horse.getHealth(), horse.getMaxHealth())
                        ? ChatFormatting.RED : ChatFormatting.YELLOW));
    }

    /** A dead horse never needs its quiet period again. */
    @SubscribeEvent
    static void onHorseDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse) {
            LAST_TOLD.remove(horse.getUUID());
        }
    }

    private static void remember(UUID horse, long now) {
        if (LAST_TOLD.size() > SWEEP_ABOVE) {
            LAST_TOLD.entrySet().removeIf(e -> now - e.getValue() > FORGET_AFTER);
        }
        LAST_TOLD.put(horse, now);
    }

}
