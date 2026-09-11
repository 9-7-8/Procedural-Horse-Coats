package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity;
import com.example.horsegenetics.neoforge.server.HorseProgress;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>The Equine Research Shelf's menu</b> - a window onto the block, like a
 * furnace's. Two tabs.
 *
 * <ul>
 *   <li><b>Store</b> - {@link EquineResearchShelfBlockEntity#SLOTS} slots of
 *       research papers, like a chest. One paper per gene.</li>
 *   <li><b>Copy</b> - pick a gene the shelf holds, put blank books in
 *       {@link #BOOK_SLOT}, and copies arrive in {@link #RESULT_SLOT} one book
 *       at a time, by rarity.</li>
 * </ul>
 *
 * <h2>It holds no state of its own</h2>
 * Every slot wraps one of the block entity's containers and the progress bar
 * reads its {@code ContainerData}, so closing the screen changes nothing: the
 * copy carries on, as a furnace smelts with its lid shut. The copy state used to
 * live here, and ended the moment the player clicked off (2026-09-10). The
 * client's copy wraps empty containers of the same shape, which the ordinary
 * slot and data sync fill in.
 */
public final class ResearchShelfMenu extends AbstractContainerMenu {

    public static final int BOOK_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    /** The first paper slot; the rest follow it in rows of nine. */
    public static final int FIRST_PAPER_SLOT = 2;
    private static final int SLOT_COUNT = FIRST_PAPER_SLOT + EquineResearchShelfBlockEntity.SLOTS;

    // ------------------------------------------------------------------
    // The window's layout, in one place - addSlot needs it here, and the
    // screen reads every one of them. A double chest's size.
    // ------------------------------------------------------------------

    public static final int WIDTH = 176;
    public static final int HEIGHT = 222;
    public static final int MARGIN = 8;

    /** Store tab: the paper grid, six rows of nine, where a double chest's are. */
    public static final int GRID_Y = 18;

    /** Copy tab: the gene list, then the book and result slots under it. */
    public static final int LIST_Y = 20;
    public static final int LIST_W = 160;
    public static final int LIST_ROWS = 5;
    public static final int LIST_H = LIST_ROWS * 12;
    public static final int SLOT_Y = LIST_Y + LIST_H + 8;
    public static final int BOOK_X = 44;
    public static final int RESULT_X = 116;
    public static final int NOTE_Y = SLOT_Y + 22;

    public static final int INV_LABEL_Y = 128;
    public static final int INV_Y = 140;
    public static final int HOTBAR_Y = INV_Y + 3 * 18 + 4;

    private final Player player;
    private final @Nullable EquineResearchShelfBlockEntity shelf;
    private final Container papers;
    private final ContainerData data;

    /**
     * <b>Which tab the screen is showing</b>, so the slots that do not belong to
     * it go inactive on the client. Only ever set there - see
     * {@link #activeOnTab}.
     */
    private boolean storeTab;

    /** Client-side: the pick the player just clicked, shown until the server's arrives. */
    private String clientPick = "";

    /** Client constructor - {@code MenuType} hands us no block entity. */
    public ResearchShelfMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public ResearchShelfMenu(int containerId, Inventory inventory,
                             @Nullable EquineResearchShelfBlockEntity shelf) {
        super(ModMenus.RESEARCH_SHELF.get(), containerId);
        this.player = inventory.player;
        this.shelf = shelf;
        this.papers = shelf != null ? shelf.papers() : new SimpleContainer(EquineResearchShelfBlockEntity.SLOTS);
        Container book = shelf != null ? shelf.book() : new SimpleContainer(1);
        Container result = shelf != null ? shelf.result() : new SimpleContainer(1);
        this.data = shelf != null ? shelf.data() : new SimpleContainerData(EquineResearchShelfBlockEntity.DATA_COUNT);

        addSlot(new Slot(book, 0, BOOK_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK);
            }

            @Override
            public boolean isActive() {
                return activeOnTab(false);
            }
        });
        addSlot(new Slot(result, 0, RESULT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return activeOnTab(false);
            }

            @Override
            public void onTake(Player taker, ItemStack taken) {
                HorseProgress.complete(taker, ProgressTask.COPY_PAPER);
                super.onTake(taker, taken);
            }
        });
        for (int i = 0; i < EquineResearchShelfBlockEntity.SLOTS; i++) {
            addSlot(new PaperSlot(papers, i, MARGIN + (i % 9) * 18, GRID_Y + (i / 9) * 18));
        }

        addDataSlots(data);

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
     * A shelf slot: a research paper with a gene on it, one to a slot, and
     * never a second paper for a gene another slot already holds.
     */
    private final class PaperSlot extends Slot {

        PaperSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return EquineResearchShelfBlockEntity.isFiledPaper(stack)
                    && !EquineResearchShelfBlockEntity.holdsElsewhere(container,
                            EquineResearchShelfBlockEntity.geneOf(stack), getContainerSlot());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return activeOnTab(true);
        }

        @Override
        public void setByPlayer(ItemStack stack, ItemStack previous) {
            super.setByPlayer(stack, previous);
            if (!stack.isEmpty()) {
                HorseProgress.complete(player, ProgressTask.FILE_PAPER);
            }
        }
    }

    // ------------------------------------------------------------------
    // What the shelf holds, and the copy
    // ------------------------------------------------------------------

    /** Every gene the shelf can copy, in display order - read from the slots on either side. */
    public List<String> storedGenes() {
        return EquineResearchShelfBlockEntity.genesIn(papers);
    }

    /** The gene being copied: the server's word, synced as an index into {@link #storedGenes}. */
    public String selectedGene() {
        if (shelf != null) {
            return shelf.selectedGene();
        }
        int i = data.get(EquineResearchShelfBlockEntity.DATA_SELECTED);
        List<String> genes = storedGenes();
        return i >= 0 && i < genes.size() ? genes.get(i) : clientPick;
    }

    /** From the client's list click; on the server it goes to the block. */
    public void selectGene(String geneKey) {
        if (shelf != null) {
            shelf.select(geneKey);
        } else {
            clientPick = geneKey == null ? "" : geneKey;
        }
    }

    public int copyProgress() {
        return data.get(EquineResearchShelfBlockEntity.DATA_PROGRESS);
    }

    public int copyTotal() {
        int total = data.get(EquineResearchShelfBlockEntity.DATA_TOTAL);
        return total <= 0 ? EquineResearchShelfBlockEntity.TICKS_PER_RARITY_TIER : total;
    }

    /**
     * <b>Every slot is live on the server; the tab only hides them on the
     * client.</b> The server cannot know which tab the player is looking at; the
     * client will not send a click on a slot it is not drawing.
     */
    private boolean activeOnTab(boolean tab) {
        return !player.level().isClientSide() || storeTab == tab;
    }

    public void setStoreTab(boolean store) {
        this.storeTab = store;
    }

    // ------------------------------------------------------------------
    // Plumbing
    // ------------------------------------------------------------------

    /**
     * Shift-click. Out of the shelf into the player; from the player, a paper
     * goes onto the shelf and a book into the book slot - {@code moveItemStackTo}
     * asks each slot's {@code mayPlace}, so a duplicate paper stays put.
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
        } else if (EquineResearchShelfBlockEntity.isFiledPaper(stack)) {
            if (!moveItemStackTo(stack, FIRST_PAPER_SLOT, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, BOOK_SLOT, BOOK_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (index == RESULT_SLOT) {
            slot.onTake(who, original);
        }
        return original;
    }

    /** Close if the shelf is gone or the player walked off - 8 blocks, past vanilla's reach. */
    @Override
    public boolean stillValid(Player who) {
        if (shelf == null) {
            return true; // client copy - the server's answer is the one that counts
        }
        return !shelf.isRemoved()
                && who.distanceToSqr(shelf.getBlockPos().getCenter()) <= 64.0;
    }
}
