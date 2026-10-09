package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.HorseWhereabouts;
import com.example.horsegenetics.neoforge.data.WildCellLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <b>What a live server has to tell us about an open issue, on one tag.</b>
 *
 * <p>{@link ActionTrace} is gated on debug tools, which a real server has off, so the only record of what
 * happens to players' horses is whatever is logged unconditionally - and until now that was whatever each
 * handler happened to print, each under its own prefix. The server's log is shared with a few hundred other
 * mods. Every line written here starts {@value #TAG}, so one grep lifts all of them out
 * ({@code procedures/review-server-logs.txt}), and the second word is the topic, which is an issue's subject.
 *
 * <h2>The rule for a line here</h2>
 * Always on, so it must be rare: a thing that happens to one horse, a few times a day, not a thing that
 * happens every tick. Anything that could repeat is rate-limited where it is written. A line names the horse
 * by UUID, because that is what the operator's own scripts log and what pairs our line with theirs.
 *
 * <p>A topic is removed with its issue: when the question is answered the call goes, and what was learned
 * moves to the owning page (hard rule 9).
 */
@EventBusSubscriber
public final class FieldLog {

    /** The one prefix. Never change it without changing the review procedure's grep. */
    public static final String TAG = "[phc-live]";

    private FieldLog() {
    }

    /** One line: the tag, the topic (an issue's subject), then what happened. */
    public static void log(String topic, String detail) {
        HorseGenetics.LOGGER.info("{} {} | {}", TAG, topic, detail);
    }

    /** {@code x.x, y.y, z.z in dimension} - where something is, to the tenth of a block. */
    public static String where(Entity entity) {
        return String.format("%.1f, %.1f, %.1f in %s", entity.getX(), entity.getY(), entity.getZ(),
                entity.level().dimension().identifier());
    }

    // ------------------------------------------------------------------
    // #35 - horses inside blocks
    // ------------------------------------------------------------------

    /** The tick of its life on which a loaded horse is checked once for standing inside blocks. */
    private static final int SETTLED_TICK = 10;

    /** How long, in game ticks, before the same suffocating horse is described again. */
    private static final long IN_WALL_REPEAT = 100L;

    /** When each suffocating horse was last described. Server thread only; cleared when it grows. */
    private static final Map<UUID, Long> IN_WALL_SAID = new HashMap<>();
    private static final int IN_WALL_SWEEP_ABOVE = 256;

    /**
     * <b>Was it buried on disk?</b> A horse whose box collides {@value #SETTLED_TICK} ticks after it joined was
     * saved inside those blocks, or put there as it was made. Ten ticks, not the first: a loaded horse is
     * vanilla-sized until its first tick ends ({@link HorseClearance#LOAD_GRACE_TICKS}). {@link HorseClearance}
     * may already have moved a horse that grew, in which case the box is clear and this says nothing.
     *
     * <p>One collision test per horse per load. It reads only the blocks the box overlaps, in the chunk the
     * horse is ticking in.
     */
    @SubscribeEvent
    static void onSettled(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || horse.tickCount != SETTLED_TICK
                || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (!level.noCollision(horse, horse.getBoundingBox())) {
            log("in-blocks", "LOADED INSIDE BLOCKS " + buried(horse, level));
        }
    }

    /**
     * <b>Where is a suffocating horse?</b> The line #35 has never had: the operator's rescue script logs only
     * where it dropped the horse. Lowest priority and not for a cancelled hit, so a hit the load grace or a
     * last stand refused is not reported as one. First hit, then every {@value #IN_WALL_REPEAT} ticks.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onInWall(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || !event.getSource().is(DamageTypes.IN_WALL)
                || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        Long said = IN_WALL_SAID.get(horse.getUUID());
        if (said != null && now - said < IN_WALL_REPEAT) {
            return;
        }
        if (IN_WALL_SAID.size() > IN_WALL_SWEEP_ABOVE) {
            IN_WALL_SAID.clear();
        }
        IN_WALL_SAID.put(horse.getUUID(), now);
        log("in-blocks", (said == null ? "SUFFOCATING " : "STILL SUFFOCATING ") + buried(horse, level));
    }

    /** Everything about a horse in a block that could say how it got there. */
    private static String buried(Horse horse, ServerLevel level) {
        AABB box = horse.getBoundingBox();
        double moved = Math.sqrt(horse.distanceToSqr(horse.xo, horse.yo, horse.zo));
        return horse.getUUID() + " (" + ActionTrace.describeShort(horse) + ") at " + where(horse)
                + String.format(" | scale %.2f, box %.2f x %.2f, hp %.1f/%.1f", horse.getScale(),
                        horse.getBbWidth(), horse.getBbHeight(), horse.getHealth(), horse.getMaxHealth())
                + " | in the world " + horse.tickCount + " ticks"
                + String.format(", moved %.2f this tick", moved)
                + (horse.onGround() ? ", on ground" : ", off ground")
                + " | eyes in " + blockAt(level, BlockPos.containing(horse.getEyePosition()))
                + ", box touches " + touching(level, box)
                + " | " + (horse.isTamed() ? "tamed" : "wild") + (horse.isBaby() ? ", foal" : "")
                + (horse.isLeashed() ? ", leashed" : "")
                + (horse.getVehicle() != null ? ", riding " + BuiltInRegistries.ENTITY_TYPE.getKey(
                        horse.getVehicle().getType()) : "")
                + (horse.isVehicle() ? ", ridden" : "");
    }

    /** Most blocks named in one line. */
    private static final int MAX_TOUCHING = 6;

    /** The solid blocks a box overlaps, each once. Never loads a chunk: a column that is not loaded is skipped. */
    private static String touching(ServerLevel level, AABB box) {
        Set<String> names = new LinkedHashSet<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
            for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
                if (!level.hasChunkAt(x, z)) {
                    continue;
                }
                for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (!state.getCollisionShape(level, pos).isEmpty() && names.size() < MAX_TOUCHING) {
                        names.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()) + "@" + pos.toShortString());
                    }
                }
            }
        }
        return names.isEmpty() ? "nothing solid" : String.join("; ", names);
    }

    private static String blockAt(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) ? BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString()
                : "an unloaded chunk";
    }

    // ------------------------------------------------------------------
    // #210 - how big the saved tables are
    // ------------------------------------------------------------------

    /** Once a real-time hour at twenty ticks a second. */
    private static final int TABLES_EVERY = 72_000;

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        tables(event.getServer(), "at start");
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        tables(event.getServer(), "at stop");
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % TABLES_EVERY == 0) {
            tables(event.getServer(), "hourly");
        }
    }

    /**
     * The three tables that are written whole on every dirty save, by row count - the term that grows with a
     * world's whole population, loaded or not. Counts only; nothing is walked.
     */
    private static void tables(MinecraftServer server, String when) {
        log("tables", when + ": ancestry " + HorseAncestryData.get(server).size() + " records, whereabouts "
                + HorseWhereabouts.get(server).size() + " horses, wild cells "
                + WildCellLedger.get(server.overworld()).size() + " stamps (overworld)");
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        IN_WALL_SAID.clear();
    }
}
