package com.example.horsegenetics.common.breed;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * <b>Where a breed is allowed to come from.</b> A breed names any subset; the
 * default is all of them, which is what almost every breed wants.
 *
 * <p>It is a checklist rather than a mode because the four are genuinely
 * independent. "A breed the cowboy has heard of but that no longer runs wild"
 * is {@code [cowboy, spawn_egg]}; "a breed that only exists as a collector's
 * egg" is {@code [spawn_egg]}; "a landrace nobody farms" is {@code [wild]}.
 * None of those is a special case in any of the four consumers - each one just
 * asks {@link Breed#allows}.
 *
 * <ul>
 *   <li>{@link #WILD} - may head a wild herd in one of its {@link Breed#biomes()}.</li>
 *   <li>{@link #COWBOY} - the cowboy may breed and sell one (as a signed
 *       transfer paper, priced from {@link Breed#price()}).</li>
 *   <li>{@link #SPAWN_EGG} - a breed spawn egg for it exists, and can turn up
 *       in dungeon loot or in the horseman's stock. Clearing this is the
 *       "opt out of having a spawn egg" switch.</li>
 *   <li>{@link #STABLE} - may be pre-placed in a generated stable.</li>
 * </ul>
 */
public enum BreedSource {
    WILD,
    COWBOY,
    SPAWN_EGG,
    STABLE;

    /** Every source - what a breed that names none is given. */
    public static final Set<BreedSource> ALL =
            Collections.unmodifiableSet(EnumSet.allOf(BreedSource.class));

    /** The lower-case token this is written as in a breed JSON file. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The source {@code token} names, or {@code null} if it names none. */
    public static BreedSource byId(String token) {
        if (token == null) {
            return null;
        }
        String t = token.trim().toLowerCase(Locale.ROOT);
        for (BreedSource s : values()) {
            if (s.id().equals(t)) {
                return s;
            }
        }
        return null;
    }
}
