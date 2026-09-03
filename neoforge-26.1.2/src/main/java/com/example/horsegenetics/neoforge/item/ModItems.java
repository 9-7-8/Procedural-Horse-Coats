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
 *   <li><b>Breeding carrots</b> ({@link #MUTINOGENIC_CARROT},
 *       {@link #CHAOS_CARROT}, {@link #STABILIZER_CARROT},
 *       {@link #MAGNIFIER_CARROT}) - the four general breeding modifiers.</li>
 *   <li><b>{@link #MAGIC_GENE_CARROT}</b> - one generic item (the roadmap's
 *       per-gene parameterisation needs a data component; deferred).</li>
 *   <li><b>{@link #PLACEHOLDER_GENE_BOOK}</b> - stand-in for the research paper
 *       that gates carrot recipes. Literally named "PLACEHOLDER GENE BOOK";
 *       to be replaced.</li>
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

    // Dev/testing tool - opens an age/sex/genome editor before spawning. Not a
    // real SpawnEggItem; see client/CustomHorseSpawnEggClient.
    public static final DeferredItem<Item> CUSTOM_HORSE_SPAWN_EGG = simple("custom_horse_spawn_egg");

    // --- material floor -----------------------------------------------------
    public static final DeferredItem<Item> HORSE_HAIR = simple("horse_hair");
    public static final DeferredItem<Item> HORSE_HAIR_BUNDLE = simple("horse_hair_bundle");

    // --- the four breeding carrots (roadmap §14.1) -------------------------
    public static final DeferredItem<Item> MUTINOGENIC_CARROT = simple("mutinogenic_carrot");
    public static final DeferredItem<Item> CHAOS_CARROT = simple("chaos_carrot");
    public static final DeferredItem<Item> STABILIZER_CARROT = simple("stabilizer_carrot");
    public static final DeferredItem<Item> MAGNIFIER_CARROT = simple("magnifier_carrot");

    // --- magic gene carrot (roadmap §14.2) - one generic item for now -----
    public static final DeferredItem<Item> MAGIC_GENE_CARROT = simple("magic_gene_carrot");

    // --- knowledge (roadmap §16.2) - placeholder for the research paper ---
    public static final DeferredItem<Item> PLACEHOLDER_GENE_BOOK = simple("placeholder_gene_book");

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
        }
    }

    private ModItems() {
    }
}
