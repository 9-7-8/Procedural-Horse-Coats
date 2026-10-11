package com.example.horsegenetics.common.gear;

import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartAnchor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>One box-built piece on one horse, resolved</b>: which row of
 * {@link WornPieces} it is, the part to draw, and the anchor it hangs on.
 *
 * <p>This is the worn side of the attached parts. A grown part comes from the
 * genotype and a worn one from what the horse has on, and both are an
 * {@link AttachedPart} by the time they reach the one layer that draws them
 * ({@code AttachedPartLayer}), so a shoe and a horn cannot grow two renderers
 * with two opinions about draw order.
 *
 * <p>The anchor is here and not on the part's kind because gear is hung where its
 * slot is: one shoe mesh goes on four feet. A grown part's place is its kind's
 * ({@code PartKind.anchor()}), since a gene grows a horn in one place.
 *
 * <p>What this does <b>not</b> say is when a horse wears the piece. That is a gear
 * slot and an item, per-version state, and it lives beside the layer - as it does
 * for a lifted row.
 *
 * @param piece  the row; always {@link WornPieces.Kind#BOXES}
 * @param part   what is drawn; always a {@code worn()} kind
 * @param anchor where on the horse it hangs
 */
public record WornPart(WornPieces.Piece piece, AttachedPart part, PartAnchor anchor) {

    public WornPart {
        if (piece == null || part == null || anchor == null) {
            throw new IllegalArgumentException("a worn part needs a row, a part and an anchor");
        }
        if (piece.kind() != WornPieces.Kind.BOXES) {
            throw new IllegalArgumentException(piece.name() + " is a lifted piece: it is cut from its mask, not hung on an anchor");
        }
        if (!part.kind().worn()) {
            throw new IllegalArgumentException(part.kind() + " is grown, not worn: " + piece.name() + " cannot be drawn as one");
        }
    }

    /** The same part on each of {@code anchors}, in their order - a piece worn on more than one place. */
    public static List<WornPart> on(WornPieces.Piece piece, AttachedPart part, List<PartAnchor> anchors) {
        List<WornPart> out = new ArrayList<>(anchors.size());
        for (PartAnchor anchor : anchors) {
            out.add(new WornPart(piece, part, anchor));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Debug only: the test block on all four feet, in one colour
     * ({@link WornPieces#TEST_BLOCK}).
     *
     * @param tint opaque ARGB
     */
    public static List<WornPart> testBlocks(int tint) {
        return on(WornPieces.TEST_BLOCK, AttachedPart.testBlock(tint), PartAnchor.hooves());
    }
}
