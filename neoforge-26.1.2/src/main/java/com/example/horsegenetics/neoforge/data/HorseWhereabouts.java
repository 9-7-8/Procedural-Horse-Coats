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
 * <p>And it remembers <b>stasis</b>, which is the same shape of problem again: a
 * horse in a chamber is neither alive in a chunk nor dead, it will never write
 * another sighting, and the browser was left calling it "not loaded" for ever.
 * The mark is written by the capture and cleared by the release - the two
 * moments that are certain - rather than inferred from anything.
 *
 * <p>Server-global, like the ancestry data. Shared infrastructure rather than
 * whistle infrastructure: the browser, the stall records and the tickets all
 * want the same lookup, which is exactly why the roadmap preferred it.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class HorseWhereabouts extends SavedData {

    /**
     * One horse's last sighting.
     *
     * @param stasis it is in a stasis chamber - the one case where the horse is
     *               not an entity anywhere and its whereabouts are nonetheless
     *               known. {@link #pos} is then where it went in, which is of no
     *               use for finding it and is kept only because releasing it
     *               writes a fresh sighting over the top anyway.
     */
    public record Seen(UUID horse, ResourceKey<Level> dimension, BlockPos pos, boolean dead,
                       boolean stasis) {
        public static final Codec<Seen> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("horse").forGetter(Seen::horse),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Seen::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Seen::pos),
                Codec.BOOL.optionalFieldOf("dead", false).forGetter(Seen::dead),
                Codec.BOOL.optionalFieldOf("stasis", false).forGetter(Seen::stasis)
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

    /**
     * Record a sighting. A dead horse stays dead - a stale sighting cannot
     * revive it - and a sighting clears the stasis mark, because a horse
     * something can see is a horse that is out of the bottle however it got
     * there.
     */
    public void seen(UUID horse, ResourceKey<Level> dimension, BlockPos pos) {
        Seen before = byHorse.get(horse);
        if (before != null && before.dead()) {
            return;
        }
        if (before != null && !before.stasis()
                && before.dimension().equals(dimension) && before.pos().distSqr(pos) < MOVE_SQR) {
            return;
        }
        byHorse.put(horse, new Seen(horse, dimension, pos.immutable(), false, false));
        setDirty();
    }

    /** It died. Kept rather than deleted, so a whistle in a chest can still learn it. */
    public void died(UUID horse, ResourceKey<Level> dimension, BlockPos pos) {
        byHorse.put(horse, new Seen(horse, dimension, pos.immutable(), true, false));
        setDirty();
    }

    /**
     * <b>It went into a stasis chamber.</b> The horse is not an entity any more,
     * so nothing else will ever write another sighting for it - which is exactly
     * why this has to be written at the moment it goes in, and why the browser
     * would otherwise show it as "not loaded" for ever.
     *
     * <p>Where it went in is kept as the position for want of anything truer; a
     * chamber travels, and the only honest answer to <i>where</i> is the one the
     * table prints, which is that it is in a chamber.
     */
    public void enteredStasis(UUID horse, ResourceKey<Level> dimension, BlockPos pos) {
        byHorse.put(horse, new Seen(horse, dimension, pos.immutable(), false, true));
        setDirty();
    }

    /**
     * <b>It came back out.</b> Called from the release, before the horse has
     * moved far enough for {@link #seen} to notice it - so the mark is cleared
     * at the one moment that is certain rather than on the next slow stagger.
     */
    public void leftStasis(UUID horse, ResourceKey<Level> dimension, BlockPos pos) {
        Seen before = byHorse.get(horse);
        if (before != null && before.dead()) {
            return;
        }
        byHorse.put(horse, new Seen(horse, dimension, pos.immutable(), false, false));
        setDirty();
    }

    public Optional<Seen> lookup(UUID horse) {
        return Optional.ofNullable(byHorse.get(horse));
    }

    public boolean isDead(UUID horse) {
        Seen s = byHorse.get(horse);
        return s != null && s.dead();
    }

    /** Is this horse in a stasis chamber? The browser's whereabouts column. */
    public boolean inStasis(UUID horse) {
        Seen s = byHorse.get(horse);
        return s != null && s.stasis();
    }
}
