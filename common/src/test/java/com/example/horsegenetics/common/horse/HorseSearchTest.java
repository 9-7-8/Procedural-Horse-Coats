package com.example.horsegenetics.common.horse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.ArcaneStock;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genome;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/** The filter box over a dealer's string (owner, 2026-09-18). */
class HorseSearchTest {

    /** One arcane dealer's horse, and the pairs it was built with. */
    private record Subject(HorseRecord record, List<AllelePair> forced) {
    }

    private static Subject subject() {
        SeededRng rng = new SeededRng(20260918L, "search");
        List<AllelePair> forced = ArcaneStock.rollHorse(rng, new LinkedHashSet<>());
        Genome genome = BreedFounder.roll(Breeds.FERAL_MIXED, rng, forced);
        HorseRecord record = HorseRecord.founder(
                UUID.nameUUIDFromBytes("search".getBytes()), "Nim", "Blackthorn",
                genome, BreedLineage.MIXED.toToken());
        return new Subject(record, forced);
    }

    @Test
    void aBlankQueryMatchesEverything() {
        HorseRecord record = subject().record();
        assertTrue(HorseSearch.matches(record, ""));
        assertTrue(HorseSearch.matches(record, "   "));
        assertTrue(HorseSearch.matches(record, null));
    }

    @Test
    void everyGeneItShowsIsFindableByNameKeyAndAllele() {
        Subject subject = subject();
        for (AllelePair pair : subject.forced()) {
            Gene gene = pair.gene();
            assertTrue(HorseSearch.matches(subject.record(), gene.name()),
                    "not findable by gene name: " + gene.name());
            assertTrue(HorseSearch.matches(subject.record(), gene.key()),
                    "not findable by gene key: " + gene.key());
            assertTrue(HorseSearch.matches(subject.record(), pair.first().token()),
                    "not findable by allele token: " + ArcaneStock.token(pair));
        }
    }

    @Test
    void theBreedLabelIsSearchable() {
        assertTrue(HorseSearch.matches(subject().record(), "mixed"));
        assertTrue(HorseSearch.matches(subject().record(), "MIXED"), "matching is case-insensitive");
        assertFalse(HorseSearch.matches(subject().record(), "friesian"));
    }

    @Test
    void theHorsesOwnNameIsSearchable() {
        assertTrue(HorseSearch.matches(subject().record(), "blackthorn"));
        assertTrue(HorseSearch.matches(subject().record(), "nim"));
    }

    @Test
    void severalWordsMeanAllOfThem() {
        Subject subject = subject();
        Gene first = subject.forced().get(0).gene();
        assertTrue(HorseSearch.matches(subject.record(), first.name() + " mixed"),
                "both terms are true of this horse");
        assertFalse(HorseSearch.matches(subject.record(), first.name() + " friesian"),
                "one false term must reject - a search box is an AND, not a union");
    }

    @Test
    void nonsenseMatchesNothing() {
        assertFalse(HorseSearch.matches(subject().record(), "zzzznotagene"));
    }

    /**
     * The point of the baseline rule: every horse carries every locus, so a
     * gene the horse is wild type for must not answer for it - otherwise a
     * search returns the whole string and has told the player nothing.
     */
    @Test
    void aGeneTheHorseIsWildTypeForDoesNotMatch() {
        Subject subject = subject();
        int checked = 0;
        for (Gene gene : com.example.horsegenetics.common.genetics.Genes.magicalOrder()) {
            boolean carried = subject.forced().stream().anyMatch(p -> p.gene() == gene);
            if (carried) {
                continue;
            }
            AllelePair pair = subject.record().genome().genotype().pair(gene);
            if (pair == null || !pair.homozygousFor(gene.defaultAllele())) {
                continue; // a stray non-wild copy off the base roll; not what this asserts
            }
            // Matching is by substring, so a wild-type gene whose key is a prefix
            // of a carried one is found through its neighbour and is not a
            // failure: "horsegenetics.contour" lives inside
            // "horsegenetics.contour_cells". That is ordinary search-box
            // behaviour, and the horse really does carry something called that.
            if (subject.forced().stream().anyMatch(p -> p.gene().key().contains(gene.key())
                    || p.gene().name().toLowerCase().contains(gene.name().toLowerCase()))) {
                continue;
            }
            assertFalse(HorseSearch.matches(subject.record(), gene.key()),
                    gene.key() + " is wild type here and must not match");
            checked++;
            if (checked >= 20) {
                return;
            }
        }
    }
}
