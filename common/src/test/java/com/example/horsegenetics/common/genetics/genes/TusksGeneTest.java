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
import com.example.horsegenetics.common.parts.SabreGenerator;
import com.example.horsegenetics.common.parts.SabreSize;
import com.example.horsegenetics.common.parts.TuskGenerator;
import com.example.horsegenetics.common.parts.TuskSize;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tusks locus and the tusk colour locus (tusks treatment): the narwhal horn (slice
 * A), then the boar tusks and sabre fangs and the mixed pairs (slice B).
 */
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

    private static List<AttachedPart> tusks(List<AttachedPart> parts) {
        return parts.stream().filter(p -> p.kind().tusk()).toList();
    }

    private static List<AttachedPart> sabres(List<AttachedPart> parts) {
        return parts.stream().filter(p -> p.kind().sabre()).toList();
    }

    /** {@code copy} with its length set, its priority kept. */
    private static AlleleEpigenetics withLength(AlleleEpigenetics copy, double length) {
        if (copy.values().isEmpty()) {
            return copy;   // a wild-type copy carries no numbers
        }
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

    /**
     * Each form is rare, the narwhal horn the rarest (treatment); every carrier shows,
     * and no wild horse carries two forms or two copies of one.
     */
    @Test
    void everyFormIsRareInTheWild() {
        SeededRng rng = new SeededRng(91L);
        int narwhal = 0;
        int boar = 0;
        int sabre = 0;
        int twoCopies = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            AllelePair p = Genotype.random(rng).pair(GENE);
            narwhal += p.count(GENE.Nar);
            boar += p.count(GENE.Tsk);
            sabre += p.count(GENE.Sab);
            twoCopies += p.count(GENE.n) == 0 ? 1 : 0;
        }
        assertTrue(narwhal > 60 && narwhal < 150, "Nar in " + n + ": " + narwhal);
        assertTrue(boar > 140 && boar < 270, "Tsk in " + n + ": " + boar);
        assertTrue(sabre > 100 && sabre < 210, "Sab in " + n + ": " + sabre);
        assertEquals(0, twoCopies, "a founder carries one copy of one form, or nothing");
    }

    // ------------------------------------------------------------------
    // Slice B: the boar tusks, the sabre fangs, and two forms on one horse
    // ------------------------------------------------------------------

    /** Dominant, like the horn: one copy grows the pair, two grow it once, on either sex. */
    @Test
    void oneCopyGrowsAPairOfTusksOrFangs() {
        Epigenome epi = Epigenome.fromSeed(3);
        for (Sex sex : Sex.values()) {
            for (String p : List.of("Tsk/n", "n/Tsk", "Tsk/Tsk")) {
                List<AttachedPart> parts = GrownParts.of(horse(sex, "tusks", p), epi);
                assertEquals(2, parts.size(), sex + " " + p);
                assertSame(PartKind.TUSK_RIGHT, parts.get(0).kind());
                assertSame(PartKind.TUSK_LEFT, parts.get(1).kind());
                assertEquals("tusks", GENE.expressionOf(pair(p)).id(), p);
            }
            for (String p : List.of("Sab/n", "Sab/Sab")) {
                List<AttachedPart> parts = GrownParts.of(horse(sex, "tusks", p), epi);
                assertEquals(2, parts.size(), sex + " " + p);
                assertSame(PartKind.SABRE_RIGHT, parts.get(0).kind());
                assertSame(PartKind.SABRE_LEFT, parts.get(1).kind());
                assertEquals("sabre", GENE.expressionOf(pair(p)).id(), p);
            }
        }
        assertEquals(pair("Tsk/n"), pair("n/Tsk"), "a pair is its reverse");
        assertSame(GENE.n, pair("Tsk/n").first(), "Tsk was appended after n, so n is the first slot");
        assertSame(PartAnchor.JAW_RIGHT, PartKind.TUSK_RIGHT.anchor());
        assertSame(PartAnchor.JAW_LEFT, PartKind.TUSK_LEFT.anchor());
        assertSame(PartAnchor.LIP_RIGHT, PartKind.SABRE_RIGHT.anchor());
        assertSame(PartAnchor.LIP_LEFT, PartKind.SABRE_LEFT.anchor());
        for (PartKind kind : List.of(PartKind.TUSK_RIGHT, PartKind.TUSK_LEFT, PartKind.SABRE_RIGHT,
                PartKind.SABRE_LEFT)) {
            assertFalse(kind.showsOnFoal(), kind + ": a hard part comes with maturity");
            assertFalse(kind.saddleZoned());
            assertFalse(kind.scalesPerElement());
            assertEquals(0, kind.translucentRegions());
        }
        assertEquals(List.of(new GrownParts.Listed("Boar tusks", true)),
                GrownParts.listed(horse(Sex.MALE, "tusks", "Tsk/n"), epi), "a pair is named once");
        assertEquals(List.of(new GrownParts.Listed("Sabre fangs", true)),
                GrownParts.listed(horse(Sex.MALE, "tusks", "Sab/n"), epi));
    }

    /** The forms combine (owner): a horse with two different forms grows both, whichever way the pair is written. */
    @Test
    void twoDifferentFormsGrowBoth() {
        Epigenome epi = Epigenome.fromSeed(4);
        for (String p : List.of("Tsk/Sab", "Sab/Tsk")) {
            List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "tusks", p), epi);
            assertEquals(4, parts.size(), p);
            assertEquals(2, tusks(parts).size(), p);
            assertEquals(2, sabres(parts).size(), p);
            assertEquals("tusks_sabre", GENE.expressionOf(pair(p)).id());
        }
        for (String p : List.of("Nar/Tsk", "Tsk/Nar")) {
            List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "tusks", p), epi);
            assertEquals(3, parts.size(), p);
            assertEquals(1, narwhals(parts).size(), p);
            assertEquals(2, tusks(parts).size(), p);
            assertEquals("tusks_narwhal", GENE.expressionOf(pair(p)).id());
        }
        for (String p : List.of("Nar/Sab", "Sab/Nar")) {
            List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "tusks", p), epi);
            assertEquals(3, parts.size(), p);
            assertEquals(1, narwhals(parts).size(), p);
            assertEquals(2, sabres(parts).size(), p);
            assertEquals("sabre_narwhal", GENE.expressionOf(pair(p)).id());
        }
        // One colour locus tints every form on the horse.
        List<AttachedPart> red = GrownParts.of(
                horse(Sex.MALE, "tusks", "Nar/Sab", "tusk_colour", "Red/Red"), epi);
        for (AttachedPart part : red) {
            assertEquals(red.get(0).baseTint(), part.baseTint(), part.kind() + " has its own colour");
        }
        assertEquals(List.of(new GrownParts.Listed("Narwhal horn", true), new GrownParts.Listed("Sabre fangs", true)),
                GrownParts.listed(horse(Sex.MALE, "tusks", "Nar/Sab"), epi));
    }

    /**
     * <b>Each form grows from the copy that carries it.</b> Every mixed pair, with the
     * first copy short and the second long: each form's length is its own copy's, never
     * the other's. A one-copy boar is {@code n/Tsk}, so its tusks read the <i>second</i>
     * slot - a rule that read a fixed slot fails on one side of this or the other.
     */
    @Test
    void eachFormReadsTheCopyThatCarriesIt() {
        double first = 0.05;
        double second = 0.95;
        for (String p : List.of("Tsk/Sab", "Nar/Tsk", "Nar/Sab", "n/Tsk", "n/Sab", "Nar/n")) {
            Genome g = Genome.of(horse(Sex.MALE, "tusks", p), new SeededRng(11L));
            Epigenome.Copies c = g.epigenome().copies(GENE);
            Epigenome epi = g.epigenome().with(GENE.key(),
                    new Epigenome.Copies(withLength(c.first(), first), withLength(c.second(), second)));
            AllelePair pair = g.genotype().pair(GENE);
            List<AttachedPart> parts = GrownParts.of(g.genotype(), epi);
            if (pair.has(GENE.Nar)) {
                double mine = pair.first() == GENE.Nar ? first : second;
                assertEquals(NarwhalSize.lengthFor(mine), narwhals(parts).get(0).length(), 1e-3f, p + " horn");
            }
            if (pair.has(GENE.Tsk)) {
                double mine = pair.first() == GENE.Tsk ? first : second;
                for (AttachedPart tusk : tusks(parts)) {
                    assertEquals(TuskSize.lengthFor(mine), tusk.length(), 1e-3f, p + " tusk");
                }
                assertSame(TuskSize.of(mine), GENE.tuskSizeOf(g.genotype(), epi).orElseThrow(), p);
            }
            if (pair.has(GENE.Sab)) {
                double mine = pair.first() == GENE.Sab ? first : second;
                for (AttachedPart fang : sabres(parts)) {
                    assertEquals(SabreSize.lengthFor(mine), fang.length(), 1e-3f, p + " fang");
                }
                assertSame(SabreSize.of(mine), GENE.sabreSizeOf(g.genotype(), epi).orElseThrow(), p);
            }
        }
        assertTrue(GENE.tuskSizeOf(horse(Sex.MALE, "tusks", "Nar/n"), Epigenome.fromSeed(1)).isEmpty());
        assertTrue(GENE.sabreSizeOf(horse(Sex.MALE, "tusks", "Nar/n"), Epigenome.fromSeed(1)).isEmpty());
    }

    /**
     * Every number a tusk or a fang reads reaches it, and a pair is symmetric: both
     * sides one style, one size. The style reaches every form, the curve every sweep
     * (or a fang's lean), the seed both hooks and every serration pattern.
     */
    @Test
    void everyNumberReachesTheTusksAndTheFangs() {
        Genotype gt = horse(Sex.FEMALE, "tusks", "Tsk/Sab");
        Set<Integer> tuskStyles = new HashSet<>();
        Set<Integer> tuskSizes = new HashSet<>();
        Set<Integer> sabreStyles = new HashSet<>();
        Set<Integer> sabreSizes = new HashSet<>();
        Set<Float> leans = new HashSet<>();
        Set<Float> girths = new HashSet<>();
        for (long seed = 0; seed < 2000; seed++) {
            List<AttachedPart> parts = GrownParts.of(gt, Epigenome.fromSeed(seed));
            List<AttachedPart> t = tusks(parts);
            List<AttachedPart> s = sabres(parts);
            assertEquals(t.get(0).shape().style(), t.get(1).shape().style(), "a lopsided pair of tusks");
            assertEquals(t.get(0).shape().size(), t.get(1).shape().size());
            assertEquals(t.get(0).length(), t.get(1).length());
            assertEquals(s.get(0).shape().style(), s.get(1).shape().style(), "a lopsided pair of fangs");
            assertEquals(s.get(0).tilt(), s.get(1).tilt());
            assertEquals(0f, t.get(0).tilt(), "a tusk's curve is baked, not a lean");
            assertTrue(s.get(0).tilt() <= 0f && s.get(0).tilt() >= -TusksGene.SABRE_LEAN - 1e-6,
                    "a fang leans forward or hangs straight: " + s.get(0).tilt());
            assertTrue(t.get(0).length() >= TuskSize.SHORTEST - 1e-3 && t.get(0).length() <= TuskSize.LONGEST + 1e-3);
            assertTrue(s.get(0).length() >= SabreSize.SHORTEST - 1e-3 && s.get(0).length() <= SabreSize.LONGEST + 1e-3);
            tuskStyles.add(t.get(0).shape().style());
            tuskSizes.add(t.get(0).shape().size());
            sabreStyles.add(s.get(0).shape().style());
            sabreSizes.add(s.get(0).shape().size());
            leans.add(s.get(0).tilt());
            girths.add(t.get(0).girth());
        }
        // Three forms by three sweeps, and the hooked form in both hooks.
        assertEquals(TuskGenerator.FORMS * TuskGenerator.SWEEPS + TuskGenerator.SWEEPS, tuskStyles.size(),
                "tusk styles reached: " + tuskStyles);
        // Straight, curved, and serrated in every pattern.
        assertEquals(SabreGenerator.FORMS - 1 + SabreGenerator.PATTERNS, sabreStyles.size(),
                "fang styles reached: " + sabreStyles);
        assertTrue(tuskSizes.size() >= 2, "wild tusks span classes: " + tuskSizes);
        assertTrue(sabreSizes.size() >= 2, "wild fangs span classes: " + sabreSizes);
        assertTrue(leans.size() > 100);
        assertTrue(girths.size() > 100);
    }

    /** The left of a pair is the right mirrored, box for box - two meshes, never a negative scale. */
    @Test
    void theLeftOfAPairIsTheRightMirrored() {
        for (PartShape right : PartGenerators.allShapes()) {
            PartKind other = right.kind() == PartKind.TUSK_RIGHT ? PartKind.TUSK_LEFT
                    : right.kind() == PartKind.SABRE_RIGHT ? PartKind.SABRE_LEFT : null;
            if (other == null) {
                continue;
            }
            List<PartNode> r = PartGenerators.build(right);
            List<PartNode> l = PartGenerators.build(new PartShape(other, right.style(), right.size()));
            assertEquals(r.size(), l.size(), right + " grows a different number of boxes per side");
            for (int i = 0; i < r.size(); i++) {
                PartNode a = r.get(i);
                PartNode b = l.get(i);
                assertEquals(a.parent(), b.parent());
                assertEquals(a.len(), b.len());
                assertEquals(a.girth(), b.girth());
                assertEquals(a.width(), b.width());
                assertEquals(a.rx(), b.rx());
                assertEquals(-a.rz(), b.rz(), 1e-6f, right + " box " + i + " does not mirror");
                assertEquals(-a.ox(), b.ox(), 1e-6f);
                assertEquals(a.tex(), b.tex());
            }
        }
    }

    /**
     * A tusk is one short tapering chain that leaves the jaw outward and curls back
     * toward upright; a hook turns further; every style a horse can reach is its own mesh.
     */
    @Test
    void aTuskIsACurlingChainAndEveryStyleDiffers() {
        Set<List<PartNode>> meshes = new HashSet<>();
        int reachable = 0;
        for (int form = 0; form < TuskGenerator.FORMS; form++) {
            for (int sweep = 0; sweep < TuskGenerator.SWEEPS; sweep++) {
                for (int hook = 0; hook < (form == TuskGenerator.HOOKED ? TuskGenerator.HOOKS : 1); hook++) {
                    List<PartNode> nodes = PartGenerators.build(new PartShape(PartKind.TUSK_LEFT,
                            TuskGenerator.style(form, sweep, hook), 2));
                    reachable++;
                    meshes.add(nodes);
                    assertTrue(nodes.size() >= 3 && nodes.size() <= TuskGenerator.MAX_NODES, "boxes: " + nodes.size());
                    assertTrue(nodes.get(0).rz() > 0.5f, "the left tusk leaves the jaw toward +x");
                    float turned = 0f;
                    for (int i = 1; i < nodes.size(); i++) {
                        assertEquals(i - 1, nodes.get(i).parent(), "one chain");
                        assertTrue(nodes.get(i).girth() <= nodes.get(i - 1).girth(), "it tapers");
                        assertTrue(nodes.get(i).rz() < 0f, "every box curls back toward upright");
                        turned += nodes.get(i).rz();
                    }
                    assertEquals(PartSheet.HORN, nodes.get(0).tex());
                    assertEquals(PartSheet.HORN_TIP, nodes.get(nodes.size() - 1).tex());
                    if (form == TuskGenerator.HOOKED && hook == 0) {
                        assertTrue(-turned > nodes.get(0).rz() + 1.0f, "a hook over the nose turns well past upright");
                    }
                }
            }
        }
        assertEquals(reachable, meshes.size(), "two reachable tusk styles are one mesh");
        float curl = PartGenerators.build(new PartShape(PartKind.TUSK_LEFT,
                TuskGenerator.style(TuskGenerator.CURL, 1, 0), 2)).stream().map(PartNode::len).reduce(0f, Float::sum);
        float sweep = PartGenerators.build(new PartShape(PartKind.TUSK_LEFT,
                TuskGenerator.style(TuskGenerator.SWEEP, 1, 0), 2)).stream().map(PartNode::len).reduce(0f, Float::sum);
        assertTrue(curl < sweep, "the short form is the shorter one");
    }

    /**
     * A fang is a flat blade turned over to hang, with a point; the curved one bends
     * back, and the serrated one has barbs on its back edge in a pattern of its own.
     */
    @Test
    void aFangIsAHangingBladeAndEveryStyleDiffers() {
        Set<List<PartNode>> meshes = new HashSet<>();
        for (int style : List.of(SabreGenerator.style(SabreGenerator.STRAIGHT, 0),
                SabreGenerator.style(SabreGenerator.CURVED, 0),
                SabreGenerator.style(SabreGenerator.SERRATED, 0),
                SabreGenerator.style(SabreGenerator.SERRATED, 1),
                SabreGenerator.style(SabreGenerator.SERRATED, 2))) {
            List<PartNode> nodes = PartGenerators.build(new PartShape(PartKind.SABRE_RIGHT, style, 1));
            meshes.add(nodes);
            assertTrue(nodes.size() <= SabreGenerator.MAX_NODES, "boxes: " + nodes.size());
            PartNode root = nodes.get(0);
            assertEquals((float) Math.PI, root.rx(), 1e-6f, "the root turns the fang over to hang");
            assertTrue(root.width() < root.girth(), "the blade is flat: thin across the horse");
            assertEquals(PartSheet.HORN, root.tex());
            int form = SabreGenerator.formOf(style);
            int blade = form == SabreGenerator.CURVED ? 3 : 2;
            assertEquals(PartSheet.HORN_TIP, nodes.get(blade - 1).tex(), "the point is polished");
            for (int i = 1; i < blade; i++) {
                assertEquals(i - 1, nodes.get(i).parent());
                assertTrue(nodes.get(i).girth() <= nodes.get(i - 1).girth(), "it tapers");
                assertEquals(form == SabreGenerator.CURVED, nodes.get(i).rx() > 0f, "only the curved fang bends back");
            }
            assertEquals(form == SabreGenerator.SERRATED, nodes.size() > blade, "only the serrated fang has barbs");
            for (int i = blade; i < nodes.size(); i++) {
                assertTrue(nodes.get(i).parent() < blade, "a barb grows off the blade");
                assertTrue(nodes.get(i).oz() < 0f, "on the back edge");
            }
        }
        assertEquals(5, meshes.size(), "two reachable fang styles are one mesh");
        assertEquals(SabreGenerator.style(SabreGenerator.STRAIGHT, 0), SabreGenerator.style(SabreGenerator.STRAIGHT, 2),
                "a pattern shows only on the serrated form, so the others share a mesh");
    }
}
