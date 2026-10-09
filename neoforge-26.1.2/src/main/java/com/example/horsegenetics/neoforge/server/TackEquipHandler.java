package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/**
 * <b>Right-click your horse holding a piece of tack and it goes on.</b> No
 * screen, no Gear tab, no picker - the gesture you would expect.
 *
 * <h2>It covers the seventeen slots vanilla has never heard of, and only those</h2>
 * The plan this was built from assumed right-click-to-equip did not exist in
 * 26.1.2 and would have to be written for the saddle and the barding. Reading
 * the version says otherwise, twice over, and both readings are quoted here
 * because they are the whole reason this class is as small as it is.
 *
 * <p><b>The saddle: vanilla.</b> {@code Items.SADDLE} carries
 * {@code Equippable.saddle()}, which is built with {@code setEquipOnInteract(true)}
 * - so {@code ItemStack.interactLivingEntity} calls {@code Equippable.equipOnTarget}
 * and the saddle is on before {@code AbstractHorse.mobInteract} gets as far as
 * {@code doPlayerRide}. It even refuses to swap already:
 * {@code equipOnTarget} requires {@code !target.hasItemInSlot(this.slot)}.
 *
 * <p><b>The barding: also vanilla.</b> {@code AbstractHorse.mobInteract} ends
 * with {@code if (this.isEquippableInSlot(itemStack, EquipmentSlot.BODY) &&
 * !this.isWearingBodyArmor())}, and equips it. That predicate is
 * <i>character-for-character</i> what {@link HorseTackSlot#accepts} asks for the
 * {@code BARDING} slot, so there is provably nothing this mod could add there.
 *
 * <p>So this handler deliberately skips every {@link HorseTackSlot#isVanilla()}
 * slot. Taking them over would have meant two code paths racing to put the same
 * saddle on the same horse, and the loser would have been the one with the
 * sound and the guaranteed drop.
 *
 * <h2>What that leaves is the part with no owner at all</h2>
 * The other seventeen slots ride in the {@code HORSE_GEAR} attachment, which
 * vanilla cannot see, so before this there was exactly one way to put a braid in
 * a mane: open the screen, find the Gear tab, click the slot, pick the item.
 * Today that is a rescuing braid ({@code gear/mane}, {@code gear/tail}) and a
 * chest for either flank - anything {@code HorseStorage} finds an inventory
 * in. <b>Neither is a special case in the code.</b> It walks the roster and
 * asks each slot whether the item {@linkplain HorseTackSlot#fits fits}, so every
 * piece still to be made - pads, blankets, boots, shoes - arrives already
 * wearable by hand the moment it is added to its tag, with nothing here to
 * change. The one thing it knows about chests is which flank: the one the
 * player is standing at, because that is the one they can see.
 *
 * <h2>Which clicks it takes, and which it deliberately leaves</h2>
 * <ul>
 *   <li><b>Sneaking is never ours.</b> {@code HorseInfoInteraction} and
 *       {@code HorseInteractionHandler.onFoalInventory} both live on the sneak,
 *       and a gesture that means "read this horse" must not start depending on
 *       what is in your hand.</li>
 *   <li><b>An untamed horse falls through</b> rather than being refused in
 *       words. Vanilla's {@code Horse.mobInteract} rears it up at you
 *       ({@code makeMad}), which is a clearer answer about a wild horse than any
 *       sentence this could write.</li>
 *   <li><b>A horse that already wears one falls through too</b>, to vanilla,
 *       which means you get on it. That is the decided "never swap" rule read
 *       literally, and it is the right way round: you can see the braid already
 *       in the mane, so nothing is ambiguous, and claiming the click would
 *       leave you unable to mount your own horse while a braid was in your
 *       hand.</li>
 *   <li><b>Not your horse, and a foal, are refused in a line.</b> Both are
 *       rules this mod invented, and an invented rule that fails silently reads
 *       as a bug - the same reasoning the name tag's {@code not_yours} message
 *       was written under. Owner-only is
 *       {@link HorseOwnership#isOwner}, the same question the Gear tab's own
 *       {@code TackSlotPayload} asks before it believes a click.</li>
 * </ul>
 *
 * <p><b>{@code LOWEST}, like {@code onFoalInventory} and for its reason.</b>
 * Every other horse interaction in this mod - the name tag, the carrots, the
 * transfer paper, the whistles, the shears - cancels the event when it claims a
 * click, and a cancelled event is never delivered here. So this only ever sees a
 * click nothing else wanted, and an interaction added later needs no change
 * here. It is still above vanilla whatever its priority: the event fires from
 * {@code Player.interactOn} before {@code mobInteract} is called at all.
 */
@EventBusSubscriber
public final class TackEquipHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onEquipTack(PlayerInteractEvent.EntityInteract event) {
        if (!ServerConfig.rightClickEquipsTack()) {
            return;
        }
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        Player player = event.getEntity();
        if (player.isSecondaryUseActive()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!isModTack(horse, stack)) {
            return;     // not a piece of this mod's gear - the click is none of our business
        }
        if (!horse.isTamed()) {
            return;     // vanilla rears it up at you, which says it better
        }

        boolean client = event.getLevel().isClientSide();
        if (!HorseOwnership.isOwner(horse, player.getUUID())) {
            refuse(event, player, client, "message.horsegenetics.tack.not_yours", horse);
            return;
        }
        if (horse.isBaby()) {
            // HorseTackSlot.usableOn already says foals wear nothing; this only
            // turns that silence into a sentence.
            refuse(event, player, client, "message.horsegenetics.tack.too_young", horse);
            return;
        }

        if (!HorseTackSlot.harnessed(horse) && HorseTackSlot.SADDLEBAG_LEFT.fits(horse, stack)) {
            // A rule of this mod's, so it is said rather than left to look like
            // a click that did nothing: a chest needs a harness to hang from.
            refuse(event, player, client, "message.horsegenetics.pack.needs_harness", horse);
            return;
        }

        HorseTackSlot target = firstEmptySlotFor(horse, player, stack);
        if (target == null) {
            return;     // nothing free that takes it - fall through, and let them mount
        }

        if (!client) {
            target.set(horse, stack.copyWithCount(1));
            // Creative keeps its stack: the owner's rule for every other
            // consume in this mod. Not vanilla's - Equippable.equipOnTarget
            // splits one off whatever the game mode.
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
            horse.level().playSound(null, horse.blockPosition(),
                    SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.NEUTRAL, 0.8F, 1.0F);
        }
        // Cancelled on BOTH sides, the way HorseInteractionHandler.consume does:
        // uncancelled, the client goes on to predict vanilla's path - a mount -
        // and the horse flickers underneath you before the server says otherwise.
        consume(event);
    }

    /**
     * Is this a piece of gear <i>some</i> slot of ours takes? Asked of the item
     * alone, before the horse is considered at all, so that a click holding
     * anything else is left completely untouched - including the clicks that
     * refuse below, which must not fire for a carrot.
     */
    private static boolean isModTack(Horse horse, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (HorseTackSlot slot : HorseTackSlot.values()) {
            // fits, not the tag: the two storage slots take anything that
            // stores items, which no tag lists - see HorseStorage.
            if (slot.fits(horse, stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The first slot in roster order that both takes {@code stack} and is empty,
     * or {@code null} when every slot that would take it is full.
     *
     * <p>Roster order is the paper doll's reading order, which is what makes the
     * answer predictable for the slots that come in sets: a braid goes to the
     * mane before the tail, a boot to the near fore before the off fore.
     */
    private static @Nullable HorseTackSlot firstEmptySlotFor(Horse horse, Player player, ItemStack stack) {
        // A chest goes on the flank you are standing at, which is the one you
        // can see - and on the other only when that one is taken.
        HorseTackSlot near = HorsePackHandler.slotOn(com.example.horsegenetics.common.pack.PackBox.sideOf(
                player.getX() - horse.getX(), player.getZ() - horse.getZ(), horse.yBodyRot));
        if (near.accepts(horse, stack) && near.on(horse).isEmpty()) {
            return near;
        }
        for (HorseTackSlot slot : HorseTackSlot.values()) {
            if (!slot.isVanilla() && slot.accepts(horse, stack) && slot.on(horse).isEmpty()) {
                return slot;
            }
        }
        return null;
    }

    /**
     * <b>Everything the horse wears in this mod's own slots, handed back</b> -
     * to {@code player}'s inventory or their feet, or onto the ground under the
     * horse when there is nobody. For a horse that is leaving its owner without
     * dying: sold, or turned loose. The saddle and the barding are the caller's
     * (they are vanilla's slots), and the chests on its flanks are
     * {@code HorsePackHandler.giveBack}'s; this is the fifteen in between, which
     * until #218 simply left with the horse and were deleted with it when a
     * dealer's string or the wild turnover discarded it.
     */
    public static void returnGear(Horse horse, @Nullable Player player) {
        if (!(horse.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        for (HorseTackSlot slot : HorseTackSlot.values()) {
            if (slot.isVanilla() || slot.isStorage()) {
                continue;
            }
            ItemStack worn = slot.takeOff(horse);
            if (worn.isEmpty()) {
                continue;
            }
            if (player == null) {
                horse.spawnAtLocation(level, worn);
            } else if (!player.getInventory().add(worn)) {
                player.drop(worn, false);
            }
        }
    }

    private static void refuse(PlayerInteractEvent.EntityInteract event, Player player,
                               boolean client, String key, Horse horse) {
        // ServerPlayer, not Player: the two-argument overload that puts a line on
        // the action bar rather than in the chat log is only on the server one.
        // A refusal is transient and the chat log is where the owner reads
        // breeding and death notices, which this is not.
        if (!client && player instanceof net.minecraft.server.level.ServerPlayer told) {
            String name = HorseRecords.hasRealRecord(horse)
                    ? HorseRecords.of(horse).displayName()
                    : "That horse";
            told.sendSystemMessage(Component.translatable(key, name), true);
        }
        consume(event);
    }

    private static void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private TackEquipHandler() {
    }
}
