package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>Which generated stables have already had their horses.</b> One of these per
 * level, because a structure belongs to a level.
 *
 * <h2>Why this has to exist</h2>
 * {@link StablePopulator} hangs off a chunk load, and a chunk loads every time a
 * player walks back to it. Without a record, a stable would gain another seven
 * horses every visit - the classic failure of this whole class of hook, and the
 * kind that looks like a feature for two days and then is a paddock with three
 * hundred horses in it.
 *
 * <p>Keyed by the structure id and the corner of its bounding box together: the
 * corner alone would collide between two different stables generated at the same
 * spot in different worlds, and the id alone would fill exactly one of them.
 */
public final class StablePopulationData extends SavedData {

    public static final Codec<StablePopulationData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("filled").forGetter(d -> List.copyOf(d.filled))
    ).apply(i, StablePopulationData::new));

    public static final SavedDataType<StablePopulationData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "stable_population"),
            StablePopulationData::new,
            CODEC);

    private final Set<String> filled;

    private StablePopulationData() {
        this.filled = new HashSet<>();
    }

    private StablePopulationData(List<String> keys) {
        this.filled = new HashSet<>(keys);
    }

    public static StablePopulationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Claim this stable for filling. {@code true} the first time and
     * {@code false} for ever after, so the caller can simply return.
     */
    public boolean claim(Identifier structure, BlockPos corner) {
        String key = structure + "@" + corner.getX() + "," + corner.getY() + "," + corner.getZ();
        if (!filled.add(key)) {
            return false;
        }
        setDirty();
        return true;
    }

    /** How many stables this level has filled - a debug line, not a game rule. */
    public int count() {
        return filled.size();
    }
}
