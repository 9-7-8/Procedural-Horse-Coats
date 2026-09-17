package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.CoatCheckPlan;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.block.HayPortalBlock;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds and populates the horse dimension: a corridor with a <b>different
 * question down each side</b>. All calls happen on the server thread.
 *
 * <h2>The two columns</h2>
 * Walking in from the portal:
 * <ul>
 *   <li><b>On your right</b> ({@code +Z}, {@link #NORTH_PEN}) is the <b>coat
 *       check</b> - {@link CoatCheckPlan}, one pen per allele combination of
 *       every gene whose wiki page says nobody has confirmed it by eye yet,
 *       worst-first. Each pen holds <b>four</b> horses carrying that one
 *       combination on a black, a bay, a chestnut and a white base, so a
 *       marking that only works on one of them is obvious rather than
 *       discoverable. The signs on the road say what it is meant to look
 *       like.</li>
 *   <li><b>On your left</b> ({@code -Z}, {@link #SOUTH_PEN}) is the <b>breed
 *       book</b> - every breed in the game in alphabetical order, a mare and a
 *       stallion each, looping for as long as the right-hand column runs.</li>
 * </ul>
 *
 * <p>This used to be a <b>gallery of the genotype catalogue</b>: one pen per
 * visually distinct genotype, in odometer order, with the corridor length
 * derived from {@code GenotypeCatalog}. That premise stopped being buildable
 * long before it stopped being computable - every gene multiplies the
 * catalogue, the white-pattern loci alone put it past two million, and at seven
 * blocks a segment that is a corridor a quarter of the way to the world border.
 * Then it was two thousand random pens, which had the opposite problem: it never
 * ran out, and so it never told you anything was <i>left</i>. The catalogue
 * itself stays - it is still what a punnett display and the tests want, it just
 * no longer drives the dimension.
 *
 * <p>The corridor is as long as {@link #COAT_CHECK_PENS}, which is derived from
 * the plan and therefore <b>shrinks as the mod is tested</b>: confirm a gene,
 * write its {@code Verified} block, re-bake, and its pens leave. Pens are built
 * lazily as the player goes ({@link #ensureGeneratedAheadOfPlayer}) and the
 * corridor closes in an end wall.
 *
 * <h2>Instancing</h2>
 * The dimension is a flat <b>void</b> (see {@code dimension/debug_pens.json}):
 * the generator lays down nothing at all. Every visit gets its own private
 * <b>plot</b> - a corridor built by this class at its own X, {@value
 * #PLOT_SPACING_X} blocks clear of every other live plot, so "two people never
 * end up in the same place" holds even on a shared server.
 *
 * <p>When the plot's player leaves (dimension change, logout, or a re-entry
 * that supersedes it) {@link #tearDown} discards every non-player entity in it
 * and forgets those horses' ancestry records - but <b>leaves the blocks
 * standing</b>. Leaving is therefore O(entities), not O(blocks walked).
 *
 * <p>That is safe because the <b>geometry</b> is fixed for a given build
 * ({@link #COAT_CHECK_PENS} segments, fixed {@link #PLOT_BASE_Y}), so a plot
 * rebuilt on a recycled X lands exactly on the old one; and the <b>contents</b>
 * are now deterministic too - pen <i>n</i> is the same coat check and the same
 * breed every time, which the random corridor could not promise. Every pen a
 * player can reach is rebuilt from index 0 upward as they walk, signs and horses
 * included, so nothing stale is ever visible. The only leftovers are pens past
 * the new player's frontier, which they would have to walk to - and walking
 * there rebuilds them. {@link #penShell} clears any untamed horse it finds
 * before stocking, so a previous occupant's animals cannot outlive their sign.
 *
 * <h2>Layout of one plot</h2>
 * A straight corridor running +X from {@code originX}. The wall <b>behind the
 * return portal</b> is layered (bedrock at {@code originX-3}, oak-plank wood
 * wall at {@code originX-2}, gravel face at {@code originX-1}, glowstone line
 * above). Just past it is a hay-bale return portal ({@link HorsePortalManager})
 * at {@code originX+1}; the player spawns on the road a few blocks further in,
 * facing down the corridor. Down the centre is a gravel road
 * ({@code z} in [-{@value #ROAD_HALF_WIDTH}, {@value #ROAD_HALF_WIDTH}]). A
 * pen sits on each side (one per catalogue genotype): {@value #PEN_LEN_X} blocks along X,
 * {@value #PEN_DEPTH_Z} deep, brick-wall perimeter with a <b>two-wide</b>
 * oak-fence-gate opening (horses won't cross a 1-wide gap), one gravel strip
 * between consecutive pens. Outward from each pen's back edge, flush (no grass
 * gap): a gravel strip ({@code z} = +/-{@value #GRAVEL_STRIP_Z}) with a
 * glowstone line {@value #WALL_TOP_DY} blocks above it, a single oak-plank wood
 * wall ({@code z} = +/-{@value #WALL_PLANK_Z}), then the bedrock core
 * ({@code z} = +/-{@value #WALL_BEDROCK_Z}). Outside the bedrock: open void.
 * Past the last pen the corridor is closed by an end cap laid out
 * like {@link #buildStartCap}.
 */
public final class DebugPenManager {

    public static final ResourceKey<Level> DEBUG_LEVEL = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "debug_pens"));

    private static final int PEN_LEN_X = 6;              // pen extent along the corridor
    private static final int PEN_DEPTH_Z = 20;           // pen extent from the road outward
    private static final int PEN_GAP_X = 1;              // single gravel strip between consecutive pens
    private static final int PERIOD = PEN_LEN_X + PEN_GAP_X;
    private static final int PENS_PER_SEGMENT = 2;       // one each side of the road
    // Build this many pens beyond the player so the ground never "pops in" (~180 blocks).
    private static final int LOOKAHEAD_PENS = 30;

    /**
     * <b>How long the corridor is: exactly the coat-check column.</b>
     *
     * <p>Derived again, but from something that stays small. It used to come
     * from the genotype catalogue, which meant every gene made the dimension
     * bigger and the white-pattern loci alone wanted seven <i>million</i>
     * blocks; then it was a flat two thousand, arbitrary on purpose. Now it is
     * {@link CoatCheckPlan#size()} - one pen per unverified combination - which
     * is a number that <b>shrinks as the mod is tested</b>: confirm a gene by
     * eye, write its {@code Verified} block, re-bake, and its pens leave the
     * corridor. That is the right direction for a list of things to look at.
     */
    static final int COAT_CHECK_PENS = CoatCheckPlan.size();

    /**
     * Pens either side, for anything that counts them. The left column loops
     * breeds for as long as the right column runs, so the two are the same
     * length and the corridor holds twice the coat checks - less the one right
     * pen segment 0 gives up to the test yard's doorway.
     */
    static final int PEN_COUNT = COAT_CHECK_PENS * PENS_PER_SEGMENT;

    /**
     * <b>A rest stop every this many pen segments</b>, in place of that
     * segment's two pens: no fences, no horses, and a hay-bale portal home.
     *
     * <p>A pen segment is {@value #PERIOD} blocks and the corridor runs one
     * segment per coat check, so walking it end to end is thousands of blocks
     * with, until now, exactly one way out - the return portal at the start.
     * Anyone who walked in a few hundred pens to look at something had to walk
     * all the way back. A break every twenty-five segments puts a way home
     * within about ninety blocks from anywhere.
     *
     * <p>Counted in <b>segments</b>, so it is twenty-five pens down each side
     * between breaks - which is what "every twenty-five" means to somebody
     * walking past one row of them.
     */
    private static final int REST_STOP_EVERY = 25;

    /**
     * Pen segments needed to hold the coat-check column, before any breaks.
     *
     * <p><b>One per segment, not two.</b> The right-hand pen is the coat check
     * and the left-hand one is a breed, so a segment carries exactly one entry
     * from {@link CoatCheckPlan} - and one more segment than there are entries,
     * because segment 0 gives its right-hand pen to the test yard's doorway.
     */
    private static final int PEN_SEGMENTS = COAT_CHECK_PENS + 1;

    /**
     * The corridor holds {@link #PEN_COUNT} pens, two per segment, <b>plus</b>
     * the rest-stop segments that carry none - so it is longer than the pen
     * count alone would make it, or the breaks would eat the last forty pens.
     * One break per {@code REST_STOP_EVERY - 1} pen segments, and one spare so
     * rounding can never cut the corridor short. Fixed, so a plot rebuilt on a
     * recycled X has the same geometry as the one it replaces - see the
     * teardown note in the class javadoc.
     */
    private static final int LAST_SEGMENT_INDEX =
            PEN_SEGMENTS + PEN_SEGMENTS / (REST_STOP_EVERY - 1);

    private static final int ROAD_HALF_WIDTH = 3;        // gravel road: z in [-3, 3]
    private static final int WALL_TOP_DY = 10;           // glowstone line height above the floor

    // Outward from a pen's back edge (PEN_FAR_Z): a gravel strip flush against
    // the pen (glowstone line directly above it), a single oak-plank wood wall,
    // then the bedrock core. No grass gap.
    private static final int PEN_FAR_Z = ROAD_HALF_WIDTH + PEN_DEPTH_Z;   // 23 - pen back edge (brick wall)
    private static final int GRAVEL_STRIP_Z = PEN_FAR_Z + 1;             // 24 - gravel strip, glowstone line above it
    private static final int WALL_PLANK_Z = PEN_FAR_Z + 2;              // 25 - single oak-plank wood wall
    private static final int WALL_BEDROCK_Z = PEN_FAR_Z + 3;           // 26 - bedrock core (last solid block)

    // Plots are spaced far enough apart on X that they never share chunks - the
    // full catalogue corridor plus a margin.
    private static final int PLOT_SPACING_X = (LAST_SEGMENT_INDEX + 2) * PERIOD + 1_000;
    // Fixed, not random: leaving no longer clears the blocks, so a rebuilt plot
    // has to land exactly on top of the old one and overwrite it. Same X, same
    // Y, same deterministic catalogue = same geometry, so it always does.
    private static final int PLOT_BASE_Y = 128;          // dimension is 512 tall (see dimension_type)

    /**
     * The last {@code |z|} the corridor itself occupies. Anything further out
     * is the test yard or the void - which is how
     * {@code HorseGeneticsEventHandler} tells "in the gallery" from "in the
     * yard" without having to look up whose plot it is.
     */
    static int corridorWallZ() {
        return WALL_BEDROCK_Z;
    }

    /** Geometry for one side of the road. */
    private record PenSpec(int zRoad, int zBack, Direction roadFacing) {}

    private static final PenSpec NORTH_PEN = new PenSpec(ROAD_HALF_WIDTH + 1, PEN_FAR_Z, Direction.NORTH);
    private static final PenSpec SOUTH_PEN = new PenSpec(-(ROAD_HALF_WIDTH + 1), -PEN_FAR_Z, Direction.SOUTH);

    /** One private instance of the corridor. Mutable {@code highestIndex} tracks how far it's been built. */
    static final class Plot {
        final int originX;
        final int baseY;                       // grass-surface Y - always PLOT_BASE_Y
        final ResourceKey<Level> returnDim;    // where its return portal sends you
        final BlockPos returnPos;              // exact spot to land on the way back
        int highestIndex = -1;
        BlockPos exitPortal;

        Plot(int originX, int baseY, ResourceKey<Level> returnDim, BlockPos returnPos) {
            this.originX = originX;
            this.baseY = baseY;
            this.returnDim = returnDim;
            this.returnPos = returnPos;
        }
    }

    // Live plots, keyed by the single player they belong to. Strictly 1:1 -
    // every enter() makes a new plot, so no two players ever share one.
    private static final Map<UUID, Plot> PLOTS = new HashMap<>();
    private static final Deque<Integer> FREE_ORIGINS = new ArrayDeque<>();
    private static int nextOriginX = 0;

    /** F6 entry point: remember where the player was, then drop them into a fresh plot. */
    public static void teleportAndGenerate(ServerPlayer player) {
        enter(player, player.level().dimension(), player.blockPosition());
    }

    /**
     * Move {@code player} into a brand-new plot. Any plot they already held is
     * torn down first (a visit always regenerates). {@code returnDim} /
     * {@code returnPos} is where the plot's return portal will send things.
     */
    public static void enter(ServerPlayer player, ResourceKey<Level> returnDim, BlockPos returnPos) {
        ServerLevel debug = ((ServerLevel) player.level()).getServer().getLevel(DEBUG_LEVEL);
        if (debug == null) {
            HorseGenetics.LOGGER.error("Debug pens dimension not found - is data/horsegenetics/dimension/debug_pens.json present?");
            return;
        }

        Plot old = PLOTS.remove(player.getUUID());
        if (old != null) {
            tearDown(debug, old);
        }

        int originX = allocateOriginX();
        Plot plot = new Plot(originX, PLOT_BASE_Y, returnDim, returnPos.immutable());
        PLOTS.put(player.getUUID(), plot);

        ensureBuiltUpToIndex(debug, plot, LOOKAHEAD_PENS);
        // After the corridor, because the yard's path is cut THROUGH the wall
        // the corridor lays down. Once per plot: the geometry is fixed, so a
        // plot rebuilt on a recycled X gets the same yard in the same place.
        DebugTestYard.build(debug, plot);

        // Spawn on the road just past the return portal, facing +X down the corridor.
        player.teleportTo(debug, originX + 3.5, PLOT_BASE_Y + 1, 0.5, Set.of(), -90.0f, 0.0f, false);
        giveDebugPaper(player);
    }

    /**
     * Drop the player's plot (if any) and clear it out. Safe to call for players
     * who never entered.
     */
    public static void leave(MinecraftServer server, UUID playerId) {
        Plot plot = PLOTS.remove(playerId);
        if (plot == null) {
            return;
        }
        ServerLevel debug = server.getLevel(DEBUG_LEVEL);
        if (debug != null) {
            tearDown(debug, plot);
        }
    }

    /** Called each player tick while they're in the debug dimension: build ahead of them. */
    public static void ensureGeneratedAheadOfPlayer(ServerPlayer player) {
        Plot plot = PLOTS.get(player.getUUID());
        if (plot == null || !(player.level() instanceof ServerLevel debug)) {
            return;
        }
        int localX = player.getBlockX() - plot.originX;
        int neededIndex = Math.floorDiv(Math.max(localX, 0), PERIOD) + LOOKAHEAD_PENS;
        ensureBuiltUpToIndex(debug, plot, neededIndex);
    }

    /** The plot whose corridor spans this world X, or {@code null}. */
    static Plot plotContaining(int blockX) {
        for (Plot p : PLOTS.values()) {
            int end = p.originX + (LAST_SEGMENT_INDEX + 2) * PERIOD;
            if (blockX >= p.originX - 4 && blockX < end) {
                return p;
            }
        }
        return null;
    }

    // --- allocation ---

    private static int allocateOriginX() {
        Integer recycled = FREE_ORIGINS.poll();
        if (recycled != null) {
            return recycled;
        }
        int x = nextOriginX;
        nextOriginX += PLOT_SPACING_X;
        return x;
    }

    // --- generation ---

    /** Builds up to {@code targetIndex}, but never past the end of the corridor. */
    private static void ensureBuiltUpToIndex(ServerLevel level, Plot plot, int targetIndex) {
        int capped = Math.min(targetIndex, LAST_SEGMENT_INDEX);
        while (plot.highestIndex < capped) {
            int idx = plot.highestIndex + 1;
            buildSegment(level, plot, idx);
            plot.highestIndex = idx;
            if (idx == LAST_SEGMENT_INDEX) {
                buildEndCap(level, plot);
            }
        }
    }

    /**
     * One segment = one pen on each side of the road. The right-hand pen (the
     * {@code +Z} side, on your right walking in from the portal) is pen
     * {@code 2 * index}, the left-hand one {@code 2 * index + 1} - the number
     * on its sign, and nothing more: each pen rolls its own genotype.
     */
    private static void buildSegment(ServerLevel level, Plot plot, int index) {
        int x0 = plot.originX + index * PERIOD;
        if (index == 0) {
            buildStartCap(level, plot);
        }
        buildCorridor(level, plot, x0);
        if (isRestStop(index)) {
            // The corridor floor and its outer walls are laid by buildCorridor,
            // not by buildPen, so a segment with no pens in it is still solid
            // ground between solid walls - just an open stretch with a way home.
            buildRestStop(level, plot, x0, index);
        } else {
            // Each side of the road is its own column with its own index, and
            // both count segments rather than pens: the nth segment you walk
            // past holds the nth coat check and the nth breed.
            int ordinal = penBaseFor(index) / PENS_PER_SEGMENT;
            // Segment 0's +Z pen is the test yard's doorway - DebugTestYard
            // writes a path straight through where its fence and back wall
            // would stand, so building it first and cutting it open afterwards
            // would leave brick stumps either side of the gap. So the coat-check
            // column starts one segment in, and its index is one behind the
            // segment's: the yard takes a pen's place, it does not skip an entry.
            if (index != 0) {
                buildCoatCheckPen(level, plot, x0, NORTH_PEN, ordinal - 1);
            }
            buildBreedPen(level, plot, x0, SOUTH_PEN, ordinal);
        }
        if (index == 0) {
            buildReturnPortal(level, plot);
            buildEntranceSign(level, plot);
        }
    }

    /** Is this segment a break rather than a pair of pens? Never the first one. */
    private static boolean isRestStop(int index) {
        return index > 0 && index % REST_STOP_EVERY == 0;
    }

    /**
     * The number of the first pen in segment {@code index}.
     *
     * <p><b>Not {@code index * 2}</b>, which is what it was before the breaks
     * existed: a rest stop builds no pens, so counting by segment would leave a
     * hole in the numbering and the signs would read ...49, 50, then 53. The
     * breaks before this segment are subtracted, so the numbers stay contiguous
     * down the corridor and a pen's sign still means "the nth pen you have
     * walked past". A pure function of the index, so the geometry is still
     * deterministic and a recycled plot rebuilds identically.
     */
    private static int penBaseFor(int index) {
        return (index - index / REST_STOP_EVERY) * PENS_PER_SEGMENT;
    }

    /**
     * A break in the rows: no fences, no horses, and a hay-bale portal set back
     * off the north side of the road.
     *
     * <p>It is <b>off</b> the road rather than across it on purpose. The return
     * portal at the start stands in the road because you arrive through it and
     * everything past it is the corridor; one of these standing in the road
     * would teleport out anybody walking the length of the place, which is the
     * opposite of a convenience. You step aside into this one.
     *
     * <p>It needs no destination wiring: {@code HorsePortalManager} resolves a
     * debug-dimension portal through {@link #plotContaining}, which is an X
     * range, so a portal anywhere in the plot already sends you to that plot's
     * own return position.
     */
    private static void buildRestStop(ServerLevel level, Plot plot, int x0, int index) {
        int gy = plot.baseY;
        int pz = ROAD_HALF_WIDTH + 3;        // set back from the road edge, not on it
        BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();
        BlockState portal = ModBlocks.HAY_PORTAL.get().defaultBlockState()
                .setValue(HayPortalBlock.AXIS, Direction.Axis.X);

        // Frame spans x0+1..x0+4 with a 2-wide, 3-tall interior - the same
        // shape as the return portal, turned to face the road.
        for (int x = x0 + 1; x <= x0 + 4; x++) {
            fastSet(level, new BlockPos(x, gy, pz), hay);
            fastSet(level, new BlockPos(x, gy + 4, pz), hay);
        }
        for (int y = gy + 1; y <= gy + 3; y++) {
            fastSet(level, new BlockPos(x0 + 1, y, pz), hay);
            fastSet(level, new BlockPos(x0 + 4, y, pz), hay);
            fastSetPortal(level, new BlockPos(x0 + 2, y, pz), portal);
            fastSetPortal(level, new BlockPos(x0 + 3, y, pz), portal);
        }

        int pensSoFar = penBaseFor(index);
        placeSign(level, new BlockPos(x0 + 5, gy + 1, ROAD_HALF_WIDTH + 1), Direction.SOUTH,
                List.of("Way out",
                        "step through",
                        String.format("%,d pens", pensSoFar),
                        "that way ->"));
    }

    private static void giveDebugPaper(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            if (inv.getItem(slot).isEmpty()) {
                inv.setItem(slot, new ItemStack(Items.PAPER));
                return;
            }
        }
    }

    // --- floor + walls ---

    static void groundColumn(ServerLevel level, int x, int gy, int z, BlockState surface) {
        fastSet(level, new BlockPos(x, gy - 3, z), Blocks.BEDROCK.defaultBlockState());
        fastSet(level, new BlockPos(x, gy - 2, z), Blocks.DIRT.defaultBlockState());
        fastSet(level, new BlockPos(x, gy - 1, z), Blocks.DIRT.defaultBlockState());
        fastSet(level, new BlockPos(x, gy, z), surface);
    }

    /**
     * The wall <b>behind the return portal</b>, at the very start of the plot.
     * Pushed two blocks back (was raw bedrock at {@code originX-1}) and layered
     * the same way as the E/W walls, reading from the portal outward: a gravel
     * <b>floor strip</b> at {@code originX-1} (with a glowstone line floating one
     * block above it), a single oak-plank wood wall {@code originX-2}, then the
     * bedrock core {@code originX-3}. Every column stands on a bedrock/dirt base
     * so nothing floats or falls (the earlier full-height gravel column fell
     * into the void and left a gap).
     */
    private static void buildStartCap(ServerLevel level, Plot plot) {
        int gy = plot.baseY;
        int yHi = gy + WALL_TOP_DY - 1;
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();
        for (int z = -WALL_BEDROCK_Z; z <= WALL_BEDROCK_Z; z++) {
            // bedrock core, full height
            for (int y = gy - 3; y <= yHi; y++) {
                fastSet(level, new BlockPos(plot.originX - 3, y, z), bedrock);
            }
            // wood wall on a solid base
            fastSet(level, new BlockPos(plot.originX - 2, gy - 3, z), bedrock);
            fastSet(level, new BlockPos(plot.originX - 2, gy - 2, z), dirt);
            fastSet(level, new BlockPos(plot.originX - 2, gy - 1, z), dirt);
            for (int y = gy; y <= yHi; y++) {
                fastSet(level, new BlockPos(plot.originX - 2, y, z), planks);
            }
            // gravel floor strip in front of the wood wall, plus a glowstone
            // line one block above it (proud of the wall, like the E/W strips)
            fastSet(level, new BlockPos(plot.originX - 1, gy - 3, z), bedrock);
            fastSet(level, new BlockPos(plot.originX - 1, gy - 2, z), dirt);
            fastSet(level, new BlockPos(plot.originX - 1, gy - 1, z), dirt);
            fastSet(level, new BlockPos(plot.originX - 1, gy, z), gravel);
            fastSet(level, new BlockPos(plot.originX - 1, gy + WALL_TOP_DY, z), glowstone);
        }

        // Close the corner: carry the E/W wall's oak-plank face and bedrock core
        // forward one more block (to originX-1) so they butt straight into the
        // back wall - otherwise there's an open slot at the seam and you see the
        // void through it.
        for (int side : new int[] {1, -1}) {
            for (int y = gy; y <= yHi; y++) {
                fastSet(level, new BlockPos(plot.originX - 1, y, side * WALL_PLANK_Z), planks);
            }
            for (int y = gy - 3; y <= yHi; y++) {
                fastSet(level, new BlockPos(plot.originX - 1, y, side * WALL_BEDROCK_Z), bedrock);
            }
        }
    }

    private static void buildCorridor(ServerLevel level, Plot plot, int x0) {
        int gy = plot.baseY;
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();

        for (int x = x0; x < x0 + PERIOD; x++) {
            // solid floor only inside the walls; everything past +/-WALL_BEDROCK_Z stays void
            for (int z = -WALL_BEDROCK_Z; z <= WALL_BEDROCK_Z; z++) {
                groundColumn(level, x, gy, z, grass);
            }
            for (int z = -ROAD_HALF_WIDTH; z <= ROAD_HALF_WIDTH; z++) {
                fastSet(level, new BlockPos(x, gy, z), gravel);
            }
            if (x == x0 + PEN_LEN_X) { // gap column between this pen and the next: gravel full width
                for (int z = -PEN_FAR_Z; z <= PEN_FAR_Z; z++) {
                    fastSet(level, new BlockPos(x, gy, z), gravel);
                }
            }
            for (int side : new int[] {1, -1}) {
                fastSet(level, new BlockPos(x, gy, side * GRAVEL_STRIP_Z), gravel);
                for (int y = gy - 3; y <= gy + WALL_TOP_DY - 1; y++) {
                    fastSet(level, new BlockPos(x, y, side * WALL_BEDROCK_Z), bedrock);
                }
                for (int y = gy; y <= gy + WALL_TOP_DY - 1; y++) {
                    fastSet(level, new BlockPos(x, y, side * WALL_PLANK_Z), planks);
                }
                // glowstone line directly above the gravel strip
                fastSet(level, new BlockPos(x, gy + WALL_TOP_DY, side * GRAVEL_STRIP_Z), glowstone);
            }

            // AND THE FLOOR, which the glowstone line never reached. The lines
            // run at gy+10 along the walls; a horse standing in the middle of a
            // pen is nine blocks below one and eleven across from it, which is
            // twenty by the arithmetic light actually uses - so the back of
            // every pen in this gallery reads ZERO. That cost nothing for as
            // long as the dimension spawned no monsters and its horses could
            // not be hurt. Both of those stopped being true on 2026-09-13: the
            // biome spawns zombies now, and the rule that made horses
            // invulnerable here was removed on the owner's word. A hundred and
            // forty showcase horses standing in the dark is a hundred and forty
            // horses that can be eaten overnight, and unlike the test yard's
            // stock they are not replaceable by rebuilding one pen - the whole
            // point of the gallery is that each draw is a different animal.
            //
            // Invisible full-brightness blocks rather than more glowstone: the
            // corridor is a place you look AT horses, and a lamp every four
            // blocks at eye level would be in front of half of them.
            BlockState lamp = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
            if ((x & 3) == 0) {
                for (int z = -PEN_FAR_Z + 2; z <= PEN_FAR_Z; z += 4) {
                    fastSet(level, new BlockPos(x, gy + 3, z), lamp);
                }
            }
        }
    }

    // --- one pen ---

    /**
     * <b>An empty pen, ready to stock</b> - walls, a two-wide gate, corner
     * torches, sunk water and hay, and nothing alive in it.
     *
     * <p>Shared by both columns, because a coat check and a breed pen differ
     * only in what stands inside them and what the signs say. The two used to be
     * one method that also rolled a genotype, and splitting the shell out is
     * what let the columns stop being the same thing twice.
     */
    private static void penShell(ServerLevel level, Plot plot, int x0, PenSpec pen) {
        int gy = plot.baseY;
        int floorY = gy + 1;
        int xMax = x0 + PEN_LEN_X - 1;
        // Two-wide gate opening in the middle of the road-side edge - a single
        // 1-wide gate lets horses slip out, so use two side by side.
        int gateX = x0 + PEN_LEN_X / 2 - 1;
        int zLo = Math.min(pen.zRoad(), pen.zBack());
        int zHi = Math.max(pen.zRoad(), pen.zBack());
        penWalls(level, floorY, x0, xMax, zLo, zHi, gateX, pen.zRoad(), pen.roadFacing());

        // Amenities in the two gate-side interior corners: a full water cauldron
        // in one, a hay bale in the other (one block in from the road-side wall).
        // Sunk a block into the ground (floorY - 1) so their tops sit flush with
        // the grass - a full block at floorY was a step the horses used to hop
        // the 1-high pen wall.
        int zGateInner = pen.zRoad() + Integer.signum(pen.zBack() - pen.zRoad());
        level.setBlockAndUpdate(new BlockPos(x0 + 1, floorY - 1, zGateInner),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        level.setBlockAndUpdate(new BlockPos(xMax - 1, floorY - 1, zGateInner),
                Blocks.HAY_BLOCK.defaultBlockState());

        // A pen is built exactly once per plot, so anything already standing in
        // it belongs to a previous occupant of this recycled X slot - and its
        // genotype has nothing to do with the sign that was just written. Clear
        // it out and stock fresh. Tamed horses are left alone: a player can
        // have ridden one this far ahead of the build frontier.
        AABB interior = new AABB(x0, floorY, zLo, xMax + 1, floorY + 4, zHi + 1);
        for (Horse stale : level.getEntitiesOfClass(Horse.class, interior, h -> !h.isTamed())) {
            forget(level, stale);
            stale.discard();
        }
    }

    /**
     * <b>One coat check: a combination nobody has confirmed by eye, on four base
     * coats at once.</b>
     *
     * <p>The four stand in a line across the pen, in {@link CoatCheckPlan.Base}
     * order - black, bay, chestnut, white - so they read left to right from the
     * road like a row of swatches. Spaced along Z rather than X because the pen
     * is twenty deep and six wide: four horses across six blocks would overlap,
     * and {@link #placeClear} would then scatter them.
     *
     * <p>The white one is the <b>masking check</b> and is the reason it is worth
     * a quarter of every pen: {@code KIT}'s dominant white removes every
     * pigment, so anything still drawn on that horse is a gene painting over a
     * mask it should have respected.
     */
    private static void buildCoatCheckPen(ServerLevel level, Plot plot, int x0, PenSpec pen, int planIndex) {
        CoatCheckPlan.Pen check = CoatCheckPlan.at(planIndex);
        if (check == null) {
            return;     // past the end of the plan - the corridor outruns it by a segment or two
        }
        penShell(level, plot, x0, pen);
        buildCoatCheckSigns(level, plot, x0, pen, planIndex, check);

        int floorY = plot.baseY + 1;
        int zLo = Math.min(pen.zRoad(), pen.zBack());
        int zHi = Math.max(pen.zRoad(), pen.zBack());
        double midX = x0 + PEN_LEN_X / 2.0;
        // Across the pen's depth, well inside both walls.
        double near = Math.min(zLo, zHi) + 4.5;
        double step = (Math.abs(zHi - zLo) - 9.0) / 3.0;
        CoatCheckPlan.Base[] bases = CoatCheckPlan.Base.values();
        for (int i = 0; i < bases.length; i++) {
            CoatCheckPlan.Base base = bases[i];
            Horse horse = spawnHorse(level, floorY, midX, near + step * i,
                    check.sex(), check.genotype(base).toCode());
            // The sign names the combination once; the horse names which base it
            // is, because four unlabelled horses is a puzzle rather than a test.
            if (horse != null) {
                horse.setCustomName(Component.literal(base.label()));
                horse.setCustomNameVisible(true);
            }
        }
    }

    /**
     * <b>One breed, as a mare and a stallion.</b> The left-hand column is every
     * breed in the game in alphabetical order, looping for as long as the
     * coat-check column runs - so a walk down the corridor is a walk past the
     * whole breed book, however far the right-hand side happens to go.
     */
    private static void buildBreedPen(ServerLevel level, Plot plot, int x0, PenSpec pen, int breedIndex) {
        List<Breed> all = breedColumn();
        if (all.isEmpty()) {
            return;
        }
        Breed breed = all.get(Math.floorMod(breedIndex, all.size()));
        penShell(level, plot, x0, pen);
        buildBreedSigns(level, plot, x0, pen, breedIndex, breed, all.size());

        int floorY = plot.baseY + 1;
        int zLo = Math.min(pen.zRoad(), pen.zBack());
        int zHi = Math.max(pen.zRoad(), pen.zBack());
        double midX = x0 + PEN_LEN_X / 2.0;
        double midZ = (zLo + zHi) / 2.0;
        spawnBreedHorse(level, floorY, midX, midZ - 4, Sex.MALE, breed);
        spawnBreedHorse(level, floorY, midX, midZ + 4, Sex.FEMALE, breed);
    }

    /**
     * Every breed that can stand in a pen, alphabetically.
     *
     * <p>{@link Breeds#all()} is in registration order and includes
     * {@link Breeds#FERAL_MIXED}, which is not a breed but the absence of one -
     * a pen labelled "Feral Mixed" would be an unconstrained founder draw with a
     * breed's name over it, which is exactly what the right-hand column already
     * does better. Sorted by display name so the column is walkable as an index.
     */
    private static List<Breed> breedColumn() {
        List<Breed> out = new ArrayList<>();
        for (Breed breed : Breeds.all()) {
            if (breed != Breeds.FERAL_MIXED) {
                out.add(breed);
            }
        }
        out.sort(java.util.Comparator.comparing(Breed::name));
        return out;
    }

    /** Drop {@code horse} from the ancestry database, if it has a record there. */
    private static void forget(ServerLevel level, Horse horse) {
        if (level.getServer() != null && HorseRecords.hasRealRecord(horse)) {
            HorseAncestryData.get(level.getServer()).forget(HorseRecords.of(horse).id());
        }
    }

    /**
     * <b>One pen's perimeter: brick wall, a two-wide gate, torches on the four
     * corner posts.</b> The corridor's pens are built from this and so is
     * everything in the test yard, because the yard's first version rolled its
     * own and got three separate things wrong that this had already solved -
     * fences written without neighbour updates (so they never connected),
     * torches placed into the wall line instead of on top of it, and an opening
     * made of <i>air</i> rather than gates, which is not an opening but a
     * missing wall. Two implementations of "a pen" is one too many.
     *
     * <p>{@code gateZ} is the wall the opening goes in and {@code gateFacing}
     * is the way you walk through it. A single-wide gate lets horses slip out,
     * hence two side by side.
     */
    static void penWalls(ServerLevel level, int floorY, int x0, int xMax, int zLo, int zHi,
                         int gateX, int gateZ, Direction gateFacing) {
        BlockState wall = Blocks.BRICK_WALL.defaultBlockState();
        BlockState gate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(FenceGateBlock.FACING, gateFacing);
        int otherZ = gateZ == zLo ? zHi : zLo;

        for (int x = x0; x <= xMax; x++) {
            boolean isGate = x == gateX || x == gateX + 1;
            level.setBlockAndUpdate(new BlockPos(x, floorY, gateZ), isGate ? gate : wall);
            level.setBlockAndUpdate(new BlockPos(x, floorY, otherZ), wall);
        }
        for (int z = zLo + 1; z < zHi; z++) {
            level.setBlockAndUpdate(new BlockPos(x0, floorY, z), wall);
            level.setBlockAndUpdate(new BlockPos(xMax, floorY, z), wall);
        }

        // torches on the four corner wall posts only (corners get up=true, so a solid top)
        torchOnFence(level, x0, floorY, zLo);
        torchOnFence(level, xMax, floorY, zLo);
        torchOnFence(level, x0, floorY, zHi);
        torchOnFence(level, xMax, floorY, zHi);
    }

    static void torchOnFence(ServerLevel level, int x, int floorY, int z) {
        level.setBlock(new BlockPos(x, floorY + 1, z), Blocks.TORCH.defaultBlockState(), 2);
    }

    // --- signs -----------------------------------------------------------

    private static final int SIGN_LINES = 4;              // vanilla sign: 4 lines per face
    private static final int SIGN_LINE_CHARS = 15;        // about what a vanilla sign line fits

    /**
     * <b>The four sign slots on one pen's road frontage</b>, in reading order
     * left to right as you face the pen. The gate takes the middle two blocks of
     * a six-wide pen, so what is left is the pair either side of it.
     *
     * <p>One sign was enough when a pen was a genotype code. It is not enough
     * for "what should this look like?", which is a sentence - so a pen gets as
     * many of these as it has something to say, and they are filled in order
     * rather than spread out, so a pen with three signs has a gap at the end
     * rather than a hole in the middle.
     */
    private static int[] signSlots(int x0) {
        return new int[] {x0, x0 + 1, x0 + PEN_LEN_X - 2, x0 + PEN_LEN_X - 1};
    }

    /** Write {@code boards} into the pen's sign slots, one board per sign, in order. */
    private static void placeFrontage(ServerLevel level, Plot plot, int x0, PenSpec pen,
                                      List<List<String>> boards) {
        int[] slots = signSlots(x0);
        int signZ = pen.zRoad() + pen.roadFacing().getStepZ();  // one block out onto the road
        for (int i = 0; i < boards.size() && i < slots.length; i++) {
            placeSign(level, new BlockPos(slots[i], plot.baseY + 1, signZ), pen.roadFacing(),
                    boards.get(i));
        }
    }

    /**
     * <b>What this combination is, and what it should look like.</b>
     *
     * <p>The first board identifies it - the pen's number, the gene, and the two
     * allele tokens - and the rest carry the outcome's own description, wrapped.
     * That description is the gene file's, the same sentence the wiki page and
     * the designer show, so a tester comparing the horse to the sign is
     * comparing it to what the gene <i>claims</i> rather than to a guess.
     */
    private static void buildCoatCheckSigns(ServerLevel level, Plot plot, int x0, PenSpec pen,
                                            int planIndex, CoatCheckPlan.Pen check) {
        List<List<String>> boards = new ArrayList<>();
        List<String> head = new ArrayList<>();
        head.add("#" + (planIndex + 1));
        head.addAll(wrapText(geneLabel(check.gene()), SIGN_LINE_CHARS, 2));
        head.add(check.pair().toTokens());
        boards.add(head);

        List<String> what = new ArrayList<>();
        what.add("SHOULD BE:");
        what.addAll(wrapText(check.expression().name(), SIGN_LINE_CHARS, SIGN_LINES - 1));
        boards.add(what);

        // The description is a sentence or three; it gets whatever boards are left.
        List<String> words = wrapText(check.expression().description(),
                SIGN_LINE_CHARS, SIGN_LINES * 2);
        for (int from = 0; from < words.size() && boards.size() < signSlots(x0).length; from += SIGN_LINES) {
            boards.add(words.subList(from, Math.min(from + SIGN_LINES, words.size())));
        }
        placeFrontage(level, plot, x0, pen, boards);
    }

    /** A gene's display name, falling back to its key's last segment. */
    private static String geneLabel(com.example.horsegenetics.common.genetics.Gene gene) {
        String name = gene.name();
        return name == null || name.isEmpty() ? gene.key() : name;
    }

    /**
     * <b>Which breed this is, and where in the book.</b> Breed names run long
     * ("Mecklenburger Warmblood"), so the name gets a whole board to wrap into
     * and the country and position share the next.
     */
    private static void buildBreedSigns(ServerLevel level, Plot plot, int x0, PenSpec pen,
                                        int breedIndex, Breed breed, int total) {
        List<List<String>> boards = new ArrayList<>();
        boards.add(wrapText(breed.name(), SIGN_LINE_CHARS, SIGN_LINES));

        List<String> where = new ArrayList<>();
        where.add(String.format("%d of %,d", Math.floorMod(breedIndex, total) + 1, total));
        if (breed.country() != null && !breed.country().isEmpty()) {
            where.addAll(wrapText(breed.country(), SIGN_LINE_CHARS, 2));
        }
        where.add("mare + stud");
        boards.add(where);
        placeFrontage(level, plot, x0, pen, boards);
    }

    /**
     * {@code text} broken onto at most {@code maxLines} lines of at most
     * {@code maxChars}, between whole words.
     *
     * <p>{@link GeneCodeDisplay#wrap} does this for a genotype and only for a
     * genotype - it breaks between gene tokens, which is the right rule there
     * and the wrong one for a sentence. A word longer than a line is left to
     * overflow rather than hyphenated: an allele name running one character wide
     * is better than one that cannot be read back.
     */
    private static List<String> wrapText(String text, int maxChars, int maxLines) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > maxChars) {
                out.add(line.toString());
                line.setLength(0);
                if (out.size() == maxLines) {
                    return out;
                }
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0 && out.size() < maxLines) {
            out.add(line.toString());
        }
        return out;
    }

    /**
     * <b>The two signs three blocks in front of the entrance portal</b>, which
     * have to do the one job no pen sign can: say that the corridor has two
     * sides and that they are different from each other. Somebody who walks in
     * and reads only pen labels will take the place for one long list.
     *
     * <p>It was a tally of the genotype catalogue once, and then a count of
     * random pens. Now it is the shape of the place.
     */
    private static void buildEntranceSign(ServerLevel level, Plot plot) {
        placeSign(level, new BlockPos(plot.originX + 4, plot.baseY + 1, 0), Direction.WEST,
                List.of("Horse Pens",
                        "LEFT: breeds",
                        "RIGHT: coats",
                        "not yet checked"));
        // A second sign beside it, because the right-hand column is the one that
        // needs explaining and four lines cannot do both.
        placeSign(level, new BlockPos(plot.originX + 4, plot.baseY + 1, -1), Direction.WEST,
                List.of(String.format("%,d coats", COAT_CHECK_PENS),
                        "4 horses each:",
                        "black bay",
                        "chestnut white"));
    }

    /**
     * A waxed standing oak sign at {@code pos}, its text facing {@code facing},
     * with {@code lines} written identically on both faces (lines past
     * {@value #SIGN_LINES} are dropped). Waxed so a visitor can't scribble over
     * the label.
     */
    static void placeSign(ServerLevel level, BlockPos pos, Direction facing, List<String> lines) {
        BlockState sign = Blocks.OAK_SIGN.defaultBlockState()
                .setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(facing));
        level.setBlock(pos, sign, 3);
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity be)) {
            return;
        }
        SignText front = be.getFrontText();
        SignText back = be.getBackText();
        for (int line = 0; line < SIGN_LINES; line++) {
            Component text = Component.literal(line < lines.size() ? lines.get(line) : "");
            front = front.setMessage(line, text);
            back = back.setMessage(line, text);
        }
        be.setText(front, true);
        be.setText(back, false);
        be.setWaxed(true);
        be.setChanged();
        level.sendBlockUpdated(pos, sign, sign, 3);
    }

    /**
     * The far end of the corridor, past the last pen - the mirror of
     * {@link #buildStartCap}, so the gallery finishes in a wall instead of
     * trailing off into the void.
     */
    private static void buildEndCap(ServerLevel level, Plot plot) {
        int xEnd = plot.originX + (LAST_SEGMENT_INDEX + 1) * PERIOD;  // first x past the last segment
        int gy = plot.baseY;
        int yHi = gy + WALL_TOP_DY - 1;
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();
        for (int z = -WALL_BEDROCK_Z; z <= WALL_BEDROCK_Z; z++) {
            // gravel floor strip facing the corridor, glowstone line above it
            fastSet(level, new BlockPos(xEnd, gy - 3, z), bedrock);
            fastSet(level, new BlockPos(xEnd, gy - 2, z), dirt);
            fastSet(level, new BlockPos(xEnd, gy - 1, z), dirt);
            fastSet(level, new BlockPos(xEnd, gy, z), gravel);
            fastSet(level, new BlockPos(xEnd, gy + WALL_TOP_DY, z), glowstone);
            // wood wall on a solid base
            fastSet(level, new BlockPos(xEnd + 1, gy - 3, z), bedrock);
            fastSet(level, new BlockPos(xEnd + 1, gy - 2, z), dirt);
            fastSet(level, new BlockPos(xEnd + 1, gy - 1, z), dirt);
            for (int y = gy; y <= yHi; y++) {
                fastSet(level, new BlockPos(xEnd + 1, y, z), planks);
            }
            // bedrock core, full height
            for (int y = gy - 3; y <= yHi; y++) {
                fastSet(level, new BlockPos(xEnd + 2, y, z), bedrock);
            }
        }
        // carry the E/W wall faces into the corner so there's no gap at the seam
        for (int side : new int[] {1, -1}) {
            for (int y = gy; y <= yHi; y++) {
                fastSet(level, new BlockPos(xEnd, y, side * WALL_PLANK_Z), planks);
            }
            for (int y = gy - 3; y <= yHi; y++) {
                fastSet(level, new BlockPos(xEnd, y, side * WALL_BEDROCK_Z), bedrock);
            }
        }
    }

    static Horse spawnHorse(ServerLevel level, int floorY, double x, double z, Sex sex,
                                   String geneticCode) {
        return spawnHorse(level, floorY, x, z, sex, geneticCode, false);
    }

    /**
     * <b>One horse of a named breed</b>, for the corridor's left-hand column.
     *
     * <p>Not {@link #spawnHorse} with a genotype string: a breed is not only a
     * set of alleles, it is a <i>record</i> - the founder is rolled from the
     * breed's own constrained pools and stamped with its lineage token, so the
     * horse reads as that breed everywhere downstream (its info panel, its
     * price, what its foals are called). Building the genotype by hand and
     * spawning it would produce a horse that looks right and is feral on paper.
     */
    static Horse spawnBreedHorse(ServerLevel level, int floorY, double x, double z, Sex sex,
                                 Breed breed) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.COMMAND);
        if (horse == null) {
            return null;
        }
        placeClear(level, horse, x, floorY, z);
        // Record applied before the entity joins, so HorseGeneticsEventHandler
        // sees a real record and doesn't roll a random genotype over the top.
        HorseRecords.apply(horse,
                HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), breed, sex));
        level.addFreshEntity(horse);
        return horse;
    }

    /**
     * {@code tamed} matters more than it looks: vanilla refuses to breed an
     * untamed horse ({@code AbstractHorse.canParent}), so any pen whose test
     * involves a foal has to be stocked tame or nothing can ever happen in it.
     */
    static Horse spawnHorse(ServerLevel level, int floorY, double x, double z, Sex sex,
                                   String geneticCode, boolean tamed) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.COMMAND);
        if (horse == null) {
            return null;
        }
        if (tamed) {
            horse.setTamed(true);
        }
        placeClear(level, horse, x, floorY, z);
        // Record applied before the entity joins, so HorseGeneticsEventHandler
        // sees a real record and doesn't roll a random genotype over the top.
        HorseRecords.apply(horse,
                HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), sex, Genotype.parse(geneticCode)));
        level.addFreshEntity(horse);
        return horse;
    }

    /**
     * <b>Put the horse somewhere it is not inside a block.</b>
     *
     * <p>Every pen in this dimension hands {@code spawnHorse} a hand-computed
     * pair of coordinates, and a hand-computed coordinate is wrong sooner or
     * later - a pen moves, a wall gains a thickness, a sign lands where a horse
     * was going to stand. For most of this dimension's life that cost nothing
     * visible, because the debug dimension cancelled all horse damage: a horse
     * half inside a wall simply stood there looking slightly wrong.
     *
     * <p><b>That rule was removed on 2026-09-13 and the very first build after
     * it killed a horse.</b> Half a second into the yard's construction:
     * {@code horse hurt | Viking Quark took 1.0 from inWall}, over and over,
     * until it died. Suffocation is two hearts a second and a horse is placed
     * once and never moved, so a spot that is wrong is <em>fatal</em> rather
     * than untidy - and it fails in the worst possible way for this project,
     * which is that the pen still reads as stocked in the log and is empty by
     * the time anybody walks to it.
     *
     * <p>So the coordinate is a <b>suggestion</b> now. {@code noCollision} asks
     * the level about the horse's actual bounding box rather than about a block
     * shape, which is the part a by-hand check keeps getting wrong: a horse is
     * 1.4 wide, so standing "next to" a wall on a .5 coordinate overlaps it.
     * The search spirals outward a block at a time and gives up after
     * {@value #CLEAR_RADIUS}, because a pen with nowhere clear in five blocks
     * is a pen with a real problem and quietly teleporting the horse across the
     * yard would hide it.
     */
    private static void placeClear(ServerLevel level, Horse horse, double x, double floorY,
                                   double z) {
        horse.setPos(x, floorY, z);
        if (level.noCollision(horse)) {
            return;
        }
        for (int r = 1; r <= CLEAR_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;   // ring only; the inside was tried last time round
                    }
                    horse.setPos(x + dx, floorY, z + dz);
                    if (level.noCollision(horse) && standsOnSomething(level, horse)) {
                        HorseGenetics.LOGGER.warn("[Debug] {} would have spawned inside a block at "
                                        + "{}, {}, {} - moved {} block(s)",
                                horse.getType().toShortString(), (int) x, (int) floorY, (int) z, r);
                        return;
                    }
                }
            }
        }
        horse.setPos(x, floorY, z);
        HorseGenetics.LOGGER.error("[Debug] nowhere clear within {} blocks of {}, {}, {} - a horse "
                + "is about to suffocate there, and the pen around it is wrong",
                CLEAR_RADIUS, (int) x, (int) floorY, (int) z);
    }

    /**
     * <b>And there has to be a floor.</b> {@code noCollision} answers "is this
     * space free", which over the void is emphatically yes - so the first
     * version of the spiral could walk a horse off the edge of the yard to find
     * room, and this dimension is a strip of floor surrounded by nothing. A
     * horse died {@code outOfWorld} during the yard's first build after the
     * spiral went in.
     */
    private static boolean standsOnSomething(ServerLevel level, Horse horse) {
        BlockPos under = BlockPos.containing(horse.getX(), horse.getY() - 0.2, horse.getZ());
        return !level.getBlockState(under).isAir();
    }

    /** How far {@link #placeClear} will look before it reports the pen as broken. */
    private static final int CLEAR_RADIUS = 5;

    // --- hay-bale return portal at the start of the plot ---

    private static void buildReturnPortal(ServerLevel level, Plot plot) {
        int px = plot.originX + 1;
        int gy = plot.baseY;
        BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();
        BlockState portal = ModBlocks.HAY_PORTAL.get().defaultBlockState()
                .setValue(HayPortalBlock.AXIS, Direction.Axis.Z);
        // interior z in {0,1}, y in {gy+1..gy+3}; hay frame all round it in the x = px plane
        for (int z = -1; z <= 2; z++) {
            fastSet(level, new BlockPos(px, gy, z), hay);
            fastSet(level, new BlockPos(px, gy + 4, z), hay);
        }
        for (int y = gy + 1; y <= gy + 3; y++) {
            fastSet(level, new BlockPos(px, y, -1), hay);
            fastSet(level, new BlockPos(px, y, 2), hay);
            fastSetPortal(level, new BlockPos(px, y, 0), portal);
            fastSetPortal(level, new BlockPos(px, y, 1), portal);
        }
        plot.exitPortal = new BlockPos(px, gy + 1, 0);
    }

    // --- leaving: take your tamed horses with you ---

    /**
     * The whole X slot, not just the part built so far. It has to be: a plot
     * that recycles an X where a previous visitor walked further leaves that
     * visitor's horses standing past the new frontier, and if the box stopped
     * at {@code highestIndex} they would never be cleared and the slot would
     * accumulate animals for the life of the world.
     */
    private static AABB plotBox(Plot plot) {
        // Wide enough for the back wall at originX-3..-1 AND for the test yard,
        // which hangs off the +Z side and reaches further west than the corridor
        // does. The box is what tears a plot down and what walks tamed horses
        // home, so anything built outside it leaks - see DebugTestYard.FAR_Z.
        int xLo = Math.min(plot.originX - 5, plot.originX + DebugTestYard.WEST_DX);
        int xHi = Math.max(plot.originX + (LAST_SEGMENT_INDEX + 2) * PERIOD + 3,
                plot.originX + DebugTestYard.EAST_DX);
        int yLo = plot.baseY - 4;
        int yHi = plot.baseY + WALL_TOP_DY + 2;
        return new AABB(xLo, yLo, -WALL_BEDROCK_Z - 1,
                xHi + 1, yHi + 1, Math.max(WALL_BEDROCK_Z + 2, DebugTestYard.FAR_Z));
    }

    /**
     * Teleport tamed horses out of {@code plot} to {@code destPos} in
     * {@code dest}, dropped in the air on a small grid with a brief spell of
     * invulnerability (see {@link HorsePortalManager#placeReturningHorse}).
     * {@code onlyOwner != null}
     * restricts it to horses that player tamed (used when other players are
     * still in the dimension); {@code null} takes every tamed horse (the
     * leaving player is the last one out). Returns how many were moved.
     */
    public static int evacuateTamedHorses(ServerLevel debug, Plot plot, UUID onlyOwner,
                                          ServerLevel dest, BlockPos destPos) {
        // TAMED AND OWNED, not merely tamed. The test yard stocks its pens with
        // tamed horses on purpose - vanilla will not breed an untamed one, and
        // half the yard's tests want a foal - but those are scenery, set tame
        // by setTamed(true) with no owner behind it. Taking "tamed" as the test
        // walked the entire yard out into the overworld every time the player
        // left: two dozen horses at the portal, a breeding field's worth of
        // them if it had been running, and an emptied yard on the way back in.
        // A horse a player tamed has an owner (tameWithName sets one); the
        // yard's do not, and that is the line between somebody's horse and the
        // furniture.
        List<AbstractHorse> horses = debug.getEntitiesOfClass(AbstractHorse.class, plotBox(plot),
                h -> h.isAlive() && h.isTamed() && h.getOwnerReference() != null);
        List<BlockPos> spots = new ArrayList<>();
        int moved = 0;
        for (AbstractHorse horse : horses) {
            if (onlyOwner != null) {
                EntityReference<LivingEntity> owner = horse.getOwnerReference();
                if (owner == null || !onlyOwner.equals(owner.getUUID())) {
                    continue;
                }
            }
            if (horse.isLeashed()) {
                horse.dropLeash();
            }
            // dropped in the air just above the return portal, spread on a grid,
            // with a few seconds of invulnerability to cover the short fall
            HorsePortalManager.placeReturningHorse(horse, dest, destPos, spots);
            moved++;
        }
        return moved;
    }

    // --- teardown ---

    /**
     * Clear a plot on the way out: <b>entities only</b>. Everything that isn't a
     * player is discarded, and any horse among them is dropped from the ancestry
     * database too - otherwise every visit would leave hundreds of throwaway
     * gallery records in the save forever. (Records that merely *reference* a
     * forgotten horse as a parent are left alone; {@code ancestorsOf} already
     * skips ancestors it can't find.) Tamed horses have already been moved out
     * by {@link #evacuateTamedHorses} before this runs, so they're never caught
     * here.
     *
     * <p>The <b>blocks are deliberately left standing</b>. The corridor's
     * geometry is fixed ({@link #PEN_COUNT} pens, fixed {@link #PLOT_BASE_Y}),
     * so a plot rebuilt on a recycled X lands exactly on top of the old one and
     * overwrites it. Its <i>contents</i> are random now and therefore different
     * every time, but every pen a player can reach is rebuilt from index 0
     * upward as they walk - sign and horses together - so a stale genotype is
     * never on show. Air-filling the corridor instead would be a very expensive
     * way to leave, for nothing.
     */
    private static void tearDown(ServerLevel level, Plot plot) {
        HorseAncestryData ancestry = level.getServer() == null
                ? null : HorseAncestryData.get(level.getServer());
        int removed = sweepPlot(level, plot, ancestry);
        // The watch's forced chunks go AFTER the sweep, not before. They have
        // to go at all - otherwise the dimension keeps ticking a yard that is
        // nobody's for the life of the world - but releasing them first means
        // the yard can unload out from under the very query that is supposed to
        // be emptying it.
        DebugWorldWatch.stop(level);
        FREE_ORIGINS.add(plot.originX);
        HorseGenetics.LOGGER.info("[Debug] plot at x={} torn down, {} entit{} removed",
                plot.originX, removed, removed == 1 ? "y" : "ies");
    }

    /**
     * <b>Empty a plot chunk by chunk, loading each one first.</b>
     *
     * <p>This used to be a single {@code getEntities(plotBox(plot))} and that
     * <b>silently leaked most of the plot</b>. An entity query only ever returns
     * what is in a <i>loaded</i> chunk, and a plot is a corridor thousands of
     * blocks long: the far end is unloaded the moment the player walks back,
     * so every horse they had walked past survived the teardown and stayed in
     * the world for ever.
     *
     * <p>Measured 2026-09-13, and it took a player death to make it visible.
     * The owner died in the horse dimension, respawned, and walked back in - a
     * fresh plot, so a teardown of the old one - and the census went from
     * <b>205 horses to 290</b> across the rebuild. Eighty-five left behind,
     * which is about twenty corridor segments: exactly the stretch she had
     * walked and left loaded-then-unloaded on the first visit. Every re-entry
     * would have added another heap, and the entity count - the one number that
     * says whether the dimension is leaking at all - was measuring the leak
     * rather than reporting it.
     *
     * <h2>Why chunk by chunk, and why bounded by {@code highestIndex}</h2>
     * The plot's <i>declared</i> box reaches
     * {@value #PEN_COUNT} pens, which is some seven thousand blocks of x - call
     * it seven thousand chunks with the yard's width. Forcing all of those to
     * empty a corridor that was built thirty segments deep would be far worse
     * than the bug. {@link Plot#highestIndex} is what was actually built, so
     * that is what gets walked.
     *
     * <p>One chunk is forced at a time and released immediately, so the peak
     * cost is one chunk rather than the whole corridor. It is slower than a
     * single query and it happens once, on the way out of a dimension that is
     * already teleporting you.
     */
    private static int sweepPlot(ServerLevel level, Plot plot, @Nullable HorseAncestryData ancestry) {
        AABB box = builtBox(plot);
        int removed = 0;
        int cx0 = SectionPos.blockToSectionCoord((int) Math.floor(box.minX));
        int cx1 = SectionPos.blockToSectionCoord((int) Math.ceil(box.maxX));
        int cz0 = SectionPos.blockToSectionCoord((int) Math.floor(box.minZ));
        int cz1 = SectionPos.blockToSectionCoord((int) Math.ceil(box.maxZ));
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                boolean forced = level.setChunkForced(cx, cz, true);
                AABB chunkBox = new AABB(
                        SectionPos.sectionToBlockCoord(cx), box.minY, SectionPos.sectionToBlockCoord(cz),
                        SectionPos.sectionToBlockCoord(cx + 1), box.maxY, SectionPos.sectionToBlockCoord(cz + 1));
                for (Entity e : level.getEntities((Entity) null, chunkBox.intersect(box),
                        e -> !(e instanceof ServerPlayer))) {
                    if (ancestry != null && e instanceof Horse horse && HorseRecords.hasRealRecord(horse)) {
                        ancestry.forget(HorseRecords.of(horse).id());
                    }
                    e.discard();
                    removed++;
                }
                if (forced) {
                    level.setChunkForced(cx, cz, false);
                }
            }
        }
        return removed;
    }

    /**
     * The part of {@link #plotBox} that was <b>actually built</b>. The declared
     * box is sized for {@value #PEN_COUNT} pens because that is the corridor's
     * limit; {@link Plot#highestIndex} is how many of them exist.
     */
    private static AABB builtBox(Plot plot) {
        AABB declared = plotBox(plot);
        int xHi = Math.max(plot.originX + (plot.highestIndex + 2) * PERIOD + 3,
                plot.originX + DebugTestYard.EAST_DX) + 1;
        return new AABB(declared.minX, declared.minY, declared.minZ,
                Math.min(declared.maxX, xHi), declared.maxY, declared.maxZ);
    }

    static void fastSet(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2); // UPDATE_CLIENTS only - bulk terrain, skip neighbour updates
    }

    /**
     * Like {@link #fastSet}, plus bit 16 ({@code UPDATE_KNOWN_SHAPE}) to suppress
     * neighbour <b>shape</b> updates - which flag 2 does <i>not</i>.
     *
     * <p>Only the portal cells need it, and they genuinely do:
     * {@link HayPortalBlock#updateShape} turns an unenclosed portal block into
     * air, and this method builds the frame a row at a time, so a portal block
     * placed before the hay above it would ask itself whether it is enclosed,
     * find air, and delete itself. Same reason {@code HorsePortalManager.fill}
     * passes 18.
     */
    private static void fastSetPortal(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 18);
    }

    private DebugPenManager() {
    }
}
