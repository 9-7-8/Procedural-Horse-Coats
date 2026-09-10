package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * This mod's container menus. Only one: the <b>Equine Research Shelf</b>.
 *
 * <p>The Horse Browser used to be here too and deliberately is not any more -
 * it has no slots, so it is a plain screen with no server menu behind it. A menu
 * is for a <i>block</i> you stand in front of, which is exactly what this is.
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, HorseGenetics.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchShelfMenu>> RESEARCH_SHELF =
            MENUS.register("research_shelf",
                    () -> new MenuType<>(ResearchShelfMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private ModMenus() {
    }
}
