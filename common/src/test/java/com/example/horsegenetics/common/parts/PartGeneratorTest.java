package com.example.horsegenetics.common.parts;

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
 * <b>Every part mesh the mod can bake is well formed, and there are not many of
 * them.</b>
 *
 * <p>The client bakes a {@code ModelPart} tree straight out of
 * {@link PartGenerators} in one pass, parents before children, so a generator
 * that emitted a child before its parent would throw in the renderer rather than
 * here - at which point the failure is a crash report from a player who bred a
 * horse. Everything a bake assumes is asserted here instead, over the whole
 * finite set of shapes, with no game running.
 *
 * <p>The "not many of them" half matters as much as the "well formed" half. The
 * cost of parts on a herd is bounded by <i>structure</i> rather than by a budget
 * somebody remembers to enforce, and that only holds while the shape key stays
 * coarse - so the size of {@link PartGenerators#allShapes()} is pinned.
 */
class PartGeneratorTest {

    /**
     * Thinnest box the pipeline may produce, in model units. Under about half a
     * unit a box is narrower than a texel on the {@link PartSheet} and shimmers as
     * the camera moves; {@link HornGenerator} has its own floor above this, and
     * this is the line no generator may ever cross.
     */
    private static final float MIN_GIRTH = 0.5f;

    @Test
    void everyShapeBuildsWithParentsBeforeChildren() {
        for (PartShape shape : PartGenerators.allShapes()) {
            List<PartNode> nodes = PartGenerators.build(shape);
            assertFalse(nodes.isEmpty(), shape + " built no boxes");
            int roots = 0;
            for (int i = 0; i < nodes.size(); i++) {
                PartNode node = nodes.get(i);
                if (node.isRoot()) {
                    roots++;
                } else {
                    assertTrue(node.parent() < i,
                            shape + " box " + i + " hangs off box " + node.parent()
                                    + ", which has not been built yet");
                }
                assertTrue(node.len() > 0f, shape + " box " + i + " has no length");
                assertTrue(node.girth() >= MIN_GIRTH,
                        shape + " box " + i + " is " + node.girth() + " units thick");
                assertTrue(node.t() >= 0f && node.t() <= 1f,
                        shape + " box " + i + " sits at t=" + node.t() + " along its parent");
                assertTrue(node.tex() >= 0 && node.tex() < PartSheet.CAPACITY,
                        shape + " box " + i + " samples region " + node.tex()
                                + ", which is off the sheet");
            }
            assertEquals(1, roots, shape + " has " + roots + " roots; a part is one tree");
        }
    }

    @Test
    void buildingTwiceGivesTheSameMesh() {
        for (PartShape shape : PartGenerators.allShapes()) {
            assertEquals(PartGenerators.build(shape), PartGenerators.build(shape),
                    shape + " is not deterministic - the client caches by shape and would "
                            + "hand two horses different meshes");
        }
    }

    /**
     * <b>The bound on cost.</b> Every part mesh this mod can ever bake, on any
     * machine, for any number of horses. If this number grows, it is because a shape
     * key gained an axis, and that is a performance decision rather than a detail -
     * which is what this assertion is for.
     *
     * <p>The antlers moved it once, deliberately: two sides, five habits, sixteen
     * racks and five size classes is eight hundred meshes, against the horn's
     * sixty-four. The bound is on <b>boxes</b> rather than meshes now, because that
     * is what memory costs - and the cache fills lazily, so it holds only racks
     * somebody bred. A world with every one of them in view is the case for an
     * eviction policy, and it is a measurement nobody has made.
     */
    @Test
    void theWholeSetOfMeshesIsSmall() {
        List<PartShape> all = PartGenerators.allShapes();
        int expected = 0;
        for (PartKind kind : PartKind.values()) {
            expected += kind.styles() * kind.sizeBuckets();
        }
        assertEquals(expected, all.size());
        long boxes = 0;
        for (PartShape shape : all) {
            boxes += PartGenerators.build(shape).size();
        }
        assertTrue(boxes <= 32_000, "every mesh the part cache can hold is " + boxes
                + " boxes; past this it wants a real eviction policy, not a bigger number here");
    }

    /**
     * No box carries both a bend and a roll - see {@link HornGenerator} on why a
     * segment that does turns the chain into a corkscrew. This is the invariant
     * that is easy to break by "tidying" the generator and impossible to notice
     * without looking at a horse.
     */
    @Test
    void noBoxBendsAndRollsAtOnce() {
        for (PartShape shape : PartGenerators.allShapes()) {
            for (PartNode node : PartGenerators.build(shape)) {
                assertFalse(node.rx() != 0f && node.ry() != 0f,
                        shape + " box turns about x and y at once, so the chain will coil");
            }
        }
    }

    @Test
    void aHornTapersAndWearsItsTipTexture() {
        for (int style = 0; style < HornGenerator.STYLES; style++) {
            List<PartNode> nodes = PartGenerators.build(
                    new PartShape(PartKind.HORN, style, PartShape.SIZE_BUCKETS - 1));
            assertTrue(nodes.size() > 2);
            assertTrue(nodes.size() <= HornGenerator.MAX_SEGMENTS,
                    "a horn is " + nodes.size() + " boxes; the cap is "
                            + HornGenerator.MAX_SEGMENTS);
            for (int i = 1; i < nodes.size(); i++) {
                assertTrue(nodes.get(i).girth() <= nodes.get(i - 1).girth(),
                        "box " + i + " is fatter than the one below it");
            }
            assertEquals(PartSheet.HORN_TIP, nodes.get(nodes.size() - 1).tex());
            assertEquals(PartSheet.HORN, nodes.get(0).tex());
        }
    }

    /** A longer horn is made of more boxes, so the boxes stay a sane size at both ends. */
    @Test
    void segmentCountRisesWithLength() {
        int shortest = PartGenerators.build(new PartShape(PartKind.HORN, 0, 0)).size();
        int longest = PartGenerators.build(
                new PartShape(PartKind.HORN, 0, PartShape.SIZE_BUCKETS - 1)).size();
        assertTrue(longest > shortest,
                "a narwhal horn (" + longest + " boxes) should have more segments than a nub ("
                        + shortest + ")");
    }

    // ------------------------------------------------------------------
    // PartShape - the bucketing, and the seam it has to close
    // ------------------------------------------------------------------

    /**
     * <b>The stretch closes the seam between buckets.</b> A horn's length is
     * continuous, the meshes are not, and this is the arithmetic that hides the
     * join: for any position on the ladder, the bucket's nominal length times the
     * stretch is the length that position asked for, exactly.
     */
    @Test
    void stretchLandsEveryPositionOnItsExactLength() {
        for (int i = 0; i <= 200; i++) {
            double position = i / 200.0;
            PartShape shape = PartShape.of(PartKind.HORN, 0, position);
            float drawn = shape.nominalLength() * shape.stretchTo(position);
            assertEquals(HornSize.lengthFor(position), drawn, 1e-3,
                    "position " + position + " draws the wrong length");
        }
    }

    /**
     * And it never has to stretch far enough to distort the taper. Sixteen
     * geometric buckets put neighbours under 7% apart; anything much past that
     * would read as a squashed horn rather than a shorter one.
     */
    @Test
    void theStretchIsAlwaysSmall() {
        for (int i = 0; i <= 200; i++) {
            double position = i / 200.0;
            float stretch = PartShape.of(PartKind.HORN, 0, position).stretchTo(position);
            assertTrue(stretch > 0.93f && stretch < 1.07f,
                    "position " + position + " needs a " + stretch + "x stretch");
        }
    }

    /**
     * An epigenetic value may legally sit eight design spans outside its range, so
     * a drifted length arrives here as something like -4 or 9. It clamps; it does
     * not wrap to a nub or extrapolate to a horn a chunk long.
     */
    @Test
    void aDriftedLengthClampsRatherThanWrapping() {
        PartShape lowest = new PartShape(PartKind.HORN, 0, 0);
        PartShape highest = new PartShape(PartKind.HORN, 0, PartShape.SIZE_BUCKETS - 1);
        assertEquals(lowest, PartShape.of(PartKind.HORN, 0, -8.0));
        assertEquals(highest, PartShape.of(PartKind.HORN, 0, 9.0));
        assertEquals(highest, PartShape.of(PartKind.HORN, 0, 1.0),
                "exactly 1.0 must land in the last bucket, not one past it");
        assertEquals(HornSize.NARWHAL.length, highest.nominalLength() * highest.stretchTo(4.0), 1e-3);
        assertEquals(HornSize.NUB.length, lowest.nominalLength() * lowest.stretchTo(-4.0), 1e-3);
    }

    @Test
    void anImpossibleShapeIsRefusedRatherThanDrawnWrong() {
        assertThrows(IllegalArgumentException.class,
                () -> new PartShape(PartKind.HORN, HornGenerator.STYLES, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PartShape(PartKind.HORN, 0, PartShape.SIZE_BUCKETS));
        assertThrows(IllegalArgumentException.class,
                () -> new PartShape(PartKind.ANTLER_LEFT, 0, AntlerSize.classes()));
        assertThrows(IllegalArgumentException.class, () -> new PartShape(PartKind.HORN, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new PartShape(null, 0, 0));
        // of() is the forgiving door - it is fed epigenetic numbers - and clamps
        // the style the same way it clamps the length.
        assertEquals(HornGenerator.STYLES - 1, PartShape.of(PartKind.HORN, 99, 0.5).style());
    }

    // ------------------------------------------------------------------
    // The enums, and the sheet they share
    // ------------------------------------------------------------------

    /**
     * Every kind has an anchor, a sheet region on the sheet, and at least one
     * style. A kind that resolved to none of those would be a gene granting a part
     * that cannot be drawn - which is the failure this whole package is arranged to
     * make impossible.
     */
    @Test
    void everyKindIsCompletelyDeclared() {
        for (PartKind kind : PartKind.values()) {
            assertSame(kind.anchor(), kind.anchor(), kind + " has no anchor");
            assertTrue(kind.styles() >= 1, kind + " has no styles");
            assertTrue(kind.texture() >= 0 && kind.texture() < PartSheet.CAPACITY,
                    kind + " samples region " + kind.texture() + ", which is off the sheet");
        }
    }

    @Test
    void theSheetGridAddsUp() {
        assertEquals(0, PartSheet.SIZE % PartSheet.REGION);
        assertEquals(PartSheet.ACROSS * PartSheet.ACROSS, PartSheet.CAPACITY);
        assertEquals(0, PartSheet.u(PartSheet.HORN));
        assertEquals(0, PartSheet.v(PartSheet.HORN));
        assertEquals(PartSheet.REGION, PartSheet.u(PartSheet.HORN_TIP));
        assertEquals(0, PartSheet.v(PartSheet.HORN_TIP));
        // Last region of the first row wraps to the second.
        assertEquals(0, PartSheet.u(PartSheet.ACROSS));
        assertEquals(PartSheet.REGION, PartSheet.v(PartSheet.ACROSS));
    }

    // ------------------------------------------------------------------
    // HornSize - the names, which are what the wiki and the horse screen say
    // ------------------------------------------------------------------

    @Test
    void theSizeLadderIsMonotonicAndItsNamesRoundTrip() {
        float previous = -1f;
        for (int i = 0; i <= 100; i++) {
            float length = HornSize.lengthFor(i / 100.0);
            assertTrue(length > previous, "the ladder went backwards at " + i);
            previous = length;
        }
        assertEquals(HornSize.NUB.length, HornSize.lengthFor(0.0), 1e-4);
        assertEquals(HornSize.NARWHAL.length, HornSize.lengthFor(1.0), 1e-3);
        for (HornSize size : HornSize.values()) {
            assertSame(size, HornSize.of(size.length),
                    size + " does not name its own length");
            assertFalse(size.label().isBlank());
        }
    }

    @Test
    void anAttachedPartRefusesAnAbsurdScale() {
        AttachedPart tiny = new AttachedPart(
                new PartShape(PartKind.HORN, 0, 4), 0f, -3f, 0f, 0xFFFFFFFF, 0xFFFFFFFF, false);
        assertTrue(tiny.stretch() > 0f, "a zero scale would draw a degenerate mesh");
        assertTrue(tiny.girth() > 0f, "a negative scale would draw the horn inside out");
        AttachedPart huge = new AttachedPart(
                new PartShape(PartKind.HORN, 0, 4), 500f, 500f, 0f, 0xFFFFFFFF, 0xFFFFFFFF, false);
        assertTrue(huge.stretch() < 10f, "a horn a chunk long is a hazard, not a triumph");
        assertEquals(PartKind.HORN, huge.kind());
    }

    @Test
    void aHornsDrawnLengthIsWhatTheLadderPromised() {
        for (int i = 0; i <= 50; i++) {
            double position = i / 50.0;
            AttachedPart part = AttachedPart.horn(position, 1.0, 0.0, 0);
            assertEquals(HornSize.lengthFor(position), part.length(), 1e-2,
                    "a horn asked for position " + position + " came out the wrong length");
        }
    }

    // ------------------------------------------------------------------
    // Antlers - the second kind, and the first with a count
    // ------------------------------------------------------------------

    private static List<PartShape> antlerShapes() {
        return PartGenerators.allShapes().stream().filter(s -> s.kind().antler()).toList();
    }

    @Test
    void noAntlerExceedsItsBoxCap() {
        for (PartShape shape : antlerShapes()) {
            int n = PartGenerators.build(shape).size();
            assertTrue(n <= AntlerGenerator.MAX_NODES,
                    shape + " is " + n + " boxes; the cap is " + AntlerGenerator.MAX_NODES);
        }
    }

    /**
     * <b>The tines are numbered 0..k-1 with nothing missing, and each one starts at a
     * box whose parent is outside it.</b> That is the whole of what the client's
     * "show the first k" relies on: a missing number is a tine that never shows, and
     * a group whose first box hangs off another box of the same group could not be
     * hidden as a unit.
     */
    @Test
    void tinesAreNumberedBaseToTipAndHideAsAUnit() {
        for (PartShape shape : antlerShapes()) {
            List<PartNode> nodes = PartGenerators.build(shape);
            int form = shape.style() / AntlerGenerator.VARIANTS;
            int expected = AntlerGenerator.maxTines(form, shape.size());
            Set<Integer> starts = new HashSet<>();
            for (PartNode node : nodes) {
                if (node.group() == PartNode.NO_GROUP) {
                    continue;
                }
                assertTrue(node.group() >= 0 && node.group() < expected,
                        shape + " has a box in group " + node.group() + " of " + expected);
                boolean starts_ = node.isRoot() || nodes.get(node.parent()).group() != node.group();
                if (starts_) {
                    assertTrue(starts.add(node.group()),
                            shape + " group " + node.group() + " starts twice, so it cannot hide as one");
                    assertEquals(PartNode.NO_GROUP, nodes.get(node.parent()).group(),
                            shape + " tine " + node.group() + " grows off another tine");
                }
            }
            assertEquals(expected, starts.size(), shape + " is missing a tine number");
        }
    }

    /** Every tine and the beam end on a point, which is what glow lights and crystal keeps solid. */
    @Test
    void everyAntlerChainEndsOnAPoint() {
        for (PartShape shape : antlerShapes()) {
            List<PartNode> nodes = PartGenerators.build(shape);
            boolean anyTip = false;
            for (int i = 0; i < nodes.size(); i++) {
                PartNode node = nodes.get(i);
                assertTrue(node.tex() == PartSheet.BONE || node.tex() == PartSheet.BONE_TIP
                                || node.tex() == PartSheet.BLOOM,
                        shape + " box " + i + " samples a horn region");
                if (node.tex() == PartSheet.BONE_TIP) {
                    anyTip = true;
                }
            }
            assertTrue(anyTip, shape + " has no point to glow");
        }
    }

    /** A symmetric rack is one antler and its mirror image, box for box. */
    @Test
    void theLeftAntlerIsTheRightOneMirrored() {
        for (PartShape right : antlerShapes()) {
            if (right.kind() != PartKind.ANTLER_RIGHT) {
                continue;
            }
            List<PartNode> r = PartGenerators.build(right);
            List<PartNode> l = PartGenerators.build(new PartShape(PartKind.ANTLER_LEFT, right.style(), right.size()));
            assertEquals(r.size(), l.size(), right + " grows a different number of boxes per side");
            for (int i = 0; i < r.size(); i++) {
                PartNode a = r.get(i);
                PartNode b = l.get(i);
                assertEquals(a.parent(), b.parent());
                assertEquals(a.len(), b.len());
                assertEquals(a.girth(), b.girth());
                assertEquals(a.rx(), b.rx());
                assertEquals(-a.rz(), b.rz(), 1e-6f, right + " box " + i + " does not mirror");
                assertEquals(a.group(), b.group());
                assertEquals(a.tex(), b.tex());
            }
        }
    }

    /** A bigger class is more of an antler - more boxes - not the same antler stretched. */
    @Test
    void aBiggerRackIsMoreBoxesNotAStretchedSmallOne() {
        for (int form = 0; form < AntlerGenerator.FORMS; form++) {
            int small = PartGenerators.build(new PartShape(PartKind.ANTLER_RIGHT,
                    form * AntlerGenerator.VARIANTS, 0)).size();
            int massive = PartGenerators.build(new PartShape(PartKind.ANTLER_RIGHT,
                    form * AntlerGenerator.VARIANTS, AntlerSize.classes() - 1)).size();
            assertTrue(massive > small, "habit " + form + ": massive " + massive + " vs yearling " + small);
        }
        assertEquals(0, AntlerGenerator.maxTines(AntlerGenerator.SPIKE, 0), "a yearling spike has no tines");
        assertEquals(AntlerGenerator.MAX_TINES,
                AntlerGenerator.maxTines(AntlerGenerator.FORK, AntlerSize.classes() - 1));
    }

    /** The sixteen racks of a habit are sixteen different meshes, not one repeated. */
    @Test
    void theRacksOfAHabitDiffer() {
        for (int form = 0; form < AntlerGenerator.FORMS; form++) {
            Set<List<PartNode>> seen = new HashSet<>();
            for (int v = 0; v < AntlerGenerator.VARIANTS; v++) {
                seen.add(PartGenerators.build(new PartShape(PartKind.ANTLER_RIGHT,
                        form * AntlerGenerator.VARIANTS + v, 2)));
            }
            assertEquals(AntlerGenerator.VARIANTS, seen.size(), "habit " + form + " repeats a rack");
        }
    }

    @Test
    void theAntlerLadderClampsAndNamesItsClasses() {
        assertEquals(AntlerSize.SHORTEST, AntlerSize.lengthFor(-3.0), 1e-4);
        assertEquals(AntlerSize.LONGEST, AntlerSize.lengthFor(9.0), 1e-3);
        assertSame(AntlerSize.YEARLING, AntlerSize.of(0.0));
        assertSame(AntlerSize.MASSIVE, AntlerSize.of(1.0));
        for (int i = 0; i <= 200; i++) {
            double position = i / 200.0;
            PartShape shape = PartShape.of(PartKind.ANTLER_LEFT, 0, position);
            assertEquals(AntlerSize.of(position).ordinal(), shape.size());
            float drawn = shape.nominalLength() * shape.stretchTo(position);
            assertEquals(AntlerSize.lengthFor(position), drawn, 1e-3,
                    "position " + position + " draws the wrong beam");
            float stretch = shape.stretchTo(position);
            assertTrue(stretch > 0.80f && stretch < 1.25f,
                    "position " + position + " needs a " + stretch + "x stretch inside its class");
        }
    }
}