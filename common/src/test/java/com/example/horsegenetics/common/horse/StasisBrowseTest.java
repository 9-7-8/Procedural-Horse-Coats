package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Horse Stasis Bank's Browse tab, minus the window. What is worth pinning
 * here is the <b>gate</b>: a chamber below Intermediate, and a horse the stable
 * has no papers for, must drop out of every query and must be <i>counted</i>
 * doing it - a horse that silently vanishes from a filtered list is the one
 * failure this tab cannot have, because the player's next thought is that the
 * bank lost it.
 *
 * <p>The filtering itself is {@link HorseQuery}'s and is tested there; this only
 * checks that the bank asks it the same question the browser does.
 */
class StasisBrowseTest {

    private static Genotype chestnutMare() {
        return Genotype.wildType().withSex(Sex.FEMALE)
                .with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e));
    }

    private static Genotype bayStallion() {
        return Genotype.wildType().withSex(Sex.MALE)
                .with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.A));
    }

    private static HorseListing listing(String first, String breed, Genotype genotype, int generation) {
        return HorseListing.of(UUID.nameUUIDFromBytes(first.getBytes()), first, "Testcase", "",
                breed, generation, genotype, true, true, 40, false, false, "",
                "owner", "", generation > 0, false);
    }

    /** A bank: a mare and a stallion in Intermediates, a mare in a Basic. */
    private static List<StasisBrowseRow> bank() {
        List<StasisBrowseRow> rows = new ArrayList<>();
        rows.add(new StasisBrowseRow("Amber Testcase", StasisTier.INTERMEDIATE,
                listing("Amber", "Arabian", chestnutMare(), 3)));
        rows.add(new StasisBrowseRow("Boyd Testcase", StasisTier.SPACER,
                listing("Boyd", "Shire", bayStallion(), 1)));
        rows.add(new StasisBrowseRow("Cinder Testcase", StasisTier.BASIC,
                listing("Cinder", "Arabian", chestnutMare(), 2)));
        return rows;
    }

    private static List<String> names(List<StasisBrowseRow> rows) {
        List<String> out = new ArrayList<>();
        for (StasisBrowseRow row : rows) {
            out.add(row.displayName());
        }
        return out;
    }

    @Test
    void anEmptyQueryListsEveryChamber() {
        assertEquals(3, StasisBrowseRow.filter(bank(), "").size());
        assertEquals(3, StasisBrowseRow.filter(bank(), "   ").size());
        assertEquals(3, StasisBrowseRow.filter(bank(), null).size());
    }

    @Test
    void aQueryUsesTheBrowsersOwnLanguage() {
        assertEquals(List.of("Amber Testcase"), names(StasisBrowseRow.filter(bank(), "mare")));
        assertEquals(List.of("Boyd Testcase"), names(StasisBrowseRow.filter(bank(), "shire")));
        assertEquals(List.of("Amber Testcase"), names(StasisBrowseRow.filter(bank(), "gen>2")));
    }

    @Test
    void aBasicChamberIsNeverSearchedEvenWhenItMatches() {
        // Cinder is an Arabian mare and would match both of these on her papers;
        // the chamber she is in is what keeps her out.
        assertFalse(names(StasisBrowseRow.filter(bank(), "mare")).contains("Cinder Testcase"));
        assertFalse(names(StasisBrowseRow.filter(bank(), "arabian")).contains("Cinder Testcase"));
    }

    @Test
    void aHorseWithNoPapersIsNeverSearchedEither() {
        List<StasisBrowseRow> rows = new ArrayList<>(bank());
        rows.add(new StasisBrowseRow("Stranger", StasisTier.SPACER, null));
        assertEquals(4, StasisBrowseRow.filter(rows, "").size());
        assertFalse(names(StasisBrowseRow.filter(rows, "stranger")).contains("Stranger"));
        assertEquals(2, StasisBrowseRow.locked(rows));
    }

    @Test
    void theRowsAQueryCannotReachAreCounted() {
        assertEquals(1, StasisBrowseRow.locked(bank()));
        assertEquals(0, StasisBrowseRow.locked(List.of(
                new StasisBrowseRow("Boyd Testcase", StasisTier.SPACER,
                        listing("Boyd", "Shire", bayStallion(), 1)))));
    }

    @Test
    void aRowSaysWhatItCanAndWhyItCannot() {
        List<StasisBrowseRow> rows = bank();
        assertTrue(rows.get(0).searchable());
        assertEquals("Mare - Chestnut", rows.get(0).detail());
        assertEquals("Arabian, gen 3", rows.get(0).origin());

        StasisBrowseRow basic = rows.get(2);
        assertFalse(basic.searchable());
        assertTrue(basic.detail().contains("basic"));

        StasisBrowseRow stranger = new StasisBrowseRow("Stranger", StasisTier.SPACER, null);
        assertFalse(stranger.searchable());
        assertEquals("Stranger", stranger.displayName());
        assertTrue(stranger.detail().contains("No stable record"));
        assertEquals("", stranger.origin());
    }

    /** A half-typed term filters to nothing rather than throwing into a render loop. */
    @Test
    void nothingTypedIntoTheBoxCanThrow() {
        for (String query : List.of("gen>", "speed>abc", "-", ":", "gene:", "\"", "genotype:E/")) {
            assertTrue(StasisBrowseRow.filter(bank(), query).size() <= 3, query);
        }
    }
}
