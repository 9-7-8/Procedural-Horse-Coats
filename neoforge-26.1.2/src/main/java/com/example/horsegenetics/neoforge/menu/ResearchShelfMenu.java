package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.neoforge.block.EquineResearchShelfBlockEntity;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.network.ShelfSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>The Equine Research Shelf's menu.</b> Two tabs' worth of function behind
 * three slots.
 *
 * <ul>
 *   <li><b>Craft</b> - pick a gene the shelf holds, put a blank book in
 *       {@link #BOOK_SLOT}, take its copy out of {@link #RESULT_SLOT}. The book
 *       is spent; the shelf's own paper is not, and never is.</li>
 *   <li><b>Store</b> - drop a research paper into {@link #FILE_SLOT} and the
 *       shelf files it, or click a row to take one back.</li>
 * </ul>
 *
 * <h2>The shelf is the authority, always</h2>
 * The client is told what the shelf holds ({@link ShelfSyncPayload}) so it can
 * draw the list, and it is told again after every change. It is never asked.
 * Every operation here re-reads the block entity, so a forged packet can at
 * worst ask for a gene the shelf does not hold, and get nothing.
 *
 * <h2>Why filing consumes the paper immediately</h2>
 * {@link #FILE_SLOT} is an input that empties itself: put a paper in, the gene
 * is recorded and the item is gone the same tick. It could have been a slot you
 * leave papers sitting in, but then "what is in the shelf" would have two
 * answers - the filed set and the slot - and a player would have to know that
 * one of them does not count. A paper for a gene the shelf <i>already</i> holds
 * is left alone rather than eaten, so nothing is ever destroyed for nothing.
 */
public final class ResearchShelfMenu extends AbstractContainerMenu {

    public static final int BOOK_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    public static final int FILE_SLOT = 2;
    private static final int SLOT_COUNT = 3;

    private final Player player;
    private final @Nullable EquineResearchShelfBlockEntity shelf;

    /**
     * <b>Copying takes time, and how much is the gene's rarity.</b> One iron
     * ingot's smelt - 200 ticks, ten seconds - per rarity tier, so a common gene
     * is ten seconds and a mythic one a minute. Long enough that a copy is a
     * thing you set going rather than a thing that simply happens; short enough
     * that filling a shelf is not an evening.
     *
     * <p>Tiers count from one, not zero: {@link GeneRarity#COMMON} is the first
     * tier, not the free one.
     */
    public static final int TICKS_PER_RARITY_TIER = 200;

    /** {@link ContainerData} slots - vanilla's furnace pattern, synced by the menu itself. */
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    private static final int DATA_COUNT = 2;

    private final Container input = new SimpleContainer(1);
    private final Container filing = new SimpleContainer(1);
    private final ResultContainer result = new ResultContainer();

    private String selectedGene = "";

    /**
     * <b>Which tab the screen is showing</b>, so the slots that do not belong to
     * it can go inactive. Client-only state living on the menu, the way a
     * container screen's own toggles do: {@code Slot.x/y} are final, so a slot
     * cannot be moved out of the way - but {@link Slot#isActive()} is consulted
     * for both drawing <i>and</i> hit-testing, which is exactly the pair that
     * has to agree. Always true server-side, where there is no tab.
     */
    private boolean storeTab;

    /** Client-side mirror of the shelf's contents; the server reads the block entity. */
    private List<String> clientStored = List.of();

    /**
     * Progress and its target, in ticks. A {@link ContainerData} rather than a
     * payload of our own because the menu already syncs these every tick for
     * free, and a progress bar that lags is worse than no bar - see the furnace.
     */
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

        addSlot(new Slot(input, 0, 44, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK);
            }

            @Override
            public boolean isActive() {
                return !storeTab;
            }
        });
        addSlot(new Slot(result, 0, 116, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return !storeTab;
            }

            @Override
            public void onTake(Player taker, ItemStack taken) {
                input.removeItem(0, 1);
                recomputeResult();
                super.onTake(taker, taken);
            }
        });
        addSlot(new Slot(filing, 0, 80, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RESEARCH_PAPER.get());
            }

            @Override
            public boolean isActive() {
                return storeTab;
            }
        });

        addDataSlots(data);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 160));
        }
    }

    // ------------------------------------------------------------------
    // What the shelf holds
    // ------------------------------------------------------------------

    /** Server: the block entity. Client: whatever it was last told. */
    public List<String> storedGenes() {
        return shelf != null ? shelf.storedGenes() : clientStored;
    }

    /** Is this menu looking at that shelf? Used by the block's ticker. */
    public boolean isFor(EquineResearchShelfBlockEntity candidate) {
        return shelf != null && shelf == candidate;
    }

    public void acceptStored(List<String> genes) {
        this.clientStored = List.copyOf(genes);
    }

    public String selectedGene() {
        return selectedGene;
    }

    /** Client-side, from the screen, every frame. */
    public void setStoreTab(boolean store) {
        this.storeTab = store;
    }

    /** From the client's list click, and re-checked here against the shelf. */
    public void selectGene(String geneKey) {
        this.selectedGene = geneKey == null ? "" : geneKey;
        recomputeResult();
    }

    /** Take one filed paper back out. Server-side; ignores a gene not held. */
    public void withdraw(String geneKey) {
        if (shelf == null || !shelf.withdraw(geneKey)) {
            return;
        }
        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_GENE.get(), geneKey);
        if (!player.getInventory().add(paper)) {
            player.drop(paper, false);
        }
        if (geneKey.equals(selectedGene)) {
            selectedGene = "";
        }
        recomputeResult();
        sync();
    }

    private void sync() {
        if (shelf != null && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ShelfSyncPayload(shelf.storedGenes()));
        }
    }

    /** Push the current contents at the client - called once when the screen opens. */
    public void syncOnOpen() {
        sync();
    }

    // ------------------------------------------------------------------
    // The two operations
    // ------------------------------------------------------------------

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == filing) {
            fileWhateverIsThere();
        }
        recomputeResult();
    }

    /** A paper in the filing slot is recorded and consumed; a duplicate is left alone. */
    private void fileWhateverIsThere() {
        if (shelf == null) {
            return;
        }
        ItemStack stack = filing.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        String geneKey = stack.get(ModDataComponents.RESEARCH_GENE.get());
        if (geneKey == null || geneKey.isEmpty()) {
            return; // a blank paper - nothing to file
        }
        if (shelf.file(geneKey)) {
            stack.shrink(1);
            filing.setChanged();
            sync();
        }
    }

    /**
     * Is everything in place for a copy? The gene is picked, the shelf still has
     * it, and there is a book to write on.
     */
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
     * <b>One tick of copying.</b> Driven by the block entity's ticker, so the
     * work continues whether or not anybody has the screen open - a copy you set
     * going and walked away from is the point of it taking time at all.
     *
     * <p>Anything that invalidates the job resets progress to zero rather than
     * pausing it: pulling the book out, or withdrawing the original mid-copy,
     * means the copy did not happen, and a half-finished job that resumes an
     * hour later on a different gene would be worse than starting again.
     */
    public void tickCopy() {
        if (shelf == null) {
            return;
        }
        if (!canCopy() || !result.getItem(0).isEmpty()) {
            // nothing to do, or the finished paper is still sitting there
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
    }

    /** Ticks done, and ticks needed - both 0 when nothing is being copied. */
    public int copyProgress() {
        return data.get(DATA_PROGRESS);
    }

    public int copyTotal() {
        int total = data.get(DATA_TOTAL);
        return total <= 0 ? TICKS_PER_RARITY_TIER : total;
    }

    /**
     * Clear the result and restart the clock whenever the job changes. The paper
     * itself is produced by {@link #tickCopy}; this only ever takes it away.
     */
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

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOT_COUNT) {
            // out of the machine and into the player
            if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (!moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * Close if the shelf is gone or the player walked off. Distance rather than
     * {@code stillValid(ContainerLevelAccess, ...)} because this menu is built
     * from a block entity, not from a level access - and squared, because
     * {@code distanceToSqr} is what there is. 8 blocks, comfortably past
     * vanilla's reach, so it never closes on somebody standing at the shelf.
     */
    @Override
    public boolean stillValid(Player who) {
        if (shelf == null) {
            return true; // client copy - the server's answer is the one that counts
        }
        return !shelf.isRemoved()
                && who.distanceToSqr(shelf.getBlockPos().getCenter()) <= 64.0;
    }

    /** The book and any un-filed paper go back to the player, as a workbench does. */
    @Override
    public void removed(Player who) {
        super.removed(who);
        result.setItem(0, ItemStack.EMPTY);
        if (!who.level().isClientSide()) {
            clearContainer(who, input);
            clearContainer(who, filing);
        }
    }
}
