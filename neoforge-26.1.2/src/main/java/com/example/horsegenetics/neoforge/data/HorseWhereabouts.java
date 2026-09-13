package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>Where every tamed horse was last seen, and whether it has died.</b>
 *
 * <p>The roadmap's "option C" for callable horses: whereabouts are entity facts,
 * so they are only knowable for a horse in a loaded chunk - and a whistle that
 * calls a horse two thousand blocks away, or in another dimension, has to find
 * one that is not ticking. This keeps the last dimension and block a tamed horse
 * was seen at, written on a slow stagger and whenever it leaves a level, so the
 * caller can load that chunk and look.
 *
 * <p>It also remembers <b>deaths</b>. The ancestry database keeps a dead horse's
 * record on purpose - the pedigree needs it - so "is this horse still alive" is
 * not a question that database can answer, and the ender whistle has to know.
 *
 * <p>Server-global, like the ancestry data. Shared infrastructure rather than
 * whistle infrastructure: the browser, the stall records and the tickets all
 * want the same lookup, which is exactly why the roadmap preferred it.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class HorseWhereabouts extends SavedData {

    /** One horse's last sighting. */
    public record Seen(UUID horse, ResourceKey<Level> dimension, BlockPos pos, boolean dead) {
        public static final Codec<Seen> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("horse").forGetter(Seen::horse),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Seen::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Seen::pos),
                Codec.BOOL.optionalFieldOf("dead", false).forGetter(Seen::dead)
        ).apply(i, Seen::new));
    }

    public static final Codec<HorseWhereabouts> CODEC = RecordCodecBuilder.create(i -> i.group(
            Seen.CODEC.listOf().fieldOf("seen").forGetter(HorseWhereabouts::snapshot)
    ).apply(i, HorseWhereabouts::new));

    public static final SavedDataType<HorseWhereabouts> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_whereabouts"),
            HorseWhereabouts::new,
            CODEC);

    /**
     * How far a horse must have moved before a sighting dirties the file. Close
     * enough that the chunk it names still holds the horse; far enough that a
     * horse grazing in a paddock does not rewrite the save every few seconds.
     */
    private static final double MOVE_SQR = 16.0 * 16.0;

    private final Map<UUID, Seen> byHorse = new HashMap<>();

    private HorseWhereabouts() {
    }

    private HorseWhereabouts(List<Seen> seen) {
        for (Seen s : seen) {
            byHorse.put(s.horse(), s);
        }
    }

    public static HorseWhereabouts get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Seen> snapshot() {
        return new ArrayList<>(byHorse.values());
    }

    /** Record a sighting. A dead horse stays dead - a stale sighting cannot revive it. */
    public void seen(UUID horse, ResourceKey<Level> dimension, BlockPos pos) {
        Seen before = byHorse.get(horse);
        if (before != null && before.dead()) {
            return;
        }
        if (before != null && before.dimension().equals(dimension) && before.pos().distSqr(pos) < MOVE_SQR) {
            return;
        }
        byHorse.put(horse, new Seen(horse, dimension, pos.immutable(), false));
        setDirty();
    }

    /** It died. Kept rather than deleted, so a whistle in a chest can still learn it. */
    public void died(UUID horse, ResourceKey<Level> dimension, BlockPos pos) {
        byHorse.put(horse, new Seen(horse, dimension, pos.immutable(), true));
        setDirty();
    }

    public Optional<Seen> lookup(UUID horse) {
        return Optional.ofNullable(byHorse.get(horse));
    }

    public boolean isDead(UUID horse) {
        Seen s = byHorse.get(horse);
        return s != null && s.dead();
    }
}
