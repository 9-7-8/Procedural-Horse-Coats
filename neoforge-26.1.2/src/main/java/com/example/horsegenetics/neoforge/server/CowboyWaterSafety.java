package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.CowboyBrand;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * <b>A cowboy and his string keep out of the water, and a whirlpool does not
 * take them</b> (#30).
 *
 * <p>A village can build out over the sea on a few blocks of fill, and a cowboy
 * barn among it stands with deep water a step from the door. On seed 20261002 a
 * downward bubble column - a magma block on the sea floor, forty blocks down -
 * opens at the surface one block off the barn's edge. Whatever stepped in was
 * dragged to the bottom and burned on the magma ("discovered the floor was
 * lava"), or floated up under the fill and drowned; the hitch then hired the
 * next villager and the restock bred a fresh string onto the same spot, for
 * ever. Nothing in the logs says how they got into the water - a path across
 * it, the cowboy's panic, or one horse shoving another - so this answers both
 * halves (owner's call, 2026-10-04):
 * <ul>
 *   <li><b>No path into water.</b> On dry land their water malus is
 *       {@link #KEEP_OUT}, so no goal routes them through it. In the water it is
 *       vanilla's again, or a horse that was shoved in could not path out.</li>
 *   <li><b>Out of a whirlpool.</b> One caught in a downward bubble column anyway
 *       is moved to the nearest dry ground near its cowboy - or, for the man,
 *       near his home - and logged, with or without debug tools.</li>
 * </ul>
 * Only the dealer's own: the cowboy, and the horses still carrying his brand and
 * not yet sold. A sold horse, a wild one and a player's are left to the world,
 * and a sold one gets vanilla's malus back.
 */
@EventBusSubscriber
public final class CowboyWaterSafety {

    /** Ticks between looks. A drag column pulls about three blocks in this time. */
    static final int INTERVAL = 10;

    /** How far, in blocks, from its anchor a rescued mob may be put down. */
    static final int RESCUE_REACH = 12;

    /**
     * How far above or below the anchor a surface may be. Without it the nearest
     * column wins whatever its height, and the first gametest run put a horse
     * forty blocks down the side of the scratch platform.
     */
    static final int RESCUE_RISE = 4;

    /** {@code PathType.BLOCKED}'s malus: a node of this type is never used. */
    static final float KEEP_OUT = -1.0F;

    private CowboyWaterSafety() {
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.tickCount % INTERVAL != 0
                || !(entity instanceof Cowboy || entity instanceof AbstractHorse)
                || !(entity.level() instanceof ServerLevel level)
                || !entity.isAlive()) {
            return;
        }
        check((Mob) entity, level);
    }

    /**
     * Set the water malus, and rescue the mob if a whirlpool has it. Public for
     * the gametest that holds this.
     *
     * @return what the rescue did, for the log, or {@code null} if there was
     *         nothing to rescue
     */
    public static @Nullable String check(Mob mob, ServerLevel level) {
        boolean dealers = isDealers(mob);
        float now = mob.getPathfindingMalus(PathType.WATER);
        if (dealers && !mob.isInWater()) {
            if (now != KEEP_OUT) {
                mob.setPathfindingMalus(PathType.WATER, KEEP_OUT);
            }
        } else if (now == KEEP_OUT) {
            // In the water, or sold since: vanilla's cost again. Nothing else in
            // this mod writes the water malus, so KEEP_OUT can only be ours.
            mob.setPathfindingMalus(PathType.WATER, PathType.WATER.getMalus());
        }
        if (!dealers || !inWhirlpool(mob, level)) {
            return null;
        }
        String from = mob.blockPosition().toShortString();
        BlockPos dry = dryGroundNear(mob, level, anchor(mob, level));
        String moved = null;
        if (dry != null) {
            mob.snapTo(dry.getX() + 0.5, dry.getY(), dry.getZ() + 0.5, mob.getYRot(), mob.getXRot());
            mob.setDeltaMovement(Vec3.ZERO);
            mob.resetFallDistance();
            mob.getNavigation().stop();
            moved = "moved to " + dry.toShortString();
        }
        // Always logged, debug tools or not, like [clearance]: a cowboy's string
        // dying in the sea is otherwise a run of death lines with no cause.
        HorseGenetics.LOGGER.info("[whirlpool] {} {} caught in a downward bubble column at {} - {}",
                mob.getName().getString(), mob.getUUID(), from,
                moved == null ? "no dry ground near, left where it was" : moved);
        return moved;
    }

    /** The cowboy himself, or a horse he bred and still owns. */
    private static boolean isDealers(Mob mob) {
        if (mob instanceof Cowboy) {
            return true;
        }
        if (!(mob instanceof AbstractHorse horse) || horse.isTamed()) {
            return false;
        }
        CowboyBrand brand = horse.getData(ModAttachments.COWBOY_BRAND.get());
        return brand != null && brand.cowboy().isPresent();
    }

    /** Feet in a bubble column that pulls down - the one in the sea over magma. */
    private static boolean inWhirlpool(Mob mob, ServerLevel level) {
        BlockState state = level.getBlockState(mob.blockPosition());
        return state.is(Blocks.BUBBLE_COLUMN) && state.getValue(BubbleColumnBlock.DRAG_DOWN);
    }

    /**
     * Where to look for dry ground: the man's home, or a horse's cowboy if he is
     * loaded and dry himself. Otherwise the top of the mob's own column - the
     * water's surface, since the heightmap counts fluid - and not the mob
     * itself, which may be thirty blocks down the column by now.
     */
    private static BlockPos anchor(Mob mob, ServerLevel level) {
        if (mob instanceof Cowboy cowboy) {
            return cowboy.home().orElse(cowboy.blockPosition());
        }
        CowboyBrand brand = mob.getData(ModAttachments.COWBOY_BRAND.get());
        if (brand != null && brand.cowboy().isPresent()
                && level.getEntity(brand.cowboy().get()) instanceof Cowboy cowboy
                && cowboy.isAlive() && !cowboy.isInWater()) {
            return cowboy.blockPosition();
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, mob.blockPosition());
    }

    /**
     * The nearest spot within {@link #RESCUE_REACH} of {@code anchor}, in square
     * rings outward, where the mob fits. Each column is tried at the anchor's own
     * height first (so a man whose home is inside the barn is put back inside it,
     * not on its roof) and then at its surface, if that is within
     * {@link #RESCUE_RISE} of the anchor. Columns in unloaded chunks are skipped
     * rather than loaded.
     */
    static @Nullable BlockPos dryGroundNear(Mob mob, ServerLevel level, BlockPos anchor) {
        for (int r = 0; r <= RESCUE_REACH; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;   // only the ring at this distance
                    }
                    BlockPos column = anchor.offset(dx, 0, dz);
                    if (!level.hasChunkAt(column)) {
                        continue;
                    }
                    if (fits(mob, level, column)) {
                        return column;
                    }
                    BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                    if (top.getY() != column.getY() && Math.abs(top.getY() - anchor.getY()) <= RESCUE_RISE
                            && fits(mob, level, top)) {
                        return top;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Solid ground that is not magma, the mob's own box clear of blocks, and no
     * water within a block of it - a step back from the edge, not on it. The
     * margin reaches down to the ground's own level, because the sea beside a
     * shore is one block lower than the feet standing on it. Public for the
     * gametest, which asks it of the spot the rescue chose.
     */
    public static boolean fits(Mob mob, ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isSolidRender() || ground.is(Blocks.MAGMA_BLOCK) || !level.getFluidState(below).isEmpty()) {
            return false;
        }
        AABB box = mob.getDimensions(mob.getPose())
                .makeBoundingBox(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        return level.noCollision(mob, box)
                && !level.containsAnyLiquid(box.inflate(1.0, 0.0, 1.0).expandTowards(0.0, -1.0, 0.0));
    }
}
