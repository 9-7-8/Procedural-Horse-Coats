package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.server.HorseProgress;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>The Equine Research Shelf's menu.</b> Two tabs.
 *
 * <ul>
 *   <li><b>Store</b> - {@link EquineResearchShelfBlockEntity#SLOTS} slots of
 *       research papers, <b>like a chest</b>: put a paper in, take it out,
 *       instantly. One paper per gene.</li>
 *   <li><b>Craft</b> - pick a gene the shelf holds, put a blank book in
 *       {@link #BOOK_SLOT}, and after a while by rarity take its copy out of
 *       {@link #RESULT_SLOT}. The book is spent; the shelf's own paper is
 *       not.</li>
 * </ul>
 *
 * <h2>It used to be a list and a filing slot</h2>
 * Filing consumed the paper into a set of gene keys and a clickable list gave
 * them back, which needed two custom packets to keep the client's list in step
 * - and in play, "putting a book in the research shelf still does nothing"
 * (2026-09-10). Real slots sync themselves, so the only packet left is the
 * Craft tab's gene pick.
 *
 * <h2>The shelf is still the authority</h2>
 * The paper slots wrap the block entity's own container on the server; the
 * client's copy has an empty container of the same size that the ordinary
 * slot sync fills in. The Craft list is read from those slots on both sides,
 * so a forged pick for a gene the shelf does not hold copies nothing.
 */
public final class ResearchShelfMenu extends AbstractContainerMenu {

    public static final int BOOK_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    /** The first paper slot; the rest follow it in rows of nine. */
    public static final int FIRST_PAPER_SLOT = 2;
    private static final int SLOT_COUNT = FIRST_PAPER_SLOT + EquineResearchShelfBlockEntity.SLOTS;

    // ------------------------------------------------------------------
    // The window's layout, in one place
    // ------------------------------------------------------------------
    //
    // These live on the MENU rather than the screen because addSlot needs them
    // here, and a slot is the one thing that must agree with the drawing
    // exactly. The screen reads every one of them. The window is a double
    // chest's size, because it holds a double chest's worth.

    public static final int WIDTH = 176;
    public static final int HEIGHT = 222;
    public static final int MARGIN = 8;

    /** Store tab: the paper grid, six rows of nine, where a double chest's are. */
    public static final int GRID_Y = 18;
    public static final int GRID_ROWS = EquineResearchShelfBlockEntity.SLOTS / 9;

    /** Craft tab: the gene list, then the book and result slots under it. */
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

    /**
     * <b>Copying takes time, and how much is the gene's rarity.</b> One iron
     * ingot's smelt - 200 ticks, ten seconds - per rarity tier, so a common gene
     * is ten seconds and a mythic one a minute. Kept at the owner's call when
     * storing became instant: copying is the thing worth waiting for.
     */
    public static final int TICKS_PER_RARITY_TIER = 200;

    /** {@link ContainerData} slots - vanilla's furnace pattern, synced by the menu itself. */
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    private static final int DATA_COUNT = 2;

    private final Container input = new SimpleContainer(1);
    private final ResultContainer result = new ResultContainer();

    private String selectedGene = "";

    /**
     * <b>Which tab the screen is showing</b>, so the slots that do not belong to
     * it can go inactive on the client. Only ever set there - see
     * {@link #activeOnTab}, which is why the server's copy never refuses a slot.
     */
    private boolean storeTab;

    private final ContainerData data = new SimpleContainerData(DATA_COUNT);

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

        addSlot(new Slot(input, 0, BOOK_X, SLOT_Y) {
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
                input.removeItem(0, 1);
                recomputeResult();
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

        @Override
        public void setChanged() {
            super.setChanged();
            if (!canCopy()) {
                recomputeResult(); // the original left the shelf - that copy is over
            }
        }
    }

    // ------------------------------------------------------------------
    // What the shelf holds
    // ------------------------------------------------------------------

    /** Every gene the shelf can copy, in display order - read from the slots on either side. */
    public List<String> storedGenes() {
        return EquineResearchShelfBlockEntity.genesIn(papers);
    }

    /** Is this menu looking at that shelf? Used by the block's ticker. */
    public boolean isFor(EquineResearchShelfBlockEntity candidate) {
        return shelf != null && shelf == candidate;
    }

    public String selectedGene() {
        return selectedGene;
    }

    /**
     * <b>Every slot is live on the server; the tab only hides them on the
     * client.</b> The server has no screen and cannot know which tab the player
     * is looking at; the client will not send a click on a slot it is not
     * drawing, so hiding it there is the whole of the enforcement. Deciding it
     * twice, from state only one side has, is how the old filing slot came to
     * refuse everything.
     */
    private boolean activeOnTab(boolean tab) {
        return !player.level().isClientSide() || storeTab == tab;
    }

    public void setStoreTab(boolean store) {
        this.storeTab = store;
    }

    /** From the client's list click, and re-checked here against the shelf. Also set client-side for the highlight. */
    public void selectGene(String geneKey) {
        this.selectedGene = geneKey == null ? "" : geneKey;
        recomputeResult();
    }

    // ------------------------------------------------------------------
    // Copying
    // ------------------------------------------------------------------

    /** The gene is picked, the shelf still has it, and there is a book to write on. */
    private boolean canCopy() {
        return shelf != null
                && !selectedGene.isEmpty()
                && shelf.stores(selectedGene)
                && input.getItem(0).is(Items.BOOK);
    }

    /** How long this gene takes to copy - see {@link #TICKS_PER_RARITY_TIER}. */
    public static int copyTicks(String geneKey) {
        Gene gene = Genes.byKeyOrNull(geneKey);
        GeneRarity rarity = gene == null ? GeneRarity.DEFAULT : gene.rarity();
        return (rarity.ordinal() + 1) * TICKS_PER_RARITY_TIER;
    }

    /**
     * <b>One tick of copying</b>, driven by the block entity's ticker. Anything
     * that invalidates the job resets progress to zero rather than pausing it:
     * pulling the book out, or taking the original off the shelf mid-copy,
     * means the copy did not happen.
     */
    public void tickCopy() {
        if (shelf == null) {
            return;
        }
        if (!canCopy() || !result.getItem(0).isEmpty()) {
            if (data.get(DATA_PROGRESS) != 0) {
                data.set(DATA_PROGRESS, 0);
            }
            return;
        }
        int total = copyTicks(selectedGene);
        data.set(DATA_TOTAL, total);
        int progress = data.get(DATA_PROGRESS) + 1;
        if (progress < total) {
            data.set(DATA_PROGRESS, progress);
            return;
        }
        data.set(DATA_PROGRESS, 0);
        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_GENE.get(), selectedGene);
        result.setItem(0, paper);
        broadcastChanges();
        HorseProgress.complete(player, ProgressTask.COPY_PAPER);
    }

    public int copyProgress() {
        return data.get(DATA_PROGRESS);
    }

    public int copyTotal() {
        int total = data.get(DATA_TOTAL);
        return total <= 0 ? TICKS_PER_RARITY_TIER : total;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        recomputeResult();
    }

    /** Clear the result and restart the clock whenever the job changes. */
    private void recomputeResult() {
        if (shelf == null) {
            return; // client-side; the server sends the answer
        }
        if (!canCopy()) {
            result.setItem(0, ItemStack.EMPTY);
        }
        data.set(DATA_PROGRESS, 0);
        data.set(DATA_TOTAL, selectedGene.isEmpty() ? 0 : copyTicks(selectedGene));
        broadcastChanges();
    }

    // ------------------------------------------------------------------
    // Plumbing
    // ------------------------------------------------------------------

    /**
     * Shift-click. From the shelf or the machine into the player; from the
     * player, a paper goes onto the shelf and a book into the book slot -
     * {@code moveItemStackTo} asks each slot's {@code mayPlace}, so a
     * duplicate paper simply stays where it was.
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

    /** The book goes back to the player, as a workbench does. The papers stay on the shelf. */
    @Override
    public void removed(Player who) {
        super.removed(who);
        result.setItem(0, ItemStack.EMPTY);
        if (!who.level().isClientSide()) {
            clearContainer(who, input);
        }
    }
}
