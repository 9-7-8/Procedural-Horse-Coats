package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.neoforge.HorseGenetics;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry for the mod's gameplay layer (roadmap wiki &sect;&sect;12-19).
 *
 * <p>Everything here except the {@link #CUSTOM_HORSE_SPAWN_EGG} is a
 * <b>plain {@link Item} with no behaviour yet</b> - the point of this pass is to
 * get the item objects, recipes and creative tab in place so the systems that
 * consume them (shearing, the carrot draw-modifiers, assisted reproduction, the
 * gene database, stalls/whistles) have something concrete to wire onto later.
 *
 * <ul>
 *   <li><b>{@link #HORSE_HAIR}</b> - the material floor, and the whole of it.
 *       Sheared off an adult horse once a day ({@code HorseShearHandler}) and
 *       taken directly by every recipe that wants hair. The bundle, rope and
 *       cloth rungs above it were removed; see the comment on the field.</li>
 *   <li><b>Breeding carrots</b> ({@link #UNKNOWN_EPIGENETIC_SPLICE_CARROT},
 *       {@link #UNKNOWN_GENE_SPLICE_CARROT}, {@link #STABILIZER_CARROT},
 *       {@link #MAGNIFIER_CARROT}) - the four general breeding modifiers, plus
 *       five <b>themed</b> random splices ({@link #DILUTION_GENE_SPLICE_CARROT}
 *       and friends) that roll one slice of the pool instead of all of it.</li>
 *   <li><b>{@link #KNOWN_GENE_SPLICE_CARROT}</b> - one item parameterised by a
 *       {@code carrot_effects} component holding a {@code known:<gene>:het|hom}
 *       token, produced by the paper-driven {@code KnownGeneSpliceRecipe}.</li>
 *   <li><b>{@link #RESEARCH_PAPER}</b> ({@link ResearchPaperItem}) - carries a
 *       {@code gene} component; read to unlock that gene's carrot recipe, and
 *       the ingredient {@code KnownGeneSpliceRecipe} matches on.</li>
 *   <li><b>{@link #EMPTY_SEED_JAR} / {@link #STALLION_SEED_JAR}</b> - the
 *       assisted-reproduction vessels. Items only; no collection / pregnancy
 *       mechanic (owner: IVF is out of scope for now).</li>
 *   <li><b>Tickets</b> ({@link #BLANK_TICKET}, {@link #BASIC_TICKET},
 *       {@link #BOUND_TICKET}, {@link #INTERDIMENSIONAL_TICKET}) -
 *       {@link TicketItem}: right-click your horse to send it to its stall, one
 *       use, the tier setting how far apart the two may be. The blank is the
 *       crafting base and does nothing.</li>
 *   <li><b>Stall signs</b> ({@link #STALL_SIGN} / {@link #BOUND_STALL_SIGN}) -
 *       {@link StallSignItem}: bind to a horse, place on the outside wall of an
 *       enclosed area to define that horse's stall.</li>
 *   <li><b>Whistles</b> ({@link #BASIC_WHISTLE}, {@link #GOLDEN_WHISTLE},
 *       {@link #ECHO_WHISTLE}) - {@link WhistleItem}: right-click to recall your
 *       tamed horses within 16 / 32 / 64 blocks.</li>
 * </ul>
 *
 * <p>All the placeholder-textured items are listed in
 * {@code wiki/items.html#item-art} as needing art.
 */
@EventBusSubscriber(modid = HorseGenetics.MOD_ID)
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(HorseGenetics.MOD_ID);

    /** Every gameplay-layer item, in creative-tab display order (spawn egg first). */
    public static final List<DeferredItem<? extends Item>> TAB_ITEMS = new ArrayList<>();

    private static DeferredItem<Item> simple(String name) {
        return register(name, Item::new);
    }

    private static <T extends Item> DeferredItem<T> register(String name, Function<Item.Properties, ? extends T> factory) {
        DeferredItem<T> item = ITEMS.registerItem(name, factory);
        TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<net.minecraft.world.item.BlockItem> registerBlockItem(
            String name, net.neoforged.neoforge.registries.DeferredBlock<?> block) {
        DeferredItem<net.minecraft.world.item.BlockItem> item =
                ITEMS.registerItem(name, p -> new net.minecraft.world.item.BlockItem(block.get(), p));
        TAB_ITEMS.add(item);
        return item;
    }

    // Dev/testing tool - opens an age/sex/genome editor before spawning. Not a
    // real SpawnEggItem; see client/CustomHorseSpawnEggClient.
    public static final DeferredItem<Item> CUSTOM_HORSE_SPAWN_EGG = simple("custom_horse_spawn_egg");

    /**
     * <b>Breed spawn eggs</b> - one item, the breed on a {@code breed}
     * component. Not craftable: they come out of dungeon chests and off the
     * scientist, dear. Single use, and the horse it makes is a foundation horse
     * of that breed. See {@link BreedSpawnEggItem}.
     */
    // Registered without joining TAB_ITEMS: a *blank* breed egg is a puzzle,
    // not an item. Both tabs get the filled ones instead - see addToCreativeTab.
    public static final DeferredItem<BreedSpawnEggItem> BREED_SPAWN_EGG =
            ITEMS.registerItem("breed_spawn_egg", BreedSpawnEggItem::new);

    /**
     * A horse the creative editor saved rather than spawned - the exact
     * genotype, epigenome, sex, age and breed label that were on screen. Made
     * in the custom spawn egg's screen; usable by anybody.
     */
    public static final DeferredItem<PresetHorseSpawnEggItem> PRESET_HORSE_SPAWN_EGG =
            ITEMS.registerItem("preset_horse_spawn_egg", PresetHorseSpawnEggItem::new);

    // --- material floor (roadmap §12.2) -----------------------------------
    // ONE RUNG, NOT FOUR. There was a chain here - 4 hair to a bundle, 3 bundles
    // to a braided rope, 4 bundles to a hair cloth - and everything the mod built
    // out of hair took a rope or a cloth rather than hair. That priced a double
    // fence gate at twelve hairs and a cowboy hitch at sixty-eight, which is a
    // shearing grind in front of a fence post. Owner's call: the intermediates
    // are gone and every recipe that wanted one takes raw hair instead.
    public static final DeferredItem<Item> HORSE_HAIR = simple("horse_hair");

    // --- the four breeding carrots (roadmap §14.1) -------------------------
    public static final DeferredItem<Item> UNKNOWN_EPIGENETIC_SPLICE_CARROT = simple("unknown_epigenetic_splice_carrot");
    public static final DeferredItem<Item> UNKNOWN_GENE_SPLICE_CARROT = simple("unknown_gene_splice_carrot");
    public static final DeferredItem<Item> STABILIZER_CARROT = simple("stabilizer_carrot");
    public static final DeferredItem<Item> MAGNIFIER_CARROT = simple("magnifier_carrot");

    // --- the themed random splices --------------------------------------
    // Each is an Unknown Gene Splice carrot narrowed to one slice of the safe
    // pool (common/genetics/SpliceCategory), refined from the plain one with a
    // themed reagent. The pool is a subset of the safe pool by construction, so
    // none of these can hand a player a damaged foal either.
    public static final DeferredItem<Item> DILUTION_GENE_SPLICE_CARROT = simple("dilution_gene_splice_carrot");
    public static final DeferredItem<Item> WHITE_GENE_SPLICE_CARROT = simple("white_gene_splice_carrot");
    public static final DeferredItem<Item> MARKING_GENE_SPLICE_CARROT = simple("marking_gene_splice_carrot");
    public static final DeferredItem<Item> PERFORMANCE_GENE_SPLICE_CARROT = simple("performance_gene_splice_carrot");
    public static final DeferredItem<Item> MAGICAL_GENE_SPLICE_CARROT = simple("magical_gene_splice_carrot");

    // --- Known Gene Splice carrot (roadmap §14.2) - parameterised by a paper ---
    public static final DeferredItem<Item> KNOWN_GENE_SPLICE_CARROT = simple("known_gene_splice_carrot");

    // --- knowledge (roadmap §16.2) - the research paper -------------------
    // Carries a `gene` component. Reading it adds the gene to the player's
    // database and unlocks that gene's carrot recipe; it is also the
    // ingredient the parameterised KnownGeneSpliceRecipe matches on.
    public static final DeferredItem<ResearchPaperItem> RESEARCH_PAPER =
            register("research_paper", ResearchPaperItem::new);

    // --- assisted reproduction vessels (roadmap §15.1) -------------------
    // Empty jar: filled at a stallion. Stallion seed jar: carries a
    // StoredGenome data component; impregnates a mare. Collection / impregnation
    // live in server/StallionSeedJarHandler.
    public static final DeferredItem<SeedJarItem> EMPTY_SEED_JAR = register("empty_seed_jar", SeedJarItem::new);
    public static final DeferredItem<SeedJarItem> STALLION_SEED_JAR = register("stallion_seed_jar", SeedJarItem::new);

    // The vet's kit: sneak-use gelds your stallion, plain use examines any horse.
    // Behaviour in server/VetKitHandler. Durable, like shears.
    @SuppressWarnings("deprecation") // Item(Properties) - see SeedJarItem
    public static final DeferredItem<Item> VET_KIT = register("vet_kit", p -> new Item(p.durability(64)));

    /**
     * <b>Golden carrot seeds</b> - the only way to plant a golden carrot.
     *
     * <p>A plain {@code BlockItem} on {@code ModBlocks.GOLDEN_CARROT_CROP}
     * with {@code useItemDescriptionPrefix()}, which is exactly how 26.1.2
     * builds {@code wheat_seeds} and {@code carrot} - see
     * {@code Items.createBlockItemWithCustomItemName}. The prefix call is what
     * makes it keep its own name ("Golden Carrot Seeds") rather than taking the
     * block's; there is no {@code ItemNameBlockItem} class in this version.
     *
     * <p>Deliberately <b>not craftable</b>. Two sources, both scarce: the
     * equestrian supplier's top rank, at a price, and dungeon-grade chest loot.
     * Making a golden carrot itself plantable would have handed every player
     * who ever crafted one an unlimited supply, which is the opposite of the
     * point. See GoldenCarrotCropBlock.
     */
    public static final DeferredItem<net.minecraft.world.item.BlockItem> GOLDEN_CARROT_SEEDS =
            register("golden_carrot_seeds", p -> new net.minecraft.world.item.BlockItem(
                    com.example.horsegenetics.neoforge.block.ModBlocks.GOLDEN_CARROT_CROP.get(),
                    p.useItemDescriptionPrefix()));

    // --- tickets (roadmap §11) - send a horse to its stall ----------------
    // The blank is the crafting base and stays inert; the other three are one
    // use each and differ only in reach. See TicketItem / server.TicketHandler.
    public static final DeferredItem<Item> BLANK_TICKET = simple("blank_ticket");
    public static final DeferredItem<TicketItem> BASIC_TICKET =
            register("basic_ticket", p -> new TicketItem(p, TicketItem.Tier.BASIC));
    public static final DeferredItem<TicketItem> BOUND_TICKET =
            register("bound_ticket", p -> new TicketItem(p, TicketItem.Tier.BOUND));
    public static final DeferredItem<TicketItem> INTERDIMENSIONAL_TICKET =
            register("interdimensional_ticket", p -> new TicketItem(p, TicketItem.Tier.INTERDIMENSIONAL));

    // --- stall signs (roadmap §11) - bind a horse, place on a stall wall ---
    public static final DeferredItem<StallSignItem> STALL_SIGN = register("stall_sign", StallSignItem::new);
    public static final DeferredItem<StallSignItem> BOUND_STALL_SIGN = register("bound_stall_sign", StallSignItem::new);

    // --- the holding pen - one per player, for horses with no stall yet ----
    // A sign bound to the player rather than a horse, and a ticket that sends
    // any horse you own there. HoldingPenSignItem / server.TicketHandler.
    public static final DeferredItem<HoldingPenSignItem> HOLDING_PEN_SIGN =
            register("holding_pen_sign", HoldingPenSignItem::new);
    public static final DeferredItem<HoldingPenTicketItem> HOLDING_PEN_TICKET =
            register("holding_pen_ticket", HoldingPenTicketItem::new);

    // --- transfer papers - how a horse changes hands --------------------
    // A blank is bound to whoever crafted it and can only be signed against a
    // horse that player currently owns; signing it produces a signed paper,
    // which is a bearer claim on that one animal - tradeable, and redeemed by
    // right-clicking the horse itself. The cowboy sells their stock as signed
    // papers. Mechanics: server/TransferPaperHandler.
    public static final DeferredItem<TransferPaperItem> BLANK_TRANSFER_PAPER =
            register("blank_transfer_paper", TransferPaperItem::new);
    // One horse per paper, so they never stack: two of these are never the
    // same item, and a stack of them could not show you which horse is which.
    public static final DeferredItem<SignedTransferPaperItem> SIGNED_TRANSFER_PAPER =
            register("signed_transfer_paper", p -> new SignedTransferPaperItem(p.stacksTo(1)));

    // --- the five work posts (roadmap §19) -------------------------------
    // One per job, because one block could not hand out more than one trade -
    // see block/ModBlocks.COWBOY_HITCH and village/ModPoiTypes.
    public static final DeferredItem<net.minecraft.world.item.BlockItem> COWBOY_HITCH =
            registerBlockItem("cowboy_hitch",
                    com.example.horsegenetics.neoforge.block.ModBlocks.COWBOY_HITCH);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> LEATHERWORKERS_POST =
            registerBlockItem("leatherworkers_post",
                    com.example.horsegenetics.neoforge.block.ModBlocks.LEATHERWORKERS_POST);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SCIENTISTS_POST =
            registerBlockItem("scientists_post",
                    com.example.horsegenetics.neoforge.block.ModBlocks.SCIENTISTS_POST);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SUPPLIERS_POST =
            registerBlockItem("suppliers_post",
                    com.example.horsegenetics.neoforge.block.ModBlocks.SUPPLIERS_POST);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> METALSMITHS_POST =
            registerBlockItem("metalsmiths_post",
                    com.example.horsegenetics.neoforge.block.ModBlocks.METALSMITHS_POST);

    /** The Equine Research Shelf, as an item you can carry and place. */
    public static final DeferredItem<net.minecraft.world.item.BlockItem> EQUINE_RESEARCH_SHELF =
            registerBlockItem("equine_research_shelf",
                    com.example.horsegenetics.neoforge.block.ModBlocks.RESEARCH_SHELF);

    /** The Tack Dyeing Bench, as an item you can carry and place. */
    public static final DeferredItem<net.minecraft.world.item.BlockItem> EQUESTRIAN_BENCH =
            registerBlockItem("equestrian_bench",
                    com.example.horsegenetics.neoforge.block.ModBlocks.EQUESTRIAN_BENCH);

    // --- whistles (roadmap §11) - recall your tamed horses in a radius ---
    public static final DeferredItem<WhistleItem> BASIC_WHISTLE =
            register("basic_whistle", p -> new WhistleItem(p, 16));
    public static final DeferredItem<WhistleItem> GOLDEN_WHISTLE =
            register("golden_whistle", p -> new WhistleItem(p, 32));
    public static final DeferredItem<WhistleItem> ECHO_WHISTLE =
            register("echo_whistle", p -> new WhistleItem(p, 64));
    /**
     * Bound to one horse for good, and calls it from anywhere, across dimensions -
     * see {@link EnderWhistleItem}. One to a stack: two bound whistles are never
     * the same item.
     */
    public static final DeferredItem<EnderWhistleItem> ENDER_WHISTLE =
            register("ender_whistle", p -> new EnderWhistleItem(p.stacksTo(1)));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }

    /** Keep the custom spawn egg in the vanilla Spawn Eggs tab too, next to the real one. */
    @SubscribeEvent
    static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // THE DOUBLE GATES GO IN BUILDING BLOCKS, each directly after the
        // vanilla gate it is made from - which is where somebody reaching for a
        // gate actually looks, rather than in a tab about horses. Owner's call.
        //
        // insertAfter asserts its anchor is present and THROWS if it is not, so
        // the vanilla gate is checked for first: another mod is entitled to have
        // removed it, and a hard crash on somebody else's load order would be a
        // poor trade for a tidy menu.
        //
        // THE CHECK HAS TO MATCH THE ASSERT EXACTLY, and v0.5.014's did not, in
        // two ways that both cost the player the whole tab:
        //
        //   1. PARENT_AND_SEARCH_TABS asserts the anchor is in BOTH the parent
        //      list and the search list. Checking only getParentEntries() passes
        //      for an anchor another mod put in one and not the other.
        //   2. These are ItemStackLinkedSet.createTypeAndComponentsSet(), so
        //      membership is item AND components. `stack.is(anchor)` compares
        //      the item alone, so an anchor sitting in the tab with components
        //      on it answers "present" and then fails contains().
        //
        // And the damage is out of all proportion to the cause, because
        // CreativeModeTab.buildContents assigns displayItems only AFTER the
        // event returns: a throw in here leaves the tab holding what it had
        // before, which on a first build is nothing. So one bad anchor does not
        // lose one gate - it silently empties Building Blocks, for vanilla items
        // too, with no crash and nothing in the log. That is what
        // regions_unexplored:baobab_fence_gate did in v0.5.014.
        //
        // Hence also the catch. The paragraph above is the argument for why a
        // guard is not enough on its own: the cost of being wrong here is a tab,
        // and the things that can be wrong are other people's.
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (com.example.horsegenetics.neoforge.block.DoubleGates.Gate gate
                    : com.example.horsegenetics.neoforge.block.DoubleGates.gates()) {
                fileAfter(event, gate.sourceGate().get(), gate.item().get(), gate.item().getId());
            }
            // THE JUMP IS DELIBERATELY NOT HERE. It was, for one build, filed
            // after the vanilla fence of each wood on the same argument the
            // gates use. The owner's call was that it does not carry: a gate is
            // a building block horses happen to care about, and a jump is horse
            // equipment that is useless without one. It then had a tab of its
            // own for a day, and now sits in ModCreativeTabs.MAIN with the rest
            // of the horse things - there is one jump item, and a tab holding
            // one thing is worse than a row.
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(CUSTOM_HORSE_SPAWN_EGG.get());
            // One filled egg per breed that has one. The creative tab is the
            // only place the whole set is visible at once, and a blank
            // component-driven egg in a tab is a puzzle rather than an item.
            for (com.example.horsegenetics.common.breed.Breed breed
                    : com.example.horsegenetics.common.breed.Breeds.from(
                            com.example.horsegenetics.common.breed.BreedSource.SPAWN_EGG)) {
                event.accept(BreedSpawnEggItem.of(breed));
            }
        }
    }

    /**
     * File one of ours directly after the vanilla item it is built from, or
     * failing that anywhere in the tab at all.
     *
     * <p>Shared by the double gates and the jumps. <b>Read the long note at the
     * head of {@link #addToCreativeTab} before touching this</b> - every line
     * of it is load-bearing, and the cost of getting it wrong is not one item
     * in the wrong place but the whole of Building Blocks silently empty, for
     * vanilla items too, with nothing in the log.
     *
     * @param anchor the vanilla item to file after. Resolved by the caller at
     *               event time rather than held on a record, because a block
     *               for a modded wood is filed after an item from a mod that
     *               had not registered it when ours was built; a mod that
     *               declared one in its assets and never registered it comes
     *               back as AIR here rather than throwing.
     */
    private static void fileAfter(BuildCreativeModeTabContentsEvent event,
                                  net.minecraft.world.item.Item anchor,
                                  net.minecraft.world.item.Item ourItem,
                                  Object idForLog) {
        if (anchor == null || anchor == net.minecraft.world.item.Items.AIR) {
            return;
        }
        net.minecraft.world.item.ItemStack anchorStack = new net.minecraft.world.item.ItemStack(anchor);
        net.minecraft.world.item.ItemStack ours = new net.minecraft.world.item.ItemStack(ourItem);
        try {
            if (event.getParentEntries().contains(anchorStack)
                    && event.getSearchEntries().contains(anchorStack)) {
                event.insertAfter(anchorStack, ours,
                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                // Still reachable, just not filed next to its parent. One of
                // these nobody can find is worse than one in the wrong place.
                event.accept(ours,
                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        } catch (RuntimeException somebodyElsesTab) {
            HorseGenetics.LOGGER.warn("compat: could not file {} beside {} in Building Blocks - {}",
                    idForLog, anchor, somebodyElsesTab.toString());
        }
    }

    private ModItems() {
    }
}
