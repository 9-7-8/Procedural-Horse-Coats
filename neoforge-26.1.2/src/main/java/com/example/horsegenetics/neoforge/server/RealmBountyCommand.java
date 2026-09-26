package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.HorseRealmSize;
import com.example.horsegenetics.neoforge.data.RealmBounty;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>{@code /horsebounty} - pay people for the horses they gave the field
 * before giving one paid anything.</b> (Owner, 2026-09-26.)
 *
 * <h2>What it is for</h2>
 * Turning a horse out into the realm started paying
 * {@code realm.release_emeralds} on 2026-09-26. Every horse released before that
 * was given away for nothing, and on a server that has been running a while that
 * is most of the field. This is the one-off that squares it - and it is written
 * as a <b>repeatable</b> one-off rather than a migration, because "run it again
 * next month when more old horses have turned up" is the obvious next request
 * and a migration cannot answer it.
 *
 * <h2>Who gets paid for a horse nobody owns</h2>
 * This is the whole difficulty. A horse in the realm is <i>wild</i>, so its
 * record's owner has been deliberately cleared - that clearing is what makes it
 * a donation. So the giver has to be recovered from what is left, in the order
 * the owner asked for:
 * <ol>
 *   <li><b>{@code ownerId}</b>, on the rare horse that still has one - a tamed
 *       horse standing in the field, which somebody is visiting with rather than
 *       giving away. Those are skipped, not paid; see below.</li>
 *   <li><b>{@code tamedBy}</b> - who took it out of the wild. The best available
 *       answer for a horse somebody caught and later let go.</li>
 *   <li><b>{@code bredBy}</b> - who bred it. The answer for a foal that was
 *       never tamed by anybody, which is most of a breeding programme's
 *       surplus.</li>
 * </ol>
 * The last two are <b>names, not UUIDs</b> - they are written for a pedigree to
 * print, and a cowboy can be in them. So a name only pays if the server's
 * profile cache knows it as a real player; anything else is reported as
 * unclaimable rather than quietly dropped, because "forty horses, nobody paid"
 * is a thing an operator needs to see.
 *
 * <p>A horse whose record still carries an owner is <b>skipped entirely</b> and
 * left out of the ledger. It has not been donated - it is somebody's horse,
 * standing in a field they are also standing in - so paying for it would be
 * paying a player to visit. It stays unpaid and will be picked up by a later run
 * if it is ever genuinely let go.
 *
 * <h2>Idempotent per horse</h2>
 * {@link RealmBounty} remembers which horses have been settled, so running this
 * twice pays nothing twice, and running it next month pays only what has arrived
 * since. {@code /horsebounty list} prints the ledger - which horse, and who got
 * the emeralds.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class RealmBountyCommand {

    /** How high above a recipient the emeralds appear. */
    private static final double RAIN_HEIGHT = 6.0;

    /** How wide the shower is scattered, so it reads as rain and not as a stack. */
    private static final double RAIN_SPREAD = 1.6;

    /**
     * Emeralds per item entity. A player owed ninety should not receive ninety
     * separate falling items - that is a lag spike shaped like a reward - so the
     * shower is capped by splitting the payment into stacks and letting a large
     * one be a few fat drops rather than a cloud.
     */
    private static final int PER_DROP = 16;

    private RealmBountyCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("horsebounty")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> pay(c.getSource()))
                .then(Commands.literal("list").executes(c -> list(c.getSource())))
                .then(Commands.literal("preview").executes(c -> preview(c.getSource()))));
    }

    // ------------------------------------------------------------------
    // Working out who is owed what
    // ------------------------------------------------------------------

    /** One person's share, and the horses it is for. */
    private record Share(UUID player, String name, List<UUID> horses) {
    }

    /** What a pass over the field found: who to pay, and what could not be placed. */
    private record Plan(List<Share> shares, int unclaimable, int alreadyPaid, int stillOwned) {
    }

    private static Plan plan(MinecraftServer server) {
        RealmBounty bounty = RealmBounty.get(server);
        HorseAncestryData ancestry = HorseAncestryData.get(server);
        Map<UUID, Share> byPlayer = new LinkedHashMap<>();
        int unclaimable = 0;
        int alreadyPaid = 0;
        int stillOwned = 0;

        for (HorseRealmSize.Resident resident : HorseRealmSize.get(server).residents()) {
            UUID id = resident.horse();
            if (bounty.isPaid(id)) {
                alreadyPaid++;
                continue;
            }
            HorseRecord record = ancestry.lookup(id).orElse(null);
            if (record == null) {
                unclaimable++;
                continue;
            }
            if (record.ownerId().isPresent()) {
                // Somebody's horse, standing in a field they are visiting. Not a
                // donation, so not paid - and deliberately not written to the
                // ledger either, so that letting it go later still pays.
                stillOwned++;
                continue;
            }
            NameAndId giver = giverOf(server, record);
            if (giver == null) {
                unclaimable++;
                continue;
            }
            byPlayer.computeIfAbsent(giver.id(),
                    key -> new Share(key, giver.name(), new ArrayList<>()))
                    .horses().add(id);
        }
        return new Plan(new ArrayList<>(byPlayer.values()), unclaimable, alreadyPaid, stillOwned);
    }

    /**
     * Who gave this horse away - the ladder in the class note. Returns null when
     * no step of it lands on a real player.
     */
    private static NameAndId giverOf(MinecraftServer server, HorseRecord record) {
        Optional<NameAndId> tamer = profile(server, record.tamedBy().orElse(""));
        if (tamer.isPresent()) {
            return tamer.get();
        }
        return profile(server, record.bredBy().orElse("")).orElse(null);
    }

    /**
     * A name to a player, or empty. The server's name cache is the only thing
     * that can turn the pedigree's plain string back into somebody - and it will
     * refuse a cowboy's name, which is exactly the filter wanted. The same cache
     * {@code HorseOwnership} reads in the other direction.
     */
    private static Optional<NameAndId> profile(MinecraftServer server, String name) {
        if (name.isBlank()) {
            return Optional.empty();
        }
        return server.services().nameToIdCache().get(name);
    }

    // ------------------------------------------------------------------
    // The three subcommands
    // ------------------------------------------------------------------

    private static int preview(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        Plan plan = plan(server);
        int fee = ServerConfig.realmReleaseEmeralds();
        say(source, "Horse realm bounty - nothing has been paid, this is what would be:");
        report(source, plan, fee);
        return plan.shares().size();
    }

    private static int list(CommandSourceStack source) {
        List<RealmBounty.Paid> ledger = RealmBounty.get(source.getServer()).ledger();
        if (ledger.isEmpty()) {
            say(source, "Nothing has been paid out yet. /horsebounty preview shows what would be.");
            return 0;
        }
        say(source, ledger.size() + " horses have been paid for:");
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (RealmBounty.Paid line : ledger) {
            totals.merge(line.playerName(), line.emeralds(), Integer::sum);
        }
        for (Map.Entry<String, Integer> who : totals.entrySet()) {
            say(source, "  " + who.getKey() + " - " + who.getValue() + " emeralds");
        }
        say(source, "The full horse-by-horse ledger is in the world's "
                + "horsegenetics realm_bounty saved data.");
        return ledger.size();
    }

    private static int pay(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        int fee = ServerConfig.realmReleaseEmeralds();
        if (fee <= 0) {
            say(source, "realm.release_emeralds is 0, so turning a horse out pays nothing "
                    + "and there is nothing to back-pay. Set it first.");
            return 0;
        }
        Plan plan = plan(server);
        if (plan.shares().isEmpty()) {
            say(source, "Nobody is owed anything.");
            report(source, plan, fee);
            return 0;
        }

        RealmBounty bounty = RealmBounty.get(server);
        int horses = 0;
        int delivered = 0;
        for (Share share : plan.shares()) {
            int emeralds = share.horses().size() * fee;
            ServerPlayer player = server.getPlayerList().getPlayer(share.player());
            boolean here = player != null;
            if (here) {
                rain(player, emeralds);
                player.sendSystemMessage(Component.literal("Thank you for the "
                                + share.horses().size()
                                + (share.horses().size() == 1 ? " horse" : " horses")
                                + " you gave the field. " + emeralds + " emeralds, with interest.")
                        .withStyle(ChatFormatting.GREEN));
                delivered++;
            }
            for (UUID horse : share.horses()) {
                bounty.settle(horse, share.player(), share.name(), fee, here);
            }
            horses += share.horses().size();
            say(source, "  " + share.name() + " - " + share.horses().size() + " horses, "
                    + emeralds + " emeralds" + (here ? "" : " (owed - they are offline)"));
        }
        say(source, "Paid for " + horses + " horses across " + plan.shares().size()
                + " people; " + delivered + " were here to catch it, the rest collect on login.");
        report(source, plan, fee);
        ActionTrace.log("realm", "bounty paid for " + horses + " horses across "
                + plan.shares().size() + " people at " + fee + " each");
        return horses;
    }

    private static void report(CommandSourceStack source, Plan plan, int fee) {
        int horses = 0;
        for (Share share : plan.shares()) {
            horses += share.horses().size();
        }
        say(source, "  " + horses + " horses to " + plan.shares().size() + " people at "
                + fee + " each.");
        if (plan.alreadyPaid() > 0) {
            say(source, "  " + plan.alreadyPaid() + " already settled by an earlier run.");
        }
        if (plan.stillOwned() > 0) {
            say(source, "  " + plan.stillOwned() + " still belong to somebody - visiting, "
                    + "not donated - and are left for a later run.");
        }
        if (plan.unclaimable() > 0) {
            say(source, "  " + plan.unclaimable() + " could not be traced to a player at all "
                    + "(no record, or bred and tamed by nobody who plays here).");
        }
    }

    // ------------------------------------------------------------------
    // Paying
    // ------------------------------------------------------------------

    /**
     * <b>Emeralds fall on them.</b> (Owner: "it should do this by raining down
     * the emeralds on each person who gets them.") Item entities a few blocks
     * up, scattered, with no pickup delay - so it reads as a shower and lands in
     * the inventory without anybody chasing it.
     *
     * <p>Dropped rather than added to the inventory on purpose: a full inventory
     * would silently swallow a back-payment, and the whole point of this command
     * is that somebody notices being paid.
     */
    private static void rain(ServerPlayer player, int emeralds) {
        ServerLevel level = player.level();
        int left = emeralds;
        while (left > 0) {
            int size = Math.min(PER_DROP, left);
            left -= size;
            double x = player.getX() + (level.getRandom().nextDouble() - 0.5) * RAIN_SPREAD * 2;
            double z = player.getZ() + (level.getRandom().nextDouble() - 0.5) * RAIN_SPREAD * 2;
            ItemEntity drop = new ItemEntity(level, x, player.getY() + RAIN_HEIGHT, z,
                    new ItemStack(Items.EMERALD, size));
            drop.setDeltaMovement(0.0, -0.05, 0.0);
            drop.setNoPickUpDelay();
            level.addFreshEntity(drop);
        }
    }

    /**
     * Collect what was paid while you were away. The ledger already says the
     * horse was settled; this is the delivery catching up with the accounting.
     */
    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().getServer() == null) {
            return;
        }
        RealmBounty bounty = RealmBounty.get(player.level().getServer());
        int owed = bounty.owedTo(player.getUUID());
        if (owed <= 0) {
            return;
        }
        bounty.clearOwed(player.getUUID());
        rain(player, owed);
        player.sendSystemMessage(Component.literal(
                        "Thank you for the horses you gave the field. " + owed
                                + " emeralds, held for you while you were away.")
                .withStyle(ChatFormatting.GREEN));
    }

    private static void say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
