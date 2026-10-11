package com.example.horsegenetics.common.gear;

import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartGenerators;
import com.example.horsegenetics.common.parts.PartKind;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartShape;
import com.example.horsegenetics.common.parts.PartSheet;
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
