package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.item.JumpItem;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <b>The three jump items</b> - one per style, all placing the one
 * {@link ModBlocks#JUMP} block.
 *
 * <h2>This class used to register twelve blocks and thirty-six items</h2>
 * One jump block per wood, three items each, plus a blockstate, a loot table
 * and a recipe apiece - and a duplicate-id guard, because the roster picked up
 * every wood another mod brought with it. All of that is gone (2026-09-20). The
 * woods became {@link JumpBlockEntity} data so that the rails and the standards
 * could differ, and a wood that is data needs no block, no item and no id.
 *
 * <p>What is left is the one thing a block entity cannot carry: <b>style</b>.
 * It changes the geometry, so it is a blockstate property, so a single
 * {@code BlockItem} would only ever place the default - which is exactly what
 * the owner found the first time: "I only see the vertical in the creative
 * tab". Several {@code BlockItem}s may point at one {@code Block} and differ
 * only in the state they place. See {@link JumpItem}.
 *
 * <p>The wood roster did <em>not</em> go with the blocks - a jump can still be
 * made of any wood any installed mod added. It moved to {@link JumpWoods},
 * which is a list of keys and the plank that stands for each, and is read by
 * the model, the screen and the recipes.
 *
 * @see JumpBlock for the geometry, the connection rule, and why height is
 *      stacking rather than a property
 */
public final class Jumps {

    /**
     * The id the vertical keeps; the other two hang their style off it.
     *
     * <p><b>None of the three uses {@code useBlockDescriptionPrefix()}</b>, so
     * all three description ids are {@code item.horsegenetics.*}. That is
     * deliberate: {@link JumpItem#getName} builds every name from the two woods
     * on the stack, and it wants one predictable key per item rather than one
     * item deferring to a {@code block.*} key and two not.
     */
    private static final String BASE = "jump";

    private static final Map<JumpBlock.Style, DeferredItem<JumpItem>> ITEMS =
            new EnumMap<>(JumpBlock.Style.class);

    static {
        for (JumpBlock.Style style : JumpBlock.Style.values()) {
            String name = style == JumpBlock.Style.VERTICAL
                    ? BASE
                    : BASE + "_" + style.getSerializedName();
            ITEMS.put(style, ModItems.ITEMS.registerItem(name,
                    p -> new JumpItem(ModBlocks.JUMP.get(), style, p)));
        }
    }

    /** The item that places this style. */
    public static Item item(JumpBlock.Style style) {
        return ITEMS.get(style).get();
    }

    /** All three, in {@link JumpBlock.Style} order - the order the tab lists them in. */
    public static List<Item> items() {
        List<Item> all = new java.util.ArrayList<>(ITEMS.size());
        for (JumpBlock.Style style : JumpBlock.Style.values()) {
            all.add(item(style));
        }
        return List.copyOf(all);
    }

    /**
     * Touching this class runs the static block that registers the three items,
     * which must happen before {@code ModItems.register} attaches the register
     * to the mod event bus. Called from {@code HorseGenetics}.
     */
    public static void init() {
        // Intentionally empty; see the note above.
    }

    private Jumps() {
    }
}
