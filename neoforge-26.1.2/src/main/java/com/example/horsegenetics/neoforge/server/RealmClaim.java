package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.PenRecord;
import com.example.horsegenetics.neoforge.data.StallData;
import com.example.horsegenetics.neoforge.item.HoldingPenTicketItem;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * <b>Claiming a horse out of the realm</b> - the <i>Bring home</i> button on the
 * browser's <i>Horse realm</i> tab (owner, 2026-09-25). One holding pen ticket
 * buys one wild realm horse: it is tamed to you and it arrives in your pen.
 *
 * <h2>Why taming is part of it, when nothing else in the mod tames for you</h2>
 * Taming is otherwise being thrown off a horse until it gives in, and that is
 * the right cost for a horse standing in front of you. It cannot be the cost
 * here, because the whole point of the realm is that it holds <b>more horses
 * than a player will ever ride</b> - a field of bands, bred by nobody, most of
 * which you will look at through this table and never meet. A tab that could
 * only list them would be a catalogue you cannot order from.
 *
 * <p>So the ticket is the price, and it is a real one: a holding pen ticket
 * costs a blank ticket and wheat, it is spent, and you must have hung a pen sign
 * first. That also makes this the exact inverse of the
 * {@link com.example.horsegenetics.neoforge.item.TurnoutTicketItem turnout
 * ticket} - one ticket out, one ticket back - which is the shape the pair should
 * have.
 *
 * <h2>It refuses rather than half-works</h2>
 * Every failure leaves the horse where it is and the ticket in the pocket, and
 * says which one it was. The one that matters is a horse that is <b>already
 * somebody's</b>: the realm turns loose horses feral
 * ({@link HorseRealmFeral}) but a player standing in their own realm keeps
 * theirs, so a tamed horse in the field belongs to whoever is there with it.
 */
public final class RealmClaim {

    private RealmClaim() {
    }

    public static void request(ServerPlayer player, UUID horseId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel realm = server.getLevel(HorseRealm.REALM_LEVEL);
        if (realm == null) {
            say(player, "The horse realm is not loaded on this server.");
            return;
        }
        if (!(realm.getEntity(horseId) instanceof Horse horse) || !horse.isAlive()) {
            // Not an error worth dressing up: the table is a snapshot, and a
            // horse can walk out through a portal between the draw and the click.
            say(player, "That horse is not in the realm any more.");
            return;
        }
        if (horse.isTamed()) {
            say(player, HorseRecords.of(horse).displayName() + " is already somebody's horse.");
            return;
        }
        if (horse.isVehicle()) {
            say(player, "Somebody is on that horse.");
            return;
        }

        PenRecord pen = StallData.get(server).penOf(player.getUUID());
        if (pen == null) {
            say(player, "You have no holding pen yet. Hang a holding pen sign on a pen's wall first.");
            return;
        }
        ServerLevel target = server.getLevel(pen.dimension());
        if (target == null) {
            say(player, "Your holding pen's world is not loaded.");
            return;
        }
        Vec3 landing = TicketHandler.landingSpot(target, pen.signPos(), horse);
        if (landing == null) {
            say(player, "There is no room to stand in your holding pen - check the sign is still up and "
                    + "that the pen has a floor and two blocks of headroom.");
            return;
        }
        int slot = ticketSlot(player);
        if (slot < 0 && !player.getAbilities().instabuild) {
            say(player, "That costs a holding pen ticket, and you have none. "
                    + "A blank ticket and wheat makes one.");
            return;
        }

        // Tame first, travel second. The other order would put a wild horse in
        // somebody's pen for a tick, which is a wild horse in a pen if anything
        // below were ever to fail - and nothing below can, which is why the
        // ticket is only spent after the horse has actually moved.
        horse.setTamed(true);
        horse.setOwner(player);
        // A temper of zero on a tamed horse is the freedom stick's trap read
        // backwards: taming is what this is, so give it the temper vanilla gives
        // a horse that has just accepted a rider.
        horse.setTemper(horse.getMaxTemper());
        HorseRecords.setOwner(horse, player.getUUID());
        TicketHandler.arrive(realm, target, horse, landing, player);

        if (slot >= 0 && !player.getAbilities().instabuild) {
            player.getInventory().getItem(slot).shrink(1);
        }
        String name = HorseRecords.of(horse).displayName();
        say(player, name + " is yours, and is in your holding pen.");
        ActionTrace.log("realm", player.getGameProfile().name() + " claimed "
                + ActionTrace.describeShort(horse) + " out of the realm for a holding pen ticket");
        RealmRoster.sendTo(player);
    }

    /** The first holding pen ticket in the player's inventory, or {@code -1}. */
    private static int ticketSlot(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof HoldingPenTicketItem
                    || stack.is(ModItems.HOLDING_PEN_TICKET.get())) {
                return slot;
            }
        }
        return -1;
    }

    private static void say(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }
}
