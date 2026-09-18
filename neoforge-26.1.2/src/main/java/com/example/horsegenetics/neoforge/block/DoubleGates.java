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
 * <b>The twelve double-wide gates</b>, one per wood, and the one place that
 * knows the list.
 *
 * <p>Registered in a loop rather than as twenty-four hand-written fields.
 * Nothing here varies per wood except the {@link WoodType}, the name and the
 * vanilla gate it is crafted from and filed beside, and the moment a list like
 * this is written out by hand it acquires a wood that is in the blocks and not
 * the items, or an item pointing at its neighbour's block. The loop cannot do
 * either.
 *
 * <p><b>The wood list is vanilla's, read off the 26.1.2 client jar</b>
 * ({@code assets/minecraft/blockstates/*_fence_gate.json}) rather than
 * remembered, and {@code tools/bake-double-gates.mjs} carries the same twelve.
 * The two must agree: a wood here with no generated blockstate is a purple
 * chequerboard, and a wood there with no block registered is a file the game
 * never reads. Pairing each with its vanilla {@link Items} constant is what
 * makes that a <em>compile</em> error rather than a silent one.
 *
 * @see DoubleFenceGateBlock for how a pair is placed, opened and broken
 */
public final class DoubleGates {

    /**
     * The suffix every gate's id carries.
     *
     * <p><b>There was a second style and it was dropped.</b> A "double farm
     * gate" - the same block with four rails instead of two - shipped for a few
     * hours, because the mod that prompted all of this advertised "farm-style
     * variant gates" in four words, with no screenshot, no source and no way to
     * run it. Its appearance was therefore guessed rather than reproduced, and
     * the owner's verdict on the guess was "what is the double farm gate? It
     * looks weird". Two gates whose difference nobody can justify is worse than
     * one gate, so there is one.
     */
    private static final String STYLE = "double_fence_gate";

    /**
     * One registered gate: the wood it is made of, the vanilla gate it is
     * crafted from and filed after in the creative menu, and the two registry
     * entries.
     */
    public record Gate(WoodType wood,
                       Item vanillaGate,
                       DeferredBlock<DoubleFenceGateBlock> block,
                       DeferredItem<BlockItem> item) {
    }

    /** Vanilla's twelve, each beside the gate it is built from. */
    private static final Object[][] WOODS = {
            {WoodType.OAK, Items.OAK_FENCE_GATE},
            {WoodType.SPRUCE, Items.SPRUCE_FENCE_GATE},
            {WoodType.BIRCH, Items.BIRCH_FENCE_GATE},
            {WoodType.JUNGLE, Items.JUNGLE_FENCE_GATE},
            {WoodType.ACACIA, Items.ACACIA_FENCE_GATE},
            {WoodType.DARK_OAK, Items.DARK_OAK_FENCE_GATE},
            {WoodType.PALE_OAK, Items.PALE_OAK_FENCE_GATE},
            {WoodType.MANGROVE, Items.MANGROVE_FENCE_GATE},
            {WoodType.CHERRY, Items.CHERRY_FENCE_GATE},
            {WoodType.BAMBOO, Items.BAMBOO_FENCE_GATE},
            {WoodType.CRIMSON, Items.CRIMSON_FENCE_GATE},
            {WoodType.WARPED, Items.WARPED_FENCE_GATE},
    };

    private static final List<Gate> GATES = new ArrayList<>();

    static {
        for (Object[] row : WOODS) {
            WoodType wood = (WoodType) row[0];
            Item vanillaGate = (Item) row[1];
            String name = wood.name() + "_" + STYLE;

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

            // useBlockDescriptionPrefix() or every one of these is named from an
            // `item.horsegenetics.*` key that does not exist. Item.Properties
            // defaults its descriptionId to the ITEM prefix, and BlockItem in
            // 26.1.2 does NOT override getDescriptionId() to defer to its block -
            // so a block.* lang key is simply never consulted. The mod's other
            // block items paper over this by shipping the name under both
            // prefixes; one line here beats twelve duplicate keys.
            // DoubleGateItem rather than a plain BlockItem: it exists to say why
            // a gate refused to go down when neither side had room, which used to
            // be silent. See that class.
            DeferredItem<BlockItem> item = ModItems.ITEMS.registerItem(name,
                    p -> new com.example.horsegenetics.neoforge.item.DoubleGateItem(
                            block.get(), p.useBlockDescriptionPrefix()));

            GATES.add(new Gate(wood, vanillaGate, block, item));
        }
    }

    /** Every gate, in vanilla's wood order. */
    public static List<Gate> gates() {
        return List.copyOf(GATES);
    }

    /** How many there are. Derived, so nothing has to quote a number. */
    public static int count() {
        return GATES.size();
    }

    /**
     * Touching this class runs the static block that registers all of it. The
     * registers themselves are {@code ModBlocks.BLOCKS} and
     * {@code ModItems.ITEMS}, which are attached to the mod event bus in the
     * usual place - this only has to happen before that, which class-loading
     * order guarantees.
     */
    public static void init() {
        // Intentionally empty; see the note above.
    }

    private DoubleGates() {
    }
}
