package com.example.horsegenetics.neoforge.server;

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
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
        locatePlainsVillage(player);
        locateDarkForest(player);
    }

    /**
     * {@code /testkit} lists the batches; {@code /testkit <n>} swaps the hotbar
     * for batch {@code n}. Dev runs only, and gamemaster permission, which the
     * test world's cheats give you. It exists so moving to the next batch costs
     * one line of chat rather than a rebuild and a fresh world.
     */
    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
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
                        })));
    }

    /**
     * <b>The test kit - aimed at what is waiting on {@code wiki/verification.html}
     * right now, and meant to be re-aimed every time that list changes</b>
     * (owner's standing request, 2026-09-10: "each time you do some kind of
     * debug, change what's in my inventory ... to make it as easy as possible to
     * test each new part"). Preset eggs are the lever: a horse built for one
     * test, named for it, one right-click from existing.
     *
     * <p><b>One hotbar at a time.</b> A kit that filled all 36 slots, with a
     * 30-line legend, was "SUPER overwhelming" (owner, 2026-09-11) - so the
     * checklist is cut into themed batches of at most nine, the first handed out
     * on login and the rest a {@code /testkit n} away. Every batch that tames
     * carries a <b>stick</b> (it tames on the spot - {@code HorseInteractionHandler})
     * and every batch that breeds carries <b>golden carrots</b>.
     *
     * <p>A batch is deleted once the owner has worked through it, and the rest
     * move up - the first batch is always the next thing to test. The breeds
     * batch went that way on 2026-09-11; its leftovers need no items, only the
     * dark-forest {@code /tp} and the dev run's {@code run/phc/} files (the
     * {@code kit_*.json} breeds, a {@code breed-spawning.toml} moving the
     * Friesian and switching the Morgan off). An egg whose breed or gene is not
     * loaded is simply left out.
     */
    private static final String[] BATCHES = {
            "Size by copy count (0-CA)",
            "Intake: the rebuilt marks, and their other forms (0-BZ)",
            "Intake: drips, contour cells, crackle, flakes, small drawings (0-BZ)",
            "Intake: the rest (0-BZ)",
            "Molten hooves, the sheep spawner, the ward (0-BY, 0-BX)",
            "Holding pen and stalls (0-BY, 0-BX)",
            "Potion milk and the research shelf (0-BY, 0-BX)",
    };

    private static void listBatches(ServerPlayer player) {
        tell(player, Component.literal("Test kit batches - click one, then press Enter:")
                .withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < BATCHES.length; i++) {
            tell(player, command("/testkit " + (i + 1), BATCHES[i]));
        }
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
                put(inv, legend, 0, new ItemStack(Items.STICK), "stick - tame first; the Magic tab shows body size");
                put(inv, legend, 1, new ItemStack(Items.GOLDEN_CARROT, 64), "golden carrots - breeding");
                put(inv, legend, 2, breedEgg("clydesdale"),
                        "Clydesdale - Big/n are the smaller ones, Big/Big the larger (1.14-1.43x)");
                put(inv, legend, 3, breedEgg("shire"), "Shire - should tower, as before");
                put(inv, legend, 4, breedEgg("falabella"), "Falabella - always two Small copies, below your waist");
                put(inv, legend, 5, breedEgg("connemara_pony"),
                        "Connemara - always ONE copy; spawn two, tame, breed: some foals ordinary-sized");
                put(inv, legend, 6, new ItemStack(Items.CLOCK), "clock - right-click a foal: grown at once, to compare sizes");
            }
            case 2 -> {
                intake(player, inv, legend, 0, "corolla", null, "the eye and deep heart should show inside the mark");
                intake(player, inv, legend, 1, "agate_eye", null, "two bands and a dark core");
                intake(player, inv, legend, 2, "taper_flame", null, "a spark inside the flame");
                intake(player, inv, legend, 3, "barred_wing", null, "bars inside the wing");
                intake(player, inv, legend, 4, "barred_wing", "Bwb", "the black form");
                intake(player, inv, legend, 5, "uraniid", null, "bands inside the mark");
                intake(player, inv, legend, 6, "trillium", null, "a ring and an eye");
                intake(player, inv, legend, 7, "foxglove", null, "ringed throats");
                intake(player, inv, legend, 8, "foxglove", "B", "the nightbell form");
            }
            case 3 -> {
                intake(player, inv, legend, 0, "ooze_drip", null, "hangs DOWN from the spine, not toward the tail");
                intake(player, inv, legend, 1, "rainbow_drip", null, "hangs down too");
                intake(player, inv, legend, 2, "rainbow_drip", "Rdc", "the coloured form");
                intake(player, inv, legend, 3, "contour_cells", null,
                        "nested outlines in dark patches - may take several eggs; check the far flank");
                intake(player, inv, legend, 4, "gilded_crackle", null, "pale plates with gold seams");
                intake(player, inv, legend, 5, "holo_flake", null, "separate glinting flakes on the crest");
                intake(player, inv, legend, 6, "rime", null, "must look unlike maelstrom (in the custom egg)");
                intake(player, inv, legend, 7, "candelabra", null, "small by design; a jagged edge is the simplified path");
                intake(player, inv, legend, 8, "tribal_claw", null, "three hairline strokes - do they read at a distance?");
            }
            case 4 -> {
                String[] rest = {"tidewave", "inkcoil", "opal_fire", "beadscale", "scuted",
                        "sporefall", "wishstar", "datarain", "foamed"};
                for (int i = 0; i < rest.length; i++) {
                    intake(player, inv, legend, i, rest[i], null, i == 0 ? "does each look like its icon?" : null);
                }
            }
            case 5 -> {
                put(inv, legend, 0, new ItemStack(Items.STICK), "stick - tame before saddling");
                put(inv, legend, 1, new ItemStack(Items.SADDLE), "saddle - ride them: prints follow a ridden horse too");
                put(inv, legend, 2, preset(player, "Test: molten white (dominant)", Sex.FEMALE, false,
                        "horsegenetics.molten_hooves=MltW/n"), "white glowing prints from ONE copy");
                put(inv, legend, 3, preset(player, "Test: molten black", Sex.FEMALE, false,
                        "horsegenetics.molten_hooves=MltB/MltB"), "black prints that do NOT glow - check at night");
                put(inv, legend, 4, preset(player, "Test: molten colour", Sex.FEMALE, false,
                        "horsegenetics.molten_hooves=MltC/MltC"), "glowing prints in one colour");
                put(inv, legend, 5, preset(player, "Test: molten multicolour", Sex.FEMALE, false,
                        "horsegenetics.molten_hooves=MltM/MltM"), "several colours - must differ from slot 5");
                put(inv, legend, 6, preset(player, "Test: sheep spawner", Sex.FEMALE, false,
                        "horsegenetics.spawner=Shp/Shp"), "tame, feed wheat - every sheep the SAME colour");
                put(inv, legend, 7, new ItemStack(Items.WHEAT, 64), "wheat - the spawner's meals");
                put(inv, legend, 8, preset(player, "Test: holy ward", Sex.FEMALE, false,
                        "horsegenetics.holy_ward=Hly/Hly"), "leave in the dark, stand 24+ blocks off: purple 'spawn refused'");
            }
            case 6 -> {
                put(inv, legend, 0, new ItemStack(Items.STICK), "stick - both tickets need a horse you own");
                put(inv, legend, 1, breedEgg("arabian"), "a horse to move around");
                put(inv, legend, 2, new ItemStack(ModItems.HOLDING_PEN_SIGN.get(), 2),
                        "holding pen sign - hang on a pen wall; a second one moves your pen");
                put(inv, legend, 3, new ItemStack(ModItems.HOLDING_PEN_TICKET.get(), 8),
                        "pen ticket - on your horse: it lands in the pen, dead centre. Try untamed / no pen / full");
                put(inv, legend, 4, new ItemStack(ModItems.STALL_SIGN.get(), 4),
                        "stall sign - bind it; the size message should give the real height");
                put(inv, legend, 5, new ItemStack(ModItems.BOUND_TICKET.get(), 8),
                        "stall ticket - dead centre even L-shaped or narrower than the horse");
                put(inv, legend, 6, new ItemStack(Items.HAY_BLOCK, 64),
                        "hay - fill a stall solid: the ticket must refuse and not be used up");
                put(inv, legend, 7, new ItemStack(Items.OAK_FENCE, 64), null);
                put(inv, legend, 8, new ItemStack(Items.OAK_FENCE_GATE, 8), null);
            }
            case 7 -> {
                put(inv, legend, 0, new ItemStack(Items.STICK), "stick - tame each one first");
                put(inv, legend, 1, preset(player, "Test: potion mare", Sex.FEMALE, false,
                        "horsegenetics.potion_milk=Spd/Spd"), "hurt her, then bottle her: 'She's hurt' and no potion");
                put(inv, legend, 2, preset(player, "Test: potion stallion", Sex.MALE, false,
                        "horsegenetics.potion_milk=Spd/Str"), "bottle on him - he rears, kicks and says so");
                put(inv, legend, 3, preset(player, "Test: potion foal", Sex.FEMALE, true,
                        "horsegenetics.potion_milk=Spd/Spd"), "bottle on her - a foal has nothing to give");
                put(inv, legend, 4, new ItemStack(Items.GLASS_BOTTLE, 16), "glass bottles");
                put(inv, legend, 5, new ItemStack(Items.BUCKET), "bucket - a hurt mare refuses plain milk the same way");
                put(inv, legend, 6, new ItemStack(ModItems.EQUINE_RESEARCH_SHELF.get(), 2),
                        "research shelf - copy, CLOSE the screen, come back: it kept going; break it: everything drops");
                put(inv, legend, 7, new ItemStack(Items.BOOK, 16), "books - the Copy tab, one copy per book");
                ItemStack papers = new ItemStack(ModItems.RESEARCH_PAPER.get(), 2);
                papers.set(ModDataComponents.RESEARCH_GENE.get(), "horsegenetics.silver");
                put(inv, legend, 8, papers, "two Silver papers - the second must refuse to go in");
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
                               String id, @Nullable String token, @Nullable String look) {
        Gene gene = Genes.byKeyOrNull("horsegenetics." + id);
        if (gene == null) {
            HorseGenetics.LOGGER.warn("Test kit: no gene {}", id);
            return;
        }
        String t = token != null ? token : gene.alleles().get(0).token();
        String label = gene.name() + " (" + t + "/" + t + ")";
        put(inv, legend, slot, preset(player, "Intake: " + label, Sex.FEMALE, false,
                gene.key() + "=" + t + "/" + t), look == null ? null : label + " - " + look);
    }

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
        ItemStack egg = PresetHorseSpawnEggItem.of(new StoredGenome(genome.genotypeCode(),
                genome.epigenome().toCode(), player.getUUID(), player.getGameProfile().name(), ""), baby);
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
