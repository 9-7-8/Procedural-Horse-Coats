package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.wild.TopUpPlan;
import com.example.horsegenetics.common.wild.TopUpPlan.Decision;
import com.example.horsegenetics.common.wild.TopUpPlan.Settings;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.WildCellLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Thin ground refills</b> - the other half of {@link WildTurnover}. Once a
 * game day, the first time a player comes near each fixed cell of
 * {@code wild.topup_cell_chunks} chunks, a cell holding fewer than
 * {@code wild.topup_minimum} wild horses gets a whole new herd. The rules
 * (cells, the daily stamp, the ecology signature) are {@link TopUpPlan}; the
 * stamps live in {@link WildCellLedger}.
 *
 * <h2>The herd is a natural spawn</h2>
 * Each horse is spawned with {@code EntitySpawnReason.NATURAL}, through
 * {@code EntityType.spawn}, which fires {@code FinalizeSpawnEvent}. So
 * {@link BreedSpawnHandler} refuses it where the breed settings allow nothing,
 * marks it and stamps its lifetime, and {@link HerdManager} founds the pack a
 * second later exactly as it founds a natural one - breed, band, sex and the
 * magical-herd chance. Nothing here picks a breed. The pack's size is the
 * biome's own natural count (its horse spawn entry's min..max), and its members
 * stand within a few blocks of each other so the founder sees one clump.
 *
 * <h2>Where a herd may appear</h2>
 * A spot in the cell, in a chunk that is loaded and ticking entities (no chunk is
 * ever loaded for this), that passes the horse's own spawn placement rule -
 * vanilla's, ORed with {@code HorseSpawnGround}'s breed floors - in a biome with
 * a horse spawn entry. At least {@link TopUpPlan#MIN_PLAYER_DISTANCE} blocks from
 * every player (vanilla's own floor), and not in plain view of any of them: a
 * spot a player is facing with nothing in the way is refused. And at least
 * {@link HerdManager#HERD_RADIUS} from any horse already there, or the new pack
 * would join that herd and be more of the same. A dozen tries; if none fits, the
 * cell is stamped anyway and waits for tomorrow.
 *
 * <h2>Cost</h2>
 * Once a second, a bounded number of cells ({@link #CELLS_PER_PASS}); a cell
 * already stamped today costs a map lookup and is not counted. The count is one
 * entity query over the cell's loaded sections. Nothing scales with the number
 * of horses in the world.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class WildTopUp {

    /** How often the players' cells are walked. Once a second. */
    private static final int INTERVAL_TICKS = 20;

    /** At most this many cells are counted (and maybe rolled) per pass, across every level. */
    private static final int CELLS_PER_PASS = 4;

    /** Spots tried per rolled cell before giving up for the day. */
    private static final int SPOT_TRIES = 12;

    /** Tries per pack member around the first horse. */
    private static final int MEMBER_TRIES = 4;

    /** How far a pack member stands from the first horse, at most, along each axis. */
    private static final int PACK_SPREAD = 4;

    /** A spot in a player's view within this many blocks counts as "in sight". */
    private static final double SIGHT_RANGE = 128.0;

    /** Cosine of the half-angle of a player's view cone (about 60 degrees either side). */
    private static final double VIEW_COS = 0.5;

    // Server thread only. The signature is computed once per server start, per
    // settings (a config reload that changes the cell size or minimum recomputes it).
    private static Settings signedFor;
    private static long signature;

    private WildTopUp() {
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL_TICKS != 0) {
            return;
        }
        Settings settings = ServerConfig.wildTopUp();
        if (settings.off()) {
            return;
        }
        long sig = signature(settings);
        int budget = CELLS_PER_PASS;
        for (ServerLevel level : server.getAllLevels()) {
            if (budget <= 0) {
                return;
            }
            if (WildTurnover.excluded(level) || level.players().isEmpty()
                    || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
                continue;
            }
            budget = visit(level, settings, sig, budget);
        }
    }

    /** Walk every active player's nearby cells in {@code level}. Returns the budget left. */
    static int visit(ServerLevel level, Settings settings, long sig, int budget) {
        long today = TopUpPlan.dayOf(level.getGameTime());
        WildCellLedger ledger = WildCellLedger.get(level);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            for (long cell : TopUpPlan.cellsNear(player.getX(), player.getZ(), settings.playerRadius(),
                    settings.cellChunks())) {
                if (TopUpPlan.current(ledger.stampOf(cell, today), today, sig)) {
                    continue;
                }
                if (budget-- <= 0) {
                    return 0;
                }
                rollCell(level, cell, settings, today, sig, ledger);
            }
        }
        return budget;
    }

    /** Count one cell and, if it is thin, spawn a herd in it. Always stamps it. */
    public static int rollCell(ServerLevel level, long cell, Settings settings, long today, long sig, WildCellLedger ledger) {
        int counted = countWild(level, cell, settings);
        Decision decision = TopUpPlan.decide(ledger.stampOf(cell, today), today, sig, counted, settings);
        int spawned = 0;
        if (decision == Decision.ROLL) {
            Herd herd = spawnHerd(level, cell, settings);
            spawned = herd.spawned();
            if (herd.horseCountry()) {
                DebugAnnounce.log("Wild", TopUpPlan.rolledLine(cell, herd.biome(), counted, spawned, herd.where()));
            }
        }
        if (decision != Decision.CURRENT) {
            ledger.stamp(cell, today, sig);
        }
        return spawned;
    }

    // ------------------------------------------------------------------
    // Counting
    // ------------------------------------------------------------------

    private static AABB cellBox(ServerLevel level, long cell, Settings settings) {
        int x0 = TopUpPlan.cellMinBlock(TopUpPlan.cellX(cell), settings.cellChunks());
        int z0 = TopUpPlan.cellMinBlock(TopUpPlan.cellZ(cell), settings.cellChunks());
        int size = settings.cellBlocks();
        return new AABB(x0, level.getMinY(), z0, x0 + size, level.getMaxY() + 1, z0 + size);
    }

    /**
     * The untamed wild horses standing in the cell's loaded chunks: stamped ones,
     * and members of a wild herd (a foal born into a band). Not a led horse, not a
     * tamed one; the realm is never counted because it is never visited.
     */
    static int countWild(ServerLevel level, long cell, Settings settings) {
        return level.getEntitiesOfClass(Horse.class, cellBox(level, cell, settings),
                h -> h.isAlive() && !h.isTamed() && !h.isLeashed()
                        && (WildTurnover.stamped(h) || h.getData(ModAttachments.HORSE_CARE.get()).inWildHerd()))
                .size();
    }

    // ------------------------------------------------------------------
    // Spawning
    // ------------------------------------------------------------------

    /**
     * What one roll did.
     *
     * @param horseCountry whether any spot tried was in a biome horses spawn in at all -
     *                     a roll over open ocean is not worth a debug line
     */
    record Herd(int spawned, String biome, String where, boolean horseCountry) {
    }

    static Herd spawnHerd(ServerLevel level, long cell, Settings settings) {
        RandomSource random = level.getRandom();
        int x0 = TopUpPlan.cellMinBlock(TopUpPlan.cellX(cell), settings.cellChunks());
        int z0 = TopUpPlan.cellMinBlock(TopUpPlan.cellZ(cell), settings.cellChunks());
        int size = settings.cellBlocks();
        boolean dark = level.isDarkOutside();
        boolean horseCountry = false;
        String lastBiome = "?";
        for (int attempt = 0; attempt < SPOT_TRIES; attempt++) {
            int x = x0 + random.nextInt(size);
            int z = z0 + random.nextInt(size);
            if (!level.isPositionEntityTicking(new BlockPos(x, level.getSeaLevel(), z))) {
                continue;
            }
            BlockPos spot = groundAt(level, x, z);
            Holder<Biome> biome = level.getBiome(spot);
            String biomeId = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
            MobSpawnSettings.SpawnerData entry = horseEntry(biome, random);
            if (entry == null) {
                continue;
            }
            horseCountry = true;
            lastBiome = biomeId;
            if (!Breeds.anythingMaySpawn(biomeId, dark)
                    || !standable(level, spot) || !outOfSight(level, spot) || horsesNear(level, spot)) {
                continue;
            }
            int want = TopUpPlan.packSize(entry.minCount(), entry.maxCount(), new NeoRng(random));
            int spawned = spawnPack(level, spot, want, random).size();
            if (spawned > 0) {
                return new Herd(spawned, biomeId, spot.toShortString(), true);
            }
        }
        return new Herd(0, lastBiome, "", horseCountry);
    }

    /** The first horse at {@code spot}, the rest within {@link #PACK_SPREAD} blocks of it. */
    public static List<Horse> spawnPack(ServerLevel level, BlockPos spot, int want, RandomSource random) {
        List<Horse> pack = new ArrayList<>();
        Horse first = EntityType.HORSE.spawn(level, spot, EntitySpawnReason.NATURAL);
        if (first == null) {
            return pack;   // refused (BreedSpawnHandler, or another mod's FinalizeSpawnEvent)
        }
        pack.add(first);
        for (int i = 1; i < want; i++) {
            for (int t = 0; t < MEMBER_TRIES; t++) {
                int x = spot.getX() + random.nextInt(PACK_SPREAD * 2 + 1) - PACK_SPREAD;
                int z = spot.getZ() + random.nextInt(PACK_SPREAD * 2 + 1) - PACK_SPREAD;
                if (!level.isPositionEntityTicking(new BlockPos(x, spot.getY(), z))) {
                    continue;
                }
                BlockPos at = groundAt(level, x, z);
                if (Math.abs(at.getY() - spot.getY()) > PACK_SPREAD || !standable(level, at) || !outOfSight(level, at)) {
                    continue;
                }
                Horse member = EntityType.HORSE.spawn(level, at, EntitySpawnReason.NATURAL);
                if (member != null) {
                    pack.add(member);
                    break;
                }
            }
        }
        return pack;
    }

    /** A horse spawn entry from the biome's own creature list, or {@code null} if horses do not spawn there. */
    private static MobSpawnSettings.SpawnerData horseEntry(Holder<Biome> biome, RandomSource random) {
        List<MobSpawnSettings.SpawnerData> horses = new ArrayList<>();
        for (var weighted : biome.value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap()) {
            if (weighted.value().type() == EntityType.HORSE) {
                horses.add(weighted.value());
            }
        }
        return horses.isEmpty() ? null : horses.get(random.nextInt(horses.size()));
    }

    /**
     * Where a horse would stand at column {@code (x, z)} - vanilla's own
     * {@code NaturalSpawner.getTopNonCollidingPos}, which is private: the top of
     * the horse's heightmap, and under a ceiling (the Nether) the first floor
     * below it.
     */
    public static BlockPos groundAt(ServerLevel level, int x, int z) {
        int top = level.getHeight(SpawnPlacements.getHeightmapType(EntityType.HORSE), x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        if (level.dimensionType().hasCeiling()) {
            do {
                pos.move(Direction.DOWN);
            } while (!level.getBlockState(pos).isAir() && pos.getY() > level.getMinY());
            do {
                pos.move(Direction.DOWN);
            } while (level.getBlockState(pos).isAir() && pos.getY() > level.getMinY());
        }
        return SpawnPlacements.getPlacementType(EntityType.HORSE).adjustSpawnPosition(level, pos.immutable());
    }

    /** The horse's own placement and spawn rules (vanilla's, ORed with HorseSpawnGround's), and room to stand. */
    private static boolean standable(ServerLevel level, BlockPos pos) {
        return SpawnPlacements.isSpawnPositionOk(EntityType.HORSE, level, pos)
                && SpawnPlacements.checkSpawnRules(EntityType.HORSE, level, EntitySpawnReason.NATURAL, pos,
                level.getRandom())
                && level.noCollision(EntityType.HORSE.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    /**
     * No player is within {@link TopUpPlan#MIN_PLAYER_DISTANCE}, and none within
     * {@link #SIGHT_RANGE} is facing the spot with a clear line to it. Vanilla
     * checks only the distance; a herd popping into an open field a player is
     * looking at is the one thing the owner asked never to happen.
     */
    static boolean outOfSight(ServerLevel level, BlockPos spot) {
        Vec3 head = new Vec3(spot.getX() + 0.5, spot.getY() + 1.5, spot.getZ() + 0.5);
        double min2 = TopUpPlan.MIN_PLAYER_DISTANCE * TopUpPlan.MIN_PLAYER_DISTANCE;
        for (ServerPlayer p : level.players()) {
            Vec3 eye = p.getEyePosition();
            double d2 = eye.distanceToSqr(head);
            if (d2 < min2) {
                return false;
            }
            if (d2 > SIGHT_RANGE * SIGHT_RANGE) {
                continue;
            }
            Vec3 toSpot = head.subtract(eye).normalize();
            if (p.getViewVector(1.0F).dot(toSpot) < VIEW_COS) {
                continue;   // behind or beside them
            }
            HitResult hit = level.clip(new ClipContext(eye, head, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
            if (hit.getType() == HitResult.Type.MISS) {
                return false;   // in front of them, nothing in the way
            }
        }
        return true;
    }

    /** Any horse within a herd's reach of the spot - a pack there would join it, not be a new herd. */
    private static boolean horsesNear(ServerLevel level, BlockPos spot) {
        return !level.getEntitiesOfClass(Horse.class, new AABB(spot).inflate(HerdManager.HERD_RADIUS),
                Horse::isAlive).isEmpty();
    }

    // ------------------------------------------------------------------
    // The signature
    // ------------------------------------------------------------------

    /** {@link TopUpPlan#signature} for this server's breeds, computed once per settings. */
    static long signature(Settings settings) {
        if (!settings.equals(signedFor)) {
            signature = TopUpPlan.signature(Breeds.all(), Breeds.spawnSettings().feral(), settings);
            signedFor = settings;
        }
        return signature;
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        signedFor = null;
        signature = 0L;
    }
}
