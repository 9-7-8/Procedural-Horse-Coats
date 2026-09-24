package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * <b>The Horse Stasis Bank's menu.</b> One tab, for now: a grid of
 * {@link HorseStasisBankBlockEntity#SLOTS} slots that takes stasis chambers and
 * refuses everything else.
 *
 * <h2>It holds no state of its own</h2>
 * Every slot wraps the block entity's container, exactly as
 * {@link ResearchShelfMenu}'s do, so closing the screen changes nothing. The
 * client's copy wraps an empty container of the same shape, which the ordinary
 * slot sync fills in.
 *
 * <p><b>No {@code ContainerData} and no tab state.</b> The research shelf needs
 * both because it has a second tab and a clock running on the block; this has
 * neither yet. When the Browse tab arrives (stage three on
 * {@code wiki/horse-stasis.html}) it wants the shelf's
 * {@code setStoreTab}/{@code activeOnTab} pair, which is a client-side field and
 * an {@code isActive()} override on each slot - copy it then, rather than carrying
 * a single-tab tab flag now.
 *
 * <h2>Every chamber's whole horse is on the wire</h2>
 * A chamber's {@code stasis_snapshot} component carries the entire entity tag,
 * for the creative pick-block reason {@code StasisSnapshot} explains, and this
 * menu can hold fifty-four of them. That is the one thing about the bank that is
 * genuinely more expensive than the item it stores, and it is on the page's
 * Verification tab as a thing to measure with a full bank rather than a thing to
 * fix by sending less - sending less is how a creative player silently erases a
 * horse.
 */
public final class HorseStasisBankMenu extends AbstractContainerMenu {

    /** The chamber grid is the first block of slots; the player's follow it. */
    public static final int FIRST_CHAMBER_SLOT = 0;
    private static final int SLOT_COUNT = FIRST_CHAMBER_SLOT + HorseStasisBankBlockEntity.SLOTS;

    // ------------------------------------------------------------------
    // The window's layout, in one place - addSlot needs it here and the screen
    // reads every one of them. Deliberately the research shelf's numbers: it is
    // the same double-chest grid over the same player inventory, and two windows
    // in one mod that are almost the same size look like a mistake.
    // ------------------------------------------------------------------

    public static final int WIDTH = 176;
    public static final int HEIGHT = 222;
    public static final int MARGIN = 8;
    public static final int GRID_Y = 18;
    public static final int INV_LABEL_Y = 128;
    public static final int INV_Y = 140;
    public static final int HOTBAR_Y = INV_Y + 3 * 18 + 4;

    private final @Nullable HorseStasisBankBlockEntity bank;
    private final Container chambers;

    /** Client constructor - {@code MenuType} hands us no block entity. */
    public HorseStasisBankMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public HorseStasisBankMenu(int containerId, Inventory inventory,
                               @Nullable HorseStasisBankBlockEntity bank) {
        super(ModMenus.HORSE_STASIS_BANK.get(), containerId);
        this.bank = bank;
        this.chambers = bank != null
                ? bank.chambers()
                : new SimpleContainer(HorseStasisBankBlockEntity.SLOTS);

        for (int i = 0; i < HorseStasisBankBlockEntity.SLOTS; i++) {
            addSlot(new ChamberSlot(chambers, i, MARGIN + (i % 9) * 18, GRID_Y + (i / 9) * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, MARGIN + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, MARGIN + col * 18, HOTBAR_Y));
        }
    }

    /**
     * A bank slot: a stasis chamber of any tier, empty or with a horse in it, one
     * to a slot.
     *
     * <p>Unlike the shelf's paper slot there is no second condition - no "one per
     * gene" equivalent - because two chambers are never the same chamber and a
     * rack of spare empties is a reasonable thing to keep beside the full ones.
     */
    private static final class ChamberSlot extends Slot {

        ChamberSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return HorseStasisBankBlockEntity.isChamber(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // ------------------------------------------------------------------
    // What the bank holds - read off the slots, so both sides agree
    // ------------------------------------------------------------------

    /** How many chambers are filed here, full or empty. */
    public int filed() {
        return HorseStasisBankBlockEntity.filed(chambers);
    }

    /** How many of them have a horse in them. */
    public int occupied() {
        return HorseStasisBankBlockEntity.occupied(chambers);
    }

    // ------------------------------------------------------------------
    // Plumbing
    // ------------------------------------------------------------------

    /**
     * Shift-click. Out of the bank into the player; from the player, a chamber
     * goes into the bank and anything else stays where it is -
     * {@code moveItemStackTo} asks each slot's {@code mayPlace}.
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
        } else if (HorseStasisBankBlockEntity.isChamber(stack)) {
            if (!moveItemStackTo(stack, FIRST_CHAMBER_SLOT, SLOT_COUNT, false)) {
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

    /** Close if the bank is gone or the player walked off - 8 blocks, the shelf's reach. */
    @Override
    public boolean stillValid(Player who) {
        if (bank == null) {
            return true; // client copy - the server's answer is the one that counts
        }
        return !bank.isRemoved()
                && who.distanceToSqr(bank.getBlockPos().getCenter()) <= 64.0;
    }
}
