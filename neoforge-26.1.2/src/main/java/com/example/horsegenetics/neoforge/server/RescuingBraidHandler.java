package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.LastStand;
import com.example.horsegenetics.common.care.RescuingBraid;
import com.example.horsegenetics.common.horse.StasisRescue;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.PenRecord;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.data.StallRecord;
import com.example.horsegenetics.neoforge.entity.HorseTackSlot;
import com.example.horsegenetics.neoforge.item.RescuingBraidItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jspecify.annotations.Nullable;

/**
 * <b>The rescuing braid, breaking.</b> A horse wearing one in its mane or tail
 * does not die where it stood: the braid spends itself, and the horse - with
 * whoever is aboard - is standing in its own stall instead, bolting.
 *
 * <h2>Three subscribers on one event, in a deliberate order</h2>
 * This is the third thing in the mod hanging off {@code LivingDamageEvent.Pre}
 * to keep a horse alive, and the priorities are the composition rather than
 * tidiness:
 *
 * <ol>
 *   <li>{@link HorseLastStandHandler} at {@code NORMAL} - refuses the blow and
 *       holds the horse at {@link LastStand#healthLeft}, with the totem
 *       particles.</li>
 *   <li><b>This, at {@code LOW}</b> - sees a horse standing on that floor and
 *       takes it home.</li>
 *   <li>{@link EmergencyStasisHandler} at {@code LOWEST} - the general net,
 *       which only ever sees a horse the first two could not help.</li>
 * </ol>
 *
 * <p>The braid goes before the chamber (owner's call) because it is the more
 * specific instrument: it is worn on <i>this</i> horse, deliberately, where a
 * chamber covers every horse its carrier owns. Nothing is wasted by the
 * ordering, either - a horse that has gone home is out of the fight and takes no
 * further blows, so the chamber it did not spend is still in the player's pocket
 * for the next one.
 *
 * <h2>Why it tests a state rather than a killing blow</h2>
 * By the time this runs the last stand has usually already set the damage to
 * zero, so "did this blow kill the horse" answers <i>no</i> on exactly the
 * horses a braid is for. {@link RescuingBraid#fires} therefore asks where the
 * horse <i>is</i> - see its own note, and {@code StasisRescue}'s, which reached
 * the same conclusion from the other end of the same event.
 *
 * <h2>The destination is resolved when it breaks, not when it is made</h2>
 * Owner's call, and it is the reason this item has no data component at all. A
 * braid carries nothing: when it fires it asks {@link StallData} for the horse's
 * own stall and falls back to its owner's holding pen - the same two
 * destinations, resolved by the same code, that {@code TicketHandler} and
 * {@code StallRecall} already send a horse to. A braid is therefore never bound
 * to a stall that has since been pulled down, never wrong about a horse that has
 * moved house, and a spare in a chest works for whichever horse you put it on.
 *
 * <p>The price is that a braid <b>cannot say where it will send a horse</b>
 * before it fires, and that a horse with no stall and no pen is wearing one for
 * nothing. So the refusal is said out loud, once, and <b>the braid is not
 * spent</b> - it is still in the hair when the player hangs a sign up.
 *
 * <p><b>Not play-tested.</b> Written against 26.1.2 sources.
 */
@EventBusSubscriber
public final class RescuingBraidHandler {

    private RescuingBraidHandler() {
    }

    /**
     * Horse UUID to the tick its owner was last told a braid had nowhere to send
     * it. The same shape, and the same reason, as
     * {@code EmergencyStasisHandler}'s: a horse standing in a fire is asked this
     * every half-second, and one line about a missing stall is news where forty
     * is a wall of text.
     */
    private static final Map<UUID, Long> LAST_TOLD = new HashMap<>();

    /** The two slots a braid can be worn in, in the order they are spent. */
    private static final HorseTackSlot[] SLOTS = { HorseTackSlot.MANE, HorseTackSlot.TAIL };

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onBlow(LivingDamageEvent.Pre event) {
        // Cheapest question first: this fires for every hit anything in the
        // world takes. A stall is keyed to a Horse - every stall sign, ticket
        // and holding pen in the mod narrows to one - so a donkey or a mule
        // cannot wear a braid to any purpose even though the last stand above
        // covers it.
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        float floor = LastStand.healthLeft(ServerConfig.lastStandHealth(), horse.getMaxHealth());
        if (!RescuingBraid.fires(event.getNewDamage(), horse.getHealth(), floor)) {
            return;
        }
        HorseTackSlot worn = wornBraid(horse);
        if (worn == null) {
            return;         // the common case, and it costs two attachment reads
        }
        UUID ownerId = HorseOwnership.ownerId(horse);
        if (ownerId == null) {
            return;         // a braid on a wild horse has no stall to look up
        }
        home(level, horse, ownerId, worn, event);
    }

    /**
     * The first slot holding a braid, mane before tail, or {@code null}.
     *
     * <p>Two slots are <b>two independent saves</b> (owner's call): each braid
     * breaks once and is gone, so a horse wearing both is saved twice. The
     * alternative - refusing the second slot while one is armed - leaves a slot
     * that exists and cannot be used for the thing it is for.
     */
    private static @Nullable HorseTackSlot wornBraid(Horse horse) {
        for (HorseTackSlot slot : SLOTS) {
            if (slot.on(horse).getItem() instanceof RescuingBraidItem) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Work out where home is, put the horse there, and spend the braid. Split
     * from the event handler so that everything with a cost in it - two saved-data
     * lookups and a live re-scan of the stall - is plainly behind the cheap
     * refusals.
     */
    private static void home(ServerLevel level, Horse horse, UUID ownerId,
                             HorseTackSlot slot, LivingDamageEvent.Pre event) {
        MinecraftServer server = level.getServer();
        String name = HorseRecords.of(horse).displayName();
        Destination destination = resolve(server, horse, ownerId);
        if (destination == null) {
            nowhereToSend(level, horse, ownerId, name);
            return;         // and the braid is still in its hair
        }

        // Refused before the move, not after: unlike a stasis capture, the
        // horse goes on existing, so a blow left un-refused would land on it
        // the moment it arrived and kill it in its own stall.
        event.setNewDamage(0.0F);

        ServerPlayer rider = horse.getFirstPassenger() instanceof ServerPlayer p ? p : null;
        UUID horseId = horse.getUUID();
        // Every particle, sound and untied lead the tickets already draw. The
        // braid is a ticket the horse is wearing, so it had better look like
        // one - and reusing the call means a change to how a horse travels
        // reaches this without anyone remembering it exists.
        TicketHandler.arrive(level, destination.level(), horse, destination.landing(), rider);
        reseat(destination.level(), horseId, rider, destination.landing());

        // Spent. Taken off the horse rather than dropped: it broke.
        slot.set(horse, ItemStack.EMPTY);
        LAST_TOLD.remove(horseId);

        // Nothing forces the escape behaviour and nothing should: HorseEscapeGoal
        // reads the health bar and whether anything has hurt the horse lately on
        // its own canUse tick, and both are still true on the other side of the
        // teleport - Escape.THREAT_LINGERS_TICKS outlasts the trip by a wide
        // margin. So the horse comes up bolting by the goal's own door, which is
        // the one that has been played. The exception is a horse the last stand
        // did not hold, which arrives on whatever health it had left and may be
        // too well to run; it is home either way.
        say(server, ownerId, RescuingBraid.saved(name, destination.what()), ChatFormatting.AQUA);
        HorseLog.homed(level, ownerId, horseId, name, destination.what());
        ActionTrace.log("braid", name + "'s " + slot.label().toLowerCase(java.util.Locale.ROOT)
                + " braid broke and sent it to " + destination.what() + " in "
                + destination.level().dimension().identifier().getPath()
                + ", from " + event.getSource().getMsgId());
    }

    /** Where a braid is sending this horse, and the world it is in. */
    private record Destination(ServerLevel level, Vec3 landing, String what) {
    }

    /**
     * <b>The horse's own stall, or its owner's holding pen.</b> Stall first, in
     * that order, which is {@code StallRecall}'s rule verbatim - the browser's
     * Send home button makes exactly this choice and a braid making a different
     * one would be a second answer to one question.
     *
     * <p>{@link TicketHandler#landingSpot} is what says yes: it re-reads the
     * sign, re-runs the stall detection live and finds a spot the horse fits in.
     * A stall that has been pulled down, built over or filled with hay bales
     * therefore refuses here rather than dropping a horse inside a wall.
     */
    private static @Nullable Destination resolve(MinecraftServer server, Horse horse, UUID ownerId) {
        StallData stalls = StallData.get(server);
        StallRecord stall = stalls.forHorse(horse.getUUID());
        if (stall != null) {
            Destination found = at(server, stall.dimension(), stall.signPos(), horse,
                    RescuingBraid.ITS_STALL);
            if (found != null) {
                return found;
            }
        }
        PenRecord pen = stalls.penOf(ownerId);
        if (pen != null) {
            return at(server, pen.dimension(), pen.signPos(), horse, RescuingBraid.THE_HOLDING_PEN);
        }
        return null;
    }

    private static @Nullable Destination at(MinecraftServer server, ResourceKey<Level> dimension,
                                            BlockPos signPos, Horse horse, String what) {
        ServerLevel target = server.getLevel(dimension);
        if (target == null) {
            return null;
        }
        Vec3 landing = TicketHandler.landingSpot(target, signPos, horse);
        return landing == null ? null : new Destination(target, landing, what);
    }

    /**
     * <b>Keep the rider aboard.</b>
     *
     * <p>Within one world this is a no-op: {@code teleportTo} carries an
     * entity's passengers, which is the property {@code GeneReactionHandler}'s
     * blink already relies on. <b>Across worlds it is not</b> - an entity moved
     * between levels is re-created in the target and its passengers are put
     * down, and the rider is left standing in the world the horse has just left.
     * That is the one case a braid must not have: being separated from your
     * horse by the thing that was supposed to save you both is worse than the
     * blow.
     *
     * <p>So the horse is looked up again by id in the world it landed in - it
     * may be a different entity object - and the rider, if there is one and it
     * is no longer aboard, is sent after it and put back on.
     *
     * <p><b>Unverified.</b> Nothing in this repository has watched a passenger
     * survive a cross-level teleport; {@code EnderWhistleCalls} says the same
     * about its own version of this.
     */
    private static void reseat(ServerLevel target, UUID horseId, @Nullable ServerPlayer rider,
                               Vec3 landing) {
        if (rider == null || rider.isRemoved() || rider.isPassenger()) {
            return;
        }
        Entity moved = target.getEntity(horseId);
        if (!(moved instanceof Horse arrived)) {
            return;
        }
        if (rider.level() != target) {
            rider.teleportTo(target, landing.x, landing.y, landing.z,
                    java.util.Set.of(), rider.getYRot(), rider.getXRot(), false);
        }
        // startRiding(Entity), not the forcing overload - that one is protected.
        // A player remounting the horse they were already on passes the ordinary
        // checks, and a refusal here leaves them standing beside it rather than
        // throwing.
        rider.startRiding(arrived);
    }

    /**
     * <b>A braid with nowhere to send a horse says so, once.</b> The braid is
     * not spent, and the horse falls through to whatever else was going to
     * happen to it - the last stand's window if it had one, the emergency
     * chamber at {@code LOWEST} if its owner is carrying one.
     */
    private static void nowhereToSend(ServerLevel level, Horse horse, UUID ownerId, String name) {
        long now = level.getGameTime();
        Long last = LAST_TOLD.get(horse.getUUID());
        // StasisRescue's clock, not a second one beside it. "How long to leave
        // an owner alone between two identical pieces of bad news about the same
        // horse" is one question with one answer, and a braid with nowhere to go
        // repeats on exactly the same half-second beat a full chamber does.
        if (!StasisRescue.dueAgain(last == null ? StasisRescue.NEVER : last, now)) {
            return;
        }
        remember(horse.getUUID(), now);
        say(level.getServer(), ownerId, RescuingBraid.nowhereToSend(name), ChatFormatting.RED);
    }

    /**
     * To the owner, if they are here. A braid is not the log's business when
     * nothing happened - an unfired one is a refusal, and a refusal is chat.
     */
    private static void say(MinecraftServer server, UUID ownerId, String line, ChatFormatting colour) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal(line).withStyle(colour));
        }
    }

    /** A dead horse never needs its quiet period again. */
    @SubscribeEvent
    static void onHorseDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse) {
            LAST_TOLD.remove(horse.getUUID());
        }
    }

    /** Above this many remembered horses, the long-quiet ones go on the next refusal. */
    private static final int SWEEP_ABOVE = 512;

    /** A horse nobody has refused for five minutes is not coming back to spam anyone. */
    private static final long FORGET_AFTER = 6_000L;

    private static void remember(UUID horse, long now) {
        if (LAST_TOLD.size() > SWEEP_ABOVE) {
            LAST_TOLD.entrySet().removeIf(e -> now - e.getValue() > FORGET_AFTER);
        }
        LAST_TOLD.put(horse, now);
    }
}
