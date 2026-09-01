package com.example.horsegenetics.common.coat.skin;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a point in <b>horse body-space</b> into the exact texel on the coat
 * sheet that shades it, and back again - for the <b>adult</b> ({@link Skin#ADULT},
 * {@code HdHorseModel}) and the <b>foal</b> ({@link Skin#BABY},
 * {@code HdBabyHorseModel}) meshes.
 *
 * <h2>Body-space</h2>
 * A right-handed grid in <b>model units</b> (1 unit = 1/16 block =
 * {@link #TEXELS_PER_UNIT} texels on the {@value #SHEET_SIZE}px sheet):
 * <ul>
 *   <li><b>X</b> - 0 at the rear edge of the tail, increasing toward the nose.</li>
 *   <li><b>Y</b> - 0 at the bottom of the hooves, increasing upward.</li>
 *   <li><b>Z</b> - 0 on the centre plane; <b>+Z toward the horse's right</b>.</li>
 * </ul>
 * Origins are read off each mesh (X=0 = the backmost tail texel, Y=0 = the hoof
 * undersides), so a pattern that is a plain function of X is a seamless
 * front-to-back gradient across every part.
 *
 * <h2>Parts / faces</h2>
 * Each {@link Part} is an axis-aligned box (rotated parts use their rest-pose
 * AABB - an approximation). A {@link Face} looks along one axis and is spanned
 * by the other two: NOSE/TAIL span (Z,Y); TOP/BOTTOM span (X,Z); RIGHT/LEFT
 * span (X,Y). The foal mesh has no MANE or MUZZLE part.
 *
 * <p>The static no-{@code Skin} methods target {@link Skin#ADULT}; the
 * {@code Skin}-first overloads pick the mesh. Pure data + arithmetic - keep the
 * geometry tables in sync with {@code HdHorseModel} / {@code HdBabyHorseModel}.
 */
public final class HorseSkinGeometry {

    public static final int TEXELS_PER_UNIT = 2;
    public static final int SHEET_SIZE = 128;

    /** Which model the geometry describes. */
    public enum Skin { ADULT, BABY }

    public enum Axis { X, Y, Z }

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

        public Axis normal() {
            return normal;
        }

        public Axis spanA() {
            return normal == Axis.X ? Axis.Z : Axis.X;
        }

        public Axis spanB() {
            return normal == Axis.Y ? Axis.Z : Axis.Y;
        }
    }

    /** Every box either mesh can have. The foal mesh omits MUZZLE and MANE. */
    public enum Part {
        BODY, NECK, HEAD, MUZZLE, MANE, TAIL,
        LEFT_EAR, RIGHT_EAR,
        LEFT_FRONT_LEG, RIGHT_FRONT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG;

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

    public record BodyPoint(double x, double y, double z) {
        public double along(Axis a) {
            return switch (a) { case X -> x; case Y -> y; case Z -> z; };
        }
    }

    public record Texel(double u, double v, int x, int y, boolean clamped) {}

    public record Sample(Part part, Face face, BodyPoint point) {}

    // ------------------------------------------------------------------
    // Geometry tables. pivot = the part's world position after resolving its
    // parent chain; (ox,oy,oz)+(w,h,d) = its addBox; pitch = rest-pose xRot;
    // (tu,tv) = 64-space texOffs (HD sheet = x2). Foal head / ear pivots are
    // pre-resolved through the rotated neck.
    // ------------------------------------------------------------------

    private static final double ADULT_HEAD_PITCH = Math.PI / 6.0;

    private record Raw(
        Part part,
        double px, double py, double pz,
        double ox, double oy, double oz,
        double w, double h, double d,
        double pitch,
        int tu, int tv
    ) {}

    private static final List<Raw> ADULT_RAW = List.of(
        new Raw(Part.BODY, 0, 11, 5, -5, -8, -17, 10, 10, 22, 0, 0, 32),
        new Raw(Part.NECK, 0, 4, -12, -2.05, -6, -2, 4, 12, 7, ADULT_HEAD_PITCH, 0, 35),
        new Raw(Part.HEAD, 0, 4, -12, -3, -11, -2, 6, 5, 7, ADULT_HEAD_PITCH, 0, 13),
        new Raw(Part.MUZZLE, 0, 4, -12, -2, -11, -7, 4, 5, 5, ADULT_HEAD_PITCH, 0, 25),
        new Raw(Part.MANE, 0, 4, -12, -1, -11, 5.01, 2, 16, 2, ADULT_HEAD_PITCH, 56, 36),
        new Raw(Part.TAIL, 0, 6, 7, -1.5, 0, 0, 3, 14, 4, ADULT_HEAD_PITCH, 42, 36),
        new Raw(Part.LEFT_EAR, 0, 4, -12, 0.55, -13, 4, 2, 3, 1, ADULT_HEAD_PITCH, 19, 0),
        new Raw(Part.RIGHT_EAR, 0, 4, -12, -2.55, -13, 4, 2, 3, 1, ADULT_HEAD_PITCH, 19, 16),
        new Raw(Part.LEFT_HIND_LEG, 4, 14, 7, -3, -1.01, -1, 4, 11, 4, 0, 26, 0),
        new Raw(Part.RIGHT_HIND_LEG, -4, 14, 7, -1, -1.01, -1, 4, 11, 4, 0, 48, 21),
        new Raw(Part.LEFT_FRONT_LEG, 4, 14, -10, -3, -1.01, -1.9, 4, 11, 4, 0, 26, 16),
        new Raw(Part.RIGHT_FRONT_LEG, -4, 14, -10, -1, -1.01, -1.9, 4, 11, 4, 0, 48, 0)
    );

    // vanilla BabyHorseModel.createBabyMesh. neck pitch 0.6109; tail pitch -0.7418.
    // head / ear pivots pre-resolved through the rotated neck (ear Z-roll ignored).
    private static final double BABY_NECK_PITCH = 0.6109;

    private static final List<Raw> BABY_RAW = List.of(
        new Raw(Part.BODY, 0, 12.5, 0, -4, -3.5, -7, 8, 7, 14, 0, 0, 13),
        new Raw(Part.NECK, 0, 10, -6, -2, -6, -2, 4, 8, 4, BABY_NECK_PITCH, 30, 0),
        new Raw(Part.HEAD, 0, 5.212, -9.713, -3, -3.9484, -6.705, 6, 4, 9, BABY_NECK_PITCH, 0, 0),
        new Raw(Part.TAIL, 0, 11.5, 7, -1.5, -1.5, -1, 3, 3, 8, -0.7418, 24, 34),
        new Raw(Part.LEFT_EAR, 2, 0.616, -10.557, -1, -2.5, -0.8, 2, 3, 1, BABY_NECK_PITCH, 0, 4),
        new Raw(Part.RIGHT_EAR, -2, 0.788, -10.802, -1, -2.5, -0.5, 2, 3, 1, BABY_NECK_PITCH, 0, 0),
        new Raw(Part.LEFT_HIND_LEG, 2.4, 16, 5.4, -1.5, -1, -1.5, 3, 9, 3, 0, 12, 46),
        new Raw(Part.RIGHT_HIND_LEG, -2.4, 16, 5.4, -1.5, -1, -1.5, 3, 9, 3, 0, 0, 46),
        new Raw(Part.LEFT_FRONT_LEG, 2.4, 16, -5.4, -1.5, -1, -1.5, 3, 9, 3, 0, 12, 34),
        new Raw(Part.RIGHT_FRONT_LEG, -2.4, 16, -5.4, -1.5, -1, -1.5, 3, 9, 3, 0, 0, 34)
    );

    private record FaceMap(double u0, double u1, boolean uUsesA, double v0, double v1, boolean vUsesA) {}

    private record PartData(Bounds bounds, Map<Face, FaceMap> faces) {}

    /** One baked mesh. */
    private static final class Mesh {
        final Map<Part, PartData> parts = new EnumMap<>(Part.class);
        final Bounds bodyBounds;
        private volatile Sample[] sampleGrid;

        Mesh(List<Raw> raw) {
            Map<Part, double[]> modelAabb = new EnumMap<>(Part.class);
            double mzMaxAll = Double.NEGATIVE_INFINITY;
            double myMaxAll = Double.NEGATIVE_INFINITY;
            for (Raw r : raw) {
                double[] aabb = modelAabbOf(r);
                modelAabb.put(r.part, aabb);
                myMaxAll = Math.max(myMaxAll, aabb[3]);
                mzMaxAll = Math.max(mzMaxAll, aabb[5]);
            }
            double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
            double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
            double zMin = Double.POSITIVE_INFINITY, zMax = Double.NEGATIVE_INFINITY;
            for (Raw r : raw) {
                double[] m = modelAabb.get(r.part);
                Bounds b = new Bounds(
                    mzMaxAll - m[5], mzMaxAll - m[4],
                    myMaxAll - m[3], myMaxAll - m[2],
                    -m[1], -m[0]);
                parts.put(r.part, new PartData(b, faceMapsOf(r)));
                xMin = Math.min(xMin, b.xMin); xMax = Math.max(xMax, b.xMax);
                yMin = Math.min(yMin, b.yMin); yMax = Math.max(yMax, b.yMax);
                zMin = Math.min(zMin, b.zMin); zMax = Math.max(zMax, b.zMax);
            }
            bodyBounds = new Bounds(xMin, xMax, yMin, yMax, zMin, zMax);
        }

        Bounds bounds(Part part) {
            PartData pd = parts.get(part);
            if (pd == null) {
                throw new IllegalArgumentException("this mesh has no part " + part);
            }
            return pd.bounds();
        }

        boolean has(Part part) {
            return parts.containsKey(part);
        }

        Sample[] grid() {
            Sample[] g = sampleGrid;
            if (g == null) {
                g = new Sample[SHEET_SIZE * SHEET_SIZE];
                for (int py = 0; py < SHEET_SIZE; py++) {
                    for (int px = 0; px < SHEET_SIZE; px++) {
                        g[py * SHEET_SIZE + px] = sampleUncached(px, py).orElse(null);
                    }
                }
                sampleGrid = g;
            }
            return g;
        }

        Optional<Sample> sample(int px, int py) {
            if (px < 0 || py < 0 || px >= SHEET_SIZE || py >= SHEET_SIZE) {
                return Optional.empty();
            }
            return Optional.ofNullable(grid()[py * SHEET_SIZE + px]);
        }

        private Optional<Sample> sampleUncached(int px, int py) {
            double cx = px + 0.5;
            double cy = py + 0.5;
            for (Map.Entry<Part, PartData> pe : parts.entrySet()) {
                PartData pd = pe.getValue();
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
                    return Optional.of(new Sample(pe.getKey(), face, pointOf(face, a, b, plane)));
                }
            }
            return Optional.empty();
        }

        Texel project(Part part, Face face, double a, double b) {
            PartData pd = parts.get(part);
            FaceMap fm = pd.faces().get(face);
            Bounds bd = pd.bounds();
            double fa = invLerp(bd.min(face.spanA()), bd.max(face.spanA()), a);
            double fb = invLerp(bd.min(face.spanB()), bd.max(face.spanB()), b);
            boolean clamped = fa < 0 || fa > 1 || fb < 0 || fb > 1;
            fa = clamp01(fa);
            fb = clamp01(fb);
            double u = lerp(fm.u0(), fm.u1(), fm.uUsesA() ? fa : fb);
            double v = lerp(fm.v0(), fm.v1(), fm.vUsesA() ? fa : fb);
            return new Texel(u, v, texelInRect(u, fm.u0(), fm.u1()), texelInRect(v, fm.v0(), fm.v1()), clamped);
        }
    }

    private static final Mesh ADULT_MESH = new Mesh(ADULT_RAW);
    private static final Mesh BABY_MESH = new Mesh(BABY_RAW);

    private static Mesh mesh(Skin skin) {
        return skin == Skin.BABY ? BABY_MESH : ADULT_MESH;
    }

    private HorseSkinGeometry() {}

    // ------------------------------------------------------------------
    // Public API - static (ADULT) + Skin-first overloads
    // ------------------------------------------------------------------

    public static boolean hasPart(Skin skin, Part part) {
        return mesh(skin).has(part);
    }

    public static Bounds bounds(Part part) {
        return ADULT_MESH.bounds(part);
    }

    public static Bounds bounds(Skin skin, Part part) {
        return mesh(skin).bounds(part);
    }

    public static Bounds bodyBounds() {
        return ADULT_MESH.bodyBounds;
    }

    public static Bounds bodyBounds(Skin skin) {
        return mesh(skin).bodyBounds;
    }

    public static void forEachTexel(TexelVisitor visitor) {
        forEachTexel(Skin.ADULT, visitor);
    }

    public static void forEachTexel(Skin skin, TexelVisitor visitor) {
        Sample[] g = mesh(skin).grid();
        for (int py = 0; py < SHEET_SIZE; py++) {
            for (int px = 0; px < SHEET_SIZE; px++) {
                Sample s = g[py * SHEET_SIZE + px];
                if (s != null) {
                    visitor.visit(px, py, s.part(), s.face(), s.point());
                }
            }
        }
    }

    public static void forEachTexel(Part part, TexelVisitor visitor) {
        forEachTexel(Skin.ADULT, part, visitor);
    }

    public static void forEachTexel(Skin skin, Part part, TexelVisitor visitor) {
        Sample[] g = mesh(skin).grid();
        for (int py = 0; py < SHEET_SIZE; py++) {
            for (int px = 0; px < SHEET_SIZE; px++) {
                Sample s = g[py * SHEET_SIZE + px];
                if (s != null && s.part() == part) {
                    visitor.visit(px, py, s.part(), s.face(), s.point());
                }
            }
        }
    }

    @FunctionalInterface
    public interface TexelVisitor {
        void visit(int px, int py, Part part, Face face, BodyPoint point);
    }

    public static Optional<Sample> sample(int px, int py) {
        return ADULT_MESH.sample(px, py);
    }

    public static Optional<Sample> sample(Skin skin, int px, int py) {
        return mesh(skin).sample(px, py);
    }

    public static Texel project(Part part, Face face, double a, double b) {
        return ADULT_MESH.project(part, face, a, b);
    }

    public static Texel project(Skin skin, Part part, Face face, double a, double b) {
        return mesh(skin).project(part, face, a, b);
    }

    public static Texel project(Part part, Face face, BodyPoint point) {
        return project(part, face, point.along(face.spanA()), point.along(face.spanB()));
    }

    // ------------------------------------------------------------------
    // Build helpers (shared)
    // ------------------------------------------------------------------

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

    private static Map<Face, FaceMap> faceMapsOf(Raw r) {
        double k = TEXELS_PER_UNIT;
        double u0 = k * r.tu;
        double u1 = k * (r.tu + r.d);
        double u2 = k * (r.tu + r.d + r.w);
        double u2b = k * (r.tu + r.d + r.w + r.w);
        double u3 = k * (r.tu + r.d + r.w + r.d);
        double u4 = k * (r.tu + r.d + r.w + r.d + r.w);
        double v0 = k * r.tv;
        double v1 = k * (r.tv + r.d);
        double v2 = k * (r.tv + r.d + r.h);

        Map<Face, FaceMap> m = new EnumMap<>(Face.class);
        m.put(Face.RIGHT, new FaceMap(u0, u1, true, v2, v1, false));
        m.put(Face.LEFT, new FaceMap(u3, u2, true, v2, v1, false));
        m.put(Face.NOSE, new FaceMap(u2, u1, true, v2, v1, false));
        m.put(Face.TAIL, new FaceMap(u3, u4, true, v2, v1, false));
        m.put(Face.TOP, new FaceMap(u2b, u2, false, v0, v1, true));
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
