package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.horse.AfterlifeBudget;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>Horses that have died recently enough to be brought back.</b> An
 * operator's undo for the one loss in this mod that cannot be undone any other
 * way - see {@code server/HorseResurrectCommand}, which is the only thing that
 * spends what is kept here.
 *
 * <h2>The whole horse, not a description of it</h2>
 * A {@link Wake} carries a {@link StasisSnapshot}: the dead animal's complete
 * {@code saveWithoutId} tag, which on NeoForge takes the data attachments with
 * it. That is the same payload a stasis chamber holds and it is here for the
 * same reason - it keeps the horse's <b>UUID</b>, and the UUID is what makes a
 * resurrected horse the same horse rather than a convincing copy. Every foal's
 * pedigree, every stall sign, every bound whistle and every transfer deed keys
 * on it. Rebuilding a horse from its {@link com.example.horsegenetics.common.horse.HorseRecord}
 * instead would produce an animal with the right coat and the wrong identity,
 * which is precisely the thing a grieving owner would notice.
 *
 * <p>It is therefore <b>not</b> a second copy of the ancestry database.
 * {@link HorseAncestryData} keeps a dead horse's record for ever on purpose,
 * because the pedigree needs it, and nothing here duplicates that. What is kept
 * here is the part the ancestry data deliberately does not hold: the live
 * entity - its age, its gear, its bond, its hunger, its pregnancy, its name tag
 * - as it stood at the moment it died.
 *
 * <h2>Why it expires: a size budget</h2>
 * An entity tag is real bytes, per horse, and a world that never throws one
 * away accumulates every horse that ever died in it. So the store as a whole
 * has a budget in bytes, {@code ops.resurrect_budget_mb}: a dead horse is kept
 * until the store would be over it, and then {@link #trimTo} lets the oldest
 * deaths go first, only as many as it takes ({@link AfterlifeBudget}; owner,
 * issue #15). Each wake carries its own cost, {@link Wake#bytes}, measured
 * once when it is kept.
 *
 * <p>The older limit is still here as an optional extra:
 * {@code ops.resurrect_grace_minutes} of the owner's <b>own time online</b>,
 * counted by {@link #spend} from a slow sweep. It is {@code 0} - off - by
 * default. Owner-online time rather than wall clock because that window exists
 * to give the owner a chance to notice and ask, and somebody who was logged off
 * when their mare met a creeper has had no such chance.
 *
 * <p>The elapsed count is kept even while the limit is {@code 0}, so an
 * operator who turns a limit back on starts expiring the backlog rather than
 * grandfathering it.
 *
 * <p><b>Only owned horses are kept.</b> A wild horse dying has nobody to ask
 * for it back, and the horse realm and the debug dimension kill them by the
 * hundred; keeping those would be the whole of this file's disk cost for none
 * of its point.
 *
 * <p>Server-global, like {@link HorseAncestryData} and {@link HorseWhereabouts}
 * - a horse dies in one dimension and is asked after in another.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class HorseAfterlife extends SavedData {

    /**
     * One dead horse, kept.
     *
     * @param ownerName     who owned it, by name, cached at death purely so the
     *                      operator's listing can say whose horse it was without
     *                      a profile lookup per row. {@link #owner} is the
     *                      authority.
     * @param ownerTicks    how much of its owner's own time online has passed
     *                      since it died. Counts <b>up</b>, and is compared
     *                      against the setting when it is read, so changing the
     *                      setting - in either direction, including to "for
     *                      ever" - applies to horses that are already dead.
     * @param deathGameTime the world's game time at the moment of death. The
     *                      listing says "twenty minutes ago" from it, and the
     *                      size budget lets the oldest of these go first. It is
     *                      <b>not</b> the owner-online clock.
     * @param bytes         what keeping this horse costs, as the gzipped size of
     *                      its entity tag - see {@link #measure}. {@code 0} on a
     *                      wake saved before the budget existed, which the store
     *                      measures as it loads.
     */
    public record Wake(StasisSnapshot snapshot, UUID owner, String ownerName,
                       ResourceKey<Level> dimension, BlockPos pos,
                       long deathGameTime, int ownerTicks, long bytes) {

        public static final Codec<Wake> CODEC = RecordCodecBuilder.create(i -> i.group(
                StasisSnapshot.CODEC.fieldOf("horse").forGetter(Wake::snapshot),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Wake::owner),
                Codec.STRING.optionalFieldOf("owner_name", "").forGetter(Wake::ownerName),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Wake::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Wake::pos),
                Codec.LONG.optionalFieldOf("died_at", 0L).forGetter(Wake::deathGameTime),
                Codec.INT.optionalFieldOf("owner_ticks", 0).forGetter(Wake::ownerTicks),
                Codec.LONG.optionalFieldOf("bytes", 0L).forGetter(Wake::bytes)
        ).apply(i, Wake::new));

        /** A horse that has just died: no owner time spent yet, and its cost measured. */
        public static Wake of(StasisSnapshot snapshot, UUID owner, String ownerName,
                              ResourceKey<Level> dimension, BlockPos pos, long deathGameTime) {
            return new Wake(snapshot, owner, ownerName, dimension, pos, deathGameTime, 0,
                    measure(snapshot));
        }

        /** Which horse this is - the snapshot's id, which is the entity's real UUID. */
        public UUID horse() {
            return snapshot.horseId();
        }

        public String horseName() {
            return snapshot.horseName();
        }

        Wake plusTicks(int ticks) {
            return new Wake(snapshot, owner, ownerName, dimension, pos, deathGameTime,
                    ownerTicks + ticks, bytes);
        }

        Wake measured() {
            return bytes > 0 ? this : new Wake(snapshot, owner, ownerName, dimension, pos,
                    deathGameTime, ownerTicks, measure(snapshot));
        }

        AfterlifeBudget.Entry<UUID> budgetEntry() {
            return new AfterlifeBudget.Entry<>(horse(), deathGameTime, bytes);
        }

        /**
         * Is this one past its window? {@code graceTicks} of {@code 0} is "for
         * ever" and never expires - the one case a caller comparing the two
         * numbers directly would get backwards.
         */
        public boolean expired(int graceTicks) {
            return graceTicks > 0 && ownerTicks >= graceTicks;
        }

        /** Ticks of owner-online time left, or {@code -1} when the window is "for ever". */
        public int remaining(int graceTicks) {
            return graceTicks <= 0 ? -1 : Math.max(0, graceTicks - ownerTicks);
        }
    }

    public static final Codec<HorseAfterlife> CODEC = RecordCodecBuilder.create(i -> i.group(
            Wake.CODEC.listOf().fieldOf("wakes").forGetter(HorseAfterlife::snapshot)
    ).apply(i, HorseAfterlife::new));

    public static final SavedDataType<HorseAfterlife> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_afterlife"),
            HorseAfterlife::new,
            CODEC);

    private final Map<UUID, Wake> byHorse = new HashMap<>();

    private HorseAfterlife() {
    }

    private HorseAfterlife(List<Wake> wakes) {
        for (Wake wake : wakes) {
            // A wake saved before the size budget existed has no cost on it
            // yet; measure it here so the budget sees the whole store.
            byHorse.put(wake.horse(), wake.measured());
        }
    }

    /**
     * <b>What one horse costs to keep</b>: the length of its entity tag
     * written the way a {@code .dat} file is written, gzipped.
     *
     * <p>The store's file is one gzip stream over every wake, which compresses
     * across horses a little better than each does alone, so the sum of these
     * slightly <b>over</b>states the file - the budget errs towards letting go
     * a little early, never towards growing past it. Measured once, when the
     * horse is kept, rather than by statting the file, because the file only
     * exists after a save and the sweep runs between saves. UNVERIFIED against
     * a real file of any size.
     */
    static long measure(StasisSnapshot snapshot) {
        CountingStream count = new CountingStream();
        try {
            NbtIo.writeCompressed(snapshot.horse(), count);
        } catch (IOException cannot) {
            // A counting stream cannot fail; if writing the tag itself did,
            // count what got written, and never call a horse free.
        }
        return Math.max(1, count.bytes);
    }

    /** An output stream that keeps nothing but a count. */
    private static final class CountingStream extends OutputStream {
        long bytes;

        @Override
        public void write(int b) {
            bytes++;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            bytes += len;
        }
    }

    public static HorseAfterlife get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Wake> snapshot() {
        return new ArrayList<>(byHorse.values());
    }

    /** It died, and somebody owned it. */
    public void died(Wake wake) {
        byHorse.put(wake.horse(), wake);
        setDirty();
    }

    public Optional<Wake> lookup(UUID horse) {
        return Optional.ofNullable(byHorse.get(horse));
    }

    /** Everything still kept, most recently dead first - the operator's listing. */
    public List<Wake> all() {
        List<Wake> wakes = snapshot();
        wakes.sort(Comparator.comparingLong(Wake::deathGameTime).reversed());
        return wakes;
    }

    /** Everything still kept that belonged to one player, most recently dead first. */
    public List<Wake> ownedBy(UUID owner) {
        List<Wake> wakes = new ArrayList<>();
        for (Wake wake : byHorse.values()) {
            if (wake.owner().equals(owner)) {
                wakes.add(wake);
            }
        }
        wakes.sort(Comparator.comparingLong(Wake::deathGameTime).reversed());
        return wakes;
    }

    /**
     * Drop one - because it has been resurrected, because it has been written
     * into an egg and is now somebody's problem rather than the save's, or
     * because an operator said no.
     */
    public boolean forget(UUID horse) {
        if (byHorse.remove(horse) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    /**
     * <b>Charge {@code ticks} of one owner's time online against every horse of
     * theirs that is being kept</b>, and drop whatever that spends.
     *
     * <p>This is the whole of the expiry mechanism, and it runs from a slow
     * sweep over the players who are actually logged in - which is what makes
     * the clock theirs. {@code graceTicks} is passed in rather than read here so
     * that the setting is consulted at one place, and so a test can drive it.
     *
     * @return how many were dropped
     */
    public int spend(UUID owner, int ticks, int graceTicks) {
        int dropped = 0;
        boolean changed = false;
        var walk = byHorse.entrySet().iterator();
        while (walk.hasNext()) {
            Map.Entry<UUID, Wake> entry = walk.next();
            if (!entry.getValue().owner().equals(owner)) {
                continue;
            }
            Wake charged = entry.getValue().plusTicks(ticks);
            changed = true;
            if (charged.expired(graceTicks)) {
                walk.remove();
                dropped++;
            } else {
                entry.setValue(charged);
            }
        }
        if (changed) {
            setDirty();
        }
        return dropped;
    }

    /**
     * Drop everything already past its window, whoever owns it. Run when the
     * setting may have been lowered since the last sweep - at which point
     * entries that were inside a longer window, or were being kept for ever,
     * are suddenly outside it and would otherwise sit there until their owner
     * next logged in.
     *
     * @return how many were dropped
     */
    public int dropExpired(int graceTicks) {
        if (graceTicks <= 0) {
            return 0;
        }
        int before = byHorse.size();
        if (byHorse.values().removeIf(wake -> wake.expired(graceTicks))) {
            setDirty();
        }
        return before - byHorse.size();
    }

    /**
     * <b>Let the oldest deaths go until the store fits in {@code budgetBytes}</b>
     * - only as many as it takes, and nothing at all for a budget of {@code 0},
     * which is "no cap". Run from the same slow sweep as {@link #spend}, never
     * on a death.
     *
     * @return the horses dropped, so the sweep can log them
     */
    public List<Wake> trimTo(long budgetBytes) {
        List<Wake> dropped = new ArrayList<>();
        if (budgetBytes <= 0) {
            return dropped;
        }
        List<AfterlifeBudget.Entry<UUID>> entries = new ArrayList<>();
        for (Wake wake : byHorse.values()) {
            entries.add(wake.budgetEntry());
        }
        for (UUID horse : AfterlifeBudget.overflow(entries, budgetBytes)) {
            Wake gone = byHorse.remove(horse);
            if (gone != null) {
                dropped.add(gone);
            }
        }
        if (!dropped.isEmpty()) {
            setDirty();
        }
        return dropped;
    }

    /** What everything kept costs, by {@link Wake#bytes}. */
    public long totalBytes() {
        long sum = 0;
        for (Wake wake : byHorse.values()) {
            sum += wake.bytes();
        }
        return sum;
    }

    public int size() {
        return byHorse.size();
    }
}
