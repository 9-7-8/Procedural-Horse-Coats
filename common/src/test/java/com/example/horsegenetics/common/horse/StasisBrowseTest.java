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
        rows.add(new StasisBrowseRow(0, "Amber Testcase", StasisTier.INTERMEDIATE,
                listing("Amber", "Arabian", chestnutMare(), 3), false));
        rows.add(new StasisBrowseRow(1, "Boyd Testcase", StasisTier.SPACER,
                listing("Boyd", "Shire", bayStallion(), 1), true));
        rows.add(new StasisBrowseRow(2, "Cinder Testcase", StasisTier.BASIC,
                listing("Cinder", "Arabian", chestnutMare(), 2), false));
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
    void aBasicChamberIsNeverSearchedByItsPapers() {
        // Cinder is an Arabian mare and would match both of these on her papers;
        // the chamber she is in is what keeps her out.
        assertFalse(names(StasisBrowseRow.filter(bank(), "mare")).contains("Cinder Testcase"));
        assertFalse(names(StasisBrowseRow.filter(bank(), "arabian")).contains("Cinder Testcase"));
    }

    /**
     * <b>A Basic chamber answers to its name, and to nothing else.</b> The name
     * is on the row and on the bottle; a query that is only that name finds her,
     * and one that adds anything the bank would have to look inside to answer -
     * a flag, a key, a comparison - leaves her out exactly as before, name and all.
     */
    @Test
    void aBasicChamberIsFoundByItsNameAndOnlyItsName() {
        assertEquals(List.of("Cinder Testcase"), names(StasisBrowseRow.filter(bank(), "cinder")));
        assertEquals(List.of("Cinder Testcase"), names(StasisBrowseRow.filter(bank(), "CIND test")));
        assertEquals(0, StasisBrowseRow.locked(bank(), "cinder"));

        for (String mixed : List.of("cinder mare", "cinder gen>2", "name:cinder", "name = cinder",
                "cinder OR amber", "NOT amber", "'cinder'", "(cinder)", "cinder -lethal")) {
            assertFalse(names(StasisBrowseRow.filter(bank(), mixed)).contains("Cinder Testcase"), mixed);
            assertEquals(1, StasisBrowseRow.locked(bank(), mixed), mixed);
        }
    }

    @Test
    void aNegatedNameLeavesABasicChamberOut() {
        assertEquals(List.of("Amber Testcase", "Boyd Testcase"),
                names(StasisBrowseRow.filter(bank(), "-cinder")));
        assertEquals(List.of("Boyd Testcase"),
                names(StasisBrowseRow.filter(bank(), "test -cinder -amber")));
        assertEquals(0, StasisBrowseRow.locked(bank(), "-cinder"));
    }

    /** The name rule is Basic's alone: a searchable row still answers the whole language. */
    @Test
    void aSearchableChamberIsUnchangedByTheNameRule() {
        assertEquals(List.of("Amber Testcase"), names(StasisBrowseRow.filter(bank(), "amber")));
        assertEquals(List.of("Amber Testcase"), names(StasisBrowseRow.filter(bank(), "amber mare")));
        // A plain word reads an Intermediate's whole papers, not just its name.
        assertEquals(List.of("Amber Testcase"), names(StasisBrowseRow.filter(bank(), "arabian")));
    }

    /** A no-papers horse in a searchable chamber is not a Basic one: its name does not reach it. */
    @Test
    void theNameRuleDoesNotReachAHorseWithNoPapers() {
        List<StasisBrowseRow> rows = new ArrayList<>(bank());
        rows.add(new StasisBrowseRow(3, "Stranger", StasisTier.SPACER, null, false));
        rows.add(new StasisBrowseRow(4, "Drifter", StasisTier.BASIC, null, false));
        assertEquals(List.of("Drifter"), names(StasisBrowseRow.filter(rows, "drifter")));
        assertEquals(List.of(), names(StasisBrowseRow.filter(rows, "stranger")));
        assertEquals(1, StasisBrowseRow.locked(rows, "stranger"));
        assertEquals(3, StasisBrowseRow.locked(rows, "stranger mare"));
    }

    @Test
    void aHorseWithNoPapersIsNeverSearchedEither() {
        List<StasisBrowseRow> rows = new ArrayList<>(bank());
        rows.add(new StasisBrowseRow(3, "Stranger", StasisTier.SPACER, null, false));
        assertEquals(4, StasisBrowseRow.filter(rows, "").size());
        assertFalse(names(StasisBrowseRow.filter(rows, "stranger")).contains("Stranger"));
        assertEquals(2, StasisBrowseRow.locked(rows));
    }

    @Test
    void theRowsAQueryCannotReachAreCounted() {
        assertEquals(1, StasisBrowseRow.locked(bank()));
        assertEquals(0, StasisBrowseRow.locked(List.of(
                new StasisBrowseRow(1, "Boyd Testcase", StasisTier.SPACER,
                        listing("Boyd", "Shire", bayStallion(), 1), false))));
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

        StasisBrowseRow stranger = new StasisBrowseRow(3, "Stranger", StasisTier.SPACER, null, false);
        assertFalse(stranger.searchable());
        assertEquals("Stranger", stranger.displayName());
        assertTrue(stranger.detail().contains("No stable record"));
        assertEquals("", stranger.origin());
    }

    /**
     * <b>Only the top rung may be put to stud</b>, and a mark on anything else
     * does not count. The mark rides on the chamber item, so a player who
     * marked a Spacer, downgraded nothing and swapped the horse into an
     * Intermediate must not find the bank still breeding it.
     */
    @Test
    void onlyASpacerCountsAsAtStud() {
        List<StasisBrowseRow> rows = bank();
        assertTrue(rows.get(1).mayStud());
        assertFalse(rows.get(0).mayStud());
        assertFalse(rows.get(2).mayStud());
        assertEquals(1, StasisBrowseRow.atStud(rows));

        // The same mark, on a tier that does not buy it: not at stud.
        assertEquals(0, StasisBrowseRow.atStud(List.of(
                new StasisBrowseRow(0, "Amber Testcase", StasisTier.INTERMEDIATE,
                        listing("Amber", "Arabian", chestnutMare(), 3), true))));
    }

    /** A half-typed term filters to nothing rather than throwing into a render loop. */
    @Test
    void nothingTypedIntoTheBoxCanThrow() {
        for (String query : List.of("gen>", "speed>abc", "-", ":", "gene:", "\"", "genotype:E/")) {
            assertTrue(StasisBrowseRow.filter(bank(), query).size() <= 3, query);
        }
    }
}
