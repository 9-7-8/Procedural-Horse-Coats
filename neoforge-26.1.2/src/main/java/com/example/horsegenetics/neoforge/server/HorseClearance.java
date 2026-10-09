package com.example.horsegenetics.neoforge.server;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * <b>A horse that grows must not grow into the blocks around it</b> (#35).
 *
 * <p>Almost every horse in this mod is placed at one size and grows to another.
 * A natural spawn, the cowboy's string and the wild top-up all check the room
 * for a <i>vanilla</i> horse, and the founding tick writes the breed's real
 * {@code SCALE} up to twenty ticks later; a reloaded horse is rebuilt at scale
 * 1 and grows back to its saved size on its first tick; a foal grows up where
 * it stands. Every one of those ends in {@code Entity.refreshDimensions}, and
 * vanilla's own nudge there ({@code fudgePositionAfterSizeChange}) has three
 * holes, all silent:
 * <ul>
 *   <li>it only looks within the size difference, and when nothing there is
 *       clear it gives up and leaves the horse where it was;</li>
 *   <li>its fallback finds room for the new <i>width</i> at the old
 *       <i>height</i>, then keeps the new, taller box - under a two-block
 *       ceiling a horse of scale 1.32 or more has its eyes in the ceiling;</li>
 *   <li>it is skipped outright for a box over four blocks either way, which
 *       is every magical giant.</li>
 * </ul>
 * The horse then suffocates, a hit every half second, whenever its chunk is
 * ticking - which on the live server looked like healthy-seeming horses dying
 * within a second of a player loading their chunk.
 *
 * <p>So this listens for the growth itself ({@link EntityEvent.Size} fires
 * inside every {@code refreshDimensions}), and after that tick - vanilla's
 * nudge has had its turn - a horse whose box still collides is moved to the
 * nearest clear spot, and failing that, if its eyes are buried, lifted onto the
 * surface. The size event does nothing but note the id: it can fire while the
 * entity is being loaded, where touching the world is not safe.
 */
@EventBusSubscriber
public final class HorseClearance {

    /** How far, in blocks, a horse may be moved to get it out of a wall. */
    static final int CLEAR_REACH = 3;

    /** Horses that grew this tick (network id). Server thread only. */
    private static final IntOpenHashSet GREW = new IntOpenHashSet();

    private HorseClearance() {
    }

    @SubscribeEvent
    static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Horse horse) || horse.level().isClientSide()) {
            return;
        }
        EntityDimensions before = event.getOldSize();
        EntityDimensions after = event.getNewSize();
        if (after.width() > before.width() || after.height() > before.height()) {
            GREW.add(horse.getId());
        }
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (GREW.isEmpty() || !(event.getEntity() instanceof Horse horse)
                || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (GREW.remove(horse.getId()) && !level.noCollision(horse, horse.getBoundingBox())) {
            String from = horse.blockPosition().toShortString();
            String moved = clearOfBlocks(horse, level);
            if (moved == null && horse.isInWall()) {
                moved = liftToSurface(horse, level);
            }
            // Always logged, debug tools or not: this is the line #35 never had,
            // the place a horse got buried.
            // On the live-server tag (FieldLog); "[clearance]" stays in the text so an old grep still finds it.
            FieldLog.log("in-blocks", "[clearance] " + horse.getUUID() + " grew into blocks at " + from
                    + " (scale " + String.format("%.2f", horse.getScale()) + ") - "
                    + (moved == null ? "no clear spot near, left where it was" : moved));
        }
    }

    /**
     * Move a horse whose box collides to the nearest spot within
     * {@link #CLEAR_REACH} blocks where its whole box is clear, searching
     * outward in shells and upward before down. Nothing happens to a horse that
     * already fits.
     *
     * @return what was done, for the caller's log line, or {@code null} if the
     *         horse fits already or no clear spot was found
     */
    static String clearOfBlocks(Horse horse, ServerLevel level) {
        AABB box = horse.getBoundingBox();
        if (level.noCollision(horse, box)) {
            return null;
        }
        for (int r = 1; r <= CLEAR_REACH; r++) {
            for (int dy = 0; dy <= r; dy = dy <= 0 ? 1 - dy : -dy) {
                if (dy < -r) {
                    break;
                }
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.max(Math.abs(dx), Math.abs(dz)), Math.abs(dy)) != r) {
                            continue;   // only the shell at this distance; the inside was tried already
                        }
                        if (level.noCollision(horse, box.move(dx, dy, dz))) {
                            horse.snapTo(horse.getX() + dx, horse.getY() + dy, horse.getZ() + dz,
                                    horse.getYRot(), horse.getXRot());
                            return "moved " + dx + ", " + dy + ", " + dz + " to " + horse.blockPosition().toShortString();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * The last resort, for a horse too big for any spot near it: onto the
     * surface of its own column, if that is above it. Its chunk is the one it is
     * ticking in, so the heightmap read loads nothing.
     */
    private static String liftToSurface(Horse horse, ServerLevel level) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, horse.blockPosition());
        if (top.getY() <= horse.getY()) {
            return null;
        }
        horse.snapTo(horse.getX(), top.getY(), horse.getZ(), horse.getYRot(), horse.getXRot());
        horse.resetFallDistance();
        return "lifted to the surface at " + horse.blockPosition().toShortString();
    }

    /** How many ticks after it is built a horse cannot be hurt by a wall - see {@link #onLoadedIntoWall}. */
    static final int LOAD_GRACE_TICKS = 5;

    /**
     * <b>A horse just loaded is vanilla-sized for one tick, and a small one can be "in a wall" for it</b> (#214).
     *
     * <p>A loaded horse is built at the entity type's size and its saved {@code SCALE} lands at the end of its
     * first tick, after {@code LivingEntity.baseTick} has already asked {@code isInWall}. A horse of scale 0.68
     * saved flush against a wall therefore has, for that one tick, a vanilla-sized eye box reaching into the
     * stone, and was hit for a point on every chunk load (the yard's RELOAD pen, 2026-10-08). So in-wall damage
     * in a horse's first {@value #LOAD_GRACE_TICKS} ticks is not real. A horse that truly is buried is hit from
     * then on exactly as before.
     */
    @SubscribeEvent
    static void onLoadedIntoWall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Horse horse && horse.tickCount <= LOAD_GRACE_TICKS
                && event.getSource().is(DamageTypes.IN_WALL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Horse horse) {
            GREW.remove(horse.getId());
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        GREW.clear();
    }
}
