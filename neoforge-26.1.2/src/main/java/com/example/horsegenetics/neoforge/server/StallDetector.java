package com.example.horsegenetics.neoforge.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;

/**
 * Works out what volume a stall sign is naming.
 *
 * <h2>It never refuses</h2>
 * <b>This used to be a gate, and the gate is what was broken.</b> It required
 * the player to have hung the sign on the outside face of the wall, the room
 * behind it to be sealed with nothing in it but air, and the whole thing to fit
 * inside one block up and one block down of wherever the sign happened to be -
 * and if any of those was untrue it refused to make a stall at all. Between a
 * doorway, a roof beam, an open top, a torch on the wall, and hanging the sign
 * from the inside, essentially every real stable failed one of them, so the
 * feature that stalls exist for could never be reached.
 *
 * <p>So the flood fill stays, but only as a <i>measurement</i>:
 *
 * <ul>
 *   <li><b>Both sides are tried.</b> A sign on a wall has a room on one side or
 *       the other, and which one the player meant is not something they should
 *       have to know. If both close, the smaller is the stall - the bigger one
 *       is the building it is in.</li>
 *   <li><b>Anything a horse could walk through is open</b>, not just air:
 *       torches, carpet, a lantern, a flower, snow. A fence, a wall, a shut gate
 *       or a door still stops the fill, because those are what a stall is made
 *       of.</li>
 *   <li><b>Five blocks of headroom</b> ({@link #DOWN}..{@link #UP} around the
 *       sign), which is a stall with a hay loft rather than a crawlspace.</li>
 *   <li><b>If nothing closes, it falls back</b> to {@link #FALLBACK_RADIUS} - a
 *       plain box in front of the sign. An open-sided pen is still where that
 *       horse lives; it is just not a room.</li>
 * </ul>
 */
public final class StallDetector {

    /** Above this the fill is treated as "did not close" and the other side is tried. */
    public static final int MAX_BLOCKS = 2048;

    /** How far the fill may travel below and above the sign it started from. */
    private static final int DOWN = 1;
    private static final int UP = 3;

    /** Half-width of the box used when neither side of the wall encloses anything. */
    public static final int FALLBACK_RADIUS = 2;

    /**
     * The block span a stall covers ({@code min}..{@code max} inclusive) and how
     * many open cells were in it. {@code enclosed} is false for the fallback box
     * - the stall is registered either way, but the sign says which it got.
     */
    public record Result(BlockPos min, BlockPos max, int blockCount, boolean enclosed) {

        public int sizeX() {
            return max.getX() - min.getX() + 1;
        }

        public int sizeY() {
            return max.getY() - min.getY() + 1;
        }

        public int sizeZ() {
            return max.getZ() - min.getZ() + 1;
        }
    }

    private StallDetector() {
    }

    /**
     * The stall a sign on {@code wall}'s {@code face} names. Always answers.
     *
     * @param wall the block the sign is hung on
     * @param face the face it is hung on - so the sign itself is at
     *             {@code wall.relative(face)} and the two candidate rooms are
     *             the cells either side of it
     */
    public static Result forSign(LevelReader level, BlockPos wall, Direction face) {
        BlockPos behind = wall.relative(face.getOpposite());
        BlockPos front = wall.relative(face);

        Result closed = smaller(fill(level, behind), fill(level, front));
        if (closed != null) {
            return closed;
        }
        // Neither side is a room. The horse still lives in front of its sign.
        return box(isOpen(level, behind) ? behind : front);
    }

    /** Whichever of the two closed, and the tighter one if both did. */
    private static Result smaller(Result a, Result b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.blockCount() <= b.blockCount() ? a : b;
    }

    /**
     * Flood-fill from {@code seed}, or {@code null} if it is solid or the fill
     * ran away without closing.
     */
    private static Result fill(LevelReader level, BlockPos seed) {
        if (!isOpen(level, seed)) {
            return null;
        }
        int seedY = seed.getY();

        Set<BlockPos> filled = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        filled.add(seed.immutable());
        frontier.add(seed.immutable());

        int minX = seed.getX();
        int minY = seed.getY();
        int minZ = seed.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        while (!frontier.isEmpty()) {
            BlockPos p = frontier.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                int dy = n.getY() - seedY;
                if (dy < -DOWN || dy > UP) {
                    continue;
                }
                if (filled.contains(n) || !isOpen(level, n)) {
                    continue;
                }
                if (filled.size() >= MAX_BLOCKS) {
                    return null; // ran away - this side is not a room
                }
                BlockPos immut = n.immutable();
                filled.add(immut);
                frontier.add(immut);
                minX = Math.min(minX, n.getX());
                minY = Math.min(minY, n.getY());
                minZ = Math.min(minZ, n.getZ());
                maxX = Math.max(maxX, n.getX());
                maxY = Math.max(maxY, n.getY());
                maxZ = Math.max(maxZ, n.getZ());
            }
        }

        return new Result(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ),
                filled.size(), true);
    }

    /** The fallback: a plain box around {@code at}, standing height and a little room either side. */
    private static Result box(BlockPos at) {
        BlockPos min = new BlockPos(at.getX() - FALLBACK_RADIUS, at.getY() - DOWN, at.getZ() - FALLBACK_RADIUS);
        BlockPos max = new BlockPos(at.getX() + FALLBACK_RADIUS, at.getY() + 1, at.getZ() + FALLBACK_RADIUS);
        int cells = (FALLBACK_RADIUS * 2 + 1) * (FALLBACK_RADIUS * 2 + 1) * (DOWN + 2);
        return new Result(min, max, cells, false);
    }

    /**
     * A cell the fill passes through: in the world, and not something that stops
     * a horse. Deliberately <b>not</b> {@code isAir} - a stall with a torch in it
     * is still a stall, and requiring bare air is most of why this never worked.
     */
    private static boolean isOpen(LevelReader level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && !level.getBlockState(pos).blocksMotion();
    }
}
