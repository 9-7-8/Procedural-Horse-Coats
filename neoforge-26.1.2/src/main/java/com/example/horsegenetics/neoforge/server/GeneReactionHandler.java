package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * <b>The two triggers that fire on damage</b> - {@code on_hurt}, which is the
 * horse being hit, and {@code on_owner_hurt}, which is the only way a gene can
 * react to something that happened to the <i>player</i>.
 *
 * <p>They share a handler because they share an event and, more importantly,
 * share the thing that makes them dangerous: {@link LivingDamageEvent} fires on
 * <b>every hit anything takes anywhere in the world</b>. Everything here is
 * arranged so that the common case - a hit on something that is not a horse and
 * not a horse owner - costs a single {@code instanceof} and returns.
 */
@EventBusSubscriber
public final class GeneReactionHandler {

    private GeneReactionHandler() {
    }

    /** How far a guardian will look for whatever hurt its owner. */
    private static final double GUARD_RANGE = 16.0;

    /** Last blink per horse, so a burst of arrows is one dodge rather than five. */
    private static final Map<UUID, Long> LAST_BLINK = new ConcurrentHashMap<>();

    /**
     * <b>The owners who have a guarding horse, and the reason this class is
     * affordable.</b>
     *
     * <p>The first version of {@link #onOwnerHurt} asked the level for horses in
     * a sixteen-block box and <i>then</i> checked whether any of them belonged
     * to the hurt player - which is an entity scan on <b>every hit any player
     * takes anywhere in the world</b>, paid for by everybody whether or not
     * anyone owns a guardian. The javadoc claimed it established ownership
     * cheaply first; it did not.
     *
     * <p>So the horse tick, which already runs per horse and already holds the
     * ability list, records its owner here, and the event does one set lookup
     * before it touches the world. On a server where nobody has bred a guardian
     * this class now costs a hash lookup per hit and nothing else.
     */
    private static final Map<UUID, Long> GUARDED_OWNERS = new ConcurrentHashMap<>();

    /** How long an owner entry survives without the tick refreshing it. */
    private static final long GUARD_STALE_TICKS = 100;

    /**
     * Called from the horse tick for a horse expressing a guardian temper.
     * Cheap by construction - the caller has already resolved the abilities.
     */
    static void noteGuardian(Horse horse) {
        if (horse.getOwner() != null) {
            GUARDED_OWNERS.put(horse.getOwner().getUUID(), horse.level().getGameTime());
        }
    }

    /** Does this player have a guarding horse anywhere? One lookup, and usually false. */
    private static boolean hasGuardian(Player player, long now) {
        Long seen = GUARDED_OWNERS.get(player.getUUID());
        if (seen == null) {
            return false;
        }
        if (now - seen > GUARD_STALE_TICKS) {
            GUARDED_OWNERS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity hurt = event.getEntity();
        if (hurt.level().isClientSide()) {
            return;
        }
        if (hurt instanceof Horse horse) {
            onHorseHurt(horse);
            return;
        }
        if (hurt instanceof Player player) {
            onOwnerHurt(player, event.getSource().getEntity());
        }
    }

    // ------------------------------------------------------------------
    // on_hurt - the horse blinks away
    // ------------------------------------------------------------------

    private static void onHorseHurt(Horse horse) {
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        for (HorseAbilities.Active active : GeneAbilityHandler.abilitiesOf(horse)) {
            if (!(active.ability() instanceof GeneAbility.Teleport t)) {
                continue;
            }
            if (!(t.trigger() instanceof GeneAbility.Trigger.OnHurt)) {
                continue;
            }
            long now = level.getGameTime();
            Long last = LAST_BLINK.get(horse.getUUID());
            if (last != null && now - last < t.cooldownTicks()) {
                return;
            }
            if (blink(horse, level, t)) {
                LAST_BLINK.put(horse.getUUID(), now);
            }
            return;
        }
    }

    /**
     * Move the horse, or do nothing at all.
     *
     * <p>Two things here are requirements rather than polish. <b>The destination
     * is validated</b> - a legal landing spot or no teleport; a verb that trusts
     * its destination will eventually suffocate a horse inside a mountain with
     * its owner on top. And <b>the rider comes along</b> without being
     * dismounted: {@code teleportTo} on the vehicle carries its passengers and
     * sends the position to the controlling client, which matters because a
     * ridden horse's movement is owned by that client and a naive server-side
     * move would rubber-band straight back.
     */
    private static boolean blink(Horse horse, ServerLevel level, GeneAbility.Teleport t) {
        if (!t.withRider() && horse.isVehicle()) {
            horse.ejectPassengers();
        }
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double distance = 2.0 + level.getRandom().nextDouble() * (t.maxDistance() - 2.0);
            double x = horse.getX() + Math.cos(angle) * distance;
            double z = horse.getZ() + Math.sin(angle) * distance;
            Double y = groundNear(level, x, horse.getY(), z);
            if (y == null) {
                continue;
            }
            Vec3 to = new Vec3(x, y, z);
            if (!fits(horse, level, to)) {
                continue;
            }
            horse.teleportTo(x, y, z);
            level.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
            return true;
        }
        return false; // nowhere legal to go; better to stand and take it
    }

    /** A surface within a few blocks of the height the horse was already at, or {@code null}. */
    private static Double groundNear(ServerLevel level, double x, double fromY, double z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        for (int dy = 0; dy <= 4; dy++) {
            for (int sign : new int[]{1, -1}) {
                int y = (int) Math.floor(fromY) + dy * sign;
                if (level.isOutsideBuildHeight(y)) {
                    continue;
                }
                var pos = new net.minecraft.core.BlockPos(ix, y, iz);
                if (!level.isLoaded(pos)) {
                    continue;
                }
                if (level.getBlockState(pos.below()).isSolid()
                        && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                    return (double) y;
                }
            }
        }
        return null;
    }

    private static boolean fits(Horse horse, Level level, Vec3 to) {
        AABB box = horse.getBoundingBox().move(to.subtract(horse.position()));
        return level.noCollision(horse, box);
    }

    // ------------------------------------------------------------------
    // on_owner_hurt - the guardian answers
    // ------------------------------------------------------------------

    /**
     * Something hurt a player. Find their horses, if any, and let the ones that
     * care take a target.
     *
     * <p><b>The early-out is the whole design of this method.</b> The event
     * fires on every hit any player in the world takes, so nothing here touches
     * the world until {@link #hasGuardian} has said this player owns a guarding
     * horse at all - one hash lookup, and false for almost everybody.
     *
     * <p><b>Players are never targets</b>, and that is a decision rather than a
     * limitation - see the branch below.
     */
    private static void onOwnerHurt(Player player, Entity attacker) {
        if (attacker == null || attacker == player) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(attacker instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        if (attacker instanceof Player) {
            // A guardian never retaliates against a player, and this is a
            // decision rather than a limitation. A horse that joins in a fight
            // between two people changes what that fight is without either of
            // them having agreed to it - and 26.1.2 exposes no server-side PvP
            // flag to gate it on, so the safe answer is also the only available
            // one. Mobs only.
            return;
        }
        // THE early-out. Before this line the method has only read fields; after
        // it, it asks the world for entities. Nobody without a guarding horse
        // ever gets past here.
        if (!hasGuardian(player, level.getGameTime())) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(GUARD_RANGE);
        for (Horse horse : level.getEntitiesOfClass(Horse.class, box, Horse::isAlive)) {
            if (horse.getOwner() == null || !horse.getOwner().getUUID().equals(player.getUUID())) {
                continue;
            }
            for (HorseAbilities.Active active : GeneAbilityHandler.abilitiesOf(horse)) {
                if (active.ability() instanceof GeneAbility.Temper t
                        && t.trigger() instanceof GeneAbility.Trigger.OnOwnerHurt
                        && horse.distanceToSqr(living) <= t.radius() * t.radius()) {
                    horse.setTarget(living);
                    break;
                }
            }
        }
    }
}
