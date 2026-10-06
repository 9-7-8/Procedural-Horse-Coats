package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.realm.RealmRestore;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseAfterlife;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.HorseWhereabouts;
import com.example.horsegenetics.neoforge.data.RealmBackups;
import com.example.horsegenetics.neoforge.data.StasisSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * <b>Back the horse realm up before changing it, and put its horses back
 * after.</b> (Owner, 2026-10-01: "before you do anything in the horse realm
 * which could kill horses on a live server, add a method that backs up their
 * horse realm first, and can teleport/resurrect all horses previously in the
 * realm after the change is done.")
 *
 * <h2>What a backup is</h2>
 * A copy of the realm's <b>entity region files</b> - {@code entities/*.mca}
 * under the realm's dimension folder - taken right after a flushing save, into
 * {@code <world>/horsegenetics/realm-backups/<name>/entities/}. That is every
 * horse in the field as a whole entity tag, UUID and attachments included,
 * <b>loaded or not</b>, and it costs no chunk loads: the realm is thousands of
 * horses across a field nobody has walked most of, and the census
 * ({@link com.example.horsegenetics.neoforge.data.HorseRealmSize}) knows who
 * they are but not what they are. Terrain is not copied - the change being
 * guarded against is usually a terrain change, and putting the old terrain back
 * would undo it. The newest {@value #KEEP} are kept.
 *
 * <h2>When one is taken</h2>
 * <ul>
 *   <li><b>Before every realm change in {@link #CHANGES}</b>, once per world,
 *       at server start - before any realm chunk has ticked. A change asks
 *       {@link #ready} before it touches a block, and does not run until its
 *       backup exists; a backup that fails leaves the change off, loudly.</li>
 *   <li><b>By hand</b>: {@code /horserealm backup}. A hand backup also covers
 *       any change still waiting for one.</li>
 * </ul>
 *
 * <h2>What a restore does</h2>
 * {@code /horserealm restore [name]} shows what it would do, and
 * {@code /horserealm restore <name> confirm} does it. Each horse in the backup
 * is judged by {@link RealmRestore} (in {@code common/}): dead or lost since
 * the backup is <b>raised</b> from the backup's copy at its old x/z, on the
 * surface; alive and in the realm is <b>moved to the surface if it is
 * stuck</b> in a block - now if it is loaded, when it next loads if not; out of
 * the realm or in a chamber is left alone. Raises are spread over ticks, and a
 * restore cut short by a restart is finished by running it again: everything
 * already raised is alive, so it is not raised twice.
 *
 * <p><b>Not verified in-game</b>, and the region-file reading is unverified API
 * use: {@link RegionFile} is vanilla's own reader, but this mod has not opened
 * one before. The entity tags are read without a DataFixer pass, which is
 * right only while the backup and the restore run on the same Minecraft
 * version - true for a change inside one release line, not across a port.
 */
@EventBusSubscriber
public final class RealmBackup {

    /** The realm lift: the floor moved from y=0 to {@link HorseRealm#GROUND_Y}. See {@link HorseRealmLift}. */
    public static final String LIFT = "floor-lift-y0-to-y64";

    /**
     * <b>Every realm change that could lose horses.</b> A new one goes here,
     * and its code calls {@link #ready} before it touches the realm. Named here
     * rather than registered from each change's own class, because a class that
     * is only an event subscriber may not have run its static initialiser by the
     * time the server starts, and a change that registered late would run with
     * no backup at all.
     */
    private static final List<String> CHANGES = List.of(LIFT);

    /**
     * How many backups are kept; the oldest goes when another is taken. Three, not ten (owner, #203): each is a
     * copy of every entity region file of a realm holding thousands of horses, and a world that already has more
     * loses the extras at its next backup.
     */
    static final int KEEP = 3;

    /** Raises per server tick. Each one may load the chunk the horse goes back to. */
    private static final int RAISES_PER_TICK = 2;

    private static final Pattern REGION = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

    /** One horse as a backup holds it. */
    public record Entry(UUID id, CompoundTag tag, Vec3 pos) {
    }

    /** A restore worked out, horse by horse, before anything is done. */
    public record Plan(String backup, ResourceKey<Level> dimension, Map<UUID, RealmRestore.Action> actions,
                       Map<UUID, Entry> entries) {

        public Map<RealmRestore.Action, Integer> counts() {
            Map<RealmRestore.Action, Integer> out = new EnumMap<>(RealmRestore.Action.class);
            for (RealmRestore.Action a : RealmRestore.Action.values()) {
                out.put(a, 0);
            }
            actions.values().forEach(a -> out.merge(a, 1, Integer::sum));
            return out;
        }

        /** The same plan for one horse only - the gametest's way of not raising every other test's horse. */
        public Plan only(UUID horse) {
            Map<UUID, RealmRestore.Action> a = new HashMap<>();
            if (actions.containsKey(horse)) {
                a.put(horse, actions.get(horse));
            }
            return new Plan(backup, dimension, a, entries);
        }
    }

    private record Raise(ResourceKey<Level> dimension, StasisSnapshot snapshot, Vec3 at) {
    }

    private static final Deque<Raise> RAISING = new ArrayDeque<>();
    private static int raised;
    private static int failed;

    /** Horses waiting on a rescue check that have loaded; looked at on their next tick. */
    private static final Set<UUID> CHECK_SOON = new HashSet<>();

    private RealmBackup() {
    }

    // ------------------------------------------------------------------
    // The gate
    // ------------------------------------------------------------------

    /** May this realm change run in this world? Only once a backup has been taken for it. */
    public static boolean ready(MinecraftServer server, String change) {
        return RealmBackups.get(server).covers(change);
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        // A count the last session stopped before finishing (#203) - the copy is on disk, so count it again.
        for (RealmBackups.Backup b : RealmBackups.get(server).all()) {
            if (!b.counted()) {
                countInBackground(server, b.name(), HorseRealm.REALM_LEVEL);
            }
        }
        List<String> missing = missing(server);
        if (missing.isEmpty()) {
            return;
        }
        ServerLevel realm = server.getLevel(HorseRealm.REALM_LEVEL);
        if (realm == null) {
            // No realm, no horses in it to lose.
            RealmBackups.get(server).cover(missing);
            return;
        }
        try {
            RealmBackups.Backup b = take(server, realm, String.join("+", missing));
            HorseGenetics.LOGGER.info("[realm-backup] took {} before: {} (horses counted in the background)",
                    b.name(), missing);
        } catch (IOException | RuntimeException e) {
            HorseGenetics.LOGGER.error("[realm-backup] could not back the horse realm up - these "
                    + "realm changes stay OFF until a backup succeeds (/horserealm backup): {}",
                    missing, e);
        }
    }

    private static List<String> missing(MinecraftServer server) {
        RealmBackups data = RealmBackups.get(server);
        return CHANGES.stream().filter(c -> !data.covers(c)).toList();
    }

    // ------------------------------------------------------------------
    // Taking one
    // ------------------------------------------------------------------

    /**
     * Back {@code level}'s horses up now. Covers every change still waiting for
     * a backup, since this is one taken before they run.
     *
     * <p>Flushes the level first ({@code save(null, true, false)}, what
     * {@code /save-all flush} does), so the files on disk hold every loaded
     * horse as it stands and nothing is still queued for writing. That is a
     * noticeable hitch on a big level, once.
     */
    public static RealmBackups.Backup take(MinecraftServer server, ServerLevel level, String reason)
            throws IOException {
        level.save(null, true, false);
        RealmBackups data = RealmBackups.get(server);
        String name = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + "-" + slug(reason);
        Path dir = backupsRoot(server).resolve(name);
        for (int n = 2; Files.exists(dir); n++) {
            dir = backupsRoot(server).resolve(name + "-" + n);
        }
        name = dir.getFileName().toString();
        Path entities = dir.resolve("entities");
        copyEntities(server, level.dimension(), entities);

        // The flush and the copy stay here, on the server thread: the copy has to be of the files as that save left
        // them. Counting the horses does not (#203) - it opened and parsed every chunk of every region file, on the
        // main thread, only to fill in a number for the listing. It reads the COPY, which nothing else writes, so it
        // cannot race the live world, and the backup is usable (plan/restore read the copy themselves) before it is
        // done.
        RealmBackups.Backup backup = new RealmBackups.Backup(name, System.currentTimeMillis(), reason,
                RealmBackups.Backup.UNCOUNTED);
        data.added(backup);
        data.cover(missing(server));
        while (data.all().size() > KEEP) {
            deleteTree(backupsRoot(server).resolve(data.dropOldest().name()));
        }
        countInBackground(server, name, level.dimension());
        return backup;
    }

    /**
     * Count a backup's horses off the server thread and hand the number back to it. A plain daemon thread rather than
     * one of Minecraft's pools, because this is one long file read, rare, and must not hold a pool thread the chunk
     * system wants. If the server stops first the number is simply never recorded; the next start counts it again.
     * A backup pruned while it was being counted makes the read fail or the record a no-op - both harmless.
     */
    private static void countInBackground(MinecraftServer server, String name, ResourceKey<Level> dimension) {
        Path entities = backupsRoot(server).resolve(name).resolve("entities");
        Thread counter = new Thread(() -> {
            int horses;
            try {
                horses = read(entities, dimension).size();
            } catch (IOException | RuntimeException e) {
                HorseGenetics.LOGGER.warn("[realm-backup] could not count the horses in {}", name, e);
                return;
            }
            // MinecraftServer is a BlockableEventLoop: execute() runs this on the server thread, where saved data
            // may be touched.
            server.execute(() -> {
                RealmBackups.get(server).counted(name, horses);
                HorseGenetics.LOGGER.info("[realm-backup] {} holds {} horses", name, horses);
            });
        }, "phc-realm-backup-count");
        counter.setDaemon(true);
        counter.start();
    }

    private static Path backupsRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(HorseGenetics.MOD_ID).resolve("realm-backups");
    }

    /** The level's own entity folder - LevelStorageSource.getDimensionPath, which is this call. */
    private static Path entityFolder(MinecraftServer server, ResourceKey<Level> dimension) {
        return DimensionType.getStorageFolder(dimension, server.getWorldPath(LevelResource.ROOT)).resolve("entities");
    }

    private static void copyEntities(MinecraftServer server, ResourceKey<Level> dimension, Path to)
            throws IOException {
        Files.createDirectories(to);
        Path from = entityFolder(server, dimension);
        if (!Files.isDirectory(from)) {
            return;     // a level nothing has ever been saved in
        }
        try (Stream<Path> files = Files.list(from)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                if (Files.isRegularFile(f)) {
                    Files.copy(f, to.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static String slug(String reason) {
        String s = reason.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "backup" : s.substring(0, Math.min(40, s.length()));
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    // ------------------------------------------------------------------
    // Reading one
    // ------------------------------------------------------------------

    /**
     * Every horse in a folder of entity region files, by id. Riders and
     * passengers are looked inside, so a horse carried by something is found
     * too.
     *
     * <p><b>Only ever on a copy.</b> {@link RegionFile} opens its file for
     * writing and may pad a short header, so it is never pointed at a live
     * level's own files - {@link #onDisk} copies first.
     */
    public static Map<UUID, Entry> read(Path entities, ResourceKey<Level> dimension) throws IOException {
        Map<UUID, Entry> out = new LinkedHashMap<>();
        if (!Files.isDirectory(entities)) {
            return out;
        }
        RegionStorageInfo info = new RegionStorageInfo("realm-backup", dimension, "entities");
        try (Stream<Path> files = Files.list(entities)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                Matcher m = REGION.matcher(f.getFileName().toString());
                if (!m.matches()) {
                    continue;
                }
                int rx = Integer.parseInt(m.group(1));
                int rz = Integer.parseInt(m.group(2));
                try (RegionFile region = new RegionFile(info, f, entities, false)) {
                    for (int lx = 0; lx < 32; lx++) {
                        for (int lz = 0; lz < 32; lz++) {
                            ChunkPos pos = new ChunkPos(rx * 32 + lx, rz * 32 + lz);
                            try (DataInputStream in = region.getChunkDataInputStream(pos)) {
                                if (in != null) {
                                    collect(NbtIo.read(in).getListOrEmpty("Entities"), out);
                                }
                            } catch (IOException | RuntimeException unreadable) {
                                HorseGenetics.LOGGER.warn("[realm-backup] skipped unreadable chunk {} in {}",
                                        pos, f, unreadable);
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private static final String HORSE_ID = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.HORSE).toString();

    private static void collect(ListTag entities, Map<UUID, Entry> out) {
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag tag = entities.getCompoundOrEmpty(i);
            if (HORSE_ID.equals(tag.getStringOr("id", ""))) {
                UUID id = tag.read("UUID", UUIDUtil.CODEC).orElse(null);
                if (id != null) {
                    ListTag p = tag.getListOrEmpty("Pos");
                    out.put(id, new Entry(id, tag,
                            new Vec3(p.getDoubleOr(0, 0), p.getDoubleOr(1, 0), p.getDoubleOr(2, 0))));
                }
            }
            collect(tag.getListOrEmpty("Passengers"), out);
        }
    }

    /** Which horses the level's own files hold right now - flushed, copied, read, the copy deleted. */
    private static Set<UUID> onDisk(MinecraftServer server, ServerLevel level) throws IOException {
        level.save(null, true, false);
        Path scratch = backupsRoot(server).resolve(".current-" + System.nanoTime());
        try {
            copyEntities(server, level.dimension(), scratch);
            return new HashSet<>(read(scratch, level.dimension()).keySet());
        } finally {
            deleteTree(scratch);
        }
    }

    // ------------------------------------------------------------------
    // Restoring
    // ------------------------------------------------------------------

    /** Work out what restoring this backup into {@code level} would do. Changes nothing but a save. */
    public static Plan plan(MinecraftServer server, ServerLevel level, String backup) throws IOException {
        Map<UUID, Entry> then = read(backupsRoot(server).resolve(backup).resolve("entities"), level.dimension());
        Set<UUID> now = onDisk(server, level);
        HorseWhereabouts whereabouts = HorseWhereabouts.get(server);
        Map<UUID, RealmRestore.Action> actions = new LinkedHashMap<>();
        for (UUID id : then.keySet()) {
            Entity live = liveHorse(server, id);
            HorseWhereabouts.Seen seen = whereabouts.lookup(id).orElse(null);
            actions.put(id, RealmRestore.decide(new RealmRestore.Facts(
                    live != null,
                    live != null && live.level() == level,
                    seen != null && seen.stasis(),
                    seen != null && seen.dead(),
                    seen == null || seen.dimension().equals(level.dimension()),
                    now.contains(id))));
        }
        return new Plan(backup, level.dimension(), actions, then);
    }

    private static Entity liveHorse(MinecraftServer server, UUID id) {
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e instanceof Horse && e.isAlive()) {
                return e;
            }
        }
        return null;
    }

    /**
     * Carry a plan out. Rescues of loaded horses happen now; unloaded ones are
     * remembered until they load; raises are queued and spread over ticks.
     *
     * @return how many loaded horses were moved out of a block
     */
    public static int execute(MinecraftServer server, Plan plan) {
        ServerLevel level = server.getLevel(plan.dimension());
        if (level == null) {
            return 0;
        }
        HorseAncestryData ancestry = HorseAncestryData.get(server);
        Set<UUID> later = new HashSet<>();
        int rescued = 0;
        for (Map.Entry<UUID, RealmRestore.Action> e : plan.actions().entrySet()) {
            UUID id = e.getKey();
            switch (e.getValue()) {
                case RESCUE -> {
                    if (level.getEntity(id) instanceof Horse h && rescue(h)) {
                        rescued++;
                    }
                }
                case RESCUE_LATER -> later.add(id);
                case RAISE -> {
                    Entry entry = plan.entries().get(id);
                    String name = ancestry.lookup(id).map(HorseRecord::displayName).orElse("");
                    RAISING.add(new Raise(plan.dimension(), new StasisSnapshot(name, id, entry.tag()), entry.pos()));
                }
                case LEAVE -> { }
            }
        }
        if (!later.isEmpty()) {
            RealmBackups.get(server).rescueOnLoad(later);
        }
        HorseGenetics.LOGGER.info("[realm-backup] restoring {}: {} to raise, {} moved out of blocks, "
                + "{} to check when they load", plan.backup(), RAISING.size(), rescued, later.size());
        return rescued;
    }

    /** Raises still queued. */
    public static int raisesPending() {
        return RAISING.size();
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (RAISING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (int n = 0; n < RAISES_PER_TICK && !RAISING.isEmpty(); n++) {
            Raise r = RAISING.poll();
            ServerLevel level = server.getLevel(r.dimension());
            // Checked again here, not only in the plan: a horse can come back
            // (an operator, a chunk loading) between the plan and its turn.
            if (level == null || HorseResurrection.alreadyAlive(server, r.snapshot())) {
                continue;
            }
            Horse horse = HorseResurrection.raise(level, landing(level, r.at()), 0f, r.snapshot());
            if (horse == null) {
                failed++;
                HorseGenetics.LOGGER.error("[realm-backup] could not raise {} ({}) from the backup",
                        r.snapshot().horseName(), r.snapshot().horseId());
                continue;
            }
            raised++;
            // It is back, so the afterlife's copy (an owned horse's) is spent:
            // otherwise /horseresurrect would offer a horse that is standing up.
            HorseAfterlife.get(server).forget(horse.getUUID());
        }
        if (RAISING.isEmpty()) {
            HorseGenetics.LOGGER.info("[realm-backup] restore finished: {} raised, {} failed", raised, failed);
            raised = 0;
            failed = 0;
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        // A restore cut short is finished by running it again; nothing here survives a restart.
        RAISING.clear();
        CHECK_SOON.clear();
        raised = 0;
        failed = 0;
    }

    /**
     * Where a raised horse stands: its old x/z, on the surface. Loads that
     * chunk if it is not loaded, on purpose (the horse is about to be there;
     * see issue #13). Outside the field - the field shrank, or it never was in
     * the realm - it goes to the arrival spot instead.
     */
    private static Vec3 landing(ServerLevel level, Vec3 at) {
        if (HorseRealm.isRealm(level) && !HorseRealm.inBounds(at.x, at.z, HorseRealm.radius(level.getServer()))) {
            return HorseRealm.arrivalSpot();
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at));
        return Vec3.atBottomCenterOf(top);
    }

    /**
     * Out of a block, onto the surface above it - only if it is <b>in</b> a
     * block. A horse under a roof is not stuck, and the surface above it is the
     * roof. Below the floor is HorseRealmRules' to catch, and it already does.
     */
    static boolean rescue(Horse horse) {
        if (!horse.isInWall()) {
            return false;
        }
        Vec3 to = landing((ServerLevel) horse.level(), horse.position());
        horse.teleportTo(to.x, to.y, to.z);
        horse.resetFallDistance();
        HorseGenetics.LOGGER.info("[realm-backup] moved {} out of a block to {}", horse.getUUID(), to);
        return true;
    }

    /** A horse a restore could not check because it was unloaded has loaded: look at it next tick. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Horse horse)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        RealmBackups data = RealmBackups.get(level.getServer());
        if (data.anyToRescue() && data.takeRescue(horse.getUUID())) {
            CHECK_SOON.add(horse.getUUID());
        }
    }

    /** Not inside the join event: moving an entity while the level is adding it is asking for trouble. */
    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (CHECK_SOON.isEmpty() || !(event.getEntity() instanceof Horse horse)
                || horse.level().isClientSide()) {
            return;
        }
        if (CHECK_SOON.remove(horse.getUUID())) {
            rescue(horse);
        }
    }
}
