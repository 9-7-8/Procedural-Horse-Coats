package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>{@code /horsesweep}</b> - an operator clears the horses nobody owns out of
 * every loaded chunk, by removing them or by sending them to the horse realm.
 * <ul>
 *   <li>{@code /horsesweep <wild|cowboy|all>} - counts them, per dimension, and
 *       changes nothing;</li>
 *   <li>{@code ... despawn} - removes them, records and all, as {@link WildTurnover}
 *       removes a wild horse whose time is up;</li>
 *   <li>{@code ... realm} - releases them into the horse realm ({@link RealmSend});</li>
 *   <li>{@code turnover [enable|disable]} - the standing version of the same job:
 *       {@code wild.turnover}, which gates {@link WildTurnover} moving old wild horses
 *       on and {@link WildTopUp} refilling thin ground, saved to the server config.</li>
 * </ul>
 * Every branch is operator-only: the root requires {@code LEVEL_GAMEMASTERS}.
 * <b>Wild</b> is an untamed horse with no brand; <b>cowboy</b> is a dealer's branded
 * stock. A tamed horse is never touched, nor one a player is handling at that moment -
 * ridden, or on a lead - nor stock a player has bought the papers for
 * ({@link RealmSend#spokenFor}), since each of those is somebody's business. Only
 * loaded chunks are swept: an unloaded horse cannot be found without loading the
 * world, and the countryside refills from {@link WildTopUp} anyway. The F6 debug
 * corridor is left alone; the realm is swept by {@code despawn}, never by
 * {@code realm}.
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HorseSweepCommand {

    private enum Which {
        WILD, COWBOY, ALL;

        boolean takes(Horse horse) {
            if (horse.isTamed()) {
                return false;
            }
            boolean branded = TransferPaperHandler.isBranded(horse);
            return switch (this) {
                case WILD -> !branded;
                case COWBOY -> branded;
                case ALL -> true;
            };
        }

        String noun() {
            return switch (this) {
                case WILD -> "wild horse";
                case COWBOY -> "cowboy horse";
                case ALL -> "wild or cowboy horse";
            };
        }
    }

    private enum Action { COUNT, DESPAWN, REALM }

    private HorseSweepCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        var root = Commands.literal("horsesweep").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (Which which : Which.values()) {
            root.then(Commands.literal(which.name().toLowerCase())
                    .executes(c -> sweep(c.getSource(), which, Action.COUNT))
                    .then(Commands.literal("despawn")
                            .executes(c -> sweep(c.getSource(), which, Action.DESPAWN)))
                    .then(Commands.literal("realm")
                            .executes(c -> sweep(c.getSource(), which, Action.REALM))));
        }
        root.then(Commands.literal("turnover")
                .executes(c -> turnoverStatus(c.getSource()))
                .then(Commands.literal("enable").executes(c -> setTurnover(c.getSource(), true)))
                .then(Commands.literal("disable").executes(c -> setTurnover(c.getSource(), false))));
        event.getDispatcher().register(root);
    }

    private static int turnoverStatus(CommandSourceStack source) {
        boolean on = ServerConfig.wildTurnover();
        source.sendSuccess(() -> Component.literal("Wild turnover is " + (on ? "ON" : "OFF")
                + " (wild.turnover): " + (on
                        ? "old wild horses move on after " + ServerConfig.wildDespawnDays()
                                + " days and thin ground is refilled."
                        : "wild horses stay, and no new herds are added.")
                + " /horsesweep turnover " + (on ? "disable" : "enable") + " changes it."), false);
        return on ? 1 : 0;
    }

    private static int setTurnover(CommandSourceStack source, boolean on) {
        if (!ServerConfig.setWildTurnover(on)) {
            source.sendFailure(Component.literal("The server config is not loaded; nothing was changed."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(on
                ? "Wild turnover is ON: old wild horses move on and thin ground is refilled. Any that "
                        + "overstayed while it was off move on soon. Saved to the server config."
                : "Wild turnover is OFF: wild horses stay where they are, and no new herds are added. "
                        + "Saved to the server config.")
                .withStyle(ChatFormatting.GOLD), true);
        DebugAnnounce.log("Wild", "/horsesweep turnover " + (on ? "enable" : "disable"));
        return 1;
    }

    private static int sweep(CommandSourceStack source, Which which, Action action) {
        ServerLevel realm = source.getServer().getLevel(HorseRealm.REALM_LEVEL);
        if (action == Action.REALM && realm == null) {
            source.sendFailure(Component.literal("This world has no horse realm."));
            return 0;
        }
        int done = 0;
        int handled = 0;
        int sold = 0;
        int failed = 0;
        int slot = 0;
        List<String> perLevel = new ArrayList<>();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            if (level.dimension().equals(DebugPenManager.DEBUG_LEVEL)
                    || (action == Action.REALM && HorseRealm.isRealm(level))) {
                continue;
            }
            // Gathered first, acted on second: removing or moving an entity while
            // walking the level's entity list is a concurrent modification.
            List<Horse> found = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (e instanceof Horse horse && horse.isAlive() && which.takes(horse)) {
                    if (beingHandled(horse)) {
                        handled++;
                    } else if (RealmSend.spokenFor(horse, level)) {
                        sold++;
                    } else {
                        found.add(horse);
                    }
                }
            }
            if (found.isEmpty()) {
                continue;
            }
            perLevel.add(found.size() + " in " + level.dimension().identifier());
            for (Horse horse : found) {
                switch (action) {
                    case COUNT -> done++;
                    case DESPAWN -> {
                        despawn(level, horse);
                        done++;
                    }
                    case REALM -> {
                        if (RealmSend.send(level, realm, horse, slot) != null) {
                            slot++;
                            done++;
                        } else {
                            failed++;
                        }
                    }
                }
            }
        }

        String what = done + " " + which.noun() + (done == 1 ? "" : "s");
        String where = perLevel.isEmpty() ? "" : " (" + String.join(", ", perLevel) + ")";
        String text = switch (action) {
            case COUNT -> what + " in loaded chunks" + where + ". Add despawn or realm to clear them.";
            case DESPAWN -> "Removed " + what + where + ".";
            case REALM -> "Released " + what + " into the horse realm" + where + "."
                    + (failed > 0 ? " " + failed + " could not be moved and are where they were." : "");
        };
        if (handled > 0) {
            text += " Left " + handled + " that a player is riding or leading.";
        }
        if (sold > 0) {
            text += " Left " + sold + " cowboy horse" + (sold == 1 ? "" : "s")
                    + " already sold to a player, or whose cowboy is not loaded.";
        }
        String line = text;
        source.sendSuccess(() -> Component.literal(line)
                .withStyle(action == Action.COUNT ? ChatFormatting.WHITE : ChatFormatting.GOLD), action != Action.COUNT);
        if (action != Action.COUNT) {
            DebugAnnounce.log("Sweep", "/horsesweep " + which.name().toLowerCase() + " "
                    + action.name().toLowerCase() + ": " + line);
        }
        return done;
    }

    /** Ridden by a player, or on a lead: somebody is working with this horse right now. */
    private static boolean beingHandled(Horse horse) {
        if (horse.isLeashed()) {
            return true;
        }
        for (Entity rider : horse.getPassengers()) {
            if (rider instanceof Player) {
                return true;
            }
        }
        return false;
    }

    private static void despawn(ServerLevel level, Horse horse) {
        if (TransferPaperHandler.isBranded(horse)) {
            Cowboy dealer = RealmSend.dealerOf(horse, level);
            if (dealer != null) {
                dealer.removeFromHerd(horse.getUUID());
            }
        }
        HorseRecords.forgetDeparting(horse); // #200: its record goes with it, if nothing needs it
        horse.discard();
    }
}
