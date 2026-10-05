package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartGenerators;
import com.example.horsegenetics.common.parts.PartKind;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartShape;
import com.example.horsegenetics.common.parts.PartSheet;
import com.example.horsegenetics.common.parts.PlateGenerator;
import com.example.horsegenetics.common.parts.PlateSize;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shoulder and hip plates and their colour locus - the third of the body parts. */
class BodyPlatesGeneTest {

    private static final BodyPlatesGene GENE = Genes.BODY_PLATES;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static List<AttachedPart> plates(List<AttachedPart> parts) {
        return parts.stream().filter(p -> p.kind().plates()).toList();
    }

    private static List<PartShape> plateShapes() {
        return PartGenerators.allShapes().stream().filter(s -> s.kind().plates()).toList();
    }

    /** Dominant: one copy grows both sides, on mares and stallions alike; none shows nothing. */
    @Test
    void oneCopyGrowsBothSidesOnEitherSex() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            assertTrue(GrownParts.of(horse(sex, "body_plates", "n/n"), epi).isEmpty(), sex + " wild");
            for (String pair : List.of("Plt/n", "Plt/Plt")) {
                List<AttachedPart> parts = GrownParts.of(horse(sex, "body_plates", pair), epi);
                assertEquals(2, parts.size(), sex + " " + pair);
                assertSame(PartKind.PLATES_RIGHT, parts.get(0).kind());
                assertSame(PartKind.PLATES_LEFT, parts.get(1).kind());
                // The two sides always match.
                assertEquals(parts.get(0).shape().style(), parts.get(1).shape().style());
                assertEquals(parts.get(0).shape().size(), parts.get(1).shape().size());
                assertEquals(parts.get(0).shown(), parts.get(1).shown());
                assertEquals(parts.get(0).stretch(), parts.get(1).stretch());
            }
        }
        assertNotEquals(GENE.expressionOf(Genotype.parse(Codes.of("body_plates", "Plt/n")).pair(GENE)).id(),
                GENE.expressionOf(Genotype.wildType().pair(GENE)).id(), "a carrier shows");
        assertFalse(GENE.affectsCoat());
        for (PartKind kind : List.of(PartKind.PLATES_RIGHT, PartKind.PLATES_LEFT)) {
            assertFalse(kind.showsOnFoal(), "a hard part comes with maturity");
            assertFalse(kind.saddleZoned(), "the clusters sit clear of the saddle");
            assertTrue(kind.scalesPerElement());
            assertEquals(0, kind.translucentRegions());
        }
        assertSame(PartAnchor.BODY_RIGHT, PartKind.PLATES_RIGHT.anchor());
        assertSame(PartAnchor.BODY_LEFT, PartKind.PLATES_LEFT.anchor());
    }

    /** Every number on the copy reaches the plates: size, count, form, overlap, spikiness. */
    @Test
    void everyEpigeneticNumberReachesThePlates() {
        Genotype gt = horse(Sex.FEMALE, "body_plates", "Plt/Plt");
        Set<Integer> sizes = new HashSet<>();
        Set<Integer> forms = new HashSet<>();
        Set<Integer> overlaps = new HashSet<>();
        Set<Integer> spikes = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> counts = new HashSet<>();
        for (long seed = 0; seed < 1500; seed++) {
            AttachedPart side = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            int style = side.shape().style();
            sizes.add(side.shape().size());
            forms.add(PlateGenerator.formOf(style));
            overlaps.add(PlateGenerator.overlapOf(style));
            spikes.add(PlateGenerator.spikinessOf(style));
            stretches.add(side.stretch());
            counts.add(side.shown());
            assertEquals(side.stretch(), side.girth(), "size is a uniform grow");
            assertTrue(side.shown() >= 3f && side.shown() <= PlateGenerator.MAX_PLATES,
                    "a wild count is three to five a cluster: " + side.shown());
        }
        assertEquals(PlateSize.classes(), sizes.size(), "size: " + sizes);
        assertEquals(PlateGenerator.FORMS, forms.size(), "form does not reach every shape");
        assertEquals(PlateGenerator.OVERLAPS, overlaps.size(), "overlap does not reach every bucket");
        assertEquals(PlateGenerator.SPIKES, spikes.size(), "spikiness does not reach every bucket");
        assertTrue(stretches.size() > 100);
        assertTrue(counts.size() > 100);
    }

    /** The style index round-trips: every form, overlap and spikiness is its own mesh. */
    @Test
    void theStyleIndexRoundTrips() {
        Set<Integer> seen = new HashSet<>();
        for (int f = 0; f < PlateGenerator.FORMS; f++) {
            for (int o = 0; o < PlateGenerator.OVERLAPS; o++) {
                for (int s = 0; s < PlateGenerator.SPIKES; s++) {
                    int style = PlateGenerator.style(f, o, s);
                    assertEquals(f, PlateGenerator.formOf(style));
                    assertEquals(o, PlateGenerator.overlapOf(style));
                    assertEquals(s, PlateGenerator.spikinessOf(style));
                    assertTrue(seen.add(style));
                }
            }
        }
        assertEquals(PlateGenerator.styles(), seen.size());
    }

    /**
     * The numbering the count relies on: every box is in a group, the groups are
     * 0..MAX-1 with none missing, and each group is exactly two trees - a slab at the
     * shoulder (forward of the anchor) and one at the hip (behind it) - stepping down
     * the flank as the number rises, so "the first k" is the top k of each cluster
     * (owner, 2026-10-05: top down).
     */
    @Test
    void eachStepIsAShoulderSlabAndAHipSlabTopDown() {
        for (PartShape shape : plateShapes()) {
            List<PartNode> nodes = PartGenerators.build(shape);
            int[] shoulders = new int[PlateGenerator.MAX_PLATES];
            int[] hips = new int[PlateGenerator.MAX_PLATES];
            float[] top = new float[PlateGenerator.MAX_PLATES];
            for (PartNode node : nodes) {
                int g = node.group();
                assertTrue(g >= 0 && g < PlateGenerator.MAX_PLATES, shape + ": a box in group " + g);
                if (node.isRoot()) {
                    if (node.oz() < 0f) {
                        shoulders[g]++;
                    } else {
                        hips[g]++;
                    }
                    top[g] = node.oy();
                } else {
                    assertEquals(g, nodes.get(node.parent()).group(), shape + ": a slab's edge is on another slab");
                    assertTrue(nodes.get(node.parent()).isRoot(), shape + ": an edge box hangs off an edge box");
                }
            }
            for (int g = 0; g < PlateGenerator.MAX_PLATES; g++) {
                assertEquals(1, shoulders[g], shape + ": step " + g + " shoulder slabs");
                assertEquals(1, hips[g], shape + ": step " + g + " hip slabs");
                if (g > 0) {
                    assertTrue(top[g] > top[g - 1], shape + ": step " + g + " is not below " + (g - 1));
                }
            }
        }
    }

    /**
     * Each slab is a flat bone box hung from its top edge (turned half round), and
     * everything on it - the lip, a rib, a spike - is a polished last box hung straight
     * off the slab. That is the whole of the two-tone rule (owner, 2026-10-05: the slab
     * is the base colour, its edge and feature the tip): a root with children sits at
     * the base end of {@code PartModel.along}, a box with none at the tip end.
     */
    @Test
    void theSlabIsTheBaseAndItsEdgeTheTip() {
        for (PartShape shape : plateShapes()) {
            List<PartNode> nodes = PartGenerators.build(shape);
            int form = PlateGenerator.formOf(shape.style());
            int[] children = new int[nodes.size()];
            for (PartNode node : nodes) {
                if (!node.isRoot()) {
                    children[node.parent()]++;
                }
            }
            for (int i = 0; i < nodes.size(); i++) {
                PartNode node = nodes.get(i);
                if (node.isRoot()) {
                    assertEquals(PartSheet.BONE, node.tex(), shape + " slab " + i);
                    assertTrue(node.width() < node.girth(), shape + " slab " + i + " is not flat");
                    assertTrue(Math.abs(Math.abs(node.rz()) - Math.PI) < 0.5,
                            shape + " slab " + i + " does not hang: rz " + node.rz());
                    int expected = form == PlateGenerator.SMOOTH ? 1 : (form == PlateGenerator.RIDGED ? 2 : 3);
                    assertEquals(expected, children[i], shape + " slab " + i + " edge boxes");
                } else {
                    assertEquals(PartSheet.BONE_TIP, node.tex(), shape + " edge box " + i);
                    assertEquals(0, children[i], shape + " edge box " + i + " has boxes on it");
                }
            }
        }
    }

    /** The left side is the right mirrored, box for box - never a negative scale. */
    @Test
    void theLeftSideIsTheRightMirrored() {
        for (PartShape right : plateShapes()) {
            if (right.kind() != PartKind.PLATES_RIGHT) {
                continue;
            }
            List<PartNode> r = PartGenerators.build(right);
            List<PartNode> l = PartGenerators.build(new PartShape(PartKind.PLATES_LEFT, right.style(), right.size()));
            assertEquals(r.size(), l.size());
            for (int i = 0; i < r.size(); i++) {
                PartNode a = r.get(i);
                PartNode b = l.get(i);
                assertEquals(a.parent(), b.parent());
                assertEquals(-a.ox(), b.ox(), 1e-6f, right + " box " + i + " x");
                assertEquals(a.oy(), b.oy());
                assertEquals(a.oz(), b.oz());
                assertEquals(a.rx(), b.rx());
                assertEquals(-a.rz(), b.rz(), 1e-6f, right + " box " + i + " lean");
                assertEquals(a.len(), b.len());
                assertEquals(a.girth(), b.girth());
                assertEquals(a.width(), b.width());
                assertEquals(a.group(), b.group());
                assertEquals(a.tex(), b.tex());
            }
            // And a right slab sits on the -x side of its anchor, a left one on +x.
            assertTrue(r.get(0).ox() < 0f, right + ": the right side faces -x");
        }
    }

    /**
     * Where the clusters land: every slab within the cluster's room down the flank,
     * and clear of the saddle along the body. The saddle range and the anchor are the
     * client's ({@code AttachedPartLayer}: saddle {@code z -9.5..0.5}, flank anchor at
     * {@code z -6}), repeated here by value because {@code common/} cannot see them -
     * so in the anchor's frame the saddle is {@code -3.5..6.5}. A slab's girth grows it
     * about its own centre by up to the largest stretch a class allows, which is in the
     * margin. Placement from the boxes, not seen.
     */
    @Test
    void theClustersFitTheFlankAndClearTheSaddle() {
        float saddleFrom = -9.5f + 6f;
        float saddleTo = 0.5f + 6f;
        for (PartShape shape : plateShapes()) {
            float height = PlateSize.values()[shape.size()].length();
            for (PartNode node : PartGenerators.build(shape)) {
                if (!node.isRoot()) {
                    continue;
                }
                assertTrue(node.oy() + node.len() <= 9.0f + 1e-4f, shape + ": a slab hangs off the belly");
                float half = node.girth() / 2f;
                assertTrue(node.oz() + half < saddleFrom || node.oz() - half > saddleTo,
                        shape + ": a slab at z " + node.oz() + " runs under the saddle");
                assertEquals(height, node.len(), 1e-4f, shape + ": the slab is its class height");
            }
        }
    }

    /** White by default; the colour locus dresses both sides, two-tone for two different copies. */
    @Test
    void theColourLocusDressesBothSides() {
        Epigenome epi = Epigenome.fromSeed(5);
        AttachedPart plain = GrownParts.of(horse(Sex.MALE, "body_plates", "Plt/n"), epi).get(0);
        for (int tint : new int[] {plain.baseTint(), plain.tipTint()}) {
            assertTrue((tint >> 16 & 0xFF) > 0xC0 && (tint >> 8 & 0xFF) > 0xC0 && (tint & 0xFF) > 0xC0,
                    "white: " + Integer.toHexString(tint));
        }

        List<AttachedPart> twoTone = GrownParts.of(horse(Sex.MALE, "body_plates", "Plt/n",
                "body_plate_colour", "Red/Blu"), epi);
        for (AttachedPart side : twoTone) {
            assertTrue(side.twoTone());
            assertEquals(twoTone.get(0).baseTint(), side.baseTint(), "the sides match");
            assertEquals(twoTone.get(0).tipTint(), side.tipTint(), "the sides match");
        }
        assertEquals(twoTone, GrownParts.of(horse(Sex.MALE, "body_plates", "Plt/n",
                "body_plate_colour", "Blu/Red"), epi), "the base is fixed by the pair, not the order");

        // The bone version: Bon/Bon is bare ivory bone, the skeleton breeds' pin. Bone has
        // nothing soft to leave off, so it stays opaque.
        for (AttachedPart bone : GrownParts.of(horse(Sex.MALE, "body_plates", "Plt/n",
                "body_plate_colour", "Bon/Bon"), epi)) {
            int r = bone.baseTint() >> 16 & 0xFF;
            int b = bone.baseTint() & 0xFF;
            assertTrue(r > 0x90 && r >= b, "bone is pale and warm: " + Integer.toHexString(bone.baseTint()));
            assertFalse(bone.translucent());
        }
    }

    /** Each part has its own colour: the plates' colour leaves the spines alone, and back. */
    @Test
    void thePlatesAndTheSpinesAreColouredApart() {
        Epigenome epi = Epigenome.fromSeed(9);
        List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "dorsal_spines", "Dsp/n",
                "body_plates", "Plt/n", "dorsal_spine_colour", "Red/Red", "body_plate_colour", "Blk/Blk"), epi);
        assertEquals(3, parts.size(), "a spine row and a pair of plates stack");
        for (AttachedPart part : parts) {
            boolean red = (part.baseTint() >> 16 & 0xFF) > 0x80;
            assertEquals(part.kind() == PartKind.SPINES, red, part.kind() + " has the other part's colour");
        }
        assertEquals(2, plates(parts).size());
    }

    @Test
    void withoutPlatesTheColourDoesNothing() {
        assertTrue(GrownParts.of(horse(Sex.MALE, "body_plate_colour", "Red/Blu"), Epigenome.fromSeed(1))
                .isEmpty());
    }

    /** A bigger class is a taller slab, and a spikier one stands further out. */
    @Test
    void sizeAndSpikinessChangeTheSlabs() {
        float small = PartGenerators.build(new PartShape(PartKind.PLATES_RIGHT, 0, 0)).get(0).len();
        float massive = PartGenerators.build(new PartShape(PartKind.PLATES_RIGHT, 0, PlateSize.classes() - 1))
                .get(0).len();
        assertTrue(massive > small);
        float blunt = spikeReach(PlateGenerator.style(PlateGenerator.SPIKED, 0, 0));
        float sharp = spikeReach(PlateGenerator.style(PlateGenerator.SPIKED, 0, PlateGenerator.SPIKES - 1));
        assertTrue(sharp > blunt, "spikiness changes the spike: " + blunt + " vs " + sharp);
        float loose = PartGenerators.build(new PartShape(PartKind.PLATES_RIGHT,
                PlateGenerator.style(0, 0, 0), 0)).stream().filter(PartNode::isRoot).toList().get(2).oy();
        float tight = PartGenerators.build(new PartShape(PartKind.PLATES_RIGHT,
                PlateGenerator.style(0, PlateGenerator.OVERLAPS - 1, 0), 0)).stream().filter(PartNode::isRoot)
                .toList().get(2).oy();
        assertTrue(tight < loose, "a higher overlap steps the slabs closer: " + loose + " vs " + tight);
    }

    /** The first slab's spike, both boxes. */
    private static float spikeReach(int style) {
        List<PartNode> nodes = PartGenerators.build(new PartShape(PartKind.PLATES_RIGHT, style, 1));
        float reach = 0f;
        for (PartNode node : nodes) {
            if (node.parent() == 0 && node.rz() != 0f) {
                reach += node.len();
            }
        }
        return reach;
    }

    @Test
    void bothAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.BODY_PLATES, Genes.BODY_PLATE_COLOUR)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
            assertFalse(g.isNatural());
            assertFalse(g.affectsCoat(), g.key() + " must not reach the coat texture");
        }
        assertEquals(List.of(new GrownParts.Listed("Shoulder and hip plates", true)),
                GrownParts.listed(horse(Sex.MALE, "body_plates", "Plt/n"), Epigenome.fromSeed(2)),
                "the designer names the pair once, adult only");
    }

    /** The spines' small share: about three horses in a thousand grow plates, and every carrier shows. */
    @Test
    void platesAreRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int plated = 0;
        int homozygous = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int count = Genotype.random(rng).pair(GENE).count(GENE.Plt);
            plated += count > 0 ? 1 : 0;
            homozygous += count == 2 ? 1 : 0;
        }
        assertTrue(plated > 200 && plated < 420, "Plt in " + n + ": " + plated);
        assertEquals(0, homozygous, "a founder is Plt/n or nothing");
    }
}
