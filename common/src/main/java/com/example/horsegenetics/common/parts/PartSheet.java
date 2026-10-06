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
     * Antler bone - a coarser, pitted grain than the horn's, running along the
     * beam. Opaque, and the region a crystalline antler draws see-through.
     */
    public static final int BONE = 2;

    /**
     * The last box of every antler chain - each tine's point and the beam's
     * end. Polished smoother and paler; the region a glowing antler lights and a
     * crystalline one keeps opaque.
     */
    public static final int BONE_TIP = 3;

    /**
     * Leaf, moss and blossom clumps on a blooming antler. A mottled grain with no
     * direction. Every antler mesh carries these boxes; only a blooming one draws
     * them - see {@code AntlerGenerator}.
     */
    public static final int BLOOM = 4;

    /**
     * Ram horn keratin - heavy cross-ridges ("growth rings") running across the
     * horn, which is what makes a ram's horn read as one. Opaque.
     */
    public static final int RAM_HORN = 5;

    /** The smoothed, polished last two boxes of a ram's horn. */
    public static final int RAM_TIP = 6;

    /**
     * Hair - fine fibres running along the box's length, in loose locks, softer and
     * finer than any keratin grain so a soft part (a goat beard, a ruff, feathering)
     * reads as hair rather than horn. Opaque. No part samples it yet: it was laid
     * down by the foundation unit ahead of the soft parts that will.
     */
    public static final int HAIR = 7;

    /**
     * Membrane - the skin stretched between a back sail's spines. Fine veins running
     * up the box's length over a smooth, pale ground. Opaque on the sheet, like every
     * region; a sail draws it see-through by tint ({@code PartKind.translucentRegions}),
     * so the same grain serves a membrane of any opacity.
     */
    public static final int MEMBRANE = 8;

    /**
     * Crystal - the shafts of a crystal growth on the back. Long clean facets running up
     * the box's length, a bright edge and a faint inner flaw, over a pale ground. Opaque
     * on the sheet, like every region; the crystal kind draws it see-through by tint
     * ({@code PartKind.translucentRegions}), and its points stay on {@link #BONE_TIP},
     * solid.
     */
    public static final int CRYSTAL = 9;

    /** Every region, as a mask for {@link #bit} tests. */
    public static final int ALL = -1;

    /** Every region but {@link #BLOOM}: the solid body of a part. */
    public static final int SOLID = ~(1 << BLOOM);

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

    /** {@code region} as a one-bit mask, for a submit that draws only some regions. */
    public static int bit(int region) {
        return 1 << region;
    }

    /** Does {@code mask} include {@code region}? */
    public static boolean in(int mask, int region) {
        return (mask & bit(region)) != 0;
    }
}
