package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Escape;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * <b>A horse close to death throws its rider off and runs.</b> Below the
 * configured fraction of its own maximum health it stops being a mount: it
 * bucks whoever is aboard, drops whatever it was fighting, and runs from what
 * hurt it, jumping anything in the way it can clear.
 *
 * <p>Owner's ask: <i>"When a horse reaches 20% health (threshold configurable),
 * it should buck off its owner, jump fences, and do whatever it can to escape
 * damage and avoid dying."</i> The threshold is
 * {@code behaviour.escape_health_fraction}; the arithmetic - when it starts,
 * when it is allowed to stop, and how long it outlasts the blow - is
 * {@link Escape}, in the game-free module, where it can be tested.
 *
 * <h2>Why this is not just vanilla's panic with a health check</h2>
 * {@code AbstractHorse} already registers {@link net.minecraft.world.entity.ai.goal.PanicGoal}
 * (swapped for {@link HorsePanicGoal} by {@link HorseAggroHandler}), and it
 * already runs from what hits it. It differs from this in all three of the
 * things the owner asked for: it <b>never runs while the horse is ridden</b>,
 * because a mounted horse's movement belongs to its passenger and panic has no
 * business ejecting anybody; it holds ground when the horse has a target, which
 * is right for a gladiator at full health and suicidal at two hearts; and it is
 * not gated on health at all, so it is the same behaviour for a scratch as for
 * a mortal wound. This goal is the mortal-wound case, and it sits <b>above</b>
 * panic so it wins the movement flag while it applies.
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
 * instead, and press jump while horizontal movement is blocked. A horse's jump
 * strength is a real stat with a real range, so a strong one clears the rail
 * and a Falabella does not. <b>An approximation of intent, not a pathfinder</b>
 * - and the one part of this that wants watching in-game.
 */
public final class HorseEscapeGoal extends Goal {

    /** Faster than vanilla's panic, which is 1.2. This one is about dying. */
    private static final double SPEED = 1.7;

    /** How far each leg of the flight runs before it is re-aimed. */
    private static final double DISTANCE = 16.0;

    /** Re-aim no more often than this, in ticks. */
    private static final int REPATH_INTERVAL = 20;

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
        boolean carried = horse.isVehicle();
        buck();
        ActionTrace.log("escape", ActionTrace.describeShort(horse) + " bolting at "
                + String.format("%.0f/%.0f", horse.getHealth(), horse.getMaxHealth())
                + (carried ? " - rider thrown" : "")
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
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (inDanger()) {
            lastDanger = horse.level().getGameTime();
        }
        // EVERY TICK, NOT ONLY AT THE START. A player who remounts a bolting
        // horse gets thrown again: the owner's word was "buck off its owner",
        // and a horse carrying somebody is steered by them rather than by this
        // goal, so a rider aboard is the same as the goal not running.
        buck();
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

    /** Off its back, whoever they are. */
    private void buck() {
        if (horse.isVehicle()) {
            horse.ejectPassengers();
        }
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
