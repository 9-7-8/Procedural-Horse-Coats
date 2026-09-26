package com.example.horsegenetics.neoforge.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.UUID;

/**
 * <b>A horse you left in the realm goes wild by itself.</b> No item, no ritual:
 * walk out through the portal and whatever you did not take with you is, within
 * a few seconds, nobody's.
 *
 * <h2>Why it is a condition, not an event</h2>
 * The obvious build is a hook on leaving - find the player's horses, release
 * them. It is wrong in four ways at once, and each one is a horse that stays
 * tamed forever in a field it was turned out into: a player who <b>logs out</b>
 * in the realm fires no dimension change; one who <b>dies</b> there respawns
 * without one either (the debug corridor needed a separate
 * {@code PlayerRespawnEvent} subscriber for exactly this); one who is
 * <b>teleported</b> out by a command or another mod goes through nothing at all;
 * and a horse standing in an <b>unloaded chunk</b> at the moment of any of those
 * cannot be found to release. The realm is sixteen thousand blocks across, so
 * that last case is the normal one rather than the corner.
 *
 * <p>So the rule is stated as a <b>property of the horse</b> instead: <i>a tamed
 * horse in the realm whose owner is not also in the realm is wild.</i> Every one
 * of those four cases satisfies it without being hooked, and a horse in an
 * unloaded chunk is simply released the moment somebody loads it - which is the
 * first moment it could possibly matter, since until then nobody can see it.
 *
 * <h2>What keeps it from firing on you</h2>
 * The owner being <b>in the realm</b>, which is checked against the live player
 * list. So a player leading a string of horses around the field keeps them; a
 * player who steps out through the portal does not. Horses standing with you
 * when you leave come home with you first
 * ({@code HorseRealm.EVACUATE_RADIUS}) and are never seen by this at all - it is
 * only ever the ones you genuinely left behind.
 *
 * <p>An <b>unowned</b> tamed horse - tamed with no owner reference, which
 * vanilla allows - is released too. There is nobody it can be waiting for.
 *
 * <p>The delay is deliberate and it is what makes the rule safe. A player who
 * crosses the portal and immediately steps back through has a few seconds in
 * which nothing has happened yet, which is the difference between a rule and a
 * trap.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HorseRealmFeral {

    /**
     * Once every five seconds per horse, staggered by entity id the way
     * {@link HorseRealmHerds} and the care tick both are. Half that tick's
     * period, so a horse is turned out before it is asked to join a band rather
     * than a cycle after - {@code HorseRealmHerds} ignores a tamed horse, and
     * two scans that disagree for ten seconds is a horse that misses its band
     * and stands alone until something else moves.
     */
    private static final int SCAN = 100;

    private HorseRealmFeral() {
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.isAlive()) {
            return;
        }
        // Cheapest first, in this order on purpose: almost every horse in the
        // game fails the second test, and no horse anywhere pays more than two
        // field reads unless it is standing in the realm.
        if (!horse.isTamed()) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level) || !HorseRealm.isRealm(level)) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SCAN != 0) {
            return;
        }
        if (!HorseRecords.hasRealRecord(horse)) {
            return;     // not one of ours yet; its record arrives on a later tick
        }
        if (horse.isVehicle()) {
            return;     // somebody is on it, so somebody is plainly still here
        }
        MinecraftServer server = level.getServer();
        if (server == null || ownerIsHere(server, horse)) {
            return;
        }

        HorseRelease.makeWild(level, horse, null);
        ActionTrace.log("realm", ActionTrace.describeShort(horse)
                + " went wild - left in the realm with its owner gone");
    }

    /**
     * Is this horse's owner standing in the realm right now?
     *
     * <p>A walk of the player list, which is short, rather than a lookup of the
     * horse's owner entity - resolving an {@link EntityReference} can load a
     * chunk in another dimension to find a player who is plainly not here, and
     * the question is only ever "is that UUID in this level".
     */
    private static boolean ownerIsHere(MinecraftServer server, Horse horse) {
        EntityReference<LivingEntity> owner = horse.getOwnerReference();
        UUID ownerId = owner == null ? null : owner.getUUID();
        if (ownerId == null) {
            return false;   // tamed by nobody - there is no one it is waiting for
        }
        ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
        return player != null && HorseRealm.isRealm(player.level());
    }
}
