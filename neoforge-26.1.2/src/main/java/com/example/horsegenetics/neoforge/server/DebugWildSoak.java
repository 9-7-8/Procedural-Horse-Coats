package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.WildCellLedger;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * <b>The wild-turnover soak</b> (owner, 2026-10-02: "can you try and test this, cause when I made a similar-ish
 * patch before, it led to lots and lots of game instability"). A measuring instrument, not gameplay: it does
 * nothing unless the JVM is started with {@code -Dhorsegenetics.wildSoak=<game days>} (the {@code wildSoakServer}
 * run sets it).
 *
 * <p>On a dedicated server: a stand-in player (the gametest harness's mock {@code ServerPlayer}, with an embedded
 * channel - it loads chunks because this calls {@code ServerChunkCache.move} for it) walks a route of twelve
 * waypoints, a leg every {@link #LEG_TICKS}, while the server sprints through the days. So cells are rolled and
 * re-rolled, herds expire while their chunks are unloaded and are removed when they load again, and some horses
 * are marked touched and handed to the realm. At home, in force-loaded chunks, a tamed horse and a kept
 * (persistent) untamed one are the controls: both must be there at the end. One {@code [soak]} line a game day
 * (tick cost, horse counts, the turnover's own counters), then a verdict, then the server halts.
 */
@EventBusSubscriber
final class DebugWildSoak {

    private DebugWildSoak() {
    }

    private static final int LEG_TICKS = 2_400;
    /** Longest a hop waits for its ground before going on anyway (counted in the verdict). */
    private static final int MAX_ARRIVE_TICKS = 1_200;
    private static final int DAY = 24_000;
    /** More loaded overworld horses than this at any report is called a runaway. */
    private static final int RUNAWAY = 600;

    private static int days;
    private static ServerLevel level;
    private static ServerPlayer walker;
    private static BlockPos home;
    private static final List<int[]> ROUTE = new ArrayList<>();
    private static int leg = -1;
    private static int tick;
    private static int legTick;
    private static boolean arriving;
    private static int slowArrivals;
    private static long tickStart;
    private static long[] dayTicks = new long[DAY];
    private static int dayFill;
    private static UUID tamedId;
    private static UUID keptId;
    private static final Set<UUID> MARKED = new HashSet<>();
    private static int maxHorses;
    private static long worstTick;
    private static boolean done;

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        String spec = System.getProperty("horsegenetics.wildSoak");
        if (spec == null || spec.isBlank()) {
            return;
        }
        days = Integer.parseInt(spec.trim());
        MinecraftServer server = event.getServer();
        level = server.overworld();
        home = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.getRespawnData().pos());
        walker = placeWalker(server, level);
        walker.getAbilities().invulnerable = true;
        walker.getAbilities().mayfly = true;
        walker.getAbilities().flying = true;
        walker.onUpdateAbilities();
        moveWalker(home.getX(), home.getY() + 1, home.getZ());

        int hx = home.getX() >> 4;
        int hz = home.getZ() >> 4;
        for (int x = hx - 1; x <= hx + 1; x++) {
            for (int z = hz - 1; z <= hz + 1; z++) {
                level.setChunkForced(x, z, true);
            }
        }
        // Each on its own column's ground, with no AI: the 2026-10-02 run lost a control off a cliff in its first
        // four seconds, which said nothing about the turnover. They still tick, so the scan still judges them.
        Horse tamed = EntityType.HORSE.spawn(level,
                level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, home.offset(3, 0, 0)),
                EntitySpawnReason.COMMAND);
        Horse kept = EntityType.HORSE.spawn(level,
                level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, home.offset(-3, 0, 0)),
                EntitySpawnReason.COMMAND);
        for (Horse control : new Horse[] {tamed, kept}) {
            if (control != null) {
                control.setNoAi(true);
                control.setInvulnerable(true);
            }
        }
        if (tamed != null) {
            tamed.tameWithName(walker);
            tamedId = tamed.getUUID();
        }
        if (kept != null) {
            kept.setPersistenceRequired();
            keptId = kept.getUUID();
        }
        ServerLevel realm = server.getLevel(HorseRealm.REALM_LEVEL);
        if (realm != null) {
            BlockPos at = HorseRealm.arrivalBlock();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    realm.setChunkForced((at.getX() >> 4) + x, (at.getZ() >> 4) + z, true);
                }
            }
        }
        // Twelve waypoints on two rings round home, visited in a shuffled but fixed order, so a place is
        // revisited every day or two: long enough for its herds to have aged out, often enough to see it.
        int[] order = {0, 5, 2, 9, 4, 11, 6, 1, 8, 3, 10, 7};
        for (int i : order) {
            double a = Math.toRadians(i * 30.0);
            int r = (i % 2 == 0) ? 350 : 750;
            ROUTE.add(new int[] {home.getX() + (int) (Math.cos(a) * r), home.getZ() + (int) (Math.sin(a) * r)});
        }
        int total = days * DAY;
        server.tickRateManager().requestGameToSprint(total + LEG_TICKS);
        HorseGenetics.LOGGER.info("[soak] ON - {} game days ({} ticks), home {}, {} waypoints, a leg every {} ticks;"
                        + " controls tamed={} kept={}; wild.despawn_days={}, topup={}",
                days, total, home.toShortString(), ROUTE.size(), LEG_TICKS, tamedId, keptId,
                com.example.horsegenetics.neoforge.ServerConfig.wildDespawnDays(),
                com.example.horsegenetics.neoforge.ServerConfig.wildTopUp());
    }

    /**
     * NeoForge's {@code FakePlayer}, added straight into the level: in {@code level.players()} and given player
     * chunk tickets, but never logged in, so no login event fires and every packet sent to it is swallowed. (The
     * gametest harness's mock player went through {@code placeNewPlayer} and its first mod payload threw: an
     * embedded channel negotiates no mod channels.)
     */
    private static ServerPlayer placeWalker(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "soak-walker"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        level.addNewPlayer(player);
        return player;
    }

    private static void moveWalker(double x, double y, double z) {
        // A FakePlayer's connection swallows teleports, so set the position itself; setPos moves it between
        // entity sections, and move() re-centres its chunk tickets.
        walker.snapTo(x, y, z, 0F, 0F);
        level.getChunkSource().move(walker);
    }

    @SubscribeEvent
    static void onPre(ServerTickEvent.Pre event) {
        if (walker != null && !done) {
            tickStart = System.nanoTime();
        }
    }

    @SubscribeEvent
    static void onPost(ServerTickEvent.Post event) {
        if (walker == null || done) {
            return;
        }
        long took = System.nanoTime() - tickStart;
        worstTick = Math.max(worstTick, took);
        dayTicks[dayFill++] = took;
        tick++;
        legTick++;
        MinecraftServer server = event.getServer();
        if (arriving) {
            // Sprinting starves the chunk system (no sleep between ticks for it to catch up), so a hop runs at
            // normal speed until the ground round the walker is generated and ticking entities - as it would be
            // for a player, who spends real minutes there - and only then sprints again.
            if (groundReady() || legTick >= MAX_ARRIVE_TICKS) {
                if (legTick >= MAX_ARRIVE_TICKS) {
                    slowArrivals++;
                }
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        walker.blockPosition());
                moveWalker(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5);
                arriving = false;
                legTick = 0;
                server.tickRateManager().requestGameToSprint(Math.max(1, days * DAY - tick + LEG_TICKS));
            }
        } else if (legTick == LEG_TICKS / 2) {
            markSomeTouched();
        } else if (legTick >= LEG_TICKS) {
            leg++;
            int[] w = ROUTE.get(leg % ROUTE.size());
            server.tickRateManager().stopSprinting();
            moveWalker(w[0] + 0.5, 300, w[1] + 0.5);
            arriving = true;
            legTick = 0;
        }
        if (dayFill == DAY) {
            report(event.getServer());
            dayFill = 0;
        }
        if (tick >= days * DAY) {
            finish(event.getServer());
        }
    }

    /** The walker's spot and four points 80 blocks out are all ticking entities. */
    private static boolean groundReady() {
        BlockPos at = walker.blockPosition();
        for (int[] d : new int[][] {{0, 0}, {80, 0}, {-80, 0}, {0, 80}, {0, -80}}) {
            if (!level.isPositionEntityTicking(at.offset(d[0], 0, d[1]))) {
                return false;
            }
        }
        return true;
    }

    /** Up to two untouched wild horses near the walker become "worked with", so the realm hand-off is exercised. */
    private static void markSomeTouched() {
        int marked = 0;
        for (Horse h : level.getEntitiesOfClass(Horse.class, walker.getBoundingBox().inflate(48),
                h -> h.isAlive() && !h.isTamed() && WildTurnover.stamped(h)
                        && !h.getPersistentData().getBooleanOr(WildTurnover.TOUCHED_KEY, false))) {
            h.getPersistentData().putBoolean(WildTurnover.TOUCHED_KEY, true);
            MARKED.add(h.getUUID());
            if (++marked >= 2) {
                break;
            }
        }
    }

    private static void report(MinecraftServer server) {
        long[] s = Arrays.copyOf(dayTicks, dayFill);
        Arrays.sort(s);
        double mean = Arrays.stream(s).average().orElse(0) / 1e6;
        int horses = 0;
        int stamped = 0;
        int entities = 0;
        for (var e : level.getAllEntities()) {
            entities++;
            if (e instanceof Horse h && h.isAlive()) {
                horses++;
                if (WildTurnover.stamped(h)) {
                    stamped++;
                }
            }
        }
        maxHorses = Math.max(maxHorses, horses);
        int realmHorses = 0;
        ServerLevel realm = server.getLevel(HorseRealm.REALM_LEVEL);
        if (realm != null) {
            for (var e : realm.getAllEntities()) {
                if (e instanceof Horse) {
                    realmHorses++;
                }
            }
        }
        Runtime rt = Runtime.getRuntime();
        HorseGenetics.LOGGER.info(String.format(Locale.ROOT,
                "[soak] day %d: tick mean %.3f p95 %.3f p99 %.3f max %.1f ms | overworld %d horses loaded (%d stamped),"
                        + " %d entities | realm %d horses loaded | removed %d, to realm %d (failed %d), cells counted %d,"
                        + " rolls %d, herds %d (%d horses) | ledger %d | marked touched %d | heap %d MB",
                tick / DAY, mean, s[(int) (s.length * 0.95)] / 1e6, s[(int) (s.length * 0.99)] / 1e6,
                s[s.length - 1] / 1e6, horses, stamped, entities, realmHorses,
                WildTurnover.removedTotal, WildTurnover.handedOffTotal, WildTurnover.handOffFailedTotal,
                WildTopUp.cellsCountedTotal, WildTopUp.rollsTotal, WildTopUp.herdsTotal, WildTopUp.horsesSpawnedTotal,
                WildCellLedger.get(level).size(), MARKED.size(), (rt.totalMemory() - rt.freeMemory()) >> 20));
    }

    private static void finish(MinecraftServer server) {
        done = true;
        List<String> fails = new ArrayList<>();
        if (!(level.getEntity(tamedId) instanceof Horse t) || !t.isAlive() || !t.isTamed()) {
            fails.add("the tamed control horse is gone or untamed");
        } else if (WildTurnover.stamped(t)) {
            fails.add("the tamed control horse carries a wild lifetime");
        }
        if (!(level.getEntity(keptId) instanceof Horse k) || !k.isAlive()) {
            fails.add("the kept (persistent, untamed) control horse is gone");
        } else if (WildTurnover.stamped(k)) {
            fails.add("the kept control horse was given a wild lifetime");
        }
        if (maxHorses > RUNAWAY) {
            fails.add("overworld horses peaked at " + maxHorses + " (runaway above " + RUNAWAY + ")");
        }
        if (WildTurnover.removedTotal == 0) {
            fails.add("no wild horse ever moved on");
        }
        if (WildTopUp.herdsTotal == 0) {
            fails.add("the top-up never spawned a herd");
        }
        if (!MARKED.isEmpty() && WildTurnover.handedOffTotal == 0) {
            fails.add(MARKED.size() + " horses were marked touched and none reached the realm");
        }
        // Where did the touched ones end up? Still out there (not yet due, or never reloaded), or in the realm.
        int inRealm = 0;
        int stillWild = 0;
        ServerLevel realm = server.getLevel(HorseRealm.REALM_LEVEL);
        for (UUID id : MARKED) {
            if (realm != null && realm.getEntity(id) instanceof Horse) {
                inRealm++;
            } else if (level.getEntity(id) instanceof Horse) {
                stillWild++;
            }
        }
        HorseGenetics.LOGGER.info("[soak] touched horses: {} marked, {} loaded in the realm now, {} loaded in the"
                        + " overworld now, the rest unloaded or (if the realm field let them wander) out of view",
                MARKED.size(), inRealm, stillWild);
        HorseGenetics.LOGGER.info("[soak] worst single tick {} ms; overworld horses peaked at {}; {} legs, {} of"
                        + " them went on before their ground was ready",
                String.format(Locale.ROOT, "%.1f", worstTick / 1e6), maxHorses, leg + 1, slowArrivals);
        HorseGenetics.LOGGER.info("[soak] RESULT {}{}", fails.isEmpty() ? "PASS" : "FAIL: ", String.join("; ", fails));
        HorseGenetics.LOGGER.info("[soak] done - halting the server");
        server.tickRateManager().stopSprinting();
        server.halt(false);
    }
}
