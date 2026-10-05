package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AlleleEpigenetics;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.HornGenerator;
import com.example.horsegenetics.common.parts.NarwhalSize;
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

/** The tusks locus's first form, the narwhal horn, and the tusk colour locus (tusks treatment, slice A). */
class TusksGeneTest {

    private static final TusksGene GENE = Genes.TUSKS;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static AllelePair pair(String tokens) {
        return Genotype.parse(Codes.of("tusks", tokens)).pair(GENE);
    }

    private static List<AttachedPart> narwhals(List<AttachedPart> parts) {
        return parts.stream().filter(p -> p.kind() == PartKind.NARWHAL).toList();
    }

    /** {@code copy} with its length set, its priority kept. */
    private static AlleleEpigenetics withLength(AlleleEpigenetics copy, double length) {
        return new AlleleEpigenetics(copy.priority(), copy.values().with(TusksGene.LENGTH, length));
    }

    /** Dominant: one copy grows one horn, on mares and stallions alike; none shows nothing. */
    @Test
    void oneCopyGrowsTheHornOnEitherSex() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            assertTrue(GrownParts.of(horse(sex, "tusks", "n/n"), epi).isEmpty(), sex + " wild");
            for (String p : List.of("Nar/n", "Nar/Nar")) {
                List<AttachedPart> parts = GrownParts.of(horse(sex, "tusks", p), epi);
                assertEquals(1, parts.size(), sex + " " + p + ": two copies of a form grow it once");
                assertSame(PartKind.NARWHAL, parts.get(0).kind());
            }
        }
        assertEquals("narwhal", GENE.expressionOf(pair("Nar/n")).id());
        assertEquals("narwhal", GENE.expressionOf(pair("Nar/Nar")).id(), "a second copy changes nothing");
        assertTrue(GENE.expressionOf(pair("n/n")).wildType());
        for (var e : GENE.expressions()) {
            assertTrue(e.wildType(), e.id() + " paints - a part never does");
        }
        assertSame(PartAnchor.SNOUT, PartKind.NARWHAL.anchor());
        assertFalse(PartKind.NARWHAL.showsOnFoal(), "a hard part comes with maturity");
        assertFalse(PartKind.NARWHAL.saddleZoned());
        assertFalse(PartKind.NARWHAL.scalesPerElement());
        assertEquals(0, PartKind.NARWHAL.translucentRegions());
    }

    /** Every number the narwhal form reads reaches the horn: length, girth, curve (as lift) and style (as twist). */
    @Test
    void everyNumberItReadsReachesTheHorn() {
        Genotype gt = horse(Sex.FEMALE, "tusks", "Nar/n");
        Set<Integer> twists = new HashSet<>();
        Set<Integer> buckets = new HashSet<>();
        Set<Float> girths = new HashSet<>();
        Set<Float> tilts = new HashSet<>();
        Set<Float> lengths = new HashSet<>();
        for (long seed = 0; seed < 1000; seed++) {
            AttachedPart horn = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            twists.add(horn.shape().style());
            buckets.add(horn.shape().size());
            girths.add(horn.girth());
            tilts.add(horn.tilt());
            lengths.add(horn.length());
            assertTrue(horn.tilt() <= 0f && horn.tilt() >= -TusksGene.NARWHAL_LIFT - 1e-6,
                    "a narwhal horn lifts, it never droops past the head's axis: " + horn.tilt());
            assertTrue(horn.length() >= NarwhalSize.SHORT.length - 1e-3
                            && horn.length() <= NarwhalSize.GREAT.length + 1e-3,
                    "a horn is on its ladder: " + horn.length());
        }
        assertEquals(HornGenerator.NARWHAL_STYLES, twists.size(), "the style does not reach every twist");
        assertTrue(buckets.size() > 5, "length: " + buckets);
        assertTrue(girths.size() > 100);
        assertTrue(tilts.size() > 100);
        assertTrue(lengths.size() > 100);
    }

    /**
     * The treatment's unverified line: a copy's numbers follow its own allele. Bred both
     * ways round - the sire carrying {@code Nar}, then the dam - every foal that grows a
     * horn grows it from the parent's {@code Nar} copy, never from the empty wild-type
     * copy beside it (which would read the schema's midpoint, a different length).
     */
    @Test
    void aFoalsHornFollowsItsNarCopyWhicheverParentGaveIt() {
        double carried = 0.95;
        float expected = NarwhalSize.lengthFor(carried);
        float midpoint = NarwhalSize.lengthFor(0.3);
        for (boolean sireCarries : new boolean[] {true, false}) {
            SeededRng rng = new SeededRng(sireCarries ? 17L : 29L);
            Genome carrier = Genome.of(horse(sireCarries ? Sex.MALE : Sex.FEMALE, "tusks", "Nar/n"), rng);
            Epigenome.Copies c = carrier.epigenome().copies(GENE);
            assertSame(GENE.Nar, carrier.genotype().pair(GENE).first(), "Nar is the first slot");
            carrier = new Genome(carrier.genotype(), carrier.epigenome().with(GENE.key(),
                    new Epigenome.Copies(withLength(c.first(), carried), c.second())));
            Genome other = Genome.of(horse(sireCarries ? Sex.FEMALE : Sex.MALE, "tusks", "n/n"), rng);
            Genome sire = sireCarries ? carrier : other;
            Genome dam = sireCarries ? other : carrier;
            int horned = 0;
            for (int i = 0; i < 200; i++) {
                Genome foal = sire.breedWith(dam, rng);
                List<AttachedPart> horn = narwhals(GrownParts.of(foal.genotype(), foal.epigenome()));
                if (horn.isEmpty()) {
                    continue;
                }
                horned++;
                float length = horn.get(0).length();
                assertTrue(Math.abs(length - expected) < Math.abs(length - midpoint),
                        (sireCarries ? "sire" : "dam") + " carries: a foal grew " + length
                                + ", nearer the wild copy's " + midpoint + " than its Nar copy's " + expected);
            }
            assertTrue(horned > 50, "half the foals inherit Nar: " + horned);
        }
    }

    /**
     * Two copies of one form grow it once, from the FIRST copy (treatment: the antlers'
     * symmetric rule) - not from the higher-priority copy every other part gene reads.
     */
    @Test
    void aHomozygoteGrowsFromItsFirstCopy() {
        Genome g = Genome.of(horse(Sex.MALE, "tusks", "Nar/Nar"), new SeededRng(5L));
        Epigenome.Copies c = g.epigenome().copies(GENE);
        AlleleEpigenetics first = new AlleleEpigenetics(1, c.first().values().with(TusksGene.LENGTH, 0.1));
        AlleleEpigenetics second = new AlleleEpigenetics(1000, c.second().values().with(TusksGene.LENGTH, 0.9));
        Epigenome epi = g.epigenome().with(GENE.key(), new Epigenome.Copies(first, second));
        AttachedPart horn = GrownParts.of(g.genotype(), epi).get(0);
        assertEquals(NarwhalSize.lengthFor(0.1), horn.length(), 1e-3f, "read the first copy");
    }

    /** The mesh is the unicorn horn's twisted chain: straight, twisted, ivory at the root and polished at the tip. */
    @Test
    void theMeshIsAStraightTwistedHornChain() {
        Set<Float> rolls = new HashSet<>();
        for (PartShape shape : PartGenerators.allShapes()) {
            if (shape.kind() != PartKind.NARWHAL) {
                continue;
            }
            List<PartNode> nodes = PartGenerators.build(shape);
            for (int i = 1; i < nodes.size(); i++) {
                PartNode node = nodes.get(i);
                assertEquals(i - 1, node.parent(), shape + ": one chain");
                assertEquals(0f, node.rx(), shape + ": a narwhal horn never bends");
                assertTrue(node.ry() > 0f, shape + ": every segment twists");
                rolls.add(node.ry());
            }
            assertEquals(PartSheet.HORN, nodes.get(0).tex());
            assertEquals(PartSheet.HORN_TIP, nodes.get(nodes.size() - 1).tex());
        }
        assertEquals(HornGenerator.NARWHAL_STYLES, rolls.size(), "spiral and tight are two twists");
        float shortest = PartGenerators.build(new PartShape(PartKind.NARWHAL, 0, 0)).size();
        float longest = PartGenerators.build(new PartShape(PartKind.NARWHAL, 0, PartShape.SIZE_BUCKETS - 1)).size();
        assertTrue(longest > shortest, "a longer horn has more segments");
    }

    /** White (ivory) by default; the colour locus makes it two-tone, and Bon/Bon is bare bone. */
    @Test
    void theColourLocusDressesTheHorn() {
        Epigenome epi = Epigenome.fromSeed(5);
        AttachedPart plain = GrownParts.of(horse(Sex.MALE, "tusks", "Nar/n"), epi).get(0);
        for (int tint : new int[] {plain.baseTint(), plain.tipTint()}) {
            assertTrue((tint >> 16 & 0xFF) > 0xC0 && (tint >> 8 & 0xFF) > 0xC0 && (tint & 0xFF) > 0xC0,
                    "white: " + Integer.toHexString(tint));
        }
        AttachedPart twoTone = GrownParts.of(horse(Sex.MALE, "tusks", "Nar/n", "tusk_colour", "Red/Blu"), epi)
                .get(0);
        assertTrue(twoTone.twoTone());
        assertEquals(twoTone, GrownParts.of(horse(Sex.MALE, "tusks", "Nar/n", "tusk_colour", "Blu/Red"), epi)
                .get(0), "the base is fixed by the pair, not the order");
        AttachedPart bone = GrownParts.of(horse(Sex.MALE, "tusks", "Nar/n", "tusk_colour", "Bon/Bon"), epi).get(0);
        int r = bone.baseTint() >> 16 & 0xFF;
        int b = bone.baseTint() & 0xFF;
        assertTrue(r > 0x90 && r >= b, "bone is pale and warm: " + Integer.toHexString(bone.baseTint()));
        assertFalse(bone.translucent());
        assertTrue(GrownParts.of(horse(Sex.MALE, "tusk_colour", "Red/Blu"), epi).isEmpty(),
                "without tusks the colour does nothing");
    }

    /** It stacks with the unicorn horn - the forehead and the muzzle - and each keeps its own colour. */
    @Test
    void aUnicornCanGrowANarwhalHornToo() {
        Epigenome epi = Epigenome.fromSeed(9);
        List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "unicorn_horn", "Horn/Horn",
                "tusks", "Nar/n", "horn_colour", "Red/Red", "tusk_colour", "Blk/Blk"), epi);
        assertEquals(2, parts.size());
        for (AttachedPart part : parts) {
            boolean red = (part.baseTint() >> 16 & 0xFF) > 0x80;
            assertEquals(part.kind() == PartKind.HORN, red, part.kind() + " has the other part's colour");
        }
        assertEquals(1, narwhals(parts).size());
    }

    @Test
    void bothAreGrownPartsTheEditorsCanSee() {
        for (Gene g : List.of(Genes.TUSKS, Genes.TUSK_COLOUR)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertTrue(EditorRules.changesLooks(g), g.key());
            assertFalse(g.description().isBlank());
            assertFalse(g.isNatural());
            assertFalse(g.affectsCoat(), g.key() + " must not reach the coat texture");
        }
        assertSame(GeneRarity.RARE, GENE.rarity());
        assertEquals(List.of(new GrownParts.Listed("Narwhal horn", true)),
                GrownParts.listed(horse(Sex.MALE, "tusks", "Nar/n"), Epigenome.fromSeed(2)),
                "the designer names it, adult only");
    }

    /** About one horse in a thousand, every carrier shows, and no wild horse is a homozygote. */
    @Test
    void theNarwhalHornIsRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int horned = 0;
        int homozygous = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            int count = Genotype.random(rng).pair(GENE).count(GENE.Nar);
            horned += count > 0 ? 1 : 0;
            homozygous += count == 2 ? 1 : 0;
        }
        assertTrue(horned > 60 && horned < 150, "Nar in " + n + ": " + horned);
        assertEquals(0, homozygous, "a founder is Nar/n or nothing");
    }
}
