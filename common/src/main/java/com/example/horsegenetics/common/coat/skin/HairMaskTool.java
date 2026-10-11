package com.example.horsegenetics.common.coat.skin;

import com.example.horsegenetics.common.gear.WornPieces;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * <b>Writes the mask every lifted worn piece is drawn through</b> - the
 * rescuing braid's mane and tail, and whatever else {@link WornPieces} lists.
 *
 * <p>A lifted piece is the horse's own mesh drawn again in a colour, and what
 * stops that from painting the entire animal is the texture: a sheet that is
 * opaque exactly where the piece is and transparent everywhere else. That is a
 * question about the mesh's texture layout, and {@link HorseSkinGeometry} is
 * the one thing in this repository that knows the answer - so each row's
 * {@link WornPieces.Region} is asked rather than guessed at with a paint program.
 *
 * <p><b>This is why it is a bake and not a drawn texture.</b> The braid's two
 * patches are at {@code texOffs(56, 36)} and {@code texOffs(42, 36)} today,
 * doubled onto the {@value HorseSkinGeometry#SHEET_SIZE}px sheet. Hand-painting
 * two rectangles at those coordinates would work, and would silently paint a
 * horse's neck the day somebody moves a cube - which is precisely the class of
 * drift the project keeps a table of re-bake rules about. Re-run it when the
 * mesh moves, or a row is added or changed; {@code WornPiecesTest} fails on a
 * checked-in mask that no longer matches its row.
 *
 * <p>The mask is <b>white</b>, because the colour comes from the piece at draw
 * time: {@code WornLayer} draws it with the piece's own tint, so one file
 * covers every colour a player can mix.
 *
 * <p>The foal mesh has no {@link HorseSkinGeometry.Part#MANE} at all, which is
 * the right answer rather than a gap - a foal wears no gear of any kind
 * ({@code HorseTackSlot.usableOn}), so no foal is ever drawn through these.
 */
public final class HairMaskTool {

    private HairMaskTool() {
    }

    /** What the layer tints. Full white, so a dye multiplies to exactly itself. */
    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: HairMaskTool <output directory>");
        }
        File dir = new File(args[0]);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("could not create " + dir);
        }
        for (WornPieces.Piece piece : WornPieces.lifted()) {
            write(dir, piece);
        }
    }

    private static void write(File dir, WornPieces.Piece piece) throws IOException {
        String name = piece.mask() + ".png";
        BufferedImage image = new BufferedImage(HorseSkinGeometry.SHEET_SIZE,
                HorseSkinGeometry.SHEET_SIZE, BufferedImage.TYPE_INT_ARGB);
        int[] painted = { 0 };
        WornPieces.forEachTexel(piece, (px, py) -> {
            image.setRGB(px, py, OPAQUE_WHITE);
            painted[0]++;
        });
        if (painted[0] == 0) {
            // A mask nothing draws through is a piece nobody can see, and it
            // would ship green: every test in the suite passes, the layer runs,
            // and the horse simply is not wearing it.
            throw new IOException(piece.name() + " covers no texel on the adult sheet - "
                    + "the mesh has moved and this tool has not been told");
        }
        File out = new File(dir, name);
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out + " (" + painted[0] + " texels)");
    }
}
