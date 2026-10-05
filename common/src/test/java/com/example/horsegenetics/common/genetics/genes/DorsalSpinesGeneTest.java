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
import com.example.horsegenetics.common.parts.DorsalSpineGenerator;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartGenerators;
import com.example.horsegenetics.common.parts.PartKind;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartShape;
import com.example.horsegenetics.common.parts.SpineSize;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The dorsal spines and their colour locus - the first of the body parts. */
class DorsalSpinesGeneTest {

    private static final DorsalSpinesGene GENE = Genes.DORSAL_SPINES;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static List<AttachedPart> spines(List<AttachedPart> parts) {
        return parts.stream().filter(p -> p.kind() == PartKind.SPINES).toList();
    }

    /** Dominant: one copy grows the row, on mares and stallions alike; none shows nothing. */
    @Test
    void oneCopyGrowsTheRowOnEitherSex() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            assertTrue(GrownParts.of(horse(sex, "dorsal_spines", "n/n"), epi).isEmpty(), sex + " wild");
            for (String pair : List.of("Dsp/n", "Dsp/Dsp")) {
                List<AttachedPart> parts = GrownParts.of(horse(sex, "dorsal_spines", pair), epi);
                assertEquals(1, parts.size(), sex + " " + pair);
                assertSame(PartKind.SPINES, parts.get(0).kind());
            }
        }
        assertFalse(GENE.expressionOf(Genotype.parse(Codes.of("dorsal_spines", "Dsp/n")).pair(GENE)).id()
                .equals(GENE.expressionOf(Genotype.wildType().pair(GENE)).id()), "a carrier shows");
        assertFalse(GENE.affectsCoat());
        assertFalse(PartKind.SPINES.showsOnFoal(), "a hard part comes with maturity");
        assertSame(PartAnchor.SPINE, PartKind.SPINES.anchor());
        assertTrue(PartKind.SPINES.saddleZoned());
        assertTrue(PartKind.SPINES.scalesPerElement());
    }

    /** Every number on the copy reaches the row: length, girth, count, form, taper. */
    @Test
    void everyEpigeneticNumberReachesTheRow() {
        Genotype gt = horse(Sex.FEMALE, "dorsal_spines", "Dsp/Dsp");
        Set<Integer> sizes = new HashSet<>();
        Set<Integer> forms = new HashSet<>();
        Set<Integer> tapers = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> girths = new HashSet<>();
        Set<Float> counts = new HashSet<>();
        for (long seed = 0; seed < 1500; seed++) {
            AttachedPart row = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            sizes.add(row.shape().size());
            forms.add(DorsalSpineGenerator.formOf(row.shape().style()));
            tapers.add(DorsalSpineGenerator.taperOf(row.shape().style()));
            stretches.add(row.stretch());
            girths.add(row.girth());
            counts.add(row.shown());
            assertTrue(row.shown() >= 1f && row.shown() <= DorsalSpineGenerator.MAX_SPINES, "count " + row.shown());
        }
        assertTrue(sizes.size() >= 3, "length: " + sizes);
        assertEquals(DorsalSpineGenerator.FORMS, forms.size(), "form does not reach every shape");
        assertEquals(DorsalSpineGenerator.TAPERS, tapers.size(), "taper does not reach every bucket");
        assertTrue(stretches.size() > 100);
        assertTrue(girths.size() > 100);
        assertTrue(counts.size() > 100);
    }

    /** White by default; the colour locus dresses the row, two-tone for two different copies. */
    @Test
    void theColourLocusDressesTheRow() {
        Epigenome epi = Epigenome.fromSeed(5);
        // Each white copy keeps its own tone, so the default can be a hair off pure white
        // and a hair two-tone - the shared colour base's rule, not asserted away here.
        AttachedPart plain = GrownParts.of(horse(Sex.MALE, "dorsal_spines", "Dsp/n"), epi).get(0);
        for (int tint : new int[] {plain.baseTint(), plain.tipTint()}) {
            assertTrue((tint >> 16 & 0xFF) > 0xC0 && (tint >> 8 & 0xFF) > 0xC0 && (tint & 0xFF) > 0xC0,
                    "white: " + Integer.toHexString(tint));
        }

        AttachedPart black = GrownParts.of(horse(Sex.MALE, "dorsal_spines", "Dsp/n",
                "dorsal_spine_colour", "Blk/Blk"), epi).get(0);
        assertTrue((black.baseTint() & 0xFF) < 0x40, "black: " + Integer.toHexString(black.baseTint()));

        AttachedPart twoTone = GrownParts.of(horse(Sex.MALE, "dorsal_spines", "Dsp/n",
                "dorsal_spine_colour", "Red/Blu"), epi).get(0);
        assertTrue(twoTone.twoTone());
        assertEquals(twoTone, GrownParts.of(horse(Sex.MALE, "dorsal_spines", "Dsp/n",
                "dorsal_spine_colour", "Blu/Red"), epi).get(0), "the base is fixed by the pair, not the order");

        // The bone version: Bon/Bon is bare ivory bone, the skeleton breeds' pin.
        AttachedPart bone = GrownParts.of(horse(Sex.MALE, "dorsal_spines", "Dsp/n",
                "dorsal_spine_colour", "Bon/Bon"), epi).get(0);
        int r = bone.baseTint() >> 16 & 0xFF;
        int b = bone.baseTint() & 0xFF;
        assertTrue(r > 0x90 && r >= b, "bone is pale and warm: " + Integer.toHexString(bone.baseTint()));
    }

    /** Each part has its own colour: the spines' colour leaves the dragon horns alone, and back. */
    @Test
    void theSpinesAndTheDragonHornsAreColouredApart() {
        Epigenome epi = Epigenome.fromSeed(9);
        List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "dragon_horns", "Drg/Drg",
                "dorsal_spines", "Dsp/n", "dragon_horn_colour", "Red/Red", "dorsal_spine_colour", "Blk/Blk"), epi);
        assertEquals(3, parts.size(), "a pair of dragon horns and a spine row stack");
        for (AttachedPart part : parts) {
            boolean red = (part.baseTint() >> 16 & 0xFF) > 0x80;
            assertEquals(part.kind().dragonHorn(), red, part.kind() + " has the other part's colour");
        }
        assertEquals(1, spines(parts).size());
    }

    @Test
    void withoutSpinesTheColourDoesNothing() {
        assertTrue(GrownParts.of(horse(Sex.MALE, "dorsal_spine_colour", "Red/Blu"), Epigenome.fromSeed(1))
                .isEmpty());
    }

    /** A graduated row falls toward the croup, an alternating one steps, a uniform one does neither. */
    @Test
    void theFormsShapeTheRow() {
        int size = SpineSize.classes() - 1;
        float[] uniform = heights(DorsalSpineGenerator.style(DorsalSpineGenerator.UNIFORM, 0), size);
        float[] graduated = heights(DorsalSpineGenerator.style(DorsalSpineGenerator.GRADUATED, 0), size);
        float[] alternating = heights(DorsalSpineGenerator.style(DorsalSpineGenerator.ALTERNATING, 0), size);
        for (int i = 1; i < uniform.length; i++) {
            assertEquals(uniform[0], uniform[i], 1e-4f, "uniform spine " + i);
            assertTrue(graduated[i] < graduated[i - 1], "graduated spine " + i + " is not shorter");
            assertTrue((alternating[i] < alternating[i - 1]) == (i % 2 == 1), "alternating spine " + i);
        }
        assertEquals(SpineSize.values()[size].length(), uniform[0], 1e-3f, "the tallest spine is the class height");
    }

    /** Each spine's summed box length, front to back. */
    private static float[] heights(int style, int size) {
        float[] out = new float[DorsalSpineGenerator.MAX_SPINES];
        for (PartNode node : PartGenerators.build(new PartShape(PartKind.SPINES, style, size))) {
            out[node.group()] += node.len();
        }
        return out;
    }

    /** A taller class is more boxes per spine, and a sharper taper narrows the point. */
    @Test
    void everySpineMeshIsSmallAndTapers() {
        int low = PartGenerators.build(new PartShape(PartKind.SPINES, 0, 0)).size();
        int towering = PartGenerators.build(new PartShape(PartKind.SPINES, 0, SpineSize.classes() - 1)).size();
        assertTrue(towering > low);
        for (PartShape shape : PartGenerators.allShapes()) {
            if (shape.kind() != PartKind.SPINES) {
                continue;
            }
            List<PartNode> nodes = PartGenerators.build(shape);
            for (int i = 0; i < nodes.size(); i++) {
                PartNode node = nodes.get(i);
                if (!node.isRoot()) {
                    assertTrue(node.girth() <= nodes.get(node.parent()).girth(), shape + " box " + i + " widens");
                } else {
                    assertTrue(node.rx() < 0f, shape + " must rake back toward the tail, not forward");
                }
            }
        }
        float blunt = PartGenerators.build(new PartShape(PartKind.SPINES, DorsalSpineGenerator.style(0, 0), 3))
                .get(2).girth();
        float sharp = PartGenerators.build(new PartShape(PartKind.SPINES, DorsalSpineGenerator.style(0, 2), 3))
                .get(2).girth();
        assertTrue(sharp < blunt, "the taper bucket changes the point");
    }

    @Test
    void bothAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.DORSAL_SPINES, Genes.DORSAL_SPINE_COLOUR)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
            assertFalse(g.isNatural());
            assertFalse(g.affectsCoat(), g.key() + " must not reach the coat texture");
        }
    }

    /** The antlers' small share: about three horses in a thousand grow a row, and every carrier shows. */
    @Test
    void spinesAreRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int rows = 0;
        int homozygous = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int count = Genotype.random(rng).pair(GENE).count(GENE.Dsp);
            rows += count > 0 ? 1 : 0;
            homozygous += count == 2 ? 1 : 0;
        }
        assertTrue(rows > 200 && rows < 420, "Dsp in " + n + ": " + rows);
        assertEquals(0, homozygous, "a founder is Dsp/n or nothing");
    }
}
