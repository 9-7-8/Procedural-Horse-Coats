package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.FireproofGene;
import com.example.horsegenetics.common.genetics.genes.SpontaneousBreedingGene;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gene editors' search box ({@link EditorRules#matchesSearch}) - one rule
 * for the spawn egg screen and the browser designer both.
 */
class EditorSearchTest {

    @Test
    void aBlankQueryMatchesEveryGene() {
        for (Gene g : Genes.codeOrder()) {
            assertTrue(EditorRules.matchesSearch(g, ""));
            assertTrue(EditorRules.matchesSearch(g, "   "));
        }
    }

    @Test
    void findsByNameKeyOrAlleleCaseInsensitively() {
        Gene fireproof = Genes.byKey(FireproofGene.KEY);
        assertTrue(EditorRules.matchesSearch(fireproof, "fire"));
        assertTrue(EditorRules.matchesSearch(fireproof, "FIREPROOF"));
        assertTrue(EditorRules.matchesSearch(fireproof, "frp"), "an allele token finds its gene");

        Gene spontaneous = Genes.byKey(SpontaneousBreedingGene.KEY);
        assertTrue(EditorRules.matchesSearch(spontaneous, "spontaneous breeding"),
                "the key's underscore reads as a space");
    }

    /** Every key starts {@code horsegenetics.}; searching that must not light up the whole list. */
    @Test
    void theNamespaceIsNotSearchable() {
        long hits = Genes.codeOrder().stream()
                .filter(g -> EditorRules.matchesSearch(g, "horsegenetics"))
                .count();
        assertEquals(0, hits);
    }

    @Test
    void nonsenseMatchesNothing() {
        for (Gene g : Genes.codeOrder()) {
            assertFalse(EditorRules.matchesSearch(g, "zzqxj"));
        }
    }
}
