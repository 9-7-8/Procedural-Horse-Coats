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
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.component.DataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jspecify.annotations.Nullable;

/**
 * Dev-only: when the "Spawn Test Horse World" title-screen button
 * ({@code client/DebugTitleScreenButton}) creates a world, it sets
 * {@link #pendingHotbarFill}; on the next player login we stock the player up
 * for exercising horse features and put them somewhere worth testing from.
 *
 * <ul>
 *   <li><b>Inventory</b>: a test kit aimed at whatever is waiting on the
 *       verification checklist - see {@link #fillInventory}, and re-aim it
 *       whenever that list changes.</li>
 *   <li><b>Directions</b>: where the nearest <b>plains village</b> is, and the
 *       {@code /tp} that gets you there ({@link #locatePlainsVillage}) - because
 *       that is where both villagers live and hunting for one on foot is most of
 *       the cost of testing them. And the nearest <b>dark forest</b>
 *       ({@link #locateDarkForest}), where the kit's drop-in breeds live.</li>
 * </ul>
 *
 * Inert in production (nothing sets the flag).
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
        fillInventory(player);
        locatePlainsVillage(player);
        locateDarkForest(player);
    }

    /**
     * <b>The test kit - aimed at what is waiting on {@code wiki/verification.html}
     * right now, and meant to be re-aimed every time that list changes</b>
     * (owner's standing request, 2026-09-10: "each time you do some kind of
     * debug, change what's in my inventory ... to make it as easy as possible to
     * test each new part"). It used to be one of every mod item, which tested
     * nothing in particular. Preset eggs are the lever: a horse built for one
     * test, named for it, one right-click from existing.
     *
     * <p>Aimed at checklist &sect;0-CC, &sect;0-CB and &sect;0-CA (2026-09-11):
     * breeds that are exactly their sheets, size by copy count, drop-in breeds
     * and genes, a breed switched off and one moved. Four of those need files in
     * the dev run's {@code run/phc/} that the kit cannot make - the kit prep
     * wrote {@code kit_*.json} breeds, an example drop-in gene and a small
     * {@code breed-spawning.toml} (Friesian to dark forest only, Morgan off);
     * an egg whose breed or gene is not loaded is simply left out. Rows 2 and 3
     * carry what is still open from &sect;0-BY / &sect;0-BX. A chat legend says
     * which item is for which.
     */
    private static void fillInventory(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<String> legend = new ArrayList<>();

        // Hotbar: the breed registry and the settings file (0-CC, 0-CB).
        put(inv, legend, 0, new ItemStack(ModItems.CUSTOM_HORSE_SPAWN_EGG.get()),
                "custom egg - Breed list starts at Feral Mixed and has Test Forest Paint; hover genes for pictures");
        put(inv, legend, 1, breedEgg("kit_forest_paint"),
                "drop-in breed - chestnut paints; Breeds tab shows its About text. Wild herds: dark forest tp below");
        put(inv, legend, 2, breedEgg("morgan"),
                "Morgan, SWITCHED OFF - tame: keeps its label, breed book says 'Not found anywhere in this world'");
        put(inv, legend, 3, breedEgg("friesian"),
                "Friesian, MOVED to dark forest only - breed book should say so; no disorder outside its list");
        put(inv, legend, 4, breedEgg("quarter_horse"),
                "Quarter Horse x a handful - some may carry HYPP / PSSM1 / HERDA (its sheet lists them)");
        put(inv, legend, 5, breedEgg("thoroughbred"),
                "Thoroughbred x a handful - nothing outside its list, and no magic on any natural breed");
        put(inv, legend, 6, preset(player, "Test: Aurora (drop-in gene)", Sex.FEMALE, false,
                "example.aurora=Aur/Aur"), "drop-in gene from phc/genes/ - the log counts '1 dropped in'");
        put(inv, legend, 7, new ItemStack(Items.GOLDEN_CARROT, 64), "golden carrots - breed two one-copy horses");
        put(inv, legend, 8, new ItemStack(Items.SADDLE), "saddle - tame by riding, and the molten horses");

        // Row 1: size by copy count (0-CA), a broken breed file, the intake.
        put(inv, legend, 9, breedEgg("clydesdale"),
                "Clydesdale - Magic body size is Big/n (smaller) or Big/Big (larger); 1.14-1.43x");
        put(inv, legend, 10, breedEgg("shire"), "Shire - should tower, as before");
        put(inv, legend, 11, breedEgg("falabella"), "Falabella - always two Small copies, below your waist");
        put(inv, legend, 12, breedEgg("connemara_pony"),
                "Connemara pony - always ONE copy; breed two, some foals ordinary-sized");
        put(inv, legend, 13, breedEgg("kit_broken_gene"),
                "Test Broken Gene - names a missing gene: loaded anyway, with a log warning");
        put(inv, legend, 14, intakeChest(player),
                "intake chest - place it: one egg per new gene (checklist 0-BZ); the last three are other forms");
        put(inv, legend, 15, new ItemStack(Items.LEAD, 4), null);
        put(inv, legend, 16, new ItemStack(Items.WHEAT, 64), "wheat - spawner meals");
        put(inv, legend, 17, new ItemStack(Items.GLASS_BOTTLE, 16), "glass bottles - potion mare and stallion");

        // Row 2: still open from 0-BY.
        put(inv, legend, 18, preset(player, "Test: molten white (dominant)", Sex.FEMALE, false,
                "horsegenetics.molten_hooves=MltW/n"), "white glowing prints from ONE copy");
        put(inv, legend, 19, preset(player, "Test: molten black", Sex.FEMALE, false,
                "horsegenetics.molten_hooves=MltB/MltB"), "black prints that do NOT glow - check at night");
        put(inv, legend, 20, preset(player, "Test: molten colour", Sex.FEMALE, false,
                "horsegenetics.molten_hooves=MltC/MltC"), "glowing prints in one colour");
        put(inv, legend, 21, preset(player, "Test: molten multicolour", Sex.FEMALE, false,
                "horsegenetics.molten_hooves=MltM/MltM"), "several colours - must differ from the one-colour horse");
        put(inv, legend, 22, preset(player, "Test: sheep spawner", Sex.FEMALE, false,
                "horsegenetics.spawner=Shp/Shp"), "feed it - every sheep it makes must be the SAME colour");
        put(inv, legend, 23, new ItemStack(ModItems.HOLDING_PEN_SIGN.get(), 2),
                "holding pen signs - hang one on a pen wall; a second moves your pen");
        put(inv, legend, 24, new ItemStack(ModItems.HOLDING_PEN_TICKET.get(), 8),
                "holding pen tickets - right-click any horse you own: it goes to your pen, dead centre");
        put(inv, legend, 25, preset(player, "Test: potion mare", Sex.FEMALE, false,
                "horsegenetics.potion_milk=Spd/Spd"), "tame, hurt her, bottle her - 'she's hurt' and no potion");
        put(inv, legend, 26, preset(player, "Test: potion stallion", Sex.MALE, false,
                "horsegenetics.potion_milk=Spd/Str"), "bottle on him - he should rear, kick and say so");

        // Row 3: the research shelf, the stall, the ward.
        put(inv, legend, 27, new ItemStack(ModItems.EQUINE_RESEARCH_SHELF.get(), 2),
                "research shelf x2 - copy a gene, CLOSE the screen, come back: it kept going; break one: all drops");
        put(inv, legend, 28, new ItemStack(Items.BOOK, 16), "books - the Copy tab; a stack copies one after another");
        String[] papers = {"silver", "dun", "silver"};
        for (int i = 0; i < papers.length; i++) {
            ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
            paper.set(ModDataComponents.RESEARCH_GENE.get(), "horsegenetics." + papers[i]);
            put(inv, legend, 29 + i, paper, i == 0
                    ? "research papers - two genes, and a second Silver that must refuse to go in" : null);
        }
        put(inv, legend, 32, new ItemStack(ModItems.STALL_SIGN.get(), 4),
                "stall signs - bind, then check the size message's height");
        put(inv, legend, 33, new ItemStack(ModItems.BOUND_TICKET.get(), 8),
                "tickets - lands dead centre: try L-shaped, narrower than the horse, full of hay (must refuse)");
        put(inv, legend, 34, new ItemStack(Items.HAY_BLOCK, 32), null);
        put(inv, legend, 35, preset(player, "Test: holy ward", Sex.FEMALE, false,
                "horsegenetics.holy_ward=Hly/Hly"), "leave in the dark, stand 24+ blocks off - purple 'spawn refused' lines");

        tell(player, Component.literal("Test kit - what each thing is for:").withStyle(ChatFormatting.GOLD));
        for (String line : legend) {
            tell(player, Component.literal(line).withStyle(ChatFormatting.WHITE));
        }
    }

    /**
     * A breed egg, or {@code null} (so the slot stays empty) when this run has
     * no such breed - the kit's {@code kit_*} drop-ins only exist in a dev run
     * whose {@code run/phc/breeds/} was prepared for them.
     */
    private static @Nullable ItemStack breedEgg(String id) {
        Breed breed = Breeds.get(id);
        if (breed == null || breed == Breeds.FERAL_MIXED) {
            HorseGenetics.LOGGER.warn("Test kit: no breed '{}' - its egg skipped", id);
            return null;
        }
        return BreedSpawnEggItem.of(breed);
    }

    /**
     * The evening intake's twenty-four genes, each homozygous for its first-listed
     * allele - the form the gene's icon shows - in checklist order, then three of
     * the other forms the install changed most. A chest because twenty-seven eggs
     * do not fit beside the kit that is already waiting on the checklist.
     */
    private static final String[][] INTAKE = {
            {"tidewave"}, {"trillium"}, {"barred_wing"}, {"uraniid"}, {"candelabra"},
            {"taper_flame"}, {"inkcoil"}, {"foxglove"}, {"opal_fire"}, {"agate_eye"},
            {"corolla"}, {"contour_cells"}, {"beadscale"}, {"scuted"}, {"sporefall"},
            {"wishstar"}, {"tribal_claw"}, {"ooze_drip"}, {"rainbow_drip"}, {"datarain"},
            {"rime"}, {"foamed"}, {"gilded_crackle"}, {"holo_flake"},
            {"foxglove", "B"}, {"barred_wing", "Bwb"}, {"rainbow_drip", "Rdc"},
    };

    private static ItemStack intakeChest(ServerPlayer player) {
        List<ItemStack> eggs = new ArrayList<>();
        for (String[] entry : INTAKE) {
            Gene gene = Genes.byKeyOrNull("horsegenetics." + entry[0]);
            if (gene == null) {
                HorseGenetics.LOGGER.warn("Test kit: no gene {} for the intake chest", entry[0]);
                continue;
            }
            String token = entry.length > 1 ? entry[1] : gene.alleles().get(0).token();
            ItemStack egg = preset(player, "Intake: " + gene.name() + " (" + token + "/" + token + ")",
                    Sex.FEMALE, false, gene.key() + "=" + token + "/" + token);
            if (egg != null) {
                eggs.add(egg);
            }
        }
        ItemStack chest = new ItemStack(Items.CHEST);
        // CONTAINER on a chest item is what a picked-up chest carries and what
        // placing it restores - checked against the 26.1.2 patched sources
        // (ItemContainerContents.fromItems, DataComponents.CONTAINER), not yet
        // seen in game.
        chest.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(eggs));
        chest.set(DataComponents.CUSTOM_NAME, Component.literal("Intake genes (checklist 0-BZ)"));
        return chest;
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
            legend.add((slot < 9 ? "hotbar " + (slot + 1) : "inv " + (slot - 8)) + ": " + purpose);
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
        offerTp(player, level, "Dark forest (drop-in breed herds, moved Friesians)", hit.getFirst());
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
