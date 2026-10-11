package com.example.horsegenetics.common.gear;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The table of worn pieces: every row obeys the floor, every row is told apart
 * from every other, and every lifted row's mask is in the jar's assets and is
 * the one its region bakes.
 */
class WornPiecesTest {

    /** Where the baked masks are checked in ({@code :common:bakeHairMasks} writes them there). */
    private static final String MASKS = "neoforge-26.1.2/src/main/resources/assets/horsegenetics/textures/entity/horse";

    @Test
    @DisplayName("every row stands at least the floor off the horse, and a row under it cannot be written")
    void everyRowObeysTheFloor() {
        assertFalse(WornPieces.all().isEmpty());
        for (WornPieces.Piece piece : WornPieces.all()) {
            assertTrue(piece.lift() >= WornRule.FLOOR, piece.name());
        }
        assertThrows(IllegalArgumentException.class,
                () -> WornPieces.lifted("thin", 0.2f, WornPieces.whole(Part.MANE), false));
        assertThrows(IllegalArgumentException.class,
                () -> new WornPieces.Piece("thin_box", WornPieces.Kind.BOXES, 0f, null, null, false));
    }

    @Test
    @DisplayName("no two rows share a name or a mask, and the table refuses a second one")
    void noTwoRowsShareAName() {
        Set<String> names = new HashSet<>();
        Set<String> masks = new HashSet<>();
        for (WornPieces.Piece piece : WornPieces.all()) {
            assertTrue(names.add(piece.name()), piece.name());
            if (piece.mask() != null) {
                assertTrue(masks.add(piece.mask()), piece.mask());
            }
        }
        WornPieces.Piece a = WornPieces.lifted("a", 0.25f, WornPieces.whole(Part.MANE), false);
        WornPieces.Piece twin = WornPieces.lifted("a", 0.5f, WornPieces.whole(Part.TAIL), false);
        assertThrows(IllegalArgumentException.class, () -> WornPieces.table(a, twin));
    }

    @Test
    @DisplayName("a lifted row has a mask and a region, a box-built row has neither")
    void aRowIsOneKindOrTheOther() {
        for (WornPieces.Piece piece : WornPieces.all()) {
            boolean lifted = piece.kind() == WornPieces.Kind.LIFTED;
            assertEquals(lifted, piece.mask() != null, piece.name());
            assertEquals(lifted, piece.region() != null, piece.name());
        }
        assertThrows(IllegalArgumentException.class,
                () -> new WornPieces.Piece("no_mask", WornPieces.Kind.LIFTED, 0.25f, null, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new WornPieces.Piece("masked_box", WornPieces.Kind.BOXES, 0.25f, "m",
                        WornPieces.whole(Part.MANE), false));
        assertNotNull(new WornPieces.Piece("box", WornPieces.Kind.BOXES, 0.5f, null, null, false));
    }

    @Test
    @DisplayName("every lifted row's mask is checked in and paints exactly the texels its region covers")
    void everyMaskExistsAndIsTheOneItsRowBakes() throws IOException {
        File dir = masks();
        for (WornPieces.Piece piece : WornPieces.lifted()) {
            Set<Integer> wanted = new HashSet<>();
            WornPieces.forEachTexel(piece, (px, py) -> wanted.add(py * HorseSkinGeometry.SHEET_SIZE + px));
            assertFalse(wanted.isEmpty(), piece.name() + " covers no texel of the adult sheet");

            File file = new File(dir, piece.mask() + ".png");
            assertTrue(file.isFile(), piece.name() + " names a mask that is not checked in: " + file
                    + " (run :common:bakeHairMasks)");
            BufferedImage image = ImageIO.read(file);
            assertEquals(HorseSkinGeometry.SHEET_SIZE, image.getWidth(), piece.name());
            assertEquals(HorseSkinGeometry.SHEET_SIZE, image.getHeight(), piece.name());
            Set<Integer> painted = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) != 0) {
                        painted.add(y * HorseSkinGeometry.SHEET_SIZE + x);
                    }
                }
            }
            assertEquals(wanted, painted, piece.name() + "'s mask is stale (run :common:bakeHairMasks)");
        }
    }

    @Test
    @DisplayName("the test boot is a sleeve: it leaves the hoof and the top of the leg bare, and wraps all four sides")
    void theBootIsASleeveClearOfTheHoof() {
        for (WornPieces.Piece boot : new WornPieces.Piece[] {WornPieces.TEST_BOOT_FORE, WornPieces.TEST_BOOT_HIND}) {
            assertTrue(boot.debug(), boot.name());
            Set<HorseSkinGeometry.Face> faces = new HashSet<>();
            double[] low = {1};
            double[] high = {0};
            HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
                HorseSkinGeometry.BodyPoint local = HorseSkinGeometry.local(Skin.ADULT, part, point);
                if (boot.region().covers(part, face, local)) {
                    faces.add(face);
                    low[0] = Math.min(low[0], local.y());
                    high[0] = Math.max(high[0], local.y());
                }
            });
            assertEquals(Set.of(HorseSkinGeometry.Face.NOSE, HorseSkinGeometry.Face.TAIL,
                    HorseSkinGeometry.Face.LEFT, HorseSkinGeometry.Face.RIGHT), faces, boot.name());
            assertTrue(low[0] >= WornPieces.BOOT_FROM && low[0] > 0.1, boot.name() + " reaches the hoof: " + low[0]);
            assertTrue(high[0] < WornPieces.BOOT_TO && high[0] < 0.6, boot.name() + " reaches the knee: " + high[0]);
        }
        assertFalse(WornPieces.BRAID_MANE.debug());
        assertFalse(WornPieces.BRAID_TAIL.debug());
    }

    /** The masks' folder, found from wherever Gradle runs the test (the module's own directory). */
    private static File masks() {
        File at = new File("").getAbsoluteFile();
        while (at != null) {
            File dir = new File(at, MASKS);
            if (dir.isDirectory()) {
                return dir;
            }
            at = at.getParentFile();
        }
        throw new IllegalStateException("no " + MASKS + " above " + new File("").getAbsolutePath());
    }
}
