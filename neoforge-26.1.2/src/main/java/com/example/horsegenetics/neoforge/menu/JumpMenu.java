package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.block.JumpBlock;
import com.example.horsegenetics.neoforge.block.JumpBlockEntity;
import com.example.horsegenetics.neoforge.block.JumpMaterials;
import com.example.horsegenetics.neoforge.block.JumpWoods;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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

    /** Button ids 0-2, which are {@link JumpBlock.Style} ordinals. See {@link #clickMenuButton}. */
    public static final int STYLE_BUTTONS = 3;

    /** Button id 3: one rung shorter. */
    public static final int BUTTON_SHORTER = STYLE_BUTTONS;

    /** Button id 4: one rung taller. */
    public static final int BUTTON_TALLER = STYLE_BUTTONS + 1;

    // ------------------------------------------------------------------
    // Layout, owned here so the screen has one place to read it from -
    // the same arrangement the other two menus in this mod use.
    // ------------------------------------------------------------------

    public static final int WIDTH = 176;
    public static final int HEIGHT = 224;
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

    // The height row. TWO LINES OF TEXT ON THE LEFT, both buttons hard right -
    // one line beside the buttons overlapped them the moment the reading grew
    // to "0.3 blocks - 0.8 to clear", which is most of the ladder.
    public static final int SIZE_LABEL_Y = 104;
    /** The second line, under the first: what a horse actually has to clear. */
    public static final int SIZE_CLEAR_Y = SIZE_LABEL_Y + 11;
    /** Where the height itself is drawn, just right of its caption. */
    public static final int SIZE_VALUE_X = 46;
    public static final int SIZE_BUTTON_Y = 102;
    public static final int SIZE_BUTTON_W = 20;
    public static final int SIZE_BUTTON_H = 20;
    public static final int SIZE_MINUS_X = 124;
    public static final int SIZE_PLUS_X = 148;

    public static final int INV_LABEL_Y = 130;
    public static final int INV_Y = 142;

    // ------------------------------------------------------------------
    // WHAT THE CLIENT KNOWS, AND HOW.
    //
    // It gets one thing: the block's POSITION, written into the menu's opening
    // packet. Everything the screen draws is then read straight off the block
    // at that position - the style from its blockstate, the woods and the paint
    // from its block entity - because JumpBlockEntity already syncs itself to
    // every client tracking the chunk. It has to: the model reads those woods.
    //
    // THIS REPLACED A ContainerData, and the reason is worth keeping. Three
    // ints carried style and two wood indices perfectly well, and then dye was
    // added and they could not: ClientboundContainerSetDataPacket reads and
    // writes its value with readShort/writeShort, so every value in a
    // ContainerData is silently truncated to 16 bits and an RGB colour needs
    // 24. Splitting each colour across two slots would have worked and would
    // have been a lie about what a menu's data vector is for. Reading the block
    // is simpler, carries anything, and cannot drift from what the model draws.
    // ------------------------------------------------------------------

    private final Player player;
    private final ContainerLevelAccess access;
    private final BlockPos pos;

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

    /**
     * <b>Client constructor</b> - the position arrives in the menu's opening
     * packet, written by {@code JumpBlock.useWithoutItem}.
     *
     * <p>This is NeoForge's extra-data menu ({@code IMenuTypeExtension.create}
     * in {@link ModMenus}), which exists precisely so a client menu can be
     * built knowing something the vanilla two-argument factory cannot tell it.
     */
    public JumpMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(containerId, inventory, ContainerLevelAccess.NULL, extra.readBlockPos());
    }

    public JumpMenu(int containerId, Inventory inventory, ContainerLevelAccess access,
                    BlockPos pos) {
        super(ModMenus.JUMP.get(), containerId);
        this.player = inventory.player;
        this.access = access;
        this.pos = pos;

        addSlot(plankSlot(SLOT_RAILS, RAILS_Y));
        addSlot(plankSlot(SLOT_STANDARDS, STANDARDS_Y));
        addStandardInventorySlots(inventory, MARGIN, INV_Y);
    }

    /** Where the jump is. Both sides have it; both sides read the block with it. */
    public BlockPos pos() {
        return this.pos;
    }

    /**
     * A slot that takes <b>a plank or a dye</b>, and nothing else.
     *
     * <p>One slot per half doing both jobs, rather than four slots. That is
     * what makes the rule statable in a sentence: <i>whatever you put in this
     * slot, this half becomes</i>. A plank re-woods it and strips its paint; a
     * dye paints it.
     *
     * <p>{@link #accepts} is the entire test, which is why it is also the
     * entire test in {@link #quickMoveStack}: one predicate, so shift-clicking
     * can never put something in a slot a click could not.
     */
    /** What a part slot will take: a plank of a known wood, or a dye. */
    public static boolean accepts(ItemStack stack) {
        return JumpWoods.keyOfPlank(stack.getItem()) != null || dyeColour(stack) != null;
    }

    /**
     * <b>The colour this stack paints with</b>, or null if it is not a dye.
     *
     * <p>Vanilla's {@code minecraft:dye} component first, which is all sixteen
     * of vanilla's and most modded dyes - a mod adding "a red dye" gives it
     * that component and has always worked. The fallback is the one case that
     * does not: a dye whose colour vanilla has no name for, which carries no
     * component and can only be read off its own art.
     *
     * <p>The same three lines as {@code EquestrianBenchMenu.dyeColour}, and
     * deliberately not shared: extracting them would make the bench depend on
     * the jumps or both on a third class, for three lines. If a third thing
     * dyes, extract it then.
     */
    public static @Nullable Integer dyeColour(ItemStack stack) {
        net.minecraft.world.item.DyeColor dye =
                stack.get(net.minecraft.core.component.DataComponents.DYE);
        if (dye != null) {
            return dye.getTextureDiffuseColor() & 0xFFFFFF;
        }
        return com.example.horsegenetics.neoforge.compat.ModdedMaterials.dyes().get(
                net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem()).toString());
    }

    private Slot plankSlot(int index, int y) {
        return new Slot(input, index, SLOT_X, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return accepts(stack);
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
        if (this.player.level().isClientSide() || this.swapping) {
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
        JumpMaterials now = jump.materials();
        String wood = JumpWoods.keyOfPlank(offered.getItem());

        if (wood != null) {
            String current = rails ? now.rails() : now.standards();
            int paint = rails ? now.railsDye() : now.standardsDye();
            // A plank of the wood it already is, on a half with no paint to
            // strip, does nothing - so it sits in the slot and is handed back
            // on close rather than being eaten for no change. If the half IS
            // painted, that same plank is a strip-back and does plenty.
            if (wood.equals(current) && paint == JumpMaterials.UNDYED) {
                return false;
            }
            // RE-WOODING STRIPS THE PAINT - withRails and withStandards do it -
            // and it is the only thing that does. A dye never comes back out.
            jump.setMaterials(rails ? now.withRails(wood) : now.withStandards(wood));
            spend(offered, slot);
            // THE DISPLACED PLANK, and only the plank. Into the player's
            // inventory rather than back into the slot, because the slot
            // usually still holds the rest of the stack they just put in and
            // two woods cannot share one slot. placeItemBackInInventory drops
            // it at their feet if there is no room, so wood is never destroyed.
            Item plank = JumpWoods.plankOf(current);
            if (plank != null) {
                this.player.getInventory().placeItemBackInInventory(new ItemStack(plank));
            }
            return true;
        }

        Integer colour = dyeColour(offered);
        if (colour == null || colour == (rails ? now.railsDye() : now.standardsDye())) {
            return false;
        }
        // A DYE IS SPENT AND NOTHING COMES BACK, which is what makes paint a
        // decision where the wood swap is a preference: a jump can always be
        // put back to the wood it was, and the dye can never be got back out
        // of it. Owner's rule.
        jump.setMaterials(rails ? now.withRailsDye(colour) : now.withStandardsDye(colour));
        spend(offered, slot);
        return true;
    }

    /** Take one off what the player offered, leaving the rest in the slot. */
    private void spend(ItemStack offered, int slot) {
        offered.shrink(1);
        this.input.setItem(slot, offered.isEmpty() ? ItemStack.EMPTY : offered);
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
        if (id == BUTTON_SHORTER || id == BUTTON_TALLER) {
            return nudgeSize(id == BUTTON_TALLER ? 1 : -1);
        }
        if (id < 0 || id >= STYLE_BUTTONS) {
            return false;
        }
        JumpBlock.Style style = JumpBlock.Style.values()[id];
        this.access.execute((level, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof JumpBlock && state.getValue(JumpBlock.STYLE) != style) {
                // THE WHOLE RUN, like the height - a fence is one kind of fence,
                // and restyling a built line block by block is eleven clicks to
                // get back where you started. JumpBlock.setRunStyle collects the
                // run before it changes anything, because a jump only connects
                // to a jump of the same style and the walk would otherwise stop
                // at its own first step.
                JumpBlock.setRunStyle(level, pos, state, style);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        });
        return true;
    }

    /**
     * <b>A rung up or down, for the whole run.</b>
     *
     * <p>A fence has a height; each block of it does not. So this sets every
     * jump joined to this one - see {@code JumpBlock.setRunSize} - which is the
     * only version that cannot go wrong by accident. A stepped line is still
     * buildable on purpose, by breaking the run.
     *
     * <p>Free, like the style buttons, and for the same reason: height is the
     * one thing about a jump that is a <i>measurement</i> rather than a
     * decoration, and charging for the rung you are testing at would be
     * charging to ask the question.
     */
    private boolean nudgeSize(int step) {
        this.access.execute((level, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof JumpBlock)) {
                return;
            }
            int now = materials().size();
            int wanted = JumpMaterials.clampSize(now + step);
            if (wanted != now) {
                JumpBlock.setRunSize(level, pos, state, wanted);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        });
        return true;
    }

    // --- what the screen reads ----------------------------------------------
    //
    // Straight off the block, on whichever side is asking. The client's copy of
    // the block entity is kept current by JumpBlockEntity's own sync, so these
    // are the same values the model is drawing with - by construction, rather
    // than by two code paths agreeing.

    /** What this jump is made of and painted with, or the default if it is gone. */
    public JumpMaterials materials() {
        return this.player.level().getBlockEntity(this.pos) instanceof JumpBlockEntity jump
                ? jump.materials()
                : JumpMaterials.DEFAULT;
    }

    /** The style the block is wearing. */
    public JumpBlock.Style style() {
        BlockState state = this.player.level().getBlockState(this.pos);
        return state.getBlock() instanceof JumpBlock
                ? state.getValue(JumpBlock.STYLE)
                : JumpBlock.Style.VERTICAL;
    }

    // --- housekeeping -------------------------------------------------------

    /**
     * Shift-click. Out of the slots into the player; from the player,
     * <b>one plank into each half at once</b>.
     *
     * <p>A stack of two or more re-woods the whole jump in a single click -
     * one plank to the rails, one to the standards - which is what somebody
     * holding a stack of birch means by shift-clicking it at an oak jump
     * (owner, 2026-09-20). A single plank goes to the rails alone, because
     * there is only one of it and the rails are the half you actually jump.
     *
     * <p>Spending exactly one per half rather than dumping the stack is what
     * makes this safe: each insert fires {@link #slotsChanged}, which performs
     * the exchange and empties the slot again, so the loop's second pass finds
     * the standards slot free. Nothing is left behind to hand back.
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
        } else if (accepts(stack)) {
            int placed = 0;
            for (int part = SLOT_RAILS; part < SLOT_COUNT && placed < original.getCount(); part++) {
                if (this.input.getItem(part).isEmpty()) {
                    this.input.setItem(part, stack.copyWithCount(1));
                    placed++;
                }
            }
            if (placed == 0) {
                return ItemStack.EMPTY;
            }
            stack.shrink(placed);
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
