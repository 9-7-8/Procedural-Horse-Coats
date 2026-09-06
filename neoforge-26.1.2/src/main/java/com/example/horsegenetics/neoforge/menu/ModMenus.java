package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * This mod's container menus. Only one: the <b>Horse Browser</b>
 * ({@link HorseBrowserMenu}), opened with the browser key. It is a real
 * server-synced menu (rather than the client-only screen it started as) so its
 * Crafting tab can have a live 3x3 grid and the player's inventory.
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, HorseGenetics.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<HorseBrowserMenu>> HORSE_BROWSER =
            MENUS.register("horse_browser",
                    () -> new MenuType<>(HorseBrowserMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private ModMenus() {
    }
}
