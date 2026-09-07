package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * <b>Hostile mobs leave the cowboy alone.</b>
 *
 * <h2>Why he needs it</h2>
 * Because he has nowhere to go. A villager survives the night by walking into a
 * house and getting into a bed, and that behaviour lives in vanilla's
 * <b>villager brain</b> - which a {@link Cowboy} does not have and cannot have:
 * he is an {@code AbstractVillager}, the same base as a wandering trader, chosen
 * precisely so that no schedule drags him away from his horses. A wandering
 * trader does not go indoors either.
 *
 * <p>Giving him one was tried at length and is what the whole barn-shelter
 * subsystem was; it is written up in {@code wiki/known-gaps.html} and it is
 * gone. So the night is survived rather than avoided: ten times a villager's
 * health ({@link Cowboy#HEALTH}), and nothing hunting him.
 *
 * <h2>How</h2>
 * {@code LivingChangeTargetEvent} fires from {@code Mob.setTarget}, which is the
 * one door every way of acquiring a target goes through - the nearest-attackable
 * goals, a raid's orders, retaliation for being hit. Cancel it when an
 * {@link Enemy} is about to pick him and he is simply never anybody's target,
 * so nothing paths to him and nothing swings at him.
 *
 * <p>{@link Enemy} rather than {@code Monster} deliberately: it is the interface
 * that means "this thing is hostile", and it catches the few hostiles that are
 * not {@code Monster} subclasses. It does not catch players, who can still hit
 * him, or wolves, or anything defending itself.
 *
 * <p><b>His horses are not covered.</b> They are ordinary animals and a wolf
 * getting one is a thing that happens; that is what the restocking is for.
 */
@EventBusSubscriber
public final class CowboySafetyHandler {

    private CowboySafetyHandler() {
    }

    @SubscribeEvent
    static void onTargetChange(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Cowboy && event.getEntity() instanceof Enemy) {
            event.setCanceled(true);
        }
    }
}
