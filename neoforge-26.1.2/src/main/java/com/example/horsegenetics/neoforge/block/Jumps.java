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
 * each paired with its vanilla {@link Items} fence. That pairing is doing two
 * jobs: it is the creative-menu anchor the jump is filed after, and it is a
 * <em>compile</em> error if a wood is misspelled - which is the whole point of
 * writing it as constants rather than composing {@code name + "_fence"}.
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
                       DeferredBlock<JumpBlock> block,
                       DeferredItem<BlockItem> item) {
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
            register(wood.name() + "_" + STYLE, () -> vanillaFence);
        }
        // ...and one for every wood another mod brought. Read out of those
        // mods' own jars before anything registers - see compat/ModdedMaterials
        // for why it cannot be read off the registry - and sorted, so two
        // players with the same mods register the same jumps in the same order
        // whatever order FML loaded them in.
        for (var wood : com.example.horsegenetics.neoforge.compat.ModdedMaterials.woods()) {
            net.minecraft.resources.Identifier source =
                    net.minecraft.resources.Identifier.parse(wood.fenceId());
            register(wood.jumpId(),
                    () -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(source));
        }
    }

    private static void register(String name, java.util.function.Supplier<Item> sourceFence) {
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

        // useBlockDescriptionPrefix() or every one of these is named from an
        // `item.horsegenetics.*` key that does not exist - BlockItem in 26.1.2
        // does NOT defer its description id to its block, so a `block.*` lang
        // key is simply never consulted. Same line, same reason, as the gates'.
        DeferredItem<BlockItem> item = ModItems.ITEMS.registerItem(name,
                p -> new BlockItem(block.get(), p.useBlockDescriptionPrefix()));

        JUMPS.add(new Jump(wood(name), sourceFence, block, item));
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
