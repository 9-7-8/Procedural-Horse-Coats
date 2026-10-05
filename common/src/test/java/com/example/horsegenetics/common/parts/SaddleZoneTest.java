package com.example.horsegenetics.common.parts;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The saddle zone on a row along the back: when it applies, which spines it hides,
 * and the numbering both it and the count rely on.
 */
class SaddleZoneTest {

    /**
     * The client's range, re-derived here by value because a common/ test cannot see
     * the client: the vanilla saddle covers body {@code z -9.5..0.5}, the
     * {@code SPINE} anchor sits at body {@code z -11}, and a spine within a unit of the
     * saddle counts as under it ({@code AttachedPartLayer.SADDLE_ZONE_FROM/TO}).
     */
    private static final float FROM = -9.5f - 1f + 11f;
    private static final float TO = 0.5f + 1f + 11f;

    @Test
    void theZoneAppliesToADrawnSaddleOrARiderAndNothingElse() {
        assertFalse(SaddleZone.covers(false, false), "a bare horse nobody rides shows the whole row");
        assertTrue(SaddleZone.covers(true, false), "a drawn saddle");
        assertTrue(SaddleZone.covers(false, true), "a phantom saddle is not drawn, but its rider sits there");
        assertTrue(SaddleZone.covers(true, true));
    }

    /** A foal wears no body part, so it never reaches the zone. */
    @Test
    void aFoalWearsNoSpines() {
        assertFalse(PartKind.SPINES.showsOnFoal());
    }

    /**
     * Under the saddle, a contiguous run from the middle of the row is hidden - never
     * none and never all - and spines stay at the withers and at the croup.
     */
    @Test
    void theSaddleHidesAMiddleRunOfEveryRow() {
        for (PartShape shape : PartGenerators.allShapes()) {
            if (!shape.kind().saddleZoned()) {
                continue;
            }
            int mask = SaddleZone.groupsWithin(PartGenerators.build(shape), FROM, TO);
            int all = (1 << DorsalSpineGenerator.MAX_SPINES) - 1;
            assertNotEquals(0, mask, shape + ": the saddle hides nothing");
            assertNotEquals(all, mask, shape + ": the saddle hides the whole row");
            assertFalse(SaddleZone.hides(mask, 0), shape + ": the spine at the withers is clear of the saddle");
            assertFalse(SaddleZone.hides(mask, DorsalSpineGenerator.MAX_SPINES - 1),
                    shape + ": the spine at the croup is clear of the saddle");
            int low = Integer.numberOfTrailingZeros(mask);
            int run = Integer.bitCount(mask);
            assertEquals(((1 << run) - 1) << low, mask, shape + ": the hidden spines are not one run");
        }
    }

    /** It hides by where a spine roots - the spines the test computes from the row's own spacing. */
    @Test
    void theHiddenSpinesAreTheOnesRootedUnderTheSaddle() {
        List<PartNode> row = PartGenerators.build(new PartShape(PartKind.SPINES, 0, 0));
        int mask = SaddleZone.groupsWithin(row, FROM, TO);
        for (int i = 0; i < DorsalSpineGenerator.MAX_SPINES; i++) {
            float z = DorsalSpineGenerator.rootZ(i);
            assertEquals(z >= FROM && z <= TO, SaddleZone.hides(mask, i), "spine " + i + " at z " + z);
        }
        assertEquals(0, SaddleZone.groupsWithin(row, 100f, 200f), "a range off the row hides nothing");
    }

    /**
     * The numbering the count and the zone rely on: every box of a row belongs to a
     * spine, the spines are 0..MAX-1 with none missing, each is one root chain, and
     * they run front to back - so "the first k" is the k nearest the withers.
     */
    @Test
    void everySpineIsOneNumberedChainFrontToBack() {
        for (PartShape shape : PartGenerators.allShapes()) {
            if (!shape.kind().scalesPerElement()) {
                continue;
            }
            List<PartNode> nodes = PartGenerators.build(shape);
            float[] rootZ = new float[DorsalSpineGenerator.MAX_SPINES];
            int[] roots = new int[DorsalSpineGenerator.MAX_SPINES];
            for (PartNode node : nodes) {
                int g = node.group();
                assertTrue(g >= 0 && g < DorsalSpineGenerator.MAX_SPINES, shape + ": a box in group " + g);
                if (node.isRoot()) {
                    roots[g]++;
                    rootZ[g] = node.oz();
                } else {
                    assertEquals(g, nodes.get(node.parent()).group(), shape + ": a spine hangs off another");
                }
            }
            for (int g = 0; g < roots.length; g++) {
                assertEquals(1, roots[g], shape + ": spine " + g + " has " + roots[g] + " roots");
                if (g > 0) {
                    assertTrue(rootZ[g] > rootZ[g - 1], shape + ": spine " + g + " is not behind " + (g - 1));
                }
            }
        }
        assertTrue(DorsalSpineGenerator.MAX_SPINES < SaddleZone.MASK_BITS,
                "the saddle zone is an int mask of spines");
    }

    @Test
    void aMaskNamesOnlyTheGroupsItCanHold() {
        assertFalse(SaddleZone.hides(-1, SaddleZone.MASK_BITS), "a group past the mask is never hidden");
        assertFalse(SaddleZone.hides(-1, -1));
        assertTrue(SaddleZone.hides(1 << 5, 5));
        assertFalse(SaddleZone.hides(1 << 5, 4));
    }
}
