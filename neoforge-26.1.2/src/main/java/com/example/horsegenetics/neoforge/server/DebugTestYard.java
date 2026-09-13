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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LightBlock;
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
    private static final int YARD_DEPTH_Z = 210;

    // ------------------------------------------------------------------
    // THE GRID
    // ------------------------------------------------------------------
    //
    // The pens were added one at a time and packed against each other, and on
    // 2026-09-13 the owner hit all three consequences of that in one visit:
    // "some have gates that are inaccessible and are getting their signs
    // overwritten", and "I cannot access the glow room because of how you laid
    // out the pens".
    //
    // All three are the same mistake. A pen built by fencedPlot puts its GATE
    // in the middle of its north wall and its SIGN one block further north
    // still - so a row butted up against the row in front of it writes its sign
    // into that row's south wall and opens its gate into it. The glow room is
    // the worst case because its door is the only way in at all.
    //
    // So: every row of pens owns a band of z, and between any two bands there
    // is an AISLE. The sign lives in the aisle, the gate opens into the aisle,
    // and the aisle runs the full width of the yard so it is reachable from the
    // centre walkway. Nothing is allowed to cross that walkway - WEST_MAX and
    // EAST_MIN are what keep the gate reachable from the door in the corridor
    // wall, a hundred blocks away.

    /** Clear blocks in front of every row: one for the sign, three to walk in. */
    private static final int AISLE = 4;

    /** The pens stop here either side, leaving the spur's width clear end to end. */
    private static final int WEST_MAX = -3;
    private static final int EAST_MIN = 3;

    /** The widest a pen may be on one side of the walkway. */
    private static final int BLOCK_W = 19;

    /** Row north walls, as offsets from the yard's mouth. Each is the one before it plus its depth plus an aisle. */
    private static final int ROW_A = 3;                         // bone meal | spawner room
    private static final int ROW_A_D = 16;
    private static final int ROW_B = ROW_A + ROW_A_D + AISLE;   // hydrophobic | lava channel
    private static final int ROW_B_D = 6;
    private static final int ROW_C = ROW_B + ROW_B_D + AISLE;   // molten hooves x4
    private static final int ROW_C_D = 20;
    private static final int ROW_D = ROW_C + ROW_C_D + AISLE;   // sound herd | intimidating
    private static final int ROW_D_D = 16;
    private static final int ROW_E = ROW_D + ROW_D_D + AISLE;   // the display row x8
    private static final int ROW_E_D = 7;
    private static final int ROW_F = ROW_E + ROW_E_D + AISLE;   // starburst, F8 | glow room
    private static final int ROW_F_D = 12;
    private static final int ROW_G = ROW_F + ROW_F_D + AISLE;   // the growing row x4
    private static final int ROW_G_D = 9;
    private static final int ROW_H = ROW_G + ROW_G_D + AISLE;   // the retinue
    private static final int ROW_H_D = 10;
    private static final int ROW_I = ROW_H + ROW_H_D + AISLE;   // the night block, temper
    private static final int ROW_I_D = 7;
    private static final int ROW_J = ROW_I + ROW_I_D + AISLE;   // the night block, watch
    private static final int ROW_J_D = 7;

    /**
     * <b>Where the player stands for the ward test, and why it is so far from
     * everything.</b>
     *
     * <p>Vanilla will not naturally spawn a monster within <b>24 blocks</b> of
     * a player ({@code known-gaps.html} gap 180). That single rule is why the
     * holy ward's real claim has never been tested: the ward reaches 8 to 16
     * blocks, which is entirely inside the radius where nothing was going to
     * spawn anyway, so standing next to a warded horse can only ever prove
     * something you already had for free.
     *
     * <p>So the chambers are put {@value #WARD_RUN} blocks past the standing
     * spot, which with their offset either side of the walkway puts both of
     * them about thirty-five blocks away - comfortably outside the dead zone,
     * and <b>equally far from both</b>, which is the part that makes the
     * comparison fair.
     */
    private static final int WARD_RUN = 30;

    /** The teal wool, and the two chambers thirty blocks beyond it. */
    private static final int WARD_STAND = ROW_J + ROW_J_D + AISLE + 2;
    private static final int ROW_K = WARD_STAND + WARD_RUN;
    private static final int ROW_K_D = 12;

    /** The west block's left edge, and the east block's right edge. */
    private static final int WEST_MIN = WEST_MAX - BLOCK_W;
    private static final int EAST_MAX = EAST_MIN + BLOCK_W;

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
        buildSpawnerRoom(level, gy, cx, mouthZ);
        buildNightBlock(level, gy, cx, mouthZ);
        buildBaseAlarmPen(level, gy, cx, mouthZ);
        buildStockedRow(level, gy, cx, mouthZ);
        buildGlowRoom(level, gy, cx, mouthZ);
        buildGrowingRow(level, gy, cx, mouthZ);
        buildBoneMealPen(level, gy, cx, mouthZ);
        buildLavaChannel(level, gy, cx, mouthZ);
        buildWeatherPens(level, gy, cx, mouthZ);
        buildDisplayRow(level, gy, cx, mouthZ);
        buildMoltenRow(level, gy, cx, mouthZ);

        // A sign at the junction, on the road, so the yard is discoverable by
        // somebody who walked in to look at pens and does not know it is there.
        DebugPenManager.placeSign(level, new BlockPos(cx + PATH_HALF_X + 1, gy + 1, ROAD_EDGE_Z),
                Direction.SOUTH,
                List.of("-> TEST YARD", PATH_LEN_Z + " blocks", "NIGHT pens, ward,", "lava, dryads"));

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

    /**
     * <b>The weather loci - the family &sect;0-BT said could not be checked at all.</b>
     *
     * <p>Its words: <i>"are the weather loci noticeable at all? They only
     * express when the sky agrees, and the magnitudes are a first guess. It is
     * the one locus family you cannot check on demand."</i> That is true of
     * <i>looking</i> at a horse and false of reading its numbers. A conditional
     * attribute modifier is either on the horse or it is not, and
     * {@code DebugWorldWatch.watchAttribute} prints which.
     *
     * <p>So this is a two-reading test and the whole of it fits in one line:
     * <b>read the census, {@code /weather rain}, read it again.</b> If the
     * range moves, the locus works and the number is its magnitude - which also
     * answers the second half of the complaint, because "a first guess" stops
     * being a guess the moment somebody can see it.
     *
     * <p>Two pens, because the two loci move different attributes and a single
     * pen could only report one of them.
     */
    private static void buildWeatherPens(ServerLevel level, int gy, int cx, int mouthZ) {
        int z0 = mouthZ + ROW_D;
        int z1 = z0 + ROW_D_D;

        int x0 = cx + EAST_MIN;
        int x1 = x0 + 9;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("WEATHER SPEED", "read the census,", "/weather rain,", "read it AGAIN"));
        stock(level, gy, x0 + 2.5, (z0 + z1) / 2.0, "horsegenetics.weather_speed",
                "the weather speed pen", 2, 0, null);
        DebugWorldWatch.watchAttribute("WEATHER SPEED",
                box(x0, gy, z0, x1, gy + 1, z1), Attributes.MOVEMENT_SPEED);

        int jx0 = cx + EAST_MIN + 11;
        int jx1 = jx0 + 9;
        fencedPlot(level, gy, jx0, jx1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(jx0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("WEATHER JUMP", "same test, jump", "instead of speed", "/weather rain"));
        stock(level, gy, jx0 + 2.5, (z0 + z1) / 2.0, "horsegenetics.weather_jump",
                "the weather jump pen", 2, 0, null);
        DebugWorldWatch.watchAttribute("WEATHER JUMP",
                box(jx0, gy, z0, jx1, gy + 1, z1), Attributes.JUMP_STRENGTH);
    }

    /** One night stall: the gene, the allele, what it should do, and whether it needs a target. */
    private record Nightly(String key, String token, String name, String what, boolean needsHerd,
                           boolean needsMonsters) {
        Nightly(String key, String token, String name, String what) {
            this(key, token, name, what, false, false);
        }
    }

    /**
     * <b>The night block: one stall per night behaviour, all fifteen.</b>
     *
     * <p>&sect;0-AT has said since 2026-09-09 that "the night loci - behaviour,
     * so nothing here is confirmed by a render". Two whole loci, thirteen
     * variants between them, plus dhampir and lycanthropy, and <i>not one of
     * them</i> has ever been watched. They were <b>unwatchable rather than
     * untested</b>, and nobody knew which: the horse dimension had no night,
     * because a {@code dimension_type} needs <b>timelines</b> as well as a
     * {@code default_clock} and it carried only the clock. Every night-gated
     * gene in the mod sat inert in the one place built for watching genes.
     *
     * <h2>Each variant gets its own stall, because the variants are the point</h2>
     * They differ by <i>who they are about</i> - riders, herds, monsters,
     * everything - and a single pen would answer for one of them and leave the
     * rest exactly as unproven as they are now. Fifteen stalls is a walk down
     * two rows with {@code /testkit night} run once.
     *
     * <h2>Two of them cannot fire here, and the sign says so</h2>
     * The <b>monster</b>-targeted forms need a hostile nearby, and the only
     * hostiles in this dimension come from the ward's spawner - which only runs
     * with a player standing beside it. A stall that cannot fire is worse than
     * no stall, so those two are labelled rather than quietly left to look
     * broken. The <b>herd</b>-targeted forms get a cow each, so they can.
     */
    private static void buildNightBlock(ServerLevel level, int gy, int cx, int mouthZ) {
        List<Nightly> temper = List.of(
                new Nightly(NIGHT_TEMPER, "Agp", "HUNT: RIDERS", "comes at YOU"),
                new Nightly(NIGHT_TEMPER, "Agc", "HUNT: HERDS", "goes for the cow", true, false),
                new Nightly(NIGHT_TEMPER, "Agh", "HUNT: MONSTERS", "zombies come to IT"),
                new Nightly(NIGHT_TEMPER, "Aga", "HUNT: ALL", "you AND the cow", true, false),
                new Nightly(NIGHT_TEMPER, "Flp", "SHY: RIDERS", "backs away from you"),
                new Nightly(NIGHT_TEMPER, "Flc", "SHY: HERDS", "avoids the cow", true, false),
                new Nightly(NIGHT_TEMPER, "Flh", "SHY: MONSTERS", "flees the zombies"),
                new Nightly(NIGHT_TEMPER, "Fla", "SHY: ALL", "avoids everything", true, false));
        List<Nightly> watch = List.of(
                new Nightly(NIGHT_WATCH, "Wst", "WATCH: FIXED", "stands and stares"),
                new Nightly(NIGHT_WATCH, "Wnr", "WATCH: CLOSING", "watches what nears"),
                new Nightly(NIGHT_WATCH, "Wsi", "WATCH: SIGHTED", "watches what it sees"),
                new Nightly(NIGHT_WATCH, "Wun", "WATCH: UNSEEN", "watches the UNSEEN"),
                new Nightly(NIGHT_WATCH, "Wbh", "WATCH: BEHIND", "watches close behind"),
                new Nightly("horsegenetics.dhampir", null, "DHAMPIR", "night-gated too"));
        // Lycan is CONFIRMED and its stall is gone: the horse became an ALLAY,
        // which is the gene working - and also why a stall could never have
        // held it, since allays fly and a pen wall is one block high.

        nightRow(level, gy, cx, mouthZ + ROW_I, ROW_I_D, temper);
        nightRow(level, gy, cx, mouthZ + ROW_J, ROW_J_D, watch);
    }

    private static final String NIGHT_TEMPER = "horsegenetics.magic_night_temper";
    private static final String NIGHT_WATCH = "horsegenetics.magic_night_watch";

    /** Up to eight stalls across a row, four each side of the walkway. */
    private static void nightRow(ServerLevel level, int gy, int cx, int z0, int depth,
                                 List<Nightly> stalls) {
        int z1 = z0 + depth;
        int[] starts = {
                cx + WEST_MIN, cx + WEST_MIN + 5, cx + WEST_MIN + 10, cx + WEST_MIN + 15,
                cx + EAST_MIN, cx + EAST_MIN + 5, cx + EAST_MIN + 10, cx + EAST_MIN + 15};
        for (int i = 0; i < stalls.size() && i < starts.length; i++) {
            Nightly n = stalls.get(i);
            int x0 = starts[i];
            int x1 = x0 + 4;
            fencedPlot(level, gy, x0, x1, z0, z1);
            DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                    List.of(n.name(), "after dark:", n.what(),
                            "/testkit night"));
            String pair = n.token() == null ? null : n.token() + "/" + n.token();
            stock(level, gy, x0 + 1.5, (z0 + z1) / 2.0, n.key(), n.name(), 1, 0, pair);
            if (n.needsHerd()) {
                spawnCow(level, gy, x0 + 3.0, (z0 + z1) / 2.0);
            }
            DebugWorldWatch.watch(n.name(), box(x0, gy, z0, x1, gy + 1, z1), null);
        }
    }

    /**
     * <b>Base alarm, beside the spawner, because that is the only place its
     * condition can be true.</b>
     *
     * <p>It was deliberately kept OUT of the sound herd for exactly this
     * reason: its sound is gated on {@code hostile_near}, so a hundred blocks
     * from the spawner it would have sat silent all night and a blank in the
     * log reads exactly like a broken gene. Here it is one pen from the
     * zombies, so when the spawner is running - which is when a player is
     * standing at it, and that is where the ward test wants her anyway - this
     * gene finally gets a chance to fire.
     *
     * <p>The census counts every gene sound by id, so the answer arrives as a
     * number beside the ward's spawn lines rather than as an impression.
     */
    private static void buildBaseAlarmPen(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + EAST_MIN + 14;
        int x1 = cx + EAST_MAX;
        int z0 = mouthZ + ROW_A;
        int z1 = z0 + ROW_A_D;
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("BASE ALARM", "fires only when a", "HOSTILE is near -", "the spawner is"));
        stock(level, gy, x0 + 2.0, (z0 + z1) / 2.0, "horsegenetics.base_alarm",
                "the base alarm pen", 2, 0, null);
        DebugWorldWatch.watch("BASE ALARM", box(x0, gy, z0, x1, gy + 1, z1), null);
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
        int z = mouthZ + ROW_G;
        // Two west, two east. Stepping straight across the yard put the third
        // pen ON the centre walkway, which is how the back half of the yard
        // walled itself off from the gate.
        int[] at = {cx + WEST_MIN, cx + WEST_MIN + 10, cx + EAST_MIN, cx + EAST_MIN + 10};
        int slot = 0;
        int x = at[slot];

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
        x = at[++slot];


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
        x = at[++slot];

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
        int x0 = cx + WEST_MIN;
        int x1 = cx + WEST_MAX;
        int z0 = mouthZ + ROW_A;
        int z1 = z0 + ROW_A_D;
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
        int z = mouthZ + ROW_F;
        int x = cx + WEST_MIN;

        // 0-CP: the shards at full size, the size range side by side, and a
        // stallion so the inheritance question can be asked at all.
        stockedPen(level, gy, x, z, "horsegenetics.starburst", "W/W", 3, 1,
                List.of("STARBURST", "shards? sizes?", "breed one pair:", "foal like parents?"));
        x = cx + WEST_MIN + 10;

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
    private static void darkRoom(ServerLevel level, int gy, int x0, int x1, int z0, int z1,
                                 int doorX) {
        darkRoom(level, gy, x0, x1, z0, z1, doorX, true);
    }

    /**
     * {@code spawnProof} is the difference between a room that is dark for
     * <i>looking</i> and one that is dark for <i>spawning</i>, and getting it
     * wrong in either direction ruins a test. The glow room wants the first: it
     * must be black to the eye and must not fill with zombies overnight. The
     * ward chambers want the second and would measure nothing at all with a
     * light block in them - which is exactly what they would have inherited,
     * because this method is shared.
     */
    private static void darkRoom(ServerLevel level, int gy, int x0, int x1, int z0, int z1,
                                 int doorX, boolean spawnProof) {
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

        // LIGHT LEVEL ONE, AND INVISIBLE. From 2026-09-13 the dimension has a
        // biome that spawns zombies in the dark, which is what the ward test
        // has always needed - and it turns a sealed unlit box into a zombie
        // trap that fills overnight. The dimension spawns monsters at block
        // light 0 exactly, so ONE is enough to stop it, and one is still black
        // to the eye: a glow is judged against the room, and a room at 1 looks
        // the same as a room at 0 while a room full of zombies does not.
        if (spawnProof) {
            for (int x = x0 + 1; x < x1; x += 4) {
                for (int z = z0 + 1; z < z1; z += 4) {
                    DebugPenManager.fastSet(level, new BlockPos(x, gy + 4, z),
                            Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 1));
                }
            }
        }
    }

    /**
     * <b>The dark room for looking at glows.</b> Tron's two-form heterozygote
     * and a lantern, indoors, with the lid on - the only place in the dimension
     * where a 22% halo is distinguishable from a 100% one.
     */
    private static void buildGlowRoom(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + EAST_MIN;
        int x1 = cx + EAST_MAX;
        int z0 = mouthZ + ROW_F;
        int z1 = z0 + ROW_F_D;
        darkRoom(level, gy, x0, x1, z0, z1, (x0 + x1) / 2);
        DebugPenManager.placeSign(level, new BlockPos((x0 + x1) / 2 - 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("GLOW ROOM", "SHUT THE DOOR", "tron: box EDGES", "lit, panels dim"));
        stock(level, gy, x0 + 3.5, (z0 + z1) / 2.0, "horsegenetics.tron",
                "the glow room (tron)", 1, 1, "Trs/Trg");
        stock(level, gy, x1 - 3.5, (z0 + z1) / 2.0, "horsegenetics.lantern",
                "the glow room (lantern)", 2, 0, null);
        // 0-CO named the two that should actually show the falloff, and neither
        // was in here. GAMMA is the softest lit mask in the mod (SPOTS at 1.8)
        // and is the one the section says to look at; RIME at 0.5 is the
        // distant fourth. Everything else glowing is hard-edged by
        // construction, so coverage-times-level is level-or-nothing on it and
        // an unchanged look is the CORRECT outcome rather than a missing one.
        stock(level, gy, x0 + 3.5, z0 + 2.5, "horsegenetics.gamma",
                "the glow room (gamma - the softest lit mask)", 1, 0, null);
        stock(level, gy, x1 - 3.5, z0 + 2.5, "horsegenetics.rime",
                "the glow room (rime)", 1, 0, null);
        // And the one horse that answers a question neither of the others can:
        // overlapping lit texels take the BRIGHTER of the two rather than
        // summing, deliberately. Blown out to flat white where they cross means
        // the max-not-sum rule has broken.
        stock(level, gy, (x0 + x1) / 2.0, z1 - 2.5, "horsegenetics.lantern",
                "the glow room (two glows at once)", 1, 0, "La/La",
                "horsegenetics.tron=Trs/Trs");
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
        int x0 = cx + EAST_MIN;
        int x1 = cx + EAST_MIN + 12;
        int z0 = mouthZ + ROW_A;
        int z1 = z0 + 12;
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
     * <b>A base coat pale enough to show a dark mark AND a white one.</b>
     *
     * <p>A stocked horse names only the locus under test and every other locus
     * falls to its default, which is a <b>black</b> horse - and on a black
     * horse a dark marking simply is not there. The owner hit this on
     * 2026-09-11 with a barred wing and a nightbell foxglove, both of which
     * "vanished". Chestnut with one cream copy is pale gold, which a black mark
     * and a white mark both stand out on, and it is the same base the test
     * kit's own intake eggs use.
     */
    private static final String PALE = "horsegenetics.extension=e/e-horsegenetics.matp=Cr/N";

    /**
     * One display stall: the gene, the thing to look for, and optionally a
     * <b>second form</b> to stand beside it.
     *
     * <p>{@code alt} exists for rainbow drip, which ships in a plain and a
     * coloured form and was two separate eggs in the old kit. Two eggs is the
     * wrong shape for "do these differ": a stall holding one of each answers it
     * by looking, and costs nothing.
     */
    private record Look(String key, String name, String check1, String check2, String alt) {
        Look(String key, String name, String check1, String check2) {
            this(key, name, check1, check2, null);
        }
    }

    /**
     * <b>The display row: eight coat genes nobody has ever seen on a horse.</b>
     *
     * <p>These are the simplest tests in the project and they have been the
     * most expensive to run, because the only way to do one was to find the
     * right spawn egg, spawn it, look, and repeat - two whole hotbar batches of
     * it. A stall each turns two batches into a walk, which is the entire
     * point: the owner's time in the game is the scarcest thing here and
     * fetching is the cheapest thing to delete.
     *
     * <p><b>Two horses per stall, not one.</b> Most of these are questions about
     * <i>variation</i> - do the drips have their own lengths, does the emblem
     * land somewhere new - and one horse cannot answer a question about
     * variation. Every horse from one preset rolls its own epigenome, so two
     * side by side is the comparison.
     *
     * <p>The row splits around the yard's centre line, because a stall sitting
     * in the walkway is a wall between the gate and everything behind it.
     */
    private static void buildDisplayRow(ServerLevel level, int gy, int cx, int mouthZ) {
        // FOUR, not eight. Rime, candelabra, tribal claw and holo flake all
        // came back "look fine" on 2026-09-13 and their stalls are gone - the
        // row is audited like every other pen, and a stall for an answered
        // question is the thing this yard exists to keep deleting.
        // TWO. Contour cells and rainbow drip both have their verdict already -
        // "one patch ate the whole barrel", "the shape is wrong" - and looking
        // at them again before the drawing is changed adds nothing. They come
        // back when there is something new to look AT.
        //
        // These two stay because each has forms nobody has seen. Ooze was
        // confirmed in its PLAIN form ("looks GREAT") and has a coloured one;
        // gilded crackle was seen in one of its three.
        List<Look> row = List.of(
                new Look("horsegenetics.ooze_drip", "OOZE - COLOURED",
                        "plain is CONFIRMED", "this is the other", "oz"),
                new Look("horsegenetics.gilded_crackle", "CRACKLE - BLACK",
                        "1 of 3 forms seen.", "wants more depth", "Gck"));

        int z0 = mouthZ + ROW_E;
        int z1 = z0 + ROW_E_D;
        int[] starts = {cx + WEST_MIN, cx + WEST_MIN + 5};
        for (int i = 0; i < row.size() && i < starts.length; i++) {
            Look look = row.get(i);
            int x0 = starts[i];
            int x1 = x0 + 4;
            fencedPlot(level, gy, x0, x1, z0, z1);
            DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                    List.of(look.name(), look.check1(), look.check2(), "(pale base)"));
            if (look.alt() == null) {
                stock(level, gy, x0 + 1.5, (z0 + z1) / 2.0, look.key(),
                        look.name(), 2, 0, null, PALE);
            } else {
                stock(level, gy, x0 + 1.0, (z0 + z1) / 2.0, look.key(),
                        look.name() + " (plain)", 1, 0, null, PALE);
                stock(level, gy, x0 + 3.0, (z0 + z1) / 2.0, look.key(),
                        look.name() + " (" + look.alt() + ")", 1, 0,
                        look.alt() + "/" + look.alt(), PALE);
            }
        }
    }

    /**
     * <b>Molten hooves' four alleles, side by side.</b>
     *
     * <p>The rendering is owner-confirmed; what is not is whether the four
     * <i>differ</i>. The kit asked for them one egg at a time, and "must differ
     * from the last one" is a question you cannot answer from memory two
     * spawns later. Four stalls in a row answers it by looking left.
     *
     * <p>White is deliberately the <b>heterozygote</b> - one copy is supposed
     * to be enough for it, and nothing has ever checked that it is.
     */
    private static void buildMoltenRow(ServerLevel level, int gy, int cx, int mouthZ) {
        String[][] forms = {
                {"MltW/n", "WHITE (1 copy)", "glowing white"},
                {"MltB/MltB", "BLACK", "must NOT glow"},
                {"MltC/MltC", "ONE COLOUR", "glowing, single"},
                {"MltM/MltM", "MULTICOLOUR", "differs from <-"}};
        // TWENTY DEEP AND NINE WIDE, not five by four. The first version was
        // four narrow stalls and the owner's verdict was immediate: "the molten
        // hooves pens are so small the horses can't move to show the marking".
        // A hoofprint gene needs a horse that is WALKING, so the pen has to be
        // somewhere a horse would choose to walk across - which is the one
        // requirement a display stall gets exactly backwards.
        int z0 = mouthZ + ROW_C;
        int z1 = z0 + ROW_C_D;
        int[] starts = {cx + WEST_MIN, cx + WEST_MIN + 10, cx + EAST_MIN, cx + EAST_MIN + 10};
        for (int i = 0; i < forms.length; i++) {
            int x0 = starts[i];
            int x1 = x0 + 9;
            fencedPlot(level, gy, x0, x1, z0, z1);
            DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                    List.of("MOLTEN " + (i + 1) + " of 4", forms[i][1], forms[i][2], "DO THE 4 DIFFER?"));
            stock(level, gy, x0 + 1.5, (z0 + z1) / 2.0, "horsegenetics.molten_hooves",
                    "molten " + forms[i][1], 2, 0, forms[i][0]);
        }
    }

    /**
     * <b>A long lava channel, to time a crossing.</b>
     *
     * <p>The float and the rider's immunity are owner-confirmed (2026-09-13).
     * What is left is a number: {@code known-gaps.html} gap 179 asks whether a
     * lava crossing at vanilla's fixed 0.02 with half-speed drag is
     * <i>acceptable</i>, or whether it is worth this project's first mixin -
     * and that cannot be answered by looking at one block of lava. It needs a
     * run long enough to be boring, which is the entire design of this pen.
     *
     * <p>{@value #LAVA_LEN} blocks of it, with dry stone at both ends to get on
     * and off. Ride in at one end, count.
     *
     * <h2>Two things this pen cannot tell you, and both are fine</h2>
     * The horse survives the lava whether or not it is fireproof, because
     * {@code HorseGeneticsEventHandler} cancels all horse damage in this
     * dimension. And the <i>rider</i> survives it whether or not the gene
     * protects them, because the test world is creative. Neither matters here:
     * both of those are already confirmed, and what is being measured is
     * <b>speed</b>. Worth writing down so nobody later reads a fireproof-less
     * horse strolling through and concludes the gene does nothing.
     *
     * <h2>Stone, not grass, and the lava two blocks from the gate</h2>
     * Lava sets fire to what it can reach, the pens are built with oak fence
     * gates, and a yard that burns its own fences down overnight would be a
     * memorable way to lose a night's readings.
     */
    private static void buildLavaChannel(ServerLevel level, int gy, int cx, int mouthZ) {
        int x0 = cx + EAST_MIN;
        int x1 = cx + EAST_MAX;
        int z0 = mouthZ + ROW_B;
        int z1 = z0 + ROW_B_D;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                DebugPenManager.groundColumn(level, x, gy, z, Blocks.STONE.defaultBlockState());
            }
        }
        // The channel itself: two wide, and two DEEP, so the horse is properly
        // in the lava rather than paddling at the edge of it.
        for (int x = x0 + 2; x <= x0 + 1 + LAVA_LEN; x++) {
            for (int z = z0 + 2; z <= z0 + 3; z++) {
                poolColumn(level, x, gy, z, Blocks.LAVA.defaultBlockState());
            }
        }
        fencedPlot(level, gy, x0, x1, z0, z1);
        DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                List.of("LAVA " + LAVA_LEN + " LONG", "speed is EPIGENETIC", "ride BOTH - do they", "DIFFER? then breed"));
        stock(level, gy, x0 + 0.5, z0 + 0.5, "horsegenetics.fireproof",
                "the lava channel", 1, 1, null);
        saddleAll(level, gy, x0, x1, z0, z1);
        // The census prints the horses' actual lava_movement. That one line
        // separates the three ways this feature can fail - the attribute never
        // reached the horse, the gene never set it, or the mixin never read it
        // - and no amount of riding up and down can.
        DebugWorldWatch.watchAttribute("LAVA CHANNEL", box(x0, gy, z0, x1, gy + 1, z1),
                com.example.horsegenetics.neoforge.entity.ModAttributes.LAVA_MOVEMENT);
    }

    /** How long the crossing is. Long enough that "is this too slow" is a real question. */
    private static final int LAVA_LEN = 19;

    /**
     * A two-deep pool of {@code fluid}, dug into the floor rather than poured
     * on top of it.
     *
     * <p>Poured on top it would need walls to hold it and would flow the moment
     * anything updated; dug in, every neighbour at both levels is already solid
     * ({@code groundColumn} lays dirt at {@code gy-1}), so the sources simply
     * sit there. Stone under it rather than dirt, because a lava channel on
     * dirt is a lava channel that has burned its own floor out.
     */
    private static void poolColumn(ServerLevel level, int x, int gy, int z, BlockState fluid) {
        DebugPenManager.fastSet(level, new BlockPos(x, gy - 3, z), Blocks.BEDROCK.defaultBlockState());
        DebugPenManager.fastSet(level, new BlockPos(x, gy - 2, z), Blocks.STONE.defaultBlockState());
        DebugPenManager.fastSet(level, new BlockPos(x, gy - 1, z), fluid);
        DebugPenManager.fastSet(level, new BlockPos(x, gy, z), fluid);
    }

    /**
     * <b>Saddle everything in a pen.</b> A ridden test whose first step is
     * "find a saddle" is a ridden test that gets put off, and these two are the
     * only pens in the yard that need one - everything else is watched rather
     * than sat on.
     *
     * <p>A real saddle, not the bareback-steering phantom: this is the horse
     * being equipped, not a bond tier being simulated.
     */
    private static void saddleAll(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        for (Horse horse : level.getEntitiesOfClass(Horse.class,
                new AABB(x0, gy, z0, x1 + 1, gy + 4, z1 + 1))) {
            horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        }
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
        stock(level, gy, x, z, key, what, mares, studs, tokens, null);
    }

    /**
     * The same, on a named base coat - {@code base} is a genotype fragment
     * appended to this locus's, for a marking that would be invisible on the
     * default black horse. See {@link #PALE}.
     */
    private static void stock(ServerLevel level, int gy, double x, double z, String key,
                              String what, int mares, int studs, String tokens, String base) {
        Gene gene = Genes.byKeyOrNull(key);
        if (gene == null) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: no {} gene, {} left empty", key, what);
            return;
        }
        String pair = tokens != null ? tokens
                : gene.alleles().get(0).token() + "/" + gene.alleles().get(0).token();
        String code = gene.key() + "=" + pair + (base == null ? "" : "-" + base);
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
