package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.HorseRealmSize;
import com.example.horsegenetics.neoforge.data.HorseWhereabouts;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.HorseRosterPayload;
import com.example.horsegenetics.neoforge.network.RealmRosterDeltaPayload;
import com.example.horsegenetics.neoforge.network.RealmRosterPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Every horse in the horse realm</b> - the whole field, not the part of it
 * somebody is standing near - gathered for the browser's <i>Horse realm</i> tab.
 * {@link HorseRoster} is the same table asked the other question, <i>which of
 * these are mine</i>, and this one deliberately asks nothing about ownership.
 *
 * <h2>It used to be a scan, and a scan could only ever see a fraction</h2>
 * The first version walked the realm level's live entities. That is the natural
 * shape for "what is in the field right now" and it was wrong for the only
 * reason that matters: <b>most of the field is unloaded most of the time</b>.
 * The realm is 1,600 blocks in radius before it grows at all, a player loads a
 * dozen chunks of it, and so the tab listed the horses within sight of whoever
 * opened it and silently called that the field. A catalogue that omits
 * everything you have not already walked past is not a catalogue.
 *
 * <h2>So it is built from the census, not from the world</h2>
 * {@link HorseRealmSize} keeps a durable set of every horse living in the realm
 * - maintained by the horses themselves, and explicitly keeping a horse that
 * nobody has loaded for a week. That answers <i>who</i>. The
 * {@linkplain HorseAncestryData ancestry database} answers <i>what</i>: a name,
 * a breed, a generation and a genotype for every horse that has ever existed,
 * with no chunk loaded anywhere. Between them the row can be built for a horse
 * on the far side of the field.
 *
 * <p>A <b>loaded</b> horse then overlays the handful of facts only an entity can
 * answer - bond, whether it is still a foal, herd membership, where exactly it
 * is standing - exactly as the stable's roster does. The two tables now differ
 * only in how they choose their rows.
 *
 * <p>Live entities are also <b>unioned in</b> rather than merely consulted: a
 * horse that arrived in the last few seconds has not yet run the census scan
 * that would enrol it, and a table that made you wait five seconds for a horse
 * you just led in would read as broken.
 *
 * <h2>Why it arrives in pieces</h2>
 * Three thousand rows do not fit in a packet - see {@link RealmRosterPayload}.
 * A request builds the whole list once and then queues it, and
 * {@link #onServerTick} posts {@value RealmRosterPayload#BATCH} rows per tick
 * until it is done. That spends the encoding cost over a couple of seconds
 * instead of in one frame, which is the owner's own tolerance for it, and it
 * means the tab can draw a partial field while the rest arrives.
 *
 * <p>One queue per player, replaced rather than appended to: asking again means
 * "I want it fresh", so a second request abandons the first run mid-flight and
 * starts over with {@code first} set, and the client throws away the half it
 * had. Nothing has to reconcile two overlapping runs.
 *
 * <h2>Sent on arrival, then kept up to date rather than re-sent</h2>
 * The full roster goes out <b>once</b>, when a player walks into the realm or
 * logs in inside it - unasked, so that by the time they press <kbd>H</kbd> the
 * field is already there. After that the server keeps everyone in the realm in
 * step with {@link RealmRosterDeltaPayload}: one horse joined, one horse left.
 * (Owner, 2026-09-26: "maintain the record server-wide... so that they don't
 * have to reload it every time.")
 *
 * <p><b>The server-wide record already existed</b> - it is the census in
 * {@link HorseRealmSize}, which every horse in the field maintains for itself.
 * What was missing was anybody telling the clients when it changed, so each one
 * re-derived the whole thing on a timer. The census's own {@code note} and
 * {@code forget} are the two events, and they are the only two: a horse is in
 * the field or it is not.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class RealmRoster {

    /** In-flight sends, by player. At most one run each; a new one replaces it. */
    private static final Map<UUID, Iterator<HorseRosterPayload.Entry>> SENDING = new HashMap<>();

    private RealmRoster() {
    }

    /**
     * Start sending the field to this player. Returns at once; the rows follow
     * over the next ticks.
     */
    public static void sendTo(ServerPlayer player) {
        List<HorseRosterPayload.Entry> all = gather(player);
        if (all.isEmpty()) {
            // Still a complete answer, and the tab needs to hear it or it sits
            // on "asking the server..." for ever.
            PacketDistributor.sendToPlayer(player, new RealmRosterPayload(List.of(), true, true));
            SENDING.remove(player.getUUID());
            return;
        }
        SENDING.put(player.getUUID(), all.iterator());
        // The first batch goes now rather than next tick: a table that fills
        // instantly for a small field should not pay for a large one's pacing.
        push(player, true);
    }

    /** Stop mid-run - the player disconnected, or asked again. */
    public static void cancel(UUID player) {
        SENDING.remove(player);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (SENDING.isEmpty()) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (SENDING.containsKey(player.getUUID())) {
                push(player, false);
            }
        }
        // A player who left mid-run leaves an iterator nobody will drain.
        SENDING.keySet().removeIf(id -> event.getServer().getPlayerList().getPlayer(id) == null);
    }

    /** One batch, and drop the run when it is drained. */
    private static void push(ServerPlayer player, boolean first) {
        Iterator<HorseRosterPayload.Entry> rest = SENDING.get(player.getUUID());
        if (rest == null) {
            return;
        }
        List<HorseRosterPayload.Entry> batch = new ArrayList<>(RealmRosterPayload.BATCH);
        while (rest.hasNext() && batch.size() < RealmRosterPayload.BATCH) {
            batch.add(rest.next());
        }
        boolean last = !rest.hasNext();
        if (last) {
            SENDING.remove(player.getUUID());
        }
        PacketDistributor.sendToPlayer(player, new RealmRosterPayload(batch, first, last));
    }

    // ------------------------------------------------------------------
    // Keeping everyone in the field up to date
    // ------------------------------------------------------------------

    /**
     * <b>A horse has joined the field.</b> Called by {@link HorseRealmCensus} the
     * first time a horse ticks in the realm, which covers every way in there is
     * - the portal, a turnout ticket, a foal born there - without any of them
     * needing a hook of its own. The same reason the census itself is a
     * condition rather than an event.
     */
    public static void entered(Horse horse) {
        if (!(horse.level() instanceof ServerLevel level) || !HorseRecords.hasRealRecord(horse)) {
            return;
        }
        broadcast(level.getServer(),
                new RealmRosterDeltaPayload(List.of(entry(HorseRecords.of(horse), horse)), List.of()));
    }

    /** <b>A horse has left the field</b>, by any route at all. */
    public static void left(MinecraftServer server, UUID horse) {
        broadcast(server, new RealmRosterDeltaPayload(List.of(), List.of(horse)));
    }

    /**
     * To everyone standing in the realm, and nobody else. A player in the
     * Overworld has no realm tab to update - it is hidden outside the field -
     * and no cached roster either, since the client drops it on the way out.
     */
    private static void broadcast(MinecraftServer server, RealmRosterDeltaPayload delta) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (HorseRealm.isRealm(player.level())) {
                PacketDistributor.sendToPlayer(player, delta);
            }
        }
    }

    /**
     * <b>Walked in.</b> The field is pushed without being asked, so that opening
     * the browser in there finds it already loaded - or already loading, which
     * is the same thing a second later and looks far better than a blank table.
     */
    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTo().equals(HorseRealm.REALM_LEVEL)) {
            sendTo(player);
        }
    }

    /** Logged in standing in the realm, which is travel the event above misses. */
    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && HorseRealm.isRealm(player.level())) {
            sendTo(player);
        }
    }

    /** Left mid-run: drop the queue rather than tick it until the player list catches up. */
    @SubscribeEvent
    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------
    // Building the list
    // ------------------------------------------------------------------

    static List<HorseRosterPayload.Entry> gather(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return List.of();
        }
        ServerLevel realm = server.getLevel(HorseRealm.REALM_LEVEL);
        if (realm == null) {
            return List.of();
        }

        HorseAncestryData ancestry = HorseAncestryData.get(server);
        HorseWhereabouts whereabouts = HorseWhereabouts.get(server);

        // The census first, in its own order, then anything standing in the
        // field that has not enrolled yet. A LinkedHashSet does the union and
        // keeps that order in one go.
        Set<UUID> ids = new LinkedHashSet<>();
        for (HorseRealmSize.Resident resident : HorseRealmSize.get(server).residents()) {
            ids.add(resident.horse());
        }
        Map<UUID, Horse> loaded = new HashMap<>();
        for (Horse horse : realm.getEntities(net.minecraft.world.entity.EntityType.HORSE,
                h -> h.isAlive() && HorseRecords.hasRealRecord(h))) {
            loaded.put(horse.getUUID(), horse);
            ids.add(horse.getUUID());
        }

        List<HorseRosterPayload.Entry> out = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            // A horse the census still counts but that died or left in a way no
            // scan caught. The census is deliberately forgiving - it would
            // rather size the field too large than lose a horse - so this table
            // is where the slack gets taken up.
            if (whereabouts.isDead(id) || whereabouts.inStasis(id)) {
                continue;
            }
            HorseRecord record = ancestry.lookup(id).orElse(null);
            if (record == null || !record.hasGenome()) {
                continue;   // never founded, or a code from another registry
            }
            out.add(entry(record, loaded.get(id)));
        }
        return List.copyOf(out);
    }

    /**
     * One horse as a table row. The same {@link HorseRosterPayload.Entry} the
     * stable uses, so the client draws both tables with one renderer and a
     * column added here is a column in both.
     *
     * @param horse the live entity if it happens to be loaded, or {@code null} -
     *              in which case the live-only fields go out marked unknown
     *              rather than guessed, exactly as the stable's do
     */
    private static HorseRosterPayload.Entry entry(HorseRecord record, Horse horse) {
        boolean loaded = horse != null;
        HorseCareAttachment care = loaded ? horse.getData(ModAttachments.HORSE_CARE.get()) : null;
        return new HorseRosterPayload.Entry(
                record.id(),
                record.firstName(),
                record.lastName(),
                record.barnName().orElse(""),
                record.lineage().displayName(),
                record.generation(),
                record.geneticCode(),
                loaded ? horse.isTamed() : record.ownerId().isPresent(),
                // A horse nobody can see is assumed grown, for the stable's
                // reason: a foal out of the world long enough to unload has
                // almost certainly aged up, and "foal" on a grown mare reads as
                // a bug. In the realm it is if anything safer - time passes
                // there whether or not anyone is watching.
                !loaded || !horse.isBaby(),
                loaded,
                care == null ? HorseRosterPayload.BOND_UNKNOWN : care.bond(),
                care != null && care.inHerd(),
                loaded ? "realm " + horse.getBlockX() + ", " + horse.getBlockY()
                                + ", " + horse.getBlockZ()
                        : "realm",
                record.tamedBy().orElse(""),
                record.bredBy().orElse(""),
                record.hasKnownParents(),
                record.gelded());
    }
}
