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
 * The browser's <i>My horses</i> filter language. The tab redraws every frame
 * over whatever the player typed, so the two things worth pinning here are that
 * every documented term means what the help line says, and that <b>no</b> input
 * throws - a half-typed {@code speed>} has to filter to nothing, not to a crash
 * in the render loop.
 */
class HorseQueryTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static Genotype chestnut() {
        return Genotype.wildType().withSex(Sex.FEMALE)
                .with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e));
    }

    private static Genotype bay() {
        return Genotype.wildType().withSex(Sex.MALE)
                .with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.A));
    }

    private static HorseListing row(String first, String breed, Genotype genotype,
                                    boolean adult, int generation, int bond) {
        return HorseListing.of(ID, first, "Testcase", "", breed, generation, genotype,
                adult, true, bond, false, true, "overworld", "owner", "", generation > 0);
    }

    private static List<HorseListing> stable() {
        List<HorseListing> rows = new ArrayList<>();
        rows.add(row("Amber", "Arabian", chestnut(), true, 3, 40));
        rows.add(row("Boyd", "Shire", bay(), true, 1, 90));
        rows.add(row("Cinder", "Arabian", bay(), false, 4, 10));
        return rows;
    }

    private static List<String> names(List<HorseListing> rows) {
        List<String> out = new ArrayList<>();
        for (HorseListing row : rows) {
            out.add(row.firstName());
        }
        return out;
    }

    @Test
    void anEmptyQueryKeepsEverything() {
        assertEquals(3, HorseQuery.filter(stable(), "").size());
        assertEquals(3, HorseQuery.filter(stable(), "   ").size());
    }

    @Test
    void bareWordsSearchNamesBreedsAndCoats() {
        assertEquals(List.of("Amber"), names(HorseQuery.filter(stable(), "amber")));
        assertEquals(List.of("Amber", "Cinder"), names(HorseQuery.filter(stable(), "arabian")));
        // The coat description is part of the haystack, and Amber is the red one.
        assertEquals(List.of("Amber"), names(HorseQuery.filter(stable(), "chestnut")));
    }

    @Test
    void flagsReadSexAndAgeTogether() {
        assertEquals(List.of("Amber"), names(HorseQuery.filter(stable(), "mare")));
        assertEquals(List.of("Boyd"), names(HorseQuery.filter(stable(), "stallion")));
        assertEquals(List.of("Cinder"), names(HorseQuery.filter(stable(), "foal")));
        assertEquals(List.of("Cinder"), names(HorseQuery.filter(stable(), "colt")));
        assertEquals(List.of("Amber", "Boyd"), names(HorseQuery.filter(stable(), "adult")));
    }

    @Test
    void negationDropsMatchingRows() {
        assertEquals(List.of("Boyd", "Cinder"), names(HorseQuery.filter(stable(), "-mare")));
        assertEquals(List.of("Boyd"), names(HorseQuery.filter(stable(), "-arabian")));
    }

    @Test
    void termsAreAndedTogether() {
        assertEquals(List.of("Cinder"), names(HorseQuery.filter(stable(), "arabian foal")));
        assertTrue(HorseQuery.filter(stable(), "arabian shire").isEmpty());
    }

    @Test
    void numericKeysCompare() {
        assertEquals(List.of("Amber", "Cinder"), names(HorseQuery.filter(stable(), "gen>2")));
        assertEquals(List.of("Amber", "Cinder"), names(HorseQuery.filter(stable(), "gen>=3")));
        assertEquals(List.of("Boyd"), names(HorseQuery.filter(stable(), "gen<2")));
        assertEquals(List.of("Amber"), names(HorseQuery.filter(stable(), "gen:3")));
        assertEquals(List.of("Boyd"), names(HorseQuery.filter(stable(), "bond>50")));
    }

    @Test
    void bondFiltersSkipHorsesWhoseBondIsNotKnown() {
        List<HorseListing> rows = List.of(
                row("Unloaded", "Arabian", bay(), true, 1, HorseListing.BOND_UNKNOWN));
        assertTrue(HorseQuery.filter(rows, "bond>0").isEmpty());
        assertTrue(HorseQuery.filter(rows, "bond<50").isEmpty());
    }

    @Test
    void textKeysMatchSubstrings() {
        assertEquals(List.of("Amber"), names(HorseQuery.filter(stable(), "name:amb")));
        assertEquals(List.of("Boyd"), names(HorseQuery.filter(stable(), "breed:shir")));
        assertEquals(List.of("Amber"), names(HorseQuery.filter(stable(), "coat:chest")));
        assertEquals(3, HorseQuery.filter(stable(), "by:owner").size());
    }

    @Test
    void carriesFindsAnAlleleTokenWhetherItShowsOrNot() {
        // A het chestnut carrier shows nothing, and is exactly what a breeder asks for.
        HorseListing carrier = row("Carrier", "Arabian",
                Genotype.wildType().with(new AllelePair(Genes.EXTENSION.E, Genes.EXTENSION.e)),
                true, 1, 0);
        assertTrue(HorseQuery.matches(carrier, HorseQuery.terms("carries:e")));
        assertFalse(HorseQuery.matches(carrier, HorseQuery.terms("expresses:chestnut")));

        HorseListing red = row("Red", "Arabian", chestnut(), true, 1, 0);
        assertTrue(HorseQuery.matches(red, HorseQuery.terms("carries:e")));
        assertTrue(HorseQuery.matches(red, HorseQuery.terms("expresses:chestnut")));
    }

    @Test
    void geneKeyAndGeneNameBothWork() {
        HorseListing carrier = row("Carrier", "Arabian",
                Genotype.wildType().with(new AllelePair(Genes.EXTENSION.E, Genes.EXTENSION.e)),
                true, 1, 0);
        assertTrue(HorseQuery.matches(carrier, HorseQuery.terms("gene:extension")));
        HorseListing plain = row("Plain", "Arabian", Genotype.wildType(), true, 1, 0);
        assertFalse(HorseQuery.matches(plain, HorseQuery.terms("gene:extension")));
    }

    /**
     * A stallion's X-linked pair reads {@code n/Y}, and the reserved {@code Y}
     * is the slot he does not have rather than something he is carrying. Get
     * this wrong and {@code gene:brindle} matches every stallion alive.
     */
    @Test
    void aStallionsReservedSlotIsNotAnAlleleHeCarries() {
        // n/Y: a plain stallion. He is homozygous for nothing, so the old
        // `homozygousFor(defaultAllele())` test called him a carrier and put a
        // brindle row on every stallion's gene page.
        AllelePair plain = new AllelePair(Genes.BRINDLE.n, Genes.BRINDLE.Y);
        assertTrue(Genes.BRINDLE.atBaseline(plain));
        HorseListing stallion = row("Stallion", "Arabian",
                Genotype.wildType().withSex(Sex.MALE).with(plain), true, 1, 0);
        assertFalse(HorseQuery.matches(stallion, HorseQuery.terms("gene:brindle")));

        AllelePair carries = new AllelePair(Genes.BRINDLE.Brn, Genes.BRINDLE.Y);
        assertFalse(Genes.BRINDLE.atBaseline(carries));
        HorseListing brindle = row("Brindle", "Arabian",
                Genotype.wildType().withSex(Sex.MALE).with(carries), true, 1, 0);
        assertTrue(HorseQuery.matches(brindle, HorseQuery.terms("gene:brindle")));
    }

    @Test
    void malformedTermsMatchNothingAndDoNotThrow() {
        for (String query : List.of("speed>", "gen>abc", "bond<", ":", ">", "-", "gene:", "expresses:")) {
            assertTrue(HorseQuery.filter(stable(), query).isEmpty(), query);
        }
    }

    @Test
    void sortingIsStableOnTheDisplayName() {
        List<HorseListing> byGen = HorseQuery.apply(stable(), "", HorseQuery.Sort.GENERATION, false);
        assertEquals(List.of("Boyd", "Amber", "Cinder"), names(byGen));
        List<HorseListing> down = HorseQuery.apply(stable(), "", HorseQuery.Sort.GENERATION, true);
        assertEquals(List.of("Cinder", "Amber", "Boyd"), names(down));
    }

    @Test
    void everySortColumnOrdersWithoutThrowing() {
        for (HorseQuery.Sort sort : HorseQuery.Sort.values()) {
            assertEquals(3, HorseQuery.apply(stable(), "", sort, false).size(), sort.name());
            assertEquals(3, HorseQuery.apply(stable(), "", sort, true).size(), sort.name());
        }
    }
}
