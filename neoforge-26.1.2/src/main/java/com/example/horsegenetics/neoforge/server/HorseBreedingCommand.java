package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.repro.BreedingSwitches;
import com.example.horsegenetics.common.repro.BreedingSwitches.Kind;
import com.example.horsegenetics.common.repro.BreedingSwitches.RealmCover;
import com.example.horsegenetics.neoforge.ServerConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * <b>{@code /horsebreeding}</b> - every breeding switch, read and set while the
 * server runs (issue #276). Each change is saved to the server config, so it holds
 * over a restart.
 * <ul>
 *   <li>{@code /horsebreeding} - lists every switch and the master;</li>
 *   <li>{@code /horsebreeding all enable|disable} - the master ({@code breeding.enabled});</li>
 *   <li>{@code /horsebreeding <kind> enable|disable} - one kind; {@code realm_cover}
 *       also takes {@code follow};</li>
 *   <li>{@code /horsebreeding enable|disable} - natural cover, as it always meant.</li>
 * </ul>
 * Off stops new breedings only: a mare already in foal still foals.
 */
@EventBusSubscriber
public final class HorseBreedingCommand {

    private HorseBreedingCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("horsebreeding")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> status(c.getSource()))
                .then(Commands.literal("enable")
                        .executes(c -> set(c.getSource(), Kind.NATURAL_COVER, true, RealmCover.ON)))
                .then(Commands.literal("disable")
                        .executes(c -> set(c.getSource(), Kind.NATURAL_COVER, false, RealmCover.OFF)))
                .then(Commands.literal("all")
                        .then(Commands.literal("enable").executes(c -> set(c.getSource(), null, true, RealmCover.ON)))
                        .then(Commands.literal("disable").executes(c -> set(c.getSource(), null, false, RealmCover.OFF))));
        for (Kind kind : Kind.values()) {
            LiteralArgumentBuilder<CommandSourceStack> sub = Commands.literal(kind.id())
                    .then(Commands.literal("enable").executes(c -> set(c.getSource(), kind, true, RealmCover.ON)))
                    .then(Commands.literal("disable").executes(c -> set(c.getSource(), kind, false, RealmCover.OFF)));
            if (kind == Kind.REALM_COVER) {
                sub.then(Commands.literal("follow").executes(c -> set(c.getSource(), kind, true, RealmCover.FOLLOW)));
            }
            root.then(sub);
        }
        event.getDispatcher().register(root);
    }

    private static int status(CommandSourceStack source) {
        BreedingSwitches s = ServerConfig.breeding();
        StringBuilder text = new StringBuilder("Breeding switches - master (breeding.enabled): ")
                .append(s.master() ? "ON" : "OFF - nothing breeds, whatever the lines below say");
        for (Kind kind : Kind.values()) {
            text.append("\n  ").append(kind.id()).append(": ").append(s.own(kind) ? "on" : "off");
            if (kind == Kind.REALM_COVER && s.realmCover() == RealmCover.FOLLOW) {
                text.append(" (follows natural_cover)");
            }
            if (kind == Kind.NATURAL_COVER && ServerConfig.yardBreeding()) {
                text.append(" (forced on for this launch by -PyardBreeding; the config is not changed)");
            }
            text.append(" - ").append(kind.label());
        }
        text.append("\n/horsebreeding <kind> enable|disable, or all enable|disable for the master.");
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return s.master() ? 1 : 0;
    }

    /** {@code kind} null is the master. {@code realm} is only read for REALM_COVER. */
    private static int set(CommandSourceStack source, Kind kind, boolean on, RealmCover realm) {
        if (!ServerConfig.setBreeding(kind, on, realm)) {
            source.sendFailure(Component.literal("The server config is not loaded; nothing was changed."));
            return 0;
        }
        String what = kind == null ? "All breeding (the master switch)" : kind.label();
        String state = kind == Kind.REALM_COVER && realm == RealmCover.FOLLOW
                ? "now follows natural cover" : on ? "is now ON" : "is now OFF";
        String tail = on ? "" : " Pregnancies already under way still foal.";
        if (on && kind != null && !ServerConfig.breeding().master()) {
            tail = " The master switch is OFF, so nothing breeds until /horsebreeding all enable.";
        }
        String line = what + " " + state + "." + tail + " Saved to the server config.";
        source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GOLD), true);
        DebugAnnounce.log("Breeding", "/horsebreeding " + (kind == null ? "all" : kind.id()) + " "
                + (kind == Kind.REALM_COVER && realm == RealmCover.FOLLOW ? "follow" : on ? "enable" : "disable"));
        return 1;
    }
}
