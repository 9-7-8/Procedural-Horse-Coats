package com.example.horsegenetics.common.gear;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The worn mesh's geometry: what is lifted, where the walls are, and where they
 * are not.
 *
 * <p>The box here is unwrapped the way a baked model cube is - the same corner
 * order and the same six rectangles - because {@link RimMesh.Face} leans on
 * that order, and a test box laid out any other way would pin nothing.
 */
class RimMeshTest {

    private static final int SHEET = 64;
    private static final float LIFT = 0.25f;
    private static final float EPS = 1.0e-4f;

    // A 4 x 6 x 2 box at the origin, unwrapped from (0, 0).
    private static final float W = 4f;
    private static final float H = 6f;
    private static final float D = 2f;

    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int WEST = 2;
    private static final int NORTH = 3;
    private static final int EAST = 4;
    private static final int SOUTH = 5;

    /** Texel rectangles of the six faces, u0 v0 u1 v1, in the order above. */
    private static final int[][] RECT = {
            {2, 0, 6, 2}, {6, 0, 10, 2}, {0, 2, 2, 8}, {2, 2, 6, 8}, {6, 2, 8, 8}, {8, 2, 12, 8}};

    private static RimMesh.Face face(float[][] v, float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
        float[] corners = new float[12];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(v[i], 0, corners, i * 3, 3);
        }
        float[] uv = {u1 / SHEET, v0 / SHEET, u0 / SHEET, v0 / SHEET, u0 / SHEET, v1 / SHEET, u1 / SHEET, v1 / SHEET};
        return new RimMesh.Face(corners, uv, nx, ny, nz);
    }

    /** A cube's six faces, built as the game bakes them. */
    private static List<RimMesh.Face> box() {
        float[] t0 = {0, 0, 0};
        float[] t1 = {W, 0, 0};
        float[] t2 = {W, H, 0};
        float[] t3 = {0, H, 0};
        float[] l0 = {0, 0, D};
        float[] l1 = {W, 0, D};
        float[] l2 = {W, H, D};
        float[] l3 = {0, H, D};
        float u0 = 0;
        float u1 = D;
        float u2 = D + W;
        float u22 = D + W + W;
        float u3 = D + W + D;
        float u4 = D + W + D + W;
        float v0 = 0;
        float v1 = D;
        float v2 = D + H;
        List<RimMesh.Face> faces = new ArrayList<>();
        faces.add(face(new float[][] {l1, l0, t0, t1}, u1, v0, u2, v1, 0, -1, 0));
        faces.add(face(new float[][] {t2, t3, l3, l2}, u2, v1, u22, v0, 0, 1, 0));
        faces.add(face(new float[][] {t0, l0, l3, t3}, u0, v1, u1, v2, -1, 0, 0));
        faces.add(face(new float[][] {t1, t0, t3, t2}, u1, v1, u2, v2, 0, 0, -1));
        faces.add(face(new float[][] {l1, t1, t2, l2}, u2, v1, u3, v2, 1, 0, 0));
        faces.add(face(new float[][] {l0, l1, l2, l3}, u3, v1, u4, v2, 0, 0, 1));
        return faces;
    }

    private static RimMesh.Texels sheet(int... faces) {
        boolean[][] painted = new boolean[SHEET][SHEET];
        for (int f : faces) {
            for (int v = RECT[f][1]; v < RECT[f][3]; v++) {
                for (int u = RECT[f][0]; u < RECT[f][2]; u++) {
                    painted[v][u] = true;
                }
            }
        }
        return texels(painted);
    }

    private static RimMesh.Texels texels(boolean[][] painted) {
        return new RimMesh.Texels() {
            @Override
            public int width() {
                return SHEET;
            }

            @Override
            public int height() {
                return SHEET;
            }

            @Override
            public boolean painted(int u, int v) {
                return u >= 0 && v >= 0 && u < SHEET && v < SHEET && painted[v][u];
            }
        };
    }

    private static float[] vertex(float[] quads, int quad, int vertex) {
        int at = quad * RimMesh.FLOATS_PER_QUAD + vertex * RimMesh.FLOATS_PER_VERTEX;
        return Arrays.copyOfRange(quads, at, at + RimMesh.FLOATS_PER_VERTEX);
    }

    /** The quad's geometric normal (by its winding) dotted with the normal it carries. */
    private static float facing(float[] quads, int quad) {
        float[] a = vertex(quads, quad, 0);
        float[] b = vertex(quads, quad, 1);
        float[] c = vertex(quads, quad, 2);
        float[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        float[] ac = {c[0] - a[0], c[1] - a[1], c[2] - a[2]};
        float cx = ab[1] * ac[2] - ab[2] * ac[1];
        float cy = ab[2] * ac[0] - ab[0] * ac[2];
        float cz = ab[0] * ac[1] - ab[1] * ac[0];
        return cx * a[5] + cy * a[6] + cz * a[7];
    }

    @Test
    @DisplayName("a piece that covers the whole box is six lifted faces and no wall")
    void aWholeBoxHasNoWalls() {
        RimMesh.Built built = RimMesh.box(box(), sheet(DOWN, UP, WEST, NORTH, EAST, SOUTH), LIFT);
        assertEquals(6, built.faces());
        assertEquals(0, built.rims());
        assertEquals(6 * RimMesh.FLOATS_PER_QUAD, built.quads().length);
    }

    @Test
    @DisplayName("a piece on one face is that face lifted and grown, with four walls down to the skin")
    void oneFaceGetsFourWalls() {
        RimMesh.Built built = RimMesh.box(box(), sheet(NORTH), LIFT);
        assertEquals(1, built.faces());
        assertEquals(4, built.rims());

        // The lifted face: the north face is z = 0 facing -z, so it moves to -LIFT
        // and overhangs the box by LIFT all round.
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float[] v = vertex(built.quads(), 0, i);
            assertEquals(-LIFT, v[2], EPS);
            assertArrayEquals(new float[] {0, 0, -1}, new float[] {v[5], v[6], v[7]}, EPS);
            minX = Math.min(minX, v[0]);
            maxX = Math.max(maxX, v[0]);
            minY = Math.min(minY, v[1]);
            maxY = Math.max(maxY, v[1]);
        }
        assertEquals(-LIFT, minX, EPS);
        assertEquals(W + LIFT, maxX, EPS);
        assertEquals(-LIFT, minY, EPS);
        assertEquals(H + LIFT, maxY, EPS);

        // Every wall runs from the lifted face down to the skin and no further.
        for (int quad = 1; quad < 5; quad++) {
            int top = 0;
            int bottom = 0;
            for (int i = 0; i < 4; i++) {
                float z = vertex(built.quads(), quad, i)[2];
                if (Math.abs(z + LIFT) < EPS) {
                    top++;
                } else if (Math.abs(z) < EPS) {
                    bottom++;
                }
            }
            assertEquals(2, top, "wall " + quad + " has two corners on the lifted face");
            assertEquals(2, bottom, "wall " + quad + " has two corners on the skin");
        }
    }

    @Test
    @DisplayName("every quad winds the way the baked faces do, and a wall faces away from the piece")
    void wallsFaceOutward() {
        RimMesh.Built built = RimMesh.box(box(), sheet(NORTH), LIFT);
        float reference = Math.signum(facing(built.quads(), 0));
        for (int quad = 1; quad < built.count(); quad++) {
            assertEquals(reference, Math.signum(facing(built.quads(), quad)), "quad " + quad);
            float[] v = vertex(built.quads(), quad, 0);
            // A wall of the north face is perpendicular to it, and its normal is a unit one.
            assertEquals(0f, v[7], EPS);
            assertEquals(1f, v[5] * v[5] + v[6] * v[6], EPS);
            // Outward: the normal points away from the face's middle.
            float cx = 0;
            float cy = 0;
            for (int i = 0; i < 4; i++) {
                cx += vertex(built.quads(), quad, i)[0] / 4f;
                cy += vertex(built.quads(), quad, i)[1] / 4f;
            }
            assertTrue((cx - W / 2f) * v[5] + (cy - H / 2f) * v[6] > 0f, "wall " + quad + " faces outward");
        }
    }

    @Test
    @DisplayName("a wall is textured from the painted texels it borders")
    void wallsTakeTheirBorderTexelsColour() {
        RimMesh.Texels tex = sheet(NORTH);
        RimMesh.Built built = RimMesh.box(box(), tex, LIFT);
        for (int quad = 1; quad < built.count(); quad++) {
            float u = 0;
            float v = 0;
            for (int i = 0; i < 4; i++) {
                u += vertex(built.quads(), quad, i)[3] / 4f;
                v += vertex(built.quads(), quad, i)[4] / 4f;
            }
            assertTrue(tex.painted((int) Math.floor(u * SHEET), (int) Math.floor(v * SHEET)),
                    "wall " + quad + " samples a painted texel");
        }
    }

    @Test
    @DisplayName("a piece that wraps a corner of the box has no wall inside the corner")
    void aWrappedCornerHasNoInnerWall() {
        // North and east share the box's x = W, z = 0 edge.
        RimMesh.Built built = RimMesh.box(box(), sheet(NORTH, EAST), LIFT);
        assertEquals(2, built.faces());
        assertEquals(6, built.rims());

        // Opposite faces share no edge, so both keep all four.
        assertEquals(8, RimMesh.box(box(), sheet(NORTH, SOUTH), LIFT).rims());

        // Top and north share the y = H edge, across a rectangle that is not
        // beside the north face's on the sheet: neighbours are found on the box.
        assertEquals(6, RimMesh.box(box(), sheet(UP, NORTH), LIFT).rims());
    }

    @Test
    @DisplayName("a band round the box is four faces, with a wall along its top and its bottom on each")
    void aBandRoundTheBox() {
        boolean[][] painted = new boolean[SHEET][SHEET];
        for (int u = 0; u < 12; u++) {
            painted[4][u] = true;
            painted[5][u] = true;
        }
        RimMesh.Built built = RimMesh.box(box(), texels(painted), LIFT);
        assertEquals(4, built.faces());
        assertEquals(8, built.rims());
    }

    @Test
    @DisplayName("a box the piece does not touch yields nothing")
    void anUntouchedBoxIsEmpty() {
        RimMesh.Built built = RimMesh.box(box(), sheet(), LIFT);
        assertEquals(0, built.count());
        assertEquals(0, built.quads().length);
    }

    @Test
    @DisplayName("a lift under the floor is refused, and the same input gives the same mesh")
    void theFloorHoldsAndBuildingIsDeterministic() {
        assertThrows(IllegalArgumentException.class, () -> RimMesh.box(box(), sheet(NORTH), 0.1f));
        assertArrayEquals(RimMesh.box(box(), sheet(NORTH, EAST), 0.5f).quads(),
                RimMesh.box(box(), sheet(NORTH, EAST), 0.5f).quads());
    }
}
