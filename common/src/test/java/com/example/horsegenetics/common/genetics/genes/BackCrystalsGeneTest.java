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
import com.example.horsegenetics.common.parts.CrystalGenerator;
import com.example.horsegenetics.common.parts.CrystalSize;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartGenerators;
import com.example.horsegenetics.common.parts.PartKind;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartShape;
import com.example.horsegenetics.common.parts.PartSheet;
import com.example.horsegenetics.common.parts.SaddleZone;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The crystal growths and their colour locus - the last of the body parts. */
class BackCrystalsGeneTest {

    private static final BackCrystalsGene GENE = Genes.BACK_CRYSTALS;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static List<PartShape> crystalShapes() {
        return PartGenerators.allShapes().stream().filter(s -> s.kind() == PartKind.CRYSTALS).toList();
    }

    /** Dominant: one copy grows the crystals, on mares and stallions alike; none shows nothing. */
    @Test
    void oneCopyGrowsTheCrystalsOnEitherSex() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            assertTrue(GrownParts.of(horse(sex, "back_crystals", "n/n"), epi).isEmpty(), sex + " wild");
            for (String pair : List.of("Crg/n", "Crg/Crg")) {
                List<AttachedPart> parts = GrownParts.of(horse(sex, "back_crystals", pair), epi);
                assertEquals(1, parts.size(), sex + " " + pair);
                assertSame(PartKind.CRYSTALS, parts.get(0).kind());
            }
        }
        assertNotEquals(GENE.expressionOf(Genotype.parse(Codes.of("back_crystals", "Crg/n")).pair(GENE)).id(),
                GENE.expressionOf(Genotype.wildType().pair(GENE)).id(), "a carrier shows");
        PartKind kind = PartKind.CRYSTALS;
        assertFalse(kind.showsOnFoal(), "a hard part comes with maturity");
        assertTrue(kind.saddleZoned(), "a row along the back passes under the saddle");
        assertTrue(kind.scalesPerElement());
        assertSame(PartAnchor.SPINE, kind.anchor());
        assertEquals(PartSheet.bit(PartSheet.CRYSTAL), kind.translucentRegions(),
                "the shafts are see-through and nothing else");
    }

    /** Every number on the copy reaches the crystals: seed, size, count, spread. */
    @Test
    void everyEpigeneticNumberReachesTheCrystals() {
        Genotype gt = horse(Sex.FEMALE, "back_crystals", "Crg/Crg");
        Set<Integer> sizes = new HashSet<>();
        Set<Integer> variants = new HashSet<>();
        Set<Integer> spreads = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> counts = new HashSet<>();
        for (long seed = 0; seed < 1500; seed++) {
            AttachedPart part = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            int style = part.shape().style();
            sizes.add(part.shape().size());
            variants.add(CrystalGenerator.variantOf(style));
            spreads.add(CrystalGenerator.spreadOf(style));
            stretches.add(part.stretch());
            counts.add(part.shown());
            assertEquals(part.stretch(), part.girth(), "size is a uniform grow");
            assertEquals(AttachedPart.CRYSTAL_ALPHA, part.opacity(), "the shafts are see-through");
            assertTrue(part.translucent());
            assertTrue(part.shown() >= 2f && part.shown() <= CrystalGenerator.MAX_CLUSTERS,
                    "a wild count is two clusters to a full row: " + part.shown());
        }
        assertEquals(CrystalSize.classes(), sizes.size(), "size: " + sizes);
        assertEquals(CrystalGenerator.VARIANTS, variants.size(), "the seed does not reach every arrangement");
        assertEquals(CrystalGenerator.SPREADS, spreads.size(), "spread does not reach every bucket");
        assertTrue(stretches.size() > 100);
        assertTrue(counts.size() > 100);
    }

    /**
     * The seed is the arrangement and nothing else moves it: a horse whose size, count
     * and spread differ but whose seed is the same keeps its arrangement.
     */
    @Test
    void theSeedPicksTheArrangementAlone() {
        Set<Integer> seen = new HashSet<>();
        for (long seed = 0; seed < 400; seed++) {
            int v = BackCrystalsGene.variantOf(seed);
            assertTrue(v >= 0 && v < CrystalGenerator.VARIANTS);
            seen.add(v);
        }
        assertEquals(CrystalGenerator.VARIANTS, seen.size());
        for (int v = 0; v < CrystalGenerator.VARIANTS; v++) {
            int[] expected = CrystalGenerator.crystalsPerCluster(v);
            for (int spread = 0; spread < CrystalGenerator.SPREADS; spread++) {
                for (int size = 0; size < CrystalSize.classes(); size++) {
                    assertArrayEquals(expected, crystalsIn(new PartShape(PartKind.CRYSTALS,
                            CrystalGenerator.style(v, spread), size)),
                            "arrangement " + v + " changes with spread " + spread + " or size " + size);
                }
            }
        }
    }

    /** The arrangements are different meshes, not one repeated. */
    @Test
    void theArrangementsDiffer() {
        Set<List<PartNode>> meshes = new HashSet<>();
        for (int v = 0; v < CrystalGenerator.VARIANTS; v++) {
            meshes.add(PartGenerators.build(new PartShape(PartKind.CRYSTALS, CrystalGenerator.style(v, 1), 1)));
        }
        assertEquals(CrystalGenerator.VARIANTS, meshes.size());
    }

    /** The style index round-trips: every arrangement and spread is its own mesh. */
    @Test
    void theStyleIndexRoundTrips() {
        Set<Integer> seen = new HashSet<>();
        for (int v = 0; v < CrystalGenerator.VARIANTS; v++) {
            for (int s = 0; s < CrystalGenerator.SPREADS; s++) {
                int style = CrystalGenerator.style(v, s);
                assertEquals(v, CrystalGenerator.variantOf(style));
                assertEquals(s, CrystalGenerator.spreadOf(style));
                assertTrue(seen.add(style));
            }
        }
        assertEquals(CrystalGenerator.styles(), seen.size());
    }

    /**
     * What the count, the saddle and the per-cluster grow rely on: every box is in a
     * group, the groups are the clusters 0..MAX-1 with none missing, each cluster is one
     * tree whose root is its central crystal standing upright, and each cluster has three
     * to seven crystals of three boxes each - two crossed see-through shafts and a solid
     * cap. Clusters run front to back along the row.
     */
    @Test
    void eachClusterIsOneTreeOfCrossedPrismsWithSolidPoints() {
        for (PartShape shape : crystalShapes()) {
            List<PartNode> nodes = PartGenerators.build(shape);
            int[] roots = new int[CrystalGenerator.MAX_CLUSTERS];
            int[] shafts = new int[CrystalGenerator.MAX_CLUSTERS];
            int[] caps = new int[CrystalGenerator.MAX_CLUSTERS];
            float[] rootZ = new float[CrystalGenerator.MAX_CLUSTERS];
            for (PartNode node : nodes) {
                int g = node.group();
                assertTrue(g >= 0 && g < CrystalGenerator.MAX_CLUSTERS, shape + ": a box in group " + g);
                if (node.isRoot()) {
                    roots[g]++;
                    rootZ[g] = node.oz();
                    assertEquals(0f, node.rx(), shape + ": the central crystal leans");
                    assertEquals(0f, node.rz(), shape + ": the central crystal leans");
                } else {
                    assertEquals(g, nodes.get(node.parent()).group(), shape + ": a crystal is on another cluster");
                }
                if (node.tex() == PartSheet.CRYSTAL) {
                    shafts[g]++;
                } else {
                    assertEquals(PartSheet.BONE_TIP, node.tex(), shape + ": a box that is neither shaft nor point");
                    caps[g]++;
                    assertEquals(PartSheet.CRYSTAL, nodes.get(node.parent()).tex(), shape + ": a point off no shaft");
                }
            }
            int[] perCluster = CrystalGenerator.crystalsPerCluster(CrystalGenerator.variantOf(shape.style()));
            for (int g = 0; g < CrystalGenerator.MAX_CLUSTERS; g++) {
                assertEquals(1, roots[g], shape + ": cluster " + g + " roots");
                assertTrue(perCluster[g] >= CrystalGenerator.MIN_CRYSTALS
                        && perCluster[g] <= CrystalGenerator.MAX_CRYSTALS, shape + ": cluster " + g);
                assertEquals(2 * perCluster[g], shafts[g], shape + ": cluster " + g + " shafts");
                assertEquals(perCluster[g], caps[g], shape + ": cluster " + g + " points");
                if (g > 0) {
                    assertTrue(rootZ[g] > rootZ[g - 1], shape + ": cluster " + g + " is not behind " + (g - 1));
                }
            }
        }
    }

    /**
     * The outer crystals fan away from the midline: one standing to the right leans
     * right, one to the left leans left, and a wider spread leans them further.
     */
    @Test
    void theOuterCrystalsFanFromTheMidline() {
        float previous = -1f;
        for (int spread = 0; spread < CrystalGenerator.SPREADS; spread++) {
            float widest = 0f;
            List<PartNode> nodes = PartGenerators.build(new PartShape(PartKind.CRYSTALS,
                    CrystalGenerator.style(2, spread), 1));
            for (PartNode node : nodes) {
                if (node.isRoot() || !nodes.get(node.parent()).isRoot() || node.ox() == 0f) {
                    continue;
                }
                assertTrue(Math.signum(node.rz()) == Math.signum(node.ox()),
                        "spread " + spread + ": a crystal at x " + node.ox() + " leans " + node.rz());
                widest = Math.max(widest, Math.abs(node.rz()));
            }
            assertTrue(widest > previous, "spread " + spread + " fans no further than the one before");
            previous = widest;
        }
    }

    /**
     * Under a saddle the clusters in the middle of the back are hidden and the two at the
     * withers and the croup stay. The saddle range is the client's
     * ({@code AttachedPartLayer}), repeated by value as {@code SaddleZoneTest} does: in
     * the anchor's frame, {@code 0.5..12.5}. The second cluster from the croup roots at
     * {@code 12} give or take its arrangement's jitter, so it may fall either side of the
     * saddle's back edge, and which it is is the arrangement's business.
     */
    @Test
    void theSaddleHidesTheMiddleClusters() {
        float from = -9.5f - 1f + 11f;
        float to = 0.5f + 1f + 11f;
        Set<Boolean> fourth = new HashSet<>();
        for (PartShape shape : crystalShapes()) {
            int mask = SaddleZone.groupsWithin(PartGenerators.build(shape), from, to);
            assertFalse(SaddleZone.hides(mask, 0), shape + ": the withers cluster is under the saddle");
            assertFalse(SaddleZone.hides(mask, CrystalGenerator.MAX_CLUSTERS - 1),
                    shape + ": the croup cluster is under the saddle");
            for (int g = 1; g < CrystalGenerator.MAX_CLUSTERS - 2; g++) {
                assertTrue(SaddleZone.hides(mask, g), shape + ": cluster " + g + " shows through the saddle");
            }
            fourth.add(SaddleZone.hides(mask, CrystalGenerator.MAX_CLUSTERS - 2));
        }
        assertTrue(fourth.contains(true), "no arrangement puts the fourth cluster under the saddle");
    }

    /** A bigger class is a longer, thicker crystal. */
    @Test
    void aBiggerClassIsABiggerCrystal() {
        PartNode small = PartGenerators.build(new PartShape(PartKind.CRYSTALS, 0, 0)).get(0);
        PartNode great = PartGenerators.build(new PartShape(PartKind.CRYSTALS, 0, CrystalSize.classes() - 1)).get(0);
        assertTrue(great.len() > small.len());
        assertTrue(great.girth() > small.girth());
        assertEquals(CrystalSize.SMALL, CrystalSize.of(0.0));
        assertEquals(CrystalSize.GREAT, CrystalSize.of(1.0));
    }

    /**
     * White by default; the colour locus dresses the crystals, the base the see-through
     * shafts and the tip the solid points. Bon/Bon is only a colour: a crystal has no
     * bone, so nothing is left off and it stays see-through.
     */
    @Test
    void theColourLocusDressesTheCrystals() {
        Epigenome epi = Epigenome.fromSeed(5);
        AttachedPart plain = GrownParts.of(horse(Sex.MALE, "back_crystals", "Crg/n"), epi).get(0);
        for (int tint : new int[] {plain.baseTint(), plain.tipTint()}) {
            assertTrue((tint >> 16 & 0xFF) > 0xC0 && (tint >> 8 & 0xFF) > 0xC0 && (tint & 0xFF) > 0xC0,
                    "white: " + Integer.toHexString(tint));
        }
        AttachedPart twoTone = GrownParts.of(horse(Sex.MALE, "back_crystals", "Crg/n",
                "back_crystal_colour", "Red/Blu"), epi).get(0);
        assertTrue(twoTone.twoTone());
        assertEquals(twoTone, GrownParts.of(horse(Sex.MALE, "back_crystals", "Crg/n",
                "back_crystal_colour", "Blu/Red"), epi).get(0), "the base is fixed by the pair, not the order");
        AttachedPart bone = GrownParts.of(horse(Sex.MALE, "back_crystals", "Crg/n",
                "back_crystal_colour", "Bon/Bon"), epi).get(0);
        assertTrue(bone.translucent(), "a bone-coloured crystal is still a crystal");
        assertEquals(AttachedPart.CRYSTAL_ALPHA, bone.opacity());
    }

    /** Each part has its own colour: the crystals' colour leaves the sail alone, and back. */
    @Test
    void theCrystalsAndTheSailAreColouredApart() {
        Epigenome epi = Epigenome.fromSeed(9);
        List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "back_sail", "Sail/Sail",
                "back_crystals", "Crg/n", "back_sail_colour", "Red/Red", "back_crystal_colour", "Blk/Blk"), epi);
        assertEquals(2, parts.size(), "a sail and crystals stack");
        for (AttachedPart part : parts) {
            boolean red = (part.baseTint() >> 16 & 0xFF) > 0x80;
            assertEquals(part.kind() == PartKind.SAIL, red, part.kind() + " has the other part's colour");
        }
    }

    @Test
    void withoutCrystalsTheColourDoesNothing() {
        assertTrue(GrownParts.of(horse(Sex.MALE, "back_crystal_colour", "Red/Blu"), Epigenome.fromSeed(1))
                .isEmpty());
        assertFalse(Genes.BACK_CRYSTAL_COLOUR.partGrows(horse(Sex.MALE, "back_crystal_colour", "Red/Blu")));
        assertTrue(Genes.BACK_CRYSTAL_COLOUR.partGrows(horse(Sex.MALE, "back_crystals", "Crg/n")));
    }

    @Test
    void bothAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.BACK_CRYSTALS, Genes.BACK_CRYSTAL_COLOUR)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
            assertFalse(g.isNatural());
            assertFalse(g.affectsCoat(), g.key() + " must not reach the coat texture");
        }
        assertEquals(List.of(new GrownParts.Listed("Crystal growths", true)),
                GrownParts.listed(horse(Sex.MALE, "back_crystals", "Crg/n"), Epigenome.fromSeed(2)),
                "the designer names the part, adult only");
    }

    /** The other dominant body parts' small share: about three horses in a thousand, every carrier showing. */
    @Test
    void crystalsAreRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int grown = 0;
        int homozygous = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int count = Genotype.random(rng).pair(GENE).count(GENE.Crg);
            grown += count > 0 ? 1 : 0;
            homozygous += count == 2 ? 1 : 0;
        }
        assertTrue(grown > 200 && grown < 420, "Crg in " + n + ": " + grown);
        assertEquals(0, homozygous, "a founder is Crg/n or nothing");
    }

    /** The crystals in each cluster of a mesh, front to back, counted by their points. */
    private static int[] crystalsIn(PartShape shape) {
        int[] out = new int[CrystalGenerator.MAX_CLUSTERS];
        for (PartNode node : PartGenerators.build(shape)) {
            if (node.tex() == PartSheet.BONE_TIP) {
                out[node.group()]++;
            }
        }
        return out;
    }
}
