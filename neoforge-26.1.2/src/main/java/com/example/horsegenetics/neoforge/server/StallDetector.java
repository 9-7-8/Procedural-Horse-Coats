package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.stable.StallFill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Works out what volume a stall sign is naming, or <b>refuses</b>.
 *
 * <h2>It used to never refuse, and that was the bug</h2>
 * When neither side of the wall enclosed anything it returned a blind
 * {@code 5x3x5} box centred in front of the sign - which is not a room, was
 * never checked, and routinely contains the wall the sign is hung on. Owner,
 * 2026-09-13: <i>"the tight stall detects an area in front of the sign, which
 * includes the wall &hellip; If it doesn't find a good area, it should just pop
 * off and refuse to place. l shaped stall, same issue. roofed stall isn't
 * successfully detecting either, it looks like none of them are."</i>
 *
 * <p>All four read the same because all four were falling back: the yard built
 * them with a two-block <em>hole</em> for a doorway and nothing in it, so the
 * fill walked out through the gap into the open yard, ran past
 * {@link #MAX_COLUMNS} and gave up. A hole is not a doorway -
 * {@link #isDoorway} counts a door, a trapdoor or a gate, and air is none of
 * them. <b>The fallback box turned every one of those failures into a
 * plausible-looking success</b>, which is why it took a person standing in one
 * to notice.
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
 *   <li><b>It asked for two blocks of clear air over every tile.</b> So a
 *       roofed stall, a low doorway or a beam across the back failed to
 *       validate at all, and the stalls that did pass shoved a dismounting
 *       rider into the ceiling. A stall is its floor and the boundary round it;
 *       see {@link #HEADROOM}.</li>
 * </ol>
 *
 * <p>Everything else a stall really contains is still passable: a torch, a
 * carpet, a lantern, a flower, a water trough, snow. Requiring bare air is most
 * of why the first version never worked at all.
 */
public final class StallDetector {

    /** Above this many floor tiles the fill is treated as "did not close" and the other side is tried. */
    public static final int MAX_COLUMNS = 512;

    /**
     * <b>Cells that must be clear for a column to be inside the stall: one.</b>
     * The floor cell itself, and nothing above it.
     *
     * <p>It was two, and two is what testers hit: a stall whose entrance - or
     * whose ceiling, or a beam across it - left only one clear cell failed to
     * validate, and the ones that did validate pushed a dismounting rider up
     * into the block overhead and hurt them. Owner's call, 2026-09-17: a stall
     * is <b>its floor and the fence, gate, wall or door around it</b>, and
     * height is not part of the question. A roofed stall, a low entrance and an
     * open-topped paddock now all measure the same way.
     *
     * <p>This loosens what counts as a <i>room</i>; it does not loosen where a
     * horse may be <i>put</i>. {@link #landingSpot} still tests the horse's real
     * bounding box with {@code noCollision}, so a room too low to hold one is
     * still refused at the moment something tries to stand in it - which is the
     * check that belongs on the horse rather than on the architecture.
     */
    private static final int HEADROOM = 1;

    /** How far below and above the sign the stall floor is allowed to sit. */
    private static final int FLOOR_BELOW = 3;
    private static final int FLOOR_ABOVE = 1;


    /**
     * The stall a sign names: the floor tiles it covers ({@code region}), the
     * block span they occupy, and whether it is a real room or the fallback box.
     *
     * <p><b>{@code region} is the authority and {@code min}/{@code max} are only
     * a drawing hint.</b> A bounding box around an L-shaped room contains the
     * wall between the arms; choosing a spot from the box is what put a horse
     * inside one. Anything placing a horse must walk {@link #region}.
     */
    public record Result(StallFill.Region region, BlockPos min, BlockPos max, int blockCount) {

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
     * The stall a sign on {@code wall}'s {@code face} names, or {@code null} if
     * neither side of the wall is an enclosed room.
     *
     * @param wall the block the sign is hung on
     * @param face the face it is hung on - so the sign itself is at
     *             {@code wall.relative(face)} and the two candidate rooms are
     *             the cells either side of it
     */
    @Nullable
    public static Result forSign(LevelReader level, BlockPos wall, Direction face) {
        Direction along = face.getClockWise();
        // Null when neither side closes. The caller refuses to place rather
        // than inventing a room - see the class note.
        return smaller(nearestRoom(level, wall.relative(face), along),
                nearestRoom(level, wall.relative(face.getOpposite()), along));
    }

    /**
     * How far along the wall, each way, a sign looks for floor when the square
     * straight across from it is solid.
     */
    private static final int SEED_REACH = 3;

    /**
     * <b>The room on one side of the wall, seeded from the nearest open floor
     * along it</b> rather than only from the one square straight across.
     *
     * <p>That one square was the whole search, and an L-shaped stall showed
     * why it is not enough (owner, 2026-09-13: the L refused). A sign hung on
     * the stretch of wall that backs onto the L's solid notch found a block of
     * stone straight behind it, had no floor to start from, and called a
     * perfectly good room "not enclosed" - while the room was one column over.
     * Any irregular stall has walls like that.
     *
     * <p>So the columns along the wall are tried nearest first - straight
     * across, then one either side, then two - and <b>the first one with floor
     * decides this side</b>, whether its fill closes or not. Stopping there
     * rather than trying every column matters twice: an open side (the aisle)
     * costs one flood instead of seven, and a search that kept going past open
     * floor could end up seeding a different stall on the far side of a divider.
     */
    @Nullable
    private static Result nearestRoom(LevelReader level, BlockPos across, Direction along) {
        for (int step = 0; step <= SEED_REACH * 2; step++) {
            int offset = step % 2 == 1 ? -((step + 1) / 2) : step / 2;  // 0, -1, +1, -2, +2 ...
            BlockPos seed = across.relative(along, offset);
            if (seedFloorY(level, seed.getX(), seed.getZ(), seed.getY()) == StallFill.NONE) {
                continue;  // solid here - try the next column along the wall
            }
            return fill(level, seed);
        }
        return null;
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
                // Up to the ceiling, so the size reported and the outline drawn
                // are the room and not just the two cells a horse needs. It used
                // to stop at HEADROOM, so every stall read as two blocks tall
                // (owner-reported 2026-09-10).
                new BlockPos(region.maxX(), region.maxY() + ceilingHeight(level, region) - 1, region.maxZ()),
                region.size());
    }

    /** How far up a room is measured before it is called open-topped. */
    private static final int MAX_HEIGHT = 8;

    /**
     * <b>The room's height</b>: from the floor to the first solid block above,
     * the tallest over every tile, capped at {@link #MAX_HEIGHT} for an
     * open-topped stall. Measured only for the report and the outline - the
     * fill itself still never looks up, which is what keeps an open-topped
     * stall a stall.
     */
    private static int ceilingHeight(LevelReader level, StallFill.Region region) {
        int tallest = HEADROOM;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (StallFill.Column c : region.columns()) {
            int h = HEADROOM;
            while (h < MAX_HEIGHT) {
                p.set(c.x(), c.y() + h, c.z());
                if (level.isOutsideBuildHeight(p) || level.getBlockState(p).blocksMotion()) {
                    break;
                }
                h++;
            }
            tallest = Math.max(tallest, h);
        }
        return tallest;
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

    /** Room to stand in this column at {@code y}: something underfoot, and the cell itself clear. */
    private static boolean standable(LevelReader level, int x, int y, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y - 1, z);
        if (level.isOutsideBuildHeight(p)) {
            return false;
        }
        BlockState under = level.getBlockState(p);
        // A SIGN IS NOT A FLOOR - and the stall's own sign was being read as one.
        // Signs are force-solid in vanilla, so blocksMotion() is true for them,
        // which made the top of a sign hung on the outside of a stall a
        // standable one-block "room": the sign underneath, open sky above,
        // nothing beside it to spread into, so the fill closed at one tile. At
        // bind time the sign did not exist yet and that square was open air, so
        // the real stall won; at ticket time the sign was there, forSign found
        // both, and smaller() picked the one-tile room. The trace said it
        // outright on 2026-09-13 - "room 9, 131, 74 to 9, 138, 74 (1 tiles)" for
        // a sign at 9, 130, 74 - and it is exactly the owner's report that the
        // ticket put the horse "right above the sign": a 1.4-wide horse centred
        // on that square overlaps the wall behind it, which is the inWall damage.
        if (!under.blocksMotion() || under.getBlock() instanceof SignBlock) {
            return false; // nothing to stand on - a drop, open air, or a sign
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
     * <p>A real room answers from its own floor, and the answer is the <b>dead
     * centre</b> of it ({@link StallFill.Region#centroid}), checked against the
     * horse's actual box. It used to be the centre of the middle <i>tile</i>,
     * and a horse is about 1.4 blocks wide, so in a stall two wide it always
     * arrived overhanging a side wall - owner-reported 2026-09-10 as "still
     * teleporting horses into walls". If the centre does not fit (the corner
     * of an L, a raised tile), the tile centres are tried nearest-first; if
     * nothing fits at all - a stall narrower or lower than the horse - it
     * refuses, because a horse put where its box collides is a horse inside a
     * block.
     *
     * <p>The fallback box has no floor that was ever checked, so every cell in
     * it is tested here, nearest the middle first.
     *
     * @return the exact position to put the horse's feet, or {@code null}
     */
    public static Vec3 landingSpot(LevelReader level, Result result, Entity horse) {
        if (result.region() != null) {
            StallFill.Region region = result.region();
            double[] c = region.centroid();
            StallFill.Column under = region.columnAt((int) Math.floor(c[0]), (int) Math.floor(c[1]));
            if (under != null) {
                Vec3 centre = new Vec3(c[0], footprintFloor(region, c[0], c[1], horse), c[1]);
                if (fits(level, horse, centre)) {
                    return centre;
                }
            }
            List<StallFill.Column> byDistance = new ArrayList<>(region.columns());
            byDistance.sort(Comparator.comparingDouble(col ->
                    sq(col.x() + 0.5 - c[0]) + sq(col.z() + 0.5 - c[1])));
            for (StallFill.Column col : byDistance) {
                Vec3 at = new Vec3(col.x() + 0.5,
                        footprintFloor(region, col.x() + 0.5, col.z() + 0.5, horse), col.z() + 0.5);
                if (fits(level, horse, at)) {
                    return at;
                }
            }
            // NOTHING FITS: refuse. This used to put the horse at dead centre
            // anyway, as "the least-bad place" - which, for a horse whose box
            // collides at every tile, is a horse placed inside a block. On
            // 2026-09-13 a ticketed horse took inWall damage eight times in four
            // seconds in the tight stall. Whether this branch is what did it is
            // not yet established (TicketHandler now logs the room and the spot
            // it chose, to settle that), but a spot that fails the fit test is
            // never a safe answer whatever else is wrong.
            return null;
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
        return best == null ? null : Vec3.atBottomCenterOf(best);
    }

    /** Would the horse's box, with its feet at {@code at}, touch anything solid? */
    private static boolean fits(LevelReader level, Entity horse, Vec3 at) {
        AABB box = horse.getDimensions(horse.getPose()).makeBoundingBox(at);
        return level.noCollision(horse, box);
    }

    /**
     * The floor height under a horse centred at {@code (x, z)}: the highest
     * tile its footprint covers, so a raised tile under one corner lifts it
     * rather than burying that corner.
     */
    private static double footprintFloor(StallFill.Region region, double x, double z, Entity horse) {
        double half = horse.getBbWidth() / 2.0;
        int y = Integer.MIN_VALUE;
        for (StallFill.Column col : region.columns()) {
            if (col.x() + 1 > x - half && col.x() < x + half
                    && col.z() + 1 > z - half && col.z() < z + half) {
                y = Math.max(y, col.y());
            }
        }
        return y == Integer.MIN_VALUE ? region.middle().y() : y;
    }

    private static double sq(double v) {
        return v * v;
    }

}
