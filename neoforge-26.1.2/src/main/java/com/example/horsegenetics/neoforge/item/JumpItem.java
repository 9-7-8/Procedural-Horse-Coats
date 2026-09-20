package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.block.JumpBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * <b>One jump item per style</b>, all placing the same block.
 *
 * <p>The style is a blockstate property, so a single {@link BlockItem} would
 * only ever place the default - which is exactly what the owner found: "I only
 * see the vertical in the creative tab". Several {@code BlockItem}s may point
 * at one {@code Block} and differ only in the state they place, so each style
 * gets an item and the creative tab gets all of them.
 *
 * <p><b>A stick still restyles a placed jump in the world</b> (see
 * {@code JumpBlock.useItemOn}); these items are how you <i>start</i> with the
 * style you want rather than placing a vertical and converting it.
 *
 * <p>The style is applied <em>after</em> the block's own
 * {@code getStateForPlacement}, which is what lets that method align a stacked
 * jump with the one below it without overriding the style the player actually
 * chose. Facing follows the stack; style follows the hand.
 */
public class JumpItem extends BlockItem {

    private final JumpBlock.Style style;

    public JumpItem(JumpBlock block, JumpBlock.Style style, Properties properties) {
        super(block, properties);
        this.style = style;
    }

    public JumpBlock.Style style() {
        return this.style;
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(JumpBlock.STYLE, this.style);
    }
}
