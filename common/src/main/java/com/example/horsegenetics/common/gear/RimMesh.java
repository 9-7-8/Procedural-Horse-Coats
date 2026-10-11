package com.example.horsegenetics.common.gear;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Turns one box of a model, and a texture, into a worn piece with depth.</b>
 *
 * <p>The soft half of "worn gear is 3D" (owner, 2026-10-09): a piece that wraps
 * the horse is the horse's own mesh, lifted off the skin, with a wall round the
 * painted outline. Given a box's faces as they were baked and a texture's
 * painted texels, this returns plain quads:
 * <ul>
 *   <li>each face the piece touches, moved out by the lift - and grown sideways
 *       by the same amount, so the lifted faces of a box still meet at its
 *       corners, exactly as a deformed armour cube does;</li>
 *   <li>one wall per {@link RimOutline.Run}, from the lifted face straight back
 *       down to the skin, textured with the row or column of texels it borders
 *       so it takes their colour (and a dye or pattern carries onto it).</li>
 * </ul>
 *
 * <p>No wall is built where the piece carries on round a corner of the box: the
 * texel beyond a face's end is looked up on the neighbouring face. A wall there
 * would lie in the neighbour's lifted plane and flicker against it.
 *
 * <p>Nothing here is a Minecraft type, so the geometry is pinned by a test and
 * comes along on a port. The game side only reads baked cubes into
 * {@link Face}s and draws what comes back.
 */
public final class RimMesh {

    /** x y z, u v (0..1), then the quad's normal. */
    public static final int FLOATS_PER_VERTEX = 8;
    public static final int FLOATS_PER_QUAD = 4 * FLOATS_PER_VERTEX;

    /**
     * The most quads one worn mesh may hold before it is drawn flat instead.
     * A clean piece is a few hundred; only a dithered or noisy texture, which
     * has an edge at every other texel, comes near this.
     */
    public static final int QUAD_LIMIT = 4096;

    /** At or above this alpha (0..255) a texel is part of the piece. The translucent entity shader discards below 0.1. */
    public static final int PAINTED_ALPHA = 26;

    /** A texture, reduced to what the outline needs. */
    public interface Texels {
        int width();

        int height();

        /** False for anything off the sheet. */
        boolean painted(int u, int v);
    }

    /**
     * One face of a box, as baked.
     *
     * @param corners four corners, x y z each, in the bone's own units. The order
     *                is a baked polygon's: corner 1 is the uv origin, corner 0
     *                differs from it in u only, corner 2 in v only.
     * @param uv      the four corners' texture coordinates, 0..1
     */
    public record Face(float[] corners, float[] uv, float nx, float ny, float nz) {
    }

    /**
     * @param quads {@link #FLOATS_PER_QUAD} floats each
     * @param faces how many of them are lifted faces
     * @param rims  how many are walls
     */
    public record Built(float[] quads, int faces, int rims) {
        public int count() {
            return this.faces + this.rims;
        }
    }

    private RimMesh() {
    }

    /**
     * Build the worn piece for one box.
     *
     * @param faces every face of the one box (they are each other's neighbours)
     * @param lift  how far the piece stands off the box; refused under {@link WornRule#FLOOR}
     */
    public static Built box(List<Face> faces, Texels tex, float lift) {
        WornRule.lift(lift);
        List<Prepared> box = new ArrayList<>();
        for (Face face : faces) {
            Prepared prepared = Prepared.of(face, tex);
            if (prepared != null) {
                box.add(prepared);
            }
        }
        List<float[]> out = new ArrayList<>();
        int lifted = 0;
        int rims = 0;
        for (Prepared face : box) {
            RimOutline.Outline outline = RimOutline.trace(face.w, face.h,
                    (u, v) -> face.inside(u, v) ? tex.painted(face.umin + u, face.vmin + v)
                            : beyond(face, box, tex, u, v));
            if (outline.painted() == 0) {
                continue;
            }
            out.add(face.lifted(lift));
            lifted++;
            for (RimOutline.Run run : outline.runs()) {
                out.add(face.wall(run, lift, tex));
                rims++;
            }
        }
        float[] quads = new float[out.size() * FLOATS_PER_QUAD];
        for (int i = 0; i < out.size(); i++) {
            System.arraycopy(out.get(i), 0, quads, i * FLOATS_PER_QUAD, FLOATS_PER_QUAD);
        }
        return new Built(quads, lifted, rims);
    }

    /**
     * Is the texel just past {@code face}'s end painted - on whichever other face
     * of the box shares that edge? Unpainted if no face does.
     */
    private static boolean beyond(Prepared face, List<Prepared> box, Texels tex, int u, int v) {
        boolean uOut = u < 0 || u >= face.w;
        boolean vOut = v < 0 || v >= face.h;
        if (uOut && vOut) {
            return false;       // a diagonal neighbour borders nothing
        }
        // The middle of the texel edge that lies on the box's edge.
        float gu = u < 0 ? 0f : u >= face.w ? face.w : u + 0.5f;
        float gv = v < 0 ? 0f : v >= face.h ? face.h : v + 0.5f;
        float[] p = face.at(face.umin + gu, face.vmin + gv, face.o, face.eu, face.ev);
        for (Prepared other : box) {
            if (other == face || Math.abs(dot(other.n, face.n)) > 0.5f) {
                continue;
            }
            float[] rel = sub(p, other.o);
            if (Math.abs(dot(rel, other.n)) > 1.0e-3f) {
                continue;       // not in that face's plane
            }
            // Half a texel into the neighbour, away from the shared edge.
            boolean alongU = Math.abs(dot(other.eu, face.n)) > 0.5f * other.lenU;
            float step = alongU ? 0.5f * other.lenU / other.w : 0.5f * other.lenV / other.h;
            float[] q = {rel[0] - face.n[0] * step, rel[1] - face.n[1] * step, rel[2] - face.n[2] * step};
            float s = dot(q, other.eu) / (other.lenU * other.lenU);
            float t = dot(q, other.ev) / (other.lenV * other.lenV);
            if (s < 0f || s > 1f || t < 0f || t > 1f) {
                continue;
            }
            int tu = clamp((int) Math.floor(other.ua + s * (other.ub - other.ua)), other.umin, other.umin + other.w - 1);
            int tv = clamp((int) Math.floor(other.va + t * (other.vb - other.va)), other.vmin, other.vmin + other.h - 1);
            return tex.painted(tu, tv);
        }
        return false;
    }

    /** A face with its texel rectangle and its two edge vectors worked out. */
    private static final class Prepared {
        final float[] o;        // corner 1, the uv origin
        final float[] eu;       // to corner 0: the whole u extent
        final float[] ev;       // to corner 2: the whole v extent
        final float[] n;
        final float lenU;
        final float lenV;
        final int ua;
        final int ub;
        final int va;
        final int vb;
        final int umin;
        final int vmin;
        final int w;
        final int h;
        final float texW;
        final float texH;
        final float[] uv;
        final float winding;

        private Prepared(Face face, Texels tex) {
            float[] c = face.corners();
            this.o = new float[] {c[3], c[4], c[5]};
            this.eu = new float[] {c[0] - c[3], c[1] - c[4], c[2] - c[5]};
            this.ev = new float[] {c[6] - c[3], c[7] - c[4], c[8] - c[5]};
            this.n = new float[] {face.nx(), face.ny(), face.nz()};
            this.lenU = length(this.eu);
            this.lenV = length(this.ev);
            this.texW = tex.width();
            this.texH = tex.height();
            this.uv = face.uv();
            this.ua = Math.round(this.uv[2] * this.texW);
            this.ub = Math.round(this.uv[0] * this.texW);
            this.va = Math.round(this.uv[3] * this.texH);
            this.vb = Math.round(this.uv[5] * this.texH);
            this.umin = Math.min(this.ua, this.ub);
            this.vmin = Math.min(this.va, this.vb);
            this.w = Math.abs(this.ub - this.ua);
            this.h = Math.abs(this.vb - this.va);
            // Which way round the baked quad runs, so the walls can match it.
            float[] a = {c[3] - c[0], c[4] - c[1], c[5] - c[2]};
            float[] b = {c[6] - c[0], c[7] - c[1], c[8] - c[2]};
            this.winding = dot(cross(a, b), this.n);
        }

        /** Null for a face this cannot unwrap: no area, or a uv layout that is not a plain rectangle. */
        static Prepared of(Face face, Texels tex) {
            float[] uv = face.uv();
            if (face.corners().length != 12 || uv.length != 8) {
                return null;
            }
            float eps = 1.0e-4f;
            if (Math.abs(uv[1] - uv[3]) > eps || Math.abs(uv[4] - uv[2]) > eps) {
                return null;
            }
            Prepared p = new Prepared(face, tex);
            return p.w == 0 || p.h == 0 || p.lenU <= 0f || p.lenV <= 0f ? null : p;
        }

        boolean inside(int u, int v) {
            return u >= 0 && v >= 0 && u < this.w && v < this.h;
        }

        /** The point at texture grid position ({@code gu}, {@code gv}) on the rectangle {@code origin, spanU, spanV}. */
        float[] at(float gu, float gv, float[] origin, float[] spanU, float[] spanV) {
            float s = (gu - this.ua) / (this.ub - this.ua);
            float t = (gv - this.va) / (this.vb - this.va);
            return new float[] {
                    origin[0] + s * spanU[0] + t * spanV[0],
                    origin[1] + s * spanU[1] + t * spanV[1],
                    origin[2] + s * spanU[2] + t * spanV[2]};
        }

        private float[] liftedOrigin(float lift) {
            float ku = lift / this.lenU;
            float kv = lift / this.lenV;
            return new float[] {
                    this.o[0] - ku * this.eu[0] - kv * this.ev[0] + lift * this.n[0],
                    this.o[1] - ku * this.eu[1] - kv * this.ev[1] + lift * this.n[1],
                    this.o[2] - ku * this.eu[2] - kv * this.ev[2] + lift * this.n[2]};
        }

        private static float[] grown(float[] span, float len, float lift) {
            float k = 1f + 2f * lift / len;
            return new float[] {span[0] * k, span[1] * k, span[2] * k};
        }

        /** The whole face, moved out and grown; the texture's alpha cuts the piece out of it. */
        float[] lifted(float lift) {
            float[] lo = liftedOrigin(lift);
            float[] su = grown(this.eu, this.lenU, lift);
            float[] sv = grown(this.ev, this.lenV, lift);
            float[][] corners = {
                    {lo[0] + su[0], lo[1] + su[1], lo[2] + su[2]},
                    lo,
                    {lo[0] + sv[0], lo[1] + sv[1], lo[2] + sv[2]},
                    {lo[0] + su[0] + sv[0], lo[1] + su[1] + sv[1], lo[2] + su[2] + sv[2]}};
            float[] quad = new float[FLOATS_PER_QUAD];
            for (int i = 0; i < 4; i++) {
                put(quad, i, corners[i], this.uv[i * 2], this.uv[i * 2 + 1], this.n);
            }
            return quad;
        }

        /** The wall under one boundary run: from the lifted face down to the skin. */
        float[] wall(RimOutline.Run run, float lift, Texels tex) {
            boolean row = run.side() == RimOutline.Side.TOP || run.side() == RimOutline.Side.BOTTOM;
            boolean far = run.side() == RimOutline.Side.BOTTOM || run.side() == RimOutline.Side.RIGHT;
            float line = (row ? this.vmin : this.umin) + run.at() + (far ? 1 : 0);
            float a = (row ? this.umin : this.vmin) + run.from();
            float b = (row ? this.umin : this.vmin) + run.to();
            // The texels the wall borders: sampled along their middle, so the wall
            // is their colour and nothing else's.
            float mid = (row ? this.vmin : this.umin) + run.at() + 0.5f;

            float[] lo = liftedOrigin(lift);
            float[] su = grown(this.eu, this.lenU, lift);
            float[] sv = grown(this.ev, this.lenV, lift);
            float[] p0 = row ? at(a, line, lo, su, sv) : at(line, a, lo, su, sv);
            float[] p1 = row ? at(b, line, lo, su, sv) : at(line, b, lo, su, sv);
            float[] d0 = {p0[0] - lift * this.n[0], p0[1] - lift * this.n[1], p0[2] - lift * this.n[2]};
            float[] d1 = {p1[0] - lift * this.n[0], p1[1] - lift * this.n[1], p1[2] - lift * this.n[2]};

            // Outward: away from the painted texels, across the boundary.
            float[] axis = row ? this.ev : this.eu;
            float len = row ? this.lenV : this.lenU;
            float increasing = Math.signum(row ? this.vb - this.va : this.ub - this.ua);
            float sign = (far ? 1f : -1f) * increasing / len;
            float[] outward = {axis[0] * sign, axis[1] * sign, axis[2] * sign};

            float ua0 = (row ? a : mid) / this.texW;
            float va0 = (row ? mid : a) / this.texH;
            float ua1 = (row ? b : mid) / this.texW;
            float va1 = (row ? mid : b) / this.texH;

            float[][] pos = {p0, p1, d1, d0};
            float[][] st = {{ua0, va0}, {ua1, va1}, {ua1, va1}, {ua0, va0}};
            float turn = dot(cross(sub(p1, p0), sub(d1, p0)), outward);
            boolean flip = turn * this.winding < 0f;
            float[] quad = new float[FLOATS_PER_QUAD];
            for (int i = 0; i < 4; i++) {
                int from = flip ? 3 - i : i;
                put(quad, i, pos[from], st[from][0], st[from][1], outward);
            }
            return quad;
        }
    }

    private static void put(float[] quad, int vertex, float[] pos, float u, float v, float[] normal) {
        int at = vertex * FLOATS_PER_VERTEX;
        quad[at] = pos[0];
        quad[at + 1] = pos[1];
        quad[at + 2] = pos[2];
        quad[at + 3] = u;
        quad[at + 4] = v;
        quad[at + 5] = normal[0];
        quad[at + 6] = normal[1];
        quad[at + 7] = normal[2];
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float[] sub(float[] a, float[] b) {
        return new float[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[] {
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]};
    }

    private static float length(float[] a) {
        return (float) Math.sqrt(dot(a, a));
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }
}
