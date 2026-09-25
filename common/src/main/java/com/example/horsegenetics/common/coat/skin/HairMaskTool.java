package com.example.horsegenetics.common.coat.skin;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * <b>Writes the mane and tail masks the rescuing braid is drawn through.</b>
 *
 * <p>A braid is a second pass of the whole horse model in a colour, and what
 * stops that from painting the entire animal is the texture: a sheet that is
 * opaque exactly where the mane or the tail is and transparent everywhere else.
 * That is a question about the mesh's texture layout, and {@link
 * HorseSkinGeometry} is the one thing in this repository that knows the answer -
 * so it is asked rather than guessed at with a paint program.
 *
 * <p><b>This is why it is a bake and not a drawn texture.</b> The two patches
 * are at {@code texOffs(56, 36)} and {@code texOffs(42, 36)} today, doubled onto
 * the {@value HorseSkinGeometry#SHEET_SIZE}px sheet. Hand-painting two
 * rectangles at those coordinates would work, and would silently paint a horse's
 * neck the day somebody moves a cube - which is precisely the class of drift the
 * project keeps a table of re-bake rules about. Re-run it when the mesh moves.
 *
 * <p>The mask is <b>white</b>, because the colour comes from the dye at draw
 * time: {@code BraidLayer} submits the model with the braid's own tint, so one
 * pair of files covers every colour a player can mix.
 *
 * <p>The foal mesh has no {@link Part#MANE} at all, which is the right answer
 * rather than a gap - a foal wears no gear of any kind
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
        write(dir, Part.MANE, "braid_mane.png");
        write(dir, Part.TAIL, "braid_tail.png");
    }

    private static void write(File dir, Part part, String name) throws IOException {
        BufferedImage image = new BufferedImage(HorseSkinGeometry.SHEET_SIZE,
                HorseSkinGeometry.SHEET_SIZE, BufferedImage.TYPE_INT_ARGB);
        int[] painted = { 0 };
        HorseSkinGeometry.forEachTexel(Skin.ADULT, part, (px, py, p, face, point) -> {
            image.setRGB(px, py, OPAQUE_WHITE);
            painted[0]++;
        });
        if (painted[0] == 0) {
            // A mask nothing draws through is a braid nobody can see, and it
            // would ship green: every test in the suite passes, the layer runs,
            // and the horse simply has no braid on it.
            throw new IOException(part + " covers no texel on the adult sheet - "
                    + "the mesh has moved and this tool has not been told");
        }
        File out = new File(dir, name);
        ImageIO.write(image, "png", out);
        System.out.println("wrote " + out + " (" + painted[0] + " texels)");
    }
}
