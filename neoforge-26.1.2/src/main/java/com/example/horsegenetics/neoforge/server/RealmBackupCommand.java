package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.realm.RealmRestore;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.RealmBackups;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * <b>{@code /horserealm}</b> - the operator's half of {@link RealmBackup}.
 * <ul>
 *   <li>{@code backup} - take one now;</li>
 *   <li>{@code backups} - list what is kept;</li>
 *   <li>{@code restore [name]} - say what restoring would do (the newest, if
 *       no name);</li>
 *   <li>{@code restore <name> confirm} - do it.</li>
 * </ul>
 * The preview is worked out again on confirm rather than remembered, so a
 * horse that came back in between is not raised a second time.
 */
@EventBusSubscriber
public final class RealmBackupCommand {

    private RealmBackupCommand() {
    }

    private static final SuggestionProvider<CommandSourceStack> BACKUPS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(RealmBackups.get(ctx.getSource().getServer()).all().stream()
                    .map(RealmBackups.Backup::name), builder);

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("horserealm")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("backup")
                        .executes(c -> backup(c.getSource())))
                .then(Commands.literal("backups")
                        .executes(c -> list(c.getSource())))
                .then(Commands.literal("restore")
                        .executes(c -> restore(c.getSource(), null, false))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(BACKUPS)
                                .executes(c -> restore(c.getSource(),
                                        StringArgumentType.getString(c, "name"), false))
                                .then(Commands.literal("confirm")
                                        .executes(c -> restore(c.getSource(),
                                                StringArgumentType.getString(c, "name"), true))))));
    }

    private static ServerLevel realm(CommandSourceStack source) {
        ServerLevel realm = source.getServer().getLevel(HorseRealm.REALM_LEVEL);
        if (realm == null) {
            fail(source, "This world has no horse realm.");
        }
        return realm;
    }

    private static int backup(CommandSourceStack source) {
        ServerLevel realm = realm(source);
        if (realm == null) {
            return 0;
        }
        try {
            RealmBackups.Backup b = RealmBackup.take(source.getServer(), realm, "by hand");
            // The horses are counted off the server thread after the copy (#203), so the number is not known yet.
            say(source, "Backed up the horse realm as " + b.name()
                    + ". Its horses are being counted - /horserealm backups shows the number when it is in.");
            return 1;
        } catch (IOException | RuntimeException e) {
            HorseGenetics.LOGGER.error("[realm-backup] /horserealm backup failed", e);
            fail(source, "The backup failed: " + e.getMessage() + " - see the log.");
            return 0;
        }
    }

    private static int list(CommandSourceStack source) {
        var all = RealmBackups.get(source.getServer()).all();
        if (all.isEmpty()) {
            say(source, "No realm backups yet. /horserealm backup takes one.");
            return 0;
        }
        SimpleDateFormat when = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        say(source, all.size() + " realm backup" + (all.size() == 1 ? "" : "s") + ", oldest first:");
        for (RealmBackups.Backup b : all) {
            source.sendSuccess(() -> Component.literal("  ")
                    .append(Component.literal(b.name()).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" - " + when.format(new Date(b.takenAt())) + ", "
                            + (b.counted() ? b.horses() + " horses" : "horses still being counted")
                            + ", before: " + b.reason()).withStyle(ChatFormatting.GRAY)),
                    false);
        }
        return all.size();
    }

    private static int restore(CommandSourceStack source, String name, boolean confirm) {
        MinecraftServer server = source.getServer();
        ServerLevel realm = realm(source);
        if (realm == null) {
            return 0;
        }
        RealmBackups data = RealmBackups.get(server);
        RealmBackups.Backup backup = name == null ? data.latest().orElse(null) : data.named(name).orElse(null);
        if (backup == null) {
            fail(source, name == null ? "There are no realm backups." : "No realm backup is called " + name + ".");
            return 0;
        }
        if (RealmBackup.raisesPending() > 0) {
            fail(source, "A restore is still raising horses (" + RealmBackup.raisesPending() + " to go).");
            return 0;
        }
        RealmBackup.Plan plan;
        try {
            plan = RealmBackup.plan(server, realm, backup.name());
        } catch (IOException | RuntimeException e) {
            HorseGenetics.LOGGER.error("[realm-backup] could not read {}", backup.name(), e);
            fail(source, "Could not read " + backup.name() + ": " + e.getMessage() + " - see the log.");
            return 0;
        }
        Map<RealmRestore.Action, Integer> n = plan.counts();
        say(source, "Restoring " + backup.name() + " (" + plan.entries().size() + " horses):");
        say(source, "  " + n.get(RealmRestore.Action.RAISE) + " dead or lost since - raised at their old spot");
        say(source, "  " + n.get(RealmRestore.Action.RESCUE) + " loaded in the realm - moved up if stuck in a block");
        say(source, "  " + n.get(RealmRestore.Action.RESCUE_LATER) + " unloaded in the realm - checked when they load");
        say(source, "  " + n.get(RealmRestore.Action.LEAVE) + " left the realm or in a chamber - left alone");
        if (!confirm) {
            say(source, "Nothing has changed yet. /horserealm restore " + backup.name() + " confirm does it.");
            return 0;
        }
        int rescued = RealmBackup.execute(server, plan);
        say(source, "Restoring: " + rescued + " moved out of blocks, " + RealmBackup.raisesPending()
                + " being raised over the next few seconds. The log says when it is done.");
        return n.get(RealmRestore.Action.RAISE);
    }

    private static void say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static void fail(CommandSourceStack source, String text) {
        source.sendFailure(Component.literal(text));
    }
}
