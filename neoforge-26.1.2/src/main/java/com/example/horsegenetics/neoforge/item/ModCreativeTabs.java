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
 * <b>This mod's creative tabs, and the order they sit in.</b>
 *
 * <p>Three, reading left to right as the thing you build with, the thing you
 * populate it with, and the thing you hitch to it:
 *
 * <ol>
 *   <li><b>Horse Genetics</b> - the gameplay layer. Carrots, papers, tickets,
 *       whistles, tack, and the blocks a yard is made of.</li>
 *   <li><b>Horse Breeds</b> - one spawn egg per breed, and nothing else.</li>
 *   <li><b>Horse Carts</b> - registered over in {@code carts/HorseCarts},
 *       because that package is a self-contained subsystem and owns its own
 *       registrations.</li>
 * </ol>
 *
 * <p><b>The breeds have a tab because of how many there are.</b> They were in
 * the main tab, one filled egg per breed that has one, and that is a list which
 * grows every time somebody writes a breed file - a shelf of eggs that pushed
 * the carrots and papers the tab exists for off the bottom of it. A tab per
 * <i>kind of thing</i> survives the roster growing; a tab per subsystem does
 * not, which is why the jumps do not have one (see {@link #MAIN}).
 *
 * <p>The custom horse spawn egg additionally shows in the vanilla Spawn Eggs
 * tab (see {@link ModItems#addToCreativeTab}). The double gates are filed into
 * vanilla's Building Blocks beside the gate of their own wood, which is where
 * somebody reaching for a gate looks.
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
                        // THE BREED EGGS ARE NOT HERE ANY MORE - they have a tab
                        // of their own (BREEDS below), because there is one per
                        // breed and that is a list that grows every time somebody
                        // writes a breed file. They were burying the carrots and
                        // papers this tab exists for.
                        //
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
     * <b>One spawn egg per breed</b>, and nothing else in it.
     *
     * <p>Filled eggs only. The blank {@code breed_spawn_egg} item is not listed
     * anywhere: its whole content is the breed component, and an egg without
     * one does nothing - it is a puzzle rather than an item.
     *
     * <p>Sits directly after {@link #MAIN}, so the two horse tabs are
     * neighbours rather than being separated by whatever else is installed.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BREEDS =
            TABS.register("breeds", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.horsegenetics.breeds"))
                    // The first breed that has an egg, rather than a named one:
                    // an icon that cannot point at a breed somebody deleted.
                    .icon(ModCreativeTabs::firstBreedEgg)
                    .withTabsAfter(MAIN.getKey())
                    .displayItems((params, output) -> {
                        for (com.example.horsegenetics.common.breed.Breed breed
                                : com.example.horsegenetics.common.breed.Breeds.from(
                                        com.example.horsegenetics.common.breed.BreedSource.SPAWN_EGG)) {
                            output.accept(BreedSpawnEggItem.of(breed));
                        }
                    })
                    .build());

    /**
     * The breeds tab's icon.
     *
     * <p>Taken from the list rather than named, so it cannot point at a breed
     * that was deleted or renamed. A blank egg is the fallback for the case
     * that should never happen - no breed carrying one at all - because a tab
     * with no icon is worse than a tab with a dull one.
     */
    private static ItemStack firstBreedEgg() {
        for (com.example.horsegenetics.common.breed.Breed breed
                : com.example.horsegenetics.common.breed.Breeds.from(
                        com.example.horsegenetics.common.breed.BreedSource.SPAWN_EGG)) {
            return BreedSpawnEggItem.of(breed);
        }
        return new ItemStack(ModItems.BREED_SPAWN_EGG.get());
    }

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
