package com.example.horsegenetics.common.coat.skin;

import com.example.horsegenetics.common.coat.skin.HorseSheetConverter.Mirror;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Patch;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sheet converter's self-checks (Appendix E of the undead treatment): it
 * reproduces the shipped white sheets to the pixel, its mirror is an involution
 * that swaps the side strips, every converted sheet on disk is what the manifest
 * says it should be, and a sheet for some other mesh is refused.
 */
class HorseSheetConverterTest {

    private static final Path ROOT = findRoot();

    private static Path findRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("common/sheet-sources/manifest.txt"))) {
            p = p.getParent();
        }
        if (p == null) {
            throw new IllegalStateException("no common/sheet-sources/manifest.txt above " + Path.of("").toAbsolutePath());
        }
        return p;
    }

    private static final Path HORSE = Path.of("common/src/main/resources/assets/horsegenetics/textures/entity/horse");

    @Test
    void straightModeReproducesTheShippedAdultWhiteSheet() throws IOException {
        int[] src = HorseSheetConvertTool.read(ROOT.resolve(HORSE).resolve("horse_white_vanilla64.png"));
        int[] shipped = HorseSheetConvertTool.read(ROOT.resolve(HORSE).resolve("horse_white.png"));
        assertArrayEquals(shipped, HorseSheetConverter.convert(src, 64, Skin.ADULT, Mirror.STRAIGHT).argb());
    }

    @Test
    void straightModeReproducesTheShippedBabyWhiteSheet() throws IOException {
        int[] src = HorseSheetConvertTool.read(ROOT.resolve(HORSE).resolve("horse_white_baby_vanilla64.png"));
        int[] shipped = HorseSheetConvertTool.read(ROOT.resolve(HORSE).resolve("horse_white_baby.png"));
        assertArrayEquals(shipped, HorseSheetConverter.convert(src, 64, Skin.BABY, Mirror.STRAIGHT).argb());
    }

    /** Every output on disk is exactly what converting its source gives - nobody hand-edited one. */
    @Test
    void everyManifestOutputIsCurrent() throws IOException {
        for (HorseSheetConvertTool.Entry e : HorseSheetConvertTool.manifest(ROOT)) {
            int[] src = HorseSheetConvertTool.read(e.source());
            int size = (int) Math.round(Math.sqrt(src.length));
            int[] want = HorseSheetConverter.convert(src, size, e.skin(), e.mirror()).argb();
            assertTrue(Arrays.equals(want, HorseSheetConvertTool.read(e.output())),
                    e.output() + " is stale: run ./gradlew :common:convertHorseSheets");
        }
    }

    @Test
    void theUndeadSourcesAreTheHorseMesh() throws IOException {
        for (String name : new String[]{"horse_skeleton", "horse_zombie"}) {
            int[] src = HorseSheetConvertTool.read(ROOT.resolve("common/sheet-sources/minecraft/" + name + ".png"));
            HorseSheetConverter.Result r = HorseSheetConverter.convert(src, 64, Skin.ADULT, Mirror.VANILLA);
            assertTrue(r.strayPixels() <= 2, name + ": " + r.strayPixels() + " pixels off the mesh");
        }
    }

    /** Each face of a 4x11x4 leg, column by column: the mirror maps the patch onto itself twice over. */
    @Test
    void theMirrorIsAnInvolutionThatSwapsTheSideStrips() {
        int w = 4, d = 4, ph = d + 11, pw = 2 * (d + w);
        for (int y = 0; y < ph; y++) {
            for (int x = 0; x < pw; x++) {
                int once = HorseSheetConverter.mirroredSource(x, y, w, d);
                if (once < 0) {
                    continue;
                }
                assertEquals(x, HorseSheetConverter.mirroredSource(once, y, w, d), "column " + x + " row " + y);
            }
        }
        // The first side strip's leftmost column shows the other strip's rightmost.
        assertEquals(2 * d + w - 1, HorseSheetConverter.mirroredSource(0, d, w, d));
        // The front face flips in place.
        assertEquals(d + w - 1, HorseSheetConverter.mirroredSource(d, d, w, d));
    }

    /** An asymmetry probe: a marker on the source leg patch lands mirrored on a left leg and straight on a right. */
    @Test
    void vanillaModeMirrorsOnlyTheLeftLegs() {
        int[] src = new int[64 * 64];
        Patch leg = HorseSkinGeometry.patches(Skin.ADULT).stream()
                .filter(p -> p.part() == HorseSkinGeometry.Part.LEFT_FRONT_LEG).findFirst().orElseThrow();
        // One opaque texel at the front face's left edge, first row of the lower band.
        int mx = leg.srcU() + leg.d(), my = leg.srcV() + leg.d();
        src[my * 64 + mx] = 0xFFFF0000;
        int[] out = HorseSheetConverter.convert(src, 64, Skin.ADULT, Mirror.VANILLA).argb();
        int n = 128;
        // Right front: unmirrored copy, marker at the same offset.
        Patch rf = HorseSkinGeometry.patches(Skin.ADULT).stream()
                .filter(p -> p.part() == HorseSkinGeometry.Part.RIGHT_FRONT_LEG).findFirst().orElseThrow();
        assertEquals(0xFFFF0000, out[(2 * (rf.dstV() + rf.d())) * n + 2 * (rf.dstU() + rf.d())]);
        // Left front: the marker is at the front face's right edge instead.
        int lx = 2 * (leg.dstU() + leg.d() + leg.w()) - 1, ly = 2 * (leg.dstV() + leg.d());
        assertEquals(0xFFFF0000, out[ly * n + lx]);
        assertEquals(0, out[ly * n + 2 * (leg.dstU() + leg.d())]);
    }

    @Test
    void aSheetForAnotherMeshIsRefused() {
        int[] src = new int[64 * 64];
        Arrays.fill(src, 0xFFFFFFFF);
        assertThrows(IllegalArgumentException.class,
                () -> HorseSheetConverter.convert(src, 64, Skin.ADULT, Mirror.VANILLA));
    }

    @Test
    void doublingAndHalvingAreTheOnlyKindsOfScale() {
        assertThrows(IllegalArgumentException.class,
                () -> HorseSheetConverter.convert(new int[96 * 96], 96, Skin.ADULT, Mirror.VANILLA));
        int[] big = new int[256 * 256];
        HorseSheetConverter.Result r = HorseSheetConverter.convert(big, 256, Skin.BABY, Mirror.VANILLA);
        assertEquals(128, r.size());
        assertTrue(r.warnings().stream().anyMatch(s -> s.contains("downscaled")));
    }
}
