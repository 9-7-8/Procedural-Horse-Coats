package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.data.HorsePacks;
import com.example.horsegenetics.neoforge.server.HorsePackHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * <b>A chest on a horse whose size vanilla has no screen for.</b> A shelf's
 * three slots, a decorated pot's one, another mod's crate of a hundred: a plain
 * grid of however many there are, over the player's inventory.
 *
 * <p>This is the fallback and not the rule. A chest of nine-by-N slots opens
 * vanilla's own {@code ChestMenu} and a shulker box vanilla's
 * {@code ShulkerBoxMenu} - see {@code HorsePackHandler.open} - so those look
 * and behave exactly as the block does, to the player and to every inventory
 * mod that knows them. Only a size neither covers lands here.
 *
 * <h2>The menu keeps nothing</h2>
 * On the server the slots are a {@code HorsePackHandler.PackContainer}, a
 * window onto the horse's own store; on the client a plain mirror the menu
 * sync fills. Same shape as {@link HorseGearMenu}.
 *
 * <h2>The layout is the count</h2>
 * Up to fifty-four slots sit nine across, as a chest's do. Past that the grid
 * widens rather than scrolls - up to eighteen across and nine down - because a
 * scrolling container is a second thing to get wrong and
 * {@link HorsePacks#MAX_SLOTS} is exactly what that holds.
 */
public final class HorsePackMenu extends AbstractContainerMenu {

    public static final int MARGIN = 8;
    public static final int SLOT = 18;
    public static final int GRID_Y = 18;
    private static final int INVENTORY_COLUMNS = 9;

    private final Container container;
    private final int size;
    private final boolean nests;
    private final int columns;
    private final int rows;

    /** <b>Client constructor</b> - the slot count and the nesting rule arrive in the opening packet. */
    public HorsePackMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(containerId, inventory, extra.readVarInt(), extra.readBoolean());
    }

    private HorsePackMenu(int containerId, Inventory inventory, int size, boolean nests) {
        this(containerId, inventory, new SimpleContainer(Mth.clamp(size, 1, HorsePacks.MAX_SLOTS)), nests);
    }

    /** <b>Server constructor</b> - a window straight onto the horse's chest. */
    public HorsePackMenu(int containerId, Inventory inventory, HorsePackHandler.PackContainer container) {
        this(containerId, inventory, container, container.nests());
    }

    private HorsePackMenu(int containerId, Inventory inventory, Container container, boolean nests) {
        super(ModMenus.HORSE_PACK.get(), containerId);
        this.container = container;
        this.size = container.getContainerSize();
        this.nests = nests;
        this.columns = columnsFor(size);
        this.rows = (size + columns - 1) / columns;
        container.startOpen(inventory.player);

        int gridX = MARGIN + (Math.max(INVENTORY_COLUMNS, columns) - columns) * SLOT / 2;
        for (int i = 0; i < size; i++) {
            addSlot(new PackSlot(container, i, gridX + (i % columns) * SLOT, GRID_Y + (i / columns) * SLOT));
        }
        addStandardInventorySlots(inventory, inventoryX(), inventoryY());
    }

    /** Nine across while that fits in six rows, as a chest; then as wide as nine rows need. */
    public static int columnsFor(int size) {
        if (size <= INVENTORY_COLUMNS) {
            return Math.max(1, size);
        }
        if (size <= 54) {
            return INVENTORY_COLUMNS;
        }
        return Mth.clamp((size + 8) / 9, INVENTORY_COLUMNS, 18);
    }

    public int size() {
        return size;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    /** Window-relative left edge of the chest's grid. */
    public int gridX() {
        return MARGIN + (Math.max(INVENTORY_COLUMNS, columns) - columns) * SLOT / 2;
    }

    /** Window-relative left edge of the player's inventory, centred under a wide grid. */
    public int inventoryX() {
        return MARGIN + (Math.max(INVENTORY_COLUMNS, columns) - INVENTORY_COLUMNS) * SLOT / 2;
    }

    /** Window-relative top of the player's inventory - vanilla's thirteen-pixel label gap. */
    public int inventoryY() {
        return GRID_Y + rows * SLOT + 13;
    }

    public int width() {
        return MARGIN * 2 + Math.max(INVENTORY_COLUMNS, columns) * SLOT - 2;
    }

    /** Three rows, the four-pixel gap, the hotbar, and the frame under it. */
    public int height() {
        return inventoryY() + 3 * SLOT + 4 + SLOT + MARGIN - 2;
    }

    /** Vanilla's chest shift-click: out of the chest into the inventory, and back. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < size) {
            if (!moveItemStackTo(stack, size, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, size, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    /** A slot of a chest that leaves with its contents refuses another of its kind, as a shulker box does. */
    private final class PackSlot extends Slot {

        PackSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !nests || stack.canFitInsideContainerItems();
        }
    }
}
