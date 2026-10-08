package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * <b>Row AW west: RELOAD - does a chunk load bury a horse that was saved standing clear?</b> (2026-10-08, #35).
 *
 * <p>On the live server horses that are healthy on disk turn up inside blocks seconds after a player loads their
 * chunk, with no {@code [clearance]} line - so no growth was reported, or it was reported in a tick where the box
 * was still clear. The owner parked #35 and asked for it to be tested on the next run-tests; this is the question
 * a pen can answer with nobody there: <i>is the reload itself the path?</i>
 *
 * <p><b>What a reload does to a horse, read in the 26.1.2 sources.</b> {@code EntityStorage.loadEntities} builds
 * each entity with {@code EntityType.loadEntitiesRecursive(..., EntitySpawnReason.LOAD)} and hands the list to
 * {@code ServerLevel.addLegacyChunkEntities}. The entity is constructed at its type's size; its saved
 * {@code SCALE} attribute is read back dirty and applied on its first tick, and this mod's founding tick
 * re-resolves the record's scale onto it as well. Either lands in {@code Entity.refreshDimensions}, whose
 * {@code fudgePositionAfterSizeChange} may move the horse. The pen does exactly that to one horse at a time: saves
 * it ({@code saveWithoutId}, the call {@code HorseStasisHandler} makes), discards it, and five ticks later rebuilds
 * it with {@code EntityType.loadEntityRecursive(HORSE, tag, level, LOAD, ...)} and adds it with
 * {@code addLegacyChunkEntities}. UNVERIFIED: that those two calls are all a real chunk load does to the entity -
 * the storage's data-fixer pass and the section manager's visibility changes are not reproduced.
 *
 * <p><b>The fixture is the tightest spot a horse can legitimately stand in:</b> flush in the corner of two
 * six-high stone walls, under a stone ceiling at the lowest whole block that clears its hitbox (never under two).
 * Any outward nudge in two of the four directions, or upward, puts it in stone. Each horse is given forty ticks to
 * be founded and take its real scale, is set there with its AI off (so nothing but a resize or
 * {@code HorseClearance} can move it), is checked to be clear, and is then reloaded and watched for
 * {@value #WATCH_TICKS} ticks. A horse wider than {@value #MAX_W} blocks or taller than {@value #MAX_H} does not
 * fit the fixture and is skipped by name.
 *
 * <p><b>Two verdicts.</b>
 * <ul>
 *   <li>{@link #CLEAR} - PASS: no reloaded horse's box collided, no horse was in a wall, and none lost health, with
 *       at least {@value #MIN_TESTED} tested and the scales spanning both sides of 1 (one at or under 0.9, one at or
 *       over 1.2). FAIL: any did, each named with its scale, where it was put and where it ended. INCONCLUSIVE:
 *       too few horses or too narrow a spread.</li>
 *   <li>{@link #SIZE} - PASS: every reloaded horse bigger than vanilla (scale over 1.02) fired at least one
 *       growing {@code EntityEvent.Size} after its reload, which is the only thing {@code HorseClearance} listens
 *       for. FAIL: one regrew without it - a growth the fix for #35 cannot see.</li>
 * </ul>
 * <b>A PASS here rules the reload out and does not close #35</b>: only the live server's log can. About three
 * minutes for the fourteen horses; deadline {@value #DEADLINE} ticks.
 */
@EventBusSubscriber
final class DebugYardReload {

    private DebugYardReload() {
    }

    static final String CLEAR = "RELOAD - a horse saved flush in a corner under the lowest ceiling it fits is still"
            + " clear of the blocks after a chunk-load rebuild (#35)";
    static final String SIZE = "RELOAD - a reloaded horse bigger than vanilla fires a growing EntityEvent.Size, so"
            + " HorseClearance sees its regrowth (#35)";

    private static final int SETTLE_TICKS = 40;
    private static final int WATCH_TICKS = 60;
    private static final long DEADLINE = 12_000L;
    private static final int MIN_TESTED = 8;
    private static final double MAX_W = 6.0;
    private static final double MAX_H = 6.0;

    /** Genotype codes, one horse each; an empty code is a random founder. */
    private static final String[] CODES = {
            "", "", "", "",
            "horsegenetics.lcorl=L/L", "horsegenetics.lcorl=L/L", "horsegenetics.lcorl=L/L",
            "horsegenetics.hmga2=p/p", "horsegenetics.hmga2=p/p", "horsegenetics.hmga2=p/p",
            "horsegenetics.body_size=Big/n", "horsegenetics.body_size=Big/Big",
            "horsegenetics.body_size=Small/n", "horsegenetics.body_size=Small/Small"};

    private static int run;
    private static @Nullable UUID watching;
    private static int growthEvents;

    private static final class State {
        final int run;
        final int gy;
        final int cornerX;      // first free block east of the north-south wall
        final int cornerZ;      // first free block south of the east-west wall
        final int holdX;
        final int holdZ;
        final List<String> bad = new ArrayList<>();
        final List<String> unseen = new ArrayList<>();
        final List<String> skipped = new ArrayList<>();
        final StringBuilder each = new StringBuilder();
        int tested;
        int bigger;
        double minScale = 99;
        double maxScale;

        State(int run, int gy, int cornerX, int cornerZ, int holdX, int holdZ) {
            this.run = run;
            this.gy = gy;
            this.cornerX = cornerX;
            this.cornerZ = cornerZ;
            this.holdX = holdX;
            this.holdZ = holdZ;
        }
    }

    static void build(ServerLevel level, int gy, int x0, int z0) {
        int thisRun = ++run;
        watching = null;
        try {
            DebugYardClockwork.expect(CLEAR);
            DebugYardClockwork.expect(SIZE);
            int x1 = x0 + 19;
            int z1 = z0 + 12;
            DebugTestYard.fencedPlot(level, gy, x0, x1, z0, z1);
            DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                    List.of("RELOAD", "saved in a corner,", "rebuilt as a chunk", "load does: buried?"));
            YardPens.register(gy, x0, x1, z0, z1, "AW RELOAD");
            // The corner: a wall along x0+2 and a wall along z0+2, six high, eight long.
            BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
            for (int i = 2; i <= 9; i++) {
                for (int y = gy + 1; y <= gy + 7; y++) {
                    level.setBlock(new BlockPos(x0 + 2, y, z0 + i), stone, 3);
                    level.setBlock(new BlockPos(x0 + i, y, z0 + 2), stone, 3);
                }
            }
            State s = new State(thisRun, gy, x0 + 3, z0 + 3, x0 + 14, z0 + 8);
            long start = level.getGameTime();
            DebugYardHerd.after(level, 60, () -> next(level, s, 0, start));
            HorseGenetics.LOGGER.info("[Debug] test yard: row AW west built (RELOAD, {} horses)", CODES.length);
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: RELOAD failed to build", e);
        }
    }

    @SubscribeEvent
    static void onSize(EntityEvent.Size event) {
        if (watching == null || event.getEntity().level().isClientSide()
                || !watching.equals(event.getEntity().getUUID())) {
            return;
        }
        EntityDimensions before = event.getOldSize();
        EntityDimensions after = event.getNewSize();
        if (after.width() > before.width() || after.height() > before.height()) {
            growthEvents++;
        }
    }

    private static void next(ServerLevel level, State s, int i, long start) {
        if (s.run != run) {
            return;
        }
        if (i >= CODES.length || level.getGameTime() - start > DEADLINE) {
            judge(s, i);
            return;
        }
        Horse h = DebugYardUnattended.horse(level, s.gy, s.holdX + 0.5, s.holdZ + 0.5, Sex.FEMALE, CODES[i], true,
                "RELOAD " + i);
        if (h == null) {
            s.skipped.add(i + " (did not spawn)");
            DebugYardHerd.after(level, 5, () -> next(level, s, i + 1, start));
            return;
        }
        DebugYardHerd.after(level, SETTLE_TICKS, () -> place(level, s, i, start, h));
    }

    private static void place(ServerLevel level, State s, int i, long start, Horse h) {
        if (s.run != run) {
            return;
        }
        double w = h.getBbWidth();
        double ht = h.getBbHeight();
        if (!h.isAlive() || !HorseRecords.hasRealRecord(h) || w > MAX_W || ht > MAX_H) {
            s.skipped.add(String.format(Locale.ROOT, "%d %s (scale %.2f, %.2f x %.2f%s)", i, code(i), h.getScale(),
                    w, ht, h.isAlive() ? HorseRecords.hasRealRecord(h) ? ", too big for the corner" : ", no record"
                            : ", dead"));
            h.discard();
            DebugYardHerd.after(level, 5, () -> next(level, s, i + 1, start));
            return;
        }
        // The ceiling: clear the last horse's, then the lowest whole block over this one's box.
        int headroom = Math.max(2, (int) Math.ceil(ht + 1.0e-4));
        for (int x = s.cornerX; x <= s.cornerX + 6; x++) {
            for (int z = s.cornerZ; z <= s.cornerZ + 6; z++) {
                for (int y = s.gy + 1; y <= s.gy + 7; y++) {
                    level.setBlock(new BlockPos(x, y, z), y == s.gy + 1 + headroom
                            ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        h.getNavigation().stop();
        h.setNoAi(true);
        h.setDeltaMovement(Vec3.ZERO);
        h.snapTo(s.cornerX + w / 2.0, s.gy + 1, s.cornerZ + w / 2.0, 0.0F, 0.0F);
        DebugYardHerd.after(level, 5, () -> reload(level, s, i, start, h, headroom));
    }

    private static void reload(ServerLevel level, State s, int i, long start, Horse h, int headroom) {
        if (s.run != run) {
            return;
        }
        String who = String.format(Locale.ROOT, "%d %s scale %.2f (%.2f x %.2f under %d)", i, code(i), h.getScale(),
                h.getBbWidth(), h.getBbHeight(), headroom);
        if (!h.isAlive() || !level.noCollision(h, h.getBoundingBox()) || h.isInWall()
                || h.getHealth() < h.getMaxHealth()) {
            // The fixture's fault, not the reload's: it was not standing clear to begin with.
            s.skipped.add(who + " - not clear before the reload (collides "
                    + !level.noCollision(h, h.getBoundingBox()) + ", in wall " + h.isInWall() + ", health "
                    + h.getHealth() + "/" + h.getMaxHealth() + ")");
            h.discard();
            DebugYardHerd.after(level, 5, () -> next(level, s, i + 1, start));
            return;
        }
        Vec3 saved = h.position();
        float scale = h.getScale();
        float health = h.getHealth();
        CompoundTag tag;
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(h.problemPath(), HorseGenetics.LOGGER)) {
            TagValueOutput out = TagValueOutput.createWithContext(reporter, h.registryAccess());
            h.saveWithoutId(out);
            tag = out.buildResult();
        }
        UUID id = h.getUUID();
        h.discard();
        DebugYardHerd.after(level, 5, () -> {
            if (s.run != run) {
                return;
            }
            watching = id;
            growthEvents = 0;
            Entity loaded = EntityType.loadEntityRecursive(EntityType.HORSE, tag, level, EntitySpawnReason.LOAD,
                    e -> e);
            if (!(loaded instanceof Horse back)) {
                s.skipped.add(who + " - did not load back");
                DebugYardHerd.after(level, 5, () -> next(level, s, i + 1, start));
                return;
            }
            String atLoad = String.format(Locale.ROOT, "%.2f x %.2f", back.getBbWidth(), back.getBbHeight());
            level.addLegacyChunkEntities(Stream.of(back));
            watch(level, s, i, start, back, who, saved, scale, health, atLoad, new Seen(), 0);
        });
    }

    private static final class Seen {
        int collided = -1;      // the first tick it did
        int inWall = -1;
        float lowest = Float.MAX_VALUE;
        double furthest;
    }

    private static void watch(ServerLevel level, State s, int i, long start, Horse h, String who, Vec3 saved,
                              float scale, float health, String atLoad, Seen seen, int tick) {
        DebugYardHerd.after(level, 1, () -> {
            if (s.run != run) {
                return;
            }
            int t = tick + 1;
            if (!h.isAlive()) {
                s.bad.add(who + " - gone " + t + " ticks after the reload (" + h.getRemovalReason() + ")");
                watching = null;
                next(level, s, i + 1, start);
                return;
            }
            if (seen.collided < 0 && !level.noCollision(h, h.getBoundingBox())) {
                seen.collided = t;
            }
            if (seen.inWall < 0 && h.isInWall()) {
                seen.inWall = t;
            }
            seen.lowest = Math.min(seen.lowest, h.getHealth());
            seen.furthest = Math.max(seen.furthest, h.position().distanceTo(saved));
            if (t < WATCH_TICKS) {
                watch(level, s, i, start, h, who, saved, scale, health, atLoad, seen, t);
                return;
            }
            boolean hurt = seen.lowest < health - 0.01F;
            boolean regrew = Math.abs(h.getScale() - scale) < 0.01F;
            String line = String.format(Locale.ROOT, "%s: box at load %s, after %d ticks scale %.2f at %s (%.3f from"
                            + " where it was saved, furthest %.3f), collided %s, in wall %s, health %.1f -> lowest"
                            + " %.1f, growing Size events %d", who, atLoad, t, h.getScale(),
                    h.blockPosition().toShortString(), h.position().distanceTo(saved), seen.furthest,
                    seen.collided < 0 ? "never" : "at tick " + seen.collided,
                    seen.inWall < 0 ? "never" : "at tick " + seen.inWall, health, seen.lowest, growthEvents);
            ActionTrace.log("test yard", "RELOAD " + line);
            s.tested++;
            s.minScale = Math.min(s.minScale, scale);
            s.maxScale = Math.max(s.maxScale, scale);
            if (seen.collided >= 0 || seen.inWall >= 0 || hurt || !regrew) {
                s.bad.add(line);
            }
            if (scale > 1.02F) {
                s.bigger++;
                if (growthEvents == 0) {
                    s.unseen.add(who);
                }
            }
            s.each.append(s.each.length() == 0 ? "" : ", ").append(String.format(Locale.ROOT, "%.2f", scale));
            watching = null;
            h.discard();
            DebugYardHerd.after(level, 5, () -> next(level, s, i + 1, start));
        });
    }

    private static void judge(State s, int reached) {
        String base = s.tested + " horse(s) reloaded of " + CODES.length + " (" + reached + " reached), scales ["
                + s.each + "]" + (s.skipped.isEmpty() ? "" : "; skipped: " + String.join(" / ", s.skipped));
        if (!s.bad.isEmpty()) {
            DebugYardClockwork.verdict(CLEAR, false, s.bad.size() + " not clear after the reload: "
                    + String.join(" // ", s.bad) + " | " + base);
        } else if (s.tested < MIN_TESTED || s.minScale > 0.9 || s.maxScale < 1.2) {
            DebugYardClockwork.inconclusive(CLEAR, "none buried, but too few horses or too narrow a spread of scale"
                    + " to say (want " + MIN_TESTED + ", one at or under 0.9 and one at or over 1.2) | " + base);
        } else {
            DebugYardClockwork.verdict(CLEAR, true, "none collided, none in a wall, none hurt, each back at its saved"
                    + " scale | " + base + " | per-horse lines: grep 'RELOAD '");
        }
        if (s.bigger == 0) {
            DebugYardClockwork.inconclusive(SIZE, "no reloaded horse was bigger than vanilla | " + base);
        } else {
            DebugYardClockwork.verdict(SIZE, s.unseen.isEmpty(), s.bigger + " reloaded horse(s) over scale 1.02; "
                    + (s.unseen.isEmpty() ? "each fired a growing EntityEvent.Size after its reload"
                    : s.unseen.size() + " regrew with NO growing EntityEvent.Size: " + String.join(" / ", s.unseen)));
        }
    }

    private static String code(int i) {
        return CODES[i].isEmpty() ? "random" : CODES[i].substring("horsegenetics.".length());
    }
}
