/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.entity.ai.goal;

import com.example.horsegenetics.neoforge.carts.util.CartWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class PullCartGoal extends Goal {
    private final Entity mob;

    public PullCartGoal(final Entity entity) {
        this.mob = entity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * The level's cart store, looked up once per level (#201). This goal is on every Mob in the world and
     * {@code CartWorld.get} is a level lookup plus a saved-data lookup per call; the store does not change
     * for the life of the level.
     */
    private net.minecraft.world.level.Level storeLevel;
    private CartWorld store;

    @Override
    public boolean canUse() {
        net.minecraft.world.level.Level level = this.mob.level();
        if (level.isClientSide()) {
            return CartWorld.get(level).isPulling(mob);
        }
        if (level != this.storeLevel || this.store == null) {
            this.storeLevel = level;
            this.store = CartWorld.get(level);
        }
        return !this.store.getPulling().isEmpty() && this.store.isPulling(mob);
    }
}
