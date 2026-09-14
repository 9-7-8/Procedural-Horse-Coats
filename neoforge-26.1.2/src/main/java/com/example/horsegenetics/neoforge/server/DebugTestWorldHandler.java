package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.data.StoredGenome;
import com.example.horsegenetics.neoforge.item.BreedSpawnEggItem;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.PresetHorseSpawnEggItem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.component.DataComponents;
import java.util.ArrayList;
import java.util.List;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.jspecify.annotations.Nullable;

/**
 * Dev-only: when the "Spawn Test Horse World" title-screen button
 * ({@code client/DebugTitleScreenButton}) creates a world, it sets
 * {@link #pendingHotbarFill}; on the next player login we stock the player up
 * for exercising horse features and put them somewhere worth testing from.
 *
 * <ul>
 *   <li><b>Hotbar</b>: a test kit aimed at whatever is waiting on the
 *       verification checklist, one batch of nine at a time - see
 *       {@link #BATCHES} and {@code /testkit}, and re-aim it whenever that list
 *       changes.</li>
 *   <li><b>Directions</b>: where the nearest <b>plains village</b> is, and the
 *       {@code /tp} that gets you there ({@link #locatePlainsVillage}) - because
 *       that is where both villagers live and hunting for one on foot is most of
 *       the cost of testing them. And the nearest <b>dark forest</b>
 *       ({@link #locateDarkForest}), where the kit's drop-in breeds live.</li>
 * </ul>
 *
 * Inert in production (nothing sets the flag, and {@code /testkit} is not
 * registered).
 */
@EventBusSubscriber
public final class DebugTestWorldHandler {

    public static volatile boolean pendingHotbarFill = false;

    /**
     * How far to hunt for a plains village, in chunks. Villages sit on a
     * 34-chunk grid, so this is many cells in every direction and a miss means
     * the spawn landed somewhere with no plains for a thousand blocks rather
     * than that the search was too tight.
     */
    private static final int VILLAGE_SEARCH_CHUNKS = 100;

    /**
     * The altitude the offered {@code /tp} aims at. <b>An absolute y, not an
     * offset</b> - you arrive in the air over the village and fly down, which is
     * the right arrival in a creative dev world and beats materialising inside
     * whatever happens to be standing on the block the structure search named.
     *
     * <p>Clamped up to the local surface, so a village on a mountainside cannot
     * hand you a command that buries you.
     */
    private static final int TP_ALTITUDE = 100;

    private DebugTestWorldHandler() {
    }

    @SubscribeEvent
    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!pendingHotbarFill || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        pendingHotbarFill = false;
        giveBatch(player, 1);
        reportSpawnBiome(player);
        locatePlainsVillage(player);
        locateDarkForest(player);
    }

    /**
     * <b>Say which biome you actually landed in</b>, and hand over a teleport
     * when it is not plains.
     *
     * <p>The test world's seed is fixed so that it is always the same plains,
     * which is the ground every coat is judged against. That is a claim about
     * worldgen, and worldgen is somebody else's code: a Minecraft update, a
     * dimension preset change or a mod that adds biomes can move the spawn
     * without moving the seed. So the promise checks itself out loud on every
     * login rather than being believed - and if it has drifted, the answer is
     * one click away instead of a hunt.
     */
    private static void reportSpawnBiome(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Holder<Biome> here = level.getBiome(player.blockPosition());
        String name = here.unwrapKey().map(k -> k.identifier().toString()).orElse("an unnamed biome");
        long seed = level.getSeed();
        HorseGenetics.LOGGER.info("[Debug] test world seed {} spawned in {} at {}",
                seed, name, player.blockPosition());

        if (here.is(Biomes.PLAINS)) {
            tell(player, Component.literal("Spawn is plains, as the seed intends (seed " + seed + ").")
                    .withStyle(ChatFormatting.GREEN));
            return;
        }
        tell(player, Component.literal("Spawn is " + name + ", NOT plains - worldgen has moved under "
                        + "seed " + seed + ". Coats are judged against plains; here is the nearest.")
                .withStyle(ChatFormatting.RED));
        Pair<BlockPos, Holder<Biome>> hit = level.findClosestBiome3d(
                b -> b.is(Biomes.PLAINS), player.blockPosition(), 6400, 32, 64);
        if (hit == null) {
            tell(player, Component.literal("No plains within 6400 blocks either. Pick a new seed: "
                    + "DebugTitleScreenButton.TEST_WORLD_SEED.").withStyle(ChatFormatting.RED));
            return;
        }
        offerTp(player, level, "Plains (what the fixed seed was chosen for)", hit.getFirst());
    }

    /**
     * The same reading, on a <b>dedicated</b> server, at startup.
     *
     * <p>This is what makes the seed checkable without a client at all:
     * {@code runServer} on a given {@code level-seed} prints the biome its
     * spawn landed in, so choosing the seed is a loop somebody can run rather
     * than a number somebody remembers. The client path above cannot do that
     * job - it needs a person to log in, which is the thing being avoided.
     */
    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        if (!ServerConfig.debugTools()) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        // 26.1.2: the world spawn is the overworld's respawn data, not a
        // getSharedSpawnPos() on the level - see wiki/api-notes.html.
        BlockPos spawn = level.getLevelData().getRespawnData().pos();
        Holder<Biome> biome = level.getBiome(spawn);
        String name = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("unnamed");
        // How much plains, not just "is the one block under you plains": a
        // spawn in a pocket wedged between a forest and a shore satisfies the
        // letter of it and is no use to stand a horse in. Sixteen points on a
        // 64-block ring is enough to tell a field from a pocket.
        int plains = 0;
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8.0;
            BlockPos at = spawn.offset((int) Math.round(Math.cos(angle) * 64), 0,
                    (int) Math.round(Math.sin(angle) * 64));
            if (level.getBiome(at).is(Biomes.PLAINS)) {
                plains++;
            }
        }
        HorseGenetics.LOGGER.info("[Debug] world seed {} - spawn {} is {}{}, and {}/16 of a "
                        + "64-block ring round it is plains",
                level.getSeed(), spawn, name,
                biome.is(Biomes.PLAINS) ? " (PLAINS - what the test world wants)" : " (NOT plains)",
                plains);
    }

    /**
     * {@code /testkit} lists the batches; {@code /testkit <n>} swaps the hotbar
     * for batch {@code n}; {@code /testkit village} and {@code /testkit forest}
     * hand out the two teleports. Gated on {@link ServerConfig#debugTools()} and
     * gamemaster permission. It exists so moving to the next batch costs one
     * line of chat rather than a rebuild and a fresh world.
     *
     * <h2>Why the teleports are commands now</h2>
     * They used to be handed to you on login, from {@link #onPlayerLogin},
     * together with batch 1. That works in singleplayer because the title-screen
     * button and the integrated server are the <b>same JVM</b>, so the button
     * can set {@code pendingHotbarFill} and the login hook can read it.
     *
     * <p>On a dedicated server they are two processes and that flag is never
     * set, so none of it fires: no hotbar, no teleports. The login path is left
     * in place because it is still the nicer route in a local test world - but
     * everything it does is reachable by command as well, which is what testing
     * a release build against a real server needs.
     */
    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        // Says, once per server start, which way the gate went. The lesson is
        // DebugAnnounce's: "/testkit does nothing" and "/testkit was never
        // registered" are opposite bugs that look identical from a chat box.
        HorseGenetics.LOGGER.info("[Debug] test kit commands enabled={} (config debug.tools)",
                ServerConfig.debugTools());
        if (!ServerConfig.debugTools()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("testkit")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> {
                    listBatches(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.argument("batch", IntegerArgumentType.integer(1, BATCHES.length))
                        .executes(c -> {
                            giveBatch(c.getSource().getPlayerOrException(),
                                    IntegerArgumentType.getInteger(c, "batch"));
                            return 1;
                        }))
                .then(Commands.literal("village")
                        .executes(c -> {
                            locatePlainsVillage(c.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("forest")
                        .executes(c -> {
                            locateDarkForest(c.getSource().getPlayerOrException());
                            return 1;
                        }))
                // The login report again, on demand. An hour into a session you
                // are a thousand blocks from spawn and the one line that said
                // where you started has scrolled away.
                .then(Commands.literal("seed")
                        .executes(c -> {
                            reportSpawnBiome(c.getSource().getPlayerOrException());
                            return 1;
                        }))
                // The horse dimension has a sky and no fixed time, so it runs
                // on the world's clock - which means "make it night to look at
                // a glow" is a thing you do from in there, and hunting for two
                // vanilla commands is the friction that stops it happening.
                .then(Commands.literal("night")
                        .executes(c -> setNight(c.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("day")
                        .executes(c -> setNight(c.getSource().getPlayerOrException(), false)))
                // Print the whole watch now. Eight hours is a very long time to
                // find out in the morning that nothing was being recorded, and
                // one line of chat before bed is the entire check.
                .then(Commands.literal("census")
                        .executes(c -> census(c.getSource().getPlayerOrException())))
                // The weather loci are conditional modifiers and nothing else,
                // so the whole test is "read it, change the sky, read it again"
                // - and hunting for the vanilla command is exactly the friction
                // that turns that into "check the weather genes tomorrow".
                .then(Commands.literal("rain")
                        .executes(c -> setWeather(c.getSource().getPlayerOrException(), "rain")))
                .then(Commands.literal("clear")
                        .executes(c -> setWeather(c.getSource().getPlayerOrException(), "clear"))));

        event.getDispatcher().register(Commands.literal("bond")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> reportBond(c.getSource().getPlayerOrException()))
                .then(Commands.argument("bond",
                                IntegerArgumentType.integer(0, HorseCareAttachment.MAX_BOND))
                        .executes(c -> setBond(c.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(c, "bond")))));
    }

    // ------------------------------------------------------------------
    // /bond - the one thing that made every bond tier untestable
    // ------------------------------------------------------------------

    /**
     * <b>{@code /bond <0-100>} sets the bond of the horse you are riding or
     * looking at; {@code /bond} alone reports it.</b>
     *
     * <p>It exists because every <i>real</i> source of bond is deliberately slow
     * - a daily cap of {@value HorseCareAttachment#DAILY_CAP}, a point per
     * {@link HorseCareAttachment#TICKS_PER_BOND_POINT} ticks of proximity - so
     * reaching tier 3 honestly is an hour of standing about, and exercising the
     * tiers meant either grinding or temporarily editing the thresholds and
     * rebuilding. Both are worse than a command, and the second changes the
     * thing under test.
     *
     * <p>It writes through {@link HorseCareHandler#syncCare}, so it is not a
     * back door: the client is told, and the bond <b>tier progress tasks are
     * credited</b>, exactly as if the horse had earned it. That matters - a
     * command that set the number without crediting would make the progress
     * surface lie, and the progress surface is one of the things being tested.
     */
    private static int reportBond(ServerPlayer player) {
        Horse horse = targetHorse(player);
        if (horse == null) {
            player.sendSystemMessage(Component.literal(
                    "Ride a horse, or look at one within 12 blocks.").withStyle(ChatFormatting.RED));
            return 0;
        }
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        player.sendSystemMessage(Component.literal(
                horse.getName().getString() + ": bond " + care.bond() + "/"
                        + HorseCareAttachment.MAX_BOND + ", behaviour tier "
                        + care.behaviourTier() + (care.inHerd() ? ", in a herd" : ""))
                .withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static int setBond(ServerPlayer player, int bond) {
        Horse horse = targetHorse(player);
        if (horse == null) {
            player.sendSystemMessage(Component.literal(
                    "Ride a horse, or look at one within 12 blocks.").withStyle(ChatFormatting.RED));
            return 0;
        }
        HorseCareAttachment before = horse.getData(ModAttachments.HORSE_CARE.get());
        HorseCareAttachment after = before.withBond(bond);
        horse.setData(ModAttachments.HORSE_CARE.get(), after);
        HorseCareHandler.syncCare(horse, after);
        player.sendSystemMessage(Component.literal(
                horse.getName().getString() + ": bond " + before.bond() + " -> " + after.bond()
                        + ", behaviour tier " + before.behaviourTier() + " -> " + after.behaviourTier())
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    /**
     * The horse this command means: the one being ridden, else the nearest one
     * the player is actually looking at. Riding wins because the tier that is
     * hardest to reach honestly (bareback steering, following) is the one you
     * are most likely to be sitting on when you want to change it.
     */
    private static @Nullable Horse targetHorse(ServerPlayer player) {
        if (player.getVehicle() instanceof Horse ridden) {
            return ridden;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Horse best = null;
        double bestDot = 0.97;   // roughly "in the crosshair", not "somewhere in front"
        for (Horse horse : player.level().getEntitiesOfClass(Horse.class,
                player.getBoundingBox().inflate(TARGET_RANGE))) {
            Vec3 to = horse.getBoundingBox().getCenter().subtract(eye);
            double len = to.length();
            if (len < 1.0E-4) {
                continue;
            }
            double dot = to.scale(1.0 / len).dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = horse;
            }
        }
        return best;
    }

    /** How far {@code /bond} will look for a horse. */
    private static final double TARGET_RANGE = 12.0;

    /**
     * <b>The test kit - aimed at what is waiting on {@code wiki/verification.html}
     * right now, and meant to be re-aimed every time that list changes</b>
     * (owner's standing request, 2026-09-10: "each time you do some kind of
     * debug, change what's in my inventory ... to make it as easy as possible to
     * test each new part"). Preset eggs are the lever: a horse built for one
     * test, named for it, one right-click from existing.
     *
     * <p><b>A batch that breeds carries both sexes.</b>
     * {@code HorseBreedingHandler} cancels same-sex pairings, so a slot that
     * says "breed these" and hands out mares is an instruction nothing can
     * follow - which is exactly what batch 3's spontaneous-breeding test was
     * from the day it was written ("spawn FOUR in a wide fenced field, come
     * back and COUNT" - of four mares), found by the owner playing it on
     * 2026-09-12. <b>A test that cannot pass is worse than a missing one</b>,
     * because it sits on the checklist looking like coverage.
     *
     * <p><b>One hotbar at a time.</b> A kit that filled all 36 slots, with a
     * 30-line legend, was "SUPER overwhelming" (owner, 2026-09-11) - so the
     * checklist is cut into themed batches of at most nine, the first handed out
     * on login and the rest a {@code /testkit n} away. Every batch that tames
     * carries a <b>stick</b> (it tames on the spot - {@code HorseInteractionHandler})
     * and every batch that breeds carries <b>golden carrots</b>.
     *
     * <p>A batch is deleted once the owner has worked through it, and the rest
     * move up - the first batch is always the next thing to test. The breeds,
     * size and rebuilt-intake batches went that way on 2026-09-11. An egg whose breed or gene
     * is not loaded is simply left out.
     */
    /**
     * <b>Seven batches: fertility outside the yard, this evening's two, then the four the yard left.</b>
     *
     * <p>Batch 1 is the part of fertility the yard cannot do. Rows O-T
     * ({@code DebugYardFertility}) are one pen per breeding scenario, so the
     * paddock batch that stood here for an hour went the way the item batches
     * did. What is left needs the overworld: a birth while the mare's chunk is
     * unloaded, and a save and reload mid-pregnancy - leaving the horse
     * dimension tears the yard down, so neither can happen in it.
     *
     * <p>Batches 2 and 3 are 2026-09-13 evening's unplayed work
     * ({@code wiki/verification.html} 0-DT and 0-R): the ender whistle and band
     * life, then the Dhampir's loci and the day loci. They are eggs and items
     * rather than pens because every one of those tests is about the horse
     * reacting to <i>you</i> - binding, calling across dimensions, biting the
     * nearest thing that is not you - and a pen cannot stand in for the player.
     *
     * <p><b>The four below were four batches, down from six, because the yard took two of them.</b>
     *
     * <p>What were batches 3 and 4 were the item layer in a hotbar - pen signs, stall
     * signs, tickets, hay, fences, bottles, the research shelf - and every one
     * of those is now standing in the yard's rows B, C and D with a chest
     * beside it. A hotbar of items whose legend reads "build a stall, then fill
     * it solid" is a batch that gets taken and put away again, which is what
     * happened to both of them for three days running.
     *
     * <p>What is left here is the residue: the things that need a
     * <b>rider</b>, the one that needs to be <b>fed by hand</b>, and the intake
     * genes that are just eggs to look at. If a batch can be replaced by a pen,
     * it should be - see {@code wiki/horse-dimension.html}.
     */
    private static final String[] BATCHES = {
            "NEW: fertility OUTSIDE the yard - birth while away, reloads (rows O-T do the rest)",
            "NEW: the ender whistle, and wild bands (0-DT)",
            "NEW: the Dhampir's loci - white, sun, blood - and the day loci (0-R, 0-DT)",
            "START THE NIGHT - walk the yard, check the census, then leave it alone (0-CX)",
            "Ridden: ender echo and ocean-born, plus the guardian, which needs YOU hurt",
            "The sheep spawner - the one thing in 0-BY the yard cannot hand you",
            "Intake: the rest (0-BZ)",
    };

    /**
     * Midnight with the cycle held, or noon with it running again.
     *
     * <p>Both levels are set, not just the one the player is standing in: the
     * glow tests are done in the horse dimension and judged against how the
     * same horse looked outside, and a clock that disagreed between the two
     * would make that comparison a lie.
     *
     * <h2>The horse dimension had no night at all until 2026-09-12</h2>
     * 26.1.2 made time <b>per dimension</b>: a {@code dimension_type} names a
     * {@code default_clock} and one that names none does not have a time of day
     * to set. {@code debug_pens} named none, so {@code /time set midnight} in
     * the overworld left it in permanent daylight and a glowing horse could not
     * be judged in the one place built for looking at horses (owner: "day and
     * night are different in different dimensions and the horse dimension
     * doesn't have a night"). It runs on {@code minecraft:overworld}'s clock
     * now, so this command reaches it.
     *
     * <p>It still does not make the dimension <b>dark</b> - the corridor and
     * the yard are lit by glowstone by design, and block light does not care
     * what time it is. That is what the yard's glow room is for; this is the
     * other half of the same problem.
     */
    private static int setNight(ServerPlayer player, boolean night) {
        var server = player.level().getServer();
        if (server == null) {
            return 0;
        }
        // Through the vanilla commands rather than the clock API directly.
        // 26.1.2 replaced day-time with a ServerClockManager of WorldClock
        // holders and time markers, and "set it to midnight" is several lookups
        // deep in an API this does not otherwise touch - where /time already
        // does exactly the right thing and will keep doing it across versions.
        var source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, night ? "time set midnight" : "time set noon");
        server.getCommands().performPrefixedCommand(source,
                "gamerule doDaylightCycle " + (night ? "false" : "true"));
        // SAY WHETHER IT WORKED, in the dimension the player is actually standing
        // in. The owner reported on 2026-09-13 that "night isn't working in the
        // horse dimension, it still looks like day", and from a chat box there
        // is no way to tell the three candidates apart: the clock did not move,
        // the clock moved but this dimension does not follow it, or both are
        // fine and the yard's glowstone is simply doing its job. isBrightOutside
        // is the same test every gene condition in this mod uses, so an answer
        // here is an answer for them too.
        // READ IT THREE TICKS LATER, NOT NOW. This reported a false alarm for
        // most of 2026-09-13: the command said "this dimension now reads DAY"
        // immediately after setting midnight, and the owner reasonably took
        // that at face value ("night isn't working") - twice, once when it was
        // a real bug and once when it was not. The cause is ordering, not the
        // clock. Level.updateSkyBrightness() runs during the level's own tick,
        // so a read taken inside a command - which executes before that - gets
        // the PREVIOUS tick's value, every time. A diagnostic that cries wolf
        // is worse than none: it cost a whole second investigation into a
        // dimension that was already fixed.
        afterTicks(server, 3, () -> {
            boolean bright = player.level().isBrightOutside();
            tell(player, Component.literal(night
                            ? "Night set. This dimension now reads " + (bright ? "DAY" : "NIGHT") + "."
                            : "Day set. This dimension now reads " + (bright ? "DAY" : "NIGHT") + ".")
                    .withStyle(night == bright ? ChatFormatting.RED : ChatFormatting.GOLD));
            if (night && bright) {
                tell(player, Component.literal("That is the bug: the clock was set and this dimension "
                                + "did not follow it. Worth saying in the report - the overworld "
                                + "clock is shared by default_clock, so this is a sync problem "
                                + "rather than a missing clock.")
                        .withStyle(ChatFormatting.RED));
            } else if (night) {
                tell(player, Component.literal("The sky is dark. If it still LOOKS like day where you "
                                + "are standing, that is the glowstone - the corridor and the yard are "
                                + "lit by design. Use the yard's GLOW ROOM, which has a lid.")
                        .withStyle(ChatFormatting.GOLD));
            }
        });
        ActionTrace.log("testkit", (night ? "night" : "day") + " set by " + player.getGameProfile().name());
        return 1;
    }

    /**
     * Run {@code task} {@code ticks} server ticks from now.
     *
     * <p>{@code server.execute} queues for the next task drain, and a task that
     * queues another lands a tick after that - so chaining is the whole
     * mechanism, and there is no scheduler here to add one to.
     */
    private static void afterTicks(net.minecraft.server.MinecraftServer server, int ticks,
                                   Runnable task) {
        if (ticks <= 0) {
            task.run();
            return;
        }
        server.execute(() -> afterTicks(server, ticks - 1, task));
    }

    /**
     * Where the horses are. The kit used to carry them; the test yard stocks
     * them now, which is better - a pen with the right floor and the right
     * animals already standing in it is a test somebody can start, and a
     * hotbar of eggs is a test somebody has to set up first.
     */
    private static void tellYard(ServerPlayer player) {
        tell(player, Component.literal("The TEST YARD is most of the mod now: hay portal, then "
                        + "right off the arrival road, and it runs in rows either side of "
                        + "the walkway all the way to the back wall.")
                .withStyle(ChatFormatting.GOLD));
        tell(player, Component.literal("  A  2 dryad pens | the ward's spawner    "
                        + "B  tack room | horseman + cowboy")
                .withStyle(ChatFormatting.WHITE));
        tell(player, Component.literal("  C  ticket stalls | carrot bench         "
                        + "D  dairy + shears | egg layer x8")
                .withStyle(ChatFormatting.WHITE));
        tell(player, Component.literal("  E  crackle, the pool | food preference  "
                        + "F  starburst, F8 | the 3 stat pens")
                .withStyle(ChatFormatting.WHITE));
        tell(player, Component.literal("  I  (west empty) | eyesight, lit and dark "
                        + "J  guardian | the infirmary")
                .withStyle(ChatFormatting.WHITE));
        tell(player, Component.literal("  K  the deathbed | ender echo            "
                        + "L, M, N  the ten splice-carrot pairs")
                .withStyle(ChatFormatting.WHITE));
        // G AND H ARE NOT MISSING FROM THIS LIST, THEY ARE GONE - the growing
        // row's last two pens moved into A's west block when oak/birch and bone
        // meal were confirmed, and H had been depth zero since the retinue pen
        // was deleted. The letters are deliberately NOT re-lettered: every sign
        // in the yard, every open question on wiki/verification.html and a
        // year of session notes say "row J west", and renaming rows to close a
        // gap would invalidate all of it to save two characters.
        tell(player, Component.literal("Rows A, D, I and K run themselves - walk in, then "
                        + "leave it alone. B and C need your hands, and every chest in them is "
                        + "labelled. (There is no G or H: both were emptied and deleted.)")
                .withStyle(ChatFormatting.GOLD));
    }

    /**
     * <b>Print the watch now, and say in chat whether there is one.</b>
     *
     * <p>The whole overnight design rests on {@link DebugWorldWatch} having
     * been started, which happens when the yard is built, which happens when
     * you walk through the hay portal. Getting that wrong costs a night and is
     * invisible until the morning - so this is the check, and it is one
     * command.
     */
    /**
     * <b>Set the weather, then say what the dimension actually reads.</b>
     *
     * <p>Built the same way {@code /testkit night} was and for the same reason:
     * "I don't think weather works in the horse realm" has more than one cause
     * and a chat box cannot separate them. The reading is taken a few ticks
     * later, which is the lesson night taught - {@code rainLevel} moves during
     * the level tick, so asking in the same tick as the command returns the
     * value from before it.
     */
    private static int setWeather(ServerPlayer player, String what) {
        var server = player.level().getServer();
        if (server == null) {
            return 0;
        }
        var source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source,
                "rain".equals(what) ? "weather rain" : "weather clear");
        tell(player, Component.literal("Weather set to " + what
                        + " - run /testkit census in a moment; the header says what this "
                        + "dimension reads, and the weather pens print their attribute.")
                .withStyle(ChatFormatting.GOLD));
        ActionTrace.log("testkit", "weather " + what + " by " + player.getGameProfile().name());
        return 1;
    }

    private static int census(ServerPlayer player) {
        ServerLevel debug = player.level().getServer() == null ? null
                : player.level().getServer().getLevel(DebugPenManager.DEBUG_LEVEL);
        if (debug == null) {
            tell(player, Component.literal("No horse dimension on this server.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        DebugWorldWatch.censusNow(debug);
        tell(player, Component.literal("Census written to the log - grep [watch]. If it says "
                        + "nothing is being watched, go through the hay portal first: the yard "
                        + "registers its pens as it builds them.")
                .withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static void listBatches(ServerPlayer player) {
        tell(player, Component.literal("Test kit batches - click one, then press Enter:")
                .withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < BATCHES.length; i++) {
            tell(player, command("/testkit " + (i + 1), BATCHES[i]));
        }
        // Two of the first three batches can only be judged after dark, and
        // hunting for the command is the sort of friction that turns "check the
        // glow" into "check the glow tomorrow".
        tell(player, command("/testkit night", "night, clock held - then use the yard's GLOW ROOM"));
        tell(player, command("/testkit day", "and back to day"));
        tell(player, command("/testkit census", "print every watched pen to the log now - the "
                + "check to run before leaving it overnight"));
    }

    /** Empty the hotbar and fill it with batch {@code n} (1-based), then say what each slot is for. */
    private static void giveBatch(ServerPlayer player, int n) {
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            inv.setItem(slot, ItemStack.EMPTY);
        }
        List<String> legend = new ArrayList<>();
        switch (n) {
            case 1 -> {
                // FERTILITY AND GESTATION. With debug.tools on, the reproductive
                // day is one minute: a heat is a minute, a pregnancy is a minute,
                // a whole cycle is two. The clock steps a mare along rather than
                // making anyone wait for it.
                put(inv, legend, 0, many(preset(player, "Test: fertility mare", Sex.FEMALE, false), 2),
                        "two plain mares - open the info screen: Body now has a Breeding line "
                                + "(in heat / not in heat, and how long)");
                put(inv, legend, 1, preset(player, "Test: fertility stallion", Sex.MALE, false),
                        "a plain stallion - he and a mare IN HEAT should walk towards each other");
                put(inv, legend, 2, new ItemStack(Items.STICK), "stick - tame all of them");
                put(inv, legend, 3, new ItemStack(Items.CLOCK),
                        "clock - right-click an ADULT MARE: into peak heat; once pregnant, due now; "
                                + "just foaled, straight to foal heat");
                put(inv, legend, 4, new ItemStack(ModItems.STABILIZER_CARROT.get(), 8),
                        "stabilizer carrots - feed one to the mare ANY time: no hearts, it just waits on "
                                + "her. Then golden-carrot the pair while she is in heat: NO instant foal, "
                                + "she is pregnant (or it didn't take, and the carrot is still on her)");
                put(inv, legend, 5, many(new ItemStack(ModItems.EMPTY_SEED_JAR.get()), 3),
                        "empty seed jars - fill from the stallion (golden carrot him first), use on a "
                                + "mare IN HEAT: pregnant. Out of heat: refused, jar kept");
                put(inv, legend, 6, new ItemStack(Items.GOLDEN_CARROT, 16),
                        "golden carrots - plain breeding is still an INSTANT foal. Offer one to a "
                                + "pregnant mare: refused");
                put(inv, legend, 7, preset(player, "Test: MET carrier mare", Sex.FEMALE, false,
                                "horsegenetics.met=met/N"),
                        "MET carrier mare - breed her to the MET stallion by jar or carrots");
                put(inv, legend, 8, preset(player, "Test: MET carrier stallion", Sex.MALE, false,
                                "horsegenetics.met=met/N"),
                        "MET carrier stallion - about one pregnancy in four is LOST EARLY, with the "
                                + "miscarriage line");
                tell(player, Component.literal("Do these in the OVERWORLD: the yard's rows O-T cover "
                                + "everything else. A pregnancy runs about a minute. Get a mare pregnant, "
                                + "ride far enough that her chunk unloads for a minute, come back: she foals "
                                + "within two seconds. Then quit to title mid-pregnancy and reload.")
                        .withStyle(ChatFormatting.GOLD));
            }
            case 2 -> {
                // THE ENDER WHISTLE, and the one band-life test that needs a
                // hand on the horse. The whistle's far call is the test that can
                // fail without a sound - a portal ticket that never loads the
                // chunk reads exactly like "nothing answered" - so the saddle is
                // here to get a thousand blocks away, and the obsidian to prove
                // the cross-dimension half.
                put(inv, legend, 0, new ItemStack(ModItems.ENDER_WHISTLE.get()),
                        "ENDER WHISTLE - right-click one of YOUR tamed horses to bind it for good. "
                                + "Then walk off and right-click the air to call it");
                put(inv, legend, 1, new ItemStack(ModItems.ENDER_WHISTLE.get()),
                        "a second whistle - bind it to a horse, put it in a chest, kill that horse, "
                                + "take the whistle out: it should crumble to dust");
                put(inv, legend, 2, new ItemStack(Items.STICK),
                        "stick - tame the eggs in slot 4. Also try binding to an UNTAMED horse: refused");
                put(inv, legend, 3, many(preset(player, "Test: whistle horse", Sex.FEMALE, false, PALOMINO), 3),
                        "three plain horses to bind, call and (one of them) kill");
                put(inv, legend, 4, new ItemStack(Items.SADDLE),
                        "saddle - ride a THOUSAND blocks out so the horse's chunk unloads, then call: "
                                + "\"echoes a long way off\" and the horse within five seconds");
                put(inv, legend, 5, new ItemStack(Items.OBSIDIAN, 10),
                        "obsidian - a Nether portal. Call the horse across it both ways, then open its "
                                + "info screen: same name, coat and genes");
                put(inv, legend, 6, new ItemStack(Items.FLINT_AND_STEEL), "flint and steel - to light it");
                put(inv, legend, 7, new ItemStack(Items.IRON_SWORD),
                        "sword - for the crumble test. Kill a bound horse while HOLDING its whistle too");
                put(inv, legend, 8, new ItemStack(Items.LEAD),
                        "lead - BAND LIFE: lead a WILD mare twenty blocks from her band and let go. "
                                + "Her stallion should come round and push her back");
                tell(player, Component.literal("Band life is mostly watching wild horses in the "
                                + "overworld. The one that matters most: kill a family band's "
                                + "stallion and watch two minutes - the mares keep following ONE "
                                + "mare instead of scattering. Open a wild mare's info screen for "
                                + "the new Social section.").withStyle(ChatFormatting.GOLD));
            }
            case 3 -> {
                // THE DHAMPIR, TAKEN APART. Each locus gets its own egg, so a
                // behaviour can be pinned on the allele that owns it: the white
                // one is everything at once, and the two singles are the check
                // that burning and biting really did come apart. The hunt's
                // order (mob, then you, then a tamed pet) needs a cow and a
                // wolf in reach, which is what slots 4-6 are.
                put(inv, legend, 0, preset(player, "Test: white dhampir", Sex.FEMALE, false,
                                "horsegenetics.magic_white=Wm/Wm", "horsegenetics.sun_sensitivity=Sun/Sun",
                                "horsegenetics.diet=Dbld/Dbld", "horsegenetics.magic_health=Vmp/Vmp",
                                "horsegenetics.magic_speed=Vmp/Vmp", "horsegenetics.magic_jump=Vmp/Vmp"),
                        "the whole animal - burns and hides at dawn, turns all food away, hunts after dark");
                put(inv, legend, 1, preset(player, "Test: blood diet only", Sex.FEMALE, false,
                                "horsegenetics.diet=Dbld/Dbld"),
                        "blood diet ONLY - hunts at any hour, sunlight included, and never burns");
                put(inv, legend, 2, preset(player, "Test: sun only", Sex.FEMALE, false,
                                "horsegenetics.sun_sensitivity=Sun/Sun"),
                        "sun sensitivity ONLY - burns and hides at dawn, but eats hay like any horse");
                put(inv, legend, 3, new ItemStack(Items.COW_SPAWN_EGG, 4),
                        "cows - hurt the white one after dark beside a cow and yourself: it bites the COW. "
                                + "Take the cow away: now you (survival mode)");
                put(inv, legend, 4, new ItemStack(Items.WOLF_SPAWN_EGG, 2),
                        "wolves - tame one, go creative: the pet is bitten only once nothing else is near");
                put(inv, legend, 5, new ItemStack(Items.BONE, 8), "bones - to tame the wolf");
                put(inv, legend, 6, new ItemStack(Items.STICK),
                        "stick - tame the day-shy horse so you can try to ride it");
                put(inv, legend, 7, preset(player, "Test: day-shy", Sex.FEMALE, false,
                                "horsegenetics.magic_day_temper=Flp/Flp"),
                        "day temper Flp/Flp - runs from you at noon, lets you ride it at midnight");
                put(inv, legend, 8, preset(player, "Test: day watcher", Sex.FEMALE, false,
                                "horsegenetics.magic_day_watch=Wbh/Wbh"),
                        "day watch Wbh/Wbh - stands behind you by day, stops when you turn round");
                tell(player, Component.literal("It must NEVER bite another horse or a zombie. "
                                + "One bite per animal per day.").withStyle(ChatFormatting.GOLD));
                tell(player, command("/testkit night", "night - for the hunt"));
                tell(player, command("/testkit day", "day - for the burning and the day loci"));
            }
            case 4 -> {
                // THE NIGHT SHIFT. Six of the yard's pens now run on a clock
                // and write their own readings (DebugWorldWatch), so this batch
                // is not a list of things to do - it is the short list of
                // things that have to happen ONCE before walking away, in
                // order, because getting any of them wrong costs a whole night
                // and none of them announces itself.
                put(inv, legend, 0, new ItemStack(ModItems.CUSTOM_HORSE_SPAWN_EGG.get()),
                        "custom spawn egg - open it and press MAKE EGG (0-CR). It was being "
                                + "clicked by a widget on top of it; the fix is untested");
                put(inv, legend, 1, new ItemStack(Items.STICK),
                        "stick - the yard's horses all come TAMED now, this is for anything else");
                put(inv, legend, 2, new ItemStack(Items.GOLDEN_CARROT, 16),
                        "golden carrots - rows L, M and N. The BREEDING half is confirmed (8 "
                                + "carrots, 8 foals, 2026-09-13). What is left: breed a pair, then "
                                + "OPEN THE FOAL and check the substituted locus is actually there");
                put(inv, legend, 3, new ItemStack(Items.CLOCK),
                        "clock - or /testkit night, which the dimension now honours");
                put(inv, legend, 4, new ItemStack(Items.SADDLE),
                        "saddle - the ridden tests are batch 5");
                // THE CONTROL FOR GAP 218, and the reason it is dark oak and not
                // oak: the oak, birch, mushroom and bone-meal dryad pens all
                // completed and are deleted, and dark oak planted saplings for a
                // whole day without ever producing a log. Two explanations fit
                // that - the clustering search never closes a 2x2, or a 2x2
                // closes and the pen refuses to grow it - and hand-placing four
                // saplings in a square separates them in ten seconds. If a
                // hand-placed square grows, the gene's search is the bug; if it
                // does not, the pen is.
                put(inv, legend, 5, new ItemStack(Items.DARK_OAK_SAPLING, 8),
                        "dark oak saplings - DRYAD DARK, row A WEST (row G is gone): plant FOUR "
                                + "in a 2x2 yourself. Grows = the gene's clustering is the bug; "
                                + "refuses = the pen is");
                tellYard(player);
                tell(player, Component.literal("BEFORE YOU LEAVE IT RUNNING - four things, in this "
                                + "order:").withStyle(ChatFormatting.GOLD));
                tell(player, Component.literal("  1. Go through the hay portal. The yard only "
                                + "exists once you do, and so does the watch.")
                        .withStyle(ChatFormatting.WHITE));
                tell(player, command("/testkit census", "2. run this - it prints every pen to the "
                        + "log. If it says nothing is being watched, the night is not running"));
                tell(player, Component.literal("  3. Stay logged in and OUT of the pause menu - "
                                + "singleplayer stops the server on Esc. The yard's chunks are "
                                + "force-loaded, so you can stand anywhere in the dimension.")
                        .withStyle(ChatFormatting.WHITE));
                tell(player, Component.literal("  4. Do NOT leave the horse dimension: that tears "
                                + "the plot down and the night with it.")
                        .withStyle(ChatFormatting.RED));
            }
            case 5 -> {
                // EVERY ONE OF THESE IS A PEN NOW, and all three come saddled
                // in it - so this batch is no longer "here are three horses",
                // it is the handful of things a rider needs that a pen cannot
                // hold: a sword for the arena, a clock for timing, and the
                // spare saddle for when one gets lost.
                put(inv, legend, 0, new ItemStack(Items.STICK), "stick");
                put(inv, legend, 1, new ItemStack(Items.SADDLE),
                        "spare saddle - the yard's ridden pens come saddled already");
                put(inv, legend, 2, new ItemStack(Items.IRON_SWORD),
                        "sword - the DEATHBED pen, row K west: six horses whose whole gene is "
                                + "what happens when they die. Three drop something, three leave "
                                + "something behind");
                put(inv, legend, 3, new ItemStack(Items.CLOCK),
                        "clock - or /testkit night, which the dimension honours now");
                put(inv, legend, 4, new ItemStack(Items.GLASS_BOTTLE, 16),
                        "bottles - the DAIRY, row D west: the mare is already hurt and the foal "
                                + "is already a foal, so all three refusals are one walk");
                put(inv, legend, 5, new ItemStack(Items.SHEARS),
                        "shears - two horses in the same pen are named SHEAR ME");
                put(inv, legend, 6, new ItemStack(Items.APPLE, 16),
                        "apples - FOOD PREFERENCE, row E east: four horses, four different loves, "
                                + "one pen. Feed all four the same apple and exactly one should react");
                tell(player, Component.literal("Row J west is the GUARDIAN, and it is the only "
                                + "horse in the yard that arrives UNTAMED - on purpose, because "
                                + "its gene fires on the owner being hurt and a horse with no "
                                + "owner matches nobody. Tame it, stand next to it, then let a "
                                + "zombie from the chest hit YOU. Gladiator is confirmed and gone.")
                        .withStyle(ChatFormatting.GOLD));
                tell(player, Component.literal("Rows B, C and D are the ITEM LAYER - tack room, "
                                + "horseman and cowboy at their own workstations, the ticket "
                                + "stalls, the carrot bench. Every chest is labelled; nothing "
                                + "needs fetching.")
                        .withStyle(ChatFormatting.GOLD));
            }
            case 6 -> {
                // Molten hooves left this batch on 2026-09-13: its four alleles
                // are four stalls in the yard now, side by side, which is the
                // only arrangement that can answer "does this one differ from
                // the last" - a question nobody can settle from memory two
                // spawns apart. What is left here is the one test in 0-BY that
                // needs a person: the spawner has to be FED.
                put(inv, legend, 0, new ItemStack(Items.STICK), "stick - tame it first");
                put(inv, legend, 1, preset(player, "Test: sheep spawner", Sex.FEMALE, false,
                        "horsegenetics.spawner=Shp/Shp"), "tame, feed wheat - every sheep the SAME colour");
                put(inv, legend, 2, new ItemStack(Items.WHEAT, 64), "wheat - the spawner's meals");
                put(inv, legend, 3, new ItemStack(Items.SADDLE),
                        "saddle - prints follow a ridden horse too; the molten stalls are in the yard");
            }
            case 7 -> {
                String[] rest = {"tidewave", "inkcoil", "opal_fire", "beadscale", "scuted",
                        "sporefall", "wishstar", "datarain", "foamed"};
                for (int i = 0; i < rest.length; i++) {
                    intake(player, inv, legend, i, rest[i], null, i == 0 ? "does each look like its icon?" : null);
                }
            }
            default -> {
                return;
            }
        }

        tell(player, Component.literal("Test kit " + n + "/" + BATCHES.length + ": " + BATCHES[n - 1])
                .withStyle(ChatFormatting.GOLD));
        for (String line : legend) {
            tell(player, Component.literal(line).withStyle(ChatFormatting.WHITE));
        }
        if (n < BATCHES.length) {
            tell(player, command("/testkit " + (n + 1), "next: " + BATCHES[n]));
        }
    }

    /**
     * An intake gene's egg, homozygous for {@code token} - or for the gene's
     * first-listed allele, the form its icon shows, when {@code token} is null.
     */
    private static void intake(ServerPlayer player, Inventory inv, List<String> legend, int slot,
                               String id, @Nullable String token, @Nullable String look, String... base) {
        Gene gene = Genes.byKeyOrNull("horsegenetics." + id);
        if (gene == null) {
            HorseGenetics.LOGGER.warn("Test kit: no gene {}", id);
            return;
        }
        String t = token != null ? token : gene.alleles().get(0).token();
        String label = gene.name() + " (" + t + "/" + t + ")";
        String[] pairs = new String[base.length + 1];
        pairs[0] = gene.key() + "=" + t + "/" + t;
        System.arraycopy(base, 0, pairs, 1, base.length);
        put(inv, legend, slot, preset(player, "Intake: " + label, Sex.FEMALE, false, pairs),
                look == null ? null : label + " - " + look);
    }

    /**
     * A base coat for a dark marking. A preset egg names only the loci it is
     * given, and every other locus sits at its default - which the owner saw as a
     * black horse, where a black barred wing and a nightbell foxglove simply
     * vanish (2026-09-11). Chestnut with one cream copy is pale gold, which a
     * black mark and a white one both show on.
     */
    private static final String[] PALOMINO = {"horsegenetics.extension=e/e", "horsegenetics.matp=Cr/N"};

    /** A chat line that puts {@code cmd} in the chat box when clicked. */
    private static Component command(String cmd, String text) {
        return Component.literal("[" + cmd + "] " + text).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.SuggestCommand(cmd))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click, then press Enter"))));
    }

    /**
     * A breed egg, or {@code null} (so the slot stays empty) when this run has
     * no such breed - {@link Breeds#get} answers Feral Mixed for an unknown id,
     * and the kit's {@code kit_*} drop-ins only exist in a dev run whose
     * {@code run/phc/breeds/} was prepared for them.
     */
    private static @Nullable ItemStack breedEgg(String id) {
        Breed breed = Breeds.get(id);
        if (breed == null || breed == Breeds.FERAL_MIXED) {
            HorseGenetics.LOGGER.warn("Test kit: no breed '{}' - its egg skipped", id);
            return null;
        }
        return BreedSpawnEggItem.of(breed);
    }

    /** Put a stack in a slot and, if it has a purpose worth saying, add it to the legend. */
    private static void put(Inventory inv, List<String> legend, int slot, @Nullable ItemStack stack,
                            @Nullable String purpose) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.setCount(Math.min(stack.getCount(), stack.getMaxStackSize()));
        inv.setItem(slot, stack);
        if (purpose != null) {
            legend.add("slot " + (slot + 1) + ": " + purpose);
        }
    }

    /**
     * A preset egg for a horse built from {@code key=a/b} pairs, named for its
     * test. A key or allele this build does not have is logged and skipped
     * rather than failing the login - the kit outliving a gene rename should
     * cost one egg, not the whole inventory.
     */
    /**
     * {@code n} of a stack, or {@code null} if it was not built.
     *
     * <p>A test that reads "spawn six and line them up" needs six eggs in the
     * slot, not one and a trip to the creative menu. They stack because a
     * preset egg's components are identical from egg to egg - and the horses
     * are not, because the stored epigenome is deliberately empty and every use
     * rolls a fresh one.
     */
    private static @Nullable ItemStack many(@Nullable ItemStack stack, int n) {
        if (stack != null) {
            stack.setCount(n);
        }
        return stack;
    }

    private static @Nullable ItemStack preset(ServerPlayer player, String label, Sex sex, boolean baby,
                                              String... pairs) {
        List<AllelePair> built = new ArrayList<>();
        for (String spec : pairs) {
            String[] kv = spec.split("=");
            String[] tokens = kv[1].split("/");
            Gene gene = Genes.byKeyOrNull(kv[0]);
            Allele a = gene == null ? null : allele(gene, tokens[0]);
            Allele b = gene == null ? null : allele(gene, tokens[1]);
            if (a == null || b == null) {
                HorseGenetics.LOGGER.warn("Test kit: no {} - '{}' skipped", spec, label);
                return null;
            }
            built.add(new AllelePair(a, b));
        }
        Genome genome = new Genome(Genotype.of(built).withSex(sex),
                Epigenome.random(new NeoRng(player.getRandom())));
        // An EMPTY epigenome, so HorseEggSpawner.spawnPreset rolls a fresh one at
        // every use. A stored one made every horse from a creative egg the same
        // horse - the owner spawned a stack of trillium and taper flame to see
        // posU move them, and every mark stood in one place (2026-09-11).
        ItemStack egg = PresetHorseSpawnEggItem.of(new StoredGenome(genome.genotypeCode(),
                "", player.getUUID(), player.getGameProfile().name(), ""), baby);
        egg.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        return egg;
    }

    private static @Nullable Allele allele(Gene gene, String token) {
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Say where the nearest plains village is, and hand over the {@code /tp}
     * that goes there. <b>Does not move the player.</b>
     *
     * <p>Teleporting them on login was the first version of this and it was the
     * wrong shape: it threw away spawn - the one place you can reliably get back
     * to - to put you somewhere you had not asked to be, before you had seen
     * anything of the world. A line of chat with a command in it costs one click
     * when you want it and nothing at all when you do not, and it leaves the
     * test world a normal world.
     *
     * <p>The search is the same one {@code /locate structure minecraft:village_plains}
     * runs, aimed at that one structure rather than the
     * {@code #on_plains_village_maps} tag, so what is reported is what the
     * command would report. Plains only, because that is the only village kind
     * the {@link com.example.horsegenetics.neoforge.entity.Cowboy cowboy} spawns
     * with ({@code wiki/villagers.html}) - a savanna or desert hit is a wasted
     * trip.
     *
     * <p>It is a synchronous structure search, which generates chunks and can
     * take a moment on a fresh world. That is acceptable here and nowhere else:
     * this handler only ever fires once, in a throwaway dev world, on the one
     * login that created it.
     */
    private static void locatePlainsVillage(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos found = nearestPlainsVillage(level, player.blockPosition());
        if (found == null) {
            HorseGenetics.LOGGER.warn("Test world: no plains village within {} chunks of spawn", VILLAGE_SEARCH_CHUNKS);
            tell(player, Component.literal("No plains village within "
                            + (VILLAGE_SEARCH_CHUNKS * 16) + " blocks of spawn.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        offerTp(player, level, "Plains village", found);
    }

    /**
     * Where the nearest <b>dark forest</b> is - the one biome the kit's drop-in
     * breeds and the moved Friesian live in, and no shipped breed's default.
     * The same search {@code /locate biome minecraft:dark_forest} runs, with its
     * radius and sampling steps; a biome search samples the biome source and
     * generates nothing, so it is cheaper than the village one.
     */
    private static void locateDarkForest(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Pair<BlockPos, Holder<Biome>> hit = level.findClosestBiome3d(
                biome -> biome.is(Biomes.DARK_FOREST), player.blockPosition(), 6400, 32, 64);
        if (hit == null) {
            tell(player, Component.literal("No dark forest within 6400 blocks of spawn.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        offerTp(player, level, "Dark forest (Friesian herds by day, cremellos at night)", hit.getFirst());
    }

    /** One chat line: a place, and a {@code /tp} to above it that one click puts in the chat box. */
    private static void offerTp(ServerPlayer player, ServerLevel level, String what, BlockPos found) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found);
        int altitude = Math.max(TP_ALTITUDE, surface.getY() + 2);
        String command = "/tp @s " + surface.getX() + " " + altitude + " " + surface.getZ();
        HorseGenetics.LOGGER.info("Test world: {} at {}", what, surface);

        tell(player, Component.literal(what + " at "
                        + surface.getX() + ", " + surface.getY() + ", " + surface.getZ()
                        + " (tp drops you in at y=" + altitude + "). ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("[" + command + "]")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.AQUA)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.SuggestCommand(command))
                                .withHoverEvent(new HoverEvent.ShowText(
                                        Component.literal("Click to put this in the chat box"))))));
    }

    /**
     * The nearest {@code minecraft:village_plains}, or {@code null}.
     *
     * <p>{@code ServerLevel.findNearestMapStructure} only takes a structure
     * <i>tag</i>, so this goes to the generator directly with a one-element
     * {@link HolderSet} - which is exactly what {@code /locate structure} does
     * with a single-structure argument.
     */
    private static @Nullable BlockPos nearestPlainsVillage(ServerLevel level, BlockPos from) {
        Holder<Structure> village = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(BuiltinStructures.VILLAGE_PLAINS);
        Pair<BlockPos, Holder<Structure>> hit = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(village), from, VILLAGE_SEARCH_CHUNKS, false);
        return hit == null ? null : hit.getFirst();
    }

    private static void tell(ServerPlayer player, Component message) {
        player.sendSystemMessage(Component.literal("[Test world] ")
                .withStyle(ChatFormatting.GRAY).append(message));
    }

}
