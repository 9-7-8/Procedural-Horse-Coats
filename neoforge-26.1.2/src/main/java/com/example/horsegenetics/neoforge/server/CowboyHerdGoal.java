package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.CowboyBrand;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Keeps a <b>branded</b> horse - one the cowboy bred and still owns - with the
 * cowboy.
 *
 * <p><b>The man is the lead.</b> They used to ride one of them and the rest
 * trailed the horse they were on, which was the same thing positionally and a
 * great deal more machinery; they do not ride any more, so they follow them
 * directly and there is no lead horse to keep track of.
 *
 * <p>The same shape as {@link WildHerdGoal}, and deliberately not the same goal.
 * A wild herd elects its own lead and knows nothing about a man; this one has a
 * lead chosen for it and a retirement condition - the moment a player redeems
 * the horse's transfer papers it is tamed, the brand is cleared, and this stops
 * running for good.
 *
 * <p>By day it only closes the gap when the gap is real ({@link #FOLLOW_RANGE}),
 * so the herd spreads out and grazes around them instead of stacking on them.
 * After dark it tightens right up and quickens ({@link #NIGHT_RANGE}): a string
 * gathered on its owner is the closest thing to a stable there is.
 */
public final class CowboyHerdGoal extends Goal {

    /** Start closing on the cowboy past this. */
    private static final double FOLLOW_RANGE = 14.0;
    /** Close enough by day. */
    private static final double SETTLE_RANGE = 7.0;
    /** Close enough after dark - gathered on the parked lead rather than strung out. */
    private static final double NIGHT_RANGE = 4.0;

    private static final double SPEED = 1.0;
    /** Keeping up with a lead that may be bolting. */
    private static final double NIGHT_SPEED = 1.5;
    private static final int REPATH_INTERVAL = 20;

    private final AbstractHorse horse;
    private Cowboy cowboy;
    private int repathCooldown;

    public CowboyHerdGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private Cowboy resolveCowboy() {
        if (!(horse.level() instanceof ServerLevel level)) {
            return null;
        }
        CowboyBrand brand = horse.getData(ModAttachments.COWBOY_BRAND.get());
        if (brand == null || brand.cowboy().isEmpty()) {
            return null;
        }
        UUID id = brand.cowboy().get();
        Entity entity = level.getEntity(id);
        return entity instanceof Cowboy c && c.isAlive() ? c : null;
    }

    @Override
    public boolean canUse() {
        if (horse.isTamed() || horse.isVehicle() || horse.isLeashed()) {
            return false; // sold, or somebody is on it
        }
        this.cowboy = resolveCowboy();
        return cowboy != null && needsToMove();
    }

    /**
     * Hysteresis on purpose: it takes {@link #FOLLOW_RANGE} to get a horse
     * moving and {@link #SETTLE_RANGE} to let it stop, so the herd closes
     * properly instead of quitting the instant it is marginally less far away.
     * Testing "is the navigation done?" here would be worse than useless - the
     * goal has not pathed anywhere yet on its first tick, so it would stop
     * before it ever started.
     */
    @Override
    public boolean canContinueToUse() {
        if (horse.isTamed() || horse.isVehicle() || horse.isLeashed()) {
            return false;
        }
        return cowboy != null && cowboy.isAlive() && !settled();
    }

    @Override
    public void stop() {
        this.cowboy = null;
        horse.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }

    private boolean night() {
        return horse.level().isDarkOutside();
    }

    /** The man. They are the lead - see the class comment. */
    private BlockPos target() {
        return cowboy.blockPosition();
    }

    /** Far enough away to be worth setting off. */
    private boolean needsToMove() {
        return !horse.blockPosition().closerThan(target(), night() ? NIGHT_RANGE : FOLLOW_RANGE);
    }

    /** Close enough to stop. */
    private boolean settled() {
        return horse.blockPosition().closerThan(target(), night() ? NIGHT_RANGE : SETTLE_RANGE);
    }

    private double speed() {
        return night() ? NIGHT_SPEED : SPEED;
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        if (cowboy == null) {
            return;
        }
        if (settled()) {
            horse.getNavigation().stop();
            return;
        }
        BlockPos target = target();
        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }
        repathCooldown = REPATH_INTERVAL;
        horse.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed());
    }
}
