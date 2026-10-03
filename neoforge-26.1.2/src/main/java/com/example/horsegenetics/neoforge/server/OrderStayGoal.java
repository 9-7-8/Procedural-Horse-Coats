package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.HorseOrder;
import com.example.horsegenetics.common.care.HorseOrders;
import com.example.horsegenetics.neoforge.data.HorseOrderAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import java.util.EnumSet;

/**
 * <b>The Stay order</b>: hold MOVE at the spot it was given, and walk back to it after
 * anything that moved the horse. It is also Hunt monsters' way home: that order is
 * anchored too, and between fights the hunter stands at its spot like a stayed horse -
 * and so does a horse told to Guard here, which is Hunt monsters with a short reach.
 * Graze nearby is anchored but roams, so it is {@link OrderGrazeGoal}'s, not this one's.
 * The fight itself is {@link OrderCombatGoal} picking a target and {@code HorseMeleeGoal}
 * (3) out-ranking this to chase it; when the order lets the target go, this walks it back.
 *
 * <p><b>Priority {@link #PRIORITY} (4), not the 1 the treatment recommended.</b> A goal at
 * equal or worse priority is never even asked {@code canUse} while a better one runs, so
 * at 1 a stayed horse would never start eating ({@code HungerFoodGoal}, 3) - and the
 * owner's rule is that an order must never starve a horse. At 4 everything that is a
 * NEED pre-empts it - escape (0), panic and a whistle call (1), food and hunger, sparring
 * and fighting (3) - and the horse is walked back afterwards; everything that is merely
 * idling - herd life (5-7), vanilla's stroll (6) - is held off, because this holds MOVE.
 * {@code BondFollowGoal} shares 4 and stands down under any order.
 *
 * <p>Paused, not cleared, while ridden or on a lead: it resumes when that ends.
 */
public final class OrderStayGoal extends Goal {

    public static final int PRIORITY = 4;

    private static final int REPATH_EVERY = 20;

    private final AbstractHorse horse;
    private BlockPos given;
    private BlockPos anchor;
    private int repath;

    public OrderStayGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (horse.isVehicle() || horse.isLeashed()) {
            return false;
        }
        HorseOrderAttachment o = HorseOrdering.current(horse);
        if (!o.order().stands() || o.anchor().isEmpty()) {
            return false;
        }
        BlockPos given = o.anchor().get();
        if (!given.equals(this.given)) {
            this.given = given;
            anchor = HorseOrdering.groundAt(horse.level(), given);
        }
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
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Across the ground only: a horse on a slab or a step beside its spot is there.
        double slack = HorseOrders.STAY_SLACK_BLOCKS;
        double dx = horse.getX() - (anchor.getX() + 0.5);
        double dz = horse.getZ() - (anchor.getZ() + 0.5);
        if (dx * dx + dz * dz <= slack * slack && Math.abs(horse.getY() - anchor.getY()) <= 1.5) {
            horse.getNavigation().stop();
            return;
        }
        if (--repath <= 0 || horse.getNavigation().isDone()) {
            repath = REPATH_EVERY;
            horse.getNavigation().moveTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5, 1.0);
        }
    }
}
