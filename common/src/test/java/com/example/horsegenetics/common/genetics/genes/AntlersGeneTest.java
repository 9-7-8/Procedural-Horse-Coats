package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AlleleEpigenetics;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AntlerGenerator;
import com.example.horsegenetics.common.parts.AntlerSize;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.PartKind;
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

/**
 * The antler loci: a dominant presence locus with a sex-limited allele, a habit
 * series, and three modifiers - all magical, all painting nothing.
 *
 * <p>As with the horn, the test worth the most is
 * {@link #everyEpigeneticNumberReachesTheRack}: a declared number nothing draws is a
 * promise the game does not keep, and nothing else goes red when one stops being
 * read.
 */
class AntlersGeneTest {

    private static final AntlersGene GENE = Genes.ANTLERS;

    private static Genotype horse(Sex sex, String... geneThenPair) {
        return Genotype.parse(Codes.of(geneThenPair)).with(Genes.SEX.pairFor(sex));
    }

    private static AllelePair pair(String tokens) {
        return Genotype.parse(Codes.of("antlers", tokens)).pair(GENE);
    }

    /** {@code epi} with {@code name} set to {@code value} on both of this gene's copies. */
    private static Epigenome with(Epigenome epi, Gene gene, String name, double value) {
        Epigenome.Copies c = epi.copies(gene);
        return epi.with(gene.key(), new Epigenome.Copies(
                new AlleleEpigenetics(c.first().priority(), c.first().values().with(name, value)),
                new AlleleEpigenetics(c.second().priority(), c.second().values().with(name, value))));
    }

    @Test
    void theSeriesIsDominantAndPaintsNothing() {
        assertTrue(GENE.expressionOf(pair("n/n")).wildType());
        assertEquals("antlers", GENE.expressionOf(pair("Ant/n")).id());
        assertEquals("antlers", GENE.expressionOf(pair("Ant/Antm")).id(), "Ant over Antm");
        assertEquals("antlers-stallion", GENE.expressionOf(pair("Antm/n")).id());
        for (var e : GENE.expressions()) {
            assertTrue(e.wildType(), e.id() + " paints - a part never does");
        }
        assertFalse(GENE.isNatural());
        assertFalse(GENE.affectsCoat());
    }

    /**
     * <b>The sex gate.</b> {@code Antm} is stallions only, applied where the part is
     * resolved and not in the expression - so a mare's {@code Antm/n} shows nothing
     * and a stallion's shows a rack, while {@code Ant} shows on both.
     */
    @Test
    void stallionAntlersSkipTheMares() {
        Epigenome epi = Epigenome.fromSeed(11);
        assertTrue(GrownParts.of(horse(Sex.FEMALE, "antlers", "Antm/n"), epi).isEmpty());
        assertEquals(2, GrownParts.of(horse(Sex.MALE, "antlers", "Antm/n"), epi).size());
        assertEquals(2, GrownParts.of(horse(Sex.FEMALE, "antlers", "Ant/n"), epi).size());
        assertEquals(2, GrownParts.of(horse(Sex.MALE, "antlers", "Ant/Antm"), epi).size());
        assertTrue(GrownParts.of(horse(Sex.MALE, "antlers", "n/n"), epi).isEmpty());
        assertTrue(GENE.sizeOf(horse(Sex.FEMALE, "antlers", "Antm/Antm"), epi).isEmpty());
    }

    @Test
    void aRackIsARightAndALeftAntlerAndNoFoalWearsOne() {
        List<AttachedPart> parts = GrownParts.of(horse(Sex.FEMALE, "antlers", "Ant/n"), Epigenome.fromSeed(4));
        assertSame(PartKind.ANTLER_RIGHT, parts.get(0).kind());
        assertSame(PartKind.ANTLER_LEFT, parts.get(1).kind());
        assertFalse(PartKind.ANTLER_LEFT.showsOnFoal());
        assertFalse(PartKind.ANTLER_RIGHT.showsOnFoal());
        assertTrue(PartKind.HORN.showsOnFoal(), "the horn keeps its half-size foal horn");
    }

    @Test
    void aHornAndAntlersGrowTogether() {
        Genotype both = horse(Sex.MALE, "antlers", "Ant/Ant", "unicorn_horn", "Horn/Horn");
        assertEquals(3, GrownParts.of(both, Epigenome.fromSeed(2)).size());
    }

    /**
     * <b>Every number the schema declares changes the rack.</b> Reachability over a
     * population: {@code size} to the class and the stretch, {@code rack} to the
     * variant, {@code tines} to the count, {@code reach} and {@code girth} to the
     * scales, {@code tint} to the colour. {@code asymmetry} has its own test, since
     * no founder carries any.
     */
    @Test
    void everyEpigeneticNumberReachesTheRack() {
        Genotype gt = horse(Sex.FEMALE, "antlers", "Ant/n");
        Set<Integer> classes = new HashSet<>();
        Set<Integer> variants = new HashSet<>();
        Set<Float> shown = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> girths = new HashSet<>();
        Set<Integer> tints = new HashSet<>();
        for (long seed = 0; seed < 2000; seed++) {
            AttachedPart right = GENE.racksFor(gt, Epigenome.fromSeed(seed)).orElseThrow().get(0);
            classes.add(right.shape().size());
            variants.add(right.shape().style() % AntlerGenerator.VARIANTS);
            shown.add(right.shown());
            stretches.add(right.stretch());
            girths.add(right.girth());
            tints.add(right.baseTint());
        }
        assertTrue(classes.size() >= 3, "size barely reaches the class: " + classes);
        assertEquals(AntlerGenerator.VARIANTS, variants.size(), "rack does not reach every variant");
        assertTrue(shown.size() > 100, "tines is not being read");
        assertTrue(stretches.size() > 100, "size/reach do not reach the stretch");
        assertTrue(girths.size() > 100, "girth is not being read");
        assertTrue(tints.size() > 50, "tint is not being read");
    }

    /**
     * <b>Symmetric by default (D3, owner).</b> No founder rolls any asymmetry and
     * drift cannot give it any, because its design range is zero wide; a rack is
     * therefore the same variant on both sides. Only a value put there - a breed's
     * band - makes it lopsided, and then every amount of it shows.
     */
    @Test
    void asymmetryIsZeroOnEveryFounderAndOnlyABandMovesIt() {
        EpiValue asym = GENE.epiSchema().values().stream()
                .filter(v -> v.name().equals(AntlersGene.ASYMMETRY)).findFirst().orElseThrow();
        assertEquals(0.0, asym.designSpan(), 0.0);
        Genotype gt = horse(Sex.MALE, "antlers", "Ant/n");
        SeededRng rng = new SeededRng(5);
        for (long seed = 0; seed < 500; seed++) {
            Epigenome epi = Epigenome.fromSeed(seed);
            assertEquals(0.0, epi.expressedValues(GENE, gt).get(AntlersGene.ASYMMETRY), 0.0);
            EpiValues drifted = epi.expressed(GENE, gt).drifted(rng).values();
            assertEquals(0.0, drifted.get(AntlersGene.ASYMMETRY), 0.0, "drift moved asymmetry");
            List<AttachedPart> rack = GENE.racksFor(gt, epi).orElseThrow();
            assertEquals(rack.get(0).shape().style(), rack.get(1).shape().style(), "a founder rack is lopsided");
            assertEquals(rack.get(0).shown(), rack.get(1).shown(), 0f);
        }
        Epigenome epi = with(Epigenome.fromSeed(9), GENE, AntlersGene.ASYMMETRY, 0.6);
        List<AttachedPart> rack = GENE.racksFor(gt, epi).orElseThrow();
        assertNotEquals(rack.get(0).shape().style(), rack.get(1).shape().style(),
                "past the threshold the left antler is a rack of its own");
        assertTrue(rack.get(1).shown() < rack.get(0).shown(), "and carries fewer tines");
        Epigenome slight = with(Epigenome.fromSeed(9), GENE, AntlersGene.ASYMMETRY, 0.05);
        List<AttachedPart> under = GENE.racksFor(gt, slight).orElseThrow();
        assertEquals(under.get(0).shape().style(), under.get(1).shape().style());
        assertTrue(under.get(1).shown() < under.get(0).shown(), "below it, still a little lopsided");
    }

    @Test
    void sizeClampsToTheLadderSoABredLineStopsAtMassive() {
        Genotype gt = horse(Sex.MALE, "antlers", "Ant/n");
        Epigenome huge = with(Epigenome.fromSeed(3), GENE, AntlersGene.SIZE, 50.0);
        assertSame(AntlerSize.MASSIVE, GENE.sizeOf(gt, huge).orElseThrow());
        AttachedPart right = GENE.racksFor(gt, huge).orElseThrow().get(0);
        assertEquals(AntlerSize.classes() - 1, right.shape().size());
        Epigenome many = with(Epigenome.fromSeed(3), GENE, AntlersGene.TINES, 500.0);
        assertTrue(GENE.racksFor(gt, many).orElseThrow().get(0).shown() <= AntlerGenerator.MAX_TINES);
    }

    // ------------------------------------------------------------------
    // The other four loci
    // ------------------------------------------------------------------

    @Test
    void theFormIsAPlainDominanceSeriesDrawnOnBothSides() {
        AntlerFormGene form = Genes.ANTLER_FORM;
        AllelePair palFrk = Genotype.parse(Codes.of("antler_form", "Frk/Pal")).pair(form);
        assertEquals(AntlerGenerator.PALMATE, form.habitOf(palFrk));
        assertEquals(AntlerGenerator.CROWN,
                form.habitOf(Genotype.parse(Codes.of("antler_form", "Brw/Crn")).pair(form)));
        assertEquals(AntlerGenerator.SPIKE,
                form.habitOf(Genotype.parse(Codes.of("antler_form", "n/n")).pair(form)));
        Genotype gt = horse(Sex.FEMALE, "antlers", "Ant/n", "antler_form", "Frk/Pal");
        List<AttachedPart> rack = GrownParts.of(gt, Epigenome.fromSeed(1));
        for (AttachedPart side : rack) {
            assertEquals(AntlerGenerator.PALMATE, side.shape().style() / AntlerGenerator.VARIANTS);
        }
    }

    @Test
    void glowCrystalAndBloomDressARackAndNothingElse() {
        Genotype plain = horse(Sex.FEMALE, "antlers", "Ant/n");
        Genotype dressed = horse(Sex.FEMALE, "antlers", "Ant/n", "antler_glow", "Glw/Glw",
                "antler_crystal", "Cry/Cry", "antler_bloom", "Blm/Blm");
        Epigenome epi = Epigenome.fromSeed(6);
        AttachedPart bare = GrownParts.of(plain, epi).get(0);
        AttachedPart done = GrownParts.of(dressed, epi).get(0);
        assertFalse(bare.emissive() || bare.translucent() || bare.blooms());
        assertTrue(done.emissive() && done.translucent() && done.blooms());
        assertEquals(bare.shape(), done.shape(), "dressing never reshapes");
        assertTrue(AntlerCrystalGene.GEMS.contains(done.baseTint()), "a crystal rack wears its gem");
        // ...and on a horse with no antlers, nothing at all.
        Genotype bald = horse(Sex.FEMALE, "antler_glow", "Glw/Glw", "antler_crystal", "Cry/Cry",
                "antler_bloom", "Blm/Blm");
        assertTrue(GrownParts.of(bald, epi).isEmpty());
        // One copy shows nothing.
        Genotype carrier = horse(Sex.FEMALE, "antlers", "Ant/n", "antler_glow", "Glw/n",
                "antler_crystal", "Cry/n", "antler_bloom", "Blm/n");
        AttachedPart c = GrownParts.of(carrier, epi).get(0);
        assertFalse(c.emissive() || c.translucent() || c.blooms());
    }

    @Test
    void theGemAndTheGrowthAreReadOffTheCopy() {
        Set<Integer> gems = new HashSet<>();
        Set<Integer> blooms = new HashSet<>();
        Genotype gt = horse(Sex.FEMALE, "antlers", "Ant/n", "antler_crystal", "Cry/Cry", "antler_bloom", "Blm/Blm");
        for (long seed = 0; seed < 400; seed++) {
            AttachedPart p = GrownParts.of(gt, Epigenome.fromSeed(seed)).get(0);
            gems.add(p.baseTint());
            blooms.add(p.bloomTint());
        }
        assertEquals(AntlerCrystalGene.GEMS.size(), gems.size());
        assertEquals(AntlerBloomGene.GROWTHS.size(), blooms.size());
    }

    @Test
    void allFiveLiveInTheGrownPartsFamilyAndTheEditorsCanSeeThem() {
        for (Gene g : List.of(Genes.ANTLERS, Genes.ANTLER_FORM, Genes.ANTLER_GLOW,
                Genes.ANTLER_CRYSTAL, Genes.ANTLER_BLOOM)) {
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(g), g.key());
            assertFalse(Genes.influencesCoat(g), g.key() + " paints");
            assertTrue(GrownParts.shapes(g), g.key() + " is invisible to the editors' randomize");
            assertTrue(EditorRules.changesLooks(g));
            assertFalse(g.description().isBlank(), g.key());
        }
    }

    /**
     * Rare and hand-seeded (D7): a few antlered horses in a thousand, stallion-only
     * commoner than both-sexes, and almost no homozygotes.
     */
    @Test
    void antlersAreRareInTheWild() {
        SeededRng rng = new SeededRng(31337L);
        int ant = 0;
        int antm = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            AllelePair p = Genotype.random(rng).pair(GENE);
            ant += p.count(GENE.Ant) > 0 ? 1 : 0;
            antm += p.count(GENE.Antm) > 0 && p.count(GENE.Ant) == 0 ? 1 : 0;
        }
        assertTrue(ant > 60 && ant < 260, "Ant rate off: " + ant + " / " + n);
        assertTrue(antm > 150 && antm < 400, "Antm rate off: " + antm + " / " + n);
        assertTrue(antm > ant, "the stallion-only allele is the commoner");
    }
}
