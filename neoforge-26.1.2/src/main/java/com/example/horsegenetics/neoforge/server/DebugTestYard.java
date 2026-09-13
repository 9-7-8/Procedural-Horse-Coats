package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
    private static final int YARD_DEPTH_Z = 64;

    /** Matches the corridor, so the yard reads as the same building. */
    private static final int WALL_TOP_DY = 10;

    /** The corridor's own outer wall, which the spur passes through rather than repeats. */
    private static final int WALL_PLANK_Z = 25;
    private static final int WALL_BEDROCK_Z = 26;

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
        buildStockedRow(level, gy, cx, mouthZ);
        buildGlowRoom(level, gy, cx, mouthZ);

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

    /**
     * One cow, for the pack leader to lead. Non-horse mobs are allowed in the
     * yard - see {@code HorseGeneticsEventHandler}, which deletes them
     * everywhere else in this dimension and used to delete them here too.
     */
    private static void spawnCow(ServerLevel level, int gy, double x, double z) {
        Cow cow = EntityType.COW.create(level, EntitySpawnReason.COMMAND);
        if (cow == null) {
            return;
        }
        cow.setPos(x, gy + 1, z);
        cow.setPersistenceRequired();
        level.addFreshEntity(cow);
    }

    /** Fences, signs, torches and the spawner room are meant to be in the way. */
    private static boolean isFurniture(ServerLevel level, int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return state.is(Blocks.OAK_FENCE) || state.is(Blocks.OAK_SIGN) || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH) || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.SPAWNER) || state.is(Blocks.OAK_PLANKS);
    }

    /**
     * <b>The row along the back: one pen per open test, already stocked.</b>
     *
     * <p>What is in it is whatever <code>wiki/verification.html</code> is
     * waiting on that needs a horse, which means it goes stale - a pen for a
     * test that has been confirmed is exactly the waste this row exists to stop.
     * It is audited with the test kit, at the same time and against the same
     * page.
     */
    private static void buildStockedRow(ServerLevel level, int gy, int cx, int mouthZ) {
        int z = mouthZ + 48;
        int x = cx - YARD_HALF_X + 2;

        // 0-CP: the shards at full size, the size range side by side, and a
        // stallion so the inheritance question can be asked at all.
        stockedPen(level, gy, x, z, "horsegenetics.starburst", "W/W", 3, 1,
                List.of("STARBURST", "shards? sizes?", "breed one pair:", "foal like parents?"));
        x += PEN_W + 2;

        // 0-CQ needs no horse, but the highlight is easiest to judge with a
        // herd in front of you, and a lead only exists where there is one.
        stockedPen(level, gy, x, z, "horsegenetics.lantern", null, 3, 0,
                List.of("F8 HERD", "press F8 twice:", "2nd says OFF?", "lead in red?"));
    }

    /**
     * The spur. <b>It only builds walls where there is nothing to walk on.</b>
     *
     * <p>The first version walled the whole thirty blocks, which meant it built
     * a corridor <em>inside</em> the corridor: from the road out to the far
     * wall it is crossing ground the pens already stand on, and a second set of
     * planks there cuts into them for no reason. The owner's words: "the far
     * wall is enough, you don't need to duplicate it to create a hallway".
     *
     * <p>So there are three stretches. Across the pen zone: gravel underfoot to
     * show the way, nothing else. At the corridor's own wall: a doorway cut
     * through it. Past it, over open void: floor, walls and lights, because out
     * there the alternative is falling.
     */
    private static void buildPath(ServerLevel level, int gy, int cx, int mouthZ) {
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        int wallInner = WALL_PLANK_Z - 1;   // last z the corridor's own floor covers

        for (int z = ROAD_EDGE_Z; z < mouthZ; z++) {
            boolean overVoid = z > WALL_BEDROCK_Z;
            for (int x = cx - PATH_HALF_X; x <= cx + PATH_HALF_X; x++) {
                DebugPenManager.groundColumn(level, x, gy, z, gravel);
                // Head room the whole way: across the pens this is just air
                // that was already air, and at the wall it is the doorway.
                for (int y = gy + 1; y <= gy + WALL_TOP_DY; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
            if (!overVoid && z <= wallInner) {
                continue;   // inside the corridor: its walls and floor already exist
            }
            for (int side : new int[] {1, -1}) {
                int x = cx + side * (PATH_HALF_X + 1);
                DebugPenManager.groundColumn(level, x, gy, z, gravel);
                for (int y = gy + 1; y <= gy + WALL_TOP_DY - 1; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z), planks);
                }
                DebugPenManager.fastSet(level, new BlockPos(x, gy + WALL_TOP_DY, z), glowstone);
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
                List.of("SPONTANEOUS", "BREEDING", "4 are in here:", "leave, then COUNT"));
        stock(level, gy, x0 + 4.0, (z0 + z1) / 2.0, "horsegenetics.spontaneous_breeding",
                "the breeding field", 2, 2, null);
    }

    /**
     * <b>A roofed, unlit stone box with a two-wide doorway.</b>
     *
     * <p>Two things in this yard need the dark and they need it for opposite
     * reasons: a spawner will not run in the light, and a glow cannot be judged
     * in it. The dimension has a sky and follows the world's clock, so
     * {@code /testkit night} makes it night - but the corridor and the yard are
     * lit by glowstone lines by design, and a glowing horse standing under one
     * is a horse you cannot see glowing. A room with a lid is the only place in
     * here that is actually dark.
     */
    private static void darkRoom(ServerLevel level, int gy, int x0, int x1, int z0, int z1, int doorX) {
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                DebugPenManager.groundColumn(level, x, gy, z, stone);
                for (int y = gy + 1; y <= gy + 4; y++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, y, z),
                            edge ? stone : Blocks.AIR.defaultBlockState());
                }
                DebugPenManager.fastSet(level, new BlockPos(x, gy + 5, z), stone);
            }
        }
        // A DOOR, not a hole. A two-wide gap let the yard's glowstone straight
        // in - so the glow room was never dark - and let the horses straight
        // out. A closed wooden door blocks light and a horse cannot open one,
        // which is both requirements with no redstone in it: a piston door
        // would look better and is a contraption to place blind, and this is
        // already pitch black with the door shut.
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        level.setBlockAndUpdate(new BlockPos(doorX, gy + 1, z0), lower);
        level.setBlockAndUpdate(new BlockPos(doorX, gy + 2, z0), upper);
        // A torch outside it, so the door is findable from the yard without
        // putting any light inside the room.
        DebugPenManager.fastSet(level, new BlockPos(doorX + 1, gy + 2, z0 - 1),
                Blocks.TORCH.defaultBlockState());
    }

    /**
     * <b>The dark room for looking at glows.</b> Tron's two-form heterozygote
     * and a lantern, indoors, with the lid on - the only place in the dimension
     * where a 22% halo is distinguishable from a 100% one.
     */
    private static void buildGlowRoom(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + 4;
        int x1 = cx + 18;
        int z0 = mouthZ + 48;
        int z1 = z0 + 12;
        darkRoom(level, gy, x0, x1, z0, z1, cx + 10);
        DebugPenManager.placeSign(level, new BlockPos(cx + 9, gy + 1, z0 - 1), Direction.NORTH,
                List.of("GLOW ROOM", "SHUT THE DOOR", "tron: box EDGES", "lit, panels dim"));
        stock(level, gy, x0 + 3.5, (z0 + z1) / 2.0, "horsegenetics.tron",
                "the glow room (tron)", 1, 1, "Trs/Trg");
        stock(level, gy, x1 - 3.5, (z0 + z1) / 2.0, "horsegenetics.lantern",
                "the glow room (lantern)", 2, 0, null);
    }

    /**
     * <b>The holy ward.</b> A dark stone box with a real zombie spawner in it:
     * stand the warded horse outside and the spawner must keep producing.
     * Roofed and unlit, since a spawner that cannot spawn for ordinary reasons
     * proves nothing about the gene.
     */
    private static void buildSpawnerRoom(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + 4;
        int x1 = cx + 14;
        int z0 = mouthZ + 3;
        int z1 = z0 + 10;
        darkRoom(level, gy, x0, x1, z0, z1, cx + 8);
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
                List.of("HOLY WARD", "one is already", "outside: spawner", "must KEEP going"));
        // Outside the door rather than inside it: the ward's claim is about
        // what happens NEAR it, and a horse shut in a dark box with a spawner
        // is a horse being hit by zombies.
        // z0 - 1.5, not z0 - 3.5: the room's front wall is three blocks inside
        // the yard's own, so the old spot was ON the yard wall and the horse
        // never appeared at all. Right in front of the door instead, which is
        // where a horse warding a spawner should stand anyway.
        stock(level, gy, cx + 8.5, z0 - 1.5, "horsegenetics.holy_ward", "the ward post");
    }

    /** <b>Pack leader.</b> A pen to put three or four in and watch F3's tick line. */
    private static void buildWolfPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx - YARD_HALF_X + 2;
        int x1 = x0 + 14;
        int z0 = mouthZ + 28;
        int z1 = z0 + 16;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("PACK LEADER", "2 horses, 4 cows", "do the cows", "follow them?"));
        // COWS, not wolves. The gene has one allele per non-hostile mob, so
        // "test it with cows" is a different allele rather than a different
        // gene - and a cow that follows you is a thing you can SEE happening,
        // where a wolf that follows you looks like a wolf. Four of them go in
        // with the horses, since the test is whether they trail it about.
        stock(level, gy, x0 + 4.0, (z0 + z1) / 2.0, "horsegenetics.pack_leader",
                "the pack-leader pen", 2, 0, "Cow/Cow");
        for (int i = 0; i < 4; i++) {
            spawnCow(level, gy, x0 + 8.0 + i * 1.5, (z0 + z1) / 2.0 + 2.0);
        }
    }

    /**
     * <b>The dryad</b>, fenced, with a dryad horse already standing in it.
     *
     * <p>Bare grass and nothing else, so anything growing there later was
     * planted rather than generated - but <i>fenced</i>, because the test is to
     * leave it alone for a real half hour and an unfenced horse spends that
     * half hour somewhere else. And stocked, because a test whose first step is
     * "find the right spawn egg" is a test that gets skipped: the horse that
     * has to be there is there.
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
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("DRYAD", "one is already", "in here: leave", "it half an hour"));
        stock(level, gy, (x0 + x1) / 2.0, (z0 + z1) / 2.0,
                "horsegenetics.dryad", "the dryad plot");
    }

    /**
     * <b>Put a homozygous carrier of one gene where its test happens.</b>
     *
     * <p>A test whose first step is "find the right spawn egg" is a test that
     * gets skipped, and these two have to stand in a particular place anyway -
     * the dryad inside its fence, the ward beside the spawner - so the yard
     * puts them there rather than describing where they go.
     *
     * <p>The genotype names <i>only</i> that locus; every other gene falls to
     * its default. So what is standing there is a plain horse that does one
     * thing, and anything else it does is the gene. A gene this build does not
     * have is logged and skipped rather than failing the yard, which is the
     * trade the test kit's eggs make for the same reason.
     */
    private static void stock(ServerLevel level, int gy, double x, double z, String key, String what) {
        stock(level, gy, x, z, key, what, 1, 0, null);
    }

    /**
     * {@code mares} mares and {@code studs} stallions, spread along a short
     * line so they are not standing inside one another.
     *
     * <p>{@code tokens} names the alleles when the test wants a specific pair -
     * tron's two tube forms are a heterozygote, and a homozygote of either is a
     * different outcome - otherwise the gene's first allele is used twice.
     */
    private static void stock(ServerLevel level, int gy, double x, double z, String key,
                              String what, int mares, int studs, String tokens) {
        Gene gene = Genes.byKeyOrNull(key);
        if (gene == null) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: no {} gene, {} left empty", key, what);
            return;
        }
        String pair = tokens != null ? tokens
                : gene.alleles().get(0).token() + "/" + gene.alleles().get(0).token();
        String code = gene.key() + "=" + pair;
        int placed = 0;
        try {
            for (int i = 0; i < mares; i++) {
                DebugPenManager.spawnHorse(level, gy + 1, x + placed++ * 1.5, z, Sex.FEMALE, code);
            }
            for (int i = 0; i < studs; i++) {
                DebugPenManager.spawnHorse(level, gy + 1, x + placed++ * 1.5, z, Sex.MALE, code);
            }
            ActionTrace.log("test yard", "stocked " + what + " with " + placed + "x " + code
                    + " (" + mares + " mare, " + studs + " stallion)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: could not stock {}", what, e);
        }
    }

    /**
     * <b>A signed, stocked pen in the row along the back of the yard.</b>
     *
     * <p>The yard used to be four plots you brought horses to. It is now the
     * place the horses already are: the owner's time in the game is the
     * scarcest thing here, and "walk to the yard, then go back for the right
     * eggs" spends it on fetching. One pen per open test, each with the animals
     * it needs standing in it.
     */
    private static void stockedPen(ServerLevel level, int gy, int x0, int z0,
                                   String key, String tokens, int mares, int studs,
                                   List<String> sign) {
        int x1 = x0 + PEN_W;
        int z1 = z0 + PEN_D;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        stock(level, gy, x0 + 2.5, (z0 + z1) / 2.0, key, sign.get(0), mares, studs, tokens);
    }

    private static final int PEN_W = 9;
    private static final int PEN_D = 9;

    /**
     * A pen, built by {@code DebugPenManager.penWalls} - the same brick wall,
     * the same two-wide gate, the same corner torches as the pens up the
     * corridor. This used to be its own fence-laying loop and every difference
     * between the two was a bug: posts that did not connect, torches that
     * replaced the corner posts, and an opening made of air that horses walked
     * straight out of. Now there is one pen builder and the yard calls it.
     */
    private static void fencedPlot(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        DebugPenManager.penWalls(level, gy + 1, x0, x1, z0, z1,
                (x0 + x1) / 2, z0, Direction.NORTH);
    }

}
