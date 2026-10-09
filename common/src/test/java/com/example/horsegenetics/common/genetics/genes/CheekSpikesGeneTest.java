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
import com.example.horsegenetics.common.parts.CheekSpikeGenerator;
import com.example.horsegenetics.common.parts.CheekSpikeSize;
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

/** The cheek spikes and their colour locus - a row on the head that scales per spike. */
class CheekSpikesGeneTest {

    private static final CheekSpikesGene GENE = Genes.CHEEK_SPIKES;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static List<PartShape> spikeShapes() {
        return PartGenerators.allShapes().stream().filter(s -> s.kind().cheekSpike()).toList();
    }

    /** Dominant: one copy grows both cheeks, on mares and stallions alike; none shows nothing. */
    @Test
    void oneCopyGrowsBothCheeksOnEitherSex() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            assertTrue(GrownParts.of(horse(sex, "cheek_spikes", "n/n"), epi).isEmpty(), sex + " wild");
            for (String pair : List.of("Chk/n", "Chk/Chk")) {
                List<AttachedPart> parts = GrownParts.of(horse(sex, "cheek_spikes", pair), epi);
                assertEquals(2, parts.size(), sex + " " + pair);
                assertSame(PartKind.CHEEK_SPIKE_RIGHT, parts.get(0).kind());
                assertSame(PartKind.CHEEK_SPIKE_LEFT, parts.get(1).kind());
                assertEquals(parts.get(0).shape().size(), parts.get(1).shape().size());
                assertEquals(parts.get(0).shown(), parts.get(1).shown());
                assertEquals(parts.get(0).stretch(), parts.get(1).stretch());
                assertEquals(parts.get(0).girth(), parts.get(1).girth());
            }
        }
        assertNotEquals(GENE.expressionOf(Genotype.parse(Codes.of("cheek_spikes", "Chk/n")).pair(GENE)).id(),
                GENE.expressionOf(Genotype.wildType().pair(GENE)).id(), "a carrier shows");
        assertFalse(GENE.affectsCoat());
        for (PartKind kind : List.of(PartKind.CHEEK_SPIKE_RIGHT, PartKind.CHEEK_SPIKE_LEFT)) {
            assertFalse(kind.showsOnFoal(), "a hard part comes with maturity");
            assertFalse(kind.saddleZoned());
            assertTrue(kind.scalesPerElement(), "a spike points across the anchor's up");
            assertEquals(0, kind.translucentRegions());
            assertEquals(1, kind.styles(), "one style: everything else is draw-time");
        }
        assertSame(PartAnchor.CHEEK_RIGHT, PartKind.CHEEK_SPIKE_RIGHT.anchor());
        assertSame(PartAnchor.CHEEK_LEFT, PartKind.CHEEK_SPIKE_LEFT.anchor());
    }

    /** Every number on the copy reaches the spikes: length, girth, count. */
    @Test
    void everyEpigeneticNumberReachesTheSpikes() {
        Genotype gt = horse(Sex.FEMALE, "cheek_spikes", "Chk/Chk");
        Set<Integer> sizes = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> ratios = new HashSet<>();
        Set<Float> counts = new HashSet<>();
        for (long seed = 0; seed < 1500; seed++) {
            AttachedPart side = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            sizes.add(side.shape().size());
            stretches.add(side.stretch());
            float ratio = side.girth() / side.stretch();
            ratios.add(ratio);
            counts.add(side.shown());
            assertTrue(ratio > 0.79f && ratio < 1.26f, "a wild girth is 0.8 to 1.25: " + ratio);
            assertTrue(side.shown() >= 2f && side.shown() <= CheekSpikeGenerator.MAX_SPIKES,
                    "a wild count is two to four a cheek: " + side.shown());
        }
        assertEquals(CheekSpikeSize.classes(), sizes.size(), "length: " + sizes);
        assertTrue(stretches.size() > 100);
        assertTrue(ratios.size() > 100);
        assertTrue(counts.size() > 100);
    }

    /**
     * The numbering the count and the per-element scale rely on: every box is in a group,
     * the groups are 0..MAX-1 with none missing, each is one tree of a base and a point
     * rooted on the anchor - and they run from the back of the cheek forward, the longest
     * first, so "the first k" is the big end of the row.
     */
    @Test
    void theRowIsNumberedFromTheBackLongestFirst() {
        for (PartShape shape : spikeShapes()) {
            List<PartNode> nodes = PartGenerators.build(shape);
            assertEquals(2 * CheekSpikeGenerator.MAX_SPIKES, nodes.size(), shape.toString());
            float previousZ = Float.MAX_VALUE;
            float previousLen = Float.MAX_VALUE;
            for (int j = 0; j < CheekSpikeGenerator.MAX_SPIKES; j++) {
                PartNode base = nodes.get(2 * j);
                PartNode point = nodes.get(2 * j + 1);
                assertTrue(base.isRoot(), shape + " spike " + j + " is not rooted on the anchor");
                assertEquals(j, base.group());
                assertEquals(j, point.group());
                assertEquals(2 * j, point.parent(), shape + " spike " + j + "'s point is on another spike");
                assertEquals(PartSheet.BONE, base.tex());
                assertEquals(PartSheet.BONE_TIP, point.tex());
                assertTrue(point.girth() <= base.girth(), shape + " spike " + j + " does not taper");
                assertEquals(CheekSpikeGenerator.rootZ(j), base.oz(), 1e-6f);
                assertTrue(base.oz() < previousZ, shape + " spike " + j + " is not forward of " + (j - 1));
                assertTrue(base.len() < previousLen, shape + " spike " + j + " is not shorter than " + (j - 1));
                // Laid over a quarter turn to point out, and swept back (a negative bend).
                assertEquals(Math.PI / 2, Math.abs(base.rz()), 1e-6);
                assertTrue(base.rx() < 0f, shape + " spike " + j + " does not sweep back");
                previousZ = base.oz();
                previousLen = base.len();
            }
        }
        // The row is centred on the anchor.
        assertEquals(0f, CheekSpikeGenerator.rootZ(0) + CheekSpikeGenerator.rootZ(CheekSpikeGenerator.MAX_SPIKES - 1),
                1e-6f);
        float shortest = PartGenerators.build(new PartShape(PartKind.CHEEK_SPIKE_RIGHT, 0, 0)).get(0).len();
        float longest = PartGenerators.build(new PartShape(PartKind.CHEEK_SPIKE_RIGHT, 0,
                CheekSpikeSize.classes() - 1)).get(0).len();
        assertTrue(longest > shortest, "a bigger class is a longer spike");
    }

    /** The left cheek is the right one mirrored, box for box - never a negative scale. */
    @Test
    void theLeftSideIsTheRightMirrored() {
        for (PartShape right : spikeShapes()) {
            if (right.kind() != PartKind.CHEEK_SPIKE_RIGHT) {
                continue;
            }
            List<PartNode> r = PartGenerators.build(right);
            List<PartNode> l = PartGenerators.build(
                    new PartShape(PartKind.CHEEK_SPIKE_LEFT, right.style(), right.size()));
            assertEquals(r.size(), l.size());
            for (int i = 0; i < r.size(); i++) {
                PartNode a = r.get(i);
                PartNode b = l.get(i);
                assertEquals(a.parent(), b.parent());
                assertEquals(a.oz(), b.oz());
                assertEquals(a.rx(), b.rx());
                assertEquals(-a.rz(), b.rz(), 1e-6f, right + " box " + i + " lean");
                assertEquals(a.len(), b.len());
                assertEquals(a.girth(), b.girth());
                assertEquals(a.group(), b.group());
                assertEquals(a.tex(), b.tex());
            }
            // A positive turn about z takes a box's up toward +x: the right cheek's point to -x.
            assertTrue(r.get(0).rz() < 0f, right + ": the right cheek's spikes point to -x");
        }
    }

    /** White by default; the colour locus dresses both cheeks; Bon/Bon is the bone version. */
    @Test
    void theColourLocusDressesBothCheeks() {
        Epigenome epi = Epigenome.fromSeed(5);
        AttachedPart plain = GrownParts.of(horse(Sex.MALE, "cheek_spikes", "Chk/n"), epi).get(0);
        for (int tint : new int[] {plain.baseTint(), plain.tipTint()}) {
            assertTrue((tint >> 16 & 0xFF) > 0xC0 && (tint >> 8 & 0xFF) > 0xC0 && (tint & 0xFF) > 0xC0,
                    "white: " + Integer.toHexString(tint));
        }
        List<AttachedPart> twoTone = GrownParts.of(horse(Sex.MALE, "cheek_spikes", "Chk/n",
                "cheek_spike_colour", "Red/Blu"), epi);
        for (AttachedPart side : twoTone) {
            assertTrue(side.twoTone());
            assertEquals(twoTone.get(0).baseTint(), side.baseTint(), "the sides match");
            assertEquals(twoTone.get(0).tipTint(), side.tipTint(), "the sides match");
        }
        for (AttachedPart bone : GrownParts.of(horse(Sex.MALE, "cheek_spikes", "Chk/n",
                "cheek_spike_colour", "Bon/Bon"), epi)) {
            int r = bone.baseTint() >> 16 & 0xFF;
            int b = bone.baseTint() & 0xFF;
            assertTrue(r > 0x90 && r >= b, "bone is pale and warm: " + Integer.toHexString(bone.baseTint()));
            assertFalse(bone.translucent());
            assertFalse(bone.emissive());
        }
        assertTrue(GrownParts.of(horse(Sex.MALE, "cheek_spike_colour", "Red/Blu"), epi).isEmpty(),
                "without spikes the colour does nothing");
        // Its own locus: the brow ridge's colour leaves the spikes alone.
        assertEquals(plain.baseTint(), GrownParts.of(horse(Sex.MALE, "cheek_spikes", "Chk/n",
                "brow_ridge_colour", "Red/Red"), epi).get(0).baseTint());
    }

    @Test
    void bothAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.CHEEK_SPIKES, Genes.CHEEK_SPIKE_COLOUR)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
            assertFalse(g.isNatural());
            assertFalse(g.affectsCoat(), g.key() + " must not reach the coat texture");
        }
        assertEquals(List.of(new GrownParts.Listed("Cheek spikes", true)),
                GrownParts.listed(horse(Sex.MALE, "cheek_spikes", "Chk/n"), Epigenome.fromSeed(2)),
                "the designer names the pair once, adult only");
        assertSame(CheekSpikeSize.LONG, CheekSpikeSize.of(1.0));
    }

    /** The plates' small share: about three horses in a thousand grow them, and every carrier shows. */
    @Test
    void spikesAreRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int spiked = 0;
        int homozygous = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int count = Genotype.random(rng).pair(GENE).count(GENE.Chk);
            spiked += count > 0 ? 1 : 0;
            homozygous += count == 2 ? 1 : 0;
        }
        assertTrue(spiked > 200 && spiked < 420, "Chk in " + n + ": " + spiked);
        assertEquals(0, homozygous, "a founder is Chk/n or nothing");
    }
}
