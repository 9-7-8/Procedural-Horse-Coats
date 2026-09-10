package com.example.horsegenetics.common.stable;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stalls drawn as pictures.
 *
 * <p>Every case here is one that was reported from a real world or is the
 * direct cause of one that was. The one that matters most is
 * {@link #anLShapedRoomDoesNotClaimTheWallInsideItsBoundingBox()} - the old
 * detector kept only a bounding box, the landing code picked a spot inside it,
 * and a horse was teleported into a wall and suffocated.
 *
 * <h2>The map</h2>
 * <pre>
 *   #  wall, fence, or a shut door - nowhere a horse can stand
 *   g  a gate or doorway, standing open: still an edge, on purpose
 *   .  floor, at the base height
 *   +  floor one block up - a slab or a step
 *   ^  floor two blocks up - too far to be the same room
 *   (space) open void with no floor
 * </pre>
 */
class StallFillTest {

    private static final int BASE = 64;
    private static final int BUDGET = 2048;

    /**
     * A picture, indexed by row (z) and column (x), origin at (0, 0).
     *
     * <p>Note what this deliberately does <b>not</b> model: height above the
     * floor. The whole point of the rewrite is that the search never looks up,
     * so an open-topped stall and a roofed one are the same problem.
     */
    private record Picture(String[] rows) implements StallFill.Columns {

        @Override
        public int floorY(int x, int z, int nearY) {
            if (z < 0 || z >= rows.length) {
                return StallFill.NONE;
            }
            String row = rows[z];
            if (x < 0 || x >= row.length()) {
                return StallFill.NONE;
            }
            return switch (row.charAt(x)) {
                case '.' -> BASE;
                case '+' -> BASE + 1;
                case '^' -> BASE + 2;
                default -> StallFill.NONE;
            };
        }
    }

    private static StallFill.Region fill(String[] rows, int x, int z) {
        return StallFill.fill(new Picture(rows), x, z, BASE, BUDGET);
    }

    @Test
    void asealedRoomClosesAndCountsItsFloor() {
        StallFill.Region r = fill(new String[] {
                "#####",
                "#...#",
                "#...#",
                "#####",
        }, 2, 1);

        assertNotNull(r, "a sealed room must close");
        assertEquals(6, r.size(), "3x2 of floor");
        assertEquals(1, r.minX());
        assertEquals(3, r.maxX());
        assertEquals(1, r.minZ());
        assertEquals(2, r.maxZ());
    }

    /**
     * <b>The reported bug.</b> A stall fronted by a fence with the gate left
     * open. The old fill climbed over the fence - it was allowed three blocks
     * of rise and a fence is one block tall - and escaped into the aisle, so
     * the stall never closed and the horse got a fallback box straddling a
     * wall. Here the gate is an edge whatever state it is in, so the stall is
     * the stall.
     */
    @Test
    void anOpenGateIsStillTheEdgeOfTheStall() {
        String[] stable = {
                "#########",
                "#...#...#",   // two stalls
                "#...g...#",   // ... with an open gate in the party fence
                "##g######",   // ... and one onto the aisle
                "#.......#",   // the aisle, which the fill must not reach
                "#########",
        };

        StallFill.Region r = fill(stable, 2, 1);
        assertNotNull(r, "the stall must close even with its gate open");
        assertEquals(6, r.size(), "3x2 of stall floor, and none of the aisle");
        assertTrue(r.maxZ() <= 2, "the fill leaked through the gate into the aisle");
        assertFalse(StallFill.contains(r, 5, 1), "the fill leaked into the neighbouring stall");
    }

    /**
     * The horse-killer. An L keeps a solid corner inside its own bounding box;
     * anything picking a landing spot from the box rather than from the floor
     * can choose that corner.
     */
    @Test
    void anLShapedRoomDoesNotClaimTheWallInsideItsBoundingBox() {
        StallFill.Region r = fill(new String[] {
                "#####",
                "#...#",
                "#.###",
                "#.###",
                "#####",
        }, 1, 1);

        assertNotNull(r);
        assertEquals(5, r.size());
        // Inside the bounding box (x 1..3, z 1..3) but solid.
        assertFalse(StallFill.contains(r, 3, 3), "a wall inside the box must not be part of the stall");
        assertFalse(StallFill.contains(r, 2, 2), "a wall inside the box must not be part of the stall");
        assertTrue(StallFill.contains(r, 1, 3));
    }

    @Test
    void aSlabOrCarpetStepIsTheSameRoomButATwoBlockJumpIsNot() {
        StallFill.Region step = fill(new String[] {
                "#####",
                "#.+.#",
                "#####",
        }, 1, 1);
        assertNotNull(step);
        assertEquals(3, step.size(), "a slab in the middle of the floor does not divide the room");

        StallFill.Region jump = fill(new String[] {
                "#####",
                "#.^.#",
                "#####",
        }, 1, 1);
        assertNotNull(jump);
        assertEquals(1, jump.size(), "two blocks up is a different floor, and it walls off what is past it");
        assertFalse(StallFill.contains(jump, 2, 1), "two steps up is a different floor");
        assertFalse(StallFill.contains(jump, 3, 1), "and everything beyond it is unreachable");
    }

    @Test
    void anOpenFieldNeverCloses() {
        String[] field = new String[64];
        for (int i = 0; i < field.length; i++) {
            field[i] = ".".repeat(64);
        }
        assertNull(fill(field, 32, 32), "4096 columns of open ground is not a room");
    }

    @Test
    void aSeedInsideAWallIsNotAStall() {
        assertNull(fill(new String[] {
                "#####",
                "#...#",
                "#####",
        }, 0, 0), "a solid seed has no room to offer");
    }

    @Test
    void theMiddleOfARegionIsOneOfItsOwnFloorTiles() {
        StallFill.Region r = fill(new String[] {
                "#####",
                "#...#",
                "#.###",
                "#.###",
                "#####",
        }, 1, 1);

        assertNotNull(r);
        StallFill.Column m = r.middle();
        assertTrue(StallFill.contains(r, m.x(), m.z()),
                "middle() must return somewhere the horse can actually stand");
        assertEquals(BASE, m.y());
    }

    @Test
    void everyColumnCarriesTheHeightItsOwnFloorIsAt() {
        StallFill.Region r = fill(new String[] {
                "####",
                "#.+#",
                "####",
        }, 1, 1);

        assertNotNull(r);
        List<StallFill.Column> cs = r.columns();
        assertEquals(2, cs.size());
        for (StallFill.Column c : cs) {
            assertEquals(c.x() == 1 ? BASE : BASE + 1, c.y());
        }
        assertEquals(BASE, r.minY());
        assertEquals(BASE + 1, r.maxY());
    }
}
