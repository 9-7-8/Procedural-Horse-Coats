package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.genes.LycanGene.Form;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The LYCAN locus. Almost everything it <i>does</i> is a night-time entity swap
 * in the NeoForge module and cannot be reached from here, so what this pins is
 * the genetics that decide whether the swap happens at all - and that is the
 * whole of the gene's design: <b>two copies of the same allele, or nothing</b>.
 */
class LycanGeneTest {

    private static final LycanGene GENE = Genes.LYCAN;

    private static AllelePair pair(String a, String b) {
        return new AllelePair(GENE.fromToken(a), GENE.fromToken(b));
    }

    // ------------------------------------------------------------------
    // Shape
    // ------------------------------------------------------------------

    @Test
    void everyFormIsADistinctMobWithADistinctToken() {
        Set<String> mobs = new HashSet<>();
        Set<String> tokens = new HashSet<>();
        for (Form form : GENE.forms()) {
            assertTrue(mobs.add(form.mob()), "two alleles would shift into the same mob: " + form.mob());
            assertTrue(tokens.add(form.allele().token()), "duplicate token " + form.allele().token());
            assertTrue(form.mob().startsWith("minecraft:"), "not a vanilla mob id: " + form.mob());
        }
        assertEquals(GENE.forms().size() + 1, GENE.alleles().size(), "the forms plus one wild type");
        assertSame(GENE.wildTypeAllele(), GENE.defaultAllele());
    }

    /**
     * A horse that turns into a horse at night is not a gene. The equines are
     * the one deliberate hole in the "every non-hostile mob" rule, so it is
     * worth a test rather than a comment.
     */
    @Test
    void noFormIsAHorse() {
        for (Form form : GENE.forms()) {
            assertFalse(form.mob().contains("horse") || form.mob().endsWith("donkey")
                            || form.mob().endsWith("mule"),
                    "the horse family must not be a form: " + form.mob());
        }
    }

    @Test
    void theWildTypeSortsLastSoAPairCanBeReadOffItsSlots() {
        List<Allele> alleles = GENE.alleles();
        for (int i = 0; i < alleles.size(); i++) {
            assertEquals(i, alleles.get(i).order());
        }
        assertEquals(GENE.wildTypeAllele(), alleles.get(alleles.size() - 1));
    }

    // ------------------------------------------------------------------
    // Two copies of the same allele, or nothing
    // ------------------------------------------------------------------

    @Test
    void onlyAMatchedPairShifts() {
        assertEquals("minecraft:wolf", GENE.formOf(pair("Wlf", "Wlf")).mob());
        assertNull(GENE.formOf(pair("Wlf", "n")), "one copy is a horse");
        assertNull(GENE.formOf(pair("n", "n")));
    }

    /**
     * The outcome that makes the locus a search rather than a lottery: two
     * different shapes and the horse takes neither. It is a named outcome of its
     * own, distinct from both the carrier and the plain horse, because it is a
     * genuinely different thing to be holding.
     */
    @Test
    void twoDifferentShapesTakeNeither() {
        assertNull(GENE.formOf(pair("Wlf", "Cat")));
        assertSame(GENE.MISMATCHED, GENE.expressionOf(pair("Wlf", "Cat")));
        assertFalse(GENE.shifts(pair("Wlf", "Cat")));

        Expression carrier = GENE.expressionOf(pair("Wlf", "n"));
        Expression plain = GENE.expressionOf(pair("n", "n"));
        assertFalse(carrier.equals(plain), "a carrier is its own outcome");
        assertFalse(GENE.MISMATCHED.equals(carrier), "carrying two shapes is not carrying one");
    }

    /** Every combination lands on a declared outcome, and every outcome is reachable. */
    @Test
    void theCombinationTableIsTotalAndEveryOutcomeIsReachable() {
        Set<Expression> seen = new HashSet<>();
        List<Allele> alleles = GENE.alleles();
        for (int i = 0; i < alleles.size(); i++) {
            for (int j = i; j < alleles.size(); j++) {
                Expression e = GENE.expressionOf(new AllelePair(alleles.get(i), alleles.get(j)));
                assertNotNull(e);
                assertTrue(GENE.expressions().contains(e), "undeclared outcome " + e.id());
                seen.add(e);
            }
        }
        assertEquals(GENE.expressions().size(), seen.size(), "some outcome is unreachable");
    }

    // ------------------------------------------------------------------
    // It paints nothing and grants no ability
    // ------------------------------------------------------------------

    /**
     * Thirty-seven alleles for one gallery entry. It also grants no
     * {@code effects} verb at all - the shift is hand-written behaviour on the
     * game side, the same split {@link DhampirGene} makes - so a shifter's
     * ability list must be empty here.
     */
    @Test
    void itPaintsNothingAndGrantsNoVerb() {
        for (Expression e : GENE.expressions()) {
            assertTrue(e.wildType(), e.id() + " should change nothing about the coat");
        }
        assertFalse(GENE.affectsCoat());
        assertEquals(1, GenotypeCatalog.distinctPairsOf(GENE).size());
        assertFalse(Genotype.random(new SeededRng(11)).coatCode().contains(LycanGene.KEY),
                "the locus must stay out of the texture key");

        Genotype werewolf = Genotype.wildType().with(pair("Wlf", "Wlf"));
        for (HorseAbilities.Active active : HorseAbilities.activeFor(werewolf)) {
            assertFalse(active.geneKey().equals(LycanGene.KEY),
                    "the shift is behaviour, not an effects verb");
        }
    }

    // ------------------------------------------------------------------
    // The founder population
    // ------------------------------------------------------------------

    /**
     * Wild horses are plain or they are shifters - never carriers, never
     * mismatched. Same rule the particle locus settled, for the same reason: a
     * recessive whose carrier is invisible has to put its <i>expressing</i>
     * combination in the wild or nobody can find the allele on purpose.
     */
    @Test
    void everyFounderCombinationEitherShiftsOrIsPlain() {
        for (AllelePair p : GENE.founderTable(null).pairs()) {
            if (p.homozygousFor(GENE.wildTypeAllele())) {
                continue;
            }
            assertTrue(GENE.shifts(p), "a founder that is neither plain nor a shifter: " + p);
        }
    }

    @Test
    void aShifterIsRareAndAnyOneAnimalIsRarer() {
        int draws = 200_000;
        int shifters = 0;
        int wolves = 0;
        SeededRng rng = new SeededRng(0x1CA4);
        for (int i = 0; i < draws; i++) {
            AllelePair p = Genotype.random(rng).pair(GENE);
            Form form = GENE.formOf(p);
            if (form != null) {
                shifters++;
                if (form.mob().equals("minecraft:wolf")) {
                    wolves++;
                }
            }
        }
        double share = (double) shifters / draws;
        assertTrue(share > 0.005 && share < 0.02,
                "expected roughly 1.1% of founders to shift, got " + share);
        assertTrue(wolves > 0 && wolves < shifters / 4,
                "no one animal should dominate the shifters, got " + wolves + " of " + shifters);
    }

    // ------------------------------------------------------------------
    // The cloud colour
    // ------------------------------------------------------------------

    private static int cloudOf(AllelePair p, long seed) {
        Genotype genotype = Genotype.wildType().with(p);
        Genome genome = new Genome(genotype, Epigenome.random(new SeededRng(seed)));
        return GeneEpigenetics.forGene(GENE, genome.genotype(), genome.epigenome())
                .expressed().rgb("cloud");
    }

    /** Two were-wolves are not the same wolf: the cloud is drawn per copy. */
    @Test
    void twoShiftersOfOneShapeNeedNotTrailTheSameColour() {
        Set<Integer> colours = new HashSet<>();
        for (long seed = 0; seed < 40; seed++) {
            colours.add(cloudOf(pair("Wlf", "Wlf"), seed));
        }
        assertTrue(colours.size() > 30, "expected a wide spread of cloud colours, got " + colours.size());
    }

    /** ...and the same horse always answers the same, on the server, after a reload, here. */
    @Test
    void theSameHorseAlwaysTrailsTheSameColour() {
        assertEquals(cloudOf(pair("Cat", "Cat"), 77), cloudOf(pair("Cat", "Cat"), 77));
    }
}
