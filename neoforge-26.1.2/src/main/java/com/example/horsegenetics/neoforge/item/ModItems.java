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
 *   <li><b>{@link #HORSE_HAIR} / {@link #HORSE_HAIR_BUNDLE}</b> - the material
 *       floor. Hair is sheared off a horse (mechanic not built); 4 hair craft
 *       a bundle and back (roadmap &sect;12.2, first two rungs only - rope and
 *       cloth are not built).</li>
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
 *       {@link #BOUND_TICKET}, {@link #INTERDIMENSIONAL_TICKET}) - intended to
 *       teleport a horse back to its stall. Inert placeholders.</li>
 *   <li><b>Stall signs</b> ({@link #STALL_SIGN} / {@link #BOUND_STALL_SIGN}) -
 *       {@link StallSignItem}: bind to a horse, place on the outside wall of an
 *       enclosed area to define that horse's stall.</li>
 *   <li><b>Whistles</b> ({@link #BASIC_WHISTLE}, {@link #GOLDEN_WHISTLE},
 *       {@link #ECHO_WHISTLE}) - {@link WhistleItem}: right-click to recall your
 *       tamed horses within 16 / 32 / 64 blocks.</li>
 * </ul>
 *
 * <p>All the placeholder-textured items are listed in
 * {@code wiki/verification.html} as needing art.
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
     * horseman, dear. Single use, and the horse it makes is a foundation horse
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
    public static final DeferredItem<Item> HORSE_HAIR = simple("horse_hair");
    public static final DeferredItem<Item> HORSE_HAIR_BUNDLE = simple("horse_hair_bundle");
    public static final DeferredItem<Item> BRAIDED_ROPE = simple("braided_rope");
    public static final DeferredItem<Item> HAIR_CLOTH = simple("hair_cloth");

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

    // --- tickets (roadmap §11) - stall teleport, not built ---------------
    public static final DeferredItem<Item> BLANK_TICKET = simple("blank_ticket");
    public static final DeferredItem<Item> BASIC_TICKET = simple("basic_ticket");
    public static final DeferredItem<Item> BOUND_TICKET = simple("bound_ticket");
    public static final DeferredItem<Item> INTERDIMENSIONAL_TICKET = simple("interdimensional_ticket");

    // --- stall signs (roadmap §11) - bind a horse, place on a stall wall ---
    public static final DeferredItem<StallSignItem> STALL_SIGN = register("stall_sign", StallSignItem::new);
    public static final DeferredItem<StallSignItem> BOUND_STALL_SIGN = register("bound_stall_sign", StallSignItem::new);

    // --- transfer papers - how a horse changes hands --------------------
    // A blank is bound to whoever crafted it and can only be signed against a
    // horse that player currently owns; signing it produces a signed paper,
    // which is a bearer claim on that one animal - tradeable, and redeemed by
    // right-clicking the horse itself. The cowboy sells his stock as signed
    // papers. Mechanics: server/TransferPaperHandler.
    public static final DeferredItem<TransferPaperItem> BLANK_TRANSFER_PAPER =
            register("blank_transfer_paper", TransferPaperItem::new);
    // One horse per paper, so they never stack: two of these are never the
    // same item, and a stack of them could not show you which horse is which.
    public static final DeferredItem<SignedTransferPaperItem> SIGNED_TRANSFER_PAPER =
            register("signed_transfer_paper", p -> new SignedTransferPaperItem(p.stacksTo(1)));

    // --- the two work posts (roadmap §19) --------------------------------
    // One each, because one block could not hand out both trades - see
    // block/ModBlocks.COWBOY_HITCH.
    public static final DeferredItem<net.minecraft.world.item.BlockItem> COWBOY_HITCH =
            registerBlockItem("cowboy_hitch",
                    com.example.horsegenetics.neoforge.block.ModBlocks.COWBOY_HITCH);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> HORSEMANS_TABLE =
            registerBlockItem("horsemans_table",
                    com.example.horsegenetics.neoforge.block.ModBlocks.HORSEMANS_TABLE);

    // --- whistles (roadmap §11) - recall your tamed horses in a radius ---
    public static final DeferredItem<WhistleItem> BASIC_WHISTLE =
            register("basic_whistle", p -> new WhistleItem(p, 16));
    public static final DeferredItem<WhistleItem> GOLDEN_WHISTLE =
            register("golden_whistle", p -> new WhistleItem(p, 32));
    public static final DeferredItem<WhistleItem> ECHO_WHISTLE =
            register("echo_whistle", p -> new WhistleItem(p, 64));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }

    /** Keep the custom spawn egg in the vanilla Spawn Eggs tab too, next to the real one. */
    @SubscribeEvent
    static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
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

    private ModItems() {
    }
}
