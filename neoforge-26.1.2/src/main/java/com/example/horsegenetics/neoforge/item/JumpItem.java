package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.block.JumpBlock;
import com.example.horsegenetics.neoforge.block.JumpMaterials;
import com.example.horsegenetics.neoforge.block.JumpWoods;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * <b>One jump item per style</b>, all placing the same block, and each carrying
 * the two woods it is made of.
 *
 * <h2>Style is a property, woods are components</h2>
 * The style is a blockstate property, so a single {@link BlockItem} would only
 * ever place the default - which is exactly what the owner found: "I only see
 * the vertical in the creative tab". Several {@code BlockItem}s may point at
 * one {@code Block} and differ only in the state they place, so each style gets
 * an item.
 *
 * <p>The <i>woods</i> go the other way. Twelve of them times twelve again is
 * not a property anybody can afford, so they ride on the stack as two string
 * components and are handed to the block entity by
 * {@code JumpBlock.setPlacedBy}. That is why there are three items here and not
 * thirty-six.
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

    /**
     * The style goes on <b>after</b> the block's own
     * {@code getStateForPlacement}, and the connection flags are then recomputed
     * <b>after that</b>.
     *
     * <p>The recompute is not optional: a jump only connects to a jump of the
     * same style, and the block computed its flags while this stack's style was
     * still the default. Without the second pass an oxer placed beside a
     * vertical would come out sharing its standards - the look that was
     * deliberately stopped.
     */
    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        return JumpBlock.connected(state.setValue(JumpBlock.STYLE, this.style),
                context.getLevel(), context.getClickedPos());
    }

    /**
     * <b>"Oak Jump", or "Oak &amp; Birch Oxer".</b>
     *
     * <p>Three items covering every wood means the id cannot name the wood any
     * more, so the name is built from the components instead. Without this a
     * chest of jumps in six woods is six stacks all called "Jump", which is
     * worse than the thirty-six items were.
     *
     * <p>A jump whose rails and standards differ names both, rails first,
     * because that is the order the screen lists them in and the rails are the
     * half you actually jump.
     */
    @Override
    public Component getName(ItemStack stack) {
        JumpMaterials materials = JumpMaterials.fromComponents(stack);
        // The item's own description id - item.horsegenetics.jump_oxer and so
        // on - which is what bake-jumps.mjs writes the two keys under. None of
        // the three uses useBlockDescriptionPrefix(), precisely so that this is
        // predictable.
        String key = this.getDescriptionId();
        if (materials.uniform()) {
            return Component.translatable(key, JumpWoods.label(materials.rails()));
        }
        return Component.translatable(key + ".mixed",
                JumpWoods.label(materials.rails()), JumpWoods.label(materials.standards()));
    }
}
