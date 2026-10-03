package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.HorseOrder;
import com.example.horsegenetics.common.care.HorseOrders;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseOrderAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import java.util.EnumSet;

/**
 * <b>The Graze nearby order</b> (command whistle, Piece 2): the horse lives as it likes -
 * vanilla's stroll, grazing, eating, its herd goals - until it has roamed past
 * {@code orders.graze_radius} of the spot it was given; then this takes MOVE and walks
 * it back until it is well inside ({@link HorseOrders.Tether}).
 *
 * <p><b>Its own goal, not vanilla's home radius.</b> 26.1.2 has {@code Mob.setHomeTo} and
 * {@code isWithinHome}, but vanilla saves that home and a lead sets it, so the two would
 * fight over it (answered when Piece 3 was built; wiki/item-whistles.html#combat-code).
 * This reads the order's anchor and nothing else, so a lead never moves it.
 *
 * <p>Priority {@link #PRIORITY} (4), as the other order goals and for the same reason
 * ({@link OrderStayGoal}): every need pre-empts it, and it pre-empts vanilla's stroll (6)
 * and herd life (5-7) only while the horse is out past its tether. {@code BondFollowGoal}
 * stands down under any order, so a grazing horse never trails its owner off the field.
 *
 * <p>Paused, not cleared, while ridden or on a lead, like every order goal.
 */
public final class OrderGrazeGoal extends Goal {

    /** OrderStayGoal's, written out: bake-behaviour-hierarchy.mjs reads a literal. */
    public static final int PRIORITY = 4;

    private static final int REPATH_EVERY = 20;

    private final AbstractHorse horse;
    private BlockPos given;
    private BlockPos anchor;
    private int repath;

    public OrderGrazeGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return grazing() && ServerConfig.grazeTether().strayed(fromSpotSq());
    }

    @Override
    public boolean canContinueToUse() {
        return grazing() && !ServerConfig.grazeTether().backInside(fromSpotSq());
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (--repath <= 0 || horse.getNavigation().isDone()) {
            repath = REPATH_EVERY;
            horse.getNavigation().moveTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5, 1.0);
        }
    }

    /** Is a Graze nearby order standing on a horse free to carry it out? Sets {@link #anchor}. */
    private boolean grazing() {
        if (horse.isVehicle() || horse.isLeashed()) {
            return false;
        }
        HorseOrderAttachment o = HorseOrdering.current(horse);
        if (o.order() != HorseOrder.GRAZE_NEARBY || o.anchor().isEmpty()) {
            return false;
        }
        BlockPos given = o.anchor().get();
        if (!given.equals(this.given)) {
            this.given = given;
            anchor = HorseOrdering.groundAt(horse.level(), given);
        }
        return true;
    }

    /** Across the ground only, as {@link OrderStayGoal} judges its spot. */
    private double fromSpotSq() {
        double dx = horse.getX() - (anchor.getX() + 0.5);
        double dz = horse.getZ() - (anchor.getZ() + 0.5);
        return dx * dx + dz * dz;
    }
}
