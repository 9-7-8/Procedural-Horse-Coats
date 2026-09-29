package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.BoundHorse;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.RidingPassAttachment;
import com.example.horsegenetics.neoforge.item.JockeyPassItem;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>Jockey passes</b>: lending somebody your horse for an afternoon.
 *
 * <h2>What it is for</h2>
 * Owner, 2026-09-29: <i>"a way to add another person to be able to ride a horse,
 * maybe temporarily - thinking temporary jockeys for races."</i>
 *
 * <p>{@link HorseRiding} answers who may ride from ownership and teams, and both
 * are <b>standing</b> relationships. Neither can say <em>this person, this
 * horse, this afternoon</em>: putting a jockey on your team to lend them one
 * mount for one race hands them every horse you own, indefinitely, and taking
 * them off again afterwards is a chore nobody will do. A race meeting needs
 * something that expires on its own.
 *
 * <h2>The three moments</h2>
 * Deliberately the same shape as {@link TransferPaperHandler}'s, because it is
 * the same kind of object and a player who has used one should not have to learn
 * the other:
 * <ol>
 *   <li><b>Made.</b> Paper, a horse hair and a piece of leather is a blank pass.
 *       It names nobody and does nothing.</li>
 *   <li><b>Made out.</b> The <b>owner</b> right-clicks their horse with a blank
 *       and it becomes a pass naming that animal. Ownership is checked here and
 *       only here, the same {@link HorseOwnership#bindRefusal} every other bound
 *       item asks - and creative is no exception, for the reason that method's
 *       note gives.</li>
 *   <li><b>Fed.</b> Whoever is holding it right-clicks <i>that</i> horse, the
 *       pass is eaten, and they may ride it for a day. Only the
 *       horse the pass names; a pass for Kestrel does nothing to anything
 *       else.</li>
 * </ol>
 *
 * <h2>Feeding a second one adds a day</h2>
 * "1 minecraft day per item", so the passes stack in time rather than replacing
 * each other - see {@link RidingPassAttachment#grant}. A three-day meeting is
 * three passes, and a jockey handed two on the first morning is not quietly
 * refunded one.
 *
 * <p><b>The pass is worth nothing on a horse that has changed hands.</b>
 * Ownership moving clears every pass on the animal, because a pass is permission
 * its <i>previous</i> owner gave. That is done where ownership moves -
 * {@link HorseGiveCommand} and {@link TransferPaperHandler} - rather than here.
 *
 * <p><b>Not verified in-game.</b> Feeding needs a second player.
 */
@EventBusSubscriber
public final class JockeyPassHandler {

    /**
     * A Minecraft day, which is both the unit a pass is sold in and the unit
     * {@link #days} reports left in. The same 24000 every other daily measure in
     * this mod counts in.
     *
     * <p>What one pass is <i>worth</i> is {@code behaviour.jockey_pass_days} and
     * is read through {@link ServerConfig#jockeyPassTicks()}, not from here - the
     * owner's number is one day and a server is allowed to disagree.
     */
    public static final long DAY_TICKS = 24_000L;

    private JockeyPassHandler() {
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) {
            return;
        }
        ItemStack held = event.getItemStack();
        boolean blank = held.is(ModItems.JOCKEY_PASS.get());
        boolean bound = held.is(ModItems.BOUND_JOCKEY_PASS.get());
        if (!blank && !bound) {
            return;
        }
        // Cancelled on both sides whatever happens next, so the click never
        // falls through to mounting the horse - the same reason every other
        // bound-item handler here cancels. HorseRiding would refuse the mount
        // anyway on somebody else's horse, but a refusal message about not
        // knowing the horse is a confusing answer to "I fed it a pass".
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        if (blank) {
            makeOut(player, horse, held, event.getHand());
        } else {
            feed(player, horse, held);
        }
    }

    // ------------------------------------------------------------------
    // made out: the owner names a horse on it
    // ------------------------------------------------------------------

    private static void makeOut(Player player, Horse horse, ItemStack blank,
                                net.minecraft.world.InteractionHand hand) {
        String name = HorseRecords.hasRealRecord(horse)
                ? HorseRecords.of(horse).displayName()
                : "That horse";
        String refusal = HorseOwnership.bindRefusal(horse, player, name);
        if (refusal != null) {
            say(player, Component.literal(refusal));
            return;
        }

        ItemStack made = new ItemStack(ModItems.BOUND_JOCKEY_PASS.get());
        made.set(ModDataComponents.BOUND_HORSE.get(), new BoundHorse(horse.getUUID(), name));
        // Exactly StallSignHandler's swap: the whole stack becomes the bound one
        // when it is the last blank, and otherwise one is spent and the result
        // goes to the inventory. Writing a pass should never destroy the rest of
        // the stack in your hand.
        if (blank.getCount() <= 1) {
            player.setItemInHand(hand, made);
        } else {
            blank.shrink(1);
            if (!player.addItem(made)) {
                player.drop(made, false);
            }
        }
        say(player, Component.translatable("message.horsegenetics.jockey.made_out",
                Component.literal(name)));
    }

    // ------------------------------------------------------------------
    // fed: the bearer buys a day
    // ------------------------------------------------------------------

    private static void feed(Player player, Horse horse, ItemStack pass) {
        BoundHorse bound = JockeyPassItem.horseOf(pass);
        if (bound == null) {
            // A bound_jockey_pass with no component: only reachable by /give or
            // another mod, and it names no horse, so it buys nothing.
            say(player, Component.translatable("message.horsegenetics.jockey.unwritten"));
            return;
        }
        if (!bound.id().equals(horse.getUUID())) {
            say(player, Component.translatable("message.horsegenetics.jockey.wrong_horse",
                    Component.literal(bound.name().isBlank() ? "another horse" : bound.name())));
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }

        String name = HorseRecords.hasRealRecord(horse)
                ? HorseRecords.of(horse).displayName()
                : bound.name();
        // The owner does not need one and should be told so rather than quietly
        // spending a pass that buys them nothing. Their standing permission is
        // not stored here and would outlive this anyway.
        if (HorseOwnership.isOwner(horse, player.getUUID())) {
            say(player, Component.translatable("message.horsegenetics.jockey.already_yours",
                    Component.literal(name)));
            return;
        }

        long now = level.getGameTime();
        RidingPassAttachment passes = horse.getData(ModAttachments.RIDING_PASS.get());
        horse.setData(ModAttachments.RIDING_PASS.get(),
                passes.grant(player.getUUID(), now, ServerConfig.jockeyPassTicks()));
        if (!player.getAbilities().instabuild) {
            pass.shrink(1);
        }

        // Eaten, and it looks like it: the horse takes it from your hand.
        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.HORSE_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        long left = horse.getData(ModAttachments.RIDING_PASS.get())
                .remaining(player.getUUID(), now);
        say(player, Component.translatable("message.horsegenetics.jockey.fed",
                Component.literal(name), Component.literal(days(left))));

        // The horse's owner is told, if they are about. Lending a horse is not a
        // secret, and an owner who finds a stranger on their mare should be able
        // to remember why.
        if (horse.getOwner() instanceof ServerPlayer owner && owner != player) {
            owner.sendSystemMessage(Component.translatable(
                            "message.horsegenetics.jockey.owner_notice",
                            Component.literal(player.getGameProfile().name()),
                            Component.literal(name), Component.literal(days(left)))
                    .withStyle(ChatFormatting.GRAY));
        }
        ActionTrace.log("jockey", player.getGameProfile().name() + " fed a pass to " + name
                + " - " + days(left) + " left");
    }

    // ------------------------------------------------------------------

    /**
     * A tick count as something a player can read. Whole days while there is
     * more than one, then hours, then minutes - a pass with four hours on it and
     * a pass with four days on it are different situations and "0 days" is the
     * wrong way to say the first.
     */
    public static String days(long ticks) {
        if (ticks >= DAY_TICKS) {
            long d = ticks / DAY_TICKS;
            return d + (d == 1 ? " day" : " days");
        }
        long minutes = ticks / (20 * 60);
        if (minutes >= 60) {
            long h = minutes / 60;
            return h + (h == 1 ? " hour" : " hours");
        }
        // Floored to at least one: a pass with forty seconds left is nearly
        // spent, not "0 minutes", and the plural has to follow the number that
        // is actually printed rather than the one before the floor.
        long m = Math.max(1, minutes);
        return m + (m == 1 ? " minute" : " minutes");
    }

    private static void say(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        }
    }
}
