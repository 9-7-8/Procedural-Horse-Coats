package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.horse.HorseDatabase;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.InMemoryHorseDatabase;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-global persistent ancestry store. A thin wrapper: every real
 * operation is delegated to the Layer-1 {@link HorseDatabase}; this class only
 * adds NeoForge {@link SavedData} persistence (via {@link #CODEC}) and
 * dirty-tracking. It is stored on the server-wide data storage
 * ({@link MinecraftServer#getDataStorage()}), not per-level, since lineage is
 * global.
 */
public final class HorseAncestryData extends SavedData implements HorseDatabase {

    public static final Codec<HorseAncestryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(HorseRecordCodecs.CODEC).fieldOf("horses").forGetter(HorseAncestryData::snapshot)
    ).apply(instance, HorseAncestryData::new));

    public static final SavedDataType<HorseAncestryData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_ancestry"),
            HorseAncestryData::new,
            CODEC);

    private final InMemoryHorseDatabase delegate;

    private HorseAncestryData() {
        this.delegate = new InMemoryHorseDatabase();
    }

    private HorseAncestryData(List<HorseRecord> horses) {
        this.delegate = new InMemoryHorseDatabase(horses);
    }

    public static HorseAncestryData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<HorseRecord> snapshot() {
        return List.copyOf(delegate.all());
    }

    @Override
    public void record(HorseRecord horse) {
        // Only dirty the file when something actually changed (HorseRecord is a
        // value type, so an unchanged re-record compares equal).
        if (!delegate.lookup(horse.id()).map(horse::equals).orElse(false)) {
            delegate.record(horse);
            setDirty();
        }
    }

    @Override
    public Optional<HorseRecord> lookup(UUID id) {
        return delegate.lookup(id);
    }

    @Override
    public boolean forget(UUID id) {
        if (!delegate.forget(id)) {
            return false;
        }
        setDirty();
        return true;
    }

    @Override
    public int offspringCount(UUID parentA, UUID parentB) {
        return delegate.offspringCount(parentA, parentB);
    }

    @Override
    public List<HorseRecord> ancestorsOf(UUID id, int depth) {
        return delegate.ancestorsOf(id, depth);
    }

    public int size() {
        return delegate.size();
    }
}
