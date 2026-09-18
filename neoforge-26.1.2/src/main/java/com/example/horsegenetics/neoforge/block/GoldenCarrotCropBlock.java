package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>Golden carrots, grown rather than crafted.</b>
 *
 * <p>Mechanically it is vanilla's carrot, copied on purpose: eight ages, needs
 * farmland and light, bone-mealable, same growth rate. Everything that is
 * different about it is in how you <i>get</i> the seeds - the equestrian
 * supplier's top rank, or a dungeon chest - and in what it drops.
 *
 * <h2>No gold comes out of it, at any point</h2>
 * That was the condition on building it at all. The loot table drops golden
 * carrots and its own seeds and nothing else - no nugget, no ingot - and a
 * golden carrot cannot be turned back into gold by any vanilla recipe. So the
 * crop is a way to <b>grow horse food</b>, not a gold farm, and a player who
 * plants a field of it ends up with a field of horse food.
 *
 * <p>It is also why the seeds are their own item rather than the golden carrot
 * itself being plantable: if a golden carrot were the seed, every golden carrot
 * ever crafted from eight nuggets would become plantable, and the rarity gate
 * the owner asked for would be worth nothing.
 *
 * <h2>Why it is not simply a data-driven block</h2>
 * {@link CropBlock} needs to know its own seed item
 * ({@link #getBaseSeedId()}), which is a Java method and not a property. That
 * one override is the whole class.
 */
public class GoldenCarrotCropBlock extends CropBlock {

    public GoldenCarrotCropBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Vanilla's carrot properties, as close as they can be copied.
     *
     * <p>{@code randomTicks} is the one that matters - without it the crop is
     * planted and never grows, which looks exactly like a broken age property
     * and is the first thing to check if someone reports one.
     */
    public static BlockBehaviour.Properties cropProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.PLANT)
                .noCollision()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.GOLDEN_CARROT_SEEDS.get();
    }

    /**
     * Bone meal grows it a little slower than a carrot would.
     *
     * <p>Not a balance lever so much as a signal: this is the expensive crop,
     * and a stack of bone meal should not turn one traded seed into a field in
     * ten seconds. Vanilla's {@code CropBlock} default is 2-5 ages per
     * application; this is 1-3.
     */
    @Override
    protected int getBonemealAgeIncrease(final net.minecraft.world.level.Level level) {
        return 1 + level.getRandom().nextInt(3);
    }

    @Override
    public boolean isRandomlyTicking(final BlockState state) {
        return !this.isMaxAge(state);
    }
}
