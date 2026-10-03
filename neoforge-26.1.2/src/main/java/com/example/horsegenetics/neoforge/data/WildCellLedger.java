package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.wild.TopUpPlan;
import com.example.horsegenetics.common.wild.TopUpPlan.CellStamp;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * <b>Which top-up cells have been looked at today</b>, per level - the stamps
 * {@link TopUpPlan} reads. Kept on the level's own data storage (as vanilla's
 * raids are), so each dimension has its own and a cell key needs no dimension.
 *
 * <p>Only today's stamps are ever worth keeping - an older one means exactly
 * what a missing one means ({@link TopUpPlan#keep}) - so the first touch on a new
 * day drops the rest. That is the whole bound: the ledger holds the cells players
 * have been near since the day turned. Saved so a restart the same day does not
 * roll every cell a second time. A world without the file starts empty, which
 * rolls each cell once on its first visit - the intended first day.
 */
public final class WildCellLedger extends SavedData {

    private record Entry(long cell, long day, long signature) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("cell").forGetter(Entry::cell),
                Codec.LONG.fieldOf("day").forGetter(Entry::day),
                Codec.LONG.fieldOf("signature").forGetter(Entry::signature)
        ).apply(i, Entry::new));
    }

    public static final Codec<WildCellLedger> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.list(Entry.CODEC).optionalFieldOf("cells", List.of()).forGetter(WildCellLedger::snapshot)
    ).apply(i, WildCellLedger::new));

    public static final SavedDataType<WildCellLedger> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "wild_cells"),
            WildCellLedger::new,
            CODEC);

    private final Long2ObjectOpenHashMap<CellStamp> stamps = new Long2ObjectOpenHashMap<>();

    /** The day the stale stamps were last dropped. Not saved: the first touch after a load prunes. */
    private long prunedDay = Long.MIN_VALUE;

    private WildCellLedger() {
    }

    private WildCellLedger(List<Entry> entries) {
        for (Entry e : entries) {
            stamps.put(e.cell(), new CellStamp(e.day(), e.signature()));
        }
    }

    public static WildCellLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Entry> snapshot() {
        List<Entry> out = new ArrayList<>(stamps.size());
        for (Long2ObjectOpenHashMap.Entry<CellStamp> e : stamps.long2ObjectEntrySet()) {
            out.add(new Entry(e.getLongKey(), e.getValue().day(), e.getValue().signature()));
        }
        return out;
    }

    /** The cell's stamp, or {@code null}. Drops yesterday's stamps first, once a day. */
    public CellStamp stampOf(long cell, long today) {
        if (prunedDay != today) {
            prunedDay = today;
            if (stamps.values().removeIf(s -> !TopUpPlan.keep(s, today))) {
                setDirty();
            }
        }
        return stamps.get(cell);
    }

    public void stamp(long cell, long today, long signature) {
        stamps.put(cell, new CellStamp(today, signature));
        setDirty();
    }

    public int size() {
        return stamps.size();
    }
}
