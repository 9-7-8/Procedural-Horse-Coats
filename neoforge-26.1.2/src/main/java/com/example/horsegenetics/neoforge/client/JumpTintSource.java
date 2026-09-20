package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.block.JumpBlockEntity;
import com.example.horsegenetics.neoforge.block.JumpMaterials;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * <b>The paint on a jump</b>, as a tint on the half it was applied to.
 *
 * <h2>A tint, not a texture, and here that is the right tool</h2>
 * The jump's <i>wood</i> is a texture swap and could never have been a tint -
 * multiplying a colour over birch grain does not produce jungle grain, which is
 * why the woods became block-entity data and a per-position model. <b>Paint is
 * the opposite case.</b> It is a colour laid over a wood that still shows
 * through, which is exactly what multiplying does, so the same mechanism
 * vanilla dyes leather with is the honest one.
 *
 * <p>It also means paint costs nothing the woods did not already cost: the two
 * halves are already separate model parts, so tagging every rail face
 * {@code tintindex: 0} and every upright face {@code tintindex: 1} is enough to
 * colour them independently.
 *
 * <h2>Multiplying can only darken, and the art is chosen for it</h2>
 * A tint is a multiply, so a painted jump is <b>the wood seen through the
 * colour</b> rather than a flat coat of it. Red on birch is a strong red; red
 * on dark oak is a dark, woody red. That is the look, and it is why the owner
 * asked for a "high opacity tint" rather than a subtle wash: vanilla's dye
 * colours are saturated enough to dominate a mid-tone plank. A player wanting
 * the brightest possible paint picks a pale wood to paint, which is also how
 * real painted rails work.
 *
 * <h2>Registration is a LIST, and the list index is the tint index</h2>
 * {@code RegisterColorHandlersEvent.BlockTintSources.register(List, Block...)}.
 * <b>Not</b> {@code RegisterColorHandlersEvent.Block} with a per-index lambda,
 * which is what every older version and every tutorial has - see
 * {@code wiki/coding-notes.html}. So {@link #BLOCK_SOURCES} is ordered, and its
 * order is the same contract as {@code RAILS_TINT}/{@code STANDARDS_TINT} in
 * {@code bake-jumps.mjs} and the {@code tints} array in the item definition.
 *
 * @see JumpBlockEntity for the re-mesh that makes a fresh coat of paint appear
 */
public final class JumpTintSource implements BlockTintSource {

    /** White: a multiply by this is the wood exactly as it was drawn. */
    private static final int BARE = 0xFFFFFFFF;

    private final boolean rails;

    private JumpTintSource(boolean rails) {
        this.rails = rails;
    }

    /**
     * The two halves, <b>in tint-index order</b>: rails first.
     *
     * <p>Registered against the one jump block by {@code ClientSetup}.
     */
    public static final List<BlockTintSource> BLOCK_SOURCES =
            List.of(new JumpTintSource(true), new JumpTintSource(false));

    /**
     * Out of the world - an inventory block, a particle with no position.
     * Bare wood, because there is no block entity to ask and a guess would be
     * wrong more often than white is.
     */
    @Override
    public int color(BlockState state) {
        return BARE;
    }

    /**
     * <b>In the world, where the paint actually lives.</b>
     *
     * <p>Runs on <b>chunk-meshing worker threads</b>, exactly as
     * {@code JumpModel.collectParts} does, and against the same region
     * snapshot. Reading the block entity through {@code getModelData} rather
     * than through {@code getBlockEntity} is what keeps that safe: the data is
     * a snapshot of an immutable record taken on the main thread, which is the
     * whole reason {@link JumpMaterials} is a record in the first place.
     */
    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        JumpMaterials materials = level.getModelData(pos).get(JumpBlockEntity.MATERIALS);
        return materials == null ? BARE : ARGB.opaque(materials.tint(this.rails));
    }

    /**
     * <b>The same paint on the item's icon.</b>
     *
     * <p>A separate class because block tints and item tints are different
     * interfaces with different inputs - a block tint is handed a position and
     * an item tint is handed a stack - and the item's two woods and two colours
     * are components rather than block-entity data.
     *
     * <p>Registered by id ({@code horsegenetics:jump_rails_tint} and
     * {@code horsegenetics:jump_standards_tint}) and named in the {@code tints}
     * array of {@code assets/horsegenetics/items/jump.json}, exactly as
     * {@link TackTintSource} is named in the saddle's.
     */
    public record Item(boolean rails) implements ItemTintSource {

        public static final MapCodec<Item> RAILS_CODEC = MapCodec.unit(new Item(true));
        public static final MapCodec<Item> STANDARDS_CODEC = MapCodec.unit(new Item(false));

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level,
                             @Nullable LivingEntity owner) {
            return ARGB.opaque(JumpMaterials.fromComponents(stack).tint(this.rails));
        }

        @Override
        public MapCodec<? extends ItemTintSource> type() {
            return this.rails ? RAILS_CODEC : STANDARDS_CODEC;
        }
    }
}
