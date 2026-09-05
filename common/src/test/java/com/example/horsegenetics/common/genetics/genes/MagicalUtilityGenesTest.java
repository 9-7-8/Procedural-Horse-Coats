package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.MidpointRng;
import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.testutil.Codes;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Severity;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.common.trait.Viability;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seven <b>magical utility genes</b> - milk, magic body size, mane colour,
 * tail colour, light, healer and verdant - as a set, because they were designed
 * as one and most of the claims worth pinning are about how they behave
 * together rather than one at a time.
 *
 * <p>What is pinned here:
 * <ul>
 *   <li>every combination table is <b>total</b>, and every outcome a gene
 *       declares is reachable by a horse that can exist;</li>
 *   <li>the three genes that paint nothing really do paint nothing, so they
 *       stay out of the texture key and the catalogue;</li>
 *   <li>milk's water/lava heterozygote is an <b>embryonic lethal</b> - absent
 *       from the founder population, ruled out of the catalogue, and reported
 *       as lethal at conception;</li>
 *   <li>magic size is <b>epigenetic</b>: one genotype spans a wide range of
 *       horses, the same seed always gives the same one, and it can carry a
 *       horse past the natural size clamp;</li>
 *   <li>the two hair loci are independent and their heterozygote really does
 *       use <b>two</b> colours;</li>
 *   <li>light is codominant - two variant copies show both regions;</li>
 *   <li>the abilities each gene grants are the ones it claims to.</li>
 * </ul>
 */
class MagicalUtilityGenesTest {

    private static final List<Gene> UTILITY = List.of(
            Genes.MILK, Genes.BODY_SIZE, Genes.MANE_COLOR, Genes.TAIL_COLOR,
            Genes.LIGHT, Genes.HEALER, Genes.VERDANT);

    // ------------------------------------------------------------------
    // The tables
    // ------------------------------------------------------------------

    /** Every combination lands on a declared outcome - including ones no horse can carry. */
    @Test
    void everyCombinationOfEveryLocusLandsOnADeclaredOutcome() {
        for (Gene gene : UTILITY) {
            Set<Expression> declared = new HashSet<>(gene.expressions());
            int seen = 0;
            for (Allele a : gene.alleles()) {
                for (Allele b : gene.alleles()) {
                    if (a.order() > b.order()) {
                        continue;
                    }
                    Expression e = gene.expressionOf(new AllelePair(a, b));
                    assertTrue(declared.contains(e),
                            gene.key() + " " + a.token() + "/" + b.token() + " -> undeclared " + e);
                    seen++;
                }
            }
            int n = gene.alleles().size();
            assertEquals(n * (n + 1) / 2, seen, gene.key() + " should answer for every combination");
        }
    }

    /**
     * No dead rows: a declared outcome nothing can reach lies to the wiki.
     *
     * <p>"Reachable" here means <i>by some combination</i>, not by one a horse
     * can carry - milk's water/lava clash is an embryonic lethal, so nothing
     * ever carries it, and it still needs an outcome because parsing is tolerant
     * and a hand-written code can name it. That is the only such row in the
     * seven, and the next test pins it down by name.
     */
    @Test
    void everyDeclaredOutcomeIsReachable() {
        for (Gene gene : UTILITY) {
            Set<Expression> reached = new HashSet<>();
            for (Allele a : gene.alleles()) {
                for (Allele b : gene.alleles()) {
                    if (a.order() <= b.order()) {
                        reached.add(gene.expressionOf(new AllelePair(a, b)));
                    }
                }
            }
            assertEquals(new HashSet<>(gene.expressions()), reached,
                    gene.key() + " declares an outcome no combination reaches");
        }
    }

    /**
     * And the only outcome a live horse cannot show is milk's lethal. Every
     * other row of every other table describes a horse somebody could actually
     * be looking at.
     */
    @Test
    void theOnlyOutcomeNoHorseCanShowIsTheMilkLethal() {
        for (Gene gene : UTILITY) {
            Set<Expression> carryable = new HashSet<>();
            for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
                carryable.add(gene.expressionOf(pair));
            }
            Set<Expression> unreachable = new HashSet<>(gene.expressions());
            unreachable.removeAll(carryable);
            if (gene == Genes.MILK) {
                assertEquals(1, unreachable.size(), "milk should have exactly one unshowable outcome");
                assertEquals("milk-lethal", unreachable.iterator().next().id());
            } else {
                assertTrue(unreachable.isEmpty(), gene.key() + " has unreachable outcomes " + unreachable);
            }
        }
    }

    /**
     * Milk, size and verdant are heritable and do plenty - and none of it is a
     * pixel. So they are out of the texture key and the genotype gallery
     * collapses each of them to a single entry, exactly as the non-coat body
     * loci do.
     */
    @Test
    void theThreeGenesThatPaintNothingStayOutOfTheCoat() {
        for (Gene gene : List.of(Genes.MILK, Genes.BODY_SIZE, Genes.VERDANT)) {
            assertFalse(gene.affectsCoat(), gene.key() + " should not affect the coat");
            assertEquals(1, GenotypeCatalog.distinctPairsOf(gene).size(),
                    gene.key() + " should collapse to one gallery entry");
        }
        for (Gene gene : List.of(Genes.MANE_COLOR, Genes.TAIL_COLOR, Genes.LIGHT, Genes.HEALER)) {
            assertTrue(gene.affectsCoat(), gene.key() + " should affect the coat");
        }
    }

    /** Every one of them is magical - none may restrict pigment in phase 1. */
    @Test
    void allSevenAreMagical() {
        for (Gene gene : UTILITY) {
            assertFalse(gene.isNatural(), gene.key() + " should be a magical gene");
            assertTrue(Genes.magicalOrder().contains(gene), gene.key() + " should be in magicalOrder()");
        }
    }

    // ------------------------------------------------------------------
    // Milk
    // ------------------------------------------------------------------

    /**
     * The two magical variants are recessive to the wild type <b>and to each
     * other</b>: one copy of either is a carrier that still gives ordinary milk.
     */
    @Test
    void oneMilkVariantCopyShowsNothing() {
        MilkGene milk = Genes.MILK;
        assertEquals("minecraft:milk_bucket", milk.yieldItem(new AllelePair(milk.n, milk.n)));
        assertEquals("minecraft:milk_bucket", milk.yieldItem(new AllelePair(milk.Watr, milk.n)));
        assertEquals("minecraft:milk_bucket", milk.yieldItem(new AllelePair(milk.Lava, milk.n)));
        assertEquals("minecraft:water_bucket", milk.yieldItem(new AllelePair(milk.Watr, milk.Watr)));
        assertEquals("minecraft:lava_bucket", milk.yieldItem(new AllelePair(milk.Lava, milk.Lava)));
    }

    /**
     * Water beside lava is an embryonic lethal, and all three of the things that
     * follow from that hold: no such horse can occur, no founder draw produces
     * one, and the trait walk reports it as lethal at conception so the breeding
     * handler cancels the birth.
     */
    @Test
    void waterBesideLavaIsAnEmbryonicLethal() {
        MilkGene milk = Genes.MILK;
        AllelePair clash = new AllelePair(milk.Watr, milk.Lava);

        assertFalse(milk.canOccur(clash));
        assertFalse(GenotypeCatalog.allPairsOf(milk).contains(clash));
        assertEquals(0.0, milk.founderTable(null).share(clash), 0.0);

        Traits traits = HorseTraits.resolve(Genotype.parse(Codes.of("milk", "Watr/Lava")));
        assertEquals(Viability.LETHAL_AT_CONCEPTION, traits.viability());
        assertEquals(Severity.LETHAL_AT_CONCEPTION,
                traits.lethalCondition().orElseThrow().severity());
    }

    /** The far commoner combinations are perfectly viable - only the clash is ruled out. */
    @Test
    void everyOtherMilkCombinationIsViable() {
        MilkGene milk = Genes.MILK;
        for (AllelePair pair : GenotypeCatalog.allPairsOf(milk)) {
            Traits traits = HorseTraits.resolve(Genotype.parse(Codes.of("milk", pair.toTokens())));
            assertEquals(Viability.VIABLE, traits.viability(), pair.toTokens() + " should be viable");
        }
    }

    /** A horse yields exactly one thing, and it is the one its combination says. */
    @Test
    void milkGrantsExactlyOneYieldAbility() {
        MilkGene milk = Genes.MILK;
        for (AllelePair pair : GenotypeCatalog.allPairsOf(milk)) {
            List<GeneAbility> abilities = milk.abilitiesFor(pair, Genotype.wildType());
            assertEquals(1, abilities.size(), pair.toTokens());
            GeneAbility.Yield y = (GeneAbility.Yield) abilities.get(0);
            assertEquals("minecraft:bucket", y.consumes());
            assertEquals(milk.yieldItem(pair), y.produces());
        }
    }

    // ------------------------------------------------------------------
    // Magic body size
    // ------------------------------------------------------------------

    /**
     * Codominant: <b>both</b> copies contribute and the percentages add, which is
     * the whole change from the first draft, where a second copy bought nothing.
     *
     * <p>A second copy always adds <i>something</i> (the per-copy floor is
     * positive) but not reliably as much as the first - it is its own draw off
     * its own seed, and it may have rolled low. So the strong claim is about the
     * <b>average</b>, and the per-seed claim is only that more is more.
     */
    @Test
    void bothSizeCopiesContributeAndTheirPercentagesAdd() {
        for (long seed = 0; seed < 200; seed++) {
            double one = scaleOf(Codes.of("body_size", "Big/n"), seed) - 1.0;
            double two = scaleOf(Codes.of("body_size", "Big/Big"), seed) - 1.0;
            assertTrue(two > one, "a second big copy must add something, at seed " + seed);

            double oneSmall = 1.0 - scaleOf(Codes.of("body_size", "Small/n"), seed);
            double twoSmall = 1.0 - scaleOf(Codes.of("body_size", "Small/Small"), seed);
            assertTrue(twoSmall > oneSmall, "and the same in reverse, at seed " + seed);
        }

        int n = 2000;
        double one = 0;
        double two = 0;
        for (long seed = 0; seed < n; seed++) {
            one += scaleOf(Codes.of("body_size", "Big/n"), seed) - 1.0;
            two += scaleOf(Codes.of("body_size", "Big/Big"), seed) - 1.0;
        }
        assertEquals(2.0, (two / n) / (one / n), 0.05,
                "on average two copies are worth exactly twice one");
    }

    /**
     * The doubled combination is exactly the sum of the two copies' own
     * percentages - which is what "the percentages add" has to mean if the
     * heterozygote's number is to stay meaningful.
     */
    @Test
    void aDoubledSizeIsTheSumOfItsTwoCopies() {
        Epigenome epi = Epigenome.fromSeed(31L);
        Epigenome.Copies copies = epi.copies(MagicSizeGene.KEY);
        double first = MagicSizeGene.delta(new SeededRng(copies.first().epigeneticSeed(), MagicSizeGene.KEY));
        double second = MagicSizeGene.delta(new SeededRng(copies.second().epigeneticSeed(), MagicSizeGene.KEY));

        double big = HorseTraits.resolve(Genotype.parse(Codes.of("body_size", "Big/Big")), epi, true).scale();
        assertEquals(1.0 + first + second, big, 1e-9);

        double small = HorseTraits.resolve(Genotype.parse(Codes.of("body_size", "Small/Small")), epi, true).scale();
        assertEquals(1.0 - first - second, small, 1e-9);

        // one of each subtracts one from the other, so it lands near 1.0
        double mixed = HorseTraits.resolve(Genotype.parse(Codes.of("body_size", "Big/Small")), epi, true).scale();
        assertEquals(1.0 + first - second, mixed, 1e-9);
    }

    /**
     * {@code Big/Small} is <b>near</b> 1.0, not exactly on it: the two
     * percentages are independent draws. Exact cancellation would need a special
     * case, and the residual is worth having - a horse a hair off ordinary size
     * is carrying both extremes.
     */
    @Test
    void bigBesideSmallNearlyCancels() {
        for (long seed = 0; seed < 60; seed++) {
            double scale = scaleOf(Codes.of("body_size", "Big/Small"), seed);
            assertTrue(Math.abs(scale - 1.0) < 0.35,
                    "Big/Small should stay near ordinary size, got " + scale);
        }
    }

    /** The two directions oppose, at one copy and at two. */
    @Test
    void theTwoDirectionsOppose() {
        for (long seed : new long[] {11L, 19L, 23L}) {
            double plain = scaleOf(Codes.wildType(), seed);
            assertEquals(1.0, plain, 1e-9, "an n/n horse is untouched by this locus");
            assertTrue(scaleOf(Codes.of("body_size", "Big/n"), seed) > plain);
            assertTrue(scaleOf(Codes.of("body_size", "Big/Big"), seed) > plain);
            assertTrue(scaleOf(Codes.of("body_size", "Small/n"), seed) < plain);
            assertTrue(scaleOf(Codes.of("body_size", "Small/Small"), seed) < plain);
        }
    }

    /**
     * The distribution is normal and <b>centred on 1.1x / 0.9x</b> for one copy.
     * Averaged over enough horses the centre has to land there, or the whole
     * "most horses carry it and it is usually subtle" design is not what ships.
     */
    @Test
    void oneCopyAveragesTenPercent() {
        int n = 3000;
        double bigSum = 0;
        double smallSum = 0;
        for (long seed = 0; seed < n; seed++) {
            bigSum += scaleOf(Codes.of("body_size", "Big/n"), seed);
            smallSum += scaleOf(Codes.of("body_size", "Small/n"), seed);
        }
        assertEquals(1.0 + MagicSizeGene.MEAN_DELTA, bigSum / n, 0.01);
        assertEquals(1.0 - MagicSizeGene.MEAN_DELTA, smallSum / n, 0.01);
    }

    /**
     * ...and it is a <b>bell</b>, not a flat spread: most of the population sits
     * within one standard deviation, which is what keeps a single copy subtle on
     * most horses while still giving a paddock a visible range.
     */
    @Test
    void theSizeDrawIsNormallyDistributed() {
        int n = 3000;
        int withinOneSigma = 0;
        double min = Double.MAX_VALUE;
        double max = 0;
        for (long seed = 0; seed < n; seed++) {
            double delta = scaleOf(Codes.of("body_size", "Big/n"), seed) - 1.0;
            if (Math.abs(delta - MagicSizeGene.MEAN_DELTA) <= MagicSizeGene.SIGMA_DELTA) {
                withinOneSigma++;
            }
            min = Math.min(min, delta);
            max = Math.max(max, delta);
        }
        double share = withinOneSigma / (double) n;
        assertTrue(share > 0.60 && share < 0.75,
                "a normal distribution puts ~68% within one sigma, got " + share);
        assertTrue(max - min > 3 * MagicSizeGene.SIGMA_DELTA, "the tails should still reach");
    }

    /** The floor holds: a "big" allele can never come out making a horse smaller. */
    @Test
    void aVariantCopyNeverPointsTheWrongWay() {
        assertEquals(MagicSizeGene.MIN_DELTA, MagicSizeGene.delta(constant(0.0f)), 1e-9);
        for (long seed = 0; seed < 2000; seed++) {
            assertTrue(scaleOf(Codes.of("body_size", "Big/n"), seed) > 1.0);
            assertTrue(scaleOf(Codes.of("body_size", "Small/n"), seed) < 1.0);
        }
    }

    /** The bounded Gaussian: 0 and 1 are its two ends, and 0.5 is exactly the mean. */
    @Test
    void theGaussianIsBoundedAndCentred() {
        assertEquals(0.0, MidpointRng.INSTANCE.nextGaussian(), 1e-9);
        assertEquals(-6.0, constant(0.0f).nextGaussian(), 1e-6);
        assertEquals(6.0, constant(1.0f).nextGaussian(), 1e-6);
        assertEquals(MagicSizeGene.MEAN_DELTA + 6 * MagicSizeGene.SIGMA_DELTA,
                MagicSizeGene.delta(constant(1.0f)), 1e-6);
    }

    /**
     * The magical locus can still carry a horse past the bound that keeps the
     * natural size loci honest - that is the whole difference between it and
     * {@code LCORL}. It takes <b>two</b> good copies now rather than one lucky
     * roll, which is the point of making it codominant.
     */
    @Test
    void twoGoodCopiesReachPastTheNaturalScaleClamp() {
        double best = 1.0 + 2 * MagicSizeGene.delta(constant(1.0f));
        assertTrue(best > HorseTraits.MAX_SCALE,
                "two maximal big copies should exceed the natural clamp, got " + best);
        assertTrue(best <= HorseTraits.MAGICAL_MAX_SCALE);
        assertEquals(MagicSizeGene.MAX_FACTOR_APPROX, best, 1e-9);
        // and one copy alone cannot - a giant is a breeding result, not a catch
        assertTrue(1.0 + MagicSizeGene.delta(constant(1.0f)) < HorseTraits.MAX_SCALE);
    }

    /** Same genome, same horse - every time it is asked, which is what makes it heritable. */
    @Test
    void theSizeIsStableForOneGenome() {
        String code = Codes.of("body_size", "Big/n");
        assertEquals(scaleOf(code, 42L), scaleOf(code, 42L), 0.0);
        assertNotEquals(scaleOf(code, 42L), scaleOf(code, 43L));
    }

    /** With no epigenome to read, the answer is the midpoint of the distribution. */
    @Test
    void withoutAnEpigenomeTheSizeIsTheMidpoint() {
        assertEquals(1.0 + MagicSizeGene.MEAN_DELTA,
                HorseTraits.resolve(Genotype.parse(Codes.of("body_size", "Big/n"))).scale(), 1e-9);
        assertEquals(1.0 + 2 * MagicSizeGene.MEAN_DELTA,
                HorseTraits.resolve(Genotype.parse(Codes.of("body_size", "Big/Big"))).scale(), 1e-9);
        assertEquals(1.0 - MagicSizeGene.MEAN_DELTA,
                HorseTraits.resolve(Genotype.parse(Codes.of("body_size", "Small/n"))).scale(), 1e-9);
        assertEquals(1.0,
                HorseTraits.resolve(Genotype.parse(Codes.of("body_size", "Big/Small"))).scale(), 1e-9);
    }

    /**
     * <b>Only heterozygotes are born wild</b>, and most horses are one. Every
     * doubled horse in the world is therefore something somebody bred - the same
     * rule the health loci use, pointed at something worth having.
     */
    @Test
    void onlyHeterozygousSizeHorsesSpawnInTheWild() {
        MagicSizeGene size = Genes.BODY_SIZE;
        int carriers = 0;
        int n = 20_000;
        for (long seed = 0; seed < n; seed++) {
            AllelePair pair = Genotype.random(new SeededRng(seed)).pair(size);
            assertFalse(pair.homozygousFor(size.Big), "a wild Big/Big at seed " + seed);
            assertFalse(pair.homozygousFor(size.Small), "a wild Small/Small at seed " + seed);
            assertFalse(pair.has(size.Big) && pair.has(size.Small), "a wild Big/Small at seed " + seed);
            if (!pair.homozygousFor(size.n)) {
                carriers++;
            }
        }
        assertEquals(MagicSizeGene.WILD_CARRIER_PERCENT / 100.0, carriers / (double) n, 0.02,
                "most wild horses should carry a copy");
    }

    /** Every combination is its own outcome - which is what a codominant locus is. */
    @Test
    void everySizeCombinationIsItsOwnOutcome() {
        MagicSizeGene size = Genes.BODY_SIZE;
        assertEquals(6, size.expressions().size());
        assertEquals(6, GenotypeCatalog.allPairsOf(size).size());
        Set<String> ids = new HashSet<>();
        for (AllelePair pair : GenotypeCatalog.allPairsOf(size)) {
            assertTrue(ids.add(size.expressionOf(pair).id()), "two combinations share an outcome");
        }
        assertEquals(6, ids.size());
    }

    private static double scaleOf(String code, long seed) {
        Genotype genotype = Genotype.parse(code);
        return HorseTraits.resolve(genotype, Epigenome.fromSeed(seed), true).scale();
    }

    /** An {@link Rng} whose every uniform draw is {@code v} - for pinning the distribution's ends. */
    private static Rng constant(float v) {
        return new Rng() {
            @Override public float nextFloat() { return v; }
            @Override public boolean nextBoolean() { return false; }
            @Override public int nextInt(int bound) { return 0; }
            @Override public long nextLong() { return 0L; }
        };
    }

    // ------------------------------------------------------------------
    // Mane and tail colour
    // ------------------------------------------------------------------

    /** Two loci, not one: a horse can be coloured at one end and ordinary at the other. */
    @Test
    void maneAndTailAreIndependentLoci() {
        assertNotEquals(Genes.MANE_COLOR.key(), Genes.TAIL_COLOR.key());
        Genotype maneOnly = Genotype.parse(Codes.of("mane_color", "Mnsld/n"));
        assertTrue(Genes.MANE_COLOR.isVisible(maneOnly.pair(Genes.MANE_COLOR), maneOnly));
        assertFalse(Genes.TAIL_COLOR.isVisible(maneOnly.pair(Genes.TAIL_COLOR), maneOnly));
    }

    /**
     * The heterozygote is a look neither allele makes alone, so it has to be its
     * own outcome - and it has to be non-deterministic, since both of its
     * colours come off the copies.
     */
    @Test
    void theHairHeterozygoteIsItsOwnVaryingOutcome() {
        for (HairColorGene gene : List.of(Genes.MANE_COLOR, Genes.TAIL_COLOR)) {
            Expression both = gene.expressionOf(new AllelePair(gene.solid(), gene.striped()));
            assertEquals("solid-striped", both.id());
            assertNotEquals(both, gene.expressionOf(new AllelePair(gene.solid(), gene.solid())));
            assertNotEquals(both, gene.expressionOf(new AllelePair(gene.striped(), gene.striped())));
            for (Expression e : gene.expressions()) {
                assertEquals(e.wildType(), e.deterministic(),
                        gene.key() + " " + e.id() + ": a painted hair outcome must vary per horse");
            }
        }
    }

    /**
     * Two independent colours means two independent hues, so the same seed has
     * to produce different colours for the two copies - which is exactly the
     * thing asking for "the expressed copy" would get wrong.
     */
    @Test
    void theTwoHairCopiesCarryDifferentColours() {
        Genotype genotype = Genotype.parse(Codes.of("mane_color", "Mnsld/Mnstrp"));
        int differing = 0;
        for (long seed = 0; seed < 20; seed++) {
            Epigenome epi = Epigenome.fromSeed(seed);
            Epigenome.Copies copies = epi.copies(ManeColorGene.KEY);
            int first = com.example.horsegenetics.common.coat.pattern.HairPattern.randomBrightColour(
                    new SeededRng(copies.first().epigeneticSeed(), ManeColorGene.KEY));
            int second = com.example.horsegenetics.common.coat.pattern.HairPattern.randomBrightColour(
                    new SeededRng(copies.second().epigeneticSeed(), ManeColorGene.KEY));
            if (first != second) {
                differing++;
            }
        }
        assertEquals(20, differing, "the two copies should never agree on a colour by accident");
        assertTrue(genotype.pair(Genes.MANE_COLOR).has(Genes.MANE_COLOR.solid()));
    }

    // ------------------------------------------------------------------
    // Light
    // ------------------------------------------------------------------

    /**
     * Ten combinations, seven outcomes, and the heterozygotes show <b>both</b>
     * regions - which is codominance, and is the thing a dominance ranking of
     * three alleles could not have expressed.
     */
    @Test
    void lightIsCodominantAcrossThreeVariants() {
        LightGene light = Genes.LIGHT;
        assertEquals(4, light.alleles().size());
        assertEquals(7, light.expressions().size());
        assertEquals(10, GenotypeCatalog.allPairsOf(light).size());

        assertEquals(Set.of(LightGene.Region.HOOVES),
                light.regionsOf(new AllelePair(light.Lthf, light.n)));
        assertEquals(Set.of(LightGene.Region.HOOVES),
                light.regionsOf(new AllelePair(light.Lthf, light.Lthf)));
        assertEquals(Set.of(LightGene.Region.HOOVES, LightGene.Region.MANE),
                light.regionsOf(new AllelePair(light.Lthf, light.Ltmn)));
        assertEquals(Set.of(LightGene.Region.MANE, LightGene.Region.EYES),
                light.regionsOf(new AllelePair(light.Ltmn, light.Lteye)));
        assertEquals(Set.of(), light.regionsOf(new AllelePair(light.n, light.n)));
    }

    /** Any variant copy lights the horse, and it is always the same torch-strength. */
    @Test
    void everyGlowingCombinationEmitsTheSameLight() {
        LightGene light = Genes.LIGHT;
        for (AllelePair pair : GenotypeCatalog.allPairsOf(light)) {
            List<GeneAbility> abilities = light.abilitiesFor(pair, Genotype.wildType());
            if (light.regionsOf(pair).isEmpty()) {
                assertTrue(abilities.isEmpty(), pair.toTokens() + " should grant nothing");
                continue;
            }
            assertEquals(1, abilities.size(), pair.toTokens());
            assertEquals(LightGene.LIGHT_LEVEL, ((GeneAbility.Glow) abilities.get(0)).light());
        }
    }

    // ------------------------------------------------------------------
    // Healer
    // ------------------------------------------------------------------

    /** Recessive: one copy is an invisible carrier, two are a healer. */
    @Test
    void onlyTwoHealerCopiesDoAnything() {
        HealerGene healer = Genes.HEALER;
        assertFalse(healer.isHealer(new AllelePair(healer.Hlr, healer.n)));
        assertTrue(healer.isHealer(new AllelePair(healer.Hlr, healer.Hlr)));
        assertTrue(healer.abilitiesFor(new AllelePair(healer.Hlr, healer.n), Genotype.wildType()).isEmpty());

        List<GeneAbility> aura = healer.abilitiesFor(new AllelePair(healer.Hlr, healer.Hlr), Genotype.wildType());
        assertEquals(1, aura.size());
        GeneAbility.Healing h = (GeneAbility.Healing) aura.get(0);
        assertEquals(HealerGene.HEAL_RADIUS, h.radius(), 0.0);
        assertEquals("players", h.target());
    }

    /** The mark varies and the effect does not - a coat you can read must not be a stat sheet. */
    @Test
    void theHealerStripeVariesButTheAuraDoesNot() {
        HealerGene healer = Genes.HEALER;
        AllelePair pair = new AllelePair(healer.Hlr, healer.Hlr);
        assertFalse(healer.expressionOf(pair).deterministic());
        assertEquals(healer.abilitiesFor(pair, Genotype.wildType()),
                healer.abilitiesFor(pair, Genotype.parse(Codes.of("extension", "e/e"))));
    }

    // ------------------------------------------------------------------
    // Verdant
    // ------------------------------------------------------------------

    /** Every variant needs two of itself: a mixed pair is inert, not half of each. */
    @Test
    void onlyMatchingVerdantCopiesSpreadAnything() {
        VerdantGene v = Genes.VERDANT;
        assertEquals("mycelium", v.coverOf(new AllelePair(v.mush, v.mush)));
        assertEquals("moss", v.coverOf(new AllelePair(v.moss, v.moss)));
        assertEquals("grass", v.coverOf(new AllelePair(v.grass, v.grass)));
        for (AllelePair mixed : List.of(
                new AllelePair(v.mush, v.moss), new AllelePair(v.mush, v.grass),
                new AllelePair(v.moss, v.grass), new AllelePair(v.grass, v.n),
                new AllelePair(v.n, v.n))) {
            assertEquals("", v.coverOf(mixed), mixed.toTokens() + " should spread nothing");
            assertTrue(v.abilitiesFor(mixed, Genotype.wildType()).isEmpty(), mixed.toTokens());
        }
    }

    /** The cover a horse spreads is the one its combination names. */
    @Test
    void verdantGrantsTheSpreadItNames() {
        VerdantGene v = Genes.VERDANT;
        for (Allele variant : List.of(v.mush, v.moss, v.grass)) {
            AllelePair pair = new AllelePair(variant, variant);
            List<GeneAbility> abilities = v.abilitiesFor(pair, Genotype.wildType());
            assertEquals(1, abilities.size());
            assertEquals(v.coverOf(pair), ((GeneAbility.Spread) abilities.get(0)).cover());
        }
    }

    // ------------------------------------------------------------------
    // Founders
    // ------------------------------------------------------------------

    /**
     * The founder tables have to agree with the tables above: nothing that
     * cannot occur may be drawn, and every one of these genes has to actually
     * turn up in the wild or it is unreachable without a spawn egg.
     */
    @Test
    void founderDrawsNeverProduceACombinationThatCannotOccur() {
        Set<String> seenVariant = new HashSet<>();
        for (long seed = 0; seed < 20_000; seed++) {
            Genotype g = Genotype.random(new SeededRng(seed));
            for (Gene gene : UTILITY) {
                AllelePair pair = g.pair(gene);
                assertTrue(gene.canOccur(pair), gene.key() + " drew " + pair.toTokens());
                if (!pair.homozygousFor(gene.defaultAllele())) {
                    seenVariant.add(gene.key());
                }
            }
        }
        assertEquals(UTILITY.size(), seenVariant.size(),
                "every magical utility gene should turn up in 20 000 founders");
    }
}
