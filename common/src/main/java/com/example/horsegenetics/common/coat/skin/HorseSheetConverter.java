package com.example.horsegenetics.common.coat.skin;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Patch;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Any sheet painted on the vanilla horse mesh, onto this mod's layout.</b>
 *
 * <p>The white template was made this way by hand once (vanilla's 64px sheet,
 * scaled 2x by nearest neighbour, then the leg and ear patches copied into the
 * spare room the HD mesh gives each leg and ear); this is that process written
 * down, so the undead sheets, a resource pack's white horse or anyone else's
 * coat go through the same code. The layout comes from
 * {@link HorseSkinGeometry#patches} - no coordinate is typed here.
 *
 * <ul>
 *   <li><b>Scale</b> by an integer factor only: up by nearest neighbour, so a
 *       binary alpha stays binary and pixel art keeps its edges; down by an exact
 *       box average, which {@link Result#warnings} reports.</li>
 *   <li><b>Re-lay</b> the adult: every patch the HD mesh moved is copied from
 *       vanilla's shared leg or ear patch to its own place. The baby is scaled
 *       only - vanilla's foal already gives every box its own patch.</li>
 *   <li><b>Mirror mode.</b> {@link Mirror#STRAIGHT} copies the shared patch as it
 *       is, which is what the white template did and what reproduces it to the
 *       pixel. {@link Mirror#VANILLA} applies vanilla's cube mirror to the patches
 *       of the boxes vanilla mirrors, so a left leg looks as it does in vanilla on
 *       a mesh that does not mirror: every face flipped left to right and the two
 *       side strips swapped (read from 26.1.2's {@code ModelPart$Cube}, which swaps
 *       the box's x bounds and keeps each face's UVs, 2026-10-02).</li>
 *   <li><b>Checks</b>: drawn pixels outside every box of the vanilla layout are
 *       counted (a sheet painted for some other mesh lands there) and refused past
 *       {@link #REFUSE_STRAY_SHARE}; a destination patch that the source already
 *       draws in is a hard error, because copying over it would silently overwrite
 *       a pack that painted the spare space.</li>
 * </ul>
 * RGBA in, RGBA out; nothing is premultiplied, and a fully transparent pixel's
 * colour is left as it was.
 */
public final class HorseSheetConverter {

    /** Above this share of the drawn pixels lying outside every box, the sheet is not the horse mesh. */
    public static final double REFUSE_STRAY_SHARE = 0.02;

    /** How a patch vanilla mirrors is carried onto a mesh that does not. */
    public enum Mirror { STRAIGHT, VANILLA }

    /** A converted sheet, and what the checks found on the way. */
    public record Result(int[] argb, int size, int strayPixels, int drawnPixels, List<String> warnings) {
    }

    private HorseSheetConverter() {
    }

    /**
     * Convert {@code src} (row-major ARGB, {@code srcSize} square, a multiple of 64)
     * to this mod's layout at {@code outSize} (default {@link HorseSkinGeometry#SHEET_SIZE}).
     *
     * @throws IllegalArgumentException on a size that is not a whole multiple, a
     *         sheet that is not the vanilla horse mesh, or a destination patch the
     *         source already paints
     */
    public static Result convert(int[] src, int srcSize, Skin skin, Mirror mirror, int outSize) {
        if (srcSize <= 0 || srcSize % 64 != 0 || src.length != srcSize * srcSize) {
            throw new IllegalArgumentException("a horse sheet is square and a multiple of 64 px; got "
                    + srcSize + " with " + src.length + " pixels");
        }
        if (outSize % 64 != 0 || (outSize % srcSize != 0 && srcSize % outSize != 0)) {
            throw new IllegalArgumentException("only integer scale factors: " + srcSize + " -> " + outSize);
        }
        List<String> warnings = new ArrayList<>();
        List<Patch> patches = HorseSkinGeometry.patches(skin);
        int unit = srcSize / 64;

        // The mesh sanity check, on the source as given: what does it draw that no box reads?
        boolean[] known = new boolean[srcSize * srcSize];
        for (Patch p : patches) {
            markFaces(known, srcSize, unit, p.srcU(), p.srcV(), p);
        }
        int drawn = 0;
        int stray = 0;
        for (int i = 0; i < src.length; i++) {
            if ((src[i] >>> 24) != 0) {
                drawn++;
                if (!known[i]) {
                    stray++;
                }
            }
        }
        if (drawn > 0 && stray > drawn * REFUSE_STRAY_SHARE) {
            throw new IllegalArgumentException(stray + " of " + drawn
                    + " drawn pixels are outside every box of the vanilla horse mesh: not a horse sheet");
        }
        if (stray > 0) {
            warnings.add(stray + " drawn pixel(s) outside every box of the vanilla horse mesh (ignored)");
        }

        // The overlap check: a patch this converter writes must be empty on the source.
        for (Patch p : patches) {
            if (!p.relaid() || (p.srcU() == p.dstU() && p.srcV() == p.dstV())) {
                continue;
            }
            boolean[] dst = new boolean[srcSize * srcSize];
            markFaces(dst, srcSize, unit, p.dstU(), p.dstV(), p);
            for (int i = 0; i < dst.length; i++) {
                if (dst[i] && (src[i] >>> 24) != 0) {
                    throw new IllegalArgumentException("the source already paints " + p.part()
                            + "'s destination patch at (" + p.dstU() + ", " + p.dstV()
                            + "); converting would overwrite it");
                }
            }
            for (Patch other : patches) {
                if (other == p) {
                    continue;
                }
                boolean[] theirs = new boolean[srcSize * srcSize];
                markFaces(theirs, srcSize, unit, other.dstU(), other.dstV(), other);
                for (int i = 0; i < dst.length; i++) {
                    if (dst[i] && theirs[i]) {
                        throw new IllegalArgumentException(p.part() + " and " + other.part()
                                + " would share destination texels");
                    }
                }
            }
        }

        int[] out = scale(src, srcSize, outSize, warnings);
        int s = outSize / 64;
        for (Patch p : patches) {
            if (!p.relaid()) {
                continue;
            }
            boolean flip = mirror == Mirror.VANILLA && p.vanillaMirrors();
            copyPatch(out, outSize, s, p, flip);
        }
        return new Result(out, outSize, stray, drawn, List.copyOf(warnings));
    }

    /** {@link #convert} at the mod's own sheet size. */
    public static Result convert(int[] src, int srcSize, Skin skin, Mirror mirror) {
        return convert(src, srcSize, skin, mirror, HorseSkinGeometry.SHEET_SIZE);
    }

    private static int[] scale(int[] src, int srcSize, int outSize, List<String> warnings) {
        int[] out = new int[outSize * outSize];
        if (outSize >= srcSize) {
            int f = outSize / srcSize;
            for (int y = 0; y < outSize; y++) {
                for (int x = 0; x < outSize; x++) {
                    out[y * outSize + x] = src[(y / f) * srcSize + (x / f)];
                }
            }
            return out;
        }
        int f = srcSize / outSize;
        warnings.add("downscaled " + srcSize + " -> " + outSize + " by a " + f + "x" + f + " box average");
        int n = f * f;
        for (int y = 0; y < outSize; y++) {
            for (int x = 0; x < outSize; x++) {
                long a = 0, r = 0, g = 0, b = 0;
                for (int dy = 0; dy < f; dy++) {
                    for (int dx = 0; dx < f; dx++) {
                        int c = src[(y * f + dy) * srcSize + (x * f + dx)];
                        a += c >>> 24;
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                    }
                }
                out[y * outSize + x] = (int) ((a / n) << 24 | (r / n) << 16 | (g / n) << 8 | (b / n));
            }
        }
        return out;
    }

    /**
     * Copy a patch from its source to its destination on the scaled sheet. With
     * {@code flip}, the copy is vanilla's mirrored cube: each of the four
     * {@code w}-wide faces flipped in place, and the two {@code d}-wide side strips
     * flipped and swapped.
     */
    private static void copyPatch(int[] out, int size, int s, Patch p, boolean flip) {
        int w = p.w() * s, h = p.h() * s, d = p.d() * s;
        int pw = 2 * (d + w), ph = d + h;
        int su = p.srcU() * s, sv = p.srcV() * s, du = p.dstU() * s, dv = p.dstV() * s;
        int[] patch = new int[pw * ph];
        for (int y = 0; y < ph; y++) {
            for (int x = 0; x < pw; x++) {
                patch[y * pw + x] = out[(sv + y) * size + (su + x)];
            }
        }
        for (int y = 0; y < ph; y++) {
            for (int x = 0; x < pw; x++) {
                int from = flip ? mirroredSource(x, y, w, d) : x;
                if (from < 0) {
                    continue; // a corner no face reads
                }
                int tx = du + x, ty = dv + y;
                if (tx < size && ty < size) {
                    out[ty * size + tx] = patch[y * pw + from];
                }
            }
        }
    }

    /**
     * Which column of the source patch a mirrored cube shows at column {@code x}
     * of row {@code y}, or -1 for a corner. Top band (height d): the two faces at
     * {@code [d, d+w)} and {@code [d+w, d+2w)}, each flipped in place. Lower band:
     * {@code [0, d)} and {@code [d+w, 2d+w)} swap, flipped; {@code [d, d+w)} and
     * {@code [2d+w, 2d+2w)} flip in place.
     */
    static int mirroredSource(int x, int y, int w, int d) {
        if (y < d) {
            if (x >= d && x < d + w) {
                return d + (d + w - 1 - x);
            }
            if (x >= d + w && x < d + 2 * w) {
                return d + w + (d + 2 * w - 1 - x);
            }
            return -1;
        }
        if (x < d) {
            return d + w + (d - 1 - x);
        }
        if (x < d + w) {
            return d + (d + w - 1 - x);
        }
        if (x < 2 * d + w) {
            return (2 * d + w - 1 - x);
        }
        return 2 * d + w + (2 * d + 2 * w - 1 - x);
    }

    /** Mark the texels the faces of {@code p}'s cube occupy at {@code (u, v)} (64-space). */
    private static void markFaces(boolean[] mask, int size, int unit, int u, int v, Patch p) {
        int w = p.w(), h = p.h(), d = p.d();
        fill(mask, size, unit, u + d, v, 2 * w, d);
        fill(mask, size, unit, u, v + d, 2 * (d + w), h);
    }

    private static void fill(boolean[] mask, int size, int unit, int x0, int y0, int w, int h) {
        for (int y = y0 * unit; y < (y0 + h) * unit && y < size; y++) {
            for (int x = x0 * unit; x < (x0 + w) * unit && x < size; x++) {
                mask[y * size + x] = true;
            }
        }
    }
}
