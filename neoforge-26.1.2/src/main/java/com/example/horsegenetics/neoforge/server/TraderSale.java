package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Optional;

/**
 * <b>A wandering trader will take a horse off your hands, and pay over the
 * odds for it.</b>
 *
 * <p>(Owner, 2026-09-26: "wandering traders buy horses at double the rate of a
 * cowboy.")
 *
 * <h2>Why he pays more</h2>
 * The cowboy is a fixture. He is at the barn, he will be there tomorrow, and
 * his string is capped at {@code Cowboy.MAX_HERD} - so his price is the price
 * and you can take it or come back. The trader is passing through and will not
 * come again, has no string to fill and no reputation to keep, and buying a
 * horse out of a field is exactly the sort of opportunism the wandering trader
 * is for. {@value #RATE} times is the premium for catching him.
 *
 * <p>The markup is on the man, not the animal, which is the split
 * {@link HorsePrices#emeraldsFor(HorseRecord, boolean)} already makes and §4 of
 * the philosophy requires: nothing in the model scores a horse.
 *
 * <h2>What happens to the horse</h2>
 * <b>It is tied to him and walks off</b> (owner's call), exactly as his trader
 * llamas already do - so a sale you can watch leave rather than an animal
 * blinking out of existence in front of you, and no new state on an entity that
 * despawns. When he goes, it goes with him. There is deliberately no herd, no
 * brand and no re-listing: he is not a dealer and you cannot buy it back, which
 * is the risk that makes the better price a decision rather than free money.
 *
 * <h2>Two gestures, as with the cowboy</h2>
 * An upright click is shopping and only quotes; a <b>crouched</b> click sells.
 * {@link CowboySale#quote} explains at length why that split has to exist, and
 * the reasoning is identical here - more so, if anything, since a trader's
 * ordinary click is one people make without looking.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class TraderSale {

    /** What he pays, as a multiple of the cowboy's price. */
    public static final int RATE = 2;

    private TraderSale() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof WanderingTrader trader)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            if (sell(trader, level, player)) {
                // Eat the click so his own trading screen does not also open on
                // top of the sale we have just made.
                event.setCanceled(true);
            }
            return;
        }
        quote(level, player);
    }

    /**
     * What he would give you for the horse on your rope, said out loud and
     * doing nothing else. Silent when there is nothing to quote on, so an
     * ordinary shopping click at a trader you are not leading a horse to is
     * unchanged.
     */
    private static void quote(ServerLevel level, ServerPlayer player) {
        List<Horse> leading = CowboySale.led(level, player);
        if (leading.isEmpty()) {
            return;
        }
        Horse horse = leading.get(0);
        if (!HorseRecords.hasRealRecord(horse) || horse.isBaby()
                || !HorseOwnership.isOwner(horse, player.getUUID())) {
            return;     // the crouched click will say why
        }
        HorseRecord record = HorseRecords.of(horse);
        int price = HorsePrices.buyPriceFor(record, RATE);
        player.sendSystemMessage(Component.literal("He eyes " + record.displayName()
                        + " and holds up " + price + (price == 1 ? " emerald" : " emeralds")
                        + ". (Crouch and click to sell - he will not pass this way again.)")
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * Sell the horse on the rope, one per click - the same rule as the cowboy's,
     * and for the same reason: emeralds arriving for an animal you did not mean
     * to part with is not a thing to do in a batch.
     *
     * @return true when something was sold or refused out loud
     */
    private static boolean sell(WanderingTrader trader, ServerLevel level, ServerPlayer player) {
        List<Horse> leading = CowboySale.led(level, player);
        if (leading.isEmpty()) {
            return false;
        }
        Horse horse = leading.get(0);

        if (!HorseRecords.hasRealRecord(horse)) {
            return CowboySale.refuse(player, "He shrugs - no papers on this one.");
        }
        if (horse.isBaby()) {
            return CowboySale.refuse(player, "He waves the foal away. \"Not that one.\"");
        }
        if (!HorseOwnership.isOwner(horse, player.getUUID())) {
            return CowboySale.refuse(player, "He will not take a horse that is not yours.");
        }

        HorseRecord record = HorseRecords.of(horse);
        int price = HorsePrices.buyPriceFor(record, RATE);
        leadAway(trader, horse, player);
        CowboySale.pay(player, price);

        player.sendSystemMessage(Component.literal("He counts out " + price
                        + (price == 1 ? " emerald" : " emeralds") + " and ties "
                        + record.displayName() + " behind him.")
                .withStyle(ChatFormatting.GREEN));
        ActionTrace.log("trader", player.getGameProfile().name() + " sold "
                + ActionTrace.describeShort(horse) + " to a wandering trader for "
                + price + " emeralds");
        return true;
    }

    /**
     * The transfer. Deliberately parallel to {@code CowboySale.takeIntoStock}
     * minus everything that is about a dealer's string: no brand, no herd, no
     * rebuilt offers. What is left is the part that is simply "this is not your
     * horse any more", plus the rope.
     */
    private static void leadAway(WanderingTrader trader, Horse horse, ServerPlayer seller) {
        CowboySale.returnTack(horse, seller);
        horse.ejectPassengers();
        if (horse.isLeashed()) {
            horse.dropLeash();
        }
        horse.setTamed(false);
        horse.setOwner(null);
        horse.setTemper(0);
        HorseRecords.setOwner(horse, null);
        // So it does not quietly despawn out from under him on the walk.
        horse.setPersistenceRequired();
        // It has left whatever herd it was in; it is on a rope behind a stranger.
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        horse.setData(ModAttachments.HORSE_CARE.get(), care.withHerd(Optional.empty()));
        horse.setLeashedTo(trader, true);
        HorseLog.soldToDealer(horse, seller.getUUID(), "a wandering trader");
    }

    /**
     * <b>When he goes, it goes with him</b> (#21, 2026-10-01). The class javadoc promised this from the start and
     * nothing did it: vanilla's {@code TraderLlama} removes itself by copying its trader's despawn delay while it is on
     * his rope, and a horse has no such logic - so on his despawn the rope simply dropped and the horse stayed,
     * persistent, unowned and untamed, for ever (the yard's KEEPING HORSES pen, the same day).
     *
     * <p>Only a {@code DISCARDED} trader, which is how {@code WanderingTrader.maybeDespawn} removes him. One who is
     * killed, or whose chunk unloads, leaves his horse where it is, as vanilla leaves his llamas. And only a horse
     * still on <i>his</i> rope at that moment: one a player has since tied to themselves, or is riding, is theirs.
     * The leash is still attached when this fires - the horse finds its holder gone only on its own next tick.
     */
    @SubscribeEvent
    public static void onTraderLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof WanderingTrader trader)
                || trader.getRemovalReason() != Entity.RemovalReason.DISCARDED
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (Horse horse : level.getEntitiesOfClass(Horse.class, trader.getBoundingBox().inflate(16.0),
                h -> h.isAlive() && h.isLeashed() && h.getLeashHolder() == trader && !h.isVehicle())) {
            ActionTrace.log("trader", ActionTrace.describeShort(horse) + " leaves with the wandering trader");
            HorseRecords.forgetDeparting(horse); // #200: its record goes with it, if nothing needs it
            horse.discard();
        }
    }
}
