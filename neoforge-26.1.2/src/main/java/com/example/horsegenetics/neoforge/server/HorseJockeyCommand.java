package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.data.RidingPassAttachment;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;
import java.util.UUID;

/**
 * <b>{@code /horsejockey} - lend somebody your horse, for a while.</b>
 *
 * <p>The command half of the jockey pass ({@link JockeyPassHandler}), and it
 * stands in the same relationship to it that
 * {@link HorseGiveCommand} has to the transfer paper: the item is the
 * interesting object, and the command is for the case the item is clumsy at -
 * a race steward at the gate with twelve riders to admit and no wish to craft
 * twelve passes.
 *
 * <h2>The four forms</h2>
 * <ul>
 *   <li>{@code /horsejockey <player>} - one pass's worth
 *       ({@code behaviour.jockey_pass_days}), on the horse you are riding or
 *       looking at.</li>
 *   <li>{@code /horsejockey <player> <days>} - that many Minecraft days. It
 *       <b>adds</b> to whatever is left, exactly as feeding a second pass
 *       does.</li>
 *   <li>{@code /horsejockey <player> revoke} - take it back now. There is no
 *       item that does this, and there should not be: an item you hand somebody
 *       cannot be un-handed, whereas the person who lent the horse ought to be
 *       able to change their mind.</li>
 *   <li>{@code /horsejockey list} - who may ride this horse on borrowed time,
 *       and for how much longer.</li>
 * </ul>
 *
 * <h2>Who may run it</h2>
 * <b>Anybody, on a horse they own</b> - lending your own property needs no
 * permission, the same call {@link HorseGiveCommand} makes. There is
 * deliberately <b>no gamemaster override here</b>, which is where the two
 * commands part company: an operator settling a dispute needs to be able to
 * move ownership, and that is what {@code /horsegive} is for. Being able to
 * write yourself onto somebody else's horse for a fortnight is a different power
 * and one nobody asked for.
 *
 * <p><b>{@code commands.horse_jockey} turns it off entirely</b> - not
 * registered, not tab-completed. It is a separate flag from
 * {@code commands.horse_give} because the two do different amounts of damage if
 * misused: giving a horse away is permanent and one-way, lending one expires by
 * itself.
 *
 * <p><b>Not verified in-game.</b> It needs a second player.
 */
@EventBusSubscriber
public final class HorseJockeyCommand {

    /** How far in front of you a horse can be and still be "the one you mean". */
    private static final double REACH = 8.0;

    /** The longest single grant. A year of Minecraft days is not "temporarily". */
    private static final int MAX_DAYS = 365;

    private HorseJockeyCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!ServerConfig.horseJockeyCommand()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("horsejockey")
                .then(Commands.literal("list")
                        .executes(HorseJockeyCommand::list))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> grant(c, EntityArgument.getPlayer(c, "player"), -1))
                        .then(Commands.literal("revoke")
                                .executes(c -> revoke(c, EntityArgument.getPlayer(c, "player"))))
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, MAX_DAYS))
                                .executes(c -> grant(c, EntityArgument.getPlayer(c, "player"),
                                        IntegerArgumentType.getInteger(c, "days"))))));
    }

    // ------------------------------------------------------------------

    /**
     * @param days the number typed, or {@code -1} for "one pass's worth", which
     *             is the config value rather than a hard 1 so that the command
     *             and the item never disagree about what a lend is
     */
    private static int grant(CommandContext<CommandSourceStack> c, ServerPlayer rider, int days)
            throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        ServerPlayer lender = source.getPlayerOrException();
        Horse horse = mine(source, lender);
        if (horse == null) {
            return 0;
        }
        String name = nameOf(horse);
        if (rider.getUUID().equals(lender.getUUID())) {
            say(source, "It is already your horse.", ChatFormatting.RED);
            return 0;
        }

        long now = horse.level().getGameTime();
        long ticks = days < 0 ? ServerConfig.jockeyPassTicks() : days * JockeyPassHandler.DAY_TICKS;
        RidingPassAttachment passes = horse.getData(ModAttachments.RIDING_PASS.get());
        horse.setData(ModAttachments.RIDING_PASS.get(),
                passes.grant(rider.getUUID(), now, ticks));
        long left = horse.getData(ModAttachments.RIDING_PASS.get()).remaining(rider.getUUID(), now);

        String who = rider.getGameProfile().name();
        say(source, who + " may ride " + name + " for " + JockeyPassHandler.days(left) + ".");
        rider.sendSystemMessage(Component.literal(
                        "You may ride " + name + " for " + JockeyPassHandler.days(left) + ".")
                .withStyle(ChatFormatting.GREEN));
        ActionTrace.log("jockey", source.getTextName() + " lent " + name + " to " + who
                + " - " + JockeyPassHandler.days(left) + " left");
        return 1;
    }

    private static int revoke(CommandContext<CommandSourceStack> c, ServerPlayer rider)
            throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        ServerPlayer lender = source.getPlayerOrException();
        Horse horse = mine(source, lender);
        if (horse == null) {
            return 0;
        }
        String name = nameOf(horse);
        RidingPassAttachment passes = horse.getData(ModAttachments.RIDING_PASS.get());
        RidingPassAttachment after = passes.revoke(rider.getUUID());
        if (after == passes) {
            say(source, rider.getGameProfile().name() + " had no pass for " + name + ".",
                    ChatFormatting.RED);
            return 0;
        }
        horse.setData(ModAttachments.RIDING_PASS.get(), after);
        // Thrown off now rather than at the next sweep, since the whole point of
        // revoking is that you want them off this animal. HorseRiding's sweep
        // would do it within a second anyway; this makes the cause obvious.
        if (rider.getVehicle() == horse) {
            rider.stopRiding();
            HorseRiding.refuse(horse, rider);
        }
        say(source, rider.getGameProfile().name() + " may no longer ride " + name + ".");
        rider.sendSystemMessage(Component.literal("You may no longer ride " + name + ".")
                .withStyle(ChatFormatting.GRAY));
        ActionTrace.log("jockey", source.getTextName() + " revoked " + name + " from "
                + rider.getGameProfile().name());
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        ServerPlayer lender = source.getPlayerOrException();
        Horse horse = mine(source, lender);
        if (horse == null) {
            return 0;
        }
        String name = nameOf(horse);
        long now = horse.level().getGameTime();
        Map<String, Long> live = horse.getData(ModAttachments.RIDING_PASS.get()).live(now);
        if (live.isEmpty()) {
            say(source, "Nobody is borrowing " + name + ".");
            return 0;
        }
        say(source, live.size() + (live.size() == 1 ? " person may" : " people may")
                + " ride " + name + " on borrowed time:");
        for (Map.Entry<String, Long> e : live.entrySet()) {
            // The rider may be offline, so the name comes from the same caches an
            // absent horse owner's does rather than from a player lookup.
            String who = nameFor(source, e.getKey());
            source.sendSuccess(() -> Component.literal("  " + who + " - "
                            + JockeyPassHandler.days(e.getValue() - now) + " left")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return live.size();
    }

    // ------------------------------------------------------------------

    /**
     * The horse the player means <b>and owns</b>: the one under them, or the one
     * their crosshair is on. Says why not, and answers {@code null}, rather than
     * making the three callers each repeat it.
     *
     * <p>Ownership is required for every form including {@code list} - who is
     * borrowing your horse is your business, and it is also the fastest way to
     * find out which of your neighbours a horse belongs to.
     */
    private static Horse mine(CommandSourceStack source, ServerPlayer player) {
        Horse horse = HorseGiveCommand.targetHorse(player, REACH);
        if (horse == null) {
            say(source, "Get on the horse you want to lend, or look at it. "
                    + "Nothing within " + (int) REACH + " blocks.", ChatFormatting.RED);
            return null;
        }
        if (!horse.isTamed()) {
            say(source, "That horse is not tamed, so it is nobody's to lend - "
                    + "anybody can ride it already.", ChatFormatting.RED);
            return null;
        }
        if (!HorseOwnership.isOwner(horse, player.getUUID())) {
            say(source, nameOf(horse) + " is not your horse.", ChatFormatting.RED);
            return null;
        }
        return horse;
    }

    private static String nameOf(Horse horse) {
        return HorseRecords.hasRealRecord(horse)
                ? HorseRecords.of(horse).displayName()
                : horse.getName().getString();
    }

    /** A stored UUID string as a name, through the server's own caches. */
    private static String nameFor(CommandSourceStack source, String id) {
        try {
            UUID uuid = UUID.fromString(id);
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(uuid);
            if (online != null) {
                return online.getGameProfile().name();
            }
            return source.getServer().services().nameToIdCache().get(uuid)
                    .map(net.minecraft.server.players.NameAndId::name)
                    .orElseGet(() -> id.substring(0, 8));
        } catch (IllegalArgumentException notAnId) {
            return id;
        }
    }

    private static void say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static void say(CommandSourceStack source, String text, ChatFormatting style) {
        source.sendSuccess(() -> Component.literal(text).withStyle(style), false);
    }
}
