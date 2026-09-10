package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.stable.StallFill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Works out what volume a stall sign is naming. <b>It never refuses</b> - it
 * measures, and says which of the two answers it got.
 *
 * <h2>What this is and is not</h2>
 * The search itself is {@link StallFill}, in {@code common/}, where it has
 * tests. This class is the <b>translator</b>: it answers one question about the
 * world ("could a horse stand in this column, and at what height") and hands
 * the graph problem to code that knows nothing about Minecraft. That split is
 * not decoration. The previous version was entirely here, had nowhere to be
 * tested, and shipped a bug that killed a horse.
 *
 * <h2>The two rules that were wrong</h2>
 * <ol>
 *   <li><b>It filled air upwards.</b> The old fill went three blocks above the
 *       sign, and a fence is one block tall - so it climbed over the front of
 *       every stall anybody has ever built, spilled into the aisle, and
 *       reported "not a room". This one walks the floor and never looks up, so
 *       an open-topped stall is a stall.</li>
 *   <li><b>It judged a doorway by whether a horse could fit through it.</b>
 *       Which means the same stall measured differently depending on whether
 *       the gate happened to be swinging open. A door, a trapdoor and a gate
 *       are now the edge of the room <b>in every state</b> - see
 *       {@link #isDoorway}. That is a statement about what a player means by
 *       "this stall", not about collision.</li>
 * </ol>
 *
 * <p>Everything else a stall really contains is still passable: a torch, a
 * carpet, a lantern, a flower, a water trough, snow. Requiring bare air is most
 * of why the first version never worked at all.
 */
public final class StallDetector {

    /** Above this many floor tiles the fill is treated as "did not close" and the other side is tried. */
    public static final int MAX_COLUMNS = 512;

    /** Cells of headroom a horse needs: the floor cell it stands in, and the one above it. */
    private static final int HEADROOM = 2;

    /** How far below and above the sign the stall floor is allowed to sit. */
    private static final int FLOOR_BELOW = 3;
    private static final int FLOOR_ABOVE = 1;

    /** Half-width of the box used when neither side of the wall encloses anything. */
    public static final int FALLBACK_RADIUS = 2;

    /**
     * The stall a sign names: the floor tiles it covers ({@code region}), the
     * block span they occupy, and whether it is a real room or the fallback box.
     *
     * <p><b>{@code region} is the authority and {@code min}/{@code max} are only
     * a drawing hint.</b> A bounding box around an L-shaped room contains the
     * wall between the arms; choosing a spot from the box is what put a horse
     * inside one. Anything placing a horse must walk {@link #region}, and gets
     * {@code null} for the fallback box, which is not a room and has no floor
     * that was ever checked.
     */
    public record Result(StallFill.Region region, BlockPos min, BlockPos max, int blockCount, boolean enclosed) {

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
        BlockPos front = wall.relative(face);
        BlockPos behind = wall.relative(face.getOpposite());

        Result closed = smaller(fill(level, front), fill(level, behind));
        if (closed != null) {
            return closed;
        }
        // Neither side is a room. The horse still lives in front of its sign -
        // and "in front" means the side the player was standing on when they
        // hung it, which is the only statement of intent available here.
        return box(front);
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

    /** Walk the floor from the cell {@code seed}, or {@code null} if it is not a room. */
    private static Result fill(LevelReader level, BlockPos seed) {
        int signY = seed.getY();
        int seedFloor = seedFloorY(level, seed.getX(), seed.getZ(), signY);
        if (seedFloor == StallFill.NONE) {
            return null;
        }
        StallFill.Region region = StallFill.fill(
                (x, z, nearY) -> floorY(level, x, z, nearY, signY),
                seed.getX(), seed.getZ(), seedFloor, MAX_COLUMNS);
        if (region == null) {
            return null;
        }
        return new Result(region,
                new BlockPos(region.minX(), region.minY(), region.minZ()),
                // The span a horse occupies, so the outline drawn round it is the
                // room and not just the floor slice.
                new BlockPos(region.maxX(), region.maxY() + HEADROOM - 1, region.maxZ()),
                region.size(), true);
    }

    /**
     * The floor under the <b>seed</b> column, which gets a full scan of the
     * band rather than the neighbour-relative step every other column gets.
     *
     * <p>A sign is hung where a player can read it - head height, two or three
     * blocks above the floor they are standing on. Starting the walk at the
     * sign's own Y and only ever looking one block either way found nothing at
     * all in an ordinary stall, so the search is done from the top down: the
     * first standable surface below the sign is the floor a player means, and
     * anything under that is a cellar.
     */
    private static int seedFloorY(LevelReader level, int x, int z, int signY) {
        for (int y = signY + FLOOR_ABOVE; y >= signY - FLOOR_BELOW; y--) {
            if (standable(level, x, y, z)) {
                return y;
            }
        }
        return StallFill.NONE;
    }

    /**
     * The Y a horse standing in column {@code (x, z)} would stand at, or
     * {@link StallFill#NONE}.
     *
     * <p>Candidates are the neighbour's own height first, then one down, then
     * one up - so a flat floor stays flat and a slab or a dropped step is still
     * the same room. All of them must sit within the band around the sign,
     * which is what stops a stall walking down a staircase and out of the
     * building.
     */
    private static int floorY(LevelReader level, int x, int z, int nearY, int signY) {
        for (int dy : new int[] {0, -1, 1}) {
            int y = nearY + dy;
            if (y < signY - FLOOR_BELOW || y > signY + FLOOR_ABOVE) {
                continue;
            }
            if (standable(level, x, y, z)) {
                return y;
            }
        }
        return StallFill.NONE;
    }

    /** Room to stand in this column at {@code y}: something underfoot, and clear cells above. */
    private static boolean standable(LevelReader level, int x, int y, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y - 1, z);
        if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).blocksMotion()) {
            return false; // nothing to stand on - a drop, or open air
        }
        for (int i = 0; i < HEADROOM; i++) {
            p.set(x, y + i, z);
            if (level.isOutsideBuildHeight(p)) {
                return false;
            }
            BlockState state = level.getBlockState(p);
            if (state.blocksMotion() || isDoorway(state)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A door, trapdoor or fence gate - the edge of a room whatever state it is
     * in.
     *
     * <p>Deliberately by <b>block identity</b> and not by collision. An open
     * gate has no collision at all, so a rule based on what a horse could walk
     * through measures a different stall on Tuesday than it did on Monday, and
     * the version that did exactly that leaked into the aisle and got a horse
     * killed. A player who builds a gate has said where the stall ends.
     */
    private static boolean isDoorway(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.getBlock() instanceof TrapDoorBlock;
    }

    /**
     * <b>Somewhere in {@code result} a horse can actually be put</b>, or
     * {@code null} if there is nowhere - in which case the caller must refuse
     * rather than invent a spot.
     *
     * <p>This is the function whose absence killed a horse. The old landing
     * code scanned a bounding box, and when that found nothing it fell through
     * to {@code return stall.signPos()} - the block the sign itself occupies,
     * flush against a wall, with the wall's material at head height. It was the
     * one position in the whole routine that was never checked for anything,
     * and it was reached exactly when the stall was least understood.
     *
     * <p>A real room answers from its own floor tiles. The fallback box has no
     * floor that was ever checked, so every cell in it is tested here, nearest
     * the middle first.
     */
    public static BlockPos landingSpot(LevelReader level, Result result) {
        if (result.region() != null) {
            StallFill.Column c = result.region().middle();
            return new BlockPos(c.x(), c.y(), c.z());
        }
        BlockPos min = result.min();
        BlockPos max = result.max();
        double cx = (min.getX() + max.getX()) / 2.0;
        double cz = (min.getZ() + max.getZ()) / 2.0;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        // Downwards, so a horse lands on the pen's floor rather than on a
        // fence post or a hay bale that happens to have air over it.
        for (int y = max.getY(); y >= min.getY(); y--) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (!standable(level, x, y, z)) {
                        continue;
                    }
                    double dx = x - cx;
                    double dz = z - cz;
                    double d = dx * dx + dz * dz;
                    if (d < bestDistance) {
                        bestDistance = d;
                        best = new BlockPos(x, y, z);
                    }
                }
            }
        }
        return best;
    }

    /**
     * The fallback: a plain box in front of the sign, for an open pen that
     * encloses nothing.
     *
     * <p>It carries a {@code null} region, which is the point - <b>no cell in
     * this box has been checked for a floor or for headroom</b>, so nothing may
     * teleport a horse into it without checking first.
     */
    private static Result box(BlockPos at) {
        BlockPos min = new BlockPos(at.getX() - FALLBACK_RADIUS, at.getY() - 1, at.getZ() - FALLBACK_RADIUS);
        BlockPos max = new BlockPos(at.getX() + FALLBACK_RADIUS, at.getY() + 1, at.getZ() + FALLBACK_RADIUS);
        int cells = (FALLBACK_RADIUS * 2 + 1) * (FALLBACK_RADIUS * 2 + 1) * 3;
        return new Result(null, min, max, cells, false);
    }
}
