package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.common.horse.StasisBankGrid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A stasis bank's chamber grid, which grows</b> (issue #273). A plain
 * {@code SimpleContainer} has a fixed size; this one adds a row of
 * {@link StasisBankGrid#COLS} whenever a chamber lands in its last row, so there
 * is always an empty row at the bottom and no practical limit. The rule is
 * {@link StasisBankGrid}'s; this is only the container that obeys it.
 *
 * <p><b>It only grows.</b> A menu's slots are made when it opens, and a grid that
 * lost rows under an open menu would leave slots pointing past its end. A menu
 * opened before a growth simply does not show the new row until it is opened again.
 *
 * <p><b>The save is not vanilla's.</b> {@code ContainerHelper.saveAllItems} writes
 * each slot number as an unsigned byte ({@code ItemStackWithSlot.CODEC} uses
 * {@code ExtraCodecs.UNSIGNED_BYTE}, read from the 26.1.2 jar), so slot 256 and
 * up would not survive a save. Slots 0-255 still go under vanilla's {@code Items}
 * key - an older jar reads the first 207 of them, so a downgrade keeps those - and
 * every slot past that goes under {@link #WIDE_KEY} with an int slot. The row count
 * is not saved: it is rebuilt from the highest filled slot, which is also what
 * migrates a save from before the grid grew (it has no wide list, and its
 * {@code Items} fit the minimum).
 */
public class ChamberGrid implements Container {

    /** Where chambers past slot 255 are saved. */
    public static final String WIDE_KEY = "chambers_wide";
    /** The last slot vanilla's byte-slot list can hold. */
    private static final int BYTE_SLOTS = 256;

    private record Wide(int slot, ItemStack stack) {
        static final Codec<Wide> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("Slot").forGetter(Wide::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(Wide::stack)
        ).apply(i, Wide::new));
    }

    private final List<ItemStack> items = new ArrayList<>();
    private int rows;

    public ChamberGrid() {
        resize(StasisBankGrid.MIN_ROWS);
    }

    /** How many rows deep the grid is now. */
    public int rows() {
        return rows;
    }

    private void resize(int newRows) {
        while (items.size() < newRows * StasisBankGrid.COLS) {
            items.add(ItemStack.EMPTY);
        }
        rows = newRows;
    }

    private void growFor(int slot) {
        if (StasisBankGrid.growsAt(slot, rows)) {
            resize(StasisBankGrid.rowsFor(slot));
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = ContainerHelper.removeItem(items, slot, count);
        if (!out.isEmpty()) {
            setChanged();
        }
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) {
            return;
        }
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        if (!stack.isEmpty()) {
            growFor(slot);
        }
        setChanged();
    }

    /**
     * <b>The first empty slot</b>, growing the grid if that is what it takes - so
     * there is always one. What a command filing many horses at once uses.
     */
    public int firstEmpty() {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                return i;
            }
        }
        resize(rows + 1);
        return items.size() - StasisBankGrid.COLS;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    /** See the class comment for why slots past 255 are saved apart. */
    public void save(ValueOutput output) {
        int byteSlots = Math.min(BYTE_SLOTS, items.size());
        NonNullList<ItemStack> head = NonNullList.withSize(byteSlots, ItemStack.EMPTY);
        for (int i = 0; i < byteSlots; i++) {
            head.set(i, items.get(i));
        }
        ContainerHelper.saveAllItems(output, head);
        ValueOutput.TypedOutputList<Wide> wide = output.list(WIDE_KEY, Wide.CODEC);
        for (int i = BYTE_SLOTS; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                wide.add(new Wide(i, items.get(i)));
            }
        }
    }

    /** Replace everything with what {@link #save} wrote, or what a fixed-size bank wrote before it. */
    public void load(ValueInput input) {
        items.clear();
        rows = 0;
        List<Wide> wide = input.listOrEmpty(WIDE_KEY, Wide.CODEC).stream().toList();
        int highest = -1;
        for (Wide w : wide) {
            highest = Math.max(highest, w.slot());
        }
        NonNullList<ItemStack> head = NonNullList.withSize(BYTE_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, head);
        for (int i = 0; i < BYTE_SLOTS; i++) {
            if (!head.get(i).isEmpty()) {
                highest = Math.max(highest, i);
            }
        }
        resize(StasisBankGrid.rowsFor(highest));
        for (int i = 0; i < BYTE_SLOTS && i < items.size(); i++) {
            items.set(i, head.get(i));
        }
        for (Wide w : wide) {
            if (w.slot() >= 0 && w.slot() < items.size()) {
                items.set(w.slot(), w.stack());
            }
        }
    }
}
