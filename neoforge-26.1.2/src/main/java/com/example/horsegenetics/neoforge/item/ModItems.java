package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry. One item so far: the <b>custom horse spawn egg</b> - a
 * dev/testing tool that spawns a horse whose age, sex and genome you pick in a
 * screen first, instead of the random genotype a vanilla spawn egg (or a wild
 * spawn) rolls.
 *
 * <p>It is a plain {@link Item}; nothing about it is a real
 * {@code SpawnEggItem}. The right-click that opens the editor is caught
 * client-side in {@code client/CustomHorseSpawnEggClient}, and the actual
 * spawn happens on the server from
 * {@code network/SpawnCustomHorsePayload}. Its model reuses the vanilla horse
 * spawn-egg texture, so the icon is identical.
 */
@EventBusSubscriber(modid = HorseGenetics.MOD_ID)
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(HorseGenetics.MOD_ID);

    public static final DeferredItem<Item> CUSTOM_HORSE_SPAWN_EGG =
            ITEMS.registerItem("custom_horse_spawn_egg", Item::new);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    /** Show it in the Spawn Eggs creative tab, next to the real one. */
    @SubscribeEvent
    static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(CUSTOM_HORSE_SPAWN_EGG.get());
        }
    }

    private ModItems() {
    }
}
