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
 *       the cost of testing them.</li>
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
     * <p>Aimed at checklist &sect;0-BX / &sect;0-BS (2026-09-10): molten hooves
     * prints, the uncapped fed spawner, the potion-milk stallion and foal, holy
     * ward, a ridden particle trail, the chest shelf, stall landing and height,
     * and hand-taming a breed horse for the Breeds tab. A chat legend says which
     * item is for which.
     */
    private static void fillInventory(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<String> legend = new ArrayList<>();

        // Hotbar: the horses and what they eat.
        put(inv, legend, 0, new ItemStack(ModItems.CUSTOM_HORSE_SPAWN_EGG.get()), "custom egg - build anything else");
        put(inv, legend, 1, preset(player, "Test: molten hooves", Sex.FEMALE, false,
                "horsegenetics.molten_hooves=Mlt/Mlt"), "ride at night - prints flat, glowing, toe forward, two colours alternating");
        put(inv, legend, 2, preset(player, "Test: cow spawner", Sex.FEMALE, false,
                "horsegenetics.spawner=Cow/Cow"), "feed wheat - two cows every meal it eats; tamed + full health refuses and makes none");
        put(inv, legend, 3, new ItemStack(Items.WHEAT, 64), "wheat - spawner meals, and hand-taming (crouch, look, stand still)");
        put(inv, legend, 4, new ItemStack(Items.GLASS_BOTTLE, 16), "glass bottles - hold out to the potion stallion and foal");
        put(inv, legend, 5, preset(player, "Test: potion stallion", Sex.MALE, false,
                "horsegenetics.potion_milk=Spd/Str"), "bottle on him - he should rear, kick and say so");
        put(inv, legend, 6, preset(player, "Test: potion foal", Sex.FEMALE, true,
                "horsegenetics.potion_milk=Spd/Spd"), "bottle on her - 'a foal has no potion milk'");
        put(inv, legend, 7, new ItemStack(Items.SADDLE), "saddle - for riding the molten and particle horses");
        put(inv, legend, 8, new ItemStack(Items.LEAD), null);

        // Main inventory, row 1: the other horses.
        put(inv, legend, 9, preset(player, "Test: holy ward", Sex.FEMALE, false,
                "horsegenetics.holy_ward=Hly/Hly"), "leave in the dark, stand 24+ blocks off - purple 'spawn refused' lines");
        put(inv, legend, 10, preset(player, "Test: zombie spawner", Sex.MALE, false,
                "horsegenetics.spawner=Zmb/Zmb"), "feed it (survival, Easy+) - check the zombies' gear is ordinary");
        put(inv, legend, 11, preset(player, "Test: fading-dust trail", Sex.FEMALE, false,
                "horsegenetics.particle=Dst2/Dst2"), "ride it - the trail must show while ridden");
        Breed breed = Breeds.get("arabian") != null ? Breeds.get("arabian")
                : (Breeds.all().isEmpty() ? null : Breeds.all().get(0));
        if (breed != null) {
            put(inv, legend, 12, BreedSpawnEggItem.of(breed), "wild " + breed.name()
                    + " - tame by hand in survival, then its Breeds row should fill in");
        }
        put(inv, legend, 13, new ItemStack(Items.GOLDEN_CARROT, 16), null);

        // Row 2: the research shelf.
        put(inv, legend, 18, new ItemStack(ModItems.EQUINE_RESEARCH_SHELF.get(), 2),
                "research shelf x2 - one to fill and BREAK: every paper must drop");
        put(inv, legend, 19, new ItemStack(Items.BOOK, 16), "books - the Copy tab");
        String[] papers = {"silver", "dun", "roan", "tobiano", "champagne", "silver"};
        for (int i = 0; i < papers.length; i++) {
            ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
            paper.set(ModDataComponents.RESEARCH_GENE.get(), "horsegenetics." + papers[i]);
            put(inv, legend, 20 + i, paper, i == 0
                    ? "research papers - five genes, and a second Silver that must refuse to go in" : null);
        }

        // Row 3: build a stall.
        put(inv, legend, 27, new ItemStack(ModItems.STALL_SIGN.get(), 4),
                "stall signs - bind, then check the size message's height");
        put(inv, legend, 28, new ItemStack(ModItems.BOUND_TICKET.get(), 8),
                "tickets - horse must land dead centre: try 2-wide, L-shaped, narrower than the horse");
        put(inv, legend, 29, new ItemStack(Items.OAK_FENCE, 64), null);
        put(inv, legend, 30, new ItemStack(Items.OAK_FENCE_GATE, 8), null);
        put(inv, legend, 31, new ItemStack(Items.OAK_PLANKS, 64), null);
        put(inv, legend, 32, new ItemStack(Items.OAK_SLAB, 32), null);
        put(inv, legend, 33, new ItemStack(Items.HAY_BLOCK, 32),
                "hay - fill a stall solid: the ticket must refuse and not be used up");

        tell(player, Component.literal("Test kit - what each thing is for:").withStyle(ChatFormatting.GOLD));
        for (String line : legend) {
            tell(player, Component.literal(line).withStyle(ChatFormatting.WHITE));
        }
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

        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found);
        int altitude = Math.max(TP_ALTITUDE, surface.getY() + 2);
        String command = "/tp @s " + surface.getX() + " " + altitude + " " + surface.getZ();
        HorseGenetics.LOGGER.info("Test world: nearest plains village at {}", surface);

        tell(player, Component.literal("Plains village at "
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
