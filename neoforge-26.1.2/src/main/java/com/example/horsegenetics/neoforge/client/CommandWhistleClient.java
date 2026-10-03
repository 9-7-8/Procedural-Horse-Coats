package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.care.HorseOrders;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

import java.util.UUID;

/**
 * <b>Holding use with the command whistle opens the wheel</b> instead of doing what
 * right-clicking a horse normally does.
 *
 * <p>The hook is {@code InputEvent.InteractionKeyMappingTriggered}, cancelled. It fires
 * in {@code Minecraft.startUseItem} BEFORE {@code MultiPlayerGameMode.interact} sends the
 * interact packet (26.1.2 sources), so cancelling it means the server never hears the
 * click: the horse is not mounted and its inventory does not open, with no server-side
 * cancel to keep in step. Cancelling the entity-interact events instead would not do:
 * they fire after the packet has gone (see {@code HorseInfoInteraction}).
 *
 * <p>It fires once per hand, main hand first, and the whistle may be in either: every
 * hand's event is cancelled once the wheel is decided, or an empty main hand would mount
 * the horse before the off hand's whistle was asked.
 *
 * <p>The aimed horse is found out to {@link HorseOrders#REACH_BLOCKS} blocks, not the
 * normal three of an interaction, and never through a wall. A horse that is not the
 * player's own opens nothing, and the click goes on as normal.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class CommandWhistleClient {

    private CommandWhistleClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !holdsWhistle(player)) {
            return;
        }
        if (player.isSecondaryUseActive()) {
            claim(event);
            CommandWheelScreen.openForAll();
            return;
        }
        Horse horse = aimedOwnHorse(mc, player);
        if (horse == null) {
            return;
        }
        claim(event);
        HorseRecord record = ClientHorseRecordCache.get(horse.getId());
        String name = record != null ? record.displayName()
                : horse.hasCustomName() ? horse.getCustomName().getString() : "Your horse";
        CommandWheelScreen.openFor(horse, name);
    }

    private static void claim(InputEvent.InteractionKeyMappingTriggered event) {
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static boolean holdsWhistle(LocalPlayer player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.COMMAND_WHISTLE.get())
                || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.COMMAND_WHISTLE.get());
    }

    /** The horse of the player's own under the crosshair, out to the whistle's reach. */
    private static Horse aimedOwnHorse(Minecraft mc, LocalPlayer player) {
        double reach = HorseOrders.REACH_BLOCKS;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));
        // Never through a wall: stop the ray at the first block it meets.
        HitResult block = player.pick(reach, 1.0F, false);
        double max = block.getType() == HitResult.Type.MISS ? reach * reach : block.getLocation().distanceToSqr(eye);
        AABB box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof Horse && e.isAlive() && !e.isSpectator(), max);
        if (hit == null || !(hit.getEntity() instanceof Horse horse)) {
            return null;
        }
        return ownedBy(horse, player.getUUID()) ? horse : null;
    }

    private static boolean ownedBy(Entity entity, UUID player) {
        if (!(entity instanceof Horse horse) || !horse.isTamed()) {
            return false;
        }
        var owner = horse.getOwnerReference();
        return owner != null && player.equals(owner.getUUID());
    }
}
