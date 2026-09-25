package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.network.HorseDestinationsPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>The data half of the F8 destination lines.</b> Walks the horses near a
 * player with the highlight on, works out where each one is trying to go and
 * who decided that, and pushes the lot to that player's client, which draws it
 * ({@code HorseDestinationRenderer}).
 *
 * <h2>What "destination" means here, and why it is asked in that order</h2>
 * Three different things can answer "where is this horse walking", and they are
 * tried worst-last:
 * <ol>
 *   <li>The <b>mover</b>'s own answer. The mover is the running goal that holds
 *       {@link Goal.Flag#MOVE} at the lowest priority number - by definition the
 *       one goal currently steering the horse. If it implements
 *       {@link DebugDestination} it is asked directly, and that is the honest
 *       answer: the intent, before pathfinding had an opinion.</li>
 *   <li>Any other running {@link DebugDestination}, lowest priority first. A
 *       goal can want something while a higher-priority goal holds MOVE, and
 *       seeing what it wants is often the point.</li>
 *   <li>Failing both, the <b>navigation's</b> target, labelled with the mover's
 *       class name. This is what makes the overlay useful for vanilla goals too
 *       - nothing has to implement anything to show up as a line.</li>
 * </ol>
 *
 * <h2>Why the path's last node is sent as well</h2>
 * A path to an unreachable target is not absent, it is <i>short</i>: it stops at
 * the obstacle and {@link Path#canReach()} goes false. So the intent alone would
 * draw a line straight through a wall with no hint that the horse knows it
 * cannot get there, and the path alone would draw a line to the wall with no
 * hint that the wall is not what it wanted. Both are sent; the client draws the
 * intent as an arrow and the stopping point as a marker, and a horse bunched
 * against a wall shows as an arrow that overshoots its own marker.
 *
 * <h2>Cost</h2>
 * Runs every {@value #PUSH_INTERVAL} ticks, only for players with the highlight
 * on, and touches nothing but fields the goals have already computed -
 * {@link DebugDestination} implementors are required not to search. No path is
 * ever created here: {@link net.minecraft.world.entity.ai.navigation.PathNavigation#getPath()}
 * returns the one the horse is already walking.
 */
public final class HorseDestinationDebug {

    /**
     * Half a second. The line's <i>origin</i> is the live client-side horse, so
     * this only paces how often the far end moves - and a destination is a
     * decision, which changes far more slowly than a position does.
     */
    public static final int PUSH_INTERVAL = 10;

    private HorseDestinationDebug() {
    }

    /**
     * Gather and send. Radius is the caller's, so it matches the set of horses
     * the highlight has actually lit - a line out of an unlit horse would be a
     * second, different claim about which horses are "near".
     */
    public static void push(ServerPlayer player, double radius) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<HorseDestinationsPayload.Entry> entries = new ArrayList<>();
        for (Horse horse : level.getEntitiesOfClass(Horse.class, player.getBoundingBox().inflate(radius))) {
            HorseDestinationsPayload.Entry entry = describe(horse);
            if (entry != null) {
                entries.add(entry);
            }
        }
        // Nearest first, then truncate: past the cap the ones worth keeping are
        // the ones the player can actually see.
        if (entries.size() > HorseDestinationsPayload.MAX_ENTRIES) {
            Vec3 eye = player.getEyePosition();
            entries.sort(Comparator.comparingDouble(e -> eye.distanceToSqr(e.tx(), e.ty(), e.tz())));
            entries = entries.subList(0, HorseDestinationsPayload.MAX_ENTRIES);
        }
        PacketDistributor.sendToPlayer(player, new HorseDestinationsPayload(entries));
    }

    /**
     * Push an empty list, so the lines go out the moment the toggle does rather
     * than lingering until the client's staleness timer notices.
     */
    public static void clear(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new HorseDestinationsPayload(List.of()));
    }

    /** One horse, or null when it is not trying to walk anywhere at all. */
    private static @Nullable HorseDestinationsPayload.Entry describe(Horse horse) {
        WrappedGoal mover = mover(horse);
        Vec3 target = null;
        String label = null;

        // 1. The goal actually steering, if it can say.
        if (mover != null && mover.getGoal() instanceof DebugDestination dd) {
            target = dd.debugDestination();
            if (target != null) {
                label = dd.debugDestinationLabel();
            }
        }
        // 2. Any other running goal that wants something.
        if (target == null) {
            WrappedGoal best = null;
            Vec3 bestTarget = null;
            for (WrappedGoal wrapped : horse.goalSelector.getAvailableGoals()) {
                if (!wrapped.isRunning() || !(wrapped.getGoal() instanceof DebugDestination dd)) {
                    continue;
                }
                if (best != null && wrapped.getPriority() >= best.getPriority()) {
                    continue;
                }
                Vec3 wants = dd.debugDestination();
                if (wants != null) {
                    best = wrapped;
                    bestTarget = wants;
                }
            }
            if (best != null) {
                target = bestTarget;
                label = ((DebugDestination) best.getGoal()).debugDestinationLabel();
            }
        }

        Path path = horse.getNavigation().getPath();

        // 3. Whatever the navigation is aiming at, named for whoever aimed it.
        if (target == null) {
            if (path == null) {
                return null;
            }
            BlockPos aim = path.getTarget();
            target = Vec3.atCenterOf(aim);
            label = shortName(mover);
        }

        byte state;
        boolean hasStop = false;
        Vec3 stop = Vec3.ZERO;
        if (path == null) {
            state = HorseDestinationsPayload.STATE_NO_PATH;
        } else {
            state = path.canReach()
                    ? HorseDestinationsPayload.STATE_REACHES
                    : HorseDestinationsPayload.STATE_BLOCKED;
            int count = path.getNodeCount();
            if (count > 0) {
                Node last = path.getNode(count - 1);
                hasStop = true;
                stop = last.asVec3().add(0.5, 0.5, 0.5);
            }
        }

        return new HorseDestinationsPayload.Entry(
                horse.getId(),
                target.x, target.y, target.z,
                hasStop, stop.x, stop.y, stop.z,
                state,
                truncate(label));
    }

    /**
     * The running goal holding {@link Goal.Flag#MOVE} at the lowest priority
     * number - the one steering. Null when nothing is: a horse standing still
     * because no movement goal wants anything is a real and common state, and
     * the overlay says so by drawing no line.
     *
     * <p>{@code getAvailableGoals()} is a {@code Set} with no useful iteration
     * order, so the lowest priority is found by comparison rather than by taking
     * the first hit.
     */
    private static @Nullable WrappedGoal mover(Horse horse) {
        WrappedGoal best = null;
        for (WrappedGoal wrapped : horse.goalSelector.getAvailableGoals()) {
            if (!wrapped.isRunning() || !wrapped.getFlags().contains(Goal.Flag.MOVE)) {
                continue;
            }
            if (best == null || wrapped.getPriority() < best.getPriority()) {
                best = wrapped;
            }
        }
        return best;
    }

    /** {@code HungerFoodGoal} reads better than the package-qualified name. */
    private static String shortName(@Nullable WrappedGoal wrapped) {
        if (wrapped == null) {
            return "navigation";
        }
        String name = wrapped.getGoal().getClass().getSimpleName();
        return name.isEmpty() ? "goal" : name;
    }

    /** Truncated rather than refused - a clipped label still names the goal. */
    private static String truncate(@Nullable String label) {
        if (label == null || label.isEmpty()) {
            return "?";
        }
        return label.length() <= HorseDestinationsPayload.MAX_LABEL
                ? label
                : label.substring(0, HorseDestinationsPayload.MAX_LABEL);
    }
}
