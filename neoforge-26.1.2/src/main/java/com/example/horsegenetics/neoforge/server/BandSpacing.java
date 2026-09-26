package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * <b>A band does not want to be near another band.</b> The counter-force to
 * cohesion: every other rule in this mod pulls wild horses together, and with
 * nothing pushing back a field of them collapses into one mass and stays there.
 *
 * <h2>The report that prompted it</h2>
 * The owner, on the horse realm (2026-09-25): <i>"they all stay in one giant
 * ball. Since they're wild, shouldn't they form bands and spread out?"</i> Yes -
 * and this is half the answer. The other half is {@code HorseRealmHerds}, where
 * a band that is full now refuses new members, because <b>repulsion between
 * bands does nothing at all while there is only one band</b>, which is what the
 * realm had: one arrival point, and a join rule that put every new horse into
 * whatever band was standing on it.
 *
 * <h2>What it does</h2>
 * On the shared slow tick, a wild banded horse looks for the nearest wild horse
 * <b>of a different band</b>. Inside {@link #SPACING} it walks away from it. That
 * is the whole rule, and it is deliberately a per-horse nudge rather than a
 * band-level manoeuvre:
 *
 * <ul>
 *   <li>Cohesion is per-horse too ({@code WildHerdGoal} pulls each member toward
 *       its lead), so the two forces are the same kind of thing and settle
 *       against each other instead of fighting for the navigation.</li>
 *   <li>A band whose members each edge away from a neighbouring band <b>moves as
 *       a band</b>, because its own cohesion keeps gathering it up as it goes.
 *       Nothing has to compute a band centre, elect who decides, or keep
 *       band-level state.</li>
 *   <li>The horse nearest the other band feels it first and moves furthest,
 *       which is what an actual band edge looks like.</li>
 * </ul>
 *
 * <h2>Why it is not a goal</h2>
 * An {@code AvoidEntityGoal} would run every tick and would need a priority
 * against cohesion, and the loser of that priority would simply never act. This
 * runs on the same 100-tick scan {@code HerdSocialHandler} already pays for over
 * the same entity query, and it uses {@code BandLife.retreat} - the actuator the
 * mod already uses for a horse that has lost a fight or been displaced. A shove
 * that fades after a few seconds and is re-applied while the reason persists is
 * the right shape for a preference; a goal is the right shape for a purpose.
 *
 * <h2>Tamed horses are exempt, both ways</h2>
 * A horse in a paddock cannot act on a preference about where it stands, and a
 * player's two bands of stock should not shuffle away from each other along a
 * fence for ever. This only ever reads and moves <b>untamed banded</b> horses.
 */
@EventBusSubscriber
public final class BandSpacing {

    /**
     * Once every five seconds per horse, staggered by entity id - the same shape
     * and the same reason as every other slow scan in this package.
     */
    private static final int SCAN = 100;

    /**
     * <b>How close another band may come.</b> Comfortably outside
     * {@code HerdManager.HERD_RADIUS} (32), so a band that has settled at this
     * distance is not one the clump rule would have joined up anyway, and well
     * outside {@code WildHerdGoal}'s catch-up range, so a horse is never pushed
     * further than its own band will pull it back.
     */
    private static final double SPACING = 40.0;

    /**
     * How far a nudge aims. Short on purpose: it is one step of a gradient that
     * is re-evaluated every {@link #SCAN} ticks while the neighbour is still
     * there, not a flight. A long retreat would carry a horse clean out of its
     * own band and hand it to {@code HerdSocialHandler.keepBandWhole} to drag
     * back, which would read as two rules arguing.
     */
    private static final double STEP = 10.0;

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.isAlive() || horse.isTamed()) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if ((horse.tickCount + horse.getId()) % SCAN != 0) {
            return;
        }
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        if (!care.inWildHerd()) {
            return;     // a loose horse has no band to keep clear of anything
        }
        UUID mine = care.herd().orElse(null);
        if (mine == null) {
            return;
        }

        Horse nearest = nearestStranger(level, horse, mine);
        if (nearest != null) {
            BandLife.retreat(horse, nearest, STEP);
        }
    }

    /**
     * The nearest untamed horse of another band within {@link #SPACING}, or
     * {@code null}.
     *
     * <p>{@code YardPens.together} is honoured for the same reason every other
     * proximity rule here honours it: two horses in adjacent debug pens are a
     * block apart and are not in the same field, and a test pen full of horses
     * shoving at a fence would make the yard useless for measuring anything
     * else.
     */
    private static Horse nearestStranger(ServerLevel level, Horse horse, UUID mine) {
        List<Horse> near = level.getEntitiesOfClass(Horse.class,
                horse.getBoundingBox().inflate(SPACING),
                other -> other != horse && other.isAlive() && !other.isTamed()
                        && YardPens.together(horse, other)
                        && other.getData(ModAttachments.HORSE_CARE.get()).herd()
                                .map(theirs -> !theirs.equals(mine)).orElse(false));
        Horse best = null;
        double bestDist = Double.MAX_VALUE;
        for (Horse other : near) {
            double d = other.distanceToSqr(horse);
            if (d < bestDist) {
                bestDist = d;
                best = other;
            }
        }
        return best;
    }

    private BandSpacing() {
    }
}
