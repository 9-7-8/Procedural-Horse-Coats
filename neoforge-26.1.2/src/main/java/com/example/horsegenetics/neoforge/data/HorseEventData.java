package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.log.HorseEvent;
import com.example.horsegenetics.common.log.HorseEventLog;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.UUID;

/**
 * Server-global persistent <b>event log</b> - what has been happening to each
 * player's horses, as the browser's Log tab reads it.
 *
 * <p>A thin wrapper in exactly the shape of {@link HorseAncestryData}: every
 * real rule - ordering, the cap, deduplication - is the Layer-1
 * {@link HorseEventLog}, and this adds {@link SavedData} persistence and
 * dirty-tracking around it.
 *
 * <h2>Why server-global rather than per-player or per-level</h2>
 * Per-level is wrong because a horse dies in the Nether and its owner reads
 * about it in the overworld; the log is about a player's stable, and a stable
 * spans dimensions. Per-player data would be the other obvious home, but the
 * writers are all server-side handlers that have a horse and an owner UUID and
 * frequently <i>no player entity at all</i> - the owner is offline, which is
 * the case the Log tab exists for. A global store keyed by owner UUID is the
 * one shape that can be written to for a player who is not there.
 */
public final class HorseEventData extends SavedData {

    public static final Codec<HorseEventData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(HorseEventCodecs.OWNED_CODEC).fieldOf("events").forGetter(HorseEventData::snapshot)
    ).apply(instance, HorseEventData::new));

    public static final SavedDataType<HorseEventData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_events"),
            HorseEventData::new,
            CODEC);

    private final HorseEventLog delegate;

    private HorseEventData() {
        this.delegate = new HorseEventLog();
    }

    private HorseEventData(List<HorseEventLog.Owned> events) {
        this.delegate = new HorseEventLog(events);
    }

    public static HorseEventData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<HorseEventLog.Owned> snapshot() {
        return delegate.snapshot();
    }

    /**
     * Write one row.
     *
     * @return {@code true} if it was kept rather than deduplicated - which is
     *         also exactly when the file needs writing again
     */
    public boolean add(UUID owner, HorseEvent event) {
        if (!delegate.add(owner, event)) {
            return false;
        }
        setDirty();
        return true;
    }

    /** One player's log, newest first. */
    public List<HorseEvent> of(UUID owner) {
        return delegate.of(owner);
    }

    public int size(UUID owner) {
        return delegate.size(owner);
    }
}
