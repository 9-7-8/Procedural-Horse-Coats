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
import com.example.horsegenetics.common.parts.SailGenerator;
import com.example.horsegenetics.common.parts.SailSize;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The back sail and its colour locus - the body parts' see-through one. */
class BackSailGeneTest {

    private static final BackSailGene GENE = Genes.BACK_SAIL;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static AttachedPart sail(Genotype genotype, long seed) {
        List<AttachedPart> parts = GrownParts.of(genotype, Epigenome.fromSeed(seed));
        assertEquals(1, parts.size(), "one sail");
        return parts.get(0);
    }

    /** Recessive: two copies grow the sail, on mares and stallions alike; a carrier shows nothing. */
    @Test
    void twoCopiesGrowTheSailOnEitherSex() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            assertTrue(GrownParts.of(horse(sex, "back_sail", "n/n"), epi).isEmpty(), sex + " wild");
            assertTrue(GrownParts.of(horse(sex, "back_sail", "Sail/n"), epi).isEmpty(), sex + " carrier");
            assertSame(PartKind.SAIL, sail(horse(sex, "back_sail", "Sail/Sail"), 3).kind());
        }
        assertEquals("back-sail-carrier",
                GENE.expressionOf(Genotype.parse(Codes.of("back_sail", "Sail/n")).pair(GENE)).id());
        assertFalse(GENE.affectsCoat());
        assertFalse(PartKind.SAIL.showsOnFoal(), "a hard part comes with maturity");
        assertSame(PartAnchor.SPINE, PartKind.SAIL.anchor());
        assertTrue(PartKind.SAIL.saddleZoned());
        assertTrue(PartKind.SAIL.scalesPerElement());
    }

    /**
     * Every number on the copy reaches the sail: length, count, coverage, curve, opacity,
     * form. Girth is not one of them, and stays 1 - a thicker spine would close the
     * panel between it and the next.
     */
    @Test
    void everyEpigeneticNumberReachesTheSail() {
        Genotype gt = horse(Sex.FEMALE, "back_sail", "Sail/Sail");
        Set<Integer> sizes = new HashSet<>();
        Set<Integer> forms = new HashSet<>();
        Set<Integer> curves = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> counts = new HashSet<>();
        Set<Float> spans = new HashSet<>();
        Set<Float> opacities = new HashSet<>();
        for (long seed = 0; seed < 1500; seed++) {
            AttachedPart s = sail(gt, seed);
            sizes.add(s.shape().size());
            forms.add(SailGenerator.formOf(s.shape().style()));
            curves.add(SailGenerator.curveOf(s.shape().style()));
            stretches.add(s.stretch());
            counts.add(s.shown());
            spans.add(s.span());
            opacities.add(s.opacity());
            assertEquals(1f, s.girth(), "a sail has no thickness number");
            assertTrue(s.shown() >= 1f && s.shown() <= SailGenerator.MAX_SPINES, "count " + s.shown());
            assertTrue(s.span() >= 0.35f && s.span() <= 1f, "coverage " + s.span());
            assertTrue(s.opacity() >= BackSailGene.MIN_OPACITY - 1e-6
                    && s.opacity() <= BackSailGene.MAX_OPACITY + 1e-6, "opacity " + s.opacity());
            assertTrue(s.translucent(), "every sail's membrane is see-through");
        }
        assertTrue(sizes.size() >= 3, "length: " + sizes);
        assertEquals(SailGenerator.FORMS, forms.size(), "form does not reach every shape");
        assertEquals(SailGenerator.CURVES, curves.size(), "curve does not reach every bucket");
        assertTrue(stretches.size() > 100);
        assertTrue(counts.size() > 100);
        assertTrue(spans.size() > 100);
        assertTrue(opacities.size() > 100);
    }

    /**
     * White by default. Two different copies make the membrane one colour and the spines
     * the other: base and tip, the order fixed by the pair. Bone on both is bare rays.
     */
    @Test
    void theColourLocusDressesTheMembraneAndTheSpines() {
        AttachedPart plain = sail(horse(Sex.MALE, "back_sail", "Sail/Sail"), 5);
        for (int tint : new int[] {plain.baseTint(), plain.tipTint()}) {
            assertTrue((tint >> 16 & 0xFF) > 0xC0 && (tint >> 8 & 0xFF) > 0xC0 && (tint & 0xFF) > 0xC0,
                    "white: " + Integer.toHexString(tint));
        }

        AttachedPart twoTone = sail(horse(Sex.MALE, "back_sail", "Sail/Sail", "back_sail_colour", "Red/Blu"), 5);
        assertTrue(twoTone.twoTone());
        assertEquals(twoTone, sail(horse(Sex.MALE, "back_sail", "Sail/Sail", "back_sail_colour", "Blu/Red"), 5),
                "the base is fixed by the pair, not the order");
        assertTrue((twoTone.baseTint() >> 16 & 0xFF) > (twoTone.baseTint() & 0xFF), "red is the membrane");
        assertTrue((twoTone.tipTint() & 0xFF) > (twoTone.tipTint() >> 16 & 0xFF), "blue is the spines");

        // The bone version: Bon/Bon draws the rays in bone and leaves the membrane off.
        AttachedPart bone = sail(horse(Sex.MALE, "back_sail", "Sail/Sail", "back_sail_colour", "Bon/Bon"), 5);
        assertEquals(0f, bone.opacity(), "a bone sail has no skin");
        assertTrue(bone.translucent(), "the membrane pass is still the one that decides, and draws nothing");
        int r = bone.tipTint() >> 16 & 0xFF;
        int b = bone.tipTint() & 0xFF;
        assertTrue(r > 0x90 && r >= b, "bone is pale and warm: " + Integer.toHexString(bone.tipTint()));
        // One bone copy is a bone-coloured part with its membrane, not the bone version.
        AttachedPart half = sail(horse(Sex.MALE, "back_sail", "Sail/Sail", "back_sail_colour", "Red/Bon"), 5);
        assertTrue(half.opacity() > 0f, "one bone copy keeps the membrane");
        assertTrue(Genes.BACK_SAIL_COLOUR.boneOnly(
                horse(Sex.MALE, "back_sail_colour", "Bon/Bon").pair(Genes.BACK_SAIL_COLOUR)));
        assertFalse(Genes.BACK_SAIL_COLOUR.boneOnly(
                horse(Sex.MALE, "back_sail_colour", "Red/Bon").pair(Genes.BACK_SAIL_COLOUR)));
    }

    @Test
    void withoutASailTheColourDoesNothing() {
        assertTrue(GrownParts.of(horse(Sex.MALE, "back_sail_colour", "Red/Blu"), Epigenome.fromSeed(1)).isEmpty());
    }

    /** The sail and the spine row are coloured apart, and stack. */
    @Test
    void theSailAndTheSpinesAreColouredApart() {
        List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "back_sail", "Sail/Sail",
                "dorsal_spines", "Dsp/n", "back_sail_colour", "Red/Red", "dorsal_spine_colour", "Blk/Blk"),
                Epigenome.fromSeed(9));
        assertEquals(2, parts.size(), "a sail and a spine row stack");
        for (AttachedPart part : parts) {
            boolean red = (part.baseTint() >> 16 & 0xFF) > 0x80;
            assertEquals(part.kind() == PartKind.SAIL, red, part.kind() + " has the other part's colour");
        }
    }

    /**
     * The sheet regions: membrane is see-through on a sail and nowhere else, its spines
     * are bone, and the antler's crystal shafts are still its bone and only that.
     */
    @Test
    void onlyTheMembraneIsSeeThrough() {
        assertEquals(PartSheet.bit(PartSheet.MEMBRANE), PartKind.SAIL.translucentRegions());
        assertEquals(PartSheet.bit(PartSheet.BONE), PartKind.ANTLER_LEFT.translucentRegions());
        assertEquals(PartSheet.bit(PartSheet.BONE), PartKind.ANTLER_RIGHT.translucentRegions());
        assertEquals(PartSheet.bit(PartSheet.CRYSTAL), PartKind.CRYSTALS.translucentRegions());
        for (PartKind kind : PartKind.values()) {
            if (!kind.antler() && kind != PartKind.SAIL && kind != PartKind.CRYSTALS) {
                assertEquals(0, kind.translucentRegions(), kind + " is never see-through");
            }
        }
        // A part with no see-through region is never translucent, whatever its opacity.
        AttachedPart horn = AttachedPart.horn(0.5, 1.0, 0.0, 0).seeThrough(0.3f);
        assertFalse(horn.translucent());
        // A drifted number clamps rather than drawing a negative or super-opaque skin.
        assertEquals(0f, horn.seeThrough(Float.NaN).opacity());
        assertEquals(AttachedPart.OPAQUE, horn.seeThrough(4f).opacity());
    }

    /**
     * Every sail mesh: membrane boxes are flat and in groups 1 and up, hung off the
     * root of the spine behind them, {@link SailGenerator#COLUMNS} to a gap, and never
     * taller than the taller of the two spines they span. No spine samples the membrane.
     */
    @Test
    void everyPanelIsSpannedByTheSpinesEitherSide() {
        for (PartShape shape : PartGenerators.allShapes()) {
            if (shape.kind() != PartKind.SAIL) {
                continue;
            }
            List<PartNode> nodes = PartGenerators.build(shape);
            float[] height = heights(nodes);
            int[] columns = new int[SailGenerator.MAX_SPINES];
            for (int i = 0; i < nodes.size(); i++) {
                PartNode node = nodes.get(i);
                if (node.tex() != PartSheet.MEMBRANE) {
                    assertEquals(node.girth(), node.width(), shape + " box " + i + ": a spine is square");
                    assertTrue(node.rx() == 0f && node.rz() == 0f, shape + " box " + i + ": a ray stands upright");
                    continue;
                }
                int g = node.group();
                assertTrue(g >= 1, shape + ": spine 0 has nothing in front of it to span to");
                assertTrue(node.width() < node.girth(), shape + " box " + i + " is not flat");
                PartNode parent = nodes.get(node.parent());
                assertTrue(parent.isRoot() && parent.group() == g,
                        shape + " box " + i + " does not hang off its own spine's root");
                assertTrue(node.oz() < 0f, shape + " box " + i + " is not in front of its spine");
                assertTrue(node.len() <= Math.max(height[g - 1], height[g]) + 1e-4f,
                        shape + " box " + i + " stands above both its spines");
                columns[g]++;
            }
            for (int g = 1; g < SailGenerator.MAX_SPINES; g++) {
                assertEquals(SailGenerator.COLUMNS, columns[g], shape + ": panel " + g);
            }
        }
    }

    /** A tall sail peaks over the middle; a low one is lower; a scalloped one sags between spines. */
    @Test
    void theFormsShapeTheSail() {
        int size = SailSize.classes() - 1;
        for (int curve = 0; curve < SailGenerator.CURVES; curve++) {
            float[] tall = heights(build(SailGenerator.TALL, curve, size));
            float[] low = heights(build(SailGenerator.LOW, curve, size));
            int mid = SailGenerator.MAX_SPINES / 2;
            assertTrue(tall[mid] > tall[0] && tall[mid] > tall[SailGenerator.MAX_SPINES - 1],
                    "a tall sail peaks over the middle, curve " + curve);
            assertTrue(max(low) < 0.5f * max(tall), "a low sail is under half a tall one");
            assertTrue(max(tall) <= SailSize.values()[size].length() + 1e-3f, "the class height is the peak");

            List<PartNode> scalloped = build(SailGenerator.SCALLOPED, curve, size);
            List<PartNode> straight = build(SailGenerator.TALL, curve, size);
            float[] sag = panel(scalloped, mid);
            float[] edge = panel(straight, mid);
            assertTrue(sag[1] < sag[0] && sag[1] < sag[2], "a scalloped panel is lowest in its middle");
            assertTrue(sag[1] < edge[1], "and lower than a straight one there");
        }
        // The curve bucket is the arch: a harder curve drops the ends further.
        float gentle = heights(build(SailGenerator.TALL, 0, size))[0];
        float hard = heights(build(SailGenerator.TALL, SailGenerator.CURVES - 1, size))[0];
        assertTrue(hard < gentle, "curve does not change the arch");
        // A taller class is more boxes per spine.
        assertTrue(build(SailGenerator.TALL, 0, size).size() > build(SailGenerator.TALL, 0, 0).size());
    }

    private static List<PartNode> build(int form, int curve, int size) {
        return PartGenerators.build(new PartShape(PartKind.SAIL, SailGenerator.style(form, curve), size));
    }

    /** Each spine's summed box length, withers to croup - the membrane is not a spine. */
    private static float[] heights(List<PartNode> nodes) {
        float[] out = new float[SailGenerator.MAX_SPINES];
        for (PartNode node : nodes) {
            if (node.tex() != PartSheet.MEMBRANE) {
                out[node.group()] += node.len();
            }
        }
        return out;
    }

    /** The heights of the membrane columns in front of spine {@code g}, front to back. */
    private static float[] panel(List<PartNode> nodes, int g) {
        float[] out = new float[SailGenerator.COLUMNS];
        int j = 0;
        for (PartNode node : nodes) {
            if (node.tex() == PartSheet.MEMBRANE && node.group() == g) {
                out[j++] = node.len();
            }
        }
        return out;
    }

    private static float max(float[] xs) {
        float m = 0f;
        for (float x : xs) {
            m = Math.max(m, x);
        }
        return m;
    }

    @Test
    void bothAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.BACK_SAIL, Genes.BACK_SAIL_COLOUR)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
            assertFalse(g.isNatural());
            assertFalse(g.affectsCoat(), g.key() + " must not reach the coat texture");
        }
        assertTrue(Genes.BACK_SAIL_COLOUR.expressions().stream()
                .anyMatch(e -> e.description().contains("the membrane is one")),
                "the two-tone outcome says how a sail divides, not 'its tip'");
    }

    /** The unicorn's share: about one wild horse in four hundred grows a sail, one in ten carries. */
    @Test
    void sailsAreRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int sails = 0;
        int carriers = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int count = Genotype.random(rng).pair(GENE).count(GENE.Sail);
            sails += count == 2 ? 1 : 0;
            carriers += count == 1 ? 1 : 0;
        }
        assertTrue(sails > 150 && sails < 380, "Sail/Sail in " + n + ": " + sails);
        assertTrue(carriers > 8_500 && carriers < 10_500, "Sail/n in " + n + ": " + carriers);
    }
}
