package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.HurtNotice;
import com.example.horsegenetics.common.log.HorseEvent;
import com.example.horsegenetics.common.repro.CoverNotice;
import com.example.horsegenetics.common.repro.NaturalCover;
import com.example.horsegenetics.neoforge.data.HorseEventData;
import com.example.horsegenetics.neoforge.network.HorseLogPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * <b>Writes the browser's Log tab.</b> The translation layer between the things
 * that happen in a world and the game-free {@link HorseEvent} rows
 * {@link HorseEventData} keeps.
 *
 * <p>Every method here does the same three things: work out <i>whose</i> event
 * this is, reduce it to plain data, and hand it to the store. The rules about
 * what is a repeat, what order rows come back in and how many are kept are all
 * in {@code common}; nothing here throttles, because a second throttle beside
 * that one is a second thing to get wrong.
 *
 * <h2>It records for an owner who is not there</h2>
 * That is the whole point, and it is the one respect in which this differs from
 * every chat notice in the mod. {@code HorseDeathNoticeHandler} and
 * {@code HorseHurtNoticeHandler} deliberately say nothing to an owner who is
 * offline or in another dimension - a chat line for somebody who logged out an
 * hour ago is not an alarm. But that owner still lost a horse, and the answer
 * to "what happened while I was away" has to be written while they are away or
 * it cannot exist. So these calls resolve an owner <b>UUID</b>, never a player
 * entity, and they are not gated on the notice config: turning the chat lines
 * off is a statement about chat, not a request to stop keeping records.
 *
 * <h2>One clock</h2>
 * Every row is stamped with the <b>overworld's</b> game time, whatever
 * dimension the event happened in. A dimension keeps its own day counter, so
 * stamping locally would interleave the Nether's day 4 with the overworld's day
 * 900 and the newest-first order would be nonsense.
 */
@EventBusSubscriber
public final class HorseLog {

    private HorseLog() {
    }

    /** The wire caps the two strings; a name past them would throw at encode time. */
    private static final int NAME_MAX = 64;
    private static final int OTHER_MAX = 96;

    // --- writing ----------------------------------------------------------

    /**
     * A natural cover went through, or did not. Called from
     * {@code NaturalBreedingHandler} for every outcome its owner would want to
     * know about, successful or not; the repeat of a refusal that is still true
     * two seconds later is dropped by {@link com.example.horsegenetics.common.log.HorseEventLog}.
     */
    static void covered(Horse mare, UUID owner, CoverNotice.Reason reason,
                        NaturalCover.Crowd crowd, long now) {
        record(mare, owner, level -> HorseEvent.cover(stamp(level, now), mare.getUUID(),
                name(mare), reason, crowd));
    }

    /**
     * A foal is on the ground. Logged against the <b>dam's owner</b>, which is
     * also the foal's owner: a foal born to a tamed mare is tamed to whoever
     * owns her. A wild birth has no owner and is nobody's news.
     */
    static void born(Horse foal, Horse dam, String damName, String sireName) {
        UUID owner = HorseOwnership.ownerId(dam);
        if (owner == null) {
            return;
        }
        String parents = sireName == null || sireName.isEmpty()
                ? damName
                : damName + " x " + sireName;
        record(foal, owner, level -> HorseEvent.birth(stamp(level, level.getGameTime()),
                foal.getUUID(), name(foal), clip(parents, OTHER_MAX)));
    }

    /**
     * A horse a player owned has died.
     *
     * <p>Its own subscriber rather than a call from {@code HorseDeathNoticeHandler},
     * because that one returns early on three separate conditions - the notices
     * config, the {@code showDeathMessages} game rule, and the horse not being
     * tamed - and a log that loses rows when chat is turned off is exactly the
     * log nobody can trust. The duplicate this risks (several handlers, one
     * death) is handled where every other duplicate is.
     */
    @SubscribeEvent
    static void onHorseDied(LivingDeathEvent event) {
        // Cheapest question first: every mob in the world dies through here.
        if (!(event.getEntity() instanceof AbstractHorse horse)
                || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        var ownerRef = horse.getOwnerReference();
        if (ownerRef == null) {
            return;     // a wild horse dying is not anybody's stable news
        }
        Entity killer = event.getSource().getEntity();
        HurtNotice.Cause cause = HorseNotices.causeOf(horse, event.getSource().getMsgId());
        String by = killer == null ? cause.phrase() : killer.getDisplayName().getString();
        write(level, ownerRef.getUUID(), HorseEvent.death(stamp(level, level.getGameTime()),
                horse.getUUID(), name(horse), clip(by, OTHER_MAX)));
    }

    /**
     * A wild horse has been tamed. Called from {@code HorseOwnerTrackingHandler}
     * at the one moment {@code tamedBy} is filled in, which happens exactly once
     * in a horse's life - so this needs no guard of its own.
     */
    static void tamed(Horse horse, UUID owner) {
        record(horse, owner, level -> HorseEvent.tamed(stamp(level, level.getGameTime()),
                horse.getUUID(), name(horse)));
    }

    /**
     * <b>An emergency stasis chamber caught a horse.</b>
     *
     * <p>Takes the ids rather than the entity, because by the time the caller
     * knows it happened the horse has been {@code discard}ed - it is a data
     * component on an item, and there is nothing left to ask for a name or a
     * level. Every other writer here is handed a live animal; this one is the
     * exception, and that is the feature rather than an awkwardness.
     *
     * @param inBank the chamber was filed in a stasis bank rather than carried
     */
    static void rescued(ServerLevel level, UUID owner, UUID horseId, String horseName,
                        boolean inBank) {
        write(level, owner, HorseEvent.rescued(stamp(level, level.getGameTime()),
                horseId, clip(horseName, NAME_MAX), inBank));
    }

    /**
     * <b>A rescuing braid broke and put a horse back in its stall.</b> Takes the
     * ids for the same reason {@link #rescued} does - the caller is inside a
     * damage event and the horse has just been moved to another world.
     *
     * @param destination where it landed, in words
     */
    static void homed(ServerLevel level, UUID owner, UUID horseId, String horseName,
                      String destination) {
        write(level, owner, HorseEvent.homed(stamp(level, level.getGameTime()),
                horseId, clip(horseName, NAME_MAX), clip(destination, OTHER_MAX)));
    }

    /**
     * A horse changed hands on a transfer paper. Three rows, potentially two
     * players: the taker always gets one, and a horse that had an owner before
     * puts a matching row in <i>their</i> log, because losing a horse is news to
     * the person who lost it and there is nowhere else they would find out.
     *
     * @param from the previous owner, or {@code null} for a dealer's stock,
     *             which nobody owned
     * @param dealer who to credit the purchase to when {@code from} is null
     */
    static void changedHands(Horse horse, UUID taker, String takerName,
                             @Nullable UUID from, String fromName, String dealer) {
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        long at = stamp(level, level.getGameTime());
        String horseName = name(horse);
        if (from == null) {
            write(level, taker, HorseEvent.purchase(at, horse.getUUID(), horseName,
                    clip(dealer, OTHER_MAX)));
            return;
        }
        write(level, taker, HorseEvent.transfer(at, horse.getUUID(), horseName,
                clip(fromName, OTHER_MAX)));
        write(level, from, HorseEvent.sale(at, horse.getUUID(), horseName,
                clip(takerName, OTHER_MAX)));
    }

    // --- plumbing ---------------------------------------------------------

    /** Resolve the level, build the row and store it. The shape every writer above shares. */
    private static void record(Horse horse, @Nullable UUID owner, Row row) {
        if (owner == null || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        write(level, owner, row.of(level));
    }

    private interface Row {
        HorseEvent of(ServerLevel level);
    }

    /**
     * Store one row, and mark its owner for a push.
     *
     * <p>Marked rather than sent, because a payload carries the player's
     * <i>whole</i> log and a paddock is not a quiet place: a foaling morning can
     * land a dozen rows in the same tick, and sending the same two hundred rows a
     * dozen times over is the kind of waste that only shows up on a server with
     * people on it. {@link #flush} sends one packet per second to each player who
     * has anything new, which for a screen a human is reading is indistinguishable
     * from immediate.
     */
    private static void write(ServerLevel level, UUID owner, HorseEvent event) {
        MinecraftServer server = level.getServer();
        if (!HorseEventData.get(server).add(owner, event)) {
            return;     // a repeat - nothing changed, so nothing to send
        }
        DIRTY.add(owner);
    }

    /** Players whose log has grown since the last push. */
    private static final java.util.Set<UUID> DIRTY = new java.util.HashSet<>();

    /**
     * Push each changed log, once a second. An offline owner is simply dropped
     * from the set - their log is on disk and they get it when they next open
     * the tab.
     */
    @SubscribeEvent
    static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (DIRTY.isEmpty() || event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        for (UUID owner : DIRTY) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
                sendTo(player);
            }
        }
        DIRTY.clear();
    }

    /** The whole of one player's log, newest first. Answers the browser's request. */
    public static void sendTo(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new HorseLogPayload(HorseEventData.get(server).of(player.getUUID())));
    }

    /**
     * The overworld's clock. {@code now} is honoured when the caller already has
     * the tick in hand <i>and</i> is in the overworld; a caller elsewhere has a
     * tick from the wrong day counter, so the overworld is asked directly.
     */
    private static long stamp(ServerLevel level, long now) {
        ServerLevel overworld = level.getServer().overworld();
        return level == overworld ? now : overworld.getGameTime();
    }

    /** The horse's name as it reads now - it cannot be asked once it is dead or sold. */
    private static String name(AbstractHorse horse) {
        return clip(HorseNotices.name(horse), NAME_MAX);
    }

    private static String clip(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
