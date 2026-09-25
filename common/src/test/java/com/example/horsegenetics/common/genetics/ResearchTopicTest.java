package com.example.horsegenetics.common.genetics;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>A research paper is a gene and one allele pair, and the pair normalises.</b>
 *
 * <p>The thing worth pinning here is not the arithmetic - it is the three
 * contracts the item layer leans on and cannot check for itself:
 *
 * <ul>
 *   <li>{@code A/a} and {@code a/A} are <b>one</b> paper, or the research shelf
 *       files two rows for the same knowledge and the player cannot tell why;</li>
 *   <li>a paper written before the change (a bare gene key) still resolves - the
 *       one piece of deliberate back-compat in the repo;</li>
 *   <li>every pair a chest or a villager can hand out actually <b>crafts</b>, so
 *       no source produces a paper that makes an inert carrot.</li>
 * </ul>
 */
class ResearchTopicTest {

    private static Gene wideLocus() {
        Gene particle = Genes.byKeyOrNull("horsegenetics.particle");
        assertNotNull(particle, "the particle locus should be registered");
        return particle;
    }

    // ------------------------------------------------------------------
    // Normalisation - the reason a pair is a usable key at all
    // ------------------------------------------------------------------

    /** {@code A/a} and {@code a/A} are the same topic, the same token, and equal. */
    @Test
    void aPairEqualsItsReverse() {
        Gene gene = wideLocus();
        Allele early = gene.alleles().get(0);
        Allele late = gene.alleles().get(gene.alleles().size() - 1);

        ResearchTopic forward = new ResearchTopic(gene.key(), early.token(), late.token());
        ResearchTopic backward = new ResearchTopic(gene.key(), late.token(), early.token());

        assertEquals(forward, backward, "a pair must normalise to its reverse");
        assertEquals(forward.token(), backward.token(), "and so must the token the shelf keys on");
    }

    @Test
    void theTokenRoundTrips() {
        Gene gene = wideLocus();
        ResearchTopic topic = ResearchTopic.of(gene,
                new AllelePair(gene.alleles().get(1), gene.alleles().get(3)));
        assertEquals(topic, ResearchTopic.parse(topic.token()));
    }

    /**
     * A gene key contains dots, never a bar - so splitting the token on a bar is
     * safe. The same guarantee {@code KnownGeneSpliceTest} makes about the colon
     * the carrot token splits on, for the separator this one uses.
     */
    @Test
    void noRegisteredGeneKeyOrAlleleTokenContainsABar() {
        for (Gene gene : Genes.all()) {
            assertFalse(gene.key().contains("|"),
                    gene.key() + " contains a bar, which the research token splits on");
            for (Allele a : gene.alleles()) {
                assertFalse(a.token().contains("|"),
                        gene.key() + "'s allele " + a.token() + " contains a bar");
            }
        }
    }

    // ------------------------------------------------------------------
    // The one piece of back-compat in the repo
    // ------------------------------------------------------------------

    /**
     * <b>A paper written before this change still means something.</b> The old
     * component was a bare gene key; it now reads as the <b>homozygous, non-wild
     * pair</b> for that gene (owner's call), and the test that matters is that
     * such a paper still crafts - a migration that produced an inert carrot would
     * be worse than none.
     */
    @Test
    void anOldWholeGenePaperMigratesToACraftablePair() {
        for (Gene gene : Genes.all()) {
            if (!gene.hasGeneCarrot()) {
                continue;
            }
            ResearchTopic migrated = ResearchTopic.wholeGene(gene.key());
            assertEquals(gene.key(), migrated.geneKey());
            assertTrue(migrated.isResolved(),
                    gene.key() + ": a migrated paper must name a pair this gene allows");
            assertNotNull(migrated.splice(),
                    gene.key() + ": a migrated paper must still craft a carrot");
            assertFalse(gene.atBaseline(migrated.pair()),
                    gene.key() + ": a migrated paper must name something other than the wild type");
        }
    }

    /** A bare gene key reaching {@link ResearchTopic#parse} is the same migration. */
    @Test
    void parsingABareGeneKeyMigratesIt() {
        Gene gene = wideLocus();
        assertEquals(ResearchTopic.wholeGene(gene.key()), ResearchTopic.parse(gene.key()));
    }

    /** Homozygous where it can be - which is the half of the owner's rule worth asserting. */
    @Test
    void theMigratedPairIsHomozygousWhereverThatIsPossible() {
        int homozygous = 0;
        int total = 0;
        for (Gene gene : Genes.all()) {
            if (!gene.hasGeneCarrot()) {
                continue;
            }
            total++;
            ResearchTopic migrated = ResearchTopic.wholeGene(gene.key());
            AllelePair pair = migrated.pair();
            Allele variant = pair.first().equals(gene.defaultAllele()) ? pair.second() : pair.first();
            AllelePair doubled = new AllelePair(variant, variant);
            if (gene.canOccur(doubled) && gene.sexConsistent(doubled)) {
                assertTrue(migrated.homozygous(),
                        gene.key() + ": the homozygote is possible here, so the migration should"
                                + " have used it rather than falling back to a carrier");
                homozygous++;
            }
        }
        assertTrue(total > 0, "this test needs at least one gene with a carrot");
        assertTrue(homozygous > 0, "no gene migrated to a homozygous pair - the rule is not firing");
    }

    // ------------------------------------------------------------------
    // The pools a chest and a villager draw from
    // ------------------------------------------------------------------

    /**
     * <b>Nothing a chest hands out is inert.</b> Every pair in the pool must be
     * one the gene allows and one the carrot recipe will accept, for every
     * registered gene - including the sex-linked ones, where a pair that no horse
     * of either sex could carry is the easy mistake.
     */
    @Test
    void everyLootPairCraftsSomething() {
        for (Gene gene : Genes.all()) {
            for (ResearchTopic topic : ResearchTopic.lootPool(gene)) {
                assertTrue(topic.isResolved(), gene.key() + ": " + topic.token() + " does not resolve");
                assertNotNull(topic.splice(), gene.key() + ": " + topic.token() + " crafts nothing");
                assertTrue(gene.sexConsistent(topic.pair()),
                        gene.key() + ": " + topic.token() + " is a pair no horse could carry");
            }
        }
    }

    /** The plain wild-type pair is never a paper: an ordinary locus teaches nothing. */
    @Test
    void theLootPoolNeverOffersTheBaseline() {
        for (Gene gene : Genes.all()) {
            for (ResearchTopic topic : ResearchTopic.lootPool(gene)) {
                assertFalse(gene.atBaseline(topic.pair()),
                        gene.key() + ": " + topic.token() + " is the wild type and teaches nothing");
            }
        }
    }

    /** No duplicates, so a pool draw is a flat draw over distinct papers. */
    @Test
    void theLootPoolHasNoDuplicates() {
        for (Gene gene : Genes.all()) {
            List<ResearchTopic> pool = ResearchTopic.lootPool(gene);
            Set<String> tokens = new HashSet<>();
            for (ResearchTopic topic : pool) {
                assertTrue(tokens.add(topic.token()),
                        gene.key() + ": " + topic.token() + " is in the loot pool twice");
            }
        }
    }

    /** The true-breeding pool is the homozygous subset of the loot pool. */
    @Test
    void theBreedsTruePoolIsHomozygousAndASubset() {
        for (Gene gene : Genes.all()) {
            Set<String> loot = new HashSet<>();
            for (ResearchTopic topic : ResearchTopic.lootPool(gene)) {
                loot.add(topic.token());
            }
            for (ResearchTopic topic : ResearchTopic.breedsTruePool(gene)) {
                assertTrue(topic.homozygous(),
                        gene.key() + ": " + topic.token() + " is not homozygous");
                assertTrue(loot.contains(topic.token()),
                        gene.key() + ": " + topic.token() + " is sold but is not in the loot pool");
            }
        }
    }

    /**
     * <b>A wide locus is reachable at every allele.</b> The whole point of the
     * change: the old whole-gene paper could only ever name allele number one, so
     * forty of particle's alleles had no paper and no carrot.
     */
    @Test
    void aWideLocusHasAPaperPerAllele() {
        Gene particle = wideLocus();
        Set<String> alleles = new HashSet<>();
        for (ResearchTopic topic : ResearchTopic.lootPool(particle)) {
            alleles.add(topic.alleleA());
            alleles.add(topic.alleleB());
        }
        assertTrue(alleles.size() > 10,
                "the loot pool should reach most of a forty-allele locus, not just its first");
    }

    // ------------------------------------------------------------------
    // Nothing throws on a paper this build cannot read
    // ------------------------------------------------------------------

    /** A paper for a gene this build does not have is inert, not an exception. */
    @Test
    void aRetiredGeneIsInertRatherThanFatal() {
        ResearchTopic topic = new ResearchTopic("somebodyelse.nosuchgene", "X", "x");
        assertNull(topic.gene());
        assertNull(topic.pair());
        assertNull(topic.splice());
        assertFalse(topic.isResolved());
        assertEquals(topic, ResearchTopic.parse(topic.token()),
                "an unreadable topic must still round-trip, so the item is not silently rewritten");
        assertEquals("somebodyelse.nosuchgene: X/x", topic.label());
    }

    /** An allele the gene no longer declares is the same: inert, and never guessed at. */
    @Test
    void aRetiredAlleleIsInert() {
        ResearchTopic topic = new ResearchTopic(wideLocus().key(), "NotAnAllele", "AlsoNot");
        assertNull(topic.pair());
        assertNull(topic.splice());
        assertFalse(topic.isResolved());
    }

    /** The label and zygosity a tooltip and a shelf row read. */
    @Test
    void theLabelNamesTheGeneAndThePair() {
        Gene gene = wideLocus();
        Allele variant = gene.alleles().get(0);
        ResearchTopic homozygous = ResearchTopic.of(gene, new AllelePair(variant, variant));
        assertEquals(gene.name() + ": " + variant.token() + "/" + variant.token(), homozygous.label());
        assertEquals("homozygous", homozygous.zygosity());

        ResearchTopic carrier = ResearchTopic.of(gene, new AllelePair(variant, gene.defaultAllele()));
        assertEquals("heterozygous", carrier.zygosity());
    }

    /**
     * {@link CarrotEffect#defaultSpliceFor} is the same pair {@link
     * ResearchTopic#defaultFor} names - they are one convention, and the browser's
     * recipe ghost and a crafted carrot must not disagree about it.
     */
    @Test
    void theCarrotDefaultAndTheExamplePaperAgree() {
        for (Gene gene : Genes.all()) {
            if (!gene.hasGeneCarrot()) {
                continue;
            }
            CarrotEffect.KnownGeneSplice splice = CarrotEffect.defaultSpliceFor(gene);
            ResearchTopic topic = ResearchTopic.defaultFor(gene);
            assertEquals(topic.geneKey(), splice.geneKey(), gene.key());
            assertEquals(topic.alleleA(), splice.alleleA(), gene.key());
            assertEquals(topic.alleleB(), splice.alleleB(), gene.key());
        }
    }
}
