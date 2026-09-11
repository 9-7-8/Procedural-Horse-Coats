package com.example.horsegenetics.common.breed;

/**
 * <b>When a breed's wild herds turn up</b> - the {@code "spawn_time"} field of
 * a breed file.
 *
 * <p>It is checked at the moment a herd is <i>founded</i>, which is when the
 * host picks a breed for a clump of freshly spawned horses. So a
 * {@link #NIGHT} breed heads the herds that are founded in the dark and a
 * {@link #DAY} breed the ones founded in daylight; outside its hours the breed
 * simply is not a candidate, and the biome's other breeds share the draw.
 *
 * <p>It only speaks for {@link BreedSource#WILD}. A breeder does not refuse to
 * sell you a night-mare at noon, and a spawn egg has no idea what time it is.
 *
 * <p>Worth knowing when writing one: vanilla will not spawn an animal in the
 * dark on an existing chunk, so in practice a night breed's herds come from
 * chunks generated at night - the exploring you do after dark - rather than
 * from the slow trickle of spawns around a player.
 */
public enum SpawnTime {
    /** Day or night - the default, and what every built-in breed is. */
    ANY("any"),
    /** Only while it is light outside. */
    DAY("day"),
    /** Only while it is dark outside. */
    NIGHT("night");

    private final String id;

    SpawnTime(String id) {
        this.id = id;
    }

    /** The token a breed file writes. */
    public String id() {
        return id;
    }

    /** May a herd of this breed be founded now? {@code dark} is the host's own reading. */
    public boolean allows(boolean dark) {
        return this == ANY || (this == NIGHT) == dark;
    }

    /** The value named by {@code token}, or {@code null} if there is none. */
    public static SpawnTime byId(String token) {
        for (SpawnTime t : values()) {
            if (t.id.equals(token)) {
                return t;
            }
        }
        return null;
    }
}
