package com.example.horsegenetics.common.realm;

/**
 * <b>How much of the F6 debug corridor is built in one server tick.</b>
 *
 * <p>Issue #31: one operator pressing F6 on a live server froze it for about
 * three seconds. Half of that was chunk loads (fixed by building with no shape
 * updates); the other half was building thirty pen segments and their horses
 * in the one tick the player arrived. Nobody walks a pen segment per tick, so
 * the corridor does not need to exist ahead of the player all at once: it
 * needs to stay ahead of them.
 *
 * <p>So entering builds only the segments the test yard overlaps - the yard is
 * cut through the corridor's wall, so those have to stand before it does - and
 * every player tick after that builds at most {@link #SEGMENTS_PER_TICK} more,
 * until the frontier is the lookahead in front of them again. The NeoForge side
 * ({@code server/DebugPenManager}) measures where the player is and builds;
 * this class only says how far.
 *
 * <p>All counts are segment <i>indices</i>, inclusive: {@code -1} means nothing
 * is built yet, and {@code lastSegment} is the corridor's final segment.
 */
public final class CorridorPacing {

    /**
     * Segments built per player tick while the frontier is behind. One segment
     * is seven blocks; a sprinting player covers about a third of a block a
     * tick, so even a single segment a tick closes any gap within a second.
     */
    public static final int SEGMENTS_PER_TICK = 1;

    private CorridorPacing() {
    }

    /**
     * The last segment to build on entry: every segment the test yard's X span
     * reaches, so the yard is written over a finished corridor exactly as it
     * was when the whole lookahead was built first.
     *
     * @param yardEastDx the yard's furthest +X reach from the plot origin
     * @param period     blocks per segment
     */
    public static int entryTarget(int yardEastDx, int period, int lastSegment) {
        return Math.min(Math.floorDiv(Math.max(yardEastDx, 0), period), lastSegment);
    }

    /**
     * The last segment to have built after this tick: towards {@code needed},
     * but no more than {@link #SEGMENTS_PER_TICK} past what already stands,
     * and never past the corridor's end. Never behind what stands either: a
     * player walking back towards the portal builds nothing and unbuilds nothing.
     */
    public static int tickTarget(int highestBuilt, int needed, int lastSegment) {
        int toward = Math.min(Math.min(needed, highestBuilt + SEGMENTS_PER_TICK), lastSegment);
        return Math.max(highestBuilt, toward);
    }
}
