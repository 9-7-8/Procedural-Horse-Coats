package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Stands a small house next to the barn, so the horseman has somewhere to sleep
 * that is not the barn.
 *
 * <h2>Why not the barn</h2>
 * A villager brain claims a bed and walks to it at night, and that is the whole
 * of what the horseman needs - he is an ordinary villager and all of it is free.
 * The barn is the wrong building for it. It is a stable: four double doors and a
 * two-block gap at each end, and <b>a villager only ever shuts the door he
 * walked through</b>. Putting a bed in there is putting a bed in a corridor with
 * three doors standing open. Beds were tried in the barn and taken out again.
 *
 * <h2>What it places</h2>
 * {@code horsegenetics:cowboy_house}, which is a copy of vanilla's
 * {@code plains_small_house_1} with its jigsaw blocks resolved and a second bed
 * added ({@code tools/barn/bake-house.py}). Copied rather than referenced
 * because both of those changes are needed and because a vanilla template is not
 * ours to rely on keeping its shape.
 *
 * <h2>Where</h2>
 * On the far side of the barn from the village, {@link #NEAR} to {@link #FAR}
 * blocks out, on a patch that is <b>flat, clear and untouched</b>: every column
 * of the footprint has to agree on its surface height and be open above it. That
 * is a strict test and it fails often, which is the intended trade - a run of
 * candidates that all fail means no house, and no house is a great deal better
 * than a house dropped through somebody's roof. It is also what keeps a hitch
 * planted in the middle of a player's base from building on it.
 */
public final class CowboyHouseBuilder {

    /** The template: a vanilla plains house with the serial numbers filed off. */
    private static final Identifier HOUSE =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "cowboy_house");

    /** Nearest and furthest the house may stand from the barn. */
    private static final int NEAR = 10;
    private static final int FAR = 22;

    /** How wide a fan of directions out of town to sample. */
    private static final double ARC = Math.PI;

    /** Sites tried before giving up on a house entirely. */
    private static final int TRIES = 48;

    private CowboyHouseBuilder() {
    }

    /**
     * Build it, if there is anywhere to build it. Called once, when the cowboy
     * founds - the barn has no other "I have just been generated" moment.
     */
    public static void raise(ServerLevel level, BlockPos barn, double outward) {
        Optional<StructureTemplate> loaded = level.getServer().getStructureManager().get(HOUSE);
        if (loaded.isEmpty()) {
            HorseGenetics.LOGGER.warn("No {} template - the horseman gets no house", HOUSE);
            return;
        }
        StructureTemplate house = loaded.get();
        Rotation rotation = Rotation.getRandom(level.getRandom());
        Vec3i size = house.getSize(rotation);

        BlockPos corner = findSite(level, barn, outward, size);
        if (corner == null) {
            DebugAnnounce.log("Cowboy", "no flat clear ground near " + barn + " for a house");
            return;
        }
        house.placeInWorld(level, corner, corner,
                new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true),
                level.getRandom(), Block.UPDATE_CLIENTS);
        layPath(level, barn, corner.offset(size.getX() / 2, 0, size.getZ() / 2));
        DebugAnnounce.sayAt(level, "Cowboy", "a house went up for the horseman",
                corner, ChatFormatting.GRAY);
    }

    /**
     * Tread a path from the barn to the house.
     *
     * <p>Two buildings on the edge of a village with nothing between them read as
     * two things that happen to be near each other. A line of
     * {@code dirt_path} - the same block the village's own streets are made of -
     * is what says they belong to the same people, and it costs one block per
     * step.
     *
     * <p>It walks straight from one to the other and paves only what is
     * <b>already ground</b>. That single test does all the work: the columns
     * under the barn and the house report planks and cobble rather than dirt and
     * are skipped, so the path stops at each doorstep without anyone having to
     * work out where the walls are; and a player's floor is not dirt either, so
     * it will not draw itself across somebody's build. Anything growing on the
     * ground - grass, a flower - is trodden down, which is what a path does.
     */
    private static void layPath(ServerLevel level, BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        int steps = (int) Math.ceil(Math.sqrt(dx * dx + dz * dz));
        for (int step = 0; step <= steps; step++) {
            double along = steps == 0 ? 0.0 : (double) step / steps;
            pave(level, new BlockPos(
                    from.getX() + (int) Math.round(dx * along),
                    from.getY(),
                    from.getZ() + (int) Math.round(dz * along)));
        }
    }

    /** One step of the path, if this column is ground and nothing solid is on it. */
    private static void pave(ServerLevel level, BlockPos column) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        BlockPos ground = top.below();
        if (!level.getBlockState(ground).is(BlockTags.DIRT)) {
            return; // a floor, a roof, a road already, or water
        }
        BlockState standing = level.getBlockState(top);
        if (!standing.isAir() && !standing.canBeReplaced()) {
            return; // a fence, a wall, somebody's chest
        }
        if (!standing.isAir()) {
            level.setBlock(top, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(ground, Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /**
     * The north-west corner of a patch the house will sit on honestly.
     *
     * @param outward the bearing from the village bell to the barn, so the house
     *                goes on the country side rather than into the square
     */
    private static @Nullable BlockPos findSite(ServerLevel level, BlockPos barn, double outward, Vec3i size) {
        for (int attempt = 0; attempt < TRIES; attempt++) {
            double angle = outward + (level.getRandom().nextDouble() - 0.5) * ARC;
            int distance = NEAR + level.getRandom().nextInt(FAR - NEAR + 1);
            BlockPos centre = barn.offset(
                    (int) Math.round(Math.cos(angle) * distance), 0,
                    (int) Math.round(Math.sin(angle) * distance));
            BlockPos corner = centre.offset(-size.getX() / 2, 0, -size.getZ() / 2);
            Integer ground = levelGroundAt(level, corner, size);
            if (ground != null) {
                return new BlockPos(corner.getX(), ground, corner.getZ());
            }
        }
        return null;
    }

    /**
     * The one surface height every column of this footprint shares, or
     * {@code null} if they do not share one.
     *
     * <p>Strict on purpose. A house wants a level plot; anything else is a house
     * with a corner in the air or a corner buried, and the mod has no business
     * terraforming to avoid that. It also does the "is this ground clear?" test
     * for free - the surface heightmap sits on top of whatever is there, so a
     * column with a tree or a fence or somebody's wall in it reports a different
     * height from its neighbours and the site is rejected.
     */
    private static @Nullable Integer levelGroundAt(ServerLevel level, BlockPos corner, Vec3i size) {
        Integer surface = null;
        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                BlockPos column = corner.offset(dx, 0, dz);
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                if (surface == null) {
                    surface = top.getY();
                } else if (top.getY() != surface) {
                    return null;
                }
                if (!level.getBlockState(top.below()).isSolidRender()) {
                    return null; // water, or a hole
                }
            }
        }
        return surface;
    }
}
