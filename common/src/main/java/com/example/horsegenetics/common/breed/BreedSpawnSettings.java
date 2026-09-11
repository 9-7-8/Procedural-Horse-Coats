package com.example.horsegenetics.common.breed;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>A world's say over the breeds the mod ships</b> - which of them spawn, how
 * often, where and when - and over <b>Feral Mixed</b>, the unbred horse that
 * fills in wherever no breed does.
 *
 * <p>A player's own breeds are edited in their own files; the shipped ones are
 * inside the jar, so this is how a world changes them without unpacking it. The
 * host reads a settings file (NeoForge: {@code .minecraft/phc/breed-spawning.toml},
 * {@code BreedSpawningConfig}) and hands the result to
 * {@link Breeds#applySpawnSettings}, which swaps the affected breeds for copies
 * carrying the new biomes, weight and hours. Everything that asks the registry -
 * wild herds, the herd biome modifier, the cowboy, stables, egg loot, the breed
 * book - then sees the world's version without knowing there was a setting.
 *
 * <h2>Switching a breed off</h2>
 * A breed that is switched off keeps its registry entry - horses already
 * carrying its label still have a breed, and the breed book still explains it -
 * but it loses every {@link BreedSource}: no wild herd, no cowboy, no stable, no
 * egg. A spawn weight of 0 is narrower: the breed stops heading wild herds and
 * keeps the rest.
 *
 * <h2>Feral Mixed</h2>
 * Feral Mixed is not a file and has no sources to take away, so it has its own
 * three settings: whether a wild horse may be one at all, the biomes it may be
 * one in, and a weight against the biome's breeds when a herd is founded. A wild
 * spawn with no breed it may be <i>and</i> no Feral Mixed to fall back on is not
 * spawned - that is what lets a world contain only the breeds its owner made.
 */
public final class BreedSpawnSettings {

    /** One shipped breed's settings, as read. */
    public record BreedOverride(boolean enabled, double spawnWeight, List<String> biomes, SpawnTime spawnTime) {

        public BreedOverride {
            biomes = List.copyOf(biomes);
            spawnTime = spawnTime == null ? SpawnTime.ANY : spawnTime;
            spawnWeight = Math.max(0.0, spawnWeight);
        }

        /** What a breed says of itself - the settings that change nothing. */
        public static BreedOverride of(Breed b) {
            return new BreedOverride(true, b.spawnWeight(), b.biomes(), b.spawnTime());
        }
    }

    /**
     * Feral Mixed's settings.
     *
     * @param enabled    may a wild spawn be Feral Mixed at all
     * @param biomes     where it may be; empty is everywhere
     * @param herdWeight its weight against a biome's breeds when a herd is founded;
     *                   0 means only where no breed may spawn - the default, and
     *                   what Feral Mixed always was
     */
    public record Feral(boolean enabled, List<String> biomes, double herdWeight) {

        public static final Feral DEFAULT = new Feral(true, List.of(), 0.0);

        public Feral {
            biomes = List.copyOf(biomes);
            herdWeight = Math.max(0.0, herdWeight);
        }

        /** May a wild horse in {@code biomeId} be Feral Mixed? */
        public boolean allowedIn(String biomeId) {
            return enabled && (biomes.isEmpty() || biomes.contains(biomeId));
        }
    }

    private final boolean builtinsEnabled;
    private final Map<String, BreedOverride> overrides;
    private final Feral feral;

    public BreedSpawnSettings(boolean builtinsEnabled, Map<String, BreedOverride> overrides, Feral feral) {
        this.builtinsEnabled = builtinsEnabled;
        this.overrides = Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
        this.feral = feral == null ? Feral.DEFAULT : feral;
    }

    /** The settings that change nothing: every breed as its file says, Feral Mixed as always. */
    public static final BreedSpawnSettings DEFAULT =
            new BreedSpawnSettings(true, Map.of(), Feral.DEFAULT);

    public boolean builtinsEnabled() {
        return builtinsEnabled;
    }

    public Map<String, BreedOverride> overrides() {
        return overrides;
    }

    public Feral feral() {
        return feral;
    }

    /**
     * The breed as this world has it. {@code shipped} is whether it came from
     * the jar - only those are governed here; a drop-in breed is its own file.
     */
    Breed apply(Breed original, boolean shipped) {
        if (!shipped) {
            return original;
        }
        BreedOverride o = overrides.get(original.id());
        if (!builtinsEnabled || (o != null && !o.enabled())) {
            return copy(original, original.biomes(), original.spawnWeight(), Set.of(), original.spawnTime());
        }
        if (o == null) {
            return original;
        }
        Set<BreedSource> sources = EnumSet.noneOf(BreedSource.class);
        sources.addAll(original.sources());
        if (o.spawnWeight() <= 0.0) {
            sources.remove(BreedSource.WILD);
        }
        return copy(original, o.biomes(), o.spawnWeight(), sources, o.spawnTime());
    }

    private static Breed copy(Breed b, List<String> biomes, double weight, Set<BreedSource> sources,
                              SpawnTime time) {
        return new Breed(b.id(), b.name(), b.magical(), biomes, weight, sources, b.genePools(),
                b.scores(), b.bands(), b.notes(), b.price(), b.description(), time);
    }
}
