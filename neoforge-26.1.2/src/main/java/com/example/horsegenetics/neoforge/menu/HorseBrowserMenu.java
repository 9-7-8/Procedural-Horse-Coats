package com.example.horsegenetics.neoforge.menu;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * The <b>Horse Browser</b> menu. The screen has two tabs - a gene reference and
 * a crafting surface - but only the crafting tab uses slots: a private 3x3 grid
 * ({@link #craft}, slots 1-9), a result slot ({@link #result}, slot 0) and the
 * player inventory (slots 10-45), <b>all</b> of which go inactive on the Gene
 * Database tab, which is a full-window reference with no slots. The result is
 * computed by {@link HorseBrowserRecipes}, which only ever makes this mod's own
 * outputs.
 *
 * <p>The selected gene (for the book &rarr; gene-paper craft) is menu state set
 * from the client by {@code SelectBrowserGenePayload}; changing it recomputes
 * the result.
 */
public final class HorseBrowserMenu extends AbstractContainerMenu {

    public static final int RESULT_SLOT = 0;
    public static final int GRID_START = 1;
    public static final int GRID_END = 10; // exclusive
    public static final int INV_START = 10;
    public static final int INV_END = 46; // exclusive

    // Slot geometry, relative to the Crafting panel's leftPos / topPos.
    public static final int GRID_X = 30;
    public static final int GRID_Y = 28;
    public static final int RESULT_X = 108;
    public static final int RESULT_Y = 46;
    public static final int INV_X = 8;
    public static final int INV_Y = 124;

    private final Player player;
    private final CraftingContainer craft = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer result = new ResultContainer();

    private String selectedGeneKey = "";
    private boolean assembling;
    /**
     * Client-only: whether the Crafting tab is showing. When it is not, the
     * result + grid slots go inactive so they don't render, hover or take
     * clicks behind the gene-reference pane. The player inventory stays visible
     * on both tabs, as any container screen's does. Always true server-side.
     */
    private boolean craftingVisible = true;

    public HorseBrowserMenu(int containerId, Inventory inventory) {
        super(ModMenus.HORSE_BROWSER.get(), containerId);
        this.player = inventory.player;

        addSlot(new ResultSlot(player, this, result, RESULT_X, RESULT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(tabSlot(craft, col + row * 3, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        // The player inventory is added by hand (not addStandardInventorySlots)
        // so it too can go inactive on the Gene Database tab - that tab is a
        // full-window reference and shows no slots at all.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(tabSlot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(tabSlot(inventory, col, INV_X + col * 18, INV_Y + 58));
        }
    }

    /** A slot that is only active (rendered / hoverable / clickable) while the Crafting tab shows. */
    private Slot tabSlot(Container container, int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean isActive() {
                return craftingVisible;
            }
        };
    }

    /** Screen-driven: hide/show every slot when the tab changes. */
    public void setCraftingVisible(boolean visible) {
        this.craftingVisible = visible;
    }

    public boolean isCraftingVisible() {
        return craftingVisible;
    }

    // --- gene selection -------------------------------------------------

    public void selectGene(String geneKey) {
        this.selectedGeneKey = geneKey == null ? "" : geneKey;
        slotsChanged(craft);
    }

    public String selectedGeneKey() {
        return selectedGeneKey;
    }

    public ItemStack resultStack() {
        return result.getItem(0);
    }

    // --- crafting ------------------------------------------------------

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (assembling || player.level().isClientSide()) {
            return;
        }
        recompute();
    }

    private void recompute() {
        ItemStack out = HorseBrowserRecipes.resultFor(craft.asCraftInput(), player, selectedGeneKey);
        result.setItem(0, out);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(
                    new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT, out));
        }
    }

    /** Called by {@link ResultSlot} once the player has taken the crafted stack. */
    void onResultTaken() {
        assembling = true;
        try {
            HorseBrowserRecipes.consume(craft, player, selectedGeneKey);
        } finally {
            assembling = false;
        }
        recompute();
    }

    @Override
    public boolean stillValid(Player p) {
        return true;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        clearContainer(p, craft);
    }

    // --- shift-click --------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (index >= GRID_START && index < GRID_END) {
            if (!moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // from the inventory: into the grid first, else shuffle main <-> hotbar
            if (!moveItemStackTo(stack, GRID_START, GRID_END, false)) {
                int mainEnd = INV_START + 27;
                if (index < mainEnd) {
                    if (!moveItemStackTo(stack, mainEnd, INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, INV_START, mainEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(p, stack);
        if (index == RESULT_SLOT) {
            p.drop(stack, false);
        }
        return moved;
    }

    /** The result slot: never accepts a placed item, and drives {@link #onResultTaken()} on take. */
    private static final class ResultSlot extends Slot {

        private final HorseBrowserMenu menu;

        ResultSlot(Player player, HorseBrowserMenu menu, Container result, int x, int y) {
            super(result, 0, x, y);
            this.menu = menu;
        }

        @Override
        public boolean isActive() {
            return menu.craftingVisible;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            menu.onResultTaken();
            super.onTake(player, stack);
        }
    }
}
