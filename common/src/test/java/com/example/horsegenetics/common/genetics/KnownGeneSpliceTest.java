package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The Known Gene Splice carrot names two alleles, and the token survives a
 * round trip.</b>
 *
 * <p>This exists because the token used to be built by string concatenation in
 * two places as well as by {@code CarrotEffect}, and when it gained the allele
 * names <b>both of those still compiled</b> and silently stopped parsing - a
 * carrot that looked right in the inventory and did nothing when fed. A format
 * only one place writes and one place reads is worth pinning.
 */
class KnownGeneSpliceTest {

    @Test
    void theTokenRoundTrips() {
        CarrotEffect.KnownGeneSplice effect =
                new CarrotEffect.KnownGeneSplice("horsegenetics.particle", "Rflm", "Bflm");
        assertEquals("known:horsegenetics.particle:Rflm:Bflm", effect.id());

        Optional<CarrotEffect> back = CarrotEffect.parse(effect.id());
        assertTrue(back.isPresent(), "the token this effect writes must parse back");
        assertEquals(effect, back.get());
    }

    /** A gene key contains a dot, never a colon, so splitting on colon is safe. */
    @Test
    void everyRegisteredGeneKeyIsColonFree() {
        for (Gene gene : Genes.all()) {
            assertTrue(gene.key().indexOf(':') < 0,
                    gene.key() + " contains a colon, which the carrot token splits on");
        }
    }

    /**
     * <b>The point of the change.</b> A wide locus can now be spliced to any of
     * its alleles, not just the first-declared one - the particle locus has
     * forty, and the old shape could only ever hand over the first.
     */
    @Test
    void aWideLocusCanBeSplicedToAnyAllele() {
        Gene particle = Genes.byKeyOrNull("horsegenetics.particle");
        assertNotNull(particle, "the particle locus should be registered");
        assertTrue(particle.alleles().size() > 10, "this test is about a wide locus");

        // An allele that is emphatically not alleles().get(0).
        Allele late = particle.alleles().get(particle.alleles().size() - 2);
        CarrotEffect effect =
                new CarrotEffect.KnownGeneSplice(particle.key(), late.token(), late.token());

        GameteBias bias = CarrotEffect.fold(List.of(effect), Genotype.wildType(), new SeededRng(1));
        AllelePair substituted = bias.pairFor(particle.key(), null);
        assertNotNull(substituted, "the carrot should substitute a pair for this gene");
        assertEquals(2, substituted.count(late), "both copies should be the named allele");
    }

    /** An allele this gene does not have is dropped, never guessed at. */
    @Test
    void anUnknownAlleleSubstitutesNothing() {
        Gene particle = Genes.byKeyOrNull("horsegenetics.particle");
        assertNotNull(particle);
        CarrotEffect effect = new CarrotEffect.KnownGeneSplice(
                particle.key(), "NotAnAllele", "AlsoNot");

        GameteBias bias = CarrotEffect.fold(List.of(effect), Genotype.wildType(), new SeededRng(1));
        assertTrue(bias.isNone(), "a carrot naming alleles this gene has never had should do nothing");
    }

    /** What a carrot crafted from a whole-gene research paper still means. */
    @Test
    void theDefaultForAGeneNamesRealAlleles() {
        for (Gene gene : Genes.all()) {
            if (!gene.hasGeneCarrot()) {
                continue;
            }
            CarrotEffect.KnownGeneSplice d = CarrotEffect.defaultSpliceFor(gene);
            assertEquals(gene.key(), d.geneKey());
            assertTrue(gene.alleles().stream().anyMatch(a -> a.token().equals(d.alleleA())),
                    gene.key() + " default allele A must exist");
            assertTrue(gene.alleles().stream().anyMatch(a -> a.token().equals(d.alleleB())),
                    gene.key() + " default allele B must exist");
            assertTrue(CarrotEffect.parse(d.id()).isPresent(),
                    gene.key() + " default token must parse back");
        }
    }
}
