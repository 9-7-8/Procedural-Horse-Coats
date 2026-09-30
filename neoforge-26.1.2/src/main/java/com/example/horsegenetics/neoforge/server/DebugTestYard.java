package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LightBlock;
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
 * owner's time in the game. So the yard is audited against the index of
 * unverified work ({@code wiki/verification.html}, generated from every page's
 * Verification tab) every time it moves, and a confirmed test's pen is
 * <b>deleted</b> rather than left standing with a tick on it.
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
 * <h2>And since 2026-09-30, it is built for a session that cannot see it</h2>
 * Owner: <i>"remove everything in the debug yard which can't be tested by you
 * without my intervention, and then add in new pens which you can test without
 * me."</i> The yard is now tested by launching the game and reading the log, so
 * every pen in it must start itself and write its own answer; the ones that
 * waited for a person were deleted and what
 * a pair of hands did for them is done by {@link DebugYardClockwork}'s
 * FakePlayer. A grep for {@code CLOCKWORK} is that half's whole report.
 */
final class DebugTestYard {

    /** How far the spur runs from the road before the yard starts. */
    private static final int PATH_LEN_Z = 30;

    /** Half-width of the spur, in blocks either side of its centre line. */
    private static final int PATH_HALF_X = 2;

    /** The yard's floor, measured from the spur's centre line and its mouth. */
    private static final int YARD_HALF_X = 24;

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
    static final int AISLE = 4;

    /** The pens stop here either side, leaving the spur's width clear end to end. */
    static final int WEST_MAX = -3;
    static final int EAST_MIN = 3;

    /** The widest a pen may be on one side of the walkway. */
    static final int BLOCK_W = 19;

    /**
     * <b>Row north walls, as offsets from the yard's mouth.</b> Each is the one before it plus its
     * depth plus an aisle, so deleting a row pulls everything behind it forward.
     *
     * <h2>Eight rows, and no gaps (owner, 2026-09-30)</h2>
     * <i>"Also remove everything that's already been verified, and remove the empty space from the
     * debug yard left over from removing other pens."</i> Every pen whose own question the wiki records
     * as answered went - checked question by question against the Verification tabs, not by gene - and
     * with it every clockwork pen, all of whose checks had passed and so closed. What was left sat in
     * twenty-odd rows, most of them a single pen wide, so the survivors were moved into each other's
     * empty halves: MET NATURAL beside THE CAP, SPLICE PERFORMANCE into the dryad row, LYCAN DOOMED and
     * DEATH DIAMONDS beside NIGHT SHY, BLOOD ONLY where WATERBORN stood.
     *
     * <p><b>The letters are kept</b>, gaps and all: every sign and every Verification tab names a pen by
     * its row, and closing the alphabet would invalidate all of it. So the yard runs O, T, U, W, X, Y,
     * AA, AL - names, not positions.
     *
     * <p>Every pen registers with {@link YardPens}, so a horse's breeding and band checks see only its
     * own pen, which is what lets them share walls (packed since 2026-09-14).
     */
    static final int PACKED_AISLE = AISLE + 1;     // a chest and a sign in front of every pen
    /** THE CAP, HURT MARE | MET NATURAL ({@link DebugYardFertility}). Twelve deep for MET NATURAL's growing herd. */
    static final int ROW_O = 3;
    static final int ROW_O_D = 12;
    /** DRYAD FLOWER | DRYAD OAK+BIRCH - on stone, so a sapling over a fence cannot live ({@link DebugYardLong}). */
    static final int ROW_T = ROW_O + ROW_O_D + PACKED_AISLE;
    static final int ROW_T_D = 10;
    /** Four inheritance-ratio pens that breed all day and tally their foals ({@link DebugYardLong}). */
    static final int ROW_U = ROW_T + ROW_T_D + PACKED_AISLE;
    static final int ROW_U_D = 10;
    /** Four more: two impossible genotypes, a milk clash and a knob-carrying colour gene. */
    static final int ROW_W = ROW_U + ROW_U_D + PACKED_AISLE;
    static final int ROW_W_D = 10;
    /** NIGHT SHY | (empty) ({@link DebugYardUnattended}). */
    static final int ROW_X = ROW_W + ROW_W_D + PACKED_AISLE;
    static final int ROW_X_D = 12;
    /** REACH WALL, REACH FENCE | STATS. */
    static final int ROW_Y = ROW_X + ROW_X_D + PACKED_AISLE;
    static final int ROW_Y_D = 12;
    /** KICK HUNTER, KICK PLAIN (lidded) | SUNTOUCHED. */
    static final int ROW_AA = ROW_Y + ROW_Y_D + PACKED_AISLE;
    static final int ROW_AA_D = 12;
    /** The arcane dealer and the string he founds ({@link DebugYardArcane}). Deeper, because he places his own herd. */
    static final int ROW_AL = ROW_AA + ROW_AA_D + PACKED_AISLE;
    static final int ROW_AL_D = 16;

    /**
     * <b>The yard's depth is the last row, not a number somebody remembered to
     * bump.</b> It was a literal until 2026-09-13 and it was wrong: the yard
     * read 110 deep while its rows chained past 150, so the back of it was
     * outside the plot box that tears the plot down and carries tamed horses
     * home. Derived now, which is the whole class of bug gone.
     */
    private static final int YARD_DEPTH_Z = ROW_AL + ROW_AL_D + AISLE;

    /** The west block's left edge, and the east block's right edge. */
    static final int WEST_MIN = WEST_MAX - BLOCK_W;
    static final int EAST_MAX = EAST_MIN + BLOCK_W;

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
        // And the verdict table, before any row registers a check in it.
        DebugYardClockwork.reset();

        buildPath(level, gy, cx, mouthZ);
        buildYardFloorAndWalls(level, gy, cx, mouthZ);
        // In yard order. The combat, hunger and hands pens and DebugYardHerd's own were deleted with
        // their classes once every question they asked was answered (2026-09-30). So are the dryad and stat pens that
        // stood in rows A and F, and every clockwork pen - DebugYardClockwork keeps its fake-player hands
        // and verdict lines for the next check that wants them, and builds nothing.
        // Row O: breeding scenarios, each pen logging its horses' breeding state to the watch.
        DebugYardFertility.build(level, gy, cx, mouthZ);
        // Rows T, U and W: the dryad row and the inheritance ratios, for a run of a whole day.
        DebugYardLong.build(level, gy, cx, mouthZ);
        // Rows X, Y and AA: open tests that need nobody at the keyboard.
        DebugYardUnattended.build(level, gy, cx, mouthZ);
        // DEATH DIAMONDS, SPLICE PERFORMANCE and BLOOD ONLY (DebugYardEffects, -Births, -Dhampir) went on
        // their own PASS lines on 2026-09-30, with LYCAN DOOMED and LETHAL FOALS: the owner ruled an automatic
        // yard PASS closes a check the way a clockwork one does.
        // Row AL west: the arcane dealer, founded on the spot with his own string.
        DebugYardArcane.build(level, gy, cx, mouthZ);

        // A sign at the junction, on the road, so the yard is discoverable by
        // somebody who walked in to look at pens and does not know it is there.
        DebugPenManager.placeSign(level, new BlockPos(cx + PATH_HALF_X + 1, gy + 1, ROAD_EDGE_Z),
                Direction.SOUTH,
                List.of("-> TEST YARD", PATH_LEN_Z + " blocks", "every pen runs", "itself - read log"));

        verify(level, gy, cx, mouthZ);
        sprint(level);
    }

    /**
     * <b>Run the yard's clocks faster than the wall clock</b> (owner, 2026-09-30: "can't you just speed up
     * the tick rate, rather than waiting an actual hour?"). Every verdict in the yard counts game ticks,
     * so a sprint changes when an answer arrives, never what it says. Opt-in through
     * {@code -PyardSprint=<ticks>} on {@code runClientTestWorld}, so a person visiting the yard is not
     * dropped into fast-forward. Vanilla's {@code /tick sprint}, which runs ticks back to back with no
     * sleep between them - so it gains exactly the server's idle time, and a yard of five hundred horses
     * that already takes most of its fifty milliseconds a tick gains little. The census's ms/tick line is
     * the reading. UNVERIFIED: that an integrated server honours a sprint the way a dedicated one does.
     */
    private static void sprint(ServerLevel level) {
        String ticks = System.getProperty("horsegenetics.yardSprint");
        if (ticks == null) {
            return;
        }
        try {
            int n = Integer.parseInt(ticks.trim());
            // true means it cut short a sprint already running. Vanilla logs its own "sprint report"
            // (ticks per second, ms per tick) when the sprint ends - that line is the speed-up, measured.
            boolean interrupted = level.getServer().tickRateManager().requestGameToSprint(n);
            ActionTrace.log("test yard", "sprinting " + n + " ticks (" + n / 1200 + " game minutes)"
                    + (interrupted ? " - replaced a sprint already running" : ""));
        } catch (NumberFormatException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: horsegenetics.yardSprint is not a tick count: {}", ticks);
        }
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
        // Name the first offender of each kind. "around x={cx}" was the plot
        // centre, not the offending spot, so the standing "1 blocked at head
        // height" was unactionable and was set aside as noise in four separate
        // sessions - which is how a real obstruction survives. Gaps 260/262: a
        // check that always complains and never says what is worse than none.
        BlockPos firstNoFloor = null;
        BlockPos firstBlocked = null;
        BlockState firstBlockedState = null;
        // Every KIND, not only the first: on 2026-09-30 the warning named one poppy
        // and said nothing of the other two spots, which is the same unactionable
        // shape gaps 260/262 were about.
        java.util.Set<String> blockedKinds = new java.util.TreeSet<>();
        for (int z = ROAD_EDGE_Z; z < mouthZ + YARD_DEPTH_Z; z++) {
            boolean inYard = z >= mouthZ;
            int halfX = inYard ? YARD_HALF_X - 1 : PATH_HALF_X;
            for (int x = cx - halfX; x <= cx + halfX; x += inYard ? 4 : 1) {
                if (level.getBlockState(new BlockPos(x, gy, z)).isAir()) {
                    noFloor++;
                    if (firstNoFloor == null) {
                        firstNoFloor = new BlockPos(x, gy, z);
                    }
                }
                BlockState head = level.getBlockState(new BlockPos(x, gy + 1, z));
                if (!head.isAir() && !isFurniture(level, x, gy + 1, z)) {
                    blocked++;
                    blockedKinds.add(BuiltInRegistries.BLOCK.getKey(head.getBlock()).toString());
                    if (firstBlocked == null) {
                        firstBlocked = new BlockPos(x, gy + 1, z);
                        firstBlockedState = head;
                    }
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
            HorseGenetics.LOGGER.warn("[Debug] test yard is NOT sound: {} spot(s) with no floor"
                    + "{}, {} blocked at head height{}. Plot centre x={}.",
                    noFloor,
                    firstNoFloor == null ? "" : " (first at " + firstNoFloor.toShortString() + ")",
                    blocked,
                    firstBlocked == null ? "" : " (first at " + firstBlocked.toShortString()
                            + ", which is " + BuiltInRegistries.BLOCK.getKey(
                                    firstBlockedState.getBlock()) + "; kinds " + blockedKinds + ")",
                    cx);
        }
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
                || state.is(Blocks.SPAWNER) || state.is(Blocks.OAK_PLANKS)
                // The gameplay rows put real furniture in the yard for the
                // first time. Without these the walkability check reports a
                // sound yard as broken, which is worse than not checking: a
                // warning nobody can act on is a warning that gets ignored the
                // next time it is real.
                || state.is(Blocks.CHEST) || state.is(Blocks.HAY_BLOCK)
                // BOTH OF THESE ARE WALK-THROUGH, and leaving them out made the
                // check report a sound yard as broken on its first build: 0
                // spots with no floor and 22 "blocked at head height", every
                // one of them either the invisible light blocks that fill a
                // dark room's floor layer or the ocean-born pool. A warning
                // nobody can act on is worse than no warning - it is the one
                // that gets ignored the next time it is real.
                || state.is(Blocks.LIGHT) || state.is(Blocks.WATER)
                // The fertility rows give every pen a flush water cauldron, and it
                // was the whole of the standing "1 blocked at head height" warning -
                // named at last by the position this check now logs (-16, 129, 414).
                // Deliberate furniture, so the check was crying wolf, which is the
                // third of the possibilities gap 262 listed. Gaps 260/262.
                || state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON)
                || state.is(Blocks.LAVA_CAULDRON) || state.is(Blocks.POWDER_SNOW_CAULDRON)
                // The clockwork rows' monster cells and horse ring, and the blight pen's
                // planted strip - all deliberate, none of it a hole in the yard.
                || state.is(Blocks.GLASS) || state.is(Blocks.WHEAT) || state.is(Blocks.POTTED_DANDELION)
                || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.DANDELION)
                // HUNGER ORDER's menu. The grid is sampled every fourth block, and it only
                // started landing on these when the rows ahead of them were deleted.
                || state.is(Blocks.POPPY) || state.is(Blocks.CAKE)
                // REACH WALL's stone divider (row Y west), the wall no cover may cross -
                // under the grid for the same reason, the second build after the cut.
                || state.is(Blocks.STONE)
                || state.is(com.example.horsegenetics.neoforge.block.ModBlocks.RESEARCH_SHELF.get())
                || state.is(com.example.horsegenetics.neoforge.block.ModBlocks.LEATHERWORKERS_POST.get())
                || state.is(com.example.horsegenetics.neoforge.block.ModBlocks.SCIENTISTS_POST.get())
                || state.is(com.example.horsegenetics.neoforge.block.ModBlocks.SUPPLIERS_POST.get())
                || state.is(com.example.horsegenetics.neoforge.block.ModBlocks.METALSMITHS_POST.get())
                || state.is(com.example.horsegenetics.neoforge.block.ModBlocks.COWBOY_HITCH.get());
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

    /**
     * <b>The bone-meal pen is gone, and this note is what it produced.</b>
     *
     * <p>Its pass condition was stated when it was built and it met it exactly:
     * <i>"what settles it is the list of what the gene DID touch - one line per
     * fertilising - and a night of those with no crop among them is the
     * pass."</i> Over 2026-09-13 that list was <b>eight fertilisings, every one
     * of them {@code minecraft:grass_block}, and not one crop</b>, while the
     * strip of wheat on farmland beside it sat at eighteen all day. The pen also
     * hurried saplings into trees on its own: the oak-log count climbed from 5
     * to 51 across the day, one tree at a time, without anybody in the room.
     *
     * <p>Kept as a comment rather than a pen because the <em>refusal</em> is the
     * interesting half and it is now recorded: {@code noteBoneMeal} is still
     * hooked, so a future regression shows up as a crop in that list from
     * anywhere in the dimension, which is broader than this pen ever was.
     */
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

        // THE YARD IS LIT NOW, AND IT HAS TO BE. Two changes landed a day
        // apart and together they are a hazard: the dimension got a biome that
        // spawns zombies in the dark (2026-09-13), and the rule that made
        // horses invulnerable here is gone (2026-09-13, owner: "remove the 'no
        // horse damage' exception in the yard"). Before either, a dark yard
        // cost nothing. After both, every pen left running overnight is a pen
        // whose subject can be eaten - and a dead horse reports nothing at all,
        // so the morning's log would read as the gene having stopped.
        //
        // Invisible light blocks rather than lamp posts: full brightness, no
        // collision, nothing in the way of a fleeing horse or a sight line,
        // and nothing added to what the walkability check has to forgive. The
        // wall glowstone only ever reached a few blocks in from the edges and
        // this yard is forty-eight wide.
        //
        // It does NOT break the tests that want monsters. Every one of those
        // has its own spawner - the ward room, both arenas, the cleansing
        // light pen - and a spawner does not care about light. What it stops
        // is the thing nobody asked for: hostiles appearing in the middle of a
        // dryad pen (known-gaps, gap 212).
        // FOUR APART AND TWO UP, and both numbers are load-bearing. Light falls
        // off one per block by taxicab distance, so a level-15 source on a
        // six-block grid at gy+4 bottoms out around SIX at the floor - and six
        // is below GeneAbilityHandler.DARK_LEVEL, which is 7. That would have
        // left the eyesight pen's LIT half reading as dark to the very gene it
        // is the control for: a pen that agrees with its own experiment for the
        // wrong reason, which is the worst kind of green. At gy+3 on a
        // four-block grid the floor never drops below nine.
        BlockState lamp = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        for (int z = z0 + 2; z < z1; z += 4) {
            for (int x = cx - YARD_HALF_X + 2; x < cx + YARD_HALF_X; x += 4) {
                DebugPenManager.fastSet(level, new BlockPos(x, gy + 3, z), lamp);
            }
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
     * {@code DebugPenManager.countOwnedTamedHorses}, which counts what a leaving
     * player is about to lose and must not count the scenery. (It used to walk
     * those horses home; it does not any more, but the owner-vs-scenery line is
     * the same line and matters for the same reason.)
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
    static void stock(ServerLevel level, int gy, double x, double z, String key,
                              String what, int mares, int studs, String tokens) {
        stock(level, gy, x, z, key, what, mares, studs, tokens, null);
    }

    /**
     * The same, on a named base coat - {@code base} is a genotype fragment
     * appended to this locus's, for a marking that would be invisible on the
     * default black horse - which is where a dark marking vanishes (the owner, 2026-09-11: a barred
     * wing and a nightbell foxglove); chestnut with one cream copy,
     * {@code horsegenetics.extension=e/e-horsegenetics.matp=Cr/N}, shows both dark and white marks.
     */
    static void stock(ServerLevel level, int gy, double x, double z, String key,
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
                label(DebugPenManager.spawnHorse(level, gy + 1, x + placed++ * 1.5, z,
                        Sex.FEMALE, code, true), what);
            }
            for (int i = 0; i < studs; i++) {
                label(DebugPenManager.spawnHorse(level, gy + 1, x + placed++ * 1.5, z,
                        Sex.MALE, code, true), what);
            }
            ActionTrace.log("test yard", "stocked " + what + " with " + placed + "x " + code
                    + " (" + mares + " mare, " + studs + " stallion), tamed");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: could not stock {}", what, e);
        }
    }

    /**
     * <b>Write what a horse IS on the horse, not only on the pen's sign.</b>
     *
     * <p>Owner, 2026-09-13: <i>"can you change the names of the watcher horses
     * after generation to describe what they are? I need to let them out of
     * their pens to fully test."</i> Which is the right way to test half of
     * these - a stalker in a nine-block stall cannot really stalk, and a gene
     * about where it stands relative to you needs room to stand.
     *
     * <p>The sign is the only label a stocked horse has, and it stops being
     * attached to anything the moment the gate opens. So the name goes on the
     * animal and stays visible: five watchers loose in one yard are
     * indistinguishable otherwise, and telling them apart is the entire test.
     */
    static void label(Horse horse, String what) {
        if (horse == null) {
            return;
        }
        horse.setCustomName(Component.literal(what));
        horse.setCustomNameVisible(true);
    }


    /** A watch box, written the way a pen is: two corners in block coordinates. */
    static AABB box(int x0, int y0, int z0, int x1, int y1, int z1) {
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
    static void fencedPlot(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        DebugPenManager.penWalls(level, gy + 1, x0, x1, z0, z1,
                (x0 + x1) / 2, z0, Direction.NORTH);
        penWater(level, gy, x0 + 1, z0 + 1);
    }

    /**
     * <b>A glass cage over a {@link #fencedPlot}, for a pen whose horse will be brought low.</b>
     *
     * <p>Last Stand is doing its job when a horse near death bolts: {@code HorseEscapeGoal} adds
     * {@code Escape.JUMP_BOOST} to its jump and presses jump at whatever blocks it, so a fence is no
     * wall to it. KICK HUNTER found this on 2026-09-30 - at 3/22 it bolted from the husks and was out
     * of the pen within twenty seconds, and the census read "horses 0" for the rest of the run (owner:
     * "this is the Last Stand firing as intended ... You have to raise the walls"). A boosted jump
     * clears more than any wall this yard would build, so the pen gets a lid: glass two high on the
     * walls and a roof at {@code gy + 4}, above the lamps. The gate column is left open at
     * {@code gy + 2} so a person can still walk in; a one-high gap is no way out for a horse. The corner
     * torches stay, and so does any lamp a wall line crosses - a one-block hole is no way out either.
     */
    static void lidded(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
        BlockState glass = Blocks.GLASS.defaultBlockState();
        int gateX = (x0 + x1) / 2;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                boolean gate = z == z0 && (x == gateX || x == gateX + 1);
                if (edge && !corner && !gate) {
                    DebugPenManager.fastSet(level, new BlockPos(x, gy + 2, z), glass);
                }
                // Not over a lamp: the yard's light blocks stand at gy + 3, four apart, and a wall that
                // ate one would leave the pen dark enough to spawn in.
                BlockPos wall = new BlockPos(x, gy + 3, z);
                if (edge && !level.getBlockState(wall).is(Blocks.LIGHT)) {
                    DebugPenManager.fastSet(level, wall, glass);
                }
                DebugPenManager.fastSet(level, new BlockPos(x, gy + 4, z), glass);
            }
        }
    }

    /**
     * <b>Every pen has water, sunk into its floor.</b> Owner, 2026-09-16: water is
     * standard in a pen from here on, in the ones that exist and the ones nobody has
     * written yet - which is why it lives in {@link #fencedPlot} rather than in each
     * pen that remembered to ask for it.
     *
     * <p>A dry pen was not a cosmetic gap. Healing is gated on standing near water
     * ({@code HorseCareHandler.nearWater}), so a hurt horse in a dry pen never got the
     * point back; and a natural cover needs a healthy horse, so a mare who lost a
     * pregnancy to a lethal pairing - half a heart, from the miscarriage - was never
     * covered again, in silence, for the rest of the run. Gap 258.
     *
     * <p><b>Sunk to {@code gy}, not raised to {@code gy + 1}</b>, for the reason the
     * corridor pens already sink theirs: a block standing proud of the floor is a step,
     * and an animal beside a wall uses it to hop out. That is also why it is exempt
     * from the two-clear-blocks rule (2026-09-15, formerly gap 247), which governs
     * <i>raised</i> blocks - flush with the grass this is not a step, so it may sit in
     * the corner of even a four-wide pen, where two clear blocks would not fit.
     */
    /**
     * <b>The one exception to water in every pen: a pen whose test is a horse that must STAY hurt.</b>
     *
     * <p>Since 2026-09-14 healing is paid for by hunger and gated only on water within
     * {@code HorseCareHandler.HEAL_SCAN_RADIUS} - food underfoot stopped mattering. So the stone floors
     * that kept the old dairy's mare hurt did nothing any more, and the infirmary's "the patients must
     * not heal" control could only fail: two of its three patients were back at full health within
     * five minutes, from the pen's own cauldron (2026-09-30, the first clockwork run). Undoes
     * {@link #penWater} for a pen built by {@link #fencedPlot} at {@code (x0, z0)}.
     */
    static void dryPen(ServerLevel level, int gy, int x0, int z0) {
        DebugPenManager.groundColumn(level, x0 + 1, gy, z0 + 1, Blocks.GRASS_BLOCK.defaultBlockState());
    }

    static void penWater(ServerLevel level, int gy, int x, int z) {
        level.setBlockAndUpdate(new BlockPos(x, gy, z),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
    }

}
