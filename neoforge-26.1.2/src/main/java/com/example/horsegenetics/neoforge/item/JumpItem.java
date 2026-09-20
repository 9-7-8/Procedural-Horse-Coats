package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.block.JumpBlock;
import com.example.horsegenetics.neoforge.block.JumpMaterials;
import com.example.horsegenetics.neoforge.block.JumpWoods;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * <b>The jump item.</b> One of them, carrying the two woods it is made of.
 *
 * <h2>Nothing else is on the item, and that is the design</h2>
 * There were thirty-six of these (twelve woods times three styles), then three,
 * then one. The <b>style</b> is a blockstate property chosen in the block's own
 * screen after placing; the <b>woods</b> ride on the stack as two string
 * components and are handed to the block entity by
 * {@code JumpBlock.setPlacedBy}. Every style added from here is a button in
 * that screen rather than a thirty-seventh item.
 *
 * <p>The woods stay on the item, rather than joining style behind the screen,
 * for one reason: a jump is <em>crafted</em> out of a wood. Three birch fences
 * have to give you something that is already birch, or the recipe is a lie and
 * every crafted jump needs a visit to a screen before it looks like what you
 * paid for.
 */
public class JumpItem extends BlockItem {

    public JumpItem(JumpBlock block, Properties properties) {
        super(block, properties);
    }

    /**
     * <b>"Oak Horse Jump", or "Oak &amp; Birch Horse Jump".</b>
     *
     * <p>One item covering every wood means the id cannot name the wood any
     * more, so the name is built from the components instead. Without this a
     * chest of jumps in six woods is six stacks all called "Horse Jump", which
     * is worse than the thirty-six items were.
     *
     * <p>A jump whose rails and standards differ names both, rails first,
     * because that is the order the screen lists them in and the rails are the
     * half you actually jump. <b>The style is deliberately not in the name</b>:
     * it is not a property of the item at all, and a stack of jumps is not a
     * stack of oxers.
     */
    @Override
    public Component getName(ItemStack stack) {
        JumpMaterials materials = JumpMaterials.fromComponents(stack);
        // item.horsegenetics.jump, which bake-jumps.mjs writes both keys under.
        String key = this.getDescriptionId();
        if (materials.uniform()) {
            return Component.translatable(key, JumpWoods.label(materials.rails()));
        }
        return Component.translatable(key + ".mixed",
                JumpWoods.label(materials.rails()), JumpWoods.label(materials.standards()));
    }
}
