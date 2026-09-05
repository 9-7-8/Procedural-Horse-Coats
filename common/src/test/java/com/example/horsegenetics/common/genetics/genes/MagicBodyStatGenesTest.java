package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.testutil.Codes;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three <b>magical body-stat genes</b> - magic speed, magic health and
 * magic jump - as a set, because they share {@link AbstractMagicStatGene} and
 * every claim worth pinning is a claim about the shared shape. Magic body size
 * is the fourth of the family and is covered by {@code MagicalUtilityGenesTest}
 * (it multiplies scale, which carries its own natural clamp).
 *
 * <p>Each entry pairs a gene with the trait accessor it is supposed to move and
 * that trait's all-wild-type baseline.
 */
class MagicBodyStatGenesTest {

    private record Locus(AbstractMagicStatGene gene, String codeName,
                         Function<Traits, Double> stat, double baseline) {
    }

    private static final List<Locus> LOCI = List.of(
            new Locus(Genes.MAGIC_SPEED, "magic_speed", Traits::speed, HorseTraits.BASE_SPEED),
            new Locus(Genes.MAGIC_HEALTH, "magic_health", Traits::health, HorseTraits.BASE_HEALTH),
            new Locus(Genes.MAGIC_JUMP, "magic_jump", Traits::jump, HorseTraits.BASE_JUMP));

    // ------------------------------------------------------------------
    // The table
    // ------------------------------------------------------------------

    /** Three alleles, six combinations, six distinct outcomes - which is what codominance is. */
    @Test
    void everyLocusIsCodominantWithSixOutcomes() {
        for (Locus l : LOCI) {
            AbstractMagicStatGene g = l.gene();
            assertEquals(3, g.alleles().size(), g.key());
            assertEquals(6, g.expressions().size(), g.key());
            assertEquals(6, GenotypeCatalog.allPairsOf(g).size(), g.key());

            Set<String> ids = new HashSet<>();
            Set<Expression> declared = new HashSet<>(g.expressions());
            for (AllelePair pair : GenotypeCatalog.allPairsOf(g)) {
                Expression e = g.expressionOf(pair);
                assertTrue(declared.contains(e), g.key() + " " + pair.toTokens() + " -> undeclared");
                assertTrue(ids.add(e.id()), g.key() + " two combinations share an outcome");
            }
            assertEquals(6, ids.size(), g.key());
        }
    }

    /** Every outcome is a wild type, so none of the three paints or widens the gallery. */
    @Test
    void noneOfThemPaints() {
        for (Locus l : LOCI) {
            assertFalse(l.gene().affectsCoat(), l.gene().key() + " must not paint");
            for (Expression e : l.gene().expressions()) {
                assertTrue(e.wildType(), l.gene().key() + " " + e.id() + " should be a wild type");
            }
            assertEquals(1, GenotypeCatalog.distinctPairsOf(l.gene()).size(),
                    l.gene().key() + " must collapse to one gallery entry");
            assertTrue(Genes.magicalOrder().contains(l.gene()), l.gene().key() + " should be magical");
        }
        // no code segment for them survives coatCode()
        Genotype loaded = Genotype.parse(Codes.of("magic_speed", "Swift/Swift",
                "magic_health", "Hardy/Frail", "magic_jump", "Leaden/n"));
        assertNotEquals(Genotype.wildType().toCode(), loaded.toCode());
        assertEquals(Genotype.wildType().coatCode(), loaded.coatCode());
    }

    // ------------------------------------------------------------------
    // Codominant, and the percentages add
    // ------------------------------------------------------------------

    /** Both copies contribute, and on average two are worth exactly twice one. */
    @Test
    void bothCopiesAddAndAverageToTwice() {
        for (Locus l : LOCI) {
            for (long seed = 0; seed < 200; seed++) {
                double one = Math.abs(factor(l, up(l) + "/n", seed) - 1.0);
                double two = Math.abs(factor(l, up(l) + "/" + up(l), seed) - 1.0);
                assertTrue(two > one, l.gene().key() + ": a second up copy must add, at seed " + seed);
            }
            int n = 2000;
            double one = 0;
            double two = 0;
            for (long seed = 0; seed < n; seed++) {
                one += factor(l, up(l) + "/n", seed) - 1.0;
                two += factor(l, up(l) + "/" + up(l), seed) - 1.0;
            }
            assertEquals(2.0, (two / n) / (one / n), 0.05,
                    l.gene().key() + ": two copies average twice one");
        }
    }

    /** A doubled locus is exactly the sum of its two copies' own percentages. */
    @Test
    void aDoubledStatIsTheSumOfItsTwoCopies() {
        for (Locus l : LOCI) {
            Epigenome epi = Epigenome.fromSeed(31L);
            Epigenome.Copies copies = epi.copies(l.gene().key());
            double first = AbstractMagicStatGene.delta(
                    new SeededRng(copies.first().epigeneticSeed(), l.gene().key()));
            double second = AbstractMagicStatGene.delta(
                    new SeededRng(copies.second().epigeneticSeed(), l.gene().key()));

            double up = statWith(l, up(l) + "/" + up(l), epi) / l.baseline();
            assertEquals(1.0 + first + second, up, 1e-9, l.gene().key());

            double down = statWith(l, down(l) + "/" + down(l), epi) / l.baseline();
            assertEquals(1.0 - first - second, down, 1e-9, l.gene().key());

            double balanced = statWith(l, up(l) + "/" + down(l), epi) / l.baseline();
            assertEquals(1.0 + first - second, balanced, 1e-9, l.gene().key());
        }
    }

    /** The balanced pair stays near the horse's ordinary number - the two draws nearly cancel. */
    @Test
    void theBalancedPairNearlyCancels() {
        for (Locus l : LOCI) {
            for (long seed = 0; seed < 60; seed++) {
                double f = factor(l, up(l) + "/" + down(l), seed);
                assertTrue(Math.abs(f - 1.0) < 0.35,
                        l.gene().key() + " balanced drifted to " + f + " at seed " + seed);
            }
        }
    }

    /** The up allele can never come out subtracting, and the down allele never adding. */
    @Test
    void aVariantCopyNeverPointsTheWrongWay() {
        for (Locus l : LOCI) {
            for (long seed = 0; seed < 1500; seed++) {
                assertTrue(factor(l, up(l) + "/n", seed) > 1.0, l.gene().key() + " up<=1 at " + seed);
                assertTrue(factor(l, down(l) + "/n", seed) < 1.0, l.gene().key() + " down>=1 at " + seed);
            }
        }
    }

    // ------------------------------------------------------------------
    // The distribution
    // ------------------------------------------------------------------

    /** One copy averages a tenth either way, and it is a bell rather than a flat spread. */
    @Test
    void oneCopyAveragesTenPercentAndIsNormal() {
        for (Locus l : LOCI) {
            int n = 3000;
            double upSum = 0;
            double downSum = 0;
            int withinOneSigma = 0;
            for (long seed = 0; seed < n; seed++) {
                double up = factor(l, up(l) + "/n", seed);
                double down = factor(l, down(l) + "/n", seed);
                upSum += up;
                downSum += down;
                if (Math.abs((up - 1.0) - AbstractMagicStatGene.MEAN_DELTA) <= AbstractMagicStatGene.SIGMA_DELTA) {
                    withinOneSigma++;
                }
            }
            assertEquals(1.0 + AbstractMagicStatGene.MEAN_DELTA, upSum / n, 0.01, l.gene().key());
            assertEquals(1.0 - AbstractMagicStatGene.MEAN_DELTA, downSum / n, 0.01, l.gene().key());
            double share = withinOneSigma / (double) n;
            assertTrue(share > 0.60 && share < 0.75,
                    l.gene().key() + ": ~68% should be within one sigma, got " + share);
        }
    }

    /** The per-copy floor: an up allele at the -6 sigma end still adds MIN_DELTA. */
    @Test
    void theFloorHolds() {
        assertEquals(AbstractMagicStatGene.MIN_DELTA, AbstractMagicStatGene.delta(constant(0.0f)), 1e-9);
        assertEquals(AbstractMagicStatGene.MEAN_DELTA + 6 * AbstractMagicStatGene.SIGMA_DELTA,
                AbstractMagicStatGene.delta(constant(1.0f)), 1e-6);
    }

    // ------------------------------------------------------------------
    // Determinism, midpoint, composition
    // ------------------------------------------------------------------

    /** Same genome, same number - every time, which is what makes it heritable. */
    @Test
    void theStatIsStableForOneGenome() {
        for (Locus l : LOCI) {
            String code = Codes.of(l.codeName(), up(l) + "/n");
            assertEquals(statWith(l, up(l) + "/n", Epigenome.fromSeed(42L)),
                    statWith(l, up(l) + "/n", Epigenome.fromSeed(42L)), 0.0);
            assertNotEquals(statWith(l, up(l) + "/n", Epigenome.fromSeed(42L)),
                    statWith(l, up(l) + "/n", Epigenome.fromSeed(43L)));
        }
    }

    /** With no epigenome the answer is the midpoint of the distribution: exactly +/-10% / +/-20%. */
    @Test
    void withoutAnEpigenomeTheStatIsTheMidpoint() {
        for (Locus l : LOCI) {
            double m = AbstractMagicStatGene.MEAN_DELTA;
            assertEquals(l.baseline() * (1 + m), midpointStat(l, up(l) + "/n"), 1e-9, l.gene().key());
            assertEquals(l.baseline() * (1 + 2 * m), midpointStat(l, up(l) + "/" + up(l)), 1e-9, l.gene().key());
            assertEquals(l.baseline() * (1 - m), midpointStat(l, down(l) + "/n"), 1e-9, l.gene().key());
            assertEquals(l.baseline(), midpointStat(l, up(l) + "/" + down(l)), 1e-9, l.gene().key());
        }
    }

    /**
     * It <b>multiplies</b> whatever the natural loci settled on: a magically
     * fast horse that is also MSTN {@code C/C} ends up faster than a magically
     * fast horse that is not, by the same ratio.
     */
    @Test
    void theMagicScalesTheNaturalStat() {
        Genotype plainFast = Genotype.parse(Codes.of("magic_speed", "Swift/Swift"));
        Genotype sprintFast = Genotype.parse(Codes.of("magic_speed", "Swift/Swift", "mstn", "C/C"));
        Genotype sprintOnly = Genotype.parse(Codes.of("mstn", "C/C"));

        double magicRatio = HorseTraits.resolve(plainFast).speed() / HorseTraits.BASE_SPEED;
        assertEquals(HorseTraits.resolve(sprintOnly).speed() * magicRatio,
                HorseTraits.resolve(sprintFast).speed(), 1e-9,
                "the magic multiplies the natural speed, it does not add to it");
    }

    /** Health still cannot resolve to zero - the MIN_HEALTH floor is applied after the multiply. */
    @Test
    void magicHealthCannotZeroAHorse() {
        Genotype worst = Genotype.parse(Codes.of(
                "magic_health", "Frail/Frail",
                "acan", "D1/D1",
                "plod1", "ffs/ffs"));
        Traits t = HorseTraits.resolve(worst);
        assertEquals(HorseTraits.MIN_HEALTH, t.health(), 1e-12);
    }

    /**
     * None of the three is a {@link com.example.horsegenetics.common.trait.HealthContribution}:
     * the config's "off" position governs disorders, and must not quietly strip
     * a horse's magical vigour with them.
     */
    @Test
    void theConfigOffPositionLeavesThemAlone() {
        Genotype g = Genotype.parse(Codes.of("magic_health", "Hardy/Hardy", "magic_speed", "Swift/n"));
        assertEquals(HorseTraits.resolve(g, true), HorseTraits.resolve(g, false));
        assertTrue(HorseTraits.resolve(g, false).health() > HorseTraits.BASE_HEALTH);
    }

    /** Only heterozygotes spawn wild, and about 80% of wild horses carry a copy of each. */
    @Test
    void onlyHeterozygotesSpawnWildAndMostHorsesCarryOne() {
        for (Locus l : LOCI) {
            AbstractMagicStatGene g = l.gene();
            int carriers = 0;
            int n = 20_000;
            for (long seed = 0; seed < n; seed++) {
                AllelePair pair = Genotype.random(new SeededRng(seed)).pair(g);
                assertFalse(pair.homozygousFor(g.up), g.key() + " wild up/up at " + seed);
                assertFalse(pair.homozygousFor(g.down), g.key() + " wild down/down at " + seed);
                assertFalse(pair.has(g.up) && pair.has(g.down), g.key() + " wild up/down at " + seed);
                if (!pair.homozygousFor(g.n)) {
                    carriers++;
                }
            }
            assertEquals(AbstractMagicStatGene.WILD_CARRIER_PERCENT / 100.0, carriers / (double) n, 0.02,
                    g.key() + ": most wild horses should carry a copy");
        }
    }

    /** The four magical body-stat genes really are four independent loci. */
    @Test
    void theFourBodyStatLociAreIndependent() {
        Set<String> keys = new HashSet<>();
        for (Gene g : List.of(Genes.MAGIC_SPEED, Genes.MAGIC_HEALTH, Genes.MAGIC_JUMP, Genes.BODY_SIZE)) {
            assertTrue(keys.add(g.key()), "duplicate key " + g.key());
        }
        Genotype all = Genotype.parse(Codes.of(
                "magic_speed", "Swift/Swift",
                "magic_health", "Hardy/Hardy",
                "magic_jump", "Springy/Springy",
                "body_size", "Big/Big"));
        Traits t = HorseTraits.resolve(all, Epigenome.fromSeed(7L), true);
        assertTrue(t.speed() > HorseTraits.BASE_SPEED);
        assertTrue(t.health() > HorseTraits.BASE_HEALTH);
        assertTrue(t.jump() > HorseTraits.BASE_JUMP);
        assertTrue(t.scale() > HorseTraits.BASE_SCALE);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static String up(Locus l) {
        return l.gene().up.token();
    }

    private static String down(Locus l) {
        return l.gene().down.token();
    }

    private static double statWith(Locus l, String tokens, Epigenome epi) {
        Genotype g = Genotype.parse(Codes.of(l.codeName(), tokens));
        return l.stat().apply(HorseTraits.resolve(g, epi, true));
    }

    private static double midpointStat(Locus l, String tokens) {
        Genotype g = Genotype.parse(Codes.of(l.codeName(), tokens));
        return l.stat().apply(HorseTraits.resolve(g));
    }

    /** The multiplier this locus applied, i.e. the resolved stat over its baseline. */
    private static double factor(Locus l, String tokens, long seed) {
        return statWith(l, tokens, Epigenome.fromSeed(seed)) / l.baseline();
    }

    private static Rng constant(float v) {
        return new Rng() {
            @Override public float nextFloat() { return v; }
            @Override public boolean nextBoolean() { return false; }
            @Override public int nextInt(int bound) { return 0; }
            @Override public long nextLong() { return 0L; }
        };
    }
}
