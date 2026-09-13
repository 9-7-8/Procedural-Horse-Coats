package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Wires the <b>blood diet</b>'s one behaviour - {@link BloodHuntGoal} - onto
 * every horse. The goal itself asks the genome whether it applies, once, so a
 * wild founder whose record lands a tick after it joins is covered too. The
 * refusal of hand-feeding and of passive regen live in {@link HorseDietHandler}
 * with every other diet.
 */
@EventBusSubscriber
public final class BloodDietHandler {

    private BloodDietHandler() {
    }

    /** Beside the melee goal: a hurt blood-drinker hunting is a fight of a kind. */
    public static final int HUNT_GOAL_PRIORITY = 3;

    @SubscribeEvent
    static void addGoal(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Horse horse)) {
            return;
        }
        for (WrappedGoal w : horse.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof BloodHuntGoal) {
                return;
            }
        }
        horse.goalSelector.addGoal(HUNT_GOAL_PRIORITY, new BloodHuntGoal(horse));
    }
}
