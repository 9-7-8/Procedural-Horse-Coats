package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * <b>{@code /horsebreeding}</b> - whether horses breed on their own
 * ({@code fertility.natural_breeding}, read by {@link NaturalBreedingHandler}).
 * <ul>
 *   <li>{@code /horsebreeding} - says whether it is on;</li>
 *   <li>{@code enable} / {@code disable} - turns it on or off and saves it to the
 *       server config, so it holds over a restart.</li>
 * </ul>
 * Off stops natural covers only. A player still breeds horses the ordinary way,
 * and a mare already in foal still foals.
 */
@EventBusSubscriber
public final class HorseBreedingCommand {

    private HorseBreedingCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("horsebreeding")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> status(c.getSource()))
                .then(Commands.literal("enable")
                        .executes(c -> set(c.getSource(), true)))
                .then(Commands.literal("disable")
                        .executes(c -> set(c.getSource(), false))));
    }

    private static int status(CommandSourceStack source) {
        boolean on = ServerConfig.naturalBreeding();
        source.sendSuccess(() -> Component.literal("Horses breeding on their own is " + (on ? "ON" : "OFF")
                + (ServerConfig.yardBreeding() ? " for this launch (-PyardBreeding; the config is not changed)."
                        : " (fertility.natural_breeding). /horsebreeding " + (on ? "disable" : "enable")
                        + " changes it.")), false);
        return on ? 1 : 0;
    }

    private static int set(CommandSourceStack source, boolean on) {
        if (!ServerConfig.setNaturalBreeding(on)) {
            source.sendFailure(Component.literal("The server config is not loaded; nothing was changed."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(on
                ? "Horses now breed on their own: a stallion left with a mare in heat covers her. Saved to the server config."
                : "Horses no longer breed on their own; a foal needs a player to breed it. Pregnancies already "
                        + "under way carry on. Saved to the server config.")
                .withStyle(ChatFormatting.GOLD), true);
        DebugAnnounce.log("Breeding", "/horsebreeding " + (on ? "enable" : "disable"));
        return 1;
    }
}
