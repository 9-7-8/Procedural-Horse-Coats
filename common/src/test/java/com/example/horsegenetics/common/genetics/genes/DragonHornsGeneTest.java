package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.DragonHornGenerator;
import com.example.horsegenetics.common.parts.DragonHornSize;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartGenerators;
import com.example.horsegenetics.common.parts.PartKind;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartShape;
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

/** The dragon horns and their colour locus - the first of the head parts. */
class DragonHornsGeneTest {

    private static final DragonHornsGene GENE = Genes.DRAGON_HORNS;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static List<AttachedPart> dragon(List<AttachedPart> parts) {
        return parts.stream().filter(p -> p.kind().dragonHorn()).toList();
    }

    /** Recessive: two copies grow a pair, one shows nothing - on mares and stallions alike. */
    @Test
    void twoCopiesGrowAPairOnEitherSex() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            assertTrue(GrownParts.of(horse(sex, "dragon_horns", "Drg/n"), epi).isEmpty(), sex + " carrier");
            List<AttachedPart> pair = GrownParts.of(horse(sex, "dragon_horns", "Drg/Drg"), epi);
            assertEquals(2, pair.size(), sex.toString());
            assertSame(PartKind.DRAGON_HORN_RIGHT, pair.get(0).kind());
            assertSame(PartKind.DRAGON_HORN_LEFT, pair.get(1).kind());
            assertEquals(pair.get(0).shape().style(), pair.get(1).shape().style(), "a pair is symmetric");
            assertEquals(pair.get(0).stretch(), pair.get(1).stretch());
        }
        assertTrue(GENE.expressionOf(pair("Drg/n")).wildType());
        assertFalse(GENE.affectsCoat());
        assertFalse(PartKind.DRAGON_HORN_LEFT.showsOnFoal(), "a hard part comes with maturity");
        assertSame(PartAnchor.NAPE_RIGHT, PartKind.DRAGON_HORN_RIGHT.anchor());
        assertSame(PartAnchor.NAPE_LEFT, PartKind.DRAGON_HORN_LEFT.anchor());
    }

    private static AllelePair pair(String tokens) {
        return Genotype.parse(Codes.of("dragon_horns", tokens)).pair(GENE);
    }

    /** Every number on the copy reaches the horns: length, girth, form, sweep, splay. */
    @Test
    void everyEpigeneticNumberReachesTheHorns() {
        Genotype gt = horse(Sex.FEMALE, "dragon_horns", "Drg/Drg");
        Set<Integer> sizes = new HashSet<>();
        Set<Integer> forms = new HashSet<>();
        Set<Integer> sweeps = new HashSet<>();
        Set<Integer> splays = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> girths = new HashSet<>();
        for (long seed = 0; seed < 1500; seed++) {
            AttachedPart right = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            int style = right.shape().style();
            sizes.add(right.shape().size());
            forms.add(DragonHornGenerator.formOf(style));
            sweeps.add(style / DragonHornGenerator.SPLAYS % DragonHornGenerator.SWEEPS);
            splays.add(style % DragonHornGenerator.SPLAYS);
            stretches.add(right.stretch());
            girths.add(right.girth());
        }
        assertTrue(sizes.size() >= 3, "length: " + sizes);
        assertEquals(DragonHornGenerator.FORMS, forms.size(), "form does not reach every shape");
        assertEquals(DragonHornGenerator.SWEEPS, sweeps.size(), "sweep does not reach every bucket");
        assertEquals(DragonHornGenerator.SPLAYS, splays.size(), "splay does not reach every bucket");
        assertTrue(stretches.size() > 100);
        assertTrue(girths.size() > 100);
    }

    /** White by default; the colour locus dresses both sides, two-tone for two different copies. */
    @Test
    void theColourLocusDressesThePair() {
        Epigenome epi = Epigenome.fromSeed(5);
        List<AttachedPart> plain = GrownParts.of(horse(Sex.MALE, "dragon_horns", "Drg/Drg"), epi);
        assertEquals(0xFFFFFFFF, plain.get(0).baseTint());
        assertFalse(plain.get(0).twoTone());

        List<AttachedPart> black = GrownParts.of(horse(Sex.MALE, "dragon_horns", "Drg/Drg",
                "dragon_horn_colour", "Blk/Blk"), epi);
        for (AttachedPart side : black) {
            // Each copy keeps its own tone, so two Blk copies are two near-identical
            // blacks - the horn's rule, and why this is not asserted one-tone.
            assertTrue((side.baseTint() & 0xFF) < 0x40, "black: " + Integer.toHexString(side.baseTint()));
            assertTrue((side.tipTint() & 0xFF) < 0x40, "black: " + Integer.toHexString(side.tipTint()));
        }
        List<AttachedPart> twoTone = GrownParts.of(horse(Sex.MALE, "dragon_horns", "Drg/Drg",
                "dragon_horn_colour", "Red/Blu"), epi);
        for (AttachedPart side : twoTone) {
            assertTrue(side.twoTone());
        }
        assertEquals(twoTone.get(0).baseTint(), twoTone.get(1).baseTint(), "both sides one colour");
        assertEquals(twoTone, GrownParts.of(horse(Sex.MALE, "dragon_horns", "Drg/Drg",
                "dragon_horn_colour", "Blu/Red"), epi), "the base is fixed by the pair, not the order");
    }

    /** Each part has its own colour: the dragon colour leaves the unicorn horn alone, and back. */
    @Test
    void theHornAndTheDragonHornsAreColouredApart() {
        Epigenome epi = Epigenome.fromSeed(9);
        List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "unicorn_horn", "Horn/Horn",
                "dragon_horns", "Drg/Drg", "horn_colour", "Red/Red", "dragon_horn_colour", "Blk/Blk"), epi);
        assertEquals(3, parts.size(), "a horn and a pair of dragon horns stack");
        AttachedPart horn = parts.get(0);
        assertSame(PartKind.HORN, horn.kind());
        assertTrue((horn.baseTint() >> 16 & 0xFF) > 0x80, "the horn stays red");
        for (AttachedPart side : dragon(parts)) {
            assertTrue((side.baseTint() >> 16 & 0xFF) < 0x40, "the dragon horns are black");
        }
    }

    /** Without the pair the colour locus does nothing at all. */
    @Test
    void withoutDragonHornsTheColourDoesNothing() {
        assertTrue(GrownParts.of(horse(Sex.MALE, "dragon_horn_colour", "Red/Blu"), Epigenome.fromSeed(1))
                .isEmpty());
    }

    @Test
    void everyDragonHornMeshIsSmallMirroredAndGrowsWithItsClass() {
        for (PartShape shape : PartGenerators.allShapes()) {
            if (!shape.kind().dragonHorn()) {
                continue;
            }
            List<PartNode> nodes = PartGenerators.build(shape);
            assertTrue(nodes.size() <= DragonHornGenerator.MAX_NODES, shape + " is " + nodes.size() + " boxes");
            if (shape.kind() == PartKind.DRAGON_HORN_RIGHT) {
                List<PartNode> left = PartGenerators.build(
                        new PartShape(PartKind.DRAGON_HORN_LEFT, shape.style(), shape.size()));
                assertEquals(nodes.size(), left.size());
                for (int i = 0; i < nodes.size(); i++) {
                    assertEquals(-nodes.get(i).rz(), left.get(i).rz(), 1e-6f, shape + " box " + i);
                    assertEquals(nodes.get(i).rx(), left.get(i).rx(), 1e-6f);
                }
            }
            assertTrue(nodes.get(0).rx() < 0f, shape + " must rake back off the skull, not forward");
        }
        int budding = PartGenerators.build(new PartShape(PartKind.DRAGON_HORN_RIGHT, 0, 0)).size();
        int great = PartGenerators.build(
                new PartShape(PartKind.DRAGON_HORN_RIGHT, 0, DragonHornSize.classes() - 1)).size();
        assertTrue(great > budding);
        assertNotEquals(PartGenerators.build(new PartShape(PartKind.DRAGON_HORN_RIGHT,
                        DragonHornGenerator.style(DragonHornGenerator.SWEPT, 0, 1), 2)),
                PartGenerators.build(new PartShape(PartKind.DRAGON_HORN_RIGHT,
                        DragonHornGenerator.style(DragonHornGenerator.SWEPT, 3, 1), 2)),
                "the sweep bucket changes the mesh");
        assertNotEquals(PartGenerators.build(new PartShape(PartKind.DRAGON_HORN_RIGHT,
                        DragonHornGenerator.style(DragonHornGenerator.CURLED, 1, 0), 2)),
                PartGenerators.build(new PartShape(PartKind.DRAGON_HORN_RIGHT,
                        DragonHornGenerator.style(DragonHornGenerator.CURLED, 1, 2), 2)),
                "the splay bucket changes the mesh");
    }

    @Test
    void bothAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.DRAGON_HORNS, Genes.DRAGON_HORN_COLOUR)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
            assertFalse(g.isNatural());
            assertFalse(g.affectsCoat(), g.key() + " must not reach the coat texture");
        }
    }

    /** The unicorn's figure: about one horse in four hundred grows a pair. */
    @Test
    void dragonHornsAreRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int pairs = 0;
        int carriers = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int count = Genotype.random(rng).pair(GENE).count(GENE.Drg);
            pairs += count == 2 ? 1 : 0;
            carriers += count == 1 ? 1 : 0;
        }
        assertTrue(pairs > 150 && pairs < 360, "Drg/Drg in " + n + ": " + pairs);
        assertTrue(carriers > 8_500 && carriers < 10_500, "Drg/n in " + n + ": " + carriers);
    }
}
