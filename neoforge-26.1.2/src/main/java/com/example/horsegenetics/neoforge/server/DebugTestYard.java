package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * <b>The yard: somewhere to test the horses that can break a world.</b>
 *
 * <p>Four of the behaviour genes cannot be judged in a pen and cannot safely be
 * judged in a real save. Spontaneous breeding is a population that grows while
 * nobody is watching; the holy ward is a claim about a mob farm <i>continuing
 * to work</i>; the pack leader is a claim about tick cost; the dryad plants
 * things over real minutes. Each needs room, a controlled floor, and somewhere
 * that does not matter if it fills up with horses - which is exactly what the
 * horse dimension is for, and it did not have one.
 *
 * <p>So: a five-wide path leaving the arrival road to the <b>right</b> (the
 * player arrives facing {@code +X}, so right is {@code +Z}), {@value #PATH_LEN_Z}
 * blocks of it, through a door cut in the corridor wall, opening into a walled
 * yard holding four plots with a sign on each.
 *
 * <h2>Why it is flat and plain</h2>
 * Every one of these tests is a <i>count</i> or a <i>did it still work</i>, and
 * both are ruined by terrain. A flat grass floor inside a bedrock box means a
 * horse that wandered off did not fall down a hole, a farm that stopped did not
 * stop because something spawned in a cave, and four horses in a field are four
 * horses in a field. The yard is deliberately the least interesting place in
 * the mod.
 *
 * <h2>The one thing it cannot promise</h2>
 * The spawner is a real vanilla monster spawner set to zombies, which is enough
 * to answer "is the farm still producing" by standing and watching. It is not a
 * <i>farm</i> - no water, no drop, no collection - because the holy ward test
 * only asks whether spawning continues near a warded horse, and a killing floor
 * would add a second thing that can be broken.
 */
final class DebugTestYard {

    /** How far the spur runs from the road before the yard starts. */
    private static final int PATH_LEN_Z = 30;

    /** Half-width of the spur, in blocks either side of its centre line. */
    private static final int PATH_HALF_X = 2;

    /** The yard's floor, measured from the spur's centre line and its mouth. */
    private static final int YARD_HALF_X = 24;
    private static final int YARD_DEPTH_Z = 48;

    /** Matches the corridor, so the yard reads as the same building. */
    private static final int WALL_TOP_DY = 10;

    /** Where the spur leaves the road, relative to the plot origin. */
    private static final int SPUR_CENTRE_DX = 3;

    /** The first z the spur occupies - one block past the road's gravel. */
    private static final int ROAD_EDGE_Z = 4;

    /**
     * <b>The yard's extent, for {@code DebugPenManager.plotBox}.</b>
     *
     * <p>These exist because the yard is the first thing in the dimension that
     * lives <i>outside</i> the corridor's walls, and the plot's bounding box is
     * what tears a plot down and what carries tamed horses home. A yard outside
     * that box would leak: the spontaneous-breeding field is a population that
     * grows on purpose, and left out of the sweep it would outlive its plot and
     * accumulate on a recycled origin for ever - a world-breaking horse
     * breaking a world, in the room built to watch it not do that.
     */
    static final int FAR_Z = ROAD_EDGE_Z + PATH_LEN_Z + YARD_DEPTH_Z + 2;
    static final int WEST_DX = SPUR_CENTRE_DX - YARD_HALF_X - 2;
    static final int EAST_DX = SPUR_CENTRE_DX + YARD_HALF_X + 2;

    private DebugTestYard() {
    }

    /**
     * Build the spur and the yard for {@code plot}. Called once, when the plot
     * is created and its first segments already exist - so this writes
     * <i>over</i> the first pen on the {@code +Z} side, which
     * {@code DebugPenManager} deliberately leaves unbuilt for it.
     */
    static void build(ServerLevel level, DebugPenManager.Plot plot) {
        int gy = plot.baseY;
        int cx = plot.originX + SPUR_CENTRE_DX;
        int mouthZ = ROAD_EDGE_Z + PATH_LEN_Z;          // where the path ends and the yard begins

        buildPath(level, gy, cx, mouthZ);
        buildYardFloorAndWalls(level, gy, cx, mouthZ);
        buildBreedingField(level, gy, cx, mouthZ);
        buildSpawnerRoom(level, gy, cx, mouthZ);
        buildWolfPen(level, gy, cx, mouthZ);
        buildDryadPlot(level, gy, cx, mouthZ);

        // A sign at the junction, on the road, so the yard is discoverable by
        // somebody who walked in to look at pens and does not know it is there.
        DebugPenManager.placeSign(level, new BlockPos(cx + PATH_HALF_X + 1, gy + 1, ROAD_EDGE_Z),
                Direction.SOUTH,
                List.of("-> TEST YARD", PATH_LEN_Z + " blocks", "breeding, ward,", "wolves, dryad"));

        verify(level, gy, cx, mouthZ);
    }

    /**
     * <b>Read the yard back and say whether it is walkable.</b>
     *
     * <p>Every other thing in this dimension is built by code nobody can see
     * run, and the failure mode is a floor with a hole in it or a doorway that
     * was never cut - both of which look like nothing at all until somebody
     * walks into a wall or falls into the void. This walks the path's centre
     * line and a grid over the yard, checks each spot has something to stand on
     * and room to stand in, and logs <i>one</i> line either way.
     *
     * <p>It is a check rather than a repair on purpose: a yard that failed to
     * build wants somebody to read the reason, not a second pass papering over
     * whatever the first one got wrong.
     */
    private static void verify(ServerLevel level, int gy, int cx, int mouthZ) {
        int noFloor = 0;
        int blocked = 0;
        for (int z = ROAD_EDGE_Z; z < mouthZ + YARD_DEPTH_Z; z++) {
            boolean inYard = z >= mouthZ;
            int halfX = inYard ? YARD_HALF_X - 1 : PATH_HALF_X;
            for (int x = cx - halfX; x <= cx + halfX; x += inYard ? 4 : 1) {
                if (level.getBlockState(new BlockPos(x, gy, z)).isAir()) {
                    noFloor++;
                }
                if (!level.getBlockState(new BlockPos(x, gy + 1, z)).isAir()
                        && !isFurniture(level, x, gy + 1, z)) {
                    blocked++;
                }
            }
            if (inYard) {
                z += 3;   // the yard is sampled on a grid, not exhaustively
            }
        }
        if (noFloor == 0 && blocked == 0) {
            HorseGenetics.LOGGER.info("[Debug] test yard built and walkable at x={}, z={}..{}",
                    cx, ROAD_EDGE_Z, mouthZ + YARD_DEPTH_Z);
        } else {
            HorseGenetics.LOGGER.warn("[Debug] test yard is NOT sound: {} spot(s) with no floor, "
                    + "{} blocked at head height, around x={}", noFloor, blocked, cx);
        }
    }

    /** Fences, signs, torches and the spawner room are meant to be in the way. */
    private static boolean isFurniture(ServerLevel level, int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return state.is(Blocks.OAK_FENCE) || state.is(Blocks.OAK_SIGN) || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH) || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.SPAWNER);
    }

    /** The spur itself: gravel floor, plank sides, a glowstone line above each. */
    private static void buildPath(ServerLevel level, int gy, int cx, int mouthZ) {
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();

        for (int z = ROAD_EDGE_Z; z < mouthZ; z++) {
            for (int x = cx - PATH_HALF_X; x <= cx + PATH_HALF_X; x++) {
                DebugPenManager.groundColumn(level, x, gy, z, gravel);
                // Cut the doorway: the corridor's wall and everything above the
                // path has to be air, or the spur runs into the plank wall it
                // was meant to pass through.
                for (int y = gy + 1; y <= gy + WALL_TOP_DY; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
            for (int side : new int[] {1, -1}) {
                int x = cx + side * (PATH_HALF_X + 1);
                DebugPenManager.groundColumn(level, x, gy, z, gravel);
                for (int y = gy + 1; y <= gy + WALL_TOP_DY - 1; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z), planks);
                }
                DebugPenManager.fastSet(level, new BlockPos(x, gy + WALL_TOP_DY, z), glowstone);
                // Bedrock skin outside the planks, as the corridor has.
                int outer = cx + side * (PATH_HALF_X + 2);
                for (int y = gy - 3; y <= gy + WALL_TOP_DY; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(outer, y, z), bedrock);
                }
            }
        }
    }

    /** The yard's grass floor, and the bedrock-and-plank box round it. */
    private static void buildYardFloorAndWalls(ServerLevel level, int gy, int cx, int mouthZ) {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        int z0 = mouthZ;
        int z1 = mouthZ + YARD_DEPTH_Z;

        for (int z = z0; z <= z1; z++) {
            for (int x = cx - YARD_HALF_X; x <= cx + YARD_HALF_X; x++) {
                DebugPenManager.groundColumn(level, x, gy, z, grass);
            }
        }
        // Walls on all four sides, with the mouth of the path left open.
        for (int z = z0; z <= z1; z++) {
            for (int side : new int[] {1, -1}) {
                wallColumn(level, gy, cx + side * YARD_HALF_X, z, planks, bedrock, glowstone);
            }
        }
        for (int x = cx - YARD_HALF_X; x <= cx + YARD_HALF_X; x++) {
            boolean doorway = x >= cx - PATH_HALF_X && x <= cx + PATH_HALF_X;
            if (!doorway) {
                wallColumn(level, gy, x, z0, planks, bedrock, glowstone);
            }
            wallColumn(level, gy, x, z1, planks, bedrock, glowstone);
        }
    }

    private static void wallColumn(ServerLevel level, int gy, int x, int z,
                                   BlockState planks, BlockState bedrock, BlockState glowstone) {
        for (int y = gy; y <= gy + WALL_TOP_DY - 1; y++) {
            DebugPenManager.fastSet(level, new BlockPos(x, y, z), planks);
        }
        DebugPenManager.fastSet(level, new BlockPos(x, gy + WALL_TOP_DY, z), glowstone);
        for (int y = gy - 3; y <= gy - 1; y++) {
            DebugPenManager.fastSet(level, new BlockPos(x, y, z), bedrock);
        }
    }

    /**
     * <b>Spontaneous breeding.</b> The biggest plot, because the gene's own cap
     * is a headcount within eight blocks and a field that crowds it would test
     * the cap rather than the breeding. Fenced, because the point of the test is
     * to leave and come back to a number.
     */
    private static void buildBreedingField(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx - YARD_HALF_X + 2;
        int x1 = x0 + 20;
        int z0 = mouthZ + 3;
        int z1 = z0 + 20;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("SPONTANEOUS", "BREEDING", "2 mares + 2", "studs, then go"));
    }

    /**
     * <b>The holy ward.</b> A dark stone box with a real zombie spawner in it:
     * stand the warded horse outside and the spawner must keep producing.
     * Roofed and unlit, since a spawner that cannot spawn for ordinary reasons
     * proves nothing about the gene.
     */
    private static void buildSpawnerRoom(ServerLevel level, int gy, int cx, int mouthZ) {
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        int x0 = cx + 4;
        int x1 = cx + 14;
        int z0 = mouthZ + 3;
        int z1 = z0 + 10;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                DebugPenManager.groundColumn(level, x, gy, z, stone);
                for (int y = gy + 1; y <= gy + 4; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z),
                            edge ? stone : Blocks.AIR.defaultBlockState());
                }
                // Roof: a spawner needs the dark, and the yard's glowstone line
                // would otherwise light the whole floor.
                DebugPenManager.fastSet(level, new BlockPos(x, gy + 5, z), stone);
            }
        }
        // A two-wide doorway, so you can see in and a horse could follow you.
        for (int x = cx + 8; x <= cx + 9; x++) {
            for (int y = gy + 1; y <= gy + 2; y++) {
                DebugPenManager.fastSet(level, new BlockPos(x, y, z0), Blocks.AIR.defaultBlockState());
            }
        }
        BlockPos spawner = new BlockPos((x0 + x1) / 2, gy + 1, (z0 + z1) / 2);
        DebugPenManager.fastSet(level, spawner, Blocks.SPAWNER.defaultBlockState());
        // Set it to zombies here rather than leaving an empty spawner for the
        // tester to charge with an egg: an empty one produces nothing, which is
        // indistinguishable from the ward having stopped it.
        level.setBlock(spawner, Blocks.SPAWNER.defaultBlockState(), 3);
        if (level.getBlockEntity(spawner) instanceof SpawnerBlockEntity be) {
            be.setEntityId(EntityType.ZOMBIE, level.getRandom());
            be.setChanged();
        }
        DebugPenManager.placeSign(level, new BlockPos(cx + 8, gy + 1, z0 - 1), Direction.NORTH,
                List.of("HOLY WARD", "zombie spawner", "must KEEP", "producing"));
    }

    /** <b>Pack leader.</b> A pen to put three or four in and watch F3's tick line. */
    private static void buildWolfPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx - YARD_HALF_X + 2;
        int x1 = x0 + 14;
        int z0 = mouthZ + 28;
        int z1 = z0 + 16;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("PACK LEADER", "3-4 in here,", "then watch the", "tick time (F3)"));
    }

    /**
     * <b>The dryad.</b> Open grass, no fence - it plants things over real
     * minutes and the question is whether anything appeared, so the plot is
     * bare on purpose and anything standing in it afterwards was planted.
     */
    private static void buildDryadPlot(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + 4;
        int x1 = cx + 20;
        int z0 = mouthZ + 28;
        int z1 = z0 + 16;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        DebugPenManager.placeSign(level, new BlockPos(x0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("DRYAD", "leave one here", "half an hour:", "did it plant?"));
    }

    /**
     * A rectangle of oak fence with a two-wide gate opening in its near wall -
     * two, because a horse will not cross a one-block gap, which is the same
     * reason the pens up the corridor have one.
     */
    private static void fencedPlot(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
        int gateX = (x0 + x1) / 2;
        for (int x = x0; x <= x1; x++) {
            for (int z : new int[] {z0, z1}) {
                boolean gate = z == z0 && (x == gateX || x == gateX + 1);
                DebugPenManager.fastSet(level, new BlockPos(x, gy + 1, z),
                        gate ? Blocks.AIR.defaultBlockState() : fence);
            }
        }
        for (int z = z0 + 1; z < z1; z++) {
            DebugPenManager.fastSet(level, new BlockPos(x0, gy + 1, z), fence);
            DebugPenManager.fastSet(level, new BlockPos(x1, gy + 1, z), fence);
        }
        DebugPenManager.torchOnFence(level, x0, gy, z0);
        DebugPenManager.torchOnFence(level, x1, gy, z0);
        DebugPenManager.torchOnFence(level, x0, gy, z1);
        DebugPenManager.torchOnFence(level, x1, gy, z1);
    }
}
