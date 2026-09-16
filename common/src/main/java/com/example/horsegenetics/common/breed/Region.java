package com.example.horsegenetics.common.breed;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The part of the old world a breed - and therefore a horse trader - came from.
 *
 * <p>A cowboy does not sell a random assortment of the world's horses. He sells
 * what the people he came with brought, which is why his string is drawn from
 * one region and his name from the same one: villagers got pulled out of the
 * real world along with their herds, and landed here together.
 *
 * <p>This is the first consumer of {@link Breed#country()}, which has been a
 * lower-case token on every shipped breed from the start and until now was read
 * by nothing. The parser has always forced it lower case precisely so it could
 * be keyed on (see {@code BreedSpecParser}); this is the key.
 *
 * <h2>Why these groupings and not the obvious ones</h2>
 *
 * <p>The rule the groups answer to is: <b>a cowboy must be able to stock at
 * least five different breeds from his own region</b>, and the countries in a
 * region must be near each other. Those two pull against each other, because the
 * breed roster is not evenly spread over the globe - it is overwhelmingly
 * European and North American. Four sensible-looking regions (North Africa,
 * Sub-Saharan Africa, the Middle East, Central America) have one or two breeds
 * each and cannot seat a trader on their own, so each is folded into its nearest
 * neighbour rather than left to spawn an empty stall.
 *
 * <p>Two regions run over what would otherwise be a fifteen-breed ceiling -
 * {@link #NORTH_AMERICA} and {@link #BRITAIN_AND_IRELAND} - and there is no
 * honest fix for that short of inventing countries. Those two are simply where
 * the real-world breed registries are densest.
 *
 * <p>A breed with no country at all - a magical one the mod invented, or
 * {@code Breeds.FERAL_MIXED} - belongs to no region and is never regional stock.
 * {@link #forCountry(String)} returns empty for it, and a cowboy will not put one
 * in his string on the strength of where he is from.
 */
public enum Region {

    NORTH_AMERICA("north_america", "North America",
            "united_states", "canada"),

    BRITAIN_AND_IRELAND("britain_and_ireland", "Britain and Ireland",
            "united_kingdom", "ireland"),

    WESTERN_EUROPE("western_europe", "Western Europe",
            "france", "germany", "netherlands", "belgium"),

    SCANDINAVIA("scandinavia", "Scandinavia and the North",
            "norway", "sweden", "denmark", "finland", "iceland"),

    MEDITERRANEAN("mediterranean", "Mediterranean Europe",
            "portugal", "spain", "italy", "croatia", "montenegro", "serbia",
            "bosnia_and_herzegovina", "greece"),

    EASTERN_EUROPE("eastern_europe", "Eastern Europe",
            "austria", "poland", "czech_republic", "hungary", "romania", "bulgaria", "ukraine"),

    CENTRAL_ASIA("central_asia", "Russia, Central Asia and India",
            "russia", "mongolia", "azerbaijan", "turkmenistan", "uzbekistan", "india"),

    ASIA_PACIFIC("asia_pacific", "East Asia and the Pacific",
            "china", "japan", "australia", "new_zealand"),

    AFRICA_NEAR_EAST("africa_near_east", "Africa and the Near East",
            "morocco", "south_africa", "saudi_arabia", "iran", "turkey"),

    LATIN_AMERICA("latin_america", "Latin America",
            "mexico", "puerto_rico", "argentina", "brazil", "chile", "colombia");

    /** Country token to region, built once. Every country belongs to exactly one. */
    private static final Map<String, Region> BY_COUNTRY = byCountry();

    private final String id;
    private final String label;
    private final Set<String> countries;

    Region(String id, String label, String... countries) {
        this.id = id;
        this.label = label;
        this.countries = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(countries)));
    }

    /** The lower-case token this region is keyed and filed under. */
    public String id() {
        return id;
    }

    /** What a person would call it, for a wiki table or a debug line. */
    public String label() {
        return label;
    }

    /** The {@link Breed#country()} tokens that land in this region. */
    public Set<String> countries() {
        return countries;
    }

    /**
     * The region a country token belongs to, or empty if it belongs to none -
     * which means either a blank country (a magical breed) or a country no
     * shipped breed has claimed yet. A drop-in breed from a new country is the
     * second case, and it is deliberately not an error: it simply never becomes
     * a cowboy's regional speciality.
     */
    public static Optional<Region> forCountry(String country) {
        if (country == null || country.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_COUNTRY.get(country));
    }

    /** The region a breed is from, if it claims a country this mod groups. */
    public static Optional<Region> forBreed(Breed breed) {
        return breed == null ? Optional.empty() : forCountry(breed.country());
    }

    /**
     * Every breed of this region that the given source may produce, in registry
     * order. The cowboy asks with {@link BreedSource#COWBOY}; nothing stops
     * another caller asking with {@link BreedSource#WILD}, and the answer is a
     * different and equally true claim about the region.
     */
    public List<Breed> breeds(BreedSource source) {
        List<Breed> out = new java.util.ArrayList<>();
        for (Breed breed : Breeds.all()) {
            if (breed.allows(source) && countries.contains(breed.country())) {
                out.add(breed);
            }
        }
        return out;
    }

    private static Map<String, Region> byCountry() {
        Map<String, Region> map = new HashMap<>();
        for (Region region : values()) {
            for (String country : region.countries) {
                Region clash = map.put(country, region);
                if (clash != null) {
                    throw new IllegalStateException(
                            "country \"" + country + "\" is in two regions: " + clash + " and " + region);
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
