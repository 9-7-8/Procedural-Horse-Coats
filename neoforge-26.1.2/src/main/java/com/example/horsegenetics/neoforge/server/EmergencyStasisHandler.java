package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.StasisRescue;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.item.EmergencyStasisChamberItem;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * <b>The Emergency Horse Stasis Chamber, watching.</b> A player carrying one has
 * every horse they own insured: the instant one of them is about to fall below
 * {@code behaviour.emergency_stasis_fraction} of its health, the horse is in the
 * bottle instead. Nothing is clicked, nothing is aimed, and it works with the
 * owner in another dimension - they are only told after the fact.
 *
 * <p>The capture <b>is</b> the save. A horse in a chamber is not an entity at
 * all and cannot be hurt by anything, so this is a hard stop rather than a
 * delay, which is why it is worth eight ender pearls. Letting the horse out
 * hands the chamber straight back, empty and armed again.
 *
 * <h2>Where the hook had to go, against the plan</h2>
 * {@code wiki/horse-stasis.html}'s roadmap expected to hang this off "whatever
 * handler ends up watching for the buck-off-below-20% escape behaviour". There
 * is no such handler: {@link HorseEscapeGoal} is an AI goal that reads the
 * health bar on its own {@code canUse} tick, and a goal cannot see the blow
 * coming. So this is modelled on {@link HorseLastStandHandler} instead, which is
 * the same question asked at the same moment - <i>is this hit about to take the
 * horse somewhere it should not go</i> - and gets the answer from the same
 * event.
 *
 * <h2>{@code LivingDamageEvent.Pre}, at {@code LOW}</h2>
 * {@code Pre} for {@link HorseLastStandHandler}'s reason: it is the first point
 * at which {@code getNewDamage} is the number that will actually come off the
 * health, armour and enchantments already taken out of it, so a horse in barding
 * is not judged on a blow it was never going to take in full.
 *
 * <p><b>{@code LOW} so that the last stand goes first</b>, and that ordering is
 * the whole of how the two features compose. A killing blow is caught by the
 * last stand, which zeroes the damage and leaves the horse on a sliver; this
 * then runs, sees a horse on a sliver, and takes it out of the fight. The player
 * gets the totem particles <i>and</i> the rescue, in that order, which is
 * exactly the story. Without the priority the two would race, and in the losing
 * order the chamber would swallow a horse the last stand was about to save
 * anyway - spending the insurance on a blow that was never going to land.
 *
 * <h2>Cheap, because the only horses it can see are being hit</h2>
 * The roadmap's worry was that watching every owned horse's health is the kind
 * of server-wide per-tick scan the whole stasis feature exists to avoid. It is
 * not one: health only moves when something moves it, so the question is asked
 * on a damage event and nowhere else. Past the two field reads that answer
 * "is this a horse, and is it low", the cost is one walk of one player's
 * inventory - and that only for a horse that is already about to die.
 *
 * <p><b>Not play-tested.</b> Written against 26.1.2 sources.
 */
@EventBusSubscriber
public final class EmergencyStasisHandler {

    private EmergencyStasisHandler() {
    }

    /**
     * Horse UUID to the game tick its owner was last told a full chamber could
     * not save it. A refusal is per horse rather than per player: two horses in
     * the same fire are two different pieces of news.
     */
    private static final Map<UUID, Long> LAST_REFUSED = new HashMap<>();

    /** Above this many remembered horses, the long-quiet ones go on the next refusal. */
    private static final int SWEEP_ABOVE = 512;

    /** A horse nobody has refused for five minutes is not coming back to spam anyone. */
    private static final long FORGET_AFTER = 6_000L;

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onBlow(LivingDamageEvent.Pre event) {
        // Cheapest question first: this fires for every hit anything in the
        // world takes, and almost none of them are a horse. A chamber holds a
        // Horse and nothing else - capture and release are both written against
        // EntityType.HORSE - so a donkey or a mule is not insurable here, the
        // same answer HorseStasisHandler gives to a right-click.
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        float after = horse.getHealth() - event.getNewDamage();
        if (!StasisRescue.rescues(after, horse.getMaxHealth(), ServerConfig.emergencyStasisFraction())) {
            return;
        }
        UUID ownerId = HorseOwnership.ownerId(horse);
        if (ownerId == null) {
            return;         // wild, or untamed - nothing owns it to insure it
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            // Logged out. The chamber is in their inventory, wherever that is
            // saved, and an offline player's inventory is not somewhere this is
            // going to go rummaging - the insurance is on being here, and that
            // is honest: a chamber cannot save a horse nobody is carrying it for.
            return;
        }
        rescue(level, horse, owner, event);
    }

    /**
     * Find an armed chamber on the owner and put the horse in it. Split from the
     * event handler so the walk of the inventory - the only part with any cost
     * in it - is plainly the last thing that happens.
     */
    private static void rescue(ServerLevel level, Horse horse, ServerPlayer owner,
                               LivingDamageEvent.Pre event) {
        ItemStack armed = null;
        boolean sawFull = false;
        Inventory inventory = owner.getInventory();
        // Indices past the 36 carried slots are the equipment ones, offhand
        // included, which is where a player is most likely to keep this - so the
        // whole Container view rather than the hotbar.
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof EmergencyStasisChamberItem)) {
                continue;
            }
            if (StasisChamberItem.snapshotOf(stack) == null) {
                armed = stack;
                break;      // the first empty one wins; carrying two is carrying two lives
            }
            sawFull = true;
        }

        if (armed == null) {
            if (sawFull) {
                refuse(level, horse, owner);
            }
            return;         // no chamber at all is not news - most players have none
        }

        String name = HorseRecords.of(horse).displayName();
        // A rider would be discarded along with the horse, which is why an
        // ordinary capture refuses one outright. Refusing here would make the
        // item useless in the exact situation it is bought for, so the rider is
        // set down instead and the horse vanishes from under them. By this point
        // HorseEscapeGoal has usually thrown them off already - it bolts at twice
        // this health - so it is the uncommon path, not the normal one.
        horse.ejectPassengers();

        HorseStasisHandler.swallow(level, horse, armed, name);
        // Nothing is left to subtract it from, but the blow is refused properly
        // rather than landing on a discarded entity. Ordered after the capture so
        // that a horse which somehow could not be stored still takes its damage
        // instead of being handed free immunity.
        event.setNewDamage(0.0F);

        LAST_REFUSED.remove(horse.getUUID());
        owner.sendSystemMessage(Component.literal(StasisRescue.saved(name))
                .withStyle(ChatFormatting.AQUA));
        ActionTrace.log("stasis", ActionTrace.describeShort(horse)
                + " was caught by an emergency chamber at " + horse.getHealth() + "/"
                + horse.getMaxHealth() + " health, from " + event.getSource().getMsgId());
    }

    /**
     * <b>One clear line naming the horse it could not save.</b> Owner's call,
     * 2026-09-24: a full emergency chamber refuses a second horse rather than
     * queueing it or swapping the healthier one out, and says so - because a
     * player who believes they are insured and is not would otherwise find out
     * by finding a corpse.
     */
    private static void refuse(ServerLevel level, Horse horse, ServerPlayer owner) {
        long now = level.getGameTime();
        Long last = LAST_REFUSED.get(horse.getUUID());
        if (!StasisRescue.dueAgain(last == null ? StasisRescue.NEVER : last, now)) {
            return;
        }
        remember(horse.getUUID(), now);
        owner.sendSystemMessage(Component.literal(
                StasisRescue.refused(HorseRecords.of(horse).displayName()))
                .withStyle(ChatFormatting.RED));
    }

    /** A dead horse never needs its quiet period again. */
    @SubscribeEvent
    static void onHorseDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse) {
            LAST_REFUSED.remove(horse.getUUID());
        }
    }

    private static void remember(UUID horse, long now) {
        if (LAST_REFUSED.size() > SWEEP_ABOVE) {
            LAST_REFUSED.entrySet().removeIf(e -> now - e.getValue() > FORGET_AFTER);
        }
        LAST_REFUSED.put(horse, now);
    }
}
