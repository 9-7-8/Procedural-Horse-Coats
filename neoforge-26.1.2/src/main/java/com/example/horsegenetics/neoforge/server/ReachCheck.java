package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;

/**
 * "Could this horse actually walk there?" - a real
 * {@link net.minecraft.world.entity.ai.navigation.PathNavigation#createPath}
 * call, which is far too expensive for a <code>canUse</code> that runs every
 * tick for every horse, so the answer is cached for {@link #EVERY} ticks.
 * A path that exists but stops short ({@code !canReach()}) is a fence: it
 * counts as no path.
 *
 * <p>Born inside {@link BondFollowGoal}; shared since #259, when every goal that
 * walks a horse to another horse - {@link HerdGoals}' heat, dam-and-foal,
 * grooming, sparring and displacing, and {@link WildHerdGoal} - turned out to
 * have none, and a mare in heat stood pressed to the rails of the next pen's
 * stallion for as long as her heat lasted. A goal whose target can change calls
 * {@link #reset()} when it picks one, so the cached answer is never about the
 * last target.
 */
final class ReachCheck {

    /** Two seconds - a fence does not move, and a gate opening is not urgent. */
    private static final int EVERY = 40;

    /**
     * Checks in a row that may reuse the answer while neither end has moved a block
     * (#201). A horse standing beside its standing owner has no current path, so every
     * check was a fresh A* for an answer that could not have changed - on every bonded
     * horse near its owner. A gate opening with nobody moving is noticed within
     * {@code EVERY * MAX_STILL_REUSES} ticks instead of {@code EVERY}.
     */
    private static final int MAX_STILL_REUSES = 4;

    /**
     * Inside this, a path that stops short is a fence and nothing else. Past it the
     * search may simply have run out of room - {@code createPath} explores a region
     * the size of the mob's follow range, 16 for a horse unless something changes it
     * (vanilla's {@code Mob} default; <b>not checked against a horse's own attribute
     * table</b>) - so a far target in open ground also gives a short path.
     */
    static final double SURE = 12.0;

    /**
     * {@link #approaching()}: past {@link #SURE}, how far the path's end must be from the
     * horse for it to count as walking toward the target rather than standing at the
     * fence it ends on.
     */
    private static final double PROGRESS = 2.0;

    private final boolean approach;
    private int cooldown;
    private boolean reachable;
    private long horseAt = Long.MIN_VALUE;
    private long targetAt = Long.MIN_VALUE;
    private int stillReuses;

    /** Strict: a short path is no path at any distance. What the bond goals have always asked. */
    ReachCheck() {
        this(false);
    }

    private ReachCheck(boolean approach) {
        this.approach = approach;
    }

    /**
     * For a goal that walks to a horse up to 48 blocks off (a foal to its dam): strict
     * within {@link #SURE}; past it, a short path still counts while it would carry the
     * horse {@link #PROGRESS} blocks or more, so it walks the open ground toward its
     * target and stops at the first fence instead of leaning on it.
     */
    static ReachCheck approaching() {
        return new ReachCheck(true);
    }

    boolean test(Mob horse, LivingEntity target) {
        if (--this.cooldown > 0) {
            return this.reachable;
        }
        this.cooldown = EVERY;
        long h = horse.blockPosition().asLong();
        long t = target.blockPosition().asLong();
        if (h == this.horseAt && t == this.targetAt && this.stillReuses < MAX_STILL_REUSES) {
            this.stillReuses++;
            return this.reachable;
        }
        this.horseAt = h;
        this.targetAt = t;
        this.stillReuses = 0;
        Path path = horse.getNavigation().createPath(target, 0);
        this.reachable = path != null && (path.canReach()
                || this.approach && horse.distanceToSqr(target) > SURE * SURE && path.getEndNode() != null
                        && path.getEndNode().asBlockPos().distSqr(horse.blockPosition()) >= PROGRESS * PROGRESS);
        return this.reachable;
    }

    /** Forget the cached answer: the next {@link #test} asks the navigator. */
    void reset() {
        this.cooldown = 0;
        this.horseAt = Long.MIN_VALUE;
        this.targetAt = Long.MIN_VALUE;
        this.stillReuses = 0;
    }
}
