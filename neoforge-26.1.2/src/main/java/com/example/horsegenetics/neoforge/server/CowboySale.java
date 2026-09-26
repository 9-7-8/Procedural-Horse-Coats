package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.data.CowboyBrand;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/**
 * <b>The other half of the counter: a dealer buys a horse off you.</b> Lead one
 * up to a cowboy and right-click him, and he takes it into his string and pays
 * for it. (Owner, 2026-09-26.)
 *
 * <h2>Why a lead, and not a paper</h2>
 * The mod already has a proof of ownership you can hand over - sign a blank
 * transfer paper against your own horse and the signed one names it. Selling
 * <i>that</i> would have been less code. It is the wrong object here, because a
 * paper is a promise about a horse somewhere else: the cowboy would be paying
 * for an animal still standing in your paddock, and the only thing that could
 * make the sale real is him walking to it, which he cannot do. Paper is how a
 * horse changes hands between two <b>players</b>, who can both travel.
 *
 * <p>A lead settles all of it at once. The horse is here, it is unambiguously
 * the one you mean, and the transfer is the animal itself rather than a claim on
 * it. It is also the gesture this mod already uses for moving a horse somewhere
 * it cannot take itself - the roped-horse shortcut through a portal is the same
 * hand on the same rope.
 *
 * <h2>What he will not do</h2>
 * <ul>
 *   <li><b>Buy back his own.</b> A horse already in this dealer's herd - one he
 *       bred, or one you sold him earlier - is refused. Without this, his own
 *       stock is a loop: buy a horse off him for what the breed is worth, walk
 *       it round the barn, sell it back for what its loci are worth.</li>
 *   <li><b>Take more than he can hold.</b> {@link Cowboy#MAX_HERD} live,
 *       unsold horses and he is full, and says so. A dealer with an unbounded
 *       string is an unbounded emerald tap, and it is also a merchant screen
 *       nobody can read.</li>
 *   <li><b>Take a foal.</b> His string is horses he can sell on, and a paper
 *       naming an animal that cannot be ridden or bred from yet is a paper
 *       somebody is going to be annoyed about.</li>
 * </ul>
 *
 * <h2>What the horse becomes</h2>
 * Stock. Untamed, unowned, branded to this dealer and in his herd, which is
 * exactly the state {@code CowboyHandler.settle} leaves a horse he bred himself
 * in - so it appears in his offers on the next look, as a signed paper, priced
 * by {@link HorsePrices#emeraldsFor} like the rest of his string. It keeps its
 * name, genes, epigenome and pedigree, because nothing in this mod ever takes
 * those off a horse.
 *
 * <p>Deliberately <b>not</b> {@code HorseRelease.makeWild}: that is a horse
 * being let go, and it marks a wild spawn, drops the tack on the floor and -
 * since this morning - pays a realm release fee. A sale is not a release. The
 * tack comes off into the seller's hands here, which is the one thing the two
 * do share.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class CowboySale {

    /** How far a led horse may trail behind you and still be the one you are selling. */
    private static final double LEAD_RADIUS = 12.0;

    private CowboySale() {
    }

    /**
     * <b>What he would give you for the horse on your rope</b>, said out loud
     * and doing nothing else - then the trading screen opens as it always has.
     *
     * <h2>Why the sale needs two gestures</h2>
     * An ordinary right-click on a cowboy is how you shop, and it is a click
     * people make constantly. If that click also sold whatever happened to be
     * tied behind them, then walking to the barn leading a mare you are keeping,
     * and buying something while you are there, would part you from her for a
     * handful of emeralds with no way back - he has her, she is untamed, and the
     * paper that buys her back costs more than he paid. There is no undo for
     * that and no message that would be read in time.
     *
     * <p>So shopping is a click and selling is a <b>crouched</b> click, which is
     * the vanilla disambiguator for "I mean this one specifically", and this
     * line is what makes the second gesture discoverable: you find out the price
     * and how to take it in the same breath, while still only having shopped.
     */
    public static void quote(Cowboy cowboy, ServerLevel level, ServerPlayer player) {
        List<Horse> leading = led(level, player);
        if (leading.isEmpty()) {
            return;
        }
        Horse horse = leading.get(0);
        if (!HorseRecords.hasRealRecord(horse) || horse.isBaby()
                || cowboy.herdIds().contains(horse.getUUID())
                || !HorseOwnership.isOwner(horse, player.getUUID())) {
            return;     // the crouched click will say why; a shopping click is not the place
        }
        HorseRecord record = HorseRecords.of(horse);
        int price = HorsePrices.buyPriceFor(record);
        player.sendSystemMessage(Component.literal("He looks over " + record.displayName()
                        + ". \"" + price + (price == 1 ? " emerald" : " emeralds")
                        + " for her, if you're selling.\" (Crouch and click to sell.)")
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * Sell the horse on the rope. One per click even when several are leashed:
     * emeralds arriving for an animal you did not mean to part with is not a
     * thing to do in a batch, and the second click is cheap.
     *
     * <p>Only ever reached from a <b>crouched</b> click - see {@link #quote}.
     *
     * @return true when something was sold or refused out loud - i.e. when this
     *         click has been dealt with and the trading screen should not open
     */
    public static boolean sell(Cowboy cowboy, ServerLevel level, ServerPlayer player) {
        List<Horse> leading = led(level, player);
        if (leading.isEmpty()) {
            return false;
        }
        Horse horse = leading.get(0);

        if (!HorseRecords.hasRealRecord(horse)) {
            return refuse(player, "He looks it over and shakes his head - no papers on this one.");
        }
        if (horse.isBaby()) {
            return refuse(player, "\"Come back when it's grown. I can't sell a foal on.\"");
        }
        if (cowboy.herdIds().contains(horse.getUUID())) {
            return refuse(player, "\"That one's mine already. I'll not buy my own horse twice.\"");
        }
        if (!HorseOwnership.isOwner(horse, player.getUUID())) {
            return refuse(player, "\"That's not yours to sell.\"");
        }
        if (stringSize(cowboy, level) >= Cowboy.MAX_HERD) {
            return refuse(player, "\"My string's full. Sell some of what I've got first.\"");
        }

        HorseRecord record = HorseRecords.of(horse);
        int price = HorsePrices.buyPriceFor(record);
        takeIntoStock(cowboy, level, horse, player);
        pay(player, price);

        player.sendSystemMessage(Component.literal("\"" + record.displayName() + "\". Done - "
                        + price + (price == 1 ? " emerald" : " emeralds") + ".")
                .withStyle(ChatFormatting.GREEN));
        ActionTrace.log("cowboy", player.getGameProfile().name() + " sold "
                + ActionTrace.describeShort(horse) + " to " + cowboy.cowboyName()
                + " for " + price + " emeralds");
        return true;
    }

    /** Horses on a rope in this player's hand, nearest first. */
    private static List<Horse> led(ServerLevel level, Player player) {
        return level.getEntitiesOfClass(Horse.class,
                        player.getBoundingBox().inflate(LEAD_RADIUS),
                        h -> h.isAlive() && h.isLeashed() && h.getLeashHolder() == player)
                .stream()
                .sorted(java.util.Comparator.comparingDouble(h -> h.distanceToSqr(player)))
                .toList();
    }

    /**
     * How full his string is. Sold horses do not count against it - they are
     * spoken for and the buyer will walk them off - and neither does one that
     * has wandered out of the world, since a herd id whose horse no longer
     * exists would wall the dealer off for good.
     */
    private static int stringSize(Cowboy cowboy, ServerLevel level) {
        int n = 0;
        for (Horse horse : cowboy.liveHerd(level)) {
            if (!cowboy.hasSold(horse.getUUID())) {
                n++;
            }
        }
        return n;
    }

    /**
     * The transfer. Same end state as a horse he bred - see
     * {@code CowboyHandler.settle}, which this deliberately mirrors rather than
     * calls, because that one also puts the entity into the world and this horse
     * is already standing here.
     */
    private static void takeIntoStock(Cowboy cowboy, ServerLevel level, Horse horse,
                                      ServerPlayer seller) {
        returnTack(horse, seller);
        horse.ejectPassengers();
        if (horse.isLeashed()) {
            horse.dropLeash();
        }
        horse.setTamed(false);
        horse.setOwner(null);
        horse.setTemper(0);
        HorseRecords.setOwner(horse, null);
        horse.setPersistenceRequired();
        horse.setData(ModAttachments.COWBOY_BRAND.get(), CowboyBrand.of(cowboy.getUUID()));
        cowboy.addToHerd(horse.getUUID());
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        horse.setData(ModAttachments.HORSE_CARE.get(), care.withHerd(Optional.of(cowboy.getUUID())));
        cowboy.rebuildOffers(level);
        HorseLog.soldToDealer(horse, seller.getUUID(), cowboy.cowboyName());
    }

    /**
     * The saddle and barding come off and go back to the seller. You sold him a
     * horse, not your tack - and his own string is never saddled, so leaving it
     * on would also put gear into his shop window that is not for sale.
     */
    private static void returnTack(Horse horse, ServerPlayer seller) {
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
                net.minecraft.world.entity.EquipmentSlot.SADDLE,
                net.minecraft.world.entity.EquipmentSlot.BODY}) {
            ItemStack worn = horse.getItemBySlot(slot);
            if (worn.isEmpty()) {
                continue;
            }
            horse.setItemSlot(slot, ItemStack.EMPTY);
            giveOrDrop(seller, worn.copy());
        }
    }

    private static void pay(ServerPlayer player, int emeralds) {
        giveOrDrop(player, new ItemStack(Items.EMERALD, emeralds));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    private static boolean refuse(ServerPlayer player, String why) {
        player.sendSystemMessage(Component.literal(why).withStyle(ChatFormatting.YELLOW));
        return true;
    }
}
