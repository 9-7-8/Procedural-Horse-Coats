package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.BreedStatCurve;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.AlleleEpigenetics;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TargetBand;
import com.example.horsegenetics.common.trait.TraitBreakdown;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The pulling-ability locus.</b> Nothing in the game reads
 * {@link Traits#pull()} yet, which is exactly why it is worth pinning now: a
 * number no screen shows is a number that can rot silently, and every claim in
 * {@link MagicPullGene}'s own documentation is checkable without a client.
 *
 * <p>Four things are being held down here. That the arithmetic is the one the
 * gene describes - both copies, signed by their allele, added to a baseline of
 * five. That the locus stays invisible: it paints nothing and costs the coat
 * nothing. That a breed's 1-10 score really arrives on its founders, on the
 * same scale the sheet writes it in. And that the dhampir's two strains land on
 * <b>different</b> scores, which is the case the whole per-strain {@code stats}
 * block exists for.
 */
class MagicPullGeneTest {

    private static final MagicPullGene GENE = Genes.MAGIC_PULL;
    private static final double EPS = 1e-9;

    // ------------------------------------------------------------------
    // The arithmetic
    // ------------------------------------------------------------------

    /** A horse carrying nothing here sits on the baseline, and the baseline is five. */
    @Test
    void aWildTypeHorsePullsAtTheBaseline() {
        Traits t = HorseTraits.resolve(Genotype.wildType());
        assertEquals(HorseTraits.BASE_PULL, t.pull(), EPS);
        assertEquals(5.0, HorseTraits.BASE_PULL, EPS,
                "the breed sheets are written on a 1-10 scale whose middle is five");
    }

    /**
     * One strong copy adds exactly what is written on it, one weak copy
     * subtracts exactly that, and the wild-type partner is worth nothing. This
     * is the claim that makes a pull score readable off a horse's copies.
     */
    @Test
    void oneCopyIsWorthTheNumberWrittenOnIt() {
        assertEquals(5.0 + 1.25, pullOf(pair(GENE.Strong, GENE.n), 1.25, 0.0), EPS);
        assertEquals(5.0 - 1.25, pullOf(pair(GENE.Weak, GENE.n), 1.25, 0.0), EPS);
    }

    /** Codominant: both copies contribute at once, so a homozygote is worth both numbers. */
    @Test
    void bothCopiesAdd() {
        assertEquals(5.0 + 2.5 + 1.5, pullOf(pair(GENE.Strong, GENE.Strong), 2.5, 1.5), EPS);
        assertEquals(5.0 - 2.5 - 1.5, pullOf(pair(GENE.Weak, GENE.Weak), 2.5, 1.5), EPS);
    }

    /**
     * The balanced pair cancels as far as it goes - and only as far as it goes.
     * Two equal copies land back on the baseline; two unequal ones do not, which
     * is what stops {@code Strong/Weak} being a synonym for the wild type.
     */
    @Test
    void aBalancedPairCancelsOnlyAsFarAsTheNumbersMatch() {
        assertEquals(5.0, pullOf(pair(GENE.Strong, GENE.Weak), 2.0, 2.0), EPS);
        double uneven = pullOf(pair(GENE.Strong, GENE.Weak), 3.0, 1.0);
        assertEquals(5.0 + 2.0, uneven, EPS);
        assertSame(GENE.expressionOf(pair(GENE.Strong, GENE.Weak)),
                GENE.expressionOf(pair(GENE.Weak, GENE.Strong)),
                "the pair canonicalises, so which parent sent which copy does not rename the outcome");
    }

    /** Pull never resolves to zero or below - see {@link HorseTraits#MIN_PULL}. */
    @Test
    void pullHasAFloor() {
        double floored = pullOf(pair(GENE.Weak, GENE.Weak), 40.0, 40.0);
        assertEquals(HorseTraits.MIN_PULL, floored, EPS);
        assertTrue(floored > 0.0, "a zero-pull horse is a hitch that cannot exist, not a weak horse");
    }

    /** Nothing multiplies pull, so the breakdown reports one additive term and no factor. */
    @Test
    void theBreakdownReportsPullAsAnAdditiveTerm() {
        Genome g = genome(pair(GENE.Strong, GENE.Strong), 2.0, 1.0);
        List<TraitBreakdown.Term> terms =
                TraitBreakdown.on(TraitBreakdown.of(g.genotype(), g.epigenome(), true), StatAxis.PULL);
        assertEquals(1, terms.size(), "only the pull locus moves pull");
        TraitBreakdown.Term term = terms.get(0);
        assertSame(GENE, term.gene());
        assertEquals(3.0, term.pull(), EPS);
        assertEquals(1.0, term.speedFactor(), EPS, "pull is additive; no axis multiplies for it");
    }

    // ------------------------------------------------------------------
    // The locus stays invisible
    // ------------------------------------------------------------------

    /**
     * Every outcome is a wild type, so the locus is out of the texture key and
     * two horses differing only here share a coat - which is what lets the
     * genotype gallery collapse it to one entry.
     */
    @Test
    void itPaintsNothing() {
        assertFalse(GENE.affectsCoat(), "pull is a body number, not a marking");
        for (var expression : GENE.expressions()) {
            assertTrue(expression.wildType(), expression.id() + " should be a wild-type outcome");
        }
    }

    /** Three alleles, declared up / down / wild - the order BreedFounder indexes by. */
    @Test
    void theAllelesAreStrongWeakAndWildTypeInThatOrder() {
        assertEquals(List.of(GENE.Strong, GENE.Weak, GENE.n), GENE.alleles());
        assertSame(GENE.n, GENE.defaultAllele());
        assertSame(GENE.Strong, GENE.alleles().get(0), "index 0 is the allele a band pushes up to");
        assertSame(GENE.Weak, GENE.alleles().get(1), "index 1 is the one it pushes down to");
    }

    /**
     * No wild horse is born doubled. The founder table lists the two carriers
     * and the plain horse and nothing else, so a homozygote is one somebody
     * bred - the same bargain the three additive stats make.
     */
    @Test
    void onlyHeterozygotesAreBornWild() {
        Rng rng = new SeededRng(4242L);
        int carriers = 0;
        int n = 4000;
        for (int i = 0; i < n; i++) {
            AllelePair p = GENE.founderTable(null).draw(rng);
            assertFalse(p.count(GENE.Strong) == 2 || p.count(GENE.Weak) == 2,
                    "a wild founder should never be doubled at this locus: " + p.toTokens());
            if (p.count(GENE.n) < 2) {
                carriers++;
            }
        }
        assertEquals(MagicPullGene.WILD_CARRIER_PERCENT / 100.0, carriers / (double) n, 0.03);
    }

    // ------------------------------------------------------------------
    // A breed's score reaches its founders
    // ------------------------------------------------------------------

    /**
     * <b>The score is the number.</b> Unlike the three additive axes there is no
     * curve in between, so a Shire founder resolves at about ten and a Falabella
     * at about one - the values the breed sheets literally carry.
     */
    @Test
    void aBreedsFoundersLandOnItsScore() {
        assertScored("shire", 10);
        assertScored("percheron", 10);
        assertScored("suffolk_punch", 10);
        assertScored("falabella", 1);
        assertScored("american_miniature", 1);
        assertScored("cleveland_bay", 7);
        assertScored("caspian_pony", 2);
    }

    /**
     * A breed scored at the baseline carries no pull alleles at all, rather than
     * being forced homozygous for a pushing allele worth nothing. Same
     * near-baseline rule the other four axes follow.
     */
    @Test
    void anOrdinaryBreedIsLeftWildAtTheLocus() {
        Breed b = breed("andalusian");
        assertEquals(5.0, b.scores().pull().orElseThrow().lo(), EPS);
        assertNull(b.statTargets().band(StatAxis.PULL),
                "a score of five is not a target - it is the absence of one");
        for (long seed = 0; seed < 200; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            assertEquals(2, g.genotype().pair(GENE).count(GENE.n), "seed " + seed);
            assertEquals(HorseTraits.BASE_PULL, HorseTraits.resolve(g).pull(), EPS);
        }
    }

    /** Four and six are ordinary scores on the sheets, and both have to survive the neutral window. */
    @Test
    void scoresEitherSideOfTheBaselineAreStillTargets() {
        assertNull(BreedStatCurve.pullBand(5));
        TargetBand up = BreedStatCurve.pullBand(6);
        TargetBand down = BreedStatCurve.pullBand(4);
        assertNotNull(up, "six is a real target");
        assertNotNull(down, "four is a real target");
        assertTrue(up.pushesUp(StatAxis.PULL.baseline()));
        assertFalse(down.pushesUp(StatAxis.PULL.baseline()));
        assertEquals(5.0, StatAxis.PULL.baseline(), EPS);
        assertEquals(1.0, StatAxis.SPEED.baseline(), EPS,
                "the multiplier axes keep measuring against one");
    }

    /**
     * <b>The case the per-strain stats block exists for.</b> A white dhampir
     * founds at nine and a seal brown at six. Copy count cannot say that - two
     * copies of anything are only ever worth twice one - so the number is
     * written on the strain, and a founder gets whichever strain it drew.
     */
    @Test
    void theDhampirsTwoStrainsPullAtDifferentScores() {
        Breed b = breed("dhampir");
        assertTrue(b.scores().pull().isEmpty(), "the dhampir scores pull per strain, not per breed");

        List<Double> white = new ArrayList<>();
        List<Double> brown = new ArrayList<>();
        for (long seed = 0; seed < 600; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            double pull = HorseTraits.resolve(g).pull();
            // Magic white is the strain marker: two copies is the white strain.
            if (g.genotype().pair(Genes.MAGIC_WHITE).count(Genes.MAGIC_WHITE.Wm) == 2) {
                white.add(pull);
            } else {
                brown.add(pull);
            }
        }
        assertFalse(white.isEmpty(), "600 founders should include some whites");
        assertFalse(brown.isEmpty());
        assertEquals(9.0, mean(white), 0.5, "white dhampirs found at nine");
        assertEquals(6.0, mean(brown), 0.5, "seal browns found at six");
        for (double p : white) {
            assertTrue(p > 7.5, "a white dhampir should be well clear of a seal brown, got " + p);
        }
        for (double p : brown) {
            assertTrue(p < 7.5, "a seal brown should be well clear of a white, got " + p);
        }
    }

    /**
     * Every breed on the roster is scored, so a horse's pull is always a
     * decision somebody made rather than a default nobody noticed. The dhampir
     * is the one exception and says so on its strains.
     */
    @Test
    void everyBreedScoresPullSomewhere() {
        for (Breed b : Breeds.all()) {
            if (b == Breeds.FERAL_MIXED) {
                continue;   // the absence of a breed, not a breed
            }
            boolean scored = b.scores().pull().isPresent();
            for (Breed.Strain s : b.strains()) {
                scored |= s.scores().pull().isPresent();
            }
            assertTrue(scored, b.id() + " has no pull score");
        }
    }

    // ------------------------------------------------------------------
    // Inheritance
    // ------------------------------------------------------------------

    /**
     * The number rides on the copy. A foal of two draught horses is a draught
     * horse because of what it inherited, not because anything re-reads its
     * breed - which is the property that lets a bred line leave its breed
     * standard behind.
     */
    @Test
    void aFoalOfTwoStrongParentsIsStrong() {
        Breed shire = breed("shire");
        int stronger = 0;
        int n = 300;
        for (long seed = 0; seed < n; seed++) {
            Rng rng = new SeededRng(seed);
            Genome dam = BreedFounder.roll(shire, rng);
            Genome sire = BreedFounder.roll(shire, rng);
            Genome foal = dam.breedWith(sire, rng);
            double pull = HorseTraits.resolve(foal).pull();
            assertTrue(pull > 7.0,
                    "seed " + seed + ": a foal of two Shires pulls like one, got " + pull);
            if (pull > HorseTraits.BASE_PULL) {
                stronger++;
            }
        }
        assertEquals(n, stronger);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static void assertScored(String id, double expected) {
        Breed b = breed(id);
        assertEquals(expected, b.scores().pull().orElseThrow().lo(), EPS,
                id + "'s sheet should score pull " + expected);
        double total = 0.0;
        int n = 120;
        for (long seed = 0; seed < n; seed++) {
            double pull = HorseTraits.resolve(BreedFounder.roll(b, new SeededRng(seed))).pull();
            assertEquals(expected, pull, 1.0, id + " founder at seed " + seed + " pulls " + pull);
            total += pull;
        }
        assertEquals(expected, total / n, 0.4, id + "'s founders should average its score");
    }

    private static Breed breed(String id) {
        Breed b = Breeds.get(id);
        assertNotNull(b, id + " should be a registered breed");
        return b;
    }

    private static double mean(List<Double> values) {
        double total = 0.0;
        for (double v : values) {
            total += v;
        }
        return total / values.size();
    }

    private static AllelePair pair(Allele a, Allele b) {
        return new AllelePair(a, b);
    }

    /** What a horse with this pair and these two copy numbers resolves to. */
    private static double pullOf(AllelePair p, double first, double second) {
        return HorseTraits.resolve(genome(p, first, second)).pull();
    }

    /**
     * A horse carrying {@code p} with the two numbers written on its copies. The
     * genome is rolled first so the copies exist and are aligned to the pair,
     * then the one value is overwritten - the same two steps
     * {@code BreedFounder.stampStatTargets} takes.
     */
    private static Genome genome(AllelePair p, double first, double second) {
        Genotype g = Genotype.wildType().with(p);
        Genome rolled = Genome.of(g, new SeededRng(7L));
        Epigenome.Copies c = rolled.epigenome().copies(GENE);
        Epigenome epi = rolled.epigenome().with(GENE.key(), new Epigenome.Copies(
                withDelta(c.first(), first), withDelta(c.second(), second)));
        return new Genome(g, epi);
    }

    private static AlleleEpigenetics withDelta(AlleleEpigenetics copy, double delta) {
        return copy.isEmpty()
                ? copy      // a wild-type copy carries nothing, and is worth nothing
                : new AlleleEpigenetics(copy.priority(), copy.values().with(MagicPullGene.DELTA, delta));
    }
}
