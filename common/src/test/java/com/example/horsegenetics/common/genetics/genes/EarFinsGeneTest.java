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
import com.example.horsegenetics.common.parts.EarFinGenerator;
import com.example.horsegenetics.common.parts.EarFinSize;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartGenerators;
import com.example.horsegenetics.common.parts.PartKind;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartShape;
import com.example.horsegenetics.common.parts.PartSheet;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ear fins and their colour locus - the soft one of the head parts. */
class EarFinsGeneTest {

    private static final EarFinsGene GENE = Genes.EAR_FINS;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static List<PartShape> finShapes() {
        return PartGenerators.allShapes().stream().filter(s -> s.kind().earFin()).toList();
    }

    /** Recessive: two copies grow both sides, on mares and stallions alike; one shows nothing. */
    @Test
    void twoCopiesGrowBothSidesOnEitherSex() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            assertTrue(GrownParts.of(horse(sex, "ear_fins", "n/n"), epi).isEmpty(), sex + " wild");
            assertTrue(GrownParts.of(horse(sex, "ear_fins", "Efn/n"), epi).isEmpty(), sex + " carrier");
            List<AttachedPart> parts = GrownParts.of(horse(sex, "ear_fins", "Efn/Efn"), epi);
            assertEquals(2, parts.size(), sex + " Efn/Efn");
            assertSame(PartKind.EAR_FIN_RIGHT, parts.get(0).kind());
            assertSame(PartKind.EAR_FIN_LEFT, parts.get(1).kind());
            // The two sides always match.
            assertEquals(parts.get(0).shape().style(), parts.get(1).shape().style());
            assertEquals(parts.get(0).shape().size(), parts.get(1).shape().size());
            assertEquals(parts.get(0).shown(), parts.get(1).shown());
            assertEquals(parts.get(0).stretch(), parts.get(1).stretch());
        }
        assertEquals("ear-fins-carrier",
                GENE.expressionOf(Genotype.parse(Codes.of("ear_fins", "Efn/n")).pair(GENE)).id());
        assertFalse(GENE.affectsCoat());
        for (PartKind kind : List.of(PartKind.EAR_FIN_RIGHT, PartKind.EAR_FIN_LEFT)) {
            assertTrue(kind.showsOnFoal(), "the soft head part: a foal wears it");
            assertFalse(kind.saddleZoned());
            assertTrue(kind.scalesPerElement(), "each ray is its own tree, so each is its base colour whole");
            assertEquals(0, kind.translucentRegions(), "no membrane between the rays yet");
        }
        assertSame(PartAnchor.EAR_RIGHT, PartKind.EAR_FIN_RIGHT.anchor());
        assertSame(PartAnchor.EAR_LEFT, PartKind.EAR_FIN_LEFT.anchor());
        // The hard head parts are the other way about.
        assertFalse(PartKind.CHEEK_SPIKE_LEFT.showsOnFoal());
        assertFalse(PartKind.BROW_RIDGE.showsOnFoal());
    }

    /** Every number on the copy reaches the fins: size, form, spread, rays. */
    @Test
    void everyEpigeneticNumberReachesTheFins() {
        Genotype gt = horse(Sex.FEMALE, "ear_fins", "Efn/Efn");
        Set<Integer> sizes = new HashSet<>();
        Set<Integer> forms = new HashSet<>();
        Set<Integer> spreads = new HashSet<>();
        Set<Float> grows = new HashSet<>();
        Set<Float> counts = new HashSet<>();
        for (long seed = 0; seed < 1500; seed++) {
            AttachedPart side = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            int form = EarFinGenerator.formOf(side.shape().style());
            sizes.add(side.shape().size());
            forms.add(form);
            grows.add(side.stretch());
            assertEquals(side.stretch(), side.girth(), "size is a uniform grow");
            if (form == EarFinGenerator.BLADE) {
                assertEquals(0, EarFinGenerator.spreadOf(side.shape().style()), "every blade shares a mesh");
                assertEquals(AttachedPart.ALL_ELEMENTS, side.shown(), "a blade has nothing to count");
            } else {
                spreads.add(EarFinGenerator.spreadOf(side.shape().style()));
                counts.add(side.shown());
                assertTrue(side.shown() >= 2f && side.shown() <= EarFinGenerator.maxRays(form),
                        "a wild fin shows two rays up to its form's most: " + side.shown());
            }
        }
        assertEquals(EarFinSize.classes(), sizes.size(), "size: " + sizes);
        assertEquals(EarFinGenerator.FORMS, forms.size(), "form does not reach every shape");
        assertEquals(EarFinGenerator.SPREADS, spreads.size(), "spread does not reach every bucket");
        assertTrue(grows.size() > 100);
        assertTrue(counts.size() > 100);
    }

    /**
     * The mesh the count and the two-tone rule rely on: every ray is a flat slab rooted on
     * the anchor, turned about x and leaned out, with one last box on it (its point, or a
     * frill's rod) in the tip region - so the slab is the base colour whole and the box on
     * it the tip. The rays are numbered 0..n-1 with none missing, the longest first; a
     * blade is one group.
     */
    @Test
    void everyRayIsRootedOnTheAnchorLongestFirst() {
        for (PartShape shape : finShapes()) {
            List<PartNode> nodes = PartGenerators.build(shape);
            int form = EarFinGenerator.formOf(shape.style());
            int rays = Math.max(1, EarFinGenerator.maxRays(form));
            assertEquals(2 * rays, nodes.size(), shape + ": a ray is a slab and one box on it");
            float[] length = new float[rays];
            for (int i = 0; i < nodes.size(); i += 2) {
                PartNode slab = nodes.get(i);
                PartNode last = nodes.get(i + 1);
                assertTrue(slab.isRoot(), shape + " ray " + i + " is not rooted on the anchor");
                assertEquals(PartSheet.HORN, slab.tex(), shape + " ray " + i);
                assertTrue(slab.width() < slab.girth(), shape + " ray " + i + " is not flat");
                assertEquals(EarFinGenerator.LEAN, Math.abs(slab.rz()), 1e-6f, shape + " ray " + i + " lean");
                assertEquals(0f, slab.ox());
                assertEquals(0f, slab.oy());
                assertEquals(0f, slab.oz());
                assertTrue(slab.group() >= 0 && slab.group() < rays, shape + " group " + slab.group());
                assertEquals(0f, length[slab.group()], shape + " group " + slab.group() + " twice");
                length[slab.group()] = slab.len();
                assertEquals(i, last.parent(), shape + " box " + (i + 1) + " is not on its ray");
                assertEquals(slab.group(), last.group(), shape + " box " + (i + 1) + " left its ray's group");
                assertEquals(PartSheet.HORN_TIP, last.tex(), shape + " box " + (i + 1));
            }
            for (int g = 1; g < rays; g++) {
                assertTrue(length[g] > 0f && length[g] <= length[g - 1],
                        shape + ": ray " + g + " is longer than ray " + (g - 1));
            }
        }
        assertEquals(0, EarFinGenerator.maxRays(EarFinGenerator.BLADE));
        assertEquals(3, EarFinGenerator.maxRays(EarFinGenerator.FAN));
        assertEquals(EarFinGenerator.MAX_RAYS, EarFinGenerator.maxRays(EarFinGenerator.FRILL));
    }

    /** A wider spread opens the fan further; a bigger class is a longer ray. */
    @Test
    void spreadAndSizeChangeTheMesh() {
        for (int form : new int[] {EarFinGenerator.FAN, EarFinGenerator.FRILL}) {
            assertTrue(opening(EarFinGenerator.style(form, EarFinGenerator.SPREADS - 1))
                    > opening(EarFinGenerator.style(form, 0)), "form " + form);
        }
        float small = PartGenerators.build(new PartShape(PartKind.EAR_FIN_RIGHT, 0, 0)).get(0).len();
        float grand = PartGenerators.build(new PartShape(PartKind.EAR_FIN_RIGHT, 0, EarFinSize.classes() - 1))
                .get(0).len();
        assertTrue(grand > small);
        // The style index round-trips, and every blade is the one style whatever its spread.
        Set<Integer> seen = new HashSet<>();
        for (int f = 0; f < EarFinGenerator.FORMS; f++) {
            for (int s = 0; s < EarFinGenerator.SPREADS; s++) {
                int style = EarFinGenerator.style(f, s);
                assertEquals(f, EarFinGenerator.formOf(style));
                if (f == EarFinGenerator.BLADE) {
                    assertEquals(0, style);
                    seen.add(style);
                } else {
                    assertEquals(s, EarFinGenerator.spreadOf(style));
                    assertTrue(seen.add(style));
                }
            }
        }
        assertEquals(EarFinGenerator.styles(), seen.size());
    }

    /** The angle between a fin's front-most and back-most ray. */
    private static float opening(int style) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (PartNode node : PartGenerators.build(new PartShape(PartKind.EAR_FIN_RIGHT, style, 0))) {
            if (node.isRoot()) {
                min = Math.min(min, node.rx());
                max = Math.max(max, node.rx());
            }
        }
        return max - min;
    }

    /** The left fin is the right one mirrored, box for box - never a negative scale. */
    @Test
    void theLeftSideIsTheRightMirrored() {
        for (PartShape right : finShapes()) {
            if (right.kind() != PartKind.EAR_FIN_RIGHT) {
                continue;
            }
            List<PartNode> r = PartGenerators.build(right);
            List<PartNode> l = PartGenerators.build(new PartShape(PartKind.EAR_FIN_LEFT, right.style(), right.size()));
            assertEquals(r.size(), l.size());
            for (int i = 0; i < r.size(); i++) {
                PartNode a = r.get(i);
                PartNode b = l.get(i);
                assertEquals(a.parent(), b.parent());
                assertEquals(-a.ox(), b.ox(), 1e-6f);
                assertEquals(a.rx(), b.rx());
                assertEquals(-a.rz(), b.rz(), 1e-6f, right + " box " + i + " lean");
                assertEquals(a.len(), b.len());
                assertEquals(a.girth(), b.girth());
                assertEquals(a.width(), b.width());
                assertEquals(a.group(), b.group());
                assertEquals(a.tex(), b.tex());
            }
            // A positive turn about z takes a box's up toward +x: the right fin leans to -x.
            assertTrue(r.get(0).rz() < 0f, right + ": the right fin leans out to -x");
        }
    }

    /** White by default; the colour locus dresses both sides; bone is only a colour here. */
    @Test
    void theColourLocusDressesBothSides() {
        Epigenome epi = Epigenome.fromSeed(5);
        AttachedPart plain = GrownParts.of(horse(Sex.MALE, "ear_fins", "Efn/Efn"), epi).get(0);
        for (int tint : new int[] {plain.baseTint(), plain.tipTint()}) {
            assertTrue((tint >> 16 & 0xFF) > 0xC0 && (tint >> 8 & 0xFF) > 0xC0 && (tint & 0xFF) > 0xC0,
                    "white: " + Integer.toHexString(tint));
        }
        List<AttachedPart> twoTone = GrownParts.of(horse(Sex.MALE, "ear_fins", "Efn/Efn",
                "ear_fin_colour", "Red/Blu"), epi);
        for (AttachedPart side : twoTone) {
            assertTrue(side.twoTone());
            assertEquals(twoTone.get(0).baseTint(), side.baseTint(), "the sides match");
            assertEquals(twoTone.get(0).tipTint(), side.tipTint(), "the sides match");
        }
        assertEquals(twoTone, GrownParts.of(horse(Sex.MALE, "ear_fins", "Efn/Efn",
                "ear_fin_colour", "Blu/Red"), epi), "the base is fixed by the pair, not the order");
        // No bone version (owner, 2026-10-09): Bon/Bon colours the fins and leaves nothing off.
        List<AttachedPart> bone = GrownParts.of(horse(Sex.MALE, "ear_fins", "Efn/Efn",
                "ear_fin_colour", "Bon/Bon"), epi);
        assertEquals(2, bone.size());
        assertEquals(plain.shape(), bone.get(0).shape());
        assertEquals(plain.shown(), bone.get(0).shown());
        assertTrue(GrownParts.of(horse(Sex.MALE, "ear_fin_colour", "Red/Blu"), epi).isEmpty(),
                "without fins the colour does nothing");
    }

    /** Each part has its own colour, and the head parts stack. */
    @Test
    void theFinsStackWithTheOtherHeadPartsAndAreColouredApart() {
        Epigenome epi = Epigenome.fromSeed(9);
        List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "ear_fins", "Efn/Efn",
                "dragon_horns", "Drg/Drg", "ear_fin_colour", "Red/Red", "dragon_horn_colour", "Blk/Blk"), epi);
        assertEquals(4, parts.size(), "a pair of dragon horns and a pair of fins stack");
        for (AttachedPart part : parts) {
            boolean red = (part.baseTint() >> 16 & 0xFF) > 0x80;
            assertEquals(part.kind().earFin(), red, part.kind() + " has the other part's colour");
        }
    }

    @Test
    void bothAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.EAR_FINS, Genes.EAR_FIN_COLOUR)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
            assertFalse(g.isNatural());
            assertFalse(g.affectsCoat(), g.key() + " must not reach the coat texture");
        }
        assertEquals(List.of(new GrownParts.Listed("Ear fins", false)),
                GrownParts.listed(horse(Sex.MALE, "ear_fins", "Efn/Efn"), Epigenome.fromSeed(2)),
                "the designer names the pair once, and a foal wears it");
        assertSame(EarFinSize.GRAND, EarFinSize.of(1.0));
        assertTrue(GENE.sizeOf(horse(Sex.MALE, "ear_fins", "Efn/n"), Epigenome.fromSeed(2)).isEmpty());
    }

    /** The unicorn's rarity: about one wild horse in four hundred grows them, one in ten carries. */
    @Test
    void finsAreRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int finned = 0;
        int carriers = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int count = Genotype.random(rng).pair(GENE).count(GENE.Efn);
            finned += count == 2 ? 1 : 0;
            carriers += count == 1 ? 1 : 0;
        }
        assertTrue(finned > 170 && finned < 340, "Efn/Efn in " + n + ": " + finned);
        assertTrue(carriers > 8_500 && carriers < 10_500, "carriers in " + n + ": " + carriers);
    }
}
