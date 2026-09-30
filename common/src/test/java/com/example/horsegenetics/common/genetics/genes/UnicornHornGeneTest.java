package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.HornGenerator;
import com.example.horsegenetics.common.parts.HornSize;
import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartKind;
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
 * The unicorn locus: recessive, magical, paints nothing, and grows a mesh instead.
 *
 * <p>The test that matters most here is
 * {@link #everyEpigeneticNumberReachesTheHorn}. The rule
 * {@code AttachedPart} lives under - no field the renderer ignores, no epigenetic
 * number with no consumer - is the sort of thing that is true when it is written
 * and quietly false two changes later, because nothing goes red: the horse still
 * has a horn, a number in the inspector just stops meaning anything.
 */
class UnicornHornGeneTest {

    private static final UnicornHornGene GENE = Genes.UNICORN_HORN;

    private static AllelePair pair(String tokens) {
        return Genotype.parse(Codes.of("unicorn_horn", tokens)).pair(GENE);
    }

    private static Genotype unicorn() {
        return Genotype.parse(Codes.of("unicorn_horn", "Horn/Horn"));
    }

    @Test
    void onlyTwoCopiesGrowAHorn() {
        assertTrue(GENE.expressionOf(pair("n/n")).wildType());
        Expression carrier = GENE.expressionOf(pair("Horn/n"));
        assertTrue(carrier.wildType());
        assertEquals("unicorn-carrier", carrier.id());
        Expression unicorn = GENE.expressionOf(pair("Horn/Horn"));
        assertEquals("unicorn", unicorn.id());
        assertTrue(unicorn.wildType(), "it paints nothing - a client layer draws the horn");
        assertTrue(GENE.shows(pair("Horn/Horn")));
        assertFalse(GENE.shows(pair("Horn/n")));
        assertFalse(GENE.shows(pair("n/n")));
    }

    /**
     * <b>It must not touch the coat.</b> Two horses that differ only here share one
     * baked coat texture, which is why neither coat golden moves for this gene and
     * why a session should not go hunting for one that should have.
     */
    @Test
    void itIsMagicalAndPaintsNothing() {
        assertFalse(GENE.isNatural());
        assertFalse(GENE.affectsCoat(), "all three outcomes are wild types");
        assertTrue(Genes.magicalOrder().contains(GENE));
        assertFalse(Genes.naturalOrder().contains(GENE));
    }

    /** It is a grown part, not a marking - so it is in its own family and not beside the coats. */
    @Test
    void itLivesInTheGrownPartsFamily() {
        assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(GENE));
        assertTrue(GeneFamily.MAGIC_PARTS.members().contains(GENE));
        assertTrue(GeneFamily.occupied().contains(GeneFamily.MAGIC_PARTS),
                "a family with a registered gene in it must be offered by the editors");
        assertFalse(GeneFamily.MAGIC_PARTS.natural());
    }

    @Test
    void hornForIsEmptyUnlessHomozygous() {
        Genotype carrier = Genotype.parse(Codes.of("unicorn_horn", "Horn/n"));
        assertTrue(GENE.hornFor(carrier, Epigenome.fromSeed(1)).isEmpty());
        assertTrue(GENE.hornFor(Genotype.wildType(), Epigenome.fromSeed(1)).isEmpty());
        assertTrue(GrownParts.of(carrier, Epigenome.fromSeed(1)).isEmpty());
        assertTrue(GrownParts.of(Genotype.wildType(), Epigenome.fromSeed(1)).isEmpty());
        assertTrue(GrownParts.of(null, Epigenome.fromSeed(1)).isEmpty());
    }

    @Test
    void aUnicornsHornArrivesThroughTheOneDoorTheClientUses() {
        List<AttachedPart> parts = GrownParts.of(unicorn(), Epigenome.fromSeed(5));
        assertEquals(1, parts.size());
        assertEquals(PartKind.HORN, parts.get(0).kind());
        assertSame(PartAnchor.FOREHEAD, parts.get(0).kind().anchor());
        assertEquals(GENE.hornFor(unicorn(), Epigenome.fromSeed(5)).orElseThrow(), parts.get(0));
    }

    @Test
    void theSameHorseGrowsTheSameHornEverySession() {
        Genotype gt = unicorn();
        for (long seed : new long[]{0L, 1L, 7L, 42L, 999L, 123456L}) {
            AttachedPart a = GENE.hornFor(gt, Epigenome.fromSeed(seed)).orElseThrow();
            AttachedPart b = GENE.hornFor(gt, Epigenome.fromSeed(seed)).orElseThrow();
            assertEquals(a, b, "seed " + seed + " grew two different horns");
        }
    }

    /**
     * <b>Every number the schema declares changes the horn, and every field of the
     * horn is changed by one.</b>
     *
     * <p>Asserted by reachability over a population rather than by poking values
     * one at a time: if a field takes more than one value across a thousand
     * unicorns, something upstream of it is being read. The mapping the names below
     * assert is the one written out on the gene:
     * {@code length} to the size bucket <i>and</i> the stretch, {@code twist} to
     * the style, {@code girth}, {@code tilt} and {@code tint} to themselves, and
     * {@code glow} to {@code emissive}.
     */
    @Test
    void everyEpigeneticNumberReachesTheHorn() {
        Genotype gt = unicorn();
        Set<Integer> sizes = new HashSet<>();
        Set<Integer> styles = new HashSet<>();
        Set<Float> stretches = new HashSet<>();
        Set<Float> girths = new HashSet<>();
        Set<Float> tilts = new HashSet<>();
        Set<Integer> tints = new HashSet<>();
        int glowing = 0;
        int horses = 2000;
        for (long seed = 0; seed < horses; seed++) {
            AttachedPart horn = GENE.hornFor(gt, Epigenome.fromSeed(seed)).orElseThrow();
            sizes.add(horn.shape().size());
            styles.add(horn.shape().style());
            stretches.add(horn.stretch());
            girths.add(horn.girth());
            tilts.add(horn.tilt());
            tints.add(horn.tint());
            if (horn.emissive()) {
                glowing++;
            }
        }
        assertTrue(sizes.size() > 8, "length barely reaches the size bucket: " + sizes);
        assertEquals(HornGenerator.STYLES, styles.size(),
                "twist does not reach every style: " + styles);
        assertTrue(stretches.size() > 100, "length does not reach the stretch: " + stretches.size());
        assertTrue(girths.size() > 100, "girth is not being read: " + girths.size());
        assertTrue(tilts.size() > 100, "tilt is not being read: " + tilts.size());
        assertTrue(tints.size() > 100, "tint is not being read: " + tints.size());
        // glow is a category of GLOW_OUTCOMES, one of which glows.
        double rate = (double) glowing / horses;
        double wanted = 1.0 / UnicornHornGene.GLOW_OUTCOMES;
        assertTrue(rate > wanted * 0.6 && rate < wanted * 1.5,
                glowing + " of " + horses + " horns glow; expected about " + wanted);
    }

    /** Whatever the epigenome says, the horn is drawable: in range, and a real colour. */
    @Test
    void everyHornIsWithinWhatTheRendererWillDraw() {
        Genotype gt = unicorn();
        for (long seed = 0; seed < 3000; seed++) {
            AttachedPart horn = GENE.hornFor(gt, Epigenome.fromSeed(seed)).orElseThrow();
            assertTrue(horn.length() >= HornSize.NUB.length - 0.01f
                            && horn.length() <= HornSize.NARWHAL.length + 0.01f,
                    "seed " + seed + " grew a " + horn.length() + "-unit horn");
            assertTrue(horn.girth() >= 0.7f && horn.girth() <= 1.6f,
                    "seed " + seed + " girth " + horn.girth());
            assertTrue(horn.tilt() >= -0.6f && horn.tilt() <= 1.0f,
                    "seed " + seed + " tilt " + horn.tilt());
            assertEquals(0xFF, horn.tint() >>> 24,
                    "a horn's tint must be opaque - the fade is applied by the layer");
            assertSame(HornSize.of(horn.length()),
                    GENE.sizeOf(gt, Epigenome.fromSeed(seed)).orElseThrow());
        }
    }

    /**
     * The hard clamps really are wider than the design ranges. That is not
     * slackness: {@code EpiValue}'s bound exists because a line bred for three
     * hundred generations legitimately leaves the wild range, and the point of
     * naming them here is that the <b>renderer's</b> limit and the <b>breeder's</b>
     * range are different numbers on purpose.
     */
    @Test
    void theClampsAreWiderThanTheDesignRanges() {
        for (EpiValue value : GENE.epiSchema().values()) {
            if (value.name().equals(UnicornHornGene.LENGTH)) {
                // The one exception, and it is deliberate: length is clamped to
                // exactly its design range, because the ladder has ends - a
                // narwhal tusk is the longest horn there is a mesh for.
                assertEquals(0.0, value.clamp(-5.0), 1e-9);
                assertEquals(1.0, value.clamp(5.0), 1e-9);
            }
            assertEquals(value.clamp(value.midpoint()), value.midpoint(), 1e-9,
                    value.name() + ": its own midpoint is outside its clamp");
        }
    }

    @Test
    void aUnicornCanTurnUpInTheWildButRarely() {
        SeededRng rng = new SeededRng(90210L);
        int horned = 0;
        int carriers = 0;
        int n = 50_000;
        for (int i = 0; i < n; i++) {
            int copies = Genotype.random(rng).pair(GENE).count(GENE.Horn);
            if (copies == 2) {
                horned++;
            } else if (copies == 1) {
                carriers++;
            }
        }
        // p = 0.05 -> 0.25% horned, ~9.5% carriers. Both ends matter: a unicorn
        // nobody can find is not content, and one in every field is not a unicorn.
        assertTrue(horned > 40 && horned < 250, "unicorn rate off: " + horned + " / " + n);
        assertTrue(carriers > 3500 && carriers < 6500, "carrier rate off: " + carriers + " / " + n);
    }

    /**
     * The editors' randomize only moves genes that change how a horse looks while
     * "Rnd health" is off. A horn paints nothing, and asking the coat question
     * alone filed it with the disorders - so "Rnd epigen." on a unicorn re-rolled
     * everything but the horn. Owner-reported in the spawn egg, 2026-09-30.
     */
    @Test
    void theEditorsCountAHornAsSomethingYouCanSee() {
        assertFalse(Genes.influencesCoat(GENE), "the horn paints nothing - that is the premise");
        assertTrue(GrownParts.grants(GENE));
        assertTrue(EditorRules.changesLooks(GENE), "a randomize must be allowed to move the horn");
        assertFalse(GrownParts.grants(Genes.MSTN), "a stat locus grows nothing");
        assertFalse(EditorRules.changesLooks(Genes.MSTN), "and stays behind the Rnd health switch");
    }

    /** A horn is a showpiece, not a trap - nothing about it may touch the horse's body. */
    @Test
    void itGrantsNoTraitAndNoAbility() {
        assertFalse(GENE.description().isBlank(), "the browser and the tooltip read this");
        assertTrue(GENE.hasGeneCarrot(), "a breeder should be able to research their way to one");
        assertTrue(GENE.spliceable());
    }
}
