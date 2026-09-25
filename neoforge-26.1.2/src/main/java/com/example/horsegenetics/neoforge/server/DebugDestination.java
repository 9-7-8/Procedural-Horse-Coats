package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * <b>A goal that can say where it is trying to get to.</b> Implemented by the
 * mod's own {@code Goal}s so the F8 highlight can draw a line out of each horse
 * to the thing it is walking at - see {@link HorseDestinationDebug}.
 *
 * <h2>Why an interface rather than reading the navigation</h2>
 * The navigation answers a different question. {@code PathNavigation.getPath()}
 * gives the path the horse is <i>currently walking</i>, which for an
 * unreachable target is a path that stops at the obstacle - so on its own it
 * says "this horse is walking to that wall" and hides the very fact worth
 * seeing, which is that it <i>wanted</i> the crop behind the wall. The goal is
 * the only thing that knows the intent, so the goal is what is asked.
 *
 * <p>Both are drawn: the intent as an arrow, the path's actual stopping point
 * as a second marker. A horse bunched against a wall is exactly the case where
 * the two disagree, and seeing them disagree is the diagnosis.
 *
 * <h2>Implementors report live state, not a snapshot</h2>
 * {@link #debugDestination()} is called from the server tick while the goal is
 * running, so it must read whatever field the goal is steering by right now and
 * return {@code null} the moment it is steering by nothing. It must not
 * allocate a search or touch the world - it is called for every horse near a
 * player with the highlight on.
 */
public interface DebugDestination {

    /**
     * Where this goal is trying to take the horse, in world coordinates, or
     * {@code null} when it currently has no target. A running goal returning
     * {@code null} is normal - most goals spend most of their time deciding.
     */
    @Nullable
    Vec3 debugDestination();

    /**
     * A short label for the overlay - the goal and what it is after, e.g.
     * {@code "hunger: wheat"}. Kept under
     * {@link com.example.horsegenetics.neoforge.network.HorseDestinationsPayload#MAX_LABEL}
     * characters; anything longer is truncated on the way out rather than
     * refused.
     */
    String debugDestinationLabel();
}
