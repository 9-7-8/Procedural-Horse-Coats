package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.block.HayPortalBlock;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
 * Horse portals. A rectangular cobblestone frame (nether-portal rules: inner
 * width 2..21, inner height 3..21, built vertically) lit by right-clicking any
 * of its hay blocks with a <b>golden carrot</b> fills with {@link HayPortalBlock}. Standing
 * in the portal plane long enough teleports you (see {@link PortalEventHandler}
 * for the dwell timers):
 *
 * <ul>
 *   <li>An overworld portal sends a player - or a horse led into it - to the
 *       public {@link HorseRealm}, remembering this portal as the way home.</li>
 *   <li>A realm portal sends a player back to exactly that portal, and
 *       <b>nothing else with them</b> ({@link HorseRealm#leave}). Any of the
 *       realm's hundred exits does; they are interchangeable.</li>
 *   <li>The debug corridor's portal sends a player - and any horse pushed into
 *       it - back to the overworld portal its plot remembers. <b>Neither exit
 *       gathers up horses any more</b> (owner's call): a horse comes home only
 *       by being led or ridden through the frame. The corridor warns about what
 *       is left behind, because its plot is torn down (see
 *       {@link DebugPenManager#countOwnedTamedHorses}); the realm keeps what you
 *       leave. That dimension is reached by F6 only now, not by a hay portal.</li>
 * </ul>
 *
 * <p><b>Not verified in-game:</b> cross-dimension entity teleport signature,
 * leash handling. Frame detection reworked to try every air-neighbour of the
 * clicked hay block as a flood-fill seed.
 */
public final class HorsePortalManager {

    /**
     * <b>What a portal frame is made of.</b> One answer, in one place, because
     * the question is asked from five: this class's flood fill, the portal
     * block's own "am I still enclosed" ray, the realm's two frame builders, and
     * the right-click that lights one. It was a bare
     * {@code state.is(Blocks.HAY_BLOCK)} in each of them, and that is exactly the
     * shape of thing that drifts.
     *
     * <h2>Why it is not hay any anymore</h2>
     * <b>A horse ate a portal.</b> A hay bale is food in this mod - the largest
     * single meal in the game, which a hungry horse will cross a paddock for and
     * eat <i>out of the world</i> ({@code HayBales}, {@code DietFoods}) - so a
     * frame built out of hay is a frame a horse standing next to it will
     * eventually demolish, collapsing the portal and stranding whoever was
     * through it. That is not a bug in the portal; it is two correct features
     * meeting, and the fix is for the frame to be something inedible. Cobblestone
     * is the obvious one: early, infinite, and the block a player has most of.
     *
     * <p>The <b>golden carrot still lights it</b>. The carrot was never the part
     * a horse could eat off the wall, and it is the half of the ritual worth
     * keeping - see {@code PortalEventHandler}.
     *
     * <p>The portal block itself is still registered as {@code hay_portal} and
     * its class is still {@code HayPortalBlock}. Renaming a block id rewrites
     * every world that has one placed, and there is one of those with people in
     * it; the name is wrong and the id is load-bearing, so the id stays.
     */
    public static boolean isFrame(BlockState state) {
        return state.is(Blocks.COBBLESTONE);
    }

    /** The block the realm's own frames are built out of. Pairs with {@link #isFrame}. */
    public static BlockState frameBlock() {
        return Blocks.COBBLESTONE.defaultBlockState();
    }

    private static final int MIN_INNER_W = 2;
    private static final int MAX_INNER_W = 21;
    private static final int MIN_INNER_H = 3;
    private static final int MAX_INNER_H = 21;

    /** (constCoord, minHoriz, minVert, width, height) of an inner rectangle in one vertical plane. */
    private record Rect(int constCoord, int minH, int minV, int w, int h) {}

    /**
     * Try to light a frame that {@code hayPos} is part of. Returns true and
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
                } else if (!isFrame(s)) {
                    return null; // the opening touches something that isn't frame
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

    /**
     * <b>Flag 18, not 2 - and the 16 is load-bearing.</b> Bit 16 is
     * {@code UPDATE_KNOWN_SHAPE}, which suppresses the neighbour <i>shape</i>
     * updates {@code Level.setBlock} otherwise runs on every placement. Without
     * it, each portal block placed here would ask its neighbours to re-check
     * themselves while the rest of the sheet is still air - and
     * {@link HayPortalBlock#updateShape}, which turns an unenclosed portal block
     * into air, would eat the portal as it was being lit. Vanilla's
     * {@code PortalShape.createPortalBlocks} passes 18 for exactly this reason.
     */
    private static void fill(ServerLevel level, Rect rect, Direction.Axis axis) {
        BlockState portal = ModBlocks.HAY_PORTAL.get().defaultBlockState()
                .setValue(HayPortalBlock.AXIS, axis);
        for (int hc = rect.minH(); hc < rect.minH() + rect.w(); hc++) {
            for (int y = rect.minV(); y < rect.minV() + rect.h(); y++) {
                level.setBlock(cell(rect, axis, hc, y), portal, 18);
            }
        }
    }

    /**
     * Move {@code entity} through the portal at {@code portalPos}. From the
     * horse dimension: back to the linked overworld portal, <b>and nothing else
     * comes with it</b> - a horse returns only by being led or ridden through.
     * From anywhere else: only players act, and they enter a fresh plot.
     *
     * <p>Both exits used to gather up a leaving player's own tamed horses. They
     * do not; see the note at the debug branch below and
     * {@code HorseRealm.leave}.
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

            // NOTHING IS EVACUATED. A portal takes the player and whatever they
            // are leading; horses left standing in the plot stay there, and the
            // plot is torn down behind them, so they are gone. That is the
            // owner's call and it is the same rule as the public realm's - but
            // unlike the realm, this dimension does not keep what you leave, so
            // say so plainly rather than emptying a pen in silence.
            if (entity instanceof ServerPlayer player && plot != null) {
                int leftBehind = DebugPenManager.countOwnedTamedHorses(portalLevel, plot, player.getUUID());
                if (leftBehind > 0) {
                    player.sendSystemMessage(Component.literal(
                            leftBehind == 1
                                    ? "You left a horse of your own in the test corridor. The plot is cleared "
                                            + "when you leave, so it is gone - lead one through the frame next time."
                                    : "You left " + leftBehind + " horses of your own in the test corridor. The "
                                            + "plot is cleared when you leave, so they are gone - lead them "
                                            + "through the frame next time.")
                            .withStyle(ChatFormatting.RED));
                }
            }
            if (entity instanceof Mob mob && mob.isLeashed()) {
                mob.dropLeash();
            }
            if (entity instanceof AbstractHorse horse) {
                // Somebody brought a horse out of the dimension. Credit whoever
                // owns it - a led or ridden horse is not its own achievement.
                if (horse.getOwnerReference() != null
                        && server.getPlayerList().getPlayer(horse.getOwnerReference().getUUID()) != null) {
                    HorseProgress.complete(
                            server.getPlayerList().getPlayer(horse.getOwnerReference().getUUID()),
                            ProgressTask.BRING_HORSE_HOME);
                }
                placeReturningHorse(horse, target, to, new ArrayList<>());
            } else {
                placeAt(entity, target, to);
            }
        } else if (portalLevel.dimension().equals(HorseRealm.REALM_LEVEL)) {
            HorseRealm.leave(entity, portalLevel, portalPos);
        } else {
            // Into the public realm. Horses travel in as well as players, which is
            // what makes the roped-horse shortcut in PortalEventHandler worth
            // having: you lead a string of them into the frame and follow.
            if (entity instanceof ServerPlayer player) {
                HorseProgress.complete(player, ProgressTask.ENTER_DIMENSION);
            }
            if (entity instanceof ServerPlayer || entity instanceof AbstractHorse) {
                HorseRealm.enter(entity, portalLevel, portalPos.above());
            }
        }
    }

    public static void placeAt(Entity entity, ServerLevel target, BlockPos to) {
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
