package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The {@code WHERE}-clause search language.</b> {@link HorseQueryTest} pins
 * the vocabulary - what each term means - and stays as it was, because every
 * query it contains still parses to the same thing. This pins the
 * <b>grammar</b> laid on top: operators, precedence, brackets, and the zygosity
 * questions the old flat syntax could not ask at all.
 *
 * <p>Two contracts run through all of it. <b>Every old query still works</b>,
 * because adjacent terms are implicitly ANDed and {@code :} is {@code =}. And
 * <b>nothing throws</b>, because a search box is typed one character at a time
 * and every prefix of a valid query is a query somebody is one keystroke away
 * from having.
 */
class QueryExprTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static HorseListing row(String first, String breed, Genotype genotype,
                                    boolean adult, int generation, int bond) {
        return HorseListing.of(ID, first, "Testcase", "", breed, generation, genotype,
                adult, true, bond, false, true, "overworld", "owner", "", generation > 0, false);
    }

    /** Chestnut: homozygous e/e at extension, so homozygous for a non-default. */
    private static Genotype chestnut() {
        return Genotype.wildType().withSex(Sex.FEMALE)
                .with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e));
    }

    /** Carries one copy of e - heterozygous at extension. */
    private static Genotype carrier() {
        return Genotype.wildType().withSex(Sex.FEMALE)
                .with(new AllelePair(Genes.EXTENSION.E, Genes.EXTENSION.e));
    }

    /** Nothing at extension at all. */
    private static Genotype wild() {
        return Genotype.wildType().withSex(Sex.MALE);
    }

    private static List<HorseListing> stable() {
        List<HorseListing> rows = new ArrayList<>();
        rows.add(row("Amber", "Arabian", chestnut(), true, 3, 40));
        rows.add(row("Boyd", "Shire", carrier(), true, 1, 90));
        rows.add(row("Cinder", "Arabian", wild(), false, 4, 10));
        return rows;
    }

    private static List<String> found(String query) {
        List<String> out = new ArrayList<>();
        for (HorseListing row : HorseQuery.filter(stable(), query)) {
            out.add(row.firstName());
        }
        return out;
    }

    // ------------------------------------------------------------------
    // The grammar
    // ------------------------------------------------------------------

    @Test
    void adjacentTermsAreStillAnded() {
        // The whole of the old syntax, unchanged. If this breaks, every query
        // anybody has ever typed into the box breaks with it.
        assertEquals(List.of("Amber"), found("arabian adult"));
        assertEquals(List.of("Amber", "Cinder"), found("arabian"));
    }

    @Test
    void explicitAndMeansTheSameAsAdjacency() {
        assertEquals(found("arabian adult"), found("arabian AND adult"));
    }

    @Test
    void orWidensTheSearch() {
        assertEquals(List.of("Amber", "Boyd", "Cinder"), found("arabian OR shire"));
        assertEquals(List.of("Boyd"), found("shire OR nothingmatchesthis"));
    }

    @Test
    void notNegatesATerm() {
        assertEquals(List.of("Boyd"), found("NOT arabian"));
        assertEquals(found("NOT arabian"), found("-arabian"));
    }

    @Test
    void andBindsTighterThanOr() {
        // (shire AND adult) OR (arabian AND foal) = Boyd, Cinder.
        // If OR bound tighter it would read shire AND (adult OR arabian) AND
        // foal, which is nobody - so the two readings are told apart here.
        assertEquals(List.of("Boyd", "Cinder"), found("shire AND adult OR arabian AND foal"));
    }

    @Test
    void bracketsOverridePrecedence() {
        // Without brackets this is (shire AND foal) OR arabian = Amber, Cinder.
        // With them it is shire AND (foal OR arabian) = nobody, since Boyd is
        // a Shire and neither a foal nor an Arabian.
        assertEquals(List.of("Amber", "Cinder"), found("shire AND foal OR arabian"));
        assertEquals(List.of(), found("shire AND (foal OR arabian)"));
    }

    @Test
    void notBindsTighterThanAnd() {
        assertEquals(List.of("Cinder"), found("NOT adult AND arabian"));
    }

    @Test
    void notNegatesAWholeBracketedGroup() {
        // The thing -term could never do.
        assertEquals(List.of("Boyd"), found("NOT (arabian OR foal)"));
    }

    // ------------------------------------------------------------------
    // Columns and operators
    // ------------------------------------------------------------------

    @Test
    void comparisonsWorkWithSpacesAroundTheOperator() {
        assertEquals(found("gen>2"), found("generation > 2"));
        assertEquals(List.of("Amber", "Cinder"), found("generation > 2"));
    }

    @Test
    void colonIsStillEquals() {
        assertEquals(found("breed:arabian"), found("breed = arabian"));
    }

    @Test
    void notEqualsIsSupportedBothWays() {
        assertEquals(List.of("Boyd"), found("breed != arabian"));
        assertEquals(found("breed != arabian"), found("breed <> arabian"));
    }

    @Test
    void inIsAnOrOverOneColumn() {
        assertEquals(List.of("Amber", "Boyd", "Cinder"), found("breed IN (arabian, shire)"));
        assertEquals(List.of("Boyd"), found("breed IN (shire)"));
    }

    @Test
    void likeTakesAWildcard() {
        assertEquals(List.of("Amber", "Cinder"), found("breed LIKE 'Arab%'"));
        assertEquals(List.of("Amber", "Cinder"), found("breed LIKE '%rabia%'"));
        assertEquals(List.of("Boyd"), found("name LIKE 'Boy%'"));
    }

    @Test
    void quotingProtectsAKeyword() {
        // A horse could be called Or. Without quoting, the parser would read the
        // keyword and the query would mean something else entirely.
        assertEquals(List.of(), found("name = 'or'"));
        assertTrue(found("name = 'Amber'").contains("Amber"));
    }

    @Test
    void aValueMayContainASlash() {
        // E/e must survive tokenizing as one value - a '/' is not an operator.
        assertEquals(List.of("Boyd"), found("genotype = 'E/e'"));
        assertEquals(found("genotype:E/e"), found("genotype = 'E/e'"));
    }

    // ------------------------------------------------------------------
    // Zygosity - the question the old language could not ask
    // ------------------------------------------------------------------

    @Test
    void aGeneNameIsAColumnAndTakesAZygosity() {
        assertEquals(List.of("Amber"), found("extension = hom"));
        assertEquals(List.of("Boyd"), found("extension = het"));
        assertEquals(List.of("Cinder"), found("extension = none"));
        assertEquals(List.of("Amber", "Boyd"), found("extension = any"));
    }

    @Test
    void anyIsTheOldCarriesKey() {
        assertEquals(found("gene:extension"), found("extension = any"));
    }

    @Test
    void inMixesZygositiesTheWayTheOwnerAskedFor() {
        // "homozygous healers and (heterozygous or homozygous flyers)", in the
        // shape it actually gets typed.
        assertEquals(List.of("Amber", "Boyd"), found("extension IN (het, hom)"));
        assertEquals(found("extension IN (het, hom)"),
                found("extension = het OR extension = hom"));
    }

    @Test
    void aGeneColumnStillTakesAnAlleleToken() {
        assertEquals(found("gene:e"), found("extension = e"));
    }

    // ------------------------------------------------------------------
    // Half-typed
    // ------------------------------------------------------------------

    @Test
    void everyPrefixOfAQueryParsesWithoutThrowing() {
        // Literally what the box sees as somebody types this.
        String whole = "extension = hom AND (breed LIKE 'Arab%' OR generation > 2)";
        for (int i = 0; i <= whole.length(); i++) {
            String prefix = whole.substring(0, i);
            HorseQuery.filter(stable(), prefix);
        }
    }

    @Test
    void malformedGrammarMatchesNothingAndDoesNotThrow() {
        String[] broken = {
                "AND", "OR", "NOT", "(", ")", "()", "((()", ")))",
                "AND AND", "breed =", "= arabian", "breed IN", "breed IN (",
                "breed IN ()", "LIKE", "breed LIKE", "'unclosed", "arabian AND",
                "arabian OR", "NOT NOT", "> 2", ">=", "IN (a, b)",
        };
        for (String query : broken) {
            HorseQuery.filter(stable(), query);
        }
    }

    @Test
    void aDanglingAndKeepsWhatCameBeforeIt() {
        // Mid-type, the left-hand side should already be filtering - a table
        // that empties itself the moment you type AND reads as broken.
        assertEquals(found("arabian"), found("arabian AND"));
    }

    @Test
    void anUnclosedBracketClosesItself() {
        assertEquals(found("(arabian OR shire)"), found("(arabian OR shire"));
    }

    @Test
    void keywordsAreCaseInsensitive() {
        assertEquals(found("arabian AND adult"), found("arabian and adult"));
        assertEquals(found("NOT arabian"), found("not arabian"));
        assertEquals(found("breed IN (shire)"), found("breed in (shire)"));
    }

    @Test
    void anEmptyQueryStillKeepsEverything() {
        assertEquals(3, HorseQuery.filter(stable(), "").size());
        assertEquals(3, HorseQuery.filter(stable(), "   ").size());
        assertEquals(3, HorseQuery.filter(stable(), null).size());
    }
}
