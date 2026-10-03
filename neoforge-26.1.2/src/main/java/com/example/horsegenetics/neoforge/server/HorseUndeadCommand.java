package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Undeath;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * <b>{@code /horseundead}</b> - an operator tries the undead conversion on their own
 * server before letting it loose on every vanilla undead horse in the world (owner,
 * 2026-10-03: {@code undead.convert} ships off, and is turned on from here).
 * <ul>
 *   <li>{@code /horseundead} - whether conversion is on, and whether a test has passed;</li>
 *   <li>{@code test} - spawns one vanilla skeleton horse beside the operator, lets the
 *       real converter take it on its own entity tick, checks the horse it became, then
 *       does the same with a zombie horse. Each subject carries
 *       {@link UndeadHorseConverter#CONVERT_ANYWAY}, so this works with the setting off.
 *       Run by a player, the subjects are tamed to them, named and saddled - the hard
 *       case - and the converted horses are left standing for them to look at;</li>
 *   <li>{@code enable} - turns {@code undead.convert} on and saves it, but only after a
 *       test passed since the server started;</li>
 *   <li>{@code disable} - turns it off again. Horses already converted stay converted.</li>
 * </ul>
 * The conversion is never forced from the command itself: writing a horse's scale
 * outside its entity tick has crashed the chunk system before (converter, 3.2), so the
 * command only spawns the subject and watches.
 */
@EventBusSubscriber
public final class HorseUndeadCommand {

    /** How long one subject may take to convert before the test is called failed: ten seconds. */
    private static final int TIMEOUT_TICKS = 200;

    private static final String TEST_NAME = "Undead test";

    /** One horse being watched. */
    private record Subject(UUID id, EntityType<? extends AbstractHorse> type, Undeath.Kind kind, String what) {
    }

    /** A test in progress: who asked, where, and the subjects still to come. */
    private static final class Run {
        final CommandSourceStack source;
        final ServerLevel level;
        final BlockPos at;
        final UUID owner;
        final Deque<Subject> todo = new ArrayDeque<>();
        Subject current;
        int waited;

        Run(CommandSourceStack source, ServerLevel level, BlockPos at, UUID owner) {
            this.source = source;
            this.level = level;
            this.at = at;
            this.owner = owner;
        }
    }

    private static Run running;
    private static boolean passed;

    private HorseUndeadCommand() {
    }

    /** Has {@code /horseundead test} passed since the server started? For the gametest. */
    public static boolean testPassed() {
        return passed;
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("horseundead")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> status(c.getSource()))
                .then(Commands.literal("test")
                        .executes(c -> test(c.getSource())))
                .then(Commands.literal("enable")
                        .executes(c -> enable(c.getSource())))
                .then(Commands.literal("disable")
                        .executes(c -> disable(c.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        say(source, "Undead conversion is " + (ServerConfig.undeadConvert() ? "ON" : "OFF")
                + " (undead.convert). " + (passed ? "A test passed since the server started."
                : "No test has passed since the server started."));
        if (!ServerConfig.undeadConvert()) {
            say(source, "/horseundead test converts one skeleton and one zombie horse beside you; "
                    + "/horseundead enable then turns it on for every vanilla undead horse.");
        }
        return ServerConfig.undeadConvert() ? 1 : 0;
    }

    private static int test(CommandSourceStack source) {
        if (running != null) {
            fail(source, "A test is already running.");
            return 0;
        }
        Entity caller = source.getEntity();
        UUID owner = caller instanceof Player p ? p.getUUID() : null;
        Run run = new Run(source, source.getLevel(), BlockPos.containing(source.getPosition()), owner);
        run.todo.add(new Subject(null, EntityType.SKELETON_HORSE, Undeath.Kind.SKELETON, "skeleton horse"));
        run.todo.add(new Subject(null, EntityType.ZOMBIE_HORSE, Undeath.Kind.ZOMBIE, "zombie horse"));
        passed = false;
        running = run;
        if (!next(run)) {
            running = null;
            return 0;
        }
        return 1;
    }

    /** Spawn the next subject; false (and the test failed) if it could not be placed. */
    private static boolean next(Run run) {
        Subject plan = run.todo.poll();
        AbstractHorse horse = plan.type().create(run.level, EntitySpawnReason.COMMAND);
        if (horse == null) {
            fail(run.source, "Test FAILED: could not create a " + plan.what() + ".");
            return false;
        }
        int side = plan.kind() == Undeath.Kind.SKELETON ? 2 : -2;
        horse.snapTo(run.at.getX() + side + 0.5, run.at.getY(), run.at.getZ() + 0.5, 0f, 0f);
        horse.getPersistentData().putBoolean(UndeadHorseConverter.CONVERT_ANYWAY, true);
        Player player = run.owner == null ? null : run.level.getPlayerByUUID(run.owner);
        if (player != null) {
            horse.tameWithName(player);
            horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        }
        horse.setCustomName(Component.literal(TEST_NAME));
        if (!run.level.addFreshEntity(horse)) {
            fail(run.source, "Test FAILED: the world refused the " + plan.what() + ".");
            return false;
        }
        run.current = new Subject(horse.getUUID(), plan.type(), plan.kind(), plan.what());
        run.waited = 0;
        say(run.source, "Spawned a vanilla " + plan.what() + (player != null ? " (tamed to you, named, saddled)" : "")
                + "; waiting for it to convert...");
        return true;
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        Run run = running;
        if (run == null) {
            return;
        }
        Subject s = run.current;
        Entity e = run.level.getEntity(s.id());
        if (e instanceof Horse horse && horse.isAlive()) {
            String problem = check(horse, s, run.owner != null && run.level.getPlayerByUUID(run.owner) != null);
            if (problem != null) {
                finish(run, false, "Test FAILED on the " + s.what() + ": " + problem + ".");
                return;
            }
            HorseRecord record = HorseRecords.of(horse);
            say(run.source, "The " + s.what() + " converted: " + record.displayName() + " ("
                    + (record.firstName() + " " + record.lastName()).strip() + ", "
                    + record.breed().orElse("no breed") + ") at " + horse.blockPosition().toShortString() + ".");
            if (run.todo.isEmpty()) {
                finish(run, true, "Test PASSED. /horseundead enable turns conversion on for every vanilla "
                        + "undead horse on this server; it cannot be undone for the horses it converts.");
            } else if (!next(run)) {
                running = null;
            }
            return;
        }
        if (e == null && run.waited > 20 && !run.level.isLoaded(run.at)) {
            finish(run, false, "Test FAILED: the " + s.what() + " is gone and its spot is unloaded.");
            return;
        }
        if (++run.waited > TIMEOUT_TICKS) {
            finish(run, false, "Test FAILED: the " + s.what() + " did not convert within "
                    + TIMEOUT_TICKS / 20 + " seconds" + (e == null ? " and has vanished" : "")
                    + ". The server log has the converter's reason.");
        }
    }

    /** What is wrong with the horse a subject became, or null. */
    private static String check(Horse horse, Subject s, boolean tamed) {
        if (!HorseRecords.hasRealRecord(horse)) {
            return "it has no horse record";
        }
        if (UndeadHorses.kindOf(horse) != s.kind()) {
            return "it does not express the " + s.kind().name().toLowerCase() + " gene";
        }
        if (!TEST_NAME.equals(HorseRecords.of(horse).displayName())) {
            return "it lost its name";
        }
        if (horse.getPersistentData().getBooleanOr(UndeadHorseConverter.CONVERT_ANYWAY, false)) {
            return "it still carries the test flag";
        }
        if (tamed) {
            if (!horse.isTamed() || horse.getOwnerReference() == null) {
                return "it lost its owner";
            }
            if (!horse.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE)) {
                return "it lost its saddle";
            }
        }
        return null;
    }

    private static void finish(Run run, boolean ok, String text) {
        running = null;
        passed = ok;
        if (ok) {
            run.source.sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.GREEN), true);
        } else {
            fail(run.source, text);
        }
        DebugAnnounce.log("Undead", "/horseundead test " + (ok ? "passed" : "failed") + ": " + text);
    }

    private static int enable(CommandSourceStack source) {
        if (ServerConfig.undeadConvert()) {
            say(source, "Undead conversion is already on.");
            return 1;
        }
        if (!passed) {
            fail(source, "Run /horseundead test first; conversion can be enabled once it passes.");
            return 0;
        }
        if (!ServerConfig.setUndeadConvert(true)) {
            fail(source, "The server config is not loaded; nothing was changed.");
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Undead conversion is ON: every vanilla zombie and skeleton "
                + "horse converts the next time it is ticked. Saved to the server config.")
                .withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int disable(CommandSourceStack source) {
        if (!ServerConfig.setUndeadConvert(false)) {
            fail(source, "The server config is not loaded; nothing was changed.");
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Undead conversion is OFF. Horses already converted stay "
                + "converted."), true);
        return 1;
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        running = null;
        passed = false;
    }

    private static void say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static void fail(CommandSourceStack source, String text) {
        source.sendFailure(Component.literal(text));
    }
}
