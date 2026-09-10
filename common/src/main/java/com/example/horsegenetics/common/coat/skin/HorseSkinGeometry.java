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
 * <p>{@link #posed} is the escape hatch for anything that <i>draws</i> the
 * horse: the AABB is right for looking a texel up and wrong for a silhouette,
 * because a pitched neck's box is nearly twice the neck.
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

        /**
         * Whether this face sits at the box's <b>high</b> end on its normal
         * axis - which is to say which of the two ways along that axis the
         * surface actually points.
         *
         * <p>{@link #normal} alone is the axis and not the direction, and the
         * two faces that share an axis face opposite ways: a wash on the
         * upward-pointing planes has to include TOP and exclude BOTTOM, and
         * without this it cannot tell them apart.
         */
        public boolean atMax() {
            return atMax;
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
        /** The cuboids the AABBs were taken from - what {@link #posed} poses. */
        final Map<Part, Raw> raws = new EnumMap<>(Part.class);
        final Bounds bodyBounds;
        /** The model->body offsets: bodyX = mzMax - mz, bodyY = myMax - my, bodyZ = -mx. */
        final double myMax;
        final double mzMax;
        private volatile Sample[] sampleGrid;

        Mesh(List<Raw> raw) {
            Map<Part, double[]> modelAabb = new EnumMap<>(Part.class);
            double mzMaxAll = Double.NEGATIVE_INFINITY;
            double myMaxAll = Double.NEGATIVE_INFINITY;
            for (Raw r : raw) {
                double[] aabb = modelAabbOf(r);
                modelAabb.put(r.part, aabb);
                raws.put(r.part, r);
                myMaxAll = Math.max(myMaxAll, aabb[3]);
                mzMaxAll = Math.max(mzMaxAll, aabb[5]);
            }
            this.myMax = myMaxAll;
            this.mzMax = mzMaxAll;
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

        BodyPoint posed(Part part, Face face, double fa, double fb) {
            Raw r = raw(part);
            double cx = bodyFraction(Axis.X, face, fa, fb);
            double cy = bodyFraction(Axis.Y, face, fa, fb);
            double cz = bodyFraction(Axis.Z, face, fa, fb);
            // Body and local axes run against each other on all three axes (see
            // the constructor), so a body fraction c is a local fraction 1 - c
            // on the axis it is paired with: bodyX<->local z, bodyY<->local y,
            // bodyZ<->local x.
            double lx = r.ox + (1.0 - cz) * r.w;
            double ly = r.oy + (1.0 - cy) * r.h;
            double lz = r.oz + (1.0 - cx) * r.d;
            double cos = Math.cos(r.pitch);
            double sin = Math.sin(r.pitch);
            double mx = r.px + lx;
            double my = r.py + (ly * cos - lz * sin);
            double mz = r.pz + (ly * sin + lz * cos);
            return new BodyPoint(mzMax - mz, myMax - my, -mx);
        }

        /**
         * The inverse of {@link #posed}'s frame change, as fractions: body
         * space back to model space, model space back into the part's local
         * box by undoing the pivot and the pitch, then each local coordinate
         * divided by that edge's length and flipped to run the body way.
         */
        BodyPoint local(Part part, BodyPoint p) {
            Raw r = raw(part);
            double mx = -p.z();
            double my = myMax - p.y();
            double mz = mzMax - p.x();
            double a = my - r.py;
            double b = mz - r.pz;
            double cos = Math.cos(r.pitch);
            double sin = Math.sin(r.pitch);
            double lx = mx - r.px;
            double ly = a * cos + b * sin;
            double lz = -a * sin + b * cos;
            return new BodyPoint(
                    1.0 - (lz - r.oz) / r.d,
                    1.0 - (ly - r.oy) / r.h,
                    1.0 - (lx - r.ox) / r.w);
        }

        BodyPoint posedNormal(Part part, Face face) {
            Raw r = raw(part);
            // The face's outward direction in local space. Every body axis is
            // the reverse of the local axis it pairs with, so "at max" points
            // along the negative local axis.
            double sign = face.atMax ? -1.0 : 1.0;
            double dx = face.normal() == Axis.Z ? sign : 0.0;
            double dy = face.normal() == Axis.Y ? sign : 0.0;
            double dz = face.normal() == Axis.X ? sign : 0.0;
            double cos = Math.cos(r.pitch);
            double sin = Math.sin(r.pitch);
            double ry = dy * cos - dz * sin;
            double rz = dy * sin + dz * cos;
            // Back to body space. The offsets cancel on a direction; only the
            // axis swap and the three sign flips survive.
            return new BodyPoint(-rz, -ry, -dx);
        }

        private Raw raw(Part part) {
            Raw r = raws.get(part);
            if (r == null) {
                throw new IllegalArgumentException("this mesh has no part " + part);
            }
            return r;
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

    /**
     * <b>Where a body-space point sits inside the part's own box</b>, as three
     * fractions in 0..1 - the part's <i>local frame</i>, with its rest-pose
     * pitch taken back out.
     *
     * <p>This is the answer to "along the neck" and "across the neck", which the
     * axis-aligned {@link #bounds} cannot express: a pitched part's AABB is not
     * the part, so a band on body Y across a 30&deg;-pitched neck is a collar
     * round the throat and the crest alike, not a stripe up the crest. Here the
     * pitch is undone first, so each fraction runs along one edge of the actual
     * cuboid however it is tilted.
     *
     * <p>The three fractions are named for the <b>body</b> axis each is paired
     * with and run the same way body space does - X toward the nose, Y upward,
     * Z toward the horse's right - because a gene author reading
     * {@code "axis": "X"} should get the same direction whichever space they
     * asked for. Body and local axes run against each other on all three
     * (see {@link Mesh#posed}), so each fraction is a flipped local one.
     *
     * <p>On an <b>unpitched</b> part the box <i>is</i> its AABB and this returns
     * exactly what normalising against {@link #bounds} does; the two only
     * diverge on the neck, the head, the muzzle, the mane, the ears and the
     * tail. {@code HorseSkinGeometryTest} pins that equivalence.
     *
     * <p>Values outside 0..1 are possible and are not clamped: a texel on the
     * mane sampled against the neck is legitimately off the end of it, and the
     * band arithmetic in {@code SpecPainter} wants to see that rather than have
     * it folded back onto the edge.
     */
    public static BodyPoint local(Skin skin, Part part, BodyPoint point) {
        return mesh(skin).local(part, point);
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
    // The posed mesh - the horse as it is actually drawn
    // ------------------------------------------------------------------

    /**
     * <b>The same texel, on the posed cuboid rather than on its bounding box.</b>
     *
     * <p>Everything above works in {@link Bounds} - each part's rest-pose
     * <i>axis-aligned bounding box</i> - because that is all the coat pipeline
     * needs: it projects a texel onto a box and never asks what shape the horse
     * is. For a pitched part the AABB is much bigger than the part. The adult
     * neck is a 4x12x7 cuboid tilted 30 degrees, whose AABB is 4x13.9x12.1 -
     * nearly twice as deep - so anything that <i>draws</i> from the bounds gets
     * a pile of oversized blocks rather than a horse.
     *
     * <p>This walks the raw cuboid instead ({@code origin + size}, rotated about
     * the pivot) exactly as {@code HdHorseModel} poses it, and only then flips
     * into body space. Nothing about the pipeline changes; a renderer just stops
     * lying about the silhouette. {@code wiki/gene-creator/js/model3d.js}
     * {@code emitPart} is the same arithmetic for the browser's preview horse -
     * a change here is a change there.
     *
     * @param fa fraction along the face's {@link Face#spanA()} axis, 0..1
     * @param fb fraction along the face's {@link Face#spanB()} axis, 0..1
     */
    public static BodyPoint posed(Skin skin, Part part, Face face, double fa, double fb) {
        return mesh(skin).posed(part, face, fa, fb);
    }

    /**
     * The posed position of a point given on the part's bounding box - the
     * {@link BodyPoint} {@link #forEachTexel} and {@link #sample} hand out. The
     * two span fractions are all that is read off it; the normal axis is taken
     * from the face.
     */
    public static BodyPoint posed(Skin skin, Part part, Face face, BodyPoint onBounds) {
        Bounds b = bounds(skin, part);
        double fa = invLerp(b.min(face.spanA()), b.max(face.spanA()), onBounds.along(face.spanA()));
        double fb = invLerp(b.min(face.spanB()), b.max(face.spanB()), onBounds.along(face.spanB()));
        return posed(skin, part, face, fa, fb);
    }

    /**
     * The face's outward <b>unit direction</b> in body space, after the part's
     * pitch - a vector, carried in a {@link BodyPoint} for the arithmetic. On an
     * unpitched part it is the plain body axis; on the neck or the tail it is
     * not, which is the whole reason to ask.
     */
    public static BodyPoint posedNormal(Skin skin, Part part, Face face) {
        return mesh(skin).posedNormal(part, face);
    }

    /**
     * Where a face-local {@code (a, b)} pair sits along one body axis, as a
     * fraction: the two span axes take it straight, and the axis the face looks
     * along is pinned to whichever end of the box the face is.
     */
    private static double bodyFraction(Axis axis, Face face, double fa, double fb) {
        if (face.normal() == axis) {
            return face.atMax ? 1.0 : 0.0;
        }
        return face.spanA() == axis ? fa : fb;
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
        // TOP is the face at body-space yMax (the horse's spine / topline); BOTTOM
        // is at yMin (the belly). These two UV patches were the wrong way round -
        // a painter that whitened "from below" (splash, sabino belly, frame off
        // the underline) was flooding the back, and the belly stayed coloured.
        m.put(Face.TOP, new FaceMap(u2, u1, false, v0, v1, true));
        m.put(Face.BOTTOM, new FaceMap(u2b, u2, false, v0, v1, true));
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
