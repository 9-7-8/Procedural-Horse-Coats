package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * This mod's container menus - one per thing you stand in front of and put
 * items into.
 *
 * <p>The Horse Browser used to be here too and deliberately is not any more -
 * it has no slots, so it is a plain screen with no server menu behind it. The
 * rule is <b>slots, not blocks</b>: a screen gets a menu when it has slots a
 * carried stack goes in, and not otherwise. That is why the horse information
 * screen is still a plain screen and its Gear tab's <i>Dress</i> window is
 * {@link #HORSE_GEAR} - the only menu here that is for an entity rather than a
 * block.
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
     * The <b>Horse Stasis Bank's</b> - a grid of slots that takes stasis chambers
     * and nothing else. Shelf-shaped: backed by the block entity's container, so
     * the menu keeps nothing itself. One tab today; the Browse tab is stage three
     * on {@code wiki/horse-stasis.html}.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<HorseStasisBankMenu>> HORSE_STASIS_BANK =
            MENUS.register("horse_stasis_bank",
                    () -> new MenuType<>(HorseStasisBankMenu::new, FeatureFlags.VANILLA_SET));

    /**
     * <b>A jump's</b> - two slots that take a plank or a dye, and three style
     * buttons. Backed by the block entity that holds the woods and the paint,
     * and by the blockstate that holds the style, so it keeps nothing itself.
     *
     * <p><b>The first menu here built by NeoForge's extra-data factory</b>
     * ({@link #HORSE_GEAR} is the other).
     * Vanilla's {@code MenuType} constructor hands a client menu an id and an
     * inventory and nothing else, and this one needs the block's <i>position</i>
     * - it reads everything it draws off the block rather than syncing a copy.
     * {@code IMenuTypeExtension.create} is the supported way to say so, and
     * {@code ServerPlayer.openMenu(provider, buf -> ...)} is what fills it in.
     *
     * @see JumpMenu for why it reads the block instead of carrying a ContainerData
     */
    public static final DeferredHolder<MenuType<?>, MenuType<JumpMenu>> JUMP =
            MENUS.register("jump",
                    () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension
                            .create(JumpMenu::new));

    /**
     * <b>Dressing a horse</b> - the nineteen gear slots and the player's
     * inventory. Built by the extra-data factory for the same reason as
     * {@link #JUMP}, with an entity id where the jump has a position.
     *
     * @see HorseGearMenu for why the information screen is not itself a menu
     */
    public static final DeferredHolder<MenuType<?>, MenuType<HorseGearMenu>> HORSE_GEAR =
            MENUS.register("horse_gear",
                    () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension
                            .create(HorseGearMenu::new));

    /**
     * <b>A chest on a horse</b>, when it has a number of slots vanilla has no
     * chest screen for. Extra-data, like the two above: the client is told how
     * many slots to draw.
     *
     * @see HorsePackMenu for why most chests on horses never reach it
     */
    public static final DeferredHolder<MenuType<?>, MenuType<HorsePackMenu>> HORSE_PACK =
            MENUS.register("horse_pack",
                    () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension
                            .create(HorsePackMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private ModMenus() {
    }
}
