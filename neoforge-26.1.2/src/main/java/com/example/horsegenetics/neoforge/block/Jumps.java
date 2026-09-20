package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The showjumping rails</b>, one per wood, and the one place that knows the
 * list.
 *
 * <p>Deliberately the same shape as {@link DoubleGates}, down to the dup-guard
 * and the field ordering, because it has the same job: register a family whose
 * members differ only by a wood, and pick up every wood another mod brought
 * with it. Read that class's notes - the war stories in it are this class's too,
 * and they were paid for once already.
 *
 * <p><b>The wood list is vanilla's twelve</b>, the same roster the gates carry,
 * each paired with its vanilla {@link Items} fence. <b>Nothing reads that fence
 * any more</b> - it was the creative-menu anchor until the jumps left vanilla's
 * Building Blocks for a tab of their own - and it is kept because it is still a
 * <em>compile</em> error if a wood is misspelled, which is the whole point of
 * writing the list as constants rather than composing {@code name + "_fence"}.
 * {@code tools/bake-jumps.mjs} carries the same twelve and the two must agree:
 * a wood here with no generated blockstate is a purple chequerboard, and a wood
 * there with no block registered is a file the game never reads.
 *
 * @see JumpBlock for the geometry, the connection rule, and why height is
 *      stacking rather than a property
 */
public final class Jumps {

    /** The suffix every jump's id carries. */
    private static final String STYLE = "jump";

    /**
     * One registered jump: the wood, the vanilla fence it is crafted from and
     * filed after in the creative menu, and the two registry entries.
     */
    public record Jump(WoodType wood,
                       java.util.function.Supplier<Item> sourceFence,
                       String plankId,
                       DeferredBlock<JumpBlock> block,
                       List<DeferredItem<BlockItem>> items) {

        /** The vertical, which is the style the plain {@code <wood>_jump} item places. */
        public DeferredItem<BlockItem> item() {
            return this.items.get(0);
        }
    }

    /** Vanilla's twelve, each beside the fence it is built from. */
    private static final Object[][] WOODS = {
            {WoodType.OAK, Items.OAK_FENCE},
            {WoodType.SPRUCE, Items.SPRUCE_FENCE},
            {WoodType.BIRCH, Items.BIRCH_FENCE},
            {WoodType.JUNGLE, Items.JUNGLE_FENCE},
            {WoodType.ACACIA, Items.ACACIA_FENCE},
            {WoodType.DARK_OAK, Items.DARK_OAK_FENCE},
            {WoodType.PALE_OAK, Items.PALE_OAK_FENCE},
            {WoodType.MANGROVE, Items.MANGROVE_FENCE},
            {WoodType.CHERRY, Items.CHERRY_FENCE},
            {WoodType.BAMBOO, Items.BAMBOO_FENCE},
            {WoodType.CRIMSON, Items.CRIMSON_FENCE},
            {WoodType.WARPED, Items.WARPED_FENCE},
    };

    private static final List<Jump> JUMPS = new ArrayList<>();

    /**
     * Every {@code name} already taken.
     *
     * <p><b>Declared above the static block, and it has to be</b> - static
     * initialisers run in source order, and {@link DoubleGates} crashed mod
     * construction with an NPE out of its class initialiser by having this
     * field below. Same field, same reason, same placement.
     */
    private static final java.util.Set<String> REGISTERED = new java.util.HashSet<>();

    static {
        for (Object[] row : WOODS) {
            WoodType wood = (WoodType) row[0];
            Item vanillaFence = (Item) row[1];
            register(wood.name() + "_" + STYLE, () -> vanillaFence,
                    "minecraft:" + wood.name() + "_planks");
        }
        // ...and one for every wood another mod brought. Read out of those
        // mods' own jars before anything registers - see compat/ModdedMaterials
        // for why it cannot be read off the registry - and sorted, so two
        // players with the same mods register the same jumps in the same order
        // whatever order FML loaded them in.
        for (var wood : com.example.horsegenetics.neoforge.compat.ModdedMaterials.woods()) {
            net.minecraft.resources.Identifier source =
                    net.minecraft.resources.Identifier.parse(wood.fenceId());
            // The plank ITEM id, derived from the plank TEXTURE id the scan
            // recorded - "ns:block/fir_planks" names the item "ns:fir_planks".
            // Composed rather than scanned, like Wood.fenceId, so every use of
            // it has to tolerate the item not existing.
            register(wood.jumpId(),
                    () -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(source),
                    wood.plankTexture().replace("block/", ""));
        }
    }

    private static void register(String name, java.util.function.Supplier<Item> sourceFence,
                                 String plankId) {
        // Same guard, same reason, as DoubleGates' and compat/ModdedArmour's:
        // this runs in a static initialiser on ids built out of other mods'
        // jars, and a DeferredRegister throws on a duplicate. One missing jump
        // beats a mod that will not load.
        if (!REGISTERED.add(name)) {
            com.example.horsegenetics.neoforge.HorseGenetics.LOGGER.warn(
                    "compat: two woods both want the jump id {} - the second gets none", name);
            return;
        }
        DeferredBlock<JumpBlock> block = ModBlocks.BLOCKS.registerBlock(
                name,
                JumpBlock::new,
                // Vanilla fence strength and wood sound. noOcclusion() because
                // it is nowhere near a full cube: without it the faces of the
                // blocks behind a jump are culled and a course is full of
                // holes.
                () -> BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)
                        .noOcclusion()
                        .ignitedByLava());

        // ONE ITEM PER STYLE, all placing this one block.
        //
        // Style is a blockstate property, so a single BlockItem only ever
        // places the default - which is exactly what the owner found: "I only
        // see the vertical in the creative tab". Several BlockItems may point
        // at one Block and differ only in the state they place. See JumpItem.
        //
        // The VERTICAL keeps the bare `<wood>_jump` id and takes its name from
        // the block, via useBlockDescriptionPrefix(): BlockItem in 26.1.2 does
        // NOT defer its description id to its block, so without that line a
        // `block.*` lang key is never consulted. The other styles need names of
        // their own ("Oak Oxer", not a second "Oak Jump"), so they are NOT
        // given the prefix and carry `item.horsegenetics.*` keys instead -
        // written by bake-jumps.mjs.
        List<DeferredItem<BlockItem>> items = new ArrayList<>();
        for (JumpBlock.Style style : JumpBlock.Style.values()) {
            boolean vertical = style == JumpBlock.Style.VERTICAL;
            String itemName = vertical ? name : name + "_" + style.getSerializedName();
            items.add(ModItems.ITEMS.registerItem(itemName,
                    p -> new com.example.horsegenetics.neoforge.item.JumpItem(
                            block.get(), style,
                            vertical ? p.useBlockDescriptionPrefix() : p)));
        }

        JUMPS.add(new Jump(wood(name), sourceFence, plankId, block, List.copyOf(items)));
    }

    /**
     * The {@link WoodType} is carried only so the creative-tab pass can group
     * by it; modded woods have none of vanilla's, and oak is the placeholder
     * there exactly as it is in {@link DoubleGates}.
     */
    private static WoodType wood(String name) {
        for (Object[] row : WOODS) {
            if ((((WoodType) row[0]).name() + "_" + STYLE).equals(name)) {
                return (WoodType) row[0];
            }
        }
        return WoodType.OAK;
    }

    /**
     * The jump block made of the wood whose planks these are, or null.
     *
     * <p>Backs the plank-recolour interaction in {@link JumpBlock}. Built once,
     * lazily, on first use rather than in the static block: the plank ITEMS are
     * other people's registry entries and are not there yet while this class is
     * registering its own.
     */
    public static @org.jetbrains.annotations.Nullable JumpBlock byPlank(Item plank) {
        if (BY_PLANK == null) {
            java.util.Map<Item, JumpBlock> map = new java.util.HashMap<>();
            for (Jump jump : JUMPS) {
                net.minecraft.resources.Identifier id =
                        net.minecraft.resources.Identifier.tryParse(jump.plankId());
                if (id == null) {
                    continue;
                }
                Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                // A composed id that names nothing comes back as AIR, and a wood
                // whose planks are missing simply cannot be painted on.
                if (item != null && item != Items.AIR) {
                    map.putIfAbsent(item, jump.block().get());
                }
            }
            BY_PLANK = map;
        }
        return BY_PLANK.get(plank);
    }

    private static volatile java.util.@org.jetbrains.annotations.Nullable Map<Item, JumpBlock> BY_PLANK;

    /** Every jump: vanilla's twelve in vanilla's wood order, then the modded ones by id. */
    public static List<Jump> jumps() {
        return List.copyOf(JUMPS);
    }

    /** How many there are. Derived, so nothing has to quote a number. */
    public static int count() {
        return JUMPS.size();
    }

    /**
     * Touching this class runs the static block that registers all of it, which
     * must happen before {@code ModBlocks.register} attaches the register to
     * the mod event bus. Called from {@code HorseGenetics}.
     */
    public static void init() {
        // Intentionally empty; see the note above.
    }

    private Jumps() {
    }
}
