package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.BoundHorse;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SignBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * The two server-side halves of the stall-sign flow that don't live on the item:
 *
 * <ul>
 *   <li><b>binding</b> - right-click a horse with a blank <b>or</b> bound
 *       {@code stall_sign} / {@code bound_stall_sign} rewrites the held sign to a
 *       {@code bound_stall_sign} carrying that horse's {@link BoundHorse}. Placing
 *       is done by {@link com.example.horsegenetics.neoforge.item.StallSignItem};</li>
 *   <li><b>cleanup</b> - breaking a wall-sign block that a stall was registered
 *       against releases that stall from {@link StallData}.</li>
 * </ul>
 *
 * Mirrors {@link StallionSeedJarHandler}: cancel the entity interaction on both
 * sides so the client doesn't predict a mount, mutate server-side only.
 */
@EventBusSubscriber
public final class StallSignHandler {

    @SubscribeEvent
    static void onBindToHorse(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.STALL_SIGN.get()) && !stack.is(ModItems.BOUND_STALL_SIGN.get())) {
            return;
        }

        Player player = event.getEntity();
        if (!event.getLevel().isClientSide()) {
            String name = HorseRecords.hasRealRecord(horse)
                    ? HorseRecords.of(horse).displayName()
                    : "horse";
            ItemStack bound = new ItemStack(ModItems.BOUND_STALL_SIGN.get());
            bound.set(ModDataComponents.BOUND_HORSE.get(), new BoundHorse(horse.getUUID(), name));

            if (stack.getCount() <= 1) {
                player.setItemInHand(event.getHand(), bound);
            } else {
                stack.shrink(1);
                if (!player.addItem(bound)) {
                    player.drop(bound, false);
                }
            }
            player.sendSystemMessage(Component.literal("Stall sign bound to " + name + "."));
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    static void onSignBroken(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getState().getBlock() instanceof SignBlock)) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;

        if (StallData.get(server).removeBySign(level.dimension(), event.getPos()) && event.getPlayer() != null) {
            event.getPlayer().sendSystemMessage(Component.literal("Stall released - its sign was broken."));
        }
    }

    private StallSignHandler() {
    }
}
