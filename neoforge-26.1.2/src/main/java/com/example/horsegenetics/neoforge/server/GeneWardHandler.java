package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>The spawn ward.</b> Hostile mobs do not appear near a horse expressing
 * {@link GeneAbility.Ward} - not pushed away, never spawned.
 *
 * <h2>Why this is not a mob_aura mode</h2>
 * Every other radius effect in the format runs on the horse's own tick and looks
 * outward. This one runs on <b>somebody else's event</b> and looks inward, so
 * folding it into {@code mob_aura} would put a global event handler behind a
 * verb whose whole contract is "a beat, a radius, a cap". It gets its own verb
 * and its own handler for that reason.
 *
 * <h2>Natural spawns only, and this is the important part</h2>
 * A ward that cancelled every spawn would break mob farms - the player's and
 * other mods' - <b>invisibly</b>, because nothing errors and the farm simply
 * stops producing. So {@link #isNatural} gates on the spawn reason, and
 * spawner blocks, spawn eggs, breeding, structures, dispensers and commands all
 * pass straight through. A player who builds a mob farm next to their warding
 * horse gets a working farm, which is the only acceptable outcome.
 *
 * <h2>It runs on every spawn attempt in the world</h2>
 * That is the cost, and it is why nothing here asks the level for entities. The
 * horse tick - which already runs once per horse and already has the ability
 * list in hand - writes each warding position into a small map, and the event
 * walks that. The cheap tests come first for the same reason: this event fires
 * constantly on a populated server, so anything expensive here is paid for by
 * everybody whether or not they own a horse.
 */
@EventBusSubscriber
public final class GeneWardHandler {

    private GeneWardHandler() {
    }

    /**
     * One warding horse, reduced to the only things the check needs.
     *
     * <p>{@code dimension} is <b>not optional</b>. Without it a horse warding in
     * the Nether suppresses spawns in the Overworld at the same x/z, which is
     * exactly the kind of bug nobody would ever trace back to this gene - the
     * ward is invisible, the horse is in another world, and all the player sees
     * is that monsters have stopped appearing somewhere.
     */
    private record Ward(ResourceKey<Level> dimension, double x, double y, double z,
                        double radiusSqr, long seenTick) {
    }

    /**
     * Every horse currently warding, written by the horse tick and read by the
     * spawn event.
     *
     * <p><b>This is the whole reason the gene is affordable.</b> The obvious
     * implementation asks the level for its horses on each spawn attempt, and
     * the spawn event fires constantly on a populated server - so the cost of
     * one player's horse would be paid by everybody, on every attempt. Instead
     * the horse tick, which already runs once per horse per tick and already has
     * the ability list in hand, drops its position here; the event does a map
     * walk over a handful of entries and nothing else.
     */
    private static final Map<UUID, Ward> WARDS = new ConcurrentHashMap<>();

    /** How many ticks a ward entry survives without being refreshed by the tick. */
    private static final long STALE_TICKS = 40;

    /**
     * Refuse a natural hostile spawn inside a ward.
     *
     * <p>{@link EventPriority#HIGH} so that a mod which deliberately forces a
     * spawn later in the chain still wins - a ward should beat the world's
     * ordinary spawning, not another mod's explicit intent.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!isNatural(event.getSpawnType())) {
            return; // a spawner, an egg, breeding, a structure - not this gene's business
        }
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!MobGroups.isHostile(event.getEntity())) {
            return;
        }
        for (Ward ward : liveWards(level)) {
            double dx = entity.getX() - ward.x();
            double dy = entity.getY() - ward.y();
            double dz = entity.getZ() - ward.z();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 <= ward.radiusSqr()) {
                event.setSpawnCancelled(true);
                announce(level, entity, Math.sqrt(d2));
                return;
            }
        }
    }

    /** Refusals since the last line, and when that line went out - see {@link #announce}. */
    private static int heldBack;
    private static long lastAnnounced = Long.MIN_VALUE;

    /** At most one chat line per this many ticks, however many spawns were refused. */
    private static final long ANNOUNCE_EVERY = 200;

    /**
     * <b>The ward is invisible when it works</b> - the evidence is a monster
     * that did not appear - so with {@code debug.announce} on it says so, at
     * most once every ten seconds with a count. Added 2026-09-10 when the owner
     * asked how to tell it was working at all. Remember when reading it that the
     * game never spawns monsters within 24 blocks of a player, so a player
     * standing beside the horse sees no refusals because there is nothing to
     * refuse.
     */
    private static void announce(ServerLevel level, Entity entity, double distance) {
        heldBack++;
        long now = level.getGameTime();
        if (now - lastAnnounced < ANNOUNCE_EVERY) {
            return;
        }
        DebugAnnounce.say(level, "Ward", heldBack + " natural monster spawn"
                        + (heldBack == 1 ? "" : "s") + " refused - the last a "
                        + entity.getType().getDescription().getString() + ", "
                        + (int) Math.round(distance) + " blocks from a warding horse",
                ChatFormatting.LIGHT_PURPLE);
        heldBack = 0;
        lastAnnounced = now;
    }

    /**
     * The spawn reasons a ward is entitled to refuse.
     *
     * <p>Deliberately a short allow-list rather than a deny-list: a future game
     * version adding a reason should default to <i>not</i> being cancelled, so
     * that the failure mode of being out of date is a ward that does too little
     * rather than a ward that silently breaks something new.
     */
    private static boolean isNatural(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL
                || reason == EntitySpawnReason.CHUNK_GENERATION
                || reason == EntitySpawnReason.PATROL
                || reason == EntitySpawnReason.REINFORCEMENT;
    }

    /**
     * Called from the horse tick for every horse expressing a ward.
     *
     * <p>Cheap by construction: the caller has already resolved the abilities
     * and already knows the condition holds, so this is one map write.
     */
    static void note(Horse horse, GeneAbility.Ward ward) {
        WARDS.put(horse.getUUID(), new Ward(horse.level().dimension(),
                horse.getX(), horse.getY(), horse.getZ(),
                ward.radius() * ward.radius(), horse.level().getGameTime()));
    }

    /**
     * The wards still worth trusting.
     *
     * <p>Entries expire rather than being removed on death or unload: a horse
     * that stops warding, dies, despawns or crosses into another dimension
     * simply stops refreshing its entry, and {@link #STALE_TICKS} later it is
     * gone. That is deliberately more robust than hooking every way a horse can
     * leave - a missed hook would leave a ward suppressing spawns for ever at a
     * position with no horse at it, which is the kind of bug nobody would ever
     * trace back to this gene.
     */
    private static List<Ward> liveWards(ServerLevel level) {
        long now = level.getGameTime();
        List<Ward> out = new ArrayList<>(WARDS.size());
        WARDS.entrySet().removeIf(e -> now - e.getValue().seenTick() > STALE_TICKS);
        ResourceKey<Level> here = level.dimension();
        for (Ward w : WARDS.values()) {
            if (w.dimension().equals(here)) {
                out.add(w);
            }
        }
        return out;
    }
}
