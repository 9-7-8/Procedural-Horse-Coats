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
import com.example.horsegenetics.common.parts.BrowRidgeGenerator;
import com.example.horsegenetics.common.parts.BrowRidgeSize;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The brow ridge and its colour locus - the one centred head part of the three. */
class BrowRidgeGeneTest {

    private static final BrowRidgeGene GENE = Genes.BROW_RIDGE;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static List<PartShape> ridgeShapes() {
        return PartGenerators.allShapes().stream().filter(s -> s.kind() == PartKind.BROW_RIDGE).toList();
    }

    /** Dominant: one copy grows the ridge, on mares and stallions alike; none shows nothing. */
    @Test
    void oneCopyGrowsOneRidgeOnEitherSex() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            assertTrue(GrownParts.of(horse(sex, "brow_ridge", "n/n"), epi).isEmpty(), sex + " wild");
            for (String pair : List.of("Brw/n", "Brw/Brw")) {
                List<AttachedPart> parts = GrownParts.of(horse(sex, "brow_ridge", pair), epi);
                assertEquals(1, parts.size(), sex + " " + pair);
                assertSame(PartKind.BROW_RIDGE, parts.get(0).kind());
                assertEquals(AttachedPart.ALL_ELEMENTS, parts.get(0).shown(), "a ridge has nothing to count");
            }
        }
        assertNotEquals(GENE.expressionOf(Genotype.parse(Codes.of("brow_ridge", "Brw/n")).pair(GENE)).id(),
                GENE.expressionOf(Genotype.wildType().pair(GENE)).id(), "a carrier shows");
        assertFalse(GENE.affectsCoat());
        assertFalse(PartKind.BROW_RIDGE.showsOnFoal(), "a hard part comes with maturity");
        assertFalse(PartKind.BROW_RIDGE.saddleZoned());
        assertFalse(PartKind.BROW_RIDGE.scalesPerElement());
        assertEquals(0, PartKind.BROW_RIDGE.translucentRegions());
        assertSame(PartAnchor.BROW, PartKind.BROW_RIDGE.anchor());
        // Its own anchor, so a unicorn horn stands behind it rather than in its place.
        List<AttachedPart> both = GrownParts.of(horse(Sex.MALE, "brow_ridge", "Brw/n",
                "unicorn_horn", "Horn/Horn"), epi);
        assertEquals(2, both.size(), "a horn and a ridge stack");
        assertNotEquals(both.get(0).kind().anchor(), both.get(1).kind().anchor());
    }

    /** Every number on the copy reaches the ridge: height, width, form, bump. */
    @Test
    void everyEpigeneticNumberReachesTheRidge() {
        Genotype gt = horse(Sex.FEMALE, "brow_ridge", "Brw/Brw");
        Set<Integer> sizes = new HashSet<>();
        Set<Integer> forms = new HashSet<>();
        Set<Integer> bumps = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> widths = new HashSet<>();
        for (long seed = 0; seed < 1500; seed++) {
            AttachedPart ridge = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            int form = BrowRidgeGenerator.formOf(ridge.shape().style());
            sizes.add(ridge.shape().size());
            forms.add(form);
            stretches.add(ridge.stretch());
            widths.add(ridge.girth());
            if (form == BrowRidgeGenerator.PLAIN) {
                assertEquals(0, BrowRidgeGenerator.bumpOf(ridge.shape().style()), "every plain ridge shares a mesh");
            } else {
                bumps.add(BrowRidgeGenerator.bumpOf(ridge.shape().style()));
            }
            // Width is the part's girth: wild inside the skull's six units (5 x 1.1 = 5.5).
            assertTrue(ridge.girth() >= 0.8f && ridge.girth() <= 1.1f, "a wild width: " + ridge.girth());
            assertEquals(0f, ridge.tilt());
        }
        assertEquals(BrowRidgeSize.classes(), sizes.size(), "height: " + sizes);
        assertEquals(BrowRidgeGenerator.FORMS, forms.size(), "form does not reach every shape");
        assertEquals(BrowRidgeGenerator.BUMPS, bumps.size(), "bump does not reach every bucket");
        assertTrue(stretches.size() > 100);
        assertTrue(widths.size() > 100);
    }

    /**
     * The mesh: one bar laid over a quarter turn, centred on the anchor and as tall as it
     * is deep, with its class's step, bumps or spines standing on it. The bar is bone and
     * every last box polished - the two-tone rule (the bar the base, what stands on it the
     * tip) is exactly that.
     */
    @Test
    void theBarLiesAcrossTheHeadAndWhatStandsOnItIsTheTip() {
        for (PartShape shape : ridgeShapes()) {
            List<PartNode> nodes = PartGenerators.build(shape);
            int form = BrowRidgeGenerator.formOf(shape.style());
            PartNode bar = nodes.get(0);
            assertTrue(bar.isRoot());
            assertEquals(PartSheet.BONE, bar.tex());
            assertEquals(Math.PI / 2, bar.rz(), 1e-6, shape + ": the bar is not laid across");
            assertEquals(BrowRidgeGenerator.WIDTH, bar.len(), 1e-6f);
            assertEquals(-BrowRidgeGenerator.WIDTH / 2f, bar.ox(), 1e-6f, shape + ": the bar is off centre");
            assertEquals(BrowRidgeSize.values()[shape.size()].length(), bar.girth(), 1e-6f,
                    shape + ": the bar is its class height");
            int[] children = new int[nodes.size()];
            int onBar = 0;
            for (int i = 1; i < nodes.size(); i++) {
                PartNode node = nodes.get(i);
                children[node.parent()]++;
                assertEquals(PartNode.NO_GROUP, node.group(), shape + ": nothing on a ridge is counted");
                if (node.parent() == 0) {
                    onBar++;
                    if (form != BrowRidgeGenerator.PLAIN) {
                        assertEquals(-Math.PI / 2, node.rz(), 1e-6, shape + " box " + i + " does not stand up");
                        assertTrue(node.len() > bar.girth() / 2f, shape + " box " + i + " is buried in the bar");
                    }
                }
            }
            for (int i = 1; i < nodes.size(); i++) {
                if (children[i] == 0) {
                    assertEquals(PartSheet.BONE_TIP, nodes.get(i).tex(), shape + " last box " + i);
                }
            }
            assertEquals(form == BrowRidgeGenerator.PLAIN ? 1 : 3, onBar, shape + " boxes on the bar");
            assertEquals(form == BrowRidgeGenerator.PLAIN ? 2 : (form == BrowRidgeGenerator.NOTCHED ? 4 : 7),
                    nodes.size(), shape.toString());
        }
    }

    /** A higher bump bucket stands taller; a bigger class is a taller bar; the style index round-trips. */
    @Test
    void bumpAndHeightChangeTheMesh() {
        for (int form : new int[] {BrowRidgeGenerator.NOTCHED, BrowRidgeGenerator.SPINED}) {
            float low = PartGenerators.build(new PartShape(PartKind.BROW_RIDGE,
                    BrowRidgeGenerator.style(form, 0), 1)).get(1).len();
            float high = PartGenerators.build(new PartShape(PartKind.BROW_RIDGE,
                    BrowRidgeGenerator.style(form, BrowRidgeGenerator.BUMPS - 1), 1)).get(1).len();
            assertTrue(high > low, "form " + form + ": " + low + " vs " + high);
        }
        float lowBar = PartGenerators.build(new PartShape(PartKind.BROW_RIDGE, 0, 0)).get(0).girth();
        float tallBar = PartGenerators.build(new PartShape(PartKind.BROW_RIDGE, 0, BrowRidgeSize.classes() - 1))
                .get(0).girth();
        assertTrue(tallBar > lowBar);
        Set<Integer> seen = new HashSet<>();
        for (int f = 0; f < BrowRidgeGenerator.FORMS; f++) {
            for (int b = 0; b < BrowRidgeGenerator.BUMPS; b++) {
                int style = BrowRidgeGenerator.style(f, b);
                assertEquals(f, BrowRidgeGenerator.formOf(style));
                if (f == BrowRidgeGenerator.PLAIN) {
                    // Every plain ridge is the one style whatever its bump bucket.
                    assertEquals(0, style);
                    seen.add(style);
                } else {
                    assertEquals(b, BrowRidgeGenerator.bumpOf(style));
                    assertTrue(seen.add(style));
                }
            }
        }
        assertEquals(BrowRidgeGenerator.styles(), seen.size());
    }

    /** White by default; two different copies are two-tone; Bon/Bon is the bone version. */
    @Test
    void theColourLocusDressesTheRidge() {
        Epigenome epi = Epigenome.fromSeed(5);
        AttachedPart plain = GrownParts.of(horse(Sex.MALE, "brow_ridge", "Brw/n"), epi).get(0);
        for (int tint : new int[] {plain.baseTint(), plain.tipTint()}) {
            assertTrue((tint >> 16 & 0xFF) > 0xC0 && (tint >> 8 & 0xFF) > 0xC0 && (tint & 0xFF) > 0xC0,
                    "white: " + Integer.toHexString(tint));
        }
        AttachedPart twoTone = GrownParts.of(horse(Sex.MALE, "brow_ridge", "Brw/n",
                "brow_ridge_colour", "Red/Blu"), epi).get(0);
        assertTrue(twoTone.twoTone());
        assertEquals(twoTone, GrownParts.of(horse(Sex.MALE, "brow_ridge", "Brw/n",
                "brow_ridge_colour", "Blu/Red"), epi).get(0), "the base is fixed by the pair, not the order");
        AttachedPart bone = GrownParts.of(horse(Sex.MALE, "brow_ridge", "Brw/n",
                "brow_ridge_colour", "Bon/Bon"), epi).get(0);
        int r = bone.baseTint() >> 16 & 0xFF;
        int b = bone.baseTint() & 0xFF;
        assertTrue(r > 0x90 && r >= b, "bone is pale and warm: " + Integer.toHexString(bone.baseTint()));
        assertFalse(bone.translucent());
        assertFalse(bone.emissive());
        assertTrue(GrownParts.of(horse(Sex.MALE, "brow_ridge_colour", "Red/Blu"), epi).isEmpty(),
                "without a ridge the colour does nothing");
        // Its own locus: the horn's colour leaves the ridge alone.
        assertEquals(plain.baseTint(), GrownParts.of(horse(Sex.MALE, "brow_ridge", "Brw/n",
                "horn_colour", "Red/Red"), epi).get(0).baseTint());
    }

    @Test
    void bothAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.BROW_RIDGE, Genes.BROW_RIDGE_COLOUR)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
            assertFalse(g.isNatural());
            assertFalse(g.affectsCoat(), g.key() + " must not reach the coat texture");
        }
        assertEquals(List.of(new GrownParts.Listed("Brow ridge", true)),
                GrownParts.listed(horse(Sex.MALE, "brow_ridge", "Brw/n"), Epigenome.fromSeed(2)),
                "the designer names it, adult only");
        assertSame(BrowRidgeSize.TOWERING, BrowRidgeSize.of(1.0));
    }

    /** The plates' small share: about three horses in a thousand grow one, and every carrier shows. */
    @Test
    void ridgesAreRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int ridged = 0;
        int homozygous = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int count = Genotype.random(rng).pair(GENE).count(GENE.Brw);
            ridged += count > 0 ? 1 : 0;
            homozygous += count == 2 ? 1 : 0;
        }
        assertTrue(ridged > 200 && ridged < 420, "Brw in " + n + ": " + ridged);
        assertEquals(0, homozygous, "a founder is Brw/n or nothing");
    }
}
