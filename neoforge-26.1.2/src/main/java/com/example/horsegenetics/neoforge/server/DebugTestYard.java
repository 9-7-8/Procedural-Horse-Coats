package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    /** Row north walls, as offsets from the yard's mouth. Each is the one before it plus its depth plus an aisle. */
    private static final int ROW_A = 3;                         // the two dryad pens | spawner room
    private static final int ROW_A_D = 16;
    // B, C AND D ARE THE GAMEPLAY LAYER, which had never been in this yard at
    // all. Owner, 2026-09-13: "add the entire item and gameplay layer as well
    // as villagers. Add literally everything that CAN be tested in the yard."
    // Until now every item test lived in the test kit's hotbar - which is to
    // say it needed a person holding the right nine things, and got put off.
    // A room with the props already placed and a chest of the items beside
    // them costs a build and is then free for ever.
    static final int ROW_B = ROW_A + ROW_A_D + AISLE;    // tack room | horseman's study
    static final int ROW_B_D = 14;
    // ROW C IS THE TICKET STALLS, BOTH BLOCKS. Owner on first sight: "stall
    // testing is too close to another pen, please put more space." It was: the
    // holding pen and four awkward stalls were packed into one 19-wide block
    // with single-block gaps, and a stall you have to squeeze past is a stall
    // whose bind you cannot judge. The carrots moved out to rows L, M and N,
    // which is what freed the east block.
    static final int ROW_C = ROW_B + ROW_B_D + AISLE;    // the ticket stalls, both blocks
    static final int ROW_C_D = 14;
    static final int ROW_D = ROW_C + ROW_C_D + AISLE;    // dairy and clip | egg layer
    static final int ROW_D_D = 12;
    static final int ROW_E = ROW_D + ROW_D_D + AISLE;   // crackle | food preference
    static final int ROW_E_D = 7;
    private static final int ROW_F = ROW_E + ROW_E_D + AISLE;   // starburst, F8 | the three stat pens
    private static final int ROW_F_D = 12;
    // G AND H ARE GONE, and the yard is seventeen blocks shorter for it.
    //
    // G was the growing row and had four slots. Verdant's three were confirmed
    // and deleted on 2026-09-12; oak/birch and bone meal went the same way on
    // 2026-09-13, which left TWO pens holding a nine-deep row and a four-block
    // aisle open. They moved into row A's west block, which the bone-meal pen
    // had just vacated and which is sixteen deep against their nine.
    //
    // H had been a row of depth ZERO since the retinue pen was deleted (a flat
    // 50.0 ms/tick with twenty-four re-pathing mobs, which answered the
    // question outright) - so it built nothing and still cost an aisle, which
    // is the purest dead space this yard had.
    //
    // Nothing else had to move: the rows chain off each other, so deleting a
    // depth pulls every row behind it forward on its own. That is the whole
    // reason the chain is written this way.
    static final int ROW_I = ROW_F + ROW_F_D + AISLE;   // dhampir | eyesight
    static final int ROW_I_D = 20;
    /**
     * <b>J and K are what removing the no-damage rule unlocked.</b>
     *
     * <p>Six genes - guardian, gladiator, healer, cleansing light, ender echo,
     * and both of the death loci - are <i>entirely</i> about a horse taking or
     * dealing damage, and this dimension cancelled every point of it. So they
     * have never been in the yard, because there was nothing a pen here could
     * have shown. Owner, 2026-09-13: <i>"remove the 'no horse damage'
     * exception in the yard"</i>. That was the fifth time a protection in this
     * dimension turned out to be the reason a gene "did nothing" - after the
     * mob deleter, the spread verb, the missing night and the dhampir's
     * sunburn - and it is the last of them.
     *
     * <p>They are put at the <b>far end</b> on purpose. Everything in J and K
     * either fights, explodes, or floods its own floor, and the rest of the
     * yard is full of pens whose whole result is a count of things that were
     * standing quietly.
     */
    static final int ROW_J = ROW_I + ROW_I_D + AISLE;   // the arena | the infirmary
    static final int ROW_J_D = 20;
    static final int ROW_K = ROW_J + ROW_J_D + AISLE;   // the deathbed | ender echo
    static final int ROW_K_D = 16;

    /**
     * <b>L, M and N are one breeding pair per splice carrot.</b>
     *
     * <p>Owner, 2026-09-13: <i>"we need to structure the gene splice carrots
     * better so that it's two horses to breed, and one small pair of pens per
     * carrot."</i>
     *
     * <p>The bench they replace was one pen, four horses and a chest holding
     * all eleven carrots, and it could not answer anything. <b>A splice carrot
     * biases the gamete of the parent that ate it</b>, so the result is not
     * visible on that horse at all - it is visible in a <i>foal</i>, which
     * means every test needs a named mare, a named stallion, and certainty
     * about which carrot went into which. One pen with a shared chest gives you
     * none of that: feed two carrots and the horse carries both
     * ({@code ArmedCarrotsAttachment.plus}), and the foal cannot tell you which
     * one it came from.
     *
     * <p>So each carrot gets a pair of pens with a gate between them, its own
     * chest, and nothing else in reach.
     */
    static final int ROW_L = ROW_K + ROW_K_D + AISLE;   // splice carrots 1-4
    static final int ROW_L_D = 10;
    static final int ROW_M = ROW_L + ROW_L_D + AISLE;   // splice carrots 5-8
    static final int ROW_M_D = 10;
    static final int ROW_N = ROW_M + ROW_M_D + AISLE;   // stabilizer, magnifier
    static final int ROW_N_D = 10;

    /**
     * <b>Rows O-R: breeding (O-P, and MET NATURAL in R) and band life (Q-R)</b>, built
     * by {@link DebugYardFertility} and {@link DebugYardHerd}. <b>Packed since
     * 2026-09-14</b> (owner: "condense down the testing area"): every pen registers
     * with {@link YardPens}, so a horse's breeding and band checks see only its own
     * pen. Before that these rows ran more than three hundred blocks deep, because
     * those checks count neighbours by distance through walls - a cap of eight within
     * sixteen, a 64-block dispersal search - and a packed pen would have read its
     * neighbours. Pens that are fed golden carrots still keep an air gap: vanilla's
     * breeding goal is not one of those checks. Six pens confirmed the same morning
     * were deleted rather than packed.
     */
    static final int PACKED_AISLE = AISLE + 1;     // a chest and a sign in front of every pen
    static final int ROW_O = ROW_N + ROW_N_D + PACKED_AISLE;
    static final int ROW_O_D = 8;
    static final int ROW_P = ROW_O + ROW_O_D + PACKED_AISLE;
    static final int ROW_P_D = 8;
    static final int ROW_Q = ROW_P + ROW_P_D + PACKED_AISLE;
    static final int ROW_Q_D = 9;
    static final int ROW_R = ROW_Q + ROW_Q_D + PACKED_AISLE;
    static final int ROW_R_D = 12;

    /**
     * <b>Rows S-U: the all-day pens</b>, built by {@link DebugYardLong} (owner,
     * 2026-09-14: "I'm going to leave this up all day, so add more pens which benefit
     * from being run for a very long time"). S and T are dryad pens on a stone floor, so
     * a sapling planted over the fence cannot survive to be miscounted; U is four
     * inheritance-ratio pens that breed all day and tally their foals.
     */
    static final int ROW_S = ROW_R + ROW_R_D + PACKED_AISLE;
    static final int ROW_S_D = 9;
    static final int ROW_T = ROW_S + ROW_S_D + PACKED_AISLE;
    static final int ROW_T_D = 9;
    static final int ROW_U = ROW_T + ROW_T_D + PACKED_AISLE;
    static final int ROW_U_D = 10;

    /**
     * <b>Row V: hunger</b>, built by {@link DebugYardHunger} (owner, 2026-09-14). The food
     * order, a starving horse that must not heal, a fed one that must, and a carnivore's hunt,
     * each logging its own answer.
     */
    static final int ROW_V = ROW_U + ROW_U_D + PACKED_AISLE;
    static final int ROW_V_D = 10;

    /**
     * <b>The yard's depth is the last row, not a number somebody remembered to
     * bump.</b> It was a literal until 2026-09-13 and it was wrong: the yard
     * read 110 deep while its rows chained past 150, so the back of it was
     * outside the plot box that tears the plot down and carries tamed horses
     * home. Derived now, which is the whole class of bug gone.
     */
    private static final int YARD_DEPTH_Z = ROW_V + ROW_V_D + AISLE;

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

        buildPath(level, gy, cx, mouthZ);
        buildYardFloorAndWalls(level, gy, cx, mouthZ);
        buildSpawnerRoom(level, gy, cx, mouthZ);
        buildNightBlock(level, gy, cx, mouthZ);
        buildStatPens(level, gy, cx, mouthZ);
        buildBaseAlarmPen(level, gy, cx, mouthZ);
        buildStockedRow(level, gy, cx, mouthZ);
        buildGrowingRow(level, gy, cx, mouthZ);

        // ROW I WEST IS EMPTY: dhampir is confirmed, all three of it. Owner,
        // 2026-09-13: "dhampir successfully flees the sun" - which was the last
        // question it had, after the burning and the feeding were confirmed
        // earlier the same day. The pen went away once before on a weaker claim
        // and had to be put back within the hour; this time the whole cycle is
        // sun to shelter to bite to heal, and the log shows it holding shade at
        // 66/66 rather than bleeding out a journey at a time.
        buildDisplayRow(level, gy, cx, mouthZ);

        // The two halves of the mod this yard never had: the item, block and
        // villager layer (rows B, C, D and E-east), and the six genes that
        // removing the no-damage rule unlocked (rows I-east, J and K).
        DebugYardGameplay.build(level, gy, cx, mouthZ);
        DebugYardCombat.build(level, gy, cx, mouthZ);
        // Rows O-T: every breeding and fertility scenario, self-running where it
        // can be, each pen logging its horses' breeding state to the watch.
        DebugYardFertility.build(level, gy, cx, mouthZ);
        // Rows U-X: band life, every test starting itself on a clock.
        DebugYardHerd.build(level, gy, cx, mouthZ);
        // Rows S-U: dryad pens and inheritance ratios, for a run of a whole day.
        DebugYardLong.build(level, gy, cx, mouthZ);
        // Row V: hunger - the food order, starving and fed healing, a carnivore's hunt.
        DebugYardHunger.build(level, gy, cx, mouthZ);

        // A sign at the junction, on the road, so the yard is discoverable by
        // somebody who walked in to look at pens and does not know it is there.
        DebugPenManager.placeSign(level, new BlockPos(cx + PATH_HALF_X + 1, gy + 1, ROAD_EDGE_Z),
                Direction.SOUTH,
                List.of("-> TEST YARD", PATH_LEN_Z + " blocks", "items, villagers,", "arenas, breeding"));

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
    static void spawnCow(ServerLevel level, int gy, double x, double z) {
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
                || state.is(com.example.horsegenetics.neoforge.block.ModBlocks.RESEARCH_SHELF.get())
                || state.is(com.example.horsegenetics.neoforge.block.ModBlocks.HORSEMANS_TABLE.get())
                || state.is(com.example.horsegenetics.neoforge.block.ModBlocks.COWBOY_HITCH.get());
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
    /**
     * <b>What is left of the night block: one stall.</b>
     *
     * <p>Every variant of both night loci is confirmed - eight tempers and five
     * watchers - and all thirteen stalls are gone. &sect;0-AT had listed the
     * family as unwatchable since 2026-09-09, and it went from nothing to
     * nothing-left inside a day once the dimension had a night to run them in.
     *
     * <p>Dhampir is the exception and stays. The only sighting of it so far is
     * "the dhampir did damage a cow, though I'm not sure what else that did" -
     * which is a thing happening near the horse rather than a confirmation, and
     * its neighbour at the time was a night-hunter with a cow of its own.</p>
     */
    private static void buildNightBlock(ServerLevel level, int gy, int cx, int mouthZ) {
    }

    /**
     * <b>The three magic stat loci, which the attribute readout just made
     * testable for nothing.</b>
     *
     * <p>&sect;0d has carried magic speed, health and jump as "NOT play-tested"
     * since 2026-09-05, and they were awkward for the same reason the weather
     * loci were: their entire effect is a number on the horse, and looking at a
     * horse does not show you a number. The moment the census could print a
     * named attribute's range across a pen, the test became free - so these
     * three cost one method rather than a session.
     *
     * <p><b>Unlike weather they are unconditional</b>, so there is no second
     * reading to take: the value is either moved off the breed's baseline or it
     * is not. What the range across two horses <i>also</i> shows is whether the
     * magnitude is per allele copy, which is the half nobody could see.
     */
    private static void buildStatPens(ServerLevel level, int gy, int cx, int mouthZ) {
        String[][] pens = {
                {"horsegenetics.magic_speed", "MAGIC SPEED", "speed"},
                {"horsegenetics.magic_health", "MAGIC HEALTH", "health"},
                {"horsegenetics.magic_jump", "MAGIC JUMP", "jump"}};
        int z0 = mouthZ + ROW_F;
        int z1 = z0 + 9;
        for (int i = 0; i < pens.length; i++) {
            // SIX WIDE, not seven: three sevens plus their gaps ran one block
            // past EAST_MAX and into the strip that keeps the yard's own wall
            // reachable. Caught by the layout audit rather than in game, which
            // is the only way a one-block overrun ever gets caught.
            int x0 = cx + EAST_MIN + i * 7;
            int x1 = x0 + 5;
            fencedPlot(level, gy, x0, x1, z0, z1);
            DebugPenManager.placeSign(level, new BlockPos(x0 + 1, gy + 1, z0 - 1), Direction.NORTH,
                    List.of(pens[i][1], "the census prints", "the number. Is it", "off baseline?"));
            stock(level, gy, x0 + 2.0, (z0 + z1) / 2.0, pens[i][0], pens[i][1], 2, 0, null);
            DebugWorldWatch.watchAttribute(pens[i][1], box(x0, gy, z0, x1, gy + 1, z1),
                    "speed".equals(pens[i][2]) ? Attributes.MOVEMENT_SPEED
                            : "health".equals(pens[i][2]) ? Attributes.MAX_HEALTH
                            : Attributes.JUMP_STRENGTH);
        }
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
        // ROW A'S WEST BLOCK, not a row of its own. Two pens do not need
        // nine blocks of depth and an aisle to themselves when row A's west
        // half is standing empty - the bone-meal pen was there and is
        // confirmed - and A is sixteen deep against these nine.
        int z = mouthZ + ROW_A;
        // Both west. The east block of this row is the spawner room and the
        // ward post, and nothing may cross the centre walkway: WEST_MAX and
        // EAST_MIN are what keep every gate reachable from the corridor door.
        int[] at = {cx + WEST_MIN, cx + WEST_MIN + 10};
        int slot = 0;
        int x = at[slot];

        // Verdant's three are CONFIRMED (2026-09-12) and their pens are gone.
        // What is left is the row of slow ones - the tests that cannot be
        // answered by standing still and looking, only by leaving and coming
        // back, which is exactly what this dimension is for.
        //
        // TWO DRYAD PENS LEFT of the four this row had on 2026-09-13, and the
        // two that went were both answered out of one night's log rather than
        // by looking at anything.
        //
        // DRYAD MIXED is gone: a heterozygote plants BOTH of its alleles, which
        // was the new inheritance rule and the only thing that pen was for. The
        // watch counted oak and birch separately and saw both appear, and one
        // of the oaks closed the loop by becoming a tree (6 logs, 53 leaves).
        // What it did NOT establish is the half-rate RATIO - the counts were
        // ones and twos - and a pen is the wrong instrument for a rate anyway;
        // that belongs with gaps 208 and 211, which are about exactly that.
        //
        // DRYAD BONE is gone with it - see the deleted buildBoneMealPen.

        // 1. THE DARK OAK, which is the whole reason the locus was rebuilt.
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

        // 2. THE MUSHROOM, and its floor is the test. Half podzol, half grass:
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
        // TWO WATCH AREAS OVER ONE PEN, because one could not answer the
        // question the pen was built to ask. The pass is "mushrooms on the
        // podzol half and NONE on the grass half", and a single tally reading
        // "1x red_mushroom" for the whole pen says a mushroom exists somewhere
        // and nothing about which half it is on - so the canSurvive check, the
        // entire point of the floor being split, was unreadable. It read that
        // way all of 2026-09-13. Now the two halves report separately and the
        // pass is the shape of the pair, not a number that needs interpreting.
        int mid = x + (PEN_W / 2);
        DebugWorldWatch.watch("DRYAD MUSH - PODZOL (should fill)",
                box(x, gy, z, mid, gy + 1, z + PEN_D), null,
                Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.PODZOL);
        DebugWorldWatch.watch("DRYAD MUSH - GRASS (must stay empty)",
                box(mid + 1, gy, z, x + PEN_W, gy + 1, z + PEN_D), null,
                Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.GRASS_BLOCK);
    }

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

        // THE LOOK IS CONFIRMED (2026-09-13, "starburst looks fine"). What is
        // left is not a visual question at all: the emblem's size rides the
        // gene's DIAL, drawn per allele copy, so a foal's should sit near its
        // parents' rather than re-rolling. That is the only thing the dial has
        // ever claimed and nothing has tested it - hence three mares and a
        // stallion, and golden carrots in batch 1.
        stockedPen(level, gy, x, z, "horsegenetics.starburst", "W/W", 3, 1,
                List.of("STARBURST", "LOOK is confirmed.", "BREED a pair: foal", "like its parents?"));
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
    static void darkRoom(ServerLevel level, int gy, int x0, int x1, int z0, int z1,
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
    static void darkRoom(ServerLevel level, int gy, int x0, int x1, int z0, int z1,
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
        //
        // EVERY FLOOR BLOCK, at head height, and that is the whole fix. The
        // first version put nine of these at gy+4 on a four-block grid and it
        // did nothing at all: light falls off by one per block, so a source of
        // level ONE lights its own position and nothing else. The room read
        // zero at the floor and kept spawning - the log from 2026-09-13 has
        // natural zombies in the ward room by the dozen, which is exactly what
        // this was written to prevent and exactly what it failed to. There is
        // no spacing that works for a level-1 source; the only grid that
        // covers a floor is the floor.
        if (spawnProof) {
            BlockState dim = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 1);
            for (int x = x0 + 1; x < x1; x++) {
                for (int z = z0 + 1; z < z1; z++) {
                    DebugPenManager.fastSet(level, new BlockPos(x, gy + 1, z), dim);
                }
            }
        }
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
        // A PEN FOR THE WARD POST, because it was the loose horse. Owner,
        // 2026-09-13: "also there's a loose horse." This one, and it had always
        // been loose - it is deliberately OUTSIDE the spawner room (a horse
        // shut in a dark box with a spawner is a horse being hit by zombies)
        // and nothing ever fenced it. That cost nothing while it stood where it
        // was put; the new safe-spawn search nudged it three blocks clear of
        // the sign it had been placed inside, and a tamed horse with somewhere
        // to walk walks. It still stands outside the door, which is the part
        // the test needs - it just cannot wander off now.
        // BESIDE the room, not in front of it: row A begins three blocks from
        // the yard's mouth and a pen needs more than that, so the first
        // attempt at this put the ward post OUTSIDE the yard's north wall
        // entirely. Caught by the layout audit rather than in game, which is
        // the second time that script has earned its keep.
        int wx0 = cx + EAST_MIN + 14;
        int wx1 = cx + EAST_MAX;
        fencedPlot(level, gy, wx0, wx1, z0, z1 - 4);
        DebugPenManager.placeSign(level, new BlockPos(wx0 + 2, gy + 1, z0 - 1), Direction.NORTH,
                List.of("WARD: NO HARM", "STAND HERE - a", "spawner needs you", "within 16 blocks"));
        stock(level, gy, wx0 + 3.0, z0 + 4.0, "horsegenetics.holy_ward", "THE WARD POST", 1, 0, null);
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
    static final String PALE = "horsegenetics.extension=e/e-horsegenetics.matp=Cr/N";

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
        // Ooze is CONFIRMED in both its forms now - plain "looks GREAT" and
        // coloured "looks perfect" - so its stall is gone and the row is one.
        List<Look> row = List.of(
                new Look("horsegenetics.gilded_crackle", "CRACKLE - BLACK",
                        "redrawn: 3 seams", "now. Shiny enough?", "Gck"));

        int z0 = mouthZ + ROW_E;
        int z1 = z0 + ROW_E_D;
        int[] starts = {cx + WEST_MIN};
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
     * <b>Saddle everything in a pen.</b> A ridden test whose first step is
     * "find a saddle" is a ridden test that gets put off, and these two are the
     * only pens in the yard that need one - everything else is watched rather
     * than sat on.
     *
     * <p>A real saddle, not the bareback-steering phantom: this is the horse
     * being equipped, not a bond tier being simulated.
     */
    static void saddleAll(ServerLevel level, int gy, int x0, int x1, int z0, int z1) {
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
    static void stock(ServerLevel level, int gy, double x, double z, String key,
                              String what, int mares, int studs, String tokens) {
        stock(level, gy, x, z, key, what, mares, studs, tokens, null);
    }

    /**
     * The same, on a named base coat - {@code base} is a genotype fragment
     * appended to this locus's, for a marking that would be invisible on the
     * default black horse. See {@link #PALE}.
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
    }

}
