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

    /**
     * The <b>Tack Dyeing Bench's</b>. Loom-shaped - three slots and a result
     * computed on change - so unlike the shelf's it is backed by a
     * {@code ContainerLevelAccess} rather than a block entity.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<EquestrianBenchMenu>> EQUESTRIAN_BENCH =
            MENUS.register("equestrian_bench",
                    () -> new MenuType<>(EquestrianBenchMenu::new, FeatureFlags.VANILLA_SET));

    /**
     * <b>A jump's</b> - two plank slots and three style buttons. Backed by the
     * block entity that holds the two woods, and by the blockstate that holds
     * the style, so it keeps nothing itself; see {@link JumpMenu}.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<JumpMenu>> JUMP =
            MENUS.register("jump",
                    () -> new MenuType<>(JumpMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private ModMenus() {
    }
}
