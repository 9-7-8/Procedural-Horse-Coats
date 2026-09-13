package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>The overnight watch: what happened in the horse dimension while nobody was
 * there.</b>
 *
 * <p>Asked for by the owner (2026-09-12): <i>"I'm going to leave the game and
 * the horse dimension running over night. Add as many time-reliant tests as
 * possible, focusing on those that don't require direct intervention from
 * me."</i> Half of that is the {@link DebugTestYard}'s pens. This is the other
 * half, and it is the half that decides whether the night was worth anything:
 * <b>a test nobody watches is only a test if something wrote down what it
 * did.</b>
 *
 * <h2>Why the yard needed this and {@link ActionTrace} was not enough</h2>
 * {@code ActionTrace} logs <i>things a person does</i> - a right-click, a
 * mount, a foal. Every test in the yard that matters overnight is the opposite
 * of that: a sapling that appeared, snow that stopped melting, eggs that piled
 * up to eight and then stopped, a population that did or did not plateau. None
 * of those is an event anybody fires. They are <b>differences between two
 * readings</b>, and nothing in the repo took readings.
 *
 * <h2>Two clocks</h2>
 * <ul>
 *   <li><b>The scan</b>, every {@link #SCAN_TICKS} - counts the watched blocks
 *       and creatures in each registered {@link Area} and logs <i>only what
 *       changed</i>. This is what turns "the dryad planted something" into a
 *       line with a time on it, to the nearest ten seconds, with no event hook
 *       for a gene that plants by writing the block directly.</li>
 *   <li><b>The census</b>, every {@link #CENSUS_TICKS} - logs every area in
 *       full whether it moved or not, plus the dimension's entity count and how
 *       fast the server is actually ticking. A flat line in the census is
 *       evidence; a gap in the log is not.</li>
 * </ul>
 *
 * <h2>And a pile of event hooks, because a delta cannot say why</h2>
 * The scan says the egg count went from five to eight. The item hooks say the
 * three eggs were laid at 03:14, 03:22 and 03:29 by a horse standing in the egg
 * pen - and, crucially, that two <i>earlier</i> ones despawned rather than
 * being picked up, which is the difference between a working cap and a cap that
 * never had to fire. Deltas find the interesting minute; the hooks explain it.
 *
 * <h2>The gate, and why it is the dimension and not just the config</h2>
 * {@link ServerConfig#debugTools()} <b>and</b> the debug-pen dimension. An item
 * hook that fires on every drop in a real world would bury the log in cobble
 * within a minute of mining, and this whole class is worthless the moment the
 * log stops being readable. The horse dimension is a closed box with a known
 * population, which is exactly what makes an unattended reading mean something.
 */
@EventBusSubscriber
public final class DebugWorldWatch {

    /** Everything this class writes, so a night greps out of the log in one go. */
    private static final String TAG = "[watch]";

    /** Ten seconds. How close to the event a block change can be dated. */
    private static final int SCAN_TICKS = 200;

    /** Two minutes. A full reading of everything, changed or not. */
    private static final int CENSUS_TICKS = 2400;

    /**
     * Lines per minute above which the event hooks shut up and start counting
     * instead.
     *
     * <p>The tool that finds a runaway must not be the thing that makes it
     * worse. An entity storm is one of the two failures the yard exists to
     * catch (the other is the breeding cap not holding), and the first symptom
     * of one is thousands of join events - which, logged one per line for eight
     * hours, is a log file nobody can open and a disk nobody asked for.
     */
    private static final int LINES_PER_MINUTE = 400;

    /**
     * Milliseconds per tick above which the census says the server is behind.
     *
     * <p><b>Not 50, which is the number that looks right and is wrong.</b> This
     * is wall-clock elapsed divided by ticks, and a server that is keeping up
     * sleeps out the rest of each tick - so a perfectly healthy dimension reads
     * <i>exactly</i> 50.0, every time, and cannot read less. Thresholding at 50
     * therefore leaves the verdict to floating-point noise: the first two real
     * censuses both printed 50.0 and only the second called it BEHIND.
     *
     * <p>A warning that fires on half the healthy readings is worse than no
     * warning, because it makes a genuine slowdown unremarkable - which is the
     * one thing this line exists to catch. Ten per cent of headroom, and the
     * good reading says what it is rather than being silent.
     */
    private static final double BEHIND_MS = 55.0;

    private DebugWorldWatch() {
    }

    // ------------------------------------------------------------------
    // Areas - what the scan reads
    // ------------------------------------------------------------------

    /**
     * One named box the scan reads every ten seconds.
     *
     * @param name    what the sign on the pen says, so a log line and a pen are
     *                the same thing without a coordinate lookup
     * @param box     the pen, usually two blocks tall: the floor and the air
     *                above it, which is where a sapling or a snow layer appears
     * @param blocks  the blocks worth counting here, chosen per pen - counting
     *                every block type in every pen is most of a chunk of noise
     *                to find three saplings
     * @param focus   a point to measure creature distances from, or null. The
     *                repel genes are a claim about <i>distance</i>, and a count
     *                inside a fenced pen cannot see one: penned animals cannot
     *                leave, so "four cows, all of them jammed against the far
     *                fence" and "four cows grazing round the horse" are the same
     *                number and opposite outcomes.
     */
    record Area(String name, AABB box, List<Block> blocks, @Nullable BlockPos focus) {}

    private static final List<Area> AREAS = new ArrayList<>();

    /** Last reading per area, keyed by name, for the delta. */
    private static final Map<String, Reading> LAST = new LinkedHashMap<>();

    private record Reading(int[] blocks, int horses, int otherMobs, int items,
                           double nearestOther) {}

    /** Forget every area. Called when a plot is torn down and rebuilt. */
    static void clearAreas() {
        AREAS.clear();
        LAST.clear();
    }

    /** Register a pen for the scan to read. Called by {@link DebugTestYard}. */
    static void watch(String name, AABB box, @Nullable BlockPos focus, Block... blocks) {
        AREAS.add(new Area(name, box, List.of(blocks), focus));
    }

    // ------------------------------------------------------------------
    // Lifecycle - force-loading, and the banner
    // ------------------------------------------------------------------

    /** The yard's chunks, forced so the night runs whether or not she stands in it. */
    private static final List<ChunkPos> FORCED = new ArrayList<>();

    private static long startedAtNanos;

    /**
     * <b>Start watching, and nail the yard's chunks open.</b>
     *
     * <p>The force-load is the difference between a test and a wasted night.
     * Minecraft only ticks what is near a player: blocks stop getting random
     * ticks, entities stop moving, and a gene on a 9 000-tick timer simply does
     * not fire. The yard is a hundred and ten blocks deep and its far end is
     * out of range of its near end, so <i>any</i> place to stand leaves some of
     * it asleep - which would read, in the morning, exactly like the gene at
     * that end being broken. Twenty-odd chunks is a cheap price for the
     * readings meaning what they say.
     *
     * <p>They are released again in {@link #stop}, and any left behind by a
     * crash are swept at server start - a forced chunk survives in the save,
     * and a dimension that quietly ticks for ever afterwards would be a bug
     * this tool caused rather than found.
     */
    static void start(ServerLevel level, AABB yard) {
        stop(level);
        clearAreas();
        startedAtNanos = System.nanoTime();
        int cxLo = SectionPos.blockToSectionCoord((int) yard.minX);
        int cxHi = SectionPos.blockToSectionCoord((int) yard.maxX);
        int czLo = SectionPos.blockToSectionCoord((int) yard.minZ);
        int czHi = SectionPos.blockToSectionCoord((int) yard.maxZ);
        for (int cx = cxLo; cx <= cxHi; cx++) {
            for (int cz = czLo; cz <= czHi; cz++) {
                if (level.setChunkForced(cx, cz, true)) {
                    FORCED.add(new ChunkPos(cx, cz));
                }
            }
        }
        HorseGenetics.LOGGER.info("{} watch ON - {} chunks force-loaded so the yard ticks with "
                        + "nobody standing in it; scan every {}s, full census every {}s. "
                        + "Everything below is prefixed {}.",
                TAG, FORCED.size(), SCAN_TICKS / 20, CENSUS_TICKS / 20, TAG);
    }

    /** Stop watching and let the chunks go. */
    static void stop(ServerLevel level) {
        for (ChunkPos pos : FORCED) {
            level.setChunkForced(pos.x(), pos.z(), false);
        }
        if (!FORCED.isEmpty()) {
            HorseGenetics.LOGGER.info("{} watch OFF - released {} forced chunks", TAG, FORCED.size());
        }
        FORCED.clear();
        clearAreas();
    }

    /**
     * Sweep forced chunks left in the save by a crash.
     *
     * <p>{@link #stop} runs on the way out of the dimension, and a crash has no
     * way out. Without this the debug dimension would tick a growing set of
     * chunks for the life of the world, which is a leak introduced by the leak
     * detector.
     */
    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        ServerLevel debug = event.getServer().getLevel(DebugPenManager.DEBUG_LEVEL);
        if (debug == null) {
            return;
        }
        long[] stale = debug.getForceLoadedChunks().toLongArray();
        for (long packed : stale) {
            debug.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
        }
        if (stale.length > 0) {
            HorseGenetics.LOGGER.info("{} released {} forced chunk(s) left in the horse dimension "
                    + "by a previous run", TAG, stale.length);
        }
    }

    // ------------------------------------------------------------------
    // The two clocks
    // ------------------------------------------------------------------

    private static long tick;
    private static long windowStartNanos = System.nanoTime();
    private static long windowStartTick;
    private static int suppressedThisMinute;
    private static int linesThisMinute;
    private static long minuteStartTick;
    private static int censusNumber;

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        tick++;
        if (tick - minuteStartTick >= 1200) {
            if (suppressedThisMinute > 0) {
                HorseGenetics.LOGGER.warn("{} {} event line(s) suppressed in the last minute - "
                        + "something is happening faster than this log can describe it",
                        TAG, suppressedThisMinute);
            }
            suppressedThisMinute = 0;
            linesThisMinute = 0;
            minuteStartTick = tick;
        }
        if (!ServerConfig.debugTools() || AREAS.isEmpty()) {
            return;
        }
        ServerLevel debug = event.getServer().getLevel(DebugPenManager.DEBUG_LEVEL);
        if (debug == null) {
            return;
        }
        boolean census = tick % CENSUS_TICKS == 0;
        if (census) {
            censusHeader(debug);
        }
        if (census || tick % SCAN_TICKS == 0) {
            for (Area area : AREAS) {
                scan(debug, area, census);
            }
        }
    }

    /**
     * The line that says whether the night is still healthy: how many entities
     * are in the dimension, and whether the server is keeping up.
     *
     * <p>The tick time is measured here rather than asked of the server,
     * because what matters for an overnight run is the <i>average over the last
     * two minutes</i> and not an instantaneous smoothed figure. Twenty ticks a
     * second is the target, so 50 ms per tick is the line: a census reading
     * above it means the dimension is behind, and the entity count on the same
     * line is almost always the reason.
     */
    private static void censusHeader(ServerLevel level) {
        censusNumber++;
        long now = System.nanoTime();
        long ticks = Math.max(1, tick - windowStartTick);
        double msPerTick = (now - windowStartNanos) / 1.0e6 / ticks;
        windowStartNanos = now;
        windowStartTick = tick;

        int horses = 0;
        int items = 0;
        int other = 0;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof Horse) {
                horses++;
            } else if (e instanceof ItemEntity) {
                items++;
            } else {
                other++;
            }
        }
        long upMinutes = (now - startedAtNanos) / 60_000_000_000L;
        // Game time and a day/night word, not a time-of-day number. 26.1.2 made
        // the clock per dimension (a dimension_type names a default_clock and
        // one that names none has no time of day at all), so ServerLevel no
        // longer answers getDayTime(). isBrightOutside() is the test every gene
        // condition in this mod already uses, which makes the census and the
        // genes agree about what "night" means by construction.
        long gameTime = level.getGameTime();
        HorseGenetics.LOGGER.info("{} ==== census {} | up {}m | game tick {} ({}) | {} ms/tick over "
                        + "the last {} ticks{} | entities {}: {} horses, {} items, {} other ====",
                TAG, censusNumber, upMinutes, gameTime,
                level.isBrightOutside() ? "day" : "NIGHT",
                String.format("%.1f", msPerTick), ticks,
                msPerTick > BEHIND_MS ? " - BEHIND, the server is not keeping up" : " (20 TPS is 50.0)",
                horses + items + other, horses, items, other);
        if (!SOUNDS.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : SOUNDS.entrySet()) {
                sb.append(sb.length() == 0 ? "" : ", ").append(e.getValue()).append("x ").append(e.getKey());
            }
            HorseGenetics.LOGGER.info("{} gene sounds since the last census: {}", TAG, sb);
            SOUNDS.clear();
        }
    }

    /**
     * Read one pen and say what moved.
     *
     * <p>On a census every area prints. On a scan only the ones that changed
     * do - eight hours of "DRYAD: nothing" every ten seconds is 2 880 lines
     * saying nothing, and the census already proves the watch was alive.
     */
    private static void scan(ServerLevel level, Area area, boolean census) {
        int[] counts = new int[area.blocks().size()];
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = (int) area.box().minX; x <= (int) area.box().maxX; x++) {
            for (int y = (int) area.box().minY; y <= (int) area.box().maxY; y++) {
                for (int z = (int) area.box().minZ; z <= (int) area.box().maxZ; z++) {
                    BlockState state = level.getBlockState(at.set(x, y, z));
                    for (int i = 0; i < counts.length; i++) {
                        if (state.is(area.blocks().get(i))) {
                            counts[i]++;
                        }
                    }
                }
            }
        }

        int horses = 0;
        int items = 0;
        int mobs = 0;
        double nearest = -1.0;
        // TALLER THAN THE BLOCK BOX, and it has to be. A pen's box is the floor
        // and the block above it, because that is where a sapling or a snow
        // layer appears - but an entity is only "in" an AABB if its own box
        // OVERLAPS, strictly, and a horse standing on the floor at gy+1 has its
        // feet exactly on that box's lid. So every pen read "horses 0" with two
        // horses in it, and the intimidating pen - whose entire test is the
        // distance to the nearest cow - would have reported "none in the pen"
        // all night. Caught in the first fifteen seconds of the first real
        // reading, by the watch contradicting a line of its own.
        for (Entity e : level.getEntities((Entity) null, area.box().inflate(0.0, 2.0, 0.0),
                e -> true)) {
            if (e instanceof Horse) {
                horses++;
                continue;
            }
            if (e instanceof ItemEntity) {
                items++;
                continue;
            }
            if (!(e instanceof LivingEntity)) {
                continue;
            }
            mobs++;
            if (area.focus() != null) {
                double d = Math.sqrt(e.distanceToSqr(area.focus().getX() + 0.5,
                        area.focus().getY() + 0.5, area.focus().getZ() + 0.5));
                if (nearest < 0 || d < nearest) {
                    nearest = d;
                }
            }
        }

        Reading now = new Reading(counts, horses, mobs, items, nearest);
        Reading was = LAST.put(area.name(), now);
        StringBuilder sb = new StringBuilder();
        boolean moved = false;
        for (int i = 0; i < counts.length; i++) {
            int delta = was == null ? 0 : counts[i] - was.blocks()[i];
            if (delta != 0) {
                moved = true;
            }
            if (delta != 0 || (census && counts[i] > 0)) {
                sb.append(sb.length() == 0 ? "" : ", ")
                        .append(counts[i]).append("x ")
                        .append(area.blocks().get(i).builtInRegistryHolder().key().identifier().getPath())
                        .append(delta == 0 ? "" : (delta > 0 ? " (+" : " (") + delta + ")");
            }
        }
        moved |= was != null && (was.horses() != horses || was.items() != items
                || was.otherMobs() != mobs);
        if (!census && !moved) {
            return;
        }
        String creatures = " | horses " + horses + delta(was == null ? horses : was.horses(), horses)
                + ", items " + items + delta(was == null ? items : was.items(), items)
                + (mobs > 0 || (was != null && was.otherMobs() > 0)
                        ? ", other creatures " + mobs + delta(was == null ? mobs : was.otherMobs(), mobs)
                        : "");
        String distance = area.focus() == null ? ""
                : " | nearest non-horse " + (nearest < 0 ? "none in the pen"
                        : String.format("%.1f blocks", nearest));
        HorseGenetics.LOGGER.info("{} {}{}{} | {}", TAG, area.name(), creatures, distance,
                sb.length() == 0 ? (area.blocks().isEmpty() ? "no blocks watched here"
                        : "none of its watched blocks are present") : sb);
    }

    private static String delta(int was, int now) {
        return was == now ? "" : (now > was ? " (+" : " (") + (now - was) + ")";
    }

    // ------------------------------------------------------------------
    // Event hooks - the "why" behind a delta
    // ------------------------------------------------------------------

    /**
     * Only fire in the horse dimension, and only with the tools on.
     *
     * <p>Both halves matter. The config is the ordinary gate every debug tool
     * here uses; the dimension is what keeps the hooks from being useless. An
     * item-drop line per dropped item is a readable record of a night in a
     * closed pen, and an unreadable one in a world where the player is mining.
     */
    private static boolean watching(Level level) {
        return ServerConfig.debugTools() && !level.isClientSide()
                && level.dimension().equals(DebugPenManager.DEBUG_LEVEL);
    }

    /** One event line, unless the log is already drowning. */
    private static void note(String what, String detail) {
        if (linesThisMinute >= LINES_PER_MINUTE) {
            suppressedThisMinute++;
            return;
        }
        linesThisMinute++;
        HorseGenetics.LOGGER.info("{} {} | {}", TAG, what, detail);
    }

    /**
     * <b>Something hit the ground.</b> The egg layer's whole test, and the
     * first sighting of any {@code produce} or {@code item_drop} verb actually
     * firing - both of which happen on a timer nobody watches.
     */
    @SubscribeEvent
    static void onItemJoin(EntityJoinLevelEvent event) {
        if (!watching(event.getLevel()) || !(event.getEntity() instanceof ItemEntity item)) {
            return;
        }
        note("item dropped", item.getItem().getCount() + "x "
                + item.getItem().getItem().builtInRegistryHolder().key().identifier()
                + " at " + item.blockPosition().toShortString() + inArea(item.blockPosition()));
    }

    /**
     * <b>And something left it.</b> Worth as much as the drop, and for a reason
     * that is easy to miss: a dropped item despawns after 6 000 ticks, and the
     * egg layer's interval is 4 000 to 14 000. So the accumulation cap it is
     * tested against may never bind at all with a single horse - the floor
     * clears itself faster than one horse can fill it - and a morning with
     * three eggs on the ground is evidence of the despawn, not of the cap.
     * Distinguishing the two needs the age at removal, which is here.
     */
    @SubscribeEvent
    static void onItemLeave(EntityLeaveLevelEvent event) {
        if (!watching(event.getLevel()) || !(event.getEntity() instanceof ItemEntity item)) {
            return;
        }
        int age = item.getAge();
        note("item gone", item.getItem().getCount() + "x "
                + item.getItem().getItem().builtInRegistryHolder().key().identifier()
                + " at " + item.blockPosition().toShortString()
                + " after " + age + " ticks - "
                + (age >= 5900 ? "DESPAWNED (the 6000-tick timer, not a cap)"
                        : "picked up, merged or destroyed"));
    }

    /** A non-horse creature arriving: the ward's zombies, a repelled cow's replacement. */
    @SubscribeEvent
    static void onCreatureJoin(EntityJoinLevelEvent event) {
        if (!watching(event.getLevel()) || !(event.getEntity() instanceof LivingEntity living)
                || living instanceof Horse) {
            return;
        }
        note("creature joined", living.getType().builtInRegistryHolder().key().identifier()
                + " at " + living.blockPosition().toShortString() + inArea(living.blockPosition()));
    }

    /**
     * Anything dying, not only horses. {@code ActionTrace} covers the horses
     * because their deaths are a genetics question; this covers the rest,
     * because "the cows are all gone" and "the cows were pushed into a corner"
     * are the two readings of an empty intimidating pen and only one of them is
     * the gene working.
     */
    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (!watching(event.getEntity().level()) || event.getEntity() instanceof Horse) {
            return;
        }
        note("creature died", event.getEntity().getType().builtInRegistryHolder().key().identifier()
                + " at " + event.getEntity().blockPosition().toShortString()
                + " from " + event.getSource().getMsgId());
    }

    /** Bone meal, by anybody. Nothing in the yard uses it - which is the point of watching. */
    @SubscribeEvent
    static void onBonemeal(BonemealEvent event) {
        if (!watching(event.getLevel())) {
            return;
        }
        note("bonemeal", "on " + event.getState().getBlock().builtInRegistryHolder().key().identifier()
                + " at " + event.getPos().toShortString()
                + (event.getPlayer() == null ? " (no player - a dispenser or a mod)" : " by a player"));
    }

    /**
     * <b>A sapling became a tree.</b> The far end of the dryad test: the gene
     * plants, and then vanilla grows it on random ticks - which only happen in
     * a loaded chunk, which is why this class force-loads the yard.
     */
    @SubscribeEvent
    static void onGrow(BlockGrowFeatureEvent event) {
        if (!(event.getLevel() instanceof Level level) || !watching(level)) {
            return;
        }
        note("tree grew", "at " + event.getPos().toShortString() + inArea(event.getPos()));
    }

    /** A block placed by something that is not a player - a gene, a mob, a falling block. */
    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || !watching(level)) {
            return;
        }
        note("block placed", event.getPlacedBlock().getBlock().builtInRegistryHolder().key()
                .identifier() + " at " + event.getPos().toShortString()
                + " by " + (event.getEntity() == null ? "nothing in particular"
                        : event.getEntity().getType().builtInRegistryHolder().key().identifier()));
    }

    /** An explosion in a pen of horses is worth a line whatever caused it. */
    @SubscribeEvent
    static void onExplode(ExplosionEvent.Detonate event) {
        if (!watching(event.getLevel())) {
            return;
        }
        note("EXPLOSION", event.getAffectedBlocks().size() + " blocks, "
                + event.getAffectedEntities().size() + " entities");
    }

    /** Ender echo's teleport, and anything else that moves without walking. */
    @SubscribeEvent
    static void onTeleport(EntityTeleportEvent event) {
        if (!watching(event.getEntity().level())) {
            return;
        }
        note("teleport", event.getEntity().getType().builtInRegistryHolder().key().identifier()
                + " " + BlockPos.containing(event.getPrev()).toShortString()
                + " -> " + BlockPos.containing(event.getTarget()).toShortString());
    }

    /**
     * <b>A gene played a sound.</b> Called from
     * {@code GeneAbilityHandler.maybeSound}, because there is no event for it.
     *
     * <p>Four sound genes ship with cooldowns chosen by guesswork and the open
     * question on all of them is "are these bearable in a herd" - which sounds
     * like a question only ears can answer, and is not: a herd that plays
     * eighty sounds in two minutes is unbearable by arithmetic, and that number
     * can be collected while everybody is asleep.
     *
     * <p><b>Counted, not logged.</b> A line per sound is right for a rare event
     * and wrong for this one: five horses meowing on a 300-tick timer is
     * fourteen lines a minute, which over a night is seven thousand lines that
     * drown the four that matter. The census prints the tally and resets it, so
     * the answer arrives as "meowing 36, singer 4 in the last two minutes" -
     * which is the number the question was actually asking for.
     */
    static void notePlayedSound(Horse horse, String sound) {
        if (!watching(horse.level())) {
            return;
        }
        SOUNDS.merge(sound, 1, Integer::sum);
    }

    /** Sounds played since the last census, by sound id. */
    private static final Map<String, Integer> SOUNDS = new LinkedHashMap<>();

    /**
     * <b>A gene fertilised something.</b> Called from
     * {@code GeneAbilityHandler.boneMeal}, and it is the only evidence that
     * test can produce.
     *
     * <p>The dryad's bone-meal allele refuses crops, and that refusal is
     * <i>invisible to observation</i>: a crop nobody fertilised still grows on
     * its own from random ticks, so "the wheat came up" says nothing either
     * way. The question is only answerable from the list of what the gene
     * <b>did</b> touch - and a run of these lines with no crop in it is the
     * pass. Vanilla's {@code BonemealEvent} does not fire for a direct call, so
     * without this there would be no record at all.
     */
    static void noteBoneMeal(Level level, BlockPos at, BlockState state) {
        if (!watching(level)) {
            return;
        }
        note("gene fertilised", state.getBlock().builtInRegistryHolder().key().identifier()
                + " at " + at.toShortString() + inArea(at));
    }

    /** Which pen, if any, a position is in - so a line reads without a map. */
    private static String inArea(BlockPos pos) {
        for (Area area : AREAS) {
            if (area.box().inflate(1.0).contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                return " [" + area.name() + "]";
            }
        }
        return "";
    }

    /**
     * Print every area now, whatever the clock says. {@code /testkit census}
     * calls this: eight hours is a long time to find out the watch was not
     * running, and one line of chat before bed is the whole check.
     */
    static void censusNow(ServerLevel level) {
        if (AREAS.isEmpty()) {
            HorseGenetics.LOGGER.warn("{} nothing is being watched - enter the horse dimension "
                    + "first; the yard registers its pens as it builds them", TAG);
            return;
        }
        censusHeader(level);
        for (Area area : AREAS) {
            scan(level, area, true);
        }
    }
}
