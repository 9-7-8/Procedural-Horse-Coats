package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.name.PersonNameGenerator;
import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The region grouping, and the rule it exists to keep.
 *
 * <p>The load-bearing test here is {@link #everyRegionCanSeatACowboy()}. A cowboy
 * stocks at least five breeds from his own region, so a region that cannot field
 * five is a region whose trader stands at an empty rail. That is an invariant of
 * the breed roster, not of this class, and it is exactly the sort of thing that
 * silently stops being true when somebody switches a breed's {@code cowboy}
 * source off.
 */
class RegionTest {

    /** The smallest string a cowboy is expected to be able to assemble. */
    private static final int MIN_BREEDS_PER_REGION = 5;

    @BeforeAll
    static void loadBreeds() {
        Breeds.resetForTesting();
        Breeds.loadBuiltins();
    }

    @Test
    void everyCountryBelongsToExactlyOneRegion() {
        Set<String> seen = new HashSet<>();
        for (Region region : Region.values()) {
            for (String country : region.countries()) {
                assertTrue(seen.add(country),
                        country + " is claimed by more than one region");
            }
        }
        assertFalse(seen.isEmpty());
    }

    @Test
    void mapsKnownCountriesToTheirRegion() {
        assertEquals(Region.NORTH_AMERICA, Region.forCountry("united_states").orElseThrow());
        assertEquals(Region.SCANDINAVIA, Region.forCountry("iceland").orElseThrow());
        assertEquals(Region.MEDITERRANEAN, Region.forCountry("serbia").orElseThrow());
        assertEquals(Region.CENTRAL_ASIA, Region.forCountry("india").orElseThrow());
        assertEquals(Region.ASIA_PACIFIC, Region.forCountry("new_zealand").orElseThrow());
    }

    @Test
    void aBreedWithNoCountryBelongsToNoRegion() {
        // The Dhampir is magical and claims no country; it must never become a
        // cowboy's regional speciality on the strength of where he is from.
        assertTrue(Region.forCountry("").isEmpty());
        assertTrue(Region.forCountry(null).isEmpty());
        assertTrue(Region.forCountry("atlantis").isEmpty());
    }

    @Test
    void everyRegionCanSeatACowboy() {
        for (Region region : Region.values()) {
            List<Breed> stock = region.breeds(BreedSource.COWBOY);
            assertTrue(stock.size() >= MIN_BREEDS_PER_REGION,
                    region.id() + " has only " + stock.size()
                            + " cowboy-sellable breeds, needs " + MIN_BREEDS_PER_REGION);
        }
    }

    @Test
    void everyRegionHasNameTables() {
        for (Region region : Region.values()) {
            PersonNameGenerator names = PersonNameGenerator.forRegion(region.id());
            assertTrue(names.combinations() > 100,
                    region.id() + " has too few name combinations: " + names.combinations());
            String name = names.generate(new FakeRng().ints(0, 0));
            assertTrue(name.contains(" "), region.id() + " produced: " + name);
        }
    }

    /**
     * <b>The name tables are meant to be hand-edited</b>, and half of them carry
     * diacritics - &Eacute;mile, Bj&oslash;rnstad, M&auml;kinen, In&ecirc;s. An editor
     * that saves one as Windows-1252 does not fail: {@code PersonNameGenerator}
     * decodes with an explicit UTF-8 reader, so a high byte becomes U+FFFD and the
     * game ships a cowboy called Bj<b>?</b>rnstad with nothing logged anywhere.
     *
     * <p>So this reads the files as they are on the classpath rather than going
     * through the generator, and fails on a replacement character. It is the only
     * guard between a text editor's encoding menu and a silently mangled name.
     */
    @Test
    void nameTablesSurviveAsUtf8() {
        for (Region region : Region.values()) {
            for (String half : List.of("alpha", "beta")) {
                String resource = "/horsegenetics/names/people/" + region.id() + "-" + half + ".txt";
                String text = readResource(resource);
                assertFalse(text.isEmpty(), resource + " is empty");
                // The code point, never a literal character: this test exists because source
                // files get re-saved in the wrong encoding, and that includes this one. A
                // literal here could be damaged by the very thing it is meant to catch, and
                // would still compile.
                int bad = text.indexOf(0xFFFD);
                assertEquals(-1, bad,
                        resource + " is not valid UTF-8 - a replacement character at offset "
                                + bad + ", near: \"" + snippetAround(text, bad) + "\". Re-save it as UTF-8.");
            }
        }
    }

    private static String readResource(String resource) {
        try (InputStream in = RegionTest.class.getResourceAsStream(resource)) {
            assertTrue(in != null, "missing on the classpath: " + resource);
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder all = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    all.append(line).append('\n');
                }
                return all.toString();
            }
        } catch (Exception e) {
            throw new IllegalStateException("could not read " + resource, e);
        }
    }

    /** Enough context round a bad byte to find it in the file by eye. */
    private static String snippetAround(String text, int at) {
        if (at < 0) {
            return "";
        }
        return text.substring(Math.max(0, at - 20), Math.min(text.length(), at + 20));
    }

    @Test
    void everyShippedBreedWithACountryIsPlaced() {
        for (Breed breed : Breeds.all()) {
            if (breed.country().isEmpty()) {
                continue;
            }
            assertTrue(Region.forCountry(breed.country()).isPresent(),
                    breed.id() + " is from \"" + breed.country() + "\", which no region claims");
        }
    }
}
