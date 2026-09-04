package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.ShowcaseGenotypes;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.block.HayPortalBlock;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import net.minecraft.core.BlockPos;
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
 * Builds and populates the horse dimension: a corridor of pens, each holding a
 * <b>random pair</b> of horses to look at. All calls happen on the server
 * thread.
 *
 * <h2>The pens</h2>
 * Each pen gets one {@link ShowcaseGenotypes#random showcase genotype} - an
 * ordinary founder draw with a floor under it, so a pen is never a plain bay -
 * and stocks one mare and one stallion with it. The two share the genotype and
 * <i>not</i> the epigenome, so they are two examples rather than two copies. A
 * sign on the road, to the right of the gate, names the genotype in the same
 * compact form the horse's own info panel uses.
 *
 * <p>This used to be a <b>gallery of the genotype catalogue</b>: one pen per
 * visually distinct genotype, in odometer order, with the corridor length
 * derived from {@code GenotypeCatalog}. That premise stopped being buildable
 * long before it stopped being computable - every gene multiplies the
 * catalogue, the white-pattern loci alone put it past two million, and at seven
 * blocks a segment that is a corridor a quarter of the way to the world border.
 * Random pens do not care how large the genotype space is, which is exactly why
 * the corridor stops being the thing that blocks adding genes
 * ({@code wiki/roadmap.html} §8). The catalogue itself stays - it is still what
 * a punnett display and the tests want, it just no longer drives the dimension.
 *
 * <p>The corridor is {@value #PEN_COUNT} pens long: an arbitrary number, not a
 * derived one, chosen as far more than anyone walks. Pens are built lazily as
 * the player goes ({@link #ensureGeneratedAheadOfPlayer}) and the corridor
 * closes in an end wall.
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
 * <p>That is still safe now the pens are random, but for a different reason
 * than it used to be. The <b>geometry</b> is fixed ({@value #PEN_COUNT} pens,
 * fixed {@link #PLOT_BASE_Y}), so a plot rebuilt on a recycled X lands exactly
 * on the old one; the <b>contents</b> are not, but every pen a player can reach
 * is rebuilt from index 0 upward as they walk, signs and horses included, so
 * nothing stale is ever visible. The only leftovers are pens past the new
 * player's frontier, which they would have to walk to - and walking there
 * rebuilds them. {@link #buildPen} clears any untamed horse it finds before
 * stocking, so a previous occupant's animals cannot outlive their sign.
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
     * <b>How long the corridor is.</b> An arbitrary number now, and
     * deliberately so: the length used to be derived from the genotype
     * catalogue, which meant every gene added made the dimension bigger, and by
     * the white-pattern loci it wanted a corridor about seven <i>million</i>
     * blocks long - a quarter of the way to the world border. Random pens do
     * not have to enumerate anything, so the corridor is simply as long as is
     * worth walking: {@value} pens, one genotype each, about seven thousand
     * blocks end to end.
     */
    static final int PEN_COUNT = 2_000;

    /**
     * The corridor holds {@link #PEN_COUNT} pens, two per segment. Fixed, so a
     * plot rebuilt on a recycled X has the same geometry as the one it replaces
     * - see the teardown note in the class javadoc.
     */
    private static final int LAST_SEGMENT_INDEX =
            (PEN_COUNT + PENS_PER_SEGMENT - 1) / PENS_PER_SEGMENT - 1;

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
        int base = index * PENS_PER_SEGMENT;
        buildPen(level, plot, x0, NORTH_PEN, base);
        buildPen(level, plot, x0, SOUTH_PEN, base + 1);
        if (index == 0) {
            buildReturnPortal(level, plot);
            buildEntranceSign(level, plot);
        }
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

    private static void groundColumn(ServerLevel level, int x, int gy, int z, BlockState surface) {
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
        }
    }

    // --- one pen ---

    /**
     * One pen and the pair of horses in it. The genotype is a fresh
     * {@link ShowcaseGenotypes#random showcase draw} - a wild founder roll that
     * is guaranteed to express at least one natural gene beyond extension and
     * agouti, and half the time a magical one as well - and the sign by the
     * gate names it. {@code penIndex} is only the number on that sign.
     */
    private static void buildPen(ServerLevel level, Plot plot, int x0, PenSpec pen, int penIndex) {
        if (penIndex >= PEN_COUNT) {
            return;
        }
        Genotype genotype = ShowcaseGenotypes.random(new NeoRng(level.getRandom()));
        String geneticCode = genotype.toCode();
        int gy = plot.baseY;
        int floorY = gy + 1;
        int xMax = x0 + PEN_LEN_X - 1;
        // Two-wide gate opening in the middle of the road-side edge - a single
        // 1-wide gate lets horses slip out, so use two side by side.
        int gateX = x0 + PEN_LEN_X / 2 - 1;
        int zLo = Math.min(pen.zRoad(), pen.zBack());
        int zHi = Math.max(pen.zRoad(), pen.zBack());
        BlockState wall = Blocks.BRICK_WALL.defaultBlockState();
        BlockState gate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(FenceGateBlock.FACING, pen.roadFacing());

        for (int x = x0; x <= xMax; x++) {
            boolean isGate = x == gateX || x == gateX + 1;
            level.setBlockAndUpdate(new BlockPos(x, floorY, pen.zRoad()), isGate ? gate : wall);
            level.setBlockAndUpdate(new BlockPos(x, floorY, pen.zBack()), wall);
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

        buildPenSign(level, plot, x0, pen, penIndex, genotype);

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
        double midX = x0 + PEN_LEN_X / 2.0;
        double midZ = (zLo + zHi) / 2.0;
        spawnHorse(level, floorY, midX, midZ - 4, Sex.MALE, geneticCode);
        spawnHorse(level, floorY, midX, midZ + 4, Sex.FEMALE, geneticCode);
    }

    /** Drop {@code horse} from the ancestry database, if it has a record there. */
    private static void forget(ServerLevel level, Horse horse) {
        if (level.getServer() != null && HorseRecords.hasRealRecord(horse)) {
            HorseAncestryData.get(level.getServer()).forget(HorseRecords.of(horse).id());
        }
    }

    private static void torchOnFence(ServerLevel level, int x, int floorY, int z) {
        level.setBlock(new BlockPos(x, floorY + 1, z), Blocks.TORCH.defaultBlockState(), 2);
    }

    // --- signs -----------------------------------------------------------

    private static final int SIGN_LINES = 4;              // vanilla sign: 4 lines per face
    private static final int SIGN_GENE_LINES = SIGN_LINES - 1;  // line 0 is the pen number
    private static final int SIGN_LINE_CHARS = 15;        // about what a vanilla sign line fits

    /**
     * The genotype label for one pen: a standing sign on the road, immediately
     * to the <b>right of the gate</b> as you face the pen from the road. Both
     * faces carry the same text so it reads from anywhere on the road.
     */
    private static void buildPenSign(ServerLevel level, Plot plot, int x0, PenSpec pen,
                                     int penIndex, Genotype genotype) {
        int gateX = x0 + PEN_LEN_X / 2 - 1;               // gate occupies gateX and gateX + 1
        Direction towardPen = pen.roadFacing().getOpposite();
        Direction right = towardPen.getClockWise();       // always +/-X here
        int signX = right.getStepX() > 0 ? gateX + 2 : gateX - 1;
        int signZ = pen.zRoad() + pen.roadFacing().getStepZ();  // one block out onto the road
        placeSign(level, new BlockPos(signX, plot.baseY + 1, signZ), pen.roadFacing(),
                genotypeSignLines(penIndex, genotype));
    }

    /**
     * The sign three blocks in front of the entrance portal. It used to be a
     * tally of the genotype catalogue - how many genotypes exist, how many are
     * distinct, how many of those the corridor was showing. None of that is
     * true of a random corridor, so it says what <i>is</i>: how many pens there
     * are and what is in one.
     */
    private static void buildEntranceSign(ServerLevel level, Plot plot) {
        placeSign(level, new BlockPos(plot.originX + 4, plot.baseY + 1, 0), Direction.WEST,
                List.of("Horse Pens",
                        String.format("%,d pens", PEN_COUNT),
                        "random genome",
                        "mare + stallion"));
    }

    /**
     * Line 0 is the pen's 1-based number down the corridor; the rest is the
     * genotype in the <b>same compact form the horse's info panel and paper
     * dump use</b> ({@link GeneCodeDisplay#shortForm}) - extension + agouti,
     * then only the genes actually carrying a variant. Wrapped over the
     * remaining {@value #SIGN_GENE_LINES} lines between whole gene tokens;
     * {@code wrap} deliberately overflows its last line rather than dropping a
     * gene, which {@code ShowcaseGenotypesTest} pins over random draws.
     */
    private static List<String> genotypeSignLines(int penIndex, Genotype genotype) {
        List<String> lines = new ArrayList<>();
        lines.add("#" + (penIndex + 1));
        lines.addAll(GeneCodeDisplay.wrap(genotype, SIGN_GENE_LINES, SIGN_LINE_CHARS));
        return lines;
    }

    /**
     * A waxed standing oak sign at {@code pos}, its text facing {@code facing},
     * with {@code lines} written identically on both faces (lines past
     * {@value #SIGN_LINES} are dropped). Waxed so a visitor can't scribble over
     * the label.
     */
    private static void placeSign(ServerLevel level, BlockPos pos, Direction facing, List<String> lines) {
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

    private static void spawnHorse(ServerLevel level, int floorY, double x, double z, Sex sex,
                                   String geneticCode) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.COMMAND);
        if (horse == null) {
            return;
        }
        horse.setPos(x, floorY, z);
        // Record applied before the entity joins, so HorseGeneticsEventHandler
        // sees a real record and doesn't roll a random genotype over the top.
        HorseRecords.apply(horse,
                HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), sex, Genotype.parse(geneticCode)));
        level.addFreshEntity(horse);
    }

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
            fastSet(level, new BlockPos(px, y, 0), portal);
            fastSet(level, new BlockPos(px, y, 1), portal);
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
        int xLo = plot.originX - 5; // covers the layered back wall at originX-3..-1
        int xHi = plot.originX + (LAST_SEGMENT_INDEX + 2) * PERIOD + 3;
        int yLo = plot.baseY - 4;
        int yHi = plot.baseY + WALL_TOP_DY + 2;
        return new AABB(xLo, yLo, -WALL_BEDROCK_Z - 1, xHi + 1, yHi + 1, WALL_BEDROCK_Z + 2);
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
        List<AbstractHorse> horses = debug.getEntitiesOfClass(AbstractHorse.class, plotBox(plot),
                h -> h.isAlive() && h.isTamed());
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
        for (Entity e : level.getEntities((Entity) null, plotBox(plot), e -> !(e instanceof ServerPlayer))) {
            if (ancestry != null && e instanceof Horse horse && HorseRecords.hasRealRecord(horse)) {
                ancestry.forget(HorseRecords.of(horse).id());
            }
            e.discard();
        }
        FREE_ORIGINS.add(plot.originX);
    }

    private static void fastSet(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2); // UPDATE_CLIENTS only - bulk terrain, skip neighbour updates
    }

    private DebugPenManager() {
    }
}
