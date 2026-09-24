package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Escape;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * <b>A horse close to death takes itself over and carries its rider out.</b>
 * Below the configured fraction of its own maximum health it stops answering
 * the reins - the saddle comes off into the rider's pack, it drops whatever it
 * was fighting, and it runs faster and jumps higher than it can when anybody is
 * steering, with the rider still aboard.
 *
 * <p>Owner's ask, as amended: <i>"instead of bucking the rider off, have the
 * horse run to safety with the rider, removing the saddle, and pathing itself
 * to safety, getting a boost to speed and jump height while it's fleeing."</i>
 * The threshold is {@code behaviour.escape_health_fraction}; the arithmetic -
 * when it starts, when it is allowed to stop, how long it outlasts the blow, and
 * how much of a boost it gets - is {@link Escape}, in the game-free module,
 * where it can be tested.
 *
 * <h2>Taking the saddle off <em>is</em> the mechanism, not a flourish</h2>
 * Vanilla decides who may steer inside
 * {@code AbstractHorse.getControllingPassenger()}, which hands control to a
 * player passenger <b>only if {@code isSaddled()}</b>; and
 * {@code AbstractHorse.isImmobile()} stops the mob's own AI moving a horse only
 * while it is both ridden <b>and</b> saddled. So one act settles both halves of
 * what was asked for: with the saddle gone the rider is a passenger, the
 * navigation moves the animal, and the horse carries them out of trouble. There
 * is no rider-input suppression to write and no mixin to add - which is the
 * whole reason the first version of this goal ejected the rider instead, and
 * that turned out to be the worse trade.
 *
 * <p><b>Both halves of that are read off the 26.1.2 sources and neither is
 * verified in a running game.</b> If a rider keeps steering, the
 * {@code isSaddled()} gate in {@code getControllingPassenger()} is not the only
 * one; if the horse stands still with a rider aboard and no saddle, it is
 * {@code isImmobile()} that is wrong here, not the navigation.
 *
 * <h2>How this meets bareback steering, which is the sharp edge</h2>
 * {@link BarebackSteeringHandler} implements bareback riding by <b>lending the
 * horse a real saddle</b> carrying {@link ModDataComponents#PHANTOM_SADDLE}, and
 * it re-lends it every tick. Two consequences, and both would be bugs if this
 * class did the obvious thing:
 *
 * <ul>
 *   <li><b>A phantom saddle must never reach the rider's inventory.</b> Handing
 *       one over is a saddle duplicator - the same hole {@code reclaim} exists
 *       to close. So the stack is checked for the component, and a phantom is
 *       destroyed rather than given.</li>
 *   <li><b>The lender has to stand down while the horse is bolting</b>, or it
 *       puts the reins straight back in the rider's hands on the next tick and
 *       the whole behaviour is invisible for exactly the best-bonded horses the
 *       mod is about. {@link #bolting} is what it asks, and it is deliberately
 *       a scan of the goal selector rather than a flag on the horse: there is
 *       then no state to leak when a bolting horse is unloaded mid-flight.</li>
 * </ul>
 *
 * <h2>Priority 0, and the two goals it shares it with</h2>
 * Nothing a horse can be doing outranks not dying, so this is registered at
 * priority 0 in {@link HorseCareHandler} - above panic and the shade goal at 1,
 * above the melee and feeding goals at 3, above every herd goal. It is added
 * <b>before</b> {@link InspectHoldGoal}, which also claims {@code MOVE} at 0,
 * so the tie breaks toward the horse: somebody reading the information screen
 * does not pin a bleeding animal in place.
 *
 * <p><b>It claims {@code MOVE} and not {@code JUMP}</b>, although it does jump,
 * and that is deliberate rather than an oversight. Vanilla's {@code FloatGoal}
 * is also at priority 0 and holds {@code JUMP}; claiming the flag would mean a
 * horse swimming could not start this goal at all, which is the exact case -
 * being shot at in deep water - where it most needs to move. Vanilla's own
 * {@code PanicGoal} claims {@code MOVE} alone for the same reason and leaves
 * jumping to the move control. Flagged here because
 * {@code wiki/behaviour-hierarchy.html} reads the {@code setFlags} call, so the
 * page will say {@code MOVE} while this class presses jump.
 *
 * <h2>Fences</h2>
 * There is no pathfinding-with-jumps in vanilla horse AI - a fence is simply
 * {@code BLOCKED} to a ground navigator, so no path over one is ever produced.
 * {@link SunShadeGoal} met this first and this does the same honest thing: when
 * the navigator refuses, drive the move control straight at the escape point
 * instead, and press jump while horizontal movement is blocked. What
 * {@link Escape#JUMP_BOOST} buys is that this now works for the weak end of the
 * jump range too, rather than only for a good jumper.
 */
public final class HorseEscapeGoal extends Goal {

    /** How much of its boosted speed the navigation asks for. Vanilla's panic is 1.2. */
    private static final double SPEED = 1.7;

    /** How far each leg of the flight runs before it is re-aimed. */
    private static final double DISTANCE = 16.0;

    /** Re-aim no more often than this, in ticks. */
    private static final int REPATH_INTERVAL = 20;

    /**
     * The two modifiers held for the length of the flight. Outside {@code gene/},
     * deliberately: {@code GeneAbilityHandler.clearAttributes} sweeps only the
     * ids it owns, and a shared prefix is how a pregnant mare once lost her
     * speed modifier to another system's tick.
     */
    private static final Identifier SPEED_ID =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "escape/speed");
    private static final Identifier JUMP_ID =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "escape/jump");

    private final Horse horse;

    /**
     * Where it was standing when the bolt began, and so what it runs away from
     * when nothing visible dealt the blow - a lava pool, a cactus, powder snow.
     * Replaced by the attacker's position whenever there is an attacker.
     */
    private Vec3 from;

    /** Game tick of the last thing that hurt it; {@link Escape#threatFresh} reads it. */
    private long lastDanger = -1L;

    private int repathCooldown;

    /** True once the navigator has refused a leg, so the move control drives instead. */
    private boolean fencedIn;

    public HorseEscapeGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    /**
     * <b>Is this horse running for its life right now?</b> Asked by
     * {@link BarebackSteeringHandler}, which must not lend it a saddle while it
     * is - that would hand the reins back on the next tick.
     *
     * <p>A scan rather than a flag. The callers that need it have already
     * narrowed to a horse with an eligible rider aboard, so this runs for a
     * handful of animals at most, and it cannot go stale the way a static set
     * keyed by UUID does when a bolting horse is unloaded or killed mid-flight.
     */
    static boolean bolting(AbstractHorse horse) {
        for (WrappedGoal wrapped : horse.goalSelector.getAvailableGoals()) {
            if (wrapped.isRunning() && wrapped.getGoal() instanceof HorseEscapeGoal) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canUse() {
        return horse.isAlive()
                && Escape.bolts(horse.getHealth(), horse.getMaxHealth(), ServerConfig.escapeHealthFraction())
                && inDanger();
    }

    @Override
    public boolean canContinueToUse() {
        return horse.isAlive()
                && Escape.keepsBolting(horse.getHealth(), horse.getMaxHealth(),
                        ServerConfig.escapeHealthFraction())
                && Escape.threatFresh(lastDanger, horse.level().getGameTime());
    }

    @Override
    public void start() {
        lastDanger = horse.level().getGameTime();
        from = horse.position();
        repathCooldown = 0;
        fencedIn = false;
        // Drop the fight. A horse with an aggression gene at two hearts is
        // holding a target the melee goal would keep it standing next to.
        horse.setTarget(null);
        Escape.Saddle saddle = unsaddle();
        if (horse.getFirstPassenger() instanceof Player rider) {
            // Once, on the tick it takes over. The alternative is a player whose
            // horse has silently stopped answering the controls, which reads as
            // the mod breaking rather than as the horse deciding.
            rider.sendSystemMessage(Component.literal(
                    Escape.reinsLost(HorseNotices.name(horse), saddle)));
        }
        boost();
        ActionTrace.log("escape", ActionTrace.describeShort(horse) + " bolting at "
                + String.format("%.0f/%.0f", horse.getHealth(), horse.getMaxHealth())
                + " - saddle " + saddle
                + (horse.getLastHurtByMob() == null ? " - nothing visible hurt it"
                        : " - from " + horse.getLastHurtByMob().getType()
                                .builtInRegistryHolder().key().identifier()));
    }

    @Override
    public void stop() {
        ActionTrace.log("escape", ActionTrace.describeShort(horse) + " stopped bolting at "
                + String.format("%.0f/%.0f", horse.getHealth(), horse.getMaxHealth())
                + (fencedIn ? " - the navigator never found it a way out" : ""));
        from = null;
        lastDanger = -1L;
        fencedIn = false;
        unboost();
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (inDanger()) {
            lastDanger = horse.level().getGameTime();
        }
        // EVERY TICK, NOT ONLY AT THE START. The rider can re-saddle a horse
        // they are sitting on, and a saddle back in the slot is the reins back
        // in their hands - vanilla decides who steers from inside that slot.
        if (horse.isSaddled()) {
            unsaddle();
        }
        if (--repathCooldown <= 0) {
            repathCooldown = REPATH_INTERVAL;
            Vec3 to = aim();
            if (horse.getNavigation().moveTo(to.x, to.y, to.z, SPEED)) {
                fencedIn = false;
            } else {
                // NO PATH - penned, walled, or fenced. Go at it anyway.
                fencedIn = true;
                horse.getMoveControl().setWantedPosition(to.x, to.y, to.z, SPEED);
            }
        }
        if (horse.horizontalCollision && horse.onGround()) {
            horse.getJumpControl().jump();   // a fence, a wall, a step - try it
        }
    }

    /** Is something hurting it right now? */
    private boolean inDanger() {
        LivingEntity attacker = horse.getLastHurtByMob();
        return (attacker != null && attacker.isAlive())
                || horse.hurtTime > 0
                || horse.isOnFire()
                || horse.isInLava()
                || horse.isFreezing();
    }

    /**
     * <b>Take the saddle off, and account for it.</b> Into the rider's own
     * inventory where there is a rider and room, on the ground where there is
     * not, and destroyed where it was never theirs - a phantom saddle lent by
     * {@link BarebackSteeringHandler} is the one stack that must not be handed
     * over, because handing it over mints a free saddle.
     *
     * <p>An unridden horse keeps its tack. Removing it is about who is steering,
     * and with nobody aboard there is nothing to take control of - a bolting
     * horse shedding gear across a paddock would be item churn for no gameplay.
     *
     * <p>Silent, and called again from {@link #tick} because a rider sitting on
     * a horse can put a saddle back on it, and a saddle back in the slot is the
     * reins back in their hands. Only {@link #start} says anything, or a player
     * re-saddling a bolting horse would get a line a tick.
     */
    private Escape.Saddle unsaddle() {
        ItemStack saddle = horse.getItemBySlot(EquipmentSlot.SADDLE);
        if (saddle.isEmpty() || !(horse.getFirstPassenger() instanceof Player rider)) {
            return Escape.Saddle.NONE;      // bare, or nobody to take it from
        }
        horse.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
        if (saddle.has(ModDataComponents.PHANTOM_SADDLE.get())) {
            return Escape.Saddle.NONE;      // never the rider's; it simply goes
        }
        // The same "into the pack, else the floor" as ModNetworking's tack swap,
        // so a saddle taken off a horse lands in one place whichever way it came
        // off.
        ItemStack theirs = saddle.copy();
        if (rider.getInventory().add(theirs)) {
            return Escape.Saddle.POCKETED;
        }
        rider.drop(theirs, false);
        return Escape.Saddle.DROPPED;
    }

    /** Faster and higher, for as long as it is running. */
    private void boost() {
        modify(Attributes.MOVEMENT_SPEED, SPEED_ID, Escape.SPEED_BOOST);
        modify(Attributes.JUMP_STRENGTH, JUMP_ID, Escape.JUMP_BOOST);
    }

    /** And back to itself. {@code stop} is the only thing that takes these off. */
    private void unboost() {
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
        }
        AttributeInstance jump = horse.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.removeModifier(JUMP_ID);
        }
    }

    /**
     * Transient and multiplied against the horse's own total, so a boost is a
     * proportion of what that animal already is rather than a flat amount that
     * would be everything to a Falabella and nothing to a Shire.
     */
    private void modify(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Identifier id, double amount) {
        AttributeInstance instance = horse.getAttribute(attribute);
        if (instance == null) {
            return;     // no such attribute on this animal; not an error
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /**
     * A point {@link #DISTANCE} away from what hurt it, level with the horse.
     * Away from the attacker when there is one, otherwise away from where the
     * bolt began - and, when even that is the square it is standing on, simply
     * ahead, because a horse in a lava pool has no wrong direction.
     */
    private Vec3 aim() {
        LivingEntity attacker = horse.getLastHurtByMob();
        Vec3 threat = attacker != null && attacker.isAlive() ? attacker.position() : from;
        Vec3 away = threat == null ? Vec3.ZERO : horse.position().subtract(threat);
        if (flat(away) < 1.0e-4) {
            away = horse.getViewVector(1.0f);
        }
        if (flat(away) < 1.0e-4) {
            away = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 heading = new Vec3(away.x, 0.0, away.z).normalize();
        return horse.position().add(heading.scale(DISTANCE));
    }

    /** Horizontal length squared. Spelled out rather than borrowed, per {@code common/}'s habit. */
    private static double flat(Vec3 v) {
        return v.x * v.x + v.z * v.z;
    }
}
