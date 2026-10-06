package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.wild.WildLifetime;
import com.example.horsegenetics.common.wild.WildLifetime.Fate;
import com.example.horsegenetics.common.wild.WildLifetime.Verdict;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Wild horses move on.</b> A horse from a natural spawn is stamped with the
 * game time it arrived ({@link BreedSpawnHandler}); after {@code wild.despawn_days}
 * days it leaves, the first scan it is loaded and nobody is within 32 blocks. One
 * a player has <i>touched</i> - leashed, fed, ridden, tacked - is released into the
 * horse realm instead; the rest are removed. A tamed horse never goes: taming
 * clears the stamp. The rules are {@link WildLifetime}; this is the host. Its
 * other half, which refills thin ground, is {@link WildTopUp}.
 *
 * <h2>What carries the stamp</h2>
 * Two keys in the horse's persistent data, beside {@link BreedSpawnHandler#WILD_SPAWN_KEY}
 * (which is cleared once the horse is founded, so cannot be the stamp): {@link #BORN_KEY},
 * the game time it arrived, and {@link #TOUCHED_KEY}, set once and never unset.
 * Persistent data survives unloads and dimension changes and needs no codec or sync.
 * Only a natural or chunk-generation spawn is stamped - a {@code /summon}, a spawn
 * egg, the cowboy's stock, a bred foal, another mod's horse are never stamped and
 * never move on.
 *
 * <h2>Horses from before this existed</h2>
 * An unstamped horse is stamped (a fresh lifetime) the first time it is scanned if
 * it is what a natural spawn always became: founded into a wild herd, untamed,
 * and never made persistent. Every deliberate path that leaves a horse wild -
 * a release, the cowboy, a stall, the stasis bank - marks it persistent, so none
 * of those is mistaken for one. A lone Feral Mixed from an old natural spawn is
 * not in a herd and keeps its old, permanent life.
 *
 * <h2>A herd goes together</h2>
 * A herd member is judged on its <b>lead's</b> stamp while the lead is loaded,
 * so a herd that arrived together leaves together, and a foal born into the band
 * goes with it. Each member still waits on its own watcher: a herd half in view
 * leaves half at a time. A missing lead is {@link HerdSocialHandler}'s business,
 * as it was before.
 *
 * <h2>Where the scan runs</h2>
 * On the same 30-tick, id-staggered slow scan {@link HorseCareHandler} uses, from
 * {@code EntityTickEvent.Post} - the safe phase for the realm teleport (see
 * {@link HorseFoundingTickHandler} on why not a join event or a server task). The
 * cost for an unstamped horse is a key lookup; for a stamped one, a long compare.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class WildTurnover {

    /** Persistent data: the game time a natural-spawn horse arrived. */
    public static final String BORN_KEY = "horsegenetics:wild_born";

    /** Persistent data: a player has leashed, fed, ridden or tacked this wild horse. One-way. */
    public static final String TOUCHED_KEY = "horsegenetics:wild_touched";

    private static final int SCAN_INTERVAL = 30;

    /** Running totals since start, read by {@code DebugWildSoak}. Server thread only. */
    static int removedTotal;
    static int handedOffTotal;
    static int handOffFailedTotal;

    private WildTurnover() {
    }

    // ------------------------------------------------------------------
    // The stamp
    // ------------------------------------------------------------------

    /** The realm and the F6 debug corridor never stamp, count, expire or top up anything. */
    public static boolean excluded(ServerLevel level) {
        return HorseRealm.isRealm(level) || level.dimension().equals(DebugPenManager.DEBUG_LEVEL);
    }

    /** Give {@code horse} a fresh lifetime starting {@code now}. */
    public static void stamp(Horse horse, long now) {
        horse.getPersistentData().putLong(BORN_KEY, now);
    }

    /** The horse is not wild-spawned any more - tamed, released, or arrived somewhere that keeps it. */
    public static void clear(Horse horse) {
        CompoundTag data = horse.getPersistentData();
        data.remove(BORN_KEY);
        data.remove(TOUCHED_KEY);
    }

    public static boolean stamped(Horse horse) {
        return horse.getPersistentData().contains(BORN_KEY);
    }

    /** Was this horse here before the stamp existed, as a natural spawn? See the class note. */
    private static boolean legacyWild(Horse horse) {
        return !horse.isTamed()
                && !horse.isPersistenceRequired()
                && HorseRecords.hasRealRecord(horse)
                && horse.getData(ModAttachments.HORSE_CARE.get()).inWildHerd();
    }

    // ------------------------------------------------------------------
    // Touched
    // ------------------------------------------------------------------

    /** What a scan can see a player has done: a lead on it, temper from feeding or riding, tack, a rider. */
    private static boolean touchedNow(Horse horse) {
        if (horse.isLeashed() || horse.getTemper() > 0) {
            return true;
        }
        if (!horse.getItemBySlot(EquipmentSlot.SADDLE).isEmpty() || !horse.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
            return true;
        }
        for (Entity rider : horse.getPassengers()) {
            if (rider instanceof Player) {
                return true;
            }
        }
        return false;
    }

    /**
     * <b>The moment of touching</b>, so a lead clipped on and off, or food that
     * gives no temper (hay), between two scans still counts. Food it eats, a lead,
     * or the empty-handed click that mounts an untamed horse. Not the sneak-and-use that
     * reads one ({@code HorseInfoInteraction}), and not a click another handler
     * claimed first. Naming is not here because it cannot happen: the rename window
     * opens only for a horse's owner, and an untamed horse has none.
     */
    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof Horse horse)) {
            return;
        }
        Player player = event.getEntity();
        if (horse.isTamed() || player.isSpectator() || !stamped(horse)) {
            return;
        }
        var stack = event.getItemStack();
        boolean feeds = !stack.isEmpty() && horse.isFood(stack);
        boolean leads = stack.is(Items.LEAD);
        // Vanilla mounts an untamed adult only on an empty hand; any other item makes it rear.
        boolean mounts = stack.isEmpty() && !player.isSecondaryUseActive() && !horse.isBaby();
        if (feeds || leads || mounts) {
            horse.getPersistentData().putBoolean(TOUCHED_KEY, true);
        }
    }

    // ------------------------------------------------------------------
    // The scan
    // ------------------------------------------------------------------

    /**
     * LOWEST priority: this may discard the horse or move it to another level, and every other subscriber to the
     * same tick event (founding, care, genes) must have had its turn on a living, present horse first.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (!horse.isAlive() || (horse.tickCount + horse.getId()) % SCAN_INTERVAL != 0) {
            return;
        }
        CompoundTag data = horse.getPersistentData();
        boolean has = data.contains(BORN_KEY);
        if (excluded(level) || horse.isTamed()) {
            if (has) {
                clear(horse);
            }
            return;
        }
        long now = level.getGameTime();
        if (!has) {
            if (legacyWild(horse)) {
                stamp(horse, now);
            }
            return;
        }
        long born = data.getLongOr(BORN_KEY, now);
        if (WildLifetime.needsRestamp(born, now)) {
            stamp(horse, now);
            return;
        }
        boolean touched = data.getBooleanOr(TOUCHED_KEY, false);
        if (!touched && touchedNow(horse)) {
            data.putBoolean(TOUCHED_KEY, true);
            touched = true;
        }

        Verdict verdict = WildLifetime.judge(leadStamp(level, horse).orElse(born), now, ServerConfig.wildDespawnDays());
        if (verdict == Verdict.STAY) {
            return;
        }
        Fate fate = WildLifetime.fate(verdict, watched(level, horse), touched, ServerConfig.wildRealmHandoff());
        switch (fate) {
            case STAY -> { }
            case REMOVE -> {
                DebugAnnounce.log("Wild", WildLifetime.leftLine(describe(horse),
                        horse.blockPosition().toShortString(), fate, now - born));
                HorseRecords.forgetDeparting(horse); // #200: its record goes with it, if nothing needs it
                horse.discard();
                removedTotal++;
            }
            case TO_REALM -> {
                String who = describe(horse);
                String where = horse.blockPosition().toShortString();
                if (toRealm(level, horse)) {
                    handedOffTotal++;
                    DebugAnnounce.log("Wild", WildLifetime.leftLine(who, where, fate, now - born));
                } else {
                    handOffFailedTotal++;
                }
            }
        }
    }

    /** The herd lead's stamp, when this horse follows a loaded, stamped lead other than itself. */
    private static OptionalLong leadStamp(ServerLevel level, Horse horse) {
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        if (!care.inWildHerd()) {
            return OptionalLong.empty();
        }
        UUID lead = care.herd().orElse(null);
        if (lead == null || lead.equals(horse.getUUID())) {
            return OptionalLong.empty();
        }
        if (level.getEntity(lead) instanceof Horse leader && !leader.isTamed() && stamped(leader)) {
            return OptionalLong.of(leader.getPersistentData().getLongOr(BORN_KEY, 0L));
        }
        return OptionalLong.empty();
    }

    /** Anyone at all - spectators and creative players too - within {@link WildLifetime#WATCH_RADIUS}. */
    private static boolean watched(ServerLevel level, Horse horse) {
        double r2 = WildLifetime.WATCH_RADIUS * WildLifetime.WATCH_RADIUS;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(horse) <= r2) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Horse horse) {
        return HorseRecords.hasRealRecord(horse) ? HorseRecords.of(horse).displayName() : "a wild horse";
    }

    // ------------------------------------------------------------------
    // The realm hand-off
    // ------------------------------------------------------------------

    /**
     * Release {@code horse} into the horse realm at its arrival spot, still wild.
     * It is made wild <i>here</i>, before it moves - a cross-dimension teleport
     * builds a new entity from the old one's saved data, so anything set on the
     * old one afterwards is set on nothing - which also drops a lead or any tack
     * where it stood. No fee (nobody is there to be paid) and no message.
     *
     * <p>The portal ticket loads the arrival chunk, as vanilla does for anything
     * that comes through a portal; without one a horse put into a realm nobody is
     * in would be added to a chunk that is not there. If the move fails the stamp
     * is put back and the next scan tries again: a horse a player worked with is
     * never thrown away because the hand-off did not work.
     *
     * @return whether it arrived
     */
    private static boolean toRealm(ServerLevel level, Horse horse) {
        if (level.getServer() == null) {
            return false;
        }
        ServerLevel realm = level.getServer().getLevel(HorseRealm.REALM_LEVEL);
        if (realm == null) {
            return false;
        }
        CompoundTag data = horse.getPersistentData();
        long born = data.getLongOr(BORN_KEY, level.getGameTime());
        clear(horse);
        HorseRelease.makeWild(level, horse, null);
        Vec3 at = HorseRealm.arrivalSpot();
        // Unverified in-game: a teleport from this scan, with nobody in the realm.
        // The gametest wild_touched_horse_goes_to_the_realm covers the move itself.
        Entity arrived = horse.teleport(new TeleportTransition(realm, at, Vec3.ZERO, horse.getYRot(), horse.getXRot(),
                Set.of(), TeleportTransition.PLACE_PORTAL_TICKET));
        if (arrived == null) {
            stamp(horse, born);
            data.putBoolean(TOUCHED_KEY, true);
            return false;
        }
        return true;
    }
}
