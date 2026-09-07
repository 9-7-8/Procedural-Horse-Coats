package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.TransferDeed;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.PaperBearer;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.SignedTransferPaperItem;
import com.example.horsegenetics.neoforge.item.TransferPaperItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * <b>Transfer papers</b>: how a horse changes hands without changing who bred
 * it.
 *
 * <h2>The three moments</h2>
 * <ol>
 *   <li><b>Crafted.</b> Eight paper around a horse hair makes one blank, and
 *       the bench stamps the crafter onto it. That is the only moment the game
 *       knows whose paper it is, which is why the binding happens on
 *       {@link PlayerEvent.ItemCraftedEvent} and not in the recipe.</li>
 *   <li><b>Signed.</b> The bound player right-clicks a horse they currently own
 *       and the blank becomes a signed paper naming that animal. "Currently
 *       own", not "originally tamed" - a horse you have already signed away is
 *       not yours to sign away twice.</li>
 *   <li><b>Redeemed.</b> Whoever is holding the signed paper right-clicks the
 *       horse it names, and the horse becomes theirs. Ownership is the
 *       <i>only</i> thing that moves.</li>
 * </ol>
 *
 * <h2>Why redemption is a walk and not a click</h2>
 * Buying a paper from the cowboy does not teleport a horse into your stable;
 * you have to go and find the one it names. That is what makes the paper worth
 * something as an object - it is a claim the bearer has not collected yet, so
 * it can be handed to somebody else, or sold on, or lost in a creeper hole.
 *
 * <h2>What never moves</h2>
 * {@code bredBy} is untouched by every path here. A horse bred by
 * {@code Jesse Calloway} is still bred by Jesse Calloway after four owners, and
 * {@link HorseRecord#attribution()} - which prefers the breeder over the tamer
 * - keeps saying so in the family tree. {@code tamedBy} is only ever filled in
 * if it was blank, because it records who first got a rope on the animal, and
 * that also does not change when the animal is sold.
 *
 * <h2>The cowboy's horses are not free</h2>
 * A branded horse ({@code ModAttachments.COWBOY_BRAND}) refuses every ordinary
 * interaction, because vanilla lets a player tame any untamed horse by climbing
 * on it until it stops bucking - and the cowboy's whole herd standing there
 * untamed would otherwise be a free stable rather than a shop.
 */
@EventBusSubscriber
public final class TransferPaperHandler {

    private TransferPaperHandler() {
    }

    // ------------------------------------------------------------------
    // crafted: bind the blank to whoever made it
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (!crafted.is(ModItems.BLANK_TRANSFER_PAPER.get())) {
            return;
        }
        Player player = event.getEntity();
        crafted.set(ModDataComponents.PAPER_BEARER.get(),
                new PaperBearer(player.getUUID(), player.getGameProfile().name()));
    }

    // ------------------------------------------------------------------
    // signed / redeemed / refused
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack held = event.getItemStack();
        Player player = event.getEntity();
        boolean client = event.getLevel().isClientSide();

        if (held.is(ModItems.SIGNED_TRANSFER_PAPER.get())) {
            if (!client) {
                redeem(player, horse, held);
            }
            consume(event);
            return;
        }

        if (held.is(ModItems.BLANK_TRANSFER_PAPER.get())) {
            if (!client) {
                sign(player, horse, held);
            }
            consume(event);
            return;
        }

        // Anything else on one of the cowboy's horses: hands off. Cancelled on
        // both sides so the client does not predict a mount that the server is
        // about to refuse.
        if (!client && horse.level() instanceof ServerLevel level) {
            Cowboy owner = CowboyHandler.ownerOf(horse, level);
            if (owner != null) {
                say(player, Component.translatable(
                        "message.horsegenetics.transfer.not_yours",
                        Component.literal(owner.cowboyName())));
            }
        }
        if (isBranded(horse)) {
            consume(event);
        }
    }

    /** Cheap client-side-safe test: is there a brand at all? */
    private static boolean isBranded(Horse horse) {
        var brand = horse.getData(
                com.example.horsegenetics.neoforge.data.ModAttachments.COWBOY_BRAND.get());
        return brand != null && brand.isBranded();
    }

    // ------------------------------------------------------------------

    private static void sign(Player player, Horse horse, ItemStack blank) {
        PaperBearer bearer = TransferPaperItem.bearerOf(blank);
        if (bearer == null || !bearer.id().equals(player.getUUID())) {
            say(player, "message.horsegenetics.transfer.not_your_paper");
            return;
        }
        if (!HorseRecords.hasRealRecord(horse)) {
            say(player, "message.horsegenetics.transfer.unknown_horse");
            return;
        }
        if (!ownedBy(horse, player.getUUID())) {
            say(player, "message.horsegenetics.transfer.sign_needs_ownership");
            return;
        }

        HorseRecord record = HorseRecords.of(horse);
        ItemStack signed = new ItemStack(ModItems.SIGNED_TRANSFER_PAPER.get());
        signed.set(ModDataComponents.HORSE_DEED.get(),
                TransferDeed.forHorse(record, player.getGameProfile().name()));

        blank.shrink(1);
        if (!player.getInventory().add(signed)) {
            player.drop(signed, false);
        }
        say(player, Component.translatable(
                "message.horsegenetics.transfer.signed",
                Component.literal(record.displayName())));
    }

    private static void redeem(Player player, Horse horse, ItemStack paper) {
        TransferDeed deed = SignedTransferPaperItem.deedOf(paper);
        if (deed == null) {
            say(player, "message.horsegenetics.transfer.unwritten");
            return;
        }
        if (!HorseRecords.hasRealRecord(horse)) {
            say(player, "message.horsegenetics.transfer.unknown_horse");
            return;
        }
        HorseRecord record = HorseRecords.of(horse);
        if (!deed.names(record)) {
            say(player, Component.translatable(
                    "message.horsegenetics.transfer.wrong_horse",
                    Component.literal(deed.horseName())));
            return;
        }
        if (ownedBy(horse, player.getUUID())) {
            say(player, "message.horsegenetics.transfer.already_yours");
            return;
        }

        // The transfer itself. Note what is *not* here: nothing writes bredBy,
        // and the horse's genome, name, pedigree and generation are untouched.
        horse.setOwner(player);
        horse.setTamed(true);
        CowboyHandler.clearBrand(horse); // it has an owner now; it is not his stock any more

        // "Tamed by" is who first got a rope on it, not who owns it now. Only
        // fill it in when the horse has never been tamed at all - which is
        // exactly the cowboy's stock, and nothing else.
        if (record.tamedBy().isEmpty()) {
            HorseRecords.setTamedBy(horse, player.getGameProfile().name());
        }

        if (!player.getAbilities().instabuild) {
            paper.shrink(1);
        }
        say(player, Component.translatable(
                "message.horsegenetics.transfer.redeemed",
                Component.literal(record.displayName())));
    }

    /** Is {@code playerId} this horse's current owner? */
    private static boolean ownedBy(Horse horse, UUID playerId) {
        if (!horse.isTamed()) {
            return false;
        }
        EntityReference<LivingEntity> owner = horse.getOwnerReference();
        return owner != null && playerId.equals(owner.getUUID());
    }

    /** Everything here answers on the action bar, and only ever server-side. */
    private static void say(Player player, String key) {
        say(player, Component.translatable(key));
    }

    private static void say(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        }
    }

    private static void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
