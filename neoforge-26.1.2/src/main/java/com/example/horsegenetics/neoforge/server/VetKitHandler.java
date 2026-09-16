package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>The vet's kit</b> (owner, 2026-09-13).
 *
 * <ul>
 *   <li><b>Sneak-use on your own stallion or colt</b> gelds him. Permanent; it is
 *       a flag on his {@link HorseRecord}, so it survives death and sale. Owner
 *       only, creative included, the same rule as binding a seed jar.</li>
 *   <li><b>Plain use on any horse</b> examines it - {@link ReproHandler#vetReport}:
 *       a mare's heat or pregnancy (twins included), a stallion's covers today.</li>
 * </ul>
 * Each procedure costs one durability. Cancelled on both sides, so the client
 * never predicts a mount.
 */
@EventBusSubscriber
public final class VetKitHandler {

    private VetKitHandler() {
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack kit = event.getItemStack();
        if (!kit.is(ModItems.VET_KIT.get())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide() || !HorseRecords.hasRealRecord(horse)) {
            return;
        }
        Player player = event.getEntity();
        boolean used = player.isSecondaryUseActive() ? geld(horse, player) : examine(horse, player);
        if (used && !player.getAbilities().instabuild) {
            kit.hurtAndBreak(1, player,
                    event.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
    }

    private static boolean geld(Horse horse, Player player) {
        HorseRecord record = HorseRecords.of(horse);
        String name = record.displayName();
        if (record.sex() != Sex.MALE) {
            tell(player, name + " is a mare - there is nothing to geld. Use the kit without sneaking "
                    + "to examine her.", ChatFormatting.YELLOW);
            return false;
        }
        if (record.gelded()) {
            tell(player, name + " is already a gelding.", ChatFormatting.YELLOW);
            return false;
        }
        if (!horse.isTamed()) {
            tell(player, name + " is not tamed - tame him first.", ChatFormatting.YELLOW);
            return false;
        }
        if (!HorseOwnership.isOwner(horse, player.getUUID())) {
            tell(player, name + " is not your horse - only his owner can have him gelded.", ChatFormatting.YELLOW);
            return false;
        }
        HorseRecords.apply(horse, record.withGelded(true));
        tell(player, name + " is now a gelding. He will not breed, and he will settle to you a little "
                + "faster.", ChatFormatting.GREEN);
        ActionTrace.log("vet", player.getName().getString() + " gelded " + ActionTrace.describeShort(horse));
        HorseProgress.complete(player, ProgressTask.GELD_HORSE);
        return true;
    }

    private static boolean examine(Horse horse, Player player) {
        for (String line : ReproHandler.vetReport(horse)) {
            tell(player, "[vet] " + line, ChatFormatting.GOLD);
        }
        HorseProgress.complete(player, ProgressTask.VET_KIT_USE);
        return true;
    }

    private static void tell(Player player, String text, ChatFormatting colour) {
        player.sendSystemMessage(Component.literal(text).withStyle(colour));
    }
}
