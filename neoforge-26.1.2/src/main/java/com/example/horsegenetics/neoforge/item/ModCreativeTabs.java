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
                    })
                    .build());

    /**
     * <b>The jumps, in a tab of their own.</b>
     *
     * <p>They were filed into vanilla's Building Blocks for one build, beside
     * the fence of each wood, on the same argument the
     * {@link com.example.horsegenetics.neoforge.block.DoubleGates double gates}
     * are: somebody building a paddock is in that tab. The owner's call was
     * that the argument does not carry, because a gate is a thing anyone
     * builds with and <b>a jump is horse equipment</b> - it has no use to a
     * player without a horse, so it does not belong in a vanilla tab at all.
     *
     * <p><b>A tab rather than a shelf in {@link #MAIN}</b>, which was the other
     * option, because this is one style of jump and there are to be more -
     * logs, brush, ditches, oxers, fillers. Each style is another twelve woods,
     * so the roster grows by the dozen; put two styles in the horse tab and the
     * carrots, whistles and papers it exists for are off the bottom of it.
     *
     * <p>The gates stay in Building Blocks. That is not an inconsistency: a
     * fence gate is a building block that horses happen to care about, and a
     * jump is horse equipment shaped like a fence.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> JUMPS =
            TABS.register("jumps", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.horsegenetics.jumps"))
                    // An oak vertical, stamped like every other entry below.
                    .icon(ModCreativeTabs::oakJump)
                    .withTabsBefore(MAIN.getKey())
                    .displayItems((params, output) -> {
                        // THREE ENTRIES. Not three per wood, and not thirty-six.
                        //
                        // This tab listed one stamped stack per wood per style
                        // for about an hour, which is convenient and is also
                        // indistinguishable from the thirty-six items the whole
                        // rework existed to delete - the owner's first look at
                        // it was "all the jumps are separate items in their own
                        // tab", which was the migration succeeding and reading
                        // as though it had not happened. A tab is a statement
                        // about what a thing IS, and a jump is three items whose
                        // wood is something you choose afterwards.
                        //
                        // So: oak, because that is what a jump is when nothing
                        // says otherwise, and every other wood is a craft or two
                        // planks in the block's own screen.
                        for (var item : com.example.horsegenetics.neoforge.block.Jumps.items()) {
                            ItemStack stack = new ItemStack(item);
                            com.example.horsegenetics.neoforge.block.JumpMaterials.DEFAULT
                                    .writeTo(stack);
                            output.accept(stack);
                        }
                    })
                    .build());

    /**
     * The tab's icon: an oak vertical, stamped with its woods like every entry
     * in it.
     *
     * <p>Stamped rather than bare on purpose - an unstamped jump is a jump with
     * no components, which is a different stack from the oak one the tab hands
     * out, and an icon that does not match a single item in its own tab is
     * confusing for no gain.
     */
    private static ItemStack oakJump() {
        ItemStack stack = new ItemStack(com.example.horsegenetics.neoforge.block.Jumps.item(
                com.example.horsegenetics.neoforge.block.JumpBlock.Style.VERTICAL));
        com.example.horsegenetics.neoforge.block.JumpMaterials.DEFAULT.writeTo(stack);
        return stack;
    }

    private ModCreativeTabs() {
    }
}
