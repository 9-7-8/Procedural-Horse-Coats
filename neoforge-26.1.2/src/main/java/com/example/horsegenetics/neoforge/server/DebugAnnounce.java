package com.example.horsegenetics.neoforge.server;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Says something in chat, in a dev build only.
 *
 * <h2>Why this exists</h2>
 * Most of what this mod does happens where nobody is looking. A cowboy founds
 * the moment his chunk starts ticking, usually before the barn is on screen; a
 * villager takes the horseman job silently; a mounted man changes what he is
 * doing without changing what he looks like. All three were previously
 * questions you answered by reading a server log while it scrolled, and the
 * answer to "is this working?" was several minutes of flying about.
 *
 * <p>So: a grey tag, a coloured message, and nothing at all in a real build -
 * {@link FMLEnvironment#isProduction()} is true there and every method here
 * returns immediately. These lines are <b>test scaffolding</b>: each one exists
 * to answer a specific open question in {@code wiki/known-gaps.html}, and
 * should be deleted with the question.
 */
public final class DebugAnnounce {

    private DebugAnnounce() {
    }

    /** Is this a build where the lines below do anything? */
    public static boolean enabled() {
        return !FMLEnvironment.isProduction();
    }

    /** One line to everyone in this level: {@code [tag] message}. */
    public static void say(ServerLevel level, String tag, String message, ChatFormatting colour) {
        if (!enabled()) {
            return;
        }
        Component line = Component.literal("[" + tag + "] ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(message).withStyle(colour));
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(line);
        }
    }

    /** The same, with a position written out the way you would type it into {@code /tp}. */
    public static void sayAt(ServerLevel level, String tag, String message, BlockPos at, ChatFormatting colour) {
        say(level, tag, message + " at " + at.getX() + ", " + at.getY() + ", " + at.getZ(), colour);
    }
}
