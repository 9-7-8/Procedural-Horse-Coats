package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.AbilityType;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one aggression locus, its skittish mirror, and the passification locus
 * that lets you get near the result.
 */
class AggressionGenesTest {

    private static final int GRID = AggressionGene.TIMES * AggressionGene.TARGETS;

    @Test
    void theGridIsEveryTimeCrossedWithEveryTarget() {
        // +1 for the wild type.
        assertEquals(GRID + 1, Genes.AGGRESSION.alleles().size());
        assertEquals(GRID + 1, Genes.SKITTISH.alleles().size());
    }

    @Test
    void aMatchedPairHuntsAndAMismatchedOneDoesNothing() {
        List<Allele> alleles = Genes.AGGRESSION.alleles();
        Allele first = alleles.get(0);
        Allele other = alleles.get(1);

        List<GeneAbility> matched =
                Genes.AGGRESSION.abilitiesFor(new AllelePair(first, first), Genotype.wildType());
        assertEquals(1, matched.size());
        GeneAbility.Temper t = assertInstanceOf(GeneAbility.Temper.class, matched.get(0));
        assertEquals("aggressive", t.mood());
        assertTrue(t.hold(), "it holds its ground rather than chasing - a settled call");

        // Recessive to the wild type AND to each other.
        assertTrue(Genes.AGGRESSION.abilitiesFor(
                new AllelePair(first, Genes.AGGRESSION.defaultAllele()), Genotype.wildType()).isEmpty());
        assertTrue(Genes.AGGRESSION.abilitiesFor(
                new AllelePair(first, other), Genotype.wildType()).isEmpty(),
                "two different aggression alleles settle on neither");
    }

    @Test
    void skittishnessIsTheSameGridWithTheMoodFlipped() {
        Allele shy = Genes.SKITTISH.alleles().get(0);
        List<GeneAbility> a =
                Genes.SKITTISH.abilitiesFor(new AllelePair(shy, shy), Genotype.wildType());
        assertEquals(1, a.size());
        assertEquals("flee", assertInstanceOf(GeneAbility.Temper.class, a.get(0)).mood());
    }

    /**
     * The owner's call, and the one most likely to be undone by accident: a
     * saddle is not a gate. The gladiator locus this absorbed stopped dead while
     * ridden, so anybody porting its behaviour back would reintroduce the term.
     */
    @Test
    void aggressionIsNotGatedOnAnEmptySaddle() {
        for (Allele a : Genes.AGGRESSION.alleles()) {
            for (GeneAbility ability : Genes.AGGRESSION.abilitiesFor(
                    new AllelePair(a, a), Genotype.wildType())) {
                assertFalse(mentionsRider(ability.when()),
                        a.token() + " must not gate on has_rider - it bucks instead");
            }
        }
    }

    /**
     * {@code instanceof} rather than a pattern switch: {@code common/} compiles
     * at a release where switch patterns are still a preview feature, and it has
     * three targets to keep buildable (hard rule 2).
     */
    private static boolean mentionsRider(GeneAbility.Condition c) {
        if (c instanceof GeneAbility.Condition.Flag f) {
            return "has_rider".equals(f.name());
        }
        if (c instanceof GeneAbility.Condition.All all) {
            return all.terms().stream().anyMatch(AggressionGenesTest::mentionsRider);
        }
        if (c instanceof GeneAbility.Condition.Any any) {
            return any.terms().stream().anyMatch(AggressionGenesTest::mentionsRider);
        }
        if (c instanceof GeneAbility.Condition.Not not) {
            return mentionsRider(not.term());
        }
        return false;
    }

    @Test
    void horsesAreAGroupAVerbCanName() {
        assertTrue(AbilityType.MOB_GROUPS.contains("horses"),
                "the aggression locus names its own kind");
    }

    /** The two time-of-day verbs were folded into 'temper' with a 'when'. */
    @Test
    void theTimeOfDayVerbsAreGone() {
        assertNotNull(AbilityType.byName("temper"));
        for (String gone : List.of("night_temper", "day_temper")) {
            assertThrows(gone);
        }
    }

    private static void assertThrows(String verb) {
        try {
            AbilityType.byName(verb);
            org.junit.jupiter.api.Assertions.fail(verb + " should no longer be a registered verb");
        } catch (IllegalArgumentException expected) {
            // what we want
        }
    }

    // ------------------------------------------------------------------
    // Passification
    // ------------------------------------------------------------------

    @Test
    void oneCopyOpensAWayInAndTwoDifferentOnesOpenTwo() {
        PassificationGene gene = Genes.PASSIFICATION;
        Allele n = gene.defaultAllele();
        Allele prm = gene.alleles().get(0);
        Allele tmp = gene.alleles().get(1);
        Genotype wild = Genotype.wildType();

        assertTrue(gene.routesOf(new AllelePair(n, n), epi(wild)).isEmpty(),
                "a horse with no passification allele offers nothing");

        // One copy is enough - incomplete dominant, the owner's call.
        assertEquals(1, gene.routesOf(new AllelePair(prm, n), epi(wild)).size());
        // A matched pair is one route, not the same route twice.
        assertEquals(1, gene.routesOf(new AllelePair(prm, prm), epi(wild)).size());
        // And two different alleles are genuinely two ways in.
        List<PassificationGene.Route> both = gene.routesOf(new AllelePair(prm, tmp), epi(wild));
        assertEquals(2, both.size(), "a compound heterozygote offers both routes");
        assertTrue(both.stream().anyMatch(r -> r.kind() == PassificationGene.Kind.PERMANENT));
        assertTrue(both.stream().anyMatch(r -> r.kind() == PassificationGene.Kind.TEMPORARY));
    }

    @Test
    void everyRouteAsksForSomethingThatExists() {
        PassificationGene gene = Genes.PASSIFICATION;
        Genotype wild = Genotype.wildType();
        for (Allele a : gene.alleles()) {
            for (PassificationGene.Route r : gene.routesOf(new AllelePair(a, a), epi(wild))) {
                assertTrue(PassificationGene.OFFERINGS.contains(r.item()),
                        a.token() + " asks for an item off the table");
                assertTrue(r.amount() >= 1, a.token() + " asks for at least one of it");
            }
        }
    }

    private static GeneEpigenetics epi(Genotype genotype) {
        return GeneEpigenetics.forGene(Genes.PASSIFICATION, genotype, null);
    }
}
