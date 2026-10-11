package com.example.horsegenetics.common.gear;

import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartGenerators;
import com.example.horsegenetics.common.parts.PartKind;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartShape;
import com.example.horsegenetics.common.parts.PartSheet;
import com.example.horsegenetics.common.parts.ShoeGenerator;
import com.example.horsegenetics.common.parts.WornBlockGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The worn side of the attached parts: a box-built row is hung on an anchor as
 * an ordinary attached part, there are four hoof anchors to hang one on, and the
 * debug block that proves it stands off the foot by the worn rule.
 */
class WornPartTest {

    private static final int BLUE = 0xFF2E6FD9;

    @Test
    @DisplayName("there are four hoof anchors, and the test block hangs once on each")
    void oneBlockOnEveryHoof() {
        List<PartAnchor> hooves = PartAnchor.hooves();
        assertEquals(List.of(PartAnchor.HOOF_FRONT_LEFT, PartAnchor.HOOF_FRONT_RIGHT,
                PartAnchor.HOOF_HIND_LEFT, PartAnchor.HOOF_HIND_RIGHT), hooves);

        List<WornPart> blocks = WornPart.testBlocks(BLUE);
        Set<PartAnchor> hung = new HashSet<>();
        for (WornPart block : blocks) {
            assertSame(WornPieces.TEST_BLOCK, block.piece());
            assertSame(PartKind.TEST_BLOCK, block.part().kind());
            assertEquals(BLUE, block.part().baseTint());
            assertFalse(block.part().twoTone(), "one colour is one draw");
            assertTrue(hung.add(block.anchor()), "two blocks on " + block.anchor());
        }
        assertEquals(new HashSet<>(hooves), hung);
    }

    @Test
    @DisplayName("only a box-built row is hung, and only a worn kind is hung on it")
    void aLiftedRowOrAGrownPartCannotBeHung() {
        AttachedPart block = AttachedPart.testBlock(BLUE);
        assertThrows(IllegalArgumentException.class,
                () -> new WornPart(WornPieces.BRAID_MANE, block, PartAnchor.HOOF_FRONT_LEFT));
        assertThrows(IllegalArgumentException.class,
                () -> new WornPart(WornPieces.TEST_BLOCK, AttachedPart.horn(0.5, 1.0, 0.0, 0),
                        PartAnchor.HOOF_FRONT_LEFT));
        assertThrows(IllegalArgumentException.class,
                () -> new WornPart(WornPieces.TEST_BLOCK, block, null));
    }

    @Test
    @DisplayName("the test block's row is box-built and debug, and the block stands its lift off the foot")
    void theBlockStandsOffTheFootByItsRowsLift() {
        WornPieces.Piece row = WornPieces.TEST_BLOCK;
        assertSame(WornPieces.Kind.BOXES, row.kind());
        assertTrue(row.debug(), "no player can wear the test block");
        assertTrue(WornPieces.all().contains(row));
        assertFalse(WornPieces.lifted().contains(row), "a box-built row has no mask to bake");
        assertEquals(row.lift(), WornRule.lift(WornBlockGenerator.PROUD));

        // The boxes, in the anchor's frame: x across the horse, y down, z along it.
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        float top = Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
        List<PartNode> nodes = PartGenerators.build(AttachedPart.testBlock(BLUE).shape());
        float[] x = new float[nodes.size()];
        float[] y = new float[nodes.size()];
        float[] z = new float[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            PartNode node = nodes.get(i);
            assertEquals(0f, Math.abs(node.rx()) + Math.abs(node.ry()) + Math.abs(node.rz()), "a block is square to the leg");
            boolean root = node.isRoot();
            PartNode parent = root ? null : nodes.get(node.parent());
            x[i] = (root ? 0f : x[node.parent()]) + node.ox();
            y[i] = (root ? 0f : y[node.parent()] - parent.len() * node.t()) + node.oy();
            z[i] = (root ? 0f : z[node.parent()]) + node.oz();
            minX = Math.min(minX, x[i] - node.width() / 2f);
            maxX = Math.max(maxX, x[i] + node.width() / 2f);
            minZ = Math.min(minZ, z[i] - node.girth() / 2f);
            maxZ = Math.max(maxZ, z[i] + node.girth() / 2f);
            top = Math.min(top, y[i] - node.len());
            bottom = Math.max(bottom, y[i]);
        }
        float half = WornBlockGenerator.HOOF / 2f;
        float lift = row.lift();
        assertEquals(-half - lift, minX, 1e-5f);
        assertEquals(half + lift, maxX, 1e-5f);
        assertEquals(-half - lift, minZ, 1e-5f);
        assertEquals(half + lift, maxZ, 1e-5f);
        assertEquals(lift, bottom, 1e-5f, "under the sole by the lift");
        assertEquals(-WornBlockGenerator.HEIGHT, top, 1e-5f);
    }

    /** {min x, max x, min y, max y, min z, max z} of every box, in the anchor's frame; a box is square to it or a quarter turned about y. */
    private static float[][] boxes(List<PartNode> nodes) {
        float[][] out = new float[nodes.size()][];
        float[] x = new float[nodes.size()];
        float[] y = new float[nodes.size()];
        float[] z = new float[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            PartNode node = nodes.get(i);
            boolean root = node.isRoot();
            assertTrue(root || (nodes.get(node.parent()).ry() == 0f && node.t() == 0f), "hung at the base of an unturned box");
            assertEquals(0f, Math.abs(node.rx()) + Math.abs(node.rz()));
            boolean turned = Math.abs(Math.abs(node.ry()) - (float) (Math.PI / 2)) < 1e-6f;
            assertTrue(turned || node.ry() == 0f);
            x[i] = (root ? 0f : x[node.parent()]) + node.ox();
            y[i] = (root ? 0f : y[node.parent()]) + node.oy();
            z[i] = (root ? 0f : z[node.parent()]) + node.oz();
            float acrossX = (turned ? node.girth() : node.width()) / 2f;
            float alongZ = (turned ? node.width() : node.girth()) / 2f;
            out[i] = new float[] {x[i] - acrossX, x[i] + acrossX, y[i] - node.len(), y[i], z[i] - alongZ, z[i] + alongZ};
        }
        return out;
    }

    private static boolean covers(float[][] boxes, float px, float pz) {
        for (float[] b : boxes) {
            if (px > b[0] && px < b[1] && pz > b[4] && pz < b[5]) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("a shoe is a U under the foot: toe and sides, open at the heel, the frog bare, a bare edge on the wall")
    void aShoeIsAUOpenAtTheHeel() {
        WornPieces.Piece row = WornPieces.HORSE_SHOE;
        assertSame(WornPieces.Kind.BOXES, row.kind());
        assertFalse(row.debug());
        assertEquals(row.lift(), WornRule.lift(ShoeGenerator.PROUD));
        float[][] shoe = boxes(PartGenerators.build(AttachedPart.shoe(0xFFC8C8C8).shape()));
        float half = WornBlockGenerator.HOOF / 2f;
        float lift = row.lift();
        float minX = 9f, maxX = -9f, minZ = 9f, maxZ = -9f, top = 9f, bottom = -9f;
        for (float[] b : shoe) {
            minX = Math.min(minX, b[0]);
            maxX = Math.max(maxX, b[1]);
            top = Math.min(top, b[2]);
            bottom = Math.max(bottom, b[3]);
            minZ = Math.min(minZ, b[4]);
            maxZ = Math.max(maxZ, b[5]);
        }
        // Proud of the toe and both side walls by the lift, and under the sole by it.
        assertEquals(-half - lift, minX, 1e-5f);
        assertEquals(half + lift, maxX, 1e-5f);
        assertEquals(-half - lift, minZ, 1e-5f, "front is -z");
        assertEquals(lift, bottom, 1e-5f);
        // Nothing across the back: the shoe stops short of the heel wall.
        assertTrue(maxZ < half, "the shoe reaches the back wall at z " + maxZ);
        // Only the barest bit up the hoof wall.
        assertTrue(-top <= 0.25f + 1e-5f, "the shoe climbs " + -top + " up the wall");
        // The toe and both sides are shod; the frog and the heel between the bars are bare.
        assertTrue(covers(shoe, 0f, -half), "no toe bar");
        assertTrue(covers(shoe, -half, 0f) && covers(shoe, half, 0f), "a side bar is missing");
        assertFalse(covers(shoe, 0f, 0f), "the frog is covered");
        assertFalse(covers(shoe, 0f, half - 0.3f), "the heel is closed");
        // The bars do not overlap each other anywhere (they would flicker).
        for (int i = 0; i < shoe.length; i++) {
            for (int j = i + 1; j < shoe.length; j++) {
                boolean apart = shoe[i][1] <= shoe[j][0] + 1e-5f || shoe[j][1] <= shoe[i][0] + 1e-5f
                        || shoe[i][5] <= shoe[j][4] + 1e-5f || shoe[j][5] <= shoe[i][4] + 1e-5f;
                assertTrue(apart, "bars " + i + " and " + j + " overlap");
            }
        }
    }

    @Test
    @DisplayName("a worn kind is never a foal's, and every box of one unwraps inside its sheet region")
    void aWornKindKeepsToItsRegion() {
        int worn = 0;
        for (PartShape shape : PartGenerators.allShapes()) {
            if (!shape.kind().worn()) {
                continue;
            }
            worn++;
            assertFalse(shape.kind().showsOnFoal(), shape.kind() + ": a foal wears no gear");
            for (PartNode node : PartGenerators.build(shape)) {
                assertTrue(2f * (node.width() + node.girth()) <= PartSheet.REGION,
                        shape + " unwraps " + 2f * (node.width() + node.girth()) + " texels wide");
                assertTrue(node.girth() + node.len() <= PartSheet.REGION,
                        shape + " unwraps " + (node.girth() + node.len()) + " texels tall");
            }
        }
        assertTrue(worn > 0, "no worn kind was walked");
    }
}
