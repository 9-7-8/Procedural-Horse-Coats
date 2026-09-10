package com.example.horsegenetics.common.coat.pattern;

/**
 * Deterministic procedural noise sampled in <b>body space</b> - the same
 * {@code (x, y, z)} model-unit coordinates {@code HorseSkinGeometry} hands
 * every texel.
 *
 * <p>Sampling in 3D rather than in texture space is the whole point: two texels
 * that sit next to each other on the horse get neighbouring samples even when
 * they live on opposite ends of the sheet (the body's side face and its top
 * face, say), so a pattern built from these functions crosses part seams
 * without a visible join.
 *
 * <p>Everything here is a pure function of {@code (seed, x, y, z)} - no state,
 * no {@code Random} - so a coat rebuilt next session comes out identical.
 */
public final class BodyNoise {

    private BodyNoise() {}

    /**
     * Distance from {@code (x, y, z)} to the nearest point of a <b>jittered
     * lattice</b> with unit spacing, normalized to roughly {@code [0, 1]}.
     *
     * <p>Near 0 at a lattice point, near 1 in the gaps between them - i.e. a
     * field of round cells with a web running between them, which is exactly
     * the shape of dapples on a grey horse. Callers scale their coordinates to
     * choose the cell size.
     */
    public static double cellDistance(long seed, double x, double y, double z) {
        int cx = floor(x);
        int cy = floor(y);
        int cz = floor(z);
        double best = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int lx = cx + dx;
                    int ly = cy + dy;
                    int lz = cz + dz;
                    double px = lx + hash01(seed, lx, ly, lz, 1);
                    double py = ly + hash01(seed, lx, ly, lz, 2);
                    double pz = lz + hash01(seed, lx, ly, lz, 3);
                    double d = (px - x) * (px - x) + (py - y) * (py - y) + (pz - z) * (pz - z);
                    if (d < best) {
                        best = d;
                    }
                }
            }
        }
        // A jittered unit lattice tops out around 0.9 units from every centre.
        double d = Math.sqrt(best) / 0.9;
        return d < 0 ? 0 : (d > 1 ? 1 : d);
    }

    /**
     * Distance from {@code (x, y, z)} to the nearest <b>wall</b> between two
     * cells of the jittered lattice, in lattice units - near 0 on a cell
     * boundary and largest at a cell's middle.
     *
     * <p>{@link #cellDistance} measures to the nearest <i>centre</i>, which
     * draws round blobs however hard it is pushed. A giraffe, a cracked glaze
     * and a dry lake bed are the other shape entirely: <b>polygons that tile</b>,
     * each one filled solid, separated by a narrow channel of even width. That
     * is a function of the boundary rather than of the centre, so it needs the
     * two nearest points and not just the one: half the difference of their
     * distances is, to a good approximation, the distance to the plane that
     * bisects them.
     *
     * <p>Approximate rather than exact - the true Voronoi edge needs the
     * bisector's normal as well - and the error shows only within a texel of a
     * three-cell corner, where the channel pinches slightly. Cheap, and the
     * pinch reads as a drawn junction rather than as a bug.
     */
    public static double cellEdge(long seed, double x, double y, double z) {
        int cx = floor(x);
        int cy = floor(y);
        int cz = floor(z);
        double best = Double.MAX_VALUE;
        double second = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int lx = cx + dx;
                    int ly = cy + dy;
                    int lz = cz + dz;
                    double px = lx + hash01(seed, lx, ly, lz, 1);
                    double py = ly + hash01(seed, lx, ly, lz, 2);
                    double pz = lz + hash01(seed, lx, ly, lz, 3);
                    double d = (px - x) * (px - x) + (py - y) * (py - y) + (pz - z) * (pz - z);
                    if (d < best) {
                        second = best;
                        best = d;
                    } else if (d < second) {
                        second = d;
                    }
                }
            }
        }
        return (Math.sqrt(second) - Math.sqrt(best)) / 2.0;
    }

    /**
     * Distance from {@code (x, y, z)} to the nearest <b>corner</b> of the
     * jittered lattice - the point where three cells meet - in lattice units.
     *
     * <p>{@link #cellEdge} measures to the nearest wall, which draws a channel
     * of even width because that is what an even distance to a plane is. A vein
     * network is not that: it pools where cracks meet and thins to a hairline
     * between the junctions, and no threshold on a wall distance recovers it,
     * because the wall distance does not know a junction is nearby.
     *
     * <p>A corner is where the <b>third</b>-nearest centre is as close as the
     * first two, so the same walk that finds the wall finds this by keeping one
     * more candidate: half the gap between the first and third distances is
     * near zero at a three-way corner and grows along a wall away from one.
     * Approximate for the reason {@code cellEdge} is approximate, and in the
     * same places.
     */
    public static double cellVertex(long seed, double x, double y, double z) {
        int cx = floor(x);
        int cy = floor(y);
        int cz = floor(z);
        double best = Double.MAX_VALUE;
        double second = Double.MAX_VALUE;
        double third = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int lx = cx + dx;
                    int ly = cy + dy;
                    int lz = cz + dz;
                    double px = lx + hash01(seed, lx, ly, lz, 1);
                    double py = ly + hash01(seed, lx, ly, lz, 2);
                    double pz = lz + hash01(seed, lx, ly, lz, 3);
                    double d = (px - x) * (px - x) + (py - y) * (py - y) + (pz - z) * (pz - z);
                    if (d < best) {
                        third = second;
                        second = best;
                        best = d;
                    } else if (d < second) {
                        third = second;
                        second = d;
                    } else if (d < third) {
                        third = d;
                    }
                }
            }
        }
        return (Math.sqrt(third) - Math.sqrt(best)) / 2.0;
    }

    /**
     * The nearest jittered lattice point to {@code (x, y, z)}, and two numbers
     * drawn off <b>that point</b> rather than off the sample position.
     *
     * <p>{@link #cellDistance} answers "how far to the nearest centre", which is
     * all a dapple field needs. A field of discrete <i>elements</i> - a spot, a
     * ring, a disk in a chain - needs more: whether this particular element
     * exists at all, how big it is, and which colour of a palette it took. All
     * three are per-element decisions, so they have to be drawn off the element,
     * and every texel inside one element must draw the same numbers. Hence a
     * record: one lattice walk answers all of it.
     *
     * <p>{@code distance} is in <b>lattice units</b> (unnormalised, unlike
     * {@code cellDistance}), so a caller that scaled its coordinates by
     * {@code spacing} reads a radius in the same units it chose.
     */
    public record Cell(double distance, double dx, double dy, double dz,
                       double pick, double size, double angle) {}

    /** {@link Cell} for the jittered unit lattice - scale the coordinates to choose the spacing. */
    public static Cell cell(long seed, double x, double y, double z) {
        int cx = floor(x);
        int cy = floor(y);
        int cz = floor(z);
        double best = Double.MAX_VALUE;
        int bx = cx;
        int by = cy;
        int bz = cz;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int lx = cx + dx;
                    int ly = cy + dy;
                    int lz = cz + dz;
                    double px = lx + hash01(seed, lx, ly, lz, 1);
                    double py = ly + hash01(seed, lx, ly, lz, 2);
                    double pz = lz + hash01(seed, lx, ly, lz, 3);
                    double d = (px - x) * (px - x) + (py - y) * (py - y) + (pz - z) * (pz - z);
                    if (d < best) {
                        best = d;
                        bx = lx;
                        by = ly;
                        bz = lz;
                    }
                }
            }
        }
        double px = bx + hash01(seed, bx, by, bz, 1);
        double py = by + hash01(seed, bx, by, bz, 2);
        double pz = bz + hash01(seed, bx, by, bz, 3);
        return new Cell(Math.sqrt(best), x - px, y - py, z - pz,
                hash01(seed, bx, by, bz, 11),
                hash01(seed, bx, by, bz, 12),
                hash01(seed, bx, by, bz, 13));
    }

    /**
     * A <b>ridge</b> of the value field: 1 along the surfaces where the noise
     * crosses its midpoint, falling to 0 either side.
     *
     * <p>This is the primitive behind every "tapering stroke" gene. Value noise
     * sampled in a coordinate frame stretched along one axis produces
     * contours that run along that axis; taking the ridge of it turns those
     * contours into lines that curve, fork, pinch out and taper on their own -
     * the shapes a hand-drawn marking has and a sine wave never does.
     */
    public static double ridge(long seed, double x, double y, double z) {
        double n = value(seed, x, y, z);
        return 1.0 - Math.abs(2.0 * n - 1.0);
    }

    /** Smooth value noise in {@code [0, 1]} on a unit lattice - used to warp other fields. */
    public static double value(long seed, double x, double y, double z) {
        int x0 = floor(x);
        int y0 = floor(y);
        int z0 = floor(z);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double fz = smooth(z - z0);
        double c00 = lerp(hash01(seed, x0, y0, z0, 0), hash01(seed, x0 + 1, y0, z0, 0), fx);
        double c10 = lerp(hash01(seed, x0, y0 + 1, z0, 0), hash01(seed, x0 + 1, y0 + 1, z0, 0), fx);
        double c01 = lerp(hash01(seed, x0, y0, z0 + 1, 0), hash01(seed, x0 + 1, y0, z0 + 1, 0), fx);
        double c11 = lerp(hash01(seed, x0, y0 + 1, z0 + 1, 0), hash01(seed, x0 + 1, y0 + 1, z0 + 1, 0), fx);
        return lerp(lerp(c00, c10, fy), lerp(c01, c11, fy), fz);
    }

    private static double hash01(long seed, int x, int y, int z, int salt) {
        long h = seed;
        h = (h ^ (x * 0x9E3779B97F4A7C15L)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (y * 0xC2B2AE3D27D4EB4FL)) * 0x94D049BB133111EBL;
        h = (h ^ (z * 0x165667B19E3779F9L)) * 0xD6E8FEB86659FD93L;
        h = (h ^ (salt * 0x27D4EB2F165667C5L)) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        return (h >>> 11) / (double) (1L << 53);
    }

    private static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
