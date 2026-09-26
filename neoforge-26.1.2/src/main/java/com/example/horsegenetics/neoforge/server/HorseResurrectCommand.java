package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.neoforge.data.HorseAfterlife;
import com.example.horsegenetics.neoforge.item.PresetHorseSpawnEggItem;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <b>{@code /horseresurrect} - give somebody their horse back.</b>
 *
 * <h2>What it is for</h2>
 * Every other loss in this mod is recoverable. A horse sold, turned out,
 * bottled, lost across a dimension or left in a stall is still somewhere, and
 * there is a tool that finds it. A horse that died is the exception, and the
 * thing that dies is not a stack of items - it is an animal with a name
 * somebody chose, a bond they spent weeks on and a pedigree three generations
 * deep. Rolling a fresh horse with the same alleles is not a replacement for
 * that and everyone involved can tell.
 *
 * <p>So this brings back <b>the same horse</b>. {@link HorseAfterlife} keeps the
 * whole animal as an entity tag for a while after it dies, UUID included, and
 * this is what spends it: the resurrected horse is the one every foal's
 * pedigree, every stall sign and every bound whistle already points at.
 *
 * <h2>It is an operator's tool and stays one</h2>
 * {@code LEVEL_GAMEMASTERS}, like {@code /horsebounty}. There is no item, no
 * recipe and no shrine, and that is a design position rather than an unfinished
 * feature: death is the only real stake horse care has, and a resurrection a
 * player can reach on their own removes it. An operator deciding case by case
 * that a particular loss was a server hiccup, a griefing, or somebody's
 * favourite mare and a creeper is a different thing from a mechanic.
 *
 * <p>Three things already try to stop a horse dying before it ever gets here -
 * the last stand, the rescuing braid and the emergency stasis chamber. See
 * {@link HorseAfterlifeHandler}.
 *
 * <h2>The four ways to spend one</h2>
 * <ul>
 *   <li>{@code /horseresurrect list [player]} - what is still recoverable, and
 *       for how much longer.</li>
 *   <li>{@code /horseresurrect <horse>} - it stands up in front of the
 *       operator.</li>
 *   <li>{@code /horseresurrect <horse> at <player>} - in front of somebody
 *       else, which is the usual one: the owner is stood there asking.</li>
 *   <li>{@code /horseresurrect <horse> site} - where it died. Loads that chunk,
 *       and is the only one that can put a horse back in the lava that killed
 *       it, so it is never the default.</li>
 *   <li>{@code /horseresurrect <horse> egg <player>} - into their inventory, to
 *       place where they want it. The horse leaves the save's keeping at that
 *       moment and lives in the item, so it no longer expires.</li>
 * </ul>
 *
 * <p><b>Not verified in-game.</b>
 */
@EventBusSubscriber
public final class HorseResurrectCommand {

    /** How far from the target player the horse appears. Close enough to be obviously theirs. */
    private static final double BESIDE = 1.5;

    private HorseResurrectCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("horseresurrect")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> list(c.getSource(), null))
                .then(Commands.literal("list")
                        .executes(c -> list(c.getSource(), null))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> list(c.getSource(),
                                        EntityArgument.getPlayer(c, "player")))))
                .then(Commands.argument("horse", StringArgumentType.string())
                        .suggests(PENDING_HORSES)
                        .executes(c -> atSource(c))
                        .then(Commands.literal("site")
                                .executes(c -> atSite(c)))
                        .then(Commands.literal("at")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> atPlayer(c,
                                                EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("egg")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> asEgg(c,
                                                EntityArgument.getPlayer(c, "player")))))));
    }

    /**
     * Every horse currently recoverable, by name and then by id. Names first
     * because that is what an operator has been told in chat - "Pepper died" -
     * and the id is the fallback for two horses called the same thing.
     *
     * <p><b>The names are suggested with their quotes on.</b> Every generated
     * horse name is two words ("Echo of the Ember" is four), and
     * {@link StringArgumentType#string()} takes a bare word or a quoted string
     * and nothing else - so a suggestion offered raw would complete to
     * something the parser then rejects at the first space. Brigadier does not
     * quote for you.
     */
    private static final SuggestionProvider<CommandSourceStack> PENDING_HORSES = (ctx, builder) -> {
        List<String> names = new ArrayList<>();
        for (HorseAfterlife.Wake wake : HorseAfterlife.get(ctx.getSource().getServer()).all()) {
            if (!wake.horseName().isBlank()) {
                names.add(quoted(wake.horseName()));
            }
            names.add(wake.horse().toString());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    /** A name as the parser needs to read it back: quoted unless it is one bare word. */
    private static String quoted(String name) {
        if (name.matches("[A-Za-z0-9_.+-]+")) {
            return name;
        }
        return '"' + name.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    // ------------------------------------------------------------------
    // Listing
    // ------------------------------------------------------------------

    private static int list(CommandSourceStack source, ServerPlayer only) {
        MinecraftServer server = source.getServer();
        HorseAfterlife afterlife = HorseAfterlife.get(server);
        int grace = ServerConfig.resurrectGraceTicks();
        List<HorseAfterlife.Wake> wakes = only == null
                ? afterlife.all()
                : afterlife.ownedBy(only.getUUID());

        if (wakes.isEmpty()) {
            say(source, only == null
                    ? "No dead horses are being kept. Nothing has died recently enough, "
                            + "or nothing that anybody owned."
                    : only.getGameProfile().name() + " has no horses waiting to be brought back.");
            return 0;
        }

        long now = server.overworld().getGameTime();
        say(source, wakes.size() + (wakes.size() == 1 ? " horse can" : " horses can")
                + " still be brought back:");
        for (HorseAfterlife.Wake wake : wakes) {
            String owner = wake.ownerName().isBlank()
                    ? wake.owner().toString().substring(0, 8)
                    : wake.ownerName();
            source.sendSuccess(() -> Component.literal("  ")
                    .append(Component.literal(displayOf(wake)).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" - " + owner
                                    + ", died " + ago(now, wake.deathGameTime())
                                    + ", " + left(wake, grace))
                            .withStyle(ChatFormatting.GRAY)), false);
        }
        if (grace == 0) {
            say(source, "ops.resurrect_grace_minutes is 0, so none of these will ever "
                    + "be thrown away. That is disk, per horse, for ever.");
        }
        say(source, "/horseresurrect <name> at <player> hands one back.");
        return wakes.size();
    }

    /** What to type to name this horse: its name, or its id when it has no usable name. */
    private static String displayOf(HorseAfterlife.Wake wake) {
        return wake.horseName().isBlank() ? wake.horse().toString() : wake.horseName();
    }

    /**
     * How long ago in the world's own terms. Game time rather than anything
     * real, because it is the clock every other in-game measure uses and the
     * operator is standing in that world.
     */
    private static String ago(long now, long then) {
        long ticks = Math.max(0, now - then);
        long minutes = ticks / (20 * 60);
        if (minutes < 1) {
            return "moments ago";
        }
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }
        long hours = minutes / 60;
        return hours + (hours == 1 ? " hour ago" : " hours ago");
    }

    /** How much of the owner's window is left, said in the terms the window is measured in. */
    private static String left(HorseAfterlife.Wake wake, int grace) {
        int remaining = wake.remaining(grace);
        if (remaining < 0) {
            return "kept indefinitely";
        }
        int minutes = remaining / (20 * 60);
        return minutes < 1
                ? "under a minute of their time left"
                : minutes + (minutes == 1 ? " minute" : " minutes")
                        + " of their time online left";
    }

    // ------------------------------------------------------------------
    // The four ways to spend one
    // ------------------------------------------------------------------

    private static int atSource(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        HorseAfterlife.Wake wake = resolve(c);
        if (wake == null) {
            return 0;
        }
        return place(source, wake, source.getLevel(), source.getPosition(), yRotOf(source), null);
    }

    private static int atPlayer(CommandContext<CommandSourceStack> c, ServerPlayer target)
            throws CommandSyntaxException {
        HorseAfterlife.Wake wake = resolve(c);
        if (wake == null) {
            return 0;
        }
        Vec3 beside = target.position().add(
                target.getLookAngle().x * BESIDE, 0.0, target.getLookAngle().z * BESIDE);
        return place(c.getSource(), wake, target.level(), beside, target.getYRot(), target);
    }

    /**
     * Where it died. The chunk may be nowhere near anybody, so it is forced
     * loaded for the placement - {@link ServerLevel#getChunk} with a block
     * position does that - and released again as soon as the horse is in it.
     */
    private static int atSite(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        HorseAfterlife.Wake wake = resolve(c);
        if (wake == null) {
            return 0;
        }
        ServerLevel level = source.getServer().getLevel(wake.dimension());
        if (level == null) {
            say(source, "The dimension " + wake.dimension().identifier()
                    + " it died in is not loaded on this server.", ChatFormatting.RED);
            return 0;
        }
        BlockPos at = wake.pos();
        level.getChunk(at);
        say(source, "Whatever killed it may still be there.", ChatFormatting.GRAY);
        return place(source, wake, level,
                new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), 0.0F, null);
    }

    private static int asEgg(CommandContext<CommandSourceStack> c, ServerPlayer target)
            throws CommandSyntaxException {
        CommandSourceStack source = c.getSource();
        HorseAfterlife.Wake wake = resolve(c);
        if (wake == null) {
            return 0;
        }
        if (HorseResurrection.alreadyAlive(source.getServer(), wake.snapshot())) {
            return alreadyAlive(source, wake);
        }

        ItemStack egg = PresetHorseSpawnEggItem.resurrecting(wake.snapshot());
        if (!target.getInventory().add(egg)) {
            // Dropped rather than refused: the horse is still in the store at
            // this point, so nothing is lost either way, but an operator who
            // has said "give it to them" means it.
            target.drop(egg, false);
        }
        // Out of the save's keeping and into the item. One copy, and it stops
        // being something the expiry sweep can throw away.
        HorseAfterlife.get(source.getServer()).forget(wake.horse());

        String who = target.getGameProfile().name();
        say(source, displayOf(wake) + " is in " + who + "'s inventory. "
                + "It no longer expires - the egg is the horse now.");
        target.sendSystemMessage(Component.literal(
                        displayOf(wake) + " is in this egg. Right-click the ground to put "
                                + (wake.horseName().isBlank() ? "them" : "them") + " back.")
                .withStyle(ChatFormatting.GREEN));
        ActionTrace.log("afterlife", displayOf(wake) + " given to " + who + " as an egg by "
                + source.getTextName());
        return 1;
    }

    /**
     * Put the horse down, and spend the wake. The store is only cleared once
     * the horse is genuinely standing in the world: a placement that fails
     * leaves it recoverable rather than destroying it on a bad guess about a
     * spot.
     */
    private static int place(CommandSourceStack source, HorseAfterlife.Wake wake, ServerLevel level,
                             Vec3 pos, float yRot, ServerPlayer tell) {
        if (HorseResurrection.alreadyAlive(source.getServer(), wake.snapshot())) {
            return alreadyAlive(source, wake);
        }
        Horse horse = HorseResurrection.raise(level, pos, yRot, wake.snapshot());
        if (horse == null) {
            say(source, "Could not read " + displayOf(wake) + " back out - see the log. "
                    + "It is still being kept, so this can be tried again.", ChatFormatting.RED);
            return 0;
        }
        HorseAfterlife.get(source.getServer()).forget(wake.horse());

        String name = HorseRecords.of(horse).displayName();
        say(source, name + " is back, at " + Math.round(pos.x) + " " + Math.round(pos.y)
                + " " + Math.round(pos.z) + " in " + level.dimension().identifier() + ".");
        if (tell != null) {
            tell.sendSystemMessage(Component.literal(name + " is back.")
                    .withStyle(ChatFormatting.GREEN));
        }
        ActionTrace.log("afterlife", name + " resurrected by " + source.getTextName());
        return 1;
    }

    private static int alreadyAlive(CommandSourceStack source, HorseAfterlife.Wake wake) {
        say(source, displayOf(wake) + " is already alive somewhere - two horses cannot "
                + "share one id. Dropping it from the store.", ChatFormatting.RED);
        HorseAfterlife.get(source.getServer()).forget(wake.horse());
        return 0;
    }

    // ------------------------------------------------------------------
    // Naming a horse
    // ------------------------------------------------------------------

    /**
     * <b>Which horse the operator meant.</b> A full UUID, or a display name -
     * which is what they will actually have, because a name is what the death
     * notice and the owner both said.
     *
     * <p>An ambiguous name is <b>refused with the candidates listed</b> rather
     * than resolved to the most recent. Two horses called Pepper is exactly the
     * situation where guessing wrong means resurrecting the wrong animal and
     * spending the other one's window while its owner waits.
     *
     * @return the wake, or {@code null} having already said why not
     */
    private static HorseAfterlife.Wake resolve(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        String typed = StringArgumentType.getString(c, "horse").strip();
        HorseAfterlife afterlife = HorseAfterlife.get(source.getServer());

        try {
            HorseAfterlife.Wake byId = afterlife.lookup(UUID.fromString(typed)).orElse(null);
            if (byId != null) {
                return byId;
            }
            say(source, "No horse with that id is being kept.", ChatFormatting.RED);
            return null;
        } catch (IllegalArgumentException notAnId) {
            // A name, then - the ordinary case.
        }

        List<HorseAfterlife.Wake> matches = new ArrayList<>();
        for (HorseAfterlife.Wake wake : afterlife.all()) {
            if (wake.horseName().equalsIgnoreCase(typed)) {
                matches.add(wake);
            }
        }
        if (matches.isEmpty()) {
            say(source, "No horse called \"" + typed + "\" is being kept. "
                    + "/horseresurrect list shows what is.", ChatFormatting.RED);
            return null;
        }
        if (matches.size() > 1) {
            say(source, matches.size() + " horses called \"" + typed
                    + "\" are being kept. Name one by id:", ChatFormatting.RED);
            long now = source.getServer().overworld().getGameTime();
            for (HorseAfterlife.Wake wake : matches) {
                say(source, "  " + wake.horse() + " - " + wake.ownerName()
                        + ", died " + ago(now, wake.deathGameTime()), ChatFormatting.GRAY);
            }
            return null;
        }
        return matches.get(0);
    }

    /** Where the source is facing, or north for a console that has no facing. */
    private static float yRotOf(CommandSourceStack source) {
        return source.getEntity() == null ? 0.0F : source.getEntity().getYRot();
    }

    // ------------------------------------------------------------------

    private static void say(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static void say(CommandSourceStack source, String text, ChatFormatting style) {
        source.sendSuccess(() -> Component.literal(text).withStyle(style), false);
    }
}
