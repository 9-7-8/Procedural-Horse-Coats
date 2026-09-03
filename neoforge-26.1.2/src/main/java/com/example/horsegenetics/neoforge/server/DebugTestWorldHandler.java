package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Dev-only: when the "Spawn Test Horse World" title-screen button
 * ({@code client/DebugTitleScreenButton}) creates a world, it sets
 * {@link #pendingHotbarFill}; on the next player login we stock the player up
 * for exercising horse features.
 *
 * <ul>
 *   <li><b>Hotbar</b>: the custom horse spawn egg (slot 0), then the vanilla
 *       tools the portal / taming / aging / inspect / lead features use
 *       (hay block, golden carrot, stick, clock, paper, lead).</li>
 *   <li><b>Main inventory</b>: one of every gameplay-layer item
 *       ({@link ModItems#TAB_ITEMS} minus the spawn egg) - horse hair, the
 *       breeding + gene carrots, the placeholder gene book, the seed jars, the
 *       tickets and the whistles - so their icons, tooltips and (later)
 *       behaviour can be checked without crafting them.</li>
 * </ul>
 *
 * Inert in production (nothing sets the flag).
 */
@EventBusSubscriber
public final class DebugTestWorldHandler {

    public static volatile boolean pendingHotbarFill = false;

    private DebugTestWorldHandler() {
    }

    @SubscribeEvent
    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!pendingHotbarFill || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        pendingHotbarFill = false;
        Inventory inv = player.getInventory();

        // Hotbar: spawn egg first, then the feature-test tools.
        give(inv, 0, ModItems.CUSTOM_HORSE_SPAWN_EGG.get());
        give(inv, 1, Items.HAY_BLOCK);
        give(inv, 2, Items.GOLDEN_CARROT);
        give(inv, 3, Items.STICK);
        give(inv, 4, Items.CLOCK);
        give(inv, 5, Items.PAPER);
        give(inv, 6, Items.LEAD);

        // Main inventory: one of every new gameplay-layer item.
        int slot = 9;
        for (var item : ModItems.TAB_ITEMS) {
            if (item.get() == ModItems.CUSTOM_HORSE_SPAWN_EGG.get()) {
                continue;
            }
            give(inv, slot++, item.get());
        }
    }

    private static void give(Inventory inv, int slot, Item item) {
        inv.setItem(slot, new ItemStack(item));
    }
}
