package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * <b>The freedom stick</b> - a feather, a stick and one horse hair. Right-click
 * your own horse with it inside the {@link HorseRealm} and it is no longer
 * yours: healed, wild, and standing exactly where you left it for somebody else
 * to find.
 *
 * <h2>What "released" means, precisely</h2>
 * Everything that makes the horse <i>that horse</i> stays. Its name, its
 * genotype, its epigenome, its breed, its parents and its generation are all on
 * the {@code HorseRecord} attachment and in the ancestry database, and none of
 * them is derived from who owns it - so releasing touches none of them, and the
 * next person to tame it gets a horse with a history rather than a blank.
 * Exactly three things change:
 * <ul>
 *   <li><b>It is tamed no longer</b>, which is the flag every other system reads.
 *       {@code HorseOwnerTrackingHandler} mirrors the owner off the record within
 *       two seconds on its own; the vanilla owner reference is cleared here as
 *       well rather than left to go stale in the save file.</li>
 *   <li><b>Its temper is reset to zero.</b> Without this the release is a
 *       formality - vanilla's {@code RunAroundLikeCrazyGoal} rolls against
 *       temper, and a horse that was tame is at maximum, so the first player to
 *       sit on it would tame it instantly. Zero is what makes "tame it again the
 *       ordinary way" mean anything.</li>
 *   <li><b>It is at full health.</b> The treatment asks for it and the reason is
 *       structural: a natural cover needs a healthy mare, healing is gated on
 *       standing near water, and a hurt horse released into a field is one that
 *       may never breed and never recover. Releasing is the last thing its owner
 *       will ever do for it.</li>
 * </ul>
 * Its tack comes off and goes back to the player. A wild horse cannot use a
 * saddle slot at all ({@code AbstractHorse.canUseSlot} asks {@code isTamed}), so
 * leaving the saddle on would quietly destroy it.
 *
 * <h2>Only your own, and only here</h2>
 * The realm is shared. Anyone able to un-own anyone's horse in it would make it
 * the most dangerous place in the game to leave one, which is the exact opposite
 * of the point, so the stick refuses a horse you do not own. It also refuses
 * outside the realm entirely: a release anywhere else is a horse abandoned in
 * the Overworld, not a horse given away.
 *
 * <p><b>Nothing is spent on a refusal.</b> Every one of them says why, and the
 * durability comes off only when a horse actually changed hands - the house rule
 * the ticket and the vet's kit already follow.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class FreedomStickHandler {

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack stick = event.getItemStack();
        if (!stick.is(ModItems.FREEDOM_STICK.get())) {
            return;
        }
        // Cancelled on BOTH sides before anything else, because vanilla turns any
        // item used on a tamed horse into a mount - see HorseStasisHandler's note.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide() || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getEntity();
        if (release(level, horse, player) && !player.getAbilities().instabuild) {
            stick.hurtAndBreak(1, player,
                    event.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
    }

    /** True if the horse actually went wild - the only case that costs durability. */
    private static boolean release(ServerLevel level, Horse horse, Player player) {
        if (!HorseRealm.isRealm(level)) {
            refuse(player, "A horse can only be set free in the horse realm. Lead it through a "
                    + "hay-bale portal first.");
            return false;
        }
        if (!HorseRecords.hasRealRecord(horse)) {
            return false;       // not one of ours yet; its record arrives on the next tick
        }
        if (!horse.isTamed()) {
            refuse(player, HorseNotices.name(horse) + " is already wild.");
            return false;
        }
        EntityReference<LivingEntity> owner = horse.getOwnerReference();
        UUID ownerId = owner == null ? null : owner.getUUID();
        if (ownerId != null && !ownerId.equals(player.getUUID())) {
            refuse(player, HorseNotices.name(horse) + " is not yours to set free.");
            return false;
        }

        HorseRecord record = HorseRecords.of(horse);
        returnTack(horse, player);
        horse.ejectPassengers();
        if (horse.isLeashed()) {
            horse.dropLeash();
        }
        horse.setHealth(horse.getMaxHealth());
        horse.setTamed(false);
        horse.setOwner(null);
        horse.setTemper(0);
        HorseRecords.setOwner(horse, null);
        // It stays loaded whatever happens to the mob cap: an Animal does not
        // despawn in vanilla, but "the horse you left here is still here" is the
        // promise this whole dimension makes, and it should not rest on that.
        horse.setPersistenceRequired();
        // Let it join a band. HerdManager only considers horses carrying the wild
        // -spawn mark, so without this a released horse would stand alone in a
        // field of herds - and "horses can form herds" is part of the brief.
        horse.getPersistentData().putBoolean(BreedSpawnHandler.WILD_SPAWN_KEY, true);

        level.playSound(null, horse.blockPosition(), SoundEvents.HORSE_BREATHE, SoundSource.NEUTRAL, 0.8F, 1.1F);
        player.sendSystemMessage(Component.literal(record.displayName()
                        + " is free. Whoever tames it next keeps its name and its pedigree.")
                .withStyle(ChatFormatting.GREEN));
        ActionTrace.log("realm", ActionTrace.describeShort(horse) + " released by " + player.getGameProfile().name());
        return true;
    }

    /**
     * Take the saddle and body armour off and hand them back. Dropped at the
     * player's feet if their inventory is full, which is the ordinary way this
     * mod returns something a player is owed.
     */
    private static void returnTack(Horse horse, Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.SADDLE, EquipmentSlot.BODY}) {
            ItemStack worn = horse.getItemBySlot(slot);
            if (worn.isEmpty()) {
                continue;
            }
            horse.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(worn.copy())) {
                player.drop(worn.copy(), false);
            }
        }
    }

    private static void refuse(Player player, String why) {
        player.sendSystemMessage(Component.literal(why).withStyle(ChatFormatting.YELLOW));
    }

    private FreedomStickHandler() {
    }
}
