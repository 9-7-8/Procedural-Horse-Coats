package com.example.horsegenetics.common.coat.skin;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The bridge a coat-pattern algorithm draws through: it turns a point in
 * <b>horse body-space</b> into the exact texel on the coat sheet that shades
 * that point, and back again.
 *
 * <h2>Body-space</h2>
 * A right-handed grid measured in <b>model units</b> (1 unit = 1/16 block =
 * {@link #TEXELS_PER_UNIT} texels on the {@value #SHEET_SIZE}px sheet):
 * <ul>
 *   <li><b>X</b> - 0 at the rear edge of the tail, increasing toward the nose.</li>
 *   <li><b>Y</b> - 0 at the bottom of the hooves, increasing upward.</li>
 *   <li><b>Z</b> - 0 on the centre plane; <b>+Z toward the horse's right</b>,
 *       -Z toward its left.</li>
 * </ul>
 * The origins are read off the model itself: X=0 is the backmost texel of the
 * tail box in its rest pose, Y=0 is the underside of the hoof boxes. Because
 * every part is measured on the same absolute scale, a pattern that is a plain
 * function of X (say {@code shade = X / noseX}) renders as one continuous
 * front-to-back gradient across body, legs, neck and head with no seams.
 *
 * <h2>Parts and faces</h2>
 * Each {@link Part} is treated as an axis-aligned box (see the rest-pose note
 * on rotated parts below). A {@link Face} is named for the body-space direction
 * it looks along and is spanned by the two axes it does <i>not</i> look along:
 * <pre>
 *   NOSE / TAIL     look along X   -&gt; spanned by (Z, Y)   [face.spanA(), face.spanB()]
 *   TOP  / BOTTOM   look along Y   -&gt; spanned by (X, Z)
 *   RIGHT / LEFT    look along Z   -&gt; spanned by (X, Y)
 * </pre>
 * {@link #project(Part, Face, double, double)} takes exactly those two coords,
 * in {@code (spanA, spanB)} order, and returns a {@link Texel}. The overload
 * {@link #project(Part, Face, BodyPoint)} pulls the right two coords out of a
 * 3D point so callers can't transpose them.
 *
 * <h2>Non-mirrored</h2>
 * The UV layout matches {@code HdHorseModel} (128px, every leg and ear on its
 * own patch, no {@code mirror()}). So the RIGHT face of the left-front leg and
 * the LEFT face of the right-front leg are genuinely different texels - a spot
 * painted on one flank does not ghost onto the other.
 *
 * <h2>Rest-pose approximation</h2>
 * The head, neck, muzzle, mane, ears and tail are modelled at a pitch in the
 * real geometry. Their body-space {@link Bounds} here are the axis-aligned
 * bounding box of that rotated box in the rest pose, and the six-face mapping
 * is applied to that AABB. So the X-range of the head is exact (the gradient
 * stays seamless), but within a tilted part the face projection is an
 * approximation - fine for gradients and blotches, not for pixel-tight
 * alignment with the 3D silhouette. Animation poses are ignored entirely.
 *
 * <p>Pure data + arithmetic - no Minecraft, no genetics. Keep the geometry
 * table below in sync with {@code neoforge-26.1.2/.../client/HdHorseModel}
 * (a future change could have that class consume this one).
 */
public final class HorseSkinGeometry {

    /** Texels per body-space unit. The sheet is the vanilla 64px layout at 2x. */
    public static final int TEXELS_PER_UNIT = 2;
    /** Width and height of the coat sheet in texels. */
    public static final int SHEET_SIZE = 128;

    public enum Axis { X, Y, Z }

    /** One of a box's six sides, named for the body-space direction it faces. */
    public enum Face {
        NOSE(Axis.X, true), TAIL(Axis.X, false),
        TOP(Axis.Y, true), BOTTOM(Axis.Y, false),
        RIGHT(Axis.Z, true), LEFT(Axis.Z, false);

        private final Axis normal;
        private final boolean atMax;

        Face(Axis normal, boolean atMax) {
            this.normal = normal;
            this.atMax = atMax;
        }

        /** Axis this face looks along; constant across the face. */
        public Axis normal() {
            return normal;
        }

        /** First spanning axis - the first arg to {@link #project(Part, Face, double, double)}. */
        public Axis spanA() {
            return normal == Axis.X ? Axis.Z : Axis.X;
        }

        /** Second spanning axis - the second arg to {@link #project(Part, Face, double, double)}. */
        public Axis spanB() {
            return normal == Axis.Y ? Axis.Z : Axis.Y;
        }
    }

    /** The twelve boxes that make up the horse. */
    public enum Part {
        BODY, NECK, HEAD, MUZZLE, MANE, TAIL,
        LEFT_EAR, RIGHT_EAR,
        LEFT_FRONT_LEG, RIGHT_FRONT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG;

        /** The same part on the other side, or {@code this} for the centreline parts. */
        public Part mirror() {
            return switch (this) {
                case LEFT_EAR -> RIGHT_EAR;
                case RIGHT_EAR -> LEFT_EAR;
                case LEFT_FRONT_LEG -> RIGHT_FRONT_LEG;
                case RIGHT_FRONT_LEG -> LEFT_FRONT_LEG;
                case LEFT_HIND_LEG -> RIGHT_HIND_LEG;
                case RIGHT_HIND_LEG -> LEFT_HIND_LEG;
                default -> this;
            };
        }
    }

    /** Axis-aligned body-space extent of a part, in model units. */
    public record Bounds(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        public double min(Axis a) {
            return switch (a) { case X -> xMin; case Y -> yMin; case Z -> zMin; };
        }

        public double max(Axis a) {
            return switch (a) { case X -> xMax; case Y -> yMax; case Z -> zMax; };
        }

        public double span(Axis a) {
            return max(a) - min(a);
        }
    }

    /** A point in body-space. */
    public record BodyPoint(double x, double y, double z) {
        public double along(Axis a) {
            return switch (a) { case X -> x; case Y -> y; case Z -> z; };
        }
    }

    /**
     * A location on the sheet. {@code u}/{@code v} are the precise fractional
     * texel; {@code x}/{@code y} are the floored integer texel, clamped to the
     * sheet. {@code clamped} is true when the query fell outside the part's
     * bounds on a spanning axis (the result is then the nearest edge texel).
     */
    public record Texel(double u, double v, int x, int y, boolean clamped) {}

    /** The inverse of {@link #project}: which part/face a texel belongs to, and where. */
    public record Sample(Part part, Face face, BodyPoint point) {}

    // ------------------------------------------------------------------
    // Geometry table - raw values lifted from HdHorseModel / vanilla
    // AbstractEquineModel.createBodyMesh. pivot = the part's world position
    // after resolving its parent chain; (ox,oy,oz)+(w,h,d) = its addBox; pitch
    // = static xRot in radians; (tu,tv) = 64-space texOffs (HD sheet = x2).
    // ------------------------------------------------------------------

    private static final double HEAD_PITCH = Math.PI / 6.0;

    private record Raw(
        Part part,
        double px, double py, double pz,
        double ox, double oy, double oz,
        double w, double h, double d,
        double pitch,
        int tu, int tv
    ) {}

    private static final List<Raw> RAW = List.of(
        new Raw(Part.BODY, 0, 11, 5, -5, -8, -17, 10, 10, 22, 0, 0, 32),
        new Raw(Part.NECK, 0, 4, -12, -2.05, -6, -2, 4, 12, 7, HEAD_PITCH, 0, 35),
        new Raw(Part.HEAD, 0, 4, -12, -3, -11, -2, 6, 5, 7, HEAD_PITCH, 0, 13),
        new Raw(Part.MUZZLE, 0, 4, -12, -2, -11, -7, 4, 5, 5, HEAD_PITCH, 0, 25),
        new Raw(Part.MANE, 0, 4, -12, -1, -11, 5.01, 2, 16, 2, HEAD_PITCH, 56, 36),
        new Raw(Part.TAIL, 0, 6, 7, -1.5, 0, 0, 3, 14, 4, HEAD_PITCH, 42, 36),
        new Raw(Part.LEFT_EAR, 0, 4, -12, 0.55, -13, 4, 2, 3, 1, HEAD_PITCH, 19, 0),
        new Raw(Part.RIGHT_EAR, 0, 4, -12, -2.55, -13, 4, 2, 3, 1, HEAD_PITCH, 19, 16),
        new Raw(Part.LEFT_HIND_LEG, 4, 14, 7, -3, -1.01, -1, 4, 11, 4, 0, 26, 0),
        new Raw(Part.RIGHT_HIND_LEG, -4, 14, 7, -1, -1.01, -1, 4, 11, 4, 0, 48, 21),
        new Raw(Part.LEFT_FRONT_LEG, 4, 14, -10, -3, -1.01, -1.9, 4, 11, 4, 0, 26, 16),
        new Raw(Part.RIGHT_FRONT_LEG, -4, 14, -10, -1, -1.01, -1.9, 4, 11, 4, 0, 48, 0)
    );

    /** u varies with fraction along {@code usesA ? spanA : spanB}; likewise v. */
    private record FaceMap(double u0, double u1, boolean uUsesA, double v0, double v1, boolean vUsesA) {}

    private record PartData(Bounds bounds, Map<Face, FaceMap> faces) {}

    private static final Map<Part, PartData> PARTS = new EnumMap<>(Part.class);

    static {
        // Pass 1: model-space AABB of every (rotated) box, and the global
        // maxima that pin X=0 (tail edge) and Y=0 (hoof bottom).
        Map<Part, double[]> modelAabb = new EnumMap<>(Part.class);
        double mzMaxAll = Double.NEGATIVE_INFINITY;
        double myMaxAll = Double.NEGATIVE_INFINITY;
        for (Raw r : RAW) {
            double[] aabb = modelAabbOf(r);
            modelAabb.put(r.part, aabb);
            myMaxAll = Math.max(myMaxAll, aabb[3]);
            mzMaxAll = Math.max(mzMaxAll, aabb[5]);
        }

        // Pass 2: model AABB -> body Bounds, plus the six face pixel maps.
        for (Raw r : RAW) {
            double[] m = modelAabb.get(r.part);
            // body X = mzMaxAll - modelZ ; body Y = myMaxAll - modelY ; body Z = -modelX
            Bounds b = new Bounds(
                mzMaxAll - m[5], mzMaxAll - m[4],
                myMaxAll - m[3], myMaxAll - m[2],
                -m[1], -m[0]
            );
            PARTS.put(r.part, new PartData(b, faceMapsOf(r)));
        }
    }

    private HorseSkinGeometry() {}

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Body-space bounds of a part. */
    public static Bounds bounds(Part part) {
        return PARTS.get(part).bounds();
    }

    /** Every part's body-space bounds, keyed by part. */
    public static java.util.Map<Part, Bounds> allBounds() {
        java.util.EnumMap<Part, Bounds> out = new java.util.EnumMap<>(Part.class);
        for (var e : PARTS.entrySet()) {
            out.put(e.getKey(), e.getValue().bounds());
        }
        return out;
    }

    /** Visit every mapped texel of the sheet once (in row-major order). */
    public static void forEachTexel(TexelVisitor visitor) {
        for (int py = 0; py < SHEET_SIZE; py++) {
            for (int px = 0; px < SHEET_SIZE; px++) {
                Optional<Sample> s = sample(px, py);
                if (s.isPresent()) {
                    Sample sm = s.get();
                    visitor.visit(px, py, sm.part(), sm.face(), sm.point());
                }
            }
        }
    }

    /** Visit every mapped texel that belongs to {@code part}. */
    public static void forEachTexel(Part part, TexelVisitor visitor) {
        for (int py = 0; py < SHEET_SIZE; py++) {
            for (int px = 0; px < SHEET_SIZE; px++) {
                Optional<Sample> s = sample(px, py);
                if (s.isPresent() && s.get().part() == part) {
                    Sample sm = s.get();
                    visitor.visit(px, py, sm.part(), sm.face(), sm.point());
                }
            }
        }
    }

    @FunctionalInterface
    public interface TexelVisitor {
        void visit(int px, int py, Part part, Face face, BodyPoint point);
    }

    /** Bounds of the whole horse (union of every part). Handy for normalising a pattern. */
    public static Bounds bodyBounds() {
        double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        double zMin = Double.POSITIVE_INFINITY, zMax = Double.NEGATIVE_INFINITY;
        for (PartData pd : PARTS.values()) {
            Bounds b = pd.bounds();
            xMin = Math.min(xMin, b.xMin); xMax = Math.max(xMax, b.xMax);
            yMin = Math.min(yMin, b.yMin); yMax = Math.max(yMax, b.yMax);
            zMin = Math.min(zMin, b.zMin); zMax = Math.max(zMax, b.zMax);
        }
        return new Bounds(xMin, xMax, yMin, yMax, zMin, zMax);
    }

    /**
     * Project a body-space location on one face of one part to a texel.
     *
     * @param a coordinate along {@code face.spanA()}
     * @param b coordinate along {@code face.spanB()}
     */
    public static Texel project(Part part, Face face, double a, double b) {
        PartData pd = PARTS.get(part);
        FaceMap fm = pd.faces().get(face);
        Bounds bd = pd.bounds();

        double fa = invLerp(bd.min(face.spanA()), bd.max(face.spanA()), a);
        double fb = invLerp(bd.min(face.spanB()), bd.max(face.spanB()), b);
        boolean clamped = fa < 0 || fa > 1 || fb < 0 || fb > 1;
        fa = clamp01(fa);
        fb = clamp01(fb);

        double u = lerp(fm.u0(), fm.u1(), fm.uUsesA() ? fa : fb);
        double v = lerp(fm.v0(), fm.v1(), fm.vUsesA() ? fa : fb);
        // Snap to an integer texel that is actually inside this face's patch:
        // a fraction of exactly 1 lands u/v on the patch's far edge, which
        // floors to the neighbouring patch's first texel.
        return new Texel(u, v, texelInRect(u, fm.u0(), fm.u1()), texelInRect(v, fm.v0(), fm.v1()), clamped);
    }

    /** Project a full body-space point onto {@code face} (the off-axis coord is ignored). */
    public static Texel project(Part part, Face face, BodyPoint point) {
        return project(part, face, point.along(face.spanA()), point.along(face.spanB()));
    }

    // Memoised full-sheet sample grid - built on first use. sample() is called
    // ~128*128 times per texture bake, several bakes per horse; the per-call
    // 72-rect scan adds up, so cache it.
    private static volatile Sample[] SAMPLE_GRID;

    /**
     * Inverse of {@link #project}: the part, face and body-space point a texel
     * shades. Some sheet texels are shared by more than one box (the vanilla
     * layout packs the head/neck/body patches tight); this returns the first
     * match in {@link Part} then {@link Face} order. Empty if the texel is on
     * no mapped face.
     */
    public static Optional<Sample> sample(int px, int py) {
        if (px < 0 || py < 0 || px >= SHEET_SIZE || py >= SHEET_SIZE) {
            return Optional.empty();
        }
        Sample[] grid = SAMPLE_GRID;
        if (grid == null) {
            grid = buildSampleGrid();
            SAMPLE_GRID = grid;
        }
        return Optional.ofNullable(grid[py * SHEET_SIZE + px]);
    }

    private static Sample[] buildSampleGrid() {
        Sample[] grid = new Sample[SHEET_SIZE * SHEET_SIZE];
        for (int py = 0; py < SHEET_SIZE; py++) {
            for (int px = 0; px < SHEET_SIZE; px++) {
                grid[py * SHEET_SIZE + px] = sampleUncached(px, py).orElse(null);
            }
        }
        return grid;
    }

    private static Optional<Sample> sampleUncached(int px, int py) {
        double cx = px + 0.5;
        double cy = py + 0.5;
        for (Part part : Part.values()) {
            PartData pd = PARTS.get(part);
            for (Map.Entry<Face, FaceMap> e : pd.faces().entrySet()) {
                Face face = e.getKey();
                FaceMap fm = e.getValue();
                if (!within(fm.u0(), fm.u1(), cx) || !within(fm.v0(), fm.v1(), cy)) {
                    continue;
                }
                double fu = invLerp(fm.u0(), fm.u1(), cx);
                double fv = invLerp(fm.v0(), fm.v1(), cy);
                double fa = fm.uUsesA() ? fu : fv;
                double fb = fm.uUsesA() ? fv : fu;

                Bounds bd = pd.bounds();
                double a = lerp(bd.min(face.spanA()), bd.max(face.spanA()), fa);
                double b = lerp(bd.min(face.spanB()), bd.max(face.spanB()), fb);
                double plane = face.atMax ? bd.max(face.normal()) : bd.min(face.normal());
                return Optional.of(new Sample(part, face, pointOf(face, a, b, plane)));
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // Build helpers
    // ------------------------------------------------------------------

    /** [mxMin, mxMax, myMin, myMax, mzMin, mzMax] of a box after its rest-pose pitch. */
    private static double[] modelAabbOf(Raw r) {
        double[] out = {
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        double cos = Math.cos(r.pitch);
        double sin = Math.sin(r.pitch);
        for (int cx = 0; cx < 2; cx++) {
            for (int cy = 0; cy < 2; cy++) {
                for (int cz = 0; cz < 2; cz++) {
                    double lx = r.ox + (cx == 0 ? 0 : r.w);
                    double ly = r.oy + (cy == 0 ? 0 : r.h);
                    double lz = r.oz + (cz == 0 ? 0 : r.d);
                    // rotate about X, then translate by the pivot
                    double ry = ly * cos - lz * sin;
                    double rz = ly * sin + lz * cos;
                    double mx = r.px + lx;
                    double my = r.py + ry;
                    double mz = r.pz + rz;
                    out[0] = Math.min(out[0], mx); out[1] = Math.max(out[1], mx);
                    out[2] = Math.min(out[2], my); out[3] = Math.max(out[3], my);
                    out[4] = Math.min(out[4], mz); out[5] = Math.max(out[5], mz);
                }
            }
        }
        return out;
    }

    /**
     * The six face pixel maps for a box, derived from the vanilla
     * {@code ModelPart.Cube} unwrap (u0..u4 across, v0..v2 down) scaled to the
     * HD sheet. {@code uUsesA}/{@code vUsesA} say whether that pixel axis
     * tracks the face's first or second spanning body axis; the endpoint pair
     * is ordered for span-fraction 0 -> 1.
     */
    private static Map<Face, FaceMap> faceMapsOf(Raw r) {
        double k = TEXELS_PER_UNIT;
        double u0 = k * r.tu;
        double u1 = k * (r.tu + r.d);
        double u2 = k * (r.tu + r.d + r.w);
        double u2b = k * (r.tu + r.d + r.w + r.w);   // "u22" in Cube
        double u3 = k * (r.tu + r.d + r.w + r.d);
        double u4 = k * (r.tu + r.d + r.w + r.d + r.w);
        double v0 = k * r.tv;
        double v1 = k * (r.tv + r.d);
        double v2 = k * (r.tv + r.d + r.h);

        Map<Face, FaceMap> m = new EnumMap<>(Face.class);
        // RIGHT = Cube WEST (-X model). span (X, Y). u tracks X (tail->nose = u0->u1); v tracks Y (down->up = v2->v1).
        m.put(Face.RIGHT, new FaceMap(u0, u1, true, v2, v1, false));
        // LEFT = Cube EAST (+X model). span (X, Y). u tracks X but reversed vs RIGHT (u3->u2).
        m.put(Face.LEFT, new FaceMap(u3, u2, true, v2, v1, false));
        // NOSE = Cube NORTH (-Z model). span (Z, Y). u tracks Z (left->right = u2->u1); v tracks Y.
        m.put(Face.NOSE, new FaceMap(u2, u1, true, v2, v1, false));
        // TAIL = Cube SOUTH (+Z model). span (Z, Y). u tracks Z (left->right = u3->u4).
        m.put(Face.TAIL, new FaceMap(u3, u4, true, v2, v1, false));
        // TOP = Cube UP (+Y model, topline). span (X, Z). u tracks Z (left->right = u2b->u2); v tracks X (tail->nose = v0->v1).
        m.put(Face.TOP, new FaceMap(u2b, u2, false, v0, v1, true));
        // BOTTOM = Cube DOWN (-Y model, belly). span (X, Z). u tracks Z (left->right = u2->u1); v tracks X.
        m.put(Face.BOTTOM, new FaceMap(u2, u1, false, v0, v1, true));
        return m;
    }

    private static BodyPoint pointOf(Face face, double a, double b, double plane) {
        double x = 0, y = 0, z = 0;
        switch (face.spanA()) {
            case X -> x = a;
            case Y -> y = a;
            case Z -> z = a;
        }
        switch (face.spanB()) {
            case X -> x = b;
            case Y -> y = b;
            case Z -> z = b;
        }
        switch (face.normal()) {
            case X -> x = plane;
            case Y -> y = plane;
            case Z -> z = plane;
        }
        return new BodyPoint(x, y, z);
    }

    // ------------------------------------------------------------------
    // Small maths
    // ------------------------------------------------------------------

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static double invLerp(double from, double to, double value) {
        return from == to ? 0.0 : (value - from) / (to - from);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static boolean within(double edgeA, double edgeB, double v) {
        double lo = Math.min(edgeA, edgeB);
        double hi = Math.max(edgeA, edgeB);
        return v >= lo && v <= hi;
    }

    /** Floor {@code pixel} to a texel, then pull it inside both the face patch [e0,e1) and the sheet. */
    private static int texelInRect(double pixel, double e0, double e1) {
        int lo = (int) Math.floor(Math.min(e0, e1));
        int hi = (int) Math.ceil(Math.max(e0, e1)) - 1;
        if (hi < lo) {
            hi = lo;
        }
        int p = (int) Math.floor(pixel);
        if (p < lo) {
            p = lo;
        }
        if (p > hi) {
            p = hi;
        }
        return p < 0 ? 0 : (p >= SHEET_SIZE ? SHEET_SIZE - 1 : p);
    }
}
