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
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * <b>The yard: somewhere to test the horses that can break a world.</b>
 *
 * <p>Some of the behaviour genes cannot be judged in a pen and cannot safely be
 * judged in a real save. Each needs room, a controlled floor, and somewhere that
 * does not matter if it fills up with horses - which is exactly what the horse
 * dimension is for, and it did not have one.
 *
 * <p>So: a five-wide path leaving the arrival road to the <b>right</b> (the
 * player arrives facing {@code +X}, so right is {@code +Z}), {@value #PATH_LEN_Z}
 * blocks of it, through a door cut in the corridor wall, opening into a walled
 * yard of signed pens.
 *
 * <h2>What is in it is whatever is still open, and nothing else</h2>
 * A pen for a test that has been answered is the most expensive thing in this
 * project: it spends the one resource that cannot be bought back, which is the
 * owner's time in the game. So the yard is audited against
 * {@code wiki/verification.html} every time that page moves, and a confirmed
 * test's pen is <b>deleted</b> rather than left standing with a tick on it.
 * Gone this way already: verdant's three floors, the pack leader's cows, and
 * the spontaneous-breeding field (owner, 2026-09-12: "we're done with those
 * tests").
 *
 * <h2>Now it is built for a night rather than a visit</h2>
 * The owner's second ask, the same day: <i>"I'm going to leave the game and the
 * horse dimension running over night. Add as many time-reliant tests as
 * possible, focusing on those that don't require direct intervention from
 * me."</i> That is a different design brief from the one the yard was built to,
 * and it rules out more than it lets in. A test belongs in here now only if it
 * <b>starts itself</b>, <b>runs on a clock</b> and <b>leaves a trace somebody
 * can read in the morning</b> - which is what {@link DebugWorldWatch} is for,
 * and why every pen below registers itself with it.
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
    private static final int YARD_DEPTH_Z = 80;

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
     * that box would leak: it would outlive its plot and accumulate on a
     * recycled origin for ever.
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

        // Before anything is placed: the watch's areas are registered as each
        // pen is built, so it has to be empty first or a second visit doubles
        // every reading.
        DebugWorldWatch.start(level, new AABB(
                cx - YARD_HALF_X - 2, gy - 4, ROAD_EDGE_Z,
                cx + YARD_HALF_X + 2, gy + WALL_TOP_DY + 2, mouthZ + YARD_DEPTH_Z + 2));

        buildPath(level, gy, cx, mouthZ);
        buildYardFloorAndWalls(level, gy, cx, mouthZ);
        buildEggLayerPen(level, gy, cx, mouthZ);
        buildSpawnerRoom(level, gy, cx, mouthZ);
        buildSoundHerdPen(level, gy, cx, mouthZ);
        buildIntimidatingPen(level, gy, cx, mouthZ);
        buildStockedRow(level, gy, cx, mouthZ);
        buildGlowRoom(level, gy, cx, mouthZ);
        buildGrowingRow(level, gy, cx, mouthZ);
        buildBoneMealPen(level, gy, cx, mouthZ);
        buildRetinuePen(level, gy, cx, mouthZ);

        // A sign at the junction, on the road, so the yard is discoverable by
        // somebody who walked in to look at pens and does not know it is there.
        DebugPenManager.placeSign(level, new BlockPos(cx + PATH_HALF_X + 1, gy + 1, ROAD_EDGE_Z),
                Direction.SOUTH,
                List.of("-> TEST YARD", PATH_LEN_Z + " blocks", "eggs, ward, cows,", "4 dryads, thaw"));

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
     * One cow, for something to be pushed around.
     *
     * <p>Non-horse mobs are allowed in the yard - see
     * {@code HorseGeneticsEventHandler}, which deleted them everywhere in this
     * dimension until 2026-09-12 and thereby made three of the yard's own tests
     * impossible in the place built for them.
     */
    private static void spawnCow(ServerLevel level, int gy, double x, double z) {
        Cow cow = EntityType.COW.create(level, EntitySpawnReason.COMMAND);
        if (cow == null) {
            return;
        }
        cow.setPos(x, gy + 1, z);
        // Or it despawns overnight and an empty pen reads as the gene working.
        cow.setPersistenceRequired();
        level.addFreshEntity(cow);
    }

    /**
     * Things that are <b>meant</b> to be at head height: pen walls and their
     * gates, the dark rooms and their doors, signs, torches, the spawner.
     *
     * <p>This list has to be kept honest or the check is worse than nothing.
     * When the pens moved to {@code DebugPenManager.penWalls} they stopped
     * being oak fence and became <b>brick wall</b> with fence <b>gates</b>, and
     * the dark rooms gained <b>doors</b> - none of which were in here, so a
     * perfectly good yard reported "NOT sound: 21 blocked at head height" and
     * the warning that exists to be believed cried wolf on its second outing.
     */
    private static boolean isFurniture(ServerLevel level, int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return state.is(Blocks.OAK_FENCE) || state.is(Blocks.OAK_FENCE_GATE)
                || state.is(Blocks.BRICK_WALL) || state.is(Blocks.OAK_DOOR)
                || state.is(Blocks.OAK_SIGN) || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH) || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.SPAWNER) || state.is(Blocks.OAK_PLANKS);
    }

    // ------------------------------------------------------------------
    // The overnight pens
    // ------------------------------------------------------------------

    /**
     * <b>Egg layer, and the accumulation cap.</b> Four of them in one pen,
     * which is the only arrangement that can make the cap fire at all.
     *
     * <p>The guard is {@code NEARBY_CAP = 8} of the item within six blocks, and
     * the interval on a copy is 4 000 to 14 000 ticks. A dropped item despawns
     * at 6 000. <b>So a single horse can essentially never reach its own
     * cap</b> - the floor clears itself about as fast as one horse can fill it,
     * and a morning with two eggs on the ground would be the despawn timer
     * rather than the guard. Four horses laying into one six-block circle is
     * what makes the question answerable overnight, and
     * {@code DebugWorldWatch}'s item hooks log the age at removal so a despawn
     * is never read as a cap.
     *
     * <p>What a pass looks like: the egg count climbs, sits at or below eight,
     * and the log shows drops being <i>refused</i> rather than items vanishing.
     * A floor carpeted in eggs is the failure this guard exists to stop.
     */
    private static void buildEggLayerPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx - YARD_HALF_X + 2;
        int x1 = x0 + 15;
        int z0 = mouthZ + 3;
        int z1 = z0 + 16;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("EGG LAYER", "leave it: does", "it STOP at 8?", "or carpet it?"));
        stock(level, gy, x0 + 6.0, (z0 + z1) / 2.0, "horsegenetics.egg_layer",
                "the egg pen", 2, 2, null);
        DebugWorldWatch.watch("EGG LAYER", box(x0, gy, z0, x1, gy + 1, z1), null);
    }

    /**
     * <b>The sound genes, as a number instead of an opinion.</b>
     *
     * <p>The open question on all four is "are these bearable in a herd", and
     * the cooldowns were chosen by guesswork. That reads like a question only
     * ears can settle and it is not: a herd that fires eighty sounds in two
     * minutes is unbearable arithmetically, and the arithmetic can be collected
     * while everybody is asleep. So five of them in a pen, and
     * {@code DebugWorldWatch} counts every play into the census.
     *
     * <p>Meowing and singer, not base alarm: base alarm is gated on
     * {@code hostile_near} and would sit silent in a pen a hundred blocks from
     * the spawner, which is a blank in the log that looks exactly like a broken
     * gene. A test that cannot fire is worse than a missing one.
     */
    private static void buildSoundHerdPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx - YARD_HALF_X + 2;
        int x1 = x0 + 14;
        int z0 = mouthZ + 26;
        int z1 = z0 + 16;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("SOUND HERD", "3 meow, 2 sing.", "census counts", "every play"));
        stock(level, gy, x0 + 3.0, (z0 + z1) / 2.0, "horsegenetics.meowing",
                "the sound herd (meowing)", 2, 1, null);
        stock(level, gy, x0 + 9.0, (z0 + z1) / 2.0, "horsegenetics.singer",
                "the sound herd (singer)", 1, 1, null);
        DebugWorldWatch.watch("SOUND HERD", box(x0, gy, z0, x1, gy + 1, z1), null);
    }

    /**
     * <b>Intimidating, measured as a distance rather than a count.</b>
     *
     * <p>The gene shoves every non-horse out of a radius of eight to fourteen
     * blocks. In a fenced pen the cows cannot actually leave, so <i>counting</i>
     * them proves nothing: four cows jammed in the far corner and four cows
     * grazing round the horse's feet are the same number and opposite results.
     * What separates them is the <b>nearest-cow distance</b>, which the watch
     * takes from the horse's post every ten seconds.
     *
     * <p>What a pass looks like: the distance climbs within a minute and then
     * sits high - eight or more - all night. A gene doing nothing reads as a
     * figure that wanders between one and six as the cows graze past.
     */
    private static void buildIntimidatingPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + 4;
        int x1 = x0 + 16;
        int z0 = mouthZ + 26;
        int z1 = z0 + 16;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("INTIMIDATING", "6 cows in here.", "do they keep", "their DISTANCE?"));
        int postX = (x0 + x1) / 2;
        int postZ = (z0 + z1) / 2;
        stock(level, gy, postX + 0.5, postZ + 0.5, "horsegenetics.intimidating",
                "the intimidating pen", 1, 0, null);
        // Ringed round the horse, so "they were pushed out" is a change from a
        // known start rather than wherever six cows happened to wander to.
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3.0;
            spawnCow(level, gy, postX + 0.5 + Math.cos(angle) * 3.0,
                    postZ + 0.5 + Math.sin(angle) * 3.0);
        }
        DebugWorldWatch.watch("INTIMIDATING", box(x0, gy, z0, x1, gy + 1, z1),
                new BlockPos(postX, gy + 1, postZ));
    }

    /**
     * <b>The growing row: one pen per spreading gene, floored with what that
     * gene can actually convert.</b>
     *
     * <p>All of them fail in a way that looks exactly like the gene being
     * broken, and the reason is always the floor. {@code
     * GeneAbilityHandler.convert} is a whitelist per cover and they do not
     * overlap: <b>grass</b> only converts bare dirt, so a grass pen floored
     * with grass can never show anything; and a <b>sapling</b> is the only one
     * that builds upward, so it needs air with grass or dirt under it.
     *
     * <p>Each pen is floored for its own gene and nothing else, which is the
     * difference between "the gene does not work" and "the gene had nothing to
     * work on" - the second of which is what the dryad plot was for two
     * sessions.
     */
    private static void buildGrowingRow(ServerLevel level, int gy, int cx, int mouthZ) {
        int z = mouthZ + 64;
        int x = cx - YARD_HALF_X + 2;

        // Verdant's three are CONFIRMED (2026-09-12) and their pens are gone.
        // What is left is the row of slow ones - the tests that cannot be
        // answered by standing still and looking, only by leaving and coming
        // back, which is exactly what this dimension is for.
        //
        // THREE DRYAD PENS NOW, because the locus stopped being one thing on
        // 2026-09-13 and a pen stocked with its first allele would exercise
        // none of what changed. Each one asks a different question and the
        // watch counts the actual block, so all three answer themselves.

        // 1. THE MIXED PAIR - the new inheritance rule, and the only pen that
        // can show it. Oak and birch are chosen because they are DIFFERENT
        // BLOCKS: the watch counts them separately, so "both, each at half
        // rate" is readable as a ratio rather than taken on trust.
        growPen(level, gy, x, z, Blocks.GRASS_BLOCK.defaultBlockState(),
                "horsegenetics.dryad", "Oak/Brch",
                List.of("DRYAD MIXED", "Oak/Brch - BOTH", "at HALF rate.", "count each kind"));
        // Six blocks up, because the far end of this test is a grown TREE and a
        // two-block box would count the sapling and miss the wood.
        DebugWorldWatch.watch("DRYAD MIXED", box(x, gy, z, x + PEN_W, gy + 6, z + PEN_D), null,
                Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING, Blocks.OAK_LOG, Blocks.BIRCH_LOG,
                Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES);
        x += PEN_W + 2;

        // Snow and ice for the melt to eat. A floor rather than a scatter, so
        // "how far has it got" is answerable at a glance from the gate.
        growPen(level, gy, x, z, Blocks.SNOW_BLOCK.defaultBlockState(),
                "horsegenetics.hot_blooded", null,
                List.of("HOT-BLOODED", "floor is SNOW", "+ a strip of ICE", "does it FLOOD?"));
        // A strip of ice in the same pen: ice becomes a water SOURCE rather
        // than air, which is the half of the gene that can flood something -
        // and the half a night of running is most likely to show.
        for (int ix = x + 2; ix <= x + PEN_W - 2; ix++) {
            for (int iz = z + 6; iz <= z + PEN_D - 1; iz++) {
                DebugPenManager.groundColumn(level, ix, gy, iz, Blocks.ICE.defaultBlockState());
            }
        }
        DebugWorldWatch.watch("HOT-BLOODED", box(x, gy, z, x + PEN_W, gy + 1, z + PEN_D), null,
                Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.WATER, Blocks.GRASS_BLOCK);
        x += PEN_W + 2;

        // 2. THE DARK OAK, which is the whole reason the locus was rebuilt.
        // A matched pair, so it plants only dark oak and they accumulate; the
        // translator then clusters them toward each other until a 2x2 closes.
        // THIS IS THE ONE MOST LIKELY TO BE WRONG - the clustering search is
        // new code with no test behind it, and it fails INVISIBLY, as a pen of
        // saplings that never become anything, which is the exact symptom it
        // was written to cure.
        growPen(level, gy, x, z, Blocks.GRASS_BLOCK.defaultBlockState(),
                "horsegenetics.dryad", "Dark/Dark",
                List.of("DRYAD DARK", "needs a 2x2 -", "do they CLUSTER?", "or scatter?"));
        DebugWorldWatch.watch("DRYAD DARK", box(x, gy, z, x + PEN_W, gy + 6, z + PEN_D), null,
                Blocks.DARK_OAK_SAPLING, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
        x += PEN_W + 2;

        // 3. THE MUSHROOM, and its floor is the test. Half podzol, half grass:
        // a mushroom survives ANY light on podzol and needs darkness on grass,
        // so in a lit yard the pass is mushrooms on the podzol half and NONE on
        // the grass half. A pen that is all one thing could not tell "canSurvive
        // is being consulted" from "the gene does not work", which is the
        // distinction this whole change is about.
        growPen(level, gy, x, z, Blocks.PODZOL.defaultBlockState(),
                "horsegenetics.dryad", "Mush/Mush",
                List.of("DRYAD MUSH", "podzol half ONLY", "- none on the", "grass is a PASS"));
        for (int gx = x + (PEN_W / 2) + 1; gx <= x + PEN_W; gx++) {
            for (int gz = z; gz <= z + PEN_D; gz++) {
                DebugPenManager.groundColumn(level, gx, gy, gz,
                        Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        DebugWorldWatch.watch("DRYAD MUSH", box(x, gy, z, x + PEN_W, gy + 1, z + PEN_D), null,
                Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.PODZOL, Blocks.GRASS_BLOCK);
    }

    /**
     * <b>Bone meal, whose result is only ever visible in the log.</b>
     *
     * <p>The allele refuses crops, and that refusal <i>cannot be observed</i>:
     * a crop nobody fertilised comes up anyway on random ticks, so a grown
     * wheat proves nothing either way. What settles it is the list of what the
     * gene <b>did</b> touch - {@code DebugWorldWatch.noteBoneMeal} writes one
     * line per fertilising - and a night of those with no crop among them is
     * the pass.
     *
     * <p>So the pen carries both: saplings and grass for it to hurry along, and
     * a strip of wheat on farmland that must never appear in that list.
     */
    private static void buildBoneMealPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + 16;
        int x1 = cx + 22;
        int z0 = mouthZ + 3;
        int z1 = z0 + 16;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        // Something to hurry: a row of oak saplings it should turn into trees.
        for (int x = x0 + 1; x <= x1 - 1; x += 2) {
            DebugPenManager.fastSet(level, new BlockPos(x, gy + 1, z0 + 2),
                    Blocks.OAK_SAPLING.defaultBlockState());
        }
        // And something it must leave alone, on farmland so it is a real crop.
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            DebugPenManager.groundColumn(level, x, gy, z1 - 2, Blocks.FARMLAND.defaultBlockState());
            DebugPenManager.fastSet(level, new BlockPos(x, gy + 1, z1 - 2),
                    Blocks.WHEAT.defaultBlockState());
        }
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("DRYAD BONE", "hurries saplings", "NEVER the wheat", "- read the log"));
        stock(level, gy, x0 + 3.0, (z0 + z1) / 2.0, "horsegenetics.dryad",
                "the bone meal pen", 2, 0, "Bone/Bone");
        DebugWorldWatch.watch("DRYAD BONE", box(x0, gy, z0, x1, gy + 6, z1), null,
                Blocks.OAK_SAPLING, Blocks.OAK_LOG, Blocks.WHEAT, Blocks.SHORT_GRASS);
    }

    /**
     * One growing pen: the floor its gene needs, walls, a sign saying what the
     * floor should turn into, and two carriers - two, so a failure cannot be
     * "the one horse stood in a corner".
     */
    private static void growPen(ServerLevel level, int gy, int x0, int z0, BlockState floor,
                                String key, String tokens, List<String> sign) {
        int x1 = x0 + PEN_W;
        int z1 = z0 + PEN_D;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, floor);
            }
        }
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH, sign);
        stock(level, gy, x0 + 2.5, (z0 + z1) / 2.0, key, sign.get(0), 2, 0, tokens);
    }

    /**
     * <b>The row along the back: the two tests that still want a person.</b>
     *
     * <p>Everything else in the yard now runs on a clock. These two do not, and
     * they stay because they are cheap to do on the way past rather than
     * because they suit a night.
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
     * <b>A roofed, unlit stone box with a door.</b>
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
        // which is both requirements with no redstone in it.
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
     * <b>The holy ward's NON-INTERFERENCE half, which is the only half this
     * dimension can test.</b> A dark stone box with a real zombie spawner in
     * it: stand the warded horse outside and the spawner must keep producing.
     *
     * <h2>It cannot show the ward working, and two separate facts say so</h2>
     * This pen was signed "the spawner must KEEP going" and then written up as
     * though the distance column would prove the ward <i>wards</i>. It will
     * not, ever:
     *
     * <ul>
     *   <li><b>The ward does not cancel spawner spawns on purpose.</b>
     *       {@code GeneWardHandler.isNatural} gates on the spawn reason and
     *       lets spawner blocks, eggs, breeding, structures, dispensers and
     *       commands straight through - because a gene that silently broke
     *       somebody's mob farm is the failure that whole design is avoiding.
     *       So every zombie this spawner makes is <i>expected</i> to appear,
     *       warded horse or not.</li>
     *   <li><b>Natural spawning cannot happen in this dimension at all.</b>
     *       {@code debug_pens} generates {@code minecraft:the_void}, and the
     *       void biome carries no mob spawn entries - so there is nothing for
     *       the ward to cancel anywhere in the horse dimension, at any light
     *       level, at any distance.</li>
     * </ul>
     *
     * <p>And even in a real world the warding half only matters <b>more than 24
     * blocks from every player</b>, because vanilla never naturally spawns a
     * monster closer than that and the ward reaches 8 to 16
     * ({@code known-gaps.html} gap 180).
     *
     * <p><b>So what is left here is the test that actually mattered</b>, and
     * {@code wiki/verification.html} &sect;0-BT ranks it above the other:
     * <i>holy ward can break somebody's mob farm without erroring</i>. It hooks
     * a global, high-frequency event shared with every other mod in the pack,
     * and if the allow-list is wrong a spawner stops producing and nothing
     * anywhere says why. <b>A steady stream of zombies beside a warded horse is
     * the pass.</b> Silence is the bug - and silence is also what a player
     * standing too far away produces, which is the other thing this pen has
     * already been caught on.
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
                List.of("WARD: NO HARM", "STAND HERE - a", "spawner needs you", "within 16 blocks"));
        // Outside the door rather than inside it: the ward's claim is about
        // what happens NEAR it, and a horse shut in a dark box with a spawner
        // is a horse being hit by zombies.
        stock(level, gy, cx + 8.5, z0 - 1.5, "horsegenetics.holy_ward", "the ward post", 1, 0, null);
        DebugWorldWatch.watch("WARD + SPAWNER", box(x0, gy, z0 - 3, x1, gy + 4, z1), null);
    }

    /**
     * <b>Four pack leaders and a crowd, for the one performance worry the
     * census can already answer.</b>
     *
     * <p>&sect;0-BT's own ranking: <i>"Leader of the pack is the performance
     * one. Six followers pathfinding continuously, and pathfinding is the most
     * expensive thing a mob does. One horse is certainly fine. A stable with
     * several pack leaders in it is the case I have no feel for at all."</i>
     * That the gene <i>works</i> is confirmed; what it costs is not, and it is
     * the kind of question that cannot be answered by looking at anything.
     *
     * <p><b>It needs no new apparatus, which is why it is worth adding now.</b>
     * The census already prints real milliseconds per tick every two minutes.
     * Four leaders at {@code MAX_TARGETS} each is twenty-four mobs re-pathing
     * on a forty-tick beat, all night, in a dimension whose baseline is a flat
     * 50.0 - so the answer is simply whether that number moves. A night of
     * 50.0 is "no measurable cost", which is a real result and one nobody has
     * ever been able to state.
     */
    private static void buildRetinuePen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx - YARD_HALF_X + 2;
        int x1 = x0 + 20;
        int z0 = mouthZ + 74;
        int z1 = mouthZ + 79;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("RETINUE COST", "4 leaders, 16", "cows. Watch the", "census ms/tick"));
        stock(level, gy, x0 + 3.0, (z0 + z1) / 2.0, "horsegenetics.pack_leader",
                "the retinue pen", 4, 0, null);
        for (int i = 0; i < 16; i++) {
            spawnCow(level, gy, x0 + 6.0 + (i % 8) * 1.7, z0 + 1.5 + (i / 8) * 2.0);
        }
        DebugWorldWatch.watch("RETINUE", box(x0, gy, z0, x1, gy + 1, z1), null);
    }

    /**
     * <b>Put carriers of one gene where its test happens, tamed.</b>
     *
     * <p>A test whose first step is "find the right spawn egg" is a test that
     * gets skipped, and these all have to stand in a particular place anyway -
     * so the yard puts them there rather than describing where they go.
     *
     * <p><b>Tamed, always.</b> Vanilla refuses to breed an untamed horse
     * ({@code AbstractHorse.canParent}), the test kit's own legend has told the
     * owner "the yard's horses come tamed" since the day the yard was stocked,
     * and half these pens ask for a foal. Leaving it optional produced exactly
     * the failure this class keeps rediscovering: an instruction ("breed a
     * starburst pair in the yard") that nothing in the world could carry out.
     * A tamed horse with no <i>owner</i> is deliberate and load-bearing - see
     * {@code DebugPenManager.evacuateTamedHorses}, which takes the player's
     * horses home on the way out and must not take the scenery with them.
     *
     * <p>The genotype names <i>only</i> that locus; every other gene falls to
     * its default. So what is standing there is a plain horse that does one
     * thing, and anything else it does is the gene. A gene this build does not
     * have is logged and skipped rather than failing the yard.
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
                DebugPenManager.spawnHorse(level, gy + 1, x + placed++ * 1.5, z, Sex.FEMALE, code, true);
            }
            for (int i = 0; i < studs; i++) {
                DebugPenManager.spawnHorse(level, gy + 1, x + placed++ * 1.5, z, Sex.MALE, code, true);
            }
            ActionTrace.log("test yard", "stocked " + what + " with " + placed + "x " + code
                    + " (" + mares + " mare, " + studs + " stallion), tamed");
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
     * eggs" spends it on fetching.
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

    /** A watch box, written the way a pen is: two corners in block coordinates. */
    private static AABB box(int x0, int y0, int z0, int x1, int y1, int z1) {
        return new AABB(x0, y0, z0, x1, y1, z1);
    }

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
