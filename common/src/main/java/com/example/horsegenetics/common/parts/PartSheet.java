package com.example.horsegenetics.common.parts;

/**
 * <b>Where a part's boxes get their pixels.</b> One 64x64 sheet, cut into a 4x4
 * grid of 16x16 regions; every box of every attached part samples one region.
 *
 * <p>The sheet is <b>greyscale</b>. Colour is a per-horse tint applied at draw
 * time - the {@code BraidLayer} method - so one file serves an ivory horn, a
 * gold one and a rose-pearl one, and a lineage whose horn colour drifts over
 * thirty generations needs no new art. What a region carries is <b>grain and
 * light</b>, not hue.
 *
 * <h2>Why a box samples the corner of its region, not the whole of it</h2>
 * A baked cube's UV rectangle is its own unwrap - {@code 2*(girth+girth)} texels
 * wide and {@code girth+length} tall - laid down at {@code texOffs}. A horn
 * segment is about 10x4 texels at the very largest, so it reads the top-left
 * corner of its region and nothing else. That is deliberate: it means the
 * regions never need to know what size the boxes are, and a part whose
 * proportions change with an epigenetic number does not need a new sheet. The
 * cost is that <b>a region must be usable at any sub-rectangle of itself</b> -
 * so the baker writes a uniform, tileable grain rather than a picture with a
 * composition. See {@code neoforge-26.1.2/tools/bake-part-sheet.mjs}.
 *
 * <h2>The ids are shared, so they live in one class</h2>
 * The baker, the generators here and the client all name the same regions. Three
 * copies of {@code 0} and {@code 1} would drift the first time a region was
 * inserted, and the failure is a horn textured with antler.
 */
public final class PartSheet {

    /** Edge of the whole sheet, in pixels. */
    public static final int SIZE = 64;

    /** Edge of one region, in pixels. {@link #SIZE} must be a multiple of it. */
    public static final int REGION = 16;

    /** Regions per row, and per column. */
    public static final int ACROSS = SIZE / REGION;

    /** Ivory / keratin grain, running along the horn's growth direction. Opaque. */
    public static final int HORN = 0;

    /** The same grain, denser and a shade darker: the last two segments of a horn. */
    public static final int HORN_TIP = 1;

    /**
     * How many regions the sheet has room for. The grid is deliberately larger
     * than the two regions in use: antlers, crystals and hoof feathering each
     * want one or two, and they land without moving a single UV that already
     * works. A region nobody samples is blank and costs nothing but disk.
     */
    public static final int CAPACITY = ACROSS * ACROSS;

    private PartSheet() {
    }

    /** Left edge of {@code region}, in pixels - a {@code texOffs} u. */
    public static int u(int region) {
        return (region % ACROSS) * REGION;
    }

    /** Top edge of {@code region}, in pixels - a {@code texOffs} v. */
    public static int v(int region) {
        return (region / ACROSS) * REGION;
    }
}
