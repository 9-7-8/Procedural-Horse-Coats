package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartGenerators;
import com.example.horsegenetics.common.parts.PartKind;
import com.example.horsegenetics.common.parts.PartNode;
import com.example.horsegenetics.common.parts.PartShape;
import com.example.horsegenetics.common.parts.RamHornGenerator;
import com.example.horsegenetics.common.parts.RamHornSize;
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

/** The ram horn loci - the antler pattern again - and the molten hooves allele that matches them. */
class RamHornsGeneTest {

    private static final RamHornsGene GENE = Genes.RAM_HORNS;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    @Test
    void rhHornsBothSexesAndRhmOnlyStallions() {
        Epigenome epi = Epigenome.fromSeed(3);
        assertTrue(GrownParts.of(horse(Sex.FEMALE, "ram_horns", "Rhm/n"), epi).isEmpty());
        assertEquals(2, GrownParts.of(horse(Sex.MALE, "ram_horns", "Rhm/n"), epi).size());
        assertEquals(2, GrownParts.of(horse(Sex.FEMALE, "ram_horns", "Rh/n"), epi).size());
        assertTrue(GrownParts.of(horse(Sex.MALE, "ram_horns", "n/n"), epi).isEmpty());
        List<AttachedPart> pair = GrownParts.of(horse(Sex.FEMALE, "ram_horns", "Rh/Rh"), epi);
        assertSame(PartKind.RAM_HORN_RIGHT, pair.get(0).kind());
        assertSame(PartKind.RAM_HORN_LEFT, pair.get(1).kind());
        assertFalse(PartKind.RAM_HORN_LEFT.showsOnFoal());
        assertTrue(GENE.expressionOf(pair("Rh/n")).wildType());
        assertFalse(GENE.affectsCoat());
    }

    private static AllelePair pair(String tokens) {
        return Genotype.parse(Codes.of("ram_horns", tokens)).pair(GENE);
    }

    /** Every number on the copy reaches the horns: size, curl, girth, shade. */
    @Test
    void everyEpigeneticNumberReachesTheHorns() {
        Genotype gt = horse(Sex.FEMALE, "ram_horns", "Rh/n");
        Set<Integer> sizes = new HashSet<>();
        Set<Integer> curls = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> girths = new HashSet<>();
        Set<Integer> shades = new HashSet<>();
        for (long seed = 0; seed < 1500; seed++) {
            AttachedPart right = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            sizes.add(right.shape().size());
            curls.add(right.shape().style() % RamHornGenerator.CURLS);
            stretches.add(right.stretch());
            girths.add(right.girth());
            shades.add(right.baseTint());
        }
        assertTrue(sizes.size() >= 3, "size: " + sizes);
        assertEquals(RamHornGenerator.CURLS, curls.size(), "curl does not reach every bucket");
        assertTrue(stretches.size() > 100);
        assertTrue(girths.size() > 100);
        assertTrue(shades.size() > 50);
    }

    @Test
    void theFormIsADominanceSeriesOnBothSides() {
        RamHornFormGene form = Genes.RAM_HORN_FORM;
        assertEquals(RamHornGenerator.FOUR,
                form.shapeOf(Genotype.parse(Codes.of("ram_horn_form", "Fhn/Scr")).pair(form)));
        assertEquals(RamHornGenerator.CURL,
                form.shapeOf(Genotype.parse(Codes.of("ram_horn_form", "Crl/Crk")).pair(form)));
        assertEquals(RamHornGenerator.CORKSCREW,
                form.shapeOf(Genotype.parse(Codes.of("ram_horn_form", "Crk/Scr")).pair(form)));
        assertEquals(RamHornGenerator.SCURS,
                form.shapeOf(Genotype.parse(Codes.of("ram_horn_form", "Scr/Scr")).pair(form)));
        for (AttachedPart side : GrownParts.of(
                horse(Sex.MALE, "ram_horns", "Rh/n", "ram_horn_form", "Crk/Crk"), Epigenome.fromSeed(1))) {
            assertEquals(RamHornGenerator.CORKSCREW, side.shape().style() / RamHornGenerator.CURLS);
        }
    }

    /** Hzt sits under corkscrew and over scurs, was appended (saves), and reaches both horns. */
    @Test
    void theHorizontalTwistSitsBetweenCorkscrewAndScurs() {
        RamHornFormGene form = Genes.RAM_HORN_FORM;
        assertEquals(List.of("Crl", "Crk", "Fhn", "Scr", "Hzt"),
                form.alleles().stream().map(a -> a.token()).toList().subList(0, 5),
                "allele order is saved: append only");
        assertEquals(RamHornGenerator.FORMS, form.alleles().size());
        assertEquals(form.alleles().size(), form.expressions().size());
        for (int i = 0; i < form.alleles().size(); i++) {
            assertEquals(i, form.alleles().get(i).order());
        }
        assertEquals(RamHornGenerator.HORIZONTAL, shape(form, "Hzt/Hzt"));
        assertEquals(RamHornGenerator.HORIZONTAL, shape(form, "Hzt/Scr"));
        assertEquals(RamHornGenerator.CORKSCREW, shape(form, "Crk/Hzt"));
        assertEquals(RamHornGenerator.CURL, shape(form, "Crl/Hzt"));
        assertEquals(RamHornGenerator.FOUR, shape(form, "Fhn/Hzt"));
        assertEquals("ram-horn-horizontal", form.expressionOf(
                Genotype.parse(Codes.of("ram_horn_form", "Hzt/Scr")).pair(form)).id());
        for (Sex sex : Sex.values()) {
            List<AttachedPart> horns = GrownParts.of(
                    horse(sex, "ram_horns", "Rh/n", "ram_horn_form", "Hzt/Hzt"), Epigenome.fromSeed(3));
            assertEquals(2, horns.size(), "Rh horns either sex: " + sex);
            for (AttachedPart side : horns) {
                assertEquals(RamHornGenerator.HORIZONTAL, side.shape().style() / RamHornGenerator.CURLS);
            }
        }
        assertTrue(GrownParts.of(horse(Sex.FEMALE, "ram_horns", "Rhm/n", "ram_horn_form", "Hzt/Hzt"),
                Epigenome.fromSeed(3)).isEmpty(), "Rhm is a stallion's");
    }

    /** Sci sits under curled and over corkscrew, was appended after Hzt (saves), and reaches both horns. */
    @Test
    void theScimitarSitsBetweenCurledAndCorkscrew() {
        RamHornFormGene form = Genes.RAM_HORN_FORM;
        assertEquals(List.of("Crl", "Crk", "Fhn", "Scr", "Hzt", "Sci"),
                form.alleles().stream().map(a -> a.token()).toList(), "allele order is saved: append only");
        assertEquals(RamHornGenerator.FORMS, form.alleles().size());
        assertEquals(form.alleles().size(), form.expressions().size());
        for (int i = 0; i < form.alleles().size(); i++) {
            assertEquals(i, form.alleles().get(i).order());
        }
        assertEquals(RamHornGenerator.SCIMITAR, shape(form, "Sci/Sci"));
        assertEquals(RamHornGenerator.SCIMITAR, shape(form, "Crk/Sci"));
        assertEquals(RamHornGenerator.SCIMITAR, shape(form, "Hzt/Sci"));
        assertEquals(RamHornGenerator.SCIMITAR, shape(form, "Scr/Sci"));
        assertEquals(RamHornGenerator.CURL, shape(form, "Crl/Sci"));
        assertEquals(RamHornGenerator.FOUR, shape(form, "Fhn/Sci"));
        assertEquals("ram-horn-scimitar", form.expressionOf(
                Genotype.parse(Codes.of("ram_horn_form", "Crk/Sci")).pair(form)).id());
        for (Sex sex : Sex.values()) {
            List<AttachedPart> horns = GrownParts.of(
                    horse(sex, "ram_horns", "Rh/n", "ram_horn_form", "Sci/Sci"), Epigenome.fromSeed(3));
            assertEquals(2, horns.size(), "Rh horns either sex: " + sex);
            for (AttachedPart side : horns) {
                assertEquals(RamHornGenerator.SCIMITAR, side.shape().style() / RamHornGenerator.CURLS);
            }
        }
        assertTrue(GrownParts.of(horse(Sex.FEMALE, "ram_horns", "Rhm/n", "ram_horn_form", "Sci/Sci"),
                Epigenome.fromSeed(3)).isEmpty(), "Rhm is a stallion's");
        assertEquals(2, GrownParts.of(horse(Sex.MALE, "ram_horns", "Rhm/n", "ram_horn_form", "Sci/Sci"),
                Epigenome.fromSeed(3)).size(), "and a stallion grows the pair");
    }

    /**
     * The scimitar at every curl and size: inside the box budget, one unbranched blade
     * narrower across than deep, bent backward and never rolled, sweeping further by the
     * bucket, and always an arc - well short of the curl's half turn.
     */
    @Test
    void theScimitarIsOneBladeSweptBackByTheCurl() {
        for (int size = 0; size < RamHornSize.classes(); size++) {
            float last = 0f;
            for (int curl = 0; curl < RamHornGenerator.CURLS; curl++) {
                for (boolean left : new boolean[] {true, false}) {
                    List<PartNode> nodes = RamHornGenerator.generate(RamHornGenerator.SCIMITAR, curl, size, left);
                    assertTrue(nodes.size() >= 2 && nodes.size() <= RamHornGenerator.MAX_NODES,
                            nodes.size() + " boxes");
                    PartNode root = nodes.get(0);
                    assertTrue(root.parent() < 0);
                    assertTrue(root.rx() < 0f, "the root leans back, not forward");
                    assertTrue(Math.abs(root.rz()) < Math.toRadians(30), "the root rises, it does not reach sideways");
                    assertEquals(left, root.rz() > 0f, "each side leans outward");
                    float sweep = 0f;
                    for (int i = 0; i < nodes.size(); i++) {
                        PartNode n = nodes.get(i);
                        assertTrue(n.width() < n.girth(), "a blade, box " + i);
                        if (i > 0) {
                            assertEquals(i - 1, n.parent(), "one unbranched chain");
                            assertEquals(0f, n.ry(), "a blade never rolls");
                            assertTrue(n.rx() < 0f, "every box bends backward");
                            assertTrue(n.girth() <= nodes.get(i - 1).girth(), "it tapers");
                            sweep -= n.rx();
                        }
                    }
                    assertTrue(sweep < Math.toRadians(110), "an arc, not a spiral: " + Math.toDegrees(sweep));
                    if (left) {
                        assertTrue(sweep > last, "a higher curl bucket sweeps further back");
                        last = sweep;
                    }
                }
            }
        }
    }

    private static int shape(RamHornFormGene form, String tokens) {
        return form.shapeOf(Genotype.parse(Codes.of("ram_horn_form", tokens)).pair(form));
    }

    /**
     * The horizontal twist at every curl and size: inside the box budget, flat, rolled
     * and never bent, leaving the skull more sideways than up, and tighter by the bucket.
     */
    @Test
    void theHorizontalTwistIsFlatLevelAndTwistsByTheCurl() {
        for (int size = 0; size < RamHornSize.classes(); size++) {
            float last = 0f;
            for (int curl = 0; curl < RamHornGenerator.CURLS; curl++) {
                List<PartNode> nodes = RamHornGenerator.generate(RamHornGenerator.HORIZONTAL, curl, size, true);
                assertTrue(nodes.size() >= 2 && nodes.size() <= RamHornGenerator.MAX_NODES, nodes.size() + " boxes");
                PartNode root = nodes.get(0);
                assertTrue(root.parent() < 0);
                assertTrue(root.rz() > Math.toRadians(60) && root.rz() < Math.toRadians(90),
                        "the root leaves sideways, not up: " + Math.toDegrees(root.rz()));
                float total = 0f;
                for (int i = 0; i < nodes.size(); i++) {
                    PartNode n = nodes.get(i);
                    assertTrue(n.width() < n.girth(), "a flat section, box " + i);
                    if (i > 0) {
                        assertEquals(i - 1, n.parent(), "one unbranched chain");
                        assertEquals(0f, n.rx(), "a twist never bends");
                        assertEquals(0f, n.rz(), "a twist never leans");
                        assertTrue(n.ry() > 0f);
                        total += n.ry();
                    }
                }
                assertTrue(total > last, "a higher curl bucket twists further");
                last = total;
            }
        }
    }

    /** A coloured tip keeps the horn's shade at the base; one copy shows nothing. */
    @Test
    void aTippedHornFadesFromItsShadeIntoTheTip() {
        Epigenome epi = Epigenome.fromSeed(8);
        AttachedPart plain = GrownParts.of(horse(Sex.MALE, "ram_horns", "Rh/n"), epi).get(0);
        AttachedPart tipped = GrownParts.of(
                horse(Sex.MALE, "ram_horns", "Rh/n", "ram_horn_tip", "Tip/Tip"), epi).get(0);
        AttachedPart carrier = GrownParts.of(
                horse(Sex.MALE, "ram_horns", "Rh/n", "ram_horn_tip", "Tip/n"), epi).get(0);
        assertFalse(plain.twoTone());
        assertFalse(carrier.twoTone());
        assertTrue(tipped.twoTone());
        assertEquals(plain.baseTint(), tipped.baseTint(), "the base keeps the horn's own shade");
        assertTrue(GrownParts.of(horse(Sex.MALE, "ram_horn_tip", "Tip/Tip"), epi).isEmpty());
    }

    /** MltH prints in the tip colour when there is one, the horn's shade when there is not. */
    @Test
    void hornTipHoofprintsMatchTheHornTip() {
        MoltenHoovesGene molten = Genes.MOLTEN_HOOVES;
        for (long seed = 0; seed < 50; seed++) {
            Epigenome epi = Epigenome.fromSeed(seed);
            Genotype tipped = horse(Sex.MALE, "ram_horns", "Rh/n", "ram_horn_tip", "Tip/Tip",
                    "molten_hooves", "MltH/MltH");
            int tip = GrownParts.of(tipped, epi).get(0).tipTint() & 0xFFFFFF;
            assertEquals(tip, printColour(molten, tipped, epi), "seed " + seed);
            Genotype untipped = horse(Sex.MALE, "ram_horns", "Rh/n", "molten_hooves", "MltH/MltH");
            int shade = GrownParts.of(untipped, epi).get(0).baseTint() & 0xFFFFFF;
            assertEquals(shade, printColour(molten, untipped, epi), "seed " + seed);
        }
        assertTrue(molten.expressionOf(Genotype.parse(Codes.of("molten_hooves", "MltH/n"))
                .pair(molten)).id().equals("carrier"), "one copy shows nothing");
    }

    private static int printColour(MoltenHoovesGene molten, Genotype g, Epigenome epi) {
        List<GeneAbility> abilities = molten.abilitiesFor(g.pair(molten), g,
                GeneEpigenetics.forGene(molten, g, epi));
        assertEquals(1, abilities.size());
        return ((GeneAbility.Emitter) abilities.get(0)).color();
    }

    @Test
    void everyRamHornMeshIsSmallMirroredAndGrowsWithItsClass() {
        for (PartShape shape : PartGenerators.allShapes()) {
            if (!shape.kind().ramHorn()) {
                continue;
            }
            List<PartNode> nodes = PartGenerators.build(shape);
            assertTrue(nodes.size() <= RamHornGenerator.MAX_NODES, shape + " is " + nodes.size() + " boxes");
            if (shape.kind() == PartKind.RAM_HORN_RIGHT) {
                List<PartNode> left = PartGenerators.build(
                        new PartShape(PartKind.RAM_HORN_LEFT, shape.style(), shape.size()));
                assertEquals(nodes.size(), left.size());
                for (int i = 0; i < nodes.size(); i++) {
                    assertEquals(-nodes.get(i).rz(), left.get(i).rz(), 1e-6f, shape + " box " + i);
                    assertEquals(nodes.get(i).rx(), left.get(i).rx(), 1e-6f);
                }
            }
        }
        int small = PartGenerators.build(new PartShape(PartKind.RAM_HORN_RIGHT, 0, 0)).size();
        int massive = PartGenerators.build(
                new PartShape(PartKind.RAM_HORN_RIGHT, 0, RamHornSize.classes() - 1)).size();
        assertTrue(massive > small);
        assertNotEquals(PartGenerators.build(new PartShape(PartKind.RAM_HORN_RIGHT, 0, 2)),
                PartGenerators.build(new PartShape(PartKind.RAM_HORN_RIGHT, 3, 2)),
                "the curl bucket changes the mesh");
    }

    @Test
    void allThreeAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.RAM_HORNS, Genes.RAM_HORN_FORM, Genes.RAM_HORN_TIP)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
        }
    }

    @Test
    void ramHornsAreRareInTheWild() {
        SeededRng rng = new SeededRng(77L);
        int horned = 0;
        for (int i = 0; i < 100_000; i++) {
            AllelePair p = Genotype.random(rng).pair(GENE);
            horned += p.count(GENE.n) < 2 ? 1 : 0;
        }
        assertTrue(horned > 250 && horned < 600, "carriers of either allele: " + horned);
    }
}
