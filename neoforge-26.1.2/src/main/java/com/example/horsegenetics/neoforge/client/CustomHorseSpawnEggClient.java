package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
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
 * <h2>All four gestures, not two</h2>
 * A right-click reaches exactly one of four events depending on what the
 * crosshair is over, and this used to handle two of them:
 *
 * <ul>
 *   <li>{@code RightClickBlock} - aiming at the ground, the usual spawn-egg
 *       gesture;</li>
 *   <li>{@code RightClickItem} - aiming at nothing;</li>
 *   <li>{@code EntityInteractSpecific} then {@code EntityInteract} - <b>aiming
 *       at a mob</b>, which is what you do when the thing you want to look at
 *       is a horse standing in front of you. Neither was handled, so the egg
 *       did nothing there and vanilla got the click instead - on a tamed horse
 *       that means you <i>mount it</i>. That is a right-click that visibly does
 *       the wrong thing rather than nothing, and is the most likely shape of
 *       the report that this item "does not seem to be working" in a build.</li>
 * </ul>
 *
 * <p>Nothing here is gated on the environment - the editor opens in a
 * production build exactly as it does under {@code runClient}. The only gate
 * anywhere in this feature is creative mode on the <b>Spawn</b> button itself,
 * which the screen now says on the button and the server re-checks.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class CustomHorseSpawnEggClient {

    private CustomHorseSpawnEggClient() {
    }

    // Four near-identical handlers rather than one on the shared supertype:
    // PlayerInteractEvent itself is not cancellable in this SDK, only its
    // subclasses are, and each declares setCanceled / setCancellationResult on
    // its own. See wiki/api-notes.html.

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

    @SubscribeEvent
    static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (openEditor(event.getLevel(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
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
        if (mc.screen != null) {
            // Something else already owns the screen. Say so rather than
            // swallowing the click: a right-click that does nothing and leaves
            // no trace is unreportable.
            HorseGenetics.LOGGER.info("[Custom Horse] egg used while {} was open - not opening the editor",
                    mc.screen.getClass().getSimpleName());
            return true;
        }
        mc.setScreen(new CustomHorseSpawnScreen());
        return true;
    }
}
