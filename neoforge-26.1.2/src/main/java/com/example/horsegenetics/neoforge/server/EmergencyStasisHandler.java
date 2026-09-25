package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.StasisRescue;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import com.example.horsegenetics.neoforge.data.StasisBankIndex;
import com.example.horsegenetics.neoforge.item.EmergencyStasisChamberItem;
import com.example.horsegenetics.neoforge.item.StasisChamberItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

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
 * <p><b>{@code LOWEST} so that everything else goes first</b>, and that
 * ordering is the whole of how these features compose. A killing blow is caught by the
 * last stand, which zeroes the damage and leaves the horse on a sliver; this
 * then runs, sees a horse on a sliver, and takes it out of the fight. The player
 * gets the totem particles <i>and</i> the rescue, in that order, which is
 * exactly the story. Without the priority the two would race, and in the losing
 * order the chamber would swallow a horse the last stand was about to save
 * anyway - spending the insurance on a blow that was never going to land.
 *
 * <p>{@link RescuingBraidHandler} sits between the two at {@code LOW}, and this
 * dropped from {@code LOW} to {@code LOWEST} to let it: a braid is worn on one
 * horse on purpose and a chamber is the general net, so the specific instrument
 * spends itself first. Nothing is wasted by that - a horse a braid has sent home
 * is out of the fight and takes no further blows, so the chamber it did not
 * spend is still in the player's pocket.
 *
 * <h2>Cheap, because the only horses it can see are being hit</h2>
 * The roadmap's worry was that watching every owned horse's health is the kind
 * of server-wide per-tick scan the whole stasis feature exists to avoid. It is
 * not one: health only moves when something moves it, so the question is asked
 * on a damage event and nowhere else. Past the two field reads that answer
 * "is this a horse, and is it low", the cost is one walk of one player's
 * inventory - and that only for a horse that is already about to die.
 *
 * <h2>The bank is the second place it looks</h2>
 * An empty emergency chamber <b>filed in a stasis bank you placed</b> insures
 * your horses too, which is what makes a rack of spare empties in the cabinet
 * worth having rather than a chamber you have to remember to put in your pocket.
 * The inventory is still tried first (owner's call): the bottle on your belt is
 * the one you bought for this moment, and the bank is the backstop for the day
 * you forgot it.
 *
 * <p>Finding a block somewhere in some dimension could very easily have been the
 * world search this feature refuses to do. It is not, because
 * {@link StasisBankIndex} already knows where your banks are <i>and</i> whether
 * each was last seen holding an empty emergency chamber. The usual answer -
 * none of them can help - is a walk of a short list in memory with no chunk
 * touched. A chunk is loaded only for a bank that says it is holding one, which
 * is a horse's life against one chunk load, and the search is throttled to the
 * same ten seconds per horse as the refusal so that an animal standing in a fire
 * cannot ask repeatedly.
 *
 * <h2>You are put down a tick before the horse goes</h2>
 * A rider would be discarded with the horse, so the ordinary capture refuses one
 * outright and this ejects instead. The eject and the capture are <b>a tick
 * apart</b> (owner's call): the player is set down on the ground first and the
 * horse goes into the bottle from under them on the next tick, rather than both
 * happening inside one event and the client having to work out where its feet
 * went. The horse is held harmless for that tick - see {@link #PENDING} - so the
 * grace it is given cannot be the thing that kills it.
 *
 * <p><b>Not play-tested.</b> Written against 26.1.2 sources.
 */
@EventBusSubscriber
public final class EmergencyStasisHandler {

    private EmergencyStasisHandler() {
    }

    /**
     * Horse UUID to the game tick nothing could be done for it - either its
     * owner was told a full chamber could not save it, or the banks were looked
     * in and came back empty. One clock for both, because they are the same
     * question asked of the same horse and the expensive half must not be able
     * to run while the cheap half is still quiet.
     *
     * <p>Per horse rather than per player: two horses in the same fire are two
     * different pieces of news.
     */
    private static final Map<UUID, Long> LAST_REFUSED = new HashMap<>();

    /** Above this many remembered horses, the long-quiet ones go on the next refusal. */
    private static final int SWEEP_ABOVE = 512;

    /** A horse nobody has refused for five minutes is not coming back to spam anyone. */
    private static final long FORGET_AFTER = 6_000L;

    /**
     * How many of a player's armed banks one rescue will open a chunk for before
     * giving up. A player with three banks never reaches it; a player with two
     * hundred does not get to turn a wolf bite into two hundred chunk loads.
     */
    private static final int MAX_BANKS_CONSULTED = 8;

    @SubscribeEvent(priority = EventPriority.LOWEST)
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
        if (CAUGHT.contains(horse.getUUID())) {
            // Already in the bottle in all but fact - it is standing riderless
            // through its one grace tick. Nothing may take it in that window, or
            // the courtesy of setting the player down first would occasionally
            // be the thing that killed the horse.
            event.setNewDamage(0.0F);
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
     * Find an armed chamber - carried first, then filed - and put the horse in
     * it. Split from the event handler so the two searches, which are the only
     * parts with any cost in them, are plainly the last thing that happens.
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

        HorseStasisBankBlockEntity bank = null;
        boolean searched = false;
        if (armed == null && StasisRescue.dueAgain(lastRefused(horse), level.getGameTime())) {
            // The throttle covers the search and not only the sentence. A horse
            // standing in a fire asks this every half-second, and the bank half
            // is the only part of the whole feature that can touch a chunk.
            searched = true;
            Filed filed = inBanks(level.getServer(), owner);
            if (filed != null) {
                armed = filed.chamber();
                bank = filed.bank();
            }
        }

        if (armed == null) {
            if (sawFull) {
                refuse(level, horse, owner);        // which starts the quiet period
            } else if (searched) {
                // Nothing to say - most players have no chamber at all, and a
                // line about it every half-second would be the worst of both. But
                // the clock still has to start, or a horse burning to death with
                // a stale index row against it would re-open the same chunk on
                // every blow it takes.
                remember(horse.getUUID(), level.getGameTime());
            }
            return;
        }

        String name = HorseRecords.of(horse).displayName();
        if (horse.isVehicle()) {
            // A rider would be discarded along with the horse, which is why an
            // ordinary capture refuses one outright. Refusing here would make the
            // item useless in the exact situation it is bought for, so the rider
            // is set down instead - and set down a whole tick first, so the
            // player lands on the ground rather than inside the same event that
            // deletes what they were sitting on. By this point HorseEscapeGoal
            // has usually thrown them off already - it bolts at twice this
            // health - so it is the uncommon path, not the normal one.
            horse.ejectPassengers();
            // Refused up front here, unlike the immediate path: the bottle does
            // not close until next tick, and a horse left standing for a tick
            // holding the blow that triggered all this would simply die of it.
            event.setNewDamage(0.0F);
            CAUGHT.add(horse.getUUID());
            PENDING.add(new Pending(level.getServer().getTickCount() + 1, level, horse,
                    armed, bank, owner, name, bank != null,
                    event.getSource().getMsgId()));
            return;
        }

        close(level, horse, armed, bank, owner, name, bank != null, event.getSource().getMsgId());
        // Nothing is left to subtract it from, but the blow is refused properly
        // rather than landing on a discarded entity. Ordered after the capture so
        // that a horse which somehow could not be stored still takes its damage
        // instead of being handed free immunity.
        event.setNewDamage(0.0F);
    }

    // ------------------------------------------------------------------
    // The bank half
    // ------------------------------------------------------------------

    /** An empty emergency chamber, and the bank it is filed in. */
    private record Filed(HorseStasisBankBlockEntity bank, ItemStack chamber) {
    }

    /**
     * <b>Is there an empty emergency chamber in one of this player's banks?</b>
     *
     * <p>Only banks {@link StasisBankIndex} says were last seen holding one are
     * opened at all, so the walk is over a list that is usually empty and the
     * disk is not touched. Reaching a bank that <i>is</i> on that list loads its
     * chunk, which is the one genuinely expensive thing in this class and is
     * spent on a horse that is otherwise about to die.
     *
     * <p>The flag is re-checked against the real container rather than trusted:
     * a stale row costs a wasted chunk load and finds nothing, which is the right
     * way round for a cache that is deciding whether a horse lives.
     */
    private static @Nullable Filed inBanks(MinecraftServer server, ServerPlayer owner) {
        StasisBankIndex index = StasisBankIndex.get(server);
        List<StasisBankIndex.Bank> candidates = index.armedBanksOf(owner.getUUID());
        int opened = 0;
        for (StasisBankIndex.Bank record : candidates) {
            if (opened >= MAX_BANKS_CONSULTED) {
                break;
            }
            ServerLevel level = server.getLevel(record.dimension());
            if (level == null) {
                index.forget(record.dimension(), record.pos());
                continue;   // a dimension that is no longer loaded by the server at all
            }
            opened++;
            // Loads the chunk. Unverified in a running game: this runs from
            // inside an entity's damage, which is an ordinary tick and not the
            // chunk system's own update pass - see api-notes.html on
            // EntityLeaveLevelEvent for the one that is not.
            if (!(level.getBlockEntity(record.pos()) instanceof HorseStasisBankBlockEntity bank)) {
                index.forget(record.dimension(), record.pos());
                continue;   // broken while nobody was looking, or never there
            }
            if (!owner.getUUID().equals(bank.placedBy())) {
                continue;   // the index disagreed with the block; the block wins
            }
            ItemStack chamber = armedIn(bank);
            if (chamber != null) {
                return new Filed(bank, chamber);
            }
        }
        return null;
    }

    /** The first empty emergency chamber in a bank's grid, or {@code null}. */
    private static @Nullable ItemStack armedIn(HorseStasisBankBlockEntity bank) {
        var chambers = bank.chambers();
        for (int slot = 0; slot < chambers.getContainerSize(); slot++) {
            ItemStack stack = chambers.getItem(slot);
            if (stack.getItem() instanceof EmergencyStasisChamberItem
                    && StasisChamberItem.snapshotOf(stack) == null) {
                return stack;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // The tick between the eject and the capture
    // ------------------------------------------------------------------

    /**
     * A capture that has been promised and not yet made. One tick long: the
     * rider has been put down and the horse goes in on the next server tick.
     */
    private record Pending(int dueTick, ServerLevel level, Horse horse, ItemStack chamber,
                           @Nullable HorseStasisBankBlockEntity bank, ServerPlayer owner,
                           String name, boolean fromBank, String cause) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    /**
     * The horses in {@link #PENDING}, as a set, because {@code onBlow} asks the
     * question on every hit anything in the world takes and a list walk there
     * would be the one expensive thing in the cheap path.
     */
    private static final Set<UUID> CAUGHT = new HashSet<>();

    /**
     * Close the bottles promised last tick. {@code Post} rather than {@code Pre}
     * for no deep reason - it is one tick later either way, and this is where
     * {@link HorseLog} already does its once-a-second work.
     */
    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        int now = event.getServer().getTickCount();
        Iterator<Pending> pending = PENDING.iterator();
        while (pending.hasNext()) {
            Pending p = pending.next();
            if (now < p.dueTick()) {
                continue;
            }
            pending.remove();
            CAUGHT.remove(p.horse().getUUID());
            complete(p);
        }
    }

    /**
     * The promise, kept - unless the world moved underneath it. Both refusals
     * leave the chamber empty and the player insured, which is the only safe way
     * round: the alternative is a bottle that has spent itself on nothing.
     */
    private static void complete(Pending p) {
        if (p.horse().isRemoved()) {
            ActionTrace.log("stasis", p.name()
                    + " was gone before its emergency chamber could close - nothing was spent");
            return;
        }
        if (StasisChamberItem.snapshotOf(p.chamber()) != null) {
            // Somebody filled it by hand in the one tick it was reserved for.
            ActionTrace.log("stasis", "the emergency chamber promised to " + p.name()
                    + " was filled in the meantime - it takes its chances");
            return;
        }
        close(p.level(), p.horse(), p.chamber(), p.bank(), p.owner(), p.name(),
                p.fromBank(), p.cause());
    }

    // ------------------------------------------------------------------

    /**
     * <b>The horse goes in, and the owner is told twice.</b> Shared by the
     * immediate capture and the deferred one so there is exactly one place that
     * decides what a rescue does.
     *
     * <p>Chat <i>and</i> the browser's Log tab, deliberately. Chat is the alarm
     * and it is the only thing that reaches a player mid-fight; the log row is
     * the record, and it is what answers "what happened to my mare" an hour
     * later, when the chat line has scrolled past and the horse is a bottle in a
     * cabinet with no story attached to it. The same argument {@link HorseLog}
     * makes for covers and deaths applies harder here, because a chamber fires
     * with nobody watching by design.
     */
    private static void close(ServerLevel level, Horse horse, ItemStack chamber,
                              @Nullable HorseStasisBankBlockEntity bank, ServerPlayer owner,
                              String name, boolean fromBank, String cause) {
        float at = horse.getHealth();
        float max = horse.getMaxHealth();
        UUID horseId = horse.getUUID();
        HorseStasisHandler.swallow(level, horse, chamber, name);
        if (bank != null) {
            // The stack was filled in place inside the bank's grid, which the
            // container has no way of noticing: this is what re-derives the tick
            // gate and republishes the bank as no longer armed.
            bank.chambers().setChanged();
        }

        LAST_REFUSED.remove(horseId);
        owner.sendSystemMessage(Component.literal(
                        fromBank ? StasisRescue.savedInBank(name) : StasisRescue.saved(name))
                .withStyle(ChatFormatting.AQUA));
        HorseLog.rescued(level, owner.getUUID(), horseId, name, fromBank);
        ActionTrace.log("stasis", name + " was caught by an emergency chamber "
                + (fromBank ? "in a stasis bank" : "in its owner's inventory")
                + " at " + at + "/" + max + " health, from " + cause);
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
        if (!StasisRescue.dueAgain(lastRefused(horse), now)) {
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

    /**
     * When this horse's owner was last told nothing could be done for it - the
     * clock the refusal <i>and</i> the bank search both run on.
     */
    private static long lastRefused(Horse horse) {
        Long last = LAST_REFUSED.get(horse.getUUID());
        return last == null ? StasisRescue.NEVER : last;
    }

    private static void remember(UUID horse, long now) {
        if (LAST_REFUSED.size() > SWEEP_ABOVE) {
            LAST_REFUSED.entrySet().removeIf(e -> now - e.getValue() > FORGET_AFTER);
        }
        LAST_REFUSED.put(horse, now);
    }
}
