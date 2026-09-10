package com.example.horsegenetics.common.stable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>Which floor a stall sign is naming.</b> The graph search behind
 * {@code server/StallDetector}, with the game taken out of it.
 *
 * <h2>Why it is here and not next to the world it reads</h2>
 * The first version of this lived entirely in the NeoForge module, where there
 * is no test source set it could be exercised in, and it shipped with a bug
 * that <b>teleported a horse into a wall and killed it</b>. That is the same
 * lesson the cooldown sentinel taught one release earlier (known gap 151): a
 * piece of pure logic parked on the Minecraft side is a piece of logic nobody
 * can write a five-line test for. So the search is here, over an interface the
 * game implements, and {@link StallFillTest} builds stalls out of strings.
 *
 * <h2>It fills columns, not air</h2>
 * The broken version flood-filled <b>air</b> in three dimensions, up to three
 * blocks above the sign. Every real stall is fronted by a <b>fence</b>, and a
 * fence is one block tall - so the fill went up and over the top of it every
 * single time, spilled into the aisle, ran past its cell budget and reported
 * "this is not a room". A stable made of fences could therefore never produce a
 * stall, which is the shape of stall people actually build.
 *
 * <p>This one asks a different question, and it is the question a horse would
 * ask: <i>which floor tiles can I walk between?</i> The search is 2D over
 * columns. It never rises, so it cannot climb a fence; it steps at most
 * {@link #STEP} up or down between neighbours, so a slab, a carpet or an
 * uneven floor is still one room.
 *
 * <h2>A doorway is an edge, open or shut</h2>
 * The game's {@link Columns#floorY} is expected to answer {@link #NONE} for a
 * door or a gate <b>whatever state it is in</b>. Judging by whether the horse
 * could physically pass leaks the moment somebody leaves the gate open, and the
 * player's answer to "where does the stall end" is the gate either way. That is
 * a rule about intent, so it belongs to the caller that knows block identity -
 * this class only knows a column is or is not standable.
 */
public final class StallFill {

    /** {@link Columns#floorY} for a column no horse can stand in: a wall, a fence, a doorway, a drop. */
    public static final int NONE = Integer.MIN_VALUE;

    /** How far the floor may rise or fall between neighbouring columns and still be one room. */
    public static final int STEP = 1;

    private StallFill() {
    }

    /**
     * The world, reduced to the one question the search asks.
     *
     * <p>Implemented by the NeoForge side against a real level, and by the test
     * against a picture. Coordinates are absolute block coordinates.
     */
    public interface Columns {

        /**
         * The block Y a horse standing in column {@code (x, z)} would stand on
         * top of, or {@link #NONE} if it could not stand there at all.
         *
         * @param nearY the Y of the column the search arrived from, so an
         *              implementation can look for the floor near the neighbour
         *              it is continuing from rather than scanning the world
         */
        int floorY(int x, int z, int nearY);
    }

    /**
     * One column of the stall floor: where it is, and the Y a horse stands at.
     */
    public record Column(int x, int z, int y) {
    }

    /**
     * A stall that closed. {@link #columns} are the floor tiles themselves -
     * <b>not</b> a bounding box, which is the other half of what went wrong: a
     * box around an L-shaped room contains the wall between the arms, and the
     * old landing code happily dropped a horse in it. Anything choosing where to
     * put a horse should walk these.
     */
    public record Region(List<Column> columns, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public int size() {
            return columns.size();
        }

        /** The column nearest the middle of the region, which is where a horse wants to arrive. */
        public Column middle() {
            double cx = (minX + maxX) / 2.0;
            double cz = (minZ + maxZ) / 2.0;
            Column best = columns.get(0);
            double bestD = Double.MAX_VALUE;
            for (Column c : columns) {
                double dx = c.x() - cx;
                double dz = c.z() - cz;
                double d = dx * dx + dz * dz;
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }
            return best;
        }
    }

    /**
     * Walk the floor outwards from {@code (seedX, seedZ)}.
     *
     * @param maxColumns the budget; past it the search decides this is not a
     *                   room at all and gives up, because an open pen and a
     *                   whole stable aisle look identical from inside
     * @return the region, or {@code null} if the seed is not standable or the
     *         floor ran away without closing
     */
    public static Region fill(Columns columns, int seedX, int seedZ, int seedY, int maxColumns) {
        int startY = columns.floorY(seedX, seedZ, seedY);
        if (startY == NONE) {
            return null;
        }

        Map<Long, Integer> seen = new HashMap<>();
        List<Column> found = new ArrayList<>();
        Deque<Column> frontier = new ArrayDeque<>();

        Column seed = new Column(seedX, seedZ, startY);
        seen.put(key(seedX, seedZ), startY);
        found.add(seed);
        frontier.add(seed);

        int minX = seedX;
        int minZ = seedZ;
        int minY = startY;
        int maxX = seedX;
        int maxZ = seedZ;
        int maxY = startY;

        while (!frontier.isEmpty()) {
            Column c = frontier.poll();
            for (int i = 0; i < 4; i++) {
                int nx = c.x() + (i == 0 ? 1 : i == 1 ? -1 : 0);
                int nz = c.z() + (i == 2 ? 1 : i == 3 ? -1 : 0);
                if (seen.containsKey(key(nx, nz))) {
                    continue;
                }
                int ny = columns.floorY(nx, nz, c.y());
                if (ny == NONE || Math.abs(ny - c.y()) > STEP) {
                    continue;
                }
                if (found.size() >= maxColumns) {
                    return null; // ran away - this is the aisle, not a stall
                }
                Column n = new Column(nx, nz, ny);
                seen.put(key(nx, nz), ny);
                found.add(n);
                frontier.add(n);
                minX = Math.min(minX, nx);
                minZ = Math.min(minZ, nz);
                minY = Math.min(minY, ny);
                maxX = Math.max(maxX, nx);
                maxZ = Math.max(maxZ, nz);
                maxY = Math.max(maxY, ny);
            }
        }

        return new Region(found, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** The two coordinates that identify a column, packed so they can key a map. */
    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** Every column in {@code region}, as a set that can be asked "is this cell mine". */
    public static Set<Long> keysOf(Region region) {
        Set<Long> out = new HashSet<>();
        for (Column c : region.columns()) {
            out.add(key(c.x(), c.z()));
        }
        return out;
    }

    /** Whether {@code (x, z)} is one of {@code region}'s floor tiles. */
    public static boolean contains(Region region, int x, int z) {
        for (Column c : region.columns()) {
            if (c.x() == x && c.z() == z) {
                return true;
            }
        }
        return false;
    }
}
