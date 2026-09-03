package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Server-global persistent store of {@link StallRecord}s, one per bound horse
 * (a horse can only be assigned to one stall - re-binding moves it). Kept on
 * {@link MinecraftServer#getDataStorage()} rather than per-level, like
 * {@link HorseAncestryData}, so a future cross-dimension stall-teleport has a
 * single place to look.
 */
public final class StallData extends SavedData {

    public static final Codec<StallData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(StallRecord.CODEC).fieldOf("stalls").forGetter(StallData::snapshot)
    ).apply(instance, StallData::new));

    public static final SavedDataType<StallData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "stalls"),
            StallData::new,
            CODEC);

    private final Map<UUID, StallRecord> byHorse = new LinkedHashMap<>();

    private StallData() {
    }

    private StallData(List<StallRecord> stalls) {
        for (StallRecord s : stalls) {
            byHorse.put(s.horseId(), s);
        }
    }

    public static StallData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<StallRecord> snapshot() {
        return List.copyOf(byHorse.values());
    }

    /** Assign (or move) a horse's stall. */
    public void assign(StallRecord record) {
        byHorse.put(record.horseId(), record);
        setDirty();
    }

    public StallRecord forHorse(UUID horseId) {
        return byHorse.get(horseId);
    }

    public boolean removeHorse(UUID horseId) {
        if (byHorse.remove(horseId) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    /** Drop whatever stall (if any) has its sign at {@code signPos} in {@code dimension}. */
    public boolean removeBySign(ResourceKey<Level> dimension, BlockPos signPos) {
        UUID hit = null;
        for (StallRecord s : byHorse.values()) {
            if (s.dimension().equals(dimension) && s.signPos().equals(signPos)) {
                hit = s.horseId();
                break;
            }
        }
        return hit != null && removeHorse(hit);
    }

    public List<StallRecord> all() {
        return new ArrayList<>(byHorse.values());
    }

    public List<StallRecord> inDimension(ResourceKey<Level> dimension) {
        List<StallRecord> out = new ArrayList<>();
        for (StallRecord s : byHorse.values()) {
            if (s.dimension().equals(dimension)) {
                out.add(s);
            }
        }
        return out;
    }

    public int size() {
        return byHorse.size();
    }
}
