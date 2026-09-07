package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * At first light, the cowboy walks back to his horse and gets on.
 *
 * <h2>Why he was ever off it</h2>
 * Because riding through a night is what broke him. He used to ride home at
 * dusk, open his barn and shepherd eleven horses through a two-block doorway,
 * and no amount of work made that reliable - a large horse cannot path through
 * that gap at all, a villager standing inside is a cork, and the string shoved
 * each other back out of the entrance. So he now gets <b>off</b> at dusk
 * ({@link CowboyHandler}) and spends the night as an ordinary villager with the
 * ordinary villager goals he has always had, which need no help from anyone. He
 * survives it because he has ten times the health rather than because he found a
 * bed.
 *
 * <h2>Why he walks rather than appearing on it</h2>
 * {@code startRiding} works at any range, so "put him back on" is one line that
 * would also teleport him across a village. This goal is the difference between
 * a man who fetches his horse in the morning and a man who blinks onto it: he
 * paths to the animal like anything else, and only mounts once he is beside it.
 *
 * <p>It is a goal on the <b>cowboy</b> rather than a handler, because a
 * dismounted cowboy is an ordinary {@code PathfinderMob} that can steer itself -
 * which is exactly the thing that is impossible while he is a passenger, and the
 * reason his ridden itinerary had to live on the horse instead.
 */
public final class CowboyRemountGoal extends Goal {

    /** An unhurried walk. He is fetching a horse, not chasing one. */
    private static final double SPEED = 0.6;

    /** Near enough to swing up. */
    private static final double MOUNT_RANGE = 2.5;

    /** Re-path this often on the way over. */
    private static final int REPATH_INTERVAL = 20;

    private final Cowboy cowboy;
    private @Nullable AbstractHorse mount;
    private int repathCooldown;

    public CowboyRemountGoal(Cowboy cowboy) {
        this.cowboy = cowboy;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** His own horse, if it is loaded, alive and nobody else is on it. */
    private @Nullable AbstractHorse fetchable() {
        if (cowboy.isPassenger() || !(cowboy.level() instanceof ServerLevel level)) {
            return null;
        }
        if (level.isDarkOutside()) {
            return null; // the whole point is that he is off it after dark
        }
        AbstractHorse lead = cowboy.mount(level);
        return lead != null && lead.isAlive() && !lead.isVehicle() ? lead : null;
    }

    @Override
    public boolean canUse() {
        this.mount = fetchable();
        return mount != null;
    }

    @Override
    public boolean canContinueToUse() {
        return fetchable() == mount && mount != null;
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void stop() {
        this.mount = null;
        cowboy.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (mount == null) {
            return;
        }
        cowboy.getLookControl().setLookAt(mount, 30.0F, 30.0F);
        if (cowboy.distanceToSqr(mount) <= MOUNT_RANGE * MOUNT_RANGE) {
            boolean mounted = cowboy.startRiding(mount, true, false);
            if (cowboy.level() instanceof ServerLevel level) {
                DebugAnnounce.say(level, "Cowboy", cowboy.cowboyName()
                        + (mounted ? " is back on his horse" : " could not get back on his horse"),
                        mounted ? ChatFormatting.GRAY : ChatFormatting.RED);
            }
            return;
        }
        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }
        repathCooldown = REPATH_INTERVAL;
        cowboy.getNavigation().moveTo(mount, SPEED);
    }
}
