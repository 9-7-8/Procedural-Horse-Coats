package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.block.HayPortalBlock;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.block.state.BlockState;
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
 * Builds and populates the debug-pen dimension. Debug-tool code, not a
 * gameplay feature - it exists to eyeball coat-genetics output across many
 * horses fast. All calls happen on the server thread.
 *
 * <h2>Instancing</h2>
 * The dimension is a flat <b>void</b> (see {@code dimension/debug_pens.json}):
 * the generator lays down nothing at all. Every visit gets its own private
 * <b>plot</b> - a fresh corridor built by this class at a unique X far from
 * every other plot ({@value #PLOT_SPACING_X} blocks) and at a random Y, so
 * "two people never end up in the same place" holds even on a shared server.
 * When the plot's player leaves (dimension change, logout, or a re-entry that
 * supersedes it) the plot is torn down: its entities are discarded and its
 * blocks are set back to air. Nothing left in the dimension survives.
 *
 * <h2>Layout of one plot</h2>
 * A straight corridor running +X from {@code originX}. The wall <b>behind the
 * return portal</b> is layered (bedrock at {@code originX-3}, oak-plank wood
 * wall at {@code originX-2}, gravel face at {@code originX-1}, glowstone line
 * above). Just past it is a hay-bale return portal ({@link HorsePortalManager})
 * at {@code originX+1}; the player spawns on the road a few blocks further in,
 * facing down the corridor. Down the centre is a gravel road
 * ({@code z} in [-{@value #ROAD_HALF_WIDTH}, {@value #ROAD_HALF_WIDTH}]). A
 * pen sits on each side: {@value #PEN_LEN_X} blocks along X,
 * {@value #PEN_DEPTH_Z} deep, brick-wall perimeter with a <b>two-wide</b>
 * oak-fence-gate opening (horses won't cross a 1-wide gap), one gravel strip
 * between consecutive pens. Outward from each pen's back edge, flush (no grass
 * gap): a gravel strip ({@code z} = +/-{@value #GRAVEL_STRIP_Z}) with a
 * glowstone line {@value #WALL_TOP_DY} blocks above it, a single oak-plank wood
 * wall ({@code z} = +/-{@value #WALL_PLANK_Z}), then the bedrock core
 * ({@code z} = +/-{@value #WALL_BEDROCK_Z}). Outside the bedrock: open void.
 */
public final class DebugPenManager {

    public static final ResourceKey<Level> DEBUG_LEVEL = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "debug_pens"));

    private static final int PEN_LEN_X = 6;              // pen extent along the corridor
    private static final int PEN_DEPTH_Z = 20;           // pen extent from the road outward
    private static final int PEN_GAP_X = 1;              // single gravel strip between consecutive pens
    private static final int PERIOD = PEN_LEN_X + PEN_GAP_X;
    // Build this many pens beyond the player so the ground never "pops in" (~180 blocks).
    private static final int LOOKAHEAD_PENS = 30;

    private static final int ROAD_HALF_WIDTH = 3;        // gravel road: z in [-3, 3]
    private static final int WALL_TOP_DY = 10;           // glowstone line height above the floor

    // Outward from a pen's back edge (PEN_FAR_Z): a gravel strip flush against
    // the pen (glowstone line directly above it), a single oak-plank wood wall,
    // then the bedrock core. No grass gap.
    private static final int PEN_FAR_Z = ROAD_HALF_WIDTH + PEN_DEPTH_Z;   // 23 - pen back edge (brick wall)
    private static final int GRAVEL_STRIP_Z = PEN_FAR_Z + 1;             // 24 - gravel strip, glowstone line above it
    private static final int WALL_PLANK_Z = PEN_FAR_Z + 2;              // 25 - single oak-plank wood wall
    private static final int WALL_BEDROCK_Z = PEN_FAR_Z + 3;           // 26 - bedrock core (last solid block)

    // Plots are spaced far enough apart on X that they never share chunks; the
    // random Y is belt-and-braces so nothing ever visually overlaps.
    private static final int PLOT_SPACING_X = 20_000;
    private static final int PLOT_MIN_Y = 96;
    private static final int PLOT_Y_RANGE = 300;         // dimension is 512 tall (see dimension_type)

    /** Geometry for one side of the road. */
    private record PenSpec(int zRoad, int zBack, Direction roadFacing) {}

    private static final PenSpec NORTH_PEN = new PenSpec(ROAD_HALF_WIDTH + 1, PEN_FAR_Z, Direction.NORTH);
    private static final PenSpec SOUTH_PEN = new PenSpec(-(ROAD_HALF_WIDTH + 1), -PEN_FAR_Z, Direction.SOUTH);

    /** One private instance of the corridor. Mutable {@code highestIndex} tracks how far it's been built. */
    static final class Plot {
        final int originX;
        final int baseY;                       // grass-surface Y for this plot
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
        int baseY = PLOT_MIN_Y + player.getRandom().nextInt(PLOT_Y_RANGE);
        Plot plot = new Plot(originX, baseY, returnDim, returnPos.immutable());
        PLOTS.put(player.getUUID(), plot);

        ensureBuiltUpToIndex(debug, plot, LOOKAHEAD_PENS);

        // Spawn on the road just past the return portal, facing +X down the corridor.
        player.teleportTo(debug, originX + 3.5, baseY + 1, 0.5, Set.of(), -90.0f, 0.0f, false);
        giveDebugPaper(player);
    }

    /** Drop the player's plot (if any) and wipe it. Safe to call for players who never entered. */
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
            int end = p.originX + (p.highestIndex + 2) * PERIOD;
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

    private static void ensureBuiltUpToIndex(ServerLevel level, Plot plot, int targetIndex) {
        while (plot.highestIndex < targetIndex) {
            int idx = plot.highestIndex + 1;
            buildSegment(level, plot, idx);
            plot.highestIndex = idx;
        }
    }

    private static void buildSegment(ServerLevel level, Plot plot, int index) {
        int x0 = plot.originX + index * PERIOD;
        if (index == 0) {
            buildStartCap(level, plot);
        }
        buildCorridor(level, plot, x0);
        buildPen(level, plot, x0, NORTH_PEN);
        buildPen(level, plot, x0, SOUTH_PEN);
        if (index == 0) {
            buildReturnPortal(level, plot);
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

    private static void buildPen(ServerLevel level, Plot plot, int x0, PenSpec pen) {
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

        AABB interior = new AABB(x0, floorY, zLo, xMax + 1, floorY + 4, zHi + 1);
        if (level.getEntitiesOfClass(Horse.class, interior).isEmpty()) {
            double midX = x0 + PEN_LEN_X / 2.0;
            double midZ = (zLo + zHi) / 2.0;
            spawnHorse(level, floorY, midX, midZ - 4, Sex.MALE);
            spawnHorse(level, floorY, midX, midZ + 4, Sex.FEMALE);
        }
    }

    private static void torchOnFence(ServerLevel level, int x, int floorY, int z) {
        level.setBlock(new BlockPos(x, floorY + 1, z), Blocks.TORCH.defaultBlockState(), 2);
    }

    private static void spawnHorse(ServerLevel level, int floorY, double x, double z, Sex sex) {
        Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.COMMAND);
        if (horse == null) {
            return;
        }
        horse.setPos(x, floorY, z);
        HorseRecords.apply(horse, HorseRecords.newFounder(horse, new NeoRng(horse.getRandom()), sex));
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

    private static AABB plotBox(Plot plot) {
        int xLo = plot.originX - 5; // covers the layered back wall at originX-3..-1
        int xHi = plot.originX + (plot.highestIndex + 1) * PERIOD + 3;
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

    private static void tearDown(ServerLevel level, Plot plot) {
        AABB box = plotBox(plot);
        int xLo = plot.originX - 5;
        int xHi = plot.originX + (plot.highestIndex + 1) * PERIOD + 3;
        int yLo = plot.baseY - 4;
        int yHi = plot.baseY + WALL_TOP_DY + 2;

        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof ServerPlayer))) {
            e.discard();
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = xLo; x <= xHi; x++) {
            for (int y = yLo; y <= yHi; y++) {
                for (int z = -WALL_BEDROCK_Z - 1; z <= WALL_BEDROCK_Z + 1; z++) {
                    level.setBlock(m.set(x, y, z), air, 2);
                }
            }
        }
        FREE_ORIGINS.add(plot.originX);
    }

    private static void fastSet(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2); // UPDATE_CLIENTS only - bulk terrain, skip neighbour updates
    }

    private DebugPenManager() {
    }
}
