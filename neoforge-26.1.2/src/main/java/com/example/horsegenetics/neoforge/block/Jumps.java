package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.item.JumpItem;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * <b>The jump item.</b> One of them, for the one {@link ModBlocks#JUMP} block.
 *
 * <h2>This class used to register twelve blocks and thirty-six items</h2>
 * One jump block per wood, three items each, plus a blockstate, a loot table
 * and a recipe apiece - and a duplicate-id guard, because the roster picked up
 * every wood another mod brought with it. It went to three items when the woods
 * became {@link JumpBlockEntity} data, and then to <b>one</b> (owner,
 * 2026-09-20): <i>"let's just combine this all into one horse jump item, and
 * we'll add new modular jumps through there"</i>.
 *
 * <p>That is the whole shape of the thing now. A jump is <b>one item</b> and
 * everything about a particular jump - its two woods, its style, and whatever
 * is added next - is chosen <i>after</i> it is placed, in the block's own
 * screen. Every future style is then a button, not an item: the roster stops
 * growing, and a course is built by putting down jumps and dressing them.
 *
 * <h2>The cost of that is discoverability, and it is paid for</h2>
 * An item that is customised somewhere else has nothing about it to say so. So
 * {@link #hint} puts a line on the action bar the first times one is placed,
 * and stops for good once the player has actually opened a screen. See
 * {@link ModAttachments#JUMP_SCREEN_KNOWN}.
 *
 * <p>The wood roster did <em>not</em> go with the blocks - a jump can still be
 * made of any wood any installed mod added. It lives in {@link JumpWoods}.
 *
 * @see JumpBlock for the geometry, the connection rule, and why height is
 *      stacking rather than a property
 */
public final class Jumps {

    /**
     * The item's id. Its description id is therefore
     * {@code item.horsegenetics.jump}, and <b>not</b> the block's - none of the
     * jump items has ever used {@code useBlockDescriptionPrefix()}, because
     * {@link JumpItem#getName} builds every name from the woods on the stack
     * and wants one predictable key.
     */
    private static final String NAME = "jump";

    private static final DeferredItem<JumpItem> ITEM =
            ModItems.ITEMS.registerItem(NAME, p -> new JumpItem(ModBlocks.JUMP.get(), p));

    /** The one jump item. */
    public static Item item() {
        return ITEM.get();
    }

    /**
     * <b>Tell this player how to customise a jump</b>, at most until they have.
     *
     * <p>On the <b>action bar</b>, which is vanilla's own place for a line that
     * is worth reading once and worth nothing afterwards - it is where a
     * lodestone compass, a dismount prompt and a locked-chest refusal all go.
     * Not chat: chat is a log, and a hint that accumulates in the log while
     * somebody builds a forty-jump course is litter.
     *
     * <p>Shown on <em>placement</em> rather than on first craft, because that
     * is the moment the player is looking at the jump and could act on it.
     */
    public static void hint(Player player) {
        if (player.getData(ModAttachments.JUMP_SCREEN_KNOWN)) {
            return;
        }
        // sendOverlayMessage, NOT sendSystemMessage: 26.1.2 renamed the
        // action-bar call and the boolean-flagged displayClientMessage this
        // would have been written as in any older version is gone.
        player.sendOverlayMessage(Component.translatable("horsegenetics.jump.hint"));
    }

    /**
     * They opened a screen, so they know. Called from the block rather than
     * from the menu so that it fires on the interaction itself, whether or not
     * they then change anything.
     */
    public static void learned(Player player) {
        if (!player.getData(ModAttachments.JUMP_SCREEN_KNOWN)) {
            player.setData(ModAttachments.JUMP_SCREEN_KNOWN, Boolean.TRUE);
        }
    }

    /**
     * Touching this class runs the static initialiser that registers the item,
     * which must happen before {@code ModItems.register} attaches the register
     * to the mod event bus. Called from {@code HorseGenetics}.
     */
    public static void init() {
        // Intentionally empty; see the note above.
    }

    private Jumps() {
    }
}
