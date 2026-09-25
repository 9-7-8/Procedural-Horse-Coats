package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * <b>The house rules of the horse realm, and nowhere else.</b> Every method here
 * opens by asking what dimension it is in, and every one of them answers "not
 * mine" for the Overworld, the Nether, a modded dimension and - deliberately -
 * for {@link DebugPenManager#DEBUG_LEVEL}. The debug corridor is an unrestricted
 * test bench and stays one; horses die in its yard on purpose, and six genes
 * would be untestable again if any of this leaked into it.
 *
 * <h2>The promise</h2>
 * The realm's whole proposition to a player is <i>leave a horse here and nothing
 * will happen to it</i>. That is three separate guarantees, because a horse can
 * be lost three ways:
 * <ul>
 *   <li><b>Damage is cancelled</b> before it lands, for every horse, from every
 *       source, whoever owns it. Not "most damage" - a single exception is a
 *       horse somebody comes back for and cannot find.</li>
 *   <li><b>Death is refused</b> as a backstop, because {@code /kill} and a few
 *       other paths set health directly without ever raising a damage event.</li>
 *   <li><b>Despawn is refused</b>: every horse arriving is marked persistent.
 *       {@code Animal} does not despawn in vanilla anyway, but the promise is
 *       load-bearing here and should not rest on a superclass detail.</li>
 * </ul>
 * All three end the moment a horse leaves. Nothing is written to the horse that
 * follows it home except the persistence flag, which is what every valuable
 * horse in this mod already carries.
 *
 * <h2>Horses only</h2>
 * Nothing that is not a horse may join the level - not from a spawner, a spawn
 * egg, {@code /summon}, breeding, or the natural spawner (which the biome has no
 * entries for in any case). Items, projectiles, paintings and players are all
 * untouched; the rule is about <i>mobs</i>, because a cow in a field of horses is
 * a cow somebody has to come and remove and a zombie is the promise broken.
 *
 * <h2>The edge</h2>
 * {@link HorseRealmTerrain} builds a wall of barrier blocks
 * {@value HorseRealm#WALL_HEIGHT} high one block outside the field, which is
 * what actually stops a walking horse. This class covers what a wall cannot: a
 * player flying over it, or anything else that ends a tick outside the bounds.
 * Both halves are wanted - a clamp on its own reads as teleport-stutter where a
 * wall reads as a wall, and a wall on its own has a lid missing.
 *
 * <p><b>Not verified in-game.</b> Nothing on this page has been seen running.
 */
@EventBusSubscriber
public final class HorseRealmRules {

    /**
     * How far inside the boundary a clamped entity is put. One block, so it is
     * clear of the barrier wall's own column and cannot be pushed back out by
     * the collision it just landed against.
     */
    private static final double CLAMP_INSET = 1.0;

    // --- the three guarantees ---

    /**
     * <b>HIGHEST, so nothing else has decided anything yet.</b> Several genes
     * subscribe to this event to soak, reflect or convert damage, and a few of
     * them have side effects - a guardian retaliates, a healer spends a charge.
     * In here the damage is not merely survivable, it does not happen, so this
     * has to run before any of them get to react to it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void noHorseDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse && HorseRealm.isInRealm(horse)) {
            event.setCanceled(true);
        }
    }

    /**
     * The backstop. Damage is already cancelled above, so the only ways here are
     * the ones that never raise a damage event at all - {@code /kill}, a
     * {@code setHealth(0)} from some other mod, the void. Cancelling a death
     * leaves the entity alive on zero health and it dies again next tick, so the
     * health has to be put back at the same time.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void noHorseDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse && HorseRealm.isInRealm(horse)) {
            event.setCanceled(true);
            horse.setHealth(horse.getMaxHealth());
        }
    }

    /**
     * Horses in, everything else out - and mark what came in as persistent.
     *
     * <p>This is the rule the debug dimension used to have and had to give up
     * (see {@code HorseGeneticsEventHandler.keepDebugDimensionHorsesOnly}): there
     * it deleted the zombies and cows that two gene tests were <i>about</i>. Here
     * there are no gene tests, and a field with nothing in it but horses is the
     * entire point, so the blanket rule is the right one.
     */
    @SubscribeEvent
    static void horsesOnly(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !HorseRealm.isRealm(level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof AbstractHorse horse) {
            horse.setPersistenceRequired();
            return;
        }
        if (entity instanceof Mob) {
            event.setCanceled(true);
        }
        // Everything that is not a Mob - the player, an item, an arrow, a boat,
        // a lightning bolt - passes. "No mob spawns" is not "no entities".
    }

    // --- a field nobody may rearrange ---

    /**
     * No breaking, and no placing, for anybody who is not in creative. The realm
     * is a shared commons on a server: its flat plane, its water and its hundred
     * exits are a fixed function of position, and anything a player adds to it or
     * digs out of it is something every other player has to live with. The
     * creative exemption is the same one the debug dimension settled on - an
     * operator who needs to put something here can, and a survival accident,
     * a dispenser or a horse cannot.
     */
    @SubscribeEvent
    static void noBreaking(BreakBlockEvent event) {
        if (event.getPlayer() != null
                && !event.getPlayer().getAbilities().instabuild
                && HorseRealm.isRealm(event.getPlayer().level())) {
            event.setNotifyClient(true);
            event.setCanceled(true);
        }
    }

    /** ...and the same for placing (covers EntityMultiPlaceEvent by inheritance). */
    @SubscribeEvent
    static void noPlacing(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        if (event.getLevel() instanceof Level level && HorseRealm.isRealm(level)) {
            event.setCanceled(true);
        }
    }

    // --- the edge ---

    /**
     * Put back anything that ended its tick outside the field. Horizontal motion
     * is zeroed as well as the position, or a player holding forward against the
     * boundary is teleported back every tick and reads it as lag rather than as
     * a wall.
     *
     * <p>Passengers are skipped: clamping a rider <i>and</i> the horse under them
     * fights vanilla's mount positioning. Move the vehicle and the rider comes.
     */
    @SubscribeEvent
    static void keepInsideTheField(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        // The dimension test first: it is one key comparison and it is false for
        // every entity in every world but this one, which is the whole point of
        // putting a per-tick hook on every entity in the game.
        if (!HorseRealm.isRealm(entity.level()) || entity.level().isClientSide() || entity.isPassenger()) {
            return;
        }
        Vec3 at = entity.position();
        if (HorseRealm.inBounds(at.x, at.z)) {
            return;
        }
        double x = clamp(at.x);
        double z = clamp(at.z);
        entity.setDeltaMovement(0.0, Math.min(0.0, entity.getDeltaMovement().y), 0.0);
        entity.teleportTo(x, at.y, z);
    }

    private static double clamp(double v) {
        if (v < CLAMP_INSET) {
            return CLAMP_INSET;
        }
        double far = HorseRealm.SIZE - CLAMP_INSET;
        return v > far ? far : v;
    }

    private HorseRealmRules() {
    }
}
