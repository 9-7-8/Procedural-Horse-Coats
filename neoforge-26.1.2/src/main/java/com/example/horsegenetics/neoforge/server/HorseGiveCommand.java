package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>{@code /horsegive <player>} - hand over the horse in front of you.</b>
 *
 * <h2>What it is, and what it is not</h2>
 * The same transfer a signed paper makes, without the walk. Ownership is the
 * only thing that moves: {@code bredBy} is untouched, the genome, the name, the
 * pedigree and the generation are untouched, and {@code tamedBy} is only filled
 * in when it was blank. Those are {@link TransferPaperHandler}'s rules and this
 * does not get its own set - read that class for why they are what they are.
 *
 * <p>It is <b>not</b> a replacement for the paper. The paper is an object: a
 * claim the bearer has not collected yet, which can be sold, handed on, bought
 * from the cowboy or lost in a creeper hole, and which is redeemed by going and
 * finding the animal it names. None of that is a command. What the command is
 * for is the case the paper is clumsy at - two players stood in the same
 * paddock, one of whom wants the other to have the mare they are looking at,
 * and neither of whom wants to craft eight paper and a horse hair first.
 *
 * <h2>Which horse</h2>
 * The one you are <b>riding</b>, or else the one you are <b>looking at</b>,
 * within {@link #REACH} blocks. Naming a horse by name was considered and left
 * out: a stable has four mares called Pepper sooner or later, and the failure
 * mode of guessing is giving away the wrong animal - which, unlike almost
 * everything else in this mod, the giver cannot undo on their own.
 *
 * <h2>Who may run it</h2>
 * <b>Anybody</b>, on a horse they own - it is a player's command and not an
 * operator's, because giving away your own property needs no permission. A
 * <b>gamemaster</b> may give away any horse at all, which is the half that is a
 * moderation tool: somebody has to be able to settle "he tamed my mare while I
 * was offline", and {@link HorseRiding} deliberately has no operator exception
 * for an op to ride it away with instead. Every transfer goes to
 * {@link ActionTrace} and to both players' horse logs either way.
 *
 * <p><b>{@code commands.horse_give} turns it off entirely</b> - not refused,
 * not registered. A server that wants the paper to be the only way a horse
 * changes hands gets exactly that.
 *
 * <p><b>Not verified in-game.</b> It needs a second player.
 */
@EventBusSubscriber
public final class HorseGiveCommand {

    /** How far in front of you a horse can be and still be "the one you mean". */
    private static final double REACH = 8.0;

    private HorseGiveCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!ServerConfig.horseGiveCommand()) {
            // Not registered rather than registered-and-refusing. A command that
            // tab-completes and then says no is a worse answer than one that was
            // never offered, and this is read once at startup exactly as the
            // debug tools are.
            return;
        }
        event.getDispatcher().register(Commands.literal("horsegive")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> give(c, EntityArgument.getPlayer(c, "player")))));
    }

    private static int give(CommandContext<CommandSourceStack> c, ServerPlayer recipient)
            throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        ServerPlayer giver = source.getPlayerOrException();

        Horse horse = target(giver);
        if (horse == null) {
            say(source, "Get on the horse you want to give away, or look at it. "
                    + "Nothing within " + (int) REACH + " blocks.", ChatFormatting.RED);
            return 0;
        }
        if (!horse.isTamed()) {
            say(source, "That horse is not tamed, so it is nobody's to give.", ChatFormatting.RED);
            return 0;
        }
        if (!HorseRecords.hasRealRecord(horse)) {
            say(source, "That horse has no papers - this mod does not know it.", ChatFormatting.RED);
            return 0;
        }

        UUID previousOwner = HorseOwnership.ownerId(horse);
        String previousOwnerName = HorseOwnership.ownerName(horse).orElse("");
        HorseRecord record = HorseRecords.of(horse);
        String name = record.displayName();

        if (recipient.getUUID().equals(previousOwner)) {
            say(source, name + " is already " + recipient.getGameProfile().name() + "'s.",
                    ChatFormatting.RED);
            return 0;
        }
        // Ownership, unless the source is a gamemaster settling something. Both
        // answers are taken here, BEFORE setOwner below makes the ownership one
        // false for everybody - which is also what lets the log line say whether
        // a gamemaster was acting on a horse that was not theirs.
        boolean owned = HorseOwnership.isOwner(horse, giver.getUUID());
        boolean operator = Commands.LEVEL_GAMEMASTERS.check(source.permissions());
        if (!owned && !operator) {
            say(source, name + " is not your horse.", ChatFormatting.RED);
            return 0;
        }

        // Whoever is sitting on it gets off. Not a courtesy: the rider may be
        // the giver, and HorseRiding's sweep would throw them off a second later
        // anyway - doing it here means it happens at the moment the horse
        // changes hands, which is the moment that explains it.
        horse.ejectPassengers();

        horse.setOwner(recipient);
        horse.setTamed(true);
        CowboyHandler.clearBrand(horse); // it has an owner now; it is not stock any more
        if (record.tamedBy().isEmpty()) {
            HorseRecords.setTamedBy(horse, recipient.getGameProfile().name());
        }

        HorseLog.changedHands(horse, recipient.getUUID(), recipient.getGameProfile().name(),
                previousOwner, previousOwnerName, record.bredBy().orElse(""));
        recipient.sendSystemMessage(Component.literal(name + " is yours now.")
                .withStyle(ChatFormatting.GREEN));
        say(source, name + " now belongs to " + recipient.getGameProfile().name() + ".");
        ActionTrace.log("transfer", name + " given to " + recipient.getGameProfile().name()
                + " by " + source.getTextName() + (owned ? "" : " [gamemaster, not their horse]"));
        return 1;
    }

    // ------------------------------------------------------------------
    // Which horse
    // ------------------------------------------------------------------

    /**
     * The horse the player means: the one under them, or the one their crosshair
     * is on.
     *
     * <p>The look half is a real ray test against the hitbox rather than "the
     * nearest horse roughly in front", because in a paddock those are very
     * different answers and the wrong one gives away somebody else's mare. The
     * box is inflated slightly the way vanilla's own entity pick is, so a horse
     * you are plainly aiming at is not missed by a pixel.
     */
    private static Horse target(ServerPlayer player) {
        if (player.getVehicle() instanceof Horse ridden) {
            return ridden;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(REACH));
        AABB search = player.getBoundingBox().expandTowards(player.getLookAngle().scale(REACH))
                .inflate(1.0);
        List<Horse> candidates = level.getEntitiesOfClass(Horse.class, search, Horse::isAlive);

        Horse best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Horse candidate : candidates) {
            Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = eye.distanceToSqr(hit.get());
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------

    private static void say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static void say(CommandSourceStack source, String text, ChatFormatting style) {
        source.sendSuccess(() -> Component.literal(text).withStyle(style), false);
    }
}
