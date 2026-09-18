/*
 * Derived from UsefulCarts (https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2),
 * itself a port of NiftyCarts by jmb19905, originally AstikorCarts by MennoMax.
 * Copyright (c) 2019 MennoMax
 * Copyright (c) 2023 jmb19905
 * Licensed under the MIT License. See LICENSES/UsefulCarts-MIT.txt.
 * Modified for Horse Genetics (NeoForge 26.1.2).
 */
package com.example.horsegenetics.neoforge.carts.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class CartGoalAdder<T extends Entity> {
    private final Class<T> type;

    private final Function<T, GoalSelector> selector;

    private final ImmutableList<@NotNull GoalEntry<T>> goals;

    private CartGoalAdder(final Builder<T> builder) {
        this.type = builder.type;
        this.selector = builder.selector;
        this.goals = builder.goals.build();
    }

    public void onEntityJoinWorld(final Entity entity) {
        if (!entity.level().isClientSide() && this.type.isInstance(entity)) {
            final Set<WrappedGoal> oldGoals = this.getGoals(this.type.cast(entity));
            final List<WrappedGoal> newGoals = new ArrayList<>(oldGoals.size() + this.goals.size());
            for (final GoalEntry<T> goal : this.goals) {
                newGoals.add(new WrappedGoal(goal.priority, goal.factory.apply(this.type.cast(entity))));
            }
            newGoals.addAll(oldGoals);
            oldGoals.clear();
            oldGoals.addAll(newGoals);
        }
    }

    private Set<WrappedGoal> getGoals(final T entity) {
        return this.selector.apply(entity).getAvailableGoals();
    }

    public static <T extends Mob> Builder<T> mobGoal(final Class<T> type) {
        return CartGoalAdder.builder(type, m -> m.goalSelector);
    }

    public static <T extends Mob> Builder<T> mobTarget(final Class<T> type) {
        return CartGoalAdder.builder(type, m -> m.targetSelector);
    }

    public static <T extends Entity> Builder<T> builder(final Class<T> type, final Function<T, GoalSelector> selector) {
        return new CartGoalAdder.Builder<>(type, selector);
    }

    public static final class Builder<T extends Entity> {
        private final Class<T> type;

        private final Function<T, GoalSelector> selector;

        private final ImmutableList.Builder<@NotNull GoalEntry<T>> goals = new ImmutableList.Builder<>();

        private Builder(final Class<T> type, final Function<T, GoalSelector> selector) {
            this.type = type;
            this.selector = selector;
        }

        public Builder<T> add(final int priority, final Function<T, Goal> factory) {
            this.goals.add(new GoalEntry<>(priority, factory));
            return this;
        }

        public CartGoalAdder<T> build() {
            return new CartGoalAdder<>(this);
        }
    }

    private record GoalEntry<T extends Entity>(int priority, Function<T, Goal> factory) {}
}