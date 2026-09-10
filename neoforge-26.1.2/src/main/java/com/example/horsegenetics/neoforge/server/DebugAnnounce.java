package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Says something in chat <b>and</b> writes it to the server log.
 *
 * <h2>Why this exists</h2>
 * Most of what this mod does happens where nobody is looking. A cowboy founds
 * the moment their chunk starts ticking, usually before the barn is on screen; a
 * villager takes the horseman job silently; a mounted man changes what they are
 * doing without changing what they look like. All three were previously
 * questions you answered by reading a server log while it scrolled.
 *
 * <h2>Both channels, every time</h2>
 * Chat is what you see while you play; the log is what you can paste into a bug
 * report afterwards, and it survives the chat scrolling away. A line that only
 * went to one of them always turned out to be the line that mattered.
 *
 * <h2>The gate is a config flag, and it reports itself</h2>
 * It was once {@code !FMLEnvironment.isProduction()}, and that was replaced by a
 * hard-coded {@code true} because a whole session went by with the owner
 * reporting that no debug line ever printed and no way to tell whether the gate
 * was shut or the code path was never reached - opposite bugs that looked
 * identical.
 *
 * <p>Then the mod shipped, and the hard-coded {@code true} shipped with it:
 * players who were handed 0.3.2 got {@code [Cowboy]} and {@code [Horseman]}
 * lines in their chat, which is noise to somebody who is just playing. So it is
 * now {@link ServerConfig#debugAnnounce()} - <b>on in a dev run, off in a normal
 * install, and switchable in either</b>, which is what you want from someone
 * who has a bug to report.
 *
 * <p>The lesson from the middle of that story is kept: the static block below
 * still says, once, what the gate decided <i>and</i> what the environment check
 * says, so "no debug lines printed" is answerable from the log rather than by
 * guessing.
 */
public final class DebugAnnounce {

    static {
        // Says, once, both what the gate decided and what the environment check
        // says. If a future report is "no debug lines printed", this line is the
        // first thing to look for: present means the class loaded and tells you
        // which way the switch went, absent means nothing here ever ran.
        HorseGenetics.LOGGER.info("[Debug] chat+log diagnostics enabled={} (config debug.announce), "
                        + "FMLEnvironment.isProduction()={}",
                enabled(), productionOrUnknown());
    }

    /** {@code isProduction()} throws if no loader is active; never let that break loading. */
    private static String productionOrUnknown() {
        try {
            return String.valueOf(net.neoforged.fml.loading.FMLEnvironment.isProduction());
        } catch (Throwable t) {
            return "unavailable (" + t.getClass().getSimpleName() + ")";
        }
    }

    private DebugAnnounce() {
    }

    /**
     * Is this a build where the lines below do anything? Off by default in a
     * normal install; {@code debug.announce} in the server config turns it on.
     */
    public static boolean enabled() {
        return ServerConfig.debugAnnounce();
    }

    /** One line to everyone in this level, and one to the log: {@code [tag] message}. */
    public static void say(ServerLevel level, String tag, String message, ChatFormatting colour) {
        if (!enabled()) {
            return;
        }
        HorseGenetics.LOGGER.info("[{}] {}", tag, message);
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

    /**
     * Log only, no chat. For the lines that are too frequent or too wide to read
     * in a chat box but are exactly what you want in a pasted log.
     */
    public static void log(String tag, String message) {
        if (enabled()) {
            HorseGenetics.LOGGER.info("[{}] {}", tag, message);
        }
    }
}
