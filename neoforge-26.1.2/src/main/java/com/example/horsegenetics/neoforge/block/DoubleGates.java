package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>The twenty-four double-wide gates</b> - twelve woods in two styles - and
 * the one place that knows the list.
 *
 * <p>Registered in a loop rather than as forty-eight hand-written fields. Nothing
 * here varies per wood except the {@link WoodType} and the name, and the moment a
 * list like this is written out by hand it acquires a wood that is in the blocks
 * and not the items, or an item pointing at its neighbour's block. The loop
 * cannot do either.
 *
 * <p><b>The wood list is vanilla's, read off the 26.1.2 client jar</b>
 * ({@code assets/minecraft/blockstates/*_fence_gate.json}) rather than
 * remembered, and {@code tools/bake-double-gates.mjs} carries the same twelve in
 * the same order. The two lists must agree: a wood here with no generated
 * blockstate is a purple chequerboard, and a wood there with no block registered
 * is a file the game never reads. They are short enough to compare by eye and
 * {@code missingno} in the creative tab is the loud failure if they drift.
 *
 * @see DoubleFenceGateBlock for how a pair is placed, opened and broken
 */
public final class DoubleGates {

    /** Vanilla's twelve. Order matches the generator's {@code WOODS} table. */
    private static final WoodType[] WOODS = {
            WoodType.OAK,
            WoodType.SPRUCE,
            WoodType.BIRCH,
            WoodType.JUNGLE,
            WoodType.ACACIA,
            WoodType.DARK_OAK,
            WoodType.PALE_OAK,
            WoodType.MANGROVE,
            WoodType.CHERRY,
            WoodType.BAMBOO,
            WoodType.CRIMSON,
            WoodType.WARPED,
    };

    /**
     * The two styles. They differ only in how many rails the model draws - the
     * block behaviour is identical - so the style is a name here and geometry in
     * the generator, and nothing in Java branches on it.
     */
    private static final String[] STYLES = {"double_fence_gate", "double_farm_gate"};

    private static final Map<String, DeferredBlock<DoubleFenceGateBlock>> BLOCKS = new LinkedHashMap<>();
    private static final List<DeferredItem<BlockItem>> ITEMS = new ArrayList<>();

    static {
        for (WoodType wood : WOODS) {
            for (String style : STYLES) {
                String name = wood.name() + "_" + style;
                DeferredBlock<DoubleFenceGateBlock> block = ModBlocks.BLOCKS.registerBlock(
                        name,
                        properties -> new DoubleFenceGateBlock(wood, properties),
                        // Vanilla's own fence gate properties. The sound is NOT set
                        // here: FenceGateBlock's (WoodType, Properties) constructor
                        // applies the wood's own soundType itself, so setting one
                        // would only be a chance to set the wrong one.
                        () -> BlockBehaviour.Properties.of()
                                .mapColor(MapColor.WOOD)
                                .strength(2.0F, 3.0F)
                                .sound(SoundType.WOOD)
                                .ignitedByLava()
                                .forceSolidOn());
                BLOCKS.put(name, block);
                // useBlockDescriptionPrefix() or every one of these is named from
                // an `item.horsegenetics.*` key that does not exist. Item.Properties
                // defaults its descriptionId to the ITEM prefix, and BlockItem in
                // 26.1.2 does NOT override getDescriptionId() to defer to its block -
                // so a block.* lang key is simply never consulted. The mod's other
                // block items paper over this by shipping the name under both
                // prefixes; one line here beats twenty-four duplicate keys.
                ITEMS.add(ModItems.ITEMS.registerItem(name,
                        p -> new BlockItem(block.get(), p.useBlockDescriptionPrefix())));
            }
        }
    }

    /** Every gate's item, in wood-then-style order, for the creative tab. */
    public static List<DeferredItem<BlockItem>> items() {
        return ITEMS;
    }

    /** How many gates there are. Derived, so nothing has to quote a number. */
    public static int count() {
        return BLOCKS.size();
    }

    /**
     * Touching this class runs the static block that registers all of it. The
     * registers themselves are {@code ModBlocks.BLOCKS} and {@code ModItems.ITEMS},
     * which are attached to the mod event bus in the usual place - this only has
     * to happen before that, which class-loading order guarantees.
     */
    public static void init() {
        // Intentionally empty; see the note above.
    }

    private DoubleGates() {
    }
}
