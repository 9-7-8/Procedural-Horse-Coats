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
