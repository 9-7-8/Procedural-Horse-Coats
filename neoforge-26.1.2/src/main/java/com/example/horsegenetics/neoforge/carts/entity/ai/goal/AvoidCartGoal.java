/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.entity.ai.goal;

import com.example.horsegenetics.neoforge.carts.entity.AbstractDrawnEntity;
import com.example.horsegenetics.neoforge.carts.util.CartTargetingUtil;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class AvoidCartGoal<T extends AbstractDrawnEntity>
        extends Goal {
    protected final PathfinderMob mob;
    private final double walkSpeedModifier;
    @Nullable
    protected AbstractDrawnEntity toAvoid;
    protected final float maxDist;
    @Nullable
    protected Path path;
    protected final PathNavigation pathNav;
    protected final Class<T> avoidClass;
    protected final Predicate<AbstractDrawnEntity> avoidPredicate;
    protected final Predicate<AbstractDrawnEntity> predicateOnAvoidEntity;
    private CartTargetingUtil.Conditions<AbstractDrawnEntity> avoidEntityTargeting;

    public AvoidCartGoal(PathfinderMob pathfinderMob, Class<T> class_, float f, double d) {
        this(pathfinderMob, class_, livingEntity -> true, f, d, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
    }

    public AvoidCartGoal(PathfinderMob pathfinderMob, Class<T> class_, Predicate<AbstractDrawnEntity> predicate, float f, double d, Predicate<AbstractDrawnEntity> predicate2) {
        this.mob = pathfinderMob;
        this.avoidClass = class_;
        this.avoidPredicate = predicate;
        this.maxDist = f;
        this.walkSpeedModifier = d;
        this.predicateOnAvoidEntity = predicate2;
        this.pathNav = pathfinderMob.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        this.avoidEntityTargeting = new CartTargetingUtil.Conditions<>();
        this.avoidEntityTargeting = this.avoidEntityTargeting.range(f).selector(predicate2.and(predicate));
    }

    public AvoidCartGoal(PathfinderMob pathfinderMob, Class<T> class_, float f, double d, Predicate<AbstractDrawnEntity> predicate) {
        this(pathfinderMob, class_, livingEntity -> true, f, d, predicate);
    }

    @Override
    public boolean canUse() {
        List<? extends AbstractDrawnEntity> entityList = this.mob.level().getEntitiesOfClass(this.avoidClass, this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist), entity -> true);
        this.toAvoid = CartTargetingUtil.getNearestEntity(entityList, this.avoidEntityTargeting, this.mob, this.mob.getX(), this.mob.getY(), this.mob.getZ());
        if (this.toAvoid == null) {
            return false;
        }
        Vec3 vec3 = DefaultRandomPos.getPosAway(this.mob, 5, 7, this.toAvoid.position());
        if (vec3 == null) {
            return false;
        }
        if (this.toAvoid.distanceToSqr(vec3.x, vec3.y, vec3.z) < this.toAvoid.distanceToSqr(this.mob)) {
            return false;
        }
        this.path = this.pathNav.createPath(vec3.x, vec3.y, vec3.z, 0);
        return this.path != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.pathNav.isDone();
    }

    @Override
    public void start() {
        this.pathNav.moveTo(this.path, this.walkSpeedModifier);
    }

    @Override
    public void stop() {
        this.toAvoid = null;
    }

    @Override
    public void tick() {
        this.mob.getNavigation().setSpeedModifier(this.walkSpeedModifier);
    }
}