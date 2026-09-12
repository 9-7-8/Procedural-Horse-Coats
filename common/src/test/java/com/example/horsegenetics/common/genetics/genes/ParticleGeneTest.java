package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.epi.EpiDrift;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
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

    /**
     * <b>The locus is recessive to its own wild type.</b> One copy of {@code n}
     * and the horse trails nothing whatsoever - not a fainter version of it,
     * nothing - so a variant needs two copies to be seen at all. This is the
     * rule the founder table and half the gene's design follow from.
     */
    @Test
    void oneWildTypeCopySilencesTheWholeLocus() {
        assertEquals(List.of(v("Soul")), GENE.shown(pair("Soul", "Soul")));
        assertEquals(List.of(), GENE.shown(pair("Soul", "n")));
        assertEquals(List.of(), GENE.shown(pair("n", "n")));
        assertEquals(GENE.expressionOf(pair("n", "n")), GENE.expressionOf(pair("Soul", "n")),
                "a carrier must be indistinguishable from a plain horse");
    }

    /**
     * <b>Two variants that are not partners are two carriers.</b> The locus
     * wants agreement - the same allele twice, or two of one family - and
     * anything else is silent. It used to show the lower-ranked of the two,
     * which made a cross-family heterozygote indistinguishable from a
     * homozygote and so lied to a breeder about what a horse was carrying.
     */
    @Test
    void aCrossFamilyHeterozygoteShowsNothingAtAll() {
        // Dst (dust) against Soul (life): different families, so neither draws.
        assertEquals(List.of(), GENE.shown(pair("Dst", "Soul")));
        assertEquals(GENE.expressionOf(pair("n", "n")), GENE.expressionOf(pair("Dst", "Soul")),
                "a cross-family heterozygote must be indistinguishable from a plain horse");
        assertNotEquals(GENE.expressionOf(pair("Dst", "Dst")), GENE.expressionOf(pair("Dst", "Soul")),
                "and must NOT be mistakable for a homozygote, which was the old bug");
        // ...and both copies really are still there to pass on.
        assertTrue(pair("Dst", "Soul").has(GENE.fromToken("Soul")));
        assertTrue(pair("Dst", "Soul").has(GENE.fromToken("Dst")));
    }

    @Test
    void twoAllelesOfOneFamilyBothShow() {
        List<Variant> shown = GENE.shown(pair("Dst", "Dst3"));
        assertEquals(List.of(v("Dst"), v("Dst3")), shown);
        assertNotEquals(GENE.expressionOf(pair("Dst", "Dst")), GENE.expressionOf(pair("Dst", "Dst3")));
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

    /**
     * Different families do not stack - and, since the rank rule went, do not
     * half-stack either: neither copy shows, so the horse is plain.
     */
    @Test
    void allelesOfDifferentFamiliesShowNothing() {
        assertEquals(0, GENE.shown(pair("Rflm", "Prtl")).size(), "a flame does not stack with a portal");
        assertEquals(0, GENE.shown(pair("Lava", "Snw")).size(), "two ungrouped alleles never stack");
        assertEquals(0, GENE.shown(pair("Dst", "Dstrn")).size(),
                "enchanting glyphs are not a dust however the token reads");
        assertEquals(0, GENE.shown(pair("Clrstr", "Lmstr")).size(),
                "totem sparks are not a streak however the token reads");
    }

    /**
     * Codominance is exactly "same non-empty group", everywhere, with no special
     * cases - and it is now the <i>only</i> way two different alleles show. Two
     * partners draw two particles; anything else draws none.
     */
    @Test
    void codominanceIsExactlyTheGroupRelation() {
        for (Variant a : GENE.variants()) {
            for (Variant b : GENE.variants()) {
                if (a == b) {
                    continue;
                }
                boolean sameFamily = !a.group().isEmpty() && a.group().equals(b.group());
                int shown = GENE.shown(new AllelePair(a.allele(), b.allele())).size();
                assertEquals(sameFamily ? 2 : 0, shown,
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
     * <b>Every combination a founder can be caught in is one that shows.</b> No
     * wild horse is a silent carrier, and none is a cross-family heterozygote
     * quietly sitting on a particle nobody can see - because a locus that only
     * expresses when both copies agree makes carriers invisible, and a
     * population of invisible carriers is a locus nobody can breed on purpose.
     * The table is the whole enforcement of that, so this walks it directly
     * rather than sampling.
     */
    @Test
    void noFounderIsACarrierAndNoneIsAMismatch() {
        for (AllelePair pair : GENE.founderTable(null).pairs()) {
            if (pair.first().equals(GENE.wildTypeAllele())) {
                continue;   // the plain horse, which is most of them
            }
            assertFalse(pair.has(GENE.wildTypeAllele()),
                    "a founder must never carry one silent copy: " + pair);
            assertFalse(GENE.shown(pair).isEmpty(),
                    "a founder combination that shows nothing: " + pair);
            if (!pair.homozygous()) {
                assertEquals(2, GENE.shown(pair).size(),
                        "the only wild heterozygote is a codominant one: " + pair);
            }
        }
    }

    /**
     * About one wild horse in thirteen trails something. A codominant double is
     * rarer per combination than any single, but no longer impossible: it is a
     * <i>valid</i> pair, so the wild can hand you one.
     */
    @Test
    void theWildPopulationIsMostlyPlain() {
        int draws = 60_000;
        int emitting = 0;
        int doubles = 0;
        int carriers = 0;
        SeededRng rng = new SeededRng(0xF0A1);
        for (int i = 0; i < draws; i++) {
            AllelePair pair = Genotype.random(rng).pair(GENE);
            int shown = GENE.shown(pair).size();
            if (shown > 0) {
                emitting++;
            }
            if (shown == 2) {
                doubles++;
            }
            if (shown == 0 && !pair.homozygous()) {
                carriers++;
            }
        }
        double emittingShare = (double) emitting / draws;
        assertTrue(emittingShare > 0.05 && emittingShare < 0.11,
                "expected roughly 8% of founders to emit, got " + emittingShare);
        assertTrue((double) doubles / draws < 0.04, "doubles should stay a minority, got " + doubles);
        assertEquals(0, carriers, "no founder is a silent carrier");
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
        Genome genome = genomeShowing(pair("Rflm", "Rflm"), 99);
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
            GeneAbility.Emitter e = emittersOf(genomeShowing(pair("Rflm", "Rflm"), seed)).get(0);
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

    /**
     * A foal that inherits a copy inherits the <b>exact look</b> of it - the
     * reason the locus is worth breeding at all.
     *
     * <p>Tested on a <b>codominant</b> pair rather than on a homozygote, and
     * that is not incidental. A {@code Rflm/Rflm} horse's two copies carry two
     * independent draws and only slot 0 is on show, so its other copy's colour
     * is real, heritable and unobservable - there is nothing to assert against.
     * {@code Rflm/Csmk} puts a different particle on each copy, which makes each
     * copy individually visible, so a foal's flame can be checked against the
     * flame it must have come from.
     */
    @Test
    void aFoalInheritsTheExactTrailOfTheCopyItGets() {
        Genome sire = genomeShowing(pair("Rflm", "Csmk"), 5150);
        Genome dam = genomeShowing(pair("Rflm", "Csmk"), 42);
        GeneAbility.Emitter sireFlame = emittersOf(sire).get(0);
        GeneAbility.Emitter damFlame = emittersOf(dam).get(0);

        int inherited = 0;
        int rerolled = 0;
        for (long seed = 0; seed < 80; seed++) {
            Genome foal = dam.breedWith(sire, new SeededRng(seed));
            List<GeneAbility.Emitter> trail = emittersOf(foal);
            if (trail.size() != 2) {
                continue; // Rflm/Rflm or Csmk/Csmk - only one particle to see
            }
            inherited++;
            GeneAbility.Emitter got = trail.get(0);
            assertEquals("minecraft:flame", got.particle());
            if (!matches(sireFlame, got) && !matches(damFlame, got)) {
                // EpiDrift.REPLACE_CHANCE: a category does not nudge, it is
                // re-rolled whole, rarely. Counted rather than asserted away -
                // see the comment on the bound below.
                rerolled++;
                continue;
            }
            // Everything discrete comes through untouched: drift never nudges a
            // category a step, so a foal's particles are on the same part of its
            // body and in the same number as the parent copy it came from. The
            // colour is inherited too, but one generation of drift may have
            // moved a channel by a hair - which is the point of drift, and is
            // far too small to see.
        }
        assertTrue(inherited > 20, "some foals should have inherited one copy of each");
        // Almost all, not all. A body site is an EpiValue.Kind.CATEGORY, and
        // EpiDrift does not nudge one - it re-rolls it whole, with probability
        // REPLACE_CHANCE, precisely so that a foal that emits from somewhere new
        // is a real event and not a rounding accident at a boundary. Over this
        // many breedings that fires occasionally, and the assertion used to be
        // "never", which held only because the RNG stream happened not to reach
        // it. Adding genes elsewhere in the registry moved the stream and the
        // test failed for a change that had nothing to do with particles - so
        // the bound is now the documented behaviour rather than an accident of
        // seed choice.
        assertTrue(rerolled <= inherited / 10,
                rerolled + " of " + inherited + " foals took a fresh body site; at "
                        + EpiDrift.REPLACE_CHANCE + " per breeding that is far too many");
    }

    /** Same body site, same density, and a colour at most one generation of drift away. */
    private static boolean matches(GeneAbility.Emitter parent, GeneAbility.Emitter foal) {
        return parent.anchor().equals(foal.anchor())
                && parent.count() == foal.count()
                && colourClose(parent.color(), foal.color())
                && colourClose(parent.color2(), foal.color2());
    }

    /** Two colours one generation of drift apart - a channel or two, never a new colour. */
    private static boolean colourClose(int expected, int actual) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs(((expected >> shift) & 0xFF) - ((actual >> shift) & 0xFF)) > 4) {
                return false;
            }
        }
        return true;
    }

    /**
     * Asked about a genotype with no epigenome, the answer is the midpoint - a
     * description of the genotype rather than of a horse nobody owns.
     */
    @Test
    void withNoEpigenomeTheAnswerIsTheStableMidpoint() {
        Genotype genotype = Genotype.wildType().with(pair("Note", "Note"));
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
            Genome genome = genomeShowing(new AllelePair(variant.allele(), variant.allele()), 7);
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
