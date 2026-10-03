package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.HorseOrder;
import com.example.horsegenetics.common.care.HorseOrders;
import com.example.horsegenetics.neoforge.data.HorseOrderAttachment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * <b>The Follow order</b>, and Defend me's heel: follow the player who gave it, at a walk, stopping
 * {@link HorseOrders#FOLLOW_STOP_BLOCKS} away. More than
 * {@link HorseOrders#FOLLOW_TELEPORT_BLOCKS} behind with no path that reaches them, it is
 * brought to them the way a whistle brings a horse ({@code WhistleCalls.teleportTo}).
 *
 * <p>Same priority as {@link OrderStayGoal}, for the same reason. Paused, not cleared,
 * while ridden, on a lead, or while that player is in another dimension or offline.
 * Unlike {@code BondFollowGoal} it never gives up: it is an order, not a mood.
 */
public final class OrderFollowGoal extends Goal {

    /** OrderStayGoal's, written out: bake-behaviour-hierarchy.mjs reads a literal. */
    public static final int PRIORITY = 4;

    private static final int REPATH_EVERY = 10;

    private final AbstractHorse horse;
    private ServerPlayer leader;
    private int repath;

    public OrderFollowGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (horse.isVehicle() || horse.isLeashed()) {
            return false;
        }
        HorseOrderAttachment o = HorseOrdering.current(horse);
        if (!o.order().follows()) {
            return false;
        }
        ServerPlayer p = HorseOrdering.orderedByHere(horse, o);
        if (p == null || p.level() != horse.level()) {
            return false;
        }
        leader = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        leader = null;
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        horse.getLookControl().setLookAt(leader, 10.0F, horse.getMaxHeadXRot());
        double stop = HorseOrders.FOLLOW_STOP_BLOCKS;
        double distSq = horse.distanceToSqr(leader);
        if (distSq <= stop * stop) {
            horse.getNavigation().stop();
            return;
        }
        if (--repath > 0) {
            return;
        }
        repath = REPATH_EVERY;
        double far = HorseOrders.FOLLOW_TELEPORT_BLOCKS;
        if (distSq > far * far) {
            Path path = horse.getNavigation().createPath(leader, 0);
            if (path == null || !path.canReach()) {
                WhistleCalls.teleportTo(leader, horse, 0);
                return;
            }
        }
        horse.getNavigation().moveTo(leader, 1.0);
    }
}
