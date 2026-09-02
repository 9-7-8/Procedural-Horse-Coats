package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Client-only: right-clicking with the {@link ModItems#CUSTOM_HORSE_SPAWN_EGG}
 * opens the {@link CustomHorseSpawnScreen} editor instead of doing anything in
 * the world. The interaction is cancelled so vanilla doesn't also try to use
 * the (plain) item, and so the server never spawns anything on its own - the
 * screen sends {@code SpawnCustomHorsePayload} when you hit Spawn.
 *
 * <p>Both {@code RightClickBlock} (aiming at the ground - the usual spawn-egg
 * gesture) and {@code RightClickItem} (aiming at air) are handled.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class CustomHorseSpawnEggClient {

    private CustomHorseSpawnEggClient() {
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (openEditor(event.getLevel(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (openEditor(event.getLevel(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean openEditor(Level level, ItemStack stack) {
        if (!level.isClientSide() || !stack.is(ModItems.CUSTOM_HORSE_SPAWN_EGG.get())) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new CustomHorseSpawnScreen());
        }
        return true;
    }
}
