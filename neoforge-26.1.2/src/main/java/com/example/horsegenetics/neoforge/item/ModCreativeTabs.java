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
                    .displayItems((params, output) ->
                            ModItems.TAB_ITEMS.forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }
}
