package com.example.horsegenetics.common.breed;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>What a breed's wild herds may stand on, and whether they need the light</b>
 * - the {@code "spawn_ground"} and {@code "spawn_in_dark"} fields of a breed
 * file.
 *
 * <p>It exists because a breed file could always name a biome the host would
 * then never spawn a horse in. Vanilla's animal rule is two tests against the
 * position, not the biome: the block <i>under</i> the horse must be in
 * {@code #minecraft:animals_spawnable_on} - which in this SDK is the single
 * entry {@code minecraft:grass_block} - and the raw brightness must be above 8.
 * The Nether has no grass block anywhere in generated terrain, so the two Nether
 * breeds named five biomes, had the spawn duly added to all five by
 * {@code BreedHerdsBiomeModifier}, and could not spawn in any of them. Not
 * rarely: never. The breed egg was the only route to either animal.
 *
 * <p><b>This is additive, never restrictive.</b> Vanilla's rule keeps applying
 * everywhere - a breed that names extra floors still spawns on lit grass like
 * any other. {@link #NONE}, which is what every breed that says nothing gets,
 * changes nothing at all. So this can only ever let a horse spawn somewhere it
 * previously could not, which is why it needs no migration and no config
 * override to switch off: emptying the list is the off switch.
 *
 * <p><b>{@code spawn_in_dark} is not {@code spawn_time}.</b> They are easy to
 * confuse and mean different things. {@link SpawnTime} is about the
 * <i>world clock</i> and is read when a herd is founded - "this breed's herds
 * form at night". This flag is about <i>block light at the spawn position</i>
 * and is read by the placement test - "this breed does not need a lit floor".
 * A Nether breed wants this flag and has no opinion on the hour; the Dhampir
 * wants {@code spawn_time: night} and still needs lit grass.
 *
 * @param floors block ids the horse may stand on, on top of vanilla's own tag;
 *               empty means vanilla's tag alone
 * @param inDark whether the light test is waived where one of {@link #floors}
 *               is underfoot
 */
public record SpawnGround(List<String> floors, boolean inDark) {

    /** Says nothing, changes nothing - what every breed without the fields has. */
    public static final SpawnGround NONE = new SpawnGround(List.of(), false);

    public SpawnGround {
        // An ordered de-duplicating copy: this is written back out to a
        // checked-in breed file by BreedSpecWriter, so the order has to be the
        // author's rather than a hash order. See Breed's canonical constructor
        // for why that matters here.
        floors = floors == null
                ? List.of()
                : List.copyOf(new LinkedHashSet<>(floors));
    }

    /** Nothing declared - the field was absent, or its list was empty. */
    public boolean isEmpty() {
        return floors.isEmpty();
    }

    /** Is {@code blockId} one of the floors this breed named? */
    public boolean standsOn(String blockId) {
        return blockId != null && floors.contains(blockId);
    }

    /**
     * May a herd of this breed spawn with {@code blockId} underfoot at this
     * light? {@code bright} is the host's reading of vanilla's own light test.
     *
     * <p>A declared floor at a bright position passes whatever {@link #inDark}
     * says; the flag only waives the light, and only over a floor this breed
     * actually named. A breed that names floors but leaves the flag off is
     * saying "I will stand on basalt, in the light" - which is a legitimate
     * thing to mean, and is why the two are separate fields.
     */
    public boolean allows(String blockId, boolean bright) {
        return standsOn(blockId) && (bright || inDark);
    }

    /** Every floor named by any of {@code breeds}, in registry then file order. */
    public static Set<String> floorsOf(Iterable<Breed> breeds) {
        Set<String> out = new LinkedHashSet<>();
        for (Breed b : breeds) {
            out.addAll(b.spawnGround().floors());
        }
        return Collections.unmodifiableSet(out);
    }
}
