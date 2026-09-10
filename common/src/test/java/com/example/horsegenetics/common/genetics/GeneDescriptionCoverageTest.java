package com.example.horsegenetics.common.genetics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Every registered gene can say what it is.</b>
 *
 * <p>{@link Gene#description()} is the one-to-three-sentence summary the gene
 * browser and the spawn screen's hover tooltip read. A gene with no entry
 * resolves to {@code ""}, and a caller is told to treat that as "no summary
 * available" - which is honest, and which on a hover tooltip means the gene
 * silently has nothing to say while the one above it does.
 *
 * <p>That is fine as a contract and bad as a state, so this pins it: adding a
 * gene means writing its blurb, and the failure names the genes rather than a
 * count. Built-ins get theirs from {@link GeneDescriptions}; a data-driven gene
 * carries its own {@code blurb} field.
 */
class GeneDescriptionCoverageTest {

    @Test
    void everyRegisteredGeneHasADescription() {
        List<String> missing = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            if (gene.description() == null || gene.description().isBlank()) {
                missing.add(gene.key());
            }
        }
        assertTrue(missing.isEmpty(),
                missing.size() + " of " + Genes.codeOrder().size()
                        + " genes have no description(): " + missing);
    }

    /** What the coverage actually is, so a change to it is visible in the diff. */
    @Test
    void reportsCoverage() {
        int with = 0;
        for (Gene gene : Genes.codeOrder()) {
            if (gene.description() != null && !gene.description().isBlank()) {
                with++;
            }
        }
        System.out.println("gene descriptions: " + with + " of " + Genes.codeOrder().size());
        assertTrue(with > 0);
    }
}
