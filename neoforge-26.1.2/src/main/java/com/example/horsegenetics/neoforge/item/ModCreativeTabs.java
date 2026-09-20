package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * One creative tab holding every gameplay-layer item ({@link ModItems#TAB_ITEMS}).
 * The custom horse spawn egg additionally shows in the vanilla Spawn Eggs tab
 * (see {@link ModItems#addToCreativeTab}).
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HorseGenetics.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.horsegenetics.main"))
                    .icon(() -> new ItemStack(ModItems.HORSE_HAIR.get()))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .displayItems((params, output) -> {
                        ModItems.TAB_ITEMS.forEach(item -> output.accept(item.get()));
                        // The double gates are deliberately NOT here. They are
                        // filed into vanilla's Building Blocks tab instead, each
                        // directly after the single gate of its own wood, which
                        // is where somebody reaching for a gate looks - see
                        // ModItems.addToCreativeTab. Listing them here as well
                        // would put twelve wood variants in a tab that is about
                        // horses, and bury the things it exists for.
                        // One filled breed egg per breed that has one. The blank
                        // item is not listed anywhere: its whole content is the
                        // breed component, and an egg without one does nothing.
                        for (com.example.horsegenetics.common.breed.Breed breed
                                : com.example.horsegenetics.common.breed.Breeds.from(
                                        com.example.horsegenetics.common.breed.BreedSource.SPAWN_EGG)) {
                            output.accept(BreedSpawnEggItem.of(breed));
                        }
                        // THE JUMP, folded back in here. It had a tab of its
                        // own, on the argument that "each style is another
                        // twelve woods, so the roster grows by the dozen" and
                        // would bury the carrots and papers this tab exists
                        // for. That argument is dead: there is ONE jump item
                        // now, because both the wood and the style moved into
                        // the block's own screen, and every style added from
                        // here is a button rather than a thirty-seventh item.
                        // A tab holding one thing is worse than a row.
                        output.accept(oakJump());
                    })
                    .build());

    /**
     * <b>The creative tab's jump</b> - oak, and stamped with its woods.
     *
     * <p>Stamped rather than bare on purpose: an unstamped jump is a jump with
     * no components, which is a <i>different stack</i> from the oak one the
     * recipes produce, and two stacks of the same thing that refuse to merge
     * gets reported as an inventory bug. See {@code ModDataComponents.JUMP_RAILS}.
     *
     * <p>Only the plain oak one is offered. Every other wood is three fences in
     * a grid, or two planks in the block's own screen.
     */
    private static ItemStack oakJump() {
        ItemStack stack = new ItemStack(com.example.horsegenetics.neoforge.block.Jumps.item());
        com.example.horsegenetics.neoforge.block.JumpMaterials.DEFAULT.writeTo(stack);
        return stack;
    }

    private ModCreativeTabs() {
    }
}
