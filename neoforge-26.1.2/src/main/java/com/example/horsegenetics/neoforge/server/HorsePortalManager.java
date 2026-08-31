package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.block.HayPortalBlock;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Hay-bale portals. A rectangular hay-bale frame (nether-portal rules: inner
 * width 2..21, inner height 3..21, built vertically) lit by right-clicking any
 * of its hay blocks with a <b>golden carrot</b> fills with {@link HayPortalBlock}. Standing
 * in the portal plane long enough teleports you (see {@link PortalEventHandler}
 * for the dwell timers):
 *
 * <ul>
 *   <li>An overworld portal sends a player to a fresh private plot in the horse
 *       dimension ({@link DebugPenManager#enter}), remembering this portal as
 *       the return point.</li>
 *   <li>The horse dimension's portal sends a player - and any horse pushed
 *       into it - back to exactly the linked overworld portal. As a player
 *       leaves, their tamed horses come with them (see
 *       {@link DebugPenManager#evacuateTamedHorses}).</li>
 * </ul>
 *
 * <p><b>Not verified in-game:</b> cross-dimension entity teleport signature,
 * leash handling. Frame detection reworked to try every air-neighbour of the
 * clicked hay block as a flood-fill seed.
 */
public final class HorsePortalManager {

    private static final int MIN_INNER_W = 2;
    private static final int MAX_INNER_W = 21;
    private static final int MIN_INNER_H = 3;
    private static final int MAX_INNER_H = 21;

    /** (constCoord, minHoriz, minVert, width, height) of an inner rectangle in one vertical plane. */
    private record Rect(int constCoord, int minH, int minV, int w, int h) {}

    /**
     * Try to light a hay frame that {@code hayPos} is part of. Returns true and
     * fills the interior with portal blocks on success.
     */
    static boolean tryLightPortal(ServerLevel level, BlockPos hayPos) {
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            Rect rect = findFrame(level, hayPos, axis);
            if (rect != null) {
                fill(level, rect, axis);
                return true;
            }
        }
        return false;
    }

    private static Direction inPlaneHorizontal(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    /**
     * Look for a hay-bounded rectangular air pocket in the vertical plane that
     * passes through {@code hayPos} for the given portal axis. Tries each of the
     * four in-plane neighbours of the clicked hay block as a flood-fill seed,
     * so it works no matter which frame block the player right-clicked.
     */
    private static Rect findFrame(ServerLevel level, BlockPos hayPos, Direction.Axis axis) {
        Direction hor = inPlaneHorizontal(axis);
        int constCoord = axis == Direction.Axis.X ? hayPos.getZ() : hayPos.getX();
        for (Direction d : new Direction[] {hor, hor.getOpposite(), Direction.UP, Direction.DOWN}) {
            BlockPos seed = hayPos.relative(d);
            if (level.getBlockState(seed).isAir()) {
                Rect rect = floodAndValidate(level, seed, axis, hor, constCoord);
                if (rect != null) {
                    return rect;
                }
            }
        }
        return null;
    }

    private static Rect floodAndValidate(ServerLevel level, BlockPos seed, Direction.Axis axis,
                                         Direction hor, int constCoord) {
        Set<BlockPos> air = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        air.add(seed);
        queue.add(seed);
        int cap = MAX_INNER_W * MAX_INNER_H;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : new Direction[] {hor, hor.getOpposite(), Direction.UP, Direction.DOWN}) {
                BlockPos n = p.relative(d);
                if (air.contains(n)) {
                    continue;
                }
                BlockState s = level.getBlockState(n);
                if (s.isAir()) {
                    if (air.size() >= cap) {
                        return null; // opening too big / not enclosed - not a frame
                    }
                    air.add(n);
                    queue.add(n);
                } else if (!s.is(Blocks.HAY_BLOCK)) {
                    return null; // the opening touches something that isn't hay
                }
            }
        }

        int minH = Integer.MAX_VALUE, maxH = Integer.MIN_VALUE;
        int minV = Integer.MAX_VALUE, maxV = Integer.MIN_VALUE;
        for (BlockPos p : air) {
            int hc = axis == Direction.Axis.X ? p.getX() : p.getZ();
            minH = Math.min(minH, hc);
            maxH = Math.max(maxH, hc);
            minV = Math.min(minV, p.getY());
            maxV = Math.max(maxV, p.getY());
        }
        int w = maxH - minH + 1;
        int h = maxV - minV + 1;
        if (w < MIN_INNER_W || w > MAX_INNER_W || h < MIN_INNER_H || h > MAX_INNER_H) {
            return null;
        }
        if (air.size() != w * h) {
            return null; // opening isn't a filled rectangle
        }
        return new Rect(constCoord, minH, minV, w, h);
    }

    private static BlockPos cell(Rect rect, Direction.Axis axis, int hc, int y) {
        return axis == Direction.Axis.X
                ? new BlockPos(hc, y, rect.constCoord())
                : new BlockPos(rect.constCoord(), y, hc);
    }

    private static void fill(ServerLevel level, Rect rect, Direction.Axis axis) {
        BlockState portal = ModBlocks.HAY_PORTAL.get().defaultBlockState()
                .setValue(HayPortalBlock.AXIS, axis);
        for (int hc = rect.minH(); hc < rect.minH() + rect.w(); hc++) {
            for (int y = rect.minV(); y < rect.minV() + rect.h(); y++) {
                level.setBlock(cell(rect, axis, hc, y), portal, 2);
            }
        }
    }

    /**
     * Move {@code entity} through the portal at {@code portalPos}. From the
     * horse dimension: back to the linked overworld portal - and, for a
     * player, their tamed horses come too. From anywhere else: only players
     * act, and they enter a fresh plot.
     */
    static void teleportThroughPortal(Entity entity, ServerLevel portalLevel, BlockPos portalPos) {
        MinecraftServer server = portalLevel.getServer();
        if (server == null) {
            return;
        }

        if (portalLevel.dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
            DebugPenManager.Plot plot = DebugPenManager.plotContaining(portalPos.getX());
            ResourceKey<Level> destDim = plot != null ? plot.returnDim : Level.OVERWORLD;
            ServerLevel target = server.getLevel(destDim);
            if (target == null) {
                return;
            }
            BlockPos to = plot != null ? plot.returnPos : target.getRespawnData().pos();

            if (entity instanceof ServerPlayer player && plot != null) {
                boolean lastPerson = portalLevel.players().stream().noneMatch(p -> p != player);
                UUID onlyOwner = lastPerson ? null : player.getUUID();
                DebugPenManager.evacuateTamedHorses(portalLevel, plot, onlyOwner, target, to);
            }
            if (entity instanceof Mob mob && mob.isLeashed()) {
                mob.dropLeash();
            }
            if (entity instanceof AbstractHorse horse) {
                placeReturningHorse(horse, target, to, new ArrayList<>());
            } else {
                placeAt(entity, target, to);
            }
        } else if (entity instanceof ServerPlayer player) {
            DebugPenManager.enter(player, portalLevel.dimension(), portalPos.above());
        }
        // non-player entities in a non-debug portal: nothing (documented limitation)
    }

    static void placeAt(Entity entity, ServerLevel target, BlockPos to) {
        entity.teleportTo(target, to.getX() + 0.5, to.getY(), to.getZ() + 0.5,
                Set.of(), entity.getYRot(), entity.getXRot(), false);
    }

    // --- drop-in for returning horses ---

    private static final int RETURN_DROP_HEIGHT = 5;   // blocks above the portal anchor

    /**
     * Teleport {@code horse} into {@code dest} <b>above and beside</b>
     * {@code anchor} (the linked overworld portal) - never in the portal plane
     * itself - spread on a grid so a herd doesn't spawn inside itself, and give
     * it {@value PortalEventHandler#RETURN_INVULN_TICKS} ticks of invulnerability
     * so the drop can't hurt it. No terrain is carved. {@code used} just counts
     * how many have been placed this batch, for the grid offset.
     */
    static void placeReturningHorse(AbstractHorse horse, ServerLevel dest, BlockPos anchor, List<BlockPos> used) {
        int n = used.size();
        int gx = n % 3;
        int gz = n / 3;
        // +2 on both axes keeps every horse clear of a 1-block-thick portal
        // plane whichever way it's oriented; the grid then fans out from there.
        BlockPos spot = anchor.offset(2 + gx * 3, RETURN_DROP_HEIGHT, 2 + gz * 3);
        used.add(spot);
        placeAt(horse, dest, spot);
        PortalEventHandler.grantReturnInvulnerability(horse);
    }

    private HorsePortalManager() {
    }
}
