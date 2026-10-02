package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * <b>The horse realm's backups, and what each one was taken for.</b> The files
 * themselves are on disk under {@code <world>/horsegenetics/realm-backups/}; see
 * {@code server/RealmBackup}, which is the only thing that writes this.
 *
 * <p>Three things are kept:
 * <ul>
 *   <li>the backups, oldest first, each with the realm changes it was taken
 *       before;</li>
 *   <li>which realm changes are <b>covered</b> - a change does not run in a
 *       world until a backup has been taken for it;</li>
 *   <li>the horses a restore left to be checked <b>when they next load</b>,
 *       because they were in unloaded chunks when it ran.</li>
 * </ul>
 */
public final class RealmBackups extends SavedData {

    /**
     * One backup.
     *
     * @param name       its folder name under {@code realm-backups/}
     * @param takenAt    wall-clock milliseconds - a backup is an operator's
     *                   object, and they think in dates, not game ticks
     * @param reason     why it was taken: a change's id, or "by hand"
     * @param horses     how many horses it holds, for the listing
     */
    public record Backup(String name, long takenAt, String reason, int horses) {
        public static final Codec<Backup> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Backup::name),
                Codec.LONG.fieldOf("taken_at").forGetter(Backup::takenAt),
                Codec.STRING.fieldOf("reason").forGetter(Backup::reason),
                Codec.INT.fieldOf("horses").forGetter(Backup::horses)
        ).apply(i, Backup::new));
    }

    public static final Codec<RealmBackups> CODEC = RecordCodecBuilder.create(i -> i.group(
            Backup.CODEC.listOf().optionalFieldOf("backups", List.of()).forGetter(d -> d.backups),
            Codec.STRING.listOf().optionalFieldOf("covered", List.of())
                    .forGetter(d -> new ArrayList<>(d.covered)),
            UUIDUtil.CODEC.listOf().optionalFieldOf("rescue_on_load", List.of())
                    .forGetter(d -> new ArrayList<>(d.rescueOnLoad))
    ).apply(i, RealmBackups::new));

    public static final SavedDataType<RealmBackups> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "realm_backups"),
            RealmBackups::new,
            CODEC);

    private final List<Backup> backups = new ArrayList<>();
    private final Set<String> covered = new HashSet<>();
    private final Set<UUID> rescueOnLoad = new HashSet<>();

    private RealmBackups() {
    }

    private RealmBackups(List<Backup> backups, List<String> covered, List<UUID> rescueOnLoad) {
        this.backups.addAll(backups);
        this.covered.addAll(covered);
        this.rescueOnLoad.addAll(rescueOnLoad);
    }

    public static RealmBackups get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Oldest first. */
    public List<Backup> all() {
        return List.copyOf(backups);
    }

    public Optional<Backup> latest() {
        return backups.isEmpty() ? Optional.empty() : Optional.of(backups.get(backups.size() - 1));
    }

    public Optional<Backup> named(String name) {
        return backups.stream().filter(b -> b.name().equals(name)).findFirst();
    }

    public void added(Backup backup) {
        backups.add(backup);
        setDirty();
    }

    /** Forget the oldest - its folder is already gone. */
    public Backup dropOldest() {
        Backup gone = backups.remove(0);
        setDirty();
        return gone;
    }

    public boolean covers(String change) {
        return covered.contains(change);
    }

    public void cover(Iterable<String> changes) {
        for (String change : changes) {
            covered.add(change);
        }
        setDirty();
    }

    public void rescueOnLoad(Set<UUID> horses) {
        rescueOnLoad.addAll(horses);
        setDirty();
    }

    public boolean anyToRescue() {
        return !rescueOnLoad.isEmpty();
    }

    /** True once per horse: it was waiting, and now it has been looked at. */
    public boolean takeRescue(UUID horse) {
        if (rescueOnLoad.remove(horse)) {
            setDirty();
            return true;
        }
        return false;
    }
}
