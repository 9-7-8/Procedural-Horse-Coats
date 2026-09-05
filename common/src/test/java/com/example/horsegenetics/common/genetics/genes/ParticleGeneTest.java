package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AlleleRandomness;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.genes.ParticleGene.Variant;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The particle locus - forty alleles on one gene, which is the point of it, so
 * most of what is worth pinning is that the size does not cost correctness: the
 * combination table stays total, dominance still hides a copy, codominance still
 * shows two, and the whole thing still paints nothing.
 */
class ParticleGeneTest {

    private static final ParticleGene GENE = Genes.PARTICLE;

    private static Variant v(String token) {
        for (Variant variant : GENE.variants()) {
            if (variant.allele().token().equals(token)) {
                return variant;
            }
        }
        throw new IllegalArgumentException("no variant " + token);
    }

    private static AllelePair pair(String a, String b) {
        return new AllelePair(GENE.fromToken(a), GENE.fromToken(b));
    }

    // ------------------------------------------------------------------
    // Shape
    // ------------------------------------------------------------------

    @Test
    void fortyVariantsPlusAWildType() {
        assertEquals(40, GENE.variants().size());
        assertEquals(41, GENE.alleles().size());
        assertSame(GENE.wildTypeAllele(), GENE.defaultAllele());
        assertEquals(87, GENE.expressions().size(), "1 wild + 40 single + 46 codominant double");
    }

    /**
     * The wild type must sort <b>last</b>, and the variants in rank order. Both
     * are load-bearing rather than tidy: {@code AllelePair} canonicalizes on
     * {@link Allele#order()}, so this is what makes slot 0 the copy a horse
     * shows and slot 1 the copy it hides.
     */
    @Test
    void allelesAreDeclaredInRankOrderWithTheWildTypeLast() {
        List<Allele> alleles = GENE.alleles();
        for (int i = 0; i < alleles.size(); i++) {
            assertEquals(i, alleles.get(i).order());
        }
        assertEquals(GENE.wildTypeAllele(), alleles.get(alleles.size() - 1));
        assertEquals("Dst", alleles.get(0).token(), "the most dominant allele leads");
    }

    @Test
    void everyExpressionIdIsUniqueAndEveryParticleIsDistinct() {
        Set<String> ids = new HashSet<>();
        for (Expression e : GENE.expressions()) {
            assertTrue(ids.add(e.id()), "duplicate expression id " + e.id());
        }
        Set<String> particles = new HashSet<>();
        for (Variant variant : GENE.variants()) {
            assertTrue(particles.add(variant.particle()),
                    "two alleles would look identical: " + variant.particle());
        }
    }

    /**
     * Every one of the 861 combinations lands on a declared outcome, and every
     * one of the 87 outcomes is reachable. The table is generated, so this is
     * the only thing standing between a mis-grouped allele and an outcome no
     * horse can ever have.
     */
    @Test
    void theCombinationTableIsTotalAndEveryOutcomeIsReachable() {
        Set<Expression> seen = new HashSet<>();
        int combinations = 0;
        List<Allele> alleles = GENE.alleles();
        for (int i = 0; i < alleles.size(); i++) {
            for (int j = i; j < alleles.size(); j++) {
                Expression e = GENE.expressionOf(new AllelePair(alleles.get(i), alleles.get(j)));
                assertNotNull(e);
                assertTrue(GENE.expressions().contains(e), "undeclared outcome " + e.id());
                seen.add(e);
                combinations++;
            }
        }
        assertEquals(861, combinations);
        assertEquals(GENE.expressions().size(), seen.size(), "some outcome is unreachable");
    }

    // ------------------------------------------------------------------
    // It paints nothing
    // ------------------------------------------------------------------

    @Test
    void itPaintsNothingSoTheCatalogueDoesNotGrow() {
        for (Expression e : GENE.expressions()) {
            assertTrue(e.wildType(), e.id() + " should change nothing about the coat");
        }
        assertFalse(GENE.affectsCoat());
        assertEquals(1, GenotypeCatalog.distinctPairsOf(GENE).size(),
                "forty alleles, one entry - every outcome is a wild type");
        assertFalse(Genotype.random(new SeededRng(4)).coatCode().contains(ParticleGene.KEY),
                "the locus must stay out of the texture key");
    }

    // ------------------------------------------------------------------
    // Dominance and codominance
    // ------------------------------------------------------------------

    @Test
    void aVariantOverTheWildTypeShowsItself() {
        assertEquals(List.of(v("Soul")), GENE.shown(pair("Soul", "n")));
        assertEquals(List.of(v("Soul")), GENE.shown(pair("Soul", "Soul")));
        assertEquals(List.of(), GENE.shown(pair("n", "n")));
    }

    /** The lower rank wins, and the loser is carried silently - which is what makes the locus breedable. */
    @Test
    void aNonCodominantHeterozygoteHidesTheHigherRankedCopy() {
        // Dst (rank 1) against Soul (rank 99): dust shows, the soul is carried.
        assertEquals(List.of(v("Dst")), GENE.shown(pair("Dst", "Soul")));
        assertEquals(GENE.expressionOf(pair("Dst", "n")), GENE.expressionOf(pair("Dst", "Soul")),
                "a hidden copy must be indistinguishable from the wild type");
        // ...and it really is still there to pass on.
        assertTrue(pair("Dst", "Soul").has(GENE.fromToken("Soul")));
    }

    @Test
    void twoAllelesOfOneFamilyBothShow() {
        List<Variant> shown = GENE.shown(pair("Dst", "Dst3"));
        assertEquals(List.of(v("Dst"), v("Dst3")), shown);
        assertNotEquals(GENE.expressionOf(pair("Dst", "n")), GENE.expressionOf(pair("Dst", "Dst3")));
    }

    /**
     * The flames and the smokes are <b>one</b> family of eight, not two of four -
     * the rule that any {@code -flm} stacks with any {@code -smk} as well as with
     * its own kind. Eight alleles is 28 of the locus's 46 double outcomes, so
     * getting this wrong would quietly delete most of them.
     */
    @Test
    void flamesAndSmokesAreOneFamily() {
        assertEquals(2, GENE.shown(pair("Rflm", "Bflm")).size(), "flame with flame");
        assertEquals(2, GENE.shown(pair("Smk", "Csmk")).size(), "smoke with smoke");
        assertEquals(2, GENE.shown(pair("Rflm", "Csmk")).size(), "flame with smoke");

        int burn = 0;
        for (Variant variant : GENE.variants()) {
            if (variant.group().equals("burn")) {
                burn++;
            }
        }
        assertEquals(8, burn);
    }

    @Test
    void allelesOfDifferentFamiliesNeverStack() {
        assertEquals(1, GENE.shown(pair("Rflm", "Prtl")).size(), "a flame does not stack with a portal");
        assertEquals(1, GENE.shown(pair("Lava", "Snw")).size(), "two ungrouped alleles never stack");
        assertEquals(1, GENE.shown(pair("Dst", "Dstrn")).size(),
                "enchanting glyphs are not a dust however the token reads");
        assertEquals(1, GENE.shown(pair("Clrstr", "Lmstr")).size(),
                "totem sparks are not a streak however the token reads");
    }

    /** Codominance is exactly "same non-empty group", everywhere, with no special cases. */
    @Test
    void codominanceIsExactlyTheGroupRelation() {
        for (Variant a : GENE.variants()) {
            for (Variant b : GENE.variants()) {
                if (a == b) {
                    continue;
                }
                boolean sameFamily = !a.group().isEmpty() && a.group().equals(b.group());
                int shown = GENE.shown(new AllelePair(a.allele(), b.allele())).size();
                assertEquals(sameFamily ? 2 : 1, shown,
                        a.allele().token() + "/" + b.allele().token());
            }
        }
    }

    @Test
    void theCherryHeartSoulTrioIsMutual() {
        assertEquals(2, GENE.shown(pair("Chrylf", "Hrt")).size());
        assertEquals(2, GENE.shown(pair("Hrt", "Soul")).size());
        assertEquals(2, GENE.shown(pair("Chrylf", "Soul")).size());
    }

    // ------------------------------------------------------------------
    // The founder population
    // ------------------------------------------------------------------

    /**
     * About one wild horse in thirteen trails something; a codominant double is
     * about one in ten thousand, so it is a thing you breed rather than catch.
     */
    @Test
    void theWildPopulationIsMostlyPlainAndDoublesAreEffectivelyUnobtainable() {
        int draws = 60_000;
        int emitting = 0;
        int doubles = 0;
        SeededRng rng = new SeededRng(0xF0A1);
        for (int i = 0; i < draws; i++) {
            int shown = GENE.shown(Genotype.random(rng).pair(GENE)).size();
            if (shown > 0) {
                emitting++;
            }
            if (shown == 2) {
                doubles++;
            }
        }
        double emittingShare = (double) emitting / draws;
        assertTrue(emittingShare > 0.05 && emittingShare < 0.11,
                "expected roughly 8% of founders to emit, got " + emittingShare);
        assertTrue((double) doubles / draws < 0.002, "codominant doubles should be a rarity, got " + doubles);
    }

    // ------------------------------------------------------------------
    // The epigenetics - the whole point of the locus
    // ------------------------------------------------------------------

    private static List<GeneAbility.Emitter> emittersOf(Genome genome) {
        List<GeneAbility.Emitter> out = new ArrayList<>();
        for (HorseAbilities.Active active : HorseAbilities.activeFor(genome.genotype(), genome.epigenome())) {
            if (active.geneKey().equals(ParticleGene.KEY)
                    && active.ability() instanceof GeneAbility.Emitter e) {
                out.add(e);
            }
        }
        return out;
    }

    private static Genome genomeShowing(AllelePair pair, long seed) {
        Genotype genotype = Genotype.wildType().with(pair);
        return new Genome(genotype, Epigenome.random(new SeededRng(seed)));
    }

    @Test
    void aWildTypeHorseEmitsNothing() {
        assertEquals(List.of(), emittersOf(genomeShowing(pair("n", "n"), 1)));
    }

    @Test
    void theSameHorseAlwaysProducesTheSameTrail() {
        Genome genome = genomeShowing(pair("Rflm", "n"), 99);
        assertEquals(emittersOf(genome), emittersOf(genome));

        // ...and re-parsing it off its code strings changes nothing, which is
        // what "the record is enough" means.
        Genome reparsed = Genome.parse(genome.genotypeCode(), genome.epigenomeCode());
        assertEquals(emittersOf(genome), emittersOf(reparsed));
    }

    @Test
    void twoHorsesWithTheSameAlleleNeedNotLookAlike() {
        Set<String> looks = new HashSet<>();
        for (long seed = 0; seed < 40; seed++) {
            GeneAbility.Emitter e = emittersOf(genomeShowing(pair("Rflm", "n"), seed)).get(0);
            assertEquals("minecraft:flame", e.particle(), "the allele fixes the particle and only that");
            looks.add(e.color() + "|" + e.anchor() + "|" + e.count());
        }
        assertTrue(looks.size() > 20, "expected a wide spread of colours and sites, got " + looks.size());
    }

    /**
     * Both halves of a codominant pair are drawn from their own copy, so they
     * are independent - which is what makes "red flames off the front hooves and
     * blue smoke off the tail" a horse nobody designed.
     */
    @Test
    void thetwoHalvesOfACodominantPairAreDrawnIndependently() {
        int differing = 0;
        for (long seed = 0; seed < 30; seed++) {
            List<GeneAbility.Emitter> both = emittersOf(genomeShowing(pair("Rflm", "Csmk"), seed));
            assertEquals(2, both.size());
            assertEquals("minecraft:flame", both.get(0).particle());
            assertEquals("minecraft:campfire_cosy_smoke", both.get(1).particle());
            if (both.get(0).color() != both.get(1).color()
                    || !both.get(0).anchor().equals(both.get(1).anchor())) {
                differing++;
            }
        }
        assertEquals(30, differing, "the two copies should never be forced to agree");
    }

    /** A foal that inherits the copy inherits the exact look - the reason this is worth breeding. */
    @Test
    void aFoalInheritsTheExactTrailOfTheCopyItGets() {
        Genome sire = genomeShowing(pair("Bflm", "n"), 5150);
        GeneAbility.Emitter sireTrail = emittersOf(sire).get(0);
        Genome dam = genomeShowing(pair("n", "n"), 42);

        int inherited = 0;
        for (long seed = 0; seed < 60; seed++) {
            Genome foal = dam.breedWith(sire, new SeededRng(seed));
            List<GeneAbility.Emitter> trail = emittersOf(foal);
            if (trail.isEmpty()) {
                continue; // it drew the sire's wild-type copy
            }
            inherited++;
            assertEquals(sireTrail, trail.get(0),
                    "the copy carries its colour, its site and its density with it");
        }
        assertTrue(inherited > 10, "some foals should have inherited the variant copy");
    }

    /**
     * Asked about a genotype with no epigenome, the answer is the midpoint - a
     * description of the genotype rather than of a horse nobody owns.
     */
    @Test
    void withNoEpigenomeTheAnswerIsTheStableMidpoint() {
        Genotype genotype = Genotype.wildType().with(pair("Note", "n"));
        assertEquals(HorseAbilities.activeFor(genotype), HorseAbilities.activeFor(genotype, null));

        List<HorseAbilities.Active> ours = new ArrayList<>();
        for (HorseAbilities.Active active : HorseAbilities.activeFor(genotype)) {
            if (active.geneKey().equals(ParticleGene.KEY)) {
                ours.add(active);
            }
        }
        assertEquals(1, ours.size());
        assertTrue(ours.get(0).ability() instanceof GeneAbility.Emitter);
    }

    @Test
    void everyEmitterIsWithinTheVerbsLimits() {
        for (Variant variant : GENE.variants()) {
            Genome genome = genomeShowing(new AllelePair(variant.allele(), GENE.wildTypeAllele()), 7);
            GeneAbility.Emitter e = emittersOf(genome).get(0);
            assertEquals(variant.particle(), e.particle());
            assertTrue(e.count() >= 1 && e.count() <= ParticleGene.MAX_COUNT, "count " + e.count());
            assertTrue(e.data() >= 0 && e.data() < 1, "data " + e.data());
            assertTrue(ParticleGene.SITES.contains(e.anchor()), "site " + e.anchor());
            assertTrue(e.chance() > 0 && e.chance() <= 1);
        }
    }

    // ------------------------------------------------------------------
    // Registry
    // ------------------------------------------------------------------

    @Test
    void itSitsInTheMagicalBandAndPaintsInNoPhase() {
        assertFalse(GENE.isNatural());
        assertTrue(GENE.priority() >= 100);
        assertTrue(Genes.magicalOrder().contains(GENE));
        Gene previous = null;
        for (Gene gene : Genes.codeOrder()) {
            if (gene == GENE) {
                break;
            }
            previous = gene;
        }
        assertNotNull(previous);
        assertTrue(previous.priority() <= GENE.priority());
    }
}
