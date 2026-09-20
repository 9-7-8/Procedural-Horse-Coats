package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.block.JumpBlock;
import com.example.horsegenetics.neoforge.block.JumpBlockEntity;
import com.example.horsegenetics.neoforge.block.JumpMaterials;
import com.example.horsegenetics.neoforge.block.JumpWoods;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>The jump's screen.</b> Two plank slots - the rails and the standards - and
 * three style buttons.
 *
 * <h2>Everything about a placed jump is edited here</h2>
 * This replaced two right-click interactions (owner, 2026-09-20): a stick used
 * to cycle the style and a plank used to repaint the whole block, each spending
 * its item. Both were invented when a jump was <i>twelve blocks</i>, one per
 * wood, and a repaint was a swap to a sibling block. Two woods on one jump
 * cannot be a block each, so the woods became block-entity data, and once they
 * are data a window is the honest way to edit them.
 *
 * <h2>The exchange, which is the whole idea</h2>
 * Put a plank in a slot and the jump takes that wood <b>immediately</b>: one
 * plank is spent and <b>a plank of the wood it used to be is handed back</b>.
 * So restyling a built course costs nothing net and is reversible, which is the
 * opposite of the rule the old interaction followed - it ate the plank, and the
 * argument was that a course is a thing you build rather than fiddle with. The
 * owner's design overrules it: a one-for-one swap is a decision you can take
 * back, and a course you cannot re-dress is a course you rebuild instead.
 *
 * <p>The rest of the stack stays in the slot, and is handed back on close like
 * the {@link EquestrianBenchMenu}'s inputs are. A window that eats what is left
 * in it is a window nobody opens twice.
 *
 * <h2>No result slot, and no state of its own</h2>
 * The block entity is the only storage; this is a window onto it, like a
 * furnace's. Closing the screen mid-anything loses nothing because there is no
 * "anything" - every click has already landed on the block.
 *
 * @see JumpBlockEntity for the data and the re-mesh that makes a change visible
 */
public final class JumpMenu extends AbstractContainerMenu {

    public static final int SLOT_RAILS = 0;
    public static final int SLOT_STANDARDS = 1;
    private static final int SLOT_COUNT = 2;

    /** Button ids, which are {@link JumpBlock.Style} ordinals. See {@link #clickMenuButton}. */
    public static final int STYLE_BUTTONS = 3;

    // ------------------------------------------------------------------
    // Layout, owned here so the screen has one place to read it from -
    // the same arrangement the other two menus in this mod use.
    // ------------------------------------------------------------------

    public static final int WIDTH = 176;
    public static final int HEIGHT = 194;
    public static final int MARGIN = 8;
    public static final int TITLE_Y = 6;

    public static final int SLOT_X = 8;
    public static final int RAILS_Y = 20;
    public static final int STANDARDS_Y = 42;
    /** Where the "Rails"/"Standards" caption and the wood's name sit. */
    public static final int LABEL_X = 30;

    public static final int STYLE_LABEL_Y = 68;
    public static final int BUTTON_Y = 78;
    public static final int BUTTON_W = 54;
    public static final int BUTTON_H = 20;
    /** The three style buttons' left edges. */
    public static final int[] BUTTON_X = {8, 62, 116};

    public static final int INV_LABEL_Y = 100;
    public static final int INV_Y = 112;

    // ------------------------------------------------------------------
    // Synced state. Three ints, because the client's copy of this menu is
    // built by a MenuType that hands it no position and no block entity.
    //
    // Woods travel as INDICES INTO JumpWoods.keys(), not as strings: a
    // ContainerData is a vector of ints and that is all it is. The list is
    // identical on both sides - it is read out of the installed jars rather
    // than off a registry, which is exactly the property compat/ModdedMaterials
    // exists to give. An index that does not resolve falls back to the default
    // wood rather than throwing.
    // ------------------------------------------------------------------

    public static final int DATA_STYLE = 0;
    public static final int DATA_RAILS = 1;
    public static final int DATA_STANDARDS = 2;
    private static final int DATA_COUNT = 3;

    private final Player player;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    /**
     * Guards {@link #slotsChanged} against itself.
     *
     * <p>The exchange writes back into the very container whose change started
     * it - {@code shrink} then {@code setItem} - and {@code SimpleContainer}
     * calls {@code setChanged} on the way out, so without this the swap runs a
     * second time on its own result. The second pass is harmless today (the
     * slot now holds the wood the block already is, so it declines), but it is
     * harmless <i>by luck</i>, and the next condition added to {@link #swap}
     * gets to discover that.
     */
    private boolean swapping;

    private final Container input = new SimpleContainer(SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            JumpMenu.this.slotsChanged(this);
        }
    };

    /** Client constructor - the menu type hands us no world access. */
    public JumpMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL, new net.minecraft.world.inventory.SimpleContainerData(DATA_COUNT));
    }

    public JumpMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        this(containerId, inventory, access, null);
    }

    private JumpMenu(int containerId, Inventory inventory, ContainerLevelAccess access,
                     @Nullable ContainerData clientData) {
        super(ModMenus.JUMP.get(), containerId);
        this.player = inventory.player;
        this.access = access;
        this.data = clientData != null ? clientData : liveData(access);

        addSlot(plankSlot(SLOT_RAILS, RAILS_Y));
        addSlot(plankSlot(SLOT_STANDARDS, STANDARDS_Y));
        addStandardInventorySlots(inventory, MARGIN, INV_Y);
        addDataSlots(this.data);
    }

    /**
     * The server's three ints, read off the world every time they are polled.
     *
     * <p>Live rather than cached on purpose: the style lives in the
     * <i>blockstate</i> and the woods in the <i>block entity</i>, and both can
     * be changed by something other than this menu - a second player's screen,
     * a command, a piston one day. {@code broadcastChanges} diffs these against
     * what the client last saw, so reading them fresh costs three field reads a
     * tick and keeps two open screens honest with each other.
     */
    private static ContainerData liveData(ContainerLevelAccess access) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return access.evaluate((level, pos) -> {
                    BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof JumpBlock)) {
                        return 0;
                    }
                    if (index == DATA_STYLE) {
                        return state.getValue(JumpBlock.STYLE).ordinal();
                    }
                    JumpMaterials materials = materialsAt(level, pos);
                    String wood = index == DATA_RAILS ? materials.rails() : materials.standards();
                    return Math.max(0, JumpWoods.keys().indexOf(wood));
                }, 0);
            }

            @Override
            public void set(int index, int value) {
                // Nothing to write back. The client never sets these - every
                // change goes through a slot or clickMenuButton, which land on
                // the block and come back round through get().
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static JumpMaterials materialsAt(net.minecraft.world.level.Level level,
                                             net.minecraft.core.BlockPos pos) {
        return level.getBlockEntity(pos) instanceof JumpBlockEntity jump
                ? jump.materials()
                : JumpMaterials.DEFAULT;
    }

    /**
     * A slot that takes a plank of any wood a jump can be made of, and nothing
     * else.
     *
     * <p>{@code JumpWoods.keyOfPlank} is the entire test, which is why it is
     * also the entire test in {@link #quickMoveStack}: one predicate, so
     * shift-clicking can never put something in a slot a click could not.
     */
    private Slot plankSlot(int index, int y) {
        return new Slot(input, index, SLOT_X, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return JumpWoods.keyOfPlank(stack.getItem()) != null;
            }
        };
    }

    // --- the exchange -------------------------------------------------------

    /**
     * A plank went into a slot: take the wood, spend one, hand the old one back.
     *
     * <p>Server only - the client's copy of this menu has
     * {@link ContainerLevelAccess#NULL} and no block entity to write to, and
     * doing the arithmetic there as well would double-spend the plank the
     * moment the server's answer arrived.
     */
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (this.access == ContainerLevelAccess.NULL || this.swapping) {
            return;
        }
        this.swapping = true;
        try {
            this.access.execute((level, pos) -> {
                if (!(level.getBlockEntity(pos) instanceof JumpBlockEntity jump)) {
                    return;
                }
                boolean swapped = swap(jump, SLOT_RAILS, true) | swap(jump, SLOT_STANDARDS, false);
                if (swapped) {
                    level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            });
        } finally {
            this.swapping = false;
        }
        broadcastChanges();
    }

    /**
     * One half of the jump, one plank.
     *
     * @param rails which half - true for the poles a horse jumps, false for the
     *              uprights
     * @return whether anything actually changed, so the caller plays one sound
     *         for a double swap rather than two
     */
    private boolean swap(JumpBlockEntity jump, int slot, boolean rails) {
        ItemStack offered = this.input.getItem(slot);
        if (offered.isEmpty()) {
            return false;
        }
        String wood = JumpWoods.keyOfPlank(offered.getItem());
        JumpMaterials now = jump.materials();
        String current = rails ? now.rails() : now.standards();
        // A plank of the wood it already is sits in the slot doing nothing and
        // is handed back on close. Refusing to spend it is the same courtesy
        // the old plank-repaint paid: better than eating one for no change.
        if (wood == null || wood.equals(current)) {
            return false;
        }

        jump.setMaterials(rails ? now.withRails(wood) : now.withStandards(wood));
        offered.shrink(1);
        this.input.setItem(slot, offered.isEmpty() ? ItemStack.EMPTY : offered);

        // THE DISPLACED PLANK. Into the player's inventory rather than back into
        // the slot, because the slot usually still holds the rest of the stack
        // they just put in and two woods cannot share one slot.
        // placeItemBackInInventory drops it at their feet if there is no room,
        // so the wood is never destroyed.
        Item plank = JumpWoods.plankOf(current);
        if (plank != null) {
            this.player.getInventory().placeItemBackInInventory(new ItemStack(plank));
        }
        return true;
    }

    /**
     * <b>The three style buttons.</b> The id is the {@link JumpBlock.Style}
     * ordinal, which is what the screen sends.
     *
     * <p>Free, unlike the stick it replaced. Style is cosmetic - every style
     * collides identically, which is the only reason a stack of three means the
     * same thing whatever it is built from - so charging for it was charging for
     * a change of mind about decoration.
     *
     * <p>{@code setBlockAndUpdate} rather than a raw state write, so the two
     * connection flags on the neighbours get their chance to notice: the oxer's
     * standards are deeper than the vertical's, and a run that did not re-update
     * would keep the old block's posts.
     */
    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (id < 0 || id >= STYLE_BUTTONS) {
            return false;
        }
        JumpBlock.Style style = JumpBlock.Style.values()[id];
        this.access.execute((level, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof JumpBlock && state.getValue(JumpBlock.STYLE) != style) {
                level.setBlockAndUpdate(pos, state.setValue(JumpBlock.STYLE, style));
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        });
        return true;
    }

    // --- what the screen reads ----------------------------------------------

    /** The style the block is wearing, as both sides see it. */
    public JumpBlock.Style style() {
        int ordinal = this.data.get(DATA_STYLE);
        JumpBlock.Style[] all = JumpBlock.Style.values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : JumpBlock.Style.VERTICAL;
    }

    /** The wood key of the rails, or the default if the index does not resolve. */
    public String railsWood() {
        return wood(DATA_RAILS);
    }

    /** The wood key of the standards. */
    public String standardsWood() {
        return wood(DATA_STANDARDS);
    }

    private String wood(int index) {
        List<String> keys = JumpWoods.keys();
        int i = this.data.get(index);
        return i >= 0 && i < keys.size() ? keys.get(i) : JumpMaterials.DEFAULT_WOOD;
    }

    // --- housekeeping -------------------------------------------------------

    /**
     * Shift-click. Out of the slots into the player; from the player, a plank
     * finds the first free plank slot and everything else goes nowhere.
     */
    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (JumpWoods.keyOfPlank(stack.getItem()) != null) {
            if (!moveItemStackTo(stack, SLOT_RAILS, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** Give the leftovers back rather than eating them when the screen closes. */
    @Override
    public void removed(Player who) {
        super.removed(who);
        this.access.execute((level, pos) -> clearContainer(who, this.input));
    }

    @Override
    public boolean stillValid(Player who) {
        return stillValid(this.access, who, ModBlocks.JUMP.get());
    }
}
