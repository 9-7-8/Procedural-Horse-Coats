package com.example.horsegenetics.common.gear;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Face;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>The table of everything a horse can wear that is drawn on the horse.</b>
 *
 * <p>A worn piece is a row here, not a render layer of its own (owner,
 * 2026-10-09: no new piece gets a bespoke layer or a flat repaint). A row is a
 * name, a {@link Kind}, how far it stands off the horse, and - for a lifted
 * piece - the {@link Region} of the horse's own mesh it covers. The region is
 * the whole description of the piece's shape: its mask texture is baked from it
 * ({@code HairMaskTool}, {@code :common:bakeHairMasks}), and the client cuts
 * that mask into a lifted, rimmed mesh on the bones the region lies on. So a
 * new soft piece is one row and a re-bake.
 *
 * <p>A lift under {@link WornRule#FLOOR} cannot be written down: the row's
 * constructor refuses it.
 *
 * <p>What a row does <b>not</b> say is when a horse wears it, or in what
 * colour. That is per-version state (a gear slot, a dye) and lives beside the
 * layer that draws the table.
 */
public final class WornPieces {

    /** How a piece gets its depth. */
    public enum Kind {
        /** A region of the horse's own mesh, lifted off the skin, with a rim round its painted outline. */
        LIFTED,
        /** Boxes built in code and hung on an anchor ({@link WornPart}). Drawn by the worn side of the attached parts, not from a mask. */
        BOXES
    }

    /** Which texels of the horse's sheet a lifted piece covers. */
    @FunctionalInterface
    public interface Region {
        /**
         * @param local the texel's place in its part, as {@link HorseSkinGeometry#local}
         *              gives it: three fractions, X toward the nose, Y upward, Z toward
         *              the horse's right
         */
        boolean covers(Part part, Face face, BodyPoint local);
    }

    /**
     * One row.
     *
     * @param mask   the file stem of the mask the region is baked to, or null for {@link Kind#BOXES}
     * @param region null for {@link Kind#BOXES}
     * @param debug  true for a piece no player can wear: it exists to prove something in a photo shoot
     */
    public record Piece(String name, Kind kind, float lift, String mask, Region region, boolean debug) {
        public Piece {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("a worn piece needs a name");
            }
            WornRule.lift(lift);
            boolean lifted = kind == Kind.LIFTED;
            if (lifted != (mask != null && !mask.isEmpty()) || lifted != (region != null)) {
                throw new IllegalArgumentException(name + ": a lifted piece has a mask and a region, a box-built one has neither");
            }
        }
    }

    /** The rescuing braid worked into the mane. The first row, and the piece that proved the builder. */
    public static final Piece BRAID_MANE = lifted("braid_mane", WornRule.FLOOR, whole(Part.MANE), false);

    /** The rescuing braid worked into the tail. */
    public static final Piece BRAID_TAIL = lifted("braid_tail", WornRule.FLOOR, whole(Part.TAIL), false);

    /**
     * Where a boot sits on a leg, as fractions of the leg's height from the
     * ground: clear of the hoof below and ending under the knee, so the piece has
     * a wall at both ends and the hoof is left bare.
     */
    static final double BOOT_FROM = 0.14;
    static final double BOOT_TO = 0.55;

    /**
     * Debug only: a boot on the near fore leg, to prove a lifted piece follows an
     * animated leg before boots are committed to as lifted pieces.
     */
    public static final Piece TEST_BOOT_FORE =
            lifted("test_boot_fore", WornRule.FLOOR, band(Part.LEFT_FRONT_LEG, BOOT_FROM, BOOT_TO), true);

    /** Debug only: the same boot on the near hind leg. */
    public static final Piece TEST_BOOT_HIND =
            lifted("test_boot_hind", WornRule.FLOOR, band(Part.LEFT_HIND_LEG, BOOT_FROM, BOOT_TO), true);

    /**
     * Debug only, and the first box-built row: a plain block round each foot, to
     * prove that boxes hung on a hoof anchor ride the leg before horse shoes are
     * built on that drawing. Its boxes are {@code parts.WornBlockGenerator}'s, and
     * {@link WornPart#testBlocks} hangs one on every hoof.
     */
    public static final Piece TEST_BLOCK = new Piece("test_block", Kind.BOXES, WornRule.FLOOR, null, null, true);

    private static final List<Piece> ALL =
            table(BRAID_MANE, BRAID_TAIL, TEST_BOOT_FORE, TEST_BOOT_HIND, TEST_BLOCK);

    private WornPieces() {
    }

    /** Every row, in the order they are drawn. */
    public static List<Piece> all() {
        return ALL;
    }

    /** The rows a mask is baked for. */
    public static List<Piece> lifted() {
        List<Piece> lifted = new ArrayList<>();
        for (Piece piece : ALL) {
            if (piece.kind() == Kind.LIFTED) {
                lifted.add(piece);
            }
        }
        return lifted;
    }

    /** A lifted row whose mask is named after it. */
    static Piece lifted(String name, float lift, Region region, boolean debug) {
        return new Piece(name, Kind.LIFTED, lift, name, region, debug);
    }

    /** Every texel of one part. */
    public static Region whole(Part part) {
        return (p, face, local) -> p == part;
    }

    /**
     * A band round one part, between two heights given as fractions of the
     * part from its low end. The part's top and bottom faces are left out: a
     * band is a sleeve, open at both ends.
     */
    public static Region band(Part part, double from, double to) {
        if (!(from >= 0 && to <= 1 && from < to)) {
            throw new IllegalArgumentException("a band runs between two fractions of the part, not " + from + " to " + to);
        }
        return (p, face, local) -> p == part && face.normal() != Axis.Y && local.y() >= from && local.y() < to;
    }

    /** Visits the texel of the adult sheet a piece's mask paints. */
    @FunctionalInterface
    public interface MaskVisitor {
        void visit(int px, int py);
    }

    /** Every texel of the adult horse's sheet that {@code piece} covers; none for a box-built piece. */
    public static void forEachTexel(Piece piece, MaskVisitor visitor) {
        if (piece.region() == null) {
            return;
        }
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (piece.region().covers(part, face, HorseSkinGeometry.local(Skin.ADULT, part, point))) {
                visitor.visit(px, py);
            }
        });
    }

    /** The rows as a list, refusing two that share a name or a mask: the second would silently draw as the first. */
    static List<Piece> table(Piece... rows) {
        Set<String> names = new HashSet<>();
        Set<String> masks = new HashSet<>();
        List<Piece> table = new ArrayList<>();
        for (Piece row : rows) {
            if (!names.add(row.name())) {
                throw new IllegalArgumentException("two worn pieces are named " + row.name());
            }
            if (row.mask() != null && !masks.add(row.mask())) {
                throw new IllegalArgumentException("two worn pieces are painted by " + row.mask());
            }
            table.add(row);
        }
        return Collections.unmodifiableList(table);
    }
}
