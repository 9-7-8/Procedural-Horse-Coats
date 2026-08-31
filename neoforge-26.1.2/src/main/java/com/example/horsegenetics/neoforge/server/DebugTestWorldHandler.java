package com.example.horsegenetics.neoforge.server;

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
 * {@link #pendingHotbarFill}; on the next player login we fill the hotbar with
 * the tools for exercising horse features. Inert in production (nothing sets
 * the flag).
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
        give(inv, 0, Items.HAY_BLOCK);
        give(inv, 1, Items.GOLDEN_CARROT);
        give(inv, 2, Items.STICK);
        give(inv, 3, Items.CLOCK);
        give(inv, 4, Items.PAPER);
        give(inv, 5, Items.LEAD);
    }

    private static void give(Inventory inv, int hotbarSlot, Item item) {
        inv.setItem(hotbarSlot, new ItemStack(item));
    }
}
